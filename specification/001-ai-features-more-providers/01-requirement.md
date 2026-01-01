# Requirements: AI Features - Multiple Providers

**Feature ID**: 001
**Status**: Implementation Complete (Phase 1), Bug Fixes Applied (Phase 2)
**Created**: 2025-12-31
**Last Updated**: 2026-01-01

---

## Table of Contents

1. [Overview](#overview)
2. [Functional Requirements](#functional-requirements)
3. [Technical Requirements](#technical-requirements)
4. [Implementation Summary](#implementation-summary)
5. [All Phases Changes](#all-phases-changes)

---

## Overview

Add support for multiple AI providers (OpenAI-compatible and Anthropic Claude) to the Feeder RSS reader app's article summarization feature.

### Goals

1. Support multiple AI providers through a unified interface
2. Maintain backward compatibility with existing OpenAI settings
3. Allow easy switching between providers
4. Support provider-specific features (model listing, custom endpoints)

### Success Criteria

- [x] Users can select between OpenAI-compatible and Anthropic providers
- [x] Provider settings persist across app restarts
- [x] Provider switching works without data loss
- [x] Article summarization works with both providers
- [x] Bug fixes applied (Phase 2)

---

## Functional Requirements

### FR-1: Provider Selection

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-1.1 | Provider dropdown in AI settings | Must | ✅ |
| FR-1.2 | Provider selection persists across restarts | Must | ✅ |
| FR-1.3 | Default to OpenAI for existing users | Must | ✅ |
| FR-1.4 | Switching providers preserves settings | Must | ✅ |

### FR-2: OpenAI-compatible Provider

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-2.1 | Support OpenAI API | Must | ✅ |
| FR-2.2 | Support Azure OpenAI | Should | ✅ |
| FR-2.3 | Support OpenAI-compatible endpoints (DeepSeek, Perplexity) | Should | ✅ |
| FR-2.4 | Dynamic model listing from API | Should | ✅ |
| FR-2.5 | Custom base URL support | Must | ✅ |

### FR-3: Anthropic Claude Provider

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-3.1 | Support Anthropic Messages API | Must | ✅ |
| FR-3.2 | Model ID input (no dropdown) | Must | ✅ |
| FR-3.3 | Custom base URL support | Should | ✅ |
| FR-3.4 | Language detection in summary | Should | ✅ |

### FR-4: User Interface

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| FR-4.1 | Provider dropdown in settings | Must | ✅ |
| FR-4.2 | API key masking (•••••••) | Must | ✅ |
| FR-4.3 | Model selection (dropdown for OpenAI, input for Anthropic) | Must | ✅ |
| FR-4.4 | Real-time validation | Must | ✅ |
| FR-4.5 | Error messages for API failures | Must | ✅ |

---

## Technical Requirements

### TR-1: Architecture

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-1.1 | Factory pattern for client creation | Must | ✅ |
| TR-1.2 | Sealed interface for settings | Must | ✅ |
| TR-1.3 | StateFlow + flatMapLatest for reactive settings | Must | ✅ |
| TR-1.4 | Repository pattern for data access | Must | ✅ |

### TR-2: SDKs

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-2.1 | Use official OpenAI Java SDK | Must | ✅ |
| TR-2.2 | Use official Anthropic Java SDK | Must | ✅ |
| TR-2.3 | Remove third-party Kotlin wrapper | Must | ✅ |

### TR-3: Data Storage

| ID | Requirement | Priority | Status |
|----|------------|----------|--------|
| TR-3.1 | Separate SharedPreferences keys per provider | Must | ✅ |
| TR-3.2 | Provider type stored separately | Must | ✅ |
| TR-3.3 | No settings migration (fresh install) | Should | ✅ |

---

## Implementation Summary

### Phase 1: Multi-Provider Architecture (2025-12-31)

**Commit**: `0ec80f2065c2dda4e34edb9ad4accb34e37964e1`

**New Files** (6):
- `AIClient.kt` - Unified interface
- `AIApi.kt` - Factory
- `AIProvider.kt` - Provider enum
- `AISettings.kt` - Sealed interface for settings
- `OpenAICompatibleClient.kt` - OpenAI client
- `AnthropicClient.kt` - Anthropic client

**Modified Files** (11):
- `SettingsStore.kt` - Added aiSettingsFlow
- `Repository.kt` - Exposed AI methods
- `AIProviderSection.kt` - Multi-provider UI
- `SettingsViewModel.kt` - Event handling
- `ArticleViewModel.kt` - Uses aiSettingsFlow
- And 6 others...

**Deleted Files** (2):
- `OpenAIApi.kt` (replaced by AIApi.kt)
- `OpenAIClient.kt` (replaced by OpenAICompatibleClient.kt)

**Renamed Files** (1):
- `OpenAISection.kt` → `AIProviderSection.kt`

### Phase 2: Bug Fixes (2026-01-01)

#### Bug #002: Provider Type Synchronization

**Issue**: Anthropic API showed "invalid setting" even with correct API key.

**Root Cause**: `aiProviderType` not updated when settings changed.

**Fix**: Added `setAIProviderType()` calls in `SettingsViewModel.onOpenAISettingsEvent()`.

**Files Modified**:
- `SettingsViewModel.kt`

#### Fix: Anthropic Model List

**Issue**: Anthropic client had hardcoded model list.

**Solution**: Users input model ID directly. `listModels()` returns `emptyList()`.

**Files Modified**:
- `AnthropicClient.kt`

#### Fix: "No Models" Message

**Issue**: "No models were found" message showed for Anthropic.

**Fix**: Added `isAnthropic` parameter to `AIModelsStatus` to skip message.

**Files Modified**:
- `AIProviderSection.kt`

---

## All Phases Changes

### Files Changed Summary (from commit 748fd571)

| Category | Count |
|----------|-------|
| New Files Created | 6 |
| Modified Files | 11 |
| Deleted Files | 2 |
| Renamed Files | 1 |
| Total Changes | 20 files |

### Complete File List

**Created**:
```
app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt
app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt
app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt
app/src/main/java/com/nononsenseapps/feeder/ai/provider/AIProvider.kt
app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt
app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt
```

**Deleted**:
```
app/src/main/java/com/nononsenseapps/feeder/openai/OpenAIApi.kt
app/src/main/java/com/nononsenseapps/feeder/openai/OpenAIClient.kt
```

**Renamed**:
```
app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/OpenAISection.kt
  → app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt
```

**Modified**:
```
app/build.gradle.kts
app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt
app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt
app/src/main/java/com/nononsenseapps/feeder/di/ArchModelModule.kt
app/src/main/java/com/nononsenseapps/feeder/model/opml/OPMLImporter.kt
app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt
app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt
app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/AIProviderSection.kt
app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt
app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SettingsViewModel.kt
app/src/main/res/values/strings.xml
gradle/libs.versions.toml
```

---

## References

- Architecture: [./02-architecture.md](./02-architecture.md)
- Debug Analysis: [./03-debug-analysis.md](./03-debug-analysis.md)
- Technical Specification: [./04-specification.md](./04-specification.md)
- Implementation Plan: [./05-implementation-plan.md](./05-implementation-plan.md)
- Testing Strategy: [./06-testing-strategy.md](./06-testing-strategy.md)
- API Documentation: [./07-api-documentation.md](./07-api-documentation.md)
- Migration Guide: [./08-migration-guide.md](./08-migration-guide.md)
- Implementation Summary: [./09-implementation-summary.md](./09-implementation-summary.md)
