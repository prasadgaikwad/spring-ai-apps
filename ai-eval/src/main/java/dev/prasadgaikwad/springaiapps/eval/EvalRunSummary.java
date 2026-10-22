package dev.prasadgaikwad.springaiapps.eval;

/**
 * Compact listing entry for one stored Eval run.
 */
public record EvalRunSummary(String runId, int totalCases, int passedCases, double passRate) {
}
