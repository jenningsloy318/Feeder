# Technical Specification: Text Selection Menu Settings

**Date:** 2025-11-27 22:25:00 PST
**Phase:** 6 - Specification Writing
**Status:** Draft

## Document Overview

This document provides the complete technical specification for implementing text selection menu settings in the Feeder Android application. This feature allows users to customize which actions appear in the text selection menu and reorder them using drag-and-drop.

**Prerequisites:**
- [01-requirements.md](./01-requirements.md) - User requirements and acceptance criteria
- [02-research-report.md](./02-research-report.md) - Technology research and patterns
- [03-code-assessment.md](./03-code-assessment.md) - Codebase analysis
- [04-ui-ux-design.md](./04-ui-ux-design.md) - Visual and interaction design

## Executive Summary

### Feature Description

Add a new settings screen that allows users to:
1. **Enable/disable** text selection menu items (Copy, Select All, text processors)
2. **Reorder** items using drag-and-drop interaction
3. **Reset** to default configuration
4. **Persist** configuration across app restarts

### Key Technology Decisions

| Component | Technology | Justification |
|-----------|-----------|---------------|
| UI Framework | Jetpack Compose | Consistent with existing app |
| Drag-and-Drop | `sh.calvin.reorderable:2.4.0` | Material 3 integration, accessibility, haptic feedback |
| State Management | ViewModel + StateFlow | Existing pattern in app |
| Data Persistence | SharedPreferences (JSON) | Simple, lightweight, fits use case |
| Architecture | MVVM | Consistent with existing screens |

### Scope

**In Scope:**
- Settings screen UI with drag-and-drop reordering
- Enable/disable toggle for each menu item
- Reset to defaults functionality
- Configuration persistence
- Text processor discovery and integration
- Accessibility support (TalkBack, keyboard)
- Haptic feedback

**Out of Scope:**
- Adding custom menu items (auto-discovered only)
- Text processor installation/uninstallation
- Menu item customization beyond enable/disable
- Cloud sync of settings

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────┐
│                  UI Layer (Compose)                  │
├─────────────────────────────────────────────────────┤
│                                                       │
│  TextSelectionMenuSettingsScreen                    │
│    │                                                  │
│    ├─ SensibleTopAppBar (Reset button)              │
│    └─ LazyColumn (Reorderable)                      │
│         └─ MenuItemCard (Drag handle + Switch)      │
│                                                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    │ collectAsStateWithLifecycle
                    ▼
┌─────────────────────────────────────────────────────┐
│               ViewModel Layer                        │
├─────────────────────────────────────────────────────┤
│                                                       │
│  TextSelectionMenuSettingsViewModel                 │
│    │                                                  │
│    ├─ State: StateFlow<MenuConfigState>            │
│    │                                                  │
│    ├─ Actions:                                       │
│    │   ├─ moveItem(fromIndex, toIndex)              │
│    │   ├─ toggleItemEnabled(id)                      │
│    │   └─ resetToDefaults()                          │
│    │                                                  │
│    └─ Dependencies:                                  │
│         ├─ TextSelectionMenuConfigRepository        │
│         └─ PackageManager (text processor discovery)│
│                                                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    │ Repository Pattern
                    ▼
┌─────────────────────────────────────────────────────┐
│               Data Layer                             │
├─────────────────────────────────────────────────────┤
│                                                       │
│  TextSelectionMenuConfigRepository                  │
│    │                                                  │
│    ├─ loadConfiguration(): MenuConfiguration        │
│    ├─ saveConfiguration(config)                      │
│    └─ getDefaultConfiguration(): MenuConfiguration  │
│                                                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    │ SharedPreferences (JSON)
                    ▼
┌─────────────────────────────────────────────────────┐
│           Data Storage (SharedPreferences)          │
├─────────────────────────────────────────────────────┤
│                                                       │
│  Key: "text_selection_menu_config"                  │
│  Format: JSON                                        │
│                                                       │
│  {                                                    │
│    "version": 1,                                      │
│    "items": [                                         │
│      {"id": "copy", "order": 0, "enabled": true},   │
│      {"id": "select_all", "order": 1, "enabled": true}│
│    ]                                                  │
│  }                                                    │
│                                                       │
└─────────────────────────────────────────────────────┘
```

### Integration Points

#### 1. Settings Screen Integration

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsScreen.kt`

**Change**: Add new `ExternalSetting` after "Text Settings":

```kotlin
ExternalSetting(
    title = stringResource(R.string.text_selection_menu_settings),
    currentValue = stringResource(
        R.string.text_selection_menu_items_configured,
        menuItemCount
    ),
    onClick = {
        navController.navigate("textSelectionMenuSettings")
    }
)
```

#### 2. Navigation Graph

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/FeederNavGraph.kt`

**Change**: Add new route:

```kotlin
composable("textSelectionMenuSettings") {
    TextSelectionMenuSettingsScreen(
        onNavigateUp = { navController.navigateUp() }
    )
}
```

#### 3. Text Selection Menu Builder

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/SelectionMenu.kt`

**Change**: Modify `buildTextSelectionMenu()` to read configuration:

```kotlin
private fun buildTextSelectionMenu(
    context: Context,
    config: MenuConfiguration
): List<SelectionMenuItem> {
    val items = mutableListOf<SelectionMenuItem>()

    // Build items according to config.items order and enabled state
    config.items
        .sortedBy { it.order }
        .forEach { configItem ->
            val menuItem = when (configItem.id) {
                "copy" -> createCopyMenuItem()
                "select_all" -> createSelectAllMenuItem()
                else -> createTextProcessorMenuItem(configItem.id, context)
            }

            menuItem.isEnabled = configItem.enabled
            items.add(menuItem)
        }

    return items
}
```

## Data Models

### MenuConfiguration

```kotlin
/**
 * Represents the complete text selection menu configuration.
 */
data class MenuConfiguration(
    val version: Int = 1,
    val items: List<MenuItemConfig>
) {
    companion object {
        /**
         * Default configuration with Copy and Select All.
         */
        fun default() = MenuConfiguration(
            version = 1,
            items = listOf(
                MenuItemConfig("copy", order = 0, enabled = true),
                MenuItemConfig("select_all", order = 1, enabled = true)
            )
        )
    }
}

/**
 * Configuration for a single menu item.
 */
data class MenuItemConfig(
    val id: String,           // "copy", "select_all", or text processor package name
    val order: Int,           // Display order (0-based)
    val enabled: Boolean      // Whether item appears in menu
)
```

### MenuItemUiModel

```kotlin
/**
 * UI model for displaying menu item in settings.
 */
data class MenuItemUiModel(
    val id: String,
    val title: String,              // e.g., "Copy", "Translate"
    val subtitle: String? = null,   // e.g., "com.google.android.apps.translate"
    val order: Int,
    val enabled: Boolean,
    val type: MenuItemType
)

enum class MenuItemType {
    BUILT_IN,        // Copy, Select All
    TEXT_PROCESSOR   // Third-party text processors
}
```

### MenuConfigState

```kotlin
/**
 * UI state for settings screen.
 */
data class MenuConfigState(
    val items: List<MenuItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val showResetDialog: Boolean = false
)
```

## Implementation Details

### Phase 1: Data Layer

#### 1.1 Repository Implementation

**File**: `app/src/main/java/com/nononsenseapps/feeder/model/TextSelectionMenuConfigRepository.kt`

**Responsibilities:**
- Load configuration from SharedPreferences
- Save configuration to SharedPreferences
- Provide default configuration
- Handle JSON serialization/deserialization

**Dependencies:**
- `SharedPreferences`
- `Gson` or `kotlinx.serialization` (use existing serialization in app)

**Key Methods:**

```kotlin
class TextSelectionMenuConfigRepository(
    private val prefs: SharedPreferences,
    private val gson: Gson
) {
    private val configKey = "text_selection_menu_config"

    suspend fun loadConfiguration(): MenuConfiguration {
        return withContext(Dispatchers.IO) {
            val json = prefs.getString(configKey, null)
            if (json != null) {
                try {
                    gson.fromJson(json, MenuConfiguration::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse config, using default", e)
                    getDefaultConfiguration()
                }
            } else {
                getDefaultConfiguration()
            }
        }
    }

    suspend fun saveConfiguration(config: MenuConfiguration) {
        withContext(Dispatchers.IO) {
            val json = gson.toJson(config)
            prefs.edit {
                putString(configKey, json)
            }
        }
    }

    fun getDefaultConfiguration(): MenuConfiguration {
        return MenuConfiguration.default()
    }

    companion object {
        private const val TAG = "TextSelectionMenuConfigRepo"
    }
}
```

#### 1.2 Text Processor Discovery

**Location**: Repository or dedicated helper class

**Method**:

```kotlin
/**
 * Discovers installed text processors via PackageManager.
 * Called on first text selection or manually in settings.
 */
fun discoverTextProcessors(context: Context): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
    }

    return context.packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
}

/**
 * Converts ResolveInfo to MenuItemConfig with next available order.
 */
fun createConfigFromTextProcessor(
    resolveInfo: ResolveInfo,
    nextOrder: Int,
    packageManager: PackageManager
): MenuItemConfig {
    return MenuItemConfig(
        id = resolveInfo.activityInfo.packageName,
        order = nextOrder,
        enabled = true
    )
}
```

### Phase 2: ViewModel Layer

#### 2.1 ViewModel Implementation

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsViewModel.kt`

**Responsibilities:**
- Load initial configuration
- Handle user actions (move, toggle, reset)
- Persist changes
- Expose UI state

**Dependencies:**
- `TextSelectionMenuConfigRepository`
- `PackageManager`
- `Application` context (for PackageManager access)

**Implementation:**

```kotlin
@HiltViewModel
class TextSelectionMenuSettingsViewModel @Inject constructor(
    private val repository: TextSelectionMenuConfigRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuConfigState())
    val uiState: StateFlow<MenuConfigState> = _uiState.asStateFlow()

    private var currentConfig: MenuConfiguration = MenuConfiguration.default()

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            currentConfig = repository.loadConfiguration()

            // Discover text processors and merge with config
            val textProcessors = discoverTextProcessors(application)
            currentConfig = mergeTextProcessors(currentConfig, textProcessors)

            // Convert to UI models
            val uiItems = currentConfig.items
                .sortedBy { it.order }
                .map { configToUiModel(it) }

            _uiState.update {
                it.copy(
                    items = uiItems,
                    isLoading = false
                )
            }
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val items = _uiState.value.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        // Update order
        val updatedItems = items.mapIndexed { index, menuItem ->
            menuItem.copy(order = index)
        }

        _uiState.update { it.copy(items = updatedItems) }

        // Persist immediately
        persistCurrentState()
    }

    fun toggleItemEnabled(id: String) {
        val items = _uiState.value.items.map { item ->
            if (item.id == id) {
                item.copy(enabled = !item.enabled)
            } else {
                item
            }
        }

        _uiState.update { it.copy(items = items) }

        // Persist immediately
        persistCurrentState()
    }

    fun showResetDialog() {
        _uiState.update { it.copy(showResetDialog = true) }
    }

    fun hideResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            currentConfig = repository.getDefaultConfiguration()

            // Rediscover text processors
            val textProcessors = discoverTextProcessors(application)
            currentConfig = mergeTextProcessors(currentConfig, textProcessors)

            repository.saveConfiguration(currentConfig)

            val uiItems = currentConfig.items
                .sortedBy { it.order }
                .map { configToUiModel(it) }

            _uiState.update {
                it.copy(
                    items = uiItems,
                    showResetDialog = false
                )
            }
        }
    }

    private fun persistCurrentState() {
        viewModelScope.launch {
            val config = MenuConfiguration(
                version = 1,
                items = _uiState.value.items.map { uiModel ->
                    MenuItemConfig(
                        id = uiModel.id,
                        order = uiModel.order,
                        enabled = uiModel.enabled
                    )
                }
            )

            repository.saveConfiguration(config)
        }
    }

    private fun configToUiModel(config: MenuItemConfig): MenuItemUiModel {
        return when (config.id) {
            "copy" -> MenuItemUiModel(
                id = "copy",
                title = application.getString(R.string.copy),
                subtitle = null,
                order = config.order,
                enabled = config.enabled,
                type = MenuItemType.BUILT_IN
            )
            "select_all" -> MenuItemUiModel(
                id = "select_all",
                title = application.getString(R.string.select_all),
                subtitle = null,
                order = config.order,
                enabled = config.enabled,
                type = MenuItemType.BUILT_IN
            )
            else -> {
                // Text processor - get label from PackageManager
                val pm = application.packageManager
                val label = try {
                    val appInfo = pm.getApplicationInfo(config.id, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    config.id // Fallback to package name
                }

                MenuItemUiModel(
                    id = config.id,
                    title = label,
                    subtitle = config.id,
                    order = config.order,
                    enabled = config.enabled,
                    type = MenuItemType.TEXT_PROCESSOR
                )
            }
        }
    }

    private fun mergeTextProcessors(
        config: MenuConfiguration,
        textProcessors: List<ResolveInfo>
    ): MenuConfiguration {
        val existingIds = config.items.map { it.id }.toSet()
        val nextOrder = (config.items.maxOfOrNull { it.order } ?: -1) + 1

        val newItems = config.items.toMutableList()

        textProcessors.forEachIndexed { index, resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName !in existingIds) {
                newItems.add(
                    MenuItemConfig(
                        id = packageName,
                        order = nextOrder + index,
                        enabled = true
                    )
                )
            }
        }

        return config.copy(items = newItems)
    }
}
```

### Phase 3: UI Layer

#### 3.1 Settings Screen

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsScreen.kt`

**Responsibilities:**
- Display list of menu items with drag-and-drop
- Handle user interactions
- Show reset confirmation dialog

**Dependencies:**
- `sh.calvin.reorderable:reorderable:2.4.0`
- Material 3 Compose components

**Implementation:**

```kotlin
@Composable
fun TextSelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TextSelectionMenuSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            SensibleTopAppBar(
                title = stringResource(R.string.text_selection_menu_settings),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showResetDialog() }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.reset_to_defaults)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MenuItemList(
                items = uiState.items,
                onMoveItem = viewModel::moveItem,
                onToggleEnabled = viewModel::toggleItemEnabled,
                hapticFeedback = hapticFeedback,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (uiState.showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = viewModel::resetToDefaults,
            onDismiss = viewModel::hideResetDialog
        )
    }
}

@Composable
private fun MenuItemList(
    items: List<MenuItemUiModel>,
    onMoveItem: (Int, Int) -> Unit,
    onToggleEnabled: (String) -> Unit,
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveItem(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        state = lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                MenuItemCard(
                    item = item,
                    isDragging = isDragging,
                    onToggleEnabled = { onToggleEnabled(item.id) },
                    hapticFeedback = hapticFeedback
                )
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItemUiModel,
    isDragging: Boolean,
    onToggleEnabled: () -> Unit,
    hapticFeedback: HapticFeedback
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 4.dp else 1.dp,
        label = "card_elevation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.drag_to_reorder),
                modifier = Modifier
                    .draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.GestureThresholdActivate
                            )
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.GestureEnd
                            )
                        },
                        interactionSource = interactionSource
                    ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Enable/disable switch
            Switch(
                checked = item.enabled,
                onCheckedChange = { onToggleEnabled() }
            )
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.reset_to_defaults))
        },
        text = {
            Text(stringResource(R.string.reset_text_selection_menu_confirmation))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
```

#### 3.2 String Resources

**File**: `app/src/main/res/values/strings.xml`

**Add:**

```xml
<string name="text_selection_menu_settings">Text Selection Menu Settings</string>
<string name="text_selection_menu_items_configured">%d items configured</string>
<string name="drag_to_reorder">Drag to reorder</string>
<string name="reset_to_defaults">Reset to defaults</string>
<string name="reset_text_selection_menu_confirmation">Reset text selection menu to default settings? This will restore the default order and enable all items.</string>
<string name="reset">Reset</string>
<string name="copy">Copy</string>
<string name="select_all">Select All</string>
```

### Phase 4: Integration

#### 4.1 Add Dependency

**File**: `app/build.gradle.kts`

**Add to dependencies block:**

```kotlin
dependencies {
    // ... existing dependencies ...

    // Drag-and-drop reordering
    implementation("sh.calvin.reorderable:reorderable:2.4.0")
}
```

#### 4.2 Modify Text Selection Menu Builder

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/SelectionMenu.kt`

**Changes:**

1. Inject `TextSelectionMenuConfigRepository`
2. Load configuration before building menu
3. Apply order and enabled state to menu items

## Testing Strategy

### Unit Tests

#### Repository Tests

**File**: `TextSelectionMenuConfigRepositoryTest.kt`

**Test Cases:**
- ✅ Load configuration when none exists returns default
- ✅ Load configuration when exists returns saved config
- ✅ Save configuration persists to SharedPreferences
- ✅ Save and load round-trip preserves data
- ✅ Load configuration with invalid JSON returns default
- ✅ Default configuration has Copy and Select All

#### ViewModel Tests

**File**: `TextSelectionMenuSettingsViewModelTest.kt`

**Test Cases:**
- ✅ Initial state loads configuration from repository
- ✅ moveItem updates order correctly
- ✅ moveItem persists changes
- ✅ toggleItemEnabled updates enabled state
- ✅ toggleItemEnabled persists changes
- ✅ resetToDefaults restores default configuration
- ✅ resetToDefaults closes dialog
- ✅ Text processors are discovered and merged

### Integration Tests

#### UI Tests

**File**: `TextSelectionMenuSettingsScreenTest.kt`

**Test Cases:**
- ✅ Screen displays list of menu items
- ✅ Drag handle is visible on each item
- ✅ Switch reflects enabled state
- ✅ Tapping switch toggles enabled state
- ✅ Reset button shows confirmation dialog
- ✅ Reset confirmation applies defaults
- ✅ Cancel button dismisses dialog without changes
- ✅ Back navigation works

#### End-to-End Tests

**Scenarios:**
- ✅ User reorders items using drag-and-drop
- ✅ User disables an item, it appears grayed in text selection menu
- ✅ User resets to defaults, configuration reverts
- ✅ Configuration persists across app restarts
- ✅ Newly installed text processor appears in list

### Manual Testing Checklist

See [04-ui-ux-design.md](./04-ui-ux-design.md#testing-checklist) for comprehensive manual testing checklist.

## Performance Considerations

### Optimization Strategies

1. **Lazy Loading**: Only discover text processors when needed
2. **Caching**: Cache PackageManager queries for text processor labels
3. **Immediate Persistence**: Save configuration on each change (no batching needed due to small data size)
4. **Efficient Reordering**: Use library's built-in optimization for list updates

### Performance Targets

- **Configuration Load**: < 50ms
- **Text Processor Discovery**: < 100ms
- **Drag Interaction**: 60 FPS (no jank)
- **Configuration Save**: < 10ms

## Security & Privacy

### Data Sensitivity

- **Sensitivity Level**: Low
- **Data Type**: User preferences (menu order and enabled state)
- **Storage**: Local only (SharedPreferences)

### Security Considerations

1. **No network transmission**: Configuration never leaves device
2. **No PII**: No personally identifiable information collected
3. **Sandboxed**: SharedPreferences protected by Android app sandbox
4. **No third-party analytics**: Configuration not tracked

## Accessibility

### Requirements

1. **TalkBack Support**:
   - Drag handle announces as "Drag to reorder, button"
   - Position changes announced during drag
   - Switch state announced automatically

2. **Keyboard Navigation** (external keyboard):
   - Tab to navigate between elements
   - Space/Enter to activate drag
   - Arrow keys to move items
   - Escape to cancel drag

3. **Content Descriptions**:
   - All interactive elements have content descriptions
   - Semantic roles properly assigned

4. **High Contrast**:
   - Sufficient color contrast for all text
   - Visual distinction for disabled states

See [04-ui-ux-design.md](./04-ui-ux-design.md#accessibility) for detailed accessibility specifications.

## Error Handling

### Error Scenarios

| Scenario | Handling |
|----------|----------|
| Configuration load fails | Use default configuration, log error |
| Configuration save fails | Show error toast, retry on next change |
| Text processor discovery fails | Show only built-in items, log error |
| PackageManager query fails | Use package name as fallback label |
| Invalid JSON in SharedPreferences | Use default configuration, log error |

### Logging Strategy

- **Error Level**: Failed operations (save/load failures)
- **Warning Level**: Fallback scenarios (invalid JSON, missing labels)
- **Debug Level**: Normal operations (for troubleshooting)

**Log Tag**: `TextSelectionMenuConfig`

## Backward Compatibility

### Migration Strategy

**Version 1** (Initial):
- No migration needed (first version)
- Missing configuration = use defaults

**Future Versions**:
- Add `version` field to configuration
- Implement migration logic in repository
- Preserve user preferences when possible

### Upgrade Path

No breaking changes in initial release. Future changes will maintain backward compatibility by:
1. Preserving unknown menu items
2. Migrating order/enabled state
3. Falling back to defaults if migration fails

## Localization

### Localizable Strings

All user-facing strings defined in `strings.xml`:
- Screen title
- Reset confirmation dialog
- Drag handle content description
- Built-in item labels (Copy, Select All)

### Text Processor Labels

Labels from third-party text processors use `PackageManager.getApplicationLabel()`, which respects the app's locale.

## Future Enhancements

### Potential Features (Out of Scope for v1)

1. **Custom Menu Items**: Allow users to add custom intents
2. **Item Icons**: Display icons for text processors
3. **Search/Filter**: Search menu items (if list grows large)
4. **Import/Export**: Share configuration between devices
5. **Cloud Sync**: Sync configuration via user's sync provider
6. **Advanced Settings**: Configure intent filters, custom labels

### Technical Debt

None anticipated for initial implementation.

## Dependencies

### Required Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| sh.calvin.reorderable | 2.4.0 | Drag-and-drop reordering |
| androidx.compose.material3 | (existing) | UI components |
| androidx.hilt | (existing) | Dependency injection |
| androidx.lifecycle | (existing) | ViewModel, StateFlow |
| kotlinx.coroutines | (existing) | Async operations |
| gson | (existing) | JSON serialization |

### Version Constraints

- Minimum SDK: 24 (Android 7.0) - matches app minimum
- Target SDK: 34 (Android 14) - matches app target
- Kotlin: 1.9.x - matches app version

## Deployment

### Release Checklist

- [ ] Unit tests passing (repository, ViewModel)
- [ ] Integration tests passing (UI, end-to-end)
- [ ] Manual testing completed (all scenarios)
- [ ] Accessibility tested with TalkBack
- [ ] Localization verified for all supported languages
- [ ] Code review completed
- [ ] Documentation updated
- [ ] Release notes written

### Rollout Strategy

**Phase 1**: Internal testing (1-2 days)
- Deploy to internal testers
- Verify core functionality
- Check for crashes/ANRs

**Phase 2**: Beta release (1 week)
- Release to beta testers
- Monitor crash reports
- Gather user feedback

**Phase 3**: Production rollout (staged)
- 10% rollout: Day 1
- 50% rollout: Day 3
- 100% rollout: Day 7

### Monitoring

**Key Metrics:**
- Crash-free sessions (target: > 99.5%)
- ANR rate (target: < 0.1%)
- Feature adoption (% of users who access settings)
- Configuration save failures (target: < 0.01%)

## Conclusion

This specification provides a complete blueprint for implementing text selection menu settings with drag-and-drop reordering. The design leverages well-maintained libraries (`sh.calvin.reorderable`), follows Material Design 3 principles, and integrates seamlessly with the existing Feeder codebase.

**Key Success Factors:**
1. ✅ Modern, intuitive drag-and-drop UX
2. ✅ Full accessibility support (TalkBack, keyboard)
3. ✅ Consistent with existing app patterns
4. ✅ Minimal dependencies (1 new library)
5. ✅ Comprehensive testing strategy
6. ✅ Clear error handling and recovery

**Next Steps:**
1. Review specification (Phase 7)
2. Begin implementation (Phase 8)
3. Concurrent QA and documentation (Phase 9)
4. Deploy to production (Phase 10-12)
