package dev.prasadgaikwad.springaiapps.common;

/**
 * Signals a client error in the request. Translated by {@link ApiExceptionHandler}
 * into a 400 {@link CommonError}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
