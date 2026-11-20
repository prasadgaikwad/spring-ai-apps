package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure-logic tests for reciprocal rank fusion; no database or model needed. */
class HybridDocumentRetrieverTest {

    @Test
    void accumulatesRrfScoresAcrossBothRetrievers() {
        List<Document> semantic = List.of(doc("a", "alpha", "semantic"), doc("b", "bravo", "semantic"));
        List<Document> keyword = List.of(doc("b", "bravo", "keyword"), doc("c", "charlie", "keyword"));

        List<Document> fused = HybridDocumentRetriever.reciprocalRankFusion(semantic, keyword, 10);

        // b appears in both lists, so its scores accumulate above the rank-1-only documents.
        assertThat(ids(fused)).containsExactly("b", "a", "c");
    }

    @Test
    void rankTiesKeepSemanticFirstSeenOrder() {
        List<Document> semantic = List.of(doc("a", "alpha", "s"), doc("b", "bravo", "s"));
        List<Document> keyword = List.of(doc("c", "charlie", "k"), doc("d", "delta", "k"));

        List<Document> fused = HybridDocumentRetriever.reciprocalRankFusion(semantic, keyword, 10);

        assertThat(ids(fused)).containsExactly("a", "c", "b", "d");
    }

    @Test
    void dedupesOverlappingIdsAndKeepsSemanticMetadata() {
        List<Document> semantic = List.of(doc("x", "semantic text", "semantic"));
        List<Document> keyword = List.of(doc("x", "keyword text", "keyword"));

        List<Document> fused = HybridDocumentRetriever.reciprocalRankFusion(semantic, keyword, 10);

        assertThat(fused).hasSize(1);
        assertThat(fused.get(0).getId()).isEqualTo("x");
        assertThat(fused.get(0).getText()).isEqualTo("semantic text");
        assertThat(fused.get(0).getMetadata()).containsEntry("source", "semantic");
    }

    @Test
    void respectsTopK() {
        List<Document> semantic = List.of(doc("a", "a", "s"), doc("b", "b", "s"), doc("c", "c", "s"));

        assertThat(ids(HybridDocumentRetriever.reciprocalRankFusion(semantic, List.of(), 2)))
                .containsExactly("a", "b");
    }

    @Test
    void handlesEmptyVectorAndKeywordResults() {
        List<Document> semantic = List.of(doc("a", "a", "s"), doc("b", "b", "s"));

        assertThat(HybridDocumentRetriever.reciprocalRankFusion(List.of(), List.of(), 10)).isEmpty();
        assertThat(ids(HybridDocumentRetriever.reciprocalRankFusion(semantic, List.of(), 10)))
                .containsExactly("a", "b");
        assertThat(ids(HybridDocumentRetriever.reciprocalRankFusion(List.of(),
                List.of(doc("k1", "k1", "k"), doc("k2", "k2", "k")), 10)))
                .containsExactly("k1", "k2");
    }

    private static List<String> ids(List<Document> documents) {
        return documents.stream().map(Document::getId).toList();
    }

    private static Document doc(String id, String text, String source) {
        return Document.builder().id(id).text(text).metadata("source", source).build();
    }
}
