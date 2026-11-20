# ai-observability

An embedded DuckDB sink for Micrometer observations in this application, plus REST insertion and queries. Package: `dev.prasadgaikwad.springaiapps.observability`. All sink configuration and persistence code remain local to this Demo module.

## Run and storage

```bash
mvn -pl ai-common -am install -DskipTests
mvn -pl ai-observability spring-boot:run
```

No external database is needed. `trace.duckdb.url` defaults to `jdbc:duckdb:${user.home}/.spring-ai-apps/ai-observability/traces.duckdb`, so the file is stable regardless of the application's working directory. `DuckDbConfig` creates parent directories and initializes `trace_spans` plus indexes on `trace_id`, `name`, and `start_time`. Override the property (or `TRACE_DUCKDB_URL`) to choose a different file. File-backed storage survives restart. `trace.duckdb.retention-days` (default `7`, override `TRACE_DUCKDB_RETENTION_DAYS`) deletes spans older than the cutoff at startup and after writes; there is no migration framework, so schema changes require manual handling.

The six-demo chat-profile configuration is present, but these span APIs do not themselves invoke a ChatModel. The POM includes Actuator and Micrometer APIs, but YAML no longer enables `management.tracing.*` or exposes `traces`: there is no W3C trace-context bridge and no `/actuator/traces`. Health/info/metrics remain available under `/actuator`. Use the module's explicit span/trace REST contract below for stored observations.

## Public API

| Method/path | Behavior |
| --- | --- |
| `POST /api/observability/spans` | Record a span; requires non-blank `traceId` and `name` |
| `GET /api/observability/spans` | Optional exact `traceId`, case-insensitive substring `name`; default `limit=100` |
| `GET /api/observability/traces` | Group by trace ID; default `limit=50` |
| `GET /api/observability/ping` | Module/status |

```bash
curl -s http://localhost:8080/api/observability/spans -H 'Content-Type: application/json' \
  -d '{"traceId":"example-trace","name":"manual.chat","kind":"CLIENT","startTime":"2026-11-01T12:00:00Z","endTime":"2026-11-01T12:00:01Z","tags":{"model":"example"}}'
curl -s 'http://localhost:8080/api/observability/spans?traceId=example-trace&name=chat&limit=10'
curl -s http://localhost:8080/api/observability/traces
```

Optional insertion fields are `parentSpanId`, `kind`, ISO-8601 `startTime` / `endTime`, and string-valued `tags`. Missing start/end times default to current time; missing tags become empty. Limits clamp to 1–500. Spans sort by start time **ascending**, so a limited query returns the earliest matching spans, not the latest. Trace summaries sort by aggregated minimum start time descending and contain `traceId`, `startTime`, `endTime`, `spanCount`, `names`.

## Capture and query architecture

```mermaid
flowchart TB
    Local[Observations emitted in this JVM] --> H[DuckDbObservationHandler.onStop]
    Post[POST spans] --> C[ObservabilityController]
    H --> S[TraceSpanSink: async single writer]
    C --> S
    S --> R[TraceSpanRepository]
    R --> DB[(DuckDB trace_spans)]
    Get[GET spans / traces] --> C
    C --> R
```

```mermaid
classDiagram
    ObservationHandler <|.. DuckDbObservationHandler
    DuckDbObservationHandler --> TraceSpanSink
    ObservabilityController --> TraceSpanSink
    ObservabilityController --> TraceSpanRepository
    TraceSpanSink --> TraceSpanRepository
    TraceSpanRepository --> TraceSpan
    DuckDbConfig --> DataSource
    TraceSpanRepository --> DataSource
    class TraceSpan {
        String id
        String traceId
        String parentSpanId
        String name
        Instant startTime
        Instant endTime
        Long durationMs
        String serviceName
        String status
        Map tags
    }
```

`supportsContext` returns true, so the handler accepts all observations offered by this application's registry, including Spring AI operations and ordinary application observations. On stop it copies all key/value tags and adds the error class name when present. Handler state uses weak keys, so a context that never stops does not pin it in memory; a stop without a start still writes a zero-length span. Rows are queued to `TraceSpanSink`, a single daemon writer; `POST /api/observability/spans` calls `flush()` before responding, while observation writes are flushed at shutdown. The service name comes from `ai.observability.service-name` (default `ai-observability`), tags are stored as JSON, and duration is clamped to non-negative milliseconds. The redundant read-after-write was removed; the built span is returned directly.

## Correlation and timing semantics

Automatic capture assigns a generated UUID span ID to every started observation and records the actual `onStart` time. The first observed span in a chain starts a generated trace ID; descendants inherit the nearest observed ancestor's trace ID and store that ancestor's span ID as `parentSpanId`. Each span is stored under its assigned span ID, so parent references point at real rows.

If a parent was never observed/started, the child starts a new trace. If an observation stops without a corresponding start, the handler still stores it with a generated identity and a zero-length interval. Error-bearing observations store status `ERROR` and an `error` tag with the exception class; successful observations store `OK`. Manual POST spans keep their caller-chosen trace IDs and timestamps and are stored as `OK`.

This is **in-process observation correlation, not distributed tracing**. Micrometer Tracing span IDs require a tracer bridge (for example Brave or OpenTelemetry); this module intentionally avoids adding that dependency, so correlation is derived from observation parent links instead. IDs are not W3C trace-context IDs and are not propagated to other Demo JVMs. The module is not an OpenTelemetry collector and does not export spans. Manual POST remains the way to inject spans with external trace IDs. Persistence is asynchronous through a bounded single-writer queue; REST writes flush before returning. Retention prunes by `start_time` at startup and after flushes; there is no migration framework. [ADR-0005](../docs/adr/0005-all-local-observability-in-ai-observability-only.md) records the local-sink decision separately from these implementation limits.

## Tests and learnings

`mvn -pl ai-observability -am test` covers manual REST insert/query, required fields, tag serialization/immutability, context loading using DuckDB, configured service name, retention pruning, and automatic handler parent/child correlation, start-to-stop timing, error status, and a stop-without-start regression. It does not establish distributed trace-context propagation or cross-process collection. See [implementation learnings](../docs/learnings/implementation-learnings.md).
