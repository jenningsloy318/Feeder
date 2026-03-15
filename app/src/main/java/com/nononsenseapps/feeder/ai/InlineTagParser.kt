package com.nononsenseapps.feeder.ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import com.nononsenseapps.feeder.ui.compose.text.asFontFamily
import com.nononsenseapps.feeder.ui.compose.theme.CodeInlineStyle
import com.nononsenseapps.feeder.ui.compose.theme.LinkTextStyle
import com.nononsenseapps.feeder.ui.compose.theme.LocalTypographySettings
import com.nononsenseapps.feeder.ui.compose.theme.TypographySettings

/**
 * Parses XML-like inline tags in translated text and produces Compose AnnotatedString.
 * Uses a single-pass state machine. Never throws - falls back to plain text on malformed input.
 */
object InlineTagParser {
    private val KNOWN_TAGS = setOf("b", "i", "code", "link", "s", "u", "sup", "sub", "mono", "font")

    @Composable
    fun parse(
        text: String,
        onLinkClick: (url: String) -> Unit,
    ): AnnotatedString {
        val codeStyle = CodeInlineStyle()
        val typographySettings = LocalTypographySettings.current
        val linkColor = MaterialTheme.colorScheme.primary
        return parseInternal(text, onLinkClick, codeStyle, typographySettings, linkColor)
    }

    /**
     * Non-composable entry point for unit testing.
     * Uses default styles that don't require Compose context.
     */
    internal fun parseForTest(
        text: String,
        onLinkClick: (url: String) -> Unit = {},
    ): AnnotatedString {
        val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = Color.LightGray)
        val typographySettings = TypographySettings(
            fontScale = 1f,
            sansFontFamily = FontFamily.SansSerif,
            monoFontFamily = FontFamily.Monospace,
            serifFontFamily = FontFamily.Serif,
        )
        val linkColor = Color.Blue
        return parseInternal(text, onLinkClick, codeStyle, typographySettings, linkColor)
    }

    private fun parseInternal(
        text: String,
        onLinkClick: (url: String) -> Unit,
        codeStyle: SpanStyle,
        typographySettings: TypographySettings,
        linkColor: Color,
    ): AnnotatedString {
        if (text.isEmpty()) {
            return AnnotatedString("")
        }

        val builder = AnnotatedString.Builder()

        // Style stack: each entry is a tag name + its resolved style/link info
        data class TagEntry(
            val tagName: String,
            val style: SpanStyle? = null,
            val linkHref: String? = null,
        )

        val styleStack = mutableListOf<TagEntry>()
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]

            if (ch == '&') {
                // Try to unescape XML entity
                val entity = tryParseEntity(text, i)
                if (entity != null) {
                    builder.append(entity.first)
                    i += entity.second
                    continue
                }
                // Not a recognized entity - literal &
                builder.append('&')
                i++
                continue
            }

            if (ch == '<') {
                val tagResult = tryParseTag(text, i)
                if (tagResult != null) {
                    val (tagInfo, consumed) = tagResult

                    if (tagInfo.isClosing) {
                        // Closing tag
                        if (tagInfo.name in KNOWN_TAGS) {
                            // Find and pop matching open tag from stack
                            val matchIndex = styleStack.indexOfLast { it.tagName == tagInfo.name }
                            if (matchIndex >= 0) {
                                styleStack.removeAt(matchIndex)
                            }
                            // If no match found, ignore the close tag (tolerance)
                        } else {
                            // Unknown close tag - render as literal text
                            builder.append(text.substring(i, i + consumed))
                        }
                        i += consumed
                        continue
                    } else {
                        // Opening tag
                        if (tagInfo.name in KNOWN_TAGS) {
                            val style = resolveTagStyle(tagInfo, codeStyle, typographySettings)
                            styleStack.add(TagEntry(tagInfo.name, style, tagInfo.attributes["href"]))

                            // Push style/link range start
                            if (tagInfo.name == "link" && tagInfo.attributes.containsKey("href")) {
                                val href = tagInfo.attributes["href"] ?: ""
                                builder.pushLink(
                                    LinkAnnotation.Clickable(
                                        tag = href,
                                        styles = TextLinkStyles(
                                            style = SpanStyle(
                                                color = linkColor,
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                        ),
                                        linkInteractionListener = {
                                            onLinkClick(href)
                                        },
                                    ),
                                )
                            } else if (style != null) {
                                builder.pushStyle(style)
                            }

                            i += consumed
                            continue
                        } else {
                            // Unknown open tag - render as literal text
                            builder.append(text.substring(i, i + consumed))
                            i += consumed
                            continue
                        }
                    }
                }

                // Failed to parse as tag - literal <
                builder.append('<')
                i++
                continue
            }

            // Regular character
            builder.append(ch)
            i++
        }

        // Close any unclosed tags (tolerance for malformed input)
        for (entry in styleStack.reversed()) {
            if (entry.linkHref != null && entry.tagName == "link") {
                builder.pop() // pop link
            } else if (entry.style != null) {
                builder.pop() // pop style
            }
        }

        return builder.toAnnotatedString()
    }

    private data class TagInfo(
        val name: String,
        val isClosing: Boolean,
        val attributes: Map<String, String> = emptyMap(),
    )

    /**
     * Tries to parse a tag starting at position i (which is '<').
     * Returns (TagInfo, chars consumed) or null if not a valid tag.
     */
    private fun tryParseTag(text: String, start: Int): Pair<TagInfo, Int>? {
        if (start >= text.length || text[start] != '<') return null

        val closeAngle = text.indexOf('>', start)
        if (closeAngle == -1) return null

        val tagContent = text.substring(start + 1, closeAngle).trim()
        if (tagContent.isEmpty()) return null

        val consumed = closeAngle - start + 1

        // Closing tag
        if (tagContent.startsWith("/")) {
            val name = tagContent.substring(1).trim().lowercase()
            if (name.isEmpty()) return null
            return Pair(TagInfo(name = name, isClosing = true), consumed)
        }

        // Opening tag - parse name and attributes
        val parts = tagContent.split(Regex("\\s+"), limit = 2)
        val name = parts[0].lowercase()
        if (name.isEmpty()) return null

        val attributes = if (parts.size > 1) {
            parseAttributes(parts[1])
        } else {
            emptyMap()
        }

        return Pair(TagInfo(name = name, isClosing = false, attributes = attributes), consumed)
    }

    /**
     * Parses attributes from tag content, e.g. `href="url" face="serif"`.
     */
    private fun parseAttributes(attrString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = Regex("""(\w+)\s*=\s*"([^"]*)"""")
        for (match in pattern.findAll(attrString)) {
            val key = match.groupValues[1].lowercase()
            val value = unescapeXmlAttribute(match.groupValues[2])
            result[key] = value
        }
        return result
    }

    /**
     * Tries to parse an XML entity at position i (which is '&').
     * Returns (unescaped char, chars consumed) or null.
     */
    private fun tryParseEntity(text: String, start: Int): Pair<Char, Int>? {
        if (start >= text.length || text[start] != '&') return null

        val semicolonPos = text.indexOf(';', start)
        if (semicolonPos == -1 || semicolonPos - start > 6) return null

        val entity = text.substring(start, semicolonPos + 1)
        val unescaped = when (entity) {
            "&amp;" -> '&'
            "&lt;" -> '<'
            "&gt;" -> '>'
            "&quot;" -> '"'
            else -> return null
        }

        return Pair(unescaped, entity.length)
    }

    private fun unescapeXmlAttribute(value: String): String =
        value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")

    private fun resolveTagStyle(
        tagInfo: TagInfo,
        codeStyle: SpanStyle,
        typographySettings: TypographySettings,
    ): SpanStyle? =
        when (tagInfo.name) {
            "b" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i" -> SpanStyle(fontStyle = FontStyle.Italic)
            "code" -> codeStyle
            "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript)
            "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript)
            "mono" -> SpanStyle(fontFamily = typographySettings.monoFontFamily)
            "font" -> {
                val face = tagInfo.attributes["face"]
                if (face != null) {
                    val fontFamily = face.asFontFamily(typographySettings)
                    if (fontFamily != null) SpanStyle(fontFamily = fontFamily) else null
                } else {
                    null
                }
            }
            "link" -> null // Links handled separately via pushLink
            else -> null
        }
}
