# ADR 0001: Provider portability through profiles, with Ollama default

## Status

Accepted. Clarified against the implemented wiring in issue #28.

## Context and decision

The six Spring AI Demo modules share a chat contract but should be usable with local or cloud models. Choose one chat provider through `spring.profiles.active` (`ollama`, `openai`, `anthropic`), defaulting to local Ollama rather than requiring cloud credentials. Shared `ChatModelConfiguration` exposes a profile-specific `@Primary` alias to the provider's existing bean. This keeps demo services dependent on `ChatModel`, rather than embedding provider selection in every service.

This chooses profile-driven aliases over duplicating provider-specific service implementations. The alias resolves injection ambiguity; it does **not** disable the other starters or enforce a complete capability selection. Non-empty placeholder cloud keys in base YAML allow unused beans to construct; a real cloud operation still requires real credentials. Empty environment values can defeat the placeholders.

## Embedding compatibility and limits

The Vector store must use compatible embeddings at ingestion and query time. Shared aliases exist for Ollama and OpenAI when the Vector store API is present. There is no Anthropic embedding alias, so an Anthropic chat profile is not a turnkey RAG provider switch. RAG defaults to 768 dimensions; changing embedding model/provider can also require a fresh/rebuilt schema and re-ingestion, not merely a profile change. Preserve compatibility of existing vectors before using a different query model.

Media interfaces are resolved separately: selecting the primary ChatModel does not select or disable image, speech, and transcription models. `ai-decision` uses its own `ai.decision.*` adapter settings; it is outside this chat-profile contract.

## Consequences and references

Services stay chat-provider-agnostic, while startup bean behavior and capability-specific gaps remain explicit. Multiple active provider profiles can create competing primaries; use one chat profile at a time. This records implemented wiring, not evidence of equal quality or verified live-provider behavior across all profiles.

- [Shared configuration](../../ai-common/README.md)
- [RAG compatibility](../../ai-rag/README.md)
- [Media capability selection](../../ai-multimodal/README.md)
- [Original scope](../0001-spring-ai-apps-core-and-ai-rag-spec.md), [ADR-0003](0003-decision-model-integration.md)
