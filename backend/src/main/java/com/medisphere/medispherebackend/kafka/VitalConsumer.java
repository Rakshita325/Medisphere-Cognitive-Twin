package com.medisphere.medispherebackend.kafka;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import com.medisphere.medispherebackend.service.PatientDataFhirService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Service
@ConditionalOnProperty(name = "medisphere.kafka.enabled", havingValue = "true")
public class VitalConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VitalConsumer.class);
    private final VitalRecordRepository vitalRecordRepository;
    private final IGenericClient fhirClient;

    public VitalConsumer(VitalRecordRepository vitalRecordRepository, IGenericClient fhirClient) {
        this.vitalRecordRepository = vitalRecordRepository;
        this.fhirClient = fhirClient;
    }

    @KafkaListener(
            topics = "patient-vitals",
            groupId = "medisphere-vitals"
    )
    public void consumeVitalData(VitalData vitalData) {
        try {
            validate(vitalData);
            VitalRecord record = new VitalRecord();
            record.setPatientId(vitalData.getPatientId());
            record.setType(vitalData.getType());
            record.setValue(vitalData.getValue());
            record.setUnit(vitalData.getUnit());
            record.setSystolic(vitalData.getSystolicBP());
            record.setDiastolic(vitalData.getDiastolicBP());
            record.setRecordedAt(vitalData.getRecordedAt());
            record.setCreatedAt(Instant.now());
            record.setSource("kafka:patient-vitals");
            vitalRecordRepository.save(record);
            createFhirObservation(vitalData);
        } catch (Exception exception) {
            logger.error("Unable to process vital event for patient {}: {}", vitalData == null ? null : vitalData.getPatientId(), exception.getMessage());
        }
    }

    private void validate(VitalData vitalData) {
        if (vitalData == null || vitalData.getPatientId() == null || vitalData.getPatientId().isBlank()) {
            throw new IllegalArgumentException("patientId is required");
        }
        if (vitalData.getType() == null || vitalData.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (!"Blood Pressure".equalsIgnoreCase(vitalData.getType()) && vitalData.getValue() == null) {
            throw new IllegalArgumentException("value is required");
        }
    }

    private void createFhirObservation(VitalData vitalData) {
        Bundle bundle = fhirClient.search().forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(PatientDataFhirService.IDENTIFIER_SYSTEM, vitalData.getPatientId()))
                .returnBundle(Bundle.class).execute();
        if (!bundle.hasEntry() || !(bundle.getEntryFirstRep().getResource() instanceof Patient patient)) {
            logger.warn("FHIR patient not found for vital event: {}", vitalData.getPatientId());
            return;
        }

        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference("Patient/" + patient.getIdElement().getIdPart()));
        String type = vitalData.getType();
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(codeFor(type)).setDisplay(type);
        observation.getCode().setText(type);
        if (vitalData.getRecordedAt() != null && !vitalData.getRecordedAt().isBlank()) {
            observation.setEffective(new org.hl7.fhir.r4.model.DateTimeType(vitalData.getRecordedAt()));
        }
        if ("Blood Pressure".equalsIgnoreCase(type)) {
            addComponent(observation, "8480-6", vitalData.getSystolicBP());
            addComponent(observation, "8462-4", vitalData.getDiastolicBP());
        } else {
            observation.setValue(new Quantity().setValue(Double.parseDouble(vitalData.getValue())).setUnit(vitalData.getUnit()));
        }
        fhirClient.create().resource(observation).execute();
    }

    private void addComponent(Observation observation, String code, Double value) {
        if (value != null) {
            Observation.ObservationComponentComponent component = observation.addComponent();
            component.getCode().addCoding().setSystem("http://loinc.org").setCode(code);
            component.setValue(new Quantity().setValue(value).setUnit("mmHg"));
        }
    }

    private String codeFor(String type) {
        if ("Heart Rate".equalsIgnoreCase(type)) return "8867-4";
        if ("SpO2".equalsIgnoreCase(type)) return "59408-5";
        if ("Temperature".equalsIgnoreCase(type)) return "8310-5";
        if ("Blood Pressure".equalsIgnoreCase(type)) return "85354-9";
        return "8716-3";
    }
}