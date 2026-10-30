# ai-agents

Agentic demo showcasing tool calling and MCP integration with a chat UI.

## Features

- **Plain Tool calling** — `@Tool`/`@ToolParam` annotated beans (`CalculatorTools`) with a tool-augmented ChatClient.
- **MCP server** — Exposes tools via Spring AI MCP server starter (`@McpTool`).
- **MCP client** — Discovers and uses tools from connected MCP servers.
- **Thymeleaf UI** — Chat interface at `/chat`.

## APIs

- `POST /api/agents/ask` — Ask with local tools
- `POST /api/agents/mcp/ask` — Ask with MCP-discovered tools
- `GET /api/agents/tools` — List local tools
- `GET /api/agents/mcp/tools` — List MCP-discovered tools
- OpenAPI docs at `/v3/api-docs`

## Run

```bash
mvn -pl ai-agents -am spring-boot:run
```
