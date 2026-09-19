
package com.medisphere.medispherebackend.repository;

import com.medisphere.medispherebackend.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends MongoRepository<Alert, String> {

    List<Alert> findByStatusOrderByDetectedAtDesc(String status);

    List<Alert> findByPatientIdOrderByDetectedAtDesc(String patientId);

    Optional<Alert> findFirstByPatientIdAndVitalTypeAndStatusOrderByDetectedAtDesc(
            String patientId,
            String vitalType,
            String status);
}
