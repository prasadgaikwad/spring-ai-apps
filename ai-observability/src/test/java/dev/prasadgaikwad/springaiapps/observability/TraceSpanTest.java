package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused unit tests for the trace sink mapping/serialization — pure logic inside
 * ai-observability, isolated from the database and HTTP layers.
 */
class TraceSpanTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void tagsAreImmutableAndEmptyByDefault() {
        TraceSpan span = new TraceSpan("1", "t", null, "x", "SPAN", Instant.now(), null, 10L, "s", "OK", null);
        assertThat(span.tags()).isEmpty();
        assertThatThrownBy(() -> span.tags().put("a", "b")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toTagsJsonRoundTrips() {
        String json = TraceSpan.toTagsJson(Map.of("model", "gpt-4o", "op", "chat"), mapper);
        assertThat(json).contains("model");
        TraceSpan restored = TraceSpan.from("1", "t", null, "x", "SPAN", Instant.now(), Instant.now(), 5L, "s", "OK", json, mapper);
        assertThat(restored.tags().get("model")).isEqualTo("gpt-4o");
    }
}
