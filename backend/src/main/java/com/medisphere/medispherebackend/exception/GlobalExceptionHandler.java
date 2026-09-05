package com.medisphere.medispherebackend.exception;

import com.medisphere.medispherebackend.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ErrorResponse handleStatus(ResponseStatusException exception) {
        return new ErrorResponse(exception.getStatusCode().value(), exception.getReason(), Instant.now());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ErrorResponse handleBadRequest(Exception exception) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid request data", Instant.now());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpected(Exception exception) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Request could not be completed", Instant.now());
    }
}
