package dev.prasadgaikwad.springaiapps.decision;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.prasadgaikwad.springaiapps.common.UpstreamServiceException;
import dev.prasadgaikwad.springaiapps.common.UpstreamTimeoutException;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionResponse;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.ScoreAnswer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    DecisionClient client;

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

    @Test
    void decideRejectsNullStateValues() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{\"accountId\":null},\"questions\":[{\"id\":\"q1\",\"type\":\"SCORE\",\"prompt\":\"Score it\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("'state' must not contain null values"));
    }

    @Test
    void decideRejectsNullQuestionElement() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question 0 must not be null"));
    }

    @Test
    void decideRejectsBlankQuestionIdAndPrompt() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"  \",\"type\":\"SCORE\",\"prompt\":\"  \"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question 0 requires a non-blank 'id'"));
    }

    @Test
    void decideRejectsMissingQuestionType() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"q1\",\"prompt\":\"Score it\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question 0 requires a 'type'"));
    }

    @Test
    void decideRejectsChoiceQuestionWithoutChoices() throws Exception {
        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"q1\",\"type\":\"CHOICE\",\"prompt\":\"Pick one\",\"choices\":[]}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question 0 of type CHOICE requires non-empty 'choices'"));
    }

    @Test
    void decideReturnsAnswersFromClient() throws Exception {
        when(client.decide(any(), any())).thenReturn(
                new DecisionResponse(List.of(new ScoreAnswer("q1", 0.8, 0.9))));

        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"q1\",\"type\":\"SCORE\",\"prompt\":\"Score it\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers[0].questionId").value("q1"))
                .andExpect(jsonPath("$.answers[0].score").value(0.8))
                .andExpect(jsonPath("$.answers[0].probability").value(0.9));
    }

    @Test
    void decideMapsUpstreamFailureToBadGatewayWithoutLeakingDetail() throws Exception {
        when(client.decide(any(), any())).thenThrow(
                new UpstreamServiceException("raw upstream detail", new IllegalStateException("secret")));

        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"q1\",\"type\":\"SCORE\",\"prompt\":\"Score it\"}]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.message").value("Upstream service failed"));
    }

    @Test
    void decideMapsUpstreamTimeoutToGatewayTimeout() throws Exception {
        when(client.decide(any(), any())).thenThrow(
                new UpstreamTimeoutException("raw timeout detail", new IllegalStateException("secret")));

        mockMvc.perform(post("/api/decision/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":{},\"questions\":[{\"id\":\"q1\",\"type\":\"SCORE\",\"prompt\":\"Score it\"}]}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("GATEWAY_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("Upstream service timed out"));
    }
}
