# ai-agents

Two tool-enabled chat paths: local `@Tool` methods and tools discovered through MCP. The module also defines MCP server tools and a minimal `/chat` Thymeleaf page. Package: `dev.prasadgaikwad.springaiapps.agents`.

## Run and configure

```bash
mvn -pl ai-common -am install -DskipTests
mvn -pl ai-agents spring-boot:run
```

Use a running Ollama with a tool-capable model, or select `openai` / `anthropic` with real credentials as described in the [root guide](../README.md). The default model setting is `llama3.2`; model capability and tool behavior must be checked with your chosen provider.

MCP server publication is **disabled by default** (`spring.ai.mcp.server.enabled=false`). Set that property to `true` to enable the server starter. The configured server name is `spring-ai-agents-mcp`, version `0.1.0`. The current POM includes the web MVC MCP **server** starter, but no MCP client starter, and no peer connection is supplied in `application.yml`. The service has a callback-consumption seam rather than an out-of-the-box remote MCP client: a real client flow requires adding/configuring the appropriate client integration or supplying a `ToolCallbackProvider`. Server publication does not create a loopback connection.

## API examples

| Method/path | Behavior |
| --- | --- |
| `POST /api/agents/ask` | `{"message":"..."}` → `{"message":"...","answer":"..."}` using local tools |
| `POST /api/agents/mcp/ask` | Same shape, with discovered MCP callbacks |
| `GET /api/agents/tools` | `source` and local tool names |
| `GET /api/agents/mcp/tools` | `source` and sorted discovered MCP tool names |
| `GET /api/agents/ping` | Module response |
| `GET /chat` | Chat page |

```bash
curl -s http://localhost:8080/api/agents/tools
curl -s http://localhost:8080/api/agents/ask -H 'Content-Type: application/json' \
  -d '{"message":"Convert 10 kilometres to miles using the tool."}'
curl -s http://localhost:8080/api/agents/mcp/tools
```

OpenAPI is at `/v3/api-docs` and `/swagger-ui.html`. Blank messages produce a shared 400 error. An absent model result is rejected. No application-level conversation history is stored between requests.

## Tool ownership

| Bean | Tools | Role |
| --- | --- | --- |
| `CalculatorTools` | `add`, `multiply`, `convertUnit`, `daysBetween` | Local `@Tool` callbacks registered on `toolAssistant` |
| `KnowledgeMcpTools` | `lookupGlossary`, `currentVersion` | `@McpTool` methods published when the MCP server is enabled |

Conversion supports km↔mi and kg↔lb (including aliases); incompatible dimensions are rejected. Dates use ISO `yyyy-MM-dd` and whole signed day differences. The MCP glossary is a small fixed switch of definitions, not a dynamic reader of `CONTEXT.md`.

## Orchestration sequence

```mermaid
sequenceDiagram
    participant U as Caller
    participant A as AgentController / AgentService
    participant C as ChatClient
    participant M as ChatModel
    participant T as Tool callback
    U->>A: POST /api/agents/ask or /mcp/ask
    alt Local Tool path
        A->>C: use toolAssistant with CalculatorTools
    else MCP path
        A->>A: resolve ToolCallbackProvider callbacks
        A->>C: attach callbacks to this prompt
    end
    C->>M: message plus tool definitions
    opt Model requests a tool
        M-->>C: tool call
        C->>T: execute local callback or MCP callback
        T-->>C: tool result
        C->>M: tool result for answer generation
    end
    M-->>C: final answer
    C-->>A: ChatResponse
    A-->>U: message and answer
```

Spring AI handles the tool-call exchange. Tools are available to the model; a request does not prove a tool was invoked. The MCP path resolves an optional `ToolCallbackProvider`; with none it attaches an empty array and can still answer as ordinary chat. Multiple provider beans require unambiguous wiring (`getIfAvailable` expects a unique candidate).

## Tests and learnings

`mvn -pl ai-agents -am test` runs REST/UI context tests with a named mock `ollamaChatModel` plus focused calculator tests. REST tests cover answers, blank-message rejection, catalogs, and MCP behavior without a connected peer. They do not exercise a real MCP transport handshake or model-generated tool calls. The useful seam is the observable answer/catalog; pure tool calculations are tested separately. See [implementation learnings](../docs/learnings/implementation-learnings.md).
