package dev.prasadgaikwad.springaiapps.decision;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Focused unit tests for the decision client configuration contract. */
class DecisionPropertiesTest {

    @Test
    void appliesDefaults() {
        DecisionProperties properties = new DecisionProperties(null, null, null, null, null);

        assertThat(properties.provider()).isEqualTo("jev");
        assertThat(properties.baseUrl()).isEqualTo("https://api.typesafe.ai");
        assertThat(properties.model()).isEqualTo("jev-latest");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void normalizesSupportedProvider() {
        DecisionProperties properties = new DecisionProperties(" JEV ", null, null, null, null);
        assertThat(properties.provider()).isEqualTo("jev");
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> new DecisionProperties("other", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported ai.decision.provider");
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThatThrownBy(() -> new DecisionProperties(null, null, null, null, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> new DecisionProperties(null, null, null, null, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }
}
