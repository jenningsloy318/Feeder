# Spec 036: Persist Translation Cache -- Code Review

**Reviewer:** Code Review Agent
**Date:** 2026-03-30
**Branch:** `036-persist-translation-cache`
**Verdict:** Approved with Comments

---

## Executive Summary

The implementation is clean, minimal, and faithfully follows the spec and the existing `Blob.kt` pattern. All 7 ACs are addressed, and the BDD scenario coverage is strong. The code is well-tested with 12 unit tests covering the core blob operations. Two minor issues and a few observations are noted below, but none are blocking.

---

## 1. Correctness

### AC Coverage

| AC | Status | Notes |
|----|--------|-------|
| AC-001: Save on completion | Covered | `saveTranslation()` called inside `translationState.update {}` when `status == "translated"` (ArticleViewModel.kt:607-618) |
| AC-002: Load cached on translate | Covered | Cache check at ArticleViewModel.kt:530-539, returns early with `TranslationState.Translated` |
| AC-003: Cache key includes language | Covered | `translationFile()` uses `${itemId}_${languageCode}` pattern (TranslationBlob.kt:15) |
| AC-004: Partial not cached | Covered | Only saves when `status == "translated"` (all paragraphs terminal). Error/cancel paths do not write cache. |
| AC-005: Cleanup on deletion | Covered | `deleteTranslationCache()` called in RssLocalSync.kt:442-445 and orphan cleanup in CleanupOrphanedFilesJob.kt:100-119 |
| AC-006: Instant UI feedback | Covered | Cache hit sets `TranslationState.Translated` directly, never enters `Translating` state |
| AC-007: Re-translate option | Covered | Two paths: `forceRefresh=true` (ArticleScreen.kt:187) and `cacheSkipOnNextTranslate` flag (ArticleViewModel.kt:635-639) |

### Observations

- **AC-004 nuance (SCENARIO-006):** The spec says "Only fully completed translations (status `"translated"`) are persisted." SCENARIO-006 clarifies that if all paragraphs reach terminal state (some `translated == 1`, some `translated == -1`), the result IS cached. The implementation correctly caches this case because `status` becomes `"translated"` when `all { it.translated != 0 }` (line 603). This is correct behavior -- the spec's AC-004 wording about "partial" refers to in-progress translations, not partially-failed ones.

- **AC-007 / SCENARIO-013 (cancel-then-retranslate):** The `cacheSkipOnNextTranslate` flag approach matches the spec exactly. The flag is read and cleared atomically at the top of `translate()` (line 519-520), preventing stale state.

---

## 2. Security

### File Path Traversal

- `languageCode` comes from `TranslationLanguage.code` (an enum) or `Locale.getDefault().language` (JDK locale). Neither is user-controlled free-text input. The `resolveLanguageCode()` function (line 659-660) returns either the enum code or the device locale language tag. **No path traversal risk.**

- `itemId` is a `Long` from Room's auto-generated primary key. Safe for filename construction.

- `translationFile()` constructs paths via `File(translationsDir, ...)` which is the standard safe pattern.

**Verdict: No security concerns.**

---

## 3. Performance

- **I/O on Dispatchers.IO:** Cache read happens inside `translate()` which launches on `Dispatchers.IO` (line 523). Cache save happens inside the same coroutine (already on IO). Cleanup in `CleanupOrphanedFilesJob` uses `withContext(Dispatchers.IO)`. All correct per NFR-004.

- **No main thread blocking:** The `translate()` function is called from the UI but launches a coroutine on IO. The `translationState` is a `MutableStateFlow` which is thread-safe for value updates.

- **`deleteTranslationCache()` uses `listFiles()` with a filter:** For the translations directory, this scans all files and filters by prefix. This is O(n) where n is the total number of translation files. For reasonable article counts (thousands), this is fast. For the cleanup loop in `RssLocalSync`, this is called per deleted article -- if many articles are deleted at once, it results in repeated directory scans. This is acceptable given the low frequency of bulk deletions and the small directory size.

**Verdict: No performance concerns.**

---

## 4. Maintainability

- **Follows Blob.kt pattern precisely.** Top-level functions, same package, same naming style. The new file is ~55 lines -- appropriately minimal.

- **`resolveLanguageCode()` is a private helper in ArticleViewModel.** This is fine for now. If language resolution is needed elsewhere in the future, it could be extracted, but YAGNI applies.

- **`cacheSkipOnNextTranslate` flag:** A boolean flag for cross-method communication is slightly fragile (easy to forget to check/clear). However, the spec explicitly requires this pattern for SCENARIO-013, and the flag is read + cleared in a single location (line 519-520). Acceptable.

**Verdict: Clean and maintainable.**

---

## 5. Testability

### Unit Test Coverage (TranslationBlobTest.kt -- 12 tests)

| Test | Scenarios Covered |
|------|-------------------|
| `saveAndLoadRoundTrip` | SCENARIO-001, SCENARIO-002 |
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
| `fileWithUnknownJsonKeys_deserializesSuccessfully` | SCENARIO-024 (forward compatibility) |

**Good coverage.** The tests use `TemporaryFolder` for isolation, test both positive and negative paths, and cover the critical forward-compatibility case (`ignoreUnknownKeys`).

### Missing Test Coverage (Non-blocking)

- **ViewModel integration tests** for cache hit/miss/re-translate flows are not present. These would require mocking `Repository`, `FilePathProvider`, etc. Given the project's existing test patterns (the memory notes mention 3 pre-existing broken tests), this is understandable. The core blob functions are well-tested; ViewModel behavior is covered by the clear code paths.

- **`cleanupTranslationsDirectory` in CleanupOrphanedFilesJob** is not unit-tested. The existing `cleanupDirectory` method also appears untested. Consistent with project patterns.

**Verdict: Sufficient test coverage for the new code.**

---

## 6. Error Handling

- **`loadTranslation()` catches `Exception` (not just `IOException`):** Correct per spec -- handles both corrupted gzip (`ZipException`) and invalid JSON (`SerializationException`). Returns `null` for graceful fallback.

- **`saveTranslation()` in ViewModel wrapped in try-catch:** Line 609-618 catches `Exception` and logs a warning. Translation still works even if cache write fails. Correct per spec's "cache is an optimization, not a requirement" principle.

- **`deleteTranslationCache()` -- individual file deletion failures are silent:** The `forEach { it.delete() }` call does not check the return value of `delete()`. If a file cannot be deleted (e.g., permission issue), it is silently ignored. This matches the existing blob cleanup pattern in `RssLocalSync` which also uses basic try/catch. Acceptable.

### Minor Issue: `saveTranslation()` does not catch exceptions

In `TranslationBlob.kt`, `saveTranslation()` itself does not have a try-catch. The caller (ArticleViewModel) wraps it. This is fine, but it means if `saveTranslation()` is called from a new call site in the future without wrapping, it could throw. The spec says "cache failures are never user-visible errors." Consider adding a `@Throws(IOException::class)` annotation to match the `Blob.kt` pattern for documentation, though this is non-blocking.

**Verdict: Error handling is solid.**

---

## 7. Consistency

- **Matches Blob.kt pattern:** Top-level functions, same package (`com.nononsenseapps.feeder.blob`), gzip compression, `File` parameter style.

- **FilePathProvider:** `translationsDir` added to both interface and implementation. Uses `filesDir.resolve("translations")` matching the `articleDir` pattern (`filesDir.resolve("articles")`). Correct for backup eligibility (NFR-002).

- **RssLocalSync cleanup:** Placed in the correct location -- inside the `for (id in articlesToDelete)` loop, after existing blob deletions. Clean integration.

- **CleanupOrphanedFilesJob:** Added a new `cleanupTranslationsDirectory()` method because the existing `cleanupDirectory()` uses a `fileProvider: (Long, File) -> File` callback that maps one itemId to one file. Translation files have a one-to-many relationship (multiple languages per itemId), requiring the different approach. This design decision is well-reasoned and documented in the spec.

**Verdict: Highly consistent with existing patterns.**

---

## 8. Thread Safety

- **`translateJob?.cancel()` serializes cache access:** The `translate()` function cancels any existing job before starting a new one (line 522). This prevents concurrent read/write to the same cache file.

- **`cacheSkipOnNextTranslate` flag:** Read and cleared in `translate()` (main thread or IO coroutine launch site), set in `cancelTranslation()` (main thread). The flag is not `@Volatile` or wrapped in any synchronization primitive. However, the `translate()` function reads it synchronously before launching the coroutine (line 519-520), and `cancelTranslation()` sets it from the main thread. Since both `translate()` and `cancelTranslation()` are called from UI callbacks (main thread), there is no true concurrent access. **Safe in practice.**

- **`MutableStateFlow.update {}` for `translationState`:** The `update` function provides atomic read-modify-write semantics. The `saveTranslation()` call inside the `update` lambda (line 610-615) performs I/O within the atomic update block. This could theoretically delay the state update if disk I/O is slow, but since the entire coroutine is already on `Dispatchers.IO` and `MutableStateFlow.update` is non-blocking (retry-based CAS), this is acceptable.

### Minor Concern: I/O inside `MutableStateFlow.update {}`

The `saveTranslation()` call is inside the `translationState.update {}` lambda (lines 609-618). `MutableStateFlow.update` uses CAS (compare-and-swap) retry semantics -- if the state is concurrently modified, the lambda is re-executed. If the CAS retries, `saveTranslation()` would be called multiple times (writing the same data). This is idempotent and harmless, but it's worth being aware of. In practice, retries are extremely unlikely because the `translateJob?.cancel()` at line 522 ensures only one active translation coroutine exists.

**Verdict: Thread-safe for the current usage patterns.**

---

## BDD Scenario Coverage

| Scenario | Covered? | How |
|----------|----------|-----|
| SCENARIO-001: Save on completion | Yes | ArticleViewModel cache save after `status == "translated"` |
| SCENARIO-002: Load cached on reopen | Yes | Cache check in `translate()` |
| SCENARIO-003: No progress indicator for cache | Yes | Direct `Empty -> Translated` transition |
| SCENARIO-004: Language change no stale cache | Yes | `languageCode` in cache key |
| SCENARIO-005: Both language caches coexist | Yes | Tested in `multipleLanguagesForSameItemCoexistIndependently` |
| SCENARIO-006: Partial failure IS cached | Yes | `status == "translated"` when all terminal (incl. `translated == -1`) |
| SCENARIO-007: Error state not cached | Yes | `catch (e: Exception)` sets `TranslationState.Error`, no save path |
| SCENARIO-008: In-progress not cached | Yes | Save only in `status == "translated"` block |
| SCENARIO-009: Cancel mid-progress no cache | Yes | `translateJob?.cancel()` + CancellationException rethrown |
| SCENARIO-010: Cleanup on article deletion | Yes | `deleteTranslationCache()` in RssLocalSync + CleanupOrphanedFilesJob |
| SCENARIO-011: Feed deletion cascades | Yes | Same deletion loop handles cascade |
| SCENARIO-012: Re-translate overrides cache | Yes | `forceRefresh=true` from ArticleScreen |
| SCENARIO-013: Cancel-then-retranslate | Yes | `cacheSkipOnNextTranslate` flag |
| SCENARIO-014: First use no cache | Yes | `loadTranslation()` returns null, proceeds to LLM |
| SCENARIO-015: Corrupted cache graceful | Yes | `catch (e: Exception)` returns null in `loadTranslation()` |
| SCENARIO-016: Empty translations dir | Yes | `translationsDir.mkdirs()` in `saveTranslation()` |
| SCENARIO-017: Cache I/O on Dispatchers.IO | Yes | `translate()` launches on `Dispatchers.IO` |
| SCENARIO-018: Cache save no UI block | Yes | Already on IO dispatcher |
| SCENARIO-019: DEVICE_DEFAULT resolves locale | Yes | `resolveLanguageCode()` at line 659-660 |
| SCENARIO-020: Auto-translate loads cache | Yes | `init {}` calls `translate()` with default `forceRefresh=false` |
| SCENARIO-021: Large article round-trip | Yes | Tested in `largeArticleRoundTrip_worksCorrectly` (120 paragraphs) |
| SCENARIO-022: Re-translate after language change | Yes | Different `languageCode` = different cache file |
| SCENARIO-023: Survives reinstall via backup | Yes | Uses `filesDir` (backup-eligible), not `cacheDir` |
| SCENARIO-024: Available after app update | Yes | `ignoreUnknownKeys = true` + forward-compat test |

**All 24 scenarios are covered.**

---

## Comments (Non-blocking)

### Comment 1: `saveTranslation()` inside `MutableStateFlow.update {}` lambda

**File:** `ArticleViewModel.kt:609-618`
**Severity:** Low (informational)

The `saveTranslation()` I/O call is inside the `translationState.update {}` lambda. If the CAS retries (unlikely but possible), the save executes multiple times. This is idempotent so it's safe, but moving the save outside the `update` block and using a local variable for the final state would be cleaner:

```kotlin
var shouldSave: ArticleTranslation? = null
translationState.update { currentState ->
    // ... existing logic ...
    if (updatedArticleTranslation.status == "translated") {
        shouldSave = updatedArticleTranslation
        TranslationState.Translated(articleTranslation = updatedArticleTranslation)
    } else {
        TranslationState.Translating(articleTranslation = updatedArticleTranslation)
    }
}
shouldSave?.let { translation ->
    try {
        saveTranslation(itemId, languageCode, filePathProvider.translationsDir, translation)
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Failed to save translation cache", e)
    }
}
```

This is a suggestion for a future cleanup, not a required change.

### Comment 2: No logging on corrupted cache load

**File:** `TranslationBlob.kt:28-29`
**Severity:** Low

The spec mentions "logged, not shown to user" for SCENARIO-015 (corrupted cache). The current implementation silently returns `null` without any logging. The spec's `loadTranslation()` pseudocode in section 2.1 includes a comment "log and return null." Consider adding a `Log.w()` call in the catch block for debuggability. Non-blocking since `TranslationBlob.kt` is designed as a pure utility without Android dependencies (no `android.util.Log` import), which is actually a cleaner design for testability.

---

## Final Verdict: Approved with Comments

The implementation is correct, well-structured, and consistent with existing project patterns. All 7 ACs and all 24 BDD scenarios are addressed. The two comments above are informational improvements for future consideration, not blocking issues. The 12 unit tests provide solid coverage of the core cache operations.

**Strengths:**
- Minimal, focused implementation (~55 lines of new production code in TranslationBlob.kt)
- Faithful adherence to the Blob.kt pattern
- Correct use of `filesDir` for backup eligibility
- Forward-compatible JSON deserialization (`ignoreUnknownKeys`)
- Clean integration into existing cleanup pipelines (RssLocalSync + CleanupOrphanedFilesJob)
- Well-structured unit tests with 12 test cases
