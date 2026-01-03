# Testing Strategy: Translation Configuration

**Specification:** [./01-tech-spec.md](./01-tech-spec.md)
**Implementation Plan:** [./02-implementation-plan.md](./02-implementation-plan.md)
**Task List:** [./03-tasks.md](./03-tasks.md)

## Overview

This document defines the comprehensive testing strategy for the translation configuration feature. The strategy covers unit tests, integration tests, UI tests, accessibility tests, and performance tests to ensure the feature is robust, accessible, and performant.

## Testing Philosophy

Following the project's development philosophy:
- **从现有代码学习**: Test patterns match existing tests in the codebase
- **渐进式开发**: Write tests alongside implementation, each commit passes tests
- **务实而非教条**: Focus on high-value tests that catch real bugs
- **可测试性**: All components are designed to be testable from the start

## Test Coverage Goals

| Type | Target Coverage | Rationale |
|------|----------------|-----------|
| Unit Tests | >90% | Covers business logic, data layer, ViewModel |
| UI Tests | >80% | Covers user interactions, navigation |
| Integration Tests | Key flows only | End-to-end verification |
| Accessibility Tests | 100% of interactive elements | WCAG 2.1 AA compliance |

## 1. Unit Tests

### 1.1 TranslationLanguage Enum Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ai/model/TranslationLanguageTest.kt`

```kotlin
class TranslationLanguageTest {
    @Test
    fun `fromCode with valid code returns correct enum`() {
        assertEquals(TranslationLanguage.ENGLISH, TranslationLanguage.fromCode("en"))
        assertEquals(TranslationLanguage.CHINESE, TranslationLanguage.fromCode("zh"))
        assertEquals(TranslationLanguage.SPANISH, TranslationLanguage.fromCode("es"))
    }

    @Test
    fun `fromCode with invalid code returns DEVICE_DEFAULT`() {
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, TranslationLanguage.fromCode("invalid"))
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, TranslationLanguage.fromCode("xx"))
    }

    @Test
    fun `fromCode with null returns DEVICE_DEFAULT`() {
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, TranslationLanguage.fromCode(null))
    }

    @Test
    fun `fromCode with empty string returns DEVICE_DEFAULT`() {
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, TranslationLanguage.fromCode(""))
    }

    @Test
    fun `all enum entries have non-empty properties`() {
        TranslationLanguage.entries.forEach { entry ->
            assertTrue(entry.displayName > 0, "displayName must be valid string resource")
            assertTrue(entry.languageName.isNotBlank(), "languageName must not be blank")
            assertTrue(entry.code.isNotBlank() || entry == TranslationLanguage.DEVICE_DEFAULT,
                "Only DEVICE_DEFAULT can have empty code")
        }
    }

    @Test
    fun `enum entries are in sync with language list`() {
        // Verify we have DEVICE_DEFAULT + 12 languages
        assertEquals(13, TranslationLanguage.entries.size)
    }
}
```

### 1.2 SettingsStore Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`

```kotlin
class SettingsStoreTranslationTest {
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setup() {
        mockSharedPreferences = mock()
        mockEditor = mock()
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.apply()).then {}

        // Default prefs
        whenever(mockSharedPreferences.getBoolean("pref_translation_enabled", false))
            .thenReturn(false)
        whenever(mockSharedPreferences.getString("pref_translation_language", null))
            .thenReturn(null)

        settingsStore = SettingsStore(mockSharedPreferences)
    }

    @Test
    fun `translationEnabled defaults to false`() {
        val observer = settingsStore.translationEnabled.test()
        assertEquals(false, observer.values().first())
    }

    @Test
    fun `setTranslationEnabled updates StateFlow and persists`() {
        val observer = settingsStore.translationEnabled.test()

        settingsStore.setTranslationEnabled(true)

        assertEquals(true, observer.values().last())
        verify(mockEditor).putBoolean("pref_translation_enabled", true)
        verify(mockEditor).apply()
    }

    @Test
    fun `translationLanguage defaults to DEVICE_DEFAULT`() {
        whenever(mockSharedPreferences.getString("pref_translation_language", null))
            .thenReturn(null)

        val observer = settingsStore.translationLanguage.test()
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, observer.values().first())
    }

    @Test
    fun `translationLanguage loads persisted code`() {
        whenever(mockSharedPreferences.getString("pref_translation_language", null))
            .thenReturn("zh")

        val store = SettingsStore(mockSharedPreferences)
        val observer = store.translationLanguage.test()
        assertEquals(TranslationLanguage.CHINESE, observer.values().first())
    }

    @Test
    fun `setTranslationLanguage persists code and updates StateFlow`() {
        val observer = settingsStore.translationLanguage.test()

        settingsStore.setTranslationLanguage(TranslationLanguage.SPANISH)

        assertEquals(TranslationLanguage.SPANISH, observer.values().last())
        verify(mockEditor).putString("pref_translation_language", "es")
        verify(mockEditor).apply()
    }

    @Test
    fun `translationLanguage StateFlow emits on change`() {
        val observer = settingsStore.translationLanguage.test()

        settingsStore.setTranslationLanguage(TranslationLanguage.FRENCH)
        settingsStore.setTranslationLanguage(TranslationLanguage.GERMAN)

        assertEquals(3, observer.values().size) // initial + 2 changes
    }
}
```

### 1.3 Repository Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/archmodel/RepositoryTest.kt`

```kotlin
class RepositoryTranslationTest {
    private lateinit var mockSettingsStore: SettingsStore
    private lateinit var repository: Repository

    @Before
    fun setup() {
        mockSettingsStore = mock()
        repository = Repository(mockSettingsStore)
    }

    @Test
    fun `translationEnabled exposes StateFlow from SettingsStore`() {
        val flow = MutableStateFlow(false)
        whenever(mockSettingsStore.translationEnabled).thenReturn(flow)

        assertEquals(flow, repository.translationEnabled)
    }

    @Test
    fun `translationLanguage exposes StateFlow from SettingsStore`() {
        val flow = MutableStateFlow(TranslationLanguage.DEVICE_DEFAULT)
        whenever(mockSettingsStore.translationLanguage).thenReturn(flow)

        assertEquals(flow, repository.translationLanguage)
    }

    @Test
    fun `setTranslationEnabled delegates to SettingsStore`() {
        repository.setTranslationEnabled(true)

        verify(mockSettingsStore).setTranslationEnabled(true)
    }

    @Test
    fun `setTranslationLanguage delegates to SettingsStore`() {
        repository.setTranslationLanguage(TranslationLanguage.CHINESE)

        verify(mockSettingsStore).setTranslationLanguage(TranslationLanguage.CHINESE)
    }
}
```

### 1.4 ViewModel Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModelTest.kt`

```kotlin
class TranslationSettingsViewModelTest {
    private lateinit var viewModel: TranslationSettingsViewModel
    private lateinit var mockRepository: Repository
    private lateinit var mockDI: DI

    @Before
    fun setup() {
        mockRepository = mock()
        mockDI = DI {
            bind<Repository>() with instance(mockRepository)
        }

        // Setup StateFlows
        whenever(mockRepository.translationEnabled)
            .thenReturn(MutableStateFlow(false))
        whenever(mockRepository.translationLanguage)
            .thenReturn(MutableStateFlow(TranslationLanguage.DEVICE_DEFAULT))

        viewModel = TranslationSettingsViewModel(mockDI)
    }

    @Test
    fun `translationEnabled exposes StateFlow from Repository`() {
        assertNotNull(viewModel.translationEnabled)
    }

    @Test
    fun `translationLanguage exposes StateFlow from Repository`() {
        assertNotNull(viewModel.translationLanguage)
    }

    @Test
    fun `setTranslationEnabled calls Repository`() = runTest {
        viewModel.setTranslationEnabled(true)

        verify(mockRepository).setTranslationEnabled(true)
    }

    @Test
    fun `setTranslationLanguage calls Repository`() = runTest {
        viewModel.setTranslationLanguage(TranslationLanguage.SPANISH)

        verify(mockRepository).setTranslationLanguage(TranslationLanguage.SPANISH)
    }

    @Test
    fun `ViewModel uses viewModelScope for coroutines`() = runTest {
        // Verify that coroutine scope is used
        // This is implicit in the implementation, but we can verify async behavior
        val job = viewModel.setTranslationEnabled(true)
        assertTrue(job.isCompleted)
    }
}
```

## 2. UI Tests

### 2.1 TranslationSettingsScreen Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreenTest.kt`

```kotlin
class TranslationSettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: TranslationSettingsViewModel

    @Before
    fun setup() {
        mockViewModel = mock()
        whenever(mockViewModel.translationEnabled)
            .thenReturn(MutableStateFlow(false))
        whenever(mockViewModel.translationLanguage)
            .thenReturn(MutableStateFlow(TranslationLanguage.DEVICE_DEFAULT))
    }

    @Test
    fun `screen renders toggle and language selector`() {
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                viewModel = mockViewModel
            )
        }

        // Verify toggle is displayed
        composeTestRule.onNodeWithText("Enable Auto Translation").assertIsDisplayed()

        // Verify language selector is displayed
        composeTestRule.onNodeWithText("Target Language").assertIsDisplayed()
    }

    @Test
    fun `toggle switch updates ViewModel`() {
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                viewModel = mockViewModel
            )
        }

        // Find and click the switch
        composeTestRule.onNodeWithContentDescription("Enable Auto Translation")
            .performClick()

        verify(mockViewModel).setTranslationEnabled(true)
    }

    @Test
    fun `language dropdown expands on click`() {
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                viewModel = mockViewModel
            )
        }

        // Click on language selector
        composeTestRule.onNodeWithText("Target Language").performClick()

        // Verify dropdown is shown (check for first language)
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun `selecting language updates ViewModel`() {
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                viewModel = mockViewModel
            )
        }

        // Open dropdown
        composeTestRule.onNodeWithText("Target Language").performClick()

        // Select Chinese
        composeTestRule.onNodeWithText("Chinese").performClick()

        verify(mockViewModel).setTranslationLanguage(TranslationLanguage.CHINESE)
    }

    @Test
    fun `language selector is disabled when translation is off`() {
        whenever(mockViewModel.translationEnabled)
            .thenReturn(MutableStateFlow(false))

        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                viewModel = mockViewModel
            )
        }

        // Verify language selector is not enabled
        composeTestRule.onNodeWithText("Target Language")
            .assertIsNotEnabled()
    }

    @Test
    fun `back navigation calls onNavigateUp`() {
        var backPressed = false
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = { backPressed = true },
                viewModel = mockViewModel
            )
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Go back").performClick()

        assertTrue(backPressed)
    }

    @Test
    fun `provider link navigates to provider list`() {
        var navigatedToProviders = false
        composeTestRule.setContent {
            TranslationSettingsScreen(
                onNavigateUp = {},
                onNavigateToProviderList = { navigatedToProviders = true },
                viewModel = mockViewModel
            )
        }

        // Click provider link
        composeTestRule.onNodeWithText("AI Provider").performClick()

        assertTrue(navigatedToProviders)
    }
}
```

### 2.2 Navigation Integration Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/navigation/NavigationTest.kt`

```kotlin
class TranslationNavigationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `can navigate to Translation Settings from Settings`() {
        // Navigate to Settings
        onView(withId(R.id.settings)).perform(click())

        // Click Translation Settings link
        onView(withText("Translation Settings")).perform(click())

        // Verify Translation Settings screen is shown
        onView(withText("Enable Auto Translation")).check(matches(isDisplayed()))
    }

    @Test
    fun `back navigation returns to Settings`() {
        // Navigate to Settings
        onView(withId(R.id.settings)).perform(click())

        // Navigate to Translation Settings
        onView(withText("Translation Settings")).perform(click())

        // Press back
        pressBack()

        // Verify we're back on Settings screen
        onView(withText("Translation Settings")).check(matches(isDisplayed()))
    }
}
```

## 3. Integration Tests

### 3.1 Settings Persistence Integration Test

```kotlin
class TranslationSettingsIntegrationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `settings persist across app restarts`() {
        // Navigate to Translation Settings
        // ... navigation steps ...

        // Enable translation
        onView(withText("Enable Auto Translation")).perform(click())

        // Select Chinese
        onView(withText("Target Language")).perform(click())
        onView(withText("Chinese")).perform(click())

        // Restart app
        activityRule.scenario.recreate()

        // Navigate back to Translation Settings
        // ... navigation steps ...

        // Verify settings persisted
        onView(withText("Enable Auto Translation"))
            .check(matches(isChecked()))

        onView(withText("Target Language"))
            .check(matches(withSubstring("Chinese")))
    }
}
```

### 3.2 Deep Link Integration Test

```kotlin
class DeepLinkIntegrationTest {
    @Test
    fun `deep link opens Translation Settings`() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("feeder://settings/translation")
        }

        activityRule.launchActivity(intent)

        // Verify Translation Settings is shown
        onView(withText("Enable Auto Translation"))
            .check(matches(isDisplayed()))
    }
}
```

## 4. Accessibility Tests

### 4.1 TalkBack Tests

**Manual Test Steps:**
1. Enable TalkBack in device settings
2. Open Translation Settings
3. Navigate through each element
4. Verify announcements:
   - Toggle: "Enable Auto Translation, Switch, Off" (or "On")
   - Language selector: "Target Language, English, Button, Double tap to change"
   - Provider link: "AI Provider, OpenAI Compatible, Button, Double tap to manage providers"

**Expected Results:**
- All interactive elements are announced
- State changes are announced ("On", "Off")
- Roles are correct (Switch, Button)
- Hints are provided where appropriate

### 4.2 Switch Access Tests

**Manual Test Steps:**
1. Enable Switch Access
2. Open Translation Settings
3. Navigate through all controls
4. Verify all controls are accessible
5. Test toggle and language selection

**Expected Results:**
- All controls reachable via Switch Access
- Can toggle switch
- Can select language from dropdown
- No focus traps

### 4.3 Color Contrast Tests

**Automated Verification:**
- Use Accessibility Scanner or similar tool
- Check all text elements against WCAG AA (4.5:1 ratio)

**Expected Results:**
- Title contrast: ≥4.5:1
- Subtitle contrast: ≥4.5:1
- Switch state indication: ≥4.5:1

## 5. Performance Tests

### 5.1 Memory Leak Tests

**Test Steps:**
1. Open Translation Settings
2. Navigate back
3. Repeat 10 times
4. Use Android Profiler to monitor memory
5. Force garbage collection
6. Check for increasing memory usage

**Expected Results:**
- Memory usage stabilizes after iterations
- No increasing trend
- ViewModel cleared properly

### 5.2 Frame Rate Tests

**Test Steps:**
1. Enable GPU profiling
2. Open Translation Settings
3. Toggle switch rapidly
4. Open/close dropdown repeatedly
5. Monitor frame rate

**Expected Results:**
- 60fps maintained during interactions
- No dropped frames
- Smooth animations

### 5.3 Startup Time Tests

**Test Steps:**
1. Measure time from tapping "Translation Settings" to screen render
2. Use Android Profiler or manual stopwatch

**Expected Results:**
- Screen renders in <100ms
- No jank on navigation

## 6. Edge Case Tests

### 6.1 Invalid SharedPreferences Data

**Test Scenarios:**
- Corrupted language code in SharedPreferences
- Missing preference keys
- Null values

**Expected Behavior:**
- Defaults to DEVICE_DEFAULT
- No crashes
- Graceful degradation

### 6.2 Rapid Toggle Switching

**Test Steps:**
1. Open Translation Settings
2. Toggle switch on/off 10 times rapidly
3. Verify final state is correct

**Expected Results:**
- Last state wins
- No race conditions
- No crashes

### 6.3 Device Language Change

**Test Steps:**
1. Set translation language to DEVICE_DEFAULT
2. Change device language in system settings
3. Restart app
4. Verify DEVICE_DEFAULT picks up new language

**Expected Results:**
- No crashes
- New device language used

### 6.4 No Providers Configured

**Test Steps:**
1. Clear all AI provider configurations
2. Open Translation Settings
3. Verify provider link shows warning

**Expected Results:**
- Warning indicator displayed
- Link still navigates to provider setup

## 7. Test Execution Plan

### Pre-Commit Tests (Run Locally)
```bash
# Unit tests
./gradlew testDebugUnitTest

# Fastest subset for quick feedback
./gradlew test --tests "*TranslationLanguageTest"
./gradlew test --tests "*SettingsStoreTranslationTest"
```

### Pre-Push Tests (Run Locally)
```bash
# All unit tests
./gradlew testDebugUnitTest

# Lint checks
./gradlew lintDebug

# Build verification
./gradlew assembleDebug
```

### CI/CD Tests (Run on PR)
```bash
# All unit tests with coverage
./gradlew testDebugUnitTest coverageVerification

# UI tests (on emulator)
./gradlew connectedDebugAndroidTest

# Screenshot tests (if using Paparazzi)
./gradlew verifyPaparazziDebug
```

## 8. Test Data Management

### Test Fixtures

**File:** `app/src/test/resources/fixtures/translation-settings.json`
```json
{
  "languages": [
    {"code": "", "name": "Device Default"},
    {"code": "en", "name": "English"},
    {"code": "zh", "name": "Chinese"},
    {"code": "es", "name": "Spanish"}
  ],
  "defaultSettings": {
    "enabled": false,
    "language": "Device Default"
  }
}
```

### Mock SharedPreferences

Use a in-memory SharedPreferences implementation for tests:
```kotlin
fun createMockSharedPreferences(): SharedPreferences {
    return ContextWrapper(Application()).getSharedPreferences(
        "test_prefs",
        Context.MODE_PRIVATE
    )
}
```

## 9. Test Maintenance

### When to Update Tests
- When adding new languages to TranslationLanguage enum
- When changing UI layout
- When adding new settings
- When refactoring business logic

### Test Review Checklist
- [ ] All tests pass locally
- [ ] Coverage report shows >90% for new code
- [ ] No flaky tests (tests that sometimes fail)
- [ ] Tests are fast (<5 seconds for unit tests)
- [ ] Tests are readable and maintainable

## 10. Continuous Improvement

### Metrics to Track
- Test execution time
- Test failure rate
- Code coverage percentage
- Bug escape rate (bugs found in production vs caught by tests)

### Test Smells to Avoid
- Fragile tests that break on minor changes
- Slow tests that take too long
- Over-mocked tests that test nothing
- Tests that are too coupled to implementation

## References

- **Architecture:** [../05-architecture.md](../05-architecture.md)
- **Tech Spec:** [./01-tech-spec.md](./01-tech-spec.md)
- **Task List:** [./03-tasks.md](./03-tasks.md)
- **Testing Best Practices:** [Android Testing Guide](https://developer.android.com/training/testing)
- **Compose Testing:** [Testing Jetpack Compose](https://developer.android.com/jetpack/compose/testing)
- **Accessibility Testing:** [Accessibility Testing Guide](https://developer.android.com/guide/topics/ui/accessibility/testing)
