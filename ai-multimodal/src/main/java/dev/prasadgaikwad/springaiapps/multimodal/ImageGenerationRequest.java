package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to generate an image from a text prompt (image modality). {@code width} and
 * {@code height} are applied independently when present and must be positive.
 */
public record ImageGenerationRequest(String prompt, Integer width, Integer height) {
}
