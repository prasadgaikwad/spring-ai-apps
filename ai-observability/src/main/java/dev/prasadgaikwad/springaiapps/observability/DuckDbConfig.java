package dev.prasadgaikwad.springaiapps.observability;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All-local DuckDB configuration scoped strictly to ai-observability.
 *
 * <p>Observability config lives only here per ADR/issue #5; no DuckDB wiring leaks to
 * ai-common or other demos. Uses a file-backed DuckDB by default so traces survive
 * restarts, but works with no external services.
 */
@Configuration(proxyBeanMethods = false)
public class DuckDbConfig {

    @Bean
    public DataSource traceDataSource(TraceProperties properties) {
        DuckDbConfig.ensureDbDirectory(properties.url());
        DataSource ds = DataSourceBuilder.create()
                .driverClassName("org.duckdb.DuckDBDriver")
                .url(properties.url())
                .build();
        initializeSchema(ds);
        return ds;
    }

    private void initializeSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS trace_spans (
                        id VARCHAR PRIMARY KEY,
                        trace_id VARCHAR NOT NULL,
                        parent_span_id VARCHAR,
                        name VARCHAR NOT NULL,
                        kind VARCHAR,
                        start_time TIMESTAMP NOT NULL,
                        end_time TIMESTAMP,
                        duration_ms BIGINT,
                        service_name VARCHAR,
                        status VARCHAR,
                        tags JSON
                    )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trace_spans_trace_id ON trace_spans(trace_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trace_spans_name ON trace_spans(name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trace_spans_start_time ON trace_spans(start_time)");
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize DuckDB trace schema", ex);
        }
    }

    /** Ensures a directory exists for file-backed DuckDB databases. */
    public static void ensureDbDirectory(String url) {
        if (url == null) {
            return;
        }
        if (url.startsWith("jdbc:duckdb:")) {
            String path = url.substring("jdbc:duckdb:".length());
            if (!":memory:".equals(path) && !path.startsWith(":memory:")) {
                java.io.File file = new java.io.File(path);
                java.io.File dir = file.getParentFile();
                if (dir != null && !dir.exists()) {
                    dir.mkdirs();
                }
            }
        }
    }
}
