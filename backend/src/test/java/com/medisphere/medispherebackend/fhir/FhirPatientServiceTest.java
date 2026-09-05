package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.service.PatientDataFhirService;
import com.medisphere.medispherebackend.service.PatientTwinService;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FhirPatientServiceTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) IGenericClient fhirClient;
    @Mock PatientTwinService patientTwinService;
    @Mock PatientDataFhirService patientDataFhirService;

    @Test
    void sourcePatientIdResolvesToGeneratedFhirPatient() {
        Patient patient = new Patient();
        patient.setId("Patient/fhir-123");
        patient.addIdentifier().setSystem(PatientDataFhirService.IDENTIFIER_SYSTEM).setValue("P001");
        patient.addName().setFamily("Sharma");
        patient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);

        when(fhirClient.read().resource(Patient.class).withId("fhir-123").execute()).thenReturn(patient);

        var result = new FhirPatientService(fhirClient, patientTwinService, patientDataFhirService)
                .getPatient("fhir-123");

        assertEquals("fhir-123", result.getFhirPatientId());
        assertEquals("P001", result.getSourcePatientId());
        assertEquals("male", result.getGender());
    }
}
