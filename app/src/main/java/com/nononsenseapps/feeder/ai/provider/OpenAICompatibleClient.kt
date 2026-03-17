package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.SummaryResponseParser
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.TranslationPromptBuilder
import com.nononsenseapps.feeder.ai.model.OpenAISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
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

            val summaryData = SummaryResponseParser.parse(text)

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
            AIClient.SummaryResult.Error(
                content = SummaryResponseParser.sanitizeErrorMessage(e.message ?: e.cause?.message),
            )
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
            val prompt = TranslationPromptBuilder.buildTranslationPrompt(translatableTexts, targetLanguage)

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

    private class AIClientException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
