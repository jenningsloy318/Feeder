# Spec 036: Persist Translation Cache -- Code Assessment

## 1. File Inventory

### Files to Create

| File | Purpose |
|------|---------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationCacheManager.kt` | Cache read/write/delete logic; gzip JSON serialization of `ArticleTranslation` |
| `app/src/test/java/com/nononsenseapps/feeder/ai/TranslationCacheManagerTest.kt` | Unit tests for cache manager (round-trip, corrupted file, missing file, cleanup) |

### Files to Modify

| File | Lines | Change Description |
|------|-------|--------------------|
| `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt` | 8-9, 47-48 | Add `translationDir: File` property (`filesDir/translations/`) |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | 514-599 | Modify `translate()` to check cache before calling coordinator; write cache after `Translated` state |
| `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt` | 417-441 | Add translation cache file cleanup in article deletion loop |
| `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt` | 47-51 | Add translation directory cleanup alongside article/fullArticle cleanup |

### Files That Already Support the Feature (No Modification Needed)

| File | Why |
|------|-----|
| `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt` | Already `@Serializable` with `kotlinx.serialization` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslation.kt` | Already `@Serializable` |
| `app/src/main/java/com/nononsenseapps/feeder/blob/Blob.kt` | Reference pattern only; translation cache follows same gzip approach but with its own naming scheme |
| `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt` | No changes needed; coordinator is called only when cache miss occurs |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslatableTextExtractor.kt` | No changes; extraction happens before cache check |
| `gradle/libs.versions.toml` | `kotlinx-serialization-json` already declared at line 118 |

---

## 2. Translation System Analysis

### Data Flow (Current)

```
translate() called
  -> extractTranslatableParagraphs() from LinearArticle content tree
  -> Build initial ArticleTranslation (all paragraphs with translated=0)
  -> Set TranslationState.Translating
  -> ParagraphTranslationCoordinator.translateParagraphs() emits Flow<ParagraphTranslationProgress>
  -> Each ParagraphComplete/ParagraphFailed updates ArticleTranslation.contents
  -> When all resolved -> TranslationState.Translated
```

### Data Flow (With Cache)

```
translate() called
  -> Get targetLanguage from repository.translationLanguage
  -> Resolve effective language code (handle DEVICE_DEFAULT -> Locale.getDefault().language)
  -> TranslationCacheManager.load(itemId, languageCode) on Dispatchers.IO
     -> If HIT: Set TranslationState.Translated directly (AC-006), RETURN
     -> If MISS/CORRUPT: Continue to LLM translation
  -> extractTranslatableParagraphs() from LinearArticle content tree
  -> Build initial ArticleTranslation (all paragraphs with translated=0)
  -> Set TranslationState.Translating
  -> ParagraphTranslationCoordinator.translateParagraphs() ...
  -> When all resolved -> TranslationState.Translated
     -> TranslationCacheManager.save(itemId, languageCode, articleTranslation) on Dispatchers.IO
```

### Key Observations

1. **`ArticleTranslation` and `ParagraphTranslation` are already `@Serializable`** -- no annotation changes needed.
2. **`TranslationState` is a sealed interface in `ArticleViewModel.kt`** (lines 748-766) -- not serializable, and doesn't need to be. Only the `ArticleTranslation` payload inside `Translated` is persisted.
3. **`translate()` runs on `Dispatchers.IO`** (line 516) -- cache I/O naturally happens in the right dispatcher context.
4. **`cancelTranslation()` resets to `TranslationState.Empty`** (line 604) -- no cache write triggered.

### Re-translate Behavior (AC-007 / SCENARIO-012 & 013)

Two paths to re-translate:
- **Tap translate while in `Translated` state**: The current `translate()` method calls `translateJob?.cancel()` at line 515 before starting fresh. The new implementation should skip the cache check when the current state is already `Translated` (user explicitly wants re-translation).
- **Cancel then translate**: `cancelTranslation()` sets state to `Empty`, then user taps translate again. Since state is `Empty`, the cache should NOT be loaded (SCENARIO-013 says "perform a fresh LLM translation"). This requires a `skipCache` flag or checking whether the user explicitly canceled.

**Recommended approach**: Add a `forceRetranslate` parameter to `translate()` (default false). When true, skip cache lookup. Set to true when called from `Translated` state. For the cancel-then-translate flow, add a `cacheInvalidated: Boolean` flag set to `true` by `cancelTranslation()` and reset by `translate()`.

### Target Language Resolution (DEVICE_DEFAULT)

`TranslationLanguage.DEVICE_DEFAULT` has `code = ""`. The cache file name needs a real locale code. Resolution: `if (code.isEmpty()) Locale.getDefault().language else code`. This matches NFR-003 and SCENARIO-019.

---

## 3. Integration Point Analysis

### 3.1 Cache Read -- `ArticleViewModel.translate()` (line 514)

**Where**: At the start of `translate()`, after getting `targetLanguage` but before extracting paragraphs.

**How**:
```kotlin
fun translate() {
    translateJob?.cancel()
    translateJob = viewModelScope.launch(Dispatchers.IO) {
        try {
            val targetLanguage = repository.translationLanguage.first()
            val languageCode = resolveLanguageCode(targetLanguage)

            // Cache check (skip if re-translating or cache invalidated)
            if (!forceRetranslate && !cacheInvalidated) {
                val cached = translationCacheManager.load(itemId, languageCode)
                if (cached != null) {
                    translationState.value = TranslationState.Translated(cached)
                    return@launch
                }
            }
            cacheInvalidated = false  // Reset flag

            // ... existing extraction + coordinator logic ...
        }
    }
}
```

### 3.2 Cache Write -- After `TranslationState.Translated` transition (line 584)

**Where**: Inside the `translationState.update { }` block at line 556, when status becomes `"translated"`.

**How**: After setting `TranslationState.Translated`, launch a non-blocking cache write:
```kotlin
if (updatedArticleTranslation.status == "translated") {
    // Write cache (fire-and-forget within the IO coroutine)
    translationCacheManager.save(itemId, languageCode, updatedArticleTranslation)
    TranslationState.Translated(articleTranslation = updatedArticleTranslation)
}
```

The `languageCode` must be captured outside the `update {}` lambda and stored for use here.

### 3.3 Cache Cleanup -- Article Deletion in `RssLocalSync.kt` (lines 417-441)

**Where**: Inside the `for (id in articlesToDelete)` loop, add translation file cleanup after blob/fullBlob cleanup.

**How**: Delete all translation cache files matching the pattern `{itemId}_*.translation.json.gz` in the translations directory:
```kotlin
translationCacheManager.deleteAllForArticle(id)
```

This is different from blob cleanup (which targets a single known file per article). Translation cache may have multiple files per article (one per language). Use `File.listFiles()` with a name filter or provide a known set of suffixes.

### 3.4 Orphaned File Cleanup -- `CleanupOrphanedFilesJob.kt` (lines 47-51)

**Where**: After the existing `cleanupDirectory` calls for articleDir and fullArticleDir.

**How**: Add a third cleanup pass for the translations directory. The existing `cleanupDirectory` method won't work directly because translation files have a `{itemId}_{lang}` pattern rather than just `{itemId}`. Need a modified approach:
```kotlin
cleanupTranslationsDirectory(filePathProvider.translationDir, validFeedItemIds)
```
This method would parse the itemId from filename prefix (before `_`) and delete if not in `validFeedItemIds`.

### 3.5 DI / FilePathProvider

**Where**: `FilePathProvider.kt` interface and implementation.

**How**: Add `val translationDir: File` to the interface and implement as `filesDir.resolve("translations")` in `FilePathProviderImpl`. Uses `filesDir` (not `cacheDir`) per NFR-002 to survive reinstall via Android auto-backup.

No DI module changes needed -- `FilePathProvider` is already bound as a singleton in `FeederApplication.kt` (line 76).

---

## 4. TranslationCacheManager Design

### Responsibility

Single class encapsulating all translation cache I/O. Dependencies: `FilePathProvider` (for directory), `kotlinx.serialization.json.Json` (for serialization).

### API Surface

```kotlin
class TranslationCacheManager(private val translationDir: File) {
    fun load(itemId: Long, languageCode: String): ArticleTranslation?
    fun save(itemId: Long, languageCode: String, translation: ArticleTranslation)
    fun deleteAllForArticle(itemId: Long)
    fun deleteOrphaned(validItemIds: Set<Long>)

    companion object {
        fun cacheFile(itemId: Long, languageCode: String, translationDir: File): File
    }
}
```

### File Naming

`{itemId}_{languageCode}.translation.json.gz` (e.g., `42_zh.translation.json.gz`)

### Serialization

```kotlin
private val json = Json { ignoreUnknownKeys = true }  // Forward-compatible
```

Using `ignoreUnknownKeys = true` for forward compatibility: if a future version adds fields to `ArticleTranslation` or `ParagraphTranslation`, older cached files can still be deserialized without crashing.

---

## 5. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| **Thread safety**: Concurrent read/write to same cache file (e.g., auto-translate triggers while user also taps translate) | Medium | Low | `translate()` already cancels any existing `translateJob` before starting a new one (line 515). This serializes access. Cache writes only happen after full completion, so no concurrent writes. |
| **Serialization compatibility**: Future schema changes to `ArticleTranslation` / `ParagraphTranslation` break deserialization | Medium | Medium | Use `Json { ignoreUnknownKeys = true }`. New fields should have default values. Document that removing or renaming existing fields requires a migration. |
| **Storage size impact** | Low | Low | ~2-5 KB per article per language (gzip JSON). Even 1000 articles in 3 languages = ~15 MB. Well within Android auto-backup 25 MB limit. |
| **Corrupted cache file** | Low | Low | Wrap deserialization in try-catch; log error and fall back to LLM translation. Corrupted file is overwritten on next successful translation (SCENARIO-015). |
| **DEVICE_DEFAULT locale change**: User changes device locale, old cache has different locale code | Low | Low | Cache is keyed by resolved locale code. If user changes device locale from `ja` to `en`, old `ja` cache remains but `en` cache doesn't exist, so LLM is called. Old `ja` files are harmless orphans until article is deleted. |
| **Impact on existing test suite** | Low | Low | No changes to existing test files. 3 known broken tests (`CustomFeederTextToolbarTest`, `MenuConfigStoreTest` x2) are pre-existing and unrelated. New `TranslationCacheManagerTest` is isolated. |
| **Auto-translate race with articleContentFlow** | Medium | Low | Auto-translate in `init {}` block (line 246) triggers `translate()` when `articleContent.elements.isNotEmpty()`. Cache check must happen before paragraph extraction, which it does in the proposed flow. |
| **File I/O errors (disk full, permissions)** | Low | Low | Wrap save in try-catch; log and ignore. Translation still works from LLM, just won't be cached. |

---

## 6. Recommended Approach

### Phase 1: Core Cache Manager (TDD)

1. Create `TranslationCacheManager` with `save()`, `load()`, `deleteAllForArticle()`, `deleteOrphaned()`.
2. Add `translationDir` to `FilePathProvider`.
3. Write unit tests for all cache operations (round-trip, corrupted file, missing file, multi-language, orphan cleanup).

### Phase 2: ViewModel Integration

1. Add `TranslationCacheManager` as a dependency in `ArticleViewModel` (via `FilePathProvider` instance already available).
2. Modify `translate()` to check cache first, write cache on completion.
3. Handle re-translate behavior (skip cache when state is `Translated` or when user has canceled).
4. Add `resolveLanguageCode()` helper for DEVICE_DEFAULT resolution.

### Phase 3: Cleanup Integration

1. Add translation cache cleanup in `RssLocalSync.syncFeed()` article deletion loop.
2. Add translation directory cleanup in `CleanupOrphanedFilesJob`.

### Phase 4: Integration Testing

1. Verify auto-translate loads from cache (SCENARIO-020).
2. Verify re-translate overwrites cache (SCENARIO-012).
3. Verify cancel-then-translate flow (SCENARIO-013).
4. Build verification: `./gradlew :app:compileFdroidDebugKotlin`
5. Test suite: `./gradlew :app:testFdroidDebugUnitTest`

---

## 7. Key Design Decisions

1. **`TranslationCacheManager` is a plain class, not DI-bound.** It takes a `File` directory parameter. The `ArticleViewModel` constructs it from `filePathProvider.translationDir`. This keeps DI changes minimal and the class easily testable with `@TempDir`.

2. **Cache check happens at the start of `translate()`, not in `init {}`**. This ensures cache is only loaded when translation is actually requested (manual or auto-translate), not on every article open.

3. **`ignoreUnknownKeys = true`** for forward-compatible deserialization.

4. **`filesDir/translations/`** (not `cacheDir`) per NFR-002 to survive reinstall.

5. **Wildcard deletion** (`{itemId}_*.translation.json.gz`) in cleanup to handle multiple language caches per article without needing to know which languages were translated.

6. **No database changes** per NFR-005. Pure file-based storage.
