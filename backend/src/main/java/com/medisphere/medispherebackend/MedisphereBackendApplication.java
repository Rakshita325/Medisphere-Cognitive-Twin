package com.medisphere.medispherebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
public class MedisphereBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedisphereBackendApplication.class, args);
    }

}
