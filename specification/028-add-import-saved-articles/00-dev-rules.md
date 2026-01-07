# Development Rules - Spec 28: Add Import Saved Articles

## Project Coding Standards

### File: `.editorconfig`

**Character Encoding**: UTF-8

**Indentation**:
- Style: Spaces (NOT tabs)
- Size: 4 spaces (2 for YAML, XML)
- Continuation indent: 8 spaces

**Line Endings**: LF (Unix-style)

**Line Length**: Maximum 200 characters for Kotlin files

**Whitespace**:
- Insert final new line: YES
- Trim trailing whitespace: YES

**Kotlin Style**: ktlint_official

**Compose-Specific Rules**:
- Allowed composition locals: LocalTypographySettings, LocalDI, LocalDimens, LocalWindowSizeMetrics, LocalWindowSize, LocalFoldableHinge
- Allowed forwarding: .*Screen

## Build Configuration

**Android SDK**:
- minSdk: 29 (Android 10)
- targetSdk: compileSdk (from version catalog)
- compileSdk: From version catalog

**Kotlin**:
- Toolchain: Java 17+
- JVM Target: JvmTarget (from Kotlin DSL)

**Key Plugins**:
- android.application
- kotlin.android
- kotlin.compose.compiler
- kotlin.ksp
- kotlin.parcelize
- kotlin.serialization
- ktlint.gradle

## Project Structure

**Package**: `com.nononsenseapps.feeder`

**Architecture**: MVVM with Compose UI
- Room database for persistence
- Kodein for dependency injection
- Coroutines for async operations
- Compose for UI

## Testing Standards

**Test Runner**: AndroidJUnitRunner (Espresso)

## Dependencies

From build.gradle.kts:
- Compose UI framework
- Room database with KSP
- Kotlin serialization
- Ktlint for code quality

## Development Philosophy

From global CLAUDE.md:
- **Incremental development**: Small commits, each compiles and passes tests
- **Learn from existing code**: Research and plan before implementing
- **Pragmatic over dogmatic**: Adapt to project reality
- **Clear intent over clever code**: Simple, understandable solutions
- Avoid over-design, keep code clean and simple
- Pay attention to cyclomatic complexity, maximize code reuse
- Modular design with design patterns
- Minimize changes, avoid touching other modules

## Quality Standards

Each commit must:
- Compile successfully
- Pass all existing tests
- Include new tests for new features
- Follow project formatting/linting rules

## Decision Framework Priority

1. Testability
2. Readability (6-month future understanding)
3. Consistency (project patterns)
4. Simplicity (simplest viable solution)
5. Reversibility (ease of future modification)

## Error Handling

- Maximum 3 attempts before stopping
- Document failure reasons and error details
- Research 2-3 alternative implementations
- Question assumptions: over-abstracted? can be decomposed?
