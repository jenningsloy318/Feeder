# Implementation Plan: Improve Summary Prompt

**Spec ID:** 021
**Status:** Ready for Implementation
**Estimated Time:** 3-5 days

## Overview

This implementation plan details the step-by-step process to enhance the AI-powered article summarization feature with improved prompts, JSON response format, and configurable timeout settings.

## Implementation Phases

### Phase 1: Data Model Updates (Day 1)

#### Task 1.1: Create Summary Response Data Model
**File:** `AIClient.kt` (or new `SummaryResponse.kt`)

**Actions:**
1. Create `@Serializable data class SummaryResponseJson`
2. Add fields: language, title, keyPoints, summary, sentiment
3. Update `SummaryResult.Success` to include new fields with defaults

**Acceptance Criteria:**
- [ ] Data class created with proper serialization
- [ ] All fields have appropriate types
- [ ] Default values maintain backward compatibility

**Estimated Time:** 1 hour

#### Task 1.2: Add Summary Timeout Constants
**File:** `SettingsStore.kt`

**Actions:**
1. Add `PREF_SUMMARY_TIMEOUT_SECONDS` constant
2. Add `DEFAULT_SUMMARY_TIMEOUT_SECONDS = 90`
3. Add MutableStateFlow for summaryTimeout
4. Add public StateFlow for summaryTimeout
5. Add setSummaryTimeout() method with coercion (30-600)

**Acceptance Criteria:**
- [ ] Constants defined
- [ ] StateFlow created and exposed
- [ ] Setter method with range coercion
- [ ] Follows translation timeout pattern exactly

**Estimated Time:** 30 minutes

#### Task 1.3: Expose Timeout in Repository
**File:** `Repository.kt`

**Actions:**
1. Add `val summaryTimeout = settingsStore.summaryTimeout`

**Acceptance Criteria:**
- [ ] Timeout accessible from repository
- [ ] Follows translation pattern

**Estimated Time:** 15 minutes

---

### Phase 2: Prompt Enhancements (Day 1-2)

#### Task 2.1: Update Anthropic Summary Prompt
**File:** `AnthropicClient.kt`

**Actions:**
1. Replace `buildSummaryPrompt()` implementation
2. Add professional role assignment
3. Add clear task description
4. Add JSON output format specification
5. Add comprehensive summarization guidelines
6. Add quality standards
7. Add format rules
8. Include example JSON response
9. Handle both AUTO_DETECT and specific languages

**Acceptance Criteria:**
- [ ] Professional role assigned ("expert news analyst")
- [ ] Clear task description present
- [ ] JSON schema specified
- [ ] Guidelines section present (5 sections)
- [ ] Format rules specified
- [ ] Example JSON included
- [ ] Both language modes handled

**Estimated Time:** 2 hours

#### Task 2.2: Update OpenAI Summary Prompt
**File:** `OpenAICompatibleClient.kt`

**Actions:**
1. Same changes as Task 2.1 for OpenAI client

**Acceptance Criteria:**
- [ ] Identical prompt structure to Anthropic
- [ ] All sections present

**Estimated Time:** 1 hour

#### Task 2.3: Increase Max Tokens
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`

**Actions:**
1. Update `maxTokens` from 1024 to 2048 for JSON responses

**Acceptance Criteria:**
- [ ] Max tokens increased in both clients
- [ ] Comment explains why (JSON response)

**Estimated Time:** 15 minutes

---

### Phase 3: JSON Parsing Implementation (Day 2)

#### Task 3.1: Implement JSON Parser
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`

**Actions:**
1. Add kotlinx.serialization imports
2. Create Json parser instance with lenient settings
3. Create `SummaryResponseData` data class
4. Implement `parseSummaryJsonResponse()` method
5. Implement `extractJsonFromMarkdown()` helper
6. Implement `parseLegacySummaryResponse()` fallback
7. Handle all exceptions

**Acceptance Criteria:**
- [ ] JSON parser configured (ignoreUnknownKeys, isLenient)
- [ ] Extracts JSON from markdown code blocks
- [ ] Validates required fields
- [ ] Returns valid SummaryResponseData
- [ ] Falls back to legacy parsing on error
- [ ] Handles SerializationException
- [ ] Handles generic Exception

**Estimated Time:** 3 hours

#### Task 3.2: Update generateSummary Method
**Files:** `AnthropicClient.kt`, `OpenAICompatibleClient.kt`

**Actions:**
1. Replace `parseSummaryResponse()` call with `parseSummaryJsonResponse()`
2. Extract all fields from SummaryResponseData
3. Update `SummaryResult.Success` call with new fields
4. Keep backward compatibility (content field)

**Acceptance Criteria:**
- [ ] Uses new JSON parser
- [ ] Passes all new fields to Success result
- [ ] Content field populated with summary markdown
- [ ] No breaking changes to existing consumers

**Estimated Time:** 1 hour

---

### Phase 4: Timeout Implementation (Day 2-3)

#### Task 4.1: Update AIApi for Timeout
**File:** `AIApi.kt`

**Actions:**
1. In `generateSummary()` method, before calling client:
2. Get summaryTimeout from repository
3. Create settingsWithTimeout (following translation pattern)
4. Create client with timeout settings
5. Call generateSummary on timeout-aware client

**Acceptance Criteria:**
- [ ] Timeout retrieved from repository
- [ ] Settings copied with timeout
- [ ] Both OpenAI and Anthropic handled
- [ ] Client created with timeout settings
- [ ] Follows translation pattern exactly

**Estimated Time:** 1 hour

---

### Phase 5: UI Implementation (Day 3)

#### Task 5.1: Create SummarySettingsViewModel
**File:** `SummarySettingsViewModel.kt` (NEW)

**Actions:**
1. Create ViewModel class
2. Inject repository
3. Expose summaryTimeout StateFlow
4. Implement setSummaryTimeout() method
5. Follow TranslationSettingsViewModel pattern

**Acceptance Criteria:**
- [ ] ViewModel created
- [ ] Timeout exposed as StateFlow
- [ ] Setter delegates to repository
- [ ] Follows translation pattern

**Estimated Time:** 30 minutes

#### Task 5.2: Create SummarySettingsScreen
**File:** `SummarySettingsScreen.kt` (NEW)

**Actions:**
1. Create SummarySettingsScreen composable
2. Add SettingsScreen wrapper
3. Add SettingHeader with title/subtitle/icon
4. Add TimeoutSetting component
5. Collect state from viewModel
6. Implement onTimeoutChange callback

**Acceptance Criteria:**
- [ ] Composable created
- [ ] Proper title and subtitle
- [ ] Timer icon displayed
- [ ] Timeout slider functional
- [ ] Follows translation screen pattern

**Estimated Time:** 1 hour

#### Task 5.3: Add String Resources
**File:** `app/src/main/res/values/strings.xml`

**Actions:**
1. Add summary_settings_title
2. Add summary_settings_header
3. Add summary_settings_subtitle
4. Add summary_timeout_title
5. Add summary_timeout_description
6. Add summary_timeout_seconds_label

**Acceptance Criteria:**
- [ ] All strings defined
- [ ] Clear and concise
- [ ] Follow translation string pattern

**Estimated Time:** 15 minutes

#### Task 5.4: Update Settings Navigation
**File:** Settings navigation configuration

**Actions:**
1. Add "Summary" option under "AI Integration"
2. Route to SummarySettingsScreen
3. Add SummarySettingsViewModel to graph

**Acceptance Criteria:**
- [ ] Summary appears in settings list
- [ ] Navigates to correct screen
- [ ] ViewModel properly initialized

**Estimated Time:** 30 minutes

---

### Phase 6: Testing (Day 4)

#### Task 6.1: Unit Tests for JSON Parsing
**File:** Test files for AnthropicClient and OpenAICompatibleClient

**Actions:**
1. Test valid JSON with all fields
2. Test valid JSON with missing optional fields
3. Test JSON in markdown code blocks
4. Test malformed JSON (fallback)
5. Test legacy format (Lang: prefix)
6. Test empty strings
7. Test edge cases

**Acceptance Criteria:**
- [ ] All test cases pass
- [ ] 100% coverage of parsing logic
- [ ] Fallback behavior verified

**Estimated Time:** 2 hours

#### Task 6.2: Unit Tests for Timeout
**File:** SettingsStore tests

**Actions:**
1. Test default value (90 seconds)
2. Test range coercion (min: 30, max: 600)
3. Test persistence
4. Test StateFlow emission

**Acceptance Criteria:**
- [ ] All tests pass
- [ ] Range coercion works
- [ ] Persistence verified

**Estimated Time:** 1 hour

#### Task 6.3: Integration Tests
**File:** AIApi integration tests

**Actions:**
1. Test end-to-end summary generation
2. Test JSON response parsing
3. Test timeout application
4. Test error handling

**Acceptance Criteria:**
- [ ] End-to-end flow works
- [ ] JSON responses parsed correctly
- [ ] Timeout applied to API calls
- [ ] Errors handled gracefully

**Estimated Time:** 2 hours

#### Task 6.4: Manual Testing
**Manual testing checklist**

**Test Cases:**
1. Generate summary for short article
2. Generate summary for long article
3. Generate summary in different languages
4. Test timeout setting (min, max, default)
5. Verify settings persistence
6. Test with various article types
7. Verify markdown rendering
8. Verify key points extraction

**Acceptance Criteria:**
- [ ] All manual tests pass
- [ ] Summaries are high quality
- [ ] Timeout works as expected
- [ ] No regressions

**Estimated Time:** 2 hours

---

### Phase 7: Documentation & Code Review (Day 5)

#### Task 7.1: Update Documentation
**Files:** Various documentation files

**Actions:**
1. Update inline code comments
2. Update README if needed
3. Update CHANGELOG
4. Update specification with any deviations

**Acceptance Criteria:**
- [ ] Code is well-commented
- [ ] Documentation accurate
- [ ] Changes documented

**Estimated Time:** 1 hour

#### Task 7.2: Self-Review
**Actions:**
1. Review all changes
2. Ensure patterns followed
3. Check for potential issues
4. Verify test coverage

**Acceptance Criteria:**
- [ ] All code reviewed
- [ ] Patterns consistent
- [ ] No obvious issues

**Estimated Time:** 1 hour

#### Task 7.3: Final Verification
**Actions:**
1. Run all tests
2. Build project successfully
3. Verify no warnings
4. Test on device/emulator

**Acceptance Criteria:**
- [ ] All tests pass
- [ ] Build successful
- [ ] No warnings
- [ ] Works on device

**Estimated Time:** 1 hour

---

## Implementation Order

### Recommended Sequence

1. **Day 1 Morning:** Data Model Updates (Phase 1)
2. **Day 1 Afternoon:** Prompt Enhancements (Phase 2)
3. **Day 2 Morning:** JSON Parsing (Phase 3)
4. **Day 2 Afternoon:** Timeout Implementation (Phase 4)
5. **Day 3:** UI Implementation (Phase 5)
6. **Day 4:** Testing (Phase 6)
7. **Day 5:** Documentation & Review (Phase 7)

### Parallel Work Opportunities

None - implementation should be sequential to avoid conflicts.

---

## Risk Mitigation

### Risk 1: JSON Parsing Failures

**Mitigation:**
- Comprehensive fallback logic
- Extensive test coverage
- Manual testing with real AI responses

### Risk 2: Breaking Changes

**Mitigation:**
- Add new fields as optional with defaults
- Keep content field populated
- Maintain existing API contracts
- Thorough regression testing

### Risk 3: UI Issues

**Mitigation:**
- Follow proven translation pattern
- Reuse existing components
- Test on different screen sizes

---

## Success Criteria

### Must Have (Phase 1 Complete)

- [ ] JSON response format working
- [ ] Enhanced prompts implemented
- [ ] JSON parsing with fallback
- [ ] Timeout setting functional
- [ ] UI screen implemented
- [ ] All tests passing
- [ ] No regressions

### Should Have (If Time Permits)

- [ ] Additional unit tests
- [ ] Performance optimization
- [ ] Enhanced error messages

### Could Have (Future Work)

- [ ] Summary length options
- [ ] Summary style options
- [ ] Advanced sentiment display

---

## Rollout Checklist

### Pre-Merge

- [ ] All tasks complete
- [ ] All tests passing
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] CHANGELOG updated

### Post-Merge

- [ ] Monitor for issues
- [ ] Gather user feedback
- [ ] Track summary quality
- [ ] Monitor token usage

---

## Notes

### Implementation Tips

1. **Follow Translation Pattern:** The translation feature has all the patterns we need. Copy it closely.

2. **Test Incrementally:** Don't wait until the end to test. Test each phase as you complete it.

3. **Keep Backward Compatibility:** Ensure existing code continues to work by populating the `content` field.

4. **Use Fallback:** The legacy format fallback is important for robustness.

5. **Monitor Token Usage:** JSON responses may use more tokens. Monitor during testing.

### Common Pitfalls

1. ❌ Forgetting to update both Anthropic and OpenAI clients
2. ❌ Not handling JSON in markdown code blocks
3. ❌ Breaking backward compatibility
4. ❌ Not testing fallback scenarios
5. ❌ Forgetting to increase max tokens

---

## Contact

For questions or issues during implementation, refer to:
- Specification: `03-specification.md`
- Research: `02-research-report.md`
- Code Assessment: `04-code-assessment.md`
