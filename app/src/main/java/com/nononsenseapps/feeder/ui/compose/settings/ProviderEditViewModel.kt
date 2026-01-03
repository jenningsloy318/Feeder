package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * UI state for the provider edit screen.
 *
 * @property provider The current provider configuration being edited
 * @property isNew Whether this is a new provider (true) or editing existing (false)
 * @property isSaving Whether a save operation is in progress
 * @property saveResult The result of the last save operation (null if no save attempted)
 */
data class ProviderEditState(
    val provider: ProviderConfig,
    val isNew: Boolean = false,
    val isSaving: Boolean = false,
    val saveResult: Result<Unit>? = null,
)

/**
 * UI state for provider edit screen with convenience properties.
 */
data class ProviderEditUiState(
    val provider: ProviderConfig,
    val isNewProvider: Boolean,
    val isSaving: Boolean,
    val isLoading: Boolean,
    val saveResult: Result<Unit>?,
) {
    val name: String
        get() = provider.name

    val providerType: AIProvider
        get() = provider.providerType

    val apiKey: String
        get() = provider.openAISettings?.key ?: provider.anthropicSettings?.key ?: ""

    val baseUrl: String
        get() = provider.openAISettings?.baseUrl ?: provider.anthropicSettings?.baseUrl ?: ""

    val modelId: String
        get() = provider.openAISettings?.modelId ?: provider.anthropicSettings?.modelId ?: ""

    val isActive: Boolean
        get() = provider.isActive
}

/**
 * ViewModel for editing a single AI provider configuration.
 *
 * This ViewModel handles both creating new providers and editing existing ones.
 * It manages the edit form state and saves changes to the repository.
 *
 * Parameters are passed via navigation arguments:
 * - providerId: ID of existing provider (null for new providers)
 * - providerType: Type of provider for new providers (null for editing)
 */
class ProviderEditViewModel(
    di: DI,
    savedState: SavedStateHandle,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    // Get parameters from navigation arguments
    private val providerId: String? = savedState["providerId"]
    private val providerType: AIProvider? =
        savedState.get<String>("providerType")?.let { typeString ->
            AIProvider.entries.find { it.name == typeString }
        }

    private val _internalState = MutableStateFlow<ProviderEditState>(
        if (providerId != null) {
            // Editing existing provider
            val existingProvider = run {
                // Synchronously get the provider from the current value
                val providers = repository.providers.value
                providers.find { it.id == providerId }
                    ?: createDefaultProvider()
            }
            ProviderEditState(
                provider = existingProvider,
                isNew = false,
                saveResult = null,
            )
        } else {
            // Creating new provider
            val newProvider = ProviderConfig.fromAISettings(
                settings = com.nononsenseapps.feeder.ai.model.AISettings.defaultForProvider(
                    providerType ?: AIProvider.OPENAI_COMPATIBLE,
                ),
                name = "",
                isActive = false,
            )
            ProviderEditState(
                provider = newProvider,
                isNew = true,
                saveResult = null,
            )
        },
    )

    val viewModelState: StateFlow<ProviderEditState> = _internalState.asStateFlow()

    /**
     * UI state for the screen.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProviderEditUiState> =
        object : StateFlow<ProviderEditUiState> {
            override val value: ProviderEditUiState
                get() = toUiState(viewModelState.value)

            override val replayCache: List<ProviderEditUiState>
                get() = listOf(value)

            override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<ProviderEditUiState>) =
                viewModelState.collect { state ->
                    collector.emit(toUiState(state))
                }
        }

    /**
     * Update the provider configuration.
     */
    fun updateProvider(provider: ProviderConfig) {
        _internalState.value = _internalState.value.copy(provider = provider)
    }

    /**
     * Update the provider name.
     */
    fun updateName(name: String) {
        val current = _internalState.value.provider
        updateProvider(current.copy(name = name))
    }

    /**
     * Update provider settings based on the provider type.
     */
    fun updateSettings(settings: com.nononsenseapps.feeder.ai.model.AISettings) {
        val current = _internalState.value.provider
        val updatedProvider = when (settings) {
            is com.nononsenseapps.feeder.ai.model.AISettings.OpenAI ->
                current.copy(openAISettings = settings.openaiSettings)
            is com.nononsenseapps.feeder.ai.model.AISettings.Anthropic ->
                current.copy(anthropicSettings = settings.anthropicSettings)
        }
        updateProvider(updatedProvider)
    }

    /**
     * Update the provider type.
     */
    fun updateProviderType(type: AIProvider) {
        val current = _internalState.value.provider
        val newSettings = com.nononsenseapps.feeder.ai.model.AISettings.defaultForProvider(type)
        val updatedProvider = when (newSettings) {
            is com.nononsenseapps.feeder.ai.model.AISettings.OpenAI ->
                current.copy(
                    providerType = type,
                    openAISettings = newSettings.openaiSettings,
                    anthropicSettings = null,
                )
            is com.nononsenseapps.feeder.ai.model.AISettings.Anthropic ->
                current.copy(
                    providerType = type,
                    openAISettings = null,
                    anthropicSettings = newSettings.anthropicSettings,
                )
        }
        updateProvider(updatedProvider)
    }

    /**
     * Update the API key.
     */
    fun updateApiKey(key: String) {
        val current = _internalState.value.provider
        val updatedProvider = when (current.providerType) {
            AIProvider.OPENAI_COMPATIBLE ->
                current.copy(
                    openAISettings = current.openAISettings?.copy(key = key)
                        ?: com.nononsenseapps.feeder.ai.model.OpenAISettings(key = key),
                )
            AIProvider.ANTHROPIC ->
                current.copy(
                    anthropicSettings = current.anthropicSettings?.copy(key = key)
                        ?: com.nononsenseapps.feeder.ai.model.AnthropicSettings(key = key),
                )
        }
        updateProvider(updatedProvider)
    }

    /**
     * Update the base URL.
     */
    fun updateBaseUrl(url: String) {
        val current = _internalState.value.provider
        val updatedProvider = when (current.providerType) {
            AIProvider.OPENAI_COMPATIBLE ->
                current.copy(
                    openAISettings = current.openAISettings?.copy(baseUrl = url)
                        ?: com.nononsenseapps.feeder.ai.model.OpenAISettings(baseUrl = url),
                )
            AIProvider.ANTHROPIC ->
                current.copy(
                    anthropicSettings = current.anthropicSettings?.copy(baseUrl = url)
                        ?: com.nononsenseapps.feeder.ai.model.AnthropicSettings(baseUrl = url),
                )
        }
        updateProvider(updatedProvider)
    }

    /**
     * Update the model ID.
     */
    fun updateModelId(modelId: String) {
        val current = _internalState.value.provider
        val updatedProvider = when (current.providerType) {
            AIProvider.OPENAI_COMPATIBLE ->
                current.copy(
                    openAISettings = current.openAISettings?.copy(modelId = modelId)
                        ?: com.nononsenseapps.feeder.ai.model.OpenAISettings(modelId = modelId),
                )
            AIProvider.ANTHROPIC ->
                current.copy(
                    anthropicSettings = current.anthropicSettings?.copy(modelId = modelId)
                        ?: com.nononsenseapps.feeder.ai.model.AnthropicSettings(modelId = modelId),
                )
        }
        updateProvider(updatedProvider)
    }

    /**
     * Update the active status (set as default).
     */
    fun updateIsActive(isActive: Boolean) {
        val current = _internalState.value.provider
        updateProvider(current.copy(isActive = isActive))
    }

    /**
     * Save the provider configuration.
     */
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
                    repository.addProvider(current)
                } else {
                    repository.updateProvider(current)
                }

                // If this provider is marked as active, activate it
                // This will deactivate all other providers
                if (current.isActive) {
                    repository.activateProvider(current.id)
                }

                _internalState.value = _internalState.value.copy(isSaving = false, saveResult = Result.success(Unit))
            } catch (e: com.nononsenseapps.feeder.archmodel.SettingsStore.DuplicateProviderNameException) {
                // Handle duplicate name exception with user-friendly message
                _internalState.value = _internalState.value.copy(
                    isSaving = false,
                    saveResult = Result.failure(
                        IllegalArgumentException(
                            "A provider with the name '${e.duplicateName}' already exists. Please choose a different name.",
                            e,
                        ),
                    ),
                )
            } catch (e: Exception) {
                _internalState.value = _internalState.value.copy(isSaving = false, saveResult = Result.failure(e))
            }
        }
    }

    /**
     * Clear save result.
     */
    fun clearSaveResult() {
        _internalState.value = _internalState.value.copy(saveResult = null)
    }

    private fun createDefaultProvider(): ProviderConfig =
        ProviderConfig.fromAISettings(
            settings = com.nononsenseapps.feeder.ai.model.AISettings.OpenAI(),
            name = "Unknown Provider",
            isActive = false,
        )

    private fun toUiState(state: ProviderEditState) = ProviderEditUiState(
        provider = state.provider,
        isNewProvider = state.isNew,
        isSaving = state.isSaving,
        isLoading = false,
        saveResult = state.saveResult,
    )
}
