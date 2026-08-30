package dev.prasadgaikwad.springaiapps.observability;

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
 * Context-load + REST smoke test for the {@code ai-observability} module (the agreed blanket seam).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class AiObservabilityApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void scaffoldEndpointResponds() throws Exception {
        mockMvc.perform(get("/api/observability/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("ai-observability"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
