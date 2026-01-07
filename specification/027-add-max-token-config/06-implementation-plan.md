# Implementation Plan: Add max_tokens Configuration

**Spec ID:** 027-add-max-token-config
**Date:** 2026-01-07
**Status:** READY
**Estimated Time:** 65 minutes

---

## Overview

This implementation plan adds `max_tokens` configuration to the AI provider edit/creation screen, allowing users to control the maximum number of tokens the model can output.

---

## Implementation Phases

### Phase 1: Data Model Updates (5 minutes)

**Objective:** Add max_tokens field to settings data classes

**Files:**
1. `app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`

**Tasks:**
1.1. Add `maxTokens: Int? = null` to `OpenAISettings` data class
1.2. Add `maxTokens: Int? = null` to `AnthropicSettings` data class
1.3. Update KDoc comments for both classes

**Acceptance Criteria:**
- [ ] Both settings classes have `maxTokens` field
- [ ] Field is nullable with default null
- [ ] Code compiles without errors
- [ ] Existing tests still pass

**Risk:** Low - Adding nullable field to data class

---

### Phase 2: ViewModel Logic (15 minutes)

**Objective:** Add updateMaxTokens method and validation logic

**Files:**
1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`

**Tasks:**
2.1. Add `updateMaxTokens(maxTokens: String)` method
2.2. Add `updateMaxTokensInSettings(tokens: Int?)` private method
2.3. Add `getMaxTokensLimit()` private method
2.4. Add `maxTokens` computed property to `ProviderEditUiState`
2.5. Add `maxTokensError` computed property to `ProviderEditUiState`

**Acceptance Criteria:**
- [ ] `updateMaxTokens()` updates settings correctly
- [ ] Validation works for empty, valid, and invalid input
- [ ] Provider-specific limits enforced
- [ ] UI state reflects max_tokens value
- [ ] Unit tests pass

**Risk:** Low - Following existing patterns

---

### Phase 3: UI Implementation (15 minutes)

**Objective:** Add max_tokens text field to provider edit form

**Files:**
1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
2. `app/src/main/res/values/strings.xml`
3. `app/src/main/res/values-zh-rCN/strings.xml`

**Tasks:**
3.1. Add `OutlinedTextField` for max_tokens in `ProviderEditForm`
3.2. Add `onMaxTokensChange` parameter to `ProviderEditForm`
3.3. Add English string resources (4 strings)
3.4. Add Chinese string resources (4 strings)
3.5. Update `isFormValid` check to include max_tokens validation

**Acceptance Criteria:**
- [ ] Max tokens field displays in form
- [ ] Field has correct label and placeholder
- [ ] Keyboard type is Number
- [ ] Validation errors display correctly
- [ ] All strings localized
- [ ] UI tests pass

**Risk:** Low - Standard Compose UI pattern

---

### Phase 4: Testing (30 minutes)

**Objective:** Ensure max_tokens feature works correctly

**Files:**
1. `app/src/test/.../ProviderEditViewModelTest.kt`
2. `app/src/androidTest/.../ProviderEditScreenTest.kt`

**Tasks:**
4.1. Write unit tests for `updateMaxTokens()`
4.2. Write unit tests for validation logic
4.3. Write UI tests for max_tokens field
4.4. Run all existing tests (regression check)
4.5. Perform manual testing on device/emulator

**Acceptance Criteria:**
- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] No regressions in existing functionality
- [ ] Manual testing checklist complete

**Risk:** Medium - Need comprehensive test coverage

---

## Task List

### Task 1: Update OpenAISettings Data Class
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`
**Estimate:** 2 minutes
**Priority:** HIGH

```kotlin
// Add after timeoutSeconds field
val maxTokens: Int? = null,  // Maximum tokens for model output
```

### Task 2: Update AnthropicSettings Data Class
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`
**Estimate:** 2 minutes
**Priority:** HIGH

```kotlin
// Add after timeoutSeconds field
val maxTokens: Int? = null,  // Maximum tokens for model output
```

### Task 3: Add updateMaxTokens Method
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
**Estimate:** 10 minutes
**Priority:** HIGH

Implement `updateMaxTokens(maxTokens: String)` with:
- Empty string → null
- Valid integer → Int
- Invalid → null (don't update)

### Task 4: Add UI State Properties
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
**Estimate:** 5 minutes
**Priority:** HIGH

Add to `ProviderEditUiState`:
- `maxTokens: String` (computed property)
- `maxTokensError: String?` (computed property)

### Task 5: Add MaxTokens TextField
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Estimate:** 10 minutes
**Priority:** HIGH

Add `OutlinedTextField` after Model field with:
- Label: "Max Tokens"
- Placeholder: "1-4096 (optional)"
- KeyboardType.Number
- Validation error display

### Task 6: Add English Strings
**File:** `app/src/main/res/values/strings.xml`
**Estimate:** 3 minutes
**Priority:** MEDIUM

Add 4 strings:
- max_tokens (label)
- max_tokens_placeholder
- max_tokens_hint
- max_tokens_error_* (3 variants)

### Task 7: Add Chinese Strings
**File:** `app/src/main/res/values-zh-rCN/strings.xml`
**Estimate:** 3 minutes
**Priority:** MEDIUM

Translate 4 English strings to Chinese

### Task 8: Write Unit Tests
**File:** `app/src/test/.../ProviderEditViewModelTest.kt`
**Estimate:** 15 minutes
**Priority:** MEDIUM

Test cases:
- Valid input
- Empty input
- Invalid text
- Out of range values

### Task 9: Write UI Tests
**File:** `app/src/androidTest/.../ProviderEditScreenTest.kt`
**Estimate:** 10 minutes
**Priority:** LOW

Test cases:
- Field displays
- Input works
- Validation displays

### Task 10: Manual Testing
**Estimate:** 5 minutes
**Priority:** MEDIUM

Checklist:
- Create provider with max_tokens
- Edit provider max_tokens
- Validation works
- API call includes max_tokens

---

## Dependencies

**Task Dependencies:**
- Task 3 depends on Task 1, 2 (data model must exist first)
- Task 4 depends on Task 3 (needs updateMaxTokens implementation)
- Task 5 depends on Task 4 (needs UI state properties)
- Task 6, 7 can run in parallel
- Task 8 depends on Task 3 (tests ViewModel logic)
- Task 9 depends on Task 5 (tests UI component)
- Task 10 depends on all previous tasks

**Critical Path:**
Task 1, 2 → Task 3 → Task 4 → Task 5 → Task 10

**Parallel Opportunities:**
- Task 6 + Task 7 (localization)
- Task 8 + Task 9 (testing, after implementation)

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking existing provider configs | Use nullable field with default null |
| Validation logic incorrect | Follow existing pattern, test thoroughly |
| UI overflow | Form is scrollable, test on small screen |
| Missing localization | Use both English and Chinese from start |
| API integration forgotten | Add to task list, verify with logging |

---

## Success Criteria

### Must Have (P0):
- [ ] Max tokens field added to UI
- [ ] Field validates input correctly
- [ ] Value persists with provider config
- [ ] Backward compatible (existing configs work)
- [ ] No regressions in existing functionality

### Should Have (P1):
- [ ] Complete localization (English + Chinese)
- [ ] Unit tests for ViewModel
- [ ] UI tests for field
- [ ] Manual testing complete

### Nice to Have (P2):
- [ ] Additional validation rules
- [ ] Tooltips or help text
- [ ] Advanced settings section

---

## Rollback Plan

If issues arise:
1. Revert changes to `AISettings.kt` (remove maxTokens field)
2. Revert changes to `ProviderEditViewModel.kt` (remove updateMaxTokens)
3. Revert changes to `ProviderEditScreen.kt` (remove field)
4. Revert string resource changes

**Note:** Since field is nullable with default null, existing configs are never broken. Rollback is safe.

---

## Completion Checklist

- [ ] All tasks completed
- [ ] All tests passing
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Ready for commit

---

**End of Implementation Plan**

**Next:** Execute Phase 8 (Execution & QA)
