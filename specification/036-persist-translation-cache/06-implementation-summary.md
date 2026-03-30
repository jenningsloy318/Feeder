# Spec 036: Persist Translation Cache -- Implementation Summary

**Date:** 2026-03-30
**Branch:** `036-persist-translation-cache`
**Status:** Complete

---

## What Was Implemented

Disk-based caching of completed article translations so that re-opening an already-translated article loads the cached result instantly without re-calling the LLM. Cache files use gzip-compressed JSON stored in `filesDir/translations/`, following the existing `Blob.kt` pattern. Cache is keyed by `(itemId, languageCode)` and supports per-language coexistence, re-translation via `forceRefresh`, and cleanup on article deletion.

---

## Files Created

| File | Lines | Description |
|------|-------|-------------|
| `app/src/main/java/com/nononsenseapps/feeder/blob/TranslationBlob.kt` | 54 | Top-level functions: `translationFile()`, `loadTranslation()`, `saveTranslation()`, `deleteTranslationCache()` |
| `app/src/test/java/com/nononsenseapps/feeder/blob/TranslationBlobTest.kt` | 206 | 12 unit tests covering round-trip, corruption, directory creation, overwrite, multi-language, deletion, large articles, forward-compatible JSON |

## Files Modified

| File | Changes | Description |
|------|---------|-------------|
| `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt` | +6 lines | Added `translationsDir: File` to interface and `FilePathProviderImpl` (`filesDir.resolve("translations")`) |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | +44/-3 lines | Cache check at start of `translate()`, cache save on completion, `resolveLanguageCode()` helper, `forceRefresh` parameter, `cacheSkipOnNextTranslate` flag |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | +3/-1 lines | Pass `forceRefresh = isAlreadyTranslated` when re-translating |
| `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt` | +5 lines | Call `deleteTranslationCache()` in article deletion loop alongside existing blob cleanup |
| `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt` | +25 lines | New `cleanupTranslationsDirectory()` method for orphan cleanup (parses itemId from filename prefix, deletes files not in valid ID set) |

**Totals:** ~79 lines production code + 206 lines tests

---

## Key Design Decisions

1. **Flow B (cache loaded on translate tap):** Cache is checked when `translate()` is called, not on article open. This keeps the article loading path unchanged and avoids unnecessary I/O for articles the user doesn't intend to translate.

2. **Top-level functions (Option A):** `TranslationBlob.kt` uses top-level functions following the exact `Blob.kt` pattern -- no class, no DI dependency. Simple and consistent with the codebase.

3. **`filesDir/translations/` storage:** Uses `filesDir` (not `cacheDir`) so translation files are backup-eligible and survive app updates. Matches the `articleDir` pattern.

4. **`Json { ignoreUnknownKeys = true }`:** Forward compatibility -- future fields added to `ArticleTranslation` or `ParagraphTranslation` with defaults will not break old cached files.

5. **Dual re-translate mechanisms:** `forceRefresh` parameter for explicit UI re-translate (tap translate while in `Translated` state) and `cacheSkipOnNextTranslate` flag for cancel-then-retranslate flow. These serve different UX flows and cannot be collapsed.

6. **Separate `cleanupTranslationsDirectory()` method:** The existing `cleanupDirectory()` in `CleanupOrphanedFilesJob` uses a `fileProvider: (Long, File) -> File` callback for single-file-per-article cleanup. Translation files have a multi-file-per-article pattern (`{itemId}_{lang}`), requiring a distinct method that parses itemId from filename prefixes.

---

## Deviations from Original Spec

None. The implementation follows the specification exactly as written. All function signatures, file naming conventions, error handling strategies, and integration points match the spec.

---

## Test Coverage Summary

**New tests:** 12 in `TranslationBlobTest.kt`

| Test | Scenario |
|------|----------|
| `saveAndLoadRoundTrip_returnsIdenticalArticleTranslation` | SCENARIO-001, SCENARIO-002 |
| `loadTranslation_returnsNullWhenFileDoesNotExist` | SCENARIO-014 |
| `loadTranslation_returnsNullForCorruptedGzipFile` | SCENARIO-015 |
| `loadTranslation_returnsNullForInvalidJsonInValidGzip` | SCENARIO-015 |
| `saveTranslation_createsDirectoryIfNotExists` | SCENARIO-016 |
| `saveTranslation_overwritesExistingFile` | SCENARIO-012 |
| `multipleLanguagesForSameItemCoexistIndependently` | SCENARIO-005 |
| `deleteTranslationCache_removesAllLanguageFilesForItemId` | SCENARIO-010 |
| `deleteTranslationCache_doesNotRemoveFilesForOtherItemIds` | SCENARIO-010 |
| `deleteTranslationCache_isNoOpWhenDirectoryDoesNotExist` | Edge case |
| `largeArticleRoundTrip_worksCorrectly` | SCENARIO-021 |
| `fileWithUnknownJsonKeys_deserializesSuccessfully` | SCENARIO-024 |

**Build results:** 496 total tests, 12 new, 3 pre-existing failures (`CustomFeederTextToolbarTest`, `MenuConfigStoreTest` x2), 0 regressions.

---

## Review Verdicts

| Review | Verdict | Notes |
|--------|---------|-------|
| Code Review | **Approved with Comments** | Clean, minimal, follows spec. Minor observations on `MutableStateFlow.update` I/O and non-atomic writes. No blocking issues. |
| Adversarial Review | **CONTESTED** | F1 (non-atomic file write) accepted as tech debt -- matches existing `Blob.kt` pattern. F2 (I/O in CAS retry block) low risk due to single-writer guarantee. F3 (no content hash in cache key) out of scope. F4 (`cacheSkipOnNextTranslate` not `AtomicBoolean`) verified safe -- main thread only. |

**Tech debt items for follow-up:**
- F1: Add atomic write-to-temp-then-rename to both `Blob.kt` and `TranslationBlob.kt`
- F2: Move `saveTranslation()` call outside `MutableStateFlow.update {}` lambda
