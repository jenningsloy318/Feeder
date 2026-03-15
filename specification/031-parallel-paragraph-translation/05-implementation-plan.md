# Implementation Plan: Parallel Per-Paragraph Translation

**Specification:** `./04-technical-specification.md`
**Estimated Phases:** 4

**CRITICAL:** All phases/milestones defined in this plan MUST be implemented in a single continuous execution. The execution-coordinator will NOT pause between phases or ask for permission to continue. Every phase from Phase 1 to Final Phase will be completed automatically.

## File Inventory (MANDATORY)

### Files to be Created

| File Path | Purpose | Component/Feature |
|-----------|---------|-------------------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslation.kt` | Per-paragraph translation state data class | Data Model |
| `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt` | Article-level translation state with helper methods | Data Model |
| `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt` | Semaphore+channelFlow coordinator with retry | Coordinator |
| `app/src/test/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinatorTest.kt` | Unit tests for coordinator | Testing |
| `app/src/test/java/com/nononsenseapps/feeder/ai/ArticleTranslationTest.kt` | Unit tests for data model | Testing |

### Files to be Modified

| File Path | Changes Required | Functions Affected |
|-----------|-----------------|-------------------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | Rewrite `translate()`, replace `TranslationState` sealed interface, add `createSettingsWithTimeout()` helper | `translate()`, `TranslationState`, `createSettingsWithTimeout()` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | Update `TranslationStatusSection`, translate button disabled check, `translatedParagraphs` extraction | `TranslationStatusSection()`, toolbar translate button, `translatedParagraphs` variable |
| `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt` | Remove `translateWithProgress()` method and unused imports | `translateWithProgress()` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` | Remove `translateChunk()` method from interface | `translateChunk()` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` | Remove `translateChunk()` override and related imports | `translateChunk()` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` | Remove `translateChunk()` override and related imports | `translateChunk()` |

### Files to be Deleted

| File Path | Reason | Replacement |
|-----------|--------|-------------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/ChunkTranslationCoordinator.kt` | Replaced by per-paragraph coordinator | `ParagraphTranslationCoordinator.kt` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunker.kt` | No longer needed — paragraphs not grouped into chunks | N/A (paragraphs translated individually) |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunk.kt` | No longer needed — per-paragraph uses `TranslatableText` directly | N/A |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationProgress.kt` | Replaced by `ParagraphTranslationProgress` and new `TranslationState` | `ParagraphTranslationCoordinator.kt` (contains `ParagraphTranslationProgress`) |

### File Summary

- **Total Files Created:** 5
- **Total Files Modified:** 6
- **Total Files Deleted:** 4
- **Total Files Affected:** 15

## Milestones

### Milestone 1 (Phase 1): Data Model & Coordinator

**Goal:** Create the new data model classes and the `ParagraphTranslationCoordinator` with Semaphore-based concurrency.
**Dependencies:** None

#### Deliverables

- [x] `ParagraphTranslation` data class matching user-confirmed JSON structure
- [x] `ArticleTranslation` data class with helper computed properties
- [x] `ParagraphTranslationCoordinator` with `channelFlow + Semaphore` pattern
- [x] `ParagraphTranslationProgress` sealed interface for progress events
- [x] Retry logic with exponential backoff (`translateParagraphWithRetry`)
- [x] Error classification (`isRetryableError`)

#### Acceptance Criteria

- `ParagraphTranslation(index=1, text="hello", translation="", translated=0)` is serializable
- `ArticleTranslation.paragraphCompletedCount` correctly counts `translated == 1`
- `ArticleTranslation.buildTranslatedParagraphsList()` returns `null` for pending, text for completed
- Coordinator limits concurrent API calls to `paragraphConcurrency` (Semaphore)
- Coordinator emits `ParagraphComplete` immediately when a paragraph finishes (not batch-delayed)
- Coordinator retries retryable errors up to `paragraphMaxRetries` times with exponential backoff
- Coordinator returns `ParagraphFailed` for non-retryable errors immediately

#### Files Affected (MANDATORY)

**Created:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslation.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`

**Modified:**
- None

**Deleted:**
- None

---

### Milestone 2 (Phase 2): ViewModel Integration

**Goal:** Rewrite `ArticleViewModel.translate()` and `TranslationState` to use the new coordinator and data model for progressive per-paragraph updates.
**Dependencies:** Milestone 1

#### Deliverables

- [x] New `TranslationState` sealed interface: `Empty`, `Translating(ArticleTranslation)`, `Translated(ArticleTranslation)`, `Error(message)`
- [x] Rewritten `translate()` method that builds `ArticleTranslation`, creates coordinator, collects Flow, and updates state per-paragraph via `StateFlow.update{}`
- [x] `createSettingsWithTimeout()` helper extracted into ViewModel
- [x] Auto-translate condition still works with new state types

#### Acceptance Criteria

- `translate()` transitions from `Empty` to `Translating` immediately
- Each `ParagraphComplete` updates the corresponding `ParagraphTranslation` item atomically
- When all paragraphs resolve, state transitions to `Translated`
- Empty article sets `TranslationState.Error`
- Cancelling `viewModelScope` cancels all in-flight translation coroutines
- Auto-translate still triggers when `translationState.value is TranslationState.Empty`

#### Files Affected (MANDATORY)

**Created:**
- None

**Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (rewrite `translate()`, replace `TranslationState`, add `createSettingsWithTimeout()`)

**Deleted:**
- None

---

### Milestone 3 (Phase 3): UI Updates

**Goal:** Update `ArticleScreen.kt` to display progressive per-paragraph translation with determinate progress bar and per-paragraph inline rendering.
**Dependencies:** Milestone 2

#### Deliverables

- [x] `TranslationStatusSection` handles `Translating` state with determinate `LinearProgressIndicator` and "X/Y paragraphs translated" text
- [x] `TranslationStatusSection` handles `Translated` state (shows partial failure count if applicable)
- [x] `TranslationStatusSection` handles `Error` state
- [x] Translate button disabled during `Translating` state
- [x] `translatedParagraphs` extracted from `ArticleTranslation.buildTranslatedParagraphsList()` for both `Translating` and `Translated` states

#### Acceptance Criteria

- During translation: progress bar shows `completedCount/totalCount` fraction
- During translation: text shows "4/10 paragraphs translated"
- On completion with partial failures: error section shows "2 paragraph(s) failed to translate"
- Translate button is disabled while `Translating`, enabled otherwise
- Completed paragraph translations appear inline immediately (via `translatedParagraphs` list)
- Pending paragraphs show original text (empty string in `translatedParagraphs`)

#### Files Affected (MANDATORY)

**Created:**
- None

**Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` (update `TranslationStatusSection`, button, `translatedParagraphs` extraction)

**Deleted:**
- None

---

### Milestone 4 (Phase 4): Dead Code Cleanup & Testing

**Goal:** Remove all dead code from spec-30 chunk infrastructure and write unit tests for new components.
**Dependencies:** Milestones 1-3

#### Deliverables

- [x] Delete `ChunkTranslationCoordinator.kt`, `TranslationChunker.kt`, `TranslationChunk.kt`, `TranslationProgress.kt`
- [x] Remove `translateWithProgress()` from `AIApi.kt`
- [x] Remove `translateChunk()` from `AIClient.kt` interface
- [x] Remove `translateChunk()` from `OpenAICompatibleClient.kt` and `AnthropicClient.kt`
- [x] Remove all dead imports referencing deleted classes
- [x] Unit tests for `ParagraphTranslationCoordinator`
- [x] Unit tests for `ArticleTranslation` data model

#### Acceptance Criteria

- No references remain to `ChunkTranslationCoordinator`, `TranslationChunker`, `TranslationChunk`, `TranslationProgress`, `ChunkTranslationResult`, `translateChunk`, `translateWithProgress`
- Build compiles successfully with zero errors
- All existing tests pass
- New coordinator tests verify concurrency limiting, immediate emission, retry behavior, error classification
- New data model tests verify computed properties and list builder

#### Files Affected (MANDATORY)

**Created:**
- `app/src/test/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinatorTest.kt`
- `app/src/test/java/com/nononsenseapps/feeder/ai/ArticleTranslationTest.kt`

**Modified:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt` (remove `translateWithProgress()` and unused imports)
- `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` (remove `translateChunk()`)
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` (remove `translateChunk()` and imports)
- `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` (remove `translateChunk()` and imports)

**Deleted:**
- `app/src/main/java/com/nononsenseapps/feeder/ai/ChunkTranslationCoordinator.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunker.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunk.kt`
- `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationProgress.kt`

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| API rate limiting with 20 individual calls | Medium | Medium | Semaphore(3) limits concurrent calls; exponential backoff on 429 |
| Translation quality loss from isolated paragraphs | Medium | Low | Accept trade-off: per-paragraph speed vs cross-paragraph context |
| Race condition in concurrent StateFlow updates | Low | High | `MutableStateFlow.update{}` is atomic CAS — prevents lost updates |
| Compose recomposition overhead from frequent state updates | Low | Low | `key(paragraph.index)` in LazyColumn skips unchanged items |
| Build breakage from incomplete dead code removal | Low | Medium | Grep for all references to deleted classes before declaring done |

## Dependencies

### External Dependencies

- None — all required APIs (`Semaphore`, `channelFlow`) are in existing `kotlinx.coroutines` dependency

### Internal Dependencies

- `AIClient.translate()` — existing method, called with single-element `List<TranslatableText>` for per-paragraph translation
- `extractTranslatableParagraphs()` — existing method in `ArticleViewModel`, reused as-is
- `linearArticleContent()` — existing composable, `translatedParagraphs: List<String>?` parameter populated from `ArticleTranslation`

## Success Metrics

- [ ] 20-paragraph article shows first translated paragraph within 3-5 seconds (not waiting for all 20)
- [ ] Progress bar updates per-paragraph (20 distinct updates for 20 paragraphs)
- [ ] Partial failure: 18/20 successful translations displayed, 2 failed paragraphs show original text
- [ ] Cancellation: navigating away stops all in-flight requests
- [ ] Build compiles and all tests pass
- [ ] Zero references to deleted classes remaining in codebase
