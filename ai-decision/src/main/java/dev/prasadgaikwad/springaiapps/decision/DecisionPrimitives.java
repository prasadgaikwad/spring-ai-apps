package dev.prasadgaikwad.springaiapps.decision;

import java.util.List;
import java.util.Map;

/**
 * Decision model primitives: System One/Jev evaluate state and return typed answers
 * (Choice, Score, Noul) with calibrated probabilities — not free-form text.
 */
public class DecisionPrimitives {

    /** Arbitrary state passed to the decision model. */
    public record DecisionState(Map<String, Object> context) {
        public DecisionState {
            if (context == null) {
                context = Map.of();
            }
            context = Map.copyOf(context);
        }
    }

    /** A question posed to the decision model. */
    public record DecisionQuestion(String id, QuestionType type, String prompt, List<ChoiceOption> choices) {

        public DecisionQuestion {
            if (choices == null) {
                choices = List.of();
            }
            choices = List.copyOf(choices);
        }
    }

    public enum QuestionType {
        CHOICE,
        SCORE,
        NOUL
    }

    public record ChoiceOption(String id, String label) {
    }

    /** The typed answer returned by the decision model. */
    public sealed interface DecisionAnswer permits ChoiceAnswer, ScoreAnswer, NoulAnswer {
        String questionId();
    }

    /** A selection among provided choices with calibrated probability. */
    public record ChoiceAnswer(String questionId, String selectedChoiceId, Double probability) implements DecisionAnswer {
    }

    /** A numeric score with calibrated probability/uncertainty. */
    public record ScoreAnswer(String questionId, Double score, Double probability) implements DecisionAnswer {
    }

    /** A null/none answer indicating no valid choice applies. */
    public record NoulAnswer(String questionId, String reason, Double probability) implements DecisionAnswer {
    }

    /** Response from a decision model call. */
    public record DecisionResponse(List<DecisionAnswer> answers) {
        public DecisionResponse {
            if (answers == null) {
                answers = List.of();
            }
            answers = List.copyOf(answers);
        }
    }
}
