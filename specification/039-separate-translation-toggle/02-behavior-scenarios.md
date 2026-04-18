# 039 - Separate Translation Toggle: BDD Scenarios

---

## Feature: Separate Translation Toggle

As a user of Feeder,
I want separate controls for enabling translation functionality and auto-translation behavior,
So that I can use manual translation without auto-translation, disable all translation features entirely, or keep the existing full auto-translation behavior.

---

### Behavior Matrix States

#### SCENARIO-001: Enable Translation ON, Auto Translation ON - Full auto-translation (AC-002)

```gherkin
Scenario: Articles are automatically translated when both toggles are ON
  Given "Enable Translation" is ON
  And "Auto Translation" is ON
  And the user has a valid AI provider configured
  When the user opens an article in article view
  Then the article is automatically translated
  And the translate button shows a progress indicator during translation
  And the translation is displayed when complete
```

#### SCENARIO-002: Enable Translation ON, Auto Translation OFF - Manual translation only (AC-001)

```gherkin
Scenario: Articles are not auto-translated but manual translation is available
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF
  When the user opens an article in article view
  Then the article is NOT automatically translated
  And no translation request is sent to the AI provider
  And the translate button is visible in the toolbar
  And the translate button is functional (not greyed out)
```

#### SCENARIO-003: Enable Translation ON, Auto Translation OFF - Manual translation triggers translation (AC-001)

```gherkin
Scenario: User can manually trigger translation when auto-translation is off
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF
  And the user is viewing an article
  When the user taps the translate button in the toolbar
  Then a translation request is sent to the AI provider
  And the translate button shows a CircleProgressIconButton progress indicator
  And the translation is displayed when complete
```

#### SCENARIO-004: Enable Translation OFF, Auto Translation ON (stored) - All translation disabled (AC-003)

```gherkin
Scenario: No translation functionality when Enable Translation is OFF (auto-translation stored as ON)
  Given "Enable Translation" is OFF
  And "Auto Translation" has a stored value of ON
  When the user opens an article in article view
  Then the article is NOT automatically translated
  And the translate button is hidden in the toolbar
  And no translation request is sent to the AI provider
  And there is no way to trigger translation from the article view
```

#### SCENARIO-005: Enable Translation OFF, Auto Translation OFF (stored) - All translation disabled (AC-003)

```gherkin
Scenario: No translation functionality when Enable Translation is OFF (auto-translation stored as OFF)
  Given "Enable Translation" is OFF
  And "Auto Translation" has a stored value of OFF
  When the user opens an article in article view
  Then the article is NOT automatically translated
  And the translate button is hidden in the toolbar
  And no translation request is sent to the AI provider
  And there is no way to trigger translation from the article view
```

---

### Settings UI

#### SCENARIO-006: Auto Translation toggle is disabled when Enable Translation is OFF (AC-004)

```gherkin
Scenario: Auto Translation toggle is greyed out when Enable Translation is OFF
  Given "Enable Translation" is OFF
  When the user opens AI settings
  Then the "Auto Translation" toggle appears greyed out with reduced opacity
  And the "Auto Translation" toggle does not respond to tap events
  And tapping the "Auto Translation" toggle has no effect on its stored value
  And language selection and timeout settings remain interactive
```

#### SCENARIO-007: Auto Translation toggle is interactive when Enable Translation is ON (AC-005)

```gherkin
Scenario: Auto Translation toggle is fully interactive when Enable Translation is ON
  Given "Enable Translation" is ON
  When the user opens AI settings
  Then the "Auto Translation" toggle is fully interactive
  And the "Auto Translation" toggle has normal opacity
  And the user can turn "Auto Translation" ON
  And the user can turn "Auto Translation" OFF
```

#### SCENARIO-008: Settings visual hierarchy - Enable Translation above Auto Translation (AC-007)

```gherkin
Scenario: Enable Translation is positioned above Auto Translation in settings
  Given the user opens AI settings
  Then "Enable Translation" appears above "Auto Translation" in the settings list
  And the dependency relationship between the two toggles is visually clear
  And both toggles are in the translation settings section
```

#### SCENARIO-009: Toggling Auto Translation while Enable Translation is ON (AC-005)

```gherkin
Scenario: User toggles Auto Translation from OFF to ON while Enable Translation is ON
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF
  When the user opens AI settings
  And the user taps the "Auto Translation" toggle to turn it ON
  Then "Auto Translation" is now ON
  And subsequently opening an article triggers automatic translation
```

#### SCENARIO-010: Toggling Auto Translation from ON to OFF while Enable Translation is ON (AC-001, AC-005)

```gherkin
Scenario: User toggles Auto Translation from ON to OFF while Enable Translation is ON
  Given "Enable Translation" is ON
  And "Auto Translation" is ON
  When the user opens AI settings
  And the user taps the "Auto Translation" toggle to turn it OFF
  Then "Auto Translation" is now OFF
  And subsequently opening an article does NOT trigger automatic translation
  And the translate button remains visible and functional in article view
```

---

### Preference Preservation

#### SCENARIO-011: Auto Translation preference preserved across Enable Translation disable/enable cycle (AC-006)

```gherkin
Scenario: Auto Translation ON is preserved when Enable Translation is toggled OFF then ON
  Given "Enable Translation" is ON
  And "Auto Translation" is ON
  When the user turns "Enable Translation" OFF
  Then "Auto Translation" toggle appears greyed out
  And "Auto Translation" stored value remains ON
  When the user turns "Enable Translation" back ON
  Then "Auto Translation" is still ON
  And "Auto Translation" toggle is fully interactive
  And subsequently opening an article triggers automatic translation
```

#### SCENARIO-012: Auto Translation OFF preference preserved across Enable Translation disable/enable cycle (AC-006)

```gherkin
Scenario: Auto Translation OFF is preserved when Enable Translation is toggled OFF then ON
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF
  When the user turns "Enable Translation" OFF
  Then "Auto Translation" toggle appears greyed out
  And "Auto Translation" stored value remains OFF
  When the user turns "Enable Translation" back ON
  Then "Auto Translation" is still OFF
  And "Auto Translation" toggle is fully interactive
  And the translate button is visible but articles are not auto-translated
```

---

### Per-Feed Override

#### SCENARIO-013: Per-feed translateOnOpen gated by Enable Translation OFF (AC-008)

```gherkin
Scenario: Per-feed translateOnOpen has no effect when Enable Translation is OFF
  Given "Enable Translation" is OFF
  And a specific feed has "translateOnOpen" set to true
  When the user opens an article from that feed
  Then the article is NOT automatically translated
  And the translate button is hidden in the toolbar
  And no translation request is sent to the AI provider
```

#### SCENARIO-014: Per-feed translateOnOpen functional when Enable Translation is ON (AC-008)

```gherkin
Scenario: Per-feed translateOnOpen works normally when Enable Translation is ON
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF globally
  And a specific feed has "translateOnOpen" set to true
  When the user opens an article from that feed
  Then the article is automatically translated (per-feed override takes effect)
  And the translate button shows a progress indicator during translation
```

---

### OPML Import/Export

#### SCENARIO-015: OPML export includes Enable Translation setting (AC-009)

```gherkin
Scenario: Enable Translation setting is included in OPML export
  Given "Enable Translation" is set to false (non-default value)
  When the user exports settings to OPML
  Then the exported OPML file contains the "enableTranslation" setting with value "false"
```

#### SCENARIO-016: OPML import restores Enable Translation setting (AC-009)

```gherkin
Scenario: Enable Translation setting is correctly restored from OPML import
  Given "Enable Translation" is set to false
  And the user has exported settings to OPML
  When the user imports the OPML file on a fresh install
  Then "Enable Translation" is restored to false
  And the translate button is hidden in article view
  And the "Auto Translation" toggle is greyed out in settings
```

---

### Edge Cases

#### SCENARIO-017: Toggling Enable Translation OFF does not cancel in-progress translation

```gherkin
Scenario: In-progress translation completes silently when Enable Translation is turned OFF
  Given "Enable Translation" is ON
  And the user has opened an article
  And a translation request is currently in progress (progress indicator is showing)
  When the user navigates to AI settings and turns "Enable Translation" OFF
  Then the in-progress translation request completes silently
  And when returning to the article view the translate button is hidden
```

#### SCENARIO-018: Translation cancellation still works when Enable Translation is ON

```gherkin
Scenario: User can cancel an in-progress translation via the stop button
  Given "Enable Translation" is ON
  And the user has triggered a translation (manual or auto)
  And the translation is in progress with CircleProgressIconButton showing
  When the user taps the stop button on the progress indicator
  Then the translation request is cancelled
  And the translate button returns to its idle state
  And the user can tap the translate button again to retry
```

#### SCENARIO-019: App restart with Enable Translation ON, Auto Translation ON

```gherkin
Scenario: Settings persist across app restart - both toggles ON
  Given "Enable Translation" is ON
  And "Auto Translation" is ON
  When the app is killed and restarted
  Then "Enable Translation" is still ON
  And "Auto Translation" is still ON
  And opening an article triggers automatic translation
```

#### SCENARIO-020: App restart with Enable Translation ON, Auto Translation OFF

```gherkin
Scenario: Settings persist across app restart - Enable Translation ON, Auto Translation OFF
  Given "Enable Translation" is ON
  And "Auto Translation" is OFF
  When the app is killed and restarted
  Then "Enable Translation" is still ON
  And "Auto Translation" is still OFF
  And opening an article does NOT trigger automatic translation
  And the translate button is visible and functional
```

#### SCENARIO-021: App restart with Enable Translation OFF

```gherkin
Scenario: Settings persist across app restart - Enable Translation OFF
  Given "Enable Translation" is OFF
  And "Auto Translation" has a stored value of ON
  When the app is killed and restarted
  Then "Enable Translation" is still OFF
  And the translate button is hidden in article view
  And the "Auto Translation" toggle is greyed out in settings
  And "Auto Translation" stored value is still ON
```

#### SCENARIO-022: New installation defaults (NFR-001)

```gherkin
Scenario: Default values for new installation preserve existing behavior
  Given the app is freshly installed
  And the user has not modified any AI settings
  When the user opens AI settings
  Then "Enable Translation" defaults to ON (true)
  And "Auto Translation" retains its existing default value
  And the user experience is unchanged from before this feature was added
```

#### SCENARIO-023: Existing user upgrade - backward compatibility (NFR-001)

```gherkin
Scenario: Existing users experience no behavior change after upgrade
  Given an existing user has "Auto Translation" set to ON before the upgrade
  When the app is upgraded to a version with the "Enable Translation" toggle
  Then "Enable Translation" defaults to ON (true)
  And "Auto Translation" remains ON
  And the user's existing auto-translation behavior is preserved
  And no migration step is required
```

#### SCENARIO-024: Enable Translation toggle does not affect summary functionality

```gherkin
Scenario: Summary functionality is unaffected by Enable Translation toggle
  Given "Enable Translation" is OFF
  And "Enable Summary" is ON
  When the user opens an article in article view
  Then summary functionality remains fully available
  And the summarize button is visible and functional
  And tapping the summarize button triggers summarization normally
```

#### SCENARIO-025: Cached translations remain accessible regardless of toggle state

```gherkin
Scenario: Previously cached translations are still displayed when Enable Translation is OFF
  Given "Enable Translation" was ON
  And an article was previously translated and the translation is cached
  When the user turns "Enable Translation" OFF
  And the user opens the previously translated article
  Then the cached translation is still accessible and displayed
  And no new translation request is sent to the AI provider
```

#### SCENARIO-026: Rapid toggling of Enable Translation does not cause inconsistent state

```gherkin
Scenario: Rapid toggling of Enable Translation maintains consistent state
  Given "Enable Translation" is ON
  When the user rapidly toggles "Enable Translation" OFF and then ON
  Then the final state of "Enable Translation" is ON
  And the "Auto Translation" toggle is interactive
  And the translate button is visible in article view
  And no duplicate or orphaned translation requests are created
```

---

## Traceability Matrix

| Scenario | Acceptance Criteria | Description |
|----------|-------------------|-------------|
| SCENARIO-001 | AC-002 | Full auto-translation (both toggles ON) |
| SCENARIO-002 | AC-001 | Manual translation available (auto OFF) |
| SCENARIO-003 | AC-001 | Manual translate button triggers translation |
| SCENARIO-004 | AC-003 | All translation disabled (Enable OFF, Auto stored ON) |
| SCENARIO-005 | AC-003 | All translation disabled (Enable OFF, Auto stored OFF) |
| SCENARIO-006 | AC-004 | Auto Translation toggle greyed out when Enable OFF |
| SCENARIO-007 | AC-005 | Auto Translation toggle interactive when Enable ON |
| SCENARIO-008 | AC-007 | Settings visual hierarchy |
| SCENARIO-009 | AC-005 | Toggle Auto Translation ON while Enable is ON |
| SCENARIO-010 | AC-001, AC-005 | Toggle Auto Translation OFF while Enable is ON |
| SCENARIO-011 | AC-006 | Auto Translation ON preserved across disable/enable cycle |
| SCENARIO-012 | AC-006 | Auto Translation OFF preserved across disable/enable cycle |
| SCENARIO-013 | AC-008 | Per-feed translateOnOpen gated when Enable OFF |
| SCENARIO-014 | AC-008 | Per-feed translateOnOpen functional when Enable ON |
| SCENARIO-015 | AC-009 | OPML export includes Enable Translation |
| SCENARIO-016 | AC-009 | OPML import restores Enable Translation |
| SCENARIO-017 | AC-003 (edge case) | In-progress translation when Enable toggled OFF |
| SCENARIO-018 | AC-001, AC-002 | Translation cancellation via stop button |
| SCENARIO-019 | AC-002 | App restart persistence (both ON) |
| SCENARIO-020 | AC-001 | App restart persistence (Enable ON, Auto OFF) |
| SCENARIO-021 | AC-003, AC-004 | App restart persistence (Enable OFF) |
| SCENARIO-022 | NFR-001 | New installation defaults |
| SCENARIO-023 | NFR-001 | Existing user upgrade backward compatibility |
| SCENARIO-024 | Out of scope validation | Summary unaffected by Enable Translation |
| SCENARIO-025 | Out of scope validation | Cached translations accessible when Enable OFF |
| SCENARIO-026 | AC-001 through AC-005 | Rapid toggling robustness |
