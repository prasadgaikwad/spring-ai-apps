# Implementation learnings across the monorepo

These findings come from the implemented use cases and source/test review for issue #28. They complement the [foundation/debugging notes](scaffold-spring-ai-2-monorepo.md). Each section distinguishes working application behavior from what current tests cannot prove.

## Build, runtime, and documentation

- **Build dependencies before run.** Use `mvn -pl <demo> -am test` for reactor tests. For run, install `ai-common` and its parent with `mvn -pl ai-common -am install -DskipTests`, then invoke `spring-boot:run` only on the executable demo. Running Boot over the parent/shared reactor fails before reaching the demo.
- **Environment files are samples, not a JVM loader.** Maven/Boot do not automatically import `.env`. Export required variables. Empty cloud keys can override non-empty placeholders; empty Jev aliases do not fall through to later aliases. Compare `.env.example` against the base branch before adding settings: replacing a sample file can silently delete unrelated provider and database defaults (as happened during #13 and was restored).
- **One process per demo.** Every demo defaults to port 8080; assign `server.port` for concurrent runs. A classpath dependency does not connect processes or send observations to the observability demo.
- **Read code before drawing a diagram.** The actual agent route is `/api/agents/ask`, not `/api/agents/chat`; RAG has `QueryController`, not a `RagController`; DuckDB's path is `./data/ai-observability-traces.duckdb`. A small diagram with wrong names is worse than a source-linked explanation.
- **Separate intent, implementation, and verification.** The initial spec described six skeletons and future work; those passages are historical. The current parent has seven demos. A passing mock-backed test does not verify an external API or model capability.

## Shared configuration and ai-common

`@Primary` aliasing solves ambiguous `ChatModel` injection when multiple provider starters are present. It does not deactivate other model beans. Keep one active chat-provider profile and use bean names (`ollamaChatModel`, `openAiChatModel`, `anthropicChatModel`) when mocking. Stub `getOptions()` for client construction as the existing integration fixtures do.

The shared module owns code, not every infrastructure starter. PGVector in `ai-common` would force DataSource startup on unrelated demos. Conditional embedding aliases are activated by the Vector store API; only Ollama/OpenAI aliases currently exist. DuckDB wiring likewise belongs only in observability. Shared prompt templates were planned, but are not a current shared library.

## Eval harness and ai-eval

The shared harness is pure logic with Spring `Assert` validation and no model I/O. Criterion semantics matter: case-insensitive required/forbidden substrings, whitespace-normalized equality, and regex `find` rather than full-string `matches`. Compile invalid regexes early. Test report ordering and pass→fail semantics, rather than merely record accessors.

`CaseReport.passed` is derived even if supplied as a constructor parameter. `Summary` checks counts and bounds; a manually constructed `EvalReport` does not reconcile its summary with its case list. The harness rejects duplicate case IDs, while baseline comparison of manually created duplicates keeps the first baseline occurrence.

The REST layer differs from the shared core: blank answers trigger generation, IDs are assigned by position, and comparison uses only the preceding report. Reorder inputs and the regression identity changes meaning. History is unbounded, unsynchronized process memory; GET returns no stored regression diff. Keep a stable pair order and treat this as a reference flow rather than a durable evaluation service.

## RAG ingestion, storage, and retrieval

- **PGVector IDs are UUIDs.** Chunk IDs derive from source and text with `UUID.nameUUIDFromBytes`, matching the table's UUID key. Arbitrary hashes/IDs are not interchangeable with that schema.
- **Upsert is not source replacement.** Same source/text produces the same ID; changed text produces another ID. There is no cleanup of removed/old chunks, and identical repeated chunks can collapse to one stored row.
- **Keep embeddings compatible.** Model/provider changes may require dimensions/schema changes and fresh ingestion. RAG's base dimension is 768; OpenAI profile defaults do not automatically resize the table. Anthropic chat has no corresponding shared embedding alias.
- **Hybrid SQL is explicit schema coupling.** The keyword leg accesses PGVector `content` with English full-text functions because metadata filters cannot express content search. Preserve configured table/schema compatibility. RRF fuses ranks, avoiding raw-score calibration, and the lexical reranker is deterministic rather than learned.
- **Grounding is a model instruction.** The advisor supplies context and the service returns retrieved chunks. This is useful evidence, not a truth guarantee. Fixed-vector/mock-model tests prove flow and source propagation, not embedding relevance.
- **Smoke and integration are different.** DB-free application tests substitute services and exclude database auto-configurations. Testcontainers tests use their own PGVector instance; starting Compose is not their setup step. Docker must be available for the full reactor.

## Agents, tools, and MCP

Local calculator tools and MCP tools are separate paths. Local tools are registered on `toolAssistant`; the MCP request attaches callbacks supplied by an optional `ToolCallbackProvider`. No provider means an empty callback list, so ordinary chat can still return an answer. A successful answer alone does not prove tool execution.

The MCP server is disabled by default. Explicitly enabling server publication does not configure a client peer or a loopback connection. The POM currently has a server starter but no MCP client starter, so remote discovery needs additional integration/configuration or a supplied callback provider. The module's MCP glossary is fixed in code rather than read from the domain document. Existing tests check catalogs and mocked chat, plus direct calculator behavior; they do not prove a live MCP transport exchange.

## Structured extraction

Parsing and validation are different success criteria. `entity(Class)` may produce a valid record representation that violates `@NotBlank`, email, range, or nested constraints. Return sorted violations and `valid=false` with the value for inspection; a conversion failure instead becomes 400. Callers must inspect `valid`, not just the HTTP status. There is no repair loop or factuality checker. The order target is one product/quantity, not line items; candidate job history is `history`.

## Multimodal capability and payload handling

The primary ChatModel serves vision; image/TTS/transcription resolve their own optional interface beans. An auto-configured OpenAI media bean with a placeholder key can exist even under Ollama, so “optional” does not ensure an endpoint is offline or disabled. Test missing-bean behavior separately from invalid credentials and provider capability.

The API uses base64 JSON rather than multipart streaming. Vision defaults to PNG and wraps decode/MIME/model failures; transcription ignores its MIME field, speech ignores `voice`, and speech labels output MPEG. Image dimensions apply only as a pair and `revisedPrompt` remains null. Mock media tests validate local mapping, not file formats or actual provider responses. Keep these limitations visible rather than documenting request fields as supported options just because the DTO declares them.

## Observability and DuckDB

The handler accepts every local observation context. It copies tags/errors on stop, then writes synchronously to DuckDB. That is useful local capture but not a distributed tracing bridge: observation names stand in for trace/parent IDs, both timestamps are recorded at stop, and status is always `OK`. Do not use automatic stored duration for latency analysis or treat name-based grouping as an actual request trace.

Manual REST insertion can carry proper timestamps and chosen trace IDs. Query limits clamp to 1–500; span ordering is oldest-first while trace summaries order by latest aggregate start. The file path is relative to the JVM working directory; schema initialization is create-if-absent, with no migration or retention policy. Tests exercise persistence/query/tag behavior, not automatic tracing correctness.

## Decisions and the Jev adapter

Separate a local typed abstraction from a verified provider contract. `DecisionClient` is an extension point, but only one implementation exists and `provider` is not a bean selector. The `/v1/systemone` path and parser aliases are assumptions in this code, not facts established by the current tests.

The parser defaults probability to 1.0, score to zero, and unknown types to Choice. Blank/unrecognized answer containers can become empty success responses. These permissive mappings can hide upstream mismatch; they do not calibrate probabilities. Noul is currently a local none/null concept whose vendor meaning needs confirmation. State/list copies are shallow, and per-question validation is minimal.

Tests cover context/ping/empty questions and DTO fields/state copy. There is no adapter HTTP success/failure/timeout test. Document that gap directly rather than describing “mocked HTTP” coverage. Adding a second provider would need explicit selection and independent protocol verification.

## Maintenance checklist for future feature work

Update the affected module guide's API fields/defaults/diagrams, then check [architecture](../architecture.md), [glossary](../../CONTEXT.md), and [ADR index](../adr/README.md) for changed boundaries. Keep the glossary about terms, learnings about gotchas, and ADRs about reasons/trade-offs. Preserve original issue acceptance criteria when reporting progress. A new test result should state which seam was exercised and whether Docker or an external provider participated.
