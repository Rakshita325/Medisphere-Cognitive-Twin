package com.medisphere.medispherebackend.service;

/**
 * Encapsulates the outcome of a clinical rule evaluation.
 *
 * Provides explainable rule outcomes for decision-support and workflow actions,
 * strictly for clinical monitoring and NOT medical diagnoses.
 */
public class ClinicalRuleResult {

    private boolean ruleTriggered;
    private String severity;
    private String message;
    private String recommendedAction;
    private String ruleType;

    public ClinicalRuleResult() {
    }

    public ClinicalRuleResult(
            boolean ruleTriggered,
            String severity,
            String message,
            String recommendedAction,
            String ruleType) {
        this.ruleTriggered = ruleTriggered;
        this.severity = severity;
        this.message = message;
        this.recommendedAction = recommendedAction;
        this.ruleType = ruleType;
    }

    public static ClinicalRuleResult notTriggered() {
        return new ClinicalRuleResult(false, null, null, null, null);
    }

    public static ClinicalRuleResult triggered(
            String severity,
            String message,
            String recommendedAction,
            String ruleType) {
        return new ClinicalRuleResult(true, severity, message, recommendedAction, ruleType);
    }

    public boolean isRuleTriggered() {
        return ruleTriggered;
    }

    public void setRuleTriggered(boolean ruleTriggered) {
        this.ruleTriggered = ruleTriggered;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    @Override
    public String toString() {
        return "ClinicalRuleResult{" +
                "ruleTriggered=" + ruleTriggered +
                ", severity='" + severity + '\'' +
                ", message='" + message + '\'' +
                ", recommendedAction='" + recommendedAction + '\'' +
                ", ruleType='" + ruleType + '\'' +
                '}';
    }
}
