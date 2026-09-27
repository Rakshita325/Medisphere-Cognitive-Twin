package com.medisphere.medispherebackend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "care_plans")
public class CarePlan {

    @Id
    private String id;

    private String patientId;
    private String patientName;

    private List<CareGoal> goals = new ArrayList<>();
    private List<String> interventions = new ArrayList<>();
    private List<String> monitoringInstructions = new ArrayList<>();
    private Map<String, Object> riskInformation;

    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, COMPLETED
    private String disclaimer;

    private Instant createdAt;
    private Instant updatedAt;

    public CarePlan() {
    }

    public CarePlan(String patientId, String patientName, List<CareGoal> goals,
                    List<String> interventions, List<String> monitoringInstructions,
                    Map<String, Object> riskInformation, String status) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.goals = goals != null ? goals : new ArrayList<>();
        this.interventions = interventions != null ? interventions : new ArrayList<>();
        this.monitoringInstructions = monitoringInstructions != null ? monitoringInstructions : new ArrayList<>();
        this.riskInformation = riskInformation;
        this.status = status;
        this.disclaimer = "STUDENT/DEMO NOTICE: AI-generated care recommendations are intended for educational demonstration. Medication changes must be reviewed by a licensed healthcare provider before clinical application.";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<CareGoal> getGoals() {
        return goals;
    }

    public void setGoals(List<CareGoal> goals) {
        this.goals = goals;
    }

    public List<String> getInterventions() {
        return interventions;
    }

    public void setInterventions(List<String> interventions) {
        this.interventions = interventions;
    }

    public List<String> getMonitoringInstructions() {
        return monitoringInstructions;
    }

    public void setMonitoringInstructions(List<String> monitoringInstructions) {
        this.monitoringInstructions = monitoringInstructions;
    }

    public Map<String, Object> getRiskInformation() {
        return riskInformation;
    }

    public void setRiskInformation(Map<String, Object> riskInformation) {
        this.riskInformation = riskInformation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class CareGoal {
        private String title;
        private String description;
        private String targetMetric;
        private String status;

        public CareGoal() {
        }

        public CareGoal(String title, String description, String targetMetric, String status) {
            this.title = title;
            this.description = description;
            this.targetMetric = targetMetric;
            this.status = status;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTargetMetric() {
            return targetMetric;
        }

        public void setTargetMetric(String targetMetric) {
            this.targetMetric = targetMetric;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
