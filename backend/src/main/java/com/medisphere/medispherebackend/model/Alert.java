
package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    private String patientId;
    private String patientName;

    private String vitalType;
    private Double value;
    private String unit;

    private String severity;
    private String message;

    private Instant detectedAt;

    private String status;
    private String recommendedAction;
    private String ruleType;
    
    private String assignedDoctorRole;
    private String notificationStatus;

    public Alert() {
    }

    public Alert(
            String patientId,
            String patientName,
            String vitalType,
            Double value,
            String unit,
            String severity,
            String message,
            Instant detectedAt,
            String status) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.vitalType = vitalType;
        this.value = value;
        this.unit = unit;
        this.severity = severity;
        this.message = message;
        this.detectedAt = detectedAt;
        this.status = status;
    }

    public Alert(
            String patientId,
            String patientName,
            String vitalType,
            Double value,
            String unit,
            String severity,
            String message,
            Instant detectedAt,
            String status,
            String recommendedAction,
            String ruleType) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.vitalType = vitalType;
        this.value = value;
        this.unit = unit;
        this.severity = severity;
        this.message = message;
        this.detectedAt = detectedAt;
        this.status = status;
        this.recommendedAction = recommendedAction;
        this.ruleType = ruleType;
    }

    public Alert(
            String patientId,
            String patientName,
            String vitalType,
            Double value,
            String unit,
            String severity,
            String message,
            Instant detectedAt,
            String status,
            String recommendedAction,
            String ruleType,
            String assignedDoctorRole,
            String notificationStatus) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.vitalType = vitalType;
        this.value = value;
        this.unit = unit;
        this.severity = severity;
        this.message = message;
        this.detectedAt = detectedAt;
        this.status = status;
        this.recommendedAction = recommendedAction;
        this.ruleType = ruleType;
        this.assignedDoctorRole = assignedDoctorRole;
        this.notificationStatus = notificationStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getVitalType() {
        return vitalType;
    }

    public void setVitalType(String vitalType) {
        this.vitalType = vitalType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getAssignedDoctorRole() {
        return assignedDoctorRole;
    }

    public void setAssignedDoctorRole(String assignedDoctorRole) {
        this.assignedDoctorRole = assignedDoctorRole;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }
}
