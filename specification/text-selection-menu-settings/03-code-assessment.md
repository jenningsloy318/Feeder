# Code Assessment: Text Selection Menu Settings

**Date:** 2025-11-27 22:00:00 PST
**Phase:** 5 - Code Assessment

## Executive Summary

This assessment analyzes the exact integration points for implementing text selection menu settings in the Feeder app. Key findings:

1. **Menu Construction**: Need to intercept `onCreateActionMode` and `addTextProcessors` in `FeederTextToolbar.kt`
2. **Settings Storage**: Add new flow to `SettingsStore.kt` following existing patterns
3. **Configuration Model**: Need data class for menu item configuration with serialization
4. **UI Component**: Dedicated settings screen recommended due to reordering complexity
5. **Integration Points**: 4 main files to modify + new UI screen + strings

## Integration Points Analysis

### 1. FeederTextToolbar.kt - Menu Construction

**Current Flow**:
```
FeederTextToolbar.showMenu()
  → startActionMode(FloatingTextActionModeCallback)
    → onCreateActionMode()
      → addMenuItem(Copy/Paste/Cut/SelectAll)
      → addTextProcessors()
```

**Required Changes**:

#### A. Add Configuration Dependency

```kotlin
class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    private val menuConfig: StateFlow<TextMenuConfig>,  // NEW
) : TextToolbar {
    // ...
}
```

**Impact**: Need to pass `menuConfig` from `WithFeederTextToolbar` composable, which gets it from DI.

#### B. Modify onCreateActionMode

**Current** (lines 106-130):
```kotlin
override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    requireNotNull(menu)
    requireNotNull(mode)

    onCopyRequested?.let {
        addMenuItem(menu, MenuItemOption.Copy)
    }
    onPasteRequested?.let {
        addMenuItem(menu, MenuItemOption.Paste)
    }
    onCutRequested?.let {
        addMenuItem(menu, MenuItemOption.Cut)
    }
    onSelectAllRequested?.let {
        addMenuItem(menu, MenuItemOption.SelectAll)
    }
    onCopyRequested?.let {
        addTextProcessors(menu)
    }
    return true
}
```

**Proposed**:
```kotlin
override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    requireNotNull(menu)
    requireNotNull(mode)

    val config = menuConfig.value
    val sortedItems = config.items.sortedBy { it.order }

    sortedItems.forEach { configItem ->
        when (configItem.type) {
            MenuItemType.COPY -> {
                onCopyRequested?.let {
                    addMenuItem(menu, MenuItemOption.Copy, configItem)
                }
            }
            MenuItemType.PASTE -> {
                onPasteRequested?.let {
                    addMenuItem(menu, MenuItemOption.Paste, configItem)
                }
            }
            MenuItemType.CUT -> {
                onCutRequested?.let {
                    addMenuItem(menu, MenuItemOption.Cut, configItem)
                }
            }
            MenuItemType.SELECT_ALL -> {
                onSelectAllRequested?.let {
                    addMenuItem(menu, MenuItemOption.SelectAll, configItem)
                }
            }
            MenuItemType.TEXT_PROCESSOR -> {
                // Individual processor, handled in addConfiguredTextProcessors
            }
        }
    }

    // Add configured text processors
    onCopyRequested?.let {
        addConfiguredTextProcessors(menu, config)
    }

    return true
}
```

**Key Changes**:
- Read configuration from `menuConfig.value`
- Sort items by `order` field
- Pass `configItem` to `addMenuItem` to handle enabled/disabled state
- New method `addConfiguredTextProcessors` replaces `addTextProcessors`

#### C. Modify addMenuItem

**Current** (lines 234-241):
```kotlin
private fun addMenuItem(menu: Menu, item: MenuItemOption) {
    menu
        .add(0, item.id, item.order, item.titleResource)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
}
```

**Proposed**:
```kotlin
private fun addMenuItem(
    menu: Menu,
    item: MenuItemOption,
    config: MenuItemConfig
) {
    val menuItem = menu
        .add(0, item.id, config.order, item.titleResource)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

    // Apply disabled state
    menuItem.isEnabled = config.enabled
    if (!config.enabled) {
        // Set visual indication of disabled state
        menuItem.setAlpha(0.5f)  // or use SpannableString with color
    }
}
```

**Note**: MenuItem doesn't have setAlpha(), need to use SpannableString with color/strikethrough for visual disabled state.

#### D. New Method: addConfiguredTextProcessors

```kotlin
private fun addConfiguredTextProcessors(menu: Menu, config: TextMenuConfig) {
    textProcessors.clear()

    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
    }

    // Discover available processors
    val availableProcessors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }

    // Build map of discovered processors
    val discoveredProcessors = availableProcessors.associateBy { info ->
        ComponentName(
            info.activityInfo.applicationInfo.packageName,
            info.activityInfo.name
        )
    }

    // Get configured processor items, sorted by order
    val processorConfigs = config.items
        .filter { it.type == MenuItemType.TEXT_PROCESSOR }
        .sortedBy { it.order }

    // Add configured processors that still exist
    processorConfigs.forEach { configItem ->
        val componentName = ComponentName(
            configItem.processorPackage ?: return@forEach,
            configItem.processorActivity ?: return@forEach
        )

        val resolveInfo = discoveredProcessors[componentName] ?: return@forEach

        val label = resolveInfo.loadLabel(packageManager)
        val id = 100 + textProcessors.size

        val menuItem = menu.add(1, id, configItem.order, label)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        // Apply disabled state
        menuItem.isEnabled = configItem.enabled
        if (!configItem.enabled) {
            // Visual indication of disabled state
            menuItem.setTitle(formatDisabledTitle(label.toString()))
        }

        textProcessors.add(componentName)
    }

    // Optionally: Add newly discovered processors not in config
    // (for first-time users or when new apps are installed)
    discoveredProcessors.forEach { (componentName, resolveInfo) ->
        val alreadyConfigured = processorConfigs.any {
            it.processorPackage == componentName.packageName &&
            it.processorActivity == componentName.className
        }

        if (!alreadyConfigured) {
            // Add with default enabled=true, order after existing items
            val label = resolveInfo.loadLabel(packageManager)
            val id = 100 + textProcessors.size
            val order = (config.items.maxOfOrNull { it.order } ?: 0) + 1

            menu.add(1, id, order, label)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            textProcessors.add(componentName)
        }
    }
}

private fun formatDisabledTitle(title: String): CharSequence {
    return SpannableString(title).apply {
        setSpan(
            ForegroundColorSpan(Color.GRAY),
            0,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
```

**Key Logic**:
1. Discover all available text processors via PackageManager
2. Match discovered processors with user configuration
3. Add configured processors in user-specified order
4. Apply enabled/disabled state with visual styling
5. Optionally add newly discovered processors not yet configured

### 2. SettingsStore.kt - Configuration Storage

**Add to SettingsStore** (around line 500, before companion object):

```kotlin
private val _textMenuConfig = MutableStateFlow(
    loadTextMenuConfig()
)
val textMenuConfig = _textMenuConfig.asStateFlow()

fun setTextMenuConfig(config: TextMenuConfig) {
    _textMenuConfig.value = config
    sp.edit().putString(PREF_TEXT_MENU_CONFIG, config.toJson()).apply()
}

private fun loadTextMenuConfig(): TextMenuConfig {
    val json = sp.getString(PREF_TEXT_MENU_CONFIG, null)
    return if (json != null) {
        try {
            TextMenuConfig.fromJson(json)
        } catch (e: Exception) {
            getDefaultTextMenuConfig()
        }
    } else {
        getDefaultTextMenuConfig()
    }
}

private fun getDefaultTextMenuConfig(): TextMenuConfig {
    return TextMenuConfig(
        items = listOf(
            MenuItemConfig(
                type = MenuItemType.COPY,
                enabled = true,
                order = 0
            ),
            MenuItemConfig(
                type = MenuItemType.SELECT_ALL,
                enabled = true,
                order = 1
            ),
            // Text processors added dynamically at first discovery
        )
    )
}
```

**Add constant** (around line 614, with other PREF_* constants):
```kotlin
const val PREF_TEXT_MENU_CONFIG = "pref_text_menu_config"
```

**Add to UserSettings enum** (around line 660):
```kotlin
SETTING_TEXT_MENU_CONFIG(key = PREF_TEXT_MENU_CONFIG),
```

### 3. Data Model - TextMenuConfig.kt (NEW FILE)

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/TextMenuConfig.kt`

```kotlin
package com.nononsenseapps.feeder.archmodel

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TextMenuConfig(
    val items: List<MenuItemConfig>
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): TextMenuConfig = Json.decodeFromString(json)
    }
}

@Serializable
data class MenuItemConfig(
    val type: MenuItemType,
    val enabled: Boolean,
    val order: Int,
    val processorPackage: String? = null,  // For TEXT_PROCESSOR type
    val processorActivity: String? = null, // For TEXT_PROCESSOR type
)

@Serializable
enum class MenuItemType {
    COPY,
    PASTE,
    CUT,
    SELECT_ALL,
    TEXT_PROCESSOR
}
```

**Dependencies**: Project already uses `kotlinx.serialization` (seen in build.gradle.kts), so this is straightforward.

### 4. SettingsViewModel.kt - View State Integration

**Add to SettingsViewModel**:

```kotlin
// Around line 159, with other setter methods
fun setTextMenuConfig(config: TextMenuConfig) {
    repository.setTextMenuConfig(config)
}
```

**Add to combine() flow** (around line 204):
```kotlin
combine(
    repository.currentTheme,
    // ... all existing flows
    repository.textMenuConfig,  // NEW
) { params: Array<Any> ->
    @Suppress("UNCHECKED_CAST")
    SettingsViewState(
        // ... existing parameters
        textMenuConfig = params[30] as TextMenuConfig,  // NEW (adjust index)
    )
}.collect {
    _viewState.value = it
}
```

**Add to SettingsViewState** (around line 330):
```kotlin
data class SettingsViewState(
    // ... existing properties
    val textMenuConfig: TextMenuConfig = TextMenuConfig(emptyList()),  // NEW
)
```

### 5. Settings UI - New Screen Required

Due to drag-and-drop complexity, recommend dedicated screen.

**Add navigation**:

#### A. Settings.kt - Add ExternalSetting

In `SettingsList` composable (around line 470, after text settings):

```kotlin
HorizontalDivider(modifier = Modifier.width(dimens.maxContentWidth))

SettingsGroup(
    title = R.string.text_selection_menu,
) {
    val itemCount = textMenuConfig.items.size
    ExternalSetting(
        currentValue = stringResource(R.string.configured_items_count, itemCount),
        title = stringResource(R.string.text_selection_menu_settings),
        onClick = onTextSelectionMenuSettings,
    )
}
```

#### B. New File: TextSelectionMenuSettingsScreen.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TextSelectionMenuSettingsScreen.kt`

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
// ... imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TextSelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = R.string.text_selection_menu_settings),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.reset_to_defaults),
                        )
                    }
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = viewState.sortedItems,
                key = { it.id }
            ) { item ->
                MenuItemSettingCard(
                    item = item,
                    onToggleEnabled = { viewModel.toggleItemEnabled(item.id) },
                    onMoveUp = { viewModel.moveItemUp(item.id) },
                    onMoveDown = { viewModel.moveItemDown(item.id) },
                    canMoveUp = viewState.canMoveUp(item.id),
                    canMoveDown = viewState.canMoveDown(item.id),
                )
            }
        }
    }

    if (viewState.showResetDialog) {
        ResetConfirmationDialog(
            onDismiss = { viewModel.dismissResetDialog() },
            onConfirm = { viewModel.confirmReset() },
        )
    }
}

@Composable
fun MenuItemSettingCard(
    item: UIMenuItem,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(Icons.Default.ArrowUpward, "Move up")
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(Icons.Default.ArrowDownward, "Move down")
                }

                Switch(
                    checked = item.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
        }
    }
}

@Composable
fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.reset_to_defaults))
        },
        text = {
            Text(stringResource(R.string.reset_text_menu_confirmation))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
```

#### C. New ViewModel: TextSelectionMenuSettingsViewModel.kt

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ApplicationCoroutineScope
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.TextMenuConfig
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class TextSelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    private val applicationCoroutineScope: ApplicationCoroutineScope by instance()

    private val _viewState = MutableStateFlow(TextMenuSettingsViewState())
    val viewState: StateFlow<TextMenuSettingsViewState> = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.textMenuConfig.collect { config ->
                _viewState.value = TextMenuSettingsViewState(
                    config = config,
                    sortedItems = config.toUIMenuItems(),
                    showResetDialog = false
                )
            }
        }
    }

    fun toggleItemEnabled(itemId: String) {
        val current = _viewState.value.config
        val updated = current.copy(
            items = current.items.map { item ->
                if (item.id == itemId) {
                    item.copy(enabled = !item.enabled)
                } else {
                    item
                }
            }
        )
        repository.setTextMenuConfig(updated)
    }

    fun moveItemUp(itemId: String) {
        moveItem(itemId, -1)
    }

    fun moveItemDown(itemId: String) {
        moveItem(itemId, 1)
    }

    private fun moveItem(itemId: String, direction: Int) {
        val current = _viewState.value.config
        val sorted = current.items.sortedBy { it.order }
        val index = sorted.indexOfFirst { it.id == itemId }

        if (index == -1) return
        val newIndex = (index + direction).coerceIn(0, sorted.size - 1)
        if (newIndex == index) return

        val reordered = sorted.toMutableList()
        val item = reordered.removeAt(index)
        reordered.add(newIndex, item)

        val updated = current.copy(
            items = reordered.mapIndexed { i, item ->
                item.copy(order = i)
            }
        )
        repository.setTextMenuConfig(updated)
    }

    fun resetToDefaults() {
        _viewState.value = _viewState.value.copy(showResetDialog = true)
    }

    fun confirmReset() {
        // Get default config and save
        val defaultConfig = getDefaultTextMenuConfig()
        repository.setTextMenuConfig(defaultConfig)
        _viewState.value = _viewState.value.copy(showResetDialog = false)
    }

    fun dismissResetDialog() {
        _viewState.value = _viewState.value.copy(showResetDialog = false)
    }
}

@Immutable
data class TextMenuSettingsViewState(
    val config: TextMenuConfig = TextMenuConfig(emptyList()),
    val sortedItems: List<UIMenuItem> = emptyList(),
    val showResetDialog: Boolean = false,
) {
    fun canMoveUp(itemId: String): Boolean {
        val index = sortedItems.indexOfFirst { it.id == itemId }
        return index > 0
    }

    fun canMoveDown(itemId: String): Boolean {
        val index = sortedItems.indexOfFirst { it.id == itemId }
        return index >= 0 && index < sortedItems.size - 1
    }
}

data class UIMenuItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val enabled: Boolean,
    val order: Int,
)

private fun TextMenuConfig.toUIMenuItems(): List<UIMenuItem> {
    return items.sortedBy { it.order }.map { item ->
        UIMenuItem(
            id = item.generateId(),
            title = item.getDisplayTitle(),
            subtitle = item.getDisplaySubtitle(),
            enabled = item.enabled,
            order = item.order
        )
    }
}

private fun MenuItemConfig.generateId(): String {
    return when (type) {
        MenuItemType.TEXT_PROCESSOR -> "$processorPackage/$processorActivity"
        else -> type.name
    }
}

private fun MenuItemConfig.getDisplayTitle(): String {
    return when (type) {
        MenuItemType.COPY -> "Copy"
        MenuItemType.PASTE -> "Paste"
        MenuItemType.CUT -> "Cut"
        MenuItemType.SELECT_ALL -> "Select All"
        MenuItemType.TEXT_PROCESSOR -> processorPackage?.split(".")?.lastOrNull() ?: "Unknown"
    }
}

private fun MenuItemConfig.getDisplaySubtitle(): String? {
    return when (type) {
        MenuItemType.TEXT_PROCESSOR -> processorActivity
        else -> null
    }
}
```

### 6. Strings.xml - New Strings

**Add to** `app/src/main/res/values/strings.xml`:

```xml
<!-- Text Selection Menu Settings -->
<string name="text_selection_menu">Text Selection Menu</string>
<string name="text_selection_menu_settings">Text Selection Menu Settings</string>
<string name="configured_items_count">%d items configured</string>
<string name="reset_to_defaults">Reset to Defaults</string>
<string name="reset_text_menu_confirmation">Reset text selection menu to default settings? This will restore the default order and enable all items.</string>
<string name="reset">Reset</string>
```

## Files to Create/Modify Summary

### New Files (3)
1. `archmodel/TextMenuConfig.kt` - Data model
2. `ui/compose/settings/TextSelectionMenuSettingsScreen.kt` - UI screen
3. `ui/compose/settings/TextSelectionMenuSettingsViewModel.kt` - ViewModel

### Modified Files (5)
1. `archmodel/SettingsStore.kt` - Add textMenuConfig flow and storage
2. `ui/compose/utils/FeederTextToolbar.kt` - Apply configuration to menu
3. `ui/compose/settings/Settings.kt` - Add navigation to new screen
4. `ui/compose/settings/SettingsViewModel.kt` - Add textMenuConfig to view state
5. `res/values/strings.xml` - Add new strings

### Navigation Wiring
Need to add navigation route and wire up in main navigation graph.

## Edge Cases & Error Handling

### 1. Text Processor Lifecycle

**Scenario**: User configures a text processor, then uninstalls the app.

**Handling**:
- `addConfiguredTextProcessors` checks if ComponentName exists in discovered processors
- If not found, skip adding to menu (graceful degradation)
- Configuration kept in case app is reinstalled

### 2. First-Time Users

**Scenario**: User has no saved configuration.

**Handling**:
- `loadTextMenuConfig()` returns default config with Copy and SelectAll
- Text processors added dynamically on first menu construction
- Can save discovered processors to config on first discovery

### 3. New Text Processor Installed

**Scenario**: User installs new app with ACTION_PROCESS_TEXT support.

**Handling**:
- `addConfiguredTextProcessors` detects unconfigured processors
- Adds them automatically with default enabled=true
- Places them after existing configured items
- Alternative: Show notification in settings to configure new processor

### 4. All Items Disabled

**Scenario**: User disables all menu items.

**Handling**:
- Menu still shown with grayed-out items per requirements
- Items not clickable but visible
- User can see what's available and re-enable

### 5. Configuration Corruption

**Scenario**: JSON deserialization fails.

**Handling**:
- Catch exception in `loadTextMenuConfig()`
- Fall back to `getDefaultTextMenuConfig()`
- Log error for debugging

## Performance Considerations

### Menu Construction Performance

**Current**: Menu built on every text selection
**Impact of Changes**: Minimal

- Config read from in-memory StateFlow (fast)
- Sorting items is O(n log n) where n is small (typically < 10 items)
- PackageManager query happens regardless (existing behavior)

**Optimization**: Could cache discovered processors map for session.

### Configuration Storage

- JSON serialization only happens on save (infrequent)
- Deserialization only happens on app start
- In-memory StateFlow used during runtime

## Testing Strategy

### Unit Tests

1. **TextMenuConfig serialization/deserialization**
   - Test toJson() / fromJson()
   - Test invalid JSON handling
   - Test missing fields

2. **SettingsStore textMenuConfig flow**
   - Test default config generation
   - Test save/load round-trip
   - Test StateFlow emission

3. **ViewModel move operations**
   - Test moveItemUp/moveItemDown logic
   - Test bounds checking
   - Test order reindexing

### Integration Tests

1. **Menu construction with config**
   - Mock PackageManager responses
   - Test menu item order
   - Test enabled/disabled state

2. **Settings UI interaction**
   - Test toggle switch
   - Test move up/down buttons
   - Test reset dialog

## Migration Strategy

### Existing Users (No Config)

1. App starts, no `PREF_TEXT_MENU_CONFIG` key exists
2. `loadTextMenuConfig()` returns default config
3. On first text selection, menu built with defaults
4. Text processors discovered and added automatically
5. User can access settings anytime to customize

### Future Versions

If data model changes, add version field:

```kotlin
@Serializable
data class TextMenuConfig(
    val version: Int = 1,
    val items: List<MenuItemConfig>
)
```

Can implement migration logic based on version number.

## Accessibility Considerations

### Screen Reader Support

- Menu item titles read correctly
- Disabled state announced ("Copy, disabled")
- Move up/down buttons have clear content descriptions
- Reset dialog fully accessible

### Keyboard Navigation

- Settings screen navigable via tab
- Switch toggles work with space/enter
- Dialog buttons keyboard accessible

### Visual Indicators

- Disabled items shown with reduced opacity or color change
- Clear visual feedback for reorder operations
- High contrast support for switches

## Security Considerations

### Text Processor Validation

**Risk**: Malicious app could register ACTION_PROCESS_TEXT handler.

**Mitigation**:
- Android's permission system handles this
- User sees which app is launching
- Text copied to clipboard is temporary

### Configuration Tampering

**Risk**: User could manually edit SharedPreferences.

**Mitigation**:
- JSON validation on load
- Fall back to defaults on parse failure
- No security-sensitive data in configuration

## Recommendations

### Phase 5.5 (UI/UX Design) Focus

1. **Visual Design**: Create mockups for settings screen
2. **Reordering UI**: Finalize up/down arrows vs drag-and-drop
3. **Default Discovery Flow**: Design first-time user experience
4. **Reset Dialog**: Finalize wording and design

### Phase 6 (Specification) Include

1. **Complete data model spec** with all fields
2. **Menu construction algorithm** detailed pseudocode
3. **Text processor discovery** and merge logic
4. **Migration plan** for existing users
5. **Testing checklist** for all edge cases

### Alternative: Drag-and-Drop Research

If deciding to implement drag-and-drop instead of arrows:

**Library Option**: `sh.calvin.reorderable:reorderable:2.4.0`
- Well-maintained
- Compose-first
- Good documentation
- ~50KB added to APK

**Custom Implementation**: Complex, not recommended unless strong requirement.

## Conclusion

The integration points are clear and follow existing app patterns. The implementation requires:

1. **Data layer**: TextMenuConfig model with JSON serialization
2. **Storage layer**: SettingsStore flow following existing patterns
3. **UI layer**: Dedicated settings screen with list reordering
4. **Menu layer**: Modify FeederTextToolbar to apply configuration

The architecture is sound and extends naturally from existing code. Main complexity is in the text processor lifecycle handling (apps being installed/uninstalled), which is addressed with graceful degradation and dynamic discovery.

**Ready to proceed to Phase 5.5 (UI/UX Design) to finalize visual design and interaction patterns.**
