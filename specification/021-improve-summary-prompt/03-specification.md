# Specification: Improve Summary Prompt with JSON Response and Timeout Setting

**Spec ID:** 021
**Status:** Ready for Implementation
**Date:** 2026-01-05
**Related Specs:** 019 (improve-summary-rendering), 020 (improve-translation-page)

## Table of Contents

1. [Overview](#overview)
2. [Objectives](#objectives)
3. [Current State](#current-state)
4. [Proposed Solution](#proposed-solution)
5. [Technical Implementation](#technical-implementation)
6. [API Changes](#api-changes)
7. [UI Changes](#ui-changes)
8. [Testing Strategy](#testing-strategy)
9. [Rollout Plan](#rollout-plan)

## Overview

This specification details the enhancement of the AI-powered article summarization feature in Feeder RSS reader. The improvement focuses on three key areas:

1. **Enhanced Prompt Engineering**: Upgrade from basic prompting to professional, research-backed prompts
2. **JSON Response Format**: Replace fragile regex parsing with reliable JSON-based structured output
3. **Configurable Timeout**: Add user-configurable timeout setting (matching translation feature pattern)

## Objectives

### Primary Objectives

1. **Improve Summary Quality**
   - Implement professional role assignment in prompts
   - Add clear summarization guidelines
   - Enforce structured markdown output
   - Ensure consistency and reliability

2. **Enable Robust Parsing**
   - Replace regex-based parsing with JSON parsing
   - Add validation and error handling
   - Support fallback mechanisms

3. **Add Timeout Configuration**
   - Implement timeout setting following translation pattern
   - Provide UI in Settings → AI Integration → Summary
   - Default: 90 seconds, Range: 30-600 seconds

### Success Metrics

- ✅ Summaries use structured markdown (key points + detailed summary)
- ✅ JSON parsing succeeds 99%+ of the time
- ✅ Timeout setting appears in correct settings location
- ✅ No regressions in existing functionality
- ✅ All tests pass

## Current State

### Current Implementation

**Prompt:**
```kotlin
"""
You are a helpful assistant that summarizes news articles.
Detect the article's language and summarize in that same language.

Start your response with "Lang: " followed by the detected language code.
For example: "Lang: en"

Then provide a concise summary of the article.
"""
```

**Response Format:**
```
Lang: en
[Plain text summary]
```

**Parsing:**
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

### Problems

1. **Basic Prompt** - No professional role, minimal guidelines
2. **Fragile Parsing** - Regex-based, prone to errors
3. **Unstructured Output** - Plain text, difficult to scan
4. **No Timeout Setting** - Users cannot configure API timeout
5. **Inconsistent Quality** - Variable results due to minimal guidance

## Proposed Solution

### Solution Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Article Content                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Enhanced Summary Prompt                         │
│  • Professional role assignment                             │
│  • Clear task description                                   │
│  • JSON output format specification                         │
│  • Comprehensive guidelines                                 │
│  • Quality criteria                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   AI Provider API                            │
│              (with user timeout setting)                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              JSON Response Parsing                           │
│  • kotlinx.serialization                                    │
│  • Schema validation                                        │
│  • Error handling                                           │
│  • Fallback support                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│           Structured Summary Display                         │
│  • Key points (bulleted list)                               │
│  • Detailed summary (markdown sections)                     │
│  • Metadata (language, sentiment)                           │
└─────────────────────────────────────────────────────────────┘
```

### Enhanced Prompt Structure

Based on research from PromptLayer (2024) and GenAI Unplugged (2025):

```kotlin
"""
You are an expert news analyst and professional journalist specializing
in clear, accurate article summarization for busy professionals.

## Task

Summarize the following news article into a well-structured, scannable format.

## Input Format

- Article text will be provided
- Language may vary (detect automatically)
- Article may be short or long

## Output Format

Return ONLY a valid JSON object (no markdown code fences, no additional text):

{
  "language": "detected language code (ISO 639-1, e.g., 'en', 'zh', 'es')",
  "title": "extracted article title or empty string if not found",
  "keyPoints": [
    "point 1: most important takeaway",
    "point 2: second critical information",
    "point 3: third key insight"
  ],
  "summary": "### Key Points\\n\\n- Point 1\\n- Point 2\\n\\n### Summary\\n\\n#### Main Topic\\n\\nParagraph...",
  "sentiment": "positive, negative, neutral, or mixed"
}

## Summarization Guidelines

### Quality Standards
- **Accuracy**: Capture all essential information without distortion
- **Clarity**: Use straightforward, reader-friendly language
- **Completeness**: Include main points and important context
- **Brevity**: Keep summary concise but comprehensive
- **Objectivity**: Maintain neutral, journalistic tone for news

### Structure Requirements
- Extract 3-5 key points as bullet points
- Organize summary into logical sections with markdown headers
- Use ### for main sections, #### for subsections
- Include most important information first
- Preserve critical details and context

### Language Handling
- Detect article language automatically
- Summarize in the same language as the original article
- Return language code in 'language' field

### Content Guidelines
- Focus on substantive information, not fluff
- Include relevant data, statistics, quotes if important
- Capture the "who, what, when, where, why, how"
- Maintain journalistic objectivity
- Avoid speculation or opinion

### Format Rules
- keyPoints array: 3-5 strings, each a complete thought
- summary field: Valid markdown with proper formatting
- Use empty string "" if title cannot be determined
- Use "neutral" for sentiment if unclear
- Output ONLY the JSON object, no surrounding text

## Article to Summarize
${articleText}
"""
```

### JSON Response Schema

```kotlin
@Serializable
data class SummaryResponseJson(
    val language: String,           // ISO 639-1 code (e.g., "en", "zh", "es")
    val title: String,              // Extracted title or ""
    val keyPoints: List<String>,    // 3-5 key points
    val summary: String,            // Structured markdown
    val sentiment: String           // "positive" | "negative" | "neutral" | "mixed"
)
```

**Example Response:**
```json
{
  "language": "en",
  "title": "Breaking News: Major Tech Announcement",
  "keyPoints": [
    "Tech giant unveils revolutionary AI system",
    "New capabilities enable real-time language translation",
    "Product launches in Q1 2026 with global availability"
  ],
  "summary": "### Key Points\n\n- Tech giant unveils revolutionary AI system\n- New capabilities enable real-time language translation\n- Product launches in Q1 2026 with global availability\n\n### Summary\n\n#### Main Topic\n\nIn a groundbreaking announcement today, a leading technology company revealed its latest artificial intelligence system...\n\n#### Key Details\n\nThe new system features advanced natural language processing capabilities...\n\n#### Context\n\nThis announcement comes amid increasing competition in the AI sector...",
  "sentiment": "positive"
}
```

## Technical Implementation

### Phase 1: Data Model Updates

**File:** `AIClient.kt` (or new `SummaryResponse.kt`)

```kotlin
@Serializable
data class SummaryResponseJson(
    val language: String,
    val title: String,
    val keyPoints: List<String>,
    val summary: String,
    val sentiment: String
)

sealed interface SummaryResult {
    data class Success(
        val id: String,
        val created: Long,
        val model: String,
        val content: String,        // Keep for backward compatibility
        val promptTokens: Int,
        val completeTokens: Int,
        val totalTokens: Int,
        val detectedLanguage: String,
        // NEW: Add structured fields
        val title: String = "",
        val keyPoints: List<String> = emptyList(),
        val sentiment: String = ""
    ) : SummaryResult

    data class Error(
        val content: String
    ) : SummaryResult
}
```

### Phase 2: Prompt Updates

**File:** `AnthropicClient.kt`

```kotlin
/**
 * Builds an enhanced JSON-structured summary prompt with professional role
 * assignment and comprehensive guidelines.
 *
 * Based on research from:
 * - PromptLayer: "Best Prompts for Text Summarization" (Dec 2024)
 * - GenAI Unplugged: "How to Get Perfect JSON from AI Every Time" (Nov 2025)
 * - OpenAI: "Best Practices for Prompt Engineering"
 */
private fun buildSummaryPrompt(language: SummaryLanguage): String {
    return when (language) {
        SummaryLanguage.AUTO_DETECT -> """
            You are an expert news analyst and professional journalist specializing
            in clear, accurate article summarization for busy professionals.

            ## Task

            Summarize the following news article into a well-structured, scannable format.

            ## Output Format

            Return ONLY a valid JSON object (no markdown code fences, no additional text):

            {
              "language": "detected language code (ISO 639-1, e.g., 'en', 'zh', 'es')",
              "title": "extracted article title or empty string if not found",
              "keyPoints": [
                "point 1: most important takeaway",
                "point 2: second critical information",
                "point 3: third key insight"
              ],
              "summary": "### Key Points\\n\\n- Point 1\\n- Point 2\\n\\n### Summary\\n\\n#### Main Topic\\n\\nParagraph...",
              "sentiment": "positive, negative, neutral, or mixed"
            }

            ## Summarization Guidelines

            ### Quality Standards
            - **Accuracy**: Capture all essential information without distortion
            - **Clarity**: Use straightforward, reader-friendly language
            - **Completeness**: Include main points and important context
            - **Brevity**: Keep summary concise but comprehensive
            - **Objectivity**: Maintain neutral, journalistic tone for news

            ### Structure Requirements
            - Extract 3-5 key points as bullet points
            - Organize summary into logical sections with markdown headers
            - Use ### for main sections, #### for subsections
            - Include most important information first

            ### Language Handling
            - Detect article language automatically
            - Summarize in the same language as the original article

            ### Format Rules
            - keyPoints array: 3-5 strings, each a complete thought
            - summary field: Valid markdown with proper formatting
            - Use empty string "" if title cannot be determined
            - Use "neutral" for sentiment if unclear
            - Output ONLY the JSON object, no surrounding text

            ## Article to Summarize
        """.trimIndent()

        else -> """
            You are an expert news analyst and professional journalist specializing
            in clear, accurate article summarization for busy professionals.

            ## Task

            Summarize the following news article in ${language.languageName}
            into a well-structured, scannable format.

            ## Output Format

            Return ONLY a valid JSON object (no markdown code fences, no additional text):

            {
              "language": "${language.languageCode}",
              "title": "extracted article title or empty string if not found",
              "keyPoints": [
                "point 1: most important takeaway",
                "point 2: second critical information",
                "point 3: third key insight"
              ],
              "summary": "### Key Points\\n\\n- Point 1\\n- Point 2\\n\\n### Summary\\n\\n#### Main Topic\\n\\nParagraph...",
              "sentiment": "positive, negative, neutral, or mixed"
            }

            ## Summarization Guidelines

            ### Quality Standards
            - **Accuracy**: Capture all essential information without distortion
            - **Clarity**: Use straightforward, reader-friendly language
            - **Completeness**: Include main points and important context
            - **Brevity**: Keep summary concise but comprehensive
            - **Objectivity**: Maintain neutral, journalistic tone for news

            ### Structure Requirements
            - Extract 3-5 key points as bullet points
            - Organize summary into logical sections with markdown headers
            - Use ### for main sections, #### for subsections
            - Include most important information first

            ### Format Rules
            - keyPoints array: 3-5 strings, each a complete thought
            - summary field: Valid markdown with proper formatting
            - Use empty string "" if title cannot be determined
            - Use "neutral" for sentiment if unclear
            - Output ONLY the JSON object, no surrounding text

            ## Article to Summarize (in ${language.languageName})
        """.trimIndent()
    }
}
```

**File:** `OpenAICompatibleClient.kt` - Same changes as AnthropicClient.kt

### Phase 3: JSON Parsing Implementation

**File:** `AnthropicClient.kt`

```kotlin
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Parses JSON summary response with validation and fallback.
 */
private fun parseSummaryJsonResponse(content: String): SummaryResponseData {
    return try {
        // Try to extract JSON from markdown code blocks
        val jsonContent = extractJsonFromMarkdown(content)

        // Parse JSON
        val jsonObject = jsonParser.parseToJsonElement(jsonContent).jsonObject

        // Extract fields with validation
        val language = jsonObject["language"]?.jsonPrimitive?.content ?: ""
        val title = jsonObject["title"]?.jsonPrimitive?.content ?: ""
        val keyPoints = jsonObject["keyPoints"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.content.takeIf { it.isNotBlank() }
        } ?: emptyList()
        val summary = jsonObject["summary"]?.jsonPrimitive?.content ?: ""
        val sentiment = jsonObject["sentiment"]?.jsonPrimitive?.content ?: "neutral"

        SummaryResponseData(
            language = language,
            title = title,
            keyPoints = keyPoints,
            summary = summary,
            sentiment = sentiment
        )
    } catch (e: SerializationException) {
        // Fallback to old parsing if JSON fails
        parseLegacySummaryResponse(content)
    } catch (e: Exception) {
        // Fallback to old parsing on any error
        parseLegacySummaryResponse(content)
    }
}

/**
 * Extracts JSON from markdown code blocks or returns content as-is.
 */
private fun extractJsonFromMarkdown(content: String): String {
    // Try to extract from ```json code blocks
    val jsonBlockRegex = """```json\s*([\s\S]*?)\s*```""".toRegex()
    val match = jsonBlockRegex.find(content)
    if (match != null) {
        return match.groupValues[1].trim()
    }

    // Try to extract from ``` code blocks
    val codeBlockRegex = """```\s*([\s\S]*?)\s*```""".toRegex()
    val codeMatch = codeBlockRegex.find(content)
    if (codeMatch != null) {
        return codeMatch.groupValues[1].trim()
    }

    // Return as-is if no code blocks found
    return content.trim()
}

/**
 * Legacy parsing for fallback compatibility.
 */
private fun parseLegacySummaryResponse(content: String): SummaryResponseData {
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
    return SummaryResponseData(
        language = lang,
        title = "",
        keyPoints = emptyList(),
        summary = summary,
        sentiment = "neutral"
    )
}

/**
 * Data holder for parsed summary response.
 */
private data class SummaryResponseData(
    val language: String,
    val title: String,
    val keyPoints: List<String>,
    val summary: String,
    val sentiment: String
)
```

**Update generateSummary method:**

```kotlin
override suspend fun generateSummary(
    content: String,
    language: SummaryLanguage
): AIClient.SummaryResult {
    if (!settings.isValid) {
        return AIClient.SummaryResult.Error(content = "Invalid settings")
    }

    return try {
        val systemPrompt = buildSummaryPrompt(language)

        val params = MessageCreateParams.builder()
            .model(settings.modelId)
            .system(systemPrompt)
            .maxTokens(2048L)  // Increased for JSON response
            .addUserMessage(content)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.messages().create(params).get()
        }

        // Get content blocks
        val text = response.content().joinToString("") { contentBlock ->
            contentBlock.text().getOrNull()?.text() ?: ""
        }

        // Parse JSON response
        val summaryData = parseSummaryJsonResponse(text)

        // Get usage info
        val usage = response.usage()
        val promptTokens = usage.inputTokens().toInt()
        val completeTokens = usage.outputTokens().toInt()
        val totalTokens = promptTokens + completeTokens

        // Get stop reason
        val isComplete = response.stopReason().getOrNull()?.toString() == "end_turn"

        AIClient.SummaryResult.Success(
            id = response.id(),
            created = if (isComplete) 1L else 0L,
            model = response.model().toString(),
            content = summaryData.summary,
            promptTokens = promptTokens,
            completeTokens = completeTokens,
            totalTokens = totalTokens,
            detectedLanguage = summaryData.language,
            title = summaryData.title,
            keyPoints = summaryData.keyPoints,
            sentiment = summaryData.sentiment
        )
    } catch (e: Exception) {
        AIClient.SummaryResult.Error(content = e.message ?: e.cause?.message ?: "Unknown error")
    }
}
```

### Phase 4: Timeout Setting Implementation

**File:** `SettingsStore.kt`

```kotlin
companion object {
    // ... existing constants ...
    private const val PREF_SUMMARY_TIMEOUT_SECONDS = "summary_timeout_seconds"
    private const val DEFAULT_SUMMARY_TIMEOUT_SECONDS = 90
}

// Line ~790 (after translation timeout)
private val _summaryTimeout = MutableStateFlow(
    sp.getInt(PREF_SUMMARY_TIMEOUT_SECONDS, DEFAULT_SUMMARY_TIMEOUT_SECONDS)
)
val summaryTimeout = _summaryTimeout.asStateFlow()

fun setSummaryTimeout(value: Int) {
    _summaryTimeout.value = value.coerceIn(30, 600)
    sp.edit { putInt(PREF_SUMMARY_TIMEOUT_SECONDS, value.coerceIn(30, 600)) }
}
```

**File:** `Repository.kt`

```kotlin
// Line ~385 (after translationTimeout)
val summaryTimeout = settingsStore.summaryTimeout
```

**File:** `AIApi.kt`

```kotlin
// In generateSummary method (before calling client)
suspend fun generateSummary(
    content: String,
    language: SummaryLanguage
): AIClient.SummaryResult {
    // ... existing validation ...

    // Get summary-specific timeout
    val summaryTimeout = repository.summaryTimeout.first()

    // Create client with summary-specific timeout
    val settingsWithTimeout = when (val settings = aiSettings) {
        is AISettings.OpenAI -> {
            val updatedSettings = settings.openaiSettings.copy(timeoutSeconds = summaryTimeout)
            AISettings.OpenAI(updatedSettings)
        }
        is AISettings.Anthropic -> {
            val updatedSettings = settings.anthropicSettings.copy(timeoutSeconds = summaryTimeout)
            AISettings.Anthropic(updatedSettings)
        }
    }

    // Create client with timeout settings
    val clientWithTimeout = AIClient.create(settingsWithTimeout)

    return clientWithTimeout.generateSummary(content, language)
}
```

**File:** `SummarySettingsViewModel.kt` (NEW)

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SummarySettingsViewModel(
    private val repository: Repository,
) : ViewModel() {

    val summaryTimeout: StateFlow<Int> = repository.summaryTimeout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 90
        )

    fun setSummaryTimeout(value: Int) {
        repository.setSummaryTimeout(value)
    }
}
```

**File:** `SummarySettingsScreen.kt` (NEW)

```kotlin
package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.components.TimeoutSetting

@Composable
fun SummarySettingsScreen(
    viewModel: SummarySettingsViewModel,
) {
    val summaryTimeout by viewModel.summaryTimeout.collectAsStateWithLifecycle()

    SettingsScreen(
        title = stringResource(R.string.summary_settings_title),
    ) {
        // Header
        SettingHeader(
            title = stringResource(R.string.summary_settings_header),
            subtitle = stringResource(R.string.summary_settings_subtitle),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timeout Slider
        TimeoutSetting(
            title = stringResource(R.string.summary_timeout_title),
            description = stringResource(R.string.summary_timeout_description),
            timeoutSeconds = summaryTimeout,
            onTimeoutChange = { viewModel.setSummaryTimeout(it) },
        )
    }
}
```

**File:** `strings.xml` (add strings)

```xml
<!-- Summary Settings -->
<string name="summary_settings_title">Summary</string>
<string name="summary_settings_header">Summary Settings</string>
<string name="summary_settings_subtitle">Configure AI-powered article summarization</string>
<string name="summary_timeout_title">Summary Timeout</string>
<string name="summary_timeout_description">Maximum time to wait for AI to generate a summary</string>
<string name="summary_timeout_seconds_label">seconds</string>
```

## API Changes

### Public API (Stable)

No breaking changes to public API. `SummaryResult.Success` adds new optional fields with default values.

### Internal API

**Before:**
```kotlin
SummaryResult.Success(
    content = "Plain text summary",
    detectedLanguage = "en"
)
```

**After:**
```kotlin
SummaryResult.Success(
    content = "### Key Points\n\n- Point 1\n- Point 2\n\n### Summary\n\n...",
    detectedLanguage = "en",
    title = "Article Title",
    keyPoints = listOf("Point 1", "Point 2"),
    sentiment = "neutral"
)
```

## UI Changes

### Settings Navigation

**Before:**
Settings → AI Integration → Translation

**After:**
Settings → AI Integration → Translation
                        → Summary (NEW)

### Summary Settings Screen

- **Title:** "Summary Settings"
- **Icon:** Timer icon
- **Sections:**
  1. Summary Timeout (slider: 30-600 seconds, default: 90)

### Future Enhancements (Out of Scope for This Spec)

- Summary length options (short/medium/long)
- Summary style options (bullet points/paragraphs)
- Sentiment analysis toggle
- Summary language selector (currently auto-detect only)

## Testing Strategy

### Unit Tests

1. **JSON Parsing Tests**
   - Valid JSON with all fields
   - Valid JSON with missing optional fields
   - JSON in markdown code blocks
   - Malformed JSON (fallback)
   - Legacy format (Lang: prefix)

2. **Prompt Tests**
   - AUTO_DETECT language
   - Specific language (e.g., English)
   - Prompt structure validation

3. **Timeout Tests**
   - Default value (90 seconds)
   - Range enforcement (30-600)
   - Persistence

### Integration Tests

1. **End-to-End Summary**
   - Real AI API calls
   - JSON response parsing
   - Error handling

2. **Settings Flow**
   - Change timeout value
   - Verify persistence
   - Verify application in summary generation

### Manual Testing

1. **Summary Quality**
   - Test with various article types
   - Verify markdown structure
   - Verify key points extraction
   - Verify language detection

2. **Timeout Functionality**
   - Set minimum timeout (30s)
   - Set maximum timeout (600s)
   - Verify timeout applied to API calls

## Rollout Plan

### Phase 1: Implementation (Week 1)

- ✅ Data model updates
- ✅ Prompt enhancements
- ✅ JSON parsing implementation
- ✅ Timeout setting implementation

### Phase 2: Testing (Week 1)

- ✅ Unit tests
- ✅ Integration tests
- ✅ Manual testing

### Phase 3: Code Review (Week 1)

- ✅ Self-review
- ✅ Peer review (if available)
- ✅ Test coverage verification

### Phase 4: Deployment (Week 2)

- ✅ Merge to main branch
- ✅ Release in next app version

## Dependencies

### External Dependencies

- kotlinx.serialization (already in use)
- Kotlin coroutines (already in use)
- Anthropic Claude API (existing)
- OpenAI-compatible APIs (existing)

### Internal Dependencies

- Translation feature implementation (pattern reference)
- Settings infrastructure (already exists)
- AI provider infrastructure (already exists)

## Risks and Mitigations

### Risk 1: Increased Token Usage

**Impact:** Higher API costs
**Probability:** Medium
**Mitigation:**
- Monitor token usage in testing
- Optimize prompt if needed
- Provide user feedback on token costs

### Risk 2: JSON Parsing Failures

**Impact:** Summary generation errors
**Probability:** Low
**Mitigation:**
- Robust error handling
- Fallback to legacy format
- Comprehensive testing

### Risk 3: Quality Regression

**Impact:** Worse summaries than before
**Probability:** Very Low
**Mitigation:**
- Research-backed prompt design
- Extensive testing
- Iterative refinement

### Risk 4: Timeout Setting Issues

**Impact:** User frustration
**Probability:** Very Low
**Mitigation:**
- Follow proven translation pattern
- Thorough testing
- Clear UI labels

## Success Criteria

### Functional Requirements

- ✅ JSON response format implemented
- ✅ Structured markdown in summary field
- ✅ Key points extracted (3-5 points)
- ✅ Language detection working
- ✅ Sentiment analysis included
- ✅ Timeout setting functional
- ✅ Settings UI implemented

### Non-Functional Requirements

- ✅ No regressions in existing functionality
- ✅ 99%+ JSON parsing success rate
- ✅ Error handling robust
- ✅ Code follows existing patterns
- ✅ Tests pass
- ✅ Documentation updated

### User Experience

- ✅ Summaries are scannable and well-structured
- ✅ Timeout setting easy to find and configure
- ✅ No breaking changes to user workflow
- ✅ Clear error messages

## Open Questions

### Q1: Should we add sentiment to the UI display?

**Status:** Out of scope for this spec
**Rationale:** Spec-019 (improve-summary-rendering) may address display enhancements
**Decision:** Store sentiment in result, but don't display in UI yet

### Q2: Should we support user-selected summary length?

**Status:** Out of scope for this spec
**Rationale:** Advanced feature, can be added later
**Decision:** Keep summary length automatic for now

### Q3: What timeout value should we use as default?

**Status:** Answered
**Decision:** 90 seconds (matches translation default)
**Rationale:** Proven value, user familiarity

## Appendix

### Research Sources

1. **PromptLayer** - "Best Prompts for Text Summarization: Guide to AI Summaries" (Dec 2024)
   - https://blog.promptlayer.com/best-prompts-for-text-summarization-guide-to-ai-summaries/

2. **GenAI Unplugged** - "How to Get Perfect JSON from AI Every Time" (Nov 2025)
   - https://genaiunplugged.substack.com/p/structured-outputs-json-prompts-guide

3. **OpenAI** - "Best Practices for Prompt Engineering"
   - https://help.openai.com/en/articles/6654000-best-practices-for-prompt-engineering-with-the-openai-api

4. **dair-ai** - "Prompt Engineering Guide" (GitHub)
   - https://github.com/dair-ai/Prompt-Engineering-Guide

### Related Specifications

- **Spec-019:** Improve Summary Rendering - Markdown rendering support
- **Spec-020:** Improve Translation Page - Translation enhancements
- **Spec-018:** Decouple Target Language Setting - Settings architecture

### Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-05 | 1.0 | Initial specification |
