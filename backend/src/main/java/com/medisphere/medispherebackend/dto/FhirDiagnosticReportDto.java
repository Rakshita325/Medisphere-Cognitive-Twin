package com.medisphere.medispherebackend.dto;

import java.util.List;

public class FhirDiagnosticReportDto {
    private String id;
    private String patientId;
    private String reportId;
    private String name;
    private String status;
    private String issuedDate;
    private List<String> resultObservationIds;

    public FhirDiagnosticReportDto() {
    }

    public FhirDiagnosticReportDto(String id, String patientId, String reportId, String name,
                                    String status, String issuedDate, List<String> resultObservationIds) {
        this.id = id;
        this.patientId = patientId;
        this.reportId = reportId;
        this.name = name;
        this.status = status;
        this.issuedDate = issuedDate;
        this.resultObservationIds = resultObservationIds;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getReportId() { return reportId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getIssuedDate() { return issuedDate; }
    public List<String> getResultObservationIds() { return resultObservationIds; }
    public void setId(String id) { this.id = id; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }
    public void setResultObservationIds(List<String> resultObservationIds) { this.resultObservationIds = resultObservationIds; }
}
