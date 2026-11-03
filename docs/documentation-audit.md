# Documentation audit for issue #28

## Scope and evidence

This review covers all tracked Markdown documents, the eight Maven modules, main Java sources, YAML configuration, tests, Compose, environment samples, and the two Thymeleaf clients. Documentation describes the implementation currently checked in; it does not certify any live model/vendor API. No feature behavior is changed by this audit.

| Document surface | Source cross-check | Result |
| --- | --- | --- |
| Root README | Parent POM, module POMs, YAML, Compose | Correct module count/provider scope, runnable Maven commands, environment loading, infrastructure prerequisites, navigation/context diagram |
| `CONTEXT.md` | Module roles and shared/local DTO vocabulary | Seven demos; definitions separated from wiring; probability calibration claims qualified; misplaced Tool avoidance note fixed |
| `AGENTS.md` | Current module/test/config patterns | Replace scaffold-only status, centralize reading pointers, update workflow/testing caveats |
| Original spec | Parent/module source and completed feature paths | Preserve original user stories; prepend current implementation/status matrix and scope qualifications |
| `ai-common/README.md` | Config/error classes and all Eval types/tests | Correct one-argument factory example, aliases, summary limits, error shape, class diagram |
| `ai-rag/README.md` | Controllers, ingestion, retriever, reranker, config, templates, tests | Actual route names, ETL/query sequences, defaults, ID lifecycle, profile/dimension limits |
| `ai-agents/README.md` | Controller/service/config/tools, MCP config/POM, template/tests | Separate tool/MCP paths, exact routes, publication/connection defaults, transport test limits |
| `ai-eval/README.md` | Controller/service/specs/reports/tests | Payload example, blank-answer generation, positional regressions, memory lifecycle, flow |
| `ai-observability/README.md` | Controller, handler, repository, schema/config/tests | Correct file path/filters/order, schema ownership, name-based IDs, timestamps/status, diagrams |
| `ai-multimodal/README.md` | Controller/service/DTOs/config/tests | Per-capability selection, exact request/response fields, ignored options, base64 and test scope |
| `ai-structured/README.md` | Controller/dispatch/conversion/records/validation/tests | Exact targets/fields, HTTP vs validation success, diagrams, live-provider qualifications |
| `ai-decision/README.md` | Properties/config/controller/DTOs/client/tests | Exact adapter assumptions/parser fallbacks, environment precedence, validation/test gaps, diagrams |
| ADRs 0001–0005 | Existing boundaries and implementation | Correct scope/defaults, distinguish decisions from shortcuts; remove stray EOF artifact; add index |
| New ADRs 0006–0007 | Existing hybrid RAG and structured validation implementations | Record rationale/trade-offs for choices already delivered |
| Foundation learnings | Current build/config/test behavior | Mark historical scope, qualify auto-config claim, correct run and DB test setup |
| Implementation learnings | All implemented flows above | Add module-by-module lessons and verification boundaries |
| `samples/rag-overview.md` | TextReader/classpath ingestion | Retained as an ingestion fixture: its concise generic RAG content is still valid, not an API guide |

## Diagram coverage

- System context and build dependency maps: [architecture](architecture.md) and root README.
- Shared Eval class model: [ai-common](../ai-common/README.md).
- RAG ingestion flow and query sequence: [ai-rag](../ai-rag/README.md).
- Local tools/MCP alternative sequence: [ai-agents](../ai-agents/README.md).
- Evaluation generation/grading/history flow: [ai-eval](../ai-eval/README.md).
- Observation capture/persistence flow and classes: [ai-observability](../ai-observability/README.md).
- Four media-capability routes: [ai-multimodal](../ai-multimodal/README.md).
- Structured conversion/validation sequence and types: [ai-structured](../ai-structured/README.md).
- Decision HTTP sequence and client/answer types: [ai-decision](../ai-decision/README.md).

## Gaps recorded rather than silently promoted to guarantees

Anthropic RAG embedding selection, schema/dimension migrations, obsolete-chunk cleanup, MCP client integration (no client starter/connection supplied), real tool execution, durable/concurrent Eval history, multimodal option wiring, accurate automatic span timing/correlation, decision protocol/mapping tests, and live provider verification remain implementation gaps or operational qualifications. They are documented where they affect a caller. This ticket changes the documentation, not those feature contracts.

## Validation approach

Check local Markdown links and balanced fenced blocks, compare endpoint/DTO/property claims to code, check Mermaid syntax/renderability where tooling is available, review the full diff, and run the reactor tests to establish the current application's baseline. Report infrastructure blockers distinctly from passing tests. Live calls are not part of this documentation audit.

## Completed validation (2026-11-02)

- `mvn test`: **BUILD SUCCESS** across the parent and eight modules; Surefire reports total **87 tests, 0 failures, 0 errors, 0 skipped**, including PGVector Testcontainers tests.
- All **25 Markdown files** scanned: **103 local link targets** resolve and code fences are balanced.
- All **16 Mermaid blocks** parse successfully with Mermaid **12.2.0**. This is syntax validation, not a visual review of GitHub's renderer.
- `git diff --check`: passed.
- Two independent review passes checked documented standards/source accuracy and issue acceptance coverage. Corrected regex case-sensitivity wording, RAG test-double scope, the root calibration claim, the hybrid-search representation claim, and the root context diagram. A final POM cross-check corrected the agents MCP client-starter claim and clarified Actuator versus explicit trace-query routes.

No live AI-provider calls were made. The tests establish the existing application baseline and the documented testing seams, not upstream interoperability.
