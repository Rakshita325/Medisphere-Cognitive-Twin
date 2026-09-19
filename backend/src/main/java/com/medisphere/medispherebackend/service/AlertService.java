package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.kafka.VitalData;
import com.medisphere.medispherebackend.model.Alert;
import com.medisphere.medispherebackend.repository.AlertRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(
            AlertRepository alertRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.alertRepository = alertRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates an alert for an abnormal vital.
     */
    public Optional<Alert> createAlert(VitalData vitalData) {

        if (vitalData == null || vitalData.getType() == null) {
            return Optional.empty();
        }

        String patientId = vitalData.getPatientId();
        String vitalType = vitalData.getType();

        if (patientId == null || patientId.isBlank()) {
            return Optional.empty();
        }

        // Prevent duplicate ACTIVE alerts for the same patient and vital.
        Optional<Alert> existingAlert = alertRepository
                .findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc(
                        patientId,
                        vitalType,
                        "ACTIVE");

        if (existingAlert.isPresent()) {
            System.out.println(
                    "Existing ACTIVE alert found for patient "
                            + patientId
                            + " and vital "
                            + vitalType
                            + ". Duplicate alert skipped.");

            return existingAlert;
        }

        Double value = getVitalValue(vitalData);
        String unit = getUnit(vitalData);
        String severity = determineSeverity(vitalData);
        String message = createMessage(vitalData, value, unit);

        Alert alert = new Alert(
                patientId,
                getPatientName(vitalData),
                vitalType,
                value,
                unit,
                severity,
                message,
                Instant.now(),
                "ACTIVE");

        Alert savedAlert = alertRepository.save(alert);

        // Send the newly created alert to connected React clients.
        messagingTemplate.convertAndSend(
                "/topic/alerts",
                savedAlert);

        System.out.println(
                "ALERT CREATED | Patient: "
                        + savedAlert.getPatientId()
                        + " | Vital: "
                        + savedAlert.getVitalType()
                        + " | Value: "
                        + savedAlert.getValue()
                        + " "
                        + savedAlert.getUnit()
                        + " | Severity: "
                        + savedAlert.getSeverity());

        return Optional.of(savedAlert);
    }

    /**
     * Returns all alerts.
     */
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    /**
     * Returns active alerts, newest first.
     */
    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByDetectedAtDesc("ACTIVE");
    }

    /**
     * Returns alerts for a specific patient.
     */
    public List<Alert> getPatientAlerts(String patientId) {
        return alertRepository.findByPatientIdOrderByDetectedAtDesc(patientId);
    }

    /**
     * Acknowledges an alert.
     */
    public Optional<Alert> acknowledgeAlert(String alertId) {

        Optional<Alert> alertOptional = alertRepository.findById(alertId);

        if (alertOptional.isEmpty()) {
            return Optional.empty();
        }

        Alert alert = alertOptional.get();
        alert.setStatus("ACKNOWLEDGED");

        Alert updatedAlert = alertRepository.save(alert);

        // Notify connected React clients about the updated alert.
        messagingTemplate.convertAndSend(
                "/topic/alerts",
                updatedAlert);

        return Optional.of(updatedAlert);
    }

    /**
     * Converts VitalData's String value into Double.
     */
    private Double getVitalValue(VitalData vitalData) {

        String value = vitalData.getValue();

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            System.out.println(
                    "Unable to convert vital value to number: " + value);
            return null;
        }
    }

    /**
     * Returns the unit for the vital.
     */
    private String getUnit(VitalData vitalData) {

        if (vitalData.getUnit() != null && !vitalData.getUnit().isBlank()) {
            return vitalData.getUnit();
        }

        String type = vitalData.getType();

        if ("Heart Rate".equalsIgnoreCase(type)) {
            return "BPM";
        }

        if ("SpO2".equalsIgnoreCase(type)) {
            return "%";
        }

        if ("Temperature".equalsIgnoreCase(type)) {
            return "°C";
        }

        if ("Blood Pressure".equalsIgnoreCase(type)) {
            return "mmHg";
        }

        return "";
    }

    /**
     * Determines alert severity.
     */
    private String determineSeverity(VitalData vitalData) {

        String type = vitalData.getType();
        Double value = getVitalValue(vitalData);

        if (value == null) {
            return "WARNING";
        }

        if ("SpO2".equalsIgnoreCase(type)) {

            if (value < 88) {
                return "CRITICAL";
            }

            return "HIGH";
        }

        if ("Heart Rate".equalsIgnoreCase(type)) {

            if (value > 150 || value < 40) {
                return "CRITICAL";
            }

            return "HIGH";
        }

        if ("Temperature".equalsIgnoreCase(type)) {

            if (value > 39 || value < 34) {
                return "CRITICAL";
            }

            return "HIGH";
        }

        if ("Blood Pressure".equalsIgnoreCase(type)) {

            if (value > 180) {
                return "CRITICAL";
            }

            return "HIGH";
        }

        return "WARNING";
    }

    /**
     * Creates a readable alert message.
     */
    private String createMessage(
            VitalData vitalData,
            Double value,
            String unit) {

        String type = vitalData.getType();

        if ("SpO2".equalsIgnoreCase(type)) {
            return "Low SpO2 detected: " + value + " " + unit;
        }

        if ("Heart Rate".equalsIgnoreCase(type)) {
            return "Abnormal heart rate detected: " + value + " " + unit;
        }

        if ("Temperature".equalsIgnoreCase(type)) {
            return "Abnormal temperature detected: " + value + " " + unit;
        }

        if ("Blood Pressure".equalsIgnoreCase(type)) {
            return "Abnormal blood pressure detected: " + value + " " + unit;
        }

        return "Abnormal " + type + " detected: " + value + " " + unit;
    }

    /**
     * Returns the patient name.
     *
     * Uses patient ID as fallback if VitalData has no patientName.
     */
    private String getPatientName(VitalData vitalData) {
        return vitalData.getPatientId();
    }
}