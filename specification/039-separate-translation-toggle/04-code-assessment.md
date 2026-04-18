# 039 - Separate Translation Toggle: Code Assessment

**Date**: 2026-04-18
**Status**: Assessment complete

---

## 1. Reference Pattern (enableSummary from spec-038)

### SettingsStore.kt — enableSummary pattern

**Lines 826-832**: MutableStateFlow + pref constant + getter/setter:
```kotlin
private val _enableSummary = MutableStateFlow(sp.getBoolean(PREF_ENABLE_SUMMARY, true))
val enableSummary = _enableSummary.asStateFlow()

fun setEnableSummary(value: Boolean) {
    _enableSummary.value = value
    sp.edit().putBoolean(PREF_ENABLE_SUMMARY, value).apply()
}
```

**Line 1038**: Pref constant:
```kotlin
const val PREF_ENABLE_SUMMARY = "pref_enable_summary"
```

**Lines 1122-1123**: UserSettings enum entries:
```kotlin
SETTING_SUMMARY_ENABLED(key = PREF_SUMMARY_ENABLED),
SETTING_ENABLE_SUMMARY(key = PREF_ENABLE_SUMMARY),
```

### Repository.kt — enableSummary proxy

**Lines 389-391**:
```kotlin
val enableSummary = settingsStore.enableSummary

fun setEnableSummary(value: Boolean) = settingsStore.setEnableSummary(value)
```

### SummarySettingsViewModel.kt — enableSummary exposure

**Line 25**: StateFlow exposure:
```kotlin
val enableSummary: StateFlow<Boolean> = repository.enableSummary
```

**Lines 35-37**: Setter:
```kotlin
fun setEnableSummary(enabled: Boolean) {
    viewModelScope.launch { repository.setEnableSummary(enabled) }
}
```

### SummarySettingsScreen.kt — Two-toggle UI pattern

**Lines 92-109**: Master toggle + dependent auto-toggle:
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
    enabled = enableSummary,  // <-- KEY: greyed out when master is OFF
)
```

### ArticleViewModel.kt — enableSummary in viewState combine

**Lines 145-158**: The combine takes 12 flows (indices 0-11), with `enableSummary` at index 11:
```kotlin
combine(
    articleFlow,             // [0]
    textToDisplay,           // [1]
    articleContentFlow,      // [2]
    toolbarVisible,          // [3]
    repository.linkOpener,   // [4]
    repository.useDetectLanguage, // [5]
    ttsStateHolder.ttsState, // [6]
    ttsStateHolder.availableLanguages, // [7]
    repository.aiSettingsFlow, // [8]
    aiSummary,               // [9]
    translationState,        // [10]
    repository.enableSummary, // [11]
) { params ->
```

**Lines 170-173**: Gates showSummarize:
```kotlin
val enableSummary = params[11] as Boolean
val aiValid = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
val showSummarize = enableSummary && aiValid
val showTranslate = aiValid   // <-- NOT gated by enableTranslation yet
```

### ArticleViewModel.kt — enableSummary in auto-summary combine (init block)

**Lines 236-257**: The auto-summary combine includes `enableSummary` in its condition:
```kotlin
combine(
    articleFlow,
    repository.summaryEnabled,
    repository.enableSummary,
) { article, summaryEnabled, enableSummary ->
    // ...
    if (enableSummary && (summaryEnabled || feed?.summarizeOnOpen == true) && ...)
```

### OPMLImporter.kt — enableSummary import

**Line 164**:
```kotlin
UserSettings.SETTING_ENABLE_SUMMARY -> settingsStore.setEnableSummary(value.toBoolean())
```

---

## 2. Current Translation State

### SettingsStore.kt — existing translation settings

**Lines 855-862**: `translationEnabled` (the auto-translate toggle):
```kotlin
private val _translationEnabled = MutableStateFlow(sp.getBoolean(PREF_TRANSLATION_ENABLED, false))
val translationEnabled = _translationEnabled.asStateFlow()

fun setTranslationEnabled(value: Boolean) {
    _translationEnabled.value = value
    sp.edit().putBoolean(PREF_TRANSLATION_ENABLED, value).apply()
}
```

**Line 1049**: Pref constant:
```kotlin
const val PREF_TRANSLATION_ENABLED = "pref_translation_enabled"
```

**NOTE**: There is NO `SETTING_TRANSLATION_ENABLED` entry in the `UserSettings` enum. Translation enabled/language/timeout are NOT exported via OPML currently.

### Repository.kt — existing translation proxies

**Lines 403-406**:
```kotlin
val translationEnabled = settingsStore.translationEnabled
fun setTranslationEnabled(value: Boolean) = settingsStore.setTranslationEnabled(value)
```

### TranslationSettingsViewModel.kt

**Lines 24-32**: Exposes `translationEnabled`, `translationLanguage`, `translationTimeout` with setters.

### TranslationSettingsScreen.kt

**Lines 91-97**: Single toggle for auto-translation:
```kotlin
SwitchSetting(
    title = stringResource(R.string.translation_enabled_title),
    checked = translationEnabled,
    onCheckedChange = { viewModel.setTranslationEnabled(it) },
    description = stringResource(R.string.translation_enabled_description),
)
```

### ArticleViewModel.kt — showTranslate computation

**Line 173**: Currently NOT gated by any master toggle:
```kotlin
val showTranslate = aiValid
```

### ArticleViewModel.kt — auto-translate combine (init block)

**Lines 262-285**: Auto-translate is gated by `translationEnabled` only (no master toggle):
```kotlin
combine(
    articleFlow,
    articleContentFlow,
    repository.translationEnabled,
) { article, articleContent, translationEnabled ->
    // ...
    if (translationEnabled && translationState.value is TranslationState.Empty && ...)
```

### ArticleScreen.kt — translate button visibility

**Line 365**: Gated by `viewState.showTranslate`:
```kotlin
if (viewState.showTranslate) {
```

### String Resources (strings.xml)

**Line 332**: `<string name="translation_enabled_title">Enable Auto Translation</string>`
**Line 333**: `<string name="translation_enabled_description">Automatically translate foreign language articles</string>`

---

## 3. Required Changes (file-by-file)

### 3.1 SettingsStore.kt

| Location | Change |
|:---------|:-------|
| After line 862 (after `setTranslationEnabled`) | Add `_enableTranslation` MutableStateFlow (default `true`), `enableTranslation` exposed StateFlow, `setEnableTranslation()` setter |
| After line 1049 (after `PREF_TRANSLATION_ENABLED`) | Add `const val PREF_ENABLE_TRANSLATION = "pref_enable_translation"` |
| After line 1123 (after `SETTING_ENABLE_SUMMARY`) in UserSettings enum | Add `SETTING_ENABLE_TRANSLATION(key = PREF_ENABLE_TRANSLATION)` |
| Also add existing translation settings to UserSettings | Add `SETTING_TRANSLATION_ENABLED(key = PREF_TRANSLATION_ENABLED)`, `SETTING_TRANSLATION_LANGUAGE(key = PREF_TRANSLATION_LANGUAGE)`, `SETTING_TRANSLATION_TIMEOUT(key = PREF_TRANSLATION_TIMEOUT_SECONDS)` for OPML support |

### 3.2 Repository.kt

| Location | Change |
|:---------|:-------|
| After line 406 (after `setTranslationEnabled`) | Add `enableTranslation` pass-through proxy + `setEnableTranslation()` setter, mirroring lines 389-391 |

### 3.3 TranslationSettingsViewModel.kt

| Location | Change |
|:---------|:-------|
| Line 24 (add new field) | Add `val enableTranslation: StateFlow<Boolean> = repository.enableTranslation` |
| After line 32 (add new setter) | Add `fun setEnableTranslation(enabled: Boolean)` with `viewModelScope.launch { repository.setEnableTranslation(enabled) }` |

### 3.4 TranslationSettingsScreen.kt

| Location | Change |
|:---------|:-------|
| Line 54 (add state collection) | Add `val enableTranslation by viewModel.enableTranslation.collectAsStateWithLifecycle()` |
| Lines 91-97 (current single toggle) | Insert "Enable Translation" master toggle ABOVE the existing toggle. Change existing toggle: rename string ref + add `enabled = enableTranslation` param |

New structure mirrors SummarySettingsScreen lines 92-109:
```kotlin
// Master toggle: Enable Translation
SwitchSetting(
    title = stringResource(R.string.enable_translation_title),
    checked = enableTranslation,
    onCheckedChange = { viewModel.setEnableTranslation(it) },
    description = stringResource(R.string.enable_translation_description),
)
Spacer(modifier = Modifier.height(8.dp))
// Sub-toggle: Auto Translation (dependent on master)
SwitchSetting(
    title = stringResource(R.string.translation_enabled_title),  // renamed to "Auto Translation"
    checked = translationEnabled,
    onCheckedChange = { viewModel.setTranslationEnabled(it) },
    description = stringResource(R.string.translation_enabled_description),
    enabled = enableTranslation,
)
```

### 3.5 ArticleViewModel.kt

| Location | Change |
|:---------|:-------|
| Line 157 (combine flows array) | Add `repository.enableTranslation` as param[12] (new 13th flow) |
| Line 173 (`showTranslate = aiValid`) | Change to `val enableTranslation = params[12] as Boolean` then `val showTranslate = enableTranslation && aiValid` |
| Lines 262-285 (auto-translate combine in init) | Add `repository.enableTranslation` to the combine, and prepend `enableTranslation &&` to the condition at line 275 |

**viewState combine changes detail:**

Current (12 flows, indices 0-11):
```kotlin
combine(
    articleFlow,                    // [0]
    textToDisplay,                  // [1]
    articleContentFlow,             // [2]
    toolbarVisible,                 // [3]
    repository.linkOpener,          // [4]
    repository.useDetectLanguage,   // [5]
    ttsStateHolder.ttsState,        // [6]
    ttsStateHolder.availableLanguages, // [7]
    repository.aiSettingsFlow,      // [8]
    aiSummary,                      // [9]
    translationState,               // [10]
    repository.enableSummary,       // [11]
) { params ->
```

After (13 flows, indices 0-12):
```kotlin
combine(
    articleFlow,                    // [0]
    textToDisplay,                  // [1]
    articleContentFlow,             // [2]
    toolbarVisible,                 // [3]
    repository.linkOpener,          // [4]
    repository.useDetectLanguage,   // [5]
    ttsStateHolder.ttsState,        // [6]
    ttsStateHolder.availableLanguages, // [7]
    repository.aiSettingsFlow,      // [8]
    aiSummary,                      // [9]
    translationState,               // [10]
    repository.enableSummary,       // [11]
    repository.enableTranslation,   // [12]  <-- NEW
) { params ->
```

And extracting:
```kotlin
val enableSummary = params[11] as Boolean
val enableTranslation = params[12] as Boolean  // NEW
val aiValid = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
val showSummarize = enableSummary && aiValid
val showTranslate = enableTranslation && aiValid  // CHANGED
```

**Auto-translate combine changes detail:**

Current:
```kotlin
combine(
    articleFlow,
    articleContentFlow,
    repository.translationEnabled,
) { article, articleContent, translationEnabled ->
    Triple(article, articleContent, translationEnabled)
}
```

After:
```kotlin
combine(
    articleFlow,
    articleContentFlow,
    repository.translationEnabled,
    repository.enableTranslation,
) { article, articleContent, translationEnabled, enableTranslation ->
    // need a 4-tuple or data structure
}
```

The condition changes from:
```kotlin
if (translationEnabled && ...)
```
To:
```kotlin
if (enableTranslation && translationEnabled && ...)
```

### 3.6 ArticleScreen.kt

**Line 365**: No change needed. `viewState.showTranslate` already controls visibility, and the value will now be computed as `enableTranslation && aiValid` in the ViewModel.

### 3.7 OPMLImporter.kt

| Location | Change |
|:---------|:-------|
| After line 165 (after `SETTING_ENABLE_SUMMARY`) | Add `UserSettings.SETTING_ENABLE_TRANSLATION -> settingsStore.setEnableTranslation(value.toBoolean())` |
| Same area | Add handlers for `SETTING_TRANSLATION_ENABLED`, `SETTING_TRANSLATION_LANGUAGE`, `SETTING_TRANSLATION_TIMEOUT` if those enum entries are added |

### 3.8 strings.xml

| Line | Change |
|:-----|:-------|
| Before line 332 | Add `<string name="enable_translation_title">Enable Translation</string>` |
| Before line 332 | Add `<string name="enable_translation_description">Enable AI translation functionality</string>` |
| Line 332 | Change `"Enable Auto Translation"` to `"Auto Translation"` |
| Line 333 | Change `"Automatically translate foreign language articles"` to `"Automatically translate articles when opened"` (to match summary pattern wording) |

---

## 4. Risks

### 4.1 viewState combine grows to 13 flows (INFO)

The `combine` in `ArticleViewModel` uses the vararg `Array<Any?>` overload, with index-based casting. Adding a 13th flow increases fragility. However, this is the established pattern and was already expanded to 12 in spec-038. No structural change needed — just careful index management.

### 4.2 Auto-translate combine needs restructuring (LOW)

The current auto-translate combine uses a 3-flow `combine` with destructured `Triple`. Adding a 4th flow requires either:
- Using the vararg overload with `Array<Any?>` casting (like the viewState combine)
- Using a local data class
- Nesting combines

The simplest approach: switch to the vararg overload and use `params` array like the viewState combine does, or use `combine(f1, f2, f3, f4) { a, b, c, d -> ... }` since Kotlin's `combine` supports up to 5 flows with explicit lambda params.

### 4.3 No per-feed translateOnOpen exists (INFO)

The spec mentions per-feed `translateOnOpen` (FR-005, AC-008, SCENARIO-013/014), but this field does NOT exist in the codebase. Only `summarizeOnOpen` exists per-feed. The auto-translate logic uses the global `translationEnabled` toggle only. FR-005 is a no-op since `translateOnOpen` doesn't exist — gating `enableTranslation` on the global auto-translate condition is sufficient. No per-feed changes needed.

### 4.4 Translation settings not currently in OPML (LOW)

The `UserSettings` enum does NOT have entries for `PREF_TRANSLATION_ENABLED`, `PREF_TRANSLATION_LANGUAGE`, or `PREF_TRANSLATION_TIMEOUT_SECONDS`. This means these are NOT currently exported/imported via OPML. We need to add `SETTING_ENABLE_TRANSLATION` for the new toggle, and ideally also add the missing translation settings entries for completeness. The OPML tests will need updating to include these new enum entries.

### 4.5 String rename collision (LOW)

`translation_enabled_title` currently exists as "Enable Auto Translation". We're renaming it to "Auto Translation" and adding a new `enable_translation_title` = "Enable Translation". This is safe because:
- The string key `translation_enabled_title` stays the same (only the value changes)
- The new key `enable_translation_title` is separate
- Same approach as spec-038's rename of `summary_enabled_title`

### 4.6 Pre-existing broken tests (INFO)

4 known test failures unrelated to this spec:
- `CustomFeederTextToolbarTest`
- `MenuConfigStoreTest` (2 tests)
- `CircleProgressIconButtonTest`

These should not block this implementation.

---

## 5. Test Files

### 5.1 Existing tests that need updating

| File | Changes needed |
|:-----|:---------------|
| `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` | Add 4 tests mirroring `enableSummary*` tests (lines 429-464): `enableTranslationDefaultsToTrue`, `enableTranslationSetToFalse`, `enableTranslationSetToTrue`, `enableTranslationIndependentOfTranslationEnabled` |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlParserTest.kt` | Add `SETTING_ENABLE_TRANSLATION` entry to expected settings map (around line 109) |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlWriterKtTest.kt` | Add `SETTING_ENABLE_TRANSLATION` entry to expected settings map (around line 217) |

### 5.2 Test file locations

- `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlParserTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlWriterKtTest.kt`

---

## 6. Summary of Changes

| # | File | Type | Lines affected |
|:--|:-----|:-----|:---------------|
| 1 | `SettingsStore.kt` | Add property + constant + enum entry | After L862, after L1049, after L1123 |
| 2 | `Repository.kt` | Add proxy | After L406 |
| 3 | `TranslationSettingsViewModel.kt` | Add StateFlow + setter | After L24, after L32 |
| 4 | `TranslationSettingsScreen.kt` | Add master toggle, modify auto toggle | L54, L91-97 |
| 5 | `ArticleViewModel.kt` | Add flow to combine, gate showTranslate, gate auto-translate | L157, L173, L262-275 |
| 6 | `OPMLImporter.kt` | Add when-case | After L165 |
| 7 | `strings.xml` | Add 2 strings, rename 2 | L332-333 |
| 8 | `SettingsStoreTest.kt` | Add 4 tests | After L464 |
| 9 | `OpmlParserTest.kt` | Add enum entry to map | ~L109 |
| 10 | `OpmlWriterKtTest.kt` | Add enum entry to map | ~L217 |

**Estimated production files**: 7
**Estimated test files**: 3
**Total estimated files**: 10
