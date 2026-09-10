package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.CachedFhirPatient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CachedFhirPatientRepository extends MongoRepository<CachedFhirPatient, String> {

    Optional<CachedFhirPatient> findByFhirPatientId(String fhirPatientId);

    Optional<CachedFhirPatient> findBySourcePatientId(String sourcePatientId);
}
