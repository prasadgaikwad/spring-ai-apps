# Spring AI RAG Overview

Spring AI makes it easy to build Retrieval-Augmented Generation (RAG) applications.

## ETL Pipeline
- **Read**: Load documents from sources such as text, markdown, or PDF files.
- **Split**: Break documents into manageable chunks using a `TextSplitter`.
- **Embed**: Convert chunks to vector embeddings via the configured `EmbeddingModel`.
- **Store**: Persist embeddings in the configured `VectorStore` (PGVector in this demo).

## Query Path
- **Retrieve**: Fetch relevant chunks using similarity search.
- **Rerank**: Prioritize the most relevant context.
- **Generate**: Produce an answer grounded in the retrieved context.
