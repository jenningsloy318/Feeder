# Technical Specification: Multi-Provider AI Configuration Management

**Created:** 2026-01-01 16:02:00+08:00
**Status:** Complete
**Version:** 1.0

## Document Overview

This technical specification consolidates all requirements, architecture, and design decisions for implementing multi-provider AI configuration management in the Feeder app.

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Requirements](#requirements)
3. [Technical Approach](#technical-approach)
4. [Implementation Plan](#implementation-plan)
5. [Testing Strategy](#testing-strategy)
6. [Acceptance Criteria](#acceptance-criteria)
7. [Risk Mitigation](#risk-mitigation)

---

## Executive Summary

### Problem Statement
Users can currently only configure ONE instance of each AI provider type (OpenAI-compatible and Anthropic). This prevents use cases like:
- Multiple OpenAI endpoints (work vs personal)
- Different API keys for different projects
- Testing different providers without reconfiguration

### Solution
Implement a multi-provider configuration system that allows users to:
1. Configure multiple provider instances
2. Switch between providers easily
3. Edit and delete providers
4. Migrate existing settings seamlessly

### Impact
- **User Experience:** Significantly improved flexibility
- **Code Complexity:** Low (extends existing architecture)
- **Migration Risk:** Low (automatic migration with fallback)
- **Development Effort:** Medium (UI-heavy, backend-simple)

---

## Requirements

### Functional Requirements

#### FR1: Navigation & Label Changes
- Rename "API key" menu item to "Provider" in Settings > AI Integration
- Clicking "Provider" navigates to provider list screen

#### FR2: Provider List View
- Display all configured providers with:
  - Provider name/type
  - Active indicator (checkmark/border)
  - Validation status icon
  - Edit and delete buttons
- Tap provider item to activate it
- Empty state when no providers configured
- Add provider button (FAB)

#### FR3: Add Provider
- Navigate to edit screen (add mode)
- Provider type selection (OpenAI/Anthropic)
- Configuration form based on type
- Validation before saving
- Save to persistent storage

#### FR4: Edit Provider
- Navigate to edit screen (edit mode)
- Pre-populate form with current settings
- Validate changes before saving
- Update in list after save

#### FR5: Delete Provider
- Delete button on each item (except active)
- Confirmation dialog before deletion
- Remove from storage
- Prevent deletion of active provider

#### FR6: Activate Provider
- Tap provider item to activate
- Visual feedback (border + checkmark)
- Only one active at a time
- Persist selection

#### FR7: Data Persistence
- Store provider list in SharedPreferences (JSON)
- Migrate existing single-provider settings
- Maintain backward compatibility

### Non-Functional Requirements

#### NFR1: Performance
- Provider list loads within 100ms
- Save operations complete within 50ms
- No UI lag when switching providers

#### NFR2: Usability
- Intuitive navigation flow
- Clear error messages
- Consistent with existing Settings UI patterns

#### NFR3: Reliability
- Data persists across app restarts
- Graceful migration from old format
- No data loss during updates

#### NFR4: Maintainability
- Follow existing project patterns
- Reuse existing `AISettings` and provider client implementations
- Minimal changes to `AIApi` class

---

## Technical Approach

### Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ ProviderList │  │ ProviderEdit │  │  Settings │ │
│  │   Screen     │  │   Screen     │  │   Screen  │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│         ↓                  ↓                 ↑       │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                     │
│  ┌──────────────────┐  ┌──────────────────────┐   │
│  │ ProviderListVM   │  │  ProviderEditVM      │   │
│  └──────────────────┘  └──────────────────────┘   │
│         ↓                      ↓                    │
├─────────────────────────────────────────────────────┤
│                  Repository Layer                   │
│  ┌──────────────────────────────────────────────┐ │
│  │  Repository (thin wrapper to SettingsStore)  │ │
│  └──────────────────────────────────────────────┘ │
│         ↓                                            │
├─────────────────────────────────────────────────────┤
│                  Storage Layer                      │
│  ┌──────────────────────────────────────────────┐ │
│  │  SettingsStore (SharedPreferences + JSON)     │ │
│  │  - _providers: StateFlow<List<ProviderConfig>>│ │
│  │  - Migration logic                           │ │
│  └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### Data Model

#### ProviderConfig

```kotlin
@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val providerType: AIProvider,
    val openAISettings: OpenAISettings? = null,
    val anthropicSettings: AnthropicSettings? = null,
    val isActive: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
```

**Key Design Decisions:**
- `id`: String for simplicity and uniqueness
- `name`: User-defined label (can be blank)
- `openAISettings`/`anthropicSettings`: Nullable based on type
- `isActive`: Only one provider active at a time
- `@Serializable`: For kotlinx.serialization

### Storage Strategy

#### SharedPreferences Keys

```kotlin
private const val KEY_PROVIDER_LIST = "ai_provider_list"

// OLD KEYS (keep for backward compatibility)
private const val PREF_AI_PROVIDER_TYPE = "ai_provider_type"
private const val PREF_OPENAI_KEY = "openai_key"
// ... etc
```

#### JSON Format

```json
[
  {
    "id": "provider_1234567890",
    "name": "OpenAI - GPT-4",
    "providerType": "OPENAI_COMPATIBLE",
    "openAISettings": {
      "key": "sk-...",
      "modelId": "gpt-4o-mini",
      "baseUrl": "",
      "timeoutSeconds": 30,
      "azureApiVersion": "",
      "azureDeploymentId": ""
    },
    "anthropicSettings": null,
    "isActive": true,
    "createdAt": 1704096000000,
    "updatedAt": 1704096000000
  }
]
```

### Migration Strategy

#### Migration Logic

```kotlin
fun migrateFromOldSettings(): List<ProviderConfig> {
    // 1. Check if already migrated
    if (sp.contains(KEY_PROVIDER_LIST)) {
        return loadProviders()
    }

    // 2. Migrate OpenAI settings
    val oldOpenAIKey = sp.getString(PREF_OPENAI_KEY, "")
    if (oldOpenAIKey.isNotBlank()) {
        // Create ProviderConfig from old settings
    }

    // 3. Migrate Anthropic settings
    val oldAnthropicKey = sp.getString(PREF_ANTHROPIC_KEY, "")
    if (oldAnthropicKey.isNotBlank()) {
        // Create ProviderConfig from old settings
    }

    // 4. Save and return
    saveProviders(providers)
    return providers
}
```

#### Rollback Safety

- Old SharedPreferences keys kept intact during migration
- If migration fails, fallback to old format
- User can retry migration by deleting new key

---

## Implementation Plan

### Phase 1: Data Model & Storage (1-2 days)

**Tasks:**
1. Create `ProviderConfig.kt` data class
2. Add provider list StateFlow to `SettingsStore`
3. Implement migration logic
4. Add CRUD methods (add, update, delete, activate)
5. Extend `Repository` with provider list methods
6. Write unit tests for migration

**Deliverables:**
- `ProviderConfig.kt`
- Updated `SettingsStore.kt`
- Updated `Repository.kt`
- Unit tests

**Acceptance:**
- Migration runs automatically on app start
- Old settings successfully migrate to new format
- Repository methods work correctly

### Phase 2: ViewModels (1 day)

**Tasks:**
1. Create `ProviderListViewModel`
2. Create `ProviderEditViewModel`
3. Implement state management
4. Implement event handling
5. Write unit tests

**Deliverables:**
- `ProviderListViewModel.kt`
- `ProviderEditViewModel.kt`
- Unit tests

**Acceptance:**
- State updates correctly
- Events handled properly
- Providers load from Repository

### Phase 3: Navigation (0.5 day)

**Tasks:**
1. Add `ProviderListDestination` to navigation
2. Add `ProviderEditDestination` to navigation
3. Update Settings screen navigation
4. Test navigation flow

**Deliverables:**
- Updated `NavigationDestinations.kt`

**Acceptance:**
- Navigation to list works
- Navigation to add/edit works
- Back navigation works

### Phase 4: Provider List UI (2 days)

**Tasks:**
1. Create `ProviderListScreen`
2. Create `ProviderListItem` component
3. Create empty state component
4. Create delete confirmation dialog
5. Implement FAB
6. Apply Material Design 3 styling

**Deliverables:**
- `ProviderListScreen.kt`
- Supporting components

**Acceptance:**
- List displays all providers
- Active provider clearly marked
- Edit/delete buttons work
- Delete confirmation shown
- Empty state displays correctly

### Phase 5: Provider Edit UI (2 days)

**Tasks:**
1. Create `ProviderEditScreen`
2. Create provider type selector
3. Create OpenAI configuration form
4. Create Anthropic configuration form
5. Implement form validation
6. Implement save/cancel actions
7. Apply Material Design 3 styling

**Deliverables:**
- `ProviderEditScreen.kt`
- Form components

**Acceptance:**
- Form displays correctly for each provider type
- Validation prevents invalid saves
- Save creates/updates provider
- Cancel discards changes

### Phase 6: String Resources (1 day)

**Tasks:**
1. Add new string keys to `values/strings.xml`
2. Update existing "API key" to "Provider"
3. Translate to all supported languages (40+)
4. Verify translations

**Deliverables:**
- Updated string resources

**Acceptance:**
- All strings present in English
- Key strings translated
- No missing translations

### Phase 7: Testing (2 days)

**Tasks:**
1. Write unit tests for ProviderConfig
2. Write unit tests for migration
3. Write unit tests for ViewModels
4. Write UI tests (Compose testing)
5. Write integration tests
6. Test migration with real data
7. Test on multiple devices/API levels

**Deliverables:**
- Test suite
- Test report

**Acceptance:**
- All tests passing
- Migration tested with various scenarios
- UI flows tested

### Phase 8: Documentation (1 day)

**Tasks:**
1. Update user documentation
2. Update technical documentation
3. Document migration process
4. Create implementation summary
5. Update README (if applicable)

**Deliverables:**
- Updated documentation

**Acceptance:**
- Documentation accurate and complete

### Phase 9: Code Review & Polish (1 day)

**Tasks:**
1. Conduct code review
2. Fix any issues found
3. Optimize performance
4. Polish UI
5. Final testing

**Deliverables:**
- Code review report
- Refined code

**Acceptance:**
- All code review comments addressed
- Performance meets requirements
- UI is polished

**Total Estimated Effort:** 10.5-12.5 days

---

## Testing Strategy

### Unit Tests

#### ProviderConfig Tests
```kotlin
class ProviderConfigTest {
    @Test
    fun `toAISettings returns correct type`() {
        val openAIConfig = ProviderConfig(
            id = "1",
            name = "Test",
            providerType = AIProvider.OPENAI_COMPATIBLE,
            openAISettings = OpenAISettings(key = "test", modelId = "gpt-4"),
            isActive = false,
            createdAt = 0L,
            updatedAt = 0L,
        )
        val settings = openAIConfig.toAISettings()
        assertTrue(settings is AISettings.OpenAI)
    }

    @Test
    fun `isValid returns correct value`() {
        // Test valid and invalid configurations
    }
}
```

#### Migration Tests
```kotlin
class MigrationTest {
    @Test
    fun `migrateFromOldSettings creates correct ProviderConfig`() {
        // Setup old SharedPreferences
        // Run migration
        // Verify ProviderConfig created
    }

    @Test
    fun `migration handles empty old settings`() {
        // Test with no old settings
    }

    @Test
    fun `migration preserves active provider`() {
        // Test that active provider is correct
    }
}
```

### Integration Tests

```kotlin
class SettingsStoreIntegrationTest {
    @Test
    fun `save and load providers works`() {
        // Save provider list
        // Load provider list
        // Verify equality
    }

    @Test
    fun `activateProvider sets correct active flag`() {
        // Activate provider
        // Verify only one is active
    }

    @Test
    fun `deleteProvider works correctly`() {
        // Delete provider
        // Verify removed
        // Verify active provider preserved
    }
}
```

### UI Tests (Compose Testing)

```kotlin
class ProviderListScreenTest {
    @Test
    fun `provider list displays all providers`() {
        // Compose screen with test data
        // Verify all items displayed
    }

    @Test
    fun `tap on provider activates it`() {
        // Tap provider
        // Verify active indicator appears
    }

    @Test
    fun `delete button shows confirmation`() {
        // Click delete
        // Verify dialog shown
    }
}
```

---

## Acceptance Criteria

### Must Have (P0)

- [ ] Users can view all configured providers
- [ ] Users can add new providers
- [ ] Users can edit existing providers
- [ ] Users can delete non-active providers
- [ ] Users can activate providers by tapping
- [ ] Migration from old settings works automatically
- [ ] Active provider is clearly marked
- [ ] Invalid configurations show error indicator
- [ ] All form fields validate correctly
- [ ] Data persists across app restarts

### Should Have (P1)

- [ ] Provider list loads within 100ms
- [ ] Save operations complete within 50ms
- [ ] Empty state displays when no providers
- [ ] Delete confirmation dialog shown
- [ ] Cannot delete active provider
- [ ] Provider name can be customized
- [ ] Azure settings auto-detect from URL

### Could Have (P2)

- [ ] Provider icons based on type
- [ ] Drag to reorder providers
- [ ] Duplicate provider functionality
- [ ] Export/import provider configurations

---

## Risk Mitigation

### Risk 1: Migration Data Loss

**Probability:** Low
**Impact:** High
**Mitigation:**
- Comprehensive testing of migration logic
- Keep old SharedPreferences keys intact
- Fallback to old format if migration fails
- Provide way to retry migration

### Risk 2: UI Complexity

**Probability:** Medium
**Impact:** Medium
**Mitigation:**
- Follow existing Settings UI patterns
- Reuse components from AIProviderSection
- Iterative design and testing
- User testing before release

### Risk 3: Performance Regression

**Probability:** Low
**Impact:** Low
**Mitigation:**
- Use efficient JSON serialization
- Lazy load provider list
- Optimize list rendering (use keys)
- Performance testing

### Risk 4: User Confusion

**Probability:** Medium
**Impact:** Medium
**Mitigation:**
- Clear visual indicators
- Intuitive navigation
- Helpful error messages
- Onboarding tooltip (optional)

---

## Success Metrics

### Quantitative

- Migration success rate: > 99%
- Provider list load time: < 100ms
- Save operation time: < 50ms
- Zero data loss reports
- User adoption: > 80% configure multiple providers

### Qualitative

- Users find UI intuitive
- Provider management is straightforward
- Migration is seamless
- No increase in support tickets

---

## Dependencies

### Code Dependencies

- Existing `AIProvider` enum
- Existing `AISettings` sealed interface
- Existing `SettingsStore` class
- Existing `Repository` class
- Existing navigation infrastructure
- Jetpack Compose
- kotlinx.serialization

### Team Dependencies

- Android developer for implementation
- QA tester for testing
- Translator for string resources
- UI/UX designer review (optional)

---

## Open Questions

### Q1: Maximum number of providers?

**Status:** Resolved
**Decision:** No hard limit, but show warning after 5 providers

### Q2: Provider naming?

**Status:** Resolved
**Decision:** Auto-generate with option to edit

### Q3: Migration behavior on failure?

**Status:** Resolved
**Decision:** Fallback to old format, keep old keys

---

## Next Steps

1. ✅ Specification complete
2. ⏭️ User review and approval
3. ⏭️ Begin implementation (Phase 1)
4. ⏭️ Progress through phases 1-9
5. ⏭️ Final verification and release

---

## Appendix

### File Changes Summary

| File | Change | Complexity |
|------|--------|------------|
| `ProviderConfig.kt` | NEW | Low |
| `SettingsStore.kt` | EXTEND | Medium |
| `Repository.kt` | EXTEND | Low |
| `ProviderListViewModel.kt` | NEW | Low |
| `ProviderEditViewModel.kt` | NEW | Low |
| `ProviderListScreen.kt` | NEW | Medium |
| `ProviderEditScreen.kt` | NEW | Medium |
| `NavigationDestinations.kt` | EXTEND | Low |
| `strings.xml` (all) | UPDATE | Medium |

### API Endpoints

No backend API endpoints - all local storage.

### Database Changes

No database changes - uses SharedPreferences.

---

**Document Status:** Ready for Implementation
**Last Updated:** 2026-01-01 16:02:00+08:00
**Next Review:** After Phase 1 completion
