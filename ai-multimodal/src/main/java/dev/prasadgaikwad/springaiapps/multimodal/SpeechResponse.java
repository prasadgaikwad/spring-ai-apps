package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Synthesized speech returned as a base64 audio payload plus its MIME type.
 */
public record SpeechResponse(String audioBase64, String mimeType, int byteLength) {
}
