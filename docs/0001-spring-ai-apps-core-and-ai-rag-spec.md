# Spring AI Apps — Core Monorepo & ai-rag Reference Spec

## Problem Statement

As an AI engineering team, we want a set of production-grade reference applications that showcase the hardest, most common AI-engineering use cases with Spring Boot + Spring AI — so we don't have to re-derive the "hard" wiring (RAG ETL, agentic tooling, evaluation, observability, multimodal, structured output) from scratch each time. Today we have nothing: the repo is an empty scaffolding with no runnable reference, no shared plumbing, and no settled architecture for how the demos should hang together.

## Solution

A multi-module Maven monorepo (`spring-ai-apps`) pinning Spring Boot 4.1 + Spring AI 2.0, targeting Java 25, with one shared `ai-common` module and six runnable Demo modules. Demos are provider-agnostic via `spring.profiles.active`, defaulting to local Ollama (zero-key). We build the foundation plus the first, flagship demo — `ai-rag` — end-to-end, proving the shared plumbing before the other five demos follow.

## User Stories

### Monorepo & shared foundation

1. As a developer, I want a multi-module Maven monorepo with pinned Spring Boot 4.1 and Spring AI 2.0 versions, so that all demos share one consistent dependency baseline.
2. As a developer, I want a shared `ai-common` module, so that Vector store config, the Eval harness, prompt templates, and common REST/error handling are defined once and reused across demos.
3. As a developer, I want the parent POM to import both the Spring Boot parent and the `spring-ai-bom`, so that Spring AI dependencies resolve to compatible versions across every module.
4. As a developer, I want a single top-level Docker Compose for Postgres/PGVector, so that demos that need a Vector store share one local infra service.
5. As a developer, I want the root package to be `dev.prasadgaikwad.springaiapps.*`, so that all modules share a consistent namespace.

### Provider portability

6. As a developer, I want to select the AI provider per demo at runtime via `spring.profiles.active`, so that I can run against Ollama, OpenAI, or Anthropic without code changes.
7. As a developer, I want Ollama as the documented zero-key default provider, so that the whole monorepo runs locally without any API keys.
8. As a developer, I want OpenAI and Anthropic documented as alternative provider profiles, so that I can swap to cloud providers when needed.
9. As a developer, I want API keys provided via `.env.example` and `${KEY}` placeholders (never committed), so that secrets stay out of source control.
10. As a developer, I want the provider↔embedding coupling made explicit, so that I understand re-ingestion is required when switching the active provider profile for a Vector-store-backed demo.

### ai-rag demo (built first)

11. As a user, I want to ingest my own documents through a full ETL pipeline (read → split → transform → embed → store in PGVector), so that the RAG reference demonstrates real production ingestion.
12. As a user, I want to ask questions over my ingested documents through a REST endpoint, so that I can exercise the retrieval path.
13. As a user, I want retrieval with reranking before generation, so that the answer is grounded in the most relevant context.
14. As a user, I want hybrid search across the Vector store, so that retrieval quality benefits from both semantic and keyword matching.
15. As a user, I want answers grounded only in retrieved context, so that the demo showcases honest, hallucination-resistant RAG.
16. As a user, I want a minimal Thymeleaf chat UI over the RAG endpoint, so that I can feel the end-user chat-with-docs experience.
17. As a developer, I want the RAG query path exposed over REST + OpenAPI, so that I can call it from curl/Postman as a reference for building my own.

### Remaining demos (future work, scaffolding only now)

18. As a developer, I want module skeletons for `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, and `ai-structured`, so that the whole monorepo compiles green before each is deepened.
19. As a developer, I want `ai-agents` to show MCP server + client integration and plain `Tool` calling with orchestration, so that the agentic reference covers both the frontier interoperable pattern and the simple one.
20. As a user, I want `ai-eval` to run an in-app Eval harness grading Q&A pairs and reporting regression-style, so that I can grade generated content and guard against hallucination.
21. As a user, I want `ai-observability` to store traces all-locally in DuckDB and let me query recent spans, so that I can observe AI operations without a vendor.
22. As a user, I want `ai-multimodal` to cover image generation, vision, audio TTS, and audio transcription, so that all four model modalities are demonstrated.
23. As a user, I want `ai-structured` to map AI output to POJOs with typed extraction plus validation, so that structured output is demonstrated.
24. As a developer, I want a minimal Thymeleaf UI on `ai-agents` (and `ai-rag`), so that chat-like use cases feel real, with other demos REST+Swagger only.

### Quality & reference value

25. As a maintainer, I want one README per demo explaining what it shows, how to run it, and provider setup, so that each module is independently understandable as a reference.
26. As a maintainer, I want an exhaustive test suite for core AI logic and smoke tests for UI, so that the references are trustworthy.
27. As a maintainer, I want the domain glossary and decisions (CONTEXT.md + ADRs) kept in sync, so that future work speaks the same vocabulary and respects settled trade-offs.

## Implementation Decisions

- **Build**: Maven monorepo; parent POM uses `spring-boot-starter-parent` 4.1.1 and imports `spring-ai-bom` 2.0.1. Spring AI 2.0.x is compatible with Spring Boot 4.0.x/4.1.x; 4.1.1 is the current stable baseline and the Spring AI 2.0.1 artifacts are Boot 4.1-aligned.
- **Language/runtime**: Java 25 toolchain; root package `dev.prasadgaikwad.springaiapps.*` (Java forbids hyphens, so the package differs from the Maven `spring-ai-apps` artifactId/groupId).
- **Modules**: `ai-common` (shared) + `ai-rag`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured` (demos). `ai-rag` is the sole demo built end-to-end now; the other five are scaffolded to green and deepened later.
- **`ai-common` responsibilities**: Vector store config, Eval harness, prompt templates, common REST/error handling. Observability config is deliberately NOT here — it stays isolated in `ai-observability`.
- **Provider portability**: per-demo provider chosen via `spring.profiles.active`; default `ollama` (zero-key), documented `openai` and `anthropic` alternatives. `spring.ai.*.api-key` wired from `${KEY}` placeholders, seeded from `.env.example` and git-ignored local overrides.
- **Vector store**: PGVector via Postgres on Docker, from a single top-level Compose file. Embeddings are provider-coupled: changing the active provider of a Vector-store-backed demo requires re-ingesting with that provider's embedding model (see ADR-0001).
- **ai-rag architecture**: full ETL ingestion (read → split/transform → embed → store) plus a query path (retrieval → rerank → generate) with hybrid search, exposed over REST + OpenAPI, with a minimal Thymeleaf chat UI.
- **API contract**: each demo exposes a REST contract; every demo includes OpenAPI/Swagger. Thymeleaf UI is limited to `ai-agents` and `ai-rag` (ADR-0002 rationale).
- **Observability**: `ai-observability` hosts the all-local DuckDB trace sink and query endpoints consuming Spring AI's Micrometer observations (`gen_ai.client.operation`, `db.vector.client.operation`, tool/advisor/chat-client spans); other demos rely on Spring Boot's auto-configured tracing only.

## Testing Decisions

- **What makes a good test here**: test externally-observable behavior through the published REST contract, not internal orchestration detail. For a RAG demo, the meaningful assertion is "ingest a document → ask a question → receive a grounded answer," through the public endpoint, using a pointed/mocked ChatModel so the test is deterministic.
- **Primary seam (one per demo)**: a `@SpringBootTest` full-context test per Demo module that drives the whole slice (HTTP controller → ETL → Vector store → retrieval → rerank → generate) through the public REST API. This single seam generalizes across all six demos because each shares the same anatomy (HTTP in, ChatModel/Vector store in the middle, REST/JSON out).
- **Secondary seam (shared `ai-common`)**: focused unit tests on the Eval harness, which is pure logic (grades Q&A pairs → produces a regression-style report) reused by other modules; it merits its own seam because it has no REST contract of its own and is deterministic to test in isolation.
- **UI/testing scope**: Thymeleaf pages get a context-load smoke test only (exhaustive core, smoke UI, as decided).
- **Modules tested now**: `ai-common` + `ai-rag`. The remaining demo skeletons get context-load smoke tests.
- **Prior art**: greenfield — no prior tests exist. The chosen blanket `@SpringBootTest` seam becomes the prior art the other five demos follow.

## Out of Scope

- Building out the five remaining demo modules beyond green-thumbing skeletons (`ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`) — they are named and scaffolded only.
- Full web UI everywhere; the UI is limited to `ai-agents` and `ai-rag`.
- Vendor-specific features locked to a single provider; the design is provider-portable.
- Deployment/CI pipeline, container image builds, and cloud hosting.

## Further Notes

- Decisions are recorded as ADRs in `docs/adr/`: `0001` (provider portability via profiles with Ollama default) and `0002` (monorepo with a shared `ai-common` deep module).
- The domain glossary lives in `CONTEXT.md` and must be kept in sync as demos deepen.
- Sequence: scaffold parent + `ai-common` + all six module skeletons so the monorepo compiles green, then deepen `ai-rag` end-to-end; the other demos are deepened in later work.
