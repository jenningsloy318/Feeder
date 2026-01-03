# Task List - Article Translation Button Feature

**Spec Index:** 013
**Feature Name:** Article Translation Button
**Date:** 2026-01-03
**Status:** Ready for Execution

## Task Overview

This document breaks down the implementation of the article translation button feature into 20 specific, actionable tasks. Each task includes implementation details, testing requirements, and acceptance criteria.

## Task Summary

- **Total Tasks:** 20
- **Total Estimated Time:** 10-11 hours
- **Files Modified:** 4
- **Test Cases Added:** 12+
- **Risk Level:** Low

## Task List

### Task 1: Create Translation State Models
**ID:** T-001
**File:** `ArticleViewModel.kt`
**Estimate:** 15 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Define `TranslationState` sealed interface with Empty, Loading, Result
- Define `TranslationResult` sealed class with Success, Error
- Add KDoc documentation

**Acceptance Criteria:**
- [ ] Sealed class structure matches design
- [ ] All states defined correctly
- [ ] Code compiles without errors

**Testing:**
- Compile check only

---

### Task 2: Add Translation State to ViewModel
**ID:** T-002
**File:** `ArticleViewModel.kt`
**Estimate:** 20 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add `translationState: MutableStateFlow<TranslationState>`
- Add to `combine()` flow in viewState
- Add property to `ArticleScreenViewState` interface
- Add property to `ArticleState` data class with default Empty

**Acceptance Criteria:**
- [ ] State is MutableStateFlow
- [ ] Combined in viewState correctly
- [ ] Default value is Empty
- [ ] Code compiles

**Testing:**
- Verify state flow initialization
- Check default value

---

### Task 3: Implement translate() Method
**ID:** T-003
**File:** `ArticleViewModel.kt`
**Estimate:** 30 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Create `translate()` method with coroutine
- Create `extractTranslatableParagraphs()` helper
- Add try-catch error handling
- Add logging for debugging
- Update state: Loading → Result

**Acceptance Criteria:**
- [ ] Method launches coroutine in viewModelScope
- [ ] Uses Dispatchers.IO
- [ ] Extracts only LinearText elements
- [ ] Handles errors gracefully
- [ ] Updates state correctly

**Testing:**
- Unit test state transitions
- Test paragraph extraction
- Test error handling

---

### Task 4: Add Translation Button to ArticleScreen
**ID:** T-004
**File:** `ArticleScreen.kt` (inner)
**Estimate:** 20 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add IconButton with Icons.Default.Translate
- Wrap in PlainTooltipBox
- Position after "Summarize" button
- Disable when translationState is Loading
- Add contentDescription

**Acceptance Criteria:**
- [ ] Button visible in top app bar
- [ ] Positioned correctly (after Summarize)
- [ ] Disabled during loading
- [ ] Has tooltip
- [ ] Has content description

**Testing:**
- UI test for visibility
- Test enabled/disabled states
- Verify position

---

### Task 5: Pass onTranslate Through Screen Hierarchy
**ID:** T-005
**File:** `ArticleScreen.kt` (both composables)
**Estimate:** 10 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add `onTranslate: () -> Unit` to outer ArticleScreen
- Pass to inner ArticleScreen
- Connect to `viewModel.translate()`

**Acceptance Criteria:**
- [ ] Lambda added to both composables
- [ ] Connected to ViewModel correctly
- [ ] No compilation errors

**Testing:**
- Test button click triggers translate()

---

### Task 6: Create TranslationStatusSection Composable
**ID:** T-006
**File:** `ArticleScreen.kt`
**Estimate:** 25 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Create composable with TranslationState parameter
- Handle Empty: return nothing
- Handle Loading: show LinearProgressIndicator
- Handle Result: check for Error or Success

**Acceptance Criteria:**
- [ ] Empty state shows nothing
- [ ] Loading shows progress indicator
- [ ] Result checks nested type
- [ ] Follows existing SummarySection pattern

**Testing:**
- Test each state renders correctly
- Verify loading indicator animates

---

### Task 7: Create TranslationErrorSection Composable
**ID:** T-007
**File:** `ArticleScreen.kt`
**Estimate:** 15 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Create OutlinedCard
- Add error icon (Icons.Outlined.ErrorOutline)
- Add error title text
- Add error message text
- No retry button (users tap translate button to retry)
- Proper styling and spacing

**Acceptance Criteria:**
- [ ] Card displays error message
- [ ] Error icon visible
- [ ] Styling matches design spec
- [ ] Handles long messages

**Testing:**
- Test error card appearance

---

### Task 8: Add Translation Status to ArticleContent
**ID:** T-008
**File:** `ArticleScreen.kt`
**Estimate:** 15 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add TranslationStatusSection to ArticleContent
- Position after AI summary section

**Acceptance Criteria:**
- [ ] Status section appears in content
- [ ] Positioned after summary
- [ ] Offset counter updated

**Testing:**
- Verify section appears
- Test loading and error states

---

### Task 9: Modify linearArticleContent for Translations
**ID:** T-009
**File:** `LinearArticleContent.kt`
**Estimate:** 30 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add `translatedParagraphs: List<String>?` parameter
- Add `textElementIndex` counter variable
- Match translations to text elements by index
- Pass translation to LinearElementContent

**Acceptance Criteria:**
- [ ] Parameter optional (nullable)
- [ ] Index tracking works correctly
- [ ] Translations matched to correct paragraphs
- [ ] Handles mismatched counts

**Testing:**
- Test with various paragraph counts
- Test with null translations
- Test index matching

---

### Task 10: Modify LinearTextContent for Translations
**ID:** T-010
**File:** `LinearArticleContent.kt`
**Estimate:** 25 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Change from Box to Column layout
- Add `translation: String?` parameter
- Display original text first
- Display translation if not null
- Style translation: bodyMedium, italic, secondary color, 16dp indent

**Acceptance Criteria:**
- [ ] Original text displays correctly
- [ ] Translation displays below when present
- [ ] Null translation handled
- [ ] Styling matches design
- [ ] Spacing correct (8dp gap)

**Testing:**
- Test with translation
- Test without translation
- Verify styling
- Test bidi text

---

### Task 11: Modify LinearElementContent Signature
**ID:** T-011
**File:** `LinearArticleContent.kt`
**Estimate:** 10 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add `translation: String?` parameter
- Pass to LinearTextContent when element is LinearText
- Ignore for other element types

**Acceptance Criteria:**
- [ ] Parameter added to signature
- [ ] Passed to text content
- [ ] Other elements unaffected
- [ ] No breaking changes

**Testing:**
- Compile check
- Test with different element types

---

### Task 12: Add translate() to AIApi Interface
**ID:** T-012
**File:** `AIApi.kt`
**Estimate:** 15 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add suspend function `translate()`
- Add TranslationResult sealed class
- Add Success and Error types
- Add KDoc documentation

**Acceptance Criteria:**
- [ ] Method signature correct
- [ ] Return type defined
- [ ] Documentation complete
- [ ] Code compiles

**Testing:**
- Compile check only

---

### Task 13: Implement Dummy Translation
**ID:** T-013
**File:** AIApi implementation
**Estimate:** 15 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Override translate() method
- Return TranslationResult.Success
- Prefix each paragraph with language name
- Handle empty list

**Acceptance Criteria:**
- [ ] Returns correct type
- [ ] Prefix format correct
- [ ] Handles empty list
- [ ] Works with all languages

**Testing:**
- Test with various paragraph counts
- Test with different languages
- Verify output format

---

### Task 14: Connect Translation to ArticleContent
**ID:** T-014
**File:** `ArticleScreen.kt` (ArticleContent)
**Estimate:** 15 minutes
**Priority:** P0 (Must Has)

**Implementation:**
- Extract translatedParagraphs from translationState
- Pass to linearArticleContent()
- Handle null case

**Acceptance Criteria:**
- [ ] Translations extracted correctly
- [ ] Passed to content renderer
- [ ] Null case handled
- [ ] State change triggers recomposition

**Testing:**
- Test extraction logic
- Verify null handling

---

### Task 15: Add String Resources
**ID:** T-015
**File:** `strings.xml`
**Estimate:** 5 minutes
**Priority:** P0 (Must Have)

**Implementation:**
- Add "translate" string
- Add "translation_error" string
- Add "translate_article_content_description"

**Acceptance Criteria:**
- [ ] All strings added
- [ ] No duplicates
- [ ] Proper naming convention
- [ ] Ready for localization

**Testing:**
- Verify strings exist in UI
- Check for missing translations

---

### Task 16: Write Unit Tests
**ID:** T-016
**File:** `ArticleViewModelTest.kt`
**Estimate:** 1 hour
**Priority:** P1 (Should Have)

**Implementation:**
- Test translate() state transitions
- Test extractTranslatableParagraphs()
- Test empty article handling
- Test error handling
- Test settings integration

**Acceptance Criteria:**
- [ ] At least 5 unit tests
- [ ] Coverage ≥ 80% for ViewModel
- [ ] All tests pass
- [ ] Edge cases covered

**Testing:**
- Run tests with `./gradlew test`
- Verify coverage report

---

### Task 17: Write UI Tests
**ID:** T-017
**File:** `ArticleScreenTest.kt`
**Estimate:** 1 hour
**Priority:** P1 (Should Have)

**Implementation:**
- Test button visibility
- Test button click action
- Test loading state
- Test translation display
- Test error state (users tap translate button to retry)

**Acceptance Criteria:**
- [ ] At least 5 UI tests
- [ ] Key user flows covered
- [ ] All tests pass
- [ ] No flakiness

**Testing:**
- Run tests with `./gradlew connectedAndroidTest`
- Test on emulator/device

---

### Task 18: Manual Testing
**ID:** T-018
**Files:** Multiple
**Estimate:** 2 hours
**Priority:** P1 (Should Have)

**Test Plan:**
- Test on phone (various sizes)
- Test on tablet
- Test light theme
- Test dark theme
- Test with screen reader
- Test error scenarios
- Test long articles
- Test mixed content

**Acceptance Criteria:**
- [ ] All scenarios tested
- [ ] No crashes found
- [ ] Performance acceptable
- [ ] Bugs documented and fixed

**Testing:**
- Execute test plan
- Document results
- Fix issues found

---

### Task 19: Code Review and Polish
**ID:** T-019
**Files:** All modified files
**Estimate:** 1 hour
**Priority:** P1 (Should Have)

**Checklist:**
- Remove TODO/FIXME comments
- Remove debug code (println, etc.)
- Verify error handling complete
- Verify logging added
- Check documentation completeness
- Format code with ktlint
- Check for code smells

**Acceptance Criteria:**
- [ ] No TODO comments
- [ ] No debug code
- [ ] Proper error handling
- [ ] Logging present
- [ ] Documentation complete
- [ ] Code formatted
- [ ] No obvious code smells

**Testing:**
- Self-review all changes
- Run ktlint check
- Verify conventions followed

---

### Task 20: Build and Verification
**ID:** T-020
**Project:** Root
**Estimate:** 30 minutes
**Priority:** P0 (Must Have)

**Commands:**
```bash
./gradlew clean
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
```

**Acceptance Criteria:**
- [ ] Clean build succeeds
- [ ] Debug build succeeds
- [ ] All tests pass
- [ ] No lint errors
- [ ] No new warnings introduced
- [ ] APK size reasonable

**Testing:**
- Execute build commands
- Verify all outputs
- Check for issues

---

## Task Dependencies

```
T-001 (Create State Models)
    ↓
T-002 (Add State to ViewModel)
    ↓
T-003 (Implement translate())
    ↓
T-012 (Add API Method)
    ↓
T-013 (Implement Dummy API)
    ↓
T-004 (Add Button) + T-005 (Pass Handler)
    ↓
T-006 (Status Section) + T-007 (Error Section)
    ↓
T-008 (Add to ArticleContent)
    ↓
T-009 (Modify Content Renderer)
    ↓
T-010 (Modify Text Display)
    ↓
T-011 (Modify Element Signature)
    ↓
T-014 (Connect Translation)
    ↓
T-015 (Add Strings)
    ↓
T-016 (Unit Tests) + T-017 (UI Tests)
    ↓
T-018 (Manual Testing)
    ↓
T-019 (Code Review)
    ↓
T-020 (Build Verification)
```

## Progress Tracking

| Phase | Tasks | Status |
|-------|-------|--------|
| State & ViewModel | T-001, T-002, T-003 | Pending |
| UI Components | T-004, T-005, T-006, T-007, T-008 | Pending |
| Content Rendering | T-009, T-010, T-011 | Pending |
| API Integration | T-012, T-013, T-014 | Pending |
| Strings & I18N | T-015 | Pending |
| Testing | T-016, T-017, T-018 | Pending |
| Polish & Build | T-019, T-020 | Pending |

## Completion Criteria

**Feature Complete When:**
- [ ] All 20 tasks completed
- [ ] All acceptance criteria met
- [ ] All tests passing
- [ ] Manual testing successful
- [ ] Code reviewed
- [ ] Build succeeds
- [ ] Ready for Phase 9 (Code Review)

---

**Task List Complete**
**Ready for Phase 8 (Execution & QA)**
**Total Implementation Time:** 10-11 hours
**Risk Assessment:** Low
**Confidence Level:** High
