package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.CreateTreatmentTrackingRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.model.TreatmentTracking;
import com.medisphere.medispherebackend.repository.CarePlanRepository;
import com.medisphere.medispherebackend.repository.TreatmentTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class TreatmentTrackingService {

    private static final Logger log = LoggerFactory.getLogger(TreatmentTrackingService.class);

    private final TreatmentTrackingRepository treatmentTrackingRepository;
    private final CarePlanRepository carePlanRepository;

    public TreatmentTrackingService(TreatmentTrackingRepository treatmentTrackingRepository,
                                    CarePlanRepository carePlanRepository) {
        this.treatmentTrackingRepository = treatmentTrackingRepository;
        this.carePlanRepository = carePlanRepository;
    }

    /**
     * Create a single treatment tracking item.
     */
    public TreatmentTracking createTracking(CreateTreatmentTrackingRequest request) {
        String interventionId = request.getInterventionId();
        if (interventionId == null || interventionId.trim().isEmpty()) {
            interventionId = UUID.randomUUID().toString();
        }

        String type = request.getInterventionType();
        if (type == null || type.trim().isEmpty()) {
            type = determineInterventionType(request.getDescription());
        }

        TreatmentTracking tracking = new TreatmentTracking(
                interventionId,
                request.getPatientId(),
                request.getPatientName(),
                request.getCarePlanId(),
                type,
                request.getDescription(),
                request.getFrequency() != null ? request.getFrequency() : "Daily",
                request.getDueDate() != null ? request.getDueDate() : "Today",
                request.getStatus() != null ? request.getStatus() : "PENDING",
                request.getNotes()
        );

        return treatmentTrackingRepository.save(tracking);
    }

    /**
     * Create multiple treatment tracking items in batch.
     */
    public List<TreatmentTracking> createTrackingBatch(List<TreatmentTracking> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        for (TreatmentTracking item : items) {
            if (item.getInterventionId() == null || item.getInterventionId().trim().isEmpty()) {
                item.setInterventionId(UUID.randomUUID().toString());
            }
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(Instant.now());
            }
            item.setUpdatedAt(Instant.now());
        }
        return treatmentTrackingRepository.saveAll(items);
    }

    /**
     * Fetch treatment tracking records for a specific patient.
     * If no records exist yet but the patient has existing Care Plans, auto-initialize
     * tracking records from the latest Care Plan to provide seamless continuity.
     */
    public List<TreatmentTracking> getTrackingByPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<TreatmentTracking> list = treatmentTrackingRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

        if (list.isEmpty()) {
            // Check if patient has any existing care plans
            List<CarePlan> plans = carePlanRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
            if (!plans.isEmpty()) {
                CarePlan latest = plans.get(0);
                log.info("Auto-initializing treatment tracking for patient {} from Care Plan {}", patientId, latest.getId());
                return initializeTrackingForCarePlan(latest);
            }
        }

        return list;
    }

    /**
     * Fetch treatment tracking records for a specific Care Plan.
     */
    public List<TreatmentTracking> getTrackingByCarePlanId(String carePlanId) {
        if (carePlanId == null || carePlanId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return treatmentTrackingRepository.findByCarePlanIdOrderByCreatedAtDesc(carePlanId);
    }

    /**
     * Fetch a single treatment tracking record by ID.
     */
    public Optional<TreatmentTracking> getTrackingById(String id) {
        return treatmentTrackingRepository.findById(id);
    }

    /**
     * Update the status and optional notes/completion timestamp of an intervention.
     * Supported statuses: PENDING, IN_PROGRESS, COMPLETED, MISSED, CANCELLED.
     */
    public Optional<TreatmentTracking> updateStatus(String id, String newStatus, String notes, Instant completedAt) {
        return treatmentTrackingRepository.findById(id).map(existing -> {
            String upper = newStatus != null ? newStatus.trim().toUpperCase() : existing.getStatus();
            existing.setStatus(upper);

            if ("COMPLETED".equals(upper)) {
                existing.setCompletedAt(completedAt != null ? completedAt : Instant.now());
            } else {
                // If moving away from completed, clear completion timestamp unless explicitly provided
                if (completedAt == null && !"COMPLETED".equals(upper)) {
                    existing.setCompletedAt(null);
                }
            }

            if (notes != null) {
                existing.setNotes(notes);
            }

            existing.setUpdatedAt(Instant.now());
            return treatmentTrackingRepository.save(existing);
        });
    }

    /**
     * Update entire tracking record.
     */
    public Optional<TreatmentTracking> updateTracking(String id, TreatmentTracking updated) {
        return treatmentTrackingRepository.findById(id).map(existing -> {
            if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
            if (updated.getInterventionType() != null) existing.setInterventionType(updated.getInterventionType());
            if (updated.getFrequency() != null) existing.setFrequency(updated.getFrequency());
            if (updated.getDueDate() != null) existing.setDueDate(updated.getDueDate());
            if (updated.getStatus() != null) {
                String upper = updated.getStatus().toUpperCase();
                existing.setStatus(upper);
                if ("COMPLETED".equals(upper) && existing.getCompletedAt() == null) {
                    existing.setCompletedAt(Instant.now());
                } else if (!"COMPLETED".equals(upper)) {
                    existing.setCompletedAt(null);
                }
            }
            if (updated.getNotes() != null) existing.setNotes(updated.getNotes());
            if (updated.getCompletedAt() != null) existing.setCompletedAt(updated.getCompletedAt());
            existing.setUpdatedAt(Instant.now());
            return treatmentTrackingRepository.save(existing);
        });
    }

    /**
     * Delete a treatment tracking record.
     */
    public boolean deleteTracking(String id) {
        if (treatmentTrackingRepository.existsById(id)) {
            treatmentTrackingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Automatically create tracking records for each intervention in a Care Plan.
     */
    public List<TreatmentTracking> initializeTrackingForCarePlan(CarePlan carePlan) {
        if (carePlan == null || carePlan.getInterventions() == null || carePlan.getInterventions().isEmpty()) {
            return Collections.emptyList();
        }

        List<TreatmentTracking> createdRecords = new ArrayList<>();
        List<String> interventions = carePlan.getInterventions();

        for (String interventionText : interventions) {
            if (interventionText == null || interventionText.trim().isEmpty()) {
                continue;
            }

            String trimmed = interventionText.trim();
            String type = determineInterventionType(trimmed);
            String frequency = determineFrequency(trimmed);
            String interventionId = UUID.randomUUID().toString();

            TreatmentTracking tracking = new TreatmentTracking(
                    interventionId,
                    carePlan.getPatientId(),
                    carePlan.getPatientName(),
                    carePlan.getId(),
                    type,
                    trimmed,
                    frequency,
                    "Today",
                    "PENDING",
                    null
            );

            createdRecords.add(treatmentTrackingRepository.save(tracking));
        }

        log.info("Initialized {} treatment tracking records for care plan ID {}", createdRecords.size(), carePlan.getId());
        return createdRecords;
    }

    /**
     * Helper to classify intervention type based on textual content.
     */
    private String determineInterventionType(String text) {
        if (text == null) return "GENERAL";
        String lower = text.toLowerCase();
        if (lower.contains("medication") || lower.contains("prescribed") || lower.contains("drug") || lower.contains("dose")) {
            return "MEDICATION";
        }
        if (lower.contains("bp") || lower.contains("blood pressure") || lower.contains("pressure")) {
            return "VITAL_MONITORING";
        }
        if (lower.contains("glucose") || lower.contains("sugar") || lower.contains("glycemic") || lower.contains("hba1c")) {
            return "GLUCOSE_MONITORING";
        }
        if (lower.contains("diet") || lower.contains("nutrition") || lower.contains("food") || lower.contains("sodium") || lower.contains("salt")) {
            return "DIET";
        }
        if (lower.contains("exercise") || lower.contains("walk") || lower.contains("physical activity") || lower.contains("workout")) {
            return "EXERCISE";
        }
        return "LIFESTYLE";
    }

    /**
     * Helper to classify default frequency based on text.
     */
    private String determineFrequency(String text) {
        if (text == null) return "Daily";
        String lower = text.toLowerCase();
        if (lower.contains("twice daily") || lower.contains("bid") || lower.contains("2 times")) {
            return "Twice Daily";
        }
        if (lower.contains("weekly") || lower.contains("once a week")) {
            return "Weekly";
        }
        if (lower.contains("monthly")) {
            return "Monthly";
        }
        if (lower.contains("as needed") || lower.contains("prn")) {
            return "As Needed";
        }
        return "Daily";
    }

    /**
     * Calculate treatment adherence for the selected patient.
     * Formula: adherence = completed due activities / total due activities * 100
     * If there are no due activities, display "No data" instead of 100%.
     * Due activities: COMPLETED, PENDING, IN_PROGRESS, MISSED (excluding CANCELLED).
     */
    public Map<String, Object> calculatePatientAdherence(String patientId) {
        List<TreatmentTracking> records = treatmentTrackingRepository.findByPatientId(patientId);

        int completed = 0;
        int pending = 0;
        int missed = 0;
        int inProgress = 0;
        int cancelled = 0;

        for (TreatmentTracking t : records) {
            String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "PENDING";
            switch (s) {
                case "COMPLETED":
                    completed++;
                    break;
                case "PENDING":
                    pending++;
                    break;
                case "MISSED":
                    missed++;
                    break;
                case "IN_PROGRESS":
                    inProgress++;
                    break;
                case "CANCELLED":
                    cancelled++;
                    break;
                default:
                    pending++;
                    break;
            }
        }

        // Total due activities (excluding CANCELLED)
        int totalDue = completed + pending + missed + inProgress;

        Map<String, Object> result = new HashMap<>();
        result.put("patientId", patientId);
        result.put("totalRecords", records.size());
        result.put("totalDue", totalDue);
        result.put("completed", completed);
        result.put("pending", pending);
        result.put("missed", missed);
        result.put("inProgress", inProgress);
        result.put("cancelled", cancelled);

        if (totalDue == 0) {
            result.put("adherencePercentage", null);
            result.put("adherenceDisplay", "No data");
        } else {
            double rate = ((double) completed / totalDue) * 100.0;
            double rounded = Math.round(rate * 10.0) / 10.0;
            result.put("adherencePercentage", rounded);
            result.put("adherenceDisplay", ((int) Math.round(rate)) + "%");
        }

        return result;
    }
}
