package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.model.html.LinearAudio
import com.nononsenseapps.feeder.model.html.LinearAudioSource
import com.nononsenseapps.feeder.model.html.LinearBlockQuote
import com.nononsenseapps.feeder.model.html.LinearElement
import com.nononsenseapps.feeder.model.html.LinearImage
import com.nononsenseapps.feeder.model.html.LinearListItem
import com.nononsenseapps.feeder.model.html.LinearTable
import com.nononsenseapps.feeder.model.html.LinearTableCellItem
import com.nononsenseapps.feeder.model.html.LinearTableCellItemType
import com.nononsenseapps.feeder.model.html.LinearText
import com.nononsenseapps.feeder.model.html.LinearTextAnnotation
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationBold
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationCode
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationFont
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH1
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH2
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationItalic
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationLink
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationMonospace
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationStrikethrough
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationSubscript
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationSuperscript
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationUnderline
import com.nononsenseapps.feeder.model.html.LinearTextBlockStyle
import com.nononsenseapps.feeder.model.html.LinearVideo
import com.nononsenseapps.feeder.model.html.LinearVideoSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslatableTextExtractorTest {
    // ===== toTaggedText() - Individual annotation types =====

    @Test
    fun toTaggedText_bold() {
        // "Hello" at 0-4 (inclusive)
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 0, end = 4),
            )
        val result = TranslatableTextExtractor.toTaggedText("Hello world", annotations)
        assertEquals("<b>Hello</b> world", result)
    }

    @Test
    fun toTaggedText_italic() {
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationItalic, start = 0, end = 4),
            )
        val result = TranslatableTextExtractor.toTaggedText("Hello world", annotations)
        assertEquals("<i>Hello</i> world", result)
    }

    @Test
    fun toTaggedText_code() {
        // "map()" at indices 5-9 in "Call map() now"
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationCode, start = 5, end = 9),
            )
        val result = TranslatableTextExtractor.toTaggedText("Call map() now", annotations)
        assertEquals("Call <code>map()</code> now", result)
    }

    @Test
    fun toTaggedText_link() {
        // "here" at indices 6-9 in "Click here please"
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationLink(href = "https://example.com"), start = 6, end = 9),
            )
        val result = TranslatableTextExtractor.toTaggedText("Click here please", annotations)
        assertEquals("Click <link href=\"https://example.com\">here</link> please", result)
    }

    @Test
    fun toTaggedText_strikethrough() {
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationStrikethrough, start = 0, end = 2),
            )
        val result = TranslatableTextExtractor.toTaggedText("old new", annotations)
        assertEquals("<s>old</s> new", result)
    }

    @Test
    fun toTaggedText_underline() {
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationUnderline, start = 0, end = 8),
            )
        val result = TranslatableTextExtractor.toTaggedText("important", annotations)
        assertEquals("<u>important</u>", result)
    }

    @Test
    fun toTaggedText_superscript() {
        // "2" at index 1 in "E2"
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationSuperscript, start = 1, end = 1),
            )
        val result = TranslatableTextExtractor.toTaggedText("E2", annotations)
        assertEquals("E<sup>2</sup>", result)
    }

    @Test
    fun toTaggedText_subscript() {
        // "2" at index 1 in "H2O"
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationSubscript, start = 1, end = 1),
            )
        val result = TranslatableTextExtractor.toTaggedText("H2O", annotations)
        assertEquals("H<sub>2</sub>O", result)
    }

    @Test
    fun toTaggedText_monospace() {
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationMonospace, start = 0, end = 2),
            )
        val result = TranslatableTextExtractor.toTaggedText("foo bar", annotations)
        assertEquals("<mono>foo</mono> bar", result)
    }

    @Test
    fun toTaggedText_font() {
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationFont(face = "serif"), start = 0, end = 3),
            )
        val result = TranslatableTextExtractor.toTaggedText("text here", annotations)
        assertEquals("<font face=\"serif\">text</font> here", result)
    }

    // ===== toTaggedText() - Special cases =====

    @Test
    fun toTaggedText_noAnnotations_returnsEscapedPlainText() {
        val result = TranslatableTextExtractor.toTaggedText("Hello world", emptyList())
        assertEquals("Hello world", result)
    }

    @Test
    fun toTaggedText_xmlSpecialCharactersEscaped() {
        val result = TranslatableTextExtractor.toTaggedText("a < b && c > d", emptyList())
        assertEquals("a &lt; b &amp;&amp; c &gt; d", result)
    }

    @Test
    fun toTaggedText_xmlEscapingWithAnnotations() {
        // "a < b" with bold on "a" (index 0)
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 0, end = 0),
            )
        val result = TranslatableTextExtractor.toTaggedText("a < b", annotations)
        assertEquals("<b>a</b> &lt; b", result)
    }

    @Test
    fun toTaggedText_nestedAnnotations_boldInsideLink() {
        // "Click here now" with Link covering wider range, Bold on subset
        // Link covers "Click here" (0-9), Bold covers "here" (6-9)
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationLink(href = "https://example.com"), start = 0, end = 9),
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 6, end = 9),
            )
        val result = TranslatableTextExtractor.toTaggedText("Click here now", annotations)
        // Link (wider) wraps Bold (narrower)
        assertEquals("<link href=\"https://example.com\">Click <b>here</b></link> now", result)
    }

    @Test
    fun toTaggedText_adjacentAnnotations() {
        // "bold italic" with Bold on "bold" (0-3) and Italic on "italic" (5-10)
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 0, end = 3),
                LinearTextAnnotation(data = LinearTextAnnotationItalic, start = 5, end = 10),
            )
        val result = TranslatableTextExtractor.toTaggedText("bold italic", annotations)
        assertEquals("<b>bold</b> <i>italic</i>", result)
    }

    @Test
    fun toTaggedText_entireStringAnnotated() {
        val text = "entire string"
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 0, end = text.length - 1),
            )
        val result = TranslatableTextExtractor.toTaggedText(text, annotations)
        assertEquals("<b>entire string</b>", result)
    }

    @Test
    fun toTaggedText_headingAnnotationsFiltered() {
        // H2 annotation should NOT produce a tag (it determines ElementType, not inline formatting)
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationH2, start = 0, end = 4),
            )
        val result = TranslatableTextExtractor.toTaggedText("Title", annotations)
        assertEquals("Title", result)
    }

    @Test
    fun toTaggedText_headingAndInlineAnnotationsMixed() {
        // H2 annotation for element type detection + Bold for inline formatting
        val annotations =
            listOf(
                LinearTextAnnotation(data = LinearTextAnnotationH2, start = 0, end = 9),
                LinearTextAnnotation(data = LinearTextAnnotationBold, start = 0, end = 9),
            )
        val result = TranslatableTextExtractor.toTaggedText("Bold Title", annotations)
        // Only Bold tag should appear, H2 is filtered
        assertEquals("<b>Bold Title</b>", result)
    }

    @Test
    fun toTaggedText_emptyString() {
        val result = TranslatableTextExtractor.toTaggedText("", emptyList())
        assertEquals("", result)
    }

    // ===== extract() - LinearText =====

    @Test
    fun extract_simpleLinearText() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "Hello world",
                    blockStyle = LinearTextBlockStyle.TEXT,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].text)
        assertEquals(ElementType.PARAGRAPH, result[0].elementType)
        assertEquals(0, result[0].nestingLevel)
    }

    @Test
    fun extract_linearTextWithAnnotations() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "Click here",
                    blockStyle = LinearTextBlockStyle.TEXT,
                    LinearTextAnnotation(data = LinearTextAnnotationLink(href = "https://example.com"), start = 6, end = 9),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Click <link href=\"https://example.com\">here</link>", result[0].text)
    }

    @Test
    fun extract_linearTextWithHeadingAnnotation() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "Main Title",
                    blockStyle = LinearTextBlockStyle.TEXT,
                    LinearTextAnnotation(data = LinearTextAnnotationH1, start = 0, end = 9),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Main Title", result[0].text)
        assertEquals(ElementType.HEADING_1, result[0].elementType)
    }

    @Test
    fun extract_skipsPreformattedText() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "code here",
                    blockStyle = LinearTextBlockStyle.PRE_FORMATTED,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_skipsCodeBlockText() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "code block",
                    blockStyle = LinearTextBlockStyle.CODE_BLOCK,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_skipsBlankText() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "   ",
                    blockStyle = LinearTextBlockStyle.TEXT,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_trimsText() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "  Hello world  ",
                    blockStyle = LinearTextBlockStyle.TEXT,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].text)
    }

    // ===== extract() - LinearListItem =====

    @Test
    fun extract_linearListItem_singleLevel() {
        val elements =
            listOf<LinearElement>(
                LinearListItem(
                    ids = emptySet(),
                    orderedIndex = 1,
                    LinearText(
                        ids = emptySet(),
                        text = "First item",
                        blockStyle = LinearTextBlockStyle.TEXT,
                    ),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("First item", result[0].text)
        assertEquals(ElementType.LIST_ITEM, result[0].elementType)
        assertEquals(1, result[0].nestingLevel)
    }

    @Test
    fun extract_linearListItem_nestedThreeLevels() {
        val elements =
            listOf<LinearElement>(
                LinearListItem(
                    ids = emptySet(),
                    orderedIndex = null,
                    // Level 1 - list item contains nested list item
                    LinearListItem(
                        ids = emptySet(),
                        orderedIndex = null,
                        // Level 2 - nested list item contains further nested list item
                        LinearListItem(
                            ids = emptySet(),
                            orderedIndex = null,
                            // Level 3
                            LinearText(
                                ids = emptySet(),
                                text = "Deeply nested",
                                blockStyle = LinearTextBlockStyle.TEXT,
                            ),
                        ),
                    ),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Deeply nested", result[0].text)
        assertEquals(ElementType.LIST_ITEM, result[0].elementType)
        assertEquals(3, result[0].nestingLevel)
    }

    @Test
    fun extract_linearListItem_multipleItems() {
        val elements =
            listOf<LinearElement>(
                LinearListItem(
                    ids = emptySet(),
                    orderedIndex = 1,
                    LinearText(ids = emptySet(), text = "Item 1", blockStyle = LinearTextBlockStyle.TEXT),
                ),
                LinearListItem(
                    ids = emptySet(),
                    orderedIndex = 2,
                    LinearText(ids = emptySet(), text = "Item 2", blockStyle = LinearTextBlockStyle.TEXT),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(2, result.size)
        assertEquals("Item 1", result[0].text)
        assertEquals("Item 2", result[1].text)
        assertEquals(ElementType.LIST_ITEM, result[0].elementType)
        assertEquals(ElementType.LIST_ITEM, result[1].elementType)
        assertEquals(1, result[0].nestingLevel)
        assertEquals(1, result[1].nestingLevel)
    }

    // ===== extract() - LinearBlockQuote =====

    @Test
    fun extract_linearBlockQuote() {
        val elements =
            listOf<LinearElement>(
                LinearBlockQuote(
                    ids = emptySet(),
                    cite = null,
                    LinearText(
                        ids = emptySet(),
                        text = "Quoted text",
                        blockStyle = LinearTextBlockStyle.TEXT,
                    ),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Quoted text", result[0].text)
        assertEquals(ElementType.BLOCKQUOTE, result[0].elementType)
        assertEquals(1, result[0].nestingLevel)
    }

    // ===== extract() - LinearTable =====

    @Test
    fun extract_linearTable_singleCell() {
        val table =
            LinearTable.build(ids = emptySet(), leftToRight = true) {
                newRow()
                add(
                    LinearTableCellItem.build(colSpan = 1, rowSpan = 1, type = LinearTableCellItemType.DATA) {
                        add(LinearText(ids = emptySet(), text = "Cell content", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
            }
        val elements = listOf<LinearElement>(table)
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Cell content", result[0].text)
        assertEquals(ElementType.TABLE_CELL, result[0].elementType)
    }

    @Test
    fun extract_linearTable_multipleCells() {
        val table =
            LinearTable.build(ids = emptySet(), leftToRight = true) {
                newRow()
                add(
                    LinearTableCellItem.build(colSpan = 1, rowSpan = 1, type = LinearTableCellItemType.HEADER) {
                        add(LinearText(ids = emptySet(), text = "Header 1", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
                add(
                    LinearTableCellItem.build(colSpan = 1, rowSpan = 1, type = LinearTableCellItemType.HEADER) {
                        add(LinearText(ids = emptySet(), text = "Header 2", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
                newRow()
                add(
                    LinearTableCellItem.build(colSpan = 1, rowSpan = 1, type = LinearTableCellItemType.DATA) {
                        add(LinearText(ids = emptySet(), text = "Data 1", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
                add(
                    LinearTableCellItem.build(colSpan = 1, rowSpan = 1, type = LinearTableCellItemType.DATA) {
                        add(LinearText(ids = emptySet(), text = "Data 2", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
            }
        val elements = listOf<LinearElement>(table)
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(4, result.size)
        assertEquals("Header 1", result[0].text)
        assertEquals("Header 2", result[1].text)
        assertEquals("Data 1", result[2].text)
        assertEquals("Data 2", result[3].text)
        // All should be TABLE_CELL type
        result.forEach { assertEquals(ElementType.TABLE_CELL, it.elementType) }
    }

    @Test
    fun extract_linearTable_skipsFillerCells() {
        // A table with a cell spanning 2 cols has a filler in the second position
        val table =
            LinearTable.build(ids = emptySet(), leftToRight = true) {
                newRow()
                add(
                    LinearTableCellItem.build(colSpan = 2, rowSpan = 1, type = LinearTableCellItemType.DATA) {
                        add(LinearText(ids = emptySet(), text = "Spanning cell", blockStyle = LinearTextBlockStyle.TEXT))
                    },
                )
            }
        val elements = listOf<LinearElement>(table)
        val result = TranslatableTextExtractor.extract(elements)

        // Only 1 result (filler cell should be skipped)
        assertEquals(1, result.size)
        assertEquals("Spanning cell", result[0].text)
    }

    // ===== extract() - LinearImage =====

    @Test
    fun extract_linearImage_withCaption() {
        val elements =
            listOf<LinearElement>(
                LinearImage(
                    ids = emptySet(),
                    sources = emptyList(),
                    caption =
                        LinearText(
                            ids = emptySet(),
                            text = "Photo by John",
                            blockStyle = LinearTextBlockStyle.TEXT,
                        ),
                    link = null,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Photo by John", result[0].text)
        assertEquals(ElementType.IMAGE_CAPTION, result[0].elementType)
    }

    @Test
    fun extract_linearImage_withAnnotatedCaption() {
        val elements =
            listOf<LinearElement>(
                LinearImage(
                    ids = emptySet(),
                    sources = emptyList(),
                    caption =
                        LinearText(
                            ids = emptySet(),
                            text = "Photo by John",
                            blockStyle = LinearTextBlockStyle.TEXT,
                            LinearTextAnnotation(data = LinearTextAnnotationBold, start = 9, end = 12),
                        ),
                    link = null,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(1, result.size)
        assertEquals("Photo by <b>John</b>", result[0].text)
        assertEquals(ElementType.IMAGE_CAPTION, result[0].elementType)
    }

    @Test
    fun extract_linearImage_withoutCaption() {
        val elements =
            listOf<LinearElement>(
                LinearImage(
                    ids = emptySet(),
                    sources = emptyList(),
                    caption = null,
                    link = null,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_linearImage_withBlankCaption() {
        val elements =
            listOf<LinearElement>(
                LinearImage(
                    ids = emptySet(),
                    sources = emptyList(),
                    caption =
                        LinearText(
                            ids = emptySet(),
                            text = "   ",
                            blockStyle = LinearTextBlockStyle.TEXT,
                        ),
                    link = null,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_linearImage_withPreformattedCaption_skips() {
        val elements =
            listOf<LinearElement>(
                LinearImage(
                    ids = emptySet(),
                    sources = emptyList(),
                    caption =
                        LinearText(
                            ids = emptySet(),
                            text = "Not a regular caption",
                            blockStyle = LinearTextBlockStyle.PRE_FORMATTED,
                        ),
                    link = null,
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    // ===== extract() - Skipped elements =====

    @Test
    fun extract_skipsLinearAudio() {
        val elements =
            listOf<LinearElement>(
                LinearAudio(
                    ids = emptySet(),
                    sources = listOf(LinearAudioSource(uri = "audio.mp3", mimeType = "audio/mpeg")),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_skipsLinearVideo() {
        val elements =
            listOf<LinearElement>(
                LinearVideo(
                    ids = emptySet(),
                    sources =
                        listOf(
                            LinearVideoSource(
                                uri = "video.mp4",
                                link = "video.mp4",
                                imageThumbnail = null,
                                widthPx = null,
                                heightPx = null,
                                mimeType = "video/mp4",
                            ),
                        ),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }

    // ===== extract() - Mixed document =====

    @Test
    fun extract_mixedDocument_preservesDocumentOrder() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "Introduction",
                    blockStyle = LinearTextBlockStyle.TEXT,
                    LinearTextAnnotation(data = LinearTextAnnotationH2, start = 0, end = 11),
                ),
                LinearText(
                    ids = emptySet(),
                    text = "First paragraph",
                    blockStyle = LinearTextBlockStyle.TEXT,
                ),
                LinearListItem(
                    ids = emptySet(),
                    orderedIndex = 1,
                    LinearText(ids = emptySet(), text = "List item 1", blockStyle = LinearTextBlockStyle.TEXT),
                ),
                LinearBlockQuote(
                    ids = emptySet(),
                    cite = null,
                    LinearText(ids = emptySet(), text = "A quote", blockStyle = LinearTextBlockStyle.TEXT),
                ),
            )

        val result = TranslatableTextExtractor.extract(elements)

        assertEquals(4, result.size)
        assertEquals("Introduction", result[0].text)
        assertEquals(ElementType.HEADING_2, result[0].elementType)
        assertEquals("First paragraph", result[1].text)
        assertEquals(ElementType.PARAGRAPH, result[1].elementType)
        assertEquals("List item 1", result[2].text)
        assertEquals(ElementType.LIST_ITEM, result[2].elementType)
        assertEquals(1, result[2].nestingLevel)
        assertEquals("A quote", result[3].text)
        assertEquals(ElementType.BLOCKQUOTE, result[3].elementType)
        assertEquals(1, result[3].nestingLevel)
    }

    @Test
    fun extract_emptyElements() {
        val result = TranslatableTextExtractor.extract(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun extract_onlySkippedElements() {
        val elements =
            listOf<LinearElement>(
                LinearText(
                    ids = emptySet(),
                    text = "code block",
                    blockStyle = LinearTextBlockStyle.CODE_BLOCK,
                ),
                LinearAudio(
                    ids = emptySet(),
                    sources = listOf(LinearAudioSource(uri = "audio.mp3", mimeType = "audio/mpeg")),
                ),
            )
        val result = TranslatableTextExtractor.extract(elements)
        assertTrue(result.isEmpty())
    }
}
