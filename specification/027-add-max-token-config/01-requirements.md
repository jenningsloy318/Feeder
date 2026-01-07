# Requirements Document: Add max_tokens Configuration

**Spec ID:** 027-add-max-token-config
**Date:** 2026-01-07
**Status:** DRAFT
**Author:** Coordinator Agent

---

## 1. Executive Summary

Add a `max_tokens` configuration field to the AI provider edit/creation page in the Feeder application. This field will control the maximum number of tokens that the language model can output in a single response.

---

## 2. User Story

**As a** Feeder user configuring AI providers
**I want to** set a maximum token limit for model outputs
**So that** I can control response length, costs, and prevent excessively long outputs

---

## 3. Functional Requirements

### 3.1 Core Requirements

1. **FR-1:** Add a `max_tokens` input field to the provider edit/creation screen
2. **FR-2:** Support `max_tokens` configuration for both OpenAI and Anthropic providers
3. **FR-3:** Validate `max_tokens` input (must be a positive integer)
4. **FR-4:** Persist `max_tokens` value with provider configuration
5. **FR-5:** Apply `max_tokens` limit when making API calls to LLM providers

### 3.2 Input Validation

- **VR-1:** `max_tokens` must be an integer
- **VR-2:** `max_tokens` must be >= 1
- **VR-3:** `max_tokens` must be <= provider-specific maximum:
  - OpenAI: Typically 4096 or higher (depends on model)
  - Anthropic: Typically 4096 or higher (depends on model)
- **VR-4:** Empty/default value should use provider's default max_tokens

### 3.3 Data Model Requirements

1. **DM-1:** Add `maxTokens?: Int?` property to `OpenAISettings`
2. **DM-2:** Add `maxTokens?: Int?` property to `AnthropicSettings`
3. **DM-3:** Ensure backward compatibility (null = use provider default)

### 3.4 UI Requirements

1. **UI-1:** Add labeled text input field for max_tokens
2. **UI-2:** Include placeholder/hint text showing valid range
3. **UI-3:** Display validation error for invalid input
4. **UI-4:** Position field logically with other advanced settings
5. **UI-5:** Support keyboard navigation (IME action: Next/Done)

### 3.5 API Integration Requirements

1. **API-1:** Pass `max_tokens` parameter to OpenAI API when configured
2. **API-2:** Pass `max_tokens` parameter to Anthropic API when configured
3. **API-3:** Handle API errors if max_tokens exceeds model limits

---

## 4. Non-Functional Requirements

### 4.1 Performance
- **NFR-1:** Adding max_tokens should not impact API call latency
- **NFR-2:** Input validation should be instant (< 100ms)

### 4.2 Usability
- **NFR-3:** Input field should be intuitive and discoverable
- **NFR-4:** Clear error messages for validation failures
- **NFR-5:** Consistent with existing provider configuration UI

### 4.3 Compatibility
- **NFR-6:** Backward compatible with existing provider configurations (null defaults)
- **NFR-7:** Forward compatible with future provider types

### 4.4 Maintainability
- **NFR-8:** Follow existing code patterns and architecture
- **NFR-9:** Minimal code duplication
- **NFR-10:** Clear separation of concerns (UI, logic, data)

---

## 5. Acceptance Criteria

### AC-1: UI Display
- [ ] Max tokens field is visible on provider edit/creation screen
- [ ] Field is properly labeled with localized string
- [ ] Placeholder/hint text guides user on valid input

### AC-2: Input Validation
- [ ] Positive integers are accepted
- [ ] Non-integer input shows validation error
- [ ] Zero or negative values show validation error
- [ ] Empty value is allowed (uses provider default)

### AC-3: Data Persistence
- [ ] Max tokens value is saved with provider config
- [ ] Value persists across app restarts
- [ ] Value is correctly loaded when editing existing provider

### AC-4: API Integration
- [ ] Max tokens parameter is included in API requests when set
- [ ] API requests succeed with valid max_tokens value
- [ ] Provider default is used when max_tokens is null

### AC-5: Backward Compatibility
- [ ] Existing provider configurations without max_tokens continue to work
- [ ] Null max_tokens values do not cause errors

---

## 6. Out of Scope

The following items are explicitly OUT OF SCOPE for this feature:

- **OS-1:** Temperature configuration (already implemented)
- **OS-2:** Top-p configuration (already implemented)
- **OS-3:** Streaming response configuration (separate feature)
- **OS-4:** Token counting/display in UI (separate feature)
- **OS-5:** Dynamic max_tokens adjustment based on input length
- **OS-6:** Per-request max_tokens override
- **OS-7:** max_tokens presets or templates

---

## 7. Assumptions and Dependencies

### Assumptions
- **A-1:** Users have basic understanding of token limits
- **A-2:** Provider APIs support max_tokens parameter
- **A-3:** Existing provider configuration infrastructure is stable

### Dependencies
- **D-1:** Existing `ProviderConfig` data model
- **D-2:** Existing `ProviderEditScreen` UI
- **D-3:** Existing provider API integration code
- **D-4:** Kotlin serialization for data persistence

---

## 8. Open Questions

1. **Q-1:** What should be the default max validation limit for each provider?
   - **Proposed:** OpenAI: 4096, Anthropic: 4096 (common baseline)

2. **Q-2:** Should we show the current token count in the UI?
   - **Proposed:** No, out of scope (see OS-4)

3. **Q-3:** Should max_tokens be required or optional?
   - **Proposed:** Optional (null = provider default)

4. **Q-4:** How should we handle API errors for excessive max_tokens?
   - **Proposed:** Show error message from API, allow user to adjust

---

## 9. Success Metrics

- **SM-1:** 100% of acceptance criteria met
- **SM-2:** Zero regressions in existing provider functionality
- **SM-3:** Unit test coverage > 80% for new code
- **SM-4:** UI passes accessibility checks
- **SM-5:** No increase in API call failure rate

---

## 10. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Provider API changes max_tokens parameter | High | Low | Use well-documented, stable API parameters |
| Users set max_tokens too low, causing poor responses | Medium | Medium | Provide sensible placeholder/hint text |
| Validation logic differs between providers | Medium | Low | Research provider-specific limits, document clearly |
| Breaking existing provider configs | High | Very Low | Use nullable field with null = default behavior |

---

**End of Requirements Document**
