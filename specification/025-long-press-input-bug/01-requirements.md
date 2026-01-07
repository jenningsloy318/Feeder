# Requirements - Spec 25: Long-Press Input Bug Fix

## Bug Description

In the Feeder app, long-pressing in input text fields (TextField, OutlinedTextField) throughout the application does not show the standard Android text selection toolbar (copy, paste, select all options). However, on the article reading page, text selection works correctly and displays the toolbar as expected.

## Current Behavior

### Working: Article Page (ReaderView.kt)
- **File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ReaderView.kt`
- **Line 110**: `SelectionContainer {` wraps the article content
- Text selection toolbar appears correctly when long-pressing on article text
- User can copy, paste, and select all text

### Not Working: Input Fields Across App
**Affected screens identified**:
1. **ProviderEditScreen.kt** - AI provider configuration screen
   - `OutlinedTextField` components for provider settings
2. **EditFeedScreen.kt** - Feed creation/editing screen
   - `TextField` and `OutlinedTextField` components for feed URL, title, etc.
3. **SearchFeedScreen.kt** - Feed search functionality
4. **SyncScreen.kt** - Sync configuration
5. **EditableListDialog.kt** - Dialog with editable lists
6. **FeedNotificationsDialog.kt** - Notification settings dialog
7. **AutoCompleteText.kt** - Auto-complete component

## Root Cause Analysis

**Hypothesis**: Text fields in Compose do not automatically have `SelectionContainer` wrapping them. Unlike traditional Android views, Compose's `TextField` components require explicit `SelectionContainer` wrapping to enable text selection and toolbar functionality.

**Evidence**:
- Article page uses `SelectionContainer` explicitly (line 110 in ReaderView.kt)
- Input field screens do NOT use `SelectionContainer` wrapper
- Standard Compose behavior: `SelectionContainer` must be added manually to `TextField` and `OutlinedTextField`

## Requirements

### Functional Requirements

1. **Enable Text Selection in All Input Fields**
   - All `TextField` components must support text selection
   - All `OutlinedTextField` components must support text selection
   - Selection toolbar must appear on long-press
   - Standard actions: Copy, Paste, Select All, Cut must be available

2. **Maintain Current Functionality**
   - No changes to input field behavior
   - No changes to focus handling
   - No changes to keyboard interaction
   - No changes to validation logic

3. **Consistent User Experience**
   - Text selection should work the same way across all screens
   - Behavior should match Android standards

### Non-Functional Requirements

1. **Performance**
   - No measurable performance impact from adding selection containers
   - Memory usage should not increase significantly

2. **Code Quality**
   - Follow project coding standards (00-dev-rules.md)
   - Maintain code readability and simplicity
   - Avoid over-engineering

3. **Compatibility**
   - Must work with minSdk 29 (Android 10+)
   - Must work across different screen sizes (phone, tablet, foldable)

4. **Testing**
   - All existing tests must pass
   - New functionality should be testable (if possible with UI tests)

## Affected Components

**Priority 1 - High Frequency User Screens**:
1. `EditFeedScreen.kt` - Create/edit feeds (core functionality)
2. `ProviderEditScreen.kt` - AI provider configuration (feature from recent specs)
3. `SearchFeedScreen.kt` - Feed discovery

**Priority 2 - Settings and Configuration**:
4. `SyncScreen.kt` - Sync settings
5. `EditableListDialog.kt` - Used in various settings
6. `FeedNotificationsDialog.kt` - Notification settings

**Priority 3 - Components**:
7. `AutoCompleteText.kt` - Reusable autocomplete component (if used for input)

## Implementation Approach

**Recommended**: Apply `SelectionContainer` wrapper to all text fields systematically.

**Rationale**:
- Simple, consistent approach
- Matches existing pattern in ReaderView.kt
- Minimal code changes
- No custom logic required

**Alternative Considered**:
- Create a custom TextField component with built-in selection
- **Rejected**: Over-engineering, not aligned with "simple solutions" principle

## Success Criteria

1. ✓ Long-press in any TextField shows selection toolbar
2. ✓ Copy, paste, select all, cut actions work correctly
3. ✓ All existing tests pass
4. ✓ No regressions in input field functionality
5. ✓ Code follows project standards

## Out of Scope

- Changing the visual appearance of text fields
- Adding new features beyond text selection
- Modifying focus handling or keyboard behavior
- Performance optimization beyond selection functionality

## Dependencies

- None (pure Compose UI change)

## Risks

**Low Risk**:
- Simple wrapper addition
- Well-established Compose pattern
- Existing working example in codebase (ReaderView.kt)

**Potential Issues**:
- Edge cases with custom text field behaviors
- Interaction with other modifiers

**Mitigation**:
- Thorough testing on each affected screen
- Gradual rollout (test one screen at a time if needed)
