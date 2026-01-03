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
     * Builds a JSON-structured translation prompt for accurate, professional translation.
     *
     * Based on research of best practices for AI translation prompts, including:
     * - Professional role assignment
     * - Clear translation guidelines
     * - JSON structured input/output for reliable parsing
     * - Context preservation for technical accuracy
     */
    private fun buildTranslationPrompt(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage,
    ): String {
        // Build JSON array of paragraphs with indices
        val paragraphsJson = paragraphs.mapIndexed { index, text ->
            """        {"index": ${index + 1}, "text": ${jsonEscape(text)}}"""
        }.joinToString(",\n")

        return """
You are a distinguished professional translator and bilingual scholar specializing in ${targetLanguage.languageName}. Your expertise encompasses accurately and elegantly translating texts while meticulously considering all linguistic complexities, nuances, and cultural contexts.

## Translation Task

Translate the following article paragraphs from JSON format to ${targetLanguage.languageName}.

## Input Format (JSON)
```json
{
  "targetLanguage": "${targetLanguage.languageName}",
  "paragraphs": [
$paragraphsJson
  ]
}
```

## Output Requirements

Respond with a JSON object in the following exact format:
```json
{
  "targetLanguage": "${targetLanguage.languageName}",
  "translations": [
    {"index": 1, "translation": "..."},
    {"index": 2, "translation": "..."}
  ]
}
```

## Translation Guidelines

1. **Accuracy & Precision**: Maintain technical accuracy while ensuring the translation flows naturally in ${targetLanguage.languageName}

2. **Cultural Adaptation**: Adapt expressions and cultural references to ${targetLanguage.languageName} language conventions

3. **Tone Preservation**: Preserve the author's style, tone, and intent

4. **Technical Terms**: Keep technical terminology, code, variable names, and commands untranslated

5. **Format Preservation**: Maintain the original paragraph structure, numbering, and layout

6. **Quality**: Provide fluent, professional translations that read naturally to native speakers

7. **Consistency**: Use consistent terminology throughout the translation

8. **Completeness**: Translate ALL paragraphs. Return exactly ${paragraphs.size} translations.

## Important

- Return ONLY the JSON object, no additional text or explanations
- Ensure the JSON is valid and can be parsed
- Match each index exactly from the input (1 to ${paragraphs.size})
- Do not omit any paragraphs
- Do not add any conversational filler

Now, translate the above JSON input.
""".trimIndent()
    }

    /**
     * Parses JSON-structured translation response.
     *
     * Expected format:
     * ```json
     * {
     *   "translations": [
     *     {"index": 1, "translation": "..."},
     *     {"index": 2, "translation": "..."}
     *   ]
     * }
     * ```
     */
    private fun parseTranslationResponse(
        response: String,
        expectedParagraphs: Int,
    ): List<String> {
        // Extract JSON from markdown code blocks if present
        val jsonContent = extractJsonFromResponse(response)

        try {
            // Find the translations array
            val translationsStart = jsonContent.indexOf("\"translations\":")
            if (translationsStart == -1) {
                throw AIClientException("Translations array not found in response")
            }

            // Find the array start after "translations":
            val arrayStart = jsonContent.indexOf('[', translationsStart)
            if (arrayStart == -1) {
                throw AIClientException("Translations array start not found")
            }

            // Find matching closing bracket
            var depth = 0
            var inString = false
            var escaped = false
            var arrayEnd = -1

            for (i in arrayStart until jsonContent.length) {
                val c = jsonContent[i]

                if (escaped) {
                    escaped = false
                    continue
                }

                when (c) {
                    '\\' -> escaped = true
                    '"' -> inString = !inString
                    '[', '{' -> if (!inString) depth++
                    ']', '}' -> {
                        if (!inString) {
                            depth--
                            if (depth == 0 && c == ']') {
                                arrayEnd = i
                                break
                            }
                        }
                    }
                }
            }

            if (arrayEnd == -1) {
                throw AIClientException("Translations array end not found")
            }

            // Extract just the array content and parse each object
            val arrayContent = jsonContent.substring(arrayStart + 1, arrayEnd).trim()

            val translations = mutableMapOf<Int, String>()

            // Split by }, { to get individual objects
            val objects = arrayContent.split(Regex("""\}\s*,\s*\{"""))

            for (obj in objects) {
                // Clean up the object string (remove surrounding braces if present)
                val cleanObj = obj.trim().removeSurrounding("{", "}")

                // Extract index
                val indexMatch = Regex(""""index"\s*:\s*(\d+)""").find(cleanObj)
                // Extract translation - handle newlines and escaped characters
                val translationMatch = Regex(""""translation"\s*:\s*"((?:[^"\\]|\\.)*)""", RegexOption.DOT_MATCHES_ALL).find(cleanObj)

                if (indexMatch != null && translationMatch != null) {
                    val index = indexMatch.groupValues[1].toInt()
                    val translation = unescapeJson(translationMatch.groupValues[1])
                    translations[index] = translation
                }
            }

            if (translations.size != expectedParagraphs) {
                throw AIClientException(
                    "Expected $expectedParagraphs paragraphs, got ${translations.size}"
                )
            }

            return translations.toSortedMap().values.toList()
        } catch (e: Exception) {
            throw AIClientException(
                "Failed to parse translation response: ${e.message}. Response: $response"
            )
        }
    }

    /**
     * Extracts JSON content from response, handling markdown code blocks.
     */
    private fun extractJsonFromResponse(response: String): String {
        // Try to extract JSON from markdown code blocks
        val codeBlockPattern = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""")
        val match = codeBlockPattern.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Try to find JSON object boundaries
        val firstBrace = response.indexOf('{')
        val lastBrace = response.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1)
        }

        return response.trim()
    }

    /**
     * Escapes text for JSON string literal.
     */
    private fun jsonEscape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Unescapes JSON string literal.
     */
    private fun unescapeJson(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
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
