package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.Patient360Dto;
import com.medisphere.medispherebackend.fhir.FhirPatientService;
import com.medisphere.medispherebackend.repository.ConsentRepository;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import com.medisphere.medispherebackend.service.PatientTwinService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patient-360")
public class Patient360Controller {
    private final FhirPatientService fhirPatientService;
    private final PatientTwinService patientTwinService;
    private final ConsentRepository consentRepository;
    private final ConsentAuthorizationService consentAuthorizationService;

    public Patient360Controller(FhirPatientService fhirPatientService, PatientTwinService patientTwinService,
                                ConsentRepository consentRepository, ConsentAuthorizationService consentAuthorizationService) {
        this.fhirPatientService = fhirPatientService;
        this.patientTwinService = patientTwinService;
        this.consentRepository = consentRepository;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @GetMapping("/{sourcePatientId}")
    public Patient360Dto getPatient360(@PathVariable String sourcePatientId) {
        if (!consentAuthorizationService.isAllowed(sourcePatientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient consent is required");
        }
        return new Patient360Dto(
                fhirPatientService.getPatient(sourcePatientId),
                fhirPatientService.getConditions(sourcePatientId),
                fhirPatientService.getObservations(sourcePatientId),
                fhirPatientService.getLabs(sourcePatientId),
                fhirPatientService.getDiagnosticReports(sourcePatientId),
                fhirPatientService.getWearables(sourcePatientId),
                patientTwinService.getBySourcePatientId(sourcePatientId),
                consentRepository.findByPatientId(sourcePatientId).map(consent -> consent.isGranted()).orElse(false));
    }
}
