# AI Summary Configuration - Developer Guide

**Feature**: AI Summary Configuration
**Version**: 2.17.0 (Unreleased)
**Last Updated**: 2026-01-01

---

## Overview

This guide provides technical documentation for developers working with the AI Summary Configuration feature in Feeder. It covers architecture, implementation details, and extension points.

---

## Architecture

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
│  │  - summaryEnabled: StateFlow<Boolean>                │  │
│  │  - summaryLanguage: StateFlow<String>                │  │
│  │  - setSummaryEnabled(), setSummaryLanguage()         │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Repository.kt                                         │  │
│  │  - summaryEnabled: StateFlow<Boolean>                │  │
│  │  - setSummaryEnabled(value: Boolean)                 │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Data Layer (Persistence)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SettingsStore.kt                                      │  │
│  │  - _summaryEnabled: MutableStateFlow<Boolean>        │  │
│  │  - PREF_SUMMARY_ENABLED: String                      │  │
│  │  - setSummaryEnabled(value: Boolean)                 │  │
│  └──────────────────────┬───────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    SharedPreferences                        │
│  Key: "pref_summary_enabled"                               │
│  Value: Boolean (default: true)                            │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Layer

### SettingsStore.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Key Components**:

```kotlin
// Preference key
const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"

// StateFlow
private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true)
)
val summaryEnabled: StateFlow<Boolean> = _summaryEnabled.asStateFlow()

// Setter
fun setSummaryEnabled(value: Boolean) {
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

**Usage Example**:

```kotlin
// In ViewModel
val summaryEnabled = settingsStore.summaryEnabled
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

fun toggleSummary() {
    settingsStore.setSummaryEnabled(!summaryEnabled.value)
}
```

---

## Business Logic Layer

### Repository.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`

**Key Components**:

```kotlin
// Expose from SettingsStore
val summaryEnabled: StateFlow<Boolean> = settingsStore.summaryEnabled

// Setter
fun setSummaryEnabled(value: Boolean) {
    settingsStore.setSummaryEnabled(value)
}
```

### AIApi.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Key Components**:

```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        // Check if summaries are enabled
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

**Key Components**:

```kotlin
@Composable
fun SummarySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SummarySettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_summary_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Enable/Disable Toggle
            SwitchWithText(
                text = stringResource(R.string.ai_summary_enabled),
                checked = uiState.summaryEnabled,
                onCheckedChange = { viewModel.setSummaryEnabled(it) }
            )

            Spacer(Modifier.height(24.dp))

            // Language Selector
            Text(
                text = stringResource(R.string.ai_summarize),
                style = MaterialTheme.typography.titleMedium
            )
            LanguageSelector(
                currentLanguage = uiState.summaryLanguage,
                onLanguageSelected = { viewModel.setSummaryLanguage(it) }
            )
        }
    }
}
```

### SummarySettingsViewModel.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Key Components**:

```kotlin
class SummarySettingsViewModel(
    private val repository: Repository
) : ViewModel() {

    // State
    val summaryEnabled: StateFlow<Boolean> = repository.summaryEnabled
    val summaryLanguage: StateFlow<String> = repository.summaryLanguage

    // Actions
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

**Location**: `app/src/test/java/com/nononsenseapps/feeder/ai/AIApiTest.kt`

**Example Test**:

```kotlin
@Test
fun `summarize returns error when disabled`() = runTest {
    // Arrange
    val repository = mockk<Repository>()
    val aiApi = AIApi(repository, mockk())
    coEvery { repository.summaryEnabled.first() } returns false

    // Act
    val result = aiApi.summarize("test content")

    // Assert
    assertTrue(result is AIClient.SummaryResult.Error)
}
```

### Integration Tests

**Manual Test Cases**:

1. **Toggle Enable/Disable**:
   - Enable summaries
   - Open article → summary should appear
   - Disable summaries
   - Open article → no summary

2. **Language Selection**:
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
// Add UI component for new setting
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
    val summaryEnabled by viewModel.summaryEnabled.collectAsState()

    Switch(
        checked = summaryEnabled,
        onCheckedChange = { viewModel.setSummaryEnabled(it) }
    )
}
```

### Checking Setting in Business Logic

```kotlin
suspend fun doSomething() {
    val enabled = repository.summaryEnabled.first()
    if (enabled) {
        // Do something
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

- `SettingsStore.kt` - Data persistence
- `Repository.kt` - Business logic
- `AIApi.kt` - AI integration
- `SummarySettingsScreen.kt` - UI
- `SummarySettingsViewModel.kt` - State management
- `OPMLImporter.kt` - Import/Export
- `AIApiTest.kt` - Tests

### Related Documentation

- [AI Summary Settings - User Guide](./AI_SUMMARY_SETTINGS.md)
- [Technical Specification](../specification/04-improve-summary-config/06-technical-specification.md)
- [Implementation Summary](../specification/04-improve-summary-config/09-implementation-summary.md)

---

## Changelog

### Version 2.17.0 (Unreleased)

**Added**:
- Dedicated Summary settings screen
- Enable/disable toggle for AI summaries
- OPML import/export support for summary settings

**Changed**:
- Renamed "Summary Language" to "Summary" in AI Provider section
- Added navigation to dedicated Summary screen

---

**Last Updated**: 2026-01-01
**Document Version**: 1.0
