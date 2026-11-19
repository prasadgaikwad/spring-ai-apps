package dev.prasadgaikwad.springaiapps.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Shared REST error handling for the whole monorepo.
 *
 * <p>Every Demo module inherits this advice when it depends on {@code ai-common},
 * giving a uniform JSON error body. Explicit domain exceptions keep their
 * intentional messages; framework, upstream, and unexpected failures use fixed
 * messages so internals are not leaked.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonError> handleUnreadableBody(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request body", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonError> handleTypeMismatch(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request parameter", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonError> handleMissingParameter(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing request parameter", request);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<CommonError> handleMissingPathVariable(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing path variable", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<CommonError> handleUnsupportedMediaType(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unsupported request content type", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonError> handleMethodNotSupported(HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<CommonError> handleNoHandler(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler(UpstreamTimeoutException.class)
    public ResponseEntity<CommonError> handleUpstreamTimeout(HttpServletRequest request) {
        return error(HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT", "Upstream service timed out", request);
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<CommonError> handleUpstreamFailure(HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY", "Upstream service failed", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", request);
    }

    private static ResponseEntity<CommonError> error(HttpStatus status, String code, String message,
                                                     HttpServletRequest request) {
        var error = CommonError.of(code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
