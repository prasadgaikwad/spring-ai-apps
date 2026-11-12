package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationView;

/**
 * Micrometer {@link ObservationHandler} that captures Spring AI and application
 * observations into the all-local DuckDB trace sink.
 *
 * <p>Records observations for the AI-specific operations listed in #5:
 * {@code gen_ai.client.operation}, {@code db.vector.client.operation}, tool/advisor/chat-client
 * spans, plus any other observations emitted in this module — all persisted locally only.
 *
 * <p>Correlation is in-process: the first observed span starts a generated trace ID, and
 * descendants inherit the nearest observed ancestor's trace ID and reference its span ID.
 * Start time is captured in {@link #onStart}, so stored durations measure wall-clock time
 * between the observation's start and stop rather than the stop instant. Errors set the
 * stored status to {@code ERROR}. This is not W3C/distributed trace-context propagation.
 */
@Component
public class DuckDbObservationHandler implements ObservationHandler<Observation.Context> {

    private final TraceSpanRepository repository;

    private final Map<Observation.Context, SpanState> states =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public DuckDbObservationHandler(TraceSpanRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onStart(Observation.Context context) {
        SpanState parent = findParentState(context);
        String traceId = parent != null ? parent.traceId() : UUID.randomUUID().toString();
        String parentSpanId = parent != null ? parent.spanId() : null;
        this.states.put(context,
                new SpanState(UUID.randomUUID().toString(), traceId, parentSpanId, Instant.now()));
    }

    @Override
    public void onStop(Observation.Context context) {
        Instant end = Instant.now();
        SpanState state = this.states.remove(context);
        if (state == null) {
            SpanState parent = findParentState(context);
            state = new SpanState(UUID.randomUUID().toString(),
                    parent != null ? parent.traceId() : UUID.randomUUID().toString(),
                    parent != null ? parent.spanId() : null, end);
        }

        Map<String, String> tags = new LinkedHashMap<>();
        context.getAllKeyValues().forEach(kv -> tags.put(kv.getKey(), kv.getValue()));
        String status = "OK";
        if (context.getError() != null) {
            status = "ERROR";
            tags.put("error", context.getError().getClass().getSimpleName());
        }

        repository.save(state.spanId(), state.traceId(), state.parentSpanId(), context.getName(), "SPAN",
                state.startTime(), end, tags, status);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    private SpanState findParentState(Observation.Context context) {
        ObservationView parent = context.getParentObservation();
        while (parent != null) {
            SpanState state = this.states.get(parent.getContextView());
            if (state != null) {
                return state;
            }
            parent = parent.getContextView().getParentObservation();
        }
        return null;
    }

    private record SpanState(String spanId, String traceId, String parentSpanId, Instant startTime) {
    }
}
