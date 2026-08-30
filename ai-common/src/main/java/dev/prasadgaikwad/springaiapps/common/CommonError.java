package dev.prasadgaikwad.springaiapps.common;

import java.time.Instant;

/**
 * Canonical error payload returned by every Demo module's REST contract.
 * Shape is owned here in {@code ai-common} so clients can parse a consistent
 * error body regardless of which demo they call.
 */
public record CommonError(
        Instant timestamp,
        String code,
        String message,
        String path) {

    public static CommonError of(String code, String message, String path) {
        return new CommonError(Instant.now(), code, message, path);
    }
}
