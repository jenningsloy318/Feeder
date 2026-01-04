# Architecture Design Document: Selection Menu Configuration Feature

**Specification ID**: 015
**Feature Name**: Selection Menu Configuration
**Architecture Date**: 2026-01-04
**Architect**: Super Dev Architecture Agent
**Status**: Draft

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-04 | Super Dev | Initial architecture design |

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Requirements Overview](#requirements-overview)
3. [High-Level Architecture](#high-level-architecture)
4. [Component Design](#component-design)
5. [Data Flow Architecture](#data-flow-architecture)
6. [State Management](#state-management)
7. [Interface Specifications](#interface-specifications)
8. [Architecture Decision Records](#architecture-decision-records)
9. [Integration Strategy](#integration-strategy)
10. [Error Handling & Resilience](#error-handling--resilience)
11. [Security Considerations](#security-considerations)
12. [Performance Analysis](#performance-analysis)
13. [Testing Strategy](#testing-strategy)
14. [Deployment & Migration](#deployment--migration)
15. [Risk Assessment](#risk-assessment)

---

## 1. Executive Summary

### 1.1 Purpose

This document defines the architecture for the Selection Menu Configuration feature, which enables users to customize text selection menu items in the Feeder RSS reader's article view. Users can reorder items via drag-and-drop, enable/disable items with toggle switches, and manage third-party text processing applications.

### 1.2 Scope

**In Scope**:
- Configuration UI for selection menu items
- Persistence layer for menu configuration
- Integration with existing FeederTextToolbar
- Drag-and-drop reordering functionality
- Toggle switches for item visibility
- Third-party app discovery and management

**Out of Scope**:
- Custom text processor creation
- Per-feed or per-article configurations
- Import/export of configurations
- Selection menu styling/theming

### 1.3 Key Architectural Principles

1. **Reuse Over Reinvention**: Leverage existing SettingsStore, ViewModel, and navigation patterns
2. **Interface-First Design**: Define contracts before implementations
3. **Minimal Coupling**: New components integrate without tight coupling to existing code
4. **Reactive Updates**: StateFlow-driven real-time configuration changes
5. **Fail-Safe Defaults**: Graceful degradation with sensible defaults

---

## 2. Requirements Overview

### 2.1 Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR1 | Display all available menu items (system + third-party) | Must Have |
| FR2 | Enable/disable items via toggle switches | Must Have |
| FR3 | Reorder items via drag-and-drop | Must Have |
| FR4 | Persist configuration across app restarts | Must Have |
| FR5 | Reflect configuration in text selection toolbar | Must Have |
| FR6 | Provide sensible defaults for first-time users | Should Have |
| FR7 | Reset to defaults option | Should Have |
| FR8 | Handle third-party app installation/uninstallation | Should Have |

### 2.2 Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR1 | Settings changes apply immediately | <100ms |
| NFR2 | Drag-and-drop maintains 60fps | 60fps |
| NFR3 | Menu appearance latency | <100ms |
| NFR4 | Configuration load time | <50ms |
| NFR5 | Accessibility support | TalkBack compatible |
| NFR6 | Minimum API compatibility | API 23+ |

### 2.3 Constraints

| Constraint | Description |
|-----------|-------------|
| C1 | Must use existing Kodein DI pattern |
| C2 | Must follow existing SettingsStore pattern |
| C3 | Must integrate with existing Navigation pattern |
| C4 | Must not break existing text selection behavior |
| C5 | Must handle missing third-party apps gracefully |

---

## 3. High-Level Architecture

### 3.1 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Feeder Application                       │
│                                                                   │
│  ┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐  │
│  │   Reader    │    │  Settings Screen│    │  Settings Store │  │
│  │     View    │◄───│   Navigation    │◄───│   (Persistence) │  │
│  │             │    │                 │    │                 │  │
│  └──────┬──────┘    └────────┬────────┘    └────────┬────────┘  │
│         │                     │                      │           │
│         │                     │                      │           │
│         ▼                     ▼                      ▼           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            Selection Menu Configuration Module          │   │
│  │  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐  │   │
│  │  │  ViewModel   │  │  Drag & Drop  │  │  Data Model  │  │   │
│  │  │              │  │    Modifier   │  │              │  │   │
│  │  └──────┬───────┘  └───────────────┘  └──────────────┘  │   │
│  └─────────┼──────────────────────────────────────────────┘   │
│            │                                                   │
│            ▼                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Feeder Text Toolbar (Existing)            │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │         FeederTextActionModeCallback             │  │   │
│  │  │  (Consumes config, builds menu dynamically)      │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           Android Platform Services                    │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │   │
│  │  │ PackageManager│  │SharedPreferences│ │TextToolbar API│  │   │
│  │  └─────────────┘  └──────────────┘  └──────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ SelectionMenu   │  │   Text Settings │                  │
│  │ SettingsScreen  │  │   Screen (link) │                  │
│  │  (Compose UI)   │  │                 │                  │
│  └────────┬────────┘  └─────────────────┘                  │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────────────────────────────────────┐       │
│  │        Drag & Drop Modifier (Compose)            │       │
│  │        List Items, Toggles, Handlers            │       │
│  └─────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────────────────────────────────────────┐       │
│  │    SelectionMenuSettingsViewModel               │       │
│  │    - State management                           │       │
│  │    - Event handling                             │       │
│  │    - Business logic                             │       │
│  └────────────┬────────────────────────────────────┘       │
│               │                                             │
│               ▼                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │    SettingsStore (Repository Pattern)           │       │
│  │    - Config persistence                          │       │
│  │    - StateFlow exposure                          │       │
│  │    - Third-party discovery coordination          │       │
│  └────────────┬────────────────────────────────────┘       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────────┐
│                     Domain Layer                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │    SelectionMenuConfig (Data Model)             │       │
│  │    - Serializable                               │       │
│  │    - Validation rules                           │       │
│  │    - Default factory                            │       │
│  └─────────────────────────────────────────────────┘       │
│  ┌─────────────────────────────────────────────────┐       │
│  │    SelectionMenuItem (Entity)                   │       │
│  │    - System or Third-party                      │       │
│  │    - Enabled state                              │       │
│  │    - Order position                             │       │
│  └─────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Integration Layer                          │
│  ┌─────────────────────────────────────────────────┐       │
│  │    FeederTextToolbar (Existing, Modified)        │       │
│  │    - Accepts config via constructor              │       │
│  │    - Observes StateFlow for real-time updates    │       │
│  └────────────┬────────────────────────────────────┘       │
│               │                                             │
│               ▼                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │    FeederTextActionModeCallback                  │       │
│  │    - Builds menu from config                     │       │
│  │    - Filters disabled items                      │       │
│  │    - Respects item ordering                      │       │
│  └─────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Platform Layer (Android)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ PackageManager│  │ SharedPreferences│ │TextToolbar   │    │
│  │ (ACTION_PROCESS│  │  (JSON config)    │   API        │    │
│  │   _TEXT)      │  │               │  │              │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Component Design

### 4.1 Component Catalog

| Component | Type | Responsibility | Interfaces |
|-----------|------|----------------|-------------|
| **SelectionMenuSettingsViewModel** | ViewModel | State management, event handling, business logic | SelectionMenuEvent, SelectionMenuState |
| **SelectionMenuSettingsScreen** | Composable | UI rendering, user interactions | ViewModel callbacks |
| **DragToReorderModifier** | Modifier | Drag-and-drop gesture handling | onMove callback |
| **SelectionMenuConfig** | Data Model | Configuration data structure | Serializable |
| **SelectionMenuItem** | Entity | Individual menu item | Serializable |
| **SelectionMenuRepository** | Repository | Config persistence, third-party discovery | Repository interface |
| **FeederTextToolbar** | Integration | Text toolbar with config support | TextToolbar interface |
| **FeederTextActionModeCallback** | Callback | Menu building from config | ActionMode.Callback |

### 4.2 Component Details

#### 4.2.1 SelectionMenuSettingsViewModel

**Responsibility**: Manages UI state and handles user events for the settings screen.

**Interface**:
```kotlin
class SelectionMenuSettingsViewModel(di: DI) : DIAwareViewModel(di) {
    // State
    val viewState: StateFlow<SelectionMenuState>

    // Event handling
    fun onEvent(event: SelectionMenuEvent)

    // Operations
    suspend fun toggleItem(itemId: String, enabled: Boolean)
    suspend fun reorderItems(fromIndex: Int, toIndex: Int)
    suspend fun resetToDefaults()
    suspend fun refreshThirdPartyApps()
}
```

**State**:
```kotlin
data class SelectionMenuState(
    val items: List<SelectionMenuItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasChanges: Boolean = false,
    val showResetDialog: Boolean = false,
)
```

**Events**:
```kotlin
sealed interface SelectionMenuEvent {
    data class ToggleItem(val itemId: String, val enabled: Boolean) : SelectionMenuEvent
    data class ReorderItems(val fromIndex: Int, val toIndex: Int) : SelectionMenuEvent
    object ResetToDefaults : SelectionMenuEvent
    object ConfirmReset : SelectionMenuEvent
    object CancelReset : SelectionMenuEvent
    object RefreshThirdPartyApps : SelectionMenuEvent
}
```

#### 4.2.2 DragToReorderModifier

**Responsibility**: Handles drag-and-drop gestures for list items.

**Interface**:
```kotlin
fun Modifier.dragToReorder(
    item: SelectionMenuItem,
    itemList: List<SelectionMenuItem>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): Modifier
```

**Behavior**:
- Long press initiates drag
- Visual feedback during drag (elevation, offset)
- Updates item position as it moves over other items
- Calls `onMove` when drag completes

#### 4.2.3 SelectionMenuConfig

**Responsibility**: Serializable data model for menu configuration.

**Interface**:
```kotlin
@Serializable
data class SelectionMenuConfig(
    val items: List<SelectionMenuItem>,
    val version: Int = CURRENT_VERSION,
) {
    companion object {
        const val CURRENT_VERSION = 1
        fun createDefault(): SelectionMenuConfig
        fun mergeThirdPartyApps(config: SelectionMenuConfig, apps: List<ThirdPartyApp>): SelectionMenuConfig
    }

    fun validate(): ValidationResult
    fun getEnabledItems(): List<SelectionMenuItem>
}
```

**Validation**:
```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
)

sealed class ValidationError {
    object NoItemsEnabled : ValidationError()
    data class InvalidOrder(val details: String) : ValidationError()
    data class DuplicateItemId(val id: String) : ValidationError()
}
```

#### 4.2.4 FeederTextActionModeCallback (Modified)

**Responsibility**: Builds text selection menu from configuration.

**Changes**:
```kotlin
class FeederTextActionModeCallback(
    val context: Context,
    val activityLauncher: ActivityLauncher,
    val selectionMenuConfig: SelectionMenuConfig, // NEW
    val onActionModeDestroy: (() -> Unit)? = null,
    var rect: Rect = Rect.Zero,
    var onCopyRequested: (() -> Unit)? = null,
    var onPasteRequested: (() -> Unit)? = null,
    var onCutRequested: (() -> Unit)? = null,
    var onSelectAllRequested: (() -> Unit)? = null,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        // Build menu from config instead of hardcoded items
        buildMenuFromConfig(menu)
        return true
    }

    private fun buildMenuFromConfig(menu: Menu?) {
        config.items
            .filter { it.enabled }
            .sortedBy { it.order }
            .forEach { item ->
                when (item.type) {
                    ItemType.SYSTEM -> addSystemMenuItem(menu, item)
                    ItemType.THIRD_PARTY -> addThirdPartyMenuItem(menu, item)
                }
            }
    }
}
```

---

## 5. Data Flow Architecture

### 5.1 Configuration Read Flow

```
User Opens Settings Screen
        │
        ▼
┌─────────────────────────────────────────┐
│ SelectionMenuSettingsViewModel.init()   │
│ - Observes SettingsStore.config         │
│ - Loads third-party apps from PackageManager │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ SettingsStore.selectionMenuConfig       │
│ - Loads JSON from SharedPreferences     │
│ - Deserializes to SelectionMenuConfig   │
│ - Merges with discovered third-party apps│
│ - Emits via StateFlow                   │
└────────────┬────────────────────────────┘
             │ StateFlow emission
             ▼
┌─────────────────────────────────────────┐
│ ViewModel.collects config               │
│ - Combines with UI state                │
│ - Emits SelectionMenuState             │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ UI renders with config                  │
│ - Displays items in order              │
│ - Shows enabled/disabled state         │
│ - Handles drag-and-drop                │
└─────────────────────────────────────────┘
```

### 5.2 Configuration Update Flow

```
User Interacts (Toggle/Reorder)
        │
        ▼
┌─────────────────────────────────────────┐
│ UI Component emits event                │
│ - ToggleItem event                      │
│ - ReorderItems event                    │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ ViewModel.onEvent(event)                │
│ - Updates local state immediately       │
│ - Persists to SettingsStore            │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ SettingsStore.setSelectionMenuConfig()   │
│ - Serializes config to JSON             │
│ - Writes to SharedPreferences           │
│ - Updates StateFlow                     │
└────────────┬────────────────────────────┘
             │ StateFlow emission
             ▼
┌─────────────────────────────────────────┐
│ FeederTextToolbar observes change       │
│ - Receives new config via StateFlow     │
│ - Invalidates current menu (if shown)   │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ Next text selection shows new config    │
│ - Menu reflects changes                 │
└─────────────────────────────────────────┘
```

### 5.3 Third-Party App Discovery Flow

```
App Launch / Settings Screen Open
        │
        ▼
┌─────────────────────────────────────────┐
│ SettingsStore.init() or ViewModel.init()│
│ - Queries PackageManager for            │
│   ACTION_PROCESS_TEXT handlers          │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ PackageManager.queryIntentActivities()  │
│ - Returns ResolveInfo list              │
│ - Sorted by display name                │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ Merge with existing config              │
│ - Add new apps to config                │
│ - Mark uninstalled apps as unavailable  │
│ - Preserve user ordering for existing   │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ Persist merged config                   │
│ - Write to SharedPreferences           │
│ - Update StateFlow                     │
└─────────────────────────────────────────┘
```

### 5.4 Text Toolbar Integration Flow

```
User Selects Text in Article
        │
        ▼
┌─────────────────────────────────────────┐
│ SelectionContainer detects selection    │
│ - Requests toolbar via LocalTextToolbar │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ FeederTextToolbar.showMenu()            │
│ - Gets current config from StateFlow    │
│ - Creates ActionMode callback           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ FeederTextActionModeCallback.onCreateActionMode() │
│ - Builds menu from config               │
│ - Filters enabled items                 │
│ - Sorts by order                       │
│ - Adds to Menu object                   │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ System displays floating toolbar        │
│ - Shows configured items                │
│ - In configured order                   │
└─────────────────────────────────────────┘
```

---

## 6. State Management

### 6.1 State Machine: SelectionMenuSettingsViewModel

```
                    ┌──────────────────┐
                    │     Initial       │
                    │  (loading config) │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │      Idle        │
                    │ (waiting for input)│
                    └─────────┬─────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │  Updating    │ │  Dragging    │ │  Showing     │
    │  (toggling   │ │  (reordering)│ │  Reset Dialog│
    │   item)      │ │              │ │              │
    └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
           │                │                │
           │                │                │
           └────────────────┼────────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │      Idle        │
                    └──────────────────┘
```

**States**:
- **Initial**: Loading configuration on startup
- **Idle**: Waiting for user interaction
- **Updating**: Processing toggle or reorder operation
- **Dragging**: Active drag-and-drop in progress
- **Showing Reset Dialog**: Confirm reset dialog displayed

**Transitions**:
- Initial → Idle: Config loaded successfully
- Idle → Updating: User toggles item or reorders
- Updating → Idle: Update persisted
- Idle → Dragging: User initiates drag
- Dragging → Updating: Drag completes
- Dragging → Idle: Drag cancelled
- Idle → Showing Reset Dialog: User clicks reset
- Showing Reset Dialog → Idle: User cancels

### 6.2 State Machine: SelectionMenuItem

```
                    ┌──────────────────┐
                    │     Available    │
                    │  (enabled/disabled)│
                    └─────────┬─────────┘
                              │
                              │ App Uninstalled
                              ▼
                    ┌──────────────────┐
                    │   Unavailable    │
                    │ (not shown in UI) │
                    └─────────┬─────────┘
                              │
                              │ App Reinstalled
                              ▼
                    ┌──────────────────┐
                    │     Available    │
                    └──────────────────┘
```

**States**:
- **Available**: App installed and accessible
- **Unavailable**: App uninstalled (item hidden from UI)

**Sub-states for Available**:
- **Enabled**: Item appears in selection menu
- **Disabled**: Item hidden from selection menu

---

## 7. Interface Specifications

### 7.1 ViewModel Interface

```kotlin
interface ISelectionMenuSettingsViewModel {
    val viewState: StateFlow<SelectionMenuState>
    fun onEvent(event: SelectionMenuEvent)
    suspend fun toggleItem(itemId: String, enabled: Boolean)
    suspend fun reorderItems(fromIndex: Int, toIndex: Int)
    suspend fun resetToDefaults()
    suspend fun refreshThirdPartyApps()
}
```

**Contract**:
- `viewState`: Always emits a valid state (never null)
- `onEvent`: Returns immediately, processes asynchronously
- `toggleItem`: Persists change, emits new state
- `reorderItems`: Reorders list, persists, emits new state
- `resetToDefaults`: Restores defaults, persists, emits new state
- `refreshThirdPartyApps`: Queries PackageManager, merges with config

### 7.2 Repository Interface

```kotlin
interface ISelectionMenuRepository {
    val config: StateFlow<SelectionMenuConfig>
    suspend fun setConfig(config: SelectionMenuConfig)
    suspend fun getThirdPartyApps(): List<ThirdPartyApp>
    suspend fun mergeThirdPartyApps(apps: List<ThirdPartyApp>): SelectionMenuConfig
}
```

**Contract**:
- `config`: Emits config changes immediately upon persistence
- `setConfig`: Serializes to JSON, writes to SharedPreferences
- `getThirdPartyApps`: Queries PackageManager, returns current handlers
- `mergeThirdPartyApps`: Adds new apps, marks unavailable as disabled

### 7.3 Data Model Interface

```kotlin
@Serializable
data class SelectionMenuItem(
    val id: String,
    val type: ItemType,
    val packageName: String? = null,
    val className: String? = null,
    val enabled: Boolean = true,
    val order: Int,
) {
    fun isValid(): Boolean
    fun isAvailable(context: Context): Boolean
}

@Serializable
enum class ItemType {
    SYSTEM,
    THIRD_PARTY,
}
```

**Contract**:
- `isValid`: Returns true if required fields are present
- `isAvailable`: For THIRD_PARTY, checks if app is installed

### 7.4 Drag-and-Drop Interface

```kotlin
interface IDragHandler {
    fun onStartDrag(item: SelectionMenuItem)
    fun onDrag(offset: Offset)
    fun onEndDrag()
    fun onMove(fromIndex: Int, toIndex: Int)
}
```

**Contract**:
- `onStartDrag`: Initiates drag state, shows visual feedback
- `onDrag`: Updates visual position during drag
- `onEndDrag`: Finalizes or cancels drag
- `onMove`: Commits position change

---

## 8. Architecture Decision Records

See separate ADR files in `/adrs/` directory:

- [ADR-001: Configuration Persistence Strategy](adrs/ADR-001-persistence-strategy.md)
- [ADR-002: Drag-and-Drop Implementation Approach](adrs/ADR-002-drag-drop-implementation.md)
- [ADR-003: Real-Time Configuration Updates](adrs/ADR-003-realtime-updates.md)
- [ADR-004: Third-Party App Discovery Strategy](adrs/ADR-004-third-party-discovery.md)

---

## 9. Integration Strategy

### 9.1 Minimal Integration Approach

**Principle**: Modify existing components minimally to achieve feature goals.

**Integration Points**:

1. **SettingsStore** (New Method):
```kotlin
// Add to SettingsStore.kt
private val _selectionMenuConfig = MutableStateFlow(loadSelectionMenuConfig())
val selectionMenuConfig: StateFlow<SelectionMenuConfig> = _selectionMenuConfig.asStateFlow()

fun setSelectionMenuConfig(config: SelectionMenuConfig) {
    _selectionMenuConfig.value = config
    val jsonString = json.encodeToString(config)
    sp.edit().putString(PREF_SELECTION_MENU_CONFIG, jsonString).apply()
}
```

2. **FeederTextToolbar** (Constructor Parameter):
```kotlin
// Modify constructor signature
class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    selectionMenuConfig: StateFlow<SelectionMenuConfig>, // ADD
) : TextToolbar {
    // Observe config changes
    private val config by selectionMenuConfig.collectAsState()

    // Pass config to callback
    private val textActionModeCallback: FeederTextActionModeCallback =
        FeederTextActionModeCallback(
            context = view.context,
            activityLauncher = activityLauncher,
            selectionMenuConfig = config.value, // PASS
            onActionModeDestroy = { actionMode = null },
        )
}
```

3. **Navigation** (New Destination):
```kotlin
// Add to NavigationDestinations.kt
data object SelectionMenuSettingsDestination : NavigationDestination(
    path = "settings/selection_menu",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    override fun register(
        navGraphBuilder: NavGraphBuilder,
        navController: NavController,
        activityResultCaller: ActivityResultCaller,
    ) {
        navGraphBuilder.composable(route = route) {
            val viewModel: SelectionMenuSettingsViewModel = diAwareViewModel()
            SelectionMenuSettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                viewModel = viewModel,
            )
        }
    }
}

// Register in NavGraph
SelectionMenuSettingsDestination.register(navGraphBuilder, navController, activityResultCaller)
```

4. **TextSettingsScreen** (Navigation Entry):
```kotlin
// Add to TextSettingsContent
MenuSetting(
    title = stringResource(R.string.selection_menu),
    currentValue = null, // Navigation indicator
    values = ImmutableHolder(listOf(NavigationIndicator)),
    onSelection = {
        onNavigateToSelectionMenu.invoke()
    },
    icon = null,
)
```

### 9.2 Backward Compatibility

**Configuration Migration**:
- On first launch, create default config
- Preserve existing behavior: all system items enabled, all third-party shown
- No data loss for users

**Graceful Degradation**:
- If config corrupted, recreate defaults
- If all items disabled, enable Copy by default
- If third-party app missing, skip silently

### 9.3 Future Extensibility

**Extension Points**:
1. **Custom Items**: Data model supports adding custom item types
2. **Per-Feed Config**: Can extend to support feed-specific configs
3. **Item Groups**: Can add grouping/hierarchy in future
4. **Item Icons**: Can add custom icons per item

---

## 10. Error Handling & Resilience

### 10.1 Error Scenarios

| Error | Detection | Recovery | User Impact |
|-------|-----------|----------|-------------|
| Config corrupted | JSON parse exception | Recreate defaults | Low (settings reset) |
| All items disabled | Validation on save | Auto-enable Copy | None (prevented) |
| Third-party app missing | PackageManager check | Skip item | None (item hidden) |
| Out-of-order indices | Range check | Clamp to valid range | None |
| Duplicate item IDs | Validation on save | Remove duplicates | Low (deduplication) |

### 10.2 Error Handling Strategies

**Strategy 1: Validate on Load**
```kotlin
private fun loadSelectionMenuConfig(): SelectionMenuConfig {
    val jsonString = sp.getString(PREF_SELECTION_MENU_CONFIG, null)
    if (jsonString != null) {
        return try {
            val config = json.decodeFromString<SelectionMenuConfig>(jsonString)
            val validationResult = config.validate()
            if (validationResult.isValid) {
                config
            } else {
                Log.w(TAG, "Invalid config: ${validationResult.errors}")
                createDefaultConfig()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config", e)
            createDefaultConfig()
        }
    }
    return createDefaultConfig()
}
```

**Strategy 2: Validate on Save**
```kotlin
fun setSelectionMenuConfig(config: SelectionMenuConfig) {
    val validationResult = config.validate()
    if (!validationResult.isValid) {
        throw ValidationException(validationResult.errors)
    }

    _selectionMenuConfig.value = config
    val jsonString = json.encodeToString(config)
    sp.edit().putString(PREF_SELECTION_MENU_CONFIG, jsonString).apply()
}
```

**Strategy 3: Graceful Missing App Handling**
```kotlin
private fun addThirdPartyMenuItem(menu: Menu?, item: SelectionMenuItem) {
    if (!item.isAvailable(context)) {
        Log.d(TAG, "Third-party app not available: ${item.packageName}")
        return
    }

    // Add menu item...
}
```

### 10.3 Resilience Patterns

**Circuit Breaker**: If config fails to load 3 times, disable feature temporarily

**Retry**: On third-party discovery failure, retry once on next settings open

**Fallback**: Always have default config ready as fallback

**Logging**: Log all errors with sufficient context for debugging

---

## 11. Security Considerations

### 11.1 Threat Model

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|------------|
| Malicious third-party app | Low | Medium | Android sandbox, user approval |
| Config injection | Low | Low | JSON serialization, validation |
| Data leak | Low | Low | No sensitive data in config |
| DoS via invalid config | Low | Low | Validation limits |

### 11.2 Security Measures

**Data Validation**:
- Validate all JSON before deserialization
- Sanitize third-party app package names
- Limit config size (max 100 items)

**Permission Handling**:
- No new permissions required
- Uses existing PackageManager access

**Privacy**:
- No tracking of menu usage
- No analytics for selection patterns

---

## 12. Performance Analysis

### 12.1 Performance Targets

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Config load | <50ms | Time from init to first state emission |
| Config save | <100ms | Time from user action to persistence |
| Menu build | <50ms | Time from onCreateActionMode to menu ready |
| Toolbar display | <100ms | Time from text selection to toolbar visible |
| Drag-and-drop | 60fps | Frame time during drag operations |

### 12.2 Performance Optimization

**Strategy 1: Lazy Loading**
- Third-party apps discovered only when settings opened
- Config loaded on ViewModel init, not app start

**Strategy 2: Caching**
- Cache third-party app list, refresh on settings open
- Cache serialized config, re-serialize only on change

**Strategy 3: Efficient UI**
- Use `key()` modifier for LazyColumn items
- Use `derivedStateOf()` for computed values
- Minimize recomposition during drag

### 12.3 Memory Considerations

**Config Size**:
- Typical: 10-20 items, ~1KB JSON
- Maximum: 100 items, ~5KB JSON
- Negligible memory impact

**Third-Party Cache**:
- Typical: 5-10 apps, ~500 bytes each
- Total: ~5KB in memory

---

## 13. Testing Strategy

### 13.1 Unit Tests

**Data Model Tests**:
```kotlin
class SelectionMenuItemTest {
    @Test
    fun `isValid returns true for valid system item`() {}
    @Test
    fun `isValid returns false for item without ID`() {}
    @Test
    fun `isAvailable returns false for uninstalled third-party app`() {}
}

class SelectionMenuConfigTest {
    @Test
    fun `validate returns error when no items enabled`() {}
    @Test
    fun `validate returns error when duplicate IDs exist`() {}
    @Test
    fun `mergeThirdPartyApps adds new apps to config`() {}
}
```

**Repository Tests**:
```kotlin
class SelectionMenuRepositoryTest {
    @Test
    fun `loadConfig returns default when pref empty`() {}
    @Test
    fun `saveConfig writes JSON to SharedPreferences`() {}
    @Test
    fun `getThirdPartyApps queries PackageManager`() {}
}
```

**ViewModel Tests**:
```kotlin
class SelectionMenuSettingsViewModelTest {
    @Test
    fun `onEvent ToggleItem updates config`() {}
    @Test
    fun `onEvent ReorderItems changes item order`() {}
    @Test
    fun `resetToDefaults restores initial config`() {}
}
```

### 13.2 Integration Tests

**Toolbar Integration**:
```kotlin
class FeederTextToolbarIntegrationTest {
    @Test
    fun `toolbar shows only enabled items`() {}
    @Test
    fun `toolbar respects item order from config`() {}
    @Test
    fun `toolbar updates when config changes`() {}
}
```

### 13.3 UI Tests

**Compose UI Tests**:
```kotlin
class SelectionMenuSettingsScreenTest {
    @Test
    fun `screen displays all config items`() {}
    @Test
    fun `toggle switch updates item enabled state`() {}
    @Test
    fun `drag-and-drop reorders items`() {}
}
```

### 13.4 Manual Testing Checklist

- [ ] Settings screen opens without error
- [ ] All system items displayed
- [ ] Third-party apps discovered and displayed
- [ ] Toggle switches enable/disable items
- [ ] Drag-and-drop reorders items
- [ ] Reset to defaults works
- [ ] Text selection menu reflects config
- [ ] Changes persist across app restart
- [ ] Missing third-party apps handled gracefully
- [ ] All items disabled prevented

---

## 14. Deployment & Migration

### 14.1 Migration Strategy

**Version 1 → Version 2 (Future)**:
- Add `version` field to config
- On load, check version
- Apply migration logic if needed
- Save migrated config

**Example**:
```kotlin
private fun migrateConfig(config: SelectionMenuConfig): SelectionMenuConfig {
    return when (config.version) {
        1 -> migrateToV2(config)
        else -> config
    }
}

private fun migrateToV2(config: SelectionMenuConfig): SelectionMenuConfig {
    // Apply V2 changes
    return config.copy(version = 2)
}
```

### 14.2 Rollback Plan

**If Critical Bug Found**:
1. Revert to previous hardcoded menu behavior
2. Disable config reading in FeederTextToolbar
3. Show default menu (all items enabled)

**Rollback Trigger**:
- Crash rate >1% related to config
- User reports of broken text selection
- Performance regression >50ms in menu display

---

## 15. Risk Assessment

### 15.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Drag-and-drop performance issues | Medium | Medium | Test on low-end devices, optimize animations |
| Third-party app compatibility | Low | Low | Graceful handling, skip missing apps |
| Config corruption | Low | Medium | Validation, recreate defaults |
| Breaking existing behavior | Low | High | Extensive testing, backward compatibility |

### 15.2 User Experience Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Confusing drag-and-drop | Medium | Medium | Clear visual feedback, help text |
| Accidentally disabling all items | Low | High | Validation, prevent saving |
| Too many items overwhelming | Low | Medium | Grouping (future enhancement) |

### 15.3 Development Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Underestimated complexity | Medium | High | Incremental development, testing |
| Integration issues | Low | Medium | Follow existing patterns, minimal changes |
| Time overrun | Medium | Medium | MVP approach, defer nice-to-haves |

---

## 16. Success Criteria

### 16.1 Functional Success

- [ ] Users can customize selection menu via settings
- [ ] Users can reorder items via drag-and-drop
- [ ] Users can enable/disable items via toggles
- [ ] Configuration persists across app restarts
- [ ] Text selection menu reflects configuration
- [ ] Third-party apps discovered and manageable
- [ ] Missing apps handled gracefully

### 16.2 Non-Functional Success

- [ ] Settings changes apply in <100ms
- [ ] Drag-and-drop maintains 60fps
- [ ] Menu displays in <100ms after text selection
- [ ] No crashes in production (>99.9% stability)
- [ ] Memory increase <5MB
- [ ] APK size increase <100KB

### 16.3 User Experience Success

- [ ] Settings screen intuitive and discoverable
- [ ] Drag-and-drop feels natural
- [ ] Defaults match user expectations
- [ ] Error messages clear and helpful
- [ ] Accessibility support complete

---

## Appendix A: Glossary

- **ActionMode**: Android API for contextual action bars
- **StateFlow**: Kotlin Flow for emitting state updates
- **ComposeProviders**: Compose composition local providers
- **SelectionContainer**: Compose text selection wrapper
- **ACTION_PROCESS_TEXT**: Android intent for text processing apps
- **Kodein DI**: Dependency injection framework used in Feeder

---

## Appendix B: References

1. FeederTextToolbar.kt (existing implementation)
2. SettingsStore.kt (persistence pattern)
3. TextSettingsViewModel.kt (ViewModel pattern)
4. NavigationDestinations.kt (navigation pattern)
5. Android TextToolbar API documentation
6. Moon+ Reader (reference application)
7. Jetpack Compose Drag-and-Drop documentation

---

**Document Status**: Draft
**Next Review**: After Phase 5.5 (UI/UX Design)
**Approvals**: Pending

---

**End of Architecture Design Document**
