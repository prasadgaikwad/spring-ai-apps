package dev.prasadgaikwad.springaiapps.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.prasadgaikwad.springaiapps.common.UpstreamServiceException;
import dev.prasadgaikwad.springaiapps.common.UpstreamTimeoutException;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.ChoiceAnswer;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.ChoiceOption;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionQuestion;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionResponse;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.DecisionState;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.NoulAnswer;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.QuestionType;
import dev.prasadgaikwad.springaiapps.decision.DecisionPrimitives.ScoreAnswer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** Deterministic HTTP contract tests for {@link JevDecisionClient} (no live network). */
class JevDecisionClientContractTests {

    private final ObjectMapper json = new ObjectMapper();
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void postsRequestMappingAuthorizationAndPreservesBaseUrlPath() throws Exception {
        server.enqueue(jsonResponse("""
                {"answers":[{"questionId":"q1","type":"CHOICE","selectedChoiceId":"a","probability":0.91}]}
                """));

        JevDecisionClient client = client("test-key", "jev-test", Duration.ofSeconds(5));
        DecisionResponse response = client.decide(new DecisionState(Map.of("tier", "gold")), questions("q1"));

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/systemone");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
        assertThat(request.getHeader("Content-Type")).contains("application/json");

        JsonNode body = json.readTree(request.getBody().readUtf8());
        assertThat(body.path("model").asText()).isEqualTo("jev-test");
        assertThat(body.path("state").path("tier").asText()).isEqualTo("gold");
        assertThat(body.path("questions")).hasSize(1);
        assertThat(body.path("questions").get(0).path("id").asText()).isEqualTo("q1");

        assertThat(response.answers()).hasSize(1);
        ChoiceAnswer answer = (ChoiceAnswer) response.answers().get(0);
        assertThat(answer.questionId()).isEqualTo("q1");
        assertThat(answer.selectedChoiceId()).isEqualTo("a");
        assertThat(answer.probability()).isEqualTo(0.91);
    }

    @Test
    void omitsAuthorizationWhenApiKeyBlank() throws Exception {
        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"q1\",\"type\":\"SCORE\",\"score\":1}]}"));
        client("", "jev-test", Duration.ofSeconds(5)).decide(new DecisionState(Map.of()), questions("q1"));

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getHeader("Authorization")).isNull();
    }

    @Test
    void parsesScoreNoulAndChoiceFallbacks() {
        server.enqueue(jsonResponse("""
                {"data":{"answers":[
                  {"id":"s1","type":"SCORE","score":7.5,"confidence":0.6},
                  {"questionId":"n1","type":"NONE","reason":"no fit","probability":0.2},
                  {"questionId":"c1","type":"CHOICE","choiceId":"x","probability":0.8},
                  {"questionId":"n2","type":"NULL","reason":"nullish"}
                ]}}
                """));

        DecisionResponse response = client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("s1", "n1", "c1", "n2"));

        assertThat(response.answers()).hasSize(4);
        ScoreAnswer score = (ScoreAnswer) response.answers().get(0);
        assertThat(score.questionId()).isEqualTo("s1");
        assertThat(score.score()).isEqualTo(7.5);
        assertThat(score.probability()).isEqualTo(0.6);

        NoulAnswer noul = (NoulAnswer) response.answers().get(1);
        assertThat(noul.questionId()).isEqualTo("n1");
        assertThat(noul.reason()).isEqualTo("no fit");
        assertThat(noul.probability()).isEqualTo(0.2);

        ChoiceAnswer fallbackChoice = (ChoiceAnswer) response.answers().get(2);
        assertThat(fallbackChoice.selectedChoiceId()).isEqualTo("x");
        assertThat(fallbackChoice.probability()).isEqualTo(0.8);

        NoulAnswer nullAlias = (NoulAnswer) response.answers().get(3);
        assertThat(nullAlias.questionId()).isEqualTo("n2");
        assertThat(nullAlias.probability()).isEqualTo(1.0);
    }

    @Test
    void returnsEmptyAnswersForGenuinelyEmptyList() {
        server.enqueue(jsonResponse("{\"answers\":[]}"));
        assertThat(client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), List.of()).answers()).isEmpty();
    }

    @Test
    void rejectsBlankOrMissingPayload() {
        server.enqueue(jsonResponse("   "));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("empty response body");

        server.enqueue(jsonResponse("{\"notAnswers\":true}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("missing answers array");
    }

    @Test
    void rejectsNonArrayAnswers() {
        server.enqueue(jsonResponse("{\"answers\":{\"q1\":{}}}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("missing answers array");
    }

    @Test
    void rejectsEmptyAnswersWhenQuestionsWereRequested() {
        server.enqueue(jsonResponse("{\"answers\":[]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("missing answer for requested question");
    }

    @Test
    void rejectsDuplicateUnknownBlankAndMissingIds() {
        server.enqueue(jsonResponse("""
                {"answers":[
                  {"questionId":"q1","type":"SCORE","score":1},
                  {"questionId":"q1","type":"SCORE","score":2}
                ]}
                """));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1", "q2")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("duplicate questionId");

        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"other\",\"type\":\"SCORE\"}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("unknown questionId");

        server.enqueue(jsonResponse("{\"answers\":[{\"type\":\"SCORE\"}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("answer missing questionId");

        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"q1\",\"type\":\"SCORE\"}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1", "q2")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("missing answer for requested question");
    }

    @Test
    void rejectsUnknownTypeAndInvalidNumbers() {
        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"q1\",\"type\":\"UNKNOWN\"}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("unknown answer type");

        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"q1\",\"type\":\"SCORE\",\"probability\":1.5}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("probability out of range");

        server.enqueue(jsonResponse("{\"answers\":[{\"questionId\":\"q1\",\"type\":\"SCORE\",\"score\":\"high\"}]}"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("non-numeric score");
    }

    @Test
    void mapsHttpErrorsWithoutLeakingResponseBody() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("upstream unavailable"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessage("Decision provider request failed")
                .satisfies(ex -> assertThat(ex.getCause()).isNotNull());
    }

    @Test
    void rejectsInvalidJson() {
        server.enqueue(jsonResponse("{not-json"));
        assertThatThrownBy(() -> client("k", "m", Duration.ofSeconds(5))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("invalid JSON");
    }

    @Test
    void timesOutAsUpstreamTimeout() {
        server.enqueue(jsonResponse("{\"answers\":[]}").setBodyDelay(2, TimeUnit.SECONDS));
        assertThatThrownBy(() -> client("k", "m", Duration.ofMillis(200))
                .decide(new DecisionState(Map.of()), questions("q1")))
                .isInstanceOf(UpstreamTimeoutException.class)
                .hasMessage("Decision provider timed out");
    }

    private JevDecisionClient client(String apiKey, String model, Duration timeout) {
        WebClient webClient = WebClient.builder().build();
        DecisionProperties properties = new DecisionProperties(
                "jev", server.url("/api/").toString(), apiKey, model, timeout);
        return new JevDecisionClient(webClient, properties);
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private static List<DecisionQuestion> questions(String... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> new DecisionQuestion(id, QuestionType.SCORE, "Score " + id,
                        List.of(new ChoiceOption("a", "A"))))
                .toList();
    }
}
