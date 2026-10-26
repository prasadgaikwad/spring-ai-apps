package dev.prasadgaikwad.springaiapps.agents;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for the {@code ai-agents} module (issue #5): drives the REST contract with a
 * pointed {@link ChatModel}, so the agent flows are deterministic and offline.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class AgentIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel chatModel;

    @BeforeEach
    void stubModel() {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("12 km is about 7.46 miles.")))));
    }

    @Test
    void askWithLocalToolsReturnsAnswer() throws Exception {
        mockMvc.perform(post("/api/agents/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is 12 km in miles?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("What is 12 km in miles?"))
                .andExpect(jsonPath("$.answer").value("12 km is about 7.46 miles."));
    }

    @Test
    void askRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/agents/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void localToolCatalogListsTools() throws Exception {
        mockMvc.perform(get("/api/agents/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("local"))
                .andExpect(jsonPath("$.tools", org.hamcrest.Matchers.containsInAnyOrder(
                        "add", "multiply", "convertUnit", "daysBetween")));
    }

    @Test
    void mcpToolCatalogIsExposed() throws Exception {
        mockMvc.perform(get("/api/agents/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("mcp"));
    }

    @Test
    void mcpAskReturnsAnswerWithoutConnectedClient() throws Exception {
        mockMvc.perform(post("/api/agents/mcp/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty());
    }
}
