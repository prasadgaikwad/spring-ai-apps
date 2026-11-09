package dev.prasadgaikwad.springaiapps.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Proves a real MCP client-to-server tool flow over HTTP (SSE transport) against this
 * module's published MCP tools. The MCP server is enabled for this test; chat models are
 * mocked because provider connectivity is out of scope.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.mcp.server.enabled=true",
                "spring.ai.mcp.server.protocol=SSE",
                "spring.ai.mcp.server.name=spring-ai-agents-mcp",
                "spring.ai.mcp.server.version=0.1.0",
                "spring.ai.mcp.client.enabled=false"
        })
@ActiveProfiles("ollama")
class McpClientProtocolIntegrationTests {

    @LocalServerPort
    int port;

    @MockitoBean(name = "ollamaChatModel")
    ChatModel ollamaChatModel;

    @MockitoBean(name = "openAiChatModel")
    ChatModel openAiChatModel;

    @MockitoBean(name = "anthropicChatModel")
    ChatModel anthropicChatModel;

    private McpSyncClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            try {
                client.closeGracefully();
            }
            catch (Exception ignored) {
                client.close();
            }
        }
    }

    @Test
    void discoversAndCallsPublishedMcpToolsOverProtocol() throws Exception {
        when(ollamaChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());
        when(openAiChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());
        when(anthropicChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());

        client = connectWithRetry("http://localhost:" + port + "/sse");

        ListToolsResult tools = client.listTools();
        assertThat(tools.tools()).extracting("name")
                .contains("lookupGlossary", "currentVersion");

        CallToolResult glossary = client.callTool(new CallToolRequest(
                "lookupGlossary", Map.of("term", "RAG")));
        assertThat(text(glossary)).containsIgnoringCase("Retrieval Augmented Generation");

        CallToolResult version = client.callTool(new CallToolRequest(
                "currentVersion", Map.of()));
        assertThat(text(version)).contains("0.1.0-SNAPSHOT");
    }

    @Test
    void springAiToolCallbackProviderExposesDiscoveredMcpTools() throws Exception {
        when(ollamaChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());
        when(openAiChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());
        when(anthropicChatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().build());

        client = connectWithRetry("http://localhost:" + port + "/sse");

        SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(List.of(client));
        var callbacks = List.of(provider.getToolCallbacks());
        assertThat(callbacks).isNotEmpty();
        assertThat(callbacks).extracting(cb -> cb.getToolDefinition().name())
                .contains("lookupGlossary", "currentVersion");
    }

    private McpSyncClient connectWithRetry(String sseUrl) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(sseUrl).build();
                McpSyncClient c = McpClient.sync(transport)
                        .requestTimeout(Duration.ofSeconds(15))
                        .initializationTimeout(Duration.ofSeconds(15))
                        .build();
                c.initialize();
                return c;
            }
            catch (Exception e) {
                last = e;
                Thread.sleep(250);
            }
        }
        throw last != null ? last : new IllegalStateException("Could not connect to MCP server at " + sseUrl);
    }

    private String text(CallToolResult result) {
        return result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .reduce("", (a, b) -> a + b);
    }

}
