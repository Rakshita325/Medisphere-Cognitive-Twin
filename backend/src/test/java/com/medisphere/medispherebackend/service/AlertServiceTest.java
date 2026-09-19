package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.config.ClinicalThresholds;
import com.medisphere.medispherebackend.kafka.VitalData;
import com.medisphere.medispherebackend.model.Alert;
import com.medisphere.medispherebackend.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ClinicalRuleEngine clinicalRuleEngine;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, messagingTemplate, clinicalRuleEngine);
    }

    @Test
    void testCreateAlertWithClinicalRuleResult() {
        VitalData vital = new VitalData();
        vital.setPatientId("P001");
        vital.setType("Heart Rate");
        vital.setValue("135");
        vital.setUnit("BPM");

        ClinicalRuleResult ruleResult = ClinicalRuleResult.triggered(
                ClinicalThresholds.SEVERITY_HIGH,
                "Heart rate is above the configured safe threshold.",
                ClinicalThresholds.ACTION_REVIEW_VITALS,
                "HEART_RATE_HIGH"
        );

        when(alertRepository.findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc("P001", "Heart Rate", "ACTIVE"))
                .thenReturn(Optional.empty());

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Alert> created = alertService.createAlert(vital, ruleResult);

        assertTrue(created.isPresent());
        Alert alert = created.get();
        assertEquals("P001", alert.getPatientId());
        assertEquals("Heart Rate", alert.getVitalType());
        assertEquals(135.0, alert.getValue());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, alert.getSeverity());
        assertEquals("Heart rate is above the configured safe threshold.", alert.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, alert.getRecommendedAction());
        assertEquals("HEART_RATE_HIGH", alert.getRuleType());
        assertEquals("ACTIVE", alert.getStatus());
        
        // New fields
        assertEquals("CARDIOLOGIST", alert.getAssignedDoctorRole());
        assertEquals("NOTIFIED", alert.getNotificationStatus());

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Alert.class));
    }

    @Test
    void testCreateAlertEvaluatesViaRuleEngineWhenNotProvided() {
        VitalData vital = new VitalData();
        vital.setPatientId("P002");
        vital.setType("SpO2");
        vital.setValue("89");
        vital.setUnit("%");

        ClinicalRuleResult ruleResult = ClinicalRuleResult.triggered(
                ClinicalThresholds.SEVERITY_CRITICAL,
                "Low oxygen saturation detected.",
                ClinicalThresholds.ACTION_IMMEDIATE_REVIEW,
                "SPO2_CRITICAL"
        );

        when(clinicalRuleEngine.evaluate(vital)).thenReturn(ruleResult);
        when(alertRepository.findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc("P002", "SpO2", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Alert> created = alertService.createAlert(vital);

        assertTrue(created.isPresent());
        Alert alert = created.get();
        assertEquals(ClinicalThresholds.SEVERITY_CRITICAL, alert.getSeverity());
        assertEquals(ClinicalThresholds.ACTION_IMMEDIATE_REVIEW, alert.getRecommendedAction());
        assertEquals("SPO2_CRITICAL", alert.getRuleType());
        assertEquals("CARDIOLOGIST", alert.getAssignedDoctorRole());
        assertEquals("NOTIFIED", alert.getNotificationStatus());
    }

    @Test
    void testRoutingTemperatureToGeneralPhysician() {
        VitalData vital = new VitalData();
        vital.setPatientId("P003");
        vital.setType("Temperature");
        vital.setValue("40");
        vital.setUnit("C");

        when(clinicalRuleEngine.evaluate(vital)).thenReturn(ClinicalRuleResult.notTriggered());
        when(alertRepository.findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc("P003", "Temperature", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Alert> created = alertService.createAlert(vital);

        assertTrue(created.isPresent());
        Alert alert = created.get();
        assertEquals("GENERAL_PHYSICIAN", alert.getAssignedDoctorRole());
        assertEquals("NOTIFIED", alert.getNotificationStatus());
    }

    @Test
    void testDuplicateActiveAlertIsSkipped() {
        VitalData vital = new VitalData();
        vital.setPatientId("P001");
        vital.setType("Heart Rate");
        vital.setValue("135");

        Alert existingAlert = new Alert();
        existingAlert.setPatientId("P001");
        existingAlert.setVitalType("Heart Rate");
        existingAlert.setStatus("ACTIVE");

        when(alertRepository.findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc("P001", "Heart Rate", "ACTIVE"))
                .thenReturn(Optional.of(existingAlert));

        Optional<Alert> result = alertService.createAlert(vital);

        assertTrue(result.isPresent());
        assertEquals(existingAlert, result.get());
        verify(alertRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
