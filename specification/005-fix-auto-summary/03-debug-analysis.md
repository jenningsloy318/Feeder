# Debug Analysis: Auto-Summary Not Triggering

## Date: 2026-01-01
## Bug Type: Missing Feature Implementation
## Severity: High

## Problem Statement
When a user opens an article to read with "Enable Auto Summary" setting ON, the article is NOT automatically summarized. The user must manually click the three-dots menu → "summarize" button.

## Root Cause Analysis

### 1. Expected Behavior (Not Implemented)
```
User opens article
    ↓
ArticleViewModel initialized with itemId
    ↓
Article content loads (articleContentFlow emits)
    ↓
[MISSING] Check if summaryEnabled is true
    ↓
[MISSING] Automatically call summarize()
    ↓
Summary appears in article view
```

### 2. Actual Behavior (Current Implementation)
```
User opens article
    ↓
ArticleViewModel initialized with itemId
    ↓
Article content loads (articleContentFlow emits)
    ↓
[MISSING AUTO-TRIGGER]
    ↓
User must manually tap menu → summarize
    ↓
Summary appears
```

### 3. Code Analysis: ArticleViewModel.kt

**Initialization Flow:**
```kotlin
class ArticleViewModel(di: DI, private val state: SavedStateHandle) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    private val aiApi: AIApi by instance()

    val itemId: Long = state["itemId"] ?: throw IllegalArgumentException("Missing itemId")

    // Article data flow
    private val articleFlow = repository.getArticleFlow(itemId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    // Article content flow (parsed)
    private val articleContentFlow: StateFlow<LinearArticle> =
        combine(articleFlow, displayFullTextOverride) { article, fullTextOverride ->
            article?.let { it to (fullTextOverride ?: it.fullTextByDefault) }
        }.filterNotNull()
            .map { (article, displayFullText) -> parseArticleContent(article, displayFullText) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = LinearArticle(emptyList()))

    // AI summary state (starts Empty)
    private val aiSummary: MutableStateFlow<AISummaryState> = MutableStateFlow(AISummaryState.Empty)

    // View state combines all flows
    val viewState: StateFlow<ArticleScreenViewState> = combine(
        articleFlow,
        textToDisplay,
        articleContentFlow,
        toolbarVisible,
        repository.linkOpener,
        repository.useDetectLanguage,
        ttsStateHolder.ttsState,
        ttsStateHolder.availableLanguages,
        repository.aiSettingsFlow,
        aiSummary,
    ) { params ->
        val showSummarize = (params[8] as AISettings).isValid && !article?.link.isNullOrEmpty()
        // ... build view state
    }

    // Manual summarization method
    fun summarize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                aiSummary.value = AISummaryState.Loading
                val content = loadArticleContent()
                aiSummary.value = AISummaryState.Result(value = aiApi.summarize(content))
            } catch (e: Exception) {
                aiSummary.value = AISummaryState.Result(
                    value = AIClient.SummaryResult.Error(content = e.message ?: "Unknown error")
                )
            }
        }
    }
}
```

**Key Observations:**
1. ✅ `articleContentFlow` emits when article content is ready
2. ✅ `repository.summaryEnabled` StateFlow exists (from spec-04)
3. ✅ `summarize()` method exists and works when called manually
4. ❌ **NO AUTO-TRIGGER LOGIC** - No code to automatically call `summarize()` when article loads
5. ❌ No observation of `repository.summaryEnabled` in ArticleViewModel
6. ❌ No `init` block or Flow collection to trigger auto-summary

### 4. Code Analysis: ArticleScreen.kt

**Manual Trigger Location:**
```kotlin
@Composable
fun ArticleScreen(
    viewState: ArticleScreenViewState,
    onSummarize: () -> Unit,  // Passed from ViewModel
    // ...
) {
    // Three-dots menu
    DropdownMenu(expanded = viewState.showToolbarMenu, onDismissRequest = { onShowToolbarMenu(false) }) {
        // Share item
        DropdownMenuItem(onClick = { onShowToolbarMenu(false); onShare() }, ...)

        // Summarize item (conditional)
        if (viewState.showSummarize) {
            DropdownMenuItem(
                onClick = {
                    onShowToolbarMenu(false)
                    onSummarize()  // Manual trigger
                },
                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                text = { Text(stringResource(id = R.string.summarize)) }
            )
        }
    }

    // Summary display (if not Empty)
    if (viewState.aiSummary !is AISummaryState.Empty) {
        SummarySection(viewState.aiSummary)
    }
}
```

**Key Observations:**
1. ✅ Manual trigger works: `onSummarize = { viewModel.summarize() }`
2. ✅ Summary displays correctly when `aiSummary` is not Empty
3. ❌ **NO AUTO-TRIGGER** in Compose UI (no LaunchedEffect)

### 5. Missing Implementation

**What's Missing:**
```kotlin
// Option 1: LaunchedEffect in ArticleScreen.kt (RECOMMENDED)
@Composable
fun ArticleScreen(...) {
    // Auto-trigger when article loads and summary is enabled
    LaunchedEffect(viewState.articleId, viewState.showSummarize) {
        if (viewState.showSummarize &&
            viewState.aiSummary is AISummaryState.Empty &&
            viewState.articleContent.isNotEmpty()) {
            onSummarize()
        }
    }
}

// Option 2: Init block in ArticleViewModel.kt
init {
    viewModelScope.launch {
        combine(
            articleContentFlow,
            repository.summaryEnabled
        ) { content, enabled ->
            content to enabled
        }.filter { (content, enabled) ->
            enabled && content.items.isNotEmpty()
        }.collect { (content, enabled) ->
            if (aiSummary.value is AISummaryState.Empty) {
                summarize()
            }
        }
    }
}
```

## Implementation Location

**Recommended: ArticleScreen.kt**
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- **Location:** After `@Composable fun ArticleScreen(...)` function signature
- **Pattern:** Use `LaunchedEffect` with keys `articleId` and `showSummarize`

**Alternative: ArticleViewModel.kt**
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Location:** In `init` block or after `articleContentFlow` declaration
- **Pattern:** Collect `combine(articleContentFlow, repository.summaryEnabled)`

## Trigger Conditions

Auto-summary should trigger when ALL of these are true:
1. ✅ Article is loaded (`articleId` is valid)
2. ✅ Article content is parsed (`articleContentFlow` has emitted)
3. ✅ `repository.summaryEnabled` is `true`
4. ✅ `AISettings.isValid` is `true` (AI provider configured)
5. ✅ `aiSummary` is `AISummaryState.Empty` (no existing summary)
6. ✅ Article has content to summarize (`!article.link.isNullOrEmpty()`)

## Dependencies

**Required Data:**
- `repository.summaryEnabled` - StateFlow<Boolean> from SettingsStore
- `articleContentFlow` - StateFlow<LinearArticle> (parsed content)
- `aiSummary` - MutableStateFlow<AISummaryState> (current summary state)
- `AISettings.isValid` - Boolean (AI provider configured)

**Required Methods:**
- `summarize()` - Already exists in ArticleViewModel
- `loadArticleContent()` - Already exists in ArticleViewModel

## Prevention of Duplicate Summaries

**Must Check:**
1. `aiSummary.value is AISummaryState.Empty` before calling
2. Use `articleId` as LaunchedEffect key to prevent re-trigger on recomposition

**Edge Cases:**
- Screen rotation - Don't re-summarize (use `articleId` as key)
- User navigates back - Should summarize again if summary is Empty
- User manually summarizes - Respect manual summary (don't overwrite)

## Error Handling

**Already Handled in `summarize()`:**
```kotlin
try {
    aiSummary.value = AISummaryState.Loading
    val content = loadArticleContent()
    aiSummary.value = AISummaryState.Result(value = aiApi.summarize(content))
} catch (e: Exception) {
    aiSummary.value = AISummaryState.Result(
        value = AIClient.SummaryResult.Error(content = e.message ?: "Unknown error")
    )
}
```

**No Additional Error Handling Needed** - Auto-trigger uses same method as manual

## Testing Strategy

**Manual Testing:**
1. Enable "Enable Auto Summary" in Settings
2. Open any article
3. Verify loading indicator appears
4. Verify summary appears after content loads

**Edge Cases:**
1. Disable auto-summary → Open article → No summary should appear
2. Enable auto-summary → Open article → Summary should appear
3. Open article → Wait for summary → Navigate back → Reopen → Should NOT re-summarize (cached)
4. Open article → Wait for summary → Rotate screen → Should NOT re-summarize

## Summary

**Root Cause:** Auto-trigger logic completely missing from codebase

**Solution:** Add `LaunchedEffect` in `ArticleScreen.kt` to automatically call `onSummarize()` when conditions are met

**Implementation Effort:** Low (1-2 hours)
- Add LaunchedEffect with proper keys
- Check all trigger conditions
- Test with various scenarios

**Risk Level:** Low
- Uses existing `summarize()` method (proven to work)
- No changes to business logic
- Only adds trigger mechanism
