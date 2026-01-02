# Task List: AI Translation Feature

**Feature ID:** 008-add-translation
**Total Tasks:** 31
**Status:** Ready for Execution

---

## Foundation Phase (5 tasks)

### Task F1: Create TargetLanguage Enum
- [ ] Enum created with 12 language entries
- [ ] Each entry has code, displayName, nativeName
- [ ] Companion object with fromCode() factory method
- [ ] All properties are immutable

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High (blocks many other tasks)

---

### Task F2: Add Translation Settings to SettingsStore
- [ ] Add PREF_TRANSLATION_ENABLED and PREF_TRANSLATION_TARGET_LANGUAGE constants
- [ ] Create _translationEnabled MutableStateFlow
- [ ] Create _translationTargetLanguage MutableStateFlow
- [ ] Expose translationEnabled StateFlow
- [ ] Expose translationTargetLanguage StateFlow
- [ ] Implement setTranslationEnabled() method
- [ ] Implement setTranslationTargetLanguage() method
- [ ] Settings persist to SharedPreferences
- [ ] Default: disabled, English

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task F3: Create Translation Entity
- [ ] Entity created with all required fields
- [ ] Primary key is auto-increment ID
- [ ] Foreign key to FeedItem with CASCADE delete
- [ ] Indexes created (article_id, target_language)
- [ ] All fields have proper ColumnInfo annotations

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task F4: Create TranslationDao
- [ ] @Dao interface created
- [ ] getTranslations() query with ORDER BY
- [ ] hasTranslations() query for cache check
- [ ] insert() method with REPLACE strategy
- [ ] insertAll() method for batch insert
- [ ] delete() method for specific article/language
- [ ] deleteAll() method for entire article

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task F5: Add Database Migration
- [ ] Increment database version to 39
- [ ] Add Translation entity to entities list
- [ ] Create migration38To39 object
- [ ] migration creates translations table
- [ ] migration creates indexes
- [ ] migration adds foreign key constraint
- [ ] Test migration with sample data

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

## AI Integration Phase (6 tasks)

### Task A1: Extend AIClient Interface
- [ ] Add translate() method to interface
- [ ] Create TranslationResult sealed interface
- [ ] Success case includes translatedText and token counts
- [ ] Error case includes message and retryable flag
- [ ] Method signature: suspend fun translate(paragraph: String, targetLanguage: String): TranslationResult

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task A2: Implement translate() in OpenAICompatibleClient
- [ ] Implement translate() method
- [ ] Check settings validity
- [ ] Build translation prompt
- [ ] Call OpenAI API
- [ ] Parse response to extract translated text
- [ ] Return Success with translated text and token usage
- [ ] Catch exceptions and return Error with retryable flag

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task A3: Implement translate() in AnthropicClient
- [ ] Implement translate() method
- [ ] Check settings validity
- [ ] Build translation prompt (same as OpenAI)
- [ ] Call Anthropic API
- [ ] Parse response to extract translated text
- [ ] Return Success with translated text and token usage
- [ ] Catch exceptions and return Error with retryable flag

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task A4: Create TranslationManager
- [ ] Class created with AIApi, TranslationDao, SettingsStore dependencies
- [ ] translateArticle() method returns Flow<TranslationState>
- [ ] Check cache before API call
- [ ] Translate paragraphs in batches (5-10 per call)
- [ ] Emit Loading state with progress
- [ ] Save translations to database after each batch
- [ ] Emit Success state with all translations
- [ ] Handle errors and emit Error state
- [ ] Implement retry logic with exponential backoff

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task A5: Create TranslationState Sealed Class
- [ ] Sealed class/interface base
- [ ] Idle object (not started)
- [ ] Loading with progress (current, total)
- [ ] Success with translations list
- [ ] Error with message and retryable flag
- [ ] Immutable data classes

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task A6: Create ParagraphTranslation Data Class
- [ ] Data class with index, original, translated fields
- [ ] Immutable (all fields val)
- [ ] Equatable by index
- [ ] Used in TranslationState

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

## Settings UI Phase (6 tasks)

### Task S1: Create TranslationSettingsViewModel
- [ ] Extend DIAwareViewModel
- [ ] Inject Repository, SettingsStore dependencies
- [ ] Expose translationEnabled StateFlow
- [ ] Expose targetLanguage StateFlow
- [ ] Implement setTranslationEnabled() method
- [ ] Implement setTargetLanguage() method
- [ ] Call SettingsStore to persist values

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task S2: Create TranslationSettingsScreen Composable
- [ ] Composable function with onNavigateUp, ViewModel parameters
- [ ] TopAppBar with back button and title
- [ ] SwitchSetting for enable/disable
- [ ] LanguageSelectorSetting dropdown
- [ ] Collect state from ViewModel
- [ ] Follow Material3 design guidelines
- [ ] Use proper padding and spacing

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task S3: Create LanguageSelectorSetting Component
- [ ] ExposedDropdownMenuBox pattern
- [ ] Shows current selection
- [ ] Dropdown lists all TargetLanguage entries
- [ ] Each item shows displayName and nativeName
- [ ] Selected item has checkmark icon
- [ ] Calls onLanguageSelected when item clicked
- [ ] Disabled when enabled parameter is false

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task S4: Add Navigation Link in Settings
- [ ] Add onNavigateToTranslation parameter to SettingsScreen
- [ ] Add clickable text/button in AIProviderSection
- [ ] Route to TranslationSettingsScreen
- [ ] Follow existing navigation pattern

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Low

---

### Task S5: Add String Resources (English)
- [ ] Add all 12 English strings to values/strings.xml
- [ ] Proper XML formatting
- [ ] Descriptive string names

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task S6: Add String Resources (Chinese)
- [ ] Add all 12 Chinese strings to values-zh/strings.xml
- [ ] Proper XML formatting
- [ ] Accurate translations

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

## Article Integration Phase (7 tasks)

### Task R1: Add Translation State to ArticleViewModel
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

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task R2: Add Translation Button to ArticleScreen
- [ ] Add onTranslate parameter to ArticleScreen
- [ ] Add conditional translation button (after Fetch Full Article)
- [ ] Use Icons.Default.Translate
- [ ] Show tooltip "Translate" (localized)
- [ ] Disable button when translationState is Loading
- [ ] Pass onTranslate to ViewModel

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task R3: Create TranslatedParagraph Composable
- [ ] Composable with original and translated text parameters
- [ ] Display original paragraph in normal style
- [ ] Display translated paragraph in secondaryContainer with alpha
- [ ] Proper padding and spacing (8.dp vertical)
- [ ] FillMaxWidth modifier
- [ ] Use Material3 typography

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task R4: Modify ArticleContent for Inline Display
- [ ] Add translationState parameter to ArticleContent
- [ ] Extract translations map from Success state
- [ ] Pass translations to linearArticleContent
- [ ] For each paragraph, check if translation exists
- [ ] If translation exists, render TranslatedParagraph below original
- [ ] If no translation, render only original paragraph

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task R5: Implement Auto-Translation Trigger
- [ ] Call autoTranslateIfNeeded() after full content is loaded
- [ ] Check if translationEnabled setting is true
- [ ] Check if full text is available
- [ ] Check cache first (don't re-translate if cached)
- [ ] Trigger translation if cache miss
- [ ] Update translationState

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task R6: Implement Error Handling UI
- [ ] Handle TranslationState.Error in ArticleContent
- [ ] Show error message (localized)
- [ ] Show retry button if error is retryable
- [ ] Show link to settings if provider not configured
- [ ] Allow user to retry translation

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task R7: Implement Progress Indication
- [ ] Handle TranslationState.Loading in ArticleContent
- [ ] Show progress indicator (e.g., "Translating 3/10...")
- [ ] Use LinearProgressIndicator or similar
- [ ] Update progress as paragraphs are translated
- [ ] Hide progress when complete

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

## Testing & Polish Phase (7 tasks)

### Task T1: Unit Tests for TranslationManager
- [ ] Returns cached translations if available
- [ ] Calls API when cache miss
- [ ] Saves translations to database
- [ ] Emits Loading states with correct progress
- [ ] Emits Success state with all translations
- [ ] Emits Error state on API failure
- [ ] Implements retry logic correctly

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task T2: Integration Tests for Database
- [ ] Insert single translation
- [ ] Insert multiple translations
- [ ] Query translations by article and language
- [ ] Check cache existence
- [ ] Delete translations
- [ ] Foreign key cascade delete works

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task T3: UI Tests for Settings Screen
- [ ] Toggle switch updates setting
- [ ] Language dropdown shows all languages
- [ ] Language selection updates setting
- [ ] Settings persist after navigation

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Low

---

### Task T4: Performance Testing
- [ ] Short article (10 paragraphs) translates within 3 seconds
- [ ] Long article (50 paragraphs) translates within 15 seconds
- [ ] UI remains responsive during translation
- [ ] Progress updates every 1-2 paragraphs
- [ ] Cached translations load instantly (<100ms)

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task T5: Error Handling Testing
- [ ] Network error shows retry button
- [ ] Invalid API key shows settings link
- [ ] Rate limit shows appropriate message
- [ ] Partial success shows completed + retry
- [ ] Retry works correctly

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** Medium

---

### Task T6: Code Review & Refactoring
- [ ] All code follows project style guide
- [ ] No hardcoded strings (use string resources)
- [ ] No TODO/FIXME comments
- [ ] All functions have proper documentation
- [ ] Error handling is comprehensive
- [ ] Performance is optimized
- [ ] Build passes without warnings
- [ ] Tests pass

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

### Task T7: Final Integration Testing
- [ ] Manual translation of article
- [ ] Auto-translation of article
- [ ] Settings configuration
- [ ] Cache hit scenario
- [ ] Cache miss scenario
- [ ] Error recovery
- [ ] Language switching
- [ ] Article with existing translations

**Status:** ⬜ Not Started
**Assigned To:**
**Priority:** High

---

## Task Statistics

- **Total Tasks:** 31
- **Foundation:** 5 tasks
- **AI Integration:** 6 tasks
- **Settings UI:** 6 tasks
- **Article Integration:** 7 tasks
- **Testing & Polish:** 7 tasks

**Estimated Total Time:** ~40.5 hours (3-5 days depending on focus)

**Critical Path:** F1 → F2 → F3 → F4 → F5 → A1 → A2 → A3 → A4 → A5 → A6 → R1 → R2 → R3 → R4 → R6 → R7

**Parallel Opportunities:**
- Settings UI (S1-S6) can be done in parallel with AI Integration (A1-A6)
- Testing (T1-T7) can start as soon as components are ready

---

**Next Phase:** Phase 7 (Specification Review) → Phase 8 (Execution & QA)
