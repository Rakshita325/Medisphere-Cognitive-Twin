package com.medisphere.medispherebackend.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Re-imports Spring Boot's KafkaAutoConfiguration only when Kafka is enabled.
 * The main application class excludes KafkaAutoConfiguration globally,
 * and this class conditionally re-enables it.
 */
@Configuration
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
@Import(KafkaAutoConfiguration.class)
public class KafkaEnabledConfig {
}
