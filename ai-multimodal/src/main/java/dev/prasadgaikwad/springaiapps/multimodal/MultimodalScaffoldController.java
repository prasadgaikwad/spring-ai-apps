package dev.prasadgaikwad.springaiapps.multimodal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-multimodal} Demo module.
 *
 * <p>Replaced by image-generation, vision, TTS, and transcription flows in later
 * work (issue #5).
 */
@RestController
@RequestMapping("/api/multimodal")
public class MultimodalScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-multimodal", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
