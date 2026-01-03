# Task List: Fix Memory Leak in Kodein DI Container

**Project:** Feeder App
**Feature:** Fix Memory Leak in Kodein DI Container
**Date:** 2026-01-03
**Status:** In Progress

## Summary

Fix critical memory leak where Kodein DI container holds strong references to destroyed Activity and JobService instances. Solution: Change DI bindings from `instance` to `provider` (2 lines changed).

## Tasks

### Phase 1: Preparation ✅ COMPLETE

- [x] **T1.1** - Create spec directory structure
- [x] **T1.2** - Document requirements (`01-requirements.md`)
- [x] **T1.3** - Research Kodein DI best practices (`02-research-report.md`)
- [x] **T1.4** - Analyze debug evidence (`03-debug-analysis.md`)
- [x] **T1.5** - Assess existing DI patterns (`04-assessment.md`)
- [x] **T1.6** - Create architecture decision (`05-architecture-decision.md`)
- [x] **T1.7** - Write technical specification (`06-specification.md`)
- [x] **T1.8** - Create implementation plan (`07-implementation-plan.md`)

### Phase 2: Execution & QA (PARALLEL)

#### Development Tasks

- [ ] **T2.1** - Create git checkpoint
  ```bash
  git stash push -m "checkpoint: before memory leak fix"
  ```

- [ ] **T2.2** - Verify baseline build
  ```bash
  ./gradlew clean build
  ```
  **Acceptance:** Clean build, no errors

- [ ] **T2.3** - Fix DIAwareComponentActivity.kt
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt`
  - **Line:** 22
  - **Change:** `instance` → `provider`
  - **Add Comment:** Explain why provider is used

- [ ] **T2.4** - Fix DIAwareJobService.kt
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt`
  - **Line:** 16
  - **Change:** `instance` → `provider`
  - **Add Comment:** Explain why provider is used

- [ ] **T2.5** - Build project
  ```bash
  ./gradlew clean build
  ```
  **Acceptance:** Build succeeds, no errors or warnings

- [ ] **T2.6** - Build debug APK
  ```bash
  ./gradlew assembleDebug
  ```
  **Acceptance:** APK created successfully

#### QA Tasks (Parallel with Development)

- [ ] **T2.7** - Run unit tests
  ```bash
  ./gradlew test
  ```
  **Acceptance:** 100% pass rate

- [ ] **T2.8** - Run integration tests
  ```bash
  ./gradlew connectedAndroidTest
  ```
  **Acceptance:** 100% pass rate

- [ ] **T2.9** - Manual functional testing
  - Launch app
  - Navigate through all screens
  - Verify all features work
  - **Acceptance:** No crashes, no DI errors

- [ ] **T2.10** - LeakCanary verification
  - Install debug APK
  - Trigger memory leak scenarios:
    - Rotate device multiple times
    - Navigate through multiple Activities
    - Navigate back (destroy Activities)
    - Wait 60 seconds
  - **Acceptance:** LeakCanary shows 0 leaks

- [ ] **T2.11** - Heap dump analysis (optional)
  ```bash
  adb shell am dumpheap <pid> /sdcard/after-fix.hprof
  ```
  **Acceptance:** No destroyed Activity instances found

- [ ] **T2.12** - Memory profiler verification (optional)
  - Start Memory Profiler
  - Navigate through app
  - Trigger Activity destruction
  - **Acceptance:** Memory drops after destruction

### Phase 3: Code Review

- [ ] **T3.1** - Self-review code changes
  - [ ] Both files changed correctly
  - [ ] Comments added and clear
  - [ ] No unintended changes
  - [ ] Code follows project style

- [ ] **T3.2** - Review build output
  - [ ] No errors
  - [ ] No warnings
  - [ ] Clean build

- [ ] **T3.3** - Review test results
  - [ ] All unit tests pass
  - [ ] All integration tests pass
  - [ ] LeakCanary shows no leaks

- [ ] **T3.4** - Review documentation
  - [ ] Task list updated
  - [ ] Implementation summary created
  - [ ] Specification accurate

### Phase 4: Documentation

- [ ] **T4.1** - Update task list
  - Mark completed tasks
  - Update task status
  - **File:** `08-task-list.md`

- [ ] **T4.2** - Create implementation summary
  - Document files changed
  - Document challenges encountered
  - Document solutions applied
  - **File:** `09-implementation-summary.md`

- [ ] **T4.3** - Update specification (if needed)
  - Add `[UPDATED]` markers for deviations
  - **File:** `06-specification.md`

### Phase 5: Cleanup

- [ ] **T5.1** - Review temp files
  - Remove any temporary test files
  - Remove debug output
  - Clean up debug APKs (optional)

- [ ] **T5.2** - Verify no debug code
  - Check for `println` statements
  - Check for `Log.d` statements
  - Remove any debug code

- [ ] **T5.3** - Code formatting
  - Run code formatter (if project uses one)
  - Verify indentation consistent
  - Verify line endings

### Phase 6: Commit & Push

- [ ] **T6.1** - Stage changes
  ```bash
  git add app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt
  git add app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt
  ```

- [ ] **T6.2** - Generate commit message
  - Use generating-commit-messages skill
  - Follow project conventions

- [ ] **T6.3** - Commit changes
  ```bash
  git commit -m "[generated commit message]"
  ```

- [ ] **T6.4** - Push to remote
  ```bash
  git push origin ai-features
  ```

- [ ] **T6.5** - Verify clean state
  ```bash
  git status
  ```
  **Acceptance:** "working tree clean"

### Phase 7: Final Verification

- [ ] **T7.1** - Verify all tasks complete
  - Check task list: all marked complete
  - Check implementation summary: filled out
  - Check specification: accurate

- [ ] **T7.2** - Verify documentation complete
  - [ ] Requirements documented
  - [ ] Research documented
  - [ ] Debug analysis documented
  - [ ] Assessment documented
  - [ ] Architecture decision documented
  - [ ] Specification documented
  - [ ] Implementation plan documented
  - [ ] Task list maintained
  - [ ] Implementation summary created

- [ ] **T7.3** - Verify git status clean
  ```bash
  git status
  ```
  **Acceptance:** "nothing to commit, working tree clean"

- [ ] **T7.4** - Verify changes pushed
  ```bash
  git log -1 --oneline
  ```
  **Acceptance:** Commit shows in log

- [ ] **T7.5** - Generate final report
  - Create summary of work completed
  - List files changed
  - Document test results
  - Document any deviations from plan

## Progress Tracking

**Total Tasks:** 35
**Completed:** 8 (Phase 1 complete)
**In Progress:** 0
**Pending:** 27

**Progress:** 23% complete (8/35 tasks)

## Files to Change

1. `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt`
   - **Lines:** 1 line changed (line 22)
   - **Type:** `instance` → `provider`
   - **Impact:** Fixes 5 Activities

2. `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt`
   - **Lines:** 1 line changed (line 16)
   - **Type:** `instance` → `provider`
   - **Impact:** Fixes 1 JobService

**Total Lines Changed:** 2
**Total Files Changed:** 2

## Acceptance Criteria

### Must Have (Blocking)
- ✅ Requirements documented and approved
- ✅ Research completed and documented
- ✅ Root cause identified and documented
- ✅ Architecture decision made
- ✅ Technical specification written
- ✅ Implementation plan created
- [ ] Both files changed correctly
- [ ] Build succeeds without errors
- [ ] All tests pass (100%)
- [ ] LeakCanary shows 0 leaks
- [ ] Changes committed and pushed
- [ ] Documentation complete

### Should Have (Important)
- [ ] Build succeeds without warnings
- [ ] Comments added explaining changes
- [ ] Heap dump shows no destroyed instances
- [ ] Memory profiler shows improvement

### Could Have (Nice to Have)
- [ ] Memory comparison metrics documented
- [ ] CI/CD updated with LeakCanary
- [ ] Team notification sent

## Risks and Issues

### Risks
- **Low Risk:** Standard Kodein pattern for lifecycle components
- **Low Risk:** Minimal code changes (2 lines)
- **Low Risk:** No breaking changes
- **Low Risk:** Extensively researched and documented

### Issues
- **None identified**

## Notes

### Key Points
1. Fix is minimal (2 lines changed)
2. Risk is low (standard Kodein pattern)
3. Testing is critical (LeakCanary verification)
4. Documentation is important (explain why provider is used)

### Dependencies
- LeakCanary must be integrated in debug builds ✅ (already integrated)
- Android development environment configured ✅ (already configured)
- Device/emulator available for testing ⚠️ (verify before testing)

### Blocking Issues
- **None**

## Next Steps

1. ✅ Phase 1 complete (planning and documentation)
2. → Execute Phase 2 (Implementation & QA in PARALLEL)
3. → Execute Phase 3 (Code Review)
4. → Execute Phase 4 (Documentation Update)
5. → Execute Phase 5 (Cleanup)
6. → Execute Phase 6 (Commit & Push)
7. → Execute Phase 7 (Final Verification)

---

**Task List Status:** ✅ UP TO DATE
**Last Updated:** 2026-01-03
**Next Phase:** Execution & QA (Phase 2)
