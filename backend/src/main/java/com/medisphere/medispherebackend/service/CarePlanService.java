package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.GenerateCarePlanRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.repository.CarePlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class CarePlanService {

    private static final Logger log = LoggerFactory.getLogger(CarePlanService.class);

    private final CarePlanRepository carePlanRepository;
    private final MLDemoPatientService mlDemoPatientService;
    private final PatientTwinService patientTwinService;
    private final PatientDataService patientDataService;
    private final TreatmentTrackingService treatmentTrackingService;

    @org.springframework.beans.factory.annotation.Autowired
    public CarePlanService(CarePlanRepository carePlanRepository,
                           MLDemoPatientService mlDemoPatientService,
                           PatientTwinService patientTwinService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) PatientDataService patientDataService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) @org.springframework.context.annotation.Lazy TreatmentTrackingService treatmentTrackingService) {
        this.carePlanRepository = carePlanRepository;
        this.mlDemoPatientService = mlDemoPatientService;
        this.patientTwinService = patientTwinService;
        this.patientDataService = patientDataService;
        this.treatmentTrackingService = treatmentTrackingService;
    }

    /**
     * Generates a personalized care plan based on patient vitals and risk data.
     */
    public CarePlan generateCarePlan(GenerateCarePlanRequest request) {
        String patientId = request.getPatientId();
        if (patientId == null || patientId.trim().isEmpty()) {
            patientId = "UNKNOWN";
        }

        String patientName = request.getPatientName();
        Map<String, Object> riskInfo = request.getRiskInformation() != null ?
                new HashMap<>(request.getRiskInformation()) : new HashMap<>();

        // If patientName or riskInfo are missing, attempt lookup from rich PatientData, ML Demo Patients, or PatientTwin
        if (patientDataService != null && patientDataService.getPatientById(patientId).isPresent()) {
            com.medisphere.medispherebackend.dto.PatientData pd = patientDataService.getPatientById(patientId).get();
            if (patientName == null || patientName.trim().isEmpty() || patientName.startsWith("Patient ")) {
                if (pd.getPersonalDetails() != null) {
                    String first = pd.getPersonalDetails().getFirstName() != null ? pd.getPersonalDetails().getFirstName() : "";
                    String last = pd.getPersonalDetails().getLastName() != null ? pd.getPersonalDetails().getLastName() : "";
                    String fullName = (first + " " + last).trim();
                    if (!fullName.isEmpty()) {
                        patientName = fullName;
                    }
                }
            }
            populateRiskFromPatientData(pd, riskInfo);
        } else if (mlDemoPatientService.isMLDemoPatient(patientId)) {
            Map<String, Object> mlPatient = mlDemoPatientService.getMLDemoPatient(patientId);
            if (mlPatient != null) {
                if (patientName == null || patientName.trim().isEmpty()) {
                    patientName = (String) mlPatient.get("displayName");
                }
                populateRiskFromMLPatient(mlPatient, riskInfo);
            }
        } else {
            PatientTwin twin = patientTwinService.getBySourcePatientId(patientId);
            if (twin != null) {
                if (patientName == null || patientName.trim().isEmpty()) {
                    patientName = twin.getName();
                }
                populateRiskFromTwin(twin, riskInfo);
            }
        }

        if (patientName == null || patientName.trim().isEmpty()) {
            patientName = "Patient " + patientId;
        }

        // Generate Clinical Goals, Interventions, and Monitoring Instructions
        List<CarePlan.CareGoal> goals = generateGoals(riskInfo, request.getCustomGoals());
        List<String> interventions = generateInterventions(riskInfo, request.getCustomInterventions());
        List<String> monitoring = generateMonitoringInstructions(riskInfo, request.getCustomMonitoring());

        String initialStatus = "PENDING_APPROVAL";

        CarePlan carePlan = new CarePlan(
                patientId,
                patientName,
                goals,
                interventions,
                monitoring,
                riskInfo,
                initialStatus
        );

        CarePlan savedPlan = carePlanRepository.save(carePlan);
        if (treatmentTrackingService != null) {
            try {
                treatmentTrackingService.initializeTrackingForCarePlan(savedPlan);
            } catch (Exception e) {
                log.error("Failed to auto-create treatment tracking records for care plan {}: {}", savedPlan.getId(), e.getMessage());
            }
        }
        log.info("Generated care plan for patient ID: {}, Name: {}", patientId, patientName);
        return savedPlan;
    }

    private void populateRiskFromPatientData(com.medisphere.medispherebackend.dto.PatientData pd, Map<String, Object> riskInfo) {
        if (pd.getConditions() != null && !riskInfo.containsKey("conditions")) {
            String conds = pd.getConditions().stream()
                    .map(com.medisphere.medispherebackend.dto.ConditionData::getName)
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(null);
            if (conds != null && !conds.isEmpty()) {
                riskInfo.put("conditions", conds);
            }
        }
        if (pd.getVitals() != null) {
            for (com.medisphere.medispherebackend.dto.VitalData v : pd.getVitals()) {
                if ("Blood Pressure".equalsIgnoreCase(v.getType()) && !riskInfo.containsKey("bloodPressure")) {
                    if (v.getSystolic() != null && v.getDiastolic() != null) {
                        riskInfo.put("bloodPressure", (int) Math.round(v.getSystolic()) + "/" + (int) Math.round(v.getDiastolic()) + " " + (v.getUnit() != null ? v.getUnit() : "mmHg"));
                        riskInfo.put("sysBP", v.getSystolic());
                        riskInfo.put("diaBP", v.getDiastolic());
                    }
                } else if ("Heart Rate".equalsIgnoreCase(v.getType()) && !riskInfo.containsKey("heartRate")) {
                    if (v.getValue() != null) {
                        riskInfo.put("heartRate", v.getValue() + " " + (v.getUnit() != null ? v.getUnit() : "bpm"));
                    }
                } else if ("SpO2".equalsIgnoreCase(v.getType()) && !riskInfo.containsKey("spo2")) {
                    if (v.getValue() != null) {
                        riskInfo.put("spo2", v.getValue() + (v.getUnit() != null ? v.getUnit() : "%"));
                    }
                } else if ("Temperature".equalsIgnoreCase(v.getType()) && !riskInfo.containsKey("temperature")) {
                    if (v.getValue() != null) {
                        riskInfo.put("temperature", v.getValue() + "°" + (v.getUnit() != null ? v.getUnit() : "F"));
                    }
                }
            }
        }
        if (pd.getLabResults() != null) {
            for (com.medisphere.medispherebackend.dto.LabResult lab : pd.getLabResults()) {
                if (("Blood Glucose".equalsIgnoreCase(lab.getTest()) || "Glucose".equalsIgnoreCase(lab.getTest())) && !riskInfo.containsKey("bloodSugar")) {
                    riskInfo.put("bloodSugar", lab.getValue() + " " + (lab.getUnit() != null ? lab.getUnit() : "mg/dL"));
                } else if ("HbA1c".equalsIgnoreCase(lab.getTest()) && !riskInfo.containsKey("hba1c")) {
                    riskInfo.put("hba1c", lab.getValue() + (lab.getUnit() != null ? lab.getUnit() : "%"));
                } else if (("Total Cholesterol".equalsIgnoreCase(lab.getTest()) || "Cholesterol".equalsIgnoreCase(lab.getTest())) && !riskInfo.containsKey("cholesterol")) {
                    riskInfo.put("cholesterol", lab.getValue() + " " + (lab.getUnit() != null ? lab.getUnit() : "mg/dL"));
                } else if (("LDL Cholesterol".equalsIgnoreCase(lab.getTest()) || "LDL".equalsIgnoreCase(lab.getTest())) && !riskInfo.containsKey("ldl")) {
                    riskInfo.put("ldl", lab.getValue() + " " + (lab.getUnit() != null ? lab.getUnit() : "mg/dL"));
                }
            }
        }
        if (pd.getWearableData() != null) {
            if (!riskInfo.containsKey("steps")) {
                riskInfo.put("steps", pd.getWearableData().getSteps());
            }
            if (!riskInfo.containsKey("sleepHours")) {
                riskInfo.put("sleepHours", pd.getWearableData().getSleepHours());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateRiskFromMLPatient(Map<String, Object> mlPatient, Map<String, Object> riskInfo) {
        Map<String, Object> cvd = (Map<String, Object>) mlPatient.get("cvdFeatures");
        Map<String, Object> diabetes = (Map<String, Object>) mlPatient.get("diabetesFeatures");

        if (cvd != null) {
            if (!riskInfo.containsKey("bloodPressure") && cvd.get("sysBP") != null && cvd.get("diaBP") != null) {
                riskInfo.put("bloodPressure", cvd.get("sysBP") + "/" + cvd.get("diaBP"));
            }
            if (!riskInfo.containsKey("sysBP")) riskInfo.put("sysBP", cvd.get("sysBP"));
            if (!riskInfo.containsKey("diaBP")) riskInfo.put("diaBP", cvd.get("diaBP"));
            if (!riskInfo.containsKey("totChol") && cvd.get("totChol") != null) {
                riskInfo.put("totChol", cvd.get("totChol") + " mg/dL");
            }
        }

        if (diabetes != null || cvd != null) {
            Object glucoseVal = cvd != null ? cvd.get("glucose") : (diabetes != null ? diabetes.get("Glucose") : null);
            if (!riskInfo.containsKey("bloodSugar") && glucoseVal != null) {
                riskInfo.put("bloodSugar", glucoseVal + " mg/dL");
            }
        }
    }

    private void populateRiskFromTwin(PatientTwin twin, Map<String, Object> riskInfo) {
        if (!riskInfo.containsKey("diagnosis") && twin.getDiagnosis() != null) {
            riskInfo.put("diagnosis", twin.getDiagnosis());
        }
        if (!riskInfo.containsKey("age")) {
            riskInfo.put("age", twin.getAge());
        }
        if (!riskInfo.containsKey("gender")) {
            riskInfo.put("gender", twin.getGender());
        }
    }

    private List<CarePlan.CareGoal> generateGoals(Map<String, Object> riskInfo, List<String> customGoals) {
        List<CarePlan.CareGoal> goals = new ArrayList<>();

        // Check BP threshold
        String bpStr = String.valueOf(riskInfo.getOrDefault("bloodPressure", ""));
        Object sysBPObj = riskInfo.get("sysBP");
        double sysBP = sysBPObj instanceof Number ? ((Number) sysBPObj).doubleValue() : 0.0;
        if (sysBP == 0.0 && bpStr.contains("/")) {
            try {
                sysBP = Double.parseDouble(bpStr.split("/")[0].trim());
            } catch (Exception ignored) {}
        }

        if (sysBP >= 130 || bpStr.contains("150") || bpStr.contains("140")) {
            goals.add(new CarePlan.CareGoal(
                    "Reduce blood pressure toward target range",
                    "Lower systolic and diastolic blood pressure toward configured clinical target (< 130/80 mmHg).",
                    "BP < 130/80 mmHg",
                    "IN_PROGRESS"
            ));
        }

        // Check Blood Sugar threshold
        String bsStr = String.valueOf(riskInfo.getOrDefault("bloodSugar", ""));
        String hba1cStr = String.valueOf(riskInfo.getOrDefault("hba1c", ""));
        if (bsStr.contains("180") || bsStr.contains("140") || hba1cStr.contains("7") || hba1cStr.contains("8")) {
            goals.add(new CarePlan.CareGoal(
                    "Improve blood sugar control",
                    "Optimize glycemic control and reduce risk of microvascular and macrovascular diabetic complications.",
                    "Fasting Glucose < 120 mg/dL, HbA1c < 7.0%",
                    "IN_PROGRESS"
            ));
        }

        // Check CVD risk threshold
        String cvdRiskStr = String.valueOf(riskInfo.getOrDefault("cvdRisk", ""));
        if (cvdRiskStr.contains("18") || cvdRiskStr.contains("20") || cvdRiskStr.toLowerCase().contains("high")) {
            goals.add(new CarePlan.CareGoal(
                    "Reduce 10-year cardiovascular risk score",
                    "Implement lifestyle and therapeutic interventions to mitigate overall 10-year CVD risk.",
                    "CVD Risk < 10%",
                    "IN_PROGRESS"
            ));
        }

        // Default baseline goal if no specific flags
        if (goals.isEmpty()) {
            goals.add(new CarePlan.CareGoal(
                    "Maintain baseline health and vitals stability",
                    "Ensure ongoing stability of vital signs, physical activity, and wellness parameters.",
                    "Normal Vitals Range",
                    "IN_PROGRESS"
            ));
        }

        // Append custom goals
        if (customGoals != null) {
            for (String g : customGoals) {
                if (g != null && !g.trim().isEmpty()) {
                    goals.add(new CarePlan.CareGoal(g.trim(), "Clinician defined target", "Custom Target", "IN_PROGRESS"));
                }
            }
        }

        return goals;
    }

    private List<String> generateInterventions(Map<String, Object> riskInfo, List<String> customInterventions) {
        List<String> interventions = new ArrayList<>();
        interventions.add("Take prescribed medication as directed (provider reviewed)");
        interventions.add("Monitor BP regularly");
        interventions.add("Monitor blood sugar");
        interventions.add("Follow a healthy diet");
        interventions.add("Exercise regularly");

        if (customInterventions != null) {
            for (String item : customInterventions) {
                if (item != null && !item.trim().isEmpty() && !interventions.contains(item.trim())) {
                    interventions.add(item.trim());
                }
            }
        }
        return interventions;
    }

    private List<String> generateMonitoringInstructions(Map<String, Object> riskInfo, List<String> customMonitoring) {
        List<String> instructions = new ArrayList<>();
        instructions.add("Record BP regularly");
        instructions.add("Record glucose readings");
        instructions.add("Review vitals and progress during follow-up");

        if (customMonitoring != null) {
            for (String item : customMonitoring) {
                if (item != null && !item.trim().isEmpty() && !instructions.contains(item.trim())) {
                    instructions.add(item.trim());
                }
            }
        }
        return instructions;
    }

    public List<CarePlan> getCarePlansByPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String pid = patientId.trim();
        List<CarePlan> plans = carePlanRepository.findByPatientIdIgnoreCaseOrderByCreatedAtDesc(pid);
        if (plans.isEmpty()) {
            plans = carePlanRepository.findByPatientIdOrderByCreatedAtDesc(pid);
        }
        return plans;
    }

    public Optional<CarePlan> getLatestCarePlanByPatientId(String patientId) {
        List<CarePlan> plans = getCarePlansByPatientId(patientId);
        if (plans.isEmpty()) {
            return Optional.empty();
        }
        // Return active/approved plan if present, otherwise latest created
        return Optional.of(plans.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()) || "APPROVED".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .orElse(plans.get(0)));
    }

    public List<CarePlan> getAllCarePlans() {
        return carePlanRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<CarePlan> getCarePlanById(String id) {
        return carePlanRepository.findById(id);
    }

    public Optional<CarePlan> updateCarePlan(String id, CarePlan updatedPlan) {
        return carePlanRepository.findById(id).map(existing -> {
            if (updatedPlan.getGoals() != null) existing.setGoals(updatedPlan.getGoals());
            if (updatedPlan.getInterventions() != null) existing.setInterventions(updatedPlan.getInterventions());
            if (updatedPlan.getMonitoringInstructions() != null) existing.setMonitoringInstructions(updatedPlan.getMonitoringInstructions());
            if (updatedPlan.getRiskInformation() != null) existing.setRiskInformation(updatedPlan.getRiskInformation());
            if (updatedPlan.getStatus() != null) existing.setStatus(updatedPlan.getStatus());
            existing.setUpdatedAt(Instant.now());
            return carePlanRepository.save(existing);
        });
    }

    public Optional<CarePlan> updateCarePlanStatus(String id, String status) {
        return carePlanRepository.findById(id).map(existing -> {
            existing.setStatus(status.toUpperCase());
            existing.setUpdatedAt(Instant.now());
            return carePlanRepository.save(existing);
        });
    }
}
