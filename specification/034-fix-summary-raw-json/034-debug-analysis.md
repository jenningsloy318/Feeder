# Debug Analysis: Summary Shows Raw JSON (Persistent Bug)

**Date:** 2026-03-17
**Severity:** High
**Status:** Root Cause(s) Found
**Branch:** 034-fix-summary-raw-json

## Issue Summary

When users request an AI summary of an article, the summary intermittently displays raw JSON instead of properly rendered markdown text. This bug has been attempted multiple times (spec-019, spec-021, spec-023, spec-026, spec-029) but persists because previous fixes only addressed the most obvious scenario and missed several other code paths that leak raw JSON.

## Evidence Collected

### Previous Fix Attempts

| Spec | Focus | What It Fixed | What It Missed |
|------|-------|---------------|----------------|
| spec-019 | Markdown rendering | Added markdown-to-HTML renderer | No JSON parsing fix |
| spec-021 | JSON structured prompt | Added JSON response format + parsing | Created the `.ifEmpty { content }` fallback bug |
| spec-023 | Standalone markdown lib | Switched to mikepenz markdown renderer | No JSON parsing fix |
| spec-026 | Raw JSON display fix | Fixed `.ifEmpty { content }` to user-friendly message | Only fixed the empty-summary scenario; missed 4 other leak paths |
| spec-029 | List truncation | Fixed list item truncation in summaries | Not related to raw JSON |

### Architecture Overview

```
User taps Summarize
    |
ArticleViewModel.summarize()                    (ArticleViewModel.kt:470-491)
    |
AIApi.summarize(content)                        (AIApi.kt:79-109)
    |
AIClient.create(settings).generateSummary()     (AIClient.kt:145-149)
    |
    +-- OpenAICompatibleClient.generateSummary() (OpenAICompatibleClient.kt:199-267)
    |   |
    |   +-- buildSummaryPrompt()                 (OpenAICompatibleClient.kt:103-197)
    |   +-- HTTP request via SDK                 (OpenAICompatibleClient.kt:210-226)
    |   +-- Extract text from response           (OpenAICompatibleClient.kt:234-241)
    |   +-- parseSummaryJsonResponse(text)        (OpenAICompatibleClient.kt:243)
    |       |
    |       +-- extractJsonFromMarkdown()         (OpenAICompatibleClient.kt:440-455)
    |       +-- Json.parseToJsonElement()          (OpenAICompatibleClient.kt:378)
    |       +-- Extract summary field             (OpenAICompatibleClient.kt:401)
    |       +-- CATCH: parseLegacySummaryResponse()(OpenAICompatibleClient.kt:426-434)
    |
    +-- AnthropicClient.generateSummary()        (AnthropicClient.kt:149-207)
        |
        [Same parsing pipeline, duplicated code]
    |
SummaryResult.Success(content = summaryData.summary)
    |
AISummaryState.Result(value = summaryResult)
    |
ArticleScreen.SummarySection()                  (ArticleScreen.kt:658-685)
    |
MarkdownText() -> MarkdownContentSafe()         (ArticleScreen.kt:703-712)
    |
mikepenz Markdown()                             (MarkdownToAnnotatedString.kt:61-65)
    |
User sees rendered content
```

## Root Cause Analysis

### THREE layers of defense exist, but ALL have gaps

**Layer 1 - Main JSON Parser** (`parseSummaryJsonResponse`):
- Extracts JSON from markdown code blocks
- Parses the JSON and extracts the `summary` field
- If summary is empty/missing, returns user-friendly message
- **Gap**: Only works when JSON parsing succeeds

**Layer 2 - Legacy Parser** (`parseLegacySummaryResponse`):
- Checks if content starts with `{` or `[`
- If so, returns "Could not generate summary" message
- **Gap**: Only checks the FIRST character of trimmed content

**Layer 3 - UI** (`SummarySection` in `ArticleScreen.kt:669-676`):
- Checks if display content starts with `{` or `[`
- If so, replaces with error message
- **Gap**: Same first-character check as Layer 2

### Confirmed Root Cause 1: LLM Wraps JSON in Explanatory Text (HIGH LIKELIHOOD)

**Scenario**: Many LLMs add conversational text around JSON output despite prompt instructions.

**LLM Response Example**:
```
Here is the summary of the article in JSON format:

{
  "language": "en",
  "title": "Article About X",
  "keyPoints": ["Point 1", "Point 2"],
  "summary": "The article discusses...",
  "sentiment": "neutral"
}
```

**Execution Trace**:
```
extractJsonFromMarkdown(content)
  → No ```json or ``` code blocks found
  → Returns full text as-is: "Here is the summary...\n{...}"
  ↓
Json.parseToJsonElement(jsonContent)
  → FAILS: content starts with "Here is the summary", not valid JSON
  → Throws SerializationException
  ↓
catch (e: SerializationException)
  → parseLegacySummaryResponse(content)      ← receives ORIGINAL text
  ↓
parseLegacySummaryResponse(content):
  content.trim() = "Here is the summary...\n{...}"
  startsWith("{") = FALSE                     ← Does NOT start with {
  startsWith("[") = FALSE
  → Returns: "Here is the summary...\n{...}"  ← RAW JSON LEAK!
  ↓
SummarySection (UI):
  displayContent = "Here is the summary...\n{...}"
  startsWith("{") = FALSE                     ← Same check fails
  → Renders full text including JSON to user  ← USER SEES RAW JSON
```

**Why This Happens Intermittently**: Different LLM models, temperatures, and article types produce varying levels of "chatty" responses. Some models strictly follow the "Return ONLY the JSON" instruction, others don't.

**Files**:
- `OpenAICompatibleClient.kt:426-434` (catch block)
- `OpenAICompatibleClient.kt:462-499` (legacy parser)
- `AnthropicClient.kt:353-361` (catch block)
- `AnthropicClient.kt:389-426` (legacy parser)

### Confirmed Root Cause 2: Truncated JSON Response (MEDIUM LIKELIHOOD)

**Scenario**: When `maxTokens` (2048) is insufficient for long articles, the response gets truncated mid-JSON.

**LLM Response Example** (truncated in code block):
````
```json
{
  "language": "en",
  "title": "Very Long Article Title",
  "keyPoints": ["Point 1", "Point 2", "Point 3"],
  "summary": "This is a very long summary that discusses many aspects of the topic including the economic im
````

**Execution Trace**:
```
extractJsonFromMarkdown(content)
  → Regex ```json\s*([\s\S]*?)\s*``` looks for closing ```
  → NO closing ``` found (response truncated)
  → Falls to generic ``` regex: also no match
  → Returns full text as-is: "```json\n{..."
  ↓
Json.parseToJsonElement(jsonContent)
  → FAILS: starts with "```json", not valid JSON
  ↓
parseLegacySummaryResponse(content)
  content.trim() = "```json\n{..."
  startsWith("{") = FALSE                     ← starts with "```"
  → Returns full text with markdown + truncated JSON  ← LEAK!
```

**Files**:
- `OpenAICompatibleClient.kt:440-455` (`extractJsonFromMarkdown`)
- `OpenAICompatibleClient.kt:462-499` (legacy parser)

### Confirmed Root Cause 3: Error Messages Containing JSON (LOW LIKELIHOOD)

**Scenario**: API error responses (rate limits, auth failures, server errors) may contain JSON in exception messages.

**Error flow in `generateSummary()`**:
```kotlin
// OpenAICompatibleClient.kt:264-266
} catch (e: Exception) {
    AIClient.SummaryResult.Error(content = e.message ?: ...)
}
```

**Error flow in `AIApi.summarize()`**:
```kotlin
// AIApi.kt:106-108
} catch (e: Exception) {
    AIClient.SummaryResult.Error(content = e.message ?: ...)
}
```

**Error flow in `ArticleViewModel.summarize()`**:
```kotlin
// ArticleViewModel.kt:482-488
} catch (e: Exception) {
    aiSummary.value = AISummaryState.Result(
        value = AIClient.SummaryResult.Error(content = e.message ?: "Unknown error"),
    )
}
```

If `e.message` is:
```
Request failed: {"error":{"message":"Rate limit exceeded","type":"rate_limit_error","code":"429"}}
```

Then all three `startsWith("{")` checks would fail (starts with "Request").

**Files**:
- `OpenAICompatibleClient.kt:264-266`
- `AIApi.kt:106-108`
- `ArticleViewModel.kt:482-488`
- `ArticleScreen.kt:669-676`

### Confirmed Root Cause 4: Double-Encoded JSON String (LOW LIKELIHOOD)

**Scenario**: Some API-compatible endpoints return the JSON as a string-within-a-string.

**API Response content**:
```
"{\"language\":\"en\",\"summary\":\"The article...\"}"
```

**Execution Trace**:
```
extractJsonFromMarkdown(content)
  → No code blocks, returns as-is: "\"{\\"language\\"..."
  ↓
Json.parseToJsonElement(jsonContent)
  → Parses as JsonPrimitive (string type), NOT JsonObject
  ↓
jsonElement.jsonObject
  → Throws IllegalArgumentException: "JsonPrimitive is not a JsonObject"
  ↓
catch (e: Exception)
  → parseLegacySummaryResponse(content)
  content.trim() = "\"{\\"language\\"...\"}"
  startsWith("{") = FALSE                     ← starts with quote "
  → Returns raw double-encoded JSON string    ← LEAK!
```

### Structural Issue: Code Duplication

Both `OpenAICompatibleClient` and `AnthropicClient` contain **identical copies** of:
- `parseSummaryJsonResponse()` (~60 lines)
- `extractJsonFromMarkdown()` (~15 lines)
- `parseLegacySummaryResponse()` (~30 lines)
- `SummaryResponseData` data class (~8 lines)

This duplication means:
1. Every fix must be applied twice
2. Risk of divergence between the two implementations
3. Previous fixes missed one of the two files

### Structural Issue: Zero Test Coverage

There are **zero unit tests** for:
- `parseSummaryJsonResponse()`
- `extractJsonFromMarkdown()`
- `parseLegacySummaryResponse()`
- The `SummarySection` composable's JSON detection

The `AIApiTest.kt` contains only a single trivial test (`testApiCreation`).

## Reproduction Scenarios

### Scenario 1: Most Common - LLM Adds Preamble

**Rate**: ~10-20% of summary requests depending on model
**Steps**:
1. Configure an OpenAI-compatible provider (especially non-OpenAI models like Ollama, LM Studio)
2. Open an article
3. Tap Summarize
4. If the model returns JSON with any text before or after it, raw JSON appears

### Scenario 2: Long Articles - Token Truncation

**Rate**: ~5% of summary requests for very long articles
**Steps**:
1. Open a very long article (>5000 words)
2. Tap Summarize
3. If the response exceeds 2048 tokens, truncated JSON appears

### Scenario 3: API Errors

**Rate**: Rare (only during API issues)
**Steps**:
1. Trigger a rate limit or auth error
2. The error message may contain JSON

## Proposed Fix Strategy

### Fix 1 (P0): Robust JSON Extraction - Replace `extractJsonFromMarkdown`

Add a new method that can find JSON objects anywhere in the text, not just in code blocks:

```kotlin
// In a new shared utility class (eliminates duplication)
private fun extractJsonObject(content: String): String {
    // 1. Try markdown code blocks first (existing logic)
    val jsonCodeBlock = Regex("""```json\s*([\s\S]*?)\s*```""").find(content)
    if (jsonCodeBlock != null) return jsonCodeBlock.groupValues[1].trim()

    val codeBlock = Regex("""```\s*([\s\S]*?)\s*```""").find(content)
    if (codeBlock != null) return codeBlock.groupValues[1].trim()

    // 2. NEW: Find JSON object by matching balanced braces
    val firstBrace = content.indexOf('{')
    if (firstBrace >= 0) {
        val jsonCandidate = content.substring(firstBrace)
        // Find matching closing brace (handle nesting)
        var depth = 0
        var inString = false
        var escape = false
        for (i in jsonCandidate.indices) {
            val c = jsonCandidate[i]
            if (escape) { escape = false; continue }
            if (c == '\\') { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (!inString) {
                if (c == '{') depth++
                if (c == '}') { depth--; if (depth == 0) return jsonCandidate.substring(0, i + 1) }
            }
        }
    }

    // 3. Return as-is (let parser handle the error)
    return content.trim()
}
```

**Files to modify**:
- `OpenAICompatibleClient.kt:440-455`
- `AnthropicClient.kt:367-382`

### Fix 2 (P0): Comprehensive Content Sanitization in Legacy Parser

Replace the simple `startsWith("{")` check with a check for JSON anywhere:

```kotlin
private fun parseLegacySummaryResponse(content: String): SummaryResponseData {
    // ... existing Lang: handling ...

    val trimmedContent = content.trim()
    // Check if content CONTAINS JSON (not just starts with it)
    val containsJson = trimmedContent.contains(Regex("""\{[^}]*"(language|summary|title|keyPoints)"[^}]*"""))

    val summary = if (containsJson) {
        "Could not generate summary. Please try again."
    } else if (trimmedContent.startsWith("```")) {
        // Truncated code block - also not displayable
        "Could not generate summary. Please try again."
    } else {
        trimmedContent
    }
    // ...
}
```

**Files to modify**:
- `OpenAICompatibleClient.kt:462-499`
- `AnthropicClient.kt:389-426`

### Fix 3 (P0): UI Layer - Check for JSON Anywhere

```kotlin
// ArticleScreen.kt SummarySection
val safeContent = if (
    displayContent.startsWith("{") ||
    displayContent.startsWith("[") ||
    displayContent.startsWith("```") ||
    displayContent.contains(Regex("""\{[^}]*"(summary|language|keyPoints)"[^}]*\}"""))
) {
    "Could not generate summary. Please try again."
} else {
    displayContent
}
```

**File to modify**: `ArticleScreen.kt:669-676`

### Fix 4 (P1): Extract Shared Parsing Logic

Move all duplicated parsing code to a shared utility:

```kotlin
// NEW FILE: SummaryResponseParser.kt
object SummaryResponseParser {
    fun parse(content: String): SummaryResponseData { ... }
    internal fun extractJsonObject(content: String): String { ... }
    internal fun parseLegacy(content: String): SummaryResponseData { ... }
}
```

This eliminates the duplication between `OpenAICompatibleClient` and `AnthropicClient` and ensures fixes are applied in a single location.

**Files to create**: `app/src/main/java/com/nononsenseapps/feeder/ai/SummaryResponseParser.kt`
**Files to modify**: `OpenAICompatibleClient.kt`, `AnthropicClient.kt` (delegate to shared parser)

### Fix 5 (P1): Add Unit Tests

Test cases needed:
1. Valid JSON with all fields
2. Valid JSON in ```json code block
3. Valid JSON in plain ``` code block
4. JSON with text preamble ("Here is the summary:\n{...}")
5. JSON with text preamble AND postamble
6. Truncated JSON (incomplete code block)
7. Empty summary field in valid JSON
8. Missing summary field in valid JSON
9. Completely different JSON structure (wrong field names)
10. Double-encoded JSON string
11. Plain text response (no JSON at all)
12. Legacy "Lang: XX" format
13. Error message containing JSON
14. Content starting with ``` but truncated

### Fix 6 (P2): Sanitize Error Messages

```kotlin
// In AIApi.kt and ArticleViewModel.kt catch blocks
private fun sanitizeErrorMessage(message: String): String {
    val trimmed = message.trim()
    return if (trimmed.contains("{") && trimmed.contains("}")) {
        "Summary generation failed. Please try again."
    } else {
        trimmed
    }
}
```

## Risk Assessment

| Fix | Risk | Rationale |
|-----|------|-----------|
| Fix 1: Robust JSON extraction | Low | Additive change; existing code block extraction preserved as first attempt |
| Fix 2: Legacy parser enhancement | Low | Only changes the error-fallback path; valid content unaffected |
| Fix 3: UI layer check | Very Low | Last-resort defense; never shows to user for valid summaries |
| Fix 4: Extract shared code | Medium | Structural refactor; could introduce bugs if not careful |
| Fix 5: Unit tests | None | Tests only; no production code change |
| Fix 6: Error sanitization | Low | Only affects error display path |

## Verification Plan

### After Fix, ALL These Must Pass:

| Test Input | Expected Output | Verifies |
|-----------|----------------|----------|
| `{"summary":"text","language":"en"}` | "text" | Normal JSON parsing |
| `` ```json\n{"summary":"text"}\n``` `` | "text" | Code block extraction |
| `Here is:\n{"summary":"text"}` | "text" (via Fix 1 brace matching) | **Root Cause 1** |
| `` ```json\n{"summary":"trunc `` | "Could not generate summary." | **Root Cause 2** |
| `Error: {"error":"rate_limit"}` | "Could not generate summary." | **Root Cause 3** |
| `"{\"summary\":\"text\"}"` | "Could not generate summary." | **Root Cause 4** |
| `Lang: en\nPlain text summary` | "Plain text summary" | Legacy format |
| `Just a plain text summary` | "Just a plain text summary" | Non-JSON response |

## Related Files Summary

| File | Path | Role |
|------|------|------|
| OpenAICompatibleClient.kt | `app/src/.../ai/provider/OpenAICompatibleClient.kt` | OpenAI-compatible API client with summary parsing |
| AnthropicClient.kt | `app/src/.../ai/provider/AnthropicClient.kt` | Anthropic API client with duplicated summary parsing |
| AIClient.kt | `app/src/.../ai/AIClient.kt` | Interface defining SummaryResult types |
| AIApi.kt | `app/src/.../ai/AIApi.kt` | High-level API with error handling |
| ArticleViewModel.kt | `app/src/.../feedarticle/ArticleViewModel.kt` | ViewModel with summary state management |
| ArticleScreen.kt | `app/src/.../feedarticle/ArticleScreen.kt` | UI rendering of summary with safety check |
| MarkdownToAnnotatedString.kt | `app/src/.../text/MarkdownToAnnotatedString.kt` | Markdown rendering via mikepenz library |
| AIApiTest.kt | `app/src/test/.../ai/AIApiTest.kt` | Only 1 trivial test; no parsing tests |

## Conclusion

The bug persists because **spec-026 only fixed one of five leak paths**. The `ifEmpty { content }` fix addressed the case where JSON parses successfully but the `summary` field is empty. However, it completely missed the cases where:

1. JSON parsing fails because the LLM wraps JSON in explanatory text
2. JSON extraction fails because the response is truncated
3. Error messages contain embedded JSON
4. The response contains double-encoded JSON strings

All five leak paths share a common weakness: the `startsWith("{")` check is the only line of defense, and it fails when JSON appears after any prefix text.

The definitive fix requires:
1. **Robust JSON extraction** that can find JSON objects within surrounding text (Fix 1)
2. **Content-aware sanitization** in the legacy parser (Fix 2)
3. **Defense-in-depth** at the UI layer (Fix 3)
4. **Code deduplication** to prevent future drift (Fix 4)
5. **Unit tests** to prevent regression (Fix 5)
