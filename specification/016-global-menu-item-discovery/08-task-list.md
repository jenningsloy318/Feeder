# Task List: Global Menu Item Discovery and Display

**Version**: 2.0 (Updated for Moon+ Reader Pattern)
**Date**: 2026-01-04
**Total Tasks**: 52
**Estimated Hours**: 30-36 hours

---

## Key Changes from v1.0
- **Single flat list** (no sections)
- **Toggle switches** for visibility
- **Cross-section reordering** (no restrictions)
- **Simplified persistence** (flat order + visibility map)

---

## Task Status Legend
- [ ] Pending
- [x] Complete
- [~] In Progress
- [!] Blocked

---

## Phase 1: Data Model (Tasks 1-8)

### 1.1 Update SelectionMenuItem Data Class
- [ ] **Task 1.1**: Add MenuType enum to SelectionMenuItem.kt
  - Create enum with SYSTEM, APPLICATION, THIRD_PARTY
  - Add KDoc documentation
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 1.2**: Add `type: MenuType` field to SelectionMenuItem
  - Update constructor
  - Update @Immutable annotation
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 1.3**: Add `componentName: ComponentName?` field
  - Import android.content.ComponentName
  - Add to constructor
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 1.4**: Add `packageName: String?` field
  - Add to constructor
  - Document purpose
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 1.5**: Add `order: Int` field
  - Add to constructor with default value 0
  - Document usage
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 1.6**: Add `visible: Boolean` field (NEW for v2.0)
  - Add to constructor with default value true
  - Document purpose (user toggle)
  - **File**: `SelectionMenuItem.kt`
  - **Complexity**: Low
  - **Time**: 10min

### 1.2 Create MenuConfig Persistence Model
- [ ] **Task 1.7**: Create MenuConfig.kt data class (CHANGED for v2.0)
  - Add @Serializable annotation
  - Add `order: List<String>` (flat, not sectioned)
  - Add `visibility: Map<String, Boolean>`
  - Add default() companion method
  - Add isEmpty() method
  - Add isVisible() helper method
  - **File**: `MenuConfig.kt` (new)
  - **Complexity**: Medium
  - **Time**: 30min

### 1.3 Add Dependencies
- [ ] **Task 1.8**: Add serialization plugin to build.gradle.kts
  - Add kotlin("plugin.serialization") to plugins
  - Add kotlinx-serialization-json dependency
  - Add reorderable library dependency
  - **File**: `app/build.gradle.kts`
  - **Complexity**: Low
  - **Time**: 15min

---

## Phase 2: Service Layer (Tasks 9-15)

### 2.1 Create MenuDiscoveryService
- [ ] **Task 2.1**: Create MenuDiscoveryService.kt class
  - Add constructor with Context parameter
  - Add KDoc documentation
  - Initialize packageManager
  - **File**: `MenuDiscoveryService.kt` (new)
  - **Complexity**: Low
  - **Time**: 20min

- [ ] **Task 2.2**: Implement discoverAll() method
  - Add suspend function
  - Call all three discovery methods
  - Return flat list (no sections)
  - Use Dispatchers.Default
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 2.3**: Implement discoverSystemMenus()
  - Create 4 hardcoded items (copy, paste, cut, select_all)
  - Use android.R.string resources
  - Set type to SYSTEM
  - Set order values (0-3)
  - Set visible to true
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: Low
  - **Time**: 30min

- [ ] **Task 2.4**: Implement discoverFeederMenus()
  - Create 2 hardcoded items (read_aloud, translate)
  - Use R.string resources (to be added)
  - Set type to APPLICATION
  - Set order values (4-5)
  - Set visible to true
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: Low
  - **Time**: 30min

- [ ] **Task 2.5**: Implement discoverThirdPartyMenus()
  - Create ACTION_PROCESS_TEXT intent
  - Query packageManager
  - Handle SDK version (TIRAMISU vs older)
  - Sort with DisplayNameComparator
  - Map to SelectionMenuItem objects
  - Set type to THIRD_PARTY
  - Store ComponentName and packageName
  - Set order starting at 6
  - Set visible to true
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: High
  - **Time**: 1h

- [ ] **Task 2.6**: Add error handling to discovery
  - Wrap PackageManager calls in try-catch
  - Log errors
  - Return empty list on failure
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: Medium
  - **Time**: 20min

### 2.2 Register Service in DI
- [ ] **Task 2.7**: Add MenuDiscoveryService to DI module
  - Bind as singleton
  - Pass Context instance
  - **File**: DI configuration
  - **Complexity**: Low
  - **Time**: 10min

---

## Phase 3: ViewModel (Tasks 16-25)

### 3.1 Update SelectionMenuSettingsViewModel
- [ ] **Task 3.1**: Inject MenuDiscoveryService
  - Add private val by instance()
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Low
  - **Time**: 5min

- [ ] **Task 3.2**: Inject SharedPreferences
  - Add private val sp by instance()
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Low
  - **Time**: 5min

- [ ] **Task 3.3**: Implement loadMenus() method
  - Add viewModelScope.launch
  - Set loading state
  - Call menuDiscoveryService.discoverAll()
  - Handle success and error
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium
  - **Time**: 45min

- [ ] **Task 3.4**: Implement loadMenuConfig() method (CHANGED for v2.0)
  - Read from SharedPreferences
  - Parse JSON to MenuConfig (not MenuOrder)
  - Handle exceptions
  - Return default on error
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium
  - **Time**: 30min

- [ ] **Task 3.5**: Implement saveMenuConfig() method (CHANGED for v2.0)
  - Create MenuConfig from items list
  - Serialize to JSON
  - Write to SharedPreferences
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium
  - **Time**: 20min

- [ ] **Task 3.6**: Implement saveMenuConfigDebounced()
  - Cancel previous save job
  - Launch new coroutine with delay
  - Call saveMenuConfig
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium
  - **Time**: 20min

- [ ] **Task 3.7**: Implement mergeWithConfig() method (SIMPLIFIED for v2.0)
  - Create map of all items
  - Apply saved flat order
  - Apply saved visibility (default: true)
  - Append new items not in saved order
  - Update order property
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium (was High in v1.0)
  - **Time**: 1h (simplified from 1.5h)

- [ ] **Task 3.8**: Implement ReorderMenu event handler (SIMPLIFIED for v2.0)
  - Get current items
  - Remove from position
  - Insert at new position (no section checks)
  - Update order property
  - Update ViewState
  - Trigger debounced save
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Medium (was High in v1.0)
  - **Time**: 30min (simplified from 1h)

- [ ] **Task 3.9**: Implement ToggleItem event handler (NEW for v2.0)
  - Find item by ID
  - Toggle visible property
  - Update ViewState
  - Trigger debounced save
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Low
  - **Time**: 20min

- [ ] **Task 3.10**: Add ToggleItem to SelectionMenuEvent (NEW for v2.0)
  - Add data class ToggleItem(itemId: String)
  - Add to sealed class hierarchy
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 3.11**: Call loadMenus() in init block
  - Add init { loadMenus() }
  - **File**: `SelectionMenuSettingsViewModel.kt`
  - **Complexity**: Low
  - **Time**: 5min

---

## Phase 4: UI Implementation (Tasks 26-38)

### 4.1 Update Screen Composable
- [ ] **Task 4.1**: Replace Column with MenuList in SelectionMenuContent
  - Remove empty state when items exist
  - Call MenuList composable
  - Add onToggle callback
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 4.2**: Implement MenuList composable (SIMPLIFIED for v2.0)
  - Add rememberReorderableLazyListState
  - Implement onMove callback (no section check)
  - Add LazyColumn with reorderable state
  - Single flat list (no sections)
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low (was Medium in v1.0)
  - **Time**: 20min (simplified from 30min)

- [ ] **Task 4.3**: Add all items in single list (CHANGED for v2.0)
  - Use items viewState.items directly
  - No filtering by type
  - Use stable keys (id)
  - Wrap in ReorderableItem
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low (was 3 tasks in v1.0)
  - **Time**: 15min (was 45min for 3 tasks)

- [ ] **Task 4.4**: Implement MenuItemRow composable (UPDATED for v2.0)
  - Add Row layout
  - Add Switch on left (NEW)
  - Add icon (if available)
  - Add name and description text
  - Add drag handle icon on right
  - Apply isDragging modifier
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 45min

- [ ] **Task 4.5**: Add drag handle icon
  - Use Icons.Default.DragHandle
  - Add alpha modifier for disabled state
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 4.6**: Implement loading state
  - Show CircularProgressIndicator in SelectionMenuContent
  - Check viewState.isLoading
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 4.7**: Implement error state
  - Create ErrorState composable
  - Show error message
  - Add retry button
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 30min

- [ ] **Task 4.8**: Remove section headers (REMOVED for v2.0)
  - Delete SectionHeader composable if exists
  - No longer needed for flat list
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 5min

- [ ] **Task 4.9**: Add animations
  - Add animatedVisibility for states
  - Add elevation changes during drag
  - Add item swap animations
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 45min

- [ ] **Task 4.10**: Add toggle switch test tag (NEW for v2.0)
  - Add testTag = "toggle_switch" to Switch
  - For UI testing
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 5min

- [ ] **Task 4.11**: Implement accessibility for toggle (NEW for v2.0)
  - Add contentDescription to Switch
  - Announce state changes
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 30min

- [ ] **Task 4.12**: Implement background color change during drag
  - Use MaterialTheme.colorScheme.secondaryContainer
  - Apply when isDragging is true
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 4.13**: Implement alpha on drag handle
  - Add alpha(0.5f) modifier
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Low
  - **Time**: 5min

---

## Phase 5: Integration (Tasks 39-46)

### 5.1 String Resources
- [ ] **Task 5.1**: Add menu item name strings
  - selection_menu_read_aloud
  - selection_menu_translate
  - **File**: `res/values/strings.xml`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 5.2**: Add empty/error state strings
  - selection_menu_no_items
  - selection_menu_no_items_hint
  - **File**: `res/values/strings.xml`
  - **Complexity**: Low
  - **Time**: 10min

- [ ] **Task 5.3**: Add accessibility strings (UPDATED for v2.0)
  - selection_menu_drag_to_reorder
  - selection_menu_toggle_visibility (NEW)
  - selection_menu_visible (NEW)
  - selection_menu_hidden (NEW)
  - **File**: `res/values/strings.xml`
  - **Complexity**: Low
  - **Time**: 15min

- [ ] **Task 5.4**: Remove section header strings (REMOVED for v2.0)
  - Delete if exist from v1.0
  - **File**: `res/values/strings.xml`
  - **Complexity**: Low
  - **Time**: 5min

- [ ] **Task 5.5**: Add retry string
  - retry
  - **File**: `res/values/strings.xml`
  - **Complexity**: Low
  - **Time**: 5min

### 5.2 Accessibility
- [ ] **Task 5.6**: Add move up/down buttons (REMOVED for v2.0)
  - Not needed with toggle switches
  - Drag handle is sufficient
  - **N/A**: This task is removed

- [ ] **Task 5.7**: Add accessibility semantics (UPDATED for v2.0)
  - Add contentDescription to drag handle
  - Add contentDescription to toggle switch (NEW)
  - Add role to interactive elements
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 30min

- [ ] **Task 5.8**: Add keyboard navigation
  - Add focusable modifier
  - Handle arrow keys
  - Add focus indicators
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 45min

### 5.3 Optimization
- [ ] **Task 5.9**: Add discovery caching
  - Cache results for 5 seconds
  - Implement cache invalidation
  - **File**: `MenuDiscoveryService.kt`
  - **Complexity**: Medium
  - **Time**: 30min

- [ ] **Task 5.10**: Optimize list rendering
  - Ensure stable keys
  - Use derivedStateOf where needed
  - Minimize recomposition
  - **File**: `SelectionMenuSettingsScreen.kt`
  - **Complexity**: Medium
  - **Time**: 30min

---

## Phase 6: Testing (Tasks 47-52)

### 6.1 Unit Tests
- [ ] **Task 6.1**: Write MenuDiscoveryService tests
  - Test discoverSystemMenus()
  - Test discoverFeederMenus()
  - Test discoverThirdPartyMenus() (mock PackageManager)
  - Test error handling
  - Test all items visible by default (NEW)
  - **File**: `MenuDiscoveryServiceTest.kt`
  - **Complexity**: Medium
  - **Time**: 1h

- [ ] **Task 6.2**: Write ViewModel tests (UPDATED for v2.0)
  - Test loadMenus() updates state
  - Test reorderMenu() allows cross-section (NEW test)
  - Test toggleItem() changes visibility (NEW)
  - Test save/load persistence (both order and visibility)
  - Test merge logic
  - **File**: `SelectionMenuSettingsViewModelTest.kt`
  - **Complexity**: High
  - **Time**: 1.5h

### 6.2 UI Tests
- [ ] **Task 6.3**: Write UI tests (UPDATED for v2.0)
  - Test list displays items
  - Test toggle switch changes visibility (NEW)
  - Test drag-and-drop (if possible)
  - Test loading state
  - Test error state
  - **File**: `SelectionMenuSettingsScreenTest.kt`
  - **Complexity**: Medium
  - **Time**: 1h

### 6.3 Manual Testing
- [ ] **Task 6.4**: Manual testing checklist
  - Test on phone (portrait/landscape)
  - Test on tablet
  - Test on foldable
  - Test with no third-party apps
  - Test with many third-party apps
  - Test accessibility with TalkBack
  - Test keyboard navigation
  - Test persistence (restart app)
  - Test toggle switches work
  - Test cross-section reordering
  - **Complexity**: Low
  - **Time**: 1h

### 6.4 Bug Fixes & Polish
- [ ] **Task 6.5**: Fix reported bugs
  - Address unit test failures
  - Address UI test failures
  - Address manual testing issues
  - **Complexity**: Variable
  - **Time**: 2h

- [ ] **Task 6.6**: Final polish
  - Review all code
  - Update documentation
  - Verify all strings localized
  - Verify no warnings
  - Verify performance targets
  - **Complexity**: Low
  - **Time**: 1h

---

## Summary

**Total Tasks**: 52 (was 47 in v1.0)
**Estimated Time**: 30-36 hours (was 27-32 in v1.0)
**Critical Path**: Data Model → Service → ViewModel → UI → Testing

**Priority Order**:
1. **Must Have**: Tasks 1-25, 26-38 (Foundation + UI)
2. **Should Have**: Tasks 39-46 (Integration)
3. **Nice to Have**: Tasks 47-52 (Testing & Polish)

**Risk Tasks**:
- Task 2.5 (Third-party discovery) - High complexity
- Task 3.7 (Merge logic) - Medium complexity (simplified from High)
- Task 6.2 (ViewModel tests) - High complexity

**Dependencies**:
- Tasks 1-8 must complete before 9-15
- Tasks 9-15 must complete before 16-25
- Tasks 16-25 must complete before 26-38
- Tasks 26-38 must complete before 39-46
- All must complete before 47-52

**v2.0 Improvements**:
- Simpler data model (flat list vs sections)
- Simpler reordering (no section checks)
- Simplified merge logic
- Added toggle functionality
- More intuitive UX (Moon+ Reader pattern)

---

**Task List Complete**: 2026-01-04
**Version**: 2.0 (Moon+ Reader Pattern)
**Status**: Ready for Execution
**Next Phase**: Implementation
