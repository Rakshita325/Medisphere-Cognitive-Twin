package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.MetricProgressDto;
import com.medisphere.medispherebackend.dto.PatientProgressResponseDto;
import com.medisphere.medispherebackend.model.HealthMeasurement;
import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.HealthMeasurementRepository;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Milestone 4 Part 4: Progress / Improvement Service
 *
 * Compares a patient's earlier/baseline health measurements with their latest
 * recorded measurements stored in MongoDB.
 *
 * Derives progress dynamically:
 *   - Baseline = earlier/reference measurement (ordered by recordedAt timestamp)
 *   - Latest   = most recent measurement (ordered by recordedAt timestamp)
 *   - Change   = Latest - Baseline
 *   - Status   = IMPROVED, WORSENED, STABLE based on evidence-backed clinical direction rules
 *   - Insufficient data = "Not enough historical data for comparison."
 *   - Follow-up indication = "Follow-up may be required." if unfavorable movement.
 */
@Service
public class PatientProgressService {

    private static final Logger log = LoggerFactory.getLogger(PatientProgressService.class);

    private final HealthMeasurementRepository healthMeasurementRepository;
    private final VitalRecordRepository vitalRecordRepository;

    // Preferred clinical ordering for display
    private static final List<String> ORDERED_METRICS = List.of(
            "Blood Pressure",
            "Blood Glucose",
            "HbA1c",
            "Heart Rate",
            "SpO2",
            "Weight",
            "Cholesterol",
            "Hemoglobin",
            "Creatinine",
            "TSH",
            "Temperature"
    );

    public PatientProgressService(
            HealthMeasurementRepository healthMeasurementRepository,
            @Autowired(required = false) VitalRecordRepository vitalRecordRepository) {
        this.healthMeasurementRepository = healthMeasurementRepository;
        this.vitalRecordRepository = vitalRecordRepository;
    }

    /**
     * Calculate patient-specific health progress from MongoDB stored measurements.
     *
     * @param patientId ID of the patient (e.g. "P001", "P002")
     * @return PatientProgressResponseDto containing comparisons for each metric
     */
    public PatientProgressResponseDto calculateProgress(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            PatientProgressResponseDto empty = new PatientProgressResponseDto(patientId);
            empty.setHasHistoricalData(false);
            empty.setHasAnyComparison(false);
            empty.setSummary("Patient ID is required.");
            empty.setComputedAt(Instant.now().toString());
            return empty;
        }

        final String normalizedPid = patientId.trim();
        PatientProgressResponseDto response = new PatientProgressResponseDto(normalizedPid);
        response.setComputedAt(Instant.now().toString());

        // 1. Retrieve all health measurements for this patient
        List<HealthMeasurement> measurements = healthMeasurementRepository.findByPatientIdOrderByCreatedAtDesc(normalizedPid);

        // Group measurements by canonical metric name
        Map<String, List<HealthMeasurement>> grouped = new LinkedHashMap<>();

        if (measurements != null) {
            for (HealthMeasurement m : measurements) {
                if (m.getMeasurementType() == null || m.getMeasurementType().isBlank()) continue;
                String canonical = canonicalMetricName(m.getMeasurementType());
                grouped.computeIfAbsent(canonical, k -> new ArrayList<>()).add(m);
            }
        }

        // Also check if any vital records exist for metrics not already present
        if (vitalRecordRepository != null) {
            try {
                List<VitalRecord> vitals = vitalRecordRepository.findByPatientId(normalizedPid);
                if (vitals != null && !vitals.isEmpty()) {
                    incorporateVitalRecords(vitals, grouped, normalizedPid);
                }
            } catch (Exception e) {
                log.debug("VitalRecord lookup notice: {}", e.getMessage());
            }
        }

        if (grouped.isEmpty()) {
            response.setHasHistoricalData(false);
            response.setHasAnyComparison(false);
            response.setSummary("Not enough historical data for comparison.");
            return response;
        }

        response.setHasHistoricalData(true);

        List<MetricProgressDto> metricResults = new ArrayList<>();
        boolean hasAnyComparison = false;
        boolean followUpRequired = false;

        // Process in preferred clinical order
        Set<String> processedTypes = new HashSet<>();

        for (String metricName : ORDERED_METRICS) {
            if (grouped.containsKey(metricName)) {
                MetricProgressDto dto = evaluateMetricProgress(metricName, grouped.get(metricName));
                metricResults.add(dto);
                processedTypes.add(metricName);
                if (dto.isHasEnoughData()) {
                    hasAnyComparison = true;
                }
                if ("WORSENED".equalsIgnoreCase(dto.getStatus())) {
                    followUpRequired = true;
                }
            }
        }

        // Any remaining custom or lab metrics
        for (Map.Entry<String, List<HealthMeasurement>> entry : grouped.entrySet()) {
            if (!processedTypes.contains(entry.getKey())) {
                MetricProgressDto dto = evaluateMetricProgress(entry.getKey(), entry.getValue());
                metricResults.add(dto);
                if (dto.isHasEnoughData()) {
                    hasAnyComparison = true;
                }
                if ("WORSENED".equalsIgnoreCase(dto.getStatus())) {
                    followUpRequired = true;
                }
            }
        }

        response.setMetrics(metricResults);
        response.setHasAnyComparison(hasAnyComparison);
        response.setFollowUpRequired(followUpRequired);

        if (followUpRequired) {
            response.setFollowUpMessage("Follow-up may be required.");
        }

        if (!hasAnyComparison) {
            response.setSummary("Not enough historical data for comparison.");
        } else {
            long improved = metricResults.stream().filter(m -> "IMPROVED".equalsIgnoreCase(m.getStatus())).count();
            long worsened = metricResults.stream().filter(m -> "WORSENED".equalsIgnoreCase(m.getStatus())).count();
            long stable = metricResults.stream().filter(m -> "STABLE".equalsIgnoreCase(m.getStatus())).count();

            response.setSummary(String.format("%d improved, %d stable, %d worsened", improved, stable, worsened));
        }

        return response;
    }

    /**
     * Evaluates baseline, latest, change, and status for a single metric's measurement history.
     */
    private MetricProgressDto evaluateMetricProgress(String metricName, List<HealthMeasurement> rawList) {
        MetricProgressDto dto = new MetricProgressDto();
        dto.setMetric(metricName);
        dto.setType(toSlug(metricName));
        dto.setHistoricalCount(rawList.size());

        boolean isBP = "Blood Pressure".equalsIgnoreCase(metricName);
        dto.setBP(isBP);

        // Sort chronologically by recordedAt ascending (oldest first, newest last)
        List<HealthMeasurement> sorted = new ArrayList<>(rawList);
        sorted.sort(Comparator.comparing(m -> parseTimestamp(m.getRecordedAt(), m.getCreatedAt())));

        String defaultUnit = rawList.stream()
                .map(HealthMeasurement::getUnit)
                .filter(u -> u != null && !u.isBlank())
                .findFirst()
                .orElse(defaultUnitFor(metricName));
        dto.setUnit(defaultUnit);

        HealthMeasurement latestRecord = sorted.get(sorted.size() - 1);
        dto.setLatestRecordedAt(latestRecord.getRecordedAt() != null ? latestRecord.getRecordedAt() :
                (latestRecord.getCreatedAt() != null ? latestRecord.getCreatedAt().toString() : ""));

        // If only one measurement exists: insufficient historical data
        if (sorted.size() < 2) {
            dto.setHasEnoughData(false);
            dto.setMessage("Not enough historical data for comparison.");

            if (isBP) {
                Double sys = latestRecord.getSystolic();
                Double dia = latestRecord.getDiastolic();
                dto.setLatest(formatBP(sys, dia));
                dto.setLatestSystolic(sys);
                dto.setLatestDiastolic(dia);
            } else {
                dto.setLatest(latestRecord.getValue());
            }
            return dto;
        }

        // Two or more measurements exist: baseline is earliest, latest is newest
        HealthMeasurement baselineRecord = sorted.get(0);
        dto.setBaselineRecordedAt(baselineRecord.getRecordedAt() != null ? baselineRecord.getRecordedAt() :
                (baselineRecord.getCreatedAt() != null ? baselineRecord.getCreatedAt().toString() : ""));

        dto.setHasEnoughData(true);

        if (isBP) {
            Double bSys = baselineRecord.getSystolic();
            Double bDia = baselineRecord.getDiastolic();
            Double lSys = latestRecord.getSystolic();
            Double lDia = latestRecord.getDiastolic();

            if (bSys == null || bDia == null || lSys == null || lDia == null) {
                dto.setHasEnoughData(false);
                dto.setMessage("Not enough historical data for comparison.");
                return dto;
            }

            dto.setBaseline(formatBP(bSys, bDia));
            dto.setLatest(formatBP(lSys, lDia));
            dto.setBaselineSystolic(bSys);
            dto.setBaselineDiastolic(bDia);
            dto.setLatestSystolic(lSys);
            dto.setLatestDiastolic(lDia);

            double sysChange = round(lSys - bSys, 1);
            double diaChange = round(lDia - bDia, 1);

            dto.setSystolicChange(sysChange);
            dto.setDiastolicChange(diaChange);
            dto.setChange(formatBPChange(sysChange, diaChange));

            dto.setStatus(determineBloodPressureStatus(bSys, bDia, lSys, lDia, sysChange, diaChange));
        } else {
            Double bVal = baselineRecord.getValue();
            Double lVal = latestRecord.getValue();

            if (bVal == null || lVal == null) {
                dto.setHasEnoughData(false);
                dto.setMessage("Not enough historical data for comparison.");
                return dto;
            }

            dto.setBaseline(round(bVal, 2));
            dto.setLatest(round(lVal, 2));

            double change = round(lVal - bVal, 2);
            dto.setNumericChange(change);
            dto.setChange(formatNumericChange(change));

            dto.setStatus(determineMetricStatus(metricName, bVal, lVal, change));
        }

        return dto;
    }

    /**
     * Evidence-backed clinical direction rules for blood pressure.
     */
    private String determineBloodPressureStatus(
            double bSys, double bDia, double lSys, double lDia,
            double sysChange, double diaChange) {

        // If completely unchanged
        if (sysChange == 0.0 && diaChange == 0.0) {
            return "STABLE";
        }

        // Within small stable threshold (±2 mmHg on both)
        if (Math.abs(sysChange) <= 2.0 && Math.abs(diaChange) <= 2.0) {
            return "STABLE";
        }

        // For hypertensive or elevated baseline: decrease is improved
        if (sysChange <= -2.0 && diaChange <= 0.0) {
            return "IMPROVED";
        }
        if (diaChange <= -2.0 && sysChange <= 0.0) {
            return "IMPROVED";
        }

        // Both or either worsening
        if (sysChange >= 2.0 && diaChange >= 0.0) {
            return "WORSENED";
        }
        if (diaChange >= 2.0 && sysChange >= 0.0) {
            return "WORSENED";
        }

        // Mixed movements (e.g. sys down 3, dia up 1)
        if (sysChange < 0 && diaChange > 0 && (Math.abs(sysChange) > diaChange)) {
            return "IMPROVED";
        }
        if (sysChange > 0 && diaChange < 0 && (sysChange > Math.abs(diaChange))) {
            return "WORSENED";
        }

        return "STABLE";
    }

    /**
     * Evidence-backed clinical direction rules for standard metrics.
     */
    private String determineMetricStatus(String metricName, double baseline, double latest, double change) {
        // Section 6: If effectively unchanged
        if (change == 0.0) {
            return "STABLE";
        }

        String lower = metricName.toLowerCase();

        if (lower.contains("glucose")) {
            // Blood Glucose (mg/dL): for diabetic/elevated baseline, lower is improved
            if (Math.abs(change) <= 2.0) return "STABLE";
            return change < 0 ? "IMPROVED" : "WORSENED";
        }

        if (lower.contains("hba1c")) {
            // HbA1c (%): lower is improved
            if (Math.abs(change) < 0.1) return "STABLE";
            return change < 0 ? "IMPROVED" : "WORSENED";
        }

        if (lower.contains("heart rate") || lower.equals("hr")) {
            // Target 60 - 80 bpm resting
            if (Math.abs(change) <= 2.0) return "STABLE";
            if (baseline > 80.0) {
                return change < 0 ? "IMPROVED" : "WORSENED";
            } else if (baseline < 60.0) {
                return change > 0 ? "IMPROVED" : "WORSENED";
            }
            return (latest >= 60.0 && latest <= 80.0) ? "STABLE" : "WORSENED";
        }

        if (lower.contains("spo2") || lower.contains("oxygen")) {
            // SpO2 (%): higher is improved
            if (Math.abs(change) < 1.0) return "STABLE";
            return change > 0 ? "IMPROVED" : "WORSENED";
        }

        if (lower.contains("cholesterol")) {
            // Cholesterol (mg/dL): lower is improved
            if (Math.abs(change) <= 2.0) return "STABLE";
            return change < 0 ? "IMPROVED" : "WORSENED";
        }

        if (lower.contains("creatinine")) {
            // Creatinine (mg/dL): lower is improved
            if (Math.abs(change) < 0.1) return "STABLE";
            return change < 0 ? "IMPROVED" : "WORSENED";
        }

        if (lower.contains("hemoglobin")) {
            // Hemoglobin (g/dL): if baseline was low (< 12), increase is improved
            if (Math.abs(change) < 0.2) return "STABLE";
            if (baseline < 12.0) {
                return change > 0 ? "IMPROVED" : "WORSENED";
            }
            return "STABLE";
        }

        if (lower.contains("weight")) {
            // Weight (kg): if overweight baseline (> 75kg), decrease is improved
            if (Math.abs(change) < 0.5) return "STABLE";
            if (baseline > 75.0) {
                return change < 0 ? "IMPROVED" : "WORSENED";
            }
            return "STABLE";
        }

        if (lower.contains("temperature")) {
            // Temperature: if fever (> 37.5 C / 99.5 F), decrease towards normal is improved
            if (Math.abs(change) < 0.2) return "STABLE";
            if (baseline > 37.5) {
                return change < 0 ? "IMPROVED" : "WORSENED";
            }
            return "STABLE";
        }

        // Generic fallback where no strict direction rule exists:
        // Do not make unsupported medical conclusion.
        return null;
    }

    /**
     * Incorporates VitalRecord entities if the patient has them in vital_records collection.
     */
    private void incorporateVitalRecords(
            List<VitalRecord> vitals,
            Map<String, List<HealthMeasurement>> grouped,
            String patientId) {

        for (VitalRecord vr : vitals) {
            // If BP record
            if (vr.getSystolicBP() > 0 && vr.getDiastolicBP() > 0) {
                HealthMeasurement hm = new HealthMeasurement();
                hm.setPatientId(patientId);
                hm.setMeasurementType("Blood Pressure");
                hm.setSystolic(vr.getSystolicBP());
                hm.setDiastolic(vr.getDiastolicBP());
                hm.setUnit("mmHg");
                hm.setRecordedAt(vr.getRecordedAt());
                hm.setCreatedAt(vr.getCreatedAt());
                hm.setSource("VITAL_RECORD");
                grouped.computeIfAbsent("Blood Pressure", k -> new ArrayList<>()).add(hm);
            } else if (vr.getSystolic() != null && vr.getDiastolic() != null) {
                HealthMeasurement hm = new HealthMeasurement();
                hm.setPatientId(patientId);
                hm.setMeasurementType("Blood Pressure");
                hm.setSystolic(vr.getSystolic());
                hm.setDiastolic(vr.getDiastolic());
                hm.setUnit("mmHg");
                hm.setRecordedAt(vr.getRecordedAt());
                hm.setCreatedAt(vr.getCreatedAt());
                hm.setSource("VITAL_RECORD");
                grouped.computeIfAbsent("Blood Pressure", k -> new ArrayList<>()).add(hm);
            }

            // If Heart Rate
            if (vr.getHeartRate() > 0) {
                HealthMeasurement hm = new HealthMeasurement();
                hm.setPatientId(patientId);
                hm.setMeasurementType("Heart Rate");
                hm.setValue(vr.getHeartRate());
                hm.setUnit("BPM");
                hm.setRecordedAt(vr.getRecordedAt());
                hm.setCreatedAt(vr.getCreatedAt());
                hm.setSource("VITAL_RECORD");
                grouped.computeIfAbsent("Heart Rate", k -> new ArrayList<>()).add(hm);
            }

            // If SpO2
            if (vr.getSpo2() > 0) {
                HealthMeasurement hm = new HealthMeasurement();
                hm.setPatientId(patientId);
                hm.setMeasurementType("SpO2");
                hm.setValue(vr.getSpo2());
                hm.setUnit("%");
                hm.setRecordedAt(vr.getRecordedAt());
                hm.setCreatedAt(vr.getCreatedAt());
                hm.setSource("VITAL_RECORD");
                grouped.computeIfAbsent("SpO2", k -> new ArrayList<>()).add(hm);
            }

            // If Temperature
            if (vr.getTemperature() > 0) {
                HealthMeasurement hm = new HealthMeasurement();
                hm.setPatientId(patientId);
                hm.setMeasurementType("Temperature");
                hm.setValue(vr.getTemperature());
                hm.setUnit("°C");
                hm.setRecordedAt(vr.getRecordedAt());
                hm.setCreatedAt(vr.getCreatedAt());
                hm.setSource("VITAL_RECORD");
                grouped.computeIfAbsent("Temperature", k -> new ArrayList<>()).add(hm);
            }
        }
    }

    private Instant parseTimestamp(String timestamp, Instant fallback) {
        if (timestamp == null || timestamp.isBlank()) {
            return fallback != null ? fallback : Instant.EPOCH;
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(timestamp).atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(timestamp).atStartOfDay(ZoneId.systemDefault()).toInstant();
                } catch (Exception e3) {
                    return fallback != null ? fallback : Instant.EPOCH;
                }
            }
        }
    }

    private String canonicalMetricName(String type) {
        if (type == null) return "Unknown";
        String trim = type.trim();
        if (trim.equalsIgnoreCase("bp") || trim.equalsIgnoreCase("blood_pressure") || trim.equalsIgnoreCase("blood pressure")) {
            return "Blood Pressure";
        }
        if (trim.equalsIgnoreCase("glucose") || trim.equalsIgnoreCase("blood glucose") || trim.equalsIgnoreCase("blood_glucose")) {
            return "Blood Glucose";
        }
        if (trim.equalsIgnoreCase("hba1c") || trim.equalsIgnoreCase("hemoglobin a1c")) {
            return "HbA1c";
        }
        if (trim.equalsIgnoreCase("hr") || trim.equalsIgnoreCase("heart rate") || trim.equalsIgnoreCase("heart_rate")) {
            return "Heart Rate";
        }
        if (trim.equalsIgnoreCase("spo2") || trim.equalsIgnoreCase("oxygen saturation")) {
            return "SpO2";
        }
        if (trim.equalsIgnoreCase("weight")) {
            return "Weight";
        }
        if (trim.equalsIgnoreCase("cholesterol") || trim.equalsIgnoreCase("total cholesterol")) {
            return "Cholesterol";
        }
        return trim;
    }

    private String toSlug(String metricName) {
        if (metricName == null) return "unknown";
        String s = metricName.toLowerCase().replace(" ", "_");
        if (s.equals("blood_glucose")) return "glucose";
        return s;
    }

    private String defaultUnitFor(String metricName) {
        switch (metricName) {
            case "Blood Pressure": return "mmHg";
            case "Blood Glucose": return "mg/dL";
            case "HbA1c": return "%";
            case "Heart Rate": return "BPM";
            case "SpO2": return "%";
            case "Weight": return "kg";
            case "Cholesterol": return "mg/dL";
            case "Hemoglobin": return "g/dL";
            case "Creatinine": return "mg/dL";
            case "TSH": return "mIU/L";
            case "Temperature": return "°C";
            default: return "";
        }
    }

    private String formatBP(Double systolic, Double diastolic) {
        if (systolic == null || diastolic == null) return "—";
        String s = (systolic == Math.floor(systolic)) ? String.valueOf(systolic.intValue()) : String.valueOf(systolic);
        String d = (diastolic == Math.floor(diastolic)) ? String.valueOf(diastolic.intValue()) : String.valueOf(diastolic);
        return s + "/" + d;
    }

    private String formatBPChange(double sysChange, double diaChange) {
        String s = formatSigned(sysChange);
        String d = formatSigned(diaChange);
        return s + "/" + d;
    }

    private String formatSigned(double val) {
        String num = (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
        return num;
    }

    private Object formatNumericChange(double change) {
        if (change == Math.floor(change)) {
            return (long) change;
        }
        return change;
    }

    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
