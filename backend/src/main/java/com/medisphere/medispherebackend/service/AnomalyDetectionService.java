package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.kafka.VitalData;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    // Configurable project thresholds for anomaly detection.
    // These are screening rules for the system, not medical diagnoses.
    private static final double HEART_RATE_HIGH = 120.0;
    private static final double HEART_RATE_LOW = 50.0;

    private static final double SPO2_LOW = 92.0;

    private static final double SYSTOLIC_HIGH = 140.0;
    private static final double DIASTOLIC_HIGH = 90.0;

    private static final double TEMPERATURE_HIGH = 38.0;
    private static final double TEMPERATURE_LOW = 35.0;

    public boolean isAnomaly(VitalData vitalData) {

        if (vitalData == null || vitalData.getType() == null) {
            return false;
        }

        String type = vitalData.getType();

        if ("Heart Rate".equalsIgnoreCase(type)) {
            return isHeartRateAnomaly(vitalData);
        }

        if ("SpO2".equalsIgnoreCase(type)) {
            return isSpo2Anomaly(vitalData);
        }

        if ("Blood Pressure".equalsIgnoreCase(type)) {
            return isBloodPressureAnomaly(vitalData);
        }

        if ("Temperature".equalsIgnoreCase(type)) {
            return isTemperatureAnomaly(vitalData);
        }

        return false;
    }

    private boolean isHeartRateAnomaly(VitalData vitalData) {

        if (vitalData.getValue() == null) {
            return false;
        }

        double heartRate = Double.parseDouble(vitalData.getValue());

        return heartRate > HEART_RATE_HIGH || heartRate < HEART_RATE_LOW;
    }

    private boolean isSpo2Anomaly(VitalData vitalData) {

        if (vitalData.getValue() == null) {
            return false;
        }

        double spo2 = Double.parseDouble(vitalData.getValue());

        return spo2 < SPO2_LOW;
    }

    private boolean isBloodPressureAnomaly(VitalData vitalData) {

        Double systolic = vitalData.getSystolicBP();
        Double diastolic = vitalData.getDiastolicBP();

        if (systolic == null || diastolic == null) {
            return false;
        }

        return systolic > SYSTOLIC_HIGH || diastolic > DIASTOLIC_HIGH;
    }

    private boolean isTemperatureAnomaly(VitalData vitalData) {

        if (vitalData.getValue() == null) {
            return false;
        }

        double temperature = Double.parseDouble(vitalData.getValue());

        return temperature > TEMPERATURE_HIGH || temperature < TEMPERATURE_LOW;
    }
}