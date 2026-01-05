# Requirements Document - Improve Translation Page

**Spec Index:** 020
**Feature Name:** Improve Translation Page - Fix Nested Lists and Blockquote Translation
**Date:** 2026-01-05
**Phase:** 2 - Requirements Clarification
**Status:** Draft

## 1. Background

### 1.1 Context

In **spec-013-translation-page**, we implemented the article translation feature that allows users to translate article content on-demand. The implementation extracts translatable paragraphs from the article and displays translations paragraph-by-paragraph below the original text.

### 1.2 Previous Implementation Summary

**What Works:**
- Translation button in app bar
- Translation of regular paragraphs (`<p>` tags)
- Translation of simple list items (`<li>` tags)
- Loading and error states
- Integration with translation settings
- Auto-translation when enabled

**Translation Extraction Logic (spec-013):**
```kotlin
private fun extractTranslatableParagraphs(): List<String> {
    val content = viewState.value.articleContent
    val paragraphs = mutableListOf<String>()

    for (element in content.elements) {
        when (element) {
            is LinearText -> {
                // Only translate regular text (not code blocks or pre-formatted text)
                if (element.blockStyle == LinearTextBlockStyle.TEXT) {
                    val text = element.text
                    if (text.isNotBlank()) {
                        paragraphs.add(text.trim())
                    }
                }
            }
            is LinearListItem -> {
                // Extract text from list items
                val text = listItem.content
                    .filterIsInstance<LinearText>()
                    .filter { it.blockStyle == LinearTextBlockStyle.TEXT }
                    .joinToString(" ") { it.text }
                if (text.isNotBlank()) {
                    paragraphs.add(text.trim())
                }
            }
        }
    }

    return paragraphs
}
```

**Data Structure Understanding:**
- `LinearListItem` has `content: List<LinearElement>` - can contain nested elements
- `LinearBlockQuote` has `content: List<LinearElement>` - can contain nested elements
- Both support nested structures (lists within lists, blockquotes with paragraphs)

## 2. Problem Statement

### 2.1 Issue 1: Nested Lists Not Translated

**Description:**
Some nested list items (2 or 3 levels deep) are not being translated.

**HTML Example:**
```html
<ul>
  <li>Level 1 item</li>
  <li>
    Level 1 with nested list
    <ul>
      <li>Level 2 item (NOT TRANSLATED)</li>
      <li>
        Level 2 with deeper nesting
        <ul>
          <li>Level 3 item (NOT TRANSLATED)</li>
        </ul>
      </li>
    </ul>
  </li>
</ul>
```

**Expected Behavior:**
- All list items at all nesting levels should be translated
- Each list item should get its own translation

**Current Behavior:**
- Only top-level list items are translated
- Nested list items are skipped

**Root Cause Hypothesis:**
The current extraction logic in `ArticleViewModel.extractTranslatableParagraphs()` only iterates through top-level elements in `articleContent.elements`. It doesn't recursively traverse nested structures like:
- `LinearListItem.content` which may contain child `LinearListItem` elements
- Nested lists within list items

### 2.2 Issue 2: Blockquote Content Not Translated

**Description:**
Content within blockquote (`<blockquote>`) tags is not being translated.

**HTML Example:**
```html
<blockquote>
  <p>This quoted text is NOT TRANSLATED</p>
  <p>Another paragraph in blockquote NOT TRANSLATED</p>
</blockquote>
```

**Expected Behavior:**
- All text content within blockquotes should be translated
- Each paragraph within a blockquote should get its own translation

**Current Behavior:**
- Blockquote content is completely skipped

**Root Cause Hypothesis:**
The current extraction logic only handles `LinearText` and `LinearListItem` elements at the top level. It doesn't handle:
- `LinearBlockQuote` elements
- The nested content within blockquotes (paragraphs, text, etc.)

### 2.3 Impact Assessment

**User Impact:**
- **Severity:** Medium
- **Frequency:** High (common in blog posts, articles, documentation)
- **User Frustration:** Medium - incomplete translations reduce utility

**Content Types Affected:**
- Technical documentation (heavy use of nested lists)
- Blog posts with blockquotes
- Articles with complex formatting
- Academic papers (nested references, blockquoted sections)

**Examples:**
- Nested bullet lists in documentation
- Numbered sub-lists in articles
- Quoted passages in blog posts
- Citations and references

## 3. Requirements

### 3.1 Functional Requirements

#### FR-1: Nested List Translation
**ID:** FR-1
**Priority:** High
**Description:** The system must translate all list items at all nesting levels.

**Acceptance Criteria:**
- [ ] Level 1 list items are translated
- [ ] Level 2 nested list items are translated
- [ ] Level 3+ deeply nested list items are translated
- [ ] Each list item gets its own translation (not merged)
- [ ] Translation appears below the corresponding list item
- [ ] Translation styling matches paragraph translations (italic, secondary color)

**Test Cases:**
```html
<!-- Test Case 1: Simple nested list -->
<ul>
  <li>Item 1</li>
  <li>Item 2
    <ul>
      <li>Nested 2.1</li>
      <li>Nested 2.2</li>
    </ul>
  </li>
</ul>

<!-- Test Case 2: Triple nested list -->
<ol>
  <li>One</li>
  <li>Two
    <ol>
      <li>Two.A</li>
      <li>Two.B
        <ol>
          <li>Two.B.i</li>
          <li>Two.B.ii</li>
        </ol>
      </li>
    </ol>
  </li>
</ol>

<!-- Test Case 3: Mixed ordered/unordered -->
<ul>
  <li>Bullet 1
    <ol>
      <li>Numbered 1.1</li>
    </ol>
  </li>
</ul>
```

#### FR-2: Blockquote Translation
**ID:** FR-2
**Priority:** High
**Description:** The system must translate all text content within blockquotes.

**Acceptance Criteria:**
- [ ] Paragraphs within blockquotes are translated
- [ ] Each paragraph in blockquote gets its own translation
- [ ] Translation appears below the corresponding paragraph
- [ ] Translation maintains blockquote visual styling
- [ ] Nested structures within blockquotes are handled (e.g., lists in blockquotes)

**Test Cases:**
```html
<!-- Test Case 1: Simple blockquote -->
<blockquote>
  <p>This is a quoted paragraph.</p>
</blockquote>

<!-- Test Case 2: Multi-paragraph blockquote -->
<blockquote>
  <p>First quoted paragraph.</p>
  <p>Second quoted paragraph.</p>
</blockquote>

<!-- Test Case 3: Blockquote with cite -->
<blockquote cite="https://example.com">
  <p>Quoted text with source.</p>
</blockquote>

<!-- Test Case 4: Blockquote with nested list -->
<blockquote>
  <p>Introduction quote</p>
  <ul>
    <li>List item in quote</li>
  </ul>
</blockquote>
```

#### FR-3: Recursive Content Traversal
**ID:** FR-3
**Priority:** High
**Description:** The extraction logic must recursively traverse all nested content structures.

**Acceptance Criteria:**
- [ ] All `LinearText` elements at any depth are found
- [ ] All `LinearListItem` elements at any depth are found
- [ ] All text within `LinearBlockQuote` elements is found
- [ ] Ordering is preserved (translations match original order)
- [ ] No duplication of text (each text element extracted once)

#### FR-4: Translation Display Matching
**ID:** FR-4
**Priority:** Medium
**Description:** Translations must be displayed in the correct position relative to their source text.

**Acceptance Criteria:**
- [ ] List item translations appear below the list item text
- [ ] Blockquote paragraph translations appear below the paragraph
- [ ] Nested list item translations appear below the nested item
- [ ] Translation index matching is accurate (no misalignment)

### 3.2 Non-Functional Requirements

#### NFR-1: Performance
**ID:** NFR-1
**Priority:** Medium
**Description:** Extraction must remain fast even with deeply nested content.

**Requirements:**
- Extraction time < 50ms for typical articles
- No stack overflow on deeply nested structures (safe up to 20 levels)
- Memory usage should not increase significantly

#### NFR-2: Maintainability
**ID:** NFR-2
**Priority:** High
**Description:** Code must be easy to understand and modify.

**Requirements:**
- Clear separation between traversal logic and translation API calls
- Well-documented recursive function
- Unit tests for edge cases (deep nesting, empty content)

#### NFR-3: Backward Compatibility
**ID:** NFR-3
**Priority:** High
**Description:** Changes must not break existing translations.

**Requirements:**
- Existing simple paragraph translations continue to work
- Existing simple list item translations continue to work
- No changes to API interfaces or data models
- No changes to translation display styling

### 3.3 Technical Requirements

#### TR-1: Recursive Extraction Algorithm
**ID:** TR-1
**Priority:** High
**Description:** Implement recursive traversal of nested content.

**Technical Approach:**
```kotlin
private fun extractTranslatableParagraphs(): List<String> {
    val paragraphs = mutableListOf<String>()
    extractTextRecursively(viewState.value.articleContent.elements, paragraphs)
    return paragraphs
}

private fun extractTextRecursively(
    elements: List<LinearElement>,
    paragraphs: MutableList<String>
) {
    for (element in elements) {
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT &&
                    element.text.isNotBlank()) {
                    paragraphs.add(element.text.trim())
                }
            }
            is LinearListItem -> {
                // Extract text from this list item
                extractTextRecursively(element.content, paragraphs)
            }
            is LinearBlockQuote -> {
                // Extract text from blockquote content
                extractTextRecursively(element.content, paragraphs)
            }
            // Handle other container types as needed
        }
    }
}
```

#### TR-2: Index Computation Update
**ID:** TR-2
**Priority:** High
**Description:** Update `computeParagraphIndices()` to match recursive extraction.

**Technical Approach:**
The index computation must mirror the recursive extraction logic to ensure translations are matched to the correct text elements.

#### TR-3: Rendering Updates
**ID:** TR-3
**Priority:** High
**Description:** Ensure `LinearListItemContent` and blockquote rendering support translations.

**Technical Approach:**
- Update `LinearListItemContent` to accept and display `translation` parameter
- Update blockquote rendering to pass translations to nested content
- Ensure `LinearTextContent` is used consistently

## 4. Out of Scope

The following are explicitly **NOT** in scope for this fix:

### 4.1 Not in Scope

- **Translation of code blocks:** Pre-formatted text and code blocks should remain untranslated
- **Translation of table content:** Table cells may contain complex structures, not addressed
- **Translation of image captions:** Already handled separately
- **Translation quality:** Not fixing translation quality, just coverage
- **Performance optimization:** Beyond ensuring acceptable performance
- **Caching or persistence:** Not adding new features
- **UI changes:** No changes to translation button, loading states, error handling
- **Real AI translation:** Still using dummy translation

### 4.2 Deferred to Future Work

- Translation of inline elements within text (bold, italic, links)
- Translation of list item markers (numbers, bullets)
- Translation of blockquote citations (`cite` attribute)
- Handling of deeply nested structures (> 20 levels)
- Translation of table cell content

## 5. Dependencies

### 5.1 Technical Dependencies

- **Existing Implementation:** spec-013-translation-page
- **Data Models:** `LinearElement`, `LinearText`, `LinearListItem`, `LinearBlockQuote`
- **View Model:** `ArticleViewModel.extractTranslatableParagraphs()`
- **UI Components:** `LinearArticleContent`, `LinearListItemContent`, `LinearTextContent`
- **AI API:** `AIClient.translate()` (no changes needed)

### 5.2 External Dependencies

- None (using existing dummy translation)

## 6. Risks and Mitigation

### 6.1 Risk 1: Performance Degradation
**Risk:** Recursive traversal might be slower for complex articles.

**Mitigation:**
- Benchmark before/after performance
- Optimize if needed (e.g., tail recursion, iteration)
- Set reasonable depth limits if necessary

### 6.2 Risk 2: Index Mismatch
**Risk:** Translations might not match the correct text elements.

**Mitigation:**
- Thorough testing with nested structures
- Visual verification of translation placement
- Unit tests for index computation

### 6.3 Risk 3: Stack Overflow
**Risk:** Very deep nesting could cause stack overflow.

**Mitigation:**
- Use iterative approach if recursion depth is concerning
- Set maximum depth limit (e.g., 20 levels)
- Test with artificially deep nesting

### 6.4 Risk 4: Breaking Existing Features
**Risk:** Changes might break current translation functionality.

**Mitigation:**
- Comprehensive regression testing
- Ensure backward compatibility
- Test with existing articles

## 7. Success Metrics

### 7.1 Functional Metrics

- **Coverage:** 100% of translatable text elements (excluding code blocks)
- **Nesting Support:** Support for at least 5 levels of nesting
- **Blockquote Support:** 100% of blockquote text content translated

### 7.2 Quality Metrics

- **Test Coverage:** > 80% for new extraction logic
- **Regression:** Zero regressions in existing translation features
- **Performance:** Extraction time < 50ms for typical articles

### 7.3 Acceptance Criteria Summary

**Complete when:**
- [ ] All nested list items are translated (tested up to 3 levels)
- [ ] All blockquote content is translated
- [ ] Translations appear in correct positions
- [ ] No regressions in existing translations
- [ ] Performance acceptable
- [ ] Tests pass
- [ ] Code compiles without warnings

## 8. Related Specifications

- **spec-013-translation-page:** Original translation implementation
- **spec-014-translation-function:** Translation function improvements
- **spec-011-translation-config:** Translation configuration

## 9. Sign-Off

**Requirements Approval:**
- [ ] Product owner approved
- [ ] Technical lead approved
- [ ] QA lead approved

---

**Requirements Document Complete**
**Total Requirements:** 4 FRs, 3 NFRs, 3 TRs
**Ready for Phase 3 (Research)**
