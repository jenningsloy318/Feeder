# Architecture Design: AI Summary Configuration

**Created:** 2026-01-01 19:12:50 +08:00
**Feature:** Improve AI Integration Summary Configuration
**Status:** Architecture Design Complete
**Phase:** 5.3

---

## Overview

This document describes the architecture for enhancing the AI Summary Configuration feature. The design follows existing patterns in the Feeder app and maintains consistency with the MVVM architecture, Jetpack Compose UI, and Material Design 3.

---

## System Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  ┌─────────────────────────────────┐   │
│  │   SummarySettingsScreen         │   │
│  │   - Switch for enable/disable   │   │
│  │   - Dropdown for language       │   │
│  └─────────────────────────────────┘   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      ViewModel Layer                    │
│  ┌─────────────────────────────────┐   │
│  │   SummarySettingsViewModel      │   │
│  │   - Manages UI state            │   │
│  │   - Delegates to SettingsStore  │   │
│  └─────────────────────────────────┘   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Data Layer                         │
│  ┌─────────────────────────────────┐   │
│  │   SettingsStore                │   │
│  │   - SharedPreferences wrapper   │   │
│  │   - Exposes StateFlows          │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## Component Design

### 1. Data Layer

#### SettingsStore Extension

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Additions:**
```kotlin
// New preference key
const val PREF_SUMMARY_ENABLED = "pref_summary_enabled"

// New StateFlow
private val _summaryEnabled = MutableStateFlow(
    sp.getBoolean(PREF_SUMMARY_ENABLED, true)
)
val summaryEnabled: StateFlow<Boolean> = _summaryEnabled.asStateFlow()

// New setter
fun setSummaryEnabled(value: Boolean) {
    _summaryEnabled.value = value
    sp.edit().putBoolean(PREF_SUMMARY_ENABLED, value).apply()
}
```

**Design Decisions:**
- ✅ Follows existing pattern exactly
- ✅ Default: `true` (maintains current behavior)
- ✅ Uses StateFlow for reactive updates
- ✅ Thread-safe (SharedPreferences.apply())

**Alternatives Considered:**
1. **DataStore instead of SharedPreferences** ❌
   - Pros: Type-safe, coroutine-based
   - Cons: Inconsistent with rest of codebase, migration required
   - Decision: Stick with SharedPreferences for consistency

2. **Room Database** ❌
   - Pros: Queryable, relational
   - Cons: Overkill for single boolean, adds complexity
   - Decision: SharedPreferences is sufficient

---

### 2. ViewModel Layer

#### SummarySettingsViewModel

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt`

**Implementation:**
```kotlin
class SummarySettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    // Expose StateFlows from SettingsStore
    val summaryEnabled: StateFlow<Boolean> = settingsStore.summaryEnabled
    val summaryLanguage: StateFlow<SummaryLanguage> = settingsStore.summaryLanguage

    // Delegate actions to SettingsStore
    fun setSummaryEnabled(enabled: Boolean) {
        settingsStore.setSummaryEnabled(enabled)
    }

    fun setSummaryLanguage(language: SummaryLanguage) {
        settingsStore.setSummaryLanguage(language)
    }
}
```

**Design Decisions:**
- ✅ Thin ViewModel (delegates to SettingsStore)
- ✅ No business logic (just state management)
- ✅ Testable (can mock SettingsStore)
- ✅ Lifecycle-aware (ViewModel)

**Responsibilities:**
1. Expose state to UI
2. Handle user actions
3. Delegate persistence to SettingsStore
4. No business logic

---

### 3. UI Layer

#### SummarySettingsScreen

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**Structure:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Collect state with lifecycle awareness
    val summaryEnabled by viewModel.summaryEnabled.collectAsStateWithLifecycle()
    val summaryLanguage by viewModel.summaryLanguage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SensibleTopAppBar(
                title = stringResource(R.string.summary_settings_title),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Enable/Disable Switch
            item {
                SwitchSettingItem(
                    title = stringResource(R.string.summary_enabled_title),
                    subtitle = stringResource(R.string.summary_enabled_description),
                    checked = summaryEnabled,
                    onCheckedChange = { viewModel.setSummaryEnabled(it) },
                )
            }

            // Language Selector
            item {
                LanguageSelector(
                    title = stringResource(R.string.summary_language_title),
                    selectedLanguage = summaryLanguage,
                    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
                    enabled = summaryEnabled,
                )
            }
        }
    }
}
```

**Design Decisions:**
- ✅ Follows Provider List pattern
- ✅ Uses Scaffold + TopAppBar
- ✅ LazyColumn for scrollable content
- ✅ Proper padding and spacing
- ✅ Lifecycle-aware state collection

**Components:**
1. **SwitchSettingItem** - Reusable switch component
2. **LanguageSelector** - Dropdown for language selection

---

### 4. Navigation Layer

#### SummarySettingsDestination

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationDestinations.kt`

**Implementation:**
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

**Design Decisions:**
- ✅ Follows existing NavigationDestination pattern
- ✅ Route: `settings/summary` (hierarchical)
- ✅ Proper back navigation handling
- ✅ DI integration via `diAwareViewModel()`

---

### 5. Business Logic Integration

#### AIApi Modification

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Changes:**
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
        AIClient.SummaryResult.Error(content = e.message ?: "")
    }
}
```

**Design Decisions:**
- ✅ Early return when disabled (performance)
- ✅ Returns empty error (caller handles gracefully)
- ✅ Minimal change to existing flow
- ✅ No breaking changes

**Flow Diagram:**
```
User opens article
    ↓
ArticleScreen requests summary
    ↓
AIApi.summarize(content)
    ↓
Check repository.summaryEnabled
    ↓
    ├─ false → Return Error(empty)
    └─ true  → Proceed with generation
                ↓
            Get repository.summaryLanguage
                ↓
            client.generateSummary()
                ↓
            Return SummaryResult
```

---

## Data Flow

### State Update Flow

```
User toggles switch
    ↓
SwitchSettingItem.onCheckedChange
    ↓
SummarySettingsViewModel.setSummaryEnabled
    ↓
SettingsStore.setSummaryEnabled
    ↓
SharedPreferences.edit().putBoolean().apply()
    ↓
_summaryEnabled.value = newValue
    ↓
StateFlow propagates to UI
    ↓
SummarySettingsScreen recomposes
    ↓
UI updates with new state
```

### Navigation Flow

```
User taps "Summary" in settings
    ↓
AIProviderSection.onNavigateToSummary()
    ↓
SummarySettingsDestination.navigate(navController)
    ↓
navController.navigate("settings/summary")
    ↓
SummarySettingsDestination.RegisterScreen
    ↓
Create SummarySettingsViewModel
    ↓
SummarySettingsScreen compos
```

---

## Interface Definitions

### SettingsStore Interface

```kotlin
interface SettingsStore {
    val summaryEnabled: StateFlow<Boolean>
    val summaryLanguage: StateFlow<SummaryLanguage>

    fun setSummaryEnabled(value: Boolean)
    fun setSummaryLanguage(value: SummaryLanguage)
}
```

### SummarySettingsViewModel Interface

```kotlin
class SummarySettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val summaryEnabled: StateFlow<Boolean>
    val summaryLanguage: StateFlow<SummaryLanguage>

    fun setSummaryEnabled(enabled: Boolean)
    fun setSummaryLanguage(language: SummaryLanguage)
}
```

### SummarySettingsScreen Interface

```kotlin
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
    modifier: Modifier = Modifier,
)
```

---

## Component Dependencies

### Dependency Graph

```
SummarySettingsScreen
    ↓ (uses)
SummarySettingsViewModel
    ↓ (uses)
SettingsStore
    ↓ (uses)
SharedPreferences

SummarySettingsScreen
    ↓ (navigates via)
NavigationDestinations
    ↓ (routes to)
SummarySettingsDestination
```

### Module Boundaries

**UI Module:**
- SummarySettingsScreen
- SwitchSettingItem
- LanguageSelector

**ViewModel Module:**
- SummarySettingsViewModel

**Data Module:**
- SettingsStore

**Navigation Module:**
- NavigationDestinations

---

## State Management Strategy

### Approach: StateFlow + collectAsStateWithLifecycle

**Rationale:**
- ✅ Reactive (updates propagate automatically)
- ✅ Lifecycle-aware (no leaks)
- ✅ Kotlin-first (coroutines)
- ✅ Consistent with rest of codebase

**State Sources:**
1. **summaryEnabled:** Boolean (from SharedPreferences)
2. **summaryLanguage:** SummaryLanguage enum (from SharedPreferences)

**State Flow:**
```
SharedPreferences
    ↓
SettingsStore (MutableStateFlow)
    ↓
SummarySettingsViewModel (exposes StateFlow)
    ↓
SummarySettingsScreen (collectAsStateWithLifecycle)
    ↓
UI (recomposes on change)
```

---

## Error Handling Strategy

### UI Errors
- Toggle switch: Graceful degradation (show error message)
- Language selection: Show toast on failure

### Business Logic Errors
- AIApi.summarize: Returns `SummaryResult.Error`
- Empty error string = summaries disabled
- Non-empty error string = actual error

### Persistence Errors
- SharedPreferences failures: Silent (system-managed)
- Corruption: Use defaults (enabled=true, auto-detect)

---

## Testing Strategy

### Unit Tests

**SettingsStoreTest:**
```kotlin
@Test
fun `setSummaryEnabled updates preference`() {
    settingsStore.setSummaryEnabled(false)
    assertFalse(settingsStore.summaryEnabled.value)
}

@Test
fun `summaryEnabled defaults to true`() {
    assertTrue(settingsStore.summaryEnabled.value)
}
```

**SummarySettingsViewModelTest:**
```kotlin
@Test
fun `setSummaryEnabled delegates to SettingsStore`() {
    viewModel.setSummaryEnabled(false)
    verify(settingsStore).setSummaryEnabled(false)
}
```

**AIApiTest:**
```kotlin
@Test
fun `summarize returns empty error when disabled`() {
    whenever(repository.summaryEnabled).thenReturn(flowOf(false))
    val result = aiApi.summarize("content")
    assertTrue(result is AIClient.SummaryResult.Error)
    assertEquals("", result.content)
}
```

### UI Tests

**SummarySettingsScreenTest:**
```kotlin
@Test
fun `toggle switch updates state`() {
    composeTestRule.setContent {
        SummarySettingsScreen(...)
    }

    composeTestRule
        .onNodeWithText("Enable Summaries")
        .performClick()

    // Verify state changed
}
```

---

## Performance Considerations

### Memory
- **Additional StateFlows:** 1 (summaryEnabled)
- **Memory Impact:** < 1KB
- **Leak Risk:** None (proper lifecycle management)

### CPU
- **Additional Check:** One boolean read on summary generation
- **Impact:** Negligible (< 1ms)

### Storage
- **Additional Preference:** 1 boolean key
- **Storage Impact:** ~20 bytes

---

## Security Considerations

**Assessment:**
- ✅ No sensitive data
- ✅ No network exposure
- ✅ SharedPreferences is appropriate
- ✅ No security risks

---

## Scalability Considerations

**Future Enhancements:**
1. **Per-feed settings** - Could extend to feed-specific configuration
2. **Summary length** - Easy to add another preference
3. **Summary style** - Could add multiple styles
4. **AI provider settings** - Already implemented separately

**Architecture Support:**
- ✅ Easy to add new preferences
- ✅ Modular design allows extension
- ✅ ViewModel can handle more state
- ✅ UI can accommodate more settings

---

## Architecture Decision Records

### ADR-001: Use SharedPreferences for Summary Enabled Flag

**Context:** Need to persist summary enabled state

**Decision:** Use SharedPreferences (existing pattern)

**Rationale:**
- Consistent with existing codebase
- Simple, reliable, battle-tested
- Sufficient for boolean flag
- No migration needed

**Alternatives Considered:**
1. **DataStore** - More modern but inconsistent
2. **Room Database** - Overkill for single boolean

**Consequences:**
- ✅ Easy implementation
- ✅ Consistent with codebase
- ⚠️ Not coroutine-based (acceptable)

### ADR-002: Create Dedicated Summary Settings Screen

**Context:** Need UI for summary configuration

**Decision:** Create new screen following Provider List pattern

**Rationale:**
- Consistent with existing navigation
- Better UX than inline dropdown
- Room for future enhancements
- Follows established patterns

**Alternatives Considered:**
1. **Expandable inline section** - More complex, less flexible
2. **Bottom sheet** - Inconsistent with app patterns

**Consequences:**
- ✅ Better UX
- ✅ Scalable
- ✅ Consistent
- ⚠️ Additional navigation hop (acceptable)

### ADR-003: Check Enabled State in AIApe.summarize()

**Context:** Need to respect enabled state in business logic

**Decision:** Check repository.summaryEnabled in AIApi.summarize()

**Rationale:**
- Centralized check
- Early return (performance)
- UI doesn't need to know
- Easy to test

**Alternatives Considered:**
1. **Check in UI** - Scattered logic,容易遗漏
2. **Check in Repository** - Additional abstraction layer

**Consequences:**
- ✅ Single responsibility
- ✅ Easy to test
- ✅ Centralized logic
- ⚠️ AIApi now depends on SettingsStore (acceptable)

---

## Reuse Strategy

### Open Source Components

**None Required** - Using standard AndroidX and Jetpack libraries

### Glue Code

**Minimal Glue Code Needed:**
1. Navigation integration (standard pattern)
2. ViewModel creation (standard DI)
3. State collection (standard Compose)

---

## Interface Contracts

### Data Contract

**SettingsStore Contract:**
```kotlin
// Get current enabled state
val summaryEnabled: StateFlow<Boolean>

// Update enabled state
fun setSummaryEnabled(value: Boolean)

// Get current language
val summaryLanguage: StateFlow<SummaryLanguage>

// Update language
fun setSummaryLanguage(value: SummaryLanguage)
```

**Stability:** Stable (part of data layer)

### UI Contract

**SummarySettingsScreen Contract:**
```kotlin
// Navigation callback
onNavigateUp: () -> Unit

// ViewModel
viewModel: SummarySettingsViewModel
```

**Stability:** Stable (screen interface)

### Navigation Contract

**Route:** `settings/summary`

**Parameters:** None

**Stability:** Stable (navigation route)

---

## Modularity

### Module Boundaries

**UI Module** (`ui.compose.settings`)
- SummarySettingsScreen
- SwitchSettingItem
- LanguageSelector

**ViewModel Module** (`ui.compose.settings`)
- SummarySettingsViewModel

**Data Module** (`archmodel`)
- SettingsStore (extended)

**Navigation Module** (`ui.compose.navigation`)
- NavigationDestinations (extended)

### Replaceability

**SettingsStore:** Can be replaced with DataStore in future
**ViewModel:** Can be mocked for testing
**UI Components:** Can be replaced with alternative implementations

---

## Integration Strategy

### Migration Path

**Phase 1: Add Data Layer**
1. Add `PREF_SUMMARY_ENABLED` constant
2. Add `summaryEnabled` StateFlow to SettingsStore
3. Add `setSummaryEnabled()` method
4. Test persistence

**Phase 2: Add ViewModel**
1. Create SummarySettingsViewModel
2. Connect to SettingsStore
3. Test state management

**Phase 3: Add Navigation**
1. Create SummarySettingsDestination
2. Register in NavigationDestinations
3. Add navigation handler in AIProviderSection
4. Test navigation flow

**Phase 4: Add UI**
1. Create SummarySettingsScreen
2. Implement switch and language selector
3. Test UI interactions

**Phase 5: Integrate Business Logic**
1. Modify AIApi.summarize()
2. Test with enabled/disabled states
3. Verify no regressions

### Backward Compatibility

**No Migration Needed:**
- New preference defaults to `true`
- Existing users keep their language setting
- No breaking changes

---

## Conclusion

**Architecture Summary:**
- ✅ Follows existing MVVM pattern
- ✅ Consistent with codebase conventions
- ✅ Simple and maintainable
- ✅ Testable and scalable
- ✅ Performance-conscious

**Ready for Implementation:**
- ✅ All components defined
- ✅ Data flow documented
- ✅ Interfaces specified
- ✅ Testing strategy outlined

**Next Phase:** UI/UX Design

---

**Architecture Design Complete:** 2026-01-01 19:13:00 +08:00
