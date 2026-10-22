package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires two {@link ChatClient}s for the {@code ai-agents} Demo module:
 *
 * <ul>
 *   <li>the default client, unadorned — used by the MCP client flow, which adds
 *   MCP-discovered tools per call</li>
 *   <li>a {@code toolAssistant} client with the local {@link CalculatorTools}
 *   registered — the plain Tool-calling leg</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class AgentConfiguration {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public ChatClient toolAssistant(ChatModel chatModel, CalculatorTools calculatorTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a precise assistant. Use the provided tools for any arithmetic or unit conversion "
                        + "instead of computing in your head, then answer in one short sentence.")
                .defaultTools(calculatorTools)
                .build();
    }
}
