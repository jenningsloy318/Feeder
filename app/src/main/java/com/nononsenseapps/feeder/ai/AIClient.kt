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
     *
     * Contains translated paragraphs that correspond 1:1 with the input paragraphs.
     */
    sealed interface TranslationResult {
        /**
         * Error message if translation failed.
         */
        val content: String

        /**
         * Successful translation with paragraph-by-paragraph results.
         *
         * @param paragraphs List of translated paragraphs in the same order as input
         */
        data class Success(
            val paragraphs: List<String>,
        ) : TranslationResult {
            override val content: String
                get() = paragraphs.joinToString("\n\n")
        }

        /**
         * Error during translation.
         *
         * @param content Error message describing what went wrong
         */
        data class Error(
            override val content: String,
        ) : TranslationResult
    }

    /**
     * Generate translations for the given paragraphs.
     *
     * @param paragraphs List of text paragraphs to translate
     * @param targetLanguage Target language for translation
     * @return TranslationResult containing translated paragraphs or error information
     */
    suspend fun translate(
        paragraphs: List<String>,
        targetLanguage: com.nononsenseapps.feeder.ai.model.TranslationLanguage,
    ): TranslationResult

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
