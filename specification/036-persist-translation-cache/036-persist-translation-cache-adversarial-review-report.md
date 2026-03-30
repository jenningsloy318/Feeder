# Adversarial Review Report: Spec 036 -- Persist Translation Cache

**Date:** 2026-03-30
**Reviewer:** Adversarial Review Agent
**Spec:** 036-persist-translation-cache
**Verdict:** CONTESTED

---

## Executive Summary

The translation cache implementation is clean, well-structured, and closely follows the existing blob pattern. However, the review identifies two medium-severity issues (non-atomic file writes, prefix-collision risk in deletion), one low-severity concern (thread safety of `cacheSkipOnNextTranslate`), and several minor observations. None are blocking, but V1 and V3 warrant fixes before merge.

---

## Skeptic Lens

### V1: False Assumptions -- Can the cache key produce stale translations?

**STATUS: PASS with note**

The cache key `(itemId, languageCode)` correctly prevents cross-language staleness. The `resolveLanguageCode()` function at `ArticleViewModel.kt:659-660` correctly resolves `DEVICE_DEFAULT` (empty code) to `Locale.getDefault().language` at runtime, meaning a device locale change invalidates the old cache (correct behavior per SCENARIO-019).

**Potential concern:** If the article *content* changes (e.g., the feed source updates the article body), the cached translation is stale relative to the new content. The cache key does not incorporate a content hash. However, this is explicitly out of scope per the requirements (AC-004 says "If a cached translation exists and a new translation is triggered, the new result overwrites the old cache on completion"), and `forceRefresh` provides an escape hatch. Acceptable for v1.

### V2: Edge Cases -- Empty articles, zero paragraphs, massive articles

**STATUS: PASS**

- **Zero translatable paragraphs:** Handled at `ArticleViewModel.kt:544-549`. If `extractTranslatableParagraphs()` returns empty, the code sets `TranslationState.Error` and returns before any cache interaction. No empty cache file is created. Correct.
- **Massive articles (120+ paragraphs):** Tested in `TranslationBlobTest.kt:168-178` (`largeArticleRoundTrip_worksCorrectly`). The gzip compression handles this well per the spec's size estimates.
- **Empty `ArticleTranslation.contents` list:** Not explicitly tested, but would serialize to `{"contents":[],"status":"translated"}` which is valid JSON. The `loadTranslation` would deserialize it correctly. Low risk.

### V3: Failure Modes -- Disk full, permission denied, corrupt files

**STATUS: CONTESTED -- non-atomic write is a real risk**

1. **Corrupted cache on read:** Correctly handled. `loadTranslation()` catches all `Exception` at `TranslationBlob.kt:28-29`, returning null and falling back to LLM. Tested with both corrupted gzip and invalid JSON in `TranslationBlobTest.kt:56-77`.

2. **Disk full / permission denied on write:** The `saveTranslation()` call in `ArticleViewModel.kt:609-618` wraps the save in try/catch with a `Log.w`. The translation still completes in the UI -- cache is best-effort. Correct.

3. **Non-atomic write (MEDIUM SEVERITY):** `saveTranslation()` at `TranslationBlob.kt:38-44` writes directly to the target file via `GZIPOutputStream(file.outputStream())`. If the process crashes or disk fills mid-write, the file will be left in a corrupted half-written state. The next `loadTranslation()` will catch the exception and fall back to LLM, so data loss is limited to one cache entry. However, the existing `Blob.kt` pattern (`blobOutputStream` at `Blob.kt:22-25`) has the same issue -- it also writes directly without atomic rename. So while the risk is real, it is **consistent with the existing codebase pattern**. A write-to-temp-then-rename approach would be safer but is not required for parity.

   **Recommendation:** Accept for v1 (matches existing pattern). Consider a follow-up ticket to add atomic writes to both `Blob.kt` and `TranslationBlob.kt`.

### V4: Adversarial Inputs -- Crafted article IDs causing file path issues

**STATUS: PASS**

- `itemId` is a `Long` (Room auto-generated primary key). There is no user-controlled string in the file path. The filename format `${itemId}_${languageCode}.translation.json.gz` uses a numeric ID and a language code from the `TranslationLanguage` enum. The `languageCode` values are hardcoded enum constants (e.g., "zh", "en", "fr") or `Locale.getDefault().language` which returns an ISO 639-1 code. No path traversal risk.
- The `deleteTranslationCache` filter at `TranslationBlob.kt:51-53` uses `startsWith("${itemId}_")` which is string-prefix matching on filenames only (not paths). Safe.

---

## Architect Lens

### V5: Does TranslationBlob.kt follow the Blob.kt pattern correctly?

**STATUS: PASS**

`TranslationBlob.kt` faithfully follows the `Blob.kt` pattern:
- Top-level functions (no class)
- Same package (`com.nononsenseapps.feeder.blob`)
- Same gzip compression approach
- Same `File` parameter pattern (directory + itemId)

The addition of `Json { ignoreUnknownKeys = true }` for forward compatibility is a smart enhancement over the original blob pattern (which doesn't need JSON parsing). The `deleteTranslationCache` function is new (blob files don't have a multi-file-per-article pattern) and is correctly implemented.

### V6: Is the ViewModel integration clean?

**STATUS: PASS with observation**

The `translate()` method at `ArticleViewModel.kt:518-633` adds cache logic cleanly:
- Cache check at the top (lines 529-540), before any extraction or API calls
- Cache save inside the existing `translationState.update` block (lines 607-618)
- The `languageCode` variable is captured early (line 527) and is available in the inner lambda. Correct.

**Observation on `translationState.update {}` and I/O:** The `saveTranslation()` call at line 610 performs disk I/O inside a `MutableStateFlow.update {}` lambda. While the coroutine is already on `Dispatchers.IO` (correct for I/O), `MutableStateFlow.update` uses CAS (compare-and-swap) internally and retries the lambda on contention. If two updates race, the save could execute multiple times. In practice, this cannot happen here because `translateJob?.cancel()` at line 522 serializes access, and the update is called from a single `collect` flow. **Low risk but architecturally impure** -- ideally the save would happen after the state update, not inside it. However, moving it outside would require restructuring the flow collection, which is disproportionate effort for v1.

### V7: Is cleanup properly cascaded?

**STATUS: PASS**

Two cleanup paths exist:

1. **RssLocalSync (article deletion during sync):** `deleteTranslationCache()` is called at `RssLocalSync.kt:442-445` inside the `for (id in articlesToDelete)` loop, immediately after existing blob file deletions. This handles both individual article pruning and feed deletion cascades (both flow through the same loop). Correct.

2. **CleanupOrphanedFilesJob (daily idle cleanup):** `cleanupTranslationsDirectory()` at `CleanupOrphanedFilesJob.kt:100-120` correctly parses the `itemId` from the filename prefix, checks against `validIds`, and deletes orphans. The `toLongOrNull()` at line 109 handles malformed filenames gracefully (they get deleted as orphans, which is the correct behavior). Called at line 54 after existing blob cleanup calls.

Both paths are consistent with how existing blob files are cleaned up. No orphan leak path identified.

---

## Minimalist Lens

### V8: Is every line necessary? Any dead code or over-engineering?

**STATUS: PASS**

The implementation is lean:
- `TranslationBlob.kt`: 55 lines, 4 functions. No dead code.
- `FilePathProvider.kt`: 2 lines added (interface + implementation). Minimal.
- `ArticleViewModel.kt`: ~25 lines of new cache logic, plus `resolveLanguageCode()` (2 lines) and `cacheSkipOnNextTranslate` flag (1 line). All necessary.
- `RssLocalSync.kt`: 4 lines added for cleanup. Minimal.
- `CleanupOrphanedFilesJob.kt`: 20 lines for `cleanupTranslationsDirectory()` + 1 call line. Necessary because the existing `cleanupDirectory()` method's `fileProvider: (Long, File) -> File` callback cannot handle multi-file-per-ID patterns.
- `TranslationBlobTest.kt`: 12 tests covering round-trip, corruption, directory creation, overwrite, multi-language, deletion, large articles, and forward-compatible JSON. Good coverage, no bloat.

### Is `forceRefresh` + `cacheSkipOnNextTranslate` redundant?

**STATUS: Justified, not redundant**

- `forceRefresh` is passed explicitly from the UI when the user taps translate while already in `Translated` state (`ArticleScreen.kt:187`). This is a direct user intent.
- `cacheSkipOnNextTranslate` is an internal flag set by `cancelTranslation()` to handle the cancel-then-retranslate flow (SCENARIO-013). After cancel, `translate()` is called with default `forceRefresh = false`, but the user's intent is to get a fresh translation (otherwise why cancel?).

These serve different UX flows and cannot be collapsed into one mechanism without changing the `translate()` API contract. The current approach is clean: the flag is set in `cancelTranslation()`, consumed and cleared at the top of `translate()` (lines 519-520). No redundancy.

**Minor thread safety note (LOW SEVERITY):** `cacheSkipOnNextTranslate` is a plain `Boolean` field (not `volatile`, not `AtomicBoolean`). The spec's thread safety analysis (Section 5) claims it's only accessed from the main thread. Let's verify: `cancelTranslation()` is called from UI callbacks (main thread). `translate()` reads the flag at lines 519-520 on the calling thread (main thread), then clears it. The flag is not accessed inside the `launch(Dispatchers.IO)` block. **This is safe** -- both reads and writes happen on the main thread before the coroutine launches.

---

## Destructive Action Gate

### Does `deleteTranslationCache` use wildcard matching safely?

**STATUS: PASS**

At `TranslationBlob.kt:50-53`:
```kotlin
translationsDir.listFiles { _, name ->
    name.startsWith("${itemId}_") && name.endsWith(".translation.json.gz")
}?.forEach { it.delete() }
```

The filter requires BOTH:
1. Prefix match: `"${itemId}_"` (note the underscore -- prevents `itemId=4` matching `42_zh...`)
2. Suffix match: `.translation.json.gz`

**Prefix collision analysis:** Could `itemId=4` match files for `itemId=42`? No. The filter checks `startsWith("4_")`, not `startsWith("4")`. The underscore delimiter prevents this. File `42_zh.translation.json.gz` starts with `"42_"`, not `"4_"`. Safe.

Tested in `TranslationBlobTest.kt:146-157` (`deleteTranslationCache_doesNotRemoveFilesForOtherItemIds`): deleting itemId=42 does not affect itemId=99. Test passes.

### Could the orphan cleanup accidentally delete valid cache files?

**STATUS: PASS**

At `CleanupOrphanedFilesJob.kt:109`:
```kotlin
val itemId = file.name.substringBefore("_").toLongOrNull()
if (itemId == null || itemId !in validIds) { file.delete() }
```

- If `toLongOrNull()` returns null (malformed filename), the file is deleted. This is correct -- malformed files in the translations directory are garbage.
- If the parsed `itemId` is not in `validIds` (the set of all current feed item IDs from the database), the file is deleted. This is correct -- it's an orphan.
- If the parsed `itemId` IS in `validIds`, the file is preserved. No false positives.

The `validIds` is fetched fresh from the database at the start of `doWork()` (line 44). Race window: if an article is deleted between the `getAllFeedItemIds()` call and the cleanup loop, the orphan will survive until the next daily run. Acceptable.

---

## Findings Summary

| ID | Severity | Lens | Finding | Recommendation |
|----|----------|------|---------|----------------|
| F1 | Medium | Skeptic (V3) | Non-atomic file write in `saveTranslation()` -- crash mid-write leaves corrupted file | Accept for v1 (matches existing `Blob.kt` pattern). Follow-up ticket for atomic writes. |
| F2 | Low | Architect (V6) | `saveTranslation()` called inside `MutableStateFlow.update {}` lambda (I/O in CAS retry block) | Accept -- single-writer guarantee from `translateJob?.cancel()` prevents retry. Document as tech debt. |
| F3 | Info | Skeptic (V1) | Cache key does not include content hash -- stale translation if article body updates upstream | Out of scope per requirements. `forceRefresh` provides manual escape hatch. |
| F4 | Info | Minimalist | `cacheSkipOnNextTranslate` is a plain `Boolean`, not `AtomicBoolean` | Safe -- both reads and writes on main thread before coroutine launch. No fix needed. |

---

## Verdict: CONTESTED

**Rationale:** The implementation is well-designed, follows existing patterns, and has good test coverage (12 unit tests). The two medium/low findings (F1, F2) are real but non-blocking because:
- F1 matches the existing `Blob.kt` pattern (introducing atomic writes here alone would be inconsistent)
- F2 is theoretically impure but practically safe due to the single-writer guarantee

**Conditions to resolve CONTESTED -> PASS:**
1. Acknowledge F1 (non-atomic write) as accepted tech debt, either in a code comment or a follow-up issue
2. No code changes required -- the implementation is correct for v1

If these conditions are acknowledged, the verdict upgrades to **PASS**.
