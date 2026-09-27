package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.CreateTreatmentTrackingRequest;
import com.medisphere.medispherebackend.dto.UpdateTreatmentStatusRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.model.TreatmentTracking;
import com.medisphere.medispherebackend.service.CarePlanService;
import com.medisphere.medispherebackend.service.TreatmentTrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/treatment-tracking")
public class TreatmentTrackingController {

    private final TreatmentTrackingService treatmentTrackingService;
    private final CarePlanService carePlanService;

    public TreatmentTrackingController(TreatmentTrackingService treatmentTrackingService,
                                       CarePlanService carePlanService) {
        this.treatmentTrackingService = treatmentTrackingService;
        this.carePlanService = carePlanService;
    }

    /**
     * 1. Create a treatment tracking record
     * POST /api/treatment-tracking
     */
    @PostMapping
    public ResponseEntity<TreatmentTracking> createTracking(@RequestBody CreateTreatmentTrackingRequest request) {
        if (request.getPatientId() == null || request.getDescription() == null) {
            return ResponseEntity.badRequest().build();
        }
        TreatmentTracking created = treatmentTrackingService.createTracking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Create multiple treatment tracking records in batch
     * POST /api/treatment-tracking/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<List<TreatmentTracking>> createTrackingBatch(@RequestBody List<TreatmentTracking> items) {
        List<TreatmentTracking> created = treatmentTrackingService.createTrackingBatch(items);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 2. Get treatment tracking records for a specific patient
     * GET /api/treatment-tracking/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<TreatmentTracking>> getTrackingByPatient(@PathVariable String patientId) {
        List<TreatmentTracking> records = treatmentTrackingService.getTrackingByPatientId(patientId);
        return ResponseEntity.ok(records);
    }

    /**
     * 3. Get treatment tracking records for a specific care plan
     * GET /api/treatment-tracking/careplan/{carePlanId}
     */
    @GetMapping("/careplan/{carePlanId}")
    public ResponseEntity<List<TreatmentTracking>> getTrackingByCarePlan(@PathVariable String carePlanId) {
        List<TreatmentTracking> records = treatmentTrackingService.getTrackingByCarePlanId(carePlanId);
        return ResponseEntity.ok(records);
    }

    /**
     * Get a specific treatment tracking record by ID
     * GET /api/treatment-tracking/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TreatmentTracking> getTrackingById(@PathVariable String id) {
        return treatmentTrackingService.getTrackingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 4. Update the status of an intervention
     * PUT /api/treatment-tracking/{id}/status
     * Supports statuses: PENDING, IN_PROGRESS, COMPLETED, MISSED, CANCELLED
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TreatmentTracking> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateTreatmentStatusRequest statusRequest) {
        if (statusRequest.getStatus() == null || statusRequest.getStatus().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<TreatmentTracking> updated = treatmentTrackingService.updateStatus(
                id,
                statusRequest.getStatus(),
                statusRequest.getNotes(),
                statusRequest.getCompletedAt()
        );

        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update entire treatment tracking record
     * PUT /api/treatment-tracking/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TreatmentTracking> updateTracking(
            @PathVariable String id,
            @RequestBody TreatmentTracking updated) {
        return treatmentTrackingService.updateTracking(id, updated)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 5. Delete a treatment tracking record
     * DELETE /api/treatment-tracking/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTracking(@PathVariable String id) {
        boolean deleted = treatmentTrackingService.deleteTracking(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 6. Calculate treatment adherence for a patient
     * GET /api/treatment-tracking/patient/{patientId}/adherence
     */
    @GetMapping("/patient/{patientId}/adherence")
    public ResponseEntity<Map<String, Object>> getPatientAdherence(@PathVariable String patientId) {
        Map<String, Object> adherence = treatmentTrackingService.calculatePatientAdherence(patientId);
        return ResponseEntity.ok(adherence);
    }

    /**
     * 7. Initialize/generate tracking records directly from an existing Care Plan
     * POST /api/treatment-tracking/careplan/{carePlanId}/initialize
     */
    @PostMapping("/careplan/{carePlanId}/initialize")
    public ResponseEntity<List<TreatmentTracking>> initializeCarePlanTracking(@PathVariable String carePlanId) {
        Optional<CarePlan> planOpt = carePlanService.getCarePlanById(carePlanId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<TreatmentTracking> initialized = treatmentTrackingService.initializeTrackingForCarePlan(planOpt.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(initialized);
    }
}
