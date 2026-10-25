package dev.prasadgaikwad.springaiapps.multimodal;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for the {@code ai-multimodal} module (issue #5): drives the four
 * modality endpoints through REST with pointed models, so every flow is deterministic
 * and offline.
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
    void generatesImage() throws Exception {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(
                new ImageResponse(List.of(new ImageGeneration(new Image("https://example.com/cat.png", null)))));

        mockMvc.perform(post("/api/multimodal/image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"a cat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/cat.png"));
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
    void synthesizesSpeech() throws Exception {
        byte[] audio = {1, 2, 3, 4};
        when(textToSpeechModel.call(any(String.class))).thenReturn(audio);

        mockMvc.perform(post("/api/multimodal/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello world\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("audio/mpeg"))
                .andExpect(jsonPath("$.byteLength").value(4))
                .andExpect(jsonPath("$.audioBase64").value(Base64.getEncoder().encodeToString(audio)));
    }

    @Test
    void transcribesAudio() throws Exception {
        when(transcriptionModel.transcribe(ArgumentMatchers.any(org.springframework.core.io.Resource.class)))
                .thenReturn("hello world");

        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(new byte[]{9, 9})
                                + "\",\"mimeType\":\"audio/mpeg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hello world"));
    }

    @Test
    void rejectInvalidBase64() throws Exception {
        mockMvc.perform(post("/api/multimodal/transcribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"audioBase64\":\"not base64!!\"}"))
                .andExpect(status().isBadRequest());
    }
}
