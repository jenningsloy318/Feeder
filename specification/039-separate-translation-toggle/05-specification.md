# 039 - Separate Translation Toggle: Implementation Specification

**Date**: 2026-04-18
**Status**: Specification complete
**Pattern**: Mirror of spec-038 (Separate Summary Toggle)

---

## 1. Overview

Add a master "Enable Translation" toggle that controls whether any translation functionality is available. The existing "Enable Auto Translation" toggle is renamed to "Auto Translation" and becomes a subordinate control that is only interactive when the master toggle is ON.

This mirrors the spec-038 two-toggle pattern for summary:
- **Enable Translation** (master): gates all translation features — button visibility, auto-translate, per-feed overrides
- **Auto Translation** (subordinate): controls only automatic translation on article open

---

## 2. Data Flow

```
SettingsStore._enableTranslation (MutableStateFlow<Boolean>, default true)
  → Repository.enableTranslation (pass-through StateFlow)
    → TranslationSettingsViewModel.enableTranslation (exposed StateFlow)
      → TranslationSettingsScreen (new master toggle + enabled= on Auto Translation)
    → ArticleViewModel.viewState combine (param[12], gates showTranslate)
    → ArticleViewModel.init auto-translate combine (gates condition)
      → ArticleScreen (showTranslate controls button visibility — no change needed)
  → OPMLImporter (SETTING_ENABLE_TRANSLATION → setEnableTranslation)
```

---

## 3. Settings Behavior Matrix

| Enable Translation | Auto Translation | Translate Button | Auto-Translate on Open | Auto Translation Toggle (Settings) |
|:------------------:|:----------------:|:----------------:|:----------------------:|:-----------------------------------:|
| ON                 | ON               | Visible          | Yes                    | Enabled, interactive                |
| ON                 | OFF              | Visible          | No                     | Enabled, interactive                |
| OFF                | ON (stored)      | Hidden           | No                     | Greyed out (0.38f alpha)            |
| OFF                | OFF (stored)     | Hidden           | No                     | Greyed out (0.38f alpha)            |

---

## 4. Detailed Changes per File

### 4.1 SettingsStore.kt

**Add after line 862** (after `setTranslationEnabled`): New `_enableTranslation` MutableStateFlow, exposed StateFlow, and setter.

```kotlin
// Enable Translation master toggle (gates all translation features)
private val _enableTranslation = MutableStateFlow(sp.getBoolean(PREF_ENABLE_TRANSLATION, true))
val enableTranslation = _enableTranslation.asStateFlow()

fun setEnableTranslation(value: Boolean) {
    _enableTranslation.value = value
    sp.edit().putBoolean(PREF_ENABLE_TRANSLATION, value).apply()
}
```

**Add after line 1049** (after `PREF_TRANSLATION_ENABLED`): New pref constant.

```kotlin
const val PREF_ENABLE_TRANSLATION = "pref_enable_translation"
```

**Add after line 1123** (after `SETTING_BLOCKLIST_APPLY_TO_SUMMARIES`): New UserSettings enum entry.

```kotlin
// Translation settings
SETTING_ENABLE_TRANSLATION(key = PREF_ENABLE_TRANSLATION),
```

### 4.2 Repository.kt

**Add after line 406** (after `setTranslationEnabled`): New pass-through proxy and setter.

```kotlin
// Enable Translation Setting (master toggle)
val enableTranslation = settingsStore.enableTranslation

fun setEnableTranslation(value: Boolean) = settingsStore.setEnableTranslation(value)
```

### 4.3 TranslationSettingsViewModel.kt

**Add at line 24** (after repository declaration): New StateFlow exposure.

```kotlin
val enableTranslation: StateFlow<Boolean> = repository.enableTranslation
```

**Add after line 32** (after `setTranslationEnabled`): New setter method.

```kotlin
fun setEnableTranslation(enabled: Boolean) {
    viewModelScope.launch {
        repository.setEnableTranslation(enabled)
    }
}
```

### 4.4 TranslationSettingsScreen.kt

**Add at line 54** (after `translationEnabled` collection): Collect new state.

```kotlin
val enableTranslation by viewModel.enableTranslation.collectAsStateWithLifecycle()
```

**Replace lines 91-97** (current single toggle): Insert master toggle above, rename existing to Auto Translation with `enabled` param.

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
    title = stringResource(R.string.translation_enabled_title),
    checked = translationEnabled,
    onCheckedChange = { viewModel.setTranslationEnabled(it) },
    description = stringResource(R.string.translation_enabled_description),
    enabled = enableTranslation,
)
```

### 4.5 ArticleViewModel.kt — viewState combine

**Line 157**: Add `repository.enableTranslation` as 13th flow (index 12).

```kotlin
combine(
    articleFlow,                       // [0]
    textToDisplay,                     // [1]
    articleContentFlow,                // [2]
    toolbarVisible,                    // [3]
    repository.linkOpener,             // [4]
    repository.useDetectLanguage,      // [5]
    ttsStateHolder.ttsState,           // [6]
    ttsStateHolder.availableLanguages, // [7]
    repository.aiSettingsFlow,         // [8]
    aiSummary,                         // [9]
    translationState,                  // [10]
    repository.enableSummary,          // [11]
    repository.enableTranslation,      // [12]  ← NEW
) { params ->
```

**Lines 170-173**: Extract `enableTranslation` and gate `showTranslate`.

```kotlin
val enableSummary = params[11] as Boolean
val enableTranslation = params[12] as Boolean
val aiValid = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
val showSummarize = enableSummary && aiValid
val showTranslate = enableTranslation && aiValid  // CHANGED from: aiValid
```

### 4.6 ArticleViewModel.kt — auto-translate combine

**Lines 262-285**: Add `repository.enableTranslation` as 4th flow and prepend `enableTranslation &&` to condition.

```kotlin
combine(
    articleFlow,
    articleContentFlow,
    repository.translationEnabled,
    repository.enableTranslation,
) { article, articleContent, translationEnabled, enableTranslation ->
    data class AutoTranslateData(
        val article: Article?,
        val articleContent: LinearArticle,
        val translationEnabled: Boolean,
        val enableTranslation: Boolean,
    )
    AutoTranslateData(article, articleContent, translationEnabled, enableTranslation)
}.filterNotNull()
    .collect { (article, articleContent, translationEnabled, enableTranslation) ->
        if (enableTranslation && translationEnabled &&
            translationState.value is TranslationState.Empty &&
            article?.link != null &&
            articleContent.elements.isNotEmpty()
        ) {
            Log.d(LOG_TAG, "Auto-translate triggered for article ${article.id} with ${articleContent.elements.size} elements")
            translate()
            return@collect
        }
    }
```

Note: Kotlin `combine` supports up to 5 flows with explicit lambda parameters (no vararg needed). The 4-flow version uses a local data class for destructuring since there's no `Quadruple` in stdlib.

### 4.7 ArticleScreen.kt

**No changes needed.** `viewState.showTranslate` already controls visibility at line 365. The gating logic in `ArticleViewModel` handles everything.

### 4.8 OPMLImporter.kt

**Add after line 165** (after `SETTING_BLOCKLIST_APPLY_TO_SUMMARIES`): New when-case.

```kotlin
// Translation settings
UserSettings.SETTING_ENABLE_TRANSLATION -> settingsStore.setEnableTranslation(value.toBoolean())
```

### 4.9 strings.xml

**Add before line 332** (before `translation_enabled_title`): New strings for master toggle.

```xml
<string name="enable_translation_title">Enable Translation</string>
<string name="enable_translation_description">Enable AI translation functionality</string>
```

**Change line 332**: Rename "Enable Auto Translation" → "Auto Translation".

```xml
<string name="translation_enabled_title">Auto Translation</string>
```

**Change line 333**: Update description to match summary pattern.

```xml
<string name="translation_enabled_description">Automatically translate articles when opened</string>
```

---

## 5. OPML Support

### Export
Automatic — `UserSettings.entries` enumeration includes `SETTING_ENABLE_TRANSLATION`. The OPML writer iterates all `UserSettings` entries and writes their current SharedPreferences values.

### Import
Handled by the new `when`-case in `OPMLImporter.kt` (section 4.8).

### Scope note
Only `SETTING_ENABLE_TRANSLATION` is added. The existing translation settings (`translationEnabled`, `translationLanguage`, `translationTimeout`) are NOT in the `UserSettings` enum and thus NOT exported/imported via OPML. Adding them is out of scope for this spec to keep the change minimal.

---

## 6. Testing Strategy

### 6.1 SettingsStoreTest.kt — 4 new tests

Mirror the `enableSummary*` tests at lines 428-464:

| Test | Description | BDD Scenario |
|:-----|:-----------|:-------------|
| `enableTranslationDefaultsToTrue` | Verify default value is `true` | SCENARIO-022 |
| `enableTranslationSetToFalse` | Verify setting to `false` persists | SCENARIO-004, SCENARIO-005 |
| `enableTranslationSetToTrue` | Verify setting to `true` persists | SCENARIO-019 |
| `enableTranslationIndependentOfTranslationEnabled` | Verify independence from auto-translate toggle | SCENARIO-011, SCENARIO-012 |

### 6.2 OpmlParserTest.kt — update settings map

Add `SETTING_ENABLE_TRANSLATION -> "true"` to the `when` block in `setAllSettings()` (around line 109).

### 6.3 OpmlWriterKtTest.kt — update settings map

Add `SETTING_ENABLE_TRANSLATION -> "true"` to the `ALL_SETTINGS_WITH_VALUES` map (around line 217).

### 6.4 Pre-existing broken tests (INFO)

4 known test failures unrelated to this spec — should not block:
- `CustomFeederTextToolbarTest`
- `MenuConfigStoreTest` (2 tests)
- `CircleProgressIconButtonTest`

---

## 7. Edge Cases

### 7.1 In-progress translation when toggling OFF (SCENARIO-017)
No cancellation logic. The in-progress translation completes silently. The button disappears from the UI because `showTranslate` becomes `false`. Same behavior as spec-038's summary equivalent.

### 7.2 Rapid toggling (SCENARIO-026)
Flow-based reactivity handles this naturally. The final state of `enableTranslation` propagates through the combine, and `showTranslate` reflects the latest value. No debouncing needed.

### 7.3 Backward compatibility (SCENARIO-022, SCENARIO-023)
Default value is `true`. Existing users see no behavior change — `enableTranslation` defaults ON, preserving the current state where the translate button is visible whenever AI is configured.

### 7.4 Cached translations (SCENARIO-025)
Translation cache (`TranslationBlob`) is not affected. Cached translations remain accessible regardless of toggle state. The toggle only gates new translation requests.

### 7.5 Per-feed translateOnOpen (SCENARIO-013, SCENARIO-014)
The field `translateOnOpen` does NOT exist per-feed in the codebase. Only `summarizeOnOpen` exists. The auto-translate combine uses the global `translationEnabled` toggle. Gating with `enableTranslation &&` in the auto-translate combine is sufficient. No per-feed changes needed. FR-005/AC-008 are effectively no-ops.

### 7.6 Summary independence (SCENARIO-024)
`enableTranslation` and `enableSummary` are fully independent StateFlows. Toggling one has no effect on the other.

---

## 8. Files Summary

| # | File | Change Type | Lines Affected |
|:--|:-----|:------------|:---------------|
| 1 | `SettingsStore.kt` | Add property + constant + enum | After L862, after L1049, after L1123 |
| 2 | `Repository.kt` | Add proxy + setter | After L406 |
| 3 | `TranslationSettingsViewModel.kt` | Add StateFlow + setter | L24, after L32 |
| 4 | `TranslationSettingsScreen.kt` | Add master toggle, modify auto toggle | L54, L91-97 |
| 5 | `ArticleViewModel.kt` | Add flow to combine, gate showTranslate, gate auto-translate | L157, L170-173, L262-285 |
| 6 | `OPMLImporter.kt` | Add when-case | After L165 |
| 7 | `strings.xml` | Add 2 strings, rename 2 | L332-333 |
| 8 | `SettingsStoreTest.kt` | Add 4 tests | After L464 |
| 9 | `OpmlParserTest.kt` | Add enum entry to map | ~L109 |
| 10 | `OpmlWriterKtTest.kt` | Add enum entry to map | ~L217 |

**Production files**: 7
**Test files**: 3
**Total files**: 10
