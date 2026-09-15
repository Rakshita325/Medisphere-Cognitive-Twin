package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlPredictionResponse(
        @JsonProperty("model") String model,
        @JsonProperty("prediction") Integer prediction,
        @JsonProperty("risk_probability") Double riskProbability,
        @JsonProperty("risk_percentage") Double riskPercentage
) {
}
