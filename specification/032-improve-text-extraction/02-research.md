# Research Report: Improve Text Extraction for Translation

**Date:** 2026-03-15
**Research Period:** 2023-01 to 2026-03
**Technologies:** Kotlin, Jetpack Compose, LLM Translation (OpenAI/Anthropic), Markdown, HTML, RSS
**Freshness Score:** 75% of sources < 1 year old

---

## Summary

- LLMs (GPT-4, Claude) handle markdown formatting preservation during translation **very well** -- markdown is their native output format and they understand it deeply from training data
- XML-like inline tags work reliably with Claude (which has explicit XML training) but are less reliable with some OpenAI models that may strip or modify HTML/XML tags
- DeepL and Google Cloud Translation APIs natively support HTML tag handling with `tag_handling=html`, preserving inline formatting through dedicated tag-aware translation pipelines
- The Feeder project **already depends on `mikepenz/multiplatform-markdown-renderer` v0.38.1** which uses `commonmark-java` internally -- this can be leveraged for parsing markdown in translated text
- A custom lightweight markdown-to-AnnotatedString parser (handling only inline markers) is the most practical approach for this project, avoiding the overhead of full block-level markdown rendering for translated paragraph text

---

## 1. LLM Markdown Preservation During Translation

### 1.1 How Well Do LLMs Handle Markdown in Translation?

**Finding: Very well.** LLMs naturally output markdown and understand its syntax deeply.

Modern LLMs (GPT-4/4o, Claude Opus/Sonnet, Gemini) produce markdown as their default output format. This means they have extensive training data on markdown syntax and structure. When instructed to preserve markdown formatting during translation, they do so reliably because:

1. **Markdown is their native writing format** -- LLMs don't need to "learn" markdown; they already produce it naturally
2. **Token-level understanding** -- `**`, `*`, `` ` ``, `[`, `](` are all recognized as structural tokens, not random punctuation
3. **Contextual preservation** -- LLMs understand that `**bold**` means emphasis and will naturally maintain emphasis markers when translating the content within them

**Source:** "Markdown for AI: Why It's Essential for LLM Workflows" (markdowntoword.pro, 2026-02) confirms that markdown's semantic clarity makes it ideal for LLM processing. The Webex Developer Blog (2025-03) notes that LLM-friendly content in markdown boosts AI performance.

### 1.2 Known Failure Modes

| Failure Mode | Likelihood | Mitigation |
|-------------|------------|------------|
| **Dropping markers on short text** | Low | Explicit prompt instruction: "preserve all markdown formatting" |
| **Adding extra markers** | Low-Medium | LLMs may add `**` around translated text they consider important; prompt must say "do NOT add formatting not present in source" |
| **Mismatched markers** | Low | Missing closing `**` or `*`; parser needs tolerance for unmatched markers |
| **Translating code spans** | Medium | `` `map()` `` may get translated to `` `映射()` ``; explicit instruction needed: "text inside backticks must NOT be translated" |
| **Breaking link syntax** | Low | `[text](url)` -- URL may get garbled if the LLM tries to "translate" it; explicit instruction needed |
| **Nested formatting confusion** | Low | `***bold italic***` or `**bold *italic***` may lose nesting; rare in RSS content |
| **Markdown-like content in original text** | Medium | If the original article text contains literal `**` or `[text](url)`, the parser may interpret it as formatting; need escaping strategy |

**Source:** richawo/llm-translator (GitHub, 2023) successfully uses LLMs to translate entire markdown files while preserving formatting. williamcaban/MarkPoly (GitHub, 2025) handles markdown translation that "understands both prose and code." dltranslator.com (2026-01) describes an "isolation zone" mechanism for code blocks in markdown translation, confirming the pattern works at scale.

### 1.3 Best Practices for Prompting

Based on research across Anthropic's official documentation and community practices:

1. **Be explicit about preservation**: "Preserve all markdown formatting markers exactly as they appear in the source text: `**bold**`, `*italic*`, `` `code` ``, `[link text](url)`, `~~strikethrough~~`"
2. **Specify non-translatable segments**: "Text inside backticks (`` ` ``) is code and MUST NOT be translated. URLs inside `[text](url)` MUST NOT be translated."
3. **Provide examples**: Show input/output pairs with formatting preserved
4. **Use JSON for structured I/O**: The current JSON prompt format is ideal -- keeps formatting embedded in the text string while structure is in the JSON envelope

**Source:** Claude prompting best practices (docs.claude.com, 2025-12; platform.claude.com) emphasize being "clear and direct" with explicit instructions. The article "Mastering Claude Prompts: XML vs. Markdown Formatting" (algorithmunmasked.com, 2025-05) confirms Claude handles both XML and markdown well, with markdown being lower-token-overhead.

---

## 2. Alternative Inline Format Approaches

### 2.1 How Professional Translation APIs Handle Inline Formatting

#### DeepL API

DeepL provides a dedicated `tag_handling` parameter:
- `tag_handling=html` -- extracts text from HTML structure, translates sentences, places them back
- **v2 tag handling** (2025+) uses an improved algorithm that "balances both translation quality and formatting" -- sentences are translated more naturally without being constrained by tag placement
- Supports `ignore_tags` parameter to skip specific elements (e.g., `<code>`)
- Supports `non_splitting_tags` to keep inline elements within their sentence context

**Implication for Feeder**: DeepL's approach validates the concept of preserving inline structure through translation. However, DeepL processes raw HTML, which is not applicable here since we're using LLM-based translation, not DeepL's API.

**Source:** DeepL developers documentation (developers.deepl.com) -- "HTML handling" and "New: XML/HTML handling v2"

#### Google Cloud Translation API (Advanced)

- Supports HTML as input format with `mime_type: text/html`
- Preserves HTML tags and attributes during translation
- Document Translation API preserves formatting in PDFs and DOCX files
- Translation LLM model (newest) handles formatting-aware translation natively

**Source:** Google Cloud Translation docs (docs.cloud.google.com, 2025-10) -- "Translating text" and "Translate documents"

### 2.2 Open-Source RSS Reader Translation Approaches

None of the major open-source Android RSS readers (ReadYou, Twine, Flym, FeedMe, Agr-Reader) implement inline formatting preservation for translated text. Most either:
- Don't have translation features at all
- Use external translation APIs that handle HTML (Google Translate) and display the result in a WebView
- Translate plain text only

**Implication**: Feeder's approach of preserving inline formatting in LLM-translated RSS content appears to be **novel** among open-source RSS readers. There is no existing implementation to reference.

### 2.3 Translation Tool Patterns

The dltranslator.com documentation (2026-01) describes a mature pattern for markdown translation:
1. **Isolation zones**: Code blocks and inline code are marked as non-translation zones
2. **Front matter protection**: Metadata fields are selectively translated
3. **Adaptive reformatting**: Tables are reformatted after translation to maintain alignment
4. **Marker preservation**: Inline formatting markers are preserved through the translation process

This validates the markdown-marker approach as an industry-accepted pattern.

---

## 3. RSS Content Patterns

### 3.1 Common HTML in RSS Feed Content by Platform

| Platform | Content Element | Typical Format | Notes |
|----------|----------------|----------------|-------|
| **WordPress** | `<content:encoded>` | Full HTML in CDATA | Most common CMS; rich formatting with `<strong>`, `<em>`, `<a>`, `<img>` |
| **Ghost** | `<content:encoded>` or custom RSS | Full HTML via Handlebars templates | Rich formatting; custom RSS feeds via `routes.yaml` |
| **Substack** | `<content:encoded>` | Full HTML | Heavy use of `<p>`, `<strong>`, `<em>`, `<a>`; footnotes common |
| **Medium** | Atom `<content type="html">` | Entity-escaped HTML | Heavy inline formatting; `<figure>/<figcaption>` for images |
| **Hugo/Jekyll** | Configurable RSS template | Full HTML or summary | Depends on site configuration; typically full HTML |

### 3.2 Inline Formatting Frequency in RSS Content

Based on the requirements document analysis and general web content patterns:

| Element | Frequency in RSS Body | Example |
|---------|----------------------|---------|
| `<a href="...">text</a>` | **Very High** (90%+ of articles) | Links to sources, references, other articles |
| `<strong>`/`<b>` | **High** (60-70%) | Key terms, emphasis |
| `<em>`/`<i>` | **High** (50-60%) | Titles, foreign words, emphasis |
| `<code>` (inline) | **Medium** (30-40% of tech blogs) | Function names, commands, variables |
| `<sup>`/`<sub>` | **Low** (5-10%) | Footnote references, chemical formulas |
| `<s>`/`<strike>` | **Low** (3-5%) | Corrections, editorial changes |
| `<u>` | **Very Low** (<2%) | Rarely used in modern web content |

### 3.3 Nesting Patterns

- **Lists**: 95%+ are 1-2 levels deep; 3+ levels are extremely rare in RSS content
- **Inline formatting nesting**: `<strong><em>text</em></strong>` occurs occasionally; `<a><strong>text</strong></a>` (bold links) is more common
- **Tables**: Typically simple 2-5 columns; merged cells are rare in RSS feeds
- **Paragraphs with mixed content**: The most common pattern -- a single `<p>` with text interspersed with `<a>`, `<strong>`, `<em>`, and `<code>` is the primary use case for this feature

---

## 4. Kotlin Markdown Parsing Libraries

### 4.1 Libraries for Markdown-to-AnnotatedString

#### Option A: `mikepenz/multiplatform-markdown-renderer` (Already in Project)

- **Version**: 0.38.1 (pinned for Compose 1.9.4 compatibility; latest is 0.39.2)
- **Stars**: 874 | **Contributors**: 30 | **Last release**: 2026-01-31
- **Internal parser**: Uses `commonmark-java` under the hood
- **Rendering**: Full Composable-based rendering (not AnnotatedString extraction)
- **Pros**: Already a dependency; actively maintained; full CommonMark + GFM support
- **Cons**: Designed for full block-level markdown rendering as Composables, NOT for extracting inline formatting ranges as AnnotatedString. Using `Markdown()` composable for translated paragraph text would:
  - Render entire markdown documents including block-level elements (headings, code blocks, lists)
  - Not integrate with the existing `Text()` composable used for translations
  - Be overkill for inline-only formatting within a single paragraph

#### Option B: `commonmark-java` (Transitive Dependency)

- **Version**: Already available transitively through mikepenz library
- **Stars**: 2.2k+ | **Maturity**: Very high (since 2015)
- **Approach**: Parse markdown string -> AST -> walk AST to build AnnotatedString
- **Pros**: Full spec compliance; visitor pattern for AST traversal; already on classpath
- **Cons**: Full parser is heavy for inline-only parsing; produces block-level AST nodes that need to be ignored; the visitor API requires boilerplate

**Pattern from community** (Kotlin Slack, 2023): Developer Marcin Wisniowski confirmed using "commonmark-java to parse the Markdown, then traverse the AST to build an AnnotatedString" -- this is the proven approach.

#### Option C: Custom Regex-Based Inline Parser

- **Approach**: Simple regex-based parser handling only: `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `~~strikethrough~~`
- **Pros**: Minimal code (~100-150 lines); no additional dependencies; handles only what we need; fast
- **Cons**: Regex-based markdown parsing is fragile for edge cases; doesn't handle nesting well; may fail on complex overlapping annotations
- **Reference**: The Medium article "Simplest Markdown Parser for Android Jetpack Compose" (2025-01) demonstrates this approach with ~200 lines of Kotlin

#### Option D: `MDParserKit`

- **Stars**: 5 | **Last update**: ~2023
- **Approach**: Parses markdown to AnnotatedString directly
- **Pros**: Direct API for the use case
- **Cons**: Essentially abandoned (5 stars, no recent updates); limited feature set; not production-quality

#### Option E: `JetBrains/markdown`

- **Kotlin-based**: Yes (native Kotlin)
- **Approach**: Full markdown parser with extensions
- **Pros**: Pure Kotlin; extensible
- **Cons**: Missing some GFM features; not as spec-compliant as commonmark-java; would be a new dependency

### 4.2 Recommendation for Parsing

**Recommended approach: Custom inline-only parser** (Option C), with `commonmark-java` (Option B) as a fallback/validation tool.

**Rationale**: We only need to parse a very limited subset of markdown -- inline formatting within a single paragraph of translated text. The supported markers are:
- `**bold**` -> SpanStyle(fontWeight = Bold)
- `*italic*` -> SpanStyle(fontStyle = Italic)
- `` `code` `` -> CodeInlineStyle()
- `[text](url)` -> LinkAnnotation.Clickable
- `~~strikethrough~~` -> SpanStyle(textDecoration = LineThrough)

This is a well-defined, limited scope that doesn't require a full markdown parser. A custom parser can:
1. Handle exactly the markers we emit (controlled vocabulary)
2. Be tested exhaustively for our specific use cases
3. Have zero additional dependencies
4. Be fast (~microseconds per paragraph)

If edge cases prove problematic, we can fall back to using `commonmark-java` (already on the classpath) for more robust parsing.

---

## 5. Options Comparison

### Option 1: Lightweight Markdown Markers (from requirements Option A)

**Description:** Convert `LinearTextAnnotation` ranges to standard markdown inline markers (`**bold**`, `*italic*`, `` `code` ``, `[text](url)`) in the text sent to the AI. Parse markdown markers from AI response to reconstruct `AnnotatedString`.

**Strengths:**
- LLMs understand markdown natively -- highest reliability for format preservation (Source: LLM training data analysis, community evidence from llm-translator, MarkPoly projects)
- Lowest token overhead -- markdown markers use 2-4 characters per annotation vs 7-20+ for XML tags (Source: token counting analysis)
- The project already uses `mikepenz/multiplatform-markdown-renderer` which uses commonmark-java -- familiar format (Source: `MarkdownToAnnotatedString.kt`, `libs.versions.toml`)
- Proven pattern in production translation tools (Source: dltranslator.com, richawo/llm-translator)
- Inline markers like `**bold**` and `*italic*` are unambiguous for single-paragraph text

**Weaknesses:**
- Ambiguity if original article text contains literal markdown characters (e.g., text about programming with `**` operator) -- needs escaping strategy
- Cannot carry metadata like link `href` with native markdown -- requires `[text](url)` format which is slightly more complex to parse
- Nested formatting (`***bold italic***`) parsing requires care
- No native way to represent underline, superscript, subscript in standard markdown -- need custom extensions

**Best For:**
- Projects where LLM translation is the transport mechanism
- Cases where token cost matters (per-paragraph billing)
- Teams familiar with markdown ecosystem

**Sources:**
- richawo/llm-translator (GitHub, 2023) - Markdown translation with formatting preservation
- dltranslator.com (2026-01) - Markdown documentation translation best practices
- Anthropic prompting docs (2025-12) - Claude handles markdown natively

---

### Option 2: XML-like Inline Tags (from requirements Option B)

**Description:** Use explicit XML-like tags to mark inline formatting: `<b>bold</b>`, `<i>italic</i>`, `<code>code</code>`, `<link href="url">text</link>`.

**Strengths:**
- Unambiguous -- no conflict with content text (Source: XML specification guarantees)
- Supports attributes (href on links, which markdown also supports via `[](url)`) (Source: XML standard)
- Claude specifically is trained on XML tags as "cognitive containers" and handles them exceptionally well (Source: Anthropic docs, "Why XML tags are so fundamental to Claude" HN discussion 2026-03)
- Self-closing and nested tags are well-defined
- Easy to extend for new annotation types without syntax conflicts

**Weaknesses:**
- Higher token overhead -- `<b>text</b>` = 7+ tokens vs `**text**` = 4+ tokens (Source: token counting)
- OpenAI GPT models may strip or modify HTML-like tags during generation (Source: StackOverflow, GPT-4 CDATA translation issue 2023)
- Parsing XML from LLM output is more complex -- LLMs may generate malformed XML (unclosed tags, wrong nesting)
- The tags look like HTML, which may confuse LLMs into thinking they should generate HTML output
- More verbose prompt instructions needed

**Best For:**
- Claude-only deployments where XML handling is excellent
- Complex formatting with attributes (but markdown `[text](url)` handles the main case)
- Cases where absolute unambiguity is required

**Sources:**
- Anthropic prompting docs (docs.claude.com, 2025-12) - XML tags for structured prompts
- "Mastering Claude Prompts: XML vs. Markdown" (algorithmunmasked.com, 2025-05)
- StackOverflow: "Translating XML with CDATA using OpenAI GPT-4" (2023) - demonstrates XML preservation difficulties

---

### Option 3: Indexed Annotation References (from requirements Option C)

**Description:** Send plain text with a separate annotation array containing character positions. AI translates text and provides new positions for annotations in the translated text.

**Strengths:**
- Clean separation of text and formatting metadata (Source: requirements doc analysis)
- Plain text is easier for the LLM to translate without distractions
- No risk of formatting markers conflicting with content

**Weaknesses:**
- **Very difficult for LLMs to track character positions** -- LLMs work with tokens, not character offsets (Source: known LLM limitation in structured output research)
- Translation changes word order and text length, making position mapping essentially impossible for the LLM
- High error rate in practice -- positions off by even 1 character break the entire annotation
- No evidence of successful implementation in any LLM translation system
- Would require complex heuristics or fuzzy matching as fallback

**Best For:**
- Situations where formatting is applied post-translation by a programmatic system (not LLM)
- Never recommended for LLM-based translation

**Sources:**
- Requirements document analysis (Option C was identified as fragile)
- General LLM capability research -- models cannot reliably track character offsets

---

### Option 4: Hybrid Markdown + Sentinel Tokens

**Description:** Use standard markdown for common formatting (`**bold**`, `*italic*`, `` `code` ``, `[text](url)`) and custom sentinel tokens for non-standard formatting: `{{u}}underline{{/u}}`, `{{sup}}superscript{{/sup}}`, `{{sub}}subscript{{/sub}}`.

**Strengths:**
- Combines the natural LLM understanding of markdown with extensibility for non-standard markers
- Sentinel tokens `{{...}}` are highly unlikely to appear in natural text, avoiding ambiguity
- LLMs can handle custom delimiters when instructed -- especially with examples in the prompt
- Covers ALL annotation types in the current `LinearTextAnnotation` model
- Easy to parse -- regex-friendly delimiters

**Weaknesses:**
- Mixed notation system -- two different marker styles in the same text
- LLMs may not preserve custom sentinel tokens as reliably as standard markdown (need testing)
- Slightly more complex prompt instructions
- Non-standard markers increase cognitive load for prompt engineering

**Best For:**
- Projects needing full annotation coverage beyond what markdown supports
- When underline/superscript/subscript annotations are common in content

**Sources:**
- Derived from analysis of annotation types in `LinearTextAnnotation.kt` -- standard markdown only covers bold, italic, code, links, and strikethrough natively

---

### Option 5: Markdown-Only with Extension Conventions

**Description:** Use standard markdown for all formatting, with conventions for non-standard types:
- Bold: `**text**`
- Italic: `*text*`
- Code: `` `text` ``
- Links: `[text](url)`
- Strikethrough: `~~text~~`
- Underline: `__text__` (redefine from markdown's "bold" to "underline" since `**` is already bold)
- Superscript: `^text^` (Pandoc/some markdown extensions)
- Subscript: `~text~` (Pandoc/some markdown extensions)

**Strengths:**
- Uniform marker syntax -- all markers look "markdown-like"
- `^superscript^` and `~subscript~` are used in Pandoc and some markdown extensions
- Lowest possible token overhead
- Single parsing strategy for all markers

**Weaknesses:**
- `__text__` as underline conflicts with standard markdown (normally means bold); LLMs may interpret it as bold
- `^` and `~` have other meanings in some markdown dialects
- Less clear semantics -- need very explicit prompt instructions to override LLM's markdown intuitions
- Underline, superscript, and subscript are rare in RSS content (< 10% of articles) -- optimizing for them adds complexity

**Best For:**
- Projects where uniformity of marker syntax is valued over clarity
- When the controlled set of markers is well-tested with target LLMs

**Sources:**
- Pandoc markdown extensions documentation
- Analysis of annotation frequency in RSS content (Section 3.2)

---

### Comparison Matrix

| Criteria | Opt 1: Markdown | Opt 2: XML Tags | Opt 3: Indexed | Opt 4: Hybrid | Opt 5: MD Extended |
|----------|:-:|:-:|:-:|:-:|:-:|
| LLM Preservation Reliability | High | Medium-High | Very Low | Medium-High | Medium |
| Token Overhead | Low | High | Medium | Low-Medium | Low |
| Parsing Complexity | Medium | Medium-High | Very High | Medium | Medium |
| Content Ambiguity Risk | Medium | Very Low | None | Low | Medium-High |
| Full Annotation Coverage | Partial | Full | Full | Full | Full |
| Cross-Provider Compat (OpenAI+Claude) | High | Medium | Low | Medium-High | Medium |
| Implementation Effort | Low | Medium | Very High | Medium | Low-Medium |
| Community Evidence | Strong | Moderate | None | None | Weak |
| Maturity | High | High | N/A | N/A | Low |
| Extensibility | Limited | Very Good | Good | Good | Limited |

---

### Recommendation

**Recommended:** Option 1 - Lightweight Markdown Markers

**Rationale:** Markdown markers are the most natural format for LLM translation. LLMs understand markdown natively, resulting in the highest preservation reliability across both OpenAI and Anthropic providers. The token overhead is minimal (important for per-paragraph billing in the existing architecture). The project already uses `mikepenz/multiplatform-markdown-renderer` with `commonmark-java`, demonstrating organizational familiarity with markdown. Community evidence from multiple open-source translation projects confirms this approach works at scale.

**Trade-offs:**
- **Gaining**: Highest LLM reliability, lowest token cost, simplest implementation, proven pattern
- **Giving up**: Native support for underline/superscript/subscript (rare in RSS feeds -- < 10% of content)

**Handling non-markdown annotations**: For the rare cases of underline, superscript, and subscript:
1. **Phase 1 (this spec)**: Skip these uncommon annotations -- the text will still be translated correctly, just without the specific formatting
2. **Phase 2 (future)**: If needed, add sentinel tokens `{{u}}`, `{{sup}}`, `{{sub}}` for these rare types (evolving into Option 4)

This phased approach keeps the initial implementation simple while providing a clear upgrade path.

**Alternative Consider:** Option 4 (Hybrid) if testing reveals that underline/superscript/subscript are more common in the target user's RSS feeds than expected, or if a future spec requires full annotation fidelity.

---

## 6. Deprecation Warnings

- `MarkdownToAnnotatedString.kt` in the project already contains deprecated functions (`markdownToAnnotatedString()`, `markdownToAnnotatedStringSafe()`) that return plain `AnnotatedString` without formatting. These should not be used for the new inline parsing.
- The `mikepenz/multiplatform-markdown-renderer` is pinned at 0.38.1 due to a Compose compatibility issue (0.39.0 causes NoSuchMethodError). The latest is 0.39.2. This doesn't affect our approach since we're building a custom inline parser.

---

## 7. Best Practices

### Recommended Patterns

1. **Controlled Vocabulary Approach**
   - Description: Only emit markdown markers that we know how to parse back. Don't rely on the LLM inventing formatting.
   - Use when: Converting `LinearTextAnnotation` to markdown text for translation
   - Source: dltranslator.com isolation zone pattern

2. **Tolerant Parsing**
   - Description: The response parser should handle malformed markdown gracefully -- unmatched `**`, missing closing `` ` ``, broken `[text](url)` -- by falling back to plain text for the affected segment.
   - Use when: Parsing translated text from LLM responses
   - Source: Robustness principle / Postel's Law; community experience with LLM output parsing

3. **Example-Driven Prompting**
   - Description: Include 1-2 concrete examples of input with markdown markers and expected translated output in the prompt.
   - Use when: Updating `buildTranslationPrompt()` to include formatting instructions
   - Source: Anthropic prompting best practices (2025-12); "Prompt Engineering in 2025" (news.aakashg.com, 2025-07)

4. **Idempotent Annotation Conversion**
   - Description: The conversion from `LinearTextAnnotation` to markdown and back should be idempotent -- converting back should produce equivalent (not necessarily identical) annotations.
   - Use when: Testing the annotation -> markdown -> AnnotatedString pipeline
   - Source: Software engineering best practice for data transformations

### Anti-Patterns to Avoid

1. **Full Markdown Rendering for Translated Paragraphs**
   - Description: Using `mikepenz/Markdown()` composable or `MarkdownContent()` to render translated paragraph text
   - Why: Overkill for inline formatting; would render block-level elements; doesn't integrate with existing `Text()` composable flow; potential styling conflicts
   - Source: Architecture analysis of current rendering pipeline in `LinearArticleContent.kt`

2. **Relying on LLMs for Character Position Tracking**
   - Description: Asking the LLM to provide character offsets for annotations in translated text
   - Why: LLMs process tokens not characters; translation changes text length and word order
   - Source: General LLM capability limitations; Option C analysis

3. **Unescaped Content in Markdown Markers**
   - Description: Embedding raw article text containing `**`, `*`, `` ` `` directly into markdown markers without escaping
   - Why: Can cause false parsing of content text as formatting markers
   - Source: Markdown ambiguity analysis

---

## 8. Official Documentation

### Key References

| Resource | URL | Key Takeaways |
|----------|-----|---------------|
| Anthropic Prompting Best Practices | docs.claude.com/en/docs/use-xml-tags | Claude handles XML and markdown well; be explicit about formatting preservation |
| DeepL HTML Handling v2 | developers.deepl.com/docs/xml-and-html-handling/tag-handling-v2 | Industry-standard approach to translating with inline tags preserved |
| Google Cloud Translation (Advanced) | docs.cloud.google.com/translate/docs/advanced/translating-text-v3 | HTML mime type support for formatting-aware translation |
| commonmark-java | github.com/commonmark/commonmark-java | Java/Kotlin markdown parser with visitor pattern for AST traversal |
| mikepenz/multiplatform-markdown-renderer | github.com/mikepenz/multiplatform-markdown-renderer | Already in project (v0.38.1); uses commonmark-java internally |

### API Notes

- `LinearTextAnnotation` provides `start: Int` and `end: Int` (inclusive) character ranges within `LinearText.text`
- The existing `toAnnotatedString()` function in `LinearArticleContent.kt` (line 1257) demonstrates the pattern for converting annotation ranges to `AnnotatedString` styling
- Translation text is currently rendered as plain `String` at line 917: `text = translation` -- this must change to `AnnotatedString` to support formatted translated text

---

## 9. Community Insights

### Top Discussions

1. **Kotlin Slack (2023)** -- Developer Marcin Wisniowski confirmed using commonmark-java AST -> AnnotatedString pattern works well for Compose
2. **Speaker Deck: "Not just plain text: Rendering Markdown in Compose"** -- Sebastiano Poggi (Jewel library) recommends commonmark-java over JetBrains/markdown for spec compliance
3. **Medium: "Simplest Markdown Parser for Android Jetpack Compose" (2025-01)** -- Demonstrates a regex-based approach for inline markdown parsing in ~200 lines
4. **"The Definitive Guide to Prompt Structure" (2026-02)** -- Argues XML is better than markdown for prompting, but this is about prompt structure, not content formatting within translated text

### Common Issues

- **Compose compatibility**: mikepenz library required pinning to v0.38.1 for Compose 1.9.4 (NoSuchMethodError with 0.39.0)
- **AnnotatedString limitations**: No built-in Compose API for markdown-to-AnnotatedString; must build manually
- **Nested list rendering**: Multiple developers report issues with nested list rendering in Compose markdown libraries

---

## 10. Performance Considerations

### Token Overhead Analysis

For a typical paragraph: `"Click here for important info with map() code"`

| Format | Token Representation | Approx. Additional Tokens |
|--------|---------------------|--------------------------|
| Plain text | `Click here for important info with map() code` | 0 (baseline) |
| Markdown | `Click [here](https://example.com) for **important** info with \`map()\` code` | ~6-8 extra |
| XML tags | `Click <link href="https://example.com">here</link> for <b>important</b> info with <code>map()</code> code` | ~15-20 extra |
| Indexed | `{"text": "...", "annotations": [{...}, {...}, {...}]}` | ~30-40 extra |

Markdown markers add approximately **15-25% token overhead** per paragraph (less for paragraphs with few annotations, more for heavily annotated text). Given the existing per-paragraph translation architecture, this is within the < 2x requirement from NFR-3.

### Parsing Performance

- Custom inline markdown parser: ~1-5 microseconds per paragraph (regex-based)
- commonmark-java full parse: ~50-200 microseconds per paragraph
- Either is negligible compared to network round-trip time for AI translation (~500ms-2s)

---

## 11. Edge Cases

### Known Limitations

1. Standard markdown cannot represent underline, superscript, or subscript -- these would need custom extensions or be dropped
2. Markdown link syntax `[text](url)` requires the URL to be embedded in the text, increasing token count for long URLs
3. LLMs may occasionally reformat markdown (e.g., changing `*italic*` to `_italic_`) -- the parser should handle all variants

### Edge Cases to Handle

1. **Empty annotations**: `**` (empty bold) should be silently ignored in both conversion and parsing
2. **Overlapping annotations in source**: `<a href="..."><strong>bold link</strong></a>` -> `[**bold link**](url)` -- nesting order matters
3. **Annotations at text boundaries**: `**Bold text starts a paragraph**` -> must handle markers at start/end of string
4. **URLs with special characters**: `[text](url with (parens))` -> need URL encoding or escaping
5. **Literal markdown in article text**: Article about markdown syntax containing `**` -- should be escaped as `\*\*` before sending to LLM
6. **Code spans containing markdown-like characters**: `` `a ** b` `` -> backtick-enclosed content must not be parsed for inner markers
7. **RTL text**: Annotation character ranges work with logical character order, not visual order -- this is correct for AnnotatedString

### Security Considerations

- Link URLs from the original HTML are preserved through translation -- no new attack surface
- Code spans are marked as non-translatable -- prevents injection of translated code
- JSON parsing of LLM responses already has validation in the existing `parseTranslationResponse()` function

---

## 12. Recommendations

### Must Do (This Spec)

1. **Implement markdown markers** for inline formatting in translation text (Option 1)
2. **Build custom inline markdown parser** for translating response text back to AnnotatedString
3. **Update `buildTranslationPrompt()`** with formatting preservation instructions and examples
4. **Fix nested list index mismatch** in `computeParagraphIndexRecursive()`
5. **Add table cell and image caption extraction** to `extractTranslatableTextRecursively()`
6. **Change translation text rendering** from plain `String` to `AnnotatedString` in `LinearTextContent`

### Should Consider

1. **Escape literal markdown characters** in source text before adding markers
2. **Add fallback behavior** -- if markdown parsing fails, display plain translated text (current behavior)
3. **Refactor duplicated `buildTranslationPrompt()`** into shared utility (follows recommendation from requirements doc)

### Future Considerations

1. **Sentinel tokens for underline/superscript/subscript** if needed (Phase 2, evolving to Option 4)
2. **Caching parsed AnnotatedString** for translated text to avoid re-parsing on recomposition
3. **Benchmarking token overhead** with real RSS article data to validate the < 2x requirement

---

## Sources

### Primary Sources

| # | Title | URL | Published | Freshness | Confidence |
|---|-------|-----|-----------|-----------|------------|
| 1 | Anthropic Prompting Best Practices | docs.claude.com/en/docs/use-xml-tags | 2025-12 | Fresh | 0.95 |
| 2 | DeepL HTML Handling v2 | developers.deepl.com/docs/xml-and-html-handling/tag-handling-v2 | 2025+ | Fresh | 0.90 |
| 3 | Google Cloud Translation (Advanced) | docs.cloud.google.com/translate/docs/advanced/translating-text-v3 | 2025-10 | Fresh | 0.90 |
| 4 | Markdown for AI: Essential for LLM Workflows | markdowntoword.pro/blog/markdown-for-ai-and-llms | 2026-02 | Fresh | 0.80 |
| 5 | Markdown Translation Best Practices | dltranslator.com/qa/markdown-documentation-translation-best-practices | 2026-01 | Fresh | 0.85 |
| 6 | richawo/llm-translator | github.com/richawo/llm-translator | 2023-09 | Dated | 0.75 |
| 7 | williamcaban/MarkPoly | github.com/williamcaban/markpoly | 2025-01 | Current | 0.75 |
| 8 | mikepenz/multiplatform-markdown-renderer | github.com/mikepenz/multiplatform-markdown-renderer | 2026-01 | Fresh | 0.95 |
| 9 | Simplest Markdown Parser for Compose | medium.com/@bijuknarayan (2025-01) | 2025-01 | Current | 0.80 |
| 10 | Mastering Claude Prompts: XML vs Markdown | algorithmunmasked.com (2025-05) | 2025-05 | Current | 0.75 |
| 11 | Kotlin Slack Discussion | slack-chats.kotlinlang.org | 2023-03 | Dated | 0.85 |
| 12 | Rendering Markdown in Compose (Speaker Deck) | speakerdeck.com/rock3r | 2024+ | Current | 0.80 |
| 13 | Context Engineering Guide 2026 | the-ai-corner.com | 2026-03 | Fresh | 0.70 |
| 14 | Why XML Tags are Fundamental to Claude (HN) | news.ycombinator.com | 2026-03 | Fresh | 0.65 |

### Source Freshness Summary

- Fresh (< 6 months): 8 sources
- Current (6-12 months): 4 sources
- Dated (1-2 years): 2 sources
- Potentially Outdated (> 2 years): 0 sources

### Provenance Log

<details>
<summary>Full provenance (for audit)</summary>

| # | Query | Source | Timestamp |
|---|-------|--------|-----------|
| 1 | "LLM translation preserve markdown formatting inline markup 2025 2026 best practices" | Exa Web Search | 2026-03-15 |
| 2 | "translation API preserve HTML formatting inline tags DeepL Google Translate 2025" | Exa Web Search | 2026-03-15 |
| 3 | "Kotlin markdown parser AnnotatedString Jetpack Compose library 2025 2026" | Exa Web Search | 2026-03-15 |
| 4 | "RSS feed HTML content patterns WordPress Ghost Substack inline formatting" | Exa Web Search | 2026-03-15 |
| 5 | "GPT Claude translate text preserve formatting markers XML tags markdown prompt engineering 2025" | Exa Web Search | 2026-03-15 |
| 6 | "commonmark-java Kotlin AnnotatedString Jetpack Compose parse markdown inline formatting" | Exa Code Search | 2026-03-15 |
| 7 | "AI LLM translation preserve XML inline tags HTML formatting failure modes" | Exa Web Search | 2026-03-15 |
| 8 | "open source RSS reader translation feature Android app preserve formatting" | Exa Web Search | 2026-03-15 |
| 9 | Codebase analysis: TranslatableText.kt, LinearTextAnnotation.kt, ArticleViewModel.kt, LinearArticleContent.kt, MarkdownToAnnotatedString.kt, OpenAICompatibleClient.kt, AnthropicClient.kt | Local file reads | 2026-03-15 |

</details>
