# Code Review Guidance: Selection Menu Configuration Feature

**Feature ID**: 015
**Version**: 1.0.0
**Date**: 2026-01-04
**Status**: Ready for Implementation

---

## 1. Review Overview

This document provides specification-aware code review guidance for the Selection Menu Configuration Feature. Reviewers should verify that the implementation matches the specification and meets all quality standards.

### 1.1 Review Objectives

1. **Specification Compliance**: Verify implementation matches all specification documents
2. **Architecture Compliance**: Ensure all ADR decisions are followed
3. **Code Quality**: Verify Feeder coding standards are met
4. **Testing Coverage**: Ensure adequate test coverage
5. **Performance**: Verify performance targets are met
6. **Accessibility**: Verify accessibility requirements are met

### 1.2 Review Process

```
Implementation Complete
         ↓
   Self-Review (Developer)
         ↓
   Peer Review (Code Reviewer)
         ↓
   Specification-Aware Review (Coordinator)
         ↓
   Approval or Changes Requested
```

---

## 2. Specification Compliance Checklist

### 2.1 Requirements Compliance

Verify all functional requirements are implemented:

| FR | Requirement | Verification | Status |
|----|-------------|--------------|--------|
| FR1 | Users can specify order of actions | Check `SelectionMenuSettingsViewModel.onDragMove()` | [ ] |
| FR2 | Users can enable/disable each action | Check `SelectionMenuSettingsViewModel.updateEnabled()` | [ ] |
| FR3 | Configuration persists across sessions | Check `SelectionMenuConfigRepositoryImpl` persistence | [ ] |
| FR4 | Changes apply immediately | Check `FeederTextToolbar` observes config via Flow | [ ] |
| FR5 | Third-party apps discovered automatically | Check `ThirdPartyAppRepositoryImpl` discovery methods | [ ] |
| FR6 | UI provides clear visual feedback | Check drag elevation, shadow, visual cues | [ ] |

### 2.2 Non-Functional Requirements Compliance

Verify all NFR targets are met:

| NFR | Target | Verification Method | Status |
|-----|--------|---------------------|--------|
| NFR1 | Configuration changes ≤ 500ms | Measure time from UI change to toolbar update | [ ] |
| NFR2 | Drag-and-drop ≤ 16ms (60fps) | Profile frame time during drag | [ ] |
| NFR3 | Settings screen load ≤ 2s | Measure time to first frame | [ ] |
| NFR4 | Config data size ≤ 5KB | Check SharedPreferences file size | [ ] |

---

## 3. Architecture Compliance Checklist

### 3.1 ADR Compliance

Verify all architecture decisions are implemented:

#### ADR-001: Persistence Strategy (JSON in SharedPreferences)

**Decision**: Use JSON serialization in SharedPreferences (Score: 4.2/5)

**Verification**:
- [ ] `SelectionMenuConfigStore.kt` uses SharedPreferences
- [ ] `SelectionMenuItemSerializer.kt` uses kotlinx.serialization
- [ ] Configuration stored as JSON array
- [ ] Human-readable format (can inspect with `adb`)
- [ ] Schema version field included for migrations

**Code Locations**:
- `com/nononsenseapps/feeder/util/SelectionMenuConfigStore.kt`
- `com/nononsenseapps/feeder/ui/text/SelectionMenuItemSerializer.kt`

**Review Questions**:
1. Is the configuration stored in SharedPreferences with key `"selection_menu_config"`?
2. Is the JSON format human-readable?
3. Are nullable third-party fields handled correctly?
4. Is there a fallback to defaults if JSON is malformed?

---

#### ADR-002: Drag-and-Drop Implementation (Custom Modifier)

**Decision**: Implement custom drag-and-drop modifier (Score: 4.5/5)

**Verification**:
- [ ] `DraggableItemModifier.kt` implements custom modifier
- [ ] Long-press detection using `pointerInput`
- [ ] Drag tracking with `detectDragGestures`
- [ ] Visual feedback (elevation, shadow) during drag
- [ ] Based on Nutrient blog pattern

**Code Locations**:
- `com/nononsenseapps/feeder/ui/compose/DraggableItemModifier.kt`
- `com/nononsenseapps/feeder/ui/compose/ListItemRow.kt`

**Review Questions**:
1. Is the drag handle long-press to start dragging?
2. Is the drag position tracked correctly?
3. Is visual feedback applied (elevation increases during drag)?
4. Does the drag maintain 60fps (≤ 16ms frame time)?

---

#### ADR-003: Real-Time Updates (StateFlow Observation)

**Decision**: Use StateFlow observation in toolbar (Score: 4.6/5)

**Verification**:
- [ ] `FeederTextToolbar` observes config via Flow
- [ ] Uses `collectAsStateWithLifecycle`
- [ ] Toolbar recomposes when config changes
- [ ] Immediate updates (no polling)

**Code Locations**:
- `com/nononsenseapps/feeder/ui/text/FeederTextToolbar.kt`

**Review Questions**:
1. Does the toolbar observe `SelectionMenuConfigRepository.observeConfig()`?
2. Is the Flow collected with `collectAsStateWithLifecycle`?
3. Does the toolbar recompose when config changes?
4. Are updates immediate (no delay or polling)?

---

#### ADR-004: Third-Party Discovery (On-Demand)

**Decision**: Discover apps when settings open (Score: 4.7/5)

**Verification**:
- [ ] `ThirdPartyAppRepository` implements discovery
- [ ] Discovery triggered when settings screen opens
- [ ] Results cached for 5 minutes
- [ ] Smart merge strategy (preserve user customizations)
- [ ] Uses PackageManager queries

**Code Locations**:
- `com/nononsenseapps/feeder/ui/text/ThirdPartyAppRepository.kt`
- `com/nononsenseapps/feeder/ui/text/SelectionMenuConfigRepositoryImpl.kt`

**Review Questions**:
1. Are translator apps discovered via `ACTION_PROCESS_TEXT`?
2. Are copy apps discovered via clipboard listeners?
3. Are share targets discovered via share handlers?
4. Are results cached for 5 minutes?
5. Does the merge strategy preserve existing user customizations?

---

### 3.2 Layered Architecture Compliance

Verify the layered architecture is followed:

```
Presentation Layer (UI)
    ↓
ViewModel Layer (State Management)
    ↓
Repository Layer (Business Logic)
    ↓
Data Layer (Persistence)
```

**Verification**:
- [ ] UI layer depends only on ViewModel
- [ ] ViewModel depends only on Repository
- [ ] Repository depends only on Data layer
- [ ] No violations of dependency direction
- [ ] No circular dependencies

---

## 4. Code Quality Checklist

### 4.1 Coding Standards

Verify Feeder coding standards are followed:

**Kotlin Conventions**:
- [ ] Use Kotlin idioms (data classes, extension functions, etc.)
- [ ] No unnecessary null checks (use safe calls)
- [ ] Use `val` over `var` where possible
- [ ] Use expression body functions for single expressions
- [ ] Use named arguments for clarity

**Compose Conventions**:
- [ ] Composables are prefixed with "Compose" in function names
- [ ] Composables accept `Modifier` as last parameter
- [ ] Use `remember` for expensive computations
- [ ] Use `LaunchedEffect` for side effects
- [ ] Use `mutableStateListOf` for observable lists

**Naming Conventions**:
- [ ] Classes use PascalCase
- [ ] Functions use camelCase
- [ ] Constants use UPPER_SNAKE_CASE
- [ ] Private properties use _prefix
- [ ] Test functions use backticks and descriptive names

---

### 4.2 Documentation

Verify all code is documented:

**KDoc Comments**:
- [ ] All public classes have KDoc comments
- [ ] All public functions have KDoc comments
- [ ] All public properties have KDoc comments
- [ ] Parameters and return types documented
- [ ] Usage examples provided for complex APIs

**Inline Comments**:
- [ ] Complex logic has inline comments
- [ ] Non-obvious decisions explained
- [ ] Workarounds documented with reason
- [ ] TODO/FIXME addressed or filed as issues

**Example KDoc**:
```kotlin
/**
 * Repository for managing selection menu configuration.
 *
 * This repository provides methods to observe, update, and reset
 * the configuration of actions in the text selection toolbar.
 *
 * @property context Application context
 * @property thirdPartyRepo Repository for discovering third-party apps
 */
class SelectionMenuConfigRepositoryImpl(
    private val context: Context,
    private val thirdPartyRepo: ThirdPartyAppRepository
) : SelectionMenuConfigRepository
```

---

### 4.3 Error Handling

Verify error handling is robust:

**Error Scenarios**:
- [ ] SharedPreferences read failures handled
- [ ] Serialization failures handled
- [ ] PackageManager exceptions handled
- [ ] Network timeouts handled (if any)
- [ ] User-facing error messages

**Example**:
```kotlin
private fun loadConfig() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        try {
            repository.observeConfig().collect { config ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        menuItems = config,
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
```

---

### 4.4 Performance

Verify performance targets are met:

**Drag-and-Drop Performance**:
- [ ] Frame time ≤ 16ms during drag (60fps)
- [ ] No jank or stuttering
- [ ] Lazy recomposition used
- [ ] No unnecessary recompositions

**Configuration Updates**:
- [ ] Changes apply within 500ms
- [ ] No blocking operations on main thread
- [ ] Repository operations are suspending functions
- [ ] ViewModel uses viewModelScope

**Settings Screen Load**:
- [ ] Loads within 2 seconds
- [ ] Initial state shown immediately
- [ ] Loading indicator while loading
- [ ] Progressive rendering (not blocked on all data)

---

### 4.5 Memory Management

Verify no memory leaks:

**Common Leaks**:
- [ ] No Context leaks (use application context)
- [ ] No Flow leaks (use `collectAsStateWithLifecycle`)
- [ ] No coroutine leaks (use viewModelScope)
- [ ] No listener leaks (remove in dispose)

**Verification**:
- Run with LeakCanary
- Rotate device multiple times
- Navigate in and out of settings screen
- Check memory profiler

---

## 5. Testing Checklist

### 5.1 Unit Tests

Verify unit tests are written and passing:

**ViewModel Tests**:
- [ ] Test state management
- [ ] Test drag-and-drop logic
- [ ] Test updateEnabled method
- [ ] Test resetToDefaults method
- [ ] Test error handling

**Repository Tests**:
- [ ] Test observeConfig method
- [ ] Test updateOrder method
- [ ] Test updateEnabled method
- [ ] Test resetToDefaults method
- [ ] Test merge logic

**Serializer Tests**:
- [ ] Test serialization
- [ ] Test deserialization
- [ ] Test malformed JSON handling
- [ ] Test nullable field handling

**Test Coverage**: ≥ 80%

---

### 5.2 Integration Tests

Verify integration tests are written and passing:

**Persistence Tests**:
- [ ] Test configuration persists across app restarts
- [ ] Test SharedPreferences read/write
- [ ] Test default configuration

**Real-Time Update Tests**:
- [ ] Test toolbar updates when config changes
- [ ] Test Flow emission
- [ ] Test StateFlow collection

**Third-Party Discovery Tests**:
- [ ] Test translator app discovery
- [ ] Test copy app discovery
- [ ] Test share target discovery
- [ ] Test caching behavior

---

### 5.3 UI Tests

Verify UI tests are written and passing:

**Drag-and-Drop Tests**:
- [ ] Test drag to new position
- [ ] Test visual feedback during drag
- [ ] Test drop at new position
- [ ] Test drag cancellation

**Toggle Tests**:
- [ ] Test enable action
- [ ] Test disable action
- [ ] Test switch toggle

**Reset Tests**:
- [ ] Test reset to defaults
- [ ] Test confirmation dialog (if any)

---

### 5.4 Manual Tests

Verify manual test scenarios pass:

**Functional Tests**:
1. [ ] Drag item to new position
2. [ ] Disable action, verify it disappears from toolbar
3. [ ] Enable action, verify it appears in toolbar
4. [ ] Reset to defaults, verify order restored
5. [ ] Install third-party app, verify it appears in settings
6. [ ] Install third-party app, verify it appears in toolbar
7. [ ] Uninstall third-party app, verify it's removed
8. [ ] Restart app, verify configuration persists
9. [ ] Change configuration, verify toolbar updates immediately
10. [ ] Test on low-end device (if available)

**Accessibility Tests**:
1. [ ] Screen reader announces drag handle
2. [ ] Screen reader announces switch state
3. [ ] Touch targets meet WCAG 2.1 (48dp minimum)
4. [ ] Keyboard navigation works (Tab, Space, Arrows)

**Performance Tests**:
1. [ ] Drag-and-drop maintains 60fps
2. [ ] Configuration changes apply within 500ms
3. [ ] Settings screen loads within 2s

---

## 6. Integration Checklist

### 6.1 FeederTextToolbar Integration

Verify toolbar integration is correct:

**Code Review**:
```kotlin
@Composable
fun FeederTextToolbar(
    // ... existing parameters
    selectionMenuConfigRepository: SelectionMenuConfigRepository
) {
    val config by selectionMenuConfigRepository.observeConfig()
        .collectAsStateWithLifecycle(initial = emptyList())

    val enabledActions = config
        .filter { it.enabled }
        .sortedBy { it.order }

    // Compose toolbar buttons based on enabledActions
}
```

**Verification**:
- [ ] Repository parameter added
- [ ] Config observed via Flow
- [ ] Actions filtered by enabled status
- [ ] Actions sorted by order
- [ ] Toolbar recomposes when config changes

---

### 6.2 SettingsStore Integration

Verify SettingsStore extension is correct:

**Code Review**:
```kotlin
val Context.selectionMenuConfigStore: SettingsStore<List<SelectionMenuItem>>
    get() = SettingsStore(
        preferences = SharedPreferences(),
        key = "selection_menu_config",
        default = defaultSelectionMenuConfig,
        serializer = /* custom serializer */
    )
```

**Verification**:
- [ ] Extension property on Context
- [ ] SharedPreferences key is `"selection_menu_config"`
- [ ] Default configuration matches current hardcoded order
- [ ] Custom serializer handles JSON

---

### 6.3 Navigation Integration

Verify navigation integration is correct:

**Code Review**:
```kotlin
sealed class Screen(val route: String) {
    // ... existing screens
    object SelectionMenuSettings : Screen("selection_menu_settings")
}
```

**Verification**:
- [ ] Route added to Screen sealed class
- [ ] Settings item added in settings screen
- [ ] Navigation works correctly

---

### 6.4 Dependency Injection Integration

Verify DI integration is correct:

**Code Review**:
```kotlin
val selectionMenuConfigRepositoryModule = module {
    single { ThirdPartyAppRepositoryImpl(androidContext()) }
    single { SelectionMenuConfigRepositoryImpl(androidContext(), get()) }
}
```

**Verification**:
- [ ] Repositories added to DI module
- [ ] Dependencies injected correctly
- [ ] No circular dependencies

---

## 7. Security & Privacy Checklist

### 7.1 Data Privacy

Verify user data is protected:

- [ ] No user data transmitted externally
- [ ] Configuration stored locally only
- [ ] Third-party app metadata not sensitive (package name, class name)
- [ ] No logging of sensitive information

---

### 7.2 Permissions

Verify permissions are appropriate:

- [ ] No additional permissions required
- [ ] PackageManager queries use standard APIs
- [ ] No dangerous permissions requested

---

## 8. Accessibility Checklist

### 8.1 Screen Reader Support

Verify screen reader works correctly:

- [ ] Drag handles announce: "Double tap and hold to start dragging"
- [ ] Switches announce: "Translate enabled, double tap to toggle"
- [ ] Order changes announced: "Moved to position 2"
- [ ] All interactive elements have content descriptions

---

### 8.2 Touch Targets

Verify touch targets meet WCAG 2.1:

- [ ] Drag handle: 48dp x 48dp minimum
- [ ] Switch: 48dp x 48dp minimum
- [ ] Reset button: 48dp height minimum

---

### 8.3 Keyboard Navigation

Verify keyboard navigation works:

- [ ] Tab key navigates between items
- [ ] Space key toggles switches
- [ ] Arrow keys reorder items (alternative to drag-and-drop)
- [ ] Focus indicator visible

---

## 9. Build & Release Checklist

### 9.1 Build Verification

Verify build succeeds:

**Debug Build**:
```bash
./gradlew assembleDebug
```
- [ ] Build succeeds without errors
- [ ] No compiler warnings
- [ ] APK generated successfully

**Release Build**:
```bash
./gradlew assembleRelease
```
- [ ] Build succeeds without errors
- [ ] ProGuard/R8 rules included (if needed)
- [ ] APK signed correctly

---

### 9.2 Test Verification

Verify all tests pass:

**Unit Tests**:
```bash
./gradlew test
```
- [ ] All unit tests pass
- [ ] Test coverage ≥ 80%

**Integration Tests**:
```bash
./gradlew connectedAndroidTest
```
- [ ] All integration tests pass
- [ ] No flaky tests

---

### 9.3 Release Notes

Verify release notes are prepared:

- [ ] Feature description included
- [ ] User-facing changes documented
- [ ] Known issues listed (if any)
- [ ] Upgrade instructions (if needed)

---

## 10. Approval Decision

### 10.1 Review Criteria

The implementation is **approved** when:

1. [ ] All functional requirements (FR1-FR6) are met
2. [ ] All non-functional requirements (NFR1-NFR4) are met
3. [ ] All ADR decisions are followed
4. [ ] All code quality standards are met
5. [ ] All tests pass (unit, integration, UI)
6. [ ] All manual test scenarios pass
7. [ ] Performance targets are met
8. [ ] Accessibility requirements are met
9. [ ] No critical or high bugs
10. [ ] Documentation is complete

### 10.2 Review Outcomes

**Approved**: Implementation meets all criteria, ready for merge

**Approved with Comments**: Implementation meets all criteria, minor suggestions (no blocking issues)

**Changes Requested**: Implementation has issues that must be fixed before approval

**Blocked**: Critical issues block approval, must be fixed and re-reviewed

---

## 11. Review Feedback Template

When providing review feedback, use this template:

```markdown
## Code Review: Selection Menu Configuration Feature

### Summary
[Brief summary of review outcome]

### Specification Compliance
- [x] FR1: Users can specify order
- [x] FR2: Users can enable/disable actions
- [x] FR3: Configuration persists
- [x] FR4: Changes apply immediately
- [x] FR5: Third-party apps discovered
- [x] FR6: Clear visual feedback

### Architecture Compliance
- [x] ADR-001: JSON in SharedPreferences
- [x] ADR-002: Custom drag modifier
- [x] ADR-003: StateFlow observation
- [x] ADR-004: On-demand discovery

### Code Quality
- [x] Kotlin conventions followed
- [x] Compose conventions followed
- [x] Naming conventions followed
- [x] KDoc comments present
- [x] Error handling robust

### Testing
- [x] Unit tests pass
- [x] Integration tests pass
- [x] UI tests pass
- [x] Manual tests pass

### Performance
- [x] Drag-and-drop ≤ 16ms
- [x] Config changes ≤ 500ms
- [x] Settings load ≤ 2s

### Issues Found
[List any issues found with severity]

### Recommendations
[Any suggestions for improvement]

### Decision
[Approved / Approved with Comments / Changes Requested / Blocked]
```

---

## 12. Next Steps After Review

### 12.1 If Approved

1. Proceed to Phase 10 (Documentation Update)
2. Update inline code comments
3. Update README
4. Proceed to Phase 11 (Cleanup)

### 12.2 If Changes Requested

1. Address all blocking issues
2. Re-run tests
3. Request re-review
4. Iterate until approved

### 12.3 If Blocked

1. Address all critical issues
2. Re-run all tests
3. Request full re-review
4. Iterate until approved

---

**END OF CODE REVIEW GUIDANCE**
