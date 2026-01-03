# QA Test Plan: Translation Configuration Feature

**Specification:** 011-translation-config
**QA Agent:** Claude Code (QA Specialist)
**Date:** 2026-01-03
**Status:** Ready for Execution

## Test Summary

- **Application:** Feeder RSS Reader
- **Modality:** Android App (Compose UI)
- **Feature:** Translation Configuration Settings
- **Test Approach:** Automated (Unit + UI) + Manual Verification
- **Coverage Target:** >90% unit, >80% UI

## Test Strategy

### Risk Assessment

| Risk Area | Probability | Impact | Mitigation |
|-----------|-------------|--------|------------|
| Settings not persisting | Medium | High | Unit tests verify SharedPreferences interactions |
| Navigation issues | Low | Medium | UI tests verify navigation flows |
| Translation strings missing | Medium | Low | Verify all language files updated |
| Memory leaks | Low | Medium | Profile during testing |
| Accessibility issues | Medium | High | Manual TalkBack testing |
| StateFlow not reactive | Low | High | Unit tests verify emissions |

### Coverage Targets

- [x] Happy path scenarios
- [x] Boundary conditions (invalid language codes)
- [x] Error handling (corrupted preferences)
- [x] Edge cases (rapid toggling, disabled state)
- [x] State persistence across restarts
- [x] Accessibility compliance (WCAG 2.1 AA)

## Test Cases

### Unit Tests

#### UTC-001: TranslationLanguage Enum Tests
**Priority:** P0
**File:** `app/src/test/java/com/nononsenseapps/feeder/ai/model/TranslationLanguageTest.kt`

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UTC-001-01 | fromCode() with valid code "en" | Returns ENGLISH |
| UTC-001-02 | fromCode() with valid code "zh" | Returns CHINESE |
| UTC-001-03 | fromCode() with valid code "es" | Returns SPANISH |
| UTC-001-04 | fromCode() with invalid code "xx" | Returns DEVICE_DEFAULT |
| UTC-001-05 | fromCode() with null | Returns DEVICE_DEFAULT |
| UTC-001-06 | fromCode() with empty string | Returns DEVICE_DEFAULT |
| UTC-001-07 | All enum entries have valid displayName | No exceptions thrown |
| UTC-001-08 | All enum entries have non-blank languageName | No blank names |
| UTC-001-09 | Only DEVICE_DEFAULT has empty code | Only one entry with empty code |
| UTC-001-10 | Enum has 13 entries (DEVICE_DEFAULT + 12 languages) | Count is 13 |

#### UTC-002: SettingsStore Translation Settings Tests
**Priority:** P0
**File:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTranslationTest.kt`

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UTC-002-01 | translationEnabled defaults to false | Initial value is false |
| UTC-002-02 | setTranslationEnabled(true) updates StateFlow | StateFlow emits true |
| UTC-002-03 | setTranslationEnabled persists to SharedPreferences | putBoolean called with true |
| UTC-002-04 | translationLanguage defaults to DEVICE_DEFAULT | Initial value is DEVICE_DEFAULT |
| UTC-002-05 | translationLanguage loads persisted code "zh" | Returns CHINESE |
| UTC-002-06 | setTranslationLanguage persists code | putString called with "es" |
| UTC-002-07 | translationLanguage StateFlow emits on change | 3 emissions for 2 changes |
| UTC-002-08 | Invalid persisted code defaults to DEVICE_DEFAULT | No crash, returns DEVICE_DEFAULT |

#### UTC-003: Repository Translation Methods Tests
**Priority:** P0
**File:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/RepositoryTranslationTest.kt`

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UTC-003-01 | translationEnabled exposes StateFlow from SettingsStore | Same StateFlow instance |
| UTC-003-02 | translationLanguage exposes StateFlow from SettingsStore | Same StateFlow instance |
| UTC-003-03 | setTranslationEnabled delegates to SettingsStore | SettingsStore method called |
| UTC-003-04 | setTranslationLanguage delegates to SettingsStore | SettingsStore method called |

#### UTC-004: TranslationSettingsViewModel Tests
**Priority:** P0
**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModelTest.kt`

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UTC-004-01 | translationEnabled exposes StateFlow from Repository | StateFlow is accessible |
| UTC-004-02 | translationLanguage exposes StateFlow from Repository | StateFlow is accessible |
| UTC-004-03 | setTranslationEnabled calls Repository | Repository method called |
| UTC-004-04 | setTranslationLanguage calls Repository | Repository method called |
| UTC-004-05 | setTranslationEnabled uses viewModelScope | Coroutine is launched |
| UTC-004-06 | setTranslationLanguage uses viewModelScope | Coroutine is launched |

### UI Tests

#### UIT-001: TranslationSettingsScreen Rendering Tests
**Priority:** P0
**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreenTest.kt`

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UIT-001-01 | Screen renders toggle and language selector | Both elements displayed |
| UIT-001-02 | Toggle has correct content description | "Enable Auto Translation" |
| UIT-001-03 | Language selector has correct title | "Target Language" |
| UIT-001-04 | Back button is displayed | "Go back" button present |

#### UIT-002: TranslationSettingsScreen Interaction Tests
**Priority:** P0

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UIT-002-01 | Toggle switch updates ViewModel | setTranslationEnabled called |
| UIT-002-02 | Language dropdown expands on click | Dropdown menu shown |
| UIT-002-03 | Language dropdown shows all languages | 13 languages listed |
| UIT-002-04 | Selecting language updates ViewModel | setTranslationLanguage called |
| UIT-002-05 | Language selector disabled when translation off | Selector is not enabled |
| UIT-002-06 | Back navigation calls onNavigateUp | Callback invoked |
| UIT-002-07 | Provider link navigates to provider list | Navigation callback invoked |

#### UIT-003: Navigation Integration Tests
**Priority:** P1

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UIT-003-01 | Navigate from Settings to Translation Settings | Translation Settings screen shown |
| UIT-003-02 | Back navigation returns to Settings | Settings screen shown |
| UIT-003-03 | Deep link opens Translation Settings | Screen opens from deep link |

#### UIT-004: Settings Persistence Integration Tests
**Priority:** P0

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| UIT-004-01 | Settings persist across app restarts | Toggle and language preserved |
| UIT-004-02 | Toggle state survives configuration change | State maintained |
| UIT-004-03 | Language selection survives configuration change | State maintained |

### Edge Case Tests

#### ECT-001: Invalid Data Handling
| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| ECT-001-01 | Corrupted language code in SharedPreferences | Defaults to DEVICE_DEFAULT |
| ECT-001-02 | Missing preference keys | Uses defaults |
| ECT-001-03 | Null values in preferences | Uses defaults |
| ECT-001-04 | Rapid toggle switching (10 times) | Last state wins, no crash |
| ECT-001-05 | Back press when dropdown open | Dropdown closes, no navigation |

### Accessibility Tests

#### ACT-001: TalkBack Tests
**Priority:** P0
**Type:** Manual

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| ACT-001-01 | Toggle announcement | "Enable Auto Translation, Switch, Off/On" |
| ACT-001-02 | Language selector announcement | "Target Language, English, Button, Double tap to change" |
| ACT-001-03 | Provider link announcement | "AI Provider, OpenAI Compatible, Button" |
| ACT-001-04 | State change announced | Toggle announces "On" or "Off" |

#### ACT-002: Switch Access Tests
**Priority:** P1
**Type:** Manual

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| ACT-002-01 | All controls reachable via Switch Access | Can navigate to all elements |
| ACT-002-02 | Can toggle switch | Switch toggles correctly |
| ACT-002-03 | Can select language | Dropdown opens and selection works |
| ACT-002-04 | No focus traps | Can navigate through entire screen |

#### ACT-003: Color Contrast Tests
**Priority:** P0

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| ACT-003-01 | Title contrast ratio | ≥4.5:1 (WCAG AA) |
| ACT-003-02 | Subtitle contrast ratio | ≥4.5:1 (WCAG AA) |
| ACT-003-03 | Switch state indication | ≥4.5:1 (WCAG AA) |

### Performance Tests

#### PFT-001: Memory Leak Tests
**Priority:** P1

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| PFT-001-01 | Open/close Translation Settings 10 times | Memory usage stabilizes |
| PFT-001-02 | Force garbage collection | No increasing memory trend |
| PFT-001-03 | ViewModel cleared on back | ViewModel references released |

#### PFT-002: Frame Rate Tests
**Priority:** P1

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| PFT-002-01 | Toggle switch rapidly | 60fps maintained |
| PFT-002-02 | Open/close dropdown repeatedly | No dropped frames |
| PFT-002-03 | Screen navigation | Smooth animations |

#### PFT-003: Startup Time Tests
**Priority:** P2

| Test Case | Description | Expected Result |
|-----------|-------------|-----------------|
| PFT-003-01 | Time to render Translation Settings | <100ms |
| PFT-003-02 | No jank on navigation | Smooth transition |

### Manual Testing Scenarios

#### MNT-001: Basic Settings Flow
**Priority:** P0

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open app | App launches successfully |
| 2 | Tap Settings | Settings screen opens |
| 3 | Scroll to AI Integration | Section visible |
| 4 | Tap "Translation Settings" | Translation Settings screen opens |
| 5 | Verify title is "Translation Settings" | Title displayed |
| 6 | Tap back button | Returns to Settings |

#### MNT-002: Enable Translation Toggle
**Priority:** P0

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Translation Settings | Screen opens with toggle off |
| 2 | Tap "Enable Auto Translation" switch | Switch turns on |
| 3 | Verify switch is on | Switch shows enabled state |
| 4 | Tap switch again | Switch turns off |
| 5 | Restart app | App restarts |
| 6 | Open Translation Settings | Switch is still off |

#### MNT-003: Language Selection Flow
**Priority:** P0

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Translation Settings | Screen opens |
| 2 | Enable translation | Switch turns on |
| 3 | Tap "Target Language" | Dropdown opens |
| 4 | Verify all languages listed | 13 languages shown |
| 5 | Select "Chinese" | Dropdown closes |
| 6 | Verify subtitle shows "Chinese" | Subtitle updated |
| 7 | Reopen dropdown | "Chinese" has checkmark |
| 8 | Restart app | App restarts |
| 9 | Open Translation Settings | Language is still "Chinese" |

#### MNT-004: Disabled State Verification
**Priority:** P0

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Translation Settings | Screen opens |
| 2 | Leave translation disabled | Switch is off |
| 3 | Verify "Target Language" is grayed out | Selector appears disabled |
| 4 | Try to tap "Target Language" | Dropdown doesn't open |
| 5 | Enable translation | Switch turns on |
| 6 | Verify language selector is enabled | Selector appears enabled |

#### MNT-005: Deep Link Testing
**Priority:** P1

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run adb command | Deep link triggered |
| 2 | Verify Translation Settings opens | Screen displayed |

**Command:**
```bash
adb shell am start -W -a android.intent.action.VIEW -d "feeder://settings/translation" com.nononsenseapps.feeder.debug
```

## Test Execution Plan

### Pre-Commit Tests (Run Locally)
```bash
# Unit tests for translation components
./gradlew testPlayDebugUnitTest --tests "*TranslationLanguageTest"
./gradlew testPlayDebugUnitTest --tests "*SettingsStore*Translation*"
./gradlew testPlayDebugUnitTest --tests "*Repository*Translation*"
./gradlew testPlayDebugUnitTest --tests "*TranslationSettingsViewModelTest"
```

### Pre-Push Tests (Run Locally)
```bash
# All unit tests
./gradlew testPlayDebugUnitTest

# Lint checks
./gradlew lintPlayDebug

# Build verification
./gradlew assemblePlayDebug
```

### CI/CD Tests (Run on PR)
```bash
# All unit tests with coverage
./gradlew testPlayDebugUnitTest coverageVerification

# UI tests (on emulator)
./gradlew connectedPlayDebugAndroidTest
```

### Manual Testing Checklist
- [ ] All MNT-001 through MNT-005 scenarios executed
- [ ] ACT-001 through ACT-003 accessibility tests passed
- [ ] TalkBack announcements verified
- [ ] Switch Access tested
- [ ] Color contrast verified with Accessibility Scanner

## Test Data Management

### Test Fixtures

**File:** `app/src/test/resources/fixtures/translation-settings.json`
```json
{
  "languages": [
    {"code": "", "name": "Device Default"},
    {"code": "en", "name": "English"},
    {"code": "zh", "name": "Chinese"},
    {"code": "es", "name": "Spanish"},
    {"code": "fr", "name": "French"},
    {"code": "de", "name": "German"},
    {"code": "ja", "name": "Japanese"},
    {"code": "ko", "name": "Korean"},
    {"code": "pt", "name": "Portuguese"},
    {"code": "ru", "name": "Russian"},
    {"code": "ar", "name": "Arabic"},
    {"code": "hi", "name": "Hindi"}
  ],
  "defaultSettings": {
    "enabled": false,
    "language": "Device Default"
  }
}
```

### Mock SharedPreferences Setup

Based on existing `SettingsStoreTest.kt` pattern:
```kotlin
@Before
fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)

    // Default prefs
    every { sp.getBoolean("pref_translation_enabled", false) } returns false
    every { sp.getString("pref_translation_language", null) } returns null
}
```

## Quality Gates

All of the following must pass before marking feature complete:

### Code Quality
- [ ] All unit tests pass (100% pass rate)
- [ ] All UI tests pass (100% pass rate)
- [ ] Code coverage >90% for new code
- [ ] No lint warnings
- [ ] Build succeeds without errors
- [ ] CodeRabbit CLI review passed (no critical/high issues)

### Functional Testing
- [ ] All happy path tests pass
- [ ] All edge case tests pass
- [ ] Settings persist correctly
- [ ] Navigation works smoothly
- [ ] No crashes or ANRs

### Accessibility
- [ ] TalkBack announces all elements correctly
- [ ] Switch Access works for all controls
- [ ] Touch targets meet 48dp minimum
- [ ] Color contrast meets WCAG AA (4.5:1)

### Performance
- [ ] No memory leaks detected
- [ ] 60fps maintained during interactions
- [ ] Screen renders in <100ms

### Internationalization
- [ ] All strings translated to supported languages
- [ ] Language names display correctly in each locale
- [ ] Device language changes picked up by DEVICE_DEFAULT

## Test Report Template

### Header
- **Application:** Feeder RSS Reader
- **Feature:** Translation Configuration
- **Test Date:** [Date]
- **Tester:** QA Agent
- **Environment:** [Device/Emulator details]

### Executive Summary
- **Total Tests:** [Number]
- **Passed:** [Number]
- **Failed:** [Number]
- **Skipped:** [Number]
- **Pass Rate:** [Percentage]%

### Coverage Report
- **Unit Test Coverage:** [Percentage]%
- **New Code Coverage:** [Percentage]%
- **UI Test Coverage:** [Percentage]%

### Results by Category

#### Unit Tests
- TranslationLanguage enum: PASS/FAIL
- SettingsStore: PASS/FAIL
- Repository: PASS/FAIL
- ViewModel: PASS/FAIL

#### UI Tests
- TranslationSettingsScreen: PASS/FAIL
- Navigation Integration: PASS/FAIL
- Settings Persistence: PASS/FAIL

#### Edge Cases
- Invalid Data Handling: PASS/FAIL
- Rapid State Changes: PASS/FAIL

#### Accessibility
- TalkBack: PASS/FAIL
- Switch Access: PASS/FAIL
- Color Contrast: PASS/FAIL

#### Performance
- Memory Leaks: PASS/FAIL
- Frame Rate: PASS/FAIL
- Startup Time: PASS/FAIL

### Defects Found

#### DEF-001: [Title]
- **Severity:** Critical/High/Medium/Low
- **Test Case:** [Test ID]
- **Steps to Reproduce:** [Steps]
- **Expected:** [Expected behavior]
- **Actual:** [Actual behavior]
- **Evidence:** [Screenshot/log path]

### Recommendations
1. [Recommendation with rationale]
2. [Recommendation with rationale]

### Artifacts
- Test traces: `./traces/`
- Screenshots: `./screenshots/`
- Network logs: `./network/`
- JUnit XML: `./app/build/test-results/`
- Coverage report: `./app/build/reports/coverage/`

## Continuous Improvement

### Metrics to Track
- Test execution time
- Test failure rate
- Code coverage percentage
- Bug escape rate (production vs caught by tests)

### Test Smells to Avoid
- Fragile tests that break on minor changes
- Slow tests that take too long
- Over-mocked tests that test nothing
- Tests too coupled to implementation details

## References

- **Tech Spec:** [./01-tech-spec.md](./01-tech-spec.md)
- **Task List:** [./03-tasks.md](./03-tasks.md)
- **Testing Strategy:** [./04-testing-strategy.md](./04-testing-strategy.md)
- **Android Testing Guide:** https://developer.android.com/training/testing
- **Compose Testing:** https://developer.android.com/jetpack/compose/testing
- **Accessibility Testing:** https://developer.android.com/guide/topics/ui/accessibility/testing
