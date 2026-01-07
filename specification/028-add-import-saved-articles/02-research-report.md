# Research Report: Import Saved Articles Feature

**Spec ID:** 028-add-import-saved-articles
**Date:** 2026-01-07
**Researcher:** Coordinator Agent
**Status:** COMPLETE

---

## 1. Executive Summary

This research document analyzes existing patterns in the Feeder codebase for file import/export operations and identifies best practices for implementing the import saved articles feature. The research focuses on understanding the OPML import/export pattern, database access patterns, and UI integration approaches.

**Key Finding:** The codebase has an established pattern for file import/export (OPML) that can be directly adapted for saved articles import.

---

## 2. Existing Export Implementation Analysis

### 2.1 Saved Articles Export

**File:** `app/src/main/java/com/nononsenseapps/feeder/model/export/SavedArticlesExporter.kt`

**Key Characteristics:**
- **Format:** Plain text file, one URL per line
- **File Extension:** `.txt` (though MIME type is `text/x-opml`)
- **Database Query:** `FeedItemDao.getLinksOfBookmarks()` - `SELECT link FROM feed_items WHERE bookmarked = 1`
- **File Access:** Uses `ContentResolver.openOutputStream(uri)`
- **Error Handling:** Uses `Either<SavedArticlesExportError, Unit>` monad
- **Threading:** `withContext(Dispatchers.IO)` for I/O operations
- **Logging:** Performance timing with `measureTimeMillis`

**Export Format Example:**
```
https://example.com/article1
https://example.com/article2
https://example.com/article3
```

### 2.2 OPML Import/Export Pattern

**File:** `app/src/main/java/com/nononsenseapps/feeder/model/opml/OpmlActions.kt`

**Key Implementation Patterns:**

1. **Import Function Signature:**
   ```kotlin
   suspend fun importOpml(
       di: DI,
       uri: Uri,
   ) = withContext(Dispatchers.IO) {
       // Implementation
   }
   ```

2. **Error Handling Pattern:**
   ```kotlin
   try {
       // Import logic
   } catch (e: Throwable) {
       Log.e(LOG_TAG, "Failed to import OPML", e)
       val toastMaker = di.direct.instance<ToastMaker>()
       toastMaker.makeToast(R.string.failed_to_import_OPML)
       (e.localizedMessage ?: e.message)?.let { message ->
           toastMaker.makeToast(message)
       }
   }
   ```

3. **File Reading Pattern:**
   ```kotlin
   contentResolver.openInputStream(uri).use { inputStream ->
       inputStream?.let { stream ->
           // Parse stream
       }
   }
   ```

4. **Post-Import Action:**
   - Triggers RSS sync after import: `runOnceRssSync(di = di, triggeredByUser = true)`

**Reusability for Saved Articles Import:**
- ✅ Function signature pattern
- ✅ Error handling pattern
- ✅ File reading pattern
- ❌ Post-sync action (not needed for saved articles)

---

## 3. Database Access Patterns

### 3.1 FeedItemDao Analysis

**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItemDao.kt`

**Existing Relevant Methods:**

1. **Get Bookmark Links (Export):**
   ```kotlin
   @Query("SELECT link FROM feed_items WHERE bookmarked = 1 order by link")
   suspend fun getLinksOfBookmarks(): List<String>
   ```

2. **Set Bookmark Status:**
   ```kotlin
   @Query("UPDATE feed_items SET bookmarked = :bookmarked WHERE id IS :id")
   suspend fun setBookmarked(id: Long, bookmarked: Boolean)
   ```

3. **Get Item ID by Link (NEEDS TO BE ADDED):**
   ```kotlin
   // Not currently exists - must be added
   @Query("SELECT id FROM feed_items WHERE link = :link LIMIT 1")
   suspend fun getFeedItemIdByLink(link: String): Long?
   ```

**Database Schema:**
- Table: `feed_items`
- Columns: `id` (Long, PK), `link` (String), `bookmarked` (Boolean)
- Indexing: Link column appears to be indexed (used in `duplicationExists` query)

### 3.2 Performance Considerations

**For 1,000 URLs:**
- Individual queries: ~1-2ms each → 1-2 seconds total
- Batch query with IN clause: ~50-100ms (much better)

**Recommended Approach:**
```kotlin
// Instead of N individual queries:
@Query("SELECT id, link FROM feed_items WHERE link IN (:links)")
suspend fun getFeedItemIdsByLinks(links: List<String>): Map<String, Long>

// Then batch update:
@Query("UPDATE feed_items SET bookmarked = 1 WHERE id IN (:ids)")
suspend fun setBookmarked(ids: List<Long>)
```

---

## 4. UI Integration Patterns

### 4.1 FeedScreen Launcher Pattern

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`

**Existing Launchers:**

1. **OPML Importer:**
   ```kotlin
   val opmlImporter = rememberLauncherForActivityResult(
       ActivityResultContracts.OpenDocument()
   ) { uri ->
       if (uri != null) {
           val applicationCoroutineScope: ApplicationCoroutineScope by di.instance()
           applicationCoroutineScope.launch {
               importOpml(di, uri)
           }
       }
   }
   ```

2. **Saved Articles Exporter:**
   ```kotlin
   val savedArticleExporter = rememberLauncherForActivityResult(
       ActivityResultContracts.CreateDocument("text/x-opml")
   ) { uri ->
       if (uri != null) {
           val applicationCoroutineScope: ApplicationCoroutineScope by di.instance()
           applicationCoroutineScope.launch {
               exportSavedArticles(di, uri)
           }
       }
   }
   ```

**Pattern for Saved Articles Importer:**
```kotlin
val savedArticleImporter = rememberLauncherForActivityResult(
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

**Launch Trigger:**
```kotlin
savedArticleImporter.launch(arrayOf("text/*"))
```

### 4.2 Menu Integration

**Location:** Settings menu near export option

**Existing String Resources:**
- `export_saved_articles` → "Export saved articles"
- `import_feeds_from_opml` → "Import feeds from OPML"
- `failed_to_import_OPML` → "Failed to import OPML"
- `failed_to_export_saved_articles` → "Failed to export saved articles"

**Needed New Strings:**
- `import_saved_articles` → "Import saved articles"
- `failed_to_import_saved_articles` → "Failed to import saved articles"
- `imported_n_saved_articles` → "Imported %1$s saved articles"
- `no_saved_articles_to_import` → "No valid saved articles found in file"

---

## 5. File I/O Best Practices

### 5.1 File Reading Patterns

**Best Practice 1: Buffered Reading (for large files)**
```kotlin
contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
    reader.lineSequence()
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .toList()
}
```

**Best Practice 2: Stream Processing (for very large files)**
```kotlin
contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
    reader.forEachLine { line ->
        if (line.isNotBlank()) {
            processUrl(line.trim())
        }
    }
}
```

**Best Practice 3: Read Entire File (for small files, < 10,000 lines)**
```kotlin
contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
    reader.readLines()
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .distinct() // Remove duplicates
}
```

**Recommendation:** Use Best Practice 3 for simplicity, switch to streaming if performance issues arise.

### 5.2 File Validation

**Before Processing:**
```kotlin
// Check file exists and is readable
contentResolver.openInputStream(uri) ?: return Either.Left(
    SavedArticlesImportFileNotFoundError(uri)
)

// Check file size (optional)
contentResolver.getType(uri) // Should be "text/plain" or similar
```

### 5.3 Error Handling Hierarchy

```
File Access Errors (show toast, log details)
├── File not found
├── Permission denied
├── Invalid URI
└── I/O errors

Content Errors (skip invalid, continue processing)
├── Invalid URL format
├── Empty lines
├── Duplicate URLs
└── Malformed encoding

Database Errors (show toast, log details)
├── Connection errors
├── Query failures
└── Transaction errors
```

---

## 6. Performance Optimization Strategies

### 6.1 Batch Database Operations

**Problem:** 1,000 individual `setBookmarked()` calls = ~1-2 seconds

**Solution 1: Batch Query + Batch Update**
```kotlin
suspend fun importSavedArticles(di: DI, uri: Uri): Either<ImportError, ImportResult> {
    // Read all URLs from file
    val urls = readUrlsFromFile(uri)

    // Batch query: Get all matching IDs at once
    val idMap = feedItemDao.getFeedItemIdsByLinks(urls)

    // Batch update: Mark all as bookmarked
    val ids = idMap.values.toList()
    feedItemDao.setBookmarked(ids)

    return Either.Right(ImportResult(
        importedCount = ids.size,
        totalCount = urls.size
    ))
}
```

**Solution 2: Transaction with Batch**
```kotlin
db.withTransaction {
    val urls = readUrlsFromFile(uri)
    val idMap = feedItemDao.getFeedItemIdsByLinks(urls)
    val ids = idMap.values.toList()
    feedItemDao.setBookmarked(ids)
}
```

### 6.2 Memory Management

**For Large Files (> 10,000 URLs):**
- Process in chunks (e.g., 1,000 URLs at a time)
- Use sequence processing instead of loading all into memory
- Consider coroutine-based parallel processing for chunks

**For Normal Files (< 10,000 URLs):**
- Load entire file into memory
- Batch database operations
- Single transaction

---

## 7. Android System Integration

### 7.1 File Picker Configuration

**Contract:** `ActivityResultContracts.OpenDocument()`

**MIME Types:**
```kotlin
// Accept all text files
savedArticleImporter.launch(arrayOf("text/*"))

// Or specific types
savedArticleImporter.launch(arrayOf(
    "text/plain",
    "text/x-opml"
))
```

**File Persistence:**
```kotlin
// Take persistable URI permissions to access file later
contentResolver.takePersistableUriPermission(
    uri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION
)
```

### 7.2 Permission Handling

**No Additional Permissions Required:**
- Uses Storage Access Framework (SAF)
- User explicitly grants permission via file picker
- No `READ_EXTERNAL_STORAGE` permission needed

**Edge Case: URI Permission Loss**
```kotlin
// Handle URI permission revocation
try {
    contentResolver.openInputStream(uri)
} catch (e: SecurityException) {
    // Show error: "File access denied. Please select the file again."
}
```

---

## 8. Testing Strategy

### 8.1 Test Coverage Areas

1. **Unit Tests:**
   - URL validation logic
   - Duplicate detection
   - Empty file handling
   - Malformed URL handling

2. **Integration Tests:**
   - Full import flow with test database
   - File reading from temp file
   - Database batch operations
   - Error scenarios

3. **UI Tests:**
   - Menu trigger opens file picker
   - File picker result handling
   - Toast messages display

### 8.2 Test Data Sets

**Test File 1: Basic Import**
```
https://example.com/article1
https://example.com/article2
https://example.com/article3
```
Expected: 3 articles bookmarked

**Test File 2: Duplicates**
```
https://example.com/article1
https://example.com/article1
https://example.com/article2
```
Expected: 2 articles bookmarked (1 duplicate)

**Test File 3: Invalid URLs**
```
https://example.com/article1
not-a-url
https://example.com/article2

(empty line)

https://example.com/article3
```
Expected: 3 articles bookmarked, 1 invalid URL skipped

**Test File 4: Nonexistent Articles**
```
https://example.com/article1
https://nonexistent.example.com/article
https://example.com/article2
```
Expected: 2 articles bookmarked, 1 URL not found

**Test File 5: Empty File**
```
(empty)
```
Expected: 0 articles bookmarked, appropriate message

---

## 9. Internationalization Considerations

### 9.1 String Resource Files

**English (`values/strings.xml`):**
```xml
<string name="import_saved_articles">Import saved articles</string>
<string name="failed_to_import_saved_articles">Failed to import saved articles</string>
<string name="imported_n_saved_articles">Imported %1$s saved articles</string>
<string name="no_valid_urls_in_file">No valid article URLs found in file</string>
<string name="import_file_error">Error reading import file: %1$s</string>
```

**All Supported Languages:**
- Values exist for 30+ languages (from existing string files)
- Must add translations for all new strings
- Follow existing translation patterns

### 9.2 Number Formatting

**Plural Handling:**
```xml
<plurals name="imported_n_saved_articles_plurals">
    <item quantity="one">Imported %1$s saved article</item>
    <item quantity="other">Imported %1$s saved articles</item>
</plurals>
```

---

## 10. Security Considerations

### 10.1 Input Validation

**URL Validation:**
```kotlin
fun isValidUrl(url: String): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: Exception) {
        false
    }
}
```

**SQL Injection Prevention:**
- Use Room's parameterized queries (already built-in)
- Never concatenate strings into queries

### 10.2 File System Safety

**Path Traversal Prevention:**
- Uses Storage Access Framework (SAF)
- No direct file path access
- User explicitly selects file

**Malicious File Handling:**
- Set reasonable file size limit (e.g., 10 MB)
- Timeout on file reading operations
- Handle malformed UTF-8 gracefully

---

## 11. Comparison with Existing Import Features

| Feature | OPML Import | Saved Articles Import |
|---------|-------------|----------------------|
| **File Format** | XML (OPML) | Plain text (one URL per line) |
| **Complexity** | High (nested structure) | Low (flat list) |
| **Parsing** | Custom XML parser | Simple line reading |
| **Database Impact** | Creates feeds & tags | Updates existing items only |
| **Post-Import Action** | Triggers RSS sync | None required |
| **Error Recovery** | Continue on parse error | Continue on invalid URL |
| **User Feedback** | Import count | Import count |

**Key Difference:** Saved articles import is simpler because it only updates existing records, no record creation.

---

## 12. Recommended Implementation Approach

### 12.1 Architecture Decision

**Pattern to Follow:** OPML import pattern

**Rationale:**
- Proven pattern in codebase
- Consistent error handling
- Familiar to maintainers
- Test coverage exists

### 12.2 Implementation Layers

```
UI Layer (FeedScreen.kt)
├── Menu item: "Import saved articles"
├── Launcher: savedArticleImporter
└── Coroutine scope

Business Logic Layer (SavedArticlesImporter.kt)
├── File reading
├── URL parsing
├── Batch database operations
└── Error handling

Data Access Layer (FeedItemDao.kt)
├── getFeedItemIdsByLinks() [NEW]
└── setBookmarked() [EXISTS]
```

### 12.3 Step-by-Step Implementation Plan

1. **Add DAO Method:** `getFeedItemIdsByLinks(urls: List<String>): Map<String, Long>`
2. **Create Import Function:** `importSavedArticles(di: DI, uri: Uri): Either<ImportError, ImportResult>`
3. **Add UI Launcher:** `savedArticleImporter` in FeedScreen
4. **Add Menu Item:** "Import saved articles" in settings menu
5. **Add String Resources:** All necessary translations
6. **Write Tests:** Mirror export test structure
7. **Integration Testing:** Manual testing with real export files

---

## 13. Risk Assessment

### 13.1 Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Database performance degradation | Low | Medium | Batch operations, indexing |
| File parsing errors | Medium | Low | Robust error handling |
| UI thread blocking | Low | High | Use Dispatchers.IO |
| Memory issues with large files | Low | Medium | Chunk processing if needed |

### 13.2 User Experience Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Users import wrong file | Medium | Low | Clear file name display |
| No feedback on progress | Low | Medium | Progress indicator |
| Confusing error messages | Low | Low | User-friendly copy |

---

## 14. Best Practices Summary

### DO:
✅ Follow OPML import pattern for consistency
✅ Use batch database operations for performance
✅ Provide clear user feedback (import count)
✅ Handle errors gracefully without crashes
✅ Log detailed errors for debugging
✅ Use Dispatchers.IO for file/database operations
✅ Validate input before processing
✅ Remove duplicate URLs before processing

### DON'T:
❌ Block UI thread during import
❌ Use individual database queries in a loop
❌ Crash on malformed files
❌ Show raw error messages to users
❌ Forget to handle URI permissions
❌ Ignore edge cases (empty files, duplicates, invalid URLs)

---

## 15. Open Questions & Decisions Needed

1. **Q:** Should we add a confirmation dialog before importing?
   **Research Finding:** OPML import doesn't have confirmation - user explicitly selects file
   **Recommendation:** No confirmation needed

2. **Q:** Should we show progress during import?
   **Research Finding:** OPML import shows no progress indicator
   **Recommendation:** Add simple loading indicator if import takes > 2 seconds

3. **Q:** How to handle very large import files (> 10,000 URLs)?
   **Research Finding:** Export can handle unlimited bookmarks
   **Recommendation:** Implement chunk processing if performance issues arise

4. **Q:** Should we import in a transaction for rollback?
   **Research Finding:** OPML import doesn't use transaction for rollback
   **Recommendation:** No transaction - partial success is acceptable

---

## 16. References

### Code References
- `SavedArticlesExporter.kt` - Export implementation
- `OpmlActions.kt` - Import/export pattern
- `FeedItemDao.kt` - Database access
- `FeedScreen.kt` - UI integration
- `ExportSavedTest.kt` - Test structure

### Documentation References
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
- Room Database: https://developer.android.com/training/data-storage/room
- Kotlin Coroutines: https://developer.android.com/kotlin/coroutines

---

## 17. Conclusion

The research phase has identified a clear implementation path for the import saved articles feature. The codebase already contains all necessary patterns and infrastructure:

1. **Export Format:** Simple plain text, one URL per line
2. **Import Pattern:** Follow OPML import structure
3. **Database Access:** Need to add batch query method
4. **UI Integration:** Mirror existing export UI pattern
5. **Error Handling:** Use Either monad pattern
6. **Testing:** Follow export test structure

**Risk Level:** LOW
**Implementation Complexity:** LOW
**Estimated Effort:** 4-6 hours

**Next Phase:** Code Assessment (Phase 5)

---

**Document Status:** COMPLETE
**Version:** 1.0
**Last Updated:** 2026-01-07
