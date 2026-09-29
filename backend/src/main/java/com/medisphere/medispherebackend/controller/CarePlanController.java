package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.CarePlanValidationDto;
import com.medisphere.medispherebackend.dto.GenerateCarePlanRequest;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.dto.UpdateCarePlanStatusRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.service.CarePlanService;
import com.medisphere.medispherebackend.service.CarePlanValidationService;
import com.medisphere.medispherebackend.service.PatientDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/care-plans", "/api/careplans"})
public class CarePlanController {

    private final CarePlanService carePlanService;
    private final CarePlanValidationService carePlanValidationService;
    private final PatientDataService patientDataService;

    public CarePlanController(CarePlanService carePlanService,
                              CarePlanValidationService carePlanValidationService,
                              @Autowired(required = false) PatientDataService patientDataService) {
        this.carePlanService = carePlanService;
        this.carePlanValidationService = carePlanValidationService;
        this.patientDataService = patientDataService;
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

    /**
     * 6. Validate Care Plan by ID
     * GET /api/care-plans/{id}/validation
     */
    @GetMapping("/{id}/validation")
    public ResponseEntity<CarePlanValidationDto> getCarePlanValidation(@PathVariable String id) {
        Optional<CarePlan> planOpt = carePlanService.getCarePlanById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CarePlan plan = planOpt.get();
        PatientData pd = null;
        if (patientDataService != null && plan.getPatientId() != null) {
            pd = patientDataService.getPatientById(plan.getPatientId()).orElse(null);
        }
        CarePlanValidationDto validation = carePlanValidationService.validateCarePlan(plan, pd);
        return ResponseEntity.ok(validation);
    }

    /**
     * 7. Validate latest Care Plan for Patient
     * GET /api/care-plans/patient/{patientId}/validation
     */
    @GetMapping("/patient/{patientId}/validation")
    public ResponseEntity<CarePlanValidationDto> getPatientLatestCarePlanValidation(@PathVariable String patientId) {
        Optional<CarePlan> planOpt = carePlanService.getLatestCarePlanByPatientId(patientId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CarePlan plan = planOpt.get();
        PatientData pd = null;
        if (patientDataService != null && plan.getPatientId() != null) {
            pd = patientDataService.getPatientById(plan.getPatientId()).orElse(null);
        }
        CarePlanValidationDto validation = carePlanValidationService.validateCarePlan(plan, pd);
        return ResponseEntity.ok(validation);
    }
}
