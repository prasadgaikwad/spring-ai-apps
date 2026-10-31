package dev.prasadgaikwad.springaiapps.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the {@code ai-observability} Demo module.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiObservabilityOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-observability API")
                .description("All-local DuckDB trace sink consuming Spring AI Micrometer observations with query endpoints.")
                .version("0.1.0-SNAPSHOT"));
    }
}
