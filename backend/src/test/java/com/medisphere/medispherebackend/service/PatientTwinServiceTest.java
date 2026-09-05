package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.repository.PatientTwinRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientTwinServiceTest {

    @Mock
    private PatientTwinRepository repository;

    @Test
    void createUpdatesExistingTwinForSameSourcePatient() {
        PatientTwin existing = new PatientTwin("source-id", "Existing", 40, "female", "None");
        existing.setId("existing-id");
        when(repository.findBySourcePatientId("source-id")).thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any(PatientTwin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientTwin candidate = new PatientTwin("source-id", "Updated", 41, "female", "Asthma");
        PatientTwin result = new PatientTwinService(repository).createPatientTwin(candidate);

        assertEquals("existing-id", result.getId());
        verify(repository).save(candidate);
    }
}
