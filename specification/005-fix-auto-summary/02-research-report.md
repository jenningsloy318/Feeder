# Research Report: Auto-Summary Trigger Implementation

## Date: 2026-01-01
## Researcher: AI Assistant

## Executive Summary
Research indicates that auto-summarization should be triggered using **LaunchedEffect** in the ArticleViewModel when the article content loads and `summaryEnabled` setting is true. The manual summarization already works correctly via the `summarize()` method in ArticleViewModel.

## Key Findings

### 1. Current Architecture

**Manual Summarization Flow:**
```
User clicks menu → onSummarize() → viewModel.summarize()
    ↓
ArticleViewModel.summarize()
    ↓
aiApi.summarize(content)
    ↓
Update aiSummary StateFlow
    ↓
UI displays summary
```

**Key Components:**
- `ArticleViewModel.kt` - Contains `summarize()` method (line 395)
- `ArticleScreen.kt` - Compose UI with menu item (line 261-276)
- `AIApi.kt` - AI summarization logic (line 78)
- `SettingsStore.kt` - Contains `summaryEnabled` StateFlow (line 709)

### 2. Auto-Summary Trigger Best Practices (Jetpack Compose)

**LaunchedEffect Pattern for Auto-Trigger:**
```kotlin
@Composable
fun ArticleScreen(...) {
    val viewModel: ArticleViewModel = viewModel()

    // Trigger auto-summary when article loads and setting is enabled
    LaunchedEffect(articleId, summaryEnabled) {
        if (summaryEnabled && articleContent != null) {
            viewModel.summarize()
        }
    }
}
```

**Why LaunchedEffect:**
- Executes when key parameters change (articleId, summaryEnabled)
- Runs in coroutine scope (can call suspend functions)
- Lifecycle-aware (cancels when composable leaves composition)
- One-time execution per key change
- Standard pattern for side effects in Compose

**Alternative: ViewModel Initialization**
```kotlin
class ArticleViewModel(...) : ViewModel() {
    private val hasAutoSummarized = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(articleFlow, repository.summaryEnabled) { article, enabled ->
                article to enabled
            }.filterNotNull()
                .collect { (article, enabled) ->
                    if (enabled && !hasAutoSummarized.value) {
                        summarize()
                        hasAutoSummarized.value = true
                    }
                }
        }
    }
}
```

**Pros/Cons:**
- LaunchedEffect: Simpler, UI-driven, respects lifecycle
- ViewModel init: Better separation of concerns, testable

### 3. Research: Kotlin Flow/StateFlow Auto-Trigger Patterns

**Pattern 1: Combine + Collect (ViewModel)**
```kotlin
init {
    viewModelScope.launch {
        combine(
            articleContentFlow,
            repository.summaryEnabled
        ) { content, enabled ->
            content to enabled
        }.filter { (content, enabled) ->
            enabled && content.isNotEmpty()
        }.collect { (content, _) ->
            if (aiSummary.value is AISummaryState.Empty) {
                summarize()
            }
        }
    }
}
```

**Pattern 2: LaunchedEffect (UI)**
```kotlin
LaunchedEffect(viewState.articleId, viewState.showSummarize) {
    if (viewState.showSummarize &&
        viewState.aiSummary is AISummaryState.Empty &&
        viewState.articleContent.isNotEmpty()) {
        onSummarize()
    }
}
```

### 4. Comparison: Manual vs Auto Summarization

| Aspect | Manual | Auto (Proposed) |
|--------|--------|-----------------|
| Trigger | User action (menu click) | Article load |
| Timing | On-demand | Automatic |
| Check summaryEnabled | Implicit (via AIApi) | Explicit before calling |
| Deduplication | N/A | Must prevent repeat calls |
| Error handling | Via AIApi | Same as manual |

### 5. Key Implementation Considerations

**1. Prevent Duplicate Summaries**
- Only summarize if `aiSummary` is `AISummaryState.Empty`
- Use a flag or check existing state

**2. Respect User Preference**
- Check `repository.summaryEnabled` before triggering
- Don't auto-summarize when disabled

**3. Timing**
- Wait for article content to be fully loaded
- Use `articleContentFlow` not `articleFlow` (ensures parsing complete)

**4. Lifecycle Awareness**
- Cancel if user leaves screen before summary completes
- Use `viewModelScope` (cancels on ViewModel clear)

**5. Performance**
- Don't block UI rendering
- Run in `Dispatchers.IO` (already done in `summarize()`)

### 6. Code Analysis: Existing summarize() Method

**Location:** `ArticleViewModel.kt:395-411`

```kotlin
fun summarize() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            aiSummary.value = AISummaryState.Loading
            val content = loadArticleContent()
            aiSummary.value =
                AISummaryState.Result(
                    value = aiApi.summarize(content),
                )
        } catch (e: Exception) {
            aiSummary.value =
                AISummaryState.Result(
                    value = com.nononsenseapps.feeder.ai.AIClient.SummaryResult.Error(content = e.message ?: "Unknown error"),
                )
        }
    }
}
```

**Key Points:**
- Already uses `viewModelScope` (lifecycle-aware)
- Already handles errors gracefully
- Updates `aiSummary` StateFlow (triggers UI recomposition)
- Uses `Dispatchers.IO` (non-blocking)
- Can be called directly from auto-trigger logic

### 7. Recommended Implementation Strategy

**Option A: LaunchedEffect in ArticleScreen (RECOMMENDED)**

**Pros:**
- Simple implementation
- UI-driven (matches user expectation)
- Automatically respects lifecycle
- Easy to test in UI

**Cons:**
- Business logic in UI layer
- Tightly coupled to Compose

**Implementation:**
```kotlin
// In ArticleScreen.kt
LaunchedEffect(viewState.articleId, viewState.showSummarize) {
    if (viewState.showSummarize &&
        viewState.aiSummary is AISummaryState.Empty) {
        onSummarize()
    }
}
```

**Option B: ViewModel init with Flow combine**

**Pros:**
- Business logic in ViewModel
- Testable without Compose
- Follows MVVM principles

**Cons:**
- More complex
- Need to prevent duplicate calls
- Must manage state flags

### 8. Edge Cases to Handle

1. **Article already has summary** - Check `aiSummary` state
2. **User disables auto-summary** - Respect setting immediately
3. **Article content not yet loaded** - Wait for `articleContentFlow`
4. **Screen rotation** - Don't re-summarize (use unique key)
5. **User navigates away** - Cancel via `viewModelScope`
6. **Summary fails** - Show error, allow retry

## Technical Decisions

### Decision 1: Use LaunchedEffect in UI

**Rationale:**
- Simpler implementation
- Matches Compose best practices
- Existing manual trigger already in UI
- Less code to maintain

### Decision 2: Check aiSummary State

**Rationale:**
- Prevents duplicate API calls
- Respects manual summaries user may have generated
- Allows user to manually re-summarize if needed

### Decision 3: Use articleId as LaunchedEffect Key

**Rationale:**
- Prevents re-summarization on recomposition
- Only triggers when article changes
- Survives screen rotation

## Next Steps

1. **Debug Analysis** - Locate exact trigger point in ArticleViewModel
2. **Code Assessment** - Assess current article loading flow
3. **Specification** - Write detailed implementation spec

## References

- Jetpack Compose Side Effects: https://developer.android.com/jetpack/compose/side-effects
- LaunchedEffect documentation: https://developer.android.com/reference/kotlin/androidx/compose/runtime/LaunchedEffect
- StateFlow best practices: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
