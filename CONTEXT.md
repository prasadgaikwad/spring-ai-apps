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

**Tool**:
A client-side function a model may invoke. The agents demo distinguishes plain `Tool`s from MCP-based tools.
_Avoid_: function, plugin

**Eval harness**:
The in-app mechanism that runs evaluation over a set of Q&A pairs and produces a regression-style report.
_Avoid_: evaluator, grader
