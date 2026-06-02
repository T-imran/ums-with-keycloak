package com.erainfotech.ums.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException exception,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException exception,
                                                           HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegration(IntegrationException exception,
                                                              HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(KeycloakCommunicationException.class)
    public ResponseEntity<ApiErrorResponse> handleIdentityProviderCommunication(KeycloakCommunicationException exception,
                                                                                HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                             HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
                                                                      HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception,
                                                             HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
                                                           HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
