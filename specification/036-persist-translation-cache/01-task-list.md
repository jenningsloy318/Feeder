# Spec 036: Persist Translation Cache -- Task List

## Phase 1: Core Cache Functions (TDD)

### 1.1 Add `translationsDir` to FilePathProvider
- [x] Add `val translationsDir: File` to `FilePathProvider` interface
- [x] Add `override val translationsDir: File = filesDir.resolve("translations")` to `FilePathProviderImpl`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt`
- **Scenarios:** SCENARIO-001, SCENARIO-023, SCENARIO-024

### 1.2 Create TranslationBlob.kt with top-level functions
- [x] Create `TranslationBlob.kt` in `blob/` package with:
  - `translationFile(itemId, languageCode, translationsDir)` -- returns `File`
  - `loadTranslation(itemId, languageCode, translationsDir)` -- returns `ArticleTranslation?`
  - `saveTranslation(itemId, languageCode, translationsDir, translation)` -- writes gzip JSON
  - `deleteTranslationCache(itemId, translationsDir)` -- deletes all `{itemId}_*` files
- [x] Use `Json { ignoreUnknownKeys = true }` for forward compatibility
- **File:** `app/src/main/java/com/nononsenseapps/feeder/blob/TranslationBlob.kt` (new)
- **Scenarios:** SCENARIO-001, SCENARIO-002, SCENARIO-010, SCENARIO-014, SCENARIO-015, SCENARIO-016

### 1.3 Write unit tests for TranslationBlob functions (TDD -- write tests first)
- [x] Test: `saveTranslation` then `loadTranslation` round-trip returns identical `ArticleTranslation` (SCENARIO-001, SCENARIO-002)
- [x] Test: `loadTranslation` returns null when file doesn't exist (SCENARIO-014)
- [x] Test: `loadTranslation` returns null for corrupted gzip file (SCENARIO-015)
- [x] Test: `loadTranslation` returns null for invalid JSON in valid gzip (SCENARIO-015)
- [x] Test: `saveTranslation` creates directory if it doesn't exist (SCENARIO-016)
- [x] Test: `saveTranslation` overwrites existing file (SCENARIO-012 cache overwrite)
- [x] Test: Multiple languages for same itemId coexist independently (SCENARIO-005)
- [x] Test: `deleteTranslationCache` removes all language files for an itemId (SCENARIO-010)
- [x] Test: `deleteTranslationCache` does not remove files for other itemIds
- [x] Test: `deleteTranslationCache` is no-op when directory doesn't exist
- [x] Test: Large article (120 paragraphs) round-trip works correctly (SCENARIO-021)
- [x] Test: File with unknown JSON keys deserializes successfully (SCENARIO-024 forward compat)
- **File:** `app/src/test/java/com/nononsenseapps/feeder/blob/TranslationBlobTest.kt` (new)

### 1.4 Build verification
- [x] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [x] Run `./gradlew :app:testFdroidDebugUnitTest` -- new tests must pass

---

## Phase 2: ViewModel Integration (Cache Check + Save)

### 2.1 Add language code resolution helper
- [x] Add `resolveLanguageCode(language: TranslationLanguage): String` private function
  - Returns `language.code` if non-empty, otherwise `Locale.getDefault().language`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-004, SCENARIO-019

### 2.2 Add cache check at start of translate()
- [x] Add `forceRefresh: Boolean = false` parameter to `translate()`
- [x] Add `private var cacheSkipOnNextTranslate = false` field
- [x] Move `repository.translationLanguage.first()` before paragraph extraction
- [x] Call `resolveLanguageCode()` to get effective language code
- [x] Call `loadTranslation()` when `!forceRefresh && !cacheSkipOnNextTranslate`
- [x] On cache hit: set `TranslationState.Translated` directly and return (no `Translating` state)
- [x] Clear `cacheSkipOnNextTranslate` flag after reading it
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-002, SCENARIO-003, SCENARIO-006, SCENARIO-017, SCENARIO-020

### 2.3 Add cache save after translation completes
- [x] Capture `languageCode` variable before the `collect` block
- [x] After `updatedArticleTranslation.status == "translated"`, call `saveTranslation()`
- [x] Wrap save in try-catch (log warning on failure, don't fail translation)
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-001, SCENARIO-006, SCENARIO-014, SCENARIO-018

### 2.4 Add re-translate support
- [x] Modify `cancelTranslation()` to set `cacheSkipOnNextTranslate = true`
- [x] Update UI caller for "translate while Translated" to pass `forceRefresh = true`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-012, SCENARIO-013

### 2.5 Build verification
- [x] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [x] Run `./gradlew :app:testFdroidDebugUnitTest` -- all existing tests must pass

---

## Phase 3: Cleanup Integration

### 3.1 Add translation cache cleanup in RssLocalSync article deletion loop
- [x] Import `deleteTranslationCache` from `blob` package
- [x] Add `deleteTranslationCache(itemId = id, translationsDir = filePathProvider.translationsDir)` call inside `for (id in articlesToDelete)` loop, after existing blob deletions
- **File:** `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt` (lines 417-441)
- **Scenarios:** SCENARIO-010, SCENARIO-011

### 3.2 Add translation directory cleanup in CleanupOrphanedFilesJob
- [x] Add `cleanupTranslationsDirectory(directory, validIds)` private method
  - Parses `itemId` from filename prefix (before `_`)
  - Deletes files whose `itemId` is not in the valid set
- [x] Call `cleanupTranslationsDirectory(filePathProvider.translationsDir, validFeedItemIds.toSet())` in `doWork()` after existing cleanup calls
- **File:** `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt`
- **Scenarios:** SCENARIO-010 (orphaned files edge case)

### 3.3 Build verification
- [x] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [x] Run `./gradlew :app:testFdroidDebugUnitTest` -- all tests must pass

---

## Phase 4: Integration Testing + Edge Cases

### 4.1 Verify translate() UI call sites pass forceRefresh correctly
- [x] Find all call sites of `translate()` in composables
- [x] Ensure "re-translate" action (tap translate while in Translated state) passes `forceRefresh = true`
- [x] Ensure auto-translate path uses default `forceRefresh = false`
- **Scenarios:** SCENARIO-012, SCENARIO-020

### 4.2 Verify error states don't write cache
- [x] Confirm: `TranslationState.Error` path does not call `saveTranslation()` (SCENARIO-007)
- [x] Confirm: cancellation path does not call `saveTranslation()` (SCENARIO-008, SCENARIO-009)
- [x] Confirm: only `status == "translated"` triggers cache save (AC-004)

### 4.3 Full test suite verification
- [x] Run `./gradlew :app:testFdroidDebugUnitTest` -- all tests pass (including new TranslationBlobTest)
- [x] Run `./gradlew :app:compileFdroidDebugKotlin` -- build succeeds
- [x] Verify 3 known broken tests are still the only failures: `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` (2 tests)

---

## Summary

| Phase | New Files | Modified Files | Test Files |
|-------|-----------|----------------|------------|
| 1 | `blob/TranslationBlob.kt` | `util/FilePathProvider.kt` | `blob/TranslationBlobTest.kt` |
| 2 | -- | `feedarticle/ArticleViewModel.kt` | -- |
| 3 | -- | `model/RssLocalSync.kt`, `background/CleanupOrphanedFilesJob.kt` | -- |
| 4 | -- | Possibly composable call sites | -- |

**Total new files:** 2 (TranslationBlob.kt + TranslationBlobTest.kt)
**Total modified files:** 4 (FilePathProvider, ArticleViewModel, RssLocalSync, CleanupOrphanedFilesJob)
**Estimated new code:** ~50 lines production + ~150 lines tests
