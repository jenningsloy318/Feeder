# Technical Specification: Fix Anthropic Settings Update Bug

**Feature ID**: 001
**Bug ID**: 002
**Date**: 2026-01-01
**Status**: Draft
**Related**: [./03-debug-analysis.md](./03-debug-analysis.md)

---

## Overview

### Summary

Fix a critical bug where Anthropic (Claude) API settings show "invalid setting" even when correct API key and model ID are entered. The root cause is that the provider type (`aiProviderType`) is not updated when users modify AI settings, causing the reactive settings flow to emit the wrong provider's configuration.

### Goals

1. ✅ Ensure `aiProviderType` is synchronized whenever AI settings are updated
2. ✅ Fix `aiSettingsFlow` to emit the correct provider's settings
3. ✅ Eliminate false "invalid setting" errors for Anthropic provider
4. ✅ Maintain backward compatibility with existing OpenAI settings

---

## Technical Design

### Architecture Changes

The fix requires **no architecture changes**. The existing flow-based architecture is correct; only the event handling logic needs to be updated.

#### Current (Buggy) Flow

```
User enters Anthropic API key
    ↓
UpdateSettings(Anthropic) event
    ↓
setAnthropicSettings() called  ← Updates Anthropic settings
    ↓
aiProviderType = OPENAI_COMPATIBLE  ← NOT updated! (BUG)
    ↓
aiSettingsFlow emits: AISettings.OpenAI(empty)  ← Wrong provider!
    ↓
isValid = false  ← Validation fails
```

#### Fixed Flow

```
User enters Anthropic API key
    ↓
UpdateSettings(Anthropic) event
    ↓
setAIProviderType(ANTHROPIC)  ← NEW: Update provider type
setAnthropicSettings()  ← Update Anthropic settings
    ↓
aiProviderType = ANTHROPIC  ← Correct!
    ↓
aiSettingsFlow emits: AISettings.Anthropic(settings)  ← Correct!
    ↓
isValid = true  ← Validation succeeds
```

---

### Components to Modify

#### Component 1: SettingsViewModel

**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

**Purpose**: Handle AI settings update events

**Changes**:
- Update `onOpenAISettingsEvent()` to call `setAIProviderType()` before updating provider-specific settings
- This ensures the reactive flow switches to the correct provider

**Before**:
```kotlin
is AISettingsEvent.UpdateSettings ->
    when (event.settings) {
        is AISettings.OpenAI ->
            repository.setOpenAISettings(event.settings.openaiSettings)
        is AISettings.Anthropic ->
            repository.setAnthropicSettings(event.settings.anthropicSettings)
    }
```

**After**:
```kotlin
is AISettingsEvent.UpdateSettings ->
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
```

---

## Implementation Approach

### Files Modified

| File | Changes | Lines Affected |
|------|---------|----------------|
| `SettingsViewModel.kt` | Add `setAIProviderType()` calls | 167-177 |

### No Changes Required To

- ✅ `SettingsStore.kt` - Architecture is correct
- ✅ `Repository.kt` - Properly delegates to SettingsStore
- ✅ `ArticleViewModel.kt` - Correctly uses `aiSettingsFlow`
- ✅ `AISettings.kt` - Validation logic is correct
- ✅ UI components - Correctly trigger events

---

## Testing Strategy

### Unit Tests

#### Test: Provider Type Updates on Settings Change

**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModelTest.kt`

```kotlin
@Test
fun `UpdateSettings event updates provider type for Anthropic`() {
    // Given
    val viewModel = SettingsViewModel(di)
    val anthropicSettings = AnthropicSettings(
        key = "sk-ant-test",
        modelId = "claude-3-5-sonnet-20241022"
    )

    // When
    viewModel.onOpenAISettingsEvent(
        AISettingsEvent.UpdateSettings(
            AISettings.Anthropic(anthropicSettings)
        )
    )

    // Then
    assertEquals(AIProvider.ANTHROPIC, repository.aiProviderType.value)
    assertEquals(anthropicSettings, repository.anthropicSettings.value)
}
```

### Integration Tests

#### Test: Settings Flow Emits Correct Variant

**File**: `app/src/androidTest/java/com/nononsenseapps/feeder/archmodel/SettingsFlowTest.kt`

```kotlin
@Test
fun `aiSettingsFlow emits Anthropic settings after update`() = runTest {
    // Given: Provider is OpenAI
    settingsStore.setAIProviderType(AIProvider.OPENAI_COMPATIBLE)

    // When: Update to Anthropic settings
    val anthropicSettings = AnthropicSettings(
        key = "sk-ant-test",
        modelId = "claude-3-5-sonnet-20241022"
    )
    settingsStore.setAIProviderType(AIProvider.ANTHROPIC)
    settingsStore.setAnthropicSettings(anthropicSettings)

    // Then: Flow should emit Anthropic settings
    val emitted = settingsStore.aiSettingsFlow.value
    assertTrue(emitted is AISettings.Anthropic)
    assertEquals(anthropicSettings, (emitted as AISettings.Anthropic).anthropicSettings)
}
```

### Manual Test Cases

| Scenario | Steps | Expected Result |
|----------|-------|-----------------|
| Switch to Anthropic | 1. Open Settings → AI integration<br>2. Select "Anthropic (Claude)"<br>3. Enter API key<br>4. Select model<br>5. Save | Settings saved without error |
| Switch back to OpenAI | 1. Open Settings → AI integration<br>2. Select "OpenAI-compatible"<br>3. Enter API key<br>4. Select model<br>5. Save | Settings saved without error |
| Persist settings | 1. Configure Anthropic<br>2. Close app<br>3. Reopen app<br>4. Check settings | Settings are preserved |
| Summarize button | 1. Configure Anthropic with valid settings<br>2. Open any article | Summarize button appears in toolbar |

---

## Edge Cases Handled

### 1. Empty Settings

**Scenario**: User selects Anthropic but doesn't enter API key

**Expected**: `isValid = false`, summarize button hidden (correct behavior)

### 2. Rapid Provider Switching

**Scenario**: User quickly switches between OpenAI and Anthropic multiple times

**Expected**: Each switch updates provider type correctly, no race conditions

### 3. Existing OpenAI Users

**Scenario**: User has existing OpenAI settings and switches to Anthropic

**Expected**: OpenAI settings are preserved in SharedPreferences, can switch back

---

## Backward Compatibility

### No Breaking Changes

- ✅ Existing OpenAI settings are preserved
- ✅ SharedPreferences keys remain unchanged
- ✅ UI behavior unchanged for OpenAI provider
- ✅ No migration required

---

## References

- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Implementation Plan: [./07-implementation-plan.md](./07-implementation-plan.md)
- Task List: [./08-task-list.md](./08-task-list.md)
- API Documentation: [./api-documentation.md](./api-documentation.md)

---

## Implementation Status

**Status**: ✅ Fixed (2026-01-01)

**Fix Applied**:
1. Modified `SettingsViewModel.onOpenAISettingsEvent()` to synchronize provider type
2. Removed Anthropic hardcoded model list (users input directly)
3. Fixed "no models" message for Anthropic provider

**Build**: SUCCESS
**Testing**: Pending (APK ready for installation)
