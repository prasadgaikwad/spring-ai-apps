package dev.prasadgaikwad.springaiapps.structured;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-structured} Demo module.
 *
 * <p>Replaced by typed-extraction and validation flows in later work (issue #5).
 */
@RestController
@RequestMapping("/api/structured")
public class StructuredScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-structured", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
