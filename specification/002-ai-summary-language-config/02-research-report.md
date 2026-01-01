# Research Report: AI Summary Language Configuration

**Created:** 2026-01-01 09:53:49
**Status:** Complete

---

## 1. Executive Summary

This report documents research findings for implementing configurable language support for AI-generated summaries in the Feeder RSS reader. The feature will allow users to select from 11 languages or use auto-detect, with the AI generating summaries in the chosen language.

**Key Findings:**
- Codebase already has `SummaryLanguage` enum and `SettingsStore.summaryLanguage` flow
- AI provider architecture is clean and well-structured with factory pattern
- Settings UI follows Material 3 patterns with dropdown menus for selection
- Prompt generation needs enhancement to support language-specific instructions

---

## 2. Existing Code Analysis

### 2.1 SummaryLanguage Enum ✓
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/SummaryLanguage.kt`

**Status:** Already implemented

**Key Features:**
- 12 language options: AUTO_DETECT + 11 specific languages
- Properties: `code` (ISO 639-1), `displayName` (string resource), `languageName` (for prompts)
- Companion object with `fromCode()` parsing method

**Languages Supported:**
```
- AUTO_DETECT (auto-detect with Lang: prefix)
- ENGLISH (en)
- CHINESE (zh)
- SPANISH (es)
- FRENCH (fr)
- GERMAN (de)
- JAPANESE (ja)
- KOREAN (ko)
- PORTUGUESE (pt)
- RUSSIAN (ru)
- ARABIC (ar)
- HINDI (hi)
```

**Assessment:** Well-designed, no changes needed.

### 2.2 SettingsStore Integration ✓
**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Status:** Already implemented

**Current Implementation:**
```kotlin
// Summary language setting
private val _summaryLanguage = MutableStateFlow(
    SummaryLanguage.fromCode(sp.getString(PREF_SUMMARY_LANGUAGE, null)),
)
val summaryLanguage = _summaryLanguage.asStateFlow()

fun setSummaryLanguage(value: SummaryLanguage) {
    _summaryLanguage.value = value
    sp.edit().putString(PREF_SUMMARY_LANGUAGE, value.code).apply()
}
```

**Assessment:** Complete implementation, follows existing patterns perfectly.

### 2.3 AI Provider Architecture
**Files:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` (interface)
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Current Architecture:**
```
AIApi (high-level API)
  └─> AIClient.create(settings) (factory)
       ├─> OpenAICompatibleClient
       └─> AnthropicClient
```

**Current Prompt Generation (OpenAICompatibleClient):**
```kotlin
val systemPrompt = """You are a helpful assistant that summarizes news articles.
Start your response with "Lang: " followed by the detected language code,
then provide a concise summary of the article in that same language.

For example:
Lang: en
This article discusses...

Now summarize this article:"""
```

**Gap:** Prompt doesn't accept language parameter, always uses auto-detect.

**Assessment:** Clean architecture, factory pattern in place. Need to:
1. Add `language: SummaryLanguage` parameter to `AIClient.generateSummary()`
2. Update both provider implementations
3. Modify prompt generation based on language setting

### 2.4 Settings UI Patterns
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Dropdown Pattern Used:**
```kotlin
// ExposedDropdownMenuBox with DropdownMenu
var providerMenuExpanded by remember { mutableStateOf(false) }

Box {
    TextField(
        value = when (current.providerType) { ... },
        onValueChange = {},
        label = { Text(stringResource(R.string.ai_provider)) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { providerMenuExpanded = !providerMenuExpanded }) {
                Icon(Icons.Filled.ExpandMore, ...)
            }
        },
    )

    DropdownMenu(
        expanded = providerMenuExpanded,
        onDismissRequest = { providerMenuExpanded = false },
    ) {
        AIProvider.entries.forEach { provider ->
            DropdownMenuItem(
                text = { Text(...) },
                onClick = { /* update setting */ },
            )
        }
    }
}
```

**Assessment:** Well-established pattern. Can reuse for language selection dropdown.

---

## 3. Best Practices Research

### 3.1 Language-Specific Prompt Engineering

**Research Sources:**
- OpenAI documentation on language-specific prompts
- Anthropic Claude prompt engineering guide
- Industry best practices for multilingual AI applications

**Key Findings:**

1. **Explicit Language Instructions Work Best**
   - ✅ "Summarize in Spanish" (explicit)
   - ❌ "Spanish summary" (ambiguous)

2. **Language Should Come First in Prompt**
   ```
   Good: "You are a helpful assistant that summarizes news articles in Spanish.
   Bad: "Summarize this article. The summary should be in Spanish."
   ```

3. **Auto-Detect Requires Clear Output Format**
   ```
   Start your response with "Lang: " followed by the detected language code.
   For example: "Lang: en"
   Then provide the summary.
   ```

4. **Language Name vs. Language Code**
   - Use language names in prompts (English, Spanish, Chinese)
   - Use language codes for data storage/processing (en, es, zh)

**Recommended Prompt Templates:**

**For Specific Language (e.g., Spanish):**
```
You are a helpful assistant that summarizes news articles in Spanish.

Provide a concise summary of the following article in Spanish:

[article content]
```

**For Auto-Detect:**
```
You are a helpful assistant that summarizes news articles.
Detect the article's language and summarize in that same language.

Start your response with "Lang: " followed by the detected language code.
For example: "Lang: en"

Then provide a concise summary of the article:

[article content]
```

### 3.2 UI/UX Best Practices for Language Selection

**Research Findings:**

1. **Dropdown vs. Radio Buttons**
   - Dropdown: Better for 5+ options, saves space ✓ (our case: 12 options)
   - Radio: Better for 2-4 options, all options visible

2. **Display Format**
   ```
   Option 1: "English"
   Option 2: "English (en)" ← BETTER (shows code for clarity)
   ```

3. **Native Language Names**
   - Show language names in user's app language (not native script)
   - Example: For Chinese app user, show "英语" instead of "English"

4. **Current Selection Indicator**
   - Checkmark or bold text for selected item
   - Clear visual feedback

**Recommendation:** Use DropdownMenu with ExposedDropdownMenuBox pattern (matching existing UI).

### 3.3 String Resource Patterns

**Current Pattern in Codebase:**
```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="setting_name">Display Name</string>
<string name="setting_name_description">Description text</string>
```

**Required Resources for Feature:**
```xml
<!-- Section -->
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

---

## 4. Technical Implementation Strategy

### 4.1 Recommended Approach: Modify AIClient Interface

**Why This Approach:**
1. ✅ Clean separation of concerns
2. ✅ Type-safe language passing
3. ✅ Consistent with existing architecture
4. ✅ Each provider has full control over prompt generation

**Implementation Steps:**

**Step 1: Update AIClient Interface**
```kotlin
interface AIClient {
    suspend fun generateSummary(
        content: String,
        language: SummaryLanguage  // NEW PARAMETER
    ): SummaryResult
}
```

**Step 2: Update OpenAICompatibleClient**
```kotlin
override suspend fun generateSummary(
    content: String,
    language: SummaryLanguage
): AIClient.SummaryResult {
    val prompt = when (language) {
        SummaryLanguage.AUTO_DETECT -> buildAutoDetectPrompt()
        else -> buildLanguageSpecificPrompt(language.languageName)
    }

    val userMessage = "$prompt\n$content"
    // ... rest of implementation
}
```

**Step 3: Update AnthropicClient** (similar pattern)

**Step 4: Update AIApi**
```kotlin
suspend fun summarize(content: String): AIClient.SummaryResult {
    val language = repository.summaryLanguage.first()  // Get from settings
    return client.generateSummary(content, language)
}
```

### 4.2 Alternative Approaches Considered

**Option 2: Pass Language via AISettings**
❌ Rejected - Language is per-request, not per-config

**Option 3: Global Language State**
❌ Rejected - Breaks encapsulation, harder to test

**Option 4: Prompt Template Injection**
❌ Rejected - Less type-safe, harder to maintain

---

## 5. Integration Points

### 5.1 Settings Screen Integration

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt`

**Where to Add:**
- After the timeout field (line ~430 in AIProviderSectionEdit)
- Before Azure fields section (if any)

**Code Structure:**
```kotlin
@Composable
fun AIProviderSectionEdit(...) {
    Column(...) {
        // ... existing fields ...

        // Summary Language Selector (NEW)
        var languageMenuExpanded by remember { mutableStateOf(false) }
        val currentLanguage by repository.summaryLanguage.collectAsState()

        ExposedDropdownMenuBox(...) {
            TextField(
                value = stringResource(currentLanguage.displayName),
                onValueChange = {},
                label = { Text(stringResource(R.string.summary_language_title)) },
                readOnly = true,
                trailingIcon = { /* dropdown icon */ },
            )

            DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
            ) {
                SummaryLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(stringResource(language.displayName)) },
                        onClick = {
                            onEvent(AISettingsEvent.SetSummaryLanguage(language))
                            languageMenuExpanded = false
                        },
                    )
                }
            }
        }

        // ... rest of fields ...
    }
}
```

### 5.2 ViewModel Integration

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Need to Add:**
```kotlin
// Expose summary language from SettingsStore
val summaryLanguage: StateFlow<SummaryLanguage> =
    settingsStore.summaryLanguage

// Setter
fun setSummaryLanguage(language: SummaryLanguage) {
    settingsStore.setSummaryLanguage(language)
}
```

### 5.3 Event Handling

**Update AISettingsEvent:**
```kotlin
sealed interface AISettingsEvent {
    // ... existing events ...

    data class SetSummaryLanguage(
        val language: SummaryLanguage
    ) : AISettingsEvent
}
```

---

## 6. Testing Strategy

### 6.1 Unit Tests

**Test Files to Create:**
- `SummaryLanguagePromptTest.kt` - Test prompt generation for each language
- `AIApiLanguageTest.kt` - Test AIApi passes language correctly
- `SettingsViewModelLanguageTest.kt` - Test ViewModel language handling

**Test Cases:**
1. Each language generates correct prompt
2. Auto-detect prompt includes Lang: prefix instruction
3. Language setting persists to SharedPreferences
4. Language setting loads correctly on app start
5. ViewModel correctly delegates to SettingsStore

### 6.2 Integration Tests

**Test Scenarios:**
1. Select language in UI → Verify setting saved
2. Restart app → Verify language restored
3. Generate summary with specific language → Verify correct prompt sent
4. Generate summary with auto-detect → Verify Lang: prefix handling
5. Switch between providers → Verify language setting preserved

### 6.3 Manual QA Checklist

- [ ] Open AI settings
- [ ] See "Summary Language" dropdown
- [ ] Select "Spanish"
- [ ] Generate summary → Verify Spanish response
- [ ] Switch to "Auto-detect"
- [ ] Generate summary → Verify Lang: [code] prefix
- [ ] Close and reopen app → Verify selection preserved
- [ ] Test with OpenAI provider
- [ ] Test with Anthropic provider

---

## 7. Risks and Mitigations

### 7.1 Risk: AI Ignores Language Instruction
**Probability:** Low
**Impact:** Medium
**Mitigation:**
- Test with multiple prompts per language
- Refine prompts based on testing results
- Consider more explicit instructions if needed

### 7.2 Risk: UI Space Constraints
**Probability:** Low
**Impact:** Low
**Mitigation:**
- Use dropdown to save space
- Follow existing patterns (provider dropdown works well)

### 7.3 Risk: String Resource Translation
**Probability:** Medium
**Impact:** Low
**Mitigation:**
- Start with English strings
- Add translations incrementally
- Language names are standard (can use existing translations)

### 7.4 Risk: Backward Compatibility
**Probability:** Low
**Impact:** Low
**Mitigation:**
- Default to AUTO_DETECT (existing behavior)
- No migration needed (empty SharedPreferences → AUTO_DETECT)

---

## 8. Implementation Effort Estimate

| Task | Estimated Time |
|------|---------------|
| Add string resources | 30 minutes |
| Create prompt generation logic | 1 hour |
| Update AIClient interface | 30 minutes |
| Update OpenAICompatibleClient | 1 hour |
| Update AnthropicClient | 1 hour |
| Update AIApi integration | 30 minutes |
| Create language selector UI | 2 hours |
| Update ViewModel | 30 minutes |
| Write unit tests | 2 hours |
| Manual testing and refinement | 2 hours |
| **Total** | **~11 hours** |

---

## 9. Success Criteria

### 9.1 Functional
- [ ] User can select from 12 language options
- [ ] Selection persists across app restarts
- [ ] Summaries generated in selected language
- [ ] Auto-detect works with Lang: prefix

### 9.2 Quality
- [ ] No crashes or errors
- [ ] UI follows existing patterns
- [ ] Code follows project conventions
- [ ] All tests pass

### 9.3 Performance
- [ ] No perceptible delay added to summary generation
- [ ] UI loads instantly

---

## 10. Recommendations

### 10.1 Implementation Priority
1. **HIGH:** Prompt generation update (core functionality)
2. **HIGH:** AIClient interface update (required for prompt change)
3. **MEDIUM:** String resources (required for UI)
4. **MEDIUM:** Settings UI component (user-facing feature)
5. **LOW:** String translations (can be incremental)

### 10.2 Future Enhancements
1. Add language validation (detect if AI used wrong language)
2. Show confidence score for auto-detect
3. Add more languages (currently 11, could expand)
4. Per-feed language settings (advanced use case)

---

## 11. Conclusion

The research confirms that:
- ✅ All foundation code is in place
- ✅ Architecture supports the feature cleanly
- ✅ Implementation is straightforward
- ✅ No major blockers or risks

**Recommended Path Forward:**
Proceed to Phase 5 (Code Assessment) to verify current implementation details, then Phase 6 (Specification Writing) to create detailed implementation plan.

---

**Last Updated:** 2026-01-01 09:53:49
**Next Phase:** Phase 4 (Debug Analysis - SKIP) → Phase 5 (Code Assessment)
