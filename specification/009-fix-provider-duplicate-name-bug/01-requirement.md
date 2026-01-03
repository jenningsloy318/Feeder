# Requirements: Fix Provider Duplicate Name Bug

**Feature ID**: 009
**Bug ID**: 009-DUPLICATE-PROVIDER-NAME
**Status**: Requirements Gathering
**Created**: 2026-01-03
**Related**: [spec-001](../../../ai-features-more-providers-01/specifications/001-ai-features-more-providers/01-requirement.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Bug Description](#bug-description)
3. [Functional Requirements](#functional-requirements)
4. [Technical Requirements](#technical-requirements)
5. [User Stories](#user-stories)
6. [Acceptance Criteria](#acceptance-criteria)

---

## Overview

### Summary

A critical bug exists in the multi-provider AI feature (spec-001) where users can create multiple AI providers with identical names without any validation. This causes confusion and potential data integrity issues.

### Current Behavior

1. User creates a provider named "My OpenAI"
2. User creates another provider also named "My OpenAI"
3. Both providers are saved successfully
4. Users cannot distinguish between providers in the list

### Expected Behavior

The system should prevent creation of providers with duplicate names and provide clear feedback to users.

### Goals

1. **Prevent duplicates**: Enforce unique provider names across all providers
2. **Clear validation**: Show user-friendly error messages when duplicates are attempted
3. **Case handling**: Define whether "My Provider" and "my provider" are considered duplicates
4. **Edit scenario**: Handle renaming to an existing name during editing

---

## Bug Description

### Severity: Medium

**Impact**: User experience and data integrity

**Priority**: High (should be fixed before next release)

### Evidence

#### Location 1: ProviderEditViewModel.kt (line 258-287)

```kotlin
fun saveProvider() {
    val current = _internalState.value.provider

    // Validate before saving
    if (!current.isValid) {
        return
    }

    _internalState.value = _internalState.value.copy(isSaving = true, saveResult = null)

    viewModelScope.launch {
        try {
            if (_internalState.value.isNew) {
                repository.addProvider(current)  // ❌ No duplicate check!
            } else {
                repository.updateProvider(current)
            }
            // ...
        }
    }
}
```

**Problem**: Only validates `current.isValid`, which checks if API key and other fields are present, but does NOT check if the name already exists.

#### Location 2: SettingsStore.kt (line 641-644)

```kotlin
fun addProvider(provider: ProviderConfig) {
    val updated = _providers.value + provider  // ❌ Simply adds to list!
    saveProviders(updated)
}
```

**Problem**: No validation to check if a provider with the same name already exists.

### Related Bug Pattern

This is similar to the LazyColumn bug mentioned by the user where items can be added without proper validation, leading to duplicate or inconsistent UI states.

---

## Functional Requirements

### FR-1: Duplicate Name Prevention

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-1.1 | System must prevent creating providers with duplicate names | Must | ❌ Not Implemented |
| FR-1.2 | Duplicate check must be case-insensitive ("My Provider" = "my provider") | Must | ❌ Not Implemented |
| FR-1.3 | During edit, prevent renaming to an existing provider's name | Must | ❌ Not Implemented |
| FR-1.4 | Validation must exclude current provider when editing | Must | ❌ Not Implemented |

### FR-2: User Feedback

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-2.1 | Show error message when duplicate name is detected | Must | ❌ Not Implemented |
| FR-2.2 | Error message must indicate which provider has the duplicate name | Should | ❌ Not Implemented |
| FR-2.3 | Disable save button when duplicate name is detected | Should | ❌ Not Implemented |
| FR-2.4 | Real-time validation as user types the name | Should | ❌ Not Implemented |

### FR-3: Validation Logic

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-3.1 | Trim whitespace before checking duplicates | Must | ❌ Not Implemented |
| FR-3.2 | Empty names should be caught by existing validation | Must | ✅ Already Works |
| FR-3.3 | Special characters should be allowed in names | Should | ✅ Already Works |

---

## Technical Requirements

### TR-1: Validation Location

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-1.1 | Add validation in SettingsStore.addProvider() | Must | ❌ Not Implemented |
| TR-1.2 | Add validation in SettingsStore.updateProvider() | Must | ❌ Not Implemented |
| TR-1.3 | Create utility function to check for duplicates | Should | ❌ Not Implemented |

### TR-2: Error Handling

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-2.1 | Throw specific exception for duplicate names | Must | ❌ Not Implemented |
| TR-2.2 | Exception must contain duplicate provider info | Should | ❌ Not Implemented |
| TR-2.3 | ViewModel must catch and display error to user | Must | ❌ Not Implemented |

### TR-3: Testing

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-3.1 | Unit test for duplicate detection | Must | ❌ Not Implemented |
| TR-3.2 | Unit test for case-insensitive comparison | Must | ❌ Not Implemented |
| TR-3.3 | Unit test for edit scenario (excluding self) | Must | ❌ Not Implemented |
| TR-3.4 | Integration test with ViewModel | Should | ❌ Not Implemented |

---

## User Stories

### US-1: Prevent Duplicate Creation

**As a** user configuring AI providers
**I want** to be prevented from creating two providers with the same name
**So that** I don't get confused about which provider I'm selecting

**Acceptance**:
- Cannot create provider with existing name (case-insensitive)
- Clear error message shown
- Save remains disabled until name is unique

### US-2: Prevent Duplicate Rename

**As a** user editing an existing provider
**I want** to be prevented from renaming it to match another provider
**So that** all my providers have unique, identifiable names

**Acceptance**:
- Cannot rename to existing provider's name
- Current provider's name is excluded from duplicate check
- Error message shows which provider already has that name

### US-3: Clear Error Messages

**As a** user
**I want** to see helpful error messages when I try to use a duplicate name
**So that** I understand what went wrong and how to fix it

**Acceptance**:
- Error message says "A provider named 'X' already exists"
- Error appears in both Snackbar and inline field error
- Real-time validation shows error as I type

---

## Acceptance Criteria

### AC-1: Duplicate Prevention

**Given**: I am on the "Add Provider" screen
**When**: I enter a name that matches an existing provider (case-insensitive)
**Then**:
- Save button should be disabled
- Name field should show error state
- Error message should indicate duplicate name

### AC-2: Edit Scenario

**Given**: I am editing provider "Provider A"
**And**: Another provider named "Provider B" exists
**When**: I rename "Provider A" to "Provider B"
**Then**:
- Validation should fail
- Error should indicate "Provider B" already exists
- Save should be prevented

### AC-3: Self-Exclusion

**Given**: I am editing provider "My Provider"
**When**: I save without changing the name
**Then**: Validation should pass (self is excluded from check)

### AC-4: Case-Insensitive Matching

**Given**: A provider named "My OpenAI" exists
**When**: I try to create a provider named "my openai"
**Then**: Validation should fail (case-insensitive)

### AC-5: Whitespace Handling

**Given**: A provider named "My Provider" exists
**When**: I try to create a provider named "  My Provider  "
**Then**: Validation should fail (after trimming)

---

## Edge Cases

### EC-1: Empty Provider List

**Scenario**: First provider being created
**Expected**: No duplicate validation errors (list is empty)

### EC-2: Rapid Name Changes

**Scenario**: User rapidly types and backspaces names
**Expected**: Validation updates correctly without crashes

### EC-3: Special Characters

**Scenario**: Names with special characters like "My Provider (Copy)"
**Expected**: Special characters allowed, duplicates still prevented

### EC-4: Unicode Characters

**Scenario**: Names with Unicode like "My Provider™" or "我的供应商"
**Expected**: Case-insensitive comparison works with Unicode

### EC-5: Very Long Names

**Scenario**: User enters very long name (100+ characters)
**Expected**: Validation still works, UI handles gracefully

---

## Non-Functional Requirements

### NFR-1: Performance

- Duplicate check must complete in < 50ms for lists up to 100 providers
- Validation should not block UI thread

### NFR-2: User Experience

- Validation feedback should be instant (real-time)
- Error messages should be clear and actionable
- No confusing technical jargon

### NFR-3: Data Integrity

- Provider names must remain unique after app restart
- Validation must work with serialized data from SharedPreferences

---

## Dependencies

### Internal Dependencies

- **spec-001**: Multi-provider feature (already implemented)
- **SettingsStore.kt**: Where validation must be added
- **ProviderEditViewModel.kt**: Where error handling must be added

### External Dependencies

None - pure logic change, no new libraries needed

---

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Duplicate providers can be created | ✅ Yes (BUG) | ❌ No (Fixed) |
| User receives error on duplicate | ❌ No | ✅ Yes |
| Validation is case-insensitive | ❌ No | ✅ Yes |
| Edit scenario works correctly | ❌ No | ✅ Yes |

---

## References

- Original Feature: [spec-001](../../../ai-features-more-providers-01/specifications/001-ai-features-more-providers/01-requirement.md)
- Related Spec: [spec-003](../../../ai-features-improve-providers/specifications/003-ai-features-improve-providers/) (if exists)
- Bug Report: User feedback about LazyColumn-like duplicate issue

---

## Status

**Phase**: Requirements Gathering
**Next Steps**: Research best practices for unique validation
**Estimated Complexity**: Low-Medium (simple validation logic)
**Estimated Time**: 2-3 hours
