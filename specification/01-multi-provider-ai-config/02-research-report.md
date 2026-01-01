# Research Report: Multi-Provider AI Configuration Management

**Created:** 2026-01-01 15:52:00+08:00
**Status:** Complete
**Current Date/Time:** 2026-01-01 15:52:00+08:00

## Executive Summary

Research conducted on SharedPreferences management patterns and Jetpack Compose list management best practices for Android/Kotlin applications. Key findings support using SharedPreferences with JSON serialization for multi-provider storage, and Jetpack Compose LazyColumn with mutableStateListOf for efficient list management.

## Research Sources

1. **SharedPreferences Best Practices** (Exa Code Search)
2. **Jetpack Compose List Management** (Exa Code Search)
3. **Current Codebase Analysis** (Codebase Retrieval)

## Findings: SharedPreferences for Multi-Provider Configuration

### 1. Storage Approach Recommendation

**Recommended:** SharedPreferences with JSON serialization

**Rationale:**
- Matches current project pattern (SettingsStore already uses SharedPreferences)
- Simple and sufficient for provider configurations (typically < 10 providers)
- No need for Room database complexity
- Easy migration from current single-provider format

**Implementation Pattern:**
```kotlin
// Store provider list as JSON array
private const val KEY_PROVIDER_LIST = "ai_provider_list"

data class ProviderConfig(
    val id: String,
    val name: String,
    val providerType: AIProvider,
    val openAISettings: OpenAISettings? = null,
    val anthropicSettings: AnthropicSettings? = null,
    val isActive: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

// Save provider list
fun saveProviders(providers: List<ProviderConfig>) {
    val json = Json.encodeToString(providers)
    sp.edit().putString(KEY_PROVIDER_LIST, json).apply()
}

// Load provider list
fun loadProviders(): List<ProviderConfig> {
    val json = sp.getString(KEY_PROVIDER_LIST, null)
    return if (json != null) {
        Json.decodeFromString<List<ProviderConfig>>(json)
    } else {
        emptyList()
    }
}
```

### 2. Migration Strategy

**Pattern from research:** SharedPreferences migration with fallback

```kotlin
fun migrateFromOldSettings(): List<ProviderConfig> {
    val providers = mutableListOf<ProviderConfig>()

    // Check if migration already done
    if (sp.contains(KEY_PROVIDER_LIST)) {
        return loadProviders()
    }

    // Migrate existing OpenAI settings
    val oldOpenAIKey = sp.getString(PREF_OPENAI_KEY, "")
    if (!oldOpenAIKey.isNullOrBlank()) {
        providers.add(
            ProviderConfig(
                id = "migrated-openai-${System.currentTimeMillis()}",
                name = "OpenAI Provider (Migrated)",
                providerType = AIProvider.OPENAI_COMPATIBLE,
                openAISettings = OpenAISettings(
                    key = oldOpenAIKey,
                    modelId = sp.getString(PREF_OPENAI_MODEL_ID, ""),
                    baseUrl = sp.getString(PREF_OPENAI_URL, ""),
                    timeoutSeconds = sp.getInt(PREF_OPENAI_REQUEST_TIMEOUT_SECONDS, 30),
                    azureApiVersion = sp.getString(PREF_OPENAI_AZURE_VERSION, ""),
                    azureDeploymentId = sp.getString(PREF_OPENAI_AZURE_DEPLOYMENT_ID, "")
                ),
                isActive = sp.getString(PREF_AI_PROVIDER_TYPE, "") == AIProvider.OPENAI_COMPATIBLE.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Migrate existing Anthropic settings
    val oldAnthropicKey = sp.getString(PREF_ANTHROPIC_KEY, "")
    if (!oldAnthropicKey.isNullOrBlank()) {
        providers.add(
            ProviderConfig(
                id = "migrated-anthropic-${System.currentTimeMillis()}",
                name = "Anthropic Provider (Migrated)",
                providerType = AIProvider.ANTHROPIC,
                anthropicSettings = AnthropicSettings(
                    key = oldAnthropicKey,
                    modelId = sp.getString(PREF_ANTHROPIC_MODEL_ID, ""),
                    baseUrl = sp.getString(PREF_ANTHROPIC_URL, ""),
                    timeoutSeconds = sp.getInt(PREF_ANTHROPIC_REQUEST_TIMEOUT_SECONDS, 30)
                ),
                isActive = sp.getString(PREF_AI_PROVIDER_TYPE, "") == AIProvider.ANTHROPIC.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Save migrated providers
    if (providers.isNotEmpty()) {
        saveProviders(providers)
    }

    return providers
}
```

### 3. Thread Safety Considerations

**Key Finding:** SharedPreferences is thread-safe by default
- All operations are atomic
- `apply()` is asynchronous and safe
- `commit()` is synchronous but also safe
- No additional synchronization needed

## Findings: Jetpack Compose List Management

### 1. LazyColumn with mutableStateListOf

**Best Practice Pattern:**
```kotlin
@Composable
fun ProviderListScreen(
    viewModel: ProviderListViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = providers,
            key = { it.id }  // IMPORTANT: Use stable key for performance
        ) { provider ->
            ProviderListItem(
                provider = provider,
                onEdit = { viewModel.editProvider(provider.id) },
                onDelete = { viewModel.deleteProvider(provider.id) },
                onActivate = { viewModel.activateProvider(provider.id) }
            )
        }
    }
}
```

### 2. State Management Best Practices

**Pattern:** ViewModel with StateFlow

```kotlin
class ProviderListViewModel(
    private val settingsStore: SettingsStore
) : ViewModel() {
    private val _providers = MutableStateFlow<List<ProviderConfig>>(emptyList())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    init {
        loadProviders()
    }

    fun addProvider(provider: ProviderConfig) {
        _providers.value = _providers.value + provider
        saveProviders()
    }

    fun updateProvider(provider: ProviderConfig) {
        _providers.value = _providers.value.map {
            if (it.id == provider.id) provider else it
        }
        saveProviders()
    }

    fun deleteProvider(id: String) {
        _providers.value = _providers.value.filter { it.id != id }
        saveProviders()
    }

    fun activateProvider(id: String) {
        _providers.value = _providers.value.map {
            it.copy(isActive = it.id == id)
        }
        saveProviders()
    }

    private fun saveProviders() {
        viewModelScope.launch {
            settingsStore.saveProviders(_providers.value)
        }
    }
}
```

### 3. Performance Optimization

**Critical Findings:**

1. **Always use `key` parameter in LazyColumn items**
   ```kotlin
   items(items = providers, key = { it.id }) { provider ->
       // Item content
   }
   ```
   - Prevents unnecessary recomposition
   - Essential for list stability during updates

2. **Use immutable data classes**
   ```kotlin
   data class ProviderConfig(
       val id: String,
       val name: String,
       // ...
   )
   ```
   - Enables efficient comparison
   - Works well with Compose's recomposition

3. **Avoid inline lambdas in item content**
   ```kotlin
   // BAD
   items(providers) { provider ->
       Button(onClick = { viewModel.delete(provider.id) }) { ... }
   }

   // GOOD
   items(providers, key = { it.id }) { provider ->
       Button(onClick = { viewModel.onDeleteClick(provider.id) }) { ... }
   }
   ```

### 4. Add/Edit/Delete UI Patterns

**Recommended Pattern:**
- Use separate screen/route for add/edit
- Pass navigation callback to ViewModel
- Use result pattern for returning to list

```kotlin
@Composable
fun ProviderListScreen(
    onNavigateToEdit: (String?) -> Unit
) {
    val viewModel: ProviderListViewModel = viewModel()

    LazyColumn {
        item {
            Button(onClick = { onNavigateToEdit(null) }) {
                Text("Add Provider")
            }
        }

        items(providers, key = { it.id }) { provider ->
            ProviderListItem(
                provider = provider,
                onEdit = { onNavigateToEdit(provider.id) },
                onDelete = { viewModel.deleteProvider(provider.id) },
                onActivate = { viewModel.activateProvider(provider.id) }
            )
        }
    }
}
```

## Architecture Recommendations

### 1. Repository Pattern

**Maintain existing pattern:**
```kotlin
class Repository(
    private val settingsStore: SettingsStore
) {
    val providers: StateFlow<List<ProviderConfig>>
        get() = settingsStore.providers

    suspend fun saveProviders(providers: List<ProviderConfig>) {
        settingsStore.saveProviders(providers)
    }

    val activeProvider: StateFlow<ProviderConfig?>
        get() = settingsStore.providers.map { providers ->
            providers.firstOrNull { it.isActive }
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = null
        )
}
```

### 2. SettingsStore Extension

**Add to existing SettingsStore:**
```kotlin
class SettingsStore(di: DI) {
    // Existing code...

    // New multi-provider support
    private val _providers = MutableStateFlow(emptyList<ProviderConfig>())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    init {
        // Migrate and load providers
        _providers.value = migrateFromOldSettings()
    }

    fun saveProviders(providers: List<ProviderConfig>) {
        _providers.value = providers
        val json = Json.encodeToString(providers)
        sp.edit().putString(KEY_PROVIDER_LIST, json).apply()
    }

    // Keep existing aiSettings for backward compatibility
    val aiSettings: AISettings
        get() {
            val activeProvider = _providers.value.firstOrNull { it.isActive }
            return when (activeProvider?.providerType) {
                AIProvider.OPENAI_COMPATIBLE -> AISettings.OpenAI(activeProvider.openaiSettings ?: OpenAISettings())
                AIProvider.ANTHROPIC -> AISettings.Anthropic(activeProvider.anthropicSettings ?: AnthropicSettings())
                null -> AISettings.OpenAI() // Default
            }
        }
}
```

## Implementation Complexity Assessment

### Low Complexity Components:
- ✅ SharedPreferences storage (well-understood pattern)
- ✅ JSON serialization (kotlinx.serialization built-in)
- ✅ LazyColumn with items (standard Compose pattern)

### Medium Complexity Components:
- ⚠️ Migration logic (need careful testing)
- ⚠️ Provider type selection UI (dialog or separate screen)
- ⚠️ Validation logic (reuse existing validation from AIProviderSection)

### High Complexity Components:
- ⚠️ Navigation flow (add/edit screens)
- ⚠️ Active provider management (ensure only one active)

## Recommendations Summary

### Storage Layer
1. **Use SharedPreferences** with JSON serialization
2. **Implement migration** from old settings format
3. **Maintain backward compatibility** during migration

### UI Layer
1. **Use LazyColumn** with `key` parameter for provider list
2. **Separate screens** for add/edit operations
3. **ViewModel** with StateFlow for state management

### Architecture
1. **Extend existing SettingsStore** with provider list support
2. **Keep Repository pattern** for data access
3. **Minimal changes to AIApi** (use existing active provider logic)

### Performance
1. **Always use keys** in LazyColumn items
2. **Immutable data classes** for providers
3. **Avoid inline lambdas** in composable parameters

## Next Steps

1. ✅ Requirements clarification complete
2. ✅ Research complete
3. ⏭️ Code Assessment (Phase 5)
4. ⏭️ Architecture Design (Phase 5.3)
5. ⏭️ UI/UX Design (Phase 5.5)

## References

- Exa Code Search: Android SharedPreferences patterns
- Exa Code Search: Jetpack Compose LazyColumn best practices
- Current codebase: SettingsStore.kt, AISettings.kt, AIProvider.kt
- Current codebase: AIProviderSection.kt (Compose UI patterns)
