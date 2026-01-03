# Phase 3: Research Report - AI Translation Implementation

**Date**: 2026-01-03
**Phase**: 3 - Research
**Status**: Complete

---

## Executive Summary

This research document consolidates best practices for implementing AI-powered translation using OpenAI-compatible and Anthropic Claude APIs. The focus is on efficient paragraph-by-paragraph translation with proper indexing.

---

## Key Findings

### 1. Translation Prompt Engineering

**Best Practice**: Use clear, structured prompts with numbered paragraphs to maintain paragraph boundaries.

**Effective Prompt Pattern**:
```kotlin
"""
You are a professional translator specializing in accurate translations.

Translate the following article paragraphs to ${targetLanguage}.
Maintain paragraph structure and number each paragraph in your response:

[1] First paragraph text here...
[2] Second paragraph text here...
[3] Third paragraph text here...

Provide translations in the same format:
[1] Translated first paragraph...
[2] Translated second paragraph...
[3] Translated third paragraph...

Guidelines:
- Translate ONLY the content between numbered markers
- Maintain original formatting and structure
- Preserve the meaning and tone of the original text
- Use natural, fluent ${targetLanguage} expressions
- Return only the translation with numbered markers
"""
```

**Key Elements**:
- Clear role definition
- Explicit output format with numbered brackets `[1]`, `[2]`, etc.
- Instructions to maintain structure
- Language specification
- Only translate requested content

---

### 2. Single API Call Strategy

**Finding**: Sending all paragraphs in one request is the most efficient approach.

**Advantages**:
- Reduces API call overhead
- Maintains context across paragraphs
- Faster than paragraph-by-paragraph calls
- More cost-effective

**Implementation Pattern**:
```kotlin
suspend fun translate(paragraphs: List<String>): TranslationResult {
    // Build prompt with all paragraphs numbered
    val prompt = buildTranslationPrompt(paragraphs, targetLanguage)

    // Single API call
    val response = callChatCompletion(prompt)

    // Parse response to extract numbered translations
    val translatedParagraphs = parseTranslationResponse(response, paragraphs.size)

    return TranslationResult.Success(translatedParagraphs)
}
```

---

### 3. Response Parsing Strategies

**Strategy A**: Numbered bracket format `[1]`, `[2]`
- Most reliable for structured output
- Easy to parse with regex
- Clear paragraph boundaries

**Regex Pattern**:
```kotlin
val paragraphPattern = Regex("\\[(\\d+)\\]\\s*(.+?)(?=\\[\\d+\\]|$)", RegexOption.DOT_MATCHES_ALL)
val matches = paragraphPattern.findAll(response)
val translations = matches
    .sortedBy { it.groupValues[1].toInt() }
    .map { it.groupValues[2].trim() }
```

**Strategy B**: Delimiter-based (JSON, XML, custom tags)
- More structured but potentially verbose
- JSON can be problematic with special characters
- XML tags reliable but add overhead

**Recommendation**: Use numbered bracket format `[N]` for simplicity and reliability.

---

### 4. OpenAI API Integration

**Endpoint**: `POST https://api.openai.com/v1/chat/completions`

**Request Format**:
```kotlin
val params = ChatCompletionCreateParams.builder()
    .model(settings.modelId)
    .messages(
        listOf(
            ChatMessage(
                role = ChatMessage.Role.USER,
                content = prompt
            )
        )
    )
    .temperature(0.3)  // Lower for more consistent translations
    .build()

val response = client.chat.completions.create(params)
```

**Key Parameters**:
- `temperature`: 0.0-0.4 for translation (lower = more consistent)
- `model`: gpt-4o, gpt-4o-mini for best quality/price ratio
- `max_tokens`: Calculate based on input (~2x input tokens for translation)

---

### 5. Anthropic Claude API Integration

**Endpoint**: `POST https://api.anthropic.com/v1/messages`

**Request Format**:
```kotlin
val message = anthropic.messages.create(
    model = "claude-3-5-sonnet-20241022",
    max_tokens = 4096,
    messages = listOf(
        MessageParam(
            role = "user",
            content = prompt
        )
    )
)
```

**Key Parameters**:
- `model`: claude-3-5-sonnet-20241022 or claude-3-haiku-20240307
- `max_tokens`: Sufficient for translated output
- `temperature`: 0.3 for translation consistency

**Claude-Specific Considerations**:
- Anthropic uses a different message format
- System messages handled separately
- Response structure differs from OpenAI

---

### 6. Error Handling Patterns

**Common Errors**:
1. **Network timeout**: Implement retry with exponential backoff
2. **Rate limit exceeded**: Queue requests, show user-friendly message
3. **Invalid API key**: Clear error message directing to settings
4. **Insufficient quota**: Inform user about API limits
5. **Malformed response**: Fallback to partial translation or error

**Error Handling Strategy**:
```kotlin
try {
    val response = client.chat.completions.create(params)
    return parseResponse(response)
} catch (e: OpenAIException) {
    when {
        e.message?.contains("rate limit") == true -> {
            return TranslationResult.Error("Rate limit exceeded. Please try again later.")
        }
        e.message?.contains("invalid API key") == true -> {
            return TranslationResult.Error("Invalid API key. Check your AI provider settings.")
        }
        e is SocketTimeoutException -> {
            return TranslationResult.Error("Translation timed out. Please check your connection.")
        }
        else -> {
            return TranslationResult.Error("Translation failed: ${e.message}")
        }
    }
}
```

---

### 7. Context Window Considerations

**Finding**: Most articles fit within context windows, but very long articles need special handling.

**Provider Context Limits**:
- GPT-4o: 128K tokens (~100K words)
- GPT-4o-mini: 128K tokens
- Claude 3.5 Sonnet: 200K tokens
- Claude 3 Haiku: 200K tokens

**Estimation**:
- 1 token ≈ 0.75 words (English)
- 10-20 paragraphs ≈ 500-1000 words ≈ 700-1400 tokens
- Translation response typically 2x input tokens

**Strategy**:
- For MVP: Truncate if exceeds ~80% of context window
- Show message: "Article too long. Showing partial translation."
- Future enhancement: Implement chunking with overlap

---

### 8. Translation Quality Optimization

**Best Practices**:

1. **Temperature Settings**:
   - Use 0.0-0.3 for consistent, accurate translations
   - Higher temperatures (0.5+) for more creative interpretations

2. **Prompt Engineering**:
   - Specify target language clearly
   - Include context about article type (news, blog, etc.)
   - Request "natural, fluent expressions" for better quality

3. **Model Selection**:
   - GPT-4o: Best quality, higher cost
   - GPT-4o-mini: Good quality, lower cost
   - Claude 3.5 Sonnet: Excellent quality
   - Claude 3 Haiku: Fast, good quality

4. **Quality Checks**:
   - Verify paragraph count matches
   - Check for missing or extra paragraphs
   - Validate no empty translations

---

### 9. Performance Optimization

**Caching Strategy**:
```kotlin
// Session-level caching (already in ViewModel)
private val translationCache = mutableMapOf<String, TranslationResult>()

suspend fun translate(articleId: String, paragraphs: List<String>): TranslationResult {
    // Check cache first
    cache[articleId]?.let { return it }

    // Perform translation
    val result = performTranslation(paragraphs)

    // Cache result
    cache[articleId] = result

    return result
}
```

**Request Optimization**:
- Batch all paragraphs into single request
- Use streaming responses for better UX (future)
- Implement request queuing to prevent overwhelming API

---

### 10. User Experience Patterns

**Loading States**:
- Show progress indicator during translation
- Display "Translating paragraph X of Y..." for long articles
- Maintain readable UI while translation loads

**Error Recovery**:
- Retry button on error
- Clear error messages
- Fallback to original text if translation fails

**Display Pattern**:
```kotlin
// Paragraph-by-paragraph display
LazyColumn {
    items(originalParagraphs.size) { index ->
        Column {
            // Original paragraph
            Text(originalParagraphs[index])

            // Translated paragraph (if available)
            translationState.value?.let { state ->
                when (state) {
                    is TranslationState.Result.Success -> {
                        Text(state.paragraphs[index])
                    }
                    is TranslationState.Result.Error -> {
                        Text("Translation failed", color = Color.Red)
                    }
                    else -> {}
                }
            }
        }
    }
}
```

---

## Recommended Implementation Approach

### Step 1: Build Translation Prompt

```kotlin
private fun buildTranslationPrompt(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): String {
    val numberedParagraphs = paragraphs.mapIndexed { index, text ->
        "[${index + 1}] $text"
    }.joinToString("\n\n")

    return """
        You are a professional translator. Translate the following article to ${targetLanguage.displayName}.

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

### Step 2: Parse Translation Response

```kotlin
private fun parseTranslationResponse(
    response: String,
    expectedParagraphs: Int
): List<String> {
    val paragraphPattern = Regex("\\[(\\d+)\\]\\s*(.+?)(?=\\[\\d+\\]|\\Z)", RegexOption.DOT_MATCHES_ALL)
    val matches = paragraphPattern.findAll(response)

    val translations = matches
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

### Step 3: Call Provider API

```kotlin
// OpenAI-compatible
override suspend fun translate(paragraphs: List<String>): AIClient.TranslationResult {
    return try {
        val prompt = buildTranslationPrompt(paragraphs, targetLanguage)
        val params = ChatCompletionCreateParams.builder()
            .model(settings.modelId)
            .temperature(0.3)
            .messages(listOf(ChatMessage(ChatMessage.Role.USER, prompt)))
            .build()

        val response = client.chat.completions.create(params)
        val translatedText = response.choices.first().message.content
        val translatedParagraphs = parseTranslationResponse(translatedText, paragraphs.size)

        AIClient.TranslationResult.Success(translatedParagraphs)
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(handleTranslationError(e))
    }
}

// Anthropic
override suspend fun translate(paragraphs: List<String>): AIClient.TranslationResult {
    return try {
        val prompt = buildTranslationPrompt(paragraphs, targetLanguage)
        val message = anthropic.messages.create(
            model = AnthropicModel.CLAUDE_3_5_SONNET_20241022,
            maxTokens = 8192,
            messages = listOf(MessageParam(MessageRole.USER, prompt))
        )

        val translatedText = message.content.first().text
        val translatedParagraphs = parseTranslationResponse(translatedText, paragraphs.size)

        AIClient.TranslationResult.Success(translatedParagraphs)
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(handleTranslationError(e))
    }
}
```

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| API rate limits | High | Implement exponential backoff, queue requests |
| Context window overflow | Medium | Detect and truncate with user notification |
| Malformed response | Medium | Robust parsing with validation, retry logic |
| Poor translation quality | Medium | Use temperature 0.3, quality model selection |
| Increased API costs | Low | Single API call, session caching, warn users |

---

## Technology Stack

**Providers**:
- OpenAI GPT-4o / GPT-4o-mini (recommended)
- Anthropic Claude 3.5 Sonnet / Haiku
- OpenAI-compatible endpoints (Azure, local models)

**Libraries**:
- openai-java SDK (version 4.13.0)
- anthropic-kotlin SDK
- Kotlin Coroutines for async operations

---

## References

- OpenAI Prompt Engineering Guide: https://platform.openai.com/docs/guides/prompt-engineering
- Anthropic Prompt Engineering: https://platform.claude.com/docs/en/build-with-claude/prompt-engineering
- Translation Best Practices: Research from various open-source translation projects

---

## Conclusion

The research confirms that:
1. **Single API call with paragraph indexing** is the optimal approach
2. **Numbered bracket format [N]** provides reliable parsing
3. **Temperature 0.3** balances consistency and quality
4. **GPT-4o-mini or Claude 3 Haiku** offer best quality/price ratio
5. **Robust error handling** is critical for user experience

**Ready for Phase 5**: Code Assessment
