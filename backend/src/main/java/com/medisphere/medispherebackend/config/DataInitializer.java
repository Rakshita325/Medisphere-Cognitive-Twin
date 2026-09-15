package com.medisphere.medispherebackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.model.Consent;
import com.medisphere.medispherebackend.model.Role;
import com.medisphere.medispherebackend.model.User;
import com.medisphere.medispherebackend.repository.CachedFhirPatientRepository;
import com.medisphere.medispherebackend.repository.ConsentRepository;
import com.medisphere.medispherebackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            try {
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
            } catch (Exception e) {
                log.warn("Failed to initialize users - MongoDB may not be available yet: {}", e.getMessage());
            }
        };
    }

    /**
     * Development/demo consent initialization only.
     * Seeds granted consent for demo patient IDs so DOCTOR and NURSE users
     * can view Patient 360 without 403 Forbidden errors in development.
     */
    @Bean
    CommandLineRunner initConsents(
            ConsentRepository consentRepository,
            @Autowired(required = false) CachedFhirPatientRepository cachedFhirPatientRepository) {

        return args -> {
            try {
                // Development/demo consent initialization only.
                Set<String> demoPatientIds = new LinkedHashSet<>();

                // 1. Core demo patient IDs (P001 to P010) and known FHIR demo patient
                demoPatientIds.addAll(List.of(
                        "P001", "P002", "P003", "P004", "P005",
                        "P006", "P007", "P008", "P009", "P010",
                        "cfsb1703736930464" // Adrian Allen1 demo patient ID
                ));

                // 2. Load demo IDs from bundled patient-data.json if present
                try (InputStream is = new ClassPathResource("patient-data.json").getInputStream()) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<PatientData> patients = mapper.readValue(is, new TypeReference<List<PatientData>>() {});
                    if (patients != null) {
                        for (PatientData p : patients) {
                            if (p.getPatientId() != null && !p.getPatientId().isBlank()) {
                                demoPatientIds.add(p.getPatientId().trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not read patient-data.json for consent initialization: {}", e.getMessage());
                }

                // 3. Include any cached FHIR patients already stored in MongoDB
                if (cachedFhirPatientRepository != null) {
                    try {
                        cachedFhirPatientRepository.findAll().forEach(cached -> {
                            if (cached.getFhirPatientId() != null && !cached.getFhirPatientId().isBlank()) {
                                demoPatientIds.add(cached.getFhirPatientId().trim());
                            }
                            if (cached.getSourcePatientId() != null && !cached.getSourcePatientId().isBlank()) {
                                demoPatientIds.add(cached.getSourcePatientId().trim());
                            }
                        });
                    } catch (Exception e) {
                        log.debug("Could not read cached FHIR patients for consent initialization: {}", e.getMessage());
                    }
                }

                // 4. Seed consent idempotently for each patient ID
                for (String patientId : demoPatientIds) {
                    seedConsentIfNotExists(consentRepository, patientId);
                }
            } catch (Exception e) {
                log.warn("Failed to initialize consents - MongoDB may not be available yet: {}", e.getMessage());
            }
        };
    }

    private void seedConsentIfNotExists(ConsentRepository consentRepository, String patientId) {
        if (consentRepository.findByPatientId(patientId).isEmpty()) {
            Consent consent = new Consent(
                    patientId,
                    "doctor1",
                    "Clinical Review",
                    true
            );
            consentRepository.save(consent);
            log.info("Initialized development demo consent for patient: {}", patientId);
        }
    }
}
