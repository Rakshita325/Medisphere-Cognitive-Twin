package com.medisphere.medispherebackend.dto;

public class UpdateCarePlanStatusRequest {
    private String status;

    public UpdateCarePlanStatusRequest() {
    }

    public UpdateCarePlanStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
