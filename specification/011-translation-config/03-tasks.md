# Task List: Translation Configuration

**Plan:** [./02-implementation-plan.md](./02-implementation-plan.md)
**Total Tasks:** 38

## Tasks

### Milestone 1: Data Model & Resources

- [ ] **T1.1** Create TranslationLanguage enum file
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/TranslationLanguage.kt`
  - **Details:** Create enum with DEVICE_DEFAULT + 12 languages (ENGLISH, CHINESE, SPANISH, FRENCH, GERMAN, JAPANESE, KOREAN, PORTUGUESE, RUSSIAN, ARABIC, HINDI). Each entry needs `code: String`, `displayName: StringRes`, `languageName: String`. Add companion object with `fromCode()` method.
  - **Acceptance:** Enum compiles, all entries have valid codes, fromCode() returns DEVICE_DEFAULT for invalid input

- [ ] **T1.2** Add English string resources for translation settings
  - **Files:** `app/src/main/res/values/strings.xml`
  - **Details:** Add strings for: translation_settings_title, translation_settings_subtitle, translation_enabled_title, translation_enabled_description, translation_target_language_title, translation_provider_title, and all language names (translation_language_device_default, translation_language_english, etc.)
  - **Acceptance:** All strings added, no duplicate resource IDs

- [ ] **T1.3** Add Chinese string resources for translation settings
  - **Files:** `app/src/main/res/values-zh/strings.xml`
  - **Details:** Translate all strings from T1.2 to Chinese. Use "设备默认" for DEVICE_DEFAULT, "英语" for English, "中文" for Chinese, etc.
  - **Acceptance:** All strings translated, matches English count

- [ ] **T1.4** Create unit tests for TranslationLanguage enum
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ai/model/TranslationLanguageTest.kt`
  - **Details:** Test fromCode() with valid codes (e.g., "en" returns ENGLISH), test fromCode() with invalid codes (returns DEVICE_DEFAULT), test fromCode() with null (returns DEVICE_DEFAULT), test all enum entries have non-empty displayName and languageName
  - **Acceptance:** Tests pass, >90% coverage of enum

### Milestone 2: Data Persistence Layer

- [ ] **T2.1** Add translationEnabled StateFlow to SettingsStore
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
  - **Details:** Add private `_translationEnabled` MutableStateFlow initialized from SharedPreferences (default false). Expose public `translationEnabled` StateFlow (asStateFlow). Add `PREF_TRANSLATION_ENABLED = "pref_translation_enabled"` constant.
  - **Acceptance:** Code compiles, StateFlow is publicly read-only, defaults to false

- [ ] **T2.2** Add setTranslationEnabled() method to SettingsStore
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
  - **Details:** Create method that updates `_translationEnabled.value` and persists to SharedPreferences using `sp.edit().putBoolean(PREF_TRANSLATION_ENABLED, value).apply()`
  - **Acceptance:** Method updates StateFlow and persists correctly

- [ ] **T2.3** Add translationLanguage StateFlow to SettingsStore
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
  - **Details:** Add private `_translationLanguage` MutableStateFlow initialized from SharedPreferences using `TranslationLanguage.fromCode(sp.getString(PREF_TRANSLATION_LANGUAGE, null))`. Expose public `translationLanguage` StateFlow. Add `PREF_TRANSLATION_LANGUAGE = "pref_translation_language"` constant.
  - **Acceptance:** Code compiles, StateFlow defaults to DEVICE_DEFAULT when pref is null

- [ ] **T2.4** Add setTranslationLanguage() method to SettingsStore
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
  - **Details:** Create method that updates `_translationLanguage.value` and persists code to SharedPreferences using `sp.edit().putString(PREF_TRANSLATION_LANGUAGE, value.code).apply()`
  - **Acceptance:** Method updates StateFlow and persists language code

- [ ] **T2.5** Add unit tests for SettingsStore translation settings
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
  - **Details:** Test translationEnabled defaults to false, test setTranslationEnabled() updates StateFlow and persists, test translationLanguage defaults to DEVICE_DEFAULT, test setTranslationLanguage() persists code and updates StateFlow, test StateFlow emits values
  - **Acceptance:** Tests pass, verify SharedPreferences interactions

### Milestone 3: Business Logic & ViewModel

- [ ] **T3.1** Add translationEnabled StateFlow to Repository
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
  - **Details:** Add `val translationEnabled: StateFlow<Boolean> = settingsStore.translationEnabled`. This is a thin facade.
  - **Acceptance:** Property exposes SettingsStore StateFlow correctly

- [ ] **T3.2** Add translationLanguage StateFlow to Repository
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
  - **Details:** Add `val translationLanguage: StateFlow<TranslationLanguage> = settingsStore.translationLanguage`
  - **Acceptance:** Property exposes SettingsStore StateFlow correctly

- [ ] **T3.3** Add setTranslationEnabled() to Repository
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
  - **Details:** Create method that calls `settingsStore.setTranslationEnabled(enabled)`
  - **Acceptance:** Method delegates correctly to SettingsStore

- [ ] **T3.4** Add setTranslationLanguage() to Repository
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
  - **Details:** Create method that calls `settingsStore.setTranslationLanguage(language)`
  - **Acceptance:** Method delegates correctly to SettingsStore

- [ ] **T3.5** Create TranslationSettingsViewModel class
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModel.kt`
  - **Details:** Create class extending DIAwareViewModel. Inject Repository via DI. Expose `translationEnabled: StateFlow<Boolean>` and `translationLanguage: StateFlow<TranslationLanguage>` from Repository. Create `setTranslationEnabled(enabled: Boolean)` and `setTranslationLanguage(language: TranslationLanguage)` methods that use viewModelScope.launch to call Repository methods.
  - **Acceptance:** ViewModel compiles, exposes StateFlows, uses coroutines

- [ ] **T3.6** Bind TranslationSettingsViewModel in DI module
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt`
  - **Details:** Add `bindWithComposableViewModelScope<TranslationSettingsViewModel>()` inside the DI.Module block
  - **Acceptance:** DI compiles, ViewModel can be injected

- [ ] **T3.7** Create unit tests for Repository translation methods
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/RepositoryTest.kt`
  - **Details:** Mock SettingsStore and verify translationEnabled/translationLanguage expose StateFlow, verify setTranslationEnabled/setTranslationLanguage delegate correctly
  - **Acceptance:** Tests pass, verify delegation

- [ ] **T3.8** Create unit tests for TranslationSettingsViewModel
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModelTest.kt`
  - **Details:** Mock Repository and verify StateFlows are exposed, verify setTranslationEnabled/setTranslationLanguage call Repository methods, verify viewModelScope is used
  - **Acceptance:** Tests pass, >90% coverage

### Milestone 4: UI Implementation

- [ ] **T4.1** Create TranslationSettingsScreen composable
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
  - **Details:** Create @Composable function with onNavigateUp callback and viewModel parameter. Collect translationEnabled and translationLanguage as State. Setup Scaffold with SensibleTopAppBar. Add SwitchSetting for "Enable Auto Translation". Add LanguageSelectorSetting (see T4.2). Add ExternalSetting for AI Provider link.
  - **Acceptance:** Screen compiles, renders without errors

- [ ] **T4.2** Create LanguageSelectorSetting composable
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
  - **Details:** Copy LanguageSelectorSetting pattern from SummarySettingsScreen. Accept title, currentLanguage, onLanguageSelected, enabled, menuExpanded, onMenuExpandedChange parameters. Render Row with click modifier. DropdownMenu shows all TranslationLanguage.entries. Selected item has checkmark. On selection, call onLanguageSelected and close menu.
  - **Acceptance:** Dropdown renders, selection works, disabled state works

- [ ] **T4.3** Add TranslationSettingsDestination to NavigationDestinations
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
  - **Details:** Create data object extending NavigationDestination. Set path to "settings/translation". Implement navigate() method. Implement RegisterScreen @Composable that gets ViewModel via diAwareViewModel() and calls TranslationSettingsScreen with onNavigateUp callback (popBackStack or navigate to Settings).
  - **Acceptance:** Destination compiles, follows existing pattern

- [ ] **T4.4** Register TranslationSettingsDestination in nav graph
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
  - **Details:** Find where other destinations are registered (e.g., SummarySettingsDestination). Add TranslationSettingsDestination to the registration map or list.
  - **Acceptance:** Destination is registered, no build errors

- [ ] **T4.5** Add "Translation Settings" link to Settings screen
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
  - **Details:** Find the AI Integration section (where Summary Settings link is). Add ExternalSetting with title from stringResource(R.string.translation_settings_title). Pass onNavigateToTranslationSettings callback to onClick.
  - **Acceptance:** Link appears in Settings, follows existing pattern

- [ ] **T4.6** Add onNavigateToTranslationSettings parameter to SettingsScreen
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
  - **Details:** Add `onNavigateToTranslationSettings: () -> Unit` parameter to SettingsScreen @Composable function. Pass it through to ExternalSetting in T4.5.
  - **Acceptance:** Parameter added, no compilation errors

- [ ] **T4.7** Wire navigation in calling screen
  - **Files:** Determine parent screen (likely SettingsScreen's parent)
  - **Details:** Add onNavigateToTranslationSettings = { TranslationSettingsDestination.navigate(navController) } when calling SettingsScreen.
  - **Acceptance:** Navigation works when link is tapped

- [ ] **T4.8** Create UI tests for TranslationSettingsScreen
  - **Files:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreenTest.kt`
  - **Details:** Test switch toggle updates ViewModel, test language dropdown expands, test language selection updates ViewModel, test language selector disabled when translation off, test back navigation
  - **Acceptance:** Tests pass, cover all user interactions

### Milestone 5: Internationalization & Finalization

- [ ] **T5.1** Translate strings to Spanish (es)
  - **Files:** `app/src/main/res/values-es/strings.xml`
  - **Details:** Translate all translation settings strings to Spanish. "Idioma del dispositivo" for DEVICE_DEFAULT, "Inglés" for English, "Español" for Spanish, etc.
  - **Acceptance:** All strings translated, matches English count

- [ ] **T5.2** Translate strings to French (fr)
  - **Files:** `app/src/main/res/values-fr/strings.xml`
  - **Details:** Translate all strings to French. "Langue de l'appareil" for DEVICE_DEFAULT, "Anglais" for English, "Français" for French, etc.
  - **Acceptance:** All strings translated

- [ ] **T5.3** Translate strings to German (de)
  - **Files:** `app/src/main/res/values-de/strings.xml`
  - **Details:** Translate all strings to German. "Gerätesprache" for DEVICE_DEFAULT, "Englisch" for English, "Deutsch" for German, etc.
  - **Acceptance:** All strings translated

- [ ] **T5.4** Translate strings to Japanese (ja)
  - **Files:** `app/src/main/res/values-ja/strings.xml`
  - **Details:** Translate all strings to Japanese. "デバイスのデフォルト" for DEVICE_DEFAULT, "英語" for English, "日本語" for Japanese, etc.
  - **Acceptance:** All strings translated

- [ ] **T5.5** Translate strings to other supported languages
  - **Files:** `app/src/main/res/values-{ko,pt,ru,ar,hi}/strings.xml`
  - **Details:** Translate to Korean, Portuguese, Russian, Arabic, Hindi. Follow same pattern.
  - **Acceptance:** All supported languages have translations

- [ ] **T5.6** Run all unit tests
  - **Command:** `./gradlew testDebugUnitTest`
  - **Details:** Run all unit tests and verify they pass. Check coverage report.
  - **Acceptance:** All tests pass, coverage >90%

- [ ] **T5.7** Run all UI tests
  - **Command:** `./gradlew connectedDebugAndroidTest`
  - **Details:** Run all instrumentation tests on emulator or device.
  - **Acceptance:** All UI tests pass

- [ ] **T5.8** Manual testing: Settings navigation flow
  - **Steps:** Open app → Tap Settings → Scroll to AI Integration → Tap Translation Settings → Verify screen opens → Tap back → Verify returns to Settings
  - **Acceptance:** Navigation works smoothly, no crashes

- [ ] **T5.9** Manual testing: Enable translation toggle
  - **Steps:** Open Translation Settings → Tap "Enable Auto Translation" switch → Verify it turns on → Tap it again → Verify it turns off → Restart app → Verify state persisted
  - **Acceptance:** Toggle works, state persists

- [ ] **T5.10** Manual testing: Language selection
  - **Steps:** Open Translation Settings → Enable translation → Tap "Target Language" → Verify dropdown opens → Select "Chinese" → Verify subtitle updates → Close dropdown → Reopen → Verify "Chinese" is selected → Restart app → Verify language persisted
  - **Acceptance:** Language selection works, state persists

- [ ] **T5.11** Manual testing: Disabled state
  - **Steps:** Open Translation Settings → Leave translation disabled → Verify "Target Language" is grayed out → Try to tap it → Verify dropdown doesn't open → Enable translation → Verify language selector is enabled
  - **Acceptance:** Disabled state works correctly

- [ ] **T5.12** Accessibility audit: TalkBack
  - **Steps:** Enable TalkBack → Open Translation Settings → Navigate through all elements → Verify each announces correctly (switch says "On"/"Off", language selector says "Target language, English", etc.)
  - **Acceptance:** All elements accessible, proper announcements

- [ ] **T5.13** Accessibility audit: Switch Access
  - **Steps:** Enable Switch Access → Navigate through settings → Verify all controls are accessible and operable
  - **Acceptance:** Works with Switch Access

- [ ] **T5.14** Performance verification: No memory leaks
  - **Steps:** Open Translation Settings → Navigate back → Open again → Repeat 10 times → Use memory profiler → Verify no increasing memory usage
  - **Acceptance:** No memory leaks, ViewModel cleared on back

- [ ] **T5.15** Performance verification: 60fps
  - **Steps:** Enable profiler → Open Translation Settings → Toggle switch rapidly → Open/close dropdown → Verify frame rate stays at 60fps
  - **Acceptance:** Smooth animations, no dropped frames

- [ ] **T5.16** Deep link testing
  - **Steps:** Use adb to open deep link: `adb shell am start -W -a android.intent.action.VIEW -d "feeder://settings/translation" com.nononsenseapps.feeder.debug` → Verify Translation Settings opens
  - **Acceptance:** Deep link works

### Final Tasks

- [ ] **TF.1** Code review
  - **Agent:** `super-dev:code-reviewer`
  - **Files:** All modified and new files
  - **Acceptance:** No blocking issues, all suggestions addressed

- [ ] **TF.2** Final verification against requirements
  - **Steps:** Review requirements document → Verify each requirement is implemented → Check acceptance criteria
  - **Acceptance:** All requirements met, documented

- [ ] **TF.3** Update documentation
  - **Files:** README (if feature is user-facing), CHANGELOG (if applicable)
  - **Acceptance:** Documentation reflects new feature

- [ ] **TF.4** Generate commit message
  - **Skill:** `generating-commit-messages`
  - **Steps:** Use skill to generate conventional commit message following project guidelines
  - **Acceptance:** Commit message generated, reviewed, and ready

- [ ] **TF.5** Commit and push changes
  - **Command:** `git add -A && git commit -m "<message from TF.4>" && git push`
  - **Acceptance:** Changes pushed to remote branch

## Task Dependencies

```
Milestone 1: Data Model & Resources
T1.1 ──┬──▶ T1.2 ──┬──▶ T1.3 (parallel translations)
       │          └──▶ T1.4 (needs T1.1)
       │
Milestone 2: Data Persistence Layer
       ▼
T2.1 ──┬──▶ T2.2
T2.3 ──┬──▶ T2.4
       │
       ├──▶ T2.5 (needs T2.1, T2.2, T2.3, T2.4)
       │
Milestone 3: Business Logic & ViewModel
       ▼
T3.1 ──┬──▶ T3.2 ──┬──▶ T3.3 ──┬──▶ T3.4
T3.5 ──┘          │
       │          ├──▶ T3.6 (DI binding, can parallel with T3.3, T3.4)
       │          │
       │          ├──▶ T3.7 (needs T3.1-T3.4)
       │          └──▶ T3.8 (needs T3.5)
       │
Milestone 4: UI Implementation
       ▼
T4.1 ──┬──▶ T4.2 (can parallel)
T4.3 ──┬──▶ T4.4 ──┬──▶ T4.5 ──┬──▶ T4.6 ──┬──▶ T4.7
       │                              │
       │                              └──▶ T4.8 (needs T4.1, T4.2)
       │
Milestone 5: Internationalization & Finalization
       ▼
T5.1 ──┬──▶ T5.2 ──┬──▶ T5.3 ──┬──▶ T5.4 ──┬──▶ T5.5 (parallel translations)
T5.6 ──┬──▶ T5.7 (parallel test runs)
       │
       ├──▶ T5.8 ──┬──▶ T5.9 ──┬──▶ T5.10 ──┬──▶ T5.11 (sequential manual tests)
       │
       ├──▶ T5.12 ──┬──▶ T5.13 (parallel accessibility tests)
       │
       ├──▶ T5.14 ──┬──▶ T5.15 (parallel performance tests)
       │
       └──▶ T5.16 (deep link test)
       │
Final Tasks
       ▼
TF.1 ──┬──▶ TF.2 ──┬──▶ TF.3 ──┬──▶ TF.4 ──┬──▶ TF.5
```

## Priority Order

**Critical Path (must complete in order):**
1. T1.1 - TranslationLanguage enum (blocks all other tasks)
2. T1.4 - Enum unit tests (verifies enum works)
3. T2.1-T2.4 - SettingsStore modifications (blocks Repository)
4. T2.5 - SettingsStore tests (verifies persistence)
5. T3.1-T3.5 - Repository + ViewModel (blocks UI)
6. T3.6 - DI binding (enables UI injection)
7. T3.7, T3.8 - Business logic tests (verifies layer)
8. T4.1, T4.2 - UI components (blocks navigation)
9. T4.3-T4.7 - Navigation integration (enables access)
10. T4.8 - UI tests (verifies interactions)
11. T5.6, T5.7 - Automated tests (regression check)
12. T5.8-T5.11 - Manual testing (end-to-end verification)
13. TF.1-TF.5 - Finalize and commit

**Can Parallelize (within milestones):**
- T1.2, T1.3, T5.1-T5.5 (translations)
- T3.7, T3.8 (business logic tests)
- T5.12-T5.16 (verification tests)

## Effort Estimates

| Milestone | Tasks | Estimated Time |
|-----------|-------|----------------|
| Milestone 1 | 4 tasks | 1-2 hours |
| Milestone 2 | 5 tasks | 1 hour |
| Milestone 3 | 8 tasks | 1-2 hours |
| Milestone 4 | 8 tasks | 2-3 hours |
| Milestone 5 | 13 tasks | 1-2 hours |
| **Total** | **38 tasks** | **6-10 hours** |

## Acceptance Checklist

Use this checklist before marking the feature complete:

### Functionality
- [ ] Can enable/disable translation via toggle
- [ ] Can select translation language from dropdown
- [ ] Toggle state persists across app restarts
- [ ] Language selection persists across app restarts
- [ ] Translation settings accessible from Settings → AI Integration
- [ ] Back navigation works correctly
- [ ] Provider link navigates to ProviderListScreen

### Quality
- [ ] All unit tests pass (>90% coverage)
- [ ] All UI tests pass
- [ ] No memory leaks detected
- [ ] 60fps maintained during interactions
- [ ] No crashes or exceptions in logcat

### Accessibility
- [ ] TalkBack announces all elements correctly
- [ ] Switch Access works for all controls
- [ ] Touch targets meet 48dp minimum
- [ ] Color contrast meets WCAG AA (4.5:1)

### Internationalization
- [ ] All strings translated to supported languages
- [ ] Language names display correctly in each locale
- [ ] Device language changes picked up by DEVICE_DEFAULT

### Documentation
- [ ] Code is well-commented
- [ ] String resources documented (if needed)
- [ ] Commit message follows conventions
- [ ] Feature documented (if applicable)
