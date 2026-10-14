package dev.prasadgaikwad.springaiapps.common;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Shared Vector store configuration for the monorepo's Vector-store-backed Demo modules.
 *
 * <p>A Vector store is paired with exactly one {@link EmbeddingModel}: the store persists
 * that model's embeddings and must later retrieve using the same model, so the
 * provider↔embedding coupling is a property of the store, not of any single demo
 * (ADR-0001). This module makes that coupling explicit once, for every demo that uses a
 * Vector store.
 *
 * <p>Spring AI auto-configures <em>all</em> provider embedding models that are on the
 * classpath (Ollama and OpenAI here) and fails to wire PGVector when more than one is
 * present. The active provider profile therefore marks its own embedding model as
 * {@link Primary}, so the auto-configured Vector store binds to the embeddings the demo
 * will actually query with. Switching the active profile re-points the primary and, per
 * ADR-0001, requires re-ingestion.
 *
 * <p>This configuration deliberately activates only when a {@code VectorStore} is on the
 * classpath (via {@link ConditionalOnClass}, referenced by name to avoid a compile-time
 * dependency on the Vector store API). Demos that do not use a Vector store are left
 * untouched. The heavy Vector store starter — and therefore the DataSource it needs —
 * stays in the demos that use it, never here.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.ai.vectorstore.VectorStore")
public class VectorStoreConfiguration {

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
