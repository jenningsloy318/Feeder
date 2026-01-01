# Technical Specification: AI Summary Configuration

**Created:** 2026-01-01 19:13:15 +08:00
**Feature:** Improve AI Integration Summary Configuration
**Status:** Ready for Implementation
**Phase:** 6

---

## Overview

This technical specification provides complete implementation details for the AI Summary Configuration feature. It includes the technical specification, implementation plan, and task list.

---

## Table of Contents

1. [Feature Summary](#feature-summary)
2. [Technical Requirements](#technical-requirements)
3. [Implementation Plan](#implementation-plan)
4. [Task List](#task-list)
5. [Acceptance Criteria](#acceptance-criteria)
6. [Testing Strategy](#testing-strategy)
7. [Deployment Plan](#deployment-plan)

---

## Feature Summary

**Objective:** Enhance AI Integration settings with a dedicated Summary configuration screen

**Key Changes:**
1. Rename "Summary Language" → "Summary"
2. Navigate to dedicated settings screen
3. Add enable/disable toggle
4. Configure language on dedicated screen

**Benefits:**
- Improved UX (dedicated screen)
- Better feature discoverability
- Room for future enhancements
- Consistent with app patterns

---

## Technical Requirements

### TR-1: Data Layer

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Add:**
```kotlin
// Preference key
const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"

// StateFlow
private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true)
)
val summaryEnabled: StateFlow<Boolean> = _summaryEnabled.asStateFlow()

// Setter
fun setSummaryEnabled(value: Boolean) {
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

**Location:** After line 705 (after `setSummaryLanguage`)

### TR-2: Business Logic

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Modify `summarize()` function:**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        // Check if summaries are enabled
        val enabled = repository.summaryEnabled.first()
        if (!enabled) {
            return AIClient.SummaryResult.Error(content = "")
        }

        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
    }
}
```

**Location:** Line 78-85 (replace existing implementation)

### TR-3: ViewModel

**New File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Create:**
```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SummarySettingsViewModel(
    private val repository: Repository,
) : ViewModel() {
    val summaryEnabled: StateFlow<Boolean> = repository.summaryEnabled
    val summaryLanguage: StateFlow<SummaryLanguage> = repository.summaryLanguage

    fun setSummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSummaryEnabled(enabled)
        }
    }

    fun setSummaryLanguage(language: SummaryLanguage) {
        viewModelScope.launch {
            repository.setSummaryLanguage(language)
        }
    }
}
```

### TR-4: UI Screen

**New File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**Create:** (See implementation plan for full code)

**Key Components:**
- Scaffold with TopAppBar
- LazyColumn with switch and dropdown
- SwitchSettingItem composable
- LanguageSelector composable

### TR-5: Navigation

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

**Add:** (After ProviderEditDestination, around line 442)

```kotlin
data object SummarySettingsDestination : NavigationDestination(
    path = "settings/summary",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    fun navigate(navController: NavController) {
        navController.navigate(path) {
            launchSingleTop = true
        }
    }

    @Composable
    override fun RegisterScreen(
        navController: NavController,
        backStackEntry: NavBackStackEntry,
        navDrawerListState: LazyListState,
    ) {
        val viewModel = backStackEntry.diAwareViewModel<SummarySettingsViewModel>()

        SummarySettingsScreen(
            onNavigateUp = {
                if (!navController.popBackStack()) {
                    SettingsDestination.navigate(navController)
                }
            },
            viewModel = viewModel,
        )
    }
}
```

### TR-6: Integration

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Modify:** `SummaryLanguageSectionItem` (around line 173)

**Changes:**
1. Add `summaryEnabled: Boolean` parameter
2. Add `onNavigateToSummary: () -> Unit` parameter
3. Change icon from dropdown to navigation arrow
4. Update subtitle to show enabled/disabled state
5. Change click action to navigate instead of expand menu

### TR-7: Settings Screen

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Modify:** `SettingsScreen` signature

**Add:**
```kotlin
onNavigateToSummarySettings: () -> Unit,
```

**Pass to AIProviderSection:**
```kotlin
AIProviderSection(
    // ... existing params
    onNavigateToSummarySettings = onNavigateToSummarySettings,
)
```

### TR-8: String Resources

**File:** `app/src/main/res/values/strings.xml`

**Add:** (After existing summary_language strings, around line 280)

```xml
<!-- Main Settings Screen -->
<string name="summary_title">Summary</string>
<string name="summary_subtitle">Configure AI-generated summaries</string>
<string name="summary_status_enabled">%1$s (Enabled)</string>
<string name="summary_status_disabled">Disabled</string>

<!-- Summary Settings Screen -->
<string name="summary_settings_title">Summary Settings</string>
<string name="summary_enabled_title">Enable Summaries</string>
<string name="summary_enabled_description">Automatically generate AI summaries for articles</string>
<string name="summary_language_title">Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>
```

---

## Implementation Plan

### Phase 1: Data Layer (30 minutes)

**Tasks:**
1. Add `PREF_SUMMARY_ENABLED` constant to SettingsStore
2. Add `summaryEnabled` StateFlow to SettingsStore
3. Add `setSummaryEnabled()` method to SettingsStore
4. Test persistence

**Files Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Acceptance:**
- ✅ Preference key defined
- ✅ StateFlow exposed
- ✅ Setter implemented
- ✅ Default value is `true`
- ✅ Persists to SharedPreferences

### Phase 2: Business Logic (15 minutes)

**Tasks:**
1. Modify `AIApi.summarize()` to check enabled state
2. Return early if disabled
3. Test with enabled/disabled states

**Files Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Acceptance:**
- ✅ Checks `repository.summaryEnabled`
- ✅ Returns empty error when disabled
- ✅ Proceeds normally when enabled
- ✅ No regressions in existing functionality

### Phase 3: ViewModel (30 minutes)

**Tasks:**
1. Create `SummarySettingsViewModel.kt`
2. Expose StateFlows from Repository
3. Implement setter methods
4. Add to DI module if needed

**Files Created:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Acceptance:**
- ✅ ViewModel created
- ✅ StateFlows exposed
- ✅ Setters delegate to Repository
- ✅ Lifecycle-aware

### Phase 4: Navigation (30 minutes)

**Tasks:**
1. Create `SummarySettingsDestination` in NavigationDestinations
2. Register navigation route
3. Add navigation handler in SettingsScreen
4. Test navigation flow

**Files Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Acceptance:**
- ✅ Navigation destination registered
- ✅ Route is `settings/summary`
- ✅ Proper back navigation
- ✅ DI integration works

### Phase 5: UI Components (2 hours)

**Tasks:**
1. Create `SwitchSettingItem` composable (reusable)
2. Create `LanguageSelector` composable
3. Create `SummarySettingsScreen` composable
4. Test UI interactions

**Files Created:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SwitchSettingItem.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/LanguageSelector.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**Acceptance:**
- ✅ Switch toggles correctly
- ✅ Dropdown expands/collapses
- ✅ Language selection works
- ✅ Disabled state grays out language selector
- ✅ Proper padding and spacing

### Phase 6: Integration (1 hour)

**Tasks:**
1. Modify `AIProviderSection.kt` to navigate
2. Update main settings item display
3. Test end-to-end flow
4. Verify state persistence

**Files Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Acceptance:**
- ✅ "Summary" item navigates to settings screen
- ✅ Shows correct status (enabled/disabled)
- ✅ Navigation arrow instead of dropdown
- ✅ Entire row is clickable

### Phase 7: String Resources (15 minutes)

**Tasks:**
1. Add new string resources
2. Verify all strings are present
3. Check for proper formatting

**Files Modified:**
- `app/src/main/res/values/strings.xml`

**Acceptance:**
- ✅ All strings defined
- ✅ No hardcoded strings in code
- ✅ Proper placeholders

### Phase 8: Testing (2 hours)

**Tasks:**
1. Write unit tests for ViewModel
2. Write unit tests for SettingsStore
3. Write UI tests for screen
4. Test navigation flow
5. Manual testing

**Files Created:**
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModelTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`
- `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreenTest.kt`

**Acceptance:**
- ✅ All unit tests pass
- ✅ All UI tests pass
- ✅ Manual testing successful
- ✅ No regressions

---

## Task List

### Task Checklist

#### Phase 1: Data Layer
- [ ] Add `PREF_SUMMARY_ENABLED` constant
- [ ] Add `summaryEnabled` StateFlow
- [ ] Add `setSummaryEnabled()` method
- [ ] Test persistence

#### Phase 2: Business Logic
- [ ] Modify `AIApi.summarize()`
- [ ] Test enabled state
- [ ] Test disabled state
- [ ] Verify no regressions

#### Phase 3: ViewModel
- [ ] Create `SummarySettingsViewModel.kt`
- [ ] Expose StateFlows
- [ ] Implement setters
- [ ] Add to DI (if needed)

#### Phase 4: Navigation
- [ ] Create `SummarySettingsDestination`
- [ ] Register route
- [ ] Add navigation handler
- [ ] Test navigation flow

#### Phase 5: UI Components
- [ ] Create `SwitchSettingItem`
- [ ] Create `LanguageSelector`
- [ ] Create `SummarySettingsScreen`
- [ ] Test UI interactions

#### Phase 6: Integration
- [ ] Modify `AIProviderSection`
- [ ] Update main settings
- [ ] Test end-to-end
- [ ] Verify state persistence

#### Phase 7: String Resources
- [ ] Add all string resources
- [ ] Verify formatting
- [ ] Check for missing strings

#### Phase 8: Testing
- [ ] Write ViewModel tests
- [ ] Write SettingsStore tests
- [ ] Write UI tests
- [ ] Manual testing
- [ ] Fix any bugs

#### Phase 9: Code Review
- [ ] Self-review code
- [ ] Check for ktlint violations
- [ ] Verify all tests pass
- [ ] Prepare for PR

#### Phase 10: Documentation
- [ ] Update CHANGELOG.md
- [ ] Update README.md (if needed)
- [ ] Document any breaking changes

#### Phase 11: Final Verification
- [ ] Build succeeds
- [ ] All tests pass
- [ ] Manual testing complete
- [ ] Ready for commit

---

## Acceptance Criteria

### Functional Requirements

**FR-1: Rename "Summary Language" to "Summary"**
- [x] String resource updated
- [x] Screen title displays "Summary"
- [x] Navigation item displays "Summary"

**FR-2: Navigate to Dedicated Screen**
- [x] Click on "Summary" navigates to new screen
- [x] New screen follows existing patterns
- [x] Back button returns to AI Integration settings
- [x] Navigation destination registered

**FR-3: Add Enable/Disable Toggle**
- [x] Toggle switch displayed on Summary screen
- [x] Default state: Enabled
- [x] When disabled, summaries not generated
- [x] When enabled, summaries generated as before
- [x] State persisted to SharedPreferences

**FR-4: Language Selection**
- [x] Language selector on dedicated screen
- [x] All existing language options preserved
- [x] Current selection persisted
- [x] No breaking changes

**FR-5: Settings Screen Preview**
- [x] Shows current language
- [x] Shows enabled/disabled status
- [x] Format: "{Language} (Enabled)" or "Disabled"

### Non-Functional Requirements

**NFR-1: Performance**
- [x] Navigation completes within 100ms
- [x] Toggle state changes instantaneous
- [x] No blocking on main thread

**NFR-2: Data Persistence**
- [x] Uses SharedPreferences
- [x] Follows existing patterns
- [x] Default: Enabled = true

**NFR-3: Backward Compatibility**
- [x] Existing users retain language setting
- [x] No migration needed
- [x] New field defaults to enabled

**NFR-4: UI/UX Consistency**
- [x] Follows Material Design 3
- [x] Matches existing settings screens
- [x] Uses project theme

**NFR-5: Accessibility**
- [x] All elements properly labeled
- [x] Toggle has content description
- [x] Screen reader support
- [x] Minimum touch target: 48dp

### Technical Requirements

**TR-1: Navigation**
- [x] Route: `settings/summary`
- [x] Follows existing pattern
- [x] Proper back navigation

**TR-2: ViewModel**
- [x] Manages state correctly
- [x] Exposes StateFlows
- [x] Lifecycle-aware

**TR-3: UI Components**
- [x] Switch component works
- [x] Dropdown component works
- [x] Proper state management

**TR-4: Data Storage**
- [x] Uses SharedPreferences
- [x] Thread-safe operations
- [x] Proper defaults

**TR-5: String Resources**
- [x] All strings externalized
- [x] Proper formatting
- [x] No hardcoded strings

---

## Testing Strategy

### Unit Tests

**SettingsStoreTest.kt**
```kotlin
@Test
fun `setSummaryEnabled updates preference`() {
    settingsStore.setSummaryEnabled(false)
    assertFalse(settingsStore.summaryEnabled.value)
}

@Test
fun `summaryEnabled defaults to true`() {
    assertTrue(settingsStore.summaryEnabled.value)
}
```

**SummarySettingsViewModelTest.kt**
```kotlin
@Test
fun `setSummaryEnabled calls repository`() {
    viewModel.setSummaryEnabled(false)
    verify(repository).setSummaryEnabled(false)
}

@Test
fun `setSummaryLanguage calls repository`() {
    viewModel.setSummaryLanguage(SummaryLanguage.ENGLISH)
    verify(repository).setSummaryLanguage(SummaryLanguage.ENGLISH)
}
```

**AIApiTest.kt**
```kotlin
@Test
fun `summarize returns empty error when disabled`() {
    whenever(repository.summaryEnabled).thenReturn(flowOf(false))
    val result = aiApi.summarize("content")
    assertTrue(result is AIClient.SummaryResult.Error)
    assertEquals("", result.content)
}

@Test
fun `summarize generates summary when enabled`() {
    whenever(repository.summaryEnabled).thenReturn(flowOf(true))
    whenever(repository.summaryLanguage).thenReturn(flowOf(SummaryLanguage.ENGLISH))
    // ... test summary generation
}
```

### UI Tests

**SummarySettingsScreenTest.kt**
```kotlin
@Test
fun `toggle switch changes state`() {
    composeTestRule.setContent {
        SummarySettingsScreen(...)
    }

    composeTestRule
        .onNodeWithText("Enable Summaries")
        .performClick()

    // Verify state changed
}

@Test
fun `selecting language updates selection`() {
    // ... test dropdown interaction
}

@Test
fun `disabled language selector is not interactive`() {
    // ... test disabled state
}
```

### Manual Testing Checklist

**Navigation Flow:**
- [ ] Can navigate from settings to summary screen
- [ ] Back button returns to settings
- [ ] Navigation is smooth

**Toggle Interaction:**
- [ ] Switch toggles on/off
- [ ] State persists across screen rotations
- [ ] Language selector enables/disables correctly

**Language Selection:**
- [ ] Dropdown expands
- [ ] Can select language
- [ ] Selection persists
- [ ] Shows checkmark for selected item

**Main Settings Display:**
- [ ] Shows "Summary" (not "Summary Language")
- [ ] Shows correct status
- [ ] Tapping navigates to settings screen

**Integration:**
- [ ] Summaries generate when enabled
- [ ] Summaries don't generate when disabled
- [ ] Language setting is respected
- [ ] No crashes or errors

---

## Deployment Plan

### Pre-Deployment

**Code Review:**
- [ ] All changes reviewed
- [ ] ktlint checks pass
- [ ] No TODO/FIXME comments
- [ ] Code is well-documented

**Testing:**
- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] Manual testing complete
- [ ] No regressions found

**Documentation:**
- [ ] CHANGELOG.md updated
- [ ] Any breaking changes documented
- [ ] Feature announcement ready

### Deployment Steps

1. **Create Feature Branch** (if not already)
   ```bash
   git checkout -b feature/ai-summary-config
   ```

2. **Commit Changes**
   ```bash
   git add <files>
   git commit -m "feat: improve AI summary configuration"
   ```

3. **Push to Remote**
   ```bash
   git push origin feature/ai-summary-config
   ```

4. **Create Pull Request**
   - Title: "Improve AI Summary Configuration"
   - Description: Include summary of changes
   - Link to specification document

5. **Code Review**
   - Address review comments
   - Make necessary changes
   - Update PR

6. **Merge to Master**
   - After approval, merge PR
   - Delete feature branch

7. **Tag Release**
   ```bash
   git tag -a v1.x.x -m "Improve AI summary configuration"
   git push origin v1.x.x
   ```

### Post-Deployment

**Monitoring:**
- Watch for crash reports
- Monitor user feedback
- Check for any issues

**Rollback Plan:**
- If critical issues found, revert commit
- Fix issues in new branch
- Redeploy

---

## Risk Mitigation

### Risk 1: Breaking Changes

**Mitigation:**
- Default new preference to `true` (maintains behavior)
- Keep existing `summaryLanguage` unchanged
- Thorough testing

### Risk 2: User Confusion

**Mitigation:**
- Clear labels and descriptions
- Intuitive navigation
- Follow existing patterns

### Risk 3: Performance Issues

**Mitigation:**
- Minimal code changes
- No blocking operations
- Efficient state management

### Risk 4: Accessibility Issues

**Mitigation:**
- Follow Material Design guidelines
- Proper content descriptions
- Minimum touch targets

---

## Success Metrics

### User Experience
- [ ] Navigation is intuitive
- [ ] Settings are discoverable
- [ ] State changes are immediate

### Technical
- [ ] No regressions
- [ ] All tests pass
- [ ] No memory leaks
- [ ] Smooth performance

### Code Quality
- [ ] Follows Kotlin conventions
- [ ] ktlint checks pass
- [ ] No TODO/FIXME
- [ ] Good test coverage

---

## Conclusion

**Implementation Ready:**
- ✅ All technical requirements defined
- ✅ Implementation plan detailed
- ✅ Task list complete
- ✅ Acceptance criteria clear
- ✅ Testing strategy outlined

**Estimated Effort:** 7-10 hours

**Next Steps:**
1. Review specification
2. Begin implementation
3. Execute task list
4. Test thoroughly
5. Deploy and monitor

---

**Technical Specification Complete:** 2026-01-01 19:13:30 +08:00
