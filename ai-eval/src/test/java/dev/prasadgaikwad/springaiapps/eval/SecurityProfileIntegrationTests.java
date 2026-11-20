package dev.prasadgaikwad.springaiapps.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the opt-in security profile is wired from ai-common into an executable module.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "ollama", "security" })
class SecurityProfileIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void postRequiresApiKey() throws Exception {
        mockMvc.perform(post("/api/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void pingRemainsReachable() throws Exception {
        mockMvc.perform(get("/api/eval/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void springdocIsDisabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isGreaterThanOrEqualTo(400));
    }
}
