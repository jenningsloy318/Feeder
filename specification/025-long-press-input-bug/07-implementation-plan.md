# Implementation Plan - Spec 25: Long-Press Input Bug Fix

## Overview
This plan outlines the systematic approach to add text selection support to all TextField and OutlinedTextField components in the Feeder app.

## Implementation Strategy

### Approach
**Incremental, file-by-file implementation with testing after each file**

This approach follows the project philosophy of incremental development with continuous verification.

### Why This Approach
1. **Early error detection**: Catch issues immediately
2. **Easy rollback**: Revert single file if needed
3. **Continuous testing**: Test each screen before moving on
4. **Progress tracking**: Clear progress indication
5. **Risk mitigation**: Isolate potential problems

## Phases

### Phase 1: High-Priority User Screens

#### Task 1.1: EditFeedScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/editfeed/EditFeedScreen.kt`
**Estimated TextField Count**: 5-7
**Priority**: HIGH
**User Impact**: Core feature (creating/editing feeds)

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all TextField instances
3. Find all OutlinedTextField instances
4. Wrap each with SelectionContainer
5. Build project
6. Manual test: Open feed creation, test text selection in all fields
7. Verify no regressions

**Acceptance**:
- [ ] Text selection works in all feed URL fields
- [ ] Text selection works in all feed title fields
- [ ] Text selection works in all tag fields
- [ ] Text selection works in all other text fields
- [ ] No visual artifacts
- [ ] No functional regressions

#### Task 1.2: ProviderEditScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Estimated TextField Count**: 5
**Priority**: HIGH
**User Impact**: Recent feature (Spec-21, Spec-24) - AI provider configuration

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all OutlinedTextField instances
3. Check for password fields (exclude these)
4. Wrap each with SelectionContainer
5. Build project
6. Manual test: Open provider edit, test text selection in all fields
7. Verify no regressions

**Acceptance**:
- [ ] Text selection works in provider name field
- [ ] Text selection works in API key field
- [ ] Text selection works in base URL field
- [ ] Text selection works in all configuration fields
- [ ] Password field NOT wrapped (if present)
- [ ] No visual artifacts
- [ ] No functional regressions

#### Task 1.3: SearchFeedScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/searchfeed/SearchFeedScreen.kt`
**Estimated TextField Count**: 1-2
**Priority**: HIGH
**User Impact**: Feed discovery feature

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all TextField/OutlinedTextField instances
3. Wrap each with SelectionContainer
4. Build project
5. Manual test: Open feed search, test text selection
6. Verify no regressions

**Acceptance**:
- [ ] Text selection works in search field
- [ ] No visual artifacts
- [ ] No functional regressions

### Phase 2: Settings and Configuration Screens

#### Task 2.1: SyncScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/sync/SyncScreen.kt`
**Estimated TextField Count**: TBD
**Priority**: MEDIUM
**User Impact**: Sync settings configuration

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all TextField/OutlinedTextField instances
3. Check for password fields (exclude these)
4. Wrap each with SelectionContainer
5. Build project
6. Manual test: Open sync settings, test text selection
7. Verify no regressions

**Acceptance**:
- [ ] Text selection works in all sync configuration fields
- [ ] Password fields excluded (if any)
- [ ] No visual artifacts
- [ ] No functional regressions

#### Task 2.2: EditableListDialog.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/dialog/EditableListDialog.kt`
**Estimated TextField Count**: TBD
**Priority**: MEDIUM
**User Impact**: Edit lists (tags, etc.)

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all TextField/OutlinedTextField instances
3. Wrap each with SelectionContainer
4. Build project
5. Manual test: Trigger dialog, test text selection
6. Verify no regressions

**Acceptance**:
- [ ] Text selection works in dialog text field
- [ ] No visual artifacts
- [ ] No functional regressions

#### Task 2.3: FeedNotificationsDialog.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/dialog/FeedNotificationsDialog.kt`
**Estimated TextField Count**: TBD
**Priority**: MEDIUM
**User Impact**: Notification settings

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find all TextField/OutlinedTextField instances
3. Wrap each with SelectionContainer
4. Build project
5. Manual test: Trigger dialog, test text selection
6. Verify no regressions

**Acceptance**:
- [ ] Text selection works in notification fields
- [ ] No visual artifacts
- [ ] No functional regressions

### Phase 3: Reusable Components

#### Task 3.1: AutoCompleteText.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/components/AutoCompleteText.kt`
**Estimated TextField Count**: 1 (template)
**Priority**: HIGH
**User Impact**: Reusable component used in multiple places

**Steps**:
1. Add import: `import androidx.compose.foundation.text.selection.SelectionContainer`
2. Find TextField instance in component
3. Wrap with SelectionContainer
4. Build project
5. Manual test: Test autocomplete in all usages
6. Verify no regressions

**Acceptance**:
- [ ] Text selection works in autocomplete field
- [ ] Autocomplete functionality still works
- [ ] No visual artifacts
- [ ] No functional regressions

## Implementation Details

### Code Change Pattern

#### Step 1: Add Import
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer
```

#### Step 2: Wrap TextField
```kotlin
// BEFORE
TextField(
    value = text,
    onValueChange = { text = it },
    modifier = Modifier.fillMaxWidth()
)

// AFTER
SelectionContainer {
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth()
    )
}
```

#### Step 3: Wrap OutlinedTextField
```kotlin
// BEFORE
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Label") }
)

// AFTER
SelectionContainer {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}
```

### Important Notes

1. **Modifiers stay on TextField**: Don't move modifiers to SelectionContainer
2. **Keep all TextField properties**: Don't change existing properties
3. **Check for passwords**: Exclude password fields
4. **Build after each file**: Catch compilation errors early
5. **Test after each file**: Verify functionality works

## Testing Strategy

### Build Verification
After each file change:
```bash
./gradlew assembleDebug
```

### Manual Testing Checklist
For each modified screen:
- [ ] Open the screen
- [ ] Long-press on text field
- [ ] Verify selection handles appear
- [ ] Verify toolbar appears (copy, paste, cut, select all)
- [ ] Test copy: Select text → Copy → Verify in clipboard
- [ ] Test paste: Copy text → Paste in field → Verify text appears
- [ ] Test select all: Long-press → Select all → Verify all selected
- [ ] Test keyboard: Type text → Verify keyboard interaction unchanged
- [ ] Test focus: Tab through fields → Verify focus behavior unchanged
- [ ] Test validation: Submit form → Verify validation still works

### Automated Testing
Run existing tests to ensure no regressions:
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Risk Mitigation

### If Issues Arise

**Scenario 1: Visual Artifacts**
- **Symptom**: Visual glitches after adding SelectionContainer
- **Action**: Check that modifiers are on TextField, not SelectionContainer
- **Fallback**: Remove wrapper from problematic field only

**Scenario 2: Broken Functionality**
- **Symptom**: TextField stops working (can't type, focus issues, etc.)
- **Action**: Verify SelectionContainer only wraps the TextField, not parent layouts
- **Fallback**: Remove wrapper from problematic field only

**Scenario 3: Compilation Errors**
- **Symptom**: Build fails after adding SelectionContainer
- **Action**: Verify import statement is correct
- **Action**: Verify syntax is correct (SelectionContainer { ... })
- **Fallback**: Revert file changes and investigate

**Scenario 4: Performance Issues**
- **Symptom**: Screen becomes sluggish
- **Action**: Profile to identify bottleneck
- **Likely**: Not related to SelectionContainer (lightweight wrapper)
- **Fallback**: Investigate other causes

## Rollback Plan

### Partial Rollback
If a single file causes issues:
```bash
git checkout HEAD -- app/src/main/java/com/nononsenseapps/feeder/ui/compose/ProblematicFile.kt
```

### Full Rollback
If the entire change needs to be reverted:
```bash
git reset --hard HEAD
```

## Success Criteria

### Phase Success
- [ ] All files in phase modified
- [ ] All files build without errors
- [ ] All screens in phase tested manually
- [ ] Text selection works on all screens in phase
- [ ] No regressions detected

### Overall Success
- [ ] All 7 files modified
- [ ] All screens tested
- [ ] Text selection works everywhere
- [ ] All existing tests pass
- [ ] Zero regressions
- [ ] Zero ktlint violations

## Progress Tracking

### Status Dashboard
- **Total Files**: 7
- **Completed**: 0
- **In Progress**: 0
- **Remaining**: 7

### Phase Status
- **Phase 1** (Priority 1): 0/3 files
- **Phase 2** (Priority 2): 0/3 files
- **Phase 3** (Priority 3): 0/1 files

## Time Estimates

### Per File
- **Code changes**: 5-10 minutes
- **Build**: 2-3 minutes
- **Testing**: 5-10 minutes
- **Total per file**: 12-23 minutes

### Total Estimate
- **Best case**: 84 minutes (7 files × 12 min)
- **Worst case**: 161 minutes (7 files × 23 min)
- **Average**: ~2 hours

## Notes

1. **Start with Priority 1**: High-impact screens first
2. **Test thoroughly**: Better to spend more time testing than fixing bugs later
3. **Commit frequently**: After each working file (optional)
4. **Document issues**: Note any edge cases or problems encountered
5. **Ask for help**: If stuck on a particular file

## References
- **Technical Specification**: 06-specification.md
- **Research Report**: 02-research-report.md
- **Working Example**: ReaderView.kt line 110

## Approval

**Implementation Plan Status**: Ready for Execution
**Coordinator Approval**: Pending (after specification review)
**Date**: January 7, 2026
