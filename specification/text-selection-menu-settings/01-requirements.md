# Requirements: Text Selection Menu Settings

**Date:** 2025-11-27 21:54:11 PST
**Task Type:** Feature Implementation
**Project:** Feeder (Android RSS Reader)

## Context

Current implementation in `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`:
- Text selection menu with hardcoded items: Copy, Select All, and dynamic text processors
- Menu item order is fixed in `MenuItemOption` enum
- No user configuration available

## User Requirements

### 1. Text Processor Management
**Decision:** Individual control
- Each text processor app can be shown/hidden individually
- Each text processor can be reordered individually
- Full granular control over which apps appear in the menu

### 2. Default Menu Order
**Decision:** User customizable
- No hardcoded default order
- Let user decide initial order during first setup or first access to settings
- Provide a sensible suggested order but allow immediate customization

### 3. Reset to Defaults Option
**Decision:** Yes, with confirmation dialog
- Include a "Reset to defaults" button in settings
- Show confirmation dialog before restoring defaults
- Prevents accidental reset of user's configuration

### 4. Disabled Items Behavior
**Decision:** Shown as disabled (grayed out)
- Disabled items appear in the text selection menu but grayed out
- Users can see what options are available but not currently enabled
- Makes it clear that items can be re-enabled

## Functional Requirements

1. **Settings Section**
   - Add a new settings section for text selection menu configuration
   - Accessible from the main settings screen

2. **Enable/Disable Items**
   - Toggle switches for Copy and Select All
   - Toggle switches for each detected text processor app
   - All items can be individually enabled/disabled

3. **Reorder Items**
   - Drag-and-drop interface for reordering menu items
   - Visual feedback during drag operation
   - Reorder affects both built-in items (Copy, Select All) and text processors

4. **Reset Functionality**
   - "Reset to defaults" button in settings
   - Confirmation dialog: "Reset text selection menu to default settings? This will restore the default order and enable all items."
   - After reset, restore suggested default order and enable all items

5. **Menu Rendering**
   - Disabled items shown with reduced opacity or grayed-out appearance
   - Disabled items should not be clickable
   - Enabled items function normally

## Non-Functional Requirements

1. **Persistence**
   - Settings must persist across app restarts
   - Use existing app preferences/datastore mechanism

2. **Performance**
   - Settings changes should apply immediately without app restart
   - Minimal overhead when rendering text selection menu

3. **Compatibility**
   - Work with existing text processor discovery mechanism
   - Handle dynamic addition/removal of text processor apps

4. **UX Consistency**
   - Follow existing Feeder app settings patterns
   - Use Material Design 3 components consistent with the app
   - Match existing drag-and-drop patterns if present in the app

## Technical Constraints

- Kotlin & Jetpack Compose
- Android platform (handle ACTION_PROCESS_TEXT intents)
- Must integrate with existing FeederTextToolbar implementation
- Preserve backward compatibility for users upgrading

## Success Criteria

1. Users can enable/disable Copy and Select All menu items
2. Users can enable/disable individual text processor apps
3. Users can reorder all menu items via drag-and-drop
4. Disabled items appear grayed out in the text selection menu
5. Settings persist and apply immediately
6. Reset to defaults works with confirmation
7. UI follows app's existing design patterns

## Questions for Research Phase

1. What settings framework does Feeder use? (Preferences, DataStore, etc.)
2. How are existing settings organized in the app?
3. Is there existing drag-and-drop UI in the app to follow as a pattern?
4. What is the suggested default order for items?
5. How are text processor apps currently detected and listed?
