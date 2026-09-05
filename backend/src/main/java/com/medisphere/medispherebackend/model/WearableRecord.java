package com.medisphere.medispherebackend.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "wearable_records")
public class WearableRecord {
    @Id
    private String id;
    @NotBlank
    private String patientId;
    @Min(0)
    private int steps;
    @Min(0)
    private double sleepHours;
    private String recordedAt;
    private Instant createdAt;

    public WearableRecord() { }
    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public int getSteps() { return steps; }
    public double getSleepHours() { return sleepHours; }
    public String getRecordedAt() { return recordedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setSteps(int steps) { this.steps = steps; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
