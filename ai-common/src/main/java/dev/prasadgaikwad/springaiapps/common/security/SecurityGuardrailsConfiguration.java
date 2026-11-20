package dev.prasadgaikwad.springaiapps.common.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Wires the opt-in guardrails only when the {@code security} profile is active.
 */
@Configuration
@Profile("security")
public class SecurityGuardrailsConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    FilterRegistrationBean<SecurityGuardrailsFilter> securityGuardrailsFilter(
            @Value("${app.security.api-key}") String apiKey,
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password,
            @Value("${app.security.allowed-origins:}") String allowedOrigins,
            @Value("${app.security.rate-limit-per-minute:60}") int rateLimitPerMinute) {
        Set<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toSet());
        SecurityGuardrailsFilter filter =
                new SecurityGuardrailsFilter(apiKey, username, password, origins, rateLimitPerMinute);
        FilterRegistrationBean<SecurityGuardrailsFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("securityGuardrailsFilter");
        registration.addUrlPatterns("/*");
        return registration;
    }
}
