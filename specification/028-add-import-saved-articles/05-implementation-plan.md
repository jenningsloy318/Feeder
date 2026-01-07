# Implementation Plan: Import Saved Articles Feature

**Spec ID:** 028-add-import-saved-articles
**Date:** 2026-01-07
**Status:** READY
**Estimated Effort:** 4-6 hours

---

## 1. Implementation Strategy

### 1.1 Approach

**Phased Implementation:**
1. **Phase A:** Core functionality (database + business logic)
2. **Phase B:** UI integration
3. **Phase C:** Testing and polish
4. **Phase D:** Translations and documentation

**Incremental Delivery:**
- Each phase builds on the previous
- Can stop after Phase A for basic functionality
- Each phase is independently testable

### 1.2 Risk Mitigation

**Technical Risks:**
- Performance: Use batch operations (mitigated)
- Blocking UI: Use coroutines properly (mitigated)
- Memory issues: Monitor file size (mitigated)

**Process Risks:**
- Scope creep: Clear acceptance criteria (mitigated)
- Testing gaps: Follow export test pattern (mitigated)
- Translation delays: Can ship with English first (mitigated)

---

## 2. Task Breakdown

### 2.1 Phase A: Core Functionality (Priority: HIGH)

#### Task A1: Add Database Methods

**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItemDao.kt`

**Steps:**
1. Add data class `FeedItemIdLinkPair`
2. Add `getFeedItemIdsByLinks()` method
3. Add `setBookmarked(ids: List<Long>)` method
4. Run database tests to verify

**Acceptance:**
- [ ] Code compiles without errors
- [ ] Database tests pass
- [ ] No ktlint violations
- [ ] Methods have KDoc comments

**Estimated Time:** 30 minutes

---

#### Task A2: Create Import Logic

**File:** `app/src/main/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporter.kt` (NEW)

**Steps:**
1. Create new file with package declaration
2. Add error sealed class hierarchy
3. Add `ImportResult` data class
4. Implement `importSavedArticles()` function
5. Implement `readUrlsFromFile()` helper
6. Add error handling with Either monad
7. Add logging with performance timing

**Acceptance:**
- [ ] Code compiles without errors
- [ ] Follows OPML import pattern
- [ ] Error handling covers all cases
- [ ] Proper use of coroutines
- [ ] KDoc comments on all public APIs

**Estimated Time:** 1.5 hours

---

#### Task A3: Add Basic String Resources

**File:** `app/src/main/res/values/strings.xml`

**Steps:**
1. Add all English strings for import
2. Add plurals for count messages
3. Verify no string ID conflicts

**Acceptance:**
- [ ] All required strings added
- [ ] Proper string formatting (%1$s, etc.)
- [ ] Plurals defined correctly

**Estimated Time:** 15 minutes

---

### 2.2 Phase B: UI Integration (Priority: HIGH)

#### Task B1: Add UI Launcher

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`

**Steps:**
1. Add import statement for `importSavedArticles`
2. Add `savedArticleImporter` launcher
3. Add coroutine scope handling
4. Add error toast handling

**Acceptance:**
- [ ] Code compiles without errors
- [ ] Launcher pattern matches OPML importer
- [ ] Proper coroutine scope usage
- [ ] Error handling in place

**Estimated Time:** 20 minutes

---

#### Task B2: Add Menu Item

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt`

**Steps:**
1. Locate export menu item (line ~355)
2. Add import menu item nearby
3. Add appropriate icon
4. Add error handling for no file manager

**Acceptance:**
- [ ] Menu item visible in UI
- [ ] Menu item text displays correctly
- [ ] Clicking opens file picker
- [ ] Error handling works

**Estimated Time:** 20 minutes

---

### 2.3 Phase C: Testing (Priority: HIGH)

#### Task C1: Write Integration Test

**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/model/export/ImportSavedTest.kt` (NEW)

**Steps:**
1. Create test file matching `ExportSavedTest.kt` pattern
2. Setup DI and in-memory database
3. Test basic import (3 URLs, 2 match)
4. Test duplicate handling
5. Test empty file
6. Test invalid URLs
7. Test nonexistent URLs

**Acceptance:**
- [ ] All tests pass
- [ ] Test coverage matches export tests
- [ ] Edge cases covered
- [ ] Proper assertions

**Estimated Time:** 1 hour

---

#### Task C2: Manual Testing

**Steps:**
1. Build and install debug APK
2. Export saved articles from test device
3. Import the exported file
4. Verify all articles are bookmarked
5. Test error scenarios (invalid file, empty file)
6. Test performance with 1,000 URLs

**Acceptance:**
- [ ] Basic import works end-to-end
- [ ] Error messages display correctly
- [ ] Performance is acceptable
- [ ] No crashes or ANRs

**Estimated Time:** 30 minutes

---

### 2.4 Phase D: Polish (Priority: MEDIUM)

#### Task D1: Add Translations

**Files:** `app/src/main/res/values-*/strings.xml` (30+ files)

**Steps:**
1. For each language file:
   - Add all new string IDs
   - Translate appropriately (can use machine translation initially)
   - Verify string formatting

**Acceptance:**
- [ ] All language files updated
- [ ] No missing string IDs
- [ ] Proper formatting maintained

**Estimated Time:** 2 hours (or can ship with English first)

---

#### Task D2: Add Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/model/export/SavedArticlesImporterTest.kt` (NEW)

**Steps:**
1. Test URL validation logic
2. Test duplicate removal
3. Test empty line filtering
4. Test whitespace trimming

**Acceptance:**
- [ ] All unit tests pass
- [ ] Edge cases covered
- [ ] Proper assertions

**Estimated Time:** 30 minutes

---

#### Task D3: Update Documentation

**Files:**
- `CHANGELOG.md`
- `README.md` (if applicable)

**Steps:**
1. Add entry to CHANGELOG.md
2. Update feature list in README if needed
3. Add any user-facing documentation

**Acceptance:**
- [ ] CHANGELOG updated
- [ ] Follows existing format
- [ ] Clear description of feature

**Estimated Time:** 15 minutes

---

## 3. Implementation Order

### 3.1 Critical Path

```
A1 (Database Methods)
    ↓
A2 (Import Logic)
    ↓
A3 (String Resources)
    ↓
B1 (UI Launcher)
    ↓
B2 (Menu Item)
    ↓
C1 (Integration Tests)
    ↓
C2 (Manual Testing)
```

**Total Critical Path Time:** ~4.5 hours

### 3.2 Parallel Opportunities

- **D1 (Translations)** can be done in parallel with C1/C2
- **D2 (Unit Tests)** can be done in parallel with B1/B2
- **D3 (Documentation)** can be done at any time

---

## 4. Milestones

### Milestone 1: Core Functionality Complete
**Criteria:**
- ✅ A1, A2, A3 complete
- ✅ Code compiles
- ✅ Integration test passes
- ✅ Can import via code (no UI yet)

**Estimated:** 2.5 hours

### Milestone 2: UI Integration Complete
**Criteria:**
- ✅ Milestone 1 complete
- ✅ B1, B2 complete
- ✅ Can import via UI
- ✅ End-to-end flow works

**Estimated:** +40 minutes (3.25 hours total)

### Milestone 3: Testing Complete
**Criteria:**
- ✅ Milestone 2 complete
- ✅ C1, C2 complete
- ✅ All tests pass
- ✅ Manual testing successful

**Estimated:** +1.5 hours (4.75 hours total)

### Milestone 4: Release Ready
**Criteria:**
- ✅ Milestone 3 complete
- ✅ D1, D2, D3 complete
- ✅ All translations done
- ✅ Documentation updated
- ✅ Code review approved

**Estimated:** +2.75 hours (7.5 hours total)

---

## 5. Build and Verification

### 5.1 Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest
```

### 5.2 Verification Checklist

**Before Commit:**
- [ ] Code compiles without errors
- [ ] No ktlint violations
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing successful
- [ ] No console errors or warnings
- [ ] Performance acceptable (< 5s for 1,000 URLs)

**Before Release:**
- [ ] All translations complete
- [ ] CHANGELOG updated
- [ ] Code review approved
- [ ] No known bugs
- [ ] Documentation complete

---

## 6. Rollback Plan

### 6.1 If Critical Bug Found

**Option 1: Revert Commit**
```bash
git revert <commit-hash>
git push
```

**Option 2: Hotfix**
- Create new branch
- Fix bug
- Fast-track through review
- Merge to master

### 6.2 If Performance Issues

**Mitigation:**
- Add chunk processing for large files
- Optimize batch query size
- Add progress indication

**Fallback:**
- Limit import to 1,000 URLs at a time
- Show warning for large files

---

## 7. Dependencies and Blockers

### 7.1 External Dependencies

**None** - all dependencies are already in the project

### 7.2 Internal Dependencies

**Blocking:**
- Must have FeedItemDao access for A1
- Must have business logic for B1/B2

**Non-Blocking:**
- Translations (D1) can be done later
- Unit tests (D2) can be done later

---

## 8. Resource Allocation

### 8.1 Development Resources

**Developer:** 1
**QA Engineer:** 0 (developer testing)
**Designer:** 0 (using existing UI patterns)

### 8.2 Time Allocation

| Phase | Time | Owner |
|-------|------|-------|
| Core Functionality | 2.5h | Developer |
| UI Integration | 0.75h | Developer |
| Testing | 1.5h | Developer |
| Polish | 2.75h | Developer |
| **Total** | **7.5h** | |

**Minimum Viable Feature:** 3.25 hours (Milestones 1-2)

---

## 9. Quality Gates

### 9.1 Code Quality

**Standards:**
- ktlint compliance: 100%
- KDoc coverage: > 80% for public APIs
- No console warnings
- No TODO/FIXME in production code

### 9.2 Test Coverage

**Targets:**
- Integration tests: 100% of happy path
- Unit tests: > 80% of business logic
- Manual tests: All acceptance criteria

### 9.3 Performance Standards

**Requirements:**
- 1,000 URLs: < 5 seconds
- 10,000 URLs: < 30 seconds
- UI thread blocking: 0ms
- Memory usage: < 50MB for 1,000 URLs

---

## 10. Communication Plan

### 10.1 Status Updates

**Daily Standup:**
- Report progress on tasks
- Flag any blockers
- Estimate remaining work

### 10.2 Completion Criteria

**Definition of Done:**
1. All acceptance criteria met
2. Tests passing
3. Code review approved
4. Documentation updated
5. Translations complete (or English-only approved)

---

## 11. Risk Register

| Risk | Probability | Impact | Mitigation | Owner |
|------|-------------|--------|------------|-------|
| Performance issues | LOW | HIGH | Batch operations, testing | Dev |
| Translation delays | MEDIUM | LOW | Ship English first | Dev |
| Test coverage gaps | LOW | MEDIUM | Follow export pattern | Dev |
| UI integration issues | VERY LOW | MEDIUM | Clear pattern to follow | Dev |
| Scope creep | LOW | MEDIUM | Clear requirements | Dev |

---

## 12. Success Criteria

### 12.1 Functional Success

- ✅ Can import saved articles from exported file
- ✅ All matching articles are bookmarked
- ✅ Appropriate user feedback provided
- ✅ Errors handled gracefully

### 12.2 Non-Functional Success

- ✅ Performance meets targets
- ✅ Code quality standards met
- ✅ Tests pass
- ✅ No regressions

### 12.3 User Acceptance

- ✅ Feature works as expected
- ✅ UI is intuitive
- ✅ Error messages are helpful
- ✅ Performance is acceptable

---

## 13. Post-Implementation

### 13.1 Monitoring

**Metrics to Track:**
- Import usage frequency
- Error rates
- Average file size
- Performance metrics

### 13.2 Future Enhancements

**Potential Improvements:**
1. Progress indicator for large imports
2. Chunk processing for very large files
3. Conflict resolution UI
4. Undo import functionality
5. Import statistics dashboard

---

## 14. Task Summary

| ID | Task | Priority | Time | Dependencies |
|----|------|----------|------|--------------|
| A1 | Database Methods | HIGH | 30m | None |
| A2 | Import Logic | HIGH | 1.5h | A1 |
| A3 | String Resources | HIGH | 15m | None |
| B1 | UI Launcher | HIGH | 20m | A2 |
| B2 | Menu Item | HIGH | 20m | B1 |
| C1 | Integration Test | HIGH | 1h | A2 |
| C2 | Manual Test | HIGH | 30m | B2 |
| D1 | Translations | MED | 2h | A3 |
| D2 | Unit Tests | MED | 30m | A2 |
| D3 | Documentation | MED | 15m | None |

**Total Time:** 7.5 hours
**Critical Path:** 4.75 hours (A1 → A2 → B1 → B2 → C1 → C2)

---

**Implementation Status:** READY TO START
**Next Step:** Begin with Task A1 (Database Methods)
**Target Completion:** 2026-01-07 (same day)

---

**Document Status:** COMPLETE
**Version:** 1.0
**Last Updated:** 2026-01-07
