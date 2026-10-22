package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to generate an image from a text prompt (image modality).
 */
public record ImageGenerationRequest(String prompt, Integer width, Integer height) {
}
