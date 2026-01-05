# Technical Specification - Spec 019: Markdown Rendering for Summaries

**Specification ID:** 019
**Title:** Improve Summary Rendering to Support Markdown
**Status:** Ready for Implementation
**Created:** January 5, 2026

## Document Information

**Version:** 1.0
**Last Updated:** January 5, 2026
**Author:** Super Dev Coordinator Agent

## Related Documents

- [01-requirements.md](./01-requirements.md) - Requirements document
- [02-research-report.md](./02-research-report.md) - Research findings
- [04-code-assessment.md](./04-code-assessment.md) - Codebase assessment
- [05-architecture.md](./05-architecture.md) - Architecture design
- [06-ui-ux-design.md](./06-ui-ux-design.md) - UI/UX design
- [07-implementation-plan.md](./07-implementation-plan.md) - Implementation plan
- [08-task-list.md](./08-task-list.md) - Detailed task list

## 1. Introduction

### 1.1 Purpose

This technical specification defines the implementation of markdown rendering support for AI-generated article summaries in the Feeder Android app. Currently, summaries are displayed as plain text, losing markdown formatting that AI models may include in their responses.

### 1.2 Scope

**In Scope:**
- Markdown to HTML conversion using JetBrains Markdown library
- HTML sanitization for security (XSS prevention)
- HTML to AnnotatedString conversion (reusing existing infrastructure)
- Update SummarySection composable to render markdown
- Comprehensive testing (unit, integration, security)
- Documentation updates

**Out of Scope:**
- Rich text markdown editor
- Custom markdown syntax extensions
- Image embedding in markdown
- Syntax highlighting for code blocks
- Exporting formatted summaries

### 1.3 Goals

1. **Enhance Readability:** Display AI summaries with proper markdown formatting
2. **Maintain Security:** Prevent XSS vulnerabilities through sanitization
3. **Minimize Changes:** Reuse existing infrastructure where possible
4. **Ensure Performance:** Smooth rendering without scroll jank
5. **Support Accessibility:** Screen reader compatibility

## 2. Technical Approach

### 2.1 Architecture

**Flow:** `Markdown → HTML → AnnotatedString → Compose Text`

**Components:**
1. `MarkdownToAnnotatedString.kt` - New file
2. `ArticleScreen.kt` - Modified (SummarySection only)

### 2.2 Technology Stack

**Libraries:**
- `org.jetbrains:markdown` - Markdown parsing (already in project)
- `org.jsoup:jsoup` - HTML sanitization (already in project)
- `androidx.compose.ui:ui-text` - AnnotatedString (already in project)

**No new dependencies required**

### 2.3 Implementation Strategy

**Key Decision:** Extend existing HTML→AnnotatedString pipeline rather than using a dedicated markdown renderer library.

**Rationale:**
- Zero new dependencies
- Minimal code changes
- Proven security (Jsoup)
- Consistent with article content rendering

## 3. Functional Requirements

### 3.1 Supported Markdown Elements

| Element | Syntax | Support |
|---------|--------|---------|
| Headings | `# H1`, `## H2`, etc. | ✅ Required |
| Bold | `**bold**` or `__bold__` | ✅ Required |
| Italic | `*italic*` or `_italic_` | ✅ Required |
| Links | `[text](url)` | ✅ Required |
| Unordered Lists | `- item` or `* item` | ✅ Required |
| Ordered Lists | `1. item` | ✅ Required |
| Code (inline) | `` `code` `` | ✅ Required |
| Code (block) | ` ```code``` ` | ✅ Required |
| Blockquotes | `> quote` | ✅ Required |

### 3.2 Security Requirements

**FR1:** All HTML generated from markdown must be sanitized
**FR2:** Script tags must be removed
**FR3:** Event handlers (onclick, etc.) must be removed
**FR4:** Only http/https protocols allowed in links
**FR5:** Iframes and embedded content must be removed

### 3.3 Performance Requirements

**PR1:** Markdown parsing must not block UI thread
**PR2:** Total rendering time < 100ms for typical summary
**PR3:** No scroll performance degradation
**PR4:** Memory overhead < 1MB per summary

### 3.4 Error Handling

**ER1:** Malformed markdown falls back to plain text
**ER2:** Parsing exceptions are caught and logged
**ER3:** User sees content even if rendering fails

## 4. Non-Functional Requirements

### 4.1 Compatibility

**NFR1:** Must work with existing AI providers (OpenAI, Anthropic)
**NFR2:** Must be backward compatible with plain text summaries
**NFR3:** Must support Android API levels supported by app

### 4.2 Accessibility

**NFR4:** Screen readers must announce formatted content
**NFR5:** Links must have proper content descriptions
**NFR6:** Color contrast must meet WCAG AA standards
**NFR7:** Text scaling must work correctly

### 4.3 Internationalization

**NFR8:** Must support RTL (Right-to-Left) languages
**NFR9:** Must handle mixed LTR/RTL content
**NFR10:** Must work with various language scripts

### 4.4 Maintainability

**NFR11:** Code must follow ktlint style
**NFR12:** Code must be well-documented with KDoc
**NFR13:** Tests must cover critical paths (>80% coverage)

## 5. Design Specifications

### 5.1 Component: MarkdownToAnnotatedString

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt`

**Public API:**
```kotlin
fun markdownToAnnotatedString(markdown: String): AnnotatedString
fun markdownToAnnotatedStringSafe(markdown: String): AnnotatedString
```

**Internal Functions:**
```kotlin
private fun parseMarkdownToHTML(markdown: String): String
private fun createMarkdownCleaner(): Cleaner
private fun sanitizeHTML(html: String): String
```

### 5.2 Component: MarkdownText

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Signature:**
```kotlin
@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
)
```

**Implementation:**
```kotlin
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

### 5.3 HTML Sanitization Whitelist

**Allowed Tags:**
- Headings: `h1`, `h2`, `h3`, `h4`, `h5`, `h6`
- Text formatting: `strong`, `b`, `em`, `i`, `u`, `sub`, `sup`
- Lists: `ul`, `ol`, `li`
- Code: `pre`, `code`
- Structural: `p`, `br`, `blockquote`
- Links: `a` (with `href` attribute only)

**Allowed Attributes:**
- `a.href` (http/https protocols only)

**Disallowed:**
- Script tags: `script`, `noscript`
- Embedded content: `iframe`, `embed`, `object`
- Forms: `form`, `input`, `button`
- Styles: `style`
- Event handlers: `onclick`, `onerror`, etc.

## 6. Testing Requirements

### 6.1 Unit Tests

**File:** `MarkdownToAnnotatedStringTest.kt`

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

**Coverage Target:** >80%

### 6.2 Integration Tests

**File:** `ArticleScreenTest.kt`

**Test Cases:**
1. `testSummarySectionDisplaysMarkdown()`
2. `testSummarySectionHandlesLoading()`
3. `testSummarySectionHandlesError()`
4. `testSummaryLinksClickable()`

### 6.3 Security Tests

**File:** `MarkdownSecurityTest.kt`

**Test Cases:**
1. `testScriptTagRemoved()`
2. `testOnClickAttributeRemoved()`
3. `testIframeRemoved()`
4. `testJavaScriptProtocolBlocked()`

### 6.4 Manual Testing

**Test Scenarios:**
1. Generate summary with various markdown elements
2. Tap on links → verify they open
3. Test in light mode
4. Test in dark mode
5. Test with RTL language
6. Test with very long summary
7. Test with malformed markdown

## 7. Deployment Plan

### 7.1 Phases

**Phase 1: Implementation** (Week 1)
- Create MarkdownToAnnotatedString.kt
- Update ArticleScreen.kt
- Write tests
- Verify build

**Phase 2: Testing** (Week 1-2)
- Run all tests
- Manual testing
- Security review
- Fix issues

**Phase 3: Beta Release** (Week 2-3)
- Release to beta users
- Monitor crash reports
- Gather feedback
- Fix issues

**Phase 4: Production Release** (Week 4)
- Release to all users
- Monitor metrics
- Address issues

### 7.2 Rollback Plan

**Immediate Rollback:**
1. Revert SummarySection to plain text
2. Keep MarkdownToAnnotatedString.kt (harmless)
3. Deploy hotfix

**Alternative:**
1. Add feature flag for markdown rendering
2. Remote configuration
3. Monitor before re-enabling

## 8. Monitoring and Metrics

### 8.1 Technical Metrics

- Markdown parsing success rate
- Error fallback rate
- Rendering performance (ms)
- Memory usage (MB)
- APK size increase (KB)

### 8.2 User Experience Metrics

- User engagement with summaries
- Crash reports
- User feedback
- Link click rate

### 8.3 Logging

```kotlin
private const val LOG_TAG = "MarkdownRenderer"

// Success logging
Log.d(LOG_TAG, "Successfully parsed ${markdown.length} chars")

// Error logging
Log.e(LOG_TAG, "Failed to parse markdown, falling back to plain text", exception)
```

## 9. Success Criteria

### 9.1 Must Have

- ✅ Markdown renders correctly with formatting
- ✅ Links are tappable and work properly
- ✅ No XSS vulnerabilities
- ✅ All tests pass
- ✅ Build succeeds without warnings
- ✅ Performance regression < 10%

### 9.2 Should Have

- ✅ Works in light and dark themes
- ✅ Accessible with screen reader
- ✅ Supports RTL languages
- ✅ Smooth scrolling

### 9.3 Nice to Have

- ✅ Enhanced error messages
- ✅ Performance optimizations
- ✅ Additional markdown elements

## 10. Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| XSS vulnerabilities | Low | High | Jsoup sanitization + security tests |
| Performance degradation | Very Low | Medium | Background parsing, caching |
| Breaking changes | Very Low | High | Extensive testing, rollback plan |
| APK size increase | None | N/A | Using existing dependencies |
| Compatibility issues | Very Low | Medium | Test on multiple API levels |

## 11. Open Questions

*None at this time*

## 12. Change Log

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-05 | Initial specification |

## 13. Approval

**Status:** ✅ Approved for Implementation

**Approved By:** Super Dev Coordinator Agent
**Date:** January 5, 2026

---

## Appendix A: Example Markdown

**Input (Markdown):**
```markdown
# Summary

This article discusses **AI-powered text summarization**.

Key points:
- Improves readability
- Saves time
- Enhances comprehension

For more info, see [the docs](https://example.com).

> "The future is AI-powered"
```

**Output (Formatted):**
- Heading "Summary" in large bold text
- Bold text "AI-powered text summarization"
- Bulleted list with proper bullets
- Tappable link "the docs"
- Blockquote with visual distinction

## Appendix B: References

- [CommonMark Spec](https://commonmark.org/)
- [JetBrains Markdown](https://github.com/JetBrains/markdown)
- [Jsoup HTML Cleaner](https://jsoup.org/apidocs/org/jsoup/safety/Cleaner.html)
- [Compose Text](https://developer.android.com/jetpack/compose/text)

---

**End of Specification**
