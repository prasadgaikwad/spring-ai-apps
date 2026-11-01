package dev.prasadgaikwad.springaiapps.decision;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the decision model client (Jev/SystemOne).
 */
@ConfigurationProperties(prefix = "ai.decision")
public record DecisionProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        Duration timeout) {

    public DecisionProperties {
        if (provider == null || provider.isBlank()) {
            provider = "jev";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.typesafe.ai";
        }
        if (model == null || model.isBlank()) {
            model = "jev-latest";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(10);
        }
    }
}
