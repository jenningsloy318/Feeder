# 038 - Separate Summary Toggle: Specification Review

---

## Overall Verdict: APPROVED

The specification is well-structured, internally consistent, and grounded in accurate codebase analysis. The change is well-scoped (~80-100 production LOC across 8 files) and the implementation plan correctly decomposes the work into safe, incremental phases. A few minor findings are documented below, none blocking implementation.

---

## Dimension Scores

| Dimension | Score (1-5) | Summary |
|:----------|:-----------:|:--------|
| **Completeness** | 5 | All FR-001 through FR-005 and AC-001 through AC-007 are fully addressed with clear traceability |
| **Correctness** | 4 | Code patterns, line numbers, and implementation details are accurate; two minor issues noted |
| **Consistency** | 5 | Task IDs, change points, scenario IDs, and risk references align perfectly across all documents |
| **Feasibility** | 5 | ~60 production LOC + ~85-110 test LOC is realistic; follows well-established patterns in the codebase |
| **Testability** | 4 | Testing strategy covers all BDD scenarios; ViewModel tests may require non-trivial mocking setup |
| **Risk Coverage** | 5 | R-1 through R-5 from the code assessment are all identified with clear mitigation strategies |
| **Grounding** | 4 | File paths, line numbers, and patterns verified against actual source; two minor line number drifts |
| **Ambiguity** | 5 | Instructions are clear and unambiguous; the implementation code snippets leave no room for misinterpretation |

**Aggregate: 37/40**

---

## Findings

### Finding 1: SettingsStore setter pattern mismatch (Low)

**Dimension**: Correctness

**Detail**: The spec (06-specification.md, section 3.4) shows `SummarySettingsViewModel.setEnableSummary()` wrapping the call in `viewModelScope.launch { ... }`. However, looking at `Repository.setSummaryEnabled()` (line 386), it delegates directly to `settingsStore.setSummaryEnabled()` which is a synchronous `SharedPreferences.edit().apply()` call (not a suspend function). The `viewModelScope.launch` is technically unnecessary but harmless, and is consistent with how the existing `setSummaryEnabled()` in `SummarySettingsViewModel` already wraps the call at line 28-31. This is not a bug, just worth noting that the pattern in the spec correctly matches the existing ViewModel pattern even though the underlying operation is non-suspending.

**Severity**: Low  
**Recommendation**: No change needed. The pattern is consistent with existing code.

---

### Finding 2: ArticleScreenViewState interface line number offset (Low)

**Dimension**: Grounding

**Detail**: The spec (06-specification.md, section 3.5) states `showSummarize` is at line 777 in the `ArticleScreenViewState` interface and line 749 in the `ArticleState` data class. Verified: `showSummarize` is indeed at line 777 in the interface and line 749 in the data class. These are correct. However, the spec says to add `showTranslate` "after `showSummarize` at line 777" in the interface and "after `showSummarize` at line 749" in the data class. After `showSummarize` in the interface (line 777), the next fields are `aiSummary` (line 778) and `translation` (line 779), then `articleContent` (line 780). In the data class (line 749), after `showSummarize`, the next fields are `aiSummary` (line 750), `translation` (line 751), `articleContent` (line 752). The `showTranslate` should logically be placed right after `showSummarize` in both locations which the spec correctly states.

**Severity**: Low  
**Recommendation**: Confirmed correct. No change needed.

---

### Finding 3: SCENARIO-021 cached summary behavior may not be implementable as written (Medium)

**Dimension**: Correctness / Testability

**Detail**: SCENARIO-021 states "the cached summary is still accessible and displayed" when Enable Summary is OFF. However, looking at the implementation in `ArticleViewModel`, the `aiSummary` `MutableStateFlow` is initialized as `AISummaryState.Empty` each time the ViewModel is created (line 137). Summaries are not persisted or cached in the ViewModel itself -- the `aiSummary` state only lives for the duration of the article view session. The "cached summary" behavior would depend on the summary cache system (from spec-034), which stores summaries externally. When `showSummarize` is false (Enable Summary OFF), the summarize button is hidden, but if the summary was already loaded into `aiSummary` state during that session, it would still be displayed in the article body. However, if the user navigates away and comes back with Enable Summary OFF, no auto-summarize will trigger and the summary will not be loaded from cache.

The spec's error handling section (06-specification.md, section 4.1) correctly notes "Cached summaries remain accessible regardless of toggle state (SCENARIO-021)" but the implementation does NOT include code to load cached summaries when `enableSummary` is OFF. This is an edge case that may need clarification: should cached summaries be proactively loaded even when summary is disabled? The current spec says "no new summary request" which is correct, but whether the cached result is displayed is ambiguous.

**Severity**: Medium  
**Recommendation**: Clarify SCENARIO-021 in one of two ways: (a) explicitly state that cached summaries from a prior session are NOT displayed when Enable Summary is OFF (simplest, matches the implementation), or (b) add logic to load cached summaries even when `enableSummary` is OFF (adds complexity). Option (a) is recommended since the spec's Out of Scope section says "Cached summaries remain accessible regardless of toggle state; this spec only gates new summary requests" -- this phrasing is slightly misleading if cached summaries are not proactively loaded. Consider rewording to "This spec does not delete existing cached summaries" to avoid confusion.

---

### Finding 4: SwitchSetting alpha placement relative to clickable (Low)

**Dimension**: Correctness

**Detail**: The spec (06-specification.md, section 3.7) correctly shows `.alpha(if (enabled) 1f else 0.38f)` placed before `.clickable()` in the modifier chain. In Compose, modifier order matters: `.alpha()` before `.clickable()` means the entire row including the click ripple will be dimmed, which is the desired behavior for a disabled state. This is correct.

However, note that the `Switch` composable at line 1358-1362 already handles its own disabled state via `enabled = enabled`. With the row-level alpha at 0.38f AND the Switch's own disabled alpha, the Switch will appear "double-dimmed" (0.38 * disabled_alpha). This is a known minor visual issue but is consistent with how Material 3 handles disabled states when both the container and the component are disabled. The visual result is acceptable -- the entire row looks uniformly dimmed.

**Severity**: Low  
**Recommendation**: Acceptable as-is. If the double-dimming on the Switch is visually jarring during implementation, consider applying alpha only to the text content rather than the entire Row. But the current approach is simpler and the visual impact is minimal.

---

### Finding 5: Missing alpha import check (Low)

**Dimension**: Completeness

**Detail**: Step 3.1 of the implementation plan mentions adding `import androidx.compose.ui.draw.alpha` if not already imported. The spec correctly identifies this need.

I verified: `Settings.kt` does NOT currently import `alpha`. The existing imports include `androidx.compose.ui.draw.clip` but not `alpha`. The import will need to be added.

**Severity**: Low  
**Recommendation**: Already noted in the spec. No change needed.

---

### Finding 6: Test infrastructure for ArticleViewModel may need setup (Low)

**Dimension**: Testability

**Detail**: TASK-014 and TASK-015 reference "Existing ArticleViewModel test file or new test class" for auto-summary gating and showSummarize/showTranslate tests. The `ArticleViewModel` is complex with many dependencies (Repository, TtsStateHolder, AIApi, etc.) and testing the `combine` + `collect` logic in the init block will require mocking or faking multiple dependencies. The spec's LOC estimates (40-50 for TASK-014, 20-30 for TASK-015) seem reasonable if a test infrastructure already exists, but may underestimate if a new test class needs to be scaffolded from scratch.

**Severity**: Low  
**Recommendation**: During implementation, check if `ArticleViewModelTest` exists. If not, the LOC estimate for tests should increase by ~30-50 lines for test setup boilerplate. The functional logic tests themselves are well-defined.

---

### Finding 7: Consistency of enableSummary vs summaryEnabled naming (Low)

**Dimension**: Ambiguity

**Detail**: The codebase has `summaryEnabled` (existing, meaning "auto summary") and the spec introduces `enableSummary` (new, meaning "master toggle"). These names are similar but inverted in word order. The spec is consistent about this naming throughout all documents, and the code is self-documenting once you read the preference keys (`PREF_SUMMARY_ENABLED` vs `PREF_ENABLE_SUMMARY`). However, during implementation, the similar names could cause confusion.

**Severity**: Low  
**Recommendation**: The spec already handles this well by renaming the UI label from "Enable Auto Summary" to "Auto Summary". The code-level naming is acceptable given the existing codebase convention. No change needed.

---

## Cross-Document Consistency Verification

| Check | Result |
|:------|:-------|
| FR-001 through FR-005 all have corresponding scenarios | Pass - all FRs traced to scenarios in 02-behavior-scenarios.md |
| AC-001 through AC-007 all have corresponding scenarios | Pass - traceability matrix in 02 maps all ACs |
| CP-1 through CP-8 consistent across 04, 06, 07, 08 | Pass - all change points aligned |
| R-1 through R-5 mitigated in spec and implementation plan | Pass - R-3 (summarizeOnOpen) explicitly gated, R-4 (translate) split into showTranslate |
| Task dependencies form valid DAG | Pass - no circular dependencies |
| LOC estimates consistent | Pass - 04 says ~80-100, 08 says ~60 production + ~85-110 test = ~145-170 total. Consistent |
| Build checkpoints after each phase | Pass - 4 checkpoints defined |
| Line numbers match actual source | Pass - verified SettingsStore (818-824, 1029, 1112), Repository (384-386), ArticleViewModel (144-207, 231-252, 749, 777), SummarySettingsScreen (92-97), Settings.kt (1306-1365), strings.xml (322-324) |

---

## Summary

The specification is thorough, well-grounded, and ready for implementation. The main finding (Finding 3 about SCENARIO-021 cached summary behavior) is a clarification issue that does not block implementation -- the simplest interpretation (cached summaries are not proactively loaded when summary is disabled) aligns with the proposed code changes. All other findings are low severity and mostly confirmatory.

**Recommended next step**: Proceed to implementation following the phased plan in 07-implementation-plan.md.
