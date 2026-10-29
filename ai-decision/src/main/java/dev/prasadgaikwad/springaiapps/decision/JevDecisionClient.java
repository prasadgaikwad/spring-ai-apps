package dev.prasadgaikwad.springaiapps.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jev/SystemOne-specific adapter calling TypeSafe's decision API.
 *
 * <p>Maps Choice/Score/Noul primitives to the external API shape and back to typed
 * DTOs. Designed to be mocked in tests (no external network calls by default).
 */
@Component
public class JevDecisionClient implements DecisionClient {

    private final WebClient webClient;
    private final DecisionProperties properties;
    private final ObjectMapper objectMapper;

    public JevDecisionClient(WebClient decisionWebClient, DecisionProperties properties) {
        this.webClient = decisionWebClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public DecisionPrimitives.DecisionResponse decide(DecisionPrimitives.DecisionState state,
                                                      List<DecisionPrimitives.DecisionQuestion> questions) {
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "state", state.context(),
                "questions", questions);

        String response = webClient.post()
                .uri("/v1/systemone")
                .header("Authorization", properties.apiKey() != null && !properties.apiKey().isBlank()
                        ? "Bearer " + properties.apiKey()
                        : null)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block(properties.timeout());

        return parse(response);
    }

    private DecisionPrimitives.DecisionResponse parse(String response) {
        if (response == null || response.isBlank()) {
            return new DecisionPrimitives.DecisionResponse(List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode answers = root.path("answers");
            if (!answers.isArray() || answers.isEmpty()) {
                answers = root.path("data").path("answers");
            }
            List<DecisionPrimitives.DecisionAnswer> result = new ArrayList<>();
            if (answers.isArray()) {
                for (JsonNode node : answers) {
                    String qid = node.path("questionId").asText(node.path("id").asText());
                    String type = node.path("type").asText("CHOICE").toUpperCase();
                    double prob = node.path("probability").asDouble(node.path("confidence").asDouble(1.0));
                    if ("SCORE".equals(type)) {
                        result.add(new DecisionPrimitives.ScoreAnswer(qid, node.path("score").asDouble(), prob));
                    }
                    else if ("NOUL".equals(type) || "NONE".equals(type) || "NULL".equals(type)) {
                        result.add(new DecisionPrimitives.NoulAnswer(qid, node.path("reason").asText(""), prob));
                    }
                    else {
                        result.add(new DecisionPrimitives.ChoiceAnswer(qid,
                                node.path("selectedChoiceId").asText(node.path("choiceId").asText()), prob));
                    }
                }
            }
            return new DecisionPrimitives.DecisionResponse(result);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to parse decision response", ex);
        }
    }
}
