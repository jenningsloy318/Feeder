# Development Rules and Philosophy

**Document Version**: 1.0
**Last Updated**: 2026-01-02
**Applied To**: Move Summarize Button to Article Page Top

## Overview

This document outlines the development philosophy and rules applied to the button relocation feature, ensuring consistency with project standards and best practices.

## Core Development Philosophy

### 1. Incremental Development

- **Make small, atomic commits**: Each commit must compile/build successfully and pass all tests
- **Build verification**: Every commit is verified to compile before proceeding
- **Test coverage**: All changes include appropriate tests
- **Progressive enhancement**: Changes build upon existing functionality

### 2. Learn from Existing Code

- **Study three analogous features**: Examine similar button/menu components
- **Identify common patterns**: Determine project conventions for UI components
- **Adhere to established standards**: Use existing libraries and testing patterns
- **Reuse before creating**: Extensive reuse of existing components

### 3. Pragmatic, Not Dogmatic

- **Adapt to project realities**: Work within existing architecture
- **Choose simple solutions**: Prefer straightforward implementations
- **Avoid over-engineering**: Keep code simple and readable
- **Minimize changes**: Only modify necessary code

### 4. Explicit Intent Over Clever Code

- **Choose clarity**: Code should be immediately understandable
- **Self-documenting**: Variable and function names explain purpose
- **Comment complexity**: Explain non-obvious logic
- **Avoid tricks**: No clever code that sacrifices readability

## Quality Standards

### Build and Test Requirements

1. **Each commit must compile successfully**
   - Use `./gradlew assembleDebug` to verify build
   - Fix all compilation errors before committing
   - Ensure no warnings introduced

2. **Pass all existing tests**
   - Run `./gradlew test` before committing
   - Fix any test failures
   - Maintain test coverage standards

3. **Include tests for new functionality**
   - Unit tests for component logic
   - UI tests for button placement
   - Integration tests for menu actions

4. **Adhere to project formatting and linting**
   - Follow Kotlin coding conventions
   - Use project's code style
   - Run lint checks: `./gradlew lint`

## Decision Framework Priority

When making technical decisions, prioritize in this order:

1. **Testability** - Is it easy to test?
2. **Readability** - Will it still be understandable in six months?
3. **Consistency** - Does it conform to project patterns?
4. **Simplicity** - Is it the simplest viable solution?
5. **Reversibility** - What is the difficulty of making subsequent changes?

## Specific Rules for This Feature

### UI Component Reorganization

1. **Maintain existing component structure**
   - Reuse existing button components
   - Follow Material Design 3 guidelines
   - Maintain accessibility standards

2. **Preserve functionality**
   - No changes to button behavior
   - No changes to underlying logic
   - Only reposition UI elements

3. **Minimize code changes**
   - Move components, don't rewrite
   - Keep event handlers intact
   - Preserve state management

### Code Organization

1. **Follow existing patterns**
   - Study other menu action implementations
   - Use same component structure
   - Maintain naming conventions

2. **Modularity**
   - Keep components independent
   - Use proper separation of concerns
   - Maintain clean interfaces

### Testing Strategy

1. **Unit tests**
   - Test button placement logic
   - Test menu action registration
   - Test state management

2. **UI tests**
   - Verify button visibility
   - Test button interaction
   - Verify menu item presence

3. **Integration tests**
   - Test article screen functionality
   - Verify button actions work correctly
   - Test menu operations

## Error Handling

### Recovery Strategy

- Maximum 3 attempts for any build/test failure
- Document failure cause and error messages
- Investigate alternative approaches
- Challenge foundational assumptions if stuck

### When to Stop

Only stop execution for:
1. Critical error that cannot be resolved
2. External dependency unavailable
3. Permission denied for required operation
4. User explicitly requests stop

## Git Workflow

### Commit Standards

- **Atomic commits**: Each commit does one logical thing
- **Descriptive messages**: Follow project commit conventions
- **No intermediate broken states**: Every commit compiles and tests pass
- **Incremental progression**: Build feature step by step

### Branch Strategy

- Working in: `spec-07-move-summary-button`
- Base branch: `master`
- Merge strategy: Rebase or merge as per project standards

## Documentation Requirements

1. **Code comments**: Explain non-obvious logic
2. **Commit messages**: Clear description of changes
3. **Specification updates**: Keep docs in sync with implementation
4. **README updates**: Document any user-visible changes

## Success Criteria

The feature is complete when:

- [ ] All commits compile successfully
- [ ] All tests pass (unit, UI, integration)
- [ ] Button placement matches requirements
- [ ] Menu actions work correctly
- [ ] No regression in existing functionality
- [ ] Code follows project standards
- [ ] Documentation is updated
- [ ] Changes committed and pushed

## References

- Project README
- Kotlin coding conventions
- Material Design 3 guidelines
- Project's existing button/menu implementations

---

**Applied By**: Claude (Coordinator Agent)
**Date**: 2026-01-02
**Status**: Active
