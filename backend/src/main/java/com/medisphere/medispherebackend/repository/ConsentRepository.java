package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.Consent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConsentRepository extends MongoRepository<Consent, String> {

    List<Consent> findByPatientId(String patientId);
}