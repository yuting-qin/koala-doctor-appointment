package com.koala.koala_doctor_appointment.appointment;

import java.time.Instant;
import java.util.UUID;

public record Appointment(
        UUID id,
        UUID practiceId,
        UUID patientId,
        Instant startTime,
        Instant endTime,
        Instant createdAt
) {
}
