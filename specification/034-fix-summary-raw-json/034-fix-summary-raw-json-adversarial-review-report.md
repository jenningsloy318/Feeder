# Adversarial Review: Fix Summary Raw JSON Display

**Date:** 2026-03-17
**Reviewer:** super-dev:adversarial-reviewer
**Verdict:** PASS

## Intent
Fix a persistent bug where AI summaries intermittently display raw JSON to users. Previous fix attempts (spec-019, 021, 023, 026, 029) each addressed only a subset of leak paths. This fix eliminates all identified root causes by: (1) creating a shared `SummaryResponseParser` with robust brace-matching JSON extraction, (2) removing ~330 lines of duplicated parsing code from both clients, (3) adding defense-in-depth at the UI and error-message layers, and (4) covering all scenarios with 58 unit tests.

## Verdict Summary
Implementation is sound, addresses all 4 root causes with defense-in-depth, eliminates code duplication, and has comprehensive test coverage. No high-severity findings.

## Change Scope
| Metric | Value |
|--------|-------|
| Lines changed (modified files) | +20 / -330 |
| Lines added (new files) | ~887 (222 prod + 665 test) |
| Files changed | 7 (5 modified + 2 new) |
| Size classification | Large |
| Reviewers activated | Skeptic + Architect + Minimalist |
| Attack vectors applied | V1-V8 |

## Destructive Action Gate

**Gate Verdict:** CLEAR

| Check | Status | Evidence |
|-------|--------|----------|
| Data Destruction (DAT) | CLEAR | No destructive data operations in diff |
| Irreversible State (IRR) | CLEAR | No force-push, hard-reset, or irreversible git operations |
| Production Impact (PRD) | CLEAR | No deployment or infrastructure changes |
| Permission Escalation (PRM) | CLEAR | No permission changes |
| Secret Operations (SEC) | CLEAR | No secret/credential operations |

### HALT Findings
None

## D9 BDD Document Validation

- `01.1-behavior-scenarios.md` exists: **YES**
- Contains Traceability Matrix: **YES** (lines 141-148)
- `01-requirements.md` exists: **NO** -- but this is a bug fix, not a feature. The debug analysis (`034-debug-analysis.md`) serves as the requirements source, and the BDD scenarios define their own ACs (AC-01 through AC-04) sourced from the debug analysis. All 4 ACs are represented in the traceability matrix. **Accepted.**
- All ACs represented in traceability matrix: **YES** (AC-01: 8 scenarios, AC-02: 4 scenarios, AC-03: 2 scenarios, AC-04: all scenarios)

## Findings

### Medium

**AF-001** | Skeptic/V2 | `SummaryResponseParser.kt:202-208`
**Issue:** `containsRawJson()` uses `JSON_FIELD_PATTERN` to detect embedded JSON field names (`"summary"\s*:`, `"language"\s*:`, etc.). A legitimate summary of a technical article discussing JSON APIs could contain these exact patterns (e.g., *The API returns "summary": "..." in its response*), triggering a false positive that replaces a valid summary with an error message.
**Recommendation:** Accept as-is. This is a defense-in-depth check (Layer 3) that only fires when Layers 1-2 have already failed. The probability of a valid summary reaching this layer AND containing these patterns is extremely low. The impact (showing error message vs. raw JSON) favors the cautious direction. If false positives are observed in practice, the field list could be narrowed to just `"keyPoints"` which is the most distinctive field name.

### Low

**AF-002** | Minimalist/V7 | `ArticleScreen.kt:669`, `ArticleViewModel.kt:488`
**Issue:** Fully qualified class name `com.nononsenseapps.feeder.ai.SummaryResponseParser` used inline instead of an import statement. This adds visual noise and is inconsistent with `AIApi.kt` and both client files which use proper imports.
**Recommendation:** Add `import com.nononsenseapps.feeder.ai.SummaryResponseParser` to both files and use the short name. This is style-only, not a correctness issue.

**AF-003** | Skeptic/V5 | `SummaryResponseParser.kt:216`
**Issue:** `sanitizeErrorMessage()` checks `contains("{") && contains("}")` which could match non-JSON error messages containing curly braces (e.g., stack traces, template strings like "Error in {module}"). This would replace a potentially useful error message with a generic one.
**Recommendation:** Accept as-is. On the error path, being overly cautious is the correct trade-off -- showing a generic error is always better than leaking JSON. If diagnostic quality becomes a concern, a more targeted check like `Regex("""\{[^}]*"[^"]*"[^}]*\}""")` could be used, but the current approach is sufficient.

**AF-004** | Skeptic/V1 | `SummaryResponseParser.kt:118-136`
**Issue:** `extractJsonObject()` finds the **first** `{` in the text. If the LLM preamble contains prose with curly braces before the actual JSON (e.g., *"The function() { return x } outputs: {"summary":"text"}"*), the brace matcher would extract the wrong object (`{ return x }`), fail JSON parsing, and fall to the legacy parser.
**Recommendation:** Accept as-is. (1) LLM preambles virtually never contain balanced curly braces in prose. (2) Even if triggered, the fallback chain handles it correctly -- `parseLegacyResponse` receives the original content and detects embedded JSON fields via regex, returning an error message rather than raw JSON. The defense-in-depth architecture absorbs this edge case.

## Vector Coverage
| Vector | Lens | Findings | Highest Severity |
|--------|------|----------|-----------------|
| V1: False Assumptions | Skeptic | 1 (AF-004) | Low |
| V2: Edge Cases | Skeptic | 1 (AF-001) | Medium |
| V3: Failure Modes | Skeptic | 0 | -- |
| V4: Adversarial Input | Skeptic | 0 | -- |
| V5: Safety & Compliance | Skeptic | 1 (AF-003) | Low |
| V6: Grounding Audit | Skeptic | 0 | -- |
| V7: Dependencies | Architect + Minimalist | 1 (AF-002) | Low |
| V8: Behavior Coverage | Skeptic | 0 | -- |

## What Went Well

1. **Defense-in-depth architecture**: Four layers of protection (JSON extraction, legacy parser, UI check, error sanitization) ensure no single failure leaks raw JSON. The fallback chain is well-designed -- each layer catches what the previous layer misses.

2. **Code deduplication**: Extracting ~330 lines of duplicated parsing logic from both clients into a single `SummaryResponseParser` eliminates the root structural cause of past fix drift. Future fixes need only be applied once.

3. **Comprehensive test coverage**: 58 unit tests cover all 14 BDD scenarios plus additional edge cases. The `parse_neverThrows_withAnyInput` test with 21 diverse inputs is excellent for ensuring robustness. The `assertNoRawJson` helper provides consistent validation across tests.

## Lead Judgment

| Finding | Decision | Rationale |
|---------|----------|-----------|
| AF-001 (Medium) | **Accept** | Defense-in-depth check correctly favors safety over theoretical false positives. No action needed. |
| AF-002 (Low) | **Accept** | Style issue only; can be addressed in a follow-up cleanup. |
| AF-003 (Low) | **Accept** | Error path sanitization should be aggressive. Generic errors are always preferable to JSON leaks. |
| AF-004 (Low) | **Accept** | Fallback chain handles this correctly. Theoretical edge case with no realistic impact. |
