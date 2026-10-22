package dev.prasadgaikwad.springaiapps.common.eval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * The regression-style report of one {@link EvalHarness#run(List, List)}: a
 * pass/fail {@link CaseReport} per evaluated case plus aggregate numbers, shaped so two
 * reports can be compared run over run.
 *
 * <p>Case order and grade order follow the input order, so two runs over the same inputs
 * produce structurally identical reports and diffs stay meaningful.
 *
 * @param cases   per-case results, in input order; an unmodifiable copy
 * @param summary aggregate counts derived from {@code cases}
 */
public record EvalReport(List<CaseReport> cases, Summary summary) {

    public EvalReport {
        Assert.notEmpty(cases, "cases cannot be empty");
        Assert.notNull(summary, "summary cannot be null");
        cases = List.copyOf(cases);
    }

    /**
     * The result of evaluating one case against every criterion.
     *
     * <p>{@code passed} is <em>derived</em> from {@code grades} (a case passes only if
     * all of its grades pass) and any value supplied to the constructor is ignored —
     * the component exists for serialization, never as an independent fact.
     *
     * @param caseId   the {@link EvalCase#id()}
     * @param question the {@link EvalCase#question()}
     * @param grades   one {@link Grade} per criterion, in criterion order; non-empty
     * @param passed   derived: all grades passed
     */
    public record CaseReport(String caseId, String question, List<Grade> grades, boolean passed) {

        public CaseReport {
            Assert.hasText(caseId, "caseId cannot be blank");
            Assert.hasText(question, "question cannot be blank");
            Assert.notEmpty(grades, "grades cannot be empty");
            grades = List.copyOf(grades);
            passed = grades.stream().allMatch(Grade::passed);
        }

        /**
         * Builds a case report without supplying the derived {@code passed} flag.
         */
        public static CaseReport of(String caseId, String question, List<Grade> grades) {
            return new CaseReport(caseId, question, grades, true);
        }
    }

    /**
     * Aggregate counts for a run.
     *
     * @param totalCases   number of evaluated cases
     * @param passedCases  cases where every criterion passed
     * @param failedCases  cases where at least one criterion failed
     * @param totalGrades  number of {@link Grade} entries across all cases
     * @param passedGrades number of grades that passed
     * @param passRate     {@code passedCases / totalCases}, or {@code 0.0} for no cases
     */
    public record Summary(int totalCases, int passedCases, int failedCases, int totalGrades, int passedGrades,
            double passRate) {

        public Summary {
            Assert.isTrue(totalCases >= 0, "totalCases cannot be negative");
            Assert.isTrue(passedCases >= 0 && failedCases >= 0, "case counts cannot be negative");
            Assert.isTrue(passedCases + failedCases == totalCases, "case counts must add up to totalCases");
            Assert.isTrue(totalGrades >= 0 && passedGrades >= 0, "grade counts cannot be negative");
            Assert.isTrue(passedGrades <= totalGrades, "passedGrades cannot exceed totalGrades");
            Assert.isTrue(passRate >= 0.0 && passRate <= 1.0, "passRate must be between 0.0 and 1.0");
        }
    }

    /**
     * The case ids that are regressions against {@code baseline}: present and passing
     * there, present and failing here. Failures that already existed in the baseline and
     * cases absent from this run are not regressions.
     *
     * @param baseline a report from an earlier run to compare against
     * @return regressed case ids, in this report's case order
     */
    public List<String> regressionsAgainst(EvalReport baseline) {
        Assert.notNull(baseline, "baseline cannot be null");
        Map<String, CaseReport> baselineByCase = new LinkedHashMap<>();
        for (CaseReport baselineCase : baseline.cases()) {
            baselineByCase.putIfAbsent(baselineCase.caseId(), baselineCase);
        }
        return this.cases.stream()
                .filter(caseReport -> !caseReport.passed())
                .map(CaseReport::caseId)
                .filter(caseId -> {
                    CaseReport baselineCase = baselineByCase.get(caseId);
                    return baselineCase != null && baselineCase.passed();
                })
                .toList();
    }
}
