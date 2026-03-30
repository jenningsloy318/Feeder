# Spec 036: Persist Translation Cache -- Task List

## Phase 1: Core Cache Functions (TDD)

### 1.1 Add `translationsDir` to FilePathProvider
- [ ] Add `val translationsDir: File` to `FilePathProvider` interface
- [ ] Add `override val translationsDir: File = filesDir.resolve("translations")` to `FilePathProviderImpl`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt`
- **Scenarios:** SCENARIO-001, SCENARIO-023, SCENARIO-024

### 1.2 Create TranslationBlob.kt with top-level functions
- [ ] Create `TranslationBlob.kt` in `blob/` package with:
  - `translationFile(itemId, languageCode, translationsDir)` -- returns `File`
  - `loadTranslation(itemId, languageCode, translationsDir)` -- returns `ArticleTranslation?`
  - `saveTranslation(itemId, languageCode, translationsDir, translation)` -- writes gzip JSON
  - `deleteTranslationCache(itemId, translationsDir)` -- deletes all `{itemId}_*` files
- [ ] Use `Json { ignoreUnknownKeys = true }` for forward compatibility
- **File:** `app/src/main/java/com/nononsenseapps/feeder/blob/TranslationBlob.kt` (new)
- **Scenarios:** SCENARIO-001, SCENARIO-002, SCENARIO-010, SCENARIO-014, SCENARIO-015, SCENARIO-016

### 1.3 Write unit tests for TranslationBlob functions (TDD -- write tests first)
- [ ] Test: `saveTranslation` then `loadTranslation` round-trip returns identical `ArticleTranslation` (SCENARIO-001, SCENARIO-002)
- [ ] Test: `loadTranslation` returns null when file doesn't exist (SCENARIO-014)
- [ ] Test: `loadTranslation` returns null for corrupted gzip file (SCENARIO-015)
- [ ] Test: `loadTranslation` returns null for invalid JSON in valid gzip (SCENARIO-015)
- [ ] Test: `saveTranslation` creates directory if it doesn't exist (SCENARIO-016)
- [ ] Test: `saveTranslation` overwrites existing file (SCENARIO-012 cache overwrite)
- [ ] Test: Multiple languages for same itemId coexist independently (SCENARIO-005)
- [ ] Test: `deleteTranslationCache` removes all language files for an itemId (SCENARIO-010)
- [ ] Test: `deleteTranslationCache` does not remove files for other itemIds
- [ ] Test: `deleteTranslationCache` is no-op when directory doesn't exist
- [ ] Test: Large article (120 paragraphs) round-trip works correctly (SCENARIO-021)
- [ ] Test: File with unknown JSON keys deserializes successfully (SCENARIO-024 forward compat)
- **File:** `app/src/test/java/com/nononsenseapps/feeder/blob/TranslationBlobTest.kt` (new)

### 1.4 Build verification
- [ ] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [ ] Run `./gradlew :app:testFdroidDebugUnitTest` -- new tests must pass

---

## Phase 2: ViewModel Integration (Cache Check + Save)

### 2.1 Add language code resolution helper
- [ ] Add `resolveLanguageCode(language: TranslationLanguage): String` private function
  - Returns `language.code` if non-empty, otherwise `Locale.getDefault().language`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-004, SCENARIO-019

### 2.2 Add cache check at start of translate()
- [ ] Add `forceRefresh: Boolean = false` parameter to `translate()`
- [ ] Add `private var cacheSkipOnNextTranslate = false` field
- [ ] Move `repository.translationLanguage.first()` before paragraph extraction
- [ ] Call `resolveLanguageCode()` to get effective language code
- [ ] Call `loadTranslation()` when `!forceRefresh && !cacheSkipOnNextTranslate`
- [ ] On cache hit: set `TranslationState.Translated` directly and return (no `Translating` state)
- [ ] Clear `cacheSkipOnNextTranslate` flag after reading it
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-002, SCENARIO-003, SCENARIO-006, SCENARIO-017, SCENARIO-020

### 2.3 Add cache save after translation completes
- [ ] Capture `languageCode` variable before the `collect` block
- [ ] After `updatedArticleTranslation.status == "translated"`, call `saveTranslation()`
- [ ] Wrap save in try-catch (log warning on failure, don't fail translation)
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-001, SCENARIO-006, SCENARIO-014, SCENARIO-018

### 2.4 Add re-translate support
- [ ] Modify `cancelTranslation()` to set `cacheSkipOnNextTranslate = true`
- [ ] Update UI caller for "translate while Translated" to pass `forceRefresh = true`
- **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
- **Scenarios:** SCENARIO-012, SCENARIO-013

### 2.5 Build verification
- [ ] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [ ] Run `./gradlew :app:testFdroidDebugUnitTest` -- all existing tests must pass

---

## Phase 3: Cleanup Integration

### 3.1 Add translation cache cleanup in RssLocalSync article deletion loop
- [ ] Import `deleteTranslationCache` from `blob` package
- [ ] Add `deleteTranslationCache(itemId = id, translationsDir = filePathProvider.translationsDir)` call inside `for (id in articlesToDelete)` loop, after existing blob deletions
- **File:** `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt` (lines 417-441)
- **Scenarios:** SCENARIO-010, SCENARIO-011

### 3.2 Add translation directory cleanup in CleanupOrphanedFilesJob
- [ ] Add `cleanupTranslationsDirectory(directory, validIds)` private method
  - Parses `itemId` from filename prefix (before `_`)
  - Deletes files whose `itemId` is not in the valid set
- [ ] Call `cleanupTranslationsDirectory(filePathProvider.translationsDir, validFeedItemIds.toSet())` in `doWork()` after existing cleanup calls
- **File:** `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt`
- **Scenarios:** SCENARIO-010 (orphaned files edge case)

### 3.3 Build verification
- [ ] Run `./gradlew :app:compileFdroidDebugKotlin` -- must pass
- [ ] Run `./gradlew :app:testFdroidDebugUnitTest` -- all tests must pass

---

## Phase 4: Integration Testing + Edge Cases

### 4.1 Verify translate() UI call sites pass forceRefresh correctly
- [ ] Find all call sites of `translate()` in composables
- [ ] Ensure "re-translate" action (tap translate while in Translated state) passes `forceRefresh = true`
- [ ] Ensure auto-translate path uses default `forceRefresh = false`
- **Scenarios:** SCENARIO-012, SCENARIO-020

### 4.2 Verify error states don't write cache
- [ ] Confirm: `TranslationState.Error` path does not call `saveTranslation()` (SCENARIO-007)
- [ ] Confirm: cancellation path does not call `saveTranslation()` (SCENARIO-008, SCENARIO-009)
- [ ] Confirm: only `status == "translated"` triggers cache save (AC-004)

### 4.3 Full test suite verification
- [ ] Run `./gradlew :app:testFdroidDebugUnitTest` -- all tests pass (including new TranslationBlobTest)
- [ ] Run `./gradlew :app:compileFdroidDebugKotlin` -- build succeeds
- [ ] Verify 3 known broken tests are still the only failures: `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` (2 tests)

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
