package com.medisphere.medispherebackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Service to load and manage ML-ready demo patients for Milestone 2 Risk Prediction.
 * These are synthetic patients used for demonstration and testing of ML models.
 * Completely separate from FHIR patients - no data mixing.
 */
@Service
public class MLDemoPatientService {

    private static final Logger log = LoggerFactory.getLogger(MLDemoPatientService.class);
    private static final String ML_DEMO_PATIENTS_FILE = "ml-demo-patients.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile List<Map<String, Object>> cachedPatients;

    /**
     * Get all ML demo patients.
     * These patients are synthetic and designed for ML model testing.
     */
    public List<Map<String, Object>> getMLDemoPatients() {
        if (cachedPatients != null) {
            return cachedPatients;
        }

        try (InputStream inputStream = new ClassPathResource(ML_DEMO_PATIENTS_FILE).getInputStream()) {
            List<Map<String, Object>> patients = objectMapper.readValue(inputStream, new TypeReference<>() {});
            cachedPatients = patients;
            log.info("Loaded {} ML demo patients from {}", patients.size(), ML_DEMO_PATIENTS_FILE);
            return patients;
        } catch (Exception e) {
            log.error("Failed to load ML demo patients from {}: {}", ML_DEMO_PATIENTS_FILE, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get a specific ML demo patient by ID.
     */
    public Map<String, Object> getMLDemoPatient(String patientId) {
        return getMLDemoPatients().stream()
                .filter(p -> patientId.equals(p.get("patientId")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if a patient is an ML demo patient.
     */
    public boolean isMLDemoPatient(String patientId) {
        return getMLDemoPatient(patientId) != null;
    }
}
