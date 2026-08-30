package dev.prasadgaikwad.springaiapps.eval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the {@code ai-eval} Demo module.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.eval"
})
public class AiEvalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEvalApplication.class, args);
    }
}
