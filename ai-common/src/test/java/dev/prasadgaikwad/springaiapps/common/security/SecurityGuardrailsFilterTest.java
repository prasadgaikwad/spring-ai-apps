package dev.prasadgaikwad.springaiapps.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit tests for the opt-in security guardrails (no application context).
 */
class SecurityGuardrailsFilterTest {

    @Test
    void postWithoutCredentialsIsUnauthorized() throws Exception {
        MockHttpServletRequest request = post("/api/rag/query");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(60).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"spring-ai-apps\"");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void apiKeyAllowsPost() throws Exception {
        MockHttpServletRequest request = post("/api/rag/query");
        request.addHeader("X-API-Key", "local-demo-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(60).doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void basicAuthAllowsPost() throws Exception {
        String basic = Base64.getEncoder().encodeToString(
                "demo:local-demo-password".getBytes(StandardCharsets.UTF_8));
        MockHttpServletRequest request = post("/api/rag/query");
        request.addHeader("Authorization", "Basic " + basic);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(60).doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void unknownOriginIsDenied() throws Exception {
        MockHttpServletRequest request = post("/api/rag/query");
        request.addHeader("Origin", "https://evil.example");
        request.addHeader("X-API-Key", "local-demo-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(60).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CORS_ORIGIN_DENIED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void configuredOriginIsAllowed() throws Exception {
        MockHttpServletRequest request = post("/api/rag/query");
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("X-API-Key", "local-demo-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(60, "http://localhost:3000").doFilter(request, response, chain);

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void postIsRateLimitedAfterConfiguredLimit() throws Exception {
        SecurityGuardrailsFilter filter = filter(2);

        assertThat(postWithKey(filter).getStatus()).isEqualTo(200);
        assertThat(postWithKey(filter).getStatus()).isEqualTo(200);
        MockHttpServletResponse third = postWithKey(filter);

        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getContentAsString()).contains("RATE_LIMITED");
    }

    private static MockHttpServletResponse postWithKey(SecurityGuardrailsFilter filter) throws Exception {
        MockHttpServletRequest request = post("/api/rag/query");
        request.addHeader("X-API-Key", "local-demo-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static SecurityGuardrailsFilter filter(int limit, String... origins) {
        return new SecurityGuardrailsFilter("local-demo-key", "demo", "local-demo-password",
                Set.of(origins), limit);
    }

    private static MockHttpServletRequest post(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        return request;
    }
}
