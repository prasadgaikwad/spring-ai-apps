# Monorepo with a shared `ai-common` deep module

The project is a single Maven monorepo with one shared module (`ai-common`) and six runnable Demo modules, all pinning Spring Boot 4.1 + Spring AI 2.0 through the parent. `ai-common` owns the plumbing shared across demos: Vector store config, the Eval harness, prompt templates, common REST, and error handling.

This was chosen over fully standalone sample apps: six demos share too much real plumbing (vector store wiring, eval, prompts, REST conventions) to copy-paste. A shared module keeps the demos thin and the reference code DRY, at the cost of a demo no longer being self-contained to read in isolation — an acceptable trade for a reference library whose goal is cross-demo consistency. Observability config stays isolated in the `ai-observability` demo rather than in `ai-common`.

A deliberate sub-decision: `ai-common` holds the *shared code* for Vector store config, but the heavy `spring-ai-starter-vector-store-pgvector` dependency is deliberately **not** declared in `ai-common`. Adding it there leaks a mandatory DataSource/JDBC driver onto every Demo module and breaks context-load in modules that don't use a Vector store. A Demo that actually needs a Vector store (`ai-rag`, and any future one) declares the PGVector starter itself and reuses ai-common's config code.
