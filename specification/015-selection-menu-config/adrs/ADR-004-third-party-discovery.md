# ADR-004: Third-Party App Discovery Strategy

**Status**: Accepted
**Date**: 2026-01-04
**Decision**: On-demand discovery with merge strategy
**Context**: Selection Menu Configuration Feature

## Context

Third-party text processing apps are discovered via Android's `ACTION_PROCESS_TEXT` intent. These apps need to be:
- Discovered and added to the configuration
- Merged with existing user configuration
- Handled gracefully when installed/uninstalled

## Decision

**Perform on-demand discovery when settings screen opens, with automatic merge into existing configuration.**

### Rationale

**Option 1: On-Demand Discovery with Merge (SELECTED)**
- **Pros**:
  - Always shows current third-party apps
  - Respects user's existing configuration
  - No unnecessary background work
  - Simple implementation
- **Cons**:
  - Slight delay when settings open
  - Must handle merge conflicts
- **Score**: 4.7/5

**Option 2: Background Polling**
- **Pros**:
  - Apps always up-to-date
  - No delay when settings open
- **Cons**:
  - Battery drain
  - Unnecessary work
  - Complex lifecycle management
  - Privacy concern (continuous PackageManager queries)
- **Score**: 2.0/5

**Option 3: Manual Refresh Only**
- **Pros**:
  - User has full control
  - No automatic work
- **Cons**:
  - Poor UX (apps don't appear automatically)
  - User must remember to refresh
  - Confusing if apps missing
- **Score**: 2.5/5

### Evaluation Matrix

| Criterion | Weight | On-Demand | Background | Manual |
|-----------|--------|-----------|------------|--------|
| **Technical** | | | | |
| Modularity | 0.1 | 5 | 3 | 5 |
| Scalability | 0.05 | 5 | 2 | 5 |
| Performance | 0.1 | 4 | 2 | 5 |
| Security | 0.05 | 5 | 4 | 5 |
| **Delivery** | | | | |
| Complexity | 0.2 | 4 | 2 | 3 |
| Risk | 0.1 | 4 | 3 | 4 |
| Time-to-Value | 0.15 | 5 | 3 | 3 |
| Maintainability | 0.1 | 5 | 2 | 4 |
| **Operational** | | | | |
| Observability | 0.05 | 4 | 3 | 3 |
| Reliability | 0.05 | 5 | 3 | 5 |
| Supportability | 0.05 | 5 | 2 | 4 |
| **Weighted Total** | **1.0** | **4.7** | **2.0** | **2.5** |

## Consequences

### Positive

1. **Always Current**: Third-party apps up-to-date when settings open
2. **User-Friendly**: No manual refresh required
3. **Efficient**: No unnecessary background work
4. **Privacy-Respecting**: Only queries when needed

### Negative

1. **Merge Complexity**: Must handle new/missing apps
2. **Slight Delay**: Discovery takes ~50-100ms when settings open
3. **Order Management**: New apps need default positions

### Trade-offs

- Accept merge complexity for automatic updates
- Trade background efficiency for user experience
- Accept slight delay for accurate app list

## Implementation

### Discovery Trigger

```kotlin
class SelectionMenuSettingsViewModel(di: DI) : DIAwareViewModel(di) {
    private val repository: SelectionMenuRepository by instance()

    init {
        // Discover third-party apps on init
        refreshThirdPartyApps()
    }

    fun refreshThirdPartyApps() {
        viewModelScope.launch {
            try {
                val thirdPartyApps = repository.getThirdPartyApps()
                val mergedConfig = repository.mergeThirdPartyApps(
                    currentConfig.value,
                    thirdPartyApps,
                )
                _viewState.value = _viewState.value.copy(
                    items = mergedConfig.items,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to discover third-party apps", e)
            }
        }
    }
}
```

### Third-Party App Discovery

```kotlin
suspend fun getThirdPartyApps(): List<ThirdPartyApp> {
    val intent = Intent(ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }.map { resolveInfo ->
        ThirdPartyApp(
            id = "third_party_${resolveInfo.activityInfo.packageName}",
            packageName = resolveInfo.activityInfo.packageName,
            className = resolveInfo.activityInfo.name,
            name = resolveInfo.loadLabel(packageManager).toString(),
        )
    }
}
```

### Merge Strategy

```kotlin
fun mergeThirdPartyApps(
    config: SelectionMenuConfig,
    discoveredApps: List<ThirdPartyApp>,
): SelectionMenuConfig {
    val existingItems = config.items.associateBy { it.id }
    val discoveredItems = discoveredApps.associateBy { it.id }

    // Merge logic:
    // 1. Keep existing system items unchanged
    // 2. Keep existing third-party items (preserve user settings)
    // 3. Add newly discovered third-party apps at end
    // 4. Mark uninstalled apps as unavailable

    val mergedItems = mutableListOf<SelectionMenuItem>()

    // Add existing system items
    config.items
        .filter { it.type == ItemType.SYSTEM }
        .forEach { mergedItems.add(it) }

    // Add existing third-party items (preserve order and enabled state)
    config.items
        .filter { it.type == ItemType.THIRD_PARTY }
        .forEach { existingItem ->
            val isStillInstalled = discoveredItems.containsKey(existingItem.id)
            mergedItems.add(
                existingItem.copy(
                    // Keep user settings, mark availability
                    enabled = existingItem.enabled && isStillInstalled,
                )
            )
        }

    // Add newly discovered apps
    val maxOrder = mergedItems.maxOfOrNull { it.order } ?: -1
    discoveredApps
        .filterNot { discovered -> existingItems.containsKey(discovered.id) }
        .forEachIndexed { index, newApp ->
            mergedItems.add(
                SelectionMenuItem(
                    id = newApp.id,
                    type = ItemType.THIRD_PARTY,
                    packageName = newApp.packageName,
                    className = newApp.className,
                    enabled = true, // Default to enabled
                    order = maxOrder + index + 1,
                )
            )
        })

    // Update order values
    val reorderedItems = mergedItems
        .sortedBy { it.order }
        .mapIndexed { index, item -> item.copy(order = index) }

    return config.copy(items = reorderedItems)
}
```

### Data Model

```kotlin
data class ThirdPartyApp(
    val id: String,
    val packageName: String,
    val className: String,
    val name: String,
)
```

## Handling Missing Apps

### Strategy

When a third-party app is uninstalled:

1. **Mark as Unavailable**: Set `enabled = false` in config
2. **Keep in List**: Item remains in settings (greyed out)
3. **Skip in Toolbar**: Don't show in selection menu

### Implementation

```kotlin
// In FeederTextActionModeCallback
private fun addThirdPartyMenuItem(menu: Menu?, item: SelectionMenuItem) {
    // Check if app is still installed
    val isInstalled = try {
        packageManager.getApplicationInfo(item.packageName!!, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    if (!isInstalled) {
        Log.d(TAG, "App not installed: ${item.packageName}")
        return
    }

    // Add menu item...
}

// In Settings UI
@Composable
fun SelectionMenuItemRow(
    item: SelectionMenuItem,
    isAvailable: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .alpha(if (isAvailable) 1f else 0.5f)
            .then(if (isAvailable) Modifier else Modifier.clickable {})
    ) {
        // ...
        if (!isAvailable) {
            Text("(App not installed)")
        }
    }
}
```

## Performance Considerations

### Discovery Time

- Typical: 5-10 apps, ~50ms
- Maximum: 50 apps, ~200ms

### Optimization

1. **Cache Discovery Result**: Cache for 30 seconds
2. **Lazy Loading**: Don't discover until settings opened
3. **Background Thread**: Use IO dispatcher for PackageManager queries

```kotlin
fun refreshThirdPartyApps() {
    viewModelScope.launch(Dispatchers.IO) {
        // Discovery on IO thread
        val apps = repository.getThirdPartyApps()
        withContext(Dispatchers.Main) {
            // Update state on main thread
            _viewState.value = _viewState.value.copy(
                items = mergedConfig.items,
            )
        }
    }
}
```

## Reversibility

### Rollback Plan

If merge strategy causes issues:

**Trigger**: Users report missing third-party apps

**Rollback Steps**:
1. Disable automatic merge
2. Add manual "Refresh Apps" button
3. Time estimate: 2 hours

**Cost**: Low (simple flag change)

## Alternatives Considered

See Options 1-3 in Decision section above.

## Related Decisions

- ADR-001: Configuration Persistence Strategy
- ADR-002: Drag-and-Drop Implementation Approach
- ADR-003: Real-Time Configuration Updates

## References

- Android ACTION_PROCESS_TEXT documentation
- PackageManager.queryIntentActivities() documentation
- Current FeederTextToolbar.kt implementation (lines 190-221)
