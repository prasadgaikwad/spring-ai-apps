package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Query-path seam for the {@code ai-rag} module (ticket #3, the agreed primary seam).
 *
 * <p>Drives the whole slice through the public REST contract — ingest a document, ask a
 * question through {@code POST /api/rag/query}, receive a grounded answer — against a
 * real Testcontainers PGVector store with a pointed {@link EmbeddingModel} and a pointed
 * {@link ChatModel}, so the pipeline (hybrid retrieval → rerank → generate) is
 * deterministic and offline. Asserts both the answer the caller sees and the prompt the
 * model was actually given, which is what "grounded only in retrieved context" means
 * observably: the ingested text and the answer-only-from-context instruction must reach
 * the model.
 */
@AutoConfigureMockMvc
class RagQueryIntegrationTests extends AbstractRagIntegrationTest {

    private static final String INGESTED_TEXT = """
            Spring AI is the framework for building AI applications with Spring.
            Spring AI supports RAG, agents, evaluation, and observability.
            The ETL pipeline reads sources, splits them into chunks, embeds them, and stores them.
            """;

    private static final String STUBBED_ANSWER = "Spring AI is the framework for building AI applications with Spring.";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel chatModel;

    @Test
    void askQuestionReturnsGroundedAnswerFromRetrievedContext() throws Exception {
        stubFixedEmbeddingModel();
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        when(chatModel.call(promptCaptor.capture())).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage(STUBBED_ANSWER)))));

        mockMvc.perform(post("/api/rag/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + escape(INGESTED_TEXT) + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Spring AI?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.question").value("What is Spring AI?"))
                .andExpect(jsonPath("$.answer").value(STUBBED_ANSWER))
                .andExpect(jsonPath("$.sources").isNotEmpty())
                .andExpect(jsonPath("$.sources[0].id").isNotEmpty())
                .andExpect(jsonPath("$.sources[0].text", containsString("Spring AI")));

        String prompted = promptCaptor.getValue().getUserMessage().getText();
        assertThat(prompted).contains("Spring AI is the framework");
        assertThat(prompted).contains("Given the context information and no prior knowledge");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
