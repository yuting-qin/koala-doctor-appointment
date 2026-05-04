package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.availability.AvailabilityOrchestrator;
import com.koala.koala_doctor_appointment.availability.ClinicAvailability;
import com.koala.koala_doctor_appointment.availability.DoctorAvailability;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
public class AvailabilityController {

    private final AvailabilityOrchestrator orchestrator;

    public AvailabilityController(AvailabilityOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/clinics/{clinicId}/availability")
    public ClinicAvailabilityResponse getClinicAvailability(
            @PathVariable UUID clinicId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        ClinicAvailability result = orchestrator.getForClinic(clinicId, from, to);
        return AvailabilityMappers.toResponse(result);
    }

    @GetMapping("/doctors/{doctorId}/availability")
    public DoctorAvailabilityResponse getDoctorAvailability(
            @PathVariable UUID doctorId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        DoctorAvailability result = orchestrator.getForDoctor(doctorId, from, to);
        return AvailabilityMappers.toResponse(result);
    }
}
