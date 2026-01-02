# Requirements: Move Summarize Button to Article Page Top

**Document Version**: 1.0
**Last Updated**: 2026-01-02
**Status**: Draft

## Overview

This document clarifies the requirements for relocating UI buttons in the article page to improve user experience and accessibility.

## Current State

### Article Screen Layout

The `ArticleScreen.kt` component currently has the following button layout:

**Top Bar Actions (visible buttons)**:
1. **Fetch Full Article** - Icon: `Article` (lines 204-213)
2. **Open in Web View** - Icon: `OpenInBrowser` (lines 215-224)
3. **Three-dot Menu** - Icon: `MoreVert` (lines 226-235)

**Dropdown Menu Items** (triggered by three-dot menu):
1. Share - Icon: `Share`
2. **Summarize** - Icon: `AutoFixHigh` (conditional, `showSummarize` flag)
3. Mark as Unread - Icon: `VisibilityOff`
4. Toggle Bookmark - Icon: `Star`

### Code Location

- File: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- Composable: `ArticleScreen()` (lines 146-330+)
- Top Bar Actions: lines 203-224
- Dropdown Menu: lines 236-299+

## Problem Statement

The "Summarize" feature is a frequently used AI-powered feature that is currently hidden in the three-dot menu, making it less accessible. Meanwhile, "Open in Web View" is a secondary action that occupies prime real estate in the top bar.

### User Experience Issues

1. **Discoverability**: Users may not notice the "Summarize" feature in the dropdown menu
2. **Accessibility**: Extra tap required to access a key feature
3. **Priority**: Less frequently used action ("Open in Web View") has better visibility than more frequently used action ("Summarize")

## Requirements

### Functional Requirements

#### FR1: Move Summarize Button to Top Bar

**Requirement**: The "Summarize" button should be moved from the three-dot dropdown menu to the top action bar, positioned **before** (to the left of) the "Fetch full article" button.

**Details**:
- Position: First button in the top actions row
- Icon: `Icons.Default.AutoFixHigh`
- Content Description: `R.string.summarize`
- Tooltip: `R.string.summarize`
- onClick handler: `onSummarize()`
- Visibility: Conditional on `viewState.showSummarize` flag

**Rationale**:
- Summarize is a key AI feature that deserves prominence
- Placing it first makes it most discoverable
- Maintains logical flow with other primary actions

#### FR2: Move Open in Web View to Dropdown Menu

**Requirement**: The "Open in Web View" button should be moved from the top action bar to the three-dot dropdown menu.

**Details**:
- Position: In dropdown menu (after Share, before Mark as Unread)
- Icon: `Icons.Default.OpenInBrowser`
- Text: `R.string.open_in_web_view`
- onClick handler: `onOpenInCustomTab()`
- Menu item behavior: Closes menu on selection

**Rationale**:
- Opening in browser is a secondary action
- Less frequently used than primary actions
- Better placement in overflow menu

### Non-Functional Requirements

#### NFR1: Maintain Existing Functionality

- All button behaviors must remain unchanged
- No changes to underlying logic or handlers
- No changes to state management
- No changes to accessibility features

#### NFR2: Maintain Visual Consistency

- Follow Material Design 3 guidelines
- Use existing component styling
- Maintain spacing and alignment standards
- Preserve tooltips and content descriptions

#### NFR3: Performance

- No performance degradation
- No additional recomposition
- Smooth menu expansion/collapse

#### NFR4: Accessibility

- Maintain screen reader compatibility
- Preserve keyboard navigation
- Keep proper semantic descriptions
- Support accessibility services

#### NFR5: Code Quality

- Follow Kotlin coding conventions
- Maintain code readability
- No code duplication
- Proper separation of concerns

## Acceptance Criteria

### AC1: Top Bar Button Order

**Given**: The article screen is displayed
**When**: Viewing the top action bar
**Then**: The buttons appear in this order:
1. Summarize (if `showSummarize` is true)
2. Fetch Full Article
3. Three-dot Menu

**And**: The "Open in Web View" button is NOT in the top bar

### AC2: Dropdown Menu Items

**Given**: The article screen is displayed
**When**: Tapping the three-dot menu
**Then**: The menu items appear in this order:
1. Share
2. **Open in Web View** (NEW)
3. Mark as Unread
4. Toggle Bookmark

**And**: The "Summarize" menu item is NOT present (removed from menu)

### AC3: Summarize Button Functionality

**Given**: The article screen is displayed with Summarize button visible
**When**: Tapping the Summarize button
**Then**:
- The `onSummarize()` handler is called
- The summary is generated (existing behavior)
- No other side effects occur

### AC4: Open in Web View Menu Functionality

**Given**: The article screen is displayed
**When**: Tapping the three-dot menu and selecting "Open in Web View"
**Then**:
- The menu closes
- The `onOpenInCustomTab()` handler is called
- The article opens in a custom tab (existing behavior)

### AC5: Conditional Visibility

**Given**: The article screen is displayed
**When**: `viewState.showSummarize` is false
**Then**: The Summarize button is NOT visible in the top bar

**And**: When `viewState.showSummarize` is true
**Then**: The Summarize button IS visible in the top bar

### AC6: No Regressions

**Given**: All existing article screen features
**When**: Using any feature (bookmark, share, mark unread, TTS, etc.)
**Then**: All features work exactly as before the change

### AC7: Build and Test Success

**Given**: The code changes are complete
**When**: Running the build and test suite
**Then**:
- `./gradlew assembleDebug` succeeds
- `./gradlew test` passes
- `./gradlew lint` passes with no new warnings
- All UI tests pass

## Technical Constraints

### TC1: File Scope

- **Primary file**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- **Scope limited to**: Button reorganization only
- **No changes to**: ViewModels, repositories, database, networking

### TC2: Compose Structure

- Must use existing Compose components
- No new components required
- Maintain existing modifiers and styling
- Preserve existing state management

### TC3: Backward Compatibility

- No breaking changes
- No database migrations
- No preference changes
- No configuration file changes

## Out of Scope

The following are explicitly OUT OF SCOPE for this requirement:

1. **Changes to summarize functionality** - Only relocating the button, not changing behavior
2. **Changes to "Fetch full article"** - No changes to this button
3. **New features** - No new features being added
4. **Performance optimizations** - Beyond maintaining current performance
5. **Visual design changes** - Only repositioning, no redesign
6. **Internationalization** - No changes to strings or translations
7. **Accessibility features** - Only maintaining existing accessibility

## Dependencies

### Internal Dependencies

- `viewState.showSummarize` flag determines Summarize button visibility
- `onSummarize()` handler must be available in composable parameters
- `onOpenInCustomTab()` handler must be available in composable parameters

### External Dependencies

- Material Design 3 components
- AndroidX Compose libraries
- Existing string resources (`R.string.*`)

## Risks and Mitigations

### Risk 1: User Confusion

**Risk**: Users may be confused by button location changes

**Mitigation**:
- Change is intuitive (Summarize more prominent)
- Consistent with common UI patterns
- No behavior changes, only location

### Risk 2: Regression in Functionality

**Risk**: Moving buttons might break existing functionality

**Mitigation**:
- Comprehensive testing
- Code review before merge
- Maintain existing handlers

### Risk 3: Visual Issues

**Risk**: New button order might cause layout issues

**Mitigation**:
- Follow existing component patterns
- Test on different screen sizes
- Ensure proper spacing

## Success Metrics

### Primary Metrics

1. **Functionality**: All acceptance criteria met
2. **Code Quality**: No lint warnings, follows conventions
3. **Test Coverage**: All tests pass
4. **Performance**: No performance regression

### Secondary Metrics

1. **Code Maintainability**: Clear, readable code
2. **Documentation**: Updated specifications and comments
3. **Git History**: Clean, atomic commits

## Stakeholders

- **End Users**: Better accessibility to Summarize feature
- **Developers**: Maintained code quality and testability
- **QA Team**: Clear acceptance criteria for verification

## Timeline

- **Phase 2 (Requirements)**: 2026-01-02
- **Phase 3 (Research)**: 2026-01-02
- **Phase 5 (Assessment)**: 2026-01-02
- **Phase 6 (Specification)**: 2026-01-02
- **Phase 8 (Implementation)**: 2026-01-02
- **Phase 9 (Code Review)**: 2026-01-02
- **Phase 10 (Documentation)**: 2026-01-02

## References

- Material Design 3 Guidelines: https://m3.material.io/
- Android Compose Documentation: https://developer.android.com/jetpack/compose
- Existing Article Screen: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

---

**Author**: Claude (Coordinator Agent)
**Status**: Draft - Pending Review
**Next Phase**: Research (Phase 3)
