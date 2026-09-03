package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.Consent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConsentRepository extends MongoRepository<Consent, String> {

    Optional<Consent> findByPatientId(String patientId);
}