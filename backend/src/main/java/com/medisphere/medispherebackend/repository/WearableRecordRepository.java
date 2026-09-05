package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.WearableRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WearableRecordRepository extends MongoRepository<WearableRecord, String> {
    List<WearableRecord> findByPatientId(String patientId);
}
