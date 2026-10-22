package dev.prasadgaikwad.springaiapps.common.eval;

import org.springframework.util.Assert;

/**
 * The outcome of applying one {@link Criterion} to one {@link EvalCase} — the atomic
 * unit of an {@link EvalReport}.
 *
 * @param criterionName the criterion's {@link Criterion#name()}
 * @param expectation   the criterion's {@link Criterion#expectation()}, stated in words
 *                      so a report reads without the criterion definition beside it
 * @param passed        whether the case satisfied the criterion
 */
public record Grade(String criterionName, String expectation, boolean passed) {

    public Grade {
        Assert.hasText(criterionName, "criterionName cannot be blank");
        Assert.hasText(expectation, "expectation cannot be blank");
    }
}
