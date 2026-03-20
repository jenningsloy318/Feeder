# Spec 035: Merge master into ai-features -- Task List

## Task Execution Order

Tasks are ordered: LOW risk conflicts first, then MEDIUM, then HIGH, then post-merge integration, then verification.

---

- [ ] **T1: Start merge**
  ```bash
  git merge master --no-commit
  ```
  This starts the merge and pauses at conflicts. Verify the 10 expected conflicting files are reported. Do NOT commit yet.

---

- [ ] **T2: Resolve CHANGELOG.md** (LOW)
  - Keep ai-features' `[Unreleased]` section at the top (everything from `## [Unreleased]` down to the line before master's first release section)
  - Accept master's `[2.18.0] - 2026-03-07` section and `[2.17.0]` section below it
  - Accept master's contributor attribution fixes in `[2.16.1]` and older entries
  - Final order: header -> `[Unreleased]` -> `[2.18.0]` -> `[2.17.0]` -> `[2.16.1]` -> ...
  - `git add CHANGELOG.md`

---

- [ ] **T3: Resolve app/build.gradle.kts** (LOW)
  - Take master's build infrastructure: AGP 9.0.0 config, `versionCode = 3922`, `versionName = "2.18.0"`, removed `alias(libs.plugins.kotlin.android)`, `assets.directories.add()` API, Glance widget deps
  - Keep ai-features' Kotlin compiler args (`-Xannotation-default-target=param-property`)
  - Keep ai-features' AI SDK deps: `openai-java`, `anthropic-java`, `anthropic-java-okhttp`
  - Keep ai-features' Mikepenz markdown deps and reorderable dep
  - **DELETE** LeakCanary dep: remove `debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")`
  - Verify no old `openai-client-bom` / `openai-client` / `ktor-client-okhttp` deps remain
  - `git add app/build.gradle.kts`

---

- [ ] **T4: Resolve app/src/main/res/values/strings.xml** (LOW)
  - Keep ALL strings from both branches -- no content overlap
  - Master added ~13 strings (blocklist, open-in, paging, widget)
  - ai-features added ~120 strings (AI provider, translation, summary)
  - Accept both sets of additions in their original positions
  - `git add app/src/main/res/values/strings.xml`

---

- [ ] **T5: Resolve app/src/main/res/values-zh-rCN/strings.xml** (LOW)
  - Keep ALL translations from both branches -- no content overlap
  - Master added 9 Chinese translations; ai-features added 108
  - Accept both sets of additions
  - `git add app/src/main/res/values-zh-rCN/strings.xml`

---

- [ ] **T6: Resolve ArchModelModule.kt** (LOW)
  - Keep ai-features' version as base (already has `AIApi` binding, `TranslationSettingsViewModel`)
  - Add master's import: `import com.nononsenseapps.feeder.widget.FeedWidgetSettingsActivityViewModel`
  - Add master's binding after `bindWithActivityViewModelScope<MainActivityViewModel>()` (line 59):
    ```kotlin
    bindWithActivityViewModelScope<FeedWidgetSettingsActivityViewModel>()
    ```
  - Discard master's `OpenAIApi`, `OpenAIClient`, `OpenAIClientDefault`, `bindFactory` -- all replaced by `AIApi`
  - `git add app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt`

---

- [ ] **T7: Resolve OpenAIApi.kt -- DELETE** (HIGH)
  - This file was deleted on ai-features and modified on master
  - During merge conflict resolution, choose "deleted by us" (ai-features)
  - The `openai/` directory should not exist after merge
  - Command: `git rm app/src/main/java/com/nononsenseapps/feeder/openai/OpenAIApi.kt`
  - Also ensure no other files in `openai/` directory are re-introduced by merge:
    ```bash
    git rm -r app/src/main/java/com/nononsenseapps/feeder/openai/ 2>/dev/null || true
    ```
  - Rationale: ai-features' `ai/` package (AIApi, OpenAICompatibleClient, AnthropicClient, SummaryResponseParser) fully supersedes master's markdown prompt improvements

---

- [ ] **T8: Resolve MainActivity.kt** (MEDIUM)
  - **From master -- port**:
    1. Remove notification permission handling: delete `requestNotificationsPermission` field, `maybeRequestNotificationPermission()` method, and its calls in `onResume()` and `onCreate()`
    2. Remove imports: `android.Manifest`, `android.content.pm.PackageManager`, `android.os.Build`, `ActivityResultContracts`, `ContextCompat`, `PreferenceManager`, `BuildConfig`
    3. Add imports: `android.view.KeyEvent`, `com.nononsenseapps.feeder.archmodel.Repository`, `com.nononsenseapps.feeder.ui.ScrollDirection`
    4. Add field: `private val repository: Repository by instance()`
    5. Add `onKeyDown()` override for volume button paging (see spec section 3.7)
    6. Remove `maybeRequestNotificationPermission()` call from `onResume()` -- keep only `setResumeTime()` + `maybeRequestSync()`
    7. Remove `KEY_NOTIFICATION_PERMISSION_REQUESTED` from companion object
  - **From master -- port register() calls**:
    - All existing destinations: add `mainActivityViewModel` as 4th param
  - **From ai-features -- keep**:
    - AI destination imports and registrations
  - **Update AI destinations**: add `mainActivityViewModel` param to all 5 AI register() calls:
    ```kotlin
    ProviderListDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
    ProviderEditDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
    SummarySettingsDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
    TranslationSettingsDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
    SelectionMenuSettingsDestination.register(this, navController, navDrawerListState, mainActivityViewModel)
    ```
  - **Remove LeakCanary**: delete import `updateLeakCanaryNotificationState`, delete all calls to it
  - `git add app/src/main/java/com/nononsenseapps/feeder/ui/MainActivity.kt`

---

- [ ] **T9: Resolve SettingsViewModel.kt** (MEDIUM)
  - Keep ai-features' AI settings architecture (`AIApi`, `AISettings`, `AISettingsEvent`, `ModelsState`)
  - Port master's new settings to `combine()` flow:
    - Add `repository.applyBlocklistToSummaries` after `repository.blockList` (new index 18)
    - Add `repository.isPagingMode` at end (new index 33)
    - Add `repository.isAnimatedPaging` at end (new index 34)
  - Re-index ALL `params[]` accesses according to the index table in spec section 3.8 (35 total params, indices 0-34)
  - Add fields to `SettingsViewState`: `applyBlocklistToSummaries`, `isPagingMode`, `isAnimatedPaging`
  - Add setter methods: `setBlockListApplyToSummaries()`, `setPagingMode()`, `setAnimatedPaging()`
  - **CRITICAL**: Count carefully. A miscount causes runtime `ClassCastException`
  - `git add app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt`

---

- [ ] **T10: Resolve ArticleScreen.kt** (HIGH)
  - **ScrollState migration** (from master):
    - Replace `LazyListState` with `ScrollState`, `rememberLazyListState()` with `rememberScrollState()`
    - Pass `articleScrollState: ScrollState` instead of `articleListState: LazyListState` to inner composables
    - Update anchor scrolling from `animateScrollToItem()` to `animateScrollTo(pixelOffset)`
  - **Paging** (from master):
    - Add `mainActivityViewModel: MainActivityViewModel` parameter to outer `ArticleScreen`
    - Add `isPagingMode` state collection
    - Add `LaunchedEffect` for `scrollCommand` collection with 90% viewport scroll
    - Add invisible left/right tap zone overlay in `ReaderArticleContent`
  - **AI UI** (from ai-features -- keep):
    - `AISummaryState`, `TranslationState` state handling
    - `CircleProgressIconButton` for summarize/translate toolbar icons
    - `MarkdownContentSafe` for summary rendering (NOT master's AnnotatedString approach)
    - `TranslationStatusSection`, translation progress display
    - `WithFeederTextToolbar`, `TextSelectionMenuHandler`
    - `translatedParagraphs` extraction and passing
  - **Summary rendering**: Use ai-features' `MarkdownContentSafe` (Mikepenz). Discard master's `summary.annotatedStrings.forEach { Text(it) }` approach
  - `git add app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

---

- [ ] **T11: Resolve ArticleViewModel.kt** (HIGH)
  - **Accept ai-features' version entirely**
  - Discard ALL master changes:
    - No `AnnotatedString` import
    - No `convertSummaryToAnnotatedStrings()` method
    - No `annotatedStrings` field in summary state
    - No `MarkdownToHtmlConverter` usage
  - ai-features' version already has: `AIApi`, `AISummaryState`, `TranslationState`, cancellation, `SummaryResponseParser`, auto-fetch full text
  - `git add app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

---

- [ ] **T12: Post-merge: adapt LinearArticleContent for Column approach**
  - Master created `ColumnArticleContent()` composable (in `html/LinearArticleContent.kt`)
  - ai-features extended `linearArticleContent()` (LazyListScope) with translation params
  - **Action**: Add translation parameters to `ColumnArticleContent()`:
    - `translatedParagraphs: Map<Int, String>` param
    - `parentTranslationIndex: Int` param
    - Port `computeParagraphIndices()` and `ParagraphCounter` from ai-features' LazyListScope version
    - Each `LinearElementContent()` call must receive translation params
  - Update `ArticleScreen.kt` / `ReaderView.kt` to call `ColumnArticleContent()` instead of `linearArticleContent()`
  - The old `linearArticleContent()` LazyListScope extension can be removed if no longer referenced
  - Verify: `ReaderView.kt` uses `Column` + `verticalScroll(ScrollState)` (from master) and calls the updated `ColumnArticleContent()`

---

- [ ] **T13: Post-merge: add mainActivityViewModel to AI navigation destinations**
  - File: `NavigationDestinations.kt`
  - Update sealed class `NavigationDestination`:
    - `register()` method: add `mainActivityViewModel: MainActivityViewModel` parameter (4th param)
    - `RegisterScreen()` abstract method: add `mainActivityViewModel: MainActivityViewModel` parameter
  - Update ALL 13 destination objects' `override fun RegisterScreen()` to accept and (where needed) forward `mainActivityViewModel`:
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
    - `ArticleDestination` (line 643+) -- MUST forward to `ArticleScreen(mainActivityViewModel = ...)`
    - `SyncScreenDestination` (line 700+)
  - Add import: `import com.nononsenseapps.feeder.ui.MainActivityViewModel`

---

- [ ] **T14: Post-merge: clean up old OpenAI client references**
  - Verify `app/src/main/java/com/nononsenseapps/feeder/openai/` directory does not exist
  - Search for any remaining imports: `grep -rn "com.nononsenseapps.feeder.openai" app/src/`
  - Remove any found references
  - Verify `libs.versions.toml` does not contain `openai-client-bom` or `openai-client` entries
  - Verify `build.gradle.kts` does not contain old OpenAI BOM deps or `ktor-client-okhttp`

---

- [ ] **T15: Post-merge: remove LeakCanary references**
  - DELETE `app/src/debug/java/com/nononsenseapps/feeder/util/LeakCanaryCompat.kt`
  - DELETE `app/src/release/java/com/nononsenseapps/feeder/util/LeakCanaryCompat.kt`
  - Remove from `app/build.gradle.kts`: `debugImplementation("com.squareup.leakcanary:leakcanary-android:3.0-alpha-8")`
  - Remove from `MainActivity.kt`: import and calls to `updateLeakCanaryNotificationState`
  - Verify: `grep -rn "LeakCanary\|leakcanary" app/src/ app/build.gradle.kts` returns nothing

---

- [ ] **T16: Build verification**
  ```bash
  ./gradlew :app:compileFdroidDebugKotlin
  ```
  Must return BUILD SUCCESSFUL. If build fails, fix compilation errors (likely import issues, missing params, or type mismatches) before proceeding.

---

- [ ] **T17: Test verification**
  ```bash
  ./gradlew :app:testFdroidDebugUnitTest
  ```
  All tests must pass except 3 known pre-existing failures:
  - `CustomFeederTextToolbarTest` (1 test)
  - `MenuConfigStoreTest` (2 tests)

  If new test failures appear, investigate and fix before committing.
