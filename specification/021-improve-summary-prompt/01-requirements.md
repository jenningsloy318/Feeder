# Requirements: Improve Summary Prompt with JSON Response and Structured Markdown

## User Request
Check the prompt of the summary, online deep research the best prompt for summary, and also use json format as response. And the actual content of the summary should be structured markdown.

**ADDITIONAL REQUIREMENT**: Add the same timeout setting for summary that exists for translation, under Settings → AI Integration → Summary

## Current State Analysis

### Existing Implementation
The current summary generation is implemented in:
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` (lines 34-51)
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` (lines 81-98)

### Current Prompt
```
You are a helpful assistant that summarizes news articles.
Detect the article's language and summarize in that same language.

Start your response with "Lang: " followed by the detected language code.
For example: "Lang: en"

Then provide a concise summary of the article.
```

### Current Response Format
Plain text with "Lang:" prefix for language detection.

### Current Issues
1. **Basic prompt**: Lacks advanced prompting techniques
2. **Plain text parsing**: Fragile regex-based parsing of "Lang:" prefix
3. **No structure**: Summary is unstructured plain text
4. **Inconsistent quality**: No guidelines for summary quality

### Successful Pattern to Follow
The translation feature (implemented in the same files) already uses:
- Sophisticated JSON-structured prompts
- Professional role assignment
- Clear guidelines and constraints
- JSON response format with reliable parsing
- Structure awareness

## Functional Requirements

### FR1: Enhanced Prompt Engineering
- Research and implement best practices for AI summarization
- Use professional role assignment (like translation does)
- Provide clear summarization guidelines
- Include quality criteria

### FR2: JSON Response Format
- Change from plain text to JSON response
- Include structured metadata (language, summary sections, etc.)
- Enable reliable JSON parsing (avoid regex)
- Follow the same pattern as translation feature

### FR3: Structured Markdown Content
- Summary content should be well-structured markdown
- Include logical sections (e.g., Key Points, Details, Conclusion)
- Use markdown formatting (headers, bullets, etc.)
- Ensure readability and scanability

### FR4: Backward Compatibility
- Maintain existing `AIClient.SummaryResult` interface
- Keep language detection working
- Preserve all existing metadata (tokens, model, etc.)
- No breaking changes to API consumers

### FR5: Summary Timeout Setting
- Add timeout setting for summary generation (like translation)
- Add UI setting under Settings → AI Integration → Summary
- Add repository field for `summaryTimeout`
- Apply timeout when calling summary API
- Follow the same pattern as translation timeout

## Non-Functional Requirements

### NFR1: Quality
- Summary should be accurate and comprehensive
- Should capture main points effectively
- Should be concise but complete
- Should preserve important details

### NFR2: Performance
- JSON parsing should be efficient
- No significant increase in token usage
- Should not increase API call latency

### NFR3: Maintainability
- Code should follow existing patterns (translation implementation)
- Clear separation of concerns
- Well-documented prompt structure
- Easy to adjust prompt in future

### NFR4: Robustness
- Handle malformed JSON responses gracefully
- Provide clear error messages
- Fallback to current format if JSON parsing fails

## Technical Requirements

### TR1: JSON Schema
Define a clear JSON schema for summary response:
```json
{
  "language": "en",
  "title": "Optional article title",
  "summary": "### Key Points\n\n- Point 1\n- Point 2\n\n### Summary\n\nDetailed summary...",
  "keyPoints": ["Point 1", "Point 2", "Point 3"],
  "sentiment": "positive/negative/neutral"
}
```

### TR2: Prompt Structure
- Professional role (expert summarizer)
- Clear task description
- Input format specification
- Output format requirements
- Summarization guidelines
- Quality criteria
- Examples (optional)

### TR3: Error Handling
- JSON extraction from markdown code blocks
- Validation of required fields
- Fallback mechanisms
- User-friendly error messages

## Acceptance Criteria

### AC1: JSON Response Format
- [ ] Response is valid JSON
- [ ] Contains language field
- [ ] Contains structured markdown summary
- [ ] Can be parsed reliably without regex

### AC2: Structured Markdown
- [ ] Summary uses markdown formatting
- [ ] Has logical sections (headers, bullets)
- [ ] Renders correctly in UI
- [ ] Is easy to read and scan

### AC3: Improved Quality
- [ ] Summaries are more comprehensive
- [ ] Capture key points effectively
- [ ] Maintain accuracy
- [ ] Are appropriately concise

### AC4: No Regressions
- [ ] All existing tests pass
- [ ] Language detection still works
- [ ] API consumers unaffected
- [ ] No breaking changes

### AC5: Code Quality
- [ ] Follows existing patterns (translation)
- [ ] Well-documented
- [ ] Handles errors gracefully
- [ ] Maintains separation of concerns

### AC6: Summary Timeout Setting
- [ ] Timeout setting appears in Settings → AI Integration → Summary
- [ ] Setting can be configured by user
- [ ] Timeout is applied when generating summaries
- [ ] Follows same pattern as translation timeout
- [ ] Settings are persisted correctly

## Out of Scope

- Changing the UI rendering of summaries (handled by separate feature spec-019)
- Modifying the summary configuration/settings
- Changing when summaries are generated
- Modifying the translation feature
- Adding new summary languages

## Dependencies

### Existing Code
- `AIClient.generateSummary()` interface must be preserved
- `AIClient.SummaryResult` sealed interface must be maintained
- Translation implementation as reference pattern

### External APIs
- OpenAI API (existing)
- Anthropic Claude API (existing)

## Risks

### Risk1: Increased Token Usage
**Mitigation**: Optimize prompt to be concise while effective

### Risk2: JSON Parsing Failures
**Mitigation**: Robust error handling with fallbacks

### Risk3: Quality Regression
**Mitigation**: Thorough testing and prompt iteration

## Open Questions

1. **Q**: Should we include sentiment analysis?
   **A**: Optional - can be added later if needed

2. **Q**: What should the exact markdown structure be?
   **A**: To be determined by research and testing

3. **Q**: Should we include key points separately?
   **A**: Yes - improves scanability

4. **Q**: How to handle very long articles?
   **A**: Keep summary concise as before, focus on key points
