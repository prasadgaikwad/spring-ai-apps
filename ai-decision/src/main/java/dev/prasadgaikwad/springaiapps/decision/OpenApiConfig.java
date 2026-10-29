package dev.prasadgaikwad.springaiapps.decision;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiDecisionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-decision API")
                .description("System One/Jev-style decision model support: Choice, Score, Noul primitives.")
                .version("0.1.0-SNAPSHOT"));
    }
}
