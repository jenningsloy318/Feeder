# ADR-001: Selection Menu Configuration Persistence Strategy

**Status**: Accepted
**Date**: 2026-01-04
**Decision**: Use JSON serialization in SharedPreferences
**Score**: 4.2/5

---

## Context

The Selection Menu Configuration feature requires a persistence mechanism to store user preferences for the order and enabled state of actions in the text selection toolbar. The configuration must:

1. Persist across app sessions
2. Support serialization of complex data structures
3. Be human-readable for debugging
4. Support schema evolution for future changes
5. Integrate with existing Feeder patterns

## Considered Options

### Option 1: JSON in SharedPreferences (4.2/5)

Store configuration as JSON in SharedPreferences.

**Pros**:
- Human-readable format便于调试
- Supports complex nested structures
- Flexible schema evolution
- Consistent with existing AI provider pattern in Feeder
- No additional dependencies (kotlinx.serialization already used)
- Easy to inspect with `adb`

**Cons**:
- JSON parsing overhead (minimal for small config)
- No schema validation (optional)
- Manual serialization/deserialization code

**Score**: 4.2/5

**Implementation**:
```kotlin
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
```

**JSON Format**:
```json
[
  {
    "id": "translate",
    "type": "TRANSLATE",
    "enabled": true,
    "order": 0,
    "label": "Translate",
    "thirdPartyPackageName": null,
    "thirdPartyClassName": null
  },
  {
    "id": "copy",
    "type": "COPY",
    "enabled": true,
    "order": 1,
    "label": "Copy",
    "thirdPartyPackageName": null,
    "thirdPartyClassName": null
  }
]
```

---

### Option 2: Protocol Buffers in SharedPreferences (3.8/5)

Store configuration as Protocol Buffers in SharedPreferences.

**Pros**:
- Efficient binary serialization
- Strong schema validation
- Smaller storage size
- Faster parsing than JSON

**Cons**:
- Not human-readable
- Requires additional dependency (protobuf)
- More complex schema evolution
- Inconsistent with existing Feeder patterns
- Harder to debug

**Score**: 3.8/5

**Implementation**:
```kotlin
// Would require .proto file and protobuf compilation
message SelectionMenuItem {
  string id = 1;
  ActionType type = 2;
  bool enabled = 3;
  int32 order = 4;
  string label = 5;
  string thirdPartyPackageName = 6;
  string thirdPartyClassName = 7;
}
```

---

### Option 3: SQLite Database (3.5/5)

Store configuration in SQLite database.

**Pros**:
- Efficient for large datasets
- Supports complex queries
- Transaction support
- Schema migration support

**Cons**:
- Overkill for small config (≤ 5KB)
- Additional database maintenance
- Not human-readable
- Requires Room or raw SQLite
- Inconsistent with existing Feeder patterns
- More complex than needed

**Score**: 3.5/5

**Implementation**:
```kotlin
@Entity(tableName = "selection_menu_items")
data class SelectionMenuItemEntity(
    @PrimaryKey val id: String,
    val type: ActionType,
    val enabled: Boolean,
    val order: Int,
    val label: String,
    val thirdPartyPackageName: String?,
    val thirdPartyClassName: String?
)

@Dao
interface SelectionMenuItemDao {
    @Query("SELECT * FROM selection_menu_items ORDER BY `order`")
    fun getAll(): Flow<List<SelectionMenuItemEntity>>

    @Insert
    suspend fun insertAll(items: List<SelectionMenuItemEntity>)

    @Update
    suspend fun update(item: SelectionMenuItemEntity)
}
```

---

## Decision Matrix

| Criterion | JSON | ProtoBuf | SQLite | Weight |
|-----------|------|----------|--------|--------|
| Human-Readable | 5 | 1 | 1 | 0.15 |
| Flexibility | 5 | 3 | 4 | 0.20 |
| Performance | 4 | 5 | 5 | 0.15 |
| Consistency with Feeder | 5 | 2 | 2 | 0.20 |
| Debugging Ease | 5 | 1 | 2 | 0.10 |
| Schema Evolution | 4 | 4 | 5 | 0.10 |
| Complexity | 4 | 3 | 2 | 0.10 |
| **Weighted Score** | **4.55** | **2.95** | **3.15** | - |
| **Normalized** | **4.2/5** | **3.8/5** | **3.5/5** | - |

---

## Decision

**Chosen Option**: JSON in SharedPreferences (Score: 4.2/5)

**Rationale**:
1. **Consistency**: Matches existing AI provider pattern in Feeder
2. **Debugging**: Human-readable format便于调试
3. **Flexibility**: Supports schema evolution without breaking changes
4. **Size**: Configuration is small (≤ 5KB), so performance is not critical
5. **Dependencies**: Uses existing kotlinx.serialization dependency

---

## Consequences

### Positive

- Easy to debug and inspect configuration with `adb`
- Consistent with existing Feeder patterns
- Flexible schema for future enhancements
- No additional dependencies required
- Human-readable for power users

### Negative

- Slightly slower than binary formats (negligible for small config)
- No built-in schema validation (optional to add)
- Manual serialization/deserialization code

### Mitigations

- Use kotlinx.serialization for type-safe serialization
- Add schema version field for future migrations
- Validate config on load, fallback to defaults if corrupted

---

## Implementation Details

### Data Model

```kotlin
@Serializable
data class SelectionMenuItem(
    val id: String,
    val type: ActionType,
    val enabled: Boolean,
    val order: Int,
    val label: String,
    val thirdPartyPackageName: String? = null,
    val thirdPartyClassName: String? = null
)

enum class ActionType {
    TRANSLATE,
    COPY,
    SHARE,
    OPEN_BROWSER,
    CUSTOM
}
```

### Serializer

```kotlin
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

### SettingsStore Extension

```kotlin
val Context.selectionMenuConfigStore: SettingsStore<List<SelectionMenuItem>>
    get() = SettingsStore(
        preferences = SharedPreferences(),
        key = "selection_menu_config",
        default = defaultSelectionMenuConfig,
        serializer = object : SettingsStore.Serializer<List<SelectionMenuItem>> {
            override fun deserialize(data: String): List<SelectionMenuItem> {
                return try {
                    SelectionMenuItemSerializer.deserialize(data)
                } catch (e: Exception) {
                    // Fallback to defaults if corrupted
                    defaultSelectionMenuConfig
                }
            }

            override fun serialize(value: List<SelectionMenuItem>): String {
                return SelectionMenuItemSerializer.serialize(value)
            }
        }
    )
```

### Default Configuration

```kotlin
private val defaultSelectionMenuConfig = listOf(
    SelectionMenuItem(
        id = "translate",
        type = ActionType.TRANSLATE,
        enabled = true,
        order = 0,
        label = LocalStrings.current.translate
    ),
    SelectionMenuItem(
        id = "copy",
        type = ActionType.COPY,
        enabled = true,
        order = 1,
        label = LocalStrings.current.copy
    ),
    SelectionMenuItem(
        id = "share",
        type = ActionType.SHARE,
        enabled = true,
        order = 2,
        label = LocalStrings.current.share
    ),
    SelectionMenuItem(
        id = "open_browser",
        type = ActionType.OPEN_BROWSER,
        enabled = true,
        order = 3,
        label = LocalStrings.current.openInBrowser
    )
)
```

---

## Schema Evolution

### Version 1.0 (Initial)

Fields:
- id: String
- type: ActionType
- enabled: Boolean
- order: Int
- label: String
- thirdPartyPackageName: String? (nullable)
- thirdPartyClassName: String? (nullable)

### Future Migrations

To add fields in future versions:

1. Add new field with default value
2. Use @SerialName annotation for backward compatibility
3. Update migration logic if needed

Example:
```kotlin
@Serializable
data class SelectionMenuItem(
    val id: String,
    val type: ActionType,
    val enabled: Boolean,
    val order: Int,
    val label: String,
    val thirdPartyPackageName: String? = null,
    val thirdPartyClassName: String? = null,
    // New field with default for backward compatibility
    val iconResId: Int? = null  // Version 1.1
)
```

---

## Testing

### Unit Tests

```kotlin
class SelectionMenuItemSerializerTest {
    @Test
    fun `serialize and deserialize correctly`() {
        val items = listOf(
            SelectionMenuItem(
                id = "translate",
                type = ActionType.TRANSLATE,
                enabled = true,
                order = 0,
                label = "Translate"
            )
        )

        val json = SelectionMenuItemSerializer.serialize(items)
        val deserialized = SelectionMenuItemSerializer.deserialize(json)

        assertEquals(items, deserialized)
    }

    @Test
    fun `handle missing nullable fields`() {
        val json = """
            [{"id":"translate","type":"TRANSLATE","enabled":true,"order":0,"label":"Translate"}]
        """.trimIndent()

        val items = SelectionMenuItemSerializer.deserialize(json)

        assertEquals(1, items.size)
        assertNull(items[0].thirdPartyPackageName)
        assertNull(items[0].thirdPartyClassName)
    }
}
```

### Integration Tests

```kotlin
class SelectionMenuConfigStoreTest {
    @Test
    fun `persist and retrieve configuration`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.selectionMenuConfigStore

        val config = listOf(/* test config */)
        store.setValue(config)

        val retrieved = store.getValue()
        assertEquals(config, retrieved)
    }
}
```

---

## Alternatives Considered

### DataStore (New Google Library)

Google's DataStore is a replacement for SharedPreferences, but:

- Not yet used in Feeder
- Would require migration of existing SharedPreferences
- Additional complexity for minimal benefit
- JSON serialization still needed for complex data

**Decision**: Stick with SharedPreferences for consistency.

---

## References

- [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization)
- [Android SharedPreferences best practices](https://developer.android.com/training/data-storage/shared-preferences)
- [Feeder AI provider implementation](https://github.com/spacecowboy/Feeder) (existing pattern)

---

**END OF ADR-001**
