# Spec 036: Persist Translation Cache -- QA Test Plan

## 1. Overview

This document maps all 24 BDD scenarios from `01.1-behavior-scenarios.md` to test types, identifies coverage from the planned `TranslationBlobTest.kt` (Phase 1), and lists gaps requiring additional tests or manual verification.

**Test conventions observed in existing codebase:**
- Test runner: JUnit (via `kotlin.test.Test`)
- Assertions: `kotlin.test` (`assertEquals`, `assertTrue`, `assertFalse`, `assertNull`)
- No MockK usage in blob/AI layer tests -- pure function testing with real temp directories
- Package structure mirrors source: `com.nononsenseapps.feeder.blob` -> `app/src/test/java/com/nononsenseapps/feeder/blob/`
- Build variant: `fdroidDebug`
- Known broken tests (3): `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` (2 tests)

---

## 2. BDD Scenario to Test Type Mapping

### Legend
- **Unit**: Automated unit test in `TranslationBlobTest.kt` or similar
- **Integration**: Requires ViewModel/Repository/Sync wiring (typically instrumented or complex unit test)
- **Code Review**: Verified by reading source code (no runtime test needed)
- **Manual**: Requires device/emulator interaction
- **Platform**: Verified by Android platform guarantees (no test needed)

| # | Scenario | Test Type | Covered by TranslationBlobTest? | Phase | Notes |
|---|----------|-----------|:------------------------------:|:-----:|-------|
| 001 | Completed translation saved to cache | Unit | YES | 1 | save + load round-trip |
| 002 | Cached translation loaded on reopen | Unit + Code Review | Partial (load) | 1, 2 | Unit: load returns valid data. Code review: ViewModel sets `Translated` directly |
| 003 | No progress indicator for cached translation | Code Review | NO | 2 | Verify `TranslationState` never enters `Translating` on cache hit |
| 004 | Language change doesn't serve stale cache | Unit | Implicit | 1 | Different `languageCode` -> different file -> `loadTranslation` returns null |
| 005 | Both language caches coexist | Unit | YES | 1 | Test: save `en` + `zh` for same itemId, load each independently |
| 006 | Partial translation failure is cached | Unit | YES (implied) | 1 | save/load round-trip with mixed `translated` flags |
| 007 | Translation error state not cached | Code Review | NO | 2 | Verify `saveTranslation` only called when `status == "translated"` |
| 008 | In-progress translation not cached | Code Review | NO | 2 | Verify coroutine cancellation path doesn't call `saveTranslation` |
| 009 | Cancel mid-progress -- no cache written | Code Review | NO | 2 | Same as 008, explicit cancel path |
| 010 | Cache cleanup on article deletion | Unit + Code Review | YES (delete) | 1, 3 | Unit: `deleteTranslationCache`. Code review: called in RssLocalSync loop |
| 011 | Feed deletion cascades to translation cleanup | Code Review | NO | 3 | Verify RssLocalSync deletion loop includes `deleteTranslationCache` |
| 012 | Re-translate overrides existing cache | Code Review | NO | 2 | Verify `forceRefresh=true` skips cache and save overwrites |
| 013 | Cancel-then-retranslate flow | Code Review | NO | 2 | Verify `cacheSkipOnNextTranslate` flag logic |
| 014 | First use -- no cache file exists | Unit | YES | 1 | `loadTranslation` returns null when file absent |
| 015 | Corrupted cache file handled gracefully | Unit | YES | 1 | Both invalid gzip and invalid JSON cases |
| 016 | Empty translations directory on first launch | Unit | YES | 1 | `saveTranslation` creates directory |
| 017 | Cache I/O doesn't block main thread | Code Review | NO | 2 | Verify `Dispatchers.IO` in `translate()` launch |
| 018 | Cache save doesn't block UI | Code Review | NO | 2 | Verify save runs inside existing IO coroutine |
| 019 | DEVICE_DEFAULT resolves actual locale | Code Review | NO | 2 | Verify `resolveLanguageCode` helper |
| 020 | Auto-translate loads from cache | Code Review | NO | 2 | Verify `init {}` block calls `translate()` with default `forceRefresh=false` |
| 021 | Large article (120 paragraphs) round-trip | Unit | YES | 1 | Stress test with 120 `ParagraphTranslation` entries |
| 022 | Re-translate after language change | Code Review | NO | 2 | Composite of SCENARIO-004 + SCENARIO-012 |
| 023 | Cache survives app reinstall via backup | Platform | NO | -- | `filesDir` is backup-eligible by default; no code test needed |
| 024 | Cache available after app update | Unit + Platform | YES (forward compat) | 1 | Unit: unknown JSON keys deserialize successfully. Platform: `filesDir` survives updates |

---

## 3. TranslationBlobTest.kt Expected Coverage (Phase 1)

Based on task list `1.3`, the dev-executor should create these 12 unit tests:

| # | Test Description | BDD Scenario(s) |
|---|-----------------|-----------------|
| 1 | save then load round-trip returns identical `ArticleTranslation` | 001, 002 |
| 2 | `loadTranslation` returns null when file doesn't exist | 014 |
| 3 | `loadTranslation` returns null for corrupted gzip file | 015 |
| 4 | `loadTranslation` returns null for invalid JSON in valid gzip | 015 |
| 5 | `saveTranslation` creates directory if it doesn't exist | 016 |
| 6 | `saveTranslation` overwrites existing file | 012 (cache overwrite) |
| 7 | Multiple languages for same itemId coexist independently | 005 |
| 8 | `deleteTranslationCache` removes all language files for an itemId | 010 |
| 9 | `deleteTranslationCache` does not remove files for other itemIds | 010 |
| 10 | `deleteTranslationCache` is no-op when directory doesn't exist | 010 |
| 11 | Large article (120 paragraphs) round-trip works correctly | 021 |
| 12 | File with unknown JSON keys deserializes successfully | 024 |

**BDD scenarios fully covered by unit tests:** 001, 005, 010, 014, 015, 016, 021, 024 (8 of 24)

**BDD scenarios partially covered:** 002, 006, 012 (3 of 24)

---

## 4. Coverage Gaps

### 4.1 Scenarios requiring code review only (no additional tests needed)

These scenarios are behavioral contracts verified by inspecting the ViewModel implementation. They don't have dedicated unit tests because they require ViewModel mocking infrastructure that doesn't exist in the current test suite for this layer.

| Scenario | What to verify in code |
|----------|----------------------|
| 003 | `translate()` sets `TranslationState.Translated` directly on cache hit, never `Translating` |
| 007 | `saveTranslation()` is only called inside the `status == "translated"` branch |
| 008 | ViewModel destruction cancels coroutine; `saveTranslation` is after the collect loop |
| 009 | `cancelTranslation()` cancels job; CancellationException prevents reaching save |
| 011 | `deleteTranslationCache` called in `RssLocalSync.for (id in articlesToDelete)` loop |
| 012 | `forceRefresh=true` skips cache check; save still runs on completion |
| 013 | `cancelTranslation()` sets `cacheSkipOnNextTranslate=true`; `translate()` reads and clears it |
| 017 | `translate()` launched with `Dispatchers.IO` |
| 018 | Save occurs inside the same IO coroutine (no dispatcher switch needed) |
| 019 | `resolveLanguageCode()` returns `Locale.getDefault().language` when `code.isEmpty()` |
| 020 | `init {}` auto-translate path calls `translate()` with default `forceRefresh=false` |
| 022 | Composite: language-keyed file naming + forceRefresh mechanism |

### 4.2 Platform-guarantee scenarios (no test needed)

| Scenario | Guarantee |
|----------|-----------|
| 023 | Android auto-backup includes `filesDir` by default when `allowBackup="true"` |
| 024 | App updates preserve `filesDir` contents (Android platform guarantee) |

### 4.3 Potential additional unit tests (nice to have, not in Phase 1 task list)

These are edge cases that could strengthen confidence but are not strictly required:

1. **Round-trip with partial failure (`translated = -1`)**: Save an `ArticleTranslation` with mixed `translated` values (1, -1, 0), verify load returns identical data. (Strengthens SCENARIO-006 coverage)
2. **`translationFile` naming correctness**: Verify file path is `{translationsDir}/{itemId}_{lang}.translation.json.gz` for various inputs including edge cases (itemId=0, long language codes like `zh-Hans`)
3. **Concurrent save safety**: Two rapid saves to the same file don't corrupt (low priority -- serialized by `translateJob?.cancel()`)
4. **Empty `ArticleTranslation`**: Round-trip with `contents = emptyList()` (degenerate case)
5. **`CleanupOrphanedFilesJob` translation cleanup**: Verify `cleanupTranslationsDirectory` correctly parses itemId from filename prefix and deletes orphaned files while preserving valid ones

---

## 5. Edge Cases to Watch

| Edge Case | Risk | Mitigation |
|-----------|------|------------|
| `itemId` containing `_` in filename | Low -- `itemId` is `Long`, no underscores possible | `substringBefore("_")` in cleanup is safe |
| Very long language code (e.g., `zh-Hans`) | Low -- valid BCP-47 codes work in file names | No special handling needed |
| Race between save and cleanup | Very low -- cleanup runs in sync/scheduled worker, save in ViewModel IO coroutine | File-level atomicity at OS level |
| `translationsDir` on read-only storage | Very low -- `filesDir` is always writable | `saveTranslation` catches exceptions |
| Gzip stream not closed properly | Medium -- could leave truncated files | Verify `use { }` blocks in implementation |
| Serialization schema evolution | Low -- `ignoreUnknownKeys = true` handles additive changes | Forward compat test covers this |

---

## 6. Test Execution Plan

### Step 1: Build verification (after Phase 1)
```bash
cd /home/jenningsl/development/personal/jenningsloy318/Feeder/.worktree/036-persist-translation-cache
./gradlew :app:compileFdroidDebugKotlin
```

### Step 2: Run full test suite
```bash
cd /home/jenningsl/development/personal/jenningsloy318/Feeder/.worktree/036-persist-translation-cache
./gradlew :app:testFdroidDebugUnitTest
```

### Step 3: Verify results
- Count total tests passed/failed
- Count new tests added (expect 12 in `TranslationBlobTest.kt`)
- Verify only the 3 known broken tests fail:
  - `CustomFeederTextToolbarTest`
  - `MenuConfigStoreTest` (2 tests)
- Report any regressions

### Step 4: BDD coverage summary
- Report number of scenarios covered by automated tests
- Report number verified by code review
- Report overall coverage percentage

---

## 7. Manual Testing Checklist (for future device testing)

These scenarios should be verified on a device/emulator after all phases are complete:

- [ ] Translate an article -> close app -> reopen -> tap translate -> loads from cache instantly
- [ ] Translate in English -> change language to Chinese -> translate same article -> fresh LLM call
- [ ] While translation is in progress, cancel -> tap translate again -> fresh LLM call (not cached)
- [ ] Delete a feed -> verify translation cache files are removed from `filesDir/translations/`
- [ ] Force-stop app during translation -> reopen -> translate -> no corrupted cache served
- [ ] Translate article with 50+ paragraphs -> verify cache file written and loads correctly
