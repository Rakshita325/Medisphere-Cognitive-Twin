package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.config.ClinicalThresholds;
import com.medisphere.medispherebackend.kafka.VitalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Clinical Rule Engine evaluates abnormal patient vitals using simple,
 * explainable clinical rules.
 *
 * NOTE: This engine provides monitoring decision-support and workflow actions.
 * It does NOT perform medical diagnoses or claim to diagnose diseases.
 */
@Service
public class ClinicalRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalRuleEngine.class);

    public ClinicalRuleResult evaluate(VitalData vitalData) {
        if (vitalData == null || vitalData.getType() == null) {
            return ClinicalRuleResult.notTriggered();
        }

        String type = vitalData.getType();

        if ("Heart Rate".equalsIgnoreCase(type)) {
            return evaluateHeartRate(vitalData);
        }

        if ("SpO2".equalsIgnoreCase(type)) {
            return evaluateSpo2(vitalData);
        }

        if ("Blood Pressure".equalsIgnoreCase(type)) {
            return evaluateBloodPressure(vitalData);
        }

        if ("Temperature".equalsIgnoreCase(type)) {
            return evaluateTemperature(vitalData);
        }

        return ClinicalRuleResult.notTriggered();
    }

    private ClinicalRuleResult evaluateHeartRate(VitalData vitalData) {
        Double heartRate = parseDouble(vitalData.getValue());
        if (heartRate == null) {
            return ClinicalRuleResult.notTriggered();
        }

        if (heartRate > ClinicalThresholds.HEART_RATE_HIGH) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Heart rate is above the configured safe threshold.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "HEART_RATE_HIGH"
            );
        }

        if (heartRate < ClinicalThresholds.HEART_RATE_LOW) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Heart rate is below the configured safe threshold.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "HEART_RATE_LOW"
            );
        }

        return ClinicalRuleResult.notTriggered();
    }

    private ClinicalRuleResult evaluateSpo2(VitalData vitalData) {
        Double spo2 = parseDouble(vitalData.getValue());
        if (spo2 == null) {
            return ClinicalRuleResult.notTriggered();
        }

        if (spo2 < ClinicalThresholds.SPO2_CRITICAL) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_CRITICAL,
                    "Low oxygen saturation detected.",
                    ClinicalThresholds.ACTION_IMMEDIATE_REVIEW,
                    "SPO2_CRITICAL"
            );
        }

        if (spo2 < ClinicalThresholds.SPO2_LOW) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Low oxygen saturation detected.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "SPO2_LOW"
            );
        }

        return ClinicalRuleResult.notTriggered();
    }

    private ClinicalRuleResult evaluateBloodPressure(VitalData vitalData) {
        Double systolic = vitalData.getSystolicBP();
        Double diastolic = vitalData.getDiastolicBP();

        // Fallback: If systolic/diastolic fields are null, attempt to parse from value string "systolic/diastolic"
        if (systolic == null && diastolic == null && vitalData.getValue() != null) {
            String[] parts = vitalData.getValue().split("/");
            if (parts.length == 2) {
                systolic = parseDouble(parts[0].trim());
                diastolic = parseDouble(parts[1].trim());
            } else {
                systolic = parseDouble(vitalData.getValue().trim());
            }
        }

        boolean systolicElevated = systolic != null && systolic > ClinicalThresholds.BP_SYSTOLIC_HIGH;
        boolean diastolicElevated = diastolic != null && diastolic > ClinicalThresholds.BP_DIASTOLIC_HIGH;
        boolean criticalSystolic = systolic != null && systolic >= ClinicalThresholds.BP_SYSTOLIC_CRITICAL;
        boolean criticalDiastolic = diastolic != null && diastolic >= ClinicalThresholds.BP_DIASTOLIC_CRITICAL;

        // If both are significantly elevated or individual reading reaches critical range
        if ((systolicElevated && diastolicElevated) || criticalSystolic || criticalDiastolic) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_CRITICAL,
                    "Significantly elevated blood pressure detected: systolic and diastolic above threshold.",
                    ClinicalThresholds.ACTION_IMMEDIATE_REVIEW,
                    "BP_COMBINED_HIGH"
            );
        }

        if (systolicElevated) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Systolic blood pressure is above the configured safe threshold.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "BP_SYSTOLIC_HIGH"
            );
        }

        if (diastolicElevated) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Diastolic blood pressure is above the configured safe threshold.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "BP_DIASTOLIC_HIGH"
            );
        }

        return ClinicalRuleResult.notTriggered();
    }

    private ClinicalRuleResult evaluateTemperature(VitalData vitalData) {
        Double temperature = parseDouble(vitalData.getValue());
        if (temperature == null) {
            return ClinicalRuleResult.notTriggered();
        }

        if (temperature > ClinicalThresholds.TEMPERATURE_HIGH) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Abnormal body temperature detected: elevated temperature.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "TEMPERATURE_HIGH"
            );
        }

        if (temperature < ClinicalThresholds.TEMPERATURE_LOW) {
            return ClinicalRuleResult.triggered(
                    ClinicalThresholds.SEVERITY_HIGH,
                    "Abnormal body temperature detected: low body temperature.",
                    ClinicalThresholds.ACTION_REVIEW_VITALS,
                    "TEMPERATURE_LOW"
            );
        }

        return ClinicalRuleResult.notTriggered();
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse numeric vital value: {}", value);
            return null;
        }
    }
}
