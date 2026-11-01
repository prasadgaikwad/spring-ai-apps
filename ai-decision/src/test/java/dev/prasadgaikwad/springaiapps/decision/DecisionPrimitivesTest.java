package dev.prasadgaikwad.springaiapps.decision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for decision primitives and mapping — no external network calls.
 */
class DecisionPrimitivesTest {

    @Test
    void choiceAnswerStoresFields() {
        DecisionPrimitives.ChoiceAnswer answer = new DecisionPrimitives.ChoiceAnswer("q1", "yes", 0.92);
        assertThat(answer.questionId()).isEqualTo("q1");
        assertThat(answer.selectedChoiceId()).isEqualTo("yes");
        assertThat(answer.probability()).isEqualTo(0.92);
    }

    @Test
    void scoreAnswerStoresFields() {
        DecisionPrimitives.ScoreAnswer answer = new DecisionPrimitives.ScoreAnswer("q2", 0.85, 0.88);
        assertThat(answer.score()).isEqualTo(0.85);
    }

    @Test
    void noulAnswerStoresFields() {
        DecisionPrimitives.NoulAnswer answer = new DecisionPrimitives.NoulAnswer("q3", "no match", 0.76);
        assertThat(answer.reason()).isEqualTo("no match");
    }

    @Test
    void decisionStateCopiesContext() {
        DecisionPrimitives.DecisionState state = new DecisionPrimitives.DecisionState(java.util.Map.of("x", 1));
        assertThat(state.context().get("x")).isEqualTo(1);
    }
}
