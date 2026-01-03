# Phase 6: Implementation Plan & Task List

**Date**: 2026-01-03
**Estimated Effort**: 8-12 hours
**Priority**: High

---

## Implementation Strategy

### Phased Approach

**Phase A**: Core Translation Implementation (4-6 hours)
- Implement `translate()` in both providers
- Add prompt building
- Add response parsing
- Add error handling

**Phase B**: Testing & Validation (2-3 hours)
- Unit tests for parsing logic
- Integration tests with mock APIs
- Manual testing scenarios

**Phase C**: Refinement & Polish (2-3 hours)
- Error message improvements
- Edge case handling
- Code review feedback
- Documentation updates

---

## Task Breakdown

### Task 1: OpenAI Provider Translation Implementation

**File**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

**Subtasks**:
1.1 Replace dummy `translate()` method implementation
1.2 Add `buildTranslationPrompt()` private method
1.3 Add `parseTranslationResponse()` private method
1.4 Add `handleTranslationError()` private method
1.5 Add `getTargetLanguage()` helper method

**Estimated Time**: 2-3 hours

**Implementation Details**:
```kotlin
// In OpenAICompatibleClient.kt, replace lines 158-172

override suspend fun translate(
    paragraphs: List<String>,
): AIClient.TranslationResult {
    if (paragraphs.isEmpty()) {
        return AIClient.TranslationResult.Error(
            content = "No translatable content found in this article"
        )
    }

    return try {
        val targetLanguage = repository.translationLanguage.first()
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

    if (translations.size != expectedParagraphs) {
        throw TranslationException(
            "Expected $expectedParagraphs paragraphs, got ${translations.size}"
        )
    }

    return translations
}

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

**Dependencies**: None

**Testing**:
- Test with valid paragraphs (should return Success)
- Test with empty list (should return Error)
- Test with API error (should return Error with message)
- Test response parsing with correct format
- Test response parsing with incorrect format (should throw exception)

---

### Task 2: Anthropic Provider Translation Implementation

**File**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

**Subtasks**:
2.1 Replace dummy `translate()` method implementation
2.2 Add `buildTranslationPrompt()` private method
2.3 Add `parseTranslationResponse()` private method
2.4 Add `handleTranslationError()` private method

**Estimated Time**: 2-3 hours

**Implementation Details**:
```kotlin
// In AnthropicClient.kt, replace lines 110-124

override suspend fun translate(
    paragraphs: List<String>,
): AIClient.TranslationResult {
    if (paragraphs.isEmpty()) {
        return AIClient.TranslationResult.Error(
            content = "No translatable content found in this article"
        )
    }

    return try {
        val targetLanguage = repository.translationLanguage.first()
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
            ?: throw TranslationException("Empty response from Anthropic API")

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

// buildTranslationPrompt, parseTranslationResponse, handleTranslationError
// Same as OpenAI implementation (can extract to shared utility if needed)
```

**Dependencies**: Task 1 (can reuse parsing logic)

**Testing**:
- Same test scenarios as Task 1
- Verify Anthropic-specific response handling

---

### Task 3: Add Repository Dependency to Clients

**Files**:
- `OpenAICompatibleClient.kt`
- `AnthropicClient.kt`

**Subtasks**:
3.1 Add Repository parameter to client constructors
3.2 Update AIClient.create() factory method
3.3 Update DI configuration

**Estimated Time**: 1 hour

**Implementation Details**:

**Current**:
```kotlin
class OpenAICompatibleClient(
    private val settings: OpenAISettings
) : AIClient
```

**Updated**:
```kotlin
class OpenAICompatibleClient(
    private val settings: OpenAISettings,
    private val repository: Repository  // Add this
) : AIClient
```

**Factory Update**:
```kotlin
// In AIClient.kt, companion object
fun create(settings: AISettings, repository: Repository): AIClient {
    return when (settings) {
        is AISettings.OpenAI ->
            OpenAICompatibleClient(settings.openaiSettings, repository)
        is AISettings.Anthropic ->
            AnthropicClient(settings.anthropicSettings, repository)
    }
}
```

**DI Configuration Update**:
```kotlin
// In appropriate DI module
bind<AIClient>() with provider {
    val settings = instance<Repository>().aiSettings
    val repository = instance<Repository>()
    AIClient.create(settings, repository)
}
```

**Testing**:
- Verify compilation
- Test translation with repository access
- Verify target language is retrieved correctly

---

### Task 4: Auto-Translate Trigger (Verify or Implement)

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Subtasks**:
4.1 Verify if auto-translate is already implemented
4.2 If not, add auto-translate trigger in init block
4.3 Test auto-translate behavior

**Estimated Time**: 1-2 hours (or 0 if already implemented)

**Implementation Details** (if needed):

```kotlin
// In ArticleViewModel.kt init block
init {
    // ... existing code ...

    // Auto-translate if enabled
    viewModelScope.launch {
        val enabled = repository.translationEnabled.first()
        val currentTranslation = translationState.value

        if (enabled && currentTranslation is TranslationState.Empty) {
            // Wait for article to load
            delay(500) // Small delay to ensure UI renders
            translate()
        }
    }
}
```

**Testing**:
- Test with auto-translate enabled (should trigger automatically)
- Test with auto-translate disabled (should not trigger)
- Test that manual translation button still works

---

### Task 5: Unit Tests

**Files** (new):
- `app/src/test/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClientTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/ai/provider/AnthropicClientTest.kt`

**Subtasks**:
5.1 Create test file structure
5.2 Implement response parsing tests
5.3 Implement error handling tests
5.4 Implement edge case tests

**Estimated Time**: 2-3 hours

**Test Cases**:

```kotlin
class TranslationParserTest {
    @Test
    fun `parseTranslationResponse extracts numbered paragraphs`() { }

    @Test
    fun `parseTranslationResponse handles special characters`() { }

    @Test
    fun `parseTranslationResponse throws on count mismatch`() { }

    @Test
    fun `parseTranslationResponse handles empty paragraphs`() { }

    @Test
    fun `handleTranslationError returns correct messages`() { }
}
```

**Testing**:
- All tests should pass
- Coverage > 80% for new code

---

### Task 6: Integration Tests

**Files** (new):
- `app/src/androidTest/java/com/nononsenseapps/feeder/ai/TranslationIntegrationTest.kt`

**Subtasks**:
6.1 Create integration test file
6.2 Set up mock API responses
6.3 Test end-to-end translation flow

**Estimated Time**: 1-2 hours

**Test Scenarios**:
- Full translation flow with mock API
- Error scenarios (timeout, rate limit)
- Paragraph count validation

---

### Task 7: Manual Testing

**Subtasks**:
7.1 Test with OpenAI provider
7.2 Test with Anthropic provider
7.3 Test auto-translate feature
7.4 Test manual translation
7.5 Test error scenarios

**Estimated Time**: 1-2 hours

**Test Cases**:
- Translate short article (3-5 paragraphs)
- Translate long article (20+ paragraphs)
- Translate article with special characters
- Translate with invalid API key
- Translate with network disabled
- Auto-translate on article open
- Manual translation via button

---

### Task 8: Code Review & Refinement

**Subtasks**:
8.1 Self-review code changes
8.2 Address any issues found
8.3 Optimize code if needed
8.4 Add missing documentation

**Estimated Time**: 1 hour

---

### Task 9: Documentation Updates

**Subtasks**:
9.1 Update CHANGELOG.md
9.2 Update any relevant README files
9.3 Add inline comments if needed

**Estimated Time**: 0.5 hours

---

## Dependencies Between Tasks

```
Task 1 (OpenAI Impl)
    ↓
Task 2 (Anthropic Impl) ← can reuse parsing logic
    ↓
Task 3 (Repository DI)
    ↓
Task 4 (Auto-translate)
    ↓
Task 5 (Unit Tests) ← can run in parallel with Task 6
Task 6 (Integration Tests)
    ↓
Task 7 (Manual Tests)
    ↓
Task 8 (Review)
    ↓
Task 9 (Docs)
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Repository access in clients | Add Repository parameter to constructors |
| Parsing logic duplication | Extract to shared utility class if needed |
| Auto-translate timing issues | Add delay and verify state before triggering |
| Test API costs | Use mock responses for most tests |

---

## Rollout Plan

1. **Alpha**: Developer testing only
2. **Beta**: Internal testing with real API keys
3. **Release**: Merge to main branch after approval

---

## Success Criteria

- [ ] All tasks completed
- [ ] All tests passing
- [ ] Manual testing successful
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] No regressions in existing functionality

---

## Total Estimated Effort: 8-12 hours

**Breakdown**:
- Core implementation: 4-6 hours
- Testing: 3-5 hours
- Review & docs: 1-2 hours

---

## Notes

- Tasks 1-4 are sequential
- Tasks 5-6 can run in parallel with 1-4 (test-first approach)
- Task 7 must wait for implementation completion
- Continuous testing recommended (test after each task)
