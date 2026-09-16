package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.model.Consent;
import com.medisphere.medispherebackend.repository.ConsentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentAuthorizationServiceTest {
    @Mock ConsentRepository consentRepository;
    @Mock IGenericClient fhirClient;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantedConsentAllowsClinicalUser() {
        allowRole("DOCTOR");
        Consent consent = new Consent("P001", "doctor1", "healthcare", true);
        when(consentRepository.findByPatientId("P001")).thenReturn(java.util.List.of(consent));

        assertTrue(new ConsentAuthorizationService(consentRepository, fhirClient).isAllowed("P001"));
    }

    @Test
    void revokedConsentDeniesClinicalUser() {
        allowRole("DOCTOR");
        Consent consent = new Consent("P001", "doctor1", "healthcare", false);
        when(consentRepository.findByPatientId("P001")).thenReturn(java.util.List.of(consent));

        assertFalse(new ConsentAuthorizationService(consentRepository, fhirClient).isAllowed("P001"));
    }

    @Test
    void adminBypassesPatientConsent() {
        allowRole("ADMIN");

        assertTrue(new ConsentAuthorizationService(consentRepository, fhirClient).isAllowed("P001"));
    }

    private void allowRole(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", 
                        java.util.List.of(() -> "ROLE_" + role)));
    }
}
