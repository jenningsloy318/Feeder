# Specification: Robust Translation Error Handling
**Date:** 2026-01-05
**Version:** 1.0
**Status:** Ready for Implementation

## Overview

This specification defines robust error handling mechanisms for the translation feature to handle incomplete responses, network failures, and other transient errors gracefully.

## Goals

1. **Reliability:** Handle transient errors without total failure
2. **User Experience:** Provide partial results when possible
3. **Resilience:** Recover from network issues and timeouts
4. **Transparency:** Clear error messages and status feedback

## Non-Goals

- Complete redesign of translation architecture
- Changes to LLM prompt engineering
- UI/UX changes beyond error messaging

## Requirements

### Functional Requirements

#### FR-1: Retry Logic with Exponential Backoff
**Priority:** P0 (Critical)

The system MUST retry failed translation requests with exponential backoff:

```kotlin
suspend fun translate(
    translatableTexts: List<TranslatableText>,
    targetLanguage: TranslationLanguage
): TranslationResult {
    return retryWithBackoff(
        maxAttempts = 3,
        initialDelayMs = 1000,
        backoffMultiplier = 2.0,
        isRetryable = ::isTranslationErrorRetryable
    ) {
        performTranslation(translatableTexts, targetLanguage)
    }
}

private fun isTranslationErrorRetryable(error: Throwable): Boolean {
    return when (error) {
        is TimeoutException -> true
        is HttpRequestException -> true
        is AIClientException -> error.isRetryable
        else -> false
    }
}
```

**Acceptance Criteria:**
- [ ] Retry logic implemented for translation
- [ ] Max 3 retry attempts
- [ ] Exponential backoff: 1s, 2s, 4s
- [ ] Only retryable errors trigger retry
- [ ] Non-retryable errors fail immediately

#### FR-2: Error Classification
**Priority:** P0 (Critical)

Errors MUST be classified as retryable or non-retryable:

```kotlin
sealed class TranslationError {
    data class Retryable(
        val type: RetryableType,
        val cause: Throwable,
        val message: String
    ) : TranslationError()

    data class NonRetryable(
        val type: NonRetryableType,
        val cause: Throwable,
        val userAction: String?
    ) : TranslationError()

    enum class RetryableType {
        TIMEOUT,
        NETWORK_ERROR,
        SERVER_ERROR_5XX,
        RATE_LIMIT_429,
        INCOMPLETE_RESPONSE
    }

    enum class NonRetryableType {
        MALFORMED_RESPONSE,
        API_KEY_ERROR,
        QUOTA_EXCEEDED,
        INVALID_REQUEST
    }
}
```

**Acceptance Criteria:**
- [ ] All errors classified
- [ ] Retryable errors identified correctly
- [ ] Non-retryable errors fail immediately
- [ ] Error type included in result

#### FR-3: Partial Response Recovery
**Priority:** P0 (Critical)

When responses are incomplete, the system MUST extract available translations:

```kotlin
private fun parseTranslationResponseWithRecovery(
    response: String,
    expectedCount: Int,
    originals: List<TranslatableText>
): List<String> {
    return try {
        // Try full parse first
        parseTranslationResponse(response, expectedCount)
    } catch (e: AIClientException) {
        if (e.message?.contains("array end not found") == true) {
            // Attempt partial recovery
            val partial = parsePartialTranslationResponse(response)
            if (partial.isNotEmpty()) {
                // Fill gaps with original text
                mergeWithOriginals(partial, originals)
            } else {
                throw e
            }
        } else {
            throw e
        }
    }
}

private fun parsePartialTranslationResponse(
    response: String
): Map<Int, String> {
    // Extract available {"index": N, "text": "..."} objects
    // even if array is incomplete
    // ...
}

private fun mergeWithOriginals(
    partial: Map<Int, String>,
    originals: List<TranslatableText>
): List<String> {
    return originals.mapIndexed { index, original ->
        partial[index] ?: original.text
    }
}
```

**Acceptance Criteria:**
- [ ] Parse incomplete JSON arrays
- [ ] Extract available translation objects
- [ ] Fill missing indices with original text
- [ ] Return result even if partially complete
- [ ] Only throw if truly unrecoverable

#### FR-4: Progressive Timeout
**Priority:** P1 (High)

Timeout MUST increase on each retry attempt:

```kotlin
data class RetryConfig(
    val maxAttempts: Int = 3,
    val timeouts: List<Long> = listOf(30L, 60L, 90L) // seconds
)

suspend fun <T> retryWithBackoff(
    config: RetryConfig,
    operation: suspend (timeout: Long) -> T
): T {
    var lastException: Exception? = null
    repeat(config.maxAttempts) { attempt ->
        try {
            return operation(config.timeouts[attempt])
        } catch (e: Exception) {
            lastException = e
            if (attempt < config.maxAttempts - 1) {
                delay(calculateBackoff(attempt))
            }
        }
    }
    throw lastException!!
}
```

**Acceptance Criteria:**
- [ ] First attempt: use configured timeout (default 30s)
- [ ] Second attempt: 2x timeout (60s)
- [ ] Third attempt: 3x timeout (90s)
- [ ] Respect user-configured max (600s)

#### FR-5: Enhanced Error Messages
**Priority:** P1 (High)

Error messages MUST guide user action:

```kotlin
sealed class TranslationResult {
    data class Success(val paragraphs: List<String>) : TranslationResult()

    data class Partial(
        val paragraphs: List<String>,
        val completedCount: Int,
        val totalCount: Int,
        val message: String
    ) : TranslationResult()

    data class Error(
        val message: String,
        val type: ErrorType,
        val canRetry: Boolean,
        val userAction: String?
    ) : TranslationResult()
}

// Example messages:
// - "Translation timed out. Retrying with longer timeout..."
// - "Network error. Checking connection..."
// - "Translated 3 of 5 paragraphs. The rest were not translated due to incomplete response."
// - "API quota exceeded. Please check your plan."
```

**Acceptance Criteria:**
- [ ] Error messages are user-friendly
- [ ] Retryable errors indicate retry in progress
- [ ] Non-retryable errors suggest user action
- [ ] Partial results show completion ratio

### Non-Functional Requirements

#### NFR-1: Performance
- Retry overhead MUST NOT exceed 10 seconds for transient failures
- Partial parsing MUST complete within 100ms for typical responses

#### NFR-2: Reliability
- Success rate for retryable errors MUST be > 80%
- Partial recovery MUST succeed for > 90% of incomplete responses

#### NFR-3: Maintainability
- Retry logic MUST be reusable for other AI features (summary, etc.)
- Error handling MUST be testable in isolation

## Architecture Design

### Component Structure

```
com.nononsenseapps.feeder.ai
├── util
│   └── RetryWithBackoff.kt          // Generic retry utility
├── provider
│   ├── OpenAICompatibleClient.kt    // Enhanced with retry
│   ├── AnthropicClient.kt           // Enhanced with retry
│   └── TranslationParser.kt         // Extracted parser logic
├── model
│   ├── TranslationError.kt          // Error classification
│   └── TranslationResult.kt         // Enhanced result types
└── AIClient.kt                      // Interface updates
```

### Data Flow

```
User Request
    ↓
translate() with retry wrapper
    ↓
┌─────────────────────────────────┐
│  Attempt 1: timeout=30s         │
│  ├─ Success → return            │
│  ├─ Retryable error → wait 1s   │
│  └─ Non-retryable → fail        │
└─────────────────────────────────┘
    ↓ (if retryable)
┌─────────────────────────────────┐
│  Attempt 2: timeout=60s         │
│  ├─ Success → return            │
│  ├─ Retryable error → wait 2s   │
│  └─ Non-retryable → fail        │
└─────────────────────────────────┘
    ↓ (if retryable)
┌─────────────────────────────────┐
│  Attempt 3: timeout=90s         │
│  ├─ Success → return            │
│  ├─ Incomplete → partial recovery│
│  └─ Any error → fail            │
└─────────────────────────────────┘
    ↓
Return result (Success/Partial/Error)
```

### State Machine

```
[Start]
   ↓
[Parse Response]
   ├─→ [Complete JSON] → [Success]
   ├─→ [Incomplete JSON]
   │     ├─→ [Extract Available] → [Merge w/ Originals] → [Partial Success]
   │     └─→ [None Extractable] → [Error]
   └─→ [Malformed JSON] → [Error]

[Error Classification]
   ├─→ [Retryable] → [Retry with Backoff]
   │                    └─→ [Max Attempts Reached] → [Error]
   └─→ [Non-Retryable] → [Error]
```

## Implementation Details

### 1. Retry Utility

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/util/RetryWithBackoff.kt`

```kotlin
package com.nononsenseapps.feeder.ai.util

import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Duration = 1.seconds,
    backoffMultiplier: Double = 2.0,
    isRetryable: (Throwable) -> Boolean = { true },
    operation: suspend (attempt: Int) -> T
): T {
    var lastException: Throwable? = null

    repeat(maxAttempts) { attempt ->
        try {
            return operation(attempt)
        } catch (e: Throwable) {
            lastException = e

            // Don't retry on non-retryable errors
            if (!isRetryable(e)) {
                throw e
            }

            // Don't wait after last attempt
            if (attempt < maxAttempts - 1) {
                val delayMs = initialDelay
                    .times(backoffMultiplier.pow(attempt))
                    .toLong(DurationUnit.MILLISECONDS)
                    .toLong()

                delay(delayMs)
            }
        }
    }

    throw lastException!!
}
```

### 2. Error Classification

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/TranslationError.kt`

```kotlin
package com.nononsenseapps.feeder.ai.model

sealed class TranslationError : Exception() {
    abstract val type: ErrorType
    abstract val isRetryable: Boolean
    abstract val userMessage: String
    abstract val technicalMessage: String

    data class Timeout(
        override val cause: Throwable? = null
    ) : TranslationError() {
        override val type = ErrorType.TIMEOUT
        override val isRetryable = true
        override val userMessage = "Translation request timed out"
        override val technicalMessage = "Request exceeded timeout limit"
    }

    data class NetworkError(
        override val cause: Throwable? = null
    ) : TranslationError() {
        override val type = ErrorType.NETWORK_ERROR
        override val isRetryable = true
        override val userMessage = "Network error occurred"
        override val technicalMessage = "Failed to connect to translation service"
    }

    data class IncompleteResponse(
        val receivedCount: Int,
        val expectedCount: Int,
        override val cause: Throwable? = null
    ) : TranslationError() {
        override val type = ErrorType.INCOMPLETE_RESPONSE
        override val isRetryable = false // Handled by partial recovery
        override val userMessage =
            "Received incomplete translation ($receivedCount/$expectedCount)"
        override val technicalMessage =
            "Response parsing failed: array end not found"
    }

    data class MalformedResponse(
        override val cause: Throwable? = null
    ) : TranslationError() {
        override val type = ErrorType.MALFORMED_RESPONSE
        override val isRetryable = false
        override val userMessage = "Invalid response format"
        override val technicalMessage = "Response parsing failed: malformed JSON"
    }

    enum class ErrorType {
        TIMEOUT,
        NETWORK_ERROR,
        SERVER_ERROR_5XX,
        RATE_LIMIT_429,
        INCOMPLETE_RESPONSE,
        MALFORMED_RESPONSE,
        API_KEY_ERROR,
        QUOTA_EXCEEDED,
        INVALID_REQUEST
    }
}
```

### 3. Enhanced Translation Result

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/TranslationResult.kt`

```kotlin
package com.nononsenseapps.feeder.ai

sealed class TranslationResult {
    data class Success(
        val paragraphs: List<String>
    ) : TranslationResult()

    data class Partial(
        val paragraphs: List<String>,
        val completedCount: Int,
        val totalCount: Int,
        val message: String
    ) : TranslationResult() {
        val completionRatio: Float
            get() = completedCount.toFloat() / totalCount.toFloat()

        val isMostlyComplete: Boolean
            get() = completionRatio >= 0.5f
    }

    data class Error(
        val message: String,
        val type: TranslationError.ErrorType,
        val canRetry: Boolean,
        val retryAttempted: Boolean = false,
        val userAction: String? = null
    ) : TranslationResult()
}
```

### 4. Partial Response Parser

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/TranslationParser.kt`

```kotlin
package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.model.TranslationError

object TranslationParser {
    /**
     * Parse translation response with partial recovery.
     *
     * Attempts to parse complete response first.
     * If parsing fails, attempts to extract partial translations.
     */
    fun parseWithRecovery(
        response: String,
        expectedCount: Int,
        originals: List<TranslatableText>
    ): ParseResult {
        return try {
            // Try complete parse
            val translations = parseComplete(response, expectedCount)
            ParseResult.Complete(translations)
        } catch (e: TranslationError.MalformedResponse) {
            // Attempt partial recovery
            val partial = parsePartial(response)
            if (partial.isNotEmpty()) {
                val merged = mergeWithOriginals(partial, originals)
                ParseResult.Partial(
                    translations = merged,
                    receivedCount = partial.size,
                    expectedCount = expectedCount
                )
            } else {
                ParseResult.Error(e)
            }
        } catch (e: TranslationError.IncompleteResponse) {
            // Attempt partial recovery
            val partial = parsePartial(response)
            if (partial.isNotEmpty()) {
                val merged = mergeWithOriginals(partial, originals)
                ParseResult.Partial(
                    translations = merged,
                    receivedCount = partial.size,
                    expectedCount = expectedCount
                )
            } else {
                ParseResult.Error(e)
            }
        }
    }

    private fun parseComplete(
        response: String,
        expectedCount: Int
    ): List<String> {
        // Existing parseTranslationResponse logic
        // ... (current implementation)
    }

    private fun parsePartial(response: String): Map<Int, String> {
        val translations = mutableMapOf<Int, String>()

        // Extract all {"index": N, "text": "..."} patterns
        // even if array is incomplete or malformed
        val pattern = """"index"\s*:\s*(\d+)\s*,\s*"text"\s*:\s*"([^"]*(?:\\"[^"]*)*)"""".toRegex()

        pattern.findAll(response).forEach { match ->
            val index = match.groupValues[1].toIntOrNull()
            val text = match.groupValues[2]
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")

            if (index != null) {
                translations[index] = text
            }
        }

        return translations
    }

    private fun mergeWithOriginals(
        partial: Map<Int, String>,
        originals: List<TranslatableText>
    ): List<String> {
        return originals.mapIndexed { index, original ->
            partial[index] ?: original.text
        }
    }

    sealed class ParseResult {
        data class Complete(val translations: List<String>) : ParseResult()
        data class Partial(
            val translations: List<String>,
            val receivedCount: Int,
            val expectedCount: Int
        ) : ParseResult()
        data class Error(val error: TranslationError) : ParseResult()
    }
}
```

### 5. Enhanced Client Implementation

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

```kotlin
override suspend fun translate(
    translatableTexts: List<TranslatableText>,
    targetLanguage: TranslationLanguage
): AIClient.TranslationResult {
    if (translatableTexts.isEmpty()) {
        return AIClient.TranslationResult.Error(
            content = "No translatable content found in this article"
        )
    }

    return try {
        retryWithBackoff(
            maxAttempts = 3,
            isRetryable = ::isTranslationErrorRetryable
        ) { attempt ->
            val timeout = getTimeoutForAttempt(attempt)
            val result = performTranslationWithTimeout(
                translatableTexts,
                targetLanguage,
                timeout
            )

            when (result) {
                is TranslationParser.ParseResult.Complete ->
                    AIClient.TranslationResult.Success(result.translations)

                is TranslationParser.ParseResult.Partial ->
                    AIClient.TranslationResult.Partial(
                        paragraphs = result.translations,
                        completedCount = result.receivedCount,
                        totalCount = result.expectedCount,
                        message = "Translated ${result.receivedCount} of ${result.expectedCount} paragraphs"
                    )

                is TranslationParser.ParseResult.Error ->
                    throw result.error
            }
        }
    } catch (e: TranslationError) {
        AIClient.TranslationResult.Error(
            message = e.userMessage,
            type = e.type,
            canRetry = e.isRetryable,
            userAction = getUserActionForError(e)
        )
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            message = handleTranslationError(e),
            type = TranslationError.ErrorType.INVALID_REQUEST,
            canRetry = false
        )
    }
}

private fun isTranslationErrorRetryable(error: Throwable): Boolean {
    return when (error) {
        is TranslationError.Timeout,
        is TranslationError.NetworkError,
        is TranslationError.IncompleteResponse -> true
        else -> false
    }
}

private fun getTimeoutForAttempt(attempt: Int): Long {
    // Progressive timeout: 30s, 60s, 90s
    val baseTimeout = settings.timeoutSeconds.toLong()
    return (baseTimeout * (attempt + 1)).coerceAtMost(600)
}

private fun getUserActionForError(error: TranslationError): String? {
    return when (error) {
        is TranslationError.Timeout ->
            "Check your internet connection and try again"
        is TranslationError.NetworkError ->
            "Check your network connection"
        is TranslationError.MalformedResponse ->
            "Try translating less text at once"
        else -> null
    }
}
```

## Test Cases

### Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ai/util/RetryWithBackoffTest.kt`

```kotlin
class RetryWithBackoffTest {
    @Test
    fun `success on first attempt does not retry`() = runTest {
        var attempts = 0
        val result = retryWithBackoff(maxAttempts = 3) {
            attempts++
            "success"
        }
        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `retryable error triggers retry`() = runTest {
        var attempts = 0
        val result = retryWithBackoff(
            maxAttempts = 3,
            isRetryable = { true }
        ) {
            attempts++
            if (attempts < 2) throw RuntimeException("fail")
            "success"
        }
        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `non-retryable error fails immediately`() = runTest {
        var attempts = 0
        assertThrows<IllegalStateException> {
            retryWithBackoff(
                maxAttempts = 3,
                isRetryable = { e -> e !is IllegalStateException }
            ) {
                attempts++
                throw IllegalStateException("fail")
            }
        }
        assertEquals(1, attempts)
    }

    @Test
    fun `max attempts reached throws last exception`() = runTest {
        var attempts = 0
        assertThrows<RuntimeException> {
            retryWithBackoff(
                maxAttempts = 2,
                isRetryable = { true }
            ) {
                attempts++
                throw RuntimeException("fail")
            }
        }
        assertEquals(2, attempts)
    }
}
```

**File:** `app/src/test/java/com/nononsenseapps/feeder/ai/provider/TranslationParserTest.kt`

```kotlin
class TranslationParserTest {
    @Test
    fun `parse complete valid response`() {
        val response = """
            {"translations": [
                {"index": 0, "text": "First"},
                {"index": 1, "text": "Second"}
            ]}
        """.trimIndent()

        val originals = listOf(
            TranslatableText("First original", emptyList()),
            TranslatableText("Second original", emptyList())
        )

        val result = TranslationParser.parseWithRecovery(
            response,
            expectedCount = 2,
            originals = originals
        )

        assertTrue(result is TranslationParser.ParseResult.Complete)
        assertEquals(listOf("First", "Second"),
            (result as TranslationParser.ParseResult.Complete).translations)
    }

    @Test
    fun `parse incomplete response recovers partial translations`() {
        val response = """
            {"translations": [
                {"index": 0, "text": "First"},
                {"index": 1, "text": "Secon...
        """.trimIndent()

        val originals = listOf(
            TranslatableText("First original", emptyList()),
            TranslatableText("Second original", emptyList())
        )

        val result = TranslationParser.parseWithRecovery(
            response,
            expectedCount = 2,
            originals = originals
        )

        assertTrue(result is TranslationParser.ParseResult.Partial)
        val partial = result as TranslationParser.ParseResult.Partial
        assertEquals(listOf("First", "Second original"), partial.translations)
        assertEquals(1, partial.receivedCount)
        assertEquals(2, partial.expectedCount)
    }

    @Test
    fun `parse malformed response falls back to original text`() {
        val response = "Invalid JSON {{{"

        val originals = listOf(
            TranslatableText("Original 1", emptyList()),
            TranslatableText("Original 2", emptyList())
        )

        val result = TranslationParser.parseWithRecovery(
            response,
            expectedCount = 2,
            originals = originals
        )

        // Either error or partial with all originals
        assertTrue(
            result is TranslationParser.ParseResult.Error ||
            (result is TranslationParser.ParseResult.Partial &&
             result.translations == listOf("Original 1", "Original 2"))
        )
    }

    @Test
    fun `empty response returns error`() {
        val response = ""
        val originals = listOf(TranslatableText("Original", emptyList()))

        val result = TranslationParser.parseWithRecovery(
            response,
            expectedCount = 1,
            originals = originals
        )

        assertTrue(result is TranslationParser.ParseResult.Error)
    }
}
```

### Integration Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ai/TranslationErrorHandlingTest.kt`

```kotlin
@ExperimentalCoroutinesApi
class TranslationErrorHandlingTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `network timeout triggers retry with longer timeout`() = runTest {
        // Mock client that times out first attempt, succeeds second
        val mockClient = createMockClient(
            responses = listOf(
                Result.failure(TimeoutException()),
                Result.success(mockValidResponse())
            )
        )

        val result = mockClient.translate(
            translatableTexts = listOf(TranslatableText("Test", emptyList())),
            targetLanguage = TranslationLanguage.SPANISH
        )

        assertTrue(result is AIClient.TranslationResult.Success)
        verify(mockClient, times(2)).performTranslation(any(), any())
    }

    @Test
    fun `incomplete response returns partial result with fallback`() = runTest {
        val mockClient = createMockClient(
            responses = listOf(Result.success(mockIncompleteResponse()))
        )

        val result = mockClient.translate(
            translatableTexts = listOf(
                TranslatableText("Original 1", emptyList()),
                TranslatableText("Original 2", emptyList())
            ),
            targetLanguage = TranslationLanguage.SPANISH
        )

        assertTrue(result is AIClient.TranslationResult.Partial)
        val partial = result as AIClient.TranslationResult.Partial
        assertEquals(1, partial.completedCount)
        assertEquals(2, partial.totalCount)
    }
}
```

## Rollout Plan

### Phase 1: Core Retry Logic (Week 1)
- Implement `RetryWithBackoff.kt`
- Add error classification
- Update `OpenAICompatibleClient` with retry
- Add unit tests

### Phase 2: Partial Recovery (Week 2)
- Implement `TranslationParser.kt`
- Add partial response parsing
- Update `TranslationResult` with `Partial` state
- Add unit tests

### Phase 3: Integration & Testing (Week 3)
- Integration tests
- Manual testing with real APIs
- Performance testing
- Bug fixes

### Phase 4: Documentation & Cleanup (Week 4)
- Update documentation
- Code review
- Merge to main branch

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Translation success rate | ~85% | >95% |
| Timeout recovery rate | 0% | >80% |
| Partial recovery success | N/A | >90% |
| User-reported translation failures | ~15% | <5% |

## Risks and Mitigations

### Risk 1: Increased latency due to retries
**Mitigation:** Progressive timeout, cap max attempts at 3

### Risk 2: Partial results confuse users
**Mitigation:** Clear messaging about completion status

### Risk 3: Retry loop on persistent errors
**Mitigation:** Error classification, max 3 attempts

### Risk 4: Increased API usage and costs
**Mitigation:** Only retry retryable errors, log retry patterns

## Future Enhancements

1. **Retry statistics tracking** for monitoring
2. **Adaptive timeout** based on content length
3. **User-configurable retry behavior**
4. **Offline translation queue** with retry on reconnect
5. **Translation cache** to avoid redundant requests

## References

- [Debug Analysis](./15-debug-analysis-parsing-error.md)
- [Code Assessment](./16-code-assessment-error-handling.md)
- Original spec-020 translation feature specification

