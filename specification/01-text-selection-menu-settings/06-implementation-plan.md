# Implementation Plan: Text Selection Menu Settings

**Date:** 2025-11-27 22:30:00 PST
**Phase:** 6 - Specification Writing
**Status:** Draft

## Document Overview

This document provides the detailed implementation plan for the Text Selection Menu Settings feature. It breaks down the work into phases, milestones, and concrete tasks with clear dependencies and acceptance criteria.

**Related Documents:**
- [05-specification.md](./05-specification.md) - Complete technical specification
- [04-ui-ux-design.md](./04-ui-ux-design.md) - UI/UX design details

## Implementation Strategy

### Approach: Bottom-Up Implementation

We'll build from the data layer up to the UI layer, ensuring each layer is tested before proceeding to the next. This allows for early validation and reduces integration issues.

**Rationale:**
1. Data layer is simplest and can be tested independently
2. ViewModel depends on data layer
3. UI depends on ViewModel
4. Integration points can be added last

### Development Phases

```
Phase 1: Foundation (Data Layer)           [2-3 hours]
  ├─ Data models
  ├─ Repository implementation
  ├─ Text processor discovery
  └─ Unit tests

Phase 2: Business Logic (ViewModel)        [2-3 hours]
  ├─ ViewModel implementation
  ├─ State management
  ├─ Actions (move, toggle, reset)
  └─ Unit tests

Phase 3: User Interface (UI Layer)         [3-4 hours]
  ├─ Settings screen composable
  ├─ Drag-and-drop integration
  ├─ Reset dialog
  └─ UI tests

Phase 4: Integration                       [2-3 hours]
  ├─ Add dependency
  ├─ Navigation setup
  ├─ Settings screen link
  ├─ Text selection menu integration
  └─ End-to-end tests

Phase 5: Polish & Testing                  [2-3 hours]
  ├─ Accessibility testing
  ├─ Manual QA
  ├─ Bug fixes
  └─ Documentation

Total Estimated Time: 11-16 hours
```

## Phase 1: Foundation (Data Layer)

### Milestone 1.1: Data Models

**Objective**: Define Kotlin data classes for configuration and UI models.

**Files to Create:**
- `app/src/main/java/com/nononsenseapps/feeder/model/MenuConfiguration.kt`

**Tasks:**

1. **Create MenuConfiguration data class**
   - Add `version: Int` field
   - Add `items: List<MenuItemConfig>` field
   - Add `default()` companion function
   - Add JSON serialization annotations (if using kotlinx.serialization)

2. **Create MenuItemConfig data class**
   - Add `id: String` field
   - Add `order: Int` field
   - Add `enabled: Boolean` field
   - Add JSON serialization annotations

3. **Create MenuItemUiModel data class**
   - Add `id`, `title`, `subtitle`, `order`, `enabled` fields
   - Add `type: MenuItemType` enum field

4. **Create MenuItemType enum**
   - Add `BUILT_IN` value
   - Add `TEXT_PROCESSOR` value

5. **Create MenuConfigState data class**
   - Add `items: List<MenuItemUiModel>` field
   - Add `isLoading: Boolean` field
   - Add `showResetDialog: Boolean` field

**Acceptance Criteria:**
- [ ] All data classes compile without errors
- [ ] JSON serialization/deserialization works (manual test)
- [ ] Default configuration contains Copy and Select All
- [ ] Code follows existing project patterns

**Dependencies:** None

**Estimated Time:** 30 minutes

---

### Milestone 1.2: Repository Implementation

**Objective**: Implement data persistence using SharedPreferences.

**Files to Create:**
- `app/src/main/java/com/nononsenseapps/feeder/model/TextSelectionMenuConfigRepository.kt`

**Tasks:**

1. **Create TextSelectionMenuConfigRepository class**
   - Inject `SharedPreferences` dependency
   - Inject serialization dependency (Gson or kotlinx.serialization)

2. **Implement loadConfiguration()**
   - Load JSON string from SharedPreferences
   - Deserialize to MenuConfiguration
   - Return default on error
   - Add error logging

3. **Implement saveConfiguration()**
   - Serialize MenuConfiguration to JSON
   - Save to SharedPreferences using `edit { }`
   - Use IO dispatcher for blocking operations

4. **Implement getDefaultConfiguration()**
   - Return `MenuConfiguration.default()`

5. **Add constants**
   - Configuration key: `"text_selection_menu_config"`
   - Log tag: `"TextSelectionMenuConfigRepo"`

**Acceptance Criteria:**
- [ ] Repository compiles without errors
- [ ] Can save and load configuration
- [ ] Returns default when no configuration exists
- [ ] Returns default when JSON is invalid
- [ ] Uses IO dispatcher for blocking operations
- [ ] Follows existing repository patterns in app

**Dependencies:** Milestone 1.1 (Data Models)

**Estimated Time:** 45 minutes

---

### Milestone 1.3: Text Processor Discovery

**Objective**: Implement text processor discovery via PackageManager.

**Files to Modify:**
- `app/src/main/java/com/nononsenseapps/feeder/model/TextSelectionMenuConfigRepository.kt` (add methods)

**Tasks:**

1. **Add discoverTextProcessors() method**
   - Create `ACTION_PROCESS_TEXT` intent with `type = "text/plain"`
   - Query PackageManager with `MATCH_DEFAULT_ONLY`
   - Return list of `ResolveInfo`

2. **Add createConfigFromTextProcessor() method**
   - Extract package name from ResolveInfo
   - Create MenuItemConfig with given order
   - Set enabled = true by default

3. **Add mergeTextProcessors() method**
   - Combine existing config with newly discovered processors
   - Preserve existing order and enabled state
   - Add new processors at end with next available order
   - Avoid duplicates

4. **Add getTextProcessorLabel() method**
   - Query PackageManager for application label
   - Fall back to package name if query fails
   - Handle exceptions gracefully

**Acceptance Criteria:**
- [ ] Can discover text processors on device
- [ ] Can merge new processors with existing config
- [ ] Preserves existing configuration for known processors
- [ ] Handles missing or uninstalled processors gracefully
- [ ] Logs errors appropriately

**Dependencies:** Milestone 1.2 (Repository)

**Estimated Time:** 45 minutes

---

### Milestone 1.4: Data Layer Unit Tests

**Objective**: Write comprehensive unit tests for data layer.

**Files to Create:**
- `app/src/test/java/com/nononsenseapps/feeder/model/TextSelectionMenuConfigRepositoryTest.kt`

**Tasks:**

1. **Set up test infrastructure**
   - Create test SharedPreferences (use Robolectric or in-memory mock)
   - Create repository instance
   - Create test data fixtures

2. **Test loadConfiguration() - no existing config**
   - Verify returns default configuration
   - Verify default has Copy and Select All

3. **Test loadConfiguration() - existing config**
   - Save known configuration
   - Load configuration
   - Verify loaded matches saved

4. **Test saveConfiguration()**
   - Save configuration
   - Verify JSON written to SharedPreferences
   - Verify JSON is valid

5. **Test saveConfiguration() → loadConfiguration() round-trip**
   - Save configuration
   - Load configuration
   - Verify loaded matches saved exactly

6. **Test loadConfiguration() - invalid JSON**
   - Write invalid JSON to SharedPreferences
   - Load configuration
   - Verify returns default
   - Verify error logged

7. **Test mergeTextProcessors() - new processors**
   - Start with config of 2 items
   - Merge 2 new text processors
   - Verify 4 total items with correct order

8. **Test mergeTextProcessors() - existing processors**
   - Start with config including text processor
   - Merge same text processor again
   - Verify no duplicates

**Acceptance Criteria:**
- [ ] All tests pass
- [ ] Code coverage > 90% for repository
- [ ] Tests are deterministic (no flakiness)
- [ ] Tests follow existing test patterns in app

**Dependencies:** Milestones 1.1-1.3

**Estimated Time:** 1 hour

---

## Phase 2: Business Logic (ViewModel)

### Milestone 2.1: ViewModel Implementation

**Objective**: Implement ViewModel with state management and user actions.

**Files to Create:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsViewModel.kt`

**Tasks:**

1. **Create ViewModel class with Hilt**
   - Add `@HiltViewModel` annotation
   - Inject TextSelectionMenuConfigRepository
   - Inject Application context

2. **Set up state management**
   - Create `_uiState` as `MutableStateFlow<MenuConfigState>`
   - Expose `uiState` as `StateFlow<MenuConfigState>`
   - Create `currentConfig` private var

3. **Implement init block**
   - Call `loadConfiguration()`

4. **Implement loadConfiguration()**
   - Set loading state
   - Load config from repository
   - Discover text processors
   - Merge processors with config
   - Convert to UI models
   - Update UI state
   - Handle errors

5. **Implement moveItem(fromIndex, toIndex)**
   - Create mutable copy of items
   - Move item from → to
   - Update order fields (0-based)
   - Update UI state
   - Call persistCurrentState()

6. **Implement toggleItemEnabled(id)**
   - Map over items
   - Toggle enabled for matching id
   - Update UI state
   - Call persistCurrentState()

7. **Implement showResetDialog() / hideResetDialog()**
   - Update showResetDialog state

8. **Implement resetToDefaults()**
   - Get default configuration
   - Discover text processors
   - Merge processors
   - Save configuration
   - Update UI state
   - Hide dialog

9. **Implement persistCurrentState()**
   - Convert UI models to MenuItemConfig list
   - Create MenuConfiguration
   - Save via repository

10. **Implement configToUiModel()**
    - Map "copy" → Copy UI model
    - Map "select_all" → Select All UI model
    - Map package names → text processor UI models
    - Query PackageManager for labels
    - Handle errors gracefully

**Acceptance Criteria:**
- [ ] ViewModel compiles without errors
- [ ] All public methods work as specified
- [ ] State updates trigger UI recomposition
- [ ] Follows MVVM pattern used in app
- [ ] Follows existing ViewModel patterns (StateFlow, Hilt)

**Dependencies:** Phase 1 (Data Layer)

**Estimated Time:** 1.5 hours

---

### Milestone 2.2: ViewModel Unit Tests

**Objective**: Write comprehensive unit tests for ViewModel.

**Files to Create:**
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsViewModelTest.kt`

**Tasks:**

1. **Set up test infrastructure**
   - Create mock repository
   - Create test Application context
   - Create ViewModel instance
   - Create test data fixtures

2. **Test initialization**
   - Create ViewModel
   - Verify loadConfiguration() called
   - Verify UI state populated

3. **Test moveItem()**
   - Move item from index 0 to 2
   - Verify order updated correctly
   - Verify persistCurrentState() called

4. **Test toggleItemEnabled()**
   - Toggle item from enabled to disabled
   - Verify enabled state updated
   - Verify persistCurrentState() called

5. **Test showResetDialog() / hideResetDialog()**
   - Call showResetDialog()
   - Verify showResetDialog = true
   - Call hideResetDialog()
   - Verify showResetDialog = false

6. **Test resetToDefaults()**
   - Add custom configuration
   - Call resetToDefaults()
   - Verify configuration reverted to defaults
   - Verify dialog hidden

7. **Test text processor merging**
   - Mock text processor discovery
   - Verify new processors added to config
   - Verify existing processors preserved

8. **Test error handling**
   - Mock repository failure
   - Verify ViewModel doesn't crash
   - Verify error logged

**Acceptance Criteria:**
- [ ] All tests pass
- [ ] Code coverage > 90% for ViewModel
- [ ] Tests use coroutine test dispatcher
- [ ] Tests are deterministic
- [ ] Tests follow existing ViewModel test patterns

**Dependencies:** Milestone 2.1

**Estimated Time:** 1.5 hours

---

## Phase 3: User Interface (UI Layer)

### Milestone 3.1: Settings Screen UI

**Objective**: Create settings screen composable with drag-and-drop.

**Files to Create:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsScreen.kt`

**Tasks:**

1. **Create TextSelectionMenuSettingsScreen composable**
   - Add `onNavigateUp` parameter
   - Get ViewModel via `hiltViewModel()`
   - Collect UI state with `collectAsStateWithLifecycle()`
   - Create `Scaffold` with top app bar

2. **Implement SensibleTopAppBar**
   - Set title from string resource
   - Add back navigation button
   - Add reset action button (RestartAlt icon)

3. **Implement content area**
   - Show CircularProgressIndicator when loading
   - Show MenuItemList when loaded
   - Apply padding from scaffold

4. **Create MenuItemList composable**
   - Create `LazyListState` with `rememberLazyListState()`
   - Create `ReorderableLazyListState` with `rememberReorderableLazyListState()`
   - Set up onMove lambda to call viewModel.moveItem()
   - Add haptic feedback in onMove
   - Create LazyColumn with reorderable state
   - Set content padding and spacing

5. **Create MenuItemCard composable**
   - Wrap in ReorderableItem
   - Create Card with animated elevation
   - Use Row for layout
   - Add drag handle icon (left)
   - Add title and subtitle column (middle)
   - Add switch (right)
   - Add spacing between elements

6. **Implement drag handle**
   - Use `Icons.Rounded.DragHandle`
   - Apply `Modifier.draggableHandle()`
   - Add haptic feedback callbacks (onDragStarted, onDragStopped)
   - Set content description

7. **Implement title/subtitle**
   - Display title with bodyLarge typography
   - Display subtitle with bodySmall typography (if present)
   - Apply ellipsis for overflow
   - Set max lines = 1

8. **Implement enable/disable switch**
   - Bind to item.enabled
   - Call viewModel.toggleItemEnabled() on change

9. **Create ResetConfirmationDialog composable**
   - Create AlertDialog
   - Set title, message, buttons
   - Bind to showResetDialog state
   - Call viewModel.resetToDefaults() on confirm
   - Call viewModel.hideResetDialog() on dismiss

**Acceptance Criteria:**
- [ ] Screen compiles without errors
- [ ] Drag-and-drop works smoothly
- [ ] Haptic feedback works on drag events
- [ ] Switch toggles enabled state
- [ ] Reset dialog appears and functions correctly
- [ ] UI matches design in 04-ui-ux-design.md
- [ ] Follows existing Compose patterns in app

**Dependencies:** Phase 2 (ViewModel)

**Estimated Time:** 2.5 hours

---

### Milestone 3.2: String Resources

**Objective**: Add all user-facing strings to resources.

**Files to Modify:**
- `app/src/main/res/values/strings.xml`

**Tasks:**

1. **Add screen title**
   - `text_selection_menu_settings`: "Text Selection Menu Settings"

2. **Add item count string**
   - `text_selection_menu_items_configured`: "%d items configured"
   - Use plural resource for proper i18n

3. **Add drag handle description**
   - `drag_to_reorder`: "Drag to reorder"

4. **Add reset strings**
   - `reset_to_defaults`: "Reset to defaults"
   - `reset_text_selection_menu_confirmation`: Full confirmation message
   - `reset`: "Reset"

5. **Add built-in item labels**
   - `copy`: "Copy"
   - `select_all`: "Select All"

6. **Verify existing strings**
   - `cancel`: "Cancel" (should already exist)
   - `go_back`: "Go back" (should already exist)

**Acceptance Criteria:**
- [ ] All strings added to strings.xml
- [ ] No hardcoded strings in code
- [ ] Strings follow existing naming conventions
- [ ] Plural forms added where needed

**Dependencies:** None

**Estimated Time:** 15 minutes

---

### Milestone 3.3: UI Tests

**Objective**: Write UI tests for settings screen.

**Files to Create:**
- `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsScreenTest.kt`

**Tasks:**

1. **Set up test infrastructure**
   - Create test rule for Compose
   - Create mock ViewModel or use Hilt test
   - Create test data fixtures

2. **Test screen displays items**
   - Set up UI state with 3 items
   - Launch screen
   - Verify all items visible

3. **Test drag handle visibility**
   - Launch screen
   - Verify drag handle icon visible for each item

4. **Test switch displays enabled state**
   - Set up item with enabled = true
   - Launch screen
   - Verify switch is ON

5. **Test switch toggle**
   - Launch screen
   - Click switch
   - Verify toggleItemEnabled() called

6. **Test reset button shows dialog**
   - Launch screen
   - Click reset button
   - Verify dialog appears

7. **Test reset confirmation**
   - Show reset dialog
   - Click "Reset" button
   - Verify resetToDefaults() called

8. **Test reset cancellation**
   - Show reset dialog
   - Click "Cancel" button
   - Verify dialog dismissed
   - Verify resetToDefaults() NOT called

9. **Test back navigation**
   - Launch screen
   - Click back button
   - Verify onNavigateUp() called

10. **Test loading state**
    - Set isLoading = true
    - Launch screen
    - Verify CircularProgressIndicator visible

**Acceptance Criteria:**
- [ ] All tests pass
- [ ] Tests are deterministic
- [ ] Tests follow existing UI test patterns
- [ ] Code coverage > 80% for composables

**Dependencies:** Milestone 3.1

**Estimated Time:** 1 hour

---

## Phase 4: Integration

### Milestone 4.1: Add Dependency

**Objective**: Add reorderable library dependency.

**Files to Modify:**
- `app/build.gradle.kts`

**Tasks:**

1. **Add dependency to build.gradle.kts**
   - Add `implementation("sh.calvin.reorderable:reorderable:2.4.0")`
   - Place in appropriate section (alphabetically or grouped by category)

2. **Sync Gradle**
   - Run Gradle sync
   - Verify no conflicts
   - Verify library downloaded

3. **Verify import**
   - Add test import in a Kotlin file
   - Verify IDE recognizes library

**Acceptance Criteria:**
- [ ] Dependency added successfully
- [ ] Gradle sync completes without errors
- [ ] No version conflicts
- [ ] Library can be imported

**Dependencies:** None

**Estimated Time:** 5 minutes

---

### Milestone 4.2: Navigation Setup

**Objective**: Add navigation route for settings screen.

**Files to Modify:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/FeederNavGraph.kt`

**Tasks:**

1. **Find composable routes section**
   - Locate where other settings routes are defined

2. **Add textSelectionMenuSettings route**
   - Add `composable("textSelectionMenuSettings") { }`
   - Instantiate TextSelectionMenuSettingsScreen
   - Pass `onNavigateUp = { navController.navigateUp() }`

3. **Test navigation**
   - Manually navigate to route (if possible)
   - Verify screen loads

**Acceptance Criteria:**
- [ ] Route added to nav graph
- [ ] Screen accessible via navigation
- [ ] Back navigation works
- [ ] Follows existing navigation patterns

**Dependencies:** Milestone 3.1 (Settings Screen)

**Estimated Time:** 10 minutes

---

### Milestone 4.3: Settings Screen Link

**Objective**: Add link to Text Selection Menu Settings from main settings.

**Files to Modify:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsScreen.kt`

**Tasks:**

1. **Find appropriate section**
   - Locate "Text Settings" section
   - Identify insertion point (after "Text Settings", before "Synchronization")

2. **Add Spacer**
   - Add `Spacer(modifier = Modifier.height(16.dp))` for spacing

3. **Add section header**
   - Add "Text Selection Menu" header (or use existing pattern)

4. **Add ExternalSetting**
   - Set title = "Text Selection Menu Settings"
   - Set currentValue = "$menuItemCount items configured"
   - Set onClick to navigate to "textSelectionMenuSettings"
   - Get menuItemCount from ViewModel or repository

5. **Update ViewModel (if needed)**
   - Add state for menu item count
   - Load count when settings screen loads

**Acceptance Criteria:**
- [ ] Link appears in correct location
- [ ] Clicking link navigates to settings screen
- [ ] Item count displays correctly
- [ ] Follows existing settings screen patterns

**Dependencies:** Milestone 4.2 (Navigation)

**Estimated Time:** 20 minutes

---

### Milestone 4.4: Text Selection Menu Integration

**Objective**: Integrate configuration into text selection menu building.

**Files to Modify:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/SelectionMenu.kt`

**Tasks:**

1. **Identify menu building function**
   - Find function that builds text selection menu
   - Understand current implementation

2. **Inject repository**
   - Add TextSelectionMenuConfigRepository as dependency
   - Use Hilt or manual injection (match existing pattern)

3. **Load configuration**
   - Load configuration before building menu
   - Cache configuration (consider app-level singleton or ViewModel)

4. **Apply order to menu items**
   - Sort menu items by `order` field
   - Build menu in configured order

5. **Apply enabled state**
   - Set `menuItem.isEnabled` based on configuration
   - Gray out disabled items (existing implementation should handle this)

6. **Handle missing items**
   - If configuration doesn't include an item, add at end with enabled = true
   - Log warning for unexpected situation

**Acceptance Criteria:**
- [ ] Text selection menu respects order configuration
- [ ] Text selection menu respects enabled configuration
- [ ] Disabled items appear grayed out
- [ ] Changes in settings immediately affect menu (or after app restart)
- [ ] No crashes or errors

**Dependencies:** Phase 1 (Repository)

**Estimated Time:** 1 hour

---

### Milestone 4.5: End-to-End Tests

**Objective**: Write end-to-end tests for complete user flows.

**Files to Create:**
- `app/src/androidTest/java/com/nononsenseapps/feeder/e2e/TextSelectionMenuE2ETest.kt`

**Tasks:**

1. **Set up test infrastructure**
   - Create test rule for app
   - Create test data fixtures
   - Set up Hilt test

2. **Test: Reorder items end-to-end**
   - Open settings
   - Navigate to text selection menu settings
   - Drag item from position 0 to position 2
   - Navigate back
   - Open text in app
   - Select text
   - Verify menu items in new order

3. **Test: Disable item end-to-end**
   - Open settings
   - Navigate to text selection menu settings
   - Disable "Select All"
   - Navigate back
   - Open text in app
   - Select text
   - Verify "Select All" grayed out

4. **Test: Reset to defaults end-to-end**
   - Open settings
   - Navigate to text selection menu settings
   - Reorder items
   - Click reset
   - Confirm reset
   - Verify items back in default order

5. **Test: Configuration persists across restart**
   - Open settings
   - Configure menu
   - Restart app (or kill and reopen)
   - Open settings again
   - Verify configuration preserved

**Acceptance Criteria:**
- [ ] All E2E tests pass
- [ ] Tests cover critical user paths
- [ ] Tests are deterministic
- [ ] Tests follow existing E2E test patterns

**Dependencies:** Milestones 4.1-4.4

**Estimated Time:** 1.5 hours

---

## Phase 5: Polish & Testing

### Milestone 5.1: Accessibility Testing

**Objective**: Verify accessibility with TalkBack and keyboard.

**Tasks:**

1. **TalkBack testing**
   - Enable TalkBack on device
   - Navigate to settings screen
   - Verify all elements announced correctly
   - Verify drag handle announced as "Drag to reorder, button"
   - Test drag gestures with TalkBack
   - Verify position changes announced
   - Test switch toggle with TalkBack

2. **Keyboard testing (if device supports)**
   - Connect external keyboard
   - Navigate with Tab key
   - Verify focus order logical
   - Test drag with Space/Enter and arrow keys
   - Verify drag cancellation with Escape

3. **Content descriptions**
   - Verify all interactive elements have content descriptions
   - Verify descriptions are clear and helpful

4. **High contrast testing**
   - Enable high contrast mode
   - Verify all text readable
   - Verify visual distinction for all states

**Acceptance Criteria:**
- [ ] All TalkBack announcements correct
- [ ] Drag-and-drop works with TalkBack gestures
- [ ] Keyboard navigation works (if testable)
- [ ] High contrast mode works
- [ ] Meets WCAG 2.1 Level AA standards

**Dependencies:** Phase 3 (UI Layer)

**Estimated Time:** 1 hour

---

### Milestone 5.2: Manual QA

**Objective**: Comprehensive manual testing of all scenarios.

**Test Cases:**

Refer to [04-ui-ux-design.md Testing Checklist](./04-ui-ux-design.md#testing-checklist) for complete test case list.

**Key Scenarios:**

1. **Visual Tests** (10 test cases)
   - Screen rendering on phone and tablet
   - Drag handle visibility
   - Elevation during drag
   - Dark theme rendering

2. **Interaction Tests** (13 test cases)
   - Drag-and-drop mechanics
   - Switch toggle
   - Reset dialog flow
   - Edge cases (single item, 2 items)

3. **Haptic Feedback Tests** (4 test cases)
   - Feedback on drag start
   - Feedback during drag
   - Feedback on drag end

4. **Animation Tests** (5 test cases)
   - Elevation animation
   - Item repositioning animation
   - Smoothness (no jank)

**Acceptance Criteria:**
- [ ] All manual test cases pass
- [ ] No visual glitches
- [ ] No performance issues
- [ ] Haptic feedback works on supported devices

**Dependencies:** Phase 4 (Integration)

**Estimated Time:** 1.5 hours

---

### Milestone 5.3: Bug Fixes

**Objective**: Fix any issues discovered during testing.

**Process:**

1. **Triage bugs**
   - Critical: Crashes, data loss, broken core functionality
   - High: Major UX issues, accessibility problems
   - Medium: Minor visual issues, edge cases
   - Low: Polish, nice-to-haves

2. **Fix critical and high priority bugs**
   - Address crashes first
   - Fix accessibility issues
   - Fix core functionality issues

3. **Re-test after fixes**
   - Run affected tests
   - Verify fix doesn't introduce new issues

4. **Document remaining issues**
   - Create issues for medium/low priority bugs
   - Decide if they block release

**Acceptance Criteria:**
- [ ] All critical bugs fixed
- [ ] All high priority bugs fixed
- [ ] Medium/low bugs documented
- [ ] All tests pass after fixes

**Dependencies:** Milestones 5.1-5.2

**Estimated Time:** Variable (1-3 hours)

---

### Milestone 5.4: Documentation

**Objective**: Update documentation and create release notes.

**Tasks:**

1. **Code documentation**
   - Add KDoc to public APIs
   - Add inline comments for complex logic
   - Update existing docs if integration changes behavior

2. **README update (if applicable)**
   - Document new feature
   - Add screenshots
   - Update feature list

3. **Release notes**
   - Write user-facing release notes
   - Describe feature and benefits
   - Note any breaking changes (unlikely)

4. **Implementation summary**
   - Update `07-implementation-summary.md`
   - Document final implementation decisions
   - Note any deviations from spec

**Acceptance Criteria:**
- [ ] All public APIs documented
- [ ] Release notes written
- [ ] Implementation summary updated
- [ ] Documentation accurate and clear

**Dependencies:** Milestones 5.1-5.3

**Estimated Time:** 30 minutes

---

## Risk Management

### Identified Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Drag-and-drop library incompatibility with app | Low | High | Test early in Phase 3.1, have fallback plan (up/down arrows) |
| Text processor discovery performance issues | Medium | Medium | Implement caching, run on background thread |
| Configuration save failures | Low | Medium | Add retry logic, log errors, show user feedback |
| TalkBack drag gestures not working | Medium | Medium | Test early in Phase 5.1, work with reorderable library maintainer |
| Existing text selection menu code complex | Medium | Medium | Allocated extra time for Phase 4.4, may need refactoring |

### Mitigation Strategies

1. **Early testing of critical dependencies**
   - Test reorderable library integration early (Phase 3.1)
   - Test TalkBack compatibility early (Phase 5.1)

2. **Fallback plans**
   - If drag-and-drop doesn't work: Fall back to up/down arrows
   - If text processor discovery too slow: Lazy load on settings screen open

3. **Extra time buffer**
   - Built 20% time buffer into estimates
   - Phase 4.4 (integration) has extra time allocated

## Dependency Graph

```
Phase 1: Foundation
  ├─ 1.1 Data Models ─────────────┐
  ├─ 1.2 Repository ←─── 1.1 ─────┤
  ├─ 1.3 Text Processor Discovery ←─── 1.2 ─┐
  └─ 1.4 Unit Tests ←─── 1.1, 1.2, 1.3 ─────┤
                                              │
Phase 2: Business Logic                      │
  ├─ 2.1 ViewModel ←─── Phase 1 ─────────────┘
  └─ 2.2 Unit Tests ←─── 2.1

Phase 3: User Interface
  ├─ 3.1 Settings Screen ←─── Phase 2
  ├─ 3.2 String Resources (parallel)
  └─ 3.3 UI Tests ←─── 3.1

Phase 4: Integration
  ├─ 4.1 Add Dependency (parallel)
  ├─ 4.2 Navigation ←─── 3.1
  ├─ 4.3 Settings Link ←─── 4.2
  ├─ 4.4 Menu Integration ←─── Phase 1
  └─ 4.5 E2E Tests ←─── 4.1, 4.2, 4.3, 4.4

Phase 5: Polish
  ├─ 5.1 Accessibility Testing ←─── Phase 3
  ├─ 5.2 Manual QA ←─── Phase 4
  ├─ 5.3 Bug Fixes ←─── 5.1, 5.2
  └─ 5.4 Documentation ←─── 5.1, 5.2, 5.3
```

## Checkpoints & Commits

### Git Commit Strategy

**Commit after each milestone:**
- Milestone 1.1: "feat(model): add text selection menu configuration data models"
- Milestone 1.2: "feat(repo): implement text selection menu config repository"
- Milestone 1.3: "feat(repo): add text processor discovery"
- Milestone 1.4: "test(repo): add repository unit tests"
- Milestone 2.1: "feat(vm): implement text selection menu settings ViewModel"
- Milestone 2.2: "test(vm): add ViewModel unit tests"
- Milestone 3.1: "feat(ui): implement text selection menu settings screen"
- Milestone 3.2: "feat(res): add string resources for text selection menu settings"
- Milestone 3.3: "test(ui): add settings screen UI tests"
- Milestone 4.1: "deps: add sh.calvin.reorderable library"
- Milestone 4.2: "feat(nav): add text selection menu settings route"
- Milestone 4.3: "feat(ui): add link to text selection menu settings in main settings"
- Milestone 4.4: "feat(text): integrate configuration into text selection menu"
- Milestone 4.5: "test(e2e): add text selection menu E2E tests"
- Milestone 5.4: "docs: update documentation for text selection menu settings"

**Commit Message Format:**
Follow existing project conventions (appears to use conventional commits based on git history).

### Checkpoints

Create checkpoints (git stash or branch) at these points:
1. After Phase 1 complete (data layer working)
2. After Phase 2 complete (ViewModel working)
3. After Phase 3 complete (UI working)
4. After Phase 4 complete (fully integrated)
5. After Phase 5 complete (ready to ship)

## Success Criteria

### Definition of Done

Feature is complete when:
- [ ] All phases complete
- [ ] All unit tests pass (>90% coverage for data/ViewModel)
- [ ] All UI tests pass (>80% coverage)
- [ ] All E2E tests pass
- [ ] Manual QA complete (all scenarios pass)
- [ ] Accessibility verified (TalkBack, keyboard, high contrast)
- [ ] No critical or high priority bugs
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Release notes written

### Validation Checklist

Before marking feature complete:
- [ ] Drag-and-drop works smoothly (60 FPS)
- [ ] Haptic feedback works on all drag events
- [ ] Configuration persists across app restarts
- [ ] Text selection menu respects configuration
- [ ] Reset to defaults works
- [ ] TalkBack announcements correct
- [ ] No crashes or ANRs
- [ ] Performance acceptable (<100ms for all operations)
- [ ] Works on Android 7.0+ (minSdk 24)
- [ ] Works on phones and tablets
- [ ] Works in dark mode
- [ ] All strings localized

## Next Steps

After implementation plan approval:
1. **Review implementation plan** (Phase 7)
2. **Begin Phase 1** (Data Layer)
3. **Update task list** as work progresses
4. **Create implementation summary** document
5. **Track progress** and adjust estimates as needed

## Conclusion

This implementation plan provides a clear, structured approach to building the Text Selection Menu Settings feature. By following the bottom-up approach and thorough testing at each phase, we ensure a high-quality implementation that integrates seamlessly with the existing Feeder application.

**Key Success Factors:**
- ✅ Clear milestones with concrete acceptance criteria
- ✅ Comprehensive testing strategy at each phase
- ✅ Early validation of critical dependencies (drag-and-drop library)
- ✅ Time buffer for unexpected issues (20% contingency)
- ✅ Accessibility first-class concern (tested in dedicated phase)

**Total Estimated Time:** 11-16 hours over 3-5 days

Ready to proceed with implementation upon approval.
