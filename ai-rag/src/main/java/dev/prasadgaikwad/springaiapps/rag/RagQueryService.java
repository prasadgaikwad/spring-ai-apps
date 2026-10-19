package dev.prasadgaikwad.springaiapps.rag;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

/**
 * The query path for {@code ai-rag}: retrieval → rerank → generate.
 *
 * <p>Delegates the whole pipeline to the {@link RetrievalAugmentationAdvisor} bound to
 * the module's {@link ChatClient}: the advisor runs {@link HybridDocumentRetriever}
 * (hybrid retrieval), {@link LexicalReranker} (rerank), and the grounded prompt
 * augmentation before the active provider's model generates the answer. This service
 * shapes that result into the REST contract, including the retrieved chunks — the
 * advisor publishes them as {@link RetrievalAugmentationAdvisor#DOCUMENT_CONTEXT} on the
 * response context, which is what makes the answer's evidence observable.
 */
@Service
public class RagQueryService {

    private final ChatClient chatClient;

    public RagQueryService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public QueryResponse ask(String question) {
        ChatClientResponse response = this.chatClient.prompt()
                .user(question)
                .call()
                .chatClientResponse();
        String answer = response.chatResponse().getResult().getOutput().getText();
        List<QueryResponse.Source> sources = retrievedDocuments(response).stream()
                .map(QueryResponse.Source::from)
                .toList();
        return QueryResponse.ok(question, answer, sources);
    }

    @SuppressWarnings("unchecked")
    private static List<Document> retrievedDocuments(ChatClientResponse response) {
        Object documents = response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        return documents instanceof List<?> list ? (List<Document>) list : List.of();
    }
}
