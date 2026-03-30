# Spec 036: Research — Persist Translation Cache

## 1. Existing Pattern Analysis

### 1.1 Blob.kt (Article Content Storage)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/blob/Blob.kt`

`Blob.kt` is a set of **top-level utility functions** (no class) that manage gzip-compressed article HTML files:

```
blobFile(itemId, filesDir)        → File("$filesDir/$itemId.txt.gz")
blobInputStream(itemId, filesDir) → GZIPInputStream(file.inputStream())
blobOutputStream(itemId, filesDir)→ GZIPOutputStream(file.outputStream())

blobFullFile(itemId, filesDir)        → File("$filesDir/$itemId.full.html.gz")
blobFullInputStream(itemId, filesDir) → GZIPInputStream(...)
blobFullOutputStream(itemId, filesDir)→ GZIPOutputStream(...)
```

**Key observations:**
- Pure functions — no state, no DI, no class instantiation
- Caller is responsible for directory creation (`filePathProvider.articleDir.mkdirs()` in `RssLocalSync`)
- No error handling beyond `@Throws(IOException::class)` annotations
- Two file types share the same pattern (regular blob + full-text blob)
- Files keyed by `itemId` only (no language or other dimension)

### 1.2 FilePathProvider

**Location:** `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt`

Interface with paths for various directories:
- `filesDir` — root for backup-eligible files
- `articleDir` → `filesDir.resolve("articles")` — regular article blobs
- `fullArticleDir` → `cacheDir.resolve("full_articles")` — full-text blobs (cache, NOT backup-eligible)
- `cacheDir` — ephemeral cache (cleared by system)

**Important distinction:** `articleDir` is under `filesDir` (backup-eligible), while `fullArticleDir` is under `cacheDir` (NOT backup-eligible). Translation cache must go under `filesDir` since translations represent paid API results that should survive reinstall.

### 1.3 Translation Flow in ArticleViewModel

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

The `translate()` method follows this flow:

```
1. Extract TranslatableText paragraphs from LinearArticle via TranslatableTextExtractor
2. Build initial ArticleTranslation (all paragraphs translated=0)
3. Set TranslationState.Translating
4. Get targetLanguage from repository.translationLanguage
5. Create ParagraphTranslationCoordinator with AIClient
6. Collect progress events → update ArticleTranslation contents
7. When all paragraphs resolved → TranslationState.Translated
```

**Integration points for cache:**
- **Load:** Before step 1, check for cached `ArticleTranslation` matching `(itemId, languageCode)`. If found, skip directly to `TranslationState.Translated`.
- **Save:** After step 7 (transition to `Translated`), persist the final `ArticleTranslation` to disk.

The ViewModel gets `itemId` from `SavedStateHandle` and `targetLanguage` from `repository.translationLanguage.first()`.

### 1.4 Article Cleanup in RssLocalSync

**Location:** `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt`, lines 417-443

When articles are pruned from a feed:

```kotlin
for (id in articlesToDelete) {
    blobFile(itemId = id, filesDir = filePathProvider.articleDir).delete()
    blobFullFile(itemId = id, filesDir = filePathProvider.fullArticleDir).delete()
}
repository.deleteFeedItems(articlesToDelete)
```

**Pattern:** For each deleted article ID, delete its blob file(s), then delete the DB rows. Translation cache cleanup must be added to this same loop. Since translation files include a language suffix, cleanup needs to delete all `{itemId}_*.translation.json.gz` files (glob by prefix).

### 1.5 Data Model (Already Serializable)

- `ArticleTranslation` — `@Serializable`, contains `List<ParagraphTranslation>` + `status: String`
- `ParagraphTranslation` — `@Serializable`, contains `index`, `text`, `translation`, `translated`
- `kotlinx-serialization-json` already in `libs.versions.toml`

No additional serialization setup needed.

### 1.6 DI and Settings

- `ArchModelModule.kt` wires singletons via Kodein DI
- Settings use `SharedPreferences` (no DataStore in project)
- `TranslationLanguage` enum has a `code` property (e.g., `"zh"`, `"en"`); `DEVICE_DEFAULT` has `code = ""`

---

## 2. Implementation Options

### Option A: Extend Blob.kt with Translation Functions

**Description:** Add new top-level functions to `Blob.kt` (or a new `TranslationBlob.kt` file in the same `blob/` package) following the exact same pattern:

```kotlin
// In blob/TranslationBlob.kt
fun translationFile(itemId: Long, languageCode: String, filesDir: File): File =
    File(filesDir, "${itemId}_${languageCode}.translation.json.gz")

fun translationInputStream(itemId: Long, languageCode: String, filesDir: File): InputStream = ...
fun translationOutputStream(itemId: Long, languageCode: String, filesDir: File): OutputStream = ...

fun deleteTranslationFiles(itemId: Long, filesDir: File) {
    filesDir.listFiles { _, name -> name.startsWith("${itemId}_") && name.endsWith(".translation.json.gz") }
        ?.forEach { it.delete() }
}
```

Callers (ViewModel, RssLocalSync) use these directly with `filePathProvider.translationsDir`.

| Aspect | Assessment |
|--------|-----------|
| **Consistency** | Highest — identical pattern to existing Blob.kt |
| **Complexity** | Very low — ~30 lines of utility functions |
| **Testability** | High — pure functions, easy to test with temp dirs |
| **DI changes** | None for blob functions; add `translationsDir` to `FilePathProvider` |
| **Effort** | Low |

**Pros:**
- Minimal new code, follows existing convention exactly
- No new classes or abstractions to maintain
- Callers handle serialization/deserialization directly (simple, explicit)
- No DI wiring needed for the functions themselves

**Cons:**
- Callers must handle JSON serialization themselves (minor duplication between save and load sites)
- No encapsulation of the serialize/deserialize logic

---

### Option B: Dedicated TranslationCacheStore Class

**Description:** Create a `TranslationCacheStore` class that encapsulates file I/O + JSON serialization, registered as a Kodein singleton:

```kotlin
class TranslationCacheStore(
    private val filesDir: File,
    private val json: Json = Json,
) {
    private val translationsDir = filesDir.resolve("translations")

    suspend fun load(itemId: Long, languageCode: String): ArticleTranslation? = withContext(Dispatchers.IO) { ... }
    suspend fun save(itemId: Long, languageCode: String, translation: ArticleTranslation) = withContext(Dispatchers.IO) { ... }
    fun deleteForArticle(itemId: Long) { ... }
}
```

| Aspect | Assessment |
|--------|-----------|
| **Consistency** | Medium — other stores exist (SettingsStore, FeedStore) but they wrap Room/SharedPrefs, not files |
| **Complexity** | Low-medium — one class, ~60-80 lines |
| **Testability** | High — injectable, mockable for ViewModel tests |
| **DI changes** | Add binding in ArchModelModule |
| **Effort** | Low-medium |

**Pros:**
- Encapsulates serialization logic in one place
- Mockable in ViewModel unit tests (if needed)
- Suspend functions enforce IO dispatcher internally
- Clean API: `load()` / `save()` / `deleteForArticle()`

**Cons:**
- Adds a class + DI binding for a relatively simple operation
- Slightly different pattern than Blob.kt (class vs. top-level functions)
- The "Store" naming suggests a more complex data layer than warranted

---

### Option C: Room Database Table

**Description:** Add a new `translation_cache` table in Room with columns `(item_id, language_code, translation_json, created_at)`.

| Aspect | Assessment |
|--------|-----------|
| **Consistency** | Low — requirements explicitly say "no database schema change" (NFR-005) |
| **Complexity** | High — new Entity, DAO, migration, schema version bump |
| **Testability** | Medium — requires instrumented tests or in-memory DB |
| **DI changes** | New DAO binding |
| **Effort** | High |

**Pros:**
- Atomic writes, queryable, integrates with existing cascade deletes
- ForeignKey on `feed_items.id` would auto-cleanup on article deletion

**Cons:**
- **Violates NFR-005** (no database schema change)
- Requires Room migration (version bump, migration code)
- Larger JSON blobs in SQLite are inefficient compared to gzip files
- Overkill for simple key-value cache

**Recommendation: Eliminated** — explicitly excluded by requirements.

---

### Option D: Jetpack DataStore (Preferences or Proto)

**Description:** Use Jetpack DataStore to store translation cache entries.

| Aspect | Assessment |
|--------|-----------|
| **Consistency** | Low — project uses SharedPreferences, no DataStore anywhere |
| **Complexity** | Medium — new dependency, unfamiliar pattern for this codebase |
| **Testability** | Medium |
| **DI changes** | New DataStore instance binding |
| **Effort** | Medium-high |

**Pros:**
- Type-safe (Proto DataStore), async by default
- Modern Jetpack recommended approach for preferences

**Cons:**
- **Not used anywhere in the codebase** — introduces an entirely new dependency/pattern
- Preferences DataStore is key-value (would need one entry per article, awkward)
- Proto DataStore requires protobuf setup (build complexity)
- Poor fit: DataStore is designed for small, single-object state — not per-article caches
- Translation JSON per article can be 5-15 KB; DataStore is meant for small config data

**Recommendation: Eliminated** — poor fit for per-article cache data, adds unnecessary dependency.

---

### Option E: Hybrid — Top-Level Functions + Thin Helper for Serialization

**Description:** Keep Blob.kt-style top-level functions in `blob/TranslationBlob.kt` for file management, but add a small `TranslationCache` object with `save()`/`load()` methods that combine file I/O with JSON serialization:

```kotlin
// blob/TranslationBlob.kt — file path functions (same as Option A)
fun translationFile(itemId: Long, languageCode: String, filesDir: File): File = ...
fun deleteTranslationFiles(itemId: Long, filesDir: File) { ... }

// ai/TranslationCache.kt — serialization helper (no DI, no class)
object TranslationCache {
    fun save(itemId: Long, languageCode: String, filesDir: File, translation: ArticleTranslation) { ... }
    fun load(itemId: Long, languageCode: String, filesDir: File): ArticleTranslation? { ... }
}
```

| Aspect | Assessment |
|--------|-----------|
| **Consistency** | High — file functions follow Blob.kt pattern; cache object is a thin helper |
| **Complexity** | Low — ~50 lines total across two files |
| **Testability** | High — pure functions + object with no state |
| **DI changes** | None — only `translationsDir` added to `FilePathProvider` |
| **Effort** | Low |

**Pros:**
- Best of both worlds: Blob.kt consistency + no serialization duplication
- No DI wiring needed (object is stateless)
- Callers just call `TranslationCache.save(...)` / `TranslationCache.load(...)`
- Easy to test with temp directories

**Cons:**
- Two files for a small feature (but good separation of concerns)
- Not mockable for unit tests (but can test with real temp dirs instead)

---

## 3. Comparison Matrix

| Criterion | A: Extend Blob.kt | B: CacheStore Class | C: Room Table | D: DataStore | E: Hybrid |
|-----------|:-:|:-:|:-:|:-:|:-:|
| Follows existing patterns | +++  | ++   | --  | --  | +++  |
| Minimal code | +++  | ++   | -   | -   | ++   |
| No DI changes needed | +++  | -    | -   | -   | +++  |
| Encapsulates serialization | -    | +++  | +++ | ++  | ++   |
| Testability | +++  | +++  | +   | ++  | +++  |
| Meets NFR-005 (no schema) | +++  | +++  | --- | +++ | +++  |
| Effort | Low  | Low-Med | High | Med-High | Low |

---

## 4. Recommendation

**Primary: Option A (Extend Blob.kt)** — with serialization done inline at the two call sites (save in ViewModel after translation completes, load in ViewModel before starting translation).

**Rationale:**
1. **Maximum consistency** with the existing `Blob.kt` pattern — the codebase already uses top-level functions for file I/O, and callers handle the content format themselves (HTML text for blobs, JSON for translations).
2. **Minimum code** — ~30 lines of new functions in a `TranslationBlob.kt` file, plus ~20 lines of serialization at the two call sites in `ArticleViewModel`.
3. **No DI changes** needed for the functions themselves (only `FilePathProvider` gets a new `translationsDir` property).
4. **Simple cleanup** — `deleteTranslationFiles()` called in the same loop as blob deletion in `RssLocalSync`.

If the team prefers slightly more encapsulation (to avoid inline serialization in the ViewModel), **Option E** is the second choice — it adds a thin `TranslationCache` object for save/load while keeping the file-level functions Blob.kt-consistent.

---

## 5. Key Implementation Decisions to Confirm

1. **Language code for DEVICE_DEFAULT:** When `TranslationLanguage.DEVICE_DEFAULT` is selected (code = ""), the cache key should use the resolved device locale (e.g., `Locale.getDefault().language` → "en"). This means if the user changes device language, old cache won't match — which is correct behavior.

2. **Where to add `translationsDir`:** Add to `FilePathProvider` interface as `val translationsDir: File` → `filesDir.resolve("translations")`. This ensures backup eligibility.

3. **Cleanup in RssLocalSync:** In the `for (id in articlesToDelete)` loop (line 417), add `deleteTranslationFiles(itemId = id, filesDir = filePathProvider.translationsDir)` alongside the existing blob deletions.

4. **Feed deletion cascade:** `Repository.deleteFeeds()` calls `feedStore.deleteFeeds()` which cascades in Room. But blob files are only cleaned up in `RssLocalSync.syncFeed()` during the pruning loop. Need to verify if feed deletion also triggers blob cleanup or if this is a known gap. Translation cleanup should match whatever blob cleanup does.

5. **Re-translate behavior:** When the user taps translate and a cache exists → load from cache. When the user cancels (→ Empty) then taps translate again → fresh LLM call (no cache check since `cancelTranslation()` resets to Empty and the next `translate()` call could check cache again). Need to decide: should cancel + re-translate bypass cache? Options:
   - A) Always check cache in `translate()` — cancel + translate loads from cache again
   - B) Add a `forceRefresh` parameter to `translate()` for explicit re-translation
   - Recommend **(B)** — the "retranslate" action should explicitly bypass cache

6. **Auto-translate integration:** The `init` block in `ArticleViewModel` triggers `translate()` when `translationEnabled` is true. With cache, this auto-translate path will hit cache first, making cached articles load instantly.
