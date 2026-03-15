# Code Review: Improve Text Extraction for Translation (Spec-32)

**Date:** 2026-03-15 (updated after re-review)
**Reviewer:** super-dev:code-reviewer
**Status:** Approved with Comments
**Base SHA:** ai-features
**Head SHA:** 4c26cb2d (spec-32-improve-text-extraction)

## Summary Statistics

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 0 (1 fixed) |
| Medium | 1 |
| Low | 1 |
| Info | 2 |

| Dimension | Issues |
|-----------|--------|
| Correctness | 1 (1 fixed) |
| Security | 0 |
| Performance | 0 |
| Maintainability | 1 |
| Testability | 0 |
| Error Handling | 1 |
| Consistency | 1 |
| Accessibility | 0 |

## Specification Validation

| Criterion | Status | Evidence |
|-----------|--------|----------|
| AC-1: Preserve inline formatting via XML tags for all 10 annotation types | Met | `TranslatableTextExtractor.toTaggedText()` handles all 10 types with correct tag mapping. `InlineTagParser` correctly resolves all 10 tags to `SpanStyle`/`LinkAnnotation`. Tests verify each type individually. |
| AC-2: Translate table cell content | Met | `TranslatableTextExtractor.extractRecursively()` handles `LinearTable` at line 71-78. `LinearTableContent` threads `translatedParagraphs` and `tableTranslationStartIndex`. Tests at `TranslatableTextExtractorTest.kt:429-501`. |
| AC-3: Translate image captions | Met | `TranslatableTextExtractor.extractRecursively()` handles `LinearImage` at line 80-93. `LinearImageContent` receives `captionTranslation` at line 709. Tests at `TranslatableTextExtractorTest.kt:506-596`. |
| AC-4: Fix deeply nested list index mismatch | Met | Index mapping fixed via `countTranslatableTexts()`. Rendering path fixed in commit 4c26cb2d by propagating `parentTranslationIndex` through `LinearListItemContent` → `LinearElementContent`. All nesting depths now receive correct start indices. |
| AC-5: Eliminate duplicated prompt/parsing code | Met | `TranslationPromptBuilder` extracts all 6 duplicated functions. Both `OpenAICompatibleClient` and `AnthropicClient` delegate correctly. ~220 LOC removed per client. |

### Non-Goals Check
- [x] NG-1: No translation of pre-formatted code blocks -- Not implemented (correct)
- [x] NG-2: No translation of audio/video metadata -- Not implemented (correct)
- [x] NG-3: No restructuring of LinearElement model -- Not implemented (correct)
- [x] NG-4: No changes to per-paragraph translation approach -- Not implemented (correct)
- [x] NG-5: No batch table cell optimization -- Not implemented (correct)
- [x] NG-6: No merging text fragments split by inline images -- Not implemented (correct)
- [x] NG-7: No extraction of duplicated summary functions -- Not implemented (correct)

## Findings

### High

**F-001** | Correctness | `LinearArticleContent.kt:645` | **FIXED in 4c26cb2d**
**Issue:** `LinearListItemContent` did not pass `parentTranslationIndex` when calling `LinearElementContent` for nested children, causing deeply nested list items (depth 3+) to use wrong translation start indices.

**Fix verified:** Added `parentTranslationIndex = childTranslationIndices[currentChildIndex - 1]` at line 645. The index arithmetic is correct: `currentChildIndex` is incremented at line 636 before the call, so `currentChildIndex - 1` references the correct map entry for the current element. The fix completes the rendering counterpart of the `countTranslatableTexts()` counting fix.

**Status:** Resolved. AC-4 is now fully met.

---

### Medium

**F-002** | Error Handling | `InlineTagParser.kt:107-117`
**Issue:** When closing tags arrive in wrong order (e.g., `<b><i>text</b></i>`), the `styleStack.removeAt(matchIndex)` correctly removes the matching tag from the tracking stack, but `builder.pop()` pops the LAST pushed entry from the `AnnotatedString.Builder` -- which may be a different tag. This causes the two stacks to desync, applying styles to wrong ranges.

Example: for `<b>bold <i>both</b> italic</i>`:
- `</b>` pops italic from builder (wrong), bold from tracking stack
- `</i>` pops bold from builder (wrong), italic from tracking stack
- Result: "bold " gets bold (correct), "both" gets bold+italic (correct), " italic" gets bold (should be italic)

The spec states "Never throws" -- maintained. The spec says "Mismatched close tags: Ignored" -- this refers to close tags with no matching open, which IS correctly handled. The issue is specifically about misordered closing where a match exists but at wrong stack depth.

**Suggestion:** When `matchIndex` is not the last element in `styleStack`, pop and re-push intermediate entries:
```kotlin
if (matchIndex >= 0) {
    val entry = styleStack.removeAt(matchIndex)
    // Pop all entries above matchIndex from builder, then re-push them
    val entriesToRepush = styleStack.drop(matchIndex)
    for (e in entriesToRepush.reversed()) {
        if (e.linkHref != null && e.tagName == "link") builder.pop()
        else if (e.style != null) builder.pop()
    }
    // Pop the matching entry
    if (entry.linkHref != null && entry.tagName == "link") builder.pop()
    else if (entry.style != null) builder.pop()
    // Re-push remaining entries
    for (e in entriesToRepush) {
        if (e.tagName == "link" && e.linkHref != null) {
            // re-push link...
        } else if (e.style != null) {
            builder.pushStyle(e.style)
        }
    }
}
```

Alternatively, accept the current behavior since LLMs are instructed to preserve nesting and this edge case is rare.

**Rationale:** While the visual degradation is minor and only occurs with malformed LLM output, the current logic has a subtle correctness issue. The spec's "never throws" guarantee is maintained.

---

### Low

**F-003** | Consistency | `TranslationPromptBuilder.kt:30`
**Issue:** Pre-existing: `jsonEscape()` returns escaped text without wrapping in JSON string quotes. The `"text"` field in the prompt JSON is unquoted:
```
{"index": 1, "type": "paragraph", "text": Hello world}
```
Compare with `"type": "$structureInfo"` which IS quoted because `$structureInfo` is interpolated inside raw-string quotes.

**Suggestion:** Either wrap `jsonEscape()` output in quotes at the call site, or rename to clarify it only escapes but doesn't produce a valid JSON string literal:
```kotlin
"""{"index": ${index + 1}, "type": "$structureInfo", "text": "${jsonEscape(tt.text)}"}"""
```

**Rationale:** While this works in practice (LLMs handle the format), it produces technically invalid JSON in the prompt. Pre-existing issue extracted from both clients -- not introduced by this spec.

---

### Info

**F-004** | Consistency | `LinearArticleContent.kt:920-972`
**Issue:** `LinearBlockQuoteContent` only handles `LinearText` and `LinearListItem` children for translation threading. Other element types (`LinearTable`, `LinearImage`, nested `LinearBlockQuote`) within blockquotes have their content index incremented but no translation rendered. Pre-existing limitation of the blockquote renderer, not introduced by this spec.

---

**F-005** | Maintainability | `OpenAICompatibleClient.kt` / `AnthropicClient.kt`
**Issue:** `buildSummaryPrompt()`, `parseSummaryJsonResponse()`, `extractJsonFromMarkdown()`, `parseLegacySummaryResponse()`, and `parseSummaryResponse()` remain duplicated between both clients (~200 LOC). Acknowledged as a non-goal in the spec ("only translation functions are scoped"), noted for future cleanup.

---

## Strengths

- **Counter parity**: `TranslatableTextExtractor.extractRecursively()` and `countTranslatableTexts()` are structurally identical -- handling the same element types, same conditions, same nesting. The unified `computeContentTranslationIndices()` correctly replaces the two previously duplicated functions. (`LinearArticleContent.kt:171-202` vs `TranslatableTextExtractor.kt:42-97`)

- **Tag whitelist security**: `InlineTagParser.KNOWN_TAGS` only recognizes 10 known tags. Unknown tags (including `<script>`, `<img>`) are rendered as literal text, preventing XSS-like injection. (`InlineTagParser.kt:26`)

- **XML escaping round-trip**: Content text is properly escaped in `TranslatableTextExtractor.escapeXmlContent()` (`&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`) and correctly unescaped in `InlineTagParser.tryParseEntity()`. The `mapPositionToEscaped()` function correctly adjusts annotation positions for escaped characters.

- **Composable/non-composable split in InlineTagParser**: The `parse()` / `parseForTest()` / `parseInternal()` pattern cleanly separates Compose-dependent styling from the core parsing logic, enabling thorough unit testing without a Compose test runner.

- **Comprehensive test coverage**: 138 new tests covering all 10 annotation types, nested tags, XML escaping, entity unescaping, malformed input tolerance, all element types in extraction, and prompt format verification.

- **Clean delegation pattern**: Both AI clients now delegate to `TranslationPromptBuilder` with minimal ceremony -- the `translate()` methods are concise and focused on API interaction. (`OpenAICompatibleClient.kt:282-339`, `AnthropicClient.kt:222-266`)

- **Table translation rendering**: The `remember`-based pre-computation of `cellTranslationStartIndices` in `LinearTableContent` avoids redundant recomputation on recomposition. (`LinearArticleContent.kt:1123-1136`)

## Recommendations

- **F-001 is fixed**: The `parentTranslationIndex` propagation was added in commit 4c26cb2d. AC-4 is now fully satisfied.
- **F-002 is optional**: The misordered closing tag case is rare with LLM output. Consider adding a test documenting the current behavior.
- **Future**: Extract remaining duplicated summary code between AI clients (F-005) in a separate spec.

## Verdict

**Approved with Comments**

**Reasoning:** All 5 acceptance criteria are now fully met. The implementation is architecturally sound with proper separation of concerns, comprehensive testing (138 tests, all passing), correct security handling (tag whitelist, XML escaping), and clean delegation patterns. F-001 (the only blocking issue) has been resolved. The remaining findings (F-002 Medium, F-003 Low, F-004/F-005 Info) are non-blocking and acceptable for merge.

**Blocking Issues:** None
