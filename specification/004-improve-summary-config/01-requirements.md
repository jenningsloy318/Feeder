# Requirements: Improve AI Integration Summary Configuration

**Created:** 2026-01-01 19:12:10 +08:00
**Feature:** Improve AI Integration Summary Configuration
**Status:** Requirements Clarification
**Phase:** 2

---

## Executive Summary

Enhance the AI Integration settings to improve the user experience for configuring AI-generated summaries. The current "Summary Language" setting will be expanded into a comprehensive "Summary" configuration page with additional controls.

---

## Current State Analysis

### Existing Implementation

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Current UI:**
- "Summary Language" section item in AI Integration settings
- Clicking opens a dropdown menu to select language
- Stored in SharedPreferences via `SettingsStore.kt`
- Key: `PREF_SUMMARY_LANGUAGE` ("pref_summary_language")
- Data type: String (language code)
- Options: Auto-detect, English, Chinese, Spanish, French, German, Japanese, Korean, Portuguese, Russian, Arabic, Hindi

**Data Model:**
- Enum class `SummaryLanguage` in `app/src/main/java/com/nononsenseapps/feeder/ai/model/SummaryLanguage.kt`
- Properties:
  - `code`: ISO 639-1 language code (empty for auto-detect)
  - `displayName`: String resource ID
  - `languageName`: Human-readable name for prompts

**String Resources:**
- `summary_language_title`: "Summary Language"
- `summary_language_description`: "Choose the language for AI-generated summaries"
- `summary_language_auto_detect`: "Auto-detect"

**Navigation:**
- Settings screen navigates to Provider List screen
- Pattern: Settings → Provider List (similar pattern needed for Summary config)

---

## User Requirements

### Functional Requirements

#### FR-1: Rename "Summary Language" to "Summary"
**Priority:** High
**Description:** Change the UI label from "Summary Language" to "Summary"
**Rationale:** Broader term that encompasses multiple configuration options

**Acceptance Criteria:**
- [ ] String resource `summary_language_title` changed to "Summary"
- [ ] Screen title displays "Summary" on the dedicated configuration page
- [ ] Navigation item displays "Summary" in settings list

#### FR-2: Navigate to Dedicated Summary Configuration Page
**Priority:** High
**Description:** Clicking "Summary" should navigate to a new dedicated screen

**Acceptance Criteria:**
- [ ] Click on "Summary" item navigates to new screen
- [ ] New screen follows existing navigation patterns (similar to Provider List)
- [ ] Back button returns to AI Integration settings
- [ ] Navigation destination registered in `NavigationDestinations.kt`
- [ ] Route: `settings/summary`

#### FR-3: Add Enable/Disable Summary Toggle
**Priority:** High
**Description:** Add a toggle to enable or disable automatic summarization for all page reading

**Acceptance Criteria:**
- [ ] Toggle switch displayed at top of Summary configuration page
- [ ] Default state: Enabled (maintain current behavior)
- [ ] When disabled, AI summaries are not generated automatically
- [ ] When enabled, AI summaries are generated as before
- [ ] State persisted in SharedPreferences
- [ ] Key: `pref_summary_enabled` (boolean)
- [ ] String resources:
  - `summary_enabled_title`: "Enable Summaries"
  - `summary_enabled_description`: "Automatically generate AI summaries for articles"

#### FR-4: Language Selection on Dedicated Page
**Priority:** High
**Description:** Move language selection to the dedicated Summary configuration page

**Acceptance Criteria:**
- [ ] Language selector displayed below enable/disable toggle
- [ ] All existing language options preserved
- [ ] Current selection persisted as before
- [ ] No breaking changes to existing data storage

#### FR-5: Settings Screen Summary Preview
**Priority:** Medium
**Description:** Display brief summary status on the main settings screen

**Acceptance Criteria:**
- [ ] "Summary" item shows current language
- [ ] Shows indicator if summaries are disabled
- [ ] Example: "Summary: English (Enabled)" or "Summary: Disabled"

---

## Non-Functional Requirements

### NFR-1: Performance
- Navigation to Summary screen must complete within 100ms
- Toggle state changes must be instantaneous
- No blocking operations on main thread

### NFR-2: Data Persistence
- Use existing SharedPreferences mechanism
- Follow existing patterns in `SettingsStore.kt`
- Default: Enabled = true, Language = Auto-detect

### NFR-3: Backward Compatibility
- Existing users retain their current language setting
- No migration needed for existing data
- New enable/disable field defaults to enabled (no behavior change)

### NFR-4: UI/UX Consistency
- Follow Material Design 3 guidelines
- Match existing settings screen patterns
- Use project's theme system
- Consistent with Provider List navigation pattern

### NFR-5: Accessibility
- All UI elements properly labeled
- Toggle switch has content description
- Screen reader support for all interactive elements
- Minimum touch target: 48dp

---

## Technical Requirements

### TR-1: Navigation
- Create new navigation destination: `SummarySettingsDestination`
- Register in `NavigationDestinations.kt`
- Route: `settings/summary`
- Follow existing navigation pattern

### TR-2: ViewModel
- Create or extend ViewModel for Summary settings
- Manage state for:
  - Summary enabled (Boolean)
  - Summary language (SummaryLanguage)
- Expose StateFlow for reactive updates

### TR-3: UI Components
- Create new Composable screen: `SummarySettingsScreen`
- Follow existing patterns in `settings/` package
- Components needed:
  - Switch for enable/disable
  - Dropdown menu for language selection
  - Preview/status section

### TR-4: Data Storage
- Add to `SettingsStore.kt`:
  ```kotlin
  private val _summaryEnabled = MutableStateFlow(
      sp.getBoolean(PREF_SUMMARY_ENABLED, true)
  )
  val summaryEnabled = _summaryEnabled.asStateFlow()

  fun setSummaryEnabled(value: Boolean) {
      _summaryEnabled.value = value
      sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
  }
  ```
- Constant: `const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"`

### TR-5: String Resources
Add to `strings.xml`:
```xml
<!-- Main settings screen -->
<string name="summary_title">Summary</string>
<string name="summary_subtitle">Configure AI-generated summaries</string>
<string name="summary_status_enabled">%1$s (Enabled)</string>
<string name="summary_status_disabled">Disabled</string>

<!-- Summary configuration screen -->
<string name="summary_settings_title">Summary Settings</string>
<string name="summary_enabled_title">Enable Summaries</string>
<string name="summary_enabled_description">Automatically generate AI summaries for articles</string>
<string name="summary_language_title">Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>
```

### TR-6: Architecture
- **MVVM Pattern:** View → ViewModel → Model
- **State Management:** StateFlow for reactive updates
- **Single Source of Truth:** SettingsStore
- **Dependency Injection:** Use existing DI pattern

---

## User Stories

### US-1: As a user, I want to disable summaries
**Scenario:** User wants to read articles without AI summaries

**Given:** User is on Summary configuration screen
**When:** User toggles "Enable Summaries" to off
**Then:** Summaries are disabled and not displayed in articles

### US-2: As a user, I want to enable summaries in a specific language
**Scenario:** User prefers summaries in Chinese

**Given:** User is on Summary configuration screen
**When:** User enables summaries and selects Chinese
**Then:** Articles are summarized in Chinese

### US-3: As a user, I want to quickly access summary settings
**Scenario:** User wants to change summary configuration

**Given:** User is on AI Integration settings screen
**When:** User taps on "Summary"
**Then:** User is navigated to Summary configuration screen

---

## Edge Cases and Constraints

### EC-1: No Provider Configured
- If no AI provider is configured, show message in Summary screen
- Link to provider configuration

### EC-2: Migration from Old Version
- Existing users have language setting but no enabled flag
- Default enabled flag to true to maintain current behavior

### EC-3: Network Unavailable
- Summaries may fail to generate if AI provider is unreachable
- Show error message but don't change settings

### EC-4: Invalid Language Code
- Handle unknown language codes gracefully
- Default to Auto-detect

---

## Success Metrics

### User Experience
- [ ] Navigation flow is intuitive (follows existing patterns)
- [ ] Settings are discoverable
- [ ] Toggle state changes are immediate

### Technical
- [ ] No regressions in existing functionality
- [ ] All tests pass
- [ ] No memory leaks
- [ ] Smooth navigation (no jank)

### Code Quality
- [ ] Follows Kotlin coding conventions
- [ ] ktlint checks pass
- [ ] No TODO/FIXME comments
- [ ] Comprehensive test coverage

---

## Open Questions

### OQ-1: Trigger Mechanism
**Question:** How are summaries currently triggered?

**Investigation Needed:**
- [ ] Review code where summaries are generated
- [ ] Identify if summaries are auto-generated or on-demand
- [ ] Determine how enable/disable toggle affects this flow

**Answer Location:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` or similar

### OQ-2: Feed-Level Settings
**Question:** Should summary settings be configurable per feed?

**Decision:** NO - Out of scope for this feature
**Future Consideration:** May be added in future iteration

### OQ-3: Summary Display
**Question:** How are summaries currently displayed in the article reader?

**Investigation Needed:**
- [ ] Review ArticleScreen composable
- [ ] Identify summary display component
- [ ] Ensure disabled state hides summary properly

**Answer Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/`

---

## Dependencies and Integration Points

### Internal Dependencies
1. **SettingsStore.kt** - Data persistence layer
2. **NavigationDestinations.kt** - Navigation registration
3. **AIProviderSection.kt** - Parent settings screen (will be modified)
4. **SummaryLanguage.kt** - Existing language enum (no changes needed)

### External Dependencies
- AndroidX Navigation Component
- Jetpack Compose
- Material Design 3

### Affected Screens
1. **AI Integration Settings** - Add navigation handler
2. **New Summary Settings Screen** - Create new screen
3. **Article Reader Screen** - May need updates to respect enabled flag

---

## Implementation Phases

### Phase 1: Data Layer
- [ ] Add `PREF_SUMMARY_ENABLED` constant
- [ ] Add `summaryEnabled` StateFlow to SettingsStore
- [ ] Add `setSummaryEnabled()` method to SettingsStore

### Phase 2: Navigation
- [ ] Create `SummarySettingsDestination` in NavigationDestinations.kt
- [ ] Register navigation route
- [ ] Add navigation handler in AIProviderSection

### Phase 3: UI - Summary Settings Screen
- [ ] Create `SummarySettingsScreen` composable
- [ ] Create `SummarySettingsViewModel`
- [ ] Implement enable/disable toggle
- [ ] Implement language selector
- [ ] Add string resources

### Phase 4: UI - Main Settings Integration
- [ ] Update AIProviderSection to navigate to Summary screen
- [ ] Update summary preview on main settings
- [ ] Update string resources (rename "Summary Language" to "Summary")

### Phase 5: Integration and Testing
- [ ] Update article reader to respect enabled flag
- [ ] Write unit tests
- [ ] Write UI tests
- [ ] Manual testing

---

## Risk Assessment

### High Risk
- **Breaking existing functionality:** Mitigate by thorough testing
- **Data loss:** Mitigate by using safe defaults (enabled = true)

### Medium Risk
- **User confusion:** Mitigate by clear labels and descriptions
- **Navigation complexity:** Mitigate by following existing patterns

### Low Risk
- **Performance impact:** Minimal (simple boolean check)
- **Storage impact:** Minimal (one additional boolean preference)

---

## Approval Criteria

This feature is complete when:
1. [ ] All functional requirements are met
2. [ ] All non-functional requirements are satisfied
3. [ ] All tests pass (unit, integration, UI)
4. [ ] Code review approved
5. [ ] No regressions in existing functionality
6. [ ] Documentation updated

---

## References

- **Dev Rules:** `00-dev-rules.md`
- **Previous Implementation:** `specification/002-ai-summary-language-config/`
- **Multi-Provider Config:** `specification/003-multi-provider-ai-config/`
- **Code Files:**
  - `app/src/main/java/com/nononsenseapps/feeder/ai/model/SummaryLanguage.kt`
  - `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
  - `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`
  - `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

---

**Next Phase:** Phase 3 - Research
