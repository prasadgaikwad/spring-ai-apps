package dev.prasadgaikwad.springaiapps.rag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;

/**
 * Deterministic lexical re-ranker: reorders retrieved chunks by how much of the
 * question's vocabulary each one actually contains, then cuts to the best
 * {@code maxDocuments}.
 *
 * <p>Runs after retrieval fusion and before generation — the "rerank" stage of the
 * retrieval → rerank → generate query path. RRF fusion only knows each retriever's
 * <em>rank</em>, not how strongly a chunk matches the question; this reranker adds that
 * signal with plain term coverage (a cross-encoder-free stand-in, kept offline and
 * deterministic so the demo needs no extra model). The sort is stable, so chunks with
 * equal coverage keep their fused order — semantic-first results survive ties.
 */
public final class LexicalReranker implements DocumentPostProcessor {

    /**
     * Minimal English stopword list, so function words do not inflate the coverage of
     * every chunk ("what is the ..." matching on "the").
     */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "could", "did", "do", "does",
            "for", "from", "had", "has", "have", "how", "i", "if", "in", "into", "is", "it", "its", "may", "might",
            "of", "on", "or", "should", "that", "the", "their", "then", "there", "these", "they", "this", "to",
            "was", "were", "what", "when", "where", "which", "who", "why", "will", "would", "with", "you", "your");

    private final int maxDocuments;

    public LexicalReranker(int maxDocuments) {
        Assert.isTrue(maxDocuments > 0, "maxDocuments must be greater than 0");
        this.maxDocuments = maxDocuments;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        Assert.notNull(query, "query cannot be null");
        Assert.notNull(documents, "documents cannot be null");
        Set<String> queryTerms = tokenize(query.text());
        return documents.stream()
                .sorted(Comparator.comparingDouble((Document document) -> -coverage(queryTerms, document)))
                .limit(this.maxDocuments)
                .toList();
    }

    private static double coverage(Set<String> queryTerms, Document document) {
        if (queryTerms.isEmpty()) {
            return 0.0;
        }
        Set<String> documentTerms = tokenize(document.getText());
        long matched = queryTerms.stream().filter(documentTerms::contains).count();
        return (double) matched / queryTerms.size();
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(term -> !term.isBlank())
                .filter(term -> !STOPWORDS.contains(term))
                .collect(Collectors.toUnmodifiableSet());
    }
}
