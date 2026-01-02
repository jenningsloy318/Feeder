package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.provider.AIProvider
import com.nononsenseapps.feeder.ai.provider.AnthropicClient
import com.nononsenseapps.feeder.ai.provider.OpenAICompatibleClient

/**
 * Unified interface for AI clients supporting multiple providers.
 */
interface AIClient {
    /**
     * List available models for the configured provider.
     *
     * @return List of model IDs
     */
    suspend fun listModels(): List<String>

    /**
     * Generate a summary for the given content.
     *
     * @param content The text content to summarize
     * @param language The target language for the summary (default: AUTO_DETECT)
     * @return SummaryResult containing the summary or error information
     */
    suspend fun generateSummary(
        content: String,
        language: SummaryLanguage = SummaryLanguage.AUTO_DETECT,
    ): SummaryResult

    /**
     * Translate a paragraph of text to a target language.
     *
     * @param paragraph The text paragraph to translate
     * @param targetLanguage The target language code (e.g., "zh", "es")
     * @return TranslationResult containing the translated text or error information
     */
    suspend fun translate(
        paragraph: String,
        targetLanguage: String,
    ): TranslationResult

    /**
     * The name of the AI provider (e.g., "OpenAI", "Anthropic").
     */
    val providerName: String

    /**
     * The model ID being used (e.g., "gpt-4o", "claude-3-5-sonnet-20241022").
     */
    val modelName: String

    /**
     * Result of a summary generation request.
     */
    sealed interface SummaryResult {
        val content: String

        /**
         * Successful summary generation with metadata.
         */
        data class Success(
            val id: String,
            val created: Long,
            val model: String,
            override val content: String,
            val promptTokens: Int,
            val completeTokens: Int,
            val totalTokens: Int,
            val detectedLanguage: String,
        ) : SummaryResult

        /**
         * Error during summary generation.
         */
        data class Error(
            override val content: String,
        ) : SummaryResult
    }

    /**
     * Result of a translation request.
     */
    sealed interface TranslationResult {
        /**
         * Successful translation with metadata.
         */
        data class Success(
            val translatedText: String,
            val promptTokens: Int,
            val completionTokens: Int,
            val totalTokens: Int,
        ) : TranslationResult

        /**
         * Error during translation.
         *
         * @property message Error message describing what went wrong
         * @property retryable Whether the operation can be retried (e.g., network error vs invalid API key)
         */
        data class Error(
            val message: String,
            val retryable: Boolean = true,
        ) : TranslationResult
    }

    /**
     * Result of a model listing request.
     */
    sealed interface ModelsResult {
        /**
         * Missing API key.
         */
        data object MissingToken : ModelsResult

        /**
         * Azure API version required but not provided.
         */
        data object AzureApiVersionRequired : ModelsResult

        /**
         * Azure deployment ID required but not provided.
         */
        data object AzureDeploymentIdRequired : ModelsResult

        /**
         * Successfully retrieved model list.
         */
        data class Success(
            val ids: List<String>,
        ) : ModelsResult

        /**
         * Error during model listing.
         */
        data class Error(
            val message: String?,
        ) : ModelsResult
    }

    companion object {
        /**
         * Factory method to create the appropriate client based on settings.
         */
        fun create(settings: AISettings): AIClient {
            return when (settings) {
                is AISettings.OpenAI -> OpenAICompatibleClient(settings.openaiSettings)
                is AISettings.Anthropic -> AnthropicClient(settings.anthropicSettings)
            }
        }
    }
}
