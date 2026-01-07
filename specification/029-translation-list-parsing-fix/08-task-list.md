# Task List - Translation List Parsing Fix

**Spec Index:** 029
**Feature Name:** Fix Translation List Parsing - Missing First Paragraph and Incorrect Matching
**Date:** 2026-01-07
**Phase:** 6 - Specification Writing
**Status:** Ready for Execution

## Task Summary

| Phase | Tasks | Completed | Blocked |
|-------|-------|-----------|---------|
| Phase 1: Core Implementation | 4 | 0 | 0 |
| Phase 2: Testing | 2 | 0 | 0 |
| Phase 3: Validation | 3 | 0 | 0 |
| **Total** | **9** | **0** | **0** |

---

## Phase 1: Core Implementation

### Task 1.1: Add Flattening Helper Functions
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 30 minutes
**Assignee:** Developer

**Description:**
Add two new helper functions to `LinearArticleContent.kt`:
- `buildFlatTranslatableList()` - Main entry point for flattening
- `flattenTranslatableElement()` - Recursive traversal function

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Open `LinearArticleContent.kt`
2. Locate line 163 (after `ParagraphCounter` class)
3. Add `buildFlatTranslatableList()` function with full documentation
4. Add `flattenTranslatableElement()` function with full documentation
5. Verify code compiles

**Acceptance Criteria:**
- [ ] Both functions added to file
- [ ] Functions compile without errors
- [ ] Functions have comprehensive KDoc comments
- [ ] Type signatures are correct

**Dependencies:** None

**Blocked By:** None

---

### Task 1.2: Rewrite computeParagraphIndices Function
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 30 minutes
**Assignee:** Developer

**Description:**
Replace the existing `computeParagraphIndices()` function with a new implementation that uses the flattening helpers.

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Open `LinearArticleContent.kt`
2. Locate lines 137-153 (current `computeParagraphIndices` function)
3. Replace entire function with new implementation
4. Add validation logging at end of function
5. Verify code compiles

**Acceptance Criteria:**
- [ ] Old function completely replaced
- [ ] New function uses `buildFlatTransatableList()`
- [ ] New function assigns translation indices correctly
- [ ] Validation logging added
- [ ] Function compiles without errors

**Dependencies:** Task 1.1

**Blocked By:** Task 1.1

---

### Task 1.3: Remove Old computeParagraphIndexRecursive Function
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 30 minutes
**Assignee:** Developer

**Description:**
Delete the old `computeParagraphIndexRecursive()` function which is no longer needed with the new approach.

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Open `LinearArticleContent.kt`
2. Locate lines 169-237 (old `computeParagraphIndexRecursive` function)
3. Delete entire function including comments
4. Search for any references to this function
5. Verify no references remain
6. Verify code compiles

**Acceptance Criteria:**
- [ ] Old function completely removed
- [ ] No references to function remain in codebase
- [ ] Code compiles without errors
- [ ] No warnings about unused code

**Dependencies:** Task 1.2

**Blocked By:** Task 1.2

---

### Task 1.4: Add Validation and Logging
**Status:** ⏳ Pending
**Priority:** MEDIUM
**Effort:** 30 minutes
**Assignee:** Developer

**Description:**
Add validation logging to `computeParagraphIndices()` to help with debugging and monitoring.

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Open `LinearArticleContent.kt`
2. Locate end of `computeParagraphIndices()` function
3. Add log statement with computed count
4. Add warning log if counts don't match
5. Verify logs are at appropriate level (DEBUG)
6. Verify code compiles

**Acceptance Criteria:**
- [ ] Debug log added with translation count
- [ ] Warning log added for count mismatches
- [ ] Logs use existing `LOG_TAG` constant
- [ ] Log messages are clear and actionable
- [ ] Code compiles without errors

**Dependencies:** Task 1.2

**Blocked By:** Task 1.2

---

## Phase 2: Testing

### Task 2.1: Add Test Helper Functions
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 30 minutes
**Assignee:** QA Engineer

**Description:**
Add helper functions to `HtmlLinearizerTest.kt` to support translation testing.

**Files:**
- `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Steps:**
1. Open `HtmlLinearizerTest.kt`
2. Add `extractTranslatableParagraphs()` helper function
3. Add `extractTranslatableTextRecursively()` helper function
4. Verify functions match ArticleViewModel logic
5. Verify code compiles

**Acceptance Criteria:**
- [ ] Helper functions added to test file
- [ ] Functions mirror ArticleViewModel logic
- [ ] Functions compile without errors
- [ ] Functions have appropriate documentation

**Dependencies:** None

**Blocked By:** None

---

### Task 2.2: Add Comprehensive Test Cases
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 90 minutes
**Assignee:** QA Engineer

**Description:**
Add comprehensive test cases for multi-paragraph list items, nested lists, and mixed content.

**Files:**
- `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Steps:**
1. Create `TranslationListParsingTest` test class
2. Add test: `single paragraph list item gets correct translation`
3. Add test: `multi-paragraph list item gets correct translations`
4. Add test: `nested lists get correct translations`
5. Add test: `mixed content with lists and blockquotes gets correct translations`
6. Add test: `empty list item gets no translation`
7. Add test: `code blocks are not translated`
8. Run all tests and verify they pass
9. Verify code compiles

**Acceptance Criteria:**
- [ ] Test class created with 6 test methods
- [ ] All tests compile without errors
- [ ] All tests pass
- [ ] Edge cases are covered
- [ ] Tests are well-documented
- [ ] Tests follow project testing patterns

**Dependencies:** Task 2.1

**Blocked By:** Task 2.1

---

## Phase 3: Validation

### Task 3.1: Manual Testing
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 30 minutes
**Assignee:** QA Engineer

**Description:**
Perform manual testing to verify the fix works correctly in the application.

**Files:** N/A (Application testing)

**Steps:**
1. Build and install the application
2. Navigate to a feed with list items
3. Verify lists render correctly without crashes
4. Find an article with multi-paragraph list items
5. Enable translation for the feed
6. Verify each paragraph shows correct translation
7. Verify no missing or duplicate translations
8. Find an article with nested lists
9. Verify all list items show correct translations
10. Document any issues found

**Acceptance Criteria:**
- [ ] Application launches without crashes
- [ ] Lists render correctly
- [ ] Multi-paragraph list items show correct translations
- [ ] Nested lists work correctly
- [ ] No visual regressions
- [ ] No performance issues
- [ ] Test results documented

**Dependencies:** Tasks 1.1-1.4, 2.1-2.2

**Blocked By:** All Phase 1 and Phase 2 tasks

---

### Task 3.2: Performance Testing
**Status:** ⏳ Pending
**Priority:** MEDIUM
**Effort:** 15 minutes
**Assignee:** Developer

**Description:**
Verify that the changes don't negatively impact performance.

**Files:**
- `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Steps:**
1. Add performance benchmark test
2. Generate large article with 100 elements
3. Measure time for index computation
4. Verify time is < 10ms
5. Verify no memory leaks
6. Document performance results

**Acceptance Criteria:**
- [ ] Performance test added
- [ ] Index computation < 10ms for typical articles
- [ ] No memory leaks detected
- [ ] Performance results documented
- [ ] No performance regressions

**Dependencies:** Task 2.2

**Blocked By:** Task 2.2

---

### Task 3.3: Code Review
**Status:** ⏳ Pending
**Priority:** HIGH
**Effort:** 15 minutes
**Assignee:** Tech Lead

**Description:**
Review all code changes to ensure quality and maintainability.

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`
- `app/src/test/java/com/nononsenseapps/feeder/model/html/HtmlLinearizerTest.kt`

**Steps:**
1. Review code changes in LinearArticleContent.kt
2. Review test code in HtmlLinearizerTest.kt
3. Verify code follows project style guidelines
4. Verify all functions have clear documentation
5. Verify error handling is appropriate
6. Verify tests are comprehensive
7. Verify no code duplication
8. Verify performance is acceptable
9. Verify backward compatibility is maintained
10. Approve or request changes

**Acceptance Criteria:**
- [ ] Code follows project style guidelines
- [ ] All functions have clear documentation
- [ ] Error handling is appropriate
- [ ] Tests are comprehensive
- [ ] No code duplication
- [ ] Performance is acceptable
- [ ] Backward compatibility is maintained
- [ ] Code review approved

**Dependencies:** All Phase 1 and Phase 2 tasks

**Blocked By:** All Phase 1 and Phase 2 tasks

---

## Task Dependencies

```
Phase 1: Core Implementation
├─ Task 1.1 (Add helpers) ─┐
├─ Task 1.2 (Rewrite main) ├─┐
│                          │ │
├─ Task 1.3 (Remove old) ───┘ │
│                             │
└─ Task 1.4 (Add logging) ────┘

Phase 2: Testing
├─ Task 2.1 (Test helpers) ─┐
│                           │
└─ Task 2.2 (Add tests) ────┘

Phase 3: Validation
├─ Task 3.1 (Manual testing) ─┐
├─ Task 3.2 (Performance) ────┼─┐
│                            │ │
└─ Task 3.3 (Code review) ───┘ │
└─────────────────────────────┘
   (Depends on Phase 1 & 2)
```

---

## Progress Tracking

### Phase 1: Core Implementation
- [ ] Task 1.1: Add Flattening Helper Functions
- [ ] Task 1.2: Rewrite computeParagraphIndices Function
- [ ] Task 1.3: Remove Old computeParagraphIndexRecursive Function
- [ ] Task 1.4: Add Validation and Logging

**Phase 1 Progress:** 0/4 tasks complete (0%)

### Phase 2: Testing
- [ ] Task 2.1: Add Test Helper Functions
- [ ] Task 2.2: Add Comprehensive Test Cases

**Phase 2 Progress:** 0/2 tasks complete (0%)

### Phase 3: Validation
- [ ] Task 3.1: Manual Testing
- [ ] Task 3.2: Performance Testing
- [ ] Task 3.3: Code Review

**Phase 3 Progress:** 0/3 tasks complete (0%)

### Overall Progress
**Total:** 0/9 tasks complete (0%)

---

## Notes

### Implementation Notes
- All code changes are in `LinearArticleContent.kt`
- All test changes are in `HtmlLinearizerTest.kt`
- No changes to data structures or APIs
- Backward compatible with existing functionality

### Testing Notes
- Tests should cover both single and multi-paragraph list items
- Tests should cover nested structures
- Tests should verify synchronization with extraction logic
- Performance tests should verify < 10ms target

### Validation Notes
- Manual testing is critical to verify UI rendering
- Performance testing should verify no regressions
- Code review should ensure maintainability

---

**End of Task List**
