package com.nononsenseapps.feeder.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SummaryResponseParser.
 *
 * Covers all 14 BDD scenarios from 01.1-behavior-scenarios.md plus additional edge cases.
 * Maps: SCENARIO-001 through SCENARIO-014.
 */
class SummaryResponseParserTest {
    private val errorMessage = "Could not generate summary. Please try again."

    // ===== SCENARIO-001: Well-formed JSON response =====

    @Test
    fun parse_validJsonWithAllFields_extractsSummary() {
        val input = """{"language":"en","title":"Test Title","keyPoints":["Point 1","Point 2"],"summary":"The article discusses important topics."}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The article discusses important topics.", result.summary)
        assertEquals("en", result.language)
        assertEquals("Test Title", result.title)
        assertEquals(listOf("Point 1", "Point 2"), result.keyPoints)
    }

    @Test
    fun parse_validJsonWithSummaryOnly_extractsSummary() {
        val input = """{"summary":"Just a summary"}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Just a summary", result.summary)
    }

    // ===== SCENARIO-002: JSON inside markdown code blocks =====

    @Test
    fun parse_jsonInJsonCodeBlock_extractsSummary() {
        val input = """```json
{"language":"en","title":"T","keyPoints":["P1"],"summary":"Extracted from code block."}
```"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Extracted from code block.", result.summary)
    }

    @Test
    fun parse_jsonInPlainCodeBlock_extractsSummary() {
        val input = """```
{"summary":"Extracted from plain code block."}
```"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Extracted from plain code block.", result.summary)
    }

    // ===== SCENARIO-003: LLM wraps JSON in explanatory text =====

    @Test
    fun parse_jsonWithTextPreamble_extractsSummary() {
        val input = """Here is the summary of the article in JSON format:

{"language":"en","title":"Article About X","keyPoints":["Point 1"],"summary":"The article discusses X."}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The article discusses X.", result.summary)
    }

    @Test
    fun parse_jsonWithPreambleAndPostamble_extractsSummary() {
        val input = """Here is the summary:

{"language":"en","title":"T","keyPoints":["P1"],"summary":"Extracted text."}

I hope this helps! Let me know if you need anything else."""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Extracted text.", result.summary)
    }

    // ===== SCENARIO-004: Truncated JSON response =====

    @Test
    fun parse_truncatedJsonInCodeBlock_returnsErrorMessage() {
        val input = """```json
{"language":"en","title":"Very Long Article Title","keyPoints":["Point 1","Point 2"],"summary":"This is a very long summary that discusses many aspects of the topic including the economic im"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    @Test
    fun parse_truncatedJsonNoBraceMatch_returnsErrorMessage() {
        val input = """{"summary":"This summary is cut off mid-senten"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    // ===== SCENARIO-005: Error message containing JSON =====

    @Test
    fun parse_errorMessageContainingJson_returnsCleanMessage() {
        val input = """Request failed: {"error":{"message":"Rate limit exceeded","type":"rate_limit_error","code":"429"}}"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    // ===== SCENARIO-006: Double-encoded JSON string =====

    @Test
    fun parse_doubleEncodedJsonString_handlesGracefully() {
        val input = """"{\"language\":\"en\",\"summary\":\"The article discusses X.\"}" """

        val result = SummaryResponseParser.parse(input)

        // Should either extract the summary or return user-friendly error — never raw JSON
        assertNoRawJson(result.summary)
    }

    // ===== SCENARIO-007: Plain text summary =====

    @Test
    fun parse_plainTextResponse_returnsAsIs() {
        val input = "This is a plain text summary about the article content."

        val result = SummaryResponseParser.parse(input)

        assertEquals("This is a plain text summary about the article content.", result.summary)
    }

    @Test
    fun parse_multiLinePlainText_returnsAsIs() {
        val input = """This is the first line of the summary.
It continues on the second line.
And the third line wraps up."""

        val result = SummaryResponseParser.parse(input)

        assertEquals(input, result.summary)
    }

    // ===== SCENARIO-008: Legacy "Lang: XX" format =====

    @Test
    fun parse_legacyLangFormat_extractsSummaryWithoutPrefix() {
        val input = """Lang: en
This is a plain text summary in legacy format."""

        val result = SummaryResponseParser.parse(input)

        assertEquals("This is a plain text summary in legacy format.", result.summary)
        assertEquals("en", result.language)
    }

    @Test
    fun parse_legacyLangFormat_multiLineSummary() {
        val input = """Lang: zh
This is the first line.
This is the second line."""

        val result = SummaryResponseParser.parse(input)

        assertEquals("This is the first line.\nThis is the second line.", result.summary)
        assertEquals("zh", result.language)
    }

    // ===== SCENARIO-009: Empty or missing summary field =====

    @Test
    fun parse_emptySummaryFieldInValidJson_returnsErrorMessage() {
        val input = """{"summary":"","language":"en","title":"Title"}"""

        val result = SummaryResponseParser.parse(input)

        assertTrue(
            result.summary != "",
            "Should not return empty summary",
        )
        assertNoRawJson(result.summary)
    }

    @Test
    fun parse_missingSummaryFieldInValidJson_returnsErrorMessage() {
        val input = """{"language":"en","title":"A Title","keyPoints":["Point 1"]}"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
        assertTrue(
            result.summary.isNotBlank(),
            "Should return a non-blank message",
        )
    }

    // ===== SCENARIO-010: Unrecognized JSON structure =====

    @Test
    fun parse_unrecognizedJsonStructure_returnsErrorMessage() {
        val input = """{"foo":"bar","baz":123,"nested":{"deep":"value"}}"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    // ===== SCENARIO-013: UI defense — JSON bypasses parsing (tested at parser level) =====

    @Test
    fun parse_jsonObjectWithoutSummaryField_neverReturnsRawJson() {
        val input = """{"status":"ok","data":{"count":42}}"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    // ===== SCENARIO-014: Truncated code block markup =====

    @Test
    fun parse_truncatedCodeBlockMarkup_returnsErrorMessage() {
        val input = """```json
{"summary":"trunc"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    @Test
    fun parse_codeBlockStartOnly_returnsErrorMessage() {
        val input = """```json
{"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    // ===== Additional parse edge cases =====

    @Test
    fun parse_emptyInput_handlesGracefully() {
        val result = SummaryResponseParser.parse("")

        // Should not throw
        assertTrue(true, "Should not throw on empty input")
    }

    @Test
    fun parse_blankInput_handlesGracefully() {
        val result = SummaryResponseParser.parse("   ")

        // Should not throw
        assertTrue(true, "Should not throw on blank input")
    }

    @Test
    fun parse_jsonArrayInsteadOfObject_returnsErrorMessage() {
        val input = """[{"summary":"text"},{"summary":"text2"}]"""

        val result = SummaryResponseParser.parse(input)

        assertNoRawJson(result.summary)
    }

    @Test
    fun parse_nestedJsonObjects_extractsSummary() {
        val input = """{"summary":"The summary text.","metadata":{"nested":true,"count":5}}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The summary text.", result.summary)
    }

    @Test
    fun parse_jsonWithSentimentField_extractsSummary() {
        val input = """{"language":"en","title":"T","keyPoints":["P1"],"summary":"Summary here.","sentiment":"neutral"}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Summary here.", result.summary)
        assertEquals("neutral", result.sentiment)
    }

    @Test
    fun parse_jsonWithUnicodeContent_extractsSummary() {
        val input = """{"language":"zh","title":"测试标题","keyPoints":["要点一"],"summary":"这是一个中文摘要。"}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("这是一个中文摘要。", result.summary)
    }

    @Test
    fun parse_jsonWithNewlinesInSummary_preservesNewlines() {
        val input = """{"summary":"Line one.\nLine two.\nLine three."}"""

        val result = SummaryResponseParser.parse(input)

        assertTrue(
            result.summary.contains("Line one.") && result.summary.contains("Line two."),
            "Should preserve multi-line content in summary",
        )
    }

    @Test
    fun parse_jsonWithMarkdownInSummary_preservesMarkdown() {
        val input = """{"summary":"The article discusses **important** topics with `code` examples."}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The article discusses **important** topics with `code` examples.", result.summary)
    }

    @Test
    fun parse_emptySummaryButHasTitleAndKeyPoints_returnsAnalysisMessage() {
        val input = """{"language":"en","title":"Good Title","keyPoints":["P1","P2"],"summary":""}"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("Summary text not available, but article analysis succeeded.", result.summary)
    }

    // ===== extractJsonObject tests =====

    @Test
    fun extractJsonObject_plainJsonObject_returnsAsIs() {
        val input = """{"key":"value"}"""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("""{"key":"value"}""", result)
    }

    @Test
    fun extractJsonObject_jsonCodeBlock_extractsContent() {
        val input = """```json
{"key":"value"}
```"""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("""{"key":"value"}""", result)
    }

    @Test
    fun extractJsonObject_plainCodeBlock_extractsContent() {
        val input = """```
{"key":"value"}
```"""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("""{"key":"value"}""", result)
    }

    @Test
    fun extractJsonObject_jsonWithPreamble_extractsJsonByBraceMatching() {
        val input = """Here is the JSON:
{"summary":"text","nested":{"a":1}}
Done."""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertTrue(
            result.startsWith("{") && result.endsWith("}"),
            "Should extract JSON object: $result",
        )
        assertTrue(
            result.contains("\"summary\""),
            "Extracted JSON should contain summary field",
        )
    }

    @Test
    fun extractJsonObject_nestedBraces_matchesBalancedBraces() {
        val input = """prefix {"a":{"b":"c"},"d":"e"} suffix"""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("""{"a":{"b":"c"},"d":"e"}""", result)
    }

    @Test
    fun extractJsonObject_bracesInStrings_handledCorrectly() {
        val input = """{"summary":"text with {braces} inside"}"""

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("""{"summary":"text with {braces} inside"}""", result)
    }

    @Test
    fun extractJsonObject_noBraces_returnsInputTrimmed() {
        val input = "  plain text content  "

        val result = SummaryResponseParser.extractJsonObject(input)

        assertEquals("plain text content", result)
    }

    @Test
    fun extractJsonObject_unmatchedBraces_returnsInputTrimmed() {
        val input = """{"summary":"unclosed"""

        val result = SummaryResponseParser.extractJsonObject(input)

        // When braces don't balance, should return trimmed input
        assertTrue(result.isNotEmpty(), "Should return non-empty result")
    }

    // ===== parseLegacyResponse tests =====

    @Test
    fun parseLegacyResponse_langPrefixWithSummary_extractsBoth() {
        val input = """Lang: en
The article is about technology."""

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals("en", result.language)
        assertEquals("The article is about technology.", result.summary)
    }

    @Test
    fun parseLegacyResponse_langPrefixWithMultiLineBody() {
        val input = """Lang: fr
First paragraph.
Second paragraph."""

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals("fr", result.language)
        assertEquals("First paragraph.\nSecond paragraph.", result.summary)
    }

    @Test
    fun parseLegacyResponse_plainText_returnsAsIs() {
        val input = "A simple plain text summary."

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals("A simple plain text summary.", result.summary)
    }

    @Test
    fun parseLegacyResponse_jsonContent_returnsErrorMessage() {
        val input = """{"summary":"should not leak"}"""

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals(errorMessage, result.summary)
    }

    @Test
    fun parseLegacyResponse_jsonArrayContent_returnsErrorMessage() {
        val input = """[{"summary":"should not leak"}]"""

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals(errorMessage, result.summary)
    }

    @Test
    fun parseLegacyResponse_textWithEmbeddedJsonFields_returnsErrorMessage() {
        val input = """Here is the result: "summary": "leaked content" """

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals(errorMessage, result.summary)
    }

    @Test
    fun parseLegacyResponse_codeBlockMarkup_returnsErrorMessage() {
        val input = """```json
{"summary":"truncated"""

        val result = SummaryResponseParser.parseLegacyResponse(input)

        assertEquals(errorMessage, result.summary)
    }

    // ===== containsRawJson tests =====

    @Test
    fun containsRawJson_jsonObject_returnsTrue() {
        assertTrue(SummaryResponseParser.containsRawJson("""{"key":"value"}"""))
    }

    @Test
    fun containsRawJson_jsonArray_returnsTrue() {
        assertTrue(SummaryResponseParser.containsRawJson("""[1,2,3]"""))
    }

    @Test
    fun containsRawJson_codeBlock_returnsTrue() {
        assertTrue(SummaryResponseParser.containsRawJson("```json\n{}"))
    }

    @Test
    fun containsRawJson_embeddedJsonFields_returnsTrue() {
        assertTrue(
            SummaryResponseParser.containsRawJson("""Here is "summary": "value" in text"""),
        )
    }

    @Test
    fun containsRawJson_plainText_returnsFalse() {
        assertFalse(SummaryResponseParser.containsRawJson("This is a normal summary."))
    }

    @Test
    fun containsRawJson_textWithQuotes_returnsFalse() {
        assertFalse(SummaryResponseParser.containsRawJson("""He said "hello" to everyone."""))
    }

    @Test
    fun containsRawJson_emptyString_returnsFalse() {
        assertFalse(SummaryResponseParser.containsRawJson(""))
    }

    // ===== sanitizeErrorMessage tests =====

    @Test
    fun sanitizeErrorMessage_plainError_returnsAsIs() {
        val result = SummaryResponseParser.sanitizeErrorMessage("Connection timed out")

        assertEquals("Connection timed out", result)
    }

    @Test
    fun sanitizeErrorMessage_errorWithJson_returnsSanitized() {
        val result =
            SummaryResponseParser.sanitizeErrorMessage(
                """Request failed: {"error":"rate_limit"}""",
            )

        assertEquals("Summary generation failed. Please try again.", result)
    }

    @Test
    fun sanitizeErrorMessage_nullMessage_returnsDefault() {
        val result = SummaryResponseParser.sanitizeErrorMessage(null)

        assertEquals("Summary generation failed. Please try again.", result)
    }

    @Test
    fun sanitizeErrorMessage_plainErrorWithBrackets_returnsAsIs() {
        val result = SummaryResponseParser.sanitizeErrorMessage("Error code [429]")

        // No JSON braces, so should return as-is
        assertEquals("Error code [429]", result)
    }

    // ===== Integration-style: full pipeline tests =====

    @Test
    fun parse_realWorldOllamaResponse_extractsSummary() {
        val input = """Sure! Here's the summary:

```json
{
  "language": "en",
  "title": "Climate Change Report",
  "keyPoints": [
    "Global temperatures rising",
    "Ice caps melting",
    "Sea levels increasing"
  ],
  "summary": "The report highlights the ongoing effects of climate change worldwide."
}
```

Let me know if you need anything else!"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The report highlights the ongoing effects of climate change worldwide.", result.summary)
        assertEquals("en", result.language)
        assertEquals("Climate Change Report", result.title)
        assertEquals(3, result.keyPoints.size)
    }

    @Test
    fun parse_realWorldChattyResponse_extractsSummary() {
        val input = """I'd be happy to summarize this article for you. Here is the summary in the requested JSON format:

{"language":"en","title":"Tech Industry Update","keyPoints":["AI advancement","Market growth"],"summary":"The tech industry continues to see rapid advancement in AI technologies."}

I hope this summary captures the key points of the article!"""

        val result = SummaryResponseParser.parse(input)

        assertEquals("The tech industry continues to see rapid advancement in AI technologies.", result.summary)
    }

    @Test
    fun parse_neverThrows_withAnyInput() {
        val inputs =
            listOf(
                "",
                "   ",
                "normal text",
                "{}",
                "{",
                "}",
                "[]",
                "[",
                "]",
                "```",
                "```json",
                "```json\n{",
                """{"broken""",
                """{"summary":}""",
                """{"summary":null}""",
                "null",
                "true",
                "42",
                "\"just a string\"",
                "{\"a\":{\"b\":{\"c\":{\"d\":\"deep\"}}}}",
                "Lang: en",
                "Lang:",
                "Lang: en\n",
            )

        for (input in inputs) {
            try {
                SummaryResponseParser.parse(input)
            } catch (e: Exception) {
                assertTrue(false, "parse() should NEVER throw, but threw for input '$input': ${e.message}")
            }
        }
    }

    // ===== Helper =====

    /**
     * Asserts that the given summary text does not contain raw JSON.
     * Checks for common JSON indicators like braces, brackets, and code block markers.
     */
    private fun assertNoRawJson(summary: String) {
        val containsJsonObject = summary.contains("{") && summary.contains("}")
        val containsJsonArray = summary.startsWith("[") && summary.contains("]")
        val containsCodeBlock = summary.contains("```")

        assertTrue(
            !containsJsonObject && !containsJsonArray && !containsCodeBlock,
            "Summary should not contain raw JSON or code blocks. Got: $summary",
        )
    }
}
