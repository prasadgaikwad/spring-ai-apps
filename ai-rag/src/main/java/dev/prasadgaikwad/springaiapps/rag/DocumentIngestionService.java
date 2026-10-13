package dev.prasadgaikwad.springaiapps.rag;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import dev.prasadgaikwad.springaiapps.common.BadRequestException;

/**
 * The full ETL ingestion pipeline for {@code ai-rag}: read a source into
 * {@link Document}s, split them into chunks, embed them, and write the embeddings to
 * the configured {@link VectorStore}.
 *
 * <p>Chunk ids are deterministic (derived from the source and chunk text), so re-ingesting
 * the same content is idempotent: {@link VectorStore} implementations upsert on id rather
 * than duplicating rows. PGVector's {@code vector_store} table keys rows on a UUID column,
 * so chunk ids are canonical {@link UUID}s rather than arbitrary-length hashes.
 */
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;

    public DocumentIngestionService(VectorStore vectorStore, TextSplitter textSplitter) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    public IngestResponse ingestResource(Resource resource) {
        List<Document> documents;
        try {
            documents = new TextReader(resource).get();
        }
        catch (RuntimeException ex) {
            throw new BadRequestException("Unable to read source '" + resource.getDescription() + "': " + ex.getMessage());
        }
        if (documents.isEmpty()) {
            throw new BadRequestException("Source produced no content: " + resource.getDescription());
        }
        String source = resource.getDescription();
        return ingest(source, documents);
    }

    public IngestResponse ingestText(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("'text' must not be blank");
        }
        ByteArrayResource resource = new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8), "inline-text");
        List<Document> documents;
        try {
            documents = new TextReader(resource).get();
        }
        catch (RuntimeException ex) {
            throw new BadRequestException("Unable to parse inline text: " + ex.getMessage());
        }
        return ingest(resource.getDescription(), documents);
    }

    private IngestResponse ingest(String source, List<Document> documents) {
        List<Document> chunks = textSplitter.split(documents);
        List<Document> idempotent = chunks.stream()
                .map(chunk -> Document.builder()
                        .id(chunkId(source, chunk.getText()))
                        .text(chunk.getText())
                        .metadata(chunk.getMetadata())
                        .build())
                .toList();
        vectorStore.add(idempotent);
        return IngestResponse.ok(source, documents.size(), idempotent.size());
    }

    private static String chunkId(String source, String text) {
        return UUID.nameUUIDFromBytes((source + "::" + text).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
