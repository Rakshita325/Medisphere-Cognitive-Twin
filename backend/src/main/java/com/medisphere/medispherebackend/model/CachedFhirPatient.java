package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "cached_fhir_patients")
public class CachedFhirPatient {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String fhirPatientId;

    @Indexed(unique = true, sparse = true)
    private String sourcePatientId;

    private String name;
    private String gender;
    private String birthDate;
    private Instant lastUpdated;

    public CachedFhirPatient() {
    }

    public CachedFhirPatient(String fhirPatientId, String sourcePatientId, String name, String gender, String birthDate) {
        this.id = fhirPatientId;
        this.fhirPatientId = fhirPatientId;
        this.sourcePatientId = sourcePatientId;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.lastUpdated = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFhirPatientId() {
        return fhirPatientId;
    }

    public void setFhirPatientId(String fhirPatientId) {
        this.fhirPatientId = fhirPatientId;
    }

    public String getSourcePatientId() {
        return sourcePatientId;
    }

    public void setSourcePatientId(String sourcePatientId) {
        this.sourcePatientId = sourcePatientId;
    }

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

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
