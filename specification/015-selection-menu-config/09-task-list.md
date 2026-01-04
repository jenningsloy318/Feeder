# Task List: Selection Menu Configuration Feature

**Feature ID**: 015
**Version**: 1.0.0
**Last Updated**: 2026-01-04
**Total Tasks**: 22
**Estimated Time**: 17-24 hours

---

## Task Legend

- [ ] **Not Started**
- [x] **Complete**
- [~] **In Progress**
- [!] **Blocked**

---

## Phase 1: Data Layer (4-5 hours)

### Task 1.1: Create Data Models
- [ ] **T1.1.1**: Create `SelectionMenuItem` data class with kotlinx.serialization annotations
- [ ] **T1.1.2**: Create `ActionType` enum (TRANSLATE, COPY, SHARE, OPEN_BROWSER, CUSTOM)
- [ ] **T1.1.3**: Create `TranslatorApp`, `CopyApp`, `ShareTarget` data classes
- [ ] **T1.1.4**: Add nullable fields for third-party package name and class name
- [ ] **T1.1.5**: Verify data models compile successfully

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: None
**Acceptance Criteria**:
- Data models in `com/nononsenseapps/feeder/ui/text/SelectionMenuModels.kt`
- All models have kotlinx.serialization annotations
- Nullable fields have default values

---

### Task 1.2: Implement JSON Serializer
- [ ] **T1.2.1**: Create `SelectionMenuItemSerializer` object
- [ ] **T1.2.2**: Implement `serialize()` method using kotlinx.serialization
- [ ] **T1.2.3**: Implement `deserialize()` method with error handling
- [ ] **T1.2.4**: Add JSON configuration (ignoreKeys, coerceInputValues)
- [ ] **T1.2.5**: Test serialization/deserialization with sample data

**Estimated Time**: 1 hour
**Priority**: High
**Dependencies**: T1.1 (Data Models)
**Acceptance Criteria**:
- Serializer in `com/nononsenseapps/feeder/ui/text/SelectionMenuItemSerializer.kt`
- Can serialize list of items to JSON
- Can deserialize valid JSON to list of items
- Throws exception for malformed JSON

---

### Task 1.3: Create SettingsStore Extension
- [ ] **T1.3.1**: Create extension property `Context.selectionMenuConfigStore`
- [ ] **T1.3.2**: Define `defaultSelectionMenuConfig` with 4 built-in actions
- [ ] **T1.3.3**: Implement custom serializer for SettingsStore
- [ ] **T1.3.4**: Add migration path from hardcoded order (if needed)
- [ ] **T1.3.5**: Verify SharedPreferences key and default value

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: T1.2 (Serializer)
**Acceptance Criteria**:
- SettingsStore in `com/nononsenseapps/feeder/util/SelectionMenuConfigStore.kt`
- Default configuration has 4 items in order 0-3
- All items enabled by default
- SharedPreferences key is "selection_menu_config"

---

### Task 1.4: Add Gradle Dependencies
- [ ] **T1.4.1**: Apply kotlinx-serialization plugin in `build.gradle.kts`
- [ ] **T1.4.2**: Add `kotlinx-serialization-json` dependency
- [ ] **T1.4.3**: Sync Gradle and verify no errors
- [ ] **T1.4.4**: Verify plugin version compatibility with Kotlin version

**Estimated Time**: 0.5 hours
**Priority**: High
**Dependencies**: None
**Acceptance Criteria**:
- Plugin applied in `build.gradle.kts` (app module)
- Dependency version 1.6.0 or later
- Gradle sync succeeds without errors

---

## Phase 2: Repository Layer (3-4 hours)

### Task 2.1: Create Repository Interface
- [ ] **T2.1.1**: Create `SelectionMenuConfigRepository` interface
- [ ] **T2.1.2**: Define `observeConfig(): Flow<List<SelectionMenuItem>>`
- [ ] **T2.1.3**: Define `updateOrder(items: List<SelectionMenuItem>)`
- [ ] **T2.1.4**: Define `updateEnabled(itemId: String, enabled: Boolean)`
- [ ] **T2.1.5**: Define `resetToDefaults()` method

**Estimated Time**: 0.5 hours
**Priority**: High
**Dependencies**: T1.3 (SettingsStore)
**Acceptance Criteria**:
- Interface in `com/nononsenseapps/feeder/ui/text/SelectionMenuConfigRepository.kt`
- All methods have correct signatures
- Flow used for reactive updates

---

### Task 2.2: Implement Third-Party App Discovery
- [ ] **T2.2.1**: Create `ThirdPartyAppRepository` interface
- [ ] **T2.2.2**: Implement `discoverTranslators()` using PackageManager
- [ ] **T2.2.3**: Implement `discoverCopyApps()` using PackageManager
- [ ] **T2.2.4**: Implement `discoverShareTargets()` using PackageManager
- [ ] **T2.2.5**: Add caching with 5-minute TTL
- [ ] **T2.2.6**: Handle PackageManager exceptions gracefully

**Estimated Time**: 1.5 hours
**Priority**: Medium
**Dependencies**: None
**Acceptance Criteria**:
- Repository in `com/nononsenseapps/feeder/ui/text/ThirdPartyAppRepository.kt`
- Can discover translator apps (ACTION_PROCESS_TEXT)
- Can discover copy apps (clipboard listeners)
- Can discover share targets (share handlers)
- Results cached for 5 minutes
- Handles PackageManager exceptions

---

### Task 2.3: Implement Repository
- [ ] **T2.3.1**: Create `SelectionMenuConfigRepositoryImpl` class
- [ ] **T2.3.2**: Inject `Context` and `ThirdPartyAppRepository`
- [ ] **T2.3.3**: Implement `observeConfig()` with merge logic
- [ ] **T2.3.4**: Implement `updateOrder()` to persist changes
- [ ] **T2.3.5**: Implement `updateEnabled()` to toggle items
- [ ] **T2.3.6**: Implement `resetToDefaults()` to restore defaults
- [ ] **T2.3.7**: Implement `mergeThirdPartyApps()` with smart merge strategy

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: T2.1 (Interface), T2.2 (Third-Party Discovery)
**Acceptance Criteria**:
- Implementation in `com/nononsenseapps/feeder/ui/text/SelectionMenuConfigRepositoryImpl.kt`
- `observeConfig()` emits merged config
- `updateOrder()` persists to SharedPreferences
- `updateEnabled()` toggles enabled state
- `resetToDefaults()` restores default config
- Third-party apps merged correctly (new apps added, existing preserved)

---

## Phase 3: ViewModel Layer (3-4 hours)

### Task 3.1: Create ViewModel
- [ ] **T3.1.1**: Create `SelectionMenuSettingsViewModel` class
- [ ] **T3.1.2**: Define `SelectionMenuSettingsUiState` data class
- [ ] **T3.1.3**: Define `DraggedItem` data class
- [ ] **T3.1.4**: Create `_uiState: MutableStateFlow<UiState>`
- [ ] **T3.1.5**: Implement `loadConfig()` to observe repository
- [ ] **T3.1.6**: Implement `onDragStart(index: Int)` method
- [ ] **T3.1.7**: Implement `onDragMove(newIndex: Int)` method
- [ ] **T3.1.8**: Implement `onDragEnd()` method
- [ ] **T3.1.9**: Implement `updateEnabled(itemId: String, enabled: Boolean)` method
- [ ] **T3.1.10**: Implement `resetToDefaults()` method

**Estimated Time**: 2 hours
**Priority**: High
**Dependencies**: T2.3 (Repository)
**Acceptance Criteria**:
- ViewModel in `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsViewModel.kt`
- StateFlow emits UiState
- Drag-and-drop logic reorders items
- Repository methods called correctly
- Error handling in place

---

### Task 3.2: Create ViewModel Factory
- [ ] **T3.2.1**: Create `SelectionMenuSettingsViewModelFactory` class
- [ ] **T3.2.2**: Implement `create()` method
- [ ] **T3.2.3**: Inject `SelectionMenuConfigRepository` dependency
- [ ] **T3.2.4**: Add type safety check for ViewModel class

**Estimated Time**: 0.5 hours
**Priority**: High
**Dependencies**: T3.1 (ViewModel)
**Acceptance Criteria**:
- Factory in `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsViewModelFactory.kt`
- Creates ViewModel instances
- Repository injected correctly
- No unchecked cast warnings

---

### Task 3.3: Add Dependency Injection Setup
- [ ] **T3.3.1**: Add provider for `ThirdPartyAppRepository` in DI module
- [ ] **T3.3.2**: Add provider for `SelectionMenuConfigRepository` in DI module
- [ ] **T3.3.3**: Verify DI module compiles without errors
- [ ] **T3.3.4**: Verify no circular dependencies

**Estimated Time**: 0.5 hours
**Priority**: Medium
**Dependencies**: T2.3 (Repository), T3.2 (Factory)
**Acceptance Criteria**:
- DI module updated in `com/nononsenseapps/feeder/di/AppModule.kt`
- Repositories can be injected
- No circular dependencies

---

## Phase 4: UI Layer (4-5 hours)

### Task 4.1: Create Drag-and-Drop Modifier
- [ ] **T4.1.1**: Create `draggableItem()` extension function on Modifier
- [ ] **T4.1.2**: Implement long-press detection using `pointerInput`
- [ ] **T4.1.3**: Implement drag tracking with `detectDragGestures`
- [ ] **T4.1.4**: Add visual feedback (elevation when dragging)
- [ ] **T4.1.5**: Test drag-and-drop responsiveness (target: 60fps)

**Estimated Time**: 2 hours
**Priority**: High
**Dependencies**: None
**Acceptance Criteria**:
- Modifier in `com/nononsenseapps/feeder/ui/compose/DraggableItemModifier.kt`
- Long-press triggers drag
- Drag movement tracked correctly
- Visual feedback applied (elevation, shadow)
- Responsive during drag (60fps)

---

### Task 4.2: Create List Item Row
- [ ] **T4.2.1**: Create `ListItemRow` composable
- [ ] **T4.2.2**: Add drag handle icon (Icons.Default.DragHandle)
- [ ] **T4.2.3**: Add text label for item name
- [ ] **T4.2.4**: Add Switch component for enable/disable
- [ ] **T4.2.5**: Apply `draggableItem` modifier
- [ ] **T4.2.6**: Add elevation change when dragging
- [ ] **T4.2.7**: Set minimum touch target size (48dp)

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: T4.1 (Drag Modifier)
**Acceptance Criteria**:
- Composable in `com/nononsenseapps/feeder/ui/compose/ListItemRow.kt`
- Drag handle visible on left
- Item label in center
- Switch on right
- Elevation changes when dragging
- Touch targets meet WCAG 2.1 (48dp minimum)

---

### Task 4.3: Create Settings Screen
- [ ] **T4.3.1**: Create `SelectionMenuSettingsScreen` composable
- [ ] **T4.3.2**: Add TopAppBar with title "Selection Menu Settings"
- [ ] **T4.3.3**: Add LazyColumn for scrollable list
- [ ] **T4.3.4**: Add "Built-in Actions" section header
- [ ] **T4.3.5**: Add "Third-Party Apps" section header
- [ ] **T4.3.6**: Render built-in actions using `items()`
- [ ] **T4.3.7**: Render third-party apps using `items()`
- [ ] **T4.3.8**: Add "Reset to Defaults" button
- [ ] **T4.3.9**: Connect to ViewModel (collect UiState)
- [ ] **T4.3.10**: Handle loading and error states

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: T4.2 (List Item Row), T3.1 (ViewModel)
**Acceptance Criteria**:
- Screen in `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsScreen.kt`
- Displays menu items in LazyColumn
- Sections separated with headers
- Drag-and-drop works
- Switches toggle enabled state
- Reset button restores defaults
- Loading and error states handled

---

## Phase 5: Integration (3-4 hours)

### Task 5.1: Integrate with FeederTextToolbar
- [ ] **T5.1.1**: Add `SelectionMenuConfigRepository` parameter to `FeederTextToolbar`
- [ ] **T5.1.2**: Observe configuration via `collectAsStateWithLifecycle`
- [ ] **T5.1.3**: Filter actions by enabled status
- [ ] **T5.1.4**: Sort actions by order field
- [ ] **T5.1.5**: Compose toolbar buttons based on sorted, filtered actions
- [ ] **T5.1.6**: Test toolbar updates when config changes

**Estimated Time**: 2 hours
**Priority**: High
**Dependencies**: T2.3 (Repository), T4.3 (Screen)
**Acceptance Criteria**:
- `FeederTextToolbar` observes config
- Buttons sorted by order
- Disabled actions filtered out
- Toolbar updates when config changes
- No visual glitches during updates

---

### Task 5.2: Add Navigation Entry
- [ ] **T5.2.1**: Add `SelectionMenuSettings` route to `Screen` sealed class
- [ ] **T5.2.2**: Add "Selection Menu" settings item in settings screen
- [ ] **T5.2.3**: Connect settings item click to navigation
- [ ] **T5.2.4**: Test navigation to selection menu settings

**Estimated Time**: 0.5 hours
**Priority**: Medium
**Dependencies**: T4.3 (Screen)
**Acceptance Criteria**:
- Route added to `com/nononsenseapps/feeder/ui/Navigation.kt`
- Settings item visible in settings screen
- Navigation works correctly

---

### Task 5.3: End-to-End Testing
- [ ] **T5.3.1**: Build debug APK and install on device/emulator
- [ ] **T5.3.2**: Navigate to selection menu settings
- [ ] **T5.3.3**: Test drag-and-drop reordering
- [ ] **T5.3.4**: Test disable action, verify toolbar update
- [ ] **T5.3.5**: Test enable action, verify toolbar update
- [ ] **T5.3.6**: Test reset to defaults, verify toolbar restore
- [ ] **T5.3.7**: Install third-party translator app
- [ ] **T5.3.8**: Verify third-party app appears in settings
- [ ] **T5.3.9**: Verify third-party app appears in toolbar
- [ ] **T5.3.10**: Uninstall third-party app, verify removal
- [ ] **T5.3.11**: Test configuration persistence (restart app)
- [ ] **T5.3.12**: Measure drag-and-drop performance (target: 60fps)
- [ ] **T5.3.13**: Test on low-end device (if available)
- [ ] **T5.3.14**: Test accessibility (screen reader, keyboard nav)

**Estimated Time**: 1.5 hours
**Priority**: High
**Dependencies**: T5.1 (Toolbar Integration), T5.2 (Navigation)
**Acceptance Criteria**:
- All test scenarios pass
- No crashes or ANRs
- Drag-and-drop maintains 60fps
- Configuration persists across app restarts
- Accessibility features work correctly

---

## Phase 6: Documentation (0.5-1 hour)

### Task 6.1: Add Code Comments
- [ ] **T6.1.1**: Add KDoc comments to `SelectionMenuItem` data class
- [ ] **T6.1.2**: Add KDoc comments to `SelectionMenuConfigRepository` interface
- [ ] **T6.1.3**: Add KDoc comments to `SelectionMenuConfigRepositoryImpl` class
- [ ] **T6.1.4**: Add KDoc comments to `SelectionMenuSettingsViewModel` class
- [ ] **T6.1.5**: Add KDoc comments to `SelectionMenuSettingsScreen` composable
- [ ] **T6.1.6**: Add inline comments for drag-and-drop logic
- [ ] **T6.1.7**: Add inline comments for merge logic

**Estimated Time**: 0.5 hours
**Priority**: Medium
**Dependencies**: All implementation tasks
**Acceptance Criteria**:
- All public classes have KDoc comments
- All public methods have KDoc comments
- Complex logic has inline comments
- Parameters and return types documented

---

### Task 6.2: Update README
- [ ] **T6.2.1**: Add "Selection Menu Configuration" section to README
- [ ] **T6.2.2**: Document features (reorder, enable/disable, third-party)
- [ ] **T6.2.3**: Document configuration JSON format
- [ ] **T6.2.4**: Document third-party app integration
- [ ] **T6.2.5**: Add screenshot (if available)

**Estimated Time**: 0.5 hours
**Priority**: Low
**Dependencies**: T6.1 (Code Comments)
**Acceptance Criteria**:
- README updated with feature description
- Configuration format documented
- Third-party integration explained

---

## Additional Tasks

### Testing Tasks
- [ ] **T7.1**: Write unit tests for ViewModel (drag-and-drop logic)
- [ ] **T7.2**: Write unit tests for Repository (merge logic)
- [ ] **T7.3**: Write unit tests for Serializer (serialization/deserialization)
- [ ] **T7.4**: Write UI tests for drag-and-drop interaction
- [ ] **T7.5**: Write integration tests for persistence

**Estimated Time**: 4-6 hours (can be done in parallel with implementation)

---

### Code Review Tasks
- [ ] **T8.1**: Self-review code changes
- [ ] **T8.2**: Address any code quality issues
- [ ] **T8.3**: Verify all acceptance criteria met
- [ ] **T8.4**: Prepare for peer review

**Estimated Time**: 1-2 hours

---

### Release Tasks
- [ ] **T9.1**: Create release build APK
- [ ] **T9.2**: Test release build on device
- [ ] **T9.3**: Prepare release notes
- [ ] **T9.4**: Tag version in git

**Estimated Time**: 0.5 hours

---

## Task Summary

| Phase | Tasks | Time | Status |
|-------|-------|------|--------|
| Phase 1: Data Layer | 4 | 4-5h | Pending |
| Phase 2: Repository Layer | 3 | 3-4h | Pending |
| Phase 3: ViewModel Layer | 3 | 3-4h | Pending |
| Phase 4: UI Layer | 3 | 4-5h | Pending |
| Phase 5: Integration | 3 | 3-4h | Pending |
| Phase 6: Documentation | 2 | 0.5-1h | Pending |
| Testing | 5 | 4-6h | Pending |
| Code Review | 4 | 1-2h | Pending |
| Release | 4 | 0.5h | Pending |
| **Total** | **31** | **24-32h** | |

---

## Task Dependencies

```
T1.1 → T1.2 → T1.3 → T2.1 → T2.3 → T3.1 → T3.2 → T3.3
                           ↓
T1.4                   T2.2 ────────┘
                           ↓
                        T4.1 → T4.2 → T4.3 → T5.2
                                      ↓
                                   T5.1 → T5.3
                                      ↓
                                   T6.1 → T6.2
```

---

## Risk Tasks (High Priority)

These tasks have high risk and should be completed early:

- [ ] **T1.4**: Gradle dependencies (blocker for all serialization)
- [ ] **T2.2**: Third-party discovery (untested PackageManager APIs)
- [ ] **T4.1**: Drag-and-drop modifier (complex gesture handling)
- [ ] **T5.1**: Toolbar integration (requires careful state management)

---

## Definition of Done

A task is **complete** when:
- [ ] Code is written and compiles
- [ ] Unit tests pass (if applicable)
- [ ] Acceptance criteria met
- [ ] Code reviewed (self or peer)
- [ ] Documentation updated (if needed)

A phase is **complete** when:
- [ ] All tasks in phase are complete
- [ ] Integration tests pass
- [ ] No blocking issues
- [ ] Ready for next phase

---

**END OF TASK LIST**
