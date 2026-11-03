# Architecture decision records

These records explain enduring boundaries and trade-offs visible in the current implementation. Accepted design does not mean every external-provider assumption is verified; limitations are called out in the affected records and module guides.

| ADR | Decision | Status / scope |
| --- | --- | --- |
| [0001](0001-provider-portability-profiles-ollama-default.md) | Profile-selected primary chat aliases with Ollama default | Accepted; embeddings/media have capability-specific limits |
| [0002](0002-monorepo-shared-ai-common-module.md) | Maven monorepo and shared configuration boundary | Accepted; eight modules total |
| [0003](0003-decision-model-integration.md) | Separate typed-decision client | Accepted boundary; upstream contract not validated |
| [0004](0004-eval-harness-in-ai-common.md) | Pure deterministic Eval harness in ai-common | Accepted; generation/history remain in ai-eval |
| [0005](0005-all-local-observability-in-ai-observability-only.md) | Local DuckDB observation sink in one demo | Accepted; correlation/timing shortcuts remain gaps |
| [0006](0006-hybrid-rag-retrieval.md) | PGVector + PostgreSQL full-text, RRF, lexical rerank | Accepted; explicit schema coupling |
| [0007](0007-structured-output-validation.md) | Separate typed conversion from domain validation | Accepted; invalid records returned with violations |

ADRs 0001–0002 originated with the foundation spec; 0003 followed issue #13; 0004–0005 capture the Eval/observability work; 0006–0007 record already implemented choices during the issue #28 audit. They document actual trade-offs rather than a new feature roadmap. MCP optionality and media bean resolution are explained in their module guides and ADR-0001/0002 rather than creating a record for every method.

For a boundary change, consult the relevant record, preserve why it was chosen, and record whether a later decision supersedes it. Add sequentially numbered ADRs for substantive trade-offs; put API/config details in module guides and gotchas in [learnings](../learnings/implementation-learnings.md).
