# Implementation Plan: AI Summary Language Configuration

**Created:** 2026-01-01 09:53:49
**Status:** Complete
**Estimated Effort:** ~11 hours

---

## Implementation Strategy

This plan follows an incremental approach, implementing core functionality first, then adding UI, and finally testing. Each phase can be committed independently.

**Phases:**
1. Foundation: String Resources (30 min)
2. Core: AI Provider Updates (2.5 hours)
3. Integration: AIApi Updates (30 min)
4. UI: Language Selector (2 hours)
5. ViewModel: Event Handling (30 min)
6. Testing: Unit & Integration (2 hours)
7. QA: Manual Testing (2 hours)
8. Documentation: Updates (1 hour)

---

## Phase 1: Foundation - String Resources

**Duration:** 30 minutes
**Priority:** HIGH (blocks UI implementation)

### Tasks

#### T1.1: Add String Resources

**File:** `app/src/main/res/values/strings.xml`

**Location:** After existing AI-related strings

**Add:**
```xml
<!-- Summary Language Settings -->
<string name="summary_language_title">Summary Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>

<!-- Language Options -->
<string name="summary_language_auto_detect">Auto-detect</string>
<string name="summary_language_english">English</string>
<string name="summary_language_chinese">Chinese</string>
<string name="summary_language_spanish">Spanish</string>
<string name="summary_language_french">French</string>
<string name="summary_language_german">German</string>
<string name="summary_language_japanese">Japanese</string>
<string name="summary_language_korean">Korean</string>
<string name="summary_language_portuguese">Portuguese</string>
<string name="summary_language_russian">Russian</string>
<string name="summary_language_arabic">Arabic</string>
<string name="summary_language_hindi">Hindi</string>
```

**Verification:**
- Build succeeds with no missing resource errors
- All string IDs are unique

**Commit Message:**
```
feat: add string resources for summary language configuration

Add display names for 12 language options (11 languages + auto-detect)
in AI settings. Resources include title, description, and language
names.

Related: #002-ai-summary-language-config
```

---

## Phase 2: Core - AI Provider Updates

**Duration:** 2.5 hours
**Priority:** HIGH (core functionality)

### Tasks

#### T2.1: Update AIClient Interface

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

**Location:** Interface definition (~line 25)

**Change:**
```kotlin
// BEFORE
suspend fun generateSummary(content: String): SummaryResult

// AFTER
suspend fun generateSummary(
    content: String,
    language: SummaryLanguage = SummaryLanguage.AUTO_DETECT
): SummaryResult
```

**Rationale:** Add language parameter with default for backward compatibility

#### T2.2: Update OpenAICompatibleClient

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

**Location:** After existing imports, before `generateSummary()` method

**Add Method:**
```kotlin
private fun buildSummaryPrompt(language: SummaryLanguage): String {
    return when (language) {
        SummaryLanguage.AUTO_DETECT -> """
            You are a helpful assistant that summarizes news articles.
            Detect the article's language and summarize in that same language.

            Start your response with "Lang: " followed by the detected language code.
            For example: "Lang: en"

            Then provide a concise summary of the article.
        """.trimIndent()

        else -> """
            You are a helpful assistant that summarizes news articles in ${language.languageName}.

            Provide a concise summary of the following article in ${language.languageName}.
        """.trimIndent()
    }
}
```

**Modify `generateSummary()` Method:**

**Location:** ~line 77

**Change:**
```kotlin
// BEFORE
override suspend fun generateSummary(content: String): AIClient.SummaryResult {

// AFTER
override suspend fun generateSummary(
    content: String,
    language: SummaryLanguage
): AIClient.SummaryResult {
```

**Update Prompt Usage:**

**Location:** ~line 83 (where systemPrompt is defined)

**Change:**
```kotlin
// BEFORE
val systemPrompt = """You are a helpful assistant..."""

// AFTER
val systemPrompt = buildSummaryPrompt(language)
```

**Verification:**
- Build succeeds
- Method signature matches interface
- Prompt includes language instruction

#### T2.3: Update AnthropicClient

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

**Same changes as T2.2, adapted for Anthropic:**

1. Add `buildSummaryPrompt()` method (same implementation)
2. Update `generateSummary()` signature
3. Use `buildSummaryPrompt(language)` instead of hardcoded prompt

**Note:** May need to test prompt format with Claude API and adjust if needed.

**Commit Message:**
```
feat: add language parameter to AI provider interfaces

Update AIClient, OpenAICompatibleClient, and AnthropicClient to
accept SummaryLanguage parameter in generateSummary(). Implement
language-specific prompt generation for auto-detect and specific
languages.

Backward compatible: default value is AUTO_DETECT.

Related: #002-ai-summary-language-config
```

---

## Phase 3: Integration - AIApi Updates

**Duration:** 30 minutes
**Priority:** HIGH (connects settings to providers)

### Tasks

#### T3.1: Update AIApi.summarize()

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Location:** `summarize()` method (~line 77)

**Change:**
```kotlin
// BEFORE
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        client.generateSummary(content)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
    }
}

// AFTER
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
    }
}
```

**Rationale:** Retrieve language from settings and pass to client

**Verification:**
- Build succeeds
- Language retrieved from repository
- Language passed to client

**Commit Message:**
```
feat: integrate language setting into summary generation

Retrieve selected language from repository.summaryLanguage and
pass to AI providers when generating summaries. Language setting
now affects all summary generation requests.

Related: #002-ai-summary-language-config
```

---

## Phase 4: UI - Language Selector

**Duration:** 2 hours
**Priority:** HIGH (user-facing feature)

### Tasks

#### T4.1: Add AISettingsEvent.SetSummaryLanguage

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Location:** `AISettingsEvent` sealed interface (~line 594)

**Add:**
```kotlin
sealed interface AISettingsEvent {
    // ... existing events ...

    data class SetSummaryLanguage(
        val language: SummaryLanguage
    ) : AISettingsEvent
}
```

#### T4.2: Create SummaryLanguageSelector Composable

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Location:** After `AIModelsStatus()` function, before data classes

**Add:**
```kotlin
@Composable
fun SummaryLanguageSelector(
    currentLanguage: SummaryLanguage,
    onLanguageChange: (SummaryLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = stringResource(currentLanguage.displayName),
            onValueChange = {},
            label = {
                Text(stringResource(R.string.summary_language_title))
            },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SummaryLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(language.displayName),
                            color = if (language == currentLanguage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onLanguageChange(language)
                        expanded = false
                    },
                    leadingIcon = if (language == currentLanguage) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else null,
                )
            }
        }
    }
}
```

**Import Required Icons:**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
```

#### T4.3: Integrate into AIProviderSectionEdit

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Location:** In `AIProviderSectionEdit()`, after timeout field (~line 430)

**Add:**
```kotlin
// Summary Language Selector
val currentLanguage by repository.summaryLanguage.collectAsStateWithLifecycle()

SummaryLanguageSelector(
    currentLanguage = currentLanguage,
    onLanguageChange = { language ->
        onEvent(AISettingsEvent.SetSummaryLanguage(language))
    },
    modifier = Modifier.fillMaxWidth(),
)
```

**Import Required:**
```kotlin
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
```

**Verification:**
- Build succeeds
- Dropdown appears in settings
- All 12 languages shown
- Selection updates visually

**Commit Message:**
```
feat: add language selector to AI settings UI

Implement SummaryLanguageSelector dropdown component in
AIProviderSection. Users can now select from 12 language
options (11 languages + auto-detect). Selection persists
and updates immediately.

UI follows Material 3 ExposedDropdownMenuBox pattern.

Related: #002-ai-summary-language-config
```

---

## Phase 5: ViewModel - Event Handling

**Duration:** 30 minutes
**Priority:** HIGH (connects UI to settings)

### Tasks

#### T5.1: Expose summaryLanguage in SettingsViewModel

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Location:** In class body, with other settings flows

**Add:**
```kotlin
val summaryLanguage: StateFlow<SummaryLanguage> =
    settingsStore.summaryLanguage
```

**Import:**
```kotlin
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
```

#### T5.2: Handle SetSummaryLanguage Event

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Location:** In `onEvent()` method or wherever events are handled

**Add to when expression:**
```kotlin
when (event) {
    is AISettingsEvent.SetSummaryLanguage -> {
        settingsStore.setSummaryLanguage(event.language)
    }
    // ... existing event handlers ...
}
```

**Note:** If SettingsViewModel doesn't directly handle AISettingsEvent, this may be in AIProviderSectionViewModel or similar. Adjust location accordingly.

**Verification:**
- Build succeeds
- Event handler calls SettingsStore
- Language state exposed for UI

**Commit Message:**
```
feat: wire language setting through ViewModel

Expose summaryLanguage StateFlow from SettingsStore and
handle SetSummaryLanguage events in ViewModel. UI can now
observe and update language preference through standard
ViewModel pattern.

Related: #002-ai-summary-language-config
```

---

## Phase 6: Testing

**Duration:** 2 hours
**Priority:** MEDIUM (ensure quality)

### Tasks

#### T6.1: Unit Tests - Prompt Generation

**File:** Create `app/src/test/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClientTest.kt`

**Add Tests:**
```kotlin
class OpenAICompatibleClientTest {
    private lateinit var client: OpenAICompatibleClient
    private val settings = OpenAISettings()

    @Before
    fun setup() {
        client = OpenAICompatibleClient(settings)
    }

    @Test
    fun `buildSummaryPrompt for AUTO_DETECT includes language detection instruction`() {
        val prompt = client.buildSummaryPrompt(SummaryLanguage.AUTO_DETECT)

        assertThat(prompt).contains("Lang: ")
        assertThat(prompt).contains("detected language code")
    }

    @Test
    fun `buildSummaryPrompt for Spanish includes Spanish instruction`() {
        val prompt = client.buildSummaryPrompt(SummaryLanguage.SPANISH)

        assertThat(prompt).contains("Spanish")
        assertThat(prompt).doesNotContain("Lang: ")
    }

    @Test
    fun `buildSummaryPrompt for all languages returns non-empty prompts`() {
        SummaryLanguage.entries.forEach { language ->
            val prompt = client.buildSummaryPrompt(language)
            assertThat(prompt).isNotEmpty()
            assertThat(prompt.trim()).isNotEmpty()
        }
    }
}
```

#### T6.2: Unit Tests - Settings Persistence

**File:** Add to `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt`

**Add Tests:**
```kotlin
@Test
fun `setSummaryLanguage persists to SharedPreferences`() {
    val testLanguage = SummaryLanguage.FRENCH

    settingsStore.setSummaryLanguage(testLanguage)

    verify(sp).edit()
    verify(editor).putString("pref_summary_language", "fr")
    verify(editor).apply()
}

@Test
fun `summaryLanguage loads from SharedPreferences`() {
    whenever(sp.getString("pref_summary_language", null))
        .thenReturn("es")

    val language = settingsStore.summaryLanguage.value

    assertThat(language).isEqualTo(SummaryLanguage.SPANISH)
}

@Test
fun `summaryLanguage defaults to AUTO_DETECT when not set`() {
    whenever(sp.getString("pref_summary_language", null))
        .thenReturn(null)

    val language = settingsStore.summaryLanguage.value

    assertThat(language).isEqualTo(SummaryLanguage.AUTO_DETECT)
}
```

#### T6.3: Integration Test - End-to-End Flow

**File:** Create `app/src/test/java/com/nononsenseapps/feeder/ai/AIApiIntegrationTest.kt`

**Add Test:**
```kotlin
@Test
fun `summarize retrieves language from repository and passes to client`() = runTest {
    // Setup
    val mockRepository = mock<Repository>()
    val mockClient = mock<AIClient>()
    whenever(mockRepository.summaryLanguage).thenReturn(
        MutableStateFlow(SummaryLanguage.GERMAN)
    )
    whenever(mockClient.generateSummary(any(), any())).thenReturn(
        AIClient.SummaryResult.Success(
            id = "test",
            created = 0L,
            model = "test-model",
            content = "Test summary",
            promptTokens = 10,
            completeTokens = 20,
            totalTokens = 30,
            detectedLanguage = "de"
        )
    )

    val api = AIApi(mockRepository, "en")
    api.client = mockClient // Inject mock

    // Execute
    val result = api.summarize("Test content")

    // Verify
    verify(mockClient).generateSummary("Test content", SummaryLanguage.GERMAN)
    assertThat(result).isInstanceOf<AIClient.SummaryResult.Success>()
}
```

**Verification:**
- All tests pass
- Code coverage > 80% for new code
- No test failures or flakiness

**Commit Message:**
```
test: add unit and integration tests for language configuration

Test prompt generation for all languages, settings persistence,
and end-to-end flow from settings through AIApi to providers.

Coverage: prompt generation, state management, integration.

Related: #002-ai-summary-language-config
```

---

## Phase 7: Manual QA

**Duration:** 2 hours
**Priority:** MEDIUM (user experience validation)

### Test Scenarios

#### QA-1: Language Selection and Persistence

**Steps:**
1. Install app with fresh profile (or clear data)
2. Open AI Settings
3. Find "Summary Language" field
4. Verify it shows "Auto-detect"
5. Tap dropdown
6. Verify all 12 languages listed
7. Select "Spanish"
8. Verify field updates to "Spanish"
9. Verify checkmark on "Spanish"
10. Tap "Save"
11. Close app completely
12. Reopen app
13. Open AI Settings
14. Verify field still shows "Spanish"

**Expected:** All steps pass

**Actual:** _______________

**Issues:** _______________

#### QA-2: Summary Generation with Specific Language

**Steps:**
1. Set language to "English"
2. Find a news article in Spanish or French
3. Tap "Summarize" button
4. Wait for summary to generate
5. Read summary
6. Verify summary is in English

**Expected:** Summary in English regardless of article language

**Actual:** _______________

**Issues:** _______________

#### QA-3: Summary Generation with Auto-Detect

**Steps:**
1. Set language to "Auto-detect"
2. Find an English article
3. Generate summary
4. Check if response starts with "Lang: en"
5. Verify summary is in English
6. Repeat with Spanish article
7. Verify response starts with "Lang: es"
8. Verify summary is in Spanish

**Expected:** Auto-detect works, language prefix present

**Actual:** _______________

**Issues:** _______________

#### QA-4: Provider Compatibility

**Steps:**
1. Test with OpenAI-compatible provider (e.g., OpenAI)
2. Test with Anthropic provider
3. Verify language setting works with both
4. Switch between providers
5. Verify language setting persists

**Expected:** Language setting works with all providers

**Actual:** _______________

**Issues:** _______________

#### QA-5: Edge Cases

**Steps:**
1. Open/close dropdown rapidly 5 times
2. Switch languages 5 times rapidly
3. Rotate device while dropdown open
4. Background app while dropdown open
5. Generate summary, then change language immediately

**Expected:** No crashes, no state corruption

**Actual:** _______________

**Issues:** _______________

#### QA-6: Accessibility

**Steps:**
1. Enable TalkBack
2. Open AI Settings
3. Navigate to language dropdown
4. Expand dropdown
5. Navigate through options
6. Verify announcements ("Selected, Spanish", "English", etc.)
7. Select option with double-tap
8. Verify selection announced

**Expected:** Full accessibility support

**Actual:** _______________

**Issues:** _______________

---

## Phase 8: Documentation

**Duration:** 1 hour
**Priority:** LOW (but important)

### Tasks

#### T8.1: Update Implementation Summary

**File:** `specifications/002-ai-summary-language-config/09-implementation-summary.md`

**Update:**
- Mark all tasks complete
- Add notes on any deviations from spec
- Document technical decisions made during implementation
- Record any challenges encountered

#### T8.2: Update Specification (if needed)

**File:** `specifications/002-ai-summary-language-config/06-specification.md`

**Add:** `[UPDATED: YYYY-MM-DD]` markers for any changes

**Example:**
```markdown
### 5.1 Components

[UPDATED: 2026-01-01] Added SummaryLanguageSelector component to AIProviderSection.
```

#### T8.3: Create User Documentation (optional)

If this feature is user-facing and requires explanation:

**File:** `docs/user-guide/ai-features.md` (if exists)

**Add Section:**
```markdown
## Summary Language

You can choose the language for AI-generated summaries:

1. Open Settings
2. Tap "AI Settings"
3. Tap "Summary Language"
4. Select your preferred language
5. Tap "Save"

**Auto-detect:** The AI will detect the article's language and summarize in that same language.

**Specific Language:** The AI will always summarize in the selected language, regardless of the article's original language.
```

**Commit Message:**
```
docs: update documentation for language configuration feature

Document implementation, update specification with changes,
add user-facing documentation for summary language selection.

Related: #002-ai-summary-language-config
```

---

## Implementation Checklist

### Prerequisites
- [ ] Feature branch created from master
- [ ] Latest code pulled from master
- [ ] Development environment set up

### Foundation (Phase 1)
- [ ] Add string resources to strings.xml
- [ ] Verify build succeeds
- [ ] Commit changes

### Core (Phase 2)
- [ ] Update AIClient interface
- [ ] Update OpenAICompatibleClient
- [ ] Update AnthropicClient
- [ ] Test prompt generation manually
- [ ] Commit changes

### Integration (Phase 3)
- [ ] Update AIApi.summarize()
- [ ] Verify language retrieval
- [ ] Test with mock repository
- [ ] Commit changes

### UI (Phase 4)
- [ ] Add AISettingsEvent.SetSummaryLanguage
- [ ] Create SummaryLanguageSelector composable
- [ ] Integrate into AIProviderSectionEdit
- [ ] Test UI in isolation
- [ ] Commit changes

### ViewModel (Phase 5)
- [ ] Expose summaryLanguage in ViewModel
- [ ] Handle SetSummaryLanguage event
- [ ] Test ViewModel integration
- [ ] Commit changes

### Testing (Phase 6)
- [ ] Write prompt generation tests
- [ ] Write settings persistence tests
- [ ] Write integration tests
- [ ] Verify all tests pass
- [ ] Commit changes

### QA (Phase 7)
- [ ] Complete QA-1: Selection and Persistence
- [ ] Complete QA-2: Specific Language Summary
- [ ] Complete QA-3: Auto-dect Summary
- [ ] Complete QA-4: Provider Compatibility
- [ ] Complete QA-5: Edge Cases
- [ ] Complete QA-6: Accessibility
- [ ] Document any issues found

### Documentation (Phase 8)
- [ ] Update implementation summary
- [ ] Update specification if needed
- [ ] Create user documentation if applicable
- [ ] Commit documentation changes

### Final Steps
- [ ] Run full build: `./gradlew build`
- [ ] Run all tests: `./gradlew test`
- [ ] Run lint: `./gradlew lint`
- [ ] Verify no warnings or errors
- [ ] Create final commit with all changes
- [ ] Push to feature branch
- [ ] Create pull request
- [ ] Request code review

---

## Risk Mitigation

### Risk 1: AI Doesn't Follow Language Instruction

**Mitigation:**
- Test with real API calls during development
- Have fallback: if first attempt fails, try more explicit prompt
- Document model-specific behaviors in code comments

### Risk 2: String Resource Build Errors

**Mitigation:**
- Create all strings upfront (Phase 1)
- Build after adding strings
- Use `@StringRes` annotations for compile-time checking

### Risk 3: State Not Updating

**Mitigation:**
- Use `collectAsStateWithLifecycle()` (not `collectAsState()`)
- Verify StateFlow emission in unit tests
- Test configuration changes (rotation, backgrounding)

### Risk 4: UI Not Appearing

**Mitigation:**
- Test UI in isolation with Preview
- Add to AIProviderSectionEdit in correct location
- Verify modifier parameters (fillMaxWidth, etc.)

---

## Success Criteria

Implementation is complete when:

1. ✅ All 12 languages selectable in UI
2. ✅ Selection persists across app restarts
3. ✅ Summaries generated in selected language
4. ✅ Auto-detect includes "Lang:" prefix
5. ✅ Works with both OpenAI and Anthropic providers
6. ✅ All unit tests pass
7. ✅ All integration tests pass
8. ✅ Manual QA tests pass
9. ✅ No build warnings
10. ✅ Code review approved

---

## Rollback Plan

If critical issues found:

1. **Option 1:** Hide language selector UI
   - Comment out SummaryLanguageSelector in AIProviderSectionEdit
   - Language setting still functional but not visible

2. **Option 2:** Ignore language parameter
   - Set default language parameter to AUTO_DETECT in AIApi
   - Always use auto-dect regardless of setting

3. **Option 3:** Full revert
   - Revert commits for this feature
   - No data loss (setting can be reapplied later)

**Recommended:** Start with Option 1, escalate to Option 3 if needed.

---

## Next Steps After Implementation

1. **Monitor:** Track user language selections (analytics)
2. **Gather Feedback:** User reports on language accuracy
3. **Iterate:** Refine prompts based on real-world results
4. **Enhance:** Add features like per-feed language settings

---

**Last Updated:** 2026-01-01 09:53:49
**Ready for Implementation:** Yes
**Estimated Completion:** 1-2 days
