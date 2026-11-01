package dev.prasadgaikwad.springaiapps.decision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "dev.prasadgaikwad.springaiapps.common",
        "dev.prasadgaikwad.springaiapps.decision"
})
public class AiDecisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDecisionApplication.class, args);
    }
}
