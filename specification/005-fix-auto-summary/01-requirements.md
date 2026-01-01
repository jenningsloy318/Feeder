# Requirements: Fix Auto-Summary Not Triggering

## Bug Report
**Date:** 2026-01-01
**Type:** Bug Fix
**Priority:** High
**Status:** Requirements Clarified

## Problem Statement
The auto-summarization feature is not working when users open articles. The crash in settings has been fixed (spec-04), but the core functionality - automatic summarization when reading articles - is still broken.

## User Report

### Steps to Reproduce
1. Open Feeder app
2. Navigate to Settings → AI Integration → Summary
3. Verify "Enable Auto Summary" toggle is ON
4. Tap on any article to open it
5. Article content loads but **no summary appears automatically**

### Expected Behavior
- When opening an article with auto-summary enabled:
  - After the full content is loaded, the summary should **automatically appear**
  - Same behavior as manually clicking the "summarize" button in the three-dots menu
  - Summary should display in the **same location** as the existing manual summary
- The existing manual summary button works correctly (in the three-dots menu)

### Actual Behavior
- Opening an article shows the content but **no auto-summary**
- User must manually tap the three-dots menu → "summarize" button to see the summary
- Manual summarization **works correctly** (using Anthropic provider)

### Environment
- **Android version:** 16
- **Device:** Physical device
- **AI Provider:** Anthropic (manual summarize works fine)

## Technical Context
- The manual summarize button in the three-dots menu works correctly
- Need to find where the article view is loaded and trigger auto-summary there
- Check the `summaryEnabled` setting before auto-summarizing
- The summary should appear after full content is loaded
- Settings are stored in `SettingsStore` with `summaryEnabled` StateFlow
- AI integration uses `AIApi.summarize()` method

## Success Criteria
1. When `summaryEnabled` setting is ON:
   - Opening an article triggers automatic summarization
   - Summary appears after article content is loaded
   - No user interaction required
2. When `summaryEnabled` setting is OFF:
   - Manual summarization still works via three-dots menu
3. No crashes or errors during auto-summarization
4. Auto-summarization respects the same error handling as manual summarization

## Related Work
- spec-04: Fixed crash in SummarySettingsViewModel (now extends DIAwareViewModel)
- Manual summarization feature already working
