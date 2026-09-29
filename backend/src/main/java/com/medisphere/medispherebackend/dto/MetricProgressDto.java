package com.medisphere.medispherebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Milestone 4 Part 4: Progress / Improvement
 *
 * Represents the progress comparison for a single health metric
 * between its baseline (earlier/reference) and latest recorded measurement.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricProgressDto {

    private String metric;
    private String type;
    private String unit;
    private boolean isBP;

    // Formatted or general values (supports numeric or string like "135/85")
    private Object baseline;
    private Object latest;
    private Object change;

    // Numeric changes for analytics
    private Double numericChange;
    private Double systolicChange;
    private Double diastolicChange;

    // Individual BP values
    private Double baselineSystolic;
    private Double baselineDiastolic;
    private Double latestSystolic;
    private Double latestDiastolic;

    // Timestamps
    private String baselineRecordedAt;
    private String latestRecordedAt;

    // Status: IMPROVED, WORSENED, STABLE, or null
    private String status;

    // Flags
    private boolean hasEnoughData;
    private String message;
    private int historicalCount;

    public MetricProgressDto() {
    }

    // --- Getters & Setters ---

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isBP() {
        return isBP;
    }

    public void setBP(boolean BP) {
        isBP = BP;
    }

    public Object getBaseline() {
        return baseline;
    }

    public void setBaseline(Object baseline) {
        this.baseline = baseline;
    }

    public Object getLatest() {
        return latest;
    }

    public void setLatest(Object latest) {
        this.latest = latest;
    }

    public Object getChange() {
        return change;
    }

    public void setChange(Object change) {
        this.change = change;
    }

    public Double getNumericChange() {
        return numericChange;
    }

    public void setNumericChange(Double numericChange) {
        this.numericChange = numericChange;
    }

    public Double getSystolicChange() {
        return systolicChange;
    }

    public void setSystolicChange(Double systolicChange) {
        this.systolicChange = systolicChange;
    }

    public Double getDiastolicChange() {
        return diastolicChange;
    }

    public void setDiastolicChange(Double diastolicChange) {
        this.diastolicChange = diastolicChange;
    }

    public Double getBaselineSystolic() {
        return baselineSystolic;
    }

    public void setBaselineSystolic(Double baselineSystolic) {
        this.baselineSystolic = baselineSystolic;
    }

    public Double getBaselineDiastolic() {
        return baselineDiastolic;
    }

    public void setBaselineDiastolic(Double baselineDiastolic) {
        this.baselineDiastolic = baselineDiastolic;
    }

    public Double getLatestSystolic() {
        return latestSystolic;
    }

    public void setLatestSystolic(Double latestSystolic) {
        this.latestSystolic = latestSystolic;
    }

    public Double getLatestDiastolic() {
        return latestDiastolic;
    }

    public void setLatestDiastolic(Double latestDiastolic) {
        this.latestDiastolic = latestDiastolic;
    }

    public String getBaselineRecordedAt() {
        return baselineRecordedAt;
    }

    public void setBaselineRecordedAt(String baselineRecordedAt) {
        this.baselineRecordedAt = baselineRecordedAt;
    }

    public String getLatestRecordedAt() {
        return latestRecordedAt;
    }

    public void setLatestRecordedAt(String latestRecordedAt) {
        this.latestRecordedAt = latestRecordedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHasEnoughData() {
        return hasEnoughData;
    }

    public void setHasEnoughData(boolean hasEnoughData) {
        this.hasEnoughData = hasEnoughData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getHistoricalCount() {
        return historicalCount;
    }

    public void setHistoricalCount(int historicalCount) {
        this.historicalCount = historicalCount;
    }
}
