# Research Report: max_tokens Implementation Best Practices

**Spec ID:** 027-add-max-token-config
**Date:** 2026-01-07
**Status:** COMPLETE
**Research Type:** Online Research + Codebase Analysis

---

## Executive Summary

This report documents research findings on implementing `max_tokens` configuration for AI providers (OpenAI and Anthropic) in a Jetpack Compose Android application. Research covers API best practices, UI implementation patterns, and validation strategies.

---

## Table of Contents

1. [API Best Practices](#1-api-best-practices)
2. [UI/UX Implementation Patterns](#2-uiux-implementation-patterns)
3. [Validation Strategies](#3-validation-strategies)
4. [Codebase Analysis](#4-codebase-analysis)
5. [Recommendations](#5-recommendations)
6. [References](#6-references)

---

## 1. API Best Practices

### 1.1 OpenAI max_tokens Parameter

#### Key Findings:

1. **Performance Optimization**
   - Lower `max_tokens` values result in **reduced latency**
   - Set `max_tokens` to match expected response size for optimal performance
   - Helps manage rate limits by reducing estimated token usage

2. **Cost Management**
   - Using `max_tokens` to limit response length is a **key cost-saving strategy**
   - Smaller `max_tokens` values directly reduce API costs
   - Usage is estimated from `max_tokens` value

3. **Flexibility vs Control**
   - Leaving `max_tokens` blank allows **longest possible response** for a given prompt
   - Setting specific values provides **predictability** in response length
   - Useful when you need maximum flexibility vs controlled output

4. **Production Recommendations**
   - **Lower `max_tokens`** for similar token generation counts
   - Include **stop sequences** for better control
   - **Reduce `max_completion_tokens`** to match the size of your completions
   - Reduces chance of rate limit issues

#### Source: [Production best practices | OpenAI API](https://platform.openai.com/docs/guides/production-best-practices)

### 1.2 Anthropic Claude max_tokens Parameter

#### Key Findings:

1. **Default Values**
   - **Claude Sonnet 4**: Defaults to **1,000 output tokens** when `max_tokens` is not set
   - Default values vary by model

2. **Extended Thinking Considerations**
   - With **extended thinking** features, `budget_tokens` must be less than `max_tokens`
   - Interleaved thinking (beta) with tools allows `budget_tokens` to exceed `max_tokens`
   - Special considerations needed when using extended thinking mode

3. **Practical Usage**
   - Example: `max_tokens: 1024` for approximately **768 words**
   - Max tokens limits vary by deployment (e.g., AWS Bedrock may have different limits)

4. **Model-Specific Limits**
   - Each Claude model has different `max_tokens` limits
   - Consult specific model documentation for exact limits

#### Sources:
- [Building Production Apps with Claude API: Complete Technical Guide](https://medium.com/@reliabledataengineering/building-production-apps-with-claude-api-the-complete-technical-guide-to-prompts-tokens-and-8a740b9bab3a)
- [Official Claude Extended Thinking Documentation](https://platform.claude.com/docs/en/build-with-claude/extended-thinking)
- [AWS Bedrock Max Tokens Discussion](https://repost.aws/questions/QUmhH3_oqCTRm1PlshqIWqEg)

### 1.3 Provider-Specific Recommendations

| Provider | Recommended Default | Max Limit | Notes |
|----------|-------------------|-----------|-------|
| OpenAI | 4096 | Model-dependent (4k-128k) | Lower values reduce latency |
| Anthropic | 4096 | Model-dependent (4k-8k) | Sonnet 4 defaults to 1000 |

---

## 2. UI/UX Implementation Patterns

### 2.1 Jetpack Compose Text Field Best Practices

#### State Management
- Use **state-based text fields** for more complete and reliable state management
- Implement `rememberSaveable` for handling process death and configuration changes
- Use `StateFlow` in ViewModel for robust state management

#### Input Configuration
```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    ),
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
```

#### Sources:
- [Configure text fields | Jetpack Compose - Android Developers](https://developer.android.com/develop/ui/compose/text/user-input)
- [Validate input as the user types | Jetpack Compose](https://developer.android.com/develop/ui/compose/quick-guides/content/validate-input)

### 2.2 Number Input Specific Considerations

#### Keyboard Type
- Use `KeyboardType.Number` for numeric input
- Note: Some devices may not show numeric keyboard with `KeyboardType.Number`
- Consider using `KeyboardType.Decimal` if decimal values are needed (not needed for max_tokens)

#### Input Filtering
- Implement `onValueChange` to filter non-numeric characters
- Provide immediate visual feedback for invalid input
- Show clear error messages for validation failures

#### Source: [Jetpack Compose Number Input in TextField - Stack Overflow](https://stackoverflow.com/questions/67687674/jetpack-compose-number-input-in-to-textfield)

### 2.3 User Experience Patterns

#### Placeholder/Hint Text
- Provide **informative placeholder text** showing valid range
- Example: "Max tokens (1-4096, empty for default)"
- Helps users understand valid input without reading documentation

#### Validation Feedback
- **Real-time validation** as user types
- Display **error messages** below the field
- Use **supporting text** for hints and error messages
- Clear error when user leaves the field (on focus change)

#### Field Labeling
- Use **clear, descriptive labels**
- Consider adding **info icon** with tooltip for advanced settings
- Position logically with other advanced settings (temperature, top_p)

#### Sources:
- [How to Validate Fields Using Jetpack Compose in Android](https://medium.com/@rzmeneghelo/how-to-validate-fields-using-jetpack-compose-in-android-43be70597e82)
- [Command Your User Inputs with Jetpack Compose—Text Field Features](https://proandroiddev.com/command-your-user-inputs-with-jetpack-compose-text-field-features-hidden-in-plain-sight-47aaacc56aaf)

---

## 3. Validation Strategies

### 3.1 Validation Timing

#### Real-Time Validation
- Validate input **as the user types** for immediate feedback
- Prevents user from submitting invalid data
- Improves user experience by catching errors early

#### On-Save Validation
- Validate when user taps "Save"
- Final validation before persisting data
- Show snackbar/toast for validation errors

#### Source: [Validate input as the user types | Jetpack Compose](https://developer.android.com/develop/ui/compose/quick-guides/content/validate-input)

### 3.2 Validation Rules for max_tokens

1. **Type Validation**
   - Must be an integer
   - Reject decimal points, letters, special characters

2. **Range Validation**
   - Must be **>= 1** (positive integer)
   - Must be **<= provider maximum**
   - Show specific error for out-of-range values

3. **Optional Field**
   - **Empty value is allowed** (null = provider default)
   - Clear placeholder indicates optional nature
   - Don't show validation error for empty field

### 3.3 Error Messaging

#### Clear, Actionable Error Messages
- ✅ "Max tokens must be a positive number"
- ✅ "Max tokens cannot exceed 4096"
- ❌ "Invalid input"

#### Error Display Pattern
```kotlin
OutlinedTextField(
    // ... other parameters
    isError = validationError != null,
    supportingText = {
        if (validationError != null) {
            Text(validationError)
        }
    }
)
```

#### Source: [Creating a Form using Jetpack Compose and Material Design 3](https://www.waseefakhtar.com/android/form-using-jetpack-compose-and-material-design/)

---

## 4. Codebase Analysis

### 4.1 Existing Provider Configuration Structure

#### Data Model
Located at: `app/src/main/java/com/nononsenseapps/feeder/ai/model/ProviderConfig.kt`

```kotlin
@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val providerType: AIProvider,
    val openAISettings: OpenAISettings? = null,
    val anthropicSettings: AnthropicSettings? = null,
    val isActive: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
```

#### Key Observations:
- ProviderConfig uses **sealed class pattern** for settings
- Settings are **nullable** for provider-specific configurations
- Uses **Kotlin serialization** for persistence
- Already has **temperature** and **top_p** parameters in settings

### 4.2 Existing Settings Classes

#### OpenAISettings Pattern
```kotlin
@Serializable
data class OpenAISettings(
    val apiKey: String,
    val baseUrl: String? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    // maxTokens should be added here
)
```

#### AnthropicSettings Pattern
```kotlin
@Serializable
data class AnthropicSettings(
    val apiKey: String,
    val model: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    // maxTokens should be added here
)
```

### 4.3 Existing UI Pattern

#### ProviderEditScreen
Located at: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`

#### Key UI Patterns Observed:
- Uses `OutlinedTextField` for text input
- Implements `ProviderEditViewModel` for state management
- Uses `StateFlow` for reactive UI updates
- Shows validation errors with `supportingText`
- Uses `SnackbarHost` for save feedback

#### Example Existing Field Pattern:
```kotlin
OutlinedTextField(
    value = uiState.model,
    onValueChange = { viewModel.onModelChange(it) },
    label = { Text(stringResource(R.string.model)) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    )
)
```

### 4.4 Existing Validation Pattern

#### ViewModel Validation
- Validation logic in **ViewModel**
- Updates UI state with validation errors
- Clear boolean flags for form validity

```kotlin
data class UiState(
    val model: String = "",
    val modelError: String? = null,
    val isValid: Boolean = false
)
```

---

## 5. Recommendations

### 5.1 Data Model Changes

#### Add maxTokens to Settings Classes

```kotlin
@Serializable
data class OpenAISettings(
    val apiKey: String,
    val baseUrl: String? = null,
    val model: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,  // NEW: Optional max tokens
)

@Serializable
data class AnthropicSettings(
    val apiKey: String,
    val model: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,  // NEW: Optional max tokens
)
```

**Rationale:**
- Nullable field (`Int?`) allows **optional configuration**
- `null` value = use provider default
- Backward compatible with existing configurations

### 5.2 UI Implementation

#### Field Layout

```kotlin
// Position after model field, before advanced settings
OutlinedTextField(
    value = uiState.maxTokens,
    onValueChange = { viewModel.onMaxTokensChange(it) },
    label = { Text(stringResource(R.string.max_tokens)) },
    placeholder = { Text(stringResource(R.string.max_tokens_placeholder)) },
    supportingText = {
        when {
            uiState.maxTokensError != null -> {
                Text(uiState.maxTokensError)
            }
            else -> {
                Text(stringResource(R.string.max_tokens_hint))
            }
        }
    },
    isError = uiState.maxTokensError != null,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    ),
    modifier = Modifier.fillMaxWidth()
)
```

#### String Resources

```xml
<!-- Label -->
<string name="max_tokens">Max Tokens</string>

<!-- Placeholder showing range -->
<string name="max_tokens_placeholder">1-4096 (optional)</string>

<!-- Hint text -->
<string name="max_tokens_hint">Maximum tokens in model response. Leave empty for default.</string>

<!-- Error messages -->
<string name="max_tokens_error_invalid">Must be a positive number</string>
<string name="max_tokens_error_range">Must be between 1 and %d</string>
```

### 5.3 Validation Logic

#### ViewModel Validation

```kotlin
fun onMaxTokensChange(value: String) {
    val trimmed = value.trim()

    when {
        trimmed.isEmpty() -> {
            // Empty is valid (null = provider default)
            _uiState.update { it.copy(
                maxTokens = "",
                maxTokensError = null
            )}
            validateForm()
        }
        trimmed.toIntOrNull() == null -> {
            _uiState.update { it.copy(
                maxTokens = trimmed,
                maxTokensError = "Must be a number"
            )}
        }
        trimmed.toInt() < 1 -> {
            _uiState.update { it.copy(
                maxTokens = trimmed,
                maxTokensError = "Must be at least 1"
            )}
        }
        trimmed.toInt() > getMaxTokensLimit() -> {
            _uiState.update { it.copy(
                maxTokens = trimmed,
                maxTokensError = "Cannot exceed ${getMaxTokensLimit()}"
            )}
        }
        else -> {
            _uiState.update { it.copy(
                maxTokens = trimmed,
                maxTokensError = null
            )}
            validateForm()
        }
    }
}

private fun getMaxTokensLimit(): Int {
    return when (uiState.value.providerType) {
        AIProvider.OPENAI_COMPATIBLE -> 4096
        AIProvider.ANTHROPIC -> 4096
    }
}
```

### 5.4 API Integration

#### Pass maxTokens to API Calls

When making API requests, include `maxTokens` if configured:

```kotlin
val request = OpenAIRequest(
    // ... other parameters
    maxTokens = settings.maxTokens,  // Null if not set
)
```

**Note:** Ensure backend API client code respects the `maxTokens` parameter when making requests to OpenAI/Anthropic APIs.

### 5.5 Testing Strategy

#### Unit Tests
1. Test validation logic with valid inputs
2. Test validation logic with invalid inputs (negative, zero, non-numeric)
3. Test null/empty value handling
4. Test provider-specific max limits

#### UI Tests
1. Test field displays correctly
2. Test validation error display
3. Test keyboard type and IME action
4. Test save with valid and invalid values

#### Integration Tests
1. Test API calls with maxTokens set
2. Test API calls without maxTokens (null)
3. Verify responses respect max_tokens limit

---

## 6. References

### API Documentation
- [Production best practices | OpenAI API](https://platform.openai.com/docs/guides/production-best-practices)
- [What are the best practices for managing my rate limits in the API?](https://help.openai.com/en/articles/6891753-what-are-the-best-practices-for-managing-my-rate-limits-in-the-api)
- [Max_Tokens - Best practice for long-form answers?](https://community.openai.com/t/max-tokens-best-practice-for-long-form-answers/151736)
- [Best Practices for Reducing Your OpenAI API Costs by 70%](https://ai.plainenglish.io/best-practices-for-reducing-your-openai-api-costs-by-70-8cf72a0b809f)
- [Building Production Apps with Claude API: Complete Technical Guide](https://medium.com/@reliabledataengineering/building-production-apps-with-claude-api-the-complete-technical-guide-to-prompts-tokens-and-8a740b9bab3a)
- [Official Claude Extended Thinking Documentation](https://platform.claude.com/docs/en/build-with-claude/extended-thinking)
- [AWS Bedrock Max Tokens Discussion](https://repost.aws/questions/QUmhH3_oqCTRm1PlshqIWqEg)

### UI/UX Resources
- [Configure text fields | Jetpack Compose - Android Developers](https://developer.android.com/develop/ui/compose/text/user-input)
- [Validate input as the user types | Jetpack Compose](https://developer.android.com/develop/ui/compose/quick-guides/content/validate-input)
- [How to Validate Fields Using Jetpack Compose in Android](https://medium.com/@rzmeneghelo/how-to-validate-fields-using-jetpack-compose-in-android-43be70597e82)
- [Command Your User Inputs with Jetpack Compose—Text Field Features](https://proandroiddev.com/command-your-user-inputs-with-jetpack-compose-text-field-features-hidden-in-plain-sight-47aaacc56aaf)
- [Creating a Form using Jetpack Compose and Material Design 3](https://www.waseefakhtar.com/android/form-using-jetpack-compose-and-material-design/)
- [Jetpack Compose Number Input in TextField - Stack Overflow](https://stackoverflow.com/questions/67687674/jetpack-compose-number-input-in-to-textfield)

---

**End of Research Report**

**Next Phase:** Code Assessment (Phase 5)
