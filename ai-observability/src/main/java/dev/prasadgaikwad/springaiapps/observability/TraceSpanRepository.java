package dev.prasadgaikwad.springaiapps.observability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * All-local DuckDB repository for trace spans.
 *
 * <p>Writes spans captured by the observation handler and serves query endpoints with
 * simple filters. Stays entirely inside ai-observability.
 */
@Repository
public class TraceSpanRepository {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public TraceSpanRepository(DataSource traceDataSource) {
        this.dataSource = traceDataSource;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /** Persists a span (insert or replace). */
    public TraceSpan save(String traceId, String parentSpanId, String name, String kind,
                          Instant startTime, Instant endTime, Map<String, String> tags) {
        String id = UUID.randomUUID().toString();
        String tagsJson = TraceSpan.toTagsJson(tags, objectMapper);
        String sql = """
                INSERT OR REPLACE INTO trace_spans
                (id, trace_id, parent_span_id, name, kind, start_time, end_time, duration_ms, service_name, status, tags)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, traceId);
            ps.setString(3, parentSpanId);
            ps.setString(4, name);
            ps.setString(5, kind);
            ps.setTimestamp(6, Timestamp.from(startTime));
            Instant end = endTime == null ? Instant.now() : endTime;
            ps.setTimestamp(7, Timestamp.from(end));
            ps.setLong(8, Math.max(0, end.toEpochMilli() - startTime.toEpochMilli()));
            ps.setString(9, "ai-observability");
            ps.setString(10, "OK");
            ps.setString(11, tagsJson);
            ps.executeUpdate();
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to persist trace span", ex);
        }
        return findById(id);
    }

    /** Lists spans with optional filters, ordered by start time ascending. */
    public List<TraceSpan> find(String traceId, String nameContains, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM trace_spans WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(traceId)) {
            sql.append(" AND trace_id = ?");
            params.add(traceId);
        }
        if (StringUtils.hasText(nameContains)) {
            sql.append(" AND lower(name) LIKE ?");
            params.add("%" + nameContains.toLowerCase() + "%");
        }
        sql.append(" ORDER BY start_time ASC LIMIT ?");
        params.add(Math.max(1, Math.min(limit, 500)));
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TraceSpan> spans = new ArrayList<>();
                while (rs.next()) {
                    spans.add(mapRow(rs));
                }
                return spans;
            }
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to query trace spans", ex);
        }
    }

    /** Lists distinct traces with summary info, ordered by most recent start. */
    public List<Map<String, Object>> findTraces(int limit) {
        String sql = """
                SELECT trace_id, MIN(start_time) AS start_time, MAX(end_time) AS end_time,
                       COUNT(*) AS span_count, STRING_AGG(DISTINCT name, ',') AS names
                FROM trace_spans
                GROUP BY trace_id
                ORDER BY start_time DESC
                LIMIT ?
                """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, Math.min(limit, 500)));
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> traces = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> trace = new LinkedHashMap<>();
                    trace.put("traceId", rs.getString("trace_id"));
                    trace.put("startTime", rs.getTimestamp("start_time").toInstant());
                    trace.put("endTime", rs.getTimestamp("end_time").toInstant());
                    trace.put("spanCount", rs.getLong("span_count"));
                    trace.put("names", rs.getString("names"));
                    traces.add(trace);
                }
                return traces;
            }
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to query traces", ex);
        }
    }

    private TraceSpan findById(String id) {
        String sql = "SELECT * FROM trace_spans WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        catch (SQLException ex) {
            throw new IllegalStateException("Failed to fetch trace span", ex);
        }
        throw new IllegalStateException("Span not found after insert: " + id);
    }

    private TraceSpan mapRow(ResultSet rs) throws SQLException {
        Instant endTime = null;
        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) {
            endTime = end.toInstant();
        }
        Long duration = rs.getObject("duration_ms", Long.class);
        return TraceSpan.from(rs.getString("id"), rs.getString("trace_id"), rs.getString("parent_span_id"),
                rs.getString("name"), rs.getString("kind"), rs.getTimestamp("start_time").toInstant(),
                endTime, duration, rs.getString("service_name"), rs.getString("status"), rs.getString("tags"),
                objectMapper);
    }
}
