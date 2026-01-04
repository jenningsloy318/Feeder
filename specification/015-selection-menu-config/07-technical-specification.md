# Technical Specification: Selection Menu Configuration Feature

**Feature ID**: 015
**Version**: 1.0.0
**Last Updated**: 2026-01-04
**Status**: Ready for Implementation

---

## 1. Overview

This document provides the complete technical specification for implementing customizable selection menus in the Feeder RSS reader application. Users will be able to customize the order and visibility of actions (translate, share, copy, open in browser) that appear in the text selection toolbar.

### 1.1 Scope

**In Scope**:
- Customizable order of text selection actions
- Enable/disable individual actions
- Drag-and-drop UI for reordering
- Real-time updates to selection toolbar
- Third-party app integration (Translate, Copy, Share, Open in Browser)
- Persistence across app sessions
- Automatic discovery of third-party apps

**Out of Scope**:
- Adding new action types beyond the four core actions
- Custom shortcuts or keyboard bindings
- Per-feed configuration (global settings only)

---

## 2. Functional Requirements

### 2.1 User Stories

**Primary User Story**:
> As a power user, I want to customize the order of actions in the selection menu, so that my most-used actions are readily accessible.

**Secondary User Stories**:
> As a user, I want to disable actions I never use, so that the menu is less cluttered.
> As a user, I want to reorder actions via drag-and-drop, so that I can arrange them intuitively.
> As a user with third-party apps installed, I want discovered apps to be added automatically to the menu, so that I don't have to configure them manually.

### 2.2 Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR1 | Users can specify the order of actions in the text selection toolbar | Must Have |
| FR2 | Users can enable/disable each action individually | Must Have |
| FR3 | Configuration persists across app sessions | Must Have |
| FR4 | Changes to configuration apply immediately to the text selection toolbar | Must Have |
| FR5 | Third-party apps (Translate, Copy, Share, Open in Browser) are discovered and added automatically | Should Have |
| FR6 | UI provides clear visual feedback for drag-and-drop operations | Should Have |

---

## 3. Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR1 | Configuration changes should apply within 500ms | ≤ 500ms |
| NFR2 | Drag-and-drop UI should respond within 16ms (60fps) | ≤ 16ms |
| NFR3 | Settings screen should load within 2 seconds | ≤ 2s |
| NFR4 | Configuration data size should be minimal | ≤ 5KB |

---

## 4. Technical Architecture

### 4.1 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
├─────────────────────────────────────────────────────────────┤
│  SelectionMenuSettingsScreen (Jetpack Compose)              │
│  ├── ListItemRow (drag-and-drop enabled)                    │
│  ├── SectionHeader                                          │
│  └── ActionSwitch                                           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                          │
├─────────────────────────────────────────────────────────────┤
│  SelectionMenuSettingsViewModel                             │
│  ├── _uiState: StateFlow<UiState>                          │
│  ├── _draggedItem: MutableStateFlow<DraggedItem?>          │
│  └── updateActionOrder(), updateActionEnabled()            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Repository Layer                         │
├─────────────────────────────────────────────────────────────┤
│  SelectionMenuConfigRepository (Interface)                  │
│  └── observeConfig(): Flow<List<SelectionMenuItem>>        │
│                                                              │
│  SelectionMenuConfigRepositoryImpl                          │
│  ├── _selectionMenuConfigStore: SettingsStore               │
│  └── _thirdPartyAppRepository: ThirdPartyAppRepository      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                               │
├─────────────────────────────────────────────────────────────┤
│  SettingsStore (SharedPreferences)                          │
│  ├── KEY: "selection_menu_config"                           │
│  └── Format: JSON array of SelectionMenuItem                │
│                                                              │
│  ThirdPartyAppRepository                                    │
│  ├── discoverTranslators(): List<TranslatorApp>            │
│  ├── discoverCopyApps(): List<CopyApp>                     │
│  └── discoverShareTargets(): List<ShareTarget>             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Data Models

```kotlin
/**
 * Represents a single action in the selection menu
 */
@Serializable
data class SelectionMenuItem(
    val id: String,              // Unique identifier (e.g., "translate", "copy", "share", "open_browser")
    val type: ActionType,        // Type of action (enum)
    val enabled: Boolean,        // Whether the action is enabled
    val order: Int,              // Display order (0-based)
    val label: String,           // Display label (localized)
    val thirdPartyPackageName: String? = null,  // For third-party apps
    val thirdPartyClassName: String? = null     // For third-party apps
)

/**
 * Types of selection menu actions
 */
enum class ActionType {
    TRANSLATE,      // Translation action
    COPY,           // Copy to clipboard
    SHARE,          // Share content
    OPEN_BROWSER,   // Open in browser
    CUSTOM          // Third-party custom actions
}

/**
 * Third-party translator app metadata
 */
data class TranslatorApp(
    val packageName: String,
    val className: String,
    val appName: String,
    val icon: Int?
)

/**
 * ViewModel UI state
 */
data class SelectionMenuSettingsUiState(
    val menuItems: List<SelectionMenuItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val draggedItem: DraggedItem? = null
)

/**
 * Drag-and-drop state
 */
data class DraggedItem(
    val index: Int,
    val item: SelectionMenuItem
)
```

### 4.3 Key Interfaces

```kotlin
/**
 * Repository interface for selection menu configuration
 */
interface SelectionMenuConfigRepository {
    /**
     * Observe the current selection menu configuration
     */
    observeConfig(): Flow<List<SelectionMenuItem>>

    /**
     * Update the order of menu items
     */
    suspend fun updateOrder(items: List<SelectionMenuItem>)

    /**
     * Update the enabled state of a menu item
     */
    suspend fun updateEnabled(itemId: String, enabled: Boolean)

    /**
     * Reset to default configuration
     */
    suspend fun resetToDefaults()
}

/**
 * Repository for discovering third-party apps
 */
interface ThirdPartyAppRepository {
    /**
     * Discover installed translator apps
     */
    suspend fun discoverTranslators(): List<TranslatorApp>

    /**
     * Discover apps that can handle copy actions
     */
    suspend fun discoverCopyApps(): List<CopyApp>

    /**
     * Discover share targets
     */
    suspend fun discoverShareTargets(): List<ShareTarget>
}
```

---

## 5. Component Specifications

### 5.1 SelectionMenuSettingsScreen

**Purpose**: Main composable for the selection menu settings screen

**Responsibilities**:
- Display list of configurable actions
- Handle drag-and-drop gestures
- Enable/disable actions via switches
- Reset to defaults

**Signature**:
```kotlin
@Composable
fun SelectionMenuSettingsScreen(
    uiState: SelectionMenuSettingsUiState,
    onUpdateOrder: (List<SelectionMenuItem>) -> Unit,
    onUpdateEnabled: (String, Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Layout**:
```
┌─────────────────────────────────────┐
│  Selection Menu Settings            │  ← TopAppBar
├─────────────────────────────────────┤
│                                     │
│  Built-in Actions                   │  ← SectionHeader
│  ┌───────────────────────────────┐  │
│  │ [⋮⋮] Translate        [ON]   │  │  ← ListItemRow (draggable)
│  ├───────────────────────────────┤  │
│  │ [⋮⋮] Copy              [ON]   │  │
│  ├───────────────────────────────┤  │
│  │ [⋮⋮] Share             [ON]   │  │
│  ├───────────────────────────────┤  │
│  │ [⋮⋮] Open in Browser   [ON]   │  │
│  └───────────────────────────────┘  │
│                                     │
│  Third-Party Apps                   │  ← SectionHeader
│  ┌───────────────────────────────┐  │
│  │ [⋮⋮] DeepL Translate   [ON]   │  │  ← Dynamically discovered
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │     Reset to Defaults         │  │  ← Button
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 5.2 ListItemRow

**Purpose**: Draggable list item with drag handle and switch

**Responsibilities**:
- Display action label
- Show drag handle on long press
- Enable/disable via switch
- Visual feedback during drag

**Signature**:
```kotlin
@Composable
fun ListItemRow(
    item: SelectionMenuItem,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
```

### 5.3 Drag-and-Drop Modifier

**Purpose**: Custom modifier for handling drag-and-drop gestures

**Responsibilities**:
- Detect long press to initiate drag
- Track drag position
- Reorder items on drop
- Visual feedback (elevation, shadow)

**Signature**:
```kotlin
fun Modifier.draggableItem(
    draggedItem: StateFlow<DraggedItem?>,
    onDragStart: (Int) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier
```

### 5.4 SelectionMenuSettingsViewModel

**Purpose**: Manage UI state and business logic

**Responsibilities**:
- Load configuration from repository
- Handle drag-and-drop logic
- Update configuration
- Reset to defaults

**Signature**:
```kotlin
class SelectionMenuSettingsViewModel(
    private val repository: SelectionMenuConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectionMenuSettingsUiState())
    val uiState: StateFlow<SelectionMenuSettingsUiState> = _uiState.asStateFlow()

    fun updateOrder(newOrder: List<SelectionMenuItem>)
    fun updateEnabled(itemId: String, enabled: Boolean)
    fun resetToDefaults()
}
```

---

## 6. Data Flow

### 6.1 Configuration Loading Flow

```
User opens settings screen
    ↓
SelectionMenuSettingsViewModel initializes
    ↓
ViewModel calls repository.observeConfig()
    ↓
Repository reads from SettingsStore (SharedPreferences)
    ↓
Repository merges with discovered third-party apps
    ↓
ViewModel emits UiState with menu items
    ↓
SelectionMenuSettingsScreen composes UI
```

### 6.2 Drag-and-Drop Flow

```
User long-presses drag handle
    ↓
onDragStart() called with item index
    ↓
ViewModel updates draggedItem state
    ↓
UI shows elevated item with shadow
    ↓
User drags item to new position
    ↓
onDragMove() called with position
    ↓
UI shows placeholder at new position
    ↓
User releases drag
    ↓
onDragEnd() called
    ↓
ViewModel reorders list and calls repository.updateOrder()
    ↓
Repository persists to SharedPreferences
    ↓
ViewModel emits updated UiState
    ↓
FeederTextToolbar recomposes with new order
```

### 6.3 Real-Time Update Flow

```
User changes configuration in settings
    ↓
Configuration persisted to SharedPreferences
    ↓
FeederTextToolbar observes configuration via Flow
    ↓
Toolbar recomposes with new configuration
    ↓
Next text selection shows updated menu
```

### 6.4 Third-Party App Discovery Flow

```
User opens settings screen
    ↓
ViewModel triggers third-party discovery
    ↓
ThirdPartyAppRepository queries PackageManager
    ↓
Repository discovers apps for each action type:
    - Translate: Apps with ACTION_PROCESS_TEXT
    - Copy: Apps with clipboard listeners
    - Share: Apps with share handlers
    ↓
Discovered apps merged with built-in actions
    ↓
Merge strategy:
    - Add new apps after built-in actions
    - Preserve existing user customizations
    - Update metadata for existing apps
    ↓
ViewModel emits updated UiState
    ↓
UI shows all available actions
```

---

## 7. State Machines

### 7.1 ViewModel State Machine

```
┌─────────────┐
│  Initial    │
└──────┬──────┘
       │ loadConfig()
       ↓
┌─────────────┐
│   Loading   │
└──────┬──────┘
       │
       ├─────────────────┐
       │                 │
       ↓                 ↓
┌─────────────┐   ┌─────────────┐
│    Ready    │   │    Error    │
└──────┬──────┘   └─────────────┘
       │
       ├──────────────┐
       │              │
       ↓              ↓
┌─────────────┐  ┌─────────────┐
│   Dragging  │  │  Editing    │
└─────────────┘  └─────────────┘
       │              │
       └──────┬───────┘
              ↓
       ┌─────────────┐
       │    Ready    │
       └─────────────┘
```

### 7.2 Drag State Machine

```
┌──────────────┐
│   Idle       │
└──────┬───────┘
       │ onDragStart()
       ↓
┌──────────────┐
│   Dragging   │ ← Item elevated, shadow visible
└──────┬───────┘
       │ onDragMove()
       │ (Track position)
       │
       ├────────────────┐
       │                │
       │                ↓
       │         ┌──────────────┐
       │         │ Over Item    │
       │         └──────────────┘
       │                │
       │                │ (Swap positions)
       │                │
       └────────────────┘
       │ onDragEnd()
       ↓
┌──────────────┐
│   Dropping   │ ← Apply reorder, persist
└──────┬───────┘
       │
       ↓
┌──────────────┐
│   Idle       │
└──────────────┘
```

---

## 8. Integration Points

### 8.1 FeederTextToolbar Integration

**File**: `com.nononsenseapps.feeder.ui.text/FeederTextToolbar.kt`

**Changes Required**:
1. Add dependency on SelectionMenuConfigRepository
2. Observe configuration via Flow
3. Sort toolbar buttons based on configuration order
4. Filter out disabled actions
5. Recompose when configuration changes

**Integration Points**:
```kotlin
// In FeederTextToolbar
@Composable
fun FeederTextToolbar(
    // ... existing parameters
    selectionMenuConfigRepository: SelectionMenuConfigRepository
) {
    val config by selectionMenuConfigRepository.observeConfig()
        .collectAsStateWithLifecycle(initial = emptyList())

    val enabledActions = config
        .filter { it.enabled }
        .sortedBy { it.order }

    // Compose toolbar buttons based on enabledActions
}
```

### 8.2 SettingsStore Integration

**File**: `com.nononsenseapps.feeder.util/SettingsStore.kt`

**Changes Required**:
1. Add extension functions for selection menu config
2. Use kotlinx.serialization for JSON handling
3. Provide default configuration

**Integration Points**:
```kotlin
// In SettingsStore
val Context.selectionMenuConfigStore: SettingsStore<List<SelectionMenuItem>>
    get() = SettingsStore(
        preferences = SharedPreferences(),
        key = "selection_menu_config",
        default = defaultSelectionMenuConfig,
        serializer = SelectionMenuItemSerializer
    )

private val defaultSelectionMenuConfig = listOf(
    SelectionMenuItem(
        id = "translate",
        type = ActionType.TRANSLATE,
        enabled = true,
        order = 0,
        label = LocalStrings.current.translate
    ),
    // ... other defaults
)
```

### 8.3 Navigation Integration

**File**: `com.nononsenseapps.feeder.ui/Navigation.kt`

**Changes Required**:
1. Add route for selection menu settings screen
2. Add menu item in settings screen

**Integration Points**:
```kotlin
// In Navigation
sealed class Screen(val route: String) {
    // ... existing screens
    object SelectionMenuSettings : Screen("selection_menu_settings")
}

// In SettingsScreen
@Composable
fun SettingsScreen() {
    // ... existing settings items
    SettingsItem(
        title = "Selection Menu",
        onClick = { navTo(Screen.SelectionMenuSettings.route) }
    )
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**Scope**: Business logic, data models, repository

**Test Cases**:
1. ViewModel state management
2. Drag-and-drop reordering logic
3. Configuration serialization/deserialization
4. Default configuration generation
5. Third-party app discovery
6. Merge strategies

**Example**:
```kotlin
class SelectionMenuSettingsViewModelTest {
    @Test
    fun `updateOrder should reorder items and persist`() = runTest {
        // Given
        val items = listOf(
            SelectionMenuItem("translate", ..., order = 0),
            SelectionMenuItem("copy", ..., order = 1)
        )
        val newOrder = items.reversed()

        // When
        viewModel.updateOrder(newOrder)

        // Then
        verify(repository).updateOrder(newOrder)
        assertEquals(newOrder, viewModel.uiState.value.menuItems)
    }
}
```

### 9.2 Integration Tests

**Scope**: Repository layer, persistence

**Test Cases**:
1. Configuration persistence across app restarts
2. Third-party app discovery
3. Real-time updates to toolbar
4. Merge strategies

**Example**:
```kotlin
class SelectionMenuConfigRepositoryTest {
    @Test
    fun `configuration should persist across app restarts`() = runTest {
        // Given
        val config = listOf(/* test config */)
        repository.updateOrder(config)

        // When
        val loaded = repository.observeConfig().first()

        // Then
        assertEquals(config, loaded)
    }
}
```

### 9.3 UI Tests

**Scope**: Compose UI, drag-and-drop

**Test Cases**:
1. Drag-and-drop reordering
2. Toggle enable/disable
3. Reset to defaults
4. Visual feedback during drag
5. Third-party app display

**Example**:
```kotlin
class SelectionMenuSettingsScreenTest {
    @Test
    fun `dragging item should reorder list`() {
        // Given
        composeTestRule.setContent {
            SelectionMenuSettingsScreen(/* ... */)
        }

        // When
        composeTestRule.onNodeWithText("Translate")
            .performTouchInput { swipeDown() }

        // Then
        composeTestRule.onNodeAtPosition(1)
            .assertTextContains("Translate")
    }
}
```

### 9.4 Manual Testing

**Test Scenarios**:
1. Drag item to new position
2. Disable action, verify it disappears from toolbar
3. Enable action, verify it appears in toolbar
4. Reset to defaults, verify order restored
5. Install third-party app, verify it appears in settings
6. Uninstall third-party app, verify it's removed

---

## 10. Error Handling

### 10.1 Error Scenarios

| Scenario | Error Type | Handling |
|----------|-----------|----------|
| SharedPreferences read failure | IOException | Show error message, use defaults |
| Serialization failure | SerializationException | Show error message, use defaults |
| Third-party discovery timeout | TimeoutCancellationException | Skip discovery, log warning |
| PackageManager unavailable | RuntimeException | Show error message, continue without third-party apps |

### 10.2 Error States

```kotlin
sealed class SelectionMenuError {
    object LoadFailed : SelectionMenuError()
    object SaveFailed : SelectionMenuError()
    object DiscoveryFailed : SelectionMenuError()
    data class Unknown(val message: String) : SelectionMenuError()
}

data class SelectionMenuSettingsUiState(
    // ... existing fields
    val error: SelectionMenuError? = null
)
```

### 10.3 Error Recovery

```kotlin
private fun loadConfig() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        try {
            repository.observeConfig().collect { config ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        menuItems = config,
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = SelectionMenuError.LoadFailed
                )
            }
        }
    }
}
```

---

## 11. Performance Considerations

### 11.1 Optimization Strategies

1. **Lazy Loading**: Discover third-party apps only when settings screen opens
2. **Caching**: Cache discovered apps for 5 minutes
3. **Diffing**: Use `mutableStateListOf` for efficient recomposition
4. **Debouncing**: Debounce drag-and-drop updates to avoid excessive writes

### 11.2 Performance Targets

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Configuration load | ≤ 100ms | Time to first frame |
| Drag-and-drop reordering | ≤ 16ms | Frame time during drag |
| Configuration save | ≤ 50ms | Write to SharedPreferences |
| Third-party discovery | ≤ 2s | One-time discovery on settings open |

---

## 12. Security Considerations

### 12.1 Data Privacy

- No user data is transmitted
- Configuration stored locally only
- Third-party app metadata (package name, class name) is not sensitive

### 12.2 Permissions

- No additional permissions required
- PackageManager queries use standard APIs

---

## 13. Accessibility

### 13.1 Screen Reader Support

- Drag handles announce: "Double tap and hold to start dragging"
- Switches announce: "Translate enabled, double tap to toggle"
- Order changes announced: "Moved to position 2"

### 13.2 Touch Targets

- Minimum 48dp x 48dp touch targets (WCAG 2.1)
- Drag handle: 48dp x 48dp
- Switch: 48dp x 48dp

### 13.3 Keyboard Navigation

- Tab key navigates between items
- Space key toggles switches
- Arrow keys reorder items (alternative to drag-and-drop)

---

## 14. Internationalization

### 14.1 Localized Strings

| String Key | English | Chinese |
|------------|---------|---------|
| selection_menu_settings | Selection Menu | 选择菜单 |
| built_in_actions | Built-in Actions | 内置操作 |
| third_party_apps | Third-Party Apps | 第三方应用 |
| reset_to_defaults | Reset to Defaults | 重置为默认 |
| drag_handle | Drag to reorder | 拖动以重新排序 |

---

## 15. Future Enhancements

### 15.1 Potential Features

1. **Per-feed configuration**: Allow different menus for different feeds
2. **Custom actions**: Let users define custom actions
3. **Import/export**: Share configurations between devices
4. **Analytics**: Track most-used actions for smarter defaults
5. **Gesture customization**: Configure gestures for quick actions

### 15.2 Technical Debt

1. **Migration path**: Plan for migrating from current hardcoded order
2. **Schema versioning**: Add version field to config for future migrations
3. **Telemetry**: Add usage analytics to inform default order

---

## 16. Appendices

### Appendix A: Configuration JSON Schema

```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "id": { "type": "string" },
      "type": { "type": "string", "enum": ["TRANSLATE", "COPY", "SHARE", "OPEN_BROWSER", "CUSTOM"] },
      "enabled": { "type": "boolean" },
      "order": { "type": "integer" },
      "label": { "type": "string" },
      "thirdPartyPackageName": { "type": "string", "nullable": true },
      "thirdPartyClassName": { "type": "string", "nullable": true }
    },
    "required": ["id", "type", "enabled", "order", "label"]
  }
}
```

### Appendix B: Default Configuration

```json
[
  {
    "id": "translate",
    "type": "TRANSLATE",
    "enabled": true,
    "order": 0,
    "label": "Translate"
  },
  {
    "id": "copy",
    "type": "COPY",
    "enabled": true,
    "order": 1,
    "label": "Copy"
  },
  {
    "id": "share",
    "type": "SHARE",
    "enabled": true,
    "order": 2,
    "label": "Share"
  },
  {
    "id": "open_browser",
    "type": "OPEN_BROWSER",
    "enabled": true,
    "order": 3,
    "label": "Open in Browser"
  }
]
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-01-04 | Super Dev | Initial specification |

---

**END OF TECHNICAL SPECIFICATION**
