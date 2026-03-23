package com.shopsphere.orderservice.config;

import com.shopsphere.common.dto.ErrorResponse;
import com.shopsphere.common.exception.*;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── CUSTOM ADDITION: HANDLING EXTERNAL SERVICE FAILURES ──────────
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        
        // Log as Warning (it's a dependency issue, not a bug in the Order Service)
        log.warn("Dependency Service Unavailable at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Unavailable",
                        ex.getMessage(), 
                        request.getRequestURI()));
    }

    // ─── BACKUP: HANDLE FEIGN CLIENT EXCEPTIONS ──────────────────────
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex, HttpServletRequest request) {
        
        log.error("Feign Client Error at {}: {}", request.getRequestURI(), ex.getMessage());

        // Determine appropriate status code from Feign exception
        int status = ex.status();
        HttpStatus httpStatus = HttpStatus.resolve(status);
        
        // Default to 503 Service Unavailable if status is unknown or indicates service issue
        if (httpStatus == null || status >= 500) {
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return ResponseEntity.status(httpStatus)
                .body(new ErrorResponse(
                        httpStatus.value(),
                        "External Service Error",
                        "A dependent service is temporarily unavailable. Please try again later.",
                        request.getRequestURI()));
    }

    // ─── EXISTING HANDLERS ──────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                        "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(),
                        "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Unprocessable Entity", ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed", message, request.getRequestURI()));
    }

    // ─── CATCH-ALL FOR UNEXPECTED BUGS ──────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()));
    }
}