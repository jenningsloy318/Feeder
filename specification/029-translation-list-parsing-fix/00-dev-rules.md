# Development Rules - Feeder AI Summary Configuration Feature

**Created:** 2026-01-01
**Current Time:** 2026-01-01 19:11:55 +08:00
**Project:** Feeder (Android RSS Reader)
**Technology Stack:** Kotlin, Android SDK, Gradle

---

## Core Development Philosophy

### 1. Incremental Development
- **Atomic Commits:** Make small, focused commits that compile successfully and pass all tests
- **Continuous Verification:** Each commit must build and pass tests
- **No Big Bang Changes:** Break down complex features into small, verifiable increments

### 2. Learn Before Implementing
- **Research First:** Study existing code patterns before making changes
- **Understand Context:** Analyze at least 3 similar features/components in the codebase
- **Pattern Recognition:** Identify and follow established conventions

### 3. Pragmatic Over Dogmatic
- **Real-World Focus:** Adapt solutions to actual project needs, not theoretical ideals
- **Practical Solutions:** Choose working solutions over perfect ones
- **Context Aware:** Consider team expertise, maintenance burden, and delivery timeline

### 4. Explicit Intent Over Clever Code
- **Readability First:** Write code that clearly expresses intent
- **Avoid Cleverness:** Simple, clear solutions over tricky optimizations
- **Self-Documenting:** Code should explain itself without excessive comments

### 5. Occam's Razor
- **Minimal Complexity:** Do not add code unless necessary
- **Simplest Solution:** Choose the simplest viable solution that meets requirements
- **YAGNI:** You Aren't Gonna Need It - avoid speculative features

### 6. No Backward Compatibility
- **Break Freely:** Old formats can be broken without concern for migration
- **Clean Slate:** Design for current needs, not historical constraints
- **Data Migration:** Use database migrations when schema changes are needed

### 7. First Principles Analysis
- **Fundamental Truths:** Break down problems to basic truths
- **Question Assumptions:** Don't accept "we've always done it this way"
- **Build from Ground Up:** Reason up from first principles, not by analogy

---

## Implementation Process

### Phase 1: Understanding
1. Study existing code patterns in the codebase (minimum 3 similar features)
2. Identify common conventions (naming, structure, architecture)
3. Map dependencies and integration points
4. Document assumptions and验证 with code analysis

### Phase 2: Planning
1. Decompose complex work into 3-5 stages
2. Define clear interfaces and contracts before implementation
3. Plan database schema changes with migration scripts
4. Design UI components following existing patterns

### Phase 3: Implementation
1. **No Reinventing the Wheel:** Extensively reuse open-source components
2. **Glue Code:** Use AI to write integration/adaptation layers
3. **Modularity:** Develop like assembling building blocks
4. **Versioned APIs:** Use versioned endpoints (e.g., `/api/v1/xxx`)
5. **Android Best Practices:**
   - Use proper lifecycle-aware components
   - Follow Android Architecture Components patterns
   - Use Coroutines for async operations
   - Implement proper state management

### Phase 4: Quality Assurance
1. Each commit must compile successfully
2. All existing tests must pass
3. New features must include tests
4. Adhere to ktlint formatting and linting rules
5. No TODO/FIXME comments in committed code

---

## Decision Framework Priority

When making technical decisions, prioritize in this order:

1. **Testability** - Is it easy to test? Can we write unit/integration/UI tests?
2. **Readability** - Will it be understandable in 6 months by another developer?
3. **Consistency** - Does it conform to existing project patterns?
4. **Simplicity** - Is it the simplest viable solution?
5. **Reversibility** - How difficult will it be to make subsequent changes?

---

## Android-Specific Rules

### Architecture
- **MVVM Pattern:** Use Model-View-ViewModel architecture
- **Repository Pattern:** Abstract data sources behind repositories
- **Dependency Injection:** Use manual DI or Koin (verify which is used in project)
- **State Management:** Use StateFlow/LiveData for reactive UI

### UI Development
- **Jetpack Compose:** Check if project uses Compose or XML layouts
- **Material Design:** Follow Material Design 3 guidelines
- **Navigation:** Use Jetpack Navigation Component
- **Theming:** Use project's existing theme system

### Data Layer
- **Room Database:** Use Room for local persistence
- **DataStore:** Use DataStore for key-value preferences
- **Retrofit/OkHttp:** Use for network operations (if applicable)
- **Coroutines:** Use Kotlin Coroutines for async operations

### Build Configuration
- **Gradle KTS:** Use Kotlin DSL for Gradle scripts
- **Build Variants:** Support debug and release builds
- **ProGuard/R8:** Configure code shrinking for release builds

---

## Code Quality Standards

### Kotlin Code Style
- **ktlint:** Follow ktlint formatting rules
- **Naming Conventions:**
  - Classes: PascalCase (e.g., `SummaryConfigViewModel`)
  - Functions: camelCase (e.g., `getSummaryConfig`)
  - Constants: UPPER_SNAKE_CASE (e.g., `MAX_SUMMARY_LENGTH`)
  - Packages: lowercase with dots (e.g., `com.nononsenseapps.feeder.ui.summary`)

### Code Organization
- **Package Structure:**
  - `di/` - Dependency injection
  - `ui/` - UI components (activities, fragments, composables)
  - `viewmodel/` - ViewModels
  - `model/` - Data models
  - `repository/` - Repository implementations
  - `db/` - Database entities, DAOs, migrations
  - `util/` - Utility functions and extensions

### Documentation
- **KDoc:** Document public APIs with KDoc comments
- **README:** Update README for significant features
- **CHANGELOG:** Record user-visible changes in CHANGELOG.md

---

## Testing Requirements

### Unit Tests
- Test business logic in ViewModels
- Test repository implementations
- Test utility functions
- Aim for >80% code coverage on new code

### Integration Tests
- Test database operations
- Test repository interactions with data sources
- Test complex workflows

### UI Tests
- Test critical user flows
- Test navigation between screens
- Test form validation
- Use Espresso or Compose Testing

---

## Git Workflow

### Commit Rules
- **Atomic Commits:** One logical change per commit
- **Descriptive Messages:** Use imperative mood ("Add feature", not "Added feature")
- **Commit Message Format:**
  ```
  <type>: <subject>

  <body (optional)>

  <footer (optional)>
  ```

  Types: feat, fix, refactor, test, docs, chore, style

### Branch Rules
- **Feature Branches:** Create branches for each feature
- **Branch Naming:** Use descriptive names (e.g., `feature/ai-summary-config`)
- **No Direct Commits:** Never commit directly to master

### Push Rules
- **Selective Staging:** Only stage files you edited/created in this session
- **NEVER use `git add -A`** - explicitly specify files
- **Verify Clean State:** Ensure `git status` shows clean after commits

---

## Error Handling

### When Stuck
1. **Stop After 3 Attempts:** Don't spin wheels
2. **Document Failures:** Record error messages and what was tried
3. **Explore Alternatives:** Investigate 2-3 different approaches
4. **Question Assumptions:** Challenge foundational assumptions
5. **Decompose:** Break problem into smaller parts
6. **Ask for Help:** Consult team or project documentation

### Error Recovery
- **Build Failures:** Fix compilation errors before committing
- **Test Failures:** Fix tests or update them if requirements changed
- **Runtime Crashes:** Add proper error handling and logging

---

## Security and Privacy

### User Data
- **Minimal Collection:** Only collect data necessary for features
- **Local Storage:** Store sensitive data locally when possible
- **Encryption:** Encrypt sensitive data at rest

### API Keys
- **Never Commit Keys:** Never commit API keys or secrets
- **BuildConfig:** Use BuildConfig for API keys
- **Environment Variables:** Use environment variables for CI/CD

---

## Performance Considerations

### UI Performance
- **Main Thread:** Keep main thread unblocked
- **Lazy Loading:** Use lazy loading for large lists
- **Image Loading:** Use Coil or Glide for efficient image loading
- **Memory Leaks:** Watch for memory leaks (use LeakCanary in debug)

### Battery and Network
- **Efficient Polling:** Use efficient polling strategies
- **Background Work:** Use WorkManager for background tasks
- **Network Optimization:** Minimize network calls, use caching

---

## Accessibility

### Material Accessibility
- **Content Descriptions:** Add content descriptions for images
- **Minimum Touch Target:** 48dp minimum touch target size
- **Contrast Ratios:** Ensure WCAG AA compliant contrast
- **Screen Reader:** Support TalkBack navigation

---

## Current Feature Context

### Feature: Improve AI Integration Summary Configuration
**Current State:**
- "Summary Language" is a simple config key
- Single setting for summary language

**Desired State:**
1. Rename "Summary Language" to "Summary"
2. Navigate to dedicated page on click
3. Configure:
   - Enable/Disable summary for page reading
   - Configure language for summarization

**Impact Areas:**
- UI: Settings screens, navigation
- Data: Configuration storage
- Business Logic: Summary generation trigger logic

---

## Checklist for Each Phase

### Before Coding
- [ ] Reviewed similar features in codebase
- [ ] Understood existing patterns
- [ ] Identified data model changes
- [ ] Planned UI changes
- [ ] Considered migration strategy

### During Coding
- [ ] Following project conventions
- [ ] Writing tests alongside code
- [ ] Using ktlint formatting
- [ ] No TODO/FIXME comments
- [ ] Proper error handling

### Before Commit
- [ ] Code compiles successfully
- [ ] All tests pass
- [ ] ktlint checks pass
- [ ] No debug code remaining
- [ ] Only edited files staged

---

## References

- **Kotlin Coding Conventions:** https://kotlinlang.org/docs/coding-conventions.html
- **Android App Quality Guidelines:** https://developer.android.com/quality
- **Material Design:** https://m3.material.io/
- **Android Architecture:** https://developer.android.com/topic/architecture

---

**Note:** These rules are living documents. Update them as project patterns evolve.
