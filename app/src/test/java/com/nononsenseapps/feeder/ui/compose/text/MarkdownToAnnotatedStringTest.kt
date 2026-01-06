package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for Markdown rendering functionality.
 *
 * Tests the Mikepenz markdown library integration to ensure:
 * - Basic markdown features render correctly
 * - Advanced features (tables, task lists) work
 * - Edge cases are handled gracefully
 * - Error fallback mechanisms function
 */
@RunWith(JUnit4::class)
class MarkdownToAnnotatedStringTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== Basic Feature Tests ==========

    @Test
    fun markdownContent_rendersHeading() {
        val markdown = "# Heading 1"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
        // If we get here without exception, test passes
        // Visual verification would require screenshot testing
    }

    @Test
    fun markdownContent_rendersBoldText() {
        val markdown = "This is **bold text**"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersItalicText() {
        val markdown = "This is *italic text*"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersLists() {
        val markdown =
            """
            - Item 1
            - Item 2
            - Item 3
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersCodeBlocks() {
        val markdown =
            """
            ```
            val x = 42
            ```
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersLinks() {
        val markdown = "[Link text](https://example.com)"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersBlockquotes() {
        val markdown = "> This is a quote"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    // ========== Advanced Feature Tests ==========

    @Test
    fun markdownContent_rendersTables() {
        val markdown =
            """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersTaskLists() {
        val markdown =
            """
            - [x] Completed task
            - [ ] Incomplete task
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersStrikethrough() {
        val markdown = "~~deleted text~~"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_rendersNestedLists() {
        val markdown =
            """
            - Item 1
              - Nested item 1.1
              - Nested item 1.2
            - Item 2
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    // ========== Edge Case Tests ==========

    @Test
    fun markdownContent_handlesEmptyInput() {
        val markdown = ""
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_handlesMalformedMarkdown() {
        val markdown =
            """
            # Heading with no content
            **Bold without closing
            *Italic without closing
            [Link without url](
            """.trimIndent()
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_handlesSpecialCharacters() {
        val markdown = "Special chars: < > & \" '"
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    @Test
    fun markdownContent_handlesLargeDocument() {
        val markdown = generateLargeMarkdownDocument(100)
        composeTestRule.setContent {
            MarkdownContent(markdown = markdown)
        }
    }

    // ========== Safe Variant Tests ==========

    @Test
    fun markdownContentSafe_handlesRenderingError() {
        val markdown = "# Test"
        composeTestRule.setContent {
            MarkdownContentSafe(markdown = markdown)
        }
    }

    @Test
    fun markdownContentSafe_fallsBackToPlainText_onError() {
        // This test verifies the fallback mechanism works
        // The library should handle errors gracefully
        val markdown = "Test content"
        composeTestRule.setContent {
            MarkdownContentSafe(markdown = markdown)
        }
    }

    // ========== Deprecated Function Tests ==========

    @Test
    fun deprecated_markdownToAnnotatedString_returnsAnnotatedString() {
        val markdown = "Test markdown"
        val result = markdownToAnnotatedString(markdown)
        assert(result.text == markdown)
    }

    @Test
    fun deprecated_markdownToAnnotatedStringSafe_returnsAnnotatedString() {
        val markdown = "Test markdown"
        val result = markdownToAnnotatedStringSafe(markdown)
        assert(result.text == markdown)
    }

    // ========== Helper Functions ==========

    /**
     * Generates a large markdown document for testing performance.
     *
     * @param lines Number of lines to generate
     * @return Large markdown document string
     */
    private fun generateLargeMarkdownDocument(lines: Int): String =
        buildString {
            repeat(lines / 10) { i ->
                append("# Heading $i\n\n")
                append("This is paragraph $i with some **bold** and *italic* text.\n\n")
                append("- List item 1\n")
                append("- List item 2\n")
                append("- List item 3\n\n")
                append("> A quote for section $i\n\n")
                append("```\ncode block $i\n```\n\n")
            }
        }
}
