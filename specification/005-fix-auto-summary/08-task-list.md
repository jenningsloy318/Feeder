# Task List: Fix Auto-Summary Not Triggering

## Document Information
- **Date:** 2026-01-01
- **Current Time:** 2026-01-01 21:25:33 (Asia/Shanghai)
- **Status:** Ready for Execution

---

## Phase 1: Specification Setup ✅ COMPLETE

- [x] Create spec directory
- [x] Create workflow tracking JSON
- [x] Create initial task list

**Completed At:** 2026-01-01 21:11:10+08:00

---

## Phase 2: Requirements Clarification ✅ COMPLETE

- [x] Gather reproduction steps from user
- [x] Clarify expected behavior
- [x] Understand current behavior
- [x] Document requirements

**Completed At:** 2026-01-01 21:11:15+08:00

---

## Phase 3: Research ✅ COMPLETE

- [x] Research auto-summarization patterns in Android apps
- [x] Find best practices for triggering background tasks
- [x] Research Kotlin Flow/StateFlow patterns for auto-triggering

**Completed At:** 2026-01-01 21:11:20+08:00

---

## Phase 4: Debug Analysis ✅ COMPLETE

- [x] Locate article reading screen code
- [x] Find where manual summarization is triggered
- [x] Identify where auto-summarization should be triggered
- [x] Analyze SettingsStore.summaryEnabled flow

**Completed At:** 2026-01-01 21:11:25+08:00

---

## Phase 5: Code Assessment ✅ COMPLETE

- [x] Assess article reading component architecture
- [x] Identify viewmodels and repositories involved
- [x] Check existing summarization integration points
- [x] Review AI API usage patterns

**Completed At:** 2026-01-01 21:25:33+08:00

---

## Phase 6: Specification Writing ✅ COMPLETE

- [x] Write technical specification (06-specification.md)
- [x] Create implementation plan (07-implementation-plan.md)
- [x] Update task list with detailed tasks (08-task-list.md)

**Completed At:** 2026-01-01 21:25:33+08:00

---

## Phase 7: Specification Review 🔄 IN PROGRESS

- [ ] Review specification for completeness
- [ ] Verify implementation plan is feasible
- [ ] Approve specification for execution

**Started At:** 2026-01-01 21:25:33+08:00

---

## Phase 8: Execution & QA ⏳ PENDING

### Task 8.1: Modify ArticleViewModel.init Block (15 min)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Lines:** 179-192

**Description:**
Replace the current init block that only checks `feed?.summarizeOnOpen` with a new implementation that:
- Combines `articleFlow` with `repository.summaryEnabled`
- Checks both user-level (`summaryEnabled`) and feed-level (`summarizeOnOpen`) settings
- Prevents duplicate calls by checking `aiSummary.value is AISummaryState.Empty`
- Validates article has a link before summarizing

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
                    return@collect
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

**Acceptance Criteria:**
- [ ] Code compiles without errors
- [ ] No new warnings introduced
- [ ] Follows existing code style
- [ ] Uses existing imports

**Status:** ⏳ Pending

---

### Task 8.2: Verify Build (10 min)

**Command:** `./gradlew assembleDebug`

**Description:**
Build the project to ensure code changes compile successfully and no new warnings are introduced.

**Acceptance Criteria:**
- [ ] Build succeeds (exit code 0)
- [ ] No compilation errors
- [ ] No new warnings

**Status:** ⏳ Pending

---

### Task 8.3: Write Unit Tests (30 min)

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt`

**Description:**
Create unit tests to verify auto-summarization behavior.

**Test Cases:**
- [ ] Test: Auto-summarize when `summaryEnabled` is true
- [ ] Test: No auto-summarize when `summaryEnabled` is false
- [ ] Test: No duplicate calls when summary already exists
- [ ] Test: Feed-level `summarizeOnOpen` setting works independently

**Acceptance Criteria:**
- [ ] All tests compile
- [ ] All tests pass
- [ ] Code coverage adequate

**Status:** ⏳ Pending

---

### Task 8.4: Manual Testing (30 min)

**Description:**
Install app on device and perform manual testing scenarios.

**Test Scenarios:**
- [ ] Scenario 1: Enable auto-summary → Open article → Summary appears
- [ ] Scenario 2: Disable auto-summary → Open article → No summary appears
- [ ] Scenario 3: Verify caching (reopen same article → No duplicate call)
- [ ] Scenario 4: Verify screen rotation (summary persists)
- [ ] Scenario 5: Verify manual summarize still works when disabled
- [ ] Scenario 6: Verify feed-level `summarizeOnOpen` setting
- [ ] Scenario 7: Verify navigation cancels summary
- [ ] Scenario 8: Verify error handling (invalid API key)

**Acceptance Criteria:**
- [ ] All 8 scenarios tested
- [ ] All scenarios pass
- [ ] No crashes or unexpected behavior

**Status:** ⏳ Pending

---

## Phase 9: Code Review ⏳ PENDING

- [ ] Perform code review
- [ ] Address any findings
- [ ] Verify against specification

**Status:** ⏳ Pending

---

## Phase 10: Documentation Update ⏳ PENDING

- [ ] Update implementation summary
- [ ] Update task list (mark completed tasks)
- [ ] Document any deviations from plan

**Status:** ⏳ Pending

---

## Phase 11: Cleanup ⏳ PENDING

- [ ] Remove debug code (if any)
- [ ] Clean up temporary files
- [ ] Verify no TODO/FIXME comments left

**Status:** ⏳ Pending

---

## Phase 12: Commit & Push ⏳ PENDING

- [ ] Stage all changes (only files modified in this session)
- [ ] Commit with descriptive message
- [ ] Push to remote branch
- [ ] Verify git status clean

**Commit Message:**
```
fix: implement auto-summary trigger based on user setting

- Modify ArticleViewModel.init to observe repository.summaryEnabled
- Combine articleFlow with summaryEnabled for reactive auto-trigger
- Check both user-level (summaryEnabled) and feed-level (summarizeOnOpen) settings
- Prevent duplicate API calls by checking aiSummary state
- Add proper guards (article link, empty summary check)

Closes #spec-05
```

**Files to Commit:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt` (if created)

**GitHub Rule Reminder:**
- ✅ Only commit files edited in this session
- ❌ Do NOT use `git add -A`
- ❌ Do NOT commit .github/workflows/ files

**Status:** ⏳ Pending

---

## Phase 13: Final Verification ⏳ PENDING

- [ ] Verify all documents complete
  - [ ] 01-requirements.md ✅
  - [ ] 02-research-report.md ✅
  - [ ] 03-debug-analysis.md ✅
  - [ ] 04-assessment.md ✅
  - [ ] 06-specification.md ✅
  - [ ] 07-implementation-plan.md ✅
  - [ ] 08-task-list.md ✅
  - [ ] 08-implementation-summary.md (to be created)
- [ ] Verify code changes complete
  - [ ] ArticleViewModel.kt init block modified
  - [ ] No TODO/FIXME comments left
  - [ ] No debug code remaining
- [ ] Verify tests passing
  - [ ] Unit tests pass
  - [ ] Manual testing scenarios pass
- [ ] Verify git status clean
  - [ ] All changes committed
  - [ ] All changes pushed
  - [ ] Working tree clean

**Status:** ⏳ Pending

---

## Summary

**Total Phases:** 13
**Completed Phases:** 6 (46%)
**In Progress:** 1 (Phase 7: Specification Review)
**Pending Phases:** 6 (54%)

**Next Steps:**
1. Complete Phase 7 (Specification Review)
2. Execute Phase 8 (Execution & QA) - PARALLEL with dev-executor and qa-agent
3. Iterate Phase 8 ↔ Phase 9 until code review passes
4. Complete remaining phases

**Estimated Time to Complete:** 2 hours (Phase 8 execution)

---

**Last Updated:** 2026-01-01 21:25:33 (Asia/Shanghai)
