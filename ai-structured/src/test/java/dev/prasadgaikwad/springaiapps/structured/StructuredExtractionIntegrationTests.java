package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
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
 * Primary seam for the {@code ai-structured} module (issue #5): drives
 * {@code POST /api/structured/extract} with a pointed {@link ChatModel} whose reply is
 * the JSON the model would emit, exercising the real structured-output converter and the
 * real bean-validation second half.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class StructuredExtractionIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel chatModel;

    @BeforeEach
    void stubOptions() {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
    }

    @Test
    void extractsValidCandidateProfile() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("""
                {"fullName":"Ada Lovelace","email":"ada@example.com","currentRole":"Engineer",
                 "yearsOfExperience":10,"topSkills":["Java","Spring"],
                 "history":[{"company":"Analytical Engines","title":"Lead","startYear":2010,"endYear":2020}]}
                """));

        mockMvc.perform(post("/api/structured/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"candidate\",\"text\":\"Ada Lovelace, ada@example.com ...\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.value.email").value("ada@example.com"))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.violations").isEmpty());
    }

    @Test
    void surfacesValidationViolationsForMalformedModelOutput() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("""
                {"fullName":"","email":"not-an-email","currentRole":"","yearsOfExperience":99,"topSkills":[],"history":[]}
                """));

        mockMvc.perform(post("/api/structured/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"candidate\",\"text\":\"garbage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.violations").isNotEmpty());
    }

    @Test
    void extractsValidOrder() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("""
                {"customerName":"Ada","product":"Widget","quantity":3,"shippingAddress":"1 Main St","notes":[]}
                """));

        mockMvc.perform(post("/api/structured/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order\",\"text\":\"Please send 3 widgets to 1 Main St\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.product").value("Widget"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void rejectsUnknownType() throws Exception {
        mockMvc.perform(post("/api/structured/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"nonsense\",\"text\":\"anything\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/structured/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"candidate\",\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    private static ChatResponse response(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }
}
