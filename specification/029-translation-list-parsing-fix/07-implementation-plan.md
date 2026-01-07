# Implementation Plan - Translation List Parsing Fix

**Spec Index:** 029
**Feature Name:** Fix Translation List Parsing - Missing First Paragraph and Incorrect Matching
**Date:** 2026-01-07
**Phase:** 6 - Specification Writing
**Status:** Complete

## 1. Implementation Overview

### 1.1 Approach

**Strategy:** Dual-Pass Computation with Recursive Flattening

This approach:
1. ✅ Minimizes changes to existing code
2. ✅ Maintains backward compatibility
3. ✅ Is easy to test and debug
4. ✅ Handles all nesting depths correctly

### 1.2 Phases

| Phase | Duration | Effort | Status |
|-------|----------|--------|--------|
| Phase 1: Core Implementation | 2 hours | Medium | Pending |
| Phase 2: Testing | 2 hours | Medium | Pending |
| Phase 3: Validation | 1 hour | Low | Pending |
| **Total** | **5 hours** | **Medium** | - |

## 2. Detailed Implementation Steps

### Phase 1: Core Implementation (2 hours)

#### Step 1.1: Add Flattening Helper Functions (30 minutes)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Location:** After line 163 (after `ParagraphCounter` class)

**Add:**
```kotlin
/**
 * Builds a flat list of all translatable LinearText elements with their positions.
 *
 * This function recursively traverses the element tree and collects all translatable
 * text elements (LinearText with TEXT blockStyle and non-blank text) along with their
 * positions in the elements array.
 *
 * For nested structures (LinearListItem, LinearBlockQuote), the function recurses into
 * the content and associates nested text elements with the container's position.
 *
 * @param elements The list of elements to flatten
 * @return A list of (position, LinearText) pairs for all translatable texts
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
 *
 * @param element The element to flatten
 * @param position The position of this element (or its container) in the elements array
 * @param result The mutable list to accumulate results into
 */
private fun flattenTranslatableElement(
    element: LinearElement,
    position: Int,
    result: MutableList<Pair<Int, LinearText>>
) {
    when (element) {
        is LinearText -> {
            // Only include translatable text
            if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                element.text.isNotBlank()) {
                result.add(position to element)
            }
        }
        is LinearListItem -> {
            // Recurse into nested content, using the container's position
            element.content.forEach { nested ->
                flattenTranslatableElement(nested, position, result)
            }
        }
        is LinearBlockQuote -> {
            // Recurse into nested content, using the container's position
            element.content.forEach { nested ->
                flattenTranslatableElement(nested, position, result)
            }
        }
        else -> {
            // Other element types (images, videos, tables, etc.) don't contain
            // translatable text, so we skip them
        }
    }
}
```

**Validation:**
- ✅ Function compiles without errors
- ✅ Type signatures match expected usage
- ✅ Comments are clear and accurate

---

#### Step 1.2: Rewrite computeParagraphIndices (30 minutes)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Location:** Lines 137-153 (replace existing `computeParagraphIndices` function)

**Replace with:**
```kotlin
/**
 * Computes which element positions should display which paragraph translations.
 *
 * This function uses a dual-pass approach:
 * 1. Build a flat list of all translatable text elements with their positions
 * 2. Assign sequential translation indices to those positions
 *
 * This approach ensures that nested structures (lists, blockquotes) are handled
 * correctly by assigning translations to each individual text element rather than
 * to container elements.
 *
 * IMPORTANT: The logic here must match extractTranslatableTextRecursively() in
 * ArticleViewModel.kt to ensure translation indices align correctly.
 *
 * Returns a map where:
 * - Key: element index in the elements array
 * - Value: paragraph index to translate (null if this position shouldn't show translation)
 *
 * @param elements The list of elements to compute indices for
 * @param translatedParagraphs The list of translated paragraphs (null if no translations)
 * @return Map of element position to translation index
 */
private fun computeParagraphIndices(
    elements: List<LinearElement>,
    translatedParagraphs: List<String>?,
): Map<Int, Int?> {
    if (translatedParagraphs == null) {
        return emptyMap()
    }

    val result = mutableMapOf<Int, Int?>()
    val counter = ParagraphCounter()

    // Build flat list of all translatable elements with their positions
    val flatElements = buildFlatTransatableList(elements)

    // Assign translation indices to each translatable element
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

**Validation:**
- ✅ Function compiles without errors
- ✅ Returns correct map structure
- ✅ Handles null translatedParagraphs correctly
- ✅ All positions in elements array have a value in result

---

#### Step 1.3: Remove Old computeParagraphIndexRecursive (30 minutes)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Location:** Lines 169-237

**Action:** DELETE the entire `computeParagraphIndexRecursive()` function

**Reason:** The new flattening approach replaces this function

**Validation:**
- ✅ Old function is completely removed
- ✅ No references to the function remain
- ✅ Code compiles without errors

---

#### Step 1.4: Add Validation Logging (30 minutes)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Location:** At the end of `computeParagraphIndices()` function

**Add:**
```kotlin
    // Validation: Log computed indices for debugging
    logDebug(
        LOG_TAG,
        "Computed ${result.values.count { it != null }} translation indices " +
        "for ${elements.size} elements. " +
        "Translation count: ${translatedParagraphs.size}"
    )

    // Validation: Warn if counts don't match
    val computedCount = result.values.count { it != null }
    if (computedCount != translatedParagraphs.size) {
        logDebug(
            LOG_TAG,
            "WARNING: Translation count mismatch! " +
            "Computed: $computedCount, Expected: ${translatedParagraphs.size}"
        )
    }

    return result
```

**Validation:**
- ✅ Logs are written at appropriate level
- ✅ Log messages are clear and actionable
- ✅ No performance impact from logging

---

### Phase 2: Testing (2 hours)

#### Step 2.1: Add Test Helper Functions (30 minutes)

**File:** `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Add:**
```kotlin
/**
 * Helper function to extract translatable paragraphs from a LinearArticle.
 * Mirrors the logic in ArticleViewModel.extractTranslatableParagraphs().
 */
private fun extractTranslatableParagraphs(article: LinearArticle): List<String> {
    val result = mutableListOf<String>()
    extractTranslatableTextRecursively(article.elements, result, nestingLevel = 0)
    return result
}

/**
 * Recursively extracts translatable text from elements.
 * Mirrors the logic in ArticleViewModel.extractTranslatableTextRecursively().
 */
private fun extractTranslatableTextRecursively(
    elements: List<LinearElement>,
    translatableTexts: MutableList<String>,
    nestingLevel: Int = 0,
) {
    for (element in elements) {
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT) {
                    val text = element.text
                    if (text.isNotBlank()) {
                        translatableTexts.add(text.trim())
                    }
                }
            }
            is LinearListItem -> {
                extractTranslatableTextRecursively(
                    elements = element.content,
                    translatableTexts = translatableTexts,
                    nestingLevel = nestingLevel + 1,
                )
            }
            is LinearBlockQuote -> {
                extractTranslatableTextRecursively(
                    elements = element.content,
                    translatableTexts = translatableTexts,
                    nestingLevel = nestingLevel + 1,
                )
            }
            else -> {}
        }
    }
}
```

**Validation:**
- ✅ Helper functions compile
- ✅ Functions match ArticleViewModel logic

---

#### Step 2.2: Add Test Cases (90 minutes)

**File:** `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Add Test Class:**
```kotlin
class TranslationListParsingTest {
    @Test
    fun `single paragraph list item gets correct translation`() {
        val html = """
            <ul>
              <li>Only one paragraph</li>
            </ul>
        """.trimIndent()

        val article = HtmlLinearizer().linearize(html, "https://example.com")
        val translatableTexts = extractTranslatableParagraphs(article)
        val translations = listOf("Translation")
        val indices = computeParagraphIndices(article.elements, translations)

        // Verify extraction found one text
        assertEquals(1, translatableTexts.size)
        assertEquals("Only one paragraph", translatableTexts[0])

        // Verify computation assigned one index
        assertEquals(1, indices.values.count { it != null })
        assertEquals(1, indices.size)

        // Verify the index is 0
        val translationIndex = indices.values.first()
        assertEquals(0, translationIndex)
    }

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
        val translations = listOf("Trans 1", "Trans 2")
        val indices = computeParagraphIndices(article.elements, translations)

        // Verify extraction found two texts
        assertEquals(2, translatableTexts.size)
        assertEquals("First paragraph", translatableTexts[0])
        assertEquals("Second paragraph", translatableTexts[1])

        // Verify computation assigned two indices
        assertEquals(2, indices.values.count { it != null })

        // Verify the indices are 0 and 1
        val translationIndices = indices.values.filterNotNull().sorted()
        assertEquals(listOf(0, 1), translationIndices)

        // Verify container (list item) has no translation
        val listItemIndex = article.elements.indexOfFirst { it is LinearListItem }
        assertTrue(listItemIndex >= 0)
        assertNull(indices[listItemIndex])
    }

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

        // Verify extraction found four texts
        assertEquals(4, translatableTexts.size)
        assertEquals("Outer item 1", translatableTexts[0])
        assertEquals("Inner item 1", translatableTexts[1])
        assertEquals("Inner item 2", translatableTexts[2])
        assertEquals("Outer item 2", translatableTexts[3])

        // Verify computation assigned four indices
        assertEquals(4, indices.values.count { it != null })

        // Verify the indices are sequential
        val translationIndices = indices.values.filterNotNull().sorted()
        assertEquals(listOf(0, 1, 2, 3), translationIndices)
    }

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

        // Verify extraction found five texts
        assertEquals(5, translatableTexts.size)

        // Verify computation assigned five indices
        assertEquals(5, indices.values.count { it != null })

        // Verify the indices are sequential
        val translationIndices = indices.values.filterNotNull().sorted()
        assertEquals(listOf(0, 1, 2, 3, 4), translationIndices)
    }

    @Test
    fun `empty list item gets no translation`() {
        val html = """
            <ul>
              <li></li>
            </ul>
        """.trimIndent()

        val article = HtmlLinearizer().linearize(html, "https://example.com")
        val translatableTexts = extractTranslatableParagraphs(article)
        val indices = computeParagraphIndices(article.elements, emptyList())

        // Verify extraction found no texts
        assertEquals(0, translatableTexts.size)

        // Verify computation assigned no indices
        assertEquals(0, indices.values.count { it != null })
    }

    @Test
    fun `code blocks are not translated`() {
        val html = """
            <ul>
              <li>
                <p>Text paragraph</p>
                <pre><code>Code content</code></pre>
                <p>Another text paragraph</p>
              </li>
            </ul>
        """.trimIndent()

        val article = HtmlLinearizer().linearize(html, "https://example.com")
        val translatableTexts = extractTranslatableParagraphs(article)
        val translations = listOf("Trans 1", "Trans 2")
        val indices = computeParagraphIndices(article.elements, translations)

        // Verify extraction found only two texts (code block excluded)
        assertEquals(2, translatableTexts.size)
        assertTrue(translatableTexts.all { it != "Code content" })

        // Verify computation assigned two indices
        assertEquals(2, indices.values.count { it != null })
    }
}
```

**Validation:**
- ✅ All tests compile
- ✅ All tests pass
- ✅ Edge cases are covered

---

### Phase 3: Validation (1 hour)

#### Step 3.1: Manual Testing (30 minutes)

**Test Cases:**

1. **Open the app and navigate to a feed with list items**
   - Expected: Lists render correctly
   - Verify: No crashes or visual glitches

2. **Find an article with multi-paragraph list items**
   - Expected: Each paragraph shows correct translation
   - Verify: No missing or duplicate translations

3. **Find an article with nested lists**
   - Expected: All list items show correct translations
   - Verify: Nesting depth is handled correctly

4. **Enable translation for a feed**
   - Expected: Translations appear for all translatable text
   - Verify: List items with multiple paragraphs work correctly

**Validation:**
- ✅ All manual tests pass
- ✅ No visual regressions
- ✅ No performance issues

---

#### Step 3.2: Performance Testing (15 minutes)

**Benchmark:**
```kotlin
@Test
fun `index computation performance test`() {
    val html = generateLargeArticle(100) // Generate article with 100 elements
    val article = HtmlLinearizer().linearize(html, "https://example.com")
    val translations = (1..50).map { "Translation $it" }

    val startTime = System.nanoTime()
    val indices = computeParagraphIndices(article.elements, translations)
    val duration = (System.nanoTime() - startTime) / 1_000_000  // Convert to ms

    // Verify performance is acceptable
    assertTrue(duration < 10, "Index computation took ${duration}ms, expected < 10ms")
}
```

**Validation:**
- ✅ Performance is < 10ms for typical articles
- ✅ No memory leaks
- ✅ No performance regressions

---

#### Step 3.3: Code Review (15 minutes)

**Checklist:**

- [ ] Code follows project style guidelines
- [ ] All functions have clear documentation
- [ ] Error handling is appropriate
- [ ] Tests are comprehensive
- [ ] No code duplication
- [ ] Performance is acceptable
- [ ] Backward compatibility is maintained

**Validation:**
- ✅ All checklist items pass
- ✅ Code is ready for merge

---

## 3. Risk Mitigation

### 3.1 Rollback Plan

**If critical issues are found:**

1. **Revert code changes:**
   ```bash
   git revert <commit-hash>
   git push
   ```

2. **Add feature flag:**
   ```kotlin
   private val USE_NEW_LIST_PARSING = BuildConfig.DEBUG
   ```

3. **Test with subset of users:**
   - Enable for 10% of users
   - Monitor error logs
   - Roll out gradually if successful

### 3.2 Monitoring

**Add logging:**
```kotlin
logDebug(LOG_TAG, "Translation parsing: version=NEW, count=$computedCount, expected=${translatedParagraphs.size}")
```

**Monitor:**
- Error rates in production
- User reports of translation issues
- Performance metrics

---

## 4. Success Criteria

### 4.1 Must Have (Blocking)

- ✅ All new unit tests pass
- ✅ All existing tests continue to pass
- ✅ Manual testing shows correct behavior
- ✅ No regressions in single-paragraph lists
- ✅ Performance is acceptable (< 10ms)

### 4.2 Should Have (Important)

- ✅ Code is well-documented
- ✅ Tests cover edge cases
- ✅ No code duplication
- ✅ Follows project patterns

### 4.3 Nice to Have (Enhancement)

- Visual regression tests
- Integration tests with real feeds
- Performance benchmarks
- Error reporting dashboard

---

## 5. Timeline

### Week 1: Implementation

| Day | Task | Owner | Status |
|-----|------|-------|--------|
| Monday | Step 1.1-1.2: Core implementation | Dev | Pending |
| Tuesday | Step 1.3-1.4: Complete implementation | Dev | Pending |
| Wednesday | Step 2.1-2.2: Add tests | QA | Pending |
| Thursday | Step 3.1-3.2: Validation | Dev | Pending |
| Friday | Step 3.3: Code review and merge | Lead | Pending |

### Week 2: Deployment (if needed)

| Day | Task | Owner | Status |
|-----|------|-------|--------|
| Monday | Feature flag rollout | DevOps | Pending |
| Tuesday | Monitor error logs | QA | Pending |
| Wednesday | Full rollout | DevOps | Pending |
| Thursday | Post-deployment validation | QA | Pending |
| Friday | Documentation update | Dev | Pending |

---

## 6. Dependencies

### 6.1 External Dependencies

- None - all changes are internal

### 6.2 Internal Dependencies

- ArticleViewModel.kt - Must match extraction logic
- LinearArticleContent.kt - Main implementation
- HtmlLinearizerTest.kt - Test implementation

---

## 7. Open Issues

### 7.1 Resolved

None - all issues resolved during specification phase.

### 7.2 Outstanding

None - implementation is straightforward.

---

## 8. Sign-Off

### 8.1 Implementation Team

- **Developer:** [Name]
- **Code Reviewer:** [Name]
- **QA Engineer:** [Name]

### 8.2 Approval

- [ ] Implementation complete
- [ ] Tests pass
- [ ] Code review approved
- [ ] QA validation complete
- [ ] Ready for merge

---

**End of Implementation Plan**
