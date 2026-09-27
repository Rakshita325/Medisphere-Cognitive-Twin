package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.TreatmentTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreatmentTrackingRepository extends MongoRepository<TreatmentTracking, String> {
    List<TreatmentTracking> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<TreatmentTracking> findByCarePlanIdOrderByCreatedAtDesc(String carePlanId);
    List<TreatmentTracking> findByPatientIdAndCarePlanId(String patientId, String carePlanId);
    List<TreatmentTracking> findByPatientId(String patientId);
    void deleteByCarePlanId(String carePlanId);
}
