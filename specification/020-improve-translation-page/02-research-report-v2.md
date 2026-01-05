# Research Report - RSS Standards & Content Structure Analysis

**Spec Index:** 020-v2
**Research Date:** 2026-01-05
**Phase:** 3 - Research
**Status:** Complete

## Executive Summary

This research investigates RSS/Atom feed standards, HTML content encoding practices, and best practices for handling structured content in feed readers. The goal is to determine if the current translation approach can be improved by operating at a different level in the parsing/rendering pipeline.

## 1. RSS/Atom Feed Standards

### 1.1 RSS 2.0 Specification

**Key Elements:**
- **Channel Level:** `<title>`, `<link>`, `<description>`, `<language>`, `<item>` elements
- **Item Level:** `<title>`, `<link>`, `<description>`, `<content:encoded>`, `<author>`, `<category>`, `<guid>`, `<pubDate>`, `<enclosure>`

**HTML Content Handling:**
```xml
<!-- Method 1: Escaped HTML in description -->
<description>
  &lt;p&gt;This is a paragraph with &lt;strong&gt;bold&lt;/strong&gt; text.&lt;/p&gt;
</description>

<!-- Method 2: CDATA section (RECOMMENDED) -->
<content:encoded>
  <![CDATA[
    <p>This is a paragraph with <strong>bold</strong> text.</p>
    <ul>
      <li>List item 1</li>
      <li>List item 2</li>
    </ul>
  ]]>
</content:encoded>
```

**Best Practices:**
- Use `<content:encoded>` for full HTML content
- Wrap HTML in `<![CDATA[ ]]>` sections to avoid escaping issues
- Provide both `<description>` (plain text or simple HTML) and `<content:encoded>` (full HTML)
- Use semantic HTML5 elements (`<article>`, `<section>`, `<aside>`, etc.)

### 1.2 Atom 1.0 Specification

**Key Elements:**
- **Feed Level:** `<title>`, `<link>`, `<subtitle>`, `<entry>` elements
- **Entry Level:** `<title>`, `<link>`, `<content>`, `<summary>`, `<author>`, `<category>`, `<id>`, `<published>`, `<updated>`

**HTML Content Handling:**
```xml
<!-- Atom uses type attribute to specify content type -->
<content type="html">
  <![CDATA[
    <p>This is a paragraph with <strong>bold</strong> text.</p>
  ]]>
</content>

<content type="xhtml">
  <div xmlns="http://www.w3.org/1999/xhtml">
    <p>This is XHTML content</p>
  </div>
</content>
```

**Best Practices:**
- Use `type="html"` for HTML content with CDATA
- Use `type="text"` for plain text
- Use `type="xhtml"` for well-formed XHTML with proper namespaces
- Prefer `<content>` over `<summary>` for full article content

### 1.3 Common HTML Elements in RSS/Atom Feeds

**Structured Content Elements:**
```html
<!-- Paragraphs -->
<p>Regular paragraph text</p>

<!-- Lists -->
<ul>
  <li>Unordered list item</li>
  <li>Nested list:
    <ul>
      <li>Level 2 item</li>
      <li>Level 2 with deeper nesting:
        <ol>
          <li>Level 3 ordered item</li>
        </ol>
      </li>
    </ul>
  </li>
</ul>

<!-- Blockquotes -->
<blockquote>
  <p>Quoted paragraph</p>
  <p>Second quoted paragraph</p>
  <cite>Source citation</cite>
</blockquote>

<!-- Headings -->
<h1>Main heading</h1>
<h2>Subheading</h2>
<h3>Sub-subheading</h3>

<!-- Tables -->
<table>
  <tr><th>Header</th></tr>
  <tr><td>Data</td></tr>
</table>

<!-- Code Blocks -->
<pre><code>function example() {
  return "code";
}</code></pre>

<!-- Media -->
<img src="image.jpg" alt="Description" />
<video src="video.mp4"></video>
<audio src="audio.mp3"></audio>

<!-- Links -->
<a href="https://example.com">Link text</a>

<!-- Inline Formatting -->
<strong>Bold text</strong>
<em>Italic text</em>
<code>Inline code</code>
```

## 2. Content Encoding Best Practices

### 2.1 HTML Encoding Methods

**1. XML Entity Escaping (NOT RECOMMENDED for complex HTML):**
```xml
<description>&lt;p&gt;Paragraph&lt;/p&gt;</description>
```
- ❌ Hard to read and maintain
- ❌ Easy to make escaping errors
- ❌ Doesn't handle CDATA sections well
- ✅ Works for simple text only

**2. CDATA Sections (RECOMMENDED):**
```xml
<content:encoded><![CDATA[
  <p>Paragraph with <strong>bold</strong> text</p>
]]></content:encoded>
```
- ✅ Preserves HTML exactly as written
- ✅ No escaping issues
- ✅ Easier to read and debug
- ✅ Handles all HTML5 elements
- ⚠️ Cannot contain `]]>` sequence in content

**3. XHTML with Namespaces (Atom only):**
```xml
<content type="xhtml">
  <div xmlns="http://www.w3.org/1999/xhtml">
    <p>Well-formed XHTML content</p>
  </div>
</content>
```
- ✅ Well-formed and validated
- ✅ Supports namespaces
- ❌ Stricter than HTML5
- ❌ More verbose

### 2.2 Current Industry Practices (2025)

**Major Publishers:**
- **WordPress:** Uses `<content:encoded>` with CDATA for full content
- **Blogger:** Uses `<content>` element with HTML
- **Medium:** Uses `<content:encoded>` with full article HTML
- **News sites:** Mix of `<description>` (summary) and `<content:encoded>` (full)
- **Podcasts:** Use standard RSS with `<content:encoded>` for show notes

**Content Structure Patterns:**
```html
<!-- Typical blog post structure -->
<article>
  <h1>Article Title</h1>
  <p>Introduction paragraph with <a href="#">links</a> and <em>formatting</em>.</p>

  <h2>Section with Lists</h2>
  <ul>
    <li>Item 1</li>
    <li>Item 2 with nested content:
      <ul>
        <li>Sub-item 2.1</li>
        <li>Sub-item 2.2</li>
      </ul>
    </li>
  </ul>

  <blockquote>
    <p>Important quote from source</p>
  </blockquote>

  <h2>Code Examples</h2>
  <pre><code>function example() {
    return true;
  }</code></pre>

  <p>Conclusion paragraph</p>
</article>
```

## 3. Nested Content Structures

### 3.1 List Nesting Patterns

**Common Nesting Depths:**
- Level 1: Standard bullet points
- Level 2: Sub-points (very common)
- Level 3: Detailed breakdowns (common in technical docs)
- Level 4+: Rare but exists in complex documentation

**Real-World Examples:**
```html
<!-- Technical Documentation -->
<ol>
  <li>Install the software
    <ol>
      <li>Download installer</li>
      <li>Run setup
        <ol>
          <li>Choose installation directory</li>
          <li>Select components
            <ul>
              <li>Core files</li>
              <li>Documentation</li>
              <li>Examples</li>
            </ul>
          </li>
        </ol>
      </li>
    </ol>
  </li>
</ol>
```

### 3.2 Blockquote Nesting

**Patterns:**
```html
<!-- Simple blockquote -->
<blockquote>
  <p>Quote text</p>
</blockquote>

<!-- Blockquote with nested structures -->
<blockquote>
  <p>Introduction quote</p>
  <ul>
    <li>Point 1</li>
    <li>Point 2</li>
  </ul>
  <p>Conclusion quote</p>
</blockquote>

<!-- Nested blockquotes (rare) -->
<blockquote>
  <p>Outer quote</p>
  <blockquote>
    <p>Inner quote</p>
  </blockquote>
</blockquote>
```

### 3.3 Mixed Content Patterns

**Real-world complex structures:**
```html
<blockquote>
  <p>As stated in <a href="#">the documentation</a>:</p>
  <ul>
    <li>First principle
      <ul>
        <li>Sub-principle 1.1</li>
        <li>Sub-principle 1.2</li>
      </ul>
    </li>
    <li>Second principle
      <blockquote>
        <p>Additional quote within list</p>
      </blockquote>
    </li>
  </ul>
  <p><cite>— Author Name</cite></p>
</blockquote>
```

## 4. Feed Reader Implementation Patterns

### 4.1 Parsing Approaches

**1. DOM-Based Parsing (Current Feeder Approach):**
- Parse HTML into document object model
- Traverse tree to extract structured content
- Linearize into custom data structures
- ✅ Preserves structure and hierarchy
- ✅ Handles nested content correctly
- ❌ Memory intensive for large documents
- ❌ Complex traversal logic

**2. Streaming/SAX Parsing:**
- Event-driven parsing
- Process elements as they're encountered
- ✅ Low memory footprint
- ✅ Fast for large documents
- ❌ Hard to maintain nested structure
- ❌ Complex state management

**3. Regex/Pattern Matching:**
- Extract patterns using regex
- ✅ Simple for flat content
- ❌ Breaks on nested structures
- ❌ Fragile and error-prone

### 4.2 Content Storage Strategies

**1. Store Raw HTML:**
```kotlin
data class Article(
    val id: Long,
    val rawHtml: String,  // Original HTML from feed
    val parsedContent: LinearArticle  // Parsed structure
)
```
- ✅ Can reparse if needed
- ✅ Original content preserved
- ❌ Double storage
- ❌ Reparsing overhead

**2. Store Parsed Structure Only:**
```kotlin
data class Article(
    val id: Long,
    val parsedContent: LinearArticle  // Parsed structure only
)
```
- ✅ Single storage
- ✅ Fast rendering
- ❌ Can't recover original HTML
- ❌ Reparsing impossible

**3. Hybrid (Current Feeder Approach):**
```kotlin
data class Article(
    val id: Long,
    val blobFile: File,  // Raw HTML stored as blob
    // LinearArticle generated on-demand from blob
)
```
- ✅ Best of both worlds
- ✅ Flexible reparsing
- ⚠️ File I/O overhead
- ⚠️ Complex file management

## 5. Translation Strategies for Feeds

### 5.1 Translation Timing Options

**Option 1: Translate at Parse Time (EARLY)**
```kotlin
// During feed parsing
ParsedFeed(
    items = listOf(
        ParsedArticle(
            originalText = "<p>Hello</p>",
            translatedText = "<p>Hola</p>",  // Translate immediately
            // Store both versions
        )
    )
)
```
- ✅ Translation cached for all time
- ✅ No translation latency at display time
- ❌ Translations can't be updated
- ❌ Storage overhead
- ❌ Can't change target language later

**Option 2: Translate at Display Time (LATE - Current Approach)**
```kotlin
// During article display
fun displayArticle(article: Article) {
    val originalContent = parseArticleContent(article)
    val translatedContent = translate(originalContent)  // Translate on demand
    render(originalContent, translatedContent)
}
```
- ✅ Flexible target language
- ✅ No storage overhead
- ✅ Can update translations
- ❌ Translation latency
- ❌ Need to retranslate for each view

**Option 3: Translate at Storage Time (MIDDLE)**
```kotlin
// After parsing, before storage
ParsedArticle(
    originalHtml = "<p>Hello</p>",
    // Translation cache stored separately
)
TranslationCache(
    articleId = article.id,
    targetLanguage = "es",
    translation = "<p>Hola</p>"
)
```
- ✅ Translation cached
- ✅ Can cache multiple languages
- ✅ Can invalidate/update cache
- ⚠️ Cache management complexity
- ⚠️ Storage overhead

### 5.2 Translation Granularity Options

**Option 1: Document-Level Translation**
```kotlin
// Translate entire document at once
val translation = translate(article.fullHtmlText)
```
- ✅ Maintains document context
- ✅ Better for quality
- ❌ Can't mix languages
- ❌ Large translation requests
- ❌ All or nothing

**Option 2: Paragraph-Level Translation (Current Approach)**
```kotlin
// Extract paragraphs, translate each
val paragraphs = extractParagraphs(article)
val translations = paragraphs.map { translate(it) }
```
- ✅ Granular control
- ✅ Can handle mixed content
- ✅ Better error recovery
- ⚠️ Context loss between paragraphs
- ⚠️ Index matching complexity

**Option 3: Element-Level Translation**
```kotlin
// Translate each HTML element
fun translateElement(element: Element): Element {
    when (element) {
        is TextElement -> translateText(element)
        is ListElement -> translateList(element)
        is BlockquoteElement -> translateBlockquote(element)
        // Preserve code blocks, images, etc.
    }
}
```
- ✅ Maximum structure preservation
- ✅ Can skip non-translatable elements
- ❌ Complex implementation
- ❌ Context fragmentation

## 6. Recommendations

### 6.1 For RSS/Atom Feed Handling

**Best Practices:**
1. Support both RSS 2.0 and Atom 1.0
2. Prefer `<content:encoded>` or `<content>` over `<description>`
3. Handle CDATA sections correctly
4. Preserve HTML structure from feeds
5. Support nested HTML elements (lists, blockquotes)

**Content Storage:**
1. Store raw HTML from feed
2. Parse to linear structure on demand
3. Cache parsed structure
4. Allow reparsing for updates

### 6.2 For Translation Implementation

**Recommended Approach:**

**Translation Timing:** Display-time (current approach is good)
- Rationale: Flexibility for language changes, no storage overhead

**Translation Granularity:** Element-level with paragraph chunks
- Rationale: Balances structure preservation with API efficiency

**Proposed Architecture:**
```kotlin
// Translation-aware parsing
data class TranslatableContent(
    val elements: List<TranslatableElement>
)

sealed class TranslatableElement {
    data class TextParagraph(
        val id: String,
        val originalText: String,
        val translation: String? = null,  // Cached translation
        val blockStyle: BlockStyle
    ) : TranslatableElement()

    data class NestedStructure(
        val id: String,
        val type: StructureType,  // List, Blockquote, etc.
        val content: List<TranslatableElement>,
        val translations: Map<String, String>? = null  // Cached by element ID
    ) : TranslatableElement()
}
```

### 6.3 For Nested Content Handling

**Current Assessment:**
- ✅ Feeder correctly parses nested HTML from feeds
- ✅ LinearArticle structure supports nesting
- ⚠️ Translation extraction needs recursive traversal (already fixed in spec-020)
- ⚠️ Translation display needs index matching for nested elements

**Recommended Improvements:**
1. Ensure recursive extraction covers all nested types
2. Maintain element ID hierarchy for translation matching
3. Preserve structure during translation display
4. Handle mixed content (lists in blockquotes, etc.)

## 7. Key Findings

### 7.1 RSS/Atom Standards

**Finding 1:** HTML content in feeds is well-standardized
- RSS uses `<content:encoded>` with CDATA
- Atom uses `<content>` with type attribute
- Both support full HTML5 content

**Finding 2:** Nested structures are common and expected
- Technical documentation uses 3-4 level nesting
- Blockquotes often contain lists
- Mixed nesting patterns exist

**Finding 3:** No single "correct" encoding method
- CDATA is most common for complex HTML
- Entity escaping used for simple content
- XHTML namespaces used in Atom

### 7.2 Translation Architecture

**Finding 1:** Current approach (display-time translation) is sound
- Allows language flexibility
- No storage overhead
- Industry-standard approach

**Finding 2:** Paragraph-level granularity is reasonable
- Balances API efficiency with context
- Allows mixed content handling
- Industry standard practice

**Finding 3:** Improvements needed in nested content handling
- Recursive extraction essential (spec-020 addresses this)
- Element ID tracking needed for accurate matching
- Structure preservation during display

### 7.3 Data Flow Analysis

**Finding 1:** Feeder's parsing pipeline is solid
- GoFeed adapter handles RSS/Atom correctly
- HTML linearization preserves structure
- Blob storage allows reparsing

**Finding 2:** Translation happens at correct level
- Translation operates on LinearArticle (post-parsing)
- Good separation of concerns
- Flexible for different content types

**Finding 3:** Display rendering needs enhancement
- Must support nested translation display
- Index matching must account for hierarchy
- Visual consistency needed

## 8. Conclusion

The current architecture is fundamentally sound. The translation approach (display-time, paragraph-level) is appropriate for a feed reader. The main improvements needed are:

1. **Ensure complete recursive extraction** of nested content (spec-020)
2. **Enhance index matching** to handle nested structures correctly
3. **Improve rendering** to display translations at appropriate nesting levels

No architectural changes are needed to the parsing or translation pipeline. The improvements are localized to the extraction and display logic.

---

**Research Complete**
**Ready for Phase 4: Code Assessment**
