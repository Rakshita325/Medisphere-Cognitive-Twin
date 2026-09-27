package com.medisphere.medispherebackend.dto;

import java.util.List;
import java.util.Map;

public class GenerateCarePlanRequest {
    private String patientId;
    private String patientName;
    private Map<String, Object> riskInformation;
    private List<String> customGoals;
    private List<String> customInterventions;
    private List<String> customMonitoring;

    public GenerateCarePlanRequest() {
    }

    public GenerateCarePlanRequest(String patientId, String patientName, Map<String, Object> riskInformation) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.riskInformation = riskInformation;
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

    public Map<String, Object> getRiskInformation() {
        return riskInformation;
    }

    public void setRiskInformation(Map<String, Object> riskInformation) {
        this.riskInformation = riskInformation;
    }

    public List<String> getCustomGoals() {
        return customGoals;
    }

    public void setCustomGoals(List<String> customGoals) {
        this.customGoals = customGoals;
    }

    public List<String> getCustomInterventions() {
        return customInterventions;
    }

    public void setCustomInterventions(List<String> customInterventions) {
        this.customInterventions = customInterventions;
    }

    public List<String> getCustomMonitoring() {
        return customMonitoring;
    }

    public void setCustomMonitoring(List<String> customMonitoring) {
        this.customMonitoring = customMonitoring;
    }
}
