# Task List: Fix Provider Duplicate Name Bug

**Feature ID**: 009
**Bug ID**: 009-DUPLICATE-PROVIDER-NAME
**Date**: 2026-01-03
**Status**: Ready for Execution

---

## Task Overview

**Total Tasks**: 10
**Estimated Time**: 3 hours
**Priority**: High
**Complexity**: Low-Medium

---

## Task Details

### Task 1: Create DuplicateProviderNameException Class

**ID**: T1
**Phase**: 1
**Priority**: High
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Create a custom exception class for duplicate provider names.

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Implementation**:
```kotlin
/**
 * Exception thrown when attempting to add/update a provider with a duplicate name.
 *
 * @property name The duplicate name
 * @property existingProvider The provider that already has this name (null if not found)
 */
class DuplicateProviderNameException(
    val name: String,
    val existingProvider: ProviderConfig?
) : IllegalArgumentException(
    "A provider named '$name' already exists"
)
```

**Acceptance Criteria**:
- [ ] Class compiles without errors
- [ ] Extends `IllegalArgumentException`
- [ ] Has `name` and `existingProvider` properties
- [ ] Has descriptive message
- [ ] Has KDoc documentation

**Dependencies**: None

**Testing**:
- [ ] Can instantiate exception
- [ ] Properties are accessible
- [ ] Message is correct

---

### Task 2: Implement isProviderNameDuplicate() Function

**ID**: T2
**Phase**: 1
**Priority**: High
**Estimated Time**: 30 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Implement validation function to check for duplicate provider names.

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Implementation**:
```kotlin
/**
 * Check if a provider name already exists.
 *
 * Comparison is case-insensitive and ignores leading/trailing whitespace.
 *
 * @param name The name to check
 * @param excludeId Optional provider ID to exclude from check (for edit scenarios)
 * @return true if a provider with this name already exists, false otherwise
 */
fun isProviderNameDuplicate(name: String, excludeId: String? = null): Boolean {
    val normalizedName = name.trim().lowercase()
    return _providers.value.any { provider ->
        provider.id != excludeId &&
        provider.name.trim().lowercase() == normalizedName
    }
}
```

**Acceptance Criteria**:
- [ ] Function compiles without errors
- [ ] Returns true for exact match
- [ ] Returns true for case-insensitive match
- [ ] Returns true for trimmed match
- [ ] Returns false when name is unique
- [ ] Excludes provider by ID when specified
- [ ] Handles empty list correctly
- [ ] Has KDoc documentation

**Dependencies**: T1 (may throw exception)

**Testing**:
- [ ] Test exact match
- [ ] Test case-insensitive match
- [ ] Test trimmed whitespace match
- [ ] Test excludeId parameter
- [ ] Test unique name returns false
- [ ] Test empty list returns false

---

### Task 3: Update addProvider() to Validate

**ID**: T3
**Phase**: 2
**Priority**: High
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Update `addProvider()` function to validate for duplicate names before adding.

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Implementation**:
```kotlin
fun addProvider(provider: ProviderConfig) {
    // Validate name is unique
    if (isProviderNameDuplicate(provider.name)) {
        val existing = _providers.value.find {
            it.name.trim().lowercase() == provider.name.trim().lowercase()
        }
        throw DuplicateProviderNameException(provider.name, existing)
    }

    val updated = _providers.value + provider
    saveProviders(updated)
}
```

**Acceptance Criteria**:
- [ ] Function compiles without errors
- [ ] Throws exception on duplicate name
- [ ] Exception includes existing provider
- [ ] Adds provider when name is unique
- [ ] Updated KDoc documentation

**Dependencies**: T2 (uses validation function)

**Testing**:
- [ ] Test duplicate name throws exception
- [ ] Test unique name succeeds
- [ ] Test exception has correct properties
- [ ] Verify provider not added when duplicate

---

### Task 4: Update updateProvider() to Validate

**ID**: T4
**Phase**: 2
**Priority**: High
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Update `updateProvider()` function to validate for duplicate names before updating.

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Implementation**:
```kotlin
fun updateProvider(provider: ProviderConfig) {
    // Validate name is unique (exclude self)
    if (isProviderNameDuplicate(provider.name, excludeId = provider.id)) {
        val existing = _providers.value.find {
            it.id != provider.id &&
            it.name.trim().lowercase() == provider.name.trim().lowercase()
        }
        throw DuplicateProviderNameException(provider.name, existing)
    }

    val updated = _providers.value.map {
        if (it.id == provider.id) provider else it
    }
    saveProviders(updated)
}
```

**Acceptance Criteria**:
- [ ] Function compiles without errors
- [ ] Throws exception on duplicate name (different provider)
- [ ] Allows update to same name (same provider)
- [ ] Exception includes existing provider
- [ ] Updated KDoc documentation

**Dependencies**: T2 (uses validation function)

**Testing**:
- [ ] Test duplicate name throws exception
- [ ] Test update to same name succeeds
- [ ] Test update to different name succeeds
- [ ] Test exception has correct properties

---

### Task 5: Update saveProvider() Error Handling

**ID**: T5
**Phase**: 3
**Priority**: High
**Estimated Time**: 30 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Update `saveProvider()` in ViewModel to catch and handle duplicate name exceptions.

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`

**Implementation**:
```kotlin
viewModelScope.launch {
    try {
        if (_internalState.value.isNew) {
            repository.settingsStore.addProvider(current)
        } else {
            repository.settingsStore.updateProvider(current)
        }

        if (current.isActive) {
            repository.activateProvider(current.id)
        }

        _internalState.value = _internalState.value.copy(
            isSaving = false,
            saveResult = Result.success(Unit)
        )
    } catch (e: SettingsStore.DuplicateProviderNameException) {
        // Handle duplicate name with user-friendly message
        _internalState.value = _internalState.value.copy(
            isSaving = false,
            saveResult = Result.failure(
                UserException(
                    "A provider with this name already exists"
                )
            )
        )
    } catch (e: Exception) {
        // Handle other errors
        _internalState.value = _internalState.value.copy(
            isSaving = false,
            saveResult = Result.failure(e)
        )
    }
}
```

**Acceptance Criteria**:
- [ ] Code compiles without errors
- [ ] Catches `DuplicateProviderNameException`
- [ ] Converts to user-friendly error
- [ ] Other exceptions still handled
- [ ] Error appears in saveResult

**Dependencies**: T3, T4 (needs updated SettingsStore)

**Testing**:
- [ ] Test duplicate name shows error
- [ ] Test success on unique name
- [ ] Test other exceptions still handled

---

### Task 6: Write Unit Tests for SettingsStore

**ID**: T6
**Phase**: 4
**Priority**: High
**Estimated Time**: 30 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Write comprehensive unit tests for SettingsStore validation functions.

**File**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`

**Test Cases**:
1. `isProviderNameDuplicate returns true for exact match`
2. `isProviderNameDuplicate returns true for case-insensitive match`
3. `isProviderNameDuplicate returns true for trimmed match`
4. `isProviderNameDuplicate excludes current provider when editing`
5. `isProviderNameDuplicate returns false for unique name`
6. `isProviderNameDuplicate returns false for empty list`
7. `addProvider throws exception for duplicate name`
8. `addProvider succeeds for unique name`
9. `updateProvider throws exception for duplicate name`
10. `updateProvider allows renaming to same name`

**Acceptance Criteria**:
- [ ] All test cases implemented
- [ ] All tests pass
- [ ] Code coverage > 90%

**Dependencies**: T2, T3, T4

---

### Task 7: Write Unit Tests for ViewModel

**ID**: T7
**Phase**: 4
**Priority**: High
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Write unit tests for ViewModel error handling.

**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModelTest.kt`

**Test Cases**:
1. `saveProvider returns error for duplicate name`
2. `saveProvider succeeds for unique name`

**Acceptance Criteria**:
- [ ] All test cases implemented
- [ ] All tests pass
- [ ] Error handling verified

**Dependencies**: T5

---

### Task 8: Write Integration Tests

**ID**: T8
**Phase**: 4
**Priority**: Medium
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Write integration tests for end-to-end provider creation with validation.

**File**: `app/src/androidTest/java/com/nononsenseapps/feeder/archmodel/ProviderValidationIntegrationTest.kt`

**Test Cases**:
1. `end-to-end provider creation prevents duplicates`
2. `end-to-end provider update prevents duplicates`

**Acceptance Criteria**:
- [ ] All test cases implemented
- [ ] All tests pass
- [ ] Full flow verified

**Dependencies**: T5

---

### Task 9: Build and Test APK

**ID**: T9
**Phase**: 5
**Priority**: Medium
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Build debug APK and verify it compiles successfully.

**Steps**:
1. Run `./gradlew assembleDebug`
2. Verify build succeeds
3. Check for warnings
4. Install on device/emulator (optional)

**Acceptance Criteria**:
- [ ] Build succeeds without errors
- [ ] No critical warnings
- [ ] APK can be installed

**Dependencies**: T6, T7, T8 (all tests must pass)

---

### Task 10: Manual Testing and Verification

**ID**: T10
**Phase**: 5
**Priority**: Medium
**Estimated Time**: 15 minutes
**Status**: Pending
**Assignee**: TBD

**Description**:
Perform manual testing of the duplicate name validation.

**Test Cases**:

| Case | Steps | Expected Result |
|------|-------|-----------------|
| Create duplicate | 1. Create "Provider A"<br>2. Create "provider a" | Error: "A provider with this name already exists" |
| Edit to duplicate | 1. Create "Provider A" and "Provider B"<br>2. Edit "Provider A" → "Provider B" | Error: "A provider with this name already exists" |
| Edit to same name | 1. Create "Provider A"<br>2. Edit "Provider A" (no name change) | Save succeeds |
| Whitespace duplicate | 1. Create "Provider A"<br>2. Create "  Provider A  " | Error: "A provider with this name already exists" |
| Unique name | 1. Create "Provider A"<br>2. Create "Provider B" | Both save successfully |

**Acceptance Criteria**:
- [ ] All manual test cases pass
- [ ] No crashes or ANRs
- [ ] Error messages display correctly
- [ ] UX is smooth and intuitive

**Dependencies**: T9

---

## Task Dependencies Graph

```
T1 (Exception)
  ↓
T2 (Validation) → T3 (addProvider) ─┐
                → T4 (updateProvider) ─┼→ T5 (ViewModel) → T6 (Tests)
                                     ↓                ↓
                                   T7 (Tests) ←──────┘
                                     ↓
                                   T8 (Integration)
                                     ↓
                                   T9 (Build)
                                     ↓
                                   T10 (Manual Testing)
```

---

## Progress Tracking

### Overall Progress

**Completed**: 0/10 tasks (0%)
**In Progress**: 0 tasks
**Pending**: 10 tasks
**Blocked**: 0 tasks

### Phase Progress

| Phase | Tasks | Completed | Progress |
|-------|-------|-----------|----------|
| Phase 1: Validation Infrastructure | 2 | 0 | 0% |
| Phase 2: Provider Operations | 2 | 0 | 0% |
| Phase 3: Error Handling | 1 | 0 | 0% |
| Phase 4: Testing | 3 | 0 | 0% |
| Phase 5: Manual Testing | 2 | 0 | 0% |

---

## Definition of Done

A task is considered **Done** when:

- [ ] Code is implemented
- [ ] Code compiles without errors
- [ ] Code follows project standards
- [ ] Unit tests are written (if applicable)
- [ ] Unit tests pass
- [ ] Code is reviewed (if applicable)
- [ ] Documentation is updated

The entire feature is **Done** when:

- [ ] All 10 tasks are complete
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual testing is successful
- [ ] APK builds successfully
- [ ] No critical bugs
- [ ] Documentation is complete

---

## Notes

### Estimated Completion Time

- **Optimistic**: 2 hours (if all goes smoothly)
- **Realistic**: 3 hours (includes testing and fixes)
- **Pessimistic**: 4 hours (includes unexpected issues)

### Critical Path

T1 → T2 → T3/T4 → T5 → T6/T7/T8 → T9 → T10

**Minimum Time**: 3 hours (sequential execution)
**Optimized Time**: 2.5 hours (T3 and T4 can be done in parallel, T6-T8 can be done in parallel)

### Blockers

- None currently identified

---

## References

- Implementation Plan: [./06-implementation-plan.md](./06-implementation-plan.md)
- Technical Specification: [./05-specification.md](./05-specification.md)
- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)

---

**Status**: ✅ Task List Complete
**Ready for Execution**: Yes
**Next Phase**: Phase 7 - Specification Review
