package com.medisphere.medispherebackend.dto;

public class FhirObservationDto {
    private String id;
    private String patientId;
    private String type;
    private String code;
    private String value;
    private String unit;
    private String recordedAt;
    private Double systolic;
    private Double diastolic;

    public FhirObservationDto() {
    }

    public FhirObservationDto(String id, String patientId, String type, String code, String value,
                               String unit, String recordedAt, Double systolic, Double diastolic) {
        this.id = id;
        this.patientId = patientId;
        this.type = type;
        this.code = code;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getType() { return type; }
    public String getCode() { return code; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public String getRecordedAt() { return recordedAt; }
    public Double getSystolic() { return systolic; }
    public Double getDiastolic() { return diastolic; }
    public void setId(String id) { this.id = id; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setType(String type) { this.type = type; }
    public void setCode(String code) { this.code = code; }
    public void setValue(String value) { this.value = value; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
    public void setSystolic(Double systolic) { this.systolic = systolic; }
    public void setDiastolic(Double diastolic) { this.diastolic = diastolic; }
}
