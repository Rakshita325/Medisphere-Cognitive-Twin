package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.MlPredictionRequest;
import com.medisphere.medispherebackend.dto.MlPredictionResponse;
import com.medisphere.medispherebackend.exception.MlPredictionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class MlPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);

    private final RestClient restClient;

    public MlPredictionService(@Value("${ml.service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(7000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public MlPredictionResponse predictCvd(List<Double> features) {
        return callPrediction("/predict/cvd", features);
    }

    public MlPredictionResponse predictDiabetes(List<Double> features) {
        return callPrediction("/predict/diabetes", features);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getModelMetadata(String modelName) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/models/{modelName}", modelName)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw new MlPredictionException(
                        "ML metadata unavailable",
                        "Flask metadata response was empty",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return body;
        } catch (HttpClientErrorException ex) {
            throw new MlPredictionException(
                    "ML metadata unavailable",
                    "Flask metadata endpoint returned an error: " + ex.getStatusText(),
                    HttpStatus.BAD_GATEWAY
            );
        } catch (ResourceAccessException ex) {
            throw new MlPredictionException(
                    "ML service unavailable",
                    "Unable to connect to the Flask prediction service",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientException ex) {
            throw new MlPredictionException(
                    "ML metadata unavailable",
                    "Unable to read Flask model metadata",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private MlPredictionResponse callPrediction(String path, List<Double> features) {
        try {
            MlPredictionResponse body = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MlPredictionRequest(features))
                    .retrieve()
                    .body(MlPredictionResponse.class);

            if (body == null || body.model() == null || body.prediction() == null) {
                throw new MlPredictionException(
                        "Malformed ML response",
                        "Flask response was missing prediction fields",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return body;
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new MlPredictionException(
                    "ML request invalid",
                    extractFlaskError(ex, "Flask rejected the prediction request"),
                    HttpStatus.BAD_REQUEST
            );
        } catch (HttpClientErrorException ex) {
            throw new MlPredictionException(
                    "ML service error",
                    extractFlaskError(ex, "Flask prediction service returned an error"),
                    ex.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST
            );
        } catch (ResourceAccessException ex) {
            throw new MlPredictionException(
                    "ML service unavailable",
                    "Unable to connect to the Flask prediction service",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientException ex) {
            log.error("Unexpected Flask communication error for {}", path, ex);
            throw new MlPredictionException(
                    "ML service unavailable",
                    "Unable to communicate with the Flask prediction service",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private String extractFlaskError(HttpClientErrorException ex, String fallbackMessage) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return fallbackMessage;
        }

        String cleaned = body.replaceAll("[\\r\\n]", " ").trim();
        return cleaned.length() > 240 ? cleaned.substring(0, 240) + "..." : cleaned;
    }
}
