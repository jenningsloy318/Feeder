# ADR-001: Separate TranslationLanguage Enum from SummaryLanguage

## Status
Accepted

## Context and Problem Statement
The Feeder app currently has a `SummaryLanguage` enum for AI summary language selection. The new translation feature requires similar language selection functionality. We need to decide whether to reuse the existing `SummaryLanguage` enum or create a separate `TranslationLanguage` enum.

## Decision Drivers
- Type safety: Prevent mixing summary and translation language values
- Independent evolution: Translation and summary features may need different languages
- Code maintainability: Balance between DRY and separation of concerns
- User experience: Translation may need DEVICE_DEFAULT, summary has AUTO_DETECT

## Considered Options
1. **Reuse SummaryLanguage enum**
2. **Create separate TranslationLanguage enum**

## Decision Outcome
Chosen option: "Create separate TranslationLanguage enum", because it provides type safety, allows independent evolution of features, and maintains clear semantic separation despite minor code duplication.

### Consequences
- **Good**: Type-safe - compiler prevents accidentally passing wrong language type
- **Good**: Clear separation of concerns - translation and summary are independent features
- **Good**: Independent evolution - can add translation-specific languages (DEVICE_DEFAULT) without affecting summary
- **Bad**: Minor code duplication (~50 lines for enum entries)
- **Mitigation**: Keep language lists in sync via code review; consider common base enum if divergence grows

## Pros and Cons of the Options

### Option 1: Reuse SummaryLanguage enum
- **Good**, because less code duplication
- **Good**, because single source of truth for language list
- **Bad**, because couples translation to summary features
- **Bad**, because less type-safe (can pass summary language to translation)
- **Bad**, because DEVICE_DEFAULT vs AUTO_DETECT have different semantics

### Option 2: Create separate TranslationLanguage enum
- **Good**, because type-safe (distinct types prevent misuse)
- **Good**, because clear separation of concerns
- **Good**, because independent evolution (translation can add DEVICE_DEFAULT)
- **Good**, because consistent with domain modeling (different features = different types)
- **Bad**, because maintains two similar enums (~50 lines duplication)
- **Mitigation**: Language entries are stable, low maintenance burden

## Evaluation Matrix

| Criteria | Weight | Option 1: Reuse | Option 2: Separate |
|----------|--------|-----------------|-------------------|
| **Technical Quality** |
| Modularity | 0.10 | 2 (tightly coupled) | 5 (independent modules) |
| Coupling/Cohesion | 0.10 | 2 (high coupling) | 5 (low coupling, high cohesion) |
| Scalability | 0.10 | 3 (shared constraints) | 5 (independent evolution) |
| Performance | 0.10 | 5 (same) | 5 (same) |
| Security | 0.10 | 5 (same) | 5 (same) |
| **Delivery** |
| Implementation Complexity | 0.08 | 4 (reuse existing) | 3 (create new) |
| Risk | 0.08 | 3 (coupling risk) | 5 (isolated) |
| Time-to-Value | 0.07 | 4 (faster) | 3 (slightly slower) |
| Maintainability | 0.04 | 2 (coupled changes) | 5 (independent) |
| Testability | 0.03 | 3 (shared tests) | 5 (isolated tests) |
| **Operational** |
| Observability | 0.05 | 5 (same) | 5 (same) |
| Reliability | 0.05 | 4 (good) | 5 (better) |
| Cost | 0.05 | 5 (less code) | 4 (more code) |
| Supportability | 0.03 | 3 (ambiguous) | 5 (clear intent) |
| Reversibility | 0.02 | 3 (harder to split) | 5 (easy to merge) |
| **Weighted Total** | | **3.34** | **4.61** |

## Links
- Related to: [Translation Configuration Architecture](./05-architecture.md)
- Similar pattern: SummaryLanguage enum in `com.nononsenseapps.feeder.ai.model`
