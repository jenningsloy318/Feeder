# Implementation Plan: Global Menu Item Discovery and Display

**Version**: 1.0
**Date**: 2026-01-04
**Estimated Duration**: 3-5 days
**Team Size**: 1 developer

---

## 1. Implementation Phases

### Phase 1: Foundation (Day 1)
**Goal**: Set up data model and service layer

**Tasks**:
1. Extend SelectionMenuItem data class
2. Create MenuType enum
3. Create MenuOrder persistence model
4. Implement MenuDiscoveryService
5. Add dependencies (reorderable library, serialization)

**Deliverables**:
- Updated SelectionMenuItem.kt
- New MenuDiscoveryService.kt
- MenuOrder data class
- Updated build.gradle.kts

**Acceptance Criteria**:
- All code compiles without errors
- Unit tests for discovery service pass
- Service can discover all three menu types

---

### Phase 2: ViewModel & Persistence (Day 2)
**Goal**: Implement state management and persistence

**Tasks**:
1. Implement SelectionMenuSettingsViewModel
2. Add SharedPreferences integration
3. Implement LoadMenus event handler
4. Implement ReorderMenu event handler
5. Add debounced save logic
6. Create merge logic for saved order

**Deliverables**:
- Updated SelectionMenuSettingsViewModel.kt
- Working persistence layer
- ViewModel unit tests

**Acceptance Criteria**:
- ViewModel loads items on init
- Reorder updates ViewState
- Order persists to SharedPreferences
- Order loads correctly on restart

---

### Phase 3: UI Implementation (Day 3)
**Goal**: Build user interface with drag-and-drop

**Tasks**:
1. Replace empty state with LazyColumn
2. Implement MenuItemRow composable
3. Add drag handle icon
4. Integrate reorderable library
5. Add section headers
6. Implement loading state
7. Implement error state

**Deliverables**:
- Updated SelectionMenuSettingsScreen.kt
- Working drag-and-drop UI
- All UI states implemented

**Acceptance Criteria**:
- List displays all discovered items
- Drag-and-drop works smoothly
- Section-constrained reordering works
- Loading/error states display correctly

---

### Phase 4: Integration & Polish (Day 4)
**Goal**: Complete integration and add polish

**Tasks**:
1. Add string resources
2. Implement accessibility features
3. Add move up/down buttons
4. Test on different screen sizes
5. Add proper error handling
6. Performance optimization
7. Add animations

**Deliverables**:
- Complete string resources
- Accessibility support
- Optimized performance
- UI tests passing

**Acceptance Criteria**:
- All strings localized
- TalkBack announces all elements
- Works on phone, tablet, foldable
- Performance targets met

---

### Phase 5: Testing & QA (Day 5)
**Goal**: Comprehensive testing and bug fixes

**Tasks**:
1. Write unit tests
2. Write UI tests
3. Write integration tests
4. Manual testing
5. Accessibility testing
6. Performance testing
7. Bug fixes

**Deliverables**:
- Comprehensive test suite
- Test report
- All bugs fixed

**Acceptance Criteria**:
- Unit test coverage > 80%
- All UI tests pass
- Manual testing checklist complete
- Zero critical bugs

---

## 2. Task Breakdown

### 2.1 Data Model Tasks

| Task | File | Complexity | Time |
|------|------|------------|------|
| Add MenuType enum | SelectionMenuItem.kt | Low | 15min |
| Add type field to SelectionMenuItem | SelectionMenuItem.kt | Low | 15min |
| Add componentName field | SelectionMenuItem.kt | Low | 10min |
| Add packageName field | SelectionMenuItem.kt | Low | 10min |
| Add order field | SelectionMenuItem.kt | Low | 10min |
| Create MenuOrder data class | MenuOrder.kt | Medium | 30min |
| Add serialization support | build.gradle.kts | Low | 15min |

**Total**: ~2 hours

### 2.2 Service Layer Tasks

| Task | File | Complexity | Time |
|------|------|------------|------|
| Create MenuDiscoveryService class | MenuDiscoveryService.kt | Medium | 30min |
| Implement discoverSystemMenus() | MenuDiscoveryService.kt | Low | 30min |
| Implement discoverFeederMenus() | MenuDiscoveryService.kt | Low | 30min |
| Implement discoverThirdPartyMenus() | MenuDiscoveryService.kt | High | 1h |
| Add SDK version handling | MenuDiscoveryService.kt | Medium | 30min |
| Unit tests for service | MenuDiscoveryServiceTest.kt | Medium | 1h |

**Total**: ~4 hours

### 2.3 ViewModel Tasks

| Task | File | Complexity | Time |
|------|------|------------|------|
| Implement LoadMenus handler | ViewModel | Medium | 45min |
| Implement ReorderMenu handler | ViewModel | High | 1h |
| Add SharedPreferences integration | ViewModel | Medium | 45min |
| Implement debounced save | ViewModel | Medium | 30min |
| Implement merge logic | ViewModel | High | 1.5h |
| ViewModel unit tests | ViewModelTest | High | 1.5h |

**Total**: ~6 hours

### 2.4 UI Tasks

| Task | File | Complexity | Time |
|------|------|------------|------|
| Add reorderable dependency | build.gradle.kts | Low | 15min |
| Implement MenuList composable | Screen | Medium | 45min |
| Implement MenuItemRow composable | Screen | Medium | 45min |
| Add drag handle | Screen | Low | 30min |
| Add section headers | Screen | Low | 30min |
| Implement loading state | Screen | Low | 30min |
| Implement error state | Screen | Low | 30min |
| Add animations | Screen | Medium | 45min |
| UI tests | ScreenTest | Medium | 1h |

**Total**: ~5 hours

### 2.5 Integration Tasks

| Task | File | Complexity | Time |
|------|------|------------|------|
| Add string resources | strings.xml | Low | 30min |
| Add move up/down buttons | Screen | Medium | 45min |
| Add accessibility labels | Screen | Medium | 45min |
| Keyboard navigation | Screen | Medium | 45min |
| Performance optimization | Multiple | Medium | 1h |
| Cross-device testing | - | Low | 1h |

**Total**: ~4.5 hours

### 2.6 Testing Tasks

| Task | Type | Complexity | Time |
|------|------|------------|------|
| Integration tests | Test | High | 1.5h |
| Manual testing | Manual | Medium | 1h |
| Accessibility testing | Manual | Medium | 1h |
| Bug fixes | Code | Variable | 2h |

**Total**: ~5.5 hours

---

## 3. Dependencies & Risks

### 3.1 Task Dependencies

```
Data Model → Service Layer → ViewModel → UI → Integration → Testing
```

**Critical Path**:
1. Data Model must be complete before Service Layer
2. Service Layer must be complete before ViewModel
3. ViewModel must be complete before UI
4. UI must be complete before Integration
5. All must be complete before Testing

### 3.2 Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Reorderable library has bugs | Low | High | Test early, have fallback |
| PackageManager slow on devices | Medium | Medium | Cache results, show loading |
| Cross-section reordering complex | Medium | Low | Section filtering in code |
| SharedPreferences corruption | Very Low | Medium | Validate JSON, use default |
| Accessibility not fully supported | Medium | Medium | Add move up/down buttons |
| Performance issues | Low | Medium | Optimize, test on low-end |

---

## 4. Daily Schedule

### Day 1: Foundation
- **Morning** (4h): Data Model + Service Layer
- **Afternoon** (4h): Service completion + unit tests

**Deliverable**: Working service with tests

### Day 2: ViewModel
- **Morning** (4h): ViewModel implementation
- **Afternoon** (4h): Persistence + merge logic

**Deliverable**: Working ViewModel with persistence

### Day 3: UI
- **Morning** (4h): List UI + drag-and-drop
- **Afternoon** (4h): States + animations

**Deliverable**: Complete UI with drag-and-drop

### Day 4: Integration
- **Morning** (4h): Accessibility + strings
- **Afternoon** (4h): Polish + optimization

**Deliverable**: Complete feature

### Day 5: Testing
- **Morning** (4h): Tests + manual testing
- **Afternoon** (4h): Bug fixes + final polish

**Deliverable**: Production-ready feature

---

## 5. Acceptance Checklist

### 5.1 Code Quality
- [ ] All code compiles without errors
- [ ] All code compiles without warnings
- [ ] All functions have KDoc comments
- [ ] All public properties documented
- [ ] Follows project conventions
- [ ] No TODO comments left
- [ ] No debug code remaining

### 5.2 Functionality
- [ ] System menus discovered
- [ ] Feeder menus discovered
- [ ] Third-party menus discovered
- [ ] Items display in correct sections
- [ ] Drag-and-drop works
- [ ] Section-constrained reordering
- [ ] Order persists to SharedPreferences
- [ ] Order loads on startup
- [ ] New items append to sections

### 5.3 UI/UX
- [ ] Loading state displays
- [ ] Error state displays
- [ ] Empty state displays (if applicable)
- [ ] Drag handles visible
- [ ] Smooth animations
- [ ] Works on all screen sizes
- [ ] Works in all orientations
- [ ] Material3 design compliance

### 5.4 Accessibility
- [ ] TalkBack announcements work
- [ ] Move up/down buttons available
- [ ] Keyboard navigation works
- [ ] Proper semantics labels
- [ ] Sufficient color contrast
- [ ] Touch targets 48dp minimum

### 5.5 Performance
- [ ] Discovery completes in < 500ms
- [ ] UI renders in < 100ms
- [ ] Drag-and-drop 60fps
- [ ] No memory leaks
- [ ] No excessive recomposition

### 5.6 Testing
- [ ] Unit tests pass (coverage > 80%)
- [ ] UI tests pass
- [ ] Integration tests pass
- [ ] Manual testing complete
- [ ] Accessibility testing complete

### 5.7 Documentation
- [ ] Code documented
- [ ] String resources added
- [ ] Test documentation complete
- [ ] Spec documents complete

---

## 6. Handoff Criteria

### 6.1 Code Complete
- All tasks implemented
- All tests passing
- Code reviewed
- Documentation complete

### 6.2 Ready for QA
- Feature branch created
- All commits pushed
- PR created
- QA checklist prepared

### 6.3 Ready for Merge
- QA approved
- No outstanding bugs
- Performance verified
- Accessibility verified

---

## 7. Rollout Plan

### 7.1 Testing Environment
1. Create feature branch: `feature/global-menu-discovery`
2. Implement all changes
3. Test on physical devices
4. Test on emulators (phone, tablet, foldable)
5. Accessibility testing

### 7.2 Code Review
1. Create pull request
2. Peer review
3. Address feedback
4. Approval from lead

### 7.3 Merge & Deploy
1. Merge to master
2. Run full test suite
3. Create release tag
4. Deploy to testing
5. Monitor for issues
6. Deploy to production

---

**Implementation Plan Complete**: 2026-01-04
**Status**: Ready for Execution
**Next Phase**: Task List
