package com.medisphere.medispherebackend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class VitalProducer {

    private static final Logger logger = LoggerFactory.getLogger(VitalProducer.class);
    private static final String TOPIC = "patient-vitals";

    private final KafkaTemplate<String, VitalData> kafkaTemplate;

    public VitalProducer(KafkaTemplate<String, VitalData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendVitalData(VitalData vitalData) {
        try {
            kafkaTemplate.send(TOPIC, vitalData.getPatientId(), vitalData).whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.warn("Could not deliver vital data to Kafka topic {} for patient {}: {}",
                            TOPIC, vitalData.getPatientId(), ex.getMessage());
                } else {
                    logger.debug("Delivered vital data to topic {} at offset {}",
                            TOPIC, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            logger.warn("Kafka send failed for patient {}: {}", vitalData.getPatientId(), e.getMessage());
        }
    }
}