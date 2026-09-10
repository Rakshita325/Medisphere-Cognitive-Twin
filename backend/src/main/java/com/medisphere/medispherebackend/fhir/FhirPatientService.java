package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.medispherebackend.dto.FhirConditionDto;
import com.medisphere.medispherebackend.dto.FhirDiagnosticReportDto;
import com.medisphere.medispherebackend.dto.FhirObservationDto;
import com.medisphere.medispherebackend.dto.FhirPatientDto;
import com.medisphere.medispherebackend.dto.PatientData;
import com.medisphere.medispherebackend.model.CachedFhirPatient;
import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.repository.CachedFhirPatientRepository;
import com.medisphere.medispherebackend.service.PatientDataFhirService;
import com.medisphere.medispherebackend.service.PatientTwinService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FhirPatientService {

    private static final Logger log = LoggerFactory.getLogger(FhirPatientService.class);

    private volatile long fhirOfflineUntil = 0L;
    private volatile boolean seedImportAttempted = false;

    private final IGenericClient fhirClient;
    private final PatientTwinService patientTwinService;
    private final PatientDataFhirService patientDataFhirService;
    private final CachedFhirPatientRepository cachedFhirPatientRepository;

    @Autowired
    public FhirPatientService(
            IGenericClient fhirClient,
            PatientTwinService patientTwinService,
            PatientDataFhirService patientDataFhirService,
            @Autowired(required = false) CachedFhirPatientRepository cachedFhirPatientRepository) {

        this.fhirClient = fhirClient;
        this.patientTwinService = patientTwinService;
        this.patientDataFhirService = patientDataFhirService;
        this.cachedFhirPatientRepository = cachedFhirPatientRepository;
    }

    public FhirPatientService(
            IGenericClient fhirClient,
            PatientTwinService patientTwinService,
            PatientDataFhirService patientDataFhirService) {
        this(fhirClient, patientTwinService, patientDataFhirService, null);
    }

    private boolean isFhirAvailable() {
        return System.currentTimeMillis() >= fhirOfflineUntil;
    }

    private void recordFhirFailure(Throwable t) {
        log.warn("FHIR server communication failed ({}). Activating offline fallback mode for 30s.", t.getMessage());
        fhirOfflineUntil = System.currentTimeMillis() + 30_000L;
    }

    private void recordFhirSuccess() {
        fhirOfflineUntil = 0L;
    }

    public List<FhirPatientDto> getPatients() {
        if (!seedImportAttempted && isFhirAvailable()) {
            seedImportAttempted = true;
            try {
                patientDataFhirService.importPatientsToFhir();
            } catch (Exception e) {
                log.warn("FHIR seed import could not be completed: {}", e.getMessage());
            }
        }

        if (isFhirAvailable()) {
            try {
                Bundle bundle = fhirClient
                        .search()
                        .forResource(Patient.class)
                        .returnBundle(Bundle.class)
                        .execute();

                List<FhirPatientDto> patients = extractPatientsFromBundle(bundle);

                if (!patients.isEmpty()) {
                    recordFhirSuccess();
                    cachePatients(patients);
                    return patients;
                }
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        List<FhirPatientDto> cached = getCachedPatients();
        if (!cached.isEmpty()) {
            return cached;
        }

        List<FhirPatientDto> fallback = loadFallbackPatientsFromData();
        if (!fallback.isEmpty()) {
            cachePatients(fallback);
            return fallback;
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "FHIR server is unavailable and no cached patient data is available");
    }

    public FhirPatientDto getPatient(String patientId) {
        if (isFhirAvailable()) {
            try {
                Patient patient = resolvePatient(patientId);
                recordFhirSuccess();
                String name = patient.hasName() ? patient.getNameFirstRep().getNameAsSingleString() : "Unknown";
                String gender = patient.hasGender() ? patient.getGender().toCode() : "Unknown";
                String sourcePatientId = patient.getIdentifier().stream()
                        .filter(identifier -> PatientDataFhirService.IDENTIFIER_SYSTEM.equals(identifier.getSystem()))
                        .map(identifier -> identifier.getValue()).findFirst().orElse(null);
                String birthDate = patient.hasBirthDate() ? patient.getBirthDateElement().getValueAsString() : null;
                return new FhirPatientDto(patient.getIdElement().getIdPart(), sourcePatientId, name, gender, birthDate);
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        CachedFhirPatient cached = findCachedPatient(patientId);
        if (cached != null) {
            return new FhirPatientDto(cached.getFhirPatientId(), cached.getSourcePatientId(), cached.getName(), cached.getGender(), cached.getBirthDate());
        }
        PatientTwin twin = patientTwinService != null ? patientTwinService.getBySourcePatientId(patientId) : null;
        if (twin != null) {
            return new FhirPatientDto(twin.getFhirPatientId(), twin.getSourcePatientId(), twin.getName(), twin.getGender(), null);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found in FHIR server or cache: " + patientId);
    }

    public List<FhirConditionDto> getConditions(String patientId) {
        if (isFhirAvailable()) {
            try {
                String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
                List<FhirConditionDto> list = searchConditions(fhirId).stream()
                        .map(condition -> new FhirConditionDto(
                                condition.getIdElement().getIdPart(), fhirId,
                                condition.hasCode() ? condition.getCode().getText() : null,
                                condition.hasClinicalStatus() ? condition.getClinicalStatus().getText() : null,
                                condition.hasOnsetDateTimeType() ? condition.getOnsetDateTimeType().getValueAsString() : null))
                        .toList();
                recordFhirSuccess();
                return list;
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        PatientTwin twin = patientTwinService != null ? patientTwinService.getBySourcePatientId(patientId) : null;
        if (twin != null && twin.getConditions() != null && !twin.getConditions().isEmpty()) {
            return twin.getConditions();
        }
        return Collections.emptyList();
    }

    public List<FhirObservationDto> getObservations(String patientId) {
        if (isFhirAvailable()) {
            try {
                String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
                List<FhirObservationDto> list = searchObservations(fhirId).stream()
                        .map(this::toObservationDto)
                        .toList();
                recordFhirSuccess();
                return list;
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        PatientTwin twin = patientTwinService != null ? patientTwinService.getBySourcePatientId(patientId) : null;
        if (twin != null && twin.getVitals() != null && !twin.getVitals().isEmpty()) {
            return twin.getVitals();
        }
        return Collections.emptyList();
    }

    public List<FhirDiagnosticReportDto> getDiagnosticReports(String patientId) {
        if (isFhirAvailable()) {
            try {
                String fhirId = resolvePatient(patientId).getIdElement().getIdPart();
                List<FhirDiagnosticReportDto> list = searchDiagnosticReports(fhirId).stream()
                        .map(report -> new FhirDiagnosticReportDto(
                                report.getIdElement().getIdPart(), fhirId,
                                report.getCode().getCodingFirstRep().getCode(), report.getCode().getText(),
                                report.hasStatus() ? report.getStatus().toCode() : null,
                                report.hasIssued() ? report.getIssued().toInstant().toString() : null,
                                report.getResult().stream().map(reference -> reference.getReference()).toList()))
                        .toList();
                recordFhirSuccess();
                return list;
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        PatientTwin twin = patientTwinService != null ? patientTwinService.getBySourcePatientId(patientId) : null;
        if (twin != null && twin.getLabReports() != null && !twin.getLabReports().isEmpty()) {
            return twin.getLabReports();
        }
        return Collections.emptyList();
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

    public PatientTwin saveFhirPatientAsTwin(String patientId) {
        if (isFhirAvailable()) {
            try {
                Patient patient = resolvePatient(patientId);
                recordFhirSuccess();
                String name = patient.hasName() ? patient.getNameFirstRep().getNameAsSingleString() : "Unknown";
                String gender = patient.hasGender() ? patient.getGender().toCode() : "Unknown";
                int age = 0;
                if (patient.hasBirthDate()) {
                    LocalDate birthDate = patient
                            .getBirthDate()
                            .toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    age = Period.between(birthDate, LocalDate.now()).getYears();
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
            } catch (Exception e) {
                recordFhirFailure(e);
            }
        }

        CachedFhirPatient cached = findCachedPatient(patientId);
        if (cached != null) {
            int age = 0;
            if (cached.getBirthDate() != null) {
                try {
                    LocalDate birthDate = LocalDate.parse(cached.getBirthDate());
                    age = Period.between(birthDate, LocalDate.now()).getYears();
                } catch (Exception ignored) {
                }
            }
            String effectiveId = cached.getFhirPatientId() != null ? cached.getFhirPatientId() : patientId;
            PatientTwin patientTwin = new PatientTwin(
                    effectiveId,
                    cached.getName() != null ? cached.getName() : "Unknown",
                    age,
                    cached.getGender() != null ? cached.getGender() : "Unknown",
                    "Unknown"
            );
            patientTwin.setSourcePatientId(cached.getSourcePatientId() != null ? cached.getSourcePatientId() : patientId);
            patientTwin.setFhirPatientId(effectiveId);
            return patientTwinService.createPatientTwin(patientTwin);
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Patient could not be resolved from FHIR server or cache: " + patientId);
    }

    private List<FhirPatientDto> extractPatientsFromBundle(Bundle bundle) {
        List<FhirPatientDto> patients = new ArrayList<>();
        while (bundle != null) {
            if (bundle.hasEntry()) {
                bundle.getEntry().forEach(entry -> {
                    if (entry.getResource() instanceof Patient patient) {
                        String id = patient.getIdElement().getIdPart();
                        String name = "Unknown";
                        if (patient.hasName()) {
                            name = patient.getNameFirstRep().getNameAsSingleString();
                        }
                        String gender = "Unknown";
                        if (patient.hasGender()) {
                            gender = patient.getGender().toCode();
                        }
                        String birthDate = null;
                        if (patient.hasBirthDate()) {
                            birthDate = patient.getBirthDateElement().getValueAsString();
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
            bundle = bundle.getLink("next") == null ? null : fhirClient.loadPage().next(bundle).execute();
        }
        return patients;
    }

    private void cachePatients(List<FhirPatientDto> patients) {
        if (cachedFhirPatientRepository == null || patients == null) {
            return;
        }
        try {
            for (FhirPatientDto dto : patients) {
                String fhirId = dto.getFhirPatientId() != null ? dto.getFhirPatientId() : dto.getId();
                CachedFhirPatient existing = null;
                if (dto.getSourcePatientId() != null) {
                    existing = cachedFhirPatientRepository.findBySourcePatientId(dto.getSourcePatientId()).orElse(null);
                }
                if (existing == null && fhirId != null) {
                    existing = cachedFhirPatientRepository.findByFhirPatientId(fhirId).orElse(null);
                }
                if (existing == null) {
                    existing = new CachedFhirPatient(fhirId, dto.getSourcePatientId(), dto.getName(), dto.getGender(), dto.getBirthDate());
                } else {
                    if (fhirId != null) existing.setFhirPatientId(fhirId);
                    if (dto.getSourcePatientId() != null) existing.setSourcePatientId(dto.getSourcePatientId());
                    if (dto.getName() != null) existing.setName(dto.getName());
                    if (dto.getGender() != null) existing.setGender(dto.getGender());
                    if (dto.getBirthDate() != null) existing.setBirthDate(dto.getBirthDate());
                    existing.setLastUpdated(Instant.now());
                }
                cachedFhirPatientRepository.save(existing);
            }
        } catch (Exception e) {
            log.warn("Failed to cache FHIR patients in MongoDB: {}", e.getMessage());
        }
    }

    private List<FhirPatientDto> getCachedPatients() {
        if (cachedFhirPatientRepository == null) {
            return Collections.emptyList();
        }
        try {
            return cachedFhirPatientRepository.findAll().stream()
                    .map(cp -> new FhirPatientDto(cp.getFhirPatientId(), cp.getSourcePatientId(), cp.getName(), cp.getGender(), cp.getBirthDate()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to read cached patients from MongoDB: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<FhirPatientDto> loadFallbackPatientsFromData() {
        try (InputStream is = new ClassPathResource("patient-data.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            List<PatientData> list = mapper.readValue(is, new TypeReference<>() {});
            List<FhirPatientDto> dtos = new ArrayList<>();
            for (PatientData pd : list) {
                String fhirId = pd.getPatientId();
                String sourceId = pd.getPatientId();
                String name = "Unknown";
                String gender = "Unknown";
                String birthDate = null;
                if (pd.getPersonalDetails() != null) {
                    name = (pd.getPersonalDetails().getFirstName() != null ? pd.getPersonalDetails().getFirstName() + " " : "")
                            + (pd.getPersonalDetails().getLastName() != null ? pd.getPersonalDetails().getLastName() : "");
                    name = name.trim().isEmpty() ? "Unknown" : name.trim();
                    gender = pd.getPersonalDetails().getGender() != null ? pd.getPersonalDetails().getGender().toLowerCase() : "Unknown";
                    birthDate = pd.getPersonalDetails().getBirthDate();
                }
                dtos.add(new FhirPatientDto(fhirId, sourceId, name, gender, birthDate));
            }
            return dtos;
        } catch (Exception e) {
            log.warn("Failed to load fallback patient data from patient-data.json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private CachedFhirPatient findCachedPatient(String patientId) {
        if (cachedFhirPatientRepository == null) {
            return null;
        }
        try {
            return cachedFhirPatientRepository.findBySourcePatientId(patientId)
                    .or(() -> cachedFhirPatientRepository.findByFhirPatientId(patientId))
                    .or(() -> cachedFhirPatientRepository.findById(patientId))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Error looking up cached patient {}: {}", patientId, e.getMessage());
            return null;
        }
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
}