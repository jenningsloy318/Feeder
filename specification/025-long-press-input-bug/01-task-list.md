# Task List: Spec 25 - Long Press Input Bug

**Status:** ✅ Completed

## Overview
Fix text selection toolbar not appearing when long-pressing in TextField/OutlinedTextField input components, while maintaining custom toolbar for ArticleScreen.

---

## Tasks

### Phase 1: Investigation & Research
- [x] Investigate why TextField doesn't show text selection toolbar
- [x] Research how TextField text selection works in Jetpack Compose
- [x] Research how SelectionContainer text selection works
- [x] Understand the relationship between LocalTextToolbar and TextField vs SelectionContainer
- [x] Identify that CustomFeederTextToolbar was designed for SelectionContainer, not TextField

### Phase 2: Root Cause Analysis
- [x] Identify that TextField uses native Android EditText which bypasses LocalTextToolbar
- [x] Identify that providing CustomFeederTextToolbar globally prevents TextField from using default toolbar
- [x] Determine that ComposeFoundationFlags.isNewContextMenuEnabled affects SelectionContainer, not TextField

### Phase 3: Solution Design
- [x] Design solution: Remove global WithFeederTextToolbar provider
- [x] Keep WithFeederTextToolbar only where SelectionContainer is used (ArticleScreen)
- [x] Let TextField use default Android text selection toolbar

### Phase 4: Implementation
- [x] Remove WithFeederTextToolbar from ComposeProviders.kt
- [x] Remove TextSelectionMenuHandler from ComposeProviders.kt
- [x] Verify ArticleScreen still has WithFeederTextToolbar and TextSelectionMenuHandler
- [x] Build successfully

### Phase 5: Testing & Verification
- [x] User tested TextField text selection - works ✅
- [x] User tested ArticleScreen text selection - works ✅
- [x] No crashes in ArticleScreen ✅

### Phase 6: Documentation
- [x] Create task list document
- [x] Create specification document
- [x] Create implementation summary document
- [x] Commit and push all changes

---

## Summary

**Root Cause:** CustomFeederTextToolbar was designed for SelectionContainer (static text selection), not TextField (editable text fields). Providing it globally prevented TextField from using its default Android text selection toolbar.

**Solution:** Remove global WithFeederTextToolbar provider. TextField now uses default Android toolbar, while ArticleScreen (SelectionContainer) keeps its custom toolbar.

**Files Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/ComposeProviders.kt`
