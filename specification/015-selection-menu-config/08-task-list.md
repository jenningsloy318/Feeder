# Task List: Selection Menu Configuration Feature

**List Version**: 1.0
**Date**: 2026-01-04
**Total Tasks**: 25
**Estimated Time**: 2-3 hours

## Task Status Legend
- [ ] Pending
- [x] Complete
- [!] Blocked
- [~] In Progress

## 1. Data Model Tasks

### Task 1.1: Create SelectionMenuItem.kt
- [ ] Create file at `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SelectionMenuItem.kt`
- [ ] Add package declaration
- [ ] Add imports (Immutable, etc.)
- [ ] Create data class with properties:
  - [ ] id: String
  - [ ] name: String
  - [ ] description: String?
  - [ ] icon: String?
  - [ ] enabled: Boolean
- [ ] Add @Immutable annotation
- [ ] Add KDoc comment
- [ ] Verify file compiles

**Estimated Time**: 5 minutes
**Dependencies**: None
**Priority**: HIGH

## 2. ViewModel Tasks

### Task 2.1: Create SelectionMenuViewState
- [ ] Create data class in SelectionMenuSettingsViewModel.kt
- [ ] Add @Immutable annotation
- [ ] Add properties:
  - [ ] isLoading: Boolean = false
  - [ ] items: List<SelectionMenuItem> = emptyList()
  - [ ] error: String? = null
- [ ] Add KDoc comment

**Estimated Time**: 5 minutes
**Dependencies**: Task 1.1
**Priority**: HIGH

### Task 2.2: Create SelectionMenuEvent sealed class
- [ ] Create sealed class in SelectionMenuSettingsViewModel.kt
- [ ] Add events:
  - [ ] object LoadMenus
  - [ ] data class AddMenu
  - [ ] data class RemoveMenu
- [ ] Add KDoc comments

**Estimated Time**: 5 minutes
**Dependencies**: Task 1.1
**Priority**: HIGH

### Task 2.3: Create SelectionMenuSettingsViewModel class
- [ ] Create class extending DIAwareViewModel
- [ ] Add DI parameter to constructor
- [ ] Create _viewState MutableStateFlow
- [ ] Expose viewState as StateFlow
- [ ] Implement onEvent function
- [ ] Add placeholder logic for LoadMenus
- [ ] Add placeholder logic for AddMenu
- [ ] Add placeholder logic for RemoveMenu
- [ ] Add KDoc comment

**Estimated Time**: 15 minutes
**Dependencies**: Task 2.1, Task 2.2
**Priority**: HIGH

## 3. UI Component Tasks

### Task 3.1: Create EmptyState composable
- [ ] Create @Composable function
- [ ] Add Box with center alignment
- [ ] Add Icon (Icons.Outlined.Menu)
- [ ] Add primary Text (selection_menu_empty)
- [ ] Add secondary Text (selection_menu_empty_hint)
- [ ] Apply proper styling (MaterialTheme)
- [ ] Add semantics for accessibility
- [ ] Verify layout looks correct

**Estimated Time**: 15 minutes
**Dependencies**: Task 2.3
**Priority**: HIGH

### Task 3.2: Create SelectionMenuContent composable
- [ ] Create @Composable function
- [ ] Add Column with vertical scroll
- [ ] Add width constraint (maxContentWidth)
- [ ] Add conditional rendering:
  - [ ] If items.isEmpty() → EmptyState
  - [ ] Else → (placeholder for list)
- [ ] Add proper padding
- [ ] Verify scrolling works

**Estimated Time**: 15 minutes
**Dependencies**: Task 3.1
**Priority**: HIGH

### Task 3.3: Create SelectionMenuSettingsScreen composable
- [ ] Create @Composable function
- [ ] Add parameters:
  - [ ] onNavigateUp: () -> Unit
  - [ ] viewModel: SelectionMenuSettingsViewModel
  - [ ] modifier: Modifier = Modifier
- [ ] Collect viewState with collectAsStateWithLifecycle
- [ ] Create scrollBehavior
- [ ] Add Scaffold with SensibleTopAppBar
- [ ] Add navigation icon (ArrowBack)
- [ ] Add title (selection_menu_title)
- [ ] Call SelectionMenuContent with proper padding
- [ ] Add KDoc comment

**Estimated Time**: 15 minutes
**Dependencies**: Task 3.2
**Priority**: HIGH

## 4. Navigation Tasks

### Task 4.1: Create SelectionMenuSettingsDestination
- [ ] Add data object to NavigationDestinations.kt
- [ ] Extend NavigationDestination
- [ ] Set path to "settings/selection-menu"
- [ ] Set navArguments to emptyList()
- [ ] Set deepLinks to emptyList()
- [ ] Implement navigate() function
- [ ] Implement RegisterScreen() @Composable
- [ ] Get ViewModel via diAwareViewModel()
- [ ] Call SelectionMenuSettingsScreen
- [ ] Set onNavigateUp to popBackStack()

**Estimated Time**: 15 minutes
**Dependencies**: Task 3.3
**Priority**: HIGH

### Task 4.2: Register destination in NavGraph
- [ ] Find NavGraph setup
- [ ] Add SelectionMenuSettingsDestination.register()
- [ ] Verify no conflicts with existing destinations

**Estimated Time**: 5 minutes
**Dependencies**: Task 4.1
**Priority**: HIGH

## 5. Settings Integration Tasks

### Task 5.1: Add navigation parameter to SettingsScreen
- [ ] Add onNavigateToSelectionMenuSettings parameter
- [ ] Set default to empty lambda
- [ ] Add KDoc comment

**Estimated Time**: 5 minutes
**Dependencies**: Task 4.1
**Priority**: HIGH

### Task 5.2: Pass parameter through SettingsList
- [ ] Add onNavigateToSelectionMenuSettings to SettingsList
- [ ] Set default to empty lambda
- [ ] Pass from SettingsScreen to SettingsList

**Estimated Time**: 5 minutes
**Dependencies**: Task 5.1
**Priority**: HIGH

### Task 5.3: Wire up navigation in SettingsDestination
- [ ] Add onNavigateToSelectionMenuSettings lambda
- [ ] Call SelectionMenuSettingsDestination.navigate()
- [ ] Pass navController

**Estimated Time**: 5 minutes
**Dependencies**: Task 5.2
**Priority**: HIGH

### Task 5.4: Add ExternalSetting to TextSettings
- [ ] Find TextSettingsContent composable
- [ ] Add ExternalSetting after preview section
- [ ] Set currentValue to empty string
- [ ] Set title to selection_menu_title
- [ ] Set onClick to onSelectionMenuSettings callback
- [ ] Verify it appears in correct position

**Estimated Time**: 5 minutes
**Dependencies**: Task 5.1
**Priority**: HIGH

### Task 5.5: Add navigation parameter to TextSettingsScreen
- [ ] Add onSelectionMenuSettings parameter
- [ ] Set default to empty lambda
- [ ] Pass to ExternalSetting onClick

**Estimated Time**: 2 minutes
**Dependencies**: Task 5.1
**Priority**: HIGH

## 6. String Resource Tasks

### Task 6.1: Add strings to strings.xml
- [ ] Open app/src/main/res/values/strings.xml
- [ ] Add selection_menu_title
- [ ] Add selection_menu_empty
- [ ] Add selection_menu_empty_hint
- [ ] Verify no duplicate names
- [ ] Verify formatting is correct

**Estimated Time**: 5 minutes
**Dependencies**: None
**Priority**: MEDIUM

## 7. Testing Tasks

### Task 7.1: Create ViewModel test file
- [ ] Create SelectionMenuSettingsViewModelTest.kt
- [ ] Add package declaration
- [ ] Add imports (JUnit, MockK, etc.)
- [ ] Create test class

**Estimated Time**: 5 minutes
**Dependencies**: Task 2.3
**Priority**: MEDIUM

### Task 7.2: Write initial state test
- [ ] Test that viewState returns empty state initially
- [ ] Assert isLoading is false
- [ ] Assert items is empty
- [ ] Assert error is null

**Estimated Time**: 5 minutes
**Dependencies**: Task 7.1
**Priority**: MEDIUM

### Task 7.3: Write LoadMenus event test
- [ ] Test LoadMenus event
- [ ] Verify state updates correctly
- [ ] Add assertions for expected behavior

**Estimated Time**: 5 minutes
**Dependencies**: Task 7.1
**Priority**: LOW (placeholder)

### Task 7.4: Write AddMenu event test
- [ ] Test AddMenu event
- [ ] Verify item is added to list
- [ ] Add assertions for expected behavior

**Estimated Time**: 5 minutes
**Dependencies**: Task 7.1
**Priority**: LOW (placeholder)

### Task 7.5: Write RemoveMenu event test
- [ ] Test RemoveMenu event
- [ ] Verify item is removed from list
- [ ] Add assertions for expected behavior

**Estimated Time**: 5 minutes
**Dependencies**: Task 7.1
**Priority**: LOW (placeholder)

### Task 7.6: Run all tests
- [ ] Run ./gradlew test
- [ ] Verify all tests pass
- [ ] Check test coverage
- [ ] Fix any failures

**Estimated Time**: 10 minutes
**Dependencies**: Tasks 7.1-7.5
**Priority**: HIGH

### Task 7.7: Manual navigation testing
- [ ] Launch app on device/emulator
- [ ] Navigate to Settings
- [ ] Tap Text
- [ ] Tap Selection Menu
- [ ] Verify screen appears
- [ ] Tap back button
- [ ] Verify returns to Text Settings
- [ ] Test on phone portrait
- [ ] Test on phone landscape
- [ ] Test on tablet (if available)

**Estimated Time**: 15 minutes
**Dependencies**: All implementation tasks
**Priority**: HIGH

## 8. Quality Assurance Tasks

### Task 8.1: Verify compilation
- [ ] Run ./gradlew assembleDebug
- [ ] Check for errors
- [ ] Check for warnings
- [ ] Fix any issues

**Estimated Time**: 5 minutes
**Dependencies**: All implementation tasks
**Priority**: HIGH

### Task 8.2: Code review checklist
- [ ] Verify naming conventions followed
- [ ] Verify KDoc comments present
- [ ] Verify no TODOs left (except intentional placeholders)
- [ ] Verify no hardcoded strings
- [ ] Verify proper error handling
- [ ] Verify accessibility labels present
- [ ] Verify Material3 components used
- [ ] Verify proper state management

**Estimated Time**: 10 minutes
**Dependencies**: Task 8.1
**Priority**: HIGH

### Task 8.3: UI polish verification
- [ ] Check empty state appearance
- [ ] Check icon size and opacity
- [ ] Check text alignment
- [ ] Check padding and margins
- [ ] Check scrolling behavior
- [ ] Check orientation change handling

**Estimated Time**: 10 minutes
**Dependencies**: Task 8.1
**Priority**: MEDIUM

## 9. Documentation Tasks

### Task 9.1: Update CHANGELOG
- [ ] Add entry for Selection Menu Configuration
- [ ] Describe placeholder nature of feature
- [ ] Note future enhancements

**Estimated Time**: 5 minutes
**Dependencies**: Task 8.3
**Priority**: LOW

## 10. Final Verification Tasks

### Task 10.1: Complete smoke test
- [ ] Build project
- [ ] Run all tests
- [ ] Launch app
- [ ] Navigate through feature
- [ ] Verify no crashes
- [ ] Verify no regressions

**Estimated Time**: 10 minutes
**Dependencies**: All tasks
**Priority**: HIGH

### Task 10.2: Create git commit
- [ ] Stage all changed files
- [ ] Review git diff
- [ ] Write commit message
- [ ] Create commit
- [ ] Verify commit created

**Estimated Time**: 5 minutes
**Dependencies**: Task 10.1
**Priority**: HIGH

## Task Summary

### By Priority
**HIGH (17 tasks)**: Core implementation and testing
**MEDIUM (5 tasks)**: Quality assurance and polish
**LOW (3 tasks)**: Documentation and future tests

### By Phase
1. **Data Model**: 1 task (5 min)
2. **ViewModel**: 3 tasks (25 min)
3. **UI Components**: 3 tasks (45 min)
4. **Navigation**: 2 tasks (20 min)
5. **Settings Integration**: 5 tasks (22 min)
6. **Strings**: 1 task (5 min)
7. **Testing**: 7 tasks (45 min)
8. **QA**: 3 tasks (25 min)
9. **Documentation**: 1 task (5 min)
10. **Final**: 2 tasks (15 min)

**Total**: 25 tasks in ~3 hours

### Critical Path
```
1.1 → 2.1 → 2.2 → 2.3 → 3.1 → 3.2 → 3.3 → 4.1 → 4.2 → 5.1 → 5.2 → 5.3 → 5.4 → 5.5 → 7.6 → 8.1 → 10.1 → 10.2
```

### Parallelizable Tasks
- Task 6.1 (strings) can be done anytime
- Task 7.1-7.5 (tests) can be done in parallel with implementation

---

**List Completed**: 2026-01-04
**Status**: ✅ READY FOR EXECUTION
**Next Action**: Start Task 1.1 - Create SelectionMenuItem.kt
