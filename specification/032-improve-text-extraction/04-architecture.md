# Architecture: Improve Text Extraction for Translation (Spec-32)

**Date:** 2026-03-15
**Author:** Claude (Architecture Agent)
**Status:** Draft (v2 -- updated for XML tag approach)
**Inputs:** 01-requirements.md, 02-research.md, 03-code-assessment.md

---

## 1. Overview

This architecture improves the text extraction and translation pipeline to:
1. Preserve **all** inline formatting (bold, italic, links, code, strikethrough, underline, superscript, subscript, monospace, font) using XML-like inline tags
2. Extract table cell content and image captions for translation
3. Fix deeply nested list translation index mismatch
4. Handle inline images that split paragraphs
5. Eliminate ~220 LOC of duplicated translation prompt/parsing code between AI clients

The approach chosen is **XML-like Inline Tags** (user decision updated from markdown markers). This provides full coverage of all 10 annotation types in `LinearTextAnnotation.kt`, including underline, superscript, subscript, monospace, and font which have no standard markdown equivalents.

---

## 2. Architectural Drivers

- **Counter parity rule**: Extraction count MUST equal index mapping count at all nesting depths
- **DRY principle**: Identical prompt/parsing code in two AI clients creates high risk of desynchronization
- **Backwards compatibility**: `TranslatableText` is `@Serializable`; new fields need defaults
- **`AIClient.translate()` returns `List<String>`**: Tag-to-AnnotatedString parsing happens on the rendering side
- **Full annotation coverage**: All 10 `LinearTextAnnotationData` subtypes must be representable
- **Existing patterns**: `when` exhaustive dispatch on `LinearElement`, composable parameter threading, recursive traversal

---

## 3. Architecture Options

### Context

The codebase has five distinct concerns that need changes:
1. **Extraction** -- walking `LinearElement` tree to produce `TranslatableText`
2. **Prompt/parsing** -- building the AI prompt and parsing responses (duplicated in 2 clients)
3. **Index mapping** -- `computeParagraphIndexRecursive()` + 2 helper functions
4. **Tag parsing** -- new: converting XML-like tags in translated text to `AnnotatedString`
5. **Rendering** -- changing `LinearTextContent` et al. to display formatted translations

The question is: how to organize these changes to minimize risk, maximize testability, and follow existing patterns.

---

### Option 1: Minimal In-Place Changes

**Description:** Make the smallest possible changes to existing files. Keep extraction in `ArticleViewModel`, keep index mapping functions as-is (just fix the bugs), add tag parser as a new utility, keep prompt code in each client (sync manually).

**Strengths:**
- Smallest diff; fewest files touched
- No structural refactoring risk
- Fastest to implement

**Weaknesses:**
- Prompt/parsing still duplicated (high risk for future changes)
- Extraction still untestable (private on ViewModel)
- Index mapping still 3 separate functions that must stay manually synchronized

**Complexity:** Low | **Risk:** Medium-High (duplication risk)

---

### Option 2: Extract Shared Utilities + Fix In-Place (Recommended)

**Description:** Pre-refactor the duplicated code into shared utilities, then make the feature changes. Extract: (a) translation prompt/parsing into `TranslationPromptBuilder`, (b) extraction logic into a standalone function, (c) unify the 3 index-mapping functions into a single recursive function. Add tag parser as a new utility.

**Strengths:**
- Eliminates duplication before making changes (reduces blast radius from 2 files to 1)
- Extraction becomes unit-testable
- Single index-mapping function eliminates sync bugs
- Tag parser is independently testable
- Follows existing project pattern of utility functions

**Weaknesses:**
- Larger initial diff (refactoring + feature)
- Refactoring step requires careful verification

**Complexity:** Medium | **Risk:** Low (refactoring reduces overall risk)

---

### Option 3: Full Pipeline Abstraction

**Description:** Create a `TranslationPipeline` abstraction that encapsulates the entire flow: extraction -> prompt building -> response parsing -> tag parsing -> AnnotatedString rendering. Both AI clients and the ViewModel delegate to this pipeline.

**Strengths:**
- Cleanest separation of concerns
- All pipeline stages independently testable
- Single entry point for the entire translation flow

**Weaknesses:**
- Over-engineered for the current need (YAGNI violation)
- Very large diff; high risk of regression
- Creates abstractions for one-time use
- Doesn't follow existing project patterns (no pipeline abstraction pattern in codebase)

**Complexity:** High | **Risk:** High (over-engineering, large blast radius)

---

### Comparison Matrix

| Criteria | Weight | Opt 1: In-Place | Opt 2: Shared Utils | Opt 3: Full Pipeline |
|----------|--------|:---:|:---:|:---:|
| Modularity | 0.10 | 2 | 4 | 5 |
| Coupling/Cohesion | 0.10 | 2 | 4 | 5 |
| Scalability | 0.10 | 3 | 4 | 4 |
| Performance | 0.10 | 4 | 4 | 4 |
| Security | 0.10 | 3 | 3 | 3 |
| Impl. Complexity | 0.08 | 5 | 4 | 2 |
| Risk | 0.08 | 2 | 4 | 2 |
| Time-to-Value | 0.07 | 5 | 4 | 2 |
| Maintainability | 0.04 | 2 | 4 | 4 |
| Testability | 0.03 | 1 | 5 | 5 |
| Observability | 0.05 | 3 | 3 | 4 |
| Reliability | 0.05 | 2 | 4 | 3 |
| Cost | 0.05 | 4 | 4 | 3 |
| Supportability | 0.03 | 3 | 4 | 3 |
| Reversibility | 0.02 | 4 | 4 | 2 |
| **Weighted Total** | **1.00** | **2.93** | **3.90** | **3.36** |

### Recommendation

**Recommended: Option 2 -- Extract Shared Utilities + Fix In-Place**

**Rationale:** Option 2 eliminates the highest-risk technical debt (code duplication) before making feature changes, reducing the blast radius of prompt/parsing modifications from 2 files to 1. It makes extraction and tag parsing independently testable. It follows the project's existing pattern of utility functions without introducing unnecessary abstractions.

**Trade-offs:**
- **What we gain:** Eliminated duplication, testable extraction, single index-mapping function, clean tag parser
- **What we give up:** Slightly larger initial diff than Option 1 (but lower total risk)

---

## 4. Module Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        ArticleViewModel                          │
│  translateArticle() calls TranslatableTextExtractor               │
│  then ParagraphTranslationCoordinator                             │
└──────────────┬───────────────────────────────────────────────────┘
               │ List<TranslatableText>
               ▼
┌──────────────────────────────────────────────────────────────────┐
│              ParagraphTranslationCoordinator                      │
│  Per-paragraph parallel translation (unchanged)                   │
│  Calls AIClient.translate() for each paragraph                   │
└──────────────┬───────────────────────────────────────────────────┘
               │ AIClient.translate(List<TranslatableText>)
               ▼
┌──────────────────────────────────────────────────────────────────┐
│           OpenAICompatibleClient / AnthropicClient                │
│  translate() delegates to TranslationPromptBuilder                │
│  for prompt building and response parsing                         │
└──────────────┬───────────────────────────────────────────────────┘
               │ List<String> (XML-tagged translations)
               ▼
┌──────────────────────────────────────────────────────────────────┐
│                 LinearArticleContent.kt (UI)                      │
│  computeParagraphIndices() -- unified recursive counter           │
│  LinearTextContent() -- parses XML tags via InlineTagParser       │
│  LinearTableContent() -- receives translation params              │
│  LinearImageContent() -- receives caption translation             │
└──────────────────────────────────────────────────────────────────┘
```

### New/Modified Files

```
app/src/main/java/com/nononsenseapps/feeder/ai/
├── TranslatableText.kt          [MODIFIED] -- add new ElementTypes
├── TranslatableTextExtractor.kt [NEW]      -- extraction from LinearElement tree
├── TranslationPromptBuilder.kt  [NEW]      -- shared prompt building + response parsing
├── InlineTagParser.kt           [NEW]      -- XML tags -> AnnotatedString
├── AIClient.kt                  [UNCHANGED]
├── ParagraphTranslationCoordinator.kt [UNCHANGED]
├── provider/
│   ├── OpenAICompatibleClient.kt  [MODIFIED] -- delegate to TranslationPromptBuilder
│   └── AnthropicClient.kt        [MODIFIED] -- delegate to TranslationPromptBuilder

app/src/main/java/com/nononsenseapps/feeder/ui/compose/
├── feedarticle/
│   └── ArticleViewModel.kt      [MODIFIED] -- call TranslatableTextExtractor
├── html/
│   └── LinearArticleContent.kt  [MODIFIED] -- unified index mapping, formatted rendering

app/src/test/java/com/nononsenseapps/feeder/ai/
├── TranslatableTextExtractorTest.kt [NEW]  -- extraction unit tests
├── TranslationPromptBuilderTest.kt  [NEW]  -- prompt/parsing unit tests
├── InlineTagParserTest.kt           [NEW]  -- tag parsing unit tests
```

---

## 5. Detailed Component Design

### A. Data Model Changes

#### A.1 `ElementType` Enum -- Add New Values

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

#### A.2 `TranslatableText` -- No Structural Change Needed

The `text` field will now contain XML-tagged text (e.g., `"Click <link href=\"https://example.com\">here</link> for <b>important</b> info"`). No new fields are needed because:

- The XML tags ARE the inline formatting data -- embedded in the text string itself
- `@Serializable` compatibility is maintained (no new fields, just different text content)
- Backwards compat: if annotations are absent from a `LinearText`, the extraction produces plain text (current behavior)

This is the key insight: XML tags in the text string are the annotation representation for the translation pipeline. No separate annotation list is needed on `TranslatableText`.

---

### B. Shared Translation Utility: `TranslationPromptBuilder`

#### B.1 Location

`app/src/main/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilder.kt`

#### B.2 Design

```kotlin
// TranslationPromptBuilder.kt
package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage

/**
 * Shared utility for building translation prompts and parsing responses.
 * Eliminates duplication between OpenAICompatibleClient and AnthropicClient.
 */
object TranslationPromptBuilder {

    fun buildTranslationPrompt(
        translatableTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): String { /* ... */ }

    fun parseTranslationResponse(
        response: String,
        expectedParagraphs: Int,
    ): List<String> { /* ... */ }

    // Internal helpers (private)
    internal fun extractJsonFromResponse(response: String): String
    internal fun jsonEscape(text: String): String
    internal fun unescapeJson(text: String): String
    fun handleTranslationError(e: Exception): String
}
```

#### B.3 Functions Moved from AI Clients

| Function | From (both clients) | To |
|----------|--------------------|----|
| `buildTranslationPrompt()` | Private in each client | `TranslationPromptBuilder.buildTranslationPrompt()` |
| `parseTranslationResponse()` | Private in each client | `TranslationPromptBuilder.parseTranslationResponse()` |
| `extractJsonFromResponse()` | Private in each client | `TranslationPromptBuilder.extractJsonFromResponse()` |
| `jsonEscape()` | Private in each client | `TranslationPromptBuilder.jsonEscape()` |
| `unescapeJson()` | Private in each client | `TranslationPromptBuilder.unescapeJson()` |
| `handleTranslationError()` | Private in each client | `TranslationPromptBuilder.handleTranslationError()` |

Both clients' `translate()` methods change from:
```kotlin
val prompt = buildTranslationPrompt(translatableTexts, targetLanguage)
// ... API call ...
val translations = parseTranslationResponse(content, translatableTexts.size)
```
to:
```kotlin
val prompt = TranslationPromptBuilder.buildTranslationPrompt(translatableTexts, targetLanguage)
// ... API call (unchanged) ...
val translations = TranslationPromptBuilder.parseTranslationResponse(content, translatableTexts.size)
```

The private AI client methods are deleted from both files.

**Note:** Summary-related duplicated functions (`buildSummaryPrompt()`, `parseSummaryJsonResponse()`, etc.) are **out of scope** for this spec. Only translation-related functions are extracted.

---

### C. Text Extraction Pipeline: `TranslatableTextExtractor`

#### C.1 Location

`app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractor.kt`

#### C.2 Design

```kotlin
// TranslatableTextExtractor.kt
package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.model.html.*

/**
 * Extracts translatable text from a LinearArticle's element tree.
 * Produces XML-tagged text for inline annotations.
 */
object TranslatableTextExtractor {

    fun extract(elements: List<LinearElement>): List<TranslatableText> {
        val result = mutableListOf<TranslatableText>()
        extractRecursively(elements, result, nestingLevel = 0)
        return result
    }

    private fun extractRecursively(
        elements: List<LinearElement>,
        result: MutableList<TranslatableText>,
        nestingLevel: Int,
    ) { /* ... */ }

    // Converts LinearTextAnnotation ranges to XML tags in text
    internal fun toTaggedText(text: String, annotations: List<LinearTextAnnotation>): String

    // Escapes XML special chars in content text
    private fun escapeXmlContent(text: String): String

    // Detects heading element type from annotations
    private fun getElementTypeFromAnnotations(annotations: List<LinearTextAnnotation>): ElementType
}
```

#### C.3 Extraction Changes by Element Type

**`LinearText` (modified)**:

```
BEFORE: element.text (plain text)
AFTER:  toTaggedText(element.text, element.annotations) (XML-tagged)
```

The `toTaggedText()` function:
1. Escapes XML special characters in the content text (`<` -> `&lt;`, `>` -> `&gt;`, `&` -> `&amp;`)
2. Sorts annotations by start position
3. Walks through the text character by character
4. At each annotation boundary, inserts the appropriate XML open/close tag
5. Handles nesting correctly (e.g., `<link href="url"><b>bold link</b></link>`)

**`LinearTable` (new branch)**:

```kotlin
is LinearTable -> {
    for (row in 0 until element.rowCount) {
        for (col in 0 until element.colCount) {
            val cell = element.cellAt(row, col) ?: continue
            if (cell.isFiller) continue  // Skip spanned filler cells
            // Recurse into cell content with TABLE_CELL element type
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

**`LinearImage` (new branch -- captions only)**:

```kotlin
is LinearImage -> {
    element.caption?.let { caption ->
        if (caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
            result.add(TranslatableText(
                text = toTaggedText(caption.text, caption.annotations),
                elementType = ElementType.IMAGE_CAPTION,
                nestingLevel = nestingLevel,
            ))
        }
    }
}
```

**Inline images within paragraphs (Problem 2):**

The `HtmlLinearizer` splits a `<p>` with inline `<img>` into separate `LinearText` + `LinearImage` + `LinearText` siblings at the top level. Since each `LinearText` is already a separate extraction unit, and these fragments are short, the recommended approach is:

- **Keep them as separate translation units** (current behavior for text fragments)
- **Do NOT attempt paragraph reconstruction** -- merging fragments across `LinearImage` boundaries is fragile and changes the extraction count, which would complicate index mapping
- The AI translates each fragment independently. While this may occasionally produce slightly less coherent translations for split sentences, it avoids complex merging logic and maintains index mapping simplicity

This is the pragmatic trade-off: correctness and simplicity over marginal translation quality improvement for a rare case.

#### C.4 XML Tag Conversion: `toTaggedText()`

**Algorithm:**

```
Input:  text = "Click here for important info"
        annotations = [
            Link(href="https://example.com", start=6, end=9),   // "here"
            Bold(start=15, end=23),                              // "important"
        ]

Output: "Click <link href="https://example.com">here</link> for <b>important</b> info"
```

**Steps:**

1. Escape XML special chars in the source text: `&` -> `&amp;`, `<` -> `&lt;`, `>` -> `&gt;`
2. Build a list of "events" from annotations: `(position, type, open/close, data)`
3. Sort events by position (opens before closes at same position for nesting)
4. Walk through the escaped text, inserting tags at event positions

**Complete tag mapping (all 10 annotation types):**

| LinearTextAnnotationData | Open Tag | Close Tag | Notes |
|--------------------------|----------|-----------|-------|
| `Bold` | `<b>` | `</b>` | |
| `Italic` | `<i>` | `</i>` | |
| `Code` | `<code>` | `</code>` | Content inside NOT translated |
| `Link(href)` | `<link href="...">` | `</link>` | URL in attribute |
| `Strikethrough` | `<s>` | `</s>` | |
| `Underline` | `<u>` | `</u>` | NEW: no markdown equivalent |
| `Superscript` | `<sup>` | `</sup>` | NEW: no markdown equivalent |
| `Subscript` | `<sub>` | `</sub>` | NEW: no markdown equivalent |
| `Monospace` | `<mono>` | `</mono>` | NEW: uses custom tag to avoid confusion with `<code>` |
| `Font(face)` | `<font face="...">` | `</font>` | NEW: face in attribute |
| `H1-H6` | -- | -- | Already detected as ElementType, not inline tags |

**Escaping strategy:**

Content text is XML-entity-escaped **before** tag insertion:
```
& -> &amp;   (must be first)
< -> &lt;
> -> &gt;
" -> &quot;  (only inside attribute values)
```

Characters that are part of annotation boundaries are NOT escaped -- they get replaced by tags.

**Nesting priority (when annotations overlap at the same position):**

Link wraps other annotations: `<link href="url"><b>bold link text</b></link>` not `<b><link href="url">bold link text</link></b>`

This matches how HTML nests: `<a><strong>text</strong></a>`.

#### C.5 ArticleViewModel Changes

`ArticleViewModel.extractTranslatableParagraphs()` changes from calling the private `extractTranslatableTextRecursively()` to:

```kotlin
private fun extractTranslatableParagraphs(): List<TranslatableText> {
    val content = viewState.value.articleContent
    return TranslatableTextExtractor.extract(content.elements)
}
```

The private methods `extractTranslatableTextRecursively()` and `getElementTypeFromAnnotations()` are deleted from `ArticleViewModel`.

---

### D. Translation Prompt Changes

#### D.1 Updated JSON Schema

**Input to AI:**
```json
{
  "targetLanguage": "Chinese",
  "paragraphs": [
    {"index": 1, "type": "heading level 2", "text": "Introduction"},
    {"index": 2, "type": "paragraph", "text": "Click <link href=\"https://example.com\">here</link> for <b>important</b> info with <code>map()</code> code"},
    {"index": 3, "type": "list item (nesting level: 1)", "text": "First item with <i>emphasis</i> and <u>underline</u>"},
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
    {"index": 1, "translation": "介绍"},
    {"index": 2, "translation": "点击<link href=\"https://example.com\">这里</link>获取<b>重要</b>信息，使用<code>map()</code>代码"},
    {"index": 3, "translation": "第一项带有<i>强调</i>和<u>下划线</u>"},
    {"index": 4, "translation": "单元格内容"},
    {"index": 5, "translation": "摄影：<b>John Doe</b>"}
  ]
}
```

#### D.2 Prompt Additions

Add these sections to the prompt after the existing Translation Guidelines:

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
Output: "点击<link href="https://example.com">这里</link>获取<b>重要</b>信息，使用<code>map()</code>代码"
```

#### D.3 Non-Translatable Segments

The prompt explicitly instructs:
- Code spans (`<code>...</code>`) must NOT be translated
- URLs inside `<link href="...">` must NOT be modified
- Font face attributes must NOT be modified
- Technical terms inside code tags are preserved verbatim

---

### E. Response Parsing Changes

#### E.1 `parseTranslationResponse()` -- Minimal Change

The existing `parseTranslationResponse()` extracts the `"translation"` string value from each JSON object in the response. Since XML tags are embedded in the text string, **no parsing logic changes are needed** in this function.

The function already handles:
- JSON extraction from markdown code blocks
- Bracket matching for array boundaries
- String unescaping for JSON content

The returned `List<String>` now contains XML-tagged strings instead of plain text. The tag-to-AnnotatedString conversion happens on the rendering side.

#### E.2 JSON Escaping Note

The `jsonEscape()` function already handles `"` -> `\"` which is needed for XML attributes like `href="..."` inside JSON strings. The existing escaping is sufficient.

#### E.3 Tolerant Handling

If the AI strips or mangles XML tags, the downstream `InlineTagParser` falls back to plain text for the affected segment. This is inherently tolerant because:
- Unmatched `<b>` -> tag is ignored, rendered as plain text
- Broken `<link href=` -> malformed tag ignored, rendered as plain text
- This degrades gracefully to the current behavior (plain text translation)

---

### F. Inline Tag Parser: `InlineTagParser`

#### F.1 Location

`app/src/main/java/com/nononsenseapps/feeder/ai/InlineTagParser.kt`

#### F.2 Design

```kotlin
// InlineTagParser.kt
package com.nononsenseapps.feeder.ai

import androidx.compose.ui.text.AnnotatedString

/**
 * Parses XML-like inline tags in translated text and produces an AnnotatedString.
 *
 * Supported tags:
 * - <b>bold</b>           -> SpanStyle(fontWeight = Bold)
 * - <i>italic</i>         -> SpanStyle(fontStyle = Italic)
 * - <code>code</code>     -> CodeInlineStyle()
 * - <link href="url">text</link> -> LinkAnnotation.Clickable
 * - <s>strikethrough</s>  -> SpanStyle(textDecoration = LineThrough)
 * - <u>underline</u>      -> SpanStyle(textDecoration = Underline)
 * - <sup>superscript</sup> -> SpanStyle(baselineShift = Superscript)
 * - <sub>subscript</sub>  -> SpanStyle(baselineShift = Subscript)
 * - <mono>monospace</mono> -> SpanStyle(fontFamily = monoFontFamily)
 * - <font face="x">text</font> -> SpanStyle(fontFamily = face.asFontFamily())
 *
 * Nesting is supported: <link href="url"><b>bold link</b></link>
 *
 * Fallback: If parsing fails for any segment, the raw text is used as-is.
 */
object InlineTagParser {

    fun parse(
        text: String,
        onLinkClick: (url: String) -> Unit,
    ): AnnotatedString { /* ... */ }
}
```

#### F.3 Parsing Algorithm

The parser uses a **single-pass state machine** scanning for `<` and `>` characters:

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
      - If tag is <link>, emit LinkAnnotation for the accumulated text
   b. Otherwise -> opening tag
      - Read tag name and attributes until '>'
      - Push tag + attributes onto style stack
      - If self-closing (ends with '/>'), pop immediately
3. At end of string:
   - Close any unclosed tags (tolerance for malformed input)
```

**Supported tag names (whitelist):**

```kotlin
private val KNOWN_TAGS = setOf("b", "i", "code", "link", "s", "u", "sup", "sub", "mono", "font")
```

Unknown tags are ignored (treated as literal text including the angle brackets). This prevents the parser from interpreting arbitrary HTML that the AI might inject.

**Attribute parsing:**

Only two tags have attributes:
- `<link href="...">` -- extract `href` value
- `<font face="...">` -- extract `face` value

Attribute parsing is simple: find `key="value"` pattern within the tag. No need for a full XML attribute parser since we control the vocabulary.

**Nesting handling:**

```
Input:  "<link href="url"><b>bold link</b></link>"
Parse:
  <link href="url"> -> push Link(url) onto stack
  <b>               -> push Bold onto stack
  bold link         -> emit with [Link, Bold] styles
  </b>              -> pop Bold
  </link>           -> pop Link, emit LinkAnnotation

Result: AnnotatedString with:
  - LinkAnnotation over "bold link" with href="url"
  - SpanStyle(Bold) over "bold link"
```

**Tag-to-style mapping:**

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
| `<font face="x">` | `SpanStyle(fontFamily = x.asFontFamily())` |

These match exactly the styles used in `LinearText.toAnnotatedString()` (lines 1257-1382 of `LinearArticleContent.kt`).

**Fallback behavior:**

If the parser encounters malformed input (e.g., unclosed tags, unknown tags, garbled `<` sequences):
1. Unknown tags -> rendered as literal text (including angle brackets)
2. Unclosed tags at end of string -> styles applied up to end, tags closed implicitly
3. Mismatched close tags -> ignored (pop nothing)
4. Never throws -- always returns a valid AnnotatedString

This ensures the worst case is visually similar to the current behavior (plain text translation with some stray tag text).

#### F.4 Entity Unescape Handling

The parser recognizes and unescapes XML entities:
- `&amp;` -> `&`
- `&lt;` -> `<`
- `&gt;` -> `>`
- `&quot;` -> `"`

This complements the escaping done in `toTaggedText()`.

---

### G. Rendering Pipeline Changes

#### G.1 `LinearTextContent` -- Formatted Translations

Current (line 912-922):
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

The `translation` parameter type stays as `String?` -- the conversion to `AnnotatedString` happens inside `LinearTextContent`. This minimizes the change surface across all callers.

#### G.2 `LinearTableContent` -- Pass Translation Data

Current signature:
```kotlin
fun LinearTableContent(
    linearTable: LinearTable,
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
)
```

After:
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

Inside the cell rendering loop, compute the translation index for each cell and pass it to `LinearElementContent`:

```kotlin
for (element in it.content) {
    LinearElementContent(
        linearElement = element,
        translation = cellTranslation,  // NEW: from index mapping
        allowHorizontalScroll = false,
        onLinkClick = onLinkClick,
        modifier = Modifier.fillMaxWidth(),
        idToIndex = idToIndex,
    )
}
```

The table cell translation index is computed by iterating cells in the same order as extraction (row-major, skipping fillers).

#### G.3 `LinearImageContent` -- Pass Caption Translation

Current signature:
```kotlin
fun LinearImageContent(
    linearImage: LinearImage,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
)
```

After:
```kotlin
fun LinearImageContent(
    linearImage: LinearImage,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
    captionTranslation: String? = null,              // NEW
)
```

In the caption rendering:
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

#### G.4 `LinearElementContent` -- Thread New Parameters

Update the `when` dispatch in `LinearElementContent` (line 318-427):

```kotlin
is LinearTable ->
    LinearTableContent(
        linearTable = linearElement,
        allowHorizontalScroll = allowHorizontalScroll,
        onLinkClick = onLinkClick,
        modifier = modifier,
        idToIndex = idToIndex,
        translatedParagraphs = translatedParagraphs,          // NEW
        tableTranslationStartIndex = parentTranslationIndex ?: -1, // NEW
    )

is LinearImage ->
    LinearImageContent(
        linearImage = linearElement,
        onLinkClick = onLinkClick,
        modifier = modifier,
        idToIndex = idToIndex,
        captionTranslation = translation,   // NEW: translation for caption
    )
```

---

### H. Index Mapping Fix

#### H.1 The Core Bug

`computeParagraphIndexRecursive()` uses `filterIsInstance<LinearText>()` on **direct children only** for nested `LinearListItem` and `LinearBlockQuote` elements. But `extractTranslatableTextRecursively()` uses true recursion into all nesting levels.

#### H.2 Fix: Unified Recursive Counter

Replace the three separate counting functions with a **single recursive function** that mirrors `TranslatableTextExtractor.extractRecursively()` exactly:

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

#### H.3 Revised `computeParagraphIndexRecursive()`

```kotlin
private fun computeParagraphIndexRecursive(
    element: LinearElement,
    elementIndex: Int,
    result: MutableMap<Int, Int?>,
    paragraphCounter: ParagraphCounter,
) {
    when (element) {
        is LinearText -> {
            if (element.blockStyle == LinearTextBlockStyle.TEXT && element.text.isNotBlank()) {
                result[elementIndex] = paragraphCounter.increment()
            } else {
                result[elementIndex] = null
            }
        }
        is LinearListItem -> {
            result[elementIndex] = paragraphCounter.index
            // TRUE RECURSION -- count all nested translatable texts
            paragraphCounter.index += countTranslatableTexts(element.content)
        }
        is LinearBlockQuote -> {
            result[elementIndex] = paragraphCounter.index
            // TRUE RECURSION -- count all nested translatable texts
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
        else -> {
            result[elementIndex] = null
        }
    }
}
```

#### H.4 Unified `computeContentTranslationIndices()`

Replace both `computeChildTranslationIndices()` and `computeBlockQuoteContentTranslationIndices()` with a single function that uses `countTranslatableTexts()`:

```kotlin
private fun computeContentTranslationIndices(
    content: List<LinearElement>,
    startIndex: Int,
): Map<Int, Int?> {
    val result = mutableMapOf<Int, Int?>()
    var translationIndex = startIndex

    content.forEachIndexed { childIndex, element ->
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT && element.text.isNotBlank()) {
                    result[childIndex] = translationIndex++
                } else {
                    result[childIndex] = null
                }
            }
            is LinearListItem -> {
                result[childIndex] = translationIndex
                translationIndex += countTranslatableTexts(element.content)
            }
            is LinearBlockQuote -> {
                result[childIndex] = translationIndex
                translationIndex += countTranslatableTexts(element.content)
            }
            is LinearTable -> {
                result[childIndex] = translationIndex
                translationIndex += countTranslatableTexts(listOf(element))
            }
            is LinearImage -> {
                val caption = element.caption
                if (caption != null && caption.blockStyle == LinearTextBlockStyle.TEXT && caption.text.isNotBlank()) {
                    result[childIndex] = translationIndex++
                } else {
                    result[childIndex] = null
                }
            }
            else -> {
                result[childIndex] = null
            }
        }
    }
    return result
}
```

All callers of `computeChildTranslationIndices()` and `computeBlockQuoteContentTranslationIndices()` now call `computeContentTranslationIndices()`.

---

## 6. Data Flow Diagrams

### Before (Current)

```
HTML ──> HtmlLinearizer ──> LinearArticle(elements)
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
            [Extraction]                     [Rendering]
                    │                               │
      ArticleViewModel                 LinearArticleContent.kt
      .extractTranslatableText..()     computeParagraphIndices()
                    │                   ┌───────────┤
                    │                   │           │
                    v                   │     computeChild..()
            List<TranslatableText>      │     computeBlockQuote..()
              (plain text)              │           │
                    │                   └─────┬─────┘
                    v                         v
       ParagraphTranslation..       Map<Int, Int?> position->index
                    │                         │
                    v                         v
         OpenAI/Anthropic            LinearTextContent()
         buildTranslationPrompt()       Text(translation)  <-- PLAIN STRING
         parseTranslationResponse()
         (DUPLICATED in both)
                    │
                    v
            List<String> (plain text)
```

### After (Proposed)

```
HTML ──> HtmlLinearizer ──> LinearArticle(elements)
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
            [Extraction]                     [Rendering]
                    │                               │
       TranslatableTextExtractor       LinearArticleContent.kt
       .extract(elements)              computeParagraphIndices()
       .toTaggedText()                      │
                    │               computeContentTranslation..()
                    │                  (SINGLE unified function)
                    v                       │
            List<TranslatableText>          v
             (XML-tagged text)       Map<Int, Int?> position->index
                    │                       │
                    v                       v
       ParagraphTranslation..       LinearTextContent()
                    │                  InlineTagParser.parse()
                    v                  Text(annotatedTranslation)
         OpenAI/Anthropic                   │
         TranslationPromptBuilder     LinearTableContent()
         .buildTranslationPrompt()     (with translation params)
         .parseTranslationResponse()        │
         (SHARED utility)             LinearImageContent()
                    │                  (with caption translation)
                    v
            List<String> (XML-tagged text)
```

### Class Relationship Diagram

```
┌──────────────────────┐     ┌──────────────────────┐
│   ArticleViewModel   │     │   TranslatableText    │
│ ─────────────────── │     │ ───────────────────── │
│ extractTranslatable  │────>│ text: String           │
│   Paragraphs()       │     │ elementType: ElementType│
│                      │     │ nestingLevel: Int       │
└──────────┬───────────┘     │                        │
           │                 │ ElementType:            │
           │ delegates to    │  + TABLE_CELL (NEW)    │
           ▼                 │  + IMAGE_CAPTION (NEW) │
┌──────────────────────┐     └────────────────────────┘
│TranslatableTextExtractor│           ▲
│ ─────────────────────── │           │ produces
│ extract(elements)       │───────────┘
│ toTaggedText(text,      │
│   annotations)          │
│ escapeXmlContent()      │
│ extractRecursively()    │
└─────────────────────────┘

┌──────────────────────┐     ┌──────────────────────┐
│ OpenAICompatibleClient│     │  AnthropicClient     │
│ ──────────────────── │     │ ──────────────────── │
│ translate()          │     │ translate()          │
│   delegates to ──────┼─┐   │   delegates to ──────┼─┐
└──────────────────────┘ │   └──────────────────────┘ │
                         │                             │
                         ▼                             ▼
                ┌─────────────────────────┐
                │ TranslationPromptBuilder │
                │ ─────────────────────── │
                │ buildTranslationPrompt() │
                │ parseTranslationResponse()│
                │ jsonEscape()             │
                │ unescapeJson()           │
                │ extractJsonFromResponse()│
                │ handleTranslationError() │
                └─────────────────────────┘

┌──────────────────────┐     ┌──────────────────────┐
│ LinearTextContent()  │────>│  InlineTagParser      │
│ (Composable)         │     │ ──────────────────── │
│ renders translation  │     │ parse(text,           │
│ as AnnotatedString   │     │   onLinkClick)        │
└──────────────────────┘     │ -> AnnotatedString    │
                             │                       │
                             │ Supports all 10 tags: │
                             │ b, i, code, link, s,  │
                             │ u, sup, sub, mono,    │
                             │ font                  │
                             └───────────────────────┘
```

---

## 7. Implementation Phases

### Phase 1: Pre-Refactoring (Risk Reduction)

1. Extract `TranslationPromptBuilder` from both AI clients
2. Update both clients to call shared utility
3. Verify: all existing tests pass, translation behavior unchanged

### Phase 2: Extraction & Index Mapping

1. Create `TranslatableTextExtractor` with `toTaggedText()`
2. Add `TABLE_CELL`, `IMAGE_CAPTION` to `ElementType`
3. Update `ArticleViewModel` to use `TranslatableTextExtractor`
4. Fix `computeParagraphIndexRecursive()` with unified recursive counting
5. Unify `computeChildTranslationIndices()` and `computeBlockQuoteContentTranslationIndices()`
6. Add extraction unit tests
7. Add index mapping unit tests

### Phase 3: Prompt & Parser

1. Update `TranslationPromptBuilder.buildTranslationPrompt()` with XML tag formatting instructions
2. Create `InlineTagParser`
3. Add tag parser unit tests

### Phase 4: Rendering Integration

1. Update `LinearTextContent` to use `InlineTagParser` for translations
2. Update `LinearTableContent` to accept and thread translation parameters
3. Update `LinearImageContent` to accept caption translation
4. Update `LinearElementContent` to thread new parameters
5. Integration testing

---

## 8. Validation Gates

### Reuse Gate

| Component | Reuse Decision | Justification |
|-----------|---------------|---------------|
| `commonmark-java` | **Not used** | XML-based approach; no markdown parsing needed |
| `mikepenz/markdown-renderer` | **Not used** for translations | Designed for markdown, not XML tags |
| Existing `toAnnotatedString()` | **Pattern reused** | The new `InlineTagParser` follows the same `AnnotatedString.Builder` pattern and exact same style mappings |
| Existing `ParagraphTranslationCoordinator` | **Fully reused** | No changes needed |

### Glue Code Gate

| Adapter | Responsibility | Testing |
|---------|---------------|---------|
| `TranslatableTextExtractor` | Bridges `LinearElement` tree to `TranslatableText` list with XML tags | Unit tests: extraction for each element type, tag generation |
| `TranslationPromptBuilder` | Bridges `TranslatableText` to AI prompt format and back | Unit tests: prompt format, response parsing |
| `InlineTagParser` | Bridges XML-tagged strings to `AnnotatedString` | Unit tests: each tag type, nesting, attributes, entity unescaping, fallback |

### Interface-First Gate

| Interface | Contract | Stability |
|-----------|----------|-----------|
| `AIClient.translate()` | `(List<TranslatableText>, TranslationLanguage) -> TranslationResult` | **Unchanged** (stable) |
| `TranslatableTextExtractor.extract()` | `(List<LinearElement>) -> List<TranslatableText>` | **New** (evolving v0.x) |
| `TranslationPromptBuilder.buildTranslationPrompt()` | `(List<TranslatableText>, TranslationLanguage) -> String` | **New** (evolving v0.x) |
| `TranslationPromptBuilder.parseTranslationResponse()` | `(String, Int) -> List<String>` | **New** (evolving v0.x) |
| `InlineTagParser.parse()` | `(String, (String) -> Unit) -> AnnotatedString` | **New** (evolving v0.x) |

---

## 9. ADRs

### ADR-001: Use XML-like Inline Tags for Formatting in Translation

**Status:** Accepted (user-selected, updated from markdown markers)

**Context:** Inline formatting is stripped during text extraction, losing bold, italic, links, code spans, and more in translated text. The system must preserve all 10 annotation types from `LinearTextAnnotation.kt`.

**Decision Drivers:**
- Full annotation coverage: All 10 types (bold, italic, code, link, strikethrough, underline, superscript, subscript, monospace, font) must be representable
- Claude has explicit XML training and handles XML tags exceptionally well
- Unambiguous: no conflict with content text (unlike markdown `*` or `**`)
- Extensible: new annotation types can be added as new tags without syntax conflicts
- Attributes support: `href` on links, `face` on fonts map naturally to XML attributes

**Considered Options:**
1. Lightweight Markdown Markers -- covers only 5 of 10 types
2. XML-like Inline Tags -- covers all 10 types
3. Indexed Annotation References -- fragile with LLMs

**Decision Outcome:** Option 2 - XML-like Inline Tags, because they provide complete coverage of all 10 annotation types, are unambiguous, and Claude handles XML natively. For OpenAI models, explicit prompt instructions ensure reliable tag preservation.

**Consequences:**
- Good: Full coverage of all 10 annotation types
- Good: Unambiguous -- no content/markup confusion
- Good: Self-documenting tag names (e.g., `<b>`, `<i>`, `<link>`)
- Good: Nesting is well-defined via XML nesting rules
- Bad: Higher token overhead than markdown (~15-20 extra tokens per tagged paragraph vs ~6-8 for markdown)
- Bad: OpenAI models may occasionally strip HTML-like tags (mitigated by explicit prompt instructions and tolerant parser fallback)

**Reversibility Plan:** If XML tags prove unreliable with specific LLM providers, the `toTaggedText()` function can be swapped to produce markdown markers with only the extraction and parser changing. The rest of the pipeline (prompt builder, index mapping, rendering) is format-agnostic.

---

### ADR-002: Extract Shared TranslationPromptBuilder

**Status:** Proposed

**Context:** `buildTranslationPrompt()`, `parseTranslationResponse()`, and 4 helper functions are character-for-character identical in both `OpenAICompatibleClient.kt` and `AnthropicClient.kt` (~220 LOC duplicated). Spec-32 modifies the prompt format -- doing this in 2 files doubles the risk.

**Decision Drivers:**
- DRY principle: any prompt change must be done twice
- Risk: manual sync between files may introduce subtle differences
- Testability: private methods in AI clients cannot be unit-tested

**Considered Options:**
1. Keep duplication, sync manually
2. Extract to shared `TranslationPromptBuilder` object
3. Create abstract base class for AI clients

**Decision Outcome:** Option 2 - Shared object, because it eliminates duplication with minimal structural change, makes prompt logic testable, and follows the project's existing utility pattern.

**Reversibility Plan:** If needed, the shared functions can be copied back into individual clients (trivial).

**Consequences:**
- Good: Single source of truth for prompt/parsing logic
- Good: Unit-testable prompt format and parsing
- Good: Reduces spec-32 blast radius
- Bad: One more file to navigate (minimal)

---

### ADR-003: Custom XML Tag Parser Over Library

**Status:** Proposed

**Context:** Translated text contains XML-like inline tags that must be converted to `AnnotatedString` for Compose rendering. Options include using a full XML parser library, regex-based parsing, or a custom single-pass state machine.

**Decision Drivers:**
- Only 10 specific tag names from a controlled vocabulary
- No need for full XML spec compliance (no DTD, no CDATA, no processing instructions)
- Tags are simple: `<tag>`, `<tag attr="val">`, `</tag>` -- no self-closing, no namespaces
- Must be tolerant of malformed input from LLM responses
- Must produce `AnnotatedString` with exact same styles as existing `toAnnotatedString()`

**Considered Options:**
1. Custom single-pass tag parser (~200 LOC)
2. Full XML parser (javax.xml or kotlinx-serialization-xml)
3. Regex-based tag extraction

**Decision Outcome:** Option 1 - Custom parser, because the controlled vocabulary of 10 known tags doesn't justify a full XML parser. The custom parser is tolerant of malformed input (a requirement for LLM output), whereas a full XML parser would throw on invalid XML. Regex-based parsing doesn't handle nesting correctly.

**Reversibility Plan:** The `InlineTagParser.parse()` interface stays the same regardless of implementation. Can swap to a library-based parser if edge cases prove problematic.

**Consequences:**
- Good: Zero additional dependencies
- Good: ~200 LOC, fully testable
- Good: Tolerant of malformed LLM output (degrades to plain text)
- Good: Handles exactly the 10 tags we emit (whitelist approach)
- Bad: Doesn't handle full XML spec (by design -- we only emit 10 specific tags)

---

## 10. Security Considerations

- **Link URLs preserved through translation**: URLs from original HTML are embedded in `<link href="...">` tags. The parser extracts them and creates `LinkAnnotation.Clickable` -- same as current behavior for original text. No new attack surface.
- **Tag whitelist**: `InlineTagParser` only recognizes 10 known tag names. Unknown tags (including any injected `<script>`, `<img>`, etc.) are rendered as literal text, preventing XSS-like issues.
- **XML entity escaping**: Content text is entity-escaped (`<` -> `&lt;`) before tag insertion, preventing content from being interpreted as markup.
- **Code spans marked non-translatable**: Prevents injection of translated code constructs.
- **JSON response parsing**: Existing validation in `parseTranslationResponse()` is preserved in the shared utility.
- **No user input in XML tags**: The tagged text is generated programmatically from `LinearTextAnnotation` data, not from user input.

---

## 11. Performance Considerations

- **Token overhead**: XML tags add ~15-25% token overhead per paragraph (slightly higher than markdown's ~15-20% due to longer close tags like `</link>` vs `)`). Well within the < 2x NFR-3 requirement.
- **Parsing performance**: Custom tag parser: ~2-10 microseconds per paragraph. Negligible vs. ~500ms-2s network round-trip for AI translation.
- **Index mapping**: `countTranslatableTexts()` is O(n) where n = total elements in the tree. Called once per article render. No performance concern.
- **Table extraction**: Each non-filler cell is a separate translation unit. For a typical table (2-5 cols, 5-10 rows), this adds 10-50 translation units. The `ParagraphTranslationCoordinator` already handles parallel translation with Semaphore(3), so this is handled.

---

## 12. Future Considerations (NOT to be implemented now)

1. **Translation caching**: Persist translated text with its annotation data to avoid re-translation
2. **Paragraph merging for inline images**: Reconstruct split paragraphs for better translation context
3. **Batch table cells**: Group small cells into fewer translation units to reduce API calls
4. **Summary code deduplication**: Extract `buildSummaryPrompt()` and related functions similarly
5. **Provider-specific tag format**: If OpenAI proves unreliable with XML tags, could use markdown for OpenAI and XML for Claude (via provider-specific `toTaggedText()` strategy)
