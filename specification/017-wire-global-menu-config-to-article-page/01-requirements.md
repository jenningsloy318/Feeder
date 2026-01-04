# Requirements: Wire Global Menu Config to Article Page

**Document Version**: 1.0
**Date**: 2026-01-04
**Status**: Draft
**Spec Index**: 017

---

## 1. Feature Overview

### 1.1 Purpose
Connect the Selection Menu Configuration (implemented in specs 15-16) to the actual text selection menu on the article page. Currently, the article page shows a hardcoded text selection menu. This feature will make it respect the user's configured menu order and visibility settings.

### 1.2 Current State
- **Settings → Text → Selection Menu**: Users can toggle menu items on/off and reorder them
- **Article Page Text Selection**: Shows hardcoded menu items (copy, paste, translate, etc.)
- **Problem**: Configuration in settings doesn't affect the actual selection menu

### 1.3 Desired State
- When user selects text on an article page, the menu shows only visible items
- Menu items appear in the user's configured order
- Changes in settings immediately affect the article page menu

---

## 2. User Story

**As a user**, I want the text selection menu on article pages to reflect my configuration settings,

**So that** I can customize which menu items appear and in what order when selecting text.

**Acceptance**: I can toggle "Read Aloud" off in settings, and it no longer appears when I select text in an article.

---

## 3. Functional Requirements

### FR1: Load Menu Configuration in Article Screen
**Location**: Article reading screen/composable

When the article screen loads:
1. Read `MenuConfig` from SharedPreferences (same key as settings: `selection_menu_config`)
2. Parse the JSON to get:
   - `order`: List of item IDs in user's preferred order
   - `visibility`: Map of item ID to boolean (visible = true)
3. Merge with available menu items (system, Feeder, third-party)
4. Make configuration available to text selection menu builder

### FR2: Filter Menu Items by Visibility
**Location**: Text selection menu construction

When building the text selection menu:
1. Start with all discovered menu items (system, Feeder, third-party)
2. Filter using `visibility` map from `MenuConfig`
3. Only include items where `visibility[itemId] == true`
4. Sort items by their position in the `order` list
5. Items not in `order` list (newly discovered) should be appended at the end

### FR3: Sort Menu Items by Configured Order
**Location**: Text selection menu construction

After filtering:
1. Use the `order` list from `MenuConfig` as the primary sort key
2. Map each item ID in `order` to its actual menu item
3. Handle missing items gracefully (app uninstalled, etc.)
4. Append newly discovered items (not in saved order) to the end

### FR4: Detect and Include Third-Party Apps
**Location**: Article screen menu discovery

When showing the selection menu:
1. Query PackageManager for `ACTION_PROCESS_TEXT` handlers
2. Include third-party apps that are:
   - Visible in user's configuration (`visibility[id] == true`)
   - In the configured `order` list (or append if new)
3. Use the same discovery logic as `MenuDiscoveryService` in settings

### FR5: Handle Configuration Changes
**Location**: Article screen lifecycle

When user changes settings:
1. Article screen should observe `MenuConfig` changes (SharedPreferences listener or Flow)
2. When config changes, reload menu configuration
3. Next text selection will use updated configuration
4. No need to update currently visible menu (only affects new selections)

---

## 4. Non-Functional Requirements

### NFR1: Performance
- Menu construction time: < 100ms (must feel instant)
- Config load time: < 50ms (SharedPreferences read)
- No UI lag when selecting text
- Cache discovered items for article session

### NFR2: User Experience
- Menu items appear in the same order as configured in settings
- Toggleed-off items never appear in the menu
- Visual feedback matches expectations
- Smooth animations when menu appears

### NFR3: Compatibility
- Android API 29+ (minSdk from project)
- Works on phone, tablet, and foldable
- Portrait and landscape orientations
- Different screen densities

### NFR4: Code Quality
- Follow project coding standards
- Unit test coverage: > 80%
- No compiler warnings
- Clean architecture with MVVM pattern
- Reuse existing `MenuDiscoveryService` and `MenuConfig` from settings

### NFR5: Localization
- Menu names use existing localized strings
- No new localization needed (reuse from settings)

---

## 5. Technical Requirements

### TR1: Existing Code Analysis

**Target**: Find where text selection menu is built in article screen

Likely locations:
- `FeederTextActionModeCallback` - Handles text selection actions
- Article screen composables - Where text is displayed
- Menu construction logic - Builds the selection menu

**Key Questions**:
1. How is the current menu built?
2. Where is `ActionMode` or `Menu` created?
3. How are third-party apps currently added?
4. Can we reuse this logic or need to refactor?

### TR2: Data Flow

```
Article Screen Loads
    ↓
Load MenuConfig from SharedPreferences
    ↓
Parse JSON → {order, visibility}
    ↓
User Selects Text
    ↓
Build Menu:
    1. Discover available items (system, Feeder, third-party)
    2. Filter by visibility
    3. Sort by order
    4. Create Menu/ActionMode items
    ↓
Show menu to user
```

### TR3: Components to Modify

#### Modify: FeederTextActionModeCallback (or equivalent)
- Load `MenuConfig` on initialization
- Filter menu items by visibility
- Sort items by configured order
- Include third-party apps from configuration

#### Modify: Article Screen
- Inject `MenuConfig` or SharedPreferences
- Pass configuration to menu builder
- Observe configuration changes (optional)

#### Reuse: MenuDiscoveryService
- Already implemented in settings
- Discover system, Feeder, and third-party items
- Returns `List<SelectionMenuItem>`

#### Reuse: MenuConfig
- Already implemented in settings
- Data class for persistence
- JSON serialization/deserialization

### TR4: String Resources

**No new strings needed** - reuse from settings:
- `selection_menu_read_aloud`
- `selection_menu_translate`
- Android system strings (copy, paste, cut, selectAll)

### TR5: Dependencies

**No new dependencies** - reuse existing:
- SharedPreferences (already in project)
- Kotlin serialization (already in project)
- PackageManager (Android SDK)

---

## 6. Architecture Design

### 6.1 Component Diagram

```
┌──────────────────────────────────────────────────────┐
│              Article Screen (UI)                     │
│  - Displays article text                            │
│  - Handles text selection                           │
└───────────────┬──────────────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────────────┐
│         MenuConfigLoader (New or Existing)          │
│  - Loads MenuConfig from SharedPreferences          │
│  - Provides config to menu builder                  │
└───────────────┬──────────────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────────────┐
│       MenuBuilder (Refactor Existing)               │
│  - Uses MenuConfig (order, visibility)              │
│  - Calls MenuDiscoveryService                       │
│  - Filters and sorts items                          │
│  - Creates Menu/ActionMode items                    │
└───────────────┬──────────────────────────────────────┘
                │
                ↓
┌──────────────────────────────────────────────────────┐
│         MenuDiscoveryService (Reuse)                │
│  - discoverSystemMenus()                            │
│  - discoverFeederMenus()                            │
│  - discoverThirdPartyMenus()                        │
└──────────────────────────────────────────────────────┘
```

### 6.2 Data Flow

```
SharedPreferences
    │
    ├─→ "selection_menu_config" (JSON)
    │     └─→ MenuConfig {order, visibility}
    │
    ↓
Article Screen.init()
    │
    ├─→ Load MenuConfig
    │     └─→ Parse JSON
    │
    ↓
User Selects Text
    │
    ├─→ Trigger menu building
    │     ├─→ Call MenuDiscoveryService.discoverAll()
    │     │     └─→ List<SelectionMenuItem> (all items)
    │     │
    │     ├─→ Filter: keep only visible items
    │     │     └─→ List<SelectionMenuItem> (visible items)
    │     │
    │     ├─→ Sort: reorder by config.order
    │     │     └─→ List<SelectionMenuItem> (sorted visible items)
    │     │
    │     └─→ Build Menu/ActionMode items
    │           └─→ Show to user
```

---

## 7. Acceptance Criteria

### AC1: Configuration Loading
- [ ] Article screen loads MenuConfig from SharedPreferences
- [ ] MenuConfig is parsed correctly (order, visibility)
- [ ] Missing/invalid config falls back to defaults
- [ ] Config is cached for the article session

### AC2: Menu Filtering
- [ ] Menu items marked as invisible don't appear in selection menu
- [ ] All visible items appear in selection menu
- [ ] Filtering works for all item types (system, Feeder, third-party)
- [ ] Toggle in settings immediately affects next selection

### AC3: Menu Ordering
- [ ] Menu items appear in user's configured order
- [ ] System items respect configured order
- [ ] Feeder items respect configured order
- [ ] Third-party items respect configured order
- [ ] Reordering in settings immediately affects next selection

### AC4: Third-Party Apps
- [ ] Third-party apps in configuration appear in menu
- [ ] Third-party apps not in configuration don't appear
- [ ] Newly installed apps appear in menu (as visible)
- [ ] Uninstalled apps are removed from menu

### AC5: Code Quality
- [ ] All code compiles without errors
- [ ] All code compiles without warnings
- [ ] Unit tests written and passing (coverage > 80%)
- [ ] Follows project naming conventions
- [ ] Reuses existing components (MenuDiscoveryService, MenuConfig)

### AC6: Integration
- [ ] No conflicts with existing menu logic
- [ ] No conflicts with settings screen
- [ ] Configuration changes sync correctly
- [ ] Backward compatibility (works with no config)

---

## 8. Out of Scope (Future Work)

- Real-time config updates (currently: only affects new selections)
- Per-article menu configuration
- Menu customization from article page (without going to settings)
- Menu preview in settings
- Import/export menu configurations

---

## 9. Dependencies

### Existing Code
- `FeederTextActionModeCallback` - Current menu handling
- `MenuDiscoveryService` - Menu item discovery (spec-016)
- `MenuConfig` - Configuration data class (spec-016)
- `SelectionMenuItem` - Menu item data class (spec-016)
- Article screen composables - Where text selection happens

### External Libraries
- SharedPreferences (Android SDK)
- Kotlin serialization (already in project)
- PackageManager (Android SDK)

### Related Specifications
- Spec-015: Selection Menu Configuration (placeholder)
- Spec-016: Global Menu Item Discovery (discovery, toggle, reorder)
- Spec-017: Wire Config to Article Page (this spec)

---

## 10. Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Existing menu logic tightly coupled | Medium | Refactor carefully, add abstraction layer |
| Performance regression | Medium | Cache config, measure menu build time |
| Third-party app discovery inconsistent | Low | Reuse MenuDiscoveryService logic |
| Config parsing failures | Low | Validate JSON, fallback to defaults |
| Backward compatibility | Medium | Handle missing config gracefully |

---

## 11. Success Metrics

- Menu appears within 100ms of text selection
- Menu items match user's configuration 100%
- Toggle in settings immediately affects menu (next selection)
- Reorder in settings immediately affects menu order
- No crashes when config is missing/invalid
- Unit test coverage > 80%
- Zero compiler warnings

---

## 12. Questions and Clarifications

### Q1: Should configuration changes update the currently visible menu?
**A1**: No. Only affect new text selections. Updating the currently visible menu is complex and not necessary for good UX.

### Q2: What if a configured item is no longer available (app uninstalled)?
**A2**: Skip it. The menu builder should handle missing items gracefully and not show them.

### Q3: Should we show newly discovered items (not in config)?
**A3**: Yes, append them to the end of the menu as visible. This ensures new apps/installations are accessible.

### Q4: How do we handle the first launch (no config yet)?
**A4**: Show all items in default order (system → Feeder → third-party sorted by name).

### Q5: Can we reuse MenuDiscoveryService from settings?
**A5**: Yes! Inject it into the article screen or menu builder. This ensures consistency.

---

## 13. Sign-off

**Product Owner**: Requirements pending approval
**Date**: 2026-01-04
**Priority**: High (Completes specs 15-16 feature)

**Next Steps**:
1. Research existing menu code
2. Assess architecture and integration points
3. Write technical specification
4. Implement and test

---

**Document Version**: 1.0
**Last Updated**: 2026-01-04
**Status**: Draft - Ready for Review
