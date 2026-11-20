# Documentation audit for issue #28 (frozen snapshot)

> This is a point-in-time record of the issue-#28 documentation pass, not a live verification contract. Claims superseded after #28 are listed in [Post-#28 status (issue #55)](#post-28-status-issue-55); the original audit content and its historical validation figures are retained for provenance.

## Scope and evidence

This review covers all tracked Markdown documents, the eight Maven modules, main Java sources, YAML configuration, tests, Compose, environment samples, and the two Thymeleaf clients. Documentation describes the implementation currently checked in; it does not certify any live model/vendor API. No feature behavior is changed by this audit.

| Document surface | Source cross-check | Result |
| --- | --- | --- |
| Root README | Parent POM, module POMs, YAML, Compose | Correct module count/provider scope, runnable Maven commands, environment loading, infrastructure prerequisites, navigation/context diagram |
| `CONTEXT.md` | Module roles and shared/local DTO vocabulary | Seven demos; definitions separated from wiring; probability calibration claims qualified; misplaced Tool avoidance note fixed |
| `AGENTS.md` | Current module/test/config patterns | Replace scaffold-only status, centralize reading pointers, update workflow/testing caveats |
| Original spec | Parent/module source and completed feature paths | Preserve original user stories; prepend current implementation/status matrix and scope qualifications |
| `ai-common/README.md` | Config/error classes and all Eval types/tests | Correct one-argument factory example, aliases, summary limits, error shape, class diagram |
| `ai-rag/README.md` | Controllers, ingestion, retriever, reranker, config, templates, tests | Actual route names, ETL/query sequences, defaults, ID lifecycle, profile/dimension limits |
| `ai-agents/README.md` | Controller/service/config/tools, MCP config/POM, template/tests | Separate tool/MCP paths, exact routes, publication/connection defaults, transport test limits |
| `ai-eval/README.md` | Controller/service/specs/reports/tests | Payload example, blank-answer generation, positional regressions, memory lifecycle, flow |
| `ai-observability/README.md` | Controller, handler, repository, schema/config/tests | Correct file path/filters/order, schema ownership, name-based IDs, timestamps/status, diagrams |
| `ai-multimodal/README.md` | Controller/service/DTOs/config/tests | Per-capability selection, exact request/response fields, ignored options, base64 and test scope |
| `ai-structured/README.md` | Controller/dispatch/conversion/records/validation/tests | Exact targets/fields, HTTP vs validation success, diagrams, live-provider qualifications |
| `ai-decision/README.md` | Properties/config/controller/DTOs/client/tests | Exact adapter assumptions/parser fallbacks, environment precedence, validation/test gaps, diagrams |
| ADRs 0001–0005 | Existing boundaries and implementation | Correct scope/defaults, distinguish decisions from shortcuts; remove stray EOF artifact; add index |
| New ADRs 0006–0007 | Existing hybrid RAG and structured validation implementations | Record rationale/trade-offs for choices already delivered |
| Foundation learnings | Current build/config/test behavior | Mark historical scope, qualify auto-config claim, correct run and DB test setup |
| Implementation learnings | All implemented flows above | Add module-by-module lessons and verification boundaries |
| `samples/rag-overview.md` | TextReader/classpath ingestion | Retained as an ingestion fixture: its concise generic RAG content is still valid, not an API guide |

## Diagram coverage

- System context and build dependency maps: [architecture](architecture.md) and root README.
- Shared Eval class model: [ai-common](../ai-common/README.md).
- RAG ingestion flow and query sequence: [ai-rag](../ai-rag/README.md).
- Local tools/MCP alternative sequence: [ai-agents](../ai-agents/README.md).
- Evaluation generation/grading/history flow: [ai-eval](../ai-eval/README.md).
- Observation capture/persistence flow and classes: [ai-observability](../ai-observability/README.md).
- Four media-capability routes: [ai-multimodal](../ai-multimodal/README.md).
- Structured conversion/validation sequence and types: [ai-structured](../ai-structured/README.md).
- Decision HTTP sequence and client/answer types: [ai-decision](../ai-decision/README.md).

## Gaps recorded rather than silently promoted to guarantees

Anthropic RAG embedding selection, schema/dimension migrations, obsolete-chunk cleanup, MCP peer connection (both client and server starters are present but disabled by default), real tool execution, durable Eval history, multimodal option wiring, accurate automatic span timing/correlation, and live provider verification remain implementation gaps or operational qualifications. Decision request/response mapping is covered by MockWebServer tests against an assumed Jev contract; live vendor semantics and probability calibration remain unverified. They are documented where they affect a caller. This ticket changes the documentation, not those feature contracts.

## Validation approach

Check local Markdown links and balanced fenced blocks, compare endpoint/DTO/property claims to code, check Mermaid syntax/renderability where tooling is available, review the full diff, and run the reactor tests to establish the current application's baseline. Report infrastructure blockers distinctly from passing tests. Live calls are not part of this documentation audit.

## Completed validation (2026-11-02)

- `mvn test`: **BUILD SUCCESS** across the parent and eight modules; Surefire reports total **87 tests, 0 failures, 0 errors, 0 skipped**, including PGVector Testcontainers tests.
- All **25 Markdown files** scanned: **103 local link targets** resolve and code fences are balanced.
- All **16 Mermaid blocks** parse successfully with Mermaid **12.2.0**. This is syntax validation, not a visual review of GitHub's renderer.
- `git diff --check`: passed.
- Two independent review passes checked documented standards/source accuracy and issue acceptance coverage. Corrected regex case-sensitivity wording, RAG test-double scope, the root calibration claim, the hybrid-search representation claim, and the root context diagram. A final POM cross-check corrected the agents MCP client-starter claim and clarified Actuator versus explicit trace-query routes.

No live AI-provider calls were made. The tests establish the existing application baseline and the documented testing seams, not upstream interoperability.

## Post-#28 status (issue #55)

This section supersedes claims in the frozen snapshot above; the original text and validation figures are not rewritten.

- **Decision adapter verification**: `ai-decision/src/test/java/dev/prasadgaikwad/springaiapps/decision/JevDecisionClientContractTests.java` uses MockWebServer to verify request mapping, response aliases/fallbacks, blank/missing payloads, non-2xx, invalid JSON, and timeout against an **assumed** Jev contract. Live vendor semantics and probability calibration remain unverified. Adapter HTTP/mapping tests are not absent; a REST success/failure seam with a stubbed `DecisionClient` is still missing.
- **Decision adapter hardening (#54)**: subsequent work replaced the "still missing" status above. `DecisionControllerTests` now stubs `DecisionClient` for success and 502/504 mapping; the adapter rejects malformed identity/types/numbers, preserves base-URL paths, omits blank authorization, and maps provider failures to shared upstream errors. Mock tests pin the assumed contract only; live vendor semantics remain unverified.
- **RAG retrieval seams (#53)**: pure RRF/reranker unit tests cover fusion, ties, dedup, precedence, limits, and lexical coverage. Docker-backed tests tagged `docker` include a multi-chunk Testcontainers case for order, idempotent upsert, and source propagation; they are excluded from the no-Docker command and are not live-provider validation.
- **Eval history**: bounded by `ai.eval.max-runs` (environment override `AI_EVAL_MAX_RUNS`), synchronized, and process-local, with default `100` in both source and YAML. It evicts oldest runs and is not durable. Blank answers use bounded parallelism via `ai.eval.generation-concurrency` (default `4` in source and YAML) and are restored to input order before grading.
- **MCP client starter**: present in `ai-agents/pom.xml`; server and client are disabled by default in `application.yml`, and no peer connection is configured.
- **Version pins at that time**: root `pom.xml` pinned Boot `4.1.2` / Spring AI `2.0.2`; ADR-0002 records Boot `4.1.1` / Spring AI `2.0.1` as the original baseline. Superseded by the GA pins below.
- **Upgrade planning at that time**: the [upgrade note](upgrades/spring-boot-4.2-spring-ai-2.1.md) no longer conditions streaming tests on an unmerged branch and treats all seven REST demos as the springdoc gate. It now records the completed GA upgrade.
- **API and navigation**: [architecture](architecture.md) lists `DELETE /api/eval/runs`; the root [README](../README.md) links the upgrade plan, spikes #45/#46, plan #47, and the [2026-11-17 application review](application-review-2026-11-17.md) with status labels.

## Post-#57 status

This section records the P3 cleanup tranche; the frozen validation figures above are not rewritten.

- **Dependency boundaries**: `ai-common` depends on the Spring AI model API and web infrastructure only. The six chat demos declare their own provider starters; `ai-decision` has no `ChatModel` beans, asserted by `DecisionControllerTests`. DuckDB JDBC `1.1.3` and MockWebServer `4.12.0` are pinned in root `dependencyManagement`.
- **CI checks**: Enforcer rules (Java/Maven versions and dependency convergence) run through `validate`; `dependency:analyze` is a non-failing report because Boot starters intentionally expose transitive APIs.
- **Eval generation**: blank answers use a per-run fixed pool capped by `ai.eval.generation-concurrency` (default `4` in source and YAML); `EvalRunServiceConcurrencyTest` verifies the cap and input-order preservation with a mocked ChatModel.
- **Observation lifecycle**: `DuckDbObservationHandler` uses weak-key state and a stop-without-start regression test. `TraceSpanSink` is an async single writer; REST writes flush before responding and shutdown flushes too. The redundant read-after-write is gone, service name comes from `ai.observability.service-name`, and the inert `management.tracing.*` keys were removed from YAML/README.
- **DuckDB lifecycle**: the default URL is under `${user.home}/.spring-ai-apps/ai-observability/`, `trace.duckdb.retention-days` defaults to `7`, and pruning runs at startup and after flushes. There is still no migration framework; schema changes remain manual.
- **Environment sample**: empty cloud/Jev keys are commented out, with commented eval/observability/port tunables and a warning that blank Jev keys are logged at startup and send no Authorization header.

These changes are local mock/infrastructure verification only. No live provider, Jev, or tracing backend behavior is established.

## Post-#36 status (GA upgrade)

This section supersedes the earlier pin and planning notes; the frozen validation figures above are not rewritten.

- **Version pins**: root `pom.xml` pins Boot `4.2.0` / Spring AI `2.1.0`, with `springdoc` unchanged at `3.2.0`, Testcontainers at `2.0.3`, and Java 25. ADR-0002 keeps `4.1.1` / `2.0.1` only as the original baseline.
- **Upgrade record**: [Spring Boot 4.2 / Spring AI 2.1](upgrades/spring-boot-4.2-spring-ai-2.1.md) changed from an RC plan to a GA upgrade record. Parent and BOM moved together; Enforcer convergence and compile passed with no suppressions.
- **springdoc gate**: app-context tests in all seven modules now assert both `/v3/api-docs` and `/swagger-ui/index.html`. They pass on Boot 4.2 / Framework 7.1 with springdoc `3.2.0`; no dependency bump was needed.
- **Dependency tree**: Spring AI `2.1.0` resolves MCP SDK `2.2.0` and Anthropic SDK `3.8.0`. `spring-ai-systemone` and `spring-ai-autoconfigure-model-chat-memory` remain on the six chat demos; `ai-decision` still has no `ChatModel` beans.
- **Unchanged boundaries**: `DecisionClient`/`ai.decision.*`, disabled MCP defaults, and the #47 no-session-migration decision were not altered. The `getDefaultOptions()` stubs touched by the upgrade now use `getOptions()`.
- **Verification limits**: the no-Docker reactor tests pass locally (176 tests); PGVector/Testcontainers coverage passed in CI run `44836550600` (`docker-integration`). Live OpenAI/Anthropic/Ollama and Jev calls, MCP peer connections, and modern MCP adoption remain untested or out of scope.
