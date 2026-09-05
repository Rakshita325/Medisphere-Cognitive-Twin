package com.medisphere.medispherebackend.kafka;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VitalConsumerTest {
    @Mock VitalRecordRepository repository;
    @Mock IGenericClient fhirClient;

    @Test
    void invalidVitalIsRejectedWithoutPersistenceOrFhirCall() {
        VitalData vital = new VitalData();
        vital.setType("Heart Rate");
        vital.setValue("80");

        new VitalConsumer(repository, fhirClient).consumeVitalData(vital);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(fhirClient);
    }

    private void verifyNoInteractions(IGenericClient client) {
        org.mockito.Mockito.verifyNoInteractions(client);
    }
}
