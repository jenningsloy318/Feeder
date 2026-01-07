package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.ChunkTranslationResult
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.TranslationChunk
import com.nononsenseapps.feeder.ai.model.OpenAISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.SocketTimeoutException
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

    companion object {
        private const val TAG = "OpenAICompatibleClient"
    }

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
            val modelsResponse =
                withContext(Dispatchers.IO) {
                    client.models().list().get()
                }

            // Extract model IDs from the response
            modelsResponse
                .data()
                .stream()
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

    /**
     * Builds a comprehensive summary prompt based on research-backed best practices.
     *
     * Based on research from PromptLayer, GenAI Unplugged, and OpenAI documentation:
     * - Professional role assignment for quality output
     * - JSON structured output for reliable parsing
     * - Comprehensive quality guidelines
     * - Structured markdown format for readability
     * - Language detection and handling
     */
    private fun buildSummaryPrompt(language: SummaryLanguage): String {
        val basePrompt =
            """
You are an expert news analyst and professional journalist specializing in concise, accurate article summarization. Your expertise includes identifying key information, extracting essential points, and presenting complex topics in clear, accessible language.

## Task

Summarize the following news article in ${if (language == SummaryLanguage.AUTO_DETECT) "its original language" + " (detect the language first)" else language.languageName}.

## Output Format

Respond with a JSON object in the following exact format:

```json
{
  "language": "ISO 639-1 code (e.g., 'en', 'es', 'zh')",
  "title": "Concise headline that captures the main topic",
  "keyPoints": [
    "First key point or takeaway",
    "Second key point or takeaway",
    "Third key point or takeaway",
    "Fourth key point if applicable"
  ],
  "summary": "Comprehensive summary with clear markdown structure",
  "sentiment": "positive | negative | neutral | mixed"
}
```

## Summarization Guidelines

### Quality Standards

1. **Accuracy**: Ensure all information is factually correct and faithful to the source article
2. **Objectivity**: Maintain neutral stance; present facts without bias or personal opinion
3. **Clarity**: Use clear, accessible language; avoid jargon unless necessary for the topic
4. **Completeness**: Cover all major points from the article; don't omit critical information
5. **Conciseness**: Be comprehensive yet efficient; avoid unnecessary elaboration

### Structure Requirements

1. **Title**: Create a descriptive headline (5-10 words) that captures the essence
2. **Key Points**: Extract 3-5 bullet points that represent the most important takeaways
3. **Summary Structure**:
   - Start with a brief overview paragraph (2-3 sentences)
   - Use ## headings for major sections if the article has multiple topics
   - Use ### subheadings for subsections when appropriate
   - Include bullet points for lists or key details
   - End with a conclusion paragraph if applicable
4. **Markdown Formatting**: Use markdown for better readability (bold, italics, headers, lists)
5. **Paragraph Length**: Keep paragraphs focused (3-5 sentences each)

### Language Handling

${if (language == SummaryLanguage.AUTO_DETECT) {
                """
- **Language Detection**: Identify the article's language from the content
- **Summarize in Same Language**: Always summarize in the detected language
- **Language Field**: Report the detected ISO 639-1 language code in the JSON response
"""
            } else {
                """
- **Target Language**: Summarize in ${language.languageName}
- **Language Field**: Use the fixed language code "${language.code}" in the JSON response
"""
            }}

### Content Guidelines

1. **Main Focus**: Prioritize the central theme, arguments, and conclusions
2. **Supporting Details**: Include crucial supporting information, data, or quotes
3. **Context**: Provide necessary background for understanding the topic
4. **Attribution**: Note sources or key individuals mentioned when relevant
5. **Avoid**: Minor details, tangential information, or excessive examples

### Format Rules

1. **Markdown**: Use markdown formatting for better readability (headers, bold, lists, etc.)
2. **Headings**: Use ## for main sections, ### for subsections
3. **Bullets**: Use bullet points for lists and key details
4. **Bold**: Use **bold** for emphasis on important terms or concepts
5. **Code**: If mentioning code or commands, use inline `code` format

## Important

- Return ONLY the JSON object, no additional text or explanations
- Ensure the JSON is valid and can be parsed
- The summary field MUST contain proper markdown formatting
- Do not include conversational filler like "Here's the summary"
- Extract exactly 3-5 key points (not more, not less)

Now, summarize the following article:
            """.trimIndent()

        return basePrompt
    }

    override suspend fun generateSummary(
        content: String,
        language: SummaryLanguage,
    ): AIClient.SummaryResult {
        if (!settings.isValid) {
            return AIClient.SummaryResult.Error(content = "Invalid settings")
        }

        return try {
            val systemPrompt = buildSummaryPrompt(language)

            val params =
                ChatCompletionCreateParams
                    .builder()
                    .model(settings.modelId)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(content)
                    .maxTokens(2048) // Increased for JSON structured response
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client
                        .chat()
                        .completions()
                        .create(params)
                        .get()
                }

            // Get the first choice
            val choice =
                response.choices().firstOrNull()
                    ?: return AIClient.SummaryResult.Error(content = "No response from API")

            // Get message content - use stream API to collect text
            val text =
                choice
                    .message()
                    .content()
                    .stream()
                    .map { obj -> obj.toString() }
                    .reduce { a, b -> "$a$b" }
                    .orElse("")

            val summaryData = parseSummaryJsonResponse(text)

            // Get usage info - handle optional properly with getOrNull
            val usage = response.usage().getOrNull()
            val promptTokens = usage?.promptTokens()?.toInt() ?: 0
            val completeTokens = usage?.completionTokens()?.toInt() ?: 0
            val totalTokens = usage?.totalTokens()?.toInt() ?: 0

            AIClient.SummaryResult.Success(
                id = response.id(),
                created = response.created(),
                model = response.model().toString(),
                content = summaryData.summary,
                promptTokens = promptTokens,
                completeTokens = completeTokens,
                totalTokens = totalTokens,
                detectedLanguage = summaryData.language,
                title = summaryData.title,
                keyPoints = summaryData.keyPoints,
                sentiment = summaryData.sentiment,
            )
        } catch (e: Exception) {
            AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "Unknown error")
        }
    }

    /**
     * Translates article paragraphs with structure context using OpenAI-compatible API.
     *
     * Sends all paragraphs with structure information (element type, nesting level) in a
     * single request with numbered indexing to maintain paragraph structure.
     * Parses the response to extract translated paragraphs.
     *
     * The structure context helps the AI understand document hierarchy for better translations.
     *
     * @param translatableTexts List of translatable texts with structure metadata
     * @param targetLanguage Target language for translation
     * @return TranslationResult containing translated paragraphs or error
     */
    override suspend fun translate(
        translatableTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): AIClient.TranslationResult {
        if (translatableTexts.isEmpty()) {
            return AIClient.TranslationResult.Error(
                content = "No translatable content found in this article",
            )
        }

        return try {
            val prompt = buildTranslationPrompt(translatableTexts, targetLanguage)

            val params =
                ChatCompletionCreateParams
                    .builder()
                    .model(settings.modelId)
                    .temperature(0.3)
                    .addUserMessage(prompt)
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client
                        .chat()
                        .completions()
                        .create(params)
                        .get()
                }

            // Get the first choice
            val choice =
                response.choices().firstOrNull()
                    ?: return AIClient.TranslationResult.Error(content = "No response from API")

            // Get message content
            val translatedText =
                choice
                    .message()
                    .content()
                    .stream()
                    .map { obj -> obj.toString() }
                    .reduce { a, b -> "$a$b" }
                    .orElse("")

            val translatedParagraphs =
                parseTranslationResponse(
                    translatedText,
                    translatableTexts.size,
                )

            AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(
                content = handleTranslationError(e),
            )
        }
    }

    /**
     * Translates a single chunk of content using OpenAI-compatible API.
     *
     * This method is optimized for chunked translation of long-form content.
     * It uses the same translation logic as translate() but processes smaller
     * chunks (typically 1500-2500 characters) to avoid timeouts.
     *
     * @param chunk TranslationChunk containing texts to translate
     * @param targetLanguage Target language for translation
     * @return ChunkTranslationResult with translated texts or error
     */
    override suspend fun translateChunk(
        chunk: TranslationChunk,
        targetLanguage: TranslationLanguage,
    ): ChunkTranslationResult {
        if (chunk.texts.isEmpty()) {
            return ChunkTranslationResult.Error(
                chunkId = chunk.id,
                error = "Empty chunk - no texts to translate",
                canRetry = false,
            )
        }

        return try {
            val prompt = buildTranslationPrompt(chunk.texts, targetLanguage)

            val params =
                ChatCompletionCreateParams
                    .builder()
                    .model(settings.modelId)
                    .temperature(0.3)
                    .addUserMessage(prompt)
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client
                        .chat()
                        .completions()
                        .create(params)
                        .get()
                }

            // Get the first choice
            val choice =
                response.choices().firstOrNull()
                    ?: return ChunkTranslationResult.Error(
                        chunkId = chunk.id,
                        error = "No response from API",
                        canRetry = true,
                    )

            // Get message content
            val translatedText =
                choice
                    .message()
                    .content()
                    .stream()
                    .map { obj -> obj.toString() }
                    .reduce { a, b -> "$a$b" }
                    .orElse("")

            val translatedParagraphs =
                parseTranslationResponse(
                    translatedText,
                    chunk.texts.size,
                )

            ChunkTranslationResult.Success(
                chunkId = chunk.id,
                translatedTexts = translatedParagraphs,
            )
        } catch (e: Exception) {
            val canRetry = isRetryableError(e)
            ChunkTranslationResult.Error(
                chunkId = chunk.id,
                error = handleTranslationError(e),
                canRetry = canRetry,
            )
        }
    }

    private fun buildClient(): OpenAIClientAsync {
        val builder =
            OpenAIOkHttpClientAsync
                .builder()
                .apiKey(settings.key)
                .timeout(Duration.ofSeconds(settings.timeoutSeconds.toLong()))

        // Set custom base URL if provided
        if (settings.baseUrl.isNotEmpty()) {
            builder.baseUrl(settings.baseUrl)
        }

        return builder.build()
    }

    /**
     * Data class representing the structured JSON summary response.
     */
    @Serializable
    private data class SummaryResponseData(
        val language: String = "",
        val title: String = "",
        val keyPoints: List<String> = emptyList(),
        val summary: String = "",
        val sentiment: String = "",
        val isValid: Boolean = true,  // Track if parsing succeeded
    )

    /**
     * Parses JSON-structured summary response.
     *
     * Extracts and validates all fields from the JSON response with fallback handling.
     */
    private fun parseSummaryJsonResponse(content: String): SummaryResponseData {
        val jsonContent = extractJsonFromMarkdown(content)

        return try {
            val jsonElement = Json.parseToJsonElement(jsonContent)

            val jsonObject = jsonElement.jsonObject

            val language = jsonObject["language"]?.jsonPrimitive?.content ?: ""
            val title = jsonObject["title"]?.jsonPrimitive?.content ?: ""
            val sentiment = jsonObject["sentiment"]?.jsonPrimitive?.content ?: ""

            // Parse keyPoints array
            val keyPoints =
                try {
                    val keyPointsElement = jsonObject["keyPoints"]
                    if (keyPointsElement is JsonArray) {
                        keyPointsElement.mapNotNull { item ->
                            item.jsonPrimitive.content.takeIf { it.isNotEmpty() }
                        }
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }

            val summary = jsonObject["summary"]?.jsonPrimitive?.content ?: ""

            // Fix for spec-26: Prevent raw JSON display to users
            // When summary field is empty or missing, show user-friendly error message
            // instead of displaying the entire raw JSON response.
            val hasUsefulContent = summary.isNotBlank() ||
                                   title.isNotBlank() ||
                                   keyPoints.isNotEmpty()

            val finalSummary = when {
                summary.isNotBlank() -> summary
                title.isNotBlank() || keyPoints.isNotEmpty() ->
                    "Summary text not available, but article analysis succeeded."
                else ->
                    "Could not generate summary. Please try again."
            }

            SummaryResponseData(
                language = language,
                title = title,
                keyPoints = keyPoints,
                summary = finalSummary,  // User-friendly message instead of raw JSON
                sentiment = sentiment,
                isValid = hasUsefulContent,  // Track validity
            )
        } catch (e: SerializationException) {
            // JSON parsing failed, fall back to legacy format
            android.util.Log.e(TAG, "JSON parsing failed for summary", e)
            parseLegacySummaryResponse(content)
        } catch (e: Exception) {
            // Any other error, fall back to legacy format
            android.util.Log.e(TAG, "Unexpected error parsing summary", e)
            parseLegacySummaryResponse(content)
        }
    }

    /**
     * Extracts JSON from markdown code blocks.
     */
    private fun extractJsonFromMarkdown(content: String): String {
        // Try ```json code blocks
        val jsonCodeBlock = Regex("""```json\s*([\s\S]*?)\s*```""").find(content)
        if (jsonCodeBlock != null) {
            return jsonCodeBlock.groupValues[1].trim()
        }

        // Try ``` code blocks
        val codeBlock = Regex("""```\s*([\s\S]*?)\s*```""").find(content)
        if (codeBlock != null) {
            return codeBlock.groupValues[1].trim()
        }

        // Return as-is
        return content.trim()
    }

    /**
     * Parses legacy summary response format.
     *
     * Handles "Lang: XX" prefix format for backward compatibility.
     */
    private fun parseLegacySummaryResponse(content: String): SummaryResponseData {
        val lines = content.lines()
        val lang =
            if (lines.firstOrNull()?.startsWith("Lang:") == true) {
                lines
                    .first()
                    .removePrefix("Lang:")
                    .trim()
                    .take(2)
            } else {
                ""
            }

        // Fix for spec-26: Check if content is raw JSON and prevent displaying it to users
        val summary =
            if (lines.firstOrNull()?.startsWith("Lang:") == true) {
                lines.drop(1).joinToString("\n").trim()
            } else {
                // Check if content looks like JSON (starts with '{')
                val trimmedContent = content.trim()
                if (trimmedContent.startsWith("{") || trimmedContent.startsWith("[")) {
                    // This is likely raw JSON - don't show it to users
                    android.util.Log.w(TAG, "Detected raw JSON in legacy parser, returning error message")
                    "Could not generate summary. Please try again."
                } else {
                    // Not JSON, return as-is (plain text summary)
                    trimmedContent
                }
            }

        return SummaryResponseData(
            language = lang,
            title = "",
            keyPoints = emptyList(),
            summary = summary,
            sentiment = "",
        )
    }

    /**
     * Legacy parseSummaryResponse for backward compatibility.
     * @deprecated Use parseSummaryJsonResponse instead.
     */
    private fun parseSummaryResponse(content: String): Pair<String, String> {
        val summaryData = parseSummaryJsonResponse(content)
        return summaryData.language to summaryData.summary
    }

    /**
     * Builds a JSON-structured translation prompt with structure context for accurate, professional translation.
     *
     * Based on research of best practices for AI translation prompts, including:
     * - Professional role assignment
     * - Clear translation guidelines
     * - JSON structured input/output for reliable parsing
     * - Context preservation for technical accuracy
     * - Structure-aware translation for better quality
     */
    private fun buildTranslationPrompt(
        translatableTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): String {
        // Build JSON array of paragraphs with indices and structure context
        val paragraphsJson =
            translatableTexts
                .mapIndexed { index, tt ->
                    val structureInfo = tt.getStructureDescription()
                    """        {"index": ${index + 1}, "type": "$structureInfo", "text": ${jsonEscape(tt.text)}}"""
                }.joinToString(",\n")

        return """
You are a distinguished professional translator and bilingual scholar specializing in ${targetLanguage.languageName}. Your expertise encompasses accurately and elegantly translating texts while meticulously considering all linguistic complexities, nuances, and cultural contexts.

## Translation Task

Translate the following article paragraphs from JSON format to ${targetLanguage.languageName}.
Each paragraph includes structure information (element type and nesting level) to help you provide better translations.

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

1. **Structure Awareness**: Consider the element type and nesting level:
   - Headings should remain concise and authoritative
   - Paragraphs should flow naturally in ${targetLanguage.languageName}
   - Nested list items should maintain proper indentation and hierarchy
   - Blockquotes should preserve the quoted tone

2. **Accuracy & Precision**: Maintain technical accuracy while ensuring the translation flows naturally in ${targetLanguage.languageName}

3. **Cultural Adaptation**: Adapt expressions and cultural references to ${targetLanguage.languageName} language conventions

4. **Tone Preservation**: Preserve the author's style, tone, and intent based on element type

5. **Technical Terms**: Keep technical terminology, code, variable names, and commands untranslated

6. **Format Preservation**: Maintain the original paragraph structure, numbering, and layout

7. **Quality**: Provide fluent, professional translations that read naturally to native speakers

8. **Consistency**: Use consistent terminology throughout the translation

9. **Completeness**: Translate ALL paragraphs. Return exactly ${translatableTexts.size} translations.

## Important

- Return ONLY the JSON object, no additional text or explanations
- Ensure the JSON is valid and can be parsed
- Match each index exactly from the input (1 to ${translatableTexts.size})
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
                    "Expected $expectedParagraphs paragraphs, got ${translations.size}",
                )
            }

            return translations.toSortedMap().values.toList()
        } catch (e: Exception) {
            throw AIClientException(
                "Failed to parse translation response: ${e.message}. Response: $response",
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
    private fun jsonEscape(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    /**
     * Unescapes JSON string literal.
     */
    private fun unescapeJson(text: String): String =
        text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    /**
     * Handles translation errors with user-friendly messages.
     */
    private fun handleTranslationError(e: Exception): String =
        when {
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

    /**
     * Determines if an error is retryable for chunk translation.
     *
     * Retryable errors:
     * - Timeouts (temporary network issues)
     * - Rate limits (can retry after delay)
     * - Server errors (5xx)
     *
     * Non-retryable errors:
     * - Invalid API key (configuration issue)
     * - Quota exceeded (account issue)
     * - Invalid request (4xx client errors)
     */
    private fun isRetryableError(e: Exception): Boolean =
        when {
            e is SocketTimeoutException -> true
            e.message?.contains("timeout", ignoreCase = true) == true -> true
            e.message?.contains("rate limit", ignoreCase = true) == true -> true
            e.message?.contains("server error", ignoreCase = true) == true -> true
            e.message?.contains("5", ignoreCase = true) == true -> true  // 5xx errors
            e.message?.contains("invalid api key", ignoreCase = true) == true -> false
            e.message?.contains("quota exceeded", ignoreCase = true) == true -> false
            e.message?.contains("insufficient quota", ignoreCase = true) == true -> false
            else -> false  // Default to non-retryable for unknown errors
        }

    private class AIClientException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
