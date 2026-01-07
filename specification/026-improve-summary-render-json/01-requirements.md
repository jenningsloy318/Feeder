# Requirements - Fix Auto-Summary Displaying Raw JSON

**Date**: 2025-01-07
**Issue**: Auto-summary sometimes displays raw JSON instead of formatted markdown
**Priority**: High (User Experience Bug)
**Type**: Bug Fix

## Problem Statement

When opening an article and triggering auto-summary, sometimes the summary displays raw JSON content like:

```json
{"content":"...","title":"...","keyPoints":[...],"sentiment":"..."}
```

Instead of properly formatted markdown text. This is an intermittent bug that significantly degrades user experience.

**Evidence**: Screenshot shows article displaying `{"content":"...","title":"..."}` instead of formatted summary.

## Current Behavior

### Expected Flow
1. User opens article
2. Auto-summary triggers (or user clicks summarize button)
3. AI generates JSON-structured response with fields: `language`, `title`, `keyPoints`, `summary`, `sentiment`
4. App parses JSON and extracts `summary` field
5. App displays formatted markdown summary to user

### Actual Behavior (Bug)
Sometimes the entire JSON object is displayed to the user instead of just the summary content. This happens when:

1. AI returns JSON with empty or missing `summary` field
2. OR JSON parsing fails partially
3. Code falls back to displaying raw JSON response instead of user-friendly message

## Root Cause Analysis

### Code Locations

**File**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`
```kotlin
// Line 329
summary = summary.ifEmpty { content }, // Fallback to original content
```

**File**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`
```kotlin
// Line 402
summary = summary.ifEmpty { content }, // Fallback to original content
```

### The Bug

When `summary` field parsing results in empty string:
- The `ifEmpty` block executes
- `content` variable = raw JSON text from AI response
- User sees raw JSON instead of helpful message

**Why This Happens**:
1. AI sometimes omits or creates empty `summary` field in JSON response
2. `extractJsonFromMarkdown()` extracts JSON but parsing fails on `summary` field
3. Fallback logic uses raw `content` (the JSON text) instead of graceful error handling

## Requirements

### Functional Requirements

**FR-1: Proper Fallback Handling**
- When `summary` field is empty or missing, display user-friendly error message
- DO NOT display raw JSON to user
- Provide retry mechanism or guidance

**FR-2: JSON Parsing Robustness**
- Improve `parseSummaryJsonResponse()` to handle missing fields gracefully
- Validate JSON structure before using
- Log parsing failures for debugging

**FR-3: User Experience**
- Show loading state during summarization
- Show error message if summarization fails
- Allow user to retry summarization
- Display structured data (title, keyPoints) if available

### Non-Functional Requirements

**NFR-1: Error Handling**
- Never display raw JSON to end users
- Always show user-friendly messages
- Log technical details for debugging

**NFR-2: Backward Compatibility**
- Maintain existing API structure
- Don't break working manual summarization
- Keep JSON structured output format

**NFR-3: Performance**
- No significant performance impact
- Maintain current response times

## User Stories

**US-1**: As a user, I should never see raw JSON code when using auto-summary feature
**US-2**: As a user, I should see a clear error message if summarization fails
**US-3**: As a user, I should be able to retry summarization if it fails

## Acceptance Criteria

**AC-1**: Auto-summary never displays raw JSON to users
**AC-2**: When summary field is empty, show user-friendly message: "Could not generate summary. Please try again."
**AC-3**: Manual summarization continues to work correctly
**AC-4**: Error states are logged for debugging
**AC-5**: All structured fields (title, keyPoints) are displayed when available

## Success Metrics

- Zero occurrences of raw JSON display in production
- User-reported summary issues reduced by 90%
- Error rate for auto-summarization < 5%

## Out of Scope

- Changing AI prompt structure (covered in spec-21)
- Modifying summary UI design
- Changing markdown rendering library (covered in spec-23)
- Modifying manual summarization flow

## Related Specifications

- **spec-19**: Improve summary rendering - markdown library integration
- **spec-21**: Improve summary prompt - AI prompt optimization
- **spec-23**: Use standalone lib for summary render - Compose compatibility fix
- **spec-5**: Fix auto-summary - previous auto-summary issues

## Dependencies

- Kotlin 2.2.20
- kotlinx.serialization library
- Existing AI provider clients (Anthropic, OpenAI-compatible)
- Markdown rendering library (mikepenz)

## Technical Context

### Data Flow

```
Article Content
    ↓
AI Provider (Anthropic/OpenAI)
    ↓
JSON Response: {"language":"en", "title":"...", "summary":"...", ...}
    ↓
parseSummaryJsonResponse(content)
    ↓
Extracts summary field
    ↓
BUG: If summary.empty → fallback to raw JSON ❌
    ↓
Displays to user
```

### Fix Required

```
parseSummaryJsonResponse(content)
    ↓
Extracts summary field
    ↓
If summary.empty → return error/fallback message ✅
    ↓
Displays to user
```

## Open Questions

1. Should we attempt to re-parse with different field names?
   - **Decision**: No, keep it simple. If `summary` field is missing/empty, show error.

2. Should we display other fields (title, keyPoints) when summary is empty?
   - **Decision**: Yes, show title and keyPoints if available, with error message for summary.

3. Should we add retry button in error state?
   - **Decision**: No, user can tap summarize button again.

4. Should we change AI prompt to force non-empty summary?
   - **Decision**: No, that's separate work (spec-21 already improved prompts).
