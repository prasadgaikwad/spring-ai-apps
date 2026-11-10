package dev.prasadgaikwad.springaiapps.multimodal;

import java.util.Base64;
import java.util.Locale;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.metadata.OpenAiImageGenerationMetadata;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * The four multimodal flows, each backed by the provider-specific Spring AI model:
 *
 * <ul>
 *   <li><b>image generation</b> — {@link ImageModel}</li>
 *   <li><b>vision</b> — the {@link ChatModel}, via a multimodal {@link UserMessage}</li>
 *   <li><b>speech (TTS)</b> — {@link TextToSpeechModel}</li>
 *   <li><b>transcription</b> — {@link TranscriptionModel}</li>
 * </ul>
 *
 * <p>Models that a provider may not offer (Ollama has no image/TTS/transcription) are
 * injected through {@link ObjectProvider}s so the context still loads on the zero-key
 * default profile; the corresponding endpoint then fails with a clear {@link BadRequestException}
 * telling the caller to switch profiles.
 */
@Service
public class MultimodalService {

    private final ObjectProvider<ImageModel> imageModel;
    private final ObjectProvider<TextToSpeechModel> speechModel;
    private final ObjectProvider<TranscriptionModel> transcriptionModel;
    private final ChatClient visionClient;

    public MultimodalService(ObjectProvider<ImageModel> imageModel,
                             ObjectProvider<TextToSpeechModel> speechModel,
                             ObjectProvider<TranscriptionModel> transcriptionModel,
                             ChatModel chatModel) {
        this.imageModel = imageModel;
        this.speechModel = speechModel;
        this.transcriptionModel = transcriptionModel;
        this.visionClient = ChatClient.builder(chatModel)
                .defaultSystem("You are a precise vision assistant. Answer the user's question about the image "
                        + "in one or two sentences, grounded only in what is visible.")
                .build();
    }

    /** Generates an image from a text prompt. */
    public GeneratedImage generateImage(ImageGenerationRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new BadRequestException("'prompt' must not be blank");
        }
        ImageModel model = require(imageModel, "image generation", "an OpenAI profile");
        var optionsBuilder = ImageOptionsBuilder.builder().n(1);
        if (request.width() != null) {
            requirePositive(request.width(), "width");
            optionsBuilder.width(request.width());
        }
        if (request.height() != null) {
            requirePositive(request.height(), "height");
            optionsBuilder.height(request.height());
        }
        ImageResponse response = model.call(new ImagePrompt(request.prompt(), optionsBuilder.build()));
        ImageGeneration generation = firstImage(response);
        if (generation == null || generation.getOutput() == null) {
            throw new BadRequestException("The image model returned no image");
        }
        var image = generation.getOutput();
        return new GeneratedImage(image.getUrl(), image.getB64Json(), revisedPrompt(generation));
    }

    /** Answers a question about a supplied image. */
    public String describeImage(VisionRequest request) {
        if (request == null || !StringUtils.hasText(request.question()) || !StringUtils.hasText(request.imageBase64())) {
            throw new BadRequestException("'question' and 'imageBase64' must not be blank");
        }
        MimeType mimeType = parseImageMimeType(request.mimeType());
        byte[] bytes = decodeBase64(request.imageBase64(), "imageBase64");
        Media media = new Media(mimeType, new ByteArrayResource(bytes));
        UserMessage message = UserMessage.builder()
                .text(request.question())
                .media(media)
                .build();
        try {
            String answer = visionClient.prompt().messages(message).call().content();
            if (!StringUtils.hasText(answer)) {
                throw new BadRequestException("The vision model returned no answer");
            }
            return answer;
        }
        catch (BadRequestException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new BadRequestException("Vision request failed: " + ex.getMessage());
        }
    }

    /** Synthesizes speech from text. */
    public SpeechResponse synthesizeSpeech(SpeechRequest request) {
        if (request == null || !StringUtils.hasText(request.text())) {
            throw new BadRequestException("'text' must not be blank");
        }
        String voice = optionalText(request.voice());
        String format = optionalText(request.format());
        if (format != null) {
            format = format.toLowerCase(Locale.ROOT);
        }
        if (request.speed() != null && request.speed() <= 0) {
            throw new BadRequestException("'speed' must be positive");
        }
        TextToSpeechModel model = require(speechModel, "speech synthesis", "an OpenAI profile");
        TextToSpeechOptions.Builder options = TextToSpeechOptions.builder();
        if (voice != null) {
            options.voice(voice);
        }
        if (format != null) {
            options.format(format);
        }
        if (request.speed() != null) {
            options.speed(request.speed());
        }
        TextToSpeechResponse response = model.call(new TextToSpeechPrompt(request.text(), options.build()));
        Speech speech = firstSpeech(response);
        byte[] audio = (speech != null) ? speech.getOutput() : null;
        if (audio == null || audio.length == 0) {
            throw new BadRequestException("The speech model returned no audio");
        }
        return new SpeechResponse(Base64.getEncoder().encodeToString(audio), audioMimeType(format), audio.length);
    }

    /** Transcribes speech to text. */
    public TranscriptionResponse transcribe(TranscriptionRequest request) {
        if (request == null || !StringUtils.hasText(request.audioBase64())) {
            throw new BadRequestException("'audioBase64' must not be blank");
        }
        MimeType mimeType = parseAudioMimeType(request.mimeType());
        TranscriptionModel model = require(transcriptionModel, "audio transcription", "an OpenAI profile");
        byte[] bytes = decodeBase64(request.audioBase64(), "audioBase64");
        String text = model.transcribe(new AudioResource(bytes, audioFilename(mimeType)));
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException("The transcription model returned no text");
        }
        return new TranscriptionResponse(text);
    }

    private <T> T require(ObjectProvider<T> provider, String capability, String suggestion) {
        T model = provider.getIfAvailable();
        if (model == null) {
            throw new BadRequestException("No " + capability + " model is configured for the active profile; "
                    + "run with " + suggestion + " to enable it");
        }
        return model;
    }

    private MimeType parseImageMimeType(String mimeType) {
        MimeType parsed = parseMimeType(mimeType);
        if (!"image".equals(parsed.getType())) {
            throw new BadRequestException("'mimeType' must be an image type");
        }
        return parsed;
    }

    private MimeType parseAudioMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return MimeType.valueOf("audio/mpeg");
        }
        MimeType parsed = parseMimeType(mimeType);
        if (!"audio".equals(parsed.getType())) {
            throw new BadRequestException("'mimeType' must be an audio type");
        }
        return parsed;
    }

    private MimeType parseMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        try {
            return MimeTypeUtils.parseMimeType(mimeType);
        }
        catch (Exception ex) {
            throw new BadRequestException("Invalid mimeType: " + mimeType);
        }
    }

    private String revisedPrompt(ImageGeneration generation) {
        if (generation.getMetadata() instanceof OpenAiImageGenerationMetadata metadata) {
            return metadata.getRevisedPrompt();
        }
        return null;
    }

    private ImageGeneration firstImage(ImageResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        return response.getResult();
    }

    private Speech firstSpeech(TextToSpeechResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        return response.getResult();
    }

    private String optionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void requirePositive(Integer value, String field) {
        if (value <= 0) {
            throw new BadRequestException("'" + field + "' must be positive");
        }
    }

    private String audioMimeType(String format) {
        if (format == null) {
            return "audio/mpeg";
        }
        return switch (format) {
            case "mp3", "mpeg" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "opus", "ogg" -> "audio/ogg";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            case "pcm" -> "audio/L16";
            case "webm" -> "audio/webm";
            default -> "audio/" + format;
        };
    }

    private String audioFilename(MimeType mimeType) {
        return "audio." + audioExtension(mimeType);
    }

    private String audioExtension(MimeType mimeType) {
        return switch (mimeType.getSubtype().toLowerCase(Locale.ROOT)) {
            case "mpeg", "mp3", "mpga" -> "mp3";
            case "wav", "x-wav", "wave" -> "wav";
            case "mp4", "m4a", "x-m4a" -> "m4a";
            case "ogg", "opus" -> "ogg";
            case "flac", "x-flac" -> "flac";
            case "webm" -> "webm";
            case "aac" -> "aac";
            default -> throw new BadRequestException("Unsupported audio mimeType: " + mimeType);
        };
    }

    private byte[] decodeBase64(String value, String field) {
        try {
            return Base64.getDecoder().decode(value);
        }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("'" + field + "' is not valid base64");
        }
    }

    private static final class AudioResource extends ByteArrayResource {

        private final String filename;

        AudioResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
