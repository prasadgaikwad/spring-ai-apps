package dev.prasadgaikwad.springaiapps.structured;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-structured} Demo module.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.structured"
})
public class AiStructuredApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStructuredApplication.class, args);
    }
}
