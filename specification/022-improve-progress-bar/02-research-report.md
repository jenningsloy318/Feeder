# Research Report: Material Design 3 Progress Indicators

## Date
2026-01-05

## Progress Indicator Best Practices

### Material Design 3 Guidelines

According to Material Design 3 documentation, progress indicators should:

1. **Show context with text labels**
   - Text labels should describe what is loading
   - Place text above or below the indicator
   - Use sentence case for labels
   - Keep labels concise (1-3 words)

2. **Typography for progress indicators**
   - Use `bodySmall` or `labelMedium` typography for labels
   - Color: `onSurfaceVariant` for secondary text
   - Same color family as the progress indicator

3. **Layout patterns**
   - Vertical spacing: 8dp - 16dp between text and indicator
   - Horizontal alignment: center or start
   - Card padding: 16dp standard

### Implementation Patterns in Android Compose

```kotlin
// Pattern 1: Text above indicator
Column(
    modifier = Modifier.padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = "Loading...",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth()
    )
}

// Pattern 2: Text below indicator
Column(
    modifier = Modifier.padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Loading...",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

### Accessibility Considerations

1. **Screen reader support**
   - Progress indicators should have content descriptions
   - Text labels should be included in semantics
   - Use `semantics { contentDescription = "..." }`

2. **Color contrast**
   - Ensure text meets WCAG AA standards (4.5:1)
   - `onSurfaceVariant` provides good contrast

## Feeder App Patterns

### Existing Progress Indicators

From code review, Feeder uses:
- `LinearProgressIndicator` for loading states
- `OutlinedCard` container for AI features
- 16dp padding standard
- Center alignment for content

### Current Implementation

```kotlin
// SummarySection - Current (no text)
OutlinedCard(modifier = Modifier.fillMaxWidth()) {
    LinearProgressIndicator(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    )
}

// TranslationStatusSection - Current (no text)
OutlinedCard(modifier = Modifier.fillMaxWidth()) {
    LinearProgressIndicator(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    )
}
```

## Recommended Approach

Based on research and existing patterns:

1. **Add text above the progress indicator** (more visible)
2. **Use `bodySmall` typography** for consistency
3. **Use `onSurfaceVariant` color** for secondary text
4. **8dp spacing** between text and indicator
5. **Center align** text and indicator

### String Resources

Add to `strings.xml`:
```xml
<string name="summarizing_progress">Summarizing...</string>
<string name="translating_progress">Translating...</string>
```

## References
- Material Design 3: Progress Indicators
- Android Compose Material3 Documentation
- Existing Feeder app patterns
