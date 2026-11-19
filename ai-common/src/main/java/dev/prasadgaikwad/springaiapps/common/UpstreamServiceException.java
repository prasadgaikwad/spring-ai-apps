package dev.prasadgaikwad.springaiapps.common;

/**
 * Signals that a model or other remote dependency failed. Translated by
 * {@link ApiExceptionHandler} into a 502 {@link CommonError} with a safe message.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
