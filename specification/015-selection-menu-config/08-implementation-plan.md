# Implementation Plan: Selection Menu Configuration Feature

**Feature ID**: 015
**Version**: 1.0.0
**Last Updated**: 2026-01-04
**Estimated Time**: 17-24 hours

---

## 1. Implementation Overview

This plan breaks down the implementation of the selection menu configuration feature into 5 phases, following a layered architecture approach from bottom to top.

### 1.1 Implementation Phases

1. **Phase 1: Data Layer** (4-5 hours) - Persistence and data models
2. **Phase 2: Repository Layer** (3-4 hours) - Business logic and third-party discovery
3. **Phase 3: ViewModel Layer** (3-4 hours) - State management
4. **Phase 4: UI Layer** (4-5 hours) - Composable screens and drag-and-drop
5. **Phase 5: Integration** (3-4 hours) - Toolbar integration and testing
6. **Phase 6: Documentation** (0.5-1 hour) - Code comments and inline docs

### 1.2 Build Strategy

- **Incremental builds**: Build and test after each phase
- **Feature flags**: Use feature flag to hide incomplete feature
- **Parallel testing**: Run unit tests while implementing next phase

---

## 2. Phase 1: Data Layer (4-5 hours)

### 2.1 Objectives

- Create data models for selection menu configuration
- Implement SettingsStore extension for persistence
- Add serialization support for JSON handling
- Define default configuration

### 2.2 Tasks

#### Task 1.1: Create Data Models (1.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuModels.kt` (NEW)

**Steps**:
1. Create `SelectionMenuItem` data class
2. Create `ActionType` enum
3. Create `TranslatorApp`, `CopyApp`, `ShareTarget` data classes
4. Add kotlinx.serialization annotations
5. Implement equals/hashCode/toString

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import kotlinx.serialization.Serializable

@Serializable
data class SelectionMenuItem(
    val id: String,
    val type: ActionType,
    val enabled: Boolean,
    val order: Int,
    val label: String,
    val thirdPartyPackageName: String? = null,
    val thirdPartyClassName: String? = null
) {
    // No auto-generated equals/hashCode - data class handles it
}

enum class ActionType {
    TRANSLATE,
    COPY,
    SHARE,
    OPEN_BROWSER,
    CUSTOM
}
```

**Acceptance Criteria**:
- [ ] Data models compile successfully
- [ ] Serialization annotations present
- [ ] Default values for nullable fields

---

#### Task 1.2: Implement JSON Serializer (1 hour)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuItemSerializer.kt` (NEW)

**Steps**:
1. Create `List<SelectionMenuItem>` serializer
2. Handle nullable third-party fields
3. Add error handling for malformed JSON

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

object SelectionMenuItemSerializer {
    private val json = Json {
        ignoreKeys = true
        coerceInputValues = true
    }

    fun serialize(items: List<SelectionMenuItem>): String {
        return json.encodeToString(ListSerializer(SelectionMenuItem.serializer()), items)
    }

    fun deserialize(data: String): List<SelectionMenuItem> {
        return json.decodeFromString(ListSerializer(SelectionMenuItem.serializer()), data)
    }
}
```

**Acceptance Criteria**:
- [ ] Can serialize list of items
- [ ] Can deserialize valid JSON
- [ ] Throws exception for malformed JSON

---

#### Task 1.3: Create SettingsStore Extension (1.5 hours)

**File**: `com/nononsenseapps/feeder/util/SelectionMenuConfigStore.kt` (NEW)

**Steps**:
1. Create extension property on Context
2. Define default configuration
3. Implement SettingsStore with custom serializer
4. Add migration path from hardcoded order

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.util

import android.content.Context
import com.nononsenseapps.feeder.ui.text.SelectionMenuItem
import com.nononsenseapps.feeder.ui.text.ActionType
import com.nononsenseapps.feeder.ui.text.SelectionMenuItemSerializer

val Context.selectionMenuConfigStore: SettingsStore<List<SelectionMenuItem>>
    get() = SettingsStore(
        preferences = SharedPreferences(),
        key = "selection_menu_config",
        default = defaultSelectionMenuConfig,
        serializer = object : SettingsStore.Serializer<List<SelectionMenuItem>> {
            override fun deserialize(data: String): List<SelectionMenuItem> {
                return SelectionMenuItemSerializer.deserialize(data)
            }

            override fun serialize(value: List<SelectionMenuItem>): String {
                return SelectionMenuItemSerializer.serialize(value)
            }
        }
    )

private val defaultSelectionMenuConfig = listOf(
    SelectionMenuItem(
        id = "translate",
        type = ActionType.TRANSLATE,
        enabled = true,
        order = 0,
        label = "Translate"
    ),
    SelectionMenuItem(
        id = "copy",
        type = ActionType.COPY,
        enabled = true,
        order = 1,
        label = "Copy"
    ),
    SelectionMenuItem(
        id = "share",
        type = ActionType.SHARE,
        enabled = true,
        order = 2,
        label = "Share"
    ),
    SelectionMenuItem(
        id = "open_browser",
        type = ActionType.OPEN_BROWSER,
        enabled = true,
        order = 3,
        label = "Open in Browser"
    )
)
```

**Acceptance Criteria**:
- [ ] Extension property compiles
- [ ] Default configuration has 4 items
- [ ] Items are in correct order (0-3)
- [ ] All items enabled by default

---

#### Task 1.4: Add Gradle Dependencies (0.5 hours)

**File**: `build.gradle.kts` (app module)

**Steps**:
1. Add kotlinx-serialization dependency
2. Apply serialization plugin

**Implementation**:
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

**Acceptance Criteria**:
- [ ] Gradle sync succeeds
- [ ] Serialization plugin applied
- [ ] Dependencies resolved

---

### 2.3 Testing

**Unit Tests**:
- Test serialization/deserialization
- Test default configuration
- Test SettingsStore read/write

**Build Verification**:
```bash
./gradlew assembleDebug
```

---

## 3. Phase 2: Repository Layer (3-4 hours)

### 3.1 Objectives

- Create repository interface
- Implement repository with SettingsStore
- Implement third-party app discovery
- Add merge strategies

### 3.2 Tasks

#### Task 2.1: Create Repository Interface (0.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuConfigRepository.kt` (NEW)

**Steps**:
1. Define `SelectionMenuConfigRepository` interface
2. Define observe, update, reset methods
3. Add Flow return types

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import kotlinx.coroutines.flow.Flow

interface SelectionMenuConfigRepository {
    fun observeConfig(): Flow<List<SelectionMenuItem>>
    suspend fun updateOrder(items: List<SelectionMenuItem>)
    suspend fun updateEnabled(itemId: String, enabled: Boolean)
    suspend fun resetToDefaults()
}
```

**Acceptance Criteria**:
- [ ] Interface compiles
- [ ] Methods have correct signatures
- [ ] Flow used for reactive updates

---

#### Task 2.2: Implement Third-Party App Discovery (1.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/ThirdPartyAppRepository.kt` (NEW)

**Steps**:
1. Create `ThirdPartyAppRepository` interface
2. Implement discovery for translate apps
3. Implement discovery for copy apps
4. Implement discovery for share targets
5. Add caching

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ThirdPartyAppRepository {
    suspend fun discoverTranslators(): List<TranslatorApp>
    suspend fun discoverCopyApps(): List<CopyApp>
    suspend fun discoverShareTargets(): List<ShareTarget>
}

class ThirdPartyAppRepositoryImpl(
    private val context: Context
) : ThirdPartyAppRepository {

    private val cache = mutableMapOf<String, List<Any>>()
    private val cacheDuration = 5 * 60 * 1000L // 5 minutes

    override suspend fun discoverTranslators(): List<TranslatorApp> = withContext(Dispatchers.IO) {
        val cached = cache["translators"] as? List<TranslatorApp>
        if (cached != null) return@withContext cached

        val apps = mutableListOf<TranslatorApp>()
        val intent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT)
        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        for (info in resolveInfos) {
            apps.add(
                TranslatorApp(
                    packageName = info.activityInfo.packageName,
                    className = info.activityInfo.name,
                    appName = info.loadLabel(pm).toString(),
                    icon = null // Load icon lazily
                )
            )
        }

        cache["translators"] = apps
        apps
    }

    // Similar implementations for discoverCopyApps and discoverShareTargets
}
```

**Acceptance Criteria**:
- [ ] Can discover translator apps
- [ ] Can discover copy apps
- [ ] Can discover share targets
- [ ] Results cached for 5 minutes
- [ ] Handles PackageManager exceptions

---

#### Task 2.3: Implement Repository (1.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuConfigRepositoryImpl.kt` (NEW)

**Steps**:
1. Implement repository interface
2. Inject SettingsStore and ThirdPartyAppRepository
3. Implement observeConfig with merge logic
4. Implement updateOrder, updateEnabled, resetToDefaults

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.nononsenseapps.feeder.util.selectionMenuConfigStore

class SelectionMenuConfigRepositoryImpl(
    private val context: Context,
    private val thirdPartyRepo: ThirdPartyAppRepository
) : SelectionMenuConfigRepository {

    override fun observeConfig(): Flow<List<SelectionMenuItem>> {
        // Merge stored config with discovered third-party apps
        return combine(
            context.selectionMenuConfigStore.data,
            flowOf(thirdPartyRepo.discoverTranslators())
        ) { stored, translators ->
            mergeThirdPartyApps(stored, translators)
        }
    }

    private fun mergeThirdPartyApps(
        stored: List<SelectionMenuItem>,
        translators: List<TranslatorApp>
    ): List<SelectionMenuItem> {
        val result = stored.toMutableList()

        // Add new translators
        translators.forEach { app ->
            if (result.none { it.thirdPartyPackageName == app.packageName }) {
                result.add(
                    SelectionMenuItem(
                        id = "translator_${app.packageName}",
                        type = ActionType.TRANSLATE,
                        enabled = true,
                        order = result.size,
                        label = app.appName,
                        thirdPartyPackageName = app.packageName,
                        thirdPartyClassName = app.className
                    )
                )
            }
        }

        return result.sortedBy { it.order }
    }

    override suspend fun updateOrder(items: List<SelectionMenuItem>) {
        context.selectionMenuConfigStore.setValue(items)
    }

    override suspend fun updateEnabled(itemId: String, enabled: Boolean) {
        val current = context.selectionMenuConfigStore.getValue()
        val updated = current.map { item ->
            if (item.id == itemId) item.copy(enabled = enabled)
            else item
        }
        context.selectionMenuConfigStore.setValue(updated)
    }

    override suspend fun resetToDefaults() {
        context.selectionMenuConfigStore.resetValue()
    }
}
```

**Acceptance Criteria**:
- [ ] observeConfig emits merged config
- [ ] updateOrder persists changes
- [ ] updateEnabled toggles items
- [ ] resetToDefaults restores defaults
- [ ] Third-party apps merged correctly

---

### 3.3 Testing

**Unit Tests**:
- Test repository observeConfig
- Test updateOrder, updateEnabled, resetToDefaults
- Test third-party app discovery
- Test merge strategies

**Build Verification**:
```bash
./gradlew assembleDebug
```

---

## 4. Phase 3: ViewModel Layer (3-4 hours)

### 4.1 Objectives

- Create ViewModel for settings screen
- Implement state management
- Handle drag-and-drop logic
- Connect to repository

### 4.2 Tasks

#### Task 3.1: Create ViewModel (2 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsViewModel.kt` (NEW)

**Steps**:
1. Create ViewModel class
2. Define UiState data class
3. Define DraggedItem data class
4. Implement state management with StateFlow
5. Implement drag-and-drop logic
6. Connect to repository

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SelectionMenuSettingsUiState(
    val menuItems: List<SelectionMenuItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val draggedItem: DraggedItem? = null
)

data class DraggedItem(
    val index: Int,
    val item: SelectionMenuItem
)

class SelectionMenuSettingsViewModel(
    private val repository: SelectionMenuConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectionMenuSettingsUiState())
    val uiState: StateFlow<SelectionMenuSettingsUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.observeConfig().collect { config ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            menuItems = config,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun onDragStart(index: Int) {
        val item = _uiState.value.menuItems.getOrNull(index) ?: return
        _uiState.update {
            it.copy(draggedItem = DraggedItem(index, item))
        }
    }

    fun onDragMove(newIndex: Int) {
        val dragged = _uiState.value.draggedItem ?: return
        if (dragged.index == newIndex) return

        val items = _uiState.value.menuItems.toMutableList()
        val item = items.removeAt(dragged.index)
        items.add(newIndex, item)

        // Update order values
        items.forEachIndexed { index, menuItem ->
            items[index] = menuItem.copy(order = index)
        }

        _uiState.update {
            it.copy(
                menuItems = items,
                draggedItem = dragged.copy(index = newIndex)
            )
        }
    }

    fun onDragEnd() {
        val items = _uiState.value.menuItems
        viewModelScope.launch {
            repository.updateOrder(items)
        }

        _uiState.update {
            it.copy(draggedItem = null)
        }
    }

    fun updateEnabled(itemId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateEnabled(itemId, enabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
```

**Acceptance Criteria**:
- [ ] ViewModel compiles
- [ ] StateFlow emits UiState
- [ ] Drag-and-drop logic works
- [ ] Repository methods called correctly

---

#### Task 3.2: Create ViewModel Factory (0.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsViewModelFactory.kt` (NEW)

**Steps**:
1. Create factory class
2. Inject repository dependency
3. Implement create method

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SelectionMenuSettingsViewModelFactory(
    private val repository: SelectionMenuConfigRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectionMenuSettingsViewModel::class.java)) {
            return SelectionMenuSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

**Acceptance Criteria**:
- [ ] Factory creates ViewModel
- [ ] Repository injected correctly

---

#### Task 3.3: Add Dependency Injection Setup (0.5 hours)

**File**: `com/nononsenseapps/feeder/di/AppModule.kt` (MODIFY)

**Steps**:
1. Add provider for SelectionMenuConfigRepository
2. Add provider for ThirdPartyAppRepository
3. Update DI module

**Implementation**:
```kotlin
// In existing DI module
val selectionMenuConfigRepositoryModule = module {
    single { ThirdPartyAppRepositoryImpl(androidContext()) }
    single { SelectionMenuConfigRepositoryImpl(androidContext(), get()) }
}
```

**Acceptance Criteria**:
- [ ] DI module compiles
- [ ] Repositories can be injected
- [ ] No circular dependencies

---

### 4.3 Testing

**Unit Tests**:
- Test ViewModel state management
- Test drag-and-drop logic
- Test repository interaction
- Test error handling

**Build Verification**:
```bash
./gradlew assembleDebug
```

---

## 5. Phase 4: UI Layer (4-5 hours)

### 5.1 Objectives

- Create settings screen composable
- Implement drag-and-drop UI
- Implement list items with switches
- Add visual feedback

### 5.2 Tasks

#### Task 4.1: Create Drag-and-Drop Modifier (2 hours)

**File**: `com/nononsenseapps/feeder/ui/compose/DraggableItemModifier.kt` (NEW)

**Steps**:
1. Create custom Modifier
2. Implement long-press detection
3. Implement drag tracking
4. Add visual feedback (elevation, shadow)

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun Modifier.draggableItem(
    draggedItem: StateFlow<DraggedItem?>,
    index: Int,
    onDragStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    detectDragGestures(
        onDragStart = {
            // Check if long press
            onDragStart()
        },
        onDrag = { change, dragAmount ->
            change.consume()
            onDragMove(dragAmount)
        },
        onDragEnd = {
            onDragEnd()
        },
        onDragCancel = {
            onDragEnd()
        }
    )
}

data class DraggedItem(
    val index: Int,
    val item: SelectionMenuItem
)
```

**Acceptance Criteria**:
- [ ] Modifier compiles
- [ ] Long-press triggers drag
- [ ] Drag movement tracked
- [ ] Visual feedback applied

---

#### Task 4.2: Create List Item Row (1.5 hours)

**File**: `com/nononsenseapps/feeder/ui/compose/ListItemRow.kt` (NEW)

**Steps**:
1. Create ListItemRow composable
2. Add drag handle icon
3. Add switch for enable/disable
4. Add visual feedback for dragging state

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListItemRow(
    item: SelectionMenuItem,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = if (isDragging) 8.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .draggableItem(
                draggedItem = rememberDraggedItem(),
                index = item.order,
                onDragStart = onDragStart,
                onDragMove = { /* Handle drag move */ },
                onDragEnd = { /* Handle drag end */ }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Item label
            Text(
                text = item.label,
                modifier = Modifier.weight(1f)
            )

            // Enable/disable switch
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}
```

**Acceptance Criteria**:
- [ ] List item displays correctly
- [ ] Drag handle visible
- [ ] Switch toggles enabled state
- [ ] Elevation changes when dragging

---

#### Task 4.3: Create Settings Screen (1.5 hours)

**File**: `com/nononsenseapps/feeder/ui/text/SelectionMenuSettingsScreen.kt` (NEW)

**Steps**:
1. Create main screen composable
2. Add LazyColumn for list
3. Add section headers
4. Add reset button
5. Connect to ViewModel

**Implementation**:
```kotlin
package com.nononsenseapps.feeder.ui.text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SelectionMenuSettingsScreen(
    viewModel: SelectionMenuSettingsViewModel = viewModel(
        factory = SelectionMenuSettingsViewModelFactory(/* Inject repository */)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selection Menu Settings") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Built-in actions section
            item {
                SectionHeader("Built-in Actions")
            }

            items(uiState.menuItems.filter { it.type != ActionType.CUSTOM }) { item ->
                val isDragging = uiState.draggedItem?.index == item.order
                ListItemRow(
                    item = item,
                    isDragging = isDragging,
                    onDragStart = { viewModel.onDragStart(item.order) },
                    onEnabledChange = { enabled -> viewModel.updateEnabled(item.id, enabled) }
                )
            }

            // Third-party apps section
            item {
                SectionHeader("Third-Party Apps")
            }

            items(uiState.menuItems.filter { it.type == ActionType.CUSTOM }) { item ->
                val isDragging = uiState.draggedItem?.index == item.order
                ListItemRow(
                    item = item,
                    isDragging = isDragging,
                    onDragStart = { viewModel.onDragStart(item.order) },
                    onEnabledChange = { enabled -> viewModel.updateEnabled(item.id, enabled) }
                )
            }

            // Reset button
            item {
                Button(
                    onClick = { viewModel.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
```

**Acceptance Criteria**:
- [ ] Screen displays menu items
- [ ] Drag-and-drop works
- [ ] Switches toggle enabled state
- [ ] Reset button restores defaults
- [ ] Sections separated correctly

---

### 5.3 Testing

**UI Tests**:
- Test drag-and-drop interaction
- Test switch toggles
- Test reset button
- Test visual feedback

**Build Verification**:
```bash
./gradlew assembleDebug
```

---

## 6. Phase 5: Integration (3-4 hours)

### 6.1 Objectives

- Integrate with FeederTextToolbar
- Add navigation entry
- Connect to settings screen
- Test end-to-end

### 6.2 Tasks

#### Task 5.1: Integrate with FeederTextToolbar (2 hours)

**File**: `com/nononsenseapps/feeder/ui/text/FeederTextToolbar.kt` (MODIFY)

**Steps**:
1. Add SelectionMenuConfigRepository dependency
2. Observe configuration via Flow
3. Sort toolbar buttons based on order
4. Filter out disabled actions
5. Recompose when config changes

**Implementation**:
```kotlin
// In FeederTextToolbar.kt
@Composable
fun FeederTextToolbar(
    // ... existing parameters
    selectionMenuConfigRepository: SelectionMenuConfigRepository
) {
    val config by selectionMenuConfigRepository.observeConfig()
        .collectAsStateWithLifecycle(initial = emptyList())

    val enabledActions = config
        .filter { it.enabled }
        .sortedBy { it.order }

    // Compose toolbar buttons based on enabledActions
    Row {
        enabledActions.forEach { action ->
            when (action.type) {
                ActionType.TRANSLATE -> TranslateButton()
                ActionType.COPY -> CopyButton()
                ActionType.SHARE -> ShareButton()
                ActionType.OPEN_BROWSER -> OpenBrowserButton()
                ActionType.CUSTOM -> CustomActionButton(action)
            }
        }
    }
}
```

**Acceptance Criteria**:
- [ ] Toolbar observes config
- [ ] Buttons sorted by order
- [ ] Disabled actions filtered out
- [ ] Toolbar updates when config changes

---

#### Task 5.2: Add Navigation Entry (0.5 hours)

**File**: `com/nononsenseapps/feeder/ui/Navigation.kt` (MODIFY)

**Steps**:
1. Add SelectionMenuSettings route
2. Add menu item in settings screen

**Implementation**:
```kotlin
// In Navigation.kt
sealed class Screen(val route: String) {
    // ... existing screens
    object SelectionMenuSettings : Screen("selection_menu_settings")
}

// In SettingsScreen.kt
@Composable
fun SettingsScreen() {
    // ... existing settings items
    SettingsItem(
        title = "Selection Menu",
        onClick = { navTo(Screen.SelectionMenuSettings.route) }
    )
}
```

**Acceptance Criteria**:
- [ ] Route added to navigation
- [ ] Menu item visible in settings
- [ ] Navigation works

---

#### Task 5.3: End-to-End Testing (1.5 hours)

**Steps**:
1. Build and install app
2. Navigate to settings
3. Reorder items
4. Disable/enable items
5. Verify toolbar updates
6. Reset to defaults
7. Verify toolbar restored
8. Install third-party app
9. Verify it appears in settings
10. Verify it appears in toolbar

**Test Cases**:
- Drag item to new position
- Disable action, verify it disappears from toolbar
- Enable action, verify it appears in toolbar
- Reset to defaults, verify order restored
- Install third-party app, verify it appears
- Uninstall third-party app, verify it's removed

**Acceptance Criteria**:
- [ ] All test cases pass
- [ ] No crashes or exceptions
- [ ] UI responsive (60fps during drag)
- [ ] Configuration persists across app restarts

---

### 6.3 Build and Release

**Build Verification**:
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

---

## 7. Phase 6: Documentation (0.5-1 hour)

### 7.1 Objectives

- Add code comments
- Update inline documentation
- Document public APIs

### 7.2 Tasks

#### Task 6.1: Add Code Comments (0.5 hours)

**Files**:
- All new Kotlin files

**Steps**:
1. Add KDoc comments to public classes
2. Add KDoc comments to public methods
3. Add inline comments for complex logic
4. Document parameters and return types

**Example**:
```kotlin
/**
 * Repository for managing selection menu configuration.
 *
 * This repository provides methods to observe, update, and reset
 * the configuration of actions in the text selection toolbar.
 *
 * @property context Application context
 * @property thirdPartyRepo Repository for discovering third-party apps
 */
class SelectionMenuConfigRepositoryImpl(
    private val context: Context,
    private val thirdPartyRepo: ThirdPartyAppRepository
) : SelectionMenuConfigRepository {
    // ...
}
```

**Acceptance Criteria**:
- [ ] All public classes have KDoc
- [ ] All public methods have KDoc
- [ ] Complex logic has inline comments

---

#### Task 6.2: Update README (0.5 hours)

**File**: `README.md` (MODIFY)

**Steps**:
1. Add feature description
2. Add screenshot (if available)
3. Document configuration format
4. Document third-party integration

**Implementation**:
```markdown
## Selection Menu Configuration

Users can customize the order and visibility of actions in the text selection toolbar.

### Features

- Drag-and-drop reordering
- Enable/disable actions
- Third-party app integration
- Persistent configuration

### Configuration

Configuration is stored in SharedPreferences as JSON:

```json
[
  {
    "id": "translate",
    "type": "TRANSLATE",
    "enabled": true,
    "order": 0,
    "label": "Translate"
  }
]
```

### Third-Party Apps

Third-party apps are automatically discovered and added to the selection menu:
- **Translate**: Apps with ACTION_PROCESS_TEXT
- **Copy**: Apps with clipboard listeners
- **Share**: Apps with share handlers
```

**Acceptance Criteria**:
- [ ] Feature documented
- [ ] Configuration format explained
- [ ] Third-party integration explained

---

## 8. Risk Mitigation

### 8.1 Known Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Drag-and-drop performance issues | High | Use lazy recomposition, test on low-end devices |
| Third-party app discovery fails | Medium | Graceful degradation, log errors |
| SharedPreferences corruption | Low | Validation, fallback to defaults |
| Serialization version conflicts | Low | Add schema version, migration path |

### 8.2 Rollback Plan

If issues arise:
1. Revert to hardcoded toolbar order
2. Disable settings screen entry
3. Keep data models for future implementation

---

## 9. Success Criteria

### 9.1 Functional

- [ ] Users can reorder selection menu actions
- [ ] Users can enable/disable actions
- [ ] Configuration persists across app sessions
- [ ] Toolbar updates immediately when config changes
- [ ] Third-party apps discovered automatically

### 9.2 Non-Functional

- [ ] Drag-and-drop responds within 16ms (60fps)
- [ ] Configuration changes apply within 500ms
- [ ] Settings screen loads within 2 seconds
- [ ] No memory leaks
- [ ] No crashes or ANRs

### 9.3 Quality

- [ ] Unit test coverage ≥ 80%
- [ ] All tests pass
- [ ] Code review approved
- [ ] Documentation complete

---

## 10. Timeline

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| Phase 1: Data Layer | 4-5 hours | Day 1 | Day 1 |
| Phase 2: Repository Layer | 3-4 hours | Day 1 | Day 2 |
| Phase 3: ViewModel Layer | 3-4 hours | Day 2 | Day 2 |
| Phase 4: UI Layer | 4-5 hours | Day 2 | Day 3 |
| Phase 5: Integration | 3-4 hours | Day 3 | Day 3 |
| Phase 6: Documentation | 0.5-1 hour | Day 3 | Day 3 |
| **Total** | **17-24 hours** | | |

---

## 11. Dependencies

### 11.1 External Dependencies

- `kotlinx-serialization-json:1.6.0`
- `androidx.compose.material3:material3`
- `androidx.lifecycle:lifecycle-viewmodel-compose`

### 11.2 Internal Dependencies

- `SettingsStore`
- `FeederTextToolbar`
- `Navigation`
- `DI module`

---

## 12. Handoff Checklist

Before marking implementation complete:

- [ ] All phases implemented
- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] Code review approved
- [ ] Documentation complete
- [ ] Build artifacts created
- [ ] Release notes prepared

---

**END OF IMPLEMENTATION PLAN**
