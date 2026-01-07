# Technical Specification: Add max_tokens Configuration

**Spec ID:** 027-add-max-token-config
**Date:** 2026-01-07
**Status:** READY FOR IMPLEMENTATION
**Version:** 1.0

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture Design](#2-architecture-design)
3. [UI/UX Design](#3-uiux-design)
4. [Implementation Details](#4-implementation-details)
5. [Data Model Changes](#5-data-model-changes)
6. [ViewModel Changes](#6-viewmodel-changes)
7. [UI Changes](#7-ui-changes)
8. [Validation Logic](#8-validation-logic)
9. [API Integration](#9-api-integration)
10. [Testing Strategy](#10-testing-strategy)
11. [Localization](#11-localization)
12. [Deployment](#12-deployment)

---

## 1. Overview

Add a `max_tokens` configuration field to the AI provider edit/creation screen, allowing users to control the maximum number of tokens the model can output.

**Goals:**
- Provide user control over response length
- Help manage API costs
- Reduce latency for shorter responses
- Maintain backward compatibility

**Scope:**
- Add max_tokens field to provider edit/creation UI
- Support both OpenAI and Anthropic providers
- Implement validation and persistence
- Pass max_tokens to API calls

---

## 2. Architecture Design

### 2.1 Layer Architecture

```
┌─────────────────────────────────────┐
│  Presentation Layer (Compose UI)    │
│  - ProviderEditScreen              │
│  - MaxTokensTextField              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Business Logic Layer (ViewModel)   │
│  - ProviderEditViewModel            │
│  - updateMaxTokens()                │
│  - Validation logic                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Data Layer (Models & Repository)   │
│  - OpenAISettings.maxTokens         │
│  - AnthropicSettings.maxTokens      │
│  - ProviderConfig                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  API Layer (Clients)                │
│  - OpenAICompatibleClient           │
│  - AnthropicClient                  │
└─────────────────────────────────────┘
```

### 2.2 Data Flow

```
User Input (UI)
    │
    ▼
ViewModel.updateMaxTokens(String)
    │
    ├─► Validation (is valid integer?)
    │       │
    │       ├─► Yes: Convert to Int?
    │       │       │
    │       │       ├─► Success: Update State
    │       │       │
    │       │       └─► Fail: Show error
    │       │
    │       └─► No: Show error
    │
    ▼
ProviderConfig (with maxTokens: Int?)
    │
    ▼
Repository.saveProvider()
    │
    ▼
SettingsStore (Persistence)
    │
    ▼
API Client (maxTokens parameter)
```

### 2.3 Design Decisions

| Decision | Rationale |
|----------|-----------|
| Use `Int?` (nullable) | Allows empty = provider default |
| Validate in ViewModel | Centralized validation logic |
| Use `KeyboardType.Number` | Better UX for numeric input |
| Show errors in `supportingText` | Material Design 3 pattern |
| Provider-specific limits | Different models have different limits |

---

## 3. UI/UX Design

### 3.1 Field Layout

```
┌─────────────────────────────────────┐
│ Provider Name                       │
│ ┌─────────────────────────────────┐ │
│ │ Enter provider name             │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Provider Type                       │
│ ┌─────────────────────────────────┐ │
│ │ OpenAI Compatible         ▼     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ API Key *                           │
│ ┌─────────────────────────────────┐ │
│ │ sk-...                          │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Base URL                            │
│ ┌─────────────────────────────────┐ │
│ │ https://api.openai.com/v1       │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Model                               │
│ ┌─────────────────────────────────┐ │
│ │ gpt-4o-mini                     │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Max Tokens                          │  ← NEW FIELD
│ ┌─────────────────────────────────┐ │
│ │ 1-4096 (optional)               │ │
│ └─────────────────────────────────┘ │
│ Maximum tokens in response.        │
│ Leave empty for default.           │
│                                     │
│ ☐ Set as default provider           │
│                                     │
│  [Cancel]                    [Save] │
└─────────────────────────────────────┘
```

### 3.2 Field Specifications

**Component:** `OutlinedTextField`

**Properties:**
```kotlin
OutlinedTextField(
    value = uiState.maxTokens ?: "",
    onValueChange = onMaxTokensChange,
    label = { Text(stringResource(R.string.max_tokens)) },
    placeholder = { Text(stringResource(R.string.max_tokens_placeholder)) },
    supportingText = {
        when {
            uiState.maxTokensError != null ->
                Text(uiState.maxTokensError)
            else ->
                Text(stringResource(R.string.max_tokens_hint))
        }
    },
    isError = uiState.maxTokensError != null,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done
    ),
    modifier = Modifier.fillMaxWidth()
)
```

**Interaction Behavior:**
1. **Initial State:** Empty field, placeholder visible
2. **Focus:** No validation error yet
3. **Input:** Real-time validation on each keystroke
4. **Invalid Input:** Show error in supporting text, field turns red
5. **Valid Input:** Clear error, field returns to normal
6. **Empty Field:** Valid (uses provider default)
7. **Save:** Validate all fields, show snackbar on success/error

### 3.3 Validation States

| State | Field Appearance | Supporting Text | Can Save? |
|-------|------------------|-----------------|-----------|
| Empty | Normal | "Maximum tokens in response. Leave empty for default." | ✅ Yes |
| Valid (e.g., "2048") | Normal | "Maximum tokens in response. Leave empty for default." | ✅ Yes |
| Invalid text | Red | "Must be a number" | ❌ No |
| Out of range (< 1) | Red | "Must be at least 1" | ❌ No |
| Out of range (> 4096) | Red | "Cannot exceed 4096" | ❌ No |

---

## 4. Implementation Details

### 4.1 Files to Modify

| File | Changes | Lines Added |
|------|---------|-------------|
| `OpenAISettings.kt` | Add `maxTokens` field | +1 |
| `AnthropicSettings.kt` | Add `maxTokens` field | +1 |
| `ProviderEditViewModel.kt` | Add `updateMaxTokens()` method | +30 |
| `ProviderEditScreen.kt` | Add max_tokens field | +20 |
| `strings.xml` (en) | Add 4 strings | +4 |
| `strings.xml` (zh-CN) | Add 4 strings | +4 |

**Total:** 6 files, ~60 lines of code

### 4.2 Implementation Phases

**Phase 1: Data Model** (5 minutes)
- Add `maxTokens: Int? = null` to `OpenAISettings`
- Add `maxTokens: Int? = null` to `AnthropicSettings`

**Phase 2: ViewModel** (15 minutes)
- Add `updateMaxTokens()` method
- Add validation logic
- Add computed property to `ProviderEditUiState`

**Phase 3: UI** (15 minutes)
- Add `OutlinedTextField` for max_tokens
- Wire up callbacks
- Add string resources

**Phase 4: Testing** (30 minutes)
- Unit tests for ViewModel
- UI tests for field
- Manual testing

**Total Estimated Time:** 65 minutes

---

## 5. Data Model Changes

### 5.1 OpenAISettings

**Before:**
```kotlin
@Serializable
data class OpenAISettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
    val azureApiVersion: String = "",
    val azureDeploymentId: String = "",
)
```

**After:**
```kotlin
@Serializable
data class OpenAISettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
    val azureApiVersion: String = "",
    val azureDeploymentId: String = "",
    val maxTokens: Int? = null,  // NEW: Maximum tokens for model output
)
```

### 5.2 AnthropicSettings

**Before:**
```kotlin
@Serializable
data class AnthropicSettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
)
```

**After:**
```kotlin
@Serializable
data class AnthropicSettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 90,
    val maxTokens: Int? = null,  // NEW: Maximum tokens for model output
)
```

**Notes:**
- ✅ Backward compatible (null default)
- ✅ No migration needed
- ✅ Kotlin serialization handles null
- ✅ Existing configs continue to work

---

## 6. ViewModel Changes

### 6.1 New Method: updateMaxTokens()

```kotlin
/**
 * Update the max tokens setting.
 * @param maxTokens String input from UI (can be empty)
 */
fun updateMaxTokens(maxTokens: String) {
    val trimmed = maxTokens.trim()

    when {
        trimmed.isEmpty() -> {
            // Empty is valid (null = provider default)
            updateMaxTokensInSettings(null)
        }
        trimmed.toIntOrNull() == null -> {
            // Not a number - keep invalid input but don't save
            updateMaxTokensInSettings(null)
        }
        trimmed.toInt() < 1 -> {
            // Too small - keep invalid input but don't save
            updateMaxTokensInSettings(null)
        }
        trimmed.toInt() > getMaxTokensLimit() -> {
            // Too large - keep invalid input but don't save
            updateMaxTokensInSettings(null)
        }
        else -> {
            // Valid - update settings
            updateMaxTokensInSettings(trimmed.toInt())
        }
    }
}

private fun updateMaxTokensInSettings(tokens: Int?) {
    val current = _internalState.value.provider
    val updatedProvider = when (current.providerType) {
        AIProvider.OPENAI_COMPATIBLE ->
            current.copy(
                openAISettings = current.openAISettings?.copy(
                    maxTokens = tokens
                ) ?: com.nononsenseapps.feeder.ai.model.OpenAISettings(
                    maxTokens = tokens
                )
            )
        AIProvider.ANTHROPIC ->
            current.copy(
                anthropicSettings = current.anthropicSettings?.copy(
                    maxTokens = tokens
                ) ?: com.nononsenseapps.feeder.ai.model.AnthropicSettings(
                    maxTokens = tokens
                )
            )
    }
    updateProvider(updatedProvider)
}

private fun getMaxTokensLimit(): Int {
    return when (_internalState.value.provider.providerType) {
        AIProvider.OPENAI_COMPATIBLE -> 4096
        AIProvider.ANTHROPIC -> 4096
    }
}
```

### 6.2 UI State Extension

**Add to `ProviderEditUiState`:**
```kotlin
val maxTokens: String
    get() = provider.openAISettings?.maxTokens?.toString()
        ?: provider.anthropicSettings?.maxTokens?.toString()
        ?: ""

val maxTokensError: String?
    get() = when {
        maxTokens.isNotEmpty() && maxTokens.toIntOrNull() == null ->
            "Must be a number"
        maxTokens.isNotEmpty() && maxTokens.toInt() < 1 ->
            "Must be at least 1"
        maxTokens.isNotEmpty() && maxTokens.toInt() > 4096 ->
            "Cannot exceed 4096"
        else -> null
    }
```

---

## 7. UI Changes

### 7.1 ProviderEditForm Updates

**Add to form (after Model field):**
```kotlin
// Max Tokens (optional)
OutlinedTextField(
    value = uiState.maxTokens,
    onValueChange = viewModel::updateMaxTokens,
    label = {
        Text(stringResource(R.string.max_tokens))
    },
    placeholder = {
        Text(stringResource(R.string.max_tokens_placeholder))
    },
    supportingText = {
        if (uiState.maxTokensError != null) {
            Text(uiState.maxTokensError)
        } else {
            Text(stringResource(R.string.max_tokens_hint))
        }
    },
    isError = uiState.maxTokensError != null,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done
    ),
    modifier = Modifier.fillMaxWidth()
)
```

### 7.2 Form Validation Update

**Update `isFormValid` check:**
```kotlin
val isFormValid =
    uiState.name.isNotBlank() &&
        uiState.apiKey.isNotBlank() &&
        uiState.maxTokensError == null  // NEW: Check max_tokens validation
```

---

## 8. Validation Logic

### 8.1 Validation Rules

1. **Empty Value:** ✅ Valid (null = provider default)
2. **Integer Only:** Must be parseable as `Int`
3. **Positive:** Must be >= 1
4. **Provider Limit:** Must be <= 4096 (both providers)

### 8.2 Error Messages

| Condition | Error Message |
|-----------|---------------|
| Not a number | "Must be a number" |
| Less than 1 | "Must be at least 1" |
| Greater than 4096 | "Cannot exceed 4096" |

### 8.3 Validation Timing

- **Real-time:** Validate on each keystroke
- **On blur:** Validate when user leaves field
- **On save:** Final validation before saving

---

## 9. API Integration

### 9.1 OpenAI Client

**Update request building:**
```kotlin
val request = OpenAIRequest(
    model = settings.modelId,
    messages = messages,
    maxTokens = settings.maxTokens,  // Add this parameter
    // ... other parameters
)
```

### 9.2 Anthropic Client

**Update request building:**
```kotlin
val request = AnthropicRequest(
    model = settings.modelId,
    messages = messages,
    maxTokens = settings.maxTokens,  // Add this parameter
    // ... other parameters
)
```

**Note:** If `maxTokens` is null, omit the parameter to use provider default.

---

## 10. Testing Strategy

### 10.1 Unit Tests

**ViewModel Tests:**
```kotlin
@Test
fun `updateMaxTokens with valid number updates settings`() {
    // Arrange
    val viewModel = createViewModel()
    val validInput = "2048"

    // Act
    viewModel.updateMaxTokens(validInput)

    // Assert
    assertEquals(2048, viewModel.uiState.value.provider.openAISettings?.maxTokens)
}

@Test
fun `updateMaxTokens with empty string sets null`() {
    // Arrange
    val viewModel = createViewModel()

    // Act
    viewModel.updateMaxTokens("")

    // Assert
    assertNull(viewModel.uiState.value.provider.openAISettings?.maxTokens)
}

@Test
fun `updateMaxTokens with invalid text does not update settings`() {
    // Arrange
    val viewModel = createViewModel()
    val invalidInput = "abc"

    // Act
    viewModel.updateMaxTokens(invalidInput)

    // Assert
    assertNull(viewModel.uiState.value.provider.openAISettings?.maxTokens)
}
```

### 10.2 UI Tests

```kotlin
@Test
fun `maxTokens field displays and accepts input`() {
    // Compose test rule
    composeTestRule.setContent {
        ProviderEditScreen(/* ... */)
    }

    // Assert field exists
    composeTestRule
        .onNodeWithText("Max Tokens")
        .assertIsDisplayed()

    // Enter text
    composeTestRule
        .onNodeWithText("Max Tokens")
        .performTextInput("2048")

    // Assert value
    composeTestRule
        .onNodeWithText("2048")
        .assertIsDisplayed()
}
```

### 10.3 Manual Testing Checklist

- [ ] Create new provider with max_tokens
- [ ] Edit existing provider, add max_tokens
- [ ] Leave max_tokens empty (should work)
- [ ] Enter invalid text (should show error)
- [ ] Enter negative number (should show error)
- [ ] Enter number > 4096 (should show error)
- [ ] Verify API call includes max_tokens
- [ ] Verify provider switching preserves max_tokens

---

## 11. Localization

### 11.1 English (strings.xml)

```xml
<!-- Label -->
<string name="max_tokens">Max Tokens</string>

<!-- Placeholder -->
<string name="max_tokens_placeholder">1-4096 (optional)</string>

<!-- Hint -->
<string name="max_tokens_hint">Maximum tokens in response. Leave empty for default.</string>

<!-- Error messages -->
<string name="max_tokens_error_nan">Must be a number</string>
<string name="max_tokens_error_min">Must be at least 1</string>
<string name="max_tokens_error_max">Cannot exceed %d</string>
```

### 11.2 Chinese (zh-CN/strings.xml)

```xml
<string name="max_tokens">最大令牌数</string>
<string name="max_tokens_placeholder">1-4096（可选）</string>
<string name="max_tokens_hint">响应的最大令牌数。留空则使用默认值。</string>
<string name="max_tokens_error_nan">必须是数字</string>
<string name="max_tokens_error_min">必须至少为 1</string>
<string name="max_tokens_error_max">不能超过 %d</string>
```

---

## 12. Deployment

### 12.1 Pre-Deployment Checklist

- [ ] All code changes implemented
- [ ] All unit tests passing
- [ ] All UI tests passing
- [ ] Manual testing complete
- [ ] String resources localized
- [ ] Code reviewed by team
- [ ] Documentation updated

### 12.2 Release Notes

```
Feature: Add max_tokens Configuration

Users can now configure the maximum number of tokens for AI model outputs in the provider settings.

- Added max_tokens field to provider edit/creation screen
- Supports OpenAI and Anthropic providers
- Optional field (leave empty for provider default)
- Input validation with helpful error messages
- Reduces latency and costs for shorter responses
```

### 12.3 Backward Compatibility

✅ **Fully backward compatible**
- Existing provider configs without max_tokens continue to work
- Null value uses provider default
- No migration needed

---

**End of Technical Specification**

**Ready for Implementation: Phase 8 (Execution & QA)**
