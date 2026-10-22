package dev.prasadgaikwad.springaiapps.eval;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.prasadgaikwad.springaiapps.common.eval.EvalHarness;

/**
 * Wires the shared {@link EvalHarness} as a plain bean (it is deliberately
 * Spring-free in {@code ai-common}, so the Demo module wires it explicitly).
 */
@Configuration(proxyBeanMethods = false)
public class EvalConfig {

    @Bean
    public EvalHarness evalHarness() {
        return new EvalHarness();
    }
}
