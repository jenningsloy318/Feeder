package com.nononsenseapps.feeder.ai

import kotlinx.serialization.Serializable

/**
 * Represents a text element with structure metadata for improved translation quality.
 *
 * This class captures not just the text content, but also the structural context
 * of where the text appears in the document. This information helps AI translators
 * produce better quality translations by understanding:
 * - The element type (paragraph, heading, list item, blockquote)
 * - The nesting level (for nested lists or blockquotes)
 *
 * @property text The actual text content to translate
 * @property elementType The type of HTML element this text came from
 * @property nestingLevel The depth of nesting (0 for top-level, 1+ for nested content)
 */
@Serializable
data class TranslatableText(
    val text: String,
    val elementType: ElementType,
    val nestingLevel: Int = 0,
) {
    /**
     * Returns a formatted string describing the structure context.
     * Used in prompts to AI translators.
     */
    fun getStructureDescription(): String {
        return when (elementType) {
            ElementType.PARAGRAPH -> "paragraph"
            ElementType.HEADING_1 -> "heading level 1"
            ElementType.HEADING_2 -> "heading level 2"
            ElementType.HEADING_3 -> "heading level 3"
            ElementType.HEADING_4 -> "heading level 4"
            ElementType.HEADING_5 -> "heading level 5"
            ElementType.HEADING_6 -> "heading level 6"
            ElementType.LIST_ITEM -> "list item${if (nestingLevel > 0) " (nesting level: $nestingLevel)" else ""}"
            ElementType.BLOCKQUOTE -> "blockquote${if (nestingLevel > 0) " (nesting level: $nestingLevel)" else ""}"
        }
    }

    /**
     * Returns the text with structure context prepended.
     * Format: "[ElementType] text"
     */
    fun withStructurePrefix(): String {
        return "[${getStructureDescription()}] $text"
    }

    companion object {
        /**
         * Creates a TranslatableText from plain text with default settings.
         */
        fun fromPlainText(text: String): TranslatableText {
            return TranslatableText(
                text = text,
                elementType = ElementType.PARAGRAPH,
                nestingLevel = 0,
            )
        }
    }
}

/**
 * Represents the type of HTML element that contains the text.
 */
@Serializable
enum class ElementType {
    /** Regular paragraph text */
    PARAGRAPH,

    /** Level 1 heading (H1) */
    HEADING_1,

    /** Level 2 heading (H2) */
    HEADING_2,

    /** Level 3 heading (H3) */
    HEADING_3,

    /** Level 4 heading (H4) */
    HEADING_4,

    /** Level 5 heading (H5) */
    HEADING_5,

    /** Level 6 heading (H6) */
    HEADING_6,

    /** List item (LI) - can be nested */
    LIST_ITEM,

    /** Blockquote content - can be nested */
    BLOCKQUOTE,
}
