# Specification: Fix TextField Text Selection Toolbar

**Spec ID:** 25
**Title:** Long Press Input Bug - TextField Text Selection Toolbar
**Status:** ✅ Completed
**Created:** 2026-01-07
**Updated:** 2026-01-07

---

## Problem Statement

### Bug Description
When long-pressing in TextField/OutlinedTextField input components (e.g., add feed screen, provider edit screen), the text selection toolbar (copy, paste, select all, etc.) does NOT appear. However, text selection toolbar DOES work correctly on the ArticleScreen for static text selection.

### Affected Components
- `OutlinedTextField` in EditFeedScreen.kt
- `OutlinedTextField` in ProviderEditScreen.kt
- Other TextField components throughout the app

### Working Components
- `SelectionContainer` in ArticleScreen.kt (custom popup menu works correctly)

---

## Root Cause Analysis

### Technical Investigation

1. **CustomFeederTextToolbar Design**
   - The comment in `CustomFeederTextToolbar.kt` (line 61) states: "Called by Compose SelectionContainer when text is selected"
   - This confirms the custom toolbar was designed specifically for `SelectionContainer`, NOT for `TextField`

2. **TextField vs SelectionContainer**
   - `TextField`/`OutlinedTextField` uses `CoreTextField` which wraps native Android `EditText`
   - Native Android `EditText` has its own text selection handling that **bypasses `LocalTextToolbar` entirely**
   - `SelectionContainer` explicitly calls `LocalTextToolbar.showMenu()` when text is selected

3. **Global Provider Issue**
   - `WithFeederTextToolbar` was provided globally in `ComposeProviders.kt`
   - This provided `CustomFeederTextToolbar` via `LocalTextToolbar` to the entire app
   - For `SelectionContainer`: Works correctly (uses `LocalTextToolbar`)
   - For `TextField`: Doesn't work (uses native Android handling, ignores `LocalTextToolbar`)

4. **ComposeFoundationFlags Setting**
   - `ComposeFoundationFlags.isNewContextMenuEnabled = false` (set in `FeederApplication.kt`)
   - This forces `SelectionContainer` to use the old code path that calls `LocalTextToolbar.showMenu()`
   - It does NOT affect `TextField` which uses different code path

### Root Cause Summary

> **The root cause is architectural:** `CustomFeederTextToolbar` was designed for `SelectionContainer` (static text selection), not `TextField` (editable input fields). Providing it globally prevented `TextField` from using its default Android text selection toolbar.

---

## Solution Design

### Approach
Remove the global `WithFeederTextToolbar` provider and only provide it where `SelectionContainer` is explicitly used.

### Implementation Details

1. **Remove from ComposeProviders.kt**
   ```kotlin
   // BEFORE (WRONG - breaks TextField):
   FeederTheme {
       WithFeederTextToolbar(onReadAloud = null, onTranslate = null) {
           TextSelectionMenuHandler()
           content()
       }
   }

   // AFTER (CORRECT - lets TextField use default):
   FeederTheme {
       content()
   }
   ```

2. **Keep in ArticleScreen.kt**
   ```kotlin
   // This remains - SelectionContainer needs CustomFeederTextToolbar:
   WithFeederTextToolbar(
       onReadAloud = { ttsOnPlay() },
       onTranslate = { onTranslate() },
   ) {
       ArticleScreenInternal(...)
   }
   ```

### Expected Behavior After Fix

| Component | Text Selection Behavior |
|-----------|------------------------|
| TextField / OutlinedTextField | Default Android toolbar (copy, paste, select all, cut) |
| SelectionContainer (ArticleScreen) | Custom popup menu with read aloud, translate, etc. |

---

## Technical Considerations

### Why TextField Doesn't Use LocalTextToolbar

```
┌─────────────────────────────────────────────────────────────┐
│                    TextField Architecture                    │
├─────────────────────────────────────────────────────────────┤
│  TextField → CoreTextField → Native Android EditText        │
│                                                              │
│  Native EditText handles text selection internally:          │
│  - Shows/hides its own toolbar                               │
│  - Bypasses LocalTextToolbar.showMenu()                      │
│  - Uses platform's TextToolbar implementation                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                SelectionContainer Architecture               │
├─────────────────────────────────────────────────────────────┤
│  SelectionContainer → LocalTextToolbar.showMenu()           │
│                                                              │
│  SelectionContainer explicitly calls:                        │
│  - LocalTextToolbar.current.showMenu()                       │
│  - Respects CustomFeederTextToolbar implementation           │
└─────────────────────────────────────────────────────────────┘
```

### Files Modified

1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/ComposeProviders.kt`
   - Removed global `WithFeederTextToolbar` provider
   - Removed global `TextSelectionMenuHandler`

### Files Unchanged (Verified)

1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
   - Still has `WithFeederTextToolbar` wrapper ✅
   - Still has `TextSelectionMenuHandler` ✅

2. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/editfeed/EditFeedScreen.kt`
   - TextField components (no changes needed)

3. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
   - OutlinedTextField components (no changes needed)

---

## Testing Checklist

- [x] **TextField text selection**: Long-press in TextField shows default Android toolbar
- [x] **OutlinedTextField text selection**: Long-press in OutlinedTextField shows default Android toolbar
- [x] **ArticleScreen text selection**: Select text in article shows custom popup menu
- [x] **No crashes**: ArticleScreen text selection works without crashes
- [x] **Copy/Paste functionality**: Verify copy and paste work in TextField

---

## Related Code

### CustomFeederTextToolbar.kt
- Implements `TextToolbar` interface
- Designed for `SelectionContainer` usage
- `showMenu()` is called by `SelectionContainer`, NOT by `TextField`

### WithFeederTextToolbar (Composable)
- Provides `CustomFeederTextToolbar` via `LocalTextToolbar`
- Should only be used where `SelectionContainer` is present

### ComposeFoundationFlags.isNewContextMenuEnabled
- Set to `false` in `FeederApplication.kt`
- Forces `SelectionContainer` to use old code path
- Does NOT affect `TextField` behavior
