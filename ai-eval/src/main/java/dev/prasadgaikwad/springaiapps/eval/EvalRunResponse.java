package dev.prasadgaikwad.springaiapps.eval;

import java.util.List;

import dev.prasadgaikwad.springaiapps.common.eval.EvalReport;

/**
 * The stored result of one Eval run: the graded {@link EvalReport} plus the case ids
 * that regressed against the previous run (passed there, failing now).
 */
public record EvalRunResponse(String runId, EvalReport report, List<String> regressions) {

    public EvalRunResponse {
        regressions = List.copyOf(regressions);
    }
}
