package dev.prasadgaikwad.springaiapps.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Shared REST error handling for the whole monorepo.
 *
 * <p>Every Demo module inherits this advice when it depends on {@code ai-common},
 * giving a uniform JSON error body. Handlers are deliberately thin: they translate
 * an exception into a {@link CommonError} without leaking internals.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        var error = CommonError.of("NOT_FOUND", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        var error = CommonError.of("BAD_REQUEST", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonError> handleGeneric(Exception ex, HttpServletRequest request) {
        var error = CommonError.of("INTERNAL_ERROR", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
