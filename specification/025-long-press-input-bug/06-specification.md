# Technical Specification - Spec 25: Long-Press Input Bug Fix

## Specification Information
- **Spec ID**: 025
- **Title**: Long-Press Input Bug - Text Selection Toolbar Not Showing in Input Fields
- **Type**: Bug Fix
- **Priority**: Medium
- **Complexity**: Low
- **Created**: January 7, 2026

## Problem Statement

Users cannot select text in input fields (TextField, OutlinedTextField) throughout the Feeder app. Long-pressing does not show the Android text selection toolbar with copy, paste, select all options. This is a usability issue as users expect standard Android text selection behavior.

However, text selection works correctly on the article reading page, indicating the capability exists in the codebase but is not applied to input fields.

## Solution Overview

Add `SelectionContainer` wrapper to all TextField and OutlinedTextField components throughout the application. This is a standard Jetpack Compose pattern that enables text selection and the system text toolbar.

## Technical Details

### Root Cause
Missing `SelectionContainer` wrapper in TextField and OutlinedTextField composables.

### Solution
Wrap TextField and OutlinedTextField components with `SelectionContainer`.

### Implementation Pattern

**Before**:
```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Label") },
    modifier = Modifier.fillMaxWidth()
)
```

**After**:
```kotlin
SelectionContainer {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") },
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Required Import
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer
```

## Scope

### In Scope
- All TextField components in screen composables
- All OutlinedTextField components in screen composables
- Custom components that contain TextField (e.g., AutoCompleteText)

### Out of Scope
- Password fields (should NOT be selectable for security)
- Read-only text displays (already working in ReaderView)
- Business logic changes
- Database modifications
- API changes

## Affected Components

### Priority 1: High-Frequency User Screens

1. **EditFeedScreen.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/editfeed/EditFeedScreen.kt`)
   - Create/edit RSS feeds
   - Estimated TextField count: 5-7
   - User impact: HIGH

2. **ProviderEditScreen.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`)
   - AI provider configuration
   - Estimated TextField count: 5
   - User impact: HIGH (recent feature from Spec-21, Spec-24)

3. **SearchFeedScreen.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/searchfeed/SearchFeedScreen.kt`)
   - Feed discovery
   - Estimated TextField count: 1-2
   - User impact: HIGH

### Priority 2: Settings and Configuration

4. **SyncScreen.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/sync/SyncScreen.kt`)
   - Sync settings configuration
   - User impact: MEDIUM

5. **EditableListDialog.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/dialog/EditableListDialog.kt`)
   - Edit lists (tags, etc.)
   - User impact: MEDIUM

6. **FeedNotificationsDialog.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/dialog/FeedNotificationsDialog.kt`)
   - Notification settings
   - User impact: MEDIUM

### Priority 3: Reusable Components

7. **AutoCompleteText.kt** (`app/src/main/java/com/nononsenseapps/feeder/ui/compose/components/AutoCompleteText.kt`)
   - Reusable autocomplete component
   - User impact: HIGH (if fixed here, benefits all usages)

## Implementation Requirements

### Functional Requirements

1. **Text Selection Must Work**
   - Long-press on any TextField shows selection handles
   - System toolbar appears with copy, paste, cut, select all options
   - User can select text ranges
   - User can copy selected text
   - User can paste from clipboard
   - User can cut text (if editable)
   - User can select all text

2. **No Behavior Changes**
   - TextField focus behavior unchanged
   - Keyboard interaction unchanged
   - Input validation unchanged
   - Text state management unchanged
   - IME action handling unchanged

3. **Visual Consistency**
   - No visual artifacts from SelectionContainer
   - Material3 design maintained
   - TextField styling unchanged

### Non-Functional Requirements

1. **Performance**
   - No measurable performance degradation
   - No increased memory usage
   - Smooth animations maintained

2. **Compatibility**
   - Works on minSdk 29 (Android 10+)
   - Works on all screen sizes
   - Works with different input methods

3. **Code Quality**
   - Follows project coding standards (00-dev-rules.md)
   - Maintains code readability
   - No unnecessary complexity
   - Consistent with existing patterns

4. **Testing**
   - All existing tests must pass
   - Manual testing on each affected screen
   - Verify text selection works

## Technical Constraints

### Must
- Use SelectionContainer from Compose Foundation
- Follow existing pattern from ReaderView.kt
- Maintain all TextField properties and modifiers
- Add import statement to each file

### Must Not
- Change TextField internal logic
- Modify business logic
- Break existing functionality
- Add new dependencies

## Edge Cases and Special Considerations

### Password Fields
**Rule**: DO NOT wrap password TextField in SelectionContainer
**Reason**: Security - users should not be able to copy passwords
**Detection**: Check for `KeyboardType.Password`

### Read-Only Fields
**Rule**: Wrap read-only TextField in SelectionContainer
**Reason**: Users may want to copy read-only text
**Benefit**: Improved usability

### Disabled Fields
**Rule**: Can wrap disabled TextField in SelectionContainer
**Reason**: Users might want to copy disabled text
**Note**: Selection works on disabled fields

### Multi-Line Fields
**Rule**: Wrap same as single-line fields
**Reason**: Selection works for both
**Note**: No special handling needed

## Acceptance Criteria

### Criteria 1: Text Selection Works
- [ ] Long-press on TextField shows selection handles
- [ ] Toolbar appears with copy, paste, select all, cut
- [ ] Can select text range
- [ ] Can copy selected text
- [ ] Can paste from clipboard
- [ ] Can select all text

### Criteria 2: No Regressions
- [ ] All existing tests pass
- [ ] Focus behavior works as before
- [ ] Keyboard interaction works as before
- [ ] Input validation works as before
- [ ] No visual artifacts

### Criteria 3: All Screens Fixed
- [ ] EditFeedScreen - text selection works
- [ ] ProviderEditScreen - text selection works
- [ ] SearchFeedScreen - text selection works
- [ ] SyncScreen - text selection works
- [ ] EditableListDialog - text selection works
- [ ] FeedNotificationsDialog - text selection works
- [ ] AutoCompleteText component - text selection works

### Criteria 4: Code Quality
- [ ] Code follows project standards
- [ ] No ktlint violations
- [ ] Code is readable and simple
- [ ] Consistent with existing patterns

## Dependencies

### External Dependencies
- None (using built-in Compose components)

### Internal Dependencies
- None (pure UI change, isolated to composables)

## Risks and Mitigations

### Risk 1: Regression in TextField Behavior
**Probability**: Low
**Impact**: Medium
**Mitigation**: Thorough manual testing of each screen

### Risk 2: Visual Artifacts
**Probability**: Very Low
**Impact**: Low
**Mitigation**: SelectionContainer is transparent wrapper

### Risk 3: Performance Impact
**Probability**: Very Low
**Impact**: Low
**Mitigation**: SelectionContainer is lightweight

### Risk 4: Password Field Security
**Probability**: N/A (we'll exclude password fields)
**Impact**: High
**Mitigation**: Identify and exclude password fields

## Testing Strategy

### Manual Testing Required

**For each affected screen**:
1. Open the screen
2. Long-press on each TextField
3. Verify selection handles appear
4. Verify toolbar appears
5. Test copy: Select text → Copy → Paste elsewhere
6. Test paste: Copy text → Paste in field
7. Test select all: Long-press → Select all
8. Test cut: Select text → Cut
9. Verify keyboard still works
10. Verify focus behavior unchanged

### Automated Testing

**Existing Tests**: Must all pass
**New Tests**: Optional (UI tests for selection)

### Test Devices/Emulators
- Phone (API 29+)
- Tablet (if available)
- Different screen sizes

## Rollout Plan

### Phase 1: Core Screens (Priority 1)
1. EditFeedScreen.kt
2. ProviderEditScreen.kt
3. SearchFeedScreen.kt

### Phase 2: Settings Screens (Priority 2)
4. SyncScreen.kt
5. EditableListDialog.kt
6. FeedNotificationsDialog.kt

### Phase 3: Components (Priority 3)
7. AutoCompleteText.kt

### Testing Between Phases
Test each screen after implementation before proceeding to next phase.

## Success Metrics

### Primary Metrics
- ✅ Text selection toolbar appears on long-press in all input fields
- ✅ Copy, paste, cut, select all actions work correctly
- ✅ No regressions in existing functionality

### Secondary Metrics
- ✅ Zero test failures
- ✅ Zero ktlint violations
- ✅ No performance degradation

## Documentation Updates

### Code Comments
No additional comments needed - self-explanatory wrapper

### User Documentation
Not needed - standard Android behavior, users expect it to work

### Developer Documentation
This spec serves as documentation

## References

- **Requirements**: 01-requirements.md
- **Research**: 02-research-report.md
- **Debug Analysis**: 03-debug-analysis.md
- **Code Assessment**: 04-assessment.md
- **Working Example**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ReaderView.kt:110`

## Appendix A: Code Examples

### Example 1: Simple TextField
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer

SelectionContainer {
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
```

### Example 2: OutlinedTextField with Label
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer

SelectionContainer {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Email Address") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email
        )
    )
}
```

### Example 3: TextField with Focus Management
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer

val focusRequester = remember { FocusRequester() }

SelectionContainer {
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        )
    )
}
```

## Appendix B: Checklist for Each File

- [ ] Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
- [ ] Identify all TextField instances
- [ ] Identify all OutlinedTextField instances
- [ ] Check for password fields (exclude these)
- [ ] Wrap each TextField in SelectionContainer
- [ ] Wrap each OutlinedTextField in SelectionContainer
- [ ] Verify modifiers remain on TextField (not moved to SelectionContainer)
- [ ] Build the project (ensure no compilation errors)
- [ ] Test the screen manually
- [ ] Verify text selection works
- [ ] Verify no regressions

## Approval

**Status**: Ready for Implementation
**Approved By**: Coordinator (after specification review)
**Date**: January 7, 2026
