package com.medisphere.medispherebackend.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class VitalProducer {

    private static final String TOPIC = "patient-vitals";

    private final KafkaTemplate<String, VitalData> kafkaTemplate;

    public VitalProducer(KafkaTemplate<String, VitalData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendVitalData(VitalData vitalData) {
        kafkaTemplate.send(TOPIC, vitalData.getPatientId(), vitalData);
    }
}