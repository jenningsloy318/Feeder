package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationPromptBuilderTest {

    // ===== buildTranslationPrompt() =====

    @Test
    fun buildTranslationPrompt_containsTargetLanguage() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("Chinese"), "Prompt should contain target language name")
    }

    @Test
    fun buildTranslationPrompt_containsParagraphText() {
        val texts = listOf(TranslatableText.fromPlainText("Hello world"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("Hello world"), "Prompt should contain the paragraph text")
    }

    @Test
    fun buildTranslationPrompt_containsParagraphIndices() {
        val texts = listOf(
            TranslatableText.fromPlainText("First"),
            TranslatableText.fromPlainText("Second"),
        )
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("\"index\": 1"), "Prompt should contain index 1")
        assertTrue(prompt.contains("\"index\": 2"), "Prompt should contain index 2")
    }

    @Test
    fun buildTranslationPrompt_containsStructureDescriptions() {
        val texts = listOf(
            TranslatableText(text = "Title", elementType = ElementType.HEADING_2, nestingLevel = 0),
            TranslatableText(text = "Content", elementType = ElementType.PARAGRAPH, nestingLevel = 0),
            TranslatableText(text = "Item", elementType = ElementType.LIST_ITEM, nestingLevel = 1),
        )
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("heading level 2"), "Prompt should contain heading type")
        assertTrue(prompt.contains("\"type\": \"paragraph\""), "Prompt should contain paragraph type")
        assertTrue(prompt.contains("list item (nesting level: 1)"), "Prompt should contain nested list item type")
    }

    @Test
    fun buildTranslationPrompt_containsExpectedParagraphCount() {
        val texts = listOf(
            TranslatableText.fromPlainText("One"),
            TranslatableText.fromPlainText("Two"),
            TranslatableText.fromPlainText("Three"),
        )
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("3"), "Prompt should mention expected count")
    }

    @Test
    fun buildTranslationPrompt_containsJsonOutputFormat() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("\"translations\""), "Prompt should contain translations key")
        assertTrue(prompt.contains("\"translation\""), "Prompt should contain translation key")
    }

    @Test
    fun buildTranslationPrompt_containsBlockquoteDescription() {
        val texts = listOf(
            TranslatableText(text = "Quoted text", elementType = ElementType.BLOCKQUOTE, nestingLevel = 1),
        )
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("blockquote (nesting level: 1)"), "Prompt should contain blockquote type")
    }

    @Test
    fun buildTranslationPrompt_singleParagraph() {
        val texts = listOf(TranslatableText.fromPlainText("Only paragraph"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.SPANISH)

        assertTrue(prompt.contains("Spanish"), "Prompt should contain Spanish")
        assertTrue(prompt.contains("Only paragraph"), "Prompt should contain the text")
        assertTrue(prompt.contains("\"index\": 1"), "Prompt should have index 1")
    }

    // ===== Inline formatting tags in prompt (T3.4) =====

    @Test
    fun buildTranslationPrompt_containsInlineFormattingTagsSection() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("## Inline Formatting Tags"), "Prompt should contain inline formatting tags section")
    }

    @Test
    fun buildTranslationPrompt_containsAllTagTypes() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        val expectedTags = listOf("<b>", "<i>", "<code>", "<link", "<s>", "<u>", "<sup>", "<sub>", "<mono>", "<font")
        for (tag in expectedTags) {
            assertTrue(prompt.contains(tag), "Prompt should mention tag: $tag")
        }
    }

    @Test
    fun buildTranslationPrompt_containsCodeNoTranslateRule() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(
            prompt.contains("code") && prompt.contains("MUST NOT be translated"),
            "Prompt should instruct not to translate code content",
        )
    }

    @Test
    fun buildTranslationPrompt_containsLinkUrlPreserveRule() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(
            prompt.contains("URL") && prompt.contains("MUST NOT"),
            "Prompt should instruct to preserve URLs",
        )
    }

    @Test
    fun buildTranslationPrompt_containsTagPreservationExample() {
        val texts = listOf(TranslatableText.fromPlainText("Hello"))
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(prompt.contains("### Example"), "Prompt should contain an inline tag example")
        assertTrue(prompt.contains("<link href="), "Example should show link tag usage")
    }

    @Test
    fun buildTranslationPrompt_preservesXmlTagsInInputText() {
        val texts = listOf(
            TranslatableText(
                text = "Click <link href=\"https://example.com\"><b>here</b></link> for info",
                elementType = ElementType.PARAGRAPH,
                nestingLevel = 0,
            ),
        )
        val prompt = TranslationPromptBuilder.buildTranslationPrompt(texts, TranslationLanguage.CHINESE)

        assertTrue(
            prompt.contains("<link href=\\\"https://example.com\\\">"),
            "Prompt should preserve link tags in text (JSON escaped)",
        )
        assertTrue(prompt.contains("<b>here</b>"), "Prompt should preserve bold tags in text")
    }

    // ===== parseTranslationResponse with XML tags =====

    @Test
    fun parseTranslationResponse_preservesXmlTagsInTranslation() {
        val response = """
        {
          "translations": [
            {"index": 1, "translation": "点击<link href=\"https://example.com\">这里</link>获取<b>重要</b>信息"}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertTrue(result[0].contains("<link href="), "Should preserve link tag")
        assertTrue(result[0].contains("</link>"), "Should preserve closing link tag")
        assertTrue(result[0].contains("<b>"), "Should preserve bold tag")
    }

    @Test
    fun parseTranslationResponse_preservesCodeTagContent() {
        val response = """
        {
          "translations": [
            {"index": 1, "translation": "使用 <code>map()</code> 函数"}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertTrue(result[0].contains("<code>map()</code>"), "Should preserve code tag with content exactly")
    }

    // ===== parseTranslationResponse() =====

    @Test
    fun parseTranslationResponse_parsesValidJson() {
        val response = """
        {
          "targetLanguage": "Chinese",
          "translations": [
            {"index": 1, "translation": "你好"},
            {"index": 2, "translation": "世界"}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 2)

        assertEquals(listOf("你好", "世界"), result)
    }

    @Test
    fun parseTranslationResponse_parsesMarkdownWrappedJson() {
        val response = """
Here's the translation:
```json
{
  "translations": [
    {"index": 1, "translation": "Hola"}
  ]
}
```
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertEquals(listOf("Hola"), result)
    }

    @Test
    fun parseTranslationResponse_parsesMarkdownCodeBlockWithoutJsonTag() {
        val response = """
```
{
  "translations": [
    {"index": 1, "translation": "Bonjour"}
  ]
}
```
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertEquals(listOf("Bonjour"), result)
    }

    @Test
    fun parseTranslationResponse_handlesEscapedCharactersInTranslation() {
        val response = """
        {
          "translations": [
            {"index": 1, "translation": "Line 1\nLine 2"}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertEquals(listOf("Line 1\nLine 2"), result)
    }

    @Test
    fun parseTranslationResponse_handlesEscapedQuotesInTranslation() {
        val response = """
        {
          "translations": [
            {"index": 1, "translation": "He said \"hello\""}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertEquals(listOf("He said \"hello\""), result)
    }

    @Test
    fun parseTranslationResponse_throwsOnMissingTranslationsArray() {
        val response = """{"targetLanguage": "Chinese"}"""

        try {
            TranslationPromptBuilder.parseTranslationResponse(response, 1)
            assertTrue(false, "Should have thrown")
        } catch (e: Exception) {
            assertTrue(
                e.message?.contains("Failed to parse") == true,
                "Exception should mention parsing failure: ${e.message}",
            )
        }
    }

    @Test
    fun parseTranslationResponse_throwsOnWrongParagraphCount() {
        val response = """
        {
          "translations": [
            {"index": 1, "translation": "Hello"}
          ]
        }
        """.trimIndent()

        try {
            TranslationPromptBuilder.parseTranslationResponse(response, 2)
            assertTrue(false, "Should have thrown for wrong count")
        } catch (e: Exception) {
            assertTrue(
                e.message?.contains("Failed to parse") == true,
                "Exception should mention parsing failure: ${e.message}",
            )
        }
    }

    @Test
    fun parseTranslationResponse_sortsTranslationsByIndex() {
        val response = """
        {
          "translations": [
            {"index": 2, "translation": "Second"},
            {"index": 1, "translation": "First"}
          ]
        }
        """.trimIndent()

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 2)

        assertEquals(listOf("First", "Second"), result)
    }

    @Test
    fun parseTranslationResponse_handlesGarbageSurroundingJson() {
        val response = """Sure! Here you go: {"translations": [{"index": 1, "translation": "Salut"}]} Hope that helps!"""

        val result = TranslationPromptBuilder.parseTranslationResponse(response, 1)

        assertEquals(listOf("Salut"), result)
    }

    // ===== jsonEscape() =====

    @Test
    fun jsonEscape_escapesBackslash() {
        assertEquals("a\\\\b", TranslationPromptBuilder.jsonEscape("a\\b"))
    }

    @Test
    fun jsonEscape_escapesQuotes() {
        assertEquals("a\\\"b", TranslationPromptBuilder.jsonEscape("a\"b"))
    }

    @Test
    fun jsonEscape_escapesNewline() {
        assertEquals("a\\nb", TranslationPromptBuilder.jsonEscape("a\nb"))
    }

    @Test
    fun jsonEscape_escapesCarriageReturn() {
        assertEquals("a\\rb", TranslationPromptBuilder.jsonEscape("a\rb"))
    }

    @Test
    fun jsonEscape_escapesTab() {
        assertEquals("a\\tb", TranslationPromptBuilder.jsonEscape("a\tb"))
    }

    @Test
    fun jsonEscape_plainTextUnchanged() {
        assertEquals("hello world", TranslationPromptBuilder.jsonEscape("hello world"))
    }

    @Test
    fun jsonEscape_multipleSpecialCharacters() {
        val input = "He said \"hello\"\nand\ttab\\back"
        val expected = "He said \\\"hello\\\"\\nand\\ttab\\\\back"
        assertEquals(expected, TranslationPromptBuilder.jsonEscape(input))
    }

    // ===== unescapeJson() =====

    @Test
    fun unescapeJson_unescapesNewline() {
        assertEquals("a\nb", TranslationPromptBuilder.unescapeJson("a\\nb"))
    }

    @Test
    fun unescapeJson_unescapesCarriageReturn() {
        assertEquals("a\rb", TranslationPromptBuilder.unescapeJson("a\\rb"))
    }

    @Test
    fun unescapeJson_unescapesTab() {
        assertEquals("a\tb", TranslationPromptBuilder.unescapeJson("a\\tb"))
    }

    @Test
    fun unescapeJson_unescapesQuotes() {
        assertEquals("a\"b", TranslationPromptBuilder.unescapeJson("a\\\"b"))
    }

    @Test
    fun unescapeJson_unescapesBackslash() {
        assertEquals("a\\b", TranslationPromptBuilder.unescapeJson("a\\\\b"))
    }

    @Test
    fun unescapeJson_plainTextUnchanged() {
        assertEquals("hello world", TranslationPromptBuilder.unescapeJson("hello world"))
    }

    @Test
    fun unescapeJson_reverseOfJsonEscape() {
        val original = "Hello \"world\"\nNew line\ttab\\back"
        val escaped = TranslationPromptBuilder.jsonEscape(original)
        val unescaped = TranslationPromptBuilder.unescapeJson(escaped)
        assertEquals(original, unescaped)
    }

    // ===== extractJsonFromResponse() =====

    @Test
    fun extractJsonFromResponse_extractsFromJsonCodeBlock() {
        val input = """
Here's the result:
```json
{"translations": []}
```
        """.trimIndent()

        assertEquals("{\"translations\": []}", TranslationPromptBuilder.extractJsonFromResponse(input))
    }

    @Test
    fun extractJsonFromResponse_extractsFromPlainCodeBlock() {
        val input = """
```
{"translations": []}
```
        """.trimIndent()

        assertEquals("{\"translations\": []}", TranslationPromptBuilder.extractJsonFromResponse(input))
    }

    @Test
    fun extractJsonFromResponse_extractsFromBraces() {
        val input = """Some text {"translations": []} more text"""

        assertEquals("{\"translations\": []}", TranslationPromptBuilder.extractJsonFromResponse(input))
    }

    @Test
    fun extractJsonFromResponse_returnsPlainJsonUnchanged() {
        val input = """{"translations": []}"""

        assertEquals("{\"translations\": []}", TranslationPromptBuilder.extractJsonFromResponse(input))
    }

    @Test
    fun extractJsonFromResponse_trimsWhitespace() {
        val input = """   {"translations": []}   """

        assertEquals("{\"translations\": []}", TranslationPromptBuilder.extractJsonFromResponse(input))
    }

    // ===== handleTranslationError() =====

    @Test
    fun handleTranslationError_rateLimitMessage() {
        val error = Exception("Rate limit exceeded by API")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("Rate limit", ignoreCase = true), "Should mention rate limit: $result")
    }

    @Test
    fun handleTranslationError_invalidApiKeyMessage() {
        val error = Exception("invalid api key provided")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("API key", ignoreCase = true), "Should mention API key: $result")
    }

    @Test
    fun handleTranslationError_socketTimeoutMessage() {
        val error = SocketTimeoutException("Connection timed out")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("timed out", ignoreCase = true), "Should mention timeout: $result")
    }

    @Test
    fun handleTranslationError_timeoutInMessage() {
        val error = Exception("Request timeout occurred")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("timed out", ignoreCase = true), "Should mention timeout: $result")
    }

    @Test
    fun handleTranslationError_insufficientQuota() {
        val error = Exception("insufficient quota for this request")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("quota", ignoreCase = true), "Should mention quota: $result")
    }

    @Test
    fun handleTranslationError_quotaExceeded() {
        val error = Exception("quota exceeded")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("quota", ignoreCase = true), "Should mention quota: $result")
    }

    @Test
    fun handleTranslationError_genericError() {
        val error = Exception("Something went wrong")
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("Something went wrong"), "Should include error message: $result")
    }

    @Test
    fun handleTranslationError_nullMessage() {
        val error = Exception()
        val result = TranslationPromptBuilder.handleTranslationError(error)

        assertTrue(result.contains("Unknown error"), "Should handle null message: $result")
    }
}
