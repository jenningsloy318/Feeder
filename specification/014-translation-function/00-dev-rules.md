# Phase 0: Development Rules Assessment

**Project**: Feeder - RSS Reader Application
**Feature**: AI Translation Function Implementation
**Date**: 2026-01-03
**Phase**: 0 - Apply Dev Rules

---

## Project Overview

**Type**: Android Application (Kotlin)
**Framework**: Jetpack Compose
**Architecture**: MVVM with Repository Pattern
**DI Framework**: Kodein DI
**Async**: Kotlin Coroutines & Flow

---

## Coding Standards and Conventions

### 1. Language and Framework

**Primary Language**: Kotlin
- Use Kotlin idiomatic patterns
- Favor immutable data classes
- Use sealed interfaces for result types
- Leverage extension functions for utilities

**UI Framework**: Jetpack Compose
- Composable functions for UI
- State management with StateFlow and remember
- Navigation with Compose Navigation
- Material Design 3 components

### 2. Architecture Patterns

**MVVM Pattern**:
```
View (Compose) -> ViewModel -> Repository -> Data Source
```

**Repository Pattern**:
- Single source of truth for data
- Exposes StateFlow for reactive updates
- Handles business logic
- Abstracts data sources (local DB, remote API)

**Dependency Injection**:
- Kodein DI for dependency management
- Constructor injection preferred
- DI modules organized by feature

### 3. Code Organization

**Package Structure**:
```
com.nononsenseapps.feeder/
├── ai/                    # AI features (summary, translation)
│   ├── AIClient.kt       # Unified AI client interface
│   ├── AIApi.kt         # High-level AI API
│   ├── model/           # Data models
│   └── provider/        # Provider implementations
├── ui/
│   └── compose/
│       ├── navigation/  # Navigation setup
│       └── settings/    # Settings screens
├── model/              # Domain models
├── db/                 # Room database
├── di/                 # DI modules
└── archmodel/          # Architecture components (Repository, etc.)
```

### 4. Naming Conventions

**Files**: PascalCase (e.g., `AIClient.kt`, `TranslationSettingsViewModel.kt`)

**Functions**: camelCase (e.g., `generateSummary()`, `translateParagraphs()`)

**Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`, `DEFAULT_TIMEOUT`)

**Private Properties**: camelCase with possible prefix (e.g., `client`, `_settings`)

### 5. Error Handling

**Sealed Results Pattern**:
```kotlin
sealed interface TranslationResult {
    val content: String

    data class Success(
        val paragraphs: List<String>
    ) : TranslationResult {
        override val content: String
            get() = paragraphs.joinToString("\n\n")
    }

    data class Error(
        override val content: String
    ) : TranslationResult
}
```

**Try-Catch Pattern**:
```kotlin
suspend fun translate(): TranslationResult {
    return try {
        // Operation
        TranslationResult.Success(result)
    } catch (e: Exception) {
        TranslationResult.Error(e.message ?: "Translation failed")
    }
}
```

### 6. Coroutines and Flow

**Repository Pattern with Flow**:
```kotlin
val translationLanguage: StateFlow<TranslationLanguage>
    get() = store.translationLanguage.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = TranslationLanguage.AUTO_DETECT,
    )
```

**Coroutine Context**:
- Use `Dispatchers.IO` for network/database operations
- Use `Dispatchers.Default` for CPU-intensive work
- Main thread automatically handled in Compose

### 7. Testing Strategy

**Unit Tests**:
- ViewModel testing with StateFlow verification
- Repository testing with fake data sources
- Utility function testing

**Integration Tests**:
- End-to-end AI provider calls
- Database operations
- UI integration with Compose testing

### 8. Code Quality Standards

**Complexity Management**:
- Maximum cyclomatic complexity: 10
- Function length: < 50 lines preferred
- Class length: < 300 lines preferred
- Extract frequently used patterns into extensions

**Documentation**:
- KDoc comments for public APIs
- Inline comments for complex logic
- TODO markers for temporary implementations

### 9. AI Integration Patterns

**Current Implementation** (Spec 11 & 13):
- AI provider abstraction (OpenAI, Anthropic)
- Settings management via Repository
- Translation settings screen implemented
- Translation button on article page
- Paragraph-by-paragraph display ready
- **Status**: Stub implementation, needs real AI calls

**Provider Interface**:
```kotlin
interface AIClient {
    suspend fun translate(paragraphs: List<String>): TranslationResult
}
```

### 10. Key Constraints for This Feature

**Translation Requirements**:
1. Send full article content in ONE request (not paragraph-by-paragraph)
2. Index each paragraph to map translations back to originals
3. Support both auto-translate and manual modes
4. Use default provider from AI Integration settings

**Performance**:
- Single API call per article translation
- Avoid rate limits by batching content
- Show loading state during translation

**User Experience**:
- Smooth paragraph-by-paragraph display
- Toggle between original and translation
- Remember translation preference per article

---

## Implementation Guidelines

### DO:
- Follow existing MVVM pattern
- Use sealed interfaces for results
- Leverage Flow for reactive state
- Keep functions short and focused
- Document public APIs with KDoc
- Handle errors gracefully with user-friendly messages
- Use coroutine dispatchers appropriately

### DON'T:
- Create new architectural patterns
- Ignore existing AI infrastructure
- Block main thread with network calls
- Use callback-style async patterns
- Expose implementation details
- Skip error handling
- Duplicate existing code (DRY principle)

---

## Success Criteria

1. Code follows all project conventions
2. No architectural deviations
3. Proper error handling in all async operations
4. Clean separation of concerns
5. Comprehensive test coverage
6. No code duplication
7. Performance-optimized (single API call)
8. User-friendly error messages

---

## Next Steps

Proceed to **Phase 1: Specification Setup** to create the spec directory structure and begin requirements gathering.
