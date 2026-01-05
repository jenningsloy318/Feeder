# Requirements: Wire Global Menu Configuration to Article Page

**Document Version**: 1.0
**Date**: 2026-01-05
**Author**: Coordinator Agent
**Status**: Requirements Clarification
**Spec Index**: 017

---

## 1. Executive Summary

### 1.1 Purpose
Wire the global menu configuration system (implemented in specs 015 and 016) to the article reading page, enabling users to see their customized text selection menu when selecting text in articles.

### 1.2 Context
In **spec-015**, we created:
- Navigation to Selection Menu Settings screen
- Placeholder UI for menu configuration

In **spec-016**, we implemented:
- Menu item discovery (system, Feeder, third-party)
- Drag-and-drop reordering with Moon+ Reader pattern
- Toggle items on/off
- Order and visibility persistence to SharedPreferences

**Current State**: Users can configure menus, BUT the article page still uses the default Android ActionMode text selection menu.

**Desired State**: Article page should display the user's customized menu based on their configuration.

### 1.3 Critical Technical Constraint
Per **contextual-toolbar-report.md** research:
- **Android 13+ bypasses ActionMode.Callback** with system's Smart Actions floating toolbar
- The standard `view.startActionMode()` approach is **intercepted by the system**
- **Recommended Solution**: Implement Compose-based Popup that bypasses Android View ActionMode entirely

### 1.4 Scope
- **In Scope**:
  - Replace FeederTextToolbar with Compose Popup implementation
  - Wire global menu config to article page text selection
  - Support Android 7-15 with consistent behavior
  - Handle menu item actions (read aloud, translate, third-party apps)

- **Out of Scope**:
  - Menu configuration UI (already done in spec-016)
  - ActionMode fallback for Android < 13 (not needed with Compose Popup)
  - Smart Actions integration (intentionally bypassed)

---

## 2. User Stories

### Primary Story
**As a reader**, I want to see my customized text selection menu when selecting text in an article, so that I can quickly access my preferred actions (read aloud, translate, Anki cards) in the order I configured.

**Acceptance**:
- When I select text in an article, I see my custom menu (not system default)
- Menu items appear in the order I configured
- Disabled menu items don't appear
- Tapping a menu item executes the action

### Secondary Stories

#### Story 2: Read Aloud
**As a reader**, I want to tap "Read Aloud" in my custom menu to hear the selected text spoken aloud.

**Acceptance**:
- "Read Aloud" appears in menu if enabled in settings
- Tapping it triggers TTS for selected text
- Works with custom menu ordering

#### Story 3: Translate
**As a reader**, I want to tap "Translate" in my custom menu to translate the selected text.

**Acceptance**:
- "Translate" appears in menu if enabled in settings
- Tapping it opens translation dialog/screen
- Works with custom menu ordering

#### Story 4: Third-Party Actions
**As a reader**, I want to tap third-party app actions (e.g., "Add to Anki") in my custom menu.

**Acceptance**:
- Third-party apps appear in menu if enabled in settings
- Tapping them launches the external app with selected text
- Works with custom menu ordering

---

## 3. Functional Requirements

### FR1: Custom TextToolbar Implementation
**Location**: Article reading page, text selection area

**Requirement**: Replace the current `FeederTextToolbar` with a Compose Popup-based implementation that:
1. **Does NOT call `view.startActionMode()`** (critical for Android 13+)
2. Observes text selection state via `LocalTextToolbar`
3. Updates `MutableState<Rect?>` with selection bounds when text is selected
4. Triggers a Compose Popup to render menu at selection coordinates

**Rationale**: Per contextual-toolbar-report.md, Android 13+ intercepts `startActionMode()` and shows system menu. Compose Popup bypasses this entirely.

### FR2: Menu Configuration Integration
**Location**: TextToolbar implementation

**Requirement**: Read user's menu configuration from SharedPreferences:
1. **Load enabled items**: Only show items where `enabled = true`
2. **Apply custom order**: Display items in user's preferred order
3. **Handle missing items**: If saved item no longer exists, skip it
4. **Handle new items**: If new item discovered (not in saved order), append to end

**Configuration Keys** (from spec-016):
- `selection_menu_order`: JSON array of item IDs
- `selection_menu_visibility`: Map of item ID to boolean

### FR3: Popup Menu Rendering
**Location**: Compose Popup at selection coordinates

**Requirement**: Render Material 3 floating menu with:
1. **Position**: Above or below selection rect based on available space
2. **Items**: One menu item per enabled action
3. **Structure**:
   - Icon (if available)
   - Menu name
   - Optional description
4. **Behavior**:
   - Dismiss on tap outside
   - Dismiss on item tap
   - Dismiss on text selection change
   - Animate in/out (Material 3 motion)

### FR4: Menu Item Actions
**Location**: Menu item click handlers

**Requirement**: Execute actions when menu items are tapped:

#### FR4.1: System Actions
- **Copy**: Copy selected text to clipboard
- **Paste**: Not applicable in read-only article view
- **Cut**: Not applicable in read-only article view
- **Select All**: Expand selection to full article text

#### FR4.2: Feeder Actions
- **Read Aloud**: Trigger TTS for selected text (reuse existing implementation)
- **Translate**: Open translation dialog/screen (reuse existing implementation)

#### FR4.3: Third-Party Actions
- **Launch External App**: Use `ACTION_PROCESS_TEXT` intent
- **Pass Selected Text**: Intent.putExtra(Intent.EXTRA_TEXT, selectedText)
- **Handle Component**: Set specific ComponentName for targeted app

### FR5: Selection Bounds Detection
**Location**: TextToolbar.showMenu() callback

**Requirement**: Accurately capture selection coordinates:
1. **Receive rect**: Compose provides `Rect` with selection bounds
2. **Convert coordinates**: Translate Compose coordinates to screen coordinates
3. **Position popup**: Center popup horizontally above/below selection
4. **Handle edge cases**:
   - Selection near top of screen → show below
   - Selection near bottom of screen → show above
   - Selection too wide → truncate or scroll popup

### FR6: Menu Dismissal
**Location**: Popup state management

**Requirement**: Dismiss menu when:
1. **User taps outside**: Click outside popup bounds
2. **User taps item**: Action executed
3. **Selection changes**: User selects different text
4. **Selection cleared**: User taps outside text area
5. **Navigation occurs**: User leaves article page

---

## 4. Non-Functional Requirements

### NFR1: Performance
- Menu appears within **100ms** of text selection
- Popup renders at **60fps** (smooth animations)
- No lag during menu interactions
- Configuration read should be cached (don't read from disk on every selection)

### NFR2: Compatibility
- **Android 7 (API 24) through Android 15+**: Single implementation for all versions
- **Phone, tablet, foldable**: Responsive popup positioning
- **Portrait and landscape**: Proper orientation handling
- **Dark/light theme**: Menu follows system theme

### NFR3: Accessibility
- **TalkBack support**: Menu items announced properly
- **Keyboard navigation**: Arrow keys to navigate, Enter to select
- **Touch target size**: Minimum 48dp (Material 3 guidelines)
- **High contrast mode**: Proper contrast ratios

### NFR4: User Experience
- **Visual consistency**: Menu matches Material 3 design language
- **Smooth animations**: Standard Material motion easing
- **Predictable behavior**: Menu appears where user expects
- **No flicker**: Smooth transition between hidden/visible states

### NFR5: Code Quality
- **Follow project patterns**: MVVM architecture, Compose UI
- **Test coverage**: ≥80% for business logic
- **Zero compiler warnings**
- **Clean architecture**: Separation of concerns (UI, logic, data)

---

## 5. Technical Requirements

### TR1: Component Architecture

```
ArticleReadingScreen
  └─> SelectionContainer
        └─> CompositionLocalProvider(LocalTextToolbar provides CustomTextToolbar)
              └─> CustomTextToolbar
                    ├─> MenuConfigStore (read configuration)
                    ├─> SelectionRectState (MutableState<Rect?>)
                    └─> MenuPopup (observes SelectionRectState)

MenuPopup
  ├─> Popup (position at selection bounds)
  └─> MenuItem (list of enabled items in custom order)
        ├─> System Actions (copy, select all)
        ├─> Feeder Actions (read aloud, translate)
        └─> Third-Party Actions (Anki, etc.)
```

### TR2: Data Models

#### MenuItem (reuse from spec-016)
```kotlin
data class MenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val type: MenuType,
    val enabled: Boolean,
    val order: Int,
    val componentName: ComponentName? = null  // For third-party
)
```

#### MenuConfiguration (new)
```kotlin
data class MenuConfiguration(
    val items: List<MenuItem>,
    val order: List<String>,  // Item IDs in custom order
    val visibility: Map<String, Boolean>  // Item ID to enabled state
)
```

### TR3: Components to Create

#### 3.1 CustomTextToolbar.kt
```kotlin
class CustomTextToolbar(
    private val context: Context,
    private val menuConfigStore: MenuConfigStore
) : TextToolbar {

    private val selectionRectState = mutableStateOf<Rect?>(null)

    override fun showMenu(
        rect: Rect,
        text: String
    ) {
        // Update state to trigger popup
        selectionRectState.value = rect
    }

    override fun hideMenu() {
        // Clear state to dismiss popup
        selectionRectState.value = null
    }

    // Provide state to observe
    fun getSelectionRectState(): MutableState<Rect?> = selectionRectState
}
```

#### 3.2 MenuConfigStore.kt (new)
```kotlin
class MenuConfigStore(
    private val sharedPreferences: SharedPreferences
) {
    fun loadConfiguration(): MenuConfiguration {
        val orderJson = sharedPreferences.getString("selection_menu_order", "[]")
        val visibilityJson = sharedPreferences.getString("selection_menu_visibility", "{}")

        // Parse JSON and return MenuConfiguration
    }
}
```

#### 3.3 TextSelectionMenuPopup.kt (new)
```kotlin
@Composable
fun TextSelectionMenuPopup(
    selectionRect: Rect?,
    menuConfiguration: MenuConfiguration,
    onActionExecuted: () -> Unit,
    onDismiss: () -> Unit
) {
    if (selectionRect != null) {
        Popup(
            offset = IntOffset(selectionRect.left, selectionRect.top - 100),
            onDismissRequest = onDismiss
        ) {
            LazyColumn {
                items(menuConfiguration.items.filter { it.enabled }) { item ->
                    MenuItemRow(
                        item = item,
                        onClick = {
                            executeAction(item)
                            onActionExecuted()
                        }
                    )
                }
            }
        }
    }
}
```

#### 3.4 MenuItemRow.kt (new)
```kotlin
@Composable
fun MenuItemRow(
    item: MenuItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { icon ->
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(text = item.name)
            item.description?.let { desc ->
                Text(text = desc, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

### TR4: Integration Points

#### 4.1 Article Reading Screen
```kotlin
@Composable
fun ArticleReadingScreen(
    article: Article,
    customTextToolbar: CustomTextToolbar
) {
    CompositionLocalProvider(LocalTextToolbar provides customTextToolbar) {
        SelectionContainer {
            ArticleContent(article = article)
        }

        // Observe selection state and show popup
        val selectionRect by customTextToolbar.getSelectionRectState()
        val menuConfiguration = customTextToolbar.getMenuConfiguration()

        TextSelectionMenuPopup(
            selectionRect = selectionRect,
            menuConfiguration = menuConfiguration,
            onActionExecuted = { customTextToolbar.hideMenu() },
            onDismiss = { customTextToolbar.hideMenu() }
        )
    }
}
```

#### 4.2 Dependency Injection
```kotlin
// In DI module
bind<MenuConfigStore>() with singleton {
    MenuConfigStore(instance())
}

bind<CustomTextToolbar>() with provider {
    CustomTextToolbar(instance(), instance())
}
```

### TR5: String Resources
```xml
<!-- System Actions -->
<string name="menu_action_copy">Copy</string>
<string name="menu_action_select_all">Select All</string>

<!-- Feeder Actions -->
<string name="menu_action_read_aloud">Read Aloud</string>
<string name="menu_action_translate">Translate</string>

<!-- Third-Party Actions (dynamic, from PackageManager) -->
```

---

## 6. Acceptance Criteria

### AC1: Configuration Integration
- [ ] Article page reads menu configuration from SharedPreferences
- [ ] Only enabled menu items appear in selection menu
- [ ] Menu items appear in user's custom order
- [ ] Changes in settings screen immediately affect article page

### AC2: Popup Rendering
- [ ] Popup appears when text is selected in article
- [ ] Popup positioned correctly relative to selection
- [ ] Popup renders at 60fps (smooth animations)
- [ ] Popup dismisses on tap outside

### AC3: System Actions
- [ ] Copy action copies selected text to clipboard
- [ ] Select All action expands selection to full text
- [ ] System actions work consistently across Android versions

### AC4: Feeder Actions
- [ ] Read Aloud action triggers TTS for selected text
- [ ] Translate action opens translation screen
- [ ] Feeder actions work with custom ordering

### AC5: Third-Party Actions
- [ ] Third-party apps launch with selected text
- [ ] Correct ComponentName used for targeted launch
- [ ] Third-party actions work with custom ordering

### AC6: Dismissal Behavior
- [ ] Menu dismisses when tapping outside
- [ ] Menu dismisses when action executed
- [ ] Menu dismisses when selection changes
- [ ] Menu dismisses when navigating away

### AC7: Performance
- [ ] Menu appears within 100ms of selection
- [ ] No lag during menu interactions
- [ ] Configuration is cached (not read on every selection)

### AC8: Compatibility
- [ ] Works on Android 7 through Android 15+
- [ ] Works on phone, tablet, and foldable
- [ ] Works in portrait and landscape
- [ ] Follows system theme (dark/light)

### AC9: Code Quality
- [ ] All code compiles without errors
- [ ] All code compiles without warnings
- [ ] Unit test coverage ≥80%
- [ ] Follows project naming conventions
- [ ] Proper accessibility semantics

---

## 7. Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Popup positioning issues on edge cases | High | Test thoroughly, implement smart positioning logic |
| Third-party app launch failures | Medium | Wrap in try-catch, show user-friendly error |
| Performance issues on low-end devices | Medium | Cache configuration, optimize popup rendering |
| Accessibility gaps | Medium | Work with accessibility guidelines, test with TalkBack |
| Android version compatibility | High | Test on multiple API levels, use Compose for consistency |
| Configuration sync issues | Low | Use SharedPreferences listeners or observe as StateFlow |

---

## 8. Dependencies

### Internal Dependencies
- **spec-015**: SelectionMenuSettingsScreen (configuration UI)
- **spec-016**: Menu discovery and persistence (SharedPreferences)
- **Article reading page**: Integration point for CustomTextToolbar
- **TTS module**: Reuse for Read Aloud action
- **Translation module**: Reuse for Translate action

### External Dependencies
- Jetpack Compose (already in project)
- AndroidX (already in project)
- Material3 (already in project)
- Kodein DI (already in project)

---

## 9. Success Metrics

- **Functional**: All menu actions work correctly in article page
- **Performance**: Menu appears within 100ms of text selection
- **Quality**: ≥80% test coverage, zero compiler warnings
- **Compatibility**: Works on Android 7-15+, all screen sizes
- **User Experience**: Smooth animations, predictable behavior

---

## 10. Questions and Clarifications

### Q1: Should we support legacy ActionMode for Android < 13?
**A1**: No. The Compose Popup approach works on all Android versions (API 24+), providing consistent behavior. Per contextual-toolbar-report.md, ActionMode is unreliable on Android 13+ anyway.

### Q2: How do we handle third-party app launch failures?
**A2**: Wrap launch in try-catch, show toast message: "Unable to open [app name]"

### Q3: Should menu be configurable per-article or globally?
**A3**: Globally. The configuration from spec-016 is app-wide, not per-article.

### Q4: What happens if user disables all menu items?
**A4**: Show empty menu with message: "No menu items enabled. Configure in Settings → Text → Selection Menu"

### Q5: Should we support keyboard shortcuts for menu actions?
**A5**: Not in scope for this spec. Future enhancement.

---

## 11. Sign-off

**Product Owner**: Requirements clarified
**Date**: 2026-01-05
**Priority**: High (Completes menu customization feature)
**Dependencies**: spec-015, spec-016, contextual-toolbar-report.md

---

## Appendix A: References

- **spec-015**: Selection Menu Configuration (placeholder UI)
- **spec-016**: Global Menu Item Discovery (configuration implementation)
- **contextual-toolbar-report.md**: Research on Android 13+ ActionMode limitations
- **Moon+ Reader**: Reference implementation for custom text menus
