package com.medisphere.medispherebackend.controller;

import com.medisphere.medispherebackend.dto.CreateTreatmentTrackingRequest;
import com.medisphere.medispherebackend.dto.UpdateTreatmentStatusRequest;
import com.medisphere.medispherebackend.model.TreatmentTracking;
import com.medisphere.medispherebackend.service.CarePlanService;
import com.medisphere.medispherebackend.service.TreatmentTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TreatmentTrackingControllerTest {

    @Mock
    private TreatmentTrackingService treatmentTrackingService;

    @Mock
    private CarePlanService carePlanService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TreatmentTrackingController(treatmentTrackingService, carePlanService)
        ).build();
    }

    @Test
    void testGetPatientTracking() throws Exception {
        TreatmentTracking t1 = new TreatmentTracking(
                "INT-1", "P001", "John Doe", "CP-1", "VITAL_MONITORING",
                "Monitor BP regularly", "Daily", "Today", "PENDING", null
        );
        t1.setId("TRK-1");

        when(treatmentTrackingService.getTrackingByPatientId("P001")).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/treatment-tracking/patient/P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("TRK-1"))
                .andExpect(jsonPath("$[0].patientId").value("P001"))
                .andExpect(jsonPath("$[0].description").value("Monitor BP regularly"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void testUpdateStatus() throws Exception {
        TreatmentTracking updated = new TreatmentTracking(
                "INT-1", "P001", "John Doe", "CP-1", "VITAL_MONITORING",
                "Monitor BP regularly", "Daily", "Today", "COMPLETED", "Done"
        );
        updated.setId("TRK-1");

        when(treatmentTrackingService.updateStatus(eq("TRK-1"), eq("COMPLETED"), any(), any()))
                .thenReturn(Optional.of(updated));

        String requestBody = "{\"status\": \"COMPLETED\", \"notes\": \"Done\"}";

        mockMvc.perform(put("/api/treatment-tracking/TRK-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("TRK-1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testGetPatientAdherence() throws Exception {
        Map<String, Object> adh = new HashMap<>();
        adh.put("patientId", "P001");
        adh.put("totalDue", 4);
        adh.put("completed", 3);
        adh.put("pending", 1);
        adh.put("missed", 0);
        adh.put("adherencePercentage", 75.0);
        adh.put("adherenceDisplay", "75%");

        when(treatmentTrackingService.calculatePatientAdherence("P001")).thenReturn(adh);

        mockMvc.perform(get("/api/treatment-tracking/patient/P001/adherence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("P001"))
                .andExpect(jsonPath("$.completed").value(3))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.adherenceDisplay").value("75%"));
    }
}
