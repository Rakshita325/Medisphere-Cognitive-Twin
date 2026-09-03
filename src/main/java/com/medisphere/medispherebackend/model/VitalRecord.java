package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vital_records")
public class VitalRecord {

    @Id
    private String id;

    private String patientId;
    private double heartRate;
    private double systolicBP;
    private double diastolicBP;
    private double spo2;
    private double temperature;

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