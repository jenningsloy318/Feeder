# 038 - Separate Summary Toggle: Implementation Specification

---

## 1. Overview

### 1.1 Goal

Introduce a new master toggle ("Enable Summary") that controls whether summary functionality is available at all, while the existing toggle ("Auto Summary") controls only the automatic summarization behavior. This separation lets users:

- Use manual summarization without auto-summary (Enable Summary ON, Auto Summary OFF)
- Disable all summary features entirely (Enable Summary OFF)
- Retain the current full auto-summary behavior (both ON)

### 1.2 Scope

- **In scope**: New `enableSummary` setting, settings UI hierarchy, `ArticleViewModel` gating, `showSummarize`/`showTranslate` split, `SwitchSetting` disabled alpha
- **Out of scope**: Translation toggles, per-feed settings, AI provider/client changes, summary cache behavior, blocklist settings

### 1.3 Guiding Principles

- Default `true` for backward compatibility (NFR-001)
- Store settings independently; derive effective state in ViewModel/UI (FR-003)
- Minimal code impact: ~80–100 LOC across 8 existing files, no new files (NFR-002)

---

## 2. Architecture Changes

The change flows through three layers:

```
Data Layer                    ViewModel Layer                  UI Layer
─────────────                 ──────────────                   ────────

SettingsStore                 SummarySettingsViewModel         SummarySettingsScreen
  + enableSummary    ──►        + enableSummary       ──►       + "Enable Summary" toggle
  + setEnableSummary            + setEnableSummary              + Auto Summary disabled state

Repository (proxy)            ArticleViewModel
  + enableSummary    ──►        + gate auto-summary   ──►     ArticleScreen
  + setEnableSummary            + showSummarize split           + separate showSummarize
                                + showTranslate                 + separate showTranslate
```

---

## 3. Detailed Implementation

### 3.1 CP-1: New `enableSummary` Setting in SettingsStore

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Pattern**: Follow the `_summaryEnabled` pattern at lines 817–824.

**Changes**:

1. **Add preference constant** (after line 1029, alongside `PREF_SUMMARY_ENABLED`):
   ```kotlin
   const val PREF_ENABLE_SUMMARY = "pref_enable_summary"
   ```

2. **Add MutableStateFlow + public accessor + setter** (after line 824, after the `summaryEnabled` block):
   ```kotlin
   private val _enableSummary = MutableStateFlow(sp.getBoolean(PREF_ENABLE_SUMMARY, true))
   val enableSummary = _enableSummary.asStateFlow()

   fun setEnableSummary(value: Boolean) {
       _enableSummary.value = value
       sp.edit().putBoolean(PREF_ENABLE_SUMMARY, value).apply()
   }
   ```

3. **Add UserSettings enum entry** (after `SETTING_SUMMARY_ENABLED` at line 1112):
   ```kotlin
   SETTING_ENABLE_SUMMARY(key = PREF_ENABLE_SUMMARY),
   ```

**Default**: `true` — existing users see no behavior change on upgrade (NFR-001).

### 3.2 CP-2: Repository Proxy

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

**Pattern**: Follow lines 383–386 (`summaryEnabled` proxy).

**Changes** (after line 386):
```kotlin
val enableSummary = settingsStore.enableSummary

fun setEnableSummary(value: Boolean) = settingsStore.setEnableSummary(value)
```

### 3.3 CP-8: String Resources

**File**: `app/src/main/res/values/strings.xml`

**Add** (after line 324):
```xml
<string name="enable_summary_title">Enable Summary</string>
<string name="enable_summary_description">Enable AI summary functionality</string>
```

**Rename** existing strings (lines 323–324):
```xml
<string name="summary_enabled_title">Auto Summary</string>
<string name="summary_enabled_description">Automatically summarize articles when opened</string>
```

The rename from "Enable Auto Summary" to "Auto Summary" prevents user confusion between the two toggles.

### 3.4 CP-3: SummarySettingsViewModel

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Changes**:

Add alongside the existing `summaryEnabled` property (after line 24):
```kotlin
val enableSummary: StateFlow<Boolean> = repository.enableSummary
```

Add setter method (after `setSummaryEnabled` at line 32):
```kotlin
fun setEnableSummary(enabled: Boolean) {
    viewModelScope.launch {
        repository.setEnableSummary(enabled)
    }
}
```

### 3.5 CP-7: ArticleViewModel — Split `showSummarize` / `showTranslate`

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Problem (R-4)**: Currently, `showSummarize` at line 169 gates both the summarize AND translate buttons in `ArticleScreen.kt` (line 340 and line 365). Gating `showSummarize` on `enableSummary` would accidentally hide the translate button.

**Solution**: Add `repository.enableSummary` to the viewState `combine` and introduce a separate `showTranslate` flag.

**Changes to the `combine` block** (lines 144–207):

1. Add `repository.enableSummary` as a 12th flow in the `combine`:
   ```kotlin
   val viewState: StateFlow<ArticleScreenViewState> =
       combine(
           articleFlow,
           textToDisplay,
           articleContentFlow,
           toolbarVisible,
           repository.linkOpener,
           repository.useDetectLanguage,
           ttsStateHolder.ttsState,
           ttsStateHolder.availableLanguages,
           repository.aiSettingsFlow,
           aiSummary,
           translationState,
           repository.enableSummary,      // NEW: 12th param
       ) { params ->
   ```

2. Extract the new parameter and derive two separate flags:
   ```kotlin
   val enableSummary = params[11] as Boolean

   val aiValid = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
   val showSummarize = enableSummary && aiValid
   val showTranslate = aiValid
   ```

**Changes to `ArticleScreenViewState` interface** (line 777):

Add:
```kotlin
val showTranslate: Boolean
```

**Changes to `ArticleState` data class** (line 749):

Add:
```kotlin
override val showTranslate: Boolean = false,
```

And in the constructor call inside `combine` (after `showSummarize = showSummarize`):
```kotlin
showTranslate = showTranslate,
```

### 3.6 CP-6: ArticleViewModel — Gate Auto-Summary on `enableSummary`

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Where**: Init block, lines 231–252.

**Problem (R-3)**: The current condition `(summaryEnabled || feed?.summarizeOnOpen == true)` allows per-feed `summarizeOnOpen` to bypass the global toggle. The new `enableSummary` master toggle must gate BOTH.

**Change**: Add `repository.enableSummary` to the combine and gate the entire condition:

```kotlin
viewModelScope.launch {
    combine(
        articleFlow,
        repository.summaryEnabled,
        repository.enableSummary,         // NEW
    ) { article, summaryEnabled, enableSummary ->
        Triple(article, summaryEnabled, enableSummary)
    }.filterNotNull()
        .collect { (article, summaryEnabled, enableSummary) ->
            val feedId = article?.item?.feedId
            if (feedId != null) {
                val feed = repository.getFeed(feedId)
                if (enableSummary &&      // NEW: master gate
                    (summaryEnabled || feed?.summarizeOnOpen == true) &&
                    aiSummary.value is AISummaryState.Empty &&
                    article?.link != null
                ) {
                    summarize()
                    return@collect
                }
            }
        }
}
```

This ensures:
- `enableSummary = false` → no auto-summary regardless of `summaryEnabled` or `summarizeOnOpen`
- `enableSummary = true, summaryEnabled = false, summarizeOnOpen = false` → no auto-summary
- `enableSummary = true, summaryEnabled = true` → auto-summary (existing behavior)
- `enableSummary = true, summarizeOnOpen = true` → auto-summary for that feed (existing behavior)

### 3.7 CP-5: SwitchSetting Disabled Alpha

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Where**: `SwitchSetting` composable, lines 1306–1365.

**Problem**: When `enabled = false`, the `Switch` component shows its own disabled state, but the text labels (title + description) remain at full opacity.

**Change**: Apply `Modifier.alpha()` to the Row:

```kotlin
import androidx.compose.ui.draw.alpha

@Composable
fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: @Composable (() -> Unit)? = {},
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dimens = LocalDimens.current
    Row(
        modifier =
            modifier
                .width(dimens.maxContentWidth)
                .heightIn(min = 64.dp)
                .alpha(if (enabled) 1f else 0.38f)   // NEW: disabled alpha
                .clickable(
                    enabled = enabled,
                    onClick = { onCheckedChange(!checked) },
                )
                // ... rest unchanged
```

**Risk assessment (R-1)**: All existing callers of `SwitchSetting` use the default `enabled = true`, so alpha is always `1f` for them. The `0.38f` value is the Material 3 standard disabled content alpha.

### 3.8 CP-4: SummarySettingsScreen — Master Toggle + Sub-Toggle Disabled State

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**Changes**:

1. **Collect the new state** (after line 54):
   ```kotlin
   val enableSummary by viewModel.enableSummary.collectAsStateWithLifecycle()
   ```

2. **Add new SwitchSetting above existing toggle** (replace lines 91–97):
   ```kotlin
   // Master toggle: Enable Summary
   SwitchSetting(
       title = stringResource(R.string.enable_summary_title),
       checked = enableSummary,
       onCheckedChange = { viewModel.setEnableSummary(it) },
       description = stringResource(R.string.enable_summary_description),
   )

   Spacer(modifier = Modifier.height(8.dp))

   // Sub-toggle: Auto Summary (dependent on master)
   SwitchSetting(
       title = stringResource(R.string.summary_enabled_title),
       checked = summaryEnabled,
       onCheckedChange = { viewModel.setSummaryEnabled(it) },
       description = stringResource(R.string.summary_enabled_description),
       enabled = enableSummary,    // disabled when master is OFF
   )
   ```

3. The existing language and timeout settings remain unchanged and interactive regardless of `enableSummary` — they configure how summaries behave when eventually re-enabled.

### 3.9 CP-4b: ArticleScreen — Use Separate `showTranslate` Flag

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Where**: Line 365.

**Change**: Replace `if (viewState.showSummarize)` for the translate button with `if (viewState.showTranslate)`:

```kotlin
// Translate button (conditional)
if (viewState.showTranslate) {
```

The summarize button condition at line 340 remains `if (viewState.showSummarize)` — unchanged.

---

## 4. Error Handling

### 4.1 In-Progress Summary Cancellation (SCENARIO-013)

When `enableSummary` changes to `false` while a summary is in progress, the `showSummarize` flag becomes `false` in the next viewState emission. The summarize button (including the cancel button) disappears from the UI. The underlying coroutine (`summarizeJob`) continues to completion but the result is still stored in `aiSummary` state — this is acceptable because:

- Cached summaries remain accessible regardless of toggle state (SCENARIO-021)
- The button is hidden, so the user cannot interact with it
- No additional network cost beyond the already-sent request

Explicit cancellation on toggle change is a low-priority edge case per FR-003. If desired in the future, it can be added by observing `enableSummary` changes in `ArticleViewModel.init` and calling `cancelSummarize()`.

### 4.2 Rapid Toggling (SCENARIO-022)

The `StateFlow` nature of `enableSummary` means only the latest value is observed. Rapid toggling produces no race conditions because:

- `viewState` combine recomputes on each emission
- Auto-summary `collect` block checks `enableSummary` on each emission
- No debouncing is needed; `StateFlow` conflation handles rapid changes

### 4.3 Default Value Safety (SCENARIO-018, SCENARIO-019)

The default value of `true` for `enableSummary` ensures:

- New installations: summary is available by default
- Existing users: no behavior change on upgrade
- No migration needed — `SharedPreferences.getBoolean()` returns the default on first access

---

## 5. Testing Strategy

### 5.1 Unit Tests — SettingsStore

| Test | Scenario |
|:---|:---|
| `enableSummary defaults to true` | SCENARIO-018, SCENARIO-019 |
| `setEnableSummary(false) persists` | SCENARIO-015–017 |
| `enableSummary and summaryEnabled are independent` | SCENARIO-011, SCENARIO-012 |

Follow the pattern from `applyBlocklistToSummaries` tests in `SettingsStoreTest`.

### 5.2 Unit Tests — ArticleViewModel Auto-Summary Gating

| Test | Scenarios |
|:---|:---|
| `enableSummary=true, summaryEnabled=true → auto-summarize` | SCENARIO-001 |
| `enableSummary=true, summaryEnabled=false → no auto-summarize` | SCENARIO-002 |
| `enableSummary=false, summaryEnabled=true → no auto-summarize` | SCENARIO-004 |
| `enableSummary=false, summaryEnabled=false → no auto-summarize` | SCENARIO-005 |
| `enableSummary=false, summarizeOnOpen=true → no auto-summarize` | SCENARIO-004 (R-3) |
| `enableSummary=true, summarizeOnOpen=true → auto-summarize` | Existing behavior |

### 5.3 Unit Tests — ArticleViewModel showSummarize / showTranslate

| Test | Scenarios |
|:---|:---|
| `enableSummary=true → showSummarize=true (if aiSettings valid)` | SCENARIO-001–003 |
| `enableSummary=false → showSummarize=false` | SCENARIO-004, SCENARIO-005 |
| `enableSummary=false → showTranslate=true (if aiSettings valid)` | SCENARIO-020 |
| `enableSummary=true → showTranslate=true (if aiSettings valid)` | SCENARIO-020 |

### 5.4 Unit Tests — SummarySettingsViewModel

| Test | Scenarios |
|:---|:---|
| `enableSummary flow exposes repository value` | SCENARIO-006–008 |
| `setEnableSummary updates repository` | SCENARIO-009, SCENARIO-010 |

### 5.5 Compose UI Tests (Optional)

| Test | Scenarios |
|:---|:---|
| `Enable Summary OFF → Auto Summary toggle disabled` | SCENARIO-006 |
| `Enable Summary ON → Auto Summary toggle interactive` | SCENARIO-007 |
| `Enable Summary above Auto Summary in layout` | SCENARIO-008 |
| `Disabled SwitchSetting has reduced alpha` | CP-5 verification |

### 5.6 Manual QA Verification

| Scenario Group | What to Verify |
|:---|:---|
| SCENARIO-001–005 | All four matrix states produce correct article view behavior |
| SCENARIO-006–010 | Settings UI interactions |
| SCENARIO-011–012 | Preference preservation across toggle cycles |
| SCENARIO-013–014 | Cancellation behavior |
| SCENARIO-015–017 | Persistence across app restart |
| SCENARIO-020 | Translation unaffected |
| SCENARIO-021 | Cached summaries still accessible |
| SCENARIO-022 | Rapid toggling stability |

---

## 6. Migration & Backward Compatibility

### 6.1 No Migration Required

The new `enableSummary` setting uses `SharedPreferences.getBoolean(PREF_ENABLE_SUMMARY, true)`. On first access (upgrade or new install), it returns `true` — preserving existing behavior.

### 6.2 OPML Export

The new `SETTING_ENABLE_SUMMARY` entry in `UserSettings` ensures the setting is included in OPML exports. On import, if the key is missing (older OPML), the default `true` applies.

### 6.3 Existing `summaryEnabled` Unchanged

The existing `PREF_SUMMARY_ENABLED` / `summaryEnabled` field retains its key, default, and storage. It now represents "Auto Summary" instead of the only summary toggle. Its stored value is preserved when `enableSummary` is toggled OFF and back ON (AC-006).

### 6.4 String Resource Rename

Renaming `summary_enabled_title` from "Enable Auto Summary" to "Auto Summary" is purely cosmetic and affects no stored data. The preference key `PREF_SUMMARY_ENABLED` remains unchanged.

---

## 7. Files Changed Summary

| File | Change | CP |
|:---|:---|:---|
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` | New `enableSummary` field + constant + enum | CP-1 |
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` | New `enableSummary` proxy | CP-2 |
| `app/src/main/res/values/strings.xml` | New strings + rename existing | CP-8 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt` | New `enableSummary` flow + setter | CP-3 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | Gate auto-summary + split showSummarize/showTranslate | CP-6, CP-7 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt` | Disabled alpha on SwitchSetting Row | CP-5 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt` | New master toggle + sub-toggle `enabled` | CP-4 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | Use `showTranslate` for translate button | CP-4b |
