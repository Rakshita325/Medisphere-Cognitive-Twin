package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.PatientData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PatientDataServiceTest {

    private PatientDataService patientDataService;

    @BeforeEach
    void setUp() {
        patientDataService = new PatientDataService();
    }

    @Test
    void testGetAllPatients() {
        List<PatientData> patients = patientDataService.getAllPatients();
        assertNotNull(patients);
        assertFalse(patients.isEmpty());
        assertEquals(10, patients.size());
    }

    @Test
    void testGetPatientP001Rahul() {
        Optional<PatientData> opt = patientDataService.getPatientById("P001");
        assertTrue(opt.isPresent(), "P001 should exist");
        PatientData p = opt.get();
        assertEquals("P001", p.getPatientId());
        assertEquals("Rahul", p.getPersonalDetails().getFirstName());
        assertEquals("Sharma", p.getPersonalDetails().getLastName());
        assertEquals(2, p.getConditions().size());
        assertEquals(4, p.getVitals().size());
        assertEquals(3, p.getLabResults().size());
        assertNotNull(p.getWearableData());
        assertEquals(6540, p.getWearableData().getSteps());
        assertEquals(6.5, p.getWearableData().getSleepHours());
    }

    @Test
    void testGetPatientP002Priya() {
        Optional<PatientData> opt = patientDataService.getPatientById("P002");
        assertTrue(opt.isPresent(), "P002 should exist");
        PatientData p = opt.get();
        assertEquals("P002", p.getPatientId());
        assertEquals("Priya", p.getPersonalDetails().getFirstName());
        assertEquals("Nair", p.getPersonalDetails().getLastName());
        assertEquals(1, p.getConditions().size());
        assertEquals("Asthma", p.getConditions().get(0).getName());
        assertEquals(8240, p.getWearableData().getSteps());
        // Verify P002 does NOT have HbA1c
        boolean hasHbA1c = p.getLabResults().stream().anyMatch(l -> "HbA1c".equalsIgnoreCase(l.getTest()));
        assertFalse(hasHbA1c, "P002 must NOT have HbA1c");
    }

    @Test
    void testGetPatientP003Arjun() {
        Optional<PatientData> opt = patientDataService.getPatientById("P003");
        assertTrue(opt.isPresent(), "P003 should exist");
        PatientData p = opt.get();
        assertEquals("P003", p.getPatientId());
        assertEquals("Arjun", p.getPersonalDetails().getFirstName());
        assertEquals("Patel", p.getPersonalDetails().getLastName());
        assertEquals(4320, p.getWearableData().getSteps());
        boolean hasLdl = p.getLabResults().stream().anyMatch(l -> "LDL Cholesterol".equalsIgnoreCase(l.getTest()));
        assertTrue(hasLdl, "P003 must have LDL Cholesterol");
    }
}
