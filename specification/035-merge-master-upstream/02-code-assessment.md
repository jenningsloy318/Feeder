# Code Assessment: merge master into ai-features

**Divergence point**: `748fd571` (tag v2.16.1)
**Master commits since divergence**: ~50 (feature + lang + deps)
**AI-features commits since divergence**: ~40 (specs 17-34)

---

## 1. OpenAIApi.kt -- CRITICAL (modify/delete conflict)

### Master version (`openai/OpenAIApi.kt`)
- Single file handling summary via `com.aallam.openai:openai-client` (BOM 4.0.1)
- Commit `c973eab5` added markdown formatting instructions to the system prompt:
  ```
  "For readability use markdown formatting: **bold** for emphasis, *italics* for quotes,
   bullet points (-) for lists, # headers for sections, and > for block quotes."
  ```
- Summary parsing: simple `Lang: "XX"` first-line regex, remainder is content
- `MarkdownToHtmlConverter` converts markdown to HTML, then `htmlStringToAnnotatedString()` renders it
- No translation support

### AI-features replacement (`ai/` package)
- **Deleted** `openai/OpenAIApi.kt` entirely
- Replaced with multi-provider architecture:
  - `AIClient.kt` -- interface with `SummaryResult`, `TranslationResult`, `ModelsResult`
  - `OpenAICompatibleClient.kt` -- uses `com.openai:openai-java` SDK 4.13.0 (official)
  - `AnthropicClient.kt` -- uses `com.anthropic:anthropic-java` SDK 2.11.1 (official)
  - `AIApi.kt` -- high-level API with timeout/language settings
  - `SummaryResponseParser.kt` -- robust JSON extraction with brace-matching + legacy fallback
- Both clients use JSON structured output format (not `Lang:` prefix)
- Summary prompt is much richer: JSON with `language`, `title`, `keyPoints`, `summary`, `sentiment`
- Full translation support with paragraph-by-paragraph progress

### KEY QUESTION: Does ai-features cover master's markdown summary parsing?

**YES, and it goes much further.** Analysis:

| Feature | Master | AI-features |
|---------|--------|-------------|
| Summary prompt | Text format with markdown hints | JSON structured output with markdown in `summary` field |
| Markdown in summaries | Prompt asks for it, renders via `MarkdownToHtmlConverter` -> AnnotatedString | Prompt asks for it, renders via `MarkdownContentSafe` (Mikepenz library) |
| Response parsing | `Lang:` prefix regex | `SummaryResponseParser` with JSON extraction, legacy fallback, sanitization |
| Error handling | Basic exception catch | `sanitizeErrorMessage()`, `containsRawJson()` UI guard |
| SDK | `com.aallam.openai:openai-client` (3rd party) | `com.openai:openai-java` (official) + `com.anthropic:anthropic-java` |

**The master markdown summary enhancement is FULLY SUPERSEDED by ai-features.** The ai-features summary prompt already requests markdown in the JSON `summary` field, and the rendering pipeline uses the Mikepenz library for comprehensive CommonMark rendering.

### Resolution
- **Keep ai-features' entire `ai/` package** (delete incoming `openai/OpenAIApi.kt`)
- **Do NOT port** master's `MarkdownToHtmlConverter` approach (ai-features uses Mikepenz directly)
- **Do NOT port** master's prompt changes (ai-features' prompt is strictly better)
- The old `com.aallam.openai:openai-client-bom` dependency can be removed entirely

---

## 2. ArticleViewModel.kt -- HIGH risk conflict

### Master version (551 lines)
- Uses `OpenAIApi` (old package) for summary
- `openAiSummary: MutableStateFlow<OpenAISummaryState>` -- summary state
- `OpenAISummaryState.Result` contains `OpenAIApi.SummaryResult` + `annotatedStrings: List<AnnotatedString>`
- `convertSummaryToAnnotatedStrings()` -- uses `MarkdownToHtmlConverter` -> `htmlStringToAnnotatedString()`
- `summarize()` -- launches coroutine, no cancellation, no `CancellationException` handling
- `viewState` combines 10 flows (no translation)
- `init{}` block: auto-summarize on feed `summarizeOnOpen`
- Import: `com.nononsenseapps.feeder.openai.OpenAIApi`

### AI-features version (780 lines)
- Uses `AIApi` (new package) for summary
- `aiSummary: MutableStateFlow<AISummaryState>` -- renamed from `openAiSummary`
- `AISummaryState.Result` contains `AIClient.SummaryResult` (no AnnotatedString conversion)
- **No `convertSummaryToAnnotatedStrings()`** -- rendering moved to composable layer (`MarkdownContentSafe`)
- `summarize()` -- stores job in `summarizeJob`, supports cancellation via `cancelSummarize()`
- `translationState: MutableStateFlow<TranslationState>` -- entire translation system
- `translate()`, `cancelTranslation()`, `extractTranslatableParagraphs()` -- translation pipeline
- `viewState` combines 11 flows (adds `translationState`)
- `init{}` block: auto-fetch full text, auto-summarize (with `summaryEnabled` check), auto-translate
- Additional imports: AI package, CancellationException, Job, etc.

### Conflict areas

1. **DI injection**: Master has `openAIApi: OpenAIApi`, ai-features has `aiApi: AIApi`
2. **Summary state type**: `OpenAISummaryState` vs `AISummaryState`
3. **Summary rendering**: Master converts to AnnotatedString in ViewModel; ai-features renders markdown in composable
4. **ViewState interface**: Master has `openAiSummary: OpenAISummaryState`; ai-features has `aiSummary: AISummaryState` + `translation: TranslationState`
5. **init block**: Completely different auto-trigger logic

### Resolution
- **Keep ai-features version entirely**
- Master's `convertSummaryToAnnotatedStrings()` is obsolete (replaced by composable-level `MarkdownContentSafe`)
- No code from master's ArticleViewModel needs to be ported

---

## 3. ArticleScreen.kt -- MEDIUM-HIGH risk conflict

### Master version (621 lines)
Three features added since divergence:
1. **Selected text crash fix** (`0416165d`): Switched from `LazyColumn` to `Column` with `ScrollState`
   - `articleScrollState: ScrollState` parameter
   - `rememberScrollState()` instead of `rememberLazyListState()`
   - `ColumnArticleContent` composable instead of `linearArticleContent` LazyListScope extension
   - Removed `LazyColumnScrollbar` dependency
2. **Paging** (`70ed0f25`): Added `isPagingMode` / `isAnimatedPaging` / volume button support
   - `mainActivityViewModel` parameter in outer `ArticleScreen`
   - `LaunchedEffect` for `scrollCommand` collection
   - Invisible left/right tap zones for screen-edge paging
   - Forwarded from `MainActivityViewModel.scrollCommand`
3. **Markdown summaries** (`c973eab5`): Updated `SummarySection` to use `AnnotatedString` list

### AI-features version (793 lines)
- **Still uses `LazyColumn`** with `LazyListState` and `LazyColumnScrollbar`
- `linearArticleContent()` LazyListScope extension (supports `translatedParagraphs` parameter)
- `CircleProgressIconButton` for summarize/translate (toolbar icons with circular progress)
- `WithFeederTextToolbar` / `TextSelectionMenuHandler` for text selection context menu
- Translation UI: `TranslationStatusSection`, `TranslationErrorSection`
- Summary rendering: `MarkdownContentSafe` composable (not AnnotatedString list)
- NO paging support
- Uses `articleListState: LazyListState` throughout

### Critical architectural divergence: ScrollState vs LazyColumn

Master switched from `LazyColumn` to `Column + ScrollState` to fix the selected-text-offscreen crash.
AI-features still uses `LazyColumn` because the translation paragraph system relies on `LazyListScope.linearArticleContent()`.

**This is the most complex conflict in the entire merge.**

Options:
1. **Keep ai-features' LazyColumn approach**: Risk the selected-text crash remaining. Need to find alternative fix.
2. **Port master's ScrollState approach**: Must rewrite `linearArticleContent()` from a `LazyListScope` extension to a `@Composable` function, and adapt all translation paragraph rendering accordingly.
3. **Hybrid**: Use LazyColumn but apply the text selection fix differently.

### Recommended resolution

**Option 2 (port master's ScrollState)** is recommended because:
- The selected-text crash is a real production issue that upstream fixed
- The paging feature requires `ScrollState` (uses `scrollTo()` / `animateScrollTo()` with pixel offsets)
- `ColumnArticleContent` is a simpler composable that can be extended with translation support

However, this requires:
- Converting `linearArticleContent()` from `LazyListScope` extension to `@Composable` function
- Adding `translatedParagraphs` and `parentTranslationIndex` params to `ColumnArticleContent`
- Porting the `computeParagraphIndices()` logic from ai-features' LazyList version
- Adding paging overlay (invisible left/right tap zones)
- Adding `CircleProgressIconButton` for toolbar icons
- Keeping translation status sections
- Removing `LazyColumnScrollbar` dependency (`my.nanihadesuka.compose`)

### Resolution for individual master features

| Feature | Resolution |
|---------|-----------|
| Selected text crash fix (ScrollState) | Port: convert ReaderView to ScrollState approach |
| Paging (volume buttons + tap zones) | Port: add `MainActivityViewModel` integration, scroll commands, tap zones |
| Markdown summaries (AnnotatedString) | Skip: ai-features uses MarkdownContentSafe composable (better approach) |

---

## 4. Build System Changes

### Version conflicts

| Dependency | Master | AI-features | Resolution |
|-----------|--------|-------------|-----------|
| `androidPlugin` (AGP) | **9.0.0** | 8.11.1 | Take master (9.0.0) |
| `ksp` | **2.3.6** | 2.2.20-2.0.3 | Take master (2.3.6) -- must match AGP 9 |
| `okhttp` | **5.3.2** | 5.1.0 | Take master (5.3.2) |
| `kotlin` | 2.2.20 | 2.2.20 | Same -- no conflict |
| `compileSdk` | 36 | 36 | Same -- no conflict |

### New dependencies on ai-features (keep all)
- `openai-java = "4.13.0"` (official OpenAI SDK)
- `anthropic-java = "2.11.1"` (official Anthropic SDK)
- `anthropic-java-okhttp` (Anthropic OkHttp transport)
- `mikepenz-markdown = "0.38.1"` (markdown rendering)
- `mikepenz-markdown-m3`, `mikepenz-markdown-coil3`
- `sh.calvin.reorderable:reorderable:2.4.0` (drag-and-drop)

### New dependencies on master (port all)
- `glance = "1.1.1"` (widget framework)
- `glance-appwidget`, `glance-material3`, `glance-preview`, `glance-appwidget-preview`

### Dependencies to remove
- `openai-client-bom` / `openai-client` (old `com.aallam.openai` library -- replaced by official SDK)
- `ktor-client-okhttp` (was transitive dependency of old OpenAI client)
- `lazycolumnscrollbar` (`my.nanihadesuka.compose`) -- if switching to ScrollState

### build.gradle.kts resolution
- Take ai-features' AI SDK dependencies (`openai-java`, `anthropic-java`, `anthropic-java-okhttp`)
- Take ai-features' markdown dependencies (`mikepenz-markdown`, etc.)
- Take ai-features' reorderable dependency
- Port master's Glance dependencies
- Remove old `openai-client-bom` / `openai-client` / `ktor-client-okhttp`
- Take ai-features' compiler args (`-Xannotation-default-target=param-property`, `-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi`)

### AGP 9.0.0 impact
- AGP 9.0.0 requires KSP 2.3.6 (not 2.2.20-2.0.3)
- AGP 9.0.0 may require Gradle updates (check `gradle-wrapper.properties`)
- Build script syntax may have minor changes
- The `compileSdk = 36` is already set on both branches

---

## 5. DI Module (`ArchModelModule.kt`)

### Master version
```kotlin
bindFactory<OpenAISettings, OpenAIClient> { settings -> OpenAIClientDefault(settings) }
bind<OpenAIApi>() with singleton { OpenAIApi(instance(), appLang = ..., factory()) }
bindWithActivityViewModelScope<FeedWidgetSettingsActivityViewModel>()
```

### AI-features version
```kotlin
bind<AIApi>() with singleton { AIApi(instance(), appLang = ...) }
bindWithComposableViewModelScope<TranslationSettingsViewModel>()
// No FeedWidgetSettingsActivityViewModel
```

### Differences
1. **AI binding**: Master binds `OpenAIApi` + factory for `OpenAIClient`; ai-features binds `AIApi` (simpler, no factory needed)
2. **Widget**: Master has `FeedWidgetSettingsActivityViewModel`; ai-features does not
3. **Translation**: AI-features has `TranslationSettingsViewModel`; master does not

### Resolution
- Keep ai-features' `AIApi` binding
- Port master's `FeedWidgetSettingsActivityViewModel` binding
- Keep ai-features' `TranslationSettingsViewModel` binding
- Remove `OpenAIApi`, `OpenAIClient`, `OpenAIClientDefault` factory binding

---

## 6. SettingsViewModel.kt

### Master additions since divergence
- Paging settings: `isPagingMode`, `isAnimatedPaging`, `setPagingMode()`, `setAnimatedPaging()`
- Blocklist for summaries: `blockListApplyToSummaries`, `setBlockListApplyToSummaries()`
- Read item opacity: `readItemOpacity`, `setReadItemOpacity()`
- Uses `OpenAIApi`, `OpenAISettings`, `OpenAISettingsEvent`, `OpenAIModelsState`

### AI-features changes
- Replaced `OpenAIApi` with `AIApi`
- Replaced `OpenAISettings` with `AISettings`
- Replaced `OpenAISettingsEvent` with `AISettingsEvent`
- Replaced `OpenAIModelsState` with `ModelsState`
- Added `autoFetchFullArticle`, `summaryLanguage`, `translationLanguage`
- Added `AISettingsState` (replaces `OpenAISettingsState`)

### Resolution
- Keep ai-features' AI settings refactoring
- Port master's paging settings (`isPagingMode`, `isAnimatedPaging`)
- Port master's blocklist for summaries
- Port master's read item opacity
- The `combine()` flow indexing will need careful adjustment (ai-features shifted all indices)

---

## 7. Repository.kt and SettingsStore.kt

### Master additions
- `isPagingMode`, `isAnimatedPaging`, `setPagingMode()`, `setAnimatedPaging()` (paging)
- `blockListApplyToSummaries`, `setBlockListApplyToSummaries()` (blocklist)
- `readItemOpacity`, `setReadItemOpacity()` (read opacity)

### AI-features additions
- `aiProviderType`, `setAIProviderType()` (provider selection)
- `openAISettings` (renamed from `openAiSettings`), `anthropicSettings`
- `providers`, `addProvider()`, `updateProvider()`, `deleteProvider()`, `activateProvider()`
- `summaryLanguage`, `translationLanguage`, `summaryEnabled`, `translationEnabled`
- `summaryTimeout`, `translationTimeout`
- `autoFetchFullArticle`
- `aiSettings` computed property, `aiSettingsFlow`

### Resolution
- Keep ALL ai-features additions
- Port master's paging settings
- Port master's blocklist for summaries
- Port master's read opacity setting
- The `SettingsStore` changes are **additive on different lines** so should merge cleanly after resolving the OpenAI settings rename

---

## 8. MainActivityViewModel.kt

### Master version
Added paging infrastructure:
```kotlin
enum class ScrollDirection { UP, DOWN }
val scrollCommand: SharedFlow<ScrollDirection>
fun emitScrollCommand(direction: ScrollDirection)
val isPagingMode: StateFlow<Boolean>
val isAnimatedPaging: StateFlow<Boolean>
```

### AI-features version
Unchanged from divergence point (no paging).

### Resolution
- Port master's entire paging addition -- it's purely additive

---

## 9. Widget System (master only -- new files)

Master added a complete widget system:
- `FeedWidget.kt` -- Glance AppWidget implementation
- `FeedWidgetReceiver.kt` -- BroadcastReceiver
- `FeedWidgetSettingsActivity.kt` -- configuration activity
- `FeedWidgetSettingsActivityViewModel.kt` -- ViewModel
- AndroidManifest.xml changes (receiver, activity declarations)
- Glance dependencies in build files

### Resolution
- Port ALL widget files as-is (they're new, no conflicts)
- Add Glance dependencies to libs.versions.toml
- Add DI binding for `FeedWidgetSettingsActivityViewModel`
- Port AndroidManifest.xml changes

---

## 10. ReaderView.kt -- HIGH risk

### Master version
Completely rewritten from `LazyColumn` to `Column + ScrollState`:
- `articleScrollState: ScrollState` parameter
- `articleBody: @Composable (indexOffset: Int) -> Unit` (was `LazyListScope.(indexOffset: Int) -> Unit`)
- Regular `Column` with `verticalScroll` modifier
- Removed `LazyColumnScrollbar` / `my.nanihadesuka.compose` dependency

### AI-features version
Still uses LazyColumn:
- `articleListState: LazyListState` parameter
- `articleBody: LazyListScope.(indexOffset: Int) -> Unit`
- `LazyColumnScrollbar` wrapper

### Resolution
- This is intertwined with ArticleScreen.kt (see section 3)
- If adopting ScrollState approach, ReaderView must be rewritten
- If keeping LazyColumn, master's crash fix is lost

---

## 11. LinearArticleContent.kt -- HIGH risk

### Master version
- `linearArticleContent()` -- LazyListScope extension (kept for backward compat)
- `ColumnArticleContent()` -- NEW composable for ScrollState-based rendering
- Both call `LinearElementContent()` (same core rendering)

### AI-features version
- `linearArticleContent()` -- heavily extended with translation support
- Added `translatedParagraphs` parameter
- `computeParagraphIndices()` -- maps element positions to translation indices
- `ParagraphCounter` -- recursive counter matching `TranslatableTextExtractor` logic
- Each `LinearElementContent()` call gets `translation` and `parentTranslationIndex`
- Table cell translation, image caption translation support

### Resolution
If adopting ScrollState approach:
- Create `ColumnArticleContent()` with translation support (merge of master's column approach + ai-features' translation logic)
- Keep `computeParagraphIndices()`, `ParagraphCounter`, and all translation parameter threading
- The `LinearElementContent()` function signature must include all ai-features' translation params

---

## 12. Other master changes to port

| File / Feature | Commit | Complexity |
|---------------|--------|-----------|
| Nav drawer divider | `dfdf7703` | LOW -- additive UI change |
| Widget content fix | `b9fe83a6` | LOW -- part of widget system |
| Compact layout scroll fix | `04e7c2dc` | LOW -- isolated fix |
| Article delete during sync | `addfd2b2` | LOW -- isolated fix |
| Failing tests fix | `cbaf177b` | LOW -- test fix |
| Open-in options | `fea8df02` | LOW -- context menu |
| YouTube channel suggest | `4d3f8379` | LOW -- feed suggestion |
| Read item opacity | `e24d2df1` | MEDIUM -- Settings + UI |
| Blocklist for summaries | `3cced03f` | MEDIUM -- Settings + filter logic |
| Weblate translations | ~20 commits | LOW -- string resources only |

---

## Integration Risks (post-merge)

### 1. LazyColumn -> Column migration for translation
The biggest risk is converting the translation paragraph rendering from `LazyListScope` to `@Composable Column`. The `computeParagraphIndices()` and `ParagraphCounter` logic must be preserved exactly, or translation indices will mismatch.

### 2. AGP 9.0.0 compatibility
AGP 9.0.0 may introduce build-breaking changes. The ai-features code has been tested with AGP 8.11.1. Key risks:
- KSP version must be upgraded to 2.3.6
- Room annotation processing may change
- Kotlin compiler plugin compatibility
- R8/ProGuard rule changes

### 3. OkHttp 5.3.2 upgrade
The AI SDKs (openai-java, anthropic-java) use OkHttp internally. Upgrading from 5.1.0 to 5.3.2 should be backward compatible, but the BOM alignment needs verification.

### 4. Mikepenz markdown library compatibility
`mikepenz-markdown 0.38.1` was specifically downgraded from 0.39.0 for Compose 1.9.4 compatibility. With master's updated Compose BOM, need to verify this is still correct.

### 5. Old OpenAI client removal
The old `com.aallam.openai:openai-client` and its `ktor-client-okhttp` dependency must be completely removed. Any lingering references will cause compilation errors. Files referencing the old package:
- `openai/OpenAIApi.kt` -- DELETE
- `openai/OpenAIClient.kt` -- DELETE
- `openai/OpenAIClientDefault.kt` -- DELETE (if exists)
- All imports of `com.nononsenseapps.feeder.openai.*` in other files

### 6. Widget + AI coexistence in DI
Both `FeedWidgetSettingsActivityViewModel` and `TranslationSettingsViewModel` need to be registered in `ArchModelModule`. No technical conflict, but the merged module needs both bindings.

### 7. Text selection context menu
AI-features added `WithFeederTextToolbar` / `TextSelectionMenuHandler` for read-aloud and translate from text selection. Master's switch to `Column` (away from `LazyColumn`) should not affect this, but needs testing.
