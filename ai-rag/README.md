# ai-rag

Retrieval-Augmented Generation demo with grounding and a chat UI.

## Features

- **Hybrid search** — Semantic search (embeddings) combined with PostgreSQL full-text search, merged via Reciprocal Rank Fusion.
- **Reranking** — Post-retrieval reranking for improved relevance.
- **Grounding** — Answers are constrained to retrieved context with a grounding guard.
- **ETL pipeline** — Ingest documents via REST, chunk, embed, and store in PGVector.
- **Thymeleaf UI** — Minimal chat interface at `/chat`.

## APIs

- `POST /api/rag/ingest` — Ingest a document
- `POST /api/rag/query` — Query with retrieval + reranking + generation
- OpenAPI docs at `/v3/api-docs`

## Configuration

Defaults to `ollama`. Requires Postgres with PGVector for vector storage (see `docker-compose.yml` at repo root).

```bash
docker compose up -d
mvn -pl ai-rag -am spring-boot:run
```

## Testing

Integration tests use Testcontainers with PGVector and mocked Chat/Embedding models to exercise the full pipeline deterministically.
