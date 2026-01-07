# Debug Analysis - Spec 25: Long-Press Input Bug Fix

## Bug Statement
**Symptom**: Long-pressing in input fields (TextField, OutlinedTextField) does not show the text selection toolbar (copy, paste, select all).
**Context**: Works correctly on article page but not in input forms throughout the app.

## Evidence Collection

### 1. Working Example: Article Page

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ReaderView.kt`
**Line**: 110

```kotlin
SelectionContainer {
    LazyColumnScrollbar(
        state = articleListState,
        settings = ScrollbarSettings.Default.copy(
            thumbUnselectedColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        LazyColumn(
            state = articleListState,
            // ... LazyColumn content
        ) {
            // Article text rendered here
        }
    }
}
```

**Analysis**:
- `SelectionContainer` wrapper is present
- Text selection works as expected
- Toolbar appears on long-press
- User can copy, paste, select all

### 2. Non-Working Examples: Input Screens

#### Example 1: ProviderEditScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`

**Import Check**:
```bash
grep -n "import.*SelectionContainer" ProviderEditScreen.kt
# Result: No import found
```

**TextField Usage** (lines 100+):
```kotlin
// No SelectionContainer wrapper
OutlinedTextField(
    value = ...,
    onValueChange = ...,
    // ... other properties
)
```

**Analysis**: Missing `SelectionContainer` wrapper

#### Example 2: EditFeedScreen.kt
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/editfeed/EditFeedScreen.kt`

**Import Check**:
```bash
grep -n "import.*SelectionContainer" EditFeedScreen.kt
# Result: No import found
```

**TextField Usage**:
```kotlin
// Multiple TextField and OutlinedTextField instances
// None wrapped in SelectionContainer
```

**Analysis**: Missing `SelectionContainer` wrapper

### 3. Global Search Results

**Search Command**:
```bash
find app/src/main -name "*.kt" | xargs grep -l "SelectionContainer"
```

**Result**: Only 1 file found
- `ReaderView.kt` (article reading screen)

**Complementary Search**:
```bash
find app/src/main -name "*.kt" | xargs grep -l "TextField\|OutlinedTextField"
```

**Result**: 9+ files with TextField components, only 1 with SelectionContainer

## Root Cause Identification

### Primary Cause
**Missing `SelectionContainer` wrapper** in TextField and OutlinedTextField components throughout the application.

### Technical Explanation

#### Jetpack Compose Text Selection Behavior

**Default Behavior**:
- `TextField` and `OutlinedTextField` do NOT have text selection enabled by default
- Long-press without SelectionContainer: No effect or cursor movement only
- Long-press with SelectionContainer: Text selection + toolbar

**Why Article Page Works**:
- Explicitly wrapped in `SelectionContainer` at line 110 of ReaderView.kt
- Developer intentionally added wrapper for text selection

**Why Input Fields Don't Work**:
- No `SelectionContainer` wrapper present
- Compose default behavior (no selection)

### Is This a Compose Bug?
**No**. This is expected Compose behavior requiring explicit implementation.

**From Android Documentation**:
> "SelectionContainer is a layout that wraps composables and allows them to be selected."

**Implication**: Must be added manually, not automatic.

## Code Pattern Analysis

### Pattern 1: Working Pattern (ReaderView.kt)

```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer

// In composable
SelectionContainer {
    // Content with selectable text
}
```

**Characteristics**:
- Import present
- Wrapper applied
- Text selection works

### Pattern 2: Broken Pattern (Input Screens)

```kotlin
// NO import of SelectionContainer

// In composable
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    // No wrapper
)
```

**Characteristics**:
- No import
- No wrapper
- Text selection doesn't work

## Verification Steps

### Step 1: Confirm Hypothesis
**Action**: Add SelectionContainer to a single TextField
**Expected Result**: Text selection starts working
**Confidence Level**: High (based on research and existing working example)

### Step 2: Test Manual Fix
**Test File**: `ProviderEditScreen.kt`
**Test Change**:
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer

// Wrap one OutlinedTextField
SelectionContainer {
    OutlinedTextField(
        value = providerName,
        onValueChange = { viewModel.onProviderNameChange(it) },
        label = { Text(stringResource(R.string.provider_name)) },
        // ... rest of properties
    )
}
```

**Verification**: Long-press should show toolbar

### Step 3: Systematic Application
**Approach**: Apply wrapper to all TextField instances across all affected files
**Validation**: Test each screen after changes

## Impact Assessment

### Severity
**Medium**: Usability issue but not a blocker
- Users can still type and edit text
- Only copy/paste/select all functionality is missing
- Workaround: Use keyboard shortcuts (if available)

### Scope
**Wide**: Affects most input forms in the app
- 9+ files identified
- Estimated 20-30 TextField instances
- Core user workflows affected (creating feeds, configuring settings)

### User Impact
**Medium-High**:
- Users expect copy/paste to work in text fields
- Android standard behavior violated
- Particularly frustrating on mobile (no keyboard shortcuts)

## Fix Strategy

### Solution
Add `SelectionContainer` wrapper to all TextField and OutlinedTextField components.

### Implementation Plan
1. Add import statement to each affected file
2. Wrap each TextField/OutlinedTextField with SelectionContainer
3. Test each screen to verify functionality

### Complexity
**Low**: Simple wrapper addition
- No logic changes
- No new dependencies
- Minimal code changes

## Risk Assessment

### Technical Risk
**Low**:
- Well-documented Compose pattern
- Already working in ReaderView.kt
- No side effects expected

### Regression Risk
**Low**:
- Doesn't change existing behavior
- Only adds selection capability
- Easy to revert if issues arise

## Conclusion

**Root Cause**: Missing `SelectionContainer` wrapper in TextField components

**Solution**: Add SelectionContainer wrapper systematically to all input fields

**Confidence**: Very High - supported by:
- Working example in codebase (ReaderView.kt)
- Official Compose documentation
- Standard Android pattern

**Recommendation**: Proceed with fix as outlined in Research Report (02-research-report.md)
