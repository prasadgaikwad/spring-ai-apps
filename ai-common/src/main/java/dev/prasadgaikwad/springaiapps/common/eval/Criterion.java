package dev.prasadgaikwad.springaiapps.common.eval;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * A named, deterministic expectation applied to an {@link EvalCase}.
 *
 * <p>Criteria are the data the {@link EvalHarness} grades against: each one is a
 * self-describing rule (name + expectation text for the report) backed by a side-effect-
 * free check. The built-in factories cover the common shapes — required text, forbidden
 * text (the hallucination guard), a pattern, normalized equality — while
 * {@link #of(String, String, Predicate)} admits any custom rule, which is how a
 * consumer grades something richer (e.g. against a reference answer it holds) without
 * extending the harness.
 */
public final class Criterion {

    private final String name;

    private final String expectation;

    private final Predicate<EvalCase> check;

    private Criterion(String name, String expectation, Predicate<EvalCase> check) {
        Assert.hasText(name, "name cannot be blank");
        Assert.hasText(expectation, "expectation cannot be blank");
        Assert.notNull(check, "check cannot be null");
        this.name = name;
        this.expectation = expectation;
        this.check = check;
    }

    /**
     * The answer must contain {@code expectedText}, compared case-insensitively.
     */
    public static Criterion contains(String expectedText) {
        Assert.hasText(expectedText, "expectedText cannot be blank");
        return new Criterion("contains", "answer contains \"" + expectedText + "\"",
                evalCase -> containsIgnoreCase(evalCase.answer(), expectedText));
    }

    /**
     * The answer must <em>not</em> contain {@code forbiddenText}, compared
     * case-insensitively — the hallucination guard.
     */
    public static Criterion excludes(String forbiddenText) {
        Assert.hasText(forbiddenText, "forbiddenText cannot be blank");
        return new Criterion("excludes", "answer does not contain \"" + forbiddenText + "\"",
                evalCase -> !containsIgnoreCase(evalCase.answer(), forbiddenText));
    }

    /**
     * The answer must contain at least one occurrence of {@code regex}. An invalid
     * pattern fails fast here, not at grading time.
     */
    public static Criterion matchesRegex(String regex) {
        Assert.hasText(regex, "regex cannot be blank");
        Pattern pattern = Pattern.compile(regex);
        return new Criterion("matchesRegex", "answer matches pattern /" + regex + "/",
                evalCase -> pattern.matcher(evalCase.answer()).find());
    }

    /**
     * The answer must equal {@code expectedText} after trimming, collapsing internal
     * whitespace runs to single spaces, and ignoring case.
     */
    public static Criterion equalsText(String expectedText) {
        Assert.hasText(expectedText, "expectedText cannot be blank");
        String normalizedExpected = normalize(expectedText);
        return new Criterion("equalsText",
                "answer equals \"" + expectedText + "\" (ignoring case and whitespace)",
                evalCase -> normalize(evalCase.answer()).equals(normalizedExpected));
    }

    /**
     * An arbitrary rule: {@code name} keys the criterion in the report and
     * {@code expectation} states it in words; {@code check} does the deciding.
     */
    public static Criterion of(String name, String expectation, Predicate<EvalCase> check) {
        return new Criterion(name, expectation, check);
    }

    public String name() {
        return this.name;
    }

    public String expectation() {
        return this.expectation;
    }

    public boolean isSatisfiedBy(EvalCase evalCase) {
        Assert.notNull(evalCase, "evalCase cannot be null");
        return this.check.test(evalCase);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
