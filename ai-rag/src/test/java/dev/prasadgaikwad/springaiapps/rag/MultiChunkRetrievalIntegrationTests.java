package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Multi-chunk retrieval seam against a real PGVector/Testcontainers store.
 *
 * <p>Ingests two distinct chunks, re-ingests one to prove idempotent upsert, then
 * exercises the real hybrid retriever and lexical reranker. This covers the fusion,
 * dedup, ordering, and source-metadata propagation that the one-chunk fixture cannot.
 * Run with: {@code ./mvnw -pl ai-rag -am test -Dgroups=docker}.
 */
@Tag("docker")
@AutoConfigureMockMvc
class MultiChunkRetrievalIntegrationTests extends AbstractRagIntegrationTest {

    private static final String QUANTUM_CHUNK =
            "Quantum gardening explores lunar soil and ancient seeds in extreme conditions.";
    private static final String COOKING_CHUNK =
            "Classical cooking relies on slow simmering sauces and carefully balanced herbs.";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HybridDocumentRetriever hybridDocumentRetriever;

    @Autowired
    LexicalReranker lexicalReranker;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}")
    String tableName;

    @BeforeEach
    void clearStore() {
        // The Testcontainers instance is shared by all RAG integration classes.
        jdbcTemplate.execute("DELETE FROM " + tableName);
    }

    @Test
    void hybridRetrievalReranksDedupesAndPropagatesSource() throws Exception {
        stubFixedEmbeddingModel();
        ingest(QUANTUM_CHUNK);
        ingest(COOKING_CHUNK);
        ingest(QUANTUM_CHUNK); // deterministic id -> upsert, not a duplicate row

        Long chunkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        assertThat(chunkCount).isEqualTo(2L);

        Query query = new Query("quantum gardening");
        List<Document> retrieved = hybridDocumentRetriever.retrieve(query);

        assertThat(retrieved).hasSize(2);
        assertThat(retrieved).extracting(Document::getId).doesNotHaveDuplicates();
        assertThat(retrieved).allSatisfy(document ->
                assertThat(document.getMetadata()).containsEntry("source", "inline-text"));

        List<Document> reranked = lexicalReranker.process(query, retrieved);
        assertThat(reranked).hasSize(2);
        assertThat(reranked.get(0).getText()).contains("Quantum gardening");
        assertThat(reranked.get(1).getText()).contains("Classical cooking");
    }

    private void ingest(String text) throws Exception {
        mockMvc.perform(post("/api/rag/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + escape(text) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.chunks").value(1));
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
