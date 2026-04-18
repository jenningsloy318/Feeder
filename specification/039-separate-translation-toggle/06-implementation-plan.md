# 039 - Separate Translation Toggle: Implementation Plan

**Date**: 2026-04-18
**Estimated effort**: Small — mechanical pattern replication

---

## Phase 1: SettingsStore + Repository (Data Layer)

**Goal**: Add `enableTranslation` preference storage and repository proxy.

### SettingsStore.kt
1. Add `_enableTranslation` MutableStateFlow after `setTranslationEnabled()` (after line 862):
   - `private val _enableTranslation = MutableStateFlow(sp.getBoolean(PREF_ENABLE_TRANSLATION, true))`
   - `val enableTranslation = _enableTranslation.asStateFlow()`
   - `fun setEnableTranslation(value: Boolean)` with SharedPreferences write
2. Add `PREF_ENABLE_TRANSLATION` constant after `PREF_TRANSLATION_ENABLED` (after line 1049)
3. Add `SETTING_ENABLE_TRANSLATION(key = PREF_ENABLE_TRANSLATION)` to `UserSettings` enum (after line 1123, before the `;`)

### Repository.kt
4. Add `enableTranslation` pass-through proxy after `setTranslationEnabled()` (after line 406):
   - `val enableTranslation = settingsStore.enableTranslation`
   - `fun setEnableTranslation(value: Boolean) = settingsStore.setEnableTranslation(value)`

**Verification**: `./gradlew :app:compileFdroidDebugKotlin` passes.

---

## Phase 2: TranslationSettingsViewModel + TranslationSettingsScreen (Settings UI)

**Goal**: Add master toggle to settings UI with subordinate dependency.

### TranslationSettingsViewModel.kt
1. Add `val enableTranslation: StateFlow<Boolean> = repository.enableTranslation` at line 24
2. Add `fun setEnableTranslation(enabled: Boolean)` setter after `setTranslationEnabled()`

### TranslationSettingsScreen.kt
3. Add `val enableTranslation by viewModel.enableTranslation.collectAsStateWithLifecycle()` at line 54
4. Replace the existing single toggle (lines 91-97) with the two-toggle pattern:
   - Master "Enable Translation" toggle using `R.string.enable_translation_title`
   - Spacer(8.dp)
   - Subordinate "Auto Translation" toggle with `enabled = enableTranslation`

**Verification**: `./gradlew :app:compileFdroidDebugKotlin` passes.

---

## Phase 3: ArticleViewModel (Translation Gating)

**Goal**: Gate translate button visibility and auto-translate trigger by `enableTranslation`.

### ArticleViewModel.kt — viewState combine
1. Add `repository.enableTranslation` as 13th flow (index 12) in the combine at line 157
2. Extract `val enableTranslation = params[12] as Boolean` at line 170
3. Change `val showTranslate = aiValid` to `val showTranslate = enableTranslation && aiValid`

### ArticleViewModel.kt — auto-translate combine
4. Add `repository.enableTranslation` as 4th flow in the combine at line 263
5. Update destructuring to include `enableTranslation`
6. Prepend `enableTranslation &&` to the condition at line 275

**Verification**: `./gradlew :app:compileFdroidDebugKotlin` passes.

---

## Phase 4: OPML Import

**Goal**: Handle `SETTING_ENABLE_TRANSLATION` in OPML import.

### OPMLImporter.kt
1. Add `UserSettings.SETTING_ENABLE_TRANSLATION -> settingsStore.setEnableTranslation(value.toBoolean())` after line 165

**Verification**: `./gradlew :app:compileFdroidDebugKotlin` passes.

---

## Phase 5: String Resources

**Goal**: Add new strings and rename existing ones.

### strings.xml
1. Add `enable_translation_title` = "Enable Translation" (before line 332)
2. Add `enable_translation_description` = "Enable AI translation functionality" (before line 332)
3. Rename `translation_enabled_title` value from "Enable Auto Translation" to "Auto Translation" (line 332)
4. Update `translation_enabled_description` value from "Automatically translate foreign language articles" to "Automatically translate articles when opened" (line 333)

**Verification**: `./gradlew :app:compileFdroidDebugKotlin` passes.

---

## Phase 6: Unit Tests

**Goal**: Add tests for new setting and update OPML test maps.

### SettingsStoreTest.kt
1. Add `enableTranslationDefaultsToTrue` test
2. Add `enableTranslationSetToFalse` test
3. Add `enableTranslationSetToTrue` test
4. Add `enableTranslationIndependentOfTranslationEnabled` test

### OpmlParserTest.kt
5. Add `UserSettings.SETTING_ENABLE_TRANSLATION -> "true"` to `setAllSettings()` when-block (~line 109)

### OpmlWriterKtTest.kt
6. Add `UserSettings.SETTING_ENABLE_TRANSLATION -> "true"` to `ALL_SETTINGS_WITH_VALUES` map (~line 217)

**Verification**: `./gradlew :app:testFdroidDebugUnitTest` passes (excluding 4 pre-existing failures).

---

## Phase 7: Build Verification

**Goal**: Full build and test pass.

1. `./gradlew :app:compileFdroidDebugKotlin` — compilation
2. `./gradlew :app:testFdroidDebugUnitTest` — unit tests
3. Verify no regressions in existing tests (4 pre-existing failures remain unchanged)

---

## Commit Strategy

Each phase is an atomic commit that compiles successfully:

| Phase | Commit message pattern |
|:------|:----------------------|
| 1 | `feat spec-039-separate-translation-toggle: add enableTranslation to SettingsStore and Repository` |
| 2 | `feat spec-039-separate-translation-toggle: add master Enable Translation toggle to settings UI` |
| 3 | `feat spec-039-separate-translation-toggle: gate translation by enableTranslation in ArticleViewModel` |
| 4 | `feat spec-039-separate-translation-toggle: add OPML import for enableTranslation` |
| 5 | `feat spec-039-separate-translation-toggle: add and rename string resources` |
| 6 | `test spec-039-separate-translation-toggle: add SettingsStore and OPML tests` |
| 7 | (no commit — verification only) |

Phases may be combined into fewer commits at developer discretion, as long as each commit compiles.
