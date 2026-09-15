package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlApiErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("message") String message
) {
}
