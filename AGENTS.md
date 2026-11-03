# AGENTS.md

Maven monorepo with `ai-common` and seven implemented Demo modules. Read the domain and architecture documents before changing feature behavior.

## Read for the task

- `CONTEXT.md`: canonical domain vocabulary.
- `docs/0001-spring-ai-apps-core-and-ai-rag-spec.md`: original scope plus current implementation-status notes; distinguish the original plan from delivered behavior.
- `docs/architecture.md` and the affected module's `README.md`: current API contracts, flow diagrams, configuration, and known implementation gaps.
- `docs/adr/README.md`: index of settled decisions; consult relevant records before changing boundaries.
- `docs/learnings/implementation-learnings.md`: module-specific gotchas and testing limitations. For Boot/model auto-configuration failures, also read `docs/learnings/scaffold-spring-ai-2-monorepo.md`.

## Build and run

- Java 25, Maven 3.9+. `mvn -DskipTests compile`; `mvn test` for the reactor.
- RAG integration tests require Docker and provision their own PGVector container; they do not use the manually started Compose database. Other default tests use mocks/local infrastructure.
- For selected builds/tests use `mvn -pl ai-rag -am test` (substitute module). `-am` builds prerequisites in-reactor.
- To run a demo, first `mvn -pl ai-common -am install -DskipTests`, then `mvn -pl ai-rag spring-boot:run`. Apply Boot run only to the executable module, not the parent/shared reactor.
- `docker compose up -d` starts the local RAG database. Each Demo module defaults to port 8080; override `server.port` for concurrent processes.
- `.env.example` documents keys/defaults. Export needed variables into the JVM environment; Maven/Boot do not automatically load `.env`. Keep real keys in ignored local configuration.

## Architecture guardrails

- Parent pins Boot 4.1.1, Spring AI 2.0.1, Java 25. Java root package: `dev.prasadgaikwad.springaiapps.*`; Maven coordinates retain `spring-ai-apps`.
- Modules: `ai-common`, `ai-rag`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`, `ai-decision`.
- `ai-common` owns shared configuration/errors and the pure Eval harness. PGVector starter stays in `ai-rag`; DuckDB sink/configuration stays in `ai-observability`.
- Six Spring AI demos default to `ollama` and define `openai`/`anthropic` chat profiles. `ai-decision` uses `ai.decision.*` and one Jev adapter; its `provider` property does not select beans.
- Chat profile selection uses `@Primary` aliases; this does not disable other provider beans or select all media capabilities. Embedding aliases exist for Ollama/OpenAI, not Anthropic. Preserve stored/query embedding compatibility and rebuild/re-ingest on incompatible model/dimension changes.
- Only `ai-rag` and `ai-agents` have Thymeleaf pages. All seven expose REST and OpenAPI; ping endpoints remain alongside features.

## Testing and learned constraints

- Primary seam: public REST behavior through `@SpringBootTest` + MockMvc, deterministic model mocks. Keep the Eval harness's focused pure-logic unit seam; existing tool/record tests also verify local contracts.
- Boot 4 MockMvc support is parent test dependency `spring-boot-webmvc-test`; annotation package `org.springframework.boot.webmvc.test.autoconfigure`.
- For a mocked ChatModel use `@MockitoBean` with the provider bean name and stub `getOptions()` when constructing ChatClients. See the affected module's integration tests.
- The configured cloud starters can create unused beans at startup. Six demos use non-empty placeholder keys in base YAML; empty exported values can defeat these placeholders. `ai-decision` does not include those cloud starters.
- RAG smoke tests exclude database auto-configuration and mock services; PGVector integration tests exercise the real database separately. Do not describe a smoke-test pass as retrieval validation.
- `ai-decision` currently tests context/ping/empty input and DTOs, not upstream HTTP mapping. The observation sink uses names as trace IDs and stop-time timestamps; describe these limitations accurately.

## Workflow

- Track work on `prasadgaikwad/spring-ai-apps`; `ready-for-agent` identifies specified tickets. Preserve original issue acceptance criteria when recording progress.
- Follow the existing naming/style and relevant ADRs. Keep glossary definitions separate from implementation details.
- For documentation changes, cross-check routes, fields, property defaults, test seams, and diagrams against source. Distinguish intended design, verified application behavior, and unverified external-provider behavior.
- Before changing shared configuration, review all consuming modules. Before editing environment samples, compare against the base branch and preserve unrelated entries.
