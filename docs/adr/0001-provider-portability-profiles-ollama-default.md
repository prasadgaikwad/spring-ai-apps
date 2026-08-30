# Provider portability via `spring.profiles.active` with Ollama default

Demos are provider-agnostic: each targets one AI provider at runtime via `spring.profiles.active`, documented across local Ollama (the zero-key default), OpenAI, and Anthropic. Ollama is the default so the whole monorepo runs without API keys, with cloud providers as documented swaps.

The non-obvious consequence: **embeddings are provider-coupled**, so the embedding model must match the active provider profile and the Vector store the demo writes to. Switching a RAG demo's profile to a different provider also means re-ingesting with that provider's embedding model, since provider A's embeddings are not compatible with provider B's retrieval. This coupling is why provider choice is an explicit, documented decision in every demo rather than an invisible implementation detail.
