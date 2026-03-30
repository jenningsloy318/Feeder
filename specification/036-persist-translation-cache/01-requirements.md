# Spec 036: Persist Translation Cache

## Summary

Persist completed article translations to disk so that re-opening an already-translated article loads the cached result instantly instead of re-sending every paragraph to the LLM. This saves API costs and provides immediate translated content for previously translated articles.

## Current Behavior Analysis

### Translation Data Lifecycle (In-Memory Only)

1. **`ArticleViewModel`** holds a `MutableStateFlow<TranslationState>` initialized to `TranslationState.Empty`.
2. When `translate()` is called, it extracts `TranslatableText` paragraphs from the `LinearArticle` content tree via `TranslatableTextExtractor`, builds an `ArticleTranslation` with empty paragraph translations, and sets state to `TranslationState.Translating`.
3. **`ParagraphTranslationCoordinator`** translates paragraphs in parallel (Semaphore(3) + channelFlow). Each completed paragraph emits a `ParagraphTranslationProgress` event, and the ViewModel updates the `ArticleTranslation` contents in place.
4. When all paragraphs are resolved, state transitions to `TranslationState.Translated`.
5. **On navigation away or ViewModel destruction, the translation is lost entirely.** Re-opening the same article triggers a fresh LLM translation of all paragraphs.

### Key Data Structures

| Class | Location | Description |
|-------|----------|-------------|
| `ArticleTranslation` | `ai/ArticleTranslation.kt` | `@Serializable` — contains `List<ParagraphTranslation>` + `status` string |
| `ParagraphTranslation` | `ai/ParagraphTranslation.kt` | `@Serializable` — `index`, `text` (source), `translation`, `translated` (0/1/-1) |
| `TranslationState` | `ArticleViewModel.kt` | Sealed interface: `Empty`, `Translating(ArticleTranslation)`, `Translated(ArticleTranslation)`, `Error(String)` |
| `TranslatableText` | `ai/TranslatableText.kt` | Source text + element type + nesting level (not serializable, extracted at runtime) |

### Article Identification

- Articles are identified by `FeedItem.id` (Long, auto-generated primary key in Room `feed_items` table).
- The `ArticleViewModel` receives `itemId: Long` from `SavedStateHandle`.
- This `itemId` is stable for the lifetime of the feed item in the database.

### Target Language

- Stored in `SettingsStore` as `TranslationLanguage` enum (SharedPreferences key `PREF_TRANSLATION_LANGUAGE`).
- The cache key must include the target language — if the user changes language, old translations for a different language must not be served.

### Existing Persistence Patterns

| Pattern | Used For | Mechanism |
|---------|----------|-----------|
| Room database | Feed items, feeds, blocklist, sync | SQLite via Room with DAOs |
| Blob files (gzip) | Article HTML content | `{itemId}.txt.gz` in `filesDir/articles/` |
| Full-text blob files | Full article HTML | `{itemId}.full.html.gz` in `cacheDir/full_articles/` |
| SharedPreferences | Settings, AI config | Key-value via `SettingsStore` |

The **blob file pattern** (`Blob.kt`) is the closest analogue: it stores per-article content as gzip-compressed files keyed by `itemId`. Translation cache can follow the same pattern using JSON serialization of `ArticleTranslation`.

### Data Size Estimate

A typical `ArticleTranslation` JSON (20 paragraphs, avg 200 chars each source + translation) is ~8-15 KB uncompressed, ~2-5 KB gzip-compressed. Even large articles (100+ paragraphs) stay under 100 KB compressed.

## Acceptance Criteria

### AC-001: Save Translation on Completion

When all paragraphs in an `ArticleTranslation` reach a terminal state (`translated == 1` or `translated == -1`) and `TranslationState` becomes `Translated`, the `ArticleTranslation` JSON must be persisted to a file keyed by `(itemId, targetLanguageCode)`.

### AC-002: Load Cached Translation on Article Open

When an article's `translate()` is invoked (either manually or via auto-translate), the system must first check for a cached translation matching `(itemId, currentTargetLanguage)`. If a fully-completed cache hit exists, load it directly into `TranslationState.Translated` without calling the LLM.

### AC-003: Cache Key Includes Target Language

The cache key must incorporate both the article `itemId` and the `TranslationLanguage.code`. Changing the target language in settings must not serve a stale translation for a different language.

### AC-004: Partial Translation Not Cached

Only fully completed translations (status `"translated"`) are persisted. In-progress or error-state translations are not written to disk. If a cached translation exists and a new translation is triggered (e.g., user retries), the new result overwrites the old cache on completion.

### AC-005: Cache File Cleanup on Article Deletion

When a `FeedItem` is deleted from the Room database (cascade from feed deletion or cleanup), associated translation cache files should be cleaned up. Follow the same lifecycle as blob files.

### AC-006: Instant UI Feedback for Cached Translations

Loading a cached translation must be visually instant (no progress indicator, no "Translating..." state). The UI transitions directly from `Empty` to `Translated`.

### AC-007: Re-translate Option

Users must still be able to re-translate an article even if a cache exists. The cancel-then-translate flow (canceling resets to `Empty`, then translate triggers fresh LLM call) should work. Alternatively, if the user taps translate when already in `Translated` state, it should re-translate from LLM and update the cache.

## Non-Functional Requirements

### NFR-001: Storage Format

Use `kotlinx.serialization.json` to serialize `ArticleTranslation` (already `@Serializable`). Store as gzip-compressed JSON files following the existing blob pattern in `Blob.kt`.

### NFR-002: Storage Location (Survive App Reinstall)

Store translation cache files in a location that **survives app reinstall**, similar to the database file. Use the same storage approach as the Room database (internal `databases/` or backup-eligible `filesDir`). On Android, `filesDir` is backed up by Auto Backup by default (up to 25 MB to Google Drive), which covers reinstall scenarios. Alternatively, consider `getExternalFilesDir()` if the project already uses external storage.

The user explicitly requires this data to persist across reinstalls, since translations represent paid API results. Suggested path: `filesDir/translations/` (backup-eligible by default) or alongside the database directory.

### NFR-003: File Naming Convention

File name format: `{itemId}_{languageCode}.translation.json.gz` (e.g., `12345_zh.translation.json.gz`). For `DEVICE_DEFAULT` language, resolve the actual device locale code at save time.

### NFR-004: I/O Performance

File reads/writes must happen on `Dispatchers.IO`. Cache lookups must not block the main thread or delay article content rendering.

### NFR-005: No Database Schema Change

This feature should use file-based storage only, avoiding a Room database migration. This keeps the change minimal and follows the existing blob file pattern.

### NFR-006: Backward Compatibility

Articles translated before this feature will simply not have a cache file. The system gracefully falls back to LLM translation when no cache exists.

## Out of Scope

- **Cache eviction policy** (LRU, time-based expiry): Not needed for v1. Translation files are small and bounded by the number of articles in the database.
- **Cache size settings UI**: No user-facing controls for cache management in this iteration.
- **Summary caching**: Only translation results are cached. AI summary caching is a separate feature.
- **Cross-device sync of translations**: Translations are local-only.
- **Partial cache restoration** (resuming a half-translated article from cache): Only fully completed translations are cached.
- **Migration of in-memory translations to cache**: Only new translations going forward will be persisted.
