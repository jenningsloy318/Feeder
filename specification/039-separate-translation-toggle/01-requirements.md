# 039 - Separate Translation Toggle

## Summary

Currently, the "Enable Auto Translation" toggle controls only automatic translation on article open; the manual translate button in the article toolbar is always visible when the AI provider is configured. There is no master toggle to disable all translation features entirely. This change introduces a new master toggle ("Enable Translation") that controls whether any translation functionality is available, while the existing toggle is renamed to "Auto Translation" and controls only the automatic translation behavior. This separation allows users to use manual translation without auto-translation, disable all translation features entirely, or keep the existing full auto-translation behavior.

---

## Functional Requirements

### FR-001: New "Enable Translation" Master Toggle

A new boolean setting "Enable Translation" shall be added to the AI settings section. This toggle acts as the master switch for all translation-related functionality in the application.

- **Default value**: `true` (enabled) -- preserves existing behavior for users who already have AI configured
- **Persistence**: Stored in `SettingsStore` alongside other AI settings, accessible via Flow-based API
- **Location in UI**: Displayed in the AI settings screen, positioned above the existing "Auto Translation" toggle (within the translation settings section)

### FR-002: "Enable Translation" ON Behavior

When "Enable Translation" is ON:

- The translate button in the article view toolbar shall be visible and functional
- The user can manually trigger translation by tapping the translate button
- The "Auto Translation" toggle in settings shall be enabled (interactive, not greyed out)
- `CircleProgressIconButton` progress indicators for translation shall function normally
- Translation cancellation via the stop button shall function normally
- Per-feed `translateOnOpen` overrides shall function normally

### FR-003: "Enable Translation" OFF Behavior

When "Enable Translation" is OFF:

- The translate button in the article view toolbar shall be hidden (not visible)
- No translation requests shall be sent to the AI provider
- The "Auto Translation" toggle in settings shall be visually greyed out / disabled (non-interactive)
- The "Auto Translation" toggle's stored value shall be preserved (not reset) so that re-enabling "Enable Translation" restores the previous auto-translation preference
- No cancellation of in-progress translation when toggling OFF (low-priority edge case; the button disappears, but the request completes silently)

### FR-004: "Auto Translation" Toggle Dependency

The existing "Enable Auto Translation" toggle is renamed to "Auto Translation" and its behavior is refined:

- "Auto Translation" can only be toggled when "Enable Translation" is ON
- When "Auto Translation" is ON **and** "Enable Translation" is ON: articles are automatically translated when opened in article view (existing behavior preserved)
- When "Auto Translation" is OFF **and** "Enable Translation" is ON: articles are NOT auto-translated, but the user can manually tap the translate button
- When "Enable Translation" is OFF: "Auto Translation" state is irrelevant; no translation functionality is available regardless of its value

### FR-005: "Enable Translation" Gates Per-Feed translateOnOpen

The `enableTranslation` setting shall gate both the global auto-translate AND per-feed `translateOnOpen` overrides:

- When "Enable Translation" is OFF, per-feed `translateOnOpen` shall have no effect (no auto-translation regardless of per-feed settings)
- When "Enable Translation" is ON, per-feed `translateOnOpen` functions as before

### FR-006: Settings UI Presentation

- "Enable Translation" toggle shall appear before (above) "Auto Translation" in the translation settings section
- When "Enable Translation" is OFF, the "Auto Translation" row shall appear greyed out with reduced opacity (0.38f alpha, per Material 3 standard) and shall not respond to tap/click events
- The visual hierarchy shall clearly communicate the dependency relationship between the two toggles
- Language selection and timeout settings shall remain interactive regardless of "Enable Translation" state (these configure how translations behave when eventually re-enabled)

### FR-007: OPML Import/Export Support

- The new `enableTranslation` setting shall be included in OPML export (automatic via `UserSettings.entries` enumeration)
- OPML import shall recognize and restore the `enableTranslation` setting
- The `SETTING_ENABLE_TRANSLATION` enum entry shall be handled in the `OPMLImporter.kt` import `when` block

### FR-008: String Resources

- New string resource `enable_translation_title` with value "Enable Translation"
- New string resource `enable_translation_description` with value "Enable AI translation functionality"
- Existing `translate_enabled_title` renamed from "Enable Auto Translation" to "Auto Translation"
- Existing `translate_enabled_description` updated to reflect that it controls only auto-translation behavior

---

## Non-Functional Requirements

### NFR-001: Backward Compatibility

- Existing users upgrading shall experience no change in behavior. If they previously had auto-translation enabled, both "Enable Translation" and "Auto Translation" shall default to ON
- The new setting shall not require a migration; it defaults to `true`
- The translate button visibility is unchanged for users who do not modify the new toggle

### NFR-002: Minimal Code Impact

- Changes shall be localized to settings storage, settings UI, the `ArticleViewModel` translation trigger logic, and `ArticleScreen` translate button visibility
- No changes to `AIClient`, `AIApi`, `OpenAICompatibleClient`, `AnthropicClient`, or the translation parsing/coordination layer
- No changes to `ParagraphTranslationCoordinator`, `TranslatableTextExtractor`, `TranslationPromptBuilder`, or `InlineTagParser`
- No changes to summary functionality
- No changes to the translation cache (`TranslationBlob`)

### NFR-003: Performance

- Adding the new toggle shall not introduce additional network calls, background tasks, or observable performance impact

### NFR-004: Testability

- The new setting shall be observable via Flow so that unit tests can verify behavior
- The conditional logic for showing/hiding the translate button and gating auto-translation shall be testable in isolation

### NFR-005: Pattern Consistency

- The implementation shall follow the exact same pattern as spec-038's `enableSummary` implementation:
  - `SettingsStore`: `_enableTranslation` MutableStateFlow + `PREF_ENABLE_TRANSLATION` constant + `SETTING_ENABLE_TRANSLATION` enum entry
  - `Repository`: pass-through proxy property + setter
  - ViewModel: exposed StateFlow + setter method
  - Settings screen: new toggle above auto-translation with `enabled = enableTranslation` on the auto toggle
  - `ArticleViewModel`: `showTranslate = enableTranslation && aiValid` (replacing current `showTranslate = aiValid`)
  - `ArticleViewModel.init`: `enableTranslation &&` prepended to auto-translation condition

---

## Acceptance Criteria

- **AC-001: Manual Translation with Auto-Translation Disabled**
  - **Given** "Enable Translation" is ON and "Auto Translation" is OFF
  - **When** the user opens an article
  - **Then** the article is NOT automatically translated
  - **And** the translate button is visible and functional in the toolbar
  - **And** tapping the translate button triggers translation with progress indicator

- **AC-002: Full Auto-Translation**
  - **Given** "Enable Translation" is ON and "Auto Translation" is ON
  - **When** the user opens an article
  - **Then** the article is automatically translated (existing behavior preserved)

- **AC-003: All Translation Disabled**
  - **Given** "Enable Translation" is OFF
  - **When** the user opens an article
  - **Then** the article is NOT automatically translated
  - **And** the translate button is hidden in the toolbar
  - **And** there is no way to trigger translation from the article view

- **AC-004: Auto-Translation Toggle Disabled State**
  - **Given** "Enable Translation" is OFF
  - **When** the user opens translation settings
  - **Then** the "Auto Translation" toggle appears greyed out / disabled
  - **And** tapping the "Auto Translation" toggle has no effect
  - **And** language and timeout settings remain interactive

- **AC-005: Auto-Translation Toggle Enabled State**
  - **Given** "Enable Translation" is ON
  - **When** the user opens translation settings
  - **Then** the "Auto Translation" toggle is fully interactive
  - **And** the user can turn "Auto Translation" ON or OFF

- **AC-006: Preserved Auto-Translation Preference**
  - **Given** "Enable Translation" is ON and "Auto Translation" is ON
  - **When** the user turns "Enable Translation" OFF and then back ON
  - **Then** "Auto Translation" is still ON (the stored value was preserved)

- **AC-007: Settings Visual Hierarchy**
  - **Given** the user opens translation settings
  - **Then** "Enable Translation" appears above "Auto Translation"
  - **And** the dependency relationship is visually clear

- **AC-008: Per-Feed Override Gated**
  - **Given** "Enable Translation" is OFF
  - **And** a specific feed has `translateOnOpen` set to `true`
  - **When** the user opens an article from that feed
  - **Then** the article is NOT automatically translated

- **AC-009: OPML Round-Trip**
  - **Given** "Enable Translation" is set to a non-default value (e.g., `false`)
  - **When** the user exports settings to OPML and imports them on a fresh install
  - **Then** the "Enable Translation" setting is correctly restored

---

## Settings Behavior Matrix

| Enable Translation | Auto Translation | Translate Button (Article View) | Auto-Translate on Open | Per-Feed translateOnOpen | Auto Translation Toggle (Settings) |
|:------------------:|:----------------:|:-------------------------------:|:----------------------:|:------------------------:|:-----------------------------------:|
| ON                 | ON               | Visible, functional             | Yes                    | Functional               | Enabled, interactive                |
| ON                 | OFF              | Visible, functional             | No                     | Functional               | Enabled, interactive                |
| OFF                | ON (stored)      | Hidden                          | No                     | No effect                | Greyed out, non-interactive         |
| OFF                | OFF (stored)     | Hidden                          | No                     | No effect                | Greyed out, non-interactive         |

---

## Out of Scope

- **Summary functionality**: No changes to summary toggles or behavior
- **Per-feed translation settings**: This spec covers a global toggle only; per-feed `translateOnOpen` behavior is gated but not modified
- **AI provider configuration**: No changes to provider selection, API key management, or model settings
- **Translation cache behavior**: Cached translations remain accessible regardless of toggle state; this spec only gates new translation requests
- **Translation coordination layer**: No changes to `ParagraphTranslationCoordinator`, `TranslatableTextExtractor`, `TranslationPromptBuilder`, or `InlineTagParser`
- **Prompt or model changes**: No changes to translation prompts, response parsing, or AI client implementations
- **i18n of new strings**: Only English strings are added in this spec; community translations follow the standard localization process
