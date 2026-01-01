# Development Rules - Auto Fetch Full Article Feature

**Project:** Feeder
**Feature:** Auto Fetch Full Article (Feature 006)
**Date:** 2026-01-01
**Coordinator:** Super Dev Coordinator

## Core Development Principles

### 1. Incremental Development
- Make small, atomic commits
- Each commit must compile/build successfully
- Each commit must pass all tests
- No partial implementations in commits

### 2. Learn from Existing Code
- Research and plan before beginning implementation
- Study three analogous features/components in the codebase
- Identify common patterns and project conventions
- Adhere to existing standards and libraries

### 3. Pragmatic Approach
- Adapt to project realities
- Choose simple, clear solutions
- Avoid over-engineering
- Keep code simple, readable, and practical

### 4. Decision Framework Priority
1. **Testability** - Is it easy to test?
2. **Readability** - Will it still be understandable in six months?
3. **Consistency** - Does it conform to project patterns?
4. **Simplicity** - Is it the simplest viable solution?
5. **Reversibility** - What is the difficulty of making subsequent changes?

### 5. Code Quality Standards
- Maximize code reuse
- Manage cyclomatic complexity
- Emphasize modular design
- Use design patterns where appropriate
- Minimize changes to existing code
- Occam's Razor: Do not add code unless necessary

## Implementation Process

### Phase-Based Development
1. **Understand existing patterns** - Study analogous features
2. **Identify common patterns** - Determine project conventions
3. **Adhere to existing standards** - Use same libraries/tools
4. **Implement in phases** - Decompose into 3-5 stages
5. **No reinventing the wheel** - Reuse open-source components

### API Implementation Rules
- Always use versioned API endpoints (e.g., `/api/v1/xxx`)
- In Next.js: Use Route Handlers instead of global middleware
- Prefer implementing proxy via `/app/api/proxy/route.ts`
- Use `next.config.js` rewrites/redirects when appropriate

### Project Structure Rules
- If project includes both frontend and backend, split into `/frontend` and `/backend`
- Maintain separate build/test pipelines for each

## Quality Standards

### Build Requirements
- Each commit must compile successfully
- Pass all existing tests
- Include tests for new functionality
- Adhere to project formatting and linting checks

### Testing Requirements
- Write unit tests for new functionality
- Write integration tests for complex workflows
- Maintain test coverage according to project standards

## Refactoring Process

1. Analyze project according to Clean Code principles
2. Prepare incremental refactoring checklist
3. Prioritize items from highest to lowest
4. Execute items one by one
5. Update to-do status after each completion
6. Obtain approval before proceeding to next step

## Error Handling

### When Stuck
- Stop after maximum of three attempts
- Record failure cause and error messages
- Investigate 2-3 alternative implementation approaches
- Challenge foundational assumptions
- Consider: Is solution over-abstracted? Can it be decomposed?

### Error Recovery
- Build failure: Fix code, rebuild (max 3 attempts)
- Test failure: Fix code or test, re-run (max 3 attempts)
- Missing file: Create required file
- Sub-agent timeout: Retry invocation

## Project-Specific Rules

### No Backward Compatibility
- Break old formats freely when needed
- Focus on clean implementation for current needs

### GitHub Rules
- Never create GitHub actions for new projects
- Don't add `.github` files to git cache
- Don't commit GitHub action files
- Only commit files edited/created in current session
- Use `git add file1 file2` (NOT `git add -A`)

### Time Context
- Always include current date/time in prompts
- Use Time MCP for accurate time context

### Figma Integration (if applicable)
1. Run `get_design_context` first
2. If response too large, run `get_metadata` then re-fetch
3. Run `get_screenshot` for visual reference
4. Only after both, download assets and start implementation
5. Translate Figma output to project conventions
6. Validate against Figma for 1:1 look and behavior

## First Principles Analysis

For bug fixes or complex features:
- Apply First Principles thinking
- Break down to fundamental truths
- Build solution from ground up
- Question assumptions and existing approaches

## Success Criteria

### Code Quality
- [ ] No TODO/FIXME comments for current feature
- [ ] No debug code or console.log remaining
- [ ] Build passes without errors
- [ ] Build passes without warnings
- [ ] All tests passing

### Git State
- [ ] All changes staged
- [ ] Commit message follows conventions
- [ ] Changes committed
- [ ] Changes pushed to remote
- [ ] Git status shows "working tree clean"

### Documentation
- [ ] All specification documents complete
- [ ] Implementation summary written
- [ ] Task list shows all tasks complete
- [ ] Code review approved

## Project Conventions (To Be Discovered)

During Phase 5 (Code Assessment), identify:
- Programming language(s) and version(s)
- Framework(s) in use
- Testing framework(s)
- Code formatting tools
- Linting tools
- Build system
- Dependency management
- State management approach
- Routing patterns
- API design patterns
- Component organization
- File naming conventions
- Git commit message conventions

---

**Last Updated:** 2026-01-01
**Status:** Active - Apply to all development work for Feature 006
