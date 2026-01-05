package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.ui.text.AnnotatedString
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Converts markdown text to AnnotatedString for Compose rendering.
 *
 * This function provides markdown rendering support by converting markdown to HTML,
 * sanitizing it for security, and then converting to AnnotatedString using the
 * existing HTML rendering infrastructure.
 *
 * The conversion process is:
 * 1. Parse markdown to HTML using a simple markdown parser
 * 2. Sanitize HTML to prevent XSS attacks
 * 3. Convert HTML to AnnotatedString using existing infrastructure
 *
 * Supported markdown elements:
 * - Headings: # ## ### #### ##### ######
 * - Bold: **text** or __text__
 * - Italic: *text* or _text_
 * - Links: [text](url)
 * - Unordered lists: - item or * item
 * - Ordered lists: 1. item
 * - Code (inline): `code`
 * - Code (block): ```code```
 * - Blockquotes: > quote
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString ready for Compose Text rendering
 * @throws MarkdownParseException if markdown parsing fails
 */
fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    try {
        // Step 1: Convert markdown to HTML
        val html = parseMarkdownToHTML(markdown)

        // Step 2: Sanitize HTML to prevent XSS
        val cleanHtml = sanitizeHTML(html)

        // Step 3: Convert HTML to AnnotatedString using existing infrastructure
        val inputStream = ByteArrayInputStream(cleanHtml.toByteArray(StandardCharsets.UTF_8))
        val annotatedStrings = htmlToAnnotatedString(
            inputStream = inputStream,
            baseUrl = "",
        )

        // Combine multiple AnnotatedStrings into one
        return if (annotatedStrings.isEmpty()) {
            AnnotatedString("")
        } else {
            annotatedStrings.reduce { acc, item ->
                if (acc.text.isEmpty()) {
                    item
                } else if (item.text.isEmpty()) {
                    acc
                } else {
                    acc + AnnotatedString("\n\n") + item
                }
            }
        }
    } catch (e: Exception) {
        throw MarkdownParseException("Failed to convert markdown to AnnotatedString", e)
    }
}

/**
 * Converts markdown to AnnotatedString, returning plain text on error.
 *
 * This is a safe version that catches exceptions and returns the original
 * markdown as plain text if parsing fails.
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString on success, plain text AnnotatedString on error
 */
fun markdownToAnnotatedStringSafe(markdown: String): AnnotatedString {
    return try {
        markdownToAnnotatedString(markdown)
    } catch (e: Exception) {
        // Fallback to plain text
        AnnotatedString(markdown)
    }
}

/**
 * Parses markdown text to HTML string.
 *
 * This implements a simple markdown parser that handles common markdown elements.
 * It uses regex-based replacements for simplicity and performance.
 *
 * @param markdown The markdown text to parse
 * @return HTML string
 */
private fun parseMarkdownToHTML(markdown: String): String {
    var html = markdown

    // Normalize line breaks: 3+ consecutive newlines -> 2 newlines (prevents extra spacing)
    html = html.replace(Regex("\n\n+")) { "\n\n" }

    // Escape HTML special characters first to prevent XSS
    html = html.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    // Code blocks (must be done before other processing)
    html = html.replace(Regex("```([\\s\\S]*?)```")) { matchResult ->
        "<pre><code>${matchResult.groupValues[1]}</code></pre>"
    }

    // Inline code (must be done before other processing)
    html = html.replace(Regex("`([^`]+)`")) { matchResult ->
        "<code>${matchResult.groupValues[1]}</code>"
    }

    // Headings
    html = html.replace(Regex("^######\\s+(.+)$", RegexOption.MULTILINE)) { "<h6>${it.groupValues[1]}</h6>" }
    html = html.replace(Regex("^#####\\s+(.+)$", RegexOption.MULTILINE)) { "<h5>${it.groupValues[1]}</h5>" }
    html = html.replace(Regex("^####\\s+(.+)$", RegexOption.MULTILINE)) { "<h4>${it.groupValues[1]}</h4>" }
    html = html.replace(Regex("^###\\s+(.+)$", RegexOption.MULTILINE)) { "<h3>${it.groupValues[1]}</h3>" }
    html = html.replace(Regex("^##\\s+(.+)$", RegexOption.MULTILINE)) { "<h2>${it.groupValues[1]}</h2>" }
    html = html.replace(Regex("^#\\s+(.+)$", RegexOption.MULTILINE)) { "<h1>${it.groupValues[1]}</h1>" }

    // Bold and Italic (order matters - bold first)
    html = html.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*")) { "<strong><em>${it.groupValues[1]}</em></strong>" }
    html = html.replace(Regex("___(.+?)___")) { "<strong><em>${it.groupValues[1]}</em></strong>" }
    html = html.replace(Regex("\\*\\*(.+?)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
    html = html.replace(Regex("__(.+?)__")) { "<strong>${it.groupValues[1]}</strong>" }
    html = html.replace(Regex("\\*(.+?)\\*")) { "<em>${it.groupValues[1]}</em>" }
    html = html.replace(Regex("_(.+?)_")) { "<em>${it.groupValues[1]}</em>" }

    // Links
    html = html.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")) { matchResult ->
        "<a href=\"${matchResult.groupValues[2]}\">${matchResult.groupValues[1]}</a>"
    }

    // Blockquotes
    html = html.replace(Regex("^>\\s+(.+)$", RegexOption.MULTILINE)) { "<blockquote>${it.groupValues[1]}</blockquote>" }

    // Unordered lists
    html = html.replace(Regex("^[\\-\\*]\\s+(.+)$", RegexOption.MULTILINE)) { "<li>${it.groupValues[1]}</li>" }
    html = html.replace(Regex("(<li>.+?</li>\\n?)+", RegexOption.DOT_MATCHES_ALL)) { "<ul>${it.value}</ul>" }

    // Ordered lists
    html = html.replace(Regex("^\\d+\\.\\s+(.+)$", RegexOption.MULTILINE)) { "<li>${it.groupValues[1]}</li>" }
    html = html.replace(Regex("(<li>.+?</li>\\n?)+", RegexOption.DOT_MATCHES_ALL)) { "<ol>${it.value}</ol>" }

    // Line breaks and paragraphs
    html = html.replace(Regex("\n\n")) { "</p><p>" }
    html = "<p>$html</p>"
    html = html.replace(Regex("<p></p>")) { "" }
    html = html.replace(Regex("<p>(<h[1-6]>)")) { it.groupValues[1] }
    html = html.replace(Regex("(</h[1-6]>)</p>")) { it.groupValues[1] }
    html = html.replace(Regex("<p>(<ul>)")) { it.groupValues[1] }
    html = html.replace(Regex("(</ul>)</p>")) { it.groupValues[1] }
    html = html.replace(Regex("<p>(<ol>)")) { it.groupValues[1] }
    html = html.replace(Regex("(</ol>)</p>")) { it.groupValues[1] }
    html = html.replace(Regex("<p>(<pre>)")) { it.groupValues[1] }
    html = html.replace(Regex("(</pre>)</p>")) { it.groupValues[1] }
    html = html.replace(Regex("<p>(<blockquote>)")) { it.groupValues[1] }
    html = html.replace(Regex("(</blockquote>)</p>")) { it.groupValues[1] }
    html = html.replace(Regex("\n")) { "<br>" }

    return html
}

/**
 * Creates a Jsoup Cleaner with a safe HTML whitelist for markdown.
 *
 * The whitelist allows only safe HTML elements that are typically generated
 * from markdown. Dangerous elements like scripts, iframes, and forms are
 * explicitly removed.
 *
 * @return Jsoup Cleaner configured for safe HTML
 */
private fun createMarkdownCleaner(): Cleaner {
    val safelist = Safelist.relaxed()
        .addTags("h1", "h2", "h3", "h4", "h5", "h6")
        .addTags("strong", "b", "em", "i", "u", "sub", "sup")
        .addTags("ul", "ol", "li")
        .addTags("pre", "code")
        .addTags("blockquote", "p", "br")
        .addTags("a")
        .addAttributes("a", "href")
        .addProtocols("a", "href", "http", "https")
        .removeTags("script", "noscript", "iframe", "embed", "object", "form", "input", "button", "style")

    return Cleaner(safelist)
}

/**
 * Sanitizes HTML to prevent XSS attacks.
 *
 * Uses Jsoup Cleaner to remove dangerous HTML elements and attributes.
 * Only safe elements from the markdown whitelist are preserved.
 *
 * @param html The HTML to sanitize
 * @return Sanitized HTML string
 */
private fun sanitizeHTML(html: String): String {
    val cleaner = createMarkdownCleaner()
    val dirtyDoc = Jsoup.parse(html)
    val cleanDoc = cleaner.clean(dirtyDoc)
    return cleanDoc.body().html()
}

/**
 * Exception thrown when markdown parsing fails.
 */
class MarkdownParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
