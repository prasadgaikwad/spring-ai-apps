package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(TraceSpan.class);

    public TraceSpan {
        tags = sanitize(tags);
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
                log.warn("Malformed tags JSON for trace {}; using empty tags", traceId, ex);
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
            log.warn("Could not serialize span tags; using empty JSON object", ex);
            return "{}";
        }
    }

    private static Map<String, String> sanitize(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
