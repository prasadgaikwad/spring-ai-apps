package dev.prasadgaikwad.springaiapps.decision;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;
import dev.prasadgaikwad.springaiapps.common.UpstreamServiceException;
import dev.prasadgaikwad.springaiapps.common.UpstreamTimeoutException;

/**
 * Jev/SystemOne-specific adapter calling TypeSafe's decision API.
 *
 * <p>Maps Choice/Score/Noul primitives to the external API shape and back to typed
 * DTOs. Provider drift is treated as an upstream failure, not an empty success.
 * Designed to be mocked in tests (no external network calls by default).
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
        Set<String> requestedIds = requestedQuestionIds(questions);
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "state", state.context(),
                "questions", questions);

        String endpoint = trimTrailingSlash(properties.baseUrl()) + "/v1/systemone";
        WebClient.RequestBodySpec spec = webClient.post().uri(URI.create(endpoint));
        if (StringUtils.hasText(properties.apiKey())) {
            spec = spec.header("Authorization", "Bearer " + properties.apiKey().trim());
        }

        String response;
        try {
            response = spec.bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(properties.timeout())
                    .onErrorMap(TimeoutException.class,
                            ex -> new UpstreamTimeoutException("Decision provider timed out", ex))
                    .block();
        }
        catch (UpstreamTimeoutException ex) {
            throw ex;
        }
        catch (WebClientResponseException ex) {
            throw new UpstreamServiceException("Decision provider request failed", ex);
        }
        catch (RuntimeException ex) {
            if (isTimeout(ex)) {
                throw new UpstreamTimeoutException("Decision provider timed out", ex);
            }
            throw new UpstreamServiceException("Decision provider call failed", ex);
        }
        return parse(response, requestedIds);
    }

    private static Set<String> requestedQuestionIds(List<DecisionPrimitives.DecisionQuestion> questions) {
        Set<String> ids = new HashSet<>();
        if (questions == null) {
            return ids;
        }
        for (int i = 0; i < questions.size(); i++) {
            DecisionPrimitives.DecisionQuestion question = questions.get(i);
            if (question == null || !StringUtils.hasText(question.id())) {
                throw new BadRequestException("Question " + i + " requires a non-blank 'id'");
            }
            if (!ids.add(question.id())) {
                throw new BadRequestException("Question ids must be unique: " + question.id());
            }
        }
        return ids;
    }

    private DecisionPrimitives.DecisionResponse parse(String response, Set<String> requestedIds) {
        if (!StringUtils.hasText(response)) {
            throw providerError("empty response body");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        }
        catch (Exception ex) {
            throw providerError("invalid JSON", ex);
        }

        JsonNode answers = root.get("answers");
        if (answers == null || !answers.isArray()) {
            answers = root.path("data").get("answers");
        }
        if (answers == null || !answers.isArray()) {
            throw providerError("missing answers array");
        }
        if (answers.isEmpty()) {
            if (requestedIds.isEmpty()) {
                return new DecisionPrimitives.DecisionResponse(List.of());
            }
            throw providerError("missing answer for requested question");
        }

        List<DecisionPrimitives.DecisionAnswer> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode node : answers) {
            String questionId = text(node, "questionId", "id", null);
            if (!StringUtils.hasText(questionId)) {
                throw providerError("answer missing questionId");
            }
            if (!requestedIds.contains(questionId)) {
                throw providerError("unknown questionId");
            }
            if (!seen.add(questionId)) {
                throw providerError("duplicate questionId");
            }
            result.add(mapAnswer(node, questionId));
        }
        if (!seen.containsAll(requestedIds)) {
            throw providerError("missing answer for requested question");
        }
        return new DecisionPrimitives.DecisionResponse(result);
    }

    private static DecisionPrimitives.DecisionAnswer mapAnswer(JsonNode node, String questionId) {
        String type = text(node, "type", null, null);
        if (!StringUtils.hasText(type)) {
            throw providerError("answer missing type");
        }
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "SCORE" -> new DecisionPrimitives.ScoreAnswer(
                    questionId, score(node), probability(node));
            case "NOUL", "NONE", "NULL" -> new DecisionPrimitives.NoulAnswer(
                    questionId, text(node, "reason", null, ""), probability(node));
            case "CHOICE" -> new DecisionPrimitives.ChoiceAnswer(
                    questionId, choiceId(node), probability(node));
            default -> throw providerError("unknown answer type");
        };
    }

    private static String choiceId(JsonNode node) {
        String choiceId = text(node, "selectedChoiceId", "choiceId", null);
        if (!StringUtils.hasText(choiceId)) {
            throw providerError("choice answer missing selectedChoiceId");
        }
        return choiceId;
    }

    private static double score(JsonNode node) {
        return number(node, "score", null, 0.0);
    }

    private static double probability(JsonNode node) {
        double value = number(node, "probability", "confidence", 1.0);
        if (value < 0.0 || value > 1.0) {
            throw providerError("probability out of range");
        }
        return value;
    }

    private static String text(JsonNode node, String primary, String fallback, String defaultValue) {
        JsonNode value = node.get(primary);
        if (value == null && fallback != null) {
            value = node.get(fallback);
        }
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }

    private static double number(JsonNode node, String primary, String fallback, double defaultValue) {
        JsonNode value = node.get(primary);
        if (value == null && fallback != null) {
            value = node.get(fallback);
        }
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isNumber()) {
            throw providerError("non-numeric " + primary);
        }
        double result = value.asDouble();
        if (!Double.isFinite(result)) {
            throw providerError("non-finite " + primary);
        }
        return result;
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException
                    || current.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String trimTrailingSlash(String baseUrl) {
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static UpstreamServiceException providerError(String detail) {
        return new UpstreamServiceException("Decision provider response was invalid: " + detail, null);
    }

    private static UpstreamServiceException providerError(String detail, Throwable cause) {
        return new UpstreamServiceException("Decision provider response was invalid: " + detail, cause);
    }
}
