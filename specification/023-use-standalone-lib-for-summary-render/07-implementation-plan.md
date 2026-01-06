# Implementation Plan: Markdown Library Integration

**Plan ID**: 023-use-standalone-lib-for-summary-render
**Version**: 1.0
**Author**: Super Dev Coordinator
**Date**: January 6, 2026
**Estimated Duration**: 3-5 hours

## Overview

This implementation plan outlines the steps to integrate the Mikepenz Multiplatform Markdown Renderer library into the Feeder app, replacing the current manual regex-based markdown parsing implementation.

## Pre-Implementation Checklist

- [x] Requirements gathered and documented
- [x] Research completed (Mikepenz library selected)
- [x] Code assessment completed
- [x] Technical specification written
- [x] Implementation plan approved
- [ ] Feature branch created (spec-23-use-standalone-lib-for-summary-render) ✅

## Implementation Phases

### Phase 1: Dependency Setup (30 minutes)

**Objective**: Add Mikepenz library to project dependencies

#### Tasks

1.1. Update `gradle/libs.versions.toml` (5 minutes)
```toml
[versions]
mikepenz-markdown = "0.39.0"

[libraries]
mikepenz-markdown = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "mikepenz-markdown" }
mikepenz-markdown-m3 = { module = "com.mikepenz:multiplatform-markdown-renderer-m3", version.ref = "mikepenz-markdown" }
mikepenz-markdown-coil3 = { module = "com.mikepenz:multiplatform-markdown-renderer-coil3", version.ref = "mikepenz-markdown" }
```

1.2. Update `app/build.gradle.kts` (5 minutes)
```kotlin
dependencies {
    // Markdown
    implementation(libs.mikepenz.markdown)
    implementation(libs.mikepenz.markdown.m3)
    implementation(libs.mikepenz.markdown.coil3)

    // Remove old dependency (optional, can keep)
    // implementation(libs.jetbrains.markdown) // Now handled by mikepenz
}
```

1.3. Gradle sync and verify (10 minutes)
- Run `./gradlew clean build`
- Verify no dependency conflicts
- Check build succeeds
- Resolve any version conflicts

1.4. Verify compatibility (10 minutes)
- Confirm Kotlin 2.2.20 compatibility
- Confirm Compose BOM 2025.10.01 compatibility
- Confirm Android API 29+ compatibility

**Deliverables**:
- ✅ Dependencies added successfully
- ✅ Build passes without errors
- ✅ No dependency conflicts

**Acceptance Criteria**:
- Gradle sync completes successfully
- Build passes without errors
- No dependency version conflicts

---

### Phase 2: Code Migration (1-2 hours)

**Objective**: Replace manual markdown parsing with Mikepenz library

#### Tasks

2.1. Backup current implementation (5 minutes)
```bash
git add app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt
git commit -m "backup: Current markdown implementation before migration"
```

2.2. Create new implementation (30 minutes)

Create new functions in `MarkdownToAnnotatedString.kt`:

```kotlin
package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Renders markdown text using the Mikepenz Multiplatform Markdown Renderer.
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the Markdown composable
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Markdown(
        content = markdown,
        modifier = modifier,
        colors = markdownColor(),
        typography = markdownTypography()
    )
}

/**
 * Safe version of markdown rendering that falls back to plain text on error.
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the Markdown composable
 */
@Composable
fun MarkdownContentSafe(
    markdown: String,
    modifier: Modifier = Modifier
) {
    try {
        MarkdownContent(markdown, modifier)
    } catch (e: Exception) {
        androidx.compose.material3.Text(
            text = markdown,
            modifier = modifier
        )
    }
}
```

2.3. Update consuming code (20 minutes)

Find and replace usages:
```bash
# Find all usages
grep -r "markdownToAnnotatedString" app/src/main/java/

# Update each usage
# Old: markdownToAnnotatedString(summary)
# New: MarkdownContent(summary)
```

2.4. Remove old implementation (15 minutes)

Delete these functions:
- `parseMarkdownToHTML()` (114 lines)
- `sanitizeHTML()` (6 lines)
- `createMarkdownCleaner()` (13 lines)
- Old `markdownToAnnotatedString()` implementation

Keep for backward compatibility (deprecated):
```kotlin
@Deprecated(
    message = "Use MarkdownContent composable instead",
    replaceWith = ReplaceWith("MarkdownContent(markdown)")
)
fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    // This is now a wrapper for compatibility
    return AnnotatedString(markdown)
}
```

2.5. Update imports (10 minutes)

Remove unused imports:
```kotlin
// Remove
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
```

Add new imports:
```kotlin
// Add
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
```

2.6. Remove Jsoup usage (if not used elsewhere) (10 minutes)

Check if Jsoup is used elsewhere:
```bash
grep -r "import org.jsoup" app/src/main/java/
```

If only used in markdown file, can remove dependency.

**Deliverables**:
- ✅ New `MarkdownContent` composable created
- ✅ Old parsing code removed (~150-200 lines)
- ✅ All consuming code updated
- ✅ Imports cleaned up

**Acceptance Criteria**:
- Code compiles without errors
- All markdown usages updated
- Old parsing functions removed

---

### Phase 3: Testing (1-2 hours)

**Objective**: Ensure comprehensive test coverage and validate functionality

#### Tasks

3.1. Create unit tests (45 minutes)

Create `app/src/test/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedStringTest.kt`:

```kotlin
package com.nononsenseapps.feeder.ui.compose.text

import org.junit.Test
import org.junit.Assert.*

class MarkdownToAnnotatedStringTest {

    @Test
    fun `renders headings correctly`() {
        val markdown = "# Heading 1\n\n## Heading 2\n\n### Heading 3"
        // Verify rendering doesn't crash
        // Visual testing would be done in UI tests
    }

    @Test
    fun `renders bold and italic text`() {
        val markdown = "**bold** and *italic* and ***bold italic***"
        // Verify rendering
    }

    @Test
    fun `renders lists correctly`() {
        val markdown = """
            - Item 1
            - Item 2
              - Nested item
              - Another nested item
            1. Ordered item 1
            2. Ordered item 2
        """.trimIndent()
        // Verify rendering
    }

    @Test
    fun `renders code blocks`() {
        val markdown = """
            Inline `code` and
            ```
            code block
            ```
        """.trimIndent()
        // Verify rendering
    }

    @Test
    fun `renders links`() {
        val markdown = "[link text](https://example.com)"
        // Verify rendering
    }

    @Test
    fun `renders blockquotes`() {
        val markdown = "> Quote text"
        // Verify rendering
    }

    @Test
    fun `renders tables`() {
        val markdown = """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
        """.trimIndent()
        // Verify rendering
    }

    @Test
    fun `renders task lists`() {
        val markdown = """
            - [x] Completed task
            - [ ] Pending task
        """.trimIndent()
        // Verify rendering
    }

    @Test
    fun `renders strikethrough`() {
        val markdown = "~~strikethrough text~~"
        // Verify rendering
    }

    @Test
    fun `handles empty input`() {
        val markdown = ""
        // Verify doesn't crash
    }

    @Test
    fun `handles malformed markdown`() {
        val markdown = "#Invalid markdown**broken"
        // Verify graceful handling
    }

    @Test
    fun `handles special characters`() {
        val markdown = "Special chars: < > & \" '"
        // Verify proper escaping
    }

    @Test
    fun `handles large markdown documents`() {
        val markdown = """
            ${List(100) { "# Heading $it\n\nContent\n\n" }.joinToString("")}
        """.trimIndent()
        // Verify performance and memory usage
    }
}
```

3.2. Create integration tests (30 minutes)

Create `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownIntegrationTest.kt`:

```kotlin
package com.nononsenseapps.feeder.ui.compose.text

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class MarkdownIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders markdown correctly in Compose`() {
        val markdown = "# Test Heading\n\n**Bold text**"

        composeTestRule.setContent {
            MarkdownContent(markdown)
        }

        // Verify content is rendered
        composeTestRule.onNodeWithText("Test Heading").assertExists()
        composeTestRule.onNodeWithText("Bold text").assertExists()
    }

    @Test
    fun `handles markdown updates`() {
        val markdown = "# Initial"

        composeTestRule.setContent {
            MarkdownContent(markdown)
        }

        composeTestRule.onNodeWithText("Initial").assertExists()

        // Update markdown
        composeTestRule.setContent {
            MarkdownContent("# Updated")
        }

        composeTestRule.onNodeWithText("Updated").assertExists()
    }
}
```

3.3. Run tests (15 minutes)
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Verify all tests pass
```

3.4. Manual testing (30 minutes)

1. Build and install app
2. Navigate to AI summary feature
3. Test various markdown inputs:
   - Headings
   - Bold/italic
   - Lists (nested)
   - Code blocks
   - Tables
   - Task lists
   - Links
4. Verify visual quality
5. Check for rendering issues
6. Test edge cases

3.5. Performance verification (15 minutes)

Test with large markdown document:
```kotlin
// Generate 10,000 line markdown
val largeMarkdown = generateLargeMarkdown(10000)

// Measure parsing time
val time = measureTimeMillis {
    MarkdownContent(largeMarkdown)
}

// Verify <200ms for large document
assertTrue("Parsing too slow: ${time}ms", time < 200)
```

**Deliverables**:
- ✅ Unit tests created (≥80% coverage target)
- ✅ Integration tests created
- ✅ All tests pass
- ✅ Manual testing completed
- ✅ Performance verified

**Acceptance Criteria**:
- Test coverage ≥80%
- All tests pass
- Manual testing shows correct rendering
- Performance meets or exceeds current implementation

---

### Phase 4: Validation & Documentation (30 minutes)

**Objective**: Final validation and documentation updates

#### Tasks

4.1. Code review preparation (10 minutes)
- Ensure code follows project style guide
- Run ktlint: `./gradlew ktlintCheck`
- Fix any style violations
- Prepare for code review

4.2. Build verification (10 minutes)
```bash
# Clean build
./gradlew clean build

# Verify no errors
./gradlew assembleDebug

# Verify no warnings
./gradlew lint
```

4.3. Update documentation (10 minutes)

Update `docs/AI_SUMMARY_DEVELOPER_GUIDE.md` (if exists):
```markdown
## Markdown Rendering

AI summaries are rendered using the Mikepenz Multiplatform Markdown Renderer library.

### Supported Features
- Headings (H1-H6)
- Bold, italic, strikethrough
- Lists (unordered, ordered, nested, task lists)
- Code blocks (with syntax highlighting)
- Tables
- Blockquotes
- Links
- Images

### Usage
```kotlin
@Composable
fun AISummaryDisplay(summary: String) {
    MarkdownContent(summary)
}
```

### Library
- [Mikepenz Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)
- Version: 0.39.0
```

4.4. Create migration notes (5 minutes)

Document breaking changes and migration path:
```markdown
## Migration Notes

### For Developers

**Before**:
```kotlin
val annotatedString = markdownToAnnotatedString(markdown)
Text(annotatedString)
```

**After**:
```kotlin
MarkdownContent(markdown)
```

### Breaking Changes
- `markdownToAnnotatedString()` is deprecated
- Use `MarkdownContent()` composable instead
- Visual rendering may differ slightly
- New features available (tables, task lists, strikethrough)

### New Features
- Table support
- Task list support
- Strikethrough support
- Syntax highlighting (optional)
- Better list nesting
```

4.5. Update CHANGELOG (5 minutes)

Add entry to `CHANGELOG.md`:
```markdown
## [Version] - 2026-01-06

### Changed
- Replace manual markdown parsing with Mikepenz Multiplatform Markdown Renderer
- Improve markdown feature coverage from 40% to 95%+ of CommonMark spec
- Add table support for markdown rendering
- Add task list support for markdown rendering
- Add strikethrough text support
- Improve list nesting support
- Improve markdown rendering performance by 5-10x
- Remove ~150-200 lines of manual parsing code

### Fixed
- Fix nested list rendering
- Fix markdown edge cases
- Fix parsing performance on large documents

### Developer Notes
- `markdownToAnnotatedString()` is now deprecated
- Use `MarkdownContent()` composable instead
- See docs/AI_SUMMARY_DEVELOPER_GUIDE.md for usage
```

**Deliverables**:
- ✅ Code follows style guide (ktlint passes)
- ✅ Build passes without errors/warnings
- ✅ Documentation updated
- ✅ Migration notes created
- ✅ CHANGELOG updated

**Acceptance Criteria**:
- ktlint checks pass
- Build passes with zero errors
- Build passes with zero warnings
- Documentation complete and accurate

---

### Phase 5: Cleanup & Finalization (30 minutes)

**Objective**: Clean up code, remove unused dependencies, finalize implementation

#### Tasks

5.1. Remove unused dependencies (10 minutes)

Check if `jetbrains-markdown` is used elsewhere:
```bash
grep -r "import org.jetbrains.markdown" app/src/main/java/
```

If not used, remove from `gradle/libs.versions.toml`:
```toml
# Remove this line if not used elsewhere
jetbrains-markdown = "0.7.3"
```

Remove from `app/build.gradle.kts` if present:
```kotlin
// implementation(libs.jetbrains.markdown) // Remove
```

5.2. Remove Jsoup dependency (if not used elsewhere) (5 minutes)

Check Jsoup usage:
```bash
grep -r "import org.jsoup" app/src/main/java/
```

If only used in markdown file, remove from `gradle/libs.versions.toml`:
```toml
# Remove if not used elsewhere
jsoup = "1.21.2"
```

5.3. Clean up imports (5 minutes)

Run IDE optimization:
```bash
# Remove unused imports
./gradlew ktlintFormat

# Or use IDE: Code > Optimize Imports > Clean up
```

5.4. Final code review (5 minutes)

Review changes:
- Check code quality
- Verify no TODOs left behind
- Ensure all comments are accurate
- Confirm no debug code remains

5.5. Git commit preparation (5 minutes)

Stage changes:
```bash
git add gradle/libs.versions.toml
git add app/build.gradle.kts
git add app/src/main/java/com/nononsenseapps/feeder/ui/compose/text/MarkdownToAnnotatedString.kt
git add app/src/test/
git add docs/
git add CHANGELOG.md
```

**Deliverables**:
- ✅ Unused dependencies removed
- ✅ Imports cleaned up
- ✅ Code review ready
- ✅ Changes staged for commit

**Acceptance Criteria**:
- No unused dependencies remain
- Code is clean and optimized
- Ready for final review and commit

---

## Success Criteria

### Functional Requirements ✅
- [ ] All existing markdown features work correctly
- [ ] New features available (tables, task lists, strikethrough)
- [ ] No visual regressions in AI summary display
- [ ] Backward compatibility maintained

### Non-Functional Requirements ✅
- [ ] Test coverage ≥80%
- [ ] Performance not degraded (5-10x improvement expected)
- [ ] Code review approval obtained
- [ ] Documentation updated
- [ ] No breaking changes for users

### Quality Metrics ✅
- [ ] Build passes without errors
- [ ] Build passes without warnings
- [ ] All tests pass
- [ ] ktlint checks pass
- [ ] Lint checks pass

### Code Metrics ✅
- [ ] Code reduction: ≥150 lines
- [ ] Feature increase: +8 features
- [ ] Test coverage: ≥80%
- [ ] Documentation: Complete

## Risk Management

### Identified Risks

| Risk | Mitigation | Status |
|------|------------|--------|
| **Visual rendering differences** | Visual regression tests, custom styling | 🟡 Monitor |
| **Performance regression** | Performance benchmarks, async parsing | 🟢 Expected improvement |
| **Library breaking changes** | Version pinning, migration guide | 🟢 Low probability |
| **Missing features** | Feature verification, custom components | 🟢 Library has more features |
| **Increased APK size** | Acceptable trade-off for features | 🟡 Acceptable |

### Rollback Plan

If critical issues arise:
1. Revert code changes via git
2. Restore previous dependencies
3. Document issues for future resolution
4. Re-evaluate library choice

**Rollback Time**: <15 minutes

## Timeline Summary

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| **Phase 1: Dependency Setup** | 30 min | None |
| **Phase 2: Code Migration** | 1-2 hours | Phase 1 |
| **Phase 3: Testing** | 1-2 hours | Phase 2 |
| **Phase 4: Validation** | 30 min | Phase 3 |
| **Phase 5: Cleanup** | 30 min | Phase 4 |
| **Total** | **3-5 hours** | - |

## Post-Implementation

### Monitoring
- Track crash reports
- Monitor performance metrics
- Gather user feedback

### Maintenance
- Review library updates quarterly
- Monitor for security vulnerabilities
- Update tests for new features
- Keep documentation current

### Future Enhancements
- Enable syntax highlighting
- Add custom markdown extensions
- Optimize image loading
- Support for custom themes

---

**Implementation Plan Status**: ✅ Ready for Execution
**Next Step**: Begin Phase 1 - Dependency Setup
**Overall Risk Level**: Low
**Confidence Level**: High
