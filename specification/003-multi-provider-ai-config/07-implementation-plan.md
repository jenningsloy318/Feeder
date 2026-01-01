# Implementation Plan: Multi-Provider AI Configuration

**Created:** 2026-01-01 16:03:00+08:00
**Status:** Ready for Execution
**Estimated Duration:** 10.5-12.5 days

## Overview

This document breaks down the implementation into actionable tasks with clear dependencies, acceptance criteria, and testing requirements.

---

## Phase 1: Data Model & Storage (Days 1-2)

### Task 1.1: Create ProviderConfig Data Class
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/ProviderConfig.kt`
**Effort:** 2 hours

**Steps:**
1. Create ProviderConfig data class
2. Implement toAISettings() method
3. Implement validation property
4. Implement getDisplayName() method
5. Implement companion object methods
6. Add @Serializable annotation

**Acceptance Criteria:**
- [ ] Data class compiles without errors
- [ ] All properties defined correctly
- [ ] toAISettings() returns correct type
- [ ] Validation works correctly
- [ ] JSON serialization/deserialization works

**Testing:**
```kotlin
@Test
fun testProviderConfigToAISettings() {
    val config = ProviderConfig(
        id = "test-id",
        name = "Test Provider",
        providerType = AIProvider.OPENAI_COMPATIBLE,
        openAISettings = OpenAISettings(key = "test-key", modelId = "gpt-4"),
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
    val settings = config.toAISettings()
    assertTrue(settings is AISettings.OpenAI)
    assertEquals("test-key", (settings as AISettings.OpenAI).openaiSettings.key)
}
```

---

### Task 1.2: Extend SettingsStore for Provider List
**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
**Effort:** 4 hours

**Steps:**
1. Add provider list StateFlow
2. Implement migration logic
3. Implement saveProviders() method
4. Implement loadProviders() method
5. Implement addProvider() method
6. Implement updateProvider() method
7. Implement deleteProvider() method
8. Implement activateProvider() method
9. Update aiSettings property to use provider list
10. Update aiSettingsFlow to use provider list

**Acceptance Criteria:**
- [ ] Provider list StateFlow added
- [ ] Migration runs on init
- [ ] CRUD methods work correctly
- [ ] aiSettings returns active provider
- [ ] Backward compatibility maintained

**Testing:**
```kotlin
@Test
fun testMigrationFromOldSettings() {
    // Setup old SharedPreferences
    val sp = SharedPreferences(...)
    sp.edit().putString("openai_key", "test-key").commit()
    sp.edit().putString("openai_model_id", "gpt-4").commit()

    // Create SettingsStore
    val store = SettingsStore(di)

    // Verify migration
    val providers = store.providers.value
    assertEquals(1, providers.size)
    assertEquals("test-key", providers[0].openAISettings?.key)
}
```

---

### Task 1.3: Extend Repository for Provider List
**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
**Effort:** 1 hour

**Steps:**
1. Add providers StateFlow property
2. Add addProvider() method
3. Add updateProvider() method
4. Add deleteProvider() method
5. Add activateProvider() method
6. Add activeProvider StateFlow property

**Acceptance Criteria:**
- [ ] All methods delegate to SettingsStore
- [ ] activeProvider StateFlow works correctly

**Testing:**
```kotlin
@Test
fun testRepositoryDelegation() {
    val repo = Repository(store)
    repo.providers.test {
        assertEquals(expectedProviders, awaitItem())
    }
}
```

---

### Task 1.4: Write Unit Tests for Storage Layer
**Files:** Test files for SettingsStore and Repository
**Effort:** 3 hours

**Test Cases:**
- [ ] Migration from old OpenAI settings
- [ ] Migration from old Anthropic settings
- [ ] Migration with no old settings
- [ ] Migration preserves active provider
- [ ] saveProviders() saves to SharedPreferences
- [ ] loadProviders() loads from SharedPreferences
- [ ] addProvider() adds to list
- [ ] updateProvider() updates existing provider
- [ ] deleteProvider() removes from list
- [ ] activateProvider() sets correct active flag
- [ ] aiSettings returns active provider
- [ ] Backward compatibility with old format

---

## Phase 2: ViewModels (Day 3)

### Task 2.1: Create ProviderListViewModel
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListViewModel.kt`
**Effort:** 2 hours

**Steps:**
1. Create ProviderListEvent sealed interface
2. Create ProviderListState data class
3. Create ProviderListViewModel class
4. Implement loadProviders() method
5. Implement onEvent() method
6. Implement confirmDelete() method
7. Add StateFlow properties

**Acceptance Criteria:**
- [ ] ViewModel loads providers on init
- [ ] State updates when providers change
- [ ] Delete confirmation works correctly

**Testing:**
```kotlin
@Test
fun testDeleteProviderShowsConfirmation() {
    val viewModel = ProviderListViewModel(repository)
    viewModel.onEvent(ProviderListEvent.DeleteProvider("provider-1"))

    assertEquals(
        "provider-1",
        viewModel.uiState.value.providerToDelete?.id
    )
}
```

---

### Task 2.2: Create ProviderEditViewModel
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
**Effort:** 3 hours

**Steps:**
1. Create ProviderEditEvent sealed interface
2. Create ProviderEditState data class
3. Create ProviderEditViewModel class
4. Implement loadProvider() method
5. Implement onEvent() method
6. Implement saveProvider() method
7. Add validation logic

**Acceptance Criteria:**
- [ ] ViewModel loads provider in edit mode
- [ ] ViewModel starts with defaults in add mode
- [ ] Validation prevents invalid saves
- [ ] Save creates or updates provider

**Testing:**
```kotlin
@Test
fun testSaveProviderValidates() {
    val viewModel = ProviderEditViewModel(repository, null)
    viewModel.onEvent(ProviderEditEvent.SetName("")) // Empty name
    viewModel.onEvent(ProviderEditEvent.Save)

    assertNotNull(viewModel.uiState.value.error)
}
```

---

### Task 2.3: Write Unit Tests for ViewModels
**Files:** Test files for ViewModels
**Effort:** 3 hours

**Test Cases:**
- [ ] ProviderListViewModel loads providers
- [ ] ProviderListViewModel handles delete event
- [ ] ProviderListViewModel confirms delete
- [ ] ProviderListViewModel dismisses confirmation
- [ ] ProviderEditViewModel loads provider in edit mode
- [ ] ProviderEditViewModel starts empty in add mode
- [ ] ProviderEditViewModel validates name
- [ ] ProviderEditViewModel validates settings
- [ ] ProviderEditViewModel saves new provider
- [ ] ProviderEditViewModel updates existing provider

---

## Phase 3: Navigation (Day 4, Half Day)

### Task 3.1: Add Navigation Destinations
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
**Effort:** 2 hours

**Steps:**
1. Create ProviderListDestination object
2. Create ProviderEditDestination object
3. Implement RegisterScreen() for list
4. Implement RegisterScreen() for edit
5. Implement navigate() methods
6. Update Settings screen to navigate to list

**Acceptance Criteria:**
- [ ] Navigation to list works
- [ ] Navigation to add works
- [ ] Navigation to edit works with provider ID
- [ ] Back navigation works correctly

---

## Phase 4: Provider List UI (Days 4-5)

### Task 4.1: Create ProviderListScreen
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListScreen.kt`
**Effort:** 4 hours

**Steps:**
1. Create screen scaffold with top bar
2. Create FAB for adding provider
3. Implement LazyColumn for provider list
4. Connect to ViewModel
5. Handle state changes
6. Handle navigation events

**Acceptance Criteria:**
- [ ] Screen displays all providers
- [ ] FAB navigates to add screen
- [ ] Empty state displays when no providers
- [ ] State updates from ViewModel

---

### Task 4.2: Create ProviderListItem Component
**File:** In ProviderListScreen.kt
**Effort:** 2 hours

**Steps:**
1. Create item layout with Card
2. Add provider name and type
3. Add active indicator
4. Add validation icon
5. Add edit button
6. Add delete button (if not active)
7. Implement click handler for activation
8. Add animations

**Acceptance Criteria:**
- [ ] Item displays provider info correctly
- [ ] Active provider has visual indicator
- [ ] Invalid provider shows warning icon
- [ ] Delete button hidden for active provider
- [ ] Click activates provider

---

### Task 4.3: Create Empty State Component
**File:** In ProviderListScreen.kt
**Effort:** 1 hour

**Steps:**
1. Create empty state layout
2. Add icon and message
3. Add button to add first provider
4. Style according to Material Design 3

**Acceptance Criteria:**
- [ ] Displays when provider list is empty
- [ ] Button navigates to add screen
- [ ] Styled correctly

---

### Task 4.4: Create Delete Confirmation Dialog
**File:** In ProviderListScreen.kt
**Effort:** 1 hour

**Steps:**
1. Create AlertDialog
2. Display provider name
3. Add confirm and dismiss buttons
4. Style error button correctly

**Acceptance Criteria:**
- [ ] Dialog displays provider name
- [ ] Confirm button is red
- [ ] Dismiss button cancels deletion

---

## Phase 5: Provider Edit UI (Days 6-7)

### Task 5.1: Create ProviderEditScreen Scaffold
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Effort:** 2 hours

**Steps:**
1. Create screen scaffold with top bar
2. Add scrollable content column
3. Add save and cancel buttons
4. Connect to ViewModel
5. Handle state changes
6. Handle save/cancel events

**Acceptance Criteria:**
- [ ] Screen title reflects add/edit mode
- [ ] Form is scrollable
- [ ] Save button disabled while saving
- [ ] Cancel button navigates back

---

### Task 5.2: Create Provider Type Selector
**File:** In ProviderEditScreen.kt
**Effort:** 1 hour

**Steps:**
1. Create FilterChip group
2. Add OpenAI option
3. Add Anthropic option
4. Implement selection logic
5. Update ViewModel on selection

**Acceptance Criteria:**
- [ ] Both provider types shown
- [ ] Selection updates ViewModel
- [ ] Visual feedback for selected type

---

### Task 5.3: Create OpenAI Configuration Form
**File:** In ProviderEditScreen.kt
**Effort:** 3 hours

**Steps:**
1. Create provider name text field
2. Create API key text field (password)
3. Create model ID text field
4. Create base URL text field (optional)
5. Create timeout text field (number)
6. Create Azure settings section (conditional)
7. Add validation indicators
8. Add helper text

**Acceptance Criteria:**
- [ ] All fields display correctly
- [ ] Required fields marked
- [ ] Validation shows errors
- [ ] Azure section shows for Azure URLs
- [ ] Helper text displays

---

### Task 5.4: Create Anthropic Configuration Form
**File:** In ProviderEditScreen.kt
**Effort:** 2 hours

**Steps:**
1. Create provider name text field
2. Create API key text field (password)
3. Create model ID text field
4. Create base URL text field (optional)
5. Create timeout text field (number)
6. Add validation indicators
7. Add helper text

**Acceptance Criteria:**
- [ ] All fields display correctly
- [ ] Required fields marked
- [ ] Validation shows errors
- [ ] Helper text displays

---

### Task 5.5: Implement Form Validation
**File:** In ProviderEditScreen.kt
**Effort:** 2 hours

**Steps:**
1. Validate provider name (not blank)
2. Validate API key (not blank)
3. Validate model ID (not blank)
4. Validate timeout (30-600)
5. Validate Azure settings (if applicable)
6. Display error messages
7. Disable save button when invalid

**Acceptance Criteria:**
- [ ] All validations work correctly
- [ ] Error messages display
- [ ] Save button disabled when invalid
- [ ] Save button enabled when valid

---

## Phase 6: String Resources (Day 8)

### Task 6.1: Update English Strings
**File:** `app/src/main/res/values/strings.xml`
**Effort:** 1 hour

**Steps:**
1. Add new string keys
2. Update "api_key" to "Provider"
3. Verify all strings present

**Strings to Add:**
- provider_list_title
- no_providers_configured
- add_provider_to_get_started
- add_first_provider
- active_provider
- invalid_configuration
- edit_provider
- delete_provider
- delete_provider_title
- delete_provider_confirmation
- add_provider
- edit_provider (reuse edit_provider)
- provider_name_label
- provider_type_label
- save
- cancel
- api_key_label
- model_id_label
- base_url_label
- base_url_placeholder
- base_url_help
- timeout_label
- timeout_help
- timeout_range_error
- azure_settings
- azure_api_version
- azure_deployment_id
- required_for_azure
- required_field
- default_model

**Acceptance Criteria:**
- [ ] All new strings added
- [ ] No missing strings
- [ ] "api_key" updated to "Provider"

---

### Task 6.2: Translate to All Languages
**Files:** All `values-*/strings.xml`
**Effort:** 4 hours (or outsource to translators)

**Languages:**
- Spanish (es)
- German (de)
- French (fr)
- Chinese Simplified (zh-rCN)
- Chinese Traditional (zh-rTW)
- Japanese (ja)
- Korean (ko)
- Portuguese Brazil (pt-rBR)
- Russian (ru)
- Italian (it)
- Dutch (nl)
- Polish (pl)
- Turkish (tr)
- Ukrainian (uk)
- Thai (th)
- Indonesian (in)
- Vietnamese (vi)
- Swedish (sv)
- Norwegian (no)
- Danish (da)
- Finnish (fi)
- Greek (el)
- Czech (cs)
- Romanian (ro)
- Hungarian (hu)
- Bulgarian (bg)
- Serbian (sr)
- Slovak (sk)
- Croatian (hr)
- Catalan (ca)
- Hebrew (iw)
- Arabic (ar)
- Hindi (hi)
- Bengali (bn)
- Farsi (fa)

**Acceptance Criteria:**
- [ ] All translations added
- [ ] No missing translations
- [ ] Context maintained

---

## Phase 7: Testing (Days 9-10)

### Task 7.1: Write Unit Tests
**Files:** Test files
**Effort:** 4 hours

**Test Coverage:**
- [ ] ProviderConfig data class
- [ ] SettingsStore migration
- [ ] SettingsStore CRUD operations
- [ ] Repository delegation
- [ ] ProviderListViewModel
- [ ] ProviderEditViewModel

**Target:** > 80% code coverage

---

### Task 7.2: Write UI Tests (Compose Testing)
**Files:** UI test files
**Effort:** 4 hours

**Test Cases:**
- [ ] Provider list displays
- [ ] Tap provider activates it
- [ ] Edit button navigates correctly
- [ ] Delete button shows dialog
- [ ] Confirm delete removes provider
- [ ] FAB navigates to add screen
- [ ] Add screen displays correctly
- [ ] Edit screen pre-populates
- [ ] Form validation works
- [ ] Save creates provider
- [ ] Save updates provider
- [ ] Cancel discards changes

---

### Task 7.3: Integration Testing
**Effort:** 3 hours

**Test Scenarios:**
- [ ] Migration from old settings
- [ ] Add → Edit → Delete flow
- [ ] Switch between providers
- [ ] Persistence across app restart
- [ ] Invalid configuration handling

---

### Task 7.4: Manual Testing
**Effort:** 3 hours

**Test Devices:**
- [ ] Phone (API 26, 30, 33)
- [ ] Tablet
- [ ] Foldable
- [ ] Dark/Light mode

**Test Flows:**
- [ ] New user (no migration)
- [ ] Existing user (migration)
- [ ] Add multiple providers
- [ ] Edit providers
- [ ] Delete providers
- [ ] Switch between providers
- [ ] Invalid configurations

---

## Phase 8: Documentation (Day 11)

### Task 8.1: Update User Documentation
**File:** User-facing documentation
**Effort:** 2 hours

**Updates:**
- [ ] Add AI provider setup section
- [ ] Explain multi-provider feature
- [ ] Add screenshots
- [ ] Update troubleshooting

---

### Task 8.2: Update Technical Documentation
**File:** Technical docs
**Effort:** 2 hours

**Updates:**
- [ ] Document architecture changes
- [ ] Document data model
- [ ] Document migration process
- [ ] Update API docs (internal)

---

### Task 8.3: Create Implementation Summary
**File:** `specification/01-multi-provider-ai-config/06-implementation-summary.md`
**Effort:** 2 hours

**Contents:**
- [ ] Files created
- [ ] Files modified
- [ ] Technical decisions
- [ ] Challenges encountered
- [ ] Deviations from spec
- [ ] Known issues

---

## Phase 9: Code Review & Polish (Day 12)

### Task 9.1: Conduct Code Review
**Effort:** 3 hours

**Review Checklist:**
- [ ] Code follows project conventions
- [ ] No TODO/FIXME comments
- [ ] No debug code
- [ ] Proper error handling
- [ ] Adequate testing
- [ ] Documentation complete
- [ ] Performance acceptable

---

### Task 9.2: Fix Issues & Polish
**Effort:** 4 hours

**Tasks:**
- [ ] Fix code review comments
- [ ] Optimize performance
- [ ] Polish UI interactions
- [ ] Refine animations
- [ ] Improve error messages

---

### Task 9.3: Final Testing
**Effort:** 2 hours

**Tasks:**
- [ ] Smoke test all features
- [ ] Test on real device
- [ ] Verify migration works
- [ ] Check all string resources
- [ ] Verify accessibility

---

## Task Dependencies

```
Phase 1 (Data Model & Storage)
  ├─ Task 1.1: ProviderConfig
  ├─ Task 1.2: SettingsStore (depends on 1.1)
  ├─ Task 1.3: Repository (depends on 1.2)
  └─ Task 1.4: Tests (depends on 1.1, 1.2, 1.3)

Phase 2 (ViewModels)
  ├─ Task 2.1: ProviderListViewModel (depends on Phase 1)
  ├─ Task 2.2: ProviderEditViewModel (depends on Phase 1)
  └─ Task 2.3: Tests (depends on 2.1, 2.2)

Phase 3 (Navigation)
  └─ Task 3.1: Navigation (depends on Phase 2)

Phase 4 (Provider List UI)
  ├─ Task 4.1: ProviderListScreen (depends on Phase 3)
  ├─ Task 4.2: ProviderListItem (depends on 4.1)
  ├─ Task 4.3: Empty State (depends on 4.1)
  └─ Task 4.4: Delete Dialog (depends on 4.1)

Phase 5 (Provider Edit UI)
  ├─ Task 5.1: ProviderEditScreen (depends on Phase 3)
  ├─ Task 5.2: Type Selector (depends on 5.1)
  ├─ Task 5.3: OpenAI Form (depends on 5.1)
  ├─ Task 5.4: Anthropic Form (depends on 5.1)
  ├─ Task 5.5: Validation (depends on 5.2, 5.3, 5.4)
  └─ Task 5.6: Error Display (depends on 5.5)

Phase 6 (Strings)
  └─ Task 6.1-6.2: Strings (can run parallel with Phase 4-5)

Phase 7 (Testing)
  ├─ Task 7.1: Unit Tests (depends on Phase 1-5)
  ├─ Task 7.2: UI Tests (depends on Phase 4-5)
  ├─ Task 7.3: Integration Tests (depends on Phase 1-5)
  └─ Task 7.4: Manual Tests (depends on Phase 1-6)

Phase 8 (Documentation)
  └─ Task 8.1-8.3: Docs (depends on Phase 1-7)

Phase 9 (Code Review & Polish)
  ├─ Task 9.1: Code Review (depends on Phase 1-8)
  ├─ Task 9.2: Fix & Polish (depends on 9.1)
  └─ Task 9.3: Final Testing (depends on 9.2)
```

---

## Risk Mitigation

### Risk: Migration Data Loss

**Mitigation:**
- Comprehensive testing (Task 1.4, 7.3, 7.4)
- Keep old SharedPreferences keys
- Fallback to old format
- Test with real user data

---

### Risk: UI Complexity

**Mitigation:**
- Follow existing patterns
- Reuse components
- Iterative design (Tasks 4.1-4.4, 5.1-5.6)
- User testing (Task 7.4)

---

### Risk: Performance Issues

**Mitigation:**
- Use LazyColumn with keys
- Efficient JSON serialization
- Performance testing (Task 7.3, 7.4)
- Optimize in Task 9.2

---

## Success Criteria

### Must Complete (Phase Gate)

- [ ] All phases complete
- [ ] All tests passing (> 80% coverage)
- [ ] Manual testing successful
- [ ] Code review approved
- [ ] Documentation complete

### Definition of Done

- [ ] Code compiles without warnings
- [ ] All tests passing
- [ ] No TODO/FIXME comments
- [ ] No debug code
- [ ] String resources complete
- [ ] Documentation updated
- [ ] Code review approved

---

## Next Steps

1. ✅ Implementation plan complete
2. ⏭️ User review and approval
3. ⏭️ Begin Phase 1: Data Model & Storage
4. ⏭️ Execute phases 1-9 sequentially
5. ⏭️ Final verification and release

---

**Status:** Ready for Execution
**Last Updated:** 2026-01-01 16:03:00+08:00
