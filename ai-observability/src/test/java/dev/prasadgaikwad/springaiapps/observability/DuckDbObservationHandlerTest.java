package dev.prasadgaikwad.springaiapps.observability;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused tests for automatic observation capture: in-process trace/parent
 * correlation, start-to-stop timing, and error status.
 */
class DuckDbObservationHandlerTest {

    private TraceSpanRepository repository;

    private TraceSpanSink sink;

    private ObservationRegistry registry;

    @BeforeEach
    void setUp() {
        TraceProperties properties = new TraceProperties("jdbc:duckdb:", 7);
        DataSource dataSource = new DuckDbConfig().traceDataSource(properties);
        this.repository = new TraceSpanRepository(dataSource, new ObservabilityProperties("ai-observability"));
        this.sink = new TraceSpanSink(this.repository, properties);
        this.registry = ObservationRegistry.create();
        this.registry.observationConfig().observationHandler(new DuckDbObservationHandler(this.sink));
    }

    @AfterEach
    void tearDown() {
        this.sink.destroy();
    }

    @Test
    void correlatesParentAndChildSpansWithMeasuredTiming() throws Exception {
        Observation parent = Observation.start("parent.operation", this.registry);
        Thread.sleep(25);
        Observation child = Observation.createNotStarted("child.operation", this.registry)
                .parentObservation(parent)
                .start();
        Thread.sleep(25);
        child.stop();
        parent.stop();
        this.sink.flush();

        List<TraceSpan> spans = this.repository.find(null, null, 10);
        assertThat(spans).hasSize(2);

        TraceSpan parentSpan = spans.stream()
                .filter(span -> span.name().equals("parent.operation"))
                .findFirst()
                .orElseThrow();
        TraceSpan childSpan = spans.stream()
                .filter(span -> span.name().equals("child.operation"))
                .findFirst()
                .orElseThrow();

        assertThat(parentSpan.traceId()).isNotEqualTo("parent.operation");
        assertThat(childSpan.traceId()).isEqualTo(parentSpan.traceId());
        assertThat(childSpan.parentSpanId()).isEqualTo(parentSpan.id());
        assertThat(parentSpan.parentSpanId()).isNull();
        assertThat(parentSpan.status()).isEqualTo("OK");
        assertThat(parentSpan.durationMs()).isGreaterThanOrEqualTo(20L);
        assertThat(childSpan.durationMs()).isGreaterThanOrEqualTo(20L);
    }

    @Test
    void recordsErrorStatusAndTag() {
        Observation observation = Observation.start("failing.operation", this.registry);
        observation.error(new IllegalStateException("boom"));
        observation.stop();
        this.sink.flush();

        TraceSpan span = this.repository.find(null, "failing.operation", 10).getFirst();
        assertThat(span.status()).isEqualTo("ERROR");
        assertThat(span.tags()).containsEntry("error", "IllegalStateException");
    }

    @Test
    void stopWithoutStartWritesZeroLengthSpanWithoutRetainingState() {
        Observation.createNotStarted("orphan.operation", this.registry).stop();
        this.sink.flush();

        List<TraceSpan> spans = this.repository.find(null, "orphan.operation", 10);
        assertThat(spans).hasSize(1);
        TraceSpan span = spans.getFirst();
        assertThat(span.id()).isNotBlank();
        assertThat(span.traceId()).isNotBlank();
        assertThat(span.parentSpanId()).isNull();
        assertThat(span.status()).isEqualTo("OK");
        assertThat(span.durationMs()).isZero();
    }
}
