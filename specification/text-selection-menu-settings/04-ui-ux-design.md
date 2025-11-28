# UI/UX Design: Text Selection Menu Settings

**Date:** 2025-11-27 22:05:00 PST
**Phase:** 5.5 - UI/UX Design

## Executive Summary

This document defines the visual design and interaction patterns for the text selection menu settings feature. Design decisions follow Material Design 3 principles and existing Feeder app patterns.

**Key Decisions**:
1. **Dedicated Settings Screen**: Separate screen accessed from main settings
2. **Card-Based List**: Each menu item in a Material 3 Card
3. **Up/Down Arrows**: For reordering (no drag-and-drop)
4. **Inline Toggle**: Switch on each card for enable/disable
5. **Reset Button**: In app bar with confirmation dialog

## Screen Flow

```
Main Settings Screen
  → Text Selection Menu (ExternalSetting)
    → Text Selection Menu Settings Screen
      → [List of menu items with controls]
      → Reset button (app bar) → Confirmation Dialog
```

## Main Settings Screen Integration

### Visual Design

**Location**: After "Text Settings" section, before "Synchronization"

```
┌────────────────────────────────────────┐
│ Text                                    │
├────────────────────────────────────────┤
│   [System Default]                      │
│   Text Settings                    >   │
├────────────────────────────────────────┤
│                                         │
├────────────────────────────────────────┤
│ Text Selection Menu                     │
├────────────────────────────────────────┤
│   [5 items configured]                  │
│   Text Selection Menu Settings     >   │
├────────────────────────────────────────┤
│                                         │
├────────────────────────────────────────┤
│ Synchronization                         │
└────────────────────────────────────────┘
```

**Component**: `ExternalSetting`
- **Title**: "Text Selection Menu Settings"
- **Current Value**: "X items configured" (dynamic count)
- **Icon**: None (consistent with other text/settings items)
- **Action**: Navigate to Text Selection Menu Settings Screen

## Text Selection Menu Settings Screen

### App Bar

```
┌────────────────────────────────────────┐
│ ←  Text Selection Menu Settings   ⟲   │
└────────────────────────────────────────┘
```

**Components**:
- **Navigation Icon**: Back arrow (left)
- **Title**: "Text Selection Menu Settings"
- **Action Icon**: Reset icon (⟲ RestartAlt) (right)

**Behavior**:
- Back arrow: Navigate back to main settings
- Reset icon: Show reset confirmation dialog

### Content Area

**Layout**: `LazyColumn` with `Card` items, 16dp padding, 8dp spacing

```
┌────────────────────────────────────────┐
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ Copy                              │  │
│  │                         ↑ ↓  [●] │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ Select All                        │  │
│  │                         ↑ ↓  [●] │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ Translate                         │  │
│  │ com.google.android.apps.translate │  │
│  │                         ↑ ↓  [●] │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ To Read                           │  │
│  │ com.example.toread                │  │
│  │                         ↑ ↓  [○] │  │
│  └──────────────────────────────────┘  │
│                                         │
└────────────────────────────────────────┘
```

### Menu Item Card

**Structure**:
```
┌──────────────────────────────────────┐
│ [Title]                               │
│ [Subtitle] (optional)                 │
│                         ↑ ↓  [●/○]   │
└──────────────────────────────────────┘
```

**Components**:

1. **Left Side** (Column, weight=1f):
   - **Title** (Text):
     - Typography: `bodyLarge`
     - Color: `onSurface`
     - Examples: "Copy", "Select All", "Translate"

   - **Subtitle** (Text, optional):
     - Typography: `bodySmall`
     - Color: `onSurfaceVariant`
     - Examples: "com.google.android.apps.translate"
     - Only shown for text processors

2. **Right Side** (Row, horizontal spacing 4dp):
   - **Up Arrow** (IconButton):
     - Icon: `Icons.Default.ArrowUpward`
     - Enabled: When item can move up (not first)
     - Disabled: Gray out when first item

   - **Down Arrow** (IconButton):
     - Icon: `Icons.Default.ArrowDownward`
     - Enabled: When item can move down (not last)
     - Disabled: Gray out when last item

   - **Toggle Switch** (Switch):
     - Checked: Item enabled (appears in menu)
     - Unchecked: Item disabled (grayed in menu)

**Card Styling**:
- Material 3 Card with default elevation
- Fill max width
- 16dp padding inside card

### Visual States

#### Enabled Item

```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑ ↓  [●]     │
└──────────────────────────────────────┘
```

- Title: Full opacity
- Switch: ON (checked)
- Arrows: Enabled (based on position)

#### Disabled Item

```
┌──────────────────────────────────────┐
│ Select All                            │
│                         ↑ ↓  [○]     │
└──────────────────────────────────────┘
```

- Title: Full opacity (no visual change in settings screen)
- Switch: OFF (unchecked)
- Arrows: Enabled (based on position)

**Note**: Disabled state affects text selection menu appearance, not the settings screen card.

#### First Item

```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑̶ ↓  [●]     │
└──────────────────────────────────────┘
```

- Up arrow: Disabled (grayed out)
- Down arrow: Enabled
- Can only move down

#### Last Item

```
┌──────────────────────────────────────┐
│ To Read                               │
│ com.example.toread                    │
│                         ↑ ↓̶  [○]     │
└──────────────────────────────────────┘
```

- Up arrow: Enabled
- Down arrow: Disabled (grayed out)
- Can only move up

### Reset Confirmation Dialog

```
┌──────────────────────────────────────┐
│ Reset to Defaults                     │
│                                       │
│ Reset text selection menu to default │
│ settings? This will restore the       │
│ default order and enable all items.   │
│                                       │
│                   [CANCEL]  [RESET]  │
└──────────────────────────────────────┘
```

**Components**:
- **Title**: "Reset to Defaults"
- **Content**: Confirmation message explaining action
- **Buttons**:
  - **Cancel**: Dismiss dialog, no changes
  - **Reset**: Confirm reset, restore defaults

**Behavior**:
- Triggered by reset icon in app bar
- Modal dialog (blocks interaction)
- Cancel: Close dialog
- Reset:
  - Apply default configuration
  - Close dialog
  - Settings screen updates to show defaults

## Text Selection Menu Appearance

### Current (Before Settings)

All items shown in fixed order:

```
┌──────────────────────┐
│ Copy                 │
│ Select All           │
│ Translate            │
│ To Read              │
└──────────────────────┘
```

### After User Configuration

Example: User disabled "Select All" and reordered:

**Settings Screen Shows**:
```
1. Copy [●]
2. Translate [●]
3. To Read [●]
4. Select All [○]  ← Disabled
```

**Text Selection Menu Shows** (when text selected):
```
┌──────────────────────┐
│ Copy                 │  ← Enabled, clickable
│ Translate            │  ← Enabled, clickable
│ To Read              │  ← Enabled, clickable
│ Select All           │  ← Grayed out, not clickable
└──────────────────────┘
```

**Visual Styling for Disabled Items**:
- Text color: Gray (using `ForegroundColorSpan`)
- Not clickable: `menuItem.isEnabled = false`
- Still visible: User can see it's available

## Interaction Patterns

### Reordering Items

**User Action**: Tap up arrow on "Translate" item (currently position 2)

**Before**:
```
1. Copy [●]
2. Translate [●]  ← Tap up arrow
3. Select All [●]
```

**After**:
```
1. Translate [●]  ← Moved up
2. Copy [●]
3. Select All [●]
```

**Visual Feedback**:
- Immediate update (no animation)
- List reorders instantly
- Arrow button states update (first item's up arrow disables)

**User Action**: Tap down arrow on "Copy" item

**Before**:
```
1. Translate [●]
2. Copy [●]  ← Tap down arrow
3. Select All [●]
```

**After**:
```
1. Translate [●]
2. Select All [●]
3. Copy [●]  ← Moved down
```

### Enabling/Disabling Items

**User Action**: Toggle switch for "Select All" (currently enabled)

**Before**:
```
Select All                    ↑ ↓  [●]  ← Switch ON
```

**After**:
```
Select All                    ↑ ↓  [○]  ← Switch OFF
```

**Effect**:
- Switch animates to OFF state
- "Select All" will appear grayed in text selection menu
- Configuration saved immediately
- No other visual change in settings

### Reset to Defaults

**User Flow**:

1. **User taps reset icon** in app bar
   ```
   → Reset confirmation dialog appears
   ```

2. **User taps "RESET" button**
   ```
   → Dialog closes
   → Settings revert to defaults:
      - Order: Copy (0), Select All (1), [processors...]
      - All items enabled
   → List updates instantly
   ```

3. **Alternative: User taps "CANCEL"**
   ```
   → Dialog closes
   → No changes applied
   ```

## Accessibility

### Screen Reader Announcements

**Menu Item Card**:
```
"Copy. Enabled. Position 1 of 4. Move up, button, disabled. Move down, button. Toggle, switch, on."
```

**When switch toggled**:
```
"Copy disabled" or "Copy enabled"
```

**Move up button tapped**:
```
"Copy moved to position 1" (or specific position)
```

### Content Descriptions

- **Up Arrow**: "Move up" (or "Move up, disabled" when first)
- **Down Arrow**: "Move down" (or "Move down, disabled" when last)
- **Switch**: State handled automatically by Material 3 Switch
- **Reset Icon**: "Reset to defaults"

### Keyboard Navigation

- **Tab**: Move between interactive elements
- **Space/Enter**: Activate buttons and switches
- **Arrow keys**: Navigate list (optional enhancement)

### High Contrast

- Disabled arrow buttons have clear visual distinction
- Switch states clearly visible
- Card borders visible in high contrast mode

## Responsive Design

### Phone (Portrait)

Standard layout as described above.

**Constraints**:
- Cards fill width minus 16dp horizontal padding
- Content width: `dimens.maxContentWidth` (consistent with app)

### Tablet (Landscape)

Same layout with max content width constraint:

```
┌────────────────────────────────────────────────┐
│                                                 │
│      ┌──────────────────────────────────┐      │
│      │ Card items (max width)           │      │
│      └──────────────────────────────────┘      │
│                                                 │
└────────────────────────────────────────────────┘
```

**Constraints**:
- Content width: `dimens.maxContentWidth` (typically 600dp)
- Centered horizontally
- Same vertical spacing

## Visual Design Specs

### Typography

| Element | Style | Size | Weight |
|---------|-------|------|--------|
| Screen title | titleLarge | 22sp | Normal |
| Card title | bodyLarge | 16sp | Normal |
| Card subtitle | bodySmall | 12sp | Normal |
| Dialog title | titleLarge | 22sp | Normal |
| Dialog content | bodyMedium | 14sp | Normal |
| Buttons | labelLarge | 14sp | Medium |

### Colors

| Element | Light Theme | Dark Theme |
|---------|-------------|------------|
| Card background | surface | surface |
| Card title | onSurface | onSurface |
| Card subtitle | onSurfaceVariant | onSurfaceVariant |
| Icon (enabled) | onSurfaceVariant | onSurfaceVariant |
| Icon (disabled) | onSurface (38% opacity) | onSurface (38% opacity) |
| Switch (on) | primary | primary |
| Switch (off) | outline | outline |

### Spacing

| Element | Value |
|---------|-------|
| Screen horizontal padding | 16dp |
| Card vertical spacing | 8dp |
| Card internal padding | 16dp |
| Icon button spacing | 4dp |
| Dialog padding | 24dp |

### Dimensions

| Element | Size |
|---------|------|
| Icon button | 48dp × 48dp |
| Switch width | ~52dp (Material default) |
| Card min height | ~64dp (auto, based on content) |
| App bar height | 64dp (Material default) |

## Elevation

| Element | Elevation |
|---------|-----------|
| App bar | 0dp (scrollBehavior handles) |
| Card | 1dp (Material default) |
| Dialog | 24dp (Material default) |

## Animation

### Switch Toggle

- Duration: 150ms
- Easing: Fast out, slow in
- Material 3 default animation

### List Reorder

- **No animation**: Instant update
- Reason: Simple, predictable, no layout shift issues
- Alternative future enhancement: Animate items swapping

### Dialog

- **Entry**: Fade in + scale up (Material default)
- **Exit**: Fade out + scale down (Material default)
- Duration: 300ms

## Edge Cases - Visual Handling

### No Text Processors Installed

```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑̶ ↓  [●]     │
└──────────────────────────────────────┘
│ Select All                            │
│                         ↑ ↓̶  [●]     │
└──────────────────────────────────────┘
```

Only built-in items (Copy, Select All) shown. No text processors in list.

### All Items Disabled

```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑ ↓  [○]     │
└──────────────────────────────────────┘
│ Select All                            │
│                         ↑ ↓  [○]     │
└──────────────────────────────────────┘
```

All switches OFF. Text selection menu shows all items grayed out.

### Single Item

```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑̶ ↓̶  [●]     │
└──────────────────────────────────────┘
```

Both arrows disabled (can't reorder single item).

### Text Processor with Long Name

```
┌──────────────────────────────────────┐
│ Super Long Application Name That...  │
│ com.example.very.long.package.name   │
│                         ↑ ↓  [●]     │
└──────────────────────────────────────┘
```

- Title: Ellipsis at end if too long
- Subtitle: Ellipsis at end if too long
- Max lines: 1 for title, 1 for subtitle

## First-Time User Experience

### Scenario: User accesses settings for first time (no saved config)

**Step 1**: User navigates to "Text Selection Menu Settings"

**What they see**:
```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑̶ ↓  [●]     │
└──────────────────────────────────────┘
│ Select All                            │
│                         ↑ ↓̶  [●]     │
└──────────────────────────────────────┘
```

Only default items (Copy, Select All) shown.

**Why**: Text processors not yet discovered (discovery happens on first text selection).

**Step 2**: User selects text somewhere in app

**What happens**:
- Text selection menu built for first time
- Text processors discovered via PackageManager
- Automatically added to configuration with default order

**Step 3**: User returns to settings

**What they see**:
```
┌──────────────────────────────────────┐
│ Copy                                  │
│                         ↑ ↓  [●]     │
└──────────────────────────────────────┘
│ Select All                            │
│                         ↑ ↓  [●]     │
└──────────────────────────────────────┘
│ Translate                             │
│ com.google.android.apps.translate     │
│                         ↑ ↓  [●]     │
└──────────────────────────────────────┘
│ To Read                               │
│ com.example.toread                    │
│                         ↑ ↓̶  [●]     │
└──────────────────────────────────────┘
```

Text processors now visible and configurable.

**No onboarding dialog**: User can explore and configure at their own pace.

## Comparison with Existing Patterns

### Similar to "Block List" Setting

Both use:
- External setting navigation
- Dedicated screen for management
- List-based UI
- Add/remove or enable/disable items

**Differences**:
- Block List: Text input to add, tap to remove
- Text Menu: No add/remove (auto-discovered), reordering capability

### Similar to "Notifications" Setting

Both use:
- External setting navigation
- List with toggle switches
- Per-item configuration

**Differences**:
- Notifications: Permissions handling, grouped by feed
- Text Menu: Reordering, simpler structure

### Consistent with Overall App

**Follows these Feeder patterns**:
- Material Design 3 components
- `SensibleTopAppBar` with scroll behavior
- `ExternalSetting` navigation
- `Card` for list items
- `dimens.maxContentWidth` for responsive layout
- Switch toggles for boolean states

## Alternative Designs Considered

### Drag-and-Drop Reordering

**Pros**:
- More intuitive for reordering
- Modern interaction pattern
- Fewer taps for multiple reorders

**Cons**:
- Complex implementation (no existing pattern in app)
- Accessibility challenges (needs keyboard alternative)
- Risk of accidental drags
- Requires third-party library or custom code

**Decision**: Rejected in favor of up/down arrows for simplicity and accessibility.

### Inline in Main Settings

**Pros**:
- No navigation needed
- Immediate visibility

**Cons**:
- Would clutter main settings screen
- Reordering UI doesn't fit inline pattern
- Complex interaction in constrained space

**Decision**: Rejected in favor of dedicated screen.

### Separate Enable/Disable and Reorder Screens

**Pros**:
- Simpler per-screen UI
- Clear separation of concerns

**Cons**:
- Extra navigation layer
- Split configuration flow

**Decision**: Rejected in favor of unified screen with both controls.

## Implementation Notes

### Material 3 Components Used

- `Scaffold`: Screen structure
- `SensibleTopAppBar`: App bar with scroll behavior
- `LazyColumn`: Scrollable list
- `Card`: Item containers
- `IconButton`: Up/down arrows
- `Switch`: Enable/disable toggle
- `AlertDialog`: Reset confirmation
- `TextButton`: Dialog actions

All components from `androidx.compose.material3`.

### Compose Patterns

- `remember`: State for dialog visibility
- `collectAsStateWithLifecycle`: ViewModel state observation
- `nestedScroll`: Scroll behavior integration
- Modifier chaining for layout

### No Custom Components Needed

All UI elements use standard Material 3 components. No custom composables required beyond what's specified in code assessment.

## Testing Checklist

### Visual Tests

- [ ] Cards display correctly on phone
- [ ] Cards display correctly on tablet
- [ ] Arrow buttons enable/disable based on position
- [ ] Switch states render correctly
- [ ] Dialog appears centered and styled correctly
- [ ] Dark theme renders correctly
- [ ] High contrast mode works

### Interaction Tests

- [ ] Up arrow moves item up
- [ ] Down arrow moves item down
- [ ] First item's up arrow is disabled
- [ ] Last item's down arrow is disabled
- [ ] Switch toggles item enabled state
- [ ] Reset icon shows dialog
- [ ] Reset confirms and applies defaults
- [ ] Cancel closes dialog without changes
- [ ] Back navigation works

### Accessibility Tests

- [ ] Screen reader announces items correctly
- [ ] Button states announced (enabled/disabled)
- [ ] Switch states announced
- [ ] Keyboard navigation works
- [ ] Content descriptions present
- [ ] Focus order logical

## Conclusion

This UI/UX design provides a clean, accessible, and consistent interface for configuring text selection menu items. The design:

1. **Follows Material Design 3** principles and Feeder app patterns
2. **Uses simple up/down arrows** instead of complex drag-and-drop
3. **Combines enable/disable and reordering** in one screen
4. **Provides clear visual feedback** for all actions
5. **Handles edge cases** gracefully
6. **Meets accessibility** requirements

**Ready to proceed to Phase 6 (Specification Writing) to formalize all details into final specification.**
