# Research Report - Spec 25: Long-Press Input Bug Fix

## Research Date
January 7, 2026

## Research Focus
Jetpack Compose text selection in TextField components and best practices for implementing text selection toolbars.

## Compose Text Selection Architecture

### SelectionContainer Component

**Purpose**: Enables text selection within Compose composables.

**Key Characteristics**:
- Wraps composables containing text
- Enables long-press to select text
- Shows standard Android text selection toolbar
- Works with Text, TextField, and other text composables

**Import Statement**:
```kotlin
import androidx.compose.foundation.text.selection.SelectionContainer
```

### Usage Patterns

#### Pattern 1: Basic SelectionContainer
```kotlin
SelectionContainer {
    Text("This text can be selected")
}
```

#### Pattern 2: TextField with Selection
```kotlin
SelectionContainer {
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth()
    )
}
```

#### Pattern 3: OutlinedTextField with Selection
```kotlin
SelectionContainer {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}
```

## Best Practices

### 1. When to Use SelectionContainer

**USE when**:
- User needs to copy text from read-only text displays
- User needs to copy/paste within input fields
- Text selection is a core user need
- Following Android Material Design guidelines

**AVOID when**:
- Text is sensitive (passwords, tokens)
- Text is extremely short (single character)
- Selection would interfere with gestures

### 2. Performance Considerations

**Impact**: Minimal
- SelectionContainer is a lightweight wrapper
- No significant memory overhead
- No measurable performance impact on modern devices

**Optimization**: None needed for typical use cases

### 3. Modifier Order

**Best practice**:
```kotlin
SelectionContainer {
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .focusable()
    )
}
```

**Note**: SelectionContainer should be the outermost wrapper for content, not applied as a modifier to the TextField itself.

### 4. Nested SelectionContainers

**Behavior**: Not recommended, can cause conflicts.

**Alternative**: Use a single SelectionContainer wrapping all selectable content.

## Android Compatibility

### Minimum SDK Support
- **Required**: Android API 19+ (KitKat)
- **Project minSdk**: 29 (Android 10) - ✓ Fully supported
- **Tested on**: Project targetSdk

### Known Issues
None for SDK 29+. Text selection is mature and stable.

## TextField-Specific Considerations

### 1. Text Selection vs. Cursor Management

**TextField has built-in cursor management**:
- Tap to place cursor
- Long-press to select text (when wrapped in SelectionContainer)
- Drag handles to adjust selection

**No conflicts**: SelectionContainer enhances TextField without interfering with cursor behavior.

### 2. Keyboard Interaction

**Behavior**:
- Software keyboard still works normally
- Hardware keyboard shortcuts (Ctrl+C, Ctrl+V) work
- No changes needed to keyboard handling

### 3. Focus Management

**No impact**:
- Focus requests work the same way
- Focus changes unaffected
- IME action handling unchanged

## Compose Material3 Considerations

### Material3 TextField
- Compatible with SelectionContainer
- No visual conflicts
- Maintains Material3 design language

### OutlinedTextField
- Compatible with SelectionContainer
- Selection works within outlined border
- No visual glitches

## Alternative Approaches Considered

### 1. Custom Selection Logic
**Rejected**:
- Over-engineered
- Reinventing the wheel
- Maintenance burden

### 2. Third-Party Libraries
**Not needed**:
- SelectionContainer is built-in
- No additional dependencies
- Standard Android behavior

### 3. TextField Subclassing
**Not applicable in Compose**:
- Composable functions, not classes
- Composition over inheritance
- Wrapper approach is idiomatic

## Code Examples from Project

### Current Working Example: ReaderView.kt

**Location**: `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ReaderView.kt:110`

```kotlin
SelectionContainer {
    LazyColumnScrollbar(
        state = articleListState,
        settings = ScrollbarSettings.Default.copy(
            thumbUnselectedColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        LazyColumn(
            // ... content
        ) {
            // Article text content here
        }
    }
}
```

**Analysis**:
- SelectionContainer wraps the entire LazyColumn
- Works with complex nested composables
- No performance issues with scrollable content
- Proven pattern in production code

## Implementation Recommendations

### 1. Apply SelectionContainer Consistently

**Pattern**:
```kotlin
// BEFORE
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Label") }
)

// AFTER
SelectionContainer {
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}
```

### 2. Minimal Changes

**Approach**: Add wrapper only, don't modify existing TextField properties.

**Rationale**:
- Reduces risk of introducing bugs
- Easier to review and test
- Follows "minimal modification" principle

### 3. Testing Strategy

**Manual Testing**:
- Long-press each field
- Verify toolbar appears
- Test copy/paste/cut/select all
- Test with different input methods

**Automated Testing**:
- UI tests can verify toolbar appears
- Espresso interactions with selection
- Not strictly necessary for simple wrapper

## Potential Edge Cases

### 1. Password Fields
**Action**: Do NOT wrap password TextField in SelectionContainer
**Reason**: Security - users shouldn't copy passwords

### 2. Disabled Fields
**Action**: SelectionContainer works fine with disabled fields
**Reason**: User might want to copy disabled text

### 3. Read-Only Fields
**Action**: Wrap in SelectionContainer
**Reason**: User likely wants to copy read-only text

## References

- [Android Compose SelectionContainer Documentation](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/selection/SelectionContainer)
- [Material Design Text Fields](https://material.io/components/text-fields/android)
- Jetpack Compose Samples (GitHub)

## Conclusion

**Finding**: Adding `SelectionContainer` wrapper to TextField and OutlinedTextField components is the standard, recommended approach in Jetpack Compose for enabling text selection.

**Confidence**: High - backed by official documentation, working example in codebase, and Android best practices.

**Risk Level**: Low - minimal code changes, proven pattern, no dependencies.

**Recommendation**: Proceed with systematic application of SelectionContainer wrapper to all input fields.
