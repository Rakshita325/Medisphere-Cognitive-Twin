package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.kafka.VitalData;
import com.medisphere.medispherebackend.kafka.VitalProducer;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vitals")
public class VitalController {

    private final VitalProducer vitalProducer;

    public VitalController(VitalProducer vitalProducer) {
        this.vitalProducer = vitalProducer;
    }

    @PostMapping
    public String sendVitals(@RequestBody VitalData vitalData) {

        vitalProducer.sendVitalData(vitalData);

        return "Vital data sent successfully";
    }
}