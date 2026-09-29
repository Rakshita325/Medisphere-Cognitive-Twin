package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.HealthMeasurement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Milestone 4 Part 3: Health Monitoring
 *
 * MongoDB repository for HealthMeasurement documents.
 * Provides patient-specific queries and type-filtered retrieval
 * ordered by createdAt descending so the latest measurements appear first.
 */
public interface HealthMeasurementRepository extends MongoRepository<HealthMeasurement, String> {

    /**
     * Find all measurements for a specific patient, newest first.
     */
    List<HealthMeasurement> findByPatientIdOrderByCreatedAtDesc(String patientId);

    /**
     * Find measurements for a specific patient filtered by measurement type, newest first.
     */
    List<HealthMeasurement> findByPatientIdAndMeasurementTypeOrderByCreatedAtDesc(
            String patientId, String measurementType);
}
