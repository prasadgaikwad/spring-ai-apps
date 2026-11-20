package dev.prasadgaikwad.springaiapps.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Focused retention test for the all-local DuckDB sink. */
class TraceSpanRepositoryTest {

    private TraceSpanRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DuckDbConfig().traceDataSource(new TraceProperties("jdbc:duckdb:", 7));
        this.repository = new TraceSpanRepository(dataSource, new ObservabilityProperties("test-service"));
    }

    @Test
    void deleteOlderThanRemovesOnlyStaleSpans() {
        Instant now = Instant.now();
        this.repository.save("stale", "trace", null, "old.span", "SPAN", now.minus(Duration.ofDays(30)),
                now.minus(Duration.ofDays(30)), Map.of(), "OK");
        this.repository.save("fresh", "trace", null, "new.span", "SPAN", now, now, Map.of(), "OK");

        int deleted = this.repository.deleteOlderThan(now.minus(Duration.ofDays(7)));

        assertThat(deleted).isEqualTo(1);
        assertThat(this.repository.find(null, null, 10)).extracting(TraceSpan::id).containsExactly("fresh");
    }
}
