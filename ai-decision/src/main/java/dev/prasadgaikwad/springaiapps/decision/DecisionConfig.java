package dev.prasadgaikwad.springaiapps.decision;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(DecisionProperties.class)
public class DecisionConfig {

    private static final Logger log = LoggerFactory.getLogger(DecisionConfig.class);

    @Bean
    public WebClient decisionWebClient(DecisionProperties properties) {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.warn("ai.decision.api-key is blank; Jev requests are sent without an Authorization header");
        }
        Duration timeout = properties.timeout();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
