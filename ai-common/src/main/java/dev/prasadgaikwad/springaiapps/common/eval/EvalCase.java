package dev.prasadgaikwad.springaiapps.common.eval;

import org.springframework.util.Assert;

/**
 * One Q&amp;A pair for the {@link EvalHarness}: the question that was asked and the
 * answer produced for it, identified for report tracking.
 *
 * <p>The answer may be an empty string (a model can legitimately return nothing);
 * criteria decide whether that is acceptable. Blank ids and questions are rejected so
 * report entries always carry a case identity and a question worth reading.
 *
 * @param id       stable case identifier, unique within one harness run (report entries
 *                 and baseline comparisons are keyed on it)
 * @param question the question that was asked
 * @param answer   the answer under evaluation (may be empty, never {@code null})
 */
public record EvalCase(String id, String question, String answer) {

    public EvalCase {
        Assert.hasText(id, "id cannot be blank");
        Assert.hasText(question, "question cannot be blank");
        Assert.notNull(answer, "answer cannot be null");
    }
}
