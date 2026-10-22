package dev.prasadgaikwad.springaiapps.eval;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for the {@code ai-eval} module (issue #5): drives {@code POST /api/eval/run}
 * through the public REST contract with a pointed {@link ChatModel}, exercising the
 * shared Eval harness (grade Q&amp;A pairs, regression diff against a prior run, history).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class EvalRunIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel chatModel;

    @Test
    void contextLoads() {
    }

    @Test
    void runGradesProvidedAnswersAndReturnsReport() throws Exception {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("unused")))));

        String request = """
                {
                  "pairs": [
                    {"question": "What is Spring AI?", "answer": "Spring AI is a framework for building AI applications with Spring."},
                    {"question": "What does RAG mean?", "answer": "RAG stands for Retrieval Augmented Generation in Spring AI contexts."}
                  ],
                  "criteria": [
                    {"type": "contains", "value": "Spring"},
                    {"type": "excludes", "value": "hallucination"}
                  ]
                }
                """;

        String body =         mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.regressions", hasSize(0)))
                .andExpect(jsonPath("$.report.cases", hasSize(2)))
                .andExpect(jsonPath("$.report.cases[0].caseId").value("case-0"))
                .andExpect(jsonPath("$.report.cases[0].passed").value(true))
                .andExpect(jsonPath("$.report.cases[1].passed").value(true))
                .andExpect(jsonPath("$.report.summary.totalCases").value(2))
                .andExpect(jsonPath("$.report.summary.passedCases").value(2))
                .andExpect(jsonPath("$.report.summary.passRate").value(1.0))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("regressions");
    }

    @Test
    void runGeneratesAnswersWhenOmittedAndTracksRegressions() throws Exception {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Spring AI is a framework")))));

        String first = """
                {
                  "pairs": [
                    {"question": "What is Spring AI?", "answer": null}
                  ],
                  "criteria": [
                    {"type": "contains", "value": "framework"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.cases[0].passed").value(true))
                .andExpect(jsonPath("$.regressions", hasSize(0)));

        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("irrelevant")))));

        String second = """
                {
                  "pairs": [
                    {"question": "What is Spring AI?", "answer": null}
                  ],
                  "criteria": [
                    {"type": "contains", "value": "framework"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report.cases[0].passed").value(false))
                .andExpect(jsonPath("$.regressions", hasSize(1)))
                .andExpect(jsonPath("$.regressions[0]").value("case-0"));

        mockMvc.perform(get("/api/eval/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void runRejectsBadRequest() throws Exception {
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pairs\":[],\"criteria\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
