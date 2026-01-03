# Phase 6: Technical Specification

**Feature**: AI Translation Function Implementation
**Date**: 2026-01-03
**Phase**: 6 - Specification Writing
**Status**: Complete

---

## Overview

This specification defines the technical implementation of real AI-powered translation to replace the dummy implementation in Feeder RSS reader.

---

## System Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────┐
│                   ArticleViewModel                   │
│  - Manages translation state                        │
│  - Triggers translation (auto + manual)             │
│  - Caches results in session                        │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│                      AIApi                           │
│  - High-level translation API                       │
│  - Gets target language from settings               │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│                   AIClient                          │
│  - Unified interface                                │
│  - Factory creates provider client                  │
└─────┬───────────────────────────┬───────────────────┘
      │                           │
      ▼                           ▼
┌──────────────────┐    ┌──────────────────┐
│ OpenAICompatible  │    │   Anthropic      │
│     Client        │    │     Client       │
│                   │    │                  │
│ - GPT-4o/4o-mini  │    │ - Claude 3.5     │
│ - Azure OpenAI    │    │ - Claude 3 Haiku │
└───────────────────┘    └──────────────────┘
```

---

## API Design

### AIClient.translate() Interface

```kotlin
interface AIClient {
    sealed interface TranslationResult {
        val content: String

        data class Success(
            val paragraphs: List<String>
        ) : TranslationResult {
            override val content: String
                get() = paragraphs.joinToString("\n\n")
        }

        data class Error(
            override val content: String
        ) : TranslationResult
    }

    suspend fun translate(
        paragraphs: List<String>
    ): TranslationResult
}
```

### OpenAI Implementation

```kotlin
override suspend fun translate(
    paragraphs: List<String>
): AIClient.TranslationResult {
    return try {
        val targetLanguage = getTargetLanguage()
        val prompt = buildTranslationPrompt(paragraphs, targetLanguage)

        val params = ChatCompletionCreateParams.builder()
            .model(settings.modelId)
            .temperature(0.3)
            .messages(listOf(
                ChatMessage(ChatMessage.Role.USER, prompt)
            ))
            .build()

        val response = withContext(Dispatchers.IO) {
            client.chat.completions.create(params).get()
        }

        val translatedText = response.choices.first().message.content
        val translatedParagraphs = parseTranslationResponse(
            translatedText,
            paragraphs.size
        )

        AIClient.TranslationResult.Success(translatedParagraphs)
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            content = handleTranslationError(e)
        )
    }
}
```

### Anthropic Implementation

```kotlin
override suspend fun translate(
    paragraphs: List<String>
): AIClient.TranslationResult {
    return try {
        val targetLanguage = getTargetLanguage()
        val prompt = buildTranslationPrompt(paragraphs, targetLanguage)

        val params = MessageCreateParams.builder()
            .model(settings.modelId)
            .maxTokens(8192L)
            .addUserMessage(prompt)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.messages().create(params).get()
        }

        val translatedText = response.content
            .firstOrNull { it.hasText() }
            ?.text()
            ?: throw TranslationException("Empty response")

        val translatedParagraphs = parseTranslationResponse(
            translatedText,
            paragraphs.size
        )

        AIClient.TranslationResult.Success(translatedParagraphs)
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            content = handleTranslationError(e)
        )
    }
}
```

---

## Prompt Engineering

### Translation Prompt Template

```kotlin
private fun buildTranslationPrompt(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): String {
    val numberedParagraphs = paragraphs.mapIndexed { index, text ->
        "[${index + 1}] $text"
    }.joinToString("\n\n")

    return """
        You are a professional translator. Translate the following article to ${targetLanguage.languageName}.

        $numberedParagraphs

        Provide your translation in the same numbered format:
        [1] (translation of paragraph 1)
        [2] (translation of paragraph 2)
        ...

        Guidelines:
        - Maintain the numbered format [N] for each paragraph
        - Translate only the content, not the numbers
        - Preserve the meaning and tone
        - Use natural, fluent expressions
        - Return only the numbered translations
    """.trimIndent()
}
```

---

## Response Parsing

### Parsing Algorithm

```kotlin
private fun parseTranslationResponse(
    response: String,
    expectedParagraphs: Int
): List<String> {
    val paragraphPattern = Regex(
        "\\[(\\d+)\\]\\s*(.+?)(?=\\[\\d+\\]|\\Z)",
        RegexOption.DOT_MATCHES_ALL
    )

    val translations = paragraphPattern.findAll(response)
        .associate { it.groupValues[1].toInt() to it.groupValues[2].trim() }
        .toSortedMap()
        .values
        .toList()

    // Validate paragraph count
    if (translations.size != expectedParagraphs) {
        throw TranslationException(
            "Expected $expectedParagraphs paragraphs, got ${translations.size}"
        )
    }

    return translations
}
```

### Error Handling Strategy

```kotlin
private fun handleTranslationError(e: Exception): String {
    return when {
        e.message?.contains("rate limit", ignoreCase = true) == true ->
            "Rate limit exceeded. Please try again later."

        e.message?.contains("invalid api key", ignoreCase = true) == true ->
            "Invalid API key. Check your AI provider settings."

        e.message?.contains("timeout", ignoreCase = true) == true ||
        e is SocketTimeoutException ->
            "Translation timed out. Please check your connection."

        e.message?.contains("insufficient quota", ignoreCase = true) == true ->
            "API quota exceeded. Please check your account."

        else ->
            "Translation failed: ${e.message ?: "Unknown error"}"
    }
}
```

---

## Auto-Translate Implementation

### Trigger Logic (ArticleViewModel.kt)

```kotlin
init {
    // ... existing init code ...

    // Check if auto-translate is enabled
    viewModelScope.launch {
        val enabled = repository.translationEnabled.first()
        if (enabled && translationState.value is TranslationState.Empty) {
            // Trigger auto-translation after article loads
            translate()
        }
    }
}
```

**Note**: This may already be implemented. Will verify during execution.

---

## Data Models

### Translation State

```kotlin
sealed interface TranslationState {
    object Empty : TranslationState
    object Loading : TranslationState
    data class Result(val value: AIClient.TranslationResult) : TranslationState
}
```

### Translation Settings

```kotlin
// Already in SettingsStore.kt
val translationEnabled: StateFlow<Boolean>
val translationLanguage: StateFlow<TranslationLanguage>
```

---

## Performance Considerations

### Token Estimation

```kotlin
private fun estimateTokens(text: String): Int {
    // Rough estimation: 1 token ≈ 4 characters
    return (text.length / 4).toInt()
}

private fun willExceedContextWindow(
    paragraphs: List<String>,
    maxTokens: Int = 128000
): Boolean {
    val totalText = paragraphs.joinToString("\n\n")
    val estimatedTokens = estimateTokens(totalText)
    // Use 80% of context window to be safe
    return estimatedTokens > (maxTokens * 0.8).toInt()
}
```

### Truncation Strategy (Future)

```kotlin
private fun truncateToFitContext(
    paragraphs: List<String>,
    maxTokens: Int
): List<String> {
    val result = mutableListOf<String>()
    var currentTokens = 0

    for (paragraph in paragraphs) {
        val paragraphTokens = estimateTokens(paragraph)
        if (currentTokens + paragraphTokens > maxTokens) {
            break
        }
        result.add(paragraph)
        currentTokens += paragraphTokens
    }

    return result
}
```

---

## Testing Strategy

### Unit Tests

```kotlin
class TranslationParserTest {
    @Test
    fun `parseTranslationResponse extracts numbered paragraphs correctly`() {
        val response = """
            [1] First translation
            [2] Second translation
            [3] Third translation
        """.trimIndent()

        val result = parseTranslationResponse(response, 3)

        assertEquals(3, result.size)
        assertEquals("First translation", result[0])
        assertEquals("Second translation", result[1])
        assertEquals("Third translation", result[2])
    }

    @Test
    fun `parseTranslationResponse throws exception on mismatched count`() {
        val response = "[1] First translation"

        assertThrows<TranslationException> {
            parseTranslationResponse(response, 2)
        }
    }
}
```

### Integration Tests

```kotlin
class OpenAITranslationTest {
    @Test
    fun `translate with mock API returns success`() = runTest {
        // Mock OpenAI client response
        val mockResponse = ChatCompletion(
            id = "test-id",
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(
                        role = ChatMessage.Role.ASSISTANT,
                        content = "[1] Translated text"
                    )
                )
            )
        )

        // Test translation
        val result = client.translate(listOf("Original text"))

        assertTrue(result is AIClient.TranslationResult.Success)
    }
}
```

---

## Security Considerations

### Input Sanitization

```kotlin
private fun sanitizeInput(paragraphs: List<String>): List<String> {
    return paragraphs.map { paragraph ->
        paragraph
            .trim()
            .take(10000) // Max 10k chars per paragraph
    }
}
```

### Prompt Injection Prevention

- Use delimiters (numbered brackets) to separate instructions from content
- Clear instructions about expected format
- Validate response format before accepting

---

## Deployment Checklist

- [ ] Both provider implementations complete
- [ ] Error handling tested
- [ ] Response parsing validated
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Manual testing completed
- [ ] Documentation updated
- [ ] Code reviewed

---

## Success Criteria

1. ✅ Translation uses real AI provider API
2. ✅ Full article content translated in single API call
3. ✅ Paragraphs correctly indexed and mapped
4. ✅ Auto-translate triggers when enabled
5. ✅ Manual translation works via button
6. ✅ Error handling provides user-friendly messages
7. ✅ All tests passing
8. ✅ Code reviewed and approved

---

## Open Questions

**Q1**: Should we implement token estimation and truncation for MVP?

**A1**: No, for MVP we'll trust that most articles fit within context windows. Add warning if translation fails due to length.

**Q2**: Should auto-translate trigger immediately on article open or after content loads?

**A2**: After content loads to ensure smooth UI and proper paragraph extraction.

---

## Dependencies

**Existing**:
- `com.openai:openai-java:4.13.0`
- `com.anthropic:anthropic:2.11.1`

**New**: None

---

## References

- Research Report: `02-research-report.md`
- Code Assessment: `04-code-assessment.md`
- Requirements: `01-requirements.md`

---

**Status**: ✅ Ready for Implementation
