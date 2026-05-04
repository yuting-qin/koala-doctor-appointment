package com.koala.koala_doctor_appointment.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DoctorAvailabilityResponse(
        UUID doctorId,
        String doctorName,
        String specialty,
        Instant from,
        Instant to,
        List<PracticeAvailabilityResponse> practices
) {
}
