package com.nononsenseapps.feeder.ai

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared parser for AI summary JSON responses.
 *
 * Handles multiple response formats from different LLM providers:
 * - Well-formed JSON with summary fields
 * - JSON wrapped in markdown code blocks
 * - JSON embedded in conversational preamble/postamble text
 * - Truncated JSON from token limit responses
 * - Legacy "Lang: XX" format
 * - Plain text summaries
 */
object SummaryResponseParser {
    private const val TAG = "SummaryResponseParser"

    private const val ERROR_MESSAGE = "Could not generate summary. Please try again."

    /**
     * Regex to detect JSON-like content containing known summary response fields.
     */
    private val JSON_FIELD_PATTERN =
        Regex(""""(language|summary|title|keyPoints|sentiment)"\s*:""")

    /**
     * Parsed summary response data.
     */
    data class SummaryResponseData(
        val language: String = "",
        val title: String = "",
        val keyPoints: List<String> = emptyList(),
        val summary: String = "",
        val sentiment: String = "",
    )

    /**
     * Parse a raw LLM response into structured summary data.
     *
     * Tries JSON extraction first, then falls back to legacy format handling.
     */
    fun parse(content: String): SummaryResponseData {
        val jsonContent = extractJsonObject(content)

        return try {
            val jsonElement = Json.parseToJsonElement(jsonContent)
            val jsonObject = jsonElement.jsonObject

            val language = jsonObject["language"]?.jsonPrimitive?.content ?: ""
            val title = jsonObject["title"]?.jsonPrimitive?.content ?: ""
            val sentiment = jsonObject["sentiment"]?.jsonPrimitive?.content ?: ""

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

            val finalSummary =
                when {
                    summary.isNotBlank() -> summary
                    title.isNotBlank() || keyPoints.isNotEmpty() ->
                        "Summary text not available, but article analysis succeeded."
                    else -> ERROR_MESSAGE
                }

            SummaryResponseData(
                language = language,
                title = title,
                keyPoints = keyPoints,
                summary = finalSummary,
                sentiment = sentiment,
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing failed for summary", e)
            parseLegacyResponse(content)
        }
    }

    /**
     * Extract a JSON object from content that may contain markdown code blocks
     * or surrounding text.
     *
     * Strategy:
     * 1. Try markdown ```json code blocks
     * 2. Try generic ``` code blocks
     * 3. Find JSON object by balanced-brace matching anywhere in text
     * 4. Return content as-is for the parser to handle
     */
    internal fun extractJsonObject(content: String): String {
        // 1. Try ```json code blocks
        val jsonCodeBlock = Regex("""```json\s*([\s\S]*?)\s*```""").find(content)
        if (jsonCodeBlock != null) {
            return jsonCodeBlock.groupValues[1].trim()
        }

        // 2. Try generic ``` code blocks
        val codeBlock = Regex("""```\s*([\s\S]*?)\s*```""").find(content)
        if (codeBlock != null) {
            return codeBlock.groupValues[1].trim()
        }

        // 3. Find JSON object by balanced-brace matching
        val firstBrace = content.indexOf('{')
        if (firstBrace >= 0) {
            val jsonCandidate = content.substring(firstBrace)
            var depth = 0
            var inString = false
            var escape = false
            for (i in jsonCandidate.indices) {
                val c = jsonCandidate[i]
                if (escape) {
                    escape = false
                    continue
                }
                if (c == '\\') {
                    escape = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (c == '{') depth++
                    if (c == '}') {
                        depth--
                        if (depth == 0) return jsonCandidate.substring(0, i + 1)
                    }
                }
            }
        }

        // 4. Return as-is
        return content.trim()
    }

    /**
     * Parse legacy summary response format.
     *
     * Handles:
     * - "Lang: XX" prefix format
     * - Content that starts with JSON characters
     * - Content that contains JSON fields anywhere
     * - Content starting with truncated code blocks
     * - Plain text summaries
     */
    internal fun parseLegacyResponse(content: String): SummaryResponseData {
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

        val summary =
            if (lines.firstOrNull()?.startsWith("Lang:") == true) {
                lines.drop(1).joinToString("\n").trim()
            } else {
                val trimmedContent = content.trim()
                when {
                    // Starts with JSON
                    trimmedContent.startsWith("{") || trimmedContent.startsWith("[") -> {
                        Log.w(TAG, "Detected raw JSON in legacy parser")
                        ERROR_MESSAGE
                    }
                    // Double-encoded JSON string (e.g. "{\"language\":\"en\",...}")
                    trimmedContent.startsWith("\"") && trimmedContent.contains("\\\"") -> {
                        Log.w(TAG, "Detected double-encoded JSON in legacy parser")
                        ERROR_MESSAGE
                    }
                    // Truncated code block
                    trimmedContent.startsWith("```") -> {
                        Log.w(TAG, "Detected truncated code block in legacy parser")
                        ERROR_MESSAGE
                    }
                    // JSON fields embedded anywhere in text
                    JSON_FIELD_PATTERN.containsMatchIn(trimmedContent) -> {
                        Log.w(TAG, "Detected embedded JSON fields in legacy parser")
                        ERROR_MESSAGE
                    }
                    // Plain text - return as-is
                    else -> trimmedContent
                }
            }

        return SummaryResponseData(
            language = lang,
            summary = summary,
        )
    }

    /**
     * Check if content looks like it contains raw JSON that should not be displayed.
     *
     * Used as a UI-layer defense-in-depth check.
     */
    fun containsRawJson(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") ||
            trimmed.startsWith("[") ||
            trimmed.startsWith("```") ||
            JSON_FIELD_PATTERN.containsMatchIn(trimmed)
    }

    /**
     * Sanitize an error message that may contain embedded JSON.
     */
    fun sanitizeErrorMessage(message: String?): String {
        if (message == null) return "Summary generation failed. Please try again."
        val trimmed = message.trim()
        return if (trimmed.contains("{") && trimmed.contains("}")) {
            "Summary generation failed. Please try again."
        } else {
            trimmed
        }
    }
}
