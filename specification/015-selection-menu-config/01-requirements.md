# Requirements Document: Selection Menu Configuration Feature

**Specification ID**: 015
**Feature Name**: Selection Menu Configuration
**Status**: Draft
**Last Updated**: 2026-01-04

## Executive Summary

Add a configurable selection menu system for the article reader that allows users to customize which items appear in the text selection toolbar and their order. The feature should follow the pattern established by Moon+ Reader, providing a settings interface under "Settings -> Text -> Selection Menu".

## User Stories

### Primary User Story
> As a reader, I want to customize which actions appear when I select text in an article, so that I can prioritize my most-used text processing actions and reduce clutter.

### Secondary User Stories
- As a reader, I want to enable/disable third-party text processors (like Anki, Perplexity) from appearing in the selection menu
- As a reader, I want to reorder system actions (Copy, Cut, Paste, Select All) to match my workflow
- As a reader, I want to permanently hide certain actions I never use

## Functional Requirements

### FR1: Selection Menu Settings Screen
**Priority**: Must Have
**Description**: A new settings screen accessible from Settings -> Text -> Selection Menu

**Acceptance Criteria**:
- Screen should be accessible via navigation from Settings screen
- Screen should follow the existing settings screen pattern (e.g., TextSettingsScreen)
- Screen title should be "Selection Menu" (localized)

### FR2: Display Available Menu Items
**Priority**: Must Have
**Description**: List all available menu items including system and third-party items

**Acceptance Criteria**:
- System items: Copy, Paste, Cut, Select All (when applicable)
- Third-party items: All apps that respond to `ACTION_PROCESS_TEXT` intent
- Each item should display its name/icon
- Items should be grouped by type (System vs Third-party) for clarity

### FR3: Enable/Disable Toggle
**Priority**: Must Have
**Description**: Each menu item should have a toggle switch to enable/disable it

**Acceptance Criteria**:
- Toggle switch should be consistent with Material Design 3
- When disabled, item should not appear in text selection toolbar
- At least one system item must remain enabled (validation)
- Settings should persist across app restarts

### FR4: Drag-and-Drop Reordering
**Priority**: Must Have
**Description**: Users can reorder items using drag-and-drop

**Acceptance Criteria**:
- Visual feedback during drag operation
- Reorder should persist immediately to settings
- Order should be respected in text selection toolbar
- Smooth animations during reorder operations

### FR5: Integration with Text Selection Toolbar
**Priority**: Must Have
**Description**: ReaderView's SelectionContainer should use the custom configuration

**Acceptance Criteria**:
- Selection toolbar should only show enabled items
- Items should appear in the configured order
- Toolbar should respect visibility rules (e.g., Paste only when clipboard has content)
- Default configuration should match current behavior

### FR6: Default Configuration
**Priority**: Should Have
**Description**: Provide sensible defaults for first-time users

**Acceptance Criteria**:
- All system items enabled by default
- Common third-party apps enabled by default (if detected)
- Default order: Copy, Paste, Cut, Select All, then third-party apps alphabetically

## Non-Functional Requirements

### NFR1: Performance
- Settings changes should apply immediately (no app restart required)
- Drag-and-drop should maintain 60fps animations
- Selection toolbar should appear within 100ms of text selection

### NFR2: Usability
- Settings screen should be accessible for users with motor disabilities
- Minimum touch target size: 48dp
- Support keyboard navigation and screen readers

### NFR3: Compatibility
- Support Android API levels consistent with app requirements
- Handle third-party apps that become unavailable/uninstalled
- Gracefully handle cases where no text processors are installed

### NFR4: Data Persistence
- Configuration should persist in SharedPreferences
- Survive app updates
- Survive device reboots

## Technical Context

### Current Implementation
The app currently uses `FeederTextActionModeCallback` in `FeederTextToolbar.kt`:
- System items are hardcoded in `MenuItemOption` enum
- Third-party processors are discovered via `ACTION_PROCESS_TEXT` intent
- Menu items are added in fixed order
- No customization options exist

### Required Changes
1. **SettingsStore**: Add selection menu configuration state
2. **ViewModel**: Create SelectionMenuSettingsViewModel
3. **UI**: Create SelectionMenuSettingsScreen with drag-and-drop
4. **FeederTextToolbar**: Modify to read configuration from settings
5. **Navigation**: Add SelectionMenuSettingsDestination

## Constraints & Dependencies

### Platform Constraints
- Must use Android's TextToolbar API
- Must respect ACTION_PROCESS_TEXT intent contract
- Material Design 3 guidelines

### Technical Dependencies
- Existing SettingsStore pattern
- Existing navigation infrastructure
- Kodein DI for ViewModel injection

### Dependencies on Other Features
- None (standalone feature)

## Out of Scope

The following items are explicitly out of scope for this feature:
- Custom text processors (only system + third-party apps)
- Text processing action configuration (e.g., custom Anki card format)
- Selection menu styling/theming
- Per-feed or per-article selection menu configurations
- Import/export of selection menu configurations

## Open Questions

1. Should we allow users to hide ALL third-party apps with a single toggle?
2. Should we show a count of how many third-party apps are available?
3. Should we provide a "Reset to Defaults" option?
4. How should we handle third-party apps that are installed/removed while settings are open?

## Success Metrics

- User can customize selection menu within 3 taps from Settings
- Settings changes apply immediately to selection toolbar
- No performance regression in selection toolbar appearance time
- Crash-free when third-party apps are installed/removed
