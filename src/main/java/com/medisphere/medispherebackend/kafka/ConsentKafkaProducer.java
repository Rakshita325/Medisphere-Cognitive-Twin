package com.medisphere.medispherebackend.kafka;

import com.medisphere.medispherebackend.model.Consent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsentKafkaProducer {

    private static final String TOPIC = "consent-events";

    private final KafkaTemplate<String, Consent> kafkaTemplate;

    public ConsentKafkaProducer(KafkaTemplate<String, Consent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendConsentEvent(Consent consent) {
        kafkaTemplate.send(TOPIC, consent.getPatientId(), consent);
    }
}