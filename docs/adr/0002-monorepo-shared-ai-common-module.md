# ADR 0002: Maven monorepo with a shared ai-common module

## Status

Accepted. Originally six demos; `ai-decision` subsequently added as the seventh.

## Context and decision

Use one parent Maven reactor to pin Boot 4.1.1, Spring AI 2.0.1, and Java 25 for `ai-common` and the seven independent Demo applications. Shared configuration, error contracts, and the deterministic Eval harness live in `ai-common`. This is preferred to standalone repositories or repeated plumbing in each demo: the reference collection benefits from consistent contracts, at the cost of requiring readers and builds to navigate the shared dependency.

Each demo declares its use-case dependencies. `ai-common` currently includes web infrastructure and the Ollama starter; it does not contain every provider starter. Shared prompt templates remain an original scope item rather than a delivered library. The Java namespace is `dev.prasadgaikwad.springaiapps.*`, while Maven coordinates retain `spring-ai-apps`.

## Dependency locality

Keep `spring-ai-starter-vector-store-pgvector` only in `ai-rag`, which needs the DataSource. Putting it in `ai-common` imposed JDBC auto-configuration on unrelated demos and broke their startup. Shared embedding alias code is conditional on the Vector store API being present.

Keep DuckDB dependencies, configuration, schema, handler, and queries only in `ai-observability`. These demos are separately launched processes; a shared module must not silently install the sink everywhere. Decision-specific DTOs and adapter code remain in `ai-decision` until a genuine shared consumer warrants extraction.

## Consequences and references

- Build/tests can use `-pl <module> -am`; run goals target executable modules after installing shared prerequisites.
- Component scanning must include the shared package; sharing a dependency alone is insufficient.
- Only RAG and agents have Thymeleaf UIs; other demos expose REST/OpenAPI. Ping remains alongside implemented features.
- A shared configuration change has multiple consumers and needs reactor-level verification.

See [architecture](../architecture.md), [ADR-0004](0004-eval-harness-in-ai-common.md), and [ADR-0005](0005-all-local-observability-in-ai-observability-only.md).
