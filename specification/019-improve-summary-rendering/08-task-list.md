# Task List - Spec 019: Markdown Rendering for Summaries

**Created:** January 5, 2026
**Status:** Ready for Execution

## Overview

This document contains the complete task list for implementing markdown rendering in AI-generated article summaries. Tasks are organized by phase and priority.

## Task Summary

- **Total Tasks:** 18
- **Estimated Time:** 3-5 days
- **Complexity:** Low-Medium
- **Risk Level:** Low

## Task List

### Phase 1: Implementation (9 tasks)

#### T1.1: Create MarkdownToAnnotatedString.kt
**Priority:** P0 (Critical)
**Estimated Time:** 2 hours
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Create new file `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt`

**Subtasks:**
- [ ] Create file with package declaration
- [ ] Add imports (JetBrains Markdown, Jsoup)
- [ ] Implement `parseMarkdownToHTML()` function
- [ ] Implement `createMarkdownCleaner()` function with whitelist
- [ ] Implement `sanitizeHTML()` function
- [ ] Implement `markdownToAnnotatedString()` function
- [ ] Implement `markdownToAnnotatedStringSafe()` function
- [ ] Add KDoc documentation
- [ ] Add error handling

**Acceptance Criteria:**
- File created at correct path
- All functions implemented
- Code follows ktlint style
- KDoc documentation complete

**Dependencies:** None

---

#### T1.2: Update ArticleScreen.kt - Add Imports
**Priority:** P0 (Critical)
**Estimated Time:** 5 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Add necessary imports to `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Subtasks:**
- [ ] Add import for `markdownToAnnotatedStringSafe`
- [ ] Verify no import conflicts

**Acceptance Criteria:**
- Imports added correctly
- No import errors

**Dependencies:** T1.1

---

#### T1.3: Update ArticleScreen.kt - Modify SummarySection
**Priority:** P0 (Critical)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Update `SummarySection` composable to render markdown instead of plain text

**Subtasks:**
- [ ] Replace `Text(summary.value.content)` with `MarkdownText`
- [ ] Test rendering with sample markdown

**Acceptance Criteria:**
- SummarySection updated
- Markdown renders correctly
- Loading state still works
- Error state still works

**Dependencies:** T1.1, T1.2

---

#### T1.4: Add MarkdownText Composable
**Priority:** P0 (Critical)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Create `MarkdownText` composable in ArticleScreen.kt

**Subtasks:**
- [ ] Create `@Composable MarkdownText()` function
- [ ] Add `remember` caching for AnnotatedString
- [ ] Add `Text` composable with Material 3 styling
- [ ] Add error handling

**Acceptance Criteria:**
- Composable created
- Caching implemented
- Material 3 styling applied
- Error handling in place

**Dependencies:** T1.1

---

#### T1.5: Write Unit Tests - Markdown Parsing
**Priority:** P1 (High)
**Estimated Time:** 2 hours
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Create `app/src/test/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedStringTest.kt`

**Subtasks:**
- [ ] Create test file
- [ ] Test bold text rendering
- [ ] Test italic text rendering
- [ ] Test link rendering
- [ ] Test list rendering
- [ ] Test code block rendering
- [ ] Test header rendering
- [ ] Test blockquote rendering

**Acceptance Criteria:**
- Test file created
- All tests pass
- Coverage > 80%

**Dependencies:** T1.1

---

#### T1.6: Write Security Tests
**Priority:** P0 (Critical)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Add security tests to prevent XSS vulnerabilities

**Subtasks:**
- [ ] Test script tag removal
- [ ] Test onclick attribute removal
- [ ] Test iframe removal
- [ ] Test JavaScript protocol blocking
- [ ] Test HTML injection prevention

**Acceptance Criteria:**
- All security tests pass
- No XSS vulnerabilities

**Dependencies:** T1.1

---

#### T1.7: Write Error Handling Tests
**Priority:** P1 (High)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Test error handling and fallback behavior

**Subtasks:**
- [ ] Test malformed markdown
- [ ] Test empty markdown
- [ ] Test null handling
- [ ] Test exception handling
- [ ] Verify plain text fallback

**Acceptance Criteria:**
- All error tests pass
- Graceful fallback verified

**Dependencies:** T1.1

---

#### T1.8: Verify Build
**Priority:** P0 (Critical)
**Estimated Time:** 15 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Ensure project builds successfully

**Subtasks:**
- [ ] Run `./gradlew assembleDebug`
- [ ] Fix any build errors
- [ ] Run `./gradlew lint`
- [ ] Fix any lint warnings

**Acceptance Criteria:**
- Build succeeds
- No lint warnings

**Dependencies:** T1.1, T1.2, T1.3, T1.4

---

#### T1.9: Run All Tests
**Priority:** P0 (Critical)
**Estimated Time:** 15 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Execute all tests to verify implementation

**Subtasks:**
- [ ] Run unit tests
- [ ] Run integration tests
- [ ] Fix any failing tests
- [ ] Verify test coverage

**Acceptance Criteria:**
- All tests pass
- Coverage > 80%

**Dependencies:** T1.5, T1.6, T1.7, T1.8

---

### Phase 2: QA Testing (4 tasks)

#### T2.1: Manual Testing - Basic Markdown
**Priority:** P1 (High)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** QA Agent

**Description:**
Manual testing of markdown rendering with various elements

**Subtasks:**
- [ ] Test bold text
- [ ] Test italic text
- [ ] Test links (tap to open)
- [ ] Test lists (ordered and unordered)
- [ ] Test headers (H1-H6)
- [ ] Test code blocks
- [ ] Test blockquotes

**Acceptance Criteria:**
- All markdown elements render correctly
- Links are tappable
- No visual regressions

**Dependencies:** T1.9

---

#### T2.2: Manual Testing - Themes
**Priority:** P1 (High)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** QA Agent

**Description:**
Test markdown rendering in different themes

**Subtasks:**
- [ ] Test in light mode
- [ ] Test in dark mode
- [ ] Verify color contrast
- [ ] Verify all elements visible

**Acceptance Criteria:**
- Works in light mode
- Works in dark mode
- Sufficient contrast in both modes

**Dependencies:** T1.9

---

#### T2.3: Manual Testing - Edge Cases
**Priority:** P2 (Medium)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** QA Agent

**Description:**
Test edge cases and unusual markdown

**Subtasks:**
- [ ] Test very long summary (> 1000 words)
- [ ] Test nested markdown
- [ ] Test malformed markdown
- [ ] Test mixed LTR/RTL content
- [ ] Test special characters

**Acceptance Criteria:**
- No crashes
- Graceful handling of edge cases
- Smooth scrolling

**Dependencies:** T1.9

---

#### T2.4: Manual Testing - Accessibility
**Priority:** P1 (High)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** QA Agent

**Description:**
Verify accessibility with screen reader

**Subtasks:**
- [ ] Test with TalkBack enabled
- [ ] Verify headings announced
- [ ] Verify links announced
- [ ] Verify lists announced
- [ ] Verify text scaling

**Acceptance Criteria:**
- Screen reader reads formatted content
- All elements accessible
- Text scaling works

**Dependencies:** T1.9

---

### Phase 3: Code Review (3 tasks)

#### T3.1: Self-Review
**Priority:** P0 (Critical)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Review own code before submitting for code review

**Subtasks:**
- [ ] Check for TODO comments
- [ ] Check for debug code
- [ ] Verify ktlint compliance
- [ ] Verify documentation
- [ ] Check for unused imports

**Acceptance Criteria:**
- No TODO/FIXME comments
- No debug code
- ktlint compliant
- Documentation complete

**Dependencies:** T1.9

---

#### T3.2: Code Review
**Priority:** P0 (Critical)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** Code Reviewer Agent

**Description:**
Perform specification-aware code review

**Subtasks:**
- [ ] Review MarkdownToAnnotatedString.kt
- [ ] Review ArticleScreen.kt changes
- [ ] Review test coverage
- [ ] Review security implementation
- [ ] Verify acceptance criteria
- [ ] Check for potential issues
- [ ] Provide feedback

**Acceptance Criteria:**
- Code meets specification
- No critical issues found
- Tests adequate
- Security verified

**Dependencies:** T3.1

---

#### T3.3: Address Review Feedback
**Priority:** P0 (Critical)
**Estimated Time:** 1 hour
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Address feedback from code review

**Subtasks:**
- [ ] Review feedback
- [ ] Make necessary changes
- [ ] Re-run tests
- [ ] Verify fixes

**Acceptance Criteria:**
- All feedback addressed
- Tests still pass
- No new issues introduced

**Dependencies:** T3.2

---

### Phase 4: Documentation & Cleanup (2 tasks)

#### T4.1: Update Documentation
**Priority:** P1 (High)
**Estimated Time:** 30 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Update project documentation

**Subtasks:**
- [ ] Update CHANGELOG.md
- [ ] Add inline code comments (if needed)
- [ ] Verify external documentation

**Acceptance Criteria:**
- Changelog updated
- Code documented
- No outdated docs

**Dependencies:** T3.3

---

#### T4.2: Cleanup
**Priority:** P2 (Medium)
**Estimated Time:** 15 minutes
**Status:** Pending
**Assigned To:** Dev Agent

**Description:**
Clean up temporary files and artifacts

**Subtasks:**
- [ ] Remove debug code
- [ ] Remove temporary files
- [ ] Verify clean git status
- [ ] Check for unused imports

**Acceptance Criteria:**
- No temporary files
- No debug code
- Clean working directory

**Dependencies:** T4.1

---

## Task Dependencies

```
T1.1 (MarkdownToAnnotatedString.kt)
├─ T1.2 (Add imports)
│  └─ T1.3 (Modify SummarySection)
├─ T1.4 (MarkdownText composable)
├─ T1.5 (Unit tests)
├─ T1.6 (Security tests)
├─ T1.7 (Error tests)
└─ T1.8 (Verify build)
   └─ T1.9 (Run tests)
      ├─ T2.1-T2.4 (QA tests)
      └─ T3.1 (Self-review)
         └─ T3.2 (Code review)
            └─ T3.3 (Address feedback)
               └─ T4.1 (Documentation)
                  └─ T4.2 (Cleanup)
```

## Progress Tracking

### Phase 1: Implementation
- [ ] T1.1: Create MarkdownToAnnotatedString.kt
- [ ] T1.2: Add imports
- [ ] T1.3: Modify SummarySection
- [ ] T1.4: Add MarkdownText composable
- [ ] T1.5: Write unit tests
- [ ] T1.6: Write security tests
- [ ] T1.7: Write error tests
- [ ] T1.8: Verify build
- [ ] T1.9: Run all tests

### Phase 2: QA Testing
- [ ] T2.1: Manual testing - basic markdown
- [ ] T2.2: Manual testing - themes
- [ ] T2.3: Manual testing - edge cases
- [ ] T2.4: Manual testing - accessibility

### Phase 3: Code Review
- [ ] T3.1: Self-review
- [ ] T3.2: Code review
- [ ] T3.3: Address feedback

### Phase 4: Documentation & Cleanup
- [ ] T4.1: Update documentation
- [ ] T4.2: Cleanup

## Completion Criteria

### Must Complete (P0)
- All P0 tasks complete
- All tests pass
- Build succeeds
- No critical issues

### Should Complete (P1)
- All P1 tasks complete
- Manual testing done
- Accessibility verified

### Nice to Have (P2)
- All P2 tasks complete
- Edge cases tested
- Performance optimized

## Blocked Issues

*None identified*

## Notes

- Tasks can be done in parallel where dependencies allow
- T2.1-T2.4 can run in parallel with T3.1
- Total time may vary based on complexity
- Buffer time included for unforeseen issues

---

**Task List Status:** ✅ Complete and ready for execution
