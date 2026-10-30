# ai-observability

All-local observability with DuckDB trace sink and Micrometer observation capture. **Observability config lives only in this module.**

## Features

- **DuckDB trace sink** — File-backed DuckDB (`./data/ai-observability-traces.duckdb`) stores spans with indexes; no external services required.
- **Micrometer observation capture** — `ObservationHandler` captures Spring AI observations (`gen_ai.client.operation`, `db.vector.client.operation`, tool/advisor/chat-client spans) and custom observations.
- **Query endpoints** — List spans/traces with filters.

## APIs

- `POST /api/observability/spans` — Record a span
- `GET /api/observability/spans` — Query spans (`traceId`, `name`, `limit`)
- `GET /api/observability/traces` — List traces with summaries
- OpenAPI docs at `/v3/api-docs`

## Run

```bash
mvn -pl ai-observability -am spring-boot:run
```
