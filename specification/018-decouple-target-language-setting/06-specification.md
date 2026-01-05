# Technical Specification: Decouple Target Language Settings

**Date:** 2026-01-05
**Author:** Specification Writer Agent
**Status:** Draft
**Feature Index:** 018

---

## 1. Overview

### 1.1 Summary

This specification details the technical implementation for decoupling target language settings from auto-summary and auto-translation feature toggles. Currently, language selectors are disabled when their respective auto-features are disabled (line 101 in both `SummarySettingsScreen.kt` and `TranslationSettingsScreen.kt`), preventing users from configuring target languages for manual operations.

**Core Change:** Remove the `enabled` parameter dependency from `LanguageSelectorSetting` components, allowing users to select target languages at any time, regardless of auto-feature state.

**Scope:** Minimal code change with significant usability improvement.
- **Files Modified:** 2
- **Lines Changed:** 6 (4 removed, 2 modified)
- **Risk Level:** Low

### 1.2 Goals

1. **Enable Independent Configuration:** Users can set target language preferences without enabling auto-features
2. **Support Manual Workflows:** Language settings are available for on-demand summary/translation operations
3. **Reduce Configuration Friction:** Users don't need to temporarily enable auto-features just to change language
4. **Maintain Design Consistency:** Follow existing Material3 design patterns and project conventions
5. **Preserve Accessibility:** Maintain WCAG 2.1 AA compliance and keyboard navigation

### 1.3 Non-Goals

- **NOT changing the underlying data model** - language preferences are already stored independently
- **NOT modifying ViewModels** - state management remains unchanged
- **NOT adding new UI components** - reuse existing dropdown menu
- **NOT changing string resources** - no new user-facing text needed
- **NOT modifying auto-feature behavior** - only the coupling between settings is removed

---

## 2. Background

### 2.1 Context

**From Requirements Clarification (Phase 2):**
> Users report frustration when they cannot set target language for manual operations because the language selector is disabled when auto-features are turned off. This forces users into an awkward workflow: enable auto-feature → change language → disable auto-feature.

**User Pain Points Identified:**
1. Language selector is grayed out when auto-summary/translation is OFF
2. No visual indication that language setting applies to manual operations
3. Confusing coupling between independent settings
4. Blocks efficient workflow for users who prefer manual operations

### 2.2 Current State

**From Code Assessment (Phase 5):**

**Coupling Points Identified:**

| File | Line | Coupling | Impact |
|------|------|----------|--------|
| `SummarySettingsScreen.kt` | 101 | `enabled = summaryEnabled` | Language selector disabled when switch OFF |
| `TranslationSettingsScreen.kt` | 101 | `enabled = translationEnabled` | Language selector disabled when switch OFF |

**Component Signature (Current):**
```kotlin
@Composable
private fun LanguageSelectorSetting(
    title: String,
    currentLanguage: SummaryLanguage, // or TranslationLanguage
    onLanguageSelected: (SummaryLanguage) -> Unit,
    enabled: Boolean,  // ← PROBLEMATIC: Coupled to switch state
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Current Behavior:**
```kotlin
// SummarySettingsScreen.kt:96-104
LanguageSelectorSetting(
    title = stringResource(R.string.summary_language_title),
    currentLanguage = summaryLanguage,
    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
    enabled = summaryEnabled,  // ← Disables selector when switch is OFF
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

**Risk Assessment (From Code Assessment):**
- **Change Risk:** Low - removing parameter doesn't affect component logic
- **Regression Risk:** Low - no changes to state management or data flow
- **Migration Risk:** None - no database or schema changes
- **Test Coverage:** Existing tests should pass; new tests for independence

### 2.3 Problem Statement

**Root Cause:**
The `enabled` parameter in `LanguageSelectorSetting` is tied to the auto-feature toggle state (`summaryEnabled` or `translationEnabled`). This creates an unnecessary dependency between two independent settings:
1. **Auto-feature toggle** - controls automatic behavior
2. **Target language** - controls output language for BOTH automatic and manual operations

**Why This Is Wrong:**
- Language preference is a user setting, not a feature-dependent setting
- Manual operations (summary/translation buttons) use the same language preference
- Coupling forces users into awkward configuration workflows
- Violates principle of independent settings

**Evidence From Design Spec (Phase 5.5):**
> User research shows that manual workflow users frequently express frustration: "I just want to translate to Spanish, why do I have to enable auto-translation first?"

---

## 3. Technical Design

### 3.1 Architecture

#### Current Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Settings Screen                       │
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │  Switch: Enable Auto Feature        [ON/OFF]   │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                     │
│                   │ Controls                             │
│                   ▼                                     │
│  ┌────────────────────────────────────────────────┐     │
│  │  Language Selector                  [DISABLED] │ ← PROBLEM
│  │  (enabled = switchState)                       │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

#### Proposed Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Settings Screen                       │
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │  Switch: Enable Auto Feature        [ON/OFF]   │     │
│  └────────────────────────────────────────────────┘     │
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │  Language Selector                   [ENABLED] │ ← FIXED
│  │  (always enabled, independent)                 │     │
│  └────────────────────────────────────────────────┘     │
│                                                          │
│  Both settings are INDEPENDENT                          │
└─────────────────────────────────────────────────────────┘
```

**Key Changes:**
1. Remove `enabled` parameter from `LanguageSelectorSetting` signature
2. Remove `enabled = summaryEnabled` / `enabled = translationEnabled` arguments from calls
3. Remove `.clickable(enabled = enabled)` logic (replace with `.clickable`)

### 3.2 Components

#### Component 1: SummarySettingsScreen

**Purpose:** Configure auto-summary behavior and target language preference

**Responsibilities:**
- Display auto-summary toggle switch
- Display language selector (ALWAYS ENABLED)
- Observe state from `SummarySettingsViewModel`
- Update state via ViewModel methods

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**Current Implementation (Lines 96-104):**
```kotlin
LanguageSelectorSetting(
    title = stringResource(R.string.summary_language_title),
    currentLanguage = summaryLanguage,
    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
    enabled = summaryEnabled,  // ← TO BE REMOVED
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

**Modified Implementation:**
```kotlin
LanguageSelectorSetting(
    title = stringResource(R.string.summary_language_title),
    currentLanguage = summaryLanguage,
    onLanguageSelected = { viewModel.setSummaryLanguage(it) },
    // enabled parameter removed
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

---

#### Component 2: TranslationSettingsScreen

**Purpose:** Configure auto-translation behavior and target language preference

**Responsibilities:**
- Display auto-translation toggle switch
- Display language selector (ALWAYS ENABLED)
- Observe state from `TranslationSettingsViewModel`
- Update state via ViewModel methods

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`

**Current Implementation (Lines 96-104):**
```kotlin
LanguageSelectorSetting(
    title = stringResource(R.string.translation_target_language_title),
    currentLanguage = translationLanguage,
    onLanguageSelected = { viewModel.setTranslationLanguage(it) },
    enabled = translationEnabled,  // ← TO BE REMOVED
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

**Modified Implementation:**
```kotlin
LanguageSelectorSetting(
    title = stringResource(R.string.translation_target_language_title),
    currentLanguage = translationLanguage,
    onLanguageSelected = { viewModel.setTranslationLanguage(it) },
    // enabled parameter removed
    menuExpanded = languageMenuExpanded,
    onMenuExpandedChange = { languageMenuExpanded = it },
)
```

---

#### Component 3: LanguageSelectorSetting (SummarySettingsScreen)

**Purpose:** Dropdown menu for selecting target summary language

**Location:** `SummarySettingsScreen.kt:110-182`

**Current Signature:**
```kotlin
@Composable
private fun LanguageSelectorSetting(
    title: String,
    currentLanguage: SummaryLanguage,
    onLanguageSelected: (SummaryLanguage) -> Unit,
    enabled: Boolean,  // ← TO BE REMOVED
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Modified Signature:**
```kotlin
@Composable
private fun LanguageSelectorSetting(
    title: String,
    currentLanguage: SummaryLanguage,
    onLanguageSelected: (SummaryLanguage) -> Unit,
    // enabled parameter removed
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Current Body (Lines 119-180):**
```kotlin
val dimens = LocalDimens.current

Box(
    modifier =
        modifier
            .fillMaxSize()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) {  // ← MODIFIED
                onMenuExpandedChange(true)
            }
            .semantics {
                role = Role.Button
            }
            .padding(
                horizontal = dimens.margin,
                vertical = 8.dp,
            ),
) {
    // ... rest of component
}
```

**Modified Body:**
```kotlin
val dimens = LocalDimens.current

Box(
    modifier =
        modifier
            .fillMaxSize()
            .heightIn(min = 48.dp)
            .clickable {  // ← REMOVED: enabled = enabled
                onMenuExpandedChange(true)
            }
            .semantics {
                role = Role.Button
            }
            .padding(
                horizontal = dimens.margin,
                vertical = 8.dp,
            ),
) {
    // ... rest of component unchanged
}
```

---

#### Component 4: LanguageSelectorSetting (TranslationSettingsScreen)

**Purpose:** Dropdown menu for selecting target translation language

**Location:** `TranslationSettingsScreen.kt:110-182`

**Changes:** Same pattern as Component 3
- Remove `enabled: Boolean` parameter from signature
- Remove `enabled = enabled` from `.clickable()` modifier

---

### 3.3 Data Model

**No Changes Required** - Data model already supports independent settings:

```kotlin
// ViewModel state flows - UNCHANGED
val summaryEnabled: StateFlow<Boolean>
val summaryLanguage: StateFlow<SummaryLanguage>

val translationEnabled: StateFlow<Boolean>
val translationLanguage: StateFlow<TranslationLanguage>

// Repository layer - UNCHANGED
// Language preference is already stored independently
// of enabled state in SharedPreferences/DataStore
```

**Key Point:** The data model already supports independent storage. The issue is purely UI-level coupling via the `enabled` parameter.

---

### 3.4 API Design

**No API Changes** - This is purely a UI layer change. No backend, network, or database APIs are affected.

**Internal APIs (ViewModel) - UNCHANGED:**
```kotlin
// SummarySettingsViewModel
fun setSummaryEnabled(enabled: Boolean)
fun setSummaryLanguage(language: SummaryLanguage)

// TranslationSettingsViewModel
fun setTranslationEnabled(enabled: Boolean)
fun setTranslationLanguage(language: TranslationLanguage)
```

---

### 3.5 Error Handling

**No Error States in This Component:**
- Language selection is infallible (enum-based, no validation errors)
- No network calls in settings UI
- No database writes triggered by UI state changes

**Edge Case Handling:**
- **Screen Rotation:** Menu dismisses, language selection preserved (existing behavior)
- **Process Death:** State restored from saved preferences (existing behavior)
- **Rapid Selections:** Last selection wins (Compose single-threaded UI)

---

## 4. Implementation Approach

### 4.1 Technology Stack

- **Language:** Kotlin
- **Framework:** Jetpack Compose with Material3
- **Architecture:** MVVM (Model-View-ViewModel)
- **State Management:** StateFlow with collectAsStateWithLifecycle()
- **Min SDK:** Android API 24+ (verify from build.gradle.kts)
- **Target SDK:** Android API 35+ (verify from build.gradle.kts)

### 4.2 Dependencies

**No New Dependencies Required:**
```kotlin
// Existing dependencies - NO CHANGES
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.lifecycle:lifecycle-runtime-compose")
```

### 4.3 Configuration

**No Configuration Changes Required:**
- No build.gradle.kts modifications
- No ProGuard/R8 rule changes
- No AndroidManifest.xml changes
- No resource file changes (strings, colors, etc.)

---

## 5. Testing Strategy

### 5.1 Unit Tests

**ViewModel Tests (Existing - Should Still Pass):**
```kotlin
@Test
fun `setting language saves independently of enabled state`() {
    // Given: Auto-summary is disabled
    viewModel.setSummaryEnabled(false)

    // When: User sets language
    viewModel.setSummaryLanguage(SummaryLanguage.SPANISH)

    // Then: Language is saved
    assertEquals(SummaryLanguage.SPANISH, viewModel.summaryLanguage.value)
}

@Test
fun `enabling auto-feature does not change language preference`() {
    // Given: Language is set to French, auto-summary disabled
    viewModel.setSummaryLanguage(SummaryLanguage.FRENCH)
    viewModel.setSummaryEnabled(false)

    // When: User enables auto-summary
    viewModel.setSummaryEnabled(true)

    // Then: Language remains French
    assertEquals(SummaryLanguage.FRENCH, viewModel.summaryLanguage.value)
}
```

**Component Tests (New - To Verify Decoupling):**
```kotlin
@Test
fun `language selector is enabled when switch is off`() {
    // Given: Auto-summary is disabled
    composeTestRule.setContent {
        val viewModel = viewModelFactory.create()
        viewModel.setSummaryEnabled(false)

        SummarySettingsScreen(
            onNavigateUp = {},
            viewModel = viewModel,
        )
    }

    // When: Checking language selector
    composeTestRule.onNodeWithText("Summary Language")
        .assertIsEnabled()  // ← PASS: Selector is enabled
        .performClick()

    // Then: Dropdown menu appears
    composeTestRule.onNodeWithText("Spanish")
        .assertIsDisplayed()
}
```

### 5.2 Integration Tests

**Manual Workflow Integration Test:**
```kotlin
@Test
fun `manual translation uses language preference without auto-feature`() {
    // Given:
    // - Auto-translation is OFF
    // - Target language is set to Spanish
    viewModel.setTranslationEnabled(false)
    viewModel.setTranslationLanguage(TranslationLanguage.SPANISH)

    // When: User manually triggers translation
    // (Simulate tapping translate button on article)

    // Then: Translation uses Spanish
    // Verify translation API called with Spanish language code
}
```

### 5.3 Edge Cases

| Edge Case | Expected Behavior | Test |
|-----------|-------------------|------|
| **Switch OFF, language changed** | Language saves, selector remains enabled | Unit test: setLanguage when enabled=false |
| **Screen rotation with menu open** | Menu closes, language preserved | UI test: rotate device |
| **Rapid language changes** | Last selection wins | Unit test: rapid setLanguage calls |
| **Long language names** | Text truncates with ellipsis | UI test: truncate verification |
| **First launch, no language set** | Defaults to system/English | Unit test: initial state |
| **Manual operation without auto-feature** | Uses saved language preference | Integration test: manual summary |

---

## 6. Security Considerations

### 6.1 Input Validation

| Input | Validation | Sanitization |
|-------|------------|--------------|
| Language selection | Enum values (compile-time safe) | Not applicable - no user input |

**No User Input Validation Needed:**
- Language selection is dropdown-only (no free text)
- Enum values provide compile-time safety
- No injection risks

### 6.2 Authentication & Authorization

- **Auth required:** No (settings are local preferences)
- **Permission checks:** None (settings app doesn't need permissions)
- **Role restrictions:** None

### 6.3 Data Protection

- **Sensitive data:** None (language preferences are not sensitive)
- **Encryption:** Not applicable (local preferences only)
- **Logging:** No sensitive data logged

### 6.4 OWASP Considerations

| Risk | Applicable | Mitigation |
|------|------------|------------|
| Injection | No | Enum-only values, no user input |
| Broken Auth | No | Not applicable |
| XSS | No | Not a web application |
| CSRF | No | Not a web application |
| Security Misconfiguration | No | No security config in settings |

**Security Posture:** This change does not introduce any security risks. It's purely a UI/UX improvement with no attack surface.

---

## 7. Performance Considerations

### 7.1 Complexity Analysis

| Operation | Time Complexity | Space Complexity |
|-----------|-----------------|------------------|
| Language selection | O(1) | O(1) |
| Switch toggle | O(1) | O(1) |
| Menu render | O(n) where n = language count (typically < 20) | O(n) |

**No Performance Impact:** Removing the `enabled` parameter does not change algorithmic complexity.

### 7.2 Compose Recomposition

**Before Change:**
```kotlin
// Language selector recomposes when:
// - summaryEnabled changes (via enabled parameter)
// - summaryLanguage changes
// - languageMenuExpanded changes
```

**After Change:**
```kotlin
// Language selector recomposes when:
// - summaryLanguage changes
// - languageMenuExpanded changes
// (One fewer trigger - summaryEnabled no longer causes recomposition)
```

**Optimization:** Removing `enabled` parameter actually **reduces unnecessary recomposition**, slightly improving performance.

### 7.3 Caching Strategy

**No Caching Changes:**
- StateFlow values are cached by default
- No additional caching needed
- Existing cache invalidation unchanged

### 7.4 Resource Usage

| Resource | Before | After | Impact |
|----------|--------|-------|--------|
| Memory | O(1) | O(1) | None |
| CPU | O(1) per interaction | O(1) per interaction | None |
| Battery | Negligible | Negligible | None |

---

## 8. Rollout Plan

### 8.1 Deployment Phases

**Single-Phase Deployment:**
1. **Code Review:** Review specification and implementation plan
2. **Implementation:** Make code changes (6 lines modified)
3. **Testing:** Run unit tests, UI tests, manual QA
4. **Commit:** Commit changes with descriptive message
5. **Merge:** Merge to main branch
6. **Release:** Deploy in next app release

**No Canary/Phased Rollout Needed:**
- Low-risk change
- No backend dependencies
- No migration required
- Easy rollback if needed

### 8.2 Rollback Plan

**Rollback Scenario:** If users report unexpected behavior (unlikely)

**Rollback Steps:**
1. Revert commit: `git revert <commit-hash>`
2. Hotfix release
3. Deploy to Play Store

**Rollback Risk:** Low - simple revert of 6 lines

---

## 9. Open Questions

**None** - This is a straightforward, well-understood change with minimal complexity.

---

## 10. References

### Source Documents (All Phases Completed)

1. **Requirements (Phase 2 - Agent a62db2c):**
   - Link: Not accessible in current context
   - Key findings: User pain points, manual workflow support needed

2. **Research Report (Phase 3 - Agent afb0f8b):**
   - Link: Not accessible in current context
   - Key findings: Android best practices, Jetpack Compose patterns

3. **Code Assessment (Phase 5 - Agent a3a6e16):**
   - Link: Not accessible in current context
   - Key findings: 2 files, 2 coupling points identified, low risk

4. **Design Specification (Phase 5.5 - Agent a19ee90):**
   - Link: `./05-design-spec.md`
   - Key findings: Option 1 recommended (Always Enabled), minimal visual change

### Code References

**Files to Modify:**
1. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`
   - Line 101: Remove `enabled = summaryEnabled` parameter
   - Line 114: Remove `enabled: Boolean` from function signature
   - Line 125: Remove `enabled = enabled` from `.clickable()`

2. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`
   - Line 101: Remove `enabled = translationEnabled` parameter
   - Line 114: Remove `enabled: Boolean` from function signature
   - Line 125: Remove `enabled = enabled` from `.clickable()`

**Related Files (No Changes):**
- `SummarySettingsViewModel.kt` - ViewModels remain unchanged
- `TranslationSettingsViewModel.kt` - ViewModels remain unchanged
- `SummaryLanguage.kt` - Enum remains unchanged
- `TranslationLanguage.kt` - Enum remains unchanged

---

## Appendix A: Change Summary

### Files Modified

| File | Lines Changed | Type |
|------|---------------|------|
| `SummarySettingsScreen.kt` | 3 (1 removed, 1 removed, 1 modified) | UI |
| `TranslationSettingsScreen.kt` | 3 (1 removed, 1 removed, 1 modified) | UI |
| **Total** | **6** | **Low Risk** |

### Detailed Changes

**File 1: SummarySettingsScreen.kt**

```diff
@@ -95,7 +95,6 @@ fun SummarySettingsScreen(
             // Language Selector
             LanguageSelectorSetting(
                 title = stringResource(R.string.summary_language_title),
                 currentLanguage = summaryLanguage,
                 onLanguageSelected = { viewModel.setSummaryLanguage(it) },
-                enabled = summaryEnabled,
                 menuExpanded = languageMenuExpanded,
                 onMenuExpandedChange = { languageMenuExpanded = it },
             )
@@ -109,7 +108,6 @@ private fun LanguageSelectorSetting(
     title: String,
     currentLanguage: SummaryLanguage,
     onLanguageSelected: (SummaryLanguage) -> Unit,
-    enabled: Boolean,
     menuExpanded: Boolean,
     onMenuExpandedChange: (Boolean) -> Unit,
     modifier: Modifier = Modifier,
@@ -119,7 +117,7 @@ private fun LanguageSelectorSetting(
     Box(
         modifier =
             modifier
                 .fillMaxSize()
                 .heightIn(min = 48.dp)
-                .clickable(enabled = enabled) {
+                .clickable {
                     onMenuExpandedChange(true)
                 }
```

**File 2: TranslationSettingsScreen.kt**

```diff
@@ -95,7 +95,6 @@ fun TranslationSettingsScreen(
             // Language Selector
             LanguageSelectorSetting(
                 title = stringResource(R.string.translation_target_language_title),
                 currentLanguage = translationLanguage,
                 onLanguageSelected = { viewModel.setTranslationLanguage(it) },
-                enabled = translationEnabled,
                 menuExpanded = languageMenuExpanded,
                 onMenuExpandedChange = { languageMenuExpanded = it },
             )
@@ -109,7 +108,6 @@ private fun LanguageSelectorSetting(
     title: String,
     currentLanguage: TranslationLanguage,
     onLanguageSelected: (TranslationLanguage) -> Unit,
-    enabled: Boolean,
     menuExpanded: Boolean,
     onMenuExpandedChange: (Boolean) -> Unit,
     modifier: Modifier = Modifier,
@@ -119,7 +117,7 @@ private fun LanguageSelectorSetting(
     Box(
         modifier =
             modifier
                 .fillMaxSize()
                 .heightIn(min = 48.dp)
-                .clickable(enabled = enabled) {
+                .clickable {
                     onMenuExpandedChange(true)
                 }
```

---

## Appendix B: Testing Checklist

### Unit Tests

- [ ] Language saves when auto-feature is disabled
- [ ] Enabling auto-feature doesn't override language
- [ ] Disabling auto-feature doesn't change language
- [ ] State flow updates correctly

### UI Tests

- [ ] Language selector is enabled when switch is OFF
- [ ] Language selector is enabled when switch is ON
- [ ] Dropdown menu opens in all states
- [ ] Language selection updates UI immediately
- [ ] Menu dismisses on back/tap-out/escape

### Integration Tests

- [ ] Manual summary uses saved language
- [ ] Manual translation uses saved language
- [ ] Auto-summary uses saved language
- [ ] Auto-translation uses saved language

### Accessibility Tests

- [ ] Keyboard navigation works (Tab, Enter, Arrow keys, Escape)
- [ ] Screen reader announcements correct
- [ ] Focus indicators visible
- [ ] Touch targets meet minimums (48dp)

### Responsive Tests

- [ ] Mobile layout correct (< 600dp)
- [ ] Tablet layout correct (600-840dp)
- [ ] Desktop layout correct (> 840dp)
- [ ] Landscape orientation works

### Edge Cases

- [ ] Screen rotation with menu open
- [ ] Rapid language changes
- [ ] Long language names
- [ ] First launch (no language set)
- [ ] Locale switch

---

## Appendix C: Success Metrics

### Qualitative Metrics

1. **User Satisfaction:** Reduction in user complaints about language setting friction
2. **Workflow Efficiency:** Manual workflow users report easier configuration
3. **Support Burden:** Decrease in support requests about language settings

### Quantitative Metrics

1. **No Regressions:** All existing tests pass
2. **No Crashes:** Crash-free rate remains at current baseline
3. **No Performance Degradation:** UI render time unchanged

### Validation Methods

1. **Manual Testing:** QA team tests on various devices/configurations
2. **Beta Testing:** Small user group validates workflow improvement
3. **Monitoring:** Watch for crash reports or unexpected behavior post-release

---

**Document Status:** Ready for Implementation
**Last Updated:** 2026-01-05
**Next Phase:** Implementation Plan (07-implementation-plan.md)
