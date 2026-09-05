package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.PatientTwin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientTwinRepository extends MongoRepository<PatientTwin, String> {
	Optional<PatientTwin> findBySourcePatientId(String sourcePatientId);
}
