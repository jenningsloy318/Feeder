# Task List: Parallel Per-Paragraph Translation

**Plan:** `./05-implementation-plan.md`
**Total Tasks:** 20

## Tasks

### Milestone 1: Data Model & Coordinator

- [ ] **T1.1** Create `ParagraphTranslation` data class
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslation.kt`
  - **Details:** `@Serializable data class ParagraphTranslation(val index: Int, val text: String, val translation: String, val translated: Int)` matching user-confirmed JSON structure. 1-based index, `translated`: 0=pending, 1=completed, -1=failed.
  - **Acceptance:** Class compiles, is serializable, fields match spec exactly

- [ ] **T1.2** Create `ArticleTranslation` data class with helper properties
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt`
  - **Details:** `@Serializable data class ArticleTranslation(val contents: List<ParagraphTranslation>, val status: String)` with computed properties: `paragraphCompletedCount`, `paragraphFailedCount`, `paragraphTotalCount`, `isAllCompleted`, `buildTranslatedParagraphsList()`. Status values: `"initial"`, `"translating"`, `"translated"`.
  - **Acceptance:** Class compiles, computed properties return correct values, `buildTranslatedParagraphsList()` returns null for pending and text for completed

- [ ] **T1.3** Create `ParagraphTranslationProgress` sealed interface
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`
  - **Details:** Sealed interface with two variants: `ParagraphComplete(paragraphIndex: Int, translatedText: String)` and `ParagraphFailed(paragraphIndex: Int, errorMessage: String)`. `paragraphIndex` is 1-based.
  - **Acceptance:** Sealed interface compiles, both variants have correct fields

- [ ] **T1.4** Create `ParagraphTranslationCoordinator` class with `translateParagraphs()` method
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`
  - **Details:** Constructor takes `aiClient: AIClient`, `paragraphConcurrency: Int = DEFAULT_PARAGRAPH_CONCURRENCY`, `paragraphMaxRetries: Int = DEFAULT_PARAGRAPH_MAX_RETRIES`. `translateParagraphs()` uses `channelFlow + Semaphore(paragraphConcurrency)` to launch one coroutine per paragraph via `launch(Dispatchers.IO)` + `semaphore.withPermit`. Each coroutine calls `translateParagraphWithRetry()` and `send()`s the result immediately. Constants: `DEFAULT_PARAGRAPH_CONCURRENCY=3`, `MAX_PARAGRAPH_CONCURRENCY=5`, `DEFAULT_PARAGRAPH_MAX_RETRIES=3`.
  - **Acceptance:** Coordinator limits concurrent calls to `paragraphConcurrency`, emits results immediately per-paragraph (not batch), handles empty input

- [ ] **T1.5** Implement `translateParagraphWithRetry()` private method
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`
  - **Details:** Calls `aiClient.translate(listOf(paragraphText), targetLanguage)` for single-paragraph translation. On success, extracts `result.paragraphs.firstOrNull()` and returns `ParagraphComplete`. On retryable failure, waits `2^attempt` seconds and retries. On non-retryable failure or max retries exceeded, returns `ParagraphFailed`. For `TranslationResult.Error`, treat as retryable if not last attempt.
  - **Acceptance:** Retries up to `paragraphMaxRetries` times with exponential backoff, fails immediately for non-retryable errors

- [ ] **T1.6** Implement `isRetryableError()` private method
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`
  - **Details:** Same logic as `ChunkTranslationCoordinator.isRetryableError()`: retryable for SocketTimeoutException, "timeout", "rate limit", "server error", "5"; non-retryable for "invalid api key", "quota exceeded", "insufficient quota".
  - **Acceptance:** Returns true for timeout/rate-limit/5xx, false for auth/quota errors

### Milestone 2: ViewModel Integration

- [ ] **T2.1** Replace `TranslationState` sealed interface in `ArticleViewModel.kt`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:** Replace the existing `TranslationState` (lines 761-772) with: `Empty` (data object), `Translating(articleTranslation: ArticleTranslation)` (data class), `Translated(articleTranslation: ArticleTranslation)` (data class), `Error(errorMessage: String)` (data class). Remove old `Loading` and `Result` variants.
  - **Acceptance:** New sealed interface compiles, old `Loading`/`Result` references updated

- [ ] **T2.2** Add `createSettingsWithTimeout()` helper to `ArticleViewModel`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:** Private function that takes `translationTimeoutSeconds: Int` and returns `AISettings` with updated timeout. Handles both `AISettings.OpenAI` and `AISettings.Anthropic`. Extracts existing pattern from `AIApi.translate()`.
  - **Acceptance:** Returns correct `AISettings` subtype with updated timeout

- [ ] **T2.3** Rewrite `translate()` method in `ArticleViewModel`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:** Replace existing `translate()` (lines 491-527) with: (1) extract paragraphs via `extractTranslatableParagraphs()`, (2) build `ArticleTranslation` with `status="translating"`, (3) set `translationState.value = TranslationState.Translating(initialArticleTranslation)`, (4) get `targetLanguage` and `translationTimeout` from repository, (5) create `ParagraphTranslationCoordinator` with `AIClient.create(settingsWithTimeout)`, (6) collect `translateParagraphs()` flow, (7) on each progress event: `translationState.update{}` to update specific paragraph atomically, (8) when `isAllCompleted`: transition to `Translated` state. Empty article returns `TranslationState.Error`. Catch block sets `TranslationState.Error`.
  - **Acceptance:** State transitions: Empty -> Translating -> (per-paragraph updates) -> Translated. Atomic updates via `StateFlow.update{}`. Cancellation via `viewModelScope`.

- [ ] **T2.4** Add required imports to `ArticleViewModel`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:** Add imports for `ParagraphTranslation`, `ArticleTranslation`, `ParagraphTranslationCoordinator`, `ParagraphTranslationProgress`, `AIClient`, `AISettings`. Remove any now-unused imports referencing old `TranslationState.Loading`/`Result` or `AIClient.TranslationResult`.
  - **Acceptance:** All imports resolve, no unused import warnings

### Milestone 3: UI Updates

- [ ] **T3.1** Update `TranslationStatusSection` composable
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
  - **Details:** Replace existing `TranslationStatusSection` (lines 695-727) to handle new `TranslationState` variants: `Empty` -> nothing, `Translating` -> `OutlinedCard` with determinate `LinearProgressIndicator(progress = { completedCount.toFloat() / totalCount })` and text "$completedCount/$totalCount paragraphs translated", `Translated` -> show `TranslationErrorSection` if `paragraphFailedCount > 0`, `Error` -> show `TranslationErrorSection(errorMessage)`.
  - **Acceptance:** Determinate progress bar during translation, failure count on completion, error message on fatal error

- [ ] **T3.2** Update translate button disabled state
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
  - **Details:** Change line 287 from `val isLoading = viewState.translation is TranslationState.Loading` to `val isTranslationInProgress = viewState.translation is TranslationState.Translating`. Update `enabled = !isTranslationInProgress`. Update `contentDescription` to check `isTranslationInProgress`.
  - **Acceptance:** Button disabled during `Translating`, enabled for `Empty`/`Translated`/`Error`

- [ ] **T3.3** Update `translatedParagraphs` extraction for inline display
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
  - **Details:** Replace existing `translatedParagraphs` extraction (lines 555-563) with: for `Translating` and `Translated` states, call `articleTranslation.buildTranslatedParagraphsList().map { it ?: "" }`. For `Empty` and `Error`, return `null`. Empty string for pending paragraphs shows original text (existing behavior).
  - **Acceptance:** Completed paragraphs show translated text inline, pending paragraphs show original text, null means no translation (existing behavior)

- [ ] **T3.4** Update `TranslationStatusSection` visibility check
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
  - **Details:** Line 543 check `if (viewState.translation !is TranslationState.Empty)` remains correct — no change needed. Verify this still compiles with new `TranslationState` type.
  - **Acceptance:** Status section shows for `Translating`, `Translated`, `Error`; hidden for `Empty`

### Milestone 4: Dead Code Cleanup & Testing

- [ ] **T4.1** Delete dead code files
  - **Files:** Delete `ChunkTranslationCoordinator.kt`, `TranslationChunker.kt`, `TranslationChunk.kt`, `TranslationProgress.kt`
  - **Details:** Delete all 4 files from `app/src/main/java/com/nononsenseapps/feeder/ai/`
  - **Acceptance:** Files no longer exist on disk

- [ ] **T4.2** Remove `translateWithProgress()` from `AIApi.kt`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`
  - **Details:** Remove the entire `translateWithProgress()` method (lines 178-266). Remove now-unused imports: `kotlinx.coroutines.CoroutineScope`, `kotlinx.coroutines.flow.Flow`. Keep `translate()` method (lines 125-156) for potential future use.
  - **Acceptance:** `AIApi.kt` compiles without `translateWithProgress()`

- [ ] **T4.3** Remove `translateChunk()` from `AIClient.kt`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`
  - **Details:** Remove `translateChunk()` method declaration (lines 107-121). Remove import of `ChunkTranslationResult` if present (it is defined in the now-deleted `TranslationProgress.kt`).
  - **Acceptance:** `AIClient` interface compiles without `translateChunk()`

- [ ] **T4.4** Remove `translateChunk()` from provider implementations
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`, `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`
  - **Details:** In `OpenAICompatibleClient.kt`: remove `translateChunk()` override (lines 354-420) and imports for `ChunkTranslationResult`, `TranslationChunk`. In `AnthropicClient.kt`: remove `translateChunk()` override (lines 281-330) and imports for `ChunkTranslationResult`, `TranslationChunk`.
  - **Acceptance:** Both provider classes compile without `translateChunk()` overrides

- [ ] **T4.5** Verify no remaining references to deleted classes
  - **Files:** All files in `app/src/main/java/`
  - **Details:** Grep for `ChunkTranslationCoordinator`, `TranslationChunker`, `TranslationChunk`, `TranslationProgress`, `ChunkTranslationResult`, `translateChunk`, `translateWithProgress`. Fix any remaining references.
  - **Acceptance:** Zero grep matches for any of these identifiers (except in spec files)

- [ ] **T4.6** Write unit tests for `ParagraphTranslationCoordinator`
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinatorTest.kt`
  - **Details:** Test concurrency limiting (mock AIClient, verify max concurrent calls), immediate emission, retry with backoff, error classification, empty input, cancellation. Use `runTest` and `TestDispatcher`.
  - **Acceptance:** All tests pass, coverage for happy path, retry, error, and cancellation scenarios

- [ ] **T4.7** Write unit tests for `ArticleTranslation` data model
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ai/ArticleTranslationTest.kt`
  - **Details:** Test `paragraphCompletedCount`, `paragraphFailedCount`, `paragraphTotalCount`, `isAllCompleted`, `buildTranslatedParagraphsList()` with various combinations of pending/completed/failed paragraphs.
  - **Acceptance:** All tests pass, coverage for all computed properties

### Final Tasks

- [ ] **TF.1** Run full build and fix any compilation errors
  - **Command:** `./gradlew assembleDebug`
  - **Acceptance:** Build succeeds with zero errors

- [ ] **TF.2** Run all existing tests and fix any failures
  - **Command:** `./gradlew test`
  - **Acceptance:** All tests pass (existing + new)

- [ ] **TF.3** Code review
  - **Agent:** `super-dev:code-reviewer`
  - **Acceptance:** No blocking issues

- [ ] **TF.4** Commit and push changes
  - **Message format:** Use `generating-commit-messages` skill
  - **Acceptance:** Changes committed and pushed to `spec-31-parallel-paragraph-translation` branch

## Task Dependencies

```
T1.1 ──┐
T1.2 ──┼──▶ T1.3 ──▶ T1.4 ──▶ T1.5 ──▶ T1.6
       │                                    │
       └────────────────────────────────────┘
                                            │
                     ┌──────────────────────┘
                     ▼
              T2.1 ──▶ T2.2 ──▶ T2.3 ──▶ T2.4
                                            │
                     ┌──────────────────────┘
                     ▼
              T3.1 ──┬──▶ T3.2
                     ├──▶ T3.3
                     └──▶ T3.4
                              │
                     ┌────────┘
                     ▼
              T4.1 ──┬──▶ T4.2 ──▶ T4.3 ──▶ T4.4 ──▶ T4.5
                     │
                     ├──▶ T4.6
                     └──▶ T4.7
                              │
                     ┌────────┘
                     ▼
              TF.1 ──▶ TF.2 ──▶ TF.3 ──▶ TF.4
```

## Priority Order

1. **T1.1, T1.2** — Data model foundation (can be created in parallel)
2. **T1.3** — Progress types needed by coordinator
3. **T1.4, T1.5, T1.6** — Coordinator implementation (sequential, build on each other)
4. **T2.1** — TranslationState change (prerequisite for all ViewModel/UI work)
5. **T2.2, T2.3, T2.4** — ViewModel integration (sequential)
6. **T3.1, T3.2, T3.3, T3.4** — UI updates (can be parallelized after T2)
7. **T4.1** — Delete dead files (after all new code is wired)
8. **T4.2, T4.3, T4.4** — Remove dead methods (sequential, depends on T4.1)
9. **T4.5** — Verify cleanup completeness
10. **T4.6, T4.7** — Write tests (can be parallelized with T4.1-T4.5)
11. **TF.1-TF.4** — Build, test, review, commit (sequential)
