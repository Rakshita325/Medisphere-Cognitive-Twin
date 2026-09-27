package com.medisphere.medispherebackend.kafka;

import com.medisphere.medispherebackend.model.Consent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class ConsentKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(ConsentKafkaProducer.class);
    private static final String TOPIC = "consent-events";

    private final KafkaTemplate<String, Consent> kafkaTemplate;

    public ConsentKafkaProducer(KafkaTemplate<String, Consent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendConsentEvent(Consent consent) {
        try {
            kafkaTemplate.send(TOPIC, consent.getPatientId(), consent).whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.warn("Could not deliver consent event to Kafka topic {} for patient {}: {}",
                            TOPIC, consent.getPatientId(), ex.getMessage());
                } else {
                    logger.debug("Delivered consent event to topic {} at offset {}",
                            TOPIC, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            logger.warn("Kafka consent send failed for patient {}: {}", consent.getPatientId(), e.getMessage());
        }
    }
}