package dev.prasadgaikwad.springaiapps.observability;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Query endpoints for the all-local DuckDB trace sink.
 *
 * <ul>
 *   <li>{@code POST /api/observability/spans} — record a span (for testing/integration)</li>
 *   <li>{@code GET /api/observability/spans} — list spans with filters</li>
 *   <li>{@code GET /api/observability/traces} — list traces with summaries</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final TraceSpanRepository repository;

    public ObservabilityController(TraceSpanRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/spans")
    public TraceSpan recordSpan(@RequestBody RecordSpanRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("'name' must not be blank");
        }
        if (request.traceId() == null || request.traceId().isBlank()) {
            throw new BadRequestException("'traceId' must not be blank");
        }
        Map<String, String> tags = request.tags() == null ? Map.of() : request.tags();
        return repository.save(request.traceId(), request.parentSpanId(), request.name(), request.kind(),
                request.startTime() == null ? java.time.Instant.now() : request.startTime(),
                request.endTime(), tags);
    }

    @GetMapping("/spans")
    public List<TraceSpan> spans(@RequestParam(required = false) String traceId,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(defaultValue = "100") int limit) {
        return repository.find(traceId, name, limit);
    }

    @GetMapping("/traces")
    public List<Map<String, Object>> traces(@RequestParam(defaultValue = "50") int limit) {
        return repository.findTraces(limit);
    }

    /** Ping endpoint retained for module smoke tests. */
    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-observability", "ok");
    }

    public record RecordSpanRequest(String traceId, String parentSpanId, String name, String kind,
                                    java.time.Instant startTime, java.time.Instant endTime,
                                    Map<String, String> tags) {
    }

    public record PingResponse(String module, String status) {
    }
}
