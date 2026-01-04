# UI/UX Design Specification: Selection Menu Configuration Feature

**Specification ID**: 015
**Feature Name**: Selection Menu Configuration
**Design Date**: 2026-01-04
**Designer**: Super Dev UI/UX Designer
**Status**: Draft

## Table of Contents

1. [User Analysis](#user-analysis)
2. [Design Overview](#design-overview)
3. [Screen Layouts](#screen-layouts)
4. [Component Specifications](#component-specifications)
5. [Interaction Design](#interaction-design)
6. [Visual Design](#visual-design)
7. [Accessibility](#accessibility)
8. [Responsive Design](#responsive-design)
9. [States & Feedback](#states--feedback)
10. [Animations & Transitions](#animations--transitions)
11. [Design Evaluation](#design-evaluation)

---

## 1. User Analysis

### 1.1 User Personas

**Persona 1: Power Reader**
- Name: "Alex"
- Uses app: Daily, 2+ hours
- Goals: Optimize reading workflow, quick access to favorite tools
- Pain points: Too many menu items, cluttered interface
- Tech proficiency: High

**Persona 2: Casual Reader**
- Name: "Jordan"
- Uses app: Weekly, 30 min sessions
- Goals: Simple interface, essential features only
- Pain points: Confused by too many options
- Tech proficiency: Medium

**Persona 3: Accessibility User**
- Name: "Sam"
- Uses app: Daily
- Goals: Accessible text processing, screen reader compatible
- Pain points: Custom menus not accessible
- Tech proficiency: High with assistive tech

### 1.2 User Journey

**Current Journey (Problem)**:
1. User selects text in article
2. Toolbar appears with ALL items (cluttered)
3. User must search for desired action
4. User frustrated by unused items

**New Journey (Solution)**:
1. User opens Settings → Text → Selection Menu
2. User sees organized list of items
3. User disables unused items, reorders favorites
4. User saves changes
5. User selects text in article
6. Toolbar shows only enabled items in preferred order
7. User delighted by streamlined experience

### 1.3 User Goals

**Primary Goals**:
1. Reduce clutter in text selection menu
2. Prioritize frequently-used actions
3. Hide never-used third-party apps
4. Customize order to match workflow

**Secondary Goals**:
1. Quickly reset if mistakes made
2. Understand what each item does
3. Add new third-party apps easily

---

## 2. Design Overview

### 2.1 Design Principles

1. **Simplicity First**: Clear, uncluttered interface
2. **Immediate Feedback**: Visual response to every action
3. **Progressive Disclosure**: Show info when needed
4. **Accessibility**: Inclusive design for all users
5. **Consistency**: Match existing Feeder settings patterns

### 2.2 Design System

**Based on**: Material Design 3 (already used in Feeder)

**Key Components**:
- `Scaffold`: Layout structure
- `SensibleTopAppBar`: Navigation header
- `LazyColumn`: Scrollable item list
- `Switch`: Toggle controls
- `ListItem`: Item rows
- Material 3 colors, typography, spacing

### 2.3 Information Architecture

```
Selection Menu Settings Screen
├── Top Bar
│   ├── Back button
│   └── Title: "Selection Menu"
├── Content (Scrollable)
│   ├── System Items Section
│   │   ├── Section Header: "System Actions"
│   │   ├── Copy (switch + drag handle)
│   │   ├── Paste (switch + drag handle)
│   │   ├── Cut (switch + drag handle)
│   │   └── Select All (switch + drag handle)
│   ├── Third-Party Items Section
│   │   ├── Section Header: "Third-Party Apps" (with count)
│   │   ├── Anki (switch + drag handle)
│   │   ├── Perplexity (switch + drag handle)
│   │   └── ...
│   └── Actions Section
│       ├── "Reset to Defaults" button
│       └── Help text
└── Bottom Bar (optional)
    └── "Save" / "Apply" button (auto-save preferred)
```

---

## 3. Screen Layouts

### 3.1 Primary Screen Layout

```
┌─────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                              ⋮      │  <- TopAppBar
├─────────────────────────────────────────────────────────┤
│                                                         │
│  System Actions                                        │  <- Section Header
│  ─────────────────────────────────────────────────────  │
│                                                         │
│  ⋮⋮  Copy                                   [●] ON    │  <- List Item
│     Quickly copy selected text to clipboard            │
│                                                         │
│  ⋮⋮  Paste                                  [○] OFF    │  <- List Item
│     Paste text from clipboard                           │
│                                                         │
│  ⋮⋮  Cut                                    [●] ON    │
│     Cut selection and copy to clipboard                │
│                                                         │
│  ⋮⋮  Select All                             [●] ON    │
│     Select all text in current view                    │
│                                                         │
│  Third-Party Apps (3)                                   │  <- Section Header
│  ─────────────────────────────────────────────────────  │
│                                                         │
│  ⋮⋮  Anki                                   [●] ON    │
│     Create Anki flashcard from selection                │
│                                                         │
│  ⋮⋮  Perplexity AI                           [●] ON    │
│     Search with Perplexity AI                           │
│                                                         │
│  ⋮⋮  DeepL Translator                       [○] OFF   │
│     Translate text with DeepL                           │
│                                                         │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Reset to Defaults                                 │ │  <- Button
│  └───────────────────────────────────────────────────┘ │
│                                                         │
│  Changes are saved automatically                       │  <- Helper text
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Drag-and-Drop Interaction State

```
┌─────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                              ⋮      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  System Actions                                        │
│  ─────────────────────────────────────────────────────  │
│                                                         │
│  ⋮⋮  Copy                                   [●] ON    │  <- Static
│     Quickly copy selected text to clipboard            │
│                                                         │
│  ┌─────────────────────────────────────────────────────┐│
│  │ ⋮⋮  Paste                                 [○] OFF  ││  <- Dragging
│  │     Paste text from clipboard                       ││  (elevated)
│  └─────────────────────────────────────────────────────┘│
│     ↑ Drag up to reorder                               │
│                                                         │
│  ⋮⋮  Cut                                    [●] ON    │  <- Offset down
│     Cut selection and copy to clipboard                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.3 Empty State

```
┌─────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                              ⋮      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                                                         │
│                  ⚠                                     │
│            No menu items found                          │
│                                                         │
│  All text processing apps are disabled.                │
│  Enable at least one item to use text selection.       │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Reset to Defaults                                 │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.4 Loading State

```
┌─────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                              ⋮      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                                                         │
│                    ○ ○ ○                                │
│                  Loading...                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.5 Error State

```
┌─────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                              ⋮      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                                                         │
│                  ⚠                                     │
│            Couldn't load menu items                     │
│                                                         │
│  Please check your connection and try again.            │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Retry                                            │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.6 Tablet / Foldable Layout

```
┌─────────────────────────────────────────────────────────────┐
│ ◀   Selection Menu                                       ⋮│
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  System Actions              │  Third-Party Apps (3)        │
│  ───────────────────────────  │  ─────────────────────────  │
│                             │                              │
│  ⋮⋮  Copy            [●] ON │  ⋮⋮  Anki              [●] ON│
│  ⋮⋮  Paste           [○] OFF│  ⋮⋮  Perplexity AI      [●] ON│
│  ⋮⋮  Cut             [●] ON │  ⋮⋮  DeepL Translator   [○] OFF│
│  ⋮⋮  Select All      [●] ON │                              │
│                             │  ┌────────────────────────┐   │
│                             │  │  Reset to Defaults     │   │
│                             │  └────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Component Specifications

### 4.1 ListItemRow

**Purpose**: Display a single menu item with controls

**Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│ [⋮] [Icon + Name]                    [Description] [Switch] │
│      [Subtitle]                                                │
└─────────────────────────────────────────────────────────────┘
```

**Elements**:
- **Drag Handle** (⋮): Left side, 24dp x 24dp, touch target 48dp
- **Icon**: 24dp x 24dp, optional (for third-party apps)
- **Name**: Primary text, title_medium style
- **Subtitle**: Secondary text, body_small style (optional)
- **Switch**: Right side, Material 3 Switch

**Spacing**:
- Horizontal padding: 16dp
- Vertical padding: 8dp top/bottom
- Between drag handle and icon: 8dp
- Between icon and text: 12dp
- Between text and switch: 8dp

**States**:
- Normal: Full opacity
- Disabled: 50% opacity (for uninstalled apps)
- Dragging: Elevated (8dp shadow), 80% opacity

### 4.2 SectionHeader

**Purpose**: Group and label item categories

**Layout**:
```
System Actions                                    (3 items)
─────────────────────────────────────────────────────────────
```

**Elements**:
- **Title**: "System Actions" or "Third-Party Apps", label_medium style
- **Badge**: Item count for third-party (e.g., "(3)")
- **Divider**: Full-width line below title

**Spacing**:
- Top margin: 24dp (from previous section)
- Bottom margin: 8dp (to first item)
- Horizontal padding: 16dp

**Styling**:
- Title color: Primary (Material 3)
- Divider color: OutlineVariant (Material 3)

### 4.3 DragHandle

**Purpose**: Initiate drag-and-drop

**Visual**:
- Icon: `Icons.Default.DragHandle` or custom ⋮⋮
- Size: 24dp x 24dp
- Touch target: 48dp x 48dp (expanded with padding)

**Interaction**:
- Long press: Initiate drag (200ms delay)
- Visual feedback: Icon grows to 32dp during drag

**Accessibility**:
- Content description: "Drag to reorder"
- Action: "Double tap and hold to drag"

### 4.4 Switch (Toggle)

**Purpose**: Enable/disable menu items

**Visual**:
- Material 3 Switch component
- Width: 52dp
- Height: 32dp

**States**:
- ON (checked): Green thumb, filled track
- OFF (unchecked): Grey thumb, outline track

**Validation**:
- At least one system item must remain ON
- Prevent disabling last enabled item

**Feedback**:
- Haptic feedback on toggle (light tap)
- Toast if trying to disable last item

### 4.5 ResetButton

**Purpose**: Restore default configuration

**Visual**:
- TextButton style (outlined)
- Text: "Reset to Defaults"
- Width: As wide as content
- Height: 40dp (minimum touch target)

**Behavior**:
- Click: Show confirmation dialog
- Confirm: Restore defaults, persist
- Cancel: Dismiss dialog

### 4.6 ConfirmationDialog

**Purpose**: Confirm reset action

**Layout**:
```
┌─────────────────────────────────────────────┐
│  Reset to Defaults               ✕         │
├─────────────────────────────────────────────┤
│                                             │
│  Are you sure you want to reset to        │
│  default settings? This will:             │
│                                             │
│  • Enable all system actions               │
│  • Enable all third-party apps             │
│  • Restore default order                  │
│                                             │
│  This action cannot be undone.             │
│                                             │
│  ┌─────────┐  ┌─────────┐                   │
│  │ Cancel  │  │ Reset   │                   │
│  └─────────┘  └─────────┘                   │
└─────────────────────────────────────────────┘
```

---

## 5. Interaction Design

### 5.1 Drag-and-Drop Interaction

**Flow**:
1. **Initiation**: Long press on drag handle (200ms)
2. **Visual Feedback**: Item elevates, others make space
3. **Dragging**: Item follows finger, other items shift
4. **Drop**: Release to complete reorder
5. **Commit**: New order persists immediately

**Visual Feedback**:
- Dragging item: Elevation 8dp, scale 1.05
- Other items: Smooth animated displacement
- Insert indicator: Horizontal line at drop position

**Cancellation**:
- Drag outside list: Cancel
- Timeout (10s): Cancel, return to original position

**Accessibility**:
- Alternative: "Move Up" / "Move Down" buttons
- Screen reader: "Item 2 of 5, double tap and hold to drag"

### 5.2 Toggle Interaction

**Flow**:
1. **Tap**: Toggle switch
2. **Validation**: Check if at least one item enabled
3. **Success**: Persist immediately, show brief checkmark
4. **Error**: Vibrate, show toast if invalid

**Feedback**:
- Visual: Switch animates to new position
- Haptic: Light tap on successful toggle
- Error: Vibrate + toast if validation fails

### 5.3 Reset Interaction

**Flow**:
1. **Tap**: "Reset to Defaults" button
2. **Confirm**: Show dialog with consequences
3. **Confirm**: Restore defaults
4. **Success**: Show toast "Reset to defaults"

**Dialog Details**:
- Title: "Reset to Defaults"
- Message: List of what will change
- Buttons: "Cancel" (text), "Reset" (filled, primary color)

### 5.4 Navigation

**Entry Point**:
- Settings → Text → Selection Menu
- MenuSetting in TextSettingsScreen

**Back Navigation**:
- Back button: Save and return
- System back: Same as back button
- Swipe back (gesture navigation): Same as back button

**Auto-Save**:
- Changes persist immediately
- No "Save" button needed
- Reduces complexity

---

## 6. Visual Design

### 6.1 Color Palette

**Based on**: Material 3 dynamic colors

| Element | Color | Token |
|---------|-------|-------|
| Background | Surface | `MaterialTheme.colorScheme.surface` |
| Surface | SurfaceVariant | `MaterialTheme.colorScheme.surfaceVariant` |
| Primary Text | On Surface | `MaterialTheme.colorScheme.onSurface` |
| Secondary Text | On Surface Variant | `MaterialTheme.colorScheme.onSurfaceVariant` |
| Section Header | Primary | `MaterialTheme.colorScheme.primary` |
| Divider | Outline Variant | `MaterialTheme.colorScheme.outlineVariant` |
| Switch ON | Primary | `MaterialTheme.colorScheme.primary` |
| Switch OFF | Outline | `MaterialTheme.colorScheme.outline` |
| Error | Error | `MaterialTheme.colorScheme.error` |

### 6.2 Typography

**Based on**: Material 3 type scale

| Element | Style | Size | Weight |
|---------|-------|------|--------|
| Screen Title | displaySmall | 36sp | Regular |
| Section Header | labelMedium | 12sp | Medium |
| Item Name | titleMedium | 16sp | Regular |
| Item Subtitle | bodySmall | 12sp | Regular |
| Button Text | labelLarge | 14sp | Medium |
| Helper Text | bodySmall | 12sp | Regular |

### 6.3 Spacing

**Based on**: Material 3 spacing tokens

| Element | Size | Usage |
|---------|------|-------|
| XS | 4dp | Icon padding, tight spacing |
| SM | 8dp | Internal padding, gaps |
| MD | 16dp | Standard padding, margins |
| LG | 24dp | Section spacing |
| XL | 32dp | Large gaps |

### 6.4 Icons

**Based on**: Material Symbols

| Icon | Usage | Size |
|------|-------|------|
| Drag Handle | Drag indicator | 24dp |
| Back | Navigation | 24dp |
| More | Options menu | 24dp |
| Check | Success state | 24dp |
| Warning | Error state | 24dp |
| Info | Help tooltip | 24dp |

### 6.5 Elevation

**Based on**: Material 3 elevation system

| State | Level | Shadow |
|-------|-------|--------|
| Resting | 0 | None |
| Dragging | 2 | 8dp shadow |
| Dialog | 5 | 16dp shadow |
| Switch | 1 | 4dp shadow |

---

## 7. Accessibility

### 7.1 Screen Reader Support

**Accessibility Labels**:
```
- Screen title: "Selection Menu Settings"
- Section header: "System Actions, 4 items"
- List item: "Copy, Enabled, Double tap to toggle, Drag handle to reorder"
- Switch: "Copy, Enabled, Double tap to toggle"
- Drag handle: "Drag to reorder, Double tap and hold"
- Reset button: "Reset to defaults, Double tap to reset"
```

**Accessibility Actions**:
```kotlin
Modifier.semantics {
    heading() // For section headers
    stateDescription = "Enabled" // For switches
    contentDescription = "Copy, Enabled" // For list items
    traversalIndex = 0 // Custom order
}
```

### 7.2 Touch Targets

**Minimum Size**: 48dp x 48dp (WCAG AAA)

**Components**:
- List item row: Full width, min height 48dp
- Switch: 52dp x 32dp (in 48dp touch target)
- Drag handle: 24dp icon in 48dp touch target
- Reset button: Min height 40dp, padded to 48dp

### 7.3 Keyboard Navigation

**Focus Order**:
1. Screen title (not focusable)
2. System Actions section header (not focusable)
3. Copy item (switch is focusable)
4. Paste item (switch is focusable)
5. Cut item (switch is focusable)
6. Select All item (switch is focusable)
7. Third-Party Apps section header (not focusable)
8. [Third-party items...]
9. Reset button

**Keyboard Actions**:
- Tab/Shift+Tab: Navigate between items
- Space: Toggle switch
- Enter: Toggle switch (same as Space)
- Arrow keys: Alternative for drag-and-drop

### 7.4 Color Contrast

**Requirements**: WCAG AA (4.5:1 for text, 3:1 for UI)

**Combinations**:
- Primary text on surface: 14.2:1 (AAA)
- Secondary text on surface: 7.5:1 (AAA)
- Primary on background: 4.6:1 (AA)
- Primary on primary: Auto-adjusted

### 7.5 Font Scaling

**Support**: 100% - 200% (Android settings)

**Implementation**:
- Use `LocalTypographySettings` (already in Feeder)
- Use `ProvideScaledText` composable
- Test at 100%, 150%, 200%

### 7.6 Animation Reduction

**Respect**: "Reduce Animation" system setting

**Implementation**:
```kotlin
val animationSpec = if (LocalTextStyle.current.isReducedMotion) {
    spring(stiffness = Spring.StiffnessVeryLow)
} else {
    spring(dampingRatio = 0.8f, stiffness = 400f)
}
```

---

## 8. Responsive Design

### 8.1 Breakpoints

**Based on**: Material 3 window size classes

| Class | Width | Layout |
|-------|-------|--------|
| Compact | <600dp | Single column (mobile) |
| Medium | 600dp - 840dp | Single column, wider margins (tablet) |
| Expanded | >840dp | Dual column (foldable/tablet) |

### 8.2 Mobile Layout (Compact)

```
Single column
───────────────────────────────
┌─────────────────────────────┐
│  System Actions             │
│  ─────────────────────────  │
│  ⋮⋮  Copy             [●]   │
│  ⋮⋮  Paste            [○]   │
│  ⋮⋮  Cut              [●]   │
│  ⋮⋮  Select All       [●]   │
│                             │
│  Third-Party Apps           │
│  ─────────────────────────  │
│  ⋮⋮  Anki             [●]   │
│  ⋮⋮  Perplexity       [●]   │
└─────────────────────────────┘
```

**Specifications**:
- Width: 100%
- Margins: 16dp horizontal
- Max width: None (full width)

### 8.3 Tablet Layout (Medium)

```
Single column, wider margins
─────────────────────────────────────
    ┌───────────────────────────────┐
    │  System Actions              │
    │  ───────────────────────────  │
    │  ⋮⋮  Copy               [●]  │
    │  ⋮⋮  Paste              [○]  │
    │  ⋮⋮  Cut                [●]  │
    │  ⋮⋮  Select All         [●]  │
    │                             │
    │  Third-Party Apps           │
    │  ───────────────────────────  │
    │  ⋮⋮  Anki               [●]  │
    │  ⋮⋮  Perplexity         [●]  │
    └───────────────────────────────┘
```

**Specifications**:
- Width: 600dp max
- Margins: Auto (centered)
- Padding: 24dp horizontal

### 8.4 Foldable/Desktop Layout (Expanded)

```
Dual column
───────────────────────────────────────────────────────────────
    ┌─────────────────────────────┐  ┌─────────────────────────┐
    │  System Actions             │  │  Third-Party Apps (3)   │
    │  ─────────────────────────  │  │  ─────────────────────  │
    │  ⋮⋮  Copy               [●]│  │  ⋮⋮  Anki             [●]│
    │  ⋮⋮  Paste              [○]│  │  ⋮⋮  Perplexity       [●]│
    │  ⋮⋮  Cut                [●]│  │  ⋮⋮  DeepL            [○]│
    │  ⋮⋮  Select All         [●]│  │                          │
    │                             │  │  ┌──────────────────┐    │
    └─────────────────────────────┘  │  │  Reset to Defaults│    │
                                        │  └──────────────────┘    │
                                        └─────────────────────────┘
```

**Specifications**:
- Width: 50% each column
- Gap: 24dp between columns
- Max content width: 400dp per column

---

## 9. States & Feedback

### 9.1 Loading State

**Trigger**: ViewModel loading initial config

**Visual**:
- Circular progress indicator centered
- Text: "Loading..."
- Skeleton screens: Optional (for faster perceived performance)

**Duration**: <500ms expected

**Implementation**:
```kotlin
if (viewState.isLoading) {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center),
    )
}
```

### 9.2 Success State

**Trigger**: Settings saved successfully

**Visual**:
- Brief checkmark animation (1s)
- Auto-dismiss (no user action)
- Toast: "Changes saved"

**Implementation**:
```kotlin
LaunchedEffect(viewState.hasChanges) {
    if (viewState.hasChanges) {
        delay(1000)
        // Show checkmark or snackbar
    }
}
```

### 9.3 Error State

**Trigger**: Config load/save fails

**Visual**:
- Warning icon
- Error message
- Retry button

**Messages**:
- Load failure: "Couldn't load menu items"
- Save failure: "Couldn't save changes"
- Validation error: "At least one item must be enabled"

**Implementation**:
```kotlin
if (viewState.error != null) {
    ErrorView(
        message = viewState.error,
        onRetry = { viewModel.refresh() },
    )
}
```

### 9.4 Validation Error

**Trigger**: User tries to disable all items

**Visual**:
- Vibrate (error feedback)
- Toast: "At least one item must be enabled"
- Switch snaps back to ON

**Implementation**:
```kotlin
fun onToggleItem(itemId: String, enabled: Boolean) {
    if (!enabled && getEnabledItems().size == 1) {
        // Show error
        showToast("At least one item must be enabled")
        vibration.vibrate(VibrationEffect.EFFECT_CLICK)
        return
    }
    // Proceed with toggle
}
```

### 9.5 Empty State

**Trigger**: No items available (all disabled/uninstalled)

**Visual**:
- Warning icon
- Message: "No menu items found"
- Explanation
- Reset button

**Implementation**:
```kotlin
if (viewState.items.isEmpty()) {
    EmptyStateView(
        message = "No menu items found",
        description = "All text processing apps are disabled.",
        action = { viewModel.resetToDefaults() },
    )
}
```

---

## 10. Animations & Transitions

### 10.1 Drag-and-Drop Animations

**Item Lift**:
- Duration: 200ms
- Easing: EaseOutCubic
- Properties: Scale (1.0 → 1.05), Elevation (0 → 8dp)

**Item Drop**:
- Duration: 200ms
- Easing: EaseInOutCubic
- Properties: Scale (1.05 → 1.0), Elevation (8dp → 0)

**Other Items Shifting**:
- Duration: 300ms
- Easing: EaseInOutCubic
- Properties: Translation Y (using `animateItemPlacement()`)

**Implementation**:
```kotlin
Modifier
    .animateItemPlacement(
        spring(
            dampingRatio = 0.8f,
            stiffness = 400f,
        )
    )
```

### 10.2 Toggle Animations

**Switch Toggle**:
- Duration: 200ms
- Easing: EaseInOutCubic
- Material 3 Switch handles animation

**Success Checkmark**:
- Duration: 300ms
- Easing: EaseOutBack
- Fade in, scale down

### 10.3 Screen Transitions

**Enter Screen**:
- Duration: 300ms
- Easing: EaseOutCubic
- Direction: Fade in + slide from right

**Exit Screen**:
- Duration: 300ms
- Easing: EaseInCubic
- Direction: Fade out + slide to right

### 10.4 List Item Changes

**Item Added**:
- Duration: 400ms
- Easing: EaseOutBack
- Animation: Fade in + slide in from bottom

**Item Removed**:
- Duration: 300ms
- Easing: EaseInCubic
- Animation: Fade out + collapse height

### 10.5 Reduced Motion

**Check System Setting**:
```kotlin
val reduceMotion = LocalTextStyle.current.isReducedMotion

val animationSpec = if (reduceMotion) {
    spring(stiffness = Spring.StiffnessVeryLow)
} else {
    spring(dampingRatio = 0.8f, stiffness = 400f)
}
```

---

## 11. Design Evaluation

### 11.1 UI/UX Options Evaluation

#### Option 1: Single-Column List with Drag Handles (SELECTED)

**Description**: Traditional settings list with drag handles on left, switches on right.

**Pros**:
- Familiar pattern (users expect it)
- Clear visual hierarchy
- Easy to implement
- Works well on all screen sizes
- Accessible (drag handle + keyboard alternatives)

**Cons**:
- Drag handle adds visual clutter
- Requires long press (discoverability issue)

**Score**: 4.3/5

**Evaluation Breakdown**:

| Criterion | Weight | Score | Weighted |
|-----------|--------|-------|----------|
| **Delivery (0.30)** | | | |
| Implementation Feasibility | 0.05 | 5 | 0.25 |
| Complexity | 0.08 | 4 | 0.32 |
| Risk | 0.07 | 4 | 0.28 |
| Time-to-Value | 0.07 | 4 | 0.28 |
| Maintainability | 0.05 | 5 | 0.25 |
| Testability | 0.03 | 5 | 0.15 |
| **Technical (0.45)** | | | |
| Accessibility | 0.10 | 5 | 0.50 |
| Performance | 0.08 | 5 | 0.40 |
| Design System Alignment | 0.07 | 5 | 0.35 |
| Scalability of UI Patterns | 0.05 | 4 | 0.20 |
| Consistency | 0.05 | 5 | 0.25 |
| Observability | 0.03 | 4 | 0.12 |
| Reliability | 0.03 | 5 | 0.15 |
| Supportability | 0.02 | 5 | 0.10 |
| Reversibility | 0.02 | 4 | 0.08 |
| **Experiential (0.25)** | | | |
| Usability | 0.10 | 4 | 0.40 |
| Learnability | 0.05 | 5 | 0.25 |
| Discoverability | 0.05 | 3 | 0.15 |
| Aesthetic Fit | 0.05 | 4 | 0.20 |
| **TOTAL** | **1.00** | | **4.3** |

#### Option 2: Reorder Mode Button

**Description**: Toggle button to enter "reorder mode," then tap items to reorder.

**Pros**:
- No drag handles (cleaner UI)
- Explicit mode (clearer intent)
- Can reorder with single tap

**Cons**:
- Extra tap to enter mode
- More complex (mode management)
- Less discoverable
- Mode can confuse users

**Score**: 3.1/5

#### Option 3: Swipe-to-Reorder

**Description**: Swipe item left/right to reveal reorder controls.

**Pros**:
- Modern gesture
- No drag handles

**Cons**:
- Conflict with other swipe actions
- Harder to discover
- Less precise ordering
- Accessibility challenges

**Score**: 2.8/5

### 11.2 Comparative Summary

| Option | Score | Key Strengths | Key Weaknesses |
|--------|-------|---------------|----------------|
| 1. Drag Handles | 4.3 | Familiar, accessible, simple | Visual clutter |
| 2. Reorder Mode | 3.1 | Clean UI, explicit | Extra taps, complex |
| 3. Swipe-to-Reorder | 2.8 | Modern, no handles | Hard to discover, conflicts |

### 11.3 Final Recommendation

**Select Option 1: Single-Column List with Drag Handles**

**Rationale**:
- Highest overall score (4.3/5)
- Best accessibility (critical for this app)
- Aligns with existing Feeder settings patterns
- Lowest risk and complexity
- Excellent consistency with platform conventions

**Trade-offs**:
- Accept visual clutter of drag handles for accessibility
- Accept long-press interaction for familiarity

**Reversibility Plan**:
- **Trigger**: User testing shows confusion with drag handles
- **Rollback**: Implement Option 2 (Reorder Mode)
- **Cost**: Medium (requires refactoring list item rows)
- **Time Estimate**: 4 hours

### 11.4 Reuse Plan

**Selected Open-Source Components**:

1. **Material 3 Components** (AndroidX)
   - Components: `Switch`, `ListItem`, `Scaffold`, `TopAppBar`
   - License: Apache 2.0
   - Rationale: Already used in Feeder, consistent design
   - Integration: Direct usage via Compose Material 3 library

2. **Compose Animations** (AndroidX)
   - Components: `animateItemPlacement()`, `Crossfade`, `AnimatedContent`
   - License: Apache 2.0
   - Rationale: Smooth animations, well-tested
   - Integration: Built-in Compose APIs

3. **Custom Drag Modifier** (Based on Nutrient Blog Pattern)
   - Components: `Modifier.dragToReorder()`
   - License: MIT (from blog)
   - Rationale: No external dependencies, proven pattern
   - Integration: Copy-paste with adaptation for our data model

**Glue Code List**:

1. **SelectionMenuItemAdapter**
   - Responsibility: Map `SelectionMenuItem` to UI list item
   - Tests: Unit tests for mapping, snapshot tests for UI
   - Integration: Connects data model to Compose UI

2. **DragEventAdapter**
   - Responsibility: Bridge drag modifier with ViewModel events
   - Tests: Unit tests for event mapping
   - Integration: Emits `SelectionMenuEvent.ReorderItems`

3. **ConfigStateAdapter**
   - Responsibility: Convert `SelectionMenuConfig` to UI state
   - Tests: Unit tests for state transformation
   - Integration: Used in ViewModel to expose `SelectionMenuState`

### 11.5 Interface-First Specification

**Component Contracts**:

```kotlin
// List Item Component Contract
@Composable
fun SelectionMenuItemRow(
    item: SelectionMenuItem,
    isDragging: Boolean,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
)

// Props:
// - item: Data to display (id, name, type, enabled, order)
// - isDragging: Visual state for drag feedback
// - onToggle: Callback when switch toggled
// - modifier: External modifiers (including drag modifier)

// Events:
// - onToggle(itemId, enabled): Emitted when switch changes

// States:
// - Normal: Item is visible and interactive
// - Dragging: Item is elevated, other items shift
// - Disabled: Item is 50% opacity (for uninstalled apps)

// Stability:
// - Stable interface (won't change without major version bump)
// - Backward compatible: New props optional, existing props unchanged
```

```kotlin
// Drag Modifier Contract
fun Modifier.dragToReorder(
    item: SelectionMenuItem,
    itemList: List<SelectionMenuItem>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): Modifier

// Props:
// - item: Current item to attach drag behavior
// - itemList: Full list for index calculation
// - onMove: Callback when drag completes

// Events:
// - onStartDrag: Visual feedback begins
// - onDrag(offset): Item moves with finger
// - onEndDrag: Finalize or cancel reorder
// - onMove(from, to): Commit position change

// States:
// - Idle: Not dragging
// - Dragging: Item is being dragged

// Stability:
// - Stable interface
// - May extend with custom animation parameters
```

**Interaction Flows**:

```
User Flow: Toggle Item
1. User taps switch
2. SelectionMenuItemRow detects tap
3. onToggle(itemId, !enabled) emitted
4. ViewModel.onEvent(ToggleItem(itemId, !enabled))
5. ViewModel validates (at least one enabled)
6. If valid: persist config, update state
7. If invalid: show toast, revert UI
8. UI recomposes with new state
```

```
User Flow: Reorder Item
1. User long-presses drag handle
2. Drag modifier detects gesture
3. Visual feedback: Item elevates, scales up
4. User drags item vertically
5. Other items shift via animateItemPlacement()
6. User releases (drop)
7. onMove(fromIndex, toIndex) emitted
8. ViewModel.onEvent(ReorderItems(from, to))
9. ViewModel reorders list, persists
10. UI recomposes with new order
```

---

## 12. Implementation Notes

### 12.1 Auto-Save Behavior

**Decision**: Save changes immediately (no "Save" button)

**Rationale**:
- Reduces complexity
- Matches modern Android patterns
- Users expect immediate persistence
- No risk of data loss (app doesn't crash often)

**Implementation**:
```kotlin
fun onToggleItem(itemId: String, enabled: Boolean) {
    viewModelScope.launch {
        repository.setItemEnabled(itemId, enabled)
        // StateFlow updates automatically
    }
}
```

### 12.2 Third-Party App Icons

**Decision**: Show app icons when available

**Rationale**:
- Visual recognition
- Distinguishes similar apps
- Professional appearance

**Implementation**:
```kotlin
val icon = remember(item.packageName) {
    if (item.type == ItemType.THIRD_PARTY) {
        try {
            packageManager.getApplicationIcon(item.packageName!!)
        } catch (e: Exception) {
            // Fallback to default icon
            Icons.Default.Apps
        }
    } else {
        null // System items use text only
    }
}
```

### 12.3 Section Headers

**Decision**: Always show both sections, even if empty

**Rationale**:
- Consistency
- User can see what's available
- Empty section encourages enabling items

**Implementation**:
```kotlin
if (systemItems.isNotEmpty()) {
    SectionHeader("System Actions")
    systemItems.forEach { item ->
        SelectionMenuItemRow(item)
    }
}

if (thirdPartyItems.isNotEmpty()) {
    SectionHeader("Third-Party Apps (${thirdPartyItems.size})")
    thirdPartyItems.forEach { item ->
        SelectionMenuItemRow(item)
    }
}
```

---

## 13. Design Validation

### 13.1 Design Review Checklist

- [ ] Layout matches wireframes
- [ ] Colors follow Material 3 tokens
- [ ] Typography follows type scale
- [ ] Spacing follows spacing tokens
- [ ] Touch targets meet 48dp minimum
- [ ] Contrast ratios meet WCAG AA
- [ ] Screen reader labels defined
- [ ] Keyboard navigation supported
- [ ] Drag-and-drop works with accessibility services
- [ ] Animations respect reduced motion setting
- [ ] Error states defined
- [ ] Loading states defined
- [ ] Empty states defined
- [ ] Responsive layouts defined
- [ ] Transitions specified

### 13.2 Usability Testing Plan

**Tasks**:
1. Disable unused third-party apps
2. Reorder items to prioritize Copy
3. Reset to defaults
4. Handle validation error (try to disable all items)

**Success Criteria**:
- All tasks completed without assistance
- Task completion time <2 minutes
- Satisfaction rating 4+/5

**Test Participants**:
- 5 users: 2 power users, 2 casual users, 1 accessibility user

---

## 14. Conclusion

This UI/UX design specification provides a comprehensive, accessible, and visually consistent interface for the Selection Menu Configuration feature. The design follows Material 3 guidelines, matches existing Feeder settings patterns, and prioritizes usability through familiar interaction patterns.

**Key Design Decisions**:
1. Single-column list with drag handles (familiar, accessible)
2. Auto-save behavior (simpler UX)
3. Section grouping (clear organization)
4. Immediate visual feedback during drag (responsive)
5. Comprehensive accessibility support (inclusive)

**Next Steps**:
- Proceed to Phase 6: Specification Writing
- Create implementation plan
- Create task list
- Begin implementation

---

**End of UI/UX Design Specification**
