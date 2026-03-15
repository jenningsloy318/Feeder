# Task List: Improve Text Extraction for Translation

**Specification:** `./05-specification.md`
**Architecture:** `./04-architecture.md`
**Total Tasks:** 30

**CRITICAL:** All phases defined in this plan MUST be implemented in a single continuous execution. The execution-coordinator will NOT pause between phases or ask for permission to continue. Every phase from Phase 1 to Final Phase will be completed automatically.

---

## Phase 1: Pre-Refactoring (Extract Shared TranslationPromptBuilder)

**Goal:** Eliminate ~220 LOC of duplicated translation prompt/parsing code between AI clients before making feature changes. This reduces the blast radius of all subsequent prompt modifications from 2 files to 1.

**Dependencies:** None (pure refactoring of existing code)

- [ ] **T1.1** Create `TranslationPromptBuilder` object with functions extracted from AI clients
  - **Files:**
    - CREATE: `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilder.kt`
    - READ: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` (lines 520-768)
  - **Details:** Create `TranslationPromptBuilder` object. Copy the following functions from `OpenAICompatibleClient.kt` (they are identical in both clients):
    - `buildTranslationPrompt()` (lines 520-597)
    - `parseTranslationResponse()` (lines 612-702)
    - `extractJsonFromResponse()` (lines 707-723)
    - `jsonEscape()` (lines 728-735)
    - `unescapeJson()` (lines 739-745)
    - `handleTranslationError()` (lines 750-768)
  - Make `buildTranslationPrompt()`, `parseTranslationResponse()`, and `handleTranslationError()` public. Make `extractJsonFromResponse()`, `jsonEscape()`, `unescapeJson()` internal (for testing).
  - **Complexity:** Medium
  - **Acceptance:** File compiles. Functions match the original implementations exactly.

- [ ] **T1.2** Update `OpenAICompatibleClient` to delegate to `TranslationPromptBuilder`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`
  - **Details:** In the `translate()` method, replace:
    - `buildTranslationPrompt(...)` -> `TranslationPromptBuilder.buildTranslationPrompt(...)`
    - `parseTranslationResponse(...)` -> `TranslationPromptBuilder.parseTranslationResponse(...)`
    - `handleTranslationError(...)` -> `TranslationPromptBuilder.handleTranslationError(...)`
  - Delete the 6 private functions that were extracted.
  - **Complexity:** Low
  - **Acceptance:** File compiles. No private translation functions remain.

- [ ] **T1.3** Update `AnthropicClient` to delegate to `TranslationPromptBuilder`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`
  - **Details:** Same changes as T1.2 for the Anthropic client. Delete the 6 identical private functions.
  - **Complexity:** Low
  - **Acceptance:** File compiles. No private translation functions remain.

- [ ] **T1.4** Create `TranslationPromptBuilderTest` with tests migrated from existing logic
  - **Files:**
    - CREATE: `app/src/test/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilderTest.kt`
  - **Details:** Write unit tests for:
    - `buildTranslationPrompt()`: correct JSON format, paragraph indexing, structure descriptions
    - `parseTranslationResponse()`: valid JSON, missing fields, partial responses
    - `jsonEscape()`: special characters, quotes, newlines
    - `unescapeJson()`: reverse of jsonEscape
    - `extractJsonFromResponse()`: plain JSON, markdown-wrapped JSON, garbage prefix/suffix
    - `handleTranslationError()`: various exception types
  - **Complexity:** Medium
  - **Acceptance:** All tests pass. Coverage for prompt building and parsing.

- [ ] **T1.5** Build verification for Phase 1
  - **Command:** `./gradlew :app:compileFdroidDebugKotlin && ./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** Build succeeds. All existing tests pass. No behavioral changes.

---

## Phase 2: Extraction + Index Mapping Fixes

**Goal:** Create `TranslatableTextExtractor` with XML-tagged text support. Add `TABLE_CELL` and `IMAGE_CAPTION` element types. Fix the deeply nested list index mismatch bug. Unify the three duplicated index mapping functions.

**Dependencies:** Phase 1 complete (TranslationPromptBuilder exists)

- [ ] **T2.1** Add `TABLE_CELL` and `IMAGE_CAPTION` to `ElementType` enum
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableText.kt`
  - **Details:**
    - Add `TABLE_CELL` and `IMAGE_CAPTION` to the `ElementType` enum
    - Add branches to `getStructureDescription()`: `TABLE_CELL -> "table cell"`, `IMAGE_CAPTION -> "image caption"`
  - **Complexity:** Low
  - **Acceptance:** File compiles. New enum values exist.

- [ ] **T2.2** Add tests for new `ElementType` values
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ai/TranslatableTextTest.kt`
  - **Details:** Add tests for:
    - `TranslatableText(text = "cell", elementType = ElementType.TABLE_CELL).getStructureDescription()` returns `"table cell"`
    - `TranslatableText(text = "caption", elementType = ElementType.IMAGE_CAPTION).getStructureDescription()` returns `"image caption"`
  - **Complexity:** Low
  - **Acceptance:** New tests pass.

- [ ] **T2.3** Create `TranslatableTextExtractor` with `toTaggedText()` and `extractRecursively()`
  - **Files:**
    - CREATE: `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractor.kt`
  - **Details:** Create `TranslatableTextExtractor` object with:
    - `fun extract(elements: List<LinearElement>): List<TranslatableText>` -- public entry point
    - `private fun extractRecursively(elements, result, nestingLevel, defaultElementType)` -- recursive DFS traversal handling:
      - `LinearText`: if blockStyle == TEXT and text is not blank, call `toTaggedText()` to convert annotations to XML tags, determine ElementType from heading annotations, use `defaultElementType` for non-headings
      - `LinearListItem`: recurse into `element.content` with `nestingLevel + 1`
      - `LinearBlockQuote`: recurse into `element.content` with `nestingLevel + 1`
      - `LinearTable`: iterate rows/cols, skip fillers, recurse into cell content with `defaultElementType = TABLE_CELL`
      - `LinearImage`: if caption is non-null, non-blank TEXT, add as `IMAGE_CAPTION`
      - `else -> {}`: skip LinearAudio, LinearVideo
    - `internal fun toTaggedText(text: String, annotations: List<LinearTextAnnotation>): String` -- converts annotation ranges to XML tags:
      1. Filter out H1-H6 annotations (used for ElementType detection, not inline tags)
      2. If no remaining annotations, return `escapeXmlContent(text)`
      3. Escape XML special chars in content text
      4. Build events list from annotations (open at start, close at endExclusive)
      5. Sort events: by position, opens before closes at same position, wider spans first among opens
      6. Walk through escaped text inserting tags at event positions
    - `private fun escapeXmlContent(text: String): String` -- `&` -> `&amp;`, `<` -> `&lt;`, `>` -> `&gt;`
    - `private fun getElementTypeFromAnnotations(annotations: List<LinearTextAnnotation>): ElementType` -- moved from ArticleViewModel
  - **Complexity:** High
  - **Acceptance:** File compiles. `toTaggedText()` correctly produces XML-tagged text for all 10 annotation types.

- [ ] **T2.4** Update `ArticleViewModel` to use `TranslatableTextExtractor`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:**
    - Change `extractTranslatableParagraphs()` to: `return TranslatableTextExtractor.extract(content.elements)`
    - Delete private methods `extractTranslatableTextRecursively()` and `getElementTypeFromAnnotations()`
    - Add import for `TranslatableTextExtractor`
  - **Complexity:** Low
  - **Acceptance:** File compiles. Private extraction methods removed.

- [ ] **T2.5** Add `countTranslatableTexts()` function to `LinearArticleContent.kt`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`
  - **Details:** Add private function `countTranslatableTexts(elements: List<LinearElement>): Int` that recursively counts translatable text elements. Must handle all element types exactly matching `TranslatableTextExtractor.extractRecursively()`:
    - `LinearText`: count if blockStyle == TEXT and text.isNotBlank()
    - `LinearListItem`: recurse into content
    - `LinearBlockQuote`: recurse into content
    - `LinearTable`: iterate rows/cols, skip fillers, recurse into cell content
    - `LinearImage`: count if caption is non-null, non-blank TEXT
    - `else -> {}`: skip others
  - **Complexity:** Medium
  - **Acceptance:** Function compiles and mirrors extraction logic exactly.

- [ ] **T2.6** Fix `computeParagraphIndexRecursive()` to use `countTranslatableTexts()`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 174-250)
  - **Details:** Replace the inline counting logic in the `LinearListItem` and `LinearBlockQuote` branches with `countTranslatableTexts()`. Add new branches for `LinearTable` and `LinearImage`:
    - `is LinearListItem`: `result[elementIndex] = paragraphCounter.index; paragraphCounter.index += countTranslatableTexts(element.content)`
    - `is LinearBlockQuote`: same pattern
    - `is LinearTable`: `result[elementIndex] = paragraphCounter.index; paragraphCounter.index += countTranslatableTexts(listOf(element))`
    - `is LinearImage`: if caption is translatable, `result[elementIndex] = paragraphCounter.increment()` else `null`
  - **Complexity:** Medium
  - **Acceptance:** Deeply nested lists produce correct indices. Table and image captions get indices.

- [ ] **T2.7** Unify `computeChildTranslationIndices()` and `computeBlockQuoteContentTranslationIndices()` into single `computeContentTranslationIndices()`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 658-746)
  - **Details:**
    - Create `computeContentTranslationIndices(content: List<LinearElement>, startIndex: Int): Map<Int, Int?>` using `countTranslatableTexts()` for nested element counting
    - Handle all element types: `LinearText`, `LinearListItem`, `LinearBlockQuote`, `LinearTable`, `LinearImage`, `else`
    - Delete `computeChildTranslationIndices()` (lines 658-697) and `computeBlockQuoteContentTranslationIndices()` (lines 707-746)
    - Update all callers in `LinearListItemContent` and `LinearBlockQuoteContent` to call `computeContentTranslationIndices()`
  - **Complexity:** Medium
  - **Acceptance:** Both old functions replaced. Callers updated. Index mapping correct at all nesting levels.

- [ ] **T2.8** Write unit tests for `TranslatableTextExtractor`
  - **Files:**
    - CREATE: `app/src/test/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractorTest.kt`
  - **Details:** Test cases:
    - `toTaggedText()` with each of the 10 annotation types individually
    - `toTaggedText()` with nested annotations (bold inside link)
    - `toTaggedText()` with XML special characters in content (`&`, `<`, `>`)
    - `toTaggedText()` with empty annotations list (returns escaped plain text)
    - `toTaggedText()` with adjacent annotations
    - `extract()` with LinearText (plain, with annotations)
    - `extract()` with LinearListItem (single level, nested 3 levels)
    - `extract()` with LinearBlockQuote
    - `extract()` with LinearTable (single cell, multi-cell, filler cells)
    - `extract()` with LinearImage (with caption, without caption, blank caption)
    - `extract()` with mixed elements in document order
    - `extract()` skips PRE_FORMATTED and CODE_BLOCK LinearText
    - `extract()` skips LinearAudio and LinearVideo
  - **Complexity:** High
  - **Acceptance:** All tests pass. Comprehensive coverage of extraction logic.

- [ ] **T2.9** Build verification for Phase 2
  - **Command:** `./gradlew :app:compileFdroidDebugKotlin && ./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** Build succeeds. All tests pass (existing + new).

---

## Phase 3: Prompt Updates + Tag Parser

**Goal:** Update the translation prompt to include XML tag formatting instructions. Create `InlineTagParser` to convert XML-tagged translation strings to `AnnotatedString`.

**Dependencies:** Phase 2 complete (extraction produces XML-tagged text)

- [ ] **T3.1** Update `TranslationPromptBuilder.buildTranslationPrompt()` with XML tag instructions
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilder.kt`
  - **Details:** Add to the prompt (after existing Translation Guidelines section):
    - "Inline Formatting Tags" section with tag reference table (all 10 tags)
    - Rules: preserve all tags, do NOT translate `<code>` content, do NOT modify URLs in `<link href>`, do NOT modify `face` attribute, preserve nesting
    - Example showing input/output with tags preserved
    - Update guideline 5 to mention `<code>` tags
    - Add guidelines 10 (table cells) and 11 (image captions)
  - **Complexity:** Medium
  - **Acceptance:** Prompt includes formatting tag instructions. Prompt includes example.

- [ ] **T3.2** Create `InlineTagParser` with single-pass state machine
  - **Files:**
    - CREATE: `app/src/main/java/com/nononsenseapps/feeder/ai/InlineTagParser.kt`
  - **Details:** Create `InlineTagParser` object with:
    - `@Composable fun parse(text: String, onLinkClick: (url: String) -> Unit): AnnotatedString`
    - Single-pass state machine that scans for `<` and `>` characters
    - Known tags whitelist: `setOf("b", "i", "code", "link", "s", "u", "sup", "sub", "mono", "font")`
    - Tag-to-style mapping matching `LinearText.toAnnotatedString()` exactly:
      - `<b>` -> `SpanStyle(fontWeight = FontWeight.Bold)`
      - `<i>` -> `SpanStyle(fontStyle = FontStyle.Italic)`
      - `<code>` -> `CodeInlineStyle()`
      - `<link href="url">` -> `LinkAnnotation.Clickable` with `LinkTextStyle()`
      - `<s>` -> `SpanStyle(textDecoration = TextDecoration.LineThrough)`
      - `<u>` -> `SpanStyle(textDecoration = TextDecoration.Underline)`
      - `<sup>` -> `SpanStyle(baselineShift = BaselineShift.Superscript)`
      - `<sub>` -> `SpanStyle(baselineShift = BaselineShift.Subscript)`
      - `<mono>` -> `SpanStyle(fontFamily = monoFontFamily)` (from `LocalTypographySettings`)
      - `<font face="x">` -> `SpanStyle(fontFamily = face.asFontFamily())`
    - Attribute parsing for `href` and `face`: find `key="value"` pattern
    - Entity unescaping: `&amp;` -> `&`, `&lt;` -> `<`, `&gt;` -> `>`, `&quot;` -> `"`
    - Fallback: unknown tags -> literal text, unclosed tags -> close implicitly, mismatched close -> ignore, never throws
    - Style stack: maintain a stack of active styles, apply all active styles to each character
  - **Complexity:** High
  - **Acceptance:** Parser handles all 10 tags with correct styles. Fallback for malformed input works. Never throws.

- [ ] **T3.3** Write unit tests for `InlineTagParser`
  - **Files:**
    - CREATE: `app/src/test/java/com/nononsenseapps/feeder/ai/InlineTagParserTest.kt`
  - **Details:** Test cases:
    - Plain text (no tags) -> AnnotatedString with no spans
    - Each of the 10 tag types individually -> correct SpanStyle/LinkAnnotation
    - Nested tags (`<link><b>text</b></link>`) -> both styles applied
    - `<link href="url">` attribute parsing -> correct href extracted
    - `<font face="serif">` attribute parsing -> correct face extracted
    - Entity unescaping (`&amp;`, `&lt;`, `&gt;`, `&quot;`)
    - Unknown tag (`<script>alert()</script>`) -> rendered as literal text
    - Unclosed tag (`<b>bold text`) -> style applied to end
    - Mismatched close tag (`<b>text</i>`) -> `</i>` ignored
    - Empty tags (`<b></b>`) -> empty span
    - Tags at string boundaries (`<b>entire string</b>`)
    - Multiple adjacent tags (`<b>bold</b> <i>italic</i>`)
    - Code tag content preserved exactly (`<code>map()</code>`)
  - **Complexity:** Medium
  - **Acceptance:** All tests pass.

- [ ] **T3.4** Update `TranslationPromptBuilderTest` for XML tag prompt format
  - **Files:** `app/src/test/java/com/nononsenseapps/feeder/ai/TranslationPromptBuilderTest.kt`
  - **Details:** Add tests verifying:
    - Prompt contains "Inline Formatting Tags" section
    - Prompt contains tag reference table
    - Prompt contains "do NOT translate" instruction for `<code>` content
    - Prompt contains example with XML tags
    - Prompt handles `TABLE_CELL` and `IMAGE_CAPTION` type descriptions
    - Response parsing works with XML-tagged translation strings
  - **Complexity:** Low
  - **Acceptance:** All tests pass.

- [ ] **T3.5** Build verification for Phase 3
  - **Command:** `./gradlew :app:compileFdroidDebugKotlin && ./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** Build succeeds. All tests pass.

---

## Phase 4: Rendering Integration

**Goal:** Wire up `InlineTagParser` in the rendering pipeline. Pass translation data to table and image components.

**Dependencies:** Phase 3 complete (InlineTagParser exists)

- [ ] **T4.1** Update `LinearTextContent` to use `InlineTagParser` for translation rendering
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 912-922)
  - **Details:** Replace the plain `Text(text = translation, ...)` with:
    ```kotlin
    val annotatedTranslation = InlineTagParser.parse(
        text = translation,
        onLinkClick = { url -> onLinkClick(url, null) },
    )
    Text(text = annotatedTranslation, ...)
    ```
    Keep the existing `color`, `modifier`, and `softWrap` parameters on the `Text()` call.
  - **Complexity:** Low
  - **Acceptance:** Translation text renders with formatting. Links in translations are clickable.

- [ ] **T4.2** Add `captionTranslation` parameter to `LinearImageContent`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 748-867)
  - **Details:**
    - Add `captionTranslation: String? = null` parameter to `LinearImageContent`
    - In the caption rendering block (line 859), pass `translation = captionTranslation` to `LinearTextContent`
  - **Complexity:** Low
  - **Acceptance:** Image captions can receive translation text. Default `null` preserves current behavior.

- [ ] **T4.3** Add translation parameters to `LinearTableContent`
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 1150-1241)
  - **Details:**
    - Add `translatedParagraphs: List<String>? = null` and `tableTranslationStartIndex: Int = -1` parameters
    - Inside the cell rendering loop, compute translation indices for cells using `computeContentTranslationIndices()` called once per table, iterating cells in the same row-major order as extraction
    - For each cell's `LinearElementContent`, pass the computed translation string
  - **Complexity:** Medium
  - **Acceptance:** Table cells can receive and display translation text.

- [ ] **T4.4** Update `LinearElementContent` to thread new parameters
  - **Files:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` (lines 318-427)
  - **Details:** Update the `when` dispatch:
    - `is LinearTable` branch: pass `translatedParagraphs` and `tableTranslationStartIndex = parentTranslationIndex ?: -1`
    - `is LinearImage` branch: pass `captionTranslation = translation`
  - **Complexity:** Low
  - **Acceptance:** Translation data flows to table and image components.

- [ ] **T4.5** Build verification for Phase 4
  - **Command:** `./gradlew :app:compileFdroidDebugKotlin && ./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** Build succeeds. All tests pass.

---

## Final Tasks

- [ ] **TF.1** Run all tests and fix any failures
  - **Command:** `./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** All tests pass (0 failures).

- [ ] **TF.2** Code review
  - **Agent:** `super-dev:code-reviewer`
  - **Acceptance:** No blocking issues.

- [ ] **TF.3** Commit and push changes
  - **Skill:** `generating-commit-messages`
  - **Message format:** `feat spec-32-improve-text-extraction: <description>`
  - **Acceptance:** Changes committed and pushed to `spec-32-improve-text-extraction` branch.

---

## Task Dependencies

```
T1.1 ──> T1.2 ──┐
         T1.3 ──┤
         T1.4 ──┤
                └──> T1.5 ──> T2.1 ──> T2.3 ──> T2.4
                              T2.2      │
                                        ├──> T2.5 ──> T2.6
                                        │              T2.7
                                        └──> T2.8
                                                  ──> T2.9 ──> T3.1
                                                               T3.2 ──> T3.3
                                                               T3.4
                                                         ──> T3.5 ──> T4.1
                                                                       T4.2
                                                                       T4.3
                                                                       T4.4
                                                                 ──> T4.5 ──> TF.1 ──> TF.2 ──> TF.3
```

## Priority Order

1. **T1.1-T1.5** (Phase 1) -- Eliminate duplication first to reduce risk for all subsequent changes
2. **T2.1-T2.9** (Phase 2) -- Core extraction and index mapping changes
3. **T3.1-T3.5** (Phase 3) -- Prompt and parser (depends on extraction)
4. **T4.1-T4.5** (Phase 4) -- Rendering integration (depends on parser)
5. **TF.1-TF.3** (Final) -- Verification and commit
