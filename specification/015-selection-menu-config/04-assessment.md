# Code Assessment Report: Selection Menu Configuration Feature

**Specification ID**: 015
**Feature Name**: Selection Menu Configuration
**Assessment Date**: 2026-01-04
**Assessed By**: Super Dev Code Assessor

## Executive Summary

Comprehensive codebase assessment completed for the Selection Menu Configuration feature. The codebase follows consistent architectural patterns with clear separation of concerns. Existing infrastructure (SettingsStore, navigation, ViewModels) provides solid foundation for the new feature. The text selection toolbar implementation is well-structured and modular, enabling extension for custom configuration.

## 1. Architecture Overview

### 1.1 Application Structure

**Package Organization**:
```
com.nononsenseapps.feeder/
├── archmodel/              # Architecture models (SettingsStore, Repository)
├── ui/compose/
│   ├── feedarticle/       # ReaderView, ArticleScreen
│   ├── settings/          # Settings screens and ViewModels
│   ├── navigation/        # Navigation destinations
│   └── utils/             # Utilities (FeederTextToolbar)
├── db/room/               # Database entities
└── util/                  # Utilities and constants
```

**Key Architectural Patterns**:
- **MVVM**: Model-View-ViewModel with Compose UI
- **Repository Pattern**: Repository mediates between data sources and ViewModels
- **StateFlow**: Reactive state management throughout
- **Kodein DI**: Dependency injection for ViewModels and dependencies
- **SharedPreferences**: Simple persistence layer for settings

### 1.2 Text Selection Architecture

**Current Implementation** (`FeederTextToolbar.kt`):

```
ReaderView (Compose)
  └── SelectionContainer (Compose)
      └── LocalTextToolbar provides
          └── FeederTextToolbar (TextToolbar implementation)
              └── FeederTextActionModeCallback (ActionMode.Callback)
                  ├── System menu items (Copy, Paste, Cut, Select All)
                  └── Third-party processors (ACTION_PROCESS_TEXT)
```

**Key Components**:
1. **WithFeederTextToolbar**: Composable providing custom TextToolbar
2. **FeederTextToolbar**: Implements Compose TextToolbar interface
3. **FeederTextActionModeCallback**: Handles menu item creation and clicks
4. **MenuItemOption enum**: Defines system menu items (Copy, Paste, Cut, Select All)
5. **FloatingTextActionModeCallback**: Wrapper for API 23+ floating toolbar

**Integration Points**:
- `ReaderView.kt` line 110: Wraps content in SelectionContainer
- `SelectionContainer` uses `LocalTextToolbar` from ComposeProviders
- `WithFeederTextToolbar` in ComposeProviders provides custom implementation

## 2. Code Standards & Patterns

### 2.1 SettingsStore Pattern

**File**: `SettingsStore.kt`

**Pattern Observed**:
```kotlin
class SettingsStore(di: DI) : DIAware {
    // Private mutable state
    private val _setting = MutableStateFlow(loadFromPrefs())

    // Public read-only state
    val setting: StateFlow<T> = _setting.asStateFlow()

    // Mutator updates both state and persistence
    fun setSetting(value: T) {
        _setting.value = value
        sp.edit().putType(PREF_KEY, value).apply()
    }
}
```

**Relevant Examples**:
- Lines 541-700: Multi-provider AI settings with JSON serialization
- Lines 744-782: Summary and translation language settings
- Pattern: JSON for complex types, direct types for primitives

**Our Feature Should Follow**:
```kotlin
// Add to SettingsStore
private val json = Json { ignoreUnknownKeys = true }
private val _selectionMenuConfig = MutableStateFlow(loadSelectionMenuConfig())
val selectionMenuConfig: StateFlow<SelectionMenuConfig> = _selectionMenuConfig.asStateFlow()

fun setSelectionMenuConfig(config: SelectionMenuConfig) {
    _selectionMenuConfig.value = config
    val jsonString = json.encodeToString(config)
    sp.edit().putString(PREF_SELECTION_MENU_CONFIG, jsonString).apply()
}
```

### 2.2 ViewModel Pattern

**File**: `TextSettingsViewModel.kt` (and similar)

**Pattern Observed**:
```kotlin
class FeatureViewModel(di: DI) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    // Event handling
    fun onEvent(event: FeatureEvent) {
        when (event) {
            is FeatureEvent.SetSomething -> setSomething(event.value)
            // ...
        }
    }

    // View state as StateFlow
    private val _viewState = MutableStateFlow(FeatureState())
    val viewState: StateFlow<FeatureState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine multiple flows into state
            combine(flow1, flow2) { params ->
                FeatureState(params[0], params[1])
            }.collect { state ->
                _viewState.value = state
            }
        }
    }
}
```

**Our Feature Should Follow**:
```kotlin
class SelectionMenuSettingsViewModel(di: DI) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.ToggleItem -> toggleItem(event.itemId)
            is SelectionMenuEvent.ReorderItems -> reorderItems(event.from, event.to)
            is SelectionMenuEvent.ResetToDefaults -> resetToDefaults()
        }
    }

    private val _viewState = MutableStateFlow(SelectionMenuState())
    val viewState: StateFlow<SelectionMenuState> = _viewState.asStateFlow()
}
```

### 2.3 Settings Screen Pattern

**File**: `TextSettings.kt`

**Pattern Observed**:
```kotlin
@Composable
fun FeatureSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: FeatureViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            SensibleTopAppBar(
                title = stringResource(R.string.feature_settings),
                navigationIcon = { /* back button */ },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        FeatureSettingsView(
            viewState = viewState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}
```

**Common Components Used**:
- `SensibleTopAppBar`: Standard top app bar
- `Scaffold`: Material 3 scaffold layout
- `Header()`: Section header composable (line 410)
- `MenuSetting()`: Dropdown/radio button settings
- `ScaleSetting()`: Slider settings (line 429)
- Material 3 components: `Switch`, `Slider`, etc.

### 2.4 Navigation Pattern

**File**: `NavigationDestinations.kt`

**Pattern Observed**:
```kotlin
sealed class NavigationDestination(
    protected val path: String,
    protected val navArguments: List<NavigationArgument>,
    val deepLinks: List<NavDeepLink>,
    // transitions...
) {
    // Route is constructed from path + arguments

    fun register(navGraphBuilder: NavGraphBuilder, ...) {
        navGraphBuilder.composable(
            route = route,
            arguments = arguments,
            deepLinks = deepLinks,
        ) { backStackEntry ->
            // Get ViewModel with diAwareViewModel()
            // Call screen composable
        }
    }
}

data object FeatureSettingsDestination : NavigationDestination(
    path = "settings/feature",
    navArguments = emptyList(),
    deepLinks = emptyList(),
)
```

**Our Feature Should Add**:
```kotlin
data object SelectionMenuSettingsDestination : NavigationDestination(
    path = "settings/selection_menu",
    navArguments = emptyList(),
    deepLinks = emptyList(),
)
```

## 3. Integration Points

### 3.1 ReaderView Integration

**File**: `ReaderView.kt` (line 110)

**Current Code**:
```kotlin
@Composable
fun ReaderView(/* ... */) {
    SelectionContainer {
        // Article content
    }
}
```

**Required Changes**:
- **Minimal changes to ReaderView itself**
- FeederTextToolbar already uses LocalTextToolbar
- FeederTextActionModeCallback needs to read config from SettingsStore
- No changes to SelectionContainer needed

**Integration Strategy**:
```kotlin
// In FeederTextActionModeCallback
class FeederTextActionModeCallback(
    val context: Context,
    val activityLauncher: ActivityLauncher,
    // ADD: Pass settings store or config
    val selectionMenuConfig: SelectionMenuConfig,
    // ...
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        // ADD: Read config and build menu accordingly
        config.items
            .filter { it.enabled }
            .sortedBy { it.order }
            .forEach { item ->
                when (item.type) {
                    SYSTEM -> addSystemMenuItem(menu, item)
                    THIRD_PARTY -> addThirdPartyMenuItem(menu, item)
                }
            }
    }
}
```

### 3.2 Settings Screen Integration

**Navigation**: Add entry point from TextSettingsScreen

**Location**: `TextSettings.kt` or `Settings.kt`

**Implementation**:
```kotlin
// In TextSettingsContent
MenuSetting(
    title = stringResource(R.string.selection_menu),
    currentValue = /* navigate indicator */,
    values = ImmutableHolder(listOf(navigateIndicator)),
    onSelection = {
        onNavigateToSelectionMenuSettings.invoke()
    },
    icon = null,
)
```

### 3.3 ComposeProviders Integration

**File**: `ComposeProviders.kt`

**Current Code**:
```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    CompositionLocalProvider(
        LocalTextToolbar provides FeederTextToolbar(LocalView.current, activityLauncher)
    ) {
        content()
    }
}
```

**Required Changes**:
```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val settingsStore: SettingsStore by LocalDI.current.instance()

    // Observe settings changes
    val config by settingsStore.selectionMenuConfig.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalTextToolbar provides FeederTextToolbar(
            LocalView.current,
            activityLauncher,
            config = config.value, // ADD: Pass config
        )
    ) {
        content()
    }
}
```

## 4. Data Models

### 4.1 Recommended Data Model

Based on SettingsStore patterns (lines 541-700 for AI providers):

```kotlin
@Serializable
data class SelectionMenuItem(
    val id: String,                   // Unique ID
    val type: ItemType,               // SYSTEM or THIRD_PARTY
    val packageName: String? = null,   // For third-party
    val className: String? = null,     // For third-party
    val enabled: Boolean = true,       // Toggle state
    val order: Int,                    // Position in menu
)

@Serializable
enum class ItemType {
    SYSTEM,
    THIRD_PARTY,
}

@Serializable
data class SelectionMenuConfig(
    val items: List<SelectionMenuItem>,
    val version: Int = 1, // For migration
)
```

### 4.2 Default Configuration Strategy

Based on migration pattern (lines 571-634):

```kotlin
private fun loadSelectionMenuConfig(): SelectionMenuConfig {
    val jsonString = sp.getString(PREF_SELECTION_MENU_CONFIG, null)
    if (jsonString != null) {
        return try {
            json.decodeFromString<SelectionMenuConfig>(jsonString)
        } catch (e: Exception) {
            createDefaultConfig()
        }
    }
    return createDefaultConfig()
}

private fun createDefaultConfig(): SelectionMenuConfig {
    val systemItems = listOf(
        SelectionMenuItem("copy", ItemType.SYSTEM, enabled = true, order = 0),
        SelectionMenuItem("paste", ItemType.SYSTEM, enabled = true, order = 1),
        SelectionMenuItem("cut", ItemType.SYSTEM, enabled = true, order = 2),
        SelectionMenuItem("select_all", ItemType.SYSTEM, enabled = true, order = 3),
    )
    // Third-party items added dynamically
    return SelectionMenuConfig(items = systemItems)
}
```

## 5. Dependencies & Libraries

### 5.1 Current Dependencies

**UI Framework**:
- Jetpack Compose (Material 3)
- androidx.compose.foundation, material3, ui

**State Management**:
- kotlinx.coroutines.flow.*
- StateFlow, MutableStateFlow

**Persistence**:
- androidx.preference.SharedPreferences
- kotlinx.serialization.json.Json (for complex types)

**DI**:
- org.kodein.di:kodein-di

**Navigation**:
- androidx.navigation.compose

### 5.2 Required for Feature

**No New Dependencies Required**:
- Drag-and-drop: Custom modifier (from research)
- JSON serialization: Already used for AI providers
- StateFlow: Already used throughout
- Material 3: Switch, ListItem already available

## 6. Technical Debt & Risks

### 6.1 Code Quality Observations

**Strengths**:
- Consistent MVVM architecture
- Clear separation of concerns
- Strong typing with sealed interfaces for events
- Comprehensive state management with StateFlow
- Good use of Kodein DI

**Areas Caution**:
1. **Large SettingsStore file** (1165 lines): Consider module separation for future
2. **Hardcoded menu items** (FeederTextToolbar.kt): Currently in enum, needs refactoring to data model
3. **Clipboard hack** (lines 154-180): Workaround for ACTION_PROCESS_TEXT - keep as-is

### 6.2 Complexity Analysis

**FeederTextActionModeCallback**:
- Lines 84-253: ~170 lines
- Complexity: Medium
- Handles: Menu creation, item clicks, third-party discovery
- **Risk**: Adding config reading increases complexity moderately

**Recommendation**:
- Extract menu building logic to separate class: `SelectionMenuBuilder`
- Keep ActionMode.Callback focused on interaction handling

### 6.3 Performance Considerations

**Current Performance**:
- Third-party discovery on every toolbar show (lines 190-221)
- Caching: textProcessors list cleared and rebuilt

**Our Feature Impact**:
- Config read: Cached in StateFlow (minimal overhead)
- Menu building: Increased slightly due to filtering/sorting
- **Recommendation**: Cache discovered third-party apps, refresh only when needed

## 7. Reusable Components

### 7.1 UI Components

**From TextSettings.kt**:
- `Header()` (line 410): Section headers
- `MenuSetting()`: Dropdown settings (not in this file, but used)
- `ScaleSetting()` (line 429): Slider pattern
- `SensibleTopAppBar`: Top app bar
- `Scaffold` pattern: Standard layout

**From Other Settings Screens**:
- Toggle switches for boolean settings
- List items with drag handles (need to create)

### 7.2 Code Patterns

**Event Pattern**:
```kotlin
sealed interface FeatureEvent {
    data class SetSomething(val value: Type) : FeatureEvent
    data class DoSomething(val param: Type) : FeatureEvent
}
```

**State Pattern**:
```kotlin
data class FeatureState(
    val prop1: Type = default1,
    val prop2: Type = default2,
)
```

## 8. Testing Considerations

### 8.1 Current Testing Infrastructure

**Test Files Found** (from earlier grep):
- `TextSettingsViewModelTest.kt` (likely exists)
- `SettingsDestinationTest.kt`
- Navigation tests use Compose Testing

### 8.2 Test Strategy for Feature

**Unit Tests**:
- SettingsStore: Config serialization/deserialization
- ViewModel: Event handling, state updates
- Data model: Validation logic

**UI Tests**:
- Compose UI tests for drag-and-drop
- Toggle interactions
- Navigation

**Integration Tests**:
- FeederTextToolbar with custom config
- Settings persist across restarts

## 9. Migration & Compatibility

### 9.1 First Launch Experience

**Pattern from AI Settings** (lines 571-634):
- Check if config exists
- If not, create default config
- Save defaults to preferences
- Show UI with defaults populated

### 9.2 Third-Party App Handling

**Current Discovery** (lines 190-221):
- Uses `PackageManager.queryIntentActivities()`
- Sorts by display name
- Assigns IDs starting at 100

**Our Feature Enhancement**:
- Add discovered apps to config with `enabled = true` by default
- Allow user to toggle visibility
- Maintain order alongside system items
- **Handle missing apps**: Gracefully skip if uninstalled

## 10. Implementation Guidance

### 10.1 Recommended Implementation Order

1. **Data Model & Persistence** (SettingsStore)
   - Lowest complexity
   - Foundation for other components
   - Can test independently

2. **ViewModel & UI Scaffold**
   - Create ViewModel following pattern
   - Create screen scaffold
   - Wire up to SettingsStore

3. **Drag-and-Drop Modifier**
   - Independent component
   - Can develop in isolation
   - Test with sample data

4. **FeederTextToolbar Integration**
   - Most complex
   - Depends on config being ready
   - Requires careful testing

5. **Navigation Setup**
   - Simple but required
   - Do last or in parallel

### 10.2 Code Standards Checklist

- [ ] Extend SettingsStore with selectionMenuConfig StateFlow
- [ ] Create SelectionMenuSettingsViewModel extending DIAwareViewModel
- [ ] Create SelectionMenuSettingsScreen following pattern
- [ ] Add SelectionMenuSettingsDestination to Navigation
- [ ] Modify FeederTextActionModeCallback to accept config
- [ ] Update WithFeederTextToolbar to pass config
- [ ] Use Material 3 components (Switch, ListItem, etc.)
- [ ] Follow JSON serialization pattern from AI providers
- [ ] Implement drag-and-drop with custom modifier
- [ ] Add navigation entry from TextSettings or Settings

### 10.3 Quality Metrics

**Cyclomatic Complexity**:
- Keep methods under 10 (currently FeederTextActionModeCallback is borderline)
- Extract menu building logic if complexity increases

**Test Coverage**:
- Aim for >80% on new code
- Critical path: Config -> Toolbar -> Menu display

**Performance**:
- Menu display: <100ms (current standard)
- Config load: <50ms
- Drag-and-drop: 60fps

## 11. Key Findings Summary

### 11.1 Strengths

1. **Solid Foundation**: Existing patterns are consistent and well-documented
2. **Modular Design**: Text toolbar already abstracted (FeederTextToolbar)
3. **Reactive Architecture**: StateFlow enables real-time updates
4. **JSON Pattern**: Multi-provider settings provide template for complex config

### 11.2 Integration Complexity

**Low Complexity**:
- SettingsStore extension (follows existing pattern)
- Navigation (add one destination)
- ViewModel (follows existing pattern)

**Medium Complexity**:
- Drag-and-drop UI (custom modifier, documented patterns)
- FeederTextToolbar integration (need to read config, maintain existing behavior)

**High Complexity**:
- None identified

### 11.3 Risks & Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| Performance regression in menu display | Medium | Cache config, optimize menu building |
| Breaking existing text selection | Low | Extensive testing, backward compatibility |
| Third-party app changes during use | Low | Refresh discovery on toolbar show, handle missing apps |
| Complex drag-and-drop on older devices | Medium | Test on min API level, optimize animations |

## 12. Recommendations

### 12.1 Architecture Recommendations

1. **Follow Existing Patterns**: Don't reinvent - use SettingsStore, ViewModel, navigation patterns
2. **JSON for Config**: Leverage existing JSON serialization (no need for Room)
3. **Extract Menu Builder**: If FeederTextActionModeCallback becomes too complex, extract menu building
4. **Cache Third-Party Apps**: Don't query PackageManager every toolbar show

### 12.2 Implementation Recommendations

1. **Incremental Development**:
   - Start with data model + persistence (test independently)
   - Add ViewModel + UI scaffold (test with mock data)
   - Implement drag-and-drop (test in isolation)
   - Integrate with FeederTextToolbar (comprehensive testing)

2. **Testing Strategy**:
   - Unit tests for config serialization
   - UI tests for drag-and-drop interactions
   - Integration test for toolbar with custom config
   - Manual testing on physical devices

3. **Code Quality**:
   - Keep method complexity low
   - Follow existing naming conventions
   - Use sealed interfaces for events
   - Document any non-obvious logic

### 12.3 Next Steps

1. **Proceed to Architecture Design (Phase 5.3)**:
   - Design component interactions
   - Define data flow
   - Create sequence diagrams
   - Document ADRs if needed

2. **UI/UX Design (Phase 5.5)**:
   - Design settings screen layout
   - Design drag-and-drop interactions
   - Consider accessibility
   - Create mockups/wireframes

3. **Specification Writing (Phase 6)**:
   - Create technical specification
   - Create implementation plan
   - Create task list
   - Estimate effort

## Conclusion

The codebase assessment reveals a well-structured Android application with consistent architectural patterns. The Selection Menu Configuration feature can be implemented by following existing patterns for settings, ViewModels, and navigation. The modular design of FeederTextToolbar provides a clean integration point. No significant blockers or risks identified. Recommended approach: incremental development with comprehensive testing at each stage.
