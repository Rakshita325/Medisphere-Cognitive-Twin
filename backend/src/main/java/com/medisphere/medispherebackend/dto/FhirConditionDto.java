package com.medisphere.medispherebackend.dto;

public class FhirConditionDto {
    private String id;
    private String patientId;
    private String name;
    private String status;
    private String diagnosedDate;

    public FhirConditionDto() {
    }

    public FhirConditionDto(String id, String patientId, String name, String status, String diagnosedDate) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.status = status;
        this.diagnosedDate = diagnosedDate;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getDiagnosedDate() { return diagnosedDate; }
    public void setId(String id) { this.id = id; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
    public void setDiagnosedDate(String diagnosedDate) { this.diagnosedDate = diagnosedDate; }
}
