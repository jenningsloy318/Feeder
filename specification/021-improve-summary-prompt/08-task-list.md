# Task List: Improve Summary Prompt

**Spec ID:** 021
**Status:** Ready for Implementation
**Last Updated:** 2026-01-05

## Task Status Legend

- [ ] Pending
- [x] Complete
- [~] In Progress
- [!] Blocked

## Phase 1: Data Model Updates

### Task 1.1: Create Summary Response Data Model
**File:** `AIClient.kt`
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Create `@Serializable data class SummaryResponseJson`
  - [ ] `language: String` (ISO 639-1 code)
  - [ ] `title: String` (extracted title)
  - [ ] `keyPoints: List<String>` (3-5 key points)
  - [ ] `summary: String` (structured markdown)
  - [ ] `sentiment: String` (positive/negative/neutral/mixed)
- [ ] Update `SummaryResult.Success` data class
  - [ ] Add `title: String = ""`
  - [ ] Add `keyPoints: List<String> = emptyList()`
  - [ ] Add `sentiment: String = ""`
  - [ ] Keep `content` field for backward compatibility

### Task 1.2: Add Summary Timeout Constants
**File:** `SettingsStore.kt`
**Estimate:** 30 minutes
**Status:** [ ]

- [ ] Add constant `PREF_SUMMARY_TIMEOUT_SECONDS`
- [ ] Add constant `DEFAULT_SUMMARY_TIMEOUT_SECONDS = 90`
- [ ] Create `private val _summaryTimeout = MutableStateFlow(...)`
- [ ] Expose `val summaryTimeout: StateFlow<Int>`
- [ ] Create `fun setSummaryTimeout(value: Int)`
  - [ ] Coerce value to range 30-600
  - [ ] Update MutableStateFlow
  - [ ] Persist to SharedPreferences

### Task 1.3: Expose Timeout in Repository
**File:** `Repository.kt`
**Estimate:** 15 minutes
**Status:** [ ]

- [ ] Add `val summaryTimeout = settingsStore.summaryTimeout`

---

## Phase 2: Prompt Enhancements

### Task 2.1: Update Anthropic Summary Prompt
**File:** `AnthropicClient.kt` - `buildSummaryPrompt()`
**Estimate:** 2 hours
**Status:** [ ]

- [ ] Add professional role assignment
  - [ ] "You are an expert news analyst and professional journalist..."
- [ ] Add clear task description
  - [ ] "## Task" section
  - [ ] "Summarize the following news article..."
- [ ] Add JSON output format specification
  - [ ] "## Output Format" section
  - [ ] JSON schema with all fields
  - [ ] Field descriptions and types
- [ ] Add summarization guidelines
  - [ ] "## Summarization Guidelines" section
  - [ ] "### Quality Standards" (5 standards)
  - [ ] "### Structure Requirements" (5 requirements)
  - [ ] "### Language Handling" (2 rules)
  - [ ] "### Content Guidelines" (5 guidelines)
  - [ ] "### Format Rules" (5 rules)
- [ ] Add important reminders
  - [ ] "## Important" section
  - [ ] "ONLY the JSON object, no surrounding text"
- [ ] Handle both language modes
  - [ ] AUTO_DETECT with language detection
  - [ ] Specific language with fixed language

### Task 2.2: Update OpenAI Summary Prompt
**File:** `OpenAICompatibleClient.kt` - `buildSummaryPrompt()`
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Copy updated prompt from AnthropicClient
- [ ] Verify identical structure
- [ ] Test with OpenAI API

### Task 2.3: Increase Max Tokens
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`
**Estimate:** 15 minutes
**Status:** [ ]

- [ ] Update `maxTokens` from 1024 to 2048
- [ ] Add comment explaining why (JSON response)
- [ ] Verify in both clients

---

## Phase 3: JSON Parsing Implementation

### Task 3.1: Implement JSON Parser
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`
**Estimate:** 3 hours
**Status:** [ ]

- [ ] Add kotlinx.serialization imports
  - [ ] `import kotlinx.serialization.SerializationException`
  - [ ] `import kotlinx.serialization.json.Json`
  - [ ] `import kotlinx.serialization.json.jsonObject`
  - [ ] `import kotlinx.serialization.json.jsonPrimitive`
- [ ] Create Json parser instance
  - [ ] `private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }`
- [ ] Create `SummaryResponseData` data class
  - [ ] Same fields as `SummaryResponseJson`
- [ ] Implement `parseSummaryJsonResponse(content: String)`
  - [ ] Extract JSON from markdown code blocks
  - [ ] Parse JSON with kotlinx.serialization
  - [ ] Extract all fields with validation
  - [ ] Handle missing fields with defaults
  - [ ] Catch SerializationException → fallback
  - [ ] Catch Exception → fallback
- [ ] Implement `extractJsonFromMarkdown(content: String)`
  - [ ] Try ```json code blocks
  - [ ] Try ``` code blocks
  - [ ] Return content as-is if no blocks
- [ ] Implement `parseLegacySummaryResponse(content: String)`
  - [ ] Keep existing parsing logic
  - [ ] Return SummaryResponseData with defaults

### Task 3.2: Update generateSummary Method
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Replace `parseSummaryResponse()` with `parseSummaryJsonResponse()`
- [ ] Extract fields from SummaryResponseData
- [ ] Update `SummaryResult.Success()` call
  - [ ] `content = summaryData.summary`
  - [ ] `detectedLanguage = summaryData.language`
  - [ ] `title = summaryData.title`
  - [ ] `keyPoints = summaryData.keyPoints`
  - [ ] `sentiment = summaryData.sentiment`
- [ ] Verify backward compatibility

---

## Phase 4: Timeout Implementation

### Task 4.1: Update AIApi for Timeout
**File:** `AIApi.kt` - `generateSummary()`
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Get summary timeout
  - [ ] `val summaryTimeout = repository.summaryTimeout.first()`
- [ ] Create settings with timeout
  - [ ] Match on aiSettings
  - [ ] For OpenAI: `copy(timeoutSeconds = summaryTimeout)`
  - [ ] For Anthropic: `copy(timeoutSeconds = summaryTimeout)`
- [ ] Create client with timeout settings
  - [ ] `val clientWithTimeout = AIClient.create(settingsWithTimeout)`
- [ ] Call generateSummary on timeout-aware client
  - [ ] `return clientWithTimeout.generateSummary(content, language)`

---

## Phase 5: UI Implementation

### Task 5.1: Create SummarySettingsViewModel
**File:** `SummarySettingsViewModel.kt` (NEW)
**Estimate:** 30 minutes
**Status:** [ ]

- [ ] Create class extending ViewModel
- [ ] Inject Repository
- [ ] Expose `val summaryTimeout: StateFlow<Int>`
- [ ] Implement `fun setSummaryTimeout(value: Int)`
  - [ ] Delegate to repository

### Task 5.2: Create SummarySettingsScreen
**File:** `SummarySettingsScreen.kt` (NEW)
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Create `@Composable fun SummarySettingsScreen()`
- [ ] Add SettingsScreen wrapper
  - [ ] Title: "Summary Settings"
- [ ] Add SettingHeader
  - [ ] Title: "Summary Settings"
  - [ ] Subtitle: "Configure AI-powered article summarization"
  - [ ] Icon: Icons.Filled.Timer
- [ ] Add TimeoutSetting component
  - [ ] Title: stringResource(R.string.summary_timeout_title)
  - [ ] Description: stringResource(R.string.summary_timeout_description)
  - [ ] Value: summaryTimeout
  - [ ] OnChange: { viewModel.setSummaryTimeout(it) }

### Task 5.3: Add String Resources
**File:** `app/src/main/res/values/strings.xml`
**Estimate:** 15 minutes
**Status:** [ ]

- [ ] `<string name="summary_settings_title">Summary</string>`
- [ ] `<string name="summary_settings_header">Summary Settings</string>`
- [ ] `<string name="summary_settings_subtitle">Configure AI-powered article summarization</string>`
- [ ] `<string name="summary_timeout_title">Summary Timeout</string>`
- [ ] `<string name="summary_timeout_description">Maximum time to wait for AI to generate a summary</string>`
- [ ] `<string name="summary_timeout_seconds_label">seconds</string>`

### Task 5.4: Update Settings Navigation
**File:** Settings navigation configuration
**Estimate:** 30 minutes
**Status:** [ ]

- [ ] Add "Summary" to AI Integration settings
- [ ] Create navigation route to SummarySettingsScreen
- [ ] Add SummarySettingsViewModel to navigation graph
- [ ] Test navigation flow

---

## Phase 6: Testing

### Task 6.1: Unit Tests for JSON Parsing
**Estimate:** 2 hours
**Status:** [ ]

- [ ] Test valid JSON with all fields
- [ ] Test valid JSON with missing title
- [ ] Test valid JSON with missing sentiment
- [ ] Test JSON in ```json code blocks
- [ ] Test JSON in ``` code blocks
- [ ] Test malformed JSON (fallback to legacy)
- [ ] Test legacy format (Lang: en)
- [ ] Test empty string
- [ ] Test null handling
- [ ] Verify all fields extracted correctly

### Task 6.2: Unit Tests for Timeout
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Test default value (90 seconds)
- [ ] Test minimum value (30 seconds)
- [ ] Test maximum value (600 seconds)
- [ ] Test below minimum coercion
- [ ] Test above maximum coercion
- [ ] Test SharedPreferences persistence
- [ ] Test StateFlow emissions

### Task 6.3: Integration Tests
**Estimate:** 2 hours
**Status:** [ ]

- [ ] Test end-to-end summary generation
- [ ] Test JSON response parsing
- [ ] Test timeout application to API call
- [ ] Test error handling
- [ ] Test with real AI providers (if possible)

### Task 6.4: Manual Testing
**Estimate:** 2 hours
**Status:** [ ]

- [ ] Generate summary for short article (< 200 words)
- [ ] Generate summary for long article (> 1000 words)
- [ ] Generate summary for English article
- [ ] Generate summary for non-English article
- [ ] Test timeout at minimum (30s)
- [ ] Test timeout at maximum (600s)
- [ ] Test timeout at default (90s)
- [ ] Verify settings persistence across app restart
- [ ] Test with news article
- [ ] Test with blog post
- [ ] Test with technical article
- [ ] Verify markdown rendering looks good
- [ ] Verify key points are extracted
- [ ] Verify language detection works

---

## Phase 7: Documentation & Review

### Task 7.1: Update Documentation
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Add inline code comments
  - [ ] Document prompt sections
  - [ ] Document JSON parsing logic
  - [ ] Document timeout implementation
- [ ] Update README (if needed)
- [ ] Update CHANGELOG
  - [ ] Add entry for spec-021
  - [ ] List all changes
- [ ] Update specification document
  - [ ] Document any deviations from spec
  - [ ] Add lessons learned

### Task 7.2: Self-Review
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Review all code changes
- [ ] Verify patterns followed consistently
- [ ] Check for potential bugs
- [ ] Verify error handling is complete
- [ ] Check for hardcoded values
- [ ] Verify no TODO/FIXME left behind
- [ ] Ensure code is clean and readable

### Task 7.3: Final Verification
**Estimate:** 1 hour
**Status:** [ ]

- [ ] Run all unit tests
  - [ ] All tests pass
- [ ] Run all integration tests
  - [ ] All tests pass
- [ ] Build project
  - [ ] Build successful
  - [ ] No compilation errors
  - [ ] No warnings
- [ ] Test on device/emulator
  - [ ] APK installs successfully
  - [ ] App launches without crashes
  - [ ] Summary generation works
  - [ ] Settings UI works
- [ ] Verify git status is clean
  - [ ] All changes committed
  - [ ] No uncommitted files

---

## Pre-Merge Checklist

- [ ] All tasks in Phases 1-5 complete
- [ ] All tasks in Phase 6 complete (testing)
- [ ] All tasks in Phase 7 complete (documentation)
- [ ] Code reviewed
- [ ] No regressions detected
- [ ] All acceptance criteria met
- [ ] Ready for merge

---

## Progress Tracking

**Total Tasks:** 52
**Completed:** 0
**In Progress:** 0
**Pending:** 52
**Completion:** 0%

### Phase Completion Status

- Phase 1: Data Model Updates - [ ] 0/3 tasks
- Phase 2: Prompt Enhancements - [ ] 0/3 tasks
- Phase 3: JSON Parsing - [ ] 0/2 tasks
- Phase 4: Timeout Implementation - [ ] 0/1 tasks
- Phase 5: UI Implementation - [ ] 0/4 tasks
- Phase 6: Testing - [ ] 0/4 tasks
- Phase 7: Documentation & Review - [ ] 0/3 tasks

---

## Notes

### Implementation Tips

1. Work sequentially through phases
2. Test each task as you complete it
3. Commit frequently after each phase
4. Follow translation pattern closely
5. Keep backward compatibility

### Common Pitfalls to Avoid

1. Don't forget to update BOTH Anthropic and OpenAI clients
2. Don't skip error handling in JSON parsing
3. Don't break existing API contracts
4. Don't forget to test fallback scenarios
5. Don't forget to increase max tokens
6. Don't skip updating both clients for max tokens

### Getting Help

Refer to:
- Specification: `03-specification.md`
- Research: `02-research-report.md`
- Code Assessment: `04-code-assessment.md`
- Implementation Plan: `07-implementation-plan.md`
