# Code Assessment: Summary Feature Implementation

**Date:** 2026-01-05
**Assessment Focus:** Current summary implementation and translation timeout pattern for replication

## Executive Summary

The codebase has a well-structured AI implementation with a successful translation feature that demonstrates all the patterns we need to replicate for the summary feature. The current summary implementation uses basic prompting and fragile regex parsing, which needs to be upgraded to match the sophistication of the translation feature.

## Current Summary Implementation Analysis

### Files Involved

1. **`AIClient.kt`** - Interface definition
   - `generateSummary()` method signature
   - `SummaryResult` sealed interface (Success/Error)
   - `SummaryLanguage` enum

2. **`AnthropicClient.kt`** (lines 34-52, 174-187)
   - `buildSummaryPrompt()` - Basic prompt construction
   - `parseSummaryResponse()` - Fragile regex-based parsing
   - `generateSummary()` - Main implementation

3. **`OpenAICompatibleClient.kt`** (lines 81-98, 227-240)
   - Same structure as AnthropicClient
   - Similar basic prompt
   - Similar fragile parsing

### Current Prompt Structure

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

### Current Parsing Logic

```kotlin
private fun parseSummaryResponse(content: String): Pair<String, String> {
    val lines = content.lines()
    val lang = if (lines.firstOrNull()?.startsWith("Lang:") == true) {
        lines.first().removePrefix("Lang:").trim().take(2)
    } else {
        ""
    }
    val summary = if (lines.firstOrNull()?.startsWith("Lang:") == true) {
        lines.drop(1).joinToString("\n").trim()
    } else {
        content.trim()
    }
    return lang to summary
}
```

### Issues Identified

1. **Basic Prompt**
   - ❌ No professional role assignment
   - ❌ No clear summarization guidelines
   - ❌ No quality criteria
   - ❌ Lacks structure awareness
   - ❌ Minimal instructions

2. **Fragile Parsing**
   - ❌ Relies on string prefix matching
   - ❌ Prone to parsing errors
   - ❌ No structured output
   - ❌ No validation
   - ❌ Regex-based language extraction

3. **Unstructured Output**
   - ❌ Plain text only
   - ❌ No logical sections
   - ❌ Inconsistent quality
   - ❌ Difficult to scan

## Translation Feature as Reference Pattern

### Translation Implementation Strengths

The translation feature demonstrates EXCELLENT patterns that should be replicated:

### 1. Professional Role Assignment

```kotlin
"""
You are a distinguished professional translator and bilingual scholar
specializing in ${targetLanguage.languageName}. Your expertise encompasses
accurately and elegantly translating texts while meticulously considering all
linguistic complexities, nuances, and cultural contexts.
"""
```

**Key Elements:**
- Professional role ("distinguished professional translator")
- Expertise level ("bilingual scholar")
- Specialization mentioned
- Emphasis on quality and expertise

### 2. Clear Task Description

```kotlin
"""
## Translation Task

Translate the following article paragraphs from JSON format to ${targetLanguage.languageName}.
Each paragraph includes structure information (element type and nesting level)
to help you provide better translations.
"""
```

**Key Elements:**
- Clear task name ("## Translation Task")
- Specific action ("Translate")
- Input format specification
- Context explanation

### 3. Structured JSON Input/Output

```kotlin
"""
## Input Format (JSON)
```json
{
  "targetLanguage": "${targetLanguage.languageName}",
  "paragraphs": [
    {"index": 1, "type": "heading", "text": "..."},
    {"index": 2, "type": "paragraph", "text": "..."}
  ]
}
```

## Output Requirements

Respond with a JSON object in the following exact format:
```json
{
  "targetLanguage": "${targetLanguage.languageName}",
  "translations": [
    {"index": 1, "translation": "..."},
    {"index": 2, "translation": "..."}
  ]
}
```
"""
```

**Key Elements:**
- Clear input format specification
- Example JSON structure
- Explicit output requirements
- "exact format" emphasis

### 4. Comprehensive Guidelines

```kotlin
"""
## Translation Guidelines

1. **Structure Awareness**: Consider the element type and nesting level:
   - Headings should remain concise and authoritative
   - Paragraphs should flow naturally in ${targetLanguage.languageName}
   - Nested list items should maintain proper indentation and hierarchy
   - Blockquotes should preserve the quoted tone

2. **Accuracy & Precision**: Maintain technical accuracy while ensuring
   the translation flows naturally in ${targetLanguage.languageName}

3. **Cultural Adaptation**: Adapt expressions and cultural references to
   ${targetLanguage.languageName} language conventions

4. **Tone Preservation**: Preserve the author's style, tone, and intent
   based on element type

5. **Technical Terms**: Keep technical terminology, code, variable names,
   and commands untranslated

6. **Format Preservation**: Maintain the original paragraph structure,
   numbering, and layout

7. **Quality**: Provide fluent, professional translations that read
   naturally to native speakers

8. **Consistency**: Use consistent terminology throughout the translation

9. **Completeness**: Translate ALL paragraphs. Return exactly
   ${translatableTexts.size} translations.
"""
```

**Key Elements:**
- Numbered guidelines (1-9)
- Bold category names
- Specific instructions per category
- Clear quality standards
- Completeness requirements

### 5. Important Reminders

```kotlin
"""
## Important

- Return ONLY the JSON object, no additional text or explanations
- Ensure the JSON is valid and can be parsed
- Match each index exactly from the input (1 to ${translatableTexts.size})
"""
```

**Key Elements:**
- "## Important" section
- Specific format rules
- Validation reminders
- Index matching requirements

### 6. Robust JSON Parsing

The translation feature uses proper JSON parsing (not regex):

```kotlin
// Parse translation response as JSON
val translations = parseTranslationResponse(text)
```

With validation and error handling.

## Translation Timeout Implementation Pattern

### 1. Repository Layer (`SettingsStore.kt`)

```kotlin
// Line 785-786
private val _translationTimeout = MutableStateFlow(sp.getInt(PREF_TRANSLATION_TIMEOUT_SECONDS, 90))
val translationTimeout = _translationTimeout.asStateFlow()

// Line 788-789
fun setTranslationTimeout(value: Int) {
    _translationTimeout.value = value.coerceIn(30, 600)
    sp.edit { putInt(PREF_TRANSLATION_TIMEOUT_SECONDS, value.coerceIn(30, 600)) }
}
```

**Key Elements:**
- MutableStateFlow for reactive state
- SharedPreferences persistence
- Default value: 90 seconds
- Range coercion: 30-600 seconds
- Public immutable StateFlow exposure

### 2. Repository Exposure (`Repository.kt`)

```kotlin
// Line 383
val translationTimeout = settingsStore.translationTimeout
```

**Key Elements:**
- Simple delegation to settingsStore
- Public StateFlow for observation

### 3. API Usage (`AIApi.kt`)

```kotlin
// Line 115
val translationTimeout = repository.translationTimeout.first()

// Line 118-127
val settingsWithTimeout = when (val settings = aiSettings) {
    is AISettings.OpenAI -> {
        val updatedSettings = settings.openaiSettings.copy(timeoutSeconds = translationTimeout)
        AISettings.OpenAI(updatedSettings)
    }
    is AISettings.Anthropic -> {
        val updatedSettings = settings.anthropicSettings.copy(timeoutSeconds = translationTimeout)
        AISettings.Anthropic(updatedSettings)
    }
}
```

**Key Elements:**
- Get timeout from repository (using `.first()` for current value)
- Copy settings with updated timeout
- Handle both OpenAI and Anthropic
- Create new AISettings instance

### 4. ViewModel (`TranslationSettingsViewModel.kt`)

```kotlin
val translationTimeout: StateFlow<Int> = repository.translationTimeout

fun setTranslationTimeout(value: Int) {
    repository.setTranslationTimeout(value)
}
```

**Key Elements:**
- Expose timeout from repository
- Delegate setter to repository

### 5. UI Screen (`TranslationSettingsScreen.kt`)

```kotlin
// Line 116-121
TimeoutSetting(
    title = stringResource(R.string.translation_timeout_title),
    description = stringResource(R.string.translation_timeout_description),
    timeoutSeconds = translationTimeout,
    onTimeoutChange = { viewModel.setTranslationTimeout(it) },
)
```

**Key Elements:**
- Reusable `TimeoutSetting` composable
- String resources for localization
- State observation
- Callback for changes

### 6. UI Component (`TimeoutSetting` composable)

Slider with:
- Range: 30-600 seconds
- Step: 10 seconds
- Visual feedback
- Real-time updates

## Implementation Plan for Summary Feature

### Phase 1: Update Summary Prompts

**Files to Modify:**
- `AnthropicClient.kt` - `buildSummaryPrompt()` method
- `OpenAICompatibleClient.kt` - `buildSummaryPrompt()` method

**Changes:**
1. Add professional role assignment
2. Add clear task description
3. Add JSON output format specification
4. Add summarization guidelines
5. Add quality criteria
6. Add example JSON response
7. Add important reminders

### Phase 2: Add JSON Parsing

**Files to Modify:**
- `AnthropicClient.kt` - Replace `parseSummaryResponse()`
- `OpenAICompatibleClient.kt` - Replace `parseSummaryResponse()`

**Changes:**
1. Create new `parseSummaryJsonResponse()` method
2. Use kotlinx.serialization for JSON parsing
3. Add validation for required fields
4. Add error handling for malformed JSON
5. Add fallback to old format if needed

### Phase 3: Add Summary Timeout Setting

**Files to Modify:**
1. `SettingsStore.kt`
   - Add `PREF_SUMMARY_TIMEOUT_SECONDS` constant
   - Add `_summaryTimeout` MutableStateFlow
   - Add `summaryTimeout` StateFlow
   - Add `setSummaryTimeout()` method

2. `Repository.kt`
   - Add `val summaryTimeout = settingsStore.summaryTimeout`

3. `AIApi.kt`
   - Add timeout logic to `generateSummary()` method
   - Follow same pattern as translation

4. Create `SummarySettingsScreen.kt`
   - Follow `TranslationSettingsScreen.kt` pattern
   - Add timeout slider
   - Add language selector (if not already present)

5. Create `SummarySettingsViewModel.kt`
   - Follow `TranslationSettingsViewModel.kt` pattern

6. Add string resources
   - `summary_timeout_title`
   - `summary_timeout_description`
   - `summary_timeout_seconds_label`

7. Update settings navigation
   - Add "Summary" option under "AI Integration"

### Phase 4: Update Data Models

**Files to Modify:**
- `AIClient.kt` or create new `SummaryResponse.kt`

**Changes:**
1. Create `@Serializable data class SummaryResponseJson`
   - `language: String`
   - `title: String`
   - `keyPoints: List<String>`
   - `summary: String`
   - `sentiment: String`

2. Update `SummaryResult.Success` if needed
   - Currently returns `content: String`
   - May need to add fields for new structured data

## Code Quality Assessment

### Strengths

✅ **Excellent Architecture**
- Clean separation of concerns
- Provider abstraction (AIClient interface)
- Factory pattern for client creation
- Sealed classes for results

✅ **Translation Feature as Template**
- Professional prompting
- JSON structured I/O
- Robust error handling
- Timeout settings
- UI components

✅ **Reactive State Management**
- StateFlow for settings
- Coroutine-based async operations
- Proper lifecycle handling

### Weaknesses

❌ **Summary Implementation Outdated**
- Basic prompting
- Fragile parsing
- No timeout setting
- No structured output

❌ **Inconsistent Feature Parity**
- Translation has sophisticated prompts
- Summary has basic prompts
- Translation has timeout
- Summary has no timeout

## Recommendations

### High Priority

1. **Upgrade Summary Prompts**
   - Follow translation pattern
   - Add professional role
   - Add JSON output format
   - Add comprehensive guidelines

2. **Implement JSON Parsing**
   - Replace regex with proper JSON parsing
   - Add validation
   - Add error handling

3. **Add Timeout Setting**
   - Follow translation timeout pattern exactly
   - Add to Settings → AI Integration → Summary
   - Default: 90 seconds
   - Range: 30-600 seconds

### Medium Priority

4. **Consider UI Enhancements**
   - Separate Summary settings screen (like Translation)
   - Currently may be mixed with other settings

5. **Add Testing**
   - Unit tests for JSON parsing
   - Integration tests for summary generation
   - Error handling tests

### Low Priority

6. **Future Enhancements**
   - Summary length options (short/medium/long)
   - Summary style options (bullet points/paragraphs)
   - Sentiment analysis toggle

## Technical Debt

### Current Debt

1. **Fragile Parsing** - Regex-based language extraction
2. **No Validation** - No JSON schema validation
3. **No Timeout** - No user control over API timeout
4. **Inconsistent Quality** - Basic prompts lead to variable results

### Debt Resolution

Implementing this spec will resolve:
- ✅ Fragile parsing → Proper JSON parsing
- ✅ No validation → JSON schema validation
- ✅ No timeout → Configurable timeout setting
- ✅ Inconsistent quality → Professional prompts with guidelines

## Conclusion

The codebase has an excellent foundation with the translation feature demonstrating all the patterns needed. The summary feature needs to be upgraded to match the translation feature's sophistication. The implementation should be straightforward by following the established patterns.

**Next Steps:**
1. ✅ Code assessment complete
2. ⏭️ Create detailed specification
3. ⏭️ Implement summary prompt upgrades
4. ⏭️ Implement JSON parsing
5. ⏭️ Implement timeout setting
6. ⏭️ Test and validate
