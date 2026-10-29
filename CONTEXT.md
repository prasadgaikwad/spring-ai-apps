# Spring AI Apps

A multi-module Maven monorepo of production-grade Spring AI reference applications. It pins Spring Boot 4.1 + Spring AI 2.0 and showcases hard, common AI-engineering use cases for future reference. Targets Java 25, uses Ollama as the zero-key default provider with documented OpenAI/Anthropic swap, and a shared Postgres/PGVector compose.

## Language

### Project structure

**Monorepo**:
A single Maven build hosting one shared module and six runnable demo modules, sharing pinned dependency versions.
_Avoid_: repo, playground

**ai-common**:
The shared module holding the *shared code* the Demo modules depend on: Vector store config, the Eval harness, prompt templates, and common REST/error handling. It deliberately does not drag the heavy PGVector starter onto every Demo module — a Demo brings that dependency itself when it actually uses a Vector store.
_Avoid_: core, lib, scaffolding

### Demos

**Demo module**:
A runnable Spring Boot application, one per showcased use case. Six exist: `ai-rag`, `ai-agents`, `ai-eval`, `ai-observability`, `ai-multimodal`, `ai-structured`.
_Avoid_: app, sample app

**ai-rag**:
The retrieval-augmented-generation demo with a full ETL ingestion pipeline and a query path (retrieval → rerank → generate) over a vector store.

**ai-agents**:
The agentic demo showcasing both Model Context Protocol (MCP) server/client integration and plain tool calling with orchestration.

**ai-eval**:
The evaluation demo: a runnable in-app harness that grades Q&A pairs against criteria and reports regression-style.

**ai-observability**:
The observability demo consuming Spring AI's auto-configured tracing with all-local DuckDB trace storage. Observability config and the DuckDB sink live in this module only; other demos rely on Spring Boot's auto-configured tracing.

**ai-multimodal**:
The multimodal demo covering all four modalities: image generation, vision, audio TTS, and audio transcription.

**ai-structured**:
The structured-output demo: typed extraction to POJOs plus validation.

### Cross-cutting

**Provider choice**:
Which AI model provider a demo targets, selected at runtime via `spring.profiles.active` and documented across OpenAI, Anthropic, and local Ollama.
_Avoid_: model choice, LLM choice

**Vector store**:
The store embeddings are written to and retrieved from (PGVector via Postgres on Docker). Embeddings are provider-coupled, so the embedding model must match the provider profile.
_Avoid_: database

**Hybrid search**:
Retrieval that combines semantic (embedding) search with keyword (full-text) search over the same Vector store, fused by rank rather than raw scores (Reciprocal Rank Fusion), so neither signal alone can starve the other.
_Avoid_: mixed search

**Rerank**:
The post-retrieval stage that reorders retrieved chunks by how well each matches the question, before generation. `ai-rag` uses a deterministic lexical re-rank (term coverage), not a learned cross-encoder.
_Avoid_: reordering

**Grounded answer**:
A generated answer constrained to the retrieved context: if the context does not contain the answer, the model must say it does not know rather than improvise.
_Avoid_: hallucination-free answer

**Tool**:
A client-side function a model may invoke. The agents demo distinguishes plain `Tool`s from MCP-based tools.

**Decision model**:
A model (e.g. System One / Jev by TypeSafe AI) that evaluates state and returns typed decisions (Choice, Score, Noul) with calibrated probabilities, rather than generating free-form text.

**Choice**:
A decision primitive selecting one option from provided choices with a calibrated probability.

**Score**:
A decision primitive returning a numeric score with calibrated probability/uncertainty.

**Noul**:
A decision primitive indicating no valid choice applies (null/none) with a calibrated probability.
_Avoid_: function, plugin

**Eval harness**:
The in-app mechanism that runs evaluation over a set of Q&A pairs and produces a regression-style report.
_Avoid_: evaluator, grader

**Criterion**:
One named expectation a Q&A pair is graded against — required text, forbidden text (the hallucination guard), a pattern, or a custom rule — stated in words so report entries read standalone.
