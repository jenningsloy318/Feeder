# Code Review: Fix Summary Raw JSON Display

**Date:** 2026-03-17
**Reviewer:** super-dev:code-reviewer
**Status:** Approved with Comments
**Base SHA:** N/A (unstaged changes)
**Head SHA:** HEAD (034-fix-summary-raw-json branch)

## Summary Statistics

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 0 |
| Medium | 2 |
| Low | 2 |
| Info | 2 |

| Dimension | Issues |
|-----------|--------|
| Correctness | 0 |
| Security | 0 |
| Performance | 0 |
| Maintainability | 2 |
| Testability | 1 |
| Error Handling | 1 |
| Consistency | 2 |
| Accessibility | 0 |

## Specification Validation

| Criterion | Status | Evidence |
|-----------|--------|----------|
| AC-01: Summary never shows raw JSON regardless of response format | Met | `SummaryResponseParser.kt:48-92` (3-layer extraction + legacy + UI defense) |
| AC-02: Existing summary functionality preserved | Met | Tests: `parse_validJsonWithAllFields_extractsSummary`, `parse_plainTextResponse_returnsAsIs`, `parse_legacyLangFormat_extractsSummaryWithoutPrefix` |
| AC-03: Both clients use shared parsing logic | Met | `OpenAICompatibleClient.kt:238`, `AnthropicClient.kt:175` — both call `SummaryResponseParser.parse(text)` |
| AC-04: Unit tests cover all edge cases | Met | 58 tests in `SummaryResponseParserTest.kt`, all passing |

### Non-Goals Check
- [x] No changes to translation pipeline (correct)
- [x] No changes to markdown rendering (correct)
- [x] No changes to AI prompt construction (correct)

## BDD Scenario Coverage

| Scenario ID | Title | Test Reference | Status |
|-------------|-------|---------------|--------|
| SCENARIO-001 | Well-formed JSON response | `parse_validJsonWithAllFields_extractsSummary` | Covered |
| SCENARIO-002 | JSON inside markdown code block | `parse_jsonInJsonCodeBlock_extractsSummary` | Covered |
| SCENARIO-003 | LLM wraps JSON in explanatory text | `parse_jsonWithTextPreamble_extractsSummary` | Covered |
| SCENARIO-004 | Truncated JSON response | `parse_truncatedJsonInCodeBlock_returnsErrorMessage` | Covered |
| SCENARIO-005 | Error message containing JSON | `parse_errorMessageContainingJson_returnsCleanMessage` | Covered |
| SCENARIO-006 | Double-encoded JSON string | `parse_doubleEncodedJsonString_handlesGracefully` | Covered |
| SCENARIO-007 | Plain text summary | `parse_plainTextResponse_returnsAsIs` | Covered |
| SCENARIO-008 | Legacy "Lang: XX" format | `parse_legacyLangFormat_extractsSummaryWithoutPrefix` | Covered |
| SCENARIO-009 | Empty summary field | `parse_emptySummaryFieldInValidJson_returnsErrorMessage` | Covered |
| SCENARIO-010 | Unrecognized JSON structure | `parse_unrecognizedJsonStructure_returnsErrorMessage` | Covered |
| SCENARIO-011 | OpenAI client uses shared parser | Code: `OpenAICompatibleClient.kt:238` | Covered |
| SCENARIO-012 | Anthropic client uses shared parser | Code: `AnthropicClient.kt:175` | Covered |
| SCENARIO-013 | UI layer catches JSON | `containsRawJson_*` tests + `ArticleScreen.kt:669` | Covered |
| SCENARIO-014 | Truncated code block markup | `parse_truncatedCodeBlockMarkup_returnsErrorMessage` | Covered |

**Coverage:** 14/14 scenarios covered
**Gate:** PASS

## Findings

### Medium

**F-001** | Consistency | `ArticleScreen.kt:669`, `ArticleViewModel.kt:488`
**Issue:** Fully-qualified class references instead of imports. Both files use `com.nononsenseapps.feeder.ai.SummaryResponseParser.containsRawJson(...)` and `com.nononsenseapps.feeder.ai.SummaryResponseParser.sanitizeErrorMessage(...)` inline, while `OpenAICompatibleClient.kt:4` and `AnthropicClient.kt:7` use proper `import` statements.
**Suggestion:** Add `import com.nononsenseapps.feeder.ai.SummaryResponseParser` to both files and use short names.
**Rationale:** Fully-qualified names reduce readability and are inconsistent with the rest of the codebase. The same fix was done correctly in the provider clients.

**F-002** | Consistency | `ArticleViewModel.kt:486-489`
**Issue:** Fully-qualified reference to `com.nononsenseapps.feeder.ai.AIClient.SummaryResult.Error` when `AIClient` is already imported at line 8.
**Suggestion:** Use `AIClient.SummaryResult.Error(...)` instead of the fully-qualified path.
**Rationale:** `AIClient` is already imported in this file. The FQN adds unnecessary noise.

### Low

**F-003** | Error Handling | `SummaryResponseParser.kt:216`
**Issue:** `sanitizeErrorMessage` checks for `{` AND `}` to detect JSON in error messages. Legitimate error messages with template-style braces (e.g., `"Failed to connect to {hostname}"`) would be falsely sanitized.
**Suggestion:** Consider using `JSON_FIELD_PATTERN` for consistency with `containsRawJson`, or check for `"` + `{` + `:` patterns typical of JSON.
**Rationale:** Current implementation is a reasonable heuristic for error paths and the risk is minimal — template-style error messages from HTTP libraries are rare. However, aligning the detection logic with the existing `JSON_FIELD_PATTERN` would be more precise.

**F-004** | Testability | `SummaryResponseParserTest.kt:613-647`
**Issue:** The `parse_neverThrows_withAnyInput` test uses a try-catch assertion pattern (`assertTrue(false, ...)` inside catch) rather than JUnit's standard approach.
**Suggestion:** Use `assertDoesNotThrow` or simply call the function without try-catch (JUnit will report any uncaught exceptions as test failures).
**Rationale:** Minor style point. The current pattern works but `assertDoesNotThrow` would be more idiomatic and provide clearer failure messages.

### Info

**F-005** | Maintainability | `SummaryResponseParser.kt:89`
**Issue:** `android.util.Log` is used directly in a non-Android-framework utility class. This works because unit tests run with mocked Android framework, but creates a structural coupling.
**Suggestion:** No action needed — this is consistent with the project's existing logging pattern throughout the `ai` package.
**Rationale:** Noting for awareness. If the project ever moves parsing to a shared module, this dependency would need addressing.

**F-006** | Maintainability | `SummaryResponseParser.kt:202-208`
**Issue:** `containsRawJson` could theoretically produce false positives if a valid summary about JSON or APIs contains `"summary":` as literal text. For example, a summary discussing REST API response formats.
**Suggestion:** No action needed — this is a UI-layer defense-in-depth check. The primary parser (Layer 1) would have already correctly extracted the summary text, so the UI check only fires when the parser failed, making false positives practically impossible.
**Rationale:** Documenting the design trade-off for future reference.

## Strengths

- **Excellent deduplication**: ~165 lines of identical code removed from each client (`OpenAICompatibleClient.kt`, `AnthropicClient.kt`), consolidated into a single `SummaryResponseParser.kt:20-222` — a net reduction of ~310 lines of duplicated code
- **Robust brace-matching algorithm** (`SummaryResponseParser.kt:117-136`): Single-pass O(n) parser that correctly handles nested braces, escaped characters, and strings containing braces — addresses the primary root cause (LLM wrapping JSON in text)
- **Defense-in-depth**: Three layers of protection — JSON extraction with brace matching (Layer 1), legacy parser content checks (Layer 2), UI-level `containsRawJson` guard (Layer 3)
- **Comprehensive test coverage**: 58 tests covering all 14 BDD scenarios plus edge cases including Unicode, markdown in summaries, empty inputs, and real-world LLM response patterns
- **Robustness test** (`parse_neverThrows_withAnyInput`): Excellent fuzz-like test with 22 adversarial inputs ensuring the parser never throws
- **Clean removal**: Old parsing code, unused serialization imports, and `isValid` field all cleanly removed with no dead code left behind

## Recommendations

- Address F-001 and F-002 (import consistency) before merging — these are quick fixes that improve readability
- F-003 and F-004 are optional improvements that can be deferred

## Verdict

**Approved with Comments**

**Reasoning:** The implementation correctly addresses all 4 root causes identified in the debug analysis. The shared parser eliminates code duplication between providers, the brace-matching algorithm handles the primary failure mode (JSON wrapped in explanatory text), and defense-in-depth ensures raw JSON never reaches the user. All 14 BDD scenarios are covered by 58 passing tests. The two Medium findings (F-001, F-002) are import style issues that don't affect correctness.

**Blocking Issues:** None
