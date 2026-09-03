package com.medisphere.medispherebackend.kafka;

import com.medisphere.medispherebackend.model.VitalRecord;
import com.medisphere.medispherebackend.repository.VitalRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VitalConsumer {

    private final VitalRecordRepository vitalRecordRepository;

    public VitalConsumer(VitalRecordRepository vitalRecordRepository) {
        this.vitalRecordRepository = vitalRecordRepository;
    }

    @KafkaListener(topics = "patient-vitals", groupId = "medisphere-vitals")
    public void consumeVitalData(VitalData vitalData) {

        System.out.println("Received vital data:");
        System.out.println("Patient ID: " + vitalData.getPatientId());
        System.out.println("Heart Rate: " + vitalData.getHeartRate());
        System.out.println("Systolic BP: " + vitalData.getSystolicBP());
        System.out.println("Diastolic BP: " + vitalData.getDiastolicBP());
        System.out.println("SpO2: " + vitalData.getSpo2());
        System.out.println("Temperature: " + vitalData.getTemperature());

        VitalRecord record = new VitalRecord(
                vitalData.getPatientId(),
                vitalData.getHeartRate(),
                vitalData.getSystolicBP(),
                vitalData.getDiastolicBP(),
                vitalData.getSpo2(),
                vitalData.getTemperature()
        );

        vitalRecordRepository.save(record);

        System.out.println("Vital data saved to MongoDB successfully.");
    }
}