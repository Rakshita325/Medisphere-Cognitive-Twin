package com.medisphere.medispherebackend.dto;

public class FhirPatientDto {

    private String id;
    private String fhirPatientId;
    private String sourcePatientId;
    private String name;
    private String gender;
    private String birthDate;

    public FhirPatientDto() {
    }

    public FhirPatientDto(String id, String name, String gender, String birthDate) {
        this.id = id;
        this.fhirPatientId = id;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public FhirPatientDto(String fhirPatientId, String sourcePatientId, String name,
                          String gender, String birthDate) {
        this.id = fhirPatientId;
        this.fhirPatientId = fhirPatientId;
        this.sourcePatientId = sourcePatientId;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.fhirPatientId = id;
    }

    public String getFhirPatientId() { return fhirPatientId; }
    public String getSourcePatientId() { return sourcePatientId; }
    public void setFhirPatientId(String fhirPatientId) { this.fhirPatientId = fhirPatientId; }
    public void setSourcePatientId(String sourcePatientId) { this.sourcePatientId = sourcePatientId; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
}