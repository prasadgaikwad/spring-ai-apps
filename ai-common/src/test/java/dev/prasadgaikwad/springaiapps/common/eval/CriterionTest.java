package dev.prasadgaikwad.springaiapps.common.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link Criterion} factories and evaluation semantics — the
 * normalization contracts are part of the module's promise, so they are locked down
 * here.
 */
class CriterionTest {

    private static final EvalCase CASE = new EvalCase("case-1", "What is Spring AI?", "Spring AI is a framework");

    @Test
    void containsPassesWhenTextIsPresentIgnoringCase() {
        Criterion criterion = Criterion.contains("spring ai");

        assertThat(criterion.isSatisfiedBy(CASE)).isTrue();
        assertThat(criterion.name()).isEqualTo("contains");
        assertThat(criterion.expectation()).isEqualTo("answer contains \"spring ai\"");
    }

    @Test
    void containsFailsWhenTextIsAbsent() {
        assertThat(Criterion.contains("quarkus").isSatisfiedBy(CASE)).isFalse();
    }

    @Test
    void containsIsCaseInsensitiveOnBothSides() {
        assertThat(Criterion.contains("SPRING").isSatisfiedBy(CASE)).isTrue();
        assertThat(Criterion.contains("framework").isSatisfiedBy(new EvalCase("c", "q", "FRAMEWORK"))).isTrue();
    }

    @Test
    void excludesIsTheInverseOfContains() {
        assertThat(Criterion.excludes("quarkus").isSatisfiedBy(CASE)).isTrue();
        assertThat(Criterion.excludes("SPRING ai").isSatisfiedBy(CASE)).isFalse();
    }

    @Test
    void matchesRegexFindsAnOccurrenceAnywhereInTheAnswer() {
        assertThat(Criterion.matchesRegex("\\d+").isSatisfiedBy(new EvalCase("c", "q", "version 2 released"))).isTrue();
        assertThat(Criterion.matchesRegex("^version 9").isSatisfiedBy(new EvalCase("c", "q", "version 2 released"))).isFalse();
    }

    @Test
    void matchesRegexFailsFastOnInvalidPattern() {
        assertThatThrownBy(() -> Criterion.matchesRegex("["))
                .isInstanceOf(java.util.regex.PatternSyntaxException.class);
    }

    @Test
    void equalsTextIgnoresCaseAndWhitespaceRuns() {
        Criterion criterion = Criterion.equalsText("Spring AI is a framework");

        assertThat(criterion.isSatisfiedBy(new EvalCase("c", "q", "  spring   ai IS A framework "))).isTrue();
        assertThat(criterion.isSatisfiedBy(new EvalCase("c", "q", "Spring AI is a database"))).isFalse();
    }

    @Test
    void equalsTextFailsOnEmptyAnswer() {
        assertThat(Criterion.equalsText("something").isSatisfiedBy(new EvalCase("c", "q", ""))).isFalse();
    }

    @Test
    void ofAppliesCustomPredicateToTheWholeCase() {
        Criterion questionLength = Criterion.of("shortQuestion", "question has at most 5 characters",
                evalCase -> evalCase.question().length() <= 5);

        assertThat(questionLength.isSatisfiedBy(new EvalCase("c", "hi", "answer"))).isTrue();
        assertThat(questionLength.isSatisfiedBy(CASE)).isFalse();
        assertThat(questionLength.name()).isEqualTo("shortQuestion");
        assertThat(questionLength.expectation()).isEqualTo("question has at most 5 characters");
    }

    @Test
    void blankFactoryArgumentsAreRejected() {
        assertThatThrownBy(() -> Criterion.contains(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.excludes("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.matchesRegex(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.equalsText("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.of(" ", "expectation", evalCase -> true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.of("name", " ", evalCase -> true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Criterion.of("name", "expectation", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCaseIsRejectedAtEvaluationTime() {
        assertThatThrownBy(() -> Criterion.contains("x").isSatisfiedBy(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
