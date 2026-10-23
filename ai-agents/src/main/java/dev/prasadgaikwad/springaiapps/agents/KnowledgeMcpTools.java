package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Server side of the MCP leg: this bean's annotated methods are auto-published as MCP
 * tools by the MCP server starter when {@code spring.ai.mcp.server.enabled=true}, so
 * any MCP client (including this module's own client, if pointed back at the server)
 * can discover and call them.
 *
 * <p>Deterministic and side-effect-free, so the published tool set is stable to test.
 */
@Component
public class KnowledgeMcpTools {

    @McpTool(name = "lookupGlossary", description = "Look up the definition of a term in the shared Spring AI glossary.")
    public String lookupGlossary(
            @McpToolParam(description = "the term to define, e.g. RAG, tool, vector store") String term) {
        if (term == null || term.isBlank()) {
            return "No term provided.";
        }
        return switch (term.trim().toLowerCase()) {
            case "rag" -> "RAG (Retrieval Augmented Generation): grounding a model's answer in retrieved context.";
            case "tool" -> "Tool: a function the model may call to act or fetch facts.";
            case "vector store" -> "Vector store: a database of embeddings supporting similarity search.";
            case "eval harness" -> "Eval harness: the in-app mechanism grading Q&A pairs and reporting regression-style.";
            default -> "No glossary entry for '" + term.trim() + "'.";
        };
    }

    @McpTool(name = "currentVersion", description = "Return the current Spring AI reference-collection version.")
    public String currentVersion() {
        return "0.1.0-SNAPSHOT";
    }
}
