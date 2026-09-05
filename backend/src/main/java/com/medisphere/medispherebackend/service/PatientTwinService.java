package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.repository.PatientTwinRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;

@Service
public class PatientTwinService {

    private final PatientTwinRepository patientTwinRepository;

    public PatientTwinService(PatientTwinRepository patientTwinRepository) {
        this.patientTwinRepository = patientTwinRepository;
    }

    public PatientTwin createPatientTwin(PatientTwin patientTwin) {
        if (patientTwin.getSourcePatientId() != null) {
            PatientTwin existing = patientTwinRepository
                    .findBySourcePatientId(patientTwin.getSourcePatientId())
                    .orElse(null);
            if (existing != null && patientTwin.getId() == null) {
                patientTwin.setId(existing.getId());
            }
        }
        patientTwin.setLastUpdated(Instant.now());
        return patientTwinRepository.save(patientTwin);
    }

    public List<PatientTwin> getAllPatientTwins() {
        return patientTwinRepository.findAll();
    }

    public PatientTwin getPatientTwinById(String id) {
        return patientTwinRepository.findById(id).orElse(null);
    }

    public PatientTwin getBySourcePatientId(String sourcePatientId) {
        return patientTwinRepository.findBySourcePatientId(sourcePatientId).orElse(null);
    }

    public void deletePatientTwin(String id) {
        patientTwinRepository.deleteById(id);
    }
}