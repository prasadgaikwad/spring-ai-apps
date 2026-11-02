# ADR 0003: Decision Model Integration (System One/Jev)

## Status
Accepted

## Context
The repo adds `ai-decision` to demonstrate typed decision models (e.g. TypeSafe AI's Jev / System One) that return structured answers with calibrated probabilities rather than free-form text. We need a design consistent with:
- Monorepo structure (Maven modules, `ai-common` shared, provider portability)
- Spring AI 2.0 patterns and Boot 4
- Testing seams (mocked external calls, no mandatory DB for skeleton)
- Observability separation (tracing stays in `ai-observability`)

## Decision
1. **Separate decision models from chat models.** Decision models are not ChatModels; they are a distinct capability (typed primitives) and must not be forced into the ChatModel abstraction.
2. **Pluggable interface first.** Define `DecisionClient` with `JevDecisionClient` implementation now (vendor-specific), keeping the door open for local/mock providers later.
3. **Typed primitives.** Support `Choice`, `Score`, and `Noul` as first-class DTOs (`DecisionPrimitives`) with calibrated probabilities/uncertainty.
4. **HTTP adapter.** Use Spring WebClient for HTTP calls; configuration via `DecisionProperties` (base URL, API key, model, timeout). Keys read from env with fallbacks.
5. **Demo module placement.** Add `ai-decision` as a new demo module (like others), REST + OpenAPI, no Thymeleaf UI unless justified later.
6. **Testing.** Default tests use mocked clients/HTTP or in-memory behavior; no external network dependencies. Context-load smoke tests follow repo patterns.
7. **Docs/glossary.** Update `CONTEXT.md` with Decision model, Choice, Score, Noul.

## Consequences
- Clear separation of concerns from LLM chat flows
- Easy to mock in tests and swap providers
- Consistent with existing module patterns
- Minimal surface area; can evolve to full provider abstraction if multiple vendors are added

## References
- Issue #13, PR #24
- `ai-decision/` module
