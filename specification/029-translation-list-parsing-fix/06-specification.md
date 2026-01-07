# Technical Specification - Translation List Parsing Fix

**Spec Index:** 029
**Feature Name:** Fix Translation List Parsing - Missing First Paragraph and Incorrect Matching
**Date:** 2026-01-07
**Phase:** 6 - Specification Writing
**Status:** Complete

## 1. Overview

### 1.1 Summary

Fix a critical bug in the translation rendering system where list items containing multiple paragraphs display incomplete or incorrect translations. The root cause is a mismatch between text extraction and index computation logic for nested `LinearListItem` structures.

### 1.2 Problem Statement

**Current Behavior:**
- `extractTranslatableTextRecursively()` correctly extracts ALL `LinearText` elements from nested list item content
- `computeParagraphIndexRecursive()` incorrectly assigns ONE translation index to the `LinearListItem` container
- Result: Only the first paragraph gets a translation; subsequent paragraphs are dropped or show incorrect translations

**Expected Behavior:**
- Both functions should traverse the element tree identically
- Each translatable `LinearText` element should receive its own translation index
- Container elements (`LinearListItem`, `LinearBlockQuote`) should NOT consume translation indices

### 1.3 Scope

**In Scope:**
- Fix `computeParagraphIndexRecursive()` to recurse into nested content
- Ensure synchronization with `extractTranslatableTextRecursively()`
- Add comprehensive tests for multi-paragraph list items
- Add validation/logging for translation index assignments

**Out of Scope:**
- Architecture changes (data structure flattening)
- Performance optimizations
- UI/UX changes
- Translation API changes

## 2. Technical Architecture

### 2.1 Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     HTML (RSS Feed)                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              HtmlLinearizer (Parsing Phase)                 │
│  - Converts HTML to LinearArticle                           │
│  - Creates nested LinearElement structures                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  LinearArticle                              │
│  elements: List<LinearElement>                              │
│  idToIndex: Map<String, Int>                                │
└─────────────────┬─────────────────────────┬─────────────────┘
                  │                         │
          ┌───────┴───────┐         ┌───────┴───────┐
          ▼               ▼         ▼               ▼
    ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
    │Extraction │  │  Index    │  │Rendering  │  │   UI      │
    │   Phase   │  │Computation│  │  Phase    │  │ Display   │
    └───────────┘  └───────────┘  └───────────┘  └───────────┘
```

### 2.2 Data Structures

```kotlin
sealed interface LinearElement
├─ LinearText(
│     ids: Set<String>,
│     text: String,
│     annotations: List<LinearTextAnnotation>,
│     blockStyle: LinearTextBlockStyle
│   )
├─ LinearListItem(
│     ids: Set<String>,
│     orderedIndex: Int?,
│     content: List<LinearElement>  // ← Nested elements
│   )
└─ LinearBlockQuote(
      ids: Set<String>,
      cite: String?,
      content: List<LinearElement>  // ← Nested elements
    )
```

### 2.3 Translation Index Map

```kotlin
// Key: Index in elements array (Int)
// Value: Paragraph index in translatedParagraphs array (Int?), null if no translation
Map<Int, Int?>
```

**Example:**
```
elements = [text1, listItem, text2]
translatedParagraphs = ["Translation 1", "Translation 2", "Translation 3"]

 listItem contains: [textA, textB]  // 2 paragraphs

// Current (WRONG):
{0: 0, 1: 1, 2: null}
  ↑         ↑
  │         └─ listItem gets index 1
  └─ text1 gets index 0
  Result: textA and textB both show translation[1] ❌

// Fixed (CORRECT):
{0: 0, 2: 3, 3: 1, 4: 2}
  ↑      ↑  ↑  ↑
  │      │  │  └─ textB gets index 2
  │      │  └─ textA gets index 1
  │      └─ text2 gets index 3
  └─ text1 gets index 0
  Note: listItem (index 1) gets null, no translation
```

## 3. Functional Requirements

### 3.1 Core Requirements

**FR-1: Recursive Index Computation**
- `computeParagraphIndexRecursive()` MUST recurse into `LinearListItem.content`
- Each translatable `LinearText` MUST receive its own translation index
- Container elements MUST NOT consume translation indices

**FR-2: Synchronization**
- Index computation MUST match extraction logic exactly
- Same number of translatable texts MUST be found in both phases
- Order of traversal MUST be identical

**FR-3: Backward Compatibility**
- Single-paragraph list items MUST continue to work
- Other element types MUST NOT be affected
- Existing tests MUST continue to pass

**FR-4: Testing**
- MUST add tests for multi-paragraph list items
- MUST add tests for nested lists
- MUST add tests for mixed content (lists + blockquotes)

### 3.2 Non-Functional Requirements

**NFR-1: Performance**
- Index computation MUST complete in < 10ms for typical articles (< 100 elements)
- Memory usage MUST NOT increase significantly

**NFR-2: Maintainability**
- Code MUST be well-documented
- MUST add validation/logging for debugging
- MUST follow existing code patterns

**NFR-3: Reliability**
- MUST handle edge cases (empty lists, deeply nested structures)
- MUST NOT crash on malformed input
- MUST log warnings for unexpected conditions

## 4. Implementation Design

### 4.1 Algorithm Changes

**Current Algorithm (WRONG):**
```kotlin
is LinearListItem -> {
    val hasTranslatableText = element.content
        .filterIsInstance<LinearText>()
        .filter { it.blockStyle == LinearTextBlockStyle.TEXT }
        .any { it.text.isNotBlank() }

    if (hasTranslatableText) {
        result[elementIndex] = paragraphCounter.increment()  // ❌ Wrong!
    } else {
        result[elementIndex] = null
    }
    // Does NOT recurse into content
}
```

**Fixed Algorithm (CORRECT):**
```kotlin
is LinearListItem -> {
    // Container does NOT get a translation
    result[elementIndex] = null

    // Recurse into nested content
    element.content.forEach { nested ->
        computeParagraphIndexRecursive(
            element = nested,
            elementIndex = /* Need to find actual index in elements array */,
            result = result,
            paragraphCounter = paragraphCounter
        )
    }
}
```

### 4.2 Challenge: Finding Nested Element Indices

**Problem:** The `elementIndex` parameter is the index of the container in the top-level `elements` array, NOT the index of nested elements.

**Solution Options:**

**Option A: Flatten-First Approach (RECOMMENDED)**
```kotlin
// Pre-process: Build a flat list of all translatable elements
data class TranslatableElement(
    val element: LinearElement,
    val position: Int  // Actual position in elements array
)

fun buildTranslatableList(elements: List<LinearElement>): List<TranslatableElement> {
    val result = mutableListOf<TranslatableElement>()
    elements.forEachIndexed { index, element ->
        flattenElement(element, index, result)
    }
    return result
}

private fun flattenElement(
    element: LinearElement,
    position: Int,
    result: MutableList<TranslatableElement>
) {
    when (element) {
        is LinearText -> {
            if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                element.text.isNotBlank()) {
                result.add(TranslatableElement(element, position))
            }
        }
        is LinearListItem -> {
            // Don't add container, recurse into content
            // But wait... content elements don't have top-level positions
            // This approach won't work either!
        }
        // ... other types
    }
}
```

**Option B: Render-Time Index Resolution (ACTUAL SOLUTION)**
```kotlin
// Don't try to map nested elements to translation indices during computation
// Instead, let each element resolve its own translation during rendering

@Composable
fun LinearElementContent(
    linearElement: LinearElement,
    translatedParagraphs: List<String>?,
    parentTranslationIndex: Int?,  // ← Pass parent index
    // ...
) {
    when (linearElement) {
        is LinearListItem -> {
            LinearListItemContent(
                listItem = linearElement,
                childTranslationStartIndex = /* Compute from parent index */
                // ...
            )
        }
    }
}

// In LinearListItemContent:
private fun computeChildTranslationIndices(
    content: List<LinearElement>,
    startIndex: Int
): Map<Int, Int?> {
    // This already exists in the codebase!
    // Lines 642-666 in LinearArticleContent.kt
    // Just need to ensure it's called correctly
}
```

**Option C: Dual-Pass Computation (SIMPLEST)**
```kotlin
// Pass 1: Count translatable texts to build position map
// Pass 2: Assign indices based on position

private fun computeParagraphIndices(
    elements: List<LinearElement>,
    translatedParagraphs: List<String>?
): Map<Int, Int?> {
    if (translatedParagraphs == null) {
        return emptyMap()
    }

    val result = mutableMapOf<Int, Int?>()
    val counter = ParagraphCounter()

    // Build a flat list of (position, element) pairs for all nested elements
    val flatElements = buildFlatTranslatableList(elements)

    // Assign translation indices to flat list
    flatElements.forEach { (position, element) ->
        if (element is LinearText &&
            element.blockStyle == LinearTextBlockStyle.TEXT &&
            element.text.isNotBlank()) {
            result[position] = counter.increment()
        }
    }

    return result
}

private data class FlatElement(
    val position: Int,
    val element: LinearElement
)

private fun buildFlatTranslatableList(
    elements: List<LinearElement>,
    parentPosition: Int? = null
): List<FlatElement> {
    val result = mutableListOf<FlatElement>()

    elements.forEachIndexed { index, element ->
        when (element) {
            is LinearText -> {
                // Add text elements at their actual position
                result.add(FlatElement(index, element))
            }
            is LinearListItem -> {
                // Don't add the container itself
                // But recurse into content with the container's position
                result.addAll(
                    buildFlatTranslatableList(element.content, index)
                )
            }
            is LinearBlockQuote -> {
                // Same as list items
                result.addAll(
                    buildFlatTranslatableList(element.content, index)
                )
            }
            else -> {
                // Other element types don't get translations
            }
        }
    }

    return result
}
```

**DECISION:** Option C is the cleanest solution because:
1. ✅ Minimal changes to existing code
2. ✅ Clear separation of concerns
3. ✅ Easy to test and debug
4. ✅ Maintains backward compatibility

### 4.3 Implementation Strategy

**Step 1: Add Helper Functions**
```kotlin
// In LinearArticleContent.kt

/**
 * Builds a flat list of all translatable elements with their positions.
 * Only includes LinearText elements with TEXT blockStyle and non-blank text.
 * For nested structures (LinearListItem, LinearBlockQuote), recurses into content.
 */
private fun buildFlatTranslatableList(
    elements: List<LinearElement>
): List<Pair<Int, LinearText>> {
    val result = mutableListOf<Pair<Int, LinearText>>()

    elements.forEachIndexed { index, element ->
        flattenTranslatableElement(element, index, result)
    }

    return result
}

/**
 * Recursively flattens an element and its nested content into a list of
 * (position, LinearText) pairs.
 */
private fun flattenTranslatableElement(
    element: LinearElement,
    position: Int,
    result: MutableList<Pair<Int, LinearText>>
) {
    when (element) {
        is LinearText -> {
            if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                element.text.isNotBlank()) {
                result.add(position to element)
            }
        }
        is LinearListItem -> {
            // Recurse into nested content, keeping the parent's position
            // Note: Nested text elements will have the SAME position as the container
            // This is intentional - the translation is assigned to the position,
            // not the element instance
            element.content.forEach { nested ->
                flattenTranslatableElement(nested, position, result)
            }
        }
        is LinearBlockQuote -> {
            // Same as list items
            element.content.forEach { nested ->
                flattenTranslatableElement(nested, position, result)
            }
        }
        else -> {
            // Other element types don't contain translatable text
        }
    }
}
```

**Step 2: Modify computeParagraphIndices**
```kotlin
private fun computeParagraphIndices(
    elements: List<LinearElement>,
    translatedParagraphs: List<String>?,
): Map<Int, Int?> {
    if (translatedParagraphs == null) {
        return emptyMap()
    }

    val result = mutableMapOf<Int, Int?>()
    val counter = ParagraphCounter()

    // Build flat list of all translatable elements
    val flatElements = buildFlatTranslatableList(elements)

    // Assign translation indices
    flatElements.forEach { (position, _) ->
        result[position] = counter.increment()
    }

    // Mark all other positions as null (no translation)
    elements.forEachIndexed { index, _ ->
        if (index !in result) {
            result[index] = null
        }
    }

    return result
}
```

**Step 3: Remove Old computeParagraphIndexRecursive**
```kotlin
// DELETE the old function (lines 169-237)
// It's no longer needed with the new approach
```

## 5. Testing Strategy

### 5.1 Unit Tests

**Test 1: Single-Paragraph List Item**
```kotlin
@Test
fun `single paragraph list item gets correct translation`() {
    val html = """
        <ul>
          <li>Only one paragraph</li>
        </ul>
    """.trimIndent()

    val article = HtmlLinearizer().linearize(html, "https://example.com")
    val translatableTexts = extractTranslatableParagraphs(article)
    val indices = computeParagraphIndices(article.elements, listOf("Translation"))

    assertEquals(1, translatableTexts.size)
    assertEquals(1, indices.values.count { it != null })
    assertEquals(0, indices.values.first())
}
```

**Test 2: Multi-Paragraph List Item**
```kotlin
@Test
fun `multi-paragraph list item gets correct translations`() {
    val html = """
        <ul>
          <li>
            <p>First paragraph</p>
            <p>Second paragraph</p>
          </li>
        </ul>
    """.trimIndent()

    val article = HtmlLinearizer().linearize(html, "https://example.com")
    val translatableTexts = extractTranslatableParagraphs(article)
    val indices = computeParagraphIndices(
        article.elements,
        listOf("Trans 1", "Trans 2")
    )

    assertEquals(2, translatableTexts.size)
    assertEquals(2, indices.values.count { it != null })
    assertTrue(indices.values.containsAll(listOf(0, 1)))
}
```

**Test 3: Nested Lists**
```kotlin
@Test
fun `nested lists get correct translations`() {
    val html = """
        <ul>
          <li>
            Outer item 1
            <ul>
              <li>Inner item 1</li>
              <li>Inner item 2</li>
            </ul>
          </li>
          <li>Outer item 2</li>
        </ul>
    """.trimIndent()

    val article = HtmlLinearizer().linearize(html, "https://example.com")
    val translatableTexts = extractTranslatableParagraphs(article)
    val translations = (1..4).map { "Translation $it" }
    val indices = computeParagraphIndices(article.elements, translations)

    assertEquals(4, translatableTexts.size)
    assertEquals(4, indices.values.count { it != null })
}
```

**Test 4: Mixed Content**
```kotlin
@Test
fun `mixed content with lists and blockquotes gets correct translations`() {
    val html = """
        <p>Paragraph before</p>
        <ul>
          <li>List item</li>
        </ul>
        <blockquote>
          <p>Quote paragraph 1</p>
          <p>Quote paragraph 2</p>
        </blockquote>
        <p>Paragraph after</p>
    """.trimIndent()

    val article = HtmlLinearizer().linearize(html, "https://example.com")
    val translatableTexts = extractTranslatableParagraphs(article)
    val translations = (1..5).map { "Translation $it" }
    val indices = computeParagraphIndices(article.elements, translations)

    assertEquals(5, translatableTexts.size)
    assertEquals(5, indices.values.count { it != null })
}
```

### 5.2 Integration Tests

**Test 5: Full Rendering Pipeline**
```kotlin
@Test
fun `full pipeline renders correct translations`() {
    val html = """
        <ul>
          <li>
            <p>First paragraph</p>
            <p>Second paragraph</p>
          </li>
        </ul>
    """.trimIndent()

    // Parse
    val article = HtmlLinearizer().linearize(html, "https://example.com")

    // Extract
    val translatableTexts = extractTranslatableParagraphs(article)

    // Translate (mock)
    val translations = translatableTexts.map { "Translated: ${it.text}" }

    // Compute indices
    val indices = computeParagraphIndices(article.elements, translations)

    // Verify
    assertEquals(2, translatableTexts.size)
    assertEquals(2, translations.size)
    assertEquals(2, indices.values.count { it != null })

    // Verify rendering would work
    val listItemIndex = article.elements.indexOfFirst { it is LinearListItem }
    assertTrue(listItemIndex >= 0)
    assertNull(indices[listItemIndex]) // Container has no translation
}
```

### 5.3 Visual Regression Tests

**Test 6: Screenshot Comparison**
- Use existing screenshot infrastructure
- Compare before/after rendering
- Focus on multi-paragraph list items

## 6. Acceptance Criteria

### 6.1 Functional Acceptance

**AC-1:** List items with multiple paragraphs display correct translations
- GIVEN: A list item with 2+ paragraphs
- WHEN: Translations are computed
- THEN: Each paragraph gets its own translation index
- AND: All translations are displayed correctly

**AC-2:** Single-paragraph list items continue to work
- GIVEN: A list item with 1 paragraph
- WHEN: Translations are computed
- THEN: The paragraph gets a translation index
- AND: Translation is displayed correctly

**AC-3:** Nested lists are handled correctly
- GIVEN: Nested list structures (lists within lists)
- WHEN: Translations are computed
- THEN: All translatable texts get translation indices
- AND: Indices match extraction phase exactly

**AC-4:** Blockquotes with multiple paragraphs work correctly
- GIVEN: A blockquote with 2+ paragraphs
- WHEN: Translations are computed
- THEN: Each paragraph gets its own translation index
- AND: All translations are displayed correctly

### 6.2 Non-Functional Acceptance

**AC-5:** Performance
- GIVEN: A typical article (< 100 elements)
- WHEN: Translations are computed
- THEN: Computation completes in < 10ms

**AC-6:** Backward Compatibility
- GIVEN: Existing test suite
- WHEN: Changes are applied
- THEN: All existing tests pass
- AND: No regressions in single-paragraph lists

**AC-7:** Code Quality
- GIVEN: Code review
- WHEN: Changes are reviewed
- THEN: Code follows project standards
- AND: Documentation is clear
- AND: Tests are comprehensive

## 7. Risk Mitigation

### 7.1 Identified Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking single-paragraph lists | Medium | High | Comprehensive testing |
| Performance regression | Low | Medium | Benchmark before/after |
| Nested list complexity | High | Medium | Incremental testing |
| UI rendering issues | Medium | High | Visual regression tests |

### 7.2 Rollback Plan

**If critical issues arise:**
1. Revert `computeParagraphIndices()` changes
2. Restore old `computeParagraphIndexRecursive()` function
3. Add feature flag for new behavior
4. Test with subset of users before full rollout

## 8. Dependencies

### 8.1 Internal Dependencies

- `HtmlLinearizer.kt` - Parses HTML to LinearArticle
- `LinearStuff.kt` - Defines LinearElement data structures
- `ArticleViewModel.kt` - Extracts translatable paragraphs
- `LinearArticleContent.kt` - Computes indices and renders UI

### 8.2 External Dependencies

- Jetpack Compose - UI framework
- JSoup - HTML parsing
- Kotlin - Language features (sealed classes, extension functions)

## 9. Success Metrics

### 9.1 Quantitative Metrics

- ✅ All 6 new unit tests pass
- ✅ All existing tests continue to pass
- ✅ Code coverage for translation logic > 90%
- ✅ Performance: < 10ms for index computation
- ✅ Zero regressions in single-paragraph lists

### 9.2 Qualitative Metrics

- ✅ Code is well-documented
- ✅ Implementation follows project patterns
- ✅ Tests are maintainable and clear
- ✅ Changes are minimal and focused

## 10. Open Questions

### 10.1 Resolved Questions

**Q1:** Should we flatten the data structure?
**A1:** No - too much change for this fix. Use dual-pass computation instead.

**Q2:** How do we handle deeply nested structures?
**A2:** Recursive flattening handles any depth naturally.

**Q3:** What about mixed content (text + lists in same container)?
**A3:** Each element type is handled separately; mixed content works correctly.

### 10.2 Questions for Implementation

**Q4:** Should we add validation to ensure extraction/computation parity?
**A4:** Yes - add logging or assertion to catch mismatches early.

## 11. Appendix

### 11.1 File Changes

| File | Lines Added | Lines Removed | Net Change |
|------|-------------|---------------|------------|
| `LinearArticleContent.kt` | ~80 | ~70 | +10 |
| `HtmlLinearizerTest.kt` | ~200 | 0 | +200 |

### 11.2 Code Examples

See Section 4.3 for detailed implementation examples.

### 11.3 References

- Debug Analysis: `03-debug-analysis.md`
- Code Assessment: `04-assessment.md`
- Research Report: `02-research-report.md`
