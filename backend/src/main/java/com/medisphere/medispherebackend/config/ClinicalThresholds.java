package com.medisphere.medispherebackend.config;

/**
 * Centralized clinical thresholds and recommended workflow actions for the
 * Clinical Rule Engine.
 *
 * NOTE: These are screening rules and workflow recommendations for monitoring,
 * NOT medical diagnoses.
 */
public final class ClinicalThresholds {

    private ClinicalThresholds() {
        // Prevent instantiation
    }

    // Heart Rate (BPM)
    public static final double HEART_RATE_LOW = 50.0;
    public static final double HEART_RATE_HIGH = 120.0;

    // SpO2 (%)
    public static final double SPO2_LOW = 92.0;
    public static final double SPO2_CRITICAL = 90.0;

    // Blood Pressure (mmHg)
    public static final double BP_SYSTOLIC_HIGH = 140.0;
    public static final double BP_DIASTOLIC_HIGH = 90.0;
    public static final double BP_SYSTOLIC_CRITICAL = 180.0;
    public static final double BP_DIASTOLIC_CRITICAL = 110.0;

    // Temperature (°C)
    public static final double TEMPERATURE_LOW = 35.0;
    public static final double TEMPERATURE_HIGH = 38.0;

    // Standard Severities
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    // Standard Workflow Recommended Actions
    public static final String ACTION_REVIEW_VITALS = "Review patient vitals";
    public static final String ACTION_IMMEDIATE_REVIEW = "Immediate clinical review recommended";
}
