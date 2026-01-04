# Implementation Plan: Selection Menu Configuration Feature

**Plan Version**: 1.0
**Date**: 2026-01-04
**Estimated Duration**: 2-3 hours
**Complexity**: Low

## 1. Implementation Phases

### Phase 1: Foundation (30 minutes)

**Objective**: Create data models and ViewModel

#### Task 1.1: Create SelectionMenuItem data class
**File**: `SelectionMenuItem.kt`
**Time**: 5 minutes
```kotlin
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val enabled: Boolean = true,
)
```

#### Task 1.2: Create ViewState and Event classes
**File**: `SelectionMenuSettingsViewModel.kt`
**Time**: 10 minutes
```kotlin
@Immutable
data class SelectionMenuViewState(
    val isLoading: Boolean = false,
    val items: List<SelectionMenuItem> = emptyList(),
    val error: String? = null,
)

sealed class SelectionMenuEvent {
    data object LoadMenus : SelectionMenuEvent()
    data class AddMenu(val item: SelectionMenuItem) : SelectionMenuEvent()
    data class RemoveMenu(val id: String) : SelectionMenuEvent()
}
```

#### Task 1.3: Create ViewModel class
**File**: `SelectionMenuSettingsViewModel.kt`
**Time**: 15 minutes
- Extend DIAwareViewModel
- Create StateFlow for viewState
- Implement onEvent function
- Add placeholder logic for events

### Phase 2: UI Components (45 minutes)

**Objective**: Create screen UI components

#### Task 2.1: Create EmptyState composable
**File**: `SelectionMenuSettingsScreen.kt`
**Time**: 15 minutes
```kotlin
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
) {
    // Icon, primary text, secondary text
}
```

#### Task 2.2: Create SelectionMenuContent composable
**File**: `SelectionMenuSettingsScreen.kt`
**Time**: 15 minutes
```kotlin
@Composable
fun SelectionMenuContent(
    viewState: SelectionMenuViewState,
    modifier: Modifier = Modifier,
) {
    // Scrollable column with empty state
}
```

#### Task 2.3: Create SelectionMenuSettingsScreen composable
**File**: `SelectionMenuSettingsScreen.kt`
**Time**: 15 minutes
- Scaffold with SensibleTopAppBar
- State collection
- Content rendering

### Phase 3: Navigation Integration (30 minutes)

**Objective**: Integrate with navigation system

#### Task 3.1: Add SelectionMenuSettingsDestination
**File**: `NavigationDestinations.kt`
**Time**: 15 minutes
- Create destination object
- Implement navigate() function
- Implement RegisterScreen() composable
- Register in NavGraph

#### Task 3.2: Add navigation parameter to Settings
**File**: `Settings.kt`
**Time**: 10 minutes
- Add onNavigateToSelectionMenuSettings parameter
- Add to SettingsList parameters
- Wire up in SettingsDestination

#### Task 3.3: Add navigation item to TextSettings
**File**: `TextSettings.kt`
**Time**: 5 minutes
- Add ExternalSetting for Selection Menu
- Position after text preview section

### Phase 4: String Resources (5 minutes)

**Objective**: Add required string resources

#### Task 4.1: Add strings to strings.xml
**File**: `app/src/main/res/values/strings.xml`
**Time**: 5 minutes
```xml
<string name="selection_menu_title">Selection Menu</string>
<string name="selection_menu_empty">No selection menus configured.</string>
<string name="selection_menu_empty_hint">This feature will allow you to configure global selection menus.</string>
```

### Phase 5: Testing (45 minutes)

**Objective**: Write and run tests

#### Task 5.1: Create ViewModel unit tests
**File**: `SelectionMenuSettingsViewModelTest.kt`
**Time**: 30 minutes
- Test initial state
- Test event handling
- Test state updates
- Achieve ≥80% coverage

#### Task 5.2: Test navigation flow
**Time**: 15 minutes
- Settings → Text → Selection Menu
- Back navigation
- Verify no crashes

### Phase 6: Polish and Refine (30 minutes)

**Objective**: Final polish and verification

#### Task 6.1: Code review checklist
**Time**: 15 minutes
- [ ] Compiles without errors
- [ ] Compiles without warnings
- [ ] Follows naming conventions
- [ ] Has proper documentation
- [ ] Accessibility labels present

#### Task 6.2: UI polish
**Time**: 15 minutes
- Verify on phone portrait
- Verify on phone landscape
- Verify on tablet
- Check empty state appearance
- Test orientation changes

## 2. Implementation Order Summary

```
1. SelectionMenuItem.kt (5 min)
   ↓
2. SelectionMenuSettingsViewModel.kt (15 min)
   ↓
3. SelectionMenuSettingsScreen.kt (45 min)
   ├─ EmptyState
   ├─ SelectionMenuContent
   └─ SelectionMenuSettingsScreen
   ↓
4. NavigationDestinations.kt (15 min)
   ↓
5. Settings.kt (10 min)
   ↓
6. TextSettings.kt (5 min)
   ↓
7. strings.xml (5 min)
   ↓
8. SelectionMenuSettingsViewModelTest.kt (30 min)
   ↓
9. Testing & Polish (45 min)
```

**Total Time**: 2-3 hours

## 3. File Creation Checklist

### New Files (3)
- [ ] `SelectionMenuItem.kt`
- [ ] `SelectionMenuSettingsViewModel.kt`
- [ ] `SelectionMenuSettingsScreen.kt`

### Modified Files (4)
- [ ] `NavigationDestinations.kt`
- [ ] `Settings.kt`
- [ ] `TextSettings.kt`
- [ ] `strings.xml`

### Test Files (1)
- [ ] `SelectionMenuSettingsViewModelTest.kt`

## 4. Code Templates

### Template 1: SelectionMenuItem.kt
```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.runtime.Immutable

/**
 * Represents a single selection menu item.
 *
 * @property id Unique identifier for the menu item
 * @property name Display name of the menu item
 * @property description Optional description
 * @property icon Optional icon resource name
 * @property enabled Whether the menu item is enabled
 */
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
)
```

### Template 2: ViewModel Structure
```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for the Selection Menu Configuration screen.
 *
 * Manages state for:
 * - Loading menu items
 * - Displaying menu list
 * - Handling user actions
 */
class SelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val _viewState = MutableStateFlow(SelectionMenuViewState())
    val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()

    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.LoadMenus -> {
                // TODO: Implement in Phase 2
            }
            is SelectionMenuEvent.AddMenu -> {
                // TODO: Implement in Phase 2
            }
            is SelectionMenuEvent.RemoveMenu -> {
                // TODO: Implement in Phase 2
            }
        }
    }
}
```

### Template 3: Screen Structure
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.selection_menu_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SelectionMenuContent(
            viewState = viewState,
            modifier = Modifier
                .padding(paddingValues)
                .navigationBarsPadding()
        )
    }
}
```

## 5. Testing Strategy

### 5.1 Unit Test Template
```kotlin
class SelectionMenuSettingsViewModelTest {
    private lateinit var viewModel: SelectionMenuSettingsViewModel
    private val testDI = DI { /* TODO: setup test DI */ }

    @Before
    fun setup() {
        viewModel = SelectionMenuSettingsViewModel(testDI)
    }

    @Test
    fun `viewState should return empty state initially`() {
        // Assert
        assertEquals(false, viewModel.viewState.value.isLoading)
        assertEquals(true, viewModel.viewState.value.items.isEmpty())
        assertEquals(null, viewModel.viewState.value.error)
    }
}
```

### 5.2 Integration Test Checklist
- [ ] Navigate: Settings → Text → Selection Menu
- [ ] Verify: TopAppBar displays correct title
- [ ] Verify: Empty state displays
- [ ] Verify: Back button returns to Text Settings
- [ ] Verify: No crashes on rotation
- [ ] Verify: No crashes on different screen sizes

## 6. Risk Mitigation

### 6.1 Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| Navigation doesn't work | Check destination registration and route path |
| Empty state doesn't show | Verify viewState.items.isEmpty() condition |
| Crash on back navigation | Ensure popBackStack() is called correctly |
| Wrong screen size | Verify maxContentWidth constraint |
| Missing strings | Check strings.xml for all required strings |

### 6.2 Rollback Plan

If critical issues arise:
1. Revert changes to NavigationDestinations.kt
2. Revert changes to Settings.kt and TextSettings.kt
3. Remove new files (SelectionMenu*)
4. Restore previous working state

## 7. Success Metrics

### 7.1 Completion Criteria

- [ ] All new files created
- [ ] All existing files modified
- [ ] Code compiles without errors
- [ ] Code compiles without warnings
- [ ] Unit tests pass (100%)
- [ ] Navigation flow works
- [ ] UI renders correctly
- [ ] No regressions

### 7.2 Quality Gates

**Gate 1: Build**
```bash
./gradlew assembleDebug
# Expected: SUCCESS
```

**Gate 2: Tests**
```bash
./gradlew test
# Expected: All tests pass
```

**Gate 3: Lint**
```bash
./gradlew lint
# Expected: No warnings
```

**Gate 4: Navigation**
- Manual test: Settings → Text → Selection Menu → Back
- Expected: Smooth navigation, no crashes

## 8. Timeline

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| Foundation | 30 min | T+0 | T+30 |
| UI Components | 45 min | T+30 | T+75 |
| Navigation | 30 min | T+75 | T+105 |
| Strings | 5 min | T+105 | T+110 |
| Testing | 45 min | T+110 | T+155 |
| Polish | 30 min | T+155 | T+185 |

**Total**: ~3 hours with buffer

## 9. Dependencies

### 9.1 External Dependencies
None - all dependencies already in project ✅

### 9.2 Internal Dependencies
- Must complete Phase 1 before Phase 2
- Must complete Phase 2 before Phase 3
- Phases 4-6 can run in parallel after Phase 3

## 10. Handoff

### 10.1 Pre-Implementation
- Review all specification documents
- Set up development environment
- Verify worktree is clean

### 10.2 Post-Implementation
- Run full test suite
- Perform manual testing
- Create pull request
- Update documentation

---

**Plan Completed**: 2026-01-04
**Next Action**: Begin Phase 1 - Foundation
**Status**: ✅ READY FOR EXECUTION
