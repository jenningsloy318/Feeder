# Spec 036: Persist Translation Cache -- Technical Specification

## 1. Overview

**Chosen approach:** Flow B (cache loaded on translate tap) + Option A (top-level functions in `TranslationBlob.kt` following `Blob.kt` pattern).

When the user taps Translate, the system checks for a cached translation file matching `(itemId, languageCode)`. If found, the cached `ArticleTranslation` is loaded directly into `TranslationState.Translated` -- no LLM call, no progress indicator. If not found, the normal LLM translation proceeds and the result is persisted on completion.

Cache files are gzip-compressed JSON stored under `filesDir/translations/` (backup-eligible). Cleanup follows the same lifecycle as article blob files.

---

## 2. Component Design

### 2.1 TranslationBlob.kt (New File)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/blob/TranslationBlob.kt`

Top-level functions following the exact `Blob.kt` pattern. Approximately 50 lines.

```kotlin
package com.nononsenseapps.feeder.blob

import com.nononsenseapps.feeder.ai.ArticleTranslation
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val json = Json { ignoreUnknownKeys = true }

/**
 * Returns the cache file for a given article + language combination.
 */
fun translationFile(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
): File = File(translationsDir, "${itemId}_${languageCode}.translation.json.gz")

/**
 * Loads a cached ArticleTranslation from disk, or null if not found / corrupted.
 * Must be called on Dispatchers.IO.
 */
fun loadTranslation(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
): ArticleTranslation? {
    val file = translationFile(itemId, languageCode, translationsDir)
    if (!file.isFile) return null
    return try {
        GZIPInputStream(file.inputStream()).bufferedReader().use { reader ->
            json.decodeFromString<ArticleTranslation>(reader.readText())
        }
    } catch (e: Exception) {
        // Corrupted file -- log and return null (SCENARIO-015)
        null
    }
}

/**
 * Saves an ArticleTranslation to disk as gzip-compressed JSON.
 * Creates the translations directory if it doesn't exist.
 * Must be called on Dispatchers.IO.
 */
fun saveTranslation(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
    translation: ArticleTranslation,
) {
    translationsDir.mkdirs()
    val file = translationFile(itemId, languageCode, translationsDir)
    GZIPOutputStream(file.outputStream()).bufferedWriter().use { writer ->
        writer.write(json.encodeToString(ArticleTranslation.serializer(), translation))
    }
}

/**
 * Deletes all translation cache files for a given article (all languages).
 * Used during article deletion in RssLocalSync and CleanupOrphanedFilesJob.
 */
fun deleteTranslationCache(
    itemId: Long,
    translationsDir: File,
) {
    if (!translationsDir.isDirectory) return
    translationsDir.listFiles { _, name ->
        name.startsWith("${itemId}_") && name.endsWith(".translation.json.gz")
    }?.forEach { it.delete() }
}
```

**Key design decisions:**
- `Json { ignoreUnknownKeys = true }` for forward compatibility (SCENARIO-024). Future fields added to `ArticleTranslation` / `ParagraphTranslation` with defaults won't break old cached files.
- `loadTranslation()` catches all exceptions (not just `IOException`) to handle both corrupted gzip and invalid JSON gracefully (SCENARIO-015).
- `saveTranslation()` calls `translationsDir.mkdirs()` to handle first-launch case (SCENARIO-016).
- `deleteTranslationCache()` uses wildcard matching on `{itemId}_*` prefix to delete all language variants (SCENARIO-010, SCENARIO-011).

### 2.2 FilePathProvider (Modify)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt`

Add `translationsDir` property to the interface and implementation.

**Interface addition:**
```kotlin
/**
 * Where translation cache files should be placed (backup-eligible)
 */
val translationsDir: File
```

**Implementation addition** (in `FilePathProviderImpl`):
```kotlin
override val translationsDir: File = filesDir.resolve("translations")
```

Uses `filesDir` (NOT `cacheDir`) per NFR-002 so that translation cache files:
- Survive app updates (SCENARIO-024)
- Are eligible for Android auto-backup and can survive reinstall (SCENARIO-023)

This matches the pattern of `articleDir` (`filesDir.resolve("articles")`).

### 2.3 ArticleViewModel Integration (Modify)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

#### 2.3.1 Language Code Resolution

Add a helper function to resolve the effective language code from `TranslationLanguage`:

```kotlin
private fun resolveLanguageCode(language: TranslationLanguage): String =
    if (language.code.isEmpty()) java.util.Locale.getDefault().language else language.code
```

When `TranslationLanguage.DEVICE_DEFAULT` is selected (`code = ""`), the actual device locale is resolved at runtime. This means:
- Cache file name uses the real locale (e.g., `42_ja.translation.json.gz`) -- SCENARIO-019
- If the device locale changes, old cache won't match (correct behavior)

#### 2.3.2 Cache Check in translate()

Modify `translate()` to accept a `forceRefresh` parameter and check cache before extracting paragraphs:

```kotlin
fun translate(forceRefresh: Boolean = false) {
    translateJob?.cancel()
    translateJob = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Step 0: Get target language and resolve code
            val targetLanguage = repository.translationLanguage.first()
            val languageCode = resolveLanguageCode(targetLanguage)

            // Step 0.5: Cache check (skip if re-translating)
            if (!forceRefresh) {
                val cached = loadTranslation(
                    itemId = itemId,
                    languageCode = languageCode,
                    translationsDir = filePathProvider.translationsDir,
                )
                if (cached != null) {
                    translationState.value = TranslationState.Translated(cached)
                    return@launch  // AC-002, AC-006: instant load, no progress indicator
                }
            }

            // Step 1: Extract paragraphs (existing code)
            val translatableTexts = extractTranslatableParagraphs()
            // ... existing steps 2-5 ...

            // Step 6: After transition to Translated, save cache
            // (inside the translationState.update block, after status == "translated")
        }
    }
}
```

#### 2.3.3 Cache Save After Translation Completes

Inside the `translationState.update { }` block, after detecting `status == "translated"`:

```kotlin
if (updatedArticleTranslation.status == "translated") {
    // Save to cache (fire-and-forget, already on Dispatchers.IO)
    try {
        saveTranslation(
            itemId = itemId,
            languageCode = languageCode,
            translationsDir = filePathProvider.translationsDir,
            translation = updatedArticleTranslation,
        )
    } catch (e: Exception) {
        // Log but don't fail -- translation still works, just won't be cached
        Log.w(LOG_TAG, "Failed to save translation cache", e)
    }
    TranslationState.Translated(articleTranslation = updatedArticleTranslation)
}
```

The `languageCode` variable must be captured before the `collect` block so it's available inside the `update` lambda.

**Important:** The save happens inside the coroutine that's already on `Dispatchers.IO`, so no additional dispatcher switch is needed (NFR-004, SCENARIO-018).

#### 2.3.4 Re-translate Behavior (AC-007)

Two paths trigger re-translation:

1. **Tap translate while in `Translated` state** (SCENARIO-012): The UI calls `translate(forceRefresh = true)`. This skips cache lookup and performs a fresh LLM call. On completion, the new result overwrites the cache file.

2. **Cancel then translate** (SCENARIO-013): `cancelTranslation()` resets to `Empty`. The next `translate()` call (default `forceRefresh = false`) would normally check cache. Per SCENARIO-013, this should perform a fresh LLM translation. Solution: `cancelTranslation()` sets a `cacheSkipOnNextTranslate` flag. `translate()` checks and clears this flag:

```kotlin
private var cacheSkipOnNextTranslate = false

fun cancelTranslation() {
    translateJob?.cancel()
    translateJob = null
    translationState.value = TranslationState.Empty
    cacheSkipOnNextTranslate = true
}

fun translate(forceRefresh: Boolean = false) {
    val skipCache = forceRefresh || cacheSkipOnNextTranslate
    cacheSkipOnNextTranslate = false
    // ... use skipCache instead of forceRefresh in cache check ...
}
```

#### 2.3.5 Integration with Auto-translate

The `init {}` block triggers `translate()` when auto-translate is enabled. Since `forceRefresh` defaults to `false` and `cacheSkipOnNextTranslate` starts as `false`, auto-translate naturally checks cache first (SCENARIO-020).

### 2.4 Cleanup in RssLocalSync (Modify)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt`, lines 417-441

Add translation cache deletion in the existing `for (id in articlesToDelete)` loop, after the blob file deletions:

```kotlin
for (id in articlesToDelete) {
    // Existing blob cleanup
    blobFile(itemId = id, filesDir = filePathProvider.articleDir).let { file -> ... }
    blobFullFile(itemId = id, filesDir = filePathProvider.fullArticleDir).let { file -> ... }

    // New: translation cache cleanup (SCENARIO-010, SCENARIO-011)
    deleteTranslationCache(
        itemId = id,
        translationsDir = filePathProvider.translationsDir,
    )
}
```

This handles both individual article deletion and feed deletion cascades (SCENARIO-011), because feed deletion flows through the same pruning loop.

### 2.5 Cleanup in CleanupOrphanedFilesJob (Modify)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt`

The existing `cleanupDirectory()` method uses a `fileProvider: (Long, File) -> File` callback that maps an itemId to a single known file. Translation files have a different pattern (`{itemId}_{lang}`) where one itemId maps to multiple files. A new method is needed:

```kotlin
private suspend fun cleanupTranslationsDirectory(
    directory: File,
    validIds: Set<Long>,
) {
    if (!directory.isDirectory) return

    withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { file ->
            try {
                // Parse itemId from filename: "{itemId}_{lang}.translation.json.gz"
                val itemId = file.name.substringBefore("_").toLongOrNull()
                if (itemId == null || itemId !in validIds) {
                    if (file.delete()) {
                        logDebug(LOG_TAG, "Deleted orphaned translation file: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error processing translation file ${file.name}", e)
            }
        }
    }
}
```

Add call in `doWork()` after the existing cleanup calls:

```kotlin
// Clean up translation cache files
cleanupTranslationsDirectory(
    filePathProvider.translationsDir,
    validFeedItemIds.toSet(),
)
```

---

## 3. File Naming Convention

**Format:** `{itemId}_{languageCode}.translation.json.gz`

**Examples:**
| itemId | Language Setting | Resolved Code | File Name |
|--------|-----------------|---------------|-----------|
| 42 | Chinese (`zh`) | `zh` | `42_zh.translation.json.gz` |
| 42 | English (`en`) | `en` | `42_en.translation.json.gz` |
| 42 | DEVICE_DEFAULT (device=ja) | `ja` | `42_ja.translation.json.gz` |
| 12345 | French (`fr`) | `fr` | `12345_fr.translation.json.gz` |

**Storage location:** `filesDir/translations/` (backup-eligible, survives app updates)

---

## 4. Error Handling Strategy

| Error Case | Handling | Scenario |
|------------|----------|----------|
| Cache file not found | Return null, proceed to LLM translation | SCENARIO-014 |
| Corrupted gzip data | Catch exception, return null, proceed to LLM | SCENARIO-015 |
| Invalid JSON in cache | Catch `SerializationException`, return null, proceed to LLM | SCENARIO-015 |
| Disk full on save | Catch `IOException`, log warning, translation still works | NFR-004 |
| Directory creation fails | Catch in `saveTranslation()`, log, skip cache write | SCENARIO-016 |
| I/O error during cleanup | Catch per-file, log, continue with remaining files | Matches existing blob cleanup pattern |

**Principle:** Cache failures are never user-visible errors. The system always falls back to LLM translation. Cache is an optimization, not a requirement.

---

## 5. Thread Safety Analysis

| Concern | Analysis | Mitigation |
|---------|----------|------------|
| Concurrent read/write to same file | `translate()` cancels any existing `translateJob` (line 515) before starting a new one. This serializes cache access. | Built-in: `translateJob?.cancel()` at start of `translate()` |
| Race between save and cleanup | Cleanup runs in `RssLocalSync` (during feed sync) or `CleanupOrphanedFilesJob` (daily idle). Save runs in `ArticleViewModel` coroutine. Extremely unlikely to target the same file simultaneously. | Low risk; file.delete() and file.write() are atomic at OS level for different files |
| `cacheSkipOnNextTranslate` flag | Accessed only from main thread (UI callbacks: `translate()`, `cancelTranslation()`) or from the coroutine that reads it (which is launched from `translate()` before any concurrent access). | Single-writer pattern; no synchronization needed |
| Multiple ViewModel instances for same article | Android ViewModel scoping prevents this -- only one `ArticleViewModel` per `itemId` exists at a time. | Framework guarantee |

---

## 6. Data Flow Diagrams

### 6.1 Cache Hit Flow (SCENARIO-002, SCENARIO-003, SCENARIO-020)

```
User taps Translate (or auto-translate triggers)
  |
  v
translate(forceRefresh=false)
  |
  v
Get targetLanguage from repository
  |
  v
resolveLanguageCode() -> languageCode
  |
  v
loadTranslation(itemId, languageCode, translationsDir)
  |
  +-- File exists & valid JSON --> TranslationState.Translated (instant, no Translating state)
  |                                 DONE
  |
  +-- File missing or corrupted --> Continue to LLM translation (existing flow)
```

### 6.2 Cache Save Flow (SCENARIO-001, SCENARIO-014)

```
LLM translation completes (all paragraphs resolved)
  |
  v
updatedArticleTranslation.status == "translated"
  |
  v
saveTranslation(itemId, languageCode, translationsDir, translation)
  |
  v
TranslationState.Translated(updatedArticleTranslation)
```

### 6.3 Re-translate Flow (SCENARIO-012, SCENARIO-013)

```
Path A: Tap translate while Translated
  |
  v
UI calls translate(forceRefresh=true)
  |
  v
Skip cache check --> LLM translation --> Save overwrites cache

Path B: Cancel then translate
  |
  v
cancelTranslation() --> sets cacheSkipOnNextTranslate=true
  |
  v
translate() --> skipCache=true (flag cleared) --> LLM translation --> Save overwrites cache
```

---

## 7. BDD Scenario Cross-Reference

| Component | Scenarios Covered |
|-----------|-------------------|
| `TranslationBlob.kt` — `saveTranslation()` | SCENARIO-001, SCENARIO-006, SCENARIO-016, SCENARIO-021 |
| `TranslationBlob.kt` — `loadTranslation()` | SCENARIO-002, SCENARIO-014, SCENARIO-015 |
| `TranslationBlob.kt` — `deleteTranslationCache()` | SCENARIO-010, SCENARIO-011 |
| `FilePathProvider.translationsDir` | SCENARIO-001, SCENARIO-023, SCENARIO-024 |
| `ArticleViewModel` — cache check in `translate()` | SCENARIO-002, SCENARIO-003, SCENARIO-004, SCENARIO-017, SCENARIO-020 |
| `ArticleViewModel` — cache save after Translated | SCENARIO-001, SCENARIO-018 |
| `ArticleViewModel` — `forceRefresh` parameter | SCENARIO-012 |
| `ArticleViewModel` — `cacheSkipOnNextTranslate` flag | SCENARIO-013 |
| `ArticleViewModel` — `resolveLanguageCode()` | SCENARIO-004, SCENARIO-005, SCENARIO-019, SCENARIO-022 |
| `ArticleViewModel` — cancel handling | SCENARIO-007, SCENARIO-008, SCENARIO-009 |
| `RssLocalSync` — cleanup in deletion loop | SCENARIO-010, SCENARIO-011 |
| `CleanupOrphanedFilesJob` — orphan cleanup | SCENARIO-010 (orphaned files) |
| Backup eligibility (`filesDir`) | SCENARIO-023, SCENARIO-024 |
