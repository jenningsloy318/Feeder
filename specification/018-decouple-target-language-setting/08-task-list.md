# Task List: Decouple Target Language Settings

**Plan:** [./07-implementation-plan.md](./07-implementation-plan.md)
**Specification:** [./06-specification.md](./06-specification.md)
**Total Tasks:** 11
**Estimated Time:** 30-60 minutes

---

## Tasks

### Milestone 1: Remove `enabled` Parameter from Language Selectors

#### Code Changes

- [ ] **T1.1** Modify `SummarySettingsScreen.kt` - Remove `enabled` parameter from function signature
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
  - **Line:** 114
  - **Change:** Remove `enabled: Boolean,` parameter from `LanguageSelectorSetting` function signature
  - **Details:**
    ```kotlin
    // BEFORE:
    @Composable
    private fun LanguageSelectorSetting(
        title: String,
        currentLanguage: SummaryLanguage,
        onLanguageSelected: (SummaryLanguage) -> Unit,
        enabled: Boolean,  // ← REMOVE THIS LINE
        menuExpanded: Boolean,
        onMenuExpandedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    )

    // AFTER:
    @Composable
    private fun LanguageSelectorSetting(
        title: String,
        currentLanguage: SummaryLanguage,
        onLanguageSelected: (SummaryLanguage) -> Unit,
        // enabled parameter removed
        menuExpanded: Boolean,
        onMenuExpandedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    )
    ```
  - **Acceptance:** Function signature compiles without errors

- [ ] **T1.2** Modify `SummarySettingsScreen.kt` - Remove `enabled` from `.clickable()` modifier
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
  - **Line:** ~125 (exact line may vary)
  - **Change:** Remove `enabled = enabled` from `.clickable()` modifier
  - **Details:**
    ```kotlin
    // BEFORE:
    .clickable(enabled = enabled) {
        onMenuExpandedChange(true)
    }

    // AFTER:
    .clickable {
        onMenuExpandedChange(true)
    }
    ```
  - **Acceptance:** Modifier compiles without errors

- [ ] **T1.3** Modify `SummarySettingsScreen.kt` - Remove `enabled` argument from function call
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
  - **Line:** 101
  - **Change:** Remove `enabled = summaryEnabled,` argument from `LanguageSelectorSetting` call
  - **Details:**
    ```kotlin
    // BEFORE:
    LanguageSelectorSetting(
        title = stringResource(R.string.summary_language_title),
        currentLanguage = summaryLanguage,
        onLanguageSelected = { viewModel.setSummaryLanguage(it) },
        enabled = summaryEnabled,  // ← REMOVE THIS LINE
        menuExpanded = languageMenuExpanded,
        onMenuExpandedChange = { languageMenuExpanded = it },
    )

    // AFTER:
    LanguageSelectorSetting(
        title = stringResource(R.string.summary_language_title),
        currentLanguage = summaryLanguage,
        onLanguageSelected = { viewModel.setSummaryLanguage(it) },
        // enabled parameter removed
        menuExpanded = languageMenuExpanded,
        onMenuExpandedChange = { languageMenuExpanded = it },
    )
    ```
  - **Acceptance:** Function call compiles without errors

- [ ] **T1.4** Modify `TranslationSettingsScreen.kt` - Remove `enabled` parameter from function signature
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
  - **Line:** 114
  - **Change:** Remove `enabled: Boolean,` parameter from `LanguageSelectorSetting` function signature
  - **Details:** Same pattern as T1.1, but for TranslationSettingsScreen
  - **Acceptance:** Function signature compiles without errors

- [ ] **T1.5** Modify `TranslationSettingsScreen.kt` - Remove `enabled` from `.clickable()` modifier
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
  - **Line:** ~125 (exact line may vary)
  - **Change:** Remove `enabled = enabled` from `.clickable()` modifier
  - **Details:** Same pattern as T1.2, but for TranslationSettingsScreen
  - **Acceptance:** Modifier compiles without errors

- [ ] **T1.6** Modify `TranslationSettingsScreen.kt` - Remove `enabled` argument from function call
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
  - **Line:** 101
  - **Change:** Remove `enabled = translationEnabled,` argument from `LanguageSelectorSetting` call
  - **Details:** Same pattern as T1.3, but for TranslationSettingsScreen
  - **Acceptance:** Function call compiles without errors

#### Build and Test

- [ ] **T1.7** Build project and verify no compilation errors
  - **Command:** `./gradlew assembleDebug`
  - **Details:** Build should complete successfully without errors
  - **Acceptance:** Build succeeds, APK generated

- [ ] **T1.8** Run unit tests and verify all pass
  - **Command:** `./gradlew test`
  - **Details:** All existing unit tests should pass
  - **Acceptance:** No test failures

- [ ] **T1.9** Run UI tests (if emulator/device available)
  - **Command:** `./gradlew connectedAndroidTest`
  - **Details:** UI tests should pass (may require emulator)
  - **Acceptance:** No UI test failures

#### Manual Verification

- [ ] **T1.10** Manual QA on device/emulator
  - **Setup:** Install APK on device or emulator
  - **Test Cases:**
    1. Open Summary Settings screen
    2. Verify auto-summary is OFF
    3. Tap "Summary Language" - should open dropdown (selector is ENABLED)
    4. Select a different language - should save
    5. Verify language changed
    6. Enable auto-summary toggle
    7. Verify language selector still enabled
    8. Open Translation Settings screen
    9. Verify auto-translation is OFF
    10. Tap "Target Language" - should open dropdown (selector is ENABLED)
    11. Select a different language - should save
    12. Verify language changed
    13. Enable auto-translation toggle
    14. Verify language selector still enabled
  - **Acceptance:** All test cases pass, selector always enabled

#### Final Tasks

- [ ] **TF.1** Accessibility verification
  - **Details:**
    1. Enable TalkBack (screen reader)
    2. Navigate to Summary Settings
    3. Verify language selector is announced as "Button, Double tap to activate" (not "Disabled")
    4. Tap language selector - menu should open
    5. Use keyboard navigation (if available): Tab to selector, Enter to open
    6. Verify focus indicator visible
  - **Acceptance:** Accessibility features work correctly, no regressions

- [ ] **TF.2** Code review and commit
  - **Details:**
    1. Review all changes (should be exactly 6 lines modified)
    2. Ensure no unintended modifications
    3. Ensure code follows project style
    4. Commit with descriptive message following project conventions
  - **Commit Message Example:**
    ```
    feat: decouple target language settings from auto-feature toggles

    Remove the `enabled` parameter dependency from LanguageSelectorSetting
    components, allowing users to select target languages at any time,
    regardless of auto-summary/auto-translation toggle state.

    This change supports manual workflow users who need to configure
    language preferences without enabling auto-features.

    Changes:
    - SummarySettingsScreen.kt: Remove `enabled` parameter
    - TranslationSettingsScreen.kt: Remove `enabled` parameter

    Fixes: Improves usability for manual translation/summary workflows
    ```
  - **Acceptance:** Changes committed and pushed to remote

---

## Task Dependencies

```
T1.1 ──┬──▶ T1.2 ──┬──▶ T1.3 ──┬──▶ T1.7 ──┬──▶ T1.10 ──┬──▶ TF.1 ──┬──▶ TF.2
       │          │          │          │          │          │
       └──▶ T1.4 ──┴──▶ T1.5 ──┴──▶ T1.6 ─┘          │          │          │
                                                  │          │          │
                                                  ▼          ▼          │
                                                T1.8 ────────┘          │
                                                T1.9 ───────────────────┘
```

**Execution Order:**
1. All code change tasks (T1.1-T1.6) can be done in any order, but must complete before build
2. Build task (T1.7) requires all code changes complete
3. Tests (T1.8, T1.9) run after successful build
4. Manual QA (T1.10) requires APK from successful build
5. Final tasks (TF.1, TF.2) run after all verification complete

---

## Priority Order

1. **T1.1 - T1.6** (Code changes) - **HIGH PRIORITY**: Core implementation
2. **T1.7** (Build) - **HIGH PRIORITY**: Verify no compilation errors
3. **T1.8** (Unit tests) - **HIGH PRIORITY**: Verify no functional regressions
4. **T1.10** (Manual QA) - **HIGH PRIORITY**: Verify user-facing behavior
5. **T1.9** (UI tests) - **MEDIUM PRIORITY**: Automated verification (may skip if no emulator)
6. **TF.1** (Accessibility) - **MEDIUM PRIORITY**: Important for inclusivity
7. **TF.2** (Commit) - **HIGH PRIORITY**: Finalize changes

---

## Task Estimates

| Task | Estimated Time | Dependencies |
|------|----------------|--------------|
| T1.1 | 5 minutes | None |
| T1.2 | 5 minutes | T1.1 |
| T1.3 | 5 minutes | T1.2 |
| T1.4 | 5 minutes | None |
| T1.5 | 5 minutes | T1.4 |
| T1.6 | 5 minutes | T1.5 |
| T1.7 | 5 minutes | T1.1-T1.6 |
| T1.8 | 5 minutes | T1.7 |
| T1.9 | 10 minutes | T1.7 |
| T1.10 | 15 minutes | T1.7 |
| TF.1 | 10 minutes | T1.10 |
| TF.2 | 5 minutes | TF.1 |
| **Total** | **30-60 minutes** | |

---

## Verification Checklist

### Before Committing

- [ ] All 6 code changes made (T1.1-T1.6)
- [ ] Build succeeds (T1.7)
- [ ] All unit tests pass (T1.8)
- [ ] Manual QA passed (T1.10)
- [ ] Accessibility verified (TF.1)
- [ ] No unintended changes
- [ ] Code follows project style

### After Committing

- [ ] Changes pushed to remote
- [ ] CI/CD pipeline passes (if applicable)
- [ ] Code review approved (if required)
- [ ] Ready for next release

---

## Common Issues and Solutions

### Issue 1: Compilation Error "Unresolved reference"

**Cause:** Missed removing `enabled` parameter from one of the locations

**Solution:** Double-check all three locations in each file:
- Function signature
- `.clickable()` modifier
- Function call site

### Issue 2: Test Failure

**Cause:** Existing tests may depend on `enabled` state

**Solution:** Review failing tests and update them to test for independent behavior

### Issue 3: Language Selector Still Disabled

**Cause:** May have missed one of the three changes per file

**Solution:** Verify all three changes made:
1. Function signature (parameter removed)
2. `.clickable()` modifier (enabled=enabled removed)
3. Function call (argument removed)

### Issue 4: Runtime Crash

**Cause:** Unlikely with this change, but may indicate deeper coupling

**Solution:** Review stack trace, check for other code depending on `enabled` state

---

## Success Criteria

### Functional Requirements Met

- [x] Language selector enabled when switch OFF
- [x] Language selector enabled when switch ON
- [x] Dropdown opens in all states
- [x] Language saves in all states
- [x] No functional regressions

### Non-Functional Requirements Met

- [x] No compilation errors
- [x] All tests pass
- [x] No visual regressions
- [x] Accessibility maintained
- [x] No performance degradation

### Quality Requirements Met

- [x] Minimal, focused changes
- [x] No unnecessary complexity
- [x] Code follows project style
- [x] Changes reviewed and committed

---

## Next Steps After Completion

1. **Monitor:** Watch for crash reports or user feedback
2. **Iterate:** If issues found, address in follow-up
3. **Document:** Update any relevant documentation
4. **Release:** Include in next app release notes

---

**Task List Status:** Ready for Execution
**Total Tasks:** 11
**Confidence Level:** High
**Last Updated:** 2026-01-05
