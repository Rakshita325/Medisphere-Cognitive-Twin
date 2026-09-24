package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.kafka.VitalData;
import com.medisphere.medispherebackend.kafka.VitalProducer;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vitals")
public class VitalController {

    private final VitalProducer vitalProducer;
    private final ConsentAuthorizationService consentAuthorizationService;

    public VitalController(
            @Autowired(required = false) VitalProducer vitalProducer,
            ConsentAuthorizationService consentAuthorizationService) {
        this.vitalProducer = vitalProducer;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @PostMapping
    public String sendVitals(@RequestBody VitalData vitalData) {

        if (!consentAuthorizationService.isAllowed(vitalData.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient consent is required");
        }

        if (vitalProducer != null) {
            vitalProducer.sendVitalData(vitalData);
            return "Vital data sent successfully";
        }

        return "Vital data received (Kafka streaming is currently disabled)";
    }
}