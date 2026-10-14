package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans local to the {@code ai-rag} Demo module.
 *
 * <p>The {@link TokenTextSplitter} is not auto-configured by Spring AI, so it is
 * declared here and shared by the ingestion pipeline. Chunk size is kept modest so a
 * sample document produces several retrievable chunks.
 *
 * <p>The provider↔embedding selection that pairs the active provider's embedding model
 * with the Vector store lives in {@code ai-common} ({@code VectorStoreConfiguration}),
 * since it is shared plumbing rather than anything {@code ai-rag}-specific.
 */
@Configuration
public class RagConfig {

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
    }
}
