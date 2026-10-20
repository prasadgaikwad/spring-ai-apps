package dev.prasadgaikwad.springaiapps.common.eval;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EvalReport} invariants (immutability, derived case status,
 * summary validation) and for baseline comparison — the regression-diff behaviour is
 * the report's reason to exist, so its edge cases are pinned down here.
 */
class EvalReportTest {

    private static final Criterion CONTAINS_SPRING = Criterion.contains("Spring AI");

    @Test
    void casesAreCopiedSoLaterMutationCannotReachTheReport() {
        List<EvalReport.CaseReport> cases = new ArrayList<>(List.of(caseReport("c1", true)));

        EvalReport report = new EvalReport(cases, summary(1, 1, 0, 1, 1, 1.0));
        cases.clear();

        assertThat(report.cases()).hasSize(1);
        assertThatThrownBy(() -> report.cases().add(caseReport("c2", true)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void caseReportDerivesPassedFromGradesAndIgnoresTheSuppliedFlag() {
        EvalReport.CaseReport mixed = new EvalReport.CaseReport("c1", "q",
                List.of(new Grade("contains", "expectation a", true), new Grade("excludes", "expectation b", false)),
                true);

        assertThat(mixed.passed()).isFalse();

        EvalReport.CaseReport allGreen = new EvalReport.CaseReport("c2", "q",
                List.of(new Grade("contains", "expectation a", true)), false);

        assertThat(allGreen.passed()).isTrue();
    }

    @Test
    void caseReportRejectsEmptyGrades() {
        assertThatThrownBy(() -> new EvalReport.CaseReport("c1", "q", List.of(), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportRejectsEmptyCasesAndNullSummary() {
        assertThatThrownBy(() -> new EvalReport(List.of(), summary(0, 0, 0, 0, 0, 0.0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalReport(List.of(caseReport("c1", true)), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summaryRejectsInconsistentCountsAndOutOfRangePassRate() {
        assertThatThrownBy(() -> new EvalReport.Summary(3, 2, 0, 6, 3, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalReport.Summary(1, 1, 0, 1, 2, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalReport.Summary(1, 1, 0, 1, 1, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvalReport.Summary(-1, -1, 0, 0, 0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regressionsAreCasesThatPassedInTheBaselineAndFailNow() {
        EvalReport baseline = reportOf(caseReport("improved", true), caseReport("stayed-pass", true),
                caseReport("stayed-fail", false), caseReport("new-in-baseline", true));
        EvalReport current = reportOf(caseReport("improved", false), caseReport("stayed-pass", true),
                caseReport("stayed-fail", false), caseReport("brand-new-fail", false));

        assertThat(current.regressionsAgainst(baseline)).containsExactly("improved");
    }

    @Test
    void failuresMissingFromTheBaselineAreNotRegressions() {
        EvalReport baseline = reportOf(caseReport("only-in-baseline", true));
        EvalReport current = reportOf(caseReport("brand-new-fail", false));

        assertThat(current.regressionsAgainst(baseline)).isEmpty();
    }

    @Test
    void regressionsKeepTheCurrentReportsCaseOrder() {
        EvalReport baseline = reportOf(caseReport("b", true), caseReport("a", true));
        EvalReport current = reportOf(caseReport("a", false), caseReport("b", false));

        assertThat(current.regressionsAgainst(baseline)).containsExactly("a", "b");
    }

    @Test
    void duplicateBaselineCaseIdsUseTheFirstOccurrence() {
        EvalReport baseline = reportOf(caseReport("dup", false), caseReport("dup", true));
        EvalReport current = reportOf(caseReport("dup", false));

        assertThat(current.regressionsAgainst(baseline)).isEmpty();
    }

    @Test
    void nullBaselineIsRejected() {
        assertThatThrownBy(() -> reportOf(caseReport("c1", true)).regressionsAgainst(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EvalReport.CaseReport caseReport(String caseId, boolean passed) {
        return new EvalReport.CaseReport(caseId, "question for " + caseId,
                List.of(new Grade(CONTAINS_SPRING.name(), CONTAINS_SPRING.expectation(), passed)), passed);
    }

    private static EvalReport reportOf(EvalReport.CaseReport... cases) {
        List<EvalReport.CaseReport> caseList = List.of(cases);
        int passed = (int) caseList.stream().filter(EvalReport.CaseReport::passed).count();
        return new EvalReport(caseList, summary(caseList.size(), passed, caseList.size() - passed,
                caseList.size(), passed, caseList.size() == 0 ? 0.0 : (double) passed / caseList.size()));
    }

    private static EvalReport.Summary summary(int totalCases, int passedCases, int failedCases, int totalGrades,
            int passedGrades, double passRate) {
        return new EvalReport.Summary(totalCases, passedCases, failedCases, totalGrades, passedGrades, passRate);
    }
}
