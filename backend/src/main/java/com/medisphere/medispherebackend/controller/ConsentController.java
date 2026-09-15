package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.kafka.ConsentKafkaProducer;
import com.medisphere.medispherebackend.model.Consent;
import com.medisphere.medispherebackend.repository.ConsentRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private final ConsentRepository consentRepository;
    private final ConsentKafkaProducer consentKafkaProducer;

    public ConsentController(
            ConsentRepository consentRepository,
            ConsentKafkaProducer consentKafkaProducer) {

        this.consentRepository = consentRepository;
        this.consentKafkaProducer = consentKafkaProducer;
    }

    // Give consent
    @PostMapping
    public Consent giveConsent(@RequestBody Consent consent) {

        consent.setGranted(true);

        Consent savedConsent = consentRepository.save(consent);

        // Send consent event to Kafka
        consentKafkaProducer.sendConsentEvent(savedConsent);

        return savedConsent;
    }

    // Revoke consent
    @PutMapping("/revoke/{patientId}")
    public String revokeConsent(@PathVariable String patientId) {

        Consent consent = consentRepository.findByPatientId(patientId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Consent not found"));

        consent.setGranted(false);

        Consent savedConsent = consentRepository.save(consent);

        // Send consent revoked event to Kafka
        consentKafkaProducer.sendConsentEvent(savedConsent);

        return "Consent revoked successfully for patient: " + patientId;
    }

    // Check consent
    @GetMapping("/{patientId}")
    public Consent getConsent(@PathVariable String patientId) {

        return consentRepository.findByPatientId(patientId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Consent not found"));
    }
}