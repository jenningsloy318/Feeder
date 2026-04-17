# 038 - Separate Summary Toggle: Handoff Document

**Date**: 2026-04-17
**Status**: Implementation complete, all 15 tasks done, awaiting commit and merge

---

## 1. What Was Done

Separated the summary master toggle ("Enable Summary") from the auto-summary toggle ("Auto Summary"). Previously, disabling "Auto Summary" disabled all summary functionality including the manual summarize button in article view. Now:

- **Enable Summary** (new) -- master switch controlling whether summary features are available at all
- **Auto Summary** (existing, renamed from "Enable Auto Summary") -- controls only automatic summarization on article open

This allows three user configurations:
1. Both ON: full auto-summary (existing behavior preserved)
2. Enable Summary ON, Auto Summary OFF: manual-only summarization via toolbar button
3. Enable Summary OFF: all summary features hidden, no requests sent

### Key Changes

1. **New `enableSummary` setting** added across the full data-flow stack:
   - `SettingsStore.kt`: `_enableSummary` MutableStateFlow + `PREF_ENABLE_SUMMARY` constant + `SETTING_ENABLE_SUMMARY` enum entry
   - `Repository.kt`: pass-through proxy property + setter
   - `SummarySettingsViewModel.kt`: exposed StateFlow + setter method
   - `SummarySettingsScreen.kt`: new "Enable Summary" SwitchSetting above Auto Summary; Auto Summary gets `enabled = enableSummary`

2. **`showSummarize` / `showTranslate` split** in `ArticleViewModel.kt`:
   - `showSummarize = enableSummary && aiValid` (gates summarize button)
   - `showTranslate = aiValid` (independent of enableSummary, prevents translate button from being accidentally hidden)
   - New `showTranslate` field added to `ArticleScreenViewState` interface and `ArticleState` data class

3. **Auto-summary gating**: `repository.enableSummary` added to the auto-summary combine in `ArticleViewModel.init`, with `enableSummary &&` prepended to the condition. This gates both global `summaryEnabled` AND per-feed `summarizeOnOpen`.

4. **SwitchSetting disabled state**: `Modifier.alpha(if (enabled) 1f else 0.38f)` added to the Row in `SwitchSetting` composable in `Settings.kt`. Uses Material 3 standard disabled content alpha (0.38f).

5. **ArticleScreen**: Translate button condition changed from `viewState.showSummarize` to `viewState.showTranslate` at line 365.

6. **OPML support**: `SETTING_ENABLE_SUMMARY` handled in `OPMLImporter.kt` for import; export is automatic via `UserSettings.entries` enumeration.

7. **String resources**: New `enable_summary_title`/"Enable Summary" and `enable_summary_description`/"Enable AI summary functionality". Existing `summary_enabled_title` renamed from "Enable Auto Summary" to "Auto Summary", description updated.

---

## 2. Key Decisions

| Decision | Rationale |
|:---|:---|
| `enableSummary` defaults to `true` | Backward compatibility (NFR-001): existing users see no behavior change on upgrade |
| Summarize button is **hidden** (not disabled/greyed) when Enable Summary OFF | Follows existing pattern where `showSummarize` controls visibility, not a disabled state |
| `showTranslate` split from `showSummarize` | Prevents translate button from being hidden when Enable Summary is OFF (R-4 from code assessment) |
| `enableSummary` gates both global `summaryEnabled` AND per-feed `summarizeOnOpen` | R-3: per-feed override should not bypass the master toggle |
| SwitchSetting disabled state uses `0.38f` alpha | Material 3 standard disabled content alpha; all existing callers unaffected (use default `enabled=true`) |
| No explicit cancellation of in-progress summary when toggling OFF | Low-priority edge case per FR-003 / spec section 4.1; button disappears, request completes silently |
| Language/timeout settings remain interactive when Enable Summary OFF | These configure how summaries behave when eventually re-enabled |

---

## 3. Files Changed

### Production (9 files)

| File | What Changed |
|:---|:---|
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` | New `_enableSummary` StateFlow, `PREF_ENABLE_SUMMARY` constant, `SETTING_ENABLE_SUMMARY` enum entry |
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` | New `enableSummary` proxy property + `setEnableSummary()` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsViewModel.kt` | New `enableSummary` StateFlow + `setEnableSummary()` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | 12th flow in viewState combine, `showSummarize`/`showTranslate` split, `enableSummary` in auto-summary init block, `showTranslate` in `ArticleScreenViewState` + `ArticleState` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt` | `alpha` modifier on `SwitchSetting` Row for disabled state |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt` | New "Enable Summary" toggle, Auto Summary `enabled = enableSummary` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | Translate button uses `viewState.showTranslate` instead of `viewState.showSummarize` |
| `app/src/main/java/com/nononsenseapps/feeder/model/opml/OPMLImporter.kt` | `SETTING_ENABLE_SUMMARY` case in import `when` block |
| `app/src/main/res/values/strings.xml` | 2 new strings + 2 renamed strings |

### Tests (3 files)

| File | What Changed |
|:---|:---|
| `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` | 4 new tests: default value, set false, set true, independence from summaryEnabled |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlParserTest.kt` | `SETTING_ENABLE_SUMMARY` added to mock setup and verification |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlWriterKtTest.kt` | `SETTING_ENABLE_SUMMARY` added to expected output and settings map |

### Documentation (2 files, also modified)

| File | What Changed |
|:---|:---|
| `docs/AI_SUMMARY_DEVELOPER_GUIDE.md` | Updated to reflect new Enable Summary setting |
| `docs/AI_SUMMARY_SETTINGS.md` | Updated settings documentation |

---

## 4. Test Results

- **4 new SettingsStore unit tests**: all pass
- **OPML parser/writer tests**: updated and passing
- **Build**: `./gradlew :app:compileFdroidDebugKotlin` passes
- **No ArticleViewModel unit tests** for `showSummarize`/`showTranslate` split or auto-summary gating: the ViewModel has no existing unit test infrastructure, and the spec marks these as requiring substantial mocking. Consistent with codebase conventions.

### Pre-existing Known Test Failures (not introduced by this spec)

1. `CustomFeederTextToolbarTest` -- pre-existing broken test
2. `MenuConfigStoreTest` -- 2 pre-existing broken tests
3. `CircleProgressIconButtonTest` -- pre-existing broken test
4. `JsonFeedParserTest > cowboyOnline` -- flaky, network-dependent

---

## 5. Unfinished Items

None. All 15 tasks (TASK-001 through TASK-015) completed.

---

## 6. Risks

- **No runtime guard in `summarize()` method**: The `summarize()` method itself doesn't check `enableSummary`. This is safe because the UI button is hidden when `enableSummary` is false, so there's no user path to invoke it. However, any future programmatic call (e.g., deep link, intent) would bypass the gate. This was noted as INFO severity in both code review and adversarial review.
- **12-flow combine fragility**: The viewState `combine` now takes 12 flows with index-based casting. This is pre-existing design debt amplified by one more parameter. No immediate issue.
- **Pre-existing broken tests**: 4 test failures exist independent of this spec. They should not block merge.

---

## 7. Code Review Status

- **Code Review**: Approved (no blocking findings, 4 low-severity informational notes)
- **Adversarial Review**: PASS (0 blocking issues across Skeptic, Architect, and Minimalist lenses)

---

## 8. What's Next

1. **Commit** the changes on branch `038-separate-summary-toggle`
2. **Merge** branch `038-separate-summary-toggle` into `ai-features`
3. **Clean up** the worktree after merge

---

## 9. Spec Index

- **Current spec**: 038
- **Next spec index**: 039
