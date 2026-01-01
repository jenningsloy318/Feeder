# Technical Specification - Auto Fetch Full Article Feature

**Feature ID:** 006
**Spec Date:** 2026-01-01
**Technical Lead:** Super Dev Spec Writer
**Project:** Feeder Android RSS Reader

## Executive Summary

This technical specification defines the implementation details for the "Auto Fetch Full Article" feature in Feeder. The feature adds a user preference setting that automatically fetches full article content when articles are opened, eliminating the need for manual button clicks.

## Table of Contents
1. [Feature Overview](#1-feature-overview)
2. [Technical Architecture](#2-technical-architecture)
3. [Data Model Changes](#3-data-model-changes)
4. [API Specifications](#4-api-specifications)
5. [Implementation Details](#5-implementation-details)
6. [Testing Strategy](#6-testing-strategy)
7. [Performance Considerations](#7-performance-considerations)
8. [Security Considerations](#8-security-considerations)
9. [Deployment Plan](#9-deployment-plan)

---

## 1. Feature Overview

### Feature Description
Add a user preference setting that automatically fetches full article text when opening an article, removing the need to manually click the "Fetch Full Article" button.

### Scope
- **In Scope:** Settings UI toggle, auto-fetch logic, persistence, tests
- **Out of Scope:** Data migration (not needed), backend changes (none), new permissions (none)

### Dependencies
- Kotlin 1.9+
- Jetpack Compose BOM 2024.10+
- Material3
- Coroutines 1.9+
- Kodein DI 7.22+
- Room Database

---

## 2. Technical Architecture

### Architecture Pattern
**MVVM (Model-View-ViewModel)** with Repository pattern

### Data Flow Diagram
```
┌──────────────┐
│   Settings   │ SharedPreferences
│   Screen     │───────┐
│   (Compose)  │       │
└──────────────┘       │
       │               │
       │ observes      │ reads/writes
       ▼               ▼
┌──────────────┐  ┌────────────────┐
│ Settings     │  │  SettingsStore │
│ ViewModel    │◄─┤ (StateFlow)    │
└──────────────┘  └────────────────┘
                          │
                          │ exposes
                          ▼
                   ┌────────────────┐
                   │ Article        │
                   │ ViewModel      │
                   └────────────────┘
                          │
                          │ uses
                          ▼
                   ┌────────────────┐
                   │ FullTextParser │
                   └────────────────┘
```

### Component Interaction
```
User Toggle → SettingsViewModel → SettingsStore
                                        ↓
                                   SharedPreferences
                                        ↓
Article Opens → ArticleViewModel → SettingsStore (read)
                                        ↓
                                   FullTextParser (fetch)
```

---

## 3. Data Model Changes

### SharedPreferences Keys

#### New Key
```kotlin
// In SettingsStore.kt
const val PREF_AUTO_FETCH_FULL_ARTICLE = "pref_auto_fetch_full_article"
```

**Data Type:** Boolean
**Default Value:** false (OFF)
**Persistence:** SharedPreferences
**Access Pattern:** StateFlow wrapper for reactive updates

### StateFlow Definition

```kotlin
// In SettingsStore.kt (around line 254)
private val _autoFetchFullArticle = MutableStateFlow(
    sp.getBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, false)
)
val autoFetchFullArticle: StateFlow<Boolean> = _autoFetchFullArticle.asStateFlow()

fun setAutoFetchFullArticle(value: Boolean) {
    _autoFetchFullArticle.value = value
    sp.edit().putBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, value).apply()
}
```

### No Database Changes Required
- ✅ No Room table modifications
- ✅ No schema migrations
- ✅ No data migrations

---

## 4. API Specifications

### SettingsStore API

#### Get Auto-Fetch Setting
```kotlin
/**
 * Auto fetch full article setting state flow
 * @return StateFlow<Boolean> - true if enabled, false otherwise
 */
val autoFetchFullArticle: StateFlow<Boolean>
```

#### Set Auto-Fetch Setting
```kotlin
/**
 * Set the auto fetch full article setting
 * @param value Boolean - true to enable, false to disable
 */
fun setAutoFetchFullArticle(value: Boolean)
```

### ArticleViewModel API

#### Init Block Logic (Internal)
```kotlin
/**
 * Initialize article view model
 * Auto-fetches full text if setting is enabled
 */
init {
    viewModelScope.launch {
        val article = articleFlow.filterNotNull().first()
        val autoFetchEnabled = settingsStore.autoFetchFullArticle.first()

        if (autoFetchEnabled && !article.fullTextByDefault) {
            toggleFullText()
        }
    }
}
```

#### Existing Method (No Changes)
```kotlin
/**
 * Toggle full text fetch (existing method - reused)
 */
fun toggleFullText()
```

---

## 5. Implementation Details

### File 1: SettingsStore.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
**Lines to Add:** ~8
**Location:** After line 253 (after `syncOnlyWhenCharging` setting)

```kotlin
// Around line 254 in SettingsStore.kt

// Add constant at top of file with other PREF_ constants
const val PREF_AUTO_FETCH_FULL_ARTICLE = "pref_auto_fetch_full_article"

// Add StateFlow and setter after syncOnlyWhenCharging (around line 254)
private val _autoFetchFullArticle = MutableStateFlow(
    sp.getBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, false)
)
val autoFetchFullArticle = _autoFetchFullArticle.asStateFlow()

fun setAutoFetchFullArticle(value: Boolean) {
    _autoFetchFullArticle.value = value
    sp.edit().putBoolean(PREF_AUTO_FETCH_FULL_ARTICLE, value).apply()
}
```

**Rationale:**
- Follows exact pattern of existing sync settings
- Placed logically with sync-related settings
- Default value `false` preserves current behavior
- Uses `apply()` for async write

---

### File 2: SettingsViewModel.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`
**Lines to Add:** ~5
**Location:** In viewState data class and ViewModel class

```kotlin
// In SettingsViewState data class
data class SettingsViewState(
    // ... existing fields
    val autoFetchFullArticle: Boolean = false,
    // ... existing fields
)

// In SettingsViewModel class
init {
    // ... existing init code
    settingsStore.autoFetchFullArticle.collect { value ->
        _viewState.update { it.copy(autoFetchFullArticle = value) }
    }
}

// Add setter function
fun setAutoFetchFullArticle(value: Boolean) {
    settingsStore.setAutoFetchFullArticle(value)
}
```

**Rationale:**
- Exposes setting to UI via ViewState
- Reactive collection for automatic updates
- Simple setter delegation to SettingsStore

---

### File 3: Settings.kt (Compose UI)

**Path:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
**Lines to Add:** ~15
**Location:** In syncing section, after "Sync only when charging" toggle

```kotlin
// In SettingsList composable function parameters
fun SettingsList(
    // ... existing parameters
    autoFetchFullArticle: Boolean = false,
    onAutoFetchFullArticleChange: (Boolean) -> Unit = {},
    // ... existing parameters
)

// In syncing section (around existing sync toggles)
// Find the syncing section and add this row

// Auto Fetch Full Article toggle
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
```

**Rationale:**
- Matches existing SettingsItem pattern
- Consistent with other toggle switches
- Simple boolean binding

---

### File 4: ArticleViewModel.kt

**Path:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
**Lines to Add:** ~15-20
**Location:** In init block and potentially new helper function

```kotlin
// In class properties (add dependency)
private val settingsStore: SettingsStore by instance()

// In init block (modify or add to existing init)
init {
    // Existing init code...

    // Add auto-fetch logic
    viewModelScope.launch {
        try {
            // Wait for article to load
            val article = articleFlow.filterNotNull().first()

            // Check if auto-fetch is enabled
            val autoFetchEnabled = settingsStore.autoFetchFullArticle.first()

            // Fetch full text if needed
            if (autoFetchEnabled && !article.fullTextByDefault) {
                // Check network constraints
                if (canFetchFullText()) {
                    toggleFullText()
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error checking auto-fetch setting", e)
        }
    }
}

// Optional helper function (can be inlined)
private suspend fun canFetchFullText(): Boolean {
    // Check WiFi constraint
    if (settingsStore.syncOnlyOnWifi.first() && !isCurrentlyOnWifi()) {
        return false
    }

    // Check charging constraint
    if (settingsStore.syncOnlyWhenCharging.first() && !isCurrentlyCharging()) {
        return false
    }

    return true
}
```

**Rationale:**
- Async check using coroutines
- Waits for article to be available
- Checks setting value (one-time read, not continuous)
- Respects network constraints
- Error handling for robustness
- Uses existing `toggleFullText()` method

**Important Notes:**
- Uses `first()` to get current value (not continuous collection)
- Checks `fullTextByDefault` to avoid re-fetching
- Network constraint checks optional (can be in FullTextParser)
- Non-blocking async operation

---

### File 5: strings.xml

**Path:** `app/src/main/res/values/strings.xml`
**Lines to Add:** 2

```xml
<!-- Auto Fetch Full Article Setting -->
<string name="setting_auto_fetch_full_article">Auto Fetch Full Article</string>
<string name="setting_auto_fetch_full_article_description">Automatically fetch full article text when opening articles</string>
```

**Rationale:**
- Clear, concise label
- Descriptive subtitle for context
- Follows existing string naming conventions

---

## 6. Implementation Strategy

### Implementation Order

#### Phase 1: Foundation (1-2 hours)
1. ✏️ Add strings to `strings.xml`
2. ✏️ Add StateFlow to `SettingsStore.kt`
3. ✅ Test SharedPreferences persistence

#### Phase 2: UI Integration (2-3 hours)
1. ✏️ Update `SettingsViewModel.kt`
2. ✏️ Add toggle to `Settings.kt`
3. ✅ Test toggle interaction
4. ✅ Verify setting persistence

#### Phase 3: Core Feature (3-4 hours)
1. ✏️ Modify `ArticleViewModel.kt` init block
2. ✏️ Add network constraint checks (optional)
3. ✅ Test auto-fetch on article open
4. ✅ Test manual fetch still works

#### Phase 4: Testing (2-3 hours)
1. ✏️ Write SettingsStore tests
2. ✏️ Write ArticleViewModel tests
3. ✏️ Write UI tests
4. ✅ Integration testing

**Total Estimated Time:** 8-12 hours

---

## 7. Testing Strategy

### Unit Tests

#### Test 1: SettingsStore Persistence
```kotlin
@Test
fun `auto fetch setting persists correctly`() = runTest {
    // Given
    val settingsStore = SettingsStore(testDI)

    // When
    settingsStore.setAutoFetchFullArticle(true)
    val value1 = settingsStore.autoFetchFullArticle.first()

    // Then
    assertTrue(value1)

    // Verify persistence
    val newStore = SettingsStore(testDI)
    val value2 = newStore.autoFetchFullArticle.first()
    assertTrue(value2)
}
```

#### Test 2: SettingsStore Default Value
```kotlin
@Test
fun `auto fetch defaults to false`() = runTest {
    val settingsStore = SettingsStore(testDI)
    val value = settingsStore.autoFetchFullArticle.first()
    assertFalse(value)
}
```

#### Test 3: ArticleViewModel Auto-Fetch Enabled
```kotlin
@Test
fun `when auto-fetch enabled, fetch full text on init`() = runTest {
    // Given
    val settingsStore = mockk<SettingsStore> {
        coEvery { autoFetchFullArticle } returns flowOf(true)
    }
    val article = Article(
        id = 1L,
        fullTextByDefault = false,
        // ... other fields
    )

    // When
    val viewModel = ArticleViewModel(testDI, savedStateHandle)
    advanceUntilIdle()

    // Then
    verify { fullTextParser.parseFullArticleIfMissing(any()) }
}
```

#### Test 4: ArticleViewModel Auto-Fetch Disabled
```kotlin
@Test
fun `when auto-fetch disabled, do not fetch full text`() = runTest {
    // Given
    val settingsStore = mockk<SettingsStore> {
        coEvery { autoFetchFullArticle } returns flowOf(false)
    }

    // When
    val viewModel = ArticleViewModel(testDI, savedStateHandle)
    advanceUntilIdle()

    // Then
    verify(exactly = 0) { fullTextParser.parseFullArticleIfMissing(any()) }
}
```

#### Test 5: ArticleViewModel Already Fetched
```kotlin
@Test
fun `when article already has full text, do not refetch`() = runTest {
    // Given
    val settingsStore = mockk<SettingsStore> {
        coEvery { autoFetchFullArticle } returns flowOf(true)
    }
    val article = Article(
        id = 1L,
        fullTextByDefault = true, // Already fetched
        // ...
    )

    // When
    val viewModel = ArticleViewModel(testDI, savedStateHandle)
    advanceUntilIdle()

    // Then
    verify(exactly = 0) { fullTextParser.parseFullArticleIfMissing(any()) }
}
```

### Integration Tests

#### Test 6: End-to-End Auto-Fetch Flow
```kotlin
@Test
fun `enable setting and open article triggers auto-fetch`() = runTest {
    // 1. Enable setting
    settingsStore.setAutoFetchFullArticle(true)

    // 2. Open article
    val viewModel = ArticleViewModel(di, savedStateHandle)
    advanceUntilIdle()

    // 3. Verify full text fetched
    assertEquals(TextToDisplay.CONTENT, viewModel.textToDisplay.value)
}
```

### UI Tests

#### Test 7: Settings Toggle
```kotlin
@Test
fun `toggle auto-fetch setting updates state`() {
    composeTestRule.setContent {
        val settingsViewModel = SettingsViewModel(di)
        SettingsScreen(
            settingsViewModel = settingsViewModel,
            onNavigateUp = {}
        )
    }

    // Find toggle
    val toggle = composeTestRule
        .onNodeWithText("Auto Fetch Full Article")
        .assertIsDisplayed()

    // Toggle ON
    composeTestRule.onNode(hasContentDescription("Auto fetch toggle"))
        .performClick()

    // Verify
    assertTrue(settingsViewModel.autoFetchFullArticle.value)
}
```

### Test Coverage Targets
- **Unit Tests:** 80%+ for new code
- **Integration Tests:** Core happy path
- **UI Tests:** Toggle interaction

---

## 8. Performance Considerations

### Performance Impact Analysis

#### Memory Impact
- **Additional StateFlow:** ~100 bytes
- **String Resources:** ~200 bytes
- **Total:** < 1 KB
- **Impact:** Negligible ✅

#### CPU Impact
- **Setting Check:** O(1) - single boolean read
- **Auto-Fetch Trigger:** Same as manual fetch
- **Additional Operations:** Minimal ✅

#### Network Impact
- **Conditional:** Only if setting enabled
- **User Controlled:** Can be disabled
- **Constraint Aware:** Honors WiFi/charging settings
- **Impact:** User-controllable ✅

#### Battery Impact
- **Per-Article Check:** One additional boolean read
- **Fetch Operation:** Same as manual (existing code)
- **Mitigation:** Respects charging-only setting
- **Impact:** Minimal ✅

#### Startup Impact
- **Setting Read:** Async in init block
- **No Blocking Operations:** Uses coroutines
- **Impact:** None ✅

### Optimization Strategies

#### Strategy 1: Lazy Fetch
```kotlin
// Don't block article opening
// Fetch in background after UI is shown
viewModelScope.launch {
    delay(100) // Let UI render first
    if (shouldAutoFetch()) {
        toggleFullText()
    }
}
```

#### Strategy 2: Caching Check
```kotlin
// Cache article.fetchedByDefault check
// Don't re-fetch if already fetched
if (autoFetchEnabled && !article.fullTextByDefault) {
    toggleFullText()
}
```

#### Strategy 3: Network Constraint Respect
```kotlin
// Don't fetch if on mobile data with WiFi-only setting
if (settingsStore.syncOnlyOnWifi.value && !isOnWifi()) {
    return // Skip fetch
}
```

---

## 9. Security Considerations

### Security Assessment: ✅ LOW RISK

#### Data Privacy
- **Local Storage:** SharedPreferences (encrypted on device)
- **No Network Transmission:** Setting never leaves device
- **No Personal Data:** Boolean flag only
- **Risk:** None ✅

#### Network Security
- **Existing Stack:** Reuses FullTextParser
- **Permission:** Uses existing INTERNET permission
- **Certificate Pinning:** Inherited from existing code
- **Risk:** None ✅

#### Permission Requirements
**No new permissions required** ✅

---

## 10. Deployment Plan

### Deployment Strategy
**Release Type:** Feature Release (minor version bump)

### Version Bump
```kotlin
// version.gradle.kts or build.gradle.kts
versionName = "x.y.z" → "x.y.(z+1)" // Minor bump
```

### Release Notes
```
Feature: Auto Fetch Full Article
- Added setting to automatically fetch full article text when opening articles
- Toggle in Settings → Syncing section
- Default disabled for data conservation
- Respects existing WiFi-only and charging-only settings
```

### Rollout Plan
1. **Internal Testing:** 1 day
2. **Beta Release:** 3-5 days
3. **Stable Release:** After beta validation

### Rollback Plan
- Feature can be disabled server-side (if remote config added)
- Or: Revert commit in next hotfix
- No data migration needed (safe rollback)

---

## 11. Monitoring & Analytics

### Metrics to Track

#### Usage Metrics
- **Setting Enablement Rate:** % of users who enable the setting
- **Auto-Fetch Success Rate:** % of successful fetches
- **Auto-Fetch Failure Rate:** % of failed fetches
- **Manual Fetch Rate:** % of users still using manual button

#### Performance Metrics
- **Article Open Time:** p50, p95, p99 with auto-fetch enabled vs disabled
- **Fetch Time:** Average duration of full text fetch
- **Error Rate:** Types and frequency of errors

#### User Experience Metrics
- **Setting Toggle Frequency:** How often users change the setting
- **Data Usage Impact:** Average data consumption per user
- **Battery Impact:** Battery drain attributable to auto-fetch

---

## 12. Rollback Criteria

### Rollback Triggers
1. **Crash Rate:** > 1% crash rate on article open
2. **ANR Rate:** > 0.5% ANR rate on article open
3. **Data Usage:** > 50% increase in user complaints
4. **Battery:** > 20% increase in battery drain complaints

### Rollback Plan
1. **Disable Feature:** Default to OFF in code
2. **Hotfix Release:** Next immediate version
3. **Communication:** Inform users of rollback
4. **Investigation:** Analyze metrics, fix issues, re-release

---

## 13. Known Limitations

### Current Limitations
1. **No Bulk Settings:** Cannot enable per-feed (global only)
2. **No Smart Detection:** Doesn't detect which articles need fetching
3. **No Queuing:** Doesn't queue multiple fetches
4. **No Sync Across Devices:** Setting local to device only

### Future Enhancements (Out of Scope)
- Per-feed auto-fetch settings
- Smart detection (short articles vs. truncated)
- Bulk fetch for offline reading
- Sync setting across devices

---

## 14. Compliance & Standards

### Android Guidelines Compliance
- ✅ Material3 Design System
- ✅ Accessibility Guidelines
- ✅ Performance Best Practices
- ✅ Security Best Practices

### Code Quality Standards
- ✅ Kotlin Coding Conventions
- ✅ Coroutines Best Practices
- ✅ Jetpack Compose Best Practices
- ✅ MVVM Architecture Pattern

---

## 15. Sign-Off

### Development Checklist
- [ ] All files modified per specification
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Strings localized (all languages)

### QA Checklist
- [ ] Feature works as designed
- [ ] No regressions in existing functionality
- [ ] Performance acceptable
- [ ] No crashes or ANRs
- [ ] Accessibility verified

### Release Checklist
- [ ] Version bumped
- [ ] Release notes written
- [ ] Beta testing complete
- [ ] Stakeholder approval obtained
- [ ] Deployed to production

---

**Technical Specification Complete:** ✅
**Ready for Implementation:** ✅
**Estimated Effort:** 8-12 hours
**Risk Level:** Low

---

**Document Version:** 1.0
**Last Updated:** 2026-01-01
**Next:** Implementation Plan & Task List
