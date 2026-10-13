package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ingestion seam for the {@code ai-rag} module (ticket #2).
 *
 * <p>Drives {@code POST /api/rag/ingest} against a real Testcontainers PGVector store
 * with a pointed (mocked) {@link EmbeddingModel} so the whole ETL pipeline
 * (read → split → embed → store) is deterministic and offline. Asserts the ingested
 * chunks actually land in the Vector store.
 */
@AutoConfigureMockMvc
class DocumentIngestionIntegrationTests extends AbstractRagIntegrationTest {

    private static final float[] FIXED_EMBEDDING = fixedVector(768, 0.1f);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    VectorStore vectorStore;

    @MockitoBean(name = "ollamaEmbeddingModel")
    EmbeddingModel embeddingModel;

    @Test
    void ingestDocumentStoresChunksInVectorStore() throws Exception {
        stubEmbeddingModel();

        String text = """
                Spring AI is the framework for building AI applications with Spring.
                Spring AI supports RAG, agents, evaluation, and observability.
                This document is long enough to be split into several chunks.
                """;

        mockMvc.perform(post("/api/rag/ingest")
                        .contentType("application/json")
                        .content("{\"text\":\"" + escape(text) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.documents").value(1))
                .andExpect(jsonPath("$.chunks").value(org.hamcrest.Matchers.greaterThan(0)));

        List<Document> stored = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder().query("all about building AI apps in Spring").topK(10).build());

        assertThat(stored).isNotEmpty();
        assertThat(stored).allMatch(doc ->
                doc.getText() != null && doc.getText().contains("Spring"));
    }

    private void stubEmbeddingModel() {
        when(embeddingModel.embed(any(List.class), any(EmbeddingOptions.class), any(BatchingStrategy.class)))
                .thenAnswer(invocation -> {
                    List<?> docs = invocation.getArgument(0);
                    return docs.stream().map(d -> FIXED_EMBEDDING).toList();
                });
        when(embeddingModel.embed(any(String.class))).thenReturn(FIXED_EMBEDDING);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static float[] fixedVector(int size, float value) {
        float[] v = new float[size];
        java.util.Arrays.fill(v, value);
        return v;
    }
}
