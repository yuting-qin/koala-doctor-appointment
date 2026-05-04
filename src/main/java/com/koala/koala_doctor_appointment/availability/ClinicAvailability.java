package com.koala.koala_doctor_appointment.availability;

import com.koala.koala_doctor_appointment.clinic.Clinic;

import java.time.Instant;
import java.util.List;

public record ClinicAvailability(
        Clinic clinic,
        Instant from,
        Instant to,
        List<PracticeAvailability> practices
) {
}
