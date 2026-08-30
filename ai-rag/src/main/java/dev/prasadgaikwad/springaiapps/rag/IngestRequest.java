package dev.prasadgaikwad.springaiapps.rag;

/**
 * Request body for the {@code POST /api/rag/ingest} endpoint.
 *
 * <p>Exactly one of {@code source} or {@code text} should be provided:
 * {@code source} is a resource location (classpath path or URL) the ETL pipeline reads
 * and splits; {@code text} is raw inline content to ingest directly.
 *
 * @param source resource location to read document content from (mutually exclusive with {@code text})
 * @param text   raw inline content to ingest (mutually exclusive with {@code source})
 */
public record IngestRequest(String source, String text) {
}
