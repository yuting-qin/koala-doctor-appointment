package com.koala.koala_doctor_appointment.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClinicAvailabilityResponse(
        UUID clinicId,
        String clinicName,
        String timezone,
        Instant from,
        Instant to,
        List<PracticeAvailabilityResponse> practices
) {
}
