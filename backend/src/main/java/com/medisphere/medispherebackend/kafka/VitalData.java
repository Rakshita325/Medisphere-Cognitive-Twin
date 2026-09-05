package com.medisphere.medispherebackend.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;

public class VitalData {

    private String patientId;
    private String type;
    private String value;
    private String unit;
    private String recordedAt;

    @JsonAlias("systolic")
    private Double systolicBP;

    @JsonAlias("diastolic")
    private Double diastolicBP;

    public VitalData() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Double getSystolicBP() {
        return systolicBP;
    }

    public void setSystolicBP(Double systolicBP) {
        this.systolicBP = systolicBP;
    }

    public Double getDiastolicBP() {
        return diastolicBP;
    }

    public void setDiastolicBP(Double diastolicBP) {
        this.diastolicBP = diastolicBP;
    }
}