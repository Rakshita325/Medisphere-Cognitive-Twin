package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.repository.ConsentRepository;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ConsentAuthorizationService {
    private final ConsentRepository consentRepository;
    private final IGenericClient fhirClient;

    public ConsentAuthorizationService(ConsentRepository consentRepository, IGenericClient fhirClient) {
        this.consentRepository = consentRepository;
        this.fhirClient = fhirClient;
    }

    public boolean isAllowed(String patientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return true;
        }
        if (consentRepository.findByPatientId(patientId).map(consent -> consent.isGranted()).orElse(false)) return true;
        String sourcePatientId = resolveSourcePatientId(patientId);
        return sourcePatientId != null && consentRepository.findByPatientId(sourcePatientId)
                .map(consent -> consent.isGranted())
                .orElse(false);
    }

    private String resolveSourcePatientId(String patientId) {
        try {
            Patient patient = fhirClient.read().resource(Patient.class).withId(patientId).execute();
            return patient.getIdentifier().stream()
                    .filter(identifier -> "urn:medisphere:patient-id".equals(identifier.getSystem()))
                    .map(identifier -> identifier.getValue()).findFirst().orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
