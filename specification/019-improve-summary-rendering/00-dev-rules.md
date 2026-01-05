# Development Rules - Spec 019: Improve Summary Rendering

## Project Coding Standards

### Kotlin Code Style
- **Linting**: ktlint_official
- **Indentation**: 4 spaces
- **Max line length**: 200 characters
- **Charset**: UTF-8
- **Line endings**: LF
- **Final newline**: Required
- **Trailing whitespace**: Trimmed

### Compose-Specific Rules
- **Composition locals allowed**:
  - LocalTypographySettings
  - LocalDI
  - LocalDimens
  - LocalWindowSizeMetrics
  - LocalWindowSize
  - LocalFoldableHinge
- **Forwarding allowed**: .*Screen composables
- **Function naming**: @Composable annotated functions are exempt from standard naming checks

### Android/Compose Best Practices
- Use Jetpack Compose for UI
- Follow Material 3 design guidelines
- Use Kotlin Coroutines for async operations
- Use StateFlow for reactive state management
- Follow dependency injection with Kodein

### Testing Standards
- Write unit tests for business logic
- Write UI tests for Compose components
- Use AndroidX testing libraries
- Test both success and error paths

### Git Commit Standards
- Use conventional commit messages
- Commit frequently with small, focused changes
- Ensure all tests pass before committing
- Follow the project's commit message format

## Feature-Specific Rules

### Markdown Rendering Requirements
- Support CommonMark markdown specification
- Handle markdown rendering safely (XSS prevention)
- Maintain consistent styling with article content
- Support both light and dark themes
- Ensure proper text wrapping and RTL support
- Handle markdown parsing errors gracefully

### Performance Considerations
- Lazy load markdown rendering
- Cache parsed markdown when possible
- Avoid blocking the UI thread
- Test with various markdown complexities

## Quality Gates
- All code must pass ktlint checks
- All tests must pass
- No console.log or debug code in commits
- Manual testing on actual device required
- Accessibility testing required
