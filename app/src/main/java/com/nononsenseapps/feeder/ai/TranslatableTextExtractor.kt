package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.model.html.LinearBlockQuote
import com.nononsenseapps.feeder.model.html.LinearElement
import com.nononsenseapps.feeder.model.html.LinearImage
import com.nononsenseapps.feeder.model.html.LinearListItem
import com.nononsenseapps.feeder.model.html.LinearTable
import com.nononsenseapps.feeder.model.html.LinearText
import com.nononsenseapps.feeder.model.html.LinearTextAnnotation
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationBold
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationCode
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationFont
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH1
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH2
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH3
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH4
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH5
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationH6
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationItalic
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationLink
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationMonospace
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationStrikethrough
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationSubscript
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationSuperscript
import com.nononsenseapps.feeder.model.html.LinearTextAnnotationUnderline
import com.nononsenseapps.feeder.model.html.LinearTextBlockStyle

/**
 * Extracts translatable text from LinearElement tree, converting LinearTextAnnotation
 * ranges to XML-like inline tags for formatting preservation through translation.
 */
object TranslatableTextExtractor {
    /**
     * Extracts all translatable text from a list of LinearElement items.
     */
    fun extract(elements: List<LinearElement>): List<TranslatableText> {
        val result = mutableListOf<TranslatableText>()
        extractRecursively(elements, result, nestingLevel = 0, defaultElementType = ElementType.PARAGRAPH)
        return result
    }

    private fun extractRecursively(
        elements: List<LinearElement>,
        result: MutableList<TranslatableText>,
        nestingLevel: Int,
        defaultElementType: ElementType,
    ) {
        for (element in elements) {
            when (element) {
                is LinearText -> {
                    if (element.blockStyle == LinearTextBlockStyle.TEXT && element.text.isNotBlank()) {
                        val elementType = getElementTypeFromAnnotations(element.annotations, defaultElementType)
                        val nonHeadingAnnotations = element.annotations.filter { !isHeadingAnnotation(it) }
                        val taggedText = toTaggedText(element.text, nonHeadingAnnotations).trim()

                        result.add(
                            TranslatableText(
                                text = taggedText,
                                elementType = elementType,
                                nestingLevel = nestingLevel,
                            ),
                        )
                    }
                }
                is LinearListItem -> {
                    extractRecursively(element.content, result, nestingLevel + 1, ElementType.LIST_ITEM)
                }
                is LinearBlockQuote -> {
                    extractRecursively(element.content, result, nestingLevel + 1, ElementType.BLOCKQUOTE)
                }
                is LinearTable -> {
                    for (row in 0 until element.rowCount) {
                        for (col in 0 until element.colCount) {
                            val cell = element.cellAt(row, col) ?: continue
                            if (cell.isFiller) continue
                            extractRecursively(cell.content, result, nestingLevel, ElementType.TABLE_CELL)
                        }
                    }
                }
                is LinearImage -> {
                    element.caption?.let { caption ->
                        if (caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
                            val nonHeadingAnnotations = caption.annotations.filter { !isHeadingAnnotation(it) }
                            result.add(
                                TranslatableText(
                                    text = toTaggedText(caption.text, nonHeadingAnnotations).trim(),
                                    elementType = ElementType.IMAGE_CAPTION,
                                    nestingLevel = nestingLevel,
                                ),
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Converts text with LinearTextAnnotation ranges to XML-tagged text.
     *
     * Algorithm:
     * 1. Escape XML special characters in the source text
     * 2. Build events from annotations (open at start, close at endExclusive)
     * 3. Sort events: by position, opens before closes, wider spans first among opens
     * 4. Walk through escaped text inserting tags at event positions
     */
    internal fun toTaggedText(
        text: String,
        annotations: List<LinearTextAnnotation>,
    ): String {
        if (annotations.isEmpty()) {
            return escapeXmlContent(text)
        }

        val escaped = escapeXmlContent(text)

        // Build events: (position in escaped text, isOpen, priority for sorting, tag string)
        data class TagEvent(
            val position: Int,
            val isOpen: Boolean,
            val span: Int, // width of span for sorting (wider first among opens)
            val tag: String,
        )

        val events = mutableListOf<TagEvent>()

        for (annotation in annotations) {
            val openTag = annotationToOpenTag(annotation) ?: continue
            val closeTag = annotationToCloseTag(annotation) ?: continue

            // Map positions from original text to escaped text
            val start = mapPositionToEscaped(text, annotation.start)
            val end = mapPositionToEscaped(text, annotation.endExclusive)
            val spanWidth = annotation.endExclusive - annotation.start

            events.add(TagEvent(start, true, spanWidth, openTag))
            events.add(TagEvent(end, false, spanWidth, closeTag))
        }

        // Sort: by position, opens before closes at same position,
        // among opens at same position: wider spans first (outer wraps inner)
        // among closes at same position: narrower spans first (inner closes first)
        events.sortWith(
            compareBy<TagEvent> { it.position }
                .thenBy { if (it.isOpen) 0 else 1 }
                .thenByDescending { if (it.isOpen) it.span else -it.span },
        )

        // Build result by walking through escaped text and inserting tags
        val result = StringBuilder()
        var lastPos = 0

        for (event in events) {
            if (event.position > lastPos) {
                result.append(escaped, lastPos, event.position)
            }
            result.append(event.tag)
            lastPos = event.position
        }

        if (lastPos < escaped.length) {
            result.append(escaped, lastPos, escaped.length)
        }

        return result.toString()
    }

    private fun escapeXmlContent(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    /**
     * Maps a position in the original text to the corresponding position in the escaped text.
     */
    private fun mapPositionToEscaped(
        originalText: String,
        position: Int,
    ): Int {
        var escapedPos = 0
        for (i in 0 until minOf(position, originalText.length)) {
            escapedPos +=
                when (originalText[i]) {
                    '&' -> 5 // &amp;
                    '<' -> 4 // &lt;
                    '>' -> 4 // &gt;
                    else -> 1
                }
        }
        return escapedPos
    }

    private fun annotationToOpenTag(annotation: LinearTextAnnotation): String? =
        when (annotation.data) {
            is LinearTextAnnotationBold -> "<b>"
            is LinearTextAnnotationItalic -> "<i>"
            is LinearTextAnnotationCode -> "<code>"
            is LinearTextAnnotationLink -> "<link href=\"${escapeXmlAttribute((annotation.data as LinearTextAnnotationLink).href)}\">"
            is LinearTextAnnotationStrikethrough -> "<s>"
            is LinearTextAnnotationUnderline -> "<u>"
            is LinearTextAnnotationSuperscript -> "<sup>"
            is LinearTextAnnotationSubscript -> "<sub>"
            is LinearTextAnnotationMonospace -> "<mono>"
            is LinearTextAnnotationFont -> "<font face=\"${escapeXmlAttribute((annotation.data as LinearTextAnnotationFont).face)}\">"
            // Heading annotations are not inline tags
            is LinearTextAnnotationH1,
            is LinearTextAnnotationH2,
            is LinearTextAnnotationH3,
            is LinearTextAnnotationH4,
            is LinearTextAnnotationH5,
            is LinearTextAnnotationH6,
            -> null
        }

    private fun annotationToCloseTag(annotation: LinearTextAnnotation): String? =
        when (annotation.data) {
            is LinearTextAnnotationBold -> "</b>"
            is LinearTextAnnotationItalic -> "</i>"
            is LinearTextAnnotationCode -> "</code>"
            is LinearTextAnnotationLink -> "</link>"
            is LinearTextAnnotationStrikethrough -> "</s>"
            is LinearTextAnnotationUnderline -> "</u>"
            is LinearTextAnnotationSuperscript -> "</sup>"
            is LinearTextAnnotationSubscript -> "</sub>"
            is LinearTextAnnotationMonospace -> "</mono>"
            is LinearTextAnnotationFont -> "</font>"
            is LinearTextAnnotationH1,
            is LinearTextAnnotationH2,
            is LinearTextAnnotationH3,
            is LinearTextAnnotationH4,
            is LinearTextAnnotationH5,
            is LinearTextAnnotationH6,
            -> null
        }

    private fun escapeXmlAttribute(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun isHeadingAnnotation(annotation: LinearTextAnnotation): Boolean =
        when (annotation.data) {
            is LinearTextAnnotationH1,
            is LinearTextAnnotationH2,
            is LinearTextAnnotationH3,
            is LinearTextAnnotationH4,
            is LinearTextAnnotationH5,
            is LinearTextAnnotationH6,
            -> true
            else -> false
        }

    private fun getElementTypeFromAnnotations(
        annotations: List<LinearTextAnnotation>,
        defaultType: ElementType,
    ): ElementType {
        for (annotation in annotations) {
            when (annotation.data) {
                is LinearTextAnnotationH1 -> return ElementType.HEADING_1
                is LinearTextAnnotationH2 -> return ElementType.HEADING_2
                is LinearTextAnnotationH3 -> return ElementType.HEADING_3
                is LinearTextAnnotationH4 -> return ElementType.HEADING_4
                is LinearTextAnnotationH5 -> return ElementType.HEADING_5
                is LinearTextAnnotationH6 -> return ElementType.HEADING_6
                else -> {}
            }
        }
        return defaultType
    }
}
