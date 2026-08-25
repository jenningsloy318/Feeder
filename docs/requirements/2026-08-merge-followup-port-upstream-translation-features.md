# Requirements: Port Upstream Translation Features onto the Multi-Provider AI Architecture

Status: Planned
Source branch: `ai-features` (after merge commit `15d8864c`, which merged upstream `master` up to `3023e912`)
Scope: Phase B of the master→ai-features merge strategy. Phase A (merge, keep our architecture, drop upstream translation stack) is complete.

## Background

The 2026-08 merge of upstream `master` into `ai-features` diverged on translation:

- **Our branch** replaced the single-provider `openai/` package with a multi-provider
  architecture under `ai/` (`AIClient`, `AIApi`, `AIProvider`, `ProviderConfig`) and
  implements paragraph-by-paragraph article translation driven by
  `ParagraphTranslationCoordinator`, cached via `blob/saveTranslation`.
- **Upstream master** evolved the old `openai/` stack with three features we do not
  have. During the merge they were removed because they are wired against the old
  `OpenAISettings`/`OpenAIApi` shape that no longer exists on this branch.

Dropped upstream code is fully recoverable from `origin/master` (see commit references
below).

## Features to port

### 1. DeepL translation provider

- Upstream commits: `807d041e` (#1072), fix `b9423997` (#1153)
- Upstream implementation: `openai/OpenAIApi.kt` — DeepL-specific request/response
  models, `translateWithDeepL`, `verifyDeepLSettings`, URL/auth handling
  (`DeepL-Auth-Key` header), `toDeepLTargetLanguageCode`, `isDeepL` detection via
  baseUrl.

Requirements:

- Add DeepL as a provider type in `ai/provider/` implementing `AIClient`:
  - `AIProvider.DEEPL` enum entry
  - `DeepLClient` with `translate(texts, targetLanguage)`; `generateSummary`/`listModels`
    unsupported (return a clear error result)
  - Provider config screen fields: API key, base URL (free/pro auto-detected), no model
    id
- `AIClient.create(settings)` factory must dispatch DeepL settings to `DeepLClient`.
- Translation prompt building must not be used for DeepL (raw text in, translations out).
- LANG_REGEX-style locale handling: DeepL target codes include variants like
  `EN-US`, `PT-BR` (see `TranslationLanguageCodes` upstream).

### 2. Bergamot on-device translation (offline fallback)

- Upstream commits: `44641700` (#1143), `23313d8e` (#1147)
- Upstream implementation (all under `localtranslation/` + assets):
  - `BergamotModelManager` — downloads translation models from Mozilla's repo,
    exposes `downloadProgress`
  - `BergamotWebTranslator` — drives the WASM translator in a hidden WebView
  - `LocalTranslator` — facade deciding AI vs local, checks
    `canTranslateWithoutBergamotDownload`
  - `TranslationLanguageCodes` — supported language-pair mapping
  - Assets: `app/src/main/assets/bergamot/**` (WASM + JS; large binaries)
  - DI bindings in `di/ArchModelModule.kt`
  - Model management UI (downloaded pairs list, delete) in Settings

Requirements:

- Restore `localtranslation/` package + bergamot assets verbatim from `origin/master`
  (they have no dependency on the old `openai/` package except
  `OpenAIApi.TranslationResult` — refactor that to a local sealed result type or map to
  `AIClient.TranslationResult`).
- Integrate into translation flow as a fallback provider: when the active AI provider
  is "local/on-device", route paragraph translation through `LocalTranslator` instead
  of `AIClient`.
- Add `LOCAL_TRANSLATION` provider concept to provider config (no API key; model
  downloads managed in settings).
- Feed-screen wiring: `FeedViewModel`/`FeedScreen` model-download progress surfaces.

### 3. Feed-card (list item) translation

- Upstream commits: `807d041e` (#1072) for UI wiring, `23313d8e` (#1147) for caching
- Upstream implementation:
  - `model/TranslationManager` — `translateFeedListItem`, `getOrTranslateArticle`,
    `getCachedTranslatedFeedListItem` with SHA-256 hash-based file cache under
    `filePathProvider.translationsDir`
  - `FeedViewModel`: `translatedFeedCards` StateFlow, `translateFeedCardIfNeeded`,
    in-flight request dedup via `ConcurrentHashMap`, generation counter invalidation
  - `FeedScreen`: `TranslatedFeedCards.merge(item)`, per-item `LaunchedEffect` trigger,
    `TranslationModelDownloadProgress` overlay
  - `ArticleScreen`: `SystemTranslationSettingsDialog`, already-in-target-language
    detection ("show original" toggle)

Requirements:

- Re-add `TranslationManager` but re-typed against our architecture:
  settings come from `SettingsStore.aiSettings` / translation provider config, not
  `TranslationApiSettings` (the `OpenAISettings` typealias that no longer exists).
- Settings to restore (upstream keys kept for prefs compatibility — the
  `PREF_TRANSLATION_API_*`/`PREF_TRANSLATE_*` consts survive in `SettingsStore.kt`):
  - `preferredTranslationLanguage`
  - `translateArticlePreviewsByDefault` (feed cards)
  - `translateArticlesByDefault`
  - translation provider selection (AI provider vs DeepL vs on-device)
  - `UserSettings` enum entries + `OPMLImporter` mapping + OPML tests
- Feed list translation must degrade gracefully when no provider is configured.

## Non-goals

- No changes to the summary pipeline.
- No new DI framework usage beyond Kodein.
- Do not resurrect `openai/OpenAIApi.kt` or `OpenAISection.kt`; all UI hooks go into
  the existing `AIProviderSection`/`TranslationSettingsScreen` family.

## Acceptance criteria

1. `./gradlew testFdroidDebugUnitTest` passes (existing pre-merge failures in
   `CustomFeederTextToolbarTest`/`MenuConfigStoreTest` excluded — they fail on
   `ai-features` before the merge and are tracked separately).
2. `./gradlew ktlintCheck` passes (compose-rules 0.6.4).
3. DeepL: translating an article with DeepL creds produces cached paragraph
   translations identical in UX to the AI path.
4. On-device: with no network and a downloaded model pair, translation works offline.
5. Feed list: with "translate previews" enabled, titles/snippets translate and cache;
   invalidation on language/provider change.
6. OPML export/import round-trips the new translation settings.
7. Migration: users of upstream builds (2.21+) upgrading to this branch keep their
   DeepL/translation prefs (keys are unchanged).

## Suggested implementation order

1. DeepL provider (smallest, pure `ai/` addition)
2. Feed-card translation on current AI providers
3. Bergamot subsystem restore + local provider
4. Migration tests + OPML wiring
