# Code Assessment: AI Translation Feature

**Date:** 2026-01-02
**Assessment Scope:** Feeder RSS Reader - Android/Kotlin application
**Goal:** Implement AI translation feature with inline paragraph display

---

## Executive Summary

The Feeder application is a **Kotlin/Android RSS reader** (NOT React/React-i18next as initially researched). Key findings:

- **AI Integration:** Well-structured with multi-provider support (OpenAI, Anthropic)
- **Database:** Room database with entities for FeedItem
- **Settings:** SharedPreferences-based with SettingsStore abstraction
- **UI:** Jetpack Compose with Material3 design
- **i18n:** Android string resources (XML-based) supporting 40+ languages
- **Platform:** Native Android (not web/React)

**Critical Adjustment Needed:** The research phase focused on React/i18next, but this is a **native Android Kotlin app**. Implementation must use Android patterns.

---

## 1. Existing AI Integration Analysis

### 1.1 AI Architecture Pattern

**Key Files:**
- `/app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` - Unified interface
- `/app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt` - Factory and high-level API
- `/app/src/main/java/com/nononsenseapps/feeder/ai/provider/AIProvider.kt` - Provider enum
- `/app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt` - OpenAI implementation
- `/app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt` - Anthropic implementation

**Architecture Pattern:**
```kotlin
// Sealed interface for unified AI operations
interface AIClient {
    suspend fun listModels(): List<String>
    suspend fun generateSummary(content: String, language: SummaryLanguage): SummaryResult

    sealed interface SummaryResult {
        data class Success(...) : SummaryResult
        data class Error(val content: String) : SummaryResult
    }
}

// Factory pattern
object AIClient {
    fun create(settings: AISettings): AIClient {
        return when (settings) {
            is AISettings.OpenAI -> OpenAICompatibleClient(settings.openaiSettings)
            is AISettings.Anthropic -> AnthropicClient(settings.anthropicSettings)
        }
    }
}

// High-level API
class AIApi(
    private val repository: Repository,
    private val appLang: String,
) {
    private val client: AIClient
        get() = AIClient.create(repository.aiSettings)

    suspend fun summarize(content: String): AIClient.SummaryResult {
        return client.generateSummary(content, language)
    }
}
```

**Providers Supported:**
- `OPENAI_COMPATIBLE` - OpenAI, Azure OpenAI, Perplexity, DeepSeek, etc.
- `ANTHROPIC` - Claude models

**Key Insight:** Translation feature should follow this pattern:
1. Add `translate()` method to `AIClient` interface
2. Implement in both provider clients
3. Add high-level method in `AIApi`

### 1.2 AI Settings Model

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ai/model/AISettings.kt`

```kotlin
sealed interface AISettings {
    val providerType: AIProvider

    data class OpenAI(
        val openaiSettings: OpenAISettings = OpenAISettings(),
    ) : AISettings {
        override val providerType: AIProvider = AIProvider.OPENAI_COMPATIBLE
    }

    data class Anthropic(
        val anthropicSettings: AnthropicSettings = AnthropicSettings(),
    ) : AISettings {
        override val providerType: AIProvider = AIProvider.ANTHROPIC
    }
}

data class OpenAISettings(
    val key: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val timeoutSeconds: Int = 30,
    val azureApiVersion: String = "",
    val azureDeploymentId: String = "",
) {
    val isValid: Boolean
        get() = modelId.isNotEmpty() && key.isNotEmpty()

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}
```

**Settings Storage:** SharedPreferences via `SettingsStore`

---

## 2. Settings Page Structure

### 2.1 Settings Navigation

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`

**Navigation Pattern:**
```kotlin
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSyncScreen: () -> Unit,
    onNavigateToTextSettingsScreen: () -> Unit,
    onNavigateToProviderListScreen: () -> Unit = {},
    onNavigateToSummarySettings: () -> Unit = {},
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    // ...
    AIProviderSection(
        state = openAIState,
        onEvent = onOpenAIEvent,
        onNavigateToProviders = onNavigateToProviderList,
        onNavigateToSummary = onNavigateToSummary,
    )
}
```

**Key Findings:**
- Settings screen uses **nested navigation** for sub-settings
- AI Integration has dedicated section with links to:
  - Provider list screen (`ProviderListScreen`)
  - Summary settings screen (`SummarySettingsScreen`)
- Pattern: Add new screen for "Translation Settings"

### 2.2 Settings Storage

**File:** `/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Pattern:**
```kotlin
class SettingsStore(private val sp: SharedPreferences) {
    // AI Provider Selection
    private val _aiProviderType = MutableStateFlow(AIProvider.fromString(...))
    val aiProviderType = _aiProviderType.asStateFlow()

    // OpenAI Settings
    private val _openAISettings = MutableStateFlow(OpenAISettings(...))
    val openAISettings = _openAISettings.asStateFlow()

    // Summary language setting (PATTERN TO FOLLOW)
    private val _summaryLanguage = MutableStateFlow(
        SummaryLanguage.fromCode(sp.getString(PREF_SUMMARY_LANGUAGE, null)),
    )
    val summaryLanguage = _summaryLanguage.asStateFlow()

    fun setSummaryLanguage(value: SummaryLanguage) {
        sp.edit { putString(PREF_SUMMARY_LANGUAGE, value.code) }
        _summaryLanguage.value = value
    }
}
```

**Key Insight:** Add translation settings following same pattern:
```kotlin
// Translation enabled setting
private val _translationEnabled = MutableStateFlow(
    sp.getBoolean(PREF_TRANSLATION_ENABLED, false)
)
val translationEnabled = _translationEnabled.asStateFlow()

// Translation target language
private val _translationTargetLanguage = MutableStateFlow(
    sp.getString(PREF_TRANSLATION_TARGET_LANGUAGE, null)?.let { TargetLanguage.fromCode(it) }
        ?: TargetLanguage.ENGLISH
)
val translationTargetLanguage = _translationTargetLanguage.asStateFlow()
```

### 2.3 Summary Settings Screen (Reference Implementation)

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt`

**UI Pattern:**
```kotlin
@Composable
fun SummarySettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SummarySettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val summaryEnabled by viewModel.summaryEnabled.collectAsStateWithLifecycle()
    val summaryLanguage by viewModel.summaryLanguage.collectAsStateWithLifecycle()

    // Enable/Disable Switch
    SwitchSetting(
        title = stringResource(R.string.summary_enabled_title),
        checked = summaryEnabled,
        onCheckedChange = { viewModel.setSummaryEnabled(it) },
        description = stringResource(R.string.summary_enabled_description),
    )

    // Language Selector
    LanguageSelectorSetting(
        title = stringResource(R.string.summary_language_title),
        currentLanguage = summaryLanguage,
        onLanguageSelected = { viewModel.setSummaryLanguage(it) },
        enabled = summaryEnabled,
    )
}
```

**Key Insight:** Translation settings should mirror this structure

---

## 3. Article Page Components

### 3.1 Article Screen Structure

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Top Bar Actions (Line 203-227):**
```kotlin
actions = {
    // Summarize button (conditional)
    if (viewState.showSummarize) {
        PlainTooltipBox(tooltip = { Text(stringResource(R.string.summarize)) }) {
            IconButton(onClick = onSummarize) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = stringResource(R.string.summarize))
            }
        }
    }

    // Fetch Full Article button
    PlainTooltipBox(tooltip = { Text(stringResource(R.string.fetch_full_article)) }) {
        IconButton(onClick = onToggleFullText) {
            Icon(Icons.AutoMirrored.Filled.Article, contentDescription = stringResource(R.string.fetch_full_article))
        }
    }

    // Menu button
    PlainTooltipBox(tooltip = { Text(stringResource(id = R.string.open_menu)) }) {
        IconButton(onClick = { onShowToolbarMenu(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.open_menu))
        }
    }
}
```

**Key Insight:** Add translation button immediately after Fetch Full Article button:
```kotlin
// Fetch Full Article button
PlainTooltipBox(tooltip = { Text(stringResource(R.string.fetch_full_article)) }) {
    IconButton(onClick = onToggleFullText) {
        Icon(Icons.AutoMirrored.Filled.Article, ...)
    }
}

// TRANSLATION BUTTON (NEW - immediately after Fetch Full Article)
if (viewState.showTranslate) {
    PlainTooltipBox(tooltip = { Text(stringResource(R.string.translate)) }) {
        IconButton(onClick = onTranslate) {
            Icon(Icons.Default.Translate, contentDescription = stringResource(R.string.translate))
        }
    }
}
```

### 3.2 Article Content Rendering

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` (Line 450-477)

**Content Display Logic:**
```kotlin
when (viewState.textToDisplay) {
    TextToDisplay.CONTENT -> {
        linearArticleContent(
            articleContent = viewState.articleContent,
            onLinkClick = { link, index -> ... },
        )
    }

    TextToDisplay.LOADING_FULLTEXT -> {
        LoadingItem()
    }

    TextToDisplay.FAILED_TO_LOAD_FULLTEXT -> {
        item {
            Text(text = stringResource(id = R.string.failed_to_fetch_full_article))
        }
    }
}
```

**Key Insight:** Article content is rendered via `linearArticleContent()` which takes `LinearArticle` data structure.

### 3.3 LinearArticle Data Structure

**File:** `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (Line 225-318)

**Content Parsing:**
```kotlin
private suspend fun parseArticleContent(
    article: Article,
    fullText: Boolean,
): LinearArticle {
    return withContext(Dispatchers.IO) {
        val htmlLinearizer = HtmlLinearizer()

        when (fullText) {
            false -> {
                // Load from blob cache
                blobInputStream(article.id, filePathProvider.articleDir).use {
                    htmlLinearizer.linearize(
                        inputStream = it,
                        baseUrl = article.feedUrl ?: "",
                    )
                }
            }
            true -> {
                // Load full article
                blobFullInputStream(article.id, filePathProvider.fullArticleDir).use {
                    htmlLinearizer.linearize(
                        inputStream = it,
                        baseUrl = article.feedUrl ?: "",
                    )
                }
            }
        }
    }
}
```

**Key Finding:** Articles are stored as HTML blobs in files, parsed into `LinearArticle` structure with elements.

**Critical Question:** What is `LinearArticle` structure?
- Need to find definition to understand paragraph rendering
- Translation will need to interleave translated paragraphs after original paragraphs

---

## 4. Database Schema Assessment

### 4.1 FeedItem Entity

**File:** `/app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItem.kt`

```kotlin
@Entity(
    tableName = FEED_ITEMS_TABLE_NAME,
    indices = [
        Index(value = [COL_GUID, COL_FEEDID], unique = true),
        Index(value = [COL_FEEDID]),
        Index(value = [COL_BLOCK_TIME]),
        Index(
            name = "idx_feed_items_cursor",
            value = [COL_PRIMARYSORTTIME, COL_PUBDATE, COL_ID],
            unique = true,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Feed::class,
            parentColumns = [COL_ID],
            childColumns = [COL_FEEDID],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FeedItem(
    @PrimaryKey(autoGenerate = True)
    @ColumnInfo(name = COL_ID)
    override var id: Long = ID_UNSET,

    @ColumnInfo(name = COL_GUID) var guid: String = "",
    @ColumnInfo(name = COL_TITLE) var title: String = "",
    @ColumnInfo(name = COL_PLAINTITLE) var plainTitle: String = "",
    @ColumnInfo(name = COL_PLAINSNIPPET) var plainSnippet: String = "",

    @ColumnInfo(name = COL_FEEDID) var feedId: Long = ID_UNSET,

    @ColumnInfo(name = COL_LINK) var link: String? = null,
    @ColumnInfo(name = COL_IMAGEURL) var thumbnailImage: ThumbnailImage? = null,

    @ColumnInfo(name = COL_PUBDATE) var pubDate: Instant? = null,
    @ColumnInfo(name = COL_PRIMARY_SORT_TIME) var primarySortTime: Instant? = null,

    @ColumnInfo(name = COL_WORD_COUNT) var wordCount: Int = 0,
    @ColumnInfo(name = COL_WORD_COUNT_FULL) var wordCountFull: Int = 0,

    @ColumnInfo(name = COL_FULLTEXT_DOWNLOADED) var fullTextDownloaded: Boolean = false,

    // ... more fields
) : FeedItemForFetching, FeedItemCursor
```

**Key Fields:**
- `id`: Primary key
- `feedId`: Foreign key to Feed
- `link`: Article URL
- `fullTextDownloaded`: Flag indicating if full article is fetched
- Articles stored as **files in filesystem**, not in database

### 4.2 Database Version

**File:** `/app/src/main/java/com/nononsenseapps/feeder/db/room/AppDatabase.kt`

```kotlin
@Database(
    entities = [
        Feed::class,
        FeedItem::class,
        BlocklistEntry::class,
        SyncRemote::class,
        ReadStatusSynced::class,
        RemoteReadMark::class,
        RemoteFeed::class,
        SyncDevice::class,
    ],
    views = [
        FeedsWithItemsForNavDrawer::class,
    ],
    version = 38,
)
```

**Key Finding:** Current version is 38. Adding translation table requires:
1. Increment version to 39
2. Add migration
3. Create `TranslationEntity` for translations table

### 4.3 Translation Table Schema (Recommended)

```kotlin
@Entity(
    tableName = "translations",
    indices = [
        Index(value = ["article_id", "target_language"]),
        Index(value = ["article_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = FeedItem::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Translation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "article_id")
    val articleId: Long,

    @ColumnInfo(name = "target_language")
    val targetLanguage: String, // ISO 639-1 code (e.g., "zh", "es")

    @ColumnInfo(name = "original_paragraph")
    val originalParagraph: String,

    @ColumnInfo(name = "translated_paragraph")
    val translatedParagraph: String,

    @ColumnInfo(name = "paragraph_index")
    val paragraphIndex: Int, // For ordering

    @ColumnInfo(name = "ai_provider")
    val aiProvider: String, // "openai" or "anthropic"

    @ColumnInfo(name = "ai_model")
    val aiModel: String, // e.g., "gpt-4o", "claude-3-5-sonnet"

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Clock.System.now(),
)
```

---

## 5. Internationalization (i18n) Setup

### 5.1 Platform Pattern: Android String Resources

**Directory Structure:**
```
app/src/main/res/
  values/
    strings.xml (English - default)
  values-zh/
    strings.xml (Chinese)
  values-es/
    strings.xml (Spanish)
  values-fr/
    strings.xml (French)
  ... (40+ languages supported)
```

**Example String Resource:**
```xml
<!-- values/strings.xml -->
<string name="fetch_full_article">Fetch full article</string>
<string name="summarize">Summarize</string>

<!-- values-zh/strings.xml -->
<string name="fetch_full_article">获取全文</string>
<string name="summarize">总结</string>
```

**Usage in Code:**
```kotlin
// In Composable
Text(text = stringResource(id = R.string.fetch_full_article))

// In ViewModel
context.getString(R.string.fetch_full_article)
```

### 5.2 Adding New Translatable Strings

**Pattern:**
1. Add string to `app/src/main/res/values/strings.xml` (English default)
2. Translate and add to `values-zh/strings.xml` (Chinese)
3. Other languages can be translated later via Weblate

**Example:**
```xml
<!-- English (values/strings.xml) -->
<string name="translation">Translation</string>
<string name="translate_article">Translate Article</string>
<string name="translation_enabled_title">Enable Auto Translation</string>
<string name="translation_target_language">Target Language</string>
<string name="translating">Translating...</string>
<string name="translation_failed">Translation failed. Please try again.</string>

<!-- Chinese (values-zh/strings.xml) -->
<string name="translation">翻译</string>
<string name="translate_article">翻译文章</string>
<string name="translation_enabled_title">启用自动翻译</string>
<string name="translation_target_language">目标语言</string>
<string name="translating">正在翻译...</string>
<string name="translation_failed">翻译失败。请重试。</string>
```

### 5.3 Supported Languages

From build.gradle.kts: The app supports 40+ languages. Translation feature should support at minimum:
- English (en)
- Chinese (zh)
- Spanish (es)
- French (fr)
- German (de)
- Japanese (ja)
- Korean (ko)

---

## 6. Summary of Similar Features to Study

### 6.1 AI Summary Feature (CLOSEST MATCH)

**Why Study:**
- Already integrated with AI providers
- Has language selection dropdown
- Has enable/disable toggle
- Shows loading state
- Displays results inline

**Key Files:**
1. `/app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt` - Interface
2. `/app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt` - API
3. `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/SummarySettingsScreen.kt` - Settings UI
4. `/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` - Settings storage
5. `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` - Article button

**Reusability:**
- ✅ Language enum pattern (`SummaryLanguage` → `TargetLanguage`)
- ✅ Settings UI pattern (Switch + Language Selector)
- ✅ AI API call pattern
- ✅ Settings storage pattern
- ⚠️ Translation needs paragraph-by-paragraph rendering (different from summary)

### 6.2 Fetch Full Article Feature

**Why Study:**
- Button in top bar (same placement needed)
- Loading state handling
- Error handling with retry
- File-based content storage

**Key Files:**
1. `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` - ViewModel logic
2. `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` - Button placement

**Reusability:**
- ✅ Button placement pattern
- ✅ Loading state pattern
- ✅ Error handling pattern
- ✅ Retry mechanism

### 6.3 Multi-Provider AI Config

**Why Study:**
- Shows how to add new AI settings
- Database migration pattern
- Settings navigation pattern

**Key Files:**
1. `/app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt` - Settings storage
2. `/app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderListScreen.kt` - Provider list
3. `/app/src/main/java/com/nononsenseapps/feeder/db/room/AppDatabase.kt` - Database migrations

**Reusability:**
- ✅ Settings storage pattern
- ✅ Navigation pattern for new settings screen
- ✅ Database migration pattern

---

## 7. Recommended Code Locations for Translation Feature

### 7.1 New Files to Create

```
app/src/main/java/com/nononsenseapps/feeder/ai/translation/
  TranslationClient.kt          # Translation interface
  TranslationManager.kt         # High-level API
  model/
    TargetLanguage.kt           # Language enum
    TranslationResult.kt        # Result sealed class

app/src/main/java/com/nononsenseapps/feeder/db/room/
  Translation.kt                # Room entity

app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/
  TranslationSettingsScreen.kt  # Settings UI

app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/
  TranslationListItem.kt        # Inline paragraph display
```

### 7.2 Files to Modify

**AI Integration:**
1. `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`
   - Add `suspend fun translate(paragraph: String, targetLanguage: String): TranslationResult`

2. `app/src/main/java/com/nononsenseapps/feeder/ai/provider/OpenAICompatibleClient.kt`
   - Implement `translate()` method

3. `app/src/main/java/com/nononsenseapps/feeder/ai/provider/AnthropicClient.kt`
   - Implement `translate()` method

**Settings:**
4. `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`
   - Add translation settings StateFlows
   - Add setter methods

5. `app/src/main/java/com/nononsenseapps/feeder/archmodel/Repository.kt`
   - Expose translation settings

6. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/Settings.kt`
   - Add navigation to TranslationSettingsScreen

**Article Page:**
7. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
   - Add translate button to top bar
   - Add onTranslate handler

8. `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
   - Add translation logic
   - Add translation StateFlow

**Database:**
9. `app/src/main/java/com/nononsenseapps/feeder/db/room/AppDatabase.kt`
   - Add Translation entity to entities list
   - Increment version to 39
   - Add migration 38→39

10. `app/src/main/java/com/nononsenseapps/feeder/db/room/FeedItemDao.kt`
    - Add translation query methods

**String Resources:**
11. `app/src/main/res/values/strings.xml`
    - Add English strings for translation feature

12. `app/src/main/res/values-zh/strings.xml`
    - Add Chinese strings for translation feature

---

## 8. Potential Challenges & Considerations

### 8.1 Paragraph Detection and Alignment

**Challenge:** How to split article into paragraphs for translation?

**Options:**
1. **Parse HTML** - Extract `<p>` tags during `HtmlLinearizer.linearize()`
2. **Split by double newlines** - Simple text-based approach
3. **Use existing LinearArticle structure** - If it has paragraph info

**Recommendation:** Investigate `LinearArticle` and `HtmlLinearizer` to understand paragraph structure.

### 8.2 Inline Display Rendering

**Challenge:** How to interleave translated paragraphs with original?

**Options:**
1. **Modify `LinearArticle`** - Add translation field to each element
2. **Separate translation view** - Toggle between original/translated
3. **Compose wrapper** - Wrap each element with conditional translation display

**Recommendation:** Option 3 (Compose wrapper) - least invasive, maintains separation of concerns.

### 8.3 Translation State Management

**Challenge:** How to track translation progress for multiple paragraphs?

**Approach:**
```kotlin
sealed class TranslationState {
    object NotStarted : TranslationState()
    data class Loading(val progress: Int, val total: Int) : TranslationState()
    data class Success(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Error(val message: String, val retryable: Boolean) : TranslationState()
}

data class ParagraphTranslation(
    val index: Int,
    val original: String,
    val translated: String,
)
```

### 8.4 Cost and Performance

**Considerations:**
- **API Costs:** Translating long articles (10-20 paragraphs) can be expensive
- **Caching:** Essential to avoid re-translation
- **Batch Processing:** Translate 5-10 paragraphs per API call
- **Progress Indication:** Show user progress (3/10 paragraphs translated...)

**Recommendation:** Implement aggressive caching with database storage.

---

## 9. Development Recommendations

### 9.1 Implementation Phases

**Phase 1: Core Translation API**
- Add `translate()` to `AIClient` interface
- Implement in both providers
- Create `TranslationManager` high-level API
- Add translation prompts

**Phase 2: Database & Caching**
- Create `Translation` entity
- Add migration to create translations table
- Implement DAO methods
- Add caching logic

**Phase 3: Settings UI**
- Create `TranslationSettingsScreen`
- Add settings to `SettingsStore`
- Implement enable/disable toggle
- Add language selector dropdown

**Phase 4: Article Integration**
- Add translate button to article screen
- Implement translation logic in ViewModel
- Create inline paragraph display component
- Handle loading/error states

**Phase 5: Polish**
- Add string resources (EN + ZH)
- Implement progress indication
- Add retry mechanism
- Test with various article types

### 9.2 Testing Strategy

**Unit Tests:**
- Translation API calls (mock providers)
- Database CRUD operations
- Settings persistence
- Language enum conversions

**Integration Tests:**
- End-to-end translation flow
- Database migrations
- Settings screen navigation

**UI Tests:**
- Button placement and interaction
- Settings UI functionality
- Paragraph rendering

---

## 10. Key Takeaways

1. **Platform is Android/Kotlin**, NOT React - adjust implementation accordingly
2. **Follow AI Summary pattern** for settings, API calls, and UI
3. **Add translation button** immediately after "Fetch Full Article" button
4. **Use Room database** for translation persistence
5. **Android string resources** for i18n (XML-based, not react-i18next)
6. **Paragraph-by-paragraph inline display** requires careful rendering logic
7. **Caching is essential** for cost control and performance
8. **Database migration** needed (version 38 → 39)

---

## 11. Next Steps

Proceed to **Phase 5.3: Architecture Design** to create:
1. Component diagram showing translation feature architecture
2. Sequence diagram for translation flow
3. Database schema diagram
4. API interface definitions
5. Class hierarchy for translation components

---

**Assessment Complete**
**Total Files Analyzed:** 20+
**Code Patterns Identified:** 8
**Reusable Components:** 6
**Implementation Complexity:** Medium-High
