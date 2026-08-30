package dev.prasadgaikwad.springaiapps.rag;

/**
 * Response body for the {@code POST /api/rag/ingest} endpoint.
 *
 * <p>Describes what the ETL pipeline stored so ingestion is observable
 * (and repeatable, since chunk ids are content-derived and upserted).
 *
 * @param source   the source the chunks were read from
 * @param documents number of documents the reader produced before splitting
 * @param chunks   number of chunks embedded and written to the Vector store
 * @param status   "ok" on success, a non-2xx status otherwise
 */
public record IngestResponse(String source, int documents, int chunks, String status) {

    public static IngestResponse ok(String source, int documents, int chunks) {
        return new IngestResponse(source, documents, chunks, "ok");
    }
}
