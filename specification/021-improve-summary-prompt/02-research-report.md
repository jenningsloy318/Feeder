# Research Report: AI Summary Prompt Best Practices

**Date:** 2026-01-05
**Research Focus:** Best practices for AI-powered text summarization with JSON structured output and markdown content

## Executive Summary

This research report compiles best practices for AI summarization from leading sources in prompt engineering and AI applications. Key findings indicate that structured JSON outputs combined with professional role assignment and clear guidelines produce the most reliable and high-quality summaries.

## Key Research Findings

### 1. Prompt Engineering Techniques for Summarization

Based on research from PromptLayer (2024) and GenAI Unplugged (2025):

#### **Five Essential Prompting Techniques:**

1. **Role-Specific Prompting** (Critical for news summarization)
   - Assign a professional role to the AI (e.g., "expert news analyst", "journalist")
   - Tailors output to specific audience and perspective
   - Improves relevance and focus
   - Example: "As an expert news analyst, summarize this article for busy professionals..."

2. **Instruction-Heavy Prompting**
   - Provide explicit, detailed instructions
   - Specify format requirements clearly
   - Minimize ambiguity
   - Include length constraints and style guidelines

3. **Chain-of-Thought (Step-by-Step) Prompting**
   - Instruct AI to analyze before summarizing
   - Improves accuracy for complex articles
   - Reduces omissions of important details
   - Example: "Analyze the key points step-by-step, then provide a concise summary..."

4. **Zero-Shot Prompting**
   - Rely on AI's general knowledge without examples
   - Fast but can miss nuances
   - Best for straightforward articles

5. **Few-Shot Prompting**
   - Provide sample summaries to guide style
   - Useful for specialized content
   - Helps maintain consistent format

### 2. JSON Structured Output Best Practices

From GenAI Unplugged's comprehensive guide on structured outputs:

#### **Four Layers of Bulletproof JSON Outputs:**

**Layer 1: Clear Schema Definition**
```json
{
  "language": "string (ISO 639-1 code)",
  "title": "string (optional extracted title)",
  "keyPoints": ["array of 3-5 key points"],
  "summary": "string (structured markdown)",
  "sentiment": "string (positive/negative/neutral/mixed)"
}
```

**Layer 2: Strict Field Rules**
- Match field names exactly (case-sensitive)
- Use empty string "" for missing text fields
- Use empty array [] for missing array fields
- No null values or "N/A" text
- No extra fields beyond schema
- Output ONLY the JSON object, no surrounding text

**Layer 3: Perfect Example**
- Show one perfect example before rules
- Model sees target first, then understands rules
- Dramatically improves consistency

**Layer 4: Validation & Refusal Rules**
- Specify when to admit uncertainty
- Return error object for insufficient data
- Prevents hallucination

### 3. News Article Summarization Best Practices

Research from AI summarizer tools and news applications:

#### **Optimal Structure for News Summaries:**

1. **Key Points Section** (Bulleted list)
   - 3-5 main takeaways
   - Concise, scannable
   - Most important information first

2. **Detailed Summary** (Structured markdown)
   - Use markdown headers (##, ###)
   - Logical flow and structure
   - Paragraphs for context
   - Preserve important details

3. **Metadata**
   - Detected language
   - Sentiment analysis (optional)
   - Article title extraction (optional)

#### **Quality Criteria for News Summaries:**

- **Accuracy:** Captures core ideas without distortion
- **Clarity:** Straightforward, reader-friendly language
- **Completeness:** Includes all essential points
- **Brevity:** Concise enough to scan quickly
- **Relevance:** Focuses on what matters to readers
- **Objectivity:** Maintains neutral tone for news

### 4. Markdown Structure for Summary Content

Best practices from AI documentation and markdown standards:

#### **Recommended Markdown Structure:**

```markdown
### Key Points

- Point 1: Most important takeaway
- Point 2: Second critical information
- Point 3: Third key insight
- Point 4: Additional important detail
- Point 5: Final notable point

### Summary

#### Main Topic
[Paragraph about the primary subject]

#### Key Details
[Paragraph with supporting information and context]

#### Conclusion
[Final paragraph with implications or next steps]
```

#### **Markdown Formatting Guidelines:**

- Use H3 (###) for main sections within summary
- Use H4 (####) for subsections
- Use bullet points (-) for lists
- Use bold (**text**) for emphasis sparingly
- Avoid excessive formatting (keep it clean)
- Ensure proper spacing between sections

### 5. Comparison with Current Implementation

#### **Current Implementation Weaknesses:**

1. **Basic Prompt:**
   - No role assignment
   - No specific summarization guidelines
   - No quality criteria
   - Lacks professional context

2. **Fragile Parsing:**
   - Relies on regex to extract "Lang:" prefix
   - No structured output
   - Prone to parsing errors
   - No validation

3. **Unstructured Output:**
   - Plain text only
   - No logical sections
   - Difficult to scan
   - Inconsistent quality

#### **Proposed Improvements:**

1. **Professional Role Assignment**
   - "Expert news analyst and journalist"
   - "Professional summarizer"
   - Focus on accuracy and clarity

2. **JSON Response Format**
   - Reliable parsing (no regex)
   - Clear data structure
   - Type safety
   - Validation possible

3. **Structured Markdown Content**
   - Key points section
   - Detailed summary with headers
   - Logical flow
   - Easy to scan

4. **Quality Guidelines**
   - Explicit instructions
   - Length constraints
   - Format requirements
   - Error handling

### 6. Translation Feature as Reference Pattern

The existing translation implementation demonstrates:

#### **Strengths to Replicate:**

1. **Professional Role Assignment:**
   ```kotlin
   "You are a professional translator with expertise in..."
   ```

2. **Clear Task Description:**
   ```kotlin
   "Your task is to translate the following text..."
   ```

3. **Structured Output:**
   ```kotlin
   "Return the translation as a JSON object..."
   ```

4. **Quality Guidelines:**
   ```kotlin
   "Guidelines: Preserve meaning, tone, and style..."
   ```

5. **Reliable JSON Parsing:**
   ```kotlin
   // No regex, uses proper JSON parsing
   ```

6. **Structure Awareness:**
   ```kotlin
   "Preserve markdown formatting..."
   ```

## Recommended Prompt Structure

Based on research findings, the optimal summary prompt should follow this structure:

```kotlin
"""
You are an expert news analyst and professional journalist specializing in clear, accurate article summarization.

## Task
Summarize the following news article into a well-structured, scannable format.

## Input Format
- Article text will be provided
- Language may vary (detect automatically)
- Article may be short or long

## Output Format
Return ONLY a valid JSON object (no markdown code fences, no additional text):

{
  "language": "detected language code (ISO 639-1, e.g., 'en', 'zh', 'es')",
  "title": "extracted article title or empty string if not found",
  "keyPoints": [
    "point 1: most important takeaway",
    "point 2: second critical information",
    "point 3: third key insight"
  ],
  "summary": "structured markdown with ## Key Points and ## Summary sections",
  "sentiment": "overall sentiment: positive, negative, neutral, or mixed"
}

## Summarization Guidelines

### Quality Standards
- **Accuracy**: Capture all essential information without distortion
- **Clarity**: Use straightforward, reader-friendly language
- **Completeness**: Include main points and important context
- **Brevity**: Keep summary concise but comprehensive
- **Objectivity**: Maintain neutral, journalistic tone for news

### Structure Requirements
- Extract 3-5 key points as bullet points
- Organize summary into logical sections with markdown headers
- Use ### for main sections, #### for subsections
- Include most important information first
- Preserve critical details and context

### Language Handling
- Detect article language automatically
- Summarize in the same language as the original article
- Return language code in 'language' field

### Content Guidelines
- Focus on substantive information, not fluff
- Include relevant data, statistics, quotes if important
- Capture the "who, what, when, where, why, how"
- Maintain journalistic objectivity
- Avoid speculation or opinion

### Format Rules
- keyPoints array: 3-5 strings, each a complete thought
- summary field: Valid markdown with proper formatting
- Use empty string "" if title cannot be determined
- Use "neutral" for sentiment if unclear
- Output ONLY the JSON object, no surrounding text

## Example Output Format

{
  "language": "en",
  "title": "Article Title Here",
  "keyPoints": [
    "Key point 1: Main takeaway or development",
    "Key point 2: Second critical information",
    "Key point 3: Third important detail"
  ],
  "summary": "### Key Points\\n\\n- Key point 1: Main takeaway or development\\n- Key point 2: Second critical information\\n- Key point 3: Third important detail\\n\\n### Summary\\n\\n#### Main Topic\\n\\nParagraph about the primary subject and most important information...\\n\\n#### Key Details\\n\\nParagraph with supporting information, context, and additional details...\\n\\n#### Context\\n\\nParagraph providing background, implications, or relevant additional information...",
  "sentiment": "neutral"
}

## Article to Summarize
${articleText}
"""
```

## Implementation Recommendations

### 1. JSON Schema Definition

```kotlin
@Serializable
data class SummaryResponse(
    val language: String,           // ISO 639-1 code
    val title: String,              // Extracted title
    val keyPoints: List<String>,    // 3-5 key points
    val summary: String,            // Structured markdown
    val sentiment: String           // positive/negative/neutral/mixed
)
```

### 2. Error Handling Strategy

```kotlin
// Validation layers:
1. JSON validity check
2. Required fields presence
3. Field type validation
4. Business logic validation (e.g., language code format)
5. Fallback to current format if JSON parsing fails
```

### 3. Timeout Setting Pattern

Follow translation timeout pattern:
- Add `summaryTimeout` field to repository
- Add UI setting in Settings → AI Integration → Summary
- Apply timeout when calling summary API
- Default value similar to translation timeout

## References

1. **PromptLayer** - "Best Prompts for Text Summarization: Guide to AI Summaries" (Dec 2024)
   - https://blog.promptlayer.com/best-prompts-for-text-summarization-guide-to-ai-summaries/

2. **GenAI Unplugged** - "How to Get Perfect JSON from AI Every Time" (Nov 2025)
   - https://genaiunplugged.substack.com/p/structured-outputs-json-prompts-guide

3. **dair-ai** - "Prompt Engineering Guide" (GitHub)
   - https://github.com/dair-ai/Prompt-Engineering-Guide

4. **OpenAI** - "Best Practices for Prompt Engineering" (Documentation)
   - https://help.openai.com/en/articles/6654000-best-practices-for-prompt-engineering-with-the-openai-api

5. **MIT Sloan** - "Effective Prompts for AI: The Essentials"
   - https://mitsloanedtech.mit.edu/ai/basics/effective-prompts/

## Next Steps

1. ✅ Research complete - best practices identified
2. ⏭️ Proceed to Phase 5: Code Assessment
3. ⏭️ Evaluate current implementation vs research findings
4. ⏭️ Design new prompt based on research
5. ⏭️ Implement JSON response format
6. ⏭️ Add timeout setting following translation pattern
