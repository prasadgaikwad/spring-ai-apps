# ADR 0006: Hybrid RAG retrieval over one PGVector table

## Status

Accepted; records the query architecture delivered by issue #3 / PR #14.

## Context and decision

Semantic search can miss exact domain terms; keyword-only search misses semantic similarity. Query the same stored chunks with `VectorStoreDocumentRetriever` and PostgreSQL English full-text SQL, combine ranks with Reciprocal Rank Fusion (`k=60`), and deduplicate by chunk ID. Ranks avoid calibrating incomparable similarity/keyword scores. Keep the semantic document copy when both legs find the same ID, preserving metadata.

Use Spring AI's `RetrievalAugmentationAdvisor` for retrieval, document post-processing, and default context-constrained prompt augmentation. A deterministic `LexicalReranker` scores term coverage and caps final documents. This keeps orchestration on the modular RAG interfaces without introducing another search service or a learned rerank provider.

## Consequences and alternatives

Keyword SQL deliberately couples this demo to PGVector's `id`/`content` schema and configured table name. A separate search engine would loosen that SQL coupling but require extra infrastructure and a second index synchronization flow. A learned reranker may improve relevance but adds provider/model dependencies and nondeterminism. The lexical implementation is transparent, not evidence of cross-encoder-level relevance.

Deterministic UUIDs from source/chunk text permit ingestion upserts, but do not implement source-version deletion. Prompt grounding and returned sources make evidence inspectable; they do not guarantee factual accuracy. The real PGVector tests use fixed embeddings and model mocks, so live retrieval quality remains outside their claims.

See [RAG pipeline, configuration, and tests](../../ai-rag/README.md) and [embedding compatibility](0001-provider-portability-profiles-ollama-default.md).
