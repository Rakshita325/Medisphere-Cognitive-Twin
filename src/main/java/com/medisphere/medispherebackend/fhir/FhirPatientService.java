package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import com.medisphere.medispherebackend.model.PatientTwin;
import com.medisphere.medispherebackend.service.PatientTwinService;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class FhirPatientService {


    private final IGenericClient fhirClient;
    private final PatientTwinService patientTwinService;

    public FhirPatientService(
            IGenericClient fhirClient,
            PatientTwinService patientTwinService) {

        this.fhirClient = fhirClient;
        this.patientTwinService = patientTwinService;
    }

    public List<Patient> getPatients() {

        Bundle bundle = fhirClient
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();

        List<Patient> patients = new ArrayList<>();

        if (bundle.hasEntry()) {
            bundle.getEntry().forEach(entry -> {
                if (entry.getResource() instanceof Patient patient) {
                    patients.add(patient);
                }
            });
        }

        return patients;
    }

    public PatientTwin saveFhirPatientAsTwin(String patientId) {

        Patient patient = fhirClient
                .read()
                .resource(Patient.class)
                .withId(patientId)
                .execute();

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

            age = Period.between(birthDate, LocalDate.now()).getYears();
        }

        PatientTwin patientTwin = new PatientTwin(
                patient.getIdElement().getIdPart(),
                name,
                age,
                gender,
                "Unknown"
        );

        return patientTwinService.createPatientTwin(patientTwin);
    }
}
