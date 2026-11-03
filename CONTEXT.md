# Spring AI Apps

A multi-module Maven monorepo of Spring AI reference applications showcasing common AI-engineering use cases. This glossary defines the domain vocabulary; current configuration, contracts, and implementation limits live in the architecture and module guides.

## Language

### Project structure

**Monorepo**:
A single Maven build hosting one shared module and seven runnable Demo modules, sharing pinned dependency versions.
_Avoid_: repo, playground

**ai-common**:
The shared module holding common capabilities used by the Demo modules, including provider configuration, the Eval harness, and REST error conventions.
_Avoid_: core, lib, scaffolding

### Demos

**Demo module**:
A runnable Spring Boot application, one per showcased use case. Seven exist: `ai-rag`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`, `ai-decision`.
_Avoid_: app, sample app

**ai-rag**:
The retrieval-augmented-generation demo with a full ETL ingestion pipeline and a query path (retrieval → rerank → generate) over a vector store.

**ai-agents**:
The agentic demo showcasing plain tool calling, MCP server tools, and an optional MCP callback-consumption boundary.

**ai-eval**:
The evaluation demo: a runnable in-app harness that grades Q&A pairs against criteria and reports regression-style.

**ai-observability**:
The observability demo recording local observations and exposing stored span and trace-summary queries.

**ai-multimodal**:
The multimodal demo covering all four modalities: image generation, vision, audio TTS, and audio transcription.

**ai-structured**:
The structured-output demo: typed extraction to POJOs plus validation.

**ai-decision**:
The typed-decision demo exposing state and questions through a decision-client boundary.

### Cross-cutting

**Provider choice**:
Which model provider a Demo module targets for a particular capability. Chat, embeddings, media, and decisions have distinct capability requirements.
_Avoid_: model choice, LLM choice

**Vector store**:
The store embeddings are written to and retrieved from. Stored and query embeddings must use a compatible embedding model.
_Avoid_: database

**Hybrid search**:
Retrieval that combines semantic (embedding) search with keyword (full-text) search over the same Vector store, fused by rank rather than raw scores (Reciprocal Rank Fusion). Both signals contribute to ranking without guaranteeing representation from each in a truncated result list.
_Avoid_: mixed search

**Rerank**:
The post-retrieval stage that reorders retrieved chunks by how well each matches the question, before generation. `ai-rag` uses a deterministic lexical re-rank (term coverage), not a learned cross-encoder.
_Avoid_: reordering

**Grounded answer**:
A generated answer constrained to the retrieved context: if the context does not contain the answer, the model must say it does not know rather than improvise.
_Avoid_: hallucination-free answer

**Tool**:
A callable capability a model may invoke. The agents demo distinguishes plain `Tool`s from MCP-based tools.
_Avoid_: function, plugin

**Decision model**:
A model that evaluates state and questions to return typed decisions rather than free-form prose. Probability values may accompany decisions; their presence alone does not establish calibration.

**Choice**:
A decision primitive selecting one option from provided choices, with an associated probability.

**Score**:
A decision primitive returning a numeric score, with an associated probability.

**Noul**:
The local decision vocabulary for a null/none result with a reason and probability. Its external-provider meaning remains to be verified.

**Eval harness**:
The in-app mechanism that runs evaluation over a set of Q&A pairs and produces a regression-style report.
_Avoid_: evaluator, grader

**Criterion**:
One named expectation a Q&A pair is graded against — required text, forbidden text (the hallucination guard), a pattern, or a custom rule — stated in words so report entries read standalone.

**Grade**:
The pass/fail result of applying one Criterion to one evaluation case, carrying the criterion name and expectation.

**Regression**:
An evaluation case that passed in a baseline report and fails in the current report.

**Observation**:
A named operation with contextual key/value data and an optional error, offered to local observation handlers.
