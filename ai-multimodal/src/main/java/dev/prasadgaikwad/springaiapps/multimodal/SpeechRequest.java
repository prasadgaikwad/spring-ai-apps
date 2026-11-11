package dev.prasadgaikwad.springaiapps.multimodal;

/**
 * Request to synthesize speech from text (audio TTS modality). All optional fields are
 * forwarded to the provider; providers that do not support a value may reject it
 * upstream.
 */
public record SpeechRequest(String text, String voice, String format, Double speed) {
}
