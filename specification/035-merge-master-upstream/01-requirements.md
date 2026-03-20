# Spec 035: Merge master into ai-features -- Requirements

## Merge Goal

Merge the `master` branch (at commit `dfdf7703`) into `ai-features` (at commit `54666db7`) to incorporate all upstream features and fixes while preserving the full AI feature set developed on `ai-features`.

- **Merge base**: `748fd571` (v2.16.1 release)
- **Master additions**: ~50 commits (2.17.0, 2.18.0 releases)
- **ai-features additions**: ~50 commits (specs 001-034)

---

## Upstream Features to Incorporate

| # | Feature | Key Commits | Integration Risk |
|---|---------|-------------|-----------------|
| 1 | Markdown parsing for AI summaries | `c973eab5` | HIGH -- overlaps with our AI summary work |
| 2 | Paging with volume buttons/tap | `70ed0f25` | MEDIUM -- touches ArticleScreen, ReaderView, MainActivity |
| 3 | Widget support | `8da9aac3` | LOW -- new files, minimal overlap |
| 4 | Blocklist apply-to-summaries | `3cced03f` | LOW -- new setting, additive |
| 5 | AGP 9.0.0 + OkHttp 5.3.2 upgrade | `869c66a5`, `ab309c90` | LOW -- build config only |
| 6 | LazyColumn -> Column migration in ReaderView | `70ed0f25` | HIGH -- fundamental UI architecture change |
| 7 | Bug fixes (scroll crash, sync deletion, etc.) | multiple | LOW -- mostly in non-conflicting files |
| 8 | i18n updates (20+ languages) | multiple | LOW -- additive string resources |
| 9 | LeakCanary removal | `869c66a5` | LOW -- ai-features still has it, master removed it |
| 10 | Navigation register() signature change (mainActivityViewModel param) | `70ed0f25` | MEDIUM -- affects all destination registrations |

---

## Conflict Resolution Strategy (10 Conflicting Files)

### 1. `CHANGELOG.md` -- LOW risk

- **Master**: Added 2.17.0 and 2.18.0 release sections, contributor attribution fixes
- **ai-features**: Added `[Unreleased]` section with AI features changelog

**Resolution**: Keep BOTH. Place ai-features `[Unreleased]` section at the top, followed by master's `2.18.0` and `2.17.0` sections. Accept master's contributor attribution fixes in older entries.

---

### 2. `app/build.gradle.kts` -- LOW risk

- **Master**: AGP 9.0.0, versionCode 3922, versionName 2.18.0, removed `kotlin.android` plugin alias, `renderScript = false` removed, `assets.srcDir` -> `assets.directories.add`, added Glance widget deps, removed LeakCanary
- **ai-features**: Added Kotlin compiler args (`-Xannotation-default-target`, `-Xopt-in`), replaced `openai-client-bom` with `openai-java`/`anthropic-java`, added Mikepenz markdown libs, added reorderable lib, kept LeakCanary

**Resolution**: Take master's build infrastructure changes (AGP 9.0.0, version bump, plugin cleanup, asset directory API, Glance deps, LeakCanary removal). Keep ai-features' Kotlin compiler args, AI SDK deps (openai-java, anthropic-java), Mikepenz markdown deps, and reorderable dep.

Note: Master removed LeakCanary; ai-features still has it. **Use master's approach** (remove LeakCanary) since master also deleted `LeakCanaryCompat.kt` files.

---

### 3. `app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt` -- LOW risk

- **Master**: Added `FeedWidgetSettingsActivityViewModel` binding
- **ai-features**: Replaced `OpenAIClient` factory + `OpenAIApi` singleton with `AIApi` singleton, removed OpenAI imports, added `TranslationSettingsViewModel` binding

**Resolution**: Keep ai-features' AI DI changes (AIApi instead of OpenAIApi). Add master's `FeedWidgetSettingsActivityViewModel` binding. The changes are in different parts of the file and don't overlap functionally.

---

### 4. `app/src/main/java/com/nononsenseapps/feeder/openai/OpenAIApi.kt` -- HIGH risk (MODIFY/DELETE)

- **Master**: Modified -- added markdown formatting instructions to summary prompt (bold, italics, bullet points, headers, block quotes), stronger Lang line instruction
- **ai-features**: DELETED this file entirely. Functionality replaced by:
  - `com.nononsenseapps.feeder.ai.AIApi` (high-level API)
  - `com.nononsenseapps.feeder.ai.provider.OpenAICompatibleClient` (OpenAI provider)
  - `com.nononsenseapps.feeder.ai.provider.AnthropicClient` (Anthropic provider)
  - `com.nononsenseapps.feeder.ai.SummaryResponseParser` (robust response parsing)
  - `com.nononsenseapps.feeder.ai.TranslationPromptBuilder` (shared prompt building)

**Resolution**: Keep ai-features' deletion. The master's markdown prompt improvements need to be verified as already incorporated (or need to be ported) into ai-features' `TranslationPromptBuilder` and `OpenAICompatibleClient`/`AnthropicClient`.

**ACTION ITEM**: After merge, verify that ai-features' summary prompt includes markdown formatting instructions similar to master's changes. If not, port the markdown prompt improvements to `TranslationPromptBuilder` or the AI client implementations.

---

### 5. `app/src/main/java/com/nononsenseapps/feeder/ui/MainActivity.kt` -- MEDIUM risk

- **Master**:
  - Removed notification permission handling (`maybeRequestNotificationPermission`, `requestNotificationsPermission`)
  - Added `Repository` instance for `isArticleOpen` check
  - Added `onKeyDown()` for volume button paging
  - Changed all `register()` calls to pass `mainActivityViewModel` parameter
  - Removed LeakCanary-related import
- **ai-features**:
  - Added AI navigation destinations (ProviderList, ProviderEdit, SummarySettings, TranslationSettings, SelectionMenuSettings)

**Resolution**: Take master's structural changes (permission removal, `onKeyDown()`, `Repository` instance). Merge both sets of navigation registrations: master's `mainActivityViewModel` parameter for all existing destinations, PLUS ai-features' new AI destination registrations. The new AI destinations also need the `mainActivityViewModel` parameter added to their `register()` calls for consistency.

---

### 6. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` -- MEDIUM risk

- **Master**:
  - `LazyListState` -> `ScrollState` migration throughout
  - `linearArticleContent()` (LazyListScope extension) -> `ColumnArticleContent()` (Composable)
  - Added `MainActivityViewModel` parameter for paging
  - Added paging overlay (left/right click zones)
  - `LazyListScope.LoadingItem()` removed
  - Summary rendering: `Text(summary.value.content)` -> `Column { summary.annotatedStrings.forEach { Text(annotatedString) } }`
  - Added `elementPositions` map for anchor link scrolling
  - Anchor scrolling: `animateScrollToItem()` -> `animateScrollTo(yPosition)`
- **ai-features**:
  - `OpenAISummaryState` -> `AISummaryState` rename throughout
  - Added `TranslationState` rendering (TranslationStatusSection)
  - Added translate button and cancel buttons to toolbar (CircleProgressIconButton)
  - Replaced LinearProgressIndicator with circular progress
  - Added `WithFeederTextToolbar` wrapper for text selection menu
  - Added `TextSelectionMenuHandler`
  - Summary rendering: `MarkdownText` composable (Mikepenz) with `SummaryResponseParser.containsRawJson()` guard
  - Added `translatedParagraphs` extraction and passing to `linearArticleContent()`

**Resolution**: This is the most complex merge. Strategy:
1. **Use master's ScrollState migration** -- keep `ScrollState` instead of `LazyListState`
2. **Use ai-features' AI state naming** -- `AISummaryState`, `TranslationState`
3. **Use ai-features' toolbar buttons** -- CircleProgressIconButton for summarize/translate
4. **Use ai-features' summary rendering** -- `MarkdownText` with `containsRawJson()` guard (supersedes master's `AnnotatedString` approach)
5. **Use ai-features' translation UI** -- TranslationStatusSection, translatedParagraphs passing
6. **Integrate master's paging** -- add paging overlay, `MainActivityViewModel` parameter, volume button scroll
7. **Adapt anchor scrolling** -- use master's `ScrollState.animateScrollTo()` approach with ai-features' element tracking

Key adaptation needed: ai-features still uses `LazyListScope` in `linearArticleContent()`. Master converted this to `ColumnArticleContent()`. We need to use master's `Column`-based approach but with ai-features' translation parameters.

---

### 7. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` -- HIGH risk

- **Master**:
  - Added `AnnotatedString` import and type annotations on `Either.catching`
  - Added `convertSummaryToAnnotatedStrings()` method using `MarkdownToHtmlConverter` -> `htmlStringToAnnotatedString()`
  - Added `annotatedStrings` field to `OpenAISummaryState.Result`
  - Summary results now include pre-rendered markdown -> annotated strings
- **ai-features**:
  - Complete AI architecture overhaul: `OpenAIApi` -> `AIApi`, `OpenAISettings` -> `AISettings`
  - `OpenAISummaryState` -> `AISummaryState`, `openAiSummary` -> `aiSummary`
  - Added `TranslationState` with full per-paragraph translation workflow
  - Added `summarizeJob`/`translateJob` with cancellation support
  - Added `cancelSummarize()`/`cancelTranslation()` methods
  - `CancellationException` handling before general Exception catch
  - `SummaryResponseParser.sanitizeErrorMessage()` for error messages
  - Auto-fetch full text feature
  - `translate()` method with `ParagraphTranslationCoordinator`

**Resolution**: Keep ai-features' version as the base. Key decisions:
1. **Do NOT incorporate master's `AnnotatedString` approach** -- ai-features uses `MarkdownText` composable (Mikepenz) which renders markdown directly, making `AnnotatedString` conversion unnecessary
2. **Do NOT incorporate master's `convertSummaryToAnnotatedStrings()`** -- superseded by Mikepenz Markdown renderer
3. **Keep ai-features' full AI architecture** (AIApi, AISummaryState, TranslationState, cancellation, etc.)
4. **Port master's `Either.catching` type annotations** if they fix build issues (explicit `<TSSError, List<AnnotatedString>>`)

---

### 8. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt` -- MEDIUM risk

- **Master**: Added `applyBlocklistToSummaries`, `isPagingMode`, `isAnimatedPaging` settings and their setters, re-indexed all `params[]` array accesses
- **ai-features**: Replaced `OpenAISettings`/`OpenAIApi` with `AISettings`/`AIApi`, replaced `OpenAISettingsEvent`/`OpenAISettingsState`/`OpenAIModelsState` with `AISettingsEvent`/`AISettingsState`/`ModelsState`, added `autoFetchFullArticle`, `summaryLanguage`, re-indexed params

**Resolution**: Keep ai-features' AI settings architecture. Add master's `applyBlocklistToSummaries`, `isPagingMode`, `isAnimatedPaging` settings and setters. Re-index the `params[]` array carefully to include ALL settings from both branches. This requires careful manual counting.

---

### 9. `app/src/main/res/values/strings.xml` -- LOW risk

- **Master**: Added 13 strings (blocklist, open-in options, paging mode, widget)
- **ai-features**: Added ~120 strings (AI provider, translation, summary, provider management)

**Resolution**: Keep ALL strings from both branches. The strings are in different sections and don't overlap. Place master's new strings in their respective locations, keep ai-features' strings.

---

### 10. `app/src/main/res/values-zh-rCN/strings.xml` -- LOW risk

- **Master**: Added 9 Chinese translations (open-in, blocklist, widget)
- **ai-features**: Added 108 Chinese translations (AI features)

**Resolution**: Keep ALL translations from both branches. No content overlap.

---

## Non-Conflicting but Attention-Required Files

### ReaderView.kt (master changed, ai-features did not change)
Master converted `LazyColumn` to `Column` with `verticalScroll(ScrollState)`. This is a major architectural change that will auto-merge but must be verified to work with ai-features' translation rendering in `LinearArticleContent.kt`.

### LinearArticleContent.kt (both changed, but may auto-merge)
- Master added `ColumnArticleContent()` as a new function (additive)
- ai-features extensively modified `linearArticleContent()` (LazyListScope extension) to add translation params

Both changes coexist, but the new `ColumnArticleContent()` from master doesn't include ai-features' translation parameters. After merge, `ColumnArticleContent()` will need to be updated to pass translation parameters, OR we continue using the LazyListScope version adapted for Column.

### NavigationDestinations.kt
Master added `mainActivityViewModel` parameter to `register()` methods of ALL existing destinations. ai-features added new AI navigation destinations. The new AI destinations need `mainActivityViewModel` added to their `register()` signatures too.

### MainActivityViewModel.kt
Master added paging support (`ScrollDirection`, `scrollCommand`, `isPagingMode`, `isAnimatedPaging`). This auto-merges but needs verification.

### MarkdownToHtmlConverter.kt (master only, new file)
Master uses this for markdown -> HTML -> AnnotatedString conversion. ai-features uses Mikepenz Markdown renderer instead. The master file can be kept for compatibility but is effectively unused by our approach.

### libs.versions.toml (both changed, may partially conflict)
- Master: AGP 9.0.0, ksp upgrade, OkHttp 5.3.2, Glance deps
- ai-features: openai-java, anthropic-java SDKs, Mikepenz markdown

Both changes are in different sections and should auto-merge or be trivially resolvable.

---

## Feature Integration Checklist

- [ ] **Paging mode**: Verify volume button and tap paging works with ai-features' translation overlay
- [ ] **Widget**: Verify widget DI bindings work alongside AI DI bindings
- [ ] **Blocklist on summaries**: Verify `applyBlocklistToSummaries` setting integrates with AI summary flow
- [ ] **Markdown summaries**: Verify ai-features' Mikepenz renderer produces equivalent or better output than master's AnnotatedString approach
- [ ] **ScrollState migration**: Verify anchor link scrolling works with Column + ScrollState in presence of translation overlays
- [ ] **LeakCanary removal**: Ensure no references to LeakCanaryCompat remain in ai-features code
- [ ] **NavigationDestinations**: All AI destinations must have `mainActivityViewModel` parameter
- [ ] **AGP 9.0.0 build**: Verify the project builds with the new Android Gradle Plugin

---

## Acceptance Criteria

1. **Build**: `./gradlew :app:compileFdroidDebugKotlin` succeeds
2. **Tests**: `./gradlew :app:testFdroidDebugUnitTest` -- all existing tests pass (known pre-existing failures: `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` x2)
3. **AI features preserved**: Summary, translation, multi-provider, circular progress, text selection menu all functional
4. **Upstream features incorporated**: Paging mode, widget, blocklist on summaries, markdown summaries, bug fixes, i18n updates
5. **No regressions**: No deleted files that should be kept, no broken imports, no missing DI bindings

---

## Risk Areas (Highest to Lowest)

1. **ArticleScreen.kt + ReaderView.kt LazyColumn->Column migration**: This is the single biggest risk. Master fundamentally changed the scrolling architecture from `LazyColumn` (lazy item rendering) to `Column` (eager rendering with `ScrollState`). ai-features' `linearArticleContent()` is a `LazyListScope` extension function. After merge, we either:
   - (a) Adapt `linearArticleContent()` to work as a `@Composable` instead of `LazyListScope` extension, or
   - (b) Update master's `ColumnArticleContent()` to include all translation parameters

   **Recommendation**: Option (b) -- extend `ColumnArticleContent()` with translation params, since master already created the Column-based version.

2. **SettingsViewModel.kt params[] re-indexing**: Both branches re-indexed the `combine()` params array. A miscount will cause `ClassCastException` at runtime. Must be manually verified.

3. **OpenAIApi.kt deletion**: Master modified this file; ai-features deleted it. The markdown prompt improvements from master need to be verified as present in ai-features' replacement code.

4. **NavigationDestinations.kt signature changes**: Master added `mainActivityViewModel` to ALL `register()` methods. ai-features added new destinations without this param. All new destinations need updating.

5. **Dependency version conflicts**: Both branches modified `libs.versions.toml` and `build.gradle.kts` with different dependency changes. Need to ensure no version incompatibilities (especially KSP version vs Kotlin version).

---

## Merge Execution Order

1. Start merge: `git merge master` on `ai-features` branch
2. Resolve conflicts in order: LOW risk first, then MEDIUM, then HIGH
3. After conflict resolution, compile to verify
4. Run tests
5. Manual smoke test of key features
