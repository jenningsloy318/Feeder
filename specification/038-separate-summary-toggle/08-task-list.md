# 038 - Separate Summary Toggle: Task List

---

## Phase 1: Data Layer

### TASK-001: Add `PREF_ENABLE_SUMMARY` Constant

- **Description**: Add the SharedPreferences key constant for the new `enableSummary` setting.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- **Location**: After `PREF_SUMMARY_ENABLED` constant (line 1029)
- **Estimated LOC**: 3 (comment + constant)
- **Dependencies**: None
- **Verification**: Compiles

### TASK-002: Add `enableSummary` MutableStateFlow, StateFlow, and Setter

- **Description**: Add the `_enableSummary` backing field, public `enableSummary` StateFlow, and `setEnableSummary()` method following the `summaryEnabled` pattern.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- **Location**: After `setSummaryEnabled()` (line 824)
- **Estimated LOC**: 8
- **Dependencies**: TASK-001
- **Verification**: Compiles

### TASK-003: Add `SETTING_ENABLE_SUMMARY` to UserSettings Enum

- **Description**: Add enum entry for OPML export support.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- **Location**: After `SETTING_SUMMARY_ENABLED` (line 1112)
- **Estimated LOC**: 1
- **Dependencies**: TASK-001
- **Verification**: Compiles

### TASK-004: Add Repository Proxy for `enableSummary`

- **Description**: Add pass-through property and setter method in Repository.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
- **Location**: After `setSummaryEnabled()` (line 386)
- **Estimated LOC**: 4
- **Dependencies**: TASK-002
- **Verification**: Compiles

### TASK-005: Add New String Resources and Rename Existing

- **Description**: Add `enable_summary_title` and `enable_summary_description` strings. Rename `summary_enabled_title` from "Enable Auto Summary" to "Auto Summary" and `summary_enabled_description` to "Automatically summarize articles when opened".
- **File(s)**: `app/src/main/res/values/strings.xml`
- **Location**: Around lines 322–324
- **Estimated LOC**: 4 (2 new + 2 modified)
- **Dependencies**: None
- **Verification**: Compiles

### BUILD CHECKPOINT: `./gradlew :app:compileFdroidDebugKotlin`

---

## Phase 2: ViewModel Layer

### TASK-006: Add `enableSummary` Flow and Setter to SummarySettingsViewModel

- **Description**: Expose `repository.enableSummary` as a StateFlow and add `setEnableSummary()` method.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`
- **Location**: After `summaryEnabled` (line 24) and after `setSummaryEnabled()` (line 32)
- **Estimated LOC**: 7
- **Dependencies**: TASK-004
- **Verification**: Compiles

### TASK-007: Add `showTranslate` to ArticleScreenViewState and ArticleState

- **Description**: Add `showTranslate: Boolean` to the `ArticleScreenViewState` interface and `ArticleState` data class to decouple translate button visibility from `showSummarize`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Location**: Interface at line 777, data class at line 749
- **Estimated LOC**: 2
- **Dependencies**: None
- **Verification**: Compiles

### TASK-008: Add `enableSummary` to ViewState Combine and Split showSummarize/showTranslate

- **Description**: Add `repository.enableSummary` as the 12th flow in the `viewState` combine. Derive `showSummarize` gated on `enableSummary` and derive independent `showTranslate`. Pass both to `ArticleState` constructor.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Location**: `combine` block (lines 144–207)
- **Estimated LOC**: 8
- **Dependencies**: TASK-004, TASK-007
- **Verification**: Compiles

### TASK-009: Gate Auto-Summary on `enableSummary` in Init Block

- **Description**: Add `repository.enableSummary` to the auto-summary combine in `init` and prepend `enableSummary &&` to the condition. This gates both global `summaryEnabled` AND per-feed `summarizeOnOpen`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Location**: Init block (lines 231–252)
- **Estimated LOC**: 6
- **Dependencies**: TASK-004
- **Verification**: Compiles

### BUILD CHECKPOINT: `./gradlew :app:compileFdroidDebugKotlin`

---

## Phase 3: UI Layer

### TASK-010: Add Disabled Alpha to SwitchSetting Row

- **Description**: Apply `Modifier.alpha(if (enabled) 1f else 0.38f)` to the Row in `SwitchSetting` composable so text labels appear greyed out when `enabled = false`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
- **Location**: Line 1317 (Row modifier chain)
- **Estimated LOC**: 2 (import + modifier)
- **Dependencies**: None
- **Verification**: Compiles. Existing SwitchSetting callers unaffected (all use `enabled = true` default).

### TASK-011: Add Master Toggle to SummarySettingsScreen

- **Description**: Collect `enableSummary` from ViewModel. Add new "Enable Summary" `SwitchSetting` above the existing Auto Summary toggle. Pass `enabled = enableSummary` to the Auto Summary `SwitchSetting`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
- **Location**: Lines 54 (state collection) and 91–97 (toggle section)
- **Estimated LOC**: 12
- **Dependencies**: TASK-005, TASK-006, TASK-010
- **Verification**: Compiles. Manual UI check: both toggles visible, Auto Summary disables when Enable Summary OFF.

### TASK-012: Use `showTranslate` for Translate Button in ArticleScreen

- **Description**: Change the translate button's visibility condition from `viewState.showSummarize` to `viewState.showTranslate`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- **Location**: Line 365
- **Estimated LOC**: 1
- **Dependencies**: TASK-008
- **Verification**: Compiles. Translate button visible when Enable Summary OFF.

### BUILD CHECKPOINT: `./gradlew :app:compileFdroidDebugKotlin`

### FUNCTIONAL VERIFICATION

Run the app and verify:
1. Settings: Enable Summary + Auto Summary toggles with correct hierarchy
2. Settings: Auto Summary greyed out when Enable Summary OFF
3. Article: Summarize button hidden when Enable Summary OFF
4. Article: Translate button always visible (AI configured)
5. Article: Auto-summary only when both toggles ON
6. Preference preservation across Enable Summary toggle cycle

---

## Phase 4: Tests

### TASK-013: Unit Tests for SettingsStore `enableSummary`

- **Description**: Add unit tests verifying default value, persistence, and independence from `summaryEnabled`.
- **File(s)**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
- **Estimated LOC**: 25–30
- **Dependencies**: TASK-002
- **Scenarios covered**: SCENARIO-011, SCENARIO-012, SCENARIO-015–019
- **Verification**: `./gradlew :app:testFdroidDebugUnitTest`

### TASK-014: Unit Tests for ArticleViewModel Auto-Summary Gating

- **Description**: Add tests for the four matrix states of (enableSummary, summaryEnabled) and the `summarizeOnOpen` edge case (R-3).
- **File(s)**: Existing ArticleViewModel test file or new test class
- **Estimated LOC**: 40–50
- **Dependencies**: TASK-008, TASK-009
- **Scenarios covered**: SCENARIO-001–005, SCENARIO-013
- **Verification**: `./gradlew :app:testFdroidDebugUnitTest`

### TASK-015: Unit Tests for showSummarize / showTranslate Independence

- **Description**: Verify `showSummarize` is gated on `enableSummary`, while `showTranslate` is independent.
- **File(s)**: Existing ArticleViewModel test file or new test class
- **Estimated LOC**: 20–30
- **Dependencies**: TASK-008
- **Scenarios covered**: SCENARIO-004, SCENARIO-005, SCENARIO-020
- **Verification**: `./gradlew :app:testFdroidDebugUnitTest`

### TEST CHECKPOINT: `./gradlew :app:testFdroidDebugUnitTest`

---

## Task Summary

| Task | Phase | File(s) | Est. LOC | Dependencies |
|:-----|:------|:--------|:---------|:------------|
| TASK-001 | Data | SettingsStore.kt | 3 | — |
| TASK-002 | Data | SettingsStore.kt | 8 | TASK-001 |
| TASK-003 | Data | SettingsStore.kt | 1 | TASK-001 |
| TASK-004 | Data | Repository.kt | 4 | TASK-002 |
| TASK-005 | Data | strings.xml | 4 | — |
| TASK-006 | VM | SummarySettingsViewModel.kt | 7 | TASK-004 |
| TASK-007 | VM | ArticleViewModel.kt | 2 | — |
| TASK-008 | VM | ArticleViewModel.kt | 8 | TASK-004, TASK-007 |
| TASK-009 | VM | ArticleViewModel.kt | 6 | TASK-004 |
| TASK-010 | UI | Settings.kt | 2 | — |
| TASK-011 | UI | SummarySettingsScreen.kt | 12 | TASK-005, TASK-006, TASK-010 |
| TASK-012 | UI | ArticleScreen.kt | 1 | TASK-008 |
| TASK-013 | Test | SettingsStoreTest.kt | 25–30 | TASK-002 |
| TASK-014 | Test | ArticleViewModel test | 40–50 | TASK-008, TASK-009 |
| TASK-015 | Test | ArticleViewModel test | 20–30 | TASK-008 |

**Total estimated LOC**: ~145–170 (production: ~60, tests: ~85–110)
