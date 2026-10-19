package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

/**
 * Response body for the {@code POST /api/rag/query} endpoint.
 *
 * <p>Besides the answer itself, reports which retrieved chunks grounded it, so callers
 * (and the chat UI) can show the evidence behind the answer.
 *
 * @param question the question that was asked
 * @param answer   the model's answer, generated only from the retrieved context
 * @param sources  the chunks that were fed to the model as context, best match first
 * @param status   "ok" on success, a non-2xx status otherwise
 */
public record QueryResponse(String question, String answer, List<Source> sources, String status) {

    /**
     * One retrieved chunk that grounded the answer.
     *
     * @param id   the chunk's Vector store id
     * @param text the chunk's text
     */
    public record Source(String id, String text) {

        static Source from(org.springframework.ai.document.Document document) {
            return new Source(document.getId(), document.getText());
        }
    }

    public static QueryResponse ok(String question, String answer, List<Source> sources) {
        return new QueryResponse(question, answer, sources, "ok");
    }
}
