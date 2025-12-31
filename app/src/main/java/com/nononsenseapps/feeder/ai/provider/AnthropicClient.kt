package com.nononsenseapps.feeder.ai.provider

import com.anthropic.client.AnthropicClientAsync
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.model.AnthropicSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun generateSummary(content: String): AIClient.SummaryResult {
        if (!settings.isValid) {
            return AIClient.SummaryResult.Error(content = "Invalid settings")
        }

        return try {
            val systemPrompt = """You are a helpful assistant that summarizes news articles.
Start your response with "Lang: " followed by the detected language code,
then provide a concise summary of the article in that same language.

For example:
Lang: en
This article discusses...

Now summarize this article:"""

            val userMessage = "$systemPrompt\n$content"

            val params = MessageCreateParams.builder()
                .model(settings.modelId)
                .maxTokens(1024L)
                .addUserMessage(userMessage)
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

    private class AIClientException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
