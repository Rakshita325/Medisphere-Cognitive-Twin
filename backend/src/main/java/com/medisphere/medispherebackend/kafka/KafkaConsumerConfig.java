package com.medisphere.medispherebackend.kafka;

import com.medisphere.medispherebackend.model.Consent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Consent> consentConsumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "medisphere-consent");

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        JsonDeserializer<Consent> deserializer = new JsonDeserializer<>(Consent.class);

        deserializer.addTrustedPackages(
                "com.medisphere.medispherebackend.model");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Consent> consentKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Consent> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consentConsumerFactory());

        return factory;
    }
}