package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "treatment_tracking")
public class TreatmentTracking {

    @Id
    private String id;

    private String interventionId;
    private String patientId;
    private String patientName;
    private String carePlanId;
    private String interventionType; // MEDICATION, VITAL_MONITORING, GLUCOSE_MONITORING, DIET, EXERCISE, GENERAL
    private String description;
    private String frequency; // Daily, Twice Daily, Weekly, As Directed
    private String dueDate; // e.g. "Today", "2026-09-27"
    private String status; // PENDING, IN_PROGRESS, COMPLETED, MISSED, CANCELLED
    private Instant completedAt;
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    public TreatmentTracking() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = "PENDING";
    }

    public TreatmentTracking(String interventionId, String patientId, String patientName,
                             String carePlanId, String interventionType, String description,
                             String frequency, String dueDate, String status, String notes) {
        this.interventionId = interventionId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.carePlanId = carePlanId;
        this.interventionType = interventionType != null ? interventionType : "GENERAL";
        this.description = description;
        this.frequency = frequency != null ? frequency : "Daily";
        this.dueDate = dueDate != null ? dueDate : "Today";
        this.status = status != null ? status.toUpperCase() : "PENDING";
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if ("COMPLETED".equalsIgnoreCase(this.status)) {
            this.completedAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterventionId() {
        return interventionId;
    }

    public void setInterventionId(String interventionId) {
        this.interventionId = interventionId;
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

    public String getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(String carePlanId) {
        this.carePlanId = carePlanId;
    }

    public String getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(String interventionType) {
        this.interventionType = interventionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.toUpperCase() : "PENDING";
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
