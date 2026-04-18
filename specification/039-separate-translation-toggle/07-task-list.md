# 039 - Separate Translation Toggle: Task List

**Date**: 2026-04-18

---

## TASK-001: Add enableTranslation to SettingsStore

- **Description**: Add `_enableTranslation` MutableStateFlow (default `true`), exposed StateFlow, setter, `PREF_ENABLE_TRANSLATION` constant, and `SETTING_ENABLE_TRANSLATION` UserSettings enum entry.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- **Dependencies**: None
- **Acceptance criteria**: `enableTranslation` defaults to `true`, `setEnableTranslation(false)` persists to SharedPreferences, `SETTING_ENABLE_TRANSLATION` exists in UserSettings enum. (SCENARIO-022, SCENARIO-023)
- **Estimated complexity**: S

---

## TASK-002: Add enableTranslation proxy to Repository

- **Description**: Add `enableTranslation` pass-through StateFlow proxy and `setEnableTranslation()` setter to Repository, mirroring the `enableSummary` pattern.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
- **Dependencies**: TASK-001
- **Acceptance criteria**: `repository.enableTranslation` returns the SettingsStore's `enableTranslation` StateFlow. `repository.setEnableTranslation()` delegates to SettingsStore.
- **Estimated complexity**: S

---

## TASK-003: Expose enableTranslation in TranslationSettingsViewModel

- **Description**: Add `enableTranslation` StateFlow and `setEnableTranslation()` setter to TranslationSettingsViewModel.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModel.kt`
- **Dependencies**: TASK-002
- **Acceptance criteria**: `viewModel.enableTranslation` exposes a StateFlow<Boolean>. `viewModel.setEnableTranslation()` delegates to Repository via viewModelScope.
- **Estimated complexity**: S

---

## TASK-004: Add master toggle to TranslationSettingsScreen

- **Description**: Collect `enableTranslation` state. Insert "Enable Translation" master SwitchSetting above the existing toggle. Add `enabled = enableTranslation` to the existing auto-translation toggle so it greys out when master is OFF.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
- **Dependencies**: TASK-003
- **Acceptance criteria**: "Enable Translation" toggle appears above "Auto Translation". When Enable Translation is OFF, Auto Translation toggle is greyed out (0.38f alpha) and non-interactive. When Enable Translation is ON, Auto Translation toggle is fully interactive. (SCENARIO-006, SCENARIO-007, SCENARIO-008, SCENARIO-011, SCENARIO-012)
- **Estimated complexity**: S

---

## TASK-005: Gate showTranslate in ArticleViewModel viewState combine

- **Description**: Add `repository.enableTranslation` as 13th flow (index 12) in the viewState combine. Extract `enableTranslation` from params and change `showTranslate = aiValid` to `showTranslate = enableTranslation && aiValid`.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Dependencies**: TASK-002
- **Acceptance criteria**: When `enableTranslation` is false, `showTranslate` is false regardless of AI configuration. When `enableTranslation` is true, `showTranslate` equals `aiValid` (existing behavior). (SCENARIO-001, SCENARIO-002, SCENARIO-004, SCENARIO-005)
- **Estimated complexity**: S

---

## TASK-006: Gate auto-translate by enableTranslation in ArticleViewModel init

- **Description**: Add `repository.enableTranslation` as 4th flow in the auto-translate combine. Prepend `enableTranslation &&` to the auto-translate condition.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Dependencies**: TASK-002
- **Acceptance criteria**: When `enableTranslation` is false, auto-translation does not trigger regardless of `translationEnabled` value. When `enableTranslation` is true, auto-translation behavior is unchanged. (SCENARIO-001, SCENARIO-004, SCENARIO-013, SCENARIO-014)
- **Estimated complexity**: S

---

## TASK-007: Add OPML import handler for enableTranslation

- **Description**: Add `UserSettings.SETTING_ENABLE_TRANSLATION -> settingsStore.setEnableTranslation(value.toBoolean())` when-case to OPMLImporter.
- **File(s)**: `app/src/main/java/com/nononsenseapps/feeder/model/opml/OPMLImporter.kt`
- **Dependencies**: TASK-001
- **Acceptance criteria**: OPML import correctly restores enableTranslation setting. (SCENARIO-015, SCENARIO-016)
- **Estimated complexity**: S

---

## TASK-008: Add and rename string resources

- **Description**: Add `enable_translation_title` and `enable_translation_description` strings. Rename `translation_enabled_title` value from "Enable Auto Translation" to "Auto Translation". Update `translation_enabled_description` value to "Automatically translate articles when opened".
- **File(s)**: `app/src/main/res/values/strings.xml`
- **Dependencies**: None
- **Acceptance criteria**: New strings exist. Existing strings are renamed. No untranslated string warnings for English. (FR-008)
- **Estimated complexity**: S

---

## TASK-009: Add SettingsStore unit tests for enableTranslation

- **Description**: Add 4 tests mirroring the `enableSummary*` tests: `enableTranslationDefaultsToTrue`, `enableTranslationSetToFalse`, `enableTranslationSetToTrue`, `enableTranslationIndependentOfTranslationEnabled`.
- **File(s)**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
- **Dependencies**: TASK-001
- **Acceptance criteria**: All 4 new tests pass. (SCENARIO-019, SCENARIO-020, SCENARIO-021, SCENARIO-022)
- **Estimated complexity**: S

---

## TASK-010: Update OPML test maps for enableTranslation

- **Description**: Add `UserSettings.SETTING_ENABLE_TRANSLATION -> "true"` to both `OpmlParserTest.setAllSettings()` and `OpmlWriterKtTest.ALL_SETTINGS_WITH_VALUES` so the exhaustive when-block compiles and the OPML round-trip tests pass.
- **File(s)**: `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlParserTest.kt`, `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlWriterKtTest.kt`
- **Dependencies**: TASK-001
- **Acceptance criteria**: OPML parser and writer tests compile and pass with the new enum entry. (SCENARIO-015, SCENARIO-016)
- **Estimated complexity**: S

---

## TASK-011: Build verification

- **Description**: Run full compilation and unit test suite. Verify no regressions beyond the 4 known pre-existing failures.
- **File(s)**: None (verification only)
- **Dependencies**: TASK-001 through TASK-010
- **Acceptance criteria**: `./gradlew :app:compileFdroidDebugKotlin` succeeds. `./gradlew :app:testFdroidDebugUnitTest` passes with only the 4 known pre-existing failures.
- **Estimated complexity**: S

---

## Dependency Graph

```
TASK-001 (SettingsStore)
  ├── TASK-002 (Repository)
  │     ├── TASK-003 (ViewModel)
  │     │     └── TASK-004 (Settings UI)
  │     ├── TASK-005 (viewState combine)
  │     └── TASK-006 (auto-translate combine)
  ├── TASK-007 (OPML import)
  ├── TASK-009 (SettingsStore tests)
  └── TASK-010 (OPML tests)

TASK-008 (Strings) — independent, no dependencies

TASK-011 (Verification) — depends on all above
```

---

## Summary

| Task | Description | Complexity | Dependencies |
|:-----|:-----------|:-----------|:-------------|
| TASK-001 | SettingsStore: enableTranslation property + constant + enum | S | — |
| TASK-002 | Repository: enableTranslation proxy | S | TASK-001 |
| TASK-003 | TranslationSettingsViewModel: enableTranslation exposure | S | TASK-002 |
| TASK-004 | TranslationSettingsScreen: master toggle UI | S | TASK-003 |
| TASK-005 | ArticleViewModel: gate showTranslate in viewState | S | TASK-002 |
| TASK-006 | ArticleViewModel: gate auto-translate in init | S | TASK-002 |
| TASK-007 | OPMLImporter: enableTranslation when-case | S | TASK-001 |
| TASK-008 | strings.xml: add and rename strings | S | — |
| TASK-009 | SettingsStoreTest: 4 new tests | S | TASK-001 |
| TASK-010 | OPML tests: update settings maps | S | TASK-001 |
| TASK-011 | Build verification | S | All |

**Total tasks**: 11
**All tasks**: Small complexity
**Total files modified**: 10 (7 production + 3 test)
