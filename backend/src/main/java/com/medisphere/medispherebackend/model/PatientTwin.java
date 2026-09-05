package com.medisphere.medispherebackend.model;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.medisphere.medispherebackend.dto.FhirConditionDto;
import com.medisphere.medispherebackend.dto.FhirObservationDto;
import com.medisphere.medispherebackend.dto.FhirDiagnosticReportDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "patient_twins")
public class PatientTwin {

    @Id
    private String id;

    @NotBlank
    private String patientId;

    @Indexed(unique = true, sparse = true)
    private String sourcePatientId;
    private String fhirPatientId;

    @NotBlank
    private String name;

    @Min(0)
    private int age;

    @NotBlank
    private String gender;

    private String diagnosis;
    private String phone;
    private List<FhirConditionDto> conditions = new ArrayList<>();
    private List<FhirObservationDto> vitals = new ArrayList<>();
    private List<FhirObservationDto> labResults = new ArrayList<>();
    private List<FhirDiagnosticReportDto> labReports = new ArrayList<>();
    private WearableSnapshot wearableData;
    private Instant lastUpdated;
    private Boolean consentGranted;

    public PatientTwin() {
    }

    public PatientTwin(String patientId, String name, int age,
                       String gender, String diagnosis) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.diagnosis = diagnosis;
        this.sourcePatientId = patientId;
        this.fhirPatientId = patientId;
        this.lastUpdated = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getSourcePatientId() { return sourcePatientId; }
    public String getFhirPatientId() { return fhirPatientId; }
    public String getPhone() { return phone; }
    public List<FhirConditionDto> getConditions() { return conditions; }
    public List<FhirObservationDto> getVitals() { return vitals; }
    public List<FhirObservationDto> getLabResults() { return labResults; }
    public List<FhirDiagnosticReportDto> getLabReports() { return labReports; }
    public WearableSnapshot getWearableData() { return wearableData; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Boolean getConsentGranted() { return consentGranted; }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setSourcePatientId(String sourcePatientId) { this.sourcePatientId = sourcePatientId; }
    public void setFhirPatientId(String fhirPatientId) { this.fhirPatientId = fhirPatientId; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setConditions(List<FhirConditionDto> conditions) { this.conditions = conditions; }
    public void setVitals(List<FhirObservationDto> vitals) { this.vitals = vitals; }
    public void setLabResults(List<FhirObservationDto> labResults) { this.labResults = labResults; }
    public void setLabReports(List<FhirDiagnosticReportDto> labReports) { this.labReports = labReports; }
    public void setWearableData(WearableSnapshot wearableData) { this.wearableData = wearableData; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setConsentGranted(Boolean consentGranted) { this.consentGranted = consentGranted; }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public static class WearableSnapshot {
        private int steps;
        private double sleepHours;
        private String recordedAt;

        public WearableSnapshot() { }
        public WearableSnapshot(int steps, double sleepHours, String recordedAt) {
            this.steps = steps;
            this.sleepHours = sleepHours;
            this.recordedAt = recordedAt;
        }
        public int getSteps() { return steps; }
        public double getSleepHours() { return sleepHours; }
        public String getRecordedAt() { return recordedAt; }
        public void setSteps(int steps) { this.steps = steps; }
        public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }
        public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
    }
}