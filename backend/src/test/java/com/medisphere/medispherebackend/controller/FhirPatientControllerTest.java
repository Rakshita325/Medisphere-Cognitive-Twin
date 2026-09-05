package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.FhirPatientDto;
import com.medisphere.medispherebackend.fhir.FhirPatientService;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FhirPatientControllerTest {
    @Mock FhirPatientService fhirPatientService;
    @Mock ConsentAuthorizationService consentAuthorizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new FhirPatientController(fhirPatientService, consentAuthorizationService)).build();
    }

    @Test
    void patientDetailsAreReturnedWhenConsentIsGranted() throws Exception {
        when(consentAuthorizationService.isAllowed("P001")).thenReturn(true);
        when(fhirPatientService.getPatient("P001"))
                .thenReturn(new FhirPatientDto("fhir-1", "P001", "Rahul Sharma", "male", "1971-05-12"));

        mockMvc.perform(get("/api/fhir/patients/P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcePatientId").value("P001"))
                .andExpect(jsonPath("$.fhirPatientId").value("fhir-1"));
    }

    @Test
    void patientDetailsAreForbiddenWithoutConsent() throws Exception {
        when(consentAuthorizationService.isAllowed("P001")).thenReturn(false);

        mockMvc.perform(get("/api/fhir/patients/P001"))
                .andExpect(status().isForbidden());
    }
}
