package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 4 Part 4: Progress / Improvement
 *
 * REST response representing full baseline-vs-latest comparison
 * across all recorded health metrics for a patient.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientProgressResponseDto {

    private String patientId;
    private boolean hasHistoricalData;
    private boolean hasAnyComparison;
    private boolean followUpRequired;
    private String followUpMessage;
    private List<MetricProgressDto> metrics = new ArrayList<>();
    private String summary;
    private String computedAt;

    public PatientProgressResponseDto() {
    }

    public PatientProgressResponseDto(String patientId) {
        this.patientId = patientId;
    }

    // --- Getters & Setters ---

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public boolean isHasHistoricalData() {
        return hasHistoricalData;
    }

    public void setHasHistoricalData(boolean hasHistoricalData) {
        this.hasHistoricalData = hasHistoricalData;
    }

    public boolean isHasAnyComparison() {
        return hasAnyComparison;
    }

    public void setHasAnyComparison(boolean hasAnyComparison) {
        this.hasAnyComparison = hasAnyComparison;
    }

    public boolean isFollowUpRequired() {
        return followUpRequired;
    }

    public void setFollowUpRequired(boolean followUpRequired) {
        this.followUpRequired = followUpRequired;
    }

    public String getFollowUpMessage() {
        return followUpMessage;
    }

    public void setFollowUpMessage(String followUpMessage) {
        this.followUpMessage = followUpMessage;
    }

    public List<MetricProgressDto> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricProgressDto> metrics) {
        this.metrics = metrics != null ? metrics : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(String computedAt) {
        this.computedAt = computedAt;
    }
}
