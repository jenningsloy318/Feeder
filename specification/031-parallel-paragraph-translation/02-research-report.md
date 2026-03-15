# Research Report: Parallel Per-Paragraph Translation

**Date:** 2026-03-15
**Technologies:** Kotlin, Coroutines, Jetpack Compose, StateFlow
**Freshness Score:** 90% of sources < 1 year old

---

## Summary

- **Semaphore + channelFlow** is the best concurrency pattern for per-paragraph translation — it gives immediate progressive emissions (no batch-waiting) and fine-grained concurrency control
- The existing `ChunkTranslationCoordinator` uses `chunked(batchSize) + async/awaitAll`, which **blocks progress emissions until each entire batch completes** — a fundamental limitation for progressive per-paragraph updates
- **Option A (Adapt Existing Chunk Infrastructure)** is recommended: create single-paragraph chunks and replace the batch loop with Semaphore-based concurrency inside the existing coordinator
- **User-confirmed data model:** `ArticleTranslation(contents: List<ParagraphTranslation>, status: String)` with `ParagraphTranslation(index, text, translation, translated)` — the ViewModel exposes this structure directly via `TranslationState`
- `StateFlow<ArticleTranslation>` with copy-on-write via `update {}` is the correct pattern for Compose — avoids full-list recomposition when combined with `key()` in LazyColumn
- Rate limiting is already well-handled by Semaphore permits + existing exponential backoff retry logic

---

## 1. Kotlin Coroutines Parallel Patterns

### 1.1 Pattern Analysis

Five concurrency approaches were evaluated for bounded parallel API calls with progressive result emission:

#### Pattern A: `chunked(n) + async/awaitAll` (Current Implementation)

```kotlin
// Current ChunkTranslationCoordinator.kt:93
chunks.chunked(batchSize).forEachIndexed { batchIndex, batch ->
    val batchResults = withContext(Dispatchers.IO) {
        batch.map { chunk -> async { translateChunkWithRetry(chunk, targetLanguage) } }.awaitAll()
    }
    // Progress emitted ONLY after entire batch completes
    batchResults.forEachIndexed { indexInBatch, result ->
        emit(TranslationProgress.ChunkComplete(...))
    }
}
```

**Problem:** `awaitAll()` suspends until ALL items in the batch finish. If batch has 3 items and items 1,2 finish in 1s but item 3 takes 10s, no progress is emitted for 10s. Progress is "bursty" — emits N items at once, then waits.

**Sources:** Kotlin docs on `awaitAll`, Marcin Moskala (kt.academy) on flatMapMerge pitfalls (2025-06)

#### Pattern B: `Semaphore + coroutineScope/launch` (Recommended)

```kotlin
fun translateParagraphs(...): Flow<ParagraphProgress> = channelFlow {
    val semaphore = Semaphore(concurrency)
    coroutineScope {
        paragraphs.forEachIndexed { index, paragraph ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    val result = translateWithRetry(paragraph, targetLanguage)
                    send(ParagraphComplete(index, result)) // Immediate emission
                }
            }
        }
    }
}
```

**Advantage:** Each paragraph emits progress THE INSTANT it completes. When one finishes, the semaphore immediately releases a permit for the next waiting paragraph. No batch-waiting.

**Sources:** Shreyas Patil (blog.shreyaspatil.dev), Ali Khavari (Medium 2025-03), Prem Thakur (Medium 2025-10), Kotlin official Semaphore docs

#### Pattern C: `flatMapMerge(concurrency)`

```kotlin
paragraphs.asFlow()
    .flatMapMerge(concurrency) { paragraph ->
        flow { emit(translate(paragraph)) }
    }
```

**Problem:** `flatMapMerge` does NOT guarantee ordering and has subtle pitfalls documented by Marcin Moskala (2025-06). It also doesn't provide natural access to the paragraph index for progress tracking. The internal implementation uses `ChannelFlowMerge` with Semaphore anyway.

**Sources:** Moskala "Hidden pitfalls of flatMapMerge" (kt.academy 2025-06), Kotlin/kotlinx.coroutines#1147

#### Pattern D: `channelFlow + produce`

Similar to Pattern B but uses Channel explicitly. More complex, no practical advantage over Semaphore approach.

#### Pattern E: `Dispatchers.IO.limitedParallelism(n)`

```kotlin
val limited = Dispatchers.IO.limitedParallelism(3)
paragraphs.map { async(limited) { translate(it) } }.awaitAll()
```

**Problem:** Limits THREADS, not TASKS. If translations are I/O-bound (network), tasks suspend and release threads — so `limitedParallelism` doesn't actually limit concurrent API calls. Semaphore limits at the task level regardless of dispatcher.

**Sources:** Prem Thakur "limitedParallelism vs Semaphore" (Medium 2025-10)

### 1.2 Concurrency Pattern Comparison

| Criteria | chunked+awaitAll | Semaphore+channelFlow | flatMapMerge | limitedParallelism |
|---|---|---|---|---|
| Progressive emissions | Batch-delayed | Immediate per-item | Immediate but unordered | Batch-delayed |
| Concurrency control | Exact batch size | Exact permit count | Exact concurrency param | Thread count (not task) |
| Ordering guarantee | Batch-ordered | Emit order varies, index available | No ordering | Emit order varies |
| Complexity | Low | Medium | Low | Low |
| Backpressure | N/A (batch) | Channel buffer | Internal buffer | N/A |
| Cancellation | Good | Excellent (structured) | Good | Good |
| Fits progressive UI | Poor | Excellent | Acceptable | Poor |

### 1.3 Recommendation

**Use Pattern B: `Semaphore` + `channelFlow`** for the paragraph coordinator. This provides:
- Immediate per-paragraph progress emissions (best UX)
- Exact concurrency control at the task level (respects API rate limits)
- Natural integration with `Flow<TranslationProgress>` (existing pattern)
- Structured concurrency with proper cancellation

---

## 2. Confirmed Data Model & StateFlow Patterns

### 2.0 User-Confirmed Data Model (MANDATORY)

The user has confirmed the following JSON structure as the core data model. All implementation MUST use this structure:

```json
{
  "contents": [
    {"index": 1, "text": "original text", "translation": "", "translated": 0},
    {"index": 2, "text": "original text", "translation": "", "translated": 0}
  ],
  "status": "initial"
}
```

This maps to Kotlin data classes:

```kotlin
@Serializable
data class ParagraphTranslation(
    val index: Int,          // 1-based sequential identifier
    val text: String,        // Original paragraph text
    val translation: String, // Translated text (empty until done)
    val translated: Int,     // 0 = pending, 1 = completed, -1 = failed
)

@Serializable
data class ArticleTranslation(
    val contents: List<ParagraphTranslation>,
    val status: String,      // "initial" | "translating" | "translated"
)
```

**Status transitions:**
- `"initial"` — Created from extracted paragraphs, no translation started
- `"translating"` — At least one paragraph translation is in-flight
- `"translated"` — ALL paragraphs have `translated = 1` (or all completed/failed)

**`translated` field values:**
- `0` = pending (not yet translated)
- `1` = completed (translation succeeded)
- `-1` = failed (translation failed after retries)

**Integration with TranslationState:**

The existing `TranslationState` sealed interface should be enhanced to wrap `ArticleTranslation`:

```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data object Loading : TranslationState  // Brief initial state before paragraphs are prepared
    data class Translating(
        val articleTranslation: ArticleTranslation,  // Progressive updates here
    ) : TranslationState
    data class Result(
        val value: AIClient.TranslationResult,       // Keep for backwards compat
        val articleTranslation: ArticleTranslation?,  // Final state
    ) : TranslationState
}
```

### 2.1 `StateFlow<ArticleTranslation>` vs `SnapshotStateList`

| Criteria | StateFlow<List<T>> | SnapshotStateList |
|---|---|---|
| Thread safety | Built-in (atomic updates) | Compose snapshot system |
| ViewModel usage | Standard pattern in this codebase | Requires Compose dependency in ViewModel |
| Recomposition scope | Entire list reference changes | Per-element observation possible |
| Existing pattern | Already used (`translationState: MutableStateFlow`) | Not used anywhere in codebase |
| Fits ArticleTranslation model | Yes — wrap in StateFlow | Would need separate tracking |
| Testability | Easy (Flow testing) | Requires Compose test harness |

**Sources:** Dnyaneshwar Patil "Single vs Multiple StateFlows" (Medium 2025-10), Davide Agostini "Stabilizing LazyColumn" (2026-02), Reza Ramesh "Deep Dive into Snapshot System" (Stackademic 2025-11)

### 2.2 Efficient Updates with ArticleTranslation Model

The key concern: when one paragraph completes, updating the `ArticleTranslation` inside `TranslationState` creates a new object reference, which could trigger recomposition of ALL paragraph items.

**Solution: Use `key()` in LazyColumn/Column**

```kotlin
// In Compose UI
articleTranslation.contents.forEach { paragraph ->
    key(paragraph.index) { // Stable key prevents unnecessary recomposition
        ParagraphItem(paragraph)
    }
}
```

With stable keys and `@Immutable`/`@Stable` data classes, Compose skips recomposition for unchanged items even when the list reference changes. This is the established pattern in this codebase (see `ArticleItemKeyHolder`).

**Recommended StateFlow update pattern:**

```kotlin
// In ViewModel — update a single paragraph within ArticleTranslation
private fun updateParagraphTranslation(index: Int, translatedText: String) {
    translationState.update { currentState ->
        if (currentState is TranslationState.Translating) {
            val updated = currentState.articleTranslation.let { at ->
                val newContents = at.contents.toMutableList().also {
                    val i = index - 1 // 1-based index to 0-based
                    it[i] = it[i].copy(translation = translatedText, translated = 1)
                }
                val allDone = newContents.all { it.translated == 1 }
                at.copy(
                    contents = newContents,
                    status = if (allDone) "translated" else "translating"
                )
            }
            TranslationState.Translating(articleTranslation = updated)
        } else currentState
    }
}
```

`StateFlow.update {}` is atomic and thread-safe. Combined with `@Stable` annotation on `ParagraphTranslation`, Compose will skip recomposition for unchanged items.

### 2.3 Recommendation

**Use `MutableStateFlow<TranslationState>` wrapping `ArticleTranslation`** with copy-on-write updates. This:
- Follows existing codebase patterns (`MutableStateFlow<TranslationState>`)
- Is thread-safe for concurrent paragraph completion
- Works efficiently with Compose when items have stable keys
- Maps directly to the user-confirmed `ArticleTranslation` JSON structure
- Avoids pulling Compose dependencies into ViewModel/domain layer

---

## 3. Architecture Options Analysis

### Option A: Adapt Existing Chunk Infrastructure

**Description:** Create single-paragraph `TranslationChunk` instances (1 paragraph per chunk) and modify `ChunkTranslationCoordinator` to use Semaphore-based concurrency instead of `chunked/awaitAll`. Enhance `TranslationState` for per-paragraph progress.

**Strengths:**
- Reuses proven retry logic (`translateChunkWithRetry` at `ChunkTranslationCoordinator.kt:167`)
- Reuses `AIClient.translateChunk()` interface — no new API surface
- `TranslationChunker` already handles the split — just set `maxChunkSize` to force 1-per-chunk or bypass it
- `TranslationProgress` sealed class already has `ChunkComplete` — semantically maps to paragraph completion
- Minimal new code: ~50 lines to refactor the batch loop to Semaphore

**Weaknesses:**
- Semantic mismatch: a "chunk" containing exactly 1 paragraph is a naming smell
- `TranslationChunk` carries extra fields (`characterCount`, `estimatedTokens`, `startIndex`, `endIndex`) that are overhead for single paragraphs
- The `ChunkComplete` progress event name doesn't match "paragraph complete"

**Code reuse:** High (~80% of existing coordinator logic reused)
**Implementation effort:** Low (modify ~2 files)
**Testability:** Good (existing test patterns apply)
**Progressive update quality:** Excellent (with Semaphore refactor)

### Option B: New Per-Paragraph Coordinator

**Description:** Create `ParagraphTranslationCoordinator` that translates individual `TranslatableText` items directly, with its own concurrency control, retry, and progress emission.

**Strengths:**
- Clean API: operates on `TranslatableText` directly, no chunk wrapping
- Purpose-built progress events (`ParagraphComplete` vs `ChunkComplete`)
- No semantic mismatch — naming is clear
- Can be designed from scratch with Semaphore pattern

**Weaknesses:**
- Duplicates ~60% of `ChunkTranslationCoordinator` logic (retry, backoff, error classification, progress flow)
- New test surface area (need to rewrite tests for the same logic)
- Two coordinators to maintain going forward
- Violates DRY — the retry/backoff logic is identical

**Code reuse:** Low (~20%)
**Implementation effort:** Medium-High (new file, new tests, new progress types)
**Testability:** Good but redundant test effort
**Progressive update quality:** Excellent (built with Semaphore from start)

### Option C: Enhance Chunker with Paragraph Mode

**Description:** Add a mode to `TranslationChunker` that creates 1-paragraph-per-chunk, then use existing coordinator unchanged.

**Strengths:**
- Simplest change: literally `TranslationChunker(maxChunkSize = 1)` or a flag
- No coordinator changes needed (if batch-delayed progress is acceptable)

**Weaknesses:**
- Does NOT solve the core progressive update problem — coordinator still uses `chunked/awaitAll` batches
- With 20 paragraphs and concurrency=3, still emits progress in batches of 3
- `MIN_CHUNK_SIZE = 500` validation would need bypassing
- The semantic mismatch is even worse here — the chunker exists to GROUP paragraphs

**Code reuse:** High (~95%)
**Implementation effort:** Very Low (1-2 line change)
**Testability:** Existing tests work
**Progressive update quality:** Poor (batch-delayed, same as current)

### Architecture Options Comparison Matrix

| Criteria | Option A: Adapt Chunks | Option B: New Coordinator | Option C: Chunker Mode |
|---|---|---|---|
| Code Reuse | High (80%) | Low (20%) | Very High (95%) |
| Implementation Effort | Low | Medium-High | Very Low |
| Progressive Updates | Excellent* | Excellent | Poor |
| Semantic Clarity | Acceptable | Excellent | Poor |
| Testability | Good | Good (but duplicate) | Good |
| Maintenance Burden | Low | Medium (2 coordinators) | Low |
| DRY Compliance | Good | Poor | Good |
| Risk | Low | Medium | Low |

*\* Requires refactoring the batch loop to Semaphore in existing coordinator*

### Recommendation

**Recommended: Option A — Adapt Existing Chunk Infrastructure**

**Rationale:** Option A provides the best balance of code reuse and progressive update quality. The key insight is that the existing `ChunkTranslationCoordinator` already has all the right pieces (retry, backoff, error classification, progress Flow) — the only thing that needs changing is the concurrency mechanism from `chunked/awaitAll` to `Semaphore/channelFlow`. This is a ~50-line refactor that transforms batch-delayed progress into immediate per-paragraph progress.

**Trade-offs:** We accept a minor semantic mismatch ("chunk" = 1 paragraph) in exchange for reusing ~80% of proven, tested code. The naming can be mitigated by adding a type alias or renaming `ChunkComplete` to a more generic `ItemComplete`.

**Alternative Consider:** Option B if the team decides the chunk abstraction is too confusing for maintainability, and is willing to invest the extra effort to create a clean-room coordinator. This is the "right" design but costs 2-3x the implementation effort.

---

## 4. Rate Limiting and Backpressure

### 4.1 Existing Protections

The codebase already has solid rate limiting infrastructure:

1. **Concurrency cap:** `MAX_CONCURRENCY = 5` at `ChunkTranslationCoordinator.kt:240`
2. **Exponential backoff:** `2^attempt` seconds at `ChunkTranslationCoordinator.kt:186`
3. **Retryable error detection:** Rate limit, timeout, 5xx errors at `ChunkTranslationCoordinator.kt:204`
4. **Per-request timeout:** `repository.translationTimeout` applied at `AIApi.kt:135`

### 4.2 Per-Paragraph Considerations

Moving from chunks (3-5 API calls for 20 paragraphs) to per-paragraph (20 API calls) increases total API calls by ~4-6x. Key mitigations:

| Risk | Mitigation | Already Exists? |
|---|---|---|
| Rate limit exceeded | Semaphore(3) limits concurrent calls | Yes (concurrency param) |
| Too many total requests | Bounded by concurrency, natural pacing | Yes |
| Provider-specific limits | Exponential backoff on 429/rate-limit | Yes (`isRetryableError`) |
| Timeout cascade | Per-request timeout prevents pile-up | Yes (`translationTimeout`) |
| Cost increase | More calls but same total tokens | N/A (same content) |

### 4.3 Additional Recommendation

Consider adding a **small jitter** to the backoff delay to avoid thundering herd when multiple paragraphs fail simultaneously:

```kotlin
val jitter = Random.nextLong(0, 500) // 0-500ms jitter
delay(backoffDelaySeconds.seconds + jitter.milliseconds)
```

This is a minor enhancement, not blocking.

### 4.4 Short Article Optimization (AC-9)

For articles with 1-2 paragraphs (total < 2000 chars), the existing `translateWithProgress()` at `AIApi.kt:203` already has a fast path that uses single-request `translate()`. This optimization should be preserved: creating 20 individual API calls for a 2-paragraph article is wasteful.

**Threshold recommendation:** If `paragraphs.size <= 2 && totalChars < 2000`, use single-request path. Otherwise, use per-paragraph parallel path.

---

## 5. Coordinator Approach Options (Detailed)

Per the mandatory option presentation requirement, here are 5 specific implementation approaches for the coordinator:

### Option 1: Refactor ChunkTranslationCoordinator (In-Place Semaphore)

**Description:** Replace the `chunked/awaitAll` loop in `ChunkTranslationCoordinator.translateChunks()` with `channelFlow` + `Semaphore`. Keep everything else (retry, error handling, progress types).

```kotlin
fun translateChunks(chunks: List<TranslationChunk>, targetLanguage: TranslationLanguage): Flow<TranslationProgress> = channelFlow {
    val semaphore = Semaphore(concurrency)
    send(TranslationProgress.Starting(totalChunks = chunks.size))
    val results = ConcurrentHashMap<Int, ChunkTranslationResult>()
    coroutineScope {
        chunks.forEach { chunk ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    val result = translateChunkWithRetry(chunk, targetLanguage)
                    results[chunk.id] = result
                    send(TranslationProgress.ChunkComplete(
                        current = results.size, total = chunks.size, result = result
                    ))
                }
            }
        }
    }
    // Emit final state...
}
```

**Strengths:** Minimal diff, reuses all existing code, immediate per-item progress
**Weaknesses:** Still uses "chunk" naming for single paragraphs
**Best For:** This project — fastest path to working per-paragraph translation

### Option 2: Extract Shared Logic + New ParagraphCoordinator

**Description:** Extract retry/backoff/error-classification into a shared `TranslationRetryPolicy` class. Create `ParagraphTranslationCoordinator` that uses it with Semaphore pattern. Keep `ChunkTranslationCoordinator` for backwards compatibility.

**Strengths:** Clean separation, no semantic mismatch, DRY via shared policy
**Weaknesses:** More files, more abstraction layers, higher implementation effort
**Best For:** If the team plans to maintain both chunk and paragraph paths long-term

### Option 3: Generic TranslationCoordinator<T>

**Description:** Generalize the coordinator to work with any translation unit type via a generic parameter and a translation function.

```kotlin
class TranslationCoordinator<T>(
    private val translateFn: suspend (T) -> TranslationResult,
    private val concurrency: Int = 3,
    private val maxRetries: Int = 3,
)
```

**Strengths:** Ultimate reuse — works for chunks, paragraphs, or any future unit
**Weaknesses:** Over-engineering for current needs, harder to understand
**Best For:** If the translation system will support multiple granularity levels

### Option 4: AIApi-Level Paragraph Translation Method

**Description:** Add `AIApi.translateParagraphsWithProgress()` that handles all the orchestration internally, similar to existing `translateWithProgress()` but at paragraph granularity.

**Strengths:** Clean API surface for ViewModel — just call one method
**Weaknesses:** Buries concurrency logic in AIApi (already 280 lines), harder to test
**Best For:** If the team wants ViewModel to remain completely unaware of concurrency details

### Option 5: ViewModel-Level Orchestration

**Description:** Move parallel execution into ArticleViewModel using `viewModelScope`, managing paragraph states directly via `MutableStateFlow`.

**Strengths:** ViewModel has full control over progress state, simplest data flow
**Weaknesses:** Bloats ViewModel (already 800+ lines), mixes concerns, harder to test
**Best For:** Quick prototype or very simple translation needs

### Coordinator Approach Comparison

| Criteria | 1: In-Place Refactor | 2: Shared Policy | 3: Generic<T> | 4: AIApi Method | 5: ViewModel |
|---|---|---|---|---|---|
| Implementation Effort | Low | Medium | Medium-High | Medium | Low |
| Code Reuse | High | High | Very High | Medium | Low |
| Testability | Good | Excellent | Good | Acceptable | Poor |
| Semantic Clarity | Acceptable | Good | Good | Good | Poor |
| Progressive Updates | Excellent | Excellent | Excellent | Excellent | Excellent |
| Complexity | Low | Medium | High | Medium | Low |
| Codebase Consistency | High | Medium | Low | High | Low |

### Final Recommendation

**Recommended: Option 1 — In-Place Refactor of ChunkTranslationCoordinator**

**Rationale:** This option delivers the best ROI. A ~50-line change to the concurrency mechanism (batch → Semaphore) in the existing coordinator, combined with creating single-paragraph chunks, gives us immediate per-paragraph progress with minimal risk. The naming mismatch ("chunk" for single paragraph) is cosmetic and can be addressed later via rename refactoring if desired.

**Implementation sequence:**
1. Add `ParagraphTranslation` and `ArticleTranslation` data classes (user-confirmed model)
2. Refactor `ChunkTranslationCoordinator.translateChunks()` to use `channelFlow` + `Semaphore`
3. Enhance `TranslationState` to wrap `ArticleTranslation` with `Translating` state
4. Wire `ArticleViewModel.translate()` to create `ArticleTranslation` from extracted paragraphs, then collect coordinator flow to update per-paragraph state
5. Update UI to display progressive per-paragraph results from `ArticleTranslation.contents`

---

## Deprecation Warnings

None identified. All referenced APIs (`Semaphore`, `channelFlow`, `StateFlow`, `withPermit`) are stable in kotlinx.coroutines 1.7+.

---

## Edge Cases

### Known Limitations
1. **Very short paragraphs** (< 10 chars): Heading-only paragraphs may produce inconsistent translations when sent individually vs. in context. Consider grouping headings with their following paragraph.
2. **Very long single paragraphs** (> 4000 chars): A single paragraph exceeding token limits will still fail. The existing per-request timeout handles this gracefully.

### Edge Cases to Handle
1. **Empty paragraph after extraction**: Skip, don't send to API
2. **All paragraphs fail**: Transition to error state with first error message (existing pattern)
3. **Partial failure**: Show translated paragraphs + original text for failed ones (FR-7)
4. **Rapid cancellation**: User taps translate then immediately navigates away — ensure `coroutineScope` cancellation propagates to all in-flight requests

### Security Considerations
- No new security concerns. API keys are already managed via `AISettings`. Per-paragraph calls use the same authentication path.

---

## Sources

### Primary Sources

| # | Title | URL | Published | Freshness |
|---|-------|-----|-----------|-----------|
| 1 | Kotlin Semaphore API docs | kotlinlang.org/api/kotlinx.coroutines | Stable | Fresh |
| 2 | limitedParallelism vs Semaphore | medium.com/@sdevpremthakur | 2025-10 | Fresh |
| 3 | Semaphores in Kotlin Coroutines | medium.com/@khorassani64 | 2025-03 | Fresh |
| 4 | Hidden pitfalls of flatMapMerge | blog.kotlin-academy.com | 2025-06 | Fresh |
| 5 | Leveraging Semaphore in Coroutines | blog.shreyaspatil.dev | 2023 | Current |
| 6 | Single vs Multiple StateFlows | medium.com/@dnyaneshwar.patil | 2025-10 | Fresh |
| 7 | Stabilizing LazyColumn Recomposition | davideagostini.com | 2026-02 | Fresh |
| 8 | Parallel API Calls with Coroutines | levelup.gitconnected.com | 2025-11 | Fresh |
| 9 | Async LLM Patterns: Rate Limiting | dataa.dev | 2024-08 | Current |
| 10 | Parallel flow processing #1147 | github.com/Kotlin/kotlinx.coroutines | 2019-ongoing | Current |
| 11 | API Rate Limits Best Practices 2025 | orq.ai/blog | 2025-02 | Fresh |
| 12 | Rate Limiting using Kotlin Flow | sumit.me | 2023 | Dated |

### Source Freshness Summary
- Fresh (< 6 months): 8 sources
- Current (6-12 months): 3 sources
- Dated (1-2 years): 1 source
- Potentially Outdated (> 2 years): 0 sources
