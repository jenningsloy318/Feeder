# 039 - Separate Translation Toggle: Specification Review

**Date**: 2026-04-18
**Reviewer**: Specification Review Agent
**Status**: Review complete

---

## Verdict: Approved with minor issues

The specification is well-structured, thorough, and closely mirrors the proven spec-038 pattern. All code references have been verified against the actual source and are accurate. The issues identified are minor and do not block implementation.

---

## Scores

| Dimension | Score (1-5) | Notes |
|:----------|:-----------:|:------|
| Completeness | 5 | All requirements traced through BDD scenarios to tasks. 26 BDD scenarios cover all matrix states, edge cases, and backward compatibility. |
| Correctness | 4 | Code snippets, line numbers, and file paths are accurate. One conceptual issue with per-feed `translateOnOpen` (see Finding #1). |
| Consistency | 5 | All documents agree. The spec, implementation plan, and task list are fully aligned. |
| Feasibility | 5 | Each task is small, mechanical, and follows a proven pattern. No design ambiguity. |
| Testability | 4 | SettingsStore and OPML tests covered. No ViewModel-level test for `showTranslate` gating (see Finding #2). |
| Complexity | 5 | Low complexity — direct mechanical mirror of spec-038 pattern. All tasks are Small size. |
| Ambiguity | 5 | No ambiguity in the implementation. The only unclear area (per-feed translateOnOpen) is correctly flagged as a no-op. |
| Traceability | 5 | Full traceability matrix in 02-behavior-scenarios.md maps every scenario to acceptance criteria. Task list maps tasks to scenarios. |
| Grounding | 5 | All line numbers, variable names, file paths, and code patterns verified against actual source. |

**Overall: 4.75 / 5**

---

## Grounding Verification Results

### SettingsStore.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `_enableSummary` at lines 826-832 | YES | Actual: lines 826-832, pattern matches exactly |
| `PREF_ENABLE_SUMMARY` at line 1038 | YES | Actual: line 1038 |
| `SETTING_ENABLE_SUMMARY` at lines 1122-1123 | YES | Actual: lines 1121-1123 (1121 = SUMMARY_ENABLED, 1122 = ENABLE_SUMMARY) |
| `_translationEnabled` at lines 855-862 | YES | Actual: lines 856-862 (855 = comment line) |
| `PREF_TRANSLATION_ENABLED` at line 1049 | YES | Actual: line 1049 |
| UserSettings enum ends with `;` at line 1124 | YES | Actual: line 1124 |
| No `SETTING_TRANSLATION_ENABLED` in enum | YES | Confirmed — only summary entries exist |

### Repository.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `enableSummary` proxy at lines 389-391 | YES | Actual: lines 388-391 |
| `translationEnabled` proxy at lines 403-406 | YES | Actual: lines 403-406 |
| Insert point after line 406 | YES | Line 407 starts translation timeout section |

### ArticleViewModel.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| viewState combine starts at line 145 with 12 flows | YES | Lines 145-158, indices 0-11 |
| `enableSummary = params[11]` at line 170 | YES | Actual: line 170 |
| `showTranslate = aiValid` at line 173 | YES | Actual: line 173, not gated |
| Auto-translate combine at lines 262-285 | YES | Lines 262-285, 3-flow combine with Triple |
| Condition `translationEnabled &&` at line 275 | YES | Actual: line 275 |

### TranslationSettingsScreen.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `translationEnabled` collected at line 54 | YES | Actual: line 54 |
| Single toggle at lines 91-97 | YES | Actual: lines 91-97 |

### TranslationSettingsViewModel.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `translationEnabled` at line 24 | YES | Actual: line 24 |
| `setTranslationEnabled` at line 28-32 | YES | Actual: lines 28-32 |

### OPMLImporter.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `SETTING_ENABLE_SUMMARY` handler at line 164 | YES | Actual: line 164 |
| `when` block ends at line 166 (closing brace) | YES | Actual: line 166 |

### ArticleScreen.kt
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `showTranslate` gate at line 365 | YES | Actual: line 365 |

### strings.xml
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `translation_enabled_title` = "Enable Auto Translation" at line 332 | YES | Actual: line 332 |
| `translation_enabled_description` at line 333 | YES | Actual: line 333 |

### Test Files
| Claim | Verified | Notes |
|:------|:--------:|:------|
| `enableSummary*` tests at lines 428-464 | YES | Actual: lines 428-465 (4 tests) |
| OpmlParserTest settings map around line 109 | YES | Actual: lines 94-111 |
| OpmlWriterKtTest settings map around line 217 | YES | Actual: lines 204-219 |

---

## Findings

### Finding #1: Per-feed `translateOnOpen` referenced but does not exist — MEDIUM

**Location**: 01-requirements.md FR-005, AC-008; 02-behavior-scenarios.md SCENARIO-013, SCENARIO-014

**Issue**: The requirements document (FR-005) specifies that `enableTranslation` shall gate per-feed `translateOnOpen` overrides. BDD scenarios 013 and 014 test per-feed `translateOnOpen` behavior. However, `translateOnOpen` does not exist in the codebase — only `summarizeOnOpen` exists per-feed. The code assessment (04, section 4.3) and specification (05, section 7.5) correctly identify this as a no-op, but the requirements and BDD scenarios present it as though it's a real feature being gated.

**Impact**: The requirements and BDD scenarios describe behavior for a feature that doesn't exist, creating confusion for implementers and reviewers. The scenarios are untestable as written.

**Recommendation**: Add a note to FR-005 and AC-008 that `translateOnOpen` does not currently exist per-feed, and that these requirements are forward-looking. Alternatively, remove FR-005, AC-008, SCENARIO-013, and SCENARIO-014 entirely and add them when per-feed translate-on-open is actually implemented. The spec (05) and code assessment (04) already handle this correctly — the requirements and BDD just need to match.

**Severity**: MEDIUM — Conceptual inconsistency between requirements and actual codebase; does not block implementation since the spec and task list correctly treat this as a no-op.

---

### Finding #2: No ViewModel-level unit test for `showTranslate` gating — LOW

**Location**: 07-task-list.md, 05-specification.md section 6

**Issue**: The testing strategy covers SettingsStore (4 new tests) and OPML (map updates), but there is no unit test for the `ArticleViewModel` to verify that `showTranslate = enableTranslation && aiValid` works correctly. The `showSummarize` gating in spec-038 similarly has no ViewModel-level test, so this is consistent with the pattern, but it means the core gating logic is only verified by compilation + manual testing.

**Impact**: The most critical behavioral change (hiding the translate button when `enableTranslation` is false) has no automated test.

**Recommendation**: Consider adding an `ArticleViewModel` test that verifies `viewState.showTranslate` is false when `enableTranslation` is false and AI is configured. This would be a higher-confidence verification than trusting compilation alone. However, if the project convention is to skip ViewModel integration tests (as was done for spec-038), this is acceptable as-is.

**Severity**: LOW — Consistent with existing project test patterns; the logic is simple enough that compilation + manual testing provides reasonable confidence.

---

### Finding #3: Auto-translate combine restructuring approach is well-chosen — INFO

**Location**: 05-specification.md section 4.6

**Issue**: None — this is a positive finding. The spec correctly identifies that the auto-translate combine moves from 3 flows (Triple-based) to 4 flows (data class-based), and explicitly documents the approach. Kotlin's `combine` supports up to 5 flows with explicit lambda params, so the data class approach is clean.

**Severity**: INFO — Good design decision, well-documented.

---

### Finding #4: OPML export for existing translation settings out of scope — INFO

**Location**: 05-specification.md section 5 (Scope note)

**Issue**: The spec explicitly notes that existing translation settings (`translationEnabled`, `translationLanguage`, `translationTimeout`) are NOT in the `UserSettings` enum and thus NOT exported/imported via OPML. The spec adds only `SETTING_ENABLE_TRANSLATION`. The code assessment (04, section 4.4) flagged this.

**Impact**: Users cannot round-trip their auto-translation, language, and timeout preferences via OPML. This is pre-existing behavior and not introduced by this spec.

**Recommendation**: Acceptable as-is for this spec. Consider a follow-up spec to add the remaining translation settings to OPML export/import.

**Severity**: INFO — Pre-existing limitation, explicitly documented as out of scope.

---

### Finding #5: Combine parameter count grows to 13 — INFO

**Location**: 05-specification.md section 4.5

**Issue**: The `viewState` combine in `ArticleViewModel` grows from 12 to 13 flows. Index-based `params[N] as Type` casting is fragile. This is pre-existing technical debt.

**Impact**: None for this spec — it follows the established pattern. The risk is correctly categorized as INFO.

**Severity**: INFO — Pre-existing design pattern; no action needed for this spec.

---

### Finding #6: OPMLImporter `when` block is exhaustive — needs enum entry for compilation — INFO

**Location**: 07-task-list.md TASK-010

**Issue**: The `when` block in `OPMLImporter.kt` appears to be exhaustive (no `else` branch visible at the end of the block at line 166). Adding `SETTING_ENABLE_TRANSLATION` to the `UserSettings` enum without adding a corresponding `when` case would cause a compilation error. TASK-007 (OPML import handler) and TASK-010 (OPML test maps) correctly address this. The dependency graph shows TASK-007 depends on TASK-001, which is correct.

**Severity**: INFO — Correctly handled in the task list.

---

## Traceability Verification

| Requirement | BDD Scenarios | Spec Section | Tasks |
|:------------|:-------------|:-------------|:------|
| FR-001 | SCENARIO-022, 023 | 4.1 | TASK-001 |
| FR-002 | SCENARIO-001, 002, 003 | 4.5, 4.6 | TASK-005, TASK-006 |
| FR-003 | SCENARIO-004, 005 | 4.5, 4.6 | TASK-005, TASK-006 |
| FR-004 | SCENARIO-006, 007, 009, 010 | 4.4 | TASK-004 |
| FR-005 | SCENARIO-013, 014 | 7.5 (no-op) | TASK-006 (no-op) |
| FR-006 | SCENARIO-006, 007, 008 | 4.4 | TASK-004 |
| FR-007 | SCENARIO-015, 016 | 4.8, 5 | TASK-007, TASK-010 |
| FR-008 | N/A (string values) | 4.9 | TASK-008 |
| NFR-001 | SCENARIO-022, 023 | 7.3 | TASK-001 (default=true) |
| NFR-005 | All | All | All |
| AC-001 | SCENARIO-002, 003, 010 | 4.5 | TASK-005 |
| AC-002 | SCENARIO-001 | 4.5, 4.6 | TASK-005, TASK-006 |
| AC-003 | SCENARIO-004, 005, 017 | 4.5, 4.6 | TASK-005, TASK-006 |
| AC-004 | SCENARIO-006 | 4.4 | TASK-004 |
| AC-005 | SCENARIO-007, 009, 010 | 4.4 | TASK-004 |
| AC-006 | SCENARIO-011, 012 | 4.4 | TASK-004 |
| AC-007 | SCENARIO-008 | 4.4 | TASK-004 |
| AC-008 | SCENARIO-013, 014 | 7.5 (no-op) | TASK-006 (no-op) |
| AC-009 | SCENARIO-015, 016 | 4.8, 5 | TASK-007, TASK-010 |

All requirements and acceptance criteria are traced. FR-005/AC-008 are correctly identified as no-ops in the spec and code assessment.

---

## Summary

The specification is high quality — well-organized, thoroughly grounded in actual code, and follows the proven spec-038 pattern. The only substantive issue is the per-feed `translateOnOpen` references in requirements and BDD scenarios describing a feature that doesn't exist in the codebase. The spec and code assessment correctly flag this as a no-op, but the requirements document should be updated for clarity. The lack of a ViewModel-level test for `showTranslate` gating is a minor gap consistent with existing project patterns.

**Recommendation**: Proceed with implementation. Optionally add a clarifying note to FR-005/AC-008 about `translateOnOpen` not existing per-feed.
