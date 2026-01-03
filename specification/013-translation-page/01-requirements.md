# Requirements Document - Article Translation Button Feature

**Spec Index:** 013
**Feature Name:** Article Translation Button
**Date:** 2026-01-03
**Status:** Requirements Clarified

## Overview

Add a translation button to the article page that allows users to manually translate article content on-demand. The translated text will be displayed paragraph-by-paragraph below each original paragraph.

## Functional Requirements

### FR1: Translation Button in Top App Bar
- Add a translation button to the article screen's top app bar
- Button placement: After "Summarize" button
- Button sequence: "Fetch Full Article" → "Summarize" → "Translate"
- Icon: `Icons.Default.Translate` (Material Icons standard)
- Tooltip: "Translate" string resource

### FR2: Translation Trigger
- Clicking the translation button triggers article translation
- Translation works regardless of auto-translate setting in configuration
- Manual translation is independent from the auto-translate feature setting

### FR3: Loading State
- Display loading indicator identical to AI summary feature
- Use `LinearProgressIndicator` component
- Show progress at top of article content during translation
- Button remains enabled during translation (no click-to-cancel)

### FR4: Translation Display Format
- Translated content displayed paragraph-by-paragraph
- Format for each paragraph:
  ```
  [Original paragraph 1 text]
  [Translated paragraph 1 text]

  [Original paragraph 2 text]
  [Translated paragraph 2 text]
  ```
- Translated text visually distinct from original (styling to be determined in UI/UX design)

### FR5: Translation API Approach
- Send all article paragraphs to AI in single request
- Receive all translated paragraphs in single response
- No streaming or progressive translation display

### FR6: Error Handling
- On translation failure, display error message at top of article content
- Users can tap the translate button again to retry
- Error state persists until user navigates away or taps translate button again

### FR7: No Persistence
- Do NOT save translations to database
- Always re-translate on each view
- No caching of translated content

### FR8: No Cancellation
- Users cannot cancel in-progress translation
- Translation button does not change to cancel button
- Translation completes even if user navigates away (background processing)

## Non-Functional Requirements

### NFR1: Performance
- Translation should complete within reasonable time (target: < 5 seconds for typical articles)
- UI remains responsive during translation
- No blocking of main thread

### NFR2: Code Quality
- Follow existing project code patterns
- Reuse existing components where possible
- Maintain consistency with AI summary feature implementation
- Proper error handling and logging

### NFR3: User Experience
- Translation button only visible when article content is loaded
- Clear visual feedback during translation
- Error messages are user-friendly
- Translated text is readable and properly formatted

## Technical Constraints

### TC1: Language Support
- Must respect `TranslationLanguage` setting from existing configuration
- Support all languages defined in `TranslationLanguage` enum
- Handle DEVICE_DEFAULT language option

### TC2: AI Integration
- Use existing AI API infrastructure
- Integration point will be implemented in next specification (use dummy function for now)
- Prepare for future AI translation implementation

### TC3: Article Content Structure
- Work with existing `LinearArticle` content model
- Handle various element types (text, images, lists, etc.)
- Only translate `LinearText` elements, skip media/structural elements

## Dependencies

### Internal Dependencies
- `ArticleScreen.kt` - Add button to top app bar
- `ArticleViewModel.kt` - Add translation state and logic
- `AISummaryState` pattern - Reference for translation state management
- `TranslationLanguage` enum - Get target language from settings
- `LinearArticleContent.kt` - Modify to display translations

### External Dependencies
- Material Icons (`Icons.Default.Translate`)
- Jetpack Compose UI components
- Kotlin coroutines for async operations
- Kodein DI for dependency injection

## Out of Scope

The following are explicitly out of scope for this specification:
- Actual AI translation implementation (deferred to future spec)
- Auto-translation on article open
- Translation caching or persistence
- Translation history
- Batch translation of multiple articles
- Translation settings integration (beyond reading target language)
- Cancellation of in-progress translation
- Translation quality feedback mechanism

## Success Criteria

- [ ] Translation button appears in top app bar at correct position
- [ ] Clicking button triggers translation with loading indicator
- [ ] Translated paragraphs appear below original paragraphs
- [ ] Error handling displays message and allows retry via translate button
- [ ] Translation respects target language setting
- [ ] Code compiles and passes all existing tests
- [ ] Feature works on both phone and tablet layouts
- [ ] No regressions in existing article viewing functionality

## Notes

- This feature prepares the UI and integration points for AI translation
- Actual translation API call will use dummy implementation for now
- Future spec will implement real AI translation using configured AI provider
- Design should be flexible to accommodate different AI providers
