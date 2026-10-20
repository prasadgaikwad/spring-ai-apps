package dev.prasadgaikwad.springaiapps.common.eval;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the {@link EvalHarness} run/evaluate contract: grading every case
 * against every criterion, deriving case and summary outcomes, and rejecting ambiguous
 * inputs.
 */
class EvalHarnessTest {

    private final EvalHarness harness = new EvalHarness();

    private static final EvalCase PASSING_CASE = new EvalCase("pass-1", "What is Spring AI?",
            "Spring AI is a framework for building AI applications");

    private static final EvalCase FAILING_CASE = new EvalCase("fail-1", "What is Spring AI?",
            "It is a database");

    @Test
    void runGradesEveryCaseAgainstEveryCriterionInInputOrder() {
        EvalReport report = harness.run(List.of(PASSING_CASE, FAILING_CASE),
                List.of(Criterion.contains("Spring AI"), Criterion.excludes("database")));

        assertThat(report.cases()).extracting(EvalReport.CaseReport::caseId)
                .containsExactly("pass-1", "fail-1");
        assertThat(report.cases()).allSatisfy(caseReport ->
                assertThat(caseReport.grades()).extracting(Grade::criterionName)
                        .containsExactly("contains", "excludes"));
        assertThat(report.cases().get(0).question()).isEqualTo(PASSING_CASE.question());
    }

    @Test
    void casePassesOnlyWhenEveryCriterionPassed() {
        EvalReport report = harness.run(List.of(PASSING_CASE, FAILING_CASE),
                List.of(Criterion.contains("Spring AI"), Criterion.excludes("database")));

        assertThat(report.cases().get(0).passed()).isTrue();
        assertThat(report.cases().get(1).passed()).isFalse();
    }

    @Test
    void summaryAggregatesCaseAndGradeCounts() {
        EvalCase partiallyFailing = new EvalCase("partial-1", "q", "Spring AI is a database");

        EvalReport report = harness.run(List.of(PASSING_CASE, FAILING_CASE, partiallyFailing),
                List.of(Criterion.contains("Spring AI"), Criterion.excludes("database")));

        EvalReport.Summary summary = report.summary();
        assertThat(summary.totalCases()).isEqualTo(3);
        assertThat(summary.passedCases()).isEqualTo(1);
        assertThat(summary.failedCases()).isEqualTo(2);
        assertThat(summary.totalGrades()).isEqualTo(6);
        assertThat(summary.passedGrades()).isEqualTo(3);
        assertThat(summary.passRate()).isCloseTo(1.0 / 3.0, within(1e-9));
    }

    @Test
    void emptyAnswerIsGradableNotRejected() {
        EvalReport report = harness.run(List.of(new EvalCase("empty-1", "q", "")),
                List.of(Criterion.contains("anything"), Criterion.excludes("anything")));

        assertThat(report.cases().get(0).passed()).isFalse();
        assertThat(report.cases().get(0).grades())
                .extracting(Grade::passed)
                .containsExactly(false, true);
    }

    @Test
    void duplicateCaseIdsAreRejected() {
        assertThatThrownBy(() -> harness.run(List.of(PASSING_CASE, PASSING_CASE), List.of(Criterion.contains("x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pass-1");
    }

    @Test
    void emptyInputsAreRejected() {
        assertThatThrownBy(() -> harness.run(List.of(), List.of(Criterion.contains("x"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> harness.run(List.of(PASSING_CASE), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void evaluateProducesAGradeCarryingTheCriterion() {
        Criterion criterion = Criterion.contains("Spring AI");

        Grade grade = harness.evaluate(PASSING_CASE, criterion);

        assertThat(grade.criterionName()).isEqualTo("contains");
        assertThat(grade.expectation()).isEqualTo(criterion.expectation());
        assertThat(grade.passed()).isTrue();
    }

    @Test
    void evaluateRejectsNullArguments() {
        assertThatThrownBy(() -> harness.evaluate(null, Criterion.contains("x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> harness.evaluate(PASSING_CASE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
