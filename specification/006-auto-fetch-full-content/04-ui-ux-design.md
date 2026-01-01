# UI/UX Design Specification - Auto Fetch Full Article Feature

**Feature ID:** 006
**Design Date:** 2026-01-01
**Designer:** Super Dev UI/UX Designer
**Platform:** Android / Jetpack Compose / Material3

## Executive Summary

This document specifies the UI/UX design for the "Auto Fetch Full Article" feature in Feeder. The design follows Material3 guidelines and maintains consistency with existing settings UI patterns in the application.

## Table of Contents
1. [Design Principles](#1-design-principles)
2. [Settings Screen Design](#2-settings-screen-design)
3. [Article Screen Behavior](#3-article-screen-behavior)
4. [Accessibility Design](#4-accessibility-design)
5. [Visual Specifications](#5-visual-specifications)
6. [Interaction Design](#6-interaction-design)
7. [State Management](#7-state-management)
8. [Localization](#8-localization)

---

## 1. Design Principles

### Core Principles
1. **Consistency** - Match existing settings UI patterns
2. **Clarity** - Clear label and description
3. **User Control** - Easy to enable/disable
4. **Performance** - No UI blocking during fetch
5. **Accessibility** - Screen reader friendly

### Design Guidelines Followed
- Material3 Design System
- Android Accessibility Guidelines
- Feeder Design System (existing)

---

## 2. Settings Screen Design

### Placement
**Location:** Settings → Syncing section

**Rationale:**
- Groups with network/sync related settings
- Near "Sync on WiFi only" and "Sync only when charging"
- Logical grouping for data-conscious users

### Visual Layout

```
┌─────────────────────────────────────────────┐
│ Settings                        [← Back]    │
├─────────────────────────────────────────────┤
│                                             │
│ SYNCING                                     │
│ ┌─────────────────────────────────────────┐ │
│ │ Sync on resume              [TOGGLE]   │ │
│ │ ─────────────────────────────────────  │ │
│ │ Sync only on Wi-Fi           [TOGGLE]   │ │
│ │ ─────────────────────────────────────  │ │
│ │ Sync only when charging      [TOGGLE]   │ │
│ │ ─────────────────────────────────────  │ │
│ │ Auto Fetch Full Article      [TOGGLE]   │ │ ← NEW
│ │ Automatically fetch full...            │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ...other settings sections...               │
└─────────────────────────────────────────────┘
```

### Component Specifications

#### Toggle Row
```kotlin
@Composable
fun AutoFetchToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Minimum touch target
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and Description Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.setting_auto_fetch_full_article),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.setting_auto_fetch_full_article_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Switch
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null, // Null to prevent double toggle
            modifier = Modifier.semantics {
                contentDescription = "Auto fetch full article, ${
                    if (checked) "enabled" else "disabled"
                }"
            }
        )
    }
}
```

### Typography

#### Title Text
- **Font:** Roboto (system default)
- **Style:** MaterialTheme.typography.bodyLarge
- **Size:** 16sp
- **Weight:** Regular (400)
- **Color:** onSurface (high emphasis)

#### Description Text
- **Style:** MaterialTheme.typography.bodySmall
- **Size:** 14sp
- **Weight:** Regular (400)
- **Color:** onSurfaceVariant (medium emphasis)

---

## 3. Article Screen Behavior

### Loading States

#### State 1: Article Opens (Auto-Fetch Enabled)
```
┌─────────────────────────────────────────────┐
│ Article Title                  [⋮] [↻] [📄]│
├─────────────────────────────────────────────┤
│                                             │
│ Article content visible immediately...      │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Loading full article...               │ │ ← Auto-fetch indicator
│ │                                        │ │
│ │         [Circular Progress Indicator]  │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ (Cached content shows while loading)        │
└─────────────────────────────────────────────┘
```

#### State 2: Full Text Loaded
```
┌─────────────────────────────────────────────┐
│ Article Title                  [⋮] [↻] [📄]│
├─────────────────────────────────────────────┤
│                                             │
│ Full article content displayed...           │
│                                             │
│ (Original article link replaced with        │ │
│  full text content)                         │
│                                             │
└─────────────────────────────────────────────┘
```

#### State 3: Fetch Failed
```
┌─────────────────────────────────────────────┐
│ Article Title                  [⋮] [↻] [📄]│
├─────────────────────────────────────────────┤
│                                             │
│ Failed to fetch full article                │
│                                             │
│ [Try Again] button or show original link    │
└─────────────────────────────────────────────┘
```

### Loading Indicator Design

#### Circular Progress Indicator
```kotlin
@Composable
fun FullTextLoadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fetching_full_article),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### Behavior Matrix

| User Action | Auto-Fetch ON | Auto-Fetch OFF |
|-------------|---------------|----------------|
| Open article | Auto-fetch starts | Show cached content only |
| Tap fetch button | Re-fetch full text | Fetch full text |
| Navigate away | Cancel pending fetch | N/A |
| Return to article | Show cached or fetched | Show cached content |

---

## 4. Accessibility Design

### Screen Reader Support

#### Switch Semantics
```kotlin
Switch(
    checked = checked,
    onCheckedChange = null,
    modifier = Modifier.semantics {
        // Content description
        contentDescription = "Auto fetch full article"

        // State for announcement
        stateDescription = if (checked) "Enabled" else "Disabled"

        // Role identification
        role = Role.Switch

        // Click label
        onClickLabel = "Toggle auto fetch"
    }
)
```

#### Row Semantics (Alternative)
```kotlin
Row(
    modifier = Modifier
        .clearAndSetSemantics {
            contentDescription = "Auto fetch full article, ${
                if (checked) "enabled" else "disabled"
            }. Double tap to toggle."
            stateDescription = if (checked) "On" else "Off"
            toggleableState = if (checked) {
                ToggleableState.On
            } else {
                ToggleableState.Off
            }
        }
)
```

### Touch Targets
- **Minimum size:** 48x48 dp (Android guideline)
- **Row height:** 64 dp (comfortable touch)
- **Switch padding:** 8 dp around switch

### Color Contrast
- **Title:** 4.5:1 contrast ratio minimum
- **Description:** 4.5:1 contrast ratio minimum
- **Handle via Material3:** Automatic with theme

### Keyboard Navigation
- **D-pad navigation:** Row must be focusable
- **Enter/Space:** Toggle switch
- **Implemented via:** `modifier.toggleable()`

---

## 5. Visual Specifications

### Colors (Material3 Theme)

#### Light Mode
```kotlin
// Title
color = MaterialTheme.colorScheme.onSurface // Black/900

// Description
color = MaterialTheme.colorScheme.onSurfaceVariant // Gray/600

// Switch Track
checkedTrackColor = MaterialTheme.colorScheme.primary // Primary color
uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant // Gray/200

// Switch Thumb
checkedThumbColor = MaterialTheme.colorScheme.onPrimary // White
uncheckedThumbColor = MaterialTheme.colorScheme.outline // Gray/400
```

#### Dark Mode
```kotlin
// Title
color = MaterialTheme.colorScheme.onSurface // White/100

// Description
color = MaterialTheme.colorScheme.onSurfaceVariant // Gray/400

// Switch Track
checkedTrackColor = MaterialTheme.colorScheme.primary // Primary color
uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant // Gray/700

// Switch Thumb
checkedThumbColor = MaterialTheme.colorScheme.onPrimary // Black/900
uncheckedThumbColor = MaterialTheme.colorScheme.outline // Gray/600
```

### Spacing

```kotlin
// Screen padding
Padding(horizontal = 16.dp)

// Row padding
Padding(horizontal = 16.dp, vertical = 16.dp)

// Space between text columns and switch
Spacer(width = 16.dp)

// Space between title and description
Spacer(height = 4.dp)

// Space between sections
Spacer(height = 8.dp)
```

### Icons (Not Used)

This feature doesn't require icons - uses standard Material3 Switch component.

---

## 6. Interaction Design

### Toggle Interaction

#### User Tap
```
1. User taps switch or entire row
2. Switch animates to new position
3. Haptic feedback (optional)
4. Setting persists to SharedPreferences
5. StateFlow updates for reactive UI
6. Screen reader announces new state
```

#### Animation
```kotlin
// Material3 Switch handles animation automatically
// Duration: 200ms (default)
// Easing: Standard easing curve
```

#### Haptic Feedback (Optional)
```kotlin
// In onCheckedChange
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    // Provide haptic feedback on toggle
    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}
```

### Article Opening Interaction

#### With Auto-Fetch ON
```
1. User taps article in feed
2. ArticleScreen opens
3. Cached content displays immediately
4. ArticleViewModel.init() checks setting
5. Auto-fetch starts in background
6. Loading indicator appears
7. Content updates when ready
```

#### With Auto-Fetch OFF
```
1. User taps article in feed
2. ArticleScreen opens
3. Cached content displays
4. User taps "Fetch Full Article" button
5. Full text fetches
6. Content updates
```

### Performance Interaction

#### Non-Blocking UI
```
UI Thread:
  - Show article immediately
  - Show loading indicator
  - Handle user interaction

Background Thread:
  - Fetch full text
  - Parse content
  - Update UI when ready
```

---

## 7. State Management

### Settings State Flow
```
SharedPreferences (Persistent)
       ↓
SettingsStore._autoFetchFullArticle (MutableStateFlow<Boolean>)
       ↓
SettingsStore.autoFetchFullArticle (StateFlow<Boolean>) - Read-only
       ↓
SettingsViewModel exposes to UI
       ↓
SettingsScreen collects as state
```

### Article State Flow
```
Article Opens
       ↓
ArticleViewModel.init()
       ↓
Check SettingsStore.autoFetchFullArticle
       ↓
If true AND article not fetched:
    → toggleFullText()
    → Show loading state
    → Parse full text
    → Update to content state
```

### State Diagram
```
[Article Open]
     ↓
[Check Setting]
     ↓
[Setting ON?] ──No──→ [Show Cached Content]
     ↓ Yes
[Article Has Full Text?] ──Yes──→ [Show Full Text]
     ↓ No
[Auto-Fetch] ──Success──→ [Show Full Text]
     │
     └──Failure──→ [Show Error + Try Again]
```

---

## 8. Localization

### String Resources

#### English (Default)
```xml
<!-- strings.xml -->
<string name="setting_auto_fetch_full_article">Auto Fetch Full Article</string>
<string name="setting_auto_fetch_full_article_description">Automatically fetch full article text when opening articles</string>
```

#### Accessibility Strings
```xml
<!-- Existing strings reused -->
<string name="fetch_full_article">Fetch Full Article</string>
<string name="fetching_full_article">Fetching full article…</string>
<string name="failed_to_fetch_full_article">Failed to fetch full article</string>
```

#### Supported Languages
Feeder supports multiple languages - translations needed for:
- English (en) ✅ DEFAULT
- German (de)
- Spanish (es)
- French (fr)
- Italian (it)
- Japanese (ja)
- Korean (ko)
- Others (check project)

**Note:** Use crowdin or project's localization process

---

## 9. Edge Cases

### EC-1: Network Unavailable
**UI:** Show error message after timeout
**Action:** Allow manual retry
**Message:** "Network unavailable. Tap to retry."

### EC-2: Article Already Has Full Text
**UI:** No loading indicator
**Action:** Show content immediately
**Logic:** Check `article.fullTextByDefault` flag

### EC-3: User Navigates Away During Fetch
**UI:** Cancel pending fetch
**Action:** Clean up coroutine
**Logic:** `viewModelScope` auto-cancels

### EC-4: Rapid Article Navigation
**UI:** Cancel previous fetch
**Action:** Start new fetch
**Logic:** Check current article ID

### EC-5: Setting Changed While Article Open
**UI:** No immediate effect
**Action:** Applies to next article
**Logic:** Setting checked on article open, not live

---

## 10. Responsive Design

### Screen Size Support

#### Phone (Portrait)
```
┌─────────────────────┐
│ Title         [⋮][↻]│
├─────────────────────┤
│ Content             │
│ [Loading...]        │
│                     │
└─────────────────────┘
- Full width content
- Standard padding
```

#### Phone (Landscape)
```
┌───────────────────────────────┐
│ Title                  [⋮][↻]│
├───────────────────────────────┤
│ Content (multi-column if...)  │
│ [Loading...]                  │
└───────────────────────────────┘
- May use wider layout
- Adjust padding proportionally
```

#### Tablet
```
┌───────────────────────────────────────────┐
│ Title                              [⋮][↻]│
├───────────────────────────────────────────┤
│ Content (wider margins, maybe 2-col)      │
│ [Loading...]                              │
└───────────────────────────────────────────┘
- Use max-width constraints
- Larger margins
- Potentially multi-column content
```

---

## 11. Design Mockups

### Settings Screen Mockup (Text-Based)

```
╔════════════════════════════════════════════════════╗
║  Settings                               ←          ║
╠════════════════════════════════════════════════════╣
║                                                  ║
║  SYNCING                                         ║
║  ┌────────────────────────────────────────────┐  ║
║  │ Sync on resume                    ○     │  ║
║  │                                            │  ║
║  │ Sync only on Wi-Fi                 ○     │  ║
║  │                                            │  ║
║  │ Sync only when charging            ○     │  ║
║  │                                            │  ║
║  │ Auto Fetch Full Article            ○     │  ║ ← NEW
║  │ Automatically fetch full article...       │  ║
║  └────────────────────────────────────────────┘  ║
║                                                  ║
║  READ                                            ║
║  ┌────────────────────────────────────────────┐  ║
║  │ ...other settings...                       │  ║
║  └────────────────────────────────────────────┘  ║
║                                                  ║
╚════════════════════════════════════════════════════╝

Legend: ○ = Switch (OFF/ON)
```

### Article Screen Mockup (Loading State)

```
╔════════════════════════════════════════════════════╗
║  Article Title                        [⋮] [↻] [📄] ║
╠════════════════════════════════════════════════════╣
║                                                  ║
║  Here is the article content that is already...  ║
║                                                  ║
║  ┌────────────────────────────────────────────┐  ║
║  │                                            │  ║
║  │         Fetching full article...         │  ║
║  │                                            │  ║
║  │              ⏳ (spinner)                  │  ║
║  │                                            │  ║
║  └────────────────────────────────────────────┘  ║
║                                                  ║
║  (Cached content continues below)               ║
║                                                  ║
╚════════════════════════════════════════════════════╝
```

---

## 12. User Flow Diagram

```
[User Opens Settings]
        ↓
[Scrolls to Syncing Section]
        ↓
[Sees "Auto Fetch Full Article" Toggle]
        ↓
[Taps Toggle]
        ↓
[Switch Animates to ON]
        ↓
[Setting Persists]
        ↓
[User Navigates Back]
        ↓
[Opens Article]
        ↓
[Article Opens + Auto-Fetch Starts]
        ↓
[Loading Indicator Shows]
        ↓
[Full Text Fetched + Displayed]
```

---

## 13. Design System Compliance

### Material3 Guidelines
- ✅ Using Material3 Switch component
- ✅ Following Material3 typography scale
- ✅ Using Material3 color scheme
- ✅ Proper touch target sizes
- ✅ Appropriate spacing and padding

### Feeder Design System
- ✅ Matches existing Settings.kt pattern
- ✅ Uses existing color tokens
- ✅ Follows existing spacing conventions
- ✅ Consistent with other sync settings

---

## 14. Validation Checklist

### Design Validation
- [ ] Toggle matches existing settings pattern
- [ ] Touch targets meet minimum size (48dp)
- [ ] Colors have sufficient contrast (4.5:1)
- [ ] Screen reader announcements correct
- [ ] Animation smooth (200ms)
- [ ] No UI blocking during fetch
- [ ] Error states handled gracefully
- [ ] Network constraints respected

### User Experience Validation
- [ ] Setting easy to find (Syncing section)
- [ ] Label clear and understandable
- [ ] Description provides context
- [ ] Default value (OFF) safe
- [ ] Toggle behavior immediate
- [ ] Article opening not delayed
- [ ] Manual fetch still works
- [ ] Can disable if undesired

---

**Design Specification Complete:** ✅
**Ready for Implementation:** ✅
**Design Approval Needed:** ⏳

---

**Document Version:** 1.0
**Last Updated:** 2026-01-01
**Next Phase:** Technical Specification & Implementation Plan
