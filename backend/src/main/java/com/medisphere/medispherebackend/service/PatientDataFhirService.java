package com.medisphere.medispherebackend.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.medispherebackend.dto.ConditionData;
import com.medisphere.medispherebackend.dto.LabReport;
import com.medisphere.medispherebackend.dto.LabResult;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.dto.PersonalDetails;
import com.medisphere.medispherebackend.dto.VitalData;
import com.medisphere.medispherebackend.dto.WearableData;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class PatientDataFhirService {

    public static final String IDENTIFIER_SYSTEM = "urn:medisphere:patient-id";

    private final IGenericClient fhirClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PatientDataFhirService.class);

    public PatientDataFhirService(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public String importPatientsToFhir() {
        try (InputStream inputStream = new ClassPathResource("patient-data.json").getInputStream()) {
            List<PatientData> patients = objectMapper.readValue(inputStream, new TypeReference<>() { });
            ImportSummary summary = new ImportSummary();
            for (PatientData data : patients) {
                try {
                    importPatient(data, summary);
                } catch (Exception exception) {
                    summary.errors.add((data == null ? "unknown" : data.getPatientId()) + ": " + exception.getMessage());
                    if (isNetworkFailure(exception)) {
                        log.warn("FHIR host unreachable during seed import ({}); aborting remaining imports.", exception.getMessage());
                        break;
                    }
                }
            }
            return summary.message();
        } catch (Exception exception) {
            log.warn("FHIR import skipped: {}", exception.getMessage());
            return "FHIR import skipped: " + exception.getMessage();
        }
    }

    private boolean isNetworkFailure(Throwable t) {
        while (t != null) {
            if (t instanceof java.net.SocketException
                    || t instanceof java.net.ConnectException
                    || t instanceof java.net.UnknownHostException
                    || t.getClass().getName().contains("FhirClientConnectionException")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private void importPatient(PatientData data, ImportSummary summary) {
        if (data == null || isBlank(data.getPatientId())) {
            throw new IllegalArgumentException("patientId is required");
        }
        if (findExistingPatientId(data.getPatientId()) != null) {
            summary.skippedPatients++;
            return;
        }

        Patient patient = toPatient(data);
        MethodOutcome patientOutcome = fhirClient.create().resource(patient).execute();
        String fhirPatientId = patientOutcome.getId().getIdPart();
        String subject = "Patient/" + fhirPatientId;
        summary.patientCount++;

        if (data.getConditions() != null) {
            for (ConditionData conditionData : data.getConditions()) {
                Condition condition = new Condition();
                condition.setSubject(new Reference(subject));
                condition.setCode(textConcept(conditionData.getName()));
                condition.setClinicalStatus(textConcept(conditionData.getStatus()));
                if (!isBlank(conditionData.getDiagnosedDate())) {
                    condition.setOnset(new DateTimeType(conditionData.getDiagnosedDate()));
                }
                fhirClient.create().resource(condition).execute();
                summary.conditionCount++;
            }
        }

        List<String> labObservationIds = new ArrayList<>();
        if (data.getVitals() != null) {
            for (VitalData vital : data.getVitals()) {
                fhirClient.create().resource(toVitalObservation(vital, subject)).execute();
                summary.observationCount++;
            }
        }
        if (data.getLabResults() != null) {
            for (LabResult lab : data.getLabResults()) {
                MethodOutcome outcome = fhirClient.create().resource(toLabObservation(lab, subject)).execute();
                labObservationIds.add(outcome.getId().getIdPart());
                summary.observationCount++;
            }
        }
        if (data.getWearableData() != null) {
            WearableData wearable = data.getWearableData();
            fhirClient.create().resource(toWearableObservation("Steps", "41950-7", wearable.getSteps(), "steps", wearable.getRecordedAt(), subject)).execute();
            fhirClient.create().resource(toWearableObservation("Sleep duration", "93832-4", wearable.getSleepHours(), "h", wearable.getRecordedAt(), subject)).execute();
            summary.observationCount += 2;
        }
        if (data.getLabReport() != null) {
            fhirClient.create().resource(toDiagnosticReport(data.getLabReport(), subject, labObservationIds)).execute();
            summary.reportCount++;
        }
    }

    private Patient toPatient(PatientData data) {
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(IDENTIFIER_SYSTEM).setValue(data.getPatientId());
        PersonalDetails personal = data.getPersonalDetails();
        if (personal == null) throw new IllegalArgumentException("personalDetails is required");
        HumanName name = patient.addName();
        if (!isBlank(personal.getFirstName())) name.addGiven(personal.getFirstName());
        if (!isBlank(personal.getLastName())) name.setFamily(personal.getLastName());
        if (!isBlank(personal.getPhone())) patient.addTelecom().setValue(personal.getPhone());
        if (!isBlank(personal.getGender())) {
            patient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.fromCode(personal.getGender().toLowerCase(Locale.ROOT)));
        }
        if (!isBlank(personal.getBirthDate())) patient.setBirthDate(java.sql.Date.valueOf(LocalDate.parse(personal.getBirthDate())));
        return patient;
    }

    private Observation toVitalObservation(VitalData vital, String subject) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference(subject));
        String type = vital.getType() == null ? "Vital sign" : vital.getType();
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(codeFor(type)).setDisplay(type);
        observation.getCode().setText(type);
        setEffective(observation, vital.getRecordedAt());
        if ("Blood Pressure".equalsIgnoreCase(type)) {
            addComponent(observation, "8480-6", "Systolic blood pressure", vital.getSystolic());
            addComponent(observation, "8462-4", "Diastolic blood pressure", vital.getDiastolic());
        } else if (!isBlank(vital.getValue())) {
            observation.setValue(new Quantity().setValue(Double.parseDouble(vital.getValue())).setUnit(vital.getUnit()));
        }
        return observation;
    }

    private Observation toLabObservation(LabResult lab, String subject) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference(subject));
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(labCode(lab.getTest())).setDisplay(lab.getTest());
        observation.getCode().setText(lab.getTest());
        if (!isBlank(lab.getValue())) observation.setValue(new Quantity().setValue(Double.parseDouble(lab.getValue())).setUnit(lab.getUnit()));
        if (!isBlank(lab.getReferenceRange())) observation.getReferenceRangeFirstRep().setText(lab.getReferenceRange());
        setEffective(observation, !isBlank(lab.getRecordedAt()) ? lab.getRecordedAt() : lab.getDate());
        return observation;
    }

    private Observation toWearableObservation(String type, String code, double value, String unit, String recordedAt, String subject) {
        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference(subject));
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(code).setDisplay(type);
        observation.getCode().setText(type);
        observation.setValue(new Quantity().setValue(value).setUnit(unit));
        setEffective(observation, recordedAt);
        return observation;
    }

    private DiagnosticReport toDiagnosticReport(LabReport report, String subject, List<String> observationIds) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        diagnosticReport.setSubject(new Reference(subject));
        diagnosticReport.getCode().setText(report.getReportName());
        diagnosticReport.getCode().addCoding().setSystem(IDENTIFIER_SYSTEM).setCode(report.getReportId()).setDisplay(report.getReportName());
        if (!isBlank(report.getIssuedDate())) diagnosticReport.setIssued(parseDate(report.getIssuedDate()));
        for (String observationId : observationIds) diagnosticReport.addResult(new Reference("Observation/" + observationId));
        return diagnosticReport;
    }

    private String findExistingPatientId(String sourcePatientId) {
        Bundle bundle = fhirClient.search().forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(IDENTIFIER_SYSTEM, sourcePatientId))
                .returnBundle(Bundle.class).execute();
        if (bundle.hasEntry() && bundle.getEntryFirstRep().getResource() instanceof Patient patient) {
            return patient.getIdElement().getIdPart();
        }
        return null;
    }

    private void addComponent(Observation observation, String code, String display, Double value) {
        if (value != null) {
            Observation.ObservationComponentComponent component = observation.addComponent();
            component.getCode().addCoding().setSystem("http://loinc.org").setCode(code).setDisplay(display);
            component.setValue(new Quantity().setValue(value).setUnit("mmHg"));
        }
    }

    private void setEffective(Observation observation, String value) {
        if (!isBlank(value)) observation.setEffective(new DateTimeType(value));
    }

    private CodeableConcept textConcept(String value) {
        CodeableConcept concept = new CodeableConcept();
        if (!isBlank(value)) concept.setText(value);
        return concept;
    }

    private String codeFor(String type) {
        if (type.equalsIgnoreCase("Heart Rate")) return "8867-4";
        if (type.equalsIgnoreCase("SpO2")) return "59408-5";
        if (type.equalsIgnoreCase("Temperature")) return "8310-5";
        if (type.equalsIgnoreCase("Blood Pressure")) return "85354-9";
        return "8716-3";
    }

    private String labCode(String test) {
        if (test == null) return "unknown";
        if (test.equalsIgnoreCase("Blood Glucose")) return "2339-0";
        if (test.equalsIgnoreCase("HbA1c")) return "4548-4";
        if (test.equalsIgnoreCase("Total Cholesterol")) return "2093-3";
        if (test.equalsIgnoreCase("LDL Cholesterol")) return "13457-7";
        if (test.equalsIgnoreCase("Hemoglobin")) return "718-7";
        return "unknown";
    }

    private Date parseDate(String value) {
        if (value.length() == 10) return Date.from(LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC));
        return Date.from(Instant.parse(value));
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private static class ImportSummary {
        int patientCount;
        int skippedPatients;
        int conditionCount;
        int observationCount;
        int reportCount;
        List<String> errors = new ArrayList<>();

        String message() {
            return "FHIR import completed. Patients: " + patientCount
                    + ", Existing: " + skippedPatients
                    + ", Conditions: " + conditionCount
                    + ", Observations: " + observationCount
                    + ", DiagnosticReports: " + reportCount
                    + (errors.isEmpty() ? "" : ", Errors: " + String.join("; ", errors));
        }
    }
}
