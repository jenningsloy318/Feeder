# Requirements: Global Menu Item Discovery and Display

## Feature Overview
Implement functionality to discover and display all global menu items in the Selection Menu Configuration screen (created in spec-015). The system should detect three types of menus:
1. **System menus** - Built-in Android text processing menus (e.g., "copy", "paste", "cut", "select all")
2. **Our application features** - Feeder app's custom menus (e.g., "read aloud", "translate")
3. **Other application menu items** - Third-party apps that handle text processing (e.g., "Anki card", "AnkiQuick")

## User Story
As a user,
I want to see all available global menu items in the Selection Menu Configuration screen,
So that I can understand which menus are available and configure their display order.

## Context from Spec-015
In spec-015, we created:
- Navigation item under Settings → Text → Selection Menu
- SelectionMenuSettingsScreen with empty state placeholder
- SelectionMenuSettingsViewModel with TODO comments for implementation
- SelectionMenuItem data class

**Current State**: Clicking "Selection Menu" shows an empty screen with placeholder text.

**Desired State**: Screen should display discovered menu items with drag handlers for reordering.

## Functional Requirements

### FR1: Menu Item Discovery
**Location**: SelectionMenuSettingsViewModel
Discover all available global menu items across three categories:

#### FR1.1: System Menus
- Detect built-in Android text processing actions
- Items to discover:
  - Copy (android.R.string.copy)
  - Paste (android.R.string.paste)
  - Cut (android.R.string.cut)
  - Select All (android.R.string.selectAll)
- Mark these as system type with isSystem = true

#### FR1.2: Feeder Application Menus
- Detect Feeder's custom text processing features
- Known features to include:
  - Read Aloud (TTS functionality)
  - Translate (AI translation feature)
- Mark these as application type with isApplication = true
- Use internal component names for identification

#### FR1.3: Third-Party Application Menus
- Query Android PackageManager for apps handling ACTION_PROCESS_TEXT
- Discovery mechanism:
  - Create Intent with ACTION_PROCESS_TEXT
  - Query packageManager.queryIntentActivities()
  - Extract app name, package name, and component name
  - Filter out Feeder's own entries (to avoid duplicates)
- Mark these as third-party type with isThirdParty = true
- Sort by display name using ResolveInfo.DisplayNameComparator

### FR2: Menu Item Display
**Location**: SelectionMenuSettingsScreen
Display discovered menu items in a scrollable list with:

#### FR2.1: List Item Structure
Each menu item should show:
- Menu icon (if available)
- Menu name (primary text)
- Menu description or source (secondary text)
- Source indicator badge:
  - "System" for system menus
  - "Feeder" for application menus
  - Third-party app name for external apps
- Drag handler icon on the right side
- Enabled/disabled toggle

#### FR2.2: Visual Grouping
Display items in sections:
1. System Actions (always at top)
2. Feeder Features (second section)
3. Third-Party Apps (third section, sorted by name)

#### FR2.3: Empty State Handling
When no items are discovered (shouldn't happen), show enhanced empty state:
- "Unable to find any menu items"
- "Make sure other apps are installed and text processing is enabled"

### FR3: Drag and Drop Reordering
**Location**: SelectionMenuSettingsScreen
Implement drag-and-drop for menu items:

#### FR3.1: Drag Handler
- Display drag handle icon on right side of each item
- Use standard drag icon (e.g., Icons.Default.DragHandle)
- Only visible when item is draggable (all items should be draggable)

#### FR3.2: Drag Behavior
- User can long-press and drag item to reorder
- Visual feedback during drag:
  - Lifted elevation
  - Semi-transparent background
  - Other items animate to make space
- Drop target indicator shows where item will be placed

#### FR3.3: Section Constraints
- Items can be reordered within their section
- System items stay in System section
- Feeder items stay in Feeder section
- Third-party items stay in Third-Party section
- Cross-section reordering is NOT allowed

### FR4: Order Persistence
**Location**: SelectionMenuSettingsViewModel + DataStore
Save the user's preferred menu order:

#### FR4.1: Storage Mechanism
- Use DataStore for persistent storage
- Store order as JSON array of menu item IDs
- Key: "selection_menu_order"

#### FR4.2: Load Order on Startup
- When screen loads, read saved order from DataStore
- Apply saved order to discovered items
- If saved order contains items not currently discovered, remove them
- If discovered items are not in saved order, append them to appropriate section

#### FR4.3: Save Order on Change
- After drag-and-drop completes, save new order immediately
- Debounce saves to avoid excessive writes (max once per 2 seconds)
- Show save indicator briefly when order is persisted

### FR5: Menu Item State
**Location**: SelectionMenuItem data class
Extend SelectionMenuItem to include:

```kotlin
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
    // New fields:
    val type: MenuType,  // SYSTEM, APPLICATION, THIRD_PARTY
    val componentName: ComponentName? = null,  // For third-party apps
    val packageName: String? = null,  // For third-party apps
    val order: Int = 0,  // Display order
)

enum class MenuType {
    SYSTEM,
    APPLICATION,
    THIRD_PARTY
}
```

## Non-Functional Requirements

### NFR1: Performance
- Discovery must complete in < 500ms
- UI must remain responsive during discovery
- Use coroutines for background operations
- Cache discovered items to avoid repeated queries

### NFR2: User Interface
- Follow Material3 drag-and-drop guidelines
- Smooth animations for reordering (60fps)
- Accessible drag alternatives for screen readers
- Clear visual feedback during drag operations

### NFR3: Compatibility
- Android SDK 29+ (minSdk from project)
- Test on phone, tablet, and foldable form factors
- Support both portrait and landscape orientations
- Handle edge cases:
  - No third-party apps installed
  - Apps with missing labels
  - Apps with missing icons

### NFR4: Code Quality
- Follow project coding standards (Kotlin coding conventions)
- Unit test coverage: > 80%
- No compiler warnings
- Clean architecture with MVVM pattern
- Proper error handling for PackageManager queries

### NFR5: Localization
- System menu names use Android built-in strings (already localized)
- Feeder menu names use project string resources
- Third-party app names use PackageManager (already localized by apps)
- Section headers should be localizable

## Technical Requirements

### TR1: Android System Integration
Use existing FeederTextActionModeCallback as reference:
- PackageManager.queryIntentActivities() for third-party discovery
- ResolveInfo.DisplayNameComparator for sorting
- ComponentName for app identification

### TR2: Components to Create/Modify

#### Modify: SelectionMenuItem.kt
- Add MenuType enum
- Add type, componentName, packageName, order fields
- Update constructor parameters

#### Modify: SelectionMenuSettingsViewModel.kt
- Add menu discovery function:
  ```kotlin
  private fun discoverMenuItems(): List<SelectionMenuItem>
  ```
- Add DataStore integration for order persistence
- Implement LoadMenus event handler
- Add ReorderMenu event:
  ```kotlin
  data class ReorderMenu(
      val fromPosition: Int,
      val toPosition: Int
  ) : SelectionMenuEvent()
  ```

#### Modify: SelectionMenuSettingsScreen.kt
- Replace empty state with LazyColumn list
- Implement drag-and-drop using ReorderableItem or similar
- Add section headers (System, Feeder, Third-Party)
- Add menu item composable with drag handle
- Connect drag events to ViewModel

### TR3: DataStore Integration
- Create DataStore preference file: "selection_menu_preferences_pb"
- Define protobuf schema or use JSON serialization
- Inject DataStore into ViewModel via DI
- Use coroutine flows for reactive updates

### TR4: String Resources
Add to `res/values/strings.xml`:
```xml
<!-- Section Headers -->
<string name="selection_menu_section_system">System Actions</string>
<string name="selection_menu_section_feeder">Feeder Features</string>
<string name="selection_menu_section_third_party">Third-Party Apps</string>

<!-- Menu Item Names -->
<string name="selection_menu_read_aloud">Read Aloud</string>
<string name="selection_menu_translate">Translate</string>

<!-- Empty State -->
<string name="selection_menu_no_items">Unable to find any menu items</string>
<string name="selection_menu_no_items_hint">Make sure other apps are installed and text processing is enabled.</string>

<!-- Save Indicator -->
<string name="selection_menu_order_saved">Order saved</string>
```

### TR5: Dependency Injection
- Register DataStore in DI module
- Update SelectionMenuSettingsViewModel injection to include DataStore
- Follow existing Kodein DI patterns

## Acceptance Criteria

### AC1: Discovery Works
- [ ] System menus are discovered (copy, paste, cut, select all)
- [ ] Feeder menus are discovered (read aloud, translate)
- [ ] Third-party apps are discovered (Anki, AnkiQuick, etc.)
- [ ] Discovery completes in < 500ms
- [ ] No crashes when no third-party apps are installed

### AC2: Display Works
- [ ] Menu items display in correct sections
- [ ] Section headers are visible
- [ ] Icons display correctly
- [ ] Text is readable and properly aligned
- [ ] Drag handles are visible on all items
- [ ] Layout works on phone, tablet, and foldable

### AC3: Drag and Drop Works
- [ ] Items can be reordered within sections
- [ ] Visual feedback during drag is smooth
- [ ] Items cannot be moved across sections
- [ ] Drop target indicator is clear
- [ ] Order updates immediately after drop

### AC4: Persistence Works
- [ ] Order is saved to DataStore
- [ ] Order is restored when screen is reopened
- [ ] Order persists across app restarts
- [ ] Invalid items in saved order are removed
- [ ] New items are appended to appropriate sections

### AC5: Code Quality
- [ ] All code compiles without errors
- [ ] All code compiles without warnings
- [ ] Unit tests written and passing (coverage > 80%)
- [ ] Follows project naming conventions
- [ ] Proper accessibility semantics

### AC6: Integration
- [ ] No conflicts with existing FeederTextActionModeCallback
- [ ] Navigation works correctly
- [ ] ViewModel properly injected
- [ ] DataStore properly registered
- [ ] String resources defined

## Out of Scope (Future Work)
- Menu item enable/disable toggle (for now, all enabled by default)
- Menu item preview functionality
- Menu item configuration beyond ordering
- Import/export menu configurations
- Menu item search/filter

## Dependencies

### Existing Code
- FeederTextActionModeCallback - Reference for menu discovery
- SelectionMenuSettingsScreen - Display menu items
- SelectionMenuSettingsViewModel - Manage menu state
- SelectionMenuItem - Data model
- DI configuration - Register new dependencies

### External Libraries
- Jetpack Compose (already in project)
- DataStore (already in project)
- Kodein DI (already in project)
- Material3 (already in project)
- AndroidX (already in project)

### Third-Party Apps (for testing)
- Anki
- AnkiQuick
- Any other apps with ACTION_PROCESS_TEXT handlers

## Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| PackageManager query slow | Medium | Cache results, use coroutines, show loading indicator |
| Third-party app missing label | Low | Use package name as fallback, handle gracefully |
| Drag-and-drop performance issues | Medium | Use optimized library, test on low-end devices |
| DataStore corruption | Low | Validate data on load, use error handling |
| Section reordering confusion | Low | Clear visual separation, disable cross-section drag |
| Accessibility issues | Medium | Provide alternative reordering UI, proper labels |

## Success Metrics
- Discovery time < 500ms
- All three menu types discovered correctly
- Drag-and-drop 60fps smooth
- Order persistence 100% reliable
- Unit test coverage > 80%
- Zero compiler warnings
- User can reorder items and see changes persist

## Questions and Clarifications

### Q1: Should system menu items be reorderable?
**A1**: Yes, but only within the System section. System items cannot move to other sections.

### Q2: What happens if a third-party app is uninstalled?
**A2**: The item is removed from the list on next discovery. The saved order is updated to remove the missing item.

### Q3: Should we show disabled menu items?
**A3**: For this phase, all discovered items are enabled by default. Toggle functionality is future work.

### Q4: How should we handle duplicate menu names?
**A4**: Append app name in parentheses for third-party items. E.g., "Add to Card (Anki)"

### Q5: Should the order be global or per-section?
**A5**: Per-section ordering. Each section maintains its own order independently.

## Sign-off

**Product Owner**: Requirements approved
**Date**: 2026-01-04
**Priority**: High (Completes spec-015 feature)
