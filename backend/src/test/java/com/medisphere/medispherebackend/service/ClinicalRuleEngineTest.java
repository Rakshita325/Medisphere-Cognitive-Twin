package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.config.ClinicalThresholds;
import com.medisphere.medispherebackend.kafka.VitalData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalRuleEngineTest {

    private ClinicalRuleEngine clinicalRuleEngine;

    @BeforeEach
    void setUp() {
        clinicalRuleEngine = new ClinicalRuleEngine();
    }

    // 1. Normal heart rate → no rule triggered
    @Test
    @DisplayName("1. Normal heart rate should not trigger a rule")
    void testNormalHeartRate() {
        VitalData vital = createVital("Heart Rate", "75", null, null, "BPM");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertFalse(result.isRuleTriggered());
        assertNull(result.getSeverity());
    }

    // 2. High heart rate → HIGH
    @Test
    @DisplayName("2. High heart rate (>120) should trigger HIGH severity alert")
    void testHighHeartRate() {
        VitalData vital = createVital("Heart Rate", "135", null, null, "BPM");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Heart rate is above the configured safe threshold.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("HEART_RATE_HIGH", result.getRuleType());
    }

    // 3. Low heart rate → HIGH
    @Test
    @DisplayName("3. Low heart rate (<50) should trigger HIGH severity alert")
    void testLowHeartRate() {
        VitalData vital = createVital("Heart Rate", "45", null, null, "BPM");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Heart rate is below the configured safe threshold.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("HEART_RATE_LOW", result.getRuleType());
    }

    // 4. Normal SpO2 → no rule triggered
    @Test
    @DisplayName("4. Normal SpO2 (>=92%) should not trigger a rule")
    void testNormalSpo2() {
        VitalData vital = createVital("SpO2", "97", null, null, "%");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertFalse(result.isRuleTriggered());
        assertNull(result.getSeverity());
    }

    // 5. SpO2 below 92 (and >= 90) → HIGH
    @Test
    @DisplayName("5. SpO2 between 90% and 92% should trigger HIGH severity alert")
    void testLowSpo2HighSeverity() {
        VitalData vital = createVital("SpO2", "91", null, null, "%");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Low oxygen saturation detected.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("SPO2_LOW", result.getRuleType());
    }

    // 6. SpO2 below 90 → CRITICAL
    @Test
    @DisplayName("6. SpO2 below 90% should trigger CRITICAL severity alert")
    void testCriticalSpo2() {
        VitalData vital = createVital("SpO2", "88", null, null, "%");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_CRITICAL, result.getSeverity());
        assertEquals("Low oxygen saturation detected.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_IMMEDIATE_REVIEW, result.getRecommendedAction());
        assertEquals("SPO2_CRITICAL", result.getRuleType());
    }

    // 7. High systolic BP → HIGH
    @Test
    @DisplayName("7. High systolic BP (>140, diastolic normal) should trigger HIGH severity alert")
    void testHighSystolicBP() {
        VitalData vital = createVital("Blood Pressure", null, 148.0, 80.0, "mmHg");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Systolic blood pressure is above the configured safe threshold.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("BP_SYSTOLIC_HIGH", result.getRuleType());
    }

    // 8. High diastolic BP → HIGH
    @Test
    @DisplayName("8. High diastolic BP (>90, systolic normal) should trigger HIGH severity alert")
    void testHighDiastolicBP() {
        VitalData vital = createVital("Blood Pressure", null, 125.0, 96.0, "mmHg");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Diastolic blood pressure is above the configured safe threshold.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("BP_DIASTOLIC_HIGH", result.getRuleType());
    }

    // Combined High BP → CRITICAL
    @Test
    @DisplayName("Both systolic and diastolic elevated should trigger CRITICAL severity alert")
    void testBothElevatedBP() {
        VitalData vital = createVital("Blood Pressure", null, 150.0, 98.0, "mmHg");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_CRITICAL, result.getSeverity());
        assertEquals("Significantly elevated blood pressure detected: systolic and diastolic above threshold.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_IMMEDIATE_REVIEW, result.getRecommendedAction());
        assertEquals("BP_COMBINED_HIGH", result.getRuleType());
    }

    // Normal Blood Pressure
    @Test
    @DisplayName("Normal Blood Pressure (<=140 and <=90) should not trigger a rule")
    void testNormalBloodPressure() {
        VitalData vital = createVital("Blood Pressure", null, 120.0, 80.0, "mmHg");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertFalse(result.isRuleTriggered());
    }

    // 9. High temperature → HIGH
    @Test
    @DisplayName("9. High temperature (>38.0°C) should trigger HIGH severity alert")
    void testHighTemperature() {
        VitalData vital = createVital("Temperature", "39.1", null, null, "°C");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Abnormal body temperature detected: elevated temperature.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("TEMPERATURE_HIGH", result.getRuleType());
    }

    // 10. Low temperature → HIGH
    @Test
    @DisplayName("10. Low temperature (<35.0°C) should trigger HIGH severity alert")
    void testLowTemperature() {
        VitalData vital = createVital("Temperature", "34.2", null, null, "°C");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertTrue(result.isRuleTriggered());
        assertEquals(ClinicalThresholds.SEVERITY_HIGH, result.getSeverity());
        assertEquals("Abnormal body temperature detected: low body temperature.", result.getMessage());
        assertEquals(ClinicalThresholds.ACTION_REVIEW_VITALS, result.getRecommendedAction());
        assertEquals("TEMPERATURE_LOW", result.getRuleType());
    }

    // Normal Temperature
    @Test
    @DisplayName("Normal temperature (35.0 - 38.0°C) should not trigger a rule")
    void testNormalTemperature() {
        VitalData vital = createVital("Temperature", "36.8", null, null, "°C");

        ClinicalRuleResult result = clinicalRuleEngine.evaluate(vital);

        assertNotNull(result);
        assertFalse(result.isRuleTriggered());
    }

    // Null or invalid data handling
    @Test
    @DisplayName("Null or empty vital data should safely return not triggered")
    void testNullOrInvalidData() {
        assertFalse(clinicalRuleEngine.evaluate(null).isRuleTriggered());

        VitalData emptyVital = new VitalData();
        assertFalse(clinicalRuleEngine.evaluate(emptyVital).isRuleTriggered());

        VitalData invalidType = createVital("UnknownType", "100", null, null, "");
        assertFalse(clinicalRuleEngine.evaluate(invalidType).isRuleTriggered());
    }

    private VitalData createVital(String type, String value, Double systolic, Double diastolic, String unit) {
        VitalData data = new VitalData();
        data.setPatientId("P001");
        data.setType(type);
        data.setValue(value);
        data.setSystolicBP(systolic);
        data.setDiastolicBP(diastolic);
        data.setUnit(unit);
        data.setRecordedAt("2026-09-19T10:00:00Z");
        return data;
    }
}
