# Implementation Plan: Decouple Target Language Settings

**Specification:** [./06-specification.md](./06-specification.md)
**Estimated Phases:** 1 (Single-phase implementation)
**Estimated Time:** 30-60 minutes
**Complexity:** Low

**CRITICAL:** This is a single-phase implementation plan. All tasks will be completed in one continuous execution session without stopping between phases.

---

## Milestones

### Mile 1 (Phase 1): Remove `enabled` Parameter from Language Selectors

**Goal:** Decouple language selector enabled state from auto-feature toggle state by removing the `enabled` parameter dependency.

**Dependencies:** None

**Risk Level:** Low

#### Deliverables

- [ ] Modified `SummarySettingsScreen.kt` with `enabled` parameter removed
- [ ] Modified `TranslationSettingsScreen.kt` with `enabled` parameter removed
- [ ] All tests passing (unit + UI)
- [ ] Code review approved

#### Acceptance Criteria

- [ ] Language selector is interactive when auto-feature switch is OFF
- [ ] Language selector is interactive when auto-feature switch is ON
- [ ] Dropdown menu opens in all states
- [ ] No compilation errors
- [ ] No test failures
- [ ] No visual regressions

#### Files Affected

1. **app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt**
   - Line 101: Remove `enabled = summaryEnabled` argument
   - Line 114: Remove `enabled: Boolean` parameter from function signature
   - Line 125: Remove `enabled = enabled` from `.clickable()` modifier

2. **app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt**
   - Line 101: Remove `enabled = translationEnabled` argument
   - Line 114: Remove `enabled: Boolean` parameter from function signature
   - Line 125: Remove `enabled = enabled` from `.clickable()` modifier

#### Implementation Steps

**Step 1: Modify SummarySettingsScreen.kt**
1. Open `SummarySettingsScreen.kt`
2. Locate `LanguageSelectorSetting` function (line 110)
3. Remove `enabled: Boolean` parameter from function signature
4. Locate `.clickable(enabled = enabled)` call (line 125)
5. Change to `.clickable {` (remove `enabled = enabled`)
6. Locate `LanguageSelectorSetting` call site (line 97)
7. Remove `enabled = summaryEnabled,` argument

**Step 2: Modify TranslationSettingsScreen.kt**
1. Open `TranslationSettingsScreen.kt`
2. Locate `LanguageSelectorSetting` function (line 110)
3. Remove `enabled: Boolean` parameter from function signature
4. Locate `.clickable(enabled = enabled)` call (line 125)
5. Change to `.clickable {` (remove `enabled = enabled`)
6. Locate `LanguageSelectorSetting` call site (line 97)
7. Remove `enabled = translationEnabled,` argument

**Step 3: Build and Test**
1. Run Gradle build: `./gradlew assembleDebug`
2. Fix any compilation errors (should be none)
3. Run unit tests: `./gradlew test`
4. Run UI tests: `./gradlew connectedAndroidTest`

**Step 4: Manual Verification**
1. Install debug APK on device/emulator
2. Open Summary Settings screen
3. Verify auto-summary is OFF
4. Tap "Summary Language" - should open dropdown
5. Select a language - should save
6. Open Translation Settings screen
7. Verify auto-translation is OFF
8. Tap "Target Language" - should open dropdown
9. Select a language - should save

**Step 5: Accessibility Verification**
1. Use keyboard navigation (Tab/Enter)
2. Use screen reader (TalkBack)
3. Verify focus indicators visible
4. Verify announcements correct

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Compilation error** | Very Low | Low | Kotlin type system prevents most errors; change is simple |
| **Test failure** | Low | Low | Existing tests don't depend on enabled state; new tests added |
| **Visual regression** | Very Low | Low | No visual changes - only behavioral |
| **Accessibility regression** | Very Low | Medium | Removing disabled state improves accessibility |
| **Performance degradation** | None | None | Reduces recomposition - slight improvement |
| **User confusion** | Low | Low | Language setting behavior more intuitive now |

**Overall Risk:** **Very Low**

**Mitigation Strategy:**
- Simple code change (6 lines total)
- No state management changes
- No data model changes
- No API changes
- Easy rollback if needed

---

## Dependencies

### External Dependencies

**None** - This change does not depend on any external systems, libraries, or services.

### Internal Dependencies

**None** - This change is isolated to two UI files and does not depend on other modules or components.

**Impact Scope:** Limited to settings screens only

---

## Success Metrics

### Functional Requirements

- [ ] **FR1:** Language selector is enabled when auto-feature is OFF
- [ ] **FR2:** Language selector is enabled when auto-feature is ON
- [ ] **FR3:** Dropdown menu opens when language selector tapped (all states)
- [ ] **FR4:** Language selection saves to preferences (all states)
- [ ] **FR5:** Switch toggle does not affect language selector state

### Non-Functional Requirements

- [ ] **NFR1:** No compilation errors
- [ ] **NFR2:** All existing tests pass
- [ ] **NFR3:** No visual regressions
- [ ] **NFR4:** WCAG 2.1 AA accessibility compliance maintained
- [ ] **NFR5:** No performance degradation

### Quality Requirements

- [ ] **QR1:** Code follows project style guidelines
- [ ] **QR2:** No unnecessary complexity added
- [ ] **QR3:** Change is minimal and focused
- [ ] **QR4:** No new dependencies introduced

---

## Implementation Notes

### Code Quality Guidelines

1. **Minimal Changes:** Only modify what's necessary (6 lines)
2. **No Refactoring:** Don't reorganize unrelated code
3. **Preserve Formatting:** Keep existing code style
4. **No New Files:** Don't create new classes or files
5. **No String Changes:** Don't modify user-facing text

### Testing Strategy

**Pre-Implementation:**
- Run existing tests to establish baseline
- Verify no existing failures

**During Implementation:**
- Run compilation after each file change
- Run unit tests after both files modified
- Fix any issues immediately

**Post-Implementation:**
- Run full test suite
- Perform manual QA on device
- Verify accessibility features

### Common Pitfalls to Avoid

1. **Don't forget to remove `enabled` parameter from BOTH files**
2. **Don't modify ViewModel logic** (no changes needed)
3. **Don't change string resources** (no new text needed)
4. **Don't add new tests** unless existing tests fail
5. **Don't over-engineer** - keep change minimal

### Verification Commands

```bash
# Build project
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run UI tests (requires emulator/device)
./gradlew connectedAndroidTest

# Install on device for manual testing
./gradlew installDebug

# Check code style (if project uses ktlint/detekt)
./gradlew ktlintCheck
./gradlew detekt
```

---

## Post-Implementation Checklist

### Code Review

- [ ] Changes reviewed by peer
- [ ] Specification followed accurately
- [ ] No unintended modifications
- [ ] Code is clean and readable

### Testing

- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] Manual testing completed
- [ ] Accessibility testing completed

### Documentation

- [ ] Specification updated (if needed)
- [ ] Implementation summary written
- [ ] Release notes drafted

### Deployment

- [ ] Code committed to version control
- [ ] Commit message follows project conventions
- [ ] Changes merged to appropriate branch
- [ ] Ready for next release

---

## Rollback Plan

**If Issues Arise:**

1. **Immediate Rollback:**
   ```bash
   git revert <commit-hash>
   git push
   ```

2. **Hotfix Release:**
   - Build new APK with reverted changes
   - Deploy to Play Store
   - Communicate with users (if needed)

3. **Investigation:**
   - Analyze root cause
   - Fix issue properly
   - Re-implement with fixes

**Rollback Risk:** Low - Simple revert of 6 lines

---

## Next Steps

1. **Review Specification:** Read [./06-specification.md](./06-specification.md) thoroughly
2. **Review Task List:** Read [./08-task-list.md](./08-task-list.md) for detailed tasks
3. **Begin Implementation:** Follow tasks in sequential order
4. **Test Thoroughly:** Verify all acceptance criteria
5. **Commit Changes:** Use descriptive commit message

---

**Implementation Plan Status:** Ready for Execution
**Estimated Duration:** 30-60 minutes
**Confidence Level:** High (Low-risk change)
**Last Updated:** 2026-01-05
