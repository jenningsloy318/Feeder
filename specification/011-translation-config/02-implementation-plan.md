# Implementation Plan: Translation Configuration

**Specification:** [./01-tech-spec.md](./01-tech-spec.md)
**Estimated Phases:** 5

**CRITICAL:** All phases/milestones defined in this plan MUST be implemented in a single continuous execution. The execution-coordinator will NOT pause between phases or ask for permission to continue. Every phase from Phase 1 to Phase 5 will be completed automatically.

## Milestones

### Milestone 1 (Phase 1): Data Model & Resources
**Goal:** Create TranslationLanguage enum and string resources
**Dependencies:** None

#### Deliverables
- [ ] TranslationLanguage enum with all supported languages
- [ ] String resources for all languages in English
- [ ] String resources for all languages in Chinese (zh)
- [ ] Unit tests for TranslationLanguage enum

#### Acceptance Criteria
- [ ] TranslationLanguage enum has DEVICE_DEFAULT + 12 language entries
- [ ] All entries have valid ISO 639-1 codes
- [ ] fromCode() method handles valid and invalid codes
- [ ] String resources exist for all display names
- [ ] Unit tests pass with >90% coverage

#### Files Affected
- `app/src/main/java/com/nononsenseapps/feeder/ai/model/TranslationLanguage.kt` (NEW)
- `app/src/main/res/values/strings.xml` (MODIFY)
- `app/src/main/res/values-zh/strings.xml` (MODIFY)
- `app/src/test/java/com/nononsenseapps/feeder/ai/model/TranslationLanguageTest.kt` (NEW)

### Milestone 2 (Phase 2): Data Persistence Layer
**Goal:** Add translation settings to SettingsStore
**Dependencies:** Milestone 1 (TranslationLanguage enum)

#### Deliverables
- [ ] Add _translationEnabled MutableStateFlow to SettingsStore
- [ ] Add _translationLanguage MutableStateFlow to SettingsStore
- [ ] Add setTranslationEnabled() method
- [ ] Add setTranslationLanguage() method
- [ ] Add preference constants
- [ ] Unit tests for SettingsStore modifications

#### Acceptance Criteria
- [ ] translationEnabled StateFlow defaults to false
- [ ] translationLanguage StateFlow defaults to DEVICE_DEFAULT
- [ ] Settings persist to SharedPreferences correctly
- [ ] StateFlow emissions are observable
- [ ] Unit tests verify persistence and defaults

#### Files Affected
- `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` (MODIFY)
- `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` (MODIFY)

### Milestone 3 (Phase 3): Business Logic & ViewModel
**Goal:** Add Repository facade and create ViewModel
**Dependencies:** Milestone 2 (SettingsStore modifications)

#### Deliverables
- [ ] Add translationEnabled StateFlow to Repository
- [ ] Add translationLanguage StateFlow to Repository
- [ ] Add setTranslationEnabled() to Repository
- [ ] Add setTranslationLanguage() to Repository
- [ ] Create TranslationSettingsViewModel class
- [ ] Bind ViewModel in ArchModelModule
- [ ] Unit tests for Repository and ViewModel

#### Acceptance Criteria
- [ ] Repository correctly delegates to SettingsStore
- [ ] ViewModel exposes StateFlows from Repository
- [ ] ViewModel methods use viewModelScope
- [ ] DI binding creates ViewModel correctly
- [ ] Unit tests verify delegation and coroutines

#### Files Affected
- `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` (MODIFY)
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModel.kt` (NEW)
- `app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt` (MODIFY)
- `app/src/test/java/com/nononsenseapps/feeder/archmodel/RepositoryTest.kt` (MODIFY)
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModelTest.kt` (NEW)

### Milestone 4 (Phase 4): UI Implementation
**Goal:** Create TranslationSettingsScreen
**Dependencies:** Milestone 3 (ViewModel)

#### Deliverables
- [ ] Create TranslationSettingsScreen composable
- [ ] Create LanguageSelectorSetting composable (mirror SummarySettingsScreen)
- [ ] Add TranslationSettingsDestination to NavigationDestinations
- [ ] Add "Translation Settings" link to Settings screen
- [ ] UI tests for screen interactions

#### Acceptance Criteria
- [ ] Screen renders with toggle and language selector
- [ ] Toggle switch enables/disables translation
- [ ] Language dropdown shows all languages
- [ ] Language selection updates subtitle
- [ ] Language selector is disabled when translation is off
- [ ] Back navigation works correctly
- [ ] UI tests verify all interactions

#### Files Affected
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt` (NEW)
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt` (MODIFY)
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt` (MODIFY)
- `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreenTest.kt` (NEW)

### Milestone 5 (Phase 5): Internationalization & Finalization
**Goal:** Add translations for all supported languages and verify complete feature
**Dependencies:** Milestone 4 (UI implementation)

#### Deliverables
- [ ] Translate strings.xml for all supported languages
- [ ] Run all tests and verify passing
- [ ] Manual testing on device/emulator
- [ ] Accessibility audit (TalkBack, Switch Access)
- [ ] Performance verification (60fps, no leaks)

#### Acceptance Criteria
- [ ] All string resources have translations
- [ ] Unit tests pass (>90% coverage)
- [ ] UI tests pass (all interactions)
- [ ] Manual testing confirms feature works end-to-end
- [ ] Accessibility scanner passes with no errors
- [ ] No memory leaks or performance issues

#### Files Affected
- `app/src/main/res/values-*/strings.xml` (MODIFY for all languages)
- All test files (run and verify)

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| TranslationLanguage enum diverges from SummaryLanguage | Medium | Low | Keep enum entries in sync via code review; document ADR decision |
| SharedPreferences migration issues | Low | Low | New settings, no migration needed |
| UI component reuse issues | Low | Medium | Copy LanguageSelectorSetting from SummarySettingsScreen exactly |
| Navigation integration bugs | Low | Medium | Follow existing NavigationDestinations pattern closely |
| String resource overflow (missing translations) | Medium | Low | Use same languages as SummaryLanguage; leverage existing translations |
| StateFlow recomposition issues | Low | Medium | Use collectAsStateWithLifecycle as in existing screens |

## Dependencies

### External Dependencies
- **None** - all dependencies are existing project libraries

### Internal Dependencies
- **TranslationLanguage enum** - Required by SettingsStore, Repository, ViewModel, UI
- **SettingsStore modifications** - Required by Repository
- **Repository modifications** - Required by ViewModel
- **ViewModel** - Required by UI screen
- **NavigationDestinations** - Required for screen access
- **String resources** - Required by UI and enum

## Success Metrics
- [ ] All unit tests pass with >90% code coverage
- [ ] All UI tests pass covering all user interactions
- [ ] Manual testing confirms Settings → Translation Settings flow works
- [ ] Toggle switch persists across app restarts
- [ ] Language selection persists across app restarts
- [ ] Accessibility scanner passes with no errors
- [ ] No memory leaks (verified with LeakCanary or profiler)
- [ ] 60fps maintained during interactions (verified with profiler)
- [ ] Translation settings accessible via deep link (settings/translation)

## Implementation Order Summary

```
Phase 1: Data Model & Resources (1-2 hours)
  └─> TranslationLanguage enum + strings

Phase 2: Data Persistence Layer (1 hour)
  └─> SettingsStore modifications

Phase 3: Business Logic & ViewModel (1-2 hours)
  └─> Repository + ViewModel + DI

Phase 4: UI Implementation (2-3 hours)
  └─> Screen + Navigation + Settings link

Phase 5: Internationalization & Finalization (1-2 hours)
  └─> Translations + Testing + Verification

Total Estimated Effort: 6-10 hours
```

## Parallelization Opportunities

**Sequential Only:** Each phase depends on the previous phase. However, within each phase:
- String resources can be translated in parallel (different developers working on different languages)
- Unit tests can be written alongside implementation (not after)

## Continuous Integration

Each phase should:
1. Pass all existing tests
2. Add new tests for new code
3. Maintain >90% code coverage
4. Pass code quality checks (if enabled)
5. Build successfully on all variants

## Rollback Strategy

If any phase fails:
1. Revert changes for that phase
2. Fix the issue
3. Re-apply the changes
4. Re-run tests

Because this is a new feature (not modifying existing behavior), rollback is safe and won't affect users.
