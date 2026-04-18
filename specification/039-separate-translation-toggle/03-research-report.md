# 039 - Separate Translation Toggle: Research Report

**Date**: 2026-04-18
**Status**: Research complete

---

## 1. Approach

This feature is a direct mirror of spec-038 (Separate Summary Toggle), applied to translation instead of summary. Spec-038 established a proven two-toggle pattern — a master "Enable" toggle gating all functionality, with the existing auto-trigger toggle becoming a subordinate control. The same pattern applies here with minimal adaptation.

**Core change**: Replace `showTranslate = aiValid` with `showTranslate = enableTranslation && aiValid`, and gate the auto-translate combine with `enableTranslation &&`.

---

## 2. Precedent: Spec-038 Pattern

Spec-038 successfully implemented the two-toggle pattern for summary. The data flow is fully established:

```
SettingsStore._enableSummary (MutableStateFlow)
  → Repository.enableSummary (pass-through proxy)
    → SummarySettingsViewModel.enableSummary (exposed StateFlow)
      → SummarySettingsScreen (new toggle + enabled= on Auto Summary)
    → ArticleViewModel.viewState combine (param[11], gates showSummarize)
    → ArticleViewModel.init auto-summary combine (gates condition)
      → ArticleScreen (showSummarize controls button visibility)
```

**Translation mirror**:

```
SettingsStore._enableTranslation (new MutableStateFlow)
  → Repository.enableTranslation (new pass-through proxy)
    → TranslationSettingsViewModel.enableTranslation (new exposed StateFlow)
      → TranslationSettingsScreen (new toggle + enabled= on Auto Translation)
    → ArticleViewModel.viewState combine (param[12], gates showTranslate)
    → ArticleViewModel.init auto-translate combine (gates condition)
      → ArticleScreen (showTranslate controls button visibility)
```

### Existing Code Points to Modify

| Layer | File | Current State | Change |
|:------|:-----|:-------------|:-------|
| SettingsStore | `SettingsStore.kt` | Has `_translationEnabled` (auto-translate) | Add `_enableTranslation` + `PREF_ENABLE_TRANSLATION` + `SETTING_ENABLE_TRANSLATION` |
| Repository | `Repository.kt` | Has `translationEnabled` proxy | Add `enableTranslation` proxy + setter |
| ViewModel (settings) | `TranslationSettingsViewModel.kt` | Exposes `translationEnabled` | Add `enableTranslation` StateFlow + setter |
| Settings UI | `TranslationSettingsScreen.kt` | Single "Enable Auto Translation" toggle | Add "Enable Translation" above, rename existing to "Auto Translation", add `enabled=` |
| ArticleViewModel | `ArticleViewModel.kt:173` | `showTranslate = aiValid` | `showTranslate = enableTranslation && aiValid` |
| ArticleViewModel | `ArticleViewModel.kt:~263` | Auto-translate combine with `translationEnabled` | Add `enableTranslation &&` to condition |
| OPML | `OPMLImporter.kt` | Has `SETTING_SUMMARY_ENABLED` etc. | Add `SETTING_ENABLE_TRANSLATION` case |
| Strings | `strings.xml` | Has `translation_enabled_title` = "Enable Auto Translation" | Add `enable_translation_title`/`description`, rename existing |

---

## 3. Options Analysis

### Option A (Recommended): Mirror spec-038 pattern exactly

Mirror the `enableSummary` implementation 1:1 for translation:

- New `_enableTranslation` MutableStateFlow in SettingsStore (default `true`)
- Pass-through in Repository
- Exposed in TranslationSettingsViewModel
- New "Enable Translation" toggle in TranslationSettingsScreen above Auto Translation
- `showTranslate = enableTranslation && aiValid` in ArticleViewModel viewState combine
- `enableTranslation &&` prepended to auto-translate condition in init block
- OPML import/export support
- Auto Translation toggle greyed out (0.38f alpha) when Enable Translation is OFF

**Pros**: Proven pattern, consistency with summary, minimal design decisions needed, all edge cases already vetted in spec-038.

**Cons**: Inherits the same limitations as spec-038 (no runtime guard in `translate()` method, combine parameter count increases).

### Option B (Rejected): Add runtime guard in translate() method

Add an `if (!repository.enableTranslation.value) return` check at the top of `translate()`.

**Rejected**: Same reasoning as spec-038 — the UI hides the button, so there is no user path to invoke translate when disabled. A runtime guard adds defense-in-depth but is inconsistent with the summary pattern and adds unnecessary complexity. If a future programmatic call path is added, it can be gated at that point.

### Option C (Rejected): Disable button instead of hide

Show the translate button in a disabled/greyed state instead of hiding it when Enable Translation is OFF.

**Rejected**: The existing pattern is visibility-based (`showTranslate` / `showSummarize`), not enabled-state-based. Changing to a disabled button would be inconsistent with summary and would require different UI handling in ArticleScreen. The hide pattern is established and users expect it.

---

## 4. Recommendation

**Option A**: Direct mirror of spec-038 pattern. This is the lowest-risk, most consistent approach. The pattern is fully proven, the code paths are well-understood, and the implementation is mechanical.

---

## 5. Risks

| Risk | Severity | Mitigation |
|:-----|:---------|:-----------|
| No runtime guard in `translate()` | INFO | UI button is hidden when `enableTranslation` is false; no user path to invoke. Same as spec-038 decision. |
| viewState combine grows to 13 flows | INFO | Pre-existing design debt (was 12 after spec-038). Index-based casting remains fragile but is the established pattern. |
| Pre-existing broken tests | INFO | 4 known test failures unrelated to this spec: `CustomFeederTextToolbarTest`, `MenuConfigStoreTest` (2), `CircleProgressIconButtonTest`. Should not block. |
| String rename collision | LOW | `translation_enabled_title` currently used for auto-translate. Must rename existing string before adding new one. Same approach as spec-038 renaming `summary_enabled_title`. |

---

## 6. Estimated Scope

Based on spec-038 as a reference (9 production files, 3 test files), the translation mirror is expected to touch:

- **Production**: ~8-9 files (SettingsStore, Repository, TranslationSettingsViewModel, TranslationSettingsScreen, ArticleViewModel, ArticleScreen if needed, OPMLImporter, strings.xml, docs)
- **Tests**: ~3 files (SettingsStoreTest, OpmlParserTest, OpmlWriterKtTest)
- **Estimated effort**: Small — mechanical pattern replication with no design ambiguity
