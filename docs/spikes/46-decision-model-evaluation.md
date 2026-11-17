# Spring AI `DecisionModel` evaluation (issue #46)

**Status:** evaluation complete; recommendation is to keep the custom `DecisionClient`/`JevDecisionClient` boundary and the `ai.decision.*` contract for now.

**Date:** 2026-11-16

**Evidence versions:** Spring AI `2.1.0-RC2`, Spring Boot `4.2.0-RC2`, TypeSafe Java SDK `0.4.0`, Java 25.

**Committed baseline:** `main` remains on Spring AI `2.0.2` / Spring Boot `4.1.2`. The RC2 prototype ran in a detached worktree and is not part of the default build.

Tracking: [#46](https://github.com/prasadgaikwad/spring-ai-apps/issues/46), parent [#36](https://github.com/prasadgaikwad/spring-ai-apps/issues/36).

## Executive summary

Spring AI 2.1's `DecisionModel` is a genuine typed-decision API, but it is not a lossless replacement for this demo's Choice/Score/Noul contract:

- It has `YesNoAnswer`, `ChoiceAnswer`, `ScoreAnswer`, and `RefusalAnswer`, but **no Noul answer**. The official `SystemOneDecisionMapper` turns a TypeSafe `NoulAnswer` into a `YesNoAnswer` carrying the probability; the Noul identity and reason are lost.
- Its `ScoreQuestion` is an ordered set of levels and its `ScoreAnswer` is a level/value with per-level probabilities. The local `ScoreAnswer` is an arbitrary numeric score with one probability; mapping requires inventing levels and changes semantics.
- Its `ChoiceAnswer` can carry a full per-option distribution and confidence. The local choice answer carries only the selected option and one probability, so a mapping either narrows the standard type or synthesizes values.
- Its response is a map keyed by question name and validates answer/question types. The local response is a flat list keyed by `questionId`, can contain duplicate IDs, and does not require an answer for every question.
- Its configuration is provider-specific and different: `ai.decision.*` owns base URL, key, model, and timeout for one WebClient adapter, while `SystemOneDecisionModel` is built from a manually supplied `TypeSafeClient`, and `OllamaDecisionModel`/`OpenAiDecisionModel` use their provider starters and properties.

Keep the custom adapter for the 2.1 upgrade. Any later replacement or wrapper should be a separate, explicit decision with a compatibility plan; it should not be slipped into the version bump.

## Comparison matrix

| Area | Current `ai-decision` | Spring AI 2.1 `DecisionModel` | Implication |
| --- | --- | --- | --- |
| Public surface | `POST /api/decision/decide`; `DecisionPrimitives` request/response records | No REST surface; `DecisionModel.call(DecisionPrompt)` returns `DecisionResponse` | The demo's public contract must stay; standard API use has to sit behind the existing `DecisionClient` seam. |
| State input | Arbitrary `Map<String,Object>` under `state` | `DecisionPrompt` accepts text, fields, conversation/messages, or content parts | An adapter can serialize state, but the input shape and type contract change. |
| Question types | `CHOICE`, `SCORE`, `NOUL` | `YesNoQuestion`, `ChoiceQuestion`, `ScoreQuestion` | There is no Noul question. Yes/no is new to the local contract. |
| Choice | Choice IDs and labels; answer has selected ID plus one probability | Options are a value-to-description map; answer has value, per-option probabilities, and confidence | Mapping is possible, but the local payload carries less information. |
| Score | Arbitrary numeric `score` plus one probability | Ordered levels; answer value plus per-level probabilities and confidence | Not directly representable. The adapter must choose level labels and an index/score convention. |
| Noul | `reason` plus probability; means no valid choice applies | No Noul type. Official System One mapping turns `NoulAnswer` into `YesNoAnswer(probability)` (verified in the spike) | Reason and Noul semantics are lost. This is the main blocker to replacement. |
| Refusal | No explicit local answer type | `RefusalAnswer` with a reason | Standard can express a refusal the local contract cannot. |
| Answer correlation | Flat list with `questionId`; duplicates and missing answers are not rejected by the DTOs | Map keyed by question name; the response builder validates answer type against the question | An adapter must enforce unique names and decide what to do with missing/duplicate questions. |
| Configuration | `ai.decision.*`: `provider`, `base-url`, `api-key`, `model`, `timeout` | `DecisionOptions` carries the model; provider starters/properties own auth and endpoint (`spring.ai.openai.decision.*`, `spring.ai.ollama.decision.*`); `SystemOneDecisionModel` needs a `TypeSafeClient` | Replacing the adapter changes the property contract and adds provider-specific dependencies. |
| HTTP/auth | One `WebClient`, bearer token, base URL, blocking timeout | Provider SDK clients; `TypeSafeClient` for System One with its own base URL, key, model, timeout, retry policy, and headers | Not a drop-in replacement; timeout/retry/auth behavior would change. |
| Error handling | Permissive parsing; missing values default; failures become generic exceptions | Standard mapper/model types and exceptions; response builder validates | Different error surface; needs explicit mapping and tests before adoption. |
| Test seam | `MockWebServer` contract tests around the HTTP adapter | Deterministic mapper test possible with a constructed `SystemOneResponse`; model calls need a mock/local provider | The standard API is testable without a vendor, but that does not preserve the local semantics. |
| Maturity | Stable committed path; upstream vendor contract unverified | New in Spring AI 2.1; tested line is `2.1.0-RC2`; no GA at evaluation time | Not a reason alone to defer, but it reinforces keeping the stable path for the version bump. |

## Prototype and evidence

The prototype used the official `SystemOneDecisionMapper.response(...)` with a constructed `SystemOneResponse`; no vendor call was made. It answered two questions:

1. Can representative Choice and Score values map into `DecisionResponse`? Yes, but only after choosing standard question shapes and probabilities.
2. What happens to Noul? The official mapper produces a `YesNoAnswer` with the Noul probability; there is no Noul type in the standard answer hierarchy.

A representative excerpt of the spike test (run in the detached RC2 worktree, not committed to `main`):

```java
class DecisionModelSpikeTests {

    @Test
    void choiceAndScoreMapOnlyAfterAnAdapterChoosesLevelsAndProbabilities() {
        DecisionPrompt prompt = DecisionPrompt.builder()
                .text("Classify the state")
                .question("color", ChoiceQuestion.builder()
                        .text("Which color?")
                        .option("red", "Red")
                        .option("blue", "Blue")
                        .build())
                .question("urgency", ScoreQuestion.builder()
                        .text("How urgent?")
                        .levels("low", "medium", "high")
                        .build())
                .build();

        SystemOneResponse upstream = new SystemOneResponse(
                "jev-test",
                Map.of(
                        "color", new org.springaicommunity.typesafe.response.ChoiceAnswer(
                                "red", Map.of("red", 0.7, "blue", 0.3), 0.7),
                        "urgency", new org.springaicommunity.typesafe.response.ScoreAnswer(
                                1.0, Map.of(), Map.of(0, 0.2, 1, 0.7, 2, 0.1), 0.7)),
                Usage.EMPTY);

        DecisionResponse response = SystemOneDecisionMapper.response(prompt, upstream);

        ChoiceAnswer color = response.getChoice("color");
        assertThat(color.getValue()).isEqualTo("red");
        assertThat(color.getProbability("red")).isEqualTo(0.7);

        ScoreAnswer urgency = response.getScore("urgency");
        assertThat(urgency.getValue()).isEqualTo(1.0);
        assertThat(urgency.getProbabilities()).containsExactly(0.2, 0.7, 0.1);
        assertThat(urgency.getNearestLevel()).isEqualTo(1);
    }

    @Test
    void noulBecomesAYesNoProbabilityAndLosesItsIdentity() {
        DecisionPrompt prompt = DecisionPrompt.builder()
                .text("Is this state complete?")
                .question("completeness", YesNoQuestion.of("Is the state complete?"))
                .build();

        DecisionResponse response = SystemOneDecisionMapper.response(prompt, new SystemOneResponse(
                "jev-test",
                Map.of("completeness", new org.springaicommunity.typesafe.response.NoulAnswer(0.62)),
                Usage.EMPTY));

        DecisionAnswer answer = response.getAnswer("completeness");
        assertThat(answer).isInstanceOf(YesNoAnswer.class);
        assertThat(((YesNoAnswer) answer).getProbability()).isEqualTo(0.62);
        assertThat(answer.getClass().getSimpleName()).doesNotContain("Noul");
    }
}
```

Observed result:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

The test proves the standard mapper's behavior, not a live Jev contract. It also shows why a mechanical `DecisionModel` wrapper would be misleading: Noul would be exposed to callers as a yes/no probability.

## Compatibility plan and recommendation

**Keep the custom adapter.** Do not replace `JevDecisionClient` or change the REST/property contract as part of the Spring AI 2.1 upgrade.

Required current contract:

- `POST /api/decision/decide` stays available with the same request/response shape.
- `DecisionPrimitives` Choice/Score/Noul records remain the public DTOs.
- `ai.decision.*` remains the configuration contract: `provider` remains metadata-only; `base-url`, `api-key`, `model`, and `timeout` remain the adapter settings.
- The existing deterministic `MockWebServer` tests remain the default test seam; no live vendor call is added to CI.

If the standard API is reconsidered at GA, the minimum preconditions are:

- [ ] A lossless local mapping for Noul (or an explicit public contract change that removes/replaces Noul).
- [ ] A defined Score mapping, including whether the local `score` is a level index or a real value and where levels come from.
- [ ] A choice mapping that states whether a missing per-option distribution is acceptable.
- [ ] Unique question IDs/names, missing-answer behavior, and duplicate handling are specified.
- [ ] `ai.decision.*` compatibility is preserved, or a new opt-in property/profile is introduced without changing existing callers.
- [ ] Error, timeout, and retry behavior is mapped to the shared error contract.
- [ ] Tests cover the public REST path and the adapter, not only the standard mapper.
- [ ] ADR-0003 is amended or superseded with the new decision.

A future wrapper could be an opt-in `DecisionClient` implementation backed by `SystemOneDecisionModel`, but only if it can preserve the local semantics above. It should not become the default merely because the standard API exists.

## Documentation updates

- [ADR-0003](../adr/0003-decision-model-integration.md) records the evaluation and the keep-custom decision.
- [ai-decision guide](../../ai-decision/README.md) links to this evaluation.
- [Architecture verification matrix](../architecture.md) now records the existing MockWebServer coverage instead of claiming that only DTO/context tests exist.
- The upgrade plan corrects the System One dependency/auto-configuration description and links this evaluation.

## References

- [Spring AI 2.1 Decision Models](https://docs.spring.io/spring-ai/reference/2.1/api/decision.html)
- [Spring AI 2.1 System One Decision Models](https://docs.spring.io/spring-ai/reference/2.1/api/decision/systemone-decision.html)
- [Spring AI 2.1 upgrade notes](https://docs.spring.io/spring-ai/reference/2.1/upgrade-notes.html)
- [JevDecisionClient](../../ai-decision/src/main/java/dev/prasadgaikwad/springaiapps/decision/JevDecisionClient.java)
- [DecisionPrimitives](../../ai-decision/src/main/java/dev/prasadgaikwad/springaiapps/decision/DecisionPrimitives.java)
