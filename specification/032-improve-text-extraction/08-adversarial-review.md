# Adversarial Review: Improve Text Extraction for Translation (Spec-32)

**Date:** 2026-03-15
**Reviewer:** super-dev:adversarial-reviewer
**Verdict:** PASS

## Intent

Improve the text extraction and translation pipeline to preserve all 10 inline formatting types through translation using XML-like tags, add table cell and image caption translation support, fix a nested list index mismatch bug, and eliminate ~220 LOC of duplicated prompt/parsing code between AI clients.

## Verdict Summary

Implementation is correct, well-structured, and achieves all stated goals with appropriate fallbacks. No high-severity findings. One medium architectural observation about counter parity enforcement. Two low-severity edge cases.

## Change Scope

| Metric | Value |
|--------|-------|
| Lines changed | 1005 insertions, 753 deletions (252 net) |
| Files changed | 8 (3 created, 5 modified) |
| Size classification | Large |
| Reviewers activated | Skeptic + Architect + Minimalist |
| Attack vectors applied | V1-V7 |

## Destructive Action Gate

**Gate Verdict:** CLEAR

| Check | Status | Evidence |
|-------|--------|----------|
| Data Destruction (DAT) | CLEAR | No destructive data operations |
| Irreversible State (IRR) | CLEAR | No force-push, reset, or irreversible state changes |
| Production Impact (PRD) | CLEAR | No deploy or production config changes |
| Permission Escalation (PRM) | CLEAR | No permission or auth changes |
| Secret Operations (SEC) | CLEAR | No secrets or credential operations |

### HALT Findings
None

## Findings

### High

None

### Medium

**AF-001** | Architect/V1 | `LinearArticleContent.kt:171` + `TranslatableTextExtractor.kt:42`
**Issue:** Counter parity between `countTranslatableTexts()` (LinearArticleContent.kt:171-202) and `TranslatableTextExtractor.extractRecursively()` (TranslatableTextExtractor.kt:42-97) is enforced only by code convention and comments, not by a shared abstraction or automated verification. If a future developer adds a new `LinearElement` subtype or changes extraction criteria in one function without the other, translation indices will silently mismatch.
**Recommendation:** Add an integration-level test that constructs a complex document (with nested lists, tables, images, blockquotes) and verifies that `TranslatableTextExtractor.extract(elements).size` equals the sum of indices assigned by `computeParagraphIndices()`. This catches drift between the two functions. The existing comment "Must mirror TranslatableTextExtractor.extractRecursively() exactly" at line 169 is good documentation but insufficient as a safety net.

### Low

**AF-002** | Skeptic/V1 | `TranslationPromptBuilder.kt:30`
**Issue:** Pre-existing: `jsonEscape()` escapes special characters but does not wrap the result in JSON double quotes. The template at line 30 (`"text": ${jsonEscape(tt.text)}`) produces technically invalid JSON in the prompt (unquoted string values). LLMs handle this correctly in practice, and the behavior was extracted verbatim from the existing AI clients.
**Recommendation:** No action required for this spec. For a future cleanup: either modify `jsonEscape()` to return `"\"$escaped\""` or add quotes in the template. Noting for awareness only.

**AF-003** | Skeptic/V2 | `InlineTagParser.kt:199`
**Issue:** `tryParseTag()` finds the closing `>` via `text.indexOf('>', start)`, which would match a literal `>` inside an attribute value (e.g., `<link href="a>b">`). This could prematurely terminate tag parsing and produce a truncated URL.
**Recommendation:** No action required. The extraction side (`TranslatableTextExtractor.escapeXmlAttribute()` at line 231-236) correctly escapes `>` to `&gt;` in attribute values, so this pattern cannot originate from the extractor. An LLM would need to de-escape `&gt;` back to `>` inside an attribute AND the URL would need to contain `>` -- an extremely unlikely combination. Noting for awareness only.

## Vector Coverage

| Vector | Lens | Findings | Highest Severity |
|--------|------|----------|-----------------|
| V1: False Assumptions | Skeptic, Architect | 2 | Medium |
| V2: Edge Cases | Skeptic | 1 | Low |
| V3: Failure Modes | Skeptic | 0 | -- |
| V4: Adversarial Input | Skeptic | 0 | -- |
| V5: Safety & Compliance | Skeptic, Architect | 0 | -- |
| V6: Grounding Audit | Skeptic | 0 | -- |
| V7: Dependencies | Architect, Minimalist | 0 | -- |

## What Went Well

1. **Graceful degradation design**: The `InlineTagParser` never throws and falls back to plain text on malformed/stripped tags. The worst case is visually identical to pre-spec behavior. This is exactly the right failure model for LLM-dependent formatting.

2. **Counter parity discipline**: `countTranslatableTexts()` meticulously mirrors `extractRecursively()` with identical `when` branches, conditions (`blockStyle == TEXT && text.isNotBlank()`), and traversal order (row-major for tables, recursive for nested containers). The cross-reference comment is present.

3. **Comprehensive test coverage**: 138 new tests covering all 10 annotation types individually, nesting, XML escaping, entity unescaping, unknown/malformed/garbled tags, all element types in extraction, table cell spanning, and prompt format verification. The "never throws" test with 16 adversarial inputs is particularly thorough.

4. **Clean extraction of duplicated code**: The `TranslationPromptBuilder` was extracted without behavioral changes, reducing the blast radius of prompt modifications from 2 files to 1. Both AI clients now delegate identically.

5. **Security boundary preservation**: The `InlineTagParser` whitelist (10 known tags only) prevents any injected `<script>`, `<img>`, or other HTML from being interpreted. Unknown tags render as literal text. Link handling delegates to the same `onLinkClick` callback used for original article rendering.

## Lead Judgment

- **AF-001** (Counter parity): Accept. This is a known architectural trade-off documented in the spec. The functions are in different files by necessity (extraction in `ai/` package, counting in `ui/compose/html/` package). An integration test would add safety but is not blocking for merge.
- **AF-002** (Unquoted JSON): Accept. Pre-existing behavior, not introduced by this spec. Works in practice.
- **AF-003** (Attribute `>` parsing): Accept. Mitigated by extraction-side escaping. Theoretical edge case only.
