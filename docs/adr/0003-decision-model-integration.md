# ADR 0003: A separate typed-decision client boundary

## Status

Accepted for the module/client boundary. Spring AI 2.1's `DecisionModel` was evaluated in issue #46 and is not adopted; see the [evaluation](../spikes/46-decision-model-evaluation.md). Mock-HTTP tests pin the adapter's assumed contract; live Jev semantics and probability calibration remain unverified.

## Context and decision

Issue #13 / PR #24 introduced `ai-decision` for state-and-question based decisions. Keep this distinct from Spring AI `ChatModel`: a decision has typed Choice, Score, or Noul answer fields, not a free-text completion. Define `DecisionClient` in the Demo module and implement the current HTTP integration in `JevDecisionClient`. This provides a narrow extension seam without inventing a broad multi-provider framework before a second provider exists.

Use `DecisionProperties` and WebClient for base URL, key, model, and timeout. The controller depends on `DecisionClient`; decision DTOs stay in `DecisionPrimitives`. This chooses explicit domain records and adapter mapping over pretending the endpoint is an OpenAI-compatible chat model or moving vendor-specific types into shared configuration.

## Implementation qualifications

- Only one client component exists. `ai.decision.provider` does not select beans; adding providers requires explicit bean-selection wiring.
- `/v1/systemone`, default endpoint/model, and answer-field aliases are current adapter assumptions. The MockWebServer tests in `ai-decision/src/test/java/dev/prasadgaikwad/springaiapps/decision/JevDecisionClientContractTests.java` verify request/response mapping against those assumed aliases; they are local mock-HTTP checks, not live vendor verification or calibration evidence.
- Local Noul means a null/none answer with reason/probability. Its external-provider semantics require confirmation.
- Default tests cover context/startup, ping, empty questions, and DTO fields/state copy. The MockWebServer contract tests additionally cover request path/headers/body, Choice/Score/Noul parsing and fallbacks, blank/missing payloads, non-2xx, invalid JSON, and timeout. They verify the adapter's assumed contract only; live Jev endpoint semantics, response formats, and calibration remain unverified. A REST-level success/failure seam with a stubbed `DecisionClient` is still absent.
- Answer parsing is permissive (including probability default 1.0); validation, retries, and provider-specific error mapping remain gaps, not accepted guarantees.

## Spring AI 2.1 `DecisionModel` evaluation (issue #46)

Spring AI 2.1 adds `DecisionModel`, `SystemOneDecisionModel`, and `OllamaDecisionModel`. The evaluation kept the custom adapter for three reasons:

- **Noul has no standard answer type.** The official `SystemOneDecisionMapper` converts a TypeSafe `NoulAnswer` into a `YesNoAnswer` carrying the probability. The Noul reason and its "no valid choice applies" meaning are lost.
- **Score semantics differ.** The standard `ScoreQuestion` is an ordered set of levels and its answer carries per-level probabilities. The local answer is an arbitrary numeric score with one probability. A mapping has to invent levels or change the public contract.
- **The contracts would change.** `DecisionModel` returns a map keyed by question name and validates answer types; the local API returns a flat `questionId`-keyed list and is deliberately permissive. The standard providers also replace much of the `ai.decision.*` configuration with provider starters, a `TypeSafeClient`, or provider-specific properties.

Keep the current `DecisionClient` seam, `JevDecisionClient`, `DecisionPrimitives`, and `ai.decision.*` contract. Any future wrapper around `DecisionModel` must preserve Noul and Score semantics, keep the REST/property contract or introduce an explicit breaking change, and be recorded as a new or superseding decision. The spike evidence and comparison matrix are in the [evaluation](../spikes/46-decision-model-evaluation.md).

## Consequences and references

Decision callers avoid chat-specific coupling and future implementations have a clear seam. The interface alone does not deliver provider portability or a verified vendor contract. The [module guide](../../ai-decision/README.md) documents the exact request/parser behavior and current limits. Follow [ADR-0002](0002-monorepo-shared-ai-common-module.md) for module placement and shared dependency boundaries.
