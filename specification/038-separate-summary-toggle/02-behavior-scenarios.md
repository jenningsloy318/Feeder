# 038 - Separate Summary Toggle: BDD Scenarios

---

## Feature: Separate Summary Toggle

As a user of Feeder,
I want separate controls for enabling summary functionality and auto-summary behavior,
So that I can use manual summarization without auto-summary, or disable all summary features entirely.

---

### Behavior Matrix States

#### SCENARIO-001: Enable Summary ON, Auto Summary ON - Full auto-summary (AC-002)

```gherkin
Scenario: Articles are automatically summarized when both toggles are ON
  Given "Enable Summary" is ON
  And "Auto Summary" is ON
  And the user has a valid AI provider configured
  When the user opens an article in article view
  Then the article is automatically summarized
  And the summarize button shows a progress indicator during summarization
  And the summary is displayed when complete
```

#### SCENARIO-002: Enable Summary ON, Auto Summary OFF - Manual summary only (AC-001)

```gherkin
Scenario: Articles are not auto-summarized but manual summary is available
  Given "Enable Summary" is ON
  And "Auto Summary" is OFF
  When the user opens an article in article view
  Then the article is NOT automatically summarized
  And no summary request is sent to the AI provider
  And the summarize button is visible in the toolbar
  And the summarize button is functional (not greyed out)
```

#### SCENARIO-003: Enable Summary ON, Auto Summary OFF - Manual summary triggers summarization (AC-001)

```gherkin
Scenario: User can manually trigger summarization when auto-summary is off
  Given "Enable Summary" is ON
  And "Auto Summary" is OFF
  And the user is viewing an article
  When the user taps the summarize button in the toolbar
  Then a summary request is sent to the AI provider
  And the summarize button shows a CircleProgressIconButton progress indicator
  And the summary is displayed when complete
```

#### SCENARIO-004: Enable Summary OFF, Auto Summary ON (stored) - All summary disabled (AC-003)

```gherkin
Scenario: No summary functionality when Enable Summary is OFF (auto-summary stored as ON)
  Given "Enable Summary" is OFF
  And "Auto Summary" has a stored value of ON
  When the user opens an article in article view
  Then the article is NOT automatically summarized
  And the summarize button is hidden or disabled in the toolbar
  And no summary request is sent to the AI provider
  And there is no way to trigger summarization from the article view
```

#### SCENARIO-005: Enable Summary OFF, Auto Summary OFF (stored) - All summary disabled (AC-003)

```gherkin
Scenario: No summary functionality when Enable Summary is OFF (auto-summary stored as OFF)
  Given "Enable Summary" is OFF
  And "Auto Summary" has a stored value of OFF
  When the user opens an article in article view
  Then the article is NOT automatically summarized
  And the summarize button is hidden or disabled in the toolbar
  And no summary request is sent to the AI provider
  And there is no way to trigger summarization from the article view
```

---

### Settings UI

#### SCENARIO-006: Auto Summary toggle is disabled when Enable Summary is OFF (AC-004)

```gherkin
Scenario: Auto Summary toggle is greyed out when Enable Summary is OFF
  Given "Enable Summary" is OFF
  When the user opens AI settings
  Then the "Auto Summary" toggle appears greyed out with reduced opacity
  And the "Auto Summary" toggle does not respond to tap events
  And tapping the "Auto Summary" toggle has no effect on its stored value
```

#### SCENARIO-007: Auto Summary toggle is interactive when Enable Summary is ON (AC-005)

```gherkin
Scenario: Auto Summary toggle is fully interactive when Enable Summary is ON
  Given "Enable Summary" is ON
  When the user opens AI settings
  Then the "Auto Summary" toggle is fully interactive
  And the "Auto Summary" toggle has normal opacity
  And the user can turn "Auto Summary" ON
  And the user can turn "Auto Summary" OFF
```

#### SCENARIO-008: Settings visual hierarchy - Enable Summary above Auto Summary (AC-007)

```gherkin
Scenario: Enable Summary is positioned above Auto Summary in settings
  Given the user opens AI settings
  Then "Enable Summary" appears above "Auto Summary" in the settings list
  And the dependency relationship between the two toggles is visually clear
  And both toggles are in the AI settings section
```

#### SCENARIO-009: Toggling Auto Summary while Enable Summary is ON

```gherkin
Scenario: User toggles Auto Summary from OFF to ON while Enable Summary is ON
  Given "Enable Summary" is ON
  And "Auto Summary" is OFF
  When the user opens AI settings
  And the user taps the "Auto Summary" toggle to turn it ON
  Then "Auto Summary" is now ON
  And subsequently opening an article triggers automatic summarization
```

#### SCENARIO-010: Toggling Auto Summary from ON to OFF while Enable Summary is ON

```gherkin
Scenario: User toggles Auto Summary from ON to OFF while Enable Summary is ON
  Given "Enable Summary" is ON
  And "Auto Summary" is ON
  When the user opens AI settings
  And the user taps the "Auto Summary" toggle to turn it OFF
  Then "Auto Summary" is now OFF
  And subsequently opening an article does NOT trigger automatic summarization
  And the summarize button remains visible and functional in article view
```

---

### Preference Preservation

#### SCENARIO-011: Auto Summary preference preserved across Enable Summary disable/enable cycle (AC-006)

```gherkin
Scenario: Auto Summary ON is preserved when Enable Summary is toggled OFF then ON
  Given "Enable Summary" is ON
  And "Auto Summary" is ON
  When the user turns "Enable Summary" OFF
  Then "Auto Summary" toggle appears greyed out
  And "Auto Summary" stored value remains ON
  When the user turns "Enable Summary" back ON
  Then "Auto Summary" is still ON
  And "Auto Summary" toggle is fully interactive
  And subsequently opening an article triggers automatic summarization
```

#### SCENARIO-012: Auto Summary OFF preference preserved across Enable Summary disable/enable cycle (AC-006)

```gherkin
Scenario: Auto Summary OFF is preserved when Enable Summary is toggled OFF then ON
  Given "Enable Summary" is ON
  And "Auto Summary" is OFF
  When the user turns "Enable Summary" OFF
  Then "Auto Summary" toggle appears greyed out
  And "Auto Summary" stored value remains OFF
  When the user turns "Enable Summary" back ON
  Then "Auto Summary" is still OFF
  And "Auto Summary" toggle is fully interactive
  And the summarize button is visible but articles are not auto-summarized
```

---

### Edge Cases

#### SCENARIO-013: Toggling Enable Summary OFF while summary is in progress

```gherkin
Scenario: In-progress summary is cancelled when Enable Summary is turned OFF
  Given "Enable Summary" is ON
  And "Auto Summary" is ON
  And the user has opened an article
  And a summary request is currently in progress (progress indicator is showing)
  When the user navigates to AI settings and turns "Enable Summary" OFF
  Then the in-progress summary request is cancelled
  And when returning to the article view the summarize button is hidden or disabled
  And no summary result is displayed for the in-progress request
```

#### SCENARIO-014: Summary cancellation still works when Enable Summary is ON

```gherkin
Scenario: User can cancel an in-progress summary via the stop button
  Given "Enable Summary" is ON
  And the user has triggered a summary (manual or auto)
  And the summary is in progress with CircleProgressIconButton showing
  When the user taps the stop button on the progress indicator
  Then the summary request is cancelled
  And the summarize button returns to its idle state
  And the user can tap the summarize button again to retry
```

#### SCENARIO-015: App restart with Enable Summary ON, Auto Summary ON

```gherkin
Scenario: Settings persist across app restart - both toggles ON
  Given "Enable Summary" is ON
  And "Auto Summary" is ON
  When the app is killed and restarted
  Then "Enable Summary" is still ON
  And "Auto Summary" is still ON
  And opening an article triggers automatic summarization
```

#### SCENARIO-016: App restart with Enable Summary ON, Auto Summary OFF

```gherkin
Scenario: Settings persist across app restart - Enable Summary ON, Auto Summary OFF
  Given "Enable Summary" is ON
  And "Auto Summary" is OFF
  When the app is killed and restarted
  Then "Enable Summary" is still ON
  And "Auto Summary" is still OFF
  And opening an article does NOT trigger automatic summarization
  And the summarize button is visible and functional
```

#### SCENARIO-017: App restart with Enable Summary OFF

```gherkin
Scenario: Settings persist across app restart - Enable Summary OFF
  Given "Enable Summary" is OFF
  And "Auto Summary" has a stored value of ON
  When the app is killed and restarted
  Then "Enable Summary" is still OFF
  And the summarize button is hidden or disabled in article view
  And the "Auto Summary" toggle is greyed out in settings
  And "Auto Summary" stored value is still ON
```

#### SCENARIO-018: New installation defaults

```gherkin
Scenario: Default values for new installation preserve existing behavior
  Given the app is freshly installed
  And the user has not modified any AI settings
  When the user opens AI settings
  Then "Enable Summary" defaults to ON
  And "Auto Summary" retains its existing default value
  And the user experience is unchanged from before this feature was added
```

#### SCENARIO-019: Existing user upgrade - backward compatibility

```gherkin
Scenario: Existing users experience no behavior change after upgrade
  Given an existing user has "Auto Summary" set to ON before the upgrade
  When the app is upgraded to a version with the "Enable Summary" toggle
  Then "Enable Summary" defaults to ON (true)
  And "Auto Summary" remains ON
  And the user's existing auto-summary behavior is preserved
  And no migration step is required
```

#### SCENARIO-020: Enable Summary toggle does not affect translation

```gherkin
Scenario: Translation functionality is unaffected by Enable Summary toggle
  Given "Enable Summary" is OFF
  When the user opens an article in article view
  Then translation functionality remains fully available
  And the translate button is visible and functional
  And tapping the translate button triggers translation normally
```

#### SCENARIO-021: Cached summaries remain accessible regardless of toggle state

```gherkin
Scenario: Previously cached summaries are still displayed when Enable Summary is OFF
  Given "Enable Summary" was ON
  And an article was previously summarized and the summary is cached
  When the user turns "Enable Summary" OFF
  And the user opens the previously summarized article
  Then the cached summary is still accessible and displayed
  And no new summary request is sent to the AI provider
```

#### SCENARIO-022: Rapid toggling of Enable Summary does not cause inconsistent state

```gherkin
Scenario: Rapid toggling of Enable Summary maintains consistent state
  Given "Enable Summary" is ON
  When the user rapidly toggles "Enable Summary" OFF and then ON
  Then the final state of "Enable Summary" is ON
  And the "Auto Summary" toggle is interactive
  And the summarize button is visible in article view
  And no duplicate or orphaned summary requests are created
```

---

## Traceability Matrix

| Scenario | Acceptance Criteria |
|----------|-------------------|
| SCENARIO-001 | AC-002 |
| SCENARIO-002 | AC-001 |
| SCENARIO-003 | AC-001 |
| SCENARIO-004 | AC-003 |
| SCENARIO-005 | AC-003 |
| SCENARIO-006 | AC-004 |
| SCENARIO-007 | AC-005 |
| SCENARIO-008 | AC-007 |
| SCENARIO-009 | AC-005 |
| SCENARIO-010 | AC-001, AC-005 |
| SCENARIO-011 | AC-006 |
| SCENARIO-012 | AC-006 |
| SCENARIO-013 | AC-003 (edge case) |
| SCENARIO-014 | AC-001, AC-002 |
| SCENARIO-015 | AC-002 |
| SCENARIO-016 | AC-001 |
| SCENARIO-017 | AC-003, AC-004 |
| SCENARIO-018 | AC-001 through AC-007 (defaults) |
| SCENARIO-019 | AC-001 through AC-007 (backward compatibility) |
| SCENARIO-020 | Out of scope validation |
| SCENARIO-021 | Out of scope validation |
| SCENARIO-022 | AC-001 through AC-005 (robustness) |
