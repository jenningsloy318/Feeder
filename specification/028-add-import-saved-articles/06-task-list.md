# Task List: Import Saved Articles Feature

**Spec ID:** 028-add-import-saved-articles
**Date:** 2026-01-07
**Total Tasks:** 10
**Estimated Time:** 7.5 hours

---

## Task Status Legend

- [ ] **TODO** - Not started
- [x] **DONE** - Completed
- [~] **IN PROGRESS** - Currently working
- [!] **BLOCKED** - Blocked by dependency

---

## Phase A: Core Functionality

### Task A1: Add Database Methods
**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItemDao.kt`
**Priority:** HIGH
**Time:** 30 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Add `FeedItemIdLinkPair` data class
- [ ] Add `getFeedItemIdsByLinks()` @Query method
- [ ] Add `setBookmarked(ids: List<Long>)` @Query method
- [ ] Add KDoc comments for new methods
- [ ] Run database tests to verify

**Acceptance Criteria:**
- Code compiles without errors
- Database tests pass
- No ktlint violations
- Methods have proper KDoc comments

**Dependencies:** None

---

### Task A2: Create Import Logic
**File:** `app/src/main/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporter.kt` (NEW)
**Priority:** HIGH
**Time:** 1.5 hours
**Status:** [ ] TODO

**Subtasks:**
- [ ] Create new file with package declaration
- [ ] Add error sealed class hierarchy
- [ ] Add `ImportResult` data class
- [ ] Implement `importSavedArticles()` function
- [ ] Implement `readUrlsFromFile()` helper function
- [ ] Add Either monad error handling
- [ ] Add logging with performance timing
- [ ] Add KDoc comments for all public APIs

**Acceptance Criteria:**
- Code compiles without errors
- Follows OPML import pattern from `OpmlActions.kt`
- Error handling covers all cases (file not found, read error, etc.)
- Proper use of coroutines with `Dispatchers.IO`
- Performance timing logged
- KDoc comments on all public functions

**Dependencies:** A1

---

### Task A3: Add Basic String Resources
**File:** `app/src/main/res/values/strings.xml`
**Priority:** HIGH
**Time:** 15 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Add `import_saved_articles` string
- [ ] Add `failed_to_import_saved_articles` string
- [ ] Add `imported_n_saved_articles` string
- [ ] Add `imported_n_of_m_saved_articles` string
- [ ] Add `no_valid_urls_in_file` string
- [ ] Add `import_file_not_found` string
- [ ] Add `failed_to_read_import_file` string
- [ ] Add `error_no_file_manager` string
- [ ] Add `no_matching_articles_found` string
- [ ] Add plurals for count messages

**Acceptance Criteria:**
- All required strings added
- Proper string formatting (%1$s, %2$s, etc.)
- Plurals defined correctly
- No string ID conflicts

**Dependencies:** None

---

## Phase B: UI Integration

### Task B1: Add UI Launcher
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`
**Priority:** HIGH
**Time:** 20 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Add import statement: `import com.nononsenseapps.feeder.model.export.importSavedArticles`
- [ ] Add `savedArticleImporter` launcher (after `opmlImporter`)
- [ ] Configure launcher with `ActivityResultContracts.OpenDocument()`
- [ ] Add coroutine scope handling with `ApplicationCoroutineScope`
- [ ] Add error toast handling

**Acceptance Criteria:**
- Code compiles without errors
- Launcher pattern matches `opmlImporter`
- Proper coroutine scope usage
- Error handling in place

**Dependencies:** A2

---

### Task B2: Add Menu Item
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`
**Priority:** HIGH
**Time:** 20 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Locate export menu item (around line 355)
- [ ] Add import menu item nearby
- [ ] Add appropriate icon (import icon)
- [ ] Add `onClick` handler that calls `savedArticleImporter.launch()`
- [ ] Add error handling for `ActivityNotFoundException`

**Acceptance Criteria:**
- Menu item visible in UI settings
- Menu item text displays correctly
- Clicking opens file picker
- Error handling works when no file manager available

**Dependencies:** B1

---

## Phase C: Testing

### Task C1: Write Integration Test
**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/model/export/ImportSavedTest.kt` (NEW)
**Priority:** HIGH
**Time:** 1 hour
**Status:** [ ] TODO

**Subtasks:**
- [ ] Create test file matching `ExportSavedTest.kt` structure
- [ ] Setup DI with in-memory database
- [ ] Test basic import (3 URLs, 2 match, 1 not found)
- [ ] Test duplicate URL handling
- [ ] Test empty file handling
- [ ] Test invalid URL handling
- [ ] Test file read error handling

**Acceptance Criteria:**
- All tests pass
- Test coverage matches export tests
- Edge cases covered (empty, duplicates, invalid)
- Proper assertions

**Dependencies:** A2

---

### Task C2: Manual Testing
**Priority:** HIGH
**Time:** 30 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Build debug APK: `./gradlew assembleDebug`
- [ ] Install on test device/emulator
- [ ] Export saved articles from device
- [ ] Import the exported file
- [ ] Verify all articles are bookmarked
- [ ] Test invalid file error handling
- [ ] Test empty file error handling
- [ ] Test performance with 100+ URLs

**Acceptance Criteria:**
- Basic import works end-to-end
- Error messages display correctly
- Performance is acceptable (< 2s for 100 URLs)
- No crashes or ANRs

**Dependencies:** B2

---

## Phase D: Polish

### Task D1: Add Translations
**Files:** `app/src/main/res/values-*/strings.xml` (30+ files)
**Priority:** MEDIUM
**Time:** 2 hours
**Status:** [ ] TODO

**Subtasks:**
For each language file (values-es, values-fr, values-de, etc.):
- [ ] Add `import_saved_articles`
- [ ] Add `failed_to_import_saved_articles`
- [ ] Add `imported_n_saved_articles`
- [ ] Add `imported_n_of_m_saved_articles`
- [ ] Add `no_valid_urls_in_file`
- [ ] Add `import_file_not_found`
- [ ] Add `failed_to_read_import_file`
- [ ] Add `error_no_file_manager`
- [ ] Add `no_matching_articles_found`

**Acceptance Criteria:**
- All language files updated
- No missing string IDs
- Proper formatting maintained
- Translations are contextually appropriate

**Dependencies:** A3

**Note:** Can ship with English only initially

---

### Task D2: Add Unit Tests
**File:** `app/src/test/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporterTest.kt` (NEW)
**Priority:** MEDIUM
**Time:** 30 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Test URL validation logic
- [ ] Test duplicate URL removal
- [ ] Test empty line filtering
- [ ] Test whitespace trimming
- [ ] Test file read error handling

**Acceptance Criteria:**
- All unit tests pass
- Edge cases covered
- Proper assertions

**Dependencies:** A2

---

### Task D3: Update Documentation
**Files:** `CHANGELOG.md`, `README.md`
**Priority:** MEDIUM
**Time:** 15 minutes
**Status:** [ ] TODO

**Subtasks:**
- [ ] Add entry to `CHANGELOG.md`
- [ ] Update feature list in `README.md` if applicable
- [ ] Add any user-facing documentation

**Acceptance Criteria:**
- CHANGELOG updated with feature description
- Follows existing CHANGELOG format
- Clear description of new feature

**Dependencies:** None

---

## Task Summary

| ID | Task | Priority | Time | Status | Dependencies |
|----|------|----------|------|--------|--------------|
| A1 | Database Methods | HIGH | 30m | [ ] TODO | None |
| A2 | Import Logic | HIGH | 1.5h | [ ] TODO | A1 |
| A3 | String Resources | HIGH | 15m | [ ] TODO | None |
| B1 | UI Launcher | HIGH | 20m | [ ] TODO | A2 |
| B2 | Menu Item | HIGH | 20m | [ ] TODO | B1 |
| C1 | Integration Test | HIGH | 1h | [ ] TODO | A2 |
| C2 | Manual Test | HIGH | 30m | [ ] TODO | B2 |
| D1 | Translations | MED | 2h | [ ] TODO | A3 |
| D2 | Unit Tests | MED | 30m | [ ] TODO | A2 |
| D3 | Documentation | MED | 15m | [ ] TODO | None |

**Total Time:** 7.5 hours
**Critical Path:** A1 → A2 → B1 → B2 → C1 → C2 (4.75 hours)
**Minimum Viable:** A1, A2, A3, B1, B2 (3.25 hours)

---

## Progress Tracking

**Completed Tasks:** 0/10 (0%)
**In Progress:** 0 tasks
**Blocked:** 0 tasks
**Time Spent:** 0 hours
**Time Remaining:** 7.5 hours

---

## Next Steps

1. **Start:** Task A1 (Add Database Methods)
2. **Continue:** Follow critical path A1 → A2 → B1 → B2 → C1 → C2
3. **Polish:** Add D1, D2, D3 as time permits

---

**Document Status:** READY FOR IMPLEMENTATION
**Version:** 1.0
**Last Updated:** 2026-01-07
