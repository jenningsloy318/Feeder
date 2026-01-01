# Research Report: AI Summary Configuration Best Practices

**Created:** 2026-01-01 19:12:20 +08:00
**Current Date:** 2026-01-01
**Feature:** Improve AI Integration Summary Configuration
**Status:** Research Complete
**Phase:** 3

---

## Executive Summary

This report documents research findings on best practices for implementing AI summary configuration in Android apps using Jetpack Compose and Material Design 3. The research covers existing codebase patterns, industry best practices, and technical implementation strategies.

---

## Table of Contents

1. [Existing Codebase Analysis](#existing-codebase-analysis)
2. [Jetpack Compose Settings Best Practices](#jetpack-compose-settings-best-practices)
3. [Material Design 3 Switch Components](#material-design-3-switch-components)
4. [Android Navigation Patterns](#android-navigation-patterns)
5. [State Management Approaches](#state-management-approaches)
6. [Summary Generation Flow](#summary-generation-flow)
7. [Recommendations](#recommendations)

---

## Existing Codebase Analysis

### Current Architecture

**Summary Generation Flow:**
```
AIApi.summarize(content)
  ↓
  repository.summaryLanguage.first()
  ↓
  client.generateSummary(content, language)
  ↓
  Returns SummaryResult
```

**Key Files:**
1. **AIApi.kt** - High-level AI operations
2. **SettingsStore.kt** - SharedPreferences wrapper
3. **SummaryLanguage.kt** - Language enum
4. **AIProviderSection.kt** - Current UI implementation
5. **NavigationDestinations.kt** - Navigation registry

### Existing Settings Patterns

**Provider List Screen Pattern** (`ProviderListScreen.kt`):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    onNavigateUp: () -> Unit,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToAddProvider: (AIProvider) -> Unit,
    viewModel: ProviderListViewModel,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(id = R.string.provider_list_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, ...)
                    }
                },
            )
        },
        floatingActionButton = { ... }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(providers) { provider ->
                // Provider item with swipe-to-dismiss
            }
        }
    }
}
```

**Navigation Pattern** (`NavigationDestinations.kt`):
```kotlin
data object ProviderListDestination : NavigationDestination(
    path = "settings/providers",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    fun navigate(navController: NavController) {
        navController.navigate(path) {
            launchSingleTop = true
        }
    }

    @Composable
    override fun RegisterScreen(...) {
        val viewModel = backStackEntry.diAwareViewModel<ProviderListViewModel>()
        ProviderListScreen(
            onNavigateUp = { ... },
            viewModel = viewModel,
        )
    }
}
```

**SettingsStore Pattern** (`SettingsStore.kt`):
```kotlin
private val _summaryLanguage = MutableStateFlow(
    SummaryLanguage.fromCode(sp.getString(PREF_SUMMARY_LANGUAGE, null)),
)
val summaryLanguage = _summaryLanguage.asStateFlow()

fun setSummaryLanguage(value: SummaryLanguage) {
    _summaryLanguage.value = value
    sp.edit().putString(PREF_SUMMARY_LANGUAGE, value.code).apply()
}
```

### Key Observations

1. **State Management:** Uses StateFlow + collectAsStateWithLifecycle
2. **Navigation:** Custom NavigationDestination pattern
3. **Persistence:** SharedPreferences via SettingsStore
4. **DI:** Manual DI using `diAwareViewModel()`
5. **UI Pattern:** Scaffold + TopAppBar + LazyColumn
6. **Material 3:** Using Material3 components

---

## Jetpack Compose Settings Best Practices

### 1. Settings Screen Structure

**Recommended Pattern:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            SensibleTopAppBar(
                title = stringResource(R.string.settings_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, ...)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings items
        }
    }
}
```

**Best Practices:**
- ✅ Use `Scaffold` with `TopAppBar`
- ✅ Use `LazyColumn` for scrollable content
- ✅ Apply `contentPadding` for proper spacing
- ✅ Use `Arrangement.spacedBy` for consistent gaps
- ✅ Implement proper navigation (up button)
- ✅ Support scroll behavior for top bar

### 2. Settings Item Pattern

**Switch Setting Item:**
```kotlin
@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
```

**Best Practices:**
- ✅ Make entire row clickable
- ✅ Use proper spacing (16.dp vertical padding)
- ✅ Support title and subtitle
- ✅ Right-align switch
- ✅ Use appropriate typography styles

### 3. Dropdown Selector Pattern

**Language Selector:**
```kotlin
@Composable
fun DropdownSelector(
    title: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
```

**Best Practices:**
- ✅ Use `ExposedDropdownMenuBox` for Material 3
- ✅ Make text field read-only
- ✅ Show trailing icon
- ✅ Dismiss menu on selection
- ✅ Use proper menu anchor

---

## Material Design 3 Switch Components

### Switch Component API

**Basic Switch:**
```kotlin
Switch(
    checked = checked,
    onCheckedChange = { checked = it },
    enabled = enabled,
    colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
        uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
    )
)
```

**Switch with Custom Colors:**
```kotlin
Switch(
    checked = checked,
    onCheckedChange = { checked = it },
    colors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
)
```

### Best Practices for Switches

1. **Labeling:**
   - ✅ Always provide content description for accessibility
   - ✅ Use descriptive titles (e.g., "Enable Summaries" not "Toggle")
   - ✅ Add subtitle for additional context

2. **State Management:**
   - ✅ Use `remember` for local state
   - ✅ Connect to ViewModel for persistent state
   - ✅ Update state immediately on user interaction

3. **Accessibility:**
   - ✅ Minimum touch target: 48dp
   - ✅ Semantic role: Role.Switch
   - ✅ State description: "On" / "Off"

4. **Visual Design:**
   - ✅ Use Material 3 color scheme
   - ✅ Respect theme colors (primary, surface, etc.)
   - ✅ Provide visual feedback

---

## Android Navigation Patterns

### Navigation Component Integration

**Destination Registration:**
```kotlin
data object SummarySettingsDestination : NavigationDestination(
    path = "settings/summary",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    fun navigate(navController: NavController) {
        navController.navigate(path) {
            launchSingleTop = true
        }
    }

    @Composable
    override fun RegisterScreen(
        navController: NavController,
        backStackEntry: NavBackStackEntry,
        navDrawerListState: LazyListState,
    ) {
        val viewModel = backStackEntry.diAwareViewModel<SummarySettingsViewModel>()

        SummarySettingsScreen(
            onNavigateUp = {
                if (!navController.popBackStack()) {
                    SettingsDestination.navigate(navController)
                }
            },
            viewModel = viewModel,
        )
    }
}
```

### Best Practices

1. **Route Naming:**
   - ✅ Use hierarchical routes (e.g., `settings/summary`)
   - ✅ Follow existing pattern (`settings/...`)
   - ✅ Keep routes descriptive and short

2. **Navigation Handling:**
   - ✅ Always provide `onNavigateUp` callback
   - ✅ Fall back to parent screen if popBackStack fails
   - ✅ Use `launchSingleTop = true` to prevent duplicates

3. **ViewModel Scoping:**
   - ✅ Use `diAwareViewModel()` for consistent scoping
   - ✅ Scope to navigation back stack entry
   - ✅ Properly dispose on navigation away

---

## State Management Approaches

### ViewModel Pattern

**ViewModel Implementation:**
```kotlin
class SummarySettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    // StateFlows from SettingsStore
    val summaryEnabled: StateFlow<Boolean> = settingsStore.summaryEnabled
    val summaryLanguage: StateFlow<SummaryLanguage> = settingsStore.summaryLanguage

    // Actions
    fun setSummaryEnabled(enabled: Boolean) {
        settingsStore.setSummaryEnabled(enabled)
    }

    fun setSummaryLanguage(language: SummaryLanguage) {
        settingsStore.setSummaryLanguage(language)
    }
}
```

### State Collection in UI

**Recommended Pattern:**
```kotlin
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
) {
    val summaryEnabled by viewModel.summaryEnabled
        .collectAsStateWithLifecycle()

    val summaryLanguage by viewModel.summaryLanguage
        .collectAsStateWithLifecycle()

    // UI implementation
}
```

**Best Practices:**
- ✅ Use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- ✅ Collect StateFlow, not raw values
- ✅ Let ViewModel handle business logic
- ✅ Keep UI stateless

---

## Summary Generation Flow

### Current Implementation

**AIApi.summarize():**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: "")
    }
}
```

### Integration Points

**Where Summaries Are Generated:**
1. **Article Reading** - When user opens an article
2. **Feed Sync** - When new articles are fetched
3. **Manual Trigger** - User explicitly requests summary

**Impact of Enable/Disable Toggle:**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    // Check if summaries are enabled
    val enabled = repository.summaryEnabled.first()
    if (!enabled) {
        return AIClient.SummaryResult.Error(content = "Summaries are disabled")
    }

    return try {
        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: "")
    }
}
```

**Recommended Implementation:**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        val enabled = repository.summaryEnabled.first()
        if (!enabled) {
            return AIClient.SummaryResult.Error(
                content = "",
            )
        }

        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: "")
    }
}
```

---

## Recommendations

### 1. UI Structure

**Summary Settings Screen Layout:**
```
┌─────────────────────────────────────┐
│ ← Summary Settings            [⋮]   │ TopAppBar
├─────────────────────────────────────┤
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Enable Summaries          [ON] │ │ Switch
│ │ Automatically generate AI...    │ │ Subtitle
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Language                    [▼] │ │ Dropdown
│ │ English                          │ │ Selected value
│ └─────────────────────────────────┘ │
│                                     │
│ Additional options (future):       │
│ • Summary length                   │
│ • Summary style                    │
│ • Per-feed settings                │
└─────────────────────────────────────┘
```

### 2. Component Breakdown

**Create the following composables:**
1. `SummarySettingsScreen` - Main screen
2. `SummarySettingsViewModel` - State management
3. `SwitchSettingItem` - Reusable switch component
4. `LanguageSelector` - Dropdown for language selection

**File Structure:**
```
app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/
├── SummarySettingsScreen.kt          # New
├── SummarySettingsViewModel.kt       # New
├── SwitchSettingItem.kt              # New (or reuse existing)
└── AIProviderSection.kt              # Modify (add navigation)
```

### 3. Implementation Priority

**Phase 1: Data Layer (SettingsStore.kt)**
```kotlin
const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"

private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true)
)
val summaryEnabled = _summaryEnabled.asStateFlow()

fun setSummaryEnabled(value: Boolean) {
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

**Phase 2: ViewModel (SummarySettingsViewModel.kt)**
```kotlin
class SummarySettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val summaryEnabled: StateFlow<Boolean> = settingsStore.summaryEnabled
    val summaryLanguage: StateFlow<SummaryLanguage> = settingsStore.summaryLanguage

    fun setSummaryEnabled(enabled: Boolean) {
        settingsStore.setSummaryEnabled(enabled)
    }

    fun setSummaryLanguage(language: SummaryLanguage) {
        settingsStore.setSummaryLanguage(language)
    }
}
```

**Phase 3: Navigation (NavigationDestinations.kt)**
```kotlin
data object SummarySettingsDestination : NavigationDestination(
    path = "settings/summary",
    navArguments = emptyList(),
    deepLinks = emptyList(),
) {
    // ... implementation
}
```

**Phase 4: UI (SummarySettingsScreen.kt)**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
) {
    // ... implementation following ProviderListScreen pattern
}
```

**Phase 5: Integration (AIProviderSection.kt)**
```kotlin
// Modify SummaryLanguageSectionItem to navigate
@Composable
private fun SummaryLanguageSectionItem(
    summaryLanguage: SummaryLanguage,
    summaryEnabled: Boolean,
    onEvent: (AISettingsEvent) -> Unit,
    onNavigateToSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable { onNavigateToSummary() }
            .semantics { role = Role.Button },
        // ...
    ) {
        TitleAndSubtitle(
            title = { Text(stringResource(R.string.summary_title)) },
            subtitle = {
                Text(
                    text = if (summaryEnabled) {
                        stringResource(id = summaryLanguage.displayName)
                    } else {
                        stringResource(R.string.summary_status_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}
```

### 4. String Resources

**Add to `strings.xml`:**
```xml
<!-- Main Settings Screen -->
<string name="summary_title">Summary</string>
<string name="summary_subtitle">Configure AI-generated summaries</string>
<string name="summary_status_enabled">%1$s (Enabled)</string>
<string name="summary_status_disabled">Disabled</string>

<!-- Summary Settings Screen -->
<string name="summary_settings_title">Summary Settings</string>
<string name="summary_enabled_title">Enable Summaries</string>
<string name="summary_enabled_description">Automatically generate AI summaries for articles</string>
<string name="summary_language_title">Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>
```

### 5. Accessibility

**Ensure:**
- ✅ All switches have content descriptions
- ✅ Minimum touch target: 48dp
- ✅ Proper semantics (Role.Switch, Role.Button)
- ✅ Screen reader support
- ✅ Sufficient color contrast

### 6. Testing Strategy

**Unit Tests:**
- ViewModel state changes
- SettingsStore persistence
- Language code parsing

**UI Tests:**
- Navigation flow
- Switch toggle interaction
- Dropdown selection
- State persistence

**Integration Tests:**
- End-to-end settings flow
- Summary generation with enabled/disabled states

---

## Industry Best Practices

### Android Settings Guidelines

**From Material Design:**
1. **Group related settings** - Use sections and dividers
2. **Provide clear labels** - Use descriptive titles
3. **Show current values** - Display selected options
4. **Use appropriate controls** - Switch for binary, dropdown for choices
5. **Explain settings** - Add helpful descriptions
6. **Respect user preferences** - Default to safe options

### Jetpack Compose Best Practices

**State Management:**
- ✅ Use StateFlow for reactive state
- ✅ Collect with lifecycle awareness
- ✅ Keep UI stateless
- ✅ Single source of truth

**Performance:**
- ✅ Avoid recomposition with proper keys
- ✅ Use `remember` for expensive computations
- ✅ LazyColumn for long lists
- ✅ Proper modifiers usage

**Code Organization:**
- ✅ Separate UI from business logic
- ✅ Reusable composables
- ✅ Clear file structure
- ✅ Consistent naming conventions

---

## Technical Considerations

### 1. Backward Compatibility

**Migration Strategy:**
```kotlin
// Default to enabled for existing users
private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true) // Default: true
)
```

**No Data Migration Needed:**
- Existing `summaryLanguage` setting preserved
- New `summaryEnabled` defaults to `true` (maintain current behavior)
- No breaking changes

### 2. Performance

**Optimizations:**
- ✅ Use StateFlow (not LiveData)
- ✅ Collect with lifecycle awareness
- ✅ Avoid blocking operations
- ✅ Cache summary results

### 3. Error Handling

**Graceful Degradation:**
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
        AIClient.SummaryResult.Error(content = e.message ?: "")
    }
}
```

---

## Conclusion

This research has identified:

1. **Clear patterns** in the existing codebase to follow
2. **Best practices** for Jetpack Compose settings screens
3. **Material Design 3** component usage
4. **Implementation strategy** aligned with project conventions

**Key Recommendations:**
- Follow existing Provider List screen pattern
- Use Material 3 Switch and Dropdown components
- Implement proper state management with StateFlow
- Ensure accessibility compliance
- Maintain backward compatibility

**Next Steps:**
1. ✅ Complete Phase 4: Skip (not a bug fix)
2. ⏭️ Phase 5: Code Assessment
3. ⏭️ Phase 5.3: Architecture Design
4. ⏭️ Phase 5.5: UI/UX Design

---

**References:**
- Existing codebase analysis
- Jetpack Compose documentation
- Material Design 3 guidelines
- Android Architecture Components
- Project dev rules: `00-dev-rules.md`

---

**Research Complete:** 2026-01-01 19:12:30 +08:00
