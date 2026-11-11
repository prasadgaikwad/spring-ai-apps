package dev.prasadgaikwad.springaiapps.multimodal;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.metadata.OpenAiImageGenerationMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for the {@code ai-multimodal} module (issue #5): drives the four
 * modality endpoints through REST with pointed models, so every flow is deterministic
 * and offline. Option-mapping tests (issue #34) verify that request fields reach the
 * Spring AI model options rather than being silently ignored.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class MultimodalIntegrationTests {

    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel chatModel;

    @MockitoBean
    ImageModel imageModel;

    @MockitoBean
    TextToSpeechModel textToSpeechModel;

    @MockitoBean
    TranscriptionModel transcriptionModel;

    @Test
    void generatesImageWithDimensionsAndRevisedPrompt() throws Exception {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(new ImageResponse(
                List.of(new ImageGeneration(new Image("https://example.com/cat.png", null),
                        new OpenAiImageGenerationMetadata("a photo of a cat")))));

        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\",\"width\":512,\"height\":256}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/cat.png"))
                .andExpect(jsonPath("$.revisedPrompt").value("a photo of a cat"));

        ArgumentCaptor<ImagePrompt> prompt = ArgumentCaptor.forClass(ImagePrompt.class);
        verify(imageModel).call(prompt.capture());
        assertThat(prompt.getValue().getOptions().getN()).isEqualTo(1);
        assertThat(prompt.getValue().getOptions().getWidth()).isEqualTo(512);
        assertThat(prompt.getValue().getOptions().getHeight()).isEqualTo(256);
    }

    @Test
    void rejectsEmptyImageResponse() throws Exception {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(new ImageResponse(List.of()));

        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appliesSingleImageDimension() throws Exception {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(new ImageResponse(
                List.of(new ImageGeneration(new Image("https://example.com/cat.png", null)))));

        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\",\"width\":640}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ImagePrompt> prompt = ArgumentCaptor.forClass(ImagePrompt.class);
        verify(imageModel).call(prompt.capture());
        assertThat(prompt.getValue().getOptions().getWidth()).isEqualTo(640);
        assertThat(prompt.getValue().getOptions().getHeight()).isNull();
    }

    @Test
    void rejectsNonPositiveImageDimensions() throws Exception {
        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\",\"width\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\",\"height\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectBlankImagePrompt() throws Exception {
        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void describesImageWithVision() throws Exception {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("A small red square.")))));

        mockMvc.perform(post("/api/multimodal/vision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is in this image?\",\"imageBase64\":\"" + TINY_PNG_BASE64
                                + "\",\"mimeType\":\"image/png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A small red square."));
    }

    @Test
    void rejectsNonImageVisionMimeType() throws Exception {
        mockMvc.perform(post("/api/multimodal/vision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is in this image?\",\"imageBase64\":\"" + TINY_PNG_BASE64
                                + "\",\"mimeType\":\"audio/mpeg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void synthesizesSpeechWithOptions() throws Exception {
        byte[] audio = {1, 2, 3, 4};
        when(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .thenReturn(new TextToSpeechResponse(List.of(new Speech(audio))));

        mockMvc.perform(post("/api/multimodal/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello world\",\"voice\":\"alloy\",\"format\":\"WAV\",\"speed\":1.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("audio/wav"))
                .andExpect(jsonPath("$.byteLength").value(4))
                .andExpect(jsonPath("$.audioBase64").value(Base64.getEncoder().encodeToString(audio)));

        ArgumentCaptor<TextToSpeechPrompt> prompt = ArgumentCaptor.forClass(TextToSpeechPrompt.class);
        verify(textToSpeechModel).call(prompt.capture());
        assertThat(prompt.getValue().getOptions().getVoice()).isEqualTo("alloy");
        assertThat(prompt.getValue().getOptions().getFormat()).isEqualTo("wav");
        assertThat(prompt.getValue().getOptions().getSpeed()).isEqualTo(1.25);
    }

    @Test
    void synthesizesSpeechWithProviderDefaults() throws Exception {
        byte[] audio = {5};
        when(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .thenReturn(new TextToSpeechResponse(List.of(new Speech(audio))));

        mockMvc.perform(post("/api/multimodal/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello world\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("audio/mpeg"));

        ArgumentCaptor<TextToSpeechPrompt> prompt = ArgumentCaptor.forClass(TextToSpeechPrompt.class);
        verify(textToSpeechModel).call(prompt.capture());
        assertThat(prompt.getValue().getOptions().getVoice()).isNull();
        assertThat(prompt.getValue().getOptions().getFormat()).isNull();
        assertThat(prompt.getValue().getOptions().getSpeed()).isNull();
    }

    @Test
    void rejectsEmptySpeechResponse() throws Exception {
        when(textToSpeechModel.call(any(TextToSpeechPrompt.class)))
                .thenReturn(new TextToSpeechResponse(List.of()));

        mockMvc.perform(post("/api/multimodal/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello world\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveSpeechSpeed() throws Exception {
        mockMvc.perform(post("/api/multimodal/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello world\",\"speed\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transcribesAudioWithMimeDerivedFilename() throws Exception {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("hello world");

        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(new byte[]{9, 9})
                                + "\",\"mimeType\":\"audio/wav\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hello world"));

        ArgumentCaptor<Resource> resource = ArgumentCaptor.forClass(Resource.class);
        verify(transcriptionModel).transcribe(resource.capture());
        assertThat(resource.getValue().getFilename()).isEqualTo("audio.wav");
    }

    @Test
    void transcribesAudioWithDefaultMimeType() throws Exception {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("hello world");

        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(new byte[]{9, 9})
                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hello world"));

        ArgumentCaptor<Resource> resource = ArgumentCaptor.forClass(Resource.class);
        verify(transcriptionModel).transcribe(resource.capture());
        assertThat(resource.getValue().getFilename()).isEqualTo("audio.mp3");
    }

    @Test
    void rejectsNonAudioTranscriptionMimeType() throws Exception {
        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(new byte[]{9, 9})
                                + "\",\"mimeType\":\"image/png\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedAudioMimeSubtype() throws Exception {
        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(new byte[]{9, 9})
                                + "\",\"mimeType\":\"audio/aiff\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectInvalidBase64() throws Exception {
        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"not base64!!\"}"))
                .andExpect(status().isBadRequest());
    }
}
