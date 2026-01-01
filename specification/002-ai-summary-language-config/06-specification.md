# Technical Specification: AI Summary Language Configuration

**Feature:** AI Summary Language Configuration
**Version:** 1.0
**Created:** 2026-01-01 09:53:49
**Status:** Complete

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-01 | AI Coordinator | Initial specification |

---

## 1. Introduction

### 1.1 Purpose

This specification defines the technical requirements for implementing configurable language support in AI-generated summaries for the Feeder RSS reader application.

### 1.2 Scope

**In Scope:**
- Language selection UI in AI settings
- Language parameter propagation to AI providers
- Prompt generation for specific languages
- Persistence of language preference
- Support for 11 languages + auto-detect

**Out of Scope:**
- Per-feed language settings
- Language confidence scoring
- Language validation on AI responses
- Dynamic language list (hardcoded enum)

### 1.3 References

- [Requirements Document](01-requirements.md)
- [Research Report](02-research-report.md)
- [Code Assessment](03-code-assessment.md)
- [Architecture Design](04-architecture-design.md)
- [UI/UX Design](05-ui-ux-design.md)

---

## 2. System Overview

### 2.1 Feature Description

Users can select a target language for AI-generated summaries from a dropdown in AI settings. When generating a summary, the AI is instructed to summarize in the selected language. If "Auto-detect" is selected, the AI detects the article's language and summarizes in that same language.

### 2.2 User Stories

**US-1:** As a user, I want to select a language for summaries so I can read them in my preferred language.

**US-2:** As a user, I want auto-detect to work automatically so I don't have to manually select the language for each article.

**US-3:** As a user, I want my language preference to be saved so I don't have to reselect it every time.

### 2.3 Actors

- **End User:** Selects language preference in settings
- **AI Provider:** Generates summaries in specified language
- **Settings System:** Persists and retrieves language preference

---

## 3. Functional Requirements

### FR-1: Language Selection

The system SHALL provide a dropdown in AI settings allowing selection from:
- Auto-detect
- English
- Chinese
- Spanish
- French
- German
- Japanese
- Korean
- Portuguese
- Russian
- Arabic
- Hindi

**Priority:** Must Have

**Verification:**
- UI dropdown displays all 12 options
- Current selection is visually indicated
- Selection updates immediately

### FR-2: Default Behavior

The system SHALL default to "Auto-detect" for new installations.

**Priority:** Must Have

**Verification:**
- Fresh install shows "Auto-detect" selected
- SharedPreferences has no value or empty string
- `SummaryLanguage.fromCode(null)` returns AUTO_DETECT

### FR-3: Persistence

The system SHALL persist the selected language in SharedPreferences with key `pref_summary_language`.

**Priority:** Must Have

**Verification:**
- Language selection saved to SharedPreferences
- Language restored after app restart
- Language restored after device reboot

### FR-4: Prompt Generation

The system SHALL generate AI prompts that instruct the model to summarize in the selected language.

**Priority:** Must Have

**Verification:**
- For specific language: Prompt includes "Summarize in [Language]"
- For auto-detect: Prompt includes language detection instruction

### FR-5: Provider Support

The system SHALL support language configuration for both OpenAI-compatible and Anthropic providers.

**Priority:** Must Have

**Verification:**
- OpenAI-compatible generates language-specific prompts
- Anthropic generates language-specific prompts
- Both providers accept language parameter

---

## 4. Non-Functional Requirements

### NFR-1: Performance

Language selection SHALL NOT add more than 100ms overhead to summary generation.

**Measurement:** Profile summary generation with language parameter vs without.

**Acceptance:** Difference ≤ 100ms

### NFR-2: Usability

Language selection SHALL require no more than 3 taps/clicks from settings screen.

**Test Scenario:**
1. Open AI Settings (1 tap)
2. Tap dropdown (1 tap)
3. Tap language (1 tap)
4. Total: 3 taps (excluding Save)

### NFR-3: Accessibility

Language selector SHALL be fully accessible via screen reader.

**Verification:**
- All elements have semantic labels
- Selection state is announced
- Touch targets meet 48dp minimum
- Keyboard navigation works

### NFR-4: Code Quality

Implementation SHALL follow existing code patterns and conventions.

**Verification:**
- Code review confirms consistency
- No lint warnings
- Follows Kotlin style guide
- Uses Material 3 components

---

## 5. Technical Design

### 5.1 Components

#### C-1: SummaryLanguage Enum

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/SummaryLanguage.kt`

**Status:** Already exists, no changes needed

**Responsibility:** Define available languages and their properties

#### C-2: SettingsStore

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Status:** Already implemented, no changes needed

**Responsibility:** Manage language preference StateFlow and persistence

**Interface:**
```kotlin
val summaryLanguage: StateFlow<SummaryLanguage>
fun setSummaryLanguage(value: SummaryLanguage)
```

#### C-3: AIClient Interface

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

**Status:** Needs modification

**Change:** Add language parameter to `generateSummary()`

**Before:**
```kotlin
suspend fun generateSummary(content: String): SummaryResult
```

**After:**
```kotlin
suspend fun generateSummary(
    content: String,
    language: SummaryLanguage = SummaryLanguage.AUTO_DETECT
): SummaryResult
```

#### C-4: OpenAICompatibleClient

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

**Status:** Needs modification

**Changes:**
1. Implement updated `generateSummary()` signature
2. Add `buildSummaryPrompt()` method
3. Use language in prompt generation

**New Method:**
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

#### C-5: AnthropicClient

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

**Status:** Needs modification

**Changes:** Same as OpenAICompatibleClient

**Note:** May need prompt format adjustment for Claude API

#### C-6: AIApi

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Status:** Needs modification

**Change:** Retrieve language from repository and pass to client

**Implementation:**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    return try {
        val language = repository.summaryLanguage.first()
        client.generateSummary(content, language)
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(
            content = e.message ?: e.cause?.message ?: ""
        )
    }
}
```

#### C-7: SummaryLanguageSelector (NEW)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Status:** New component

**Responsibility:** Display language selection dropdown

**Signature:**
```kotlin
@Composable
fun SummaryLanguageSelector(
    currentLanguage: SummaryLanguage,
    onLanguageChange: (SummaryLanguage) -> Unit,
    modifier: Modifier = Modifier,
)
```

#### C-8: AISettingsEvent

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Status:** Needs addition

**Add:**
```kotlin
sealed interface AISettingsEvent {
    // ... existing events ...
    data class SetSummaryLanguage(val language: SummaryLanguage) : AISettingsEvent
}
```

#### C-9: SettingsViewModel

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Status:** Needs addition

**Add:**
```kotlin
// Expose language from SettingsStore
val summaryLanguage: StateFlow<SummaryLanguage> =
    settingsStore.summaryLanguage

// Handle event
fun onEvent(event: AISettingsEvent) {
    when (event) {
        is AISettingsEvent.SetSummaryLanguage -> {
            settingsStore.setSummaryLanguage(event.language)
        }
        // ... other events
    }
}
```

#### C-10: String Resources (NEW)

**File:** `app/src/main/res/values/strings.xml`

**Status:** Needs addition

**Required Strings:**
```xml
<!-- Section -->
<string name="summary_language_title">Summary Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>

<!-- Languages -->
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

### 5.2 Data Flow

**Settings Change Flow:**
```
User selects language
    ↓
onLanguageChange(newLanguage)
    ↓
SettingsViewModel.onEvent(SetSummaryLanguage(newLanguage))
    ↓
SettingsStore.setSummaryLanguage(newLanguage)
    ↓
_summaryLanguage.value = newLanguage
    ↓
SharedPreferences.put("pref_summary_language", newLanguage.code)
    ↓
StateFlow emits new value
    ↓
UI recomposes with new selection
```

**Summary Generation Flow:**
```
User requests summary
    ↓
AIApi.summarize(content)
    ↓
repository.summaryLanguage.first()
    ↓
Returns: SummaryLanguage.SPANISH
    ↓
client.generateSummary(content, SPANISH)
    ↓
buildSummaryPrompt(SPANISH)
    ↓
Returns: "Summarize in Spanish..."
    ↓
Call AI API with prompt
    ↓
Return SummaryResult
```

### 5.3 State Management

**State Source:** SettingsStore.summaryLanguage (StateFlow<SummaryLanguage>)

**Collection:** UI collects with `collectAsStateWithLifecycle()`

**Update:** Through SettingsViewModel.setSummaryLanguage()

**Persistence:** Automatic via SharedPreferences in SettingsStore

---

## 6. Implementation Details

### 6.1 File Changes Summary

| File | Change Type | Lines Added | Lines Modified |
|------|-------------|-------------|----------------|
| AIClient.kt | Modify | 1 | 1 |
| OpenAICompatibleClient.kt | Modify | ~30 | ~5 |
| AnthropicClient.kt | Modify | ~30 | ~5 |
| AIApi.kt | Modify | ~5 | ~2 |
| AIProviderSection.kt | Add/Modify | ~120 | ~5 |
| SettingsViewModel.kt | Add | ~10 | ~5 |
| strings.xml | Add | ~14 | 0 |

**Total:** ~210 lines added, ~23 lines modified

### 6.2 Code Patterns

**Pattern 1: ExposedDropdownMenuBox**

All dropdown selectors in AI settings follow this pattern. Reuse for language selector.

**Pattern 2: StateFlow Collection**

All settings use `collectAsStateWithLifecycle()`. Follow this pattern.

**Pattern 3: Sealed Event Interface**

All settings changes use sealed interfaces. Add `SetSummaryLanguage` event.

**Pattern 4: When Expression for Prompts**

Use `when(language)` to generate different prompts. Type-safe and efficient.

### 6.3 Error Handling

**No New Error Conditions:**

This feature has no error scenarios:
- Language selection is always valid (enum)
- No network calls for settings
- No validation needed

**Existing Error Handling:**

AI API errors already handled in existing code. No changes needed.

---

## 7. Testing Strategy

### 7.1 Unit Tests

**Test Suite 1: Prompt Generation**

**File:** `OpenAICompatibleClientTest.kt`

**Test Cases:**
```kotlin
@Test
fun testBuildPrompt_AutoDetect() {
    val client = OpenAICompatibleClient(settings)
    val prompt = client.buildSummaryPrompt(SummaryLanguage.AUTO_DETECT)
    assertThat(prompt).contains("Lang: ")
    assertThat(prompt).contains("detected language code")
}

@Test
fun testBuildPrompt_Spanish() {
    val client = OpenAICompatibleClient(settings)
    val prompt = client.buildSummaryPrompt(SummaryLanguage.SPANISH)
    assertThat(prompt).contains("Spanish")
}

@Test
fun testBuildPrompt_AllLanguages() {
    SummaryLanguage.entries.forEach { language ->
        val prompt = client.buildSummaryPrompt(language)
        assertThat(prompt).isNotEmpty()
        if (language != SummaryLanguage.AUTO_DETECT) {
            assertThat(prompt).contains(language.languageName)
        }
    }
}
```

**Test Suite 2: Settings Persistence**

**File:** `SettingsStoreTest.kt`

**Test Cases:**
```kotlin
@Test
fun testSetSummaryLanguage_Persists() {
    settingsStore.setSummaryLanguage(SummaryLanguage.SPANISH)
    verify(sp).edit().putString("pref_summary_language", "es")
}

@Test
fun testSummaryLanguage_LoadsFromStorage() {
    whenever(sp.getString("pref_summary_language", null))
        .thenReturn("es")
    val language = settingsStore.summaryLanguage.value
    assertThat(language).isEqualTo(SummaryLanguage.SPANISH)
}
```

**Test Suite 3: ViewModel Integration**

**File:** `SettingsViewModelTest.kt`

**Test Cases:**
```kotlin
@Test
fun testOnEvent_SetSummaryLanguage() {
    viewModel.onEvent(AISettingsEvent.SetSummaryLanguage(SummaryLanguage.FRENCH))
    verify(settingsStore).setSummaryLanguage(SummaryLanguage.FRENCH)
}
```

### 7.2 Integration Tests

**Test Scenario 1: End-to-End Language Flow**

```kotlin
@Test
fun testLanguageSettingToEndOfSummary() = runTest {
    // Set language to Spanish
    settingsStore.setSummaryLanguage(SummaryLanguage.SPANISH)

    // Generate summary
    val result = aiApi.summarize(testArticle)

    // Verify correct language was used
    verify(mockClient).generateSummary(testArticle, SummaryLanguage.SPANISH)
}
```

**Test Scenario 2: UI to Settings Flow**

```kotlin
@Test
fun testLanguageSelectorUpdatesSettings() {
    // Compose test rule
    composeTestRule.setContent {
        SummaryLanguageSelector(
            currentLanguage = SummaryLanguage.AUTO_DETECT,
            onLanguageChange = { /* track call */ }
        )
    }

    // Tap dropdown
    composeTestRule.onNodeWithText("Summary Language").performClick()

    // Tap "Spanish"
    composeTestRule.onNodeWithText("Spanish").performClick()

    // Verify callback
    verify(callback).invoke(SummaryLanguage.SPANISH)
}
```

### 7.3 Manual QA Tests

**QA-1: Language Selection**
- [ ] Open AI Settings
- [ ] Verify "Summary Language" field visible
- [ ] Tap dropdown, verify 12 options shown
- [ ] Select "Spanish"
- [ ] Verify field shows "Spanish"
- [ ] Close and reopen settings
- [ ] Verify "Spanish" still selected

**QA-2: Summary Generation**
- [ ] Set language to "English"
- [ ] Generate summary for Spanish article
- [ ] Verify summary is in English
- [ ] Set language to "Auto-detect"
- [ ] Generate summary for Spanish article
- [ ] Verify summary is in Spanish
- [ ] Verify "Lang: es" prefix in response

**QA-3: Provider Compatibility**
- [ ] Test with OpenAI provider
- [ ] Test with Anthropic provider
- [ ] Verify language setting preserved when switching providers

**QA-4: Edge Cases**
- [ ] Rapid language changes
- [ ] Screen rotation
- [ ] Configuration changes
- [ ] App backgrounding/foregrounding

---

## 8. Acceptance Criteria

### AC-1: Language Selection UI

**Given:** User opens AI Settings
**When:** User views Summary Language field
**Then:** Dropdown displays "Auto-detect" (or selected language)
**And:** Tapping dropdown shows all 12 language options
**And:** Current selection has checkmark

### AC-2: Language Persistence

**Given:** User selects "Spanish" in language dropdown
**When:** User saves settings and closes screen
**And:** User reopens AI Settings
**Then:** Language field shows "Spanish"
**And:** SharedPreferences contains "pref_summary_language" = "es"

### AC-3: Specific Language Summary

**Given:** User has selected "French" for summary language
**When:** User generates a summary for an article
**Then:** AI prompt includes "Summarize in French"
**And:** Summary is generated in French

### AC-4: Auto-Detect Summary

**Given:** User has selected "Auto-detect" for summary language
**When:** User generates a summary for an English article
**Then:** AI prompt includes language detection instruction
**And:** Summary response includes "Lang: en" prefix
**And:** Summary is in English

### AC-5: Backward Compatibility

**Given:** Existing user with no language preference set
**When:** User updates app
**Then:** Default is "Auto-detect" (matches current behavior)
**And:** No migration issues occur

---

## 9. Deployment Plan

### 9.1 Release Strategy

**Type:** Feature Release (included in regular app update)

**Backward Compatibility:** Yes (default is AUTO_DETECT)

**Data Migration:** Not required (empty value defaults correctly)

### 9.2 Rollout Plan

1. **Alpha Release:** Internal testing
2. **Beta Release:** Small group of users
3. **Stable Release:** All users

**Risk Level:** Low (non-breaking change)

### 9.3 Rollback Plan

If issues occur:
- Language setting ignored (treat all as AUTO_DETECT)
- UI can hide language selector temporarily
- No data loss (setting persists)

---

## 10. Maintenance Considerations

### 10.1 Future Enhancements

**Potential Improvements:**
1. Add more languages (add enum entries + strings)
2. Per-feed language settings
3. Language confidence display
4. Language validation with warnings

**None are blocking for initial release**

### 10.2 Known Limitations

1. **Language Detection Accuracy:** Depends on AI model capabilities
2. **No Language Validation:** We trust AI to follow language instruction
3. **Fixed Language List:** Not user-extensible (requires app update)

### 10.3 Monitoring

**Metrics to Track:**
- Language selection distribution (analytics)
- Summary generation success rate per language
- User feedback on language accuracy

**No changes to current analytics needed**

---

## 11. Security Considerations

### 11.1 Threat Model

**No Security Risks:**

- Language setting is local-only
- Not transmitted to servers (only in prompt)
- No user input validation needed (enum)
- No injection attacks possible

### 11.2 Privacy

**Data Stored:** Language preference (ISO 639-1 code)
**Data Transmitted:** None (only in prompts)
**Third Parties:** AI providers receive language in prompts (existing flow)

**No Additional Privacy Concerns**

---

## 12. Compliance

### 12.1 Accessibility

**WCAG 2.1 Compliance:**
- ✅ Level AA: Color contrast ratios
- ✅ Level AA: Touch targets (48dp minimum)
- ✅ Level AA: Screen reader support
- ✅ Level AA: Keyboard navigation

### 12.2 Localization

**String Resources:**
- All language names localizable
- Follows existing translation patterns
- Default (English) provided

**RTL Support:**
- Compose handles RTL automatically
- No special handling needed

---

## 13. Appendix

### 13.1 Terminology

| Term | Definition |
|------|------------|
| Auto-detect | AI detects article language and summarizes in same language |
| Language Code | ISO 639-1 code (e.g., "en", "es", "zh") |
| Language Name | Full language name for prompts (e.g., "English", "Spanish") |
| StateFlow | Kotlin coroutine-based state stream |

### 13.2 References

- [Android Developer Guide - Settings](https://developer.android.com/guide/topics/ui/settings)
- [Material 3 - Dropdown Menus](https://m3.material.io/components/menus/guidelines)
- [Kotlin Flow Documentation](https://kotlinlang.org/docs/flow.html)
- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
- [Anthropic Claude Documentation](https://docs.anthropic.com/claude/reference)

### 13.3 Change History

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-01 | 1.0 | Initial specification |

---

## 14. Approval

**Technical Lead:** _________________ Date: _______

**Product Owner:** _________________ Date: _______

**QA Lead:** _________________ Date: _______

---

**Last Updated:** 2026-01-01 09:53:49
**Next Phase:** Phase 7 (Specification Review)
