package dev.prasadgaikwad.springaiapps.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TraceProperties.class)
public class ObservabilityConfig {
}

@ConfigurationProperties(prefix = "trace.duckdb")
record TraceProperties(String url) {
}
