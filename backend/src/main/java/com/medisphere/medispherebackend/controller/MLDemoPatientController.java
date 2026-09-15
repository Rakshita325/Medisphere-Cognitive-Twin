package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.service.MLDemoPatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for ML demo patients (Milestone 2 ML Demo cohort).
 * These are synthetic, ML-ready patients completely separate from FHIR patients.
 * No clinical data, no consent requirements - purely for ML model demonstration.
 */
@RestController
@RequestMapping("/api/ml/demo-patients")
public class MLDemoPatientController {

    private final MLDemoPatientService mlDemoPatientService;

    public MLDemoPatientController(MLDemoPatientService mlDemoPatientService) {
        this.mlDemoPatientService = mlDemoPatientService;
    }

    /**
     * GET /api/ml/demo-patients
     * Returns all ML demo patients (ML001-ML005)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllMLDemoPatients() {
        List<Map<String, Object>> patients = mlDemoPatientService.getMLDemoPatients();
        return ResponseEntity.ok(patients);
    }

    /**
     * GET /api/ml/demo-patients/{patientId}
     * Returns a specific ML demo patient by ID
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<Map<String, Object>> getMLDemoPatient(@PathVariable String patientId) {
        Map<String, Object> patient = mlDemoPatientService.getMLDemoPatient(patientId);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "ML demo patient not found: " + patientId)
            );
        }
        return ResponseEntity.ok(patient);
    }
}
