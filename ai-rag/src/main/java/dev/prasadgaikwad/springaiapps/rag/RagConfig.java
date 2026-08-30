package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Beans local to the {@code ai-rag} Demo module.
 *
 * <p>The {@link TokenTextSplitter} is not auto-configured by Spring AI, so it is
 * declared here and shared by the ingestion pipeline. Chunk size is kept modest so a
 * sample document produces several retrievable chunks.
 */
@Configuration
public class RagConfig {

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
    }

    /**
     * The {@code ai-rag} module depends on PGVector, whose auto-configuration requires a
     * single {@link EmbeddingModel}. Because multiple provider starters (Ollama, OpenAI)
     * are on the classpath and Spring AI auto-configures all of them unconditionally, the
     * active provider's embedding needs to be the {@link Primary} one so the Vector store
     * is wired to the embeddings it will be queried with (ADR-0001: provider↔embedding
     * coupling). Each provider profile re-points the primary to its own embedding model.
     */
    @Bean
    @Primary
    @Profile("ollama")
    public EmbeddingModel primaryOllamaEmbedding(EmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }

    @Bean
    @Primary
    @Profile("openai")
    public EmbeddingModel primaryOpenAiEmbedding(EmbeddingModel openAiEmbeddingModel) {
        return openAiEmbeddingModel;
    }
}
