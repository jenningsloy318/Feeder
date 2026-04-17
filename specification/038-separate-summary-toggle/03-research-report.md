# 038 - Separate Summary Toggle: Research Report

---

## Research Question 1: Android Settings Patterns for Dependent/Hierarchical Toggles

### Findings

The standard Android pattern for dependent toggles in Jetpack Compose settings screens is straightforward:

1. **Master toggle state drives sub-toggle `enabled` parameter**: The master toggle's `checked` state (or a derived value) is passed as the `enabled` parameter of the dependent toggle. When `enabled = false`, the Switch component natively handles visual disabling and blocks user interaction.

2. **Row-level disabling**: The entire settings row (not just the Switch) should be disabled. This is achieved by:
   - Setting `enabled = false` on the `toggleable` or `clickable` modifier of the row
   - Applying reduced alpha to text labels (the Switch handles its own disabled colors)

3. **Google's own apps** (e.g., Android Settings) use this exact pattern for Wi-Fi/Bluetooth sub-settings: a master toggle enables/disables the section, and dependent items appear greyed out with `enabled = false`.

4. **Compose Samples** (android/compose-samples) confirm the pattern: use `MutableState` or `StateFlow` from ViewModel to hold master toggle state, and pass it to sub-toggle's `enabled` parameter.

**Source**: [android/compose-samples DeepWiki](https://deepwiki.com/android/compose-samples), [Settings Screen in Compose — DEV Community](https://forem.com/myougatheaxo/settings-screen-in-compose-switch-slider-selection-dialogs-3dm6)

---

## Research Question 2: Idiomatic Way to Disable a Switch/Toggle in Compose

### Findings

The Jetpack Compose Material 3 `Switch` composable has a built-in `enabled` parameter:

```kotlin
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,  // <-- KEY PARAMETER
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
)
```

**When `enabled = false`:**
- The Switch **will not respond to user input** (tap/click events are ignored)
- It **appears visually disabled** using dedicated disabled color tokens:
  - `disabledCheckedThumbColor`, `disabledCheckedTrackColor`, etc.
  - `disabledUncheckedThumbColor`, `disabledUncheckedTrackColor`, etc.
  - These use reduced opacity (composited over surface color)
- It is **disabled to accessibility services** (announced as disabled to screen readers)

**For the surrounding row/text**, there are two idiomatic approaches:

### Approach A: `Modifier.alpha()` (Recommended for Material 3)
```kotlin
val alpha = if (enabled) 1f else 0.38f
Row(
    modifier = Modifier
        .alpha(alpha)
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
) {
    Text(text = title)
    Switch(checked = checked, onCheckedChange = null, enabled = enabled)
}
```

The value `0.38f` is the Material 3 standard disabled alpha (equivalent to the old `ContentAlpha.disabled`).

### Approach B: `CompositionLocalProvider` with `LocalContentColor`
```kotlin
CompositionLocalProvider(
    LocalContentColor provides if (enabled) 
        MaterialTheme.colorScheme.onSurface 
    else 
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
) {
    // Text will inherit the disabled color automatically
}
```

### Recommendation

Use **`enabled` parameter on Switch** (handles its own disabled visuals) + **`Modifier.alpha(0.38f)` on the row** for text labels. This matches the project's existing patterns and is the most concise approach.

**Source**: [Android Developers — Switch API](http://developer.android.google.cn/reference/kotlin/androidx/compose/material3/Switch.composable), [composables.com — SwitchDefaults](https://composables.com/jetpack-compose/androidx.compose.material3/material3/objects/SwitchDefaults/api), [Jetpack Compose: Switch — alexzh.com](https://alexzh.com/jetpack-compose-switch/), [StackOverflow — Disabled Text](https://stackoverflow.com/questions/69896192)

---

## Research Question 3: DataStore/Flow Patterns for Dependent Settings

### Findings

The Feeder project uses `SettingsStore` with Flow-based access. For combining dependent settings, the standard pattern is:

### Pattern: `combine` operator

```kotlin
val summaryUiState: StateFlow<SummaryUiState> = combine(
    settingsStore.enableSummary,      // Flow<Boolean>
    settingsStore.autoSummary,        // Flow<Boolean>
) { enableSummary, autoSummary ->
    SummaryUiState(
        enableSummary = enableSummary,
        autoSummary = autoSummary,
        effectiveAutoSummary = enableSummary && autoSummary,
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SummaryUiState()
)
```

**Key best practices:**

1. **Store settings independently**: `enableSummary` and `autoSummary` are stored as separate boolean keys. The dependency logic lives in the ViewModel/UI layer, not the storage layer. This preserves the stored value of `autoSummary` when `enableSummary` is OFF (FR-003).

2. **Derive effective state in ViewModel**: Use `combine` to merge the two flows and produce a single UI state that includes both raw values and the derived "effective" state.

3. **No need for `distinctUntilChanged`**: DataStore already emits only when values change.

4. **Type-safe combine**: The Kotlin `combine` function supports up to 5 typed parameters directly. The Feeder project already uses `combine` with 35+ params in `SettingsViewModel`, so adding 1-2 more flows is trivial.

5. **Single DataStore read**: If both settings come from the same DataStore, use `dataStore.data.map { prefs -> ... }` to read both in one flow, avoiding the need for `combine` entirely.

**Source**: [Flow Composition Patterns — carrion.dev](https://carrion.dev/en/posts/flow-composition-patterns/), [Android Developers DataStore Codelab](https://developer.android.com/codelabs/android-preferences-datastore), [StackOverflow — DataStore combine](https://stackoverflow.com/questions/78137912)

---

## Research Question 4: Material Design 3 Guidelines for Dependent Settings

### Findings

Material Design 3 provides guidance for switches and toggle states:

1. **Switches are for independent settings**: M3 spec says "Switches are the best way to let people adjust settings" and "Use switches if the items in a list can be independently controlled." However, dependent toggles are a common real-world pattern.

2. **Disabled state is well-defined**: M3 defines four columns of switch states: **enabled, disabled, hover, focused, pressed** crossed with **on/off**. The disabled state uses reduced opacity tokens specific to each part (thumb, track, border, icon).

3. **Visual hierarchy through grouping**: M3 recommends using section headers and visual grouping to communicate related settings. The master toggle should appear above sub-toggles within the same section.

4. **Opacity for disabled state**: M3 uses `0.38f` alpha (38%) as the standard disabled opacity for content on surface. This is consistent across all M3 components.

5. **No explicit "dependent toggle" component**: M3 does not provide a dedicated compound component for master/sub-toggle hierarchies. The pattern is implemented by combining the `enabled` parameter with visual grouping.

**Source**: [Material Design 3 — Switch Guidelines](https://m3.material.io/components/switch/guidelines), [Material Design 3 — Switch Overview](https://m3.material.io/components/switch/overview), [material-components-android — Switch.md](https://github.com/material-components/material-components-android/blob/master/docs/components/Switch.md)

---

## Implementation Options

### Option 1: Simple `enabled` Parameter (Recommended)

Pass the master toggle's state as the `enabled` parameter to the sub-toggle's row.

```kotlin
// In Settings UI
SwitchSetting(
    title = "Enable Summary",
    checked = enableSummary,
    onCheckedChange = { viewModel.setEnableSummary(it) },
)
SwitchSetting(
    title = "Auto Summary",
    checked = autoSummary,
    enabled = enableSummary,  // <-- dependent on master toggle
    onCheckedChange = { viewModel.setAutoSummary(it) },
)
```

**Pros:**
- Minimal code change; follows existing project patterns
- Material 3 Switch handles disabled visuals natively
- Accessible by default (disabled state announced to screen readers)
- Stored value of Auto Summary is preserved (only `enabled` changes, not `checked`)

**Cons:**
- Only visual grouping implies dependency (no connecting line or indentation)

### Option 2: Indented Sub-Toggle with `enabled`

Same as Option 1, but add left padding to the sub-toggle to visually indicate hierarchy.

```kotlin
SwitchSetting(
    title = "Auto Summary",
    checked = autoSummary,
    enabled = enableSummary,
    modifier = Modifier.padding(start = 16.dp),  // indent
    onCheckedChange = { viewModel.setAutoSummary(it) },
)
```

**Pros:**
- Clearer visual hierarchy
- Common pattern in Android system settings

**Cons:**
- Slightly deviates from flat settings list aesthetic
- May look odd if there's only one sub-toggle

### Option 3: Collapsible Section

When master toggle is OFF, hide the sub-toggle entirely with an animation.

```kotlin
AnimatedVisibility(visible = enableSummary) {
    SwitchSetting(
        title = "Auto Summary",
        checked = autoSummary,
        onCheckedChange = { viewModel.setAutoSummary(it) },
    )
}
```

**Pros:**
- Clean UI; no greyed-out elements
- Clear that sub-toggle is irrelevant when master is OFF

**Cons:**
- **Violates requirements**: FR-005 specifies "greyed out with reduced opacity" not hidden
- User cannot see that Auto Summary exists when Enable Summary is OFF
- Animation adds complexity

### Option 4: Card/Section Grouping

Wrap both toggles in a Card or outlined section to visually group them.

```kotlin
Card {
    SwitchSetting(title = "Enable Summary", ...)
    SwitchSetting(title = "Auto Summary", enabled = enableSummary, ...)
}
```

**Pros:**
- Strong visual grouping
- Works well if more sub-settings are added later

**Cons:**
- Over-engineering for just two toggles
- May not match the existing flat settings list style in the app

### Option 5: Composite Toggle Row

Create a custom composable that shows both toggles in a single logical group with a divider or label.

**Pros:**
- Maximum design control

**Cons:**
- Over-engineering; creates a non-standard component
- Maintenance burden for a single use case

---

## Recommendation

**Option 1: Simple `enabled` Parameter** is the best fit for this spec.

**Rationale:**
1. **Matches requirements exactly**: FR-005 specifies greyed out with reduced opacity and non-interactive — this is exactly what `enabled = false` provides.
2. **Minimal code change**: Aligns with NFR-002 (minimal code impact).
3. **Follows existing patterns**: The Feeder project already uses Switch composables in settings; adding an `enabled` parameter is a natural extension.
4. **Material 3 compliant**: Uses the framework's built-in disabled state, ensuring correct colors, opacity, and accessibility.
5. **Preserves stored value**: The sub-toggle's `checked` value is independent of its `enabled` state, satisfying FR-003/AC-006.

For the DataStore/Flow side, the existing `SettingsStore` pattern of exposing individual `Flow<Boolean>` values works perfectly. Add a new `enableSummary` key with default `true`, and use `combine` in the ViewModel to derive the effective state for both the settings UI and the article view.

---

## Key Implementation Notes

1. **New DataStore key**: `booleanPreferencesKey("enable_summary")` with default `true`
2. **SettingsStore**: Add `val enableSummary: Flow<Boolean>` and `suspend fun setEnableSummary(value: Boolean)`
3. **SettingsViewModel**: Add `enableSummary` to the combined state (it already has 35 params in combine)
4. **Settings UI**: Add new SwitchSetting row above Auto Summary, pass `enableSummary` as `enabled` to Auto Summary row
5. **ArticleViewModel**: Gate summarize button visibility on `enableSummary`, gate auto-summary on `enableSummary && autoSummary`
6. **Alpha for row text**: Apply `Modifier.alpha(if (enabled) 1f else 0.38f)` to the Auto Summary row's text content when disabled
7. **Accessibility**: `enabled = false` on both the Switch and the Row's `toggleable` modifier ensures screen readers announce the disabled state
