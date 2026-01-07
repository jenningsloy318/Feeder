# Requirements Document: Add Import Saved Articles

**Spec ID:** 028-add-import-saved-articles
**Date:** 2026-01-07
**Status:** DRAFT
**Author:** Coordinator Agent

---

## 1. Executive Summary

Implement the import functionality for saved articles to complement the existing export feature. The app currently allows users to export their saved (bookmarked) articles to a text file, but lacks the corresponding import functionality. This feature will enable users to restore their saved articles from a previously exported file.

---

## 2. User Story

**As a** Feeder user who has exported my saved articles
**I want to** import saved articles from a text file
**So that** I can restore my bookmarked articles after device migration, data loss, or when setting up a new device

---

## 3. Functional Requirements

### 3.1 Core Requirements

1. **FR-1:** Add import functionality to complement existing export saved articles feature
2. **FR-2:** Read and parse text files containing one URL per line (export format)
3. **FR-3:** For each URL in the import file, find the corresponding feed item in the database
4. **FR-4:** Mark matching feed items as bookmarked (saved)
5. **FR-5:** Handle duplicate URLs gracefully (idempotent operation)
6. **FR-6:** Provide user feedback on import success/failure
7. **FR-7:** Add UI trigger for import functionality in the settings/menu

### 3.2 Input Format

The import file format must match the export format:
- Plain text file with one URL per line
- File extension: `.txt` (same as export)
- Encoding: UTF-8
- Line separator: LF or CRLF
- Empty lines should be skipped
- Lines with only whitespace should be skipped

**Example import file:**
```
https://example.com/article1
https://example.com/article2
https://example.com/article3
```

### 3.3 Import Behavior

1. **URL Matching:** Match URLs exactly as stored in `feed_items.link` column
2. **Bookmark Updates:** Use `FeedItemDao.setBookmarked(id, true)` for each match
3. **Unmatched URLs:** URLs not found in the database should be silently skipped
4. **Idempotency:** Importing the same file multiple times should not cause errors
5. **Order Independence:** Import order does not matter
6. **Partial Success:** Continue processing even if some URLs fail

### 3.4 Error Handling

**Error Categories:**

1. **File Access Errors:**
   - File not found
   - Permission denied
   - File read errors
   - Invalid URI

2. **Content Errors:**
   - Invalid URL format (should skip with log)
   - Empty file (inform user, not error)
   - Malformed UTF-8 encoding

3. **Database Errors:**
   - Database connection errors
   - Query failures

**Error Handling Strategy:**
- Show user-friendly toast messages for all errors
- Log detailed errors for debugging
- Never crash on import errors
- Gracefully skip individual invalid URLs
- Report total imported count vs errors

### 3.5 UI Requirements

1. **UI-1:** Add "Import Saved Articles" menu item near "Export Saved Articles"
2. **UI-2:** Use file picker (`ActivityResultContracts.OpenDocument()`) for file selection
3. **UI-3:** Accept `.txt` files (matching export format)
4. **UI-4:** Show progress indication during import (toast or dialog)
5. **UI-5:** Display success message with count of imported articles
6. **UI-6:** Display error message on failure with helpful text

### 3.6 Data Model Requirements

**No schema changes required** - use existing `feed_items` table:
- `id`: Long (primary key)
- `link`: String (URL to match)
- `bookmarked`: Boolean (to set to true)

**Required DAO methods:**
- `getLinksOfBookmarks()`: Already exists (used by export)
- `setBookmarked(id, bookmarked)`: Already exists
- **NEW:** Find feed item ID by link URL

### 3.7 Performance Requirements

1. **PERF-1:** Handle up to 10,000 URLs in import file
2. **PERF-2:** Complete import within 30 seconds for 1,000 URLs
3. **PERF-3:** Do not block UI thread during import
4. **PERF-4:** Use batch database operations for efficiency
5. **PERF-5:** Provide progress feedback for large imports

### 3.8 Security Requirements

1. **SEC-1:** Validate file URI before attempting to read
2. **SEC-2:** Sanitize URLs to prevent SQL injection
3. **SEC-3:** Handle malicious file content gracefully
4. **SEC-4:** Respect Android file permissions

### 3.9 Internationalization Requirements

1. **I18N-1:** Add string resources for import UI elements
2. **I18N-2:** Support all existing app languages
3. **I18N-3:** Translate error messages appropriately
4. **I18N-4:** Translate success messages

---

## 4. Non-Functional Requirements

### 4.1 Compatibility

- **Android Version:** Must work on minSdk 29 (Android 10) and above
- **Backward Compatibility:** Import files exported from any previous app version
- **Forward Compatibility:** Export format should remain compatible

### 4.2 Reliability

- **No Data Loss:** Import should never delete existing bookmarks
- **Idempotent:** Multiple imports of same file should be safe
- **Robust:** Handle malformed files without crashing

### 4.3 Usability

- **Discoverability:** Import option should be visible near export
- **Feedback:** Clear indication of import progress and results
- **Error Messages:** User-friendly error messages with actionable guidance

---

## 5. Acceptance Criteria

### AC-1: Basic Import Functionality
Given a valid text file with URLs
When I select the file for import
Then all matching articles are marked as saved
And I see a success message with the count

### AC-2: File Format Validation
Given an invalid file format
When I attempt to import
Then I see an appropriate error message
And no bookmarks are modified

### AC-3: Duplicate Handling
Given an import file with duplicate URLs
When I import the file
Then each URL is processed only once
And no errors occur

### AC-4: Unmatched URLs
Given an import file with URLs not in database
When I import the file
Then unmatched URLs are skipped
And matched URLs are still imported
And I see the count of successfully imported articles

### AC-5: Empty File
Given an empty import file
When I attempt to import
Then I see an appropriate message
And no error occurs

### AC-6: Large File Performance
Given an import file with 1,000 URLs
When I import the file
Then the import completes within 30 seconds
And the UI remains responsive

### AC-7: Idempotent Operation
Given I import a file successfully
When I import the same file again
Then no duplicate bookmarks are created
And no errors occur

---

## 6. Out of Scope

The following are explicitly OUT OF SCOPE for this feature:

1. **Sync/Cloud Import:** No cloud sync or remote import functionality
2. **Batch Operations:** No bulk delete or bulk unbookmark from import
3. **Format Conversion:** No support for other export formats (only plain text)
4. **URL Normalization:** URLs must match exactly; no fuzzy matching
5. **Conflict Resolution:** No import conflict resolution UI
6. **Incremental Import:** No import scheduling or auto-import
7. **Export Format Changes:** No modifications to existing export format

---

## 7. Dependencies

### 7.1 Existing Components

1. **SavedArticlesExporter:** Reference for file format
2. **FeedItemDao:** Database access layer
3. **FeedScreen:** UI entry point
4. **Kodein DI:** Dependency injection
5. **ActivityResultContracts:** File picker

### 7.2 New Components Required

1. **SavedArticlesImporter:** New import logic class
2. **Import Error Types:** Sealed class for error handling
3. **String Resources:** UI text translations
4. **UI Integration:** Menu item and launcher

---

## 8. Technical Constraints

1. **Language:** Kotlin
2. **UI Framework:** Jetpack Compose
3. **Database:** Room
4. **DI:** Kodein
5. **Async:** Coroutines with Dispatchers.IO
6. **File Access:** Android ContentResolver
7. **Testing:** AndroidJUnit4 for instrumented tests

---

## 9. Success Metrics

1. **Functional:** All acceptance criteria pass
2. **Test Coverage:** Unit tests + integration test matching export test
3. **Performance:** Import 1,000 URLs in < 30 seconds
4. **Quality:** Zero crashes on malformed files
5. **Usability:** Clear user feedback for all scenarios

---

## 10. Open Questions

1. **Q1:** Should we add a confirmation dialog before import?
   - **A1:** No - user explicitly selects file, sufficient confirmation

2. **Q2:** Should we show a preview of URLs to be imported?
   - **A2:** No - out of scope for MVP, future enhancement

3. **Q3:** Should we import in a transaction for rollback on error?
   - **A3:** No - partial success is acceptable, better UX

4. **Q4:** What about articles that were bookmarked but later unbookmarked?
   - **A4:** Import will re-bookmark them - user can manually unbookmark again

---

## Document Status

**Version:** 1.0
**Last Updated:** 2026-01-07
**Next Review:** After implementation planning
