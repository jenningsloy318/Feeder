# QA Test Plan: AI Summary Configuration Feature

**Created:** 2026-01-01 19:12:00 +08:00
**Feature:** Improve AI Integration Summary Configuration
**Status:** Ready for QA
**Version:** 1.0

---

## Test Summary

- **Application**: Feeder (Android RSS Reader)
- **Modality**: Android Mobile App (Jetpack Compose)
- **Feature**: AI Summary Configuration Screen
- **Implementation Phase**: 11 (Complete Implementation + Testing)
- **Test Execution Date**: 2026-01-01

---

## Test Strategy

### Risk Assessment

| Risk Area | Probability | Impact | Mitigation |
|-----------|-------------|--------|------------|
| Settings persistence failure | Medium | High | Unit tests for SharedPreferences, verify default values |
| Navigation flow broken | Low | High | UI navigation tests, verify back stack |
| State management issues | Medium | High | ViewModel tests, StateFlow verification |
| Accessibility violations | Low | Medium | Manual accessibility audit, content descriptions |
| Performance regression | Low | Medium | Navigation timing tests, state change latency |
| Breaking changes (regression) | Medium | Critical | Full regression test suite execution |

### Coverage Targets

Based on the specification, we must achieve:

- [x] **Unit Tests** - ViewModel, Repository, DAO
  - SummarySettingsViewModel state management
  - SettingsStore persistence
  - Repository integration
  - AIApi business logic

- [x] **Integration Tests** - Data flow
  - Settings save/load cycle
  - State propagation from Repository → ViewModel → UI
  - SharedPreferences persistence

- [x] **UI Tests** - User interactions
  - Navigation to/from Summary Settings screen
  - Toggle switch interaction
  - Language dropdown interaction
  - Disabled state handling

- [x] **Regression Tests** - Existing features
  - All existing settings functionality
  - AI provider configuration
  - Other settings screens

---

## Test Cases

### Category 1: Unit Tests

#### TC-UNIT-001: SettingsStore - summaryEnabled StateFlow
- **Priority**: P0 (Critical)
- **Preconditions**: Mock SharedPreferences
- **Steps**:
  1. Create SettingsStore with mocked SP
  2. Call `store.summaryEnabled.value`
  3. Verify default value is `true`
  4. Call `store.setSummaryEnabled(false)`
  5. Verify SharedPreferences.edit() was called
  6. Verify StateFlow updates to `false`
- **Expected Result**: Default `true`, setter persists, StateFlow reflects changes
- **Validation Type**: JUnit assertion, MockK verification

#### TC-UNIT-002: SettingsStore - PREF_SUMMARY_ENABLED Key
- **Priority**: P0 (Critical)
- **Preconditions**: Mock SharedPreferences
- **Steps**:
  1. Set summaryEnabled to false
  2. Verify SP.edit().putBoolean() called with key "pref_summary_enabled"
  3. Set summaryEnabled to true
  4. Verify SP.edit().putBoolean() called with same key
- **Expected Result**: Correct preference key used
- **Validation Type**: MockK verify

#### TC-UNIT-003: SummarySettingsViewModel - setSummaryEnabled
- **Priority**: P0 (Critical)
- **Preconditions**: Mock Repository, create ViewModel
- **Steps**:
  1. Create SummarySettingsViewModel with mocked Repository
  2. Call `viewModel.setSummaryEnabled(false)`
  3. Verify `repository.setSummaryEnabled(false)` was called
  4. Verify StateFlow updated
- **Expected Result**: ViewModel delegates to Repository
- **Validation Type**: MockK verification, StateFlow assertion

#### TC-UNIT-004: SummarySettingsViewModel - setSummaryLanguage
- **Priority**: P0 (Critical)
- **Preconditions**: Mock Repository, create ViewModel
- **Steps**:
  1. Create SummarySettingsViewModel with mocked Repository
  2. Call `viewModel.setSummaryLanguage(SummaryLanguage.ENGLISH)`
  3. Verify `repository.setSummaryLanguage()` was called
  4. Verify StateFlow updated
- **Expected Result**: ViewModel delegates to Repository
- **Validation Type**: MockK verification, StateFlow assertion

#### TC-UNIT-005: AIApi - summarize() with disabled state
- **Priority**: P0 (Critical)
- **Preconditions**: Mock Repository with summaryEnabled = false
- **Steps**:
  1. Mock repository.summaryEnabled to return flowOf(false)
  2. Call `aiApi.summarize("test content")`
  3. Capture result
  4. Verify result is SummaryResult.Error
  5. Verify result.content is empty string
  6. Verify AI client was NOT called
- **Expected Result**: Returns empty error without calling AI
- **Validation Type**: JUnit assertion, MockK verify (never)

#### TC-UNIT-006: AIApi - summarize() with enabled state
- **Priority**: P0 (Critical)
- **Preconditions**: Mock Repository with summaryEnabled = true
- **Steps**:
  1. Mock repository.summaryEnabled to return flowOf(true)
  2. Mock repository.summaryLanguage to return flowOf(SummaryLanguage.ENGLISH)
  3. Mock AIClient.generateSummary() to return success
  4. Call `aiApi.summarize("test content")`
  5. Capture result
  6. Verify AI client was called with correct parameters
- **Expected Result**: Proceeds to generate summary
- **Validation Type**: MockK verify

---

### Category 2: Integration Tests

#### TC-INT-001: Settings Persistence Cycle
- **Priority**: P0 (Critical)
- **Preconditions**: App running, SharedPreferences accessible
- **Steps**:
  1. Get current summaryEnabled value
  2. Set summaryEnabled to false
  3. Kill app process
  4. Restart app
  5. Get summaryEnabled value again
- **Expected Result**: Value persists across restarts (false)
- **Validation Type**: Assertion

#### TC-INT-002: State Propagation (Repository → ViewModel → UI)
- **Priority**: P0 (Critical)
- **Preconditions**: Compose UI active on Summary Settings screen
- **Steps**:
  1. Observe UI state for summaryEnabled
  2. Call repository.setSummaryEnabled(false) from background
  3. Verify ViewModel StateFlow updates
  4. Verify UI recomposes with new state
- **Expected Result**: State changes propagate immediately to UI
- **Validation Type**: UI observation, timing measurement

#### TC-INT-003: Default Value on Fresh Install
- **Priority**: P1 (High)
- **Preconditions**: Fresh app install (clear data)
- **Steps**:
  1. Clear app data
  2. Launch app
  3. Navigate to Summary Settings
  4. Check toggle state
- **Expected Result**: Toggle is ON (enabled = true)
- **Validation Type**: Visual verification

---

### Category 3: UI Tests

#### TC-UI-001: Navigation - Settings → Summary Settings
- **Priority**: P0 (Critical)
- **Preconditions**: On Settings screen
- **Steps**:
  1. Find "Summary" menu item
  2. Click on "Summary"
  3. Observe navigation
  4. Verify current destination route
- **Expected Result**: Navigates to "settings/summary", screen title shows "Summary Settings"
- **Validation Type**: Compose UI test, NavController assertion

#### TC-UI-002: Navigation - Back Button
- **Priority**: P0 (Critical)
- **Preconditions**: On Summary Settings screen
- **Steps**:
  1. Press back button (in toolbar or system)
  2. Observe navigation
  3. Verify current destination
- **Expected Result**: Returns to Settings screen (AI Integration section)
- **Validation Type**: Compose UI test, back stack verification

#### TC-UI-003: Toggle Switch - Enable to Disable
- **Priority**: P0 (Critical)
- **Preconditions**: On Summary Settings screen, toggle ON
- **Steps**:
  1. Find toggle switch with text "Enable Summaries"
  2. Click switch to turn OFF
  3. Verify switch state (visual)
  4. Verify language selector becomes disabled (grayed out)
  5. Check SharedPreferences for persisted value
- **Expected Result**: Switch toggles OFF, language selector disabled, value persisted
- **Validation Type**: UI assertion, SharedPreferences check

#### TC-UI-004: Toggle Switch - Disable to Enable
- **Priority**: P0 (Critical)
- **Preconditions**: On Summary Settings screen, toggle OFF
- **Steps**:
  1. Find toggle switch
  2. Click switch to turn ON
  3. Verify switch state (visual)
  4. Verify language selector becomes enabled
  5. Check SharedPreferences for persisted value
- **Expected Result**: Switch toggles ON, language selector enabled, value persisted
- **Validation Type**: UI assertion, SharedPreferences check

#### TC-UI-005: Language Selector - Expand Dropdown
- **Priority**: P1 (High)
- **Preconditions**: On Summary Settings screen, toggle ON
- **Steps**:
  1. Find language selector
  2. Click on dropdown
  3. Observe dropdown menu
- **Expected Result**: Dropdown expands, shows all language options
- **Validation Type**: Compose UI test

#### TC-UI-006: Language Selector - Select Language
- **Priority**: P1 (High)
- **Preconditions**: Dropdown expanded
- **Steps**:
  1. Click on "Japanese" (or any language)
  2. Verify dropdown closes
  3. Verify selection persists (check text)
  4. Check SharedPreferences
  5. Re-expand dropdown, verify checkmark on selected
- **Expected Result**: Language selected, persisted, checkmark shown
- **Validation Type**: UI assertion, SharedPreferences check

#### TC-UI-007: Main Settings - Display Shows "Summary"
- **Priority**: P0 (Critical)
- **Preconditions**: On Settings screen
- **Steps**:
  1. Find "AI Integration" section
  2. Find menu item below "AI Provider"
  3. Read text
- **Expected Result**: Text is "Summary" (NOT "Summary Language")
- **Validation Type**: Compose UI test, text assertion

#### TC-UI-008: Main Settings - Status Display
- **Priority**: P1 (High)
- **Preconditions**: On Settings screen, various toggle states
- **Steps**:
  1. Set summaryEnabled = true, language = English
  2. Check "Summary" subtitle
  3. Set summaryEnabled = false
  4. Check "Summary" subtitle again
- **Expected Result**:
  - When enabled: "English (Enabled)" or similar
  - When disabled: "Disabled"
- **Validation Type**: UI assertion

#### TC-UI-009: Navigation Arrow Icon
- **Priority**: P1 (High)
- **Preconditions**: On Settings screen
- **Steps**:
  1. Find "Summary" menu item
  2. Check icon
- **Expected Result**: Shows navigation arrow (chevron-right), NOT dropdown arrow
- **Validation Type**: Visual verification, icon assertion

---

### Category 4: Regression Tests

#### TC-REG-001: Existing Settings Screens Load
- **Priority**: P0 (Critical)
- **Preconditions**: App launched
- **Steps**:
  1. Navigate to each settings screen
  2. Verify no crashes
  3. Verify UI renders correctly
- **Expected Result**: All settings screens load without errors
- **Validation Type**: Manual test

#### TC-REG-002: AI Provider Configuration Unchanged
- **Priority**: P0 (Critical)
- **Preconditions**: App launched
- **Steps**:
  1. Navigate to AI Provider settings
  2. Verify all fields present
  3. Change provider settings
  4. Verify persistence
- **Expected Result**: AI provider config works as before
- **Validation Type**: Manual test

#### TC-REG-003: Summary Language Migration
- **Priority**: P0 (Critical)
- **Preconditions**: App with existing summary_language preference
- **Steps**:
  1. Set summary_language in SharedPreferences (simulate old version)
  2. Launch new version
  3. Check if language setting preserved
  4. Check if summaryEnabled defaults to true
- **Expected Result**: Old language setting migrated, new toggle defaults to enabled
- **Validation Type**: SharedPreferences check

#### TC-REG-004: Other Settings Persist Correctly
- **Priority**: P1 (High)
- **Preconditions**: Multiple settings configured
- **Steps**:
  1. Configure various settings (theme, sync, etc.)
  2. Restart app
  3. Verify all settings persisted
- **Expected Result**: No interference with other SharedPreferences
- **Validation Type**: SharedPreferences check

---

### Category 5: Performance Tests

#### TC-PERF-001: Navigation Latency
- **Priority**: P1 (High)
- **Preconditions**: On Settings screen
- **Steps**:
  1. Click "Summary"
  2. Measure time to screen render
  3. Repeat 10 times
- **Expected Result**: Navigation completes within 100ms (NFR)
- **Validation Type**: Timing measurement

#### TC-PERF-002: Toggle State Change Latency
- **Priority**: P1 (High)
- **Preconditions**: On Summary Settings screen
- **Steps**:
  1. Click toggle
  2. Measure time to visual update
  3. Repeat 10 times
- **Expected Result**: State change instantaneous (<50ms)
- **Validation Type**: Timing measurement

---

### Category 6: Accessibility Tests

#### TC-ACC-001: Toggle Switch Content Description
- **Priority**: P1 (High)
- **Preconditions**: TalkBack enabled
- **Steps**:
  1. Focus on toggle switch
  2. Listen to announcement
- **Expected Result**: Announces "Enable Summaries, On/Off"
- **Validation Type**: Manual accessibility audit

#### TC-ACC-002: Touch Targets (48dp Minimum)
- **Priority**: P1 (High)
- **Preconditions**: On Summary Settings screen
- **Steps**:
  1. Measure toggle switch size
  2. Measure dropdown touch area
  3. Verify minimum 48dp
- **Expected Result**: All interactive elements ≥48dp
- **Validation Type**: UI measurement

#### TC-ACC-003: Screen Reader Navigation
- **Priority**: P2 (Medium)
- **Preconditions**: TalkBack enabled
- **Steps**:
  1. Swipe through screen elements
  2. Verify logical order
  3. Verify all elements announced
- **Expected Result**: Logical focus order, all elements accessible
- **Validation Type**: Manual accessibility audit

---

### Category 7: Manual Testing

#### TC-MAN-001: End-to-End Workflow
- **Priority**: P0 (Critical)
- **Preconditions**: Fresh app install
- **Steps**:
  1. Launch app
  2. Configure AI provider (OpenAI/Anthropic)
  3. Navigate to Settings → AI Integration
  4. Click "Summary"
  5. Toggle summaries OFF
  6. Press back
  7. Verify "Summary" shows "Disabled"
  8. Navigate back to Summary Settings
  9. Toggle ON
  10. Select language "Japanese"
  11. Press back
  12. Verify "Summary" shows "Japanese (Enabled)"
  13. Restart app
  14. Verify settings persisted
- **Expected Result**: Complete workflow works smoothly
- **Validation Type**: Manual verification

#### TC-MAN-002: Integration with AI Summary
- **Priority**: P0 (Critical)
- **Preconditions**: AI configured, test feed available
- **Steps**:
  1. Enable summaries
  2. Open article
  3. Trigger summary generation
  4. Verify summary appears
  5. Disable summaries
  6. Try to generate summary again
- **Expected Result**:
  - When enabled: Summary generates
  - When disabled: Summary does not generate
- **Validation Type**: Manual verification

---

## Acceptance Criteria Verification

### Functional Requirements

- [ ] **FR-1:** "Summary Language" renamed to "Summary"
  - TC-UI-007: Main Settings screen shows "Summary"
  - TC-UI-008: Navigation destination title shows "Summary Settings"

- [ ] **FR-2:** Navigate to Dedicated Screen
  - TC-UI-001: Click on "Summary" navigates to new screen
  - TC-UI-002: Back button returns to AI Integration settings

- [ ] **FR-3:** Add Enable/Disable Toggle
  - TC-UNIT-005: AIApi returns empty error when disabled
  - TC-UI-003: Toggle can turn OFF
  - TC-UI-004: Toggle can turn ON
  - TC-INT-001: State persists across restarts

- [ ] **FR-4:** Language Selection
  - TC-UI-005: Language selector expands
  - TC-UI-006: Language can be selected
  - TC-INT-001: Selection persists

- [ ] **FR-5:** Settings Screen Preview
  - TC-UI-008: Shows current language and enabled/disabled status

### Non-Functional Requirements

- [ ] **NFR-1:** Performance
  - TC-PERF-001: Navigation within 100ms
  - TC-PERF-002: Toggle state changes instantaneous

- [ ] **NFR-2:** Data Persistence
  - TC-INT-001: SharedPreferences used correctly
  - TC-UNIT-001: Default value is true

- [ ] **NFR-3:** Backward Compatibility
  - TC-REG-003: Existing language setting migrated
  - TC-UNIT-001: New field defaults to enabled

- [ ] **NFR-4:** UI/UX Consistency
  - TC-MAN-001: Follows existing patterns
  - Visual inspection: Material Design 3

- [ ] **NFR-5:** Accessibility
  - TC-ACC-001: Content descriptions present
  - TC-ACC-002: Touch targets ≥48dp
  - TC-ACC-003: Screen reader support

### Technical Requirements

- [ ] **TR-1:** Navigation
  - TC-UI-001: Route is "settings/summary"
  - TC-UI-002: Proper back navigation

- [ ] **TR-2:** ViewModel
  - TC-UNIT-003: Manages state correctly
  - TC-UNIT-004: Exposes StateFlows

- [ ] **TR-3:** UI Components
  - TC-UI-003: Switch component works
  - TC-UI-005: Dropdown component works

- [ ] **TR-4:** Data Storage
  - TC-UNIT-002: Uses SharedPreferences
  - TC-INT-001: Thread-safe operations

- [ ] **TR-5:** String Resources
  - Code review: All strings externalized
  - TC-UI-007: No hardcoded strings

---

## Test Execution Plan

### Test Environment

**Device/Emulator:**
- Primary: Pixel 2 API 30 (as configured in gradle)
- Secondary: Physical device (if available)

**Build Variants:**
- fdroidDebug (for testing)
- playRelease (for smoke test before deployment)

**Test Data:**
- Test RSS feeds with articles
- AI provider credentials (OpenAI/Anthropic)

### Execution Sequence

#### Phase 1: Unit Tests (Automated)
```bash
./gradlew test --tests "*SummarySettings*" --tests "*SettingsStore*" --tests "*AIApi*"
```

#### Phase 2: Integration Tests (Automated + Manual)
```bash
./gradlew connectedFdroidDebugAndroidTest
```

#### Phase 3: UI Tests (Automated)
```bash
./gradlew connectedFdroidDebugAndroidTest --tests "*SummarySettings*Test"
```

#### Phase 4: Regression Tests (Automated)
```bash
./gradlew test
./gradlew connectedFdroidDebugAndroidTest
```

#### Phase 5: Performance Tests (Manual)
- Manual timing measurements
- Navigation latency
- State change latency

#### Phase 6: Accessibility Tests (Manual)
- TalkBack navigation
- Touch target verification
- Content description audit

#### Phase 7: End-to-End Manual Testing
- Complete user workflows
- Integration scenarios
- Edge cases

---

## Success Criteria

### Pass/Fail Thresholds

**Unit Tests:**
- Must pass: 100%
- Coverage: ≥80% for new code

**Integration Tests:**
- Must pass: 100%
- No data loss scenarios

**UI Tests:**
- Must pass: 100%
- No UI crashes or ANRs

**Regression Tests:**
- Must pass: 100% (no regressions allowed)

**Manual Tests:**
- All P0 test cases must pass
- All P1 test cases must pass
- P2 test cases: best effort

**Performance:**
- Navigation: ≤100ms (NFR)
- State change: ≤50ms

**Accessibility:**
- All critical violations fixed
- All P1 violations fixed

---

## Test Artifacts

### Deliverables

1. **Test Execution Report**
   - Pass/fail counts per category
   - Execution time
   - Device/emulator details
   - Build version

2. **Defect Report**
   - Defect ID
   - Severity (Critical/High/Medium/Low)
   - Test Case reference
   - Steps to reproduce
   - Expected vs Actual
   - Screenshots/logs

3. **Coverage Report**
   - Line coverage
   - Branch coverage
   - New/changed code coverage

4. **Performance Metrics**
   - Navigation timings
   - State change timings
   - Memory usage

5. **Accessibility Audit Report**
   - Violations found
   - WCAG compliance level
   - Recommendations

---

## Defect Severity Classification

| Severity | Definition | Example |
|----------|------------|---------|
| Critical | Blocks feature, data loss, crash | App crashes on navigation, settings not persisting |
| High | Major functionality broken | Toggle not working, navigation broken |
| Medium | Partial functionality, workaround exists | Language selector doesn't show checkmark |
| Low | Cosmetic, text issues | Misaligned text, minor layout issue |

---

## Test Schedule

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Unit Tests | 30 min | Implementation complete |
| Integration Tests | 45 min | Unit tests passing |
| UI Tests | 1 hour | Integration tests passing |
| Regression Tests | 2 hours | UI tests passing |
| Performance Tests | 30 min | All automated tests passing |
| Accessibility Tests | 1 hour | UI stable |
| Manual Testing | 2 hours | All previous phases complete |
| **Total** | **~7.5 hours** | |

---

## Contingency Plans

### If Tests Fail

1. **Unit Test Failures**
   - Investigate root cause (code vs test)
   - Fix code issues
   - Re-run only failed tests
   - Max 3 iterations before escalating

2. **UI Test Failures**
   - Check for timing issues
   - Verify selectors/IDs
   - Increase waits if needed
   - Escalate flaky tests

3. **Integration Test Failures**
   - Check SharedPreferences mocking
   - Verify DI configuration
   - Check for threading issues
   - Escalate after 3 attempts

4. **Regression Failures**
   - **IMMEDIATE STOP**
   - Investigate breaking changes
   - Do not proceed until fixed
   - May require implementation rollback

---

## Conclusion

This test plan provides comprehensive coverage for the AI Summary Configuration feature. It aligns with the technical specification and ensures all acceptance criteria are verified before deployment.

**Test Plan Status:** Ready for execution
**Next Step:** Wait for dev-executor to complete implementation

---

**QA Test Plan Complete:** 2026-01-01 19:12:00 +08:00
