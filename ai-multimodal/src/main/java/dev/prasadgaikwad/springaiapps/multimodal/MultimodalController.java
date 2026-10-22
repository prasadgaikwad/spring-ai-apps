package dev.prasadgaikwad.springaiapps.multimodal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the {@code ai-multimodal} Demo module — all four modalities.
 *
 * <ul>
 *   <li>{@code POST /api/multimodal/image} — image generation</li>
 *   <li>{@code POST /api/multimodal/vision} — image understanding</li>
 *   <li>{@code POST /api/multimodal/speech} — text-to-speech</li>
 *   <li>{@code POST /api/multimodal/transcribe} — speech-to-text</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {

    private final MultimodalService service;

    public MultimodalController(MultimodalService service) {
        this.service = service;
    }

    @PostMapping("/image")
    @ResponseStatus(HttpStatus.OK)
    public GeneratedImage generateImage(@RequestBody ImageGenerationRequest request) {
        return service.generateImage(request);
    }

    @PostMapping("/vision")
    @ResponseStatus(HttpStatus.OK)
    public VisionResponse describeImage(@RequestBody VisionRequest request) {
        return new VisionResponse(service.describeImage(request));
    }

    @PostMapping("/speech")
    @ResponseStatus(HttpStatus.OK)
    public SpeechResponse synthesizeSpeech(@RequestBody SpeechRequest request) {
        return service.synthesizeSpeech(request);
    }

    @PostMapping("/transcribe")
    @ResponseStatus(HttpStatus.OK)
    public TranscriptionResponse transcribe(@RequestBody TranscriptionRequest request) {
        return service.transcribe(request);
    }

    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public PingResponse ping() {
        return new PingResponse("ai-multimodal", "ok");
    }

    public record VisionResponse(String answer) {
    }

    public record PingResponse(String module, String status) {
    }
}
