# AI Summary Configuration - Developer Guide

**Feature**: AI Summary Configuration
**Version**: 2.17.0 (Unreleased)
**Last Updated**: 2026-01-01

---

## Overview

This guide provides technical documentation for developers working with the AI Summary Configuration feature in Feeder. It covers architecture, implementation details, and extension points.

---

## Architecture

### Two-Level Summary Toggle

The summary feature uses a two-level toggle system:

1. **Master Toggle** (`enableSummary` / `PREF_ENABLE_SUMMARY`): Controls whether the summary feature is available at all. When OFF, the summarize button is hidden from the article toolbar and auto-summary is disabled.
2. **Auto Summary Toggle** (`summaryEnabled` / `PREF_SUMMARY_ENABLED`): Controls whether articles are automatically summarized when opened. Only effective when the master toggle is ON.

The translate feature uses a similar two-level toggle system:

1. **Master Toggle** (`enableTranslation` / `PREF_ENABLE_TRANSLATION`): Controls whether the translation feature is available at all. When OFF, the translate button is hidden from the article toolbar and auto-translation is disabled.
2. **Auto Translation Toggle** (`translationEnabled` / `PREF_TRANSLATION_ENABLED`): Controls whether articles are automatically translated when opened. Only effective when the master toggle is ON.

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
│  │ Settings.kt    │  │ AIProvider       │  │ Summary     │ │
│  │                │  │ Section.kt       │  │ Settings    │ │
│  │                │  │                  │  │ Screen.kt   │ │
│  └────────┬───────┘  └────────┬─────────┘  └──────┬──────┘ │
└───────────┼──────────────────┼───────────────────┼──────────┘
            │                  │                   │
            ▼                  ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SummarySettingsViewModel                              │  │
│  │  - enableSummary: StateFlow<Boolean>    (master)     │  │
│  │  - summaryEnabled: StateFlow<Boolean>   (auto)       │  │
│  │  - summaryLanguage: StateFlow<String>                │  │
│  │  - setEnableSummary(), setSummaryEnabled()           │  │
│  │  - setSummaryLanguage()                              │  │
│  └──────────────────────┬───────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ArticleViewModel                                      │  │
│  │  - showSummarize: Boolean  (enableSummary && aiValid)│  │
│  │  - showTranslate: Boolean  (enableTranslation && aiValid) │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Repository.kt                                         │  │
│  │  - enableSummary: StateFlow<Boolean>    (master)     │  │
│  │  - summaryEnabled: StateFlow<Boolean>   (auto)       │  │
│  │  - setSummaryEnabled(value: Boolean)                 │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Data Layer (Persistence)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SettingsStore.kt                                      │  │
│  │  - _enableSummary: MutableStateFlow<Boolean>         │  │
│  │  - PREF_ENABLE_SUMMARY: String  (master toggle)      │  │
│  │  - _summaryEnabled: MutableStateFlow<Boolean>        │  │
│  │  - PREF_SUMMARY_ENABLED: String (auto summary)       │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    SharedPreferences                        │
│  Key: "pref_enable_summary"  → Master toggle (default: true)│
│  Key: "pref_summary_enabled" → Auto summary (default: true) │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Layer

### SettingsStore.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Key Components**:

```kotlin
// Master toggle: controls whether summary feature is available
const val PREF_ENABLE_SUMMARY = "pref_enable_summary"

private val _enableSummary = MutableStateFlow(sp.getBoolean(PREF_ENABLE_SUMMARY, true))
val enableSummary = _enableSummary.asStateFlow()

fun setEnableSummary(value: Boolean) {
    _enableSummary.value = value
    sp.edit().putBoolean(PREF_ENABLE_SUMMARY, value).apply()
}

// Auto summary toggle: controls automatic summarization on article open
const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"

private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true)
)
val summaryEnabled: StateFlow<Boolean> = _summaryEnabled.asStateFlow()

fun setSummaryEnabled(value: Boolean) {
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

**Naming Convention**:
- `enableSummary` / `PREF_ENABLE_SUMMARY` = **master toggle** (enable/disable the entire summary feature)
- `summaryEnabled` / `PREF_SUMMARY_ENABLED` = **auto summary** (automatically summarize on article open)

---

## Business Logic Layer

### Repository.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

**Key Components**:

```kotlin
// Master toggle
val enableSummary = settingsStore.enableSummary

// Auto summary toggle
val summaryEnabled: StateFlow<Boolean> = settingsStore.summaryEnabled

fun setSummaryEnabled(value: Boolean) {
    settingsStore.setSummaryEnabled(value)
}
```

### ArticleViewModel.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Key Components**:

The `ArticleScreenViewState` exposes separate `showSummarize` and `showTranslate` flags:

```kotlin
// In the combine block that builds ArticleScreenViewState:
val showSummarize = enableSummary && aiValid
val showTranslate = enableTranslation && aiValid
```

- `showSummarize`: Only `true` when both the master toggle (`enableSummary`) is ON and a valid AI provider is configured.
- `showTranslate`: Only `true` when both the master toggle (`enableTranslation`) is ON and a valid AI provider is configured.

Auto-summary is triggered when `enableSummary && summaryEnabled` and a per-feed auto-summary flag are all true. Similarly, auto-translation is triggered when `enableTranslation && translationEnabled` and a per-feed auto-translate flag are all true.

### AIApi.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Key Components**:

```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        val enabled = repository.summaryEnabled.first()
        if (!enabled) {
            return AIClient.SummaryResult.Error(content = "")
        }

        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
    }
}
```

**Implementation Notes**:
- Uses `first()` to get current value (one-shot read)
- Returns empty error result when disabled (no API call)
- Preserves existing error handling

---

## UI Layer

### SummarySettingsScreen.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

The Summary Settings screen uses a two-level toggle layout:

```kotlin
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val summaryEnabled by viewModel.summaryEnabled.collectAsStateWithLifecycle()
    val enableSummary by viewModel.enableSummary.collectAsStateWithLifecycle()
    // ...

    Column {
        // Master toggle: Enable Summary
        SwitchSetting(
            title = stringResource(R.string.enable_summary_title),
            checked = enableSummary,
            onCheckedChange = { viewModel.setEnableSummary(it) },
            description = stringResource(R.string.enable_summary_description),
        )

        // Sub-toggle: Auto Summary (dependent on master)
        SwitchSetting(
            title = stringResource(R.string.summary_enabled_title),
            checked = summaryEnabled,
            onCheckedChange = { viewModel.setSummaryEnabled(it) },
            description = stringResource(R.string.summary_enabled_description),
            enabled = enableSummary,  // disabled when master toggle is OFF
        )

        // Language selector, timeout, etc.
    }
}
```

**`SwitchSetting` Disabled State**: When `enabled = false`, the `SwitchSetting` composable renders text with reduced alpha (disabled visual appearance) and prevents interaction.

### ArticleScreen.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

The article toolbar conditionally shows summarize and translate buttons using separate flags:

```kotlin
// Summarize button: only shown when enableSummary is ON and AI provider is valid
if (viewState.showSummarize) {
    // Summarize icon button
}

// Translate button: only shown when enableTranslation is ON and AI provider is valid
if (viewState.showTranslate) {
    // Translate icon button
}
```

### SummarySettingsViewModel.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Key Components**:

```kotlin
class SummarySettingsViewModel(
    private val repository: Repository
) : ViewModel() {

    // Master toggle
    val enableSummary: StateFlow<Boolean> = repository.enableSummary

    // Auto summary toggle
    val summaryEnabled: StateFlow<Boolean> = repository.summaryEnabled
    val summaryLanguage: StateFlow<String> = repository.summaryLanguage

    // Actions
    fun setEnableSummary(value: Boolean) {
        repository.setEnableSummary(value)
    }

    fun setSummaryEnabled(value: Boolean) {
        repository.setSummaryEnabled(value)
    }

    fun setSummaryLanguage(language: String) {
        repository.setSummaryLanguage(language)
    }
}
```

---

## Navigation

### NavigationDestinations.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

**Key Components**:

```kotlin
object SummarySettingsDestination : FeederDestination {
    override val route = "/settings/ai/summary/"
    override val title = "Summary Settings"
}
```

### Usage in Settings.kt

```kotlin
@Composable
fun SettingsScreen(
    // ... other parameters
    onNavigateToSummarySettings: () -> Unit,
) {
    // ... in AIProviderSection
    TextButton(onClick = onNavigateToSummarySettings) {
        Text(stringResource(R.string.ai_summarize))
        Icon(Icons.Default.ChevronRight, null)
    }
}
```

---

## OPML Import/Export

### OPMLImporter.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/model/opml/OPMLImporter.kt`

**Import Logic**:

```kotlin
// Parse AI summary enabled state
val summaryEnabled = element.getElementsByTagNameNS(
    FEEDER_NS,
    "ai_summary_enabled"
).item(0)?.textContent?.toBoolean() ?: true

// Apply to repository
repository.setSummaryEnabled(summaryEnabled)
```

**Export Logic**:

```kotlin
// Add to OPML
val summaryElement = doc.createElementNS(FEEDER_NS, "feeder:ai_summary_enabled")
summaryElement.textContent = if (repository.summaryEnabled.first()) "true" else "false"
opmlElement.appendChild(summaryElement)
```

**OPML Format**:

```xml
<opml version="2.0">
    <head>
        <title>Feeder Subscriptions</title>
        <feeder:ai_provider_type>openai_compatible</feeder:ai_provider_type>
        <feeder:ai_summary_enabled>true</feeder:ai_summary_enabled>
        <feeder:ai_summary_language>en</feeder:ai_summary_language>
    </head>
    <body>
        <!-- feeds -->
    </body>
</opml>
```

---

## Testing

### Unit Tests

**Location**: `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`

**Example Tests**:

```kotlin
@Test
fun enableSummaryDefaultsToTrue() {
    every { sp.getBoolean(PREF_ENABLE_SUMMARY, true) } returns true
    assertEquals(true, store.enableSummary.value)
}

@Test
fun enableSummarySetToFalse() {
    store.setEnableSummary(false)
    assertEquals(false, store.enableSummary.value)
}

@Test
fun enableSummaryIndependentOfSummaryEnabled() {
    // Master toggle and auto summary toggle are independent
    store.setEnableSummary(false)
    assertEquals(false, store.enableSummary.value)
    // summaryEnabled is unaffected
}
```

### Integration Tests

**Manual Test Cases**:

1. **Master Toggle Enable/Disable**:
   - Enable summary master toggle
   - Open article → summarize button visible in toolbar
   - Disable summary master toggle
   - Open article → summarize button hidden, translate button still visible

2. **Auto Summary Toggle**:
   - Enable both master toggle and auto summary
   - Open article → summary generates automatically
   - Disable auto summary (master still ON)
   - Open article → summarize button visible but no auto-summary

3. **Disabled State Visual**:
   - Turn OFF master toggle
   - Auto Summary sub-toggle should appear visually disabled (reduced alpha)
   - Auto Summary sub-toggle should not respond to taps

4. **Language Selection**:
   - Change summary language
   - Open article → summary in new language

3. **OPML Export**:
   - Configure settings
   - Export OPML
   - Verify `<feeder:ai_summary_enabled>` present

4. **OPML Import**:
   - Import OPML with settings
   - Verify settings applied correctly

---

## Extension Points

### Adding New Summary Settings

To add a new summary-related setting:

1. **Add to SettingsStore**:
```kotlin
const val PREF_SUMMARY_NEW_SETTING = "pref_summary_new_setting"

private val _summaryNewSetting = MutableStateFlow(
    sp.getString(PREF_SUMMARY_NEW_SETTING, "default")
)
val summaryNewSetting: StateFlow<String> = _summaryNewSetting.asStateFlow()

fun setSummaryNewSetting(value: String) {
    _summaryNewSetting.value = value
    sp.edit().putString(PREF_SUMMARY_NEW_SETTING, value).apply()
}
```

2. **Expose in Repository**:
```kotlin
val summaryNewSetting: StateFlow<String> = settingsStore.summaryNewSetting
fun setSummaryNewSetting(value: String) {
    settingsStore.setSummaryNewSetting(value)
}
```

3. **Add to ViewModel**:
```kotlin
val summaryNewSetting: StateFlow<String> = repository.summaryNewSetting
fun setSummaryNewSetting(value: String) {
    repository.setSummaryNewSetting(value)
}
```

4. **Add UI in SummarySettingsScreen**:
```kotlin
// If the new setting should be gated by the master toggle,
// pass enabled = enableSummary to the SwitchSetting composable
SwitchSetting(
    title = "...",
    checked = newSettingValue,
    onCheckedChange = { viewModel.setSummaryNewSetting(it) },
    enabled = enableSummary,  // disabled when master toggle is OFF
)
```

5. **Update OPML Import/Export**:
```kotlin
// Import
val newSetting = element.getElementsByTagNameNS(
    FEEDER_NS,
    "ai_summary_new_setting"
).item(0)?.textContent ?: "default"

// Export
val newSettingElement = doc.createElementNS(FEEDER_NS, "feeder:ai_summary_new_setting")
newSettingElement.textContent = repository.summaryNewSetting.first()
opmlElement.appendChild(newSettingElement)
```

### Adding Per-Feed Summary Settings

To implement per-feed summary settings (future enhancement):

1. **Database Schema**:
```sql
ALTER TABLE feeds ADD COLUMN summary_enabled INTEGER DEFAULT 1;
ALTER TABLE feeds ADD COLUMN summary_language TEXT DEFAULT NULL;
```

2. **Repository Layer**:
```kotlin
fun getFeedSummaryEnabled(feedId: Long): StateFlow<Boolean>
fun setFeedSummaryEnabled(feedId: Long, enabled: Boolean)
```

3. **UI Changes**:
   - Add settings to Feed Edit Screen
   - Show indicator in feed list

---

## Performance Considerations

### StateFlow Emissions

- `summaryEnabled` is a `StateFlow` (not `SharedFlow`)
- Only emits when value actually changes
- Efficient for UI observation

### API Call Prevention

- Check `summaryEnabled` **before** making API call
- Use `first()` for one-shot read (not continuous observation)
- Saves API costs and improves performance

### Memory Management

- ViewModels use `viewModelScope`
- StateFlow cleaned up automatically
- No memory leaks

---

## Common Patterns

### Observing Settings in Compose

```kotlin
@Composable
fun MyScreen() {
    val viewModel: SummarySettingsViewModel = viewModel()
    val enableSummary by viewModel.enableSummary.collectAsStateWithLifecycle()
    val summaryEnabled by viewModel.summaryEnabled.collectAsStateWithLifecycle()

    // Master toggle
    Switch(
        checked = enableSummary,
        onCheckedChange = { viewModel.setEnableSummary(it) }
    )

    // Sub-toggle, disabled when master is OFF
    Switch(
        checked = summaryEnabled,
        onCheckedChange = { viewModel.setSummaryEnabled(it) },
        enabled = enableSummary
    )
}
```

### Checking Setting in Business Logic

```kotlin
suspend fun doSomething() {
    val masterEnabled = repository.enableSummary.first()
    val autoEnabled = repository.summaryEnabled.first()
    if (masterEnabled && autoEnabled) {
        // Auto-summarize
    }
}
```

### Migrating Settings

```kotlin
// Migrate from old preference to new
fun migrateSettings(sp: SharedPreferences) {
    if (!sp.contains("pref_summary_enabled")) {
        // Migrate from old preference
        val oldValue = sp.getBoolean("old_pref", true)
        sp.edit().putBoolean("pref_summary_enabled", oldValue).apply()
    }
}
```

---

## Debugging

### Logging

Add logging to track summary state:

```kotlin
fun setSummaryEnabled(value: Boolean) {
    Log.d("SummarySettings", "Setting summary enabled to: $value")
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

### Inspection

Use Android Studio's Layout Inspector to verify:
- UI state updates correctly
- Toggle reflects actual setting value

### Database Inspection

```bash
adb shell
run-as com.nononsenseapps.feeder
cat shared_prefs/com.nononsenseapps.feeder_preferences.xml
```

Look for:
```xml
<boolean name="pref_enable_summary" value="true" />
<boolean name="pref_summary_enabled" value="true" />
```

---

## Migration Guide

### From Previous Version

No migration needed. Feature is backward compatible:
- Existing users have summaries enabled by default
- Old preferences remain unchanged
- New preferences use sensible defaults

### For Custom OPML Handling

If you have custom OPML import/export:

```kotlin
// Handle missing element gracefully
val summaryEnabled = element.getElementsByTagNameNS(
    FEEDER_NS,
    "ai_summary_enabled"
).item(0)?.textContent?.toBoolean() ?: true // Default to true
```

---

## References

### Related Files

- `SettingsStore.kt` - Data persistence (both `enableSummary` and `summaryEnabled`)
- `Repository.kt` - Business logic
- `AIApi.kt` - AI integration
- `ArticleViewModel.kt` - `showSummarize`/`showTranslate` split logic
- `ArticleScreen.kt` - Toolbar button visibility
- `SummarySettingsScreen.kt` - UI (two-level toggle layout)
- `SummarySettingsViewModel.kt` - State management
- `OPMLImporter.kt` - Import/Export
- `SettingsStoreTest.kt` - Unit tests for `enableSummary`

### Related Documentation

- [AI Summary Settings - User Guide](./AI_SUMMARY_SETTINGS.md)
- [Technical Specification](../specification/04-improve-summary-config/06-technical-specification.md)
- [Implementation Summary](../specification/04-improve-summary-config/09-implementation-summary.md)

---

## Changelog

### Version 2.17.0 (Unreleased)

**Added**:
- Dedicated Summary settings screen
- Master "Enable Summary" toggle (`enableSummary` / `PREF_ENABLE_SUMMARY`) to control summary feature availability
- Auto summary toggle (`summaryEnabled` / `PREF_SUMMARY_ENABLED`) now depends on master toggle
- Separate `showSummarize` and `showTranslate` flags in `ArticleScreenViewState`
- `SwitchSetting` supports `enabled` parameter with disabled text alpha
- OPML import/export support for summary settings

**Changed**:
- Summarize button in article toolbar now hidden when master toggle is OFF
- Translate button is gated by `enableTranslation` master toggle (only needs valid AI provider + translation enabled)
- Auto Summary toggle appears visually disabled when master toggle is OFF

---

**Last Updated**: 2026-01-01
**Document Version**: 1.0
