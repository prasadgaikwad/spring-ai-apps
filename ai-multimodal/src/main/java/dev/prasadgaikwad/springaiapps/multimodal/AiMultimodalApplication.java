package dev.prasadgaikwad.springaiapps.multimodal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-multimodal} Demo module.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.multimodal"
})
public class AiMultimodalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiMultimodalApplication.class, args);
    }
}
