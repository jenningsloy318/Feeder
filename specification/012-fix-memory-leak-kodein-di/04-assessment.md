# Code Assessment: DI Patterns in Feeder App

**Date:** 2026-01-03
**Scope:** Evaluate existing DI patterns and identify all memory leak risks

## Executive Summary

The Feeder app uses Kodein DI with a hierarchical structure:
- **Application-level DI:** Singleton bindings (correct)
- **Activity-level DI:** Mixed patterns (memory leak in 2 base classes)
- **ViewModel DI:** Factory pattern (correct)

## DI Architecture Overview

### Hierarchical Structure

```
FeederApplication (Application-level DI)
  ↓ extend
DIAwareComponentActivity (Activity-level DI) ← MEMORY LEAK HERE
  ↓ extend
MainActivity, ManageSettingsActivity, etc. (5 Activities)

DIAwareJobService (Service-level DI) ← MEMORY LEAK HERE
  ↓ extend
FeederJobService (1 JobService)
```

### DI Container Lifetimes

| Level | Lifetime | Binding Type | Memory Leak Risk |
|-------|----------|--------------|------------------|
| Application | Process lifetime | Singleton | ✅ None (correct) |
| Activity | Screen lifetime | Instance | ❌ HIGH (leak!) |
| ViewModel | Screen lifetime | Factory | ✅ None (correct) |
| JobService | Task lifetime | Instance | ❌ HIGH (leak!) |

## DI Binding Pattern Analysis

### 1. Application-Level Bindings (CORRECT ✅)

**File:** `FeederApplication.kt`

**Patterns:**
```kotlin
// Singleton bindings - correct for Application scope
bind<Application>() with singleton { this@FeederApplication }
bind<AppDatabase>() with singleton { AppDatabase.getInstance(this@FeederApplication) }
bind<FeedDao>() with singleton { instance<AppDatabase>().feedDao() }
bind<ContentResolver>() with singleton { contentResolver }
bind<JobScheduler>() with singleton { getSystemService(JobScheduler::class.java) }

// Instance bindings - correct for immutable objects
bind<ApplicationCoroutineScope>() with instance(applicationCoroutineScope)
bind<TTSStateHolder>() with instance(ttsStateHolder)
```

**Assessment:**
- ✅ All Application-level bindings are CORRECT
- ✅ `singleton` used for services and DAOs (proper lifetime)
- ✅ `instance` used for pre-created objects (applicationScope, TTSStateHolder)
- ✅ No memory leak risk (Application lives for process lifetime)

**Count:** 20+ bindings, all correct

### 2. Activity-Level Bindings (MIXED ❌)

**File:** `DIAwareComponentActivity.kt`

**Patterns:**
```kotlin
// CORRECT - provider for framework object
bind<MenuInflater>() with provider { menuInflater }

// WRONG - instance for Activity (MEMORY LEAK!)
bind<DIAwareComponentActivity>() with instance(this@DIAwareComponentActivity)

// QUESTIONABLE - singleton captures Activity reference
bind<ActivityLauncher>() with singleton {
    ActivityLauncher(
        this@DIAwareComponentActivity,  // Captures Activity
        di.direct.instance(),
    )
}
```

**Assessment:**
- ✅ Line 21: Correct use of `provider` for MenuInflater
- ❌ Line 22: WRONG use of `instance` for Activity (memory leak!)
- ⚠️ Lines 23-29: Singleton captures Activity reference indirectly

**Impact:**
- 5 Activities affected: MainActivity, ManageSettingsActivity, AddFeedFromShareActivity, ImportOPMLFileActivity, OpenLinkInDefaultActivity
- Memory leak: 4.6 MB per destroyed Activity
- Singleton ActivityLauncher will also be GC'd when Activity DI container is GC'd (if the leak is fixed)

### 3. JobService-Level Bindings (WRONG ❌)

**File:** `DIAwareJobService.kt`

**Patterns:**
```kotlin
// WRONG - instance for JobService (MEMORY LEAK!)
bind<DIAwareJobService>() with instance(this@DIAwareJobService)
```

**Assessment:**
- ❌ WRONG use of `instance` for JobService (memory leak!)
- Same pattern as Activity leak
- JobService is also a lifecycle component (should use `provider`)

**Impact:**
- 1 JobService affected: FeederJobService
- Memory leak: Likely similar size to Activity leak

### 4. ViewModel Bindings (CORRECT ✅)

**File:** `DIAwareViewModel.kt`

**Patterns:**
```kotlin
// Factory pattern - correct for ViewModels
bind<T>() with factory { activity: DIAwareComponentActivity ->
    val factory = DIAwareViewModelFactory(activity.di)
    ViewModelProvider(activity, factory).get(T::class.java)
}
```

**Usage in ArchModelModule.kt:**
```kotlin
bindWithActivityViewModelScope<MainActivityViewModel>()
bindWithActivityViewModelScope<OpenLinkInDefaultActivityViewModel>()
bindWithActivityViewModelScope<CommonActivityViewModel>()
bindWithComposableViewModelScope<SettingsViewModel>()
bindWithComposableViewModelScope<ArticleViewModel>()
// ... 8 more ViewModels
```

**Assessment:**
- ✅ CORRECT use of `factory` for ViewModels
- ✅ Factory pattern allows ViewModel lifecycle management
- ✅ ViewModels are scoped to Activity/Compose navigation
- ✅ No memory leak risk (ViewModels cleared when Activity cleared)

**Count:** 11 ViewModel bindings, all correct

### 5. Module-Level Bindings (CORRECT ✅)

**Files:** NetworkModule.kt, AndroidModule.kt, ArchModelModule.kt

**Patterns:**
```kotlin
// NetworkModule
bind<JsonAdapter<Feed>>() with provider { feedAdapter() }
bind<FeedParser>() with provider { FeedParser(di) }
bind<SyncRestClient>() with singleton { SyncRestClient(di) }
bind<RssLocalSync>() with singleton { RssLocalSync(di) }

// AndroidModule
bind<AndroidSystemStore>() with singleton { AndroidSystemStore(di) }

// ArchModelModule
bind<Repository>() with singleton { Repository(di) }
bind<SettingsStore>() with singleton { SettingsStore(di) }
bind<FeedStore>() with singleton { FeedStore(di) }
```

**Assessment:**
- ✅ All module bindings are CORRECT
- ✅ `singleton` used for stores and services (proper lifetime)
- ✅ `provider` used for parsers and adapters (proper pattern)
- ✅ No memory leak risk

**Count:** 20+ bindings, all correct

## Memory Leak Risk Summary

### High Risk (Must Fix)

| File | Line | Binding Type | Affected Components | Risk Level |
|------|------|--------------|---------------------|------------|
| `DIAwareComponentActivity.kt` | 22 | `instance(this@Activity)` | 5 Activities | CRITICAL |
| `DIAwareJobService.kt` | 16 | `instance(this@JobService)` | 1 JobService | HIGH |

### Low Risk (Acceptable)

| File | Line | Binding Type | Risk Level | Reason |
|------|------|--------------|------------|--------|
| `DIAwareComponentActivity.kt` | 23-29 | `singleton` capturing Activity | MEDIUM | Will be GC'd when leak fixed |
| `FeederApplication.kt` | 78 | `singleton { this@Application }` | NONE | Application lives forever |
| `FeederApplication.kt` | 208-210 | `instance(preCreatedObjects)` | NONE | Immutable objects |

## DI Usage Patterns

### Correct Patterns Found

1. **Singleton for Application-scoped services**
   ```kotlin
   bind<Repository>() with singleton { Repository(di) }
   bind<AppDatabase>() with singleton { AppDatabase.getInstance(context) }
   ```

2. **Provider for framework objects**
   ```kotlin
   bind<MenuInflater>() with provider { menuInflater }
   bind<JsonAdapter<Feed>>() with provider { feedAdapter() }
   ```

3. **Factory for ViewModels**
   ```kotlin
   bind<T>() with factory { activity: DIAwareComponentActivity ->
       ViewModelProvider(activity, factory).get(T::class.java)
   }
   ```

4. **Instance for pre-created immutable objects**
   ```kotlin
   bind<ApplicationCoroutineScope>() with instance(applicationCoroutineScope)
   ```

### Incorrect Pattern Found

1. **Instance for lifecycle components** ❌
   ```kotlin
   bind<DIAwareComponentActivity>() with instance(this@Activity)
   bind<DIAwareJobService>() with instance(this@JobService)
   ```

## Impact Analysis

### Components Affected by Leak

**Activities (5):**
1. `MainActivity` - Primary leak source (reported by LeakCanary)
2. `ManageSettingsActivity`
3. `AddFeedFromShareActivity`
4. `ImportOPMLFileActivity`
5. `OpenLinkInDefaultActivity`

**JobService (1):**
1. `FeederJobService`

### Memory Leak Calculation

**Per-Instance Leak:**
- MainActivity: 4.6 MB (confirmed by LeakCanary)
- Other Activities: Similar estimate (~4-5 MB each)
- JobService: Unknown but likely similar

**Worst-Case Scenario:**
- User rotates device multiple times: 5 Activities × 4.6 MB = **23 MB leaked**
- With multiple configuration changes: **100+ MB leaked**
- JobService leak adds additional pressure

### Functional Impact

- ✅ App continues to function (leak is silent)
- ❌ Memory usage increases over time
- ❌ May trigger GC more frequently
- ❌ May cause system to kill app under memory pressure
- ❌ Poor user experience on low-memory devices

## Fix Priority

### Priority 1: Fix Activity Leak (CRITICAL)
**File:** `DIAwareComponentActivity.kt:22`
**Change:** `instance` → `provider`
**Impact:** Fixes 5 Activities, eliminates 23+ MB leak potential

### Priority 2: Fix JobService Leak (HIGH)
**File:** `DIAwareJobService.kt:16`
**Change:** `instance` → `provider`
**Impact:** Fixes 1 JobService, eliminates additional leak

### Priority 3: Review ActivityLauncher Singleton (MEDIUM)
**File:** `DIAwareComponentActivity.kt:23-29`
**Assessment:** Will be automatically GC'd when Activity leak is fixed
**Action:** Monitor after fix, no change needed initially

## Standards and Conventions

### Project DI Patterns

**DO:**
- ✅ Use `singleton` for Application-scoped services
- ✅ Use `provider` for framework objects and lifecycle components
- ✅ Use `factory` for ViewModels
- ✅ Use `instance` for pre-created immutable objects

**DON'T:**
- ❌ Use `instance` for Activity, Fragment, or Service lifecycle components
- ❌ Use `singleton` for lifecycle components
- ❌ Hold strong references to destroyed components

### Consistency with Codebase

**Existing Correct Patterns:** 50+ bindings across the codebase
**Incorrect Patterns:** Only 2 (both in base classes)

**Assessment:**
- The codebase follows Kodein best practices EXCEPT for these 2 bindings
- The incorrect patterns are isolated to base classes
- Fixing these 2 lines will bring 100% of bindings into compliance

## Testing Requirements

### Unit Tests (Existing)
- All existing DI tests should pass
- No changes to test code needed

### Integration Tests (Existing)
- All existing integration tests should pass
- Activity lifecycle tests should show improved memory behavior

### Memory Leak Detection (Required)
1. **LeakCanary:** Must show 0 Activity leaks after fix
2. **Heap Dump:** Must not show destroyed Activity instances
3. **Memory Profiler:** Must show memory drop after onDestroy

## Conclusion

### Codebase Health
- **Overall DI Architecture:** EXCELLENT (98% correct patterns)
- **Memory Leaks:** CRITICAL (2 high-risk bindings)
- **Fix Complexity:** LOW (2 lines changed)

### Recommendations
1. ✅ Fix both `instance` bindings (Activity and JobService)
2. ✅ Verify with LeakCanary
3. ✅ Add comment explaining why `provider` is used
4. ⚠️ Consider code review for any new `instance` bindings

### Risk Assessment
- **Fix Risk:** LOW (standard Kodein pattern)
- **Not Fixing Risk:** HIGH (memory leaks worsen over time)
- **Recommendation:** Fix immediately

## Next Steps

1. ✅ Code assessment complete
2. → Phase 5.3: Architecture Design (document DI architecture decision)
3. → Phase 6: Specification Writing (create task list for both fixes)
4. → Phase 8: Execution (fix both files)

**Assessment Status:** ✅ COMPLETE
**Files to Fix:** 2
**Lines to Change:** 2
**Risk Level:** LOW
