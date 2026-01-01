# Multi-Provider AI Architecture

## Overview

This document describes the multi-provider AI architecture implemented in Feeder, which allows users to configure and manage multiple AI providers (OpenAI-compatible and Anthropic) for AI-powered features like feed summarization.

## Architecture Components

### 1. Data Layer

#### ProviderConfig
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/model/ProviderConfig.kt`
- **Purpose**: Data class representing a single AI provider configuration
- **Fields**:
  - `id`: Unique identifier for the provider
  - `name`: Display name for the provider
  - `providerType`: Type (OPENAI_COMPATIBLE or ANTHROPIC)
  - `openAISettings`: OpenAI-specific settings (nullable)
  - `anthropicSettings`: Anthropic-specific settings (nullable)
  - `isActive`: Whether this provider is currently active
  - `createdAt`: Timestamp when provider was created

#### SettingsStore Integration
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- **Key Methods**:
  - `providers: StateFlow<List<ProviderConfig>>`: Stream of all providers
  - `addProvider(provider: ProviderConfig)`: Add a new provider
  - `updateProvider(provider: ProviderConfig)`: Update existing provider
  - `deleteProvider(id: String)`: Delete a provider
  - `activateProvider(id: String)`: Set a provider as active
  - `saveProviders(providers: List<ProviderConfig>)`: Persist provider list

#### Migration
- **Method**: `migrateFromOldSettings()`
- **Purpose**: Automatically migrates legacy single-provider settings to multi-provider format
- **Behavior**:
  - Reads old OpenAI/Anthropic settings
  - Creates a default provider named "Default"
  - Preserves all existing credentials and settings
  - One-time migration on first load

### 2. Business Logic Layer

#### Repository
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
- **Provider Methods**:
  ```kotlin
  val providers: StateFlow<List<ProviderConfig>>
  fun addProvider(provider: ProviderConfig)
  fun updateProvider(provider: ProviderConfig)
  fun deleteProvider(id: String)
  fun activateProvider(id: String)
  ```

### 3. ViewModels

#### ProviderListViewModel
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListViewModel.kt`
- **Responsibilities**:
  - Expose list of all providers
  - Handle provider activation
  - Handle provider deletion
- **State**: `StateFlow<List<ProviderConfig>>`

#### ProviderEditViewModel
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
- **Responsibilities**:
  - Manage provider creation/editing
  - Form validation
  - Save operations with error handling
  - Provider type switching with default configuration
- **State**: `ProviderEditUiState` (includes provider fields, loading state, save result)

### 4. UI Layer

#### ProviderListScreen
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListScreen.kt`
- **Features**:
  - LazyColumn with swipe-to-delete
  - Empty state with helpful message
  - Active provider indicator
  - FAB to add new provider
  - Loading states
  - Delete confirmation dialog

#### ProviderEditScreen
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
- **Features**:
  - Provider name input (required)
  - Provider type dropdown (OpenAI/Anthropic)
  - API key input (required)
  - Base URL input (with provider-specific defaults)
  - Model ID input
  - Save/Cancel buttons
  - Form validation with error messages
  - Snackbar notifications

#### Settings Integration
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
- **Changes**:
  - "API key" link changed to "Providers"
  - Shows provider configuration status
  - Navigates to ProviderListScreen

### 5. Navigation

#### NavigationDestinations
- **Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
- **New Routes**:
  ```kotlin
  object ProviderListDestination : NavigationDestination(...)
  object ProviderEditDestination : NavigationDestination(...)
  ```
- **Query Parameters**: `providerId` for edit mode (null = create new)

### 6. String Resources

#### Provider Management Strings
- **Location**: `app/src/main/res/values/strings.xml`
- **Keys**:
  - `provider_list_title`: "AI Providers"
  - `add_provider`: "Add provider"
  - `edit_provider`: "Edit provider"
  - `delete_provider`: "Delete provider"
  - `no_providers_configured`: "No providers configured"
  - `provider_configured`: "Providers configured"
  - `provider_name`: "Provider name"
  - `provider_saved`: "Provider saved successfully"
  - And more...

## Data Flow

### Creating a New Provider
1. User clicks "Add provider" FAB in ProviderListScreen
2. Navigation to ProviderEditScreen with `providerId = null`
3. User fills form (name, type, API key, base URL, model ID)
4. User clicks "Save"
5. ProviderEditViewModel validates form
6. If valid, calls `repository.addProvider()`
7. Repository calls `settingsStore.addProvider()`
8. SettingsStore persists to SharedPreferences
9. Success message shown via Snackbar
10. Navigation back to provider list

### Editing an Existing Provider
1. User clicks provider in ProviderListScreen
2. Navigation to ProviderEditScreen with `providerId`
3. ProviderEditViewModel loads provider from repository
4. User modifies fields
5. User clicks "Save"
6. ProviderEditViewModel validates form
7. If valid, calls `repository.updateProvider()`
8. Repository calls `settingsStore.updateProvider()`
9. SettingsStore persists to SharedPreferences
10. Success message shown via Snackbar
11. Navigation back to provider list

### Deleting a Provider
1. User swipes provider item in ProviderListScreen
2. Delete confirmation dialog appears
3. User confirms deletion
4. ProviderListViewModel calls `repository.deleteProvider()`
5. Repository calls `settingsStore.deleteProvider()`
6. SettingsStore removes from SharedPreferences
7. Provider list updates automatically

### Activating a Provider
1. User clicks provider item in ProviderListScreen (opens edit)
2. User can activate provider (future feature: quick toggle)
3. ProviderListViewModel calls `repository.activateProvider()`
4. Repository calls `settingsStore.activateProvider()`
5. SettingsStore sets `isActive = true` for selected provider
6. All other providers set to `isActive = false`

## Migration Path

### Legacy Settings → Multi-Provider

**Before (Single Provider)**:
```kotlin
openai_key = "sk-..."
openai_base_url = "https://api.openai.com/v1"
openai_model_id = "gpt-4"
```

**After (Multi-Provider)**:
```kotlin
providers = [
  {
    id: "default-migrated",
    name: "Default",
    providerType: "openai_compatible",
    openAISettings: {
      key: "sk-...",
      baseUrl: "https://api.openai.com/v1",
      modelId: "gpt-4"
    },
    isActive: true
  }
]
```

## Testing

### Manual Testing Checklist

#### Provider List Screen
- [ ] Screen loads with existing providers
- [ ] Empty state shows when no providers configured
- [ ] Active provider indicator displays correctly
- [ ] Swipe-to-delete works
- [ ] Delete confirmation dialog appears
- [ ] Cancel delete dismisses dialog
- [ ] Confirm delete removes provider
- [ ] FAB navigates to add provider screen
- [ ] Tap on provider navigates to edit screen

#### Provider Edit Screen (Create)
- [ ] Screen loads with empty form
- [ ] Provider type dropdown works
- [ ] Form validation works (all required fields)
- [ ] Save button disabled when form invalid
- [ ] Save button enabled when form valid
- [ ] Save creates new provider
- [ ] Success snackbar appears
- [ ] Navigation back to list after save
- [ ] Cancel button navigates back without saving

#### Provider Edit Screen (Edit)
- [ ] Screen loads with existing provider data
- [ ] All fields pre-populated correctly
- [ ] Form updates provider on save
- [ ] Success snackbar appears
- [ ] Changes visible in provider list

#### Settings Integration
- [ ] "Providers" link visible in settings
- [ ] Shows "Providers configured" when providers exist
- [ ] Shows "No providers configured" when none
- [ ] Tapping navigates to provider list

#### Migration
- [ ] Legacy settings migrate correctly
- [ ] Migrated provider works as expected
- [ ] Migration only runs once

## Future Enhancements

### Potential Features
1. **Quick Toggle**: Add quick activate/deactivate toggle in provider list
2. **Provider Testing**: Test provider connectivity before saving
3. **Provider Duplication**: Copy existing provider configuration
4. **Import/Export**: Share provider configurations
5. **Usage Statistics**: Track provider usage and costs
6. **Provider Templates**: Pre-configured popular providers

## Troubleshooting

### Common Issues

#### Migration Issues
- **Problem**: Migration doesn't run
- **Solution**: Clear app data and reconfigure providers
- **Check**: SharedPreferences for existing provider list

#### Save Failures
- **Problem**: Provider doesn't save
- **Solution**: Check form validation, ensure all required fields filled
- **Check**: Repository logs for errors

#### Navigation Issues
- **Problem**: Can't navigate to provider screens
- **Solution**: Verify routes registered in NavigationDestinations
- **Check**: MainActivity NavHost includes provider destinations

## Files Modified/Created

### Created Files
1. `app/src/main/java/com/nononsenseapps/feeder/ai/model/ProviderConfig.kt`
2. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListViewModel.kt`
3. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
4. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListScreen.kt`
5. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`

### Modified Files
1. `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
2. `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
3. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
4. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
5. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`
6. `app/src/main/res/values/strings.xml`

## Conclusion

This multi-provider architecture provides a flexible, scalable foundation for AI integration in Feeder. It maintains backward compatibility through automatic migration while enabling users to configure multiple AI providers for different use cases.
