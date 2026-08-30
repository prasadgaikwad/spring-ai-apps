package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder REST surface for the {@code ai-agents} Demo module.
 *
 * <p>Replaced by the MCP + tool-calling flows in later work (issue #5). Exists so
 * the module has a runnable HTTP contract and a smoke-testable endpoint.
 */
@RestController
@RequestMapping("/api/agents")
public class AgentsScaffoldController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ai-agents", "ok");
    }

    public record PingResponse(String module, String status) {
    }
}
