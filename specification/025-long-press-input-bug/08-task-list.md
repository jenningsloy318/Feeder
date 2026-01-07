# Task List - Spec 25: Long-Press Input Bug Fix

## Task Overview
Total tasks: 21 tasks across 7 files
Estimated time: ~2 hours
Priority: Bug fix - Medium priority

## Task Legend
- [ ] Pending
- [x] Complete
- [!] Blocked
- [>] In Progress

---

## Phase 1: High-Priority User Screens

### Task Group 1.1: EditFeedScreen.kt

#### Task 1.1.1: Add Import Statement
- [ ] File: `EditFeedScreen.kt`
- [ ] Action: Add `import androidx.compose.foundation.text.selection.SelectionContainer`
- [ ] Location: Top of file with other imports
- [ ] Estimated time: 1 minute
- [ ] Status: Pending

#### Task 1.1.2: Find All TextField Instances
- [ ] File: `EditFeedScreen.kt`
- [ ] Action: Search and count all TextField instances
- [ ] Action: Search and count all OutlinedTextField instances
- [ ] Note: Count and list line numbers
- [ ] Estimated time: 2 minutes
- [ ] Status: Pending

#### Task 1.1.3: Wrap TextField Instances
- [ ] File: `EditFeedScreen.kt`
- [ ] Action: Wrap each TextField with SelectionContainer
- [ ] Constraint: Keep all existing properties and modifiers
- [ ] Constraint: Don't move modifiers to SelectionContainer
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

#### Task 1.1.4: Wrap OutlinedTextField Instances
- [ ] File: `EditFeedScreen.kt`
- [ ] Action: Wrap each OutlinedTextField with SelectionContainer
- [ ] Constraint: Keep all existing properties and modifiers
- [ ] Constraint: Don't move modifiers to SelectionContainer
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

#### Task 1.1.5: Build Verification
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build the project
- [ ] Expected: Build succeeds without errors
- [ ] On failure: Fix compilation errors before proceeding
- [ ] Estimated time: 3 minutes
- [ ] Status: Pending

#### Task 1.1.6: Manual Testing
- [ ] Action: Open feed creation screen
- [ ] Action: Test long-press on feed URL field
- [ ] Action: Test long-press on feed title field
- [ ] Action: Test long-press on tag fields
- [ ] Action: Test copy functionality
- [ ] Action: Test paste functionality
- [ ] Action: Test select all functionality
- [ ] Action: Verify keyboard interaction unchanged
- [ ] Action: Verify focus behavior unchanged
- [ ] Action: Verify validation still works
- [ ] Expected: All tests pass
- [ ] Estimated time: 10 minutes
- [ ] Status: Pending

---

### Task Group 1.2: ProviderEditScreen.kt

#### Task 1.2.1: Add Import Statement
- [ ] File: `ProviderEditScreen.kt`
- [ ] Action: Add `import androidx.compose.foundation.text.selection.SelectionContainer`
- [ ] Location: Top of file with other imports
- [ ] Estimated time: 1 minute
- [ ] Status: Pending

#### Task 1.2.2: Find All TextField Instances
- [ ] File: `ProviderEditScreen.kt`
- [ ] Action: Search and count all OutlinedTextField instances
- [ ] Action: Check for password fields (KeyboardType.Password)
- [ ] Note: Count and list line numbers
- [ ] Estimated time: 2 minutes
- [ ] Status: Pending

#### Task 1.2.3: Wrap OutlinedTextField Instances
- [ ] File: `ProviderEditScreen.kt`
- [ ] Action: Wrap each OutlinedTextField with SelectionContainer
- [ ] Exception: DO NOT wrap password fields
- [ ] Constraint: Keep all existing properties and modifiers
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

#### Task 1.2.4: Build Verification
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build the project
- [ ] Expected: Build succeeds without errors
- [ ] Estimated time: 3 minutes
- [ ] Status: Pending

#### Task 1.2.5: Manual Testing
- [ ] Action: Open provider edit screen
- [ ] Action: Test long-press on provider name field
- [ ] Action: Test long-press on API key field
- [ ] Action: Test long-press on base URL field
- [ ] Action: Test copy/paste/select all
- [ ] Action: Verify password field NOT selectable
- [ ] Action: Verify no regressions
- [ ] Expected: All tests pass
- [ ] Estimated time: 10 minutes
- [ ] Status: Pending

---

### Task Group 1.3: SearchFeedScreen.kt

#### Task 1.3.1: Add Import Statement
- [ ] File: `SearchFeedScreen.kt`
- [ ] Action: Add `import androidx.compose.foundation.text.selection.SelectionContainer`
- [ ] Location: Top of file with other imports
- [ ] Estimated time: 1 minute
- [ ] Status: Pending

#### Task 1.3.2: Find All TextField Instances
- [ ] File: `SearchFeedScreen.kt`
- [ ] Action: Search and count all TextField/OutlinedTextField instances
- [ ] Note: Count and list line numbers
- [ ] Estimated time: 2 minutes
- [ ] Status: Pending

#### Task 1.3.3: Wrap TextField Instances
- [ ] File: `SearchFeedScreen.kt`
- [ ] Action: Wrap each TextField/OutlinedTextField with SelectionContainer
- [ ] Constraint: Keep all existing properties and modifiers
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

#### Task 1.3.4: Build Verification
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build the project
- [ ] Expected: Build succeeds without errors
- [ ] Estimated time: 3 minutes
- [ ] Status: Pending

#### Task 1.3.5: Manual Testing
- [ ] Action: Open feed search screen
- [ ] Action: Test long-press on search field
- [ ] Action: Test copy/paste/select all
- [ ] Action: Verify search functionality still works
- [ ] Expected: All tests pass
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

---

## Phase 2: Settings and Configuration Screens

### Task Group 2.1: SyncScreen.kt

#### Task 2.1.1: Add Import Statement
- [ ] File: `SyncScreen.kt`
- [ ] Action: Add `import androidx.compose.foundation.text.selection.SelectionContainer`
- [ ] Estimated time: 1 minute
- [ ] Status: Pending

#### Task 2.1.2: Find All TextField Instances
- [ ] File: `SyncScreen.kt`
- [ ] Action: Search and count all TextField/OutlinedTextField instances
- [ ] Action: Check for password fields
- [ ] Estimated time: 2 minutes
- [ ] Status: Pending

#### Task 2.1.3: Wrap TextField Instances
- [ ] File: `SyncScreen.kt`
- [ ] Action: Wrap each TextField/OutlinedTextField with SelectionContainer
- [ ] Exception: DO NOT wrap password fields
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

#### Task 2.1.4: Build and Test
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build and manual test
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

---

### Task Group 2.2: EditableListDialog.kt

#### Task 2.2.1: Add Import and Wrap Fields
- [ ] File: `EditableListDialog.kt`
- [ ] Action: Add import statement
- [ ] Action: Find and wrap all TextField instances
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

#### Task 2.2.2: Build and Test
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build and manual test dialog
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

---

### Task Group 2.3: FeedNotificationsDialog.kt

#### Task 2.3.1: Add Import and Wrap Fields
- [ ] File: `FeedNotificationsDialog.kt`
- [ ] Action: Add import statement
- [ ] Action: Find and wrap all TextField instances
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

#### Task 2.3.2: Build and Test
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build and manual test dialog
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

---

## Phase 3: Reusable Components

### Task Group 3.1: AutoCompleteText.kt

#### Task 3.1.1: Add Import and Wrap TextField
- [ ] File: `AutoCompleteText.kt`
- [ ] Action: Add import statement
- [ ] Action: Find and wrap TextField component
- [ ] Note: This is a reusable component, test all usages
- [ ] Estimated time: 8 minutes
- [ ] Status: Pending

#### Task 3.1.2: Build and Test
- [ ] Command: `./gradlew assembleDebug`
- [ ] Action: Build and manual test
- [ ] Action: Test autocomplete in all screens where used
- [ ] Estimated time: 10 minutes
- [ ] Status: Pending

---

## Final Tasks

### Task 7.1: Run All Tests
- [ ] Command: `./gradlew test`
- [ ] Action: Run unit tests
- [ ] Expected: All tests pass
- [ ] Estimated time: 5 minutes
- [ ] Status: Pending

### Task 7.2: Run Instrumented Tests
- [ ] Command: `./gradlew connectedAndroidTest`
- [ ] Action: Run instrumented tests
- [ ] Expected: All tests pass
- [ ] Estimated time: 10 minutes
- [ ] Status: Pending

### Task 7.3: Code Quality Check
- [ ] Command: `./gradlew ktlintCheck`
- [ ] Action: Run ktlint code quality check
- [ ] Expected: No violations
- [ ] On violations: Fix before proceeding
- [ ] Estimated time: 3 minutes
- [ ] Status: Pending

### Task 7.4: Final Verification
- [ ] Action: Review all modified files
- [ ] Action: Ensure all TextField instances are wrapped
- [ ] Action: Ensure password fields are NOT wrapped
- [ ] Action: Verify all screens tested
- [ ] Action: Verify no regressions
- [ ] Expected: Complete coverage, zero issues
- [ ] Estimated time: 10 minutes
- [ ] Status: Pending

---

## Task Summary

### By Phase
- **Phase 1** (Priority 1): 17 tasks
- **Phase 2** (Priority 2): 6 tasks
- **Phase 3** (Priority 3): 2 tasks
- **Final Tasks**: 4 tasks
- **Total**: 29 tasks

### By File
1. EditFeedScreen.kt: 6 tasks
2. ProviderEditScreen.kt: 5 tasks
3. SearchFeedScreen.kt: 5 tasks
4. SyncScreen.kt: 4 tasks
5. EditableListDialog.kt: 2 tasks
6. FeedNotificationsDialog.kt: 2 tasks
7. AutoCompleteText.kt: 2 tasks
8. Final verification: 4 tasks

### Progress Tracking
- **Total Tasks**: 29
- **Completed**: 0
- **In Progress**: 0
- **Remaining**: 29
- **Progress**: 0%

## Notes

1. **Execute tasks sequentially within each file**
2. **Don't skip build verification tasks**
3. **Test thoroughly after each file**
4. **Mark tasks complete as you go**
5. **Update progress after each file**
6. **If blocked, note the blocker and move to next file**

## References
- **Implementation Plan**: 07-implementation-plan.md
- **Technical Specification**: 06-specification.md
- **Task Tracking**: Update this file as you progress

## Task Completion Log

### File 1: EditFeedScreen.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 2: ProviderEditScreen.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 3: SearchFeedScreen.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 4: SyncScreen.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 5: EditableListDialog.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 6: FeedNotificationsDialog.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

### File 7: AutoCompleteText.kt
- Started: [DATE]
- Completed: [DATE]
- Issues: [NONE or description]
- Notes: [Any observations]

---

**Task List Status**: Ready for Execution
**Last Updated**: January 7, 2026
