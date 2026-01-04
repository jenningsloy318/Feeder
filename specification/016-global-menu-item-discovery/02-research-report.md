# Research Report: Global Menu Item Discovery and Display

## Executive Summary
This report documents research findings for implementing global menu item discovery and display in the Feeder application. The research covers Android text processing APIs, persistence mechanisms, drag-and-drop UI patterns, and Compose libraries.

**Research Date**: 2026-01-04
**Researcher**: AI Development Team
**Status**: Complete

## Table of Contents
1. [Android Text Processing Discovery](#1-android-text-processing-discovery)
2. [Persistence Mechanisms](#2-persistence-mechanisms)
3. [Drag-and-Drop UI Libraries](#3-drag-and-drop-ui-libraries)
4. [Compose Best Practices](#4-compose-best-practices)
5. [Accessibility Considerations](#5-accessibility-considerations)
6. [Performance Optimization](#6-performance-optimization)
7. [Testing Strategies](#7-testing-strategies)
8. [Recommended Implementation Approach](#8-recommended-implementation-approach)

---

## 1. Android Text Processing Discovery

### 1.1 Existing Implementation: FeederTextActionModeCallback

**Location**: `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallback.kt`

**Key Discovery Methods**:

```kotlin
private fun addTextProcessors(menu: Menu) {
    textProcessors.clear()

    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }.sortedWith(displayNameComparator)
        .forEachIndexed { index, info ->
            val label = info.loadLabel(packageManager)
            val id = 100 + index
            // Add menu item...

            textProcessors.add(
                ComponentName(
                    info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name,
                ),
            )
        }
}
```

**Key Insights**:
1. **Intent-based Discovery**: Uses `ACTION_PROCESS_TEXT` to find third-party apps
2. **PackageManager API**: `queryIntentActivities()` returns all apps handling text
3. **Sorting**: Uses `ResolveInfo.DisplayNameComparator` for consistent ordering
4. **Component Identification**: Stores `ComponentName` (package + activity) for each app
5. **SDK Version Handling**: Different APIs for Android 13+ (TIRAMISU) vs older versions

### 1.2 System Menu Items

**Built-in Android Actions**:
- Copy: `android.R.string.copy`
- Paste: `android.R.string.paste`
- Cut: `android.R.string.cut`
- Select All: `android.R.string.selectAll`

**Discovery Approach**:
- These are not discoverable via PackageManager
- Must be hard-coded as system items
- Always available on Android platform

### 1.3 Feeder Application Menus

**Existing Features**:
1. **Read Aloud**: TTS functionality for article text
2. **Translate**: AI-powered translation feature

**Discovery Approach**:
- Not discoverable via PackageManager (internal features)
- Must be manually registered with unique IDs
- Use string resources for localization

### 1.4 Third-Party App Examples

**Known Apps with ACTION_PROCESS_TEXT**:
- Anki: Flashcard creation from selected text
- AnkiQuick: Quick Anki card creation
- Google Translate: Translate selected text
- Dictionary apps: Look up words

**Data Available from ResolveInfo**:
```kotlin
data class ResolveInfo(
    val activityInfo: ActivityInfo,
    // Contains:
    // - packageName: String
    // - name: String (activity name)
    // - applicationInfo: ApplicationInfo
)

// Load display label
val label = resolveInfo.loadLabel(packageManager)

// Load icon
val icon = resolveInfo.loadIcon(packageManager)
```

---

## 2. Persistence Mechanisms

### 2.1 Current Approach: SharedPreferences

**Location**: `/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Pattern Used**:
```kotlin
class SettingsStore(override val di: DI) : DIAware {
    private val sp: SharedPreferences by instance()

    private val _someSetting = MutableStateFlow(
        sp.getString(PREF_SOME_SETTING, null)
    )
    val someSetting: StateFlow<String?> = _someSetting.asStateFlow()

    fun setSomeSetting(value: String) {
        sp.edit().putString(PREF_SOME_SETTING, value).apply()
        _someSetting.value = value
    }
}
```

**Key Features**:
1. **Kodein DI Integration**: SharedPreferences injected via DI
2. **StateFlow Wrapper**: MutableStateFlow for reactive updates
3. **Immediate Persistence**: `apply()` for asynchronous writes
4. **Initial Load**: Read from SP on initialization

### 2.2 Recommended Approach for Menu Order

**Storage Format**: JSON string in SharedPreferences

**Schema**:
```kotlin
@Serializable
data class MenuOrder(
    val system: List<String>,      // e.g., ["copy", "paste", "cut", "select_all"]
    val feeder: List<String>,       // e.g., ["read_aloud", "translate"]
    val thirdParty: List<String>    // e.g., ["com.anki/.AddCard", "..."]
)

// Serialize to JSON
val json = Json.encodeToString(menuOrder)

// Store in SharedPreferences
sp.edit().putString(PREF_MENU_ORDER, json).apply()
```

**Advantages**:
- Simple to implement
- No migration needed (JSON is flexible)
- Human-readable for debugging
- Works with existing SP infrastructure

### 2.3 Alternative: DataStore (Not Recommended)

**Why Not DataStore for This Feature**:
- Project already uses SharedPreferences extensively
- Migration cost outweighs benefits for simple JSON storage
- SharedPreferences is synchronous and sufficient for small data
- DataStore would require additional dependencies and setup

**Decision**: Use SharedPreferences for consistency with existing codebase.

---

## 3. Drag-and-Drop UI Libraries

### 3.1 Option 1: androidx.compose.foundation.gestures (Built-in)

**Approach**: Manual drag gesture detection

```kotlin
@Composable
fun DraggableItem(
    onDrag: (offset: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Vertical,
                onDragStarted = { isDragging = true },
                onDragStopped = {
                    isDragging = false
                    onDrag(Offset(offsetX, 0f))
                    offsetX = 0f
                },
                onDragCanceled = {
                    isDragging = false
                    offsetX = 0f
                }
            )
    ) {
        // Item content
    }
}
```

**Pros**:
- Built-in, no dependencies
- Full control over behavior
- Lightweight

**Cons**:
- Complex to implement correctly
- Must handle all edge cases manually
- No built-in visual feedback

### 3.2 Option 2: org.burnoutcrew.composereorderable (Recommended)

**Library**: `com.better.android:reorderable`

**Usage**:
```kotlin
@Composable
fun ReorderableList(
    items: List<MenuItem>,
    onMove: (from: Int, to: Int) -> Unit
) {
    val state = rememberReorderableLazyListState(onMove = onMove)

    LazyColumn(state = state) {
        items(items, key = { it.id }) { item ->
            ReorderableItem(state, key = item.id) { isDragging ->
                MenuItemRow(
                    item = item,
                    modifier = Modifier.draggedItem(isDragging)
                )
            }
        }
    }
}
```

**Pros**:
- Designed for Compose
- Handles complex animations automatically
- Section-aware reordering (constrain to sections)
- Well-maintained library
- Active community

**Cons**:
- External dependency
- Larger bundle size (+50KB)

**Decision**: Use `com.better.android:reorderable` for better UX and reduced development time.

### 3.3 Implementation with Sections

**Section-Constrained Reordering**:
```kotlin
val state = rememberReorderableLazyListState(onMove = { from, to ->
    // Only allow reordering within sections
    val fromSection = items[fromIndex].section
    val toSection = items[toIndex].section

    if (fromSection == toSection) {
        onMove(from, to)
        true
    } else {
        false // Block cross-section moves
    }
})
```

---

## 4. Compose Best Practices

### 4.1 State Management

**Pattern from Project**: ViewState + Event

```kotlin
@Immutable
data class SelectionMenuViewState(
    val isLoading: Boolean = false,
    val items: List<SelectionMenuItem> = emptyList(),
    val error: String? = null,
)

sealed class SelectionMenuEvent {
    data object LoadMenus : SelectionMenuEvent()
    data class ReorderMenu(val from: Int, val to: Int) : SelectionMenuEvent()
}

class SelectionMenuSettingsViewModel : DIAwareViewModel(di) {
    private val _viewState = MutableStateFlow(SelectionMenuViewState())
    val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()

    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.LoadMenus -> loadMenus()
            is SelectionMenuEvent.ReorderMenu -> reorderMenus(event.from, event.to)
        }
    }
}
```

### 4.2 Coroutine Usage

**Discovery on ViewModel Init**:
```kotlin
init {
    viewModelScope.launch {
        loadMenus()
    }
}

private suspend fun loadMenus() {
    _viewState.update { it.copy(isLoading = true) }

    withContext(Dispatchers.Default) {
        val systemItems = discoverSystemMenus()
        val feederItems = discoverFeederMenus()
        val thirdPartyItems = discoverThirdPartyMenus()

        val order = loadMenuOrder()

        val allItems = mergeWithOrder(systemItems, feederItems, thirdPartyItems, order)

        _viewState.update { it.copy(isLoading = false, items = allItems) }
    }
}
```

### 4.3 Composable Structure

**Screen → Content → Item Pattern**:
```kotlin
@Composable
fun SelectionMenuSettingsScreen(...) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(topBar = { ... }) { padding ->
        SelectionMenuContent(
            viewState = viewState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun SelectionMenuContent(...) {
    if (viewState.isLoading) {
        LoadingIndicator()
    } else {
        ReorderableMenuList(
            items = viewState.items,
            onReorder = { from, to -> viewModel.onEvent(SelectionMenuEvent.ReorderMenu(from, to)) }
        )
    }
}

@Composable
private fun MenuItemRow(item: SelectionMenuItem, ...) {
    Row {
        Icon(item.icon)
        Column {
            Text(item.name)
            Text(item.description)
        }
        Badge(item.type)
        DragHandle()
    }
}
```

### 4.4 Immutable Data Classes

**Use @Immutable for Performance**:
```kotlin
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val type: MenuType,
    // ...
)
```

**Benefits**:
- Enables Compose optimizations
- Prevents accidental mutations
- Thread-safe

---

## 5. Accessibility Considerations

### 5.1 Drag-and-Drop Alternatives

**Requirement**: Screen reader users need alternative reordering method

**Solution 1: Move Up/Down Buttons**
```kotlin
@Composable
fun MenuItemWithMoveButtons(item: SelectionMenuItem) {
    Row {
        IconButton(
            onClick = { onMoveUp(item.id) },
            contentDescription = "Move ${item.name} up"
        ) {
            Icon(Icons.Default.ArrowUpward, null)
        }

        MenuItemContent(item)

        IconButton(
            onClick = { onMoveDown(item.id) },
            contentDescription = "Move ${item.name} down"
        ) {
            Icon(Icons.Default.ArrowDownward, null)
        }
    }
}
```

**Solution 2: Long-Press Dialog**
```kotlin
var showMoveDialog by remember { mutableStateOf(false) }

if (showMoveDialog) {
    MoveItemDialog(
        currentItem = item,
        allItems = items,
        onMoveTo = { newPosition ->
            onMove(item.position, newPosition)
            showMoveDialog = false
        },
        onDismiss = { showMoveDialog = false }
    )
}
```

### 5.2 Screen Reader Semantics

**Proper Labels**:
```kotlin
Modifier.semantics {
    this.contentDescription = "${item.name}, ${item.type}, menu item"
    this.heading()
    this.editable() // Indicate can be reordered
}
```

**Drag Region**:
```kotlin
Modifier.draggable(
    startDragImmediately = true,
    onDragStarted = { ... }
).semantics {
    this.contentDescription = "Drag to reorder ${item.name}"
    this.role = Role.Button
}
```

### 5.3 Keyboard Navigation

**Support D-Pad and Keyboard**:
```kotlin
Modifier.focusable()
.onKeyEvent { keyEvent ->
    when (keyEvent.key) {
        Key.DirectionUp -> {
            onMoveUp()
            true
        }
        Key.DirectionDown -> {
            onMoveDown()
            true
        }
        else -> false
    }
}
```

---

## 6. Performance Optimization

### 6.1 Discovery Caching

**Problem**: PackageManager queries can be slow (100-500ms)

**Solution**: Cache results with invalidation

```kotlin
class MenuDiscoveryCache(private val context: Context) {
    private var cachedItems: List<ThirdPartyMenuItem>? = null
    private var cacheTimestamp = 0L
    private val CACHE_DURATION = 5_000L // 5 seconds

    fun getThirdPartyMenus(): List<ThirdPartyMenuItem> {
        val now = System.currentTimeMillis()

        if (cachedItems != null && (now - cacheTimestamp) < CACHE_DURATION) {
            return cachedItems!!
        }

        val items = performDiscovery()
        cachedItems = items
        cacheTimestamp = now
        return items
    }

    fun invalidate() {
        cachedItems = null
    }
}
```

### 6.2 Lazy Loading

**Use LazyColumn with Keys**:
```kotlin
LazyColumn {
    items(
        items = viewState.items,
        key = { it.id } // Stable key for reordering
    ) { item ->
        MenuItemRow(item)
    }
}
```

### 6.3 Debounce Saves

**Prevent Excessive Writes**:
```kotlin
private var saveJob: Job? = null

fun saveMenuOrder(order: MenuOrder) {
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
        delay(2_000) // Wait 2 seconds
        sp.edit {
            putString(PREF_MENU_ORDER, Json.encodeToString(order))
        }
    }
}
```

### 6.4 Minimize Recomposition

**Use Stable Keys**:
```kotlin
@Composable
fun MenuItemRow(
    item: SelectionMenuItem,
    modifier: Modifier = Modifier
) {
    // Don't use 'item' directly in derivedStateOf
    val title by remember(item.id) { derivedStateOf { item.name } }

    Text(title)
}
```

---

## 7. Testing Strategies

### 7.1 Unit Tests

**Test Menu Discovery**:
```kotlin
class MenuDiscoveryTest {
    @Test
    fun `discovers system menus`() {
        val items = discoverSystemMenus()
        assertEquals(4, items.size)
        assertTrue(items.any { it.id == "copy" })
        assertTrue(items.any { it.id == "paste" })
    }

    @Test
    fun `discovers feeder menus`() {
        val items = discoverFeederMenus()
        assertEquals(2, items.size)
        assertTrue(items.any { it.id == "read_aloud" })
        assertTrue(items.any { it.id == "translate" })
    }
}
```

**Test Order Persistence**:
```kotlin
class MenuOrderTest {
    @Test
    fun `saves and loads menu order`() {
        val order = MenuOrder(
            system = listOf("copy", "paste"),
            feeder = listOf("read_aloud"),
            thirdParty = emptyList()
        )

        saveMenuOrder(order)
        val loaded = loadMenuOrder()

        assertEquals(order, loaded)
    }

    @Test
    fun `removes invalid items from saved order`() {
        val saved = MenuOrder(
            system = listOf("copy", "invalid_item"),
            feeder = emptyList(),
            thirdParty = emptyList()
        )

        val cleaned = mergeWithSavedOrder(saved, currentItems)

        assertEquals(1, cleaned.system.size)
        assertEquals("copy", cleaned.system[0])
    }
}
```

### 7.2 UI Tests

**Test Drag and Drop**:
```kotlin
@Test
fun dragItem_reordersList() {
    composeTestRule.setContent {
        SelectionMenuSettingsScreen(viewModel = viewModel)
    }

    // Drag first item to second position
    composeTestRule.onNodeWithText("Copy")
        .performTouchInput {
            swipeWithVelocity(
                start = Offset(x = center.x, y = center.y),
                end = Offset(x = center.x, y = center.y + 200f),
                endVelocity = 1000f
            )
        }

    // Verify order changed
    composeTestRule.onChildAt(0)
        .assertIsNotDisplayed()
        .assertTextContains("Paste")
}
```

### 7.3 Integration Tests

**Test End-to-End Flow**:
```kotlin
@Test
fun completeWorkflow_works() {
    // 1. Load screen
    viewModel.onEvent(LoadMenus)
    assertEquals(4, viewModel.viewState.value.items.size)

    // 2. Reorder items
    viewModel.onEvent(ReorderMenu(0, 1))
    val newOrder = viewModel.viewState.value.items

    // 3. Verify order saved
    val saved = loadMenuOrder()
    assertEquals(newOrder.map { it.id }, saved.system)

    // 4. Reload and verify persistence
    val newViewModel = SelectionMenuSettingsViewModel(di)
    newViewModel.onEvent(LoadMenus)
    assertEquals(newOrder, newViewModel.viewState.value.items)
}
```

---

## 8. Recommended Implementation Approach

### 8.1 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SelectionMenuSettingsScreen              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         SelectionMenuSettingsViewModel              │   │
│  │  - Manages state (ViewState + Events)              │   │
│  │  - Discovers menu items                            │   │
│  │  - Handles reordering                              │   │
│  │  - Persists order to SharedPreferences             │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MenuDiscoveryService                   │   │
│  │  - Queries PackageManager for third-party apps     │   │
│  │  - Returns system, feeder, and third-party items   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Data Flow

```
User Opens Screen
       │
       ▼
ViewModel Init → Send LoadMenus Event
       │
       ▼
MenuDiscoveryService.discoverAllMenus()
       │
       ├──→ discoverSystemMenus() → List<MenuItem>
       ├──→ discoverFeederMenus() → List<MenuItem>
       └──→ discoverThirdPartyMenus() → List<MenuItem>
       │
       ▼
Load Saved Order from SharedPreferences
       │
       ▼
Merge Discovered Items with Saved Order
       │
       ▼
Update ViewState (items = merged list)
       │
       ▼
UI Renders List with Drag Handles
       │
       ▼
User Drags Item → Send ReorderMenu Event
       │
       ▼
ViewModel Reorders Items in ViewState
       │
       ▼
Debounced Save to SharedPreferences (2 seconds)
       │
       ▼
UI Updates with New Order
```

### 8.3 Implementation Checklist

**Phase 1: Data Model**
- [ ] Extend `SelectionMenuItem` with `MenuType`, `componentName`, `packageName`, `order`
- [ ] Create `MenuType` enum (SYSTEM, APPLICATION, THIRD_PARTY)
- [ ] Create `MenuOrder` data class for JSON serialization

**Phase 2: Discovery Service**
- [ ] Create `MenuDiscoveryService` class
- [ ] Implement `discoverSystemMenus()` (hard-coded)
- [ ] Implement `discoverFeederMenus()` (hard-coded)
- [ ] Implement `discoverThirdPartyMenus()` (PackageManager query)
- [ ] Add sorting by display name for third-party items
- [ ] Add caching layer (5-second cache)

**Phase 3: ViewModel Logic**
- [ ] Implement `loadMenus()` event handler
- [ ] Implement `reorderMenus()` event handler
- [ ] Add SharedPreferences for order persistence
- [ ] Implement debounced save (2 seconds)
- [ ] Handle merge logic (saved order + discovered items)

**Phase 4: UI Implementation**
- [ ] Add `com.better.android:reorderable` dependency
- [ ] Replace empty state with `LazyColumn`
- [ ] Implement section headers (System, Feeder, Third-Party)
- [ ] Implement `MenuItemRow` composable with drag handle
- [ ] Implement badge for source type
- [ ] Add loading indicator
- [ ] Add error handling display

**Phase 5: Accessibility**
- [ ] Add move up/down buttons for screen readers
- [ ] Add proper semantics labels
- [ ] Support keyboard navigation (D-Pad)
- [ ] Test with TalkBack enabled

**Phase 6: Testing**
- [ ] Unit tests for discovery logic
- [ ] Unit tests for order persistence
- [ ] UI tests for drag-and-drop
- [ ] Integration tests for complete flow
- [ ] Accessibility tests

**Phase 7: Polish**
- [ ] Optimize performance (caching, debouncing)
- [ ] Add animations for reordering
- [ ] Handle edge cases (no apps, missing labels)
- [ ] Localization for all strings
- [ ] Error handling and recovery

### 8.4 Dependencies to Add

```kotlin
// build.gradle.kts (app module)

dependencies {
    // Drag-and-drop library
    implementation("com.better.android:reorderable:1.2.0")

    // Kotlinx Serialization (for JSON)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // (Already in project: Compose, Kodein DI, Coroutines)
}
```

### 8.5 Strings to Add

```xml
<!-- res/values/strings.xml -->

<!-- Section Headers -->
<string name="selection_menu_section_system">System Actions</string>
<string name="selection_menu_section_feeder">Feeder Features</string>
<string name="selection_menu_section_third_party">Third-Party Apps</string>

<!-- Menu Item Names -->
<string name="selection_menu_copy">Copy</string>
<string name="selection_menu_paste">Paste</string>
<string name="selection_menu_cut">Cut</string>
<string name="selection_menu_select_all">Select All</string>
<string name="selection_menu_read_aloud">Read Aloud</string>
<string name="selection_menu_translate">Translate</string>

<!-- Empty State -->
<string name="selection_menu_no_items">Unable to find any menu items</string>
<string name="selection_menu_no_items_hint">Make sure other apps are installed and text processing is enabled.</string>

<!-- Save Indicator -->
<string name="selection_menu_order_saved">Order saved</string>

<!-- Accessibility -->
<string name="selection_menu_move_up">Move %s up</string>
<string name="selection_menu_move_down">Move %s down</string>
<string name="selection_menu_drag_to_reorder">Drag to reorder %s</string>
```

---

## 9. Risks and Mitigation

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| PackageManager query slow on low-end devices | Medium | Medium | Use caching, show loading indicator, run on Dispatchers.Default |
| Third-party app missing label/icon | Low | Low | Use package name as fallback, handle exceptions |
| Drag-and-drop library has bugs | Low | High | Thorough testing, fallback to manual implementation if needed |
| SharedPreferences corruption | Very Low | Medium | Validate JSON on load, use try-catch, provide reset option |
| Cross-section reordering causes confusion | Medium | Low | Visual separation (dividers), prevent cross-section moves in code |
| Accessibility not fully supported | Medium | Medium | Provide move up/down buttons, test with TalkBack |
| Performance issues with many third-party apps | Low | Medium | Lazy loading, pagination if > 50 items |

---

## 10. Open Questions

### Q1: Should we support disabling menu items?
**Answer**: No, out of scope for this feature. All items enabled by default.

### Q2: How do we handle new third-party apps installed after discovery?
**Answer**: Cache invalidation. When user opens screen, re-run discovery (with 5-second cache). New apps will appear on next screen open.

### Q3: Should we show app icons for third-party items?
**Answer**: Yes, load icon from `ResolveInfo.loadIcon()`. Cache icons to avoid repeated loads.

### Q4: What if user has no third-party text processing apps?
**Answer**: Show empty Third-Party section with message "No third-party apps found" or hide section entirely.

### Q5: Should we support importing/exporting menu configurations?
**Answer**: No, out of scope. Future enhancement.

---

## 11. Conclusion

The research confirms that implementing global menu item discovery and display is **feasible and straightforward** using:

1. **Existing Android APIs**: `ACTION_PROCESS_TEXT` + `PackageManager.queryIntentActivities()`
2. **Project Patterns**: SharedPreferences for persistence, ViewState/Event for state management
3. **Mature Library**: `com.better.android:reorderable` for drag-and-drop
4. **Best Practices**: Coroutines, StateFlow, Compose, Accessibility

**Estimated Effort**: 3-5 days of development
- Day 1: Data model, discovery service, ViewModel logic
- Day 2: UI implementation, drag-and-drop integration
- Day 3: Accessibility, error handling, polish
- Day 4: Testing, bug fixes
- Day 5: Code review, documentation, final polish

**Risk Level**: Low
- Well-established patterns
- Mature libraries
- Clear implementation path

**Recommendation**: Proceed with implementation as specified in this research report.

---

## 12. References

### Android Documentation
- [ACTION_PROCESS_TEXT](https://developer.android.com/reference/android/content/Intent#ACTION_PROCESS_TEXT)
- [PackageManager.queryIntentActivities()](https://developer.android.com/reference/android/content/pm/PackageManager#queryIntentActivities(android.content.Intent,%20android.content.pm.PackageManager.ResolveInfoFlags))
- [ResolveInfo](https://developer.android.com/reference/android/content/pm/ResolveInfo)

### Libraries
- [Compose Reorderable](https://github.com/aclassen/ComposeReorderable)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

### Project Files
- `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallback.kt`
- `/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
- `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SelectionMenuSettingsScreen.kt`

---

**Research Complete**: 2026-01-04
**Next Phase**: Code Assessment (Phase 5)
