package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.FhirPatientDto;
import com.medisphere.medispherebackend.dto.FhirConditionDto;
import com.medisphere.medispherebackend.dto.FhirDiagnosticReportDto;
import com.medisphere.medispherebackend.dto.FhirObservationDto;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.fhir.FhirPatientService;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fhir")
public class FhirPatientController {

    private final FhirPatientService fhirPatientService;
    private final ConsentAuthorizationService consentAuthorizationService;

    public FhirPatientController(FhirPatientService fhirPatientService,
                                 ConsentAuthorizationService consentAuthorizationService) {
        this.fhirPatientService = fhirPatientService;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @GetMapping("/patients")
    public List<FhirPatientDto> getPatients() {
        return fhirPatientService.getPatients();
    }

    @GetMapping("/patients/{id}")
    public FhirPatientDto getPatient(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getPatient(id);
    }

    @GetMapping("/patients/{id}/conditions")
    public List<FhirConditionDto> getConditions(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getConditions(id);
    }

    @GetMapping("/patients/{id}/observations")
    public List<FhirObservationDto> getObservations(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getObservations(id);
    }

    @GetMapping("/patients/{id}/labs")
    public List<FhirObservationDto> getLabs(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getLabs(id);
    }

    @GetMapping("/patients/{id}/diagnostic-reports")
    public List<FhirDiagnosticReportDto> getDiagnosticReports(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getDiagnosticReports(id);
    }

    @GetMapping("/patients/{id}/wearables")
    public List<FhirObservationDto> getWearables(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.getWearables(id);
    }

    @GetMapping("/patients/{id}/save-twin")
    public PatientTwin savePatientAsTwin(@PathVariable String id) {
        requireConsent(id);
        return fhirPatientService.saveFhirPatientAsTwin(id);
    }

    private void requireConsent(String patientId) {
        if (!consentAuthorizationService.isAllowed(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient consent is required");
        }
    }
}