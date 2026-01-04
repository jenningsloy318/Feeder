# Task List: Wire Global Menu Config to Article Page

**Document Version**: 1.0
**Date**: 2026-01-05
**Status**: Draft
**Spec Index**: 017

---

## Task Summary

**Total Tasks**: 35
**Estimated Time**: 4-6 hours
**Status**: Not Started

---

## Phase 1: DI Integration (1 hour)

### Task 1.1: Make FeederTextActionModeCallback DI-aware
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Add `override val di: DI` parameter to constructor
2. Implement `DIAware` interface
3. Verify compilation

**Acceptance**:
- [ ] Class implements `DIAware`
- [ ] Constructor accepts `di: DI`
- [ ] Code compiles

---

### Task 1.2: Inject SharedPreferences
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add `private val sp: SharedPreferences by instance()` field
2. Test injection works (add log)

**Acceptance**:
- [ ] SharedPreferences field added
- [ ] DI injection works
- [ ] Log shows injected instance

---

### Task 1.3: Inject MenuDiscoveryService
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add `private val menuDiscoveryService: MenuDiscoveryService by instance()` field
2. Test injection works (add log)

**Acceptance**:
- [ ] MenuDiscoveryService field added
- [ ] DI injection works
- [ ] Log shows injected instance

---

### Task 1.4: Update WithFeederTextToolbar to pass DI
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Add `di: DI` parameter to `WithFeederTextToolbar`
2. Pass `di` to `FeederTextToolbar`
3. Update all call sites

**Acceptance**:
- [ ] `WithFeederTextToolbar` accepts `di: DI`
- [ ] DI is passed to `FeederTextActionModeCallback`
- [ ] All call sites updated
- [ ] Code compiles

---

## Phase 2: Configuration Loading (1 hour)

### Task 2.1: Add loadMenuConfig() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Implement `loadMenuConfig()` method
2. Read from SharedPreferences key "selection_menu_config"
3. Parse JSON using `MenuConfig.fromJson()`
4. Add error handling (fallback to `MenuConfig.Default`)
5. Add log statement

**Acceptance**:
- [ ] Method implemented
- [ ] Reads from SharedPreferences
- [ ] Parses JSON correctly
- [ ] Fallback to Default on error
- [ ] Log shows loaded config

---

### Task 2.2: Add caching fields
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add `cachedDiscoveredItems` field
2. Add `cacheTimestamp` field
3. Add `CACHE_DURATION_MS` constant (5000ms)
4. Add `TAG` constant for logging

**Acceptance**:
- [ ] All fields added
- [ ] Constants defined
- [ ] Code compiles

---

### Task 2.3: Add getDiscoveredItems() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Implement `getDiscoveredItems()` method
2. Check cache validity
3. Call `menuDiscoveryService.discoverAll()`
4. Update cache
5. Return items

**Acceptance**:
- [ ] Method implemented
- [ ] Cache check works
- [ ] MenuDiscoveryService called
- [ ] Cache updated
- [ ] Returns correct items

---

### Task 2.4: Test config loading
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add log to `onCreateActionMode()`
2. Run app
3. Check logcat for config loading

**Acceptance**:
- [ ] Log shows loaded config
- [ ] Config is correct (check order and visibility)
- [ ] No errors in logcat

---

## Phase 3: Menu Filtering and Sorting (1 hour)

### Task 3.1: Add filterByVisibility() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Implement `filterByVisibility()` method
2. Filter by `config.isVisible(item.id)`
3. Add `SelectionMenuItem.isAvailable()` extension
4. Check package availability for third-party apps

**Acceptance**:
- [ ] Method implemented
- [ ] Invisible items filtered out
- [ ] Uninstalled apps skipped
- [ ] No crashes

---

### Task 3.2: Add sortByConfigOrder() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Implement `sortByConfigOrder()` method
2. Handle empty config (default order)
3. Sort by `config.order` position
4. Append new items to end

**Acceptance**:
- [ ] Method implemented
- [ ] Items sorted by config order
- [ ] New items at end
- [ ] Empty config uses default order

---

### Task 3.3: Test filtering and sorting
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Add log statements for filtering and sorting
2. Create test config (toggle items off, reorder)
3. Run app
4. Check logcat for results

**Acceptance**:
- [ ] Log shows filtered items
- [ ] Log shows sorted items
- [ ] Order matches config
- [ ] Invisible items removed

---

### Task 3.4: Performance verification
**Status**: Pending
**Priority**: Low
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add timing logs
2. Measure filter time
3. Measure sort time
4. Verify < 10ms each

**Acceptance**:
- [ ] Filter time < 10ms
- [ ] Sort time < 10ms
- [ ] No performance regression

---

## Phase 4: Menu Building (0.5 hours)

### Task 4.1: Add assignItemId() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Implement `assignItemId()` method
2. System items: 0-3
3. Third-party items: 100-199
4. Feeder items: 200-299

**Acceptance**:
- [ ] Method implemented
- [ ] Correct ID ranges
- [ ] No ID conflicts

---

### Task 4.2: Add mapToMenuItemOption() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Implement `mapToMenuItemOption()` method
2. Map system item IDs to MenuItemOption
3. Return null for non-system items

**Acceptance**:
- [ ] Method implemented
- [ ] Copy → MenuItemOption.Copy
- [ ] Paste → MenuItemOption.Paste
- [ ] Cut → MenuItemOption.Cut
- [ ] SelectAll → MenuItemOption.SelectAll

---

### Task 4.3: Add feederItems map
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 5 minutes

**Steps**:
1. Add `feederItems` field (MutableMap<Int, SelectionMenuItem>)
2. Initialize as empty map

**Acceptance**:
- [ ] Field added
- [ ] Initialized

---

### Task 4.4: Add addMenuItemFromConfig() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 15 minutes

**Steps**:
1. Implement `addMenuItemFromConfig()` method
2. Handle SYSTEM items (use MenuItemOption)
3. Handle APPLICATION items (store in feederItems)
4. Handle THIRD_PARTY items (store in textProcessors)

**Acceptance**:
- [ ] Method implemented
- [ ] System items added correctly
- [ ] Feeder items added to feederItems
- [ ] Third-party items added to textProcessors
- [ ] Correct menu group IDs

---

## Phase 5: Modify onCreateActionMode (0.5 hours)

### Task 5.1: Replace onCreateActionMode() logic
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 20 minutes

**Steps**:
1. Load config
2. Get discovered items (runBlocking)
3. Filter by visibility
4. Sort by config order
5. Build menu from sorted items
6. Remove old hardcoded logic

**Acceptance**:
- [ ] New logic implemented
- [ ] Old hardcoded logic removed
- [ ] Menu builds from config
- [ ] Code compiles

---

### Task 5.2: Test menu building
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Run app
2. Select text in article
3. Verify menu appears
4. Verify menu items match config

**Acceptance**:
- [ ] Menu appears
- [ ] Items in correct order
- [ ] Invisible items not shown
- [ ] No crashes

---

## Phase 6: Update Click Handling (0.5 hours)

### Task 6.1: Update onActionItemClicked()
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 15 minutes

**Steps**:
1. Add ID range checks
2. IDs 0-99 → system items
3. IDs 100-199 → third-party items
4. IDs 200-299 → Feeder items

**Acceptance**:
- [ ] ID ranges checked correctly
- [ ] System items handled
- [ ] Third-party items handled
- [ ] Feeder items handled

---

### Task 6.2: Add handleFeederItemClick() method
**Status**: Pending
**Priority**: High
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 15 minutes

**Steps**:
1. Implement `handleFeederItemClick()` method
2. Add placeholder for READ_ALOUD
3. Add placeholder for TRANSLATE
4. Add log statements

**Acceptance**:
- [ ] Method implemented
- [ ] Placeholder handlers added
- [ ] Log statements show which item clicked
- [ ] No crashes

---

### Task 6.3: Test click handling
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Run app
2. Select text
3. Click each menu item
4. Verify correct action

**Acceptance**:
- [ ] System items work (copy, paste, etc.)
- [ ] Third-party items launch apps
- [ ] Feeder items show placeholder logs
- [ ] No crashes

---

## Phase 7: Testing (1.5 hours)

### Task 7.1: Write unit test for loadMenuConfig()
**Status**: Pending
**Priority**: High
**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
**Estimated**: 15 minutes

**Test Cases**:
- Valid JSON → config loaded
- Invalid JSON → MenuConfig.Default
- Null JSON → MenuConfig.Default

**Acceptance**:
- [ ] All test cases pass
- [ ] Edge cases covered

---

### Task 7.2: Write unit test for filterByVisibility()
**Status**: Pending
**Priority**: High
**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
**Estimated**: 15 minutes

**Test Cases**:
- Invisible items filtered out
- Uninstalled apps filtered out
- All visible items kept

**Acceptance**:
- [ ] All test cases pass
- [ ] Edge cases covered

---

### Task 7.3: Write unit test for sortByConfigOrder()
**Status**: Pending
**Priority**: High
**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
**Estimated**: 15 minutes

**Test Cases**:
- Items sorted by config order
- New items appended to end
- Empty config uses default order

**Acceptance**:
- [ ] All test cases pass
- [ ] Edge cases covered

---

### Task 7.4: Write unit test for mapToMenuItemOption()
**Status**: Pending
**Priority**: Medium
**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
**Estimated**: 10 minutes

**Test Cases**:
- System item IDs mapped correctly
- Non-system IDs return null

**Acceptance**:
- [ ] All test cases pass

---

### Task 7.5: Write unit test for assignItemId()
**Status**: Pending
**Priority**: Medium
**File**: `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
**Estimated**: 10 minutes

**Test Cases**:
- System items → 0-3
- Third-party items → 100-199
- Feeder items → 200-299

**Acceptance**:
- [ ] All test cases pass

---

### Task 7.6: Write integration test for menu building
**Status**: Pending
**Priority**: High
**File**: `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackIntegrationTest.kt`
**Estimated**: 15 minutes

**Test Cases**:
- onCreateActionMode builds menu from config
- Menu items in correct order
- Invisible items not shown

**Acceptance**:
- [ ] All test cases pass

---

### Task 7.7: Write integration test for click handling
**Status**: Pending
**Priority**: High
**File**: `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackIntegrationTest.kt`
**Estimated**: 15 minutes

**Test Cases**:
- System item clicks work
- Third-party item clicks work
- Feeder item clicks work

**Acceptance**:
- [ ] All test cases pass

---

### Task 7.8: Manual test - No config scenario
**Status**: Pending
**Priority**: High
**Manual**: Yes
**Estimated**: 5 minutes

**Steps**:
1. Clear app data
2. Launch app
3. Select text in article
4. Verify menu shows defaults

**Acceptance**:
- [ ] Menu appears
- [ ] All items shown in default order
- [ ] No crashes

---

### Task 7.9: Manual test - Configured menu
**Status**: Pending
**Priority**: High
**Manual**: Yes
**Estimated**: 5 minutes

**Steps**:
1. Configure menu in settings
2. Toggle items off
3. Reorder items
4. Select text in article
5. Verify menu reflects config

**Acceptance**:
- [ ] Menu reflects config
- [ ] Invisible items not shown
- [ ] Items in configured order
- [ ] No crashes

---

### Task 7.10: Manual test - Third-party apps
**Status**: Pending
**Priority**: Medium
**Manual**: Yes
**Estimated**: 5 minutes

**Steps**:
1. Install third-party text processing app
2. Configure menu to include app
3. Select text
4. Click third-party item
5. Verify app launches

**Acceptance**:
- [ ] Third-party app appears in menu
- [ ] Click launches app
- [ ] Text copied to clipboard
- [ ] No crashes

---

### Task 7.11: Performance test
**Status**: Pending
**Priority**: Medium
**Estimated**: 5 minutes

**Steps**:
1. Add timing logs
2. Select text multiple times
3. Measure menu construction time
4. Verify < 100ms

**Acceptance**:
- [ ] Menu construction < 100ms
- [ ] No lag on repeated selections
- [ ] Cache works (second selection faster)

---

### Task 7.12: Test coverage verification
**Status**: Pending
**Priority**: Medium
**Estimated**: 5 minutes

**Steps**:
1. Run unit tests with coverage
2. Check coverage report
3. Verify > 80% coverage

**Acceptance**:
- [ ] Unit tests pass
- [ ] Coverage > 80%
- [ ] Critical paths covered

---

## Phase 8: Build and Verification (0.5 hours)

### Task 8.1: Clean build
**Status**: Pending
**Priority**: High
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew clean`
2. Run `./gradlew compileDebugKotlin`
3. Verify no errors
4. Verify no warnings

**Acceptance**:
- [ ] Clean build succeeds
- [ ] No compilation errors
- [ ] No compilation warnings

---

### Task 8.2: Run unit tests
**Status**: Pending
**Priority**: High
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew testDebugUnitTest`
2. Verify all tests pass
3. Check coverage report

**Acceptance**:
- [ ] All unit tests pass
- [ ] Coverage > 80%

---

### Task 8.3: Run integration tests
**Status**: Pending
**Priority**: High
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew connectedDebugAndroidTest`
2. Verify all tests pass

**Acceptance**:
- [ ] All integration tests pass

---

### Task 8.4: Build APK
**Status**: Pending
**Priority**: Medium
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew assembleDebug`
2. Verify APK builds
3. Check APK size

**Acceptance**:
- [ ] APK builds successfully
- [ ] APK size reasonable

---

### Task 8.5: Install and run
**Status**: Pending
**Priority**: High
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew installDebug`
2. Launch app on device/emulator
3. Verify app runs
4. Perform smoke tests

**Acceptance**:
- [ ] App installs
- [ ] App launches
- [ ] Smoke tests pass
- [ ] No crashes

---

### Task 8.6: Lint check
**Status**: Pending
**Priority**: Medium
**Estimated**: 5 minutes

**Steps**:
1. Run `./gradlew lintDebug`
2. Review lint report
3. Fix critical issues

**Acceptance**:
- [ ] No critical lint issues
- [ ] Code quality acceptable

---

## Phase 9: Documentation (0.5 hours)

### Task 9.1: Add KDoc comments
**Status**: Pending
**Priority**: Medium
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 15 minutes

**Steps**:
1. Add KDoc for `loadMenuConfig()`
2. Add KDoc for `filterByVisibility()`
3. Add KDoc for `sortByConfigOrder()`
4. Add KDoc for `addMenuItemFromConfig()`
5. Add KDoc for `handleFeederItemClick()`

**Acceptance**:
- [ ] All new methods have KDoc
- [ ] Documentation clear and complete

---

### Task 9.2: Add inline comments
**Status**: Pending
**Priority**: Low
**File**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`
**Estimated**: 10 minutes

**Steps**:
1. Add comments for ID range logic
2. Add comments for config fallback
3. Add comments for cache invalidation

**Acceptance**:
- [ ] Complex logic explained
- [ ] Comments helpful

---

### Task 9.3: Update implementation summary
**Status**: Pending
**Priority**: High
**File**: `specification/017-wire-global-menu-config-to-article-page/09-implementation-summary.md`
**Estimated**: 5 minutes

**Steps**:
1. Document changes made
2. Document challenges faced
3. Document decisions made
4. Document future work

**Acceptance**:
- [ ] Summary complete
- [ ] All changes documented

---

## Task Status Summary

| Phase | Tasks | Completed | Pending | In Progress |
|-------|-------|-----------|---------|-------------|
| Phase 1: DI Integration | 4 | 0 | 4 | 0 |
| Phase 2: Configuration Loading | 4 | 0 | 4 | 0 |
| Phase 3: Filtering and Sorting | 4 | 0 | 4 | 0 |
| Phase 4: Menu Building | 4 | 0 | 4 | 0 |
| Phase 5: Modify onCreateActionMode | 2 | 0 | 2 | 0 |
| Phase 6: Update Click Handling | 3 | 0 | 3 | 0 |
| Phase 7: Testing | 12 | 0 | 12 | 0 |
| Phase 8: Build and Verification | 6 | 0 | 6 | 0 |
| Phase 9: Documentation | 3 | 0 | 3 | 0 |
| **Total** | **42** | **0** | **42** | **0** |

---

## Progress Tracking

**Start Date**: TBD
**Target End Date**: TBD
**Current Status**: Not Started

**Milestones**:
- [ ] M1: DI Integration Complete (Tasks 1.1-1.4)
- [ ] M2: Config Loading Complete (Tasks 2.1-2.4)
- [ ] M3: Menu Logic Complete (Tasks 3.1-3.4)
- [ ] M4: Menu Building Complete (Tasks 4.1-4.4, 5.1-5.2)
- [ ] M5: Click Handling Complete (Tasks 6.1-6.3)
- [ ] M6: Testing Complete (Tasks 7.1-7.12)
- [ ] M7: Build and Verification Complete (Tasks 8.1-8.6)
- [ ] M8: Documentation Complete (Tasks 9.1-9.3)

---

## Notes

**Dependencies**:
- All tasks depend on completion of Phase 1
- Phase 3 depends on Phase 2
- Phase 4 depends on Phase 3
- Phase 5 depends on Phase 4
- Phase 6 depends on Phase 5
- Phase 7 can start after Phase 6
- Phase 8 depends on Phase 7
- Phase 9 runs in parallel with Phase 7-8

**Blockers**:
- None identified

**Risks**:
- DI injection may break existing code (mitigated by incremental testing)
- Performance may regress (mitigated by caching and measurement)

---

## Sign-off

**Task Lead**: Pending
**Date**: 2026-01-05
**Status**: Draft - Ready for Execution

**Next Steps**:
1. Begin Task 1.1: Make FeederTextActionModeCallback DI-aware
2. Update task status as work progresses
3. Mark milestones complete as phases finish

---

**Document Version**: 1.0
**Last Updated**: 2026-01-05
**Status**: Draft
