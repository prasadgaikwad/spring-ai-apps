package dev.prasadgaikwad.springaiapps.agents;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * Orchestrates the two agent flows:
 *
 * <ul>
 *   <li><b>tool calling</b> — ask the local {@code toolAssistant} client, letting the
 *   model invoke {@link CalculatorTools} as it sees fit</li>
 *   <li><b>MCP</b> — discover tools from the connected MCP server(s) and hand them to
 *   the model for a single call</li>
 * </ul>
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ChatClient toolAssistant;
    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    public AgentService(ChatClient chatClient, ChatClient toolAssistant,
                        ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        this.chatClient = chatClient;
        this.toolAssistant = toolAssistant;
        this.toolCallbackProviders = toolCallbackProviders;
    }

    /** Answers a question with local tools available to the model. */
    public String askWithTools(String message) {
        require(message);
        return content(toolAssistant.prompt().user(message).call().chatResponse());
    }

    /** Answers a question with MCP-discovered tools available to the model. */
    public String askWithMcpTools(String message) {
        require(message);
        ChatResponse response = chatClient.prompt()
                .user(message)
                .toolCallbacks(mcpToolCallbacks())
                .call()
                .chatResponse();
        return content(response);
    }

    /** Names of the tools discovered from connected MCP servers. */
    public List<String> mcpToolNames() {
        return java.util.Arrays.stream(mcpToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toList();
    }

    private ToolCallback[] mcpToolCallbacks() {
        ToolCallbackProvider provider = toolCallbackProviders.getIfAvailable();
        if (provider == null) {
            return new ToolCallback[0];
        }
        return provider.getToolCallbacks();
    }

    private void require(String message) {
        if (!StringUtils.hasText(message)) {
            throw new BadRequestException("'message' must not be blank");
        }
    }

    private String content(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BadRequestException("The model returned no answer");
        }
        return response.getResult().getOutput().getText();
    }
}
