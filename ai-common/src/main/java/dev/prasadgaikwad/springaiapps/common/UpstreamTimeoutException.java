package dev.prasadgaikwad.springaiapps.common;

/**
 * Signals that a model or other remote dependency timed out. Translated by
 * {@link ApiExceptionHandler} into a 504 {@link CommonError} with a safe message.
 */
public class UpstreamTimeoutException extends UpstreamServiceException {

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
