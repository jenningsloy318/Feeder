# Technical Specification: Translation Configuration

**Date:** 2026-01-03
**Author:** Claude (Specification Writer Agent)
**Status:** Draft
**Feature:** Add translation config settings under Settings → AI Integration

## 1. Overview

### 1.1 Summary
Add a global translation configuration feature to the Feeder RSS reader, allowing users to enable automatic article translation and configure the target language. The settings will be accessible via Settings → AI Integration → Translation Settings.

### 1.2 Goals
- Provide a simple toggle to enable/disable automatic translation
- Allow users to select their preferred translation language (default: device language)
- Reuse the active AI provider configuration (no duplicate setup)
- Follow existing patterns from SummarySettings for consistency
- Ensure global-only configuration (no per-feed override in this iteration)

### 1.3 Non-Goals
- Per-feed translation override (deferred to future iteration)
- Translation provider selection (uses active provider)
- Translation history or quality feedback
- Batch translation or offline translation
- Real-time translation preview

## 2. Background

### 2.1 Context

From the Requirements document:
> Users want to automatically translate foreign language articles to their preferred language using AI. The feature should be simple to configure and follow existing AI Integration patterns.

From the Research report:
> The existing SummarySettingsScreen provides an excellent template for translation configuration. Both features involve enabling an AI feature and selecting a target language. Reusing this pattern ensures consistency and reduces development effort.

From the Architecture document:
> The translation configuration follows a three-layer architecture: UI (TranslationSettingsScreen + ViewModel), Business Logic (Repository facade), and Data Layer (SettingsStore + SharedPreferences). This mirrors the SummarySettings pattern exactly.

### 2.2 Current State

From the Code Assessment:
> - SettingsStore.kt manages summary settings via SharedPreferences
> - Repository.kt provides a facade for settings operations
> - SummarySettingsScreen.kt implements the UI with SwitchSetting and LanguageSelectorSetting
> - Navigation is handled via NavigationDestinations.kt
> - DI is managed through Kodein in ArchModelModule.kt

> The codebase follows clean architecture with clear separation between UI, business logic, and data layers. All settings use StateFlow for reactive updates and SharedPreferences for persistence.

### 2.3 Problem Statement

Users who consume content in multiple languages need a convenient way to translate articles to their preferred language. Currently, there is no translation feature in Feeder. This specification defines the configuration interface for automatic translation.

## 3. Technical Design

### 3.1 Architecture

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

### 3.2 Components

#### Component 1: TranslationLanguage (Enum)
- **Purpose:** Define supported translation languages with ISO 639-1 codes
- **Responsibilities:**
  - Provide language codes for translation API
  - Provide display names for UI
  - Support device default option
  - Parse language codes from persisted settings
- **Interface:**
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
      // ... other languages

      companion object {
          fun fromCode(code: String?): TranslationLanguage
      }
  }
  ```

#### Component 2: SettingsStore (Data Layer Modifications)
- **Purpose:** Persist and expose translation settings via SharedPreferences
- **Responsibilities:**
  - Persist translation enabled flag
  - Persist selected translation language
  - Expose StateFlow for reactive updates
  - Initialize with sensible defaults
- **Interface Additions:**
  ```kotlin
  class SettingsStore {
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

#### Component 3: Repository (Business Layer Modifications)
- **Purpose:** Facade for translation settings, delegating to SettingsStore
- **Responsibilities:**
  - Expose translation settings to ViewModels
  - Provide clean interface for settings operations
  - Handle business logic validation (if any)
- **Interface Additions:**
  ```kotlin
  class Repository {
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

#### Component 4: TranslationSettingsViewModel (NEW - Presentation Layer)
- **Purpose:** Manage state for Translation Settings screen
- **Responsibilities:**
  - Expose translation settings as StateFlow
  - Handle user interactions (toggle switch, select language)
  - Delegate persistence to Repository
- **Interface:**
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

#### Component 5: TranslationSettingsScreen (NEW - UI Layer)
- **Purpose:** Compose UI for translation settings
- **Responsibilities:**
  - Render switch to enable/disable translation
  - Render language selector dropdown
  - Navigate back when done
  - Observe ViewModel state reactively
- **Interface:**
  ```kotlin
  @Composable
  fun TranslationSettingsScreen(
      onNavigateUp: () -> Unit,
      viewModel: TranslationSettingsViewModel,
      modifier: Modifier = Modifier,
  )
  ```

#### Component 6: Navigation Integration (MODIFY)
- **Purpose:** Wire translation settings into navigation graph
- **Responsibilities:**
  - Define TranslationSettingsDestination
  - Add link in Settings screen
  - Handle back navigation
- **Interface:**
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

### 3.3 Data Model

```kotlin
enum class TranslationLanguage(
    val code: String,
    @StringRes val displayName: Int,
    val languageName: String,
) {
    DEVICE_DEFAULT("", R.string.translation_language_device_default, "the device's default"),
    ENGLISH("en", R.string.translation_language_english, "English"),
    CHINESE("zh", R.string.translation_language_chinese, "Chinese"),
    SPANISH("es", R.string.translation_language_spanish, "Spanish"),
    FRENCH("fr", R.string.translation_language_french, "French"),
    GERMAN("de", R.string.translation_language_german, "German"),
    JAPANESE("ja", R.string.translation_language_japanese, "Japanese"),
    KOREAN("ko", R.string.translation_language_korean, "Korean"),
    PORTUGUESE("pt", R.string.translation_language_portuguese, "Portuguese"),
    RUSSIAN("ru", R.string.translation_language_russian, "Russian"),
    ARABIC("ar", R.string.translation_language_arabic, "Arabic"),
    HINDI("hi", R.string.translation_language_hindi, "Hindi");

    companion object {
        fun fromCode(code: String?): TranslationLanguage =
            entries.find { it.code == code } ?: DEVICE_DEFAULT
    }
}
```

**Database Changes:** None (uses SharedPreferences)

### 3.4 API Design

No external API - this is a settings feature. The translation feature itself (not in scope) would call AI provider APIs.

### 3.5 Error Handling

| Error Case | Handler | User Feedback |
|------------|---------|---------------|
| Invalid language code in SharedPreferences | SettingsStore defaults to DEVICE_DEFAULT | Silent (corrects on load) |
| SharedPreferences access failure | Platform throws, caught by platform | System error dialog |
| Navigation failure | popBackStack() returns false | Navigate to Settings screen |

## 4. Implementation Approach

### 4.1 Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **State Management:** StateFlow + collectAsStateWithLifecycle
- **DI Framework:** Kodein
- **Persistence:** SharedPreferences
- **Coroutines:** kotlinx.coroutines (viewModelScope)
- **Navigation:** Jetpack Navigation Compose

### 4.2 Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| androidx.compose.ui | existing | Compose UI framework |
| androidx.lifecycle | existing | StateFlow and lifecycle awareness |
| org.kodein.di | existing | Dependency injection |
| kotlinx.coroutines | existing | Coroutine management |
| Material3 | existing | UI components and theming |

### 4.3 Configuration

No new configuration files. Add to existing:

```kotlin
// ArchModelModule.kt - add to existing module
bindWithComposableViewModelScope<TranslationSettingsViewModel>()

// NavigationDestinations.kt - add to existing file
data object TranslationSettingsDestination : NavigationDestination(...) { ... }

// Settings.kt - add to existing AI Integration section
ExternalSetting(
    title = stringResource(R.string.translation_settings_title),
    onClick = onNavigateToTranslationSettings,
)
```

## 5. Testing Strategy

### 5.1 Unit Tests
| Component | Test Cases |
|-----------|------------|
| TranslationLanguage enum | - fromCode() with valid codes returns correct enum<br>- fromCode() with invalid/null returns DEVICE_DEFAULT<br>- All enum entries have non-empty codes (except DEVICE_DEFAULT) |
| SettingsStore | - Initial values are defaults (false, DEVICE_DEFAULT)<br>- setTranslationEnabled() updates StateFlow<br>- setTranslationLanguage() persists code<br>- StateFlow emissions are observed |
| Repository | - StateFlow delegation works<br>- Setter methods call SettingsStore<br>- No business logic bypass |
| ViewModel | - StateFlow exposure is reactive<br>- setTranslationEnabled() calls Repository<br>- setTranslationLanguage() calls Repository<br>- viewModelScope used for coroutines |

### 5.2 Integration Tests
- **Settings Flow:** Open screen → Toggle switch → Select language → Verify persisted → Restart app → Verify loaded
- **Navigation Flow:** From Settings → Click link → Verify TranslationSettingsScreen opens → Back → Verify returns to Settings
- **Provider Integration:** Click provider link → Verify navigates to ProviderListScreen

### 5.3 Edge Cases
| Edge Case | Expected Behavior | Test |
|-----------|-------------------|------|
| SharedPreferences corrupted (invalid code) | Defaults to DEVICE_DEFAULT | Unit test with mock SP |
| Rapid toggle switching | Last state wins, no crashes | UI test with rapid clicks |
| Language selector when translation disabled | Dropdown is disabled/grayed out | UI test verifies disabled state |
| Back press when dropdown open | Dropdown closes, no navigation | UI test with back press |
| Device language changes | DEVICE_DEFAULT picks up new language | Manual test (change locale) |
| No providers configured | Provider link shows warning subtitle | UI test with empty providers |

## 6. Security Considerations

### 6.1 Input Validation
| Input | Validation | Sanitization |
|-------|------------|--------------|
| Translation enabled toggle | Boolean (no validation needed) | None |
| Language selection | Enum values only | fromCode() defaults to DEVICE_DEFAULT |

### 6.2 Authentication & Authorization
- **Auth required:** No (settings are local)
- **Permission checks:** None
- **Role restrictions:** None

### 6.3 Data Protection
- **Sensitive data:** None (translation preferences are not sensitive)
- **Encryption:** Not required (stored in private app storage)
- **Logging:** No logging of user preferences

### 6.4 OWASP Considerations
| Risk | Applicable | Mitigation |
|------|------------|------------|
| Injection | No | No SQL or code injection (enum only) |
| Broken Auth | No | No auth required |
| XSS | No | Not a web application |
| CSRF | No | Not a web application |
| Security Misconfiguration | No | Uses platform defaults |

## 7. Performance Considerations

### 7.1 Complexity Analysis
| Operation | Time Complexity | Space Complexity |
|-----------|-----------------|------------------|
| Load settings on startup | O(1) | O(1) |
| Toggle translation | O(1) | O(1) |
| Select language | O(N) dropdown render | O(1) |
| Persist to SharedPreferences | O(1) | O(1) |
| Navigation | O(1) | O(1) |
| fromCode() lookup | O(N) where N=12 (enum entries) | O(1) |

### 7.2 Database Optimization
- **Indexes needed:** None (not using Room database)
- **Query optimization:** N/A
- **Connection pooling:** N/A

### 7.3 Caching Strategy
| Data | Cache Type | TTL | Invalidation |
|------|------------|-----|--------------|
| Translation settings | StateFlow in memory | Process lifetime | N/A (immutable settings) |
| TranslationLanguage enum | Compile-time constant | N/A | N/A |

### 7.4 Scalability
- **Bottlenecks:** None (single-user settings)
- **Horizontal scaling:** N/A (client-side only)
- **Rate limiting:** Not applicable

### 7.5 Resource Usage
- **Memory:** O(1) per screen lifecycle (ViewModel cleared on back)
- **CPU:** Negligible (simple state updates)
- **Network:** None (settings are local)

## 8. Rollout Plan
1. Add TranslationLanguage enum and string resources
2. Modify SettingsStore to persist translation settings
3. Modify Repository to expose translation settings
4. Create TranslationSettingsViewModel
5. Create TranslationSettingsScreen composable
6. Add TranslationSettingsDestination to navigation
7. Add "Translation Settings" link to Settings screen
8. Bind ViewModel in DI module
9. Test all user flows
10. Update translations for all supported languages

## 9. Open Questions
- [ ] Should we add a "Translate Now" button for testing the configuration? (Deferred to future iteration)
- [ ] Should we show a preview of translated text in settings? (Deferred to future iteration)
- [ ] Should we add translation statistics (cost, number of translations)? (Deferred to future iteration)

## 10. References

**Required Documents (all referenced and linked):**

- **Requirements** (super-dev:requirements-clarifier): [./00-requirements.md](./00-requirements.md) (Note: File should be in this directory)
- **Research Report** (super-dev:research-agent): [./01-research-report.md](./01-research-report.md) (Note: File should be in this directory)
- **Assessment** (super-dev:code-assessor): [./02-assessment.md](./02-assessment.md) (Note: File should be in this directory)
- **Architecture** (super-dev:architecture-agent): [../05-architecture.md](../05-architecture.md)
- **Design Spec** (super-dev:ui-ux-designer): [../design-spec-translation-config.md](../design-spec-translation-config.md)
- **ADR-001 (Separate Enum)**: [../05-adr-separate-translation-enum.md](../05-adr-separate-translation-enum.md)

**External References:**
- [Material 3 Switch Component](https://m3.material.io/components/switch/overview)
- [Material 3 Dropdown Menu](https://m3.material.io/components/menus/guidelines)
- [Jetpack Compose StateFlow Documentation](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Android SharedPreferences Best Practices](https://developer.android.com/training/data-storage/shared-preferences)

**Related Code Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt` (Template for TranslationSettingsScreen)
- `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` (Modify to add translation settings)
- `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` (Modify to add translation facade)
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt` (Add destination)
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt` (Add link)
- `app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt` (Bind ViewModel)
