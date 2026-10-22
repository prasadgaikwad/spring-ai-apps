package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to transcribe speech to text (audio transcription modality). The audio is
 * supplied as a base64 payload.
 */
public record TranscriptionRequest(String audioBase64, String mimeType) {
}
