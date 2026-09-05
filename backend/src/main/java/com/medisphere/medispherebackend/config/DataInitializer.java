package com.medisphere.medispherebackend.config;

import com.medisphere.medispherebackend.model.Role;
import com.medisphere.medispherebackend.model.User;
import com.medisphere.medispherebackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin1").isEmpty()) {
                userRepository.save(
                        new User(
                                "admin1",
                                passwordEncoder.encode("admin123"),
                                Role.ADMIN
                        )
                );
            }

            if (userRepository.findByUsername("doctor1").isEmpty()) {
                userRepository.save(
                        new User(
                                "doctor1",
                                passwordEncoder.encode("doctor123"),
                                Role.DOCTOR
                        )
                );
            }

            if (userRepository.findByUsername("nurse1").isEmpty()) {
                userRepository.save(
                        new User(
                                "nurse1",
                                passwordEncoder.encode("nurse123"),
                                Role.NURSE
                        )
                );
            }
        };
    }
}