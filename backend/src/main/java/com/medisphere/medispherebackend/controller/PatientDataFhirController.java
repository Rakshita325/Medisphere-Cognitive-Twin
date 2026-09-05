package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.service.PatientDataFhirService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir")
public class PatientDataFhirController {

    private final PatientDataFhirService service;

    public PatientDataFhirController(PatientDataFhirService service) {
        this.service = service;
    }

    @PostMapping("/import")
    public String importPatients() {
        return service.importPatientsToFhir();
    }
}