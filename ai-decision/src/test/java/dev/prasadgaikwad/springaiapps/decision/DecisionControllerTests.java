package dev.prasadgaikwad.springaiapps.decision;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primary seam for ai-decision: REST contract with mocked client behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DecisionControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void pingResponds() throws Exception {
        mockMvc.perform(get("/api/decision/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("ai-decision"));
    }

    @Test
    void decideRejectsEmptyQuestions() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
