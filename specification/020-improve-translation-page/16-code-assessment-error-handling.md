# Code Assessment: Error Handling and Retry Mechanisms
**Date:** 2026-01-05
**Focus:** Translation error handling, retry logic, timeout handling
**Status:** Complete

## Assessment Scope

This assessment evaluates:
1. Current error handling mechanisms in translation flow
2. Existing retry patterns in the codebase
3. Timeout handling implementation
4. Partial response recovery capabilities

## Current State Analysis

### 1. Translation Error Handling

**Location:** `OpenAICompatibleClient.kt:207-211`, `AnthropicClient.kt:185-189`

```kotlin
override suspend fun translate(
    translatableTexts: List<TranslatableText>,
    targetLanguage: TranslationLanguage,
): AIClient.TranslationResult {
    return try {
        // ... API call and parsing
        val translatedParagraphs = parseTranslationResponse(
            translatedText,
            translatableTexts.size
        )
        AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            content = handleTranslationError(e)
        )
    }
}
```

**Findings:**
- ✅ Generic try-catch exists
- ❌ No retry logic
- ❌ No distinction between retryable and non-retryable errors
- ❌ Single attempt only
- ❌ No exponential backoff
- ❌ No partial recovery

### 2. Retry Logic in Codebase

#### Feed Syncing Retry Pattern

**Location:** `Repository.kt:877-882`, `SearchFeedViewModel.kt:96-103`

```kotlin
suspend fun setRetryAfterForFeedsWithBaseUrl(
    host: String,
    retryAfter: Instant,
) {
    feedStore.setRetryAfterForFeedsWithBaseUrl(host = host, retryAfter = retryAfter)
}

private suspend fun handleHttpError(httpError: HttpError) {
    httpError.retryAfterSeconds?.let { retryAfterSeconds ->
        repository.setRetryAfterForFeedsWithBaseUrl(
            host = URL(httpError.url).host,
            retryAfter = Instant.now().plusSeconds(retryAfterSeconds),
        )
    }
}
```

**Pattern Analysis:**
- ✅ Server-directed retry (HTTP 429 with Retry-After header)
- ❌ No client-side retry logic
- ❌ No exponential backoff implementation
- ❌ Retry is deferred to next sync cycle, not immediate

**Reusability for Translation:**
- **NOT directly reusable** - Feed sync uses long polling (retry later), translation needs immediate retry
- **Concept applicable** - Can implement similar "retry after delay" pattern but with immediate execution

### 3. Timeout Handling

**Location:** `OpenAICompatibleClient.kt:217`

```kotlin
private fun buildClient(): OpenAIClientAsync {
    val builder = OpenAIOkHttpClientAsync.builder()
        .apiKey(settings.key)
        .timeout(Duration.ofSeconds(settings.timeoutSeconds.toLong()))
    // ...
    return builder.build()
}
```

**Configuration:**
- Default: 30 seconds
- User configurable: 30-600 seconds
- Applied at: HTTP client level (covers both connection and read)

**Findings:**
- ✅ Configurable timeout
- ✅ Reasonable default (30s)
- ✅ Wide range available (30-600s)
- ❌ Single timeout for entire request
- ❌ No progressive timeout increase on retry
- ❌ No distinction between connection timeout vs read timeout
- ❌ No early warning for long-running requests

**Timeout Best Practices Assessment:**
| Practice | Status | Notes |
|----------|--------|-------|
| Configurable timeout | ✅ Implemented | 30-600s range |
| Connection timeout | ⚠️ Partial | Combined with read timeout |
| Read timeout | ⚠️ Partial | Combined with connection timeout |
| Progressive timeout on retry | ❌ Missing | Would improve success rate |
| Timeout per retry attempt | ❌ Missing | Same timeout used for all attempts |

### 4. Error Types and Handling

**Location:** `OpenAICompatibleClient.kt:430-478`, `AnthropicClient.kt:374-422`

```kotlin
private fun handleTranslationError(e: Throwable): String {
    return when (e) {
        is AIClientException -> e.message ?: "Translation error"
        is TimeoutException -> "Request timed out. Please try again."
        is HttpRequestException -> "Network error: ${e.message}"
        else -> "Translation failed: ${e.message}"
    }
}
```

**Findings:**
- ✅ Basic error type differentiation
- ✅ User-friendly messages
- ❌ No error classification (retryable vs non-retryable)
- ❌ No error context logging
- ❌ Generic messages don't guide user action

**Error Type Coverage:**
| Error Type | Currently Handled | Retryable? | Should Retry? |
|------------|-------------------|------------|---------------|
| Timeout | ✅ Yes | ✅ | ✅ (2-3 times) |
| Network error | ✅ Yes | ✅ | ✅ (2-3 times) |
| 5xx server error | ⚠️ Generic | ✅ | ✅ (2-3 times) |
| 429 rate limit | ⚠️ Generic | ✅ | ✅ (with backoff) |
| 4xx client error | ⚠️ Generic | ❌ | ❌ |
| Malformed JSON | ❌ AIClientException | ❌ | ❌ |
| Incomplete JSON | ❌ AIClientException | ❌ | ⚠️ (partial recovery) |
| API key error | ⚠️ Generic | ❌ | ❌ |

### 5. Partial Response Handling

**Current State:** NONE

**Location:** `parseTranslationResponse()` in both clients

```kotlin
if (arrayEnd == -1) {
    throw AIClientException("Translations array end not found")
}
```

**Findings:**
- ❌ No partial parsing capability
- ❌ All-or-nothing approach
- ❌ Complete data loss on incomplete response
- ❌ No graceful degradation

**Best Practice Example (what should exist):**
```kotlin
// Pseudocode for desired behavior
if (arrayEnd == -1) {
    // Try to parse partial response
    val partialTranslations = extractPartialTranslations(jsonContent)
    if (partialTranslations.isNotEmpty()) {
        // Fill gaps with original text
        val result = mergeWithOriginals(partialTranslations, translatableTexts)
        return TranslationResult.Partial(
            paragraphs = result,
            message = "Translated ${partialTranslations.size}/${translatableTexts.size} paragraphs"
        )
    }
    // Only throw if truly unrecoverable
    throw AIClientException("Could not parse any translations")
}
```

## Codebase Patterns Assessment

### Exception Handling Patterns

**Pattern 1: Generic catch with logging**
```kotlin
try {
    // operation
} catch (e: Exception) {
    Log.e(TAG, "Operation failed", e)
    return defaultValue
}
```
**Usage:** Database operations, file I/O
**Reusability:** ⚠️ Too generic for translation (needs retry)

**Pattern 2: Specific exception types**
```kotlin
try {
    // operation
} catch (e: IOException) {
    Log.e(TAG, "IO error", e)
} catch (e: SpecificException) {
    Log.e(TAG, "Specific error", e)
}
```
**Usage:** OPML parsing, feed parsing
**Reusability:** ✅ Good pattern for translation

**Pattern 3: Error result type**
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<T>()
}
```
**Usage:** Translation result (already implemented!)
**Reusability:** ✅ Perfect - extend with `Partial` state

### Retry Patterns

**Pattern 1: Server-directed retry**
```kotlin
if (httpError.retryAfterSeconds != null) {
    scheduleRetry(retryAfterSeconds)
}
```
**Usage:** Feed syncing
**Reusability:** ⚠️ Not suitable for translation (needs immediate retry)

**Pattern 2: No retry**
```kotlin
try {
    // operation
} catch (e: Exception) {
    return Error(e.message)
}
```
**Usage:** Most operations
**Reusability:** ❌ Not suitable for translation

**Missing Pattern:** Exponential backoff retry
```kotlin
// This pattern does NOT exist in the codebase
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 1000,
    operation: suspend () -> T
): T {
    var lastException: Exception? = null
    repeat(times) { attempt ->
        try {
            return operation()
        } catch (e: Exception) {
            lastException = e
            if (attempt < times - 1) {
                delay(initialDelay * (2.0.pow(attempt).toLong()))
            }
        }
    }
    throw lastException!!
}
```
**Required for:** Translation, summary, and other AI operations

## Gaps and Recommendations

### Gap 1: No Retry Logic
**Impact:** High
**Effort:** Medium
**Priority:** 1

**Recommendation:**
```kotlin
// Add to AIClient interface or as utility
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1000,
    isRetryable: (Throwable) -> Boolean = { true },
    operation: suspend () -> T
): T
```

**Usage:**
```kotlin
override suspend fun translate(...): TranslationResult {
    return retryWithBackoff(
        maxAttempts = 3,
        initialDelayMs = 1000,
        isRetryable = { e ->
            e is TimeoutException ||
            e is HttpRequestException ||
            (e is AIClientException && e.isRetryable)
        }
    ) {
        performTranslation(translatableTexts, targetLanguage)
    }
}
```

### Gap 2: No Partial Response Recovery
**Impact:** High
**Effort:** High
**Priority:** 2

**Recommendation:**
1. Add `TranslationResult.Partial` state
2. Implement `parsePartialResponse()` function
3. Merge with original text for missing translations
4. Show warning to user

### Gap 3: Insufficient Error Classification
**Impact:** Medium
**Effort:** Low
**Priority:** 3

**Recommendation:**
```kotlin
sealed class TranslationError {
    data class Retryable(
        val cause: Throwable,
        val canRetry: Boolean = true
    ) : TranslationError()

    data class NonRetryable(
        val cause: Throwable,
        val userAction: String? = null
    ) : TranslationError()

    data class Partial(
        val completed: Int,
        val total: Int,
        val cause: Throwable? = null
    ) : TranslationError()
}
```

### Gap 4: No Progressive Timeout
**Impact:** Medium
**Effort:** Low
**Priority:** 4

**Recommendation:**
```kotlin
// Increase timeout on each retry
val timeouts = listOf(30, 60, 90) // seconds
repeat(maxAttempts) { attempt ->
    val client = buildClient(timeouts[attempt])
    // ...
}
```

### Gap 5: No Telemetry
**Impact:** Low
**Effort:** Medium
**Priority:** 5

**Recommendation:**
- Track error frequency by type
- Monitor retry success rate
- Identify problematic content patterns
- Log with structured logging

## Implementation Complexity Assessment

| Feature | Complexity | Estimated Time | Dependencies |
|---------|------------|----------------|--------------|
| Retry with backoff | Medium | 2-3 hours | None |
| Partial response recovery | High | 4-6 hours | Parser enhancement |
| Error classification | Low | 1-2 hours | Retry logic |
| Progressive timeout | Low | 1 hour | Retry logic |
| Telemetry | Medium | 3-4 hours | Logging framework |

**Total Estimated Effort:** 11-16 hours

## Reusability Opportunities

### Reusable Components to Create

1. **RetryWithBackoff.kt**
   - Generic retry utility
   - Used by: translation, summary, future AI features
   - Location: `com.nononsenseapps.feeder.util`

2. **TranslationError.kt**
   - Error classification
   - Used by: translation, potentially summary
   - Location: `com.nononsenseapps.feeder.ai`

3. **PartialResponseParser.kt**
   - Partial JSON parsing
   - Used by: translation, potentially summary
   - Location: `com.nononsenseapps.feeder.ai`

## Code Quality Metrics

### Current Error Handling Score

| Metric | Score | Target |
|--------|-------|--------|
| Retry coverage | 0% | 80% |
| Error classification | 20% | 90% |
| Partial recovery | 0% | 70% |
| User feedback | 40% | 80% |
| **Overall** | **15%** | **80%** |

### Technical Debt

- **High Priority:** No retry logic for network operations
- **Medium Priority:** Incomplete error handling for AI features
- **Low Priority:** Generic error messages

## Testing Considerations

### Current Test Coverage
- ✅ Unit tests for parsing valid responses
- ❌ No tests for incomplete responses
- ❌ No tests for timeout scenarios
- ❌ No tests for retry logic
- ❌ No tests for partial recovery

### Required Test Cases
1. Complete valid response → Success
2. Truncated response (missing `]`) → Partial or retry
3. Network timeout → Retry with longer timeout
4. Malformed JSON → Graceful error
5. 5xx server error → Retry with backoff
6. 429 rate limit → Retry with exponential backoff
7. Empty array → Error or empty result

## Conclusion

The current implementation has basic error handling but lacks robustness for production use. The codebase shows good patterns for error handling in other areas (feed syncing, parsing) but these patterns are not applied to AI operations.

**Critical Missing Features:**
1. Retry logic with exponential backoff
2. Partial response recovery
3. Error classification for retry decisions

**Recommended Approach:**
1. Create reusable retry utility
2. Add error classification
3. Implement partial response recovery
4. Add comprehensive test coverage

**Risk Assessment:**
- **Current Risk:** High - Translations fail completely on transient issues
- **Post-Implementation Risk:** Low - Robust error handling with graceful degradation

