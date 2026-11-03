# ADR 0003: A separate typed-decision client boundary

## Status

Accepted for the module/client boundary. Vendor protocol assumptions remain unverified by upstream tests.

## Context and decision

Issue #13 / PR #24 introduced `ai-decision` for state-and-question based decisions. Keep this distinct from Spring AI `ChatModel`: a decision has typed Choice, Score, or Noul answer fields, not a free-text completion. Define `DecisionClient` in the Demo module and implement the current HTTP integration in `JevDecisionClient`. This provides a narrow extension seam without inventing a broad multi-provider framework before a second provider exists.

Use `DecisionProperties` and WebClient for base URL, key, model, and timeout. The controller depends on `DecisionClient`; decision DTOs stay in `DecisionPrimitives`. This chooses explicit domain records and adapter mapping over pretending the endpoint is an OpenAI-compatible chat model or moving vendor-specific types into shared configuration.

## Implementation qualifications

- Only one client component exists. `ai.decision.provider` does not select beans; adding providers requires explicit bean-selection wiring.
- `/v1/systemone`, default endpoint/model, and answer-field aliases are current adapter assumptions. Neither successful upstream HTTP mapping nor probability calibration is established by the existing tests.
- Local Noul means a null/none answer with reason/probability. Its external-provider semantics require confirmation.
- Default tests cover startup, ping, empty questions, and DTO construction. Earlier descriptions of mocked HTTP/mapping tests overstated coverage.
- Answer parsing is permissive (including probability default 1.0); validation, retries, and provider-specific error mapping remain gaps, not accepted guarantees.

## Consequences and references

Decision callers avoid chat-specific coupling and future implementations have a clear seam. The interface alone does not deliver provider portability or a verified vendor contract. The [module guide](../../ai-decision/README.md) documents the exact request/parser behavior and current limits. Follow [ADR-0002](0002-monorepo-shared-ai-common-module.md) for module placement and shared dependency boundaries.
