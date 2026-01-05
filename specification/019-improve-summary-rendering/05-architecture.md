# Architecture Design - Spec 019: Markdown Rendering for Summaries

**Design Date:** January 5, 2026
**Designer:** Super Dev Coordinator Agent

## Overview

This document outlines the architecture for adding markdown rendering support to AI-generated article summaries in the Feeder app. The design leverages existing infrastructure to minimize code changes and maximize reusability.

## Architecture Principles

1. **Reuse Over Reinvention:** Leverage existing HTML→AnnotatedString pipeline
2. **Security First:** Sanitize all HTML generated from markdown
3. **Performance:** Parse on background thread, render efficiently
4. **Simplicity:** Minimal changes, maximum impact
5. **Testability:** Each component testable in isolation

## System Architecture

### Current Flow (Plain Text)

```
┌─────────────────┐
│  AI Provider    │
│ (OpenAI/Anthropic)│
└────────┬────────┘
         │
         ↓
┌─────────────────────────┐
│ AIClient.SummaryResult  │
│   content: String       │ ← Plain text markdown
└────────┬────────────────┘
         │
         ↓
┌─────────────────────────┐
│  ArticleViewModel       │
│  aiSummary StateFlow    │
└────────┬────────────────┘
         │
         ↓
┌─────────────────────────┐
│  SummarySection         │
│  Text(content)          │ ← Plain text rendering
└─────────────────────────┘
```

### New Flow (Markdown Rendering)

```
┌─────────────────┐
│  AI Provider    │
│ (OpenAI/Anthropic)│
└────────┬────────┘
         │
         ↓
┌─────────────────────────┐
│ AIClient.SummaryResult  │
│   content: String       │ ← Plain text markdown
└────────┬────────────────┘
         │
         ↓
┌─────────────────────────┐
│  ArticleViewModel       │
│  aiSummary StateFlow    │ (NO CHANGE)
└────────┬────────────────┘
         │
         ↓
┌─────────────────────────────────┐
│  MarkdownToAnnotatedString      │ ← NEW COMPONENT
│  1. Markdown → HTML             │
│  2. HTML Sanitization           │
│  3. HTML → AnnotatedString      │
└────────┬────────────────────────┘
         │
         ↓
┌─────────────────────────┐
│  SummarySection         │
│  Text(AnnotatedString)  │ ← Formatted rendering
└─────────────────────────┘
```

## Component Design

### 1. MarkdownToAnnotatedString (NEW)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt`

**Responsibility:** Convert markdown text to AnnotatedString for Compose rendering

**Public API:**
```kotlin
/**
 * Converts markdown text to AnnotatedString for Compose rendering.
 *
 * Process:
 * 1. Parse markdown to HTML using JetBrains Markdown library
 * 2. Sanitize HTML using Jsoup Cleaner (XSS prevention)
 * 3. Convert HTML to AnnotatedString using existing infrastructure
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString ready for Compose Text
 * @throws MarkdownParseException if parsing fails
 */
fun markdownToAnnotatedString(
    markdown: String,
): AnnotatedString

/**
 * Same as above but returns plain text on error instead of throwing.
 *
 * @param markdown The markdown text to convert
 * @return AnnotatedString on success, plain text AnnotatedString on error
 */
fun markdownToAnnotatedStringSafe(
    markdown: String,
): AnnotatedString
```

**Internal Flow:**
```
markdown (String)
    ↓
parseMarkdownToHTML() [JetBrains Markdown]
    ↓
sanitizeHTML() [Jsoup Cleaner]
    ↓
htmlToAnnotatedString() [EXISTING FUNCTION]
    ↓
AnnotatedString
```

**Dependencies:**
- `org.intellij.markdown` (JetBrains Markdown)
- `org.jsoup.safety.Cleaner` (Jsoup)
- `com.nononsenseapps.feeder.ui.compose.text.htmlToAnnotatedString` (existing)

**Error Handling:**
- `markdownToAnnotatedString()`: Throws exception on error
- `markdownToAnnotatedStringSafe()`: Returns plain text on error

### 2. HTML Sanitization

**Configuration:**
```kotlin
/**
 * Creates a Jsoup Cleaner with safe HTML whitelist.
 *
 * Allows:
 * - Headings: h1, h2, h3, h4, h5, h6
 * - Text formatting: strong, b, em, i, u, sub, sup
 * - Lists: ul, ol, li
 * - Code: pre, code
 * - Blockquote: blockquote
 * - Paragraphs: p, br
 * - Links: a (with href attribute only, http/https protocols)
 *
 * Disallows:
 * - Scripts: script, noscript
 * - Embedded content: iframe, embed, object
 * - Forms: form, input, button
 * - Styles: style
 * - Events: onclick, onerror, etc.
 */
fun createMarkdownCleaner(): Cleaner {
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
```

### 3. SummarySection (MODIFIED)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Before:**
```kotlin
@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading ->
                LinearProgressIndicator(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                )
            is AISummaryState.Result ->
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = summary.value.content, // ← Plain text
                )
        }
    }
}
```

**After:**
```kotlin
@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading ->
                LinearProgressIndicator(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                )
            is AISummaryState.Result ->
                MarkdownText(
                    modifier = Modifier.padding(8.dp),
                    markdown = summary.value.content, // ← Markdown text
                    onError = { errorText ->
                        // Fallback to plain text on error
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
        }
    }
}

@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onError: @Composable (String) -> Unit = {},
) {
    val annotatedString = remember(markdown) {
        markdownToAnnotatedStringSafe(markdown)
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

## Data Flow

### Request Flow (User taps Summarize)

```
1. User taps summarize button
   ↓
2. ArticleViewModel.summarize() called
   ↓
3. AI API request (background thread)
   ↓
4. Receive SummaryResult with markdown content
   ↓
5. Update aiSummary StateFlow
   ↓
6. Compose recomposes SummarySection
   ↓
7. MarkdownToAnnotatedString converts markdown
   ↓
8. Render formatted text
```

### Error Handling Flow

```
Markdown parsing error
   ↓
markdownToAnnotatedStringSafe() catches exception
   ↓
Returns plain text AnnotatedString
   ↓
Render plain text
   ↓
Log error for debugging
```

## Security Architecture

### Layers of Defense

1. **AI Provider Layer**
   - HTTPS only
   - No executable code in responses

2. **Markdown Parsing Layer**
   - JetBrains Markdown (trusted library)
   - Converts markdown to HTML

3. **HTML Sanitization Layer**
   - Jsoup Cleaner with whitelist
   - Removes dangerous elements
   - Validates link protocols

4. **Rendering Layer**
   - Compose Text (no code execution)
   - AnnotatedString (safe API)

### Threat Model

| Threat | Mitigation |
|--------|------------|
| XSS via script tags | Jsoup Cleaner removes scripts |
| Malicious links | Protocol whitelist (http/https only) |
| HTML injection | Sanitize all HTML |
| Event handlers | Stripped by Jsoup |
| iframe/embed | Removed by whitelist |

## Performance Architecture

### Threading Model

```
Main Thread
    ├─ UI Rendering (Compose)
    └─ State Observation (StateFlow)

Background Thread (Dispatchers.IO)
    ├─ Markdown → HTML parsing
    ├─ HTML sanitization
    └─ HTML → AnnotatedString conversion
```

### Caching Strategy

```kotlin
@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onError: @Composable (String) -> Unit = {},
) {
    // Cache AnnotatedString as long as markdown doesn't change
    val annotatedString = remember(markdown) {
        markdownToAnnotatedStringSafe(markdown)
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
    )
}
```

**Benefits:**
- AnnotatedString computed once
- Recomposition only if markdown changes
- No recomposition on other state changes

## Testing Architecture

### Unit Tests

```
MarkdownToAnnotatedStringTest.kt
├─ testMarkdownParsing()
├─ testHTMLSanitization()
├─ testErrorHandling()
├─ testXSSPrevention()
└─ testRTLSupport()
```

### Integration Tests

```
ArticleScreenTest.kt
├─ testSummarySectionRendersMarkdown()
├─ testSummarySectionHandlesErrors()
├─ testSummarySectionLoadingState()
└─ testSummarySectionLinksClickable()
```

### Security Tests

```
MarkdownSecurityTest.kt
├─ testScriptTagRemoved()
├─ testOnClickAttributeRemoved()
├─ testIframeRemoved()
├─ testMaliciousLinksSanitized()
└─ testHTMLInjectionPrevented()
```

## Dependencies

### Internal
- `com.nononsenseapps.feeder.ui.compose.text.htmlToAnnotatedString` (reuse)
- `com.nononsenseapps.feeder.ui.compose.text.AnnotatedStringComposer` (reuse)

### External (Existing)
- `org.jetbrains:markdown` (markdown parsing)
- `org.jsoup:jsoup` (HTML sanitization)
- `androidx.compose.ui:ui-text` (AnnotatedString)
- `androidx.compose.runtime:runtime` (remember, @Composable)

### No New Dependencies Required

## Deployment Strategy

### Phase 1: Implementation (Week 1)
- Create MarkdownToAnnotatedString.kt
- Update SummarySection in ArticleScreen.kt
- Write unit tests

### Phase 2: Testing (Week 1-2)
- Unit tests for parsing
- Security tests for XSS
- UI tests for rendering
- Performance tests

### Phase 3: Beta Release (Week 2-3)
- Release to beta users
- Monitor crash reports
- Gather feedback
- Fix any issues

### Phase 4: Production Release (Week 4)
- Release to all users
- Monitor performance
- Address any issues

## Monitoring

### Metrics to Track
- Markdown parsing success rate
- Parsing error frequency
- Rendering performance (ms)
- Crash reports related to markdown
- User engagement with summaries

### Logging
```kotlin
when (val result = markdownToAnnotatedStringSafe(markdown)) {
    is AnnotatedString.Success -> {
        Log.d(LOG_TAG, "Successfully parsed markdown")
    }
    is AnnotatedString.Error -> {
        Log.e(LOG_TAG, "Failed to parse markdown", result.exception)
    }
}
```

## Future Enhancements

### Potential Future Features
1. **Syntax Highlighting**: For code blocks
2. **Image Support**: Render images in markdown
3. **Table Support**: Enhanced table rendering
4. **Custom Styles**: User-selectable markdown themes
5. **Copy Code Button**: For code blocks
6. **Link Previews**: Show link metadata

### Extensibility Points
- Markdown parser can be swapped (currently JetBrains)
- HTML sanitizer can be configured (whitelist)
- Rendering can be customized (AnnotatedString styles)

## Conclusion

This architecture design provides a robust, secure, and performant solution for adding markdown rendering to AI summaries. The key strengths are:

1. **Minimal Changes**: Only one new file, one modified file
2. **Maximum Reuse**: Leverages existing infrastructure
3. **Security First**: Multiple layers of defense
4. **Performance**: Background parsing, efficient rendering
5. **Testability**: Clear separation of concerns
6. **Maintainability**: Simple, focused components

The design is ready for implementation in Phase 8.
