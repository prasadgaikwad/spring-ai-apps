package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint for the {@code ai-rag} Demo module.
 *
 * <p>The feature surfaces are the ingestion path ({@code POST /api/rag/ingest}) and the
 * query path ({@code POST /api/rag/query}, plus the {@code /chat} UI); this endpoint
 * stays as a trivial smoke-probe of the running application.
 */
@RestController
@RequestMapping("/api/rag")
public class RagScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-rag", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
