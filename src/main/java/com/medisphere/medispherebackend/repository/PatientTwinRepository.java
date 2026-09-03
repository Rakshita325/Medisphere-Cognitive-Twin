package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.PatientTwin;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientTwinRepository extends MongoRepository<PatientTwin, String> {
}
