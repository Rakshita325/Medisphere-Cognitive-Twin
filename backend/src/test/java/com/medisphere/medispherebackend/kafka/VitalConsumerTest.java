package com.medisphere.medispherebackend.kafka;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import com.medisphere.medispherebackend.service.AlertService;
import com.medisphere.medispherebackend.service.AnomalyDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalConsumerTest {

    @Mock
    private VitalRecordRepository vitalRecordRepository;

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @Mock
    private AlertService alertService;

    private VitalConsumer vitalConsumer;

    @BeforeEach
    void setUp() {
        vitalConsumer = new VitalConsumer(
                vitalRecordRepository,
                fhirClient,
                anomalyDetectionService,
                alertService);
    }

    @Test
    void shouldProcessNormalVital() {
        VitalData vitalData = new VitalData();

        vitalData.setPatientId("P001");
        vitalData.setType("SpO2");
        vitalData.setValue("98");
        vitalData.setUnit("%");
        vitalData.setRecordedAt("2026-09-18T10:00:00Z");

        when(anomalyDetectionService.isAnomaly(vitalData))
                .thenReturn(false);

        vitalConsumer.consumeVitalData(vitalData);

        verify(vitalRecordRepository).save(org.mockito.ArgumentMatchers.any(VitalRecord.class));
    }

    @Test
    void shouldProcessAnomalousVitalAndCreateAlert() {
        VitalData vitalData = new VitalData();

        vitalData.setPatientId("P001");
        vitalData.setType("SpO2");
        vitalData.setValue("88");
        vitalData.setUnit("%");
        vitalData.setRecordedAt("2026-09-18T10:00:00Z");

        when(anomalyDetectionService.isAnomaly(vitalData))
                .thenReturn(true);

        vitalConsumer.consumeVitalData(vitalData);

        verify(vitalRecordRepository).save(org.mockito.ArgumentMatchers.any(VitalRecord.class));
        verify(alertService).createAlert(vitalData);
    }
}