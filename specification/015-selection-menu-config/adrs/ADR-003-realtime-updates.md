# ADR-003: Real-Time Configuration Updates

**Status**: Accepted
**Date**: 2026-01-04
**Decision**: StateFlow observation in FeederTextToolbar
**Context**: Selection Menu Configuration Feature

## Context

Configuration changes should apply immediately to the text selection toolbar without requiring app restart. The toolbar is created on-demand when text is selected.

## Decision

**Observe configuration StateFlow in FeederTextToolbar for real-time updates.**

### Rationale

**Option 1: StateFlow Observation in Toolbar (SELECTED)**
- **Pros**:
  - Immediate updates when config changes
  - Reactive architecture (consistent with app)
  - No app restart required
  - Minimal coupling (StateFlow is reactive)
  - Efficient (only updates on config change)
- **Cons**:
  - Slightly more complex than polling
  - Requires coroutine scope in toolbar
- **Score**: 4.6/5

**Option 2: Poll on Toolbar Display**
- **Pros**:
  - Simpler implementation
  - No coroutine scope needed
  - Always shows latest config
- **Cons**:
  - Not truly real-time (updates on next text selection)
  - Wasteful (reads config even if unchanged)
  - Inconsistent with reactive architecture
- **Score**: 3.2/5

**Option 3: Broadcast Intent on Config Change**
- **Pros**:
  - Decoupled (no direct reference)
  - Works across processes (if needed)
- **Cons**:
  - Overkill for in-process communication
  - More complex than StateFlow
  - Not aligned with reactive architecture
- **Score**: 2.5/5

### Evaluation Matrix

| Criterion | Weight | StateFlow | Polling | Broadcast |
|-----------|--------|-----------|---------|-----------|
| **Technical** | | | | |
| Modularity | 0.1 | 5 | 4 | 5 |
| Scalability | 0.05 | 5 | 3 | 4 |
| Performance | 0.1 | 5 | 3 | 4 |
| Security | 0.05 | 5 | 5 | 4 |
| **Delivery** | | | | |
| Complexity | 0.2 | 3 | 5 | 2 |
| Risk | 0.1 | 4 | 4 | 3 |
| Time-to-Value | 0.15 | 4 | 5 | 3 |
| Maintainability | 0.1 | 5 | 3 | 2 |
| **Operational** | | | | |
| Observability | 0.05 | 5 | 3 | 3 |
| Reliability | 0.05 | 5 | 4 | 3 |
| Supportability | 0.05 | 5 | 4 | 2 |
| **Weighted Total** | **1.0** | **4.6** | **3.2** | **2.5** |

## Consequences

### Positive

1. **Immediate Updates**: Changes reflected in next toolbar display
2. **Reactive**: Consistent with app's StateFlow architecture
3. **Efficient**: Only observes changes, not continuous polling
4. **Testable**: StateFlow can be tested in isolation

### Negative

1. **Coroutine Scope**: Requires coroutine scope in FeederTextToolbar
2. **Lifecycle Management**: Must handle collection lifecycle properly

### Trade-offs

- Accept moderate complexity for immediate updates
- Trade simpler polling for reactive architecture
- Accept coroutine overhead for efficiency

## Implementation

### FeederTextToolbar Modification

```kotlin
class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    private val selectionMenuConfigFlow: StateFlow<SelectionMenuConfig>, // ADD
) : TextToolbar {
    private var actionMode: ActionMode? = null

    // Collect config in a coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentConfig: SelectionMenuConfig by mutableStateOf(
        selectionMenuConfigFlow.value,
    )

    init {
        // Observe config changes
        scope.launch {
            selectionMenuConfigFlow.collect { config ->
                currentConfig = config
                // Invalidate current action mode if shown
                actionMode?.invalidate()
            }
        }
    }

    private val textActionModeCallback: FeederTextActionModeCallback =
        FeederTextActionModeCallback(
            context = view.context,
            activityLauncher = activityLauncher,
            selectionMenuConfig = currentConfig, // Pass current config
            onActionModeDestroy = {
                actionMode = null
            },
        )

    // Clean up coroutine scope
    fun dispose() {
        scope.cancel()
    }
}
```

### ComposeProviders Integration

```kotlin
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val settingsStore: SettingsStore by LocalDI.current.instance()

    // Observe settings changes
    val config by settingsStore.selectionMenuConfig.collectAsStateWithLifecycle()

    DisposableEffect(config) {
        val toolbar = FeederTextToolbar(
            LocalView.current,
            activityLauncher,
            selectionMenuConfigFlow = rememberUpdatedState(config).value as StateFlow<SelectionMenuConfig>,
        )

        onDispose {
            toolbar.dispose()
        }
    }

    CompositionLocalProvider(
        LocalTextToolbar provides FeederTextToolbar(/* ... */)
    ) {
        content()
    }
}
```

### Alternative: Simpler Approach

If the above is too complex, use a simpler approach:

```kotlin
class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    private val getConfig: () -> SelectionMenuConfig, // ADD: Function to get current config
) : TextToolbar {

    private val textActionModeCallback: FeederTextActionModeCallback
        get() = FeederTextActionModeCallback(
            context = view.context,
            activityLauncher = activityLauncher,
            selectionMenuConfig = getConfig(), // Get current config each time
            onActionModeDestroy = { actionMode = null },
        )

    override fun showMenu(/* ... */) {
        // Recreate callback with latest config
        textActionModeCallback = createCallback()
        // ...
    }
}
```

**Note**: The simpler approach is preferred for lower complexity. Config is read on each `showMenu()` call, which happens when text is selected. This is sufficient for "real-time" updates (user sees changes on next text selection).

## Recommended Implementation

**Use the simpler approach**: Function to get current config, called on each toolbar display.

### Rationale for Simpler Approach

1. **Sufficient Real-Time**: Users expect updates on next text selection, not instant
2. **Lower Complexity**: No coroutine scope management
3. **No Memory Leaks**: No lifecycle concerns
4. **Testable**: Easy to mock config function

### Implementation

```kotlin
// In ComposeProviders
@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val settingsStore: SettingsStore by LocalDI.current.instance()

    // Get current config (updates on recomposition)
    val config by settingsStore.selectionMenuConfig.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalTextToolbar provides remember(config) {
            FeederTextToolbar(
                view = LocalView.current,
                activityLauncher = activityLauncher,
                getConfig = { config }, // Pass current config
            )
        }
    ) {
        content()
    }
}

// In FeederTextToolbar
class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    private val getConfig: () -> SelectionMenuConfig,
) : TextToolbar {
    // Use getConfig() whenever config is needed
}
```

## Performance Considerations

### Efficiency

- Config read: O(1) (just accessing StateFlow value)
- Toolbar creation: O(n) where n = number of menu items (same as before)
- Memory: No additional overhead (config already in memory)

### Reactivity

- Settings changes trigger recomposition
- Recomposition updates FeederTextToolbar
- Next toolbar display uses new config

## Reversibility

### Rollback Plan

If StateFlow approach causes issues:

**Trigger**: Memory leaks or performance problems

**Rollback Steps**:
1. Fall back to polling on toolbar display
2. Time estimate: 2 hours

**Cost**: Low (simple change)

## Alternatives Considered

See Options 1-3 in Decision section above.

## Related Decisions

- ADR-001: Configuration Persistence Strategy
- ADR-002: Drag-and-Drop Implementation Approach

## References

- StateFlow documentation
- Compose side-effects documentation
- Android TextToolbar API
