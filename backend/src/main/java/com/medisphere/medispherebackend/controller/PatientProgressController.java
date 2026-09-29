package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.PatientProgressResponseDto;
import com.medisphere.medispherebackend.service.PatientProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Milestone 4 Part 4: Progress / Improvement Controller
 *
 * REST controller that compares a patient's earlier/baseline health measurements
 * with their latest recorded measurements.
 *
 * Endpoint:
 *   GET /api/progress/patient/{patientId}
 */
@RestController
@RequestMapping("/api/progress")
public class PatientProgressController {

    private final PatientProgressService patientProgressService;

    public PatientProgressController(PatientProgressService patientProgressService) {
        this.patientProgressService = patientProgressService;
    }

    /**
     * GET /api/progress/patient/{patientId}
     *
     * Returns the baseline, latest, change, and status for all health metrics
     * of the requested patient.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PatientProgressResponseDto> getPatientProgress(@PathVariable String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required");
        }

        PatientProgressResponseDto response = patientProgressService.calculateProgress(patientId);
        return ResponseEntity.ok(response);
    }
}
