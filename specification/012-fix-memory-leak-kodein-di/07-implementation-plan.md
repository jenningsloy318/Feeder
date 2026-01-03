# Implementation Plan: Fix Memory Leak in Kodein DI Container

**Date:** 2026-01-03
**Status:** Ready for Execution
**Estimated Time:** 90 minutes

## Overview

This implementation plan describes the step-by-step process to fix the memory leak in the Feeder app by changing Kodein DI bindings from `instance` to `provider` for Android lifecycle components.

## Pre-Implementation Checklist

- [x] Requirements documented (`01-requirements.md`)
- [x] Research completed (`02-research-report.md`)
- [x] Debug analysis done (`03-debug-analysis.md`)
- [x] Code assessment finished (`04-assessment.md`)
- [x] Architecture decision made (`05-architecture-decision.md`)
- [x] Technical specification written (`06-specification.md`)
- [ ] Development environment ready
- [ ] Backup/stash current work
- [ ] Tests passing (baseline)

## Implementation Phases

### Phase 1: Preparation (10 minutes)

#### Task 1.1: Create Checkpoint
```bash
git stash push -m "checkpoint: before memory leak fix"
```
**Purpose:** Protect current work before making changes

#### Task 1.2: Verify Baseline
```bash
./gradlew clean build
```
**Purpose:** Ensure project builds successfully before changes
**Expected:** Clean build with no errors

#### Task 1.3: Run Tests (Baseline)
```bash
./gradlew test
```
**Purpose:** Establish baseline test pass rate
**Expected:** All tests pass

### Phase 2: Code Changes (15 minutes)

#### Task 2.1: Fix DIAwareComponentActivity.kt
**File:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt`
**Line:** 22

**Change:**
```kotlin
// BEFORE:
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)

// AFTER:
// Use provider to avoid memory leak - holds factory function, not strong reference
bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
```

**Verification:**
- [ ] Line 22 changed from `instance` to `provider`
- [ ] Comment added explaining the change
- [ ] No other lines modified

#### Task 2.2: Fix DIAwareJobService.kt
**File:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt`
**Line:** 16

**Change:**
```kotlin
// BEFORE:
bind<DIAwareJobService>() with instance(this@DIAwareJobService)

// AFTER:
// Use provider to avoid memory leak - holds factory function, not strong reference
bind<DIAwareJobService>() with provider { this@DIAwareJobService }
```

**Verification:**
- [ ] Line 16 changed from `instance` to `provider`
- [ ] Comment added explaining the change
- [ ] No other lines modified

### Phase 3: Build Verification (15 minutes)

#### Task 3.1: Clean Build
```bash
./gradlew clean build
```
**Expected:** Clean build with no errors
**Acceptance:** Build succeeds, exit code 0

#### Task 3.2: Check for Warnings
```bash
./gradlew build 2>&1 | grep -i warning
```
**Expected:** No relevant warnings
**Acceptance:** Zero warnings related to DI changes

#### Task 3.3: Build Debug APK
```bash
./gradlew assembleDebug
```
**Purpose:** Generate APK for LeakCanary testing
**Expected:** APK created successfully

### Phase 4: Test Execution (20 minutes)

#### Task 4.1: Run Unit Tests
```bash
./gradlew test
```
**Expected:** All unit tests pass
**Acceptance:** 100% pass rate, no failures

#### Task 4.2: Run Integration Tests
```bash
./gradlew connectedAndroidTest
```
**Expected:** All integration tests pass
**Acceptance:** 100% pass rate, no failures

#### Task 4.3: Verify DI Resolution
**Test Case:** Navigate through app, verify all features work
**Steps:**
1. Launch app
2. Open settings
3. Add feed
4. View feed items
5. Navigate back

**Expected:** All features work correctly
**Acceptance:** No crashes, no DI errors

### Phase 5: Memory Leak Verification (20 minutes)

#### Task 5.1: Install Debug APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
**Purpose:** Install APK with LeakCanary on device/emulator

#### Task 5.2: Trigger Memory Leak Scenarios
**Test Steps:**
1. Launch app
2. Navigate to MainActivity
3. Rotate device (triggers Activity recreation)
4. Navigate to settings
5. Rotate device again
6. Navigate back (destroy Activities)
7. Wait 60 seconds

**Expected:** No LeakCanary notification

#### Task 5.3: Check LeakCanary Results
**Method:** Check device notification tray or LeakCanary UI

**Expected Results:**
- ✅ "No leaks detected"
- ✅ No Activity leaks
- ✅ No JobService leaks

**Acceptance:** Zero leaks detected

#### Task 5.4: Capture Heap Dump (Optional)
```bash
adb shell am dumpheap <pid> /sdcard/after-fix.hprof
adb pull /sdcard/after-fix.hprof
```
**Purpose:** Verify no destroyed instances in heap

#### Task 5.5: Memory Profiler (Optional)
**Tool:** Android Studio Memory Profiler
**Steps:**
1. Start Memory Profiler
2. Navigate through app
3. Trigger Activity destruction
4. Observe memory graph

**Expected:** Memory drops after Activity destruction

### Phase 6: Documentation (10 minutes)

#### Task 6.1: Update Task List
**File:** `01-task-list.md`
**Action:** Mark all tasks complete

#### Task 6.2: Update Implementation Summary
**File:** `06-implementation-summary.md`
**Action:** Document results, challenges, decisions

#### Task 6.3: Update Specification (if needed)
**File:** `06-specification.md`
**Action:** Add `[UPDATED]` markers if implementation deviated from spec

### Phase 7: Code Review (Self-Review)

#### Review Checklist
- [ ] Both files changed correctly
- [ ] Comments added and clear
- [ ] No unintended changes
- [ ] Code follows project style
- [ ] Build succeeds
- [ ] Tests pass
- [ ] LeakCanary shows no leaks
- [ ] Documentation updated

### Phase 8: Commit & Push (10 minutes)

#### Task 8.1: Stage Changes
```bash
git add app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt
git add app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt
```

#### Task 8.2: Generate Commit Message
**Action:** Use generating-commit-messages skill

#### Task 8.3: Commit Changes
```bash
git commit -m "[commit message from skill]"
```

#### Task 8.4: Push Changes
```bash
git push origin ai-features
```

#### Task 8.5: Verify Clean State
```bash
git status
```
**Expected:** "working tree clean"

## Success Criteria

### Must Have (Blocking)
- ✅ Both files changed (DIAwareComponentActivity.kt, DIAwareJobService.kt)
- ✅ Build succeeds without errors
- ✅ All tests pass (100% pass rate)
- ✅ LeakCanary shows 0 leaks
- ✅ Changes committed and pushed

### Should Have (Important)
- ✅ Build succeeds without warnings
- ✅ Comments added explaining changes
- ✅ Heap dump shows no destroyed instances
- ✅ Memory profiler shows improvement

### Could Have (Nice to Have)
- ✅ Memory comparison before/after
- ✅ Performance metrics documented
- ✅ CI/CD updated with LeakCanary

## Risk Mitigation

### If Build Fails
1. Check syntax errors
2. Verify imports (should already exist)
3. Revert changes and investigate
4. Reapply fix after understanding issue

### If Tests Fail
1. Identify failing test
2. Determine if related to DI change
3. If unrelated, fix test
4. If related, investigate DI resolution
5. Revert and re-analyze if needed

### If LeakCanary Still Shows Leaks
1. Verify correct line changed
2. Verify APK installed (debug, not release)
3. Clear app data and retry
4. Check for other leak sources
5. Re-run test scenarios

### If Unexpected Behavior
1. Revert changes immediately
2. Investigate root cause
3. Research alternative solutions
4. Re-apply fix with adjustments

## Rollback Plan

### Immediate Rollback
```bash
git revert HEAD
git push origin ai-features
```

### Rollback to Checkpoint
```bash
git stash pop
```

### Rollback Strategy
- If critical issue: Immediate rollback
- If minor issue: Fix and redeploy
- If uncertainty: Rollback and investigate

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Preparation | 10 min | None |
| Code Changes | 15 min | Preparation |
| Build Verification | 15 min | Code Changes |
| Test Execution | 20 min | Build Verification |
| Memory Leak Verification | 20 min | Build Verification |
| Documentation | 10 min | Test Execution |
| Code Review | 5 min | Documentation |
| Commit & Push | 10 min | Code Review |

**Total Time:** 105 minutes (1 hour 45 minutes)

## Post-Implementation

### Monitoring
- Monitor Crashlytics for memory-related crashes
- Monitor Android Vitals for memory issues
- Track user reports of performance problems

### Follow-Up Actions
- Add LeakCanary to CI/CD pipeline
- Document lessons learned
- Share with team
- Update coding standards

---

**Implementation Plan Status:** ✅ COMPLETE
**Ready for Execution:** YES
**Next Step:** Execute Phase 8 (Execution & QA)
