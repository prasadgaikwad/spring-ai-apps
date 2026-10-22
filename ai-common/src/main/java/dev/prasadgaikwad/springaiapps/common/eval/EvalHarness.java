package dev.prasadgaikwad.springaiapps.common.eval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import dev.prasadgaikwad.springaiapps.common.eval.EvalReport.CaseReport;

/**
 * The shared Eval harness: grades a set of {@link EvalCase}s against a set of
 * {@link Criterion}s and produces the {@link EvalReport} entry per case plus the
 * aggregate summary.
 *
 * <p>Deliberately pure logic — no Spring beans, no model calls, no I/O — so it is
 * deterministic to test in isolation (the monorepo's dedicated unit-test seam) and
 * trivial for any Demo module to embed behind its own transport: {@code ai-eval} wires a
 * REST contract around exactly these two methods.
 *
 * <p>The report is regression-shaped: stable per-case ids, pass/fail per case, and
 * {@link EvalReport#regressionsAgainst(EvalReport)} to diff a run against a baseline.
 */
public final class EvalHarness {

    /**
     * Grades every case against every criterion, preserving input order throughout.
     *
     * @param cases    the Q&amp;A pairs to evaluate; non-empty, with unique ids
     * @param criteria the expectations to grade each case against; non-empty
     * @return the report for this run
     * @throws IllegalArgumentException if either input is empty or a case id repeats
     */
    public EvalReport run(List<EvalCase> cases, List<Criterion> criteria) {
        Assert.notEmpty(cases, "cases cannot be empty");
        Assert.notEmpty(criteria, "criteria cannot be empty");
        List<CaseReport> caseReports = new ArrayList<>(cases.size());
        Set<String> seenIds = new HashSet<>();
        for (EvalCase evalCase : cases) {
            if (!seenIds.add(evalCase.id())) {
                throw new IllegalArgumentException("Duplicate EvalCase id: " + evalCase.id());
            }
            List<Grade> grades = new ArrayList<>(criteria.size());
            for (Criterion criterion : criteria) {
                grades.add(evaluate(evalCase, criterion));
            }
            caseReports.add(CaseReport.of(evalCase.id(), evalCase.question(), grades));
        }
        return new EvalReport(caseReports, summarize(caseReports));
    }

    /**
     * Applies one criterion to one case.
     *
     * @return the resulting {@link Grade}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public Grade evaluate(EvalCase evalCase, Criterion criterion) {
        Assert.notNull(evalCase, "evalCase cannot be null");
        Assert.notNull(criterion, "criterion cannot be null");
        return new Grade(criterion.name(), criterion.expectation(), criterion.isSatisfiedBy(evalCase));
    }

    private static EvalReport.Summary summarize(List<CaseReport> cases) {
        int passedCases = (int) cases.stream().filter(CaseReport::passed).count();
        int totalGrades = cases.stream().mapToInt(caseReport -> caseReport.grades().size()).sum();
        int passedGrades = (int) cases.stream()
                .flatMap(caseReport -> caseReport.grades().stream())
                .filter(Grade::passed)
                .count();
        int totalCases = cases.size();
        double passRate = totalCases == 0 ? 0.0 : (double) passedCases / totalCases;
        return new EvalReport.Summary(totalCases, passedCases, totalCases - passedCases, totalGrades, passedGrades,
                passRate);
    }
}
