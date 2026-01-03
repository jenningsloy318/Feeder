package com.nononsenseapps.feeder.ai.provider

import com.anthropic.client.AnthropicClientAsync
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.model.AnthropicSettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

/**
 * Anthropic Claude AI client implementation.
 *
 * Uses the official Anthropic Java SDK (version 2.11.1).
 */
class AnthropicClient(
    private val settings: AnthropicSettings,
) : AIClient {
    private val client: AnthropicClientAsync by lazy { buildClient() }

    override suspend fun listModels(): List<String> {
        // Anthropic doesn't provide a models endpoint and users input model ID directly
        // Return empty list - UI should not show dropdown for Anthropic
        return emptyList()
    }

    private fun buildSummaryPrompt(language: SummaryLanguage): String {
        return when (language) {
            SummaryLanguage.AUTO_DETECT -> """
                You are a helpful assistant that summarizes news articles.
                Detect the article's language and summarize in that same language.

                Start your response with "Lang: " followed by the detected language code.
                For example: "Lang: en"

                Then provide a concise summary of the article.
            """.trimIndent()

            else -> """
                You are a helpful assistant that summarizes news articles in ${language.languageName}.

                Provide a concise summary of the following article in ${language.languageName}.
            """.trimIndent()
        }
    }

    override suspend fun generateSummary(
        content: String,
        language: SummaryLanguage
    ): AIClient.SummaryResult {
        if (!settings.isValid) {
            return AIClient.SummaryResult.Error(content = "Invalid settings")
        }

        return try {
            val systemPrompt = buildSummaryPrompt(language)

            val params = MessageCreateParams.builder()
                .model(settings.modelId)
                .system(systemPrompt)
                .maxTokens(1024L)
                .addUserMessage(content)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.messages().create(params).get()
            }

            // Get content blocks from the response - iterate and collect text
            val text = response.content().joinToString("") { contentBlock ->
                contentBlock.text().getOrNull()?.text() ?: ""
            }

            val (lang, summary) = parseSummaryResponse(text)

            // Get usage info - usage() returns Usage directly
            val usage = response.usage()
            val promptTokens = usage.inputTokens().toInt()
            val completeTokens = usage.outputTokens().toInt()
            val totalTokens = promptTokens + completeTokens

            // Get stop reason - convert to boolean for compatibility
            val isComplete = response.stopReason().getOrNull()?.toString() == "end_turn"

            AIClient.SummaryResult.Success(
                id = response.id(),
                created = if (isComplete) 1L else 0L,
                model = response.model().toString(),
                content = summary,
                promptTokens = promptTokens,
                completeTokens = completeTokens,
                totalTokens = totalTokens,
                detectedLanguage = lang,
            )
        } catch (e: Exception) {
            AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "Unknown error")
        }
    }

    /**
     * Translates article paragraphs using Anthropic Claude API.
     *
     * Sends all paragraphs in a single request with numbered indexing to maintain
     * paragraph structure. Parses the response to extract translated paragraphs.
     *
     * @param paragraphs List of text paragraphs to translate
     * @param targetLanguage Target language for translation
     * @return TranslationResult containing translated paragraphs or error
     */
    override suspend fun translate(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage,
    ): AIClient.TranslationResult {
        if (paragraphs.isEmpty()) {
            return AIClient.TranslationResult.Error(
                content = "No translatable content found in this article"
            )
        }

        return try {
            val prompt = buildTranslationPrompt(paragraphs, targetLanguage)

            val params = MessageCreateParams.builder()
                .model(settings.modelId)
                .maxTokens(8192L)
                .addUserMessage(prompt)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.messages().create(params).get()
            }

            // Get content blocks from the response
            val translatedText = response.content().joinToString("") { contentBlock ->
                contentBlock.text().getOrNull()?.text() ?: ""
            }

            val translatedParagraphs = parseTranslationResponse(
                translatedText,
                paragraphs.size
            )

            AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(
                content = handleTranslationError(e)
            )
        }
    }

    private fun buildClient(): AnthropicClientAsync {
        val builder = AnthropicOkHttpClientAsync.builder()
            .apiKey(settings.key)
            .timeout(Duration.ofSeconds(settings.timeoutSeconds.toLong()))

        // Set custom base URL if provided
        if (settings.baseUrl.isNotEmpty()) {
            builder.baseUrl(settings.baseUrl)
        }

        return builder.build()
    }

    private fun parseSummaryResponse(content: String): Pair<String, String> {
        val lines = content.lines()
        val lang = if (lines.firstOrNull()?.startsWith("Lang:") == true) {
            lines.first().removePrefix("Lang:").trim().take(2)
        } else {
            ""
        }
        val summary = if (lines.firstOrNull()?.startsWith("Lang:") == true) {
            lines.drop(1).joinToString("\n").trim()
        } else {
            content.trim()
        }
        return lang to summary
    }

    /**
     * Builds a translation prompt with numbered paragraphs for indexing.
     */
    private fun buildTranslationPrompt(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage,
    ): String {
        val numberedParagraphs = paragraphs.mapIndexed { index, text ->
            "[${index + 1}] $text"
        }.joinToString("\n\n")

        return """
            You are a professional translator. Translate the following article to ${targetLanguage.languageName}.

            $numberedParagraphs

            Provide your translation in the same numbered format:
            [1] (translation of paragraph 1)
            [2] (translation of paragraph 2)
            ...

            Guidelines:
            - Maintain the numbered format [N] for each paragraph
            - Translate only the content, not the numbers
            - Preserve the meaning and tone
            - Use natural, fluent expressions
            - Return only the numbered translations
        """.trimIndent()
    }

    /**
     * Parses the translation response to extract numbered paragraphs.
     */
    private fun parseTranslationResponse(
        response: String,
        expectedParagraphs: Int,
    ): List<String> {
        val paragraphPattern = Regex(
            "\\[(\\d+)\\]\\s*(.+?)(?=\\[\\d+\\]|\\Z)",
            RegexOption.DOT_MATCHES_ALL
        )

        val translations = paragraphPattern.findAll(response)
            .associate { it.groupValues[1].toInt() to it.groupValues[2].trim() }
            .toSortedMap()
            .values
            .toList()

        if (translations.size != expectedParagraphs) {
            throw AIClientException(
                "Expected $expectedParagraphs paragraphs, got ${translations.size}"
            )
        }

        return translations
    }

    /**
     * Handles translation errors with user-friendly messages.
     */
    private fun handleTranslationError(e: Exception): String {
        return when {
            e.message?.contains("rate limit", ignoreCase = true) == true ->
                "Rate limit exceeded. Please try again later."

            e.message?.contains("invalid api key", ignoreCase = true) == true ->
                "Invalid API key. Check your AI provider settings."

            e.message?.contains("timeout", ignoreCase = true) == true ||
            e is SocketTimeoutException ->
                "Translation timed out. Please check your connection."

            e.message?.contains("insufficient quota", ignoreCase = true) == true ||
            e.message?.contains("quota exceeded", ignoreCase = true) == true ->
                "API quota exceeded. Please check your account."

            else ->
                "Translation failed: ${e.message ?: "Unknown error"}"
        }
    }

    private class AIClientException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
