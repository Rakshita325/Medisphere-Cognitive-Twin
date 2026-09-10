package com.medisphere.medispherebackend.exception;

import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.medisphere.medispherebackend.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException exception) {
        log.warn("ResponseStatusException: {} - {}", exception.getStatusCode(), exception.getReason());
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(exception.getStatusCode().value(), exception.getReason(), Instant.now()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid request data: " + exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(BaseServerResponseException.class)
    public ResponseEntity<ErrorResponse> handleFhirServerException(BaseServerResponseException exception) {
        log.error("FHIR server error: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), "FHIR server returned an error: " + exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler({FhirClientConnectionException.class, SocketTimeoutException.class, ConnectException.class})
    public ResponseEntity<ErrorResponse> handleFhirConnectionTimeout(Exception exception) {
        log.error("FHIR server connection/timeout failure: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse(HttpStatus.GATEWAY_TIMEOUT.value(), "FHIR server communication timed out or failed to connect", Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error: ", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Request could not be completed: " + exception.getMessage(), Instant.now()));
    }
}
