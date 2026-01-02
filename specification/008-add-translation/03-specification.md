# Technical Specification: AI Translation Feature

**Feature ID:** 008-add-translation
**Status:** Ready for Implementation
**Platform:** Android (Kotlin)
**Architecture:** MVVM + Clean Architecture
**Database:** Room (version 39)

---

## 1. Overview

Add AI-powered translation capability to Feeder RSS reader with:
- Inline paragraph-by-paragraph translation display
- Settings page for translation configuration
- Auto-translation on article open (optional)
- Multi-language support (12 target languages)
- Database caching for translated content
- Integration with existing AI providers (OpenAI, Anthropic)

---

## 2. User Stories

**US1:** As a user, I want to translate articles into my native language so I can read content in languages I don't understand.

**US2:** As a user, I want translations to appear inline below each original paragraph so I can compare and learn.

**US3:** As a user, I want to configure auto-translation so articles are automatically translated when I open them.

**US4:** As a user, I want to choose my target language from a dropdown so I can read in my preferred language.

**US5:** As a user, I want translations to be cached so I don't have to re-translate the same article.

---

## 3. Functional Requirements

### FR1: Translation Button
- Add translation button to article screen top bar
- Position: Immediately after "Fetch Full Article" button
- Icon: `Icons.Default.Translate`
- Tooltip: "Translate" (localized)
- Only show when:
  - Article has full content available
  - AI provider is configured

### FR2: Translation Trigger
- **Manual:** User clicks translate button
- **Auto:** When user opens article (if auto-translate enabled AND full content fetched)

### FR3: Translation Display
- **Inline display:** Each translated paragraph appears below its original paragraph
- **Visual distinction:** Translated text has subtle background color
- **Progress indication:** Show "Translating X/Y..." during translation
- **Error handling:** Show error message with retry option

### FR4: Translation Settings
- **Settings page:** Settings → AI Integration → Translation
- **Enable/disable toggle:** "Enable Auto Translation"
- **Language selector:** Dropdown with 12 target languages
- **Persistence:** Settings saved to SharedPreferences

### FR5: Target Languages
Support 12 languages:
- English (en), Chinese (zh), Spanish (es), French (fr), German (de),
- Japanese (ja), Korean (ko), Portuguese (pt), Russian (ru), Italian (it),
- Arabic (ar), Hindi (hi)

### FR6: Caching
- Store translations in Room database
- Cache key: `articleId + targetLanguage`
- Cache invalidation: Delete on article update
- Reuse cached translations without API calls

### FR7: Error Handling
- **No AI configured:** Link to settings
- **Network error:** Retry button
- **Rate limit:** Show message, auto-retry with backoff
- **Partial success:** Show completed translations, retry failed

### FR8: Internationalization
- All UI strings support English and Chinese
- String resources in `values/strings.xml` and `values-zh/strings.xml`

---

## 4. Non-Functional Requirements

### NFR1: Performance
- Translation progress updates within 100ms
- UI remains responsive during translation
- Max 3 seconds to translate short article (10 paragraphs)

### NFR2: Cost Control
- Use default AI provider from settings
- Batch processing: 5-10 paragraphs per API call
- Aggressive caching to minimize API calls

### NFR3: Reliability
- 95%+ successful translations (with retries)
- Graceful degradation on API failure
- No data loss (database transactions)

### NFR4: Usability
- Clear progress indication
- Intuitive error messages
- Easy language selection

---

## 5. Technical Architecture

### 5.1 Components

| Component | File | Responsibility |
|-----------|------|---------------|
| TranslationManager | `ai/translation/TranslationManager.kt` | Orchestrate translation, caching |
| AIClient (extension) | `ai/AIClient.kt` | Add translate() method |
| TranslationDao | `db/room/TranslationDao.kt` | Database operations |
| Translation | `db/room/Translation.kt` | Room entity |
| TranslationSettingsScreen | `ui/compose/settings/TranslationSettingsScreen.kt` | Settings UI |
| ArticleViewModel (modified) | `ui/compose/feedarticle/ArticleViewModel.kt` | Translation state |
| ArticleScreen (modified) | `ui/compose/feedarticle/ArticleScreen.kt` | Translation button |
| TranslatedParagraph | `ui/compose/feedarticle/TranslationListItem.kt` | Inline display |

### 5.2 Data Flow

```
User Action → ViewModel → TranslationManager → AIClient → AI Provider
     ↓                                                                 ↓
   UI Update ← TranslationState ← TranslationManager ← Database Cache
```

### 5.3 Database Schema

```sql
CREATE TABLE translations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id INTEGER NOT NULL,
    target_language TEXT NOT NULL,
    original_paragraph TEXT NOT NULL,
    translated_paragraph TEXT NOT NULL,
    paragraph_index INTEGER NOT NULL,
    ai_provider TEXT NOT NULL,
    ai_model TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(article_id) REFERENCES feed_items(id) ON DELETE CASCADE
);

CREATE INDEX idx_translations_article_language ON translations(article_id, target_language);
CREATE INDEX idx_translations_article_id ON translations(article_id);
```

### 5.4 API Interface

```kotlin
// AIClient.kt
interface AIClient {
    suspend fun translate(
        paragraph: String,
        targetLanguage: String,
    ): TranslationResult

    sealed interface TranslationResult {
        data class Success(
            val translatedText: String,
            val promptTokens: Int,
            val completionTokens: Int,
            val totalTokens: Int,
        ) : TranslationResult
        data class Error(val message: String, val retryable: Boolean) : TranslationResult
    }
}

// TranslationManager.kt
class TranslationManager(
    private val aiApi: AIApi,
    private val translationDao: TranslationDao,
    private val settingsStore: SettingsStore,
) {
    suspend fun translateArticle(
        articleId: Long,
        paragraphs: List<String>,
        targetLanguage: String,
    ): Flow<TranslationState>
}

sealed class TranslationState {
    object Idle : TranslationState()
    data class Loading(val progress: Int, val total: Int) : TranslationState()
    data class Success(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Error(val message: String, val retryable: Boolean) : TranslationState()
}
```

---

## 6. Implementation Plan

### Phase 1: Foundation (Day 1)
1. Create `TargetLanguage` enum
2. Add translation settings to `SettingsStore`
3. Create `Translation` entity and `TranslationDao`
4. Add database migration (38 → 39)

### Phase 2: AI Integration (Day 1-2)
5. Add `translate()` to `AIClient` interface
6. Implement in `OpenAICompatibleClient`
7. Implement in `AnthropicClient`
8. Create `TranslationManager`

### Phase 3: Settings UI (Day 2)
9. Create `TranslationSettingsScreen`
10. Create `TranslationSettingsViewModel`
11. Add navigation from Settings screen
12. Add string resources (EN + ZH)

### Phase 4: Article Integration (Day 2-3)
13. Add translation button to `ArticleScreen`
14. Add translation state to `ArticleViewModel`
15. Implement translation logic
16. Create `TranslatedParagraph` component
17. Modify `ArticleContent` for inline display

### Phase 5: Testing & Polish (Day 3)
18. Unit tests for `TranslationManager`
19. Integration tests for database
20. UI tests for settings screen
21. Error handling testing
22. Performance optimization

---

## 7. Acceptance Criteria

### AC1: Translation Button
- [ ] Button appears after "Fetch Full Article" button
- [ ] Button has translate icon
- [ ] Button shows correct tooltip (localized)
- [ ] Button disabled during translation
- [ ] Button only shows when AI provider configured

### AC2: Translation Functionality
- [ ] Clicking button translates article
- [ ] Translations appear inline below original paragraphs
- [ ] Progress indicator shows during translation
- [ ] Success state displays all translated paragraphs
- [ ] Error state shows message with retry option

### AC3: Settings Page
- [ ] "Translation" link appears in AI Integration settings
- [ ] Translation settings screen opens
- [ ] Enable/disable toggle works
- [ ] Language dropdown shows 12 languages
- [ ] Language selection persists
- [ ] Settings survive app restart

### AC4: Auto-Translation
- [ ] When enabled, articles auto-translate on open
- [ ] Only triggers when full content is fetched
- [ ] Uses configured target language
- [ ] Shows cached translations if available

### AC5: Caching
- [ ] First translation saves to database
- [ ] Subsequent opens load from cache
- [ ] Cache is checked before API call
- [ ] Re-translate button clears cache

### AC6: Error Handling
- [ ] Network error shows retry button
- [ ] Invalid API key shows settings link
- [ ] Rate limit shows appropriate message
- [ ] Partial success shows completed + retry failed

### AC7: Internationalization
- [ ] All strings in English (values/strings.xml)
- [ ] All strings in Chinese (values-zh/strings.xml)
- [ ] Language dropdown shows localized names

### AC8: Database
- [ ] Migration 38→39 runs successfully
- [ ] Translations table created with correct schema
- [ ] Indexes created for performance
- [ ] Foreign key constraint works (cascade delete)

### AC9: Performance
- [ ] Translation completes within 3 seconds (10 paragraphs)
- [ ] UI remains responsive during translation
- [ ] Progress updates every 1-2 paragraphs
- [ ] Cached translations load instantly

### AC10: Code Quality
- [ ] All code follows project style guide
- [ ] No hardcoded strings (use string resources)
- [ ] No TODO/FIXME comments
- [ ] Unit tests for core logic
- [ ] Integration tests for database
- [ ] Build passes without warnings

---

## 8. Dependencies

### External Libraries
- `androidx.compose.runtime:runtime` (existing)
- `androidx.room:room-runtime` (existing)
- `androidx.room:room-ktx` (existing)
- `com.google.dagger:hilt-android` (existing)
- `com.openai:openai-java` (existing)
- `com.anthropic:anthropic-java` (existing)

### Internal Modules
- `com.nononsenseapps.feeder.ai` - AI integration
- `com.nononsenseapps.feeder.db.room` - Database
- `com.nononsenseapps.feeder.archmodel` - Settings, Repository
- `com.nononsenseapps.feeder.ui.compose` - UI components

---

## 9. Risks & Mitigations

### Risk 1: High API Costs
**Mitigation:** Aggressive caching, batch processing, user-configurable limits

### Risk 2: Slow Translation
**Mitigation:** Streaming progress updates, paragraph-by-paragraph display

### Risk 3: Translation Quality
**Mitigation:** Use best prompts from research, allow user to retry

### Risk 4: Database Migration Issues
**Mitigation:** Test migration thoroughly, provide rollback plan

---

## 10. Success Metrics

- **User Satisfaction:** ≥ 90% positive feedback
- **Translation Accuracy:** ≥ 85% user-rated quality
- **Performance:** ≤ 3 seconds for 10 paragraphs
- **Reliability:** ≥ 95% successful translations
- **Cache Hit Rate:** ≥ 70% (most translations from cache)

---

## 11. Post-Launch Considerations

### Future Enhancements
1. Support for more languages
2. Translation quality settings (formal vs casual)
3. Translation history dashboard
4. Batch translate multiple articles
5. Export translated articles

### Monitoring
- Track translation usage statistics
- Monitor API costs per user
- Measure cache hit rate
- Collect error rates by provider

---

**Specification Complete**
**Ready for:** Phase 6 (Implementation Plan & Task List)
