package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.CarePlanValidationDto;
import com.medisphere.medispherebackend.dto.ConditionData;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.model.CarePlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CarePlanValidationServiceTest {

    private CarePlanValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new CarePlanValidationService();
    }

    @Test
    void testHypertensionGuidelineComplianceAndSafetyPass() {
        CarePlan plan = new CarePlan();
        plan.setId("CP-101");
        plan.setPatientId("P001");
        plan.setPatientName("John Doe");
        plan.setGoals(List.of(new CarePlan.CareGoal("Reduce BP", "Target < 130/80", "BP", "IN_PROGRESS")));
        plan.setInterventions(List.of(
                "Take prescribed medication as directed (provider reviewed)",
                "Monitor BP regularly",
                "Follow low sodium diet",
                "Exercise regularly"
        ));
        plan.setDisclaimer("STUDENT/DEMO NOTICE: AI-generated care recommendations are intended for educational demonstration.");

        PatientData pd = new PatientData();
        pd.setPatientId("P001");
        ConditionData cond = new ConditionData();
        cond.setName("Essential Hypertension");
        pd.setConditions(List.of(cond));

        CarePlanValidationDto result = validationService.validateCarePlan(plan, pd);

        assertNotNull(result);
        assertEquals("PASSED", result.getOverallStatus());

        // Guideline compliance
        assertEquals("COMPLIANT", result.getGuidelineCompliance().getStatus());
        assertEquals("Compliant", result.getGuidelineCompliance().getDisplayStatus());
        assertTrue(result.getGuidelineCompliance().getGuidelinesChecked().stream().anyMatch(g -> g.contains("Hypertension")));

        // Safety check
        assertTrue(result.getSafetyCheck().isPassed());
        assertEquals("PASSED", result.getSafetyCheck().getStatus());
        assertEquals("Safety Check Passed", result.getSafetyCheck().getDisplayStatus());

        // Drug interaction
        assertFalse(result.getDrugInteraction().isHasInteraction());
        assertEquals("NO_INTERACTIONS", result.getDrugInteraction().getStatus());
        assertEquals("No configured interaction detected", result.getDrugInteraction().getDisplayStatus());
    }

    @Test
    void testDrugInteractionDetection() {
        CarePlan plan = new CarePlan();
        plan.setId("CP-102");
        plan.setPatientId("P002");
        plan.setPatientName("Jane Doe");
        plan.setGoals(List.of(new CarePlan.CareGoal("Manage Heart & Kidney", "Stabilize vitals", "Vitals", "IN_PROGRESS")));
        plan.setInterventions(List.of(
                "Take Lisinopril 20mg daily",
                "Take Spironolactone 25mg daily for heart failure",
                "Monitor BP and potassium"
        ));
        plan.setDisclaimer("Educational demo disclaimer");

        PatientData pd = new PatientData();
        pd.setPatientId("P002");
        ConditionData c1 = new ConditionData();
        c1.setName("Hypertension - Lisinopril");
        ConditionData c2 = new ConditionData();
        c2.setName("Heart Failure - Spironolactone");
        pd.setConditions(List.of(c1, c2));

        CarePlanValidationDto result = validationService.validateCarePlan(plan, pd);

        assertNotNull(result);
        assertTrue(result.getDrugInteraction().isHasInteraction());
        assertEquals("WARNING", result.getDrugInteraction().getStatus());
        assertEquals("Warning / Interaction Detected", result.getDrugInteraction().getDisplayStatus());
        assertEquals(1, result.getDrugInteraction().getDetectedInteractions().size());
        assertTrue(result.getDrugInteraction().getDetectedInteractions().get(0).getDescription().contains("hyperkalemia"));
        assertEquals("NEEDS_REVIEW", result.getOverallStatus());
    }

    @Test
    void testSafetyCheckFailsOnMissingPatientId() {
        CarePlan plan = new CarePlan();
        plan.setId("CP-103");
        plan.setPatientId("");
        plan.setPatientName("");
        plan.setGoals(Collections.emptyList());
        plan.setInterventions(Collections.emptyList());

        CarePlanValidationDto result = validationService.validateCarePlan(plan, null);

        assertNotNull(result);
        assertFalse(result.getSafetyCheck().isPassed());
        assertEquals("WARNING", result.getSafetyCheck().getStatus());
        assertEquals("Warning / Needs Review", result.getSafetyCheck().getDisplayStatus());
        assertEquals("NEEDS_REVIEW", result.getOverallStatus());
    }
}
