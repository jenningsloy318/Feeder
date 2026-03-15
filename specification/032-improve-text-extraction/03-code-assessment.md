# Code Assessment: Improve Text Extraction for Translation (Spec-32)

**Date:** 2026-03-15
**Scope:** Translation pipeline: extraction, prompt building, response parsing, rendering
**Focus:** Architecture, patterns, code duplication, change points, blast radius

---

## Executive Summary

1. **Massive code duplication** between `OpenAICompatibleClient.kt` and `AnthropicClient.kt` -- `buildTranslationPrompt()`, `parseTranslationResponse()`, `jsonEscape()`, `unescapeJson()`, `extractJsonFromResponse()`, `handleTranslationError()`, and `extractJsonFromMarkdown()` are all **character-for-character identical**. Any prompt or parsing change must be synchronized manually.

2. **Deep nesting index mismatch bug confirmed** -- `computeParagraphIndexRecursive()` uses `filterIsInstance<LinearText>()` (flat, 1-level deep) while `extractTranslatableTextRecursively()` uses true recursion. For nesting > 2 levels, the paragraph counter diverges.

3. **Translation rendering is plain text** -- `LinearTextContent` renders the original text as a rich `AnnotatedString` (via `toAnnotatedString()`) but renders the translation as a raw `String` passed directly to `Text()` composable. This is the core fidelity gap.

4. **Three element types are skipped entirely** -- `LinearImage` (captions), `LinearTable` (cell content), and inline images within paragraphs are all in the `else -> {}` branch of `extractTranslatableTextRecursively()`.

5. **No test coverage for extraction or index mapping** -- `extractTranslatableParagraphs()` is a private method on `ArticleViewModel` with no unit tests. `computeParagraphIndices()` and its helpers are private functions with no tests. The only tests are for `TranslatableText` data class and `ParagraphTranslationCoordinator`.

---

## Architecture

### Current Data Flow

```
HTML
  |
  v
HtmlLinearizer.linearize()
  |
  v
LinearArticle(elements: List<LinearElement>)
  |                                          |
  |  [Extraction]                            |  [Rendering]
  v                                          v
ArticleViewModel                        LinearArticleContent.kt
  .extractTranslatableParagraphs()        linearArticleContent()
  .extractTranslatableTextRecursively()     |
  |                                         v
  v                                     computeParagraphIndices()
List<TranslatableText>                  computeParagraphIndexRecursive()
  |                                         |
  v                                         v
ParagraphTranslationCoordinator         Map<Int, Int?> (position -> paragraph index)
  |                                         |
  v                                         v
AIClient.translate()                    LinearElementContent()
  buildTranslationPrompt()                LinearTextContent()  <-- renders translation as plain String
  parseTranslationResponse()
  |
  v
List<String> (translated plain text)
```

### Comparison to Best Practices

| Aspect | Current | Best Practice | Gap | Priority |
|--------|---------|---------------|-----|----------|
| DRY principle | Identical code in 2 AI clients (~250 LOC duplicated) | Shared utility for prompt/parse logic | **High** -- any prompt change must be done twice | High |
| Extraction-render parity | Extraction and index mapping implemented independently as mirrored recursion | Single traversal or shared logic for both | **High** -- divergence causes bug (Problem 4) | High |
| Translation richness | `TranslatableText.text` is plain `String`; translation is plain `String` | Structured text with inline annotations | **High** -- core feature gap | High |
| Test coverage | 0% for extraction, 0% for index mapping | Unit tests for both extraction and index mapping | **Medium** -- changes are risky without tests | Medium |
| Separation of concerns | `ArticleViewModel` owns extraction logic (should be in model/domain layer) | Extraction as a standalone utility | **Low** -- works but less testable | Low |

### Recommendations

1. **Extract shared translation utilities** -- Create `TranslationPromptBuilder` (or similar) with `buildTranslationPrompt()`, `parseTranslationResponse()`, `jsonEscape()`, `unescapeJson()`, `extractJsonFromResponse()`, and `handleTranslationError()`. Both clients call the shared utility.

2. **Unify extraction and index mapping** -- Consider a single traversal function that produces both `List<TranslatableText>` and the index mapping, or make the index mapping code truly recursive (matching extraction exactly).

3. **Move extraction logic out of ViewModel** -- Make `extractTranslatableTextRecursively()` a standalone function (or extension on `LinearArticle`) so it can be unit-tested without mocking the ViewModel.

---

## Detailed Change Point Analysis

### Problem 1: Inline Formatting is Stripped

**Root cause:** `ArticleViewModel.kt:654` -- `element.text` is used instead of the full `LinearText` (which includes `annotations`).

```kotlin
// ArticleViewModel.kt:654
val text = element.text  // <-- Plain text, no annotations
```

**Files that need to change:**

| File | Line(s) | Change | Blast Radius |
|------|---------|--------|--------------|
| `TranslatableText.kt:19-23` | Add inline annotation metadata (e.g., `annotations: List<InlineAnnotation>?`) | **Major** -- Serializable, used across the pipeline |
| `ArticleViewModel.kt:654-665` | Extract `LinearTextAnnotation` list alongside text; convert to markdown/tagged format | **Moderate** -- private function, no external callers |
| `OpenAICompatibleClient.kt:520-597` | Modify `buildTranslationPrompt()` to include formatting markers + instructions | **Major** -- changes AI prompt format |
| `AnthropicClient.kt:447-524` | Same as above (identical code) | **Major** |
| `OpenAICompatibleClient.kt:612-702` | Modify `parseTranslationResponse()` to parse formatted translation text | **Major** -- changes parsing logic |
| `AnthropicClient.kt:539-629` | Same as above (identical code) | **Major** |
| `LinearArticleContent.kt:912-923` | Render translated text as `AnnotatedString` instead of plain `Text()` | **Moderate** -- UI change |

**Key annotation types to preserve** (from `LinearTextAnnotation.kt:18-54`):

| Annotation | Data class | Has attribute? |
|-----------|-----------|----------------|
| Bold | `LinearTextAnnotationBold` | No |
| Italic | `LinearTextAnnotationItalic` | No |
| Link | `LinearTextAnnotationLink(href)` | Yes: `href: String` |
| Code | `LinearTextAnnotationCode` | No |
| Underline | `LinearTextAnnotationUnderline` | No |
| Strikethrough | `LinearTextAnnotationStrikethrough` | No |
| Superscript | `LinearTextAnnotationSuperscript` | No |
| Subscript | `LinearTextAnnotationSubscript` | No |
| Monospace | `LinearTextAnnotationMonospace` | No |
| Font | `LinearTextAnnotationFont(face)` | Yes: `face: String` |

**Rendering pipeline for original text** (`LinearArticleContent.kt:1257-1382`):
- `LinearText.toAnnotatedString()` processes every annotation type and builds an `AnnotatedString` with proper `SpanStyle`, `LinkAnnotation`, etc.
- Translation rendering (`LinearArticleContent.kt:913-922`) just does `Text(text = translation)` -- no annotations at all.

**Strategy decision:** The requirements suggest Option A (markdown markers). This means:
- Extraction: Convert `LinearTextAnnotation` ranges to inline markdown markers in the text string
- Prompt: Instruct AI to preserve markers
- Parsing: Parse markdown markers from translated text back into annotation ranges
- Rendering: Build `AnnotatedString` from parsed annotations (similar to `toAnnotatedString()`)

### Problem 2: Inline Images Within Paragraphs

**Root cause:** `ArticleViewModel.kt:689` -- `else -> {}` silently skips `LinearImage` elements.

The `HtmlLinearizer` splits a `<p>` containing `<img>` into separate `LinearText` + `LinearImage` + `LinearText` siblings. These appear as separate top-level elements in the `LinearArticle.elements` list.

**Files that need to change:**

| File | Line(s) | Change | Blast Radius |
|------|---------|--------|--------------|
| `ArticleViewModel.kt:649-692` | Detect adjacent `LinearText` elements separated by `LinearImage` and merge them into a single translation unit (or add image placeholders) | **Moderate** -- logic change in extraction |
| `LinearArticleContent.kt:174-250` | Update `computeParagraphIndexRecursive` to account for merged text segments | **Moderate** |

**Consideration:** This is the trickiest problem. Options include:
- Pre-pass to merge adjacent text segments around inline images
- Add a placeholder marker (e.g., `[IMG]`) in the translation unit
- Keep them as separate translation units but provide "continuation" context

### Problem 3: Table Content Not Translated

**Root cause:** `ArticleViewModel.kt:689` -- `else -> {}` skips `LinearTable`.

`LinearTable` structure (from `LinearStuff.kt:112-235`):
- `LinearTable` has `cells: Map<Coordinate, LinearTableCellItem>`
- Each `LinearTableCellItem` has `content: List<LinearElement>` which can contain `LinearText`
- `cellAt(row, col)` accessor exists
- `isFiller` flag identifies spanned cells (colSpan/rowSpan fillers)

**Files that need to change:**

| File | Line(s) | Change | Blast Radius |
|------|---------|--------|--------------|
| `TranslatableText.kt:64-91` | Add `TABLE_CELL` to `ElementType` enum | **Minor** -- additive change |
| `ArticleViewModel.kt:649-692` | Add `is LinearTable ->` branch to recurse into cells | **Moderate** |
| `LinearArticleContent.kt:174-250` | Add `is LinearTable ->` branch to `computeParagraphIndexRecursive` | **Moderate** |
| `LinearArticleContent.kt:1150-1241` | Pass `translation` + `translatedParagraphs` into `LinearTableContent`, then into cell rendering | **Moderate** -- currently `LinearTableContent` does not accept translation params |

**Rendering concern:** `LinearTableContent` (`LinearArticleContent.kt:1150-1241`) currently calls `LinearElementContent` for each cell element without passing any translation data:
```kotlin
// LinearArticleContent.kt:1228-1234
for (element in it.content) {
    LinearElementContent(
        linearElement = element,
        allowHorizontalScroll = false,
        onLinkClick = onLinkClick,
        modifier = Modifier.fillMaxWidth(),
        idToIndex = idToIndex,
        // NO translation params passed here
    )
}
```

### Problem 4: Deeply Nested List Translation Index Mismatch

**Root cause:** `LinearArticleContent.kt:202-211` and `LinearArticleContent.kt:230-239`

In `computeParagraphIndexRecursive()`, nested `LinearListItem` elements are handled with:
```kotlin
is LinearListItem -> {
    val hasTranslatableText =
        nested.content
            .filterIsInstance<LinearText>()  // Only checks DIRECT children
            .filter { it.blockStyle == LinearTextBlockStyle.TEXT }
            .any { it.text.isNotBlank() }
    if (hasTranslatableText) {
        paragraphCounter.increment()  // Increments by 1 regardless
    }
}
```

But `extractTranslatableTextRecursively()` (`ArticleViewModel.kt:670-677`) does **true recursion**:
```kotlin
is LinearListItem -> {
    extractTranslatableTextRecursively(
        elements = element.content,    // Recurses into ALL nested content
        translatableTexts = translatableTexts,
        nestingLevel = nestingLevel + 1,
    )
}
```

**Example of the bug:**
```
LinearListItem (L1)
  LinearText "A"            -- extraction: index 0
  LinearListItem (L2)
    LinearText "B"          -- extraction: index 1
    LinearListItem (L3)
      LinearText "C"        -- extraction: index 2
```

Extraction produces 3 `TranslatableText` items (A, B, C).
Index mapping for L1: sees L2 as having `hasTranslatableText` = true (B is a direct child), increments by 1. But L3 is nested inside L2, so L3's text "C" is never counted. The counter advances by 2 instead of 3.

**The same bug exists in three places:**
1. `computeParagraphIndexRecursive` for `LinearListItem` -- `LinearArticleContent.kt:195-215`
2. `computeParagraphIndexRecursive` for `LinearBlockQuote` -- `LinearArticleContent.kt:221-243`
3. `computeChildTranslationIndices` -- `LinearArticleContent.kt:675-697`
4. `computeBlockQuoteContentTranslationIndices` -- `LinearArticleContent.kt:724-746`

All four use `filterIsInstance<LinearText>()` on direct children only.

**Files that need to change:**

| File | Line(s) | Change | Blast Radius |
|------|---------|--------|--------------|
| `LinearArticleContent.kt:174-250` | Make `computeParagraphIndexRecursive` truly recursive for nested `LinearListItem` | **High** -- affects all translation index mapping |
| `LinearArticleContent.kt:658-697` | Make `computeChildTranslationIndices` truly recursive | **High** |
| `LinearArticleContent.kt:707-746` | Make `computeBlockQuoteContentTranslationIndices` truly recursive | **High** |

### Problem 5: Image Captions Not Translated

**Root cause:** `ArticleViewModel.kt:689` -- `else -> {}` skips `LinearImage`.

`LinearImage` (`LinearStuff.kt:342-347`) has `caption: LinearText?`. This is rendered at `LinearArticleContent.kt:851-864`:

```kotlin
linearImage.caption?.let { caption ->
    ProvideTextStyle(...) {
        LinearTextContent(
            linearText = caption,
            idToIndex = idToIndex,
            onLinkClick = onLinkClick,
            // NO translation param
        )
    }
}
```

**Files that need to change:**

| File | Line(s) | Change | Blast Radius |
|------|---------|--------|--------------|
| `TranslatableText.kt:64-91` | Add `IMAGE_CAPTION` to `ElementType` enum | **Minor** -- additive |
| `ArticleViewModel.kt:649-692` | Add `is LinearImage ->` branch to extract `caption?.text` | **Minor** |
| `LinearArticleContent.kt:174-250` | Add `is LinearImage ->` to `computeParagraphIndexRecursive` to count captions | **Minor** |
| `LinearArticleContent.kt:748-866` | Pass `translation` to `LinearImageContent`, then to caption's `LinearTextContent` | **Moderate** |

---

## Code Standards

### Current Standards

| Type | Tool | Config File |
|------|------|-------------|
| Formatter | ktlint (via spotless) | `build.gradle` |
| Build | Gradle + Kotlin DSL | `build.gradle.kts` (root), `app/build.gradle` |
| DI Framework | Kodein | N/A (convention-based) |

### Conventions Observed

- **Naming:** camelCase for functions/properties, PascalCase for classes/composables. Composable functions follow Compose naming (`LinearTextContent`, `LinearListItemContent`).
- **Files:** One major composable per section in `LinearArticleContent.kt` (monolithic file, ~1400 lines). Data models in dedicated files (`LinearStuff.kt`, `TranslatableText.kt`).
- **Imports:** Fully qualified names used inline in `ArticleViewModel.kt:651-686` (e.g., `com.nononsenseapps.feeder.model.html.LinearText`) instead of top-level imports. This is an inconsistency -- other files use normal imports.
- **Sealed interfaces:** Used for `LinearElement`, `TranslationState`, `AISummaryState`, `ParagraphTranslationProgress`. Consistent pattern.
- **Serialization:** `@Serializable` from kotlinx.serialization on `TranslatableText` and `ElementType`.

### Compliance Notes

- The fully-qualified inline references in `extractTranslatableTextRecursively` are unusual and should probably be converted to regular imports for consistency.
- `LinearArticleContent.kt` is very large (~1400 lines). Adding table translation rendering will grow it further. Consider extracting table rendering into its own file if the change is substantial.

---

## Dependencies

### Relevant Dependencies (from AI translation pipeline)

| Package | Role | Status |
|---------|------|--------|
| kotlinx.serialization | JSON serialization for `TranslatableText` | OK |
| openai-java SDK 4.13.0 | OpenAI API client | OK |
| anthropic-java SDK 2.11.1 | Anthropic API client | OK |
| Jetpack Compose | UI rendering | OK |
| kotlinx.coroutines | Async / Flow / Semaphore | OK |

No dependency changes needed for this spec.

---

## Framework Patterns

### Identified Patterns

| Pattern | Location | Example |
|---------|----------|---------|
| Sealed interface hierarchy | `LinearElement`, `TranslationState`, `ParagraphTranslationProgress` | Element types dispatched via `when` |
| Flow-based state | `MutableStateFlow<TranslationState>` in ViewModel | `translationState.update { ... }` |
| Composable per element type | `LinearTextContent`, `LinearListItemContent`, etc. | Each element type has its own composable |
| Index mapping via pre-computation | `computeParagraphIndices()` runs before rendering | Returns `Map<Int, Int?>` for lookup |
| Recursive traversal | `extractTranslatableTextRecursively()` | DFS over `LinearElement` tree |
| Per-paragraph parallel translation | `ParagraphTranslationCoordinator` | Semaphore(3) + channelFlow |

### Patterns to Follow

1. **`when` exhaustive dispatch** on `LinearElement` -- any new element handling must cover all sealed subtypes
2. **Composable parameter threading** -- translation data must be passed down through the composable hierarchy (see how `translatedParagraphs` and `parentTranslationIndex` are threaded through `linearArticleContent` -> `LinearElementContent` -> `LinearListItemContent`)
3. **Counter parity** -- extraction count MUST equal index mapping count. Any change to extraction MUST have a corresponding change to index mapping.

---

## Code Duplication Analysis

### Critical: Identical Code in Both AI Clients

The following functions are **character-for-character identical** between `OpenAICompatibleClient.kt` and `AnthropicClient.kt`:

| Function | OpenAI Lines | Anthropic Lines | LOC |
|----------|-------------|-----------------|-----|
| `buildTranslationPrompt()` | 520-597 | 447-524 | 78 |
| `parseTranslationResponse()` | 612-702 | 539-629 | 91 |
| `extractJsonFromResponse()` | 707-723 | 634-650 | 17 |
| `jsonEscape()` | 728-735 | 655-662 | 8 |
| `unescapeJson()` | 739-745 | 666-672 | 7 |
| `handleTranslationError()` | 750-768 | 677-695 | 19 |
| `buildSummaryPrompt()` | 103-197 | 53-147 | 95 |
| `parseSummaryJsonResponse()` | 374-435 | 301-362 | 62 |
| `extractJsonFromMarkdown()` | 440-455 | 367-382 | 16 |
| `parseLegacySummaryResponse()` | 462-499 | 389-426 | 38 |
| **Total** | | | **~431** |

**Impact on spec-32:** Every prompt/parsing change for the 5 problems must be made in both files. This is the **highest-risk duplication** for this spec.

**Recommendation:** Before implementing spec-32 changes, extract at minimum `buildTranslationPrompt()`, `parseTranslationResponse()`, `jsonEscape()`, `unescapeJson()`, and `extractJsonFromResponse()` into a shared `TranslationPromptBuilder` utility. This reduces the blast radius of prompt changes from 2 files to 1.

### Moderate: Parallel Index Mapping Functions

Three functions in `LinearArticleContent.kt` perform the same "count translatable texts" logic:
1. `computeParagraphIndexRecursive()` (lines 174-250)
2. `computeChildTranslationIndices()` (lines 658-697)
3. `computeBlockQuoteContentTranslationIndices()` (lines 707-746)

Functions 2 and 3 are also nearly identical to each other. Unifying these would fix the nesting bug in all places simultaneously.

---

## Test Coverage

### Current Tests

| Test File | Tests | What's Covered |
|-----------|-------|----------------|
| `TranslatableTextTest.kt` | 10 tests | Data class creation, `getStructureDescription()`, `withStructurePrefix()`, `fromPlainText()` |
| `ArticleTranslationTest.kt` | ~8 tests | `ArticleTranslation` data class operations |
| `ParagraphTranslationCoordinatorTest.kt` | ~15 tests | Coordinator flow, retry logic, concurrency |
| `HtmlLinearizerTest.kt` | ~50+ tests | HTML -> LinearElement conversion |

### Missing Test Coverage (Critical for Spec-32)

| Area | Risk | Recommendation |
|------|------|----------------|
| `extractTranslatableTextRecursively()` | **High** -- private method on ViewModel, untestable | Extract to standalone function, add unit tests for all element types |
| `computeParagraphIndexRecursive()` | **High** -- private function, bug confirmed here | Extract to testable function, add tests for nested lists |
| `computeChildTranslationIndices()` | **High** -- private function, same bug | Test via parent or extract |
| `buildTranslationPrompt()` | **Medium** -- private in both clients | Extract to shared utility, test prompt format |
| `parseTranslationResponse()` | **Medium** -- private in both clients | Extract to shared utility, test parsing (especially with markdown markers) |
| Translation rendering (AnnotatedString from markers) | **High** -- new code, no existing pattern | Write unit tests for markdown -> AnnotatedString conversion |

---

## Better Options / Technical Debt

### Potential Improvements

| Area | Current | Better Option | Effort | Impact |
|------|---------|---------------|--------|--------|
| AI client duplication | 431 LOC duplicated across 2 files | Shared `TranslationPromptBuilder` utility | Medium | High |
| Extraction testability | Private method on ViewModel | Standalone function / extension on `LinearArticle` | Low | High |
| Index mapping fragility | 3 separate counter functions that must stay in sync | Single recursive counter or shared counting function | Medium | High |
| `LinearArticleContent.kt` size | ~1400 lines, monolithic | Split into separate files per element type | Low | Low |
| Inline FQ references | `com.nononsenseapps.feeder.model.html.LinearText` used inline | Normal top-level imports | Trivial | Low |

### Technical Debt

| Issue | Location | Severity | Fix Effort |
|-------|----------|----------|------------|
| Duplicated AI client code | `OpenAICompatibleClient.kt`, `AnthropicClient.kt` | High | Medium (extract shared utility) |
| Nested list index bug | `LinearArticleContent.kt:202-211` | High | Low (make recursive) |
| No tests for extraction | `ArticleViewModel.kt:616-692` | Medium | Medium (extract + test) |
| Plain text translation rendering | `LinearArticleContent.kt:913-922` | High | Medium (parse markdown -> AnnotatedString) |

---

## Summary

### Must Follow (for spec-32 implementation)

1. **Counter parity rule** -- Every change to `extractTranslatableTextRecursively()` MUST have a corresponding change to `computeParagraphIndexRecursive()`, `computeChildTranslationIndices()`, and `computeBlockQuoteContentTranslationIndices()`.
2. **Dual-client sync rule** -- Every change to `buildTranslationPrompt()` or `parseTranslationResponse()` must be applied to BOTH `OpenAICompatibleClient.kt` and `AnthropicClient.kt` (unless duplication is refactored first).
3. **`TranslatableText` is `@Serializable`** -- Any new fields must have defaults for backward compatibility.
4. **`AIClient.translate()` returns `List<String>`** -- The response type is currently plain strings. If inline formatting is preserved via markdown markers, the parsing of markers back to `AnnotatedString` happens on the rendering side, not in the AI client.

### Should Consider (before starting implementation)

1. **Refactor AI client duplication first** -- Create shared utility to eliminate the dual-maintenance burden. This reduces risk for all 5 problem fixes.
2. **Extract and test extraction logic first** -- Move `extractTranslatableTextRecursively()` out of the ViewModel so it's testable, then add tests. This creates a safety net before making the extraction changes.
3. **Fix the nesting bug independently** -- Problem 4 is a standalone bug fix that can be done and tested in isolation before the larger formatting changes.

### Future Work

1. **Richer translation format** -- If markdown markers prove unreliable (e.g., AI strips or mangles them), consider Option B (XML tags) or a hybrid approach.
2. **Translation caching** -- Consider persisting translated text with its annotation data to avoid re-translation.
3. **Batch table cells** -- Small table cells could be batched into a single translation unit to reduce API calls.

---

## Files Examined

| File | Purpose | Lines |
|------|---------|-------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableText.kt` | Translation unit data model + ElementType enum | 92 |
| `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` | AIClient interface + TranslationResult types | 152 |
| `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt` | Per-paragraph parallel translation coordinator | 130 |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` | OpenAI-compatible client: prompt building + parsing | 775 |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` | Anthropic client: prompt building + parsing | 702 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | ViewModel: extraction + translation orchestration | 856 |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt` | Rendering: composables + index mapping + AnnotatedString | ~1400 |
| `app/src/main/java/com/nononsenseapps/feeder/model/html/LinearStuff.kt` | LinearElement hierarchy: LinearText, LinearImage, LinearTable, etc. | 429 |
| `app/src/main/java/com/nononsenseapps/feeder/model/html/LinearTextAnnotation.kt` | Inline annotation types (Bold, Italic, Link, Code, etc.) | 55 |
| `app/src/test/java/com/nononsenseapps/feeder/ai/TranslatableTextTest.kt` | Unit tests for TranslatableText | 161 |
| `specification/032-improve-text-extraction/01-requirements.md` | Requirements document | 461 |
