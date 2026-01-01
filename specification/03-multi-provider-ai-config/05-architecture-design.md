# Architecture Design: Multi-Provider AI Configuration Management

**Created:** 2026-01-01 15:54:00+08:00
**Status:** Complete
**Current Date/Time:** 2026-01-01 15:54:00+08:00

## Overview

This document describes the architecture for supporting multiple AI provider instances. The design maintains backward compatibility while extending the system to manage multiple configured providers.

## Architecture Principles

1. **Backward Compatibility:** Existing single-provider code continues to work
2. **Gradual Migration:** Users migrate seamlessly from old to new format
3. **Minimal Changes:** Extend rather than replace existing components
4. **Type Safety:** Leverage sealed interfaces and data classes
5. **Reactive Design:** Use StateFlow for reactive updates

## Component Architecture

### 1. Data Model Layer

#### ProviderConfig Data Class

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/ProviderConfig.kt`

```kotlin
package com.nononsenseapps.feeder.ai.model

import kotlinx.serialization.Serializable
import com.nononsenseapps.feeder.ai.provider.AIProvider

/**
 * Configuration for a single AI provider instance.
 *
 * @property id Unique identifier for this provider instance
 * @property name User-defined label for this provider
 * @property providerType Type of provider (OpenAI or Anthropic)
 * @property openAISettings OpenAI-specific settings (null if not OpenAI)
 * @property anthropicSettings Anthropic-specific settings (null if not Anthropic)
 * @property isActive Whether this is the currently active provider
 * @property createdAt Timestamp when provider was created
 * @property updatedAt Timestamp when provider was last modified
 */
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
) {
    /**
     * Convert to AISettings sealed interface.
     */
    fun toAISettings(): AISettings =
        when (providerType) {
            AIProvider.OPENAI_COMPATIBLE ->
                AISettings.OpenAI(openaiSettings ?: OpenAISettings())
            AIProvider.ANTHROPIC ->
                AISettings.Anthropic(anthropicSettings ?: AnthropicSettings())
        }

    /**
     * Check if provider configuration is valid.
     */
    val isValid: Boolean
        get() = toAISettings().isValid

    /**
     * Get display name for UI.
     */
    fun getDisplayName(): String = name.ifBlank {
        when (providerType) {
            AIProvider.OPENAI_COMPATIBLE -> "OpenAI Provider"
            AIProvider.ANTHROPIC -> "Anthropic Provider"
        }
    }

    companion object {
        /**
         * Generate unique ID for new provider.
         */
        fun generateId(): String =
            "provider_${System.currentTimeMillis()}"

        /**
         * Create ProviderConfig from AISettings.
         */
        fun fromAISettings(
            settings: AISettings,
            name: String = "",
            isActive: Boolean = false,
        ): ProviderConfig =
            when (settings) {
                is AISettings.OpenAI ->
                    ProviderConfig(
                        id = generateId(),
                        name = name,
                        providerType = AIProvider.OPENAI_COMPATIBLE,
                        openAISettings = settings.openaiSettings,
                        isActive = isActive,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                is AISettings.Anthropic ->
                    ProviderConfig(
                        id = generateId(),
                        name = name,
                        providerType = AIProvider.ANTHROPIC,
                        anthropicSettings = settings.anthropicSettings,
                        isActive = isActive,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
            }
    }
}
```

**Design Decisions:**
- `id`: String (not Long) for simplicity and uniqueness
- `name`: User-defined label, can be blank (fallback to type-based name)
- `openAISettings`/`anthropicSettings`: Nullable based on provider type
- `isActive`: Only one provider should be active at a time
- `createdAt`/`updatedAt`: For audit trail and sorting
- `@Serializable`: For JSON serialization with kotlinx.serialization

### 2. Storage Layer

#### Extended SettingsStore

**Location:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Additions to existing SettingsStore:**

```kotlin
class SettingsStore(di: DI) {
    // EXISTING CODE (keep for backward compatibility)
    private val _aiProviderType = MutableStateFlow(...)
    private val _openAISettings = MutableStateFlow(...)
    private val _anthropicSettings = MutableStateFlow(...)
    val aiSettings: AISettings get() = ...

    // NEW: Multi-provider support
    private val _providers = MutableStateFlow(emptyList<ProviderConfig>())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        // Migrate and load providers on initialization
        _providers.value = loadProviders()
    }

    // NEW: Provider list management
    fun loadProviders(): List<ProviderConfig> {
        // Check if already migrated
        val jsonString = sp.getString(KEY_PROVIDER_LIST, null)
        if (jsonString != null) {
            return try {
                json.decodeFromString<List<ProviderConfig>>(jsonString)
            } catch (e: Exception) {
                // If parsing fails, migrate from old settings
                migrateFromOldSettings()
            }
        }

        // Not migrated yet, migrate from old settings
        return migrateFromOldSettings()
    }

    private fun migrateFromOldSettings(): List<ProviderConfig> {
        val providers = mutableListOf<ProviderConfig>()
        val activeProviderType = _aiProviderType.value

        // Migrate OpenAI settings if present
        val oldOpenAIKey = sp.getString(PREF_OPENAI_KEY, "")
        if (!oldOpenAIKey.isNullOrBlank()) {
            providers.add(
                ProviderConfig(
                    id = "migrated_openai_${System.currentTimeMillis()}",
                    name = "OpenAI Provider (Migrated)",
                    providerType = AIProvider.OPENAI_COMPATIBLE,
                    openAISettings = OpenAISettings(
                        key = oldOpenAIKey,
                        modelId = sp.getString(PREF_OPENAI_MODEL_ID, OpenAISettings.DEFAULT_MODEL),
                        baseUrl = sp.getString(PREF_OPENAI_URL, ""),
                        timeoutSeconds = sp.getInt(PREF_OPENAI_REQUEST_TIMEOUT_SECONDS, 30),
                        azureApiVersion = sp.getString(PREF_OPENAI_AZURE_VERSION, ""),
                        azureDeploymentId = sp.getString(PREF_OPENAI_AZURE_DEPLOYMENT_ID, ""),
                    ),
                    isActive = activeProviderType == AIProvider.OPENAI_COMPATIBLE,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        // Migrate Anthropic settings if present
        val oldAnthropicKey = sp.getString(PREF_ANTHROPIC_KEY, "")
        if (!oldAnthropicKey.isNullOrBlank()) {
            providers.add(
                ProviderConfig(
                    id = "migrated_anthropic_${System.currentTimeMillis()}",
                    name = "Anthropic Provider (Migrated)",
                    providerType = AIProvider.ANTHROPIC,
                    anthropicSettings = AnthropicSettings(
                        key = oldAnthropicKey,
                        modelId = sp.getString(PREF_ANTHROPIC_MODEL_ID, AnthropicSettings.DEFAULT_MODEL),
                        baseUrl = sp.getString(PREF_ANTHROPIC_URL, ""),
                        timeoutSeconds = sp.getInt(PREF_ANTHROPIC_REQUEST_TIMEOUT_SECONDS, 30),
                    ),
                    isActive = activeProviderType == AIProvider.ANTHROPIC,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        // Ensure exactly one provider is active
        if (providers.none { it.isActive } && providers.isNotEmpty()) {
            providers = providers.mapIndexed { index, provider ->
                provider.copy(isActive = index == 0)
            }.toMutableList()
        }

        // Save migrated providers
        if (providers.isNotEmpty()) {
            saveProviders(providers)
        }

        return providers
    }

    fun saveProviders(providers: List<ProviderConfig>) {
        _providers.value = providers
        val jsonString = json.encodeToString(providers)
        sp.edit().putString(KEY_PROVIDER_LIST, jsonString).apply()
    }

    fun addProvider(provider: ProviderConfig) {
        val updated = _providers.value + provider
        saveProviders(updated)
    }

    fun updateProvider(provider: ProviderConfig) {
        val updated = _providers.value.map {
            if (it.id == provider.id) provider else it
        }
        saveProviders(updated)
    }

    fun deleteProvider(id: String) {
        val updated = _providers.value.filter { it.id != id }
        // Ensure at least one provider remains active
        if (updated.none { it.isActive } && updated.isNotEmpty()) {
            updated = updated.mapIndexed { index, provider ->
                provider.copy(isActive = index == 0)
            }
        }
        saveProviders(updated)
    }

    fun activateProvider(id: String) {
        val updated = _providers.value.map {
            it.copy(isActive = it.id == id)
        }
        saveProviders(updated)
    }

    // UPDATED: aiSettings now uses provider list
    override val aiSettings: AISettings
        get() {
            // Try new multi-provider format first
            val activeProvider = _providers.value.firstOrNull { it.isActive }
            if (activeProvider != null) {
                return activeProvider.toAISettings()
            }

            // Fallback to old single-provider format
            return when (_aiProviderType.value) {
                AIProvider.OPENAI_COMPATIBLE -> AISettings.OpenAI(_openAISettings.value)
                AIProvider.ANTHROPIC -> AISettings.Anthropic(_anthropicSettings.value)
            }
        }

    // UPDATED: aiSettingsFlow now uses provider list
    override val aiSettingsFlow: StateFlow<AISettings>
        get() = _providers
            .map { providers ->
                providers.firstOrNull { it.isActive }?.toAISettings()
                    ?: fallbackToOldSettings()
            }
            .stateIn(
                scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
                started = SharingStarted.Eagerly,
                initialValue = aiSettings,
            )

    private fun fallbackToOldSettings(): AISettings =
        when (_aiProviderType.value) {
            AIProvider.OPENAI_COMPATIBLE -> AISettings.OpenAI(_openAISettings.value)
            AIProvider.ANTHROPIC -> AISettings.Anthropic(_anthropicSettings.value)
        }

    companion object {
        // EXISTING keys...
        private const val PREF_AI_PROVIDER_TYPE = "ai_provider_type"
        private const val PREF_OPENAI_KEY = "openai_key"
        // etc.

        // NEW key for provider list
        private const val KEY_PROVIDER_LIST = "ai_provider_list"
    }
}
```

**Design Decisions:**
- Keep existing single-provider fields for backward compatibility
- Provider list stored as JSON in SharedPreferences
- Migration runs automatically on first load
- `aiSettings` property checks provider list first, falls back to old format
- Ensure exactly one provider is active at all times

### 3. Repository Layer

#### Extended Repository

**Location:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

**Additions to existing Repository:**

```kotlin
class Repository(
    private val settingsStore: SettingsStore
) {
    // EXISTING CODE
    val aiProviderType = settingsStore.aiProviderType
    val openAISettings = settingsStore.openAISettings
    val anthropicSettings = settingsStore.anthropicSettings
    val aiSettings = settingsStore.aiSettings
    val aiSettingsFlow = settingsStore.aiSettingsFlow

    fun setAIProviderType(value: AIProvider) = settingsStore.setAIProviderType(value)
    fun setOpenAISettings(value: OpenAISettings) = settingsStore.setOpenAISettings(value)
    fun setAnthropicSettings(value: AnthropicSettings) = settingsStore.setAnthropicSettings(value)

    // NEW: Multi-provider support
    val providers: StateFlow<List<ProviderConfig>>
        get() = settingsStore.providers

    fun addProvider(provider: ProviderConfig) = settingsStore.addProvider(provider)
    fun updateProvider(provider: ProviderConfig) = settingsStore.updateProvider(provider)
    fun deleteProvider(id: String) = settingsStore.deleteProvider(id)
    fun activateProvider(id: String) = settingsStore.activateProvider(id)

    // Get active provider as Flow
    val activeProvider: StateFlow<ProviderConfig?>
        get() = settingsStore.providers.map { providers ->
            providers.firstOrNull { it.isActive }
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
```

**Design Decisions:**
- Thin wrapper pattern maintained
- No business logic in Repository
- All logic in SettingsStore
- Expose active provider as Flow for reactive UI

### 4. ViewModel Layer

#### ProviderListViewModel

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListViewModel.kt`

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProviderListEvent {
    object NavigateToAdd : ProviderListEvent
    data class NavigateToEdit(val providerId: String) : ProviderListEvent
    data class DeleteProvider(val providerId: String) : ProviderListEvent
    data class ActivateProvider(val providerId: String) : ProviderListEvent
    object DismissConfirmation : ProviderListEvent
}

data class ProviderListState(
    val providers: List<ProviderConfig> = emptyList(),
    val providerToDelete: ProviderConfig? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ProviderListViewModel(
    private val repository: Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderListState())
    val uiState: StateFlow<ProviderListState> = _uiState.asStateFlow()

    init {
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            repository.providers.collect { providers ->
                _uiState.value = _uiState.value.copy(
                    providers = providers,
                    isLoading = false,
                )
            }
        }
    }

    fun onEvent(event: ProviderListEvent) {
        when (event) {
            is ProviderListEvent.DeleteProvider -> {
                val provider = _uiState.value.providers.find { it.id == event.providerId }
                if (provider != null && !provider.isActive) {
                    _uiState.value = _uiState.value.copy(providerToDelete = provider)
                }
            }
            is ProviderListEvent.ActivateProvider -> {
                repository.activateProvider(event.providerId)
            }
            is ProviderListEvent.DismissConfirmation -> {
                _uiState.value = _uiState.value.copy(providerToDelete = null)
            }
            is ProviderListEvent.NavigateToAdd,
            is ProviderListEvent.NavigateToEdit -> {
                // Handled by navigation
            }
        }
    }

    fun confirmDelete() {
        val provider = _uiState.value.providerToDelete ?: return
        repository.deleteProvider(provider.id)
        _uiState.value = _uiState.value.copy(providerToDelete = null)
    }
}
```

**Design Decisions:**
- Event-based state management (existing pattern)
- Separate confirmation for delete (safety)
- Cannot delete active provider (enforced in UI)

#### ProviderEditViewModel

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProviderEditEvent {
    data class SetName(val name: String) : ProviderEditEvent
    data class SetProviderType(val type: AIProvider) : ProviderEditEvent
    data class UpdateOpenAISettings(val settings: OpenAISettings) : ProviderEditEvent
    data class UpdateAnthropicSettings(val settings: AnthropicSettings) : ProviderEditEvent
    object Save : ProviderEditEvent
    object Cancel : ProviderEditEvent
}

data class ProviderEditState(
    val providerId: String? = null, // null = adding new provider
    val name: String = "",
    val providerType: AIProvider = AIProvider.OPENAI_COMPATIBLE,
    val openaiSettings: OpenAISettings = OpenAISettings(),
    val anthropicSettings: AnthropicSettings = AnthropicSettings(),
    val isSaving: Boolean = false,
    val error: String? = null,
)

class ProviderEditViewModel(
    private val repository: Repository,
    private val providerId: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderEditState())
    val uiState: StateFlow<ProviderEditState> = _uiState.asStateFlow()

    init {
        if (providerId != null) {
            loadProvider(providerId)
        }
    }

    private fun loadProvider(id: String) {
        viewModelScope.launch {
            repository.providers.collect { providers ->
                val provider = providers.find { it.id == id }
                if (provider != null) {
                    _uiState.value = ProviderEditState(
                        providerId = provider.id,
                        name = provider.name,
                        providerType = provider.providerType,
                        openaiSettings = provider.openAISettings ?: OpenAISettings(),
                        anthropicSettings = provider.anthropicSettings ?: AnthropicSettings(),
                    )
                }
            }
        }
    }

    fun onEvent(event: ProviderEditEvent) {
        when (event) {
            is ProviderEditEvent.SetName -> {
                _uiState.value = _uiState.value.copy(name = event.name)
            }
            is ProviderEditEvent.SetProviderType -> {
                _uiState.value = _uiState.value.copy(providerType = event.type)
            }
            is ProviderEditEvent.UpdateOpenAISettings -> {
                _uiState.value = _uiState.value.copy(openaiSettings = event.settings)
            }
            is ProviderEditEvent.UpdateAnthropicSettings -> {
                _uiState.value = _uiState.value.copy(anthropicSettings = event.settings)
            }
            is ProviderEditEvent.Save -> {
                saveProvider()
            }
            is ProviderEditEvent.Cancel -> {
                // Handled by navigation
            }
        }
    }

    private fun saveProvider() {
        val state = _uiState.value

        // Validate
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Provider name is required")
            return
        }

        val settings = when (state.providerType) {
            AIProvider.OPENAI_COMPATIBLE -> state.openaiSettings
            AIProvider.ANTHROPIC -> state.anthropicSettings
        }

        if (!settings.isValid) {
            _uiState.value = state.copy(error = "Invalid settings")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val now = System.currentTimeMillis()

            if (state.providerId == null) {
                // Adding new provider
                val newProvider = ProviderConfig(
                    id = ProviderConfig.generateId(),
                    name = state.name,
                    providerType = state.providerType,
                    openAISettings = if (state.providerType == AIProvider.OPENAI_COMPATIBLE) state.openaiSettings else null,
                    anthropicSettings = if (state.providerType == AIProvider.ANTHROPIC) state.anthropicSettings else null,
                    isActive = false, // New providers are not active by default
                    createdAt = now,
                    updatedAt = now,
                )
                repository.addProvider(newProvider)
            } else {
                // Updating existing provider
                val existingProvider = repository.providers.value.find { it.id == state.providerId }
                if (existingProvider != null) {
                    val updatedProvider = existingProvider.copy(
                        name = state.name,
                        providerType = state.providerType,
                        openAISettings = if (state.providerType == AIProvider.OPENAI_COMPATIBLE) state.openaiSettings else null,
                        anthropicSettings = if (state.providerType == AIProvider.ANTHROPIC) state.anthropicSettings else null,
                        updatedAt = now,
                    )
                    repository.updateProvider(updatedProvider)
                }
            }

            _uiState.value = state.copy(isSaving = false)
        }
    }
}
```

**Design Decisions:**
- Reuse existing OpenAISettings/AnthropicSettings validation
- Single ViewModel for both add and edit modes
- Validate before saving
- New providers are not active by default

### 5. Navigation Architecture

#### Navigation Routes

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

```kotlin
// NEW: Provider list destination
object ProviderListDestination : Destination {
    override val route = "provider_list"
    override val title: String
        @Composable
        get() = stringResource(R.string.provider_list_title)

    @Composable
    override fun RegisterScreen(
        navController: NavController,
        backStackEntry: NavBackStackEntry,
        navDrawerListState: LazyListState,
    ) {
        ProviderListScreen(
            onNavigateUp = {
                if (!navController.popBackStack()) {
                    SettingsScreenDestination.navigate(navController)
                }
            },
            onNavigateToAdd = {
                ProviderEditDestination.navigate(navController, null)
            },
            onNavigateToEdit = { providerId ->
                ProviderEditDestination.navigate(navController, providerId)
            },
            viewModel = backStackEntry.diAwareViewModel(),
        )
    }

    fun navigate(navController: NavController) {
        navController.navigate(route)
    }
}

// NEW: Provider edit destination
object ProviderEditDestination : Destination {
    private const val PROVIDER_ID_ARG = "provider_id"

    override val route = "provider_edit?$PROVIDER_ID_ARG={$PROVIDER_ID_ARG}"
    override val title: String
        @Composable
        get() = stringResource(R.string.provider_edit_title)

    @Composable
    override fun RegisterScreen(
        navController: NavController,
        backStackEntry: NavBackStackEntry,
        navDrawerListState: LazyListState,
    ) {
        val providerId: String? = backStackEntry.arguments?.getString(PROVIDER_ID_ARG)

        ProviderEditScreen(
            providerId = providerId,
            onNavigateUp = {
                if (!navController.popBackStack()) {
                    ProviderListDestination.navigate(navController)
                }
            },
            viewModel = backStackEntry.diAwareViewModel { ProviderEditViewModel(instance(), providerId) },
        )
    }

    fun navigate(navController: NavController, providerId: String?) {
        navController.navigate(
            route.replace(
                oldValue = "{$PROVIDER_ID_ARG}",
                newValue = providerId ?: "",
            ),
        )
    }
}
```

**Design Decisions:**
- Optional provider_id argument for edit mode
- Null provider_id = add mode
- Navigate back to list after save

### 6. Data Flow Diagrams

#### Add Provider Flow

```
UI: ProviderListScreen
  ↓ Click "Add Provider"
Navigation: Navigate to ProviderEditScreen
  ↓
UI: ProviderEditScreen (add mode)
  ↓ Fill form, click "Save"
ViewModel: ProviderEditViewModel.onEvent(Save)
  ↓ Validate
ViewModel: ProviderEditViewModel.saveProvider()
  ↓
Repository: Repository.addProvider()
  ↓
SettingsStore: SettingsStore.addProvider()
  ↓
Storage: SharedPreferences (JSON)
  ↓
StateFlow: _providers updated
  ↓
UI: ProviderListScreen (auto-updates)
```

#### Edit Provider Flow

```
UI: ProviderListScreen
  ↓ Click "Edit" on provider
Navigation: Navigate to ProviderEditScreen with providerId
  ↓
UI: ProviderEditScreen (edit mode)
  ↓ ViewModel loads provider
ViewModel: ProviderEditViewModel.loadProvider()
  ↓
Repository: Repository.providers
  ↓
UI: Form pre-populated with current settings
  ↓ Modify settings, click "Save"
ViewModel: ProviderEditViewModel.onEvent(Save)
  ↓ Validate
ViewModel: ProviderEditViewModel.saveProvider()
  ↓
Repository: Repository.updateProvider()
  ↓
SettingsStore: SettingsStore.updateProvider()
  ↓
Storage: SharedPreferences (JSON)
  ↓
StateFlow: _providers updated
  ↓
UI: ProviderListScreen (auto-updates)
```

#### Activate Provider Flow

```
UI: ProviderListScreen
  ↓ Tap on provider item
ViewModel: ProviderListViewModel.onEvent(ActivateProvider)
  ↓
Repository: Repository.activateProvider(id)
  ↓
SettingsStore: SettingsStore.activateProvider(id)
  ↓ Update: Set isActive=false for all, isActive=true for selected
  ↓
Storage: SharedPreferences (JSON)
  ↓
StateFlow: _providers updated
  ↓
StateFlow: aiSettingsFlow updated
  ↓
AIApi: Automatically uses new active provider
```

### 7. Migration Architecture

#### Migration States

```kotlin
enum class MigrationState {
    NOT_MIGRATED,    // Old format only
    IN_PROGRESS,     // Migration running
    MIGRATED,        // New format with migrated data
    NEW_USER,        // No existing data
}
```

#### Migration Flow

```
App Start
  ↓
SettingsStore.init()
  ↓
Check SharedPreferences for KEY_PROVIDER_LIST
  ↓
┌─────────────────┬──────────────────┐
│  Exists         │  Does not exist  │
│  (New format)   │  (Old format)    │
↓                 ↓                  ↓
Parse JSON        Migrate from old   Skip (new user)
↓                 settings
Success?          ↓
┌────┬────┐       ┌─────────────────┐
│ Yes │ No │       │ Create          │
↓     ↓         │ ProviderConfig   │
Load  Migrate   │ from old prefs  │
↓     ↓         ↓                 ↓
Done  Done      Save providers    Done
```

#### Rollback Strategy

If migration fails:
1. Log error
2. Keep old SharedPreferences keys intact
3. Continue using old format
4. User can retry migration later

### 8. Error Handling Architecture

#### Error Types

```kotlin
sealed interface ProviderError {
    data class ValidationError(val message: String) : ProviderError
    data class StorageError(val cause: Throwable) : ProviderError
    data class MigrationError(val cause: Throwable) : ProviderError
    data class NetworkError(val message: String) : ProviderError
}
```

#### Error Handling Strategy

1. **Validation Errors:** Show in UI, prevent save
2. **Storage Errors:** Log, show error message, retry
3. **Migration Errors:** Log, fallback to old format
4. **Network Errors:** Show in UI, allow retry

### 9. Testing Strategy

#### Unit Tests

- `ProviderConfig` data class
- Migration logic
- Validation logic
- SettingsStore provider list methods

#### Integration Tests

- SettingsStore + Repository
- Repository + ViewModels
- Migration flow

#### UI Tests

- ProviderListScreen interactions
- ProviderEditScreen form validation
- Navigation flow

### 10. Performance Considerations

#### Optimization Strategies

1. **Lazy Loading:** Only load providers when needed
2. **Efficient Serialization:** Use kotlinx.serialization (fast)
3. **StateFlow Caching:** Share single StateFlow instance
4. **Minimal Updates:** Only update changed providers

#### Expected Performance

- Provider list load: < 100ms
- Add/update provider: < 50ms
- Activate provider: < 50ms
- Migration: < 200ms (one-time)

## Architecture Decision Records

### ADR-001: SharedPreferences vs Room

**Decision:** Use SharedPreferences with JSON serialization

**Rationale:**
- Matches existing pattern (SettingsStore already uses SharedPreferences)
- Simpler than Room for small datasets (< 100 providers)
- No database migration needed
- Faster implementation
- Sufficient performance for provider list

**Alternatives Considered:**
- Room database: More powerful but overkill for this use case
- DataStore: Newer technology, less mature

### ADR-002: Sealed Interface vs Class Hierarchy

**Decision:** Keep existing sealed interface (AISettings)

**Rationale:**
- Already implemented and working
- Type-safe with exhaustive when
- No changes needed

### ADR-003: Single vs Multiple ViewModels

**Decision:** Separate ViewModels for list and edit

**Rationale:**
- Clear separation of concerns
- List ViewModel manages list state
- Edit ViewModel manages form state
- Follows existing pattern (SettingsViewModel)

### ADR-004: Navigation Strategy

**Decision:** Use Compose Navigation with routes

**Rationale:**
- Matches existing navigation pattern
- Type-safe with destination objects
- Easy to test
- Well-integrated with Compose

## Next Steps

1. ✅ Architecture design complete
2. ⏭️ UI/UX Design (Phase 5.5) - Design provider list interface
3. ⏭️ Specification Writing (Phase 6) - Create implementation plan

## Conclusion

The architecture design extends the existing system with minimal changes while adding full multi-provider support. The design maintains backward compatibility, uses existing patterns, and follows best practices for Android/Kotlin development.

Key architectural decisions:
- SharedPreferences with JSON for storage
- Sealed interfaces for type safety
- StateFlow for reactive updates
- Separate ViewModels for list and edit
- Navigation-based routing

The architecture is ready for implementation.
