# 038 - Separate Summary Toggle: Implementation Plan

---

## Phase Overview

| Phase | Description | Files | Build Check |
|:------|:-----------|:------|:------------|
| Phase 1 | Data Layer | SettingsStore, Repository, strings.xml | Compile |
| Phase 2 | ViewModel Layer | SummarySettingsViewModel, ArticleViewModel | Compile |
| Phase 3 | UI Layer | Settings.kt, SummarySettingsScreen, ArticleScreen | Compile + Run |
| Phase 4 | Tests | New test files / additions | Compile + Test |

---

## Phase 1: Data Layer (No UI Impact)

### Step 1.1: Add `PREF_ENABLE_SUMMARY` Constant

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**What**: Add a new constant after `PREF_SUMMARY_ENABLED` (line 1029):
```kotlin
const val PREF_ENABLE_SUMMARY = "pref_enable_summary"
```

**Dependencies**: None.

### Step 1.2: Add `enableSummary` Field in SettingsStore

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**What**: After the `summaryEnabled` block (line 824), add:
- `_enableSummary` MutableStateFlow (default `true`)
- `enableSummary` public StateFlow
- `setEnableSummary()` method

**Dependencies**: Step 1.1.

### Step 1.3: Add `SETTING_ENABLE_SUMMARY` to UserSettings Enum

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**What**: Add new enum entry after `SETTING_SUMMARY_ENABLED` (line 1112):
```kotlin
SETTING_ENABLE_SUMMARY(key = PREF_ENABLE_SUMMARY),
```

**Dependencies**: Step 1.1.

### Step 1.4: Add Repository Proxy

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

**What**: After `summaryEnabled` proxy (line 386), add:
- `val enableSummary = settingsStore.enableSummary`
- `fun setEnableSummary(value: Boolean) = settingsStore.setEnableSummary(value)`

**Dependencies**: Step 1.2.

### Step 1.5: Add String Resources

**File**: `app/src/main/res/values/strings.xml`

**What**:
1. Add new strings after line 324:
   - `enable_summary_title` = "Enable Summary"
   - `enable_summary_description` = "Enable AI summary functionality"
2. Rename existing strings (lines 323–324):
   - `summary_enabled_title` → "Auto Summary"
   - `summary_enabled_description` → "Automatically summarize articles when opened"

**Dependencies**: None.

### Build Checkpoint 1

```bash
./gradlew :app:compileFdroidDebugKotlin
```

Expected: Compile success. No UI changes visible yet.

---

## Phase 2: ViewModel Layer

### Step 2.1: Add `enableSummary` to SummarySettingsViewModel

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**What**:
- Add `val enableSummary: StateFlow<Boolean> = repository.enableSummary` (after line 24)
- Add `fun setEnableSummary(enabled: Boolean)` method (after line 32)

**Dependencies**: Step 1.4.

### Step 2.2: Add `showTranslate` to ArticleScreenViewState Interface

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**What**:
1. Add `val showTranslate: Boolean` to `ArticleScreenViewState` interface (after `showSummarize` at line 777)
2. Add `override val showTranslate: Boolean = false` to `ArticleState` data class (after `showSummarize` at line 749)

**Dependencies**: None.

### Step 2.3: Split `showSummarize` / `showTranslate` in ViewState Combine

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**What** (in the `combine` block starting at line 144):
1. Add `repository.enableSummary` as 12th flow parameter
2. Extract `val enableSummary = params[11] as Boolean`
3. Change `showSummarize` derivation:
   ```kotlin
   val aiValid = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
   val showSummarize = enableSummary && aiValid
   val showTranslate = aiValid
   ```
4. Pass `showTranslate = showTranslate` in the `ArticleState` constructor

**Dependencies**: Steps 1.4, 2.2.

### Step 2.4: Gate Auto-Summary on `enableSummary`

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**What** (in the init block, lines 231–252):
1. Add `repository.enableSummary` to the `combine`
2. Extract `enableSummary` from the triple (now a `Triple`)
3. Add `enableSummary &&` before the existing condition at line 243

**Dependencies**: Step 1.4.

### Build Checkpoint 2

```bash
./gradlew :app:compileFdroidDebugKotlin
```

Expected: Compile success. ArticleScreen.kt may show a warning about unused `showTranslate` — that's expected and will be resolved in Phase 3.

---

## Phase 3: UI Layer

### Step 3.1: Add Disabled Alpha to SwitchSetting

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**What**:
1. Add `import androidx.compose.ui.draw.alpha` (if not already imported)
2. In `SwitchSetting` composable (line 1317), add `.alpha(if (enabled) 1f else 0.38f)` to the Row modifier, before the `.clickable()` modifier

**Dependencies**: None.

### Step 3.2: Add Master Toggle to SummarySettingsScreen

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**What**:
1. Collect `enableSummary` state: `val enableSummary by viewModel.enableSummary.collectAsStateWithLifecycle()`
2. Add new `SwitchSetting` for "Enable Summary" above the existing toggle (before line 92)
3. Add `enabled = enableSummary` parameter to the existing Auto Summary `SwitchSetting`

**Dependencies**: Steps 1.5, 2.1, 3.1.

### Step 3.3: Use `showTranslate` in ArticleScreen

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**What**: Change line 365 from:
```kotlin
if (viewState.showSummarize) {
```
to:
```kotlin
if (viewState.showTranslate) {
```

**Dependencies**: Step 2.3.

### Build Checkpoint 3

```bash
./gradlew :app:compileFdroidDebugKotlin
```

Expected: Compile success. The feature is now functionally complete.

### Functional Verification

Run the app and manually verify:
1. Settings screen shows both toggles with correct hierarchy
2. Toggling Enable Summary OFF disables Auto Summary (greyed out)
3. Article view: summarize button hidden when Enable Summary OFF
4. Article view: translate button always visible (when AI configured)
5. Auto-summary triggers only when both Enable Summary and Auto Summary are ON

---

## Phase 4: Tests

### Step 4.1: SettingsStore Unit Tests

**File**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` (or new section)

**What**: Add tests for `enableSummary`:
- Default value is `true`
- `setEnableSummary(false)` persists and is observable
- `enableSummary` and `summaryEnabled` are independent

**Dependencies**: Step 1.2.

### Step 4.2: ArticleViewModel Unit Tests

**File**: Existing ArticleViewModel test file (or new)

**What**: Add tests for:
- Auto-summary gating (4 matrix states + summarizeOnOpen edge case)
- `showSummarize` depends on `enableSummary`
- `showTranslate` independent of `enableSummary`

**Dependencies**: Steps 2.3, 2.4.

### Test Checkpoint

```bash
./gradlew :app:testFdroidDebugUnitTest
```

Expected: All new tests pass. All existing tests pass (4 pre-existing flaky tests excluded).

---

## Dependency Graph

```
Step 1.1 ──► Step 1.2 ──► Step 1.4 ──► Step 2.1
         │                          └──► Step 2.3
         └──► Step 1.3                   └──► Step 3.3
                                    └──► Step 2.4
Step 1.5 ──────────────────────────────► Step 3.2

(independent) ────────────────────────► Step 3.1 ──► Step 3.2

Step 2.2 ──► Step 2.3

Step 1.2 ──────────────────────────────► Step 4.1
Steps 2.3, 2.4 ────────────────────────► Step 4.2
```

---

## Risk Mitigation Checkpoints

| Risk | Check | When |
|:---|:---|:---|
| R-1: SwitchSetting alpha affects other callers | Verify no other caller passes `enabled=false` | Before Step 3.1 |
| R-3: summarizeOnOpen bypasses enableSummary | Test with per-feed summarizeOnOpen=true + enableSummary=false | Step 4.2 |
| R-4: Translate button accidentally hidden | Verify translate visible when enableSummary=false | After Step 3.3 |
| R-2: Combine param count overflow | Count params (12 total, well within limit) | Step 2.3 |
