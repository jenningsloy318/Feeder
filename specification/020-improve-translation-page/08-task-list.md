# Task List - Improve Translation Page

**Spec Index:** 020
**Feature Name:** Improve Translation Page - Fix Nested Lists and Blockquote Translation
**Date:** 2026-01-05
**Phase:** 6 - Specification Writing (Task List)
**Status:** Draft

## Task Overview

**Total Tasks:** 23
**Estimated Effort:** 6 hours
**Priority:** High

## Task Breakdown

### Category 1: Extraction Logic (ArticleViewModel.kt)

#### Task 1.1: Create Recursive Helper Function
**ID:** T1.1
**Priority:** High
**Estimated Time:** 30 minutes
**Status:** Pending
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Description:**
Implement `extractTranslatableTextRecursively()` function that recursively traverses element tree to extract translatable text.

**Acceptance Criteria:**
- [ ] Function created with correct signature
- [ ] Handles `LinearText` elements
- [ ] Recursively handles `LinearListItem` elements
- [ ] Handles `LinearBlockQuote` elements
- [ ] Ignores other element types
- [ ] KDoc comments added
- [ ] Code compiles without errors

**Implementation Notes:**
- Use depth-first traversal
- Maintain document order
- Filter for `blockStyle == TEXT`
- Skip blank text

#### Task 1.2: Update Main Extraction Function
**ID:** T1.2
**Priority:** High
**Estimated Time:** 20 minutes
**Status:** Pending
**Dependencies:** T1.1
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Description:**
Simplify `extractTranslatableParagraphs()` to call the new recursive helper.

**Acceptance Criteria:**
- [ ] Function calls `extractTranslatableTextRecursively()`
- [ ] Old when-expression logic removed
- [ ] Comments updated to reflect recursive behavior
- [ ] Code compiles without errors
- [ ] Function returns correct type

#### Task 1.3: Write Unit Tests for Extraction
**ID:** T1.3
**Priority:** High
**Estimated Time:** 40 minutes
**Status:** Pending
**Dependencies:** T1.1, T1.2
**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Description:**
Write comprehensive unit tests for the recursive extraction logic.

**Acceptance Criteria:**
- [ ] Test simple paragraph extraction
- [ ] Test 2-level nested list extraction
- [ ] Test 3-level nested list extraction
- [ ] Test blockquote text extraction
- [ ] Test multi-paragraph blockquote extraction
- [ ] Test mixed content extraction
- [ ] Test that code blocks are skipped
- [ ] All tests pass
- [ ] Code coverage > 80%

### Category 2: Rendering Updates (LinearArticleContent.kt)

#### Task 2.1: Update LinearListItemContent Signature
**ID:** T2.1
**Priority:** High
**Estimated Time:** 20 minutes
**Status:** Pending
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Update `LinearListItemContent` function signature to accept translation list and start index.

**Acceptance Criteria:**
- [ ] Parameter `translation` changed to `translations: List<String>?`
- [ ] Parameter `translationStartIndex: Int` added
- [ ] Default values specified
- [ ] Function documentation updated
- [ ] Code compiles without errors

#### Task 2.2: Implement Cursor Logic in LinearListItemContent
**ID:** T2.2
**Priority:** High
**Estimated Time:** 45 minutes
**Status:** Pending
**Dependencies:** T2.1
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Implement cursor tracking logic to consume translations from the list as text elements are rendered.

**Acceptance Criteria:**
- [ ] Variable `currentIndex` tracks position in translation list
- [ ] `LinearText` elements consume translation and advance cursor
- [ ] `LinearListItem` elements recurse with current index
- [ ] Cursor advanced by nested text count after recursion
- [ ] Other element types handled correctly
- [ ] Code compiles without errors
- [ ] No Compose compilation errors

#### Task 2.3: Create countTranslatableText Helper
**ID:** T2.3
**Priority:** Medium
**Estimated Time:** 15 minutes
**Status:** Pending
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Create helper function to count translatable text elements in a list.

**Acceptance Criteria:**
- [ ] Function `countTranslatableText()` created
- [ ] Counts `LinearText` with `blockStyle == TEXT`
- [ ] Filters blank text
- [ ] Returns correct count
- [ ] Documentation added
- [ ] Code compiles without errors

#### Task 2.4: Update LinearBlockQuoteContent Signature
**ID:** T2.4
**Priority:** High
**Estimated Time:** 15 minutes
**Status:** Pending
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Update `LinearBlockQuoteContent` to accept translation list and start index.

**Acceptance Criteria:**
- [ ] Parameter `translations: List<String>?` added
- [ ] Parameter `translationStartIndex: Int` added
- [ ] Default values specified
- [ ] Function documentation updated
- [ ] Code compiles without errors

#### Task 2.5: Implement Cursor Logic in LinearBlockQuoteContent
**ID:** T2.5
**Priority:** High
**Estimated Time:** 30 minutes
**Status:** Pending
**Dependencies:** T2.4
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Implement cursor tracking logic for blockquote content rendering.

**Acceptance Criteria:**
- [ ] Variable `currentIndex` tracks position
- [ ] `LinearText` elements consume translation
- [ ] `LinearListItem` elements recurse correctly
- [ ] Cursor advanced appropriately
- [ ] Existing cite rendering preserved
- [ ] Code compiles without errors
- [ ] No Compose compilation errors

### Category 3: Integration (LinearArticleContent.kt)

#### Task 3.1: Update linearArticleContent Function
**ID:** T3.1
**Priority:** High
**Estimated Time:** 30 minutes
**Status:** Pending
**Dependencies:** T2.1, T2.2, T2.4, T2.5
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Update the main `linearArticleContent` function to use new rendering approach with cursor tracking.

**Acceptance Criteria:**
- [ ] Variable `translationIndex` tracks cursor
- [ ] Old `computeParagraphIndices()` call removed
- [ ] `when` expression handles element types
- [ ] `LinearText` passes translation and advances cursor
- [ ] `LinearListItem` passes translation list and start index
- [ ] `LinearBlockQuote` passes translation list and start index
- [ ] Cursor advanced after each element
- [ ] Code compiles without errors
- [ ] No Compose compilation errors

#### Task 3.2: Remove Old Index Computation
**ID:** T3.2
**Priority:** Low
**Estimated Time:** 10 minutes
**Status:** Pending
**Dependencies:** T3.1
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Remove the old `computeParagraphIndices()` function as it's no longer needed.

**Acceptance Criteria:**
- [ ] `computeParagraphIndices()` function removed
- [ ] No references to it remain
- [ ] Code compiles without errors

#### Task 3.3: Review and Update LinearElementContent
**ID:** T3.3
**Priority:** Medium
**Estimated Time:** 15 minutes
**Status:** Pending
**Dependencies:** T3.1
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Description:**
Review `LinearElementContent` to ensure it works with new signatures.

**Acceptance Criteria:**
- [ ] `LinearElementContent` implementation reviewed
- [ ] Updates made if needed
- [ ] All call sites verified
- [ ] Code compiles without errors

### Category 4: Testing

#### Task 4.1: Run Unit Tests
**ID:** T4.1
**Priority:** High
**Estimated Time:** 15 minutes
**Status:** Pending
**Dependencies:** All implementation tasks

**Description:**
Run all unit tests to verify no regressions.

**Acceptance Criteria:**
- [ ] All existing unit tests pass
- [ ] New extraction tests pass
- [ ] No test failures
- [ ] 100% pass rate

#### Task 4.2: Build Verification
**ID:** T4.2
**Priority:** High
**Estimated Time:** 15 minutes
**Status:** Pending
**Dependencies:** All implementation tasks

**Description:**
Perform clean build to verify no compilation errors.

**Acceptance Criteria:**
- [ ] Clean build successful: `./gradlew clean`
- [ ] Full build successful: `./gradlew assembleDebug`
- [ ] No compilation errors
- [ ] No compilation warnings (or only pre-existing ones)

#### Task 4.3: Manual Testing - Nested Lists
**ID:** T4.3
**Priority:** High
**Estimated Time:** 20 minutes
**Status:** Pending
**Dependencies:** T4.1, T4.2

**Description:**
Test nested list translation with real article.

**Acceptance Criteria:**
- [ ] Test article loaded in app
- [ ] Translation button tapped
- [ ] All 6 list items (3 levels) show translations
- [ ] Translations appear below correct items
- [ ] No crashes or errors
- [ ] Performance acceptable

**Test Article:**
```html
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
```

#### Task 4.4: Manual Testing - Blockquotes
**ID:** T4.4
**Priority:** High
**Estimated Time:** 15 minutes
**Status:** Pending
**Dependencies:** T4.1, T4.2

**Description:**
Test blockquote translation with real article.

**Acceptance Criteria:**
- [ ] Test article loaded in app
- [ ] Translation button tapped
- [ ] Blockquote paragraphs are translated
- [ ] Regular paragraphs are translated
- [ ] Correct ordering maintained
- [ ] No crashes or errors

**Test Article:**
```html
<p>Regular paragraph before quote.</p>
<blockquote>
  <p>This is a quoted paragraph.</p>
  <p>This is another quoted paragraph.</p>
</blockquote>
<p>Regular paragraph after quote.</p>
```

#### Task 4.5: Manual Testing - Mixed Content
**ID:** T4.5
**Priority:** High
**Estimated Time:** 20 minutes
**Status:** Pending
**Dependencies:** T4.1, T4.2

**Description:**
Test complex article with nested lists, blockquotes, and regular paragraphs.

**Acceptance Criteria:**
- [ ] Test article loaded in app
- [ ] Translation button tapped
- [ ] All content types translated
- [ ] Correct translation placement
- [ ] No missing translations
- [ ] No crashes or errors

**Test Article:**
```html
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
```

### Category 5: Documentation

#### Task 5.1: Update Code Comments
**ID:** T5.1
**Priority:** Low
**Estimated Time:** 10 minutes
**Status:** Pending
**Dependencies:** All implementation tasks

**Description:**
Update code comments to reflect recursive behavior.

**Acceptance Criteria:**
- [ ] Outdated comments updated
- [ ] New behavior documented
- [ ] Complex logic explained
- [ ] No misleading comments remain

#### Task 5.2: Create Implementation Summary
**ID:** T5.2
**Priority:** Medium
**Estimated Time:** 20 minutes
**Status:** Pending
**Dependencies:** All testing tasks

**Description:**
Document implementation decisions and outcomes.

**Acceptance Criteria:**
- [ ] Implementation summary created
- [ ] Key decisions documented
- [ ] Challenges and solutions noted
- [ ] Test results included
- [ ] Future improvements listed

## Task Dependencies

```
T1.1 → T1.2 → T1.3
           ↓
T2.1 → T2.2 → T2.3
T2.4 → T2.5 ──────┐
           ↓     ↓
         T3.1 → T3.2 → T3.3
                  ↓
         T4.1 ←─────┘
         T4.2 ←─────┘
    T4.3 ←┘
    T4.4 ←┘
    T4.5 ←┘
    ↓
T5.1
T5.2
```

## Progress Tracking

**Completed Tasks:** 0/23 (0%)
**In Progress:** 0/23 (0%)
**Pending:** 23/23 (100%)

### Category Progress

- [ ] Category 1: Extraction Logic (0/3 tasks)
- [ ] Category 2: Rendering Updates (0/5 tasks)
- [ ] Category 3: Integration (0/3 tasks)
- [ ] Category 4: Testing (0/5 tasks)
- [ ] Category 5: Documentation (0/2 tasks)

## Task Checklist

Use this checklist to track progress during implementation:

### Phase 1: Extraction Logic
- [ ] T1.1: Create recursive helper
- [ ] T1.2: Update main extraction function
- [ ] T1.3: Write unit tests

### Phase 2: Rendering Updates
- [ ] T2.1: Update LinearListItemContent signature
- [ ] T2.2: Implement cursor logic in LinearListItemContent
- [ ] T2.3: Create countTranslatableText helper
- [ ] T2.4: Update LinearBlockQuoteContent signature
- [ ] T2.5: Implement cursor logic in LinearBlockQuoteContent

### Phase 3: Integration
- [ ] T3.1: Update linearArticleContent function
- [ ] T3.2: Remove old index computation
- [ ] T3.3: Review LinearElementContent

### Phase 4: Testing
- [ ] T4.1: Run unit tests
- [ ] T4.2: Build verification
- [ ] T4.3: Manual testing - nested lists
- [ ] T4.4: Manual testing - blockquotes
- [ ] T4.5: Manual testing - mixed content

### Phase 5: Documentation
- [ ] T5.1: Update code comments
- [ ] T5.2: Create implementation summary

## Notes

1. **Task Order:** Follow dependency order
2. **Testing:** Don't skip manual testing
3. **Compilation:** Compile after each task
4. **Documentation:** Document as you go
5. **Time Management:** Tasks may take longer than estimated

---

**Task List Complete**
**Total Tasks:** 23
**Estimated Effort:** 6 hours
**Ready for Phase 7 (Specification Review)**
