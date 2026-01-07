# Task List: Add max_tokens Configuration

**Spec ID:** 027-add-max-token-config
**Date:** 2026-01-07
**Total Tasks:** 10
**Estimated Time:** 65 minutes

---

## Quick Reference

| Task | Description | File | Est. | Status |
|------|-------------|------|------|--------|
| T1 | Add maxTokens to OpenAISettings | AISettings.kt | 2m | ⏳ Pending |
| T2 | Add maxTokens to AnthropicSettings | AISettings.kt | 2m | ⏳ Pending |
| T3 | Implement updateMaxTokens() | ProviderEditViewModel.kt | 10m | ⏳ Pending |
| T4 | Add UI state properties | ProviderEditViewModel.kt | 5m | ⏳ Pending |
| T5 | Add max_tokens TextField | ProviderEditScreen.kt | 10m | ⏳ Pending |
| T6 | Add English strings | strings.xml (en) | 3m | ⏳ Pending |
| T7 | Add Chinese strings | strings.xml (zh-CN) | 3m | ⏳ Pending |
| T8 | Write unit tests | ProviderEditViewModelTest.kt | 15m | ⏳ Pending |
| T9 | Write UI tests | ProviderEditScreenTest.kt | 10m | ⏳ Pending |
| T10 | Manual testing | - | 5m | ⏳ Pending |

---

## Detailed Tasks

### Task 1: Add maxTokens to OpenAISettings
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`
**Line:** ~23 (after timeoutSeconds)
**Code:**
```kotlin
val maxTokens: Int? = null,  // Maximum tokens for model output
```
**Status:** ⏳ Pending

### Task 2: Add maxTokens to AnthropicSettings
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`
**Line:** ~69 (after timeoutSeconds)
**Code:**
```kotlin
val maxTokens: Int? = null,  // Maximum tokens for model output
```
**Status:** ⏳ Pending

### Task 3: Implement updateMaxTokens()
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
**Location:** After updateModelId() method (~line 268)
**Code:** See implementation plan
**Status:** ⏳ Pending

### Task 4: Add UI state properties
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditViewModel.kt`
**Location:** In ProviderEditUiState (~line 58)
**Code:**
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
**Status:** ⏳ Pending

### Task 5: Add max_tokens TextField
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt`
**Location:** In ProviderEditForm, after Model field
**Code:** See implementation plan
**Status:** ⏳ Pending

### Task 6: Add English strings
**File:** `app/src/main/res/values/strings.xml`
**Code:**
```xml
<string name="max_tokens">Max Tokens</string>
<string name="max_tokens_placeholder">1-4096 (optional)</string>
<string name="max_tokens_hint">Maximum tokens in response. Leave empty for default.</string>
<string name="max_tokens_error_nan">Must be a number</string>
<string name="max_tokens_error_min">Must be at least 1</string>
<string name="max_tokens_error_max">Cannot exceed %d</string>
```
**Status:** ⏳ Pending

### Task 7: Add Chinese strings
**File:** `app/src/main/res/values-zh-rCN/strings.xml`
**Code:**
```xml
<string name="max_tokens">最大令牌数</string>
<string name="max_tokens_placeholder">1-4096（可选）</string>
<string name="max_tokens_hint">响应的最大令牌数。留空则使用默认值。</string>
<string name="max_tokens_error_nan">必须是数字</string>
<string name="max_tokens_error_min">必须至少为 1</string>
<string name="max_tokens_error_max">不能超过 %d</string>
```
**Status:** ⏳ Pending

### Task 8: Write unit tests
**File:** `app/src/test/.../ProviderEditViewModelTest.kt`
**Tests:**
- updateMaxTokens with valid number
- updateMaxTokens with empty string
- updateMaxTokens with invalid text
- updateMaxTokens with out-of-range values
**Status:** ⏳ Pending

### Task 9: Write UI tests
**File:** `app/src/androidTest/.../ProviderEditScreenTest.kt`
**Tests:**
- Max tokens field displays
- Field accepts input
- Validation errors display
**Status:** ⏳ Pending

### Task 10: Manual testing
**Checklist:**
- [ ] Create new provider with max_tokens
- [ ] Edit existing provider, add max_tokens
- [ ] Leave max_tokens empty (verify works)
- [ ] Enter invalid text (verify error)
- [ ] Enter negative number (verify error)
- [ ] Enter number > 4096 (verify error)
**Status:** ⏳ Pending

---

## Progress Tracking

**Completed:** 0/10 tasks (0%)
**In Progress:** 0 tasks
**Blocked:** 0 tasks
**Estimated Time Remaining:** 65 minutes

---

## Execution Order

**Phase 1: Data Model (Tasks 1-2)** - 4 minutes
**Phase 2: ViewModel (Tasks 3-4)** - 15 minutes
**Phase 3: UI (Tasks 5-7)** - 16 minutes
**Phase 4: Testing (Tasks 8-10)** - 30 minutes

---

**Ready for Execution: Phase 8**
