package dev.prasadgaikwad.springaiapps.structured;

import java.util.List;

/**
 * A typed extraction plus the validation outcome.
 *
 * <p>{@code valid} is derived (no violations) and stored as a component so it appears in
 * the JSON body for clients; {@code violations} remains the source of truth.
 */
public record ExtractedResult<T>(T value, List<String> violations, boolean valid) {

    public ExtractedResult {
        violations = List.copyOf(violations);
    }

    public ExtractedResult(T value, List<String> violations) {
        this(value, violations, violations.isEmpty());
    }
}
