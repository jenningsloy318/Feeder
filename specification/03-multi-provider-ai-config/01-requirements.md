# Requirements: Multi-Provider AI Configuration Management

**Created:** 2026-01-01 15:51:30+08:00
**Status:** Draft
**Phase:** Requirements Clarification

## Context from Codebase Analysis

### Current Implementation

**Architecture:**
- Multi-provider support already implemented with factory pattern
- Two providers: `OPENAI_COMPATIBLE` and `ANTHROPIC`
- Unified `AIApi` class abstracts provider-specific details
- Settings stored in `SharedPreferences` via `SettingsStore`

**Data Models:**
```kotlin
// Provider enum
enum class AIProvider {
    OPENAI_COMPATIBLE,
    ANTHROPIC
}

// Settings structure
sealed interface AISettings {
    data class OpenAI(openaiSettings: OpenAISettings) : AISettings
    data class Anthropic(anthropicSettings: AnthropicSettings) : AISettings
}

// Current settings storage
- PREF_AI_PROVIDER_TYPE (stores selected provider enum)
- PREF_OPENAI_KEY, PREF_OPENAI_MODEL_ID, PREF_OPENAI_URL, etc.
- PREF_ANTHROPIC_KEY, PREF_ANTHROPIC_MODEL_ID, PREF_ANTHROPIC_URL, etc.
```

**UI Location:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`
- Current: Single provider configuration in Settings > AI Integration
- Label currently says "API key" (needs to be renamed to "Provider")

**Key Finding:**
The system ALREADY supports multiple providers at the code level. What's missing is the **UI/UX to manage multiple configured provider instances**. Currently, users can only configure ONE instance of each provider type.

## Problem Statement

**Current Limitation:**
- Users can only configure ONE OpenAI-compatible provider and ONE Anthropic provider
- Cannot have multiple OpenAI instances (e.g., one for OpenAI, one for Azure, one for Perplexity)
- Cannot have multiple Anthropic instances with different API keys/models
- Settings UI shows single configuration form per provider type

**User Need:**
Users want to:
1. Configure multiple provider instances (e.g., work vs personal keys)
2. Easily switch between configured providers
3. Edit existing provider configurations
4. Add new providers without replacing existing ones

## Functional Requirements

### FR1: Navigation and Label Changes
**Priority:** High
**Description:** Update Settings > AI Integration navigation

**Requirements:**
1. Rename "API key" menu item to "Provider" in Settings > AI Integration
2. When clicking "Provider", navigate to provider list screen (instead of single provider form)

**Acceptance Criteria:**
- [ ] Settings menu shows "Provider" instead of "API key"
- [ ] Clicking "Provider" opens provider list screen
- [ ] Navigation flow works correctly (back button returns to Settings)

### FR2: Provider List View
**Priority:** High
**Description:** Display all configured provider instances

**Requirements:**
1. Show list of all configured providers (across both provider types)
2. Display provider name/type (e.g., "OpenAI - GPT-4", "Anthropic - Claude 3.5")
3. Show which provider is currently active
4. Indicate if provider configuration is valid/invalid
5. Provide visual distinction between provider types (OpenAI vs Anthropic)

**UI Elements:**
- List items with provider icon/name
- Active indicator (checkmark or badge)
- Validation status icon
- Edit button per item
- Delete button per item (with confirmation)

**Acceptance Criteria:**
- [ ] Provider list displays all configured instances
- [ ] Active provider is clearly marked
- [ ] Provider type is identifiable
- [ ] Invalid configurations show error indicator
- [ ] List is scrollable if many providers

### FR3: Add Provider Functionality
**Priority:** High
**Description:** Allow users to add new provider instances

**Requirements:**
1. "Add Provider" button in provider list screen
2. Provider type selection (OpenAI-compatible or Anthropic)
3. Configuration form based on provider type:
   - **OpenAI:** API key, model ID, base URL, timeout, Azure settings (if applicable)
   - **Anthropic:** API key, model ID, base URL, timeout
4. Provider name/label (optional, for user to identify the instance)
5. Validation before saving
6. Save to persistent storage

**UI Flow:**
```
Provider List -> Click "Add Provider" -> Select Type -> Configure Form -> Save
```

**Acceptance Criteria:**
- [ ] "Add Provider" button visible and functional
- [ ] Provider type selection dialog works
- [ ] Configuration form shows correct fields for selected type
- [ ] All required fields validated
- [ ] Provider saves successfully
- [ ] Returns to list with new provider shown

### FR4: Edit Provider Functionality
**Priority:** High
**Description:** Allow users to modify existing provider configurations

**Requirements:**
1. Edit button on each provider list item
2. Pre-populate form with current configuration
3. Allow modification of all fields
4. Validate changes before saving
5. Update persistent storage

**UI Flow:**
```
Provider List -> Click Edit on Provider -> Configure Form -> Save
```

**Acceptance Criteria:**
- [ ] Edit button opens configuration form
- [ ] Form shows current provider settings
- [ ] Changes validate correctly
- [ ] Save updates provider in list
- [ ] Cancel discards changes

### FR5: Delete Provider Functionality
**Priority:** Medium
**Description:** Allow users to remove provider configurations

**Requirements:**
1. Delete button on each provider list item (except active provider)
2. Confirmation dialog before deletion
3. Remove from persistent storage
4. Handle edge case: cannot delete active provider (must switch first)

**Acceptance Criteria:**
- [ ] Delete button shown for non-active providers
- [ ] Confirmation dialog appears
- [ ] Provider removed after confirmation
- [ ] Active provider cannot be deleted

### FR6: Set Active Provider
**Priority:** High
**Description:** Allow users to switch between configured providers

**Requirements:**
1. Tap on provider list item to set as active
2. Visual feedback when provider is activated
3. Update `SettingsStore.aiProviderType` and related settings
4. Persist selection to SharedPreferences

**Acceptance Criteria:**
- [ ] Tapping provider activates it
- [ ] Visual indicator updates immediately
- [ ] Active provider persists across app restarts
- [ ] AI operations use newly activated provider

### FR7: Data Model and Storage
**Priority:** High
**Description:** Support multiple provider instances in storage layer

**Requirements:**
1. Create `ProviderConfig` data class:
   ```kotlin
   data class ProviderConfig(
       val id: String, // Unique ID
       val name: String, // User-defined label
       val providerType: AIProvider,
       val openAISettings: OpenAISettings? = null,
       val anthropicSettings: AnthropicSettings? = null,
       val isActive: Boolean = false,
       val createdAt: Long,
       val updatedAt: Long
   )
   ```
2. Store provider list in SharedPreferences or Room database
3. Migrate existing single-provider settings to multi-provider format
4. Maintain backward compatibility during migration

**Acceptance Criteria:**
- [ ] `ProviderConfig` data class created
- [ ] Storage layer supports multiple providers
- [ ] Migration from old format works correctly
- [ ] Active provider loading works on app start

## Non-Functional Requirements

### NFR1: Performance
- Provider list should load within 100ms
- Configuration changes should save within 50ms
- No UI lag when switching providers

### NFR2: Usability
- Intuitive navigation flow
- Clear error messages for validation failures
- Consistent with existing Settings UI patterns

### NFR3: Reliability
- Data persistence across app restarts
- Graceful migration from old settings format
- No data loss during updates

### NFR4: Maintainability
- Follow existing project patterns (Compose UI, StateFlow, Repository pattern)
- Reuse existing `AISettings` and provider client implementations
- Minimal changes to `AIApi` and client classes

## Technical Constraints

1. **Must use existing architecture:**
   - Jetpack Compose for UI
   - StateFlow/Flow for state management
   - SharedPreferences or Room for storage
   - Repository pattern for data access

2. **Must maintain backward compatibility:**
   - Migrate existing settings to new format
   - No breaking changes to `AIApi` interface
   - Support both old and new settings formats during migration

3. **Must follow project conventions:**
   - Kotlin code style
   - Material Design 3 components
   - Existing error handling patterns

## Open Questions

1. **Storage approach:** SharedPreferences or Room database?
   - Recommendation: SharedPreferences (simpler, matches current pattern)

2. **Provider naming:** Auto-generate names or require user input?
   - Recommendation: Auto-generate with option to edit (e.g., "OpenAI Provider 1")

3. **Maximum providers:** Should we limit the number of providers?
   - Recommendation: No hard limit, but UI shows warning after 5 providers

4. **Migration behavior:** What if migration fails?
   - Recommendation: Keep old settings as fallback, show error to user

## Dependencies

- Existing `AIProvider` enum
- Existing `AISettings` sealed interface
- Existing `SettingsStore` class
- Existing `AIApi` class
- Existing `AIProviderSection` Compose UI

## Success Metrics

- Users can configure 3+ providers without issues
- Provider switching works seamlessly
- Migration from old format succeeds in 99%+ of cases
- Settings UI loads within 100ms

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Migration data loss | High | Comprehensive testing, backup old settings |
| UI complexity | Medium | Follow existing patterns, iterative design |
| Performance degradation | Low | Efficient data structures, lazy loading |
| User confusion | Medium | Clear UI labels, onboarding tooltips |

## Next Steps

1. User confirms requirements and priorities
2. Research best practices for multi-configuration management
3. Design architecture for provider list management
4. Create detailed technical specification
