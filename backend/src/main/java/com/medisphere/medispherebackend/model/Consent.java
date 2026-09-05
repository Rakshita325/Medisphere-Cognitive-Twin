package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "consents")
public class Consent {

    @Id
    private String id;

    private String patientId;
    private String requester;
    private String purpose;
    private Boolean granted;

    public Consent() {
    }

    public Consent(String patientId, String requester, String purpose, Boolean granted) {
        this.patientId = patientId;
        this.requester = requester;
        this.purpose = purpose;
        this.granted = granted;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getRequester() {
        return requester;
    }

    public String getPurpose() {
        return purpose;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }
}