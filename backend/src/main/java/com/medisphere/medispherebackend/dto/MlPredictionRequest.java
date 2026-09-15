package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MlPredictionRequest(
        @JsonProperty("features") List<Double> features
) {
}
