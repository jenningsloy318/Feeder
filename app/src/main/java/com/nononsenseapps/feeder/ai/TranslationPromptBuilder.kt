package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import java.net.SocketTimeoutException

/**
 * Shared utility for building translation prompts and parsing responses.
 * Extracted from duplicated code in OpenAICompatibleClient and AnthropicClient.
 */
object TranslationPromptBuilder {
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
    fun buildTranslationPrompt(
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

5. **Technical Terms**: Keep technical terminology, code, variable names, and commands untranslated.
   Content inside `<code>` tags MUST NOT be translated.

6. **Format Preservation**: Maintain the original paragraph structure, numbering, and layout

7. **Quality**: Provide fluent, professional translations that read naturally to native speakers

8. **Consistency**: Use consistent terminology throughout the translation

9. **Completeness**: Translate ALL paragraphs. Return exactly ${translatableTexts.size} translations.

10. **Table Cells**: Table cell content should be translated naturally, maintaining brevity appropriate for table formatting.

11. **Image Captions**: Image captions should be translated completely, preserving the descriptive tone.

## Inline Formatting Tags

The input text contains XML-like inline formatting tags that MUST be preserved in your translation.

### Tag Reference

| Tag | Meaning | Translation Rule |
|-----|---------|-----------------|
| `<b>text</b>` | Bold | Translate text inside, preserve tags |
| `<i>text</i>` | Italic | Translate text inside, preserve tags |
| `<code>text</code>` | Code | Do NOT translate text inside, preserve exactly as-is |
| `<link href="url">text</link>` | Hyperlink | Translate link text, keep URL and tags exactly as-is |
| `<s>text</s>` | Strikethrough | Translate text inside, preserve tags |
| `<u>text</u>` | Underline | Translate text inside, preserve tags |
| `<sup>text</sup>` | Superscript | Translate text inside, preserve tags |
| `<sub>text</sub>` | Subscript | Translate text inside, preserve tags |
| `<mono>text</mono>` | Monospace | Translate text inside, preserve tags |
| `<font face="x">text</font>` | Font face | Translate text inside, keep face attribute and tags as-is |

### Rules

1. Preserve ALL formatting tags exactly as they appear -- do not add, remove, or modify any tags
2. Text inside `<code>...</code>` is code and MUST NOT be translated
3. URLs inside `<link href="...">` MUST NOT be translated or modified
4. The `face` attribute in `<font face="...">` MUST NOT be modified
5. Keep the tag structure intact even if the translated text changes word order
6. Tags can be nested (e.g., `<link href="url"><b>bold link</b></link>`) -- preserve the nesting

### Example

Input:  "Click <link href="https://example.com">here</link> for <b>important</b> info with <code>map()</code> code"
Output: "Haga clic en <link href="https://example.com">aqui</link> para obtener informacion <b>importante</b> con el codigo <code>map()</code>"

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
    fun parseTranslationResponse(
        response: String,
        expectedParagraphs: Int,
    ): List<String> {
        // Extract JSON from markdown code blocks if present
        val jsonContent = extractJsonFromResponse(response)

        try {
            // Find the translations array
            val translationsStart = jsonContent.indexOf("\"translations\":")
            if (translationsStart == -1) {
                throw TranslationParseException("Translations array not found in response")
            }

            // Find the array start after "translations":
            val arrayStart = jsonContent.indexOf('[', translationsStart)
            if (arrayStart == -1) {
                throw TranslationParseException("Translations array start not found")
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
                throw TranslationParseException("Translations array end not found")
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
                throw TranslationParseException(
                    "Expected $expectedParagraphs paragraphs, got ${translations.size}",
                )
            }

            return translations.toSortedMap().values.toList()
        } catch (e: Exception) {
            throw TranslationParseException(
                "Failed to parse translation response: ${e.message}. Response: $response",
            )
        }
    }

    /**
     * Handles translation errors with user-friendly messages.
     */
    fun handleTranslationError(e: Exception): String =
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
     * Extracts JSON content from response, handling markdown code blocks.
     */
    internal fun extractJsonFromResponse(response: String): String {
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
    internal fun jsonEscape(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    /**
     * Unescapes JSON string literal.
     */
    internal fun unescapeJson(text: String): String =
        text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    class TranslationParseException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
