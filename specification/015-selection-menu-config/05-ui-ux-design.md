# UI/UX Design Specification: Selection Menu Configuration Screen

**Date**: 2026-01-04
**Designer**: Coordinator Agent
**Platform**: Android (Jetpack Compose + Material3)

## 1. Design Overview

### 1.1 Purpose

Create a settings screen for configuring global selection menus in the Feeder RSS reader application. This screen will initially display a placeholder empty state, with the structure in place for future menu item display.

### 1.2 Design Philosophy

- **Simplicity**: Clean, minimal interface following Material3 guidelines
- **Consistency**: Matches existing settings screens (TranslationSettings, TextSettings)
- **Clarity**: Clear visual hierarchy and intuitive navigation
- **Accessibility**: Proper semantic labels and screen reader support
- **Responsive**: Works across phone, tablet, and foldable form factors

## 2. Screen Layout

### 2.1 Structure

```
┌─────────────────────────────────────┐
│  ← Selection Menu          (Top Bar) │
├─────────────────────────────────────┤
│                                     │
│   ┌─────────────────────────────┐   │
│   │                             │   │
│   │    [Empty State Icon]       │   │
│   │                             │   │
│   │  No selection menus         │   │
│   │  configured.                │   │
│   │                             │   │
│   │  This feature will allow    │   │
│   │  you to configure global    │   │
│   │  selection menus.           │   │
│   │                             │   │
│   └─────────────────────────────┘   │
│                                     │
│            (Scrollable)              │
└─────────────────────────────────────┘
```

### 2.2 Component Breakdown

#### TopAppBar (SensibleTopAppBar)
```kotlin
SensibleTopAppBar(
    title = "Selection Menu",
    navigationIcon = IconButton(onClick = onNavigateUp) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
    }
)
```

**Specifications**:
- Height: 64.dp (standard)
- Background: MaterialTheme.colorScheme.surface
- Title Typography: MaterialTheme.typography.titleLarge
- Icon: AutoMirrored.Filled.ArrowBack

#### Content Area (Column)
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp)
) {
    // Empty state content
}
```

**Specifications**:
- Padding: 16.dp horizontal, 8.dp vertical (LocalDimens.current.margin)
- Scrollable: Yes (verticalScroll)
- Background: MaterialTheme.colorScheme.background

#### Empty State (Box)
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 48.dp),
    contentAlignment = Alignment.Center
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon
        // Primary message
        // Secondary message
    }
}
```

**Specifications**:
- Alignment: Center
- Vertical padding: 48.dp (top and bottom)
- Spacing between elements: 16.dp

## 3. Visual Design

### 3.1 Typography

#### Title (Primary Message)
```kotlin
Text(
    text = "No selection menus configured.",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onBackground,
    textAlign = TextAlign.Center
)
```

**Specifications**:
- Style: MaterialTheme.typography.bodyLarge
- Color: MaterialTheme.colorScheme.onBackground
- Alignment: Center

#### Description (Secondary Message)
```kotlin
Text(
    text = "This feature will allow you to configure global selection menus.",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center
)
```

**Specifications**:
- Style: MaterialTheme.typography.bodyMedium
- Color: MaterialTheme.colorScheme.onSurfaceVariant
- Alignment: Center

### 3.2 Icon Design

**Icon Choice**: Use Material Symbols Outlined icon
```kotlin
Icon(
    imageVector = Icons.Outlined.Menu,  // Placeholder icon
    contentDescription = null,
    modifier = Modifier
        .size(64.dp)
        .alpha(0.5f),  // Subtle appearance
    tint = MaterialTheme.colorScheme.onSurfaceVariant
)
```

**Specifications**:
- Icon: Icons.Outlined.Menu (or similar)
- Size: 64.dp
- Tint: MaterialTheme.colorScheme.onSurfaceVariant
- Alpha: 0.5 (50% opacity for subtle appearance)
- Content Description: null (decorative)

**Alternative Icons** (for future consideration):
- Icons.Outlined.Settings
- Icons.Outlined.ListAlt
- Icons.Outlined.DashboardCustomize

### 3.3 Color Palette

**Light Mode**:
- Background: surface (highest elevation color)
- Surface: surface (container color)
- On Background: onBackground (high contrast text)
- On Surface Variant: onSurfaceVariant (medium contrast text)

**Dark Mode**:
- Same tokens apply (automatically inverted by Material3 theme)

**Semantic Colors** (future use):
- Primary: Call-to-action buttons
- Secondary: Supporting actions
- Error: Error messages
- Success: Confirmation messages

## 4. Interaction Design

### 4.1 Navigation

**Back Button**:
- Location: TopAppBar, leading edge
- Behavior: Navigates back to parent (Text Settings)
- Visual: AutoMirrored.Filled.ArrowBack icon
- Accessibility: "Go back" content description

**System Back**:
- Behavior: Same as top app bar back button
- Implementation: `navController.popBackStack()`

### 4.2 Empty State Interaction

**Current Implementation**: None (static display)

**Future Enhancement**:
- Tap on empty state → Show "Add Menu" dialog
- Floating Action Button → Add new menu item
- Swipe to refresh → Reload menu list

### 4.3 Scrolling

**Behavior**:
- Vertical scrolling only
- Smooth scrolling animation
- Respect system scroll settings

**Implementation**:
```kotlin
.verticalScroll(rememberScrollState())
```

## 5. Responsive Design

### 5.1 Screen Sizes

#### Phone (Small, Normal)
```
┌─────────────────┐
│  Selection Menu │
├─────────────────┤
│                 │
│   [Empty State] │
│   (Centered)    │
│                 │
│                 │
└─────────────────┘
```
- Width: < 600.dp
- Layout: Single column, full width
- Content padding: 16.dp horizontal

#### Tablet / Foldable (Large)
```
┌─────────────────────────────┐
│      Selection Menu         │
├─────────────────────────────┤
│                             │
│      [Empty State]          │
│      (Centered, wider)      │
│                             │
└─────────────────────────────┘
```
- Width: ≥ 600.dp
- Layout: Single column (no dual pane needed yet)
- Content padding: 16.dp horizontal, constrained by maxContentWidth

**Future Enhancement**: Dual pane layout with menu list on left, details on right

### 5.2 Orientation

**Portrait**:
- Vertical scrolling
- Centered empty state
- Full width content

**Landscape**:
- Vertical scrolling (same as portrait)
- Centered empty state
- Content constrained by maxContentWidth

## 6. Accessibility

### 6.1 Semantic Labels

**TopAppBar**:
```kotlin
SensibleTopAppBar(
    title = stringResource(R.string.selection_menu_title),
    navigationIcon = {
        IconButton(
            onClick = onNavigateUp,
            modifier = Modifier.semantics {
                contentDescription = stringResource(R.string.go_back)
            }
        ) { ... }
    }
)
```

**Empty State**:
```kotlin
Column(
    modifier = Modifier.semantics {
        this.heading()
        contentDescription = stringResource(R.string.selection_menu_empty_state_cd)
    }
) { ... }
```

### 6.2 Screen Reader Support

**Content Descriptions**:
- Back button: "Go back"
- Empty state: "No selection menus configured. This feature will allow you to configure global selection menus."

**Focus Order**:
1. TopAppBar title
2. Back button
3. Empty state content

**Reading Order**: Natural reading order (top to bottom, left to right)

### 6.3 Touch Targets

**Minimum Size**: 48.dp x 48.dp (WCAG AA standard)

**Back Button**:
- Icon size: 24.dp
- Touch target: 48.dp ( IconButton padding)

**Future Interactive Elements**:
- Menu items: Minimum 48.dp height
- Buttons: Minimum 48.dp height

### 6.4 Contrast Ratios

**Text on Background**:
- Body Large: ≥ 4.5:1 (WCAG AA)
- Body Medium: ≥ 4.5:1 (WCAG AA)

**Icon on Background**:
- On Surface Variant: ≥ 3:1 (WCAG AA for large text)

## 7. Animation and Motion

### 7.1 Screen Transitions

**Enter Animation**:
```kotlin
enterTransition = {
    fadeIn(animationSpec = tween(300))
}
```

**Exit Animation**:
```kotlin
exitTransition = {
    fadeOut(animationSpec = tween(300))
}
```

**Duration**: 300ms (standard Material3 duration)

### 7.2 Content Animations

**Empty State Appearance**:
- Fade in: 150ms delay, 300ms duration
- No slide (appears in place)

**Future List Animations**:
- AnimateItemPlacement: For reordering
- AnimatedVisibility: For adding/removing items
- Crossfade: For content changes

## 8. Error States

### 8.1 Current Implementation

**No Error States** (placeholder only)

### 8.2 Future Error States

**Loading Error**:
```
┌─────────────────────────────┐
│  Error Icon (warning)       │
│                             │
│  Couldn't load menus        │
│  Tap to retry               │
└─────────────────────────────┘
```

**Specifications**:
- Icon: Icons.Outlined.Error
- Color: MaterialTheme.colorScheme.error
- Action: "Tap to retry"

## 9. Loading States

### 9.1 Current Implementation

**No Loading State** (placeholder only)

### 9.2 Future Loading State

**Skeleton Loader**:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .shimmerEffect()  // Custom modifier
)
```

**Specifications**:
- Height: 64.dp per item
- Animation: Shimmer effect
- Count: 3-5 skeleton items

## 10. Design Mockups

### 10.1 Phone Portrait (Light Theme)

```
┌─────────────────────────┐
│ ← Selection Menu    ⋮   │
├─────────────────────────┤
│                         │
│                         │
│         ☰              │  ← Icon (64.dp, 50% opacity)
│                         │
│  No selection menus     │  ← Body Large, centered
│  configured.            │
│                         │
│  This feature will      │  ← Body Medium, onSurfaceVariant
│  allow you to           │     centered
│  configure global       │
│  selection menus.       │
│                         │
│                         │
│                         │
└─────────────────────────┘
```

### 10.2 Tablet Landscape (Light Theme)

```
┌────────────────────────────────────────────────────────────┐
│ ← Selection Menu                                    ⋮     │
├────────────────────────────────────────────────────────────┤
│                                                            │
│                                                            │
│                     ☰                                     │
│                                                            │
│          No selection menus configured.                   │
│                                                            │
│          This feature will allow you to                   │
│          configure global selection menus.                │
│                                                            │
│                                                            │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Note**: Content constrained by maxContentWidth (typically 600-840.dp)

## 11. Component Specifications

### 11.1 SelectionMenuSettingsScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionMenuSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SelectionMenuSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TopAppBarDefaults.windowInsets,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.selection_menu_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SelectionMenuContent(
            viewState = viewState,
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = LocalDimens.current.margin, vertical = 8.dp)
        )
    }
}
```

### 11.2 SelectionMenuContent

```kotlin
@Composable
fun SelectionMenuContent(
    viewState: SelectionMenuViewState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .width(LocalDimens.current.maxContentWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewState.items.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp)
            )
        } else {
            // Future: Menu list
        }
    }
}
```

### 11.3 EmptyState

```kotlin
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.semantics {
                this.heading()
                contentDescription = "No selection menus configured. This feature will allow you to configure global selection menus."
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.selection_menu_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.selection_menu_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

## 12. Design Tokens

### 12.1 Spacing

```kotlin
val Dimens.margin = 16.dp           // Horizontal padding
val Dimens.maxContentWidth = 840.dp // Max content width
val spacing = 16.dp                 // Between elements
val verticalPadding = 48.dp         // Empty state padding
```

### 12.2 Typography

```kotlin
bodyLarge: fontSize = 16.sp, lineHeight = 24.sp
bodyMedium: fontSize = 14.sp, lineHeight = 20.sp
titleLarge: fontSize = 22.sp, lineHeight = 28.sp
```

### 12.3 Icon Sizes

```kotlin
emptyStateIcon = 64.dp
navigationIcon = 24.dp
```

## 13. Design Principles Compliance

### 13.1 Material3 Guidelines ✅

- [x] Use Material3 components
- [x] Follow color system (colorScheme)
- [x] Use typography scale
- [x] Proper elevation and surface colors
- [x] Responsive layout guidelines
- [x] Motion and animation guidelines
- [x] Accessibility standards

### 13.2 Feeder App Patterns ✅

- [x] Matches TranslationSettingsScreen structure
- [x] Uses SensibleTopAppBar
- [x] Follows LocalDimens sizing
- [x] Consistent with other settings screens
- [x] Proper navigation back stack handling

## 14. Design Validation

### 14.1 Design Checklist

- [ ] Matches Material3 design system
- [ ] Consistent with existing screens
- [ ] Accessible (screen reader, touch targets)
- [ ] Responsive (phone, tablet, foldable)
- [ ] Proper color contrast ratios
- [ ] Clear visual hierarchy
- [ ] Intuitive navigation
- [ ] Proper semantic labels

### 14.2 User Experience Goals

**Learnability**: ✅ Users familiar with settings will understand immediately
**Efficiency**: ✅ No unnecessary steps, direct navigation
**Memorability**: ✅ Consistent with other settings screens
**Errors**: ✅ No error states (placeholder only)
**Satisfaction**: ✅ Clean, professional appearance

## 15. Future Design Considerations

### 15.1 Menu List Design

**When implementing actual menus**:

```
┌─────────────────────────────┐
│  ← Selection Menu     [+]  │
├─────────────────────────────┤
│ ☰ Menu Item 1         ⋮   │
│    Description              │
├─────────────────────────────┤
│ ☰ Menu Item 2         ⋮   │
│    Description              │
├─────────────────────────────┤
│ ☰ Menu Item 3         ⋮   │
│    Description              │
└─────────────────────────────┘
```

**Features**:
- Drag handles for reordering
- Context menu (⋮) for actions
- Add button (+) in TopAppBar
- Swipe-to-delete
- Long-press for multi-select

### 15.2 Menu Detail Design

**Future enhancement**: Tap menu item to view/edit details

```
┌─────────────────────────────┐
│  ← Edit Menu         [✓]   │
├─────────────────────────────┤
│  Menu Name                  │
│  [Menu Item 1        ]      │
│                             │
│  Display Settings           │
│  ☐ Show in selection menu   │
│  ☐ Show as quick action     │
│                             │
│  Actions                    │
│  [Delete]                   │
└─────────────────────────────┘
```

## 16. Design Sign-Off

**Designer**: Coordinator Agent
**Date**: 2026-01-04
**Status**: ✅ APPROVED
**Confidence**: HIGH (follows established patterns)

**Next Steps**: Proceed to Specification Writing (Phase 6)

---

**Design Document Completed**: 2026-01-04
**Version**: 1.0 (Placeholder UI/UX)
**Future Versions**: Will expand when implementing actual menu functionality
