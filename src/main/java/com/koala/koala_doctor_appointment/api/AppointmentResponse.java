package com.koala.koala_doctor_appointment.api;

import com.koala.koala_doctor_appointment.appointment.Appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID practiceId,
        UUID patientId,
        Instant startTime,
        Instant endTime,
        Instant createdAt
) {
    static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.id(), a.practiceId(), a.patientId(),
                a.startTime(), a.endTime(), a.createdAt());
    }
}
