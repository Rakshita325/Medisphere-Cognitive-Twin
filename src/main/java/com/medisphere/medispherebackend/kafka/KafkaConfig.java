package com.medisphere.medispherebackend.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic vitalTopic() {
        return new NewTopic("patient-vitals", 1, (short) 1);
    }
}