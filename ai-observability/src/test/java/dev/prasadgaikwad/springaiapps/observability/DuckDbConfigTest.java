package dev.prasadgaikwad.springaiapps.observability;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Focused check for the file-backed DuckDB lifecycle scoped to {@code ai-observability}. */
class DuckDbConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void fileBackedDatabasePersistsSpansAcrossDataSources() throws Exception {
        Path dbPath = this.tempDir.resolve("nested/traces.duckdb");
        String url = "jdbc:duckdb:" + dbPath;
        DuckDbConfig config = new DuckDbConfig();
        Instant now = Instant.now();

        DataSource first = config.traceDataSource(new TraceProperties(url, 7));
        try {
            new TraceSpanRepository(first, new ObservabilityProperties("file-test"))
                    .save("file-span", "trace-file", null, "file.span", "SPAN", now, now, Map.of(), "OK");
        }
        finally {
            close(first);
        }

        assertThat(dbPath.getParent()).exists();
        assertThat(dbPath).exists();

        DataSource second = config.traceDataSource(new TraceProperties(url, 7));
        try {
            TraceSpanRepository repository = new TraceSpanRepository(second,
                    new ObservabilityProperties("file-test"));
            assertThat(repository.find("trace-file", null, 10)).extracting(TraceSpan::id)
                    .containsExactly("file-span");
        }
        finally {
            close(second);
        }
    }

    private static void close(DataSource dataSource) throws Exception {
        if (dataSource instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
