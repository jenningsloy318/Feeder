# Spec 035: Merge master into ai-features -- Specification

## 1. Merge Overview

Merge `master` (commit `dfdf7703`) into `ai-features` (commit `54666db7`) to incorporate all upstream features (paging, widgets, blocklist-on-summaries, LazyColumn-to-Column migration, AGP 9.0.0, i18n updates) while preserving the full AI feature set (multi-provider summary/translation, circular progress, text selection menu, Anthropic support).

- **Merge base**: `748fd571` (v2.16.1)
- **10 conflicting files** identified by `git merge master --no-commit`
- **Risk tiers**: 3 HIGH, 3 MEDIUM, 4 LOW

---

## 2. Pre-Merge Checklist

1. Ensure the worktree is on the `ai-features` branch at commit `54666db7`
2. Verify master is at commit `dfdf7703`: `git log --oneline -1 master`
3. Verify merge base: `git merge-base ai-features master` returns `748fd571`
4. Ensure working tree is clean: `git status` shows no uncommitted changes
5. Create a backup tag: `git tag pre-merge-backup ai-features`

---

## 3. Conflict Resolution Instructions

### 3.1 CHANGELOG.md -- LOW

**Strategy**: Keep both. ai-features' `[Unreleased]` section on top, master's `[2.18.0]` and `[2.17.0]` below.

**Resolution**:
- Accept ai-features' `[Unreleased]` section (lines 9-58 approximately)
- Accept master's `[2.18.0] - 2026-03-07` section and `[2.17.0]` section after it
- Accept master's contributor attribution fixes in older entries (2.16.1 and below)
- Final order: header -> `[Unreleased]` -> `[2.18.0]` -> `[2.17.0]` -> `[2.16.1]` -> ...

---

### 3.2 app/build.gradle.kts -- LOW

**Strategy**: Take master's build infrastructure + ai-features' AI dependencies.

**From master (keep)**:
- AGP 9.0.0 build config changes (`renderScript = false` removed, `assets.srcDir` changed to `assets.directories.add`)
- `versionCode = 3922`, `versionName = "2.18.0"`
- Removed `alias(libs.plugins.kotlin.android)` plugin
- Glance widget dependencies:
  ```kotlin
  implementation(libs.glance.appwidget)
  implementation(libs.glance.material3)
  ```
- LeakCanary dependency **removed** (line 281 in ai-features: `debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")` -- DELETE this line)

**From ai-features (keep)**:
- Kotlin compiler args in `kotlinOptions`:
  ```kotlin
  freeCompilerArgs += listOf("-Xannotation-default-target=param-property")
  ```
  and `@OptIn(ExperimentalCoroutinesApi::class)` usage
- AI SDK dependencies:
  ```kotlin
  implementation(libs.openai.java)
  implementation(libs.anthropic.java)
  implementation(libs.anthropic.java.okhttp)
  ```
- Mikepenz markdown dependencies:
  ```kotlin
  implementation(libs.mikepenz.markdown)
  implementation(libs.mikepenz.markdown.m3)
  implementation(libs.mikepenz.markdown.coil3)
  ```
- Reorderable dependency:
  ```kotlin
  implementation("sh.calvin.reorderable:reorderable:2.4.0")
  ```

**Remove from ai-features**:
- Old OpenAI client BOM/deps (if any remain): `openai-client-bom`, `openai-client`, `ktor-client-okhttp`

---

### 3.3 app/src/main/res/values/strings.xml -- LOW

**Strategy**: Keep ALL strings from both branches. No content overlap.

**Resolution**: Accept both additions. Master added ~13 strings (blocklist, open-in options, paging mode, widget). ai-features added ~120 strings (AI provider, translation, summary, provider management). Place master's new strings in their original positions, keep ai-features' strings in their positions. No strings conflict by name.

---

### 3.4 app/src/main/res/values-zh-rCN/strings.xml -- LOW

**Strategy**: Keep ALL translations from both branches. No content overlap.

**Resolution**: Same approach as `strings.xml`. Master added 9 Chinese translations; ai-features added 108. Accept both.

---

### 3.5 app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt -- LOW

**Strategy**: Keep ai-features' AI DI setup. Add master's `FeedWidgetSettingsActivityViewModel` binding.

**Current ai-features state** (lines 36-72):
- Line 47: `bind<AIApi>() with singleton { AIApi(instance(), appLang = Locale.getDefault().getISO3Language()) }`
- Line 71: `bindWithComposableViewModelScope<TranslationSettingsViewModel>()`
- No `FeedWidgetSettingsActivityViewModel`

**Master additions to port**:
- Add import: `import com.nononsenseapps.feeder.widget.FeedWidgetSettingsActivityViewModel`
- Add after line 59 (`bindWithActivityViewModelScope<MainActivityViewModel>()`):
  ```kotlin
  bindWithActivityViewModelScope<FeedWidgetSettingsActivityViewModel>()
  ```

**Master items to DISCARD**:
- `import com.nononsenseapps.feeder.archmodel.OpenAISettings` -- not needed
- `import com.nononsenseapps.feeder.openai.OpenAIApi` -- deleted package
- `import com.nononsenseapps.feeder.openai.OpenAIClient` -- deleted package
- `import com.nononsenseapps.feeder.openai.OpenAIClientDefault` -- deleted package
- `import org.kodein.di.bindFactory` -- not needed
- `import org.kodein.di.factory` -- not needed
- `bindFactory<OpenAISettings, OpenAIClient> { ... }` -- replaced by AIApi
- `bind<OpenAIApi>() with singleton { ... }` -- replaced by AIApi

---

### 3.6 app/src/main/java/com/nononsenseapps/feeder/openai/OpenAIApi.kt -- HIGH (DELETE)

**Strategy**: Keep ai-features' deletion. This file was deleted on ai-features and replaced by the `ai/` package.

**Resolution**:
- During merge conflict, choose "deleted by us" (ai-features)
- The file should NOT exist after merge
- Master's markdown prompt improvements are ALREADY SUPERSEDED by ai-features' `TranslationPromptBuilder` and `OpenAICompatibleClient`/`AnthropicClient` which use richer JSON-structured prompts with markdown in the `summary` field
- Verify: `app/src/main/java/com/nononsenseapps/feeder/openai/` directory should not exist after merge

---

### 3.7 app/src/main/java/com/nononsenseapps/feeder/ui/MainActivity.kt -- MEDIUM

**Strategy**: Take master's structural changes + ai-features' AI destinations. Add `mainActivityViewModel` param to AI destinations.

**From master (port)**:
1. **Remove notification permission handling** -- delete the entire `requestNotificationsPermission` field (ai-features lines 48-63), the `maybeRequestNotificationPermission()` method (lines 160-186), and calls to it in `onResume()` (line 79) and `onCreate()` (line 100)
2. **Remove imports**: `android.Manifest`, `android.content.pm.PackageManager`, `android.os.Build`, `androidx.activity.result.contract.ActivityResultContracts`, `androidx.core.content.ContextCompat`, `androidx.preference.PreferenceManager`, `com.nononsenseapps.feeder.BuildConfig`
3. **Add imports**: `android.view.KeyEvent`, `com.nononsenseapps.feeder.archmodel.Repository`
4. **Add `Repository` instance**: `private val repository: Repository by instance()`
5. **Add `onKeyDown()` method** after `onStop()`:
   ```kotlin
   override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
       if (mainActivityViewModel.isPagingMode.value && repository.isArticleOpen.value) {
           when (keyCode) {
               KeyEvent.KEYCODE_VOLUME_UP -> {
                   mainActivityViewModel.emitScrollCommand(ScrollDirection.UP)
                   return true
               }
               KeyEvent.KEYCODE_VOLUME_DOWN -> {
                   mainActivityViewModel.emitScrollCommand(ScrollDirection.DOWN)
                   return true
               }
           }
       }
       return super.onKeyDown(keyCode, event)
   }
   ```
6. **Add `ScrollDirection` import**: `import com.nononsenseapps.feeder.ui.ScrollDirection`
7. **Remove `maybeRequestNotificationPermission()` call from `onResume()`** (keep only `setResumeTime()` and `maybeRequestSync()`)
8. **Remove `maybeRequestNotificationPermission()` call from `onCreate()`**

**From ai-features (keep)**:
- AI navigation destination imports (lines 31-39): `ProviderEditDestination`, `ProviderListDestination`, `SelectionMenuSettingsDestination`, `SummarySettingsDestination`, `TranslationSettingsDestination`
- AI destination registrations (lines 128-136)

**Update AI destination registrations to add `mainActivityViewModel` param**:
All `register()` calls must gain the 4th parameter. Change:
```kotlin
ProviderListDestination.register(this, navController, navDrawerListState)
```
to:
```kotlin
ProviderListDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
```
Apply to all 5 AI destinations: `ProviderListDestination`, `ProviderEditDestination`, `SummarySettingsDestination`, `TranslationSettingsDestination`, `SelectionMenuSettingsDestination`.

**Remove LeakCanary references**:
- Delete import: `import com.nononsenseapps.feeder.util.updateLeakCanaryNotificationState` (line 41)
- Delete calls: `updateLeakCanaryNotificationState(this)` (lines 62, 161)
- Remove `KEY_NOTIFICATION_PERMISSION_REQUESTED` from companion object (line 157)

---

### 3.8 app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt -- MEDIUM

**Strategy**: Keep ai-features' AI settings architecture. Add master's new settings. Re-index `params[]`.

**From ai-features (keep entirely)**:
- `AIApi` instead of `OpenAIApi`
- `AISettings` instead of `OpenAISettings`
- `AISettingsEvent` instead of `OpenAISettingsEvent`
- `ModelsState` instead of `OpenAIModelsState`
- `autoFetchFullArticle` setting (params[7])
- `summaryLanguage` setting (params[28])
- `loadOpenAIModels()` using `aiApi.listModelIds()`

**From master (port)**:
- Add `repository.applyBlocklistToSummaries` to the `combine()` flow
- Add `repository.isPagingMode` to the `combine()` flow
- Add `repository.isAnimatedPaging` to the `combine()` flow
- Add corresponding fields to `SettingsViewState`:
  - `applyBlocklistToSummaries: Boolean = false`
  - `isPagingMode: Boolean = false`
  - `isAnimatedPaging: Boolean = false`
- Add setter methods:
  - `fun setBlockListApplyToSummaries(value: Boolean)` -> `repository.setBlockListApplyToSummaries(value)`
  - `fun setPagingMode(value: Boolean)` -> `repository.setPagingMode(value)`
  - `fun setAnimatedPaging(value: Boolean)` -> `repository.setAnimatedPaging(value)`

**Re-indexed `combine()` params (CRITICAL -- must be exact)**:

The merged `combine()` must include ALL flows from BOTH branches. The ai-features currently has 32 params (indices 0-31). Master's extra settings to add:
- `repository.applyBlocklistToSummaries` -- insert after `blockList` (currently ai-features index 17)
- `repository.isPagingMode` -- add at end
- `repository.isAnimatedPaging` -- add at end

Final flow order and indices (35 total, indices 0-34):
```
[0]  repository.currentTheme               -> ThemeOptions
[1]  repository.preferredDarkTheme          -> DarkThemePreferences
[2]  repository.currentSorting              -> SortingOptions
[3]  repository.showFab                     -> Boolean
[4]  repository.syncOnResume                -> Boolean
[5]  repository.syncOnlyOnWifi              -> Boolean
[6]  repository.syncOnlyWhenCharging        -> Boolean
[7]  repository.autoFetchFullArticle        -> Boolean          (ai-features)
[8]  repository.loadImageOnlyOnWifi         -> Boolean
[9]  repository.showThumbnails              -> Boolean
[10] repository.maximumCountPerFeed         -> Int
[11] repository.itemOpener                  -> ItemOpener
[12] repository.linkOpener                  -> LinkOpener
[13] repository.syncFrequency               -> SyncFrequency
[14] batteryOptimizationIgnoredFlow         -> Boolean
[15] repository.feedItemStyle               -> FeedItemStyle
[16] repository.swipeAsRead                 -> SwipeAsRead
[17] repository.blockList                   -> List<String>
[18] repository.applyBlocklistToSummaries   -> Boolean          (master -- NEW)
[19] repository.useDetectLanguage           -> Boolean
[20] repository.useDynamicTheme             -> Boolean
[21] immutableFeedsSettings                 -> List<UIFeedSettings>
[22] repository.isMarkAsReadOnScroll        -> Boolean
[23] repository.maxLines                    -> Int
[24] repository.showOnlyTitle               -> Boolean
[25] repository.isOpenAdjacent              -> Boolean
[26] repository.showReadingTime             -> Boolean
[27] repository.showTitleUnreadCount        -> Boolean
[28] repository.aiSettingsFlow              -> AISettings        (ai-features)
[29] repository.summaryLanguage             -> SummaryLanguage   (ai-features)
[30] openAIModelsState                      -> ModelsState       (ai-features)
[31] repository.isOpenDrawerOnFab           -> Boolean
[32] repository.font                        -> FontSelection
[33] repository.isPagingMode                -> Boolean           (master -- NEW)
[34] repository.isAnimatedPaging            -> Boolean           (master -- NEW)
```

The `SettingsViewState` constructor must be updated to include all fields with matching index casts.

---

### 3.9 app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt -- HIGH

**Strategy**: Take master's `ScrollState` migration + paging. Keep ai-features' AI UI (summary, translation, toolbar icons). This is the most complex conflict.

**From master (port)**:
1. **ScrollState migration**: Replace `LazyListState` with `ScrollState` throughout:
   - `import androidx.compose.foundation.ScrollState` + `rememberScrollState()`
   - Remove `import androidx.compose.foundation.lazy.LazyListState` + `rememberLazyListState()`
   - `val articleScrollState = rememberScrollState()` instead of `val articleListState = rememberLazyListState()`
   - Pass `articleScrollState` instead of `articleListState` to `ReaderArticleContent`
2. **Add `MainActivityViewModel` parameter** to outer `ArticleScreen` composable
3. **Add paging `LaunchedEffect`**: collect `mainActivityViewModel.scrollCommand` to scroll up/down by 90% of viewport
4. **Add `isPagingMode` state**: `val isPagingMode by mavm.isPagingMode.collectAsStateWithLifecycle()`
5. **Add paging overlay** in `ReaderArticleContent`: invisible left/right tap zones for screen-edge paging
6. **Use `ColumnArticleContent`** instead of `linearArticleContent` (see post-merge task T12)
7. **Anchor scrolling**: Change from `animateScrollToItem()` to `animateScrollTo(pixelOffset)` using `elementPositions` map

**From ai-features (keep)**:
1. **AI state naming**: `AISummaryState`, `TranslationState` (not `OpenAISummaryState`)
2. **Toolbar buttons**: `CircleProgressIconButton` for summarize and translate
3. **Summary rendering**: `MarkdownContentSafe` composable (Mikepenz) with `SummaryResponseParser.containsRawJson()` guard (supersedes master's `AnnotatedString` approach)
4. **Translation UI**: `TranslationStatusSection`, `TranslationErrorSection`, `translatedParagraphs` extraction
5. **Text selection**: `WithFeederTextToolbar` wrapper, `TextSelectionMenuHandler`
6. **Cancel support**: `cancelSummarize()`, `cancelTranslation()` buttons

**Master items to DISCARD**:
- Master's summary rendering with `AnnotatedString` list (replaced by `MarkdownContentSafe`)
- Master's `LoadingItem()` removal (already handled differently)

---

### 3.10 app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt -- HIGH

**Strategy**: Keep ai-features' version entirely.

**Resolution**: Accept ai-features' version for ALL conflicts. Discard ALL master changes.

**Master items to DISCARD**:
- `import androidx.compose.ui.text.AnnotatedString` -- not needed (Mikepenz renders directly)
- `convertSummaryToAnnotatedStrings()` method -- superseded by composable-layer rendering
- `annotatedStrings` field in `OpenAISummaryState.Result` -- not in ai-features' `AISummaryState`
- `MarkdownToHtmlConverter` usage -- replaced by Mikepenz library
- Explicit `<TSSError, List<AnnotatedString>>` type annotations on `Either.catching` -- not applicable

**ai-features' version includes**:
- `AIApi` injection (not `OpenAIApi`)
- `AISummaryState` with `Loading`, `Result`, `Error` states
- `TranslationState` with full per-paragraph translation
- `summarizeJob`/`translateJob` with `cancelSummarize()`/`cancelTranslation()`
- `CancellationException` handling
- `SummaryResponseParser.sanitizeErrorMessage()`
- Auto-fetch full text feature
- `ParagraphTranslationCoordinator` integration

---

## 4. Post-Conflict Integration Tasks

### 4.1 LinearArticleContent: LazyListScope to Column Migration (T12)

Master converted `linearArticleContent()` (a `LazyListScope` extension) to `ColumnArticleContent()` (a `@Composable` function). ai-features heavily extended `linearArticleContent()` with translation parameters.

**Action**: Adapt master's `ColumnArticleContent()` to include ai-features' translation parameters:
- Add `translatedParagraphs: Map<Int, String>` parameter
- Add `parentTranslationIndex: Int` parameter
- Keep `computeParagraphIndices()` and `ParagraphCounter` logic from ai-features
- Each `LinearElementContent()` call must receive `translation` and `parentTranslationIndex`
- Ensure table cell and image caption translation support is preserved

The ai-features' `linearArticleContent()` (LazyListScope version) can be removed after `ColumnArticleContent()` has full translation support.

### 4.2 NavigationDestinations: Add `mainActivityViewModel` Parameter (T13)

Master changed the `register()` method signature in `NavigationDestination` (sealed class) to include `mainActivityViewModel: MainActivityViewModel`.

On master, the abstract method is at line 91-95:
```kotlin
fun register(
    navGraphBuilder: NavGraphBuilder,
    navController: NavController,
    navDrawerListState: LazyListState,
    mainActivityViewModel: MainActivityViewModel,
)
```

On ai-features, the abstract method is at line 100-104 (without `mainActivityViewModel`).

**Action**:
1. Update the sealed class `NavigationDestination.register()` to add `mainActivityViewModel` parameter
2. Update the abstract `RegisterScreen()` method to add `mainActivityViewModel` parameter
3. Update ALL 13 destination objects' `RegisterScreen()` overrides:
   - `SearchFeedDestination` (line 174)
   - `TextSettingsDestination` (line 209)
   - `AddFeedDestination` (line 261)
   - `EditFeedDestination` (line 299)
   - `SettingsDestination` (line 328)
   - `ProviderListDestination` (line 381)
   - `ProviderEditDestination` (line 442)
   - `SummarySettingsDestination` (line 477)
   - `TranslationSettingsDestination` (line 510)
   - `SelectionMenuSettingsDestination` (line 531+)
   - `FeedDestination` (line 561+)
   - `ArticleDestination` (line 643+)
   - `SyncScreenDestination` (line 700+)
4. `ArticleDestination.RegisterScreen()` must forward `mainActivityViewModel` to `ArticleScreen()`
5. `FeedDestination.RegisterScreen()` may need `mainActivityViewModel` for paging integration

### 4.3 Clean Up Old OpenAI Client References (T14)

**Files to verify/clean**:
- `app/src/main/java/com/nononsenseapps/feeder/openai/` -- should NOT exist after merge
- Any import of `com.nononsenseapps.feeder.openai.*` -- remove
- `build.gradle.kts` -- remove `openai-client-bom`, `openai-client`, `ktor-client-okhttp` if present
- `libs.versions.toml` -- remove old `openai-client` version entry if present

### 4.4 Remove LeakCanary References (T15)

**Files to clean**:
1. `app/src/debug/java/com/nononsenseapps/feeder/util/LeakCanaryCompat.kt` -- DELETE
2. `app/src/release/java/com/nononsenseapps/feeder/util/LeakCanaryCompat.kt` -- DELETE
3. `app/src/main/java/com/nononsenseapps/feeder/ui/MainActivity.kt`:
   - Remove `import com.nononsenseapps.feeder.util.updateLeakCanaryNotificationState`
   - Remove all calls to `updateLeakCanaryNotificationState(this)`
4. `app/build.gradle.kts`:
   - Remove `debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")`

---

## 5. Build & Test Verification

### Build command
```bash
./gradlew :app:compileFdroidDebugKotlin
```
**Expected**: BUILD SUCCESSFUL

### Test command
```bash
./gradlew :app:testFdroidDebugUnitTest
```
**Expected**: All tests pass except 3 known pre-existing failures:
- `CustomFeederTextToolbarTest` (1 test)
- `MenuConfigStoreTest` (2 tests)

### Post-build checks
1. Verify `openai/` directory does not exist: `ls app/src/main/java/com/nononsenseapps/feeder/openai/ 2>/dev/null`
2. Verify no LeakCanary references: `grep -rn "LeakCanary\|leakcanary" app/src/main/java/ app/src/debug/ app/src/release/ 2>/dev/null`
3. Verify no old OpenAI imports: `grep -rn "com.nononsenseapps.feeder.openai" app/src/ 2>/dev/null`

---

## 6. Acceptance Criteria

1. **Build succeeds**: `./gradlew :app:compileFdroidDebugKotlin` returns BUILD SUCCESSFUL
2. **Tests pass**: `./gradlew :app:testFdroidDebugUnitTest` -- all tests pass (known 3 pre-existing failures excluded)
3. **AI features preserved**: All `ai/` package files intact, `AIApi`, `AIClient`, `OpenAICompatibleClient`, `AnthropicClient`, `SummaryResponseParser`, `TranslationPromptBuilder`, `InlineTagParser`, `ParagraphTranslationCoordinator` all compile
4. **Upstream features incorporated**:
   - Paging mode (volume buttons + tap zones) compiles and is wired
   - Widget files present and DI binding registered
   - Blocklist-on-summaries setting present
   - AGP 9.0.0 build config active
   - i18n updates for 20+ languages present
   - `LazyColumn` -> `Column` migration in ReaderView
5. **No dead code**:
   - No `openai/` package files
   - No LeakCanary files or references
   - No old `openai-client-bom` dependency
6. **Consistent navigation**: All 13 destinations have `mainActivityViewModel` parameter in `register()` and `RegisterScreen()`
7. **Translation rendering works with Column**: `ColumnArticleContent()` includes translation parameters
