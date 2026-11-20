# Spring Boot 4.2 and Spring AI 2.1 upgrade

**Status:** GA upgrade implemented. The parent now pins Boot `4.2.0` and Spring AI `2.1.0`; the earlier RC spike is retained below as the planning record.

Tracking issue: [#36](https://github.com/prasadgaikwad/spring-ai-apps/issues/36).

## Release status

Snapshot refreshed **2026-11-20** from Maven Central metadata and the GA announcements:

| Component | Previous pin | GA/current version | Notes |
| --- | --- | --- | --- |
| Spring Boot | `4.1.2` | `4.2.0` | GA announced 2026-11-19. |
| Spring AI | `2.0.2` | `2.1.0` | GA announced 2026-11-20; built on Boot 4.2. |
| springdoc | `3.2.0` | `3.2.0` | No bump needed; compatibility verified against Boot 4.2 / Spring Framework 7.1 in this upgrade. |
| Testcontainers | `2.0.3` | `2.0.3` | Unchanged; the existing PGVector tests still use this pin. |

Spring AI `2.1.0` GA is built on Spring Boot `4.2` and moves the related SDKs forward:

- MCP Java SDK `2.2.0` (final, not the `2.2.0-M2` milestone seen in RC2)
- Anthropic Java SDK `3.x` (resolved to `3.8.0` in this reactor)
- The TypeSafe client is now `spring-ai-systemone` in Spring AI; no separate TypeSafe SDK pin remains in this repo
- GA also adds modern MCP clients and servers, `DecisionModel` implementations, the Session API, richer message parts, and fixes for 13 CVEs

The earlier milestone pairing is kept in the spike section for historical context. No milestone dependency is pinned by this upgrade.

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

- `spring-ai-starter-model-ollama` now brings `spring-ai-systemone` and decision observation auto-configuration. Since #57, only the six chat demos declare that starter; `ai-decision` depends on the model API through `ai-common` and loads no `ChatModel` beans.
- `spring-ai-autoconfigure-model-chat-memory` is still on the classpath, but ChatMemory is deprecated for removal in Spring AI 3.0.
- MCP SDK moves from `2.1.0` to `2.2.0-M2`.
- Anthropic SDK moves from `2.72.0` to `3.4.0`.

## GA upgrade result

The implementation changes the parent versions plus test seams; it does not change production APIs, public REST contracts, or configuration defaults.

```text
spring-boot-starter-parent  4.2.0
spring-ai-bom               2.1.0
springdoc-openapi           3.2.0 (unchanged)
```

| Check | Result |
| --- | --- |
| `./mvnw -U -B -DskipTests compile` | Passed; Maven Enforcer dependency convergence passed for every module |
| `./mvnw -B test -DexcludedGroups=docker -Dsurefire.failIfNoSpecifiedTests=false` | Passed: 176 tests, 0 failures/errors |
| `McpClientProtocolIntegrationTests` and `AgentStreamingIntegrationTests` | Passed on the GA pair; the SSE handshake and streaming `.content()` path are unchanged |
| `GET /v3/api-docs` and `/swagger-ui/index.html` | Added to the existing app-context tests for all seven modules; all passed |
| #33 observability tests | Passed in the full reactor run; no automatic trace bridge was introduced |
| PGVector/Testcontainers tests | Excluded locally because Docker is unavailable; validated separately by the CI `docker-integration` job |
| Live OpenAI/Anthropic/Ollama providers, MCP peer stack, and Jev endpoint | Not exercised; no live-vendor dependency was added |

The resolved dependency tree confirms `spring-ai-systemone` and `spring-ai-autoconfigure-model-chat-memory` on the six chat demos, MCP SDK `2.2.0`, and Anthropic SDK `3.8.0`. `ai-decision` still loads no `ChatModel` beans. The `getDefaultOptions()` stubs touched by this upgrade now use `getOptions()`.

## Target state

The delivered target is a matched pair, not a one-component bump:

```text
Spring Boot 4.2.0 GA
Spring AI 2.1.0 GA
springdoc 3.2.0 (verified against Boot 4.2 / Spring Framework 7.1)
Java 25
Testcontainers 2.0.3 (unchanged)
```

Spring AI 2.1 tracks Boot 4.2, and the MCP/Anthropic SDK changes are part of that pairing. No Boot-only or Spring AI-only bump was merged.

## Breaking-change checklist

| Area | Change in Boot 4.2 / Spring AI 2.1 | Repo impact | Resolution |
| --- | --- | --- | --- |
| Parent and BOM alignment | Boot 4.2 + Spring AI 2.1 | Both properties are in `pom.xml`; modules inherit them | Updated both together; Enforcer convergence and compile passed without suppressions |
| `RestTemplate` deprecation | Spring Framework 7.1 deprecates `RestTemplate`; Boot test infrastructure moves toward `RestClient`/`RestTestClient` | No `RestTemplate`, `TestRestTemplate`, or `MockRestServiceServer` usage was found in the repo | No migration needed; no new `RestTemplate` usage introduced |
| Boot configuration changes | Redis master/replica database behavior, forward-header handling, Tomcat servlet property moves, Micrometer convention changes | None of those features/properties are configured here | No configuration changes were required |
| `DecisionModel` API | New `org.springframework.ai.decision` API and OpenAI/Ollama/SystemOne implementations | `ai-decision` has its own `DecisionClient`/`JevDecisionClient` and `ai.decision.*` properties; no Java package collision | Custom adapter kept; standard `DecisionModel` remains a separate decision (below) |
| Ordered message parts | `AssistantMessage`/`UserMessage` now expose ordered `MessagePart`s; existing constructors/builders still work; `getMedia()`/`getToolCalls()` are unmodifiable and media participates in equality | `ai-multimodal` builds `UserMessage` with text + media; tests use `AssistantMessage(String)` | Multimodal tests passed; media is still read through `getMedia()`; no source change needed |
| Streaming parts / tool-call deltas | Streamed chunks carry indexed partial parts; `.stream().content()` still returns text deltas | `ai-agents` SSE streaming uses `.stream().content()` and does not stream tool calls | `AgentStreamingIntegrationTests` passed; `token`/`done`/`error` events are unchanged |
| Anthropic SDK 3.x | GA resolves `anthropic-java` `3.8.0`; sampling options are rejected; keys use bearer auth | No Anthropic sampling properties are configured; the SDK handles auth | No sampling properties found; live `anthropic` profile still untested without a key |
| OpenAI message parts / connection details | `OpenAiChatModel` builds messages from parts; new `OpenAiConnectionDetails` keeps `spring.ai.openai.*` properties working | Existing `spring.ai.openai.api-key`, profile model, and multimodal/structured paths use the documented properties | Mocked OpenAI-profile tests passed; no custom connection-details bean was added; live profile untested |
| PgVector `distance` metadata | A stored `distance` metadata key is preserved; computed distance remains on `Document.getScore()` | `ai-rag` does not read or write a `distance` metadata field; `QueryResponse.Source` exposes only id/text | Docker-backed PGVector tests are left to CI; no code change was needed |
| Chat memory / sessions | `ChatMemory` and `MessageChatMemoryAdvisor` are deprecated in favor of the new Session API | No direct `ChatMemory` or session API usage was found; starter auto-configuration may still include the deprecated module | No migration in this upgrade; #47 trigger remains unchanged |
| MCP starters | GA adds MCP `2026-07-28` "modern" clients and servers on SDK `2.2.0`; existing STDIO/SSE/STREAMABLE/STATELESS modes remain | `ai-agents` keeps both starters disabled by default and tests an SSE client/server handshake | SDK `2.2.0` resolved; `McpClientProtocolIntegrationTests` passed; `MODERN` remains opt-in and out of scope |
| springdoc | `3.2.0` was the latest release at GA time; compatibility with Framework 7.1 was the open gate | All seven REST demos expose `/v3/api-docs` and Swagger UI | Verified both endpoints in every module's app-context test; no version bump needed |
| Observability | Boot 4.2 brings Micrometer 1.18; Spring AI 2.1 adds decision observations and usage metrics | `ai-observability` uses a local Micrometer `ObservationHandler` and DuckDB sink, not a Brave/OpenTelemetry bridge | #33 tests passed; no automatic trace bridge was introduced |

## `ai-decision` and the new `DecisionModel` API

Spring AI 2.1 adds a standard typed-decision API:

- `DecisionPrompt`, `DecisionResponse`, `DecisionOptions`, and question types in `org.springframework.ai.decision`
- `OpenAiDecisionModel`
- `OllamaDecisionModel`, which uses the Ollama `/v1/systemone` endpoint through the TypeSafe Java SDK
- `SystemOneDecisionModel`, in `spring-ai-systemone`; GA also adds `spring-ai-starter-model-systemone` for auto-configuration, which this repo does not use

The repo's `ai-decision` demo is a deliberately separate Jev/SystemOne-style adapter:

- `DecisionPrimitives` defines the local Choice/Score/Noul contract
- `JevDecisionClient` posts to `/v1/systemone` through WebClient and permissively parses provider JSON
- `ai.decision.*` selects the endpoint, key, model, and timeout

There is **no class or package collision**. The demo's purpose is the adapter boundary and provider-contract tolerance, not a thin wrapper over a Spring AI model.

The #46 evaluation kept the custom adapter after the RC2 prototype: the standard API has no Noul answer, its official System One mapper turns Noul into a `YesNoAnswer` probability and loses the reason, and its Score type requires ordered levels/per-level probabilities. See [Spring AI `DecisionModel` evaluation](../spikes/46-decision-model-evaluation.md). Outcome of this upgrade:

1. `JevDecisionClient` was not replaced with `DecisionModel`.
2. The public `/api/decision` contracts and `ai.decision.*` properties are unchanged.
3. The [#46](https://github.com/prasadgaikwad/spring-ai-apps/issues/46) evaluation compared the custom adapter with `SystemOneDecisionModel`/`OllamaDecisionModel`: the standard API cannot express Noul losslessly, and its Score/Choice shapes carry different semantics. The custom adapter stays unless a later decision accepts those contract changes.
4. If the standard API is adopted later, supersede or amend [ADR-0003](../adr/0003-decision-model-integration.md) rather than silently changing the boundary.

## MCP and session follow-ups

- Keep `spring.ai.mcp.server.enabled=false` and `spring.ai.mcp.client.enabled=false` defaults.
- Keep the existing SSE integration test as the compatibility check for the enabled client/server seam.
- The "modern" MCP protocol is opt-in via `spring.ai.mcp.server.protocol=MODERN`. It is not part of this upgrade. The spike in [#45](https://github.com/prasadgaikwad/spring-ai-apps/issues/45) verified the modern server over raw HTTP but found no modern client in Spring AI `2.1.0-RC2`; GA adds modern clients on SDK `2.2.0`, resolving that gap. See [MCP `MODERN` protocol spike](../spikes/45-mcp-modern-protocol.md). The existing defaults are unchanged; adoption needs its own issue and ADR update.
- Chat memory is deprecated, not removed. Nothing changes at runtime in 2.1. Plan a session-memory migration only when the repo starts using conversation memory, or before Spring AI 3.0. The conditional plan and trigger are in [Session-memory migration plan](../plans/47-session-memory-migration.md); no migration is implemented now.

## Migration task list

Completed for the GA pair:

- [x] Confirm GA versions and record them in issue #36.
- [x] Confirm Spring AI 2.1's Boot compatibility statement and its MCP/Anthropic SDK versions.
- [x] Update `spring-boot-starter-parent` and `spring-ai.version` together.
- [x] Reconcile `springdoc.version`; no override was needed for the Spring AI-managed MCP/Anthropic versions.
- [x] Run `./mvnw -U -B -DskipTests compile`, including Enforcer dependency convergence.
- [ ] Run the Docker-tagged RAG PGVector tests in CI; local Docker is unavailable.
- [x] Run the full no-Docker reactor test command, including `ai-rag` and `ai-agents`.
- [x] Re-run `McpClientProtocolIntegrationTests` and `AgentStreamingIntegrationTests`; streaming `token`/`done`/`error` events are unchanged.
- [x] Verify `GET /v3/api-docs` and `/swagger-ui/index.html` for all seven applications.
- [x] Re-run the #33 observability tests for parent links, timing, and error status.
- [x] Replace the `getDefaultOptions()` test stubs touched by this upgrade with `getOptions()`.
- [x] Confirm no Anthropic `temperature`/`topP`/`topK` properties or code remain.
- [x] Inspect the dependency tree for `spring-ai-systemone`, `spring-ai-autoconfigure-model-chat-memory`, MCP `2.2.x`, and Anthropic `3.x`.
- [x] Update `README.md`, module guides, [architecture](../architecture.md), ADR-0002, and the documentation audit with the new pins.
- [ ] Keep issue #36 open until the implementation PR is merged with green CI; then close it with the PR reference.

## Hold/rollback criteria

Hold the upgrade and return to the stable pins if any of these is true:

- springdoc cannot generate `/v3/api-docs` for a module and no compatible release exists.
- The MCP SDK milestone causes a client/server protocol regression that cannot be isolated.
- RAG retrieval or source ordering changes without an explainable cause.
- A live Anthropic/OpenAI/Ollama profile fails at startup or on a basic call after removing now-rejected sampling options.
- `mvn test` cannot be made green, including the Docker-backed RAG tests.
- The change would require altering a public REST contract solely to satisfy the framework upgrade.

## Sources

- [Spring Boot 4.2.0 announcement](https://spring.io/blog/2026/11/19/spring-boot-4-2-0-available-now/)
- [Spring AI 2.1.0 GA announcement](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)
- [Spring AI 2.1 release notes](https://github.com/spring-projects/spring-ai/releases/tag/v2.1.0)
- [Spring AI 2.1 upgrade notes](https://docs.spring.io/spring-ai/reference/2.1-SNAPSHOT/upgrading/spring-ai-2-1.html)
- [Spring Boot 4.2.0-RC2 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-RC2-Release-Notes) and [RC2 announcement](https://spring.io/blog/2026/11/06/spring-boot-4-2-0-RC2-available-now) (planning record)
- [Spring AI 2.1.0-RC2 announcement](https://spring.io/blog/2026/11/09/spring-ai-2-1-0-RC2-available-now) (planning record)
- Maven Central metadata for `spring-boot-starter-parent`, `spring-ai-bom`, and `springdoc-openapi-starter-webmvc-ui`, checked 2026-11-11 and refreshed 2026-11-20
