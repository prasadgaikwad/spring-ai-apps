package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-rag} Demo module.
 *
 * <p>Replaced by the full ingestion and query paths in later work (issues #2 and #3).
 * Exists so the module has a runnable HTTP contract and a smoke-testable endpoint.
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
