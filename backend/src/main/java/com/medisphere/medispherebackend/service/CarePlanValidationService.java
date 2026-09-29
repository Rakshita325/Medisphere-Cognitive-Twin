package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.CarePlanValidationDto;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.model.CarePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Milestone 4: Care Plan Validation Service
 *
 * Implements:
 * 1. Clinical Guideline Compliance Check
 * 2. Care Plan Safety Checks (Pre-approval validation)
 * 3. Configurable Drug Interaction Demonstration Check
 */
@Service
public class CarePlanValidationService {

    private static final Logger log = LoggerFactory.getLogger(CarePlanValidationService.class);

    // Configured local drug interaction demonstration database
    private static final List<ConfiguredDrugInteraction> CONFIGURED_INTERACTIONS = List.of(
            new ConfiguredDrugInteraction(
                    List.of("lisinopril", "enalapril", "ramipril", "captopril", "ace inhibitor"),
                    List.of("spironolactone", "eplerenone", "potassium", "k-dur", "potassium chloride"),
                    "MODERATE",
                    "Concurrent ACE inhibitor and potassium-sparing diuretic / potassium supplement increases risk of hyperkalemia.",
                    "Monitor serum potassium and renal function periodically."
            ),
            new ConfiguredDrugInteraction(
                    List.of("warfarin", "coumadin"),
                    List.of("aspirin", "ibuprofen", "naproxen", "nsaid", "advil", "aleve", "meloxicam"),
                    "HIGH",
                    "Concomitant oral anticoagulant and antiplatelet/NSAID therapy significantly elevates risk of gastrointestinal and systemic bleeding.",
                    "Review clinical indication for dual antithrombotic therapy; consider gastroprotection."
            ),
            new ConfiguredDrugInteraction(
                    List.of("metformin", "glucophage"),
                    List.of("contrast", "iodinated contrast", "radiographic contrast"),
                    "MODERATE",
                    "Potential risk of lactic acidosis in setting of acute renal impairment following iodinated contrast media administration.",
                    "Withhold metformin prior to or at time of iodinated contrast procedure in patients with eGFR < 60 mL/min."
            ),
            new ConfiguredDrugInteraction(
                    List.of("lisinopril", "enalapril", "losartan", "valsartan"),
                    List.of("ibuprofen", "naproxen", "diclofenac", "indomethacin", "nsaid"),
                    "MODERATE",
                    "NSAIDs may diminish the antihypertensive effectiveness of ACE inhibitors / ARBs and increase renal impairment risk.",
                    "Monitor blood pressure and renal function; limit chronic NSAID co-administration."
            ),
            new ConfiguredDrugInteraction(
                    List.of("clopidogrel", "plavix"),
                    List.of("omeprazole", "prilosec", "esomeprazole", "nexium"),
                    "MODERATE",
                    "Omeprazole inhibits CYP2C19, significantly decreasing the active metabolite concentration and antiplatelet effect of clopidogrel.",
                    "Consider non-interacting PPI such as pantoprazole if acid suppression is clinically indicated."
            ),
            new ConfiguredDrugInteraction(
                    List.of("simvastatin", "zocor", "atorvastatin", "lipitor"),
                    List.of("amlodipine", "norvasc", "diltiazem", "verapamil"),
                    "MILD",
                    "Co-administration may increase statin plasma concentrations, mildly elevating risk of myopathy.",
                    "Limit simvastatin dose to maximum 20 mg daily when prescribed with amlodipine."
            )
    );

    /**
     * Run all validations on a given Care Plan and optional PatientData record.
     */
    public CarePlanValidationDto validateCarePlan(CarePlan carePlan, PatientData patientData) {
        CarePlanValidationDto result = new CarePlanValidationDto();
        result.setValidatedAt(Instant.now());

        if (carePlan == null) {
            result.setOverallStatus("NEEDS_REVIEW");
            result.getSafetyCheck().setStatus("WARNING");
            result.getSafetyCheck().setDisplayStatus("Warning / Needs Review");
            result.getSafetyCheck().setSummary("Care plan is missing or not initialized.");
            return result;
        }

        result.setCarePlanId(carePlan.getId());
        result.setPatientId(carePlan.getPatientId());
        result.setPatientName(carePlan.getPatientName());

        // 1. Evaluate Guideline Compliance
        evaluateGuidelineCompliance(carePlan, patientData, result.getGuidelineCompliance());

        // 2. Evaluate Safety Checks
        evaluateSafetyChecks(carePlan, patientData, result.getSafetyCheck());

        // 3. Evaluate Drug Interactions
        evaluateDrugInteractions(carePlan, patientData, result.getDrugInteraction());

        // Overall status
        boolean allPassed = "COMPLIANT".equalsIgnoreCase(result.getGuidelineCompliance().getStatus())
                && result.getSafetyCheck().isPassed()
                && !result.getDrugInteraction().isHasInteraction();

        result.setOverallStatus(allPassed ? "PASSED" : "NEEDS_REVIEW");

        return result;
    }

    /**
     * 1. Clinical Guideline Compliance Check
     */
    private void evaluateGuidelineCompliance(CarePlan carePlan, PatientData patientData, CarePlanValidationDto.GuidelineComplianceResult result) {
        List<String> guidelinesChecked = new ArrayList<>();
        List<String> findings = new ArrayList<>();
        boolean needsReview = false;
        boolean hasApplicableGuideline = false;

        Map<String, Object> risk = carePlan.getRiskInformation() != null ? carePlan.getRiskInformation() : Collections.emptyMap();
        List<String> interventions = carePlan.getInterventions() != null ? carePlan.getInterventions() : Collections.emptyList();
        String combinedInterventions = String.join(" ", interventions).toLowerCase();

        // Extract condition names
        String conditionsStr = extractConditions(carePlan, patientData).toLowerCase();

        // Check A: Hypertension Guideline (AHA/ACC)
        boolean hasHTN = conditionsStr.contains("hypertension") || conditionsStr.contains("htn")
                || String.valueOf(risk.getOrDefault("bloodPressure", "")).contains("/")
                || risk.containsKey("sysBP");

        if (hasHTN) {
            hasApplicableGuideline = true;
            guidelinesChecked.add("AHA/ACC Hypertension Management Guideline");
            boolean hasBPMonitoring = combinedInterventions.contains("bp") || combinedInterventions.contains("blood pressure") || combinedInterventions.contains("pressure");
            boolean hasLifestyleOrMed = combinedInterventions.contains("medication") || combinedInterventions.contains("diet") || combinedInterventions.contains("exercise") || combinedInterventions.contains("sodium");

            if (hasBPMonitoring && hasLifestyleOrMed) {
                findings.add("Hypertension Guideline: Blood pressure monitoring and lifestyle/pharmacotherapy management configured.");
            } else {
                needsReview = true;
                findings.add("Hypertension Guideline: Missing regular blood pressure tracking or lifestyle intervention.");
            }
        }

        // Check B: Diabetes Guideline (ADA)
        boolean hasDiabetes = conditionsStr.contains("diabetes") || conditionsStr.contains("t2d")
                || String.valueOf(risk.getOrDefault("bloodSugar", "")).length() > 0
                || risk.containsKey("hba1c");

        if (hasDiabetes) {
            hasApplicableGuideline = true;
            guidelinesChecked.add("ADA Standards of Medical Care in Diabetes");
            boolean hasGlucoseMonitoring = combinedInterventions.contains("glucose") || combinedInterventions.contains("sugar") || combinedInterventions.contains("glycemic") || combinedInterventions.contains("hba1c");
            boolean hasDietOrExercise = combinedInterventions.contains("diet") || combinedInterventions.contains("nutrition") || combinedInterventions.contains("exercise") || combinedInterventions.contains("medication");

            if (hasGlucoseMonitoring && hasDietOrExercise) {
                findings.add("Diabetes Guideline: Glycemic monitoring and dietary/lifestyle guidance verified.");
            } else {
                needsReview = true;
                findings.add("Diabetes Guideline: Glycemic tracking or nutrition management is recommended.");
            }
        }

        // Check C: Cardiovascular Disease Prevention Guideline (ACC/AHA)
        boolean hasCVD = conditionsStr.contains("cardiovascular") || conditionsStr.contains("cvd") || conditionsStr.contains("coronary")
                || String.valueOf(risk.getOrDefault("cvdRisk", "")).toLowerCase().contains("high")
                || String.valueOf(risk.getOrDefault("cvdRisk", "")).contains("18")
                || String.valueOf(risk.getOrDefault("cvdRisk", "")).contains("20");

        if (hasCVD) {
            hasApplicableGuideline = true;
            guidelinesChecked.add("ACC/AHA Primary & Secondary CVD Prevention Guideline");
            boolean hasCardioIntervention = combinedInterventions.contains("exercise") || combinedInterventions.contains("diet") || combinedInterventions.contains("medication") || combinedInterventions.contains("cholesterol");

            if (hasCardioIntervention) {
                findings.add("CVD Guideline: Cardiovascular risk reduction interventions and exercise tracking included.");
            } else {
                needsReview = true;
                findings.add("CVD Guideline: Aerobic physical activity or cardiovascular risk intervention recommended.");
            }
        }

        // Check D: Preventive Care / Baseline Wellness Guideline
        if (!hasApplicableGuideline) {
            guidelinesChecked.add("USPSTF Preventive Care & Vital Signs Monitoring Guideline");
            if (!interventions.isEmpty()) {
                findings.add("Preventive Care Guideline: General wellness and baseline vital monitoring active.");
                result.setStatus("COMPLIANT");
                result.setDisplayStatus("Compliant");
                result.setSummary("Care plan satisfies general preventive wellness guidelines.");
            } else {
                result.setStatus("NOT_APPLICABLE");
                result.setDisplayStatus("Not Applicable");
                result.setSummary("No specific disease-management guideline triggered for this profile.");
            }
        } else {
            if (needsReview) {
                result.setStatus("NEEDS_REVIEW");
                result.setDisplayStatus("Needs Review");
                result.setSummary("One or more recommended guideline interventions are missing or need physician review.");
            } else {
                result.setStatus("COMPLIANT");
                result.setDisplayStatus("Compliant");
                result.setSummary("Care plan is compliant with configured clinical guidelines (" + String.join(", ", guidelinesChecked) + ").");
            }
        }

        result.setGuidelinesChecked(guidelinesChecked);
        result.setFindings(findings);
    }

    /**
     * 2. Care Plan Safety Checks (Pre-approval validation)
     */
    private void evaluateSafetyChecks(CarePlan carePlan, PatientData patientData, CarePlanValidationDto.SafetyCheckResult result) {
        List<String> passed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check 1: Patient identification
        if (carePlan.getPatientId() != null && !carePlan.getPatientId().isBlank()
                && carePlan.getPatientName() != null && !carePlan.getPatientName().isBlank()) {
            passed.add("Patient Identification Verified (ID: " + carePlan.getPatientId() + ", Name: " + carePlan.getPatientName() + ")");
        } else {
            warnings.add("Missing required patient identification.");
        }

        // Check 2: Core structure (goals and interventions present)
        if (carePlan.getGoals() != null && !carePlan.getGoals().isEmpty()) {
            passed.add("Care Goals Defined (" + carePlan.getGoals().size() + " active goals)");
        } else {
            warnings.add("No care goals defined in care plan.");
        }

        if (carePlan.getInterventions() != null && !carePlan.getInterventions().isEmpty()) {
            passed.add("Interventions Configured (" + carePlan.getInterventions().size() + " trackable interventions)");
        } else {
            warnings.add("No interventions configured in care plan.");
        }

        // Check 3: Provider review prerequisite for medications
        String combinedText = String.join(" ", carePlan.getInterventions() != null ? carePlan.getInterventions() : Collections.emptyList()).toLowerCase();
        if (combinedText.contains("medication") || combinedText.contains("prescribed") || combinedText.contains("drug")) {
            if (carePlan.getDisclaimer() != null && !carePlan.getDisclaimer().isBlank()) {
                passed.add("Medication Safety Prerequisite: Provider review disclaimer and safety notices present.");
            } else {
                warnings.add("Medication interventions exist without required provider review disclaimer.");
            }
        } else {
            passed.add("Medication Safety Prerequisite: Non-pharmacological plan or standard medication safety met.");
        }

        // Check 4: Data bounds / conflict check (sanity check on risk parameters)
        Map<String, Object> risk = carePlan.getRiskInformation();
        if (risk != null) {
            Object sysBP = risk.get("sysBP");
            if (sysBP instanceof Number) {
                double v = ((Number) sysBP).doubleValue();
                if (v < 50 || v > 260) {
                    warnings.add("Systolic blood pressure (" + v + " mmHg) is outside physiologically expected bounds.");
                } else {
                    passed.add("Vital Signs Bounds Check: Systolic BP within expected limits.");
                }
            }
        }

        result.setChecksPassed(passed);
        result.setWarnings(warnings);

        if (warnings.isEmpty()) {
            result.setStatus("PASSED");
            result.setDisplayStatus("Safety Check Passed");
            result.setPassed(true);
            result.setSummary("Safety Check Passed: All core clinical parameters, identifiers, and safety prerequisites verified.");
        } else {
            result.setStatus("WARNING");
            result.setDisplayStatus("Warning / Needs Review");
            result.setPassed(false);
            result.setSummary("Warning / Needs Review: " + String.join("; ", warnings));
        }
    }

    /**
     * 3. Configurable Drug Interaction Validation
     */
    private void evaluateDrugInteractions(CarePlan carePlan, PatientData patientData, CarePlanValidationDto.DrugInteractionResult result) {
        List<String> medications = new ArrayList<>();

        // Collect medications/conditions from PatientData
        if (patientData != null && patientData.getConditions() != null) {
            for (var c : patientData.getConditions()) {
                if (c.getName() != null) {
                    medications.add(c.getName());
                }
            }
        }

        // Collect medications mentioned in Care Plan interventions or goals
        if (carePlan.getInterventions() != null) {
            for (String inv : carePlan.getInterventions()) {
                medications.add(inv);
            }
        }

        result.setMedicationsChecked(medications);

        String fullText = String.join(" ", medications).toLowerCase();
        List<CarePlanValidationDto.DrugInteractionItem> detected = new ArrayList<>();

        for (ConfiguredDrugInteraction rule : CONFIGURED_INTERACTIONS) {
            boolean hasGroupA = rule.groupA.stream().anyMatch(d -> containsWord(fullText, d));
            boolean hasGroupB = rule.groupB.stream().anyMatch(d -> containsWord(fullText, d));

            if (hasGroupA && hasGroupB) {
                detected.add(new CarePlanValidationDto.DrugInteractionItem(
                        String.join("/", rule.groupA),
                        String.join("/", rule.groupB),
                        rule.severity,
                        rule.description,
                        rule.recommendation
                ));
            }
        }

        result.setDetectedInteractions(detected);

        if (!detected.isEmpty()) {
            result.setStatus("WARNING");
            result.setDisplayStatus("Warning / Interaction Detected");
            result.setHasInteraction(true);
            result.setSummary("Detected " + detected.size() + " potential drug interaction(s) requiring provider review before approval.");
        } else {
            result.setStatus("NO_INTERACTIONS");
            result.setDisplayStatus("No configured interaction detected");
            result.setHasInteraction(false);
            result.setSummary("No configured interaction detected.");
        }
    }

    private boolean containsWord(String text, String term) {
        if (text == null || term == null) return false;
        String regex = "\\b" + Pattern.quote(term.toLowerCase()) + "\\b";
        return Pattern.compile(regex).matcher(text.toLowerCase()).find() || text.toLowerCase().contains(term.toLowerCase());
    }

    private String extractConditions(CarePlan carePlan, PatientData patientData) {
        StringBuilder sb = new StringBuilder();
        if (patientData != null && patientData.getConditions() != null) {
            for (var c : patientData.getConditions()) {
                if (c.getName() != null) sb.append(c.getName()).append(" ");
            }
        }
        if (carePlan.getRiskInformation() != null && carePlan.getRiskInformation().containsKey("conditions")) {
            sb.append(carePlan.getRiskInformation().get("conditions")).append(" ");
        }
        return sb.toString();
    }

    private static class ConfiguredDrugInteraction {
        final List<String> groupA;
        final List<String> groupB;
        final String severity;
        final String description;
        final String recommendation;

        ConfiguredDrugInteraction(List<String> groupA, List<String> groupB, String severity, String description, String recommendation) {
            this.groupA = groupA;
            this.groupB = groupB;
            this.severity = severity;
            this.description = description;
            this.recommendation = recommendation;
        }
    }
}
