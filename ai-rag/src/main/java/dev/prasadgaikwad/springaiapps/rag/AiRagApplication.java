package dev.prasadgaikwad.springaiapps.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-rag} Demo module.
 *
 * <p>Scans {@code dev.prasadgaikwad.springaiapps.common} so the shared
 * REST error handling from {@code ai-common} is picked up.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.rag"
})
public class AiRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRagApplication.class, args);
    }
}
