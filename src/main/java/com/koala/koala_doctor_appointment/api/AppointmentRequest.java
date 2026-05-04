package com.koala.koala_doctor_appointment.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull UUID practiceId,
        @NotNull UUID patientId,
        @NotNull Instant startTime
) {
}
