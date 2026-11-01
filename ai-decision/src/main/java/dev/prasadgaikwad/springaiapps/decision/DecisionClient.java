package dev.prasadgaikwad.springaiapps.decision;

/**
 * Pluggable decision client interface (Jev-specific impl now, extensible later).
 */
public interface DecisionClient {

    DecisionPrimitives.DecisionResponse decide(DecisionPrimitives.DecisionState state,
                                              java.util.List<DecisionPrimitives.DecisionQuestion> questions);
}
