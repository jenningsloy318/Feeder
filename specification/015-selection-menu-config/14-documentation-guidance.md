# Documentation Update Guidance: Selection Menu Configuration Feature

**Feature ID**: 015
**Version**: 1.0.0
**Date**: 2026-01-04
**Status**: Ready for Implementation

---

## 1. Documentation Overview

This document provides guidance for updating all project documentation after the Selection Menu Configuration Feature is implemented. Documentation should be updated in Phase 10, after code review is approved.

### 1.1 Documentation Types

1. **Code Documentation**: Inline comments and KDoc
2. **README Documentation**: Feature description and usage
3. **API Documentation**: Public API references
4. **User Documentation**: End-user guides (if applicable)

### 1.2 Update Process

```
Code Review Approved
         ↓
   Update Code Comments (KDoc, inline)
         ↓
   Update README
         ↓
   Update API Documentation (if any)
         ↓
   Update User Documentation (if any)
         ↓
   Documentation Review
         ↓
   Approved
```

---

## 2. Code Documentation

### 2.1 KDoc Comments

Add KDoc comments to all public classes, functions, and properties.

#### Classes

**Template**:
```kotlin
/**
 * Human-readable description of the class.
 *
 * Additional details about the class purpose, usage, and behavior.
 *
 * @property propertyName Description of property
 * @constructor Description of constructor parameters
 *
 * @sample com.example.package.ClassName.sampleUsage
 *
 * @author Author Name
 * @since 1.0.0
 */
class ClassName {
    // ...
}
```

**Example for SelectionMenuConfigRepositoryImpl**:
```kotlin
/**
 * Repository implementation for managing selection menu configuration.
 *
 * This repository handles the persistence and retrieval of selection menu
 * configuration, including the order and enabled state of actions in the
 * text selection toolbar. It also integrates with third-party app discovery
 * to automatically add translator, copy, and share apps.
 *
 * The repository uses SharedPreferences for persistence, storing the
 * configuration as a JSON array of [SelectionMenuItem] objects.
 *
 * @property context Application context for accessing SharedPreferences
 * @property thirdPartyRepo Repository for discovering third-party apps
 *
 * @see SelectionMenuConfigRepository
 * @see SelectionMenuItem
 *
 * @author Super Dev Workflow
 * @since 1.0.0
 */
class SelectionMenuConfigRepositoryImpl(
    private val context: Context,
    private val thirdPartyRepo: ThirdPartyAppRepository
) : SelectionMenuConfigRepository {
    // ...
}
```

#### Functions

**Template**:
```kotlin
/**
 * Human-readable description of what the function does.
 *
 * Additional details about the function behavior, parameters, and return value.
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionDescription Description of exceptions thrown
 *
 * @sample com.example.package.ClassName.functionName.sampleUsage
 */
fun functionName(paramName: Type): ReturnType {
    // ...
}
```

**Example for observeConfig**:
```kotlin
/**
 * Observe the current selection menu configuration.
 *
 * This function returns a Flow that emits the current configuration
 * and updates whenever the configuration changes. The configuration
 * includes both built-in actions (Translate, Copy, Share, Open in Browser)
 * and discovered third-party apps.
 *
 * The configuration is merged with discovered third-party apps each time
 * it's emitted, ensuring new apps are added automatically while preserving
 * user customizations.
 *
 * @return A Flow that emits the list of menu items in the current configuration
 *
 * @see SelectionMenuItem
 * @see ThirdPartyAppRepository
 */
override fun observeConfig(): Flow<List<SelectionMenuItem>> {
    // ...
}
```

#### Properties

**Template**:
```kotlin
/**
 * Description of the property.
 *
 * Additional details about the property purpose and usage.
 *
 * Default value: [defaultValue]
 */
val propertyName: Type = defaultValue
```

**Example for defaultSelectionMenuConfig**:
```kotlin
/**
 * Default selection menu configuration.
 *
 * This configuration provides the default order and enabled state
 * of actions in the text selection toolbar. It matches the
 * historical hardcoded order in Feeder:
 * 1. Translate (order 0)
 * 2. Copy (order 1)
 * 3. Share (order 2)
 * 4. Open in Browser (order 3)
 *
 * All actions are enabled by default. Users can customize the order
 * and enabled state in the Selection Menu Settings screen.
 */
private val defaultSelectionMenuConfig = listOf(
    SelectionMenuItem(
        id = "translate",
        type = ActionType.TRANSLATE,
        enabled = true,
        order = 0,
        label = LocalStrings.current.translate
    ),
    // ... other items
)
```

---

### 2.2 Inline Comments

Add inline comments for complex logic, non-obvious decisions, and workarounds.

#### Complex Logic

**Example**:
```kotlin
// Merge discovered third-party apps with stored configuration
// Strategy: Add new apps after built-in actions, preserve existing user customizations
fun mergeThirdPartyApps(
    stored: List<SelectionMenuItem>,
    translators: List<TranslatorApp>
): List<SelectionMenuItem> {
    val result = stored.toMutableList()

    // Find the maximum order among built-in actions to insert third-party apps after
    val maxBuiltInOrder = result
        .filter { it.type != ActionType.CUSTOM }
        .maxOfOrNull { it.order } ?: -1

    // Add new translators that aren't already in the configuration
    translators.forEach { app ->
        val exists = result.any { it.thirdPartyPackageName == app.packageName }
        if (!exists) {
            // Insert after built-in actions, preserve user customizations
            result.add(
                SelectionMenuItem(
                    id = "translator_${app.packageName}",
                    type = ActionType.TRANSLATE,
                    enabled = true,  // Default to enabled
                    order = maxBuiltInOrder + 1 + result.count { it.type == ActionType.CUSTOM },
                    label = app.appName,
                    thirdPartyPackageName = app.packageName,
                    thirdPartyClassName = app.className
                )
            )
        }
    }

    // Reorder all items to ensure contiguous order values
    return result.sortedBy { it.order }
}
```

#### Non-Obvious Decisions

**Example**:
```kotlin
// Use collectAsStateWithLifecycle instead of collectAsState to ensure
// the flow is collected only when the composable is in a started state
// This prevents unnecessary updates when the app is in the background
val config by selectionMenuConfigRepository.observeConfig()
    .collectAsStateWithLifecycle(initial = emptyList())
```

#### Workarounds

**Example**:
```kotlin
// Workaround for kotlinx.serialization not supporting nullable fields
// in the default JSON configuration. We use coerceInputValues = true
// to handle missing null fields gracefully.
private val json = Json {
    ignoreKeys = true
    coerceInputValues = true  // Handle missing nullable fields
}
```

---

### 2.3 TODO and FIXME

Ensure all TODOs and FIXMEs are addressed:

```kotlin
// TODO: Remove this workaround once kotlinx.serialization supports nullable fields
// FIXME: This causes a minor visual glitch when dragging quickly
```

**Actions**:
- [ ] Address all TODOs or file as issues
- [ ] Address all FIXMEs or file as issues
- [ ] Remove TODO/FIXME comments if resolved

---

## 3. README Documentation

### 3.1 Feature Description

Add a section to the README describing the new feature.

**Location**: `README.md` (root of Feeder project)

**Section to Add**:
```markdown
## Selection Menu Configuration

Users can customize the order and visibility of actions in the text selection toolbar.

### Features

- **Drag-and-Drop Reordering**: Long-press and drag action handles to reorder
- **Enable/Disable Actions**: Toggle individual actions on or off
- **Third-Party App Integration**: Automatically discovers translator, copy, and share apps
- **Persistent Configuration**: Customizations are saved across app sessions
- **Reset to Defaults**: Restore the default configuration at any time

### How to Use

1. Open Feeder Settings
2. Tap "Selection Menu"
3. Drag handles to reorder actions
4. Use switches to enable/disable actions
5. Tap "Reset to Defaults" to restore original order

### Configuration

Configuration is stored locally in SharedPreferences as JSON:

\`\`\`json
[
  {
    "id": "translate",
    "type": "TRANSLATE",
    "enabled": true,
    "order": 0,
    "label": "Translate"
  }
]
\`\`\`

### Third-Party Apps

Third-party apps are automatically discovered and added to the selection menu:

- **Translate Apps**: Apps that handle `ACTION_PROCESS_TEXT` (e.g., DeepL, Google Translate)
- **Copy Apps**: Apps with clipboard listeners
- **Share Targets**: Apps that handle share intents

Discovered apps are added after built-in actions and can be customized like built-in actions.

### Technical Details

- **Persistence**: JSON in SharedPreferences
- **Real-Time Updates**: StateFlow observation
- **Drag-and-Drop**: Custom Jetpack Compose modifier
- **Third-Party Discovery**: On-demand PackageManager queries

For more technical details, see the specification documents in the `.worktree/spec-15-global-menu-config/` directory.
```

---

### 3.2 Screenshots

Add screenshots if available (create placeholder for now):

```markdown
### Screenshots

#### Selection Menu Settings Screen
![Selection Menu Settings](screenshots/selection-menu-settings.png)

#### Drag-and-Drop Reordering
![Drag-and-Drop](screenshots/drag-and-drop.png)

#### Third-Party Apps
![Third-Party Apps](screenshots/third-party-apps.png)
```

---

### 3.3 API Documentation

If there are public APIs, document them in a separate section:

```markdown
## API Documentation

### SelectionMenuConfigRepository

Repository for managing selection menu configuration.

#### Functions

##### `observeConfig(): Flow<List<SelectionMenuItem>>`

Observe the current selection menu configuration.

**Returns**: Flow that emits the configuration and updates on changes

##### `updateOrder(items: List<SelectionMenuItem>)`

Update the order of menu items.

**Parameters**:
- `items`: New ordered list of menu items

##### `updateEnabled(itemId: String, enabled: Boolean)`

Update the enabled state of a menu item.

**Parameters**:
- `itemId`: ID of the menu item to update
- `enabled`: New enabled state

##### `resetToDefaults()`

Reset the configuration to defaults.

**See**: [defaultSelectionMenuConfig] for default values
```

---

## 4. User Documentation

### 4.1 User Guide

Create a user guide if Feeder has user-facing documentation.

**Location**: Create `docs/user-guide/selection-menu.md` (if docs directory exists)

**Content**:
```markdown
# Selection Menu Configuration

## Overview

The Selection Menu feature allows you to customize which actions appear when you select text in an article.

## Customizing Actions

### Reordering Actions

1. Open Feeder Settings
2. Tap "Selection Menu"
3. Long-press the drag handle (≡) next to an action
4. Drag the action to the desired position
5. Release to drop the action

### Enabling/Disabling Actions

1. Open Feeder Settings
2. Tap "Selection Menu"
3. Tap the switch next to an action to enable or disable it
4. The action will appear or disappear from the selection menu

### Resetting to Defaults

1. Open Feeder Settings
2. Tap "Selection Menu"
3. Tap "Reset to Defaults" at the bottom
4. Confirm the reset

## Available Actions

### Built-in Actions

- **Translate**: Translate selected text using AI translation
- **Copy**: Copy selected text to clipboard
- **Share**: Share selected text with other apps
- **Open in Browser**: Open selected text as URL in browser

### Third-Party Apps

Third-party apps that can handle text selection will automatically appear in the selection menu:

- **Translator Apps**: DeepL, Google Translate, and other translation apps
- **Copy Apps**: Apps with clipboard enhancement features
- **Share Targets**: Apps that can handle text sharing

## Troubleshooting

### Third-Party App Not Appearing

1. Ensure the app is installed on your device
2. Open Feeder Settings → Selection Menu to trigger app discovery
3. If the app still doesn't appear, check that the app handles text selection intents

### Configuration Not Persisting

1. Ensure you have storage permissions
2. Check that Feeder is not being killed by the system
3. Try resetting to defaults and reconfiguring

### Actions Not Appearing in Toolbar

1. Check that the action is enabled in Selection Menu Settings
2. Ensure the action is not hidden by another app
3. Restart Feeder and try again
```

---

### 4.2 FAQ

Add FAQ entries if Feeder has an FAQ section:

```markdown
## FAQ

### Q: Can I add custom actions to the selection menu?

A: Currently, only built-in actions and third-party apps are supported. Custom actions may be added in a future update.

### Q: How do I remove a third-party app from the selection menu?

A: You can disable third-party apps by toggling the switch next to the app in Selection Menu Settings.

### Q: Will my configuration be lost if I uninstall Feeder?

A: Yes, the configuration is stored locally and will be removed if you uninstall Feeder. You can export/import your configuration in a future update.

### Q: Why do some third-party apps appear automatically?

A: Feeder automatically discovers apps that can handle text selection, such as translator apps and share targets. This makes it easy to use your favorite apps without manual configuration.

### Q: Can I sync my configuration across devices?

A: Currently, configuration is stored locally only. Cross-device sync may be added in a future update.
```

---

## 5. Change Log

### 5.1 Version History

Add entry to CHANGELOG.md (if exists):

```markdown
## [Unreleased]

### Added
- Selection menu configuration feature
  - Drag-and-drop reordering of selection menu actions
  - Enable/disable individual actions
  - Third-party app integration (translators, copy apps, share targets)
  - Persistent configuration across app sessions
  - Reset to defaults functionality

### Changed
- FeederTextToolbar now observes configurable selection menu
- Settings screen now includes "Selection Menu" entry

### Technical
- Added kotlinx-serialization for JSON handling
- Added SelectionMenuConfigRepository for configuration management
- Added ThirdPartyAppRepository for third-party app discovery
- Added custom drag-and-drop modifier for Jetpack Compose
```

---

## 6. Developer Documentation

### 6.1 Architecture Overview

Add developer documentation if Feeder has developer docs:

```markdown
# Selection Menu Configuration - Architecture

## Overview

The Selection Menu Configuration feature follows a layered architecture:

1. **Data Layer**: SharedPreferences with JSON serialization
2. **Repository Layer**: Business logic and third-party discovery
3. **ViewModel Layer**: State management
4. **UI Layer**: Jetpack Compose screens with drag-and-drop

## Components

### Data Layer

- `SelectionMenuItem`: Data model for menu items
- `SelectionMenuItemSerializer`: JSON serialization/deserialization
- `SelectionMenuConfigStore`: SettingsStore extension for persistence

### Repository Layer

- `SelectionMenuConfigRepository`: Repository interface
- `SelectionMenuConfigRepositoryImpl`: Repository implementation
- `ThirdPartyAppRepository`: Third-party app discovery interface
- `ThirdPartyAppRepositoryImpl`: App discovery implementation

### ViewModel Layer

- `SelectionMenuSettingsViewModel`: ViewModel for settings screen
- `SelectionMenuSettingsUiState`: UI state data class
- `DraggedItem`: Drag-and-drop state

### UI Layer

- `SelectionMenuSettingsScreen`: Main settings screen
- `ListItemRow`: Draggable list item with switch
- `DraggableItemModifier`: Custom drag-and-drop modifier

## Data Flow

### Configuration Loading

1. User opens settings screen
2. ViewModel loads config from repository
3. Repository reads from SharedPreferences
4. Repository merges with discovered third-party apps
5. ViewModel emits UiState
6. UI composes with config

### Configuration Updates

1. User changes config (drag, toggle)
2. ViewModel calls repository method
3. Repository updates SharedPreferences
4. Repository emits new config via Flow
5. Toolbar observes and recomposes

## Architecture Decisions

See ADRs in `.worktree/spec-15-global-menu-config/specification/015-selection-menu-config/adrs/`:

- ADR-001: JSON in SharedPreferences (4.2/5)
- ADR-002: Custom drag-and-drop modifier (4.5/5)
- ADR-003: StateFlow observation (4.6/5)
- ADR-004: On-demand third-party discovery (4.7/5)

## Integration Points

- `FeederTextToolbar`: Observes config via Flow
- `SettingsStore`: Extension for config persistence
- `Navigation`: Route to settings screen
- `DI Module`: Repository providers

## Testing

- Unit tests: ViewModel, Repository, Serializer
- Integration tests: Persistence, real-time updates
- UI tests: Drag-and-drop, toggles, reset
- Manual tests: 12 scenarios documented in task list
```

---

## 7. Documentation Checklist

### 7.1 Code Documentation

- [ ] All public classes have KDoc comments
- [ ] All public functions have KDoc comments
- [ ] All public properties have KDoc comments
- [ ] Complex logic has inline comments
- [ ] Non-obvious decisions explained
- [ ] Workarounds documented
- [ ] TODO/FIXME addressed or filed

### 7.2 README Documentation

- [ ] Feature description added
- [ ] How to use section added
- [ ] Configuration details documented
- [ ] Third-party integration explained
- [ ] Technical details provided
- [ ] Screenshots added (if available)

### 7.3 User Documentation

- [ ] User guide created (if applicable)
- [ ] FAQ updated (if applicable)
- [ ] Troubleshooting guide added (if applicable)

### 7.4 Developer Documentation

- [ ] Architecture overview added (if applicable)
- [ ] API documentation added (if applicable)
- [ ] Component documentation added (if applicable)
- [ ] Data flow documented (if applicable)

### 7.5 Change Log

- [ ] CHANGELOG.md updated (if exists)
- [ ] Version history updated
- [ ] Breaking changes noted (if any)

---

## 8. Documentation Review

### 8.1 Review Checklist

Review documentation against this checklist:

**Completeness**:
- [ ] All features documented
- [ ] All public APIs documented
- [ ] All configuration options documented
- [ ] All user-facing changes documented

**Clarity**:
- [ ] Language is clear and concise
- [ ] Technical terms explained
- [ ] Examples provided where helpful
- [ ] Screenshots/diagrams included (if applicable)

**Accuracy**:
- [ ] Documentation matches implementation
- [ ] Code examples compile
- [ ] API signatures correct
- [ ] Configuration details accurate

**Accessibility**:
- [ ] Documentation is easy to navigate
- [ ] Sections are clearly organized
- [ ] Cross-references work
- [ ] Searchable (if applicable)

### 8.2 Review Process

1. **Self-Review**: Developer reviews documentation
2. **Peer Review**: Team member reviews documentation
3. **User Testing**: User tests documentation (if applicable)
4. **Approval**: Documentation approved

---

## 9. Next Steps

### 9.1 After Documentation Update

1. **Phase 11**: Cleanup temporary files
2. **Phase 12**: Commit and push changes
3. **Phase 13**: Final verification

### 9.2 Documentation Maintenance

- Keep documentation updated with code changes
- Review documentation periodically for accuracy
- Update screenshots when UI changes
- Add FAQ entries as questions arise

---

**END OF DOCUMENTATION UPDATE GUIDANCE**
