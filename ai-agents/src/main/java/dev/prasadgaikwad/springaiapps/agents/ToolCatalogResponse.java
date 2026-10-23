package dev.prasadgaikwad.springaiapps.agents;

/**
 * The set of tools currently available to the agent for a given flow.
 */
public record ToolCatalogResponse(String source, java.util.List<String> tools) {

    public ToolCatalogResponse {
        tools = java.util.List.copyOf(tools);
    }
}
