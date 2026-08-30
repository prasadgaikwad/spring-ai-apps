package dev.prasadgaikwad.springaiapps.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-agents} Demo module.
 *
 * <p>Scans {@code dev.prasadgaikwad.springaiapps.common} for shared REST error handling.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.agents"
})
public class AiAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentsApplication.class, args);
    }
}
