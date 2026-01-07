# Requirements: Improve Translation for Long Text

## Document Information
- **Spec ID**: 030
- **Feature**: Improve Translation for Long Text - Async Processing with Content Slicing
- **Created**: 2026-01-07
- **Status**: Draft
- **Type**: Performance Enhancement

---

## 1. Problem Statement

### 1.1 Current Issues
The Feeder app's translation feature works adequately for short articles and media content but experiences significant problems with long-form content (e.g., 1-hour interview transcripts, lengthy articles):

- **Timeout Failures**: LLM providers timeout before completing translation
- **Excessive Processing Time**: Translations take extremely long (10+ minutes for 1-hour interviews)
- **No User Feedback**: Users cannot see progress or cancel in-progress translations
- **Blocking UI**: Current synchronous implementation blocks the UI thread
- **Resource Exhaustion**: Large single requests may hit provider token limits

### 1.2 User Impact
- Unable to translate long-form content (interviews, transcripts, research papers)
- Poor user experience with frozen/unresponsive UI during translation
- Wasted API credits on failed translations
- Loss of user trust in translation feature

---

## 2. Functional Requirements

### FR-1: Content Chunking
**Description**: The system must automatically split long content into smaller, translatable chunks.

**Requirements**:
1. **FR-1.1**: Automatically chunk content exceeding 2000 characters
2. **FR-1.2**: Chunk boundaries MUST respect paragraph boundaries (never break mid-paragraph)
3. **FR-1.3**: Each chunk MUST maintain structure context (TranslatableText metadata)
4. **FR-1.4**: Chunks MUST NOT exceed provider token limits (max 4000 tokens ~ 16,000 characters)
5. **FR-1.5**: Implement semantic chunking when possible (section boundaries, heading breaks)

**Acceptance Criteria**:
- [ ] Content with 50,000 characters is split into ~25 chunks of ~2000 characters each
- [ ] No chunk breaks within a paragraph or list item
- [ ] Each chunk preserves element type and nesting level metadata
- [ ] Chunking algorithm handles edge cases (tables, code blocks, nested structures)

### FR-2: Asynchronous Processing
**Description**: Translation must occur asynchronously without blocking the UI.

**Requirements**:
1. **FR-2.1**: Translation MUST execute in background coroutine (Dispatchers.IO)
2. **FR-2.2**: UI remains responsive during translation
3. **FR-2.3**: Support multiple concurrent translations (different articles)
4. **FR-2.4**: Implement cancellation support for in-progress translations

**Acceptance Criteria**:
- [ ] User can navigate away from article while translation progresses
- [ ] User can continue reading other articles
- [ ] User can cancel translation via cancel button
- [ ] App rotation does not cancel translation

### FR-3: Progress Reporting
**Description**: Users must see real-time translation progress.

**Requirements**:
1. **FR-3.1**: Display progress indicator (percentage complete)
2. **FR-3.2**: Show current chunk being translated (e.g., "Translating chunk 5/25")
3. **FR-3.3**: Update progress in real-time as chunks complete
4. **FR-3.4**: Handle edge cases (progress unknown, network failure)

**Acceptance Criteria**:
- [ ] Progress bar updates from 0% to 100% during translation
- [ ] Text indicator shows "Translating chunk X of Y"
- [ ] Progress survives configuration changes (screen rotation)
- [ ] Progress hidden when translation completes or fails

### FR-4: Parallel Chunk Translation
**Description**: Translate chunks in parallel to reduce total time.

**Requirements**:
1. **FR-4.1**: Translate up to 3 chunks concurrently
2. **FR-4.2**: Respect provider rate limits (avoid 429 errors)
3. **FR-4.3**: Implement exponential backoff on rate limit errors
4. **FR-4.4**: Maintain chunk order when assembling results

**Acceptance Criteria**:
- [ ] 25-chunk article completes in ~1/3 of sequential time
- [ ] No 429 (rate limit) errors from provider
- [ ] Translated paragraphs maintain original order
- [ ] Failed chunks trigger retry (max 3 attempts)

### FR-5: Error Handling & Recovery
**Description**: Gracefully handle translation failures.

**Requirements**:
1. **FR-5.1**: Individual chunk failures MUST NOT fail entire translation
2. **FR-5.2**: Retry failed chunks up to 3 times with exponential backoff
3. **FR-5.3**: Display partial results if some chunks fail permanently
4. **FR-5.4**: Show clear error messages to user
5. **FR-5.5**: Support "retry failed chunks" action

**Acceptance Criteria**:
- [ ] Translation with 1 failed chunk shows 24/25 translated
- [ ] Failed chunks marked with placeholder (e.g., "[Translation failed]")
- [ ] User can tap to retry failed chunks
- [ ] Network errors show user-friendly message
- [ ] Provider errors (quota, timeout) are clearly communicated

### FR-6: Result Assembly
**Description**: Combine translated chunks into final result.

**Requirements**:
1. **FR-6.1**: Assemble chunks in original order
2. **FR-6.2**: Preserve HTML structure and metadata
3. **FR-6.3**: Handle missing/failed chunks gracefully
4. **FR-6.4**: Cache results to avoid re-translation

**Acceptance Criteria**:
- [ ] Assembled translation matches original structure
- [ ] Missing chunks show placeholders
- [ ] Cached translation loads instantly on re-open
- [ ] Cache invalidates on target language change

---

## 3. Non-Functional Requirements

### NFR-1: Performance
- **Translation Speed**: 1-hour interview (~30,000 words) translates in < 5 minutes
- **Chunk Processing**: Each chunk processes in < 30 seconds
- **UI Responsiveness**: UI remains < 16ms frame time during translation
- **Memory Usage**: Translation overhead < 100MB additional memory

### NFR-2: Reliability
- **Success Rate**: > 95% of long-form translations complete successfully
- **Retry Success**: > 80% of failed chunks succeed on retry
- **Crash Recovery**: Translation state persists across app crashes
- **Network Resilience**: Translations resume after temporary network loss

### NFR-3: Scalability
- **Content Size**: Support articles up to 100,000 characters (~25,000 words)
- **Concurrent Translations**: Support 3 simultaneous article translations
- **Provider Flexibility**: Work with OpenAI, Anthropic, and compatible providers

### NFR-4: Usability
- **Progress Visibility**: User always sees translation progress
- **Cancellability**: User can cancel any time
- **Error Clarity**: Error messages are actionable and clear
- **Consistency**: Behavior consistent across short and long content

---

## 4. User Experience Requirements

### UX-1: Progress Indication
**Before Translation**:
- Show "Translate" button as current

**During Translation**:
- Replace button with progress bar (0-100%)
- Show text: "Translating chunk X of Y..."
- Show "Cancel" button alongside progress

**After Translation**:
- Hide progress indicator
- Show translated content
- Show "Translated to [Language]" badge

**On Failure**:
- Show error message with retry option
- Show partial results if available
- Provide "Retry" button

### UX-2: User Actions
1. **Cancel Translation**: Stop translation, clear partial results
2. **Retry Failed**: Re-attempt translation for failed chunks only
3. **Re-translate**: Clear cache and translate entire article again
4. **Copy Translation**: Copy full translation to clipboard

### UX-3: Background Behavior
- Translation continues if user:
  - Navigates to different screen
  - Minimizes app
  - Locks device
- Translation pauses if user:
  - Forces app closed
  - Revokes network permission
- Translation resumes when user returns (if not canceled)

---

## 5. Technical Constraints

### TC-1: Android Platform
- **Lifecycle Awareness**: Must handle configuration changes (rotation, multi-window)
- **Background Processing**: Use WorkManager for translations continuing after app closed
- **Foreground Service**: Required for translations > 1 minute with notification
- **Battery Optimization**: Respect Doze mode and App Standby

### TC-2: LLM Provider Limits
- **Token Limits**: OpenAI 128k, Anthropic 200k (but practical limit ~16k for reliability)
- **Rate Limits**: Implement rate limiting and backoff
- **Timeout**: Provider-side timeout typically 60-120 seconds
- **Cost**: Multiple small requests vs single large request (minimal difference)

### TC-3: State Management
- **Persistence**: Save translation state to database
- **Restoration**: Restore state after process death
- **Synchronization**: Prevent race conditions with concurrent translations

### TC-4: Existing Architecture
- **Kotlin Coroutines**: Use coroutine-based async patterns
- **Repository Pattern**: Integrate with existing Repository
- **MVVM**: Use ViewModel for UI state management
- **Dependency Injection**: Use Kodein DI for dependencies

---

## 6. Edge Cases & Error Scenarios

### Edge Cases to Handle
1. **Empty Content**: Article with no translatable text
2. **Single Paragraph**: Content too short to chunk
3. **Single Chunk**: Content fits in one chunk
4. **Odd Number of Chunks**: Last chunk smaller than target size
5. **Special Characters**: Unicode, emojis, right-to-left text
6. **Code Blocks**: Pre-formatted text that shouldn't be split
7. **Nested Structures**: Deeply nested lists/quotes
8. **Mixed Languages**: Content already partially translated
9. **Network Fluctuation**: Intermittent connectivity
10. **Provider Switch**: User changes AI provider mid-translation

### Error Scenarios
1. **Timeout**: Single chunk times out (retry with larger timeout)
2. **Rate Limit**: Provider returns 429 (backoff and retry)
3. **Quota Exceeded**: User hits API quota (show upgrade message)
4. **Invalid Response**: Malformed JSON from provider (retry or skip)
5. **Network Error**: No connectivity (queue for retry)
6. **App Crash**: Process killed (restore and resume)
7. **User Cancel**: Explicit cancellation (clear partial results)
8. **Language Change**: Target language changed (invalidate cache)

---

## 7. Success Metrics

### Quantitative Metrics
- **Translation Success Rate**: > 95% of long-form translations complete
- **Translation Speed**: 30,000-word article in < 5 minutes
- **User Cancellation Rate**: < 10% of translations canceled
- **Error Rate**: < 5% of chunks fail permanently
- **Cache Hit Rate**: > 80% of repeat views load from cache

### Qualitative Metrics
- **User Satisfaction**: Positive feedback on translation speed
- **Feature Adoption**: Increased usage of translation for long articles
- **Support Tickets**: Decrease in translation-related complaints
- **User Retention**: Users continue using app for long-form content

---

## 8. Open Questions & Clarifications

### Questions for User/Product Owner
1. **Target Chunk Size**: Is 2000 characters (~500 words) appropriate, or should this be user-configurable?
2. **Parallelism**: Is 3 concurrent chunks appropriate, or should this be user-configurable?
3. **Retry Strategy**: Should failed chunks be auto-retried, or require user confirmation?
4. **Partial Results**: Should partial results be displayed immediately, or wait for completion?
5. **Background Translation**: Should translation continue after app is closed (require foreground service notification)?

### Default Assumptions (pending user confirmation)
1. Target chunk size: 2000 characters
2. Concurrency: 3 chunks in parallel
3. Auto-retry: Yes, up to 3 attempts
4. Partial results: Display as they complete
5. Background translation: Yes, with notification

---

## 9. Dependencies & Integration Points

### Dependencies
- Existing AIApi class
- Repository for settings and state
- TranslatableText model
- AI providers (OpenAI, Anthropic)

### Integration Points
- Translation UI (screen showing progress)
- Article detail screen
- Settings screen (chunk size, concurrency)
- Database (translation state cache)

---

## 10. Glossary

- **Chunk**: A portion of content split for translation
- **Async Processing**: Non-blocking execution in background thread
- **Parallel Translation**: Multiple chunks translating simultaneously
- **Exponential Backoff**: Retry strategy with increasing delays
- **Token**: LLM processing unit (~4 characters)
- **Rate Limit**: Provider restriction on request frequency
- **Foreground Service**: Android component for long-running background tasks

---

## Appendix: Current Implementation Analysis

### Current Translation Flow (Simplified)
1. User taps "Translate"
2. AIApi.translate() called synchronously
3. All paragraphs sent in single request
4. UI blocks until response or timeout
5. Results displayed or error shown

### Problems with Current Flow
- Single large request may timeout
- No progress visibility
- UI becomes unresponsive
- No cancellation support
- No retry for partial failures

---

**Document Version**: 1.0
**Last Updated**: 2026-01-07
**Next Review**: After user feedback on open questions
