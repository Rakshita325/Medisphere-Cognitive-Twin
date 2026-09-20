package com.medisphere.medispherebackend.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic vitalTopic() {
        return new NewTopic("patient-vitals", 1, (short) 1);
    }

    @Bean
    public NewTopic consentTopic() {
        return new NewTopic("consent-events", 1, (short) 1);
    }
}