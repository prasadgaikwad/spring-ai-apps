package dev.prasadgaikwad.springaiapps.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * Hybrid document retriever: fuses <em>semantic</em> (Vector store embedding) and
 * <em>keyword</em> (Postgres full-text) search over the same PGVector table.
 *
 * <p>Neither signal alone is enough for a good query path: embedding search finds
 * conceptually related chunks that may not share the query's wording, while full-text
 * search finds exact term matches ("ETL pipeline") that embeddings can rank low. Both
 * ranked lists are combined with <em>Reciprocal Rank Fusion</em> (RRF), the standard
 * deterministic hybrid-search fusion: each document scores
 * {@code sum over retrievers of 1 / (k + rank)}, with {@code k = 60} damping the
 * influence of each list's top ranks. RRF needs no score calibration between the two
 * retrievers because it fuses <em>ranks</em>, not raw scores.
 *
 * <p>The keyword leg queries the {@code content} column of the Vector store's table
 * directly (Spring AI's filter DSL only reaches {@code metadata}, not content). This
 * couples the retriever to the PGVector schema — acceptable here, since {@code ai-rag}
 * is the PGVector-backed demo and the table name comes from Spring AI's own
 * {@code spring.ai.vectorstore.pgvector.table-name} property.
 */
public final class HybridDocumentRetriever implements DocumentRetriever {

    /** Rank-fusion damping factor (standard RRF constant). */
    private static final double RRF_K = 60.0;

    private final VectorStoreDocumentRetriever semanticRetriever;

    private final JdbcTemplate jdbcTemplate;

    private final String tableName;

    private final int topK;

    public HybridDocumentRetriever(VectorStoreDocumentRetriever semanticRetriever, JdbcTemplate jdbcTemplate,
            String tableName, int topK) {
        Assert.notNull(semanticRetriever, "semanticRetriever cannot be null");
        Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
        Assert.hasText(tableName, "tableName cannot be blank");
        Assert.isTrue(topK > 0, "topK must be greater than 0");
        this.semanticRetriever = semanticRetriever;
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.topK = topK;
    }

    @Override
    public List<Document> retrieve(Query query) {
        Assert.notNull(query, "query cannot be null");
        List<Document> semantic = this.semanticRetriever.retrieve(query);
        List<Document> keyword = keywordSearch(query.text());
        return reciprocalRankFusion(semantic, keyword);
    }

    /**
     * Full-text search over the stored chunk text, ranked by {@code ts_rank}.
     */
    private List<Document> keywordSearch(String text) {
        String sql = """
                SELECT id, content FROM %s
                WHERE content IS NOT NULL
                  AND to_tsvector('english', content) @@ plainto_tsquery('english', ?)
                ORDER BY ts_rank(to_tsvector('english', content), plainto_tsquery('english', ?)) DESC
                LIMIT ?""".formatted(this.tableName);
        return this.jdbcTemplate.query(sql, (rs, rowNum) -> Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .build(), text, text, this.topK);
    }

    /**
     * Fuses the two ranked lists by Reciprocal Rank Fusion, most-relevant first.
     * Documents found by both retrievers accumulate both contributions; the semantic
     * copy wins on ties so fused documents keep their metadata.
     */
    private List<Document> reciprocalRankFusion(List<Document> semantic, List<Document> keyword) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> documentsById = new LinkedHashMap<>();
        for (int rank = 0; rank < semantic.size(); rank++) {
            accumulate(scores, documentsById, semantic.get(rank), rank);
        }
        for (int rank = 0; rank < keyword.size(); rank++) {
            accumulate(scores, documentsById, keyword.get(rank), rank);
        }
        // Stream.sorted is stable, so equal scores keep first-seen (semantic-first) order.
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(this.topK)
                .map(entry -> documentsById.get(entry.getKey()))
                .toList();
    }

    private static void accumulate(Map<String, Double> scores, Map<String, Document> documentsById, Document document,
            int rank) {
        documentsById.putIfAbsent(document.getId(), document);
        scores.merge(document.getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
    }
}
