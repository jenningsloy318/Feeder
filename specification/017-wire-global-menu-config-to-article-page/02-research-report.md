# Research Report: Wire Global Menu Config to Article Page

**Document Version**: 1.0
**Date**: 2026-01-04
**Status**: Complete
**Current Time**: 2026-01-04 23:55:30

---

## 1. Executive Summary

This report documents research findings for integrating the Selection Menu Configuration (specs 15-16) with the article page text selection menu. The research reveals a clear integration path with minimal architectural changes required.

### Key Findings
- Text selection menu is implemented in `FeederTextActionModeCallback`
- Configuration infrastructure already exists (MenuConfig, MenuDiscoveryService, SelectionMenuItem)
- Integration point identified: Modify `FeederTextActionModeCallback.onCreateActionMode()`
- No new dependencies required
- Backward compatibility achievable with fallback to defaults

---

## 2. Current Architecture Analysis

### 2.1 Text Selection Menu Implementation

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Key Components**:

```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    // Provides custom TextToolbar to Compose content
    CompositionLocalProvider(LocalTextToolbar provides FeederTextToolbar(...))
}

class FeederTextToolbar : TextToolbar {
    // Wraps ActionMode.Callback for text selection
    private val textActionModeCallback: FeederTextActionModeCallback
}

class FeederTextActionModeCallback : ActionMode.Callback {
    // Builds menu items in onCreateActionMode()
    // Handles clicks in onActionItemClicked()
    // Discovers third-party apps in addTextProcessors()
}
```

**Current Menu Building Logic**:

1. **System Items**: Added in hardcoded order (Copy → Paste → Cut → SelectAll)
   - Lines 113-124 in `onCreateActionMode()`
   - Uses `MenuItemOption` enum with IDs 0-3

2. **Third-Party Apps**: Discovered via `PackageManager.queryIntentActivities()`
   - Lines 190-221 in `addTextProcessors()`
   - Queries `ACTION_PROCESS_TEXT` handlers
   - Assigns IDs 100+ to third-party items
   - Sorts by display name using `ResolveInfo.DisplayNameComparator`

3. **Menu Interaction**:
   - System actions: Execute callbacks (onCopyRequested, etc.)
   - Third-party apps: Copy text to clipboard, launch with `ACTION_PROCESS_TEXT`

### 2.2 Configuration Infrastructure (from Spec-016)

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/`

**Components**:

#### MenuDiscoveryService.kt
```kotlin
class MenuDiscoveryService(override val di: DI) : DIAware {
    suspend fun discoverAll(): List<SelectionMenuItem>
    private fun discoverSystemMenus(): List<SelectionMenuItem>
    private fun discoverFeederMenus(): List<SelectionMenuItem>
    private fun discoverThirdPartyMenus(): List<SelectionMenuItem>
}
```

**Discovery Logic**:
- System menus: IDs like `"android.intent.action.COPY"`, `"android.intent.action.PASTE"`, etc.
- Feeder menus: IDs like `"com.nononsenseapps.feeder.action.READ_ALOUD"`, `"com.nononsenseapps.feeder.action.TRANSLATE"`
- Third-party: IDs like `"com.anki.droid/.activities.AnkiDroidActivity"` (ComponentName.flattenToString())

#### MenuConfig.kt
```kotlin
@Serializable
data class MenuConfig(
    val order: List<String> = emptyList(),      // Item IDs in display order
    val visibility: Map<String, Boolean> = emptyMap(),  // Item ID → visible
) {
    fun isVisible(itemId: String): Boolean
    fun isEmpty(): Boolean
    fun toJson(): String
    companion object {
        fun fromJson(jsonString: String): MenuConfig
        val Default = MenuConfig()
    }
}
```

**Storage**: SharedPreferences with key `"selection_menu_config"`

#### SelectionMenuItem.kt
```kotlin
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
    val type: MenuType = MenuType.SYSTEM,
    val componentName: ComponentName? = null,
    val packageName: String? = null,
    val order: Int = 0,
    val visible: Boolean = true,
)

enum class MenuType {
    SYSTEM,       // copy, paste, cut, select_all
    APPLICATION,  // read_aloud, translate
    THIRD_PARTY,  // third-party apps
}
```

---

## 3. Integration Strategy

### 3.1 Integration Point

**Target**: `FeederTextActionModeCallback.onCreateActionMode()`

**Current Implementation**:
```kotlin
override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    // Hardcoded system items
    addMenuItem(menu, MenuItemOption.Copy)   // ID 0
    addMenuItem(menu, MenuItemOption.Paste)  // ID 1
    addMenuItem(menu, MenuItemOption.Cut)    // ID 2
    addMenuItem(menu, MenuItemOption.SelectAll)  // ID 3

    // Third-party items (discovered separately)
    addTextProcessors(menu)  // IDs 100+

    return true
}
```

**Proposed Implementation**:
```kotlin
override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    // Load menu configuration
    val config = loadMenuConfig()
    val discoveredItems = runBlocking { menuDiscoveryService.discoverAll() }

    // Filter by visibility
    val visibleItems = discoveredItems.filter { config.isVisible(it.id) }

    // Sort by configured order
    val sortedItems = sortByConfigOrder(visibleItems, config)

    // Build menu from sorted items
    sortedItems.forEach { item ->
        addMenuItemFromConfig(menu, item)
    }

    return true
}
```

### 3.2 Dependency Injection

**Current**: `FeederTextActionModeCallback` is created in `FeederTextToolbar` with:
- Context
- ActivityLauncher
- Callbacks (onCopyRequested, etc.)

**Required**: Inject additional dependencies:
- SharedPreferences (to load MenuConfig)
- MenuDiscoveryService (to discover items)

**Challenge**: `FeederTextActionModeCallback` is not a DI-aware component

**Solution Options**:

1. **Option A**: Pass dependencies through constructor
   ```kotlin
   class FeederTextActionModeCallback(
       val context: Context,
       val activityLauncher: ActivityLauncher,
       val sharedPreferences: SharedPreferences,  // NEW
       val menuDiscoveryService: MenuDiscoveryService,  // NEW
       // ... callbacks
   )
   ```

2. **Option B**: Make `FeederTextActionModeCallback` DI-aware
   ```kotlin
   class FeederTextActionModeCallback(override val di: DI) : DIAware {
       private val context: Context by instance()
       private val activityLauncher: ActivityLauncher by instance()
       private val sp: SharedPreferences by instance()  // NEW
       private val menuDiscoveryService: MenuDiscoveryService by instance()  // NEW
   }
   ```

**Recommendation**: **Option B** for consistency with project patterns (DIAware)

### 3.3 Data Flow

```
User Selects Text
    ↓
FeederTextToolbar.showMenu()
    ↓
FeederTextActionModeCallback.onCreateActionMode()
    ↓
Load MenuConfig from SharedPreferences
    ├─→ Parse JSON
    └─→ Get {order, visibility}
    ↓
MenuDiscoveryService.discoverAll()
    ├─→ System items (copy, paste, etc.)
    ├─→ Feeder items (read_aloud, translate)
    └─→ Third-party items (ACTION_PROCESS_TEXT)
    ↓
Filter: keep only visible items
    └─→ config.isVisible(itemId)
    ↓
Sort: reorder by config.order
    └─→ Map item IDs to positions
    ↓
Build Menu items
    ├─→ System: Execute callbacks (onCopyRequested, etc.)
    ├─→ Feeder: Launch internal actions (TODO: implement)
    └─→ Third-party: Copy text + launch ACTION_PROCESS_TEXT
    ↓
Show menu to user
```

---

## 4. Technical Implementation Details

### 4.1 Loading Configuration

**SharedPreferences Key**: `"selection_menu_config"`

**Loading Function**:
```kotlin
private fun loadMenuConfig(): MenuConfig {
    val json = sp.getString("selection_menu_config", null)
    return if (json != null) {
        MenuConfig.fromJson(json)
    } else {
        MenuConfig.Default  // Fallback to defaults
    }
}
```

### 4.2 Mapping System Items

**Challenge**: Current implementation uses `MenuItemOption` enum with IDs 0-3

**Solution**: Map `SelectionMenuItem` IDs to `MenuItemOption`

```kotlin
private fun mapToMenuItemOption(itemId: String): MenuItemOption? {
    return when (itemId) {
        "android.intent.action.COPY" -> MenuItemOption.Copy
        "android.intent.action.PASTE" -> MenuItemOption.Paste
        "android.intent.action.CUT" -> MenuItemOption.Cut
        "android.intent.action.SELECT_ALL" -> MenuItemOption.SelectAll
        else -> null  // Not a system item
    }
}
```

### 4.3 Mapping Feeder Items

**Challenge**: Feeder items (read_aloud, translate) don't have handlers yet

**Solution**: Add new `MenuItemOption` entries or handle separately

```kotlin
private fun handleFeederItem(item: SelectionMenuItem) {
    when (item.id) {
        "com.nononsenseapps.feeder.action.READ_ALOUD" -> {
            // TODO: Trigger read aloud functionality
        }
        "com.nononsenseapps.feeder.action.TRANSLATE" -> {
            // TODO: Trigger translate functionality
        }
    }
}
```

**Note**: Feeder items may need placeholder implementation for this spec (future work: actual handlers)

### 4.4 Sorting by Configured Order

```kotlin
private fun sortByConfigOrder(
    items: List<SelectionMenuItem>,
    config: MenuConfig,
): List<SelectionMenuItem> {
    if (config.isEmpty()) {
        // First load: return default order (system → feeder → third-party)
        return items.sortedBy { it.type }
    }

    // Create a map of ID to position
    val orderMap = config.order.mapIndexed { index, id ->
        id to index
    }.toMap()

    // Sort items by their position in config.order
    return items.sortedBy { item ->
        orderMap[item.id] ?: Int.MAX_VALUE  // New items at end
    }
}
```

### 4.5 Building Menu Items

```kotlin
private fun addMenuItemFromConfig(
    menu: Menu,
    item: SelectionMenuItem,
    index: Int,
) {
    when (item.type) {
        MenuType.SYSTEM -> {
            val option = mapToMenuItemOption(item.id)
            if (option != null && hasCallback(option)) {
                menu.add(0, option.id, index, item.name)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
        MenuType.APPLICATION -> {
            // Feeder items (read_aloud, translate)
            val id = 200 + index  // Use ID range 200+ for Feeder items
            menu.add(2, id, index, item.name)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            // Store item for click handling
            feederItems[id] = item
        }
        MenuType.THIRD_PARTY -> {
            // Third-party apps
            val id = 100 + index  // Use ID range 100+ for third-party
            menu.add(1, id, index, item.name)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            // Store ComponentName for click handling
            textProcessors.add(item.componentName)
        }
    }
}
```

### 4.6 Handling Clicks

**Modified**: `onActionItemClicked()`

```kotlin
override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
    val itemId = item!!.itemId

    when {
        itemId < 100 -> {
            // System items (0-3)
            when (itemId) {
                MenuItemOption.Copy.id -> onCopyRequested?.invoke()
                MenuItemOption.Paste.id -> onPasteRequested?.invoke()
                MenuItemOption.Cut.id -> onCutRequested?.invoke()
                MenuItemOption.SelectAll.id -> onSelectAllRequested?.invoke()
            }
        }
        itemId in 100..199 -> {
            // Third-party apps
            handleThirdPartyClick(itemId)
        }
        itemId in 200..299 -> {
            // Feeder items
            handleFeederItemClick(itemId)
        }
    }

    mode?.finish()
    return true
}

private fun handleFeederItemClick(itemId: Int) {
    val item = feederItems[itemId] ?: return

    when (item.id) {
        "com.nononsenseapps.feeder.action.READ_ALOUD" -> {
            // TODO: Trigger read aloud
        }
        "com.nononsenseapps.feeder.action.TRANSLATE" -> {
            // TODO: Trigger translate
        }
    }
}
```

---

## 5. Backward Compatibility

### 5.1 First Launch (No Configuration)

**Behavior**: Show all items in default order

```kotlin
val config = loadMenuConfig()  // Returns MenuConfig.Default if empty

if (config.isEmpty()) {
    // Use default order: system → feeder → third-party (sorted by name)
    val items = menuDiscoveryService.discoverAll()
    val sortedItems = items.sortedWith(
        compareBy<SelectionMenuItem> { it.type }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )
    // Build menu from sortedItems
}
```

### 5.2 Missing Items (App Uninstalled)

**Behavior**: Skip gracefully

```kotlin
val visibleItems = discoveredItems.filter { item ->
    config.isVisible(item.id) && item.isAvailable()
}

private fun SelectionMenuItem.isAvailable(): Boolean {
    return when (type) {
        MenuType.THIRD_PARTY -> {
            try {
                packageManager.getApplicationInfo(packageName!!, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        else -> true  // System and Feeder items always available
    }
}
```

### 5.3 New Items (Newly Installed Apps)

**Behavior**: Append at end as visible

```kotlin
val configItems = config.order.mapNotNull { id ->
    discoveredItems.find { it.id == id }
}

val newItems = discoveredItems.filterNot { item ->
    config.order.contains(item.id)
}

val allItems = configItems + newItems  // New items at end
```

---

## 6. Performance Considerations

### 6.1 Menu Construction Time

**Current**: < 50ms (hardcoded items + PackageManager query)

**Expected with Config**: < 100ms
- SharedPreferences read: < 10ms
- JSON parsing: < 10ms
- Menu discovery: ~50ms ( PackageManager query)
- Filtering/sorting: < 10ms
- Menu building: < 20ms

**Optimization**: Cache discovered items for article session

### 6.2 Caching Strategy

```kotlin
class FeederTextActionModeCallback : ActionMode.Callback {
    private var cachedDiscoveredItems: List<SelectionMenuItem>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5_000  // 5 seconds

    private suspend fun getDiscoveredItems(): List<SelectionMenuItem> {
        val now = System.currentTimeMillis()
        if (cachedDiscoveredItems != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            return cachedDiscoveredItems!!
        }

        val items = menuDiscoveryService.discoverAll()
        cachedDiscoveredItems = items
        cacheTimestamp = now
        return items
    }
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

**Test Cases**:
1. `loadMenuConfig()` parses JSON correctly
2. `sortByConfigOrder()` sorts items by config
3. `mapToMenuItemOption()` maps system items
4. Filter logic hides invisible items
5. Missing items are skipped gracefully
6. New items are appended to end

### 7.2 Integration Tests

**Test Scenarios**:
1. User toggles item off → item doesn't appear in menu
2. User reorders items → menu shows new order
3. User installs new app → app appears in menu
4. User uninstalls app → app doesn't crash menu
5. No config → menu shows defaults

### 7.3 Manual Testing

**Test Cases**:
1. Select text in article
2. Verify menu items match configuration
3. Toggle items in settings
4. Return to article and select text again
5. Verify updated menu

---

## 8. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| DI injection breaks existing code | High | Low | Make constructor backward compatible |
| Performance regression | Medium | Low | Cache discovered items, measure timing |
| Third-party app discovery inconsistent | Low | Medium | Reuse MenuDiscoveryService logic |
| Config parsing failures | Medium | Low | Fallback to defaults |
| ID conflicts (system vs feeder vs third-party) | Medium | Low | Use separate ID ranges (0-3, 100-199, 200-299) |

---

## 9. Implementation Checklist

### Phase 1: Setup
- [ ] Make `FeederTextActionModeCallback` DI-aware
- [ ] Inject SharedPreferences and MenuDiscoveryService
- [ ] Add `loadMenuConfig()` function
- [ ] Test DI injection works

### Phase 2: Core Integration
- [ ] Modify `onCreateActionMode()` to load config
- [ ] Call `MenuDiscoveryService.discoverAll()`
- [ ] Filter items by visibility
- [ ] Sort items by configured order
- [ ] Build menu from sorted items

### Phase 3: System Items
- [ ] Map system item IDs to MenuItemOption
- [ ] Add system items in configured order
- [ ] Verify copy/paste/cut/selectAll work

### Phase 4: Feeder Items
- [ ] Add feeder items (read_aloud, translate)
- [ ] Assign ID range 200-299
- [ ] Add placeholder handlers (or implement if available)
- [ ] Test feeder items appear in menu

### Phase 5: Third-Party Items
- [ ] Add third-party items in configured order
- [ ] Preserve ComponentName for click handling
- [ ] Verify third-party apps launch correctly

### Phase 6: Edge Cases
- [ ] Handle missing config (use defaults)
- [ ] Handle missing items (skip gracefully)
- [ ] Handle new items (append to end)
- [ ] Test app install/uninstall scenarios

### Phase 7: Testing
- [ ] Unit tests for config loading
- [ ] Unit tests for filtering/sorting
- [ ] Integration tests for menu building
- [ ] Manual testing on device

---

## 10. Recommendations

### 10.1 Architecture

1. **Use DI-aware approach** for `FeederTextActionModeCallback` (Option B)
2. **Reuse existing components**: MenuDiscoveryService, MenuConfig, SelectionMenuItem
3. **Separate concerns**: Config loading, filtering, sorting, menu building

### 10.2 Implementation Order

1. Start with system items (lowest risk)
2. Add third-party items (medium risk)
3. Add Feeder items (may need placeholder handlers)
4. Handle edge cases (missing config, new items, etc.)

### 10.3 Future Enhancements

1. **Real-time config updates**: Observe SharedPreferences changes
2. **Per-article config**: Allow different menus for different articles
3. **Menu preview**: Show menu in settings before applying
4. **Feeder item handlers**: Implement actual read_aloud/translate functionality

---

## 11. Conclusion

The research reveals a **straightforward integration path** with minimal architectural changes:

- **Integration Point**: `FeederTextActionModeCallback.onCreateActionMode()`
- **Dependencies**: Already exist (MenuConfig, MenuDiscoveryService, SelectionMenuItem)
- **Complexity**: Low to Medium
- **Risk**: Low (backward compatible, fallback to defaults)
- **Effort**: Estimated 4-6 hours for implementation + testing

**Next Steps**: Proceed to Code Assessment (Phase 5) to validate findings and identify any additional complexities.

---

**Research Complete**: 2026-01-04 23:55:30
**Researcher**: Coordinator Agent
**Status**: ✅ Ready for Code Assessment
