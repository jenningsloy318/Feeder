# Implementation Plan & Task List: AI Translation Feature

**Feature ID:** 008-add-translation
**Estimated Duration:** 3 days
**Complexity:** Medium
**Dependencies:** AI Integration (existing), Room Database (existing)

---

## Implementation Strategy

**Approach:** Incremental implementation with continuous integration

**Phases:**
1. **Foundation** - Data model, settings, database (Day 1, morning)
2. **AI Integration** - Translation API, manager (Day 1, afternoon)
3. **Settings UI** - Configuration screen (Day 2, morning)
4. **Article Integration** - Button, display, logic (Day 2, afternoon)
5. **Testing & Polish** - Unit tests, integration tests, UI tests (Day 3)

**Parallel Work Opportunities:**
- Settings UI can be developed in parallel with AI Integration
- Database migration can be done independently

**Checkpoint Strategy:**
- Commit after each task completion
- Build must pass after each commit
- Tests must pass after each phase

---

## Detailed Task List

### Foundation Phase

#### Task F1: Create TargetLanguage Enum
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/TargetLanguage.kt`

**Description:** Create enum for supported target languages

**Acceptance Criteria:**
- [ ] Enum created with 12 language entries
- [ ] Each entry has code, displayName, nativeName
- [ ] Companion object with fromCode() factory method
- [ ] All properties are immutable

**Code:**
```kotlin
package com.nononsenseapps.feeder.ai.model

enum class TargetLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    ENGLISH("en", "English", "English"),
    CHINESE("zh", "Chinese", "中文"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский"),
    ITALIAN("it", "Italian", "Italiano"),
    ARABIC("ar", "Arabic", "العربية"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ;

    companion object {
        fun fromCode(code: String?): TargetLanguage? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
```

**Estimated Time:** 30 minutes

---

#### Task F2: Add Translation Settings to SettingsStore
**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Description:** Add settings for translation enabled and target language

**Acceptance Criteria:**
- [ ] Add PREF_TRANSLATION_ENABLED and PREF_TRANSLATION_TARGET_LANGUAGE constants
- [ ] Create _translationEnabled MutableStateFlow
- [ ] Create _translationTargetLanguage MutableStateFlow
- [ ] Expose translationEnabled StateFlow
- [ ] Expose translationTargetLanguage StateFlow
- [ ] Implement setTranslationEnabled() method
- [ ] Implement setTranslationTargetLanguage() method
- [ ] Settings persist to SharedPreferences
- [ ] Default: disabled, English

**Estimated Time:** 1 hour

---

#### Task F3: Create Translation Entity
**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/Translation.kt`

**Description:** Create Room entity for translations table

**Acceptance Criteria:**
- [ ] Entity created with all required fields
- [ ] Primary key is auto-increment ID
- [ ] Foreign key to FeedItem with CASCADE delete
- [ ] Indexes created (article_id, target_language)
- [ ] All fields have proper ColumnInfo annotations

**Estimated Time:** 45 minutes

---

#### Task F4: Create TranslationDao
**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/TranslationDao.kt`

**Description:** Create DAO for translation database operations

**Acceptance Criteria:**
- [ ] @Dao interface created
- [ ] getTranslations() query with ORDER BY
- [ ] hasTranslations() query for cache check
- [ ] insert() method with REPLACE strategy
- [ ] insertAll() method for batch insert
- [ ] delete() method for specific article/language
- [ ] deleteAll() method for entire article

**Estimated Time:** 45 minutes

---

#### Task F5: Add Database Migration
**File:** `app/src/main/java/com/nononsenseapps/feeder/db/room/AppDatabase.kt`

**Description:** Create migration from version 38 to 39

**Acceptance Criteria:**
- [ ] Increment database version to 39
- [ ] Add Translation entity to entities list
- [ ] Create migration38To39 object
- [ ] migration creates translations table
- [ ] migration creates indexes
- [ ] migration adds foreign key constraint
- [ ] Test migration with sample data

**Estimated Time:** 1 hour

---

### AI Integration Phase

#### Task A1: Extend AIClient Interface
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

**Description:** Add translate() method to AIClient interface

**Acceptance Criteria:**
- [ ] Add translate() method to interface
- [ ] Create TranslationResult sealed interface
- [ ] Success case includes translatedText and token counts
- [ ] Error case includes message and retryable flag
- [ ] Method signature: suspend fun translate(paragraph: String, targetLanguage: String): TranslationResult

**Estimated Time:** 30 minutes

---

#### Task A2: Implement translate() in OpenAICompatibleClient
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`

**Description:** Implement translation using OpenAI API

**Acceptance Criteria:**
- [ ] Implement translate() method
- [ ] Check settings validity
- [ ] Build translation prompt
- [ ] Call OpenAI API
- [ ] Parse response to extract translated text
- [ ] Return Success with translated text and token usage
- [ ] Catch exceptions and return Error with retryable flag

**Prompt Template:**
```
You are a professional translator. Translate the following text into {language}.

Guidelines:
- Maintain the original tone and style
- Preserve formatting (bold, links, etc.)
- Ensure natural, fluent phrasing
- Do not add or remove information
- Return only the translation without any explanations or metadata

Text to translate:
{paragraph}
```

**Estimated Time:** 2 hours

---

#### Task A3: Implement translate() in AnthropicClient
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`

**Description:** Implement translation using Anthropic API

**Acceptance Criteria:**
- [ ] Implement translate() method
- [ ] Check settings validity
- [ ] Build translation prompt (same as OpenAI)
- [ ] Call Anthropic API
- [ ] Parse response to extract translated text
- [ ] Return Success with translated text and token usage
- [ ] Catch exceptions and return Error with retryable flag

**Estimated Time:** 2 hours

---

#### Task A4: Create TranslationManager
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/translation/TranslationManager.kt`

**Description:** Create high-level translation orchestration manager

**Acceptance Criteria:**
- [ ] Class created with AIApi, TranslationDao, SettingsStore dependencies
- [ ] translateArticle() method returns Flow<TranslationState>
- [ ] Check cache before API call
- [ ] Translate paragraphs in batches (5-10 per call)
- [ ] Emit Loading state with progress
- [ ] Save translations to database after each batch
- [ ] Emit Success state with all translations
- [ ] Handle errors and emit Error state
- [ ] Implement retry logic with exponential backoff

**Estimated Time:** 3 hours

---

#### Task A5: Create TranslationState Sealed Class
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/translation/TranslationState.kt`

**Description:** Create state classes for translation flow

**Acceptance Criteria:**
- [ ] Sealed class/interface base
- [ ] Idle object (not started)
- [ ] Loading with progress (current, total)
- [ ] Success with translations list
- [ ] Error with message and retryable flag
- [ ] Immutable data classes

**Estimated Time:** 30 minutes

---

#### Task A6: Create ParagraphTranslation Data Class
**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/translation/ParagraphTranslation.kt`

**Description:** Data class for paragraph + translation pair

**Acceptance Criteria:**
- [ ] Data class with index, original, translated fields
- [ ] Immutable (all fields val)
- [ ] Equatable by index
- [ ] Used in TranslationState

**Estimated Time:** 15 minutes

---

### Settings UI Phase

#### Task S1: Create TranslationSettingsViewModel
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsViewModel.kt`

**Description:** ViewModel for translation settings screen

**Acceptance Criteria:**
- [ ] Extend DIAwareViewModel
- [ ] Inject Repository, SettingsStore dependencies
- [ ] Expose translationEnabled StateFlow
- [ ] Expose targetLanguage StateFlow
- [ ] Implement setTranslationEnabled() method
- [ ] Implement setTargetLanguage() method
- [ ] Call SettingsStore to persist values

**Estimated Time:** 1 hour

---

#### Task S2: Create TranslationSettingsScreen Composable
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`

**Description:** Settings UI for translation configuration

**Acceptance Criteria:**
- [ ] Composable function with onNavigateUp, ViewModel parameters
- [ ] TopAppBar with back button and title
- [ ] SwitchSetting for enable/disable
- [ ] LanguageSelectorSetting dropdown
- [ ] Collect state from ViewModel
- [ ] Follow Material3 design guidelines
- [ ] Use proper padding and spacing

**Estimated Time:** 2 hours

---

#### Task S3: Create LanguageSelectorSetting Component
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`

**Description:** Reusable language selector dropdown component

**Acceptance Criteria:**
- [ ] ExposedDropdownMenuBox pattern
- [ ] Shows current selection
- [ ] Dropdown lists all TargetLanguage entries
- [ ] Each item shows displayName and nativeName
- [ ] Selected item has checkmark icon
- [ ] Calls onLanguageSelected when item clicked
- [ ] Disabled when enabled parameter is false

**Estimated Time:** 1.5 hours

---

#### Task S4: Add Navigation Link in Settings
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Description:** Add "Translation" link in AI Integration section

**Acceptance Criteria:**
- [ ] Add onNavigateToTranslation parameter to SettingsScreen
- [ ] Add clickable text/button in AIProviderSection
- [ ] Route to TranslationSettingsScreen
- [ ] Follow existing navigation pattern

**Estimated Time:** 30 minutes

---

#### Task S5: Add String Resources (English)
**File:** `app/src/main/res/values/strings.xml`

**Description:** Add English strings for translation feature

**Strings to Add:**
```xml
<!-- Translation -->
<string name="translation">Translation</string>
<string name="translate">Translate</string>
<string name="translating">Translating...</string>
<string name="translation_settings">Translation Settings</string>
<string name="translation_enabled_title">Enable Auto Translation</string>
<string name="translation_enabled_description">Automatically translate articles when opened</string>
<string name="translation_target_language">Target Language</string>
<string name="translation_failed">Translation failed. Please try again.</string>
<string name="translation_error_no_provider">AI provider not configured. Please check settings.</string>
<string name="translation_error_network">Network error. Tap to retry.</string>
<string name="translation_error_rate_limit">Rate limited. Retrying in a few seconds...</string>
<string name="translation_partial_success">Translation partially completed. Tap to retry failed paragraphs.</string>
<string name="translation_progress">Translating paragraph %1$d of %2$d...</string>
<string name="clear_translation_cache">Clear Translation Cache</string>
<string name="re_translate">Re-translate</string>
```

**Estimated Time:** 30 minutes

---

#### Task S6: Add String Resources (Chinese)
**File:** `app/src/main/res/values-zh/strings.xml`

**Description:** Add Chinese translations for all strings

**Estimated Time:** 45 minutes

---

### Article Integration Phase

#### Task R1: Add Translation State to ArticleViewModel
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Description:** Add translation state and logic to ViewModel

**Acceptance Criteria:**
- [ ] Add translationState MutableStateFlow
- [ ] Add showTranslate boolean to viewState
- [ ] Inject TranslationManager dependency
- [ ] Implement onTranslateClicked() method
- [ ] Implement autoTranslateIfNeeded() method
- [ ] Check if auto-translation enabled in settings
- [ ] Check if full content is available
- [ ] Call TranslationManager.translateArticle()
- [ ] Collect translation state Flow
- [ ] Update translationState in ViewModel

**Estimated Time:** 3 hours

---

#### Task R2: Add Translation Button to ArticleScreen
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Description:** Add translate button to top bar actions

**Acceptance Criteria:**
- [ ] Add onTranslate parameter to ArticleScreen
- [ ] Add conditional translation button (after Fetch Full Article)
- [ ] Use Icons.Default.Translate
- [ ] Show tooltip "Translate" (localized)
- [ ] Disable button when translationState is Loading
- [ ] Pass onTranslate to ViewModel

**Estimated Time:** 1 hour

---

#### Task R3: Create TranslatedParagraph Composable
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/TranslationListItem.kt`

**Description:** Component for displaying translated paragraph inline

**Acceptance Criteria:**
- [ ] Composable with original and translated text parameters
- [ ] Display original paragraph in normal style
- [ ] Display translated paragraph in secondaryContainer with alpha
- [ ] Proper padding and spacing (8.dp vertical)
- [ ] FillMaxWidth modifier
- [ ] Use Material3 typography

**Estimated Time:** 1.5 hours

---

#### Task R4: Modify ArticleContent for Inline Display
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Description:** Update ArticleContent to display translations inline

**Acceptance Criteria:**
- [ ] Add translationState parameter to ArticleContent
- [ ] Extract translations map from Success state
- [ ] Pass translations to linearArticleContent
- [ ] For each paragraph, check if translation exists
- [ ] If translation exists, render TranslatedParagraph below original
- [ ] If no translation, render only original paragraph

**Estimated Time:** 2 hours

---

#### Task R5: Implement Auto-Translation Trigger
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Description:** Auto-translate when article is opened with full content

**Acceptance Criteria:**
- [ ] Call autoTranslateIfNeeded() after full content is loaded
- [ ] Check if translationEnabled setting is true
- [ ] Check if full text is available
- [ ] Check cache first (don't re-translate if cached)
- [ ] Trigger translation if cache miss
- [ ] Update translationState

**Estimated Time:** 1.5 hours

---

#### Task R6: Implement Error Handling UI
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Description:** Show error messages and retry button

**Acceptance Criteria:**
- [ ] Handle TranslationState.Error in ArticleContent
- [ ] Show error message (localized)
- [ ] Show retry button if error is retryable
- [ ] Show link to settings if provider not configured
- [ ] Allow user to retry translation

**Estimated Time:** 1.5 hours

---

#### Task R7: Implement Progress Indication
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Description:** Show translation progress to user

**Acceptance Criteria:**
- [ ] Handle TranslationState.Loading in ArticleContent
- [ ] Show progress indicator (e.g., "Translating 3/10...")
- [ ] Use LinearProgressIndicator or similar
- [ ] Update progress as paragraphs are translated
- [ ] Hide progress when complete

**Estimated Time:** 1 hour

---

### Testing & Polish Phase

#### Task T1: Unit Tests for TranslationManager
**File:** `app/src/test/java/com/nononsenseapps/feeder/ai/translation/TranslationManagerTest.kt`

**Description:** Test translation logic with mocked dependencies

**Test Cases:**
- [ ] Returns cached translations if available
- [ ] Calls API when cache miss
- [ ] Saves translations to database
- [ ] Emits Loading states with correct progress
- [ ] Emits Success state with all translations
- [ ] Emits Error state on API failure
- [ ] Implements retry logic correctly

**Estimated Time:** 2 hours

---

#### Task T2: Integration Tests for Database
**File:** `app/src/test/java/com/nononsenseapps/feeder/db/room/TranslationDaoTest.kt`

**Description:** Test database operations

**Test Cases:**
- [ ] Insert single translation
- [ ] Insert multiple translations
- [ ] Query translations by article and language
- [ ] Check cache existence
- [ ] Delete translations
- [ ] Foreign key cascade delete works

**Estimated Time:** 1.5 hours

---

#### Task T3: UI Tests for Settings Screen
**File:** `app/src/androidTest/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreenTest.kt`

**Description:** Test settings UI interactions

**Test Cases:**
- [ ] Toggle switch updates setting
- [ ] Language dropdown shows all languages
- [ ] Language selection updates setting
- [ ] Settings persist after navigation

**Estimated Time:** 2 hours

---

#### Task T4: Performance Testing
**Description:** Test translation performance

**Test Cases:**
- [ ] Short article (10 paragraphs) translates within 3 seconds
- [ ] Long article (50 paragraphs) translates within 15 seconds
- [ ] UI remains responsive during translation
- [ ] Progress updates every 1-2 paragraphs
- [ ] Cached translations load instantly (<100ms)

**Estimated Time:** 1 hour

---

#### Task T5: Error Handling Testing
**Description:** Test error scenarios

**Test Cases:**
- [ ] Network error shows retry button
- [ ] Invalid API key shows settings link
- [ ] Rate limit shows appropriate message
- [ ] Partial success shows completed + retry
- [ ] Retry works correctly

**Estimated Time:** 1.5 hours

---

#### Task T6: Code Review & Refactoring
**Description:** Review all code and refactor as needed

**Checklist:**
- [ ] All code follows project style guide
- [ ] No hardcoded strings (use string resources)
- [ ] No TODO/FIXME comments
- [ ] All functions have proper documentation
- [ ] Error handling is comprehensive
- [ ] Performance is optimized
- [ ] Build passes without warnings
- [ ] Tests pass

**Estimated Time:** 2 hours

---

#### Task T7: Final Integration Testing
**Description:** End-to-end testing of complete feature

**Test Scenarios:**
- [ ] Manual translation of article
- [ ] Auto-translation of article
- [ ] Settings configuration
- [ ] Cache hit scenario
- [ ] Cache miss scenario
- [ ] Error recovery
- [ ] Language switching
- [ ] Article with existing translations

**Estimated Time:** 2 hours

---

## Task Summary

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| Foundation | 5 | 4 hours |
| AI Integration | 6 | 8.5 hours |
| Settings UI | 6 | 6.5 hours |
| Article Integration | 7 | 11.5 hours |
| Testing & Polish | 7 | 10 hours |

**Total:** 31 tasks, ~40.5 hours (5 days for one developer, 3 days with focus)

**Critical Path:**
Foundation → AI Integration → Article Integration → Testing

**Parallel Opportunities:**
- Settings UI can be done in parallel with AI Integration
- Testing can start as soon as first component is ready

---

## Dependencies Between Tasks

**Foundation (must complete first):**
- F1 → F2, F3, F4
- F3, F4 → F5

**AI Integration:**
- F1 → A1
- A1 → A2, A3
- A2, A3 → A4
- F4, A5, A6 → A4

**Settings UI:**
- F1, F2 → S1
- F1 → S2, S3
- S1, S2, S3 → S4
- F1 → S5, S6

**Article Integration:**
- F1, F2, A4, A5, A6 → R1
- R1 → R2, R3, R4, R5
- R2, R4 → R6, R7

**Testing:**
- A4 → T1
- F5 → T2
- S2, S3 → T3
- All implementation → T4, T5, T6, T7

---

## Risk Mitigation

**Risk 1: Database Migration Issues**
- **Mitigation:** Test migration thoroughly in Task F5
- **Fallback:** Provide rollback migration if needed

**Risk 2: AI API Changes**
- **Mitigation:** Use stable API endpoints, test with both providers
- **Fallback:** Graceful error handling, retry logic

**Risk 3: Performance Issues**
- **Mitigation:** Implement caching early, use batch processing
- **Testing:** Performance testing in Task T4

**Risk 4: UI Complexity**
- **Mitigation:** Follow existing patterns (Summary, Fetch Full Article)
- **Simplification:** Start with basic inline display, enhance later

---

## Completion Criteria

All tasks complete when:
- [ ] All 31 tasks marked complete
- [ ] All acceptance criteria met
- [ ] Build passes without errors or warnings
- [ ] All tests pass
- [ ] Manual testing successful
- [ ] Code reviewed and approved
- [ ] Documentation updated

---

**Implementation Plan Complete**
**Ready for:** Phase 7 (Specification Review)
