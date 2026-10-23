package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the {@code ai-agents} Demo module.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiAgentsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-agents API")
                .description("Plain Tool calling with local tools, MCP server/client integration, and a chat UI.")
                .version("0.1.0-SNAPSHOT"));
    }
}
