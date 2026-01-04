# Technical Specification: Global Menu Item Discovery and Display

**Version**: 2.0 (Updated for Moon+ Reader Pattern)
**Date**: 2026-01-04
**Status**: Updated for User Confirmation
**Spec Index**: 016

---

## 1. Overview

### 1.1 Purpose
Implement functionality to discover, display, toggle, and reorder all global menu items in the Selection Menu Configuration screen, following Moon+ Reader's proven pattern.

### 1.2 Scope
- Discover three types of menu items (system, Feeder, third-party)
- Display items in a **single flat list** (no sections)
- **Toggle items on/off** with switches
- **Cross-section reordering** (any item can go anywhere)
- Persist user's preferred order and visibility
- Support accessibility and performance requirements

### 1.3 Key Changes from v1.0
| Feature | v1.0 (Original) | v2.0 (Moon+ Reader Pattern) |
|---------|----------------|----------------------------|
| List Layout | Categorized sections | Single flat list |
| Reordering | Within sections only | Cross-section (anywhere) |
| Item Visibility | Always visible | Toggleable on/off |
| Default State | Section-based order | All visible by default |

### 1.4 References
- Requirements: `01-requirements.md`
- Research: `02-research-report.md`
- Assessment: `04-assessment.md`
- Spec-015: Selection Menu Configuration (placeholder implementation)
- Moon+ Reader: https://play.google.com/store/apps/details?id=com.flyersoft.moonreader

---

## 2. Architecture

### 2.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│              SelectionMenuSettingsScreen                    │
│                    (UI Layer)                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         SelectionMenuSettingsViewModel               │   │
│  │                   (ViewModel)                        │   │
│  │  - ViewState: items, loading, error                 │   │
│  │  - Events: LoadMenus, ReorderMenu, ToggleItem       │   │
│  └───────────────────┬─────────────────────────────────┘   │
│                      │                                      │
│                      ▼                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           MenuDiscoveryService                       │   │
│  │                (Service)                             │   │
│  │  - discoverSystemMenus()                            │   │
│  │  - discoverFeederMenus()                            │   │
│  │  - discoverThirdPartyMenus()                        │   │
│  └───────────────────┬─────────────────────────────────┘   │
│                      │                                      │
│                      ▼                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Android System                          │   │
│  │  - PackageManager (third-party discovery)            │   │
│  │  - Resources (system strings)                        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Persistence Layer                        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              SettingsStore                           │   │
│  │  - get/set menu config (SharedPreferences)           │   │
│  │    * order: List<String> (item IDs)                  │   │
│  │    * visibility: Map<String, Boolean>                │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

```
User Opens Screen
       │
       ▼
ViewModel.init() → LoadMenus Event
       │
       ▼
MenuDiscoveryService.discoverAll()
       │
       ├──→ System Items (hard-coded)
       ├──→ Feeder Items (hard-coded)
       └──→ Third-Party Items (PackageManager)
       │
       ▼
Load Saved Config from SharedPreferences
       │
       ├──→ Saved order (List<String>)
       └──→ Saved visibility (Map<String, Boolean>)
       │
       ▼
Merge: Discovered Items + Saved Config
       │
       ├──→ Apply saved order
       ├──→ Apply saved visibility (default: all true)
       └──→ Handle new/removed items
       │
       ▼
Update ViewState (items = merged list)
       │
       ▼
UI Renders Single Flat List with Toggles
       │
       ├──→ Each row has: [Switch] [Icon] [Name] [DragHandle]
       └──→ Only visible items shown in selection menu
       │
       ▼
User Actions
       │
       ├──→ Drag Item → ReorderMenu Event
       ├──→ Toggle Switch → ToggleItem Event
       └──→ Both trigger debounced save (2s)
       │
       ▼
UI Updates with New Order/Visibility
```

---

## 3. Data Model

### 3.1 SelectionMenuItem

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import android.content.ComponentName
import androidx.compose.runtime.Immutable

/**
 * Represents a single selection menu item.
 *
 * @property id Unique identifier for the menu item
 * @property name Display name of the menu item
 * @property description Optional description (e.g., package name for third-party)
 * @property icon Optional icon resource name
 * @property enabled Whether the menu item is enabled
 * @property visible Whether the menu item is visible in the selection menu (user toggle)
 * @property type Type of menu item (system, application, third-party)
 * @property componentName Component name for third-party apps
 * @property packageName Package name for third-party apps
 * @property order Display order (0-based index)
 */
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
    val visible: Boolean = true,  // NEW: User can toggle visibility
    val type: MenuType,
    val componentName: ComponentName? = null,
    val packageName: String? = null,
    val order: Int = 0,
)

/**
 * Enum representing the type of menu item.
 */
enum class MenuType {
    /** Built-in Android system actions (copy, paste, etc.) */
    SYSTEM,

    /** Feeder application features (read aloud, translate) */
    APPLICATION,

    /** Third-party applications (Anki, etc.) */
    THIRD_PARTY,
}
```

### 3.2 MenuConfig (Persistence)

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import kotlinx.serialization.Serializable

/**
 * Persistent storage for menu item configuration.
 *
 * Changed from MenuOrder to support:
 * - Single flat list order (no sections)
 * - Per-item visibility toggle
 *
 * @property order Ordered list of all menu item IDs (flat)
 * @property visibility Map of item ID to visibility state
 */
@Serializable
data class MenuConfig(
    val order: List<String> = emptyList(),
    val visibility: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        /**
         * Default configuration for menu items.
         */
        fun default() = MenuConfig(
            order = listOf(
                // System items
                "copy", "paste", "cut", "select_all",
                // Feeder items
                "read_aloud", "translate",
            ),
            visibility = mapOf(
                // All items visible by default
                "copy" to true,
                "paste" to true,
                "cut" to true,
                "select_all" to true,
                "read_aloud" to true,
                "translate" to true,
            ),
        )
    }

    /**
     * Check if config is empty (first load).
     */
    fun isEmpty(): Boolean = order.isEmpty()

    /**
     * Get visibility for an item, defaulting to true.
     */
    fun isVisible(itemId: String): Boolean = visibility.getOrDefault(itemId, true)

    /**
     * Check if an item is in the order.
     */
    fun contains(itemId: String): Boolean = order.contains(itemId)
}
```

---

## 4. Service Layer

### 4.1 MenuDiscoveryService

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.nononsenseapps.feeder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for discovering all available menu items.
 *
 * Discovers three types of menu items:
 * 1. System menus (built-in Android actions)
 * 2. Feeder menus (application features)
 * 3. Third-party menus (apps with ACTION_PROCESS_TEXT)
 */
class MenuDiscoveryService(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager

    /**
     * Discover all menu items across all types.
     *
     * Returns a flat list (no sections) for Moon+ Reader-style UI.
     *
     * @return List of all discovered menu items
     */
    suspend fun discoverAll(): List<SelectionMenuItem> = withContext(Dispatchers.Default) {
        val systemItems = discoverSystemMenus()
        val feederItems = discoverFeederMenus()
        val thirdPartyItems = discoverThirdPartyMenus()

        // Return flat list (sections only for metadata, not display)
        systemItems + feederItems + thirdPartyItems
    }

    /**
     * Discover system menu items (built-in Android actions).
     *
     * @return List of system menu items
     */
    private fun discoverSystemMenus(): List<SelectionMenuItem> {
        return listOf(
            SelectionMenuItem(
                id = "copy",
                name = context.getString(android.R.string.copy),
                type = MenuType.SYSTEM,
                order = 0,
            ),
            SelectionMenuItem(
                id = "paste",
                name = context.getString(android.R.string.paste),
                type = MenuType.SYSTEM,
                order = 1,
            ),
            SelectionMenuItem(
                id = "cut",
                name = context.getString(android.R.string.cut),
                type = MenuType.SYSTEM,
                order = 2,
            ),
            SelectionMenuItem(
                id = "select_all",
                name = context.getString(android.R.string.selectAll),
                type = MenuType.SYSTEM,
                order = 3,
            ),
        )
    }

    /**
     * Discover Feeder application menu items.
     *
     * @return List of Feeder menu items
     */
    private fun discoverFeederMenus(): List<SelectionMenuItem> {
        return listOf(
            SelectionMenuItem(
                id = "read_aloud",
                name = context.getString(R.string.selection_menu_read_aloud),
                type = MenuType.APPLICATION,
                order = 4,
            ),
            SelectionMenuItem(
                id = "translate",
                name = context.getString(R.string.selection_menu_translate),
                type = MenuType.APPLICATION,
                order = 5,
            ),
        )
    }

    /**
     * Discover third-party application menu items.
     *
     * @return List of third-party menu items
     */
    private fun discoverThirdPartyMenus(): List<SelectionMenuItem> {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val displayNameComparator = ResolveInfo.DisplayNameComparator(packageManager)

        return resolveInfos
            .sortedWith(displayNameComparator)
            .mapIndexed { index, info ->
                val label = info.loadLabel(packageManager).toString()
                val packageName = info.activityInfo.applicationInfo.packageName
                val activityName = info.activityInfo.name

                SelectionMenuItem(
                    id = "$packageName/$activityName",
                    name = label,
                    description = packageName,
                    type = MenuType.THIRD_PARTY,
                    componentName = ComponentName(packageName, activityName),
                    packageName = packageName,
                    order = 6 + index, // Start after system and feeder items
                )
            }
    }
}
```

---

## 5. ViewModel

### 5.1 SelectionMenuSettingsViewModel

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Json
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for the Selection Menu Configuration screen.
 *
 * Manages state for:
 * - Loading menu items
 * - Displaying menu list (single flat list)
 * - Handling user actions (reordering, toggling visibility)
 */
class SelectionMenuSettingsViewModel(
    override val di: DI,
) : DIAwareViewModel(di) {
    private val menuDiscoveryService: MenuDiscoveryService by instance()
    private val sp: SharedPreferences by instance()

    private val _viewState = MutableStateFlow<SelectionMenuViewState>(SelectionMenuViewState())
    val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()

    private var saveJob: kotlinx.coroutines.Job? = null

    init {
        loadMenus()
    }

    /**
     * Load menu items on initialization.
     */
    private fun loadMenus() {
        viewModelScope.launch {
            _viewState.update { it.copy(isLoading = true) }

            try {
                val items = menuDiscoveryService.discoverAll()
                val savedConfig = loadMenuConfig()

                val mergedItems = mergeWithConfig(items, savedConfig)

                _viewState.update {
                    it.copy(
                        isLoading = false,
                        items = mergedItems,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load menu items",
                    )
                }
            }
        }
    }

    /**
     * Handle events from the UI.
     */
    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.LoadMenus -> loadMenus()
            is SelectionMenuEvent.ReorderMenu -> reorderMenus(event.from, event.to)
            is SelectionMenuEvent.ToggleItem -> toggleItemVisibility(event.itemId)
        }
    }

    /**
     * Reorder menu items (cross-section, no restrictions).
     */
    private fun reorderMenus(
        from: Int,
        to: Int,
    ) {
        val currentItems = _viewState.value.items.toMutableList()

        // Remove from old position
        val item = currentItems.removeAt(from)

        // Insert at new position (no section restrictions)
        currentItems.add(to, item)

        // Update order property for all items
        val reorderedItems = currentItems.mapIndexed { index, menuItem ->
            menuItem.copy(order = index)
        }

        _viewState.update { it.copy(items = reorderedItems) }

        // Debounced save
        saveMenuConfigDebounced(reorderedItems)
    }

    /**
     * Toggle item visibility.
     */
    private fun toggleItemVisibility(itemId: String) {
        val currentItems = _viewState.value.items
        val updatedItems = currentItems.map { item ->
            if (item.id == itemId) {
                item.copy(visible = !item.visible)
            } else {
                item
            }
        }

        _viewState.update { it.copy(items = updatedItems) }

        // Debounced save
        saveMenuConfigDebounced(updatedItems)
    }

    /**
     * Save menu configuration with debouncing (2 seconds).
     */
    private fun saveMenuConfigDebounced(items: List<SelectionMenuItem>) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2_000)
            saveMenuConfig(items)
        }
    }

    /**
     * Save menu configuration to SharedPreferences.
     */
    private fun saveMenuConfig(items: List<SelectionMenuItem>) {
        val config = MenuConfig(
            order = items.map { it.id },
            visibility = items.associate { it.id to it.visible },
        )

        val json = Json.encodeToString(MenuConfig.serializer(), config)
        sp.edit().putString(PREF_MENU_CONFIG, json).apply()
    }

    /**
     * Load menu configuration from SharedPreferences.
     */
    private fun loadMenuConfig(): MenuConfig {
        val json = sp.getString(PREF_MENU_CONFIG, null) ?: return MenuConfig.default()

        return try {
            Json.decodeFromString(MenuConfig.serializer(), json)
        } catch (e: Exception) {
            MenuConfig.default()
        }
    }

    /**
     * Merge discovered items with saved configuration.
     *
     * This handles:
     * - Applying saved order
     * - Applying saved visibility (default: all true)
     * - Adding new items not in saved config
     * - Removing items that no longer exist
     */
    private fun mergeWithConfig(
        items: List<SelectionMenuItem>,
        config: MenuConfig,
    ): List<SelectionMenuItem> {
        if (config.isEmpty()) {
            // First load - use default order and all visible
            return items.mapIndexed { index, item ->
                item.copy(
                    order = index,
                    visible = true,
                )
            }
        }

        // Create map for quick lookup
        val itemsMap = items.associateBy { it.id }

        // Apply saved order
        val orderedItems = config.order.mapNotNull { itemsMap[it] }

        // Append new items not in saved order
        val newItems = (itemsMap.keys - config.order.toSet())
            .mapNotNull { itemsMap[it] }

        val allItems = orderedItems + newItems

        // Apply saved visibility (default to true for new items)
        return allItems.map { item ->
            item.copy(
                visible = config.isVisible(item.id),
            )
        }
    }

    companion object {
        const val PREF_MENU_CONFIG = "selection_menu_config"
    }
}

/**
 * ViewState for the Selection Menu Configuration screen.
 *
 * @property isLoading Whether menu items are being loaded
 * @property items List of menu items to display (single flat list)
 * @property error Optional error message
 */
@Immutable
data class SelectionMenuViewState(
    val isLoading: Boolean = false,
    val items: List<SelectionMenuItem> = emptyList(),
    val error: String? = null,
)

/**
 * Events that can be triggered in the Selection Menu Configuration screen.
 */
sealed class SelectionMenuEvent {
    /**
     * Event to load menu items.
     */
    data object LoadMenus : SelectionMenuEvent()

    /**
     * Event to reorder menu items.
     * @property from Position to move from
     * @property to Position to move to
     */
    data class ReorderMenu(
        val from: Int,
        val to: Int,
    ) : SelectionMenuEvent()

    /**
     * Event to toggle item visibility.
     * @property itemId ID of the item to toggle
     */
    data class ToggleItem(
        val itemId: String,
    ) : SelectionMenuEvent()
}
```

---

## 6. UI Implementation

### 6.1 Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.better.android:reorderable:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
```

Add to `app/build.gradle.kts` plugins:

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}
```

### 6.2 Screen Implementation

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.better.android.reorderable.ReorderableItem
import com.better.android.reorderable.rememberReorderableLazyListState
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.theme.SensibleTopAppBar

/**
 * Main screen for Selection Menu Configuration.
 *
 * Follows Moon+ Reader pattern:
 * - Single flat list (no sections)
 * - Toggle switches for each item
 * - Drag handles for reordering
 * - Cross-section reordering allowed
 */
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
                title = stringResource(id = R.string.selection_menu_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        SelectionMenuContent(
            viewState = viewState,
            onReorder = { from, to ->
                viewModel.onEvent(SelectionMenuEvent.ReorderMenu(from, to))
            },
            onToggle = { itemId ->
                viewModel.onEvent(SelectionMenuEvent.ToggleItem(itemId))
            },
            modifier = Modifier
                .padding(paddingValues)
                .padding(
                    horizontal = LocalDimens.current.margin,
                    vertical = 8.dp,
                ),
        )
    }
}

/**
 * Content area for the Selection Menu Configuration screen.
 */
@Composable
private fun SelectionMenuContent(
    viewState: SelectionMenuViewState,
    onReorder: (from: Int, to: Int) -> Unit,
    onToggle: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        viewState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        viewState.error != null -> {
            ErrorState(
                error = viewState.error,
                onRetry = { onReorder(0, 0) }, // Trigger reload
                modifier = modifier.fillMaxSize(),
            )
        }
        viewState.items.isEmpty() -> {
            EmptyState(modifier = modifier.fillMaxSize())
        }
        else -> {
            MenuList(
                items = viewState.items,
                onReorder = onReorder,
                onToggle = onToggle,
                modifier = modifier.width(LocalDimens.current.maxContentWidth),
            )
        }
    }
}

/**
 * Reorderable list of menu items (single flat list).
 *
 * Moon+ Reader pattern:
 * - No sections
 * - All items in one list
 * - Cross-section reordering allowed
 */
@Composable
private fun MenuList(
    items: List<SelectionMenuItem>,
    onReorder: (from: Int, to: Int) -> Unit,
    onToggle: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, onMove = { from, to ->
        // Allow cross-section reordering (no restrictions)
        onReorder(from.index, to.index)
        true
    })

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Single flat list - no sections
        items(
            items = items,
            key = { it.id },
        ) { item ->
            ReorderableItem(reorderableState, key = item.id) { isDragging ->
                MenuItemRow(
                    item = item,
                    isDragging = isDragging,
                    onToggle = onToggle,
                )
            }
        }
    }
}

/**
 * Individual menu item row.
 *
 * Layout: [Switch] [Icon] [Name + Description] [DragHandle]
 *
 * Moon+ Reader pattern:
 * - Toggle switch on the left
 * - Icon and name in the middle
 * - Drag handle on the right
 * - Shows description (package name) for third-party apps
 */
@Composable
private fun MenuItemRow(
    item: SelectionMenuItem,
    isDragging: Boolean,
    onToggle: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isDragging -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Toggle Switch (left)
        Switch(
            checked = item.visible,
            onCheckedChange = { onToggle(item.id) },
        )

        // Icon (if available)
        if (item.icon != null) {
            Icon(
                imageVector = Icons.Default.Circle, // Replace with actual icon
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        // Name and Description (middle, expandable)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.description != null) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Drag Handle (right)
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(
                R.string.selection_menu_drag_to_reorder,
                item.name,
            ),
            modifier = Modifier
                .size(24.dp)
                .alpha(0.5f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Empty state displayed when no menu items are available.
 */
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.selection_menu_no_items),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.selection_menu_no_items_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Error state displayed when loading fails.
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Error: $error",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )

            // Retry button
            androidx.compose.material3.Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
```

---

## 7. String Resources

Add to `app/src/main/res/values/strings.xml`:

```xml
<!-- Menu Item Names -->
<string name="selection_menu_read_aloud">Read Aloud</string>
<string name="selection_menu_translate">Translate</string>

<!-- Empty State -->
<string name="selection_menu_no_items">Unable to find any menu items</string>
<string name="selection_menu_no_items_hint">Make sure other apps are installed and text processing is enabled.</string>

<!-- Accessibility -->
<string name="selection_menu_drag_to_reorder">Drag to reorder %s</string>
<string name="selection_menu_toggle_visibility">Toggle visibility for %s</string>
<string name="selection_menu_visible">%s is visible</string>
<string name="selection_menu_hidden">%s is hidden</string>

<!-- Retry -->
<string name="retry">Retry</string>
```

---

## 8. DI Configuration

Add to DI module:

```kotlin
bind() from singleton {
    MenuDiscoveryService(instance())
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**MenuDiscoveryServiceTest**:
```kotlin
class MenuDiscoveryServiceTest {
    @Test
    fun `discoverAll returns all menu types`() = runTest {
        val service = MenuDiscoveryService(context)
        val items = service.discoverAll()

        assertTrue(items.any { it.type == MenuType.SYSTEM })
        assertTrue(items.any { it.type == MenuType.APPLICATION })
        assertTrue(items.any { it.type == MenuType.THIRD_PARTY })
    }

    @Test
    fun `discoverSystemMenus returns 4 items`() {
        val service = MenuDiscoveryService(context)
        val items = service.discoverSystemMenus()

        assertEquals(4, items.size)
    }

    @Test
    fun `all items are visible by default`() {
        val service = MenuDiscoveryService(context)
        val items = service.discoverAll()

        assertTrue(items.all { it.visible })
    }
}
```

**ViewModelTest**:
```kotlin
class SelectionMenuSettingsViewModelTest {
    @Test
    fun `load menus updates view state`() = runTest {
        val viewModel = SelectionMenuSettingsViewModel(testDI)

        advanceUntilIdle()

        assertNotNull(viewModel.viewState.value.items)
        assertFalse(viewModel.viewState.value.isLoading)
    }

    @Test
    fun `reorder menu allows cross-section movement`() = runTest {
        val viewModel = SelectionMenuSettingsViewModel(testDI)
        advanceUntilIdle()

        val originalItems = viewModel.viewState.value.items
        val systemItemIndex = originalItems.indexOfFirst { it.type == MenuType.SYSTEM }
        val thirdPartyItemIndex = originalItems.indexOfFirst { it.type == MenuType.THIRD_PARTY }

        // Move system item to third-party position (should be allowed)
        viewModel.onEvent(SelectionMenuEvent.ReorderMenu(systemItemIndex, thirdPartyItemIndex))

        // Verify order changed
        assertNotEquals(originalItems, viewModel.viewState.value.items)
    }

    @Test
    fun `toggle item changes visibility`() = runTest {
        val viewModel = SelectionMenuSettingsViewModel(testDI)
        advanceUntilIdle()

        val item = viewModel.viewState.value.items.first()
        val originalVisibility = item.visible

        viewModel.onEvent(SelectionMenuEvent.ToggleItem(item.id))

        assertEquals(!originalVisibility, viewModel.viewState.value.items.first { it.id == item.id }.visible)
    }

    @Test
    fun `save and load config persists order and visibility`() = runTest {
        val viewModel = SelectionMenuSettingsViewModel(testDI)
        advanceUntilIdle()

        // Reorder and toggle
        viewModel.onEvent(SelectionMenuEvent.ReorderMenu(0, 1))
        viewModel.onEvent(SelectionMenuEvent.ToggleItem("copy"))

        // Wait for debounced save
        advanceTimeBy(2_100)

        // Create new ViewModel to load from persistence
        val viewModel2 = SelectionMenuSettingsViewModel(testDI)
        advanceUntilIdle()

        // Verify order and visibility persisted
        assertEquals(viewModel.viewState.value.items.map { it.id }, viewModel2.viewState.value.items.map { it.id })
        assertEquals(viewModel.viewState.value.items.associate { it.id to it.visible },
                     viewModel2.viewState.value.items.associate { it.id to it.visible })
    }
}
```

### 9.2 UI Tests

```kotlin
class SelectionMenuSettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysMenuItems() {
        val viewModel = SelectionMenuSettingsViewModel(testDI)

        composeTestRule.setContent {
            SelectionMenuSettingsScreen(
                onNavigateUp = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
    }

    @Test
    fun toggleSwitch_changesVisibility() {
        val viewModel = SelectionMenuSettingsViewModel(testDI)

        composeTestRule.setContent {
            SelectionMenuSettingsScreen(
                onNavigateUp = {},
                viewModel = viewModel
            )
        }

        // Find first switch and toggle it
        composeTestRule
            .onAllNodes(hasTestTag("toggle_switch"))
            .first()
            .performClick()

        // Verify visibility changed
        assertFalse(viewModel.viewState.value.items.first().visible)
    }
}
```

---

## 10. Performance Considerations

### 10.1 Optimization Strategies

1. **Discovery Caching**: Cache results for 5 seconds
2. **Lazy Loading**: Use LazyColumn with stable keys
3. **Debounced Saves**: Save config after 2 seconds of inactivity
4. **Coroutines**: Run discovery on Dispatchers.Default
5. **Minimal Recomposition**: Use stable keys and derivedStateOf

### 10.2 Performance Targets

- Discovery time: < 500ms
- UI rendering: < 100ms
- Reorder animation: 60fps
- Save operation: < 50ms
- Toggle response: < 50ms

---

## 11. Accessibility

### 11.1 Screen Reader Support

- Add content descriptions for all interactive elements
- Announce toggle state changes ("[Item] is now visible/hidden")
- Use proper semantics for list items
- Announce position changes during reorder

### 11.2 Keyboard Navigation

- Support D-Pad navigation
- Handle arrow keys for reordering
- Provide focus indicators
- Support Tab for switch toggle

---

## 12. Error Handling

### 12.1 Scenarios

1. **PackageManager query fails**: Show error state, allow retry
2. **SharedPreferences corrupted**: Use default config
3. **Third-party app missing label**: Use package name
4. **Invalid saved config**: Validate and use default
5. **Item not found during reorder**: Gracefully handle

### 12.2 Recovery

- Try-catch all external calls
- Provide user-friendly error messages
- Offer retry option
- Graceful degradation

---

## 13. Migration from v1.0

### 13.1 Breaking Changes

If `MenuOrder` (v1.0) exists in SharedPreferences, migrate to `MenuConfig` (v2.0):

```kotlin
private fun migrateMenuOrderToConfig(): MenuConfig {
    val oldOrderJson = sp.getString("selection_menu_order", null)

    return if (oldOrderJson != null) {
        try {
            val oldOrder = Json.decodeFromString(MenuOrder.serializer(), oldOrderJson)
            // Flatten sections into single list, all visible
            MenuConfig(
                order = oldOrder.system + oldOrder.feeder + oldOrder.thirdParty,
                visibility = (oldOrder.system + oldOrder.feeder + oldOrder.thirdParty)
                    .associateWith { true },
            )
        } catch (e: Exception) {
            MenuConfig.default()
        }
    } else {
        MenuConfig.default()
    }
}
```

---

**Specification Complete**: 2026-01-04
**Version**: 2.0 (Moon+ Reader Pattern)
**Status**: ✅ Implementation Complete
**Implementation Summary**: See `11-implementation-summary.md`

---

## 14. Implementation Status

### 14.1 Final Implementation Details

**Implementation Date**: 2026-01-04
**Status**: ✅ Complete and Functional

### 14.2 Library Choice (UPDATED)

**Final Library**: `sh.calvin.reorderable:reorderable:2.4.0`

| Aspect | Initial Choice (v0.9.6) | Final Choice (v2.4.0) |
|--------|-------------------------|----------------------|
| Package | `org.burnoutcrew.composereorderable` | `sh.calvin.reorderable` |
| Maintenance Status | ❌ Abandoned (Nov 2022) | ✅ Active (Aug 2025) |
| Drag Handle API | ❌ Missing | ✅ `draggableHandle()` |
| Touch Event Isolation | ❌ Broken with Switch | ✅ Working |
| Compose Compatibility | Issues with 1.5+ | ✅ Modern Compose |

**Reason for Change**: The v0.9.6 library was abandoned and had critical issues with touch event conflicts when using Switch components inside draggable items. The v2.4.0 library provides `Modifier.draggableHandle()` which designates a specific drag area and prevents touch conflicts.

### 14.3 Key Implementation Changes from Spec

#### Drag Handle Modifier (CRITICAL)

The specification didn't originally include the drag handle modifier, but it's essential for functionality:

```kotlin
Icon(
    Icons.Filled.DragHandle,
    modifier = with(dragHandleScope) {
        Modifier
            .size(24.dp)
            .draggableHandle()  // ← CRITICAL: Prevents Switch touch conflicts
    }
)
```

**Why This Matters**: Without `draggableHandle()`, the Switch component consumes all touch events, preventing drag detection. The modifier explicitly designates the drag handle as the drag trigger area.

#### API Parameter Order

The `rememberReorderableLazyListState` function requires specific parameter order:

```kotlin
val reorderableState = rememberReorderableLazyListState(
    lazyListState = lazyListState,  // ← Must be first
    onMove = { from, to -> ... }     // ← onMove callback
)
```

#### ReorderableItem Scope

The `ReorderableItem` lambda provides a scope needed for the drag handle modifier:

```kotlin
ReorderableItem(state = reorderableState, key = item.id) { isDragging ->
    MenuItemRow(
        item = item,
        isDragging = isDragging,
        dragHandleScope = this,  // ← Pass scope to MenuItemRow
    )
}
```

### 14.4 Internationalization

Added Simplified Chinese (zh-rCN) translations:

| String Resource | English | 简体中文 |
|----------------|---------|----------|
| `selection_menu_title` | Selection Menu | 选择菜单 |
| `selection_menu_drag_to_reorder` | Drag to reorder | 拖动以重新排序 |
| `selection_menu_read_aloud` | Read Aloud | 朗读 |
| `selection_menu_translate` | Translate | 翻译 |

### 14.5 Commits

| Commit Hash | Description |
|------------|-------------|
| `e10c3246` | Implement Global Menu Configuration with Moon+ Reader pattern |
| `dc2ccf86` | Implement drag-and-drop reordering for menu items |
| `2894851b` | Fix: Add visible drag handle icon to menu items |
| `c721e385` | Fix: Implement working drag-to-reorder with modern Reorderable library |

### 14.6 Known Limitations

1. **Drag handle required** - Users must long-press the drag handle icon (⋮⋮), not the text area
2. **Switch consumes tap** - Short taps on Switch toggle visibility, don't initiate drag
3. **Emulator issues** - Touch sensitivity problems may occur in emulators; physical device recommended

### 14.7 Testing Verification

Tested and verified on:
- ✅ Physical device (drag-to-reorder works)
- ✅ Toggle switches work independently
- ✅ Persistence across app restarts
- ✅ Visual feedback during drag (background color change)
- ✅ Cross-section reordering (any item can go anywhere)

---
