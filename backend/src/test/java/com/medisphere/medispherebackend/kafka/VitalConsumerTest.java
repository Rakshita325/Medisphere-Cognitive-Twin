package com.medisphere.medispherebackend.kafka;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import com.medisphere.medispherebackend.service.AlertService;
import com.medisphere.medispherebackend.service.AnomalyDetectionService;
import com.medisphere.medispherebackend.service.ClinicalRuleEngine;
import com.medisphere.medispherebackend.service.ClinicalRuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private ClinicalRuleEngine clinicalRuleEngine;

    @Mock
    private AlertService alertService;

    private VitalConsumer vitalConsumer;

    @BeforeEach
    void setUp() {
        vitalConsumer = new VitalConsumer(
                vitalRecordRepository,
                fhirClient,
                anomalyDetectionService,
                clinicalRuleEngine,
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

        verify(vitalRecordRepository).save(any(VitalRecord.class));
    }

    @Test
    void shouldProcessAnomalousVitalAndEvaluateClinicalRulesAndCreateAlert() {
        VitalData vitalData = new VitalData();

        vitalData.setPatientId("P001");
        vitalData.setType("SpO2");
        vitalData.setValue("88");
        vitalData.setUnit("%");
        vitalData.setRecordedAt("2026-09-18T10:00:00Z");

        ClinicalRuleResult ruleResult = ClinicalRuleResult.triggered(
                "CRITICAL",
                "Low oxygen saturation detected.",
                "Immediate clinical review recommended",
                "SPO2_CRITICAL");

        when(anomalyDetectionService.isAnomaly(vitalData))
                .thenReturn(true);
        when(clinicalRuleEngine.evaluate(vitalData))
                .thenReturn(ruleResult);

        vitalConsumer.consumeVitalData(vitalData);

        verify(vitalRecordRepository).save(any(VitalRecord.class));
        verify(clinicalRuleEngine).evaluate(vitalData);
        verify(alertService).createAlert(eq(vitalData), eq(ruleResult));
    }
}