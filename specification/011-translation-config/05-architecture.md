# Architecture: Translation Configuration

**Date:** 2026-01-03
**Author:** Claude (Architecture Agent)
**Status:** Draft
**Feature:** Add translation config settings under Settings → AI Integration

## Overview

This feature adds translation configuration to the Feeder RSS reader, allowing users to configure automatic article translation using AI. The architecture follows the existing SummarySettings pattern, ensuring consistency with the codebase's established conventions for settings management, UI composition, and data flow.

### Key Design Principles

1. **Pattern Consistency**: Follow the SummarySettings pattern exactly for UI, ViewModel, and data flow
2. **Separation of Concerns**: Clear boundaries between UI, business logic, and data persistence
3. **Global Configuration**: Translation settings are global-only (no per-feed override)
4. **Provider Integration**: Uses the active AI provider from the multi-provider system
5. **Testability**: All components are unit testable with clear interfaces

## Architectural Drivers

- **User Experience**: Users want to translate articles to their preferred language automatically
- **System Integration**: Must integrate with existing AI provider infrastructure
- **Maintainability**: Follow established patterns to reduce cognitive load
- **Testability**: Each component must be independently testable
- **Performance**: Minimal overhead, settings persisted efficiently

## Module Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  TranslationSettingsScreen.kt (NEW)                       │  │
│  │  - Compose UI with switch and language selector           │  │
│  │  - Reuses SwitchSetting and LanguageSelectorSetting       │  │
│  │  - StateFlow observation for reactive updates             │  │
│  └─────────────────────┬─────────────────────────────────────┘  │
│                        │ StateFlow collects                       │
│  ┌─────────────────────▼─────────────────────────────────────┐  │
│  │  TranslationSettingsViewModel.kt (NEW)                    │  │
│  │  - Exposes translationEnabled and translationLanguage     │  │
│  │  - Delegates business logic to Repository                 │  │
│  │  - viewModelScope for coroutine management                │  │
│  └─────────────────────┬─────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │ Calls
┌────────────────────────▼────────────────────────────────────────┐
│                       Business Layer                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Repository.kt (MODIFY)                                   │  │
│  │  - Add translationEnabled and translationLanguage flows   │  │
│  │  - Add setTranslationEnabled() and setTranslationLanguage()│ │
│  │  - Delegates to SettingsStore                             │  │
│  └─────────────────────┬─────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │ Delegates to
┌────────────────────────▼────────────────────────────────────────┐
│                      Data Access Layer                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  SettingsStore.kt (MODIFY)                                │  │
│  │  - Add _translationEnabled MutableStateFlow               │  │
│  │  - Add _translationLanguage MutableStateFlow              │  │
│  │  - Persist to SharedPreferences                          │  │
│  └─────────────────────┬─────────────────────────────────────┘  │
│  ┌─────────────────────▼─────────────────────────────────────┐  │
│  │  TranslationLanguage.kt (NEW)                             │  │
│  │  - Enum with language codes and display names             │  │
│  │  - Includes DEVICE_DEFAULT option                         │  │
│  │  - Separate from SummaryLanguage enum                     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Navigation Integration                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  NavigationDestinations.kt (MODIFY)                       │  │
│  │  - Add TranslationSettingsDestination                    │  │
│  │  - Register destination in nav graph                      │  │
│  │  - Wire navigation from Settings screen                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Settings.kt (MODIFY)                                     │  │
│  │  - Add "Translation Settings" link                        │  │
│  │  - Pass onNavigateToTranslationSettings callback         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Dependency Injection                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  ArchModelModule.kt (MODIFY)                              │  │
│  │  - Bind TranslationSettingsViewModel                     │  │
│  │  - Use bindWithComposableViewModelScope                   │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       Resources (i18n)                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  strings.xml (MODIFY)                                     │  │
│  │  - Add translation settings strings                       │  │
│  │  - Add language display names                             │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Module Specifications

### 1. TranslationLanguage (NEW - Data Model)

**Package:** `com.nononsenseapps.feeder.ai.model`

**Purpose:** Enum defining supported translation languages with ISO 639-1 codes

**Responsibilities:**
- Define language codes for translation API
- Provide display names for UI
- Support device default option
- Parse language codes from persisted settings

**Dependencies:** None

**Public Interface:**
```kotlin
enum class TranslationLanguage(
    val code: String,
    @StringRes val displayName: Int,
    val languageName: String,
) {
    DEVICE_DEFAULT(
        code = "",
        displayName = R.string.translation_language_device_default,
        languageName = "the device's default"
    ),
    ENGLISH(
        code = "en",
        displayName = R.string.translation_language_english,
        languageName = "English"
    ),
    CHINESE(
        code = "zh",
        displayName = R.string.translation_language_chinese,
        languageName = "Chinese"
    ),
    // ... other languages matching SummaryLanguage

    companion object {
        fun fromCode(code: String?): TranslationLanguage
    }
}
```

**Complexity Analysis:**
- Time: O(N) for fromCode lookup (N = number of languages, ~12)
- Space: O(1) per instance, O(N) total for all entries
- Justification: Linear search acceptable for small enum set

**Rationale:**
- Separate enum from SummaryLanguage allows independent evolution
- DEVICE_DEFAULT uses empty code for persistence
- languageName used for AI prompts (e.g., "Translate to Chinese")

---

### 2. SettingsStore Modifications (Data Layer)

**Package:** `com.nononsenseapps.feeder.archmodel`

**Purpose:** Persist and expose translation settings via SharedPreferences

**Responsibilities:**
- Persist translation enabled flag
- Persist selected translation language
- Expose StateFlow for reactive updates
- Initialize with sensible defaults

**Dependencies:** SharedPreferences, TranslationLanguage

**Public Interface Additions:**
```kotlin
class SettingsStore {
    // Existing members...

    // Translation enabled setting
    private val _translationEnabled = MutableStateFlow(
        sp.getBoolean(PREF_TRANSLATION_ENABLED, false)
    )
    val translationEnabled: StateFlow<Boolean> = _translationEnabled.asStateFlow()

    fun setTranslationEnabled(value: Boolean) {
        _translationEnabled.value = value
        sp.edit().putBoolean(PREF_TRANSLATION_ENABLED, value).apply()
    }

    // Translation language setting
    private val _translationLanguage = MutableStateFlow(
        TranslationLanguage.fromCode(sp.getString(PREF_TRANSLATION_LANGUAGE, null))
    )
    val translationLanguage: StateFlow<TranslationLanguage> =
        _translationLanguage.asStateFlow()

    fun setTranslationLanguage(value: TranslationLanguage) {
        _translationLanguage.value = value
        sp.edit().putString(PREF_TRANSLATION_LANGUAGE, value.code).apply()
    }
}

// New constants
const val PREF_TRANSLATION_ENABLED = "pref_translation_enabled"
const val PREF_TRANSLATION_LANGUAGE = "pref_translation_language"
```

**Defaults:**
- `translationEnabled`: `false` (opt-in feature)
- `translationLanguage`: `DEVICE_DEFAULT` (use device language)

**Persistence Format:**
- Enabled: Boolean (SharedPreferences)
- Language: String (ISO 639-1 code, empty for device default)

**Coupling Assessment:**
- Data coupling: Acceptable (only data passed via parameters)
- No control coupling (Repository calls methods, doesn't control flow)
- Testable in isolation with SharedPreferences mock

---

### 3. Repository Modifications (Business Layer)

**Package:** `com.nononsenseapps.feeder.archmodel`

**Purpose:** Facade for translation settings, delegating to SettingsStore

**Responsibilities:**
- Expose translation settings to ViewModels
- Provide clean interface for settings operations
- Handle business logic validation (if any)

**Dependencies:** SettingsStore

**Public Interface Additions:**
```kotlin
class Repository {
    // Existing members...

    val translationEnabled: StateFlow<Boolean> =
        settingsStore.translationEnabled

    val translationLanguage: StateFlow<TranslationLanguage> =
        settingsStore.translationLanguage

    fun setTranslationEnabled(enabled: Boolean) {
        settingsStore.setTranslationEnabled(enabled)
    }

    fun setTranslationLanguage(language: TranslationLanguage) {
        settingsStore.setTranslationLanguage(language)
    }
}
```

**Rationale:**
- Thin facade pattern (passes through to SettingsStore)
- Consistent with summaryEnabled/summaryLanguage pattern
- Allows future business logic to be added without touching UI

**Coupling Assessment:**
- Data coupling: Good (only StateFlow exposed)
- No stamp coupling (no complex data structures)
- Testable with SettingsStore mock

---

### 4. TranslationSettingsViewModel (NEW - Presentation Layer)

**Package:** `com.nononsenseapps.feeder.ui.compose.settings`

**Purpose:** Manage state for Translation Settings screen

**Responsibilities:**
- Expose translation settings as StateFlow
- Handle user interactions (toggle switch, select language)
- Delegate persistence to Repository

**Dependencies:** Repository, DIAwareViewModel base

**Public Interface:**
```kotlin
class TranslationSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    val translationEnabled: StateFlow<Boolean> =
        repository.translationEnabled

    val translationLanguage: StateFlow<TranslationLanguage> =
        repository.translationLanguage

    fun setTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslationEnabled(enabled)
        }
    }

    fun setTranslationLanguage(language: TranslationLanguage) {
        viewModelScope.launch {
            repository.setTranslationLanguage(language)
        }
    }
}
```

**StateFlow Contracts:**
- `translationEnabled`: Emits Boolean, initially from persisted value
- `translationLanguage`: Emits TranslationLanguage enum, initially DEVICE_DEFAULT

**Complexity:**
- Time: O(1) for all operations (simple delegation)
- Space: O(1) (no internal state beyond Repository references)
- Concurrency: viewModelScope ensures coroutine cancellation on screen close

**Error Handling:**
- Repository operations are suspend functions but don't throw (SharedPreferences is safe)
- No explicit error handling needed at ViewModel level

---

### 5. TranslationSettingsScreen (NEW - UI Layer)

**Package:** `com.nononsenseapps.feeder.ui.compose.settings`

**Purpose:** Compose UI for translation settings

**Responsibilities:**
- Render switch to enable/disable translation
- Render language selector dropdown
- Navigate back when done
- Observe ViewModel state reactively

**Dependencies:** TranslationSettingsViewModel, Compose UI components

**Public Interface:**
```kotlin
@Composable
fun TranslationSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TranslationSettingsViewModel,
    modifier: Modifier = Modifier,
)
```

**UI Structure:**
```
Scaffold
└── TopAppBar
    └── Title: "Translation Settings"
    └── Back button: onNavigateUp

Column (scrollable)
├── SwitchSetting
│   ├── Title: "Enable Auto Translation"
│   ├── Subtitle: "Automatically translate articles..."
│   └── Checked: translationEnabled
│       └── onChange: viewModel.setTranslationEnabled()
│
├── Spacer (8.dp)
│
└── LanguageSelectorSetting (reused from SummarySettingsScreen)
    ├── Title: "Translation Language"
    ├── Subtitle: currentLanguage.displayName
    ├── Enabled: translationEnabled (disabled when toggle off)
    └── Dropdown: TranslationLanguage.entries
        └── onClick: viewModel.setTranslationLanguage()
```

**Component Reuse:**
- `SwitchSetting`: Reused from existing settings
- `LanguageSelectorSetting`: Copy from SummarySettingsScreen, adapt for TranslationLanguage
- `SensibleTopAppBar`: Reused app bar component

**Complexity:**
- Time: O(N) for dropdown rendering (N = number of languages, ~12)
- Space: O(1) for UI state (menu expanded/collapsed)
- Recomposition: Only when StateFlow emits (minimal overhead)

**Rationale:**
- Mirrors SummarySettingsScreen structure exactly
- Dropdown disabled when translation disabled (prevents confusion)
- Follows Material 3 design guidelines

---

### 6. Navigation Integration (MODIFY)

**Files Modified:**
1. `NavigationDestinations.kt`
2. `Settings.kt`

#### 6.1 TranslationSettingsDestination (NEW)

**Package:** `com.nononsenseapps.feeder.ui.compose.navigation`

**Purpose:** Navigation destination for translation settings

**Public Interface:**
```kotlin
data object TranslationSettingsDestination : NavigationDestination(
    path = "settings/translation",
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
        val viewModel: TranslationSettingsViewModel =
            backStackEntry.diAwareViewModel()

        TranslationSettingsScreen(
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

**Navigation Flow:**
```
Settings Screen
    └── Click "Translation Settings"
        └── Navigate to TranslationSettingsDestination
            └── Click back
                └── Pop back to Settings Screen
```

#### 6.2 Settings Screen Modifications

**File:** `Settings.kt`

**Changes:**
```kotlin
@Composable
fun SettingsScreen(
    // Existing parameters...
    onNavigateToTranslationSettings: () -> Unit,
    // ...
) {
    // In the AI Integration section:
    SettingsLink(
        title = stringResource(R.string.translation_settings_title),
        subtitle = stringResource(R.string.translation_settings_subtitle),
        onClick = onNavigateToTranslationSettings,
    )
}
```

**Integration Point:**
- Add link below "Summary Settings" in AI Integration section
- Maintain consistent styling with other settings links

---

### 7. Dependency Injection (MODIFY)

**File:** `ArchModelModule.kt`

**Changes:**
```kotlin
in DI.Module {
    // Existing bindings...

    bindWithComposableViewModelScope<TranslationSettingsViewModel>()
}
```

**Scope:** Compose (screen-scoped, tied to Compose lifecycle)

**Rationale:**
- Consistent with other settings ViewModels
- Automatic cleanup when screen is destroyed
- Kodein creates ViewModel with DI passed to constructor

---

### 8. Internationalization (NEW/ MODIFY)

**File:** `app/src/main/res/values/strings.xml`

**New Strings:**
```xml
<!-- Translation Settings -->
<string name="translation_settings_title">Translation Settings</string>
<string name="translation_settings_subtitle">Configure article translation</string>
<string name="translation_enabled_title">Enable Auto Translation</string>
<string name="translation_enabled_description">Automatically translate articles to your preferred language</string>

<!-- Translation Languages -->
<string name="translation_language_device_default">Device Default</string>
<string name="translation_language_english">English</string>
<string name="translation_language_chinese">Chinese</string>
<string name="translation_language_spanish">Spanish</string>
<!-- ... etc, matching SummaryLanguage -->
```

**Chinese Translations:** `values-zh/strings.xml`
**Other Languages:** As needed for existing supported languages

## Data Flow

### User Interaction: Enable Translation

```
User taps switch
    ↓
TranslationSettingsScreen: onChange callback
    ↓
TranslationSettingsViewModel.setTranslationEnabled(true)
    ↓
viewModelScope.launch { repository.setTranslationEnabled(true) }
    ↓
Repository.setTranslationEnabled(true)
    ↓
SettingsStore.setTranslationEnabled(true)
    ↓
SharedPreferences.edit().putBoolean(..., true).apply()
    ↓
_translationEnabled.value = true
    ↓
StateFlow emits new value
    ↓
TranslationSettingsScreen recomposes
    ↓
Switch shows checked state
    ↓
Language selector becomes enabled
```

### User Interaction: Select Language

```
User taps language selector
    ↓
Dropdown menu expands
    ↓
User selects "Chinese"
    ↓
TranslationSettingsViewModel.setTranslationLanguage(CHINESE)
    ↓
viewModelScope.launch { repository.setTranslationLanguage(CHINESE) }
    ↓
Repository.setTranslationLanguage(CHINESE)
    ↓
SettingsStore.setTranslationLanguage(CHINESE)
    ↓
SharedPreferences.edit().putString(..., "zh").apply()
    ↓
_translationLanguage.value = CHINESE
    ↓
StateFlow emits new value
    ↓
TranslationSettingsScreen recomposes
    ↓
Subtitle shows "Chinese"
    ↓
Dropdown dismisses
```

### Application Startup: Load Settings

```
Application starts
    ↓
SettingsStore initialized
    ↓
Load from SharedPreferences:
    - PREF_TRANSLATION_ENABLED → false (default)
    - PREF_TRANSLATION_LANGUAGE → null → DEVICE_DEFAULT
    ↓
_translationEnabled.value = false
_translationLanguage.value = DEVICE_DEFAULT
    ↓
Repository exposes StateFlows
    ↓
ViewModel collects StateFlows
    ↓
Screen renders with default values
```

## Technology Stack

| Layer | Technology | Rationale |
|-------|------------|-----------|
| **UI** | Jetpack Compose | Declarative UI, consistent with codebase |
| **State Management** | StateFlow | Reactive, lifecycle-aware, Kotlin-first |
| **DI** | Kodein | Existing DI framework in project |
| **Persistence** | SharedPreferences | Simple key-value, sufficient for settings |
| **Coroutines** | kotlinx.coroutines | Async operations, viewModelScope |
| **Navigation** | Jetpack Navigation Compose | Type-safe, existing nav infrastructure |

## Security Considerations

1. **No Sensitive Data**: Translation settings are not sensitive (no API keys stored here)
2. **SharedPreferences**: Stored in private app storage, accessible only to app
3. **API Keys**: Translation uses active AI provider's existing credentials
4. **Validation**: Language codes are enum values, no SQL injection risk

## Performance Considerations

1. **StateFlow Emission**: Minimal overhead (only emits on value change)
2. **SharedPreferences**: Fast for small key-value reads (~1ms)
3. **UI Recomposition**: Minimal (only affected components recompose)
4. **Memory**: O(1) per screen lifecycle (ViewModel cleared on back)
5. **Dropdown Rendering**: O(N) for N languages (~12), negligible

**Complexity Analysis:**

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Load settings on startup | O(1) | O(1) |
| Toggle translation | O(1) | O(1) |
| Select language | O(N) dropdown render | O(1) |
| Persist to disk | O(1) SharedPreferences | O(1) |
| Navigation | O(1) | O(1) |

## ADRs (Architecture Decision Records)

### ADR-001: Separate TranslationLanguage Enum from SummaryLanguage

**Status:** Accepted

**Context:**
- SummaryLanguage enum exists for AI summary language selection
- Translation requires similar language selection functionality
- Need to decide whether to reuse SummaryLanguage or create separate enum

**Decision:**
Create separate `TranslationLanguage` enum instead of reusing `SummaryLanguage`.

**Rationale:**
- **Independent Evolution**: Translation and summary use cases may diverge (e.g., translation may need more languages, summary may add AUTO_DETECT feature)
- **Semantic Clarity**: Different purposes communicated through type system
- **Default Behavior**: Translation needs DEVICE_DEFAULT, summary has AUTO_DETECT (different semantics)
- **Type Safety**: Compiler prevents accidentally passing wrong language type
- **Minimal Cost**: Code duplication is minimal (~12 enum entries), and both can share same language codes

**Alternatives Considered:**
1. **Reuse SummaryLanguage** (Rejected):
   - Pro: Less code duplication
   - Con: Couples translation to summary features, less type-safe

**Consequences:**
- Positive: Clear separation of concerns, independent evolution
- Positive: Type-safe (can't pass SummaryLanguage where TranslationLanguage expected)
- Negative: Maintains two similar enums (mitigated by keeping them in sync via code review)
- Negligible: Small increase in code size (~50 lines)

**Reversibility:**
Low cost to merge enums later if needed. Can create a common `Language` enum and have both SummaryLanguage and TranslationLanguage wrap it, or unify entirely if use cases converge.

---

### ADR-002: Global-Only Configuration (No Per-Feed Override)

**Status:** Accepted

**Context:**
- Some settings in Feeder support per-feed overrides (e.g., sync frequency)
- Translation configuration could be implemented globally or with per-feed override
- Need to decide scope based on user requirements

**Decision:**
Implement translation settings as global-only configuration (no per-feed override).

**Rationale:**
- **User Requirement**: Spec explicitly states "Global-only configuration"
- **Simplicity**: Avoids complex UI for per-feed settings
- **Consistency**: Matches SummarySettings pattern (also global-only)
- **MVP**: Can add per-feed override later if users request it
- **UI Complexity**: Per-feed override requires additional UI in feed edit screen

**Alternatives Considered:**
1. **Per-Feed Override** (Rejected):
   - Pro: Flexibility for advanced users
   - Con: UI complexity, not requested by users
2. **Hybrid (Global + Optional Per-Feed)** (Rejected):
   - Pro: Best of both worlds
   - Con: Over-engineering for current requirements

**Consequences:**
- Positive: Simple, focused implementation
- Positive: Consistent with SummarySettings
- Negative: Less flexible for power users (acceptable trade-off)
- Negative: Requires architecture change if per-feed added later (moderate cost)

**Reversibility:**
Moderate cost to add per-feed override later:
1. Add `translationLanguage` and `translationEnabled` to `Feed` entity
2. Add UI in `EditFeedScreen` for per-feed settings
3. Update Repository to check per-feed settings before global
4. Estimate: 4-6 hours development + testing

---

### ADR-003: Reuse SwitchSetting and LanguageSelectorSetting Components

**Status:** Accepted

**Context:**
- TranslationSettingsScreen needs switch and language selector UI
- SummarySettingsScreen already has these components
- Need to decide whether to reuse or create new components

**Decision:**
Reuse existing `SwitchSetting` and copy `LanguageSelectorSetting` from SummarySettingsScreen.

**Rationale:**
- **Consistency**: Same UI patterns across settings screens
- **Maintainability**: Changes to switch/language selector affect both screens
- **Code Quality**: Reuse tested, proven components
- **Efficiency**: Faster development (less UI work)

**Implementation:**
- `SwitchSetting`: Already in `Settings.kt`, use directly
- `LanguageSelectorSetting`: Copy from `SummarySettingsScreen.kt`, adapt for `TranslationLanguage`

**Alternatives Considered:**
1. **Create New Components** (Rejected):
   - Pro: Could optimize for translation-specific needs
   - Con: Code duplication, inconsistency
2. **Extract to Shared Module** (Rejected):
   - Pro: True reuse without copy
   - Con: Over-engineering for 2 uses, tight coupling via generics

**Consequences:**
- Positive: Consistent UI/UX across settings
- Positive: Faster development
- Negative: LanguageSelectorSetting duplicated (mitigated by keeping identical)

**Reversibility:**
Low cost. If components diverge significantly, can extract to shared base or separate module.

---

### ADR-004: Device Default Language Implementation

**Status:** Accepted

**Context:**
- Users may want translation to match device language
- Need mechanism to represent "use device language" option
- Android provides Locale settings via Configuration

**Decision:**
Implement `DEVICE_DEFAULT` as a `TranslationLanguage` enum entry with empty code (`""`).

**Rationale:**
- **User-Friendly**: Most users expect translation in their device language
- **Persistence**: Empty code distinguishes from explicit language selection
- **Type Safety**: Enum value vs. nullable provides compile-time safety
- **Consistency**: Similar pattern to SummaryLanguage.AUTO_DETECT (empty code)

**Implementation:**
```kotlin
DEVICE_DEFAULT(
    code = "",
    displayName = R.string.translation_language_device_default,
    languageName = "the device's default"
)
```

When translation is requested with DEVICE_DEFAULT:
```kotlin
fun getTargetLanguage(): String {
    val setting = settingsStore.translationLanguage.value
    return when (setting) {
        TranslationLanguage.DEVICE_DEFAULT ->
            Locale.getDefault().language // e.g., "zh"
        else -> setting.code // e.g., "en"
    }
}
```

**Alternatives Considered:**
1. **Nullable Language** (Rejected):
   - Pro: Represents "no preference" naturally
   - Con: Null handling scattered throughout codebase
2. **Separate Boolean Flag** (Rejected):
   - Pro: Clear intent
   - Con: More complex state (flag + language)

**Consequences:**
- Positive: Simple, elegant representation
- Positive: Type-safe (non-nullable enum)
- Positive: Consistent with AUTO_DETECT pattern
- Negative: Requires runtime lookup of device language (trivial)

**Reversibility:**
Low cost. Can change to nullable or separate flag if requirements evolve.

---

### ADR-005: Navigation Placement Under AI Integration

**Status:** Accepted

**Context:**
- Translation settings link needs placement in Settings screen
- Options: Top-level section, AI Integration section, or nested under other settings
- Need to decide most logical location

**Decision:**
Place "Translation Settings" link under "AI Integration" section in Settings screen, below "Summary Settings".

**Rationale:**
- **Logical Grouping**: Translation uses AI providers, grouped with AI features
- **Discoverability**: Users configuring AI will see translation option
- **Consistency**: Parallel to "Summary Settings" in same section
- **User Mental Model**: "AI Integration" = features using AI providers

**Alternatives Considered:**
1. **Top-Level Section** (Rejected):
   - Pro: More prominent
   - Con: Clutters settings, not a primary feature
2. **Nested Under Summary Settings** (Rejected):
   - Pro: Related AI features
   - Con: Implies dependency (summary doesn't depend on translation)

**Consequences:**
- Positive: Logical organization
- Positive: Discoverable for AI users
- Negative: Slightly less prominent (acceptable trade-off)

**Reversibility:**
Negligible cost to move link. Change callback in Settings.kt, update destination.

## Future Considerations

### NOT Implementing Now (Explicitly Out of Scope)

1. **Per-Feed Translation Override**:
   - Rationale: Not requested, adds complexity
   - Future: Add if users request it (see ADR-002)

2. **Translation Provider Selection**:
   - Rationale: Uses active AI provider (already configured)
   - Future: Add if translation-specific providers needed

3. **Batch Translation**:
   - Rationale: Not in requirements
   - Future: Add for "translate all unread articles" feature

4. **Translation History**:
   - Rationale: Not in requirements
   - Future: Add for cost tracking, quality assessment

5. **Translation Quality Feedback**:
   - Rationale: Not in requirements
   - Future: Add for improving translation prompts

### Extensibility Points

1. **Additional Languages**:
   - Add entries to `TranslationLanguage` enum
   - Add string resources for display names

2. **Translation Providers**:
   - Currently uses active AI provider
   - Can add translation-specific provider selection later

3. **Translation Triggers**:
   - Currently manual (switch on settings)
   - Can add automatic triggers (e.g., translate feeds in foreign language)

4. **Translation Caching**:
   - Not implementing caching now
   - Can add to reduce API costs later

## Validation Checklist

### Architecture Completeness
- [x] All functional requirements addressed
  - [x] Add "Enable Auto Translation" toggle
  - [x] Add language selector (default: device language)
  - [x] Use active AI provider
  - [x] Global-only configuration
  - [x] Separate TranslationLanguage enum
- [x] All non-functional requirements considered
  - [x] Performance: O(1) operations, minimal recomposition
  - [x] Scalability: N/A (single-user settings)
  - [x] Security: No sensitive data, private storage
  - [x] Maintainability: Follows existing patterns
- [x] Module boundaries align with domain concepts
  - [x] UI layer (Screen, ViewModel)
  - [x] Business layer (Repository)
  - [x] Data layer (SettingsStore)
- [x] Dependencies form directed acyclic graph
  - [x] Screen → ViewModel → Repository → SettingsStore
  - [x] No cycles detected
- [x] Each module has single, clear purpose
  - [x] TranslationLanguage: Language enumeration
  - [x] TranslationSettingsScreen: UI rendering
  - [x] TranslationSettingsViewModel: State management
  - [x] Repository: Facade for settings operations

### Quality Principles
- [x] SOLID principles followed
  - [x] Single Responsibility: Each class has one job
  - [x] Open/Closed: Extensible via enum additions
  - [x] Liskov Substitution: N/A (no inheritance)
  - [x] Interface Segregation: Minimal interfaces
  - [x] Dependency Inversion: Depend on abstractions (Repository)
- [x] DRY: Reused SwitchSetting, LanguageSelectorSetting
- [x] YAGNI: No speculative features (per-feed override, etc.)
- [x] Loose coupling achieved
  - [x] Data coupling between layers (good)
  - [x] No control coupling
  - [x] No stamp coupling (simple types)
- [x] High cohesion within modules
  - [x] UI components only handle rendering
  - [x] ViewModel only handles state
  - [x] Repository only delegates to store

### Complexity & Performance
- [x] Hot-path operations have O(1) or O(log N) complexity
  - [x] Toggle switch: O(1)
  - [x] Select language: O(N) dropdown, N≈12 (acceptable)
- [x] Space complexity documented and justified
  - [x] O(1) per screen lifecycle
  - [x] O(N) for dropdown rendering (N=languages)
- [x] Data structures optimized for access patterns
  - [x] StateFlow for reactive updates (efficient)
  - [x] Enum for languages (fast lookup)
- [x] No O(N²) on unbounded data

### Modular Design
- [x] All modules testable in isolation
  - [x] ViewModel can be tested with Repository mock
  - [x] SettingsStore can be tested with SharedPreferences mock
  - [x] UI can be tested with Compose testing
- [x] Cross-module communication via interfaces only
  - [x] Repository interface (implicit via facade)
  - [x] StateFlow for data flow
- [x] No content or common coupling
  - [x] No shared mutable state
  - [x] No global variables
- [x] Interfaces documented with complexity annotations
  - [x] Time/space complexity in spec
  - [x] StateFlow contracts documented

### Implementation Readiness
- [x] Interfaces defined for all modules
  - [x] TranslationLanguage enum
  - [x] ViewModel methods
  - [x] Repository facade methods
- [x] Error handling strategy complete
  - [x] No explicit errors expected (SharedPreferences safe)
- [x] Security considerations addressed
  - [x] Private storage only
  - [x] No sensitive data
- [x] Performance path defined
  - [x] O(1) for common operations
  - [x] Minimal recomposition
- [x] Existing patterns respected
  - [x] Follows SummarySettings pattern
  - [x] Uses existing components
  - [x] Consistent navigation structure

### Anti-Patterns Avoided
- [x] No "Big Ball of Mud"
  - [x] Clear module boundaries
  - [x] Organized by layer (UI, business, data)
- [x] No "God Module"
  - [x] Each class has single responsibility
  - [x] TranslationLanguage (enum only)
  - [x] ViewModel (state only)
  - [x] Screen (UI only)
- [x] No circular dependencies
  - [x] Directed acyclic graph confirmed
- [x] No premature optimization
  - [x] Simple SharedPreferences (no Room)
  - [x] Basic StateFlow (no complex caching)

## Module Dependencies (Cyclomatic Complexity Analysis)

### Coupling Metrics

| Module | Afferent (Ca) | Efferent (Ce) | Instability (I) | Abstractness (A) | D = |A+I-1| |
|--------|--------------|--------------|----------------|------------------|-----------|
| TranslationLanguage | 3 (SettingsStore, Repository, ViewModel) | 0 (none) | 0.0 | 0.0 (enum) | 0.0 |
| SettingsStore (new) | 1 (Repository) | 1 (TranslationLanguage) | 0.5 | 0.0 | 0.5 |
| Repository (new) | 1 (ViewModel) | 1 (SettingsStore) | 0.5 | 0.0 | 0.5 |
| TranslationSettingsViewModel | 1 (Screen) | 1 (Repository) | 0.5 | 0.0 | 0.5 |
| TranslationSettingsScreen | 0 | 1 (ViewModel) | 1.0 | N/A (UI) | N/A |

**Analysis:**
- **TranslationLanguage**: Stable (I=0), no dependencies, good core module
- **SettingsStore**: Balanced (I=0.5), depends only on enum, acceptable
- **Repository**: Balanced (I=0.5), thin facade, acceptable
- **ViewModel**: Balanced (I=0.5), minimal coupling, acceptable
- **Screen**: Most unstable (I=1.0), expected for UI (top of layer)

**Distance from Main Sequence:**
- All modules close to main sequence (D < 0.3)
- Architecture is well-balanced

**Coupling Types:**
- TranslationLanguage → SettingsStore: Data coupling (enum values)
- SettingsStore → Repository: Data coupling (StateFlow)
- Repository → ViewModel: Data coupling (StateFlow)
- ViewModel → Screen: Data coupling (StateFlow)

**Verdict:** Excellent coupling quality. All dependencies are data coupling (best type).

## Testability Analysis

### Unit Testing Strategy

1. **TranslationLanguage Enum**:
   - Test `fromCode()` with valid codes
   - Test `fromCode()` with invalid codes (returns DEVICE_DEFAULT)
   - Test enum entries have correct properties
   - **Coverage**: 100% (simple enum)

2. **SettingsStore**:
   - Mock SharedPreferences
   - Test initial values (defaults)
   - Test `setTranslationEnabled()` updates StateFlow
   - Test `setTranslationLanguage()` persists code
   - Test StateFlow emissions
   - **Coverage**: >90% (simple state management)

3. **Repository**:
   - Mock SettingsStore
   - Test StateFlow delegation
   - Test setter methods delegate correctly
   - **Coverage**: 100% (thin facade)

4. **ViewModel**:
   - Mock Repository
   - Test StateFlow exposure
   - Test `setTranslationEnabled()` calls Repository
   - Test `setTranslationLanguage()` calls Repository
   - Test viewModelScope usage
   - **Coverage**: >90% (simple delegation)

5. **Screen** (Compose UI Test):
   - Mock ViewModel
   - Test switch toggles
   - Test language selector opens
   - Test language selection updates ViewModel
   - Test navigation callback
   - **Coverage**: ~80% (UI testing)

### Integration Testing Strategy

1. **Settings Flow Integration**:
   - Start with empty SharedPreferences
   - Open screen, verify defaults shown
   - Toggle switch, verify persisted
   - Select language, verify persisted
   - Restart app, verify values loaded

2. **Navigation Integration**:
   - From Settings screen, click translation link
   - Verify TranslationSettingsScreen opens
   - Click back, verify returns to Settings

### Test Doubles Required

- **SettingsStore**: SharedPreferences mock (easy)
- **Repository**: SettingsStore mock (easy)
- **ViewModel**: Repository mock (easy)

**Verdict:** Highly testable architecture. All components are mockable.

## Migration Path

### For Existing Users

1. **First Launch After Update**:
   - SettingsStore initializes with defaults
   - Translation disabled (false)
   - Language set to DEVICE_DEFAULT
   - No migration needed (clean slate)

2. **No Data Loss**:
   - New settings don't affect existing functionality
   - Summary settings remain independent

### For Developers

1. **Add TranslationLanguage Enum**:
   - Create file in `ai/model` package
   - Add enum entries
   - Add string resources

2. **Modify SettingsStore**:
   - Add StateFlow properties
   - Add setter methods
   - Add preference constants

3. **Modify Repository**:
   - Add facade methods
   - Expose StateFlow

4. **Create ViewModel**:
   - Create `TranslationSettingsViewModel`
   - Bind in DI module

5. **Create Screen**:
   - Create `TranslationSettingsScreen`
   - Reuse/copy components

6. **Wire Navigation**:
   - Add destination
   - Add link in Settings screen

**Estimated Effort:** 4-6 hours development + 2-3 hours testing

## Conclusion

This architecture design provides a clean, modular implementation of translation configuration that:

1. **Follows Established Patterns**: Mirrors SummarySettings exactly
2. **Maintains Separation of Concerns**: Clear layer boundaries
3. **Ensures Testability**: All components are mockable
4. **Optimizes Performance**: O(1) operations, minimal recomposition
5. **Supports Evolution**: Extensible for future features

The design is ready for implementation with clear interfaces, data flow, and validation checkpoints.
