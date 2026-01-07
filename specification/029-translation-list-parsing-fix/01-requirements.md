# Requirements - Fix Translation List Parsing Missing First Paragraph

**Spec Index:** 029
**Feature Name:** Fix Translation List Parsing - Missing First Paragraph and Incorrect Matching
**Date:** 2026-01-07
**Phase:** 2 - Requirements Clarification
**Status:** Draft
**Type:** Bug Fix

## 1. Executive Summary

**Problem:** Translation parsing has issues with list content - specifically:
1. First paragraph of list items is missing in translation
2. Original text and Chinese translation are incorrectly matched
3. Paragraph indexing misaligns when rendering RSS content with lists

**Evidence:** Screenshot shows "Story at-a-glance" section with a list where:
- List items have English titles (e.g., "Information overload crisis", "Filters create reality")
- Chinese translations are misaligned or missing first paragraphs
- Some items show completely mismatched content (e.g., "Patient-focused healing" paired with "通往疾病之路")

**Impact:** High - Users receive incorrect or incomplete translations, making the feature unreliable for RSS articles with structured content like lists.

## 2. Problem Analysis

### 2.1 Current Understanding

From screenshot analysis:
- **Structure:** Title → Section Header → Bulleted List (4 items)
- **Each item should have:** English title + Chinese translation paragraph(s)
- **Actual behavior:**
  - First bullet: English present, Chinese appears truncated
  - Second bullet: Chinese text ends abruptly ("僵化对立场" incomplete)
  - Fourth bullet: Chinese text ("通往疾病之路") doesn't match English title ("Patient-focused healing")

### 2.2 Potential Root Causes

Based on previous specs (spec-020, spec-026):

1. **Paragraph Extraction Issue** (spec-020):
   - Previous fix addressed nested lists and blockquotes
   - Current issue may be related to how list item content is indexed
   - First paragraph may be skipped during extraction

2. **Index Computation Mismatch**:
   - `computeParagraphIndices()` may not correctly map list item positions
   - Off-by-one errors could cause misalignment
   - Multiple paragraphs in list items may not be counted correctly

3. **Parsing Response Issue** (spec-020 debug analysis):
   - LLM responses may be truncated
   - Missing closing brackets could cause parsing failures
   - Fallback logic may insert incorrect content

### 2.3 What Makes This Different from Previous Fixes

**spec-013** (translation page): Initial translation feature
**spec-014** (translation function): Translation API integration
**spec-020** (improve translation page): Fixed nested lists and blockquotes
**spec-026** (improve summary render JSON): Fixed raw JSON display

**THIS SPEC (029):**
- Focuses on **first paragraph missing** in list items
- Focuses on **incorrect matching** between original and translated text
- Requires understanding **RSS rendering structure** to correctly index paragraphs

## 3. Requirements

### 3.1 Functional Requirements

**FR-1: Complete List Item Extraction**
- Extract ALL paragraphs from list items, including first paragraph
- Preserve order of paragraphs within list items
- Handle multiple paragraphs per list item

**FR-2: Accurate Index Mapping**
- Correctly map original paragraphs to translated paragraphs
- Maintain 1:1 correspondence between original and translation
- Handle cases where list items have varying paragraph counts

**FR-3: RSS Structure Awareness**
- Understand how RSS feed content is parsed into `LinearElement` structure
- Correctly traverse nested content in list items
- Preserve semantic structure during translation

**FR-4: Robust Parsing**
- Gracefully handle incomplete AI responses
- Detect and report mismatched paragraph counts
- Provide fallback for failed parsing

### 3.2 Non-Functional Requirements

**NFR-1: Performance**
- No significant performance degradation
- Maintain current translation speed
- Efficient index computation

**NFR-2: Backward Compatibility**
- Don't break existing translation functionality
- Maintain compatibility with previous AI responses
- Keep existing data structures

**NFR-3: Error Handling**
- Never show incorrect translations to users
- Log parsing errors for debugging
- Provide user feedback for failures

### 3.3 Data Requirements

**DR-1: LinearElement Structure**
```
LinearListItem
├─ ids: Set<String>
├─ orderedIndex: Int?
└─ content: List<LinearElement>
    ├─ LinearText (first paragraph) ← MAY BE MISSING
    ├─ LinearText (second paragraph)
    └─ LinearText (third paragraph)
```

**DR-2: Translation Mapping**
```
Original: [List item with 3 paragraphs]
↓ Extract
[Para1, Para2, Para3]
↓ Translate
[Translated1, Translated2, Translated3]
↓ Map Back
List item with [Translated1, Translated2, Translated3]
```

## 4. User Stories

**US-1:** As a user reading translated articles with lists, I should see complete translations of all list item paragraphs, not missing the first paragraph.

**US-2:** As a user, I should see the correct Chinese translation matched with each English section, not misaligned or swapped content.

**US-3:** As a user, if translation fails for a list item, I should see the original text or an error message, not incorrect translation.

## 5. Acceptance Criteria

**AC-1:** All paragraphs in list items are extracted for translation, including first paragraph
**AC-2:** Translated paragraphs are correctly mapped back to original positions
**AC-3:** List items with multiple paragraphs maintain paragraph order and count
**AC-4:** No misalignment between English and Chinese text in translated lists
**AC-5:** Error handling prevents displaying incorrect translations

## 6. Success Metrics

- 100% of list item paragraphs extracted for translation
- 0% misalignment between original and translated content
- User-reported translation issues reduced by 95%
- Translation accuracy for list content > 90%

## 7. Scope

### In Scope
- Fixing paragraph extraction from list items
- Fixing index computation for list content
- Improving translation parsing for list structures
- Researching RSS rendering structure

### Out of Scope
- Changing AI translation prompts
- Modifying translation UI
- Changing translation API providers
- Performance optimizations beyond fix

## 8. Related Specifications

- **spec-013**: Translation page - Initial feature
- **spec-014**: Translation function - API integration
- **spec-020**: Improve translation page - Nested lists and blockquotes
- **spec-026**: Improve summary render JSON - Raw JSON display fix
- **spec-011**: Translation config - Configuration settings

## 9. Dependencies

- Kotlin 2.2.20
- kotlinx.serialization
- Existing translation parsing code
- AI provider clients (Anthropic, OpenAI-compatible)
- LinearElement content structure

## 10. Open Questions

1. **Q1:** Does the issue affect all list items or only those with specific structures?
   - **A:** Needs research - Phase 3 will investigate

2. **Q2:** Is the first paragraph missing during extraction or during rendering?
   - **A:** Needs debugging - Phase 4 will analyze

3. **Q3:** How does RSS feed structure map to LinearElement?
   - **A:** Needs research - Phase 3 will document

4. **Q4:** Are there other content types with similar issues?
   - **A:** Needs investigation - Phase 4 will assess

## 11. Next Steps

1. **Phase 3:** Research RSS rendering and page structure
2. **Phase 4:** Debug analysis - root cause identification
3. **Phase 5:** Code assessment - evaluate translation/parsing codebase
4. **Phase 6:** Write technical specification
5. **Phase 8:** Implement fix and test

---

**Requirements Clarification Complete**
**Ready for Phase 3 (Research)**
