package dev.prasadgaikwad.springaiapps.decision;

import java.time.Duration;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
        else {
            provider = provider.trim().toLowerCase(Locale.ROOT);
        }
        if (!"jev".equals(provider)) {
            throw new IllegalArgumentException("Unsupported ai.decision.provider: " + provider);
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
        else if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("ai.decision.timeout must be positive");
        }
    }
}
