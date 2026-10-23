package dev.prasadgaikwad.springaiapps.eval;

import java.util.List;

/**
 * Request to run the Eval harness: grade every Q&A pair against every criterion.
 *
 * <p>An omitted {@code answer} means "generate one" — the Demo module answers the
 * question with its configured {@code ChatModel} before grading. Criteria arrive as
 * {@link CriterionSpec}s so the contract is pure JSON.
 */
public record EvalRunRequest(List<QAPair> pairs, List<CriterionSpec> criteria) {

    /**
     * One Q&amp;A pair; {@code answer} may be {@code null}/blank to request generation.
     */
    public record QAPair(String question, String answer) {
    }
}
