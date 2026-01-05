# Requirements: Improve Progress Bar for Summary and Translation

## User Story
As a user, I want to see different status messages when the app is summarizing or translating articles, so that I can understand what operation is currently in progress.

## Problem Statement
Currently, both the summary and translation features use the same progress indicator without any descriptive text. This makes it unclear to users which operation is being performed, especially when:
- The progress bar appears after clicking the summarize button
- The progress bar appears after clicking the translate button
- Both operations show identical loading states

## Requirements

### Functional Requirements
1. **Summary Progress Indicator**
   - When summarizing is in progress, display: "Summarizing..."
   - Show this text above or below the linear progress indicator
   - Use Material Design 3 typography guidelines

2. **Translation Progress Indicator**
   - When translating is in progress, display: "Translating..."
   - Show this text above or below the linear progress indicator
   - Use Material Design 3 typography guidelines

3. **Localization**
   - Both text strings must be translatable
   - Add entries to `strings.xml` for i18n support

### Non-Functional Requirements
1. **Performance**
   - No performance impact on loading operations
   - Text display should be immediate

2. **Consistency**
   - Follow Material Design 3 progress indicator patterns
   - Match existing app typography and spacing
   - Use same loading animation for both operations

3. **Accessibility**
   - Text should be readable for accessibility services
   - Proper content descriptions for screen readers
   - Sufficient color contrast

### Technical Requirements
1. **Code Changes Required**
   - Modify `SummarySection` composable in `ArticleScreen.kt`
   - Modify `TranslationStatusSection` composable in `ArticleScreen.kt`
   - Add string resources to `values/strings.xml`
   - Potentially update other language files if translations exist

2. **Testing**
   - Verify "Summarizing..." appears when summary is loading
   - Verify "Translating..." appears when translation is loading
   - Verify text disappears when operations complete
   - Verify error states still display correctly

## Acceptance Criteria
- [ ] Summary progress shows "Summarizing..." text
- [ ] Translation progress shows "Translating..." text
- [ ] Text is properly localized in English
- [ ] Progress indicator remains functional
- [ ] No visual glitches or layout issues
- [ ] Compatible with existing error states

## Out of Scope
- Changing the progress indicator animation style
- Adding percentage indicators
- Modifying the AI API calls
- Changing error handling logic
