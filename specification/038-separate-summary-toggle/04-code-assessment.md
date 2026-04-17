# 038 - Separate Summary Toggle: Code Assessment

---

## 1. Current State

### 1.1 SettingsStore — Summary Enabled Setting

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

- **Lines 818–824**: `_summaryEnabled` / `summaryEnabled` / `setSummaryEnabled()` — currently serves as the **only** summary toggle. Preference key: `PREF_SUMMARY_ENABLED` = `"pref_summary_enabled"`, default: `true`.
- **Line 1029**: Constant `PREF_SUMMARY_ENABLED` declared.
- **Lines 1112–1113**: `UserSettings` enum includes `SETTING_SUMMARY_ENABLED` for OPML export.

**Pattern for adding a new boolean setting** (template from `_applyBlocklistToSummaries`, lines 509–518):
```
1. private val _field = MutableStateFlow(sp.getBoolean(PREF_KEY, default))
2. val field: StateFlow<Boolean> = _field.asStateFlow()
3. fun setField(value: Boolean) { _field.value = value; sp.edit().putBoolean(PREF_KEY, value).apply() }
4. Add PREF_KEY constant
5. Add UserSettings enum entry (for OPML)
```

### 1.2 Repository — Proxy Layer

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

- **Lines 383–386**: `summaryEnabled` and `setSummaryEnabled()` — simple pass-through to `settingsStore`.
- Follow identical pattern for new setting.

### 1.3 ArticleViewModel — Auto-Summary Trigger

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

- **Lines 231–250**: Init block that combines `articleFlow` with `repository.summaryEnabled` and triggers `summarize()` when:
  ```kotlin
  if ((summaryEnabled || feed?.summarizeOnOpen == true) && ...)
  ```
  This is the **auto-summarize on article open** logic. Currently `summaryEnabled` is the ONLY global gate.

- **Lines 168–169**: `showSummarize` derivation in `viewState` combine:
  ```kotlin
  val showSummarize = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
  ```
  Currently, `showSummarize` only depends on `aiSettings.isValid` and article having a link — it does NOT check `summaryEnabled`. This means the summarize button is visible even when the summary toggle is off (auto-summary is off, but manual summary is still possible — this is actually the desired behavior for the NEW spec).

### 1.4 ArticleScreen — Summarize Button

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

- **Line 340**: `if (viewState.showSummarize)` — gates visibility of the summarize `CircleProgressIconButton`.
- **Line 365**: Same `showSummarize` flag also gates the translate button visibility (both AI buttons share the same visibility condition).
- **Lines 352–360**: `CircleProgressIconButton` for summarize with cancel support.

### 1.5 Summary Settings Screen (UI)

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

- **Lines 92–97**: Single `SwitchSetting` for "Enable Auto Summary" (`summaryEnabled`). This is the toggle that needs to become the **auto-summary sub-toggle**.
- **Lines 102–108**: Language selector.
- **Lines 113–118**: Timeout setting.
- String resources (in `strings.xml` lines 322–324):
  - `summary_settings_title` = "Summary Settings"
  - `summary_enabled_title` = "Enable Auto Summary"
  - `summary_enabled_description` = "Automatically generate AI summaries for articles"

### 1.6 SummarySettingsViewModel

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

- **Line 24**: Exposes `repository.summaryEnabled` as `StateFlow<Boolean>`.
- **Lines 28–31**: `setSummaryEnabled()` method.
- Needs: new `enableSummary` flow + setter.

### 1.7 SettingsViewModel — Main Settings State

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

- **Lines 241–318**: `combine()` with 35 parameters (indices 0–34). The new setting is NOT needed here — it's consumed in the `SummarySettingsScreen` via `SummarySettingsViewModel`.
- The main settings screen navigates TO the summary settings screen, so no changes needed here.

### 1.8 SwitchSetting Composable

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

- **Lines 1306–1365**: `SwitchSetting` composable already accepts `enabled: Boolean = true` parameter.
- **Gap**: When `enabled = false`, the `clickable` is disabled and the `Switch` shows disabled state, BUT the text labels (title + description) do NOT get reduced opacity. This needs to be addressed per FR-005 requirement for "greyed out with reduced opacity."
- Fix: Add `Modifier.alpha(if (enabled) 1f else 0.38f)` to the Row, or wrap text in a `CompositionLocalProvider` with reduced alpha.

### 1.9 ArticleScreenViewState Interface

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

- **Line 749**: `val showSummarize: Boolean` — in the `ArticleScreenViewState` interface.
- **Line 198**: Used in `ArticleState` data class.

### 1.10 Feed-Level summarizeOnOpen

**File**: `app/src/main/java/com/nononsenseapps/feeder/db/room/Feed.kt` (line 63)

- Per-feed `summarizeOnOpen` boolean, stored in Room DB.
- Used in `ArticleViewModel` line 243: `(summaryEnabled || feed?.summarizeOnOpen == true)`.
- The new `enableSummary` master toggle must gate BOTH the global auto-summary AND per-feed `summarizeOnOpen`.

---

## 2. Change Points

### CP-1: New `enableSummary` setting in SettingsStore

**Where**: `SettingsStore.kt`, after `summaryEnabled` block (around line 824)

**What**:
- Add `PREF_ENABLE_SUMMARY = "pref_enable_summary"` constant
- Add `_enableSummary = MutableStateFlow(sp.getBoolean(PREF_ENABLE_SUMMARY, true))`
- Add `val enableSummary: StateFlow<Boolean>`
- Add `fun setEnableSummary(value: Boolean)`
- Add `SETTING_ENABLE_SUMMARY` to `UserSettings` enum

**Default**: `true` (backward compatible — existing users see no change)

### CP-2: Repository proxy

**Where**: `Repository.kt`, after `summaryEnabled` (around line 386)

**What**: Add `val enableSummary` and `fun setEnableSummary(value: Boolean)`.

### CP-3: SummarySettingsViewModel — add new flow

**Where**: `SummarySettingsViewModel.kt`

**What**:
- Add `val enableSummary: StateFlow<Boolean> = repository.enableSummary`
- Add `fun setEnableSummary(enabled: Boolean)` method

### CP-4: SummarySettingsScreen — add master toggle + disable sub-toggle

**Where**: `SummarySettingsScreen.kt`, lines 92–97

**What**:
- Add new `SwitchSetting` for "Enable Summary" **above** the existing "Enable Auto Summary" toggle
- Pass `enabled = enableSummary` to the existing auto-summary `SwitchSetting`
- Collect the new `enableSummary` state from ViewModel

### CP-5: SwitchSetting — add disabled text alpha

**Where**: `Settings.kt`, lines 1317–1333 (the Row modifier in `SwitchSetting`)

**What**: Apply `Modifier.alpha(if (enabled) 1f else 0.38f)` to the Row to grey out text labels when disabled.

**Risk**: This is a shared composable — need to verify existing callers pass `enabled` correctly or default to `true`. Currently, all callers use the default `enabled = true`, so adding alpha has zero impact on existing UIs.

### CP-6: ArticleViewModel — gate auto-summary on enableSummary

**Where**: `ArticleViewModel.kt`, lines 231–250 (init block)

**What**: Change the combine to include `repository.enableSummary`:
```kotlin
combine(
    articleFlow,
    repository.summaryEnabled,
    repository.enableSummary,
) { article, summaryEnabled, enableSummary ->
    Triple(article, summaryEnabled, enableSummary)
}
```
Gate: `if (enableSummary && (summaryEnabled || feed?.summarizeOnOpen == true) && ...)`

### CP-7: ArticleViewModel — gate showSummarize on enableSummary

**Where**: `ArticleViewModel.kt`, line 169

**What**: Include `repository.enableSummary` in the viewState combine and change:
```kotlin
val showSummarize = enableSummary && (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
```

This requires adding `repository.enableSummary` as a new flow to the `combine()` call (currently 11 params, becomes 12).

### CP-8: String resources

**Where**: `app/src/main/res/values/strings.xml`

**What**: Add new strings:
- `enable_summary_title` = "Enable Summary"
- `enable_summary_description` = "Enable AI summary functionality"

Optionally rename existing `summary_enabled_title` from "Enable Auto Summary" to "Auto Summary" for clarity (to distinguish it from the new master toggle).

---

## 3. Dependencies

```
CP-1 (SettingsStore) ← CP-2 (Repository) ← CP-3 (ViewModel)
                                            ← CP-6 (ArticleVM auto-summary)
                                            ← CP-7 (ArticleVM showSummarize)
CP-5 (SwitchSetting alpha) ← CP-4 (Settings UI)
CP-8 (Strings) ← CP-4 (Settings UI)
```

CP-1 must come first. CP-5 and CP-8 are independent of CP-1. CP-4 depends on CP-1, CP-3, CP-5, and CP-8.

---

## 4. Risks

### R-1: SwitchSetting alpha change is global (LOW)
`SwitchSetting` is used by many settings. Adding alpha for disabled state affects all callers. **Mitigated**: All existing callers use `enabled = true` (the default), so alpha will always be `1f` for them. Only the auto-summary toggle will pass `enabled = false`.

### R-2: ViewState combine parameter count (LOW)
`ArticleViewModel.viewState` combine has 11 params (Kotlin `combine` vararg supports up to ~26 with the array overload). Adding 1 more is safe. The `SettingsViewModel` combine (35 params) is not affected.

### R-3: Feed-level summarizeOnOpen override (MEDIUM)
Currently `feed?.summarizeOnOpen == true` can trigger auto-summary even when global `summaryEnabled` is false. The new `enableSummary` master toggle must gate this too. If missed, feeds with `summarizeOnOpen = true` would still auto-summarize even when the user turned off all summary functionality.

### R-4: Translate button shares showSummarize condition (MEDIUM)
`ArticleScreen.kt` line 365 reuses `showSummarize` for the translate button. Gating `showSummarize` on `enableSummary` would accidentally hide the translate button too. **Solution**: Either (a) introduce a separate `showTranslate` flag in `ArticleScreenViewState`, or (b) only change the summarize button's visibility condition while keeping translate independent. Option (b) is simpler: gate the summarize `if` block on `enableSummary` in the composable, or pass a separate `showSummarize`/`showTranslate` from the ViewModel.

**Recommendation**: Split `showSummarize` into two independent flags: `showSummarize` (gated on `enableSummary && aiSettings.isValid && hasLink`) and `showTranslate` (gated on `aiSettings.isValid && hasLink`, unchanged). This is the cleanest approach.

### R-5: No existing tests for summaryEnabled in SettingsStoreTest (LOW)
The `SettingsStoreTest` has no tests for `summaryEnabled` or `PREF_SUMMARY_ENABLED`. New tests should be added for the new `enableSummary` setting following the `applyBlocklistToSummaries` test pattern (lines 397–422).

### R-6: Cached summary still accessible when enableSummary=OFF (NONE)
Per requirements, cached summaries remain accessible. The current code doesn't clear summaries when the toggle changes — no action needed.

---

## 5. Recommended Change Order

### Phase 1: Data Layer (no UI impact)
1. **CP-1**: Add `enableSummary` to `SettingsStore` (+ constant + UserSettings enum)
2. **CP-2**: Add `enableSummary` proxy to `Repository`
3. **CP-8**: Add string resources

### Phase 2: ViewModel Layer
4. **CP-3**: Add `enableSummary` to `SummarySettingsViewModel`
5. **CP-7**: Add `enableSummary` to `ArticleViewModel.viewState` combine + split `showSummarize` / `showTranslate`
6. **CP-6**: Gate auto-summary logic on `enableSummary` in `ArticleViewModel.init`

### Phase 3: UI Layer
7. **CP-5**: Fix `SwitchSetting` disabled alpha (shared composable)
8. **CP-4**: Add "Enable Summary" master toggle to `SummarySettingsScreen` + wire up auto-summary disabled state

### Phase 4: Article Screen
9. Update `ArticleScreen.kt` to use separate `showSummarize` / `showTranslate` flags (if split in CP-7)

### Phase 5: Tests
10. Unit tests for `SettingsStore.enableSummary`
11. Unit tests for `ArticleViewModel` auto-summary gating
12. UI behavior verification

---

## 6. Existing Patterns

### Boolean setting pattern (SettingsStore)
Example: `applyBlocklistToSummaries` (lines 509–518)
```kotlin
private val _applyBlocklistToSummaries =
    MutableStateFlow(sp.getBoolean(PREF_BLOCKLIST_APPLY_TO_SUMMARIES, false))
val applyBlocklistToSummaries: StateFlow<Boolean> = _applyBlocklistToSummaries.asStateFlow()

fun setApplyBlocklistToSummaries(value: Boolean) {
    _applyBlocklistToSummaries.value = value
    sp.edit().putBoolean(PREF_BLOCKLIST_APPLY_TO_SUMMARIES, value).apply()
}
```

### Repository proxy pattern
Example: `Repository.kt` lines 383–386
```kotlin
val summaryEnabled = settingsStore.summaryEnabled
fun setSummaryEnabled(value: Boolean) = settingsStore.setSummaryEnabled(value)
```

### SwitchSetting with enabled parameter
Example: Used in `EditFeedScreen.kt` for `summarizeOnOpen` (without `enabled` param, but the composable supports it).

### Settings screen structure
The `SummarySettingsScreen` follows the same pattern as `TranslationSettingsScreen` — a dedicated ViewModel + Scaffold + Column of settings.

---

## Summary

| Change Point | File | Impact | Complexity |
|:---|:---|:---|:---|
| CP-1: enableSummary in SettingsStore | SettingsStore.kt | Data layer | Low |
| CP-2: Repository proxy | Repository.kt | Data layer | Low |
| CP-3: SummarySettingsViewModel | SummarySettingsViewModel.kt | ViewModel | Low |
| CP-4: Summary Settings UI | SummarySettingsScreen.kt | UI | Low |
| CP-5: SwitchSetting disabled alpha | Settings.kt | Shared UI | Low |
| CP-6: Auto-summary gating | ArticleViewModel.kt (init) | Logic | Medium |
| CP-7: showSummarize split | ArticleViewModel.kt (viewState) | Logic | Medium |
| CP-8: String resources | strings.xml | Resources | Low |

**Total estimated LOC changed**: ~80–100 lines across 8 files. No new files needed.
