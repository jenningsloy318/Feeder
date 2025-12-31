# Implementation Plan: AI Features - Multiple Providers

**Feature ID**: 001
**Date**: 2026-01-01

---

## Part 1: Original Feature Implementation (Complete)

**Status**: ✅ Complete
**Original Plan**: [./implementation-plan.md](./implementation-plan.md)

This section documents the completed implementation of the multi-provider AI feature.

### Completion Summary

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Foundation | Complete | 100% |
| Phase 2: AI Client Interface | Complete | 100% |
| Phase 3: Data Layer | Complete | 100% |
| Phase 4: Business Logic | Complete | 100% |
| Phase 5: UI Layer | Complete | 100% |
| Phase 6: Build Config | Complete | 100% |
| Phase 7: Testing & Fixes | Complete | 100% |
| Phase 8: Documentation | Complete | 100% |

For detailed information about the original implementation, see [implementation-plan.md](./implementation-plan.md).

---

## Part 2: Bug Fix Implementation (In Progress)

**Specification**: [./06-specification.md](./06-specification.md)
**Debug Analysis**: [./03-debug-analysis.md](./03-debug-analysis.md)
**Status**: In Progress
**Estimated Phases**: 2

### Bug Summary

**Issue**: Anthropic API shows "invalid setting" even with correct API key and URL.

**Root Cause**: Provider type (`aiProviderType`) not updated when settings change, causing `aiSettingsFlow` to emit wrong provider's settings.

---

## Milestones

### Milestone 1: Fix Provider Type Update Logic

**Goal**: Ensure provider type updates when AI settings change

#### Deliverables

- [ ] Update `SettingsViewModel.onOpenAISettingsEvent()` to call `setAIProviderType()`
- [ ] Verify `aiSettingsFlow` emits correct provider settings
- [ ] Add unit tests for provider switching logic

#### Acceptance Criteria

- [ ] Selecting "Anthropic (Claude)" from dropdown updates `aiProviderType` to `ANTHROPIC`
- [ ] Entering Anthropic API key updates provider type simultaneously
- [ ] `aiSettingsFlow` emits `AISettings.Anthropic` (not `AISettings.OpenAI`)
- [ ] `isValid` returns `true` when Anthropic settings are properly configured
- [ ] Summarize button appears in article view

#### Files Affected

| File | Type | Changes |
|------|------|---------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt` | Modify | Add `setAIProviderType()` calls in `UpdateSettings` handler |

#### Implementation Details

**Function**: `onOpenAISettingsEvent(event: AISettingsEvent)`

**Change**: In the `UpdateSettings` case, call `setAIProviderType()` before updating provider-specific settings

```kotlin
is AISettingsEvent.UpdateSettings ->
    when (event.settings) {
        is AISettings.OpenAI -> {
            repository.setAIProviderType(AIProvider.OPENAI_COMPATIBLE)
            repository.setOpenAISettings(event.settings.openaiSettings)
        }
        is AISettings.Anthropic -> {
            repository.setAIProviderType(AIProvider.ANTHROPIC)
            repository.setAnthropicSettings(event.settings.anthropicSettings)
        }
    }
```

---

### Milestone 2: Testing & Verification

**Goal**: Verify fix works correctly and doesn't break existing functionality

#### Deliverables

- [ ] Build debug APK
- [ ] Manual testing with both providers
- [ ] Verify settings persistence
- [ ] Test provider switching
- [ ] Create implementation summary

#### Test Cases

| ID | Scenario | Steps | Expected Result |
|----|----------|-------|-----------------|
| T1 | Configure Anthropic | 1. Select "Anthropic"<br>2. Enter API key<br>3. Select model<br>4. Save | No "invalid setting" error |
| T2 | Switch providers | 1. Configure OpenAI<br>2. Switch to Anthropic<br>3. Verify settings | Each provider's settings preserved |
| T3 | Persist settings | 1. Configure Anthropic<br>2. Close app<br>3. Reopen app | Settings still configured |
| T4 | Summarize button | 1. Configure Anthropic<br>2. Open article | Summarize button visible |
| T5 | Revert to OpenAI | 1. Configure Anthropic<br>2. Switch back to OpenAI | OpenAI settings still work |

#### Acceptance Criteria

- [ ] All test cases pass
- [ ] No regression in OpenAI functionality
- [ ] Settings persist correctly
- [ ] No crashes or ANRs

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing OpenAI settings | Low | High | Test OpenAI provider before deploying |
| SharedPreferences corruption | Very Low | Medium | Only writes, no migration needed |
| Race conditions in rapid switching | Low | Low | StateFlow is thread-safe by design |
| UI not updating after provider switch | Low | Medium | Compose recomposes on StateFlow change |

---

## Dependencies

### Internal Dependencies

None - this is a self-contained bug fix

### External Dependencies

None - no new libraries or SDKs required

---

## Rollout Plan

### Phase 1: Development (Current)

- [ ] Implement fix in `SettingsViewModel.kt`
- [ ] Build debug APK
- [ ] Manual testing

### Phase 2: Verification

- [ ] User testing with actual API keys
- [ ] Verify both providers work
- [ ] Check settings persistence

### Phase 3: Deployment

- [ ] Code review
- [ ] Merge to main branch
- [ ] Release in next app version

---

## Success Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Anthropic settings save successfully | 100% | ✅ Fix applied |
| Provider switching works | 100% | ✅ Fix applied |
| OpenAI settings still work | 100% | 100% (unaffected) |
| Settings persist after restart | 100% | ⏳ Pending testing |

---

## Implementation Status

**Date**: 2026-01-01
**Status**: Code Changes Complete, Testing Pending

**Fixes Applied**:
1. ✅ Provider type synchronization in `SettingsViewModel.kt`
2. ✅ Anthropic model list removed (users input directly)
3. ✅ "No models" message fix for Anthropic
4. ✅ Build successful

**Next Steps**:
1. Install APK on device
2. Manual testing
3. User acceptance testing
4. Deploy if tests pass

---

## References

- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Technical Specification: [./06-specification.md](./06-specification.md)
- Task List: [./08-task-list.md](./08-task-list.md)
- Implementation Summary: [./09-implementation-summary.md](./09-implementation-summary.md)
