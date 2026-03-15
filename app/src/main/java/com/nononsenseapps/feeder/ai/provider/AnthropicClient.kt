package com.nononsenseapps.feeder.ai.provider

import com.anthropic.client.AnthropicClientAsync
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync
import com.anthropic.models.messages.MessageCreateParams
import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.TranslationPromptBuilder
import com.nononsenseapps.feeder.ai.model.AnthropicSettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    companion object {
        private const val TAG = "AnthropicClient"
    }

    override suspend fun listModels(): List<String> {
        // Anthropic doesn't provide a models endpoint and users input model ID directly
        // Return empty list - UI should not show dropdown for Anthropic
        return emptyList()
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
                MessageCreateParams
                    .builder()
                    .model(settings.modelId)
                    .system(systemPrompt)
                    .maxTokens(2048L) // Increased for JSON structured response
                    .addUserMessage(content)
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client.messages().create(params).get()
                }

            // Get content blocks from the response - iterate and collect text
            val text =
                response.content().joinToString("") { contentBlock ->
                    contentBlock.text().getOrNull()?.text() ?: ""
                }

            val summaryData = parseSummaryJsonResponse(text)

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
     * Translates article paragraphs with structure context using Anthropic Claude API.
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
            val prompt = TranslationPromptBuilder.buildTranslationPrompt(translatableTexts, targetLanguage)

            val params =
                MessageCreateParams
                    .builder()
                    .model(settings.modelId)
                    .maxTokens(8192L)
                    .addUserMessage(prompt)
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client.messages().create(params).get()
                }

            // Get content blocks from the response
            val translatedText =
                response.content().joinToString("") { contentBlock ->
                    contentBlock.text().getOrNull()?.text() ?: ""
                }

            val translatedParagraphs =
                TranslationPromptBuilder.parseTranslationResponse(
                    translatedText,
                    translatableTexts.size,
                )

            AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(
                content = TranslationPromptBuilder.handleTranslationError(e),
            )
        }
    }

    private fun buildClient(): AnthropicClientAsync {
        val builder =
            AnthropicOkHttpClientAsync
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

}
