package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import com.medisphere.medispherebackend.dto.FhirConditionDto;
import com.medisphere.medispherebackend.dto.FhirDiagnosticReportDto;
import com.medisphere.medispherebackend.dto.FhirObservationDto;
import com.medisphere.medispherebackend.dto.FhirPatientDto;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.service.PatientTwinService;
import com.medisphere.medispherebackend.service.PatientDataFhirService;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FhirPatientService {

    private final IGenericClient fhirClient;
    private final PatientTwinService patientTwinService;
    private final PatientDataFhirService patientDataFhirService;

    public FhirPatientService(
            IGenericClient fhirClient,
            PatientTwinService patientTwinService,
            PatientDataFhirService patientDataFhirService) {

        this.fhirClient = fhirClient;
        this.patientTwinService = patientTwinService;
        this.patientDataFhirService = patientDataFhirService;
    }

    public List<FhirPatientDto> getPatients() {

        patientDataFhirService.importPatientsToFhir();

        Bundle bundle = fhirClient
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();

        List<FhirPatientDto> patients = new ArrayList<>();

        while (bundle != null) {

            if (bundle.hasEntry()) {

                bundle.getEntry().forEach(entry -> {

                    if (entry.getResource() instanceof Patient patient) {

                        String id = patient.getIdElement().getIdPart();

                        String name = "Unknown";

                        if (patient.hasName()) {
                            name = patient
                                    .getNameFirstRep()
                                    .getNameAsSingleString();
                        }

                        String gender = "Unknown";

                        if (patient.hasGender()) {
                            gender = patient.getGender().toCode();
                        }

                        String birthDate = null;

                        if (patient.hasBirthDate()) {
                            birthDate = patient
                                    .getBirthDateElement()
                                    .getValueAsString();
                        }

                        String sourcePatientId = patient.getIdentifier().stream()
                            .filter(identifier -> PatientDataFhirService.IDENTIFIER_SYSTEM.equals(identifier.getSystem()))
                            .map(identifier -> identifier.getValue())
                            .findFirst()
                            .orElse(null);
                        patients.add(new FhirPatientDto(id, sourcePatientId, name, gender, birthDate));
                    }
                });
            }

            bundle = bundle.getLink("next") == null
                    ? null
                    : fhirClient.loadPage().next(bundle).execute();
        }

        return patients;
    }

    public FhirPatientDto getPatient(String patientId) {
        Patient patient = resolvePatient(patientId);
        String name = patient.hasName() ? patient.getNameFirstRep().getNameAsSingleString() : "Unknown";
        String gender = patient.hasGender() ? patient.getGender().toCode() : "Unknown";
        String sourcePatientId = patient.getIdentifier().stream()
                .filter(identifier -> PatientDataFhirService.IDENTIFIER_SYSTEM.equals(identifier.getSystem()))
                .map(identifier -> identifier.getValue()).findFirst().orElse(null);
        String birthDate = patient.hasBirthDate() ? patient.getBirthDateElement().getValueAsString() : null;
        return new FhirPatientDto(patient.getIdElement().getIdPart(), sourcePatientId, name, gender, birthDate);
    }

    public List<FhirConditionDto> getConditions(String patientId) {
        String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
        return searchConditions(fhirId).stream()
                .map(condition -> new FhirConditionDto(
                        condition.getIdElement().getIdPart(), fhirId,
                        condition.hasCode() ? condition.getCode().getText() : null,
                        condition.hasClinicalStatus() ? condition.getClinicalStatus().getText() : null,
                        condition.hasOnsetDateTimeType() ? condition.getOnsetDateTimeType().getValueAsString() : null))
                .toList();
    }

    public List<FhirObservationDto> getObservations(String patientId) {
        String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
        return searchObservations(fhirId).stream()
                .map(this::toObservationDto)
                .toList();
    }

    public List<FhirDiagnosticReportDto> getDiagnosticReports(String patientId) {
        String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
        return searchDiagnosticReports(fhirId).stream()
                .map(report -> new FhirDiagnosticReportDto(
                        report.getIdElement().getIdPart(), fhirId,
                        report.getCode().getCodingFirstRep().getCode(), report.getCode().getText(),
                        report.hasStatus() ? report.getStatus().toCode() : null,
                        report.hasIssued() ? report.getIssued().toInstant().toString() : null,
                        report.getResult().stream().map(reference -> reference.getReference()).toList()))
                .toList();
    }

    public List<FhirObservationDto> getWearables(String patientId) {
        return getObservations(patientId).stream()
                .filter(observation -> "41950-7".equals(observation.getCode()) || "93832-4".equals(observation.getCode()))
                .toList();
    }

        public List<FhirObservationDto> getLabs(String patientId) {
        return getObservations(patientId).stream()
            .filter(observation -> List.of("2339-0", "4548-4", "2093-3", "13457-7", "718-7", "unknown")
                .contains(observation.getCode()))
            .toList();
        }

    private Patient resolvePatient(String patientId) {
        if (patientId.startsWith("P")) {
            Bundle bundle = fhirClient.search().forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().systemAndCode(PatientDataFhirService.IDENTIFIER_SYSTEM, patientId))
                    .returnBundle(Bundle.class).execute();
            if (bundle.hasEntry() && bundle.getEntryFirstRep().getResource() instanceof Patient patient) return patient;
        }
        return fhirClient.read().resource(Patient.class).withId(patientId).execute();
    }

    private List<Condition> searchConditions(String fhirId) {
        Bundle bundle = fhirClient.search().forResource(Condition.class).where(Condition.SUBJECT.hasId(fhirId))
                .returnBundle(Bundle.class).execute();
        return readResources(bundle, Condition.class);
    }

    private List<Observation> searchObservations(String fhirId) {
        Bundle bundle = fhirClient.search().forResource(Observation.class).where(Observation.SUBJECT.hasId(fhirId))
                .returnBundle(Bundle.class).execute();
        return readResources(bundle, Observation.class);
    }

    private List<DiagnosticReport> searchDiagnosticReports(String fhirId) {
        Bundle bundle = fhirClient.search().forResource(DiagnosticReport.class).where(DiagnosticReport.SUBJECT.hasId(fhirId))
                .returnBundle(Bundle.class).execute();
        return readResources(bundle, DiagnosticReport.class);
    }

    private <T extends org.hl7.fhir.r4.model.Resource> List<T> readResources(Bundle bundle, Class<T> resourceType) {
        List<T> resources = new ArrayList<>();
        while (bundle != null) {
            bundle.getEntry().forEach(entry -> {
                if (resourceType.isInstance(entry.getResource())) resources.add(resourceType.cast(entry.getResource()));
            });
            bundle = bundle.getLink("next") == null ? null : fhirClient.loadPage().next(bundle).execute();
        }
        return resources;
    }

    private FhirObservationDto toObservationDto(Observation observation) {
        String value = observation.hasValueQuantity() && observation.getValueQuantity().getValue() != null
            ? observation.getValueQuantity().getValue().toString() : null;
        String unit = observation.hasValueQuantity() ? observation.getValueQuantity().getUnit() : null;
        Double systolic = null;
        Double diastolic = null;
        for (Observation.ObservationComponentComponent component : observation.getComponent()) {
            String code = component.getCode().getCodingFirstRep().getCode();
            if ("8480-6".equals(code) && component.hasValueQuantity()) systolic = component.getValueQuantity().getValue().doubleValue();
            if ("8462-4".equals(code) && component.hasValueQuantity()) diastolic = component.getValueQuantity().getValue().doubleValue();
        }
        String code = observation.getCode().getCodingFirstRep().getCode();
        return new FhirObservationDto(observation.getIdElement().getIdPart(),
                observation.getSubject().getReferenceElement().getIdPart(), observation.getCode().getText(), code,
                value, unit, observation.hasEffectiveDateTimeType() ? observation.getEffectiveDateTimeType().getValueAsString() : null,
                systolic, diastolic);
    }

    public PatientTwin saveFhirPatientAsTwin(String patientId) {

        Patient patient = resolvePatient(patientId);

        String name = "Unknown";

        if (patient.hasName()) {
            name = patient.getNameFirstRep().getNameAsSingleString();
        }

        String gender = "Unknown";

        if (patient.hasGender()) {
            gender = patient.getGender().toCode();
        }

        int age = 0;

        if (patient.hasBirthDate()) {

            LocalDate birthDate = patient
                    .getBirthDate()
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();

            age = Period
                    .between(birthDate, LocalDate.now())
                    .getYears();
        }

        PatientTwin patientTwin = new PatientTwin(
                patient.getIdElement().getIdPart(),
                name,
                age,
                gender,
                "Unknown"
        );
            patientTwin.setSourcePatientId(patient.getIdentifier().stream()
                .filter(identifier -> PatientDataFhirService.IDENTIFIER_SYSTEM.equals(identifier.getSystem()))
                .map(identifier -> identifier.getValue()).findFirst().orElse(patientId));
            patientTwin.setFhirPatientId(patient.getIdElement().getIdPart());

        return patientTwinService.createPatientTwin(patientTwin);
    }
}