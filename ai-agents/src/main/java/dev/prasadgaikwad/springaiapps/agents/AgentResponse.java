package dev.prasadgaikwad.springaiapps.agents;

/**
 * The agent's answer to a single-turn request.
 */
public record AgentResponse(String message, String answer) {
}
