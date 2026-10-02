package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientData {

    private String patientId;
    private PersonalDetails personalDetails;
    private List<ConditionData> conditions;
    private List<VitalData> vitals;
    private List<LabResult> labResults;
    private LabReport labReport;
    private WearableData wearableData;

    // Milestone 2 Model Specific Features
    private Integer education;
    @JsonProperty("is_smoking")
    private Integer is_smoking;
    private Integer cigsPerDay;
    @JsonProperty("BPMeds")
    private Integer BPMeds;
    private Integer prevalentStroke;
    private Integer prevalentHyp;
    private Integer diabetes;
    private Double totChol;
    private Double sysBP;
    private Double diaBP;
    @JsonProperty("BMI")
    private Double BMI;
    private Double heartRate;
    private Double glucose;

    @JsonProperty("Pregnancies")
    private Integer Pregnancies;
    @JsonProperty("SkinThickness")
    private Double SkinThickness;
    @JsonProperty("Insulin")
    private Double Insulin;
    @JsonProperty("DiabetesPedigreeFunction")
    private Double DiabetesPedigreeFunction;
    @JsonProperty("Age")
    private Integer Age;

    private Map<String, Object> cvdFeatures;
    private Map<String, Object> diabetesFeatures;

    private Map<String, Object> additionalProperties = new HashMap<>();

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

    public Integer getEducation() {
        return education;
    }

    public void setEducation(Integer education) {
        this.education = education;
    }

    public Integer getIs_smoking() {
        return is_smoking;
    }

    public void setIs_smoking(Integer is_smoking) {
        this.is_smoking = is_smoking;
    }

    public Integer getCigsPerDay() {
        return cigsPerDay;
    }

    public void setCigsPerDay(Integer cigsPerDay) {
        this.cigsPerDay = cigsPerDay;
    }

    public Integer getBPMeds() {
        return BPMeds;
    }

    public void setBPMeds(Integer BPMeds) {
        this.BPMeds = BPMeds;
    }

    public Integer getPrevalentStroke() {
        return prevalentStroke;
    }

    public void setPrevalentStroke(Integer prevalentStroke) {
        this.prevalentStroke = prevalentStroke;
    }

    public Integer getPrevalentHyp() {
        return prevalentHyp;
    }

    public void setPrevalentHyp(Integer prevalentHyp) {
        this.prevalentHyp = prevalentHyp;
    }

    public Integer getDiabetes() {
        return diabetes;
    }

    public void setDiabetes(Integer diabetes) {
        this.diabetes = diabetes;
    }

    public Double getTotChol() {
        return totChol;
    }

    public void setTotChol(Double totChol) {
        this.totChol = totChol;
    }

    public Double getSysBP() {
        return sysBP;
    }

    public void setSysBP(Double sysBP) {
        this.sysBP = sysBP;
    }

    public Double getDiaBP() {
        return diaBP;
    }

    public void setDiaBP(Double diaBP) {
        this.diaBP = diaBP;
    }

    public Double getBMI() {
        return BMI;
    }

    public void setBMI(Double BMI) {
        this.BMI = BMI;
    }

    public Double getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Double heartRate) {
        this.heartRate = heartRate;
    }

    public Double getGlucose() {
        return glucose;
    }

    public void setGlucose(Double glucose) {
        this.glucose = glucose;
    }

    public Integer getPregnancies() {
        return Pregnancies;
    }

    public void setPregnancies(Integer pregnancies) {
        this.Pregnancies = pregnancies;
    }

    public Double getSkinThickness() {
        return SkinThickness;
    }

    public void setSkinThickness(Double skinThickness) {
        this.SkinThickness = skinThickness;
    }

    public Double getInsulin() {
        return Insulin;
    }

    public void setInsulin(Double insulin) {
        this.Insulin = insulin;
    }

    public Double getDiabetesPedigreeFunction() {
        return DiabetesPedigreeFunction;
    }

    public void setDiabetesPedigreeFunction(Double diabetesPedigreeFunction) {
        this.DiabetesPedigreeFunction = diabetesPedigreeFunction;
    }

    public Integer getAge() {
        return Age;
    }

    public void setAge(Integer age) {
        this.Age = age;
    }

    public Map<String, Object> getCvdFeatures() {
        return cvdFeatures;
    }

    public void setCvdFeatures(Map<String, Object> cvdFeatures) {
        this.cvdFeatures = cvdFeatures;
    }

    public Map<String, Object> getDiabetesFeatures() {
        return diabetesFeatures;
    }

    public void setDiabetesFeatures(Map<String, Object> diabetesFeatures) {
        this.diabetesFeatures = diabetesFeatures;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}