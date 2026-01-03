# Phase 2: Requirements Clarification

**Feature**: AI Translation Function Implementation
**Date**: 2026-01-03
**Phase**: 2 - Requirements Clarification
**Status**: Complete

---

## Executive Summary

This document captures the complete requirements for implementing real AI-powered article translation in the Feeder RSS reader application. The feature replaces the dummy implementation from Spec 13 with actual AI translation using the configured provider.

---

## Business Context

### Problem Statement
Users want to read articles in their preferred language but the current implementation only shows dummy translation results (prefixed paragraphs with "[Translated Paragraph N]").

### Solution
Implement real AI translation that:
- Uses the configured AI provider (OpenAI-compatible or Anthropic)
- Translates full article content efficiently
- Displays paragraph-by-paragraph translations
- Supports both auto-translate and manual modes

### Success Criteria
1. Translation uses real AI provider API
2. Full article content translated in single API call
3. Translations map correctly to original paragraphs
4. Auto-translate mode works when enabled
5. Manual translation works via button click
6. Error handling provides user-friendly messages

---

## Functional Requirements

### FR-1: Translation Service Integration

**Requirement**: The translation function must call the configured AI provider's translation API.

**Details**:
- Use default provider from Settings → AI Integration → Providers
- Provider can be OpenAI-compatible or Anthropic
- Send translation request to provider's chat completion API
- Use appropriate translation prompt

**Priority**: High (MVP requirement)

**Acceptance Criteria**:
- [ ] `OpenAICompatibleClient.translate()` calls OpenAI API
- [ ] `AnthropicClient.translate()` calls Anthropic API
- [ ] Translation uses configured model from settings
- [ ] API key properly authenticated
- [ ] Response properly parsed

---

### FR-2: Full Article Translation (Single API Call)

**Requirement**: Send complete article content in ONE request with paragraph indexing for efficient translation.

**Details**:
- Concatenate all paragraphs into single prompt
- Add index markers to preserve paragraph boundaries
- Request AI to translate maintaining paragraph structure
- Parse response to extract translated paragraphs by index

**Priority**: High (Performance requirement)

**Technical Approach**:
```kotlin
// Input format to AI:
"Translate the following article paragraphs to ${targetLanguage}.
Maintain paragraph structure and number each paragraph in your response:

[1] First paragraph text here...
[2] Second paragraph text here...
[3] Third paragraph text here...

Provide translations in the same format:
[1] Translated first paragraph...
[2] Translated second paragraph...
[3] Translated third paragraph..."
```

**Acceptance Criteria**:
- [ ] All paragraphs sent in single API call
- [ ] Paragraph boundaries preserved in translation
- [ ] Translation order matches original order
- [ ] Index markers correctly parsed from response
- [ ] Handles edge cases (empty paragraphs, special characters)

---

### FR-3: Auto-Translate Mode

**Requirement**: Automatically translate article when opening if auto-translate is enabled.

**Details**:
- Check `translationEnabled` setting on article open
- If enabled, automatically call translation function
- Show loading state during translation
- Display results when complete
- Don't auto-translate if already translated

**Priority**: High (Core user experience)

**Flow**:
```
User opens article
  ↓
Check translationEnabled setting
  ↓ (if true)
Call translate() automatically
  ↓
Show Loading state
  ↓
Display paragraph-by-paragraph translation
```

**Acceptance Criteria**:
- [ ] Auto-translate triggers on article open when enabled
- [ ] Respects `translationEnabled` setting
- [ ] Only auto-translates once per article load
- [ ] Shows loading indicator during translation
- [ ] Does not auto-translate when disabled

---

### FR-4: Manual Translation Mode

**Requirement**: User can manually trigger translation via button click.

**Details**:
- Translation button visible on article screen
- Clicking button triggers translation
- Can retry translation if it failed
- Can re-translate if user wants to refresh

**Priority**: High (Core user experience)

**Flow**:
```
User taps translation button
  ↓
Call translate()
  ↓
Show Loading state
  ↓
Display paragraph-by-paragraph translation
```

**Acceptance Criteria**:
- [ ] Button triggers translation on click
- [ ] Can retry failed translation
- [ ] Can re-translate to refresh results
- [ ] Button shows appropriate state (enabled/disabled/loading)

---

### FR-5: Target Language Configuration

**Requirement**: Use target language from translation settings.

**Details**:
- Read `translationLanguage` from Repository
- Pass target language to AI prompt
- Support all configured languages (EN, ZH, ES, FR, DE, etc.)
- Handle AUTO_DETECT language setting

**Priority**: High (Core functionality)

**Acceptance Criteria**:
- [ ] Translation uses configured target language
- [ ] Language code properly formatted in prompt
- [ ] Supports all TranslationLanguage enum values
- [ ] Handles AUTO_DETECT case (detects source, translates to app language)

---

### FR-6: Error Handling

**Requirement**: Gracefully handle translation errors with user-friendly messages.

**Details**:
- Catch API errors (network, auth, rate limit, etc.)
- Show error message in UI
- Allow retry on error
- Log errors for debugging

**Priority**: High (User experience)

**Error Scenarios**:
- [ ] Network timeout: "Translation failed: Network error. Please check your connection."
- [ ] Invalid API key: "Translation failed: Invalid API key. Check your AI provider settings."
- [ ] Rate limit exceeded: "Translation failed: Too many requests. Please try again later."
- [ ] Insufficient quota: "Translation failed: API quota exceeded."
- [ ] Generic error: "Translation failed: Please try again."

**Acceptance Criteria**:
- [ ] All API exceptions caught
- [ ] User-friendly error messages displayed
- [ ] Retry possible after error
- [ ] Errors logged with context

---

## Non-Functional Requirements

### NFR-1: Performance

**Requirement**: Complete translation within reasonable time.

**Metrics**:
- Target: < 10 seconds for typical article (10-20 paragraphs)
- Maximum: < 30 seconds for long articles (50+ paragraphs)
- Timeout: 60 seconds (provider API timeout)

**Acceptance Criteria**:
- [ ] Most articles translate within 10 seconds
- [ ] Progress indication shown during translation
- [ ] Timeout handled gracefully

---

### NFR-2: Scalability

**Requirement**: Handle multiple translation requests efficiently.

**Constraints**:
- Single article translation at a time per user
- No parallel translation requests for same article
- Queue requests if multiple articles opened quickly

**Acceptance Criteria**:
- [ ] Only one active translation per article
- [ ] Concurrent requests for different articles handled
- [ ] Request queue prevents overwhelming API

---

### NFR-3: Cost Efficiency

**Requirement**: Minimize API costs for users.

**Strategy**:
- Single API call per article (not per paragraph)
- Cache translations per article session
- Don't re-translate unless user requests

**Acceptance Criteria**:
- [ ] One API call per translation
- [ ] Translation cached in ViewModel state
- [ ] No redundant API calls

---

### NFR-4: Reliability

**Requirement**: Translation feature works reliably across different providers.

**Considerations**:
- Different providers may have different response formats
- Handle OpenAI and Anthropic response variations
- Fallback to dummy if API completely fails

**Acceptance Criteria**:
- [ ] Works with OpenAI-compatible providers
- [ ] Works with Anthropic Claude
- [ ] Graceful degradation on provider-specific issues

---

## Technical Context

### Existing Implementation (Spec 11 & 13)

**Already Implemented**:
- ✅ Translation settings screen (target language, enable toggle)
- ✅ Translation button on article screen
- ✅ Translation state management (Empty, Loading, Result)
- ✅ Paragraph extraction from article body
- ✅ Paragraph-by-paragraph display UI
- ✅ Translation language enum and settings
- ✅ Repository integration for settings

**Needs Implementation**:
- ❌ Real AI translation calls (currently dummy)
- ❌ Paragraph indexing in prompt
- ❌ Response parsing to extract translations
- ❌ Auto-translate trigger on article open
- ❌ Error handling with user messages

### Integration Points

**Files to Modify**:
1. `OpenAICompatibleClient.kt` - Implement `translate()` method
2. `AnthropicClient.kt` - Implement `translate()` method
3. `ArticleViewModel.kt` - Add auto-translate logic (possibly)
4. `AIApi.kt` - May need prompt engineering improvements

**New Dependencies**: None (using existing AI infrastructure)

---

## User Stories

### US-1: Auto-Translate Article

**As a** user who has enabled auto-translate
**I want** articles to automatically translate when I open them
**So that** I don't have to manually click the translate button every time

**Acceptance**:
- When I open an article with auto-translate enabled, it automatically translates
- I see a loading indicator while translation is in progress
- The translated text appears below the original text

### US-2: Manual Translation

**As a** user reading an article
**I want** to manually trigger translation with a button
**So that** I can choose when to translate (and avoid unnecessary API calls)

**Acceptance**:
- I see a translate button on the article screen
- When I tap it, translation starts
- I can retry if translation fails

### US-3: Choose Target Language

**As a** user
**I want** to select my preferred translation language
**So that** articles are translated to a language I understand

**Acceptance**:
- I can set target language in Settings → AI Integration → Translation
- Translations use my selected language
- I can change it at any time

---

## Edge Cases and Constraints

### EC-1: Empty Article
**Scenario**: Article has no body text
**Expected**: Show "No translatable content" error

### EC-2: Very Long Article
**Scenario**: Article with 100+ paragraphs
**Expected**:
- Truncate if exceeds provider's context limit
- Or split into multiple API calls (future enhancement)
- Show partial translation with message

### EC-3: Mixed Language Content
**Scenario**: Article contains multiple languages
**Expected**: Translate entire article to target language

### EC-4: Special Characters
**Scenario**: Article contains emojis, code blocks, tables
**Expected**: Preserve formatting in translation

### EC-5: No API Key Configured
**Scenario**: User hasn't set up AI provider
**Expected**: Show error message directing to settings

---

## Open Questions and Clarifications

### Q1: Auto-Translate Trigger Timing
**Question**: When exactly should auto-translate trigger?

**Options**:
A. Immediately when article screen opens (may delay UI)
B. After article content loads (better UX)
C. When user scrolls to first paragraph (lazy)

**Decision**: **Option B** - Trigger after article content loads to ensure smooth UI rendering and proper paragraph extraction.

### Q2: Translation Caching
**Question**: Should translations be cached persistently?

**Options**:
A. Yes, cache in database (saves API calls, but stale data)
B. No, cache only in session (fresh translations, more API calls)

**Decision**: **Option B** for now. Session-only caching ensures fresh translations. Persistent caching can be future enhancement.

### Q3: Partial Translation Handling
**Scenario**: API returns translation for some paragraphs but not all

**Decision**: Display available translations and show error for missing ones. Don't show partial results as complete success.

### Q4: Context Window Limits
**Question**: What if article exceeds provider's context limit?

**Decision**: For MVP, truncate with message: "Article too long for single translation request. Showing partial translation." Future enhancement: implement chunking.

---

## Dependencies

### External Dependencies
- OpenAI API or compatible endpoint
- Anthropic Claude API
- Valid API keys configured by user

### Internal Dependencies
- `AIClient.translate()` method implementation
- `Repository.translationEnabled` setting
- `Repository.translationLanguage` setting
- `ArticleViewModel.translate()` method
- Article body paragraph extraction

---

## Success Metrics

### Quantitative Metrics
- Translation success rate: > 95%
- Average translation time: < 10 seconds
- Error recovery success rate: > 80% (on retry)

### Qualitative Metrics
- User-perceived translation quality
- Ease of use (minimal taps required)
- Error message clarity

---

## Risks and Mitigations

### Risk-1: API Rate Limits
**Impact**: High (feature becomes unavailable)
**Mitigation**:
- Show clear error message
- Implement exponential backoff for retries
- Document rate limits in settings

### Risk-2: Poor Translation Quality
**Impact**: Medium (user disappointment)
**Mitigation**:
- Use high-quality models (GPT-4, Claude 3.5)
- Allow user to retry
- Provide feedback mechanism for improvement

### Risk-3: Increased API Costs for Users
**Impact**: Medium (user resistance)
**Mitigation**:
- Clear warning in settings about API usage
- Auto-translate off by default
- Cost estimation in documentation

---

## Future Enhancements (Out of Scope)

1. Persistent translation caching
2. Chunking for very long articles
3. Parallel translation of multiple articles
4. Translation quality settings (creative vs literal)
5. Source language detection
6. Translation memory (remember user corrections)
7. Batch translation for feed items

---

## Requirements Traceability Matrix

| Req ID | Priority | Status | File | Method/Component |
|--------|----------|--------|------|------------------|
| FR-1 | High | Pending | OpenAICompatibleClient.kt | translate() |
| FR-1 | High | Pending | AnthropicClient.kt | translate() |
| FR-2 | High | Pending | OpenAICompatibleClient.kt | buildTranslationPrompt() |
| FR-2 | High | Pending | OpenAICompatibleClient.kt | parseTranslationResponse() |
| FR-3 | High | Pending | ArticleViewModel.kt | autoTranslate trigger |
| FR-4 | High | Pending | ArticleScreen.kt | button handler |
| FR-5 | High | Pending | AIApi.kt | translate() |
| FR-6 | High | Pending | All files | error handling |

---

## Approval

**Requirements Status**: ✅ Approved
**Date**: 2026-01-03
**Ready for Research Phase**: Yes

---

## Notes

- All functional requirements are well-defined
- Non-functional requirements align with project standards
- Edge cases identified and handled
- Open questions resolved with clear decisions
- Ready to proceed to Phase 3 (Research)
