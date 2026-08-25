package com.nononsenseapps.feeder.ai.model

import com.nononsenseapps.feeder.ai.provider.AIProvider
import kotlinx.serialization.Serializable

/**
 * Settings for OpenAI-compatible provider.
 *
 * @property key API key for OpenAI-compatible endpoint
 * @property modelId Model identifier (e.g., "gpt-4o-mini")
 * @property baseUrl Custom base URL for API requests (empty for default OpenAI endpoint)
 * @property timeoutSeconds Request timeout in seconds (30-600 range, default 90)
 * @property maxTokens Maximum tokens for response (1-128000 range, null for default)
 * @property azureApiVersion Azure API version (Azure OpenAI only)
 * @property azureDeploymentId Azure deployment ID (Azure OpenAI only)
 */
@Serializable
data class OpenAISettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
    val maxTokens: Int? = null,
    val azureApiVersion: String = "",
    val azureDeploymentId: String = "",
) {
    /**
     * Check if the configured base URL is an Azure OpenAI endpoint.
     */
    val isAzure: Boolean
        get() = baseUrl.contains("openai.azure.com", ignoreCase = true)

    /**
     * Check if the configured base URL is a Perplexity endpoint.
     * Perplexity doesn't support model listing.
     */
    val isPerplexity: Boolean
        get() = baseUrl.contains("api.perplexity.ai", ignoreCase = true)

    /**
     * Check if settings are valid.
     */
    val isValid: Boolean
        get() =
            modelId.isNotEmpty() &&
                key.isNotEmpty() &&
                if (isAzure) {
                    azureApiVersion.isNotBlank() && azureDeploymentId.isNotBlank()
                } else {
                    true
                }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}

/**
 * Settings for Anthropic provider.
 *
 * @property key API key for Anthropic
 * @property modelId Model identifier (e.g., "claude-3-5-sonnet-20241022")
 * @property baseUrl Custom base URL for API requests (empty for default Anthropic endpoint)
 * @property timeoutSeconds Request timeout in seconds (30-600 range, default 90)
 * @property maxTokens Maximum tokens for response (1-128000 range, null for default)
 */
@Serializable
data class AnthropicSettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
    val maxTokens: Int? = null,
) {
    /**
     * Check if settings are valid.
     */
    val isValid: Boolean
        get() = modelId.isNotEmpty() && key.isNotEmpty()

    companion object {
        const val DEFAULT_MODEL = "claude-3-5-sonnet-20241022"
    }
}

/**
 * Settings for the DeepL translation provider.
 *
 * @property key DeepL API key (free-tier keys end with ":fx")
 * @property baseUrl Custom base URL for API requests (empty for the official DeepL endpoint)
 * @property timeoutSeconds Request timeout in seconds (30-600 range, default 90)
 */
@Serializable
data class DeepLSettings(
    val key: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
) {
    /**
     * Check if settings are valid. DeepL needs only an API key.
     */
    val isValid: Boolean
        get() = key.isNotEmpty()
}

/**
 * Sealed interface for provider-specific settings.
 */
sealed interface AISettings {
    val providerType: AIProvider

    /**
     * OpenAI-compatible settings.
     */
    @Suppress("DataClassShouldBeImmutable")
    data class OpenAI(
        val openaiSettings: com.nononsenseapps.feeder.ai.model.OpenAISettings =
            com.nononsenseapps.feeder.ai.model
                .OpenAISettings(),
    ) : AISettings {
        override val providerType: AIProvider = AIProvider.OPENAI_COMPATIBLE
    }

    /**
     * Anthropic settings.
     */
    @Suppress("DataClassShouldBeImmutable")
    data class Anthropic(
        val anthropicSettings: com.nononsenseapps.feeder.ai.model.AnthropicSettings =
            com.nononsenseapps.feeder.ai.model
                .AnthropicSettings(),
    ) : AISettings {
        override val providerType: AIProvider = AIProvider.ANTHROPIC
    }

    /**
     * DeepL settings.
     */
    @Suppress("DataClassShouldBeImmutable")
    data class DeepL(
        val deepLSettings: DeepLSettings =
            com.nononsenseapps.feeder.ai.model
                .DeepLSettings(),
    ) : AISettings {
        override val providerType: AIProvider = AIProvider.DEEPL
    }

    /**
     * Check if settings are valid.
     */
    val isValid: Boolean
        get() =
            when (this) {
                is OpenAI -> openaiSettings.isValid
                is Anthropic -> anthropicSettings.isValid
                is DeepL -> deepLSettings.isValid
            }

    /**
     * Get default model ID for the current provider.
     */
    fun getDefaultModelId(): String =
        when (this) {
            is OpenAI -> OpenAISettings.DEFAULT_MODEL
            is Anthropic -> AnthropicSettings.DEFAULT_MODEL
            is DeepL -> ""
        }

    companion object {
        /**
         * Get default settings for a given provider.
         */
        fun defaultForProvider(provider: AIProvider): AISettings =
            when (provider) {
                AIProvider.OPENAI_COMPATIBLE -> OpenAI()
                AIProvider.ANTHROPIC -> Anthropic()
                AIProvider.DEEPL -> DeepL()
            }
    }
}
