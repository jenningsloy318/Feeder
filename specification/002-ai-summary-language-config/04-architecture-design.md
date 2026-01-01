# Architecture Design: AI Summary Language Configuration

**Created:** 2026-01-01 09:53:49
**Status:** Complete

---

## 1. Architectural Overview

This document describes the architectural changes and data flow for implementing language configuration in AI-generated summaries.

### 1.1 Design Philosophy

- **Minimal Changes:** Modify only necessary components
- **Type Safety:** Leverage Kotlin enum and sealed interfaces
- **Separation of Concerns:** UI, business logic, and data layers remain distinct
- **Backward Compatibility:** Default behavior unchanged (AUTO_DETECT)

---

## 2. Component Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         AIProviderSection (Compose UI)               │  │
│  │  - Language Selector Dropdown                        │  │
│  │  - Displays current language                         │  │
│  │  - Emits SetSummaryLanguage events                   │  │
│  └──────────────────────┬───────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ AISettingsEvent.SetSummaryLanguage
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            SettingsViewModel                          │  │
│  │  - Handles UI events                                 │  │
│  │  - Delegates to SettingsStore                        │  │
│  └──────────────────────┬───────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ setSummaryLanguage(language)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                      Business Logic Layer                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              SettingsStore                            │  │
│  │  - Manages StateFlow<SummaryLanguage>                │  │
│  │  - Persists to SharedPreferences                     │  │
│  └──────────────────────┬───────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ summaryLanguage: StateFlow
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                       Repository Layer                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               Repository                               │  │
│  │  - Exposes summaryLanguage: StateFlow                │  │
│  │  - Provides centralized settings access              │  │
│  └──────────────────────┬───────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ repository.summaryLanguage.first()
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                      AI Service Layer                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    AIApi                              │  │
│  │  - High-level API for AI operations                  │  │
│  │  - Retrieves language from repository                │  │
│  │  - Passes language to client                         │  │
│  └──────────────────────┬───────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │ generateSummary(content, language)
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    AI Provider Layer                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                AIClient (Interface)                   │  │
│  │  - Factory pattern: create(settings)                 │  │
│  │  - Interface: generateSummary(content, language)     │  │
│  └───────────────────┬──────────────────────────────────┘  │
│                      │ implements                           │
│         ┌────────────┴────────────┐                         │
│         ▼                         ▼                         │
│  ┌─────────────┐          ┌─────────────┐                  │
│  │   OpenAI    │          │ Anthropic   │                  │
│  │ Compatible  │          │   Client    │                  │
│  │  Client     │          │             │                  │
│  └──────┬──────┘          └──────┬──────┘                  │
│         │                        │                          │
│         │ buildPrompt(language)  │                          │
│         └────────────┬───────────┘                          │
└──────────────────────┼──────────────────────────────────────┘
                       │
                       │ Prompt with language instruction
                       ▼
              ┌─────────────────┐
              │   AI API        │
              │  (OpenAI or     │
              │  Anthropic)     │
              └─────────────────┘
```

### 2.2 Data Flow Diagrams

**Flow 1: User Changes Language Setting**

```
User Action
    │
    ├─> Tap language dropdown in settings
    │       │
    │       └─> DropdownMenu shows 12 language options
    │
    ├─> Select "Spanish"
    │       │
    │       └─> DropdownMenuItem onClick
    │               │
    │               ├─> onEvent(AISettingsEvent.SetSummaryLanguage(SPANISH))
    │               │
    │               └─> Menu closes
    │
    └─> SettingsViewModel handles event
            │
            ├─> settingsStore.setSummaryLanguage(SPANISH)
            │       │
            │       ├─> _summaryLanguage.value = SPANISH
            │       │
            │       └─> sp.edit().putString("pref_summary_language", "es")
            │
            └─> StateFlow updates
                    │
                    ├─> UI observes change → TextField updates to "Spanish"
                    │
                    └─> Next summary will use SPANISH
```

**Flow 2: Generate Summary with Language**

```
User requests summary
    │
    └─> AIApi.summarize(articleContent)
            │
            ├─> val language = repository.summaryLanguage.first()
            │       │
            │       └─> Returns SPANISH (from example)
            │
            ├─> val client = AIClient.create(settings)
            │       │
            │       └─> Returns OpenAICompatibleClient or AnthropicClient
            │
            └─> client.generateSummary(content, SPANISH)
                    │
                    ├─> Build prompt with language instruction
                    │       │
                    │       ├─> if SPANISH: "Summarize in Spanish"
                    │       │
                    │       └─> if AUTO_DETECT: "Detect language, summarize in same"
                    │
                    ├─> Call AI API with prompt
                    │
                    ├─> Parse response
                    │
                    └─> Return SummaryResult(content, detectedLanguage)
```

---

## 3. Component Specifications

### 3.1 UI Components

**AIProviderSection.kt**

**New Composable Function:**
```kotlin
@Composable
private fun SummaryLanguageSelector(
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
            value = stringResource(currentLanguage.displayName),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.summary_language_title)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
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

**Integration Point:**
- Add to `AIProviderSectionEdit()` after timeout field
- Before Azure-specific fields (if any)

### 3.2 ViewModel Updates

**SettingsViewModel.kt**

**Add:**
```kotlin
// Expose summary language from SettingsStore
val summaryLanguage: StateFlow<SummaryLanguage> =
    settingsStore.summaryLanguage

// In onEvent function
when (event) {
    is AISettingsEvent.SetSummaryLanguage -> {
        settingsStore.setSummaryLanguage(event.language)
    }
    // ... other events
}
```

### 3.3 Event Type Definition

**AISettingsEvent Sealed Interface:**

**Add:**
```kotlin
sealed interface AISettingsEvent {
    // ... existing events ...

    data class SetSummaryLanguage(
        val language: SummaryLanguage
    ) : AISettingsEvent
}
```

### 3.4 AI Provider Updates

**AIClient Interface:**

**Change:**
```kotlin
interface AIClient {
    suspend fun generateSummary(
        content: String,
        language: SummaryLanguage = SummaryLanguage.AUTO_DETECT
    ): SummaryResult
}
```

**OpenAICompatibleClient Implementation:**

**Add prompt generation method:**
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

override suspend fun generateSummary(
    content: String,
    language: SummaryLanguage
): AIClient.SummaryResult {
    val systemPrompt = buildSummaryPrompt(language)
    val userMessage = "$systemPrompt\n\n$content"
    // ... rest of implementation
}
```

**AnthropicClient Implementation:**

**Similar pattern to OpenAICompatibleClient**

**Key Difference:**
- May need different prompt format for Claude
- Test and adjust based on Anthropic API behavior

### 3.5 AIApi Integration

**AIApi.kt**

**Change:**
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

---

## 4. Data Models

### 4.1 SummaryLanguage Enum

**Already Exists:**
```kotlin
enum class SummaryLanguage(
    val code: String,
    @StringRes val displayName: Int,
    val languageName: String,
) {
    AUTO_DETECT("", R.string.summary_language_auto_detect, "the article's original"),
    ENGLISH("en", R.string.summary_language_english, "English"),
    CHINESE("zh", R.string.summary_language_chinese, "Chinese"),
    SPANISH("es", R.string.summary_language_spanish, "Spanish"),
    FRENCH("fr", R.string.summary_language_french, "French"),
    GERMAN("de", R.string.summary_language_german, "German"),
    JAPANESE("ja", R.string.summary_language_japanese, "Japanese"),
    KOREAN("ko", R.string.summary_language_korean, "Korean"),
    PORTUGUESE("pt", R.string.summary_language_portuguese, "Portuguese"),
    RUSSIAN("ru", R.string.summary_language_russian, "Russian"),
    ARABIC("ar", R.string.summary_language_arabic, "Arabic"),
    HINDI("hi", R.string.summary_language_hindi, "Hindi"),

    companion object {
        fun fromCode(code: String?): SummaryLanguage =
            entries.firstOrNull { it.code == code } ?: AUTO_DETECT
    }
}
```

**No Changes Required**

---

## 5. State Management

### 5.1 Settings Storage

**SharedPreferences Key:**
```
pref_summary_language
```

**Value Format:**
```
ISO 639-1 language code (empty string for AUTO_DETECT)
Examples: "", "en", "es", "zh", "ja"
```

**StateFlow:**
```kotlin
private val _summaryLanguage = MutableStateFlow(
    SummaryLanguage.fromCode(sp.getString(PREF_SUMMARY_LANGUAGE, null))
)
val summaryLanguage = _summaryLanguage.asStateFlow()
```

### 5.2 UI State Flow

**Collection Pattern:**
```kotlin
// In AIProviderSectionEdit
val currentLanguage by repository.summaryLanguage.collectAsStateWithLifecycle()
```

**Update Pattern:**
```kotlin
// When user selects new language
onLanguageChange(newLanguage)
    ↓
SettingsViewModel.onEvent(SetSummaryLanguage(newLanguage))
    ↓
SettingsStore.setSummaryLanguage(newLanguage)
    ↓
_summaryLanguage.value = newLanguage
    ↓
SharedPreferences updated
    ↓
StateFlow emits new value
    ↓
UI recomposes with new selection
```

---

## 6. Error Handling

### 6.1 Missing String Resources

**Detection:** Build-time error (R.string.* not found)

**Handling:**
- Create all required strings in implementation
- Use `@StringRes` annotation for compile-time checking

### 6.2 Invalid Language Code in Storage

**Detection:** Runtime when reading from SharedPreferences

**Handling:**
```kotlin
// In SummaryLanguage companion object
fun fromCode(code: String?): SummaryLanguage =
    entries.firstOrNull { it.code == code } ?: AUTO_DETECT
                    // ↑ Default to AUTO_DETECT if code invalid
```

### 6.3 AI Language Non-Compliance

**Detection:** Summary returns in different language than requested

**Handling:**
- Initial version: Trust AI, no validation
- Future enhancement: Parse response and show warning if mismatch

### 6.4 Network Errors

**Existing:** Already handled in current implementation

**No Changes Required**

---

## 7. Performance Considerations

### 7.1 Computational Complexity

| Operation | Complexity | Notes |
|-----------|------------|-------|
| Language selection | O(1) | Enum lookup |
| Prompt generation | O(1) | String template |
| State update | O(1) | StateFlow emission |
| SharedPreferences read | O(1) | Single key lookup |
| SharedPreferences write | O(1) | Single key write |

### 7.2 Memory Impact

**Additional Memory:** NEGLIGIBLE

- 1 enum instance (already exists)
- 1 StateFlow (already exists)
- 1 SharedPreferences entry (4 bytes)
- UI components (follow existing patterns)

### 7.3 Network Impact

**No Additional Network Calls**

Language setting is sent as part of existing summary generation request.

---

## 8. Security Considerations

### 8.1 Input Validation

**Language Selection:**
- Type-safe (enum, not arbitrary string)
- No user input beyond enum options
- No injection risk

### 8.2 Data Privacy

**Language Setting:**
- Stored locally on device
- Not transmitted to servers (only in prompt)
- No privacy concerns

---

## 9. Testing Architecture

### 9.1 Unit Test Structure

```
app/src/test/java/com/nononsenseapps/feeder/
├── ai/
│   ├── OpenAICompatibleClientTest.kt
│   │   └── testGenerateSummary_WithLanguage()
│   ├── AnthropicClientTest.kt
│   │   └── testGenerateSummary_WithLanguage()
│   └── AIApiTest.kt
│       └── testSummarize_UsesLanguageFromSettings()
├── archmodel/
│   └── SettingsStoreTest.kt
│       └── testSetSummaryLanguage()
└── ui/
    └── settings/
        └── SettingsViewModelTest.kt
            └── testOnEvent_SetSummaryLanguage()
```

### 9.2 Integration Test Scenarios

1. **Language Selection Flow:**
   - User selects Spanish → Verify saved to SharedPreferences
   - App restart → Verify Spanish restored

2. **Summary Generation Flow:**
   - Settings have English → Verify prompt uses English
   - Settings have AUTO_DETECT → Verify prompt uses auto-detect

3. **Provider-Specific:**
   - OpenAI with Spanish → Verify correct prompt format
   - Anthropic with French → Verify correct prompt format

---

## 10. Migration and Backward Compatibility

### 10.1 Data Migration

**No Migration Required**

- Existing users have no `pref_summary_language` key
- `fromCode(null)` returns `AUTO_DETECT` (default)
- Current behavior is auto-detect (matches default)

### 10.2 API Compatibility

**Backward Compatible**

- New parameter has default value: `language: SummaryLanguage = AUTO_DETECT`
- Existing calls without parameter continue to work
- No breaking changes

---

## 11. Future Extensibility

### 11.1 Adding New Languages

**Process:**
1. Add enum entry to `SummaryLanguage`
2. Add string resource for display name
3. No other changes needed

**Example:**
```kotlin
ITALIAN(
    code = "it",
    displayName = R.string.summary_language_italian,
    languageName = "Italian",
)
```

### 11.2 Per-Feed Language Settings (Future)

**Would Require:**
- Change from single global setting to Feed settings
- Database schema change (Feed entity)
- UI: Each feed has language override option
- Business logic: Check feed-specific setting, fall back to global

**Not in Current Scope**

### 11.3 Language Confidence Display (Future)

**Would Require:**
- Parse AI response for detected language
- Compare with requested language
- UI indicator for confidence level

**Not in Current Scope**

---

## 12. Architecture Decision Records

### ADR-001: Language Parameter in generateSummary()

**Decision:** Add `language` parameter to `AIClient.generateSummary()`

**Context:**
- Need to pass user's language preference to AI providers
- Each provider may generate different prompts

**Alternatives Considered:**
1. Global language state object
2. Language via AISettings
3. Parameter in generateSummary() ← **SELECTED**

**Rationale:**
- Clean separation of concerns
- Type-safe
- Each provider has full control
- Consistent with functional programming principles

**Consequences:**
- Positive: Clear API, easy to test
- Positive: Backward compatible with default value
- Positive: Each provider independent
- Negative: Interface change (mitigated by default value)

---

## 13. Sequence Diagrams

### 13.1 Language Selection Sequence

```
User          UI           ViewModel        SettingsStore      SP
 │             │                │                │             │
 ├─Tap dropdown│                │                │             │
 │             │                │                │             │
 │<─Show menu ─┤                │                │             │
 │             │                │                │             │
 ├─Select "ES" │                │                │             │
 │             │                │                │             │
 │             ├─SetSummaryLanguage(ES)─────────>│             │
 │             │                │                │             │
 │             │                │                ├─putString()─>│
 │             │                │                │             │
 │             │                │                │<─OK─────────┤
 │             │                │                │             │
 │             │                │<─_summaryLanguage.value = ES
 │             │                │                │             │
 │             │<─StateFlow emits─────────────────┤             │
 │             │                │                │             │
 │<─Update UI "Español"────────┤                │             │
 │             │                │                │             │
```

### 13.2 Summary Generation Sequence

```
ViewModel       AIApi           Repository        Client        AI API
   │              │                 │              │             │
   ├─summarize()  │                 │              │             │
   │              │                 │              │             │
   │              ├─summaryLanguage.first()───────>│             │
   │              │                 │              │             │
   │              │<─Returns: SPANISH─────────────┤             │
   │              │                 │              │             │
   │              ├─generateSummary(content, SPANISH)──────────>│
   │              │                 │              │             │
   │              │                 │              ├─Build prompt│
   │              │                 │              │  with "Summarize in Spanish"
   │              │                 │              │             │
   │              │                 │              ├─Call API────>│
   │              │                 │              │             │
   │              │                 │              │<─Response ──┤
   │              │                 │              │             │
   │              │                 │<─SummaryResult────────────┤
   │              │                 │              │             │
   │<─Result ─────┤                 │              │             │
   │              │                 │              │             │
```

---

## 14. Conclusion

This architecture design provides:
- ✅ Clear separation of concerns
- ✅ Type-safe language handling
- ✅ Minimal changes to existing code
- ✅ Backward compatibility
- ✅ Extensibility for future enhancements
- ✅ Testable components
- ✅ Performance efficiency

**Next Steps:**
1. Proceed to Phase 5.5 (UI/UX Design)
2. Create detailed implementation plan (Phase 6)
3. Begin implementation (Phase 8)

---

**Last Updated:** 2026-01-01 09:53:49
**Next Phase:** Phase 5.5 (UI/UX Design)
