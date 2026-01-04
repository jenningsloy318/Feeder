# Technical Specification: Selection Menu Configuration Feature

**Document Version**: 1.0
**Date**: 2026-01-04
**Author**: Coordinator Agent
**Status**: Ready for Implementation

## 1. Executive Summary

### 1.1 Overview
This specification describes the implementation of a Selection Menu Configuration feature for the Feeder RSS reader Android application. The feature adds a new settings screen accessible from Settings → Text, displaying a placeholder for future global menu configuration.

### 1.2 Scope
- **In Scope**: Navigation, empty state UI, placeholder ViewModel
- **Out of Scope**: Actual menu loading, CRUD operations, persistence

### 1.3 Key Deliverables
1. SelectionMenuSettingsScreen.kt - Main UI screen
2. SelectionMenuSettingsViewModel.kt - ViewModel with state management
3. Navigation integration in NavigationDestinations.kt
4. Settings screen integration in TextSettings.kt
5. String resources in strings.xml
6. Unit tests for ViewModel

## 2. Requirements Summary

### 2.1 Functional Requirements
- **FR1**: Add "Selection Menu" navigation item in Text Settings screen
- **FR2**: Create Selection Menu Configuration screen with empty state
- **FR3**: Display placeholder message for future menu functionality

### 2.2 Non-Functional Requirements
- **NFR1**: Follow Material3 design guidelines
- **NFR2**: Support phone, tablet, and foldable form factors
- **NFR3**: Provide proper accessibility labels
- **NFR4**: Achieve ≥80% unit test coverage
- **NFR5**: Zero compiler warnings

### 2.3 Technical Requirements
- **TR1**: Navigation route: `settings/selection-menu`
- **TR2**: MVVM architecture with DIAwareViewModel
- **TR3**: StateFlow for state management
- **TR4**: Material3 UI components
- **TR5**: Kodein DI for ViewModel injection

## 3. Architecture Design

### 3.1 Architecture Pattern

```
┌─────────────────────────────────────┐
│         Navigation Layer            │
│  (NavigationDestinations.kt)        │
└──────────────┬──────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (SelectionMenuSettingsScreen.kt)   │
└──────────────┬──────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│        ViewModel Layer              │
│  (SelectionMenuSettingsViewModel)   │
└──────────────┬──────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│      Repository Layer               │
│  (Future: SelectionMenuRepository)  │
└─────────────────────────────────────┘
```

### 3.2 Component Relationships

```kotlin
// Navigation
SettingsDestination
  └─> TextSettingsDestination
        └─> SelectionMenuSettingsDestination

// DI
DIAwareViewModel
  └─> SelectionMenuSettingsViewModel
        └─> Repository (future)
```

### 3.3 Data Flow

```kotlin
User Action (tap "Selection Menu")
  ↓
Navigation (navigate to SelectionMenuSettingsDestination)
  ↓
Screen Creation (diAwareViewModel<SelectionMenuSettingsViewModel>)
  ↓
State Collection (collectAsStateWithLifecycle)
  ↓
UI Rendering (Scaffold + EmptyState)
```

## 4. Component Specification

### 4.1 SelectionMenuSettingsScreen

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SelectionMenuSettingsScreen.kt`

**Responsibility**: Main UI screen for selection menu configuration

**Signature**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
)
```

**Components**:
1. SensibleTopAppBar with title and back button
2. SelectionMenuContent with empty state
3. Proper padding and scroll support

**State Management**:
```kotlin
val viewState by viewModel.viewState.collectAsStateWithLifecycle()
```

### 4.2 SelectionMenuSettingsViewModel

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SelectionMenuSettingsViewModel.kt`

**Responsibility**: Manage screen state and business logic

**Signature**:
```kotlin
class SelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    // State and events
}
```

**State**:
```kotlin
@Immutable
data class SelectionMenuViewState(
    val isLoading: Boolean = false,
    val items: List<SelectionMenuItem> = emptyList(),
    val error: String? = null,
)
```

**Events**:
```kotlin
sealed class SelectionMenuEvent {
    data object LoadMenus : SelectionMenuEvent()
    data class AddMenu(val item: SelectionMenuItem) : SelectionMenuEvent()
    data class RemoveMenu(val id: String) : SelectionMenuEvent()
}
```

**State Flow**:
```kotlin
private val _viewState = MutableStateFlow(SelectionMenuViewState())
val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()
```

### 4.3 SelectionMenuItem (Future)

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SelectionMenuItem.kt`

**Responsibility**: Data model for menu items

**Signature**:
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

### 4.4 Navigation Destination

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

**Addition**:
```kotlin
data object SelectionMenuSettingsDestination : NavigationDestination(
    path = "settings/selection-menu",
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
        val viewModel: SelectionMenuSettingsViewModel = backStackEntry.diAwareViewModel()

        SelectionMenuSettingsScreen(
            onNavigateUp = {
                navController.popBackStack()
            },
            viewModel = viewModel,
        )
    }
}
```

### 4.5 Settings Integration

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Changes**:
1. Add navigation parameter to SettingsScreen:
```kotlin
fun SettingsScreen(
    // ... existing parameters
    onNavigateToSelectionMenuSettings: () -> Unit = {},
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
)
```

2. Add to SettingsList parameters:
```kotlin
fun SettingsList(
    // ... existing parameters
    onNavigateToSelectionMenuSettings: () -> Unit = {},
    // ...
)
```

3. Pass to SettingsDestination.RegisterScreen:
```kotlin
onNavigateToSelectionMenuSettings = {
    SelectionMenuSettingsDestination.navigate(navController)
}
```

### 4.6 Text Settings Integration

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSettings.kt`

**Addition** (after text preview section):
```kotlin
// After TextSettingsContent(...)
ExternalSetting(
    currentValue = "",
    title = stringResource(R.string.selection_menu_title),
    onClick = onSelectionMenuSettings,
)
```

## 5. Navigation Flow

### 5.1 Navigation Graph

```
SettingsDestination (settings)
  │
  ├─> SyncScreenDestination (settings/sync)
  ├─> TextSettingsDestination (settings/text)
  │     │
  │     └─> SelectionMenuSettingsDestination (settings/selection-menu) ← NEW
  │
  ├─> ProviderListDestination (settings/providers)
  ├─> SummarySettingsDestination (settings/summary)
  └─> TranslationSettingsDestination (settings/translation)
```

### 5.2 Navigation Sequence

1. User opens Settings → Main Settings screen
2. User taps "Text" → Text Settings screen
3. User taps "Selection Menu" → Selection Menu Configuration screen
4. User taps back → Returns to Text Settings

### 5.3 Back Navigation

**Implementation**:
```kotlin
onNavigateUp = {
    navController.popBackStack()
}
```

**Fallback**: If back stack is empty, navigate to Settings

## 6. Data Flow

### 6.1 State Flow Diagram

```
SelectionMenuSettingsViewModel
  │
  ├─> _viewState: MutableStateFlow<SelectionMenuViewState>
  │     └─> viewState: StateFlow<SelectionMenuViewState> (read-only)
  │
  └─> onEvent(event: SelectionMenuEvent)
        └─> Update _viewState based on event
```

### 6.2 UI State Flow

```
User Opens Screen
  ↓
ViewModel Created (via diAwareViewModel)
  ↓
State Collected (collectAsStateWithLifecycle)
  ↓
Empty State Rendered
  ↓
User Taps Back
  ↓
popBackStack()
  ↓
Screen Destroyed, ViewModel Cleared
```

### 6.3 Event Handling (Future)

```kotlin
fun onEvent(event: SelectionMenuEvent) {
    when (event) {
        is SelectionMenuEvent.LoadMenus -> {
            viewModelScope.launch {
                // Repository call
                // Update state
            }
        }
        is SelectionMenuEvent.AddMenu -> {
            viewModelScope.launch {
                // Repository call
                // Update state
            }
        }
        is SelectionMenuEvent.RemoveMenu -> {
            viewModelScope.launch {
                // Repository call
                // Update state
            }
        }
    }
}
```

## 7. Implementation Details

### 7.1 File Structure

```
app/src/main/java/com/nononsenseapps/feeder/
├── ui/compose/settings/
│   ├── SelectionMenuSettingsScreen.kt          NEW
│   ├── SelectionMenuSettingsViewModel.kt       NEW
│   ├── SelectionMenuItem.kt                    NEW
│   ├── Settings.kt                             MODIFY
│   ├── TextSettings.kt                         MODIFY
├── ui/compose/navigation/
│   └── NavigationDestinations.kt               MODIFY
app/src/main/res/values/
└── strings.xml                                 MODIFY
app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/
└── SelectionMenuSettingsViewModelTest.kt      NEW
```

### 7.2 Implementation Order

1. **Create data models** (SelectionMenuItem.kt)
2. **Create ViewModel** (SelectionMenuSettingsViewModel.kt)
3. **Create screen UI** (SelectionMenuSettingsScreen.kt)
4. **Add navigation destination** (NavigationDestinations.kt)
5. **Integrate with Settings** (Settings.kt)
6. **Add to Text Settings** (TextSettings.kt)
7. **Add string resources** (strings.xml)
8. **Write unit tests** (SelectionMenuSettingsViewModelTest.kt)
9. **Test navigation flow**
10. **Polish and refine**

### 7.3 Code Standards

**Naming**:
- Classes: PascalCase
- Functions: camelCase
- Properties: camelCase
- Constants: UPPER_SNAKE_CASE

**Formatting**:
- Indentation: 4 spaces
- Line length: ≤ 120 characters
- Imports: Sorted alphabetically

**Documentation**:
- KDoc for public APIs
- Inline comments for complex logic
- TODO comments for future work

### 7.4 Dependencies

**Runtime Dependencies** (all existing):
- androidx.compose.ui:ui
- androidx.compose.material3:material3
- androidx.navigation:navigation-compose
- org.kodein.di:kodein-di
- androidx.lifecycle:lifecycle-*  **No new dependencies required** ✅

## 8. Testing Strategy

### 8.1 Unit Tests

**File**: `app/src/test/java/.../SelectionMenuSettingsViewModelTest.kt`

**Test Cases**:
1. `viewState should return empty state initially`
2. `onEvent(LoadMenus) should update state`
3. `onEvent(AddMenu) should add item to list`
4. `onEvent(RemoveMenu) should remove item from list`

**Example**:
```kotlin
class SelectionMenuSettingsViewModelTest {
    @Test
    fun `viewState should return empty state initially`() {
        // Arrange
        val viewModel = SelectionMenuSettingsViewModel(testDI)

        // Act
        val state = viewModel.viewState.value

        // Assert
        assertEquals(false, state.isLoading)
        assertEquals(true, state.items.isEmpty())
        assertEquals(null, state.error)
    }
}
```

**Coverage Target**: ≥ 80%

### 8.2 Integration Tests

**Navigation Flow**:
- Settings → Text → Selection Menu → Back
- Verify each transition completes
- Verify back stack management

**UI Rendering**:
- Phone portrait
- Phone landscape
- Tablet portrait
- Tablet landscape

### 8.3 Accessibility Tests

**Screen Reader**:
- Verify all elements have descriptions
- Verify reading order is logical
- Verify focus order is correct

**Touch Targets**:
- Verify minimum 48dp touch targets
- Verify interactive elements reachable

## 9. Deployment Plan

### 9.1 Build Configuration

**No build.gradle changes required** ✅

**Verification**:
```bash
./gradlew assembleDebug
```

### 9.2 Pre-Deployment Checklist

- [ ] All code compiles without errors
- [ ] All code compiles without warnings
- [ ] Unit tests pass (100%)
- [ ] Navigation flow works correctly
- [ ] UI renders on all screen sizes
- [ ] Accessibility labels present
- [ ] String resources defined
- [ ] No regressions in existing features

### 9.3 Release Notes

```
## Selection Menu Configuration (Placeholder)

Added navigation placeholder for Selection Menu Configuration feature:
- Accessible from Settings → Text → Selection Menu
- Displays empty state for future menu configuration
- Proper navigation and back stack handling

Note: This is a UI placeholder. Actual menu functionality coming in future release.
```

## 10. Maintenance Considerations

### 10.1 Future Enhancements

**Phase 2** (Menu CRUD):
- Implement SelectionMenuRepository
- Add menu items to database
- Implement load/save functionality
- Add menu item list UI

**Phase 3** (Advanced Features):
- Menu reordering (drag & drop)
- Menu categories
- Import/export configurations
- Menu presets/templates

### 10.2 Code Maintainability

**Design Decisions**:
- Simple empty state for easy enhancement
- ViewState pattern supports future complexity
- Event system supports multiple actions
- Repository abstraction for data layer

**Technical Debt**: None identified ✅

### 10.3 Documentation

**In-Code**:
- KDoc comments for public APIs
- Inline comments for complex logic
- TODO markers for future work

**External**:
- Specification documents
- Architecture diagrams
- Navigation flow charts

## 11. Risk Assessment

### 11.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Navigation conflicts | Low | Medium | Follow existing pattern exactly |
| UI inconsistency | Low | Low | Copy from TranslationSettings |
| DI registration issues | Very Low | Low | Automatic factory pattern |
| String resource conflicts | Very Low | Low | Use unique names |

### 11.2 Schedule Risks

**Estimated Time**: 2-3 hours
**Complexity**: Low
**Confidence**: High (99%)

**Buffer**: +1 hour for unexpected issues

## 12. Success Criteria

### 12.1 Functional Criteria

- [ ] Navigation from Settings to Selection Menu screen works
- [ ] Empty state displays correctly
- [ ] Back navigation returns to Text Settings
- [ ] No crashes on any supported screen size
- [ ] No crashes on orientation changes

### 12.2 Quality Criteria

- [ ] Zero compiler errors
- [ ] Zero compiler warnings
- [ ] Unit test coverage ≥ 80%
- [ ] All tests pass
- [ ] Code review approved

### 12.3 User Experience Criteria

- [ ] Navigation is intuitive
- [ ] Empty state message is clear
- [ ] Screen follows Material3 guidelines
- [ ] Accessibility features work correctly

## 13. Sign-Off

**Author**: Coordinator Agent
**Date**: 2026-01-04
**Status**: ✅ READY FOR IMPLEMENTATION
**Next Phase**: Specification Review (Phase 7)

**Approval Required**:
- [ ] Product Owner
- [ ] Tech Lead
- [ ] Security Review (if applicable)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-04
**Total Pages**: 15
**Total Requirements**: 13
**Estimated Implementation**: 2-3 hours
