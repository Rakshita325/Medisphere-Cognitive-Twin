package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vital_records")
public class VitalRecord {

    @Id
    private String id;

    private String patientId;
    private String type;
    private String value;
    private String unit;
    private double heartRate;
    private double systolicBP;
    private double diastolicBP;
    private double spo2;
    private double temperature;
    private Double systolic;
    private Double diastolic;
    private String recordedAt;
    private java.time.Instant createdAt;
    private String source;

    public VitalRecord() {
    }

    public VitalRecord(String patientId,
                       double heartRate,
                       double systolicBP,
                       double diastolicBP,
                       double spo2,
                       double temperature) {

        this.patientId = patientId;
        this.heartRate = heartRate;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.spo2 = spo2;
        this.temperature = temperature;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getType() { return type; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public Double getSystolic() { return systolic; }
    public Double getDiastolic() { return diastolic; }
    public String getRecordedAt() { return recordedAt; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public String getSource() { return source; }

    public double getHeartRate() {
        return heartRate;
    }

    public double getSystolicBP() {
        return systolicBP;
    }

    public double getDiastolicBP() {
        return diastolicBP;
    }

    public double getSpo2() {
        return spo2;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setType(String type) { this.type = type; }
    public void setValue(String value) { this.value = value; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setSystolic(Double systolic) { this.systolic = systolic; }
    public void setDiastolic(Double diastolic) { this.diastolic = diastolic; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public void setSource(String source) { this.source = source; }

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    public void setSystolicBP(double systolicBP) {
        this.systolicBP = systolicBP;
    }

    public void setDiastolicBP(double diastolicBP) {
        this.diastolicBP = diastolicBP;
    }

    public void setSpo2(double spo2) {
        this.spo2 = spo2;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}