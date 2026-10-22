package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to synthesize speech from text (audio TTS modality).
 */
public record SpeechRequest(String text, String voice) {
}
