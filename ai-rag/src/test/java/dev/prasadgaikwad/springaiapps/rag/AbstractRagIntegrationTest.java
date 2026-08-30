package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared integration-test scaffolding for the {@code ai-rag} module.
 *
 * <p>Provisions a real PGVector-enabled Postgres via Testcontainers and wires the
 * auto-configured DataSource/JdbcTemplate/VectorStore to it (via
 * {@link ServiceConnection}), so tests exercise the genuine read → split → embed →
 * store pipeline without a manually started database.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("ollama")
abstract class AbstractRagIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
}
