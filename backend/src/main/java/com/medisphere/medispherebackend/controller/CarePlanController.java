package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.GenerateCarePlanRequest;
import com.medisphere.medispherebackend.dto.UpdateCarePlanStatusRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.service.CarePlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/care-plans", "/api/careplans"})
public class CarePlanController {

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    /**
     * 1. Generate/create a care plan for a patient
     * POST /api/care-plans/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<CarePlan> generateCarePlan(@RequestBody GenerateCarePlanRequest request) {
        CarePlan createdPlan = carePlanService.generateCarePlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }

    /**
     * 2. Get a patient's care plans
     * GET /api/care-plans/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<CarePlan>> getPatientCarePlans(@PathVariable String patientId) {
        List<CarePlan> plans = carePlanService.getCarePlansByPatientId(patientId);
        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/care-plans/patient/{patientId}/latest
     * Get a patient's latest care plan
     */
    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<CarePlan> getLatestPatientCarePlan(@PathVariable String patientId) {
        return carePlanService.getLatestCarePlanByPatientId(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 3. Get all care plans
     * GET /api/care-plans
     */
    @GetMapping
    public ResponseEntity<List<CarePlan>> getAllCarePlans() {
        List<CarePlan> plans = carePlanService.getAllCarePlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/care-plans/{id}
     * Get care plan by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CarePlan> getCarePlanById(@PathVariable String id) {
        return carePlanService.getCarePlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 4. Update a care plan
     * PUT /api/care-plans/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CarePlan> updateCarePlan(@PathVariable String id, @RequestBody CarePlan updatedPlan) {
        return carePlanService.updateCarePlan(id, updatedPlan)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 5. Change care plan status
     * PATCH /api/care-plans/{id}/status or PUT /api/care-plans/{id}/status
     */
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<CarePlan> updateCarePlanStatus(
            @PathVariable String id,
            @RequestBody UpdateCarePlanStatusRequest statusRequest) {
        if (statusRequest.getStatus() == null || statusRequest.getStatus().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return carePlanService.updateCarePlanStatus(id, statusRequest.getStatus())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
