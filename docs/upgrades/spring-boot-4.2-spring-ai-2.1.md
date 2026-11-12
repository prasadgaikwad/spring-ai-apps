# Spring Boot 4.2 and Spring AI 2.1 upgrade plan

**Status:** planning/spike only. The parent still pins the stable line. Do not change `main` to a release candidate.

Tracking issue: [#36](https://github.com/prasadgaikwad/spring-ai-apps/issues/36).

## Release status

Snapshot taken **2026-11-11** from Maven Central metadata and the project release notes:

| Component | Current stable pin | Latest available | Notes |
| --- | --- | --- | --- |
| Spring Boot | `4.1.2` | `4.2.0-RC2` | `4.2.0` is still a release-candidate line. The project targets a November 2026 release, but no GA artifact exists yet. |
| Spring AI | `2.0.2` | `2.1.0-RC2` | `2.1.0` is still a release-candidate line. The Spring AI milestone is due 2026-11-19, but no GA artifact exists yet. |
| springdoc | `3.2.0` | `3.2.0` | The current release is built against Boot `4.1.2`. There is no Boot 4.2-compatible release yet. |
| Testcontainers | `2.0.3` | `2.0.3` | Explicitly managed by this repo. Keep the pin until the upgrade is proven. |

Spring AI `2.1.0-RC2` declares this pairing in its release notes:

- Spring Boot `4.2.0-RC2`
- MCP Java SDK `2.2.0-M2`
- Anthropic Java SDK `3.4.0`
- TypeSafe Java SDK `0.4.0`

MCP `2.2.0-M2` is a milestone dependency, not a final release. GA may move it. Treat that as an upgrade risk rather than pinning the milestone permanently.

## Spike result

A detached worktree based on `main` at `21be862` (2026-11-11) was tested with only the parent versions changed:

```text
spring-boot-starter-parent  4.2.0-RC2
spring-ai-bom               2.1.0-RC2
springdoc-openapi           3.2.0
```

Observed on 2026-11-11:

| Check | Result |
| --- | --- |
| `mvn -DskipTests compile` | Passed |
| Non-RAG module tests (`ai-common`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`, `ai-decision`) | Passed: 104 tests, 0 failures/errors |
| `ai-rag` smoke test (`AiRagApplicationTests`) | Passed: 3 tests |
| PGVector integration tests | Not run: Docker is unavailable in this environment |
| `/v3/api-docs` endpoint check for every module | Not run separately; springdoc remains the main third-party compatibility gate |

The spike did **not** keep any version changes. It only proves that the current source compiles and the mock-backed/default tests pass on the RC pair. It does not prove live provider behavior, RAG retrieval against PGVector, or springdoc compatibility with Spring Framework 7.1.

Observed RC2 dependency changes relevant to this repo:

- `spring-ai-starter-model-ollama` now brings `spring-ai-systemone` and decision observation auto-configuration.
- `spring-ai-autoconfigure-model-chat-memory` is still on the classpath, but ChatMemory is deprecated for removal in Spring AI 3.0.
- MCP SDK moves from `2.1.0` to `2.2.0-M2`.
- Anthropic SDK moves from `2.72.0` to `3.4.0`.

## Target state

Upgrade to a matched pair, not one component at a time:

```text
Spring Boot 4.2.x GA
Spring AI 2.1.x GA
springdoc release compatible with Boot 4.2 / Spring Framework 7.1
Java 25
Testcontainers 2.0.3 (or a verified replacement)
```

Do not merge a Boot-only or Spring AI-only bump. Spring AI 2.1 tracks Boot 4.2, and the MCP/Anthropic SDK changes are part of that pairing.

## Breaking-change checklist

| Area | Change in Boot 4.2 / Spring AI 2.1 | Repo impact | Required action |
| --- | --- | --- | --- |
| Parent and BOM alignment | Boot 4.2 + Spring AI 2.1 | Both properties are in `pom.xml`; modules inherit them | Update both, then run a full dependency-convergence and compile check |
| `RestTemplate` deprecation | Spring Framework 7.1 deprecates `RestTemplate`; Boot test infrastructure moves toward `RestClient`/`RestTestClient` | No `RestTemplate`, `TestRestTemplate`, or `MockRestServiceServer` usage was found in the repo | No migration now; do not introduce new `RestTemplate` usage |
| Boot configuration changes | Redis master/replica database behavior, forward-header handling, Tomcat servlet property moves, Micrometer convention changes | None of those features/properties are configured here | Check startup logs and the configuration changelog during the bump |
| `DecisionModel` API | New `org.springframework.ai.decision` API and OpenAI/Ollama/SystemOne implementations | `ai-decision` has its own `DecisionClient`/`JevDecisionClient` and `ai.decision.*` properties; no Java package collision | Keep the custom adapter for the version bump; evaluate standard `DecisionModel` separately (below) |
| Ordered message parts | `AssistantMessage`/`UserMessage` now expose ordered `MessagePart`s; existing constructors/builders still work; `getMedia()`/`getToolCalls()` are unmodifiable and media participates in equality | `ai-multimodal` builds `UserMessage` with text + media; tests use `AssistantMessage(String)` | Re-run multimodal tests; keep reading media through `getMedia()`; no source change expected |
| Streaming parts / tool-call deltas | Streamed chunks carry indexed partial parts; `.stream().content()` still returns text deltas | `ai-agents` SSE streaming (if #37 is merged) uses `.stream().content()` and does not stream tool calls | Rebase the SSE branch and re-run `AgentStreamingIntegrationTests` after the bump |
| Anthropic SDK 3.x | `anthropic-java` 3.4.0; API version `2026-10-21`; `temperature`, `topP`, `topK` are rejected; keys use `Authorization: Bearer` | No Anthropic sampling properties are configured; the SDK handles auth | Remove sampling properties if any are added later; smoke-test the `anthropic` profile when a key is available |
| OpenAI message parts / connection details | `OpenAiChatModel` builds messages from parts; new `OpenAiConnectionDetails` keeps `spring.ai.openai.*` properties working | Existing `spring.ai.openai.api-key`, profile model, and multimodal/structured paths use the documented properties | Re-run OpenAI-profile tests; no custom connection-details bean is needed |
| PgVector `distance` metadata | A stored `distance` metadata key is preserved; computed distance remains on `Document.getScore()` | `ai-rag` does not read or write a `distance` metadata field; `QueryResponse.Source` exposes only id/text | Run PGVector integration tests with Docker and confirm source ordering |
| Chat memory / sessions | `ChatMemory` and `MessageChatMemoryAdvisor` are deprecated in favor of session memory | No direct `ChatMemory` or session API usage was found; starter auto-configuration may still include the deprecated module | Do not migrate as part of the version bump; track removal before Spring AI 3.0 |
| MCP starters | Spring AI 2.1 adds MCP `2026-07-28` "modern" server support; current SDK is `2.2.0-M2`; existing STDIO/SSE/STREAMABLE/STATELESS modes remain | `ai-agents` keeps both starters disabled by default and tests an SSE client/server handshake | Re-run `McpClientProtocolIntegrationTests`; keep the configured protocol unchanged; evaluate `MODERN` separately |
| springdoc | `3.2.0` is built against Boot 4.1.2; no Boot 4.2 release is available | All six REST demos expose `/v3/api-docs` and Swagger UI | Treat `/v3/api-docs` success for every module as a release gate; hold the upgrade if springdoc is incompatible |
| Observability | Boot 4.2 brings Micrometer 1.18; Spring AI 2.1 adds `decision` observations and usage metrics | `ai-observability` uses a local Micrometer `ObservationHandler` and DuckDB sink, not a Brave/OpenTelemetry bridge | Re-run the #33 correlation/timing/status tests; confirm no automatic bridge is introduced |

## `ai-decision` and the new `DecisionModel` API

Spring AI 2.1 adds a standard typed-decision API:

- `DecisionPrompt`, `DecisionResponse`, `DecisionOptions`, and question types in `org.springframework.ai.decision`
- `OpenAiDecisionModel`
- `OllamaDecisionModel`, which uses the Ollama `/v1/systemone` endpoint through `SystemOneClient`
- `SystemOneDecisionModel`, backed by `spring-ai-starter-model-systemone`

The repo's `ai-decision` demo is a deliberately separate Jev/SystemOne-style adapter:

- `DecisionPrimitives` defines the local Choice/Score/Noul contract
- `JevDecisionClient` posts to `/v1/systemone` through WebClient and permissively parses provider JSON
- `ai.decision.*` selects the endpoint, key, model, and timeout

There is **no class or package collision**. The demo's purpose is the adapter boundary and provider-contract tolerance, not a thin wrapper over a Spring AI model. Recommendation for the version bump:

1. Do not replace `JevDecisionClient` with `DecisionModel` as part of the dependency upgrade.
2. Keep the public `/api/decision` contracts and `ai.decision.*` properties stable.
3. Open a separate spike to compare the custom adapter with `SystemOneDecisionModel`/`OllamaDecisionModel`, including:
   - whether the standard API can express the current Choice/Score/Noul responses
   - how provider-specific answer envelopes map to `DecisionResponse`
   - what demo value remains if the custom adapter is removed
4. If the standard API is adopted later, supersede or amend [ADR-0003](../adr/0003-decision-model-integration.md) rather than silently changing the boundary.

## MCP and session follow-ups

- Keep `spring.ai.mcp.server.enabled=false` and `spring.ai.mcp.client.enabled=false` defaults.
- Keep the existing SSE integration test as the compatibility check for the enabled client/server seam.
- The "modern" MCP protocol is opt-in via `spring.ai.mcp.server.protocol=MODERN`. It is not part of this upgrade. A separate spike should verify the modern server/client pair, including the MCP SDK GA version and the `McpSyncNotifier`/multi-round-trip model.
- Chat memory is deprecated, not removed. Nothing changes at runtime in 2.1. Plan a session-memory migration only when the repo starts using conversation memory, or before Spring AI 3.0.

## Migration task list

Complete in order when GA artifacts are available:

- [ ] Confirm GA versions and record them in issue #36.
- [ ] Confirm Spring AI 2.1's Boot compatibility statement and its MCP/Anthropic SDK versions.
- [ ] Update `spring-boot-starter-parent` and `spring-ai.version` together.
- [ ] Reconcile `springdoc.version`; do not override Spring AI-managed MCP/Anthropic versions unless a documented incompatibility requires it.
- [ ] Run `mvn -DskipTests compile`.
- [ ] Run `mvn test` with Docker available, including the RAG PGVector tests.
- [ ] Run `mvn -pl ai-rag -am test` and verify retrieval ordering/source content.
- [ ] Run `mvn -pl ai-agents -am test`, including the MCP protocol test and, if merged, `AgentStreamingIntegrationTests`.
- [ ] Verify `POST /api/agents/stream` still emits `token`/`done`/`error` events if #37 is merged.
- [ ] Verify `GET /v3/api-docs` (and Swagger UI) for all seven applications.
- [ ] Re-run the #33 observability tests for parent links, timing, and error status.
- [ ] Replace remaining `getDefaultOptions()` test stubs with `getOptions()` where the test is touched.
- [ ] Confirm no Anthropic `temperature`/`topP`/`topK` properties or code remain.
- [ ] Inspect the dependency tree for `spring-ai-systemone`, `spring-ai-autoconfigure-model-chat-memory`, MCP `2.2.x`, and Anthropic `3.x`.
- [ ] Update `README.md`, module guides, [architecture](../architecture.md), and [implementation learnings](../learnings/implementation-learnings.md) with the new pins and any behavior changes.
- [ ] Keep issue #36 open until the full test command is green; then record the evidence and close it.

## Hold/rollback criteria

Hold the upgrade and return to the stable pins if any of these is true:

- springdoc cannot generate `/v3/api-docs` for a module and no compatible release exists.
- The MCP SDK milestone causes a client/server protocol regression that cannot be isolated.
- RAG retrieval or source ordering changes without an explainable cause.
- A live Anthropic/OpenAI/Ollama profile fails at startup or on a basic call after removing now-rejected sampling options.
- `mvn test` cannot be made green, including the Docker-backed RAG tests.
- The change would require altering a public REST contract solely to satisfy the framework upgrade.

## Sources

- [Spring Boot 4.2.0-RC2 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-RC2-Release-Notes)
- [Spring Boot 4.2.0-RC2 announcement](https://spring.io/blog/2026/11/06/spring-boot-4-2-0-RC2-available-now)
- [Spring AI 2.1 upgrade notes](https://docs.spring.io/spring-ai/reference/2.1/upgrade-notes.html#upgrading-to-2-1-0)
- [Spring AI 2.1.0-RC2 announcement](https://spring.io/blog/2026/11/09/spring-ai-2-1-0-RC2-available-now)
- Maven Central metadata for `spring-boot-dependencies`, `spring-ai-bom`, and `springdoc-openapi-starter-webmvc-ui`, checked 2026-11-11
