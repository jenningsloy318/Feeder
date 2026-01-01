# Requirements Document - Auto Fetch Full Article Feature

**Feature ID:** 006
**Feature Name:** Auto Fetch Full Article
**Project:** Feeder (Android RSS Reader)
**Date:** 2026-01-01
**Status:** Draft

## Executive Summary

Currently, Feeder requires users to manually click a "Fetch Full Article" button in the article view to load the full text content. This feature adds a user preference setting to automatically fetch full article content when opening an article, improving the user experience by eliminating the manual step.

## User Stories

### Primary User Story
**As a** feed reader user
**I want** the app to automatically fetch full article content when I open an article
**So that** I don't have to manually click the "Fetch Full Article" button every time

### Secondary User Story
**As a** feed reader user
**I want** to control whether full articles are auto-fetched through a settings toggle
**So that** I can conserve data usage and battery when needed

## Functional Requirements

### FR-001: Settings Toggle
**Priority:** Must Have
**Description:** Add a toggle switch in Settings → Syncing section

**Acceptance Criteria:**
- Toggle labeled "Auto Fetch Full Article" (or similar localized text)
- Toggle is OFF by default (current behavior preserved)
- Toggle persists across app restarts using SharedPreferences
- Toggle is located in the "Syncing" settings section alongside existing sync settings

**Rationale:** Placing in Syncing section groups it with other network/transfer related settings like "Sync on WiFi only" and "Sync only when charging"

### FR-002: Auto-Fetch on Article Open
**Priority:** Must Have
**Description:** When the setting is enabled, automatically fetch full article content when an article is opened

**Acceptance Criteria:**
- When toggle is ON: Automatically call full text fetch when ArticleScreen loads
- When toggle is OFF: Require manual click of "Fetch Full Article" button (current behavior)
- Auto-fetch only occurs if full text hasn't been fetched yet
- Show loading indicator during auto-fetch
- Handle errors gracefully (show error message if fetch fails)
- User can still manually trigger fetch even if auto-fetch is enabled

**Rationale:** Matches user expectation of "auto" behavior while preserving manual override capability

### FR-003: Settings Storage
**Priority:** Must Have
**Description:** Store the auto-fetch preference persistently

**Acceptance Criteria:**
- Use SharedPreferences with key: `pref_auto_fetch_full_article`
- Default value: `false` (OFF)
- Expose via SettingsStore as `StateFlow<Boolean>` for reactive UI
- Provide setter method: `setAutoFetchFullArticle(value: Boolean)`
- Follow existing pattern used by other sync settings (syncOnlyOnWifi, syncOnResume, etc.)

**Rationale:** Consistent with existing settings architecture

## Non-Functional Requirements

### NFR-001: Performance
**Impact:** Low
**Description:** Auto-fetch should not significantly delay article opening

**Acceptance Criteria:**
- Article view must render within 100ms even with auto-fetch enabled
- Full text fetch happens asynchronously after article view is shown
- Loading indicator appears while fetch is in progress

### NFR-002: Data Usage
**Impact:** Medium
**Description:** Respect user's data constraints

**Acceptance Criteria:**
- Honor existing "Sync only on WiFi" setting when auto-fetching
- Honor existing "Sync only when charging" setting when auto-fetching
- Users concerned about data can disable auto-fetch or use WiFi-only mode

### NFR-003: Backward Compatibility
**Impact:** High
**Description:** Don't break existing functionality

**Acceptance Criteria:**
- Default is OFF, preserving current behavior for existing users
- Manual fetch button continues to work regardless of setting
- No breaking changes to existing APIs or data models

## Technical Context

### Existing Components Identified

#### 1. Settings Store
**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- Uses SharedPreferences for persistence
- Pattern: `StateFlow` for reactive values
- Example sync settings:
  ```kotlin
  private val _syncOnResume = MutableStateFlow(sp.getBoolean(PREF_SYNC_ON_RESUME, false))
  val syncOnResume = _syncOnResume.asStateFlow()
  fun setSyncOnResume(value: Boolean) { ... }
  ```

#### 2. Full Text Parser
**File:** `app/src/main/java/com/nononsenseapps/feeder/model/FullTextParser.kt`
- Handles full text extraction from article URLs
- Called by: `ArticleViewModel.toggleFullText()`
- Async operation that shows loading state

#### 3. Article Screen
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- Contains "Fetch Full Article" button
- Calls `onToggleFullText` which triggers `viewModel.toggleFullText()`
- Shows loading/error states for full text fetch

#### 4. Article ViewModel
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- Manages article display state
- Has `toggleFullText()` method that calls `fullTextParser.parseFullArticleIfMissing()`
- Uses `TextToDisplay` enum to represent states:
  - `CONTENT` - showing article content
  - `LOADING_FULLTEXT` - currently fetching
  - `FAILED_*` - various error states

#### 5. Settings UI
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
- Contains syncing section with existing toggles
- Uses Material3 Switch components
- Pattern:
  ```kotlin
  Switch(
      checked = currentSetting,
      onCheckedChange = { onSettingChange(it) }
  )
  ```

### Data Flow

**Current Manual Flow:**
```
User clicks "Fetch Full Article" button
  → onToggleFullText() callback
  → ArticleViewModel.toggleFullText()
  → fullTextParser.parseFullArticleIfMissing()
  → Update UI state to LOADING_FULLTEXT
  → Fetch complete → Update to CONTENT or FAILED_*
```

**Proposed Auto-Fetch Flow:**
```
ArticleScreen initializes
  → Check SettingsStore.autoFetchFullArticle
  → If TRUE: Automatically call toggleFullText()
  → Same fetch flow as manual
  → If FALSE: Wait for manual button click (current behavior)
```

## UI/UX Requirements

### Settings Screen
**Location:** Settings → Syncing section

**Visual Design:**
- Follow existing Material3 Switch pattern
- Label: "Auto Fetch Full Article" (localized)
- Placement: Below existing sync settings (WiFi only, Charging only, etc.)
- Description: "Automatically fetch full article text when opening articles"

**Behavior:**
- Toggle ON: Auto-fetch enabled
- Toggle OFF: Manual fetch only (current default)

### Article Screen Behavior
**When Auto-Fetch Enabled:**
- Loading indicator appears automatically when article opens
- No user interaction required to start fetch
- User can still interact with UI while fetch is in progress
- Manual fetch button remains available for re-fetch

**When Auto-Fetch Disabled:**
- Article opens immediately (current behavior)
- User must click "Fetch Full Article" button to trigger fetch
- Current behavior preserved exactly

## Localization Requirements

**String Keys to Add:**
```xml
<!-- Settings -->
<string name="setting_auto_fetch_full_article">Auto Fetch Full Article</string>
<string name="setting_auto_fetch_full_article_description">Automatically fetch full article text when opening articles</string>

<!-- SharedPreferences key (not localized) -->
pref_auto_fetch_full_article
```

**Existing Strings Used:**
- `R.string.fetch_full_article` - Button text (already exists)
- `R.string.fetching_full_article` - Loading text (already exists)
- `R.string.failed_to_fetch_full_article` - Error text (already exists)

## Edge Cases & Error Handling

### EC-001: Network Unavailable
**Scenario:** User opens article with no network connection
**Behavior:** Show error message, allow manual retry when connection available
**Priority:** Medium

### EC-002: Fetch Timeout
**Scenario:** Full text fetch takes too long
**Behavior:** Timeout after reasonable period (e.g., 30 seconds), show error
**Priority:** Medium

### EC-003: Article Already Has Full Text
**Scenario:** Article was previously fetched
**Behavior:** No fetch triggered, show cached content immediately
**Priority:** High

### EC-004: Rapid Article Navigation
**Scenario:** User quickly swipes through articles
**Behavior:** Cancel pending fetches for articles no longer viewed
**Priority:** Medium

## Dependencies & Integration Points

### Internal Dependencies
- `SettingsStore` - For storing/retrieving preference
- `FullTextParser` - For fetching full text content
- `ArticleViewModel` - For triggering auto-fetch
- `ArticleScreen` - For observing setting and triggering fetch

### External Dependencies
- None (uses existing network stack)

## Open Questions

### OQ-001: Settings Section Placement
**Question:** Should this be in "Syncing" section or create new "Reading" section?
**Recommendation:** Syncing section (with WiFi/Charging settings) as it involves network usage
**Status:** Pending stakeholder review

### OQ-002: Default Value
**Question:** Should default be ON or OFF?
**Recommendation:** OFF to preserve current behavior and avoid unexpected data usage
**Status:** Pending stakeholder review

## Success Metrics

- Feature usage rate: % of users who enable the setting
- Impact on data usage: Monitor avg data consumption per user
- User satisfaction: Collect feedback via reviews/settings
- Performance: Article open time p50, p95, p99

## Risks & Mitigations

### Risk-001: Increased Data Usage
**Impact:** Medium
**Mitigation:** Default OFF, clear setting label/description, honor WiFi-only setting
**Owner:** Product

### Risk-002: Battery Drain
**Impact:** Low
**Mitigation:** Respect "only when charging" setting, async fetch with timeout
**Owner:** Engineering

### Risk-003: Slower Article Loading
**Impact:** Low
**Mitigation:** Async fetch, show article immediately, fetch in background
**Owner:** Engineering

## Approval

| Role | Name | Approved | Date |
|------|------|----------|------|
| Product Owner | | | |
| Tech Lead | | | |
| UX Designer | | | |

---

**Document Version:** 1.0
**Last Updated:** 2026-01-01
**Next Review:** After stakeholder feedback
