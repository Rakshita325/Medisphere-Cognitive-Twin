package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.MlPredictionRequest;
import com.medisphere.medispherebackend.dto.MlPredictionResponse;
import com.medisphere.medispherebackend.service.MlPredictionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
public class MlPredictionController {

    private final MlPredictionService mlPredictionService;

    public MlPredictionController(MlPredictionService mlPredictionService) {
        this.mlPredictionService = mlPredictionService;
    }

    @PostMapping("/predict/cvd")
    public ResponseEntity<MlPredictionResponse> predictCvd(@Valid @RequestBody MlPredictionRequest request) {
        validateFeatureCount(request.features(), "CVD", 15);
        return ResponseEntity.ok(mlPredictionService.predictCvd(request.features()));
    }

    @PostMapping("/predict/diabetes")
    public ResponseEntity<MlPredictionResponse> predictDiabetes(@Valid @RequestBody MlPredictionRequest request) {
        validateFeatureCount(request.features(), "Diabetes", 8);
        return ResponseEntity.ok(mlPredictionService.predictDiabetes(request.features()));
    }

    @GetMapping("/models/{modelName}")
    public ResponseEntity<Map<String, Object>> getModelMetadata(@PathVariable String modelName) {
        if (!"cvd".equalsIgnoreCase(modelName) && !"diabetes".equalsIgnoreCase(modelName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Unsupported model",
                    "available_models", List.of("cvd", "diabetes")
            ));
        }

        return ResponseEntity.ok(mlPredictionService.getModelMetadata(modelName.toLowerCase()));
    }

    private void validateFeatureCount(List<Double> features, String modelName, int expectedCount) {
        if (features == null) {
            throw new IllegalArgumentException(modelName + " features are required");
        }

        if (features.size() != expectedCount) {
            throw new IllegalArgumentException(
                    modelName + " requires exactly " + expectedCount + " numeric features, but received " + features.size()
            );
        }

        for (int i = 0; i < features.size(); i++) {
            Double value = features.get(i);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Feature at index " + i + " must be a finite numeric value");
            }
        }
    }
}
