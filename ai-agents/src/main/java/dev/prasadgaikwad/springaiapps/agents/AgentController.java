package dev.prasadgaikwad.springaiapps.agents;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST surface for the {@code ai-agents} Demo module.
 *
 * <ul>
 *   <li>{@code POST /api/agents/ask} — plain Tool calling with local tools</li>
 *   <li>{@code POST /api/agents/mcp/ask} — ask with MCP-discovered tools</li>
 *   <li>{@code POST /api/agents/stream} — plain chat streamed as Server-Sent Events</li>
 *   <li>{@code GET /api/agents/tools} — local tool catalog</li>
 *   <li>{@code GET /api/agents/mcp/tools} — MCP-discovered tool catalog</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService service;
    private final CalculatorTools calculatorTools;

    public AgentController(AgentService service, CalculatorTools calculatorTools) {
        this.service = service;
        this.calculatorTools = calculatorTools;
    }

    @PostMapping("/ask")
    @ResponseStatus(HttpStatus.OK)
    public AgentResponse ask(@RequestBody AgentRequest request) {
        return new AgentResponse(request.message(), service.askWithTools(request.message()));
    }

    @PostMapping("/mcp/ask")
    @ResponseStatus(HttpStatus.OK)
    public AgentResponse askWithMcp(@RequestBody AgentRequest request) {
        return new AgentResponse(request.message(), service.askWithMcpTools(request.message()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody AgentRequest request) {
        return service.streamAnswer(request.message())
                .map(token -> ServerSentEvent.builder(token).event("token").build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder().event("done").build()))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("The model stream failed: " + ex.getMessage())
                        .build()));
    }

    @GetMapping("/tools")
    @ResponseStatus(HttpStatus.OK)
    public ToolCatalogResponse tools() {
        return new ToolCatalogResponse("local", calculatorTools.toolNames());
    }

    @GetMapping("/mcp/tools")
    @ResponseStatus(HttpStatus.OK)
    public ToolCatalogResponse mcpTools() {
        return new ToolCatalogResponse("mcp", service.mcpToolNames());
    }

    /** Convenience ping retained for the module smoke test. */
    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.OK)
    public PingResponse ping() {
        return new PingResponse("ai-agents", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
