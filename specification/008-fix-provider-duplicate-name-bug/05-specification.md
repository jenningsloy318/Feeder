# Technical Specification: Fix Provider Duplicate Name Bug

**Feature ID**: 009
**Bug ID**: 009-DUPLICATE-PROVIDER-NAME
**Status**: Specification Complete
**Date**: 2026-01-03
**Related**: [./03-debug-analysis.md](./03-debug-analysis.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Technical Design](#technical-design)
3. [Implementation Details](#implementation-details)
4. [API Changes](#api-changes)
5. [Error Handling](#error-handling)
6. [Testing Strategy](#testing-strategy)

---

## Overview

### Summary

Add validation to prevent creation of AI providers with duplicate names. The validation will be case-insensitive, trim whitespace, and exclude the current provider during edits.

### Goals

1. ✅ Prevent duplicate provider names (case-insensitive)
2. ✅ Provide clear error messages to users
3. ✅ Handle edit scenarios correctly (self-exclusion)
4. ✅ Maintain backward compatibility

### Non-Goals

- Real-time validation in UI (future enhancement)
- Migration of existing duplicate providers (out of scope)
- Provider name uniqueness enforcement across different types (out of scope)

---

## Technical Design

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (ProviderEditScreen)                               │
│ - Displays error messages from saveResult                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ ViewModel (ProviderEditViewModel)                           │
│ - Catches DuplicateProviderNameException                   │
│ - Sets saveResult with user-friendly error                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Repository (delegation layer)                               │
│ - No changes needed (simple delegation)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ SettingsStore (data layer)                                  │
│ - NEW: isProviderNameDuplicate()                           │
│ - NEW: DuplicateProviderNameException                      │
│ - MODIFIED: addProvider() - validates before add           │
│ - MODIFIED: updateProvider() - validates before update     │
└─────────────────────────────────────────────────────────────┘
```

### Validation Logic

#### Normalization Rules

1. **Trim Whitespace**: `"  My Provider  "` → `"My Provider"`
2. **Case-Insensitive**: `"My Provider"` == `"my provider"` == `"MY PROVIDER"`
3. **Empty Check**: Empty names fail existing validation (unchanged)

#### Duplicate Check Algorithm

```kotlin
fun isProviderNameDuplicate(name: String, excludeId: String? = null): Boolean {
    val normalizedName = name.trim().lowercase()
    return _providers.value.any { provider ->
        provider.id != excludeId &&
        provider.name.trim().lowercase() == normalizedName
    }
}
```

**Time Complexity**: O(n) where n = number of providers
**Space Complexity**: O(1) - no additional storage

---

## Implementation Details

### Component 1: SettingsStore.kt

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

#### Change 1.1: Add Custom Exception

**Location**: Inside SettingsStore class (companion object or top-level)

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

**Rationale**:
- Custom exception allows specific error handling
- Includes context (existing provider) for better error messages
- Extends IllegalArgumentException (standard Kotlin practice)

#### Change 1.2: Add Validation Function

**Location**: Inside SettingsStore class

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

**Rationale**:
- Separate read-only function enables real-time validation
- Public function can be used by UI for early validation
- Self-exclusion parameter supports edit scenario

#### Change 1.3: Update addProvider()

**Location**: SettingsStore.kt, line 641-644

**Before**:
```kotlin
fun addProvider(provider: ProviderConfig) {
    val updated = _providers.value + provider
    saveProviders(updated)
}
```

**After**:
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

**Rationale**:
- Check for duplicates before modifying state
- Include existing provider in exception for context
- Fail fast before expensive operations

#### Change 1.4: Update updateProvider()

**Location**: SettingsStore.kt, line 646-651

**Before**:
```kotlin
fun updateProvider(provider: ProviderConfig) {
    val updated = _providers.value.map {
        if (it.id == provider.id) provider else it
    }
    saveProviders(updated)
}
```

**After**:
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

**Rationale**:
- Exclude current provider from duplicate check
- Allows renaming to same name (no-op)
- Prevents renaming to conflict with another provider

---

### Component 2: ProviderEditViewModel.kt

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`

#### Change 2.1: Update saveProvider() Error Handling

**Location**: ProviderEditViewModel.kt, line 258-287

**Before**:
```kotlin
viewModelScope.launch {
    try {
        if (_internalState.value.isNew) {
            repository.addProvider(current)
        } else {
            repository.updateProvider(current)
        }

        if (current.isActive) {
            repository.activateProvider(current.id)
        }

        _internalState.value = _internalState.value.copy(
            isSaving = false,
            saveResult = Result.success(Unit)
        )
    } catch (e: Exception) {
        _internalState.value = _internalState.value.copy(
            isSaving = false,
            saveResult = Result.failure(e)
        )
    }
}
```

**After**:
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

**Rationale**:
- Catch specific exception for duplicate names
- Convert to user-friendly error message
- Preserve generic exception handling for other errors
- Note: Accessing settingsStore directly to avoid Repository changes

---

## API Changes

### Public API Changes

#### SettingsStore

**New Functions**:
```kotlin
fun isProviderNameDuplicate(name: String, excludeId: String? = null): Boolean
```

**New Exception**:
```kotlin
class DuplicateProviderNameException(name: String, existingProvider: ProviderConfig?)
```

**Modified Functions**:
```kotlin
fun addProvider(provider: ProviderConfig)  // Now throws DuplicateProviderNameException
fun updateProvider(provider: ProviderConfig)  // Now throws DuplicateProviderNameException
```

#### Repository

**No Changes** - Repository remains a simple delegation layer

#### ViewModel

**No Public API Changes** - Internal error handling enhanced only

### Backward Compatibility

| Component | Breaking Changes | Migration Required |
|-----------|------------------|-------------------|
| SettingsStore | No (new exception is subtype of IllegalArgumentException) | No |
| Repository | No | No |
| ViewModel | No | No |
| UI | No | No |

---

## Error Handling

### Error Flow

```
User saves provider with duplicate name
    ↓
SettingsStore.addProvider() / updateProvider()
    ↓
isProviderNameDuplicate() returns true
    ↓
throw DuplicateProviderNameException(name, existingProvider)
    ↓
ViewModel catches DuplicateProviderNameException
    ↓
Converts to user-friendly error message
    ↓
Sets saveResult = Result.failure(UserException(...))
    ↓
UI displays error in Snackbar
```

### Error Messages

| Scenario | Error Message | Display Location |
|----------|---------------|------------------|
| Duplicate on add | "A provider with this name already exists" | Snackbar + TextField error |
| Duplicate on edit | "A provider with this name already exists" | Snackbar + TextField error |
| Other errors | Existing error messages | Snackbar |

### User Experience

1. **Error Display**:
   - Snackbar shows error message
   - Provider name field shows error state
   - Save button remains enabled (can retry after changing name)

2. **Recovery**:
   - User changes provider name
   - Error clears automatically
   - User can save again

---

## Testing Strategy

### Unit Tests

#### Test Suite 1: SettingsStore Validation

**File**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`

```kotlin
class SettingsStoreTest {

    @Test
    fun `isProviderNameDuplicate returns true for exact match`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "My Provider"))

        assertTrue(store.isProviderNameDuplicate("My Provider"))
    }

    @Test
    fun `isProviderNameDuplicate returns true for case-insensitive match`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "My Provider"))

        assertTrue(store.isProviderNameDuplicate("MY PROVIDER"))
        assertTrue(store.isProviderNameDuplicate("my provider"))
        assertTrue(store.isProviderNameDuplicate("My PrOvIdEr"))
    }

    @Test
    fun `isProviderNameDuplicate returns true for trimmed match`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "My Provider"))

        assertTrue(store.isProviderNameDuplicate("  My Provider  "))
        assertTrue(store.isProviderNameDuplicate("My Provider"))
        assertTrue(store.isProviderNameDuplicate("  My Provider"))
    }

    @Test
    fun `isProviderNameDuplicate excludes current provider when editing`() {
        val store = createTestStore()
        val provider = testProvider(name = "My Provider", id = "provider-1")
        store.addProvider(provider)

        assertFalse(store.isProviderNameDuplicate("My Provider", excludeId = "provider-1"))
    }

    @Test
    fun `isProviderNameDuplicate returns false for unique name`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "Provider A"))

        assertFalse(store.isProviderNameDuplicate("Provider B"))
    }

    @Test
    fun `isProviderNameDuplicate returns false for empty list`() {
        val store = createTestStore()

        assertFalse(store.isProviderNameDuplicate("Any Name"))
    }

    @Test
    fun `addProvider throws exception for duplicate name`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "My Provider"))

        val exception = assertThrows<DuplicateProviderNameException> {
            store.addProvider(testProvider(name = "my provider"))
        }

        assertEquals("My Provider", exception.name)
        assertNotNull(exception.existingProvider)
        assertEquals("My Provider", exception.existingProvider?.name)
    }

    @Test
    fun `updateProvider throws exception for duplicate name`() {
        val store = createTestStore()
        store.addProvider(testProvider(name = "Provider A", id = "provider-1"))
        store.addProvider(testProvider(name = "Provider B", id = "provider-2"))

        val exception = assertThrows<DuplicateProviderNameException> {
            store.updateProvider(testProvider(name = "provider b", id = "provider-1"))
        }

        assertEquals("provider b", exception.name)
        assertEquals("provider-2", exception.existingProvider?.id)
    }

    @Test
    fun `updateProvider allows renaming to same name`() {
        val store = createTestStore()
        val provider = testProvider(name = "My Provider", id = "provider-1")
        store.addProvider(provider)

        // Should not throw - same name, same provider
        store.updateProvider(provider.copy(modelId = "new-model"))

        assertEquals("new-model", store.providers.value.find { it.id == "provider-1" }?.modelId)
    }

    @Test
    fun `addProvider succeeds for unique name`() {
        val store = createTestStore()

        store.addProvider(testProvider(name = "Provider A"))
        store.addProvider(testProvider(name = "Provider B"))

        assertEquals(2, store.providers.value.size)
    }
}
```

#### Test Suite 2: ViewModel Error Handling

**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModelTest.kt`

```kotlin
class ProviderEditViewModelTest {

    @Test
    fun `saveProvider returns error for duplicate name`() = runTest {
        val viewModel = createTestViewModel()
        val existingProvider = testProvider(name = "My Provider", id = "provider-1")
        repository.addProvider(existingProvider)

        val newProvider = testProvider(name = "my provider")  // Duplicate (case-insensitive)

        // Set up ViewModel state
        viewModel.updateProvider(newProvider)

        // Attempt save
        viewModel.saveProvider()

        // Verify error result
        val result = viewModel.uiState.value.saveResult
        assertNotNull(result)
        assertTrue(result?.isFailure ?: false)
        assertEquals("A provider with this name already exists", result?.exceptionOrNull()?.message)
    }

    @Test
    fun `saveProvider succeeds for unique name`() = runTest {
        val viewModel = createTestViewModel()
        val provider = testProvider(name = "Unique Provider")

        viewModel.updateProvider(provider)
        viewModel.saveProvider()

        val result = viewModel.uiState.value.saveResult
        assertNotNull(result)
        assertTrue(result?.isSuccess ?: false)
    }
}
```

### Integration Tests

**File**: `app/src/androidTest/java/com/nononsenseapps/feeder/archmodel/ProviderValidationIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ProviderValidationIntegrationTest {

    @Test
    fun `end-to-end provider creation prevents duplicates`() {
        // Create provider
        val provider1 = testProvider(name = "Test Provider")
        settingsStore.addProvider(provider1)

        // Attempt to create duplicate
        val provider2 = testProvider(name = "test provider")

        assertThrows<DuplicateProviderNameException> {
            settingsStore.addProvider(provider2)
        }

        // Verify only one provider exists
        assertEquals(1, settingsStore.providers.value.size)
    }
}
```

### Manual Testing

| Test Case | Steps | Expected Result |
|-----------|-------|-----------------|
| Create duplicate | 1. Create "Provider A"<br>2. Create "provider a" | Error: "A provider with this name already exists" |
| Edit to duplicate | 1. Create "Provider A" and "Provider B"<br>2. Edit "Provider A" → "Provider B" | Error: "A provider with this name already exists" |
| Edit to same name | 1. Create "Provider A"<br>2. Edit "Provider A" (no name change) | Save succeeds |
| Whitespace duplicate | 1. Create "Provider A"<br>2. Create "  Provider A  " | Error: "A provider with this name already exists" |
| Unique name | 1. Create "Provider A"<br>2. Create "Provider B" | Both save successfully |

---

## Edge Cases

### EC-1: Empty Provider List

**Scenario**: First provider being created
**Expected**: No duplicate validation errors (list is empty)
**Handled**: ✅ `isProviderNameDuplicate()` returns false for empty list

### EC-2: Special Characters

**Scenario**: Names with special characters
**Expected**: Special characters allowed, duplicates still prevented
**Handled**: ✅ Validation works with any characters

### EC-3: Unicode Characters

**Scenario**: Names with Unicode (emoji, non-Latin scripts)
**Expected**: Case-insensitive comparison works with Unicode
**Handled**: ✅ Kotlin's `lowercase()` handles Unicode correctly

### EC-4: Very Long Names

**Scenario**: Names with 100+ characters
**Expected**: Validation still works
**Handled**: ✅ String operations work on any length

### EC-5: Rapid Changes

**Scenario**: User rapidly types and backspaces names
**Expected**: Validation updates correctly
**Handled**: ✅ No state mutations during check (read-only function)

---

## Performance Considerations

### Time Complexity Analysis

| Operation | Complexity | Performance (n=100) |
|-----------|------------|---------------------|
| `isProviderNameDuplicate()` | O(n) | ~50 microseconds |
| `addProvider()` (with validation) | O(n) | ~50 microseconds |
| `updateProvider()` (with validation) | O(n) | ~50 microseconds |

**Conclusion**: Performance impact is negligible for typical use cases (n < 100)

### Memory Impact

- **Additional Storage**: O(1) - no new data structures
- **Temporary Objects**: O(1) - only string trimming
- **Total Impact**: Negligible

---

## Security Considerations

### Input Validation

| Aspect | Risk | Mitigation |
|--------|------|------------|
| SQL Injection | N/A | Not using SQL |
| Code Injection | Low | Data only used for display/comparison |
| DoS (very long names) | Very Low | String operations are safe |

### Data Integrity

- ✅ Validation prevents duplicate state
- ✅ No data loss (exception before modification)
- ✅ Existing data not affected

---

## Internationalization (i18n)

### Error Messages

**Current**: English only
**Future**: Add to strings.xml for localization

```xml
<!-- strings.xml -->
<string name="error_duplicate_provider_name">A provider with this name already exists</string>
```

### Case-Insensitive Comparison

**Current**: Uses `lowercase()` which works for most languages
**Edge Case**: Some languages have complex case rules
**Mitigation**: For this feature, `lowercase()` is sufficient

---

## Accessibility

### Error Announcement

**Current**: Snackbar + inline error
**Screen Reader**: Both announcements are accessible
**Future**: Consider aria-live regions for real-time validation

---

## Rollout Plan

### Phase 1: Development (Current)

- [ ] Implement validation in SettingsStore
- [ ] Update ViewModel error handling
- [ ] Write unit tests
- [ ] Write integration tests

### Phase 2: Testing

- [ ] Manual testing on device
- [ ] Automated test suite runs
- [ ] Edge case verification

### Phase 3: Deployment

- [ ] Code review
- [ ] Merge to main branch
- [ **Release in next app version**

### Phase 4: Monitoring

- [ ] Monitor crash reports for exceptions
- [ ] Gather user feedback
- [ ] Track duplicate name errors

---

## References

- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Code Assessment: [./04-code-assessment.md](./04-code-assessment.md)
- Requirements: [./01-requirement.md](./01-requirement.md)
- Research Report: [./02-research-report.md](./02-research-report.md)
- Implementation Plan: [./06-implementation-plan.md](./06-implementation-plan.md)
- Task List: [./07-task-list.md](./07-task-list.md)

---

## Implementation Status

**Status**: ✅ Specification Complete
**Next Step**: Implementation Plan (Phase 6)
**Estimated Effort**: 2-3 hours
**Risk Level**: Low
**Confidence**: High
