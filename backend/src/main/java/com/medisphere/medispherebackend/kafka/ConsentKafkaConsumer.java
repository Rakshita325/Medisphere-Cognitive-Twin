package com.medisphere.medispherebackend.kafka;

import com.medisphere.medispherebackend.model.Consent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class ConsentKafkaConsumer {

    @KafkaListener(
            topics = "consent-events",
            groupId = "medisphere-consent"
    )
    public void consumeConsentEvent(Consent consent) {

        System.out.println("===== CONSENT EVENT RECEIVED =====");
        System.out.println("Patient ID: " + consent.getPatientId());
        System.out.println("Requester: " + consent.getRequester());
        System.out.println("Purpose: " + consent.getPurpose());
        System.out.println("Granted: " + consent.isGranted());
        System.out.println("==================================");
    }
}