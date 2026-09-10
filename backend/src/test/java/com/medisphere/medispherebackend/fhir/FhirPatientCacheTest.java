package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.medisphere.medispherebackend.dto.FhirPatientDto;
import com.medisphere.medispherebackend.model.CachedFhirPatient;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.repository.CachedFhirPatientRepository;
import com.medisphere.medispherebackend.service.PatientDataFhirService;
import com.medisphere.medispherebackend.service.PatientTwinService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FhirPatientCacheTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient fhirClient;

    @Mock
    private PatientTwinService patientTwinService;

    @Mock
    private PatientDataFhirService patientDataFhirService;

    @Mock
    private CachedFhirPatientRepository cachedFhirPatientRepository;

    private FhirPatientService fhirPatientService;

    @BeforeEach
    void setUp() {
        fhirPatientService = new FhirPatientService(
                fhirClient,
                patientTwinService,
                patientDataFhirService,
                cachedFhirPatientRepository
        );
    }

    @Test
    void getPatientsCachesToMongoWhenFhirSucceeds() {
        Bundle bundle = new Bundle();
        Patient p1 = new Patient();
        p1.setId("Patient/fhir-001");
        p1.addIdentifier().setSystem(PatientDataFhirService.IDENTIFIER_SYSTEM).setValue("P001");
        p1.addName().setFamily("Sharma");
        p1.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
        bundle.addEntry().setResource(p1);

        when(fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute())
                .thenReturn(bundle);

        List<FhirPatientDto> result = fhirPatientService.getPatients();

        assertEquals(1, result.size());
        assertEquals("fhir-001", result.get(0).getId());
        assertEquals("P001", result.get(0).getSourcePatientId());
        verify(cachedFhirPatientRepository, atLeastOnce()).save(any(CachedFhirPatient.class));
    }

    @Test
    void getPatientsFallsBackToMongoWhenFhirFails() {
        when(fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute())
                .thenThrow(new FhirClientConnectionException("Connection refused or timed out"));

        CachedFhirPatient cached = new CachedFhirPatient("fhir-001", "P001", "Rahul Sharma", "male", "1971-05-12");
        when(cachedFhirPatientRepository.findAll()).thenReturn(List.of(cached));

        List<FhirPatientDto> result = fhirPatientService.getPatients();

        assertEquals(1, result.size());
        assertEquals("fhir-001", result.get(0).getFhirPatientId());
        assertEquals("P001", result.get(0).getSourcePatientId());
        assertEquals("Rahul Sharma", result.get(0).getName());
    }

    @Test
    void getPatientFallsBackToMongoWhenFhirFails() {
        when(fhirClient.search().forResource(Patient.class)
                .where(any(ca.uhn.fhir.rest.gclient.ICriterion.class))
                .returnBundle(Bundle.class).execute())
                .thenThrow(new FhirClientConnectionException("FHIR host down"));

        CachedFhirPatient cached = new CachedFhirPatient("fhir-001", "P001", "Rahul Sharma", "male", "1971-05-12");
        when(cachedFhirPatientRepository.findBySourcePatientId("P001")).thenReturn(Optional.of(cached));

        FhirPatientDto result = fhirPatientService.getPatient("P001");

        assertNotNull(result);
        assertEquals("P001", result.getSourcePatientId());
        assertEquals("Rahul Sharma", result.getName());
    }

    @Test
    void saveFhirPatientAsTwinFallsBackToCachedDataWhenFhirFails() {
        when(fhirClient.search().forResource(Patient.class)
                .where(any(ca.uhn.fhir.rest.gclient.ICriterion.class))
                .returnBundle(Bundle.class).execute())
                .thenThrow(new FhirClientConnectionException("FHIR host down"));

        CachedFhirPatient cached = new CachedFhirPatient("fhir-001", "P001", "Rahul Sharma", "male", "1980-01-01");
        when(cachedFhirPatientRepository.findBySourcePatientId("P001")).thenReturn(Optional.of(cached));
        when(patientTwinService.createPatientTwin(any(PatientTwin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientTwin twin = fhirPatientService.saveFhirPatientAsTwin("P001");

        assertNotNull(twin);
        assertEquals("fhir-001", twin.getFhirPatientId());
        assertEquals("P001", twin.getSourcePatientId());
        assertEquals("Rahul Sharma", twin.getName());
        verify(patientTwinService).createPatientTwin(any(PatientTwin.class));
    }
}
