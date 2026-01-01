# UI/UX Design: AI Summary Configuration

**Created:** 2026-01-01 19:13:05 +08:00
**Feature:** Improve AI Integration Summary Configuration
**Status:** UI/UX Design Complete
**Phase:** 5.5

---

## Overview

This document defines the user interface and user experience design for the AI Summary Configuration feature. The design follows Material Design 3 guidelines and maintains consistency with the existing Feeder app.

---

## Design Philosophy

**Principles:**
1. **Clarity** - Clear labels and descriptions
2. **Consistency** - Follow existing patterns
3. **Simplicity** - Minimal cognitive load
4. **Accessibility** - Inclusive design
5. **Feedback** - Immediate visual feedback

---

## Screen Layouts

### Screen 1: AI Integration Settings (Main Settings)

**Location:** Settings > AI Integration

**Current State:**
```
┌─────────────────────────────────────────┐
│ ← AI Integration                        │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ AI Provider                   [▼]   │ │
│ │ OpenAI Compatible                    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Summary Language              [▼]   │ │ ← CHANGE THIS
│ │ Auto-detect                          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**New State:**
```
┌─────────────────────────────────────────┐
│ ← AI Integration                        │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ AI Provider                   [→]   │ │
│ │ OpenAI Compatible                    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Summary                      [→]   │ │ ← CHANGED
│ │ Auto-detect (Enabled)                │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Changes:**
1. Rename "Summary Language" → "Summary"
2. Change icon from dropdown dropdown (▼) to navigation arrow (→)
3. Update subtitle to show status: "{Language} (Enabled)" or "Disabled"
4. Tapping entire row navigates to dedicated screen

### Screen 2: Summary Settings (New Screen)

**Location:** Settings > AI Integration > Summary

**Layout:**
```
┌─────────────────────────────────────────┐
│ ← Summary Settings                      │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Enable Summaries              [ON]  │ │ Switch
│ │ Automatically generate AI...        │ │ Description
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Language                      [▼]  │ │ Dropdown
│ │ Auto-detect                          │ │ Selected
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ 💡 Tip                              │ │ Info card
│ │ When enabled, summaries will be     │ │
│ │ automatically generated for articles│ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Components:**
1. **TopAppBar** - Title "Summary Settings", back button
2. **Enable Switch** - Toggle for enable/disable
3. **Language Dropdown** - Language selector (disabled if summaries disabled)
4. **Info Card** - Helpful tip (optional)

---

## Component Specifications

### Component 1: Summary Settings Item (Main Settings)

**Type:** Navigation Item

**Layout:**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onNavigateToSummary() }
        .padding(vertical = 16.dp, horizontal = 16.dp)
        .semantics { role = Role.Button },
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Icon placeholder (64x64)
    }

    Column(
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = if (summaryEnabled) {
                "$languageName (Enabled)"
            } else {
                "Disabled"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Configure summary",
    )
}
```

**States:**
1. **Enabled with language** - Shows "{Language} (Enabled)"
2. **Disabled** - Shows "Disabled"
3. **Pressed** - Ripple effect

**Accessibility:**
- Role: Button
- Content description: "Configure summary, currently {state}"
- Minimum touch target: 48dp
- Clickable entire row

### Component 2: Enable Switch (Summary Settings)

**Type:** Switch Setting

**Layout:**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
        .padding(vertical = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = "Enable Summaries",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Automatically generate AI summaries for articles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}
```

**States:**
1. **ON** - Switch is right, color is primary
2. **OFF** - Switch is left, color is secondary
3. **Pressed** - Ripple effect

**Accessibility:**
- Role: Switch
- State description: "On" / "Off"
- Content description: "Enable summaries, currently {state}"

### Component 3: Language Selector (Summary Settings)

**Type:** Dropdown Menu

**Layout:**
```kotlin
Column {
    Text(
        text = "Language",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLanguage.displayName,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,  // Disabled when summaries are disabled
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SummaryLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(id = language.displayName))
                    },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    },
                    leadingIcon = if (language == selectedLanguage) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else null,
                )
            }
        }
    }
}
```

**States:**
1. **Expanded** - Shows dropdown menu
2. **Collapsed** - Shows selected value
3. **Disabled** - Grayed out (when summaries disabled)
4. **Item Selected** - Shows checkmark

**Accessibility:**
- Role: Dropdown
- Content description: "Select language, currently {language}"
- Items are selectable

### Component 4: Info Card (Optional)

**Type:** Informational Card

**Layout:**
```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(end = 16.dp),
        )

        Column {
            Text(
                text = "Tip",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "When enabled, summaries will be automatically generated for articles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
```

---

## Visual Design

### Color Scheme

**Using Material 3 Theme Colors:**

**Switch (ON):**
- Thumb: `MaterialTheme.colorScheme.primary`
- Track: `MaterialTheme.colorScheme.primaryContainer`

**Switch (OFF):**
- Thumb: `MaterialTheme.colorScheme.outline`
- Track: `MaterialTheme.colorScheme.surfaceVariant`

**Dropdown:**
- Border: `MaterialTheme.colorScheme.outline`
- Focused Border: `MaterialTheme.colorScheme.primary`
- Text: `MaterialTheme.colorScheme.onSurface`

**Info Card:**
- Background: `MaterialTheme.colorScheme.primaryContainer`
- Text: `MaterialTheme.colorScheme.onPrimaryContainer`
- Icon: `MaterialTheme.colorScheme.onPrimaryContainer`

### Typography

**Title Text:** `MaterialTheme.typography.bodyLarge`
- Size: 16sp
- Weight: Regular

**Subtitle/Description:** `MaterialTheme.typography.bodySmall`
- Size: 12sp
- Weight: Regular
- Color: `onSurfaceVariant`

**Label:** `MaterialTheme.typography.labelMedium`
- Size: 12sp
- Weight: Medium

### Spacing

**Vertical Spacing:**
- Between sections: 16.dp
- Within sections: 8.dp
- Padding (screen): 16.dp

**Horizontal Spacing:**
- Between elements: 16.dp
- Padding (screen): 16.dp

**Icon Size:**
- Leading icon: 64.dp x 64.dp
- Trailing icon: 24.dp x 24.dp
- Info icon: 20.dp x 20.dp

---

## Interaction Design

### Navigation Flow

```
Settings Screen
    ↓ (tap "Summary")
Summary Settings Screen
    ↓ (tap back)
AI Integration Settings Screen
```

**Transition:**
- Duration: 300ms
- Easing: Standard
- Fade in/out

### Toggle Switch Interaction

**User Action:** Tap switch or entire row

**Response:**
1. Immediate visual feedback (ripple)
2. Switch animates to new position (100ms)
3. Language selector enables/disables
4. State persists to SharedPreferences

**Expected Timing:**
- Visual feedback: < 50ms
- Animation: 100ms
- Persistence: < 10ms

### Language Selection Interaction

**User Action:** Tap dropdown

**Response:**
1. Dropdown menu expands
2. Show all language options
3. Selected item has checkmark
4. Tap item to select
5. Menu closes
6. Dropdown updates with selected language
7. State persists to SharedPreferences

**Expected Timing:**
- Menu expansion: 200ms
- Selection: Immediate
- Menu collapse: 200ms
- Persistence: < 10ms

---

## States and Variations

### State 1: Summaries Enabled

**Visual:**
- Switch is ON (right position, primary color)
- Language dropdown is enabled (interactive)
- Dropdown shows current language
- Main settings shows "{Language} (Enabled)"

**Interaction:**
- User can toggle OFF
- User can change language

### State 2: Summaries Disabled

**Visual:**
- Switch is OFF (left position, outline color)
- Language dropdown is disabled (grayed out, not interactive)
- Dropdown shows current language (read-only)
- Main settings shows "Disabled"

**Interaction:**
- User can toggle ON
- User cannot change language

### State 3: Loading (Transient)

**Visual:**
- Show loading indicator if needed
- (Not expected for this feature - operations are instant)

### State 4: Error (Transient)

**Visual:**
- Show toast or snackbar on error
- (Not expected for this feature - operations are local)

---

## Accessibility

### Screen Reader Support

**Main Settings Item:**
- Label: "Summary"
- State: "Currently {language} (Enabled)" or "Disabled"
- Action: "Double tap to configure"

**Switch:**
- Label: "Enable Summaries"
- State: "On" or "Off"
- Action: "Double tap to toggle"

**Dropdown:**
- Label: "Language"
- Value: Currently selected language
- Action: "Double tap to change"

### Touch Targets

**Minimum Size:** 48dp x 48dp

**Implementation:**
- Entire row is clickable (48dp height minimum)
- Switch touch target: 48dp x 48dp (intrinsic)
- Dropdown touch target: 48dp height minimum

### Color Contrast

**Requirements:** WCAG AA compliant (4.5:1 minimum)

**Implementation:**
- Use Material 3 color system (compliant by default)
- Text on background: Primary on PrimaryContainer (compliant)
- Disabled text: OnSurfaceVariant with reduced alpha (compliant)

### Semantic Markup

**Switch:**
```kotlin
semantics {
    role = Role.Switch
    state = if (checked) "On" else "Off"
}
```

**Dropdown:**
```kotlin
semantics {
    role = Role.Dropdown
    contentDescription = "Select language"
}
```

---

## Responsive Design

### Landscape Orientation

**Layout:**
- Same as portrait
- No special handling needed
- Content scrolls if needed

### Tablet Layout

**Considerations:**
- Max content width: `LocalDimens.current.maxContentWidth`
- Center content on larger screens
- Same layout, just constrained width

---

## Animations and Transitions

### Switch Toggle

**Duration:** 100ms
**Easing:** Standard
**Animation:** Thumb slides between positions

### Dropdown Expand/Collapse

**Duration:** 200ms
**Easing:** Standard
**Animation:** Fade + Scale

### Navigation Transition

**Duration:** 300ms
**Easing:** Standard
**Animation:** Horizontal slide + Fade

---

## Error States

### No Errors Expected

This feature uses local operations (SharedPreferences) which are reliable and don't typically fail. No special error states are needed.

**Potential Issues:**
- SharedPreferences corruption (use defaults)
- Navigation failure (handle gracefully)

---

## Edge Cases

### Edge Case 1: No Language Selected (Shouldn't Happen)

**Handling:**
- Default to Auto-detect
- Enum has default value

### Edge Case 2: Rapid Toggling

**Handling:**
- State is synchronous
- Last toggle wins
- No race conditions (StateFlow is thread-safe)

### Edge Case 3: Rotation During Edit

**Handling:**
- State is preserved (remember)
- No data loss

### Edge Case 4: Background During Edit

**Handling:**
- State is preserved (ViewModel)
- No data loss

---

## Design Tokens

### Colors

```kotlin
// Material 3 theme colors
primary: Color
onPrimary: Color
primaryContainer: Color
onPrimaryContainer: Color
secondary: Color
onSecondary: Color
surface: Color
onSurface: Color
onSurfaceVariant: Color
outline: Color
```

### Typography

```kotlin
bodyLarge: TextStyle
bodySmall: TextStyle
labelMedium: TextStyle
```

### Spacing

```kotlin
paddingSmall: 8.dp
paddingMedium: 16.dp
iconSizeSmall: 20.dp
iconSizeMedium: 24.dp
iconSizeLarge: 64.dp
```

---

## Mockups

### Mockup 1: Main Settings Screen (Before)

```
┌─────────────────────────────────────────┐
│ ← AI Integration                        │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ AI Provider                   [▼]   │ │
│ │ OpenAI Compatible                    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Summary Language              [▼]   │ │
│ │ Auto-detect                          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Mockup 2: Main Settings Screen (After)

```
┌─────────────────────────────────────────┐
│ ← AI Integration                        │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ AI Provider                   [→]   │ │
│ │ OpenAI Compatible                    │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Summary                      [→]   │ │
│ │ Auto-detect (Enabled)                │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Mockup 3: Summary Settings Screen (Enabled)

```
┌─────────────────────────────────────────┐
│ ← Summary Settings                      │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Enable Summaries              [●]   │ │ ← ON
│ │ Automatically generate AI...        │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Language                      [▼]  │ │
│ │ Auto-detect                          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Mockup 4: Summary Settings Screen (Disabled)

```
┌─────────────────────────────────────────┐
│ ← Summary Settings                      │
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Enable Summaries              [○]   │ │ ← OFF
│ │ Automatically generate AI...        │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Language                      [▼]  │ │ ← Disabled
│ │ Auto-detect                          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## Usability Considerations

### Discoverability

**Problem:** Users may not know they can tap "Summary" to configure

**Solutions:**
1. Use navigation arrow (→) instead of dropdown (▼)
2. Make entire row clickable (not just arrow)
3. Show informative subtitle
4. Follow existing pattern (Provider List)

### Learnability

**Problem:** Users may not understand what "Enable Summaries" does

**Solutions:**
1. Clear description: "Automatically generate AI summaries for articles"
2. Optional info card with tip
3. Current state visible in main settings

### Memorability

**Problem:** Users may forget where to find settings

**Solutions:**
1. Consistent location (Settings > AI Integration > Summary)
2. Clear navigation hierarchy
3. Visible in main settings

### Efficiency

**Problem:** Too many taps to configure

**Solutions:**
1. One tap from main settings
2. Immediate feedback on toggle
3. No confirmation dialogs
4. Fast dropdown selection

---

## Design Validation

### Heuristic Evaluation

**Nielsen's 10 Heuristics:**
1. ✅ **Visibility of system status** - Clear switch state
2. ✅ **Match between system and real world** - Familiar switch/dropdown
3. ✅ **User control and freedom** - Easy to toggle back
4. ✅ **Consistency and standards** - Material 3 components
5. ✅ **Error prevention** - No error-prone operations
6. ✅ **Recognition rather than recall** - Visible state
7. ✅ **Flexibility and efficiency of use** - Quick access
8. ✅ **Aesthetic and minimalist design** - Clean layout
9. ✅ **Help users recognize errors** - (No errors expected)
10. ✅ **Help and documentation** - Clear labels

### Cognitive Walkthrough

**Task: Enable summaries**

1. **User Goal:** Enable AI summaries
2. **Action:** Tap "Summary" in settings
3. **Feedback:** Navigate to summary settings screen
4. **Action:** Tap "Enable Summaries" switch
5. **Feedback:** Switch toggles ON, language selector enables
6. **Result:** Summaries are enabled ✅

**Task: Change summary language**

1. **User Goal:** Change summary language to Spanish
2. **Action:** Tap "Summary" in settings
3. **Feedback:** Navigate to summary settings screen
4. **Action:** Tap "Language" dropdown
5. **Feedback:** Dropdown menu shows languages
6. **Action:** Tap "Español"
7. **Feedback:** Dropdown closes, shows "Español"
8. **Result:** Language changed ✅

---

## Conclusion

**Design Summary:**
- ✅ Clean, intuitive interface
- ✅ Follows Material Design 3 guidelines
- ✅ Consistent with existing app
- ✅ Accessible to all users
- ✅ Responsive to different screen sizes
- ✅ Clear feedback and affordances

**Ready for Implementation:**
- ✅ All screens designed
- ✅ All components specified
- ✅ Interactions defined
- ✅ Accessibility addressed
- ✅ Edge cases covered

**Next Phase:** Specification Writing

---

**UI/UX Design Complete:** 2026-01-01 19:13:10 +08:00
