# Architecture Design: Button Reorganization

**Document Version**: 1.0
**Last Updated**: 2026-01-02
**Status**: Draft
**Architect**: Claude (Architecture Agent)

## Overview

This document outlines the architecture design for reorganizing buttons in the Article Screen. This is a **simple UI refactoring** that does not require complex architectural changes, as it only involves relocating existing components within the same composable.

## Architecture Complexity Assessment

**Complexity Level**: **MINIMAL**

**Justification**:
- No new components
- No new state management
- No new interfaces or contracts
- No architectural boundaries crossed
- Purely structural UI change

**Decision**: This change does **not** require a full ADR (Architecture Decision Record) as it's a straightforward UI refactoring within existing architecture.

## Current Architecture

### Component Hierarchy

```
ArticleScreen (Public API)
├── ArticleViewModel (State Management)
├── ArticleScreenViewState (Data Model)
└── ArticleScreen (UI Implementation)
    ├── Scaffold (Layout)
    │   ├── TopAppBar (Header)
    │   │   ├── NavigationIcon (Back)
    │   │   └── Actions (Buttons)
    │   │       ├── Fetch Full Article Button
    │   │       ├── Open in Web View Button
    │   │       └── More Menu Button
    │   │           └── DropdownMenu
    │   │               ├── Share Item
    │   │               ├── Summarize Item
    │   │               ├── Mark Unread Item
    │   │               ├── Bookmark Item
    │   │               └── TTS Item
    │   ├── Content (Article Body)
    │   └── BottomBar (TTS Controls)
    └── Content (Article Content)
```

### Data Flow

```
User Action
    ↓
IconButton/MenuItem
    ↓
Handler (lambda)
    ↓
ViewModel Method
    ↓
State Update
    ↓
Recomposition
    ↓
UI Update
```

### State Management

**ViewModel**: `ArticleViewModel`
**State**: `ArticleScreenViewState`
**Flow**: StateFlow → collectAsStateWithLifecycle → Composable

## Proposed Architecture

### Changes Required

**Scope**: Single file, single composable

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Component**: `ArticleScreen(actions = {...})`

### New Component Hierarchy

```
ArticleScreen (UI Implementation)
├── Scaffold
│   ├── TopAppBar
│   │   ├── NavigationIcon (Back)
│   │   └── Actions (Buttons)
│   │       ├── Summarize Button [NEW LOCATION]
│   │       ├── Fetch Full Article Button
│   │       └── More Menu Button
│   │           └── DropdownMenu
│   │               ├── Share Item
│   │               ├── Open in Web View Item [NEW LOCATION]
│   │               ├── Mark Unread Item
│   │               ├── Bookmark Item
│   │               └── TTS Item
```

### Structural Changes

**Before**:
```kotlin
actions = {
    // Fetch Full Article (position 1)
    PlainTooltipBox(...) {
        IconButton(onClick = onToggleFullText) {...}
    }

    // Open in Web View (position 2)
    PlainTooltipBox(...) {
        IconButton(onClick = onOpenInCustomTab) {...}
    }

    // Menu (position 3)
    Box {...}
}
```

**After**:
```kotlin
actions = {
    // Summarize (position 1) [MOVED HERE]
    if (viewState.showSummarize) {
        PlainTooltipBox(...) {
            IconButton(onClick = onSummarize) {...}
        }
    }

    // Fetch Full Article (position 2)
    PlainTooltipBox(...) {
        IconButton(onClick = onToggleFullText) {...}
    }

    // Menu (position 3)
    Box {...}
}
```

**Menu Changes**:

**Before**:
```kotlin
DropdownMenuItem(...) // Share
if (viewState.showSummarize) {
    DropdownMenuItem(...) // Summarize
}
DropdownMenuItem(...) // Mark as Unread
DropdownMenuItem(...) // Bookmark
DropdownMenuItem(...) // TTS
```

**After**:
```kotlin
DropdownMenuItem(...) // Share
DropdownMenuItem(...) // Open in Web View [MOVED HERE]
DropdownMenuItem(...) // Mark as Unread
DropdownMenuItem(...) // Bookmark
DropdownMenuItem(...) // TTS
```

## Interface Design

### No New Interfaces Required

**Reasoning**: All handlers already exist and are passed as parameters

**Existing Handlers**:
```kotlin
onSummarize: () -> Unit
onOpenInCustomTab: () -> Unit
onToggleFullText: () -> Unit
onShowToolbarMenu: (Boolean) -> Unit
```

**No Changes Needed** ✅

## Data Flow Design

### Unchanged Data Flow

```
User taps Summarize button
    ↓
IconButton onClick
    ↓
onSummarize() lambda
    ↓
viewModel.summarize()
    ↓
[Existing flow continues...]
```

**Key Point**: Data flow is identical, only button location changes

## State Management Design

### No State Changes Required

**Existing State**:
- `showSummarize: Boolean` - Controls visibility
- `showToolbarMenu: Boolean` - Controls menu expansion

**No New State** ✅

**No State Changes** ✅

## Component Design

### Component Reuse

**Strategy**: Reuse all existing components

**Components Used**:
- `PlainTooltipBox` - Already in use
- `IconButton` - Already in use
- `Icon` - Already in use
- `DropdownMenuItem` - Already in use

**No New Components** ✅

### Component Patterns

**Pattern 1: Top Bar Button**
```kotlin
PlainTooltipBox(
    tooltip = { Text(stringResource(R.string.summarize)) }
) {
    IconButton(
        onClick = onSummarize,
    ) {
        Icon(
            Icons.Default.AutoFixHigh,
            contentDescription = stringResource(R.string.summarize),
        )
    }
}
```

**Pattern 2: Menu Item**
```kotlin
DropdownMenuItem(
    onClick = {
        onShowToolbarMenu(false)
        onOpenInCustomTab()
    },
    leadingIcon = {
        Icon(
            Icons.Default.OpenInBrowser,
            contentDescription = null,
        )
    },
    text = {
        Text(stringResource(R.string.open_in_web_view))
    },
)
```

## Modularity Design

### Separation of Concerns

**Current Structure**: ✅ Already well-separated

- UI: ArticleScreen composable
- Logic: ArticleViewModel
- Data: ArticleScreenViewState
- Navigation: Passed as handlers

**No Changes Needed** ✅

### Component Boundaries

**Maintained Boundaries**:
- ArticleScreen doesn't call ViewModel directly
- All interactions through handler parameters
- State flows through ViewState
- No tight coupling introduced

## Integration Design

### Integration Points

**No New Integrations** ✅

**Existing Integrations**:
- ViewModel → ArticleScreen (unchanged)
- Navigation → ArticleScreen (unchanged)
- DI → ActivityLauncher (unchanged)

### Dependency Management

**No New Dependencies** ✅

**Existing Dependencies**:
- Compose Material 3
- Material Icons
- AndroidX Core

## Performance Considerations

### Recomposition Analysis

**Before**:
- 3 IconButton components in top bar
- 5 DropdownMenuItem components in menu

**After**:
- 2-3 IconButton components in top bar (conditional)
- 5 DropdownMenuItem components in menu

**Recomposition Impact**: **NONE** ✅

**Reasoning**:
- Same number of components
- Same state dependencies
- Only structural reordering

### Memory Impact

**Assessment**: **NO IMPACT** ✅

- No new allocations
- No new objects
- No new state holders

## Scalability Considerations

### Future-Proofing

**Design Supports**:
- Adding more top bar buttons (if needed)
- Adding more menu items (if needed)
- Conditional visibility patterns
- Internationalization

**No Technical Debt Introduced** ✅

## Security Considerations

### No Security Impact

**Assessment**: **NO SECURITY CHANGES** ✅

- No new permissions
- No new data access
- No new network calls
- No user data handling changes

## Testing Architecture

### Test Strategy

**Unit Tests** (if not present):
- Verify conditional button visibility
- Test handler invocations

**UI Tests** (new):
- Verify button placement
- Test button interactions
- Verify menu contents

**Integration Tests** (existing):
- No changes needed

### Test Coverage

**Current Coverage**: Maintain existing coverage
**Target Coverage**: No reduction in coverage

## Implementation Strategy

### Phased Approach

**Phase 1: Top Bar Changes**
- Add Summarize button to top bar
- Remove Open in Web View from top bar

**Phase 2: Menu Changes**
- Remove Summarize from menu
- Add Open in Web View to menu

**Testing**: After each phase

### Implementation Order

1. ✅ Modify top bar actions (add/remove buttons)
2. ✅ Modify dropdown menu (add/remove items)
3. ✅ Verify handlers are correct
4. ✅ Test all functionality

## Error Handling

### No New Error Scenarios

**Existing Error Handling**: **UNCHANGED** ✅

- All errors handled by existing handlers
- No new error paths
- No new exceptions

## Accessibility Design

### Accessibility Features

**Maintained Features**:
- Content descriptions ✅
- Tooltips ✅
- Screen reader support ✅
- Focus management ✅
- Keyboard navigation ✅

**New Benefits**:
- Summarize button more discoverable ✅
- Improved focus order ✅

## Architecture Diagrams

### Before (Current State)

```
┌─────────────────────────────────────────────┐
│  [Back]  Feed Title              [📥] [🌐] [⋮]  │
└─────────────────────────────────────────────┘
                                    ↓ (tap)
                    ┌────────────────────────┐
                    │  Share                 │
                    │  Summarize      ✨     │
                    │  Mark as Unread        │
                    │  Bookmark              │
                    │  Text to Speech        │
                    └────────────────────────┘
```

### After (Proposed State)

```
┌─────────────────────────────────────────────┐
│  [Back]  Feed Title        [✨] [📥]     [⋮]  │
└─────────────────────────────────────────────┘
                                    ↓ (tap)
                    ┌────────────────────────┐
                    │  Share                 │
                    │  Open in Web View   🌐 │
                    │  Mark as Unread        │
                    │  Bookmark              │
                    │  Text to Speech        │
                    └────────────────────────┘
```

## Architecture Decision Summary

### Decision: Minimal Architecture Change

**Option Chosen**: Simple UI refactoring within existing architecture

**Alternatives Considered**:

1. **Create new component** - ❌ Rejected (unnecessary complexity)
2. **Extract button layout** - ❌ Rejected (over-engineering)
3. **Add state management** - ❌ Rejected (no need)
4. **Simple reorganization** - ✅ **SELECTED** (appropriate scope)

**Rationale**:
- Change is purely visual
- No logic changes required
- Follows existing patterns
- Minimal risk
- Maintains simplicity

## Technical Debt Implications

**No Technical Debt Created** ✅

**Maintains Code Quality**:
- Follows existing patterns
- No code duplication
- Proper separation of concerns
- Clear and maintainable

## Migration Strategy

**No Migration Needed** ✅

**Reasoning**:
- No data changes
- No schema changes
- No API changes
- No configuration changes

## Conclusion

### Architecture Readiness

**Status**: ✅ **READY FOR IMPLEMENTATION**

**Key Points**:
1. Minimal architecture change
2. No new components or state
3. Follows existing patterns
4. Maintains all quality standards
5. No technical debt

### Implementation Confidence

**Level**: **VERY HIGH** ⭐⭐⭐⭐⭐

**Reasons**:
1. Straightforward structural change
2. Clear component boundaries
3. No architectural risks
4. Comprehensive examples exist
5. Low complexity

### Next Steps

1. ✅ Create UI/UX design specification
2. ✅ Write technical specification
3. ✅ Create implementation plan
4. ✅ Execute implementation

---

**Architecture Design Completed**: 2026-01-02
**Architect**: Claude (Architecture Agent)
**Status**: Approved - Ready for UI/UX Design Phase
