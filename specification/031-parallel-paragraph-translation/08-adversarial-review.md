# Adversarial Review: Parallel Per-Paragraph Translation

**Date:** 2026-03-15
**Reviewer:** super-dev:adversarial-reviewer
**Verdict:** CONTESTED

## Intent

Replace the chunk-based translation system (spec-30) with per-paragraph parallel translation using `Semaphore + channelFlow`, providing immediate per-paragraph progress emission and progressive UI updates. Remove all dead code from the prior chunk infrastructure.

## Verdict Summary

One high-severity correctness bug in `isRetryableError` and one medium-severity retry logic gap prevent a PASS. Both are localized fixes that don't require architectural changes.

## Change Scope

| Metric | Value |
|--------|-------|
| Lines changed (new + modified) | ~739 |
| Lines deleted (dead code cleanup) | ~1121 |
| Files changed | 16 (5 new, 6 modified, 5 deleted) |
| Size classification | Large |
| Reviewers activated | Skeptic + Architect + Minimalist |
| Attack vectors applied | V1-V7 |

## Destructive Action Gate

**Gate Verdict:** CLEAR

| Check | Status | Evidence |
|-------|--------|----------|
| Data Destruction (DAT) | CLEAR | 4 source files + 1 test file deleted via git (recoverable). No DB operations. |
| Irreversible State (IRR) | CLEAR | No force pushes, hard resets, or irreversible git operations |
| Production Impact (PRD) | CLEAR | No deployment, CI/CD, or infrastructure changes |
| Permission Escalation (PRM) | CLEAR | No permission changes |
| Secret Operations (SEC) | CLEAR | No secrets, credentials, or API keys in diff |

### HALT Findings

None

## Findings

### High

**AF-001** | Skeptic/V1 | `ParagraphTranslationCoordinator.kt:108`
**Issue:** `isRetryableError` uses `errorMessage.contains("5")` to detect 5xx HTTP errors. This matches ANY error message containing the character "5" (e.g., "Connection refused on port 5432", "timeout after 5 seconds", "paragraph 5 translation failed"). Non-retryable errors that happen to contain "5" will be incorrectly retried up to 3 times with exponential backoff, wasting API quota and delaying user feedback by ~7 seconds.
**Recommendation:** Replace `errorMessage.contains("5")` with a proper 5xx pattern: `Regex("\\b5\\d{2}\\b").containsMatchIn(errorMessage)` or `errorMessage.contains("500") || errorMessage.contains("502") || errorMessage.contains("503")`. Note: The same bug exists in the pre-existing provider files (`AnthropicClient.kt:716`, `OpenAICompatibleClient.kt:789`) which were copied from the old `ChunkTranslationCoordinator`. Fix all three occurrences.

### Medium

**AF-002** | Skeptic/V1 | `ParagraphTranslationCoordinator.kt:69-80`
**Issue:** When `AIClient.translate()` returns `TranslationResult.Error` (a result, not an exception), the retry logic always retries with backoff regardless of the error content. For example, if the API returns `Error(content = "Invalid API key")` as a result instead of throwing, it will be retried 3 times with backoff before finally failing. The `isRetryableError()` classification is only applied to thrown exceptions (line 85), not to result-type errors.
**Recommendation:** Before retrying on `TranslationResult.Error`, check the error content against retryable patterns. Add a helper like `isRetryableErrorMessage(message: String): Boolean` and use it for both exception messages and result error content.

**AF-003** | Skeptic/V5 | `ArticleScreen.kt:295,711,728`
**Issue:** Three user-facing strings are hardcoded in English instead of using string resources:
- Line 295: `"Translating article, please wait"` (accessibility content description)
- Line 711: `"$completedCount/$totalCount paragraphs translated"`
- Line 728: `"$failedCount paragraph(s) failed to translate"`
This breaks i18n for an app that supports multiple languages via string resources throughout.
**Recommendation:** Add string resources with plurals support to `strings.xml` and use `stringResource()` / `pluralStringResource()` for all three.

### Low

**AF-004** | Minimalist/V7 | `ParagraphTranslationCoordinator.kt:118`
**Issue:** `MAX_PARAGRAPH_CONCURRENCY = 5` is declared as a companion object constant but never referenced in any code. The spec mentions it should be used for input validation (clamping `paragraphConcurrency`), but no validation is implemented.
**Recommendation:** Either add `init { require(paragraphConcurrency in 1..MAX_PARAGRAPH_CONCURRENCY) }` validation, or remove the unused constant.

**AF-005** | Architect/V1 | `ArticleScreen.kt:554-563`
**Issue:** The `translatedParagraphs` extraction logic is duplicated for `Translating` and `Translated` states with identical code:
```kotlin
is TranslationState.Translating ->
    translation.articleTranslation.buildTranslatedParagraphsList().map { it ?: "" }
is TranslationState.Translated ->
    translation.articleTranslation.buildTranslatedParagraphsList().map { it ?: "" }
```
**Recommendation:** Combine these into a single branch or extract to a local function. Minor, but adds maintenance burden.

**AF-006** | Skeptic/V1 | `MarkdownToAnnotatedStringTest.kt` (deleted)
**Issue:** A 238-line test file from spec-23 (markdown rendering) was deleted in this diff. This file is unrelated to parallel paragraph translation and its deletion is not mentioned in the spec's dead code cleanup section (Section 4). This appears to be accidental collateral.
**Recommendation:** Verify whether this test deletion is intentional. If the test is still valid (testing `htmlToAnnotatedString`), restore it. If the function it tests was removed in a prior spec, the deletion is fine but should be documented.

## Vector Coverage

| Vector | Lens | Findings | Highest Severity |
|--------|------|----------|-----------------|
| V1: False Assumptions | Skeptic | 3 (AF-001, AF-002, AF-005) | High |
| V2: Edge Cases | Skeptic | 0 | -- |
| V3: Failure Modes | Skeptic | 0 | -- |
| V4: Adversarial Input | Skeptic | 0 | -- |
| V5: Safety & Compliance | Skeptic | 1 (AF-003) | Medium |
| V6: Grounding Audit | Skeptic | 0 | -- |
| V7: Dependencies | Architect + Minimalist | 1 (AF-004) | Low |

## What Went Well

1. **Concurrency pattern is correct.** `Semaphore + channelFlow + coroutineScope` is the right choice. The semaphore properly bounds concurrent API calls, `channelFlow` enables immediate emission, and `coroutineScope` ensures structured concurrency with proper cancellation propagation. `StateFlow.update{}` uses CAS internally so no updates are lost under concurrency.

2. **Clean state machine design.** The `TranslationState` sealed interface with `Empty -> Translating -> Translated/Error` transitions is well-structured. The `ArticleTranslation` data model with computed properties (`paragraphCompletedCount`, `isAllCompleted`, `buildTranslatedParagraphsList`) cleanly separates state logic from UI rendering.

3. **Test coverage is solid.** Both `ArticleTranslationTest` (9 tests covering all computed properties) and `ParagraphTranslationCoordinatorTest` (8 tests covering concurrency limits, retry behavior, error classification, edge cases) provide meaningful coverage with a well-designed `MockAIClient`.

## Lead Judgment

| Finding | Accept/Reject | Rationale |
|---------|---------------|-----------|
| AF-001 | **Accept** | Provably wrong pattern matching. Must fix before merge. |
| AF-002 | **Accept** | Result-type errors should be classified same as exceptions. Fix is small. |
| AF-003 | **Accept** | i18n compliance is a project standard; hardcoded English strings violate it. |
| AF-004 | **Accept** | Unused constant should either be used or removed. Low priority. |
| AF-005 | **Accept** | Minor duplication, can address during cleanup. |
| AF-006 | **Accept** | Investigate and either restore or document. |
