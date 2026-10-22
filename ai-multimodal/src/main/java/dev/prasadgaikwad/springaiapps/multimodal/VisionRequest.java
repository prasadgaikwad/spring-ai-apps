package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to describe/answer a question about an image (vision modality). The image is
 * supplied as a base64 payload plus its MIME type, so the REST body is self-contained.
 */
public record VisionRequest(String question, String imageBase64, String mimeType) {
}
