# Task List - Auto Fetch Full Article Feature

**Feature ID:** 006
**Created:** 2026-01-01
**Status:** Ready for Execution

## Quick Reference

**Total Tasks:** 10
**Estimated Effort:** 14-19 hours
**Risk Level:** Low

---

## Task Checklist

### Data Layer (2 tasks)

- [ ] **T-001: Add String Resources**
  - File: `strings.xml`
  - Estimate: 15 min
  - Add 2 new strings for setting label and description

- [ ] **T-002: Add SettingsStore StateFlow**
  - File: `SettingsStore.kt`
  - Estimate: 1 hour
  - Add constant, StateFlow, and setter for auto-fetch setting

### View Model Layer (2 tasks)

- [ ] **T-003: Expose in SettingsViewModel**
  - File: `SettingsViewModel.kt`
  - Estimate: 1 hour
  - Add to ViewState, collection logic, and setter

- [ ] **T-005: Inject into ArticleViewModel**
  - File: `ArticleViewModel.kt`
  - Estimate: 30 min
  - Add SettingsStore dependency injection

### UI Layer (1 task)

- [ ] **T-004: Add Toggle to Settings Screen**
  - File: `Settings.kt`
  - Estimate: 2-3 hours
  - Add switch UI in syncing section

### Core Logic (1 task)

- [ ] **T-006: Implement Auto-Fetch Logic**
  - File: `ArticleViewModel.kt` (init block)
  - Estimate: 3-4 hours
  - Add auto-fetch trigger on article open

### Testing (3 tasks)

- [ ] **T-007: Write Unit Tests**
  - Files: `SettingsStoreTest.kt`, `ArticleViewModelTest.kt`
  - Estimate: 2-3 hours
  - Test persistence, auto-fetch trigger, edge cases

- [ ] **T-008: Write UI Tests**
  - File: `SettingsScreenTest.kt`
  - Estimate: 1-2 hours
  - Test toggle interaction

- [ ] **T-009: Integration Testing**
  - Manual & Automated
  - Estimate: 1-2 hours
  - End-to-end scenarios

### Polish (1 task)

- [ ] **T-010: Code Review & Refinement**
  - All files
  - Estimate: 1-2 hours
  - Lint, test, optimize, document

---

## Files to Modify

### Code Changes (5 files)
1. `app/src/main/res/values/strings.xml` (+2 lines)
2. `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` (+8 lines)
3. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt` (+5 lines)
4. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt` (+15 lines)
5. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (+20 lines)

**Total Lines to Add:** ~50 lines

### Test Files (2 files)
1. `app/src/test/java/com/nononsenseapps/feeder/archmodel/SettingsStoreTest.kt` (add tests)
2. `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelTest.kt` (NEW)

---

## Dependency Order

```
T-001 (Strings)
    ↓
T-002 (SettingsStore) → T-003 (ViewModel) → T-004 (Settings UI)
                            ↓
                       T-005 (Inject) → T-006 (Auto-Fetch Logic)
                                            ↓
                                      T-007 (Unit Tests)
                                            ↓
                                      T-008 (UI Tests)
                                            ↓
                                      T-009 (Integration)
                                            ↓
                                      T-010 (Review)
```

---

## Progress Tracking

### Completed Tasks (0/10)
- None yet

### In Progress (0/10)
- None yet

### Blocked (0/10)
- None

---

## Quick Links

- [Requirements Document](./01-requirements.md)
- [Research Report](./02-research-report.md)
- [Code Assessment](./03-code-assessment.md)
- [UI/UX Design](./04-ui-ux-design.md)
- [Technical Specification](./06-technical-specification.md)
- [Implementation Plan](./07-implementation-plan.md)

---

**Last Updated:** 2026-01-01
**Next Task:** T-001 - Add String Resources
