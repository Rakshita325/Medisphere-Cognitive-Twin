package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.CreateTreatmentTrackingRequest;
import com.medisphere.medispherebackend.model.CarePlan;
import com.medisphere.medispherebackend.model.TreatmentTracking;
import com.medisphere.medispherebackend.repository.CarePlanRepository;
import com.medisphere.medispherebackend.repository.TreatmentTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TreatmentTrackingServiceTest {

    private TreatmentTrackingRepository treatmentTrackingRepository;
    private CarePlanRepository carePlanRepository;
    private TreatmentTrackingService treatmentTrackingService;

    @BeforeEach
    void setUp() {
        treatmentTrackingRepository = mock(TreatmentTrackingRepository.class);
        carePlanRepository = mock(CarePlanRepository.class);
        treatmentTrackingService = new TreatmentTrackingService(treatmentTrackingRepository, carePlanRepository);
    }

    @Test
    void testCreateTracking() {
        CreateTreatmentTrackingRequest request = new CreateTreatmentTrackingRequest();
        request.setPatientId("P001");
        request.setPatientName("John Doe");
        request.setCarePlanId("CP-101");
        request.setDescription("Monitor BP regularly");
        request.setFrequency("Daily");
        request.setStatus("PENDING");

        when(treatmentTrackingRepository.save(any(TreatmentTracking.class))).thenAnswer(i -> {
            TreatmentTracking t = i.getArgument(0);
            t.setId("TRK-1");
            return t;
        });

        TreatmentTracking result = treatmentTrackingService.createTracking(request);

        assertNotNull(result);
        assertEquals("TRK-1", result.getId());
        assertEquals("P001", result.getPatientId());
        assertEquals("VITAL_MONITORING", result.getInterventionType());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void testUpdateStatusToCompleted() {
        TreatmentTracking existing = new TreatmentTracking(
                "INT-1", "P001", "John Doe", "CP-101", "MEDICATION",
                "Take prescribed medication", "Daily", "Today", "PENDING", null
        );
        existing.setId("TRK-1");

        when(treatmentTrackingRepository.findById("TRK-1")).thenReturn(Optional.of(existing));
        when(treatmentTrackingRepository.save(any(TreatmentTracking.class))).thenAnswer(i -> i.getArgument(0));

        Optional<TreatmentTracking> updated = treatmentTrackingService.updateStatus("TRK-1", "COMPLETED", "Taken after lunch", null);

        assertTrue(updated.isPresent());
        assertEquals("COMPLETED", updated.get().getStatus());
        assertNotNull(updated.get().getCompletedAt());
        assertEquals("Taken after lunch", updated.get().getNotes());
    }

    @Test
    void testAdherenceCalculationWithActivities() {
        TreatmentTracking t1 = new TreatmentTracking("I1", "P001", "John", "CP1", "MED", "Med 1", "Daily", "Today", "COMPLETED", null);
        TreatmentTracking t2 = new TreatmentTracking("I2", "P001", "John", "CP1", "BP", "BP check", "Daily", "Today", "COMPLETED", null);
        TreatmentTracking t3 = new TreatmentTracking("I3", "P001", "John", "CP1", "GLUCOSE", "Glucose check", "Daily", "Today", "COMPLETED", null);
        TreatmentTracking t4 = new TreatmentTracking("I4", "P001", "John", "CP1", "EXERCISE", "Exercise", "Daily", "Today", "PENDING", null);
        TreatmentTracking t5 = new TreatmentTracking("I5", "P001", "John", "CP1", "DIET", "Diet", "Daily", "Today", "MISSED", null);
        TreatmentTracking t6 = new TreatmentTracking("I6", "P001", "John", "CP1", "OTHER", "Cancelled item", "Daily", "Today", "CANCELLED", null);

        when(treatmentTrackingRepository.findByPatientId("P001")).thenReturn(List.of(t1, t2, t3, t4, t5, t6));

        Map<String, Object> adherence = treatmentTrackingService.calculatePatientAdherence("P001");

        assertEquals(6, adherence.get("totalRecords"));
        // Due activities = completed (3) + pending (1) + missed (1) = 5 (cancelled is excluded)
        assertEquals(5, adherence.get("totalDue"));
        assertEquals(3, adherence.get("completed"));
        assertEquals(1, adherence.get("pending"));
        assertEquals(1, adherence.get("missed"));
        assertEquals(1, adherence.get("cancelled"));
        // Adherence = 3 / 5 * 100 = 60.0%
        assertEquals(60.0, adherence.get("adherencePercentage"));
        assertEquals("60%", adherence.get("adherenceDisplay"));
    }

    @Test
    void testAdherenceCalculationZeroDue() {
        when(treatmentTrackingRepository.findByPatientId("P999")).thenReturn(Collections.emptyList());

        Map<String, Object> adherence = treatmentTrackingService.calculatePatientAdherence("P999");

        assertEquals(0, adherence.get("totalDue"));
        assertNull(adherence.get("adherencePercentage"));
        assertEquals("No data", adherence.get("adherenceDisplay"));
    }

    @Test
    void testInitializeTrackingForCarePlan() {
        CarePlan cp = new CarePlan();
        cp.setId("CP-999");
        cp.setPatientId("P001");
        cp.setPatientName("Jane Doe");
        cp.setInterventions(List.of(
                "Take prescribed medication as directed (provider reviewed)",
                "Monitor BP regularly",
                "Exercise regularly"
        ));

        when(treatmentTrackingRepository.save(any(TreatmentTracking.class))).thenAnswer(i -> i.getArgument(0));

        List<TreatmentTracking> created = treatmentTrackingService.initializeTrackingForCarePlan(cp);

        assertEquals(3, created.size());
        assertEquals("MEDICATION", created.get(0).getInterventionType());
        assertEquals("VITAL_MONITORING", created.get(1).getInterventionType());
        assertEquals("EXERCISE", created.get(2).getInterventionType());
        assertEquals("PENDING", created.get(0).getStatus());
    }
}
