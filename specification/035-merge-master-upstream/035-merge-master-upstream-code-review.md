# Spec 035: Merge master into ai-features -- Code Review

**Reviewer**: code-reviewer agent
**Date**: 2026-03-19
**Verdict**: **Approved**

---

## 1. Correctness -- Conflict Resolution

### 1.1 ArticleScreen.kt (HIGH) -- PASS

- **ScrollState migration**: `LazyListState`/`rememberLazyListState` fully replaced with `ScrollState`/`rememberScrollState`. No remnants of `LazyListState` in this file.
- **Paging overlay**: Invisible left/right tap zones (20%/60%/20% weight split) correctly wired with `coroutineScope.launch` and `animateScrollTo`/`scrollTo` based on `isAnimatedPaging`.
- **Paging LaunchedEffect**: Volume button scroll commands collected from `mavm.scrollCommand` with 90% viewport scroll amount. Both animated and non-animated paths handled.
- **AI features preserved**: `CircleProgressIconButton` for both summarize and translate toolbar icons present. `MarkdownContentSafe` renders summaries (not master's `AnnotatedString`). `TranslationStatusSection`, `TranslationErrorSection` composables intact. `WithFeederTextToolbar` wrapper and `TextSelectionMenuHandler` present.
- **Cancel support**: `onCancelSummarize` and `onCancelTranslation` callbacks properly threaded through all layers.
- **`mainActivityViewModel` parameter**: Present on outer `ArticleScreen` composable (line 98).
- **`ColumnArticleContent`**: Correctly called with `translatedParagraphs` parameter (line 704-727). `elementPositions` map for anchor scrolling is properly maintained.
- **No duplicate code**: No leftover master summary rendering (AnnotatedString approach) found.

### 1.2 ArticleViewModel.kt (HIGH) -- PASS

- **ai-features version fully preserved**: `AIApi`, `AISummaryState`, `TranslationState`, `ParagraphTranslationCoordinator`, `SummaryResponseParser.sanitizeErrorMessage()`, `TranslatableTextExtractor`, cancel support (`summarizeJob`/`translateJob`), `CancellationException` handling -- all intact.
- **No master code leaked**: No `AnnotatedString`, `convertSummaryToAnnotatedStrings()`, `MarkdownToHtmlConverter`, or explicit `Either` type annotations.
- **Auto-fetch, auto-summarize, auto-translate** logic preserved.

### 1.3 SettingsViewModel.kt (MEDIUM) -- PASS

- **35 params (indices 0-34)**: Verified the `combine()` call includes all 35 flows in the exact order specified:
  - `[0]`-`[17]`: Original ai-features flows through `blockList`
  - `[18]`: `repository.applyBlocklistToSummaries` (master NEW)
  - `[19]`-`[27]`: Remaining original flows
  - `[28]`-`[30]`: AI settings flows (ai-features)
  - `[31]`-`[32]`: `isOpenDrawerOnFab`, `font`
  - `[33]`-`[34]`: `isPagingMode`, `isAnimatedPaging` (master NEW)
- **Casts match flow order**: All `params[N] as Type` casts verified correct at lines 280-317.
- **Setter methods present**: `setApplyBlocklistToSummaries`, `setIsPagingMode`, `setIsAnimatedPaging` all delegate to repository correctly.
- **`SettingsViewState` data class**: All 3 new fields present with correct defaults: `applyBlocklistToSummaries = false`, `isPagingMode = false`, `isAnimatedPaging = false`.
- **AI settings architecture** (`AIApi`, `AISettingsEvent`, `ModelsState`) fully preserved from ai-features.

### 1.4 LinearArticleContent.kt (MEDIUM) -- PASS

- **`ColumnArticleContent`**: Takes `translatedParagraphs: List<String>?` and `onElementPosition` parameters. Uses `computeParagraphIndices()` with `ParagraphCounter` for index mapping.
- **Translation threading**: Each `LinearElementContent()` receives `translation`, `translatedParagraphs`, and `parentTranslationIndex` parameters.
- **Nested structure support**: `LinearListItemContent`, `LinearBlockQuoteContent`, `LinearImageContent` all receive appropriate translation parameters.
- **Table cell and image caption** translation paths preserved via `captionTranslation` parameter.

### 1.5 NavigationDestinations.kt (MEDIUM) -- PASS

- **Abstract `register()` method**: Correctly includes `mainActivityViewModel: MainActivityViewModel` parameter (line 101-106).
- **Abstract `RegisterScreen()`**: Correctly includes `mainActivityViewModel` parameter (line 126-131).
- **All 13 destinations updated**: `SearchFeedDestination`, `TextSettingsDestination`, `AddFeedDestination`, `EditFeedDestination`, `SettingsDestination`, `ProviderListDestination`, `ProviderEditDestination`, `SummarySettingsDestination`, `TranslationSettingsDestination`, `SelectionMenuSettingsDestination`, `FeedDestination`, `ArticleDestination`, `SyncScreenDestination` -- all have `mainActivityViewModel` in their `RegisterScreen()` override.
- **`ArticleDestination.RegisterScreen()`**: Forwards `mainActivityViewModel` to `ArticleScreen()` (line 712).

### 1.6 MainActivity.kt (MEDIUM) -- PASS

- **Notification permission handling removed**: No `requestNotificationsPermission`, no `maybeRequestNotificationPermission()`, no `KEY_NOTIFICATION_PERMISSION_REQUESTED`. Clean.
- **`Repository` instance added**: `private val repository: Repository by instance()` at line 42.
- **`onKeyDown()` method**: Volume key paging via `mainActivityViewModel.emitScrollCommand(ScrollDirection.UP/DOWN)` guarded by `isPagingMode.value && repository.isArticleOpen.value`.
- **LeakCanary removed**: No `updateLeakCanaryNotificationState` import or calls.
- **All 13 destinations registered** with `mainActivityViewModel` parameter (lines 116-136).
- **AI destinations present**: `ProviderListDestination`, `ProviderEditDestination`, `SummarySettingsDestination`, `TranslationSettingsDestination`, `SelectionMenuSettingsDestination` all registered.

### 1.7 ArchModelModule.kt (LOW) -- PASS

- **AIApi binding**: `bind<AIApi>() with singleton { ... }` (line 48). No old `OpenAIApi` or `OpenAIClient` bindings.
- **Widget binding**: `bindWithActivityViewModelScope<FeedWidgetSettingsActivityViewModel>()` (line 61). Import present (line 29).
- **No old OpenAI imports**: No `OpenAISettings`, `OpenAIClient`, `bindFactory` etc.

### 1.8 CHANGELOG.md (LOW) -- PASS

- **Order**: `[Unreleased]` -> `[2.18.0]` -> `[2.17.0]` -> `[2.16.1]` -> ... Correct.
- **Both branches' content preserved**: ai-features' Unreleased section with async translation features, master's 2.18.0 section with widget/blocklist/paging features.

### 1.9 ReaderView.kt -- PASS

- **ScrollState migration**: Uses `ScrollState` parameter (line 97: `articleScrollState: ScrollState = rememberScrollState()`), `Column` with `.verticalScroll(articleScrollState)` (line 114). No `LazyColumn` or `LazyListScope`.
- **`articleBody` lambda**: Takes `indexOffset: Int` (line 98), consistent with `ArticleContent` usage.

### 1.10 build.gradle.kts (LOW) -- PASS

- **Master build infra**: `versionCode = 3922`, `versionName = "2.18.0"`, `assets.directories.add()` (AGP 9.0.0 style), no `renderScript`.
- **ai-features deps**: `openai.java`, `anthropic.java`, `anthropic.java.okhttp`, `mikepenz.markdown`, `mikepenz.markdown.m3`, `mikepenz.markdown.coil3`, `reorderable`.
- **Master deps**: `glance.appwidget`, `glance.material3`, `glance.preview`, `glance.appwidget.preview`.
- **Removed**: No `leakcanary`, no `openai-client-bom`, no `ktor-client-okhttp`.
- **Kotlin compiler args**: `-Xannotation-default-target=param-property` and `-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi` present.

---

## 2. Completeness -- Upstream Features

| Feature | Status | Evidence |
|---------|--------|----------|
| Paging (volume buttons) | PASS | `MainActivity.onKeyDown()`, `MainActivityViewModel.scrollCommand/emitScrollCommand` |
| Paging (tap zones) | PASS | `ArticleScreen` invisible Row overlay with 20/60/20 weights |
| Animated paging toggle | PASS | `isAnimatedPaging` flows through all layers |
| Widget | PASS | `widget/` directory with 4 files, DI binding in `ArchModelModule` |
| Blocklist-on-summaries | PASS | `repository.applyBlocklistToSummaries`, `SettingsViewModel.setApplyBlocklistToSummaries()` |
| AGP 9.0.0 | PASS | `assets.directories.add()` style, no `renderScript` |
| LazyColumn-to-Column migration | PASS | `ColumnArticleContent`, `ScrollState`, no `LazyListScope` in reader path |
| i18n updates (20+ languages) | PASS | Multiple `values-*/strings.xml` files in staged changes |
| `mainActivityViewModel` on all nav destinations | PASS | All 13 destinations verified |
| Notification permission handling removed | PASS | Clean removal from `MainActivity` |

---

## 3. AI Feature Preservation

| Component | Status | Location |
|-----------|--------|----------|
| `AIApi` | PASS | `ai/AIApi.kt` |
| `AIClient` + providers | PASS | `ai/AIClient.kt`, `ai/provider/OpenAICompatibleClient.kt`, `ai/provider/AnthropicClient.kt` |
| `SummaryResponseParser` | PASS | `ai/SummaryResponseParser.kt`, used in `ArticleScreen.SummarySection()` |
| `TranslationPromptBuilder` | PASS | `ai/TranslationPromptBuilder.kt` |
| `InlineTagParser` | PASS | `ai/InlineTagParser.kt` |
| `ParagraphTranslationCoordinator` | PASS | `ai/ParagraphTranslationCoordinator.kt` |
| `TranslatableTextExtractor` | PASS | `ai/TranslatableTextExtractor.kt` |
| `MarkdownContentSafe` | PASS | Used in `ArticleScreen.MarkdownText()` |
| `CircleProgressIconButton` | PASS | `feedarticle/CircleProgressIconButton.kt`, used for summarize + translate toolbar icons |
| `WithFeederTextToolbar` | PASS | `utils/FeederTextToolbar.kt`, wraps `ArticleScreenInternal` |
| Translation UI (`TranslationStatusSection`) | PASS | In `ArticleScreen.kt` |
| Cancel support | PASS | `cancelSummarize()`/`cancelTranslation()` in ViewModel, wired to UI |

---

## 4. Integration Coherence

- **ScrollState consistency**: `ArticleScreen` -> `ReaderView` -> `ColumnArticleContent` all use `ScrollState`. No `LazyListState` in the reader path.
- **Translation parameters threaded correctly**: `ArticleContent` -> `ColumnArticleContent` -> `LinearElementContent` -> nested elements. `computeParagraphIndices()` correctly maps element indices to translation indices.
- **Paging overlay does not conflict with translation UI**: Paging tap zones are in a `Row` overlay on top of `ArticleContent`. Translation sections render inside the scrollable `Column`. No z-order conflict.
- **DI module complete**: `AIApi`, `FeedWidgetSettingsActivityViewModel`, all ViewModels bound.

---

## 5. Dead Code Check

| Check | Status |
|-------|--------|
| `openai/` package directory | PASS -- does not exist |
| LeakCanary files (debug + release) | PASS -- both deleted |
| LeakCanary references in source | PASS -- none found |
| Old `openai-client-bom` dependency | PASS -- not in `build.gradle.kts` |
| Old `com.nononsenseapps.feeder.openai` imports | PASS -- none found |
| Unresolved conflict markers | PASS -- none found |

---

## 6. Build and Test

- **Build**: `compileFdroidDebugKotlin` -- BUILD SUCCESSFUL (confirmed by task #4)
- **Tests**: 484 pass, 3 known pre-existing failures only (confirmed by task #7)

---

## 7. Summary

The merge resolution is thorough and correct across all 10 conflicting files. All upstream features (paging, widgets, blocklist-on-summaries, AGP 9.0.0, i18n, LazyColumn-to-Column migration) are properly incorporated. All AI features (multi-provider summary/translation, circular progress, text selection menu, Anthropic support, cancel support) are fully preserved. The `SettingsViewModel` params array is correctly indexed with all 35 parameters (0-34). No dead code, no conflict markers, no lost functionality.

**Verdict: Approved**
