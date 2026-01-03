# Requirements: Fix Memory Leak in Kodein DI Container

**Date:** 2026-01-03
**Type:** Bug Fix - Critical Memory Leak
**Priority:** High

## Problem Statement

LeakCanary detected a critical memory leak where destroyed MainActivity instances are retained in memory by the Kodein DI container, preventing garbage collection.

### Leak Details
- **Retained Size:** 4.6 MB per leaked Activity
- **Object Count:** 27,169 objects retained
- **Retention Duration:** 45+ seconds after Activity destruction
- **Status:** Activity#mDestroyed is true (confirmed leak)

### Reference Chain
```
MainActivity (destroyed)
  ↓ mainActivityViewModel$delegate
MainActivityViewModel
  ↓ di (LazyDI)
org.kodein.di.internal.DIImpl
  ↓ _container → tree → bindings
HashMap[DI$Key]
  ↓ ArrayList → DIDefinition → binding
InstanceBinding
  ↓ instance
MainActivity (LEAKED!)
```

### Root Cause
File: `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt:22`

```kotlin
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)
```

This creates a **strong reference** from the DI container to the Activity, creating a reference cycle that prevents garbage collection.

## Requirements

### Functional Requirements
1. **FR1:** Fix memory leak by removing strong reference from DI container to Activity
2. **FR2:** Maintain DI functionality for Activity injection
3. **FR3:** No breaking changes to existing DI usage patterns
4. **FR4:** LeakCanary must show zero Activity leaks after fix

### Non-Functional Requirements
1. **NFR1:** Minimal code changes (prefer single-line fix)
2. **NFR2:** Low risk - no architectural changes
3. **NFR3:** All existing tests must pass
4. **NFR4:** Build must succeed without warnings
5. **NFR5:** Performance impact: none or positive (less memory usage)

### Acceptance Criteria
1. ✅ DI binding changed from `instance` to `provider`
2. ✅ LeakCanary shows no Activity leaks after fix
3. ✅ All unit tests pass
4. ✅ All integration tests pass
5. ✅ Build succeeds without errors or warnings
6. ✅ Heap dump comparison shows reduced memory retention
7. ✅ No regressions in Activity lifecycle behavior

## Solution Approach

### Recommended: Option 1 - Use Provider Binding

**Change:**
```kotlin
// FROM:
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)

// TO:
bind<DIAwareComponentActivity>() with provider { this@DIAwareComponentActivity }
```

**Rationale:**
- **Single line change** - minimal risk
- `provider` creates factory function instead of holding instance reference
- Allows garbage collection when Activity is destroyed
- Follows Kodein best practices for Android lifecycle components
- No breaking changes to existing DI usage

### Alternatives Considered

**Option 2: WeakReference Wrapper**
- Pros: Explicit weak reference
- Cons: ~20 lines of code, higher risk, more complex

**Option 3: Clear DI on onDestroy**
- Pros: Explicit cleanup
- Cons: ~30 lines of code, relies on lifecycle callback timing, higher risk

## Implementation Scope

**Files to Modify:**
- `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareComponentActivity.kt` (1 line)

**Files to Verify:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/MainActivity.kt` (usage verification)
- `app/src/main/java/com/nononsenseapps/feeder/base/DIAwareViewModel.kt` (DI pattern verification)
- `app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt` (binding pattern verification)

**Testing:**
- Run LeakCanary to verify no leaks
- Run existing unit tests
- Run existing integration tests
- Create heap dump before/after comparison

## Success Metrics

1. **Memory Leak:** LeakCanary reports 0 Activity leaks
2. **Memory Retention:** < 100 KB retained after Activity destruction (vs 4.6 MB)
3. **Test Coverage:** 100% of existing tests pass
4. **Build Status:** Clean build, no warnings
5. **Code Changes:** 1 line changed (minimal diff)

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| DI resolution fails | Low | High | Comprehensive testing |
| Performance regression | Very Low | Medium | Benchmark before/after |
| Breaking existing usage | Low | High | Verify all DI usage patterns |
| Build warnings | Low | Low | Code review and verification |

## Out of Scope

- Refactoring entire DI architecture
- Changing DI framework
- Modifying other DI bindings (unless same pattern discovered)
- Adding new features
- Performance optimization beyond leak fix

## Dependencies

- LeakCanary must be integrated in debug builds
- Existing test suite must be runnable
- Android development environment configured

## Timeline Estimate

- Phase 1-7 (Planning): 30 minutes
- Phase 8 (Implementation): 15 minutes
- Phase 9 (Code Review): 15 minutes
- Phase 10 (Documentation): 15 minutes
- Phase 11-13 (Cleanup & Verify): 15 minutes

**Total Estimated Time:** 90 minutes
