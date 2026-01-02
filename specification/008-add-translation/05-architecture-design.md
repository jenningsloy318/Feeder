# Architecture Design: AI Translation Feature

**Date:** 2026-01-02
**Feature:** Add AI Translation to Feeder RSS Reader
**Platform:** Android (Kotlin)
**Architecture Pattern:** MVVM + Clean Architecture

---

## 1. System Architecture Overview

### 1.1 High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ ArticleScreen│  │Translation    │  │  Settings    │         │
│  │              │  │SettingsScreen│  │  Screen      │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                  │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐         │
│  │ Article      │  │Translation   │  │  Settings    │         │
│  │ ViewModel    │  │Settings      │  │  ViewModel   │         │
│  │              │◄─┤ViewModel     │  │              │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼──────────────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼──────────────────┐
│         │           Domain Layer (Business Logic)                │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────┐                  │
│  │ Translation  │  │   Settings    │  │  AI   │                  │
│  │  Manager     │──┤   Store      │  │  API  │                  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬┘                  │
└─────────┼──────────────────┼──────────────────┼──────────────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼──────────────────┐
│         │              Data Layer (Persistence & External)        │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐          │
│  │ Translation  │  │  Room DB     │  │   AI Client  │          │
│  │   DAO        │  │(AppDatabase) │  │  (Provider)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Layer Responsibilities

**Presentation Layer (UI):**
- `ArticleScreen` - Displays article with inline translations
- `TranslationSettingsScreen` - Configure translation settings
- `ArticleViewModel` - Manages article + translation state
- `TranslationSettingsViewModel` - Manages translation settings

**Domain Layer (Business Logic):**
- `TranslationManager` - High-level translation orchestration
- `AIApi` - Unified AI provider interface
- `SettingsStore` - Settings persistence (SharedPreferences)
- `Repository` - Data access abstraction

**Data Layer (Persistence & External):**
- `TranslationDao` - Database operations for translations
- `AppDatabase` - Room database
- `AIClient` - Provider-specific AI implementations

---

## 2. Core Components

### 2.1 Translation Manager (Domain Layer)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/translation/TranslationManager.kt`

**Responsibilities:**
- Orchestrate translation workflow
- Manage translation caching
- Handle batch translation (multiple paragraphs)
- Track translation progress

**Interface:**
```kotlin
class TranslationManager(
    private val aiApi: AIApi,
    private val translationDao: TranslationDao,
    private val settingsStore: SettingsStore,
    private val coroutineScope: CoroutineScope,
) {
    /**
     * Translate an article with caching.
     *
     * @param articleId The article ID
     * @param paragraphs List of paragraphs to translate
     * @param targetLanguage Target language code (e.g., "zh", "es")
     * @param onProgress Progress callback (current, total)
     * @return Flow emitting translation states
     */
    fun translateArticle(
        articleId: Long,
        paragraphs: List<String>,
        targetLanguage: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): Flow<TranslationState>

    /**
     * Get cached translations for an article.
     */
    suspend fun getCachedTranslations(
        articleId: Long,
        targetLanguage: String,
    ): List<Translation>?

    /**
     * Clear translation cache for an article.
     */
    suspend fun clearCache(articleId: Long, targetLanguage: String)
}

sealed class TranslationState {
    object Idle : TranslationState()
    data class Loading(val progress: Int, val total: Int) : TranslationState()
    data class Progress(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Success(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Error(val message: String, val retryable: Boolean) : TranslationState()
}

data class ParagraphTranslation(
    val index: Int,
    val original: String,
    val translated: String,
)
```

**Key Design Decisions:**
- **Flow-based API:** Enables reactive UI updates
- **Progress callbacks:** Real-time feedback for user
- **Caching:** Check database before API calls
- **Batch processing:** Translate multiple paragraphs in single call

### 2.2 AI Client Extensions (Data Layer)

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIClient.kt`

**Add to interface:**
```kotlin
interface AIClient {
    // Existing methods...
    suspend fun listModels(): List<String>
    suspend fun generateSummary(content: String, language: SummaryLanguage): SummaryResult

    // NEW: Translation method
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

        data class Error(
            val message: String,
            val retryable: Boolean = true,
        ) : TranslationResult
    }
}
```

**Implementation in OpenAICompatibleClient:**
```kotlin
override suspend fun translate(
    paragraph: String,
    targetLanguage: String,
): AIClient.TranslationResult {
    if (!settings.isValid) {
        return AIClient.TranslationResult.Error(
            message = "Invalid settings",
            retryable = false,
        )
    }

    return try {
        val systemPrompt = buildTranslationPrompt(targetLanguage)

        val params = ChatCompletionCreateParams.builder()
            .model(settings.modelId)
            .addSystemMessage(systemPrompt)
            .addUserMessage(paragraph)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.chat().completions().create(params).get()
        }

        val choice = response.choices().firstOrNull()
            ?: return AIClient.TranslationResult.Error(
                message = "No response from API",
                retryable = true,
            )

        val translatedText = choice.message().content().stream()
            .map { obj -> obj.toString() }
            .reduce { a, b -> "$a$b" }
            .orElse("")

        val usage = response.usage()
        AIClient.TranslationResult.Success(
            translatedText = translatedText,
            promptTokens = usage.promptTokens().toInt(),
            completionTokens = usage.completionTokens().toInt(),
            totalTokens = usage.totalTokens().toInt(),
        )
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            message = e.message ?: "Unknown error",
            retryable = true,
        )
    }
}

private fun buildTranslationPrompt(targetLanguage: String): String {
    val languageName = TargetLanguage.fromCode(targetLanguage)?.displayName ?: "the target language"
    return """
        You are a professional translator. Translate the following text into $languageName.

        Guidelines:
        - Maintain the original tone and style
        - Preserve formatting (bold, links, etc.)
        - Ensure natural, fluent phrasing
        - Do not add or remove information
        - Return only the translation without any explanations or metadata

        Text to translate:
    """.trimIndent()
}
```

**Implementation in AnthropicClient:**
```kotlin
override suspend fun translate(
    paragraph: String,
    targetLanguage: String,
): AIClient.TranslationResult {
    if (!settings.isValid) {
        return AIClient.TranslationResult.Error(
            message = "Invalid settings",
            retryable = false,
        )
    }

    return try {
        val systemPrompt = buildTranslationPrompt(targetLanguage)

        val params = MessageCreateParams.builder()
            .model(settings.modelId)
            .system(systemPrompt)
            .maxTokens(2048L)
            .addUserMessage(paragraph)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.messages().create(params).get()
        }

        val translatedText = response.content().joinToString("") { contentBlock ->
            contentBlock.text().getOrNull()?.text() ?: ""
        }

        val usage = response.usage()
        AIClient.TranslationResult.Success(
            translatedText = translatedText,
            promptTokens = usage.inputTokens().toInt(),
            completionTokens = usage.outputTokens().toInt(),
            totalTokens = (usage.inputTokens() + usage.outputTokens()).toInt(),
        )
    } catch (e: Exception) {
        AIClient.TranslationResult.Error(
            message = e.message ?: "Unknown error",
            retryable = true,
        )
    }
}
```

### 2.3 Database Layer (Data Layer)

**Entity:**
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
    val targetLanguage: String,

    @ColumnInfo(name = "original_paragraph")
    val originalParagraph: String,

    @ColumnInfo(name = "translated_paragraph")
    val translatedParagraph: String,

    @ColumnInfo(name = "paragraph_index")
    val paragraphIndex: Int,

    @ColumnInfo(name = "ai_provider")
    val aiProvider: String,

    @ColumnInfo(name = "ai_model")
    val aiModel: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Clock.System.now(),
)
```

**DAO:**
```kotlin
@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage ORDER BY paragraph_index")
    suspend fun getTranslations(articleId: Long, targetLanguage: String): List<Translation>

    @Query("SELECT * FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage ORDER BY paragraph_index LIMIT 1")
    suspend fun hasTranslations(articleId: Long, targetLanguage: String): Translation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translation: Translation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(translations: List<Translation>)

    @Query("DELETE FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage")
    suspend fun delete(articleId: Long, targetLanguage: String)

    @Query("DELETE FROM translations WHERE article_id = :articleId")
    suspend fun deleteAll(articleId: Long)
}
```

**Migration (38 → 39):**
```kotlin
// In AppDatabase.kt
migration38To39 = object : Migration(38, 39) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create translations table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS translations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                article_id INTEGER NOT NULL,
                target_language TEXT NOT NULL,
                original_paragraph TEXT NOT NULL,
                translated_paragraph TEXT NOT NULL,
                paragraph_index INTEGER NOT NULL,
                ai_provider TEXT NOT NULL,
                ai_model TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(article_id) REFERENCES feed_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        // Create indexes
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_translations_article_language ON translations (article_id, target_language)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_translations_article_id ON translations (article_id)",
        )
    }
}
```

### 2.4 Settings Layer (Data Layer)

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Add to SettingsStore:**
```kotlin
class SettingsStore(private val sp: SharedPreferences) {
    // Translation settings

    // Enable/disable auto-translation
    private val _translationEnabled = MutableStateFlow(
        sp.getBoolean(PREF_TRANSLATION_ENABLED, false)
    )
    val translationEnabled = _translationEnabled.asStateFlow()

    // Target language for translation
    private val _translationTargetLanguage = MutableStateFlow(
        sp.getString(PREF_TRANSLATION_TARGET_LANGUAGE, null)?.let { TargetLanguage.fromCode(it) }
            ?: TargetLanguage.ENGLISH
    )
    val translationTargetLanguage = _translationTargetLanguage.asStateFlow()

    fun setTranslationEnabled(value: Boolean) {
        sp.edit { putBoolean(PREF_TRANSLATION_ENABLED, value) }
        _translationEnabled.value = value
    }

    fun setTranslationTargetLanguage(value: TargetLanguage) {
        sp.edit { putString(PREF_TRANSLATION_TARGET_LANGUAGE, value.code) }
        _translationTargetLanguage.value = value
    }

    companion object {
        // Preference keys
        private const val PREF_TRANSLATION_ENABLED = "translation_enabled"
        private const val PREF_TRANSLATION_TARGET_LANGUAGE = "translation_target_language"
    }
}
```

**Enum for Target Languages:**
```kotlin
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

---

## 3. User Interface Components

### 3.1 Article Screen with Translation Button

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Top Bar Actions (MODIFIED):**
```kotlin
actions = {
    // Summarize button (existing)
    if (viewState.showSummarize) {
        PlainTooltipBox(tooltip = { Text(stringResource(R.string.summarize)) }) {
            IconButton(onClick = onSummarize) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = stringResource(R.string.summarize))
            }
        }
    }

    // Fetch Full Article button (existing)
    PlainTooltipBox(tooltip = { Text(stringResource(R.string.fetch_full_article)) }) {
        IconButton(onClick = onToggleFullText) {
            Icon(Icons.AutoMirrored.Filled.Article, contentDescription = stringResource(R.string.fetch_full_article))
        }
    }

    // TRANSLATION BUTTON (NEW - immediately after Fetch Full Article)
    if (viewState.showTranslate) {
        PlainTooltipBox(tooltip = { Text(stringResource(R.string.translate)) }) {
            IconButton(
                onClick = onTranslate,
                enabled = viewState.translationState !is TranslationState.Loading,
            ) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = stringResource(R.string.translate),
                )
            }
        }
    }

    // Menu button (existing)
    PlainTooltipBox(tooltip = { Text(stringResource(id = R.string.open_menu)) }) {
        IconButton(onClick = { onShowToolbarMenu(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.open_menu))
        }
    }
}
```

### 3.2 Inline Translation Display

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/TranslationListItem.kt`

**Component:**
```kotlin
@Composable
fun TranslatedParagraph(
    original: String,
    translated: String,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        // Original paragraph
        Text(
            text = original,
            style = typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = dimens.margin),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Translated paragraph (with subtle background)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.margin),
        ) {
            Text(
                text = translated,
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
```

**Usage in ArticleContent:**
```kotlin
// In ArticleScreen.kt, modify linearArticleContent()
when (viewState.textToDisplay) {
    TextToDisplay.CONTENT -> {
        val translationState = viewState.translationState
        val translations = (translationState as? TranslationState.Success)?.translations
            ?.associateBy { it.index }

        linearArticleContent(
            articleContent = viewState.articleContent,
            onLinkClick = { ... },
            // NEW: Pass translations for inline display
            translatedParagraphs = translations,
        )
    }
    // ... other states
}
```

### 3.3 Translation Settings Screen

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/TranslationSettingsScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: TranslationSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()

    var languageMenuExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            SensibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.translation_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalDimens.current.margin, vertical = 8.dp),
        ) {
            // Enable/Disable Switch
            SwitchSetting(
                title = stringResource(R.string.translation_enabled_title),
                checked = translationEnabled,
                onCheckedChange = { viewModel.setTranslationEnabled(it) },
                description = stringResource(R.string.translation_enabled_description),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Language Selector
            LanguageSelectorSetting(
                title = stringResource(R.string.translation_target_language),
                currentLanguage = targetLanguage,
                onLanguageSelected = { viewModel.setTargetLanguage(it) },
                enabled = translationEnabled,
                menuExpanded = languageMenuExpanded,
                onMenuExpandedChange = { languageMenuExpanded = it },
            )
        }
    }
}

@Composable
private fun LanguageSelectorSetting(
    title: String,
    currentLanguage: TargetLanguage,
    onLanguageSelected: (TargetLanguage) -> Unit,
    enabled: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    ExposedDropdownMenuBox(
        expanded = menuExpanded && enabled,
        onExpandedChange = onMenuExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLanguage.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled,
        )

        ExposedDropdownMenu(
            expanded = menuExpanded && enabled,
            onDismissRequest = { onMenuExpandedChange(false) },
        ) {
            TargetLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        selectedLanguage = language
                        onLanguageSelected(language)
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = if (selectedLanguage == language) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
```

---

## 4. Data Flow Diagrams

### 4.1 Translation Flow (User Initiates)

```
User clicks "Translate" button
        │
        ▼
ArticleViewModel.onTranslateClicked()
        │
        ├─► Check if auto-translation enabled (SettingsStore)
        ├─► Get target language (SettingsStore)
        └─► Get article paragraphs (ArticleContent)
        │
        ▼
TranslationManager.translateArticle()
        │
        ├─► Check cache (TranslationDao)
        │   ├─► Cache hit? → Return cached translations
        │   └─► Cache miss? → Continue
        │
        ├─► For each paragraph:
        │   │
        │   ├─► AIApi.translate(paragraph, targetLanguage)
        │   │   │
        │   │   ├─► Get AI settings (Repository)
        │   │   ├─► Create AIClient (OpenAI/Anthropic)
        │   │   └─► Call provider API
        │   │       │
        │   │       ├─► Success? → Return translated text
        │   │       └─► Error? → Return error with retryable flag
        │   │
        │   ├─► Save to database (TranslationDao)
        │   └─► Emit progress state
        │
        └─► Emit final state (Success or Error)
        │
        ▼
ArticleViewModel.translationState updated
        │
        ▼
UI recomposes with translated paragraphs
        │
        ▼
ArticleContent displays translations inline
```

### 4.2 Auto-Translation Flow (Article Opened)

```
User opens article with full content fetched
        │
        ▼
ArticleViewModel.loadArticle()
        │
        ├─► Check if full text is available
        ├─► Check if auto-translation enabled (SettingsStore)
        └─► Check if user wants auto-translation
        │
        ▼
If auto-translation enabled AND full text available:
        │
        ▼
ArticleViewModel.autoTranslateIfNeeded()
        │
        ├─► Check cache (TranslationDao)
        ├─► If cached translations exist → Load and display
        └─► If no cache → Trigger translation (same as above)
        │
        ▼
TranslationState.Success(updated translations)
        │
        ▼
UI displays translated paragraphs inline
```

### 4.3 Settings Flow

```
User opens Settings → AI Integration → Translation
        │
        ▼
TranslationSettingsScreen loads
        │
        ├─► Get translation enabled (SettingsStore.translationEnabled)
        └─► Get target language (SettingsStore.translationTargetLanguage)
        │
        ▼
User toggles "Enable Auto Translation"
        │
        ▼
TranslationSettingsViewModel.setTranslationEnabled(true)
        │
        ▼
SettingsStore.setTranslationEnabled(true)
        │
        ├─► Update SharedPreferences
        └─► Emit new value via StateFlow
        │
        ▼
UI recomposes with new setting
```

---

## 5. Sequence Diagrams

### 5.1 Translation Flow (Detailed)

```
User          ArticleScreen    ArticleViewModel    TranslationManager    AIApi    TranslationDao    AIClient
 │                 │                   │                    │            │                 │           │
 │─Click Translate─>│                   │                    │            │                 │           │
 │                 │─onTranslate()────>│                    │            │                 │           │
 │                 │                   │─translateArticle()─>│            │                 │           │
 │                 │                   │                    │─getCache()>│                 │           │
 │                 │                   │                    │<─translations│           │
 │                 │                   │                    │            │                 │           │
 │                 │                   │                    │─translate()│                 │           │
 │                 │                   │                    │            │                 │─translate()>│
 │                 │                   │                    │            │                 │           │
 │                 │                   │                    │            │                 │<─result   │
 │                 │                   │                    │<─Success   │                 │           │
 │                 │                   │                    │            │─insert()───────>│           │
 │                 │                   │                    │<─saved─────│                 │           │
 │                 │                   │<─TranslationState  │            │                 │           │
 │                 │<─translationState │                    │            │                 │           │
 │<─Recompose with │                   │                    │            │                 │           │
 │  translations   │                   │                    │            │                 │           │
```

### 5.2 Auto-Translation Flow

```
User          ArticleScreen    ArticleViewModel    SettingsStore    TranslationManager
 │                 │                   │                   │                  │
 │─Open article───>│                   │                   │                  │
 │                 │─loadArticle()────>│                   │                  │
 │                 │                   │─translationEnabled>│                  │
 │                 │                   │<──true─────────────│                  │
 │                 │                   │                   │                  │
 │                 │                   │─targetLanguage───>│                  │
 │                 │                   │<──"zh"─────────────│                  │
 │                 │                   │                   │                  │
 │                 │                   │─autoTranslate()───>│                  │
 │                 │                   │                   │                  │
 │                 │                   │                   │─checkCache()────│
 │                 │                   │                   │                  │
 │                 │                   │<──cached/empty─────│                  │
 │                 │<─translationState │                   │                  │
 │<─Display article│                   │                   │                  │
 │  with/without   │                   │                   │                  │
 │  translations   │                   │                   │                  │
```

---

## 6. Error Handling Strategy

### 6.1 Translation Error Handling

```kotlin
sealed class TranslationState {
    object Idle : TranslationState()
    data class Loading(val progress: Int, val total: Int) : TranslationState()
    data class Progress(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Success(val translations: List<ParagraphTranslation>) : TranslationState()
    data class Error(
        val message: String,
        val retryable: Boolean,
        val partialTranslations: List<ParagraphTranslation> = emptyList(),
    ) : TranslationState()
}
```

**Error Scenarios:**

1. **API Key Missing/Invalid**
   - Show: "AI provider not configured. Please check settings."
   - Retryable: false
   - Action: Link to settings

2. **Network Error**
   - Show: "Network error. Tap to retry."
   - Retryable: true
   - Action: Retry button

3. **API Rate Limit**
   - Show: "Rate limited. Retrying in 5 seconds..."
   - Retryable: true (with exponential backoff)
   - Action: Auto-retry with delay

4. **Partial Success (some paragraphs failed)**
   - Show: "Translation partially completed. Tap to retry failed paragraphs."
   - Retryable: true
   - Action: Retry only failed paragraphs

### 6.2 Retry Logic

```kotlin
private suspend fun translateWithRetry(
    paragraph: String,
    targetLanguage: String,
    maxRetries: Int = 3,
): AIClient.TranslationResult {
    var lastError: Exception? = null

    repeat(maxRetries) { attempt ->
        when (val result = aiApi.translate(paragraph, targetLanguage)) {
            is AIClient.TranslationResult.Success -> return result
            is AIClient.TranslationResult.Error -> {
                lastError = Exception(result.message)
                if (result.retryable && attempt < maxRetries - 1) {
                    // Exponential backoff: 2s, 5s, 10s
                    val delay = (2.0.pow(attempt) * 1000).toLong()
                    delay(delay)
                } else {
                    return result
                }
            }
        }
    }

    return AIClient.TranslationResult.Error(
        message = lastError?.message ?: "Max retries exceeded",
        retryable = false,
    )
}
```

---

## 7. Performance Optimization

### 7.1 Caching Strategy

**Cache Key:**
```kotlin
data class TranslationCacheKey(
    val articleId: Long,
    val targetLanguage: String,
)
```

**Cache Invalidation:**
- Delete when article is updated
- TTL-based expiration (optional): 30 days
- Manual "Re-translate" button

### 7.2 Batch Processing

**Batch Size:** 5-10 paragraphs per API call

**Benefits:**
- Reduces API calls (1 call vs N calls)
- Better pricing (bulk discounts)
- Faster overall translation

**Implementation:**
```kotlin
suspend fun translateBatch(
    paragraphs: List<String>,
    targetLanguage: String,
): List<AIClient.TranslationResult> {
    return paragraphs.map { paragraph ->
        aiApi.translate(paragraph, targetLanguage)
    }
}
```

### 7.3 Progress Indication

**UI Feedback:**
- Progress bar: "Translating paragraph 3/10..."
- Display translated paragraphs as they complete (streaming)
- Allow cancellation mid-translation

---

## 8. Security Considerations

### 8.1 API Key Management

**Best Practices:**
- Never hardcode API keys in client code
- Use backend proxy endpoint (optional, for production)
- Store keys in SharedPreferences (encrypted)
- Rotate keys regularly

### 8.2 Rate Limiting

**Per-User Limits:**
- Max 100 translations per day (configurable)
- Track usage in database
- Show warning when approaching limit

**Implementation:**
```kotlin
suspend fun checkRateLimit(userId: String): Boolean {
    val todayStart = Clock.System.now().minus(1, DateTimeUnit.DAY)
    val count = translationDao.countTranslationsSince(userId, todayStart)
    return count < MAX_DAILY_TRANSLATIONS
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**TranslationManager Test:**
```kotlin
class TranslationManagerTest {
    @Test
    fun `translateArticle returns cached translations`() = runTest {
        // Given
        val cachedTranslations = listOf(
            Translation(articleId = 1, targetLanguage = "zh", ...),
        )
        coEvery { translationDao.getTranslations(1, "zh") } returns cachedTranslations

        // When
        val result = translationManager.translateArticle(1, listOf("Hello"), "zh").first()

        // Then
        assertTrue(result is TranslationState.Success)
    }
}
```

### 9.2 Integration Tests

**End-to-End Translation Flow:**
```kotlin
@Test
fun `translate article from API to database`() = runTest {
    // Given
    val articleId = 1L
    val paragraphs = listOf("Hello world", "Goodbye world")

    // When
    viewModel.translateArticle(articleId, paragraphs, "zh")

    // Then
    val translations = translationDao.getTranslations(articleId, "zh")
    assertEquals(2, translations.size)
    assertEquals("你好世界", translations[0].translatedParagraph)
}
```

### 9.3 UI Tests (Compose Testing)

**Translation Button Test:**
```kotlin
@Test
fun `clicking translate button triggers translation`() {
    // Given
    composeTestRule.setContent {
        ArticleScreen(
            viewState = viewState.copy(showTranslate = true),
            onTranslate = { /* capture callback */ },
            ...
        )
    }

    // When
    composeTestRule
        .onNodeWithContentDescription("Translate")
        .performClick()

    // Then
    // Verify translation triggered
}
```

---

## 10. Architecture Decision Records (ADRs)

### ADR-001: Database vs File Storage for Translations

**Decision:** Use Room database (not files)

**Rationale:**
- Easier to query and filter
- Better performance for large articles
- Supports future features (translation history, re-translation)
- Normalized data structure
- Consistent with existing patterns

**Alternatives Considered:**
1. **File storage** (like articles)
   - Pros: Consistent with article storage
   - Cons: Harder to query, no SQL indexing

2. **Embedded JSON in article**
   - Pros: Simple, single source of truth
   - Cons: Bloats article table, harder to manage

### ADR-002: Inline vs Separate View for Translations

**Decision:** Inline paragraph-by-paragraph display

**Rationale:**
- Meets user requirement (explicitly requested)
- Better for language learning
- Easier to compare original vs translation

**Alternatives Considered:**
1. **Toggle between original/translated views**
   - Pros: Simpler UI, less visual clutter
   - Cons: Can't compare side-by-side

2. **Side-by-side view**
   - Pros: Easy comparison
   - Cons: Poor mobile experience, complex responsive design

### ADR-003: Immediate vs Batch Translation

**Decision:** Batch with streaming progress updates

**Rationale:**
- Better UX (see progress in real-time)
- Fewer API calls (cost-effective)
- Faster overall translation

**Implementation:**
```kotlin
// Translate 5 paragraphs at a time
paragraphs.chunked(5).forEach { batch ->
    val results = translateBatch(batch, targetLanguage)
    emit(TranslationState.Progress(results))
}
```

---

## 11. Next Steps

Proceed to **Phase 5.5: UI/UX Design** to create:
1. Mockups for translation settings screen
2. Mockups for inline translation display
3. Interaction design for translation button
4. Error state UI designs
5. Loading state designs

---

**Architecture Design Complete**
**Components Defined:** 15
**Data Flows Documented:** 3
**Sequence Diagrams:** 2
**ADRs Recorded:** 3
