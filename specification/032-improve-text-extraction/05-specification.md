# Technical Specification: Improve Text Extraction for Translation

**Date:** 2026-03-15
**Author:** Claude (Spec Writer)
**Status:** Draft
**Spec:** 032

---

## 1. Overview

### 1.1 Summary

This specification addresses five problems in the text extraction and translation pipeline of the Feeder RSS reader app. The current pipeline extracts plain text from HTML content for AI translation, stripping all inline formatting (bold, italic, links, code spans, etc.). This spec introduces XML-like inline tags to preserve formatting through translation, adds table cell and image caption extraction, fixes a deeply nested list index mismatch bug, and eliminates ~220 LOC of duplicated prompt/parsing code between the two AI client implementations.

### 1.2 Goals

1. **Preserve inline formatting** in translated text using XML-like inline tags for all 10 annotation types (bold, italic, code, link, strikethrough, underline, superscript, subscript, monospace, font)
2. **Translate table cell content** by extracting text from `LinearTable` > `LinearTableCellItem` > `content`
3. **Translate image captions** by extracting text from `LinearImage.caption`
4. **Fix deeply nested list index mismatch** in `computeParagraphIndexRecursive()` and related functions
5. **Eliminate duplicated prompt/parsing code** by extracting a shared `TranslationPromptBuilder` utility

### 1.3 Non-Goals

- Translation of pre-formatted code blocks (`<pre>`, `<pre><code>`) -- already correctly skipped
- Translation of audio/video metadata
- Translation of HTML attributes (alt text on non-caption images, title attributes)
- Restructuring the `LinearElement` model or changing `HtmlLinearizer`
- Changing the per-paragraph translation approach (spec-31's `ParagraphTranslationCoordinator` is preserved)
- Caching or persisting translation results
- Batch table cells into fewer translation units (future optimization)
- Merging text fragments split by inline images into single translation units (pragmatic trade-off for simplicity)
- Extracting duplicated summary-related functions (`buildSummaryPrompt()`, etc.) -- only translation functions are scoped

---

## 2. Background

### 2.1 Context

> From Research Report: LLMs handle XML-like inline tags reliably, especially Claude which has explicit XML training. The Feeder project already uses `LinearTextAnnotation` to store inline formatting data, but this data is discarded during extraction. A custom single-pass tag parser (~200 LOC) is the most practical approach for converting XML tags back to `AnnotatedString`.

> From Research Report: XML-like tags are unambiguous (no conflict with content text unlike markdown `*` or `**`), support attributes (`href`, `face`), and provide full coverage of all 10 annotation types. Standard markdown only covers 5 of 10 types.

### 2.2 Current State

> From Code Assessment: Massive code duplication between `OpenAICompatibleClient.kt` and `AnthropicClient.kt` -- `buildTranslationPrompt()`, `parseTranslationResponse()`, `jsonEscape()`, `unescapeJson()`, `extractJsonFromResponse()`, and `handleTranslationError()` are all character-for-character identical (~220 LOC duplicated). The translation rendering in `LinearTextContent` passes translation as a plain `String` directly to `Text()`, while the original text uses rich `AnnotatedString` via `toAnnotatedString()`.

> From Code Assessment: Three element types are skipped entirely in extraction -- `LinearImage` (captions), `LinearTable` (cell content), and inline images within paragraphs are all in the `else -> {}` branch. No test coverage exists for extraction or index mapping functions.

### 2.3 Architecture Decision

> From Architecture Document (ADR-001): Use XML-like Inline Tags for formatting in translation. User-selected, updated from initial markdown markers recommendation. Provides complete coverage of all 10 annotation types, is unambiguous, and Claude handles XML natively.

> From Architecture Document (ADR-002): Extract shared `TranslationPromptBuilder` to eliminate duplication before making feature changes, reducing blast radius from 2 files to 1.

> From Architecture Document (ADR-003): Custom single-pass XML tag parser over library. The controlled vocabulary of 10 known tags does not justify a full XML parser. The custom parser is tolerant of malformed LLM output (degrades to plain text).

---

## 3. Technical Design

### 3.1 Architecture

```
HTML --> HtmlLinearizer --> LinearArticle(elements)
                                    |
                    +---------------+---------------+
                    |                               |
            [Extraction]                     [Rendering]
                    |                               |
       TranslatableTextExtractor       LinearArticleContent.kt
       .extract(elements)              computeParagraphIndices()
       .toTaggedText()                      |
                    |               computeContentTranslationIndices()
                    |                  (SINGLE unified function)
                    v                       |
            List<TranslatableText>          v
             (XML-tagged text)       Map<Int, Int?> position->index
                    |                       |
                    v                       v
       ParagraphTranslation..       LinearTextContent()
                    |                  InlineTagParser.parse()
                    v                  Text(annotatedTranslation)
         OpenAI/Anthropic                   |
         TranslationPromptBuilder     LinearTableContent()
         .buildTranslationPrompt()     (with translation params)
         .parseTranslationResponse()        |
         (SHARED utility)             LinearImageContent()
                    |                  (with caption translation)
                    v
            List<String> (XML-tagged text)
```

### 3.2 Components

#### Component 1: TranslatableTextExtractor (NEW)

- **Purpose:** Extract translatable text from `LinearElement` tree, converting `LinearTextAnnotation` ranges to XML-like inline tags
- **Responsibilities:**
  - Walk `LinearElement` tree recursively (DFS)
  - Convert `LinearTextAnnotation` ranges to XML tags via `toTaggedText()`
  - Handle `LinearText`, `LinearListItem`, `LinearBlockQuote`, `LinearTable`, `LinearImage` elements
  - Detect heading element types from annotations
  - Escape XML special characters in content text
- **Interface:**
  ```kotlin
  object TranslatableTextExtractor {
      fun extract(elements: List<LinearElement>): List<TranslatableText>
      internal fun toTaggedText(text: String, annotations: List<LinearTextAnnotation>): String
  }
  ```
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractor.kt`

#### Component 2: TranslationPromptBuilder (NEW)

- **Purpose:** Shared utility for building translation prompts and parsing responses, eliminating duplication between AI clients
- **Responsibilities:**
  - Build JSON-formatted translation prompts with XML tag formatting instructions
  - Parse JSON translation responses
  - Handle JSON escaping/unescaping
  - Extract JSON from markdown code blocks in AI responses
  - Handle translation errors
- **Interface:**
  ```kotlin
  object TranslationPromptBuilder {
      fun buildTranslationPrompt(
          translatableTexts: List<TranslatableText>,
          targetLanguage: TranslationLanguage,
      ): String
      fun parseTranslationResponse(response: String, expectedParagraphs: Int): List<String>
      fun handleTranslationError(e: Exception): String
      internal fun extractJsonFromResponse(response: String): String
      internal fun jsonEscape(text: String): String
      internal fun unescapeJson(text: String): String
  }
  ```
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilder.kt`

#### Component 3: InlineTagParser (NEW)

- **Purpose:** Parse XML-like inline tags in translated text and produce Compose `AnnotatedString`
- **Responsibilities:**
  - Single-pass state machine scanning for tags
  - Map 10 known tags to `SpanStyle` / `LinkAnnotation`
  - Unescape XML entities (`&amp;`, `&lt;`, `&gt;`, `&quot;`)
  - Handle nesting (e.g., `<link><b>bold link</b></link>`)
  - Fall back to plain text for malformed/unknown tags
- **Interface:**
  ```kotlin
  object InlineTagParser {
      @Composable
      fun parse(
          text: String,
          onLinkClick: (url: String) -> Unit,
      ): AnnotatedString
  }
  ```
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/InlineTagParser.kt`

### 3.3 Data Model Changes

#### ElementType Enum -- Add Two Values

```kotlin
// TranslatableText.kt
@Serializable
enum class ElementType {
    PARAGRAPH,
    HEADING_1, HEADING_2, HEADING_3, HEADING_4, HEADING_5, HEADING_6,
    LIST_ITEM,
    BLOCKQUOTE,
    TABLE_CELL,      // NEW
    IMAGE_CAPTION,   // NEW
}
```

Update `getStructureDescription()`:
```kotlin
ElementType.TABLE_CELL -> "table cell"
ElementType.IMAGE_CAPTION -> "image caption"
```

#### TranslatableText -- No Structural Change

The `text` field now contains XML-tagged text (e.g., `"Click <link href=\"url\">here</link> for <b>important</b> info"`). No new fields are needed because the XML tags ARE the inline formatting data -- embedded in the text string itself. `@Serializable` compatibility is maintained.

### 3.4 XML Tag Format Specification

#### Complete Tag Mapping (All 10 Annotation Types)

| LinearTextAnnotationData | Open Tag | Close Tag | Translation Rule |
|--------------------------|----------|-----------|-----------------|
| `Bold` | `<b>` | `</b>` | Translate text inside, preserve tags |
| `Italic` | `<i>` | `</i>` | Translate text inside, preserve tags |
| `Code` | `<code>` | `</code>` | Do NOT translate text inside, preserve exactly |
| `Link(href)` | `<link href="...">` | `</link>` | Translate link text, keep URL and tags as-is |
| `Strikethrough` | `<s>` | `</s>` | Translate text inside, preserve tags |
| `Underline` | `<u>` | `</u>` | Translate text inside, preserve tags |
| `Superscript` | `<sup>` | `</sup>` | Translate text inside, preserve tags |
| `Subscript` | `<sub>` | `</sub>` | Translate text inside, preserve tags |
| `Monospace` | `<mono>` | `</mono>` | Translate text inside, preserve tags |
| `Font(face)` | `<font face="...">` | `</font>` | Translate text inside, keep face attribute as-is |
| `H1-H6` | -- | -- | Already detected as ElementType, not inline tags |

#### Examples

**Input (LinearText)**:
```
text = "Click here for important info"
annotations = [
    Link(href="https://example.com", start=6, end=9),   // "here"
    Bold(start=15, end=23),                              // "important"
]
```

**Output (tagged text)**:
```
"Click <link href="https://example.com">here</link> for <b>important</b> info"
```

**Nested annotations**:
```
text = "bold link text"
annotations = [
    Link(href="https://example.com", start=0, end=13),
    Bold(start=0, end=13),
]
```
Output: `<link href="https://example.com"><b>bold link text</b></link>`

### 3.5 Escaping Strategy

#### XML Entity Escaping (in `toTaggedText()`)

Content text is XML-entity-escaped **before** tag insertion:

| Character | Entity | Notes |
|-----------|--------|-------|
| `&` | `&amp;` | Must be escaped first |
| `<` | `&lt;` | Prevents content from being parsed as tags |
| `>` | `&gt;` | Completes angle bracket escaping |
| `"` | `&quot;` | Only inside attribute values |

Characters that are part of annotation boundaries are NOT escaped -- they get replaced by XML tags.

#### Entity Unescaping (in `InlineTagParser`)

The parser recognizes and unescapes these entities when processing text content between tags:
- `&amp;` -> `&`
- `&lt;` -> `<`
- `&gt;` -> `>`
- `&quot;` -> `"`

### 3.6 toTaggedText() Algorithm

**Steps:**

1. Escape XML special characters in the source text (`&` -> `&amp;`, `<` -> `&lt;`, `>` -> `&gt;`)
2. Build a list of "events" from annotations: `(position, type, isOpen, data)`
   - For each annotation: one open event at `annotation.start`, one close event at `annotation.endExclusive`
3. Sort events by position. At the same position: open events before close events. Among opens at the same position: wider spans first (so outer wraps inner).
4. Walk through the escaped text character by character, inserting open/close tags at event positions

**Nesting priority:** Link wraps other annotations: `<link href="url"><b>bold link text</b></link>` not `<b><link href="url">bold link text</link></b>`. This matches HTML nesting convention.

**Heading annotation filtering:** H1-H6 annotations are used to determine `ElementType` and are NOT converted to inline tags (they are block-level semantics, not inline formatting).

### 3.7 Extraction Pipeline Changes

#### LinearText (modified behavior)

```
BEFORE: element.text (plain text, trimmed)
AFTER:  toTaggedText(element.text, nonHeadingAnnotations).trim() (XML-tagged)
```

Only annotations that are NOT heading types (H1-H6) are converted to tags. The heading type is still detected via `getElementTypeFromAnnotations()` to set the `ElementType`.

#### LinearTable (new branch in extraction)

```kotlin
is LinearTable -> {
    for (row in 0 until element.rowCount) {
        for (col in 0 until element.colCount) {
            val cell = element.cellAt(row, col) ?: continue
            if (cell.isFiller) continue  // Skip spanned filler cells
            // Recurse into cell content
            extractRecursively(
                elements = cell.content,
                result = result,
                nestingLevel = nestingLevel,
                defaultElementType = ElementType.TABLE_CELL,
            )
        }
    }
}
```

Table cells are iterated in row-major order, skipping filler cells (from rowSpan/colSpan). Each cell's content is treated as a container of `LinearElement` items. `LinearText` elements within cells use `TABLE_CELL` as element type unless they have heading annotations.

The `extractRecursively()` function gains a `defaultElementType` parameter (default: `ElementType.PARAGRAPH`) used when a `LinearText` has no heading annotations. For table cells, this becomes `ElementType.TABLE_CELL`.

#### LinearImage (new branch -- captions only)

```kotlin
is LinearImage -> {
    element.caption?.let { caption ->
        if (caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
            result.add(TranslatableText(
                text = toTaggedText(caption.text, caption.annotations).trim(),
                elementType = ElementType.IMAGE_CAPTION,
                nestingLevel = nestingLevel,
            ))
        }
    }
}
```

Only `LinearImage` elements with non-null, non-blank captions produce a `TranslatableText`. The image itself is not translatable.

#### Inline Images Within Paragraphs (Problem 2)

The `HtmlLinearizer` splits a `<p>` with inline `<img>` into separate `LinearText` + `LinearImage` + `LinearText` siblings. These text fragments remain as separate translation units (current behavior). This is the pragmatic trade-off: correctness and simplicity over marginal translation quality improvement for a rare case. No merging logic is needed.

### 3.8 Translation Prompt Changes

#### Updated JSON Schema

**Input to AI:**
```json
{
  "targetLanguage": "Chinese",
  "paragraphs": [
    {"index": 1, "type": "heading level 2", "text": "Introduction"},
    {"index": 2, "type": "paragraph", "text": "Click <link href=\"https://example.com\">here</link> for <b>important</b> info with <code>map()</code> code"},
    {"index": 3, "type": "list item (nesting level: 1)", "text": "First item with <i>emphasis</i>"},
    {"index": 4, "type": "table cell", "text": "Cell content"},
    {"index": 5, "type": "image caption", "text": "Photo by <b>John Doe</b>"}
  ]
}
```

**Output from AI:**
```json
{
  "targetLanguage": "Chinese",
  "translations": [
    {"index": 1, "translation": "..."},
    {"index": 2, "translation": "...translated with tags preserved..."},
    {"index": 3, "translation": "..."},
    {"index": 4, "translation": "..."},
    {"index": 5, "translation": "..."}
  ]
}
```

#### Prompt Additions

Add the following section to the prompt after the existing Translation Guidelines:

```
## Inline Formatting Tags

The input text contains XML-like inline formatting tags that MUST be preserved in your translation.

### Tag Reference

| Tag | Meaning | Translation Rule |
|-----|---------|-----------------|
| `<b>text</b>` | Bold | Translate text inside, preserve tags |
| `<i>text</i>` | Italic | Translate text inside, preserve tags |
| `<code>text</code>` | Code | Do NOT translate text inside, preserve exactly as-is |
| `<link href="url">text</link>` | Hyperlink | Translate link text, keep URL and tags exactly as-is |
| `<s>text</s>` | Strikethrough | Translate text inside, preserve tags |
| `<u>text</u>` | Underline | Translate text inside, preserve tags |
| `<sup>text</sup>` | Superscript | Translate text inside, preserve tags |
| `<sub>text</sub>` | Subscript | Translate text inside, preserve tags |
| `<mono>text</mono>` | Monospace | Translate text inside, preserve tags |
| `<font face="x">text</font>` | Font face | Translate text inside, keep face attribute and tags as-is |

### Rules

1. Preserve ALL formatting tags exactly as they appear -- do not add, remove, or modify any tags
2. Text inside `<code>...</code>` is code and MUST NOT be translated
3. URLs inside `<link href="...">` MUST NOT be translated or modified
4. The `face` attribute in `<font face="...">` MUST NOT be modified
5. Keep the tag structure intact even if the translated text changes word order
6. Tags can be nested (e.g., `<link href="url"><b>bold link</b></link>`) -- preserve the nesting

### Example

Input:  "Click <link href="https://example.com">here</link> for <b>important</b> info with <code>map()</code> code"
Output: "Haga clic en <link href="https://example.com">aqui</link> para obtener informacion <b>importante</b> con el codigo <code>map()</code>"
```

Also add to the existing guideline 5 (Technical Terms):
```
5. **Technical Terms**: Keep technical terminology, code, variable names, and commands untranslated.
   Content inside `<code>` tags MUST NOT be translated.
```

And add guideline 10:
```
10. **Table Cells**: Table cell content should be translated naturally, maintaining brevity appropriate for table formatting.

11. **Image Captions**: Image captions should be translated completely, preserving the descriptive tone.
```

#### Response Parsing -- Minimal Change

The existing `parseTranslationResponse()` extracts the `"translation"` string value from each JSON object. Since XML tags are embedded in the text string, **no parsing logic changes are needed**. The returned `List<String>` now contains XML-tagged strings instead of plain text.

### 3.9 InlineTagParser Design

#### Parsing Algorithm (Single-Pass State Machine)

```
State: scanning text character by character
Active styles: stack of currently open tags

1. If char is NOT '<':
   - Check for XML entity references (&amp; &lt; &gt; &quot;) and unescape
   - Append char to output with current style stack applied
2. If char is '<':
   a. If next chars are '/' -> closing tag
      - Read tag name until '>'
      - Pop matching tag from style stack
   b. Otherwise -> opening tag
      - Read tag name and attributes until '>'
      - Push tag + attributes onto style stack
3. At end of string:
   - Close any unclosed tags (tolerance for malformed input)
```

#### Tag Whitelist

```kotlin
private val KNOWN_TAGS = setOf("b", "i", "code", "link", "s", "u", "sup", "sub", "mono", "font")
```

Unknown tags (including any injected `<script>`, `<img>`, etc.) are rendered as literal text including the angle brackets. This prevents the parser from interpreting arbitrary HTML.

#### Tag-to-Style Mapping

| Tag | AnnotatedString Style |
|-----|-----------------------|
| `<b>` | `SpanStyle(fontWeight = FontWeight.Bold)` |
| `<i>` | `SpanStyle(fontStyle = FontStyle.Italic)` |
| `<code>` | `CodeInlineStyle()` (from existing `LinearArticleContent.kt`) |
| `<link href="url">` | `LinkAnnotation.Clickable(tag = url, styles = LinkTextStyle())` |
| `<s>` | `SpanStyle(textDecoration = TextDecoration.LineThrough)` |
| `<u>` | `SpanStyle(textDecoration = TextDecoration.Underline)` |
| `<sup>` | `SpanStyle(baselineShift = BaselineShift.Superscript)` |
| `<sub>` | `SpanStyle(baselineShift = BaselineShift.Subscript)` |
| `<mono>` | `SpanStyle(fontFamily = monoFontFamily)` |
| `<font face="x">` | `SpanStyle(fontFamily = face.asFontFamily())` |

These match exactly the styles used in `LinearText.toAnnotatedString()` (lines 1257-1382 of `LinearArticleContent.kt`).

#### Attribute Parsing

Only two tags have attributes:
- `<link href="...">` -- extract `href` value
- `<font face="...">` -- extract `face` value

Attribute parsing: find `key="value"` pattern within the tag content.

#### Fallback Behavior

| Condition | Behavior |
|-----------|----------|
| Unknown tag name | Rendered as literal text (including angle brackets) |
| Unclosed tags at end of string | Styles applied up to end, tags closed implicitly |
| Mismatched close tags | Ignored (pop nothing from stack) |
| Garbled `<` sequences | Rendered as literal text |
| **Never throws** | Always returns a valid `AnnotatedString` |

The worst case is visually identical to current behavior (plain text translation with some stray tag text).

### 3.10 Rendering Pipeline Changes

#### LinearTextContent -- Formatted Translations

Current (`LinearArticleContent.kt:912-922`):
```kotlin
if (translation != null) {
    Text(text = translation, ...)  // Plain string
}
```

After:
```kotlin
if (translation != null) {
    val annotatedTranslation = InlineTagParser.parse(
        text = translation,
        onLinkClick = { url -> onLinkClick(url, null) },
    )
    Text(text = annotatedTranslation, ...)  // AnnotatedString
}
```

The `translation` parameter type stays `String?` -- conversion to `AnnotatedString` happens inside `LinearTextContent`. This minimizes change surface across all callers.

#### LinearTableContent -- Thread Translation Data

Add parameters:
```kotlin
fun LinearTableContent(
    linearTable: LinearTable,
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
    translatedParagraphs: List<String>? = null,     // NEW
    tableTranslationStartIndex: Int = -1,            // NEW
)
```

Inside the cell rendering loop, compute the translation index for each cell using `computeContentTranslationIndices()` and pass translation text to `LinearElementContent` for each cell element.

#### LinearImageContent -- Pass Caption Translation

Add parameter:
```kotlin
fun LinearImageContent(
    linearImage: LinearImage,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
    captionTranslation: String? = null,              // NEW
)
```

In caption rendering, pass `captionTranslation` to `LinearTextContent`:
```kotlin
linearImage.caption?.let { caption ->
    LinearTextContent(
        linearText = caption,
        translation = captionTranslation,  // NEW
        idToIndex = idToIndex,
        onLinkClick = onLinkClick,
    )
}
```

#### LinearElementContent -- Thread New Parameters

Update the `when` dispatch to pass translation data to `LinearTableContent` and `LinearImageContent`:

```kotlin
is LinearTable ->
    LinearTableContent(
        linearTable = linearElement,
        ...
        translatedParagraphs = translatedParagraphs,
        tableTranslationStartIndex = parentTranslationIndex ?: -1,
    )

is LinearImage ->
    LinearImageContent(
        linearImage = linearElement,
        ...
        captionTranslation = translation,
    )
```

### 3.11 Index Mapping Fix

#### The Core Bug

`computeParagraphIndexRecursive()` uses `filterIsInstance<LinearText>()` on **direct children only** for nested `LinearListItem` and `LinearBlockQuote` elements. But `extractTranslatableTextRecursively()` uses true recursion into all nesting levels. This causes index mismatch for lists nested > 2 levels deep.

The same bug exists in:
1. `computeParagraphIndexRecursive()` -- `LinearArticleContent.kt:195-215`
2. `computeChildTranslationIndices()` -- `LinearArticleContent.kt:658-697`
3. `computeBlockQuoteContentTranslationIndices()` -- `LinearArticleContent.kt:707-746`

#### Fix: Unified Recursive Counter

Introduce a single `countTranslatableTexts()` function that mirrors `TranslatableTextExtractor.extractRecursively()` exactly:

```kotlin
private fun countTranslatableTexts(elements: List<LinearElement>): Int {
    var count = 0
    for (element in elements) {
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT && element.text.isNotBlank()) {
                    count++
                }
            }
            is LinearListItem -> count += countTranslatableTexts(element.content)
            is LinearBlockQuote -> count += countTranslatableTexts(element.content)
            is LinearTable -> {
                for (row in 0 until element.rowCount) {
                    for (col in 0 until element.colCount) {
                        val cell = element.cellAt(row, col) ?: continue
                        if (cell.isFiller) continue
                        count += countTranslatableTexts(cell.content)
                    }
                }
            }
            is LinearImage -> {
                element.caption?.let { caption ->
                    if (caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
                        count++
                    }
                }
            }
            else -> {} // LinearAudio, LinearVideo -- no translatable text
        }
    }
    return count
}
```

#### Revised computeParagraphIndexRecursive()

Uses `countTranslatableTexts()` instead of inline flat counting:

```kotlin
is LinearListItem -> {
    result[elementIndex] = paragraphCounter.index
    paragraphCounter.index += countTranslatableTexts(element.content)
}
is LinearBlockQuote -> {
    result[elementIndex] = paragraphCounter.index
    paragraphCounter.index += countTranslatableTexts(element.content)
}
is LinearTable -> {
    result[elementIndex] = paragraphCounter.index
    paragraphCounter.index += countTranslatableTexts(listOf(element))
}
is LinearImage -> {
    val caption = element.caption
    if (caption != null && caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
        result[elementIndex] = paragraphCounter.increment()
    } else {
        result[elementIndex] = null
    }
}
```

#### Unified computeContentTranslationIndices()

Replace both `computeChildTranslationIndices()` and `computeBlockQuoteContentTranslationIndices()` with a single function using `countTranslatableTexts()`. All callers of the old functions now call the new unified function.

---

## 4. Error Handling and Fallbacks

| Error Case | Handler | User Feedback |
|------------|---------|---------------|
| AI strips XML tags from translation | `InlineTagParser` renders plain text (tags absent = no formatting) | Translation displays as plain text, same as current behavior |
| AI returns malformed XML tags | `InlineTagParser` treats unknown/broken tags as literal text | Minor visual artifact (stray `<b>` text), translation is still readable |
| AI returns unclosed tags | `InlineTagParser` closes them implicitly at string end | Formatting applied to end of string |
| `toTaggedText()` receives empty annotations list | Returns escaped plain text (no tags) | Identical to current behavior |
| `TranslatableText` with no XML tags (legacy) | `InlineTagParser.parse()` returns plain `AnnotatedString` | Backwards-compatible plain text rendering |
| JSON parse failure in translation response | Existing error handling in `parseTranslationResponse()` preserved | Error reported via `TranslationState.Error` |

**Key principle:** `InlineTagParser` NEVER throws. It always returns a valid `AnnotatedString`.

---

## 5. Backward Compatibility

1. **`TranslatableText` serialization**: No new fields added. The `text` field now may contain XML tags, but this is a content change, not a structural change. Old `TranslatableText` instances with plain text work identically.
2. **`AIClient.translate()` interface**: Unchanged. Still returns `List<String>`.
3. **`ParagraphTranslationCoordinator`**: Unchanged. Works with the updated `TranslatableText`.
4. **Translation rendering**: If a translation string has no XML tags, `InlineTagParser.parse()` returns a plain `AnnotatedString` equivalent to the current plain `Text()` rendering.
5. **New `ElementType` values**: `TABLE_CELL` and `IMAGE_CAPTION` are additive. The `getStructureDescription()` function handles them with new branches. Existing types are unaffected.

---

## 6. Testing Strategy

### 6.1 Unit Tests

| Component | Test File | Test Cases |
|-----------|-----------|------------|
| `TranslatableTextExtractor` | `TranslatableTextExtractorTest.kt` | Plain text extraction, XML tag generation, table cells, image captions, nested lists, blockquotes, skipped elements (code blocks, audio, video), empty/blank text, mixed annotations |
| `TranslatableTextExtractor.toTaggedText()` | `TranslatableTextExtractorTest.kt` | All 10 annotation types, nested annotations, overlapping annotations, empty annotations, XML escaping, text with no annotations |
| `InlineTagParser` | `InlineTagParserTest.kt` | All 10 tag types, nested tags, attribute parsing (href, face), entity unescaping, unknown tags, unclosed tags, mismatched close tags, empty input, plain text (no tags), tags at boundaries |
| `TranslationPromptBuilder` | `TranslationPromptBuilderTest.kt` | Prompt format with XML-tagged text, prompt includes formatting instructions, prompt includes TABLE_CELL and IMAGE_CAPTION types, response parsing (existing tests migrated) |
| `TranslatableText` | `TranslatableTextTest.kt` | Add tests for `TABLE_CELL` and `IMAGE_CAPTION` element types in `getStructureDescription()` |

### 6.2 Integration Tests

- Build verification: `./gradlew :app:compileFdroidDebugKotlin` after each phase
- Test verification: `./gradlew :app:testFdroidDebugUnitTest` after each phase

### 6.3 Edge Cases

| Edge Case | Expected Behavior |
|-----------|-------------------|
| Empty inline elements (`<strong></strong>`) | No tag generated (annotation with start == end is skipped) |
| Nested inline formatting (`<strong><em>bold italic</em></strong>`) | Both `<b>` and `<i>` tags generated with correct nesting |
| Code spans with special characters (`<code>a < b && c > d</code>`) | Content inside `<code>` is entity-escaped; parser unescapes it |
| Links with no href | Treated as plain text (no `<link>` tag generated) |
| Paragraphs entirely a link | Entire text wrapped in `<link>` tag |
| Table with single-column/single-row | Already handled by `HtmlLinearizer` (may not produce `LinearTable`); if it does, cells are extracted normally |
| Table with merged cells (rowspan/colspan) | Filler cells skipped, real cell content extracted |
| List items containing only images | No `LinearText` child = no `TranslatableText` produced |
| Text with literal `<` or `>` characters | Entity-escaped in `toTaggedText()`, unescaped in `InlineTagParser` |

---

## 7. Implementation Phases

### Phase 1: Pre-Refactoring (Risk Reduction)

1. Create `TranslationPromptBuilder` by extracting identical functions from both AI clients
2. Update both `OpenAICompatibleClient` and `AnthropicClient` to delegate to shared utility
3. Delete the private duplicated functions from both clients
4. Verify: all existing tests pass, build succeeds

### Phase 2: Extraction + Index Mapping Fixes

1. Add `TABLE_CELL` and `IMAGE_CAPTION` to `ElementType` enum
2. Update `getStructureDescription()` for new types
3. Create `TranslatableTextExtractor` with `toTaggedText()`, `escapeXmlContent()`, and `extractRecursively()`
4. Update `ArticleViewModel.extractTranslatableParagraphs()` to delegate to `TranslatableTextExtractor`
5. Delete private extraction methods from `ArticleViewModel`
6. Add `countTranslatableTexts()` to `LinearArticleContent.kt`
7. Fix `computeParagraphIndexRecursive()` to use `countTranslatableTexts()`
8. Create unified `computeContentTranslationIndices()` replacing the two duplicated functions
9. Update callers (`LinearListItemContent`, `LinearBlockQuoteContent`) to call unified function
10. Write unit tests for extractor and index mapping

### Phase 3: Prompt Updates + Tag Parser

1. Update `TranslationPromptBuilder.buildTranslationPrompt()` with XML tag formatting instructions section
2. Create `InlineTagParser` with single-pass state machine
3. Write unit tests for `InlineTagParser`
4. Write unit tests for updated prompt builder

### Phase 4: Rendering Integration

1. Update `LinearTextContent` to use `InlineTagParser.parse()` for translation text
2. Add `captionTranslation` parameter to `LinearImageContent`, pass to `LinearTextContent`
3. Add `translatedParagraphs` and `tableTranslationStartIndex` parameters to `LinearTableContent`
4. Add table cell translation index computation inside `LinearTableContent`
5. Update `LinearElementContent` to thread new parameters to `LinearTableContent` and `LinearImageContent`
6. Build and test verification

---

## 8. Security Considerations

- **Link URLs preserved through translation**: URLs from original HTML are embedded in `<link href="...">` tags. The `InlineTagParser` extracts them and creates `LinkAnnotation.Clickable` -- identical attack surface to current original text rendering.
- **Tag whitelist**: `InlineTagParser` only recognizes 10 known tag names. Unknown tags (including any injected `<script>`, `<img>`, etc.) are rendered as literal text, preventing XSS-like issues.
- **XML entity escaping**: Content text is entity-escaped before tag insertion, preventing content from being interpreted as markup.
- **Code spans non-translatable**: Prevents injection of translated code.
- **No user input in XML tags**: Tagged text is generated programmatically from `LinearTextAnnotation` data, not from user input.

---

## 9. Performance Considerations

- **Token overhead**: XML tags add ~15-25% token overhead per paragraph. Well within the < 2x NFR-3 requirement.
- **Parsing performance**: Custom tag parser: ~2-10 microseconds per paragraph. Negligible vs ~500ms-2s network round-trip.
- **Index mapping**: `countTranslatableTexts()` is O(n) where n = total elements in the tree. Called once per article render.
- **Table extraction**: Each non-filler cell is a separate translation unit. For a typical table (2-5 cols, 5-10 rows), this adds 10-50 translation units. Handled by `ParagraphTranslationCoordinator`'s Semaphore(3).

---

## 10. Files Affected

### Files to be Created
| File Path | Purpose |
|-----------|---------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractor.kt` | Extraction from LinearElement tree with XML tagging |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilder.kt` | Shared prompt building + response parsing |
| `app/src/main/java/com/nononsenseapps/feeder/ai/InlineTagParser.kt` | XML tags -> AnnotatedString parser |
| `app/src/test/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractorTest.kt` | Extraction unit tests |
| `app/src/test/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilderTest.kt` | Prompt/parsing unit tests |
| `app/src/test/java/com/nononsenseapps/feeder/ai/InlineTagParserTest.kt` | Tag parser unit tests |

### Files to be Modified
| File Path | Changes |
|-----------|---------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableText.kt` | Add `TABLE_CELL`, `IMAGE_CAPTION` to `ElementType`; update `getStructureDescription()` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` | Delete duplicated translation functions; delegate to `TranslationPromptBuilder` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` | Delete duplicated translation functions; delegate to `TranslationPromptBuilder` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | Replace private extraction methods with `TranslatableTextExtractor.extract()` call |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` | Fix index mapping; add `countTranslatableTexts()`; unify content translation indices; update `LinearTextContent`, `LinearTableContent`, `LinearImageContent`, `LinearElementContent` |
| `app/src/test/java/com/nononsenseapps/feeder/ai/TranslatableTextTest.kt` | Add tests for new `ElementType` values |

### File Summary
- **Total Files Created:** 6
- **Total Files Modified:** 6
- **Total Files Deleted:** 0
- **Total Files Affected:** 12

---

## 11. References

- Requirements: `./01-requirements.md`
- Research Report: `./02-research.md`
- Code Assessment: `./03-code-assessment.md`
- Architecture: `./04-architecture.md`
