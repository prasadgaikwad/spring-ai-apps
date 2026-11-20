package dev.prasadgaikwad.springaiapps.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Opt-in guardrails for the {@code security} profile.
 *
 * <p>Requires an API key ({@code X-API-Key}) or HTTP Basic credentials for POST
 * requests under {@code /api/}, enforces a deny-by-default CORS allowlist, and applies
 * a simple fixed-window rate limit to those POST requests. The filter is inert unless
 * the profile is active.
 */
public class SecurityGuardrailsFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final String apiKey;
    private final String username;
    private final String password;
    private final Set<String> allowedOrigins;
    private final int rateLimitPerMinute;
    private final Map<String, ArrayDeque<Long>> windows = new ConcurrentHashMap<>();

    public SecurityGuardrailsFilter(String apiKey, String username, String password,
                                    Set<String> allowedOrigins, int rateLimitPerMinute) {
        this.apiKey = apiKey;
        this.username = username;
        this.password = password;
        this.allowedOrigins = allowedOrigins == null ? Set.of() : Set.copyOf(allowedOrigins);
        this.rateLimitPerMinute = Math.max(1, rateLimitPerMinute);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            if (!allowedOrigins.contains(origin)) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "CORS_ORIGIN_DENIED",
                        "Origin is not allowed");
                return;
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-API-Key,Authorization");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (isProtectedPost(request)) {
            if (!hasCredentials(request)) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"spring-ai-apps\"");
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                        "API key or basic auth required");
                return;
            }
            if (!consumeRateLimit(request)) {
                reject(response, 429, "RATE_LIMITED", "Too many requests");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isProtectedPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/api/");
    }

    private boolean hasCredentials(HttpServletRequest request) {
        String providedKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank() && providedKey != null
                && constantEquals(apiKey, providedKey)) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)),
                    StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                return false;
            }
            return constantEquals(username, decoded.substring(0, separator))
                    && constantEquals(password, decoded.substring(separator + 1));
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean consumeRateLimit(HttpServletRequest request) {
        String suppliedKey = request.getHeader("X-API-Key");
        String bucketKey = suppliedKey != null && !suppliedKey.isBlank()
                ? suppliedKey
                : request.getRemoteAddr();
        long now = System.currentTimeMillis();
        ArrayDeque<Long> window = windows.computeIfAbsent(bucketKey, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= rateLimitPerMinute) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private static boolean constantEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static void reject(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
