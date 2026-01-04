# Research Report: Selection Menu Configuration Feature

**Specification ID**: 015
**Feature Name**: Selection Menu Configuration
**Research Date**: 2026-01-04
**Current Date**: 2026-01-04

## Executive Summary

Research conducted on drag-and-drop implementation in Jetpack Compose and Android text selection toolbar customization. Key findings indicate that custom drag-and-drop modifiers are well-documented in the Compose ecosystem, and `ACTION_PROCESS_TEXT` intent handling is a mature Android API.

## 1. Drag-and-Drop in Jetpack Compose

### 1.1 Official Android Guidance

**Source**: [Android Developers - Drag and Drop in Compose](https://developer.android.com/codelabs/codelab-dnd-compose) (2024-10-30)

**Key Findings**:
- Android provides official codelab on drag-and-drop in Compose
- Uses `dragAndDropSource` and `dragAndDropTarget` modifiers
- 4-stage event lifecycle: Started, Continuing, Ended, Exited
- Supports both intra-app and cross-app drag operations

**Relevance to Our Feature**:
The official approach is more suited for dragging items between apps. For list reordering, we need a custom modifier approach.

### 1.2 Custom Modifier for List Reordering

**Source**: [Nutrient - Drag-to-reorder with Compose](https://www.nutrient.io/blog/drag-to-reorder-with-compose/) (2024-08-20)

**Key Findings**:
- Provides custom `Modifier.dragToReorder()` implementation
- Uses `SlideState` enum (NONE, UP, DOWN) to track drag direction
- Implements `pointerInput` with `detectDragGesturesAfterLongPress`
- Calculates item displacement for smooth visual feedback
- Works with vertical scrolling composables (Column, LazyColumn)

**Implementation Pattern**:
```kotlin
enum class SlideState { NONE, UP, DOWN }

fun Modifier.dragToReorder(
    item: T,
    itemList: List<T>,
    itemHeight: Int,
    onMove: (Int, Int) -> Unit,
) = composed {
    // Custom drag gesture detection
    // Visual offset calculation
    // Callback when position changes
}
```

**Relevance**: This is the closest pattern to our needs. We should adapt this approach for our settings screen.

### 1.3 Alternative: Reorderable Libraries

**Source**: [Compose Reorderable Library](https://codergalib2005.medium.com/introducing-compose-reorderable-a-jetpack-compose-library-for-drag-drop-lists-and-grids-01eb60ac87f5) (2025-11-02)

**Key Findings**:
- Third-party library specifically for Compose drag-and-drop
- Supports both lists and grids
- Provides ready-to-use solution

**Relevance**: While convenient, adding a third-party dependency may not align with project philosophy of minimal dependencies. We should evaluate if a custom solution is feasible first.

## 2. Android Text Selection Toolbar

### 2.1 ACTION_PROCESS_TEXT Intent

**Source**: [Android Developers - Custom Text Selection](https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999) (2015-12-30)

**Key Findings**:
- `ACTION_PROCESS_TEXT` is the standard way to add custom text processing actions
- Third-party apps register to handle this intent via manifest
- System automatically discovers and presents these apps in selection toolbar
- Intent includes `EXTRA_PROCESS_TEXT` with selected text

**Manifest Registration**:
```xml
<activity android:name=".ProcessTextActivity">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

**Relevance**: This is exactly what Feeder's `FeederTextActionModeCallback.addTextProcessors()` already uses. We need to preserve this functionality while adding configuration.

### 2.2 Custom Text Toolbar Implementation

**Source**: [PSPDFKit - Text Selection in Android PDF Viewer](https://pspdfkit.com/guides/android/features/text-selection) (2025-01-01)

**Key Findings**:
- Demonstrates custom `TextSelectionToolbar` implementation
- Uses `OnTextSelectionModeChangeListener` and `OnTextSelectionChangeListener`
- Provides 5 default actions: Highlight, Speak, Copy, Share, Search
- Shows how to build custom toolbar with Material Design

**Relevance**: While PSPDFKit's solution is more complex (PDF-specific), it demonstrates the pattern of building custom toolbars that we can learn from.

### 2.3 Flutter Plugin Example

**Source**: [global_context_menu Flutter Package](https://pub.dev/packages/global_context_menu) (2025-04-04)

**Key Findings**:
- Flutter plugin that adds custom actions to Android text selection toolbar
- Uses same `ACTION_PROCESS_TEXT` intent mechanism
- Shows how to return processed text to replace selection
- Requires Android 6.0 (API 23) or higher

**Relevance**: Confirms that `ACTION_PROCESS_TEXT` is the cross-platform standard for text selection customization.

## 3. Current Feeder Implementation Analysis

### 3.1 FeederTextToolbar Architecture

**File**: `FeederTextToolbar.kt` (Current Implementation)

**Current Behavior**:
- Hardcoded system actions: Copy, Paste, Cut, Select All (`MenuItemOption` enum)
- Discovers third-party processors via `PackageManager.queryIntentActivities()`
- Adds all discovered processors in alphabetical order
- No user configuration options
- Actions are added sequentially in `onCreateActionMode()`

**Key Code Sections**:
```kotlin
private fun addTextProcessors(menu: Menu) {
    textProcessors.clear()
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
    }
    // Queries all apps that handle PROCESS_TEXT
    // Adds them in sorted order
}
```

### 3.2 SettingsStore Pattern

**File**: `SettingsStore.kt`

**Pattern Observed**:
- Uses SharedPreferences for persistence
- StateFlow for reactive updates
- Pattern: `private val _setting = MutableStateFlow(...)` + `val setting = _setting.asStateFlow()`
- Mutator: `fun setSetting(value: T)` updates both Flow and SharedPreferences

**Example for New Feature**:
```kotlin
private val _selectionMenuConfig = MutableStateFlow(
    loadSelectionMenuConfig()
)
val selectionMenuConfig = _selectionMenuConfig.asStateFlow()

fun setSelectionMenuConfig(config: SelectionMenuConfig) {
    _selectionMenuConfig.value = config
    val json = json.encodeToString(config)
    sp.edit().putString(PREF_SELECTION_MENU_CONFIG, json).apply()
}
```

### 3.3 Settings Navigation Pattern

**File**: `NavigationDestinations.kt`

**Pattern**:
- Each settings screen has a dedicated `NavigationDestination` object
- Path pattern: `"settings/sub-feature"`
- Registered in nav graph
- Uses `diAwareViewModel()` for dependency injection

**For Our Feature**:
```kotlin
data object SelectionMenuSettingsDestination : NavigationDestination(
    path = "settings/selection_menu",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    // ... implementation
}
```

## 4. Best Practices & Recommendations

### 4.1 Drag-and-Drop Implementation

**Recommended Approach**: Custom modifier based on Nutrient pattern

**Rationale**:
- Aligns with project philosophy (no external dependencies)
- Feasible implementation (well-documented)
- Similar complexity to existing features

**Implementation Strategy**:
1. Create `Modifier.dragToReorder()` extension function
2. Use `detectDragGesturesAfterLongPress` for gesture detection
3. Calculate visual offset with `Offset.Offset` computation
4. Provide `onMove: (from, to) -> Unit` callback
5. Animate item position changes

### 4.2 Data Model Design

**Recommended Structure**:
```kotlin
@Serializable
data class SelectionMenuItem(
    val id: String,           // Unique identifier
    val type: ItemType,       // SYSTEM or THIRD_PARTY
    val packageName: String?,  // For third-party apps
    val className: String?,    // For third-party apps
    val enabled: Boolean,      // Toggle state
    val order: Int             // Position in menu
)

enum class ItemType {
    SYSTEM,
    THIRD_PARTY
}

@Serializable
data class SelectionMenuConfig(
    val items: List<SelectionMenuItem>
)
```

### 4.3 Persistence Strategy

**Recommended**: JSON serialization in SharedPreferences

**Rationale**:
- Consistent with existing AI provider list pattern
- Flexible for future additions
- Human-readable for debugging
- Easy migration path

### 4.4 Default Configuration

**Recommended Defaults**:
```kotlin
val defaultConfig = SelectionMenuConfig(
    items = listOf(
        SelectionMenuItem("copy", ItemType.SYSTEM, null, null, true, 0),
        SelectionMenuItem("paste", ItemType.SYSTEM, null, null, true, 1),
        SelectionMenuItem("cut", ItemType.SYSTEM, null, null, true, 2),
        SelectionMenuItem("select_all", ItemType.SYSTEM, null, null, true, 3),
        // Third-party apps added dynamically
    )
)
```

### 4.5 UI/UX Considerations

**Visual Design**:
- Group items by type (System vs Third-party)
- Show section headers
- Use drag handle icon (⠿) for reordering
- Maintain 48dp minimum touch target size
- Provide visual feedback during drag (elevation, shadow)

**Accessibility**:
- Support screen readers
- Keyboard navigation alternatives
- Clear "Reset to Defaults" option

## 5. Technical Risks & Mitigation

### 5.1 Performance

**Risk**: Drag-and-drop animations may lag on older devices

**Mitigation**:
- Use `animateItemPlacement()` modifier for smooth animations
- Test on minimum supported API level
- Optimize list rendering with `key()` parameter

### 5.2 Third-App Availability

**Risk**: Apps may be installed/removed while settings are open

**Mitigation**:
- Refresh third-party list when settings screen opens
- Handle missing apps gracefully in toolbar
- Show "unavailable" indicator for disabled-but-missing apps

### 5.3 State Synchronization

**Risk**: Settings changes not reflected immediately in toolbar

**Mitigation**:
- Use StateFlow for reactive updates
- Observe config flow in FeederTextToolbar
- Ensure toolbar invalidates when config changes

## 6. Implementation Timeline Estimate

Based on research findings:

| Task | Estimated Complexity | Time |
|------|---------------------|------|
| Data model & persistence | Low | 2-3 hours |
| SettingsStore integration | Low | 1-2 hours |
| ViewModel & UI scaffold | Medium | 3-4 hours |
| Drag-and-drop modifier | Medium-High | 4-6 hours |
| FeederTextToolbar integration | Medium | 3-4 hours |
| Navigation setup | Low | 1 hour |
| Testing & refinement | Medium | 3-4 hours |
| **Total** | | **17-24 hours** |

## 7. References

1. [Android Developers - Drag and Drop in Compose](https://developer.android.com/codelabs/codelab-dnd-compose)
2. [Nutrient - Drag-to-reorder with Compose](https://www.nutrient.io/blog/drag-to-reorder-with-compose/)
3. [Android Developers - ACTION_PROCESS_TEXT](https://medium.com/androiddevelopers/custom-text-selection-actions-with-action-process-text-191f792d2999)
4. [PSPDFKit - Text Selection](https://pspdfkit.com/guides/android/features/text-selection)
5. [Compose Reorderable Library](https://codergalib2005.medium.com/introducing-compose-reorderable-a-jetpack-compose-library-for-drag-drop-lists-and-grids-01eb60ac87f5)
6. Current Feeder codebase analysis (FeederTextToolbar.kt, SettingsStore.kt, NavigationDestinations.kt)

## 8. Conclusion

Research confirms that:
1. Custom drag-and-drop is feasible with documented patterns
2. `ACTION_PROCESS_TEXT` is the standard and mature approach
3. Feeder's existing architecture supports this feature well
4. No blocking technical concerns identified

**Recommendation**: Proceed with architecture design phase using custom drag-and-drop modifier approach and JSON-based persistence.
