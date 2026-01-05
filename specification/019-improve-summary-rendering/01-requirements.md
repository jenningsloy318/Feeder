# Requirements Document - Spec 019: Improve Summary Rendering

## User Story

As a user reading AI-generated article summaries, I want the summaries to be properly formatted with markdown support so that I can better understand and engage with the summarized content.

## Problem Statement

Currently, AI-generated summaries are rendered as plain text in the Feeder app. However, AI models may return markdown-formatted summaries with:
- Bold and italic text for emphasis
- Lists for organized information
- Headers for structure
- Links to relevant resources
- Code blocks for technical content

Without markdown rendering, this formatting is lost and users see raw markdown syntax, which reduces readability and the overall quality of the AI summary feature.

## Current Behavior

- Summary is displayed in `ArticleScreen.kt` at line 624-627
- Uses a simple `Text` composable with `summary.value.content`
- No markdown parsing or formatting
- Summary data comes from `AIClient.SummaryResult.content` (String)

## Desired Behavior

### Functional Requirements

#### FR1: Markdown Parsing
- The system must parse markdown-formatted summaries from AI responses
- Must support CommonMark markdown specification
- Must handle malformed markdown gracefully (fallback to plain text)

#### FR2: Markdown Rendering
- Must render bold text (**bold**)
- Must render italic text (*italic*)
- Must render links ([text](url))
- Must render unordered lists (- item)
- Must render ordered lists (1. item)
- Must render headers (# Header)
- Must render code blocks (```code```) and inline code (`code`)
- Must render blockquotes (> quote)

#### FR3: Styling Consistency
- Markdown elements must be styled consistently with article content
- Must respect Material 3 theme (colors, typography)
- Must support both light and dark themes
- Links must be tappable and open in appropriate viewer

#### FR4: Safety
- Must sanitize HTML to prevent XSS attacks
- Must handle malicious markdown safely
- Must limit image rendering in markdown (or disable entirely)

#### FR5: Performance
- Markdown parsing must not block UI thread
- Must handle large summaries without performance issues
- Should cache parsed markdown when possible

### Non-Functional Requirements

#### NFR1: Compatibility
- Must work with existing AI providers (OpenAI, Anthropic)
- Must not break existing summary functionality
- Must be backward compatible with plain text summaries

#### NFR2: Accessibility
- Screen readers must read formatted content properly
- Proper content descriptions for links
- Sufficient color contrast for formatted text

#### NFR3: Internationalization
- Must support RTL (Right-to-Left) languages
- Must work with translated markdown content
- Must handle various language scripts correctly

## Acceptance Criteria

### AC1: Basic Markdown Formatting
Given I have an AI-generated summary with markdown formatting
When I view the article
Then I see the summary with proper formatting (bold, italic, lists)

### AC2: Links in Summaries
Given I have an AI-generated summary with markdown links
When I tap on a link
Then the link opens in the appropriate browser/viewer

### AC3: Code Blocks
Given I have an AI-generated summary with code blocks
When I view the article
Then code is displayed in a monospaced font with proper styling

### AC4: Error Handling
Given I have an AI-generated summary with malformed markdown
When I view the article
Then the summary displays as plain text (fallback behavior)

### AC5: Performance
Given I have a very long AI-generated summary with complex markdown
When I view the article
Then the summary renders smoothly without lag

### AC6: Theme Support
Given I have the app in dark mode
When I view an AI-generated summary
Then the markdown styling matches the dark theme

## Edge Cases to Consider

1. **Empty summaries**: Should display placeholder or nothing
2. **Very long summaries**: Should handle gracefully without performance issues
3. **Nested markdown**: Should handle complex nested structures
4. **Malformed markdown**: Should fallback gracefully
5. **Mixed content**: Should handle markdown with plain text mixed
6. **Special characters**: Should escape special characters properly
7. **RTL content**: Should handle right-to-left text correctly
8. **Large code blocks**: Should handle large code blocks without overflow

## Constraints

- Must use Kotlin/Compose for Android
- Must maintain compatibility with existing codebase
- Should minimize library dependencies (prefer lightweight solutions)
- Must follow project's ktlint code style
- Must not significantly increase app size

## Dependencies

- Existing AI summary infrastructure
- Jetpack Compose UI framework
- Material 3 design system
- Potential markdown parsing library (to be determined in research phase)

## Out of Scope

- Editing markdown in summaries
- Rich text editor for markdown
- Custom markdown syntax extensions
- Markdown preview mode
- Exporting summaries with formatting
