# spring-ai-apps

A multi-module Maven monorepo of production-grade Spring AI reference demos showcasing RAG, agents, evaluation, observability, multimodality, and structured output.

## Modules

| Module | Description |
| --- | --- |
| [`ai-common`](ai-common/README.md) | Shared infrastructure: provider-portable chat model wiring, VectorStore helpers, exceptions/error handling, and the Eval harness. |
| [`ai-rag`](ai-rag/README.md) | Retrieval-Augmented Generation with hybrid search (semantic + PostgreSQL full-text), reciprocal rank fusion, reranking, and a grounding guard. Includes a Thymeleaf chat UI. |
| [`ai-agents`](ai-agents/README.md) | Agentic flows with plain `@Tool` calling, MCP server/client integration, and a minimal Thymeleaf chat UI. |
| [`ai-eval`](ai-eval/README.md) | In-app evaluation using the shared Eval harness: grade Q&A pairs against criteria, track history, and report regressions. |
| [`ai-observability`](ai-observability/README.md) | All-local DuckDB trace sink consuming Spring AI Micrometer observations with query endpoints. |
| [`ai-multimodal`](ai-multimodal/README.md) | All four modalities: image generation, vision, text-to-speech, and audio transcription. |
| [`ai-structured`](ai-structured/README.md) | Typed extraction to validated POJOs using Spring AI structured output and Bean Validation. |

## Quick Start

### Prerequisites

- Java 25 (Temurin)
- Maven 3.9+
- Docker (for Postgres/PGVector when running `ai-rag`)

### Build & Test

```bash
# Compile everything
mvn -DskipTests compile

# Run all tests
mvn test
```

### Provider Profiles

All demos default to `ollama` (zero-key local). Switch profiles via `spring.profiles.active` or `SPRING_PROFILES_ACTIVE`:

| Profile | Models | Notes |
| --- | --- | --- |
| `ollama` (default) | Local Ollama | No API keys required. Ensure Ollama is running locally. |
| `openai` | OpenAI models | Requires `OPENAI_API_KEY`. |
| `anthropic` | Anthropic models | Requires `ANTHROPIC_API_KEY`. |

Copy `.env.example` to `.env` (git-ignored) and set keys as needed.

### Running a Demo

```bash
# Run ai-rag (needs ai-common built)
mvn -pl ai-rag -am spring-boot:run

# Run ai-agents
mvn -pl ai-agents -am spring-boot:run
```

Each demo exposes:
- REST APIs under `/api/<module>/...`
- OpenAPI docs at `/v3/api-docs` (Swagger UI at `/swagger-ui.html` where configured)
- Health via Spring Boot Actuator where enabled

## Architecture

- **Parent**: `spring-boot-starter-parent` 4.1.1, `spring-ai-bom` 2.0.1
- **Root package**: `dev.prasadgaikwad.springaiapps.*` (Java forbids hyphens in package names)
- **Provider portability**: Controlled via profiles; see [ADR-0001](docs/adr/0001-provider-portability-profiles-ollama-default.md)
- **Shared plumbing**: Lives in `ai-common`; see [ADR-0002](docs/adr/0002-monorepo-shared-ai-common-module.md)
- **Observability**: All-local DuckDB trace sink and Micrometer observation config live only in `ai-observability`.

## Documentation

- [CONTEXT.md](CONTEXT.md) — Domain glossary (use vocabulary verbatim)
- [docs/0001-spring-ai-apps-core-and-ai-rag-spec.md](docs/0001-spring-ai-apps-core-and-ai-rag-spec.md) — Feature specification
- [docs/adr/](docs/adr/) — Architecture Decision Records

## License

See repository for license information.
