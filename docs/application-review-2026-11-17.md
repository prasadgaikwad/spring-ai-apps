# Application review backlog — 2026-11-17

> Point-in-time review snapshot at `main@2b4c984`. Update or archive this file after the findings below are addressed; do not treat it as a live contract.

## Review baseline

Reviewed `main` at `2b4c984`; working tree clean. I read source, tests, POMs, YAML, docs, and ADRs; ran `mvn -DskipTests compile` successfully. Docker is unavailable here, so PGVector Testcontainers tests could not run. No live provider calls were made.

**No P0 / build-breaking issue found.** The main risks are shared error handling, input-validation gaps that become 500s, several unverified core algorithms, stale verification claims in docs, and missing CI/security hardening.

---

## P1 — fix before treating the demos as stable

| # | Area | Finding | Evidence | Recommended action |
|---|---|---|---|---|
| 1 | All REST modules | Catch-all handler maps framework/MVC errors such as bad query params and malformed JSON to `500`, not `400`, and returns raw `ex.getMessage()` to clients. Its Javadoc claims no internals are leaked. | `ai-common/src/main/java/dev/prasadgaikwad/springaiapps/common/ApiExceptionHandler.java:13-14,31-35` | Extend `ResponseEntityExceptionHandler`, add explicit 4xx handlers, and return a fixed safe message for 500 while logging the cause. |
| 2 | Cross-cutting security | No authentication, authorization, CORS allowlist, or rate limiting. Springdoc is enabled on all modules; actuator exposes metrics/traces. Acceptable for localhost demos, risky if deployed. | No `spring-security`/`SecurityFilterChain`/`@CrossOrigin` found; `ai-agents/src/main/resources/application.yml:27-31`; `ai-observability/src/main/resources/application.yml:15-26` | Add an explicit “local-only” warning, an opt-in security profile, restrict actuator exposure, and gate springdoc by profile. Rate-limit model-backed POSTs. |
| 3 | `ai-rag` ingestion | `text`/`source` are documented as mutually exclusive but both are accepted with `text` silently winning; neither supplied leads to `resourceLoader.getResource(null)` and a generic 500. | `ai-rag/src/main/java/dev/prasadgaikwad/springaiapps/rag/IngestController.java:18-19,36-44`; `IngestRequest.java:6-12` | Validate exactly one of `text`/`source`, reject blanks with `BadRequestException`, add REST tests for both/neither/blank. |
| 4 | `ai-rag` core logic | RRF fusion, deduplication, semantic-metadata precedence, and lexical reranking are not covered by focused tests. The fixture yields one chunk, so the integration test never exercises competing candidates. Docker is unavailable locally, so existing PGVector tests could not be validated here. | `RagConfig.java:41-43,50-53`; `HybridDocumentRetriever.java:87-108`; `LexicalReranker.java:47-64`; `RagQueryIntegrationTests.java:42-46,64-83` | Add pure unit tests for rank fusion, ties, overlap dedup, semantic metadata, stopword/coverage ordering, and `max-documents`; add a multi-chunk integration test when Docker is available. |
| 5 | `ai-decision` state | JSON `null` values in `state` become `Map.copyOf` NPEs and generic 500s. | `DecisionController.java:34-36`; `DecisionPrimitives.java:13-19` | Explicitly reject null values with 400 or use a null-tolerant immutable copy; add REST test. |
| 6 | `ai-decision` questions | Only non-empty `questions` is checked. Null/blank IDs/prompts, null types, and empty choices for `CHOICE` are sent upstream. | `DecisionController.java:30-38`; `DecisionPrimitives.java:23-31` | Validate each question by type; return 400 with field-level messages. |
| 7 | `ai-decision` answers | Blank bodies, non-array `answers`, or missing `data.answers` return HTTP 200 with an empty answer list, making contract drift indistinguishable from “no decisions”. | `JevDecisionClient.java:54-82`; `JevDecisionClientContractTests.java:130-138` | Treat missing/unrecognized containers as provider errors while retaining the documented fallback; update the test. |
| 8 | Docs/verification credibility | Several docs still deny tests that now exist or describe fixed behavior as a gap: no adapter HTTP tests, unbounded eval history, no MCP client starter. | `docs/adr/0003-decision-model-integration.md:16,18`; `docs/learnings/implementation-learnings.md:25,66`; `docs/0001-spring-ai-apps-core-and-ai-rag-spec.md:11`; `docs/documentation-audit.md:41` vs `JevDecisionClientContractTests.java:50-164`, `EvalRunService.java:34,80,130-141`, `ai-agents/pom.xml:43-50` | Reconcile to: mock HTTP tests verify an assumed contract; live vendor/Jev semantics remain unverified. Eval history is bounded/synchronized. MCP client starter is present but disabled. |
| 9 | CI/ops | No CI workflow, no Maven wrapper, no Java-version/dependency-convergence enforcement. | No `.github/` or `mvnw` at root; `README.md:26-34` documents only manual commands | Add Maven wrapper and CI running Java 25 compile + non-Docker tests; report Docker-dependent tests explicitly. Add enforcer for Java/convergence. |
| 10 | Build/security | Dual Jackson majors (`com.fasterxml.jackson...2.21.7` and `tools.jackson...3.1.7`) with source importing Jackson 2 transitively via springdoc/OpenAI starter; dependencies are not declared directly. | Verified with `mvn dependency:tree`; `JevDecisionClient.java:10-12,30`; `TraceSpan.java:7-9`; `TraceSpanRepository.java:20-21` | Declare Jackson 2 explicitly where used or migrate to Boot-managed Jackson 3; add `dependency:analyze` to CI. |

---

## P2 — correctness, robustness, and API contract gaps

### `ai-eval`

| Finding | Evidence | Action |
|---|---|---|
| Null elements in `pairs` or `criteria` cause NPE → 500; only the lists themselves are checked. | `EvalRunService.java:59-77` | Null-check each element and throw 400. |
| Model/network/auth failures during answer generation are mapped to `BadRequestException` (400) and include raw provider messages. | `EvalRunService.java:143-153` | Map upstream failures to 502/504 with sanitized messages. |
| `GET /runs/{runId}` always returns an empty regression list; stored runs cannot later show what regressed. | `EvalRunService.java:106-112` | Persist or recompute the diff. |
| Case IDs are positional (`case-0`, `case-1`), so reordering inputs changes regression identity. | `EvalRunService.java:75` | Derive stable IDs from content or allow caller-supplied IDs. |
| Missing focused tests: invalid regex, unknown criterion type, blank question, null elements, generation failure, GET success/404. | `EvalRunIntegrationTests.java` | Add REST tests for each path. |

### `ai-observability`

| Finding | Evidence | Action |
|---|---|---|
| Null tag values from JSON or Micrometer cause `Map.copyOf` NPE → 500. | `ObservabilityController.java:42-45`; `TraceSpanRepository.java:43-50,74`; `TraceSpan.java:32` | Filter null keys/values before copy/serialize; add REST test. |
| Malformed tag JSON is silently replaced with `{}` with no log. | `TraceSpan.java:39-48` | Log at WARN or surface parse status. |
| Observation state is held in an `IdentityHashMap` and only removed on `onStop`; an observation that starts but never stops leaks the context. | `DuckDbObservationHandler.java:35-36,43-49,51-60` | Use weak keys or add stale-state cleanup. |
| No retention, size cap, partitioning, or sampling; every observation is written synchronously. | `TraceSpanRepository.java:47-75`; `DuckDbConfig.java:40-64`; `DuckDbObservationHandler.java:74-77` | Add configurable retention/sampling and document capacity. |
| Micrometer tracing config is enabled but there is no W3C bridge; stored spans use local UUIDs, so `/actuator/traces` cannot correlate with DuckDB spans. | `application.yml:23-26`; `DuckDbObservationHandler.java:45,48,57-59` | Bridge trace context or remove/disable the inert tracing settings. |
| Trace/service attribution is hardcoded; configured service key is ignored. | `TraceSpanRepository.java:66`; `application.yml:21-22` | Read service name from configuration. |
| Save performs a redundant read-after-write query. | `TraceSpanRepository.java:74` | Build the returned span from values already held. |
| Missing tests: null tags, `limit` clamping, trace grouping/ordering, stop-without-start, concurrent handler use. | `ai-observability/src/test/...` | Add targeted unit/integration tests. |

### `ai-rag`

| Finding | Evidence | Action |
|---|---|---|
| Semantic leg has no similarity threshold, so up to `topK=10` chunks are accepted regardless of similarity. | `RagConfig.java:50-53`; `application.yml:35` | Expose/document `spring.ai.rag.retrieval.similarity-threshold`. |
| `openai` profile changes embedding model but leaves `dimensions: 768`; a default `text-embedding-3-small` dimension change would make stored/query vectors incompatible. Vendor dimension is not verified locally. | `application.yml:21,50-52`; `.env.example:12` | Set dimensions explicitly or fail fast; document required rebuild/re-ingestion. |
| Keyword SQL interpolates `tableName` raw and ignores configured schema name. | `HybridDocumentRetriever.java:70-79`; `RagConfig.java:48`; `application.yml:35` | Validate/quote identifiers and honor schema-qualified names. |
| Empty model output is unguarded; `getOutput().getText()` can NPE or yield null answer. | `RagQueryService.java:31-40` | Map empty output to an explicit no-answer/error response. |

### `ai-decision`

| Finding | Evidence | Action |
|---|---|---|
| Answers are not reconciled with requested questions; missing, unknown, and duplicate `questionId`s are accepted; missing IDs become `""`. | `JevDecisionClient.java:64-82` | Validate answer IDs against request IDs; reject duplicates. |
| Probabilities/scores are not range-checked; missing values silently become `1.0`/`0.0`. Javadoc says “calibrated probabilities” while glossary/README say calibration is not established. | `DecisionPrimitives.java:7-9,47-53`; `CONTEXT.md:71`; `ai-decision/README.md:60` | Validate ranges or document raw semantics; correct Javadoc. |
| Blank API key likely emits literal `Authorization: null`; test tolerates it. | `JevDecisionClient.java:43-45`; `JevDecisionClientContractTests.java:84-93` | Add header only when key is non-blank; assert absence. |
| `ai.decision.provider` is inert but any value is silently accepted. | `DecisionProperties.java:12`; `application.yml:7`; `ADR-0003:15` | Validate supported values at startup or remove the property. |
| Upstream non-2xx, timeout, and parse failures become generic 500 with raw WebClient details. | `JevDecisionClient.java:41-51`; `ApiExceptionHandler.java:31-35` | Dedicated exception types with 502/504 mapping and sanitized messages. |
| No REST-level success/failure test with a stubbed `DecisionClient`; MockWebServer tests cover only the adapter. | `DecisionControllerTests.java:21-45` | Add `@MockitoBean DecisionClient` tests. |
| Timeout bounds only `.block(...)`; no Reactor connect/response timeout, no validation that timeout is positive; base URL path is discarded by relative `/v1/systemone`. | `DecisionConfig.java:14-19`; `JevDecisionClient.java:41-49`; `DecisionProperties.java:28-30` | Configure HTTP-level timeouts, validate config, and preserve base path. |

### `ai-agents`

| Finding | Evidence | Action |
|---|---|---|
| Configured default is `STREAMABLE`, but the real MCP handshake test uses legacy SSE; `/mcp` has no integration test. | `application.yml:19`; `McpClientProtocolIntegrationTests.java:36-40,77`; `docs/architecture.md:128` | Add a STREAMABLE `/mcp` test; keep SSE as compatibility baseline. |
| `toolCallbackProviders.getIfAvailable()` fails when more than one provider exists instead of aggregating tools. | `AgentService.java:75-81`; `ai-agents/README.md:103` | Stream/flatten all providers and dedupe by tool name; add a two-provider test. |
| Mocked model tests never return a tool call, so tool registration/routing can be broken while tests pass. | `AgentIntegrationTests.java:43-57` | Add deterministic tool-call → final-answer tests for local and MCP paths. |
| `content()` checks objects but not text, so HTTP 200 with `answer:null` is possible; streaming has no empty-stream guard. | `AgentService.java:89-94,62-65`; `AgentController.java:55-57` | Require `StringUtils.hasText(text)`; emit explicit empty/error stream event. |
| SSE `error` data is raw provider text; no machine-readable code and no defined terminal event after `error`. | `AgentController.java:53-61`; `AgentStreamingIntegrationTests.java:81-95` | Define `error` as terminal or always emit `done`; use structured safe payload and assert exact event order. |
| `/tools` names come from a hand-maintained list, not actual `@Tool` metadata. | `CalculatorTools.java:33-67`; `AgentController.java:64-68`; `AgentIntegrationTests.java:68-75` | Derive catalog from callbacks/annotations. |
| MCP tool description says “shared Spring AI glossary” but implementation is a fixed switch. | `KnowledgeMcpTools.java:18-30`; `ai-agents/README.md:73` | Reword to “small fixed glossary” or actually read `CONTEXT.md`. |

### `ai-multimodal`

| Finding | Evidence | Action |
|---|---|---|
| Image/TTS/transcription availability is based only on bean presence. Cloud media beans can exist under Ollama/Anthropic profiles with placeholder keys. | `MultimodalService.java:49-61,166-173`; `README.md:13-15,60` | Add explicit active-profile capability guard; test missing-bean vs placeholder-key cases separately. |
| Raw base64 inputs are decoded without size limits; MIME type is not checked against bytes. | `VisionRequest.java:7`; `TranscriptionRequest.java:8`; `MultimodalService.java:97,158,270-276` | Enforce configured decoded-byte limits and HTTP body limits. |
| `format` is arbitrary and used to build `"audio/" + format`, despite documented supported values. | `MultimodalService.java:123-140,237-250`; `README.md:58` | Validate against documented set; add invalid-format tests. |
| Image response can be HTTP 200 with both `url` and `b64Json` null. | `MultimodalService.java:82-88`; `GeneratedImage.java:3-6` | Require at least one payload field. |
| Vision provider failures become 400 while image/speech/transcription failures can become 500. | `MultimodalService.java:103-115,82,142,159` | Normalize to dedicated upstream error codes. |

### `ai-structured`

| Finding | Evidence | Action |
|---|---|---|
| 3-arg `ExtractedResult` constructor accepts caller-supplied `valid`, so `valid=true` with violations is constructible despite Javadoc. | `ExtractedResult.java:8-19`; `ExtractionService.java:57` | Derive `valid = violations.isEmpty()` in canonical constructor or remove the 3-arg constructor. |
| `JobHistory` does not enforce `endYear >= startYear`. | `CandidateProfile.java:27-31`; `README.md:20` | Add validation constraint and focused test. |
| `CustomerOrder` Javadoc says “line items” but record has one product/quantity. | `CustomerOrder.java:5-14`; `README.md:23` | Correct Javadoc. |
| Source comment overstates strict schema guarantee; guide/ADR say provider support is not established. | `ExtractionService.java:18-21`; `README.md:60`; `ADR-0007:15` | Soften wording to “schema guidance may be attached”. |

---

## P3 — optimizations and cleanup

| Finding | Evidence | Action |
|---|---|---|
| `eval` answer generation is sequential for every blank answer. | `EvalRunService.java:64-77` | Consider bounded parallel generation preserving order. |
| Observation handler can retain un-stopped observations. | `DuckDbObservationHandler.java:35-36` | Weak keys or stale-state sweep. |
| DuckDB writes are synchronous and unbounded. | `TraceSpanRepository.java:47-75` | Batch/async writes, TTL/rotation. |
| Redundant `findById` after every span insert. | `TraceSpanRepository.java:74` | Return span built from saved values. |
| Inert Micrometer tracing configuration may mislead operators. | `ai-observability/application.yml:23-26` | Remove or implement bridge. |
| `ai-common` POM description claims prompt templates that are not delivered. | `ai-common/pom.xml:15`; `ai-common/README.md:14`; `ADR-0002:11` | Reword or mark planned. |
| `ADR-0005` says errors are copied into tags; implementation stores only exception simple name. | `ADR-0005:12`; `DuckDbObservationHandler.java:65-68` | Clarify ADR. |
| Dead imports in `DuckDbConfig`. | `DuckDbConfig.java:4,8-11` | Remove. |
| `ai-common` forces `spring-ai-starter-model-ollama` onto all demos, including WebClient-only `ai-decision`. | `ai-common/pom.xml:26-29`; dependency tree confirms | Depend on API artifacts only; put starter in chat demos. |
| Leaf POMs hardcode DuckDB/MockWebServer versions; no enforcer. | `ai-observability/pom.xml:38`; `ai-decision/pom.xml:47`; root `pom.xml:33-39` | Centralize versions; add enforcer/dependency analysis. |
| `docker-compose.yml` binds PostgreSQL to all interfaces with `postgres/postgres` and no restart policy. | `docker-compose.yml:5-17` | Bind to `127.0.0.1`, env-driven password, `restart: unless-stopped`. |
| `.env.example` exports empty provider keys, which override YAML placeholders and can break zero-key startup. | `.env.example:2-3`; base YAML `${OPENAI_API_KEY:not-configured}`; `docs/learnings/implementation-learnings.md:8` | Comment out empty keys and add clear startup validation. |
| DuckDB service name hardcoded; DB path is CWD-relative; no retention/migrations. | `TraceSpanRepository.java:66`; `DuckDbConfig.java:30-38`; `application.yml:30` | Source from config, use stable path, add retention/migration. |
| No `LICENSE`/`NOTICE` despite README referring to license information. | `README.md:103-104`; no license file at root | Add a license file and link it. |
| JEV key aliases in sample make explicitly empty `JEV_API_KEY` stop fallthrough. | `application.yml:9`; `.env.example:21-24`; `ai-decision/README.md:32` | Simplify to one key or warn on blank active key. |

---

## Documentation backlog

| Severity | Finding | Evidence | Action |
|---|---|---|---|
| High | ADR-0003/learnings/spec/audit contradict current decision test coverage. | `ADR-0003:16,18`; `learnings:66`; `docs/0001:16`; `documentation-audit.md:41` vs `JevDecisionClientContractTests.java:50-164` | Standardize on “deterministic mock-HTTP verifies assumed contract; live vendor unverified”. |
| High | ADR-0004/learnings/audit call eval history unbounded/unsynchronized; code is bounded and synchronized. | `ADR-0004:21`; `learnings:25`; `EvalRunService.java:34,80,130-141`; `architecture.md:115` | Correct to “bounded, synchronized, process-local; not durable”. |
| Medium | Spec/audit say no MCP client starter; both starters are present but disabled. | `docs/0001:11`; `documentation-audit.md:41`; `ai-agents/pom.xml:43-50`; `application.yml:14-25` | Update wording. |
| Medium | ADR-0002 pins old versions. | `ADR-0002:9` says Boot `4.1.1`/Spring AI `2.0.1`; root `pom.xml` pins `4.1.2`/`2.0.2` | Update or mark original baseline. |
| Medium | Upgrade plan has stale branch-era wording: “if #37 is merged”, “six REST demos”, “all six demos”. | `docs/upgrades/spring-boot-4.2-spring-ai-2.1.md:79,85,130-131,131` | Streaming is merged; there are seven demos. |
| Medium | Architecture API table omits `DELETE /api/eval/runs`. | `architecture.md:95-103,115`; `EvalController.java:53-57` | Add route. |
| Medium | Original spec still reads as current for six demos and two ADRs. | `docs/0001:76,87,89-91,95,102` | Label those sections as original foundation milestone or inline current-state notes. |
| Medium | Root README has no central navigation to upgrade plan, spikes #45/#46, or plan #47. | `README.md:81-89`; new docs under `docs/upgrades`, `docs/spikes`, `docs/plans` | Add “Spikes, upgrades, and plans” section with status labels. |
| Low | Documentation audit is a frozen issue-#28 snapshot but is linked as current. | `documentation-audit.md:1,5,41,47-51`; `README.md:88` | Retitle or add post-#28 status section. |
| Low | Trace-order wording conflicts. | `ai-observability/README.md:32` vs `learnings:58`; `TraceSpanRepository.java:108-117` | Use “descending by each trace’s earliest/minimum start time”. |
| Low | `.env.example` omits documented tunables. | `.env.example:1-25`; `ai-eval/README.md:50`; `ai-observability/README.md:12` | Add commented `AI_EVAL_MAX_RUNS`, `TRACE_DUCKDB_URL`, `SERVER_PORT`. |
| Low | `ai-rag` lacks explicit springdoc block while other modules have one. | `ai-rag/application.yml` vs `ai-agents/application.yml:27-31` | Add explicit config and a disable property. |
| Low | `ApiExceptionHandler` Javadoc contradicts 500 body behavior. | `ApiExceptionHandler.java:13-14,31-35`; `architecture.md:107` | Align Javadoc with actual behavior until handler is fixed. |
| Low | Unverified external claim: “OpenAI model does inspect filenames” presented as fact. | `ai-multimodal/README.md:59`; `MultimodalIntegrationTests.java:220-233` | Rephrase as provider-dependent/unverified. |
| Low | MCP test-coverage claim overstated for newly added option wiring. | `documentation-audit.md:41`; `MultimodalIntegrationTests.java:71-127,161-198,220-267`; `architecture.md:131` | Distinguish local option-mapping coverage from provider support. |

---

## Verification boundaries

**Verified locally**

- Compile is green on Java 25: `mvn -DskipTests compile`.
- Source/test/doc cross-checks above.
- Dependency tree contains both Jackson 2 and Jackson 3.
- Docker is unavailable, so RAG Testcontainers tests could not be run.

**Not verified / should remain labeled as assumptions**

- Live Jev endpoint semantics, Noul/Score calibration, and provider error formats.
- OpenAI embedding output dimension for `text-embedding-3-small`.
- Live Ollama/OpenAI/Anthropic tool calling, streaming, media support, and structured-output strictness.
- Springdoc `3.2.0` compatibility with future Boot 4.2 / Spring Framework 7.1.
- Actual startup failure when cloud keys are exported empty; documented as a known pitfall but not re-run.
- Exploitability of unauthenticated endpoints depends on deployment topology.

---

## Recommended next tranche

1. **Error taxonomy first**: fix `ApiExceptionHandler`, add upstream/timeout exception types, and sanitize 500 bodies. This affects every module.
2. **Input-to-500 bugs**: RAG ingest validation, decision `Map.copyOf` nulls, decision question validation, observability null tags, eval null elements.
3. **Core RAG confidence**: pure unit tests for RRF/dedup/rerank and a multi-chunk integration test when Docker is available.
4. **Decision contract**: reject empty/unrecognized answer containers, validate answer IDs, tighten calibration wording.
5. **Docs truth pass**: reconcile ADR-0003/0004/0005, learnings, original spec, audit, upgrade plan, and root navigation.
6. **CI/security decision**: add Maven wrapper + CI, choose local-only vs deployable posture, and add LICENSE.
7. **Then optimizations**: bounded DuckDB retention, observation state cleanup, profile-scoped beans, and dependency hygiene.
