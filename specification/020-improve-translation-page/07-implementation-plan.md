# Implementation Plan - Improve Translation Page

**Spec Index:** 020
**Feature Name:** Improve Translation Page - Fix Nested Lists and Blockquote Translation
**Date:** 2026-01-05
**Phase:** 6 - Specification Writing (Implementation Plan)
**Status:** Draft

## 1. Overview

This implementation plan defines the step-by-step process to fix translation coverage gaps for nested lists and blockquote content.

**Total Estimated Time:** 4-5 hours
**Risk Level:** Low
**Complexity:** Medium

## 2. Implementation Phases

### Phase 1: Recursive Extraction Logic (1.5 hours)

**Objective:** Implement recursive text extraction to handle nested structures.

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Steps:**

#### Step 1.1: Create Recursive Helper Function (30 min)

```kotlin
/**
 * Recursively extracts translatable text from a list of elements.
 *
 * @param elements The list of elements to traverse
 * @param paragraphs Mutable list to accumulate extracted text
 */
private fun extractTranslatableTextRecursively(
    elements: List<LinearElement>,
    paragraphs: MutableList<String>
) {
    for (element in elements) {
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT) {
                    val text = element.text
                    if (text.isNotBlank()) {
                        paragraphs.add(text.trim())
                    }
                }
            }
            is LinearListItem -> {
                // Recurse into nested list items
                extractTranslatableTextRecursively(
                    elements = element.content,
                    paragraphs = paragraphs
                )
            }
            is LinearBlockQuote -> {
                // Recurse into blockquote content
                extractTranslatableTextRecursively(
                    elements = element.content,
                    paragraphs = paragraphs
                )
            }
            else -> {
                // Ignore other types
            }
        }
    }
}
```

**Tasks:**
- [ ] Copy existing extraction logic into new function
- [ ] Add recursive case for `LinearListItem`
- [ ] Add case for `LinearBlockQuote`
- [ ] Add KDoc comments
- [ ] Verify code compiles

#### Step 1.2: Update Main Extraction Function (30 min)

```kotlin
private fun extractTranslatableParagraphs(): List<String> {
    val paragraphs = mutableListOf<String>()
    extractTranslatableTextRecursively(
        elements = viewState.value.articleContent.elements,
        paragraphs = paragraphs
    )
    return paragraphs
}
```

**Tasks:**
- [ ] Simplify `extractTranslatableParagraphs()` to call helper
- [ ] Remove old when expression logic
- [ ] Update comments to reflect recursive behavior
- [ ] Test compilation

#### Step 1.3: Unit Tests (30 min)

**Test File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Tests to Write:**
- [ ] Test simple paragraph extraction
- [ ] Test nested list item extraction (2 levels)
- [ ] Test deeply nested list extraction (3 levels)
- [ ] Test blockquote text extraction
- [ ] Test multi-paragraph blockquote extraction
- [ ] Test mixed content (paragraphs + lists + blockquotes)
- [ ] Test that code blocks are skipped

**Verification:**
- [ ] All tests pass
- [ ] Code coverage > 80% for extraction logic

### Phase 2: Rendering Updates (2 hours)

**Objective:** Update rendering to pass translations through nested structures.

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**

#### Step 2.1: Update LinearListItemContent Signature (30 min)

```kotlin
@Composable
fun LinearListItemContent(
    listItem: LinearListItem,
    translations: List<String>? = null,        // CHANGED: was translation: String?
    translationStartIndex: Int = 0,            // NEW: cursor position
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Tasks:**
- [ ] Change `translation` parameter to `translations` (list)
- [ ] Add `translationStartIndex` parameter
- [ ] Update function documentation
- [ ] Note: This is a breaking change for call sites

#### Step 2.2: Implement Cursor Logic in LinearListItemContent (45 min)

```kotlin
@Composable
fun LinearListItemContent(
    listItem: LinearListItem,
    translations: List<String>? = null,
    translationStartIndex: Int = 0,
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        // List item indicator
        if (listItem.orderedIndex != null) {
            Text("${listItem.orderedIndex}.")
        } else {
            Text("•")
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            var currentIndex = translationStartIndex

            listItem.content.forEach { element ->
                when (element) {
                    is LinearText -> {
                        val translation = translations?.getOrNull(currentIndex)
                        if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                            element.text.isNotBlank()) {
                            currentIndex++
                        }

                        LinearTextContent(
                            linearText = element,
                            translation = translation,
                            idToIndex = idToIndex,
                            onLinkClick = onLinkClick,
                        )
                    }
                    is LinearListItem -> {
                        // Recurse for nested list items
                        LinearListItemContent(
                            listItem = element,
                            translations = translations,
                            translationStartIndex = currentIndex,
                            allowHorizontalScroll = allowHorizontalScroll,
                            idToIndex = idToIndex,
                            onLinkClick = onLinkClick,
                        )

                        // Advance cursor by number of texts in nested item
                        currentIndex += countTranslatableText(element.content)
                    }
                    is LinearBlockQuote -> {
                        // Render blockquote (which may contain translatable text)
                        LinearBlockQuoteContent(
                            blockQuote = element,
                            translations = translations,
                            translationStartIndex = currentIndex,
                            allowHorizontalScroll = allowHorizontalScroll,
                            idToIndex = idToIndex,
                            onLinkClick = onLinkClick,
                        )

                        // Advance cursor
                        currentIndex += countTranslatableText(element.content)
                    }
                    else -> {
                        LinearElementContent(
                            linearElement = element,
                            translation = null,
                            allowHorizontalScroll = allowHorizontalScroll,
                            idToIndex = idToIndex,
                            onLinkClick = onLinkClick,
                        )
                    }
                }
            }
        }
    }
}
```

**Tasks:**
- [ ] Implement cursor tracking (`var currentIndex`)
- [ ] Add `when` expression for element types
- [ ] Handle `LinearText` (consume translation)
- [ ] Handle `LinearListItem` (recurse)
- [ ] Handle `LinearBlockQuote` (delegate to blockquote renderer)
- [ ] Handle other types (no translation)
- [ ] Test compilation

#### Step 2.3: Add Helper Function (15 min)

```kotlin
/**
 * Counts translatable text elements in a list.
 */
private fun countTranslatableText(elements: List<LinearElement>): Int {
    return elements.count { element ->
        element is LinearText &&
        element.blockStyle == LinearTextBlockStyle.TEXT &&
        element.text.isNotBlank()
    }
}
```

**Tasks:**
- [ ] Implement `countTranslatableText()` helper
- [ ] Add documentation
- [ ] Test with various element types

#### Step 2.4: Update LinearBlockQuoteContent (30 min)

**New Function Signature:**
```kotlin
@Composable
fun LinearBlockQuoteContent(
    blockQuote: LinearBlockQuote,
    translations: List<String>? = null,
    translationStartIndex: Int = 0,
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Implementation:**
```kotlin
@Composable
fun LinearBlockQuoteContent(
    blockQuote: LinearBlockQuote,
    translations: List<String>? = null,
    translationStartIndex: Int = 0,
    allowHorizontalScroll: Boolean,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex = translationStartIndex

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blockQuote.content.forEach { element ->
            when (element) {
                is LinearText -> {
                    val translation = translations?.getOrNull(currentIndex)
                    if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                        element.text.isNotBlank()) {
                        currentIndex++
                    }

                    ProvideTextStyle(
                        MaterialTheme.typography.bodyLarge.merge(
                            SpanStyle(
                                fontWeight = FontWeight.Light,
                            ),
                        ),
                    ) {
                        LinearTextContent(
                            linearText = element,
                            translation = translation,
                            idToIndex = idToIndex,
                            onLinkClick = onLinkClick,
                        )
                    }
                }
                is LinearListItem -> {
                    LinearListItemContent(
                        listItem = element,
                        translations = translations,
                        translationStartIndex = currentIndex,
                        allowHorizontalScroll = allowHorizontalScroll,
                        idToIndex = idToIndex,
                        onLinkClick = onLinkClick,
                    )

                    currentIndex += countTranslatableText(element.content)
                }
                else -> {
                    LinearElementContent(
                        linearElement = element,
                        translation = null,
                        allowHorizontalScroll = allowHorizontalScroll,
                        idToIndex = idToIndex,
                        onLinkClick = onLinkClick,
                    )
                }
            }
        }

        blockQuote.cite?.let { cite ->
            // Render cite (unchanged)
        }
    }
}
```

**Tasks:**
- [ ] Update `LinearBlockQuoteContent` signature
- [ ] Implement cursor tracking
- [ ] Handle nested `LinearText`
- [ ] Handle nested `LinearListItem`
- [ ] Preserve existing cite rendering
- [ ] Test compilation

### Phase 3: Integration (1 hour)

**Objective:** Update all call sites to use new signatures.

**Steps:**

#### Step 3.1: Update linearArticleContent (30 min)

```kotlin
fun LazyListScope.linearArticleContent(
    articleContent: LinearArticle,
    translatedParagraphs: List<String>? = null,
    onLinkClick: (url: String, index: Int?) -> Unit,
) {
    var translationIndex = 0

    items(
        count = articleContent.elements.size,
        key = { index ->
            val element = articleContent.elements[index]
            when (element) {
                is LinearText -> "text_${index}_${element.text.take(20)}"
                is LinearListItem -> "listitem_${index}_${element.orderedIndex ?: "bullet"}"
                is LinearBlockQuote -> "blockquote_${index}_${element.cite?.hashCode() ?: index}"
                else -> "element_${index}"
            }
        },
        contentType = { index -> articleContent.elements[index].lazyListContentType },
    ) { index ->
        val element = articleContent.elements[index]

        when (element) {
            is LinearText -> {
                val translation = translatedParagraphs?.getOrNull(translationIndex)
                if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                    element.text.isNotBlank()) {
                    translationIndex++
                }

                LinearTextContent(
                    linearText = element,
                    translation = translation,
                    idToIndex = articleContent.idToIndex,
                    onLinkClick = onLinkClick,
                    modifier = Modifier
                        .widthIn(max = minOf(maxWidth, LocalDimens.current.maxReaderWidth))
                        .fillMaxWidth(),
                )
            }
            is LinearListItem -> {
                LinearListItemContent(
                    listItem = element,
                    translations = translatedParagraphs,
                    translationStartIndex = translationIndex,
                    allowHorizontalScroll = true,
                    idToIndex = articleContent.idToIndex,
                    onLinkClick = onLinkClick,
                    modifier = Modifier
                        .widthIn(max = minOf(maxWidth, LocalDimens.current.maxReaderWidth))
                        .fillMaxWidth(),
                )

                translationIndex += countTranslatableTextInList(element)
            }
            is LinearBlockQuote -> {
                LinearBlockQuoteContent(
                    blockQuote = element,
                    translations = translatedParagraphs,
                    translationStartIndex = translationIndex,
                    allowHorizontalScroll = true,
                    idToIndex = articleContent.idToIndex,
                    onLinkClick = onLinkClick,
                    modifier = Modifier
                        .widthIn(max = minOf(maxWidth, LocalDimens.current.maxReaderWidth))
                        .fillMaxWidth(),
                )

                translationIndex += countTranslatableText(element.content)
            }
            else -> {
                LinearElementContent(
                    linearElement = element,
                    translation = null,
                    allowHorizontalScroll = true,
                    idToIndex = articleContent.idToIndex,
                    onLinkClick = onLinkClick,
                    modifier = Modifier
                        .widthIn(max = minOf(maxWidth, LocalDimens.current.maxReaderWidth))
                        .fillMaxWidth(),
                )
            }
        }
    }
}
```

**Tasks:**
- [ ] Add `translationIndex` cursor tracking
- [ ] Replace old `computeParagraphIndices` call
- [ ] Add `when` expression for element types
- [ ] Pass appropriate parameters to each renderer
- [ ] Update cursor after each element
- [ ] Test compilation

#### Step 3.2: Remove Old Index Computation (15 min)

**Tasks:**
- [ ] Remove `computeParagraphIndices()` function (no longer needed)
- [ ] Remove references to it
- [ ] Verify no compilation errors

#### Step 3.3: Update LinearElementContent (15 min)

**Note:** Check if `LinearElementContent` needs updates to handle new signatures.

**Tasks:**
- [ ] Review `LinearElementContent` implementation
- [ ] Update if it calls `LinearListItemContent` or `LinearBlockQuoteContent`
- [ ] Verify all call sites are updated

### Phase 4: Testing (1.5 hours)

**Objective:** Verify all changes work correctly.

#### Step 4.1: Unit Tests (30 min)

**Tasks:**
- [ ] Run all existing unit tests
- [ ] Fix any failures
- [ ] Run new extraction tests
- [ ] Verify 100% pass rate

#### Step 4.2: Build Verification (15 min)

**Tasks:**
- [ ] Clean build: `./gradlew clean`
- [ ] Full build: `./gradlew assembleDebug`
- [ ] Verify no compilation errors
- [ ] Verify no warnings

#### Step 4.3: Manual Testing (45 min)

**Test Article 1: Nested Lists**
```html
<!DOCTYPE html>
<html>
<body>
  <h1>Nested Lists Test</h1>
  <ul>
    <li>Level 1 item 1</li>
    <li>Level 1 item 2
      <ul>
        <li>Level 2 item 1</li>
        <li>Level 2 item 2
          <ul>
            <li>Level 3 item 1</li>
            <li>Level 3 item 2</li>
          </ul>
        </li>
      </ul>
    </li>
  </ul>
</body>
</html>
```

**Tasks:**
- [ ] Load test article in app
- [ ] Tap translate button
- [ ] Verify all 6 list items show translations
- [ ] Verify translations appear below correct items
- [ ] Verify no crashes or errors

**Test Article 2: Blockquotes**
```html
<!DOCTYPE html>
<html>
<body>
  <h1>Blockquote Test</h1>
  <p>Regular paragraph before quote.</p>
  <blockquote>
    <p>This is a quoted paragraph.</p>
    <p>This is another quoted paragraph.</p>
  </blockquote>
  <p>Regular paragraph after quote.</p>
</body>
</html>
```

**Tasks:**
- [ ] Load test article in app
- [ ] Tap translate button
- [ ] Verify blockquote paragraphs are translated
- [ ] Verify regular paragraphs are translated
- [ ] Verify correct ordering

**Test Article 3: Mixed Content**
```html
<!DOCTYPE html>
<html>
<body>
  <h1>Mixed Content Test</h1>
  <p>Intro paragraph.</p>
  <ul>
    <li>List item 1</li>
    <li>List item 2 with nested list
      <ul>
        <li>Nested item</li>
      </ul>
    </li>
  </ul>
  <blockquote>
    <p>A quote with a list:</p>
    <ul>
      <li>Quote list item</li>
    </ul>
  </blockquote>
</body>
</html>
```

**Tasks:**
- [ ] Load test article in app
- [ ] Tap translate button
- [ ] Verify all content is translated
- [ ] Verify correct translation placement
- [ ] Verify no missing translations

## 3. Risk Mitigation

### Risk 1: Breaking Existing Translations

**Mitigation:**
- Comprehensive regression testing
- Test with existing articles
- Preserve backward compatibility

### Risk 2: Performance Degradation

**Mitigation:**
- Benchmark before/after
- Profile if needed
- Optimize if necessary

### Risk 3: Index Mismatch

**Mitigation:**
- Thorough manual testing
- Visual verification of translation placement
- Unit tests for cursor logic

## 4. Rollback Plan

If issues arise:
1. Revert changes to `ArticleViewModel.kt`
2. Revert changes to `LinearArticleContent.kt`
3. Restore previous implementation
4. Investigate and fix issues

## 5. Success Criteria

**Phase 1 Complete:**
- [ ] Recursive extraction implemented
- [ ] Unit tests pass
- [ ] Code compiles without warnings

**Phase 2 Complete:**
- [ ] Rendering updates implemented
- [ ] New functions added
- [ ] Code compiles without warnings

**Phase 3 Complete:**
- [ ] All call sites updated
- [ ] Old code removed
- [ ] Clean compilation

**Phase 4 Complete:**
- [ ] All tests pass
- [ ] Manual testing successful
- [ ] No regressions detected

## 6. Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Extraction | 1.5 hours | None |
| Phase 2: Rendering | 2 hours | Phase 1 |
| Phase 3: Integration | 1 hour | Phase 2 |
| Phase 4: Testing | 1.5 hours | Phase 3 |
| **Total** | **6 hours** | |

## 7. Sign-Off

**Implementation Plan Approval:**
- [ ] Technical lead approved
- [ ] Timeline accepted
- [ ] Risks acknowledged

---

**Implementation Plan Complete**
**Ready for Phase 7 (Specification Review)**
