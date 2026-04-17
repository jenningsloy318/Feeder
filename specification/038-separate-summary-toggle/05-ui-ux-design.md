# 038 - Separate Summary Toggle: UI/UX Design Spec

---

## 1. Summary Settings Screen

### 1.1 Layout: Enable Summary ON

```
+----------------------------------------------------------+
|  [<] Summary Settings                                    |
+----------------------------------------------------------+
|                                                          |
|  [ ]  Enable Summary                          [===ON===] |
|       Enable AI summary functionality                    |
|                                                          |
|  [ ]  Auto Summary                            [===ON===] |
|       Automatically summarize articles when opened       |
|                                                          |
|  [ ]  Summary Language                                   |
|       English                                            |
|                                                          |
|  [ ]  Summary Timeout                          [-] 60 [+]|
|       Maximum time to wait for AI summary                |
|                                                          |
+----------------------------------------------------------+
```

All rows are fully interactive. Both toggles appear at full opacity (1.0).

### 1.2 Layout: Enable Summary OFF

```
+----------------------------------------------------------+
|  [<] Summary Settings                                    |
+----------------------------------------------------------+
|                                                          |
|  [ ]  Enable Summary                          [==OFF===] |
|       Enable AI summary functionality                    |
|                                                          |
|  [ ]  Auto Summary  (greyed out, 38% opacity) [==OFF===] |
|       Automatically summarize articles when opened       |
|                                                          |
|  [ ]  Summary Language                                   |
|       English                                            |
|                                                          |
|  [ ]  Summary Timeout                          [-] 60 [+]|
|       Maximum time to wait for AI summary                |
|                                                          |
+----------------------------------------------------------+
```

The "Auto Summary" row (title, description, and switch) renders at 38% opacity. The switch shows its disabled visual state. Tapping the row has no effect.

Language and timeout settings remain interactive -- they configure how summaries behave when eventually enabled again.

### 1.3 Visual Hierarchy

No cards, indentation, or section dividers between the two toggles. The dependency is communicated solely through:

1. **Ordering**: "Enable Summary" directly above "Auto Summary"
2. **Disabled state**: 38% opacity + non-interactive switch when master is OFF
3. **Naming**: "Enable Summary" (master) vs "Auto Summary" (sub-behavior)

This matches the flat list pattern used throughout Feeder's settings screens.

---

## 2. Article View Toolbar

### 2.1 Recommendation: Hidden (not disabled)

The summarize button should be **hidden** (removed from toolbar) when Enable Summary is OFF.

**Rationale:**
- Toolbar space is limited, especially on smaller screens with the translate button also present
- A disabled/greyed toolbar icon creates confusion: "why can't I use this?" without immediate context about which setting controls it
- The settings screen already communicates that summary is disabled via the toggle -- the toolbar should reflect this cleanly
- Precedent: the current codebase already uses visibility (`if (viewState.showSummarize)`) not disabled state for the summarize button
- Material 3 guidance favors removing unavailable actions from toolbars rather than showing disabled icons

### 2.2 Layout: Enable Summary ON

```
+----------------------------------------------------------+
|  [<]  Feed Title                    [Summarize][Translate]|
+----------------------------------------------------------+
|                                                          |
|  Article content...                                      |
|                                                          |
+----------------------------------------------------------+
```

Both AI buttons visible. Summarize shows `AutoFixHigh` icon; Translate shows `Translate` icon. Both use `CircleProgressIconButton` with progress/cancel behavior.

### 2.3 Layout: Enable Summary OFF

```
+----------------------------------------------------------+
|  [<]  Feed Title                             [Translate]  |
+----------------------------------------------------------+
|                                                          |
|  Article content...                                      |
|                                                          |
+----------------------------------------------------------+
```

Only the translate button is visible. The summarize button is entirely absent -- no placeholder, no disabled icon.

### 2.4 Translate Button Independence

The translate button visibility is **never** gated on `enableSummary`. It depends only on:
- `aiSettings.isValid` (AI provider configured)
- Article has a non-empty link

This requires splitting the current shared `showSummarize` flag into two independent flags in `ArticleScreenViewState`:
- `showSummarize`: `enableSummary && aiSettings.isValid && hasLink`
- `showTranslate`: `aiSettings.isValid && hasLink` (unchanged logic)

---

## 3. Interaction Design

### 3.1 Toggling "Enable Summary" ON -> OFF

1. Switch animates to OFF position
2. "Auto Summary" row transitions to 38% opacity (no animation needed -- instant state change is fine, matching standard Material 3 `enabled` behavior)
3. "Auto Summary" switch shows disabled visual state, preserving its current checked value
4. In any open article view, the summarize button disappears
5. If a summary was in progress, it gets cancelled (low priority edge case)

### 3.2 Toggling "Enable Summary" OFF -> ON

1. Switch animates to ON position
2. "Auto Summary" row returns to full opacity (1.0)
3. "Auto Summary" switch becomes interactive again, showing its preserved checked state
4. In any open article view, the summarize button reappears
5. If Auto Summary is also ON, newly opened articles will auto-summarize

### 3.3 Toggling "Auto Summary" (when enabled)

Behavior is unchanged from current implementation:
- ON: articles auto-summarize when opened
- OFF: articles do not auto-summarize; manual summarize button still available

### 3.4 Toggling "Auto Summary" (when disabled)

Tap/click is blocked. No state change occurs. No visual feedback (no ripple). The stored value is preserved.

---

## 4. Accessibility

### 4.1 Screen Reader Behavior

**Enable Summary toggle:**
- Role: Switch
- State: "On" / "Off"
- Standard `SwitchSetting` semantics (already implemented)

**Auto Summary toggle (when disabled):**
- Role: Switch  
- State: "Off" (or preserved state)
- The `enabled = false` parameter on both the `clickable` modifier and the `Switch` composable causes TalkBack to announce the element as **disabled**
- No additional `contentDescription` changes needed -- Material 3 Switch handles this natively

### 4.2 Focus Navigation

- Tab/D-pad navigation skips disabled "Auto Summary" row (standard behavior when `enabled = false` on `clickable`)
- All other settings remain focusable and interactive

### 4.3 Touch Target

No changes to touch target sizes. Both rows maintain the existing 64dp minimum height from `SwitchSetting`.

---

## 5. String Content Recommendations

### 5.1 New Strings

| Resource ID | Value | Purpose |
|:---|:---|:---|
| `enable_summary_title` | "Enable Summary" | Master toggle title |
| `enable_summary_description` | "Enable AI summary functionality" | Master toggle description |

### 5.2 Renamed Strings

| Resource ID | Old Value | New Value | Purpose |
|:---|:---|:---|:---|
| `summary_enabled_title` | "Enable Auto Summary" | "Auto Summary" | Shortened to distinguish from master toggle |
| `summary_enabled_description` | "Automatically generate AI summaries for articles" | "Automatically summarize articles when opened" | Clarifies trigger is on article open |

The rename from "Enable Auto Summary" to "Auto Summary" prevents confusion with the new "Enable Summary" master toggle. The word "Enable" in the master toggle title is sufficient to convey the on/off nature.

---

## 6. Implementation Mapping

| Design Element | Implementation Approach |
|:---|:---|
| Master toggle | New `SwitchSetting` in `SummarySettingsScreen` above existing toggle |
| Sub-toggle disabled state | `enabled = enableSummary` parameter on existing `SwitchSetting` |
| 38% opacity on disabled row | `Modifier.alpha(if (enabled) 1f else 0.38f)` on `SwitchSetting` Row (CP-5) |
| Summarize button hidden | `showSummarize` flag gated on `enableSummary` in `ArticleViewModel` (CP-7) |
| Translate button independent | New `showTranslate` flag in `ArticleScreenViewState` (CP-7, R-4) |
| Value preservation | `enabled` controls interactivity, not `checked` -- stored value untouched |
