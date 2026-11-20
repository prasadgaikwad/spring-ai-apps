package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure-logic tests for the deterministic lexical reranker. */
class LexicalRerankerTest {

    @Test
    void filtersStopwordsAndOrdersByCoverage() {
        List<Document> documents = List.of(
                doc("stop", "what is the and"),
                doc("etl", "ETL"),
                doc("none", "unrelated content"),
                doc("both", "etl pipeline"));

        List<Document> ranked = new LexicalReranker(10)
                .process(new Query("What is the ETL pipeline?"), documents);

        assertThat(ids(ranked)).containsExactly("both", "etl", "stop", "none");
    }

    @Test
    void ordersByOverlapAndCoverage() {
        List<Document> documents = List.of(
                doc("alpha", "alpha only"),
                doc("all", "alpha beta gamma"),
                doc("none", "delta epsilon"),
                doc("two", "alpha beta"));

        List<Document> ranked = new LexicalReranker(10)
                .process(new Query("alpha beta gamma"), documents);

        assertThat(ids(ranked)).containsExactly("all", "two", "alpha", "none");
    }

    @Test
    void deterministicTieBreaksKeepInputOrder() {
        List<Document> documents = List.of(
                doc("d1", "alpha"),
                doc("d2", "beta"),
                doc("d3", "alpha unrelated"),
                doc("d4", "beta unrelated"));

        List<Document> ranked = new LexicalReranker(10)
                .process(new Query("alpha beta"), documents);

        assertThat(ids(ranked)).containsExactly("d1", "d2", "d3", "d4");
    }

    @Test
    void normalizesCase() {
        List<Document> documents = List.of(
                doc("lower", "spring ai"),
                doc("mixed", "SpRiNg Ai"),
                doc("partial", "SPRING"));

        List<Document> ranked = new LexicalReranker(10)
                .process(new Query("SPRING AI"), documents);

        assertThat(ids(ranked)).containsExactly("lower", "mixed", "partial");
    }

    @Test
    void emptyAndAllStopwordQueriesKeepInputOrder() {
        List<Document> documents = List.of(doc("a", "alpha"), doc("b", "beta"));

        // Query itself rejects blank text, so mock the token source for the empty case.
        Query emptyQuery = mock(Query.class);
        when(emptyQuery.text()).thenReturn("");
        assertThat(ids(new LexicalReranker(10).process(emptyQuery, documents)))
                .containsExactly("a", "b");
        assertThat(ids(new LexicalReranker(10).process(new Query("what is the"), documents)))
                .containsExactly("a", "b");
    }

    @Test
    void limitsToMaxDocuments() {
        List<Document> documents = List.of(
                doc("a", "alpha"),
                doc("b", "beta"),
                doc("c", "gamma"));

        assertThat(ids(new LexicalReranker(2).process(new Query("alpha beta gamma"), documents)))
                .containsExactly("a", "b");
    }

    @Test
    void rejectsNonPositiveMaxDocuments() {
        assertThatThrownBy(() -> new LexicalReranker(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDocuments");
    }

    private static List<String> ids(List<Document> documents) {
        return documents.stream().map(Document::getId).toList();
    }

    private static Document doc(String id, String text) {
        return Document.builder().id(id).text(text).build();
    }
}
