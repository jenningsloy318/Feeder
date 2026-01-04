# ADR-002: Drag-and-Drop Implementation Approach

**Status**: Accepted
**Date**: 2026-01-04
**Decision**: Custom Compose modifier with pointer input
**Context**: Selection Menu Configuration Feature

## Context

The Selection Menu Configuration feature requires drag-and-drop functionality for reordering menu items in a settings list. The app uses Jetpack Compose for UI.

## Decision

**Implement a custom `Modifier.dragToReorder()` using Compose pointer input APIs.**

### Rationale

**Option 1: Custom Modifier with Pointer Input (SELECTED)**
- **Pros**:
  - No external dependencies
  - Full control over behavior and visuals
  - Follows project philosophy (minimal dependencies)
  - Well-documented pattern exists (Nutrient blog)
  - Lightweight (~100 lines of code)
- **Cons**:
  - Custom implementation required
  - More complex than using a library
- **Score**: 4.5/5

**Option 2: Accompanist (Library)**
- **Pros**:
  - Ready-to-use solution
  - Battle-tested
  - Official Google library (historically)
- **Cons**:
  - Accompanist deprecated (migrated to Compose Foundation)
  - Additional dependency
  - Less control over behavior
- **Score**: 2.0/5

**Option 3: Compose Reorderable (Third-Party)**
- **Pros**:
  - Dedicated library for this use case
  - Feature-rich
  - Less custom code
- **Cons**:
  - External dependency
  - Not aligned with project philosophy
  - Maintenance risk (third-party)
  - Overkill for simple reordering
- **Score**: 2.5/5

### Evaluation Matrix

| Criterion | Weight | Custom | Accompanist | Reorderable |
|-----------|--------|--------|------------|-------------|
| **Technical** | | | | |
| Modularity | 0.1 | 5 | 4 | 5 |
| Scalability | 0.05 | 4 | 4 | 5 |
| Performance | 0.1 | 4 | 4 | 4 |
| Security | 0.05 | 5 | 5 | 4 |
| **Delivery** | | | | |
| Complexity | 0.2 | 3 | 5 | 4 |
| Risk | 0.1 | 4 | 3 | 3 |
| Time-to-Value | 0.15 | 3 | 5 | 4 |
| Maintainability | 0.1 | 4 | 2 | 2 |
| **Operational** | | | | |
| Observability | 0.05 | 5 | 4 | 4 |
| Reliability | 0.05 | 4 | 5 | 4 |
| Supportability | 0.05 | 5 | 3 | 2 |
| **Weighted Total** | **1.0** | **4.5** | **2.0** | **2.5** |

## Consequences

### Positive

1. **No Dependencies**: Aligns with project philosophy
2. **Control**: Full control over UX and behavior
3. **Lightweight**: Minimal code footprint
4. **Maintainable**: Simple, understandable implementation

### Negative

1. **Implementation Effort**: Requires custom development (~4-6 hours)
2. **Testing**: More thorough testing required
3. **Edge Cases**: Must handle all drag scenarios

### Trade-offs

- Accept higher upfront implementation cost for long-term maintainability
- Trade convenience of library for control and simplicity
- Accept moderate complexity to avoid external dependency

## Implementation

### Modifier Signature

```kotlin
fun Modifier.dragToReorder(
    item: SelectionMenuItem,
    itemList: List<SelectionMenuItem>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): Modifier = composed { /* ... */ }
```

### Implementation Details

```kotlin
fun Modifier.dragToReorder(
    item: SelectionMenuItem,
    itemList: List<SelectionMenuItem>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): Modifier = composed {
    val currentIndex = remember(itemList) { itemList.indexOf(item) }
    val dragState = rememberDragState(currentIndex)

    Modifier
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragState.isDragging = true
                    dragState.initialIndex = currentIndex
                },
                onDrag = { change, offset ->
                    change.consume()
                    dragState.dragOffset = offset
                },
                onDragEnd = {
                    val targetIndex = calculateTargetIndex(
                        dragState.dragOffset,
                        itemList.size,
                    )
                    if (targetIndex != currentIndex && targetIndex >= 0) {
                        onMove(currentIndex, targetIndex)
                    }
                    dragState.reset()
                },
                onDragCancel = {
                    dragState.reset()
                },
            )
        }
        .graphicsLayer {
            // Visual feedback during drag
            alpha = if (dragState.isDragging) 0.8f else 1f
            elevation = if (dragState.isDragging) 8.dp.toPx() else 0.dp.toPx()
            translationY = if (dragState.isDragging) {
                dragState.dragOffset.y
            } else {
                0f
            }
        }
}

@Composable
private fun rememberDragState(initialIndex: Int): DragState {
    return remember(initialIndex) {
        DragState(initialIndex = initialIndex)
    }
}

private class DragState(
    initialIndex: Int,
) {
    var isDragging by mutableStateOf(false)
    var dragOffset by mutableStateOf(Offset.Zero)
    var initialIndex by mutableStateOf(initialIndex)

    fun reset() {
        isDragging = false
        dragOffset = Offset.Zero
    }
}
```

### Usage in List

```kotlin
@Composable
fun SelectionMenuList(
    items: List<SelectionMenuItem>,
    onMove: (from: Int, to: Int) -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            SelectionMenuItemRow(
                item = item,
                modifier = Modifier
                    .dragToReorder(
                        item = item,
                        itemList = items,
                        onMove = onMove,
                    ),
                onToggle = onToggle,
            )
        }
    }
}
```

## Performance Considerations

### Optimization Strategies

1. **Key Parameter**: Use `key = { it.id }` in LazyColumn
2. **Derived State**: Use `derivedStateOf()` for computed positions
3. **Animation**: Use `animateItemPlacement()` for smooth transitions
4. **Throttle**: Throttle drag events to avoid excessive recomposition

### Performance Targets

- Frame rate: 60fps during drag
- Input latency: <16ms
- State update: <100ms on drag end

## Reversibility

### Rollback Plan

If custom implementation proves problematic:

**Trigger**: Performance issues or UX problems on test devices

**Rollback Steps**:
1. Replace custom modifier with Compose Reorderable library
2. Time estimate: 4 hours

**Cost**: Medium (requires refactoring)

## Alternatives Considered

See Options 1-3 in Decision section above.

## Related Decisions

- ADR-001: Configuration Persistence Strategy
- ADR-003: Real-Time Configuration Updates

## References

- [Nutrient - Drag-to-reorder with Compose](https://www.nutrient.io/blog/drag-to-reorder-with-compose/)
- [Android Developers - Drag and Drop in Compose](https://developer.android.com/codelabs/codelab-dnd-compose)
- Compose Pointer Input API documentation
