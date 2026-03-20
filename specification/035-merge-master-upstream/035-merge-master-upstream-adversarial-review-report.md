# Spec 035: Merge master into ai-features -- Adversarial Review Report

**Reviewer**: adversarial-reviewer agent
**Date**: 2026-03-19
**Verdict**: **PASS**

---

## 1. Skeptic Lens -- Conflict Resolution Verification

### 1.1 SettingsViewModel params[] re-indexing -- VERIFIED CORRECT

Manually traced all 35 flows (indices 0-34) in `SettingsViewModel.kt`:

| Index | Flow | Cast Type | Correct? |
|-------|------|-----------|----------|
| 0-7 | `currentTheme` through `autoFetchFullArticle` | Mixed | YES |
| 8-17 | `loadImageOnlyOnWifi` through `blockList` | Mixed | YES |
| 18 | `applyBlocklistToSummaries` (master NEW) | Boolean | YES |
| 19-27 | `useDetectLanguage` through `showTitleUnreadCount` | Mixed | YES |
| 28 | `aiSettingsFlow` | AISettings | YES |
| 29 | `summaryLanguage` | SummaryLanguage | YES |
| 30 | `openAIModelsState` | ModelsState | YES |
| 31 | `isOpenDrawerOnFab` | Boolean | YES |
| 32 | `font` | FontSelection | YES |
| 33 | `isPagingMode` (master NEW) | Boolean | YES |
| 34 | `isAnimatedPaging` (master NEW) | Boolean | YES |

Flow order in `combine()` (lines 241-277) matches cast order in `SettingsViewState` constructor (lines 279-318). All 35 entries verified.

### 1.2 OpenAIApi.kt deletion -- SAFE

- Directory `app/src/main/java/com/nononsenseapps/feeder/openai/` does NOT exist.
- Grep for `com.nononsenseapps.feeder.openai` across `app/src/` returns ZERO matches.
- All functionality replaced by `ai/` package (`AIApi`, `AIClient`, `OpenAICompatibleClient`, `AnthropicClient`).
- No runtime crash risk from this deletion.

### 1.3 Paging + Translation UI -- NO CONFLICT

- Paging tap zones: invisible `Row` overlay with `Box` elements (20%/60%/20% weights) using `clickable` with no `indication`. These sit on TOP of the article content in z-order.
- Translation paragraphs: rendered inline within the scrollable `Column` inside `ColumnArticleContent`. These are text elements, not clickable surfaces.
- The 60% center zone is a `Spacer` -- no touch interception. Only the 20% left/right edges intercept taps.
- Conclusion: No interaction conflict. The paging tap zones do not cover article text links (those are in the center 60%).
- **Minor concern**: If a user long-presses on the 20% edge zone for text selection, the paging `clickable` might consume the gesture. However, this is master's pre-existing behavior and not a merge regression.

---

## 2. Architect Lens -- Structural Consistency

### 2.1 ScrollState migration -- CONSISTENT

| File | Uses ScrollState? | No LazyListState? |
|------|--------------------|-------------------|
| `ArticleScreen.kt` | `rememberScrollState()` (line 110) | YES |
| `ReaderView.kt` | `ScrollState` param (line 97), `.verticalScroll(articleScrollState)` (line 114) | YES |
| `ColumnArticleContent` | No scroll state (nested within ReaderView) | N/A |

No `LazyListState`/`rememberLazyListState` references remain in the reader path. The `LazyListState` import in `NavigationDestinations.kt` is for `navDrawerListState`, which is correct (the nav drawer uses a LazyColumn).

### 2.2 Translation parameters through Column rendering -- CORRECT

Parameter threading verified:

```
ArticleContent (ArticleScreen.kt:704)
  -> ColumnArticleContent(translatedParagraphs = translatedParagraphs, ...)
    -> computeParagraphIndices() maps element index to paragraph index
    -> LinearElementContent(translation = ..., translatedParagraphs = ..., parentTranslationIndex = ...)
      -> LinearListItemContent(translatedParagraphs, parentTranslationIndex)
      -> LinearBlockQuoteContent(translatedParagraphs, parentTranslationIndex)
      -> LinearImageContent(captionTranslation = ...)
      -> Table cell translation via inline parameters
```

### 2.3 DI module completeness -- COMPLETE

All required bindings present in `ArchModelModule.kt`:
- `AIApi` singleton (line 48)
- `FeedWidgetSettingsActivityViewModel` activity-scoped (line 61)
- All 13 view models bound
- No missing bindings detected

### 2.4 Navigation signatures -- CONSISTENT

All 13 `NavigationDestination` subclasses have matching `RegisterScreen()` signatures with `mainActivityViewModel: MainActivityViewModel`. `ArticleDestination` correctly forwards it to `ArticleScreen()` (line 712). `FeedDestination` correctly does NOT forward it (FeedScreen doesn't need it).

---

## 3. Attack Vectors

### V1 (False Assumptions): AnnotatedString approach superseded -- CONFIRMED

- No code path uses master's `AnnotatedString` list for summary rendering.
- `ArticleScreen.SummarySection()` uses `MarkdownContentSafe` (ai-features' approach).
- `ArticleViewModel` has no `convertSummaryToAnnotatedStrings()` method.
- The `MarkdownToHtmlConverter.kt` file exists (from master) but is only referenced by its own test file. **Not a risk**, just dead production code.

### V2 (Edge Cases): Widget + AI cross-feature -- NO INTERACTION

- Widget files (`FeedWidget.kt`, `FeedWidgetReceiver.kt`, `FeedWidgetSettingsActivity.kt`, `FeedWidgetSettingsActivityViewModel.kt`) contain zero references to `AIApi`, `summarize`, `translate`, `openai`, or `anthropic`.
- Widget displays feed items only; no AI functionality exposed through widget.

### V3 (Failure Modes): AGP 9.0.0 / OkHttp 5.3.2 compatibility -- LOW RISK

- AGP 9.0.0 (`androidPlugin = "9.0.0"`) + KSP 2.3.6 + Kotlin 2.2.20: Build succeeds. KSP version numbering does not strictly require KSP major.minor to match Kotlin major.minor in practice. Build verified.
- OkHttp 5.3.2 (`okhttp = "5.3.2"`): AI clients use `openai-java` (4.13.0) and `anthropic-java` (2.11.1) SDKs which bring their own HTTP clients. OkHttp is used via Coil/Retrofit. No direct OkHttp API calls in AI client code.

### V4 (Adversarial Inputs): Paging zones vs translation taps -- NO CONFLICT

(Detailed analysis in Section 1.3 above)

### V5 (Safety): Destructive data operations -- NONE

- No database migrations in the diff.
- No `DROP TABLE`, `DELETE FROM`, `clearAllTables`, or `destructiveMigration` in new/modified code.
- No irreversible state changes.
- No permission escalation.
- No new secret exposure (existing test API keys are test-only, existing sync password is pre-existing infrastructure).

### V6 (Grounding): All decisions based on actual code -- VERIFIED

Every finding in this review was verified by reading actual source files and grepping the codebase. No assumptions made from the specification alone.

### V7 (Dependencies): Version compatibility -- ACCEPTABLE

- Compose BOM 2025.10.01 + Material3 1.4.0 + Navigation Compose 2.9.5: Compatible set.
- Mikepenz markdown 0.38.1 (comment says "Downgraded from 0.39.0 for Compose 1.9.4 compatibility"): Correct version for current Compose BOM.
- Glance 1.1.1: Standard stable release.
- Old `openai-client = "4.0.1"` and `ktor-client-okhttp` still defined in `libs.versions.toml` but NOT used in `build.gradle.kts`. **Minor dead config -- not a risk, just cleanup opportunity.**

### V8 (Behavior Coverage): Test coverage -- ADEQUATE

- 484 tests pass (3 known pre-existing failures: `CustomFeederTextToolbarTest` x1, `MenuConfigStoreTest` x2).
- AI feature tests: 138 unit tests for extraction, prompt building, tag parsing (from spec-32).
- Summary parsing tests: 58 unit tests (from spec-034).
- New master tests included: `PagingSettingsTest.kt`, `HtmlToAnnotatedStringTest.kt`, `MarkdownToHtmlConverterTest.kt`.
- **Gap**: No integration test for paging + translation simultaneously. However, since the mechanisms are orthogonal (paging = scroll position management, translation = content rendering), this is acceptable.

---

## 4. Destructive Action Gate

| Check | Result |
|-------|--------|
| Database migrations / schema changes | NONE |
| Irreversible state changes | NONE |
| Permission escalation | NONE |
| Secret exposure | NONE -- only pre-existing test API keys in test files |
| Force push / destructive git operations | NOT APPLICABLE (merge not yet committed) |

**No HALT conditions detected.**

---

## 5. Observations (Non-Blocking)

1. **Dead config in `libs.versions.toml`**: `openai-client = "4.0.1"` (line 42) and `ktor-client-okhttp` (line 71) are defined but no longer referenced by `build.gradle.kts`. Consider removing in a future cleanup.

2. **Dead code**: `MarkdownToHtmlConverter.kt` is a production file only used by its test. It came from master's summary rendering approach which was superseded by ai-features' `MarkdownContentSafe`. Not harmful, but dead weight.

3. **Paging edge gesture vs text selection**: On the 20% left/right tap zones, `clickable` may consume long-press gestures intended for text selection. This is master's pre-existing design choice, not a merge regression.

---

## 6. Verdict

**PASS**

The merge resolution is correct and complete. All 10 conflicting files are resolved properly. The `params[]` re-indexing in SettingsViewModel is verified correct across all 35 indices. The OpenAIApi.kt deletion is safe with zero remaining references. All upstream features (paging, widget, blocklist-on-summaries, AGP 9.0.0, i18n, LazyColumn-to-Column) are incorporated. All AI features (multi-provider summary/translation, circular progress, text selection menu, Anthropic support, cancel support) are preserved. ScrollState migration is consistent across all reader files. Translation parameters are properly threaded through the new Column-based rendering. No destructive data operations, no secret exposure, no HALT conditions.

The merge is safe to commit.
