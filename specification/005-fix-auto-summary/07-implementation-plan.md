# Implementation Plan: Auto-Summary Trigger

## Document Information
- **Date:** 2026-01-01
- **Current Time:** 2026-01-01 21:25:33 (Asia/Shanghai)
- **Status:** Ready for Execution
- **Estimated Effort:** 1-2 hours

---

## 1. Implementation Overview

### 1.1 Objective
Implement automatic article summarization when users open articles, based on the "Enable Auto Summary" setting.

### 1.2 Approach
Modify the `ArticleViewModel.init` block to observe both `articleFlow` and `repository.summaryEnabled`, automatically triggering `summarize()` when conditions are met.

### 1.3 Scope
**In Scope:**
- Modify `ArticleViewModel.kt` init block
- Add observation of `repository.summaryEnabled`
- Implement auto-trigger logic with proper guards

**Out of Scope:**
- UI changes (ArticleScreen.kt)
- Settings UI changes (already done in spec-04)
- AI API changes (already implemented)

---

## 2. Implementation Phases

### Phase 1: Code Changes (15 minutes)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Location:** Lines 179-192 (init block)

**Steps:**

1. **Locate the init block**
```kotlin
init {
    viewModelScope.launch {
        articleFlow.collect { article ->
            val feedId = article?.item?.feedId
            if (feedId != null) {
                val feed = repository.getFeed(feedId)
                if (feed?.summarizeOnOpen == true) {
                    summarize()
                    return@collect
                }
            }
        }
    }
}
```

2. **Replace with new implementation**
```kotlin
init {
    viewModelScope.launch {
        combine(
            articleFlow,
            repository.summaryEnabled
        ) { article, summaryEnabled ->
            article to summaryEnabled
        }.filterNotNull()
            .collect { (article, summaryEnabled) ->
                val feedId = article?.item?.feedId
                if (feedId != null) {
                    val feed = repository.getFeed(feedId)
                    // Check both user setting and feed setting
                    if ((summaryEnabled || feed?.summarizeOnOpen == true) &&
                        aiSummary.value is AISummaryState.Empty &&
                        article?.link != null) {
                        summarize()
                        return@collect
                    }
                }
            }
    }
}
```

3. **Verify imports**
- ✅ `kotlinx.coroutines.flow.combine` (already imported)
- ✅ `kotlinx.coroutines.flow.filterNotNull` (already imported)

### Phase 2: Build Verification (10 minutes)

**Steps:**

1. **Build the project**
```bash
./gradlew assembleDebug
```

**Expected:** ✅ Build succeeds without errors

2. **Check for warnings**
```bash
./gradlew assembleDebug 2>&1 | grep -i warning
```

**Expected:** ✅ No new warnings introduced

### Phase 3: Unit Tests (30 minutes)

**Test File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Steps:**

1. **Create test file** (if not exists)
2. **Add test cases** (see Section 3 below)
3. **Run tests**
```bash
./gradlew test
```

**Expected:** ✅ All tests pass

### Phase 4: Manual Testing (30 minutes)

**Steps:**

1. **Install APK on device**
```bash
./gradlew installDebug
```

2. **Test scenarios** (see Section 4 below)

3. **Verify each scenario**

**Expected:** ✅ All scenarios pass

### Phase 5: Code Review (15 minutes)

**Steps:**

1. **Self-review code changes**
2. **Verify against specification**
3. **Check edge cases**
4. **Document any deviations**

**Expected:** ✅ Code matches specification

### Phase 6: Documentation (10 minutes)

**Steps:**

1. **Update implementation summary** (if needed)
2. **Add comments to code** (if complex)
3. **Update task list**

**Expected:** ✅ Documentation complete

---

## 3. Detailed Task List

### Task 3.1: Modify ArticleViewModel.init Block

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Lines:** 179-192

**Code Changes:**

```kotlin
// BEFORE:
init {
    viewModelScope.launch {
        articleFlow.collect { article ->
            val feedId = article?.item?.feedId
            if (feedId != null) {
                val feed = repository.getFeed(feedId)
                if (feed?.summarizeOnOpen == true) {
                    summarize()
                    return@collect // Only summarize on first load
                }
            }
        }
    }
}

// AFTER:
init {
    viewModelScope.launch {
        combine(
            articleFlow,
            repository.summaryEnabled
        ) { article, summaryEnabled ->
            article to summaryEnabled
        }.filterNotNull()
            .collect { (article, summaryEnabled) ->
                val feedId = article?.item?.feedId
                if (feedId != null) {
                    val feed = repository.getFeed(feedId)
                    // Check both user setting and feed setting
                    if ((summaryEnabled || feed?.summarizeOnOpen == true) &&
                        aiSummary.value is AISummaryState.Empty &&
                        article?.link != null) {
                        summarize()
                        return@collect // Only summarize on first load
                    }
                }
            }
    }
}
```

**Acceptance Criteria:**
- ✅ Code compiles without errors
- ✅ No new warnings introduced
- ✅ Follows existing code style
- ✅ Uses existing imports

**Estimated Time:** 15 minutes

---

### Task 3.2: Verify Build

**Command:** `./gradlew assembleDebug`

**Acceptance Criteria:**
- ✅ Build succeeds (exit code 0)
- ✅ No compilation errors
- ✅ No new warnings

**Estimated Time:** 10 minutes

---

### Task 3.3: Write Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Test Cases:**

#### Test Case 1: Auto-summarize when enabled

```kotlin
@Test
fun `when summaryEnabled is true and article loads, then summarize is called`() = runTest {
    // Given
    val mockRepository = mockk<Repository>()
    val mockAiApi = mockk<AIApi>()
    val testArticle = createTestArticle(id = 1L, link = "https://example.com")

    every { mockRepository.summaryEnabled } returns MutableStateFlow(true)
    every { mockRepository.getArticleFlow(1L) } returns flowOf(testArticle)
    every { mockRepository.getFeed(any()) } returns null
    coEvery { mockAiApi.summarize(any()) } returns SummaryResult.Success("Test summary")

    val di = DI {
        bind<Repository>() with instance(mockRepository)
        bind<AIApi>() with instance(mockAiApi)
        // ... other bindings
    }

    // When
    val viewModel = ArticleViewModel(di, testStateHandle)
    advanceUntilIdle()

    // Then
    coVerify(exactly = 1) { mockAiApi.summarize(any()) }
    assertTrue(viewModel.aiSummary.value is AISummaryState.Result)
}
```

#### Test Case 2: No auto-summarize when disabled

```kotlin
@Test
fun `when summaryEnabled is false and article loads, then summarize is NOT called`() = runTest {
    // Given
    val mockRepository = mockk<Repository>()
    val mockAiApi = mockk<AIApi>()
    val testArticle = createTestArticle(id = 1L, link = "https://example.com")

    every { mockRepository.summaryEnabled } returns MutableStateFlow(false)
    every { mockRepository.getArticleFlow(1L) } returns flowOf(testArticle)
    every { mockRepository.getFeed(any()) } returns Feed(summarizeOnOpen = false)

    val di = DI { /* bindings */ }

    // When
    val viewModel = ArticleViewModel(di, testStateHandle)
    advanceUntilIdle()

    // Then
    coVerify(exactly = 0) { mockAiApi.summarize(any()) }
    assertTrue(viewModel.aiSummary.value is AISummaryState.Empty)
}
```

#### Test Case 3: No duplicate calls

```kotlin
@Test
fun `when article already has summary, summarize is NOT called again`() = runTest {
    // Given
    val mockRepository = mockk<Repository>()
    val mockAiApi = mockk<AIApi>()
    val testArticle = createTestArticle(id = 1L, link = "https://example.com")

    every { mockRepository.summaryEnabled } returns MutableStateFlow(true)
    every { mockRepository.getArticleFlow(1L) } returns flowOf(testArticle)
    every { mockRepository.getFeed(any()) } returns null
    coEvery { mockAiApi.summarize(any()) } returns SummaryResult.Success("Test summary")

    val di = DI { /* bindings */ }

    // When
    val viewModel = ArticleViewModel(di, testStateHandle)
    advanceUntilIdle() // Wait for first summarize

    // Trigger flow again (e.g., setting change)
    // Then
    coVerify(exactly = 1) { mockAiApi.summarize(any()) }
}
```

#### Test Case 4: Feed-level setting works

```kotlin
@Test
fun `when feed summarizeOnOpen is true, summarize is called even if summaryEnabled is false`() = runTest {
    // Given
    val mockRepository = mockk<Repository>()
    val mockAiApi = mockk<AIApi>()
    val testArticle = createTestArticle(id = 1L, link = "https://example.com", feedId = 100L)

    every { mockRepository.summaryEnabled } returns MutableStateFlow(false)
    every { mockRepository.getArticleFlow(1L) } returns flowOf(testArticle)
    every { mockRepository.getFeed(100L) } returns Feed(summarizeOnOpen = true)
    coEvery { mockAiApi.summarize(any()) } returns SummaryResult.Success("Test summary")

    val di = DI { /* bindings */ }

    // When
    val viewModel = ArticleViewModel(di, testStateHandle)
    advanceUntilIdle()

    // Then
    coVerify(exactly = 1) { mockAiApi.summarize(any()) }
}
```

**Acceptance Criteria:**
- ✅ All tests compile
- ✅ All tests pass
- ✅ Code coverage adequate

**Estimated Time:** 30 minutes

---

### Task 3.4: Manual Testing

**Prerequisites:**
- Android device or emulator
- App installed with debug build
- AI provider configured (Anthropic/OpenAI)

**Test Scenarios:**

#### Scenario 1: Enable auto-summary and verify

**Steps:**
1. Open app
2. Navigate to Settings → AI Integration → Summary
3. Enable "Enable Auto Summary" toggle
4. Go back to feed list
5. Tap on any article
6. Wait for article to load

**Expected Result:**
- ✅ Article content loads
- ✅ Loading indicator appears (if summary takes time)
- ✅ Summary appears automatically below article title
- ✅ No user interaction required

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 2: Disable auto-summary and verify

**Steps:**
1. Open app
2. Navigate to Settings → AI Integration → Summary
3. Disable "Enable Auto Summary" toggle
4. Go back to feed list
5. Tap on any article
6. Wait for article to load

**Expected Result:**
- ✅ Article content loads
- ✅ No summary appears automatically
- ✅ Manual summarize still works (tap menu → "summarize")

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 3: Verify caching (no duplicate calls)

**Steps:**
1. Enable "Enable Auto Summary" in settings
2. Open any article
3. Wait for summary to appear
4. Tap back button
5. Open the same article again

**Expected Result:**
- ✅ Summary appears immediately (cached)
- ✅ No loading indicator (already cached)
- ✅ No duplicate API call

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 4: Verify screen rotation

**Steps:**
1. Enable "Enable Auto Summary" in settings
2. Open any article
3. Wait for summary to appear
4. Rotate device (portrait → landscape)

**Expected Result:**
- ✅ Summary persists after rotation
- ✅ No re-summarization occurs
- ✅ UI displays correctly in landscape

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 5: Verify manual summarize still works

**Steps:**
1. Disable "Enable Auto Summary" in settings
2. Open any article
3. Tap three-dots menu (top-right)
4. Tap "Summarize" menu item

**Expected Result:**
- ✅ Loading indicator appears
- ✅ Summary appears after loading
- ✅ Manual summarize works when auto-summary is disabled

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 6: Verify feed-level setting

**Steps:**
1. Disable "Enable Auto Summary" globally
2. Configure a specific feed with "Summarize on Open" enabled
3. Open an article from that feed

**Expected Result:**
- ✅ Summary appears automatically (feed-level setting)
- ✅ Both user-level and feed-level settings work independently

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 7: Verify navigation cancels summary

**Steps:**
1. Enable "Enable Auto Summary" in settings
2. Open a long article (slow to summarize)
3. Immediately tap back button (before summary completes)

**Expected Result:**
- ✅ Summary request is cancelled
- ✅ No error or crash
- ✅ App continues normally

**Actual Result:** _______________

**Status:** PASS / FAIL

---

#### Scenario 8: Verify error handling

**Steps:**
1. Enable "Enable Auto Summary" in settings
2. Configure AI provider with invalid API key
3. Open any article

**Expected Result:**
- ✅ Error message displayed (not crash)
- ✅ User can retry manually
- ✅ App continues normally

**Actual Result:** _______________

**Status:** PASS / FAIL

---

**Acceptance Criteria:**
- ✅ All 8 scenarios tested
- ✅ All scenarios pass
- ✅ No crashes or unexpected behavior

**Estimated Time:** 30 minutes

---

### Task 3.5: Code Review Checklist

**Review Items:**

- [ ] Code follows project style guide
- [ ] No hardcoded values or magic numbers
- [ ] Proper error handling in place
- [ ] No memory leaks (proper coroutine scoping)
- [ ] No thread safety issues
- [ ] Edge cases handled (null checks, empty states)
- [ ] Performance considerations addressed
- [ ] Code is readable and maintainable
- [ ] Comments added where necessary
- [ ] No debug code or TODOs left in

**Acceptance Criteria:**
- ✅ All review items checked
- ✅ No critical issues found
- ✅ Code is production-ready

**Estimated Time:** 15 minutes

---

### Task 3.6: Documentation Updates

**Documents to Update:**

1. **Implementation Summary**
   - File: `08-implementation-summary.md`
   - Add summary of changes made
   - Document any deviations from plan

2. **Task List**
   - File: `08-task-list.md` (update existing)
   - Mark completed tasks
   - Add notes on any issues

**Acceptance Criteria:**
- ✅ Implementation summary complete
- ✅ Task list updated
- ✅ All changes documented

**Estimated Time:** 10 minutes

---

## 4. Risk Management

### 4.1 Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation | Owner |
|------|-----------|--------|------------|-------|
| Breaking existing manual summarize | Low | High | Keep existing `summarize()` method unchanged | Dev |
| Duplicate API calls | Medium | Medium | Check `aiSummary.value is Empty` before calling | Dev |
| Performance degradation | Low | Low | Uses `Dispatchers.IO`, async execution | Dev |
| Memory leak | Very Low | High | Uses `viewModelScope` (auto-cancels) | Dev |
| Build failure | Low | Medium | Verify build after changes | Dev |
| Test failures | Medium | Medium | Fix bugs before committing | Dev |

### 4.2 Rollback Plan

**If critical issues arise:**

1. Revert `ArticleViewModel.kt` init block to original code
2. No database migrations to revert
3. No settings changes to revert
4. Users can still manually summarize

**Rollback Command:**
```bash
git checkout HEAD~1 -- app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt
```

---

## 5. Success Criteria

### 5.1 Functional Criteria

- ✅ When `summaryEnabled` is ON, opening an article automatically triggers summarization
- ✅ Summary appears after article content is loaded
- ✅ When `summaryEnabled` is OFF, no auto-summarization occurs
- ✅ Manual summarization still works via three-dots menu
- ✅ No crashes or errors during auto-summarization
- ✅ Feed-level `summarizeOnOpen` setting still works

### 5.2 Quality Criteria

- ✅ Build passes without errors or warnings
- ✅ Unit tests pass
- ✅ Manual testing scenarios pass
- ✅ Code review approved
- ✅ No regressions in existing functionality

### 5.3 Performance Criteria

- ✅ No impact on UI thread (async execution)
- ✅ No memory leaks
- ✅ No duplicate API calls
- ✅ Proper lifecycle management

---

## 6. Timeline

| Task | Estimated Time | Start Time | End Time | Status |
|------|----------------|------------|----------|--------|
| 3.1: Modify ArticleViewModel.init | 15 min | - | - | Pending |
| 3.2: Verify Build | 10 min | - | - | Pending |
| 3.3: Write Unit Tests | 30 min | - | - | Pending |
| 3.4: Manual Testing | 30 min | - | - | Pending |
| 3.5: Code Review | 15 min | - | - | Pending |
| 3.6: Documentation | 10 min | - | - | Pending |
| **Total** | **120 min (2 hours)** | - | - | - |

---

## 7. Dependencies

### 7.1 External Dependencies

**None** - All dependencies are already in the project.

### 7.2 Internal Dependencies

- ✅ `repository.summaryEnabled: StateFlow<Boolean>` (already exposed in Repository.kt)
- ✅ `articleFlow: StateFlow<Article?>` (already exists in ArticleViewModel.kt)
- ✅ `summarize()` method (already implemented in ArticleViewModel.kt)
- ✅ `aiSummary: MutableStateFlow<AISummaryState>` (already exists in ArticleViewModel.kt)

---

## 8. Sign-Off

**Prepared By:** AI Assistant (super-dev:coordinator)

**Date:** 2026-01-01 21:25:33 (Asia/Shanghai)

**Status:** ✅ **READY FOR EXECUTION**

**Next Phase:** Phase 7 (Specification Review) → Phase 8 (Execution & QA)

---

**END OF IMPLEMENTATION PLAN**
