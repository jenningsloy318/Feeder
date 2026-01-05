# Research Report - Spec 019: Markdown Rendering for Summaries

**Research Date:** January 5, 2026
**Researcher:** Super Dev Coordinator Agent

## Executive Summary

This research report evaluates options for implementing markdown rendering in the Feeder Android app's AI summary feature. The project already has excellent infrastructure in place: JetBrains Markdown library and Jsoup for HTML parsing with custom AnnotatedString composition.

**Recommended Approach:** Extend existing HTML-to-AnnotatedString infrastructure to convert markdown to HTML first, then render using the proven HTML pipeline.

## Available Libraries

### 1. JetBrains Markdown (Already in Project) ⭐ RECOMMENDED

**Dependency:** `implementation(libs.jetbrains.markdown)`

**Pros:**
- Already included in the project
- Official JetBrains library
- Lightweight
- Well-maintained
- Converts markdown to HTML
- CommonMark compliant

**Cons:**
- Requires markdown→HTML conversion first
- Needs HTML sanitization for security

**Usage Pattern:**
```kotlin
import org.intellij.markdown.flavours.gfm.GFMFlavour
import org.intellij.markdown.parser.MarkdownParser

val flavour = GFMFlavour()
val parser = MarkdownParser(flavour)
val htmlTree = parser.buildMarkdownTreeFromString(markdown)
// Convert to HTML, then use existing HtmlToAnnotatedString
```

### 2. MikePenz Multiplatform Markdown Renderer

**Source:** [GitHub - mikepenz/multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)

**Pros:**
- Native Compose implementation
- Material Design 3 support
- Cross-platform
- Highly customizable
- Recommended by community in 2025

**Cons:**
- Adds new dependency (~200KB)
- More complex setup
- Overkill for simple summaries
- Not already in project

**Relevant Articles:**
- [Markdown Editing in Jetpack Compose - Cantilever](https://www.cantilevers.org/posts/2025/09/markdown-editing-in-jetpack-compose)
- [Mastering Markdown Rendering in Jetpack Compose](https://medium.com/@sivavishnu0705/bridging-the-gap-mastering-markdown-rendering-in-jetpack-compose-ffd25f1a5685)

### 3. Markdown-Compose by Yazan98

**Source:** [GitHub - Yazan98/Markdown-Compose](https://github.com/Yazan98/Markdown-Compose)

**Pros:**
- Native Jetpack Compose
- Lightweight
- Easy to use

**Cons:**
- Less active maintenance
- Fewer features
- Not Material 3 optimized

## Existing Infrastructure Analysis

### HTML to AnnotatedString Pipeline

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/HtmlToAnnotatedString.kt`

**Current Capabilities:**
- ✅ Converts HTML to AnnotatedString
- ✅ Uses Jsoup for parsing
- ✅ Supports: headings (h1-h6), bold, italic, links, lists, code, blockquotes, tables
- ✅ Safe: Jsoup provides XSS protection by default
- ✅ Well-tested and proven in production

**Key Features:**
- Paragraph formatting with `emitParagraph()`
- Style application with `withStyle()`
- Annotation support for links
- Preformatted text for code blocks
- List handling (ordered and unordered)
- Table rendering

### Security Considerations

**XSS Prevention Resources:**
- [Preventing XSS when user edits original Markdown input - Stack Overflow](https://stackoverflow.com/questions/16539717/preventing-xss-when-user-edits-original-markdown-input)
- [Exploiting XSS via Markdown - Medium](https://medium.com/taptuit/exploiting-xss-via-markdown-72a61e774bf8)
- [XSS in Rich Text Editor - GitHub Security Advisory](https://github.com/laurent22/joplin/security/advisories/GHSA-5w3c-wj9-hq92)

**Best Practices:**
1. **Never trust user input** - Even from AI models
2. **Sanitize HTML** - Jsoup `Cleaner` with whitelist
3. **Disable scripts** - Remove `<script>`, `onclick`, etc.
4. **Limit images** - Prevent external image loading
5. **Validate URLs** - Ensure links use safe protocols

**Jsoup Safety:**
```kotlin
import org.jsoup.safety.Safelist
import org.jsoup.safety.Cleaner

// Create a whitelist for safe HTML
val safelist = Safelist.relaxed()
    .addTags("h1", "h2", "h3", "h4", "h5", "h6")
    .addTags("strong", "b", "em", "i", "code", "pre")
    .addTags("ul", "ol", "li", "blockquote", "p")
    .addTags("a")
    .addAttributes("a", "href")
    .addProtocols("a", "href", "http", "https")

val cleaner = Cleaner(safelist)
val cleanDoc = cleaner.clean(dirtyDoc)
```

## Recommended Implementation Strategy

### Phase 1: Markdown to HTML Conversion
1. Use existing JetBrains Markdown library
2. Configure for CommonMark/GFM (GitHub Flavored Markdown)
3. Generate HTML from markdown text

### Phase 2: HTML Sanitization
1. Configure Jsoup Cleaner with appropriate whitelist
2. Remove dangerous elements (scripts, iframes, etc.)
3. Allow safe elements only (formatting, links, lists, code)

### Phase 3: HTML to AnnotatedString
1. Reuse existing `HtmlToAnnotatedString.kt` infrastructure
2. Create a wrapper function: `markdownToAnnotatedString()`
3. Handle errors gracefully (fallback to plain text)

### Phase 4: Compose UI Integration
1. Replace `Text` composable in `SummarySection`
2. Use `AnnotatedString` with proper styling
3. Ensure Material 3 theming
4. Test accessibility

## Performance Considerations

### Markdown Parsing Performance
- JetBrains Markdown: Fast enough for typical summaries (100-1000 words)
- Parsing happens on background thread in ViewModel
- Result cached in ViewState

### HTML Sanitization Performance
- Jsoup Cleaner: Fast, mature, well-optimized
- Should be done on background thread
- Can cache sanitized HTML if needed

### Rendering Performance
- AnnotatedString: Native Compose, very efficient
- LazyColumn: Already used for article content
- No scroll performance impact expected

## Comparison Matrix

| Feature | JetBrains Markdown + Jsoup | MikePenz Renderer |
|---------|---------------------------|-------------------|
| **Dependencies** | ✅ Already in project | ❌ New dependency |
| **Size impact** | ✅ 0 KB added | ❌ ~200 KB |
| **Security** | ✅ Jsoup sanitization | ⚠️ Need to verify |
| **Compose Native** | ✅ Via AnnotatedString | ✅ Native Compose |
| **Maturity** | ✅ Very mature | ✅ Mature |
| **Material 3** | ✅ With styling | ✅ Built-in |
| **Maintenance** | ✅ Official JetBrains | ✅ Community |
| **Customization** | ✅ High (HTML layer) | ✅ High |
| **Learning Curve** | ✅ Low (reuse existing) | ⚠️ Medium |
| **Integration** | ✅ Seamless | ⚠️ Requires changes |

## Conclusion

**Recommended Approach:** Use JetBrains Markdown library + existing Jsoup/AnnotatedString infrastructure

**Rationale:**
1. **Zero new dependencies** - Uses existing libraries
2. **Proven security** - Jsoup sanitization is battle-tested
3. **Code reuse** - Extends existing HTML rendering pipeline
4. **Minimal changes** - Small, focused implementation
5. **Performance** - Fast and efficient
6. **Safety** - Battle-tested HTML sanitization

**Alternative:** Consider MikePenz renderer if project needs rich interactive markdown features (syntax highlighting, image embedding, etc.) in the future.

## Implementation Roadmap

1. **Create `MarkdownToAnnotatedString.kt`**
   - Convert markdown to HTML using JetBrains Markdown
   - Sanitize HTML using Jsoup Cleaner
   - Convert to AnnotatedString using existing infrastructure

2. **Update `ArticleScreen.kt`**
   - Replace `Text(summary.value.content)` with markdown renderer
   - Handle both Success and Error states
   - Fallback to plain text on error

3. **Testing**
   - Unit tests for markdown parsing
   - Security tests for XSS prevention
   - UI tests for rendering
   - Performance tests

4. **Documentation**
   - Update inline comments
   - Document supported markdown features
   - Add security notes

## References

- [MikePenz Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)
- [Markdown Editing in Jetpack Compose - Cantilever](https://www.cantilevers.org/posts/2025/09/markdown-editing-in-jetpack-compose)
- [Mastering Markdown Rendering in Jetpack Compose - Medium](https://medium.com/@sivavishnu0705/bridging-the-gap-mastering-markdown-rendering-in-jetpack-compose-ffd25f1a5685)
- [Rendering Markdown with Code Syntax Highlighting](https://levelup.gitconnected.com/rendering-markdown-with-code-syntax-highlighting-in-compose-android-f8cda0647c87)
- [Yazan98/Markdown-Compose](https://github.com/Yazan98/Markdown-Compose)
- [Preventing XSS in Markdown - Stack Overflow](https://stackoverflow.com/questions/16539717/preventing-xss-when-user-edits-original-markdown-input)
- [Exploiting XSS via Markdown - Medium](https://medium.com/taptuit/exploiting-xss-via-markdown-72a61e774bf8)
- [Joplin XSS Security Advisory](https://github.com/laurent22/joplin/security/advisories/GHSA-5w3c-wj9-hq92)
