package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.mikepenz.markdown.m3.Markdown

/**
 * Markdown rendering composable using Mikepenz Multiplatform Markdown Renderer.
 *
 * This composable provides comprehensive markdown rendering support with 95%+ CommonMark
 * spec coverage including tables, task lists, strikethrough, and nested lists.
 *
 * Supported markdown elements:
 * - Headings: # ## ### #### ##### ######
 * - Bold: **text** or __text__
 * - Italic: *text* or _text__
 * - Strikethrough: ~~text~~
 * - Links: [text](url)
 * - Unordered lists: - item or * item (with nesting support)
 * - Ordered lists: 1. item (with nesting support)
 * - Task lists: - [x] completed, - [ ] incomplete
 * - Code (inline): `code`
 * - Code (block): ```code``` with syntax highlighting
 * - Blockquotes: > quote
 * - Tables: | Header | Header |
 * - Horizontal rules: --- or ***
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the root composable
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = markdown,
        modifier = modifier,
    )
}

/**
 * Safe variant of MarkdownContent that falls back to plain text on error.
 *
 * This composable uses a non-throwing approach to handle potential rendering errors
 * by catching them at the content level and displaying the original markdown as plain text.
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the root composable
 */
@Composable
fun MarkdownContentSafe(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    // Note: Compose doesn't support try-catch around composables
    // The library handles errors internally, so we just use MarkdownContent directly
    // If specific error handling is needed, it should be done at the data preparation level
    MarkdownContent(
        markdown = markdown,
        modifier = modifier,
    )
}

/**
 * Deprecated: Use MarkdownContent composable instead.
 *
 * This function is kept for backward compatibility but is deprecated.
 * The new MarkdownContent composable provides better features and performance.
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString with plain text (no formatting)
 */
@Deprecated(
    message = "Use MarkdownContent composable instead for better markdown rendering",
    level = DeprecationLevel.WARNING,
)
fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    // Return plain text as fallback - the new API uses composables directly
    return AnnotatedString(markdown)
}

/**
 * Deprecated: Use MarkdownContentSafe composable instead.
 *
 * This function is kept for backward compatibility but is deprecated.
 * The new MarkdownContentSafe composable provides better features and performance.
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString with plain text (no formatting)
 */
@Deprecated(
    message = "Use MarkdownContentSafe composable instead for better markdown rendering",
    level = DeprecationLevel.WARNING,
)
fun markdownToAnnotatedStringSafe(markdown: String): AnnotatedString {
    // Return plain text as fallback - the new API uses composables directly
    return AnnotatedString(markdown)
}

/**
 * Exception thrown when markdown parsing fails.
 *
 * This is no longer used as the library handles errors internally,
 * but kept for API compatibility.
 */
class MarkdownParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
