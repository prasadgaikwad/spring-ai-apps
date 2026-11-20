package dev.prasadgaikwad.springaiapps.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ TraceProperties.class, ObservabilityProperties.class })
public class ObservabilityConfig {
}

@ConfigurationProperties(prefix = "trace.duckdb")
record TraceProperties(String url, int retentionDays) {

    TraceProperties {
        if (retentionDays <= 0) {
            retentionDays = 7;
        }
    }
}

@ConfigurationProperties(prefix = "ai.observability")
record ObservabilityProperties(String serviceName) {

    ObservabilityProperties {
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = "ai-observability";
        }
    }
}
