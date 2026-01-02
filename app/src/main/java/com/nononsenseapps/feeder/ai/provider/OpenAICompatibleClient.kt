package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.model.OpenAISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

/**
 * OpenAI-compatible AI client implementation.
 *
 * Supports:
 * - OpenAI API
 * - Azure OpenAI Service
 * - Other OpenAI-compatible endpoints
 *
 * This implementation uses the official openai-java SDK (version 4.13.0).
 */
class OpenAICompatibleClient(
    private val settings: OpenAISettings,
) : AIClient {
    private val client: OpenAIClientAsync by lazy { buildClient() }

    override val providerName: String
        get() = when {
            settings.isAzure -> "Azure OpenAI"
            settings.isPerplexity -> "Perplexity"
            else -> "OpenAI"
        }

    override val modelName: String
        get() = settings.modelId

    override suspend fun listModels(): List<String> {
        if (settings.key.isEmpty()) {
            throw AIClientException("Missing API key")
        }
        if (settings.isPerplexity) {
            // Perplexity doesn't support model listing
            return emptyList()
        }
        if (settings.isAzure) {
            if (settings.azureApiVersion.isBlank()) {
                throw AIClientException("Azure API version is required")
            }
            if (settings.azureDeploymentId.isBlank()) {
                throw AIClientException("Azure deployment ID is required")
            }
            // Azure doesn't have a model listing endpoint
            // Return common Azure deployment models
            return listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-35-turbo",
            )
        }

        return try {
            // Fetch models from the API using the SDK
            val modelsResponse = withContext(Dispatchers.IO) {
                client.models().list().get()
            }

            // Extract model IDs from the response
            modelsResponse.data().stream()
                .map { model -> model.id() }
                .toList()
        } catch (e: Exception) {
            // If API call fails, fall back to known OpenAI models
            listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-3.5-turbo",
            )
        }
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

            val params = ChatCompletionCreateParams.builder()
                .model(settings.modelId)
                .addSystemMessage(systemPrompt)
                .addUserMessage(content)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.chat().completions().create(params).get()
            }

            // Get the first choice
            val choice = response.choices().firstOrNull()
                ?: return AIClient.SummaryResult.Error(content = "No response from API")

            // Get message content - use stream API to collect text
            val text = choice.message().content().stream()
                .map { obj -> obj.toString() }
                .reduce { a, b -> "$a$b" }
                .orElse("")

            val (lang, summary) = parseSummaryResponse(text)

            // Get usage info - handle optional properly with getOrNull
            val usage = response.usage().getOrNull()
            val promptTokens = usage?.promptTokens()?.toInt() ?: 0
            val completeTokens = usage?.completionTokens()?.toInt() ?: 0
            val totalTokens = usage?.totalTokens()?.toInt() ?: 0

            AIClient.SummaryResult.Success(
                id = response.id(),
                created = response.created(),
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

    override suspend fun translate(
        paragraph: String,
        targetLanguage: String,
    ): AIClient.TranslationResult {
        if (!settings.isValid) {
            return AIClient.TranslationResult.Error(
                message = "Invalid settings",
                retryable = false,
            )
        }

        return try {
            val languageName = com.nononsenseapps.feeder.ai.model.TargetLanguage.fromCode(targetLanguage)?.displayName
                ?: "the target language"

            val systemPrompt = """
                You are a professional translator. Translate the following text into $languageName.

                Guidelines:
                - Maintain the original tone and style
                - Preserve formatting (bold, links, etc.)
                - Ensure natural, fluent phrasing
                - Do not add or remove information
                - Return only the translation without any explanations or metadata

                Text to translate:
            """.trimIndent()

            val params = ChatCompletionCreateParams.builder()
                .model(settings.modelId)
                .addSystemMessage(systemPrompt)
                .addUserMessage(paragraph)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.chat().completions().create(params).get()
            }

            // Get the first choice
            val choice = response.choices().firstOrNull()
                ?: return AIClient.TranslationResult.Error(
                    message = "No response from API",
                    retryable = true,
                )

            // Get message content - use stream API to collect text
            val translatedText = choice.message().content().stream()
                .map { obj -> obj.toString() }
                .reduce { a, b -> "$a$b" }
                .orElse("")

            // Get usage info
            val usage = response.usage().getOrNull()
            val promptTokens = usage?.promptTokens()?.toInt() ?: 0
            val completionTokens = usage?.completionTokens()?.toInt() ?: 0
            val totalTokens = usage?.totalTokens()?.toInt() ?: 0

            AIClient.TranslationResult.Success(
                translatedText = translatedText,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = totalTokens,
            )
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(
                message = e.message ?: "Unknown error",
                retryable = true,
            )
        }
    }

    private fun buildClient(): OpenAIClientAsync {
        val builder = OpenAIOkHttpClientAsync.builder()
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

    private class AIClientException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
