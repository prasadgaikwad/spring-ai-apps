package dev.prasadgaikwad.springaiapps.eval;

import java.util.regex.PatternSyntaxException;

import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;
import dev.prasadgaikwad.springaiapps.common.eval.Criterion;

/**
 * JSON shape of one criterion — a {@code type} naming one of the built-in
 * {@link Criterion} factories plus the {@code value} it expects.
 *
 * <p>Supported types: {@code contains}, {@code excludes}, {@code matchesRegex},
 * {@code equalsText}. The custom-rule escape hatch ({@code Criterion.of}) is
 * in-process only and intentionally not exposed over REST.
 */
public record CriterionSpec(String type, String value) {

    /**
     * Maps this spec to the shared harness's {@link Criterion}, rejecting unknown
     * types, blank values, and invalid regexes with {@link BadRequestException}.
     */
    public Criterion toCriterion() {
        if (!StringUtils.hasText(type) || !StringUtils.hasText(value)) {
            throw new BadRequestException("Each criterion requires a non-blank 'type' and 'value'");
        }
        return switch (type) {
            case "contains" -> Criterion.contains(value);
            case "excludes" -> Criterion.excludes(value);
            case "matchesRegex" -> {
                try {
                    yield Criterion.matchesRegex(value);
                }
                catch (PatternSyntaxException ex) {
                    throw new BadRequestException("Invalid regex for criterion type 'matchesRegex': " + value);
                }
            }
            case "equalsText" -> Criterion.equalsText(value);
            default -> throw new BadRequestException(
                    "Unknown criterion type: " + type + " (expected one of contains, excludes, matchesRegex, equalsText)");
        };
    }
}
