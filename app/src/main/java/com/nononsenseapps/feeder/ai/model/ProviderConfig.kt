package com.nononsenseapps.feeder.ai.model

import com.nononsenseapps.feeder.ai.provider.AIProvider
import kotlinx.serialization.Serializable

/**
 * Configuration for a single AI provider instance.
 *
 * @property id Unique identifier for this provider instance
 * @property name User-defined label for this provider
 * @property providerType Type of provider (OpenAI or Anthropic)
 * @property openAISettings OpenAI-specific settings (null if not OpenAI)
 * @property anthropicSettings Anthropic-specific settings (null if not Anthropic)
 * @property deepLSettings DeepL-specific settings (null if not DeepL)
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
    val deepLSettings: DeepLSettings? = null,
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
                AISettings.OpenAI(openAISettings ?: OpenAISettings())
            AIProvider.ANTHROPIC ->
                AISettings.Anthropic(anthropicSettings ?: AnthropicSettings())
            AIProvider.DEEPL ->
                AISettings.DeepL(deepLSettings ?: DeepLSettings())
        }

    /**
     * Check if provider configuration is valid.
     */
    val isValid: Boolean
        get() = toAISettings().isValid

    /**
     * Get display name for UI.
     */
    fun getDisplayName(): String =
        name.ifBlank {
            when (providerType) {
                AIProvider.OPENAI_COMPATIBLE -> "OpenAI Provider"
                AIProvider.ANTHROPIC -> "Anthropic Provider"
                AIProvider.DEEPL -> "DeepL Provider"
            }
        }

    companion object {
        /**
         * Generate unique ID for new provider.
         */
        fun generateId(): String = "provider_${System.currentTimeMillis()}"

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
                is AISettings.DeepL ->
                    ProviderConfig(
                        id = generateId(),
                        name = name,
                        providerType = AIProvider.DEEPL,
                        deepLSettings = settings.deepLSettings,
                        isActive = isActive,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
            }
    }
}
