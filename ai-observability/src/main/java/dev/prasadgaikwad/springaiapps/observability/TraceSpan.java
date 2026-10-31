package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * One stored observation/span from the all-local trace sink.
 */
public record TraceSpan(
        String id,
        String traceId,
        String parentSpanId,
        String name,
        String kind,
        Instant startTime,
        Instant endTime,
        Long durationMs,
        String serviceName,
        String status,
        Map<String, String> tags) {

    public TraceSpan {
        if (tags == null) {
            tags = Map.of();
        }
        else {
            tags = Map.copyOf(tags);
        }
    }

    public static TraceSpan from(String id, String traceId, String parentSpanId, String name, String kind,
                                 Instant startTime, Instant endTime, Long durationMs, String serviceName,
                                 String status, String tagsJson, ObjectMapper mapper) {
        Map<String, String> tags = Map.of();
        if (tagsJson != null && !tagsJson.isBlank()) {
            try {
                tags = mapper.readValue(tagsJson, new TypeReference<>() {
                });
            }
            catch (JsonProcessingException ex) {
                tags = Map.of();
            }
        }
        return new TraceSpan(id, traceId, parentSpanId, name, kind, startTime, endTime, durationMs,
                serviceName, status, tags);
    }

    public static String toTagsJson(Map<String, String> tags, ObjectMapper mapper) {
        if (tags == null || tags.isEmpty()) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(tags);
        }
        catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
