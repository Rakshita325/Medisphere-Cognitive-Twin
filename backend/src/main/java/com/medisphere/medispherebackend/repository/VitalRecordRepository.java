package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.VitalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VitalRecordRepository extends MongoRepository<VitalRecord, String> {

    List<VitalRecord> findByPatientId(String patientId);

    List<VitalRecord> findTop50ByPatientIdOrderByCreatedAtDesc(String patientId);
}