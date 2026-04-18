# Adversarial Review: 039-separate-translation-toggle

**Date**: 2026-04-18
**Reviewer**: Adversarial Agent
**Verdict**: **CONTESTED-accept**

---

## 1. Skeptic Lens

### 1.1 Could any code path bypass the `enableTranslation` gate?

**Severity: LOW (informational)**

The `translate()` function at `ArticleViewModel.kt:536` has **no internal guard** on `enableTranslation`. It is a public method. Currently, the only call site is `ArticleScreen.kt:199`, which is conditionally rendered inside `if (viewState.showTranslate)` at line 365. This means the UI gate is the sole barrier.

This matches the spec-038 pattern for `summarize()`, which also has no internal guard. The approach is consistent: the ViewModel's public API trusts the UI layer. However, if a future caller invokes `translate()` without checking `showTranslate`, translation will proceed even when the master toggle is OFF.

**Risk**: Very low today (single call site, both gated). Acceptable given pattern consistency with summary.

### 1.2 Are there race conditions with the combine flows?

**Severity: NONE**

The 13-flow `combine` at line 145 uses Kotlin's vararg `combine()` which emits whenever any input changes. The `enableTranslation` StateFlow is backed by `MutableStateFlow` (single-writer, atomic reads). There is no window where `showTranslate` could be stale relative to `enableTranslation`.

The auto-translate combine (4-flow, line 265) also uses `combine` with immediate propagation. The `enableTranslation &&` guard at line 280 is evaluated atomically with `translationEnabled`. No race.

### 1.3 Could toggling rapidly cause inconsistent state?

**Severity: NONE**

Rapid toggling of `enableTranslation`:
- **UI**: `showTranslate` reflects the latest value on every recomposition. Button appears/disappears instantly.
- **Auto-translate**: The combine emits on every change. If toggled OFF-ON rapidly, the `translationState.value is TranslationState.Empty` check prevents duplicate translation triggers once a translation has started.
- **In-progress translation**: Per spec edge case 7.1, an in-progress translation completes silently when toggled OFF. The button disappears but the translation result is preserved in state. This is correct behavior — no crash or leaked coroutine.

---

## 2. Architect Lens

### 2.1 Does the change increase coupling or complexity unreasonably?

**Severity: LOW**

The change adds exactly one new StateFlow through the existing `SettingsStore -> Repository -> ViewModel -> Screen` pipeline. This is the established pattern. No new abstractions, no new dependencies. Coupling is unchanged.

### 2.2 Is the 13-flow combine still manageable?

**Severity: MEDIUM (tech debt, not blocking)**

The viewState combine is now at 13 flows with `Array<Any>` parameter extraction using indexed casting (`params[12] as Boolean`). This is the same pattern used since the summary toggle was added. While it works correctly, the untyped array indexing is fragile:

- An off-by-one index produces a `ClassCastException` at runtime, not a compile error.
- Adding a 14th flow requires re-auditing all indices.

This is pre-existing technical debt from the summary implementation (spec-038). The translation toggle does not make it materially worse — it adds one more entry to an already-fragile pattern. A refactor to a typed data class or `combine` with explicit destructuring would be beneficial but is correctly out of scope for this spec.

### 2.3 Does the 4-flow auto-translate combine follow Kotlin best practices?

**Severity: LOW (minor style concern)**

The `data class AutoTranslateData` is defined **inside the lambda** of the `combine` transform at line 271. This is valid Kotlin — local data classes inside lambdas compile correctly. However, it means a new class instance is allocated per emission.

The previous implementation used `Triple`, which had the same allocation cost but was more idiomatic for 3 values. For 4 values, a local data class is the correct approach since Kotlin stdlib lacks `Quadruple`. The spec correctly notes this.

Minor concern: the `filterNotNull()` at line 278 is technically unnecessary because `AutoTranslateData` has no nullable top-level — the `combine` lambda always returns a non-null `AutoTranslateData`. However, `article` inside is nullable (`Article?`), so the filter is harmless and consistent with the summary auto-trigger pattern.

---

## 3. Minimalist Lens

### 3.1 Is anything over-engineered?

**Severity: NONE**

The implementation adds exactly what's needed:
- 1 MutableStateFlow + setter in SettingsStore (8 lines)
- 1 pass-through in Repository (4 lines)
- 1 StateFlow + setter in ViewModel (8 lines)
- 1 new SwitchSetting + `enabled=` param on existing toggle in Screen (12 lines)
- 2 guard conditions in ArticleViewModel (3 lines net)
- 1 OPML import case (1 line)
- 4 string resources (4 lines)
- 4 unit tests, 2 OPML test map entries

No new classes, no new packages, no new abstractions.

### 3.2 Are there unnecessary changes?

**Severity: LOW (informational)**

One minor observation: The `viewModelScope.launch` wrapper in `TranslationSettingsViewModel.setEnableTranslation()` (line 35-38) is technically unnecessary. `Repository.setEnableTranslation()` calls `SettingsStore.setEnableTranslation()` which is synchronous (MutableStateFlow assignment + SharedPreferences `apply()`). Neither operation is a suspend function. The coroutine launch adds overhead for a non-suspending call.

However, this matches the existing pattern for `setTranslationEnabled()` at line 29-32, which has the same unnecessary `viewModelScope.launch`. Changing this would break consistency. Acceptable.

### 3.3 Could the same be achieved with less code?

**Severity: NONE**

No. The two-toggle pattern requires:
1. A new preference key and StateFlow (irreducible)
2. UI gating via the combine (irreducible)
3. Auto-translate gating (irreducible)
4. OPML import/export (irreducible — the UserSettings enum handles export automatically)
5. Tests (irreducible)

The implementation is minimal.

### 3.4 Stale documentation

**Severity: LOW (informational)**

`docs/AI_SUMMARY_DEVELOPER_GUIDE.md` at line 52 still says:
```
│  - showTranslate: Boolean  (aiValid only)            │
```

This should now read `(enableTranslation && aiValid)` to match the updated logic. Not blocking since it's a documentation artifact, not code.

---

## 4. Overall Assessment

**Verdict: CONTESTED-accept**

The implementation is clean, minimal, and correctly mirrors the spec-038 pattern. The two findings that prevent a clean PASS are:

| # | Finding | Severity | Blocking? |
|:--|:--------|:---------|:----------|
| 1 | 13-flow combine with untyped array indexing is fragile (pre-existing debt) | MEDIUM | No |
| 2 | `translate()` has no internal `enableTranslation` guard (UI gate only) | LOW | No |
| 3 | `viewModelScope.launch` wraps synchronous call (pre-existing pattern) | LOW | No |
| 4 | Stale developer guide documentation | LOW | No |
| 5 | `filterNotNull()` on non-nullable type (harmless, consistent) | LOW | No |

None of these are introduced defects — findings 1, 3, and 5 are pre-existing patterns replicated for consistency. Finding 2 is a design choice consistent with the summary feature. Finding 4 is a documentation update that could be done in a follow-up.

**Recommendation**: Accept as-is. Optionally update the developer guide line 52 in a follow-up commit.
