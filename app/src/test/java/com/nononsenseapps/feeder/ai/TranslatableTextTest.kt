package com.nononsenseapps.feeder.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TranslatableText data class.
 *
 * Tests structure metadata handling and formatting for translation prompts.
 */
class TranslatableTextTest {
    @Test
    fun testTranslatableTextCreation() {
        val tt =
            TranslatableText(
                text = "Hello world",
                elementType = ElementType.PARAGRAPH,
                nestingLevel = 0,
            )

        assertEquals("Hello world", tt.text)
        assertEquals(ElementType.PARAGRAPH, tt.elementType)
        assertEquals(0, tt.nestingLevel)
    }

    @Test
    fun testGetStructureDescription_Paragraph() {
        val tt =
            TranslatableText(
                text = "This is a paragraph",
                elementType = ElementType.PARAGRAPH,
                nestingLevel = 0,
            )

        assertEquals("paragraph", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_Heading1() {
        val tt =
            TranslatableText(
                text = "Main Title",
                elementType = ElementType.HEADING_1,
                nestingLevel = 0,
            )

        assertEquals("heading level 1", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_Heading6() {
        val tt =
            TranslatableText(
                text = "Minor Title",
                elementType = ElementType.HEADING_6,
                nestingLevel = 0,
            )

        assertEquals("heading level 6", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_ListItem_ZeroNesting() {
        val tt =
            TranslatableText(
                text = "First item",
                elementType = ElementType.LIST_ITEM,
                nestingLevel = 0,
            )

        assertEquals("list item", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_ListItem_Nested() {
        val tt =
            TranslatableText(
                text = "Nested item",
                elementType = ElementType.LIST_ITEM,
                nestingLevel = 2,
            )

        assertEquals("list item (nesting level: 2)", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_Blockquote_Nested() {
        val tt =
            TranslatableText(
                text = "Quoted text",
                elementType = ElementType.BLOCKQUOTE,
                nestingLevel = 1,
            )

        assertEquals("blockquote (nesting level: 1)", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_TableCell() {
        val tt =
            TranslatableText(
                text = "cell content",
                elementType = ElementType.TABLE_CELL,
                nestingLevel = 0,
            )

        assertEquals("table cell", tt.getStructureDescription())
    }

    @Test
    fun testGetStructureDescription_ImageCaption() {
        val tt =
            TranslatableText(
                text = "caption text",
                elementType = ElementType.IMAGE_CAPTION,
                nestingLevel = 0,
            )

        assertEquals("image caption", tt.getStructureDescription())
    }

    @Test
    fun testWithStructurePrefix_Paragraph() {
        val tt =
            TranslatableText(
                text = "Hello world",
                elementType = ElementType.PARAGRAPH,
                nestingLevel = 0,
            )

        assertEquals("[paragraph] Hello world", tt.withStructurePrefix())
    }

    @Test
    fun testWithStructurePrefix_Heading() {
        val tt =
            TranslatableText(
                text = "Introduction",
                elementType = ElementType.HEADING_2,
                nestingLevel = 0,
            )

        assertEquals("[heading level 2] Introduction", tt.withStructurePrefix())
    }

    @Test
    fun testWithStructurePrefix_NestedListItem() {
        val tt =
            TranslatableText(
                text = "Subpoint",
                elementType = ElementType.LIST_ITEM,
                nestingLevel = 1,
            )

        assertEquals("[list item (nesting level: 1)] Subpoint", tt.withStructurePrefix())
    }

    @Test
    fun testFromPlainText() {
        val tt = TranslatableText.fromPlainText("Sample text")

        assertEquals("Sample text", tt.text)
        assertEquals(ElementType.PARAGRAPH, tt.elementType)
        assertEquals(0, tt.nestingLevel)
    }

    @Test
    fun testAllElementTypesHaveDescriptions() {
        val elementTypes = ElementType.entries

        for (elementType in elementTypes) {
            val tt =
                TranslatableText(
                    text = "Test",
                    elementType = elementType,
                    nestingLevel = 0,
                )

            val description = tt.getStructureDescription()
            assertTrue(description.isNotEmpty(), "Element type $elementType should have a description")
        }
    }
}
