package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.model.WearableRecord;
import com.medisphere.medispherebackend.repository.WearableRecordRepository;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/wearables")
public class WearableController {
    private final WearableRecordRepository wearableRecordRepository;
    private final ConsentAuthorizationService consentAuthorizationService;

    public WearableController(WearableRecordRepository wearableRecordRepository,
                               ConsentAuthorizationService consentAuthorizationService) {
        this.wearableRecordRepository = wearableRecordRepository;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @PostMapping
    public WearableRecord create(@Valid @RequestBody WearableRecord wearableRecord) {
        requireConsent(wearableRecord.getPatientId());
        wearableRecord.setCreatedAt(Instant.now());
        return wearableRecordRepository.save(wearableRecord);
    }

    @GetMapping("/{patientId}")
    public List<WearableRecord> getByPatient(@PathVariable String patientId) {
        requireConsent(patientId);
        return wearableRecordRepository.findByPatientId(patientId);
    }

    private void requireConsent(String patientId) {
        if (!consentAuthorizationService.isAllowed(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient consent is required");
        }
    }
}
