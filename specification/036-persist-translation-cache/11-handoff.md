# Spec 036: Persist Translation Cache -- Session Handoff

## 1. Session Identity

- **Spec:** 036-persist-translation-cache
- **Date:** 2026-03-30
- **Branch:** `036-persist-translation-cache` (merged into `ai-features`)
- **Worktree:** `.worktree/036-persist-translation-cache/`
- **Status:** COMPLETE

---

## 2. What Was Done

Implemented translation cache persistence so that completed article translations are saved as gzip-compressed JSON files on disk. Re-opening a previously translated article loads the cached result instantly instead of re-sending every paragraph to the LLM, saving API costs and providing immediate translated content.

Key implementation points:
- **TranslationBlob.kt** -- new file with top-level functions (`saveTranslation`, `loadTranslation`, `deleteTranslationCache`, `translationFile`) following the existing `Blob.kt` pattern
- **FilePathProvider** -- added `translationsDir` property pointing to `filesDir/translations/` (backup-eligible via Android Auto Backup)
- **ArticleViewModel** -- integrated cache check at start of `translate()` with `forceRefresh` parameter, cache save after translation completes, `cacheSkipOnNextTranslate` flag for cancel-then-retranslate flow, and `resolveLanguageCode()` helper for DEVICE_DEFAULT locale resolution
- **RssLocalSync** -- added `deleteTranslationCache()` call in the article deletion loop alongside existing blob file cleanup
- **CleanupOrphanedFilesJob** -- added `cleanupTranslationsDirectory()` method to handle orphaned translation files (separate from existing `cleanupDirectory()` because translations have a one-to-many itemId-to-file relationship)
- **ArticleScreen** -- updated re-translate UI call to pass `forceRefresh = true`
- **12 unit tests** in `TranslationBlobTest.kt` covering round-trip, corruption handling, directory creation, overwrite, multi-language coexistence, deletion, large articles, and forward-compatible JSON deserialization

---

## 3. Key Decisions Made

| Decision | Chosen Option | Rationale |
|----------|--------------|-----------|
| Cache loading timing | **Flow B**: cache loaded on translate tap, not auto-load on article open | Simpler implementation; avoids unnecessary I/O on every article open |
| Code structure | **Option A**: top-level functions (not class-based) | Matches existing `Blob.kt` pattern exactly |
| Storage location | `filesDir/translations/` | Backup-eligible for Android Auto Backup; survives app updates (unlike `cacheDir`) |
| Cache key format | `{itemId}_{langCode}.translation.json.gz` | Includes language to prevent cross-language staleness |
| Re-translate UX | `forceRefresh` param + `cacheSkipOnNextTranslate` flag | Two distinct flows: tap-translate-while-Translated vs cancel-then-retranslate; cannot be collapsed without changing API contract |
| Non-atomic write | Accepted as tech debt | Matches existing `Blob.kt` pattern; crash mid-save handled by `loadTranslation` returning null on corruption |
| JSON deserialization | `Json { ignoreUnknownKeys = true }` | Forward compatibility -- future fields added to `ArticleTranslation` with defaults won't break old cached files |

---

## 4. Files Changed

| File | Change Type | Description |
|------|-------------|-------------|
| `app/src/main/java/com/nononsenseapps/feeder/blob/TranslationBlob.kt` | **New** | ~55 lines. Top-level functions: `translationFile`, `loadTranslation`, `saveTranslation`, `deleteTranslationCache` |
| `app/src/main/java/com/nononsenseapps/feeder/util/FilePathProvider.kt` | Modified | Added `translationsDir: File` to interface and `FilePathProviderImpl` (`filesDir.resolve("translations")`) |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | Modified | Cache check in `translate()`, cache save after completion, `forceRefresh` param, `cacheSkipOnNextTranslate` flag, `resolveLanguageCode()` helper |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | Modified | Re-translate action passes `forceRefresh = true` to `translate()` |
| `app/src/main/java/com/nononsenseapps/feeder/model/RssLocalSync.kt` | Modified | Added `deleteTranslationCache()` call in article deletion loop (lines ~442-445) |
| `app/src/main/java/com/nononsenseapps/feeder/background/CleanupOrphanedFilesJob.kt` | Modified | Added `cleanupTranslationsDirectory()` method and call in `doWork()` |
| `app/src/test/java/com/nononsenseapps/feeder/blob/TranslationBlobTest.kt` | **New** | 12 unit tests for all TranslationBlob functions |

---

## 5. Unfinished Items / Follow-ups

| Item | Status | Notes |
|------|--------|-------|
| Cache eviction policy (LRU/time-based) | Explicitly out of scope for v1 | Translation files are small (~2-5 KB compressed) and bounded by article count in DB |
| Summary caching | Separate feature, not implemented | Could follow same TranslationBlob pattern with a `SummaryBlob.kt` |
| ViewModel unit tests for cache integration | Not added | Would require mocking `FilePathProvider`, `Repository`, etc.; core blob functions are well-tested |
| Non-atomic write (F1 from adversarial review) | Accepted tech debt | Matches `Blob.kt` pattern; write-to-temp-then-rename would be safer but inconsistent with existing code |
| `saveTranslation()` inside `MutableStateFlow.update {}` (F2) | Accepted tech debt | Idempotent; single-writer guarantee from `translateJob?.cancel()` prevents CAS retry in practice |
| Content hash in cache key | Out of scope | If article body updates upstream, cached translation may be stale; `forceRefresh` is the manual escape hatch |

---

## 6. Testing Status

- **Build:** SUCCESSFUL (`./gradlew :app:compileFdroidDebugKotlin`)
- **Tests:** 496 total, 12 new in `TranslationBlobTest.kt`, 3 pre-existing failures (unchanged), 0 regressions
  - Pre-existing failures: `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` (2 tests)
- **BDD Coverage:** 24 scenarios total
  - 8 fully covered by automated unit tests (001, 005, 010, 014, 015, 016, 021, 024)
  - 3 partially covered by unit tests + code review (002, 006, 012)
  - 11 verified by code review (003, 007, 008, 009, 011, 013, 017, 018, 019, 020, 022)
  - 2 covered by Android platform guarantees (023, 024)
- **Code Review:** Approved with Comments
- **Adversarial Review:** CONTESTED -> accepted (F1 non-atomic write acknowledged as tech debt matching existing pattern)

---

## 7. Risks and Warnings

| Risk | Severity | Detail |
|------|----------|--------|
| DEVICE_DEFAULT locale change | Low | `resolveLanguageCode()` resolves at runtime. If device locale changes between save and load, cache key won't match -- correctly falls back to LLM, but old cache file becomes orphaned until cleanup job runs |
| Non-atomic write | Low | Crash mid-save could leave corrupted file. Handled by `loadTranslation()` returning null (catch-all exception handler), falling back to LLM. No data loss beyond one cache entry |
| No migration needed | N/A | First-use gracefully falls back to LLM when no cache file exists. Articles translated before this feature simply have no cache. |
| `saveTranslation()` has no internal try-catch | Low | Callers must wrap in try-catch. Currently only called from `ArticleViewModel` which does wrap it. Future call sites could throw if not wrapped. Consider `@Throws(IOException::class)` annotation. |

---

## First Steps for Next Agent

- **If adding summary caching:** Follow the same `TranslationBlob.kt` pattern. Create `SummaryBlob.kt` with `saveSummary`/`loadSummary`/`deleteSummaryCache`. Add `summariesDir` to `FilePathProvider`. Integrate in `ArticleViewModel.summarize()`.
- **If adding cache eviction:** Add age check in `loadTranslation()` or a background cleanup job that deletes files older than N days. Alternatively, add LRU tracking via a small index file.
- **If adding auto-load on article open:** Move the cache check from `translate()` to `init {}` in `ArticleViewModel`. Set `TranslationState.Translated` before the UI renders if cache exists.
- **If adding atomic writes:** Implement write-to-temp-then-rename in both `TranslationBlob.kt` and `Blob.kt` for consistency. Use `File.renameTo()` after writing to a `.tmp` file in the same directory.
- **If adding content-hash cache invalidation:** Include a hash of the article body in the cache key or as metadata in the JSON. Compare on load; invalidate if mismatch.
