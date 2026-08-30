package dev.prasadgaikwad.springaiapps.common;

/**
 * Signals that a requested resource does not exist. Translated by
 * {@link ApiExceptionHandler} into a 404 {@link CommonError}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
