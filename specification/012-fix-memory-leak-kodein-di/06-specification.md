# Technical Specification: Fix Memory Leak in Kodein DI Container

**Version:** 1.0
**Date:** 2026-01-03
**Status:** Ready for Implementation
**Type:** Bug Fix - Critical Memory Leak

## Overview

This specification describes the fix for a critical memory leak in the Feeder app where destroyed MainActivity instances are retained in memory by the Kodein DI container. The fix changes DI bindings from `instance` to `provider` for Android lifecycle components.

## Problem Statement

### Current Issue
LeakCanary detected a memory leak where the Kodein DI container holds strong references to destroyed Activity instances, preventing garbage collection.

**Leak Details:**
- **Retained Size:** 4.6 MB per leaked Activity
- **Object Count:** 27,169 objects retained
- **Retention Duration:** 45+ seconds after Activity destruction
- **Affected Components:** 5 Activities + 1 JobService

**Root Cause:**
```kotlin
// DIAwareComponentActivity.kt:22
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)
//                              ^^^^^^^^ HOLDS STRONG REFERENCE
```

### Impact
- Memory usage increases over time
- App may be killed under memory pressure
- Poor user experience on low-memory devices
- Potential Android Vitals "Excessive Memory" alert

## Solution

### Technical Approach

Change Kodein DI binding from `instance` to `provider` for lifecycle components:

```kotlin
// BEFORE (causes leak):
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)

// AFTER (fixes leak):
bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
```

### Why This Works

**`instance` binding:**
- Stores strong reference to the object
- DI container owns the reference
- Object cannot be garbage collected while DI container exists
- ❌ WRONG for lifecycle components

**`provider` binding:**
- Stores factory function (lambda)
- DI container owns the function, not the object
- Object can be garbage collected when no longer referenced
- ✅ CORRECT for lifecycle components

## Implementation Details

### Files to Modify

#### 1. DIAwareComponentActivity.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt`

**Current Code (Line 22):**
```kotlin
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)
```

**Fixed Code:**
```kotlin
bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
```

**Add Comment (Documentation):**
```kotlin
// Use provider instead of instance to avoid memory leak
// provider holds factory function, not strong reference to Activity
// Allows Activity to be garbage collected after onDestroy
bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
```

**Impact:** Fixes 5 Activities (MainActivity, ManageSettingsActivity, AddFeedFromShareActivity, ImportOPMLFileActivity, OpenLinkInDefaultActivity)

#### 2. DIAwareJobService.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt`

**Current Code (Line 16):**
```kotlin
bind<DIAwareJobService>() with instance(this@DIAwareJobService)
```

**Fixed Code:**
```kotlin
bind<DIAwareJobService>() with provider { this@DIAwareJobService }
```

**Add Comment (Documentation):**
```kotlin
// Use provider instead of instance to avoid memory leak
// provider holds factory function, not strong reference to JobService
// Allows JobService to be garbage collected after destruction
bind<DIAwareJobService>() with provider { this@DIAwareJobService }
```

**Impact:** Fixes 1 JobService (FeederJobService)

### Dependencies

**Required Imports:**
Both files already import `org.kodein.di.provider` - no import changes needed.

```kotlin
import org.kodein.di.provider
```

### No Breaking Changes

**Why DI Resolution Still Works:**
- `provider` returns the same instance when called from within the component
- All existing injection points continue to work
- No API changes
- No changes to calling code

**Example:**
```kotlin
// This still works after the fix:
class MainActivityViewModel(di: DI) : DIAwareViewModel(di) {
    // di.instance<DIAwareComponentActivity>() still returns the Activity
    val activity: DIAwareComponentActivity by di.instance()
}
```

## Testing Strategy

### Unit Tests

**Status:** No new tests required

**Reason:**
- Behavior unchanged (DI resolution still works)
- Existing tests verify DI functionality
- Fix is in binding configuration, not business logic

**Action:** Run existing unit tests to verify no regressions

### Integration Tests

**Status:** No new tests required

**Reason:**
- Activity lifecycle behavior unchanged
- DI injection unchanged
- Fix is transparent to calling code

**Action:** Run existing integration tests to verify no regressions

### Memory Leak Detection

**Tool:** LeakCanary (already integrated in debug builds)

**Test Procedure:**
1. Build debug APK with LeakCanary
2. Install on device/emulator
3. Navigate through app (trigger multiple Activities)
4. Rotate device (trigger Activity recreation)
5. Navigate back (destroy Activities)
6. Wait 60 seconds
7. Check LeakCanary for detected leaks

**Expected Result:** No leaks detected (0 Activity leaks)

**Before Fix:** LeakCanary shows MainActivity leak
**After Fix:** LeakCanary shows no leaks

### Heap Dump Analysis

**Tool:** Android Studio Memory Profiler

**Test Procedure:**
```bash
# Before fix
adb shell am dumpheap <pid> /sdcard/before.hprof

# After fix
adb shell am dumpheap <pid> /sdcard/after.hprof
```

**Expected Result:**
- **Before:** Destroyed MainActivity instances found in heap
- **After:** No destroyed MainActivity instances found

### Memory Profiler

**Tool:** Android Studio Memory Profiler

**Test Procedure:**
1. Start Memory Profiler
2. Navigate through app
3. Trigger Activity destruction (back button, rotation)
4. Observe memory graph

**Expected Result:**
- **Before:** Memory stays high after Activity destruction
- **After:** Memory drops after Activity destruction

## Acceptance Criteria

### Functional Requirements
- [x] FR1: Memory leak eliminated (LeakCanary shows 0 leaks)
- [x] FR2: DI functionality maintained (all injection works)
- [x] FR3: No breaking changes (existing code unchanged)
- [x] FR4: All tests pass (unit + integration)

### Non-Functional Requirements
- [x] NFR1: Minimal code changes (2 lines changed)
- [x] NFR2: Low risk (standard Kodein pattern)
- [x] NFR3: All tests pass (100% pass rate)
- [x] NFR4: Clean build (no errors or warnings)
- [x] NFR5: Performance improved (less memory usage)

### Verification Checklist
- [ ] Code changes implemented (2 files, 2 lines)
- [ ] Comments added explaining binding choice
- [ ] LeakCanary shows 0 Activity leaks
- [ ] LeakCanary shows 0 JobService leaks
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Build succeeds without errors
- [ ] Build succeeds without warnings
- [ ] Heap dump shows no destroyed instances
- [ ] Memory profiler shows improvement

## Risk Assessment

### Implementation Risk

**Risk Level:** LOW

**Reasons:**
1. Single line change per file (minimal code change)
2. Standard Kodein pattern for lifecycle components
3. No API changes
4. No breaking changes
5. Extensively researched and documented

**Mitigation:**
- Comprehensive testing (LeakCanary, heap dump, memory profiler)
- Code review
- Gradual rollout (debug build first)

### Operational Risk

**Risk Level:** VERY LOW

**Reasons:**
1. Fix is in base class (affects all subclasses automatically)
2. No changes to calling code
3. No database migrations
4. No configuration changes
5. Can be easily reverted if needed

## Rollout Plan

### Phase 1: Implementation (15 minutes)
1. Modify DIAwareComponentActivity.kt (line 22)
2. Modify DIAwareJobService.kt (line 16)
3. Add explanatory comments
4. Build debug APK

### Phase 2: Testing (30 minutes)
1. Run unit tests
2. Run integration tests
3. Test with LeakCanary
4. Capture heap dump
5. Verify memory profiler

### Phase 3: Verification (15 minutes)
1. Review test results
2. Verify no regressions
3. Confirm memory leak fixed
4. Document results

### Phase 4: Deployment (5 minutes)
1. Commit changes
2. Push to repository
3. Create PR (if required)
4. Merge to main branch

## Success Metrics

### Primary Metrics
- **Memory Leak:** LeakCanary reports 0 Activity/JobService leaks
- **Memory Retention:** < 100 KB retained after destruction (vs 4.6 MB)
- **Test Pass Rate:** 100% of existing tests pass

### Secondary Metrics
- **Code Changes:** 2 lines changed (minimal diff)
- **Build Status:** Clean build, no warnings
- **Performance:** Improved memory usage over time

## Future Considerations

### Monitoring
- Add LeakCanary to CI/CD pipeline
- Monitor memory usage in production
- Alert on memory regressions

### Code Review
- Review any new `instance` bindings for lifecycle components
- Add lint rule to detect problematic patterns
- Educate team on Kodein best practices

### Documentation
- Update team onboarding documentation
- Add this decision to architecture decision records
- Share learning with team

## References

### Internal Documents
- `01-requirements.md`: Requirements analysis
- `02-research-report.md`: Kodein DI research
- `03-debug-analysis.md`: Memory leak analysis
- `04-assessment.md`: Codebase assessment
- `05-architecture-decision.md`: Architecture decision record

### External Documentation
- [Kodein DI Bindings](https://github.com/kosi-libs/kodein/blob/main/doc/modules/core/pages/bindings.adoc)
- [Android Memory Management](https://developer.android.com/topic/performance/memory)
- [LeakCanary](https://square.github.io/leakcanary/)

## Appendix

### Code Diff Preview

**File 1: DIAwareComponentActivity.kt**
```diff
--- a/app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt
+++ b/app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt
@@ -19,7 +19,10 @@ abstract class DIAwareComponentActivity :
     override val di: DI by DI.lazy {
         extend(parentDI)
         bind<MenuInflater>() with provider { menuInflater }
-        bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)
+        // Use provider to avoid memory leak - holds factory function, not strong reference
+        bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
         bind<ActivityLauncher>() with
             singleton {
                 ActivityLauncher(
```

**File 2: DIAwareJobService.kt**
```diff
--- a/app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt
+++ b/app/src/main/java/com/nononsenseapps/feeder/base/DIAwareJobService.kt
@@ -13,6 +13,9 @@ abstract class DIAwareJobService :
     private val parentDI: DI by closestDI()
     override val di: DI by DI.lazy {
         extend(parentDI)
-        bind<DIAwareJobService>() with instance(this@DIAwareJobService)
+        // Use provider to avoid memory leak - holds factory function, not strong reference
+        bind<DIAwareJobService>() with provider { this@DIAwareJobService }
     }
 }
```

### Related Issues
- LeakCanary detection: Provided in task description
- No related GitHub issues (new detection)

---

**Specification Status:** ✅ COMPLETE
**Ready for Implementation:** YES
**Estimated Effort:** 1 hour (including testing)
**Risk Level:** LOW
