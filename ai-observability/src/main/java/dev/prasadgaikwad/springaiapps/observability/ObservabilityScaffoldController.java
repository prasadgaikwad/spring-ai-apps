package dev.prasadgaikwad.springaiapps.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-observability} Demo module.
 *
 * <p>Replaced by the trace query endpoints in later work (issue #5).
 */
@RestController
@RequestMapping("/api/observability")
public class ObservabilityScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-observability", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
