package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.kafka.VitalData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AnomalyDetectionValidationTest {

    private final AnomalyDetectionService anomalyDetectionService = new AnomalyDetectionService();

    private static class MetricTracker {
        int total = 0;
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;

        void record(boolean expected, boolean predicted) {
            total++;
            if (expected && predicted) {
                tp++;
            } else if (!expected && predicted) {
                fp++;
            } else if (!expected && !predicted) {
                tn++;
            } else {
                fn++;
            }
        }

        double getPrecision() {
            int predictedPositive = tp + fp;
            return predictedPositive == 0 ? 0.0 : ((double) tp / predictedPositive) * 100.0;
        }

        double getFalseAlertRate() {
            // Formula: FP / (TP + FP) * 100 as specified in the project requirements
            int predictedPositive = tp + fp;
            return predictedPositive == 0 ? 0.0 : ((double) fp / predictedPositive) * 100.0;
        }

        double getAccuracy() {
            return total == 0 ? 0.0 : (((double) (tp + tn)) / total) * 100.0;
        }
    }

    @Test
    @DisplayName("Validate AnomalyDetectionService against labeled validation dataset")
    void testAnomalyDetectionValidation() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("anomaly-validation.csv");
        assertNotNull(inputStream, "anomaly-validation.csv test resource must be present");

        MetricTracker overall = new MetricTracker();
        Map<String, MetricTracker> byVital = new LinkedHashMap<>();
        byVital.put("Heart Rate", new MetricTracker());
        byVital.put("SpO2", new MetricTracker());
        byVital.put("Blood Pressure", new MetricTracker());
        byVital.put("Temperature", new MetricTracker());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 7) {
                    continue;
                }

                String patientId = parts[0].trim();
                String type = parts[1].trim();
                String value = parts[2].trim().isEmpty() ? null : parts[2].trim();
                String unit = parts[3].trim().isEmpty() ? null : parts[3].trim();
                Double systolicBP = parts[4].trim().isEmpty() ? null : Double.parseDouble(parts[4].trim());
                Double diastolicBP = parts[5].trim().isEmpty() ? null : Double.parseDouble(parts[5].trim());
                boolean expectedAnomaly = Boolean.parseBoolean(parts[6].trim());

                VitalData vitalData = new VitalData();
                vitalData.setPatientId(patientId);
                vitalData.setType(type);
                vitalData.setValue(value);
                vitalData.setUnit(unit);
                vitalData.setSystolicBP(systolicBP);
                vitalData.setDiastolicBP(diastolicBP);

                boolean predictedAnomaly = anomalyDetectionService.isAnomaly(vitalData);

                overall.record(expectedAnomaly, predictedAnomaly);
                if (byVital.containsKey(type)) {
                    byVital.get(type).record(expectedAnomaly, predictedAnomaly);
                }
            }
        }

        // Print formatted validation report
        System.out.println();
        System.out.println("========================================");
        System.out.println("MILESTONE 3 ANOMALY VALIDATION");
        System.out.println("========================================");
        System.out.println("Total Samples: " + overall.total);
        System.out.println();
        System.out.println("True Positives (TP):  " + overall.tp);
        System.out.println("False Positives (FP): " + overall.fp);
        System.out.println("True Negatives (TN):  " + overall.tn);
        System.out.println("False Negatives (FN): " + overall.fn);
        System.out.println();
        System.out.printf("Precision:        %.2f %%\n", overall.getPrecision());
        System.out.printf("False Alert Rate: %.2f %% (Definition: FP / (TP + FP) * 100)\n", overall.getFalseAlertRate());
        System.out.printf("Accuracy:         %.2f %%\n", overall.getAccuracy());
        System.out.println("========================================");
        System.out.println("BREAKDOWN BY VITAL TYPE");
        System.out.println("========================================");

        for (Map.Entry<String, MetricTracker> entry : byVital.entrySet()) {
            String vitalType = entry.getKey();
            MetricTracker tracker = entry.getValue();
            System.out.println(vitalType);
            System.out.println("  Samples: " + tracker.total);
            System.out.printf("  TP: %d | FP: %d | TN: %d | FN: %d\n", tracker.tp, tracker.fp, tracker.tn, tracker.fn);
            System.out.printf("  Precision:        %.2f %%\n", tracker.getPrecision());
            System.out.printf("  False Alert Rate: %.2f %%\n", tracker.getFalseAlertRate());
            System.out.printf("  Accuracy:         %.2f %%\n", tracker.getAccuracy());
            System.out.println();
        }
        System.out.println("========================================");

        // Verification assertions
        assertTrue(overall.total >= 100, "Validation dataset must contain at least 100 samples");
        assertEquals(120, overall.total, "Expected exactly 120 samples across 4 vital types");
    }
}
