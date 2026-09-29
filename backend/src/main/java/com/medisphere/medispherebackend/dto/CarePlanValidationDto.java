package com.medisphere.medispherebackend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 4: Care Plan Validation DTO
 * Encapsulates Guideline Compliance, Safety Checks, and Drug Interaction Validation.
 */
public class CarePlanValidationDto {

    private String carePlanId;
    private String patientId;
    private String patientName;
    private String overallStatus; // PASSED, NEEDS_REVIEW
    private Instant validatedAt;

    private GuidelineComplianceResult guidelineCompliance;
    private SafetyCheckResult safetyCheck;
    private DrugInteractionResult drugInteraction;

    public CarePlanValidationDto() {
        this.validatedAt = Instant.now();
        this.guidelineCompliance = new GuidelineComplianceResult();
        this.safetyCheck = new SafetyCheckResult();
        this.drugInteraction = new DrugInteractionResult();
    }

    public String getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(String carePlanId) {
        this.carePlanId = carePlanId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public GuidelineComplianceResult getGuidelineCompliance() {
        return guidelineCompliance;
    }

    public void setGuidelineCompliance(GuidelineComplianceResult guidelineCompliance) {
        this.guidelineCompliance = guidelineCompliance;
    }

    public SafetyCheckResult getSafetyCheck() {
        return safetyCheck;
    }

    public void setSafetyCheck(SafetyCheckResult safetyCheck) {
        this.safetyCheck = safetyCheck;
    }

    public DrugInteractionResult getDrugInteraction() {
        return drugInteraction;
    }

    public void setDrugInteraction(DrugInteractionResult drugInteraction) {
        this.drugInteraction = drugInteraction;
    }

    // ─────────────────────────────────────────────────────────────
    // Inner result classes
    // ─────────────────────────────────────────────────────────────

    public static class GuidelineComplianceResult {
        private String status; // COMPLIANT, NEEDS_REVIEW, NOT_APPLICABLE
        private String displayStatus; // Compliant, Needs Review, Not Applicable
        private String summary;
        private List<String> guidelinesChecked = new ArrayList<>();
        private List<String> findings = new ArrayList<>();
        private String disclaimer = "Configured clinical rule-based check for educational demonstration. Not an official medical diagnostic certification.";

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDisplayStatus() {
            return displayStatus;
        }

        public void setDisplayStatus(String displayStatus) {
            this.displayStatus = displayStatus;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getGuidelinesChecked() {
            return guidelinesChecked;
        }

        public void setGuidelinesChecked(List<String> guidelinesChecked) {
            this.guidelinesChecked = guidelinesChecked;
        }

        public List<String> getFindings() {
            return findings;
        }

        public void setFindings(List<String> findings) {
            this.findings = findings;
        }

        public String getDisclaimer() {
            return disclaimer;
        }

        public void setDisclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
        }
    }

    public static class SafetyCheckResult {
        private String status; // PASSED, WARNING
        private String displayStatus; // Safety Check Passed, Warning / Needs Review
        private boolean passed;
        private String summary;
        private List<String> checksPassed = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDisplayStatus() {
            return displayStatus;
        }

        public void setDisplayStatus(String displayStatus) {
            this.displayStatus = displayStatus;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getChecksPassed() {
            return checksPassed;
        }

        public void setChecksPassed(List<String> checksPassed) {
            this.checksPassed = checksPassed;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }

    public static class DrugInteractionResult {
        private String status; // NO_INTERACTIONS, WARNING
        private String displayStatus; // No configured interaction detected, Warning / Interaction Detected
        private boolean hasInteraction;
        private String summary;
        private List<DrugInteractionItem> detectedInteractions = new ArrayList<>();
        private List<String> medicationsChecked = new ArrayList<>();
        private String disclaimer = "Demonstration check based on configured interaction rules. Does not replace comprehensive clinical decision support or pharmacist review.";

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDisplayStatus() {
            return displayStatus;
        }

        public void setDisplayStatus(String displayStatus) {
            this.displayStatus = displayStatus;
        }

        public boolean isHasInteraction() {
            return hasInteraction;
        }

        public void setHasInteraction(boolean hasInteraction) {
            this.hasInteraction = hasInteraction;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<DrugInteractionItem> getDetectedInteractions() {
            return detectedInteractions;
        }

        public void setDetectedInteractions(List<DrugInteractionItem> detectedInteractions) {
            this.detectedInteractions = detectedInteractions;
        }

        public List<String> getMedicationsChecked() {
            return medicationsChecked;
        }

        public void setMedicationsChecked(List<String> medicationsChecked) {
            this.medicationsChecked = medicationsChecked;
        }

        public String getDisclaimer() {
            return disclaimer;
        }

        public void setDisclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
        }
    }

    public static class DrugInteractionItem {
        private String drugA;
        private String drugB;
        private String severity; // WARNING, MODERATE, HIGH
        private String description;
        private String recommendation;

        public DrugInteractionItem() {}

        public DrugInteractionItem(String drugA, String drugB, String severity, String description, String recommendation) {
            this.drugA = drugA;
            this.drugB = drugB;
            this.severity = severity;
            this.description = description;
            this.recommendation = recommendation;
        }

        public String getDrugA() {
            return drugA;
        }

        public void setDrugA(String drugA) {
            this.drugA = drugA;
        }

        public String getDrugB() {
            return drugB;
        }

        public void setDrugB(String drugB) {
            this.drugB = drugB;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }
}
