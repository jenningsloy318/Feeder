# API Documentation: AI Features - Multiple Providers

**Feature ID**: 001
**Status**: Implemented (Phase 1), Bug Fixes Applied (Phase 2)
**Created**: 2025-12-31
**Last Updated**: 2026-01-01

---

## Table of Contents

1. [Overview](#overview)
2. [Core Interfaces](#core-interfaces)
3. [Provider APIs](#provider-apis)
4. [Settings Models](#settings-models)
5. [Factory API](#factory-api)
6. [Repository API](#repository-api)
7. [Usage Examples](#usage-examples)
8. [Error Handling](#error-handling)
9. [Phase 2 API Changes](#phase-2-api-changes)

---

## Overview

This document provides comprehensive API reference for Feeder's multi-provider AI feature, covering all interfaces, classes, and methods used to interact with AI providers.

### Architecture Summary

```
AIClient (interface)
    ├─► OpenAICompatibleClient (implements AIClient)
    └─► AnthropicClient (implements AIClient)

AISettings (sealed interface)
    ├─► OpenAI (contains OpenAISettings)
    └─► Anthropic (contains AnthropicSettings)

AIApi (factory)
    └─► createClient(settings): AIClient

Repository
    ├─► aiSettingsFlow: StateFlow<AISettings>
    ├─► getAIClient(): AIClient
    └─► setAIProviderType(), setOpenAISettings(), setAnthropicSettings()
```

---

## Core Interfaces

### AIClient

**Package**: `com.nononsenseapps.feeder.ai`

**Purpose**: Unified interface for all AI provider implementations

```kotlin
interface AIClient {
    /**
     * Lists available models for this provider.
     *
     * For OpenAI: Dynamically fetches from API.
     * For Anthropic: Returns empty list (users input model ID directly).
     *
     * @return List of model IDs
     * @throws AIClientException if API call fails
     */
    suspend fun listModels(): List<String>

    /**
     * Generates a summary for the given article content.
     *
     * @param content The article text to summarize
     * @return SummaryResult containing either Success or Error
     */
    suspend fun generateSummary(content: String): SummaryResult

    /**
     * Sealed interface representing the result of a summary generation.
     */
    sealed interface SummaryResult {
        /**
         * Represents a successfully generated summary.
         *
         * @property id Unique identifier for the summary
         * @property created Unix timestamp of when the summary was created
         * @property model The model ID used for generation
         * @property content The generated summary text
         * @property promptTokens Number of tokens in the prompt
         * @property completeTokens Number of tokens in the completion
         * @property totalTokens Total tokens used (prompt + completion)
         * @property detectedLanguage Detected language code (e.g., "en", "zh")
         */
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

        /**
         * Represents a failed summary generation.
         *
         * @property content Error message describing what went wrong
         */
        data class Error(val content: String) : SummaryResult
    }
}
```

**Usage Example**:
```kotlin
val client: AIClient = repository.getAIClient()

when (val result = client.generateSummary(articleContent)) {
    is AIClient.SummaryResult.Success -> {
        println("Summary: ${result.content}")
        println("Language: ${result.detectedLanguage}")
    }
    is AIClient.SummaryResult.Error -> {
        println("Error: ${result.content}")
    }
}
```

---

## Provider APIs

### OpenAI-Compatible Client

**Package**: `com.nononsenseapps.feeder.ai.provider`

**Class**: `OpenAICompatibleClient`

**SDK**: `com.openai:openai-java` 4.13.0

**Purpose**: Client for OpenAI-compatible APIs (OpenAI, Azure OpenAI, DeepSeek, Perplexity, etc.)

#### Constructor

```kotlin
class OpenAICompatibleClient(
    private val settings: OpenAISettings
) : AIClient
```

**Parameters**:
- `settings`: OpenAI-specific configuration (API key, base URL, model ID)

#### Methods

##### listModels()

```kotlin
override suspend fun listModels(): List<String>
```

**Behavior**:
1. Calls `client.models().list()` to fetch models from API
2. Filters for GPT and O1 models only
3. Returns fallback list if API call fails

**Returns**: List of model IDs (e.g., `["gpt-4o-mini", "gpt-4o", "o1-preview"]`)

**Throws**: `AIClientException` if API call fails and no fallback available

**Fallback Models**:
```kotlin
private val FALLBACK_MODELS = listOf(
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-4-turbo",
    "gpt-4",
    "gpt-3.5-turbo",
    "o1-preview",
    "o1-mini"
)
```

##### generateSummary()

```kotlin
override suspend fun generateSummary(content: String): AIClient.SummaryResult
```

**Behavior**:
1. Constructs OpenAI chat completion request
2. Sends to API with configured model
3. Parses response and returns `SummaryResult.Success`
4. Returns `SummaryResult.Error` on failure

**Request Format**:
```kotlin
ChatMessage(
    role = ChatMessageRole.USER,
    content = "Please summarize the following article in 3-5 sentences:\n\n$content"
)
```

**Response Handling**:
```kotlin
Success(
    id = response.id,
    created = response.created,
    model = response.model,
    content = response.choices[0].message.content,
    promptTokens = response.usage.promptTokens,
    completeTokens = response.usage.completionTokens,
    totalTokens = response.usage.totalTokens,
    detectedLanguage = "" // OpenAI doesn't provide language detection
)
```

#### Example Usage

```kotlin
val settings = OpenAISettings(
    key = "sk-...",
    baseUrl = "https://api.openai.com/v1",
    modelId = "gpt-4o-mini"
)

val client = OpenAICompatibleClient(settings)

// List models
val models = client.listModels()
println("Available models: $models")

// Generate summary
val result = client.generateSummary(articleContent)
when (result) {
    is AIClient.SummaryResult.Success -> {
        println("Summary: ${result.content}")
        println("Tokens used: ${result.totalTokens}")
    }
    is AIClient.SummaryResult.Error -> {
        println("Error: ${result.content}")
    }
}
```

---

### Anthropic Client

**Package**: `com.nononsenseapps.feeder.ai.provider`

**Class**: `AnthropicClient`

**SDK**: `com.anthropic:anthropic-java` 2.11.1

**Purpose**: Client for Anthropic Claude API

#### Constructor

```kotlin
class AnthropicClient(
    private val settings: AnthropicSettings
) : AIClient
```

**Parameters**:
- `settings`: Anthropic-specific configuration (API key, base URL, model ID)

#### Methods

##### listModels()

```kotlin
override suspend fun listModels(): List<String>
```

**Behavior**:
- Returns empty list (Anthropic doesn't provide models endpoint)
- Users input model ID directly in settings

**Phase 2 Change**: Previously returned hardcoded list of models, now returns `emptyList()` per user request.

**Returns**: Empty list `[]`

##### generateSummary()

```kotlin
override suspend fun generateSummary(content: String): AIClient.SummaryResult
```

**Behavior**:
1. Constructs Anthropic message request
2. Sends to API with configured model
3. Parses response and returns `SummaryResult.Success`
4. Returns `SummaryResult.Error` on failure

**Request Format**:
```kotlin
Message(
    role = MessageRole.USER,
    content = "Please summarize the following article in 3-5 sentences:\n\n$content"
)
```

**Response Handling**:
```kotlin
Success(
    id = response.id,
    created = response.stopReason ?: "", // Anthropic doesn't provide created timestamp
    model = response.model,
    content = response.content[0].text,
    promptTokens = response.usage.inputTokens,
    completeTokens = response.usage.outputTokens,
    totalTokens = response.usage.inputTokens + response.usage.outputTokens,
    detectedLanguage = extractLanguage(response.content[0].text) // Language detection
)
```

**Language Detection**:
```kotlin
private fun extractLanguage(text: String): String {
    // Simple language detection based on text patterns
    return when {
        text.matches(Regex(".*[\\u4e00-\\u9fa5]+.*")) -> "zh"
        text.matches(Regex(".*[\\u0400-\\u04FF]+.*")) -> "ru"
        text.matches(Regex(".*[\\u0600-\\u06FF]+.*")) -> "ar"
        else -> "en"
    }
}
```

#### Example Usage

```kotlin
val settings = AnthropicSettings(
    key = "sk-ant-...",
    baseUrl = "https://api.anthropic.com",
    modelId = "claude-3-5-sonnet-20241022"
)

val client = AnthropicClient(settings)

// List models (returns empty)
val models = client.listModels()
println("Available models: $models") // []

// Generate summary
val result = client.generateSummary(articleContent)
when (result) {
    is AIClient.SummaryResult.Success -> {
        println("Summary: ${result.content}")
        println("Language: ${result.detectedLanguage}")
        println("Tokens used: ${result.totalTokens}")
    }
    is AIClient.SummaryResult.Error -> {
        println("Error: ${result.content}")
    }
}
```

---

## Settings Models

### AISettings

**Package**: `com.nononsenseapps.feeder.ai.model`

**Purpose**: Sealed interface for type-safe provider-specific settings

```kotlin
sealed interface AISettings {
    /**
     * Indicates whether the current settings are valid and ready to use.
     * Settings are considered valid if API key and model ID are not empty.
     */
    val isValid: Boolean

    /**
     * Represents settings for OpenAI-compatible providers.
     */
    data class OpenAI(
        val openaiSettings: OpenAISettings
    ) : AISettings {
        override val isValid: Boolean
            get() = openaiSettings.key.isNotEmpty() &&
                     openaiSettings.modelId.isNotEmpty()
    }

    /**
     * Represents settings for Anthropic Claude.
     */
    data class Anthropic(
        val anthropicSettings: AnthropicSettings
    ) : AISettings {
        override val isValid: Boolean
            get() = anthropicSettings.key.isNotEmpty() &&
                     anthropicSettings.modelId.isNotEmpty()
    }
}
```

### OpenAISettings

```kotlin
data class OpenAISettings(
    val key: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val modelId: String = "gpt-4o-mini"
)
```

**Fields**:
- `key`: OpenAI API key (format: `sk-...`)
- `baseUrl`: API base URL (default: OpenAI production)
- `modelId`: Model to use for summarization

**Example Values**:
```kotlin
// OpenAI
OpenAISettings(
    key = "sk-proj-...",
    baseUrl = "https://api.openai.com/v1",
    modelId = "gpt-4o-mini"
)

// Azure OpenAI
OpenAISettings(
    key = "your-azure-key",
    baseUrl = "https://your-resource.openai.azure.com/openai/deployments/your-deployment",
    modelId = "gpt-4"
)

// DeepSeek
OpenAISettings(
    key = "sk-...",
    baseUrl = "https://api.deepseek.com/v1",
    modelId = "deepseek-chat"
)
```

### AnthropicSettings

```kotlin
data class AnthropicSettings(
    val key: String = "",
    val baseUrl: String = "https://api.anthropic.com",
    modelId: String = "claude-3-5-sonnet-20241022"
)
```

**Fields**:
- `key`: Anthropic API key (format: `sk-ant-...`)
- `baseUrl`: API base URL (default: Anthropic production)
- `modelId`: Model to use for summarization (user-inputted)

**Example Values**:
```kotlin
// Anthropic Claude
AnthropicSettings(
    key = "sk-ant-...",
    baseUrl = "https://api.anthropic.com",
    modelId = "claude-3-5-sonnet-20241022"
)
```

---

## Factory API

### AIApi

**Package**: `com.nononsenseapps.feeder.ai`

**Purpose**: Factory for creating provider-specific AI clients

```kotlin
object AIApi {
    /**
     * Creates an AI client based on the provided settings.
     *
     * @param settings Provider-specific settings (OpenAI or Anthropic)
     * @return Configured AI client instance
     */
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

**Usage Example**:
```kotlin
// Create OpenAI client
val openAISettings = AISettings.OpenAI(OpenAISettings(
    key = "sk-...",
    baseUrl = "https://api.openai.com/v1",
    modelId = "gpt-4o-mini"
))
val openAIClient = AIApi.createClient(openAISettings)

// Create Anthropic client
val anthropicSettings = AISettings.Anthropic(AnthropicSettings(
    key = "sk-ant-...",
    baseUrl = "https://api.anthropic.com",
    modelId = "claude-3-5-sonnet-20241022"
))
val anthropicClient = AIApi.createClient(anthropicSettings)
```

---

## Repository API

### Repository

**Package**: `com.nononsenseapps.feeder.archmodel`

**Purpose**: Provides access to AI settings and client creation

#### Properties

```kotlin
/**
 * Reactive flow of AI settings that automatically switches
 * based on the selected provider type.
 */
val aiSettingsFlow: StateFlow<AISettings>
    get() = settingsStore.aiSettingsFlow
```

**Behavior**:
- Emits `AISettings.OpenAI` when provider is OPENAI_COMPATIBLE
- Emits `AISettings.Anthropic` when provider is ANTHROPIC
- Automatically switches when provider type changes

#### Methods

##### getAIClient()

```kotlin
/**
 * Creates an AI client based on the current settings.
 *
 * @return Configured AI client for the current provider
 */
fun getAIClient(): AIClient = AIApi.createClient(aiSettingsFlow.value)
```

**Usage Example**:
```kotlin
val client: AIClient = repository.getAIClient()
val summary = client.generateSummary(articleContent)
```

##### setAIProviderType()

```kotlin
/**
 * Sets the active AI provider type.
 *
 * Phase 2 Fix: This method MUST be called when updating settings
 * to ensure aiSettingsFlow switches to the correct provider.
 *
 * @param provider The provider to activate (OPENAI_COMPATIBLE or ANTHROPIC)
 */
fun setAIProviderType(provider: AIProvider) {
    settingsStore.setAIProviderType(provider)
}
```

**Phase 2 Critical Fix**: Always call this method before updating provider-specific settings to ensure `aiSettingsFlow` switches correctly.

**Usage Example**:
```kotlin
// Switch to Anthropic
repository.setAIProviderType(AIProvider.ANTHROPIC)
repository.setAnthropicSettings(AnthropicSettings(key = "sk-ant-..."))
```

##### setOpenAISettings()

```kotlin
/**
 * Updates the OpenAI-compatible provider settings.
 *
 * @param settings New OpenAI settings to save
 */
fun setOpenAISettings(settings: OpenAISettings) {
    settingsStore.setOpenAISettings(settings)
}
```

**Usage Example**:
```kotlin
repository.setOpenAISettings(OpenAISettings(
    key = "sk-...",
    baseUrl = "https://api.openai.com/v1",
    modelId = "gpt-4o-mini"
))
```

##### setAnthropicSettings()

```kotlin
/**
 * Updates the Anthropic provider settings.
 *
 * @param settings New Anthropic settings to save
 */
fun setAnthropicSettings(settings: AnthropicSettings) {
    settingsStore.setAnthropicSettings(settings)
}
```

**Usage Example**:
```kotlin
repository.setAnthropicSettings(AnthropicSettings(
    key = "sk-ant-...",
    baseUrl = "https://api.anthropic.com",
    modelId = "claude-3-5-sonnet-20241022"
))
```

---

## Usage Examples

### Complete Workflow: OpenAI Provider

```kotlin
class ArticleViewModel(
    private val repository: Repository
) : ViewModel() {

    // Collect AI settings reactively
    val aiSettings: StateFlow<AISettings> = repository.aiSettingsFlow

    fun onGenerateSummary(article: Article) {
        viewModelScope.launch {
            try {
                // Get current settings
                val settings = repository.aiSettingsFlow.value

                if (!settings.isValid) {
                    _errorMessage.value = "Please configure AI settings first"
                    return@launch
                }

                // Create client
                val client = repository.getAIClient()

                // Generate summary
                when (val result = client.generateSummary(article.content)) {
                    is AIClient.SummaryResult.Success -> {
                        _summary.value = result.content
                        _tokensUsed.value = result.totalTokens
                    }
                    is AIClient.SummaryResult.Error -> {
                        _errorMessage.value = result.content
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate summary: ${e.message}"
            }
        }
    }
}
```

### Complete Workflow: Provider Switching

```kotlin
class SettingsViewModel(
    private val repository: Repository
) : ViewModel() {

    fun onOpenAISettingsEvent(event: AISettingsEvent) {
        when (event) {
            is AISettingsEvent.SelectProvider -> {
                // Update provider type
                repository.setAIProviderType(event.provider)
            }
            is AISettingsEvent.UpdateSettings -> {
                // Phase 2 Fix: Update provider type AND settings
                when (event.settings) {
                    is AISettings.OpenAI -> {
                        repository.setAIProviderType(AIProvider.OPENAI_COMPATIBLE)
                        repository.setOpenAISettings(event.settings.openaiSettings)
                    }
                    is AISettings.Anthropic -> {
                        repository.setAIProviderType(AIProvider.ANTHROPIC)
                        repository.setAnthropicSettings(event.settings.anthropicSettings)
                    }
                }
            }
        }
    }
}
```

### Collecting AI Settings in Compose UI

```kotlin
@Composable
fun AIProviderSection(
    viewModel: SettingsViewModel
) {
    val aiSettings by viewModel.aiSettings.collectAsState()

    when (aiSettings) {
        is AISettings.OpenAI -> {
            OpenAISettingsForm(aiSettings.openaiSettings) { newSettings ->
                viewModel.onEvent(AISettingsEvent.UpdateSettings(
                    AISettings.OpenAI(newSettings)
                ))
            }
        }
        is AISettings.Anthropic -> {
            AnthropicSettingsForm(aiSettings.anthropicSettings) { newSettings ->
                viewModel.onEvent(AISettingsEvent.UpdateSettings(
                    AISettings.Anthropic(newSettings)
                ))
            }
        }
    }
}
```

---

## Error Handling

### AIClientException

**Package**: `com.nononsenseapps.feeder.ai`

```kotlin
class AIClientException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
```

**Thrown By**:
- `OpenAICompatibleClient.listModels()` - if API call fails and no fallback
- `OpenAICompatibleClient.generateSummary()` - if API call fails
- `AnthropicClient.generateSummary()` - if API call fails

### SummaryResult.Error

**Returned When**:
- API returns 4xx error (invalid credentials, rate limit, etc.)
- API returns 5xx error (server error)
- Network timeout or connection failure
- Invalid response format

**Error Messages**:
```kotlin
// OpenAI errors
"Invalid API key"
"Rate limit exceeded"
"Model not found"
"Server error"

// Anthropic errors
"Invalid API key"
"Rate limit exceeded"
"Invalid model ID"
"Server error"
```

### Error Handling Best Practices

```kotlin
fun generateSummaryWithRetry(content: String): String? {
    return runCatching {
        val client = repository.getAIClient()
        val result = client.generateSummary(content)

        when (result) {
            is AIClient.SummaryResult.Success -> result.content
            is AIClient.SummaryResult.Error -> {
                // Log error
                Log.e("AI", "Summary failed: ${result.content}")
                null
            }
        }
    }.getOrElse { e ->
        // Handle unexpected exceptions
        Log.e("AI", "Unexpected error: ${e.message}", e)
        null
    }
}
```

---

## Phase 2 API Changes

### Bug #002 Fix: Provider Type Synchronization

**Issue**: `aiProviderType` not updated when settings changed, causing `aiSettingsFlow` to emit wrong provider's settings.

**Fix**: Added `setAIProviderType()` calls in `SettingsViewModel.onOpenAISettingsEvent()`.

#### Before Fix (Buggy)

```kotlin
is AISettingsEvent.UpdateSettings ->
    when (event.settings) {
        is AISettings.OpenAI ->
            repository.setOpenAISettings(event.settings.openaiSettings)
        is AISettings.Anthropic ->
            repository.setAnthropicSettings(event.settings.anthropicSettings)
    }
// BUG: aiProviderType not updated, aiSettingsFlow stays on old provider
```

#### After Fix (Correct)

```kotlin
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
// FIX: aiProviderType updated, aiSettingsFlow switches correctly
```

**Impact**: Critical - ensures `aiSettingsFlow` always emits correct provider's settings.

### Change: Anthropic Model List

**Before**:
```kotlin
override suspend fun listModels(): List<String> {
    if (settings.key.isEmpty()) {
        throw AIClientException("Missing API key")
    }
    return ANTHROPIC_MODELS  // Hardcoded list
}

companion object {
    private val ANTHROPIC_MODELS = listOf(
        "claude-3-5-sonnet-20241022",
        "claude-3-5-haiku-20241022",
        // ... 20+ more models
    )
}
```

**After**:
```kotlin
override suspend fun listModels(): List<String> {
    // Anthropic doesn't provide a models endpoint
    // Users input model ID directly
    return emptyList()
}
```

**Rationale**: User requested removal of hardcoded model list. Users now input model ID directly in settings.

### Change: "No Models" Message

**File**: `AIProviderSection.kt`

**Before**:
```kotlin
@Composable
private fun AIModelsStatus(
    state: ModelsState,
    showError: Boolean,
    onEvent: (AISettingsEvent) -> Unit
) {
    when (state) {
        is ModelsState.Success -> {
            if (state.ids.isEmpty()) {
                // Show "No models were found" message
            }
        }
    }
}
```

**After**:
```kotlin
@Composable
private fun AIModelsStatus(
    state: ModelsState,
    showError: Boolean,
    onEvent: (AISettingsEvent) -> Unit,
    isAnthropic: Boolean = false,  // NEW PARAMETER
) {
    when (state) {
        is ModelsState.Success -> {
            // Don't show "no models" message for Anthropic
            if (state.ids.isEmpty() && !isAnthropic) {  // UPDATED
                // Show "No models were found" message
            }
        }
    }
}
```

**Rationale**: Anthropic doesn't provide model list (users input directly), so "no models" message is confusing.

---

## References

- Requirements: [./01-requirement.md](./01-requirement.md)
- Architecture: [./02-architecture.md](./02-architecture.md)
- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Technical Specification: [./04-specification.md](./04-specification.md)
- Implementation Plan: [./05-implementation-plan.md](./05-implementation-plan.md)
- Testing Strategy: [./06-testing-strategy.md](./06-testing-strategy.md)
- Migration Guide: [./08-migration-guide.md](./08-migration-guide.md)
- Implementation Summary: [./09-implementation-summary.md](./09-implementation-summary.md)
