# 039 - Separate Translation Toggle: Handoff Document

**Date**: 2026-04-18
**Status**: Implementation complete, all 11 tasks done, awaiting commit and merge

---

## 1. What Was Done

Added a master "Enable Translation" toggle that controls whether any translation functionality is available. The existing "Enable Auto Translation" toggle is renamed to "Auto Translation" and becomes a subordinate control that is only interactive when the master toggle is ON. This mirrors the spec-038 two-toggle pattern for summary.

Now users have three translation configurations:
1. Both ON: full auto-translation (existing behavior preserved)
2. Enable Translation ON, Auto Translation OFF: manual-only translation via toolbar button
3. Enable Translation OFF: all translation features hidden, no requests sent

### Key Changes

1. **New `enableTranslation` setting** added across the full data-flow stack:
   - `SettingsStore.kt`: `_enableTranslation` MutableStateFlow + `PREF_ENABLE_TRANSLATION` constant + `SETTING_ENABLE_TRANSLATION` enum entry
   - `Repository.kt`: pass-through proxy property + setter
   - `TranslationSettingsViewModel.kt`: exposed StateFlow + setter method
   - `TranslationSettingsScreen.kt`: new "Enable Translation" SwitchSetting above Auto Translation; Auto Translation gets `enabled = enableTranslation`

2. **`showTranslate` gating** in `ArticleViewModel.kt`:
   - `showTranslate = enableTranslation && aiValid` (was `showTranslate = aiValid`)
   - `repository.enableTranslation` added as 13th flow (index 12) in the viewState combine

3. **Auto-translate gating**: `repository.enableTranslation` added as 4th flow in the auto-translate combine in `ArticleViewModel.init`, with `enableTranslation &&` prepended to the condition

4. **OPML support**: `SETTING_ENABLE_TRANSLATION` handled in `OPMLImporter.kt` for import; export is automatic via `UserSettings.entries` enumeration

5. **String resources**: New `enable_translation_title`/"Enable Translation" and `enable_translation_description`/"Enable AI translation functionality". Existing `translation_enabled_title` renamed from "Enable Auto Translation" to "Auto Translation", description updated

6. **Documentation updated**: `AI_SUMMARY_DEVELOPER_GUIDE.md` and `AI_SUMMARY_SETTINGS.md` updated to reflect the new two-toggle translation system

---

## 2. Key Decisions

| Decision | Rationale |
|:---|:---|
| `enableTranslation` defaults to `true` | Backward compatibility: existing users see no behavior change on upgrade |
| Translate button is **hidden** (not disabled/greyed) when Enable Translation OFF | Follows existing pattern where `showTranslate` controls visibility, not a disabled state |
| `enableTranslation` gates auto-translate via `enableTranslation && translationEnabled` | Master toggle should override the subordinate auto-translate toggle |
| No per-feed `translateOnOpen` changes | Per-feed `translateOnOpen` does not exist in the codebase; auto-translate uses global `translationEnabled` only. FR-005/AC-008 are effectively no-ops |
| Language/timeout settings remain interactive when Enable Translation OFF | These configure how translation behaves when eventually re-enabled |
| No explicit cancellation of in-progress translation when toggling OFF | Low-priority edge case; button disappears, request completes silently. Same pattern as spec-038 summary |
| Auto-translate combine uses local `AutoTranslateData` data class | Kotlin stdlib lacks `Quadruple`; local data class is cleanest for 4-field destructuring |

---

## 3. Files Changed

### Production (7 files)

| File | What Changed |
|:---|:---|
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` | New `_enableTranslation` StateFlow, `PREF_ENABLE_TRANSLATION` constant, `SETTING_ENABLE_TRANSLATION` enum entry |
| `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt` | New `enableTranslation` proxy property + `setEnableTranslation()` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModel.kt` | New `enableTranslation` StateFlow + `setEnableTranslation()` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt` | New "Enable Translation" master toggle, Auto Translation `enabled = enableTranslation` |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | 13th flow in viewState combine, `showTranslate` gated by `enableTranslation`, auto-translate combine gated by `enableTranslation` |
| `app/src/main/java/com/nononsenseapps/feeder/model/opml/OPMLImporter.kt` | `SETTING_ENABLE_TRANSLATION` case in import `when` block |
| `app/src/main/res/values/strings.xml` | 2 new strings + 2 renamed strings |

### Documentation (2 files)

| File | What Changed |
|:---|:---|
| `docs/AI_SUMMARY_DEVELOPER_GUIDE.md` | Updated to reflect two-toggle translation system, `showTranslate` gating logic |
| `docs/AI_SUMMARY_SETTINGS.md` | Updated FAQ and feature descriptions for Enable Translation toggle |

### Tests (3 files)

| File | What Changed |
|:---|:---|
| `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` | 4 new tests: default value, set false, set true, independence from translationEnabled |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlParserTest.kt` | `SETTING_ENABLE_TRANSLATION` added to mock setup and verification |
| `app/src/test/java/com/nononsenseapps/feeder/model/opml/OpmlWriterKtTest.kt` | `SETTING_ENABLE_TRANSLATION` added to expected output and settings map |

---

## 4. Test Results

- **4 new SettingsStore unit tests**: all pass
- **OPML parser/writer tests**: updated and passing
- **Build**: `./gradlew :app:compileFdroidDebugKotlin` passes
- **Test suite**: 509 total tests
- **No ArticleViewModel unit tests** for `showTranslate` gating or auto-translate gating: the ViewModel has no existing unit test infrastructure. Consistent with codebase conventions.

### Pre-existing Known Test Failures (not introduced by this spec)

1. `CircleProgressIconButtonTest` -- pre-existing broken test
2. `CustomFeederTextToolbarTest` -- pre-existing broken test
3. `MenuConfigStoreTest` -- 2 pre-existing broken tests

---

## 5. Unfinished Items

None. All 11 tasks (TASK-001 through TASK-011) completed.

---

## 6. Risks

- **No runtime guard in `translate()` method**: The `translate()` method itself doesn't check `enableTranslation`. This is safe because the UI button is hidden when `enableTranslation` is false, so there's no user path to invoke it. However, any future programmatic call would bypass the gate. Consistent with the spec-038 summary pattern. (Adversarial review finding 1.1, LOW severity)
- **13-flow combine fragility**: The viewState `combine` now takes 13 flows with index-based casting. This is pre-existing design debt amplified by one more parameter. An off-by-one index produces a `ClassCastException` at runtime, not a compile error. (Adversarial review finding 2.2, MEDIUM severity, not blocking)
- **Pre-existing broken tests**: 4 test failures exist independent of this spec. They should not block merge.

---

## 7. Code Review Status

- **Code Review**: Approved (no blocking findings; 2 low-severity notes, 3 informational notes)
- **Adversarial Review**: CONTESTED-accept (0 blocking issues; 1 medium pre-existing tech debt note, 4 low/informational notes)

---

## 8. What's Next

1. **Commit** the changes on branch `039-separate-translation-toggle`
2. **Merge** branch `039-separate-translation-toggle` into `ai-features`
3. **Clean up** the worktree after merge

---

## 9. Spec Index

- **Current spec**: 039
- **Next spec index**: 040
