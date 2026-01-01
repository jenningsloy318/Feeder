# Implementation Plan & Task List - Auto Fetch Full Article Feature

**Feature ID:** 006
**Plan Date:** 2026-01-01
**Project Manager:** Super Dev Coordinator
**Estimated Effort:** 8-12 hours

## Executive Summary

This implementation plan breaks down the "Auto Fetch Full Article" feature into manageable tasks with clear acceptance criteria, dependencies, and time estimates. Tasks are ordered to minimize risk and enable incremental validation.

## Table of Contents
1. [Implementation Strategy](#1-implementation-strategy)
2. [Task Breakdown](#2-task-breakdown)
3. [Dependencies](#3-dependencies)
4. [Testing Plan](#4-testing-plan)
5. [Risk Management](#5-risk-management)
6. [Timeline](#6-timeline)

---

## 1. Implementation Strategy

### Development Approach
**Incremental Development with Continuous Validation**

#### Phase-Based Approach
1. **Phase 1: Data Layer** - SettingsStore changes
2. **Phase 2: View Models** - Expose setting to ViewModels
3. **Phase 3: UI Layer** - Settings screen toggle
4. **Phase 4: Core Logic** - Auto-fetch in ArticleViewModel
5. **Phase 5: Testing** - Unit, integration, UI tests
6. **Phase 6: Validation** - End-to-end verification

#### Branch Strategy
```
master (protected)
  ↑
  ├── feature/auto-fetch-full-article (development branch)
  │    ├── Task commits (atomic)
  │    └── Testing commits
  └── Pull Request → Code Review → Merge
```

### Commit Strategy
**Atomic Commits - Each task = 1 commit**

#### Commit Message Pattern
```
feat: add auto-fetch setting to SettingsStore

- Add PREF_AUTO_FETCH_FULL_ARTICLE constant
- Add StateFlow for reactive updates
- Add setter method following existing pattern

Refs: #006
```

---

## 2. Task Breakdown

### Task 1: Add String Resources
**ID:** T-001
**Priority:** P0 (Critical)
**Estimate:** 15 minutes
**Complexity:** Trivial

**File:** `app/src/main/res/values/strings.xml`

**Changes:**
```xml
<!-- Add at end of file or in settings section -->
<string name="setting_auto_fetch_full_article">Auto Fetch Full Article</string>
<string name="setting_auto_fetch_full_article_description">Automatically fetch full article text when opening articles</string>
```

**Acceptance Criteria:**
- [ ] Two new strings added to strings.xml
- [ ] Strings compile successfully
- [ ] No syntax errors in XML

**Validation:**
```bash
./gradlew compileDebugSources
```

**Dependencies:** None

**Risk:** None

---

### Task 2: Add SettingsStore StateFlow
**ID:** T-002
**Priority:** P0 (Critical)
**Estimate:** 1 hour
**Complexity:** Low

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Changes:**
```kotlin
// 1. Add constant at top (around line 788 with other PREF_ constants)
const val PREF_AUTO_FETCH_FULL_ARTICLE = "pref_auto_fetch_full_article"

// 2. Add StateFlow and setter (around line 254, after syncOnlyWhenCharging)
private val _autoFetchFullArticle = MutableStateFlow(
    sp.getBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, false)
)
val autoFetchFullArticle: StateFlow<Boolean> = _autoFetchFullArticle.asStateFlow()

fun setAutoFetchFullArticle(value: Boolean) {
    _autoFetchFullArticle.value = value
    sp.edit().putBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, value).apply()
}
```

**Acceptance Criteria:**
- [ ] Constant added correctly
- [ ] StateFlow follows existing pattern
- [ ] Default value is `false`
- [ ] Setter updates both StateFlow and SharedPreferences
- [ ] Code compiles successfully
- [ ] Code style matches existing code

**Validation:**
```kotlin
// Test in SettingsStoreTest.kt
@Test
fun `auto fetch setting has correct default value`() {
    val settingsStore = SettingsStore(testDI)
    assertFalse(settingsStore.autoFetchFullArticle.first())
}

@Test
fun `setAutoFetchFullArticle updates value and persists`() {
    val settingsStore = SettingsStore(testDI)
    settingsStore.setAutoFetchFullArticle(true)
    assertTrue(settingsStore.autoFetchFullArticle.first())

    // Verify persistence
    val newStore = SettingsStore(testDI)
    assertTrue(newStore.autoFetchFullArticle.first())
}
```

**Dependencies:** T-001 (strings not needed but good practice)

**Risk:** Low

---

### Task 3: Expose Setting in SettingsViewModel
**ID:** T-003
**Priority:** P0 (Critical)
**Estimate:** 1 hour
**Complexity:** Low

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Changes:**
```kotlin
// 1. Add field to SettingsViewState data class
data class SettingsViewState(
    // ... existing fields
    val autoFetchFullArticle: Boolean = false,
    // ... existing fields
)

// 2. Add collection in init block or existing collection logic
init {
    // ... existing code

    // Collect auto-fetch setting
    settingsStore.autoFetchFullArticle
        .onEach { value ->
            _viewState.update { it.copy(autoFetchFullArticle = value) }
        }
        .launchIn(viewModelScope)
}

// 3. Add setter function
fun setAutoFetchFullArticle(value: Boolean) {
    settingsStore.setAutoFetchFullArticle(value)
}
```

**Acceptance Criteria:**
- [ ] Field added to ViewState
- [ ] Collection logic added in init
- [ ] Setter function delegates to SettingsStore
- [ ] Code compiles successfully
- [ ] No breaking changes to existing ViewState

**Dependencies:** T-002

**Risk:** Low

---

### Task 4: Add Toggle to Settings Screen UI
**ID:** T-004
**Priority:** P0 (Critical)
**Estimate:** 2-3 hours
**Complexity:** Low-Medium

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Changes:**
```kotlin
// 1. Add parameter to SettingsList composable
@Composable
fun SettingsList(
    // ... existing parameters
    autoFetchFullArticle: Boolean = false,
    onAutoFetchFullArticleChange: (Boolean) -> Unit = {},
    // ... existing parameters
)

// 2. In syncing section, add toggle
// Find the syncing section (look for "Syncing" header)
// Add after "Sync only when charging" toggle

// Auto Fetch Full Article
SettingsItem {
    SettingsText(
        text = stringResource(R.string.setting_auto_fetch_full_article),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    )
    Switch(
        checked = autoFetchFullArticle,
        onCheckedChange = onAutoFetchFullArticleChange
    )
}

// 3. Update SettingsScreen call
SettingsList(
    // ... existing parameters
    autoFetchFullArticle = viewState.autoFetchFullArticle,
    onAutoFetchFullArticleChange = { settingsViewModel.setAutoFetchFullArticle(it) }
    // ... existing parameters
)
```

**Acceptance Criteria:**
- [ ] Toggle appears in syncing section
- [ ] Toggle label correct
- [ ] Toggle bind to ViewState
- [ ] Toggle calls setter on change
- [ ] UI renders correctly
- [ ] Toggle state persists
- [ ] No visual glitches

**Validation:**
- Manual test: Open settings, toggle setting, close, reopen, verify persisted
- UI test: Toggle interaction

**Dependencies:** T-003

**Risk:** Low

---

### Task 5: Inject SettingsStore into ArticleViewModel
**ID:** T-005
**Priority:** P0 (Critical)
**Estimate:** 30 minutes
**Complexity:** Low

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Changes:**
```kotlin
// In class properties, add dependency
class ArticleViewModel(di: DI, private val state: SavedStateHandle) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    private val ttsStateHolder: TTSStateHolder by instance()
    private val fullTextParser: FullTextParser by instance()
    private val filePathProvider: FilePathProvider by instance()
    private val aiApi: AIApi by instance()
    private val applicationCoroutineScope: ApplicationCoroutineScope by instance()
    private val settingsStore: SettingsStore by instance() // ADD THIS
```

**Acceptance Criteria:**
- [ ] SettingsStore injected via Kodein
- [ ] Code compiles successfully
- [ ] No runtime DI errors

**Dependencies:** T-002

**Risk:** Low

---

### Task 6: Add Auto-Fetch Logic to ArticleViewModel
**ID:** T-006
**Priority:** P0 (Critical)
**Estimate:** 3-4 hours
**Complexity:** Medium

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Changes:**
```kotlin
// In init block, add auto-fetch logic
init {
    // ... existing init code

    // Add auto-fetch logic
    viewModelScope.launch {
        try {
            // Wait for article to be available
            val article = articleFlow.filterNotNull().first()

            // Check if auto-fetch is enabled
            val autoFetchEnabled = settingsStore.autoFetchFullArticle.first()

            // Fetch full text if:
            // 1. Setting is enabled
            // 2. Article doesn't already have full text
            if (autoFetchEnabled && !article.fullTextByDefault) {
                // Optional: Check network constraints
                // if (canFetchFullText()) {
                    toggleFullText()
                // }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in auto-fetch logic", e)
        }
    }
}

// Optional helper function (can be inlined)
private suspend fun canFetchFullText(): Boolean {
    // WiFi check (optional - FullTextParser may already handle this)
    val syncOnlyOnWifi = settingsStore.syncOnlyOnWifi.first()
    if (syncOnlyOnWifi && !isCurrentlyOnWifi()) {
        Log.d(LOG_TAG, "Auto-fetch skipped: not on WiFi")
        return false
    }

    // Charging check (optional)
    val syncOnlyWhenCharging = settingsStore.syncOnlyWhenCharging.first()
    if (syncOnlyWhenCharging && !isCurrentlyCharging()) {
        Log.d(LOG_TAG, "Auto-fetch skipped: not charging")
        return false
    }

    return true
}
```

**Acceptance Criteria:**
- [ ] Auto-fetch triggers when setting enabled
- [ ] Auto-fetch doesn't trigger when setting disabled
- [ ] Auto-fetch doesn't re-fetch if article already has full text
- [ ] Manual fetch button still works
- [ ] Article opens without blocking
- [ ] Loading indicator shows during fetch
- [ ] Errors handled gracefully
- [ ] Code compiles successfully

**Validation:**
- Unit tests for different scenarios
- Manual test: Enable setting, open article, verify fetch
- Manual test: Disable setting, open article, verify no fetch
- Manual test: Open article with full text, verify no re-fetch

**Dependencies:** T-005

**Risk:** Medium (core feature logic)

---

### Task 7: Write Unit Tests
**ID:** T-007
**Priority:** P1 (High)
**Estimate:** 2-3 hours
**Complexity:** Medium

**Files:**
- `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt` (NEW)

**Tests to Write:**

#### SettingsStore Tests (Add to existing file)
```kotlin
@Test
fun `auto fetch default value is false`() = runTest {
    val settingsStore = SettingsStore(testDI)
    assertFalse(settingsStore.autoFetchFullArticle.first())
}

@Test
fun `setAutoFetchFullArticle updates value`() = runTest {
    val settingsStore = SettingsStore(testDI)
    settingsStore.setAutoFetchFullArticle(true)
    assertTrue(settingsStore.autoFetchFullArticle.first())
}

@Test
fun `auto fetch setting persists across instances`() = runTest {
    val store1 = SettingsStore(testDI)
    store1.setAutoFetchFullArticle(true)

    val store2 = SettingsStore(testDI)
    assertTrue(store2.autoFetchFullArticle.first())
}
```

#### ArticleViewModel Tests (New file)
```kotlin
class ArticleViewModelTest {
    @Test
    fun `when auto-fetch enabled, fetch full text on init`() = runTest {
        // Mock settings to return true
        // Create ViewModel
        // Verify toggleFullText called
    }

    @Test
    fun `when auto-fetch disabled, do not fetch on init`() = runTest {
        // Mock settings to return false
        // Create ViewModel
        // Verify toggleFullText NOT called
    }

    @Test
    fun `when article has full text, do not refetch`() = runTest {
        // Mock article with fullTextByDefault = true
        // Mock settings enabled
        // Create ViewModel
        // Verify toggleFullText NOT called
    }
}
```

**Acceptance Criteria:**
- [ ] All SettingsStore tests passing
- [ ] All ArticleViewModel tests passing
- [ ] Test coverage > 80% for new code
- [ ] Tests run successfully in CI/CD

**Dependencies:** T-002, T-006

**Risk:** Low

---

### Task 8: Write UI Tests
**ID:** T-008
**Priority:** P2 (Medium)
**Estimate:** 1-2 hours
**Complexity:** Low-Medium

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsScreenTest.kt`

**Tests to Write:**
```kotlin
@Test
fun `auto fetch toggle appears in settings`() {
    composeTestRule.setContent {
        SettingsScreen(...)
    }

    composeTestRule
        .onNodeWithText("Auto Fetch Full Article")
        .assertIsDisplayed()
}

@Test
fun `toggling auto fetch setting updates state`() {
    composeTestRule.setContent {
        SettingsScreen(...)
    }

    // Toggle ON
    composeTestRule.onNode(hasContentDescription("Auto fetch toggle"))
        .performClick()

    // Verify
    // (Need to expose state for verification or check SettingsStore)
}
```

**Acceptance Criteria:**
- [ ] Toggle is visible
- [ ] Toggle is clickable
- [ ] Toggle state changes
- [ ] Setting persists

**Dependencies:** T-004

**Risk:** Low

---

### Task 9: Integration Testing
**ID:** T-009
**Priority:** P1 (High)
**Estimate:** 1-2 hours
**Complexity:** Medium

**Scenarios to Test:**
1. Enable setting → Open article → Verify auto-fetch
2. Disable setting → Open article → Verify no auto-fetch
3. Enable setting → Open article with full text → Verify no re-fetch
4. Enable setting → Open article → Toggle off → Open new article → Verify no fetch
5. WiFi-only test: Enable WiFi-only + auto-fetch → Open article on mobile → Verify no fetch

**Acceptance Criteria:**
- [ ] All scenarios pass
- [ ] No crashes
- [ ] No ANRs
- [ ] Expected behavior verified

**Dependencies:** T-006, T-007

**Risk:** Medium

---

### Task 10: Code Review & Refinement
**ID:** T-010
**Priority:** P1 (High)
**Estimate:** 1-2 hours
**Complexity:** Low

**Activities:**
1. Self-review all changes
2. Run linter: `./gradlew ktlintCheck`
3. Run tests: `./gradlew test`
4. Fix any issues
5. Optimize code
6. Add comments if needed

**Acceptance Criteria:**
- [ ] No linting errors
- [ ] All tests passing
- [ ] Code follows project conventions
- [ ] No obvious bugs
- [ ] Performance acceptable

**Dependencies:** T-001 through T-009

**Risk:** Low

---

## 3. Dependencies

### Dependency Graph
```
T-001 (Strings) ──────────────────────────────┐
                                               │
T-002 (SettingsStore) ───────────────────────┤
                          │                   │
                          ▼                   │
                   T-003 (ViewModel) ─────────┤
                          │                   │
                          ▼                   │
                   T-004 (Settings UI) ◄──────┘
                          │
                          ▼
T-005 (Inject in ArticleViewModel) ──────────┐
                          │                   │
                          ▼                   │
                   T-006 (Auto-Fetch Logic)   │
                          │                   │
                          ▼                   │
                   T-007 (Unit Tests) ◄────────┘
                          │
                          ▼
                   T-008 (UI Tests)
                          │
                          ▼
                   T-009 (Integration Tests)
                          │
                          ▼
                   T-010 (Code Review)
```

### Critical Path
```
T-002 → T-003 → T-004 → T-005 → T-006 → T-007 → T-010
```

**Minimum Viable Feature (MVP):** T-002, T-003, T-004, T-005, T-006

---

## 4. Testing Plan

### Test Pyramid
```
        ┌─────────┐
        │  E2E    │  10% (T-009)
        │  Tests  │
       ─┼─────────┼─
       ─│   UI     │  20% (T-008)
        │  Tests   │
       ─┼─────────┼─
       ─│  Unit    │  70% (T-007)
        │  Tests   │
        └─────────┘
```

### Test Execution Order
1. **After Each Task:** Run relevant unit tests
2. **After T-006:** Run all unit tests
3. **After T-008:** Run UI tests
4. **After T-009:** Run integration tests
5. **Before T-010:** Run full test suite

### Continuous Integration
```yaml
# .github/workflows/test.yml (example)
name: Test Auto-Fetch Feature
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - checkout
      - run: ./gradlew test
      - run: ./gradlew connectedAndroidTest
```

---

## 5. Risk Management

### Risk Register

| Risk | Probability | Impact | Severity | Mitigation |
|------|-------------|--------|----------|------------|
| Breaking article loading | Low | High | Medium | Extensive testing, can disable easily |
| Performance degradation | Low | Medium | Low | Async operations, respect constraints |
| User data overage | Low | Medium | Low | Default OFF, respect WiFi setting |
| Test coverage gaps | Medium | Low | Low | Comprehensive test plan |
| Merge conflicts | Low | Low | Low | Small, focused changes |

### Mitigation Strategies

#### Strategy 1: Feature Toggle
```kotlin
// Can disable in production if needed
if (BuildConfig.AUTO_FETCH_ENABLED) {
    // Auto-fetch logic
}
```

#### Strategy 2: Safe Default
```kotlin
// Default is OFF - no impact if buggy
private val _autoFetchFullArticle = MutableStateFlow(
    sp.getBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, false) // Safe default
)
```

#### Strategy 3: Easy Rollback
```kotlin
// Wrap in try-catch to prevent crashes
try {
    if (autoFetchEnabled) {
        toggleFullText()
    }
} catch (e: Exception) {
    Log.e(LOG_TAG, "Auto-fetch failed", e)
    // Continue without auto-fetch
}
```

---

## 6. Timeline

### Sprint Plan (5 Days)

#### Day 1: Foundation
- **Hours:** 2-3
- **Tasks:** T-001, T-002
- **Deliverable:** SettingsStore changes complete

#### Day 2: View Model & UI
- **Hours:** 3-4
- **Tasks:** T-003, T-004
- **Deliverable:** Settings UI toggle functional

#### Day 3: Core Feature
- **Hours:** 4-5
- **Tasks:** T-005, T-006
- **Deliverable:** Auto-fetch logic working

#### Day 4: Testing
- **Hours:** 3-4
- **Tasks:** T-007, T-008, T-009
- **Deliverable:** All tests passing

#### Day 5: Polish & Review
- **Hours:** 2-3
- **Tasks:** T-010
- **Deliverable:** Code review ready

### Total Estimate: **14-19 hours** (across 5 days)

---

## 7. Success Criteria

### Functional Criteria
- [ ] Setting toggle appears in Settings → Syncing
- [ ] Setting persists across app restarts
- [ ] Auto-fetch triggers when enabled
- [ ] Auto-fetch doesn't trigger when disabled
- [ ] Manual fetch button still works
- [ ] No article loading delays

### Quality Criteria
- [ ] Unit test coverage > 80%
- [ ] All tests passing
- [ ] No linting errors
- [ ] No crashes or ANRs
- [ ] Performance acceptable

### User Experience Criteria
- [ ] Setting easy to find and toggle
- [ ] Label and description clear
- [ ] Behavior matches expectations
- [ ] No confusing edge cases

---

## 8. Rollout Plan

### Phase 1: Internal Testing (1 day)
- Deploy to internal testers
- Gather feedback
- Fix critical bugs

### Phase 2: Beta Release (3-5 days)
- Deploy to beta channel
- Monitor crash reports
- Monitor performance metrics
- Gather user feedback

### Phase 3: Stable Release
- Merge to master
- Tag release
- Deploy to production
- Monitor metrics for 1 week

---

## 9. Task Checklist

Use this checklist to track progress:

### Data Layer
- [ ] T-001: Add string resources
- [ ] T-002: Add SettingsStore StateFlow

### View Model Layer
- [ ] T-003: Expose in SettingsViewModel
- [ ] T-005: Inject into ArticleViewModel

### UI Layer
- [ ] T-004: Add toggle to Settings screen

### Core Logic
- [ ] T-006: Implement auto-fetch logic

### Testing
- [ ] T-007: Write unit tests
- [ ] T-008: Write UI tests
- [ ] T-009: Integration testing

### Polish
- [ ] T-010: Code review & refinement

### Documentation
- [ ] Update CHANGELOG.md
- [ ] Update README.md (if needed)
- [ ] Create release notes

---

**Implementation Plan Complete:** ✅
**Ready for Execution:** ✅
**Total Estimated Effort:** 14-19 hours

---

**Document Version:** 1.0
**Last Updated:** 2026-01-01
**Next Phase:** Specification Review (Phase 7)
