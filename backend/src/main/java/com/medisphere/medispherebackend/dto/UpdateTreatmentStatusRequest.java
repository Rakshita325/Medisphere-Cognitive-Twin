package com.medisphere.medispherebackend.dto;

import java.time.Instant;

public class UpdateTreatmentStatusRequest {

    private String status;
    private String notes;
    private Instant completedAt;

    public UpdateTreatmentStatusRequest() {
    }

    public UpdateTreatmentStatusRequest(String status, String notes, Instant completedAt) {
        this.status = status;
        this.notes = notes;
        this.completedAt = completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
