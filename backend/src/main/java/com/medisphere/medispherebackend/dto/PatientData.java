package com.medisphere.medispherebackend.dto;
import java.util.List;


public class PatientData {

    private String patientId;
    private PersonalDetails personalDetails;
    private List<ConditionData> conditions;
    private List<VitalData> vitals;
    private List<LabResult> labResults;
    private LabReport labReport;
    private WearableData wearableData;

    public PatientData() {}

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(PersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public List<ConditionData> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionData> conditions) {
        this.conditions = conditions;
    }

    public List<VitalData> getVitals() {
        return vitals;
    }

    public void setVitals(List<VitalData> vitals) {
        this.vitals = vitals;
    }

    public List<LabResult> getLabResults() {
        return labResults;
    }

    public void setLabResults(List<LabResult> labResults) {
        this.labResults = labResults;
    }

    public LabReport getLabReport() {
        return labReport;
    }

    public void setLabReport(LabReport labReport) {
        this.labReport = labReport;
    }

    public WearableData getWearableData() {
        return wearableData;
    }

    public void setWearableData(WearableData wearableData) {
        this.wearableData = wearableData;
    }
}