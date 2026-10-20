package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Beans local to the {@code ai-rag} Demo module: the ETL splitter plus the query-path
 * wiring (retrieval → rerank → generate).
 *
 * <p>The query path is assembled from Spring AI's Modular RAG building blocks rather
 * than hand-rolled orchestration:
 * <ul>
 * <li>{@link HybridDocumentRetriever} does hybrid retrieval (semantic + keyword, fused
 * by Reciprocal Rank Fusion);</li>
 * <li>{@link LexicalReranker} re-ranks the fused chunks and cuts them to the best few;</li>
 * <li>{@link RetrievalAugmentationAdvisor} runs that pipeline before every chat call and
 * injects the surviving chunks into a grounded prompt (its default augmenter instructs
 * the model to answer only from the context and to admit when it does not know);</li>
 * <li>a {@link ChatClient} carries the advisor to the active provider's {@link ChatModel}.</li>
 * </ul>
 *
 * <p>The provider↔embedding selection that pairs the active provider's embedding model
 * with the Vector store — and the provider↔chat-model selection — live in {@code ai-common}
 * ({@code VectorStoreConfiguration}, {@code ChatModelConfiguration}), since they are
 * shared plumbing rather than anything {@code ai-rag}-specific.
 */
@Configuration(proxyBeanMethods = false)
public class RagConfig {

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
    }

    @Bean
    public HybridDocumentRetriever hybridDocumentRetriever(VectorStore vectorStore, JdbcTemplate jdbcTemplate,
            @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName,
            @Value("${spring.ai.rag.retrieval.top-k:10}") int topK) {
        VectorStoreDocumentRetriever semanticRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK)
                .build();
        return new HybridDocumentRetriever(semanticRetriever, jdbcTemplate, tableName, topK);
    }

    @Bean
    public LexicalReranker lexicalReranker(@Value("${spring.ai.rag.rerank.max-documents:5}") int maxDocuments) {
        return new LexicalReranker(maxDocuments);
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(HybridDocumentRetriever hybridDocumentRetriever,
            LexicalReranker lexicalReranker) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(hybridDocumentRetriever)
                .documentPostProcessors(lexicalReranker)
                .build();
    }

    @Bean
    public ChatClient ragChatClient(ChatModel chatModel, RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }
}
