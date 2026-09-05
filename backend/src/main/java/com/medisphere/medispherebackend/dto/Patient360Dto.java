package com.medisphere.medispherebackend.dto;

import com.medisphere.medispherebackend.model.PatientTwin;
import java.util.List;

public class Patient360Dto {
    private FhirPatientDto patient;
    private List<FhirConditionDto> conditions;
    private List<FhirObservationDto> vitals;
    private List<FhirObservationDto> labs;
    private List<FhirDiagnosticReportDto> diagnosticReports;
    private List<FhirObservationDto> wearableData;
    private PatientTwin digitalTwin;
    private boolean consentGranted;

    public Patient360Dto() { }
    public Patient360Dto(FhirPatientDto patient, List<FhirConditionDto> conditions,
                         List<FhirObservationDto> vitals, List<FhirObservationDto> labs,
                         List<FhirDiagnosticReportDto> diagnosticReports,
                         List<FhirObservationDto> wearableData, PatientTwin digitalTwin,
                         boolean consentGranted) {
        this.patient = patient;
        this.conditions = conditions;
        this.vitals = vitals;
        this.labs = labs;
        this.diagnosticReports = diagnosticReports;
        this.wearableData = wearableData;
        this.digitalTwin = digitalTwin;
        this.consentGranted = consentGranted;
    }
    public FhirPatientDto getPatient() { return patient; }
    public List<FhirConditionDto> getConditions() { return conditions; }
    public List<FhirObservationDto> getVitals() { return vitals; }
    public List<FhirObservationDto> getLabs() { return labs; }
    public List<FhirDiagnosticReportDto> getDiagnosticReports() { return diagnosticReports; }
    public List<FhirObservationDto> getWearableData() { return wearableData; }
    public PatientTwin getDigitalTwin() { return digitalTwin; }
    public boolean isConsentGranted() { return consentGranted; }
    public void setPatient(FhirPatientDto patient) { this.patient = patient; }
    public void setConditions(List<FhirConditionDto> conditions) { this.conditions = conditions; }
    public void setVitals(List<FhirObservationDto> vitals) { this.vitals = vitals; }
    public void setLabs(List<FhirObservationDto> labs) { this.labs = labs; }
    public void setDiagnosticReports(List<FhirDiagnosticReportDto> diagnosticReports) { this.diagnosticReports = diagnosticReports; }
    public void setWearableData(List<FhirObservationDto> wearableData) { this.wearableData = wearableData; }
    public void setDigitalTwin(PatientTwin digitalTwin) { this.digitalTwin = digitalTwin; }
    public void setConsentGranted(boolean consentGranted) { this.consentGranted = consentGranted; }
}
