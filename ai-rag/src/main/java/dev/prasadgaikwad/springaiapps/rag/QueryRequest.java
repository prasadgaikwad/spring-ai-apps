package dev.prasadgaikwad.springaiapps.rag;

/**
 * Request body for the {@code POST /api/rag/query} endpoint.
 *
 * @param question the natural-language question to answer from the ingested documents
 */
public record QueryRequest(String question) {
}
