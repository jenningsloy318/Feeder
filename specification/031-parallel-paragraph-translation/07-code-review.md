# Code Review: Parallel Per-Paragraph Translation (Spec-31)

**Date:** 2026-03-15
**Reviewer:** super-dev:code-reviewer
**Status:** Approved with Comments
**Base SHA:** 362b11b0 (master)
**Head SHA:** spec-31-parallel-paragraph-translation (working tree)

## Summary Statistics

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 1 |
| Medium | 3 |
| Low | 2 |
| Info | 1 |

| Dimension | Issues |
|-----------|--------|
| Correctness | 2 |
| Security | 0 |
| Performance | 0 |
| Maintainability | 3 |
| Testability | 0 |
| Error Handling | 1 |
| Consistency | 1 |
| Accessibility | 0 |

## Specification Validation

| Criterion | Status | Evidence |
|-----------|--------|----------|
| AC-1: Per-paragraph data model matches user-confirmed JSON | Met | `ParagraphTranslation.kt:6-11` - fields `index`, `text`, `translation`, `translated` match spec exactly |
| AC-2: Semaphore + channelFlow concurrency pattern | Met | `ParagraphTranslationCoordinator.kt:35-50` - `channelFlow` with `Semaphore` and `coroutineScope` |
| AC-3: Per-paragraph progress tracking | Met | `ArticleTranslation.kt:10-18` - `paragraphCompletedCount`, `paragraphFailedCount`, `paragraphTotalCount` |
| AC-4: UI progressive display with progress bar | Met | `ArticleScreen.kt:699-721` - `TranslationStatusSection` with `LinearProgressIndicator` |
| AC-5: Retry with exponential backoff (2^attempt seconds) | Met | `ParagraphTranslationCoordinator.kt:57-98` - retry loop with `2.0.pow(attempt)` delay |
| AC-6: Cancellation via structured concurrency | Met | `ParagraphTranslationCoordinator.kt:37` - `coroutineScope` ensures cancellation propagation |
| AC-7: Dead code cleanup (4 files, 3 methods) | Partial | Files deleted, `translateChunk()` removed. See F-003 for remaining dead code in providers |
| AC-8: Unit tests for coordinator and data model | Met | `ArticleTranslationTest.kt` (10 tests), `ParagraphTranslationCoordinatorTest.kt` (9 tests) |
| AC-9: TranslationState sealed interface with 4 variants | Met | `ArticleViewModel.kt:821-839` - `Empty`, `Translating`, `Translated`, `Error` |
| AC-10: Button disabled during translation | Met | `ArticleScreen.kt:286-289` - `enabled = !isTranslationInProgress` |

### Non-Goals Check
- [x] NG-1: UI redesign of translation display layout - Not implemented (correct)
- [x] NG-2: User-configurable concurrency exposed in settings UI - Not implemented (correct)
- [x] NG-3: Persistent translation cache - Not implemented (correct)
- [x] NG-4: Streaming/SSE responses - Not implemented (correct)
- [x] NG-5: Manual per-paragraph retry button - Not implemented (correct)
- [x] NG-6: Translation of non-text elements - Not implemented (correct)
- [x] NG-7: Cross-paragraph context in prompts - Not implemented (correct)

## Findings

### High

**F-001** | Correctness / Error Handling | `ParagraphTranslationCoordinator.kt:108`
**Issue:** The `isRetryableError()` check `errorMessage.contains("5")` is overly broad. This matches ANY error message containing the digit "5", not just 5xx HTTP status codes. Examples of false positives: "Expected 5 paragraphs, got 3", "Retry after 5 seconds", "HTTP 405 Method Not Allowed", "Error at position 15". This could cause non-retryable errors to be retried 3 times with exponential backoff, wasting up to 7 seconds before failing.
**Suggestion:** Replace with a regex targeting 5xx codes: `Regex("\\b5\\d{2}\\b").containsMatchIn(errorMessage)` or use `errorMessage.contains("500") || errorMessage.contains("502") || errorMessage.contains("503") || errorMessage.contains("504")`. Note: The same issue exists in both providers at `OpenAICompatibleClient.kt:789` and `AnthropicClient.kt:716`, but those are pre-existing code (also now dead code per F-003).
**Rationale:** Incorrect error classification can delay failure reporting and waste API calls on non-retryable errors.

### Medium

**F-002** | Correctness | `ParagraphTranslationCoordinator.kt:27-31`
**Issue:** Missing input validation in constructor. Spec section 7.1 (Security/Input Validation) explicitly requires: `require(paragraphConcurrency > 0)` and `require(paragraphMaxRetries >= 0)`, with `paragraphConcurrency` clamped to `MAX_PARAGRAPH_CONCURRENCY`. The implementation has no `init` block or validation.
**Suggestion:** Add an `init` block:
```kotlin
init {
    require(paragraphConcurrency in 1..MAX_PARAGRAPH_CONCURRENCY) {
        "paragraphConcurrency must be between 1 and $MAX_PARAGRAPH_CONCURRENCY"
    }
    require(paragraphMaxRetries >= 0) {
        "paragraphMaxRetries must be non-negative"
    }
}
```
**Rationale:** Without validation, a `Semaphore(0)` would deadlock all translations and a negative `paragraphMaxRetries` with `repeat()` would skip all retries silently.

**F-003** | Maintainability | `OpenAICompatibleClient.kt:783-794`, `AnthropicClient.kt:710-721`
**Issue:** Dead code remaining after spec-30 cleanup. Both `isRetryableError()` private methods in the provider implementations are now unreferenced since `translateChunk()` was removed. Spec section 4.5 requires verifying "no remaining references to deleted class/file methods."
**Suggestion:** Delete `isRetryableError()` from both `OpenAICompatibleClient.kt:770-794` and `AnthropicClient.kt:697-721` (including KDoc). The retry logic now lives in `ParagraphTranslationCoordinator.isRetryableError()`.
**Rationale:** Dead code confuses future maintainers and the stale KDoc still references "chunk translation."

**F-004** | Maintainability | `MarkdownToAnnotatedStringTest.kt` (deleted)
**Issue:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedStringTest.kt` (238 lines) is deleted in the working tree but is not mentioned in the spec's dead code cleanup section (section 4). This test file appears unrelated to the paragraph translation feature.
**Suggestion:** Restore this file with `git restore app/src/test/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedStringTest.kt` unless there is a deliberate reason for its removal outside this spec's scope.
**Rationale:** Unintended test deletion reduces test coverage and is not part of the spec requirements.

### Low

**F-005** | Consistency | `ArticleScreen.kt:295`
**Issue:** Hardcoded English string "Translating article, please wait" used for content description instead of using `stringResource()` like all other content descriptions in the file.
**Suggestion:** Add a string resource entry and use `stringResource(R.string.translating_article_content_description)`.
**Rationale:** Breaks the localization pattern used throughout the file (e.g., lines 264, 277, 297).

**F-006** | Maintainability | `OpenAICompatibleClient.kt:771-772`, `AnthropicClient.kt:697-698`
**Issue:** KDoc for `isRetryableError()` in both providers references "chunk translation" which is a stale reference to the removed spec-30 infrastructure. (Subsumed by F-003 if those methods are deleted.)
**Suggestion:** If methods are kept for any reason, update KDoc to remove "chunk" references.
**Rationale:** Stale documentation misleads maintainers.

### Info

**F-007** | Correctness | `ArticleScreen.kt:701-703`
**Issue:** Progress indicator shows `completedCount/totalCount` where `completedCount` only counts `translated == 1` (success). Failed paragraphs (`translated == -1`) are not reflected in the numerator. For a 10-paragraph article where 8 succeed and 2 fail, the progress shows "8/10 paragraphs translated" during the `Translating` state, and the final `Translated` state still shows the error section with "2 paragraph(s) failed." This is technically correct per spec, but the progress bar fills to 80% and then the state transitions. Users may briefly see a "stuck" progress bar.
**Note:** No action needed. This is consistent with the spec design and the error section in `Translated` state handles the communication. A future enhancement could show `(completed + failed)/total` for progress tracking.

## Strengths

- **Exact spec compliance in data model**: `ParagraphTranslation.kt` and `ArticleTranslation.kt` match the user-confirmed JSON structure precisely with correct field names and types.
- **Clean concurrency implementation**: `channelFlow` + `Semaphore` + `coroutineScope` pattern at `ParagraphTranslationCoordinator.kt:35-50` is idiomatic Kotlin coroutines and correctly handles cancellation propagation.
- **Atomic state updates**: `translationState.update{}` at `ArticleViewModel.kt:537` uses CAS-loop for thread-safe per-paragraph state mutations.
- **Comprehensive test coverage**: 19 tests covering concurrency limits, retry behavior, error classification, edge cases (empty input, single paragraph), and all data model computed properties.
- **Clean dead code removal**: All 4 spec-30 files deleted, `translateChunk()` removed from both providers and `AIClient` interface, no stale imports remaining.
- **Progressive UI**: Translation progress bar with paragraph counts (`ArticleScreen.kt:699-721`) provides real-time feedback as specified.

## Recommendations

- Consider adding a test for `translateParagraphs` with mixed success/failure results to verify partial completion behavior end-to-end.
- The `MockAIClient` in test could be extracted to a shared test utility if future specs add more coordinator/client tests.
- Future consideration: for articles with 1-2 short paragraphs, a single API call would be more efficient (acknowledged as open question in spec section 11).

## Verdict

**Approved with Comments**

**Reasoning:** The implementation faithfully follows the spec across all major areas: data model, concurrency pattern, state management, UI updates, and dead code cleanup. The `channelFlow` + `Semaphore` pattern is correctly implemented with proper structured concurrency. All 10 acceptance criteria are met (one partially due to leftover dead code in providers). The 1 High finding (overly broad error classification) does not block approval because it matches the spec's own code exactly, but should be fixed to prevent misclassification in production. The 3 Medium findings are straightforward fixes.

**Blocking Issues:** None
