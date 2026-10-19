package dev.prasadgaikwad.springaiapps.common;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Shared Chat model configuration for the monorepo's Demo modules.
 *
 * <p>Just as a Vector store is paired with exactly one {@link org.springframework.ai.embedding.EmbeddingModel}
 * (see {@link VectorStoreConfiguration}), anything that injects a {@link ChatModel} needs
 * <em>one</em> unambiguous model to bind to — the one belonging to the active provider
 * profile (ADR-0001). Spring AI auto-configures <em>all</em> provider chat models that are
 * on the classpath (Ollama, OpenAI and Anthropic here), so a bare {@code ChatModel}
 * injection fails with "expected single matching bean" without this configuration.
 *
 * <p>The active provider profile therefore marks its own chat model as {@link Primary},
 * keeping the Provider choice a single profile switch rather than per-demo wiring.
 * Demos that never inject a {@link ChatModel} are unaffected.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
public class ChatModelConfiguration {

    @Bean
    @Primary
    @Profile("ollama")
    public ChatModel primaryOllamaChatModel(ChatModel ollamaChatModel) {
        return ollamaChatModel;
    }

    @Bean
    @Primary
    @Profile("openai")
    public ChatModel primaryOpenAiChatModel(ChatModel openAiChatModel) {
        return openAiChatModel;
    }

    @Bean
    @Primary
    @Profile("anthropic")
    public ChatModel primaryAnthropicChatModel(ChatModel anthropicChatModel) {
        return anthropicChatModel;
    }
}
