package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

/**
 * Unified AI API supporting multiple providers (OpenAI-compatible and Anthropic).
 *
 * This class provides a high-level API for AI operations, abstracting away
 * provider-specific details. It uses the factory pattern to create the appropriate
 * client based on the selected provider.
 *
 * @param repository Repository for accessing AI settings
 * @param appLang Application language code for prompts
 */
class AIApi(
    private val repository: Repository,
    private val appLang: String,
) {
    /**
     * Wrapper for summary response with language detection.
     */
    @Serializable
    data class SummaryResponse(
        val lang: String,
        val content: String,
    )

    companion object {
        private val LANG_REGEX = Regex("^Lang: \"?([a-zA-Z]+)\"?$")
    }

    private val aiSettings: AISettings
        get() = repository.aiSettings

    private val client: AIClient
        get() = AIClient.create(repository.aiSettings)

    /**
     * List available model IDs for the current provider.
     */
    suspend fun listModelIds(settings: AISettings): AIClient.ModelsResult {
        if (!settings.isValid) {
            return AIClient.ModelsResult.MissingToken
        }
        when (settings) {
            is AISettings.OpenAI -> {
                if (settings.openaiSettings.isPerplexity) {
                    return AIClient.ModelsResult.Success(ids = emptyList())
                }
                if (settings.openaiSettings.isAzure) {
                    if (settings.openaiSettings.azureApiVersion.isBlank()) {
                        return AIClient.ModelsResult.AzureApiVersionRequired
                    }
                    if (settings.openaiSettings.azureDeploymentId.isBlank()) {
                        return AIClient.ModelsResult.AzureDeploymentIdRequired
                    }
                }
            }
            is AISettings.Anthropic -> {
                // Anthropic doesn't have special endpoint checks
            }
        }
        return try {
            AIClient.create(settings)
                .listModels()
                .let { AIClient.ModelsResult.Success(ids = it) }
        } catch (e: Exception) {
            AIClient.ModelsResult.Error(message = e.message ?: e.cause?.message)
        }
    }

    /**
     * Generate a summary for the given content.
     */
    suspend fun summarize(content: String): AIClient.SummaryResult {
        return try {
            // Check if summaries are enabled
            val enabled = repository.summaryEnabled.first()
            if (!enabled) {
                return AIClient.SummaryResult.Error(content = "")
            }

            val language = repository.summaryLanguage.first()
            client.generateSummary(content, language)
        } catch (e: Exception) {
            AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
        }
    }

    /**
     * Translate paragraphs with structure context to the configured target language.
     *
     * Uses AI provider (OpenAI-compatible or Anthropic) to translate article content.
     * Sends all paragraphs with structure information in a single API request for efficient translation.
     *
     * The structure context (element type, nesting level) helps the AI produce better translations
     * by understanding the document hierarchy.
     *
     * @param translatableTexts List of translatable texts with structure metadata
     * @return TranslationResult with translated paragraphs or error
     */
    suspend fun translate(translatableTexts: List<TranslatableText>): AIClient.TranslationResult {
        return try {
            // Get target language from settings
            val language = repository.translationLanguage.first()

            if (translatableTexts.isEmpty()) {
                return AIClient.TranslationResult.Error(content = "No translatable content found in this article")
            }

            // Call AI provider to translate with structure context
            val translatedParagraphs = client.translate(translatableTexts, language)
            translatedParagraphs
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(content = e.message ?: e.cause?.message ?: "Translation failed")
        }
    }

    /**
     * Parse the summary response to extract language and content.
     */
    private fun parseSummaryResponse(content: String): SummaryResponse {
        val firstLine = content.lineSequence().firstOrNull() ?: ""
        val result = LANG_REGEX.find(firstLine)
        return SummaryResponse(
            lang = result?.groupValues?.getOrNull(1) ?: "",
            content = content.replaceFirst(firstLine, "").trim(),
        )
    }
}
