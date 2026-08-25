package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.provider.AnthropicClient
import com.nononsenseapps.feeder.ai.provider.DeepLClient
import com.nononsenseapps.feeder.ai.provider.LocalTranslationClient
import com.nononsenseapps.feeder.ai.provider.OpenAICompatibleClient
import org.kodein.di.DI

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
            val title: String = "",
            val keyPoints: List<String> = emptyList(),
            val sentiment: String = "",
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
     * Generate translations for the given paragraphs with structure context.
     *
     * @param translatableTexts List of translatable texts with structure metadata
     * @param targetLanguage Target language for translation
     * @return TranslationResult containing translated paragraphs or error information
     */
    suspend fun translate(
        translatableTexts: List<TranslatableText>,
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
         *
         * @param settings Provider settings
         * @param di App DI container; required for the on-device provider,
         *   which routes through DI-bound LocalTranslator. Callers that are
         *   DIAware should always pass it.
         */
        fun create(
            settings: AISettings,
            di: DI? = null,
        ): AIClient =
            when (settings) {
                is AISettings.OpenAI -> OpenAICompatibleClient(settings.openaiSettings)
                is AISettings.Anthropic -> AnthropicClient(settings.anthropicSettings)
                is AISettings.DeepL -> DeepLClient(settings.deepLSettings)
                is AISettings.OnDevice -> LocalTranslationClient(di, settings.onDeviceSettings)
            }
    }
}
