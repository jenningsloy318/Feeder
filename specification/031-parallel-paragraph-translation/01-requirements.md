# Requirements: Parallel Per-Paragraph Translation

**Date:** 2026-03-15
**Type:** Improvement
**Priority:** High
**Spec:** 031

---

## 1. Problem Statement

When translating long articles, the current implementation sends **all** paragraphs to the LLM in a single API request (`AIApi.translate()` at `AIApi.kt:125`). This causes:

1. **Timeouts**: Long articles exceed LLM provider response time limits
2. **Truncated responses**: LLM output token limits cut off translations mid-article
3. **No progress feedback**: `TranslationState` has only `Empty | Loading | Result` — users see a spinner with no indication of progress or partial results
4. **All-or-nothing failure**: If the single request fails, the entire translation is lost with no partial recovery

### Existing Infrastructure (spec-30)

Spec-30 added chunk-based translation (`translateWithProgress()` at `AIApi.kt:178`) with:
- `TranslationChunker`: groups paragraphs into ~2000-char chunks
- `ChunkTranslationCoordinator`: parallel chunk translation with retry
- `TranslationProgress`: progress states (Starting, Translating, ChunkComplete, Complete, Error)

**However**, this infrastructure is:
- **Not wired** to the ViewModel — `translate()` at `ArticleViewModel.kt:491` still calls the old single-request `AIApi.translate()`
- **Chunk-granularity**, not paragraph-granularity — each chunk contains multiple paragraphs, so individual paragraph status is not tracked

### Key Gap

The user wants **per-paragraph** translation units where each paragraph is independently translated, tracked, and displayed as it completes. This is finer granularity than the existing chunk-based approach.

---

## 2. Functional Requirements

### FR-1: Article Pre-processing into Paragraph Translation Units

The system SHALL pre-process article content into a structured collection of paragraph translation units before sending to the LLM.

Each unit SHALL contain:
- `index`: Sequential integer identifier (1-based)
- `text`: Original paragraph text (from existing `TranslatableText`)
- `translation`: Translated text (initially empty string)
- `translated`: Translation status flag (0 = pending, 1 = completed)

The collection SHALL track overall status: `initial` → `translating` → `translated`.

**Leverages:** `extractTranslatableParagraphs()` at `ArticleViewModel.kt:548` already extracts paragraphs with structure metadata.

### FR-2: Per-Paragraph Parallel Translation

The system SHALL translate each paragraph as an individual translation unit, sending one paragraph per LLM API call.

Multiple paragraphs SHALL be translated in parallel using controlled concurrency (configurable, default: 3 concurrent requests).

**Leverages:** `ChunkTranslationCoordinator` pattern for parallel execution with concurrency control, retry logic, and exponential backoff.

### FR-3: Per-Paragraph Status Tracking

Each paragraph's `translation` and `translated` fields SHALL be updated as its individual translation completes.

The overall collection `status` SHALL transition:
- `initial` → `translating` when the first paragraph translation begins
- `translating` → `translated` when ALL paragraphs have `translated = 1`

### FR-4: Progressive UI Updates

The UI SHALL display translation results incrementally as each paragraph completes, rather than waiting for all paragraphs to finish.

The `TranslationState` in the ViewModel SHALL be enhanced to support per-paragraph progress, including:
- Total paragraph count
- Number of paragraphs completed
- Individual paragraph translation results as they arrive
- Overall progress percentage

### FR-5: Wire translateWithProgress to ViewModel

The `translate()` method in `ArticleViewModel.kt:491` SHALL be updated to use the progress-aware translation path instead of the single-request `AIApi.translate()`.

The ViewModel SHALL collect `TranslationProgress` emissions and update `TranslationState` accordingly.

### FR-6: Retry Per-Paragraph on Failure

If a paragraph translation fails, the system SHALL retry with exponential backoff (reuse existing `translateChunkWithRetry` pattern from `ChunkTranslationCoordinator.kt:167`).

Failed paragraphs SHALL NOT block the translation of other paragraphs.

### FR-7: Partial Result Display

If some paragraphs fail after retries, the system SHALL still display successfully translated paragraphs. Failed paragraphs SHALL show an error indicator or the original text.

---

## 3. Non-Functional Requirements

### NFR-1: Performance

- Per-paragraph API calls SHALL use the configured translation timeout per request (from `repository.translationTimeout`)
- Default concurrency SHALL be 3 parallel requests (matching `ChunkTranslationCoordinator.DEFAULT_CONCURRENCY`)
- Total translation time for a 20-paragraph article SHALL be roughly `ceil(20/3) * avg_paragraph_time` rather than `single_large_request_time`

### NFR-2: Memory Efficiency

- The paragraph translation state SHALL use a StateFlow that only emits changed paragraphs, not the entire collection on every update
- Translation results SHALL be stored in a structure that allows efficient index-based lookup

### NFR-3: Cancellation Support

- Translation SHALL be cancellable via coroutine scope cancellation (existing `viewModelScope` pattern)
- Cancelling SHALL stop all in-flight paragraph translation requests

### NFR-4: Backwards Compatibility

- The existing `AIClient.translate()` and `AIClient.translateChunk()` interfaces SHALL NOT be modified
- The per-paragraph approach SHALL reuse the existing `translateChunk()` method by creating single-paragraph chunks

### NFR-5: API Rate Limit Awareness

- Concurrency level SHALL be bounded (max 5, matching `ChunkTranslationCoordinator.MAX_CONCURRENCY`)
- Exponential backoff on rate limit errors SHALL be preserved from existing retry logic

---

## 4. Acceptance Criteria

### AC-1: Pre-processing
- GIVEN an article with N paragraphs
- WHEN the user taps translate
- THEN the system creates N paragraph translation units with sequential indices, empty translations, and `translated = 0`

### AC-2: Parallel Execution
- GIVEN an article with 10 paragraphs and concurrency = 3
- WHEN translation begins
- THEN at most 3 paragraphs are being translated simultaneously at any time

### AC-3: Progressive Display
- GIVEN a translation in progress
- WHEN paragraph 3 of 10 completes translation
- THEN the UI shows the translated text for paragraph 3 immediately, while paragraphs 1-2 (if done) are also shown, and paragraphs 4-10 show loading/pending state

### AC-4: Progress Indication
- GIVEN a translation in progress with 4 of 10 paragraphs complete
- THEN the UI displays progress (e.g., "4/10 paragraphs translated" or a progress bar at 40%)

### AC-5: Full Completion
- GIVEN all paragraphs have completed translation
- THEN the overall status transitions to `translated` and the UI shows all translated paragraphs inline with the original content

### AC-6: Partial Failure
- GIVEN 8 of 10 paragraphs translated successfully and 2 failed after retries
- THEN the 8 successful translations are displayed, and the 2 failed paragraphs show an error state or original text

### AC-7: Retry Behavior
- GIVEN a paragraph translation fails with a retryable error (timeout, 5xx)
- THEN the system retries up to 3 times with exponential backoff before marking it as failed

### AC-8: Cancellation
- GIVEN a translation in progress
- WHEN the user navigates away or cancels
- THEN all in-flight requests are cancelled and no further paragraphs are sent for translation

### AC-9: Short Article Optimization
- GIVEN an article with 1-2 short paragraphs (total < 2000 characters)
- WHEN the user taps translate
- THEN the system MAY use a single API call for efficiency (optimization, not requirement)

---

## 5. Out of Scope

1. **UI redesign of translation display** — The existing inline paragraph display pattern is preserved; only progress/status indicators are added
2. **User-configurable concurrency** — Concurrency is developer-configurable, not exposed in settings UI
3. **Persistent translation cache** — Translations are not persisted to database in this spec
4. **Streaming/SSE responses** — Individual paragraph calls are request/response, not streaming
5. **Retry UI** — No manual per-paragraph retry button; automatic retry only
6. **Translation of non-text elements** — Images, code blocks, etc. remain untranslated (existing behavior)

---

## 6. Dependencies on Existing Code

| Component | File | Role | Change Needed |
|-----------|------|------|---------------|
| `ArticleViewModel.translate()` | `ArticleViewModel.kt:491` | Entry point | **Major**: Wire to per-paragraph flow |
| `TranslationState` | `ArticleViewModel.kt:761` | UI state | **Major**: Add per-paragraph progress states |
| `extractTranslatableParagraphs()` | `ArticleViewModel.kt:548` | Paragraph extraction | **None**: Reuse as-is |
| `AIApi.translateWithProgress()` | `AIApi.kt:178` | Progress-aware translation | **Moderate**: Adapt for per-paragraph (not chunk) granularity |
| `ChunkTranslationCoordinator` | `ChunkTranslationCoordinator.kt` | Parallel execution | **Moderate**: Adapt or create per-paragraph coordinator |
| `TranslationChunker` | `TranslationChunker.kt` | Chunk creation | **Minor**: Create single-paragraph chunks, or bypass |
| `TranslationProgress` | `TranslationProgress.kt` | Progress states | **Minor**: May need per-paragraph progress variant |
| `AIClient.translateChunk()` | `AIClient.kt:118` | Single chunk translation | **None**: Reuse with single-paragraph chunks |
| `TranslatableText` | `TranslatableText.kt` | Paragraph model | **None**: Reuse as-is |
| `TranslationChunk` | `TranslationChunk.kt` | Chunk model | **None**: Reuse with 1 paragraph per chunk |
| `ArticleScreen` (TranslationStatusSection) | `ArticleScreen.kt:695` | UI rendering | **Moderate**: Display per-paragraph progress |

---

## 7. Data Model

### ParagraphTranslationUnit (New)

```
index: Int              // 1-based sequential identifier
text: String            // Original paragraph text
elementType: ElementType // From TranslatableText
nestingLevel: Int       // From TranslatableText
translation: String     // Translated text (empty until done)
translated: Int         // 0 = pending, 1 = completed, -1 = failed
```

### ArticleTranslationState (New or Enhanced TranslationState)

```
status: String          // "initial" | "translating" | "translated" | "error"
paragraphs: List<ParagraphTranslationUnit>
completedCount: Int     // Derived: count of translated == 1
totalCount: Int         // Total paragraphs
errorCount: Int         // Derived: count of translated == -1
```

---

## 8. Architecture Approach Options

### Option A: Adapt Existing Chunk Infrastructure (Recommended)

Create single-paragraph `TranslationChunk` instances (1 paragraph per chunk) and feed them through the existing `ChunkTranslationCoordinator`. Enhance `TranslationState` to expose per-paragraph progress from `TranslationProgress` emissions.

- **Pros**: Minimal new code, reuses proven retry/concurrency logic
- **Cons**: Slight overhead from chunk wrapper around single paragraphs

### Option B: New Per-Paragraph Coordinator

Create a dedicated `ParagraphTranslationCoordinator` that translates individual `TranslatableText` items directly via `AIClient.translate()` (with a single-element list).

- **Pros**: Cleaner API, no chunk abstraction for single paragraphs
- **Cons**: Duplicates concurrency/retry logic from ChunkTranslationCoordinator

### Option C: Enhance Existing Chunker with Paragraph Mode

Add a mode to `TranslationChunker` that creates 1-paragraph-per-chunk splits, then use existing coordinator unchanged.

- **Pros**: Single line change to enable per-paragraph behavior
- **Cons**: Semantic mismatch (a "chunk" that is always 1 paragraph)
