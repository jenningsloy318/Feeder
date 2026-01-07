# Technical Specification: Import Saved Articles Feature

**Spec ID:** 028-add-import-saved-articles
**Date:** 2026-01-07
**Status:** DRAFT
**Author:** Coordinator Agent

---

## 1. Overview

This document provides the complete technical specification for implementing the import saved articles feature in the Feeder RSS reader application. The feature allows users to import previously exported saved articles from a plain text file.

### 1.1 Objectives

- **Primary:** Enable users to restore saved articles from exported file
- **Secondary:** Maintain consistency with existing export functionality
- **Quality:** Ensure robust error handling and user feedback
- **Performance:** Handle up to 10,000 URLs efficiently

### 1.2 Scope

**In Scope:**
- Import plain text files with one URL per line
- Match URLs to existing feed items in database
- Mark matching items as bookmarked
- Provide user feedback on import results
- Handle errors gracefully

**Out of Scope:**
- Creating new feed items (only updates existing)
- Cloud sync or remote import
- Format conversion (only plain text)
- Conflict resolution UI

---

## 2. Architecture

### 2.1 Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │ FeedScreen.kt                                    │  │
│  │  ├── savedArticleImporter (ActivityResultLauncher)│ │
│  │  ├── Menu item: "Import saved articles"         │  │
│  │  └── Toast feedback                              │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Business Logic Layer                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │ SavedArticlesImporter.kt (NEW)                  │  │
│  │  ├── importSavedArticles()                      │  │
│  │  ├── readUrlsFromFile()                         │  │
│  │  ├── validateUrls()                             │  │
│  │  └── markAsBookmarked()                         │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Data Access Layer                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │ FeedItemDao.kt                                   │  │
│  │  ├── getFeedItemIdsByLinks() [NEW - Batch]      │  │
│  │  ├── setBookmarked(ids: List<Long>) [NEW]       │  │
│  │  └── setBookmarked(id: Long, bool) [EXISTING]   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

```
User Action
    ↓
[Menu: Import saved articles]
    ↓
[File Picker: Select .txt file]
    ↓
[Launcher Callback: savedArticleImporter.launch(uri)]
    ↓
[Coroutine: ApplicationCoroutineScope.launch]
    ↓
[Suspend Function: importSavedArticles(di, uri)]
    ↓
├─→ [ContentResolver.openInputStream(uri)]
├─→ [BufferedReader.readLines()]
├─→ [Filter: Non-empty, trimmed, distinct]
├─→ [Batch Query: getFeedItemIdsByLinks(urls)]
├─→ [Batch Update: setBookmarked(ids)]
└─→ [Return: Either<Error, Result>]
    ↓
[Toast: Show success/error message]
```

---

## 3. Database Changes

### 3.1 Schema

**No schema changes required** - use existing `feed_items` table.

### 3.2 New DAO Methods

**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItemDao.kt`

#### Method 1: Get Feed Item IDs by Links (Batch)

```kotlin
/**
 * Batch query to get feed item IDs for multiple links.
 * Returns a list of pairs (id, link) for all matching items.
 *
 * @param links List of URLs to search for
 * @return List of (id, link) pairs for found items
 */
@Query("""
    SELECT id, link
    FROM feed_items
    WHERE link IN (:links)
""")
suspend fun getFeedItemIdsByLinks(links: List<String>): List<FeedItemIdLinkPair>
```

**Data Class:**
```kotlin
data class FeedItemIdLinkPair(
    val id: Long,
    val link: String
)
```

**Rationale:**
- Batch query avoids N+1 problem
- Single query for all URLs instead of N individual queries
- Returns only found items (unmatched URLs are omitted)
- Performance: ~50-100ms for 1,000 URLs vs 4+ seconds for individual queries

#### Method 2: Set Bookmarks (Batch)

```kotlin
/**
 * Batch update to mark multiple feed items as bookmarked.
 *
 * @param ids List of feed item IDs to bookmark
 * @return Number of items updated
 */
@Query("""
    UPDATE feed_items
    SET bookmarked = 1
    WHERE id IN (:ids)
""")
suspend fun setBookmarked(ids: List<Long>): Int
```

**Rationale:**
- Single UPDATE statement instead of N individual updates
- Performance: ~50-100ms for 1,000 IDs vs 2+ seconds for individual updates
- Returns count of affected rows for validation

**Alternative:** Loop with existing method (acceptable but less performant)
```kotlin
suspend fun setBookmarked(ids: List<Long>) {
    ids.forEach { id ->
        setBookmarked(id, true)
    }
}
```

---

## 4. Implementation Details

### 4.1 Import Function

**File:** `app/src/main/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporter.kt` (NEW)

```kotlin
package com.nononsenseapps.feeder.model.export

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.db.room.FeedItemDao
import com.nononsenseapps.feeder.util.Either
import com.nononsenseapps.feeder.util.ToastMaker
import com.nononsenseapps.feeder.util.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.system.measureTimeMillis

private const val LOG_TAG = "FEEDER_SAVEDARTIMPORT"

/**
 * Result of a saved articles import operation.
 *
 * @property importedCount Number of articles successfully imported
 * @property totalCount Total number of URLs in file
 * @property skippedCount Number of URLs not found in database (totalCount - importedCount)
 */
data class ImportResult(
    val importedCount: Int,
    val totalCount: Int,
    val skippedCount: Int = totalCount - importedCount
)

/**
 * Imports saved articles from a plain text file.
 *
 * The file should contain one URL per line (matching the export format).
 * Empty lines and whitespace-only lines are ignored.
 * Duplicate URLs are removed before processing.
 *
 * @param di Dependency injection container
 * @param uri URI of the file to import
 * @return Either<ImportError, ImportResult> - Left on error, Right on success
 */
suspend fun importSavedArticles(
    di: DI,
    uri: Uri,
): Either<SavedArticlesImportError, ImportResult> =
    Either.catching(
        onCatch = { throwable ->
            Log.e(LOG_TAG, "Failed to import saved articles", throwable)
            val toastMaker = di.direct.instance<ToastMaker>()

            when (throwable) {
                is SavedArticlesImportFileNotFoundError -> {
                    toastMaker.makeToast(R.string.import_file_not_found)
                }
                is SavedArticlesImportFileReadError -> {
                    toastMaker.makeToast(R.string.failed_to_read_import_file)
                    toastMaker.makeToast(throwable.throwable?.localizedMessage ?: "")
                }
                else -> {
                    toastMaker.makeToast(R.string.failed_to_import_saved_articles)
                    throwable.localizedMessage?.let { message ->
                        toastMaker.makeToast(message)
                    }
                }
            }

            throwable
        }
    ) {
        withContext(Dispatchers.IO) {
            val time = measureTimeMillis {
                val contentResolver: ContentResolver by di.instance()
                val feedItemDao: FeedItemDao by di.instance()

                // Step 1: Read URLs from file
                val urls = readUrlsFromFile(contentResolver, uri)

                if (urls.isEmpty()) {
                    val toastMaker = di.direct.instance<ToastMaker>()
                    toastMaker.makeToast(R.string.no_valid_urls_in_file)
                    return@withContext ImportResult(0, 0)
                }

                // Step 2: Batch query to find matching feed items
                val idPairs = feedItemDao.getFeedItemIdsByLinks(urls)

                // Step 3: Batch update to mark as bookmarked
                val ids = idPairs.map { it.id }
                val updatedCount = if (ids.isNotEmpty()) {
                    feedItemDao.setBookmarked(ids)
                } else {
                    0
                }

                // Step 4: Show success message
                val toastMaker = di.direct.instance<ToastMaker>()
                val message = feedItemDao.getImportedCountMessage(updatedCount, urls.size)
                toastMaker.makeToast(message)

                ImportResult(
                    importedCount = updatedCount,
                    totalCount = urls.size
                )
            }
            logDebug(LOG_TAG, "Imported saved articles in $time ms on ${Thread.currentThread().name}")
        }
    }

/**
 * Reads and parses URLs from a text file.
 *
 * @param contentResolver ContentResolver for file access
 * @param uri URI of the file to read
 * @return List of unique, non-empty URLs
 * @throws SavedArticlesImportFileNotFoundError if file cannot be opened
 * @throws SavedArticlesImportFileReadError if file cannot be read
 */
private suspend fun readUrlsFromFile(
    contentResolver: ContentResolver,
    uri: Uri
): List<String> = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.bufferedReader().use { reader ->
            reader.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()  // Remove duplicates
        }
    } ?: throw SavedArticlesImportFileNotFoundError(
        Throwable("Could not open file: $uri")
    )
}

/**
 * Sealed class representing import errors.
 */
sealed class SavedArticlesImportError {
    abstract val throwable: Throwable?
}

/**
 * Error thrown when the import file cannot be found or opened.
 */
data class SavedArticlesImportFileNotFoundError(
    override val throwable: Throwable?
) : SavedArticlesImportError()

/**
 * Error thrown when the import file cannot be read.
 */
data class SavedArticlesImportFileReadError(
    override val throwable: Throwable?
) : SavedArticlesImportError()

/**
 * Error thrown for unknown import failures.
 */
data class SavedArticlesImportUnknownError(
    override val throwable: Throwable
) : SavedArticlesImportError()

/**
 * Extension function to generate appropriate message for import results.
 */
private suspend fun FeedItemDao.getImportedCountMessage(
    importedCount: Int,
    totalCount: Int
): String {
    // This will use string resources in actual implementation
    val skippedCount = totalCount - importedCount
    return when {
        importedCount == 0 -> "No matching articles found in database"
        skippedCount == 0 -> "Imported $importedCount saved articles"
        else -> "Imported $importedCount of $totalCount saved articles ($skippedCount not found)"
    }
}
```

### 4.2 UI Integration

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`

**Add Import Statement:**
```kotlin
import com.nononsenseapps.feeder.model.export.importSavedArticles
```

**Add Launcher (near line 212, after opmlImporter):**
```kotlin
val savedArticleImporter =
    rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val applicationCoroutineScope: ApplicationCoroutineScope by di.instance()
            applicationCoroutineScope.launch {
                importSavedArticles(di, uri)
            }
        }
    }
```

**Add Menu Item (near line 355, with savedArticleExporter.launch):**
```kotlin
Menu(
    onClick = {
        try {
            savedArticleImporter.launch(arrayOf("text/*"))
        } catch (_: Exception) {
            // ActivityNotFoundException in particular
            toastMaker.makeToast(R.string.error_no_file_manager)
        }
    },
    text = stringResource(R.string.import_saved_articles),
    icon = painterResource(R.drawable.ic_import),
)
```

### 4.3 String Resources

**File:** `app/src/main/res/values/strings.xml`

```xml
<!-- Import saved articles -->
<string name="import_saved_articles">Import saved articles</string>
<string name="failed_to_import_saved_articles">Failed to import saved articles</string>
<string name="imported_n_saved_articles">Imported %1$s saved articles</string>
<string name="imported_n_of_m_saved_articles">Imported %1$s of %2$s saved articles</string>
<string name="no_valid_urls_in_file">No valid article URLs found in file</string>
<string name="import_file_not_found">Could not open import file</string>
<string name="failed_to_read_import_file">Failed to read import file</string>
<string name="error_no_file_manager">No file manager app found</string>
<string name="no_matching_articles_found">No matching articles found in database</string>
```

---

## 5. Error Handling

### 5.1 Error Hierarchy

```
SavedArticlesImportError (sealed class)
├── File Access Errors
│   ├── SavedArticlesImportFileNotFoundError
│   │   └── Cause: Invalid URI, permission denied, file not found
│   └── SavedArticlesImportFileReadError
│       └── Cause: I/O error, encoding error, file too large
│
├── Content Errors (Handled silently, logged)
│   ├── Invalid URL format
│   │   └── Action: Skip URL, log warning
│   ├── Empty lines
│   │   └── Action: Skip (filtered during read)
│   └── Duplicate URLs
│       └── Action: Deduplicate before processing
│
└── Database Errors
    ├── Connection error
    │   └── Action: Show toast, log error
    └── Query failure
        └── Action: Show toast, log error
```

### 5.2 Error Handling Strategy

**User-Facing Messages:**
- File not found: "Could not open import file"
- Read error: "Failed to read import file"
- No valid URLs: "No valid article URLs found in file"
- No matches: "No matching articles found in database"
- Partial success: "Imported X of Y saved articles"
- Success: "Imported X saved articles"
- Unknown error: "Failed to import saved articles"

**Logging:**
- All errors logged with context
- Include file URI when relevant
- Include counts (total/imported/skipped)
- Performance timing logged

**Recovery:**
- Continue processing on individual URL errors
- Never crash on malformed files
- Graceful degradation on partial failures

---

## 6. Testing Strategy

### 6.1 Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporterTest.kt` (NEW)

```kotlin
class SavedArticlesImporterTest {
    // Test URL validation logic
    @Test
    fun `valid URL passes validation`() { /* ... */ }

    @Test
    fun `invalid URL fails validation`() { /* ... */ }

    @Test
    fun `empty string is filtered out`() { /* ... */ }

    @Test
    fun `whitespace-only string is filtered out`() { /* ... */ }

    @Test
    fun `duplicate URLs are removed`() { /* ... */ }
}
```

### 6.2 Integration Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/model/export/ImportSavedTest.kt` (NEW)

```kotlin
@RunWith(AndroidJUnit4::class)
class ImportSavedTest : DIAware {
    private val context: Context = ApplicationProvider.getApplicationContext()
    lateinit var db: AppDatabase
    override val di = DI.lazy {
        // DI setup
    }

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @Test
    fun testImportSavedArticles() {
        runBlocking {
            // Setup: Create feed items and export file
            val feedId = insertTestFeed()
            val itemId1 = insertTestFeedItem(feedId, "https://example.com/article1")
            val itemId2 = insertTestFeedItem(feedId, "https://example.com/article2")

            val file = createTestFile("""
                https://example.com/article1
                https://example.com/article2
                https://example.com/nonexistent
            """.trimIndent())

            // Execute: Import from file
            val result = importSavedArticles(di, file.toUri())

            // Verify: Result is Right
            assertTrue(result.isRight())
            assertEquals(2, result.value.importedCount)
            assertEquals(3, result.value.totalCount)

            // Verify: Items are bookmarked
            assertTrue(db.feedItemDao().loadFeedItem(itemId1)?.bookmarked == true)
            assertTrue(db.feedItemDao().loadFeedItem(itemId2)?.bookmarked == true)
        }
    }

    @Test
    fun testImportWithDuplicates() {
        runBlocking {
            // Test duplicate URL handling
        }
    }

    @Test
    fun testImportEmptyFile() {
        runBlocking {
            // Test empty file handling
        }
    }

    @Test
    fun testImportInvalidUrls() {
        runBlocking {
            // Test invalid URL handling
        }
    }
}
```

### 6.3 UI Tests

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/feed/ImportSavedUiTest.kt` (NEW)

```kotlin
@RunWith(AndroidJUnit4::class)
class ImportSavedUiTest {
    @Test
    fun testImportMenuItemExists() {
        // Verify menu item is present
    }

    @Test
    fun testImportOpensFilePicker() {
        // Verify file picker opens on menu click
    }
}
```

---

## 7. Performance Considerations

### 7.1 Performance Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| File read (1,000 URLs) | < 100ms | Buffered I/O is fast |
| Batch query (1,000 URLs) | < 200ms | Single indexed query |
| Batch update (1,000 IDs) | < 100ms | Single UPDATE statement |
| **Total (1,000 URLs)** | **< 500ms** | **Sub-second response** |
| **Total (10,000 URLs)** | **< 5s** | **Acceptable wait time** |

### 7.2 Optimization Strategies

**Strategy 1: Batch Operations (REQUIRED)**
- Single query for all URLs
- Single update for all IDs
- Avoid N+1 query problem

**Strategy 2: Deduplication Before Query**
- Remove duplicate URLs before database query
- Reduces query size
- Avoids redundant work

**Strategy 3: Memory Management (For Large Files)**
```kotlin
// For files > 10,000 URLs, process in chunks
suspend fun importSavedArticlesLargeFile(di: DI, uri: Uri) {
    contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        reader.lineSequence()
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .distinct()
            .chunked(1000)  // Process 1,000 at a time
            .forEach { chunk ->
                processChunk(chunk)
            }
    }
}
```

**Strategy 4: Progress Feedback (For Large Imports)**
```kotlin
// Show progress indicator if import takes > 2 seconds
if (urls.size > 1000) {
    showLoadingIndicator()
    val result = importSavedArticles(di, uri)
    hideLoadingIndicator()
}
```

---

## 8. Security Considerations

### 8.1 Input Validation

**URL Validation:**
```kotlin
fun isValidUrl(url: String): Boolean {
    return try {
        URL(url).toURI()
        url.isNotBlank()
        url.length <= 2048  // Reasonable max length
    } catch (e: Exception) {
        false
    }
}
```

**File Size Limit:**
```kotlin
const val MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024  // 10 MB

fun validateFileSize(contentResolver: ContentResolver, uri: Uri): Boolean {
    return contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        pfd.statSize <= MAX_IMPORT_FILE_SIZE
    } ?: false
}
```

### 8.2 SQL Injection Prevention

**Room handles this automatically:**
```kotlin
// ✅ SAFE - Room parameterizes the query
@Query("SELECT id FROM feed_items WHERE link IN (:links)")
suspend fun getFeedItemIdsByLinks(links: List<String>)

// ❌ NEVER DO THIS - Manual concatenation
suspend fun unsafeQuery(links: List<String>) {
    val sql = "SELECT id FROM feed_items WHERE link IN (${links.joinToString()})"
    // DON'T DO THIS!
}
```

### 8.3 File System Safety

**Storage Access Framework (SAF) provides:**
- User-controlled file access
- No direct path access
- Automatic permission handling
- Sandboxed file operations

**No additional security measures needed**

---

## 9. Internationalization

### 9.1 Required Translations

**New Strings to Translate:**
1. `import_saved_articles`
2. `failed_to_import_saved_articles`
3. `imported_n_saved_articles`
4. `imported_n_of_m_saved_articles`
5. `no_valid_urls_in_file`
6. `import_file_not_found`
7. `failed_to_read_import_file`
8. `error_no_file_manager`
9. `no_matching_articles_found`

**Target Languages (30+):**
- All existing supported languages
- Follow existing translation patterns
- Use plural forms where appropriate

### 9.2 Plural Handling

```xml
<plurals name="imported_n_saved_articles">
    <item quantity="one">Imported %1$s saved article</item>
    <item quantity="other">Imported %1$s saved articles</item>
</plurals>
```

---

## 10. Migration and Compatibility

### 10.1 Database Migration

**No migration required** - no schema changes

### 10.2 Backward Compatibility

**Export Format:**
- Format has not changed
- Old exports can be imported
- Import is idempotent (safe to re-import)

**Forward Compatibility:**
- Future format changes should maintain backward compatibility
- Consider adding version header if format changes

---

## 11. Dependencies

### 11.1 Required Dependencies (All Present)

| Dependency | Version | Purpose |
|------------|---------|---------|
| Room KTX | From catalog | Database access |
| Kodein DI | From catalog | Dependency injection |
| Kotlin Coroutines | From catalog | Async operations |
| AndroidX Activity | From catalog | ActivityResultContracts |

### 11.2 No New Dependencies Required

---

## 12. Build Configuration

### 12.1 Build Variants

No changes to build configuration required.

### 12.2 ProGuard Rules

No additional ProGuard rules needed (all public APIs are preserved).

---

## 13. Documentation

### 13.1 Code Documentation

**KDoc Requirements:**
- All public functions must have KDoc comments
- Complex logic must be explained in comments
- Error conditions must be documented

### 13.2 User Documentation

**Features to Document:**
- How to export saved articles
- How to import saved articles
- File format requirements
- Troubleshooting common issues

---

## 14. Acceptance Criteria

### AC-1: Basic Import
✓ Can import valid URLs from text file
✓ Matching articles are marked as saved
✓ Success message shows correct count

### AC-2: Error Handling
✓ Invalid file shows error message
✓ Empty file shows appropriate message
✓ Read errors are handled gracefully

### AC-3: Edge Cases
✓ Duplicate URLs are handled correctly
✓ Invalid URLs are skipped
✓ Nonexistent URLs are skipped

### AC-4: Performance
✓ 1,000 URLs import in < 5 seconds
✓ UI remains responsive during import

### AC-5: Idempotency
✓ Importing same file twice is safe
✓ No duplicate bookmarks created

### AC-6: UI Integration
✓ Menu item is visible and functional
✓ File picker opens correctly
✓ Toast messages are appropriate

---

## 15. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Functional completeness | 100% | All AC pass |
| Test coverage | > 80% | Unit + integration tests |
| Performance | < 5s | 1,000 URLs in < 5 seconds |
| Bug count | 0 | No known bugs at release |
| Translation coverage | 100% | All 30+ languages |

---

## 16. Open Issues

| Issue | Status | Resolution |
|-------|--------|------------|
| Should we use transactions? | OPEN | No - partial success is acceptable |
| Should we show progress? | OPEN | Only if import takes > 2 seconds |
| Should we add file size limit? | OPEN | Yes - 10 MB limit recommended |

---

**Document Status:** COMPLETE
**Version:** 1.0
**Last Updated:** 2026-01-07
**Next Review:** Implementation Plan (Phase 6 continued)
