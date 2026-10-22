package dev.prasadgaikwad.springaiapps.multimodal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the {@code ai-multimodal} Demo module.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI aiMultimodalOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ai-multimodal API")
                .description("All four modalities: image generation, vision, speech synthesis, and transcription.")
                .version("0.1.0-SNAPSHOT"));
    }
}
