# 038 - Separate Summary Toggle: Adversarial Review

**Verdict: PASS**

The implementation correctly solves the stated problem with minimal, well-targeted changes across the expected layers. No blocking issues found. A few minor observations are noted below.

---

## Lens 1: Skeptic

### Finding S-1: Manual `summarize()` has no runtime guard on `enableSummary` — Severity: INFO

The `summarize()` method at `ArticleViewModel.kt:489` does not check `enableSummary` before firing a network request. This is **acceptable** because the summarize button is only rendered when `viewState.showSummarize` is true (`ArticleScreen.kt:340`), meaning the user has no UI path to invoke it when `enableSummary` is false.

However, if any future code path calls `summarize()` programmatically (e.g., a deep link or an intent), it would bypass the gate. This is an informational note, not a defect — defense-in-depth at the method level would be an optional hardening step.

### Finding S-2: Auto-summary collect loop runs continuously even when `enableSummary` is false — Severity: INFO

The `combine(articleFlow, summaryEnabled, enableSummary)` collector at `ArticleViewModel.kt:237-258` continues collecting even when `enableSummary` is false. This is fine — the check at line 248 short-circuits before any work. The StateFlow-based approach means no unnecessary emissions, and the coroutine is lightweight. No behavioral issue.

### Finding S-3: In-progress summary is NOT cancelled when `enableSummary` toggles OFF — Severity: LOW

Per FR-003, cancelling an in-progress summary when toggling `enableSummary` OFF is "low priority." The spec at section 4.1 explicitly documents this as acceptable behavior. The button disappears from the UI, and the result is stored but inaccessible via UI, which matches SCENARIO-021 (cached summaries remain accessible). The already-sent network request completes without user-facing effect.

**Assessment: Consistent with specification, acknowledged trade-off.**

### Finding S-4: All four matrix states verified — Severity: PASS

Traced through the settings behavior matrix from the requirements:

| Enable Summary | Auto Summary | showSummarize | Auto-Summarize on Open | Auto Summary Toggle |
|:-:|:-:|:-:|:-:|:-:|
| ON | ON | `enableSummary && aiValid` = true | `enableSummary && summaryEnabled` = true | `enabled=true` |
| ON | OFF | `enableSummary && aiValid` = true | `enableSummary && false` = false | `enabled=true` |
| OFF | ON (stored) | `false && aiValid` = false | `false && ...` = false | `enabled=false` |
| OFF | OFF (stored) | `false && aiValid` = false | `false && ...` = false | `enabled=false` |

All four rows produce correct behavior per AC-001 through AC-006.

### Finding S-5: Per-feed `summarizeOnOpen` gated by `enableSummary` — Severity: PASS

Line 248-249 of `ArticleViewModel.kt` correctly gates with `enableSummary && (summaryEnabled || feed?.summarizeOnOpen == true)`. This means `enableSummary=false` blocks even per-feed auto-summarize, which is the correct behavior per the requirements.

---

## Lens 2: Architect

### Finding A-1: Clean layering, follows existing patterns — Severity: PASS

The change flows through the expected three layers:
- **Data**: `SettingsStore` (new `_enableSummary` MutableStateFlow + setter + constant + enum) → follows the exact pattern of `_summaryEnabled` at lines 817-824.
- **Repository**: Direct proxy (lines 389-391) → follows the exact pattern of `summaryEnabled` proxy at lines 384-386.
- **ViewModel**: `SummarySettingsViewModel` exposes flow + setter (lines 25, 35-38) → mirrors the `summaryEnabled` pattern.
- **UI**: `SummarySettingsScreen` collects state and passes `enabled=enableSummary` to the sub-toggle.

No new abstractions, no new patterns. This is textbook consistency.

### Finding A-2: `showTranslate` / `showSummarize` split is correct — Severity: PASS

Previously, `showSummarize` was used for both the summarize and translate buttons (`ArticleScreen.kt` lines 340 and 365 historically). The split into two flags prevents the new `enableSummary` gate from accidentally hiding the translate button. The `showTranslate = aiValid` derivation at `ArticleViewModel.kt:173` correctly keeps translation independent of the summary toggle.

This is the kind of coupling that could have been a subtle regression — well handled.

### Finding A-3: `combine` with 12 flows uses vararg form correctly — Severity: INFO

The `combine` at `ArticleViewModel.kt:145-158` now takes 12 flows via the `Array<Any?>` vararg overload. Kotlin's `combine` supports up to arbitrary N flows in this form. The type casts at lines 159-175 follow the existing pattern (e.g., `params[0] as Article?`). The new param is added at index 11, matching the 12th position. No off-by-one issues.

### Finding A-4: OPML import/export covers `enableSummary` — Severity: PASS

- `SETTING_ENABLE_SUMMARY` is present in the `UserSettings` enum at `SettingsStore.kt:1122`.
- `OPMLImporter.kt:164` handles `SETTING_ENABLE_SUMMARY` with `settingsStore.setEnableSummary(value.toBoolean())`.
- Export is handled by `getAllSettings()` which iterates over `UserSettings.entries` — the new entry is included automatically.
- Missing key on import defaults to `true` via `SharedPreferences.getBoolean(PREF_ENABLE_SUMMARY, true)`.

Backward-compatible with older OPML files.

### Finding A-5: No coupling leaks to AI layer — Severity: PASS

Per NFR-002, no changes to `AIClient`, `AIApi`, `OpenAICompatibleClient`, `AnthropicClient`, or summary parsing. Verified by inspecting the diff — only 8 files were modified, all in settings/ViewModel/UI layers.

---

## Lens 3: Minimalist

### Finding M-1: Change is proportional to the task — Severity: PASS

The spec estimated ~80-100 LOC across 8 existing files. The implementation touches exactly the 8 files listed:
1. `SettingsStore.kt` — ~10 lines (field + constant + enum entry)
2. `Repository.kt` — ~4 lines (proxy)
3. `ArticleViewModel.kt` — ~10 lines (12th combine param + `showTranslate` split + auto-summary gate)
4. `SummarySettingsViewModel.kt` — ~8 lines (flow + setter)
5. `SummarySettingsScreen.kt` — ~15 lines (new toggle + `enabled` param)
6. `Settings.kt` — ~2 lines (alpha modifier + import)
7. `ArticleScreen.kt` — ~1 line (`showTranslate` instead of `showSummarize`)
8. `OPMLImporter.kt` — ~1 line (new case in when block)
9. `strings.xml` — ~4 lines (new strings + rename)

No new files were created. No unnecessary abstractions. The `SwitchSetting` disabled alpha change at `Settings.kt:1323` is reusable across any future disabled toggle — a proportional, non-speculative enhancement.

### Finding M-2: No unnecessary side effects — Severity: PASS

- Translation behavior is fully unchanged (confirmed by `showTranslate = aiValid` derivation).
- Blocklist, summary language, timeout settings remain interactive when `enableSummary` is OFF — this is correct per spec section 3.8 ("Language and timeout settings remain unchanged and interactive regardless of `enableSummary`").
- No new coroutines, no new background work, no new network calls (NFR-003 satisfied).

### Finding M-3: String rename is cosmetic-only — Severity: PASS

The rename of `summary_enabled_title` from "Enable Auto Summary" to "Auto Summary" and `summary_enabled_description` from whatever it was to "Automatically summarize articles when opened" is a UI clarity improvement. The preference key `PREF_SUMMARY_ENABLED` is unchanged, so no stored data is affected.

---

## Summary

| Lens | Findings | Blocking Issues |
|:---|:---|:---|
| Skeptic | S-1 through S-5 | 0 |
| Architect | A-1 through A-5 | 0 |
| Minimalist | M-1 through M-3 | 0 |

The implementation is a clean, minimal, well-layered change that correctly solves the stated problem. All acceptance criteria are met. The `showTranslate` / `showSummarize` split correctly prevents an accidental regression. OPML import/export is covered. Backward compatibility is preserved via the `true` default. No issues warrant blocking.
