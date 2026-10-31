package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

/**
 * Micrometer {@link ObservationHandler} that captures Spring AI and application
 * observations into the all-local DuckDB trace sink.
 *
 * <p>Records observations for the AI-specific operations listed in #5:
 * {@code gen_ai.client.operation}, {@code db.vector.client.operation}, tool/advisor/chat-client
 * spans, plus any other observations emitted in this module — all persisted locally only.
 */
@Component
public class DuckDbObservationHandler implements ObservationHandler<Observation.Context> {

    private final TraceSpanRepository repository;

    public DuckDbObservationHandler(TraceSpanRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onStop(Observation.Context context) {
        String traceId = context.getName() != null ? context.getName() : UUID.randomUUID().toString();
        String parent = null;
        if (context.getParentObservation() != null) {
            parent = context.getParentObservation().getContextView().getName();
        }

        Map<String, String> tags = new LinkedHashMap<>();
        context.getAllKeyValues().forEach(kv -> tags.put(kv.getKey(), kv.getValue()));
        if (context.getError() != null) {
            tags.put("error", context.getError().getClass().getSimpleName());
        }

        repository.save(traceId, parent, context.getName(), "SPAN", Instant.now(), Instant.now(), tags);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
