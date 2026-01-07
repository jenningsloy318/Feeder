# Research Report - RSS Rendering and Translation Parsing

**Spec Index:** 029
**Feature Name:** Fix Translation List Parsing - Missing First Paragraph and Incorrect Matching
**Date:** 2026-01-07
**Phase:** 3 - Research
**Status:** Complete

## 1. Executive Summary

This research documents how RSS feeds are rendered in the Feeder app and how translation parsing works, with focus on list structures. The goal is to understand why the first paragraph of list items is missing and why translations are misaligned.

**Key Findings:**
1. RSS HTML is parsed by Jsoup into `LinearElement` tree structure
2. `LinearListItem` contains nested `List<LinearElement>` which can have multiple paragraphs
3. Translation extraction uses recursive traversal (fixed in spec-020)
4. Index computation uses recursive traversal matching extraction
5. **CRITICAL ISSUE:** `computeParagraphIndexRecursive()` assigns translation to LIST ITEM, not individual paragraphs within it

## 2. RSS Content Rendering Pipeline

### 2.1 HTML to LinearElement Conversion

**File:** `HtmlLinearizer.kt`

**Process Flow:**
```
RSS Feed HTML (InputStream)
    ↓
Jsoup.parse() - Parse HTML into DOM
    ↓
linearizeBody() - Convert to LinearElement list
    ↓
For each HTML tag:
    ├─ <p> → LinearText (paragraph)
    ├─ <ul>/<ol> → LinearListItem (list item)
    ├─ <li> → LinearListItem with nested content
    ├─ <blockquote> → LinearBlockQuote
    ├─ <h1>-<h6> → LinearText with heading annotation
    └─ <img>, <video>, <table>, etc.
    ↓
LinearArticle(elements: List<LinearElement>)
```

### 2.2 List Structure Parsing

**Code Location:** `HtmlLinearizer.kt:488-519`

```kotlin
"ul", "ol" -> {
    finalizeAndAddCurrentElement(blockStyle)

    val ordered = element.tagName() == "ol"
    element
        .children()
        .filter { it.tagName() == "li" }
        .forEachIndexed { index, listItem ->
            val item = LinearListItem(
                ids = listItem.allIds(),
                orderedIndex = if (ordered) { index + 1 } else { null },
            ) {
                asElement(blockStyle) {
                    linearizeChildren(
                        listItem.childNodes(),
                        blockStyle = it,
                        baseUrl = baseUrl,
                    )
                }
            }

            if (item.isNotEmpty()) {
                add(item)
            }
        }
}
```

**Key Points:**
- Each `<li>` becomes a `LinearListItem`
- `<li>` child nodes are recursively processed by `linearizeChildren()`
- `<li>` can contain multiple `<p>` tags → multiple `LinearText` elements in content

**Example HTML:**
```html
<ul>
  <li>
    <p>First paragraph</p>
    <p>Second paragraph</p>
  </li>
</ul>
```

**Resulting LinearElement Structure:**
```kotlin
LinearListItem(
    ids = {...},
    orderedIndex = null,
    content = [
        LinearText("First paragraph"),
        LinearText("Second paragraph")
    ]
)
```

### 2.3 LinearElement Hierarchy

**File:** `LinearStuff.kt`

```
LinearElement (sealed interface)
├─ LinearText (leaf - actual text content)
│  ├─ ids: Set<String>
│  ├─ text: String
│  ├─ blockStyle: LinearTextBlockStyle (TEXT, PRE_FORMATTED, CODE_BLOCK)
│  └─ annotations: List<LinearTextAnnotation> (H1-H6, bold, italic, etc.)
│
├─ LinearListItem (container - can have nested elements)
│  ├─ ids: Set<String>
│  ├─ orderedIndex: Int? (null for bullet lists, 1+ for ordered)
│  └─ content: List<LinearElement>
│      ├─ LinearText (first paragraph) ← MAY BE MISSING IN TRANSLATION
│      ├─ LinearText (second paragraph)
│      ├─ LinearListItem (nested list)
│      └─ LinearBlockQuote
│
├─ LinearBlockQuote (container)
│  ├─ ids: Set<String>
│  ├─ cite: String?
│  └─ content: List<LinearElement>
│
├─ LinearImage, LinearVideo, LinearAudio, LinearTable
```

## 3. Translation Extraction Logic

### 3.1 Recursive Extraction (Fixed in spec-020)

**File:** `ArticleViewModel.kt:548-624`

```kotlin
private fun extractTranslatableParagraphs(): List<TranslatableText> {
    val content = viewState.value.articleContent
    val translatableTexts = mutableListOf<TranslatableText>()

    // Recursively extract all translatable text with structure context
    extractTranslatableTextRecursively(
        elements = content.elements,
        translatableTexts = translatableTexts,
        nestingLevel = 0,
    )

    return translatableTexts
}

private fun extractTranslatableTextRecursively(
    elements: List<LinearElement>,
    translatableTexts: MutableList<TranslatableText>,
    nestingLevel: Int = 0,
) {
    for (element in elements) {
        when (element) {
            is LinearText -> {
                if (element.blockStyle == LinearTextBlockStyle.TEXT) {
                    val text = element.text
                    if (text.isNotBlank()) {
                        val elementType = getElementTypeFromAnnotations(element.annotations)
                        translatableTexts.add(
                            TranslatableText(
                                text = text.trim(),
                                elementType = elementType,
                                nestingLevel = nestingLevel,
                            )
                        )
                    }
                }
            }
            is LinearListItem -> {
                // Recursively extract text from list item content
                extractTranslatableTextRecursively(
                    elements = element.content,
                    translatableTexts = translatableTexts,
                    nestingLevel = nestingLevel + 1,
                )
            }
            is LinearBlockQuote -> {
                // Recursively extract text from blockquote content
                extractTranslatableTextRecursively(
                    elements = element.content,
                    translatableTexts = translatableTexts,
                    nestingLevel = nestingLevel + 1,
                )
            }
            else -> {}
        }
    }
}
```

**Extraction Behavior for List with Multiple Paragraphs:**

Given this structure:
```kotlin
LinearListItem(
    content = [
        LinearText("First paragraph"),
        LinearText("Second paragraph")
    ]
)
```

Extraction produces:
```
translatableTexts = [
    TranslatableText("First paragraph", PARAGRAPH, nestingLevel=1),
    TranslatableText("Second paragraph", PARAGRAPH, nestingLevel=1)
]
```

**This is correct - both paragraphs are extracted.**

### 3.2 Index Computation Logic

**File:** `LinearArticleContent.kt:137-237`

```kotlin
private fun computeParagraphIndices(
    elements: List<LinearElement>,
    translatedParagraphs: List<String>?,
): Map<Int, Int?> {
    if (translatedParagraphs == null) {
        return emptyMap()
    }

    val result = mutableMapOf<Int, Int?>()
    val paragraphCounter = ParagraphCounter()

    elements.forEachIndexed { index, element ->
        computeParagraphIndexRecursive(element, index, result, paragraphCounter)
    }

    return result
}

private fun computeParagraphIndexRecursive(
    element: LinearElement,
    elementIndex: Int,
    result: MutableMap<Int, Int?>,
    paragraphCounter: ParagraphCounter,
) {
    when (element) {
        is LinearText -> {
            if (element.blockStyle == LinearTextBlockStyle.TEXT && element.text.isNotBlank()) {
                result[elementIndex] = paragraphCounter.increment()
            } else {
                result[elementIndex] = null
            }
        }
        is LinearListItem -> {
            // Check if list item has translatable text content at the top level
            val hasTranslatableText =
                element.content
                    .filterIsInstance<LinearText>()
                    .filter { it.blockStyle == LinearTextBlockStyle.TEXT }
                    .any { it.text.isNotBlank() }

            if (hasTranslatableText) {
                // Assign a translation to this list item
                result[elementIndex] = paragraphCounter.increment()
            } else {
                // This list item itself doesn't get a translation
                result[elementIndex] = null
            }

            // Note: Nested list items within this content will have their own translations
            // and are rendered as separate elements in the parent's content iteration
        }
        is LinearBlockQuote -> {
            // Block quotes don't get their own translation at the container level
            // Store the STARTING index for blockquote content translations
            result[elementIndex] = paragraphCounter.index

            // Recursively count translatable content within blockquote to advance counter
            element.content.forEach { nested ->
                when (nested) {
                    is LinearText -> {
                        if (nested.blockStyle == LinearTextBlockStyle.TEXT && nested.text.isNotBlank()) {
                            paragraphCounter.increment()
                        }
                    }
                    is LinearListItem -> {
                        val hasTranslatableText =
                            nested.content
                                .filterIsInstance<LinearText>()
                                .filter { it.blockStyle == LinearTextBlockStyle.TEXT }
                                .any { it.text.isNotBlank() }
                        if (hasTranslatableText) {
                            paragraphCounter.increment()
                        }
                    }
                    else -> {}
                }
            }
        }
        else -> {
            result[elementIndex] = null
        }
    }
}
```

## 4. ROOT CAUSE IDENTIFIED

### 4.1 The Problem

**Index Computation Mismatch:**

For a `LinearListItem` with multiple paragraphs:

```kotlin
LinearListItem(
    content = [
        LinearText("First paragraph"),    // Extracted as index 0
        LinearText("Second paragraph")    // Extracted as index 1
    ]
)
```

**Extraction produces:**
```
translatableTexts = [
    "First paragraph",   // index 0
    "Second paragraph"   // index 1
]
```

**Index computation assigns:**
```
result[listItemIndex] = 0  // Only ONE translation for the entire list item!
```

**Rendering:**
- `LinearListItem` gets translation at index 0 ("First paragraph translated")
- The second paragraph ("Second paragraph" / index 1) has nowhere to render!
- Second paragraph is effectively **dropped** or shown in original language

### 4.2 Why This Happens

The comment in the code says:
```kotlin
// Note: Nested list items within this content will have their own translations
// and are rendered as separate elements in the parent's content iteration
```

**But this is INCORRECT for multiple paragraphs!**

The code assumes:
- Each `LinearListItem` contains only ONE translatable `LinearText`
- OR nested `LinearListItem`s are separate elements in the parent array

**Reality:**
- A `LinearListItem` can contain MULTIPLE `LinearText` elements (multiple paragraphs)
- These paragraphs are NOT separate elements in the parent array
- They are nested INSIDE the `LinearListItem.content` list

### 4.3 Visual Representation

**Expected behavior:**
```
LinearListItem (elementIndex=5)
├─ LinearText("First") → translation[0] → Display translation
└─ LinearText("Second") → translation[1] → Display translation
```

**Actual behavior:**
```
LinearListItem (elementIndex=5) → translation[0]
├─ LinearText("First") → ???
└─ LinearText("Second") → ???
```

The second paragraph has no translation assigned to it because the index computation only assigns ONE translation to the parent `LinearListItem`, not to each nested `LinearText`.

### 4.4 How Rendering Works

**File:** `LinearArticleContent.kt:269-300`

```kotlin
val paragraphIndexForPosition = computeParagraphIndices(articleContent.elements, translatedParagraphs)

items(count = articleContent.elements.size) { index ->
    val element = articleContent.elements[index]

    // Get translation for this element position
    val translation = paragraphIndexForPosition[index]?.let { paragraphIndex ->
        translatedParagraphs?.getOrNull(paragraphIndex)
    }

    LinearElementContent(
        linearElement = element,
        translation = translation,
        // ...
    )
}
```

**The issue:**
- `paragraphIndexForPosition[5] = 0` assigns translation to position 5 (the `LinearListItem`)
- But position 5's nested `LinearText` elements (at positions 6, 7 in nested content) don't get individual translations
- They all share the same translation object meant for the parent

## 5. Data Flow Analysis

### 5.1 Correct Flow (Extraction)

```
HTML: <li><p>P1</p><p>P2</p></li>
    ↓
LinearListItem(content=[LinearText("P1"), LinearText("P2")])
    ↓
extractTranslatableTextRecursively()
    ├─ Visit LinearListItem
    ├─ Recurse into content
    ├─ Extract "P1" → translatableTexts[0]
    └─ Extract "P2" → translatableTexts[1]
    ↓
Result: ["P1 translated", "P2 translated"]
```

### 5.2 Incorrect Flow (Index Computation)

```
LinearListItem(content=[LinearText("P1"), LinearText("P2")])
    ↓
computeParagraphIndexRecursive()
    ├─ Check: hasTranslatableText = true
    ├─ Assign: result[itemIndex] = 0
    └─ Return (NO RECURSION into content for nested LinearText!)
    ↓
Result: {5: 0}  // Only ONE translation assigned to list item
```

### 5.3 Rendering Problem

```
paragraphIndexForPosition = {5: 0}
translatedParagraphs = ["P1 translated", "P2 translated"]
    ↓
Render LinearListItem at position 5
    ├─ translation = translatedParagraphs[0] = "P1 translated"
    ├─ Render nested content
    │   ├─ LinearText("P1") → Gets "P1 translated" ✅
    │   └─ LinearText("P2") → Gets "P1 translated" AGAIN? ❌
    │       OR gets NO translation? ❌
    └─ Result: P2 is missing or incorrectly translated
```

## 6. Comparison with spec-020 Fix

**spec-020** fixed recursive **extraction**:
- Added `extractTranslatableTextRecursively()` to traverse nested content
- Correctly extracts ALL paragraphs from nested structures

**BUT** index computation wasn't fully aligned:
- `computeParagraphIndexRecursive()` was updated for nested `LinearListItem` and `LinearBlockQuote`
- **However**, it assigns translation to the CONTAINER, not each nested `LinearText`
- This works for nested containers (they are separate elements)
- **FAILS** for multiple `LinearText` within same container

## 7. Solution Direction

The fix requires:

1. **Option A:** Assign translations to each `LinearText` within a `LinearListItem`, not to the list item itself
2. **Option B:** Change rendering to pass translations down to nested `LinearText` elements
3. **Option C:** Modify index computation to track nested element positions separately

**Recommended: Option A** - Align index computation with extraction logic by assigning translations to actual text elements, not container elements.

## 8. Related Files

- `HtmlLinearizer.kt` - HTML to LinearElement parsing
- `LinearStuff.kt` - LinearElement data structures
- `ArticleViewModel.kt` - Translation extraction logic
- `LinearArticleContent.kt` - Index computation and rendering

## 9. Next Steps

1. **Phase 4:** Debug analysis - Create test cases to confirm root cause
2. **Phase 5:** Code assessment - Evaluate fix options
3. **Phase 6:** Write technical specification for chosen solution

---

**Research Complete**
**Ready for Phase 4 (Debug Analysis)**
