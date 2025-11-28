# Task List: Text Selection Menu Settings

**Date:** 2025-11-27 22:35:00 PST
**Phase:** 6 - Specification Writing
**Status:** Not Started

## Document Overview

This document provides a comprehensive checklist of all tasks required to implement the Text Selection Menu Settings feature. Tasks are organized by phase and milestone, with clear status tracking.

**Related Documents:**
- [05-specification.md](./05-specification.md) - Technical specification
- [06-implementation-plan.md](./06-implementation-plan.md) - Detailed implementation plan

## Legend

- [ ] Not started
- [~] In progress
- [x] Completed
- [!] Blocked

## Phase 1: Foundation (Data Layer)

### Milestone 1.1: Data Models

- [ ] Create `MenuConfiguration.kt` file
- [ ] Create `MenuConfiguration` data class with version and items fields
- [ ] Add `MenuConfiguration.default()` companion function
- [ ] Add JSON serialization annotations to MenuConfiguration
- [ ] Create `MenuItemConfig` data class with id, order, enabled fields
- [ ] Add JSON serialization annotations to MenuItemConfig
- [ ] Create `MenuItemUiModel` data class with all required fields
- [ ] Create `MenuItemType` enum with BUILT_IN and TEXT_PROCESSOR values
- [ ] Create `MenuConfigState` data class for UI state
- [ ] Verify all classes compile without errors
- [ ] Test JSON serialization/deserialization manually
- [ ] Verify default configuration contains Copy and Select All

**Milestone Completion:** [ ]

---

### Milestone 1.2: Repository Implementation

- [ ] Create `TextSelectionMenuConfigRepository.kt` file
- [ ] Add SharedPreferences injection
- [ ] Add serialization dependency injection (Gson/kotlinx.serialization)
- [ ] Implement `loadConfiguration()` method
  - [ ] Load JSON string from SharedPreferences
  - [ ] Deserialize to MenuConfiguration
  - [ ] Handle deserialization errors with default config
  - [ ] Add error logging
- [ ] Implement `saveConfiguration()` method
  - [ ] Serialize MenuConfiguration to JSON
  - [ ] Save to SharedPreferences using edit { }
  - [ ] Use IO dispatcher for blocking operations
- [ ] Implement `getDefaultConfiguration()` method
- [ ] Add configuration key constant ("text_selection_menu_config")
- [ ] Add log tag constant ("TextSelectionMenuConfigRepo")
- [ ] Verify repository compiles without errors
- [ ] Manual test: Save and load configuration
- [ ] Manual test: Load when no configuration exists
- [ ] Manual test: Load with invalid JSON

**Milestone Completion:** [ ]

---

### Milestone 1.3: Text Processor Discovery

- [ ] Add `discoverTextProcessors(Context)` method to repository
  - [ ] Create ACTION_PROCESS_TEXT intent
  - [ ] Set intent type to "text/plain"
  - [ ] Query PackageManager with MATCH_DEFAULT_ONLY
  - [ ] Return List<ResolveInfo>
- [ ] Add `createConfigFromTextProcessor()` method
  - [ ] Extract package name from ResolveInfo
  - [ ] Create MenuItemConfig with provided order
  - [ ] Set enabled = true by default
- [ ] Add `mergeTextProcessors()` method
  - [ ] Identify existing vs new processors
  - [ ] Preserve order and enabled state for existing
  - [ ] Add new processors at end with next available order
  - [ ] Prevent duplicates
- [ ] Add `getTextProcessorLabel()` method
  - [ ] Query PackageManager for application label
  - [ ] Handle exceptions gracefully
  - [ ] Fall back to package name if query fails
- [ ] Add error logging for discovery failures
- [ ] Manual test: Discover text processors on device
- [ ] Manual test: Merge new processors with existing config
- [ ] Manual test: Handle uninstalled processors gracefully

**Milestone Completion:** [ ]

---

### Milestone 1.4: Data Layer Unit Tests

- [ ] Create `TextSelectionMenuConfigRepositoryTest.kt` file
- [ ] Set up test infrastructure
  - [ ] Create test SharedPreferences (mock or Robolectric)
  - [ ] Create repository instance
  - [ ] Create test data fixtures
- [ ] Test: loadConfiguration() with no existing config
  - [ ] Assert returns default configuration
  - [ ] Assert default has Copy and Select All
- [ ] Test: loadConfiguration() with existing config
  - [ ] Save known configuration
  - [ ] Load configuration
  - [ ] Assert loaded matches saved
- [ ] Test: saveConfiguration()
  - [ ] Save configuration
  - [ ] Assert JSON written to SharedPreferences
  - [ ] Assert JSON is valid
- [ ] Test: save/load round-trip
  - [ ] Save configuration
  - [ ] Load configuration
  - [ ] Assert loaded exactly matches saved
- [ ] Test: loadConfiguration() with invalid JSON
  - [ ] Write invalid JSON to SharedPreferences
  - [ ] Load configuration
  - [ ] Assert returns default
  - [ ] Assert error logged
- [ ] Test: mergeTextProcessors() with new processors
  - [ ] Start with 2-item config
  - [ ] Merge 2 new text processors
  - [ ] Assert 4 total items with correct order
- [ ] Test: mergeTextProcessors() with existing processors
  - [ ] Start with config including text processor
  - [ ] Merge same text processor again
  - [ ] Assert no duplicates
- [ ] Verify all tests pass
- [ ] Verify code coverage >90% for repository

**Milestone Completion:** [ ]

---

## Phase 2: Business Logic (ViewModel)

### Milestone 2.1: ViewModel Implementation

- [ ] Create `TextSelectionMenuSettingsViewModel.kt` file
- [ ] Add @HiltViewModel annotation
- [ ] Inject TextSelectionMenuConfigRepository
- [ ] Inject Application context
- [ ] Set up state management
  - [ ] Create _uiState MutableStateFlow
  - [ ] Expose uiState StateFlow
  - [ ] Create currentConfig private var
- [ ] Implement init block
  - [ ] Call loadConfiguration()
- [ ] Implement loadConfiguration()
  - [ ] Set loading state to true
  - [ ] Load config from repository
  - [ ] Discover text processors
  - [ ] Merge processors with config
  - [ ] Convert MenuItemConfig list to MenuItemUiModel list
  - [ ] Update UI state with items
  - [ ] Set loading state to false
  - [ ] Handle errors gracefully
- [ ] Implement moveItem(fromIndex, toIndex)
  - [ ] Create mutable copy of items list
  - [ ] Move item from source to destination index
  - [ ] Update order fields (0-based indexing)
  - [ ] Update UI state
  - [ ] Call persistCurrentState()
- [ ] Implement toggleItemEnabled(id)
  - [ ] Map over items list
  - [ ] Find item with matching id
  - [ ] Toggle enabled field
  - [ ] Update UI state
  - [ ] Call persistCurrentState()
- [ ] Implement showResetDialog()
  - [ ] Update showResetDialog state to true
- [ ] Implement hideResetDialog()
  - [ ] Update showResetDialog state to false
- [ ] Implement resetToDefaults()
  - [ ] Get default configuration from repository
  - [ ] Discover text processors
  - [ ] Merge processors with defaults
  - [ ] Save configuration via repository
  - [ ] Convert to UI models
  - [ ] Update UI state
  - [ ] Hide reset dialog
- [ ] Implement persistCurrentState()
  - [ ] Convert MenuItemUiModel list to MenuItemConfig list
  - [ ] Create MenuConfiguration
  - [ ] Save via repository (async)
- [ ] Implement configToUiModel(MenuItemConfig)
  - [ ] Handle "copy" → Copy UI model
  - [ ] Handle "select_all" → Select All UI model
  - [ ] Handle package names → text processor UI models
  - [ ] Query PackageManager for labels
  - [ ] Handle exceptions gracefully
- [ ] Verify ViewModel compiles without errors
- [ ] Verify all methods work as specified

**Milestone Completion:** [ ]

---

### Milestone 2.2: ViewModel Unit Tests

- [ ] Create `TextSelectionMenuSettingsViewModelTest.kt` file
- [ ] Set up test infrastructure
  - [ ] Create mock repository
  - [ ] Create test Application context
  - [ ] Create ViewModel instance
  - [ ] Create test data fixtures
  - [ ] Set up test coroutine dispatcher
- [ ] Test: initialization loads configuration
  - [ ] Create ViewModel
  - [ ] Assert loadConfiguration() called
  - [ ] Assert UI state populated with items
- [ ] Test: moveItem() updates order correctly
  - [ ] Move item from index 0 to 2
  - [ ] Assert order fields updated (0, 1, 2)
  - [ ] Assert persistCurrentState() called
- [ ] Test: toggleItemEnabled() updates state
  - [ ] Toggle item from enabled to disabled
  - [ ] Assert enabled state changed
  - [ ] Assert persistCurrentState() called
- [ ] Test: showResetDialog() / hideResetDialog()
  - [ ] Call showResetDialog()
  - [ ] Assert showResetDialog = true
  - [ ] Call hideResetDialog()
  - [ ] Assert showResetDialog = false
- [ ] Test: resetToDefaults() reverts configuration
  - [ ] Set custom configuration
  - [ ] Call resetToDefaults()
  - [ ] Assert configuration reverted to defaults
  - [ ] Assert dialog hidden
  - [ ] Assert repository.saveConfiguration() called with defaults
- [ ] Test: text processor merging
  - [ ] Mock text processor discovery returning 2 processors
  - [ ] Create ViewModel
  - [ ] Assert processors added to config
  - [ ] Assert existing processors preserved
- [ ] Test: error handling (repository failure)
  - [ ] Mock repository to throw exception
  - [ ] Create ViewModel
  - [ ] Assert ViewModel doesn't crash
  - [ ] Assert error logged
- [ ] Verify all tests pass
- [ ] Verify code coverage >90% for ViewModel

**Milestone Completion:** [ ]

---

## Phase 3: User Interface (UI Layer)

### Milestone 3.1: Settings Screen UI

- [ ] Create `TextSelectionMenuSettingsScreen.kt` file
- [ ] Create TextSelectionMenuSettingsScreen composable
  - [ ] Add onNavigateUp parameter
  - [ ] Get ViewModel via hiltViewModel()
  - [ ] Collect UI state with collectAsStateWithLifecycle()
  - [ ] Get haptic feedback with LocalHapticFeedback.current
- [ ] Implement Scaffold with top app bar
  - [ ] Add SensibleTopAppBar
  - [ ] Set title from string resource
  - [ ] Add back navigation IconButton
  - [ ] Add reset action IconButton (RestartAlt icon)
  - [ ] Set reset onClick to viewModel.showResetDialog()
- [ ] Implement content area
  - [ ] Show CircularProgressIndicator when isLoading = true
  - [ ] Show MenuItemList when isLoading = false
  - [ ] Apply padding from Scaffold
- [ ] Create MenuItemList composable
  - [ ] Add items, onMoveItem, onToggleEnabled, hapticFeedback parameters
  - [ ] Create LazyListState with rememberLazyListState()
  - [ ] Create ReorderableLazyListState with rememberReorderableLazyListState()
  - [ ] Set up onMove lambda to call onMoveItem(from.index, to.index)
  - [ ] Add haptic feedback in onMove (SegmentFrequentTick)
  - [ ] Create LazyColumn with state
  - [ ] Set contentPadding to 16.dp
  - [ ] Set verticalArrangement to spacedBy(8.dp)
  - [ ] Add items with key = item.id
  - [ ] Wrap each item in ReorderableItem
  - [ ] Pass isDragging to MenuItemCard
- [ ] Create MenuItemCard composable
  - [ ] Add item, isDragging, onToggleEnabled, hapticFeedback parameters
  - [ ] Animate elevation based on isDragging (1dp → 4dp)
  - [ ] Create MutableInteractionSource with remember
  - [ ] Create Card with animated elevation
  - [ ] Create Row layout inside Card
  - [ ] Set Row padding to 16.dp
  - [ ] Set horizontalArrangement to spacedBy(12.dp)
- [ ] Implement drag handle
  - [ ] Add Icon with Icons.Rounded.DragHandle
  - [ ] Apply Modifier.draggableHandle()
  - [ ] Set onDragStarted callback with GestureThresholdActivate haptic
  - [ ] Set onDragStopped callback with GestureEnd haptic
  - [ ] Pass interactionSource
  - [ ] Set contentDescription to "Drag to reorder"
  - [ ] Set tint to onSurfaceVariant
- [ ] Implement title/subtitle Column
  - [ ] Add Column with Modifier.weight(1f)
  - [ ] Add title Text with bodyLarge typography
  - [ ] Set title color to onSurface
  - [ ] Set title maxLines = 1
  - [ ] Set title overflow to Ellipsis
  - [ ] Add subtitle Text if item.subtitle != null
  - [ ] Set subtitle typography to bodySmall
  - [ ] Set subtitle color to onSurfaceVariant
  - [ ] Set subtitle maxLines = 1
  - [ ] Set subtitle overflow to Ellipsis
- [ ] Implement enable/disable Switch
  - [ ] Bind checked to item.enabled
  - [ ] Set onCheckedChange to call onToggleEnabled()
- [ ] Create ResetConfirmationDialog composable
  - [ ] Add onConfirm, onDismiss parameters
  - [ ] Create AlertDialog
  - [ ] Set onDismissRequest to onDismiss
  - [ ] Set title to "Reset to defaults"
  - [ ] Set text to confirmation message
  - [ ] Add confirm button (TextButton) calling onConfirm
  - [ ] Add dismiss button (TextButton) calling onDismiss
- [ ] Show ResetConfirmationDialog when showResetDialog = true
- [ ] Verify screen compiles without errors
- [ ] Verify UI matches design in 04-ui-ux-design.md

**Milestone Completion:** [ ]

---

### Milestone 3.2: String Resources

- [ ] Open `app/src/main/res/values/strings.xml`
- [ ] Add `text_selection_menu_settings` string
- [ ] Add `text_selection_menu_items_configured` string (with %d placeholder)
- [ ] Add `drag_to_reorder` string
- [ ] Add `reset_to_defaults` string
- [ ] Add `reset_text_selection_menu_confirmation` string (full message)
- [ ] Add `reset` string
- [ ] Add `copy` string
- [ ] Add `select_all` string
- [ ] Verify `cancel` string exists (or add it)
- [ ] Verify `go_back` string exists (or add it)
- [ ] Verify all strings follow naming conventions
- [ ] Consider adding plural form for items_configured if needed

**Milestone Completion:** [ ]

---

### Milestone 3.3: UI Tests

- [ ] Create `TextSelectionMenuSettingsScreenTest.kt` file
- [ ] Set up test infrastructure
  - [ ] Create Compose test rule
  - [ ] Create mock ViewModel or Hilt test setup
  - [ ] Create test data fixtures
- [ ] Test: screen displays list of items
  - [ ] Set UI state with 3 items
  - [ ] Launch TextSelectionMenuSettingsScreen
  - [ ] Assert all 3 items visible
- [ ] Test: drag handle visible on each item
  - [ ] Launch screen with items
  - [ ] Assert drag handle icon visible for each item
- [ ] Test: switch reflects enabled state
  - [ ] Set item with enabled = true
  - [ ] Launch screen
  - [ ] Assert switch is ON
- [ ] Test: switch toggle calls toggleItemEnabled
  - [ ] Launch screen
  - [ ] Click switch
  - [ ] Assert toggleItemEnabled() called with correct id
- [ ] Test: reset button shows dialog
  - [ ] Launch screen
  - [ ] Click reset button in app bar
  - [ ] Assert reset dialog visible
- [ ] Test: reset confirmation applies defaults
  - [ ] Show reset dialog
  - [ ] Click "Reset" button
  - [ ] Assert resetToDefaults() called
- [ ] Test: reset cancellation dismisses dialog
  - [ ] Show reset dialog
  - [ ] Click "Cancel" button
  - [ ] Assert dialog dismissed
  - [ ] Assert resetToDefaults() NOT called
- [ ] Test: back navigation
  - [ ] Launch screen
  - [ ] Click back button
  - [ ] Assert onNavigateUp() called
- [ ] Test: loading state shows progress indicator
  - [ ] Set isLoading = true
  - [ ] Launch screen
  - [ ] Assert CircularProgressIndicator visible
  - [ ] Assert menu items NOT visible
- [ ] Verify all tests pass
- [ ] Verify code coverage >80% for composables

**Milestone Completion:** [ ]

---

## Phase 4: Integration

### Milestone 4.1: Add Dependency

- [ ] Open `app/build.gradle.kts`
- [ ] Locate dependencies block
- [ ] Add `implementation("sh.calvin.reorderable:reorderable:2.4.0")`
- [ ] Place in appropriate section (alphabetically or by category)
- [ ] Run Gradle sync
- [ ] Verify no version conflicts
- [ ] Verify library downloaded successfully
- [ ] Test import of reorderable library in a Kotlin file
- [ ] Verify IDE recognizes library

**Milestone Completion:** [ ]

---

### Milestone 4.2: Navigation Setup

- [ ] Open `FeederNavGraph.kt`
- [ ] Locate composable routes section
- [ ] Add `composable("textSelectionMenuSettings") { }` route
- [ ] Instantiate TextSelectionMenuSettingsScreen inside route
- [ ] Pass `onNavigateUp = { navController.navigateUp() }`
- [ ] Verify route added correctly
- [ ] Test navigation to route (manually if possible)
- [ ] Verify back navigation works

**Milestone Completion:** [ ]

---

### Milestone 4.3: Settings Screen Link

- [ ] Open `SettingsScreen.kt`
- [ ] Locate "Text Settings" section
- [ ] Find insertion point (after Text Settings, before Synchronization)
- [ ] Add Spacer with height 16.dp
- [ ] Add section header for "Text Selection Menu" (if pattern requires)
- [ ] Add ExternalSetting composable
  - [ ] Set title to stringResource(R.string.text_selection_menu_settings)
  - [ ] Set currentValue to formatted string with item count
  - [ ] Set onClick to navigate to "textSelectionMenuSettings"
- [ ] Update ViewModel to provide menu item count (if needed)
- [ ] Verify link appears in correct location
- [ ] Verify clicking link navigates to settings screen
- [ ] Verify item count displays correctly

**Milestone Completion:** [ ]

---

### Milestone 4.4: Text Selection Menu Integration

- [ ] Open `SelectionMenu.kt`
- [ ] Locate function that builds text selection menu
- [ ] Understand current menu building implementation
- [ ] Inject TextSelectionMenuConfigRepository (via Hilt or manual injection)
- [ ] Load configuration before building menu
  - [ ] Consider caching configuration for performance
  - [ ] Handle async loading appropriately
- [ ] Modify menu building logic
  - [ ] Sort menu items by config.items order field
  - [ ] Set menuItem.isEnabled based on config enabled field
  - [ ] Apply visual styling for disabled items (gray out)
- [ ] Handle items not in configuration
  - [ ] Add at end with default order and enabled = true
  - [ ] Log warning for unexpected situation
- [ ] Test integration manually
  - [ ] Verify menu respects order configuration
  - [ ] Verify menu respects enabled configuration
  - [ ] Verify disabled items appear grayed out
- [ ] Test configuration changes take effect
  - [ ] Change configuration in settings
  - [ ] Verify menu updates (immediately or after restart)

**Milestone Completion:** [ ]

---

### Milestone 4.5: End-to-End Tests

- [ ] Create `TextSelectionMenuE2ETest.kt` file
- [ ] Set up test infrastructure
  - [ ] Create app test rule
  - [ ] Create Hilt test setup
  - [ ] Create test data fixtures
- [ ] Test: Reorder items end-to-end
  - [ ] Open settings
  - [ ] Navigate to text selection menu settings
  - [ ] Perform drag-and-drop (from index 0 to 2)
  - [ ] Navigate back to home
  - [ ] Open text content in app
  - [ ] Select text
  - [ ] Verify menu items in new order
- [ ] Test: Disable item end-to-end
  - [ ] Open settings
  - [ ] Navigate to text selection menu settings
  - [ ] Toggle "Select All" switch OFF
  - [ ] Navigate back
  - [ ] Open text and select
  - [ ] Verify "Select All" grayed out in menu
- [ ] Test: Reset to defaults end-to-end
  - [ ] Configure custom order and enable/disable states
  - [ ] Open text selection menu settings
  - [ ] Click reset button
  - [ ] Confirm reset in dialog
  - [ ] Verify items back in default order
  - [ ] Verify all items enabled
- [ ] Test: Configuration persists across restart
  - [ ] Configure menu (custom order + disabled item)
  - [ ] Kill and restart app
  - [ ] Open text selection menu settings
  - [ ] Verify configuration preserved exactly
- [ ] Verify all E2E tests pass
- [ ] Verify tests are deterministic

**Milestone Completion:** [ ]

---

## Phase 5: Polish & Testing

### Milestone 5.1: Accessibility Testing

- [ ] Enable TalkBack on test device
- [ ] Navigate to text selection menu settings
- [ ] Test: All elements announced correctly
  - [ ] Verify screen title announced
  - [ ] Verify each menu item announced with title and state
- [ ] Test: Drag handle announced as "Drag to reorder, button"
- [ ] Test: Drag gestures with TalkBack
  - [ ] Long-press drag handle
  - [ ] Perform TalkBack drag gesture
  - [ ] Verify drag works with TalkBack
- [ ] Test: Position changes announced during drag
  - [ ] Drag item to new position
  - [ ] Verify position changes announced ("moved to position X")
- [ ] Test: Switch toggle with TalkBack
  - [ ] Focus on switch
  - [ ] Activate switch with TalkBack gesture
  - [ ] Verify state change announced
- [ ] Test keyboard navigation (if device supports external keyboard)
  - [ ] Connect external keyboard
  - [ ] Navigate with Tab key
  - [ ] Verify focus order logical
  - [ ] Test drag with Space/Enter and arrow keys
  - [ ] Test drag cancellation with Escape
- [ ] Test: Content descriptions present
  - [ ] Verify all interactive elements have descriptions
  - [ ] Verify descriptions are clear and helpful
- [ ] Enable high contrast mode
- [ ] Test: High contrast mode
  - [ ] Verify all text readable
  - [ ] Verify sufficient color contrast
  - [ ] Verify visual distinction for all states

**Milestone Completion:** [ ]

---

### Milestone 5.2: Manual QA

**Visual Tests:**
- [ ] Cards display correctly on phone (portrait)
- [ ] Cards display correctly on phone (landscape)
- [ ] Cards display correctly on tablet
- [ ] Drag handle (≡) icon visible on all items
- [ ] Switch states render correctly (ON/OFF)
- [ ] Card elevation increases during drag (4dp shadow visible)
- [ ] Dialog appears centered and styled correctly
- [ ] Dark theme renders correctly
- [ ] High contrast mode works
- [ ] Dragged item appears elevated above others

**Interaction Tests:**
- [ ] Long-press drag handle initiates drag
- [ ] Item follows finger/pointer during drag smoothly
- [ ] Other items animate to make space during drag
- [ ] Item can be dragged up
- [ ] Item can be dragged down
- [ ] Releasing drag places item in new position
- [ ] List order persists after drag
- [ ] Switch toggles item enabled state
- [ ] Reset icon shows confirmation dialog
- [ ] Reset confirmation applies defaults
- [ ] Cancel button dismisses dialog without changes
- [ ] Back navigation works
- [ ] Can drag when only 2 items exist
- [ ] Single item shows drag handle but dragging has no effect

**Haptic Feedback Tests:**
- [ ] Haptic feedback on drag start (GestureThresholdActivate)
- [ ] Haptic feedback during drag on position change (SegmentFrequentTick)
- [ ] Haptic feedback on drag end (GestureEnd)
- [ ] No haptic feedback when drag canceled

**Animation Tests:**
- [ ] Elevation animates smoothly on drag start
- [ ] Elevation animates smoothly on drag end
- [ ] Other items slide smoothly to make space
- [ ] Spring animation feels natural (not too bouncy)
- [ ] No lag or jank during drag (60 FPS)

**Edge Cases:**
- [ ] No text processors installed - only shows Copy and Select All
- [ ] All items disabled - all switches OFF
- [ ] Single item - drag handle shown but no effect
- [ ] Text processor with long name - ellipsis works
- [ ] First-time user (no config) - shows defaults

**Configuration Persistence:**
- [ ] Configuration persists after app restart
- [ ] Configuration persists after configuration changes
- [ ] Invalid configuration falls back to defaults

**Performance:**
- [ ] Configuration load <50ms
- [ ] Text processor discovery <100ms
- [ ] Drag interaction 60 FPS (no jank)
- [ ] Configuration save <10ms

**Milestone Completion:** [ ]

---

### Milestone 5.3: Bug Fixes

- [ ] Triage all bugs found during testing
  - [ ] Critical: Crashes, data loss, broken core functionality
  - [ ] High: Major UX issues, accessibility problems
  - [ ] Medium: Minor visual issues, edge cases
  - [ ] Low: Polish, nice-to-haves
- [ ] Fix all critical bugs
  - [ ] Document bug
  - [ ] Identify root cause
  - [ ] Implement fix
  - [ ] Test fix
  - [ ] Verify no regression
- [ ] Fix all high priority bugs
  - [ ] Same process as critical
- [ ] Document remaining medium/low priority bugs
  - [ ] Create GitHub issues
  - [ ] Decide if they block release
- [ ] Re-run all tests after fixes
  - [ ] Unit tests
  - [ ] UI tests
  - [ ] E2E tests
  - [ ] Manual QA (affected scenarios)

**Milestone Completion:** [ ]

---

### Milestone 5.4: Documentation

- [ ] Add KDoc to all public APIs
  - [ ] MenuConfiguration
  - [ ] MenuItemConfig
  - [ ] MenuItemUiModel
  - [ ] TextSelectionMenuConfigRepository
  - [ ] TextSelectionMenuSettingsViewModel
- [ ] Add inline comments for complex logic
  - [ ] Text processor discovery
  - [ ] Configuration merging
  - [ ] Drag-and-drop setup
- [ ] Update README (if applicable)
  - [ ] Document new feature
  - [ ] Add screenshots
  - [ ] Update feature list
- [ ] Write release notes
  - [ ] User-facing description
  - [ ] Benefits of feature
  - [ ] Note any breaking changes (unlikely)
- [ ] Create implementation summary document
  - [ ] Document final implementation decisions
  - [ ] Note deviations from spec (if any)
  - [ ] Record lessons learned
  - [ ] Document known issues/limitations

**Milestone Completion:** [ ]

---

## Final Checklist

### Pre-Release Verification

- [ ] All phases complete
- [ ] All milestones complete
- [ ] All unit tests pass (>90% coverage)
- [ ] All UI tests pass (>80% coverage)
- [ ] All E2E tests pass
- [ ] All manual QA scenarios pass
- [ ] Accessibility verified (TalkBack, keyboard, high contrast)
- [ ] No critical or high priority bugs
- [ ] All medium/low bugs documented
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Release notes written

### Feature Validation

- [ ] Drag-and-drop works smoothly (60 FPS)
- [ ] Haptic feedback works on all drag events
- [ ] Configuration persists across app restarts
- [ ] Text selection menu respects configuration
- [ ] Reset to defaults works correctly
- [ ] TalkBack announcements correct
- [ ] No crashes or ANRs
- [ ] Performance acceptable (<100ms operations)
- [ ] Works on Android 7.0+ (minSdk 24)
- [ ] Works on phones and tablets
- [ ] Works in dark mode
- [ ] All strings localized (or ready for localization)

### Git & Deployment

- [ ] All commits follow conventional commit format
- [ ] All commits have meaningful messages
- [ ] All commits pass CI/CD (if applicable)
- [ ] Feature branch up-to-date with main
- [ ] No merge conflicts
- [ ] Pull request created
- [ ] Pull request description complete
- [ ] Pull request approved
- [ ] Pull request merged

## Progress Tracking

**Phase 1 (Foundation):** 0% (0/12 milestones)
**Phase 2 (Business Logic):** 0% (0/2 milestones)
**Phase 3 (UI Layer):** 0% (0/3 milestones)
**Phase 4 (Integration):** 0% (0/5 milestones)
**Phase 5 (Polish):** 0% (0/4 milestones)

**Overall Progress:** 0/26 milestones (0%)

---

## Notes

- Update progress percentages as milestones complete
- Mark tasks with [x] when completed
- Mark tasks with [~] when in progress
- Mark tasks with [!] when blocked
- Add notes for any deviations from plan
- Document blockers and their resolution

## Next Steps

1. Review specification and implementation plan
2. Get approval to proceed
3. Begin Phase 1 (Data Layer)
4. Update task list as work progresses
5. Create implementation summary document during development

---

**Ready to begin implementation upon approval.**
