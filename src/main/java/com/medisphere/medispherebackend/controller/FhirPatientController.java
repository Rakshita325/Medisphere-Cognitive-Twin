package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.model.PatientTwin;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.medisphere.medispherebackend.fhir.FhirPatientService;

@RestController
@RequestMapping("/api/fhir")
public class FhirPatientController {

    private final FhirPatientService fhirPatientService;

    public FhirPatientController(FhirPatientService fhirPatientService) {
        this.fhirPatientService = fhirPatientService;
    }

    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return fhirPatientService.getPatients();
    }
    @GetMapping("/patients/{id}/save-twin")
    public PatientTwin savePatientAsTwin(@PathVariable String id) {
        return fhirPatientService.saveFhirPatientAsTwin(id);
    }
}