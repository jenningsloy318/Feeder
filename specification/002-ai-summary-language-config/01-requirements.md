# Requirements Document: AI Summary Language Configuration

**Feature:** AI Summary Language Configuration
**Created:** 2026-01-01 09:53:49
**Status:** Draft

---

## 1. Overview

### 1.1 Objective
Enable users to configure the language used for AI-generated summaries of news articles in the Feeder RSS reader application.

### 1.2 Context
The Feeder app already has:
- Multi-provider AI support (OpenAI, Anthropic, Azure OpenAI)
- `SummaryLanguage` enum with 11 languages + auto-detect
- `SettingsStore.summaryLanguage` flow and setter already implemented
- Existing prompt generation that uses auto-detect

**What's Missing:**
- String resources for language display names
- UI component for language selection in settings
- Integration of language parameter into prompt generation
- Connection from settings to AI providers

---

## 2. Functional Requirements

### 2.1 Language Selection
**FR-1:** The app MUST support the following languages for summaries:
- Auto-detect (AI detects article language and summarizes in that language)
- English (en)
- Chinese (zh)
- Spanish (es)
- French (fr)
- German (de)
- Japanese (ja)
- Korean (ko)
- Portuguese (pt)
- Russian (ru)
- Arabic (ar)
- Hindi (hi)

**FR-2:** The default language setting MUST be "Auto-detect".

**FR-3:** The user MUST be able to change the summary language through the settings screen.

### 2.2 Prompt Engineering
**FR-4:** When a specific language is selected, the AI prompt MUST instruct the model to summarize in that language.

**FR-5:** When "Auto-detect" is selected, the AI prompt MUST instruct the model to:
1. Detect the article's language
2. Summarize in that same language
3. Prefix the response with "Lang: [code]" (e.g., "Lang: en")

**FR-6:** The prompt MUST include the target language in a clear, natural-language instruction that the AI can understand.

### 2.3 Storage
**FR-7:** The selected language MUST be persisted in SharedPreferences using the key `pref_summary_language`.

**FR-8:** The language setting MUST survive app restarts and device reboots.

### 2.4 Integration
**FR-9:** The `AIApi` class MUST pass the selected language to AI providers.

**FR-10:** Both OpenAI-compatible and Anthropic providers MUST respect the language setting.

---

## 3. Non-Functional Requirements

### 3.1 Performance
**NFR-1:** Language selection MUST NOT add perceptible delay to summary generation (< 100ms overhead).

**NFR-2:** Settings UI MUST load instantly with no lag when opening the language selector.

### 3.2 Usability
**NFR-3:** Language names MUST be displayed in the user's current app language (localized).

**NFR-4:** The language selector MUST be easily accessible from AI settings.

**NFR-5:** The current selection MUST be clearly indicated in the UI.

### 3.3 Compatibility
**NFR-6:** The feature MUST work with all existing AI providers (OpenAI, Anthropic, Azure).

**NFR-7:** The feature MUST be backward compatible (existing users default to auto-detect).

### 3.4 Code Quality
**NFR-8:** Code MUST follow existing project patterns (SettingsStore, Compose UI, etc.).

**NFR-9:** Changes MUST minimize modifications to existing provider code.

---

## 4. UI/UX Requirements

### 4.1 Settings Screen
**UIR-1:** A new section "Summary Language" MUST be added to the AI settings screen.

**UIR-2:** The language selector MUST use a dropdown/combo box or radio group.

**UIR-3:** Each language option MUST show:
- Display name (localized)
- Optionally, the language code in parentheses for clarity

### 4.2 Display Names (Required String Resources)
**UIR-4:** The following string resources MUST be created in `app/src/main/res/values/strings.xml`:

```xml
<!-- Summary language settings -->
<string name="summary_language_title">Summary Language</string>
<string name="summary_language_description">Choose the language for AI-generated summaries</string>

<!-- Language options -->
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

---

## 5. Acceptance Criteria

### AC-1: Language Selection
- [ ] User can open AI settings
- [ ] User can see "Summary Language" option
- [ ] User can select from 12 language options (11 languages + auto-detect)
- [ ] Selected language is persisted and restored on app restart

### AC-2: Prompt Generation
- [ ] When specific language selected, summary is generated in that language
- [ ] When auto-detect selected, AI detects language and summarizes in same language
- [ ] Auto-detect response includes "Lang: [code]" prefix

### AC-3: Integration
- [ ] Both OpenAI and Anthropic providers respect language setting
- [ ] No errors or crashes when changing language setting
- [ ] Summary generation works seamlessly with language parameter

### AC-4: Testing
- [ ] All unit tests pass
- [ ] Integration tests cover prompt generation with language parameter
- [ ] Manual testing confirms summaries are generated in correct language

---

## 6. Technical Considerations

### 6.1 Current State
**Already Implemented:**
- `SummaryLanguage` enum in `app/src/main/java/com/nononsenseapps/feeder/ai/model/SummaryLanguage.kt`
- `SettingsStore.summaryLanguage: StateFlow<SummaryLanguage>`
- `SettingsStore.setSummaryLanguage(value: SummaryLanguage)`
- Preference constant: `PREF_SUMMARY_LANGUAGE = "pref_summary_language"`

**Needs Implementation:**
- String resources for display names
- Settings UI component
- Prompt generation update to use `languageName` property
- Wiring language from `AIApi` to `AIClient` providers

### 6.2 Implementation Approach

#### Option 1: Modify AIClient Interface (Recommended)
**Pros:**
- Clean separation of concerns
- Type-safe language passing
- Consistent with existing architecture

**Cons:**
- Requires updating both provider implementations
- More code changes

**Implementation:**
1. Add `language: SummaryLanguage` parameter to `AIClient.generateSummary()`
2. Update `OpenAICompatibleClient.generateSummary()` to use language
3. Update `AnthropicClient.generateSummary()` to use language
4. Update `AIApi.summarize()` to pass language from settings

#### Option 2: Pass Language via AISettings
**Pros:**
- Fewer interface changes
- Language grouped with other settings

**Cons:**
- Less clean separation (language is per-request, not per-config)
- Requires modifying `AISettings` data classes

**Decision:** Use **Option 1** - it's cleaner and more flexible.

### 6.3 Prompt Template
For specific language (e.g., Spanish):
```
You are a helpful assistant that summarizes news articles in Spanish.

Provide a concise summary of the following article in Spanish:

[article content]
```

For auto-detect:
```
You are a helpful assistant that summarizes news articles.
Detect the article's language and summarize in that same language.

Start your response with "Lang: " followed by the detected language code.
For example: "Lang: en"

Then provide a concise summary of the article:

[article content]
```

---

## 7. Open Questions

### OQ-1: Language Detection Confidence
**Question:** How should the app handle low-confidence language detection in auto-detect mode?
**Options:**
1. Always accept AI's detected language
2. Show warning if detection confidence is low
3. Fallback to app language if detection fails

**Recommendation:** Option 1 (simplest, relies on AI capability)

### OQ-2: Mixed-Language Articles
**Question:** How should auto-detect handle articles with multiple languages?
**Options:**
1. Use first paragraph's language
2. Use majority language
3. Let AI decide best approach

**Recommendation:** Option 3 (trust AI's judgment)

### OQ-3: Language Validation
**Question:** Should we validate that the AI actually used the requested language?
**Options:**
1. Trust AI, no validation
2. Parse response to verify language matches request
3. Show warning if detected language differs from request

**Recommendation:** Option 1 (start simple), add Option 3 later if needed

---

## 8. Dependencies

### 8.1 Code Dependencies
- `SummaryLanguage.kt` (already exists)
- `SettingsStore.kt` (already has summaryLanguage flow)
- `AIApi.kt` (needs modification)
- `OpenAICompatibleClient.kt` (needs modification)
- `AnthropicClient.kt` (needs modification)
- Settings UI Composables (needs new component)

### 8.2 External Dependencies
- OpenAI Java SDK (already integrated)
- Anthropic SDK (already integrated)
- AndroidX Compose UI (already integrated)

---

## 9. Risks and Mitigations

### Risk 1: Prompt Engineering Complexity
**Risk:** AI models may not consistently follow language instructions
**Impact:** Medium
**Mitigation:** Test with multiple models and languages, refine prompts

### Risk 2: UI Space Constraints
**Risk:** Settings screen may not have space for language selector
**Impact:** Low
**Mitigation:** Use dropdown menu to save space

### Risk 3: String Resource Translation
**Risk:** Language names may not be translated into all app languages
**Impact:** Low (UI still functional, just not fully localized)
**Mitigation:** Start with English, add translations incrementally

---

## 10. Success Metrics

### 10.1 Functional Metrics
- [ ] 100% of supported languages generate correct summaries
- [ ] 0 crashes when changing language setting
- [ ] < 500ms additional latency for language-specific summaries

### 10.2 Quality Metrics
- [ ] User can select language in < 3 clicks
- [ ] Setting is immediately effective (no app restart required)
- [ ] Language selection persists across app updates

---

**Last Updated:** 2026-01-01 09:53:49
**Next Step:** Proceed to Phase 3 (Research)
