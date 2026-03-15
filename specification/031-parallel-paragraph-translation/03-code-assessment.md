# Code Assessment: Parallel Per-Paragraph Translation (Spec-31)

**Date:** 2026-03-15
**Scope:** `app/src/main/java/com/nononsenseapps/feeder/ai/`, `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/`

---

## Executive Summary

1. **Spec-30 infrastructure is complete but unwired** -- `translateWithProgress()`, `ChunkTranslationCoordinator`, `TranslationChunker`, and `TranslationProgress` are fully implemented but `ArticleViewModel.translate()` still calls the single-request `AIApi.translate()`
2. **TranslationState is too coarse** -- The current `Empty | Loading | Result` sealed interface cannot express per-paragraph progress; this is the primary UI-layer gap
3. **Batch-based concurrency limits progressive display** -- `ChunkTranslationCoordinator` processes batches via `chunks.chunked(batchSize).forEachIndexed` with `async/awaitAll`, meaning all items in a batch must complete before any item in the next batch starts
4. **Option A (Adapt Existing Chunk Infrastructure) is recommended** -- with one key refinement: replace batch-based concurrency with semaphore-based concurrency for true progressive per-paragraph updates
5. **Low test coverage in AI module** -- Only `TranslatableTextTest` exists; no tests for `ChunkTranslationCoordinator`, `TranslationChunker`, or `AIApi`

---

## User-Confirmed Data Model (MANDATORY)

The user has confirmed the following JSON structure as the core data model. All implementation MUST conform to this structure:

```json
{
  "contents": [
    {"index": 1, "text": "original text", "translation": "", "translated": 0},
    {"index": 2, "text": "original text", "translation": "", "translated": 0}
  ],
  "status": "initial"
}
```

### Kotlin Data Classes (New)

```kotlin
data class ParagraphTranslation(
    val index: Int,          // 1-based sequential identifier
    val text: String,        // Original paragraph text
    val translation: String, // Translated text (empty until done)
    val translated: Int,     // 0 = pending, 1 = completed
)

data class ArticleTranslation(
    val contents: List<ParagraphTranslation>,
    val status: String,      // "initial" | "translating" | "translated"
)
```

### Status Transitions

```
status: "initial"     -> User taps translate, paragraphs created with translated=0
status: "translating" -> First paragraph translation begins
status: "translated"  -> ALL paragraphs have translated=1
```

### Mapping to Existing Types

| User Model Field | Source | Notes |
|-----------------|--------|-------|
| `ParagraphTranslation.index` | Position in `extractTranslatableParagraphs()` result | 1-based |
| `ParagraphTranslation.text` | `TranslatableText.text` | Direct copy |
| `ParagraphTranslation.translation` | `ChunkTranslationResult.Success.translatedTexts[0]` | Single-paragraph chunk returns list of 1 |
| `ParagraphTranslation.translated` | 0 initially, 1 on success | Could add -1 for failed (requirements say `translated: Int`) |
| `ArticleTranslation.status` | Derived from paragraph states | "initial" before start, "translating" during, "translated" when all done |

### Impact on TranslationState

The `TranslationState` sealed interface should expose `ArticleTranslation` directly:

```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState                      // No translation requested
    data class Translating(                                    // In progress
        val articleTranslation: ArticleTranslation,           // Live-updating model
    ) : TranslationState
    data class Translated(                                     // All complete
        val articleTranslation: ArticleTranslation,           // Final model
    ) : TranslationState
    data class Error(val message: String) : TranslationState  // Fatal error
}
```

The `Loading` state is replaced by `Translating` which carries the live `ArticleTranslation` with per-paragraph status. The old `Result` wrapper around `AIClient.TranslationResult` is replaced by `Translated` and `Error` which are more direct.

---

## Component-by-Component Analysis

### 1. ArticleViewModel.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

| Aspect | Detail |
|--------|--------|
| `translate()` (line 491) | Calls `aiApi.translate(translatableTexts)` -- single-request path. **Must be rewired** to use progress-aware path |
| `TranslationState` (line 761) | `Empty \| Loading \| Result` -- **Must be enhanced** with per-paragraph progress states |
| `extractTranslatableParagraphs()` (line 548) | Recursively extracts `List<TranslatableText>` from `LinearArticle` with element type and nesting. **Reuse as-is** |
| `translationState` (line 120) | `MutableStateFlow<TranslationState>` -- existing reactive pattern. **Reuse pattern**, change type |
| DI pattern | Kodein: `private val aiApi: AIApi by instance()` (line 68). **Reuse as-is** |
| Coroutine scope | `viewModelScope.launch(Dispatchers.IO)` (line 492). **Reuse pattern** -- cancellation comes for free |
| Auto-translate | Lines 234-257: triggers `translate()` when `translationEnabled && translationState.value is TranslationState.Empty`. **Must update condition** to check new state type |

**Recommendation:** MODIFY -- Major changes to `translate()` method body and `TranslationState` sealed interface. Keep method signature and auto-translate trigger logic.

**Impact Scope:**
- `translate()` method body (~35 lines) -- rewrite to: (1) build `ArticleTranslation` with status="initial", (2) emit `Translating(articleTranslation)`, (3) collect `Flow<TranslationProgress>`, (4) on each `ChunkComplete`, update the specific `ParagraphTranslation` item (set `translation` and `translated=1`), (5) when all done, emit `Translated(articleTranslation)` with status="translated"
- `TranslationState` sealed interface -- replace `Loading`/`Result` with `Translating(ArticleTranslation)`/`Translated(ArticleTranslation)`/`Error(message)` per user-confirmed data model
- `viewState` combine (line 122-185) -- no change if `TranslationState` remains in same flow position (slot 10 of combine)
- Auto-translate condition (line 248) -- minor: check against new state type (`is TranslationState.Empty`)
- New: `ArticleTranslation` and `ParagraphTranslation` data classes -- define in AI package or same file

**Key pattern for progressive updates:**
```kotlin
// In translate(), after building initial ArticleTranslation:
translationState.update { currentState ->
    when (currentState) {
        is TranslationState.Translating -> {
            val updated = currentState.articleTranslation.contents.toMutableList()
            updated[paragraphIndex] = updated[paragraphIndex].copy(
                translation = translatedText,
                translated = 1,
            )
            TranslationState.Translating(
                articleTranslation = currentState.articleTranslation.copy(
                    contents = updated,
                    status = if (updated.all { it.translated == 1 }) "translated" else "translating",
                ),
            )
        }
        else -> currentState  // Ignore if cancelled/error
    }
}
```
This uses `MutableStateFlow.update {}` (atomic CAS) to avoid race conditions from concurrent paragraph completions.

---

### 2. AIApi.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

| Aspect | Detail |
|--------|--------|
| `translate()` (line 125) | Single-request translation. **Keep for backward compat** (NFR-4), but no longer called from ViewModel |
| `translateWithProgress()` (line 178) | Already returns `Flow<TranslationProgress>`. **Key modification point** for per-paragraph mode |
| Short-circuit logic (line 203) | `if (!chunker.needsChunking(translatableTexts))` uses single translate. **Must change** -- per-paragraph mode always chunks |
| Client creation pattern (line 240-252) | Timeout-aware settings copy. **Reuse pattern** |

**Recommendation:** MODIFY -- Moderate changes to `translateWithProgress()` to support per-paragraph mode. The key change is creating 1-paragraph-per-chunk `TranslationChunk` instances instead of using `TranslationChunker`.

**Specific changes needed:**
- Add a parameter or new method for per-paragraph mode (bypasses `TranslationChunker`)
- Create `TranslationChunk` with 1 paragraph each, using paragraph index as chunk ID
- Feed these single-paragraph chunks through `ChunkTranslationCoordinator`

---

### 3. ChunkTranslationCoordinator.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/ChunkTranslationCoordinator.kt`

| Aspect | Detail |
|--------|--------|
| Concurrency model (line 93) | `chunks.chunked(batchSize).forEachIndexed` -- **batch-based, not semaphore-based** |
| Progress emission (line 104-115) | Emits `ChunkComplete` after each batch item. With batch model, all batch items emit together **after the slowest item completes** |
| Retry logic (line 167) | `translateChunkWithRetry` with exponential backoff. **Reuse as-is** |
| `isRetryableError` (line 204) | Pattern-based error classification. **Reuse as-is** |
| Constants | `DEFAULT_CONCURRENCY=3`, `MAX_CONCURRENCY=5`, `DEFAULT_MAX_RETRIES=3`. **Reuse as-is** |
| Result assembly (line 139-147) | Sorts by `chunkId` and flattens. Works for single-paragraph chunks since chunkId == paragraph index |

**Critical finding -- batch vs semaphore concurrency:**

Current behavior with 10 paragraphs and concurrency=3:
```
Batch 1: [P1, P2, P3] -- all start, wait for ALL to finish
  -> emit ChunkComplete(1), ChunkComplete(2), ChunkComplete(3) -- rapid-fire after batch
Batch 2: [P4, P5, P6] -- starts only after batch 1 ALL complete
  -> emit ChunkComplete(4), ChunkComplete(5), ChunkComplete(6)
...
```

Desired behavior for progressive display:
```
P1, P2, P3 start in parallel (semaphore=3)
P1 completes -> emit immediately, P4 starts
P3 completes -> emit immediately, P5 starts
P2 completes -> emit immediately, P6 starts
...
```

**Recommendation:** MODIFY -- Replace batch-based concurrency with `Semaphore`-based concurrency for true progressive per-paragraph emission. This is the highest-impact change for user experience.

**Alternative:** Keep batch model and accept that progress updates come in bursts of 3. This is simpler but less responsive.

---

### 4. TranslationChunker.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunker.kt`

| Aspect | Detail |
|--------|--------|
| `createChunks()` (line 41) | Groups paragraphs into ~2000-char chunks. **Not used** for per-paragraph mode |
| `needsChunking()` (line 106) | Checks total size > maxChunkSize. **Not used** for per-paragraph mode |
| MIN_CHUNK_SIZE (line 50) | 500 chars minimum. **Irrelevant** for per-paragraph mode |

**Recommendation:** NO CHANGE -- Bypass entirely for per-paragraph translation. The per-paragraph approach creates `TranslationChunk` instances directly (1 paragraph each) without using `TranslationChunker`.

---

### 5. TranslationProgress.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationProgress.kt`

| Aspect | Detail |
|--------|--------|
| `Starting(totalChunks)` | Works for paragraphs -- rename concept but type works |
| `Translating(current, total)` | Works as-is for per-paragraph progress |
| `ChunkComplete(current, total, result)` | Works for per-paragraph -- `result` contains `ChunkTranslationResult` with `translatedTexts: List<String>` (list of 1 for single-paragraph chunks) |
| `Complete(translatedParagraphs)` | Works as-is -- final assembled list |
| `Error(error)` | Works as-is |
| `getProgressPercentage()` | Works as-is |

**Recommendation:** NO CHANGE or MINOR -- The existing types work semantically for per-paragraph mode. The `ChunkComplete.result` is a `ChunkTranslationResult.Success(chunkId, translatedTexts)` where `chunkId` maps to paragraph index and `translatedTexts` is a single-element list. Optionally add a `ParagraphComplete` variant for cleaner API, but not required.

---

### 6. TranslatableText.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableText.kt`

**Recommendation:** NO CHANGE -- Reuse as-is. Already contains `text`, `elementType`, `nestingLevel` needed for per-paragraph translation units.

---

### 7. TranslationChunk.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunk.kt`

**Recommendation:** NO CHANGE -- Reuse as-is for single-paragraph wrapping:
```kotlin
TranslationChunk(
    id = paragraphIndex,     // 0-based
    texts = listOf(paragraph),  // single paragraph
    characterCount = paragraph.text.length,
    estimatedTokens = paragraph.text.length / 4,
    startIndex = paragraphIndex,
    endIndex = paragraphIndex + 1,
)
```

---

### 8. AIClient.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

| Aspect | Detail |
|--------|--------|
| `translate()` (line 102) | Takes `List<TranslatableText>`, returns `TranslationResult`. **No change** (NFR-4) |
| `translateChunk()` (line 118) | Takes `TranslationChunk`, returns `ChunkTranslationResult`. **No change** (NFR-4) -- this is the workhorse for per-paragraph translation |
| `TranslationResult` (line 67) | `Success(paragraphs: List<String>) \| Error(content)`. **No change** |
| Factory `create()` (line 161) | Creates provider-specific client. **No change** |

**Recommendation:** NO CHANGE -- Per NFR-4, these interfaces are frozen. Per-paragraph translation feeds single-paragraph `TranslationChunk` through existing `translateChunk()`.

---

### 9. Provider Implementations (OpenAICompatibleClient.kt, AnthropicClient.kt)

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

Both providers implement identical patterns:
- `translateChunk()` builds prompt, calls API, parses JSON response
- The prompt template works for any number of paragraphs (including 1)
- `parseTranslationResponse()` expects `expectedParagraphs` count match
- `isRetryableError()` classifies errors for retry

**Recommendation:** NO CHANGE -- Single-paragraph chunks work through existing `translateChunk()` without modification. The prompt says "Translate ALL paragraphs. Return exactly N translations" -- with N=1, this works correctly.

**Note:** Sending 1 paragraph per API call means the prompt overhead (system instructions) is repeated for each call. This increases total token usage but each individual call is fast and bounded.

---

### 10. ArticleScreen.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

| Aspect | Detail |
|--------|--------|
| `TranslationStatusSection` (line 695) | Handles `Empty \| Loading \| Result`. **Must handle** new `Translating(ArticleTranslation)` state with determinate progress |
| Translate button (line 284-303) | Checks `viewState.translation is TranslationState.Loading`. **Must update** to disable during `Translating` |
| Inline paragraph rendering (line 554-567) | Extracts `translatedParagraphs` from `TranslationState.Result.Success.paragraphs`. **Must update** to extract from `ArticleTranslation.contents` -- both `Translating` and `Translated` states carry the data |
| `linearArticleContent()` call (line 565) | Takes `translatedParagraphs: List<String>?`. **Must change** to support partial translations from `ArticleTranslation.contents` (paragraphs where `translated == 1` have text, others are pending) |

**Recommendation:** MODIFY -- Moderate changes to support progressive display:
- `TranslationStatusSection`: Handle `Translating` state with determinate `LinearProgressIndicator(progress = completedCount / totalCount)` and text "X/Y paragraphs translated"
- Translate button: Disable during `Translating` state (not just old `Loading`)
- Paragraph rendering: Extract `translatedParagraphs` from `ArticleTranslation.contents` by filtering `translated == 1` items. Can build a `List<String?>` where index maps to paragraph position, with `null` for pending paragraphs

---

### 11. LinearArticleContent.kt

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

| Aspect | Detail |
|--------|--------|
| `linearArticleContent()` (line 252) | Takes `translatedParagraphs: List<String>?`. Currently assumes all-or-nothing |
| `computeParagraphIndices()` | Maps element positions to paragraph indices for translation lookup |

**Recommendation:** MINOR MODIFY -- The existing `translatedParagraphs: List<String>?` can be adapted to support partial results if we use a `List<String?>` (nullable elements for pending paragraphs). Alternatively, keep `List<String>?` and only pass completed paragraphs with placeholder text for pending ones.

---

## Pattern Summary

### DI Pattern
- **Framework:** Kodein
- **Registration:** `bind<AIApi>() with singleton { ... }` in `ArchModelModule.kt:47`
- **Injection:** `private val aiApi: AIApi by instance()` in ViewModel
- **Convention:** Singletons for API classes, instance creation for clients

### State Management Pattern
- **Pattern:** `MutableStateFlow<SealedInterface>` with `StateFlow` exposure via `combine().stateIn()`
- **Location:** ViewModel holds mutable flows, combines them into single `viewState: StateFlow<ArticleScreenViewState>`
- **Convention:** Sealed interfaces for state (not sealed classes). Data objects for stateless states, data classes for stateful states
- **Example:** `AISummaryState` (line 743): `Empty | Loading | Result(value)` -- same pattern as current `TranslationState`

### Coroutine Pattern
- **Scope:** `viewModelScope.launch(Dispatchers.IO)` for ViewModel operations
- **Cancellation:** Automatic via `viewModelScope` lifecycle
- **Flow collection:** Not yet used for translation (only `first()` calls), but `translateWithProgress()` returns `Flow` ready for collection

### Sealed Interface Pattern (Current vs Proposed)

Current:
```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data object Loading : TranslationState
    data class Result(val value: TranslationResult) : TranslationState
}
```

Proposed (aligned with user-confirmed data model):
```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data class Translating(val articleTranslation: ArticleTranslation) : TranslationState
    data class Translated(val articleTranslation: ArticleTranslation) : TranslationState
    data class Error(val message: String) : TranslationState
}
```
Convention: Define in same file as ViewModel. Use `@Immutable` on view state interfaces. The `ArticleTranslation` and `ParagraphTranslation` data classes should be defined in the `ai` package alongside other translation models.

### Error Handling Pattern
- Try-catch in ViewModel `translate()` with fallback to error state
- Provider-level retry classification in `isRetryableError()`
- Coordinator-level retry with exponential backoff
- User-friendly error messages in `handleTranslationError()`

### Compose UI Pattern
- Conditional rendering: `if (viewState.translation !is TranslationState.Empty)` guards
- `LazyListScope` extensions for content rendering
- `OutlinedCard` for status/error sections
- `LinearProgressIndicator` for indeterminate progress

---

## Risk Assessment

### Risk 1: API Cost and Rate Limiting (HIGH)

**Issue:** Per-paragraph translation sends N API calls instead of 1 (or ceil(totalChars/2000)). A 20-paragraph article sends 20 API calls.

**Impact:**
- 20x more API calls = higher cost per translation
- Higher chance of hitting rate limits, especially with concurrency=3
- Each call includes full prompt template (~600 tokens overhead)

**Mitigation:**
- Concurrency cap at 3 (existing `DEFAULT_CONCURRENCY`) limits parallel requests
- Exponential backoff on rate limit errors (existing retry logic)
- Consider AC-9 optimization: short articles (< 2000 chars total) use single call

### Risk 2: Translation Quality Degradation (MEDIUM)

**Issue:** Translating paragraphs in isolation loses cross-paragraph context (pronoun references, terminology consistency).

**Impact:** Translations may be inconsistent in terminology, miss contextual references.

**Mitigation:**
- Include surrounding paragraph context in prompt (e.g., previous paragraph as context)
- Or accept the trade-off: per-paragraph speed vs. cross-paragraph quality
- The prompt already instructs "Use consistent terminology throughout the translation" but with single paragraphs, the model has no "throughout" to be consistent with

### Risk 3: Batch Concurrency Limits Progressive Display (MEDIUM)

**Issue:** Current `ChunkTranslationCoordinator` uses `chunks.chunked(batchSize).forEachIndexed` with `awaitAll()`. All items in a batch must complete before the next batch starts.

**Impact:** With 10 paragraphs and concurrency=3, user sees updates in bursts of 3, not individually. If one paragraph in a batch takes 10s and others take 2s, all 3 emit after 10s.

**Mitigation:** Replace with `Semaphore(concurrency)` + individual `async` launches. Each paragraph emits immediately on completion. This is a moderate refactor of `translateChunks()`.

### Risk 4: Memory with Large Articles (LOW)

**Issue:** Holding N `TranslationChunk` objects + N `ChunkTranslationResult` objects + progressive state updates.

**Impact:** Minimal -- each object is small (string + metadata). Even a 50-paragraph article is well within Android memory bounds.

**Mitigation:** No action needed. The `StateFlow` approach efficiently handles updates.

### Risk 5: Race Condition in State Updates (LOW)

**Issue:** Multiple coroutines completing paragraphs concurrently could race when updating `translationState` MutableStateFlow. Since `ArticleTranslation.contents` is an immutable list that must be copied on each update, concurrent updates could overwrite each other.

**Impact:** Lost updates if two paragraphs complete at exactly the same time and both try to update the state.

**Mitigation:** Use `MutableStateFlow.update { }` (atomic compare-and-set) instead of direct `.value =` assignment. This is already the pattern used elsewhere in the codebase (line 50, 108, 279, etc.). The update lambda receives the current state and returns the new state atomically, preventing lost updates even with concurrent paragraph completions.

### Risk 6: Cancellation Cleanup (LOW)

**Issue:** If user navigates away during translation, all in-flight requests must be cancelled.

**Impact:** Wasted API calls and potential coroutine leaks.

**Mitigation:** Already handled by `viewModelScope` cancellation propagation. When ViewModel is cleared, all child coroutines (including the Flow collection and coordinator coroutines) are cancelled automatically.

---

## Architecture Recommendation

### Recommended: Option A -- Adapt Existing Chunk Infrastructure

**With one key refinement:** Replace batch-based concurrency in `ChunkTranslationCoordinator` with semaphore-based concurrency.

**Rationale:**

1. **Minimal new code:** The entire pipeline exists: `AIApi.translateWithProgress()` -> `ChunkTranslationCoordinator.translateChunks()` -> `AIClient.translateChunk()`. Only the input preparation and coordinator concurrency model need changes.

2. **Proven retry/concurrency logic:** `translateChunkWithRetry()` with exponential backoff, `isRetryableError()` classification -- all reusable without modification.

3. **Natural mapping:** `TranslationChunk(id=paragraphIndex, texts=listOf(singleParagraph))` maps cleanly to existing types. `ChunkTranslationResult.Success(chunkId=paragraphIndex, translatedTexts=listOf(translation))` provides paragraph-level results.

4. **Flow-based progress:** `translateChunks()` already returns `Flow<TranslationProgress>` with `ChunkComplete` per-item emissions. The ViewModel just needs to collect this Flow and update an enhanced `TranslationState`.

**Why not Option B (New Coordinator):** Duplicates concurrency/retry logic. The existing coordinator is well-structured and only needs the concurrency model refinement.

**Why not Option C (Enhance Chunker):** Adding a "paragraph mode" to `TranslationChunker` is unnecessary complexity. It's simpler to create single-paragraph chunks directly in `AIApi.translateWithProgress()`.

### Implementation Blueprint

```
Phase 1: Define Data Model (new files in ai package)
  - ParagraphTranslation(index, text, translation, translated)
  - ArticleTranslation(contents, status)
  - Matches user-confirmed JSON structure exactly

Phase 2: Enhance TranslationState (ViewModel layer)
  - Replace Loading/Result with Translating(ArticleTranslation)/Translated(ArticleTranslation)/Error
  - Translating state carries live-updating ArticleTranslation
  - Translated state carries final ArticleTranslation with status="translated"

Phase 3: Create per-paragraph chunks in AIApi
  - New method or mode in translateWithProgress() that creates 1-para-per-chunk
  - Bypass TranslationChunker
  - Each TranslationChunk.id maps to ParagraphTranslation.index (0-based internally)

Phase 4: Refine ChunkTranslationCoordinator concurrency
  - Replace chunks.chunked(batchSize) with Semaphore-based concurrency
  - Each paragraph emits ChunkComplete immediately on completion

Phase 5: Wire ViewModel to collect Flow and update ArticleTranslation
  - translate() builds initial ArticleTranslation with status="initial"
  - Collects translateWithProgress() Flow
  - On each ChunkComplete: atomically update specific ParagraphTranslation
    (set translation text, translated=1)
  - On all complete: transition to Translated state with status="translated"

Phase 6: Update ArticleScreen UI
  - TranslationStatusSection: determinate progress bar from ArticleTranslation
  - linearArticleContent: extract partial translations from ArticleTranslation.contents
  - Translate button: disable during Translating state
```

---

## Reuse vs Modify Summary

| Component | File | Recommendation | Change Scope |
|-----------|------|----------------|-------------|
| `extractTranslatableParagraphs()` | ArticleViewModel.kt:548 | **Reuse as-is** | None |
| `TranslatableText` | TranslatableText.kt | **Reuse as-is** | None |
| `TranslationChunk` | TranslationChunk.kt | **Reuse as-is** | None |
| `AIClient` interface | AIClient.kt | **Reuse as-is** (NFR-4) | None |
| `AIClient.translateChunk()` | AIClient.kt:118 | **Reuse as-is** (NFR-4) | None |
| `OpenAICompatibleClient` | OpenAICompatibleClient.kt | **Reuse as-is** | None |
| `AnthropicClient` | AnthropicClient.kt | **Reuse as-is** | None |
| `TranslationProgress` states | TranslationProgress.kt | **Reuse as-is** | None |
| `ChunkTranslationResult` | TranslationProgress.kt:98 | **Reuse as-is** | None |
| `TranslationChunker` | TranslationChunker.kt | **Bypass** (not used) | None |
| `translateChunkWithRetry()` | ChunkTranslationCoordinator.kt:167 | **Reuse as-is** | None |
| `isRetryableError()` | ChunkTranslationCoordinator.kt:204 | **Reuse as-is** | None |
| `ParagraphTranslation` | NEW file in ai package | **Create** | New data class per user model |
| `ArticleTranslation` | NEW file in ai package | **Create** | New data class per user model |
| `translateChunks()` concurrency | ChunkTranslationCoordinator.kt:77 | **Modify** | Replace batch with semaphore |
| `TranslationState` | ArticleViewModel.kt:761 | **Modify** | Replace with `Empty/Translating(ArticleTranslation)/Translated(ArticleTranslation)/Error` |
| `translate()` | ArticleViewModel.kt:491 | **Modify** | Build ArticleTranslation, collect Flow, update per-paragraph |
| `AIApi.translateWithProgress()` | AIApi.kt:178 | **Modify** | Add per-paragraph chunk creation |
| `TranslationStatusSection` | ArticleScreen.kt:695 | **Modify** | Determinate progress from ArticleTranslation |
| `linearArticleContent()` | LinearArticleContent.kt:252 | **Minor modify** | Extract partial translations from ArticleTranslation.contents |
| Translate button | ArticleScreen.kt:284 | **Minor modify** | Disable during `Translating` |

---

## Files Examined

- `ArticleViewModel.kt` (789 lines) -- ViewModel, state, translate entry point
- `AIApi.kt` (279 lines) -- API facade, translate and translateWithProgress
- `ChunkTranslationCoordinator.kt` (242 lines) -- Parallel chunk translation, retry
- `TranslationChunker.kt` (126 lines) -- Chunk creation from paragraphs
- `TranslationProgress.kt` (124 lines) -- Progress state sealed classes
- `TranslatableText.kt` (91 lines) -- Paragraph model with structure metadata
- `TranslationChunk.kt` (52 lines) -- Chunk model
- `AIClient.kt` (167 lines) -- Unified AI client interface
- `OpenAICompatibleClient.kt` (884 lines) -- OpenAI provider implementation
- `AnthropicClient.kt` (794 lines) -- Anthropic provider implementation
- `ArticleScreen.kt` (778 lines) -- UI rendering, translation display
- `LinearArticleContent.kt` (relevant excerpt) -- Inline paragraph rendering
- `TranslatableTextTest.kt` (160 lines) -- Only existing test in AI module
- `ArchModelModule.kt` (relevant line) -- DI registration
