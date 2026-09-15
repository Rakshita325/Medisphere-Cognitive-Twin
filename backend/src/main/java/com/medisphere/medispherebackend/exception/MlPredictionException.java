package com.medisphere.medispherebackend.exception;

import org.springframework.http.HttpStatus;

public class MlPredictionException extends RuntimeException {

    private final String error;
    private final HttpStatus status;

    public MlPredictionException(String error, String message, HttpStatus status) {
        super(message);
        this.error = error;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
