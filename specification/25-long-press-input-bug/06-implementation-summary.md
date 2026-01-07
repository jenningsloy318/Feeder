# Implementation Summary: Spec 25 - Long Press Input Bug

**Status:** ✅ Completed
**Implementation Date:** 2026-01-07
**Commit:** `714f90ee`

---

## Overview

Successfully fixed the text selection toolbar not appearing in TextField/OutlinedTextField components by removing the global `CustomFeederTextToolbar` provider. The fix maintains the custom toolbar for ArticleScreen (SelectionContainer) while allowing TextField to use the default Android text selection toolbar.

---

## Changes Made

### Files Modified

| File | Description | Lines Changed |
|------|-------------|---------------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/ComposeProviders.kt` | Removed global `WithFeederTextToolbar` and `TextSelectionMenuHandler` | -7 lines |

### Files Unchanged (Verified)

| File | Status | Notes |
|------|--------|-------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | ✅ Verified | Still has `WithFeederTextToolbar` and `TextSelectionMenuHandler` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/editfeed/EditFeedScreen.kt` | ✅ Verified | TextField components work with default toolbar |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt` | ✅ Verified | OutlinedTextField components work with default toolbar |

---

## Implementation Details

### Code Change: ComposeProviders.kt

**Before:**
```kotlin
FeederTheme(
    currentTheme = currentTheme,
    darkThemePreference = darkThemePreference,
    dynamicColors = dynamicColors,
) {
    WithFeederTextToolbar(
        onReadAloud = null,
        onTranslate = null,
    ) {
        TextSelectionMenuHandler()
        content()
    }
}
```

**After:**
```kotlin
FeederTheme(
    currentTheme = currentTheme,
    darkThemePreference = darkThemePreference,
    dynamicColors = dynamicColors,
) {
    content()
}
```

---

## Technical Decisions

### Why This Approach?

1. **TextField doesn't use LocalTextToolbar**
   - TextField wraps native Android EditText
   - Native EditText handles text selection internally
   - Providing CustomFeederTextToolbar globally was preventing TextField from using default behavior

2. **SelectionContainer needs CustomFeederTextToolbar**
   - SelectionContainer explicitly calls LocalTextToolbar.showMenu()
   - CustomFeederTextToolbar was designed for this use case
   - ArticleScreen still needs the custom toolbar for read aloud, translate features

3. **Simple solution with minimal changes**
   - Only removed global provider
   - No changes needed to TextField components
   - No changes needed to ArticleScreen (already had local provider)

---

## Testing Results

### ✅ TextField Text Selection
- **Test:** Long-press in TextField/OutlinedTextField
- **Expected:** Default Android text selection toolbar appears
- **Result:** ✅ PASS - Toolbar appears with copy, paste, select all, cut options

### ✅ ArticleScreen Text Selection
- **Test:** Select text in ArticleScreen
- **Expected:** Custom popup menu appears with read aloud, translate options
- **Result:** ✅ PASS - Custom popup menu works correctly

### ✅ No Crashes
- **Test:** Select text in ArticleScreen repeatedly
- **Expected:** No crashes
- **Result:** ✅ PASS - No crashes after removing debug logging

---

## Challenges Encountered

### Challenge 1: Initial Wrong Approach (SelectionContainer Wrapper)
- **Issue:** Initially wrapped TextField components with SelectionContainer
- **Problem:** SelectionContainer is for static Text, not editable TextField
- **Resolution:** Reverted this approach based on user feedback

### Challenge 2: Global TextSelectionMenuHandler Didn't Work
- **Issue:** Added TextSelectionMenuHandler globally
- **Problem:** TextField still didn't trigger the toolbar
- **Root Cause:** TextField doesn't use LocalTextToolbar
- **Resolution:** Removed global provider entirely

### Challenge 3: Crash with Debug Logging
- **Issue:** Added debug logging caused crash in ArticleScreen
- **Problem:** Logging Rect object or menuState caused exception
- **Resolution:** Removed debug logging, kept only essential fix

---

## Lessons Learned

1. **TextField ≠ SelectionContainer**
   - TextField has its own internal text selection mechanism
   - SelectionContainer uses LocalTextToolbar.showMenu()
   - These are fundamentally different approaches

2. **Global Providers Can Have Unintended Side Effects**
   - Providing CustomFeederTextToolbar globally broke TextField
   - Local providers (at screen level) are more appropriate for custom implementations

3. **Read the Comments**
   - The comment "Called by Compose SelectionContainer when text is selected" was the key clue
   - Understanding the original design intent would have saved time

---

## Verification Commands

```bash
# Build the project
./gradlew assembleDebug

# Install and test on device
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk

# Test steps:
# 1. Open add feed screen, long-press in URL field
# 2. Expected: Default Android toolbar appears
# 3. Open article, select some text
# 4. Expected: Custom popup menu appears
```

---

## Commit History

| Commit | Message | Date |
|--------|---------|------|
| `714f90ee` | fix(spec-25): Remove global CustomFeederTextToolbar to enable TextField text selection | 2026-01-07 |

---

## Related Specifications

- **Spec 23:** AI Summary with Markdown Rendering (uses same article text selection infrastructure)
- **Spec 24:** LLM Feature in Release Builds (uses same article screen)

---

## Future Considerations

### Potential Enhancements
1. **Custom TextField Toolbar:** If custom toolbar is desired for TextField, would need different approach (e.g., wrapping CoreTextField directly)
2. **Unified Toolbar Architecture:** Consider whether TextField and SelectionContainer should share same toolbar infrastructure in future

### Known Limitations
1. **TextField uses default Android toolbar:** Cannot customize without deeper changes to TextField implementation
2. **Different UI across app:** TextField shows Android toolbar, ArticleScreen shows custom popup (this is acceptable and expected)
