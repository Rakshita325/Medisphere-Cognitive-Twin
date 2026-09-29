package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.model.HealthMeasurement;
import com.medisphere.medispherebackend.repository.HealthMeasurementRepository;
import com.medisphere.medispherebackend.dto.PatientProgressResponseDto;
import com.medisphere.medispherebackend.service.PatientProgressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Milestone 4 Part 3: Health Monitoring
 *
 * REST controller for recording and retrieving patient health measurements.
 * These measurements are stored separately from the existing vital_records
 * collection so that baseline data is never overwritten.
 *
 * Endpoints:
 *   POST   /api/health-monitoring               — Record a new measurement
 *   GET    /api/health-monitoring/patient/{id}   — All measurements for patient
 *   GET    /api/health-monitoring/patient/{id}/type/{type} — Filtered by type
 *   DELETE /api/health-monitoring/{id}           — Delete a measurement
 */
@RestController
@RequestMapping("/api/health-monitoring")
public class HealthMonitoringController {

    private final HealthMeasurementRepository healthMeasurementRepository;

    /**
     * Supported measurement types.
     * Blood Pressure uses systolic/diastolic fields instead of the single value field.
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "Blood Pressure",
            "Blood Glucose",
            "HbA1c",
            "Heart Rate",
            "SpO2",
            "Weight",
            "Cholesterol",
            "Hemoglobin",
            "Creatinine",
            "TSH",
            "Temperature"
    );

    private final PatientProgressService patientProgressService;

    public HealthMonitoringController(
            HealthMeasurementRepository healthMeasurementRepository,
            @Autowired(required = false) PatientProgressService patientProgressService) {
        this.healthMeasurementRepository = healthMeasurementRepository;
        this.patientProgressService = patientProgressService;
    }

    /**
     * POST /api/health-monitoring
     *
     * Record a new health measurement for a patient.
     * Validates required fields and assigns server timestamp.
     */
    @PostMapping
    public ResponseEntity<HealthMeasurement> createMeasurement(
            @Valid @RequestBody HealthMeasurement measurement) {

        // Validate patientId
        if (measurement.getPatientId() == null || measurement.getPatientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required");
        }

        // Validate measurementType
        if (measurement.getMeasurementType() == null || measurement.getMeasurementType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "measurementType is required");
        }

        if (!SUPPORTED_TYPES.contains(measurement.getMeasurementType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported measurement type: " + measurement.getMeasurementType()
                            + ". Supported types: " + SUPPORTED_TYPES);
        }

        // Validate value — Blood Pressure uses systolic/diastolic instead
        if ("Blood Pressure".equals(measurement.getMeasurementType())) {
            if (measurement.getSystolic() == null || measurement.getDiastolic() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Blood Pressure requires both systolic and diastolic values");
            }
        } else {
            if (measurement.getValue() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "value is required for measurement type: " + measurement.getMeasurementType());
            }
        }

        // Validate unit
        if (measurement.getUnit() == null || measurement.getUnit().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unit is required");
        }

        // Validate or default recordedAt
        if (measurement.getRecordedAt() == null || measurement.getRecordedAt().isBlank()) {
            measurement.setRecordedAt(Instant.now().toString());
        }

        // Set server-side creation timestamp and default source
        measurement.setCreatedAt(Instant.now());
        if (measurement.getSource() == null || measurement.getSource().isBlank()) {
            measurement.setSource("MANUAL");
        }

        HealthMeasurement saved = healthMeasurementRepository.save(measurement);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/health-monitoring/patient/{patientId}
     *
     * Returns all health measurements for the specified patient, newest first.
     */
    @GetMapping("/patient/{patientId}")
    public List<HealthMeasurement> getByPatient(@PathVariable String patientId) {
        return healthMeasurementRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /**
     * GET /api/health-monitoring/patient/{patientId}/type/{measurementType}
     *
     * Returns measurements for the specified patient filtered by type, newest first.
     */
    @GetMapping("/patient/{patientId}/type/{measurementType}")
    public List<HealthMeasurement> getByPatientAndType(
            @PathVariable String patientId,
            @PathVariable String measurementType) {
        return healthMeasurementRepository
                .findByPatientIdAndMeasurementTypeOrderByCreatedAtDesc(patientId, measurementType);
    }

    /**
     * DELETE /api/health-monitoring/{id}
     *
     * Deletes a specific health measurement by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeasurement(@PathVariable String id) {
        if (!healthMeasurementRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found: " + id);
        }
        healthMeasurementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/health-monitoring/patient/{patientId}/progress
     *
     * Milestone 4 Part 4: Progress / Improvement
     * Returns baseline-vs-latest comparison for all metrics of the specified patient.
     */
    @GetMapping("/patient/{patientId}/progress")
    public ResponseEntity<PatientProgressResponseDto> getPatientProgress(@PathVariable String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required");
        }
        if (patientProgressService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Progress service is unavailable");
        }
        return ResponseEntity.ok(patientProgressService.calculateProgress(patientId));
    }
}
