package dev.prasadgaikwad.springaiapps.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Context-load + REST smoke test for the {@code ai-eval} module (the agreed blanket seam).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class AiEvalApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean(name = "ollamaChatModel")
    org.springframework.ai.chat.model.ChatModel chatModel;

    @org.junit.jupiter.api.BeforeEach
    void stubChatModel() {
        org.mockito.Mockito.when(chatModel.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        org.mockito.Mockito.when(chatModel.call(org.mockito.ArgumentMatchers.any(org.springframework.ai.chat.prompt.Prompt.class)))
                .thenReturn(new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(new org.springframework.ai.chat.model.Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void scaffoldEndpointResponds() throws Exception {
        mockMvc.perform(get("/api/eval/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("ai-eval"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
