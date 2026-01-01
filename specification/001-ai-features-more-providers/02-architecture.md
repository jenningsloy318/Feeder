# Architecture: AI Features - Multiple Providers

**Feature ID**: 001
**Status**: Implemented (Phase 1), Bug Fixes Applied (Phase 2)
**Created**: 2025-12-31
**Last Updated**: 2026-01-01

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Decision Records](#architecture-decision-records)
3. [Design Patterns](#design-patterns)
4. [Architecture Diagrams](#architecture-diagrams)
5. [Component Architecture](#component-architecture)
6. [Data Flow](#data-flow)
7. [State Management](#state-management)
8. [Phase 2 Changes](#phase-2-changes)

---

## Overview

This document describes the architecture for Feeder's multi-provider AI feature, which supports both OpenAI-compatible and Anthropic Claude providers through a unified interface.

### Architecture Principles

| Principle | Implementation |
|-----------|----------------|
| **Separation of Concerns** | Provider logic isolated from UI |
| **Dependency Inversion** | UI depends on `AIClient` interface, not concrete implementations |
| **Single Responsibility** | Each client handles only one provider |
| **Open/Closed** | New providers can be added without modifying existing code |
| **Reactive Programming** | Settings propagate automatically via StateFlow |

---

## Architecture Decision Records

### ADR-001: Multi-Provider AI Abstraction

**Status**: Accepted
**Date**: 2025-12-31
**Updated**: 2026-01-01 (Bug fixes documented)

#### Context

Feeder had hardcoded support for only OpenAI API. Users wanted to use other AI providers (Anthropic Claude, Azure OpenAI, DeepSeek, Perplexity) for article summarization. The existing implementation tightly coupled the UI to OpenAI-specific settings and client implementation.

#### Decision

Implement a multi-provider architecture using:

1. **Factory Pattern** for client creation
2. **Sealed Interface** for type-safe settings
3. **StateFlow + flatMapLatest** for reactive provider switching
4. **Official SDKs** instead of third-party wrappers

**Rationale**:
- Factory pattern enables adding new providers without modifying UI
- Sealed interfaces provide compile-time type safety
- Reactive flows ensure UI updates automatically when settings change
- Official SDKs are better maintained and support more features

#### Consequences

**Positive**:
- Easy to add new providers (just implement `AIClient` interface)
- Type-safe settings prevent invalid configurations
- Reactive architecture eliminates manual refresh logic
- Official SDKs provide better API coverage

**Negative**:
- Initial complexity increased
- More files to maintain
- Requires understanding of Kotlin flows

**Mitigation**:
- Comprehensive documentation
- Clear separation of concerns
- Consistent patterns across providers

---

### ADR-002: Reactive Settings with StateFlow

**Status**: Accepted
**Date**: 2025-12-31
**Bug Fix Applied**: 2026-01-01

#### Context

Settings need to be:
1. Persisted across app restarts
2. Reactive (UI updates automatically)
3. Type-safe per provider
4. Switchable without data loss

#### Decision

Use `StateFlow` + `flatMapLatest` pattern:

```kotlin
val aiSettingsFlow: StateFlow<AISettings> =
    _aiProviderType
        .flatMapLatest { provider ->
            when (provider) {
                AIProvider.OPENAI_COMPATIBLE ->
                    _openAISettings.mapLatest { ... }
                AIProvider.ANTHROPIC ->
                    _anthropicSettings.mapLatest { ... }
            }
        }
        .stateIn(...)
```

**Rationale**:
- `flatMapLatest` cancels previous flow when provider changes
- `stateIn` makes it hot and shareable
- Each provider's settings are independent

#### Phase 2 Bug Fix

**Issue**: Provider type not updated when settings changed
**Root Cause**: `UpdateSettings` event only updated provider-specific settings, not `aiProviderType`
**Fix**: Added `setAIProviderType()` calls in `SettingsViewModel.onOpenAISettingsEvent()`

**Impact**: Critical - without this fix, Anthropic provider showed "invalid setting" even with correct credentials

#### Consequences

**Positive**:
- Automatic UI updates
- No stale data
- Clean provider switching

**Negative**:
- Complex flow logic
- Requires understanding of Kotlin coroutines

---

### ADR-003: Official SDKs Over Third-Party Wrappers

**Status**: Accepted
**Date**: 2025-12-31

#### Context

Previous implementation used a third-party Kotlin wrapper for OpenAI API.

#### Decision

Switch to official SDKs:
- OpenAI: `com.openai:openai-java` 4.13.0
- Anthropic: `com.anthropic:anthropic-java` 2.11.1

**Rationale**:
- Official SDKs are maintained by API providers
- Better feature support (streaming, function calling, etc.)
- More frequent updates
- Better documentation
- Type-safe request/response models

#### Consequences

**Positive**:
- Better API coverage
- Official support
- Easier to debug

**Negative**:
- Learning curve for new SDKs
- Potential API differences between SDKs

**Mitigation**:
- Unified `AIClient` interface abstracts differences
- Comprehensive API documentation

---

## Design Patterns

### 1. Factory Pattern

**Location**: `AIApi.kt`

```kotlin
object AIApi {
    fun createClient(settings: AISettings): AIClient {
        return when (settings) {
            is AISettings.OpenAI ->
                OpenAICompatibleClient(settings.openaiSettings)
            is AISettings.Anthropic ->
                AnthropicClient(settings.anthropicSettings)
        }
    }
}
```

**Purpose**: Create provider-specific clients without exposing implementation details

**Benefits**:
- Centralized client creation logic
- Easy to add new providers
- UI doesn't need to know about concrete implementations

### 2. Sealed Interface

**Location**: `AISettings.kt`

```kotlin
sealed interface AISettings {
    val isValid: Boolean

    data class OpenAI(
        val openaiSettings: OpenAISettings
    ) : AISettings { ... }

    data class Anthropic(
        val anthropicSettings: AnthropicSettings
    ) : AISettings { ... }
}
```

**Purpose**: Type-safe provider settings

**Benefits**:
- Compile-time exhaustiveness checking
- Impossible to mix settings from different providers
- Clear separation between providers

### 3. Repository Pattern

**Location**: `Repository.kt`

```kotlin
class Repository(
    private val settingsStore: SettingsStore
) {
    val aiSettingsFlow: StateFlow<AISettings>
        get() = settingsStore.aiSettingsFlow

    fun getAIClient(): AIClient =
        AIApi.createClient(aiSettingsFlow.value)

    fun setAIProviderType(provider: AIProvider) { ... }
    fun setOpenAISettings(settings: OpenAISettings) { ... }
    fun setAnthropicSettings(settings: AnthropicSettings) { ... }
}
```

**Purpose**: Abstract data layer from business logic

**Benefits**:
- Single source of truth for AI settings
- Easy to test (can mock Repository)
- Consistent access pattern

### 4. MVVM Architecture

**Components**:
- **Model**: `Repository`, `SettingsStore`
- **View**: Jetpack Compose UI (`AIProviderSection`, `ArticleScreen`)
- **ViewModel**: `SettingsViewModel`, `ArticleViewModel`

**Data Flow**:
```
UI Event → ViewModel → Repository → SettingsStore → Persistence
         ↓
UI Update ← StateFlow ← Repository ← SettingsStore
```

---

## Architecture Diagrams

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                       │
│  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │ Settings Screen │  │ Article Screen   │  │ AI Provider     │ │
│  │                 │  │                  │  │ Section         │ │
│  └────────┬────────┘  └────────┬─────────┘  └────────┬────────┘ │
└───────────┼────────────────────┼──────────────────────┼──────────┘
            │                    │                      │
            ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                            │
│  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │ Settings        │  │ Article          │  │ Events:         │ │
│  │ ViewModel       │  │ ViewModel        │  │ - UpdateSettings│ │
│  │                 │  │                  │  │ - SelectProvider│ │
│  └────────┬────────┘  └────────┬─────────┘  └─────────────────┘ │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Repository Layer                              │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Repository                                                   │ │
│  │  - aiSettingsFlow: StateFlow<AISettings>                    │ │
│  │  - getAIClient(): AIClient                                  │ │
│  │  - setAIProviderType(), setOpenAISettings(), ...           │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────┬───────────────────────┘
                                              │
┌─────────────────────────────────────────────┴───────────────────┐
│                     SettingsStore (Persistence)                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ _aiProviderType: MutableStateFlow<AIProvider>              │ │
│  │ _openAISettings: MutableStateFlow<OpenAISettings>          │ │
│  │ _anthropicSettings: MutableStateFlow<AnthropicSettings>    │ │
│  │                                                             │ │
│  │ aiSettingsFlow = _aiProviderType.flatMapLatest { ... }     │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Factory Layer                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ AIApi.createClient(settings: AISettings): AIClient          │ │
│  │                                                             │ │
│  │   when (settings) {                                         │ │
│  │     is OpenAI -> OpenAICompatibleClient()                   │ │
│  │     is Anthropic -> AnthropicClient()                       │ │
│  │   }                                                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Provider Layer                                │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │ AIClient            │  │ AIProvider (enum)               │  │
│  │ (interface)         │  │ - OPENAI_COMPATIBLE             │  │
│  │                     │  │ - ANTHROPIC                     │  │
│  │ - listModels()      │  └─────────────────────────────────┘  │
│  │ - generateSummary() │                                      │
│  └─────────────────────┘                                      │
│           ▲                                                    │
│           │                                                    │
│  ┌────────┴────────┐  ┌─────────────────────────────────────┐  │
│  │ OpenAI          │  │ Anthropic                           │  │
│  │ Compatible      │  │ Client                              │  │
│  │ Client          │  │                                     │  │
│  └─────────────────┘  └─────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Settings Persistence Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    SharedPreferences                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Keys:                                               │   │
│  │ - "ai_provider_type" → "OPENAI_COMPATIBLE"          │   │
│  │ - "openai_key" → "sk-..."                           │   │
│  │ - "openai_base_url" → "https://api.openai.com/v1"  │   │
│  │ - "openai_model_id" → "gpt-4o-mini"                │   │
│  │ - "anthropic_key" → "sk-ant-..."                   │   │
│  │ - "anthropic_base_url" → "https://api.anthropic.com"│   │
│  │ - "anthropic_model_id" → "claude-3-5-sonnet-..."   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    SettingsStore                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ init {                                              │   │
│  │   // Load from SharedPreferences                    │   │
│  │   _aiProviderType.value = loadProviderType()       │   │
│  │   _openAISettings.value = loadOpenAISettings()     │   │
│  │   _anthropicSettings.value = loadAnthropicSettings()│   │
│  │ }                                                    │   │
│  │                                                      │   │
│  │ fun setAIProviderType(provider: AIProvider) {       │   │
│  │   _aiProviderType.value = provider                  │   │
│  │   save("ai_provider_type", provider.name)           │   │
│  │ }                                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              aiSettingsFlow (StateFlow<AISettings>)          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ _aiProviderType.flatMapLatest { provider ->         │   │
│  │   when (provider) {                                 │   │
│  │     OPENAI_COMPATIBLE -> _openAISettings.mapLatest  │   │
│  │     ANTHROPIC -> _anthropicSettings.mapLatest       │   │
│  │   }                                                  │   │
│  │ }                                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ val aiSettingsFlow: StateFlow<AISettings>           │   │
│  │ fun getAIClient(): AIClient =                        │   │
│  │   AIApi.createClient(aiSettingsFlow.value)          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ val aiSettings: StateFlow<AISettings> =             │   │
│  │   repository.aiSettingsFlow                          │   │
│  │                                                      │   │
│  │ init {                                              │   │
│  │   viewModelScope.launch {                            │   │
│  │     repository.aiSettingsFlow.collect {              │   │
│  │       _aiSettings.value = it                         │   │
│  │     }                                                │   │
│  │   }                                                  │   │
│  │ }                                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      UI (Compose)                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ @Composable                                          │   │
│  │ fun AIProviderSection(                               │   │
│  │   aiSettings: AISettings,                            │   │
│  │   onEvent: (AISettingsEvent) -> Unit                 │   │
│  │ ) { ... }                                            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Provider Switching Flow (Phase 2 Bug Fix)

```
User selects "Anthropic" in dropdown
         │
         ▼
UI emits AISettingsEvent.SelectProvider(ANTHROPIC)
         │
         ▼
SettingsViewModel.onOpenAISettingsEvent()
         │
         ├─► repository.setAIProviderType(AIProvider.ANTHROPIC)  ← ADDED IN PHASE 2
         │     │
         │     ▼
         │   SettingsStore._aiProviderType.value = ANTHROPIC
         │     │
         │     ▼
         │   aiSettingsFlow switches to Anthropic branch
         │     │
         │     ▼
         │   _anthropicSettings.mapLatest { ... }
         │     │
         │     ▼
         │   emits AISettings.Anthropic(...)
         │
         └─► (settings already loaded from SharedPreferences)
               │
               ▼
         UI receives new AISettings.Anthropic(...)
               │
               ▼
         UI recomposes with Anthropic settings form
```

**Phase 2 Bug Fix**: Before the fix, `setAIProviderType()` was NOT called when users updated settings (API key, model ID, etc.). This caused `aiSettingsFlow` to emit the wrong provider's settings, leading to "invalid setting" errors.

---

## Component Architecture

### AIClient Interface

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

**Purpose**: Unified interface for all AI providers

```kotlin
interface AIClient {
    suspend fun listModels(): List<String>

    suspend fun generateSummary(
        content: String
    ): SummaryResult

    sealed interface SummaryResult {
        data class Success(
            val id: String,
            val created: Long,
            val model: String,
            val content: String,
            val promptTokens: Int,
            val completeTokens: Int,
            val totalTokens: Int,
            val detectedLanguage: String
        ) : SummaryResult

        data class Error(val content: String) : SummaryResult
    }
}
```

**Design Rationale**:
- `suspend` functions for coroutine-based async operations
- Sealed `SummaryResult` for type-safe error handling
- `listModels()` for dynamic model fetching (OpenAI only)
- `detectedLanguage` for multilingual support (Anthropic only)

### OpenAI-Compatible Client

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

**SDK**: `com.openai:openai-java` 4.13.0

**Features**:
- Dynamic model fetching via `client.models().list()`
- Fallback to known models if API call fails
- Custom base URL support
- Configurable timeout

**Key Implementation**:
```kotlin
class OpenAICompatibleClient(
    private val settings: OpenAISettings
) : AIClient {

    private val client: OpenAiClient by lazy {
        OpenAiClient.builder()
            .apiKey(settings.key)
            .baseUrl(settings.baseUrl)
            .timeout(Duration.ofSeconds(90))
            .build()
    }

    override suspend fun listModels(): List<String> {
        return try {
            client.models().list()
                .map { it.id }
                .filter { it.startsWith("gpt-") || it.startsWith("o1-") }
        } catch (e: Exception) {
            // Fallback to known models
            FALLBACK_MODELS
        }
    }

    override suspend fun generateSummary(
        content: String
    ): SummaryResult { ... }
}
```

### Anthropic Client

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

**SDK**: `com.anthropic:anthropic-java` 2.11.1

**Features**:
- No model list (users input directly)
- Custom base URL support
- Language detection in summary response

**Phase 2 Change**: Removed hardcoded model list per user request

**Key Implementation**:
```kotlin
class AnthropicClient(
    private val settings: AnthropicSettings
) : AIClient {

    private val client: AnthropicClient by lazy {
        AnthropicClient.builder()
            .apiKey(settings.key)
            .baseUrl(settings.baseUrl)
            .timeout(Duration.ofSeconds(90))
            .build()
    }

    override suspend fun listModels(): List<String> {
        // Anthropic doesn't provide a models endpoint
        // Users input model ID directly
        return emptyList()  // CHANGED IN PHASE 2
    }

    override suspend fun generateSummary(
        content: String
    ): SummaryResult { ... }
}
```

### Settings Store

**Location**: `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Purpose**: Persist AI settings across app restarts

**Key Components**:
```kotlin
class SettingsStore(
    context: Context
) {
    // Provider type
    private val _aiProviderType = MutableStateFlow(AIProvider.OPENAI_COMPATIBLE)
    val aiProviderType: StateFlow<AIProvider> = _aiProviderType.asStateFlow()

    // Provider-specific settings
    private val _openAISettings = MutableStateFlow(OpenAISettings())
    private val _anthropicSettings = MutableStateFlow(AnthropicSettings())

    // Reactive flow that switches based on provider type
    val aiSettingsFlow: StateFlow<AISettings> =
        _aiProviderType
            .flatMapLatest { provider ->
                when (provider) {
                    AIProvider.OPENAI_COMPATIBLE ->
                        _openAISettings.mapLatest { openaiSettings ->
                            AISettings.OpenAI(openaiSettings)
                        }
                    AIProvider.ANTHROPIC ->
                        _anthropicSettings.mapLatest { anthropicSettings ->
                            AISettings.Anthropic(anthropicSettings)
                        }
                }
            }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AISettings.OpenAI(OpenAISettings())
            )

    fun setAIProviderType(provider: AIProvider) {
        _aiProviderType.value = provider
        prefs.edit().putString("ai_provider_type", provider.name).apply()
    }

    fun setOpenAISettings(settings: OpenAISettings) {
        _openAISettings.value = settings
        // Save to SharedPreferences
        prefs.edit().putString("openai_key", settings.key).apply()
        prefs.edit().putString("openai_base_url", settings.baseUrl).apply()
        prefs.edit().putString("openai_model_id", settings.modelId).apply()
    }

    fun setAnthropicSettings(settings: AnthropicSettings) {
        _anthropicSettings.value = settings
        // Save to SharedPreferences
        prefs.edit().putString("anthropic_key", settings.key).apply()
        prefs.edit().putString("anthropic_base_url", settings.baseUrl).apply()
        prefs.edit().putString("anthropic_model_id", settings.modelId).apply()
    }
}
```

---

## Data Flow

### Summary Generation Flow

```
┌─────────────────────────────────────────────────────────────┐
│ User clicks "Summarize" button in Article Screen            │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ ArticleViewModel.onGenerateSummary()                        │
│  │                                                           │
│  ├─► Get current AI settings                                │
│  │   aiSettings = repository.aiSettingsFlow.value           │
│  │                                                           │
│  ├─► Create AI client                                        │
│  │   client = repository.getAIClient()                      │
│  │     = AIApi.createClient(aiSettings)                     │
│  │                                                           │
│  ├─► Call generateSummary()                                 │
│  │   result = client.generateSummary(articleContent)        │
│  │                                                           │
│  └─► Update UI state                                         │
│      _summaryResult.value = result                           │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ AIApi.createClient()                                        │
│  │                                                           │
│  └─► when (settings) {                                      │
│        is AISettings.OpenAI ->                               │
│          OpenAICompatibleClient(settings.openaiSettings)    │
│        is AISettings.Anthropic ->                            │
│          AnthropicClient(settings.anthropicSettings)        │
│      }                                                       │
└─────────────────────────────────────────────────────────────┘
                         │
            ┌────────────┴────────────┐
            ▼                         ▼
┌─────────────────────┐   ┌─────────────────────────┐
│ OpenAI-Compatible   │   │ Anthropic               │
│ Client              │   │ Client                  │
│                     │   │                         │
│ 1. Call OpenAI API  │   │ 1. Call Anthropic API   │
│ 2. Parse response   │   │ 2. Parse response       │
│ 3. Return result    │   │ 3. Return result        │
└─────────────────────┘   └─────────────────────────┘
            │                         │
            └────────────┬────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ SummaryResult returned to ViewModel                          │
│  │                                                           │
│  └─► Update UI state:                                       │
│      _summaryResult.value =                                  │
│        SummaryResult.Success(                                │
│          id = "...",                                         │
│          content = "Article summary...",                     │
│          detectedLanguage = "en",                            │
│          ...                                                │
│        )                                                     │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ UI observes summaryResult and displays summary               │
└─────────────────────────────────────────────────────────────┘
```

---

## State Management

### State Flow Hierarchy

```
StateFlow<AISettings> (aiSettingsFlow)
         │
         ├─► AISettings.OpenAI(OpenAISettings)
         │      ├─► key: String
         │      ├─► baseUrl: String
         │      └─► modelId: String
         │
         └─► AISettings.Anthropic(AnthropicSettings)
                ├─► key: String
                ├─► baseUrl: String
                └─► modelId: String
```

### Reactive Update Flow

```
User Action (dropdown change, text input)
         │
         ▼
AISettingsEvent
         │
         ▼
SettingsViewModel.onOpenAISettingsEvent()
         │
         ├─► Phase 2 Fix: setAIProviderType()  ← CRITICAL FOR BUG FIX
         │     │
         │     ▼
         │   _aiProviderType.value updated
         │     │
         │     ▼
         │   aiSettingsFlow switches branches (flatMapLatest)
         │     │
         │     ▼
         │   Emits new AISettings
         │
         └─► setProviderSettings()
               │
               ▼
         _openAISettings.value or _anthropicSettings.value updated
               │
               ▼
         aiSettingsFlow emits new value (mapLatest)
               │
               ▼
         Repository.aiSettingsFlow emits new value
               │
               ▼
         ViewModel collects and updates UI state
               │
               ▼
         UI recomposes with new settings
```

---

## Phase 2 Changes

### Bug #002: Provider Type Synchronization

**Issue**: Anthropic API showed "invalid setting" even with correct API key.

**Root Cause**: `aiProviderType` not updated when settings changed.

**Fix**: Added `setAIProviderType()` calls in `SettingsViewModel.onOpenAISettingsEvent()`.

**Files Modified**:
- `SettingsViewModel.kt`

**Code Change**:
```kotlin
// BEFORE (buggy):
is AISettingsEvent.UpdateSettings ->
    when (event.settings) {
        is AISettings.OpenAI ->
            repository.setOpenAISettings(event.settings.openaiSettings)
        is AISettings.Anthropic ->
            repository.setAnthropicSettings(event.settings.anthropicSettings)
    }

// AFTER (fixed):
is AISettingsEvent.UpdateSettings ->
    when (event.settings) {
        is AISettings.OpenAI -> {
            repository.setAIProviderType(AIProvider.OPENAI_COMPATIBLE)  // ADDED
            repository.setOpenAISettings(event.settings.openaiSettings)
        }
        is AISettings.Anthropic -> {
            repository.setAIProviderType(AIProvider.ANTHROPIC)  // ADDED
            repository.setAnthropicSettings(event.settings.anthropicSettings)
        }
    }
```

**Impact**: Critical - ensures `aiSettingsFlow` emits correct provider's settings when users update their configuration.

### Fix: Anthropic Model List

**Issue**: Anthropic client had hardcoded model list.

**Solution**: Users input model ID directly. `listModels()` returns `emptyList()`.

**Files Modified**:
- `AnthropicClient.kt`

**Code Change**:
```kotlin
// BEFORE:
override suspend fun listModels(): List<String> {
    return ANTHROPIC_MODELS  // Hardcoded list
}

companion object {
    private val ANTHROPIC_MODELS = listOf(
        "claude-3-5-sonnet-20241022",
        "claude-3-5-haiku-20241022",
        // ... many more
    )
}

// AFTER:
override suspend fun listModels(): List<String> {
    // Anthropic doesn't provide a models endpoint
    // Users input model ID directly
    return emptyList()
}
```

### Fix: "No Models" Message

**Issue**: "No models were found" message showed for Anthropic.

**Fix**: Added `isAnthropic` parameter to `AIModelsStatus` to skip message.

**Files Modified**:
- `AIProviderSection.kt`

**Code Change**:
```kotlin
@Composable
private fun AIModelsStatus(
    state: ModelsState,
    showError: Boolean,
    onEvent: (AISettingsEvent) -> Unit,
    isAnthropic: Boolean = false,  // NEW
) {
    when (state) {
        is ModelsState.Success -> {
            // Don't show "no models" message for Anthropic
            if (state.ids.isEmpty() && !isAnthropic) {  // UPDATED
                // show message
            }
        }
    }
}
```

---

## References

- Requirements: [./01-requirement.md](./01-requirement.md)
- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Technical Specification: [./04-specification.md](./04-specification.md)
- Implementation Plan: [./05-implementation-plan.md](./05-implementation-plan.md)
- Testing Strategy: [./06-testing-strategy.md](./06-testing-strategy.md)
- API Documentation: [./07-api-documentation.md](./07-api-documentation.md)
- Migration Guide: [./08-migration-guide.md](./08-migration-guide.md)
- Implementation Summary: [./09-implementation-summary.md](./09-implementation-summary.md)
