# UI/UX Design: Text Selection Menu Settings

**Date:** 2025-11-27 22:05:00 PST
**Phase:** 5.5 - UI/UX Design

## Executive Summary

This document defines the visual design and interaction patterns for the text selection menu settings feature. Design decisions follow Material Design 3 principles and existing Feeder app patterns.

**Key Decisions**:
1. **Dedicated Settings Screen**: Separate screen accessed from main settings
2. **Card-Based List**: Each menu item in a Material 3 Card
3. **Drag-and-Drop**: Using `sh.calvin.reorderable` library for reordering
4. **Inline Toggle**: Switch on each card for enable/disable
5. **Reset Button**: In app bar with confirmation dialog

**[UPDATED: 2025-11-27]** Changed from up/down arrows to drag-and-drop reordering using the reorderable library.

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
│  │ ≡  Copy                       [●] │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ ≡  Select All                 [●] │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ ≡  Translate                  [●] │  │
│  │    com.google.android.apps...     │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ ≡  To Read                    [○] │  │
│  │    com.example.toread             │  │
│  └──────────────────────────────────┘  │
│                                         │
└────────────────────────────────────────┘
```

**[UPDATED: 2025-11-27]** Replaced up/down arrows with drag handle (≡) icon for drag-and-drop reordering.

### Menu Item Card

**Structure**:
```
┌──────────────────────────────────────┐
│ ≡  [Title]                     [●/○] │
│    [Subtitle] (optional)              │
└──────────────────────────────────────┘
```

**[UPDATED: 2025-11-27]** Replaced up/down arrows with drag handle icon.

**Components**:

1. **Drag Handle** (Icon, leftmost):
   - **Icon**: `Icons.Rounded.DragHandle` (≡)
   - **Size**: 24dp
   - **Color**: `onSurfaceVariant`
   - **Purpose**: Visual indicator for drag-and-drop
   - **Modifier**: `Modifier.draggableHandle()` from reorderable library
   - **Content Description**: "Drag to reorder"

2. **Middle Section** (Column, weight=1f):
   - **Title** (Text):
     - Typography: `bodyLarge`
     - Color: `onSurface`
     - Examples: "Copy", "Select All", "Translate"

   - **Subtitle** (Text, optional):
     - Typography: `bodySmall`
     - Color: `onSurfaceVariant`
     - Examples: "com.google.android.apps.translate"
     - Only shown for text processors

3. **Right Side** (rightmost):
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
│ ≡  Copy                          [●] │
└──────────────────────────────────────┘
```

- Title: Full opacity
- Switch: ON (checked)
- Drag handle: Always visible and active

#### Disabled Item

```
┌──────────────────────────────────────┐
│ ≡  Select All                    [○] │
└──────────────────────────────────────┘
```

- Title: Full opacity (no visual change in settings screen)
- Switch: OFF (unchecked)
- Drag handle: Always visible and active

**Note**: Disabled state affects text selection menu appearance, not the settings screen card. Items can still be reordered when disabled.

#### Dragging State

```
┌──────────────────────────────────────┐
│ ≡  Copy                          [●] │  ← Elevated, shadow visible
└──────────────────────────────────────┘
```

- **Elevation**: 4dp shadow (animated)
- **Z-index**: Lifted above other items
- **Opacity**: 100% (fully visible)
- **Haptic Feedback**:
  - Start drag: `GestureThresholdActivate`
  - During drag: `SegmentFrequentTick` (on each position change)
  - End drag: `GestureEnd`

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

**[UPDATED: 2025-11-27]** Reordering now uses drag-and-drop instead of up/down arrows.

### Drag-and-Drop Reordering

**User Action**: Long-press drag handle on "Translate" item and drag upward

**Step 1 - Start Drag**:
```
1. Copy [●]
2. Translate [●]  ← User long-presses drag handle
3. Select All [●]
```
- Haptic feedback: `GestureThresholdActivate`
- Card elevation increases to 4dp
- Item visually "lifts" above list

**Step 2 - During Drag**:
```
1. [Placeholder space]
2. Copy [●]
3. Translate [●]  ← Dragging upward (elevated)
4. Select All [●]
```
- Item follows finger/pointer position
- Other items shift to make space
- Haptic feedback: `SegmentFrequentTick` on each position change
- Smooth reordering animation

**Step 3 - Release/Drop**:
```
1. Translate [●]  ← Dropped in new position
2. Copy [●]
3. Select All [●]
```
- Haptic feedback: `GestureEnd`
- Card elevation returns to 1dp (animated)
- List state updates
- Configuration saved

**Alternative: Drag Downward**:

User can also drag "Copy" downward to reorder:

**Before**:
```
1. Copy [●]  ← Drag handle pressed
2. Select All [●]
3. Translate [●]
```

**During drag**:
```
1. [Placeholder space]
2. Select All [●]
3. Copy [●]  ← Dragging downward (elevated)
4. Translate [●]
```

**After release**:
```
1. Select All [●]
2. Translate [●]
3. Copy [●]  ← New position
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

**[UPDATED: 2025-11-27]** Accessibility updated for drag-and-drop interactions.

### Screen Reader Announcements

**Menu Item Card**:
```
"Copy. Enabled. Position 1 of 4. Drag to reorder, button. Toggle, switch, on."
```

**When drag starts**:
```
"Started dragging Copy"
```

**During drag (position changes)**:
```
"Copy moved to position 2"
```

**When drag ends**:
```
"Copy placed at position 2 of 4"
```

**When switch toggled**:
```
"Copy disabled" or "Copy enabled"
```

### Content Descriptions

- **Drag Handle**: "Drag to reorder"
- **Switch**: State handled automatically by Material 3 Switch
- **Reset Icon**: "Reset to defaults"

### Drag-and-Drop Accessibility

The reorderable library provides built-in accessibility support:

1. **Screen Reader Mode**:
   - Drag handle acts as a button
   - User can tap to select item
   - Use volume keys or custom gestures to move
   - Announces position changes

2. **Keyboard Navigation** (if using external keyboard):
   - Tab to focus drag handle
   - Space/Enter to grab item
   - Arrow keys to move up/down
   - Space/Enter again to drop
   - Escape to cancel

3. **TalkBack Integration**:
   - Supports TalkBack drag-and-drop gestures
   - Provides audio feedback for all state changes
   - Clear instructions when drag handle focused

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

**[UPDATED: 2025-11-27]** Animation updated for drag-and-drop interactions.

### Switch Toggle

- Duration: 150ms
- Easing: Fast out, slow in
- Material 3 default animation

### Drag-and-Drop Reordering

**Drag Start**:
- Elevation: Animate from 1dp to 4dp
- Duration: 150ms
- Easing: Fast out, slow in

**During Drag**:
- Item follows touch/pointer smoothly (no lag)
- Other items animate to make space
- Spring animation for item repositioning
- Smooth, fluid movement

**Drag End**:
- Elevation: Animate from 4dp back to 1dp
- Duration: 200ms
- Easing: Fast out, slow in
- Item settles into final position

**Item Repositioning** (when other items make space):
- Duration: 300ms
- Easing: Spring animation (natural bounce)
- Items slide up/down smoothly

**Haptic Feedback Timing**:
- Start: Immediately on threshold activation
- During: On each snap to new position
- End: On release

### Dialog

- **Entry**: Fade in + scale up (Material default)
- **Exit**: Fade out + scale down (Material default)
- Duration: 300ms

## Edge Cases - Visual Handling

### No Text Processors Installed

```
┌──────────────────────────────────────┐
│ ≡  Copy                          [●] │
└──────────────────────────────────────┘
│ ≡  Select All                    [●] │
└──────────────────────────────────────┘
```

Only built-in items (Copy, Select All) shown. Drag-and-drop still works for reordering these two items.

### All Items Disabled

```
┌──────────────────────────────────────┐
│ ≡  Copy                          [○] │
└──────────────────────────────────────┘
│ ≡  Select All                    [○] │
└──────────────────────────────────────┘
```

All switches OFF. Text selection menu shows all items grayed out. Drag-and-drop still works for reordering.

### Single Item

```
┌──────────────────────────────────────┐
│ ≡  Copy                          [●] │
└──────────────────────────────────────┘
```

Drag handle still shown but dragging has no effect (nowhere to move). Library handles this gracefully.

### Text Processor with Long Name

```
┌──────────────────────────────────────┐
│ ≡  Super Long Application Na...  [●] │
│    com.example.very.long.package...  │
└──────────────────────────────────────┘
```

- Title: Ellipsis at end if too long
- Subtitle: Ellipsis at end if too long
- Max lines: 1 for title, 1 for subtitle
- Drag handle always visible (fixed width)

## First-Time User Experience

### Scenario: User accesses settings for first time (no saved config)

**Step 1**: User navigates to "Text Selection Menu Settings"

**What they see**:
```
┌──────────────────────────────────────┐
│ ≡  Copy                          [●] │
└──────────────────────────────────────┘
│ ≡  Select All                    [●] │
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
│ ≡  Copy                          [●] │
└──────────────────────────────────────┘
│ ≡  Select All                    [●] │
└──────────────────────────────────────┘
│ ≡  Translate                     [●] │
│    com.google.android.apps...        │
└──────────────────────────────────────┘
│ ≡  To Read                       [●] │
│    com.example.toread                │
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

**[UPDATED: 2025-11-27]** Reversed decision - now using drag-and-drop.

### Up/Down Arrows (Previously Selected, Now Rejected)

**Pros**:
- Simple implementation
- Clear, explicit actions
- No third-party dependencies initially considered
- Precise control

**Cons**:
- More taps needed for large reorders (e.g., move item from position 1 to 10 = 9 taps)
- Less modern UX
- Tedious for frequent reordering
- Not as intuitive as drag-and-drop

**Original Decision**: Selected for simplicity
**Revised Decision**: **Rejected** after code assessment revealed `sh.calvin.reorderable` library exists with excellent Material 3 integration

### Drag-and-Drop Reordering (NOW SELECTED)

**Pros**:
- More intuitive for reordering
- Modern interaction pattern
- Fewer interactions for multiple reorders
- Better UX for power users
- Library available (`sh.calvin.reorderable`) with excellent Material 3 support
- Built-in accessibility (TalkBack, keyboard navigation)
- Haptic feedback support
- Already proven in Android ecosystem

**Cons**:
- Requires third-party library (mitigated: well-maintained, popular library)
- Slightly more complex implementation (mitigated: library handles complexity)
- Need to add dependency

**Revised Decision**: **SELECTED** because the reorderable library provides:
1. Full Material 3 integration (Card, LazyColumn support)
2. Built-in accessibility features
3. Haptic feedback out of the box
4. Well-maintained (active development)
5. Better user experience for this use case

The cons are minimal compared to the significant UX improvement.

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

**[UPDATED: 2025-11-27]** Implementation updated for drag-and-drop.

### Dependencies

**Add to build.gradle.kts**:
```kotlin
dependencies {
    implementation("sh.calvin.reorderable:reorderable:2.4.0") // Check for latest version
}
```

### Material 3 Components Used

- `Scaffold`: Screen structure
- `SensibleTopAppBar`: App bar with scroll behavior
- `LazyColumn`: Scrollable list (with reorderable support)
- `Card`: Item containers
- `Icon`: Drag handle (`Icons.Rounded.DragHandle`)
- `Switch`: Enable/disable toggle
- `AlertDialog`: Reset confirmation
- `TextButton`: Dialog actions

All components from `androidx.compose.material3`.

### Reorderable Library Components

From `sh.calvin.reorderable`:

- `rememberReorderableLazyListState`: Manages drag-and-drop state
- `ReorderableItem`: Wrapper for each draggable item
- `Modifier.draggableHandle()`: Makes drag handle interactive
- `onDragStarted`: Callback for drag start
- `onDragStopped`: Callback for drag end

### Compose Patterns

- `remember`: State for dialog visibility, interaction sources
- `collectAsStateWithLifecycle`: ViewModel state observation
- `nestedScroll`: Scroll behavior integration
- `MutableInteractionSource`: For drag event handling
- `LocalHapticFeedback.current`: Haptic feedback
- `animateDpAsState`: Elevation animation during drag
- Modifier chaining for layout

### Implementation Pattern

```kotlin
val lazyListState = rememberLazyListState()
val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    // Update list order in ViewModel
    viewModel.moveItem(from.index, to.index)

    // Haptic feedback
    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
}

LazyColumn(state = lazyListState) {
    items(menuItems, key = { it.id }) { item ->
        ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
            // Card with drag handle and switch
        }
    }
}
```

### No Custom Components Needed

All UI elements use standard Material 3 components plus the reorderable library. No custom composables required beyond what's specified in code assessment.

## Testing Checklist

**[UPDATED: 2025-11-27]** Testing updated for drag-and-drop.

### Visual Tests

- [ ] Cards display correctly on phone
- [ ] Cards display correctly on tablet
- [ ] Drag handle (≡) icon visible on all items
- [ ] Switch states render correctly
- [ ] Card elevation increases during drag (4dp shadow visible)
- [ ] Dialog appears centered and styled correctly
- [ ] Dark theme renders correctly
- [ ] High contrast mode works
- [ ] Dragged item appears elevated above others

### Interaction Tests

- [ ] Long-press drag handle initiates drag
- [ ] Item follows finger/pointer during drag
- [ ] Other items animate to make space during drag
- [ ] Item can be dragged up
- [ ] Item can be dragged down
- [ ] Releasing drag places item in new position
- [ ] List order persists after drag
- [ ] Switch toggles item enabled state
- [ ] Reset icon shows dialog
- [ ] Reset confirms and applies defaults
- [ ] Cancel closes dialog without changes
- [ ] Back navigation works
- [ ] Can drag when only 2 items exist
- [ ] Single item shows drag handle but dragging has no effect

### Haptic Feedback Tests

- [ ] Haptic feedback on drag start (`GestureThresholdActivate`)
- [ ] Haptic feedback during drag on position change (`SegmentFrequentTick`)
- [ ] Haptic feedback on drag end (`GestureEnd`)
- [ ] No haptic feedback when drag canceled

### Animation Tests

- [ ] Elevation animates smoothly on drag start
- [ ] Elevation animates smoothly on drag end
- [ ] Other items slide smoothly to make space
- [ ] Spring animation feels natural (not too bouncy)
- [ ] No lag or jank during drag

### Accessibility Tests

- [ ] Screen reader announces items correctly
- [ ] Drag handle announced as "Drag to reorder, button"
- [ ] Drag start announced ("Started dragging [item]")
- [ ] Position changes announced during drag
- [ ] Drag end announced ("Placed at position X of Y")
- [ ] Switch states announced
- [ ] TalkBack drag gestures work
- [ ] Keyboard navigation works (if external keyboard)
- [ ] Content descriptions present
- [ ] Focus order logical

## Conclusion

**[UPDATED: 2025-11-27]** Final conclusion updated to reflect drag-and-drop decision.

This UI/UX design provides a modern, intuitive, and accessible interface for configuring text selection menu items. The design:

1. **Follows Material Design 3** principles and Feeder app patterns
2. **Uses modern drag-and-drop** interaction with `sh.calvin.reorderable` library
3. **Combines enable/disable and reordering** in one unified screen
4. **Provides rich haptic feedback** for tactile interaction
5. **Includes smooth animations** for visual polish
6. **Handles edge cases** gracefully
7. **Meets accessibility requirements** with built-in TalkBack and keyboard support
8. **Leverages well-maintained library** instead of reinventing the wheel

**Key Improvement Over Original Design**:
Drag-and-drop provides significantly better UX for reordering compared to up/down arrows, especially when users need to move items multiple positions. The reorderable library eliminates implementation complexity while providing professional-grade features (haptic feedback, accessibility, animations) out of the box.

**Ready to proceed to Phase 6 (Specification Writing) to formalize all details into final specification.**
