package com.nononsenseapps.feeder.ai

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for InlineTagParser.
 *
 * Tests the core parsing logic via the internal parseForTest() function
 * which doesn't require a @Composable context.
 */
class InlineTagParserTest {
    // ===== Plain text (no tags) =====

    @Test
    fun parse_plainText_returnsUnstyledAnnotatedString() {
        val result = callParse("Hello world")

        assertEquals("Hello world", result.text)
        assertTrue(result.spanStyles.isEmpty(), "Should have no span styles")
    }

    @Test
    fun parse_emptyString() {
        val result = callParse("")
        assertEquals("", result.text)
    }

    // ===== Individual tag types =====

    @Test
    fun parse_boldTag() {
        val result = callParse("<b>bold</b> text")

        assertEquals("bold text", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(4, result.spanStyles[0].end)
    }

    @Test
    fun parse_italicTag() {
        val result = callParse("<i>italic</i> text")

        assertEquals("italic text", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(FontStyle.Italic, result.spanStyles[0].item.fontStyle)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(6, result.spanStyles[0].end)
    }

    @Test
    fun parse_codeTag() {
        val result = callParse("use <code>map()</code> here")

        assertEquals("use map() here", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Code tag should produce a span style")
        assertEquals(4, result.spanStyles[0].start) // "use " = 4 chars
        assertEquals(9, result.spanStyles[0].end) // "map()" = 5 chars
    }

    @Test
    fun parse_strikethroughTag() {
        val result = callParse("<s>old</s> new")

        assertEquals("old new", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(TextDecoration.LineThrough, result.spanStyles[0].item.textDecoration)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(3, result.spanStyles[0].end)
    }

    @Test
    fun parse_underlineTag() {
        val result = callParse("<u>underlined</u>")

        assertEquals("underlined", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(TextDecoration.Underline, result.spanStyles[0].item.textDecoration)
    }

    @Test
    fun parse_superscriptTag() {
        val result = callParse("E<sup>2</sup>")

        assertEquals("E2", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(BaselineShift.Superscript, result.spanStyles[0].item.baselineShift)
        assertEquals(1, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)
    }

    @Test
    fun parse_subscriptTag() {
        val result = callParse("H<sub>2</sub>O")

        assertEquals("H2O", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(BaselineShift.Subscript, result.spanStyles[0].item.baselineShift)
        assertEquals(1, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)
    }

    @Test
    fun parse_monoTag() {
        val result = callParse("<mono>monospaced</mono>")

        assertEquals("monospaced", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Mono tag should produce a span style")
    }

    @Test
    fun parse_fontTag() {
        val result = callParse("<font face=\"serif\">text</font>")

        assertEquals("text", result.text)
        // Font tag produces a span style when face resolves to a valid font family
    }

    @Test
    fun parse_linkTag() {
        var clickedUrl: String? = null
        val result =
            callParse(
                "<link href=\"https://example.com\">click me</link>",
                onLinkClick = { url -> clickedUrl = url },
            )

        assertEquals("click me", result.text)
        // Link uses pushLink, not pushStyle - check link annotations
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertTrue(linkAnnotations.isNotEmpty(), "Link should have a link annotation")
    }

    // ===== Nested tags =====

    @Test
    fun parse_nestedBoldInsideLink() {
        val result = callParse("<link href=\"https://example.com\"><b>bold link</b></link>")

        assertEquals("bold link", result.text)
        val boldStyles = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue(boldStyles.isNotEmpty(), "Should have bold style")
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertTrue(linkAnnotations.isNotEmpty(), "Should have link annotation")
    }

    @Test
    fun parse_multipleAdjacentTags() {
        val result = callParse("<b>bold</b> and <i>italic</i>")

        assertEquals("bold and italic", result.text)
        assertEquals(2, result.spanStyles.size)
    }

    // ===== Entity unescaping =====

    @Test
    fun parse_unescapesAmpEntity() {
        val result = callParse("a &amp; b")
        assertEquals("a & b", result.text)
    }

    @Test
    fun parse_unescapesLtEntity() {
        val result = callParse("a &lt; b")
        assertEquals("a < b", result.text)
    }

    @Test
    fun parse_unescapesGtEntity() {
        val result = callParse("a &gt; b")
        assertEquals("a > b", result.text)
    }

    @Test
    fun parse_unescapesQuotEntity() {
        val result = callParse("He said &quot;hello&quot;")
        assertEquals("He said \"hello\"", result.text)
    }

    @Test
    fun parse_multipleEntities() {
        val result = callParse("a &lt; b &amp;&amp; c &gt; d")
        assertEquals("a < b && c > d", result.text)
    }

    @Test
    fun parse_entitiesInsideTags() {
        val result = callParse("<b>a &amp; b</b>")
        assertEquals("a & b", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Bold style should be present")
    }

    // ===== Fallback/error tolerance =====

    @Test
    fun parse_unknownTag_renderedAsLiteralText() {
        val result = callParse("<script>alert()</script>")
        assertEquals("<script>alert()</script>", result.text)
    }

    @Test
    fun parse_unclosedTag_styleAppliedToEnd() {
        val result = callParse("<b>bold text without close")

        assertEquals("bold text without close", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Unclosed tag should still apply style")
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)
    }

    @Test
    fun parse_mismatchedCloseTag_ignored() {
        val result = callParse("<b>text</i>")

        assertEquals("text", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Bold style should still apply")
    }

    @Test
    fun parse_emptyTags() {
        val result = callParse("<b></b>")
        assertEquals("", result.text)
    }

    @Test
    fun parse_tagsAtBoundaries() {
        val result = callParse("<b>entire string</b>")

        assertEquals("entire string", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(result.text.length, result.spanStyles[0].end)
    }

    @Test
    fun parse_codeTagContentPreserved() {
        val result = callParse("use <code>map()</code> function")
        assertEquals("use map() function", result.text)
    }

    @Test
    fun parse_neverThrows_withGarbledInput() {
        val inputs =
            listOf(
                "<",
                ">",
                "<>",
                "</",
                "<b",
                "<<b>>",
                "<b>text<b>",
                "</b>",
                "<<<>>>",
                "<b><i>text</b></i>",
                "<link href=>text</link>",
                "<link>no href</link>",
                "normal <  text > here",
                "&",
                "&amp",
                "&invalid;",
            )

        for (input in inputs) {
            try {
                callParse(input) // Should never throw
            } catch (e: Exception) {
                assertTrue(false, "parse() should NEVER throw, but threw for input '$input': ${e.message}")
            }
        }
    }

    @Test
    fun parse_linkHrefAttributeParsing() {
        val result = callParse("<link href=\"https://example.com/path?q=1&amp;p=2\">text</link>")
        assertEquals("text", result.text)
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertTrue(linkAnnotations.isNotEmpty(), "Should have link annotation")
    }

    @Test
    fun parse_fontFaceAttributeParsing() {
        val result = callParse("<font face=\"Times New Roman\">styled</font>")
        assertEquals("styled", result.text)
    }

    // ===== Helper =====

    private fun callParse(
        text: String,
        onLinkClick: (url: String) -> Unit = {},
    ): AnnotatedString = InlineTagParser.parseForTest(text, onLinkClick)
}
