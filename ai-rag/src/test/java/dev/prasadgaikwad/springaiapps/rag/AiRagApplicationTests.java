package dev.prasadgaikwad.springaiapps.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Context-load + REST smoke test for the {@code ai-rag} module (the agreed blanket seam).
 *
 * <p>The module deliberately depends on the PGVector starter, which needs a DataSource.
 * The full Vector store integration is exercised in the ETL/query work (issues #2/#3)
 * against a running Postgres; here we exclude the DB autoconfigurations so the skeleton
 * context loads standalone.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration," +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class AiRagApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VectorStore vectorStore;

    @Test
    void contextLoads() {
    }

    @Test
    void scaffoldEndpointResponds() throws Exception {
        mockMvc.perform(get("/api/rag/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("ai-rag"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
