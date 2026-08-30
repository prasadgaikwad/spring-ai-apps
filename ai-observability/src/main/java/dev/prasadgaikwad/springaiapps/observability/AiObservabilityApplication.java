package dev.prasadgaikwad.springaiapps.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-observability} Demo module.
 *
 * <p>Hosts the all-local DuckDB trace sink and query endpoints in later work
 * (issue #5). All observability config lives here, not in {@code ai-common}.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.observability"
})
public class AiObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiObservabilityApplication.class, args);
    }
}
