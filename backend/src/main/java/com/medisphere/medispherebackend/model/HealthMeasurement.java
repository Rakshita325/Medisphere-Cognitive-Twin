package com.medisphere.medispherebackend.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Milestone 4 Part 3: Health Monitoring
 *
 * Represents a single patient health measurement recorded by a clinician.
 * Stored in a separate "health_measurements" collection to preserve
 * baseline data in patient-data.json and the Kafka-driven vital_records pipeline.
 *
 * Each measurement is an independent document with its own timestamp,
 * enabling Part 4 baseline-vs-latest comparison without overwriting originals.
 */
@Document(collection = "health_measurements")
public class HealthMeasurement {

    @Id
    private String id;

    @NotBlank
    private String patientId;

    @NotBlank
    private String measurementType;

    private Double value;

    private String unit;

    // Blood Pressure specific fields
    private Double systolic;
    private Double diastolic;

    @NotBlank
    private String recordedAt;

    private Instant createdAt;

    private String source;

    private String notes;

    public HealthMeasurement() {
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getMeasurementType() {
        return measurementType;
    }

    public Double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Double getSystolic() {
        return systolic;
    }

    public Double getDiastolic() {
        return diastolic;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSource() {
        return source;
    }

    public String getNotes() {
        return notes;
    }

    // --- Setters ---

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setMeasurementType(String measurementType) {
        this.measurementType = measurementType;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setSystolic(Double systolic) {
        this.systolic = systolic;
    }

    public void setDiastolic(Double diastolic) {
        this.diastolic = diastolic;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
