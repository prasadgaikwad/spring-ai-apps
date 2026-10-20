package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the {@code ai-rag} Demo module.
 *
 * <p>springdoc scans the module's controllers and publishes the spec at
 * {@code /v3/api-docs} (Swagger UI at {@code /swagger-ui.html}); this bean only gives
 * the document a title and description.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiRagOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-rag API")
                .description("ETL ingestion and the retrieval-re-rank-generate query path over PGVector.")
                .version("0.1.0-SNAPSHOT"));
    }
}
