package com.medisphere.medispherebackend.dto;

public class ConditionData {

    private String name;
    private String status;
    private String diagnosedDate;

    public ConditionData() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiagnosedDate() {
        return diagnosedDate;
    }

    public void setDiagnosedDate(String diagnosedDate) {
        this.diagnosedDate = diagnosedDate;
    }
}