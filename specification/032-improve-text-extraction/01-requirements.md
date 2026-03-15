# Requirements: Improve Text Extraction for Translation

**Date:** 2026-03-15
**Type:** Improvement
**Priority:** High
**Spec:** 032

---

## 1. Problem Statement

The current text extraction pipeline converts HTML article content into a flat list of `TranslatableText` objects, each representing a paragraph of plain text with element type metadata (`ElementType` + `nestingLevel`). This extraction happens in `ArticleViewModel.extractTranslatableParagraphs()` which calls `extractTranslatableTextRecursively()` to walk the `LinearElement` tree.

### What Works Today

1. **Block-level text extraction**: Top-level `<p>`, `<h1>`-`<h6>`, and text inside `<li>` and `<blockquote>` elements are extracted as separate `TranslatableText` units
2. **Element type detection**: Headings (H1-H6) are detected from `LinearTextAnnotation` data; list items and blockquotes are identified by nesting context
3. **Nesting level tracking**: `nestingLevel` increments for content inside `LinearListItem` and `LinearBlockQuote` containers
4. **Per-paragraph parallel translation**: Each `TranslatableText` is translated independently via `ParagraphTranslationCoordinator` (spec-31)
5. **Translation index mapping**: `computeParagraphIndices()` in `LinearArticleContent.kt` mirrors the extraction logic to map translations back to UI elements

### What Doesn't Work

#### Problem 1: Inline Formatting is Stripped

When `extractTranslatableTextRecursively()` extracts text from a `LinearText` element, it takes `element.text` -- the **plain text** string with all inline formatting removed. The `LinearText` model stores formatting as `LinearTextAnnotation` ranges (bold, italic, links, code spans, etc.), but these annotations are **not preserved** in the `TranslatableText` output sent to the AI.

**Example**: A paragraph `<p>Click <a href="...">here</a> for <strong>important</strong> info</p>` becomes the flat string `"Click here for important info"`. The AI translates the plain text, but:
- Links are lost in the translated output
- Bold/italic emphasis is lost
- Code spans (`<code>`) within paragraphs are lost
- When rendered, the translation is plain text while the original has rich formatting

**Impact**: Translated text loses all inline formatting, creating a visually inconsistent experience between original and translated content.

#### Problem 2: Inline Images are Skipped

When a `<p>` tag contains an inline `<img>`, the `HtmlLinearizer` calls `finalizeAndAddCurrentElement()` to flush the current text, then adds a `LinearImage` element, then continues collecting text. This splits the paragraph into multiple `LinearText` and `LinearImage` elements at the top level.

However, `extractTranslatableTextRecursively()` only processes `LinearText`, `LinearListItem`, and `LinearBlockQuote` -- it skips `LinearImage` and all other element types. The text segments around inline images become separate translation units with no indication they were originally part of the same paragraph.

**Impact**: Sentence fragments around inline images are translated independently, potentially producing incoherent translations.

#### Problem 3: Table Content is Not Translated

`LinearTable` contains `LinearTableCellItem` elements, each with a `content: List<LinearElement>` that can include `LinearText`. However, `extractTranslatableTextRecursively()` skips `LinearTable` entirely (the `else -> {}` branch).

**Impact**: Table text content is never extracted for translation.

#### Problem 4: Deeply Nested List Translation Index Mismatch

The `computeParagraphIndexRecursive()` function in `LinearArticleContent.kt` has a bug for deeply nested lists (nesting > 1 level). For nested `LinearListItem` elements, it only counts direct `LinearText` children:

```kotlin
is LinearListItem -> {
    val hasTranslatableText = nested.content
        .filterIsInstance<LinearText>()  // Only checks direct children
        .filter { ... }
        .any { ... }
    if (hasTranslatableText) {
        paragraphCounter.increment()  // Increments by 1 regardless of actual count
    }
}
```

But `extractTranslatableTextRecursively()` recurses into all nested levels and counts **every** `LinearText` individually. This causes a mismatch between extraction count and rendering index count for lists nested more than 2 levels deep.

**Impact**: Translation text gets displayed on the wrong paragraph for articles with deeply nested lists.

#### Problem 5: Image Captions are Not Translated

`LinearImage` has an optional `caption: LinearText?` field. Image captions contain meaningful text (from `<figcaption>` or `<img alt="...">`) that should be translatable. Currently these are completely ignored by the extraction pipeline.

**Impact**: Image captions remain in the original language while surrounding text is translated.

---

## 2. Current Architecture

### Data Flow

```
HTML Content (from RSS feed)
    |
    v
HtmlLinearizer.linearize()        -- Jsoup parses HTML, produces LinearArticle
    |
    v
LinearArticle(elements: List<LinearElement>)
    |
    v
ArticleViewModel.extractTranslatableParagraphs()  -- Walks LinearElement tree
    |
    v
List<TranslatableText>            -- Flat list: { text, elementType, nestingLevel }
    |
    v
ParagraphTranslationCoordinator   -- Parallel per-paragraph AI translation
    |
    v
List<String>                      -- Flat list of translated plain text strings
    |
    v
LinearArticleContent.computeParagraphIndices()  -- Maps translations to UI positions
    |
    v
UI renders original + translation side by side
```

### Key Models

| Model | Location | Role |
|-------|----------|------|
| `LinearElement` (sealed interface) | `LinearStuff.kt` | Tree nodes: `LinearText`, `LinearListItem`, `LinearBlockQuote`, `LinearImage`, `LinearTable`, `LinearVideo`, `LinearAudio` |
| `LinearText` | `LinearStuff.kt:316` | Text with `annotations: List<LinearTextAnnotation>` and `blockStyle` |
| `LinearTextAnnotation` | `LinearTextAnnotation.kt` | Inline formatting: Bold, Italic, Link(href), Code, H1-H6, Underline, Strikethrough, Superscript, Subscript, Font, Monospace |
| `TranslatableText` | `TranslatableText.kt` | Translation unit: plain `text` + `elementType` + `nestingLevel` |
| `ElementType` | `TranslatableText.kt:64` | Enum: PARAGRAPH, HEADING_1-6, LIST_ITEM, BLOCKQUOTE |
| `ParagraphTranslation` | `ParagraphTranslation.kt` | Per-paragraph state: `index`, `text`, `translation`, `translated` |
| `ArticleTranslation` | `ArticleTranslation.kt` | Collection of `ParagraphTranslation` with completion tracking |

### Translation JSON Schema (Current)

**Input to AI** (built in `buildTranslationPrompt()`):
```json
{
  "targetLanguage": "Chinese",
  "paragraphs": [
    {"index": 1, "type": "heading level 2", "text": "Introduction"},
    {"index": 2, "type": "paragraph", "text": "Click here for important info"},
    {"index": 3, "type": "list item (nesting level: 1)", "text": "First item"}
  ]
}
```

**Output from AI**:
```json
{
  "targetLanguage": "Chinese",
  "translations": [
    {"index": 1, "translation": "..."},
    {"index": 2, "translation": "..."},
    {"index": 3, "translation": "..."}
  ]
}
```

**Limitation**: The schema is one level deep -- each paragraph is a flat string with no inline structure. The AI cannot know that "here" is a link or "important" is bold, so it translates plain text and returns plain text.

---

## 3. RSS/Atom Content Format Considerations

### RSS 2.0

- `<description>`: Entity-encoded HTML allowed; typically contains summaries
- `<content:encoded>` (Content Module): CDATA-escaped full HTML content; no restriction on which HTML elements are allowed -- publishers include any valid HTML

### Atom (RFC 4287)

- `<content type="text">`: Plain text only
- `<content type="html">`: Entity-escaped HTML (like RSS `<description>`)
- `<content type="xhtml">`: Inline XHTML wrapped in a `<div>` element

### Common HTML Elements in RSS Feed Content

**Frequently encountered** (from major CMS platforms: WordPress, Ghost, Medium, Substack, Hugo, Jekyll):

| Category | Elements | Frequency |
|----------|----------|-----------|
| Block text | `<p>`, `<h1>`-`<h6>`, `<blockquote>` | Very common |
| Lists | `<ul>`, `<ol>`, `<li>` (1-2 levels nesting) | Common |
| Inline formatting | `<strong>`/`<b>`, `<em>`/`<i>`, `<a>`, `<code>` | Very common |
| Images | `<img>`, `<figure>`, `<figcaption>` | Very common |
| Code | `<pre>`, `<code>`, `<pre><code>` | Common (tech blogs) |
| Tables | `<table>`, `<tr>`, `<td>`, `<th>` | Occasional |
| Media | `<video>`, `<audio>`, `<iframe>` | Occasional |
| Other inline | `<sup>`, `<sub>`, `<s>`, `<u>`, `<span>` | Occasional |

**Nesting patterns observed**:
- Lists: Typically 1-2 levels deep; 3+ levels are rare
- Blockquotes: Usually 1 level; nested blockquotes are rare
- Mixed content in `<p>`: Very common -- text + links + emphasis + code spans in a single paragraph
- Mixed content in `<li>`: Common -- text + links, text + code, sometimes `<p>` inside `<li>`

---

## 4. Functional Requirements

### FR-1: Preserve Inline Formatting in Translation Units

The extraction pipeline SHALL include inline formatting metadata in `TranslatableText` so the AI can produce structure-aware translations and the UI can render formatted translated text.

Inline formatting types to preserve:
- **Links** (`<a href="...">`): href URL + text range
- **Bold** (`<strong>`, `<b>`): text range
- **Italic** (`<em>`, `<i>`): text range
- **Inline code** (`<code>`): text range (should NOT be translated)
- **Strikethrough** (`<s>`): text range
- **Underline** (`<u>`): text range
- **Superscript/Subscript** (`<sup>`, `<sub>`): text range

The translation prompt SHALL instruct the AI to preserve inline formatting markers in the translated output so formatting can be reconstructed.

### FR-2: Handle Inline Images Within Paragraphs

When a `<p>` tag contains inline `<img>` elements mixed with text:
- The extraction SHALL treat the surrounding text segments as a single translation unit (the full paragraph context)
- Inline image references SHALL be preserved as non-translatable markers within the text
- The translation output SHALL maintain the image position relative to translated text

### FR-3: Translate Table Cell Content

The extraction pipeline SHALL recurse into `LinearTable` > `LinearTableCellItem` > `content` to extract translatable text from table cells.

Each cell's text content SHALL be treated as a separate translation unit with a new element type (`TABLE_CELL` or similar).

### FR-4: Fix Deeply Nested List Translation Index Mismatch

The `computeParagraphIndexRecursive()` function SHALL properly count all translatable text elements at any nesting depth, matching the behavior of `extractTranslatableTextRecursively()` exactly.

This requires the rendering-side counter to recurse into nested `LinearListItem` elements the same way the extraction side does, rather than using a flat `filterIsInstance<LinearText>()` check.

### FR-5: Translate Image Captions

The extraction pipeline SHALL extract text from `LinearImage.caption` (when non-null and non-blank) as a translation unit with a new element type (`IMAGE_CAPTION` or similar).

### FR-6: Support Non-Translatable Inline Segments

Certain inline content SHALL be marked as non-translatable within a translation unit:
- Inline code spans (`<code>...</code>`)
- URLs within link text (the href, not the visible text)
- Variable names and technical identifiers

The translation prompt SHALL instruct the AI to preserve these segments verbatim.

---

## 5. Non-Functional Requirements

### NFR-1: Backwards Compatibility

- The `AIClient.translate()` interface SHALL NOT change
- The `ParagraphTranslationCoordinator` SHALL continue to work with the updated `TranslatableText`
- Existing translation prompt format SHALL be extended, not replaced
- If inline formatting data is absent (e.g., from older cached content), the system SHALL fall back to plain text translation

### NFR-2: Translation Quality

- Inline formatting markers SHALL be designed to be easily understood by LLMs (e.g., lightweight markup like `**bold**` or XML-like tags `<b>bold</b>`)
- The marker format SHALL have low token overhead to minimize API costs
- Markers SHALL be unambiguous and reliably parseable from AI responses

### NFR-3: Performance

- Adding inline formatting metadata SHALL NOT significantly increase the size of `TranslatableText` objects (< 2x increase in text length from markers)
- Table cell extraction SHALL NOT cause excessive translation units (consider batching small cells)
- The `computeParagraphIndices()` mapping SHALL remain O(n) in the number of elements

### NFR-4: Rendering Fidelity

- Translated text with inline formatting SHALL be rendered with the same visual styles as the original (bold, italic, links, etc.)
- Link hrefs SHALL be preserved in translated text so links remain functional
- Code spans in translated text SHALL remain untranslated and monospace-styled

---

## 6. Acceptance Criteria

### AC-1: Inline Formatting Preservation
- GIVEN a paragraph `<p>Click <a href="https://example.com">here</a> for <strong>important</strong> info</p>`
- WHEN the article is translated
- THEN the translated paragraph preserves clickable link and bold formatting in the UI

### AC-2: Link Functionality in Translations
- GIVEN a paragraph with `<a href="https://example.com">link text</a>`
- WHEN the translated paragraph is rendered
- THEN the translated link text is clickable and navigates to `https://example.com`

### AC-3: Code Span Preservation
- GIVEN a paragraph `<p>Use the <code>map()</code> function to transform data</p>`
- WHEN translated
- THEN `map()` remains untranslated and rendered in monospace in the translation

### AC-4: Table Content Translation
- GIVEN an article with a `<table>` containing text in cells
- WHEN translated
- THEN table cell text appears translated in the rendered table

### AC-5: Nested List Index Correctness
- GIVEN an article with a 3-level nested list (e.g., `<ul><li>A<ul><li>B<ul><li>C</li></ul></li></ul></li></ul>`)
- WHEN translated
- THEN each list item text (A, B, C) shows its correct translation, not a mismatched one

### AC-6: Image Caption Translation
- GIVEN an article with `<figure><img ...><figcaption>Photo by John</figcaption></figure>`
- WHEN translated
- THEN the caption text "Photo by John" is translated and displayed below the image

### AC-7: Mixed Inline Content in Paragraphs
- GIVEN a paragraph with text, links, bold, and italic mixed together
- WHEN translated
- THEN all formatting types are preserved in the translated output

### AC-8: Inline Images in Paragraphs
- GIVEN a paragraph like `<p>Chart showing growth <img src="chart.png"> indicates positive trends</p>`
- WHEN translated
- THEN the full sentence context is translated coherently (not as two independent fragments)

### AC-9: Backwards Compatibility
- GIVEN an article translated with the updated system
- WHEN the system encounters a `TranslatableText` without inline formatting data
- THEN translation falls back to plain text mode (current behavior)

---

## 7. Edge Cases and Constraints

### Edge Cases

1. **Empty inline elements**: `<strong></strong>` or `<a href="..."></a>` -- should be ignored
2. **Nested inline formatting**: `<strong><em>bold italic</em></strong>` -- should preserve both
3. **Very long paragraphs with many inline elements**: Performance concern for marker overhead
4. **Inline code containing special characters**: `<code>a < b && c > d</code>` -- markers must not conflict
5. **Links with no href**: `<a>text</a>` -- treat as plain text
6. **Tables with single column or single row**: Already optimized out by `HtmlLinearizer` (treated as regular content)
7. **Tables with merged cells** (rowspan/colspan): Cell content should still be extracted
8. **List items containing only images**: No text to translate -- skip gracefully
9. **Paragraphs that are entirely a link**: `<p><a href="...">Click here</a></p>` -- the entire paragraph text is linked
10. **RTL text with inline formatting**: Formatting ranges must work correctly with RTL content

### Constraints

1. **AI token limits**: Inline formatting markers increase token count per paragraph. Must keep overhead reasonable.
2. **AI response parsing**: The AI must return formatted text in a parseable format. Need robust parsing with fallback.
3. **Prompt compatibility**: Both OpenAI-compatible and Anthropic providers must handle the formatting instructions equally well.
4. **Existing UI rendering**: `LinearTextContent` already renders `LinearTextAnnotation` for original text. Translation text rendering must produce equivalent `AnnotatedString` output.
5. **Translation prompt is duplicated**: `buildTranslationPrompt()` exists in both `OpenAICompatibleClient.kt` and `AnthropicClient.kt` with identical logic -- any prompt changes must be synchronized.

---

## 8. Proposed Solution Options

### Option A: Lightweight Markdown Markers (Recommended)

Embed lightweight markdown-like markers in the text sent to the AI:

```json
{
  "index": 1,
  "type": "paragraph",
  "text": "Click [here](https://example.com) for **important** info with `map()` code"
}
```

The AI returns translated text with the same markers preserved:
```json
{
  "index": 1,
  "translation": "点击[这里](https://example.com)获取**重要**信息，使用`map()`代码"
}
```

Parse the markers from the response to reconstruct `AnnotatedString` for rendering.

- **Pros**: LLMs already understand markdown well; minimal token overhead; natural for AI to preserve; easy to instruct
- **Cons**: Ambiguity if original text contains literal markdown characters; need escaping strategy

### Option B: XML-like Inline Tags

Use explicit XML-like tags:
```json
{
  "text": "Click <link href=\"https://example.com\">here</link> for <b>important</b> info with <code>map()</code>"
}
```

- **Pros**: Unambiguous; supports attributes (href); clear boundaries
- **Cons**: Higher token overhead; some LLMs may strip or modify XML tags; more complex parsing

### Option C: Indexed Annotation References

Send text with positional references to a separate annotation array:
```json
{
  "text": "Click here for important info with map() code",
  "annotations": [
    {"start": 6, "end": 10, "type": "link", "href": "https://example.com"},
    {"start": 15, "end": 24, "type": "bold"},
    {"start": 35, "end": 40, "type": "code"}
  ]
}
```

The AI translates only the text and provides new annotation positions in the translated output.

- **Pros**: Clean separation; text remains plain
- **Cons**: Very difficult for LLMs to track character positions in translated text; high error rate; fragile

---

## 9. Dependencies on Existing Code

| Component | File | Change Needed |
|-----------|------|---------------|
| `TranslatableText` | `TranslatableText.kt` | **Major**: Add inline formatting metadata |
| `ElementType` | `TranslatableText.kt:64` | **Minor**: Add TABLE_CELL, IMAGE_CAPTION |
| `extractTranslatableTextRecursively()` | `ArticleViewModel.kt:644` | **Major**: Extract inline annotations; handle tables, captions |
| `buildTranslationPrompt()` | `OpenAICompatibleClient.kt:520` | **Major**: Include formatting markers in prompt; add formatting instructions |
| `buildTranslationPrompt()` | `AnthropicClient.kt:447` | **Major**: Same changes (keep in sync) |
| `parseTranslationResponse()` | `OpenAICompatibleClient.kt:612` | **Moderate**: Parse formatted text from response |
| `parseTranslationResponse()` | `AnthropicClient.kt:539` | **Moderate**: Same changes (keep in sync) |
| `computeParagraphIndexRecursive()` | `LinearArticleContent.kt:174` | **Moderate**: Fix deep nesting count; add table/caption support |
| `LinearTextContent` (translation rendering) | `LinearArticleContent.kt` | **Moderate**: Render translated text with formatting (AnnotatedString) |
| `ParagraphTranslationCoordinator` | `ParagraphTranslationCoordinator.kt` | **None**: No changes needed |
| `ArticleTranslation` / `ParagraphTranslation` | `ArticleTranslation.kt` / `ParagraphTranslation.kt` | **None**: No structural changes needed |

---

## 10. Out of Scope

1. **Translation of pre-formatted code blocks** (`<pre>`, `<pre><code>`) -- these are already correctly skipped
2. **Translation of audio/video metadata** -- these elements contain no translatable text
3. **Translation of HTML attributes** (e.g., `alt` text on non-caption images, `title` attributes)
4. **Restructuring the LinearElement model** -- the existing `HtmlLinearizer` output is the input; we only change how we extract from it
5. **Changing the per-paragraph translation approach** -- spec-31's `ParagraphTranslationCoordinator` is preserved
6. **Caching or persisting translation results** -- out of scope for this spec
7. **Translating feed metadata** (feed title, feed description) -- only article body content

---

## 11. Assumptions

1. **LLMs can reliably preserve markdown-like markers** during translation -- validated by common practice in AI translation tools
2. **The existing `HtmlLinearizer` correctly parses all HTML elements** we encounter in RSS feeds -- this spec does not change HTML parsing
3. **Table content worth translating is text-based** -- tables with only numbers/images don't need translation
4. **Image captions are typically short** (1-2 sentences) -- they fit well as individual translation units
5. **Most RSS feeds use standard HTML** -- edge cases like custom web components or SVG text are not expected

---

## 12. Open Questions

- [ ] Should the inline formatting marker format be configurable, or fixed to one approach (e.g., markdown)?
- [ ] How should conflicting markers be handled (e.g., AI returns `**bold**` but the original had `<strong>` with different boundaries)?
- [ ] Should we batch small table cells into a single translation unit to reduce API calls?
- [ ] For inline images that split a paragraph, should we send the full original paragraph text (reconstructed) or keep the segments separate?
- [ ] Should the prompt changes be refactored into a shared utility to eliminate the duplication between OpenAI and Anthropic clients?

---

## 13. Recommendations

Based on the analysis:

1. **Immediate (this spec)**: Implement Option A (markdown markers) for inline formatting preservation. Fix the nested list index mismatch bug. Add table cell and image caption extraction.

2. **Next (follow-up)**: Refactor the duplicated `buildTranslationPrompt()` into a shared `TranslationPromptBuilder` utility used by both AI provider clients.

3. **Future (roadmap)**: Consider a richer annotation format if markdown markers prove insufficient for complex inline structures. Consider caching translated annotations for re-rendering efficiency.
