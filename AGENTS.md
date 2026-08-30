# AGENTS.md

Multi-module Maven monorepo of production-grade Spring AI reference demos. The scaffold from the first ticket (parent POM + `ai-common` + six demo skeletons) is committed; the six demos are still placeholders (REST ping endpoints only). Read the design docs before writing feature code — they are the authoritative source.

## Authoritative docs (read first)

- `CONTEXT.md` — the domain glossary. Use its vocabulary verbatim (e.g. *Demo module*, *ai-common*, *Provider choice*, *Vector store*, *Tool*, *Eval harness*).
- `docs/0001-spring-ai-apps-core-and-ai-rag-spec.md` — the feature spec (modules, decisions, testing seams, user stories).
- `docs/adr/0001-provider-portability-profiles-ollama-default.md` and `docs/adr/0002-monorepo-shared-ai-common-module.md` — settled trade-offs you must not "fix".

## Build & run

- Java 25 (Temurin) + Maven 3.9 present.
- `mvn -DskipTests compile` — compiles the whole monorepo green.
- `mvn test` — runs every demo's `@SpringBootTest` smoke test. All green by default (see gotchas below for why).
- Running a single demo with `-pl ai-rag -am` (`-am` builds `ai-common` in-reactor; without it you'll get "Could not find artifact ai-common").
- Postgres/PGVector: `docker compose up -d` (top-level compose).
- Copy `.env.example` to a git-ignored local env and set real keys; never commit real keys.

## Settled architecture (do not re-derive)

- **Build**: Maven monorepo; parent POM is `spring-boot-starter-parent` 4.1.1 + imports `spring-ai-bom` 2.0.1.
- **Java/toolchain**: Java 25. **Root package is `dev.prasadgaikwad.springaiapps.*`** (Java forbids hyphens — the Maven groupId/artifactId remain `spring-ai-apps`, not the package).
- **Modules**: shared `ai-common` + six demos `ai-rag`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`. Don't add/rename casually.
- **Provider portability**: a demo targets one provider via `spring.profiles.active`; Ollama is the zero-key default, `openai`/`anthropic` are documented alternatives. Keys come from `.env.example` + `${KEY}` placeholders.
- **Vector store**: PGVector via Postgres on Docker. Embeddings are provider-coupled — switching a Vector-store-backed demo's profile requires re-ingestion (ADR-0001).
- **Observability**: the all-local DuckDB trace sink + query endpoints live **only** in `ai-observability`; other demos use Spring Boot's auto-configured tracing.

## Hard-earned gotchas (agent would otherwise fight these for hours)

- **`spring-ai-starter-vector-store-pgvector` is NOT in `ai-common`.** It lives only in `ai-rag`. Keeping it in `ai-common` leaked a mandatory DataSource into every demo and broke all context-loads ("Failed to determine a suitable driver class"). `ai-common` holds the *shared code* for Vector store config, not the heavy starter.
- **Spring AI 2.0 auto-configures ALL OpenAI/Anthropic model beans (chat, image, audio, transcription) unconditionally** when those starters are on the classpath, and fails context-load if there's no API key. Every demo therefore defines non-empty placeholder keys in its base config: `openai.api-key: ${OPENAI_API_KEY:not-configured}` and `anthropic.api-key: ${ANTHROPIC_API_KEY:not-configured}`. Real keys override via env for the cloud profiles.
- **Boot 4 moved MockMvc test support** out of `spring-boot-starter-test` into a new artifact **`spring-boot-webmvc-test`** (added once, test-scoped, in the parent POM). And `AutoConfigureMockMvc` moved to package **`org.springframework.boot.webmvc.test.autoconfigure`** (NOT `...test.autoconfigure.web.servlet`). Both are Boot 4.1 changes you'd otherwise miss.
- **ai-rag's smoke test excludes the DB autoconfigs** (`PgVectorStoreAutoConfiguration`, `DataSourceAutoConfiguration`) so the skeleton loads without a running Postgres. The real Vector store integration is tested in the ETL/query work (issues #2/#3).

## Conventions you'd otherwise guess wrong

- **UI surface**: only `ai-agents` and `ai-rag` get a minimal Thymeleaf UI (chosen in the spec). All other demos are REST + OpenAPI only.
- **Testing seams**: primary seam is one `@SpringBootTest` per Demo module driven through the public REST contract (deterministic via a pointed/mocked ChatModel); the only focused unit-test seam is the `ai-common` Eval harness (pure logic). UI pages get context-load smoke tests only. Test externally-observable behavior, not orchestration internals.
- **Scaffold controllers**: each demo has a placeholder `*ScaffoldController` with a `GET /api/<module>/ping` and a `PingResponse` record. Feature work replaces these per-spec.

## Workflow

- Track work as GitHub issues on `prasadgaikwad/spring-ai-apps` (the `ready-for-agent` label marks specified, seam-agreed tickets). Implement per-spec/issues, not ad hoc.
- Before editing glossary/ADRs, honor the existing decisions; update `CONTEXT.md` inline as new terms resolve.
