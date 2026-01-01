# UI/UX Design: AI Summary Language Configuration

**Created:** 2026-01-01 09:53:49
**Status:** Complete

---

## 1. Design Overview

This document specifies the user interface and experience design for the AI summary language configuration feature.

### 1.1 Design Goals

- **Clarity:** Users immediately understand the language setting purpose
- **Discoverability:** Feature is easy to find in settings
- **Efficiency:** Language selection requires minimal taps/clicks
- **Consistency:** Matches existing AI settings UI patterns
- **Accessibility:** Screen reader friendly, high contrast, touch targets

### 1.2 Design Principles

1. **Follow Material 3 Guidelines**
2. **Reuse Existing Patterns** from AIProviderSection
3. **Progressive Disclosure** - Simple default, advanced options hidden
4. **Immediate Feedback** - Selection updates instantly

---

## 2. Screen Layout

### 2.1 Placement in Settings Screen

**Location:** AI Settings Screen (AIProviderSection)

**Position:**
```
┌─────────────────────────────────────────┐
│ AI Settings                             │
├─────────────────────────────────────────┤
│ API Key               [••••••••••••]  ► │
│ Model ID              [gpt-4o-mini]  ► │
│ URL                   [https://...]    │
│ Timeout               [60 seconds]      │
│ Summary Language      [Auto-detect]  ► │ ← NEW
│                                          │
│ [Azure-specific fields if applicable]   │
└─────────────────────────────────────────┘
```

**Rationale:**
- Placed after timeout (after basic configuration)
- Before Azure-specific fields (which are conditional)
- Groups with core AI settings (not provider-specific)

### 2.2 Visual Hierarchy

**Field Order (Priority):**
1. API Key (critical, required)
2. Model ID (critical, required)
3. URL (optional, common)
4. Timeout (optional, common)
5. **Summary Language (NEW, core feature)**
6. Azure fields (provider-specific, conditional)

---

## 3. Component Design

### 3.1 Language Selector Component

**Type:** ExposedDropdownMenuBox with DropdownMenu

**Visual Design:**

**Collapsed State:**
```
┌─────────────────────────────────────────────┐
│ Summary Language        Auto-detect       ▼│
└─────────────────────────────────────────────┘
```

**Expanded State:**
```
┌─────────────────────────────────────────────┐
│ Summary Language        Auto-detect       ▲│
└─────────────────────────────────────────────┘
         ┌───────────────────────────────────┐
         │ ✓ Auto-detect                    │
         │   English                        │
         │   Chinese                        │
         │   Spanish                        │
         │   French                         │
         │   German                         │
         │   Japanese                       │
         │   Korean                         │
         │   Portuguese                     │
         │   Russian                        │
         │   Arabic                         │
         │   Hindi                          │
         └───────────────────────────────────┘
```

**Selected State (Spanish):**
```
┌─────────────────────────────────────────────┐
│ Summary Language        Spanish           ▼│
└─────────────────────────────────────────────┘
```

### 3.2 Dropdown Item Design

**Selected Item:**
```
┌─────────────────────────────────────────────┐
│ ✓ Auto-detect                    ◦│
└─────────────────────────────────────────────┘
   Primary color                    16dp icon
   (MaterialTheme.colorScheme.primary)
```

**Unselected Item:**
```
┌─────────────────────────────────────────────┐
│ ◦ English                         ◦│
└─────────────────────────────────────────────┘
   OnSurface color                  No icon
```

**Specifications:**
- Leading icon: Checkmark (Icons.Default.Check) for selected item
- Text color: Primary for selected, OnSurface for unselected
- Icon size: 16dp
- Icon tint: MaterialTheme.colorScheme.primary
- Item height: 48dp minimum (Material 3 guideline)
- Item padding: 16dp horizontal

### 3.3 Text Styles

**Label:**
```
Font: Roboto
Size: 16sp
Weight: 400 (Regular)
Color: MaterialTheme.colorScheme.onSurfaceVariant
```

**Value (Dropdown trigger):**
```
Font: Roboto
Size: 16sp
Weight: 400 (Regular)
Color: MaterialTheme.colorScheme.onSurface
```

**Dropdown item:**
```
Font: Roboto
Size: 16sp
Weight: 400 (Regular)
Color: Primary (selected) / OnSurface (unselected)
```

---

## 4. Interaction Design

### 4.1 User Flow

**Flow 1: Change Language Setting**

```
1. User opens AI Settings
   └─> AlertDialog appears with current settings

2. User taps "Summary Language" dropdown
   └─> Dropdown expands with 12 options
   └─> Current selection shows checkmark

3. User taps "Spanish"
   └─> Dropdown collapses immediately
   └─> TextField value updates to "Spanish"
   └─> Setting saved to SharedPreferences
   └─> StateFlow emits new value

4. User taps "Save"
   └─> Dialog closes
   └─> Next summary will use Spanish
```

**Flow 2: Cancel Selection**

```
1. User opens dropdown
   └─> Shows current selection (e.g., "Auto-detect")

2. User taps "English"
   └─> Selection updates to "English"
   └─> But user doesn't save yet

3. User taps outside dropdown
   └─> Dropdown collapses
   └─> Selection remains "English" in dialog

4. User taps "Cancel"
   └─> Dialog closes
   └─> Changes discarded (reverts to saved value)
```

### 4.2 Edge Cases

**Case 1: Long Language Names**
- All current names fit in one line
- Future: Consider truncation or ellipsis if needed

**Case 2: Screen Rotation**
- Dialog persists state through configuration changes
- Use `rememberSaveable` for expanded state

**Case 3: Accessibility - TalkBack**
- All dropdown items are semantic buttons
- Selection announced as "Selected, Auto-detect"
- Unselected: "English, Double tap to select"

### 4.3 Feedback Mechanisms

**Immediate Feedback:**
- Dropdown collapses on selection
- Checkmark appears on selected item
- TextField value updates instantly

**Persistent Feedback:**
- Setting saved to SharedPreferences
- Survives app restart

**No Feedback Needed:**
- Toast message (overkill for simple setting)
- Confirmation dialog (dropdown selection is clear)

---

## 5. Responsive Design

### 5.1 Screen Size Adaptation

**Phone (Portrait):**
```
┌───────────────────┐
│ Summary Language  │
│ [Auto-detect  ▼]  │
│                   │
│ [Dropdown with   │
│  12 items]       │
└───────────────────┘
```

**Phone (Landscape):**
```
┌──────────────────────────┐
│ Summary Language         │
│ [Auto-detect         ▼]  │
│                          │
│ [Dropdown with 12 items] │
└──────────────────────────┘
```

**Tablet:**
```
┌─────────────────────────────┐
│ Summary Language            │
│ [Auto-detect             ▼] │
│                             │
│ [Dropdown with 12 items]    │
│ (Centered, max width 600dp) │
└─────────────────────────────┘
```

### 5.2 Dialog Constraints

**Max Width:** 600dp (follows existing dialog pattern)

**Scrolling:**
- Dialog content is scrollable
- Dropdown appears above other content
- No z-index conflicts

---

## 6. Accessibility

### 6.1 Screen Reader Support

**Semantics:**
```kotlin
ExposedDropdownMenuBox(
    modifier = Modifier.semantics {
        role = Role.DropdownList
        contentDescription = "Summary Language, ${currentLanguage.name}"
    }
)
```

**Dropdown Item:**
```kotlin
DropdownMenuItem(
    text = { Text(stringResource(language.displayName)) },
    onClick = { /* ... */ },
    modifier = Modifier.semantics {
        role = Role.Option
        selected = (language == currentLanguage)
        contentDescription = if (language == currentLanguage) {
            "Selected, ${stringResource(language.displayName)}"
        } else {
            stringResource(language.displayName)
        }
    }
)
```

### 6.2 Touch Targets

**Minimum Size:** 48dp × 48dp (Material 3 guideline)

**Implementation:**
- DropdownMenuItem handles this automatically
- Text field with trailing icon meets requirement

### 6.3 Color Contrast

**Label:** OnSurfaceVariant on Surface (7.0:1 ratio, AAA)

**Value:** OnSurface on Surface (7.0:1 ratio, AAA)

**Selected Item:** Primary on Surface (4.5:1 ratio, AA)

**All meet WCAG AA standards**

### 6.4 Keyboard Support

**Navigation:**
- Tab: Navigate to dropdown
- Enter/Space: Expand dropdown
- Arrow keys: Navigate options
- Enter: Select option
- Escape: Close dropdown

---

## 7. Animation and Transitions

### 7.1 Dropdown Expansion

**Duration:** 200ms (Material 3 standard)

**Easing:** Standard (Fast Out Slow In)

**Scale:** Fade in + slight expand (95% → 100%)

### 7.2 Selection Animation

**Duration:** 100ms (instant feedback)

**Visual Change:**
- Checkmark fades in
- Text color changes to primary

### 7.3 Dialog Open/Close

**Duration:** 300ms (Material 3 standard)

**Easing:** Decelerate

---

## 8. Error States

### 8.1 No Error States

This component has no error conditions:
- Selection is always valid (enum values)
- No network calls involved
- No validation needed

### 8.2 Edge State: First Launch

**Scenario:** User opens AI Settings for first time

**Behavior:**
- Dropdown shows "Auto-detect" (default)
- No special "not set" state needed
- Clear default value

---

## 9. Localization

### 9.1 Language Display Names

**English (default):**
```
Auto-detect
English
Chinese
Spanish
French
German
Japanese
Korean
Portuguese
Russian
Arabic
Hindi
```

**Spanish (example):**
```
Detección automática
Inglés
Chino
Español
Francés
Alemán
Japonés
Coreano
Portugués
Ruso
Árabe
Hindi
```

**Chinese (example):**
```
自动检测
英语
中文
西班牙语
法语
德语
日语
韩语
葡萄牙语
俄语
阿拉伯语
印地语
```

### 9.2 RTL Language Support

**Arabic Layout:**
```
┌─────────────────────────────────────────────┐
│ ﺔﻐﺘﻟﺎﺑ   ﺔﻐﺗﺺﺧﻟﺍ              ◄│
└─────────────────────────────────────────────┘
```

**Implementation:**
- Compose handles RTL automatically
- Dropdown alignment follows system locale
- Checkmark position mirrors in RTL

---

## 10. Component Code Structure

### 10.1 Composable Function Signature

```kotlin
@Composable
fun SummaryLanguageSelector(
    currentLanguage: SummaryLanguage,
    onLanguageChange: (SummaryLanguage) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 10.2 State Management

```kotlin
@Composable
fun SummaryLanguageSelector(
    currentLanguage: SummaryLanguage,
    onLanguageChange: (SummaryLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        // TextField implementation
        // DropdownMenu implementation
    }
}
```

### 10.3 Integration Point

**In AIProviderSectionEdit:**

```kotlin
@Composable
fun AIProviderSectionEdit(
    state: AISettingsState,
    current: AISettings,
    onEvent: (AISettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ... existing fields ...

        // NEW: Summary Language Selector
        val currentLanguage by repository.summaryLanguage.collectAsStateWithLifecycle()

        SummaryLanguageSelector(
            currentLanguage = currentLanguage,
            onLanguageChange = { language ->
                onEvent(AISettingsEvent.SetSummaryLanguage(language))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // ... rest of fields ...
    }
}
```

---

## 11. Design Variations

### 11.1 Alternative: Radio Buttons

**Considered but Rejected:**

**Layout:**
```
Summary Language
  ○ Auto-detect
  ○ English
  ○ Spanish
  ...
```

**Pros:**
- All options visible
- No tap to expand

**Cons:**
- Takes more screen space
- 12 options is too many for radio buttons
- Harder to scan
- Doesn't match existing patterns

**Decision:** Dropdown is better for 12 options

### 11.2 Alternative: Slider

**Considered but Rejected:**

**Layout:**
```
Summary Language
[Auto-detect | English | Chinese | ...]
```

**Pros:**
- All options visible

**Cons:**
- Not appropriate for discrete choices
- Hard to implement with enum
- Not Material 3 pattern

**Decision:** Dropdown is the standard pattern

---

## 12. Usability Considerations

### 12.1 Learning Curve

**Zero Learning Required:**
- Dropdown is a standard UI pattern
- Users immediately understand it
- No explanation needed

### 12.2 Error Prevention

**No Errors Possible:**
- Selection always valid
- No invalid input
- No validation needed

### 12.3 Efficiency

**Task: Change Summary Language to Spanish**

**Steps:**
1. Open AI Settings (1 tap)
2. Tap language dropdown (1 tap)
3. Tap "Spanish" (1 tap)
4. Tap Save (1 tap)

**Total:** 4 taps, ~5 seconds

---

## 13. Visual Mockup (ASCII)

### 13.1 Settings Dialog - Collapsed

```
┌─────────────────────────────────────────────────┐
│ AI Settings                                    │
│                                  [─] [✕]       │
├─────────────────────────────────────────────────┤
│                                                 │
│ Configure your AI provider settings...          │
│                                                 │
│ Provider:                    [OpenAI ▼]         │
│                                                 │
│ API Key:                      [•••••••••••]    │
│                                                 │
│ Model ID:              [gpt-4o-mini         ▼] │
│                                                 │
│ URL:                   [https://api.opena...] │
│                                                 │
│ Timeout:               [60 seconds]            │
│                                                 │
│ Summary Language:      [Auto-detect         ▼] │ ← NEW
│                                                 │
│ ┌─────────┐                                [🔄] │
│ │  Save   │                                     │
│ └─────────┘                                     │
└─────────────────────────────────────────────────┘
```

### 13.2 Settings Dialog - Dropdown Expanded

```
┌─────────────────────────────────────────────────┐
│ AI Settings                                    │
│                                  [─] [✕]       │
├─────────────────────────────────────────────────┤
│                                                 │
│ Configure your AI provider settings...          │
│                                                 │
│ ... (other fields above)                        │
│                                                 │
│ Summary Language:      [Auto-detect         ▲] │
│                                                 │
│         ┌───────────────────────────────────┐  │
│         │ ✓ Auto-detect                    │  │
│         │   English                        │  │
│         │   Chinese                        │  │
│         │   Spanish                        │  │
│         │   French                         │  │
│         │   German                         │  │
│         │   Japanese                       │  │
│         │   Korean                         │  │
│         │   Portuguese                     │  │
│         │   Russian                        │  │
│         │   Arabic                         │  │
│         │   Hindi                          │  │
│         └───────────────────────────────────┘  │
│                                                 │
│ ... (other fields below - scrollable)           │
└─────────────────────────────────────────────────┘
```

---

## 14. Implementation Checklist

### 14.1 UI Components
- [ ] Create `SummaryLanguageSelector` composable
- [ ] Add to `AIProviderSectionEdit` layout
- [ ] Connect to SettingsViewModel
- [ ] Add AISettingsEvent.SetSummaryLanguage

### 14.2 String Resources
- [ ] Add summary_language_title
- [ ] Add summary_language_description
- [ ] Add 12 language display name strings

### 14.3 Accessibility
- [ ] Add semantics for screen readers
- [ ] Test with TalkBack enabled
- [ ] Verify touch targets
- [ ] Test keyboard navigation

### 14.4 Testing
- [ ] Test dropdown expansion/collapse
- [ ] Test selection updates
- [ ] Test state persistence
- [ ] Test all 12 languages
- [ ] Test RTL layouts (Arabic)
- [ ] Test accessibility

---

## 15. Design Review Checklist

### Visual Design
- [ ] Follows Material 3 guidelines
- [ ] Matches existing AI settings UI
- [ ] Proper spacing and alignment
- [ ] Consistent typography
- [ ] Proper color usage

### Interaction Design
- [ ] Clear feedback on selection
- [ ] Smooth animations
- [ ] No janky transitions
- [ ] Proper state management

### Usability
- [ ] Easy to understand
- [ ] Easy to use
- [ ] No errors possible
- [ ] Efficient (minimal taps)

### Accessibility
- [ ] Screen reader support
- [ ] Touch targets adequate
- [ ] Color contrast sufficient
- [ ] Keyboard navigation

---

## 16. Conclusion

This UI/UX design provides:
- ✅ Intuitive language selection
- ✅ Consistent with existing patterns
- ✅ Accessible to all users
- ✅ Efficient and fast
- ✅ No learning curve
- ✅ Clear feedback

**Next Steps:**
1. Implement composable (Phase 8)
2. Create string resources (Phase 8)
3. Test accessibility (Phase 8)
4. Verify usability (Phase 8 QA)

---

**Last Updated:** 2026-01-01 09:53:49
**Next Phase:** Phase 6 (Specification Writing)
