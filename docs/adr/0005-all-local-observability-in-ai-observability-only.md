# ADR 0005: All-local Observability in ai-observability only

## Status
Accepted

## Context
We need trace/observation visibility for demos without introducing external dependencies or leaking observability plumbing into shared modules. Spring AI 2.0 provides observation APIs; we need a local sink for development/reference.

## Decision
- **Isolation**: Observability config (DuckDB sink, handlers, schema, REST) lives **only** in `ai-observability`. Do not add to `ai-common` or other demos.
- **Storage**: Embedded/file-backed DuckDB at `./data/observability/traces.duckdb` with schema/indexes created on startup.
- **Capture**: `DuckDbObservationHandler` captures `gen_ai.client.*`, `db.vector.client.*`, tool/advisor/chat-client events, and errors.
- **APIs**: REST endpoints to POST spans and GET traces/spans for local inspection.

## Consequences
- No mandatory dependencies in other modules
- Clear separation of concerns
- Local-only by design

## References
- Issue #20, PR #22
- `ai-observability/`
EOF
