package dev.prasadgaikwad.springaiapps.structured;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the {@code ai-structured} Demo module.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiStructuredOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-structured API")
                .description("Typed extraction: map free text to validated POJOs with Spring AI structured output.")
                .version("0.1.0-SNAPSHOT"));
    }
}
