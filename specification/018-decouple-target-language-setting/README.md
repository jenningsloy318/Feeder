# Specification 018: Decouple Target Language Settings

**Status:** Ready for Implementation
**Created:** 2026-01-05
**Complexity:** Low
**Estimated Time:** 30-60 minutes

---

## Quick Summary

**Problem:** Language selectors are disabled when auto-summary/auto-translation features are turned off, preventing users from configuring target languages for manual operations.

**Solution:** Remove the `enabled` parameter dependency from `LanguageSelectorSetting` components, allowing language selection at any time.

**Impact:** Minimal code change (6 lines) with significant usability improvement for manual workflow users.

---

## Specification Documents

### Core Specification

| Document | Description | Status |
|----------|-------------|--------|
| **[06-specification.md](./06-specification.md)** | Comprehensive technical specification with architecture, design, and implementation details | ✅ Complete |
| **[07-implementation-plan.md](./07-implementation-plan.md)** | Step-by-step implementation plan with milestones and acceptance criteria | ✅ Complete |
| **[08-task-list.md](./08-task-list.md)** | Detailed task breakdown with dependencies and verification checklist | ✅ Complete |

### Supporting Documents

| Document | Description | Status |
|----------|-------------|--------|
| **[05-design-spec.md](./05-design-spec.md)** | UI/UX design specification with visual mockups and interaction flows | ✅ Complete |

---

## Implementation at a Glance

### What's Changing

**Files Modified:** 2
- `SummarySettingsScreen.kt` - Remove `enabled` parameter (3 lines)
- `TranslationSettingsScreen.kt` - Remove `enabled` parameter (3 lines)

**Total Changes:** 6 lines (4 removed, 2 modified)

### Change Summary

```diff
# SummarySettingsScreen.kt (Line 101, 114, 125)

# Before:
LanguageSelectorSetting(
    title = stringResource(R.string.summary_language_title),
    currentLanguage = summaryLanguage,
    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
-   enabled = summaryEnabled,  # ← REMOVED
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)

# After:
LanguageSelectorSetting(
    title = stringResource(R.string.summary_language_title),
    currentLanguage = summaryLanguage,
    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
    # enabled parameter removed
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

**Same pattern applied to:**
- `TranslationSettingsScreen.kt` (lines 101, 114, 125)

---

## Quick Start Guide

### For Developers

1. **Read the Specification:** Start with [06-specification.md](./06-specification.md)
2. **Review the Plan:** Check [07-implementation-plan.md](./07-implementation-plan.md)
3. **Follow the Tasks:** Execute tasks from [08-task-list.md](./08-task-list.md)

### Implementation Checklist

- [ ] Remove `enabled` parameter from `LanguageSelectorSetting` in `SummarySettingsScreen.kt` (3 locations)
- [ ] Remove `enabled` parameter from `LanguageSelectorSetting` in `TranslationSettingsScreen.kt` (3 locations)
- [ ] Build project: `./gradlew assembleDebug`
- [ ] Run tests: `./gradlew test`
- [ ] Manual QA: Verify language selector enabled when switch is OFF
- [ ] Accessibility check: Verify TalkBack navigation works
- [ ] Commit changes with descriptive message

---

## Key Design Decisions

### Decision 1: Always Enable Language Selector

**Chosen Approach:** Remove `enabled` parameter entirely (Option 1 from design spec)

**Rationale:**
- Maximum user control and flexibility
- Simplest implementation (single parameter removal)
- No visual clutter from conditional states
- Supports manual workflow users perfectly
- Follows Android best practices for settings

**Alternatives Considered:**
- Option 2: Always enabled with description text
- Option 3: Always enabled with visual indicator
- Option 4: Keep current behavior (rejected)
- Option 5: Always enabled with tooltips

**Outcome:** Option 1 selected for simplicity and effectiveness

---

## Risk Assessment

### Risk Level: **Very Low**

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Compilation error | Very Low | Low | Simple code change, well-defined |
| Test failure | Low | Low | Existing tests should pass |
| Visual regression | Very Low | Low | No visual changes |
| Accessibility issue | Very Low | Medium | Removing disabled state improves access |
| User confusion | Low | Low | Behavior more intuitive |

**Overall Risk:** This is a low-risk change with minimal complexity and easy rollback.

---

## Testing Strategy

### Automated Tests

```bash
# Build project
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run UI tests (requires emulator)
./gradlew connectedAndroidTest
```

### Manual Testing

1. Open **Summary Settings** screen
2. Verify **auto-summary is OFF**
3. Tap **"Summary Language"** → Should open dropdown (enabled)
4. Select a different language → Should save
5. Enable **auto-summary toggle**
6. Verify language selector still enabled

Repeat for **Translation Settings** screen.

### Accessibility Testing

1. Enable **TalkBack** (screen reader)
2. Navigate to language selector
3. Verify announced as "Button, Double tap to activate" (not "Disabled")
4. Tap to open menu
5. Verify keyboard navigation works (Tab/Enter)

---

## Success Criteria

### Functional Requirements

- ✅ Language selector enabled when auto-feature is OFF
- ✅ Language selector enabled when auto-feature is ON
- ✅ Dropdown menu opens in all states
- ✅ Language selection saves in all states
- ✅ Switch toggle doesn't affect language selector

### Non-Functional Requirements

- ✅ No compilation errors
- ✅ All existing tests pass
- ✅ No visual regressions
- ✅ WCAG 2.1 AA accessibility maintained
- ✅ No performance degradation

---

## Rollback Plan

**If Issues Arise:**

```bash
# Revert commit
git revert <commit-hash>
git push

# Build hotfix
./gradlew assembleDebug

# Deploy to Play Store
```

**Rollback Risk:** Low - Simple revert of 6 lines

---

## References

### Source Code

**Files to Modify:**
1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
2. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`

**Related Files (No Changes):**
- `SummarySettingsViewModel.kt`
- `TranslationSettingsViewModel.kt`
- `SummaryLanguage.kt` (enum)
- `TranslationLanguage.kt` (enum)

### External Resources

- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)
- [Material3 Design Guidelines](https://m3.material.io/)
- [Android Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)

---

## Metadata

**Specification Index:** 018
**Feature:** Decouple Target Language Settings from Auto-Feature Dependencies
**Author:** Specification Writer Agent
**Reviewers:** Pending
**Approval Status:** Pending

**Related Specifications:**
- [Spec 014](../014-translation-function/) - Translation Function
- [Spec 015](../015-selection-menu-config/) - Selection Menu Config

---

## Changelog

### 2026-01-05

- ✅ Created comprehensive technical specification (06-specification.md)
- ✅ Created implementation plan (07-implementation-plan.md)
- ✅ Created detailed task list (08-task-list.md)
- ✅ Created README as entry point (this document)
- ⏳ Awaiting implementation

---

## Next Steps

1. **Review:** All specification documents reviewed by stakeholders
2. **Approval:** Implementation plan approved
3. **Execution:** Follow task list (08-task-list.md) sequentially
4. **Testing:** Verify all acceptance criteria
5. **Deployment:** Merge to main branch and release

---

**Last Updated:** 2026-01-05
**Document Version:** 1.0.0
**Status:** ✅ Ready for Implementation
