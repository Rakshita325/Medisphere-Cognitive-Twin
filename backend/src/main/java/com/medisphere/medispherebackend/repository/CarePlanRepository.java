package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.CarePlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarePlanRepository extends MongoRepository<CarePlan, String> {
    List<CarePlan> findByPatientId(String patientId);
    List<CarePlan> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<CarePlan> findByPatientIdIgnoreCaseOrderByCreatedAtDesc(String patientId);
    List<CarePlan> findAllByOrderByCreatedAtDesc();
}
