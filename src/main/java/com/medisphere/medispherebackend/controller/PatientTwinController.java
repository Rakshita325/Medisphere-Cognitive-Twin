package com.medisphere.medispherebackend.controller;
import jakarta.validation.Valid;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.service.PatientTwinService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-twins")
public class PatientTwinController {

    private final PatientTwinService patientTwinService;

    public PatientTwinController(PatientTwinService patientTwinService) {
        this.patientTwinService = patientTwinService;
    }

    @PostMapping
    public PatientTwin createPatientTwin(@Valid @RequestBody PatientTwin patientTwin) {
        return patientTwinService.createPatientTwin(patientTwin);
    }

    @GetMapping
    public List<PatientTwin> getAllPatientTwins() {
        return patientTwinService.getAllPatientTwins();
    }

    @GetMapping("/{id}")
    public PatientTwin getPatientTwinById(@PathVariable String id) {
        return patientTwinService.getPatientTwinById(id);
    }

    @DeleteMapping("/{id}")
    public void deletePatientTwin(@PathVariable String id) {
        patientTwinService.deletePatientTwin(id);
    }
}