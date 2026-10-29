package dev.prasadgaikwad.springaiapps.observability;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for the {@code ai-observability} module (issue #20): tests the DuckDB
 * trace sink write→query path through the public REST contract and exercises the
 * observation capture integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ollama")
class TraceSinkIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void recordAndQuerySpans() throws Exception {
        String body = """
                {
                  "traceId": "trace-123",
                  "name": "gen_ai.client.operation",
                  "kind": "CLIENT",
                  "startTime": "%s",
                  "tags": {"model": "llama3.2", "provider": "ollama"}
                }
                """.formatted(Instant.now());

        mockMvc.perform(post("/api/observability/spans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("trace-123"))
                .andExpect(jsonPath("$.name").value("gen_ai.client.operation"));

        mockMvc.perform(get("/api/observability/spans").param("traceId", "trace-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/observability/traces"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsBlankSpanName() throws Exception {
        mockMvc.perform(post("/api/observability/spans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traceId\":\"t1\",\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
