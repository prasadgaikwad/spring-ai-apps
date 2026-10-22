package dev.prasadgaikwad.springaiapps.structured;

/**
 * Request to extract a structured POJO from free text.
 *
 * <p>{@code type} selects the target POJO: {@code candidate} → {@link CandidateProfile},
 * {@code order} → {@link CustomerOrder}. Keep it a small closed set so the contract is
 * documented and the schema is fixed.
 */
public record ExtractionRequest(String text, String type) {
}
