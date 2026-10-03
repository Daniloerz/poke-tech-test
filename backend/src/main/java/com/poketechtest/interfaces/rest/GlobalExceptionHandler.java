package com.poketechtest.interfaces.rest;

import com.poketechtest.application.exception.ExternalServiceException;
import com.poketechtest.application.exception.PokemonNotFoundException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String EXTERNAL_SERVICE_DETAIL = "The Pokemon service is not available. Try again later.";
    static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred.";
    static final String INVALID_PARAMETERS_DETAIL = "Invalid request parameters.";

    private static final String ERRORS_PROPERTY = "errors";

    public record ValidationError(String field, String message) {
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalService(ExternalServiceException exception) {
        // The adapter already logged the cause with its context.
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, EXTERNAL_SERVICE_DETAIL);
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    public ProblemDetail handlePokemonNotFound(PokemonNotFoundException exception) {
        // The identifier was validated by the controller, so it is safe to echo it.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(exception, badRequest(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String expectedType = exception.getRequiredType() == null ? "value" : exception.getRequiredType().getSimpleName();
        ValidationError error = new ValidationError(exception.getPropertyName(), "must be a valid " + expectedType);
        return handleExceptionInternal(exception, badRequest(List.of(error)), headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail badRequest(List<ValidationError> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_PARAMETERS_DETAIL);
        problemDetail.setProperty(ERRORS_PROPERTY, errors);
        return problemDetail;
    }
}
