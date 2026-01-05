# Implementation Plan - Spec 019: Markdown Rendering for Summaries

**Created:** January 5, 2026
**Status:** Ready for Implementation

## Overview

This document provides the technical specification and detailed implementation plan for adding markdown rendering support to AI-generated article summaries in the Feeder Android app.

## Technical Summary

**Goal:** Render markdown-formatted AI summaries with proper formatting instead of plain text.

**Approach:** Convert markdown → HTML → AnnotatedString, leveraging existing infrastructure.

**Impact:** 1 new file, 1 modified file, ~200 lines of code total.

**Dependencies:** JetBrains Markdown + Jsoup (already in project).

## File Changes

### New Files

#### 1. MarkdownToAnnotatedString.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt`

**Purpose:** Convert markdown text to AnnotatedString for Compose rendering.

**Size:** ~150 lines

**Key Functions:**
- `markdownToAnnotatedString(markdown: String): AnnotatedString`
- `markdownToAnnotatedStringSafe(markdown: String): AnnotatedString`
- `createMarkdownCleaner(): Cleaner`
- `parseMarkdownToHTML(markdown: String): String`
- `sanitizeHTML(html: String): String`

**Dependencies:**
- `org.intellij.markdown` (JetBrains Markdown)
- `org.jsoup.safety.Cleaner` (Jsoup)
- `com.nononsenseapps.feeder.ui.compose.text.htmlToAnnotatedString`

### Modified Files

#### 1. ArticleScreen.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Changes:**
- Update `SummarySection` composable to render markdown
- Add `MarkdownText` composable
- Add imports for markdown rendering

**Lines Changed:** ~20 lines

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
                    text = summary.value.content,
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
                    markdown = summary.value.content,
                )
        }
    }
}

@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
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

## Implementation Details

### Step 1: Create MarkdownToAnnotatedString.kt

**File Structure:**
```kotlin
package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.ui.text.AnnotatedString
import org.intellij.markdown.flavours.gfm.GFMFlavour
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.io.StringReader

/**
 * Converts markdown text to AnnotatedString for Compose rendering.
 *
 * Process:
 * 1. Parse markdown to HTML using JetBrains Markdown library
 * 2. Sanitize HTML using Jsoup Cleaner (XSS prevention)
 * 3. Convert HTML to AnnotatedString using existing infrastructure
 */

/**
 * Parses markdown to HTML string.
 */
private fun parseMarkdownToHTML(markdown: String): String {
    val flavour = GFMFlavour()
    val parser = MarkdownParser(flavour)
    val tree = parser.buildMarkdownTreeFromString(markdown)

    // Convert AST to HTML
    val htmlBuilder = StringBuilder()
    // ... AST traversal and HTML generation ...

    return htmlBuilder.toString()
}

/**
 * Creates a Jsoup Cleaner with safe HTML whitelist for markdown.
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
 */
private fun sanitizeHTML(html: String): String {
    val cleaner = createMarkdownCleaner()
    val dirtyDoc = Jsoup.parse(html)
    val cleanDoc = cleaner.clean(dirtyDoc)
    return cleanDoc.body().html()
}

/**
 * Converts markdown to AnnotatedString.
 *
 * @throws MarkdownParseException if parsing fails
 */
fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    try {
        // Step 1: Markdown to HTML
        val html = parseMarkdownToHTML(markdown)

        // Step 2: Sanitize HTML
        val cleanHtml = sanitizeHTML(html)

        // Step 3: HTML to AnnotatedString (reuse existing)
        val inputStream = StringReader(cleanHtml).readBytes().inputStream()
        val annotatedStrings = htmlToAnnotatedString(
            inputStream = inputStream,
            baseUrl = "",
        )

        // Combine multiple paragraphs into one
        return annotatedStrings.reduce { acc, item ->
            AnnotatedString.Builder().apply {
                append(acc)
                append("\n\n")
                append(item)
                toAnnotatedString()
            }
        }
    } catch (e: Exception) {
        throw MarkdownParseException("Failed to convert markdown to AnnotatedString", e)
    }
}

/**
 * Converts markdown to AnnotatedString, returning plain text on error.
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
 * Exception thrown when markdown parsing fails.
 */
class MarkdownParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

### Step 2: Update ArticleScreen.kt

**Add Imports:**
```kotlin
import com.nononsenseapps.feeder.ui.compose.text.markdownToAnnotatedStringSafe
```

**Replace SummarySection:**
- See "Modified Files" section above

### Step 3: Add Tests

**Create:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedStringTest.kt`

**Test Cases:**
1. `testBoldText()` - Verify **bold** renders
2. `testItalicText()` - Verify *italic* renders
3. `testLinks()` - Verify [links](url) render
4. `testLists()` - Verify lists render
5. `testCodeBlocks()` - Verify code blocks render
6. `testHeaders()` - Verify headers render
7. `testBlockquotes()` - Verify blockquotes render
8. `testXSSPrevention()` - Verify script tags removed
9. `testMaliciousMarkdown()` - Verify safe rendering
10. `testErrorHandling()` - Verify plain text fallback

## Testing Strategy

### Unit Tests

**File:** `MarkdownToAnnotatedStringTest.kt`

**Coverage:**
- Markdown parsing (all elements)
- HTML sanitization (security)
- Error handling (fallback)
- Edge cases (empty, malformed, etc.)

### Integration Tests

**File:** `ArticleScreenTest.kt` (extend existing)

**Test Cases:**
1. `testSummarySectionDisplaysMarkdown()`
2. `testSummarySectionHandlesLoading()`
3. `testSummarySectionHandlesError()`
4. `testSummaryLinksClickable()`

### Security Tests

**File:** `MarkdownSecurityTest.kt`

**Test Cases:**
1. `testScriptTagRemoved()`
2. `testOnClickAttributeRemoved()`
3. `testIframeRemoved()`
4. `testJavaScriptProtocolBlocked()`

### Manual Testing

**Test Cases:**
1. Generate summary with various markdown elements
2. Tap on links → verify they open
3. Test in light mode
4. Test in dark mode
5. Test with RTL language
6. Test with very long summary
7. Test with malformed markdown

## Build and Deployment

### Build Verification

```bash
# Build the project
./gradlew assembleDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Check for lint issues
./gradlew lint
```

### Success Criteria

- ✅ Project builds without errors
- ✅ All unit tests pass
- ✅ All integration tests pass
- ✅ All security tests pass
- ✅ Lint passes (ktlint)
- ✅ APK size increase < 50KB
- ✅ No performance regression

## Rollout Plan

### Phase 1: Implementation (Week 1)
- Create MarkdownToAnnotatedString.kt
- Update ArticleScreen.kt
- Write unit tests
- Verify build

### Phase 2: Testing (Week 1-2)
- Run all tests
- Fix any issues
- Manual testing on device
- Security review

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

### Metrics

- Markdown parsing success rate
- Error fallback rate
- Rendering performance (ms)
- Crash reports
- User feedback

### Logging

```kotlin
private const val LOG_TAG = "MarkdownRenderer"

when (val result = markdownToAnnotatedStringSafe(markdown)) {
    is AnnotatedString.Success -> {
        Log.d(LOG_TAG, "Successfully parsed ${markdown.length} chars of markdown")
    }
    is AnnotatedString.Error -> {
        Log.e(LOG_TAG, "Failed to parse markdown, falling back to plain text", result.exception)
    }
}
```

## Rollback Plan

If issues arise:

1. **Immediate Rollback:**
   - Revert `SummarySection` to plain text
   - Keep MarkdownToAnnotatedString.kt (harmless)
   - Deploy hotfix

2. **Alternative:**
   - Add feature flag to disable markdown rendering
   - Allow remote configuration
   - Monitor before re-enabling

## Dependencies and Conflicts

### Internal Dependencies

- `com.nononsenseapps.feeder.ui.compose.text.htmlToAnnotatedString` (reuse)
- `com.nononsenseapps.feeder.ui.compose.feedarticle.ArticleViewModel` (no changes)

### External Dependencies

- `org.jetbrains:markdown` (already in project)
- `org.jsoup:jsoup` (already in project)
- `androidx.compose.ui:ui-text` (already in project)

### No Conflicts Expected

## Documentation

### Code Documentation

- KDoc comments for all public functions
- Inline comments for complex logic
- Examples in function documentation

### User Documentation

- Update changelog
- Add release notes
- Update help docs (if needed)

## Success Metrics

### Technical Metrics

- ✅ All tests pass
- ✅ Build succeeds
- ✅ No lint warnings
- ✅ APK size increase < 50KB
- ✅ Rendering performance < 100ms

### User Experience Metrics

- ✅ Markdown renders correctly
- ✅ Links work as expected
- ✅ No visual regressions
- ✅ Smooth scrolling
- ✅ Accessibility maintained

## Next Steps

1. ✅ Complete technical specification (this document)
2. ⏭️ Phase 7: Specification review
3. ⏭️ Phase 8: Implementation & QA
4. ⏭️ Phase 9: Code review
5. ⏭️ Phase 10: Documentation update
6. ⏭️ Phase 11: Cleanup
7. ⏭️ Phase 12: Commit & push
8. ⏭️ Phase 13: Final verification

---

**Specification Status:** ✅ Complete and ready for implementation
