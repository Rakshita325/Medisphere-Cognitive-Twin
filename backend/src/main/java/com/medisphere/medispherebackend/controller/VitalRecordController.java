package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vital-records")
public class VitalRecordController {

    private final VitalRecordRepository vitalRecordRepository;

    public VitalRecordController(VitalRecordRepository vitalRecordRepository) {
        this.vitalRecordRepository = vitalRecordRepository;
    }

    /**
     * Returns the most recent vital records for a patient (up to 50).
     * Used by the monitoring dashboard to display current vitals and trend data.
     */
    @GetMapping("/{patientId}/latest")
    public List<VitalRecord> getLatestVitals(@PathVariable String patientId) {
        return vitalRecordRepository.findTop50ByPatientIdOrderByCreatedAtDesc(patientId);
    }
}
