package com.medisphere.medispherebackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.model.Consent;
import com.medisphere.medispherebackend.model.HealthMeasurement;
import com.medisphere.medispherebackend.model.Role;
import com.medisphere.medispherebackend.model.User;
import com.medisphere.medispherebackend.repository.CachedFhirPatientRepository;
import com.medisphere.medispherebackend.repository.ConsentRepository;
import com.medisphere.medispherebackend.repository.HealthMeasurementRepository;
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
import java.time.Instant;
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

    /**
     * Milestone 4 Part 4: Progress / Improvement
     *
     * Seeds initial historical measurements for demo patient P001:
     *   - Baseline (earlier): BP 135/85, Glucose 145, HbA1c 7.2
     *   - Latest (recent):   BP 128/80, Glucose 132, HbA1c 6.9
     * And a single measurement for P002 to test insufficient historical data.
     */
    @Bean
    CommandLineRunner initHealthMeasurements(HealthMeasurementRepository healthMeasurementRepository) {
        return args -> {
            try {
                if (healthMeasurementRepository.findByPatientIdOrderByCreatedAtDesc("P001").isEmpty()) {
                    // P001 Baseline measurements (earlier timestamp)
                    HealthMeasurement bpBaseline = new HealthMeasurement();
                    bpBaseline.setPatientId("P001");
                    bpBaseline.setMeasurementType("Blood Pressure");
                    bpBaseline.setSystolic(135.0);
                    bpBaseline.setDiastolic(85.0);
                    bpBaseline.setUnit("mmHg");
                    bpBaseline.setRecordedAt("2026-09-03T09:00:00Z");
                    bpBaseline.setCreatedAt(Instant.parse("2026-09-03T09:00:00Z"));
                    bpBaseline.setSource("MANUAL");
                    bpBaseline.setNotes("Baseline clinic checkup");
                    healthMeasurementRepository.save(bpBaseline);

                    HealthMeasurement gluBaseline = new HealthMeasurement();
                    gluBaseline.setPatientId("P001");
                    gluBaseline.setMeasurementType("Blood Glucose");
                    gluBaseline.setValue(145.0);
                    gluBaseline.setUnit("mg/dL");
                    gluBaseline.setRecordedAt("2026-09-03T09:00:00Z");
                    gluBaseline.setCreatedAt(Instant.parse("2026-09-03T09:00:00Z"));
                    gluBaseline.setSource("MANUAL");
                    gluBaseline.setNotes("Fasting baseline blood test");
                    healthMeasurementRepository.save(gluBaseline);

                    HealthMeasurement hba1cBaseline = new HealthMeasurement();
                    hba1cBaseline.setPatientId("P001");
                    hba1cBaseline.setMeasurementType("HbA1c");
                    hba1cBaseline.setValue(7.2);
                    hba1cBaseline.setUnit("%");
                    hba1cBaseline.setRecordedAt("2026-09-03T09:00:00Z");
                    hba1cBaseline.setCreatedAt(Instant.parse("2026-09-03T09:00:00Z"));
                    hba1cBaseline.setSource("MANUAL");
                    hba1cBaseline.setNotes("Quarterly lab baseline");
                    healthMeasurementRepository.save(hba1cBaseline);

                    // P001 Latest measurements (recent timestamp)
                    HealthMeasurement bpLatest = new HealthMeasurement();
                    bpLatest.setPatientId("P001");
                    bpLatest.setMeasurementType("Blood Pressure");
                    bpLatest.setSystolic(128.0);
                    bpLatest.setDiastolic(80.0);
                    bpLatest.setUnit("mmHg");
                    bpLatest.setRecordedAt("2026-09-20T10:00:00Z");
                    bpLatest.setCreatedAt(Instant.parse("2026-09-20T10:00:00Z"));
                    bpLatest.setSource("MANUAL");
                    bpLatest.setNotes("Follow-up measurement after care plan implementation");
                    healthMeasurementRepository.save(bpLatest);

                    HealthMeasurement gluLatest = new HealthMeasurement();
                    gluLatest.setPatientId("P001");
                    gluLatest.setMeasurementType("Blood Glucose");
                    gluLatest.setValue(132.0);
                    gluLatest.setUnit("mg/dL");
                    gluLatest.setRecordedAt("2026-09-20T10:00:00Z");
                    gluLatest.setCreatedAt(Instant.parse("2026-09-20T10:00:00Z"));
                    gluLatest.setSource("MANUAL");
                    gluLatest.setNotes("Morning post-diet fasting check");
                    healthMeasurementRepository.save(gluLatest);

                    HealthMeasurement hba1cLatest = new HealthMeasurement();
                    hba1cLatest.setPatientId("P001");
                    hba1cLatest.setMeasurementType("HbA1c");
                    hba1cLatest.setValue(6.9);
                    hba1cLatest.setUnit("%");
                    hba1cLatest.setRecordedAt("2026-09-20T10:00:00Z");
                    hba1cLatest.setCreatedAt(Instant.parse("2026-09-20T10:00:00Z"));
                    hba1cLatest.setSource("MANUAL");
                    hba1cLatest.setNotes("Follow-up HbA1c lab result");
                    healthMeasurementRepository.save(hba1cLatest);

                    log.info("Initialized baseline & latest health measurements for P001 in MongoDB");
                }

                // Seed single measurement for P002 to test insufficient historical data
                if (healthMeasurementRepository.findByPatientIdOrderByCreatedAtDesc("P002").isEmpty()) {
                    HealthMeasurement p2Single = new HealthMeasurement();
                    p2Single.setPatientId("P002");
                    p2Single.setMeasurementType("Blood Glucose");
                    p2Single.setValue(140.0);
                    p2Single.setUnit("mg/dL");
                    p2Single.setRecordedAt("2026-09-21T09:30:00Z");
                    p2Single.setCreatedAt(Instant.parse("2026-09-21T09:30:00Z"));
                    p2Single.setSource("MANUAL");
                    p2Single.setNotes("Single initial test");
                    healthMeasurementRepository.save(p2Single);
                    log.info("Initialized single health measurement for P002 in MongoDB");
                }
            } catch (Exception e) {
                log.warn("Failed to initialize health measurements - MongoDB may not be available yet: {}", e.getMessage());
            }
        };
    }
}
