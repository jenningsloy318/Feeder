# Implementation Plan: Provider Navigation Bug Fix

**Spec Index:** 009
**Title:** Fix Provider Navigation Back Stack Bug
**Type:** Bug Fix
**Date:** 2026-01-03

---

## Overview

This implementation plan details the steps to fix the provider navigation bug by changing the navigation method from `navigate()` to `popBackStack()` and ensuring the success snackbar displays before navigation.

**Total Changes:** 2 files, ~5 lines modified
**Estimated Time:** 30 minutes
**Risk Level:** Low

---

## Implementation Phases

### Phase 1: Fix NavigationDestinations.kt
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
**Lines:** 440-442

**Steps:**
1. Open NavigationDestinations.kt
2. Locate the `ProviderEditDestination.RegisterScreen` composable
3. Find the `ProviderEditScreen` call around line 440
4. Replace the `onNavigateUp` implementation

**Code Change:**
```kotlin
// BEFORE:
ProviderEditScreen(
    onNavigateUp = {
        ProviderListDestination.navigate(navController)
    },
    viewModel = viewModel,
)

// AFTER:
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
- Code compiles without errors
- Matches pattern in ProviderListDestination (lines 379-382)
- ktlint check passes

---

### Phase 2: Fix ProviderEditScreen.kt (Optional Enhancement)
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Lines:** 72-77

**Note:** The current implementation should work correctly with Phase 1's fix. The `showSnackbar()` suspend function will complete before `onNavigateUp()` is called, allowing the message to display. However, we can add explicit error handling.

**Code Change:**
```kotlin
// BEFORE:
LaunchedEffect(uiState.saveResult) {
    uiState.saveResult?.let { result ->
        if (result.isSuccess) {
            snackbarHostState.showSnackbar(message = successMessage)
            viewModel.clearSaveResult()
            onNavigateUp()
        }
    }
}

// AFTER:
LaunchedEffect(uiState.saveResult) {
    uiState.saveResult?.let { result ->
        if (result.isSuccess) {
            // Await snackbar result to allow message to display
            snackbarHostState.showSnackbar(message = successMessage)
            viewModel.clearSaveResult()
            // Navigate after snackbar is shown
            onNavigateUp()
        } else if (result.isFailure) {
            // Stay on screen for errors - don't navigate
            viewModel.clearSaveResult()
        }
    }
}
```

**Verification:**
- Code compiles without errors
- Success case navigates after snackbar
- Error case stays on screen

---

## Risk Assessment

### Low Risk Factors
- **Minimal Code Changes:** Only 5-10 lines modified
- **Established Pattern:** 6 out of 7 destinations already use this pattern
- **No API Changes:** Public interfaces unchanged
- **No Data Changes:** Database and business logic unaffected

### Mitigation Strategies
1. **Follow Existing Patterns:** Copy the exact pattern from ProviderListDestination
2. **Test Thoroughly:** Manual testing of all navigation flows
3. **Code Review:** Get review from team member familiar with navigation
4. **Gradual Rollout:** Use beta testing before full release

### Potential Issues

| Issue | Likelihood | Impact | Mitigation |
|-------|-----------|--------|------------|
| `popBackStack()` returns false (empty stack) | Low | Medium | Fallback to `navigate()` included |
| Snackbar timing still feels off | Low | Low | Can adjust snackbar duration if needed |
| Regression in other navigation | Very Low | High | Comprehensive testing of all settings screens |

---

## Testing Plan

### Pre-Implementation Testing
1. **Reproduce Bug:** Confirm current buggy behavior
2. **Create Test Cases:** Document expected behavior for each scenario
3. **Set Up Environment:** Ensure debug build is ready

### Post-Implementation Testing

#### Smoke Tests
- [ ] Build succeeds without errors
- [ ] App launches successfully
- [ ] Settings screen opens

#### Navigation Tests
- [ ] Settings → Provider List → Back → Settings
- [ ] Settings → Provider List → Edit → Back → Provider List
- [ ] Settings → Provider List → Edit → Save → Provider List → Back → Settings
- [ ] Edit multiple providers in sequence
- [ ] Back button from provider list returns to settings

#### Edge Cases
- [ ] Deep link to Provider Edit → Back
- [ ] Screen rotation during save
- [ ] Rapid back button presses
- [ ] Save with validation errors

---

## Rollout Plan

### Deployment Steps
1. **Create Feature Branch:** `spec-09-provider-navigation-bug` (already exists)
2. **Implement Changes:** Apply Phase 1 and Phase 2
3. **Test Locally:** Run manual tests
4. **Create Pull Request:** For code review
5. **Merge to Master:** After approval
6. **Tag Release:** Create version tag
7. **Deploy to Beta:** Internal testing
8. **Deploy to Production:** Public release

### Rollback Plan
If critical issues are found:
1. **Revert Commits:** Use `git revert` for both commits
2. **Hotfix Release:** Deploy reverted version
3. **Notify Users:** Communicate issue and workaround

---

## Build Verification

### Commands to Run
```bash
# Build debug APK
./gradlew assembleDebug

# Run ktlint check
./gradlew ktlintCheck

# Run unit tests
./gradlew test

# Run instrumented tests (if any)
./gradlew connectedAndroidTest
```

### Success Criteria
- Build completes without errors
- All ktlint checks pass
- All existing tests pass
- Manual navigation tests pass

---

## Code Review Checklist

### Before Submitting for Review
- [ ] Changes follow ktlint style guide
- [ ] No compiler warnings
- [ ] Comments added for non-obvious code
- [ ] Manual tests completed and documented
- [ ] Edge cases considered and tested

### Reviewer Checklist
- [ ] Navigation pattern matches other destinations
- [ ] Back stack behavior is correct
- [ ] No unintended side effects
- [ ] Error handling is appropriate
- [ ] Code is readable and maintainable

---

## Post-Implementation Tasks

### Documentation
1. **Update Navigation Pattern Guide:** If such a guide exists
2. **Add Code Comment:** Explain why `popBackStack()` is used
3. **Update CHANGELOG:** Document the bug fix

### Future Improvements
1. **Add Navigation Tests:** Create UI tests for back stack verification
2. **Audit All Navigation:** Review other destinations for consistency
3. **Consider Type-Safe Navigation:** Evaluate libraries like Compose Destinations

---

## Timeline

| Task | Duration | Owner |
|------|----------|-------|
| Phase 1 Implementation | 10 minutes | Developer |
| Phase 2 Implementation | 10 minutes | Developer |
| Local Testing | 15 minutes | Developer |
| Code Review | 30 minutes | Reviewer |
| Merge & Deploy | 15 minutes | Developer |
| **Total** | **~80 minutes** | |

---

## Success Metrics

### Navigation Behavior
- ✅ Back button from Provider List returns to Settings
- ✅ Multiple edits don't accumulate back stack
- ✅ Save operation shows full success message

### Performance
- ✅ Navigation completes within 300ms (after snackbar)
- ✅ No memory leaks from back stack accumulation
- ✅ Smooth transitions with no jank

### User Experience
- ✅ Navigation feels natural and predictable
- ✅ No "trapped" feeling in provider edit flow
- ✅ Clear feedback on save success

---

## References

- Technical Specification: `TECH_SPEC.md`
- Task List: `TASKS.md`
- Related Specs: 008-fix-provider-duplicate-name-bug, 005-fix-auto-summary
- Android Navigation Documentation: https://developer.android.com/guide/navigation
