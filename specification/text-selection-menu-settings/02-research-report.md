# Research Report: Text Selection Menu Settings

**Date:** 2025-11-27 21:54:11 PST
**Phase:** 3 - Research

## Executive Summary

This research analyzed the Feeder app's existing settings architecture, UI patterns, and relevant code to understand how to implement text selection menu settings. Key findings:

1. **Settings Framework**: SharedPreferences via `SettingsStore` with StateFlow-based reactive architecture
2. **Settings UI Pattern**: Consistent Material Design 3 components with `SwitchSetting`, `MenuSetting`, and `ListDialogSetting`
3. **Current Text Toolbar**: Hardcoded menu order in `FeederTextToolbar.kt` with enum-based IDs
4. **No Existing Drag-and-Drop**: No native drag-and-drop list reordering UI in the app
5. **Text Processors**: Dynamically discovered via `ACTION_PROCESS_TEXT` intent query at runtime

## Settings Architecture

### SettingsStore Pattern

**File**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

The app uses a consistent pattern for managing settings:

1. **Storage**: SharedPreferences for persistence
2. **State Management**: MutableStateFlow for reactive updates
3. **Public API**: StateFlow for read-only access, setter methods for updates
4. **Naming Convention**: Constant keys like `PREF_*` for SharedPreferences keys

**Example Pattern**:
```kotlin
private val _showFab = MutableStateFlow(sp.getBoolean(PREF_SHOW_FAB, true))
val showFab = _showFab.asStateFlow()

fun setShowFab(value: Boolean) {
    _showFab.value = value
    sp.edit().putBoolean(PREF_SHOW_FAB, value).apply()
}
```

### Data Types Stored

- **Boolean**: Most common (e.g., `showFab`, `syncOnlyOnWifi`)
- **String**: For serialized data (e.g., `PREF_THEME`, `PREF_SORT`)
- **Int**: For numeric values (e.g., `PREF_MAX_LINES`, `maximumCountPerFeed`)
- **Float**: For scale values (e.g., `PREF_TEXT_SCALE`)

### Settings Groups

All user-configurable settings are tracked in the `UserSettings` enum for OPML import/export compatibility.

## Settings UI Patterns

### File: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

The app follows consistent UI patterns:

### 1. **SettingsGroup** Component

Groups related settings under a header:

```kotlin
SettingsGroup(
    title = R.string.synchronization,
) {
    // Settings items
}
```

Features:
- Consistent visual hierarchy
- Accessibility support with heading semantics
- Horizontal dividers between groups

### 2. **SwitchSetting** Component

Boolean toggle settings:

```kotlin
SwitchSetting(
    title = stringResource(id = R.string.show_fab),
    checked = showFabValue,
    onCheckedChange = onShowFabChange,
    description = "Optional description text",
    enabled = true,
)
```

Features:
- Icon support (optional)
- Title + optional subtitle
- State description for accessibility
- Enable/disable support

### 3. **MenuSetting** Component

Dropdown selection from predefined options:

```kotlin
MenuSetting(
    title = stringResource(id = R.string.theme),
    currentValue = currentThemeValue,
    values = immutableListHolderOf(options...),
    onSelection = onThemeChange,
)
```

Features:
- Dropdown menu with radio-button style selection
- Current value displayed as subtitle
- Bold/colored text for selected item
- Keyboard accessibility (Escape to close)

### 4. **ListDialogSetting** Component

Editable list with add/remove functionality:

```kotlin
ListDialogSetting(
    title = stringResource(id = R.string.block_list),
    dialogTitle = { /* Compose title with description */ },
    currentValue = blockListValue,
    onAddItem = onBlockListAdd,
    onRemoveItem = onBlockListRemove,
)
```

Features:
- Opens `EditableListDialog` on click
- Shows preview of items (limited to 5)
- Add/remove items in dialog

### 5. **ExternalSetting** Component

Navigation to nested settings screens:

```kotlin
ExternalSetting(
    currentValue = "Display value",
    title = stringResource(R.string.text_settings),
    onClick = onTextSettings,
)
```

Used for complex settings that need their own screen.

## Current Text Toolbar Implementation

### File: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

### Menu Structure

**MenuItemOption Enum** (lines 255-277):
```kotlin
internal enum class MenuItemOption(
    val id: Int,
) {
    Copy(0),
    Paste(1),
    Cut(2),
    SelectAll(3),
    ;

    val titleResource: Int
        get() = when (this) {
            Copy -> android.R.string.copy
            Paste -> android.R.string.paste
            Cut -> android.R.string.cut
            SelectAll -> android.R.string.selectAll
        }

    val order = id
}
```

**Key Points**:
- Hardcoded order via enum position and `order` property
- IDs 0-3 reserved for standard menu items
- Text processors get IDs starting from 100

### Text Processor Discovery

**addTextProcessors method** (lines 190-221):
```kotlin
private fun addTextProcessors(menu: Menu) {
    textProcessors.clear()

    val intent =
        Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
        }

    packageManager.queryIntentActivities(intent, ...)
        .sortedWith(displayNameComparator)
        .forEachIndexed { index, info ->
            val label = info.loadLabel(packageManager)
            val id = 100 + index
            menu.add(1, id, id, label)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

            textProcessors.add(
                ComponentName(
                    info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name,
                )
            )
        }
}
```

**Key Points**:
- Text processors discovered dynamically at menu creation time
- Sorted alphabetically by display name
- Each processor identified by ComponentName (package + activity)
- IDs assigned sequentially starting from 100
- All shown in overflow menu (SHOW_AS_ACTION_NEVER)

### Menu Item Handling

Standard items (Copy, SelectAll, etc.) have callbacks (`onCopyRequested`, etc.)
Text processors handled specially (lines 152-180):
- Copy selected text to clipboard
- Launch processor with text via `ACTION_PROCESS_TEXT` intent
- Restore previous clipboard content after

## Research Questions Answered

### 1. What settings framework does Feeder use?

**SharedPreferences** with reactive **StateFlow** pattern in `SettingsStore`.

### 2. How are existing settings organized?

- Grouped by category (Synchronization, Article List Settings, Reader Settings, etc.)
- Each group uses `SettingsGroup` wrapper
- Horizontal dividers between groups
- Settings within groups arranged vertically

### 3. Is there existing drag-and-drop UI?

**No.** Search for drag/reorder patterns found only:
- Pull-to-refresh gestures
- Swipeable items (swipe-to-mark-as-read)
- Navigation drawer swipe
- No list reordering or drag-and-drop UI

**Recommendation**: Need to implement drag-and-drop using Jetpack Compose's foundation APIs or consider alternative UI patterns (e.g., up/down arrow buttons for reordering).

### 4. What is the suggested default order?

Based on user requirements: **User customizable** on first access.

Suggested sensible defaults:
1. **Copy** - Most common action
2. **Select All** - Common pairing with Copy
3. **Text Processors** - Alphabetically sorted (current behavior)

### 5. How are text processor apps detected?

Queried dynamically via PackageManager:
```kotlin
Intent(Intent.ACTION_PROCESS_TEXT).apply { type = "text/plain" }
```

**Important**: Text processors can change (apps installed/uninstalled) between app sessions, so settings must handle:
- New processors appearing
- Previously configured processors being uninstalled
- Processor display names potentially changing

## Architectural Considerations

### Storage Requirements

Need to store:

1. **Built-in menu items** (Copy, SelectAll):
   - Enabled/disabled state (boolean per item)
   - Order/position (integer per item)

2. **Text processors**:
   - ComponentName (package + activity) for identification
   - Enabled/disabled state per processor
   - Order/position per processor

3. **Storage format options**:

**Option A: Separate preferences**
```kotlin
PREF_TEXT_MENU_COPY_ENABLED: Boolean
PREF_TEXT_MENU_COPY_ORDER: Int
PREF_TEXT_MENU_SELECT_ALL_ENABLED: Boolean
PREF_TEXT_MENU_SELECT_ALL_ORDER: Int
PREF_TEXT_MENU_PROCESSORS: String (JSON array)
```

**Option B: Single serialized structure**
```kotlin
PREF_TEXT_MENU_CONFIG: String (JSON)
// {
//   "items": [
//     {"type": "copy", "enabled": true, "order": 0},
//     {"type": "selectAll", "enabled": true, "order": 1},
//     {"type": "processor", "package": "...", "activity": "...", "enabled": true, "order": 2}
//   ]
// }
```

**Recommendation**: Option B (single JSON) for:
- Atomic updates
- Easier ordering logic
- Simpler migration/reset
- Consistent with complex settings like `OpenAISettings`

### State Management

Follow existing patterns:

```kotlin
private val _textMenuConfig = MutableStateFlow(
    TextMenuConfig(
        items = loadFromPreferences() ?: defaultConfig()
    )
)
val textMenuConfig = _textMenuConfig.asStateFlow()

fun setTextMenuConfig(config: TextMenuConfig) {
    _textMenuConfig.value = config
    sp.edit().putString(PREF_TEXT_MENU_CONFIG, config.toJson()).apply()
}
```

### ViewModel Integration

Add to `SettingsViewModel`:

```kotlin
fun setTextMenuConfig(config: TextMenuConfig) {
    repository.setTextMenuConfig(config)
}
```

## UI Design Constraints

### Material Design 3 Compliance

All settings components use Material 3:
- `MaterialTheme.typography.*`
- `MaterialTheme.colorScheme.*`
- Consistent spacing (`LocalDimens.current`)

### Accessibility Requirements

Existing patterns include:
- Semantic roles (Role.Switch, Role.Button, etc.)
- State descriptions for screen readers
- Keyboard navigation support (Escape key handling)
- Clear content descriptions

### Responsive Layout

Settings use `dimens.maxContentWidth` to constrain width on tablets:
```kotlin
modifier.width(dimens.maxContentWidth)
```

## Drag-and-Drop Implementation Options

Since no existing drag-and-drop UI exists, consider:

### Option 1: Jetpack Compose Foundation DragAndDrop (Android 7.0+)

Use Compose's modifier-based drag-and-drop:
```kotlin
modifier.dragAndDropTarget(...)
```

**Pros**: Native, modern, follows Compose patterns
**Cons**: Complex implementation, requires custom visual feedback

### Option 2: Third-party Library

Libraries like `sh.calvin.reorderable` provide ready-made solutions.

**Pros**: Proven, less code
**Cons**: New dependency, maintenance risk

### Option 3: Up/Down Arrows

Simple alternative to drag-and-drop:
```kotlin
Row {
    Text(item.title)
    IconButton(onClick = moveUp) { Icon(Icons.Default.ArrowUpward) }
    IconButton(onClick = moveDown) { Icon(Icons.Default.ArrowDownward) }
}
```

**Pros**: Simple, accessible, no new patterns
**Cons**: Less intuitive for reordering multiple items

**Recommendation**: Research drag-and-drop libraries in Assessment phase, fall back to Option 3 if complexity is too high.

## Navigation Patterns

Text settings currently navigated via:
```kotlin
onNavigateToTextSettingsScreen: () -> Unit
```

Called from Settings.kt:
```kotlin
ExternalSetting(
    currentValue = currentUiFontOption.name,
    title = stringResource(R.string.text_settings),
    onClick = onTextSettings,
)
```

**Pattern**: Use similar `ExternalSetting` or consider inline configuration if UI is simple enough.

## Testing Patterns

Based on `SettingsStoreTest.kt` patterns:
- Unit tests for SettingsStore methods
- Test StateFlow emissions
- Test SharedPreferences persistence
- Mock dependencies (DI-based)

## Key Files to Modify

1. **SettingsStore.kt**: Add text menu config storage/retrieval
2. **FeederTextToolbar.kt**: Read config, apply to menu construction
3. **Settings.kt**: Add settings UI (either inline or link to new screen)
4. **SettingsViewModel.kt**: Add view state and event handlers
5. **strings.xml**: Add new strings for settings UI

## Risks & Challenges

### 1. Text Processor Lifecycle

Text processors can be installed/uninstalled. Settings must:
- Gracefully handle missing processors
- Detect new processors
- Merge user config with discovered processors

### 2. Menu Item Ordering Edge Cases

- What if all items are disabled? (Show grayed-out items per requirements)
- What if order numbers have gaps? (Re-normalize on load)
- What if two items have same order? (Use stable sort with fallback)

### 3. Migration Strategy

Existing users have no saved config:
- First access: Detect no saved config
- Show current default order
- Allow immediate customization
- Save to preferences

### 4. Performance

Menu creation happens on every text selection:
- Keep config lookup fast (in-memory StateFlow)
- Pre-process ordering on config change, not on menu creation
- Cache ComponentName lookups

## Recommendations for Next Phases

### Phase 5: Code Assessment

Focus on:
1. Analyze `FeederTextToolbar` menu construction logic deeply
2. Determine best place to intercept menu building
3. Identify all callbacks and their usage

### Phase 5.5: UI/UX Design

Design decisions needed:
1. **Inline vs Separate Screen**: Simple enough for inline or needs dedicated screen?
2. **Drag-and-Drop vs Arrows**: Which reordering UI to use?
3. **Default Order Dialog**: Show setup wizard on first access?
4. **Reset Confirmation**: Exact wording for reset dialog

### Phase 6: Specification Writing

Include:
1. Detailed data model for TextMenuConfig
2. Migration strategy for existing users
3. Text processor lifecycle handling
4. Disabled item rendering specification

## Appendix: Related Code Snippets

### Existing Settings ViewModel Pattern

```kotlin
// SettingsViewModel.kt - combine flow pattern
combine(
    repository.currentTheme,
    repository.showFab,
    // ... all settings flows
) { params: Array<Any> ->
    SettingsViewState(
        currentTheme = params[0] as ThemeOptions,
        showFab = params[1] as Boolean,
        // ...
    )
}.collect {
    _viewState.value = it
}
```

### Menu Construction in FeederTextToolbar

```kotlin
override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    requireNotNull(menu)
    requireNotNull(mode)

    onCopyRequested?.let {
        addMenuItem(menu, MenuItemOption.Copy)
    }
    // ... other items
    onCopyRequested?.let {
        addTextProcessors(menu)  // Always after built-in items
    }
    return true
}

private fun addMenuItem(menu: Menu, item: MenuItemOption) {
    menu.add(0, item.id, item.order, item.titleResource)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
}
```

**Key insight**: Menu.add() takes `groupId, itemId, order, title`. The `order` parameter controls display order. Currently uses `MenuItemOption.order` (0, 1, 2, 3) and text processors get their index + 100.

## Conclusion

The Feeder app has a well-structured, reactive settings architecture using SharedPreferences + StateFlow. The UI follows consistent Material Design 3 patterns with established components for switches, menus, and lists.

The text selection toolbar currently has hardcoded ordering via enum positions. To implement configurable ordering, we need to:

1. Store user configuration in a serialized format (JSON)
2. Add settings UI (likely a dedicated screen due to drag-and-drop complexity)
3. Modify menu construction to respect stored order and enabled state
4. Handle dynamic text processor lifecycle gracefully

No existing drag-and-drop UI means we'll need to implement this pattern from scratch or use up/down arrows as a simpler alternative.

**Next Phase**: Code Assessment to deeply analyze menu construction and determine exact integration points.
