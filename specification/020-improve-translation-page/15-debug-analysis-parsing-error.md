# Debug Analysis: Translation Parsing Error
**Date:** 2026-01-05
**Error:** "Translations array end not found"
**Status:** Complete

## Executive Summary

Translation parsing fails when LLM responses are incomplete or truncated. The current parser expects well-formed JSON arrays but throws an exception when the response is cut off mid-transmission, resulting in total failure instead of graceful degradation or recovery.

## Root Cause Analysis

### 1. Error Location

**Files Affected:**
- `OpenAICompatibleClient.kt:393` - throws `AIClientException("Translations array end not found")`
- `AnthropicClient.kt:340` - throws `AIClientException("Translations array end not found")`

**Function:** `parseTranslationResponse()`

### 2. Parsing Logic Analysis

The parser uses a bracket-matching algorithm that:

```kotlin
// Find matching closing bracket
var depth = 0
var inString = false
var escaped = false
var arrayEnd = -1

for (i in arrayStart until jsonContent.length) {
    val c = jsonContent[i]

    if (escaped) {
        escaped = false
        continue
    }

    when (c) {
        '\\' -> escaped = true
        '"' -> inString = !inString
        '[', '{' -> if (!inString) depth++
        ']', '}' -> {
            if (!inString) {
                depth--
                if (depth == 0 && c == ']') {
                    arrayEnd = i
                    break
                }
            }
        }
    }
}

if (arrayEnd == -1) {
    throw AIClientException("Translations array end not found")
}
```

**Critical Failure Point:**
- If the loop completes without finding `arrayEnd`, it throws an exception
- This happens when the JSON array is never closed (`]` is missing)
- No recovery mechanism exists

### 3. Scenarios Causing Incomplete Responses

#### Scenario 1: LLM Response Truncation
**Cause:** LLM stops generating mid-response

**Why it happens:**
- Token limit reached (max_tokens setting too low)
- Model's internal stop sequence triggered early
- Complex content causes model to halt unexpectedly

**Example of truncated response:**
```json
{
  "translations": [
    {"index": 0, "text": "First paragraph translated"},
    {"index": 1, "text": "Second paragraph tra...
```
^^^ Missing closing `]` and `}`

#### Scenario 2: Network/Transmission Issues
**Cause:** Connection drops or timeout during response streaming

**Why it happens:**
- Network interruption between API and client
- HTTP connection timeout (default: 30-600 seconds, configurable)
- Proxy or load balancer drops connection

**Current timeout configuration:**
```kotlin
// OpenAICompatibleClient.kt:217
.timeout(Duration.ofSeconds(settings.timeoutSeconds.toLong()))

// Settings default: 30 seconds (configurable 30-600)
```

**Problem:** Even with timeout, the exception is generic and doesn't distinguish between:
- Network timeout (should retry)
- Malformed response (should recover or fail gracefully)
- LLM error (should report)

#### Scenario 3: LLM Malformed Response
**Cause:** LLM generates invalid JSON

**Why it happens:**
- Model doesn't follow JSON formatting instructions
- Special characters in content not properly escaped
- Model hallucinates different JSON structure

**Example of malformed response:**
```json
{
  "translations": [
    {"index": 0, "text": "Text with "quotes" in middle"},
    // Missing comma after next line
    {"index": 1, "text": "More text"}
```
^^^ Parser may or may not handle this depending on escape sequences

### 4. Current Error Handling

**In translate() function:**
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

**Problems:**
1. **No retry logic** - Single attempt, then fails completely
2. **No partial recovery** - Can't salvage any translations from incomplete response
3. **Generic error handling** - All exceptions treated the same way
4. **No telemetry** - Can't distinguish error types for monitoring

## Impact Analysis

### User Impact
- **Severity:** High
- **Frequency:** Unknown (no telemetry to measure)
- **User Experience:** Complete translation failure, no partial results shown

### Data Loss
- **Type:** Total loss
- **Recoverability:** None (no retry or fallback)
- **User Action Required:** Manual retry (if they notice the failure)

## Contributing Factors

### 1. Missing Retry Logic
**Current State:** No retry mechanism

**Best Practice:** Exponential backoff retry for transient failures
- Network timeouts: retry 2-3 times
- 5xx errors: retry 2-3 times
- Rate limits: retry with backoff

### 2. No Partial Response Recovery
**Current State:** All-or-nothing parsing

**Best Practice:** Extract available translations from incomplete responses
```kotlin
// Example: Parse what we can, fill gaps with original text
val partialTranslations = parsePartialResponse(response)
val result = mergeWithOriginals(partialTranslations, translatableTexts)
```

### 3. Insufficient Timeout Handling
**Current State:** Single timeout value applies to entire request

**Best Practice:**
- Separate timeout for connection vs. read
- Progressive timeout increase on retries
- Early warning for long-running requests

### 4. Limited Error Context
**Current State:** Generic error message

**Best Practice:** Detailed error information for debugging
```kotlin
data class TranslationError(
    val type: ErrorType,  // TIMEOUT, MALFORMED, INCOMPLETE, etc.
    val retryable: Boolean,
    val details: String,
    val cause: Throwable?
)
```

## Scenarios Requiring Different Handling

| Scenario | Current Behavior | Ideal Behavior | Retry? |
|----------|-----------------|----------------|--------|
| LLM truncation (low tokens) | Error | Increase max_tokens, retry | Yes |
| LLM truncation (unknown) | Error | Extract partial, fill gaps | No |
| Network timeout (30s) | Error | Retry with longer timeout | Yes |
| Network timeout (600s) | Error | Report as timeout, suggest offline | No |
| Malformed JSON | Error | Attempt repair, then fail | No |
| 5xx server error | Error | Retry with backoff | Yes |
| 429 rate limit | Error | Retry with exponential backoff | Yes |
| 4xx client error | Error | Report user error | No |

## Recommendations (Priority Order)

### Priority 1: Add Retry Logic
- Implement exponential backoff for retryable errors
- Max 3 retries
- Distinguish retryable vs. non-retryable errors

### Priority 2: Partial Response Recovery
- Parse incomplete JSON arrays
- Extract available translations
- Fill missing translations with original text
- Warn user about partial results

### Priority 3: Better Error Messages
- Distinguish error types (timeout, malformed, incomplete)
- Provide actionable user feedback
- Log detailed context for debugging

### Priority 4: Telemetry
- Track error frequency by type
- Monitor retry success rate
- Identify problematic content patterns

## Next Steps

1. **Phase 5:** Assess current code for retry patterns elsewhere in codebase
2. **Phase 6:** Write specification for robust error handling
3. **Phase 8:** Implement retry logic and partial recovery
4. **Phase 9:** Verify all scenarios handled correctly

## Appendix: Test Cases for Verification

### TC1: Complete Valid Response
```json
{"translations": [
  {"index": 0, "text": "Complete 0"},
  {"index": 1, "text": "Complete 1"}
]}
```
**Expected:** Success with 2 translations

### TC2: Truncated Response (Missing Closing Bracket)
```json
{"translations": [
  {"index": 0, "text": "Complete 0"},
  {"index": 1, "text": "Incomple...
```
**Expected:** Parse index 0, fill index 1 with original, warn user

### TC3: Empty Array
```json
{"translations": []}
```
**Expected:** Error or return empty list (depending on spec)

### TC4: Malformed JSON
```json
{"translations": [
  {"index": 0, "text": "Bad "json" here"}
```
**Expected:** Attempt repair, then fail gracefully

### TC5: Network Timeout
**Response:** TimeoutException after 30s
**Expected:** Retry with longer timeout (60s, 90s)

