package com.medisphere.medispherebackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.medispherebackend.dto.PatientData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service to load and look up rich patient health records from patient-data.json.
 * Provides per-patient retrieval by patientId (P001-P010).
 * Does NOT duplicate FHIR or ML Demo patient data — this serves the existing
 * patient-data.json which contains conditions, vitals, lab results, and wearable data.
 */
@Service
public class PatientDataService {

    private static final Logger log = LoggerFactory.getLogger(PatientDataService.class);
    private static final String PATIENT_DATA_FILE = "patient-data.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile List<PatientData> cachedPatients;

    /**
     * Get all patient records from patient-data.json.
     */
    public List<PatientData> getAllPatients() {
        if (cachedPatients != null) {
            return cachedPatients;
        }
        try (InputStream inputStream = new ClassPathResource(PATIENT_DATA_FILE).getInputStream()) {
            List<PatientData> patients = objectMapper.readValue(inputStream, new TypeReference<>() {});
            cachedPatients = patients;
            log.info("Loaded {} patient records from {}", patients.size(), PATIENT_DATA_FILE);
            return patients;
        } catch (Exception e) {
            log.error("Failed to load patient data from {}: {}", PATIENT_DATA_FILE, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Look up a single patient record by patientId (e.g. "P001", "P002", "ML001").
     */
    public Optional<PatientData> getPatientById(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            return Optional.empty();
        }
        String normalizedId = patientId.trim();
        if (normalizedId.toUpperCase().startsWith("ML")) {
            normalizedId = "P" + normalizedId.substring(2);
        }
        final String searchId = normalizedId;
        return getAllPatients().stream()
                .filter(p -> p.getPatientId() != null && searchId.equalsIgnoreCase(p.getPatientId()))
                .findFirst();
    }
}
