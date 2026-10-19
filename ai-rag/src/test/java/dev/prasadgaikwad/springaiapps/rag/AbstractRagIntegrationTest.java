package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;
import java.util.Arrays;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Shared integration-test scaffolding for the {@code ai-rag} module.
 *
 * <p>Provisions a real PGVector-enabled Postgres via Testcontainers and wires the
 * auto-configured DataSource/JdbcTemplate/VectorStore to it (via
 * {@link ServiceConnection}), so tests exercise the genuine read → split → embed →
 * store pipeline without a manually started database.
 *
 * <p>Embeddings are made deterministic by {@link #stubFixedEmbeddingModel()}, which
 * points the auto-configured Ollama embedding model at one fixed vector. Subclasses
 * that go through the ETL pipeline must call it before driving ingestion.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("ollama")
abstract class AbstractRagIntegrationTest {

    private static final float[] FIXED_EMBEDDING = fixedVector(768, 0.1f);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @MockitoBean(name = "ollamaEmbeddingModel")
    EmbeddingModel embeddingModel;

    protected void stubFixedEmbeddingModel() {
        when(embeddingModel.embed(any(List.class), any(EmbeddingOptions.class), any(BatchingStrategy.class)))
                .thenAnswer(invocation -> {
                    List<?> docs = invocation.getArgument(0);
                    return docs.stream().map(d -> FIXED_EMBEDDING).toList();
                });
        when(embeddingModel.embed(any(String.class))).thenReturn(FIXED_EMBEDDING);
    }

    private static float[] fixedVector(int size, float value) {
        float[] vector = new float[size];
        Arrays.fill(vector, value);
        return vector;
    }
}
