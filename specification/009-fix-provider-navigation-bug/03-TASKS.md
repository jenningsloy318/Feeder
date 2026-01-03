# Task List: Provider Navigation Bug Fix

**Spec Index:** 009
**Title:** Fix Provider Navigation Back Stack Bug
**Date:** 2026-01-03

---

## Task Overview

This document provides a detailed task checklist for fixing the provider navigation bug. Tasks are organized by milestone with dependencies and verification steps.

**Total Tasks:** 8
**Estimated Time:** 60-90 minutes

---

## Milestone 1: Code Implementation

### Task 1.1: Fix NavigationDestinations.kt
**Priority:** Critical
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
**Lines:** 440-442

**Description:**
Replace `navigate()` with `popBackStack()` in the `ProviderEditDestination.RegisterScreen` composable to properly manage the back stack.

**Steps:**
1. Open `NavigationDestinations.kt`
2. Navigate to line 440 (in `ProviderEditDestination`)
3. Replace the `onNavigateUp` callback

**Code Change:**
```kotlin
// Replace this:
ProviderEditScreen(
    onNavigateUp = {
        ProviderListDestination.navigate(navController)
    },
    viewModel = viewModel,
)

// With this:
ProviderEditScreen(
    onNavigateUp = {
        if (!navController.popBackStack()) {
            ProviderListDestination.navigate(navController)
        }
    },
    viewModel = viewModel,
)
```

**Verification:**
- [ ] Code compiles without errors
- [ ] Pattern matches `ProviderListDestination` (lines 379-382)
- [ ] ktlint check passes

**Dependencies:** None
**Estimated Time:** 5 minutes

---

### Task 1.2: Add Explanatory Comment
**Priority:** Medium
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
**Lines:** 440-444

**Description:**
Add a comment explaining why `popBackStack()` is used instead of `navigate()` to prevent future regressions.

**Steps:**
1. Add comment above the `onNavigateUp` callback
2. Explain the back stack management
3. Reference the pattern used by other destinations

**Code Addition:**
```kotlin
ProviderEditScreen(
    onNavigateUp = {
        // Use popBackStack() to properly manage back stack and return to
        // Provider List without adding duplicate destinations.
        // This matches the pattern used by ProviderListDestination and
        // other settings screens.
        if (!navController.popBackStack()) {
            ProviderListDestination.navigate(navController)
        }
    },
    viewModel = viewModel,
)
```

**Verification:**
- [ ] Comment is clear and concise
- [ ] ktlint check passes

**Dependencies:** Task 1.1
**Estimated Time:** 2 minutes

---

### Task 1.3: Enhance Error Handling in ProviderEditScreen.kt (Optional)
**Priority:** Low
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Lines:** 72-77

**Description:**
Add explicit error handling for save failures to ensure we stay on the edit screen when errors occur.

**Steps:**
1. Open `ProviderEditScreen.kt`
2. Navigate to the `LaunchedEffect` for `saveResult`
3. Add `else if` branch for error case

**Code Change:**
```kotlin
LaunchedEffect(uiState.saveResult) {
    uiState.saveResult?.let { result ->
        if (result.isSuccess) {
            snackbarHostState.showSnackbar(message = successMessage)
            viewModel.clearSaveResult()
            onNavigateUp()
        } else if (result.isFailure) {
            // Stay on screen for errors - don't navigate
            viewModel.clearSaveResult()
        }
    }
}
```

**Verification:**
- [ ] Code compiles without errors
- [ ] Error case stays on screen
- [ ] ktlint check passes

**Dependencies:** None
**Estimated Time:** 5 minutes

---

## Milestone 2: Build and Test

### Task 2.1: Build Debug APK
**Priority:** Critical

**Description:**
Build the debug APK to ensure code compiles successfully.

**Command:**
```bash
./gradlew assembleDebug
```

**Expected Output:**
- Build completes successfully
- APK generated at `app/build/outputs/apk/debug/app-debug.apk`
- No compilation errors

**Verification:**
- [ ] Build succeeds
- [ ] No compiler warnings
- [ ] APK file exists

**Dependencies:** Tasks 1.1, 1.2
**Estimated Time:** 5 minutes

---

### Task 2.2: Run ktlint Check
**Priority:** High

**Description:**
Run ktlint to ensure code follows project style guidelines.

**Command:**
```bash
./gradlew ktlintCheck
```

**Expected Output:**
- ktlint check passes
- No style violations

**If Fails:**
```bash
./gradlew ktlintFormat
```

**Verification:**
- [ ] ktlint check passes
- [ ] No violations reported

**Dependencies:** Task 2.1
**Estimated Time:** 2 minutes

---

### Task 2.3: Manual Testing - Save Flow
**Priority:** Critical

**Description:**
Manually test the save and back navigation flow to verify the fix works correctly.

**Steps:**
1. Install and launch the app
2. Navigate to Settings > AI Providers
3. Tap on an existing provider
4. Make a change (e.g., modify the name)
5. Tap "Save"
6. Observe the success snackbar displays fully
7. Tap the system Back button
8. Verify return to Settings menu

**Expected Behavior:**
- Success snackbar displays for 2-4 seconds
- Screen transitions to Provider List after snackbar
- Back button returns to Settings (NOT edit page)

**Verification:**
- [ ] Success snackbar displays fully
- [ ] Back from Provider List goes to Settings
- [ ] No duplicate screens in back stack

**Dependencies:** Task 2.1
**Estimated Time:** 5 minutes

---

### Task 2.4: Manual Testing - Multiple Edits
**Priority:** High

**Description:**
Test editing multiple providers to ensure back stack doesn't accumulate.

**Steps:**
1. Navigate to Settings > AI Providers
2. Edit Provider A and save
3. Edit Provider B and save
4. Edit Provider C and save
5. Tap Back button
6. Verify return to Settings
7. Tap Back button again
8. Verify exit Settings or go to previous screen

**Expected Behavior:**
- Each save returns to Provider List
- Single back press returns to Settings
- No accumulation of edit screens in back stack

**Verification:**
- [ ] Multiple edits work correctly
- [ ] Back stack remains clean
- [ ] Can exit Settings normally

**Dependencies:** Task 2.3
**Estimated Time:** 5 minutes

---

### Task 2.5: Manual Testing - Edge Cases
**Priority:** Medium

**Description:**
Test edge cases to ensure robust behavior.

**Test Cases:**

**Case 1: Rapid Back Presses**
1. Edit a provider
2. Save
3. Rapidly press Back multiple times
4. Expected: No crashes, clean navigation

**Case 2: Deep Link to Edit**
1. Open deep link directly to Provider Edit (if available)
2. Save
3. Press Back
4. Expected: Proper navigation behavior

**Case 3: Screen Rotation**
1. Edit a provider
2. Rotate screen during save
3. Expected: Save completes, navigation works

**Case 4: Save Failure**
1. Edit a provider with invalid data
2. Tap Save
3. Expected: Error shown, stays on edit screen

**Verification:**
- [ ] Rapid back presses handled
- [ ] Deep link navigation works
- [ ] Rotation doesn't break navigation
- [ ] Save failure keeps user on screen

**Dependencies:** Task 2.4
**Estimated Time:** 10 minutes

---

## Milestone 3: Documentation and Cleanup

### Task 3.1: Update CHANGELOG
**Priority:** Medium
**File:** `CHANGELOG.md`

**Description:**
Add entry to CHANGELOG.md documenting the bug fix.

**Format:**
```markdown
### [Version] - 2026-01-03

### Fixed
- Fixed provider navigation back button returning to edit page instead of Settings
- Fixed provider save success message being cut short by navigation
```

**Verification:**
- [ ] CHANGELOG updated
- [ ] Entry follows project format
- [ ] Date and version correct

**Dependencies:** Task 2.5
**Estimated Time:** 2 minutes

---

### Task 3.2: Clean Up Temporary Files
**Priority:** Low

**Description:**
Remove any temporary files or debug code created during development.

**Steps:**
1. Check for any TODO comments added
2. Remove debug log statements
3. Clean up any test files

**Verification:**
- [ ] No TODO comments in committed code
- [ ] No debug logs in committed code
- [ ] Working directory clean

**Dependencies:** Task 3.1
**Estimated Time:** 2 minutes

---

## Task Dependency Diagram

```
Task 1.1 (Fix NavigationDestinations.kt)
    ↓
Task 1.2 (Add Comment) ─────┐
    ↓                       │
Task 1.3 (Enhance Error)    │
                            │
Task 2.1 (Build APK) ←──────┘
    ↓
Task 2.2 (ktlint Check)
    ↓
Task 2.3 (Manual Testing - Save)
    ↓
Task 2.4 (Manual Testing - Multiple)
    ↓
Task 2.5 (Manual Testing - Edge Cases)
    ↓
Task 3.1 (Update CHANGELOG)
    ↓
Task 3.2 (Cleanup)
```

---

## Progress Tracking

| Task | Status | Notes |
|------|--------|-------|
| 1.1 Fix NavigationDestinations.kt | ⬜ Not Started | |
| 1.2 Add Explanatory Comment | ⬜ Not Started | |
| 1.3 Enhance Error Handling | ⬜ Not Started | Optional |
| 2.1 Build Debug APK | ⬜ Not Started | |
| 2.2 Run ktlint Check | ⬜ Not Started | |
| 2.3 Manual Testing - Save Flow | ⬜ Not Started | |
| 2.4 Manual Testing - Multiple | ⬜ Not Started | |
| 2.5 Manual Testing - Edge Cases | ⬜ Not Started | |
| 3.1 Update CHANGELOG | ⬜ Not Started | |
| 3.2 Clean Up Temporary Files | ⬜ Not Started | |

**Legend:**
- ⬜ Not Started
- ⏳ In Progress
- ✅ Complete
- ❌ Failed
- ⏭️ Skipped

---

## Notes

### Common Issues and Solutions

**Issue:** ktlint check fails
**Solution:** Run `./gradlew ktlintFormat` to auto-fix

**Issue:** Build fails after changes
**Solution:** Check for syntax errors in modified lines

**Issue:** Navigation still not working after fix
**Solution:** Verify app cache cleared, reinstall APK

### Testing Devices
Recommended testing on:
- Pixel 6 (API 33) - Reference device
- Samsung Galaxy S21 (API 33) - Popular device
- Minimum SDK device (API 29) - Compatibility check

### Accessibility Testing
- Verify back button works with TalkBack enabled
- Verify navigation changes are announced to screen readers
