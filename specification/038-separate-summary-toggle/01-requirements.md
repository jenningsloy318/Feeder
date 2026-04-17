# 038 - Separate Summary Toggle

## Summary

Currently, disabling the "Auto Summary" toggle disables all summary functionality, including manual summarization via the toolbar button in article view. This change introduces a new master toggle ("Enable Summary") that controls whether summary functionality is available at all, while the existing "Auto Summary" toggle controls only the automatic summarization behavior. This separation allows users to use manual summarization without auto-summary, or disable all summary features entirely.

---

## Functional Requirements

### FR-001: New "Enable Summary" Master Toggle

A new boolean setting "Enable Summary" shall be added to the AI settings section. This toggle acts as the master switch for all summary-related functionality in the application.

- **Default value**: `true` (enabled) -- preserves existing behavior for users who already have AI configured
- **Persistence**: Stored in `SettingsStore` alongside other AI settings, accessible via Flow-based API
- **Location in UI**: Displayed in the AI settings screen, positioned above the existing "Auto Summary" toggle

### FR-002: "Enable Summary" ON Behavior

When "Enable Summary" is ON:

- The summarize button in the article view toolbar shall be visible and functional
- The user can manually trigger summarization by tapping the summarize button
- The "Auto Summary" toggle in settings shall be enabled (interactive, not greyed out)
- `CircleProgressIconButton` progress indicators for summary shall function normally
- Summary cancellation via the stop button shall function normally

### FR-003: "Enable Summary" OFF Behavior

When "Enable Summary" is OFF:

- The summarize button in the article view toolbar shall be hidden or visually disabled (non-functional)
- No summary requests shall be sent to the AI provider
- The "Auto Summary" toggle in settings shall be visually greyed out / disabled (non-interactive)
- The "Auto Summary" toggle's stored value shall be preserved (not reset) so that re-enabling "Enable Summary" restores the previous auto-summary preference
- Any in-progress summary shall be cancelled if the user disables "Enable Summary" mid-operation (edge case, low priority)

### FR-004: "Auto Summary" Toggle Dependency

The existing "Auto Summary" toggle behavior is refined:

- "Auto Summary" can only be toggled when "Enable Summary" is ON
- When "Auto Summary" is ON **and** "Enable Summary" is ON: articles are automatically summarized when opened in article view
- When "Auto Summary" is OFF **and** "Enable Summary" is ON: articles are NOT auto-summarized, but the user can manually tap the summarize button
- When "Enable Summary" is OFF: "Auto Summary" state is irrelevant; no summary functionality is available regardless of its value

### FR-005: Settings UI Presentation

- "Enable Summary" toggle shall appear before (above) "Auto Summary" in the settings list
- When "Enable Summary" is OFF, the "Auto Summary" row shall appear greyed out with reduced opacity and shall not respond to tap/click events
- The visual hierarchy shall clearly communicate the dependency relationship between the two toggles

---

## Non-Functional Requirements

### NFR-001: Backward Compatibility

- Existing users upgrading shall experience no change in behavior. If they previously had auto-summary enabled, both "Enable Summary" and "Auto Summary" shall default to ON
- The new setting shall not require a migration; it defaults to `true`

### NFR-002: Minimal Code Impact

- Changes shall be localized to settings storage, settings UI, and the `ArticleViewModel` summary trigger logic
- No changes to `AIClient`, `AIApi`, `OpenAICompatibleClient`, `AnthropicClient`, or the summary response parsing layer
- No changes to translation functionality

### NFR-003: Performance

- Adding the new toggle shall not introduce additional network calls, background tasks, or observable performance impact

### NFR-004: Testability

- The new setting shall be observable via Flow so that unit tests can verify behavior
- The conditional logic for showing/hiding the summarize button and gating auto-summary shall be testable in isolation

---

## Acceptance Criteria

- **AC-001: Manual Summary with Auto-Summary Disabled**
  - **Given** "Enable Summary" is ON and "Auto Summary" is OFF
  - **When** the user opens an article
  - **Then** the article is NOT automatically summarized
  - **And** the summarize button is visible and functional in the toolbar
  - **And** tapping the summarize button triggers summarization with progress indicator

- **AC-002: Full Auto-Summary**
  - **Given** "Enable Summary" is ON and "Auto Summary" is ON
  - **When** the user opens an article
  - **Then** the article is automatically summarized (existing behavior preserved)

- **AC-003: All Summary Disabled**
  - **Given** "Enable Summary" is OFF
  - **When** the user opens an article
  - **Then** the article is NOT automatically summarized
  - **And** the summarize button is hidden or disabled in the toolbar
  - **And** there is no way to trigger summarization from the article view

- **AC-004: Auto-Summary Toggle Disabled State**
  - **Given** "Enable Summary" is OFF
  - **When** the user opens AI settings
  - **Then** the "Auto Summary" toggle appears greyed out / disabled
  - **And** tapping the "Auto Summary" toggle has no effect

- **AC-005: Auto-Summary Toggle Enabled State**
  - **Given** "Enable Summary" is ON
  - **When** the user opens AI settings
  - **Then** the "Auto Summary" toggle is fully interactive
  - **And** the user can turn "Auto Summary" ON or OFF

- **AC-006: Preserved Auto-Summary Preference**
  - **Given** "Enable Summary" is ON and "Auto Summary" is ON
  - **When** the user turns "Enable Summary" OFF and then back ON
  - **Then** "Auto Summary" is still ON (the stored value was preserved)

- **AC-007: Settings Visual Hierarchy**
  - **Given** the user opens AI settings
  - **Then** "Enable Summary" appears above "Auto Summary"
  - **And** the dependency relationship is visually clear

---

## Settings Behavior Matrix

| Enable Summary | Auto Summary | Summarize Button (Article View) | Auto-Summarize on Open | Auto Summary Toggle (Settings) |
|:--------------:|:------------:|:-------------------------------:|:----------------------:|:------------------------------:|
| ON             | ON           | Visible, functional             | Yes                    | Enabled, interactive           |
| ON             | OFF          | Visible, functional             | No                     | Enabled, interactive           |
| OFF            | ON (stored)  | Hidden / disabled               | No                     | Greyed out, non-interactive    |
| OFF            | OFF (stored) | Hidden / disabled               | No                     | Greyed out, non-interactive    |

---

## Out of Scope

- **Translation functionality**: No changes to translation toggles or behavior
- **Per-feed summary settings**: This spec covers a global toggle only
- **AI provider configuration**: No changes to provider selection, API key management, or model settings
- **Summary cache behavior**: Cached summaries remain accessible regardless of toggle state; this spec only gates new summary requests
- **Blocklist / summary filtering**: The existing "apply blocklist to summaries" setting is unaffected
- **Prompt or model changes**: No changes to summary prompts, response parsing, or AI client implementations
