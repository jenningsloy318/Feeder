# Implementation Plan: Wire Global Menu Config to Article Page

**Document Version**: 1.0
**Date**: 2026-01-05
**Status**: Draft
**Spec Index**: 017

---

## 1. Overview

This implementation plan outlines the step-by-step approach to integrate the Selection Menu Configuration with the article page text selection menu.

### 1.1 Approach

**Incremental Implementation**:
- Start with DI injection (foundation)
- Add configuration loading (data layer)
- Modify menu building logic (presentation layer)
- Add Feeder item handlers (feature layer)
- Test and verify (quality layer)

**Risk Mitigation**:
- Each step is independently testable
- Can rollback if issues arise
- Backward compatible at each step

### 1.2 Estimated Effort

**Total**: 4-6 hours
- DI Integration: 1 hour
- Config Loading: 1 hour
- Menu Logic: 1.5 hours
- Click Handling: 0.5 hours
- Testing: 1.5 hours
- Buffer: 1 hour

---

## 2. Implementation Phases

### Phase 1: DI Integration (1 hour)

**Objective**: Make `FeederTextActionModeCallback` DI-aware

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Make `FeederTextActionModeCallback` implement `DIAware`**
   ```kotlin
   class FeederTextActionModeCallback(
       override val di: DI,
       // ... existing parameters
   ) : ActionMode.Callback, DIAware
   ```

2. **Inject dependencies**
   ```kotlin
   private val sp: SharedPreferences by instance()
   private val menuDiscoveryService: MenuDiscoveryService by instance()
   ```

3. **Update `WithFeederTextToolbar` to pass DI**
   ```kotlin
   @Composable
   fun WithFeederTextToolbar(
       di: DI,
       content: @Composable () -> Unit
   ) {
       val context = LocalContext.current
       val diContext = remember(di) { di }
       // ... pass di to FeederTextToolbar
   }
   ```

4. **Verify compilation**
   ```bash
   ./gradlew compileDebugKotlin
   ```

**Success Criteria**:
- Code compiles without errors
- DI injection works (log injected dependencies)

**Rollback**: Revert to constructor parameters

---

### Phase 2: Configuration Loading (1 hour)

**Objective**: Load and parse `MenuConfig` from SharedPreferences

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Add `loadMenuConfig()` method**
   ```kotlin
   private fun loadMenuConfig(): MenuConfig {
       val json = sp.getString("selection_menu_config", null)
       return if (json != null) {
           try {
               MenuConfig.fromJson(json)
           } catch (e: Exception) {
               Log.e(TAG, "Failed to parse menu config", e)
               MenuConfig.Default
           }
       } else {
           MenuConfig.Default
       }
   }
   ```

2. **Add caching fields**
   ```kotlin
   private var cachedDiscoveredItems: List<SelectionMenuItem>? = null
   private var cacheTimestamp: Long = 0
   private companion object {
       private const val CACHE_DURATION_MS = 5_000
       private const val TAG = "FeederTextActionMode"
   }
   ```

3. **Add `getDiscoveredItems()` method**
   ```kotlin
   private suspend fun getDiscoveredItems(): List<SelectionMenuItem> {
       val now = System.currentTimeMillis()
       val cacheValid = cachedDiscoveredItems != null &&
                        (now - cacheTimestamp) < CACHE_DURATION_MS

       if (cacheValid) {
           return cachedDiscoveredItems!!
       }

       val items = menuDiscoveryService.discoverAll()
       cachedDiscoveredItems = items
       cacheTimestamp = now
       return items
   }
   ```

4. **Test config loading**
   ```kotlin
   // Add log statement to verify
   Log.d(TAG, "Loaded config: ${loadMenuConfig()}")
   ```

**Success Criteria**:
- Config loads from SharedPreferences
- JSON parsing works
- Fallback to Default on error

**Rollback**: Remove config loading, use hardcoded menu

---

### Phase 3: Menu Filtering and Sorting (1 hour)

**Objective**: Filter items by visibility and sort by configured order

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Add `filterByVisibility()` method**
   ```kotlin
   private fun filterByVisibility(
       items: List<SelectionMenuItem>,
       config: MenuConfig
   ): List<SelectionMenuItem> {
       return items.filter { item ->
           config.isVisible(item.id) && item.isAvailable()
       }
   }

   private fun SelectionMenuItem.isAvailable(): Boolean {
       return when (type) {
           MenuType.THIRD_PARTY -> {
               try {
                   packageManager.getApplicationInfo(packageName!!, 0)
                   true
               } catch (e: PackageManager.NameNotFoundException) {
                   false
               }
           }
           else -> true
       }
   }
   ```

2. **Add `sortByConfigOrder()` method**
   ```kotlin
   private fun sortByConfigOrder(
       items: List<SelectionMenuItem>,
       config: MenuConfig
   ): List<SelectionMenuItem> {
       if (config.isEmpty()) {
           return items.sortedWith(
               compareBy<SelectionMenuItem> { it.type }
                   .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
           )
       }

       val orderMap = config.order.mapIndexed { index, id ->
           id to index
       }.toMap()

       return items.sortedBy { item ->
           orderMap[item.id] ?: Int.MAX_VALUE
       }
   }
   ```

3. **Test filtering and sorting**
   ```kotlin
   // Add log statements to verify
   val config = loadMenuConfig()
   val items = getDiscoveredItems()
   val filtered = filterByVisibility(items, config)
   val sorted = sortByConfigOrder(filtered, config)
   Log.d(TAG, "Sorted items: ${sorted.map { it.name }}")
   ```

**Success Criteria**:
- Invisible items are filtered out
- Items are sorted by configured order
- New items are appended to end

**Rollback**: Remove filtering/sorting, use default order

---

### Phase 4: Menu Building (0.5 hours)

**Objective**: Build menu from sorted items

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Add `assignItemId()` method**
   ```kotlin
   private fun assignItemId(item: SelectionMenuItem, index: Int): Int {
       return when (item.type) {
           MenuType.SYSTEM -> 0 + index
           MenuType.APPLICATION -> 200 + index
           MenuType.THIRD_PARTY -> 100 + index
       }
   }
   ```

2. **Add `mapToMenuItemOption()` method**
   ```kotlin
   private fun mapToMenuItemOption(itemId: String): MenuItemOption? {
       return when (itemId) {
           "android.intent.action.COPY" -> MenuItemOption.Copy
           "android.intent.action.PASTE" -> MenuItemOption.Paste
           "android.intent.action.CUT" -> MenuItemOption.Cut
           "android.intent.action.SELECT_ALL" -> MenuItemOption.SelectAll
           else -> null
       }
   }
   ```

3. **Add `addMenuItemFromConfig()` method**
   ```kotlin
   private fun addMenuItemFromConfig(
       menu: Menu?,
       item: SelectionMenuItem,
       index: Int
   ) {
       val itemId = assignItemId(item, index)

       when (item.type) {
           MenuType.SYSTEM -> {
               val option = mapToMenuItemOption(item.id)
               if (option != null && hasCallback(option)) {
                   menu?.add(0, option.id, index, item.name)
                       ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
               }
           }
           MenuType.APPLICATION -> {
               menu?.add(2, itemId, index, item.name)
                   ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
               feederItems[itemId] = item
           }
           MenuType.THIRD_PARTY -> {
               menu?.add(1, itemId, index, item.name)
                   ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
               textProcessors.add(item.componentName)
           }
       }
   }
   ```

4. **Add `feederItems` map**
   ```kotlin
   private val feederItems = mutableMapOf<Int, SelectionMenuItem>()
   ```

**Success Criteria**:
- Menu items are created from sorted list
- Correct ID ranges assigned
- System/Feeder/third-party items distinguished

**Rollback**: Remove `addMenuItemFromConfig`, use hardcoded menu building

---

### Phase 5: Modify onCreateActionMode (0.5 hours)

**Objective**: Replace hardcoded menu building with config-based logic

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Replace existing `onCreateActionMode()` logic**
   ```kotlin
   override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
       // Load configuration
       val config = loadMenuConfig()

       // Discover menu items (cached)
       val discoveredItems = runBlocking {
           getDiscoveredItems()
       }

       // Filter by visibility
       val visibleItems = filterByVisibility(discoveredItems, config)

       // Sort by configured order
       val sortedItems = sortByConfigOrder(visibleItems, config)

       // Build menu from sorted items
       sortedItems.forEachIndexed { index, item ->
           addMenuItemFromConfig(menu, item, index)
       }

       return true
   }
   ```

2. **Remove old hardcoded logic**
   - Remove hardcoded system items (Copy → Paste → Cut → SelectAll)
   - Keep `addTextProcessors()` logic (will be replaced by `addMenuItemFromConfig`)

**Success Criteria**:
- Menu builds from config
- No hardcoded items
- All item types supported

**Rollback**: Restore old `onCreateActionMode()` logic

---

### Phase 6: Update Click Handling (0.5 hours)

**Objective**: Handle clicks for Feeder items (IDs 200-299)

**Files Modified**:
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextToolbar.kt`

**Steps**:

1. **Update `onActionItemClicked()`**
   ```kotlin
   override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
       val itemId = item!!.itemId

       return when {
           itemId < 100 -> {
               // System items (0-3)
               handleSystemItemClick(itemId)
           }
           itemId in 100..199 -> {
               // Third-party apps
               handleThirdPartyClick(itemId)
           }
           itemId in 200..299 -> {
               // Feeder items
               handleFeederItemClick(itemId)
               mode?.finish()
               true
           }
           else -> false
       }
   }
   ```

2. **Add `handleFeederItemClick()`**
   ```kotlin
   private fun handleFeederItemClick(itemId: Int) {
       val feederItem = feederItems[itemId] ?: return

       when (feederItem.id) {
           "com.nononsenseapps.feeder.action.READ_ALOUD" -> {
               Log.d(TAG, "Read Aloud clicked (placeholder)")
               // TODO: Trigger read aloud functionality
           }
           "com.nononsenseapps.feeder.action.TRANSLATE" -> {
               Log.d(TAG, "Translate clicked (placeholder)")
               // TODO: Trigger translate functionality
           }
       }
   }
   ```

**Success Criteria**:
- System items still work
- Third-party items still work
- Feeder items show placeholder log

**Rollback**: Remove Feeder item handling

---

### Phase 7: Testing (1.5 hours)

**Objective**: Write unit tests and perform manual testing

**Files Created**:
- `app/src/test/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackTest.kt`
- `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/utils/FeederTextActionModeCallbackIntegrationTest.kt`

**Steps**:

1. **Write unit tests**
   - Test config loading
   - Test filtering
   - Test sorting
   - Test ID mapping
   - Target: > 80% coverage

2. **Write integration tests**
   - Test menu building
   - Test click handling
   - Test third-party app launching

3. **Manual testing**
   - Launch app with no config → verify defaults
   - Configure menu in settings → verify menu reflects config
   - Toggle items off → verify they don't appear
   - Reorder items → verify new order
   - Install/uninstall apps → verify graceful handling

4. **Performance testing**
   - Select text → verify menu appears within 100ms
   - Select text multiple times → verify no lag

**Success Criteria**:
- All unit tests pass
- All integration tests pass
- Manual tests pass
- Performance target met (< 100ms)

**Rollback**: Fix failing tests, rollback if critical issues

---

## 3. Build and Verification

### 3.1 Build Commands

```bash
# Clean build
./gradlew clean

# Compile code
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Run integration tests
./gradlew connectedDebugAndroidTest

# Build APK
./gradlew assembleDebug

# Install and run
./gradlew installDebug
```

### 3.2 Verification Checklist

**Compilation**:
- [ ] Code compiles without errors
- [ ] Code compiles without warnings
- [ ] No lint errors

**Unit Tests**:
- [ ] Config loading tests pass
- [ ] Filtering tests pass
- [ ] Sorting tests pass
- [ ] ID mapping tests pass
- [ ] Coverage > 80%

**Integration Tests**:
- [ ] Menu building tests pass
- [ ] Click handling tests pass

**Manual Testing**:
- [ ] No config → defaults shown
- [ ] Valid config → menu reflects config
- [ ] Invalid config → defaults shown
- [ ] Toggle off → item hidden
- [ ] Reorder → new order shown
- [ ] Install app → appears in menu
- [ ] Uninstall app → no crash

**Performance**:
- [ ] Menu appears within 100ms
- [ ] No lag on repeated selections
- [ ] No memory leaks

---

## 4. Risk Mitigation

### 4.1 Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| DI injection breaks existing code | High | Low | Test DI injection early, have rollback ready |
| Config parsing failures | Medium | Low | Fallback to MenuConfig.Default |
| Performance regression | Medium | Low | Cache discovered items, measure timing |
| Test failures | Low | Medium | Write tests incrementally, fix immediately |
| Backward compatibility issues | Medium | Low | Fallback to hardcoded menu if no config |

### 4.2 Rollback Strategy

**Phase 1-2 Rollback**: Revert to constructor parameters
**Phase 3-4 Rollback**: Use hardcoded menu building
**Phase 5-6 Rollback**: Restore old `onCreateActionMode()` logic
**Phase 7 Rollback**: Fix tests, reduce scope if needed

**Critical Rollback**: If critical bug found, revert entire commit and create hotfix branch

---

## 5. Dependencies and Blocking

### 5.1 External Dependencies

**None** - all dependencies already exist

### 5.2 Internal Dependencies

| Component | Status | Notes |
|-----------|--------|-------|
| MenuDiscoveryService | ✅ Ready | Implemented in spec-016 |
| MenuConfig | ✅ Ready | Implemented in spec-016 |
| SelectionMenuItem | ✅ Ready | Implemented in spec-016 |
| FeederTextToolbar | ✅ Ready | Existing code |

### 5.3 Blocking Items

**None** - all prerequisites complete

---

## 6. Timeline

### 6.1 Sprint Timeline

**Day 1** (2 hours):
- Phase 1: DI Integration
- Phase 2: Configuration Loading

**Day 2** (2 hours):
- Phase 3: Menu Filtering and Sorting
- Phase 4: Menu Building
- Phase 5: Modify onCreateActionMode

**Day 3** (2 hours):
- Phase 6: Update Click Handling
- Phase 7: Testing
- Verification and documentation

**Total**: 3 days × 2 hours = 6 hours

### 6.2 Milestones

| Milestone | Criteria | Date |
|-----------|----------|------|
| M1: DI Integration | Code compiles, DI works | Day 1 |
| M2: Config Loading | Config loads, tests pass | Day 1 |
| M3: Menu Logic | Filtering/sorting works | Day 2 |
| M4: Menu Building | Menu builds from config | Day 2 |
| M5: Click Handling | All clicks work | Day 3 |
| M6: Testing | All tests pass | Day 3 |

---

## 7. Success Criteria

### 7.1 Functional Requirements

- [ ] FR1: Load MenuConfig from SharedPreferences
- [ ] FR2: Filter menu items by visibility
- [ ] FR3: Sort menu items by configured order
- [ ] FR4: Detect and include third-party apps
- [ ] FR5: Handle configuration changes

### 7.2 Non-Functional Requirements

- [ ] NFR1: Performance < 100ms
- [ ] NFR2: User experience (smooth, responsive)
- [ ] NFR3: Compatibility (API 29+, all screen sizes)
- [ ] NFR4: Code quality (no warnings, > 80% coverage)
- [ ] NFR5: Localization (reuse existing strings)

### 7.3 Acceptance Criteria

- [ ] AC1: Configuration loading works
- [ ] AC2: Menu filtering works
- [ ] AC3: Menu ordering works
- [ ] AC4: Third-party apps work
- [ ] AC5: Code quality (compiles, tested, documented)
- [ ] AC6: Integration (no conflicts, backward compatible)

---

## 8. Handoff Plan

### 8.1 Documentation

**Code Documentation**:
- KDoc comments for new methods
- Inline comments for complex logic
- Update class documentation

**Specification Documents**:
- Technical specification (06-specification.md)
- Implementation plan (07-implementation-plan.md)
- Task list (08-task-list.md)

### 8.2 Knowledge Transfer

**For Developers**:
- Architecture diagram
- Data flow diagram
- Code examples

**For QA**:
- Test scenarios
- Expected behavior
- Known issues

### 8.3 Deployment

**Pre-Deployment**:
- All tests pass
- Code review approved
- Documentation complete

**Deployment**:
- Merge to main branch
- Include in next release
- Monitor crash reports

---

## 9. Post-Implementation

### 9.1 Monitoring

**Metrics to Track**:
- Crash reports
- Performance metrics
- User feedback
- Configuration usage

### 9.2 Maintenance

**Known Issues**:
- Feeder item handlers are placeholders (future work)
- No real-time config updates (future work)

**Future Enhancements**:
- Implement read_aloud handler
- Implement translate handler
- Add real-time config updates
- Add menu preview in settings

---

## 10. Sign-off

**Implementation Lead**: Pending
**Date**: 2026-01-05
**Status**: Draft - Ready for Execution

**Next Steps**:
1. Review implementation plan
2. Create detailed task list
3. Begin Phase 1: DI Integration

---

**Document Version**: 1.0
**Last Updated**: 2026-01-05
**Status**: Draft
