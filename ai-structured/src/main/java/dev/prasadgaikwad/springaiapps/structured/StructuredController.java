package dev.prasadgaikwad.springaiapps.structured;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the {@code ai-structured} Demo module.
 *
 * <p>{@code POST /api/structured/extract} maps free text to a typed POJO (candidate or
 * order) and returns it with any bean-validation violations.
 */
@RestController
@RequestMapping("/api/structured")
public class StructuredController {

    private final StructuredExtractionService service;

    public StructuredController(StructuredExtractionService service) {
        this.service = service;
    }

    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.OK)
    public ExtractedResult<?> extract(@RequestBody ExtractionRequest request) {
        return service.extract(request);
    }

    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public PingResponse ping() {
        return new PingResponse("ai-structured", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
