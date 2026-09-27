package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.service.PatientDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for serving rich patient health records from patient-data.json.
 * Provides GET /api/patient-data and GET /api/patient-data/{patientId}.
 */
@RestController
@RequestMapping("/api/patient-data")
public class PatientDataController {

    private final PatientDataService patientDataService;

    public PatientDataController(PatientDataService patientDataService) {
        this.patientDataService = patientDataService;
    }

    /**
     * GET /api/patient-data
     * Returns all patient records from patient-data.json.
     */
    @GetMapping
    public ResponseEntity<List<PatientData>> getAllPatients() {
        return ResponseEntity.ok(patientDataService.getAllPatients());
    }

    /**
     * GET /api/patient-data/{patientId}
     * Returns a specific patient record by patientId (e.g. P001, P002).
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientData> getPatientById(@PathVariable String patientId) {
        return patientDataService.getPatientById(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
