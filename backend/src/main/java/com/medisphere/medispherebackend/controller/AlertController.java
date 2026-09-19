package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.model.Alert;
import com.medisphere.medispherebackend.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Returns all active alerts, newest first.
     * Used by the React dashboard on initial page load.
     */
    @GetMapping("/active")
    public List<Alert> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }

    /**
     * Returns alerts for a specific patient.
     */
    @GetMapping("/patient/{patientId}")
    public List<Alert> getPatientAlerts(@PathVariable String patientId) {
        return alertService.getPatientAlerts(patientId);
    }

    /**
     * Acknowledges an alert by ID.
     * The AlertService also broadcasts the updated alert via WebSocket.
     */
    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(@PathVariable String id) {
        return alertService.acknowledgeAlert(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
