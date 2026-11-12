# ADR 0005: All-local Observability in ai-observability only

## Status
Accepted

## Context
We need trace/observation visibility for demos without introducing external dependencies or leaking observability plumbing into shared modules. Spring AI 2.0 provides observation APIs; we need a local sink for development/reference.

## Decision
- **Isolation**: Observability config (DuckDB sink, handlers, schema, REST) lives **only** in `ai-observability`. Do not add to `ai-common` or other demos.
- **Storage**: Embedded/file-backed DuckDB at `./data/ai-observability-traces.duckdb` (relative to the application working directory) with schema/indexes created on startup. Configure via `trace.duckdb.url`.
- **Capture**: `DuckDbObservationHandler` accepts all local observation contexts, including Spring AI operation/tool/advisor/chat-client observations when emitted. Errors are copied into tags.
- **APIs**: REST endpoints to POST spans and GET traces/spans for local inspection.

## Consequences
- No mandatory dependencies in other modules
- Clear separation of concerns
- Local-only by design

## Alternatives and current limits

An external tracing backend or shared sink in every demo would add operational dependencies and cross-module coupling. Embedded DuckDB keeps the reference locally inspectable and the dependency isolated, at the cost of no automatic cross-JVM collection or distributed tracing backend.

The automatic handler now assigns generated in-process trace/span IDs, captures start and stop times, stores the nearest observed ancestor's span ID as `parentSpanId`, and records `ERROR` status for error-bearing observations. This improves local correlation and timing but is still not distributed tracing: Micrometer Tracing span IDs would require a Brave/OpenTelemetry bridge, which would add a dependency this demo deliberately avoids; IDs are not W3C trace-context IDs, are not propagated across Demo JVMs, and there is no OpenTelemetry export. Manual REST insertion can carry caller-selected trace IDs and real timestamps. Persistence is synchronous, schema initialization uses create-if-absent, and there is no retention/migration system.

## References
- Issue #20, PR #22
- [ai-observability behavior and API](../../ai-observability/README.md)
- [Implementation learnings](../learnings/implementation-learnings.md)
