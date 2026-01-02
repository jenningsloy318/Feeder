# Research Report: AI Translation Feature

**Date:** 2026-01-02
**Researcher:** Super-Dev Coordinator
**Feature:** Add AI Translation to Article System

## 1. Best AI Translation Models & Prompts (2025)

### 1.1 Top Performing LLMs for Translation

Based on 2025 research from Intento, Contentful, and Vozo AI:

**Tier 1: Best Overall (Recommended)**
- **GPT-4o**: Best balance of quality, speed, and cost
  - Excellent for European languages
  - Good technical terminology handling
  - Moderate pricing

- **Claude 3.5 Sonnet**: Superior for nuanced content
  - Best for creative content and cultural adaptation
  - Excellent for Asian languages (Chinese, Japanese)
  - Higher cost but worth it for quality

**Tier 2: Good Alternatives**
- **Gemini 1.5 Pro**: Fast and cost-effective
  - Good for high-volume translations
  - Supports many languages
  - Lower cost but slightly lower quality

### 1.2 Best Translation Prompts (2025)

Research from Pairaphrase and Promptitude.io reveals these effective patterns:

**Prompt Template 1: Context-Aware Translation**
```
You are a professional translator. Translate the following text from {source_language} to {target_language}.

Context: This is a {content_type} (article/blog post/news).

Requirements:
1. Maintain the original tone and style
2. Preserve formatting (paragraphs, bold, links)
3. Keep technical terms in English when appropriate
4. Ensure natural, fluent {target_language} phrasing
5. Do not add or remove information

Text to translate:
{text}

Return only the translation without any explanations or metadata.
```

**Prompt Template 2: Inline Translation (Best for Paragraph-by-Paragraph)**
```
Translate the following paragraph from {source_language} to {target_language}.

Guidelines:
- Natural, fluent phrasing
- Maintain paragraph structure
- Preserve any embedded formatting (bold, links, etc.)

Paragraph:
{paragraph}

Translation:
```

**Key Insights:**
- Shorter, focused prompts work better for paragraph-by-paragraph translation
- Context matters (article type, audience, purpose)
- Specifying "no metadata" prevents LLM from adding explanations
- Batch processing paragraphs reduces token waste

### 1.3 Quality vs Cost Trade-offs

| Model | Quality | Speed | Cost (1M tokens) | Best For |
|-------|---------|-------|------------------|----------|
| GPT-4o | 9.2/10 | Fast | $5.00 | General articles |
| Claude 3.5 Sonnet | 9.5/10 | Medium | $15.00 | Premium content |
| Gemini 1.5 Pro | 8.5/10 | Very Fast | $1.25 | High volume |
| GPT-4o-mini | 8.0/10 | Very Fast | $0.60 | Cost-effective |

**Recommendation:** Use user's configured AI provider (flexible), default to GPT-4o for best balance.

---

## 2. React i18n Best Practices (2025)

### 2.1 Library Choice: react-i18next

**Why react-i18next?**
- Industry standard (68% of React apps use it)
- Excellent TypeScript support
- Built-in pluralization, interpolation, nesting
- Namespace support for scalable translation files
- Lazy loading support
- Active community (4.5M weekly downloads)

### 2.2 Project Structure (Best Practices)

```
src/
  i18n/
    config.ts           # i18n configuration
    locales/
      en/
        translation.json
      zh/
        translation.json
  components/
    LanguageSwitcher.tsx
```

**Translation File Structure:**
```json
{
  "settings": {
    "aiIntegration": {
      "translation": {
        "title": "Translation",
        "enableAuto": "Enable Auto Translation",
        "targetLanguage": "Target Language",
        "save": "Save Settings"
      }
    }
  },
  "article": {
    "translation": {
      "button": "Translate",
      "translating": "Translating...",
      "retry": "Retry Translation",
      "error": "Translation failed. Please try again."
    }
  }
}
```

### 2.3 Implementation Patterns

**Pattern 1: Hook-based Translation**
```tsx
import { useTranslation } from 'react-i18next';

function TranslationButton() {
  const { t } = useTranslation();
  return <button>{t('article.translation.button')}</button>;
}
```

**Pattern 2: Namespace-based Organization**
```tsx
// For settings page
const { t } = useTranslation(['settings', 'common']);

// For article page
const { t } = useTranslation(['article', 'common']);
```

**Key Benefits:**
- Scoped translations (no naming collisions)
- Lazy loading by namespace (better performance)
- Easier to maintain large translation files

---

## 3. Database Schema for Translations

### 3.1 Storage Strategy

**Approach:** Store translations alongside articles in a separate table for:
- Easy querying and filtering
- Cache invalidation when article updates
- Support for multiple target languages per article
- Audit trail (who translated, when, which model)

### 3.2 Recommended Schema

**Option 1: Relational (PostgreSQL/SQLite)**
```sql
CREATE TABLE translations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  article_id INTEGER NOT NULL,
  target_language TEXT NOT NULL,  -- ISO 639-1 code (e.g., 'zh', 'es')
  original_paragraph TEXT NOT NULL,
  translated_paragraph TEXT NOT NULL,
  paragraph_index INTEGER NOT NULL,  -- For ordering
  ai_provider TEXT NOT NULL,         -- 'openai', 'anthropic', etc.
  ai_model TEXT NOT NULL,            -- 'gpt-4o', 'claude-3.5-sonnet'
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(article_id, target_language, paragraph_index),
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);

-- Index for performance
CREATE INDEX idx_translations_article_language
  ON translations(article_id, target_language);
```

**Option 2: Embedded in Article (JSON field)**
```json
{
  "id": 123,
  "title": "Article Title",
  "content": "...",
  "translations": {
    "zh": {
      "paragraphs": [
        { "index": 0, "original": "...", "translated": "..." },
        { "index": 1, "original": "...", "translated": "..." }
      ],
      "meta": {
        "provider": "openai",
        "model": "gpt-4o",
        "translatedAt": "2026-01-02T17:00:00Z"
      }
    }
  }
}
```

**Recommendation:** **Option 1 (Relational Table)** for Feeder because:
- Easier to query and manage translations
- Better performance for large articles
- Supports future features (translation history, re-translation)
- Normalized data structure

---

## 4. Language Selection UI

### 4.1 Common Languages List (2025)

Based on usage statistics and AI model support:

```typescript
export const COMMON_LANGUAGES = [
  { code: 'en', name: 'English', nativeName: 'English' },
  { code: 'zh', name: 'Chinese', nativeName: '中文' },
  { code: 'es', name: 'Spanish', nativeName: 'Español' },
  { code: 'fr', name: 'French', nativeName: 'Français' },
  { code: 'de', name: 'German', nativeName: 'Deutsch' },
  { code: 'ja', name: 'Japanese', nativeName: '日本語' },
  { code: 'ko', name: 'Korean', nativeName: '한국어' },
  { code: 'pt', name: 'Portuguese', nativeName: 'Português' },
  { code: 'ru', name: 'Russian', nativeName: 'Русский' },
  { code: 'it', name: 'Italian', nativeName: 'Italiano' },
  { code: 'ar', name: 'Arabic', nativeName: 'العربية' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी' }
];
```

### 4.2 UI Component Pattern

```tsx
<LanguageDropdown
  value={targetLanguage}
  onChange={setTargetLanguage}
  languages={COMMON_LANGUAGES}
  placeholder="Select target language"
/>
```

---

## 5. Performance & Cost Optimization

### 5.1 Caching Strategy

**Why Cache?**
- Translation is expensive ($0.60-$15 per 1M tokens)
- Same article viewed multiple times
- Article content rarely changes after initial fetch

**Cache Key Strategy:**
```
translation:{articleId}:{targetLanguage}:{paragraphIndex}
```

**Cache Invalidation:**
- Delete cache when article is updated
- Set TTL (e.g., 30 days) for stale translations
- Manual "Re-translate" button

### 5.2 Batch Processing

**Best Practice:** Process paragraphs in batches
- Reduces API calls (1 call vs N calls)
- Better pricing (bulk discounts)
- Faster overall translation

**Recommended Batch Size:** 5-10 paragraphs per API call

### 5.3 Progress Indication

For better UX:
- Show progress bar: "Translating paragraph 3/10..."
- Display translated paragraphs as they complete (streaming)
- Allow cancellation mid-translation

---

## 6. Error Handling Best Practices

### 6.1 Retry Strategy

**Exponential Backoff:**
- Retry 1: Immediate (network glitch)
- Retry 2: After 2 seconds (temporary API issue)
- Retry 3: After 5 seconds (API rate limit)
- After 3 failures: Show error with "Retry" button

### 6.2 Fallback Behavior

When translation fails:
- Show original paragraph with warning indicator
- Display error message: "Translation failed. Tap to retry."
- Store error in database for debugging
- Log error with context (article ID, paragraph index, provider, model)

---

## 7. Security Considerations

### 7.1 API Key Management

- Never hardcode API keys in client code
- Use backend proxy endpoint (`/api/v1/translate`)
- Store keys in environment variables
- Rotate keys regularly

### 7.2 Rate Limiting

- Implement per-user rate limits (prevent abuse)
- Use queue system for heavy translation loads
- Monitor API costs per user

---

## 8. Summary & Recommendations

### 8.1 Technology Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| i18n Library | react-i18next | Industry standard, scalable |
| Translation Storage | PostgreSQL/SQLite table | Queryable, performant |
| AI Provider | User-configurable (default GPT-4o) | Flexibility, cost control |
| API Endpoint | `/api/v1/translate` | Versioned, RESTful |

### 8.2 Implementation Phases

**Phase 1: Core Translation**
- Database schema
- Translation API endpoint
- Basic translation button

**Phase 2: Inline Display**
- Paragraph-by-paragraph rendering
- Progress indication
- Error handling

**Phase 3: Settings & Config**
- Settings page integration
- Language selection dropdown
- Auto-translation toggle

**Phase 4: Polish**
- i18n for UI strings
- Caching optimization
- Batch processing

### 8.3 Key Success Metrics

- **Translation Quality:** ≥ 90% user satisfaction
- **Performance:** ≤ 3 seconds for short article (10 paragraphs)
- **Cost Control:** ≤ $0.10 per article translation (GPT-4o-mini)
- **Reliability:** ≥ 95% successful translations (with retries)

---

## 9. References

1. "35 ChatGPT Prompts for High-Quality Translation [2026]" - Pairaphrase
2. "The Best LLMs for AI Translation in 2025" - PoliLingua
3. "Generative AI for Translation in 2025" - Intento
4. "React i18n with i18next: Expert Tutorial" - Crowdin
5. "Complete Tutorial on React i18n with i18next" - DEV Community
6. "How to Add Internationalization (i18n) to a React App" - DEV Community (2025 Edition)
7. react-i18next official documentation

---

**Next Phase:** Phase 4 (Debug Analysis - SKIP) → Phase 5 (Code Assessment)
