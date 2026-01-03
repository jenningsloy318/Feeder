# Implementation Plan - Article Translation Button Feature

**Spec Index:** 013
**Feature Name:** Article Translation Button
**Date:** 2026-01-03
**Phase:** 6 - Implementation Plan
**Status:** Final

## 1. Project Overview

### 1.1 Objective

Implement an article translation button in the Feeder RSS reader app that allows users to manually translate article content. Translations are displayed paragraph-by-paragraph below the original text using a dummy translation function (real AI implementation deferred to future spec).

### 1.2 Success Metrics

- Translation button functional in article screen
- Loading indicator displays during translation
- Translations appear below original paragraphs
- Error handling works with retry mechanism
- Code compiles and all tests pass
- No regressions in existing functionality

### 1.3 Timeline Estimate

**Total Duration:** 3 days
- Day 1: State management and ViewModel (4-6 hours)
- Day 2: UI components and content rendering (6-8 hours)
- Day 3: API integration, testing, and polish (4-6 hours)

## 2. Implementation Strategy

### 2.1 Development Approach

**Methodology:** Incremental development with continuous testing

**Principles:**
1. Start with state management (foundation)
2. Add UI components incrementally
3. Integrate with existing patterns
4. Test at each step
5. Polish at the end

**Order of Operations:**
1. Create data models (TranslationState, TranslationResult)
2. Update ViewModel with state and translate() method
3. Add translation button to ArticleScreen
4. Create status display components
5. Modify content rendering for translations
6. Add dummy API implementation
7. Test end-to-end flow
8. Polish and handle edge cases

### 2.2 Risk Mitigation

**Risks:**
- Breaking existing article viewing functionality
- Performance issues with large articles
- State synchronization problems

**Mitigations:**
- Test existing functionality after each change
- Test with articles of varying sizes
- Follow existing state management patterns strictly
- Continuous testing and verification

## 3. Detailed Implementation Tasks

### Task 3.1: Create Translation State Models

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Steps:**
1. Define `TranslationState` sealed interface
2. Define `TranslationResult` sealed class
3. Add proper KDoc documentation

**Code:**
```kotlin
/**
 * Sealed interface representing the state of article translation.
 */
sealed interface TranslationState {
    data object Empty : TranslationState
    data object Loading : TranslationState
    data class Result(val value: TranslationResult) : TranslationState
}

/**
 * Translation result containing translated paragraphs or error.
 */
sealed class TranslationResult {
    data class Success(val translatedParagraphs: List<String>) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}
```

**Testing:**
- Compile check
- Verify sealed class structure
- Check immutability

**Estimated Time:** 15 minutes

---

### Task 3.2: Add Translation State to ViewModel

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Steps:**
1. Add `translationState` MutableStateFlow
2. Add to `combine()` flow in viewState
3. Add `translationState` property to `ArticleScreenViewState` interface
4. Add `translationState` to `ArticleState` data class

**Code:**
```kotlin
// In ArticleViewModel
private val translationState: MutableStateFlow<TranslationState> =
    MutableStateFlow(TranslationState.Empty)

val viewState: StateFlow<ArticleScreenViewState> =
    combine(
        // ... existing
        translationState,
    ) { params ->
        val translationState = params[10] as TranslationState
        ArticleState(
            // ... existing
            translationState = translationState,
        )
    }.stateIn(...)

// In ArticleScreenViewState interface
val translationState: TranslationState

// In ArticleState data class
override val translationState: TranslationState = TranslationState.Empty,
```

**Testing:**
- Compile check
- Verify state flows correctly
- Check default value is Empty

**Estimated Time:** 20 minutes

---

### Task 3.3: Implement translate() Method

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Steps:**
1. Create `translate()` method
2. Create `extractTranslatableParagraphs()` helper method
3. Add error handling
4. Add logging

**Code:** (See technical specification section 3.3)

**Testing:**
- Unit test for state transitions
- Test paragraph extraction
- Test error handling
- Test with empty article

**Estimated Time:** 30 minutes

---

### Task 3.4: Add Translation Button to ArticleScreen

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Add `onTranslate` lambda parameter to `ArticleScreen` composable
2. Add translation button to top app bar actions
3. Position after "Summarize" button
4. Disable button during loading state

**Code:** (See technical specification section 3.1)

**Testing:**
- UI test for button visibility
- Test button enabled/disabled states
- Test button position
- Verify tooltip appears

**Estimated Time:** 20 minutes

---

### Task 3.5: Pass onTranslate Through Screen Hierarchy

**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Add `onTranslate` to outer `ArticleScreen` composable
2. Pass through to inner `ArticleScreen` composable
3. Connect to `viewModel.translate()`

**Code:**
```kotlin
// In outer ArticleScreen
ArticleScreen(
    // ... existing
    onSummarize = { viewModel.summarize() },
    onTranslate = { viewModel.translate() }, // NEW
)

// In inner ArticleScreen
fun ArticleScreen(
    // ... existing
    onTranslate: () -> Unit, // NEW
    modifier: Modifier = Modifier,
)
```

**Testing:**
- Test button click triggers translate()
- Verify lambda chain works
- Check no compilation errors

**Estimated Time:** 10 minutes

---

### Task 3.6: Create TranslationStatusSection Composable

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Create `TranslationStatusSection` composable
2. Handle Empty, Loading, and Result states
3. Add appropriate UI for each state

**Code:** (See technical specification section 3.4)

**Testing:**
- Test loading state shows progress indicator
- Test empty state shows nothing
- Test error state shows error card
- Test success state shows nothing (inline)

**Estimated Time:** 25 minutes

---

### Task 3.7: Create TranslationErrorSection Composable

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Create `TranslationErrorSection` composable
2. Add error icon
3. Add error title and message
4. No retry button (users tap translate button to retry)

**Code:** (See technical specification section 3.4)

**Testing:**
- Test error card appearance
- Verify styling matches design
- Test with long error messages

**Estimated Time:** 15 minutes

---

### Task 3.8: Add Translation Status to ArticleContent

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Add `TranslationStatusSection` to `ArticleContent`
2. Position after AI summary section

**Code:**
```kotlin
// In ArticleContent, after summary section
if (viewState.translationState !is TranslationState.Empty) {
    offsetCounter++
    item {
        TranslationStatusSection(
            state = viewState.translationState
        )
    }
}
```

**Testing:**
- Test status section appears
- Test loading indicator visible
- Test error card visible
- Test position correct

**Estimated Time:** 15 minutes

---

### Task 3.9: Modify linearArticleContent for Translations

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Add `translatedParagraphs` parameter
2. Add text element index tracking
3. Match translations to text elements
4. Pass translation to `LinearElementContent`

**Code:** (See technical specification section 3.5)

**Testing:**
- Test parameter is optional
- Test index matching works
- Test with missing translations
- Test with extra translations

**Estimated Time:** 30 minutes

---

### Task 3.10: Modify LinearTextContent for Translations

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Change signature to Column layout
2. Add `translation` parameter
3. Display original text
4. Display translation if present
5. Apply proper styling

**Code:** (See technical specification section 3.5)

**Testing:**
- Test original text displays correctly
- Test translation displays below
- Test styling is correct
- Test with null translation
- Test bidi text support

**Estimated Time:** 25 minutes

---

### Task 3.11: Modify LinearElementContent Signature

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Steps:**
1. Add `translation` parameter to `LinearElementContent`
2. Pass through to `LinearTextContent`
3. Ignore for other element types

**Code:**
```kotlin
@Composable
fun LinearElementContent(
    linearElement: LinearElement,
    translation: String? = null, // NEW
    // ... existing parameters
) {
    when (linearElement) {
        is LinearText ->
            LinearTextContent(
                linearText = linearElement,
                translation = translation, // NEW
                // ... existing
            )
        // ... other cases don't use translation
    }
}
```

**Testing:**
- Test signature change doesn't break existing code
- Test translation passed correctly
- Test other elements ignore translation

**Estimated Time:** 10 minutes

---

### Task 3.12: Add translate() to AIApi Interface

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Steps:**
1. Add `translate()` method to interface
2. Add `TranslationResult` sealed class
3. Add KDoc documentation

**Code:** (See technical specification section 3.6)

**Testing:**
- Compile check
- Verify interface contract
- Check return type

**Estimated Time:** 15 minutes

---

### Task 3.13: Implement Dummy Translation

**File:** Implementation class of AIApi

**Steps:**
1. Override `translate()` method
2. Return dummy translations with language prefix
3. Handle empty list case

**Code:** (See technical specification section 3.6)

**Testing:**
- Test method returns correct type
- Test with various paragraph counts
- Test with different languages
- Verify prefix format

**Estimated Time:** 15 minutes

---

### Task 3.14: Connect Translation to ArticleContent

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Steps:**
1. Extract translated paragraphs from state
2. Pass to `linearArticleContent()`
3. Handle null case

**Code:**
```kotlin
// In ArticleContent
val translatedParagraphs = when (val state = viewState.translationState) {
    is TranslationState.Result -> {
        when (val result = state.value) {
            is TranslationResult.Success -> result.translatedParagraphs
            is TranslationResult.Error -> null
        }
    }
    else -> null
}

linearArticleContent(
    articleContent = viewState.articleContent,
    translatedParagraphs = translatedParagraphs, // NEW
    onLinkClick = { link, index -> /* ... */ },
)
```

**Testing:**
- Test translations passed correctly
- Test null handling
- Test state extraction

**Estimated Time:** 15 minutes

---

### Task 3.15: Add String Resources

**File:** `app/src/main/res/values/strings.xml`

**Steps:**
1. Add "translate" string
2. Add "translation_error" string
3. Add content description strings

**Code:**
```xml
<string name="translate">Translate</string>
<string name="translate_article_content_description">Translate article</string>
<string name="translation_error">Translation Failed</string>
```

**Testing:**
- Verify strings exist
- Check for duplicates
- Test in UI

**Estimated Time:** 5 minutes

---

### Task 3.16: Write Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Steps:**
1. Test `translate()` state transitions
2. Test paragraph extraction
3. Test empty article handling
4. Test error handling
5. Test settings integration

**Code:** (See technical specification section 5.1)

**Testing:**
- Run all tests
- Verify coverage
- Check edge cases

**Estimated Time:** 1 hour

---

### Task 3.17: Write UI Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreenTest.kt`

**Steps:**
1. Test button visibility
2. Test button click triggers action
3. Test loading state
4. Test translation display
5. Test error state (users tap translate button to retry)

**Code:** (See technical specification section 5.2)

**Testing:**
- Run on device/emulator
- Verify all scenarios pass
- Check for flakiness

**Estimated Time:** 1 hour

---

### Task 3.18: Manual Testing

**Steps:**
1. Test on phone (various screen sizes)
2. Test on tablet
3. Test in light theme
4. Test in dark theme
5. Test with accessibility tools
6. Test error scenarios
7. Test with very long articles
8. Test with mixed content

**Testing:**
- Create test plan document
- Execute tests
- Document bugs
- Fix issues

**Estimated Time:** 2 hours

---

### Task 3.19: Code Review and Polish

**Steps:**
1. Self-review code
2. Check for TODO comments
3. Verify no debug code
4. Check error handling
5. Verify logging
6. Add missing documentation
7. Format code

**Testing:**
- Read through all changes
- Check against project conventions
- Verify no regressions

**Estimated Time:** 1 hour

---

### Task 3.20: Build and Verification

**Steps:**
1. Clean build: `./gradlew clean`
2. Build debug: `./gradlew assembleDebug`
3. Run tests: `./gradlew test`
4. Run lint: `./gradlew lint`
5. Fix any issues
6. Rebuild

**Testing:**
- Verify build succeeds
- Verify all tests pass
- Verify no lint warnings
- Check APK size

**Estimated Time:** 30 minutes

## 4. Task Breakdown Summary

| Phase | Tasks | Estimated Time | Cumulative Time |
|-------|-------|----------------|-----------------|
| **Day 1: State & ViewModel** | 3.1-3.3 | 1h 5m | 1h 5m |
| **Day 2: UI Components** | 3.4-3.8 | 1h 40m | 2h 45m |
| **Day 2: Content Rendering** | 3.9-3.11 | 1h 5m | 3h 50m |
| **Day 2-3: API Integration** | 3.12-3.14 | 45m | 4h 35m |
| **Day 3: Strings & I18N** | 3.15 | 10m | 4h 45m |
| **Day 3: Testing** | 3.16-3.18 | 4h | 8h 45m |
| **Day 3: Polish & Build** | 3.19-3.20 | 1h 30m | 10h 15m |

**Total Estimated Time:** 10-11 hours

## 5. Dependencies and Prerequisites

### 5.1 External Dependencies

**Required Libraries (already in project):**
- Jetpack Compose 1.5+
- Material3 1.1+
- Kotlin Coroutines 1.7+
- Kodein DI 7.x
- JUnit 5 (testing)
- Compose Testing (UI tests)

**No new dependencies required.**

### 5.2 Code Dependencies

**Required Files (must exist):**
- `ArticleScreen.kt`
- `ArticleViewModel.kt`
- `LinearArticleContent.kt`
- `AIApi.kt`
- `TranslationLanguage.kt`

**Required Infrastructure:**
- AI settings in Repository
- TranslationLanguage enum
- Existing state management patterns

### 5.3 Knowledge Prerequisites

**Developer Skills Required:**
- Kotlin programming
- Jetpack Compose
- Coroutines and Flow
- MVVM architecture
- Material Design 3
- Unit testing (JUnit)
- UI testing (Compose Testing)

## 6. Testing Plan

### 6.1 Unit Testing

**Coverage Target:** 80% for ViewModel methods

**Test Classes:**
- `ArticleViewModelTest` (5-7 tests)

**Scenarios:**
- Normal translation flow
- Empty article
- API error
- Settings integration
- State transitions

### 6.2 UI Testing

**Coverage Target:** Key user flows

**Test Classes:**
- `ArticleScreenTest` (5-6 tests)

**Scenarios:**
- Button visibility and click
- Loading state
- Translation display
- Error state (users tap translate button to retry)

### 6.3 Manual Testing

**Test Devices:**
- Pixel 6 (Android 13)
- Samsung Galaxy S21 (Android 14)
- Tablet (Android 13)

**Test Scenarios:**
1. Open article → Verify button visible
2. Click translate → Verify loading
3. Wait for completion → Verify translations
4. Test error → Verify error card
5. Test retry (tap translate button again) → Verify re-translation
6. Test navigation → Verify state reset
7. Test with long article → Verify performance
8. Test with mixed content → Verify correct display

## 7. Rollout Plan

### 7.1 Deployment Strategy

**Phase 1: Development**
- Develop in feature branch
- Unit and UI tests
- Manual testing

**Phase 2: Code Review**
- Create pull request
- Peer review
- Address feedback

**Phase 3: Integration**
- Merge to main branch
- CI/CD build
- Beta testing (if applicable)

**Phase 4: Release**
- Include in next app release
- Monitor for bugs
- Collect user feedback

### 7.2 Rollback Plan

**If Critical Bugs Found:**
1. Revert commit or hotfix
2. Fix issues in new branch
3. Re-test thoroughly
4. Re-deploy

**Rollback Triggers:**
- Crashes in production
- Performance regression
- Data loss
- Security vulnerability

## 8. Success Criteria

### 8.1 Must Have (P0)

- [ ] Translation button visible and functional
- [ ] Loading indicator appears during translation
- [ ] Translations display below original text
- [ ] Error handling works (users tap translate button to retry)
- [ ] Code compiles without errors
- [ ] No crashes in tested scenarios

### 8.2 Should Have (P1)

- [ ] Button disabled during loading
- [ ] Translations styled correctly
- [ ] Accessibility support working
- [ ] Unit tests passing
- [ ] UI tests passing
- [ ] No regressions in existing features

### 8.3 Nice to Have (P2)

- [ ] Smooth animations
- [ ] Comprehensive error messages
- [ ] Performance optimizations
- [ ] Edge case handling

## 9. Post-Implementation

### 9.1 Monitoring

**Metrics to Track:**
- Translation button click rate
- Translation success rate
- Translation failure reasons
- Average translation time
- User feedback on quality

### 9.2 Maintenance

**Known Issues:**
- No persistence (re-translates on view)
- No timeout handling
- No paragraph limit

**Future Enhancements:**
- Real AI translation
- Translation caching
- Auto-translation
- Translation history

## 10. Sign-Off

**Implementation Complete When:**
- [ ] All tasks completed
- [ ] All tests passing
- [ ] Manual testing successful
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Ready for Phase 8 (Execution & QA)

---

**Implementation Plan Complete**
**Total Tasks:** 20
**Estimated Duration:** 10-11 hours
**Files Modified:** 4
**Tests Added:** 12+
**Risk Level:** Low
**Ready for Execution**
