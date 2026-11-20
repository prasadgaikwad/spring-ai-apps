# MCP `MODERN` protocol spike (issue #45)

**Status:** spike complete; recommendation is to defer adoption and keep the current MCP defaults unchanged.

**Date:** 2026-11-16

**Evidence versions:** Spring Boot `4.2.0-RC2`, Spring AI `2.1.0-RC2`, MCP Java SDK `2.2.0-M2`, Java 25.

**Committed baseline at the time:** `main` was on Spring Boot `4.1.2` / Spring AI `2.0.2`. This note records a detached-worktree spike; it did not change the parent build, the module dependencies, or the MCP defaults. #36 later moved the build to GA `4.2.0` / `2.1.0` and left the MCP defaults unchanged.

Tracking: [#45](https://github.com/prasadgaikwad/spring-ai-apps/issues/45), parent [#36](https://github.com/prasadgaikwad/spring-ai-apps/issues/36).

## Question

Spring AI 2.1 can expose MCP tools through the `2026-07-28` "modern" protocol (`spring.ai.mcp.server.protocol=MODERN`). Does that let `ai-agents` replace its current SSE/STREAMABLE client-server path, and what would adoption require?

## What Spring AI 2.1 provides

The modern server is a different protocol generation, not another transport option for the existing session-based server:

| Area | Modern behavior |
| --- | --- |
| Handshake | Stateless. No `initialize` handshake and no session. Each request declares the protocol version and client capabilities in `_meta`; the client can call `server/discover` to learn what the server supports. |
| HTTP | POST-only endpoint, default `/mcp`. A response is a JSON document, or a short-lived `text/event-stream` when the handler sends notifications before its result. |
| Server primitives | Annotated `@McpTool`, `@McpResource`, `@McpPrompt`, and `@McpComplete` beans, plus `ToolCallback`/`ToolCallbackProvider` beans, are served through the modern server. |
| Progress/streaming | A handler that declares the SDK `McpSyncNotifier` is served as a streaming response. Progress notifications are emitted only when the request carried a progress token. |
| Multi-round-trip requests | Handlers return `InputRequiredResult`; the client retries with `inputResponses` and the server's sealed `requestState`. `requiredCapabilities` gates methods that need a client capability. |
| Subscriptions | `subscriptions/listen` is enabled by default; per-resource subscriptions are off by default. |
| Caching | `server/discover`, list operations, and resource reads can carry TTL/cache-scope hints. |
| Async API | Not supported. Only `spring.ai.mcp.server.type=SYNC` is valid with `protocol=MODERN`; `ASYNC` fails startup. |
| Client | **No modern MCP client was present in Spring AI `2.1.0-RC2` or MCP SDK `2.2.0-M2`.** The SDK contains `io.modelcontextprotocol.modern.server` but no `io.modelcontextprotocol.modern.client` package. |

The modern server requires an MCP Java SDK `2.2.x` line; the RC2 build resolves `2.2.0-M2`, a milestone dependency.

## Required configuration for a modern server

The minimum server configuration used by the spike:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        protocol: MODERN
        type: SYNC
        modern:
          mcp-endpoint: /mcp
```

The module's normal configuration still keeps the server and client disabled, and leaves the configured protocol alone:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: false
        protocol: STREAMABLE
      client:
        enabled: false
```

## Spike setup

A detached worktree was created from `main` (`059e267`) so the committed branch remained unchanged. In that worktree only, the parent was temporarily changed to Boot `4.2.0-RC2` and `spring-ai.version` to `2.1.0-RC2`.

```bash
mvn -pl ai-agents -am -DskipTests package
java -jar ai-agents/target/ai-agents-0.1.0-SNAPSHOT.jar \
  --server.port=18080 \
  --spring.ai.mcp.server.enabled=true \
  --spring.ai.mcp.server.protocol=MODERN \
  --spring.ai.mcp.client.enabled=false
```

The application started with the modern server on `POST /mcp`. A raw HTTP client was used because no modern SDK client exists. Requests carried the MCP method headers expected by the transport and the modern `_meta` block:

```json
{
  "jsonrpc": "2.0",
  "id": "discover-1",
  "method": "server/discover",
  "params": {
    "_meta": {
      "io.modelcontextprotocol/protocolVersion": "2026-07-28",
      "io.modelcontextprotocol/clientInfo": {"name": "curl-spike", "version": "1.0.0"},
      "io.modelcontextprotocol/clientCapabilities": {}
    }
  }
}
```

```bash
curl -sS -X POST http://localhost:18080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2026-07-28' \
  -H 'Mcp-Method: server/discover' \
  --data-binary @discover.json
```

`tools/list` used `Mcp-Method: tools/list`. `tools/call` also required the tool name in `Mcp-Name`.

## Evidence

| Request | Observed result |
| --- | --- |
| `server/discover` | HTTP 200. `supportedVersions` was `["2026-07-28"]`; capabilities included completions, prompts, resources, and tools; `_meta.io.modelcontextprotocol/serverInfo` was `spring-ai-agents-mcp` `0.1.0`. |
| `tools/list` | HTTP 200. The list contained `currentVersion` and `lookupGlossary` with their input schemas and annotations. |
| `tools/call` `lookupGlossary` with `{"term":"RAG"}` | HTTP 200. Returned text: `RAG (Retrieval Augmented Generation): grounding a model's answer in retrieved context.` |
| `tools/call` `currentVersion` | HTTP 200. Returned text: `0.1.0-SNAPSHOT`. |
| `server/discover` without `Mcp-Method` | HTTP 400, JSON-RPC error `-32020`, `Missing required header: Mcp-Method`. |
| `tools/call` without `Mcp-Name` | HTTP 400, JSON-RPC error `-32020`, `Missing required header: Mcp-Name`. |

The successful discovery response was:

```json
{
  "jsonrpc": "2.0",
  "id": "discover-1",
  "result": {
    "supportedVersions": ["2026-07-28"],
    "capabilities": {
      "completions": {},
      "prompts": {"listChanged": true},
      "resources": {"subscribe": false, "listChanged": true},
      "tools": {"listChanged": true}
    },
    "ttlMs": 0,
    "cacheScope": "private",
    "resultType": "complete",
    "_meta": {
      "io.modelcontextprotocol/serverInfo": {"name": "spring-ai-agents-mcp", "version": "0.1.0"}
    }
  }
}
```

This proves the modern **server** can discover and invoke the existing `KnowledgeMcpTools` beans over HTTP. It does not prove a modern **client** can do the same, because no modern client is shipped in the tested line.

## What was not verified

- A Spring AI/MCP SDK modern client handshake. The current `McpClientProtocolIntegrationTests` uses the legacy `HttpClientSseClientTransport` and the session-based SSE server; it is not a modern-client test.
- Interoperability with an external modern MCP peer or an external modern client.
- Async server mode. The documentation states it is unsupported; the spike used the default synchronous mode.
- Multi-round-trip requests, `requiredCapabilities` enforcement, progress streaming through `McpSyncNotifier`, subscriptions, and cache hints. These are documented server features but were not exercised in this spike.
- Spring AI 2.1 GA or MCP SDK 2.2.x GA. Confidentiality, performance, and long-running keep-alive behavior on the milestone SDK remain unverified.

## Deterministic test strategy (for a future PR)

Do not make CI depend on an external MCP peer. The modern server can be tested in-process with raw HTTP, just as the spike did:

1. Add a separate `@SpringBootTest(webEnvironment = RANDOM_PORT)` class, for example `McpModernServerProtocolIntegrationTests`, with properties:
   - `spring.ai.mcp.server.enabled=true`
   - `spring.ai.mcp.server.protocol=MODERN`
   - `spring.ai.mcp.server.type=SYNC`
   - `spring.ai.mcp.client.enabled=false`
2. Mock the three named chat models as the existing protocol test does; the server-only test does not need Ollama or a live model.
3. Use a `RestClient` (or `RestTestClient`) to POST JSON-RPC requests to `/mcp` with `Mcp-Method`, `MCP-Protocol-Version`, and, for `tools/call`, `Mcp-Name`.
4. Assert:
   - `server/discover` returns `2026-07-28` and the expected capabilities.
   - `tools/list` contains `lookupGlossary` and `currentVersion`.
   - `tools/call` returns the glossary text and the current version.
   - A request without `Mcp-Method` or `Mcp-Name` returns HTTP 400 with error code `-32020`.
5. Keep `McpClientProtocolIntegrationTests` on the legacy SSE path as the compatibility baseline. The two tests use different context properties and should remain separate.
6. Add the modern test only when the dependency line supports it (Spring AI 2.1 GA or later); the current `main` code cannot compile a `MODERN` configuration.

## Recommendation

**Defer adoption.** Keep `spring.ai.mcp.server.enabled=false`, `spring.ai.mcp.client.enabled=false`, and the configured `STREAMABLE` default.

Reasons:

- The server side works, but the demo's client seam cannot complete a modern exchange because Spring AI `2.1.0-RC2`/MCP SDK `2.2.0-M2` has no modern client. Switching the default server to `MODERN` would break the current client round-trip and the existing protocol test.
- The service depends on an MCP SDK milestone (`2.2.0-M2`) and a Spring AI release candidate. Neither is a stable basis for changing defaults.
- The module's purpose includes a client/server handshake. A modern server-only demonstration would reduce the verified surface.
- The advanced features that justify the protocol (MRTR, progress streaming, subscriptions, cache hints) were not exercised.

Adoption checklist for a later, explicit change:

- [ ] Spring AI and MCP SDK versions are GA and dependency-convergence checks pass.
- [ ] A modern client is available, or the demo intentionally becomes server-only.
- [ ] The modern server test above is green and the legacy SSE test remains green.
- [ ] MRTR, notifier progress, subscriptions, and required-capability behavior have focused tests.
- [ ] `ai-agents/README.md`, the upgrade plan, and the architecture map describe the new default and its limits.
- [ ] A separate implementation issue is approved; `#45` is not used to silently change the default.

## Compatibility baseline on `main`

`mvn -pl ai-agents -am test` was run on the committed baseline before this note was added. All 19 tests passed, including the two `McpClientProtocolIntegrationTests` that perform a real SSE client/server handshake and invoke `lookupGlossary`/`currentVersion`. The `/api/agents/mcp/*` REST contracts were unchanged.

## References

- [Spring AI 2.1 upgrade plan](../upgrades/spring-boot-4.2-spring-ai-2.1.md)
- [MCP 2026-07-28 discovery](https://modelcontextprotocol.io/specification/2026-07-28/server/discover)
- [Spring AI MCP modern server docs](https://docs.spring.io/spring-ai/reference/2.1/api/mcp/mcp-modern-server-boot-starter-docs.html)
- [ai-agents guide](../../ai-agents/README.md)
- [McpClientProtocolIntegrationTests](../../ai-agents/src/test/java/dev/prasadgaikwad/springaiapps/agents/McpClientProtocolIntegrationTests.java)
